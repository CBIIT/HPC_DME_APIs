package gov.nih.nci.hpc.web.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.springframework.ui.Model;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcCollectionSearchResultDetailed;
import gov.nih.nci.hpc.web.model.HpcDatafileSearchResultDetailed;
import gov.nih.nci.hpc.web.model.HpcDownloadDatafile;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.model.HpcSearchResult;

public class HpcSearchUtil {

	public static void processResponseResults(HpcSearch search, Response restResponse, Model model, HttpSession session)
			throws JsonParseException, IOException {
		if (search.getSearchType().equalsIgnoreCase("collection"))
			processCollectionResults(search, restResponse, model, session);
		else
			processDataObjectResults(search, restResponse, model, session);
	}

	private static void processCollectionResults(HpcSearch search, Response restResponse, Model model, HttpSession session)
			throws JsonParseException, IOException {
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
		HpcDataManagementRulesDTO basePathRules = HpcClientUtil.getBasePathManagementRules(modelDTO, user.getDefaultBasePath());
		HpcCollectionListDTO collections = parser.readValueAs(HpcCollectionListDTO.class);
		if (!search.isDetailed()) {
			List<String> searchResults = collections.getCollectionPaths();
			List<HpcSearchResult> returnResults = new ArrayList<HpcSearchResult>();
			for (String result : searchResults) {
				HpcSearchResult returnResult = new HpcSearchResult();
				returnResult.setPath(result);
				returnResult.setDownload(result);
				returnResults.add(returnResult);
				returnResult.setPermission(result);

			}
			model.addAttribute("searchresults", returnResults);
			model.addAttribute("searchType", "collection");
			model.addAttribute("totalCount", collections.getTotalCount());
			model.addAttribute("currentPageSize", search.getPageSize());
			model.addAttribute("totalPages", getTotalPages(collections.getTotalCount(), collections.getLimit()));
			
		} else {
			SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm");
			List<HpcCollectionDTO> searchResults = collections.getCollections();
			List<HpcCollectionSearchResultDetailed> returnResults = new ArrayList<HpcCollectionSearchResultDetailed>();
			for (HpcCollectionDTO result : searchResults) {
				HpcCollectionSearchResultDetailed returnResult = new HpcCollectionSearchResultDetailed();
				returnResult.setPath(result.getCollection().getCollectionName());
				returnResult.setUuid(getAttributeValue("uuid", result.getMetadataEntries()));
				returnResult.setRegisteredBy(getAttributeValue("registered_by", result.getMetadataEntries()));
				returnResult.setCreatedOn(format.format(result.getCollection().getCreatedAt().getTime()));
				returnResult.setCollectionType(getAttributeValue("collection_type", result.getMetadataEntries()));
				returnResult.setDownload(result.getCollection().getAbsolutePath());
				returnResult.setPermission(result.getCollection().getAbsolutePath());
				returnResult.setMetadataEntries(new HpcMetadataEntries());
				returnResult.getMetadataEntries().getSelfMetadataEntries().addAll(new ArrayList<HpcMetadataEntry>(filterEncryptedMetadata(user, basePathRules, search, result.getCollection().getCollectionName(), result.getMetadataEntries().getSelfMetadataEntries())));
				returnResult.getMetadataEntries().getParentMetadataEntries().addAll(new ArrayList<HpcMetadataEntry>(filterEncryptedMetadata(user, basePathRules, search, result.getCollection().getCollectionName(), result.getMetadataEntries().getParentMetadataEntries())));
				returnResults.add(returnResult);

			}
			model.addAttribute("searchresults", returnResults);
			model.addAttribute("detailed", "yes");
			model.addAttribute("searchType", "collection");
			model.addAttribute("totalCount", collections.getTotalCount());
			model.addAttribute("currentPageSize", search.getPageSize());
			model.addAttribute("totalPages", getTotalPages(collections.getTotalCount(), collections.getLimit()));
		}
	}

	public static int getTotalPages(int totalCount, int limit)
	{
		int total = 0;
		if(limit <=0)
			limit = 100;
		total = totalCount / limit;
		if(totalCount % limit != 0)
			total++;
		return total;
	}
	
	private static void processDataObjectResults(HpcSearch search, Response restResponse, Model model, HttpSession session)
			throws JsonParseException, IOException {
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
		HpcDataObjectListDTO dataObjects = parser.readValueAs(HpcDataObjectListDTO.class);
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
		HpcDataManagementRulesDTO basePathRules = HpcClientUtil.getBasePathManagementRules(modelDTO, user.getDefaultBasePath());
		if (!search.isDetailed()) {
			List<String> searchResults = dataObjects.getDataObjectPaths();
			List<HpcSearchResult> returnResults = new ArrayList<HpcSearchResult>();
			for (String result : searchResults) {
				HpcSearchResult returnResult = new HpcSearchResult();
				returnResult.setPath(result);
				returnResult.setDownload(result);
				returnResult.setPermission(result);
				returnResult.setLink(result);
				returnResults.add(returnResult);

			}
			model.addAttribute("searchresults", returnResults);
			model.addAttribute("searchType", "datafile");
			model.addAttribute("totalCount", dataObjects.getTotalCount());
			model.addAttribute("currentPageSize", search.getPageSize());
			model.addAttribute("totalPages", getTotalPages(dataObjects.getTotalCount(), dataObjects.getLimit()));
		} else {
			SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm");
			List<HpcDataObjectDTO> searchResults = dataObjects.getDataObjects();
			List<HpcDatafileSearchResultDetailed> returnResults = new ArrayList<HpcDatafileSearchResultDetailed>();
			for (HpcDataObjectDTO result : searchResults) {
				HpcDatafileSearchResultDetailed returnResult = new HpcDatafileSearchResultDetailed();
				returnResult.setPath(result.getDataObject().getAbsolutePath());
				returnResult.setUuid(getAttributeValue("uuid", result.getMetadataEntries()));
				returnResult.setRegisteredBy(getAttributeValue("registered_by", result.getMetadataEntries()));
				returnResult.setCreatedOn(format.format(result.getDataObject().getCreatedAt().getTime()));
				returnResult.setChecksum(getAttributeValue("checksum", result.getMetadataEntries()));
				returnResult.setDownload(result.getDataObject().getAbsolutePath());
				returnResult.setPermission(result.getDataObject().getAbsolutePath());
				returnResult.setLink(result.getDataObject().getAbsolutePath());
				returnResult.setMetadataEntries(new HpcMetadataEntries());
				returnResult.getMetadataEntries().getSelfMetadataEntries().addAll(new ArrayList<HpcMetadataEntry>(filterEncryptedMetadata(user, basePathRules, search, result.getDataObject().getAbsolutePath(), result.getMetadataEntries().getSelfMetadataEntries())));
				returnResult.getMetadataEntries().getParentMetadataEntries().addAll(new ArrayList<HpcMetadataEntry>(filterEncryptedMetadata(user, basePathRules, search, result.getDataObject().getAbsolutePath(), result.getMetadataEntries().getParentMetadataEntries())));
				returnResults.add(returnResult);

			}
			model.addAttribute("searchresults", returnResults);
			model.addAttribute("detailed", "yes");
			model.addAttribute("searchType", "datafile");
			model.addAttribute("totalCount", dataObjects.getTotalCount());
			model.addAttribute("currentPageSize", search.getPageSize());
			model.addAttribute("totalPages", getTotalPages(dataObjects.getTotalCount(), dataObjects.getLimit()));
		}
	}

	private static List<HpcMetadataEntry> filterEncryptedMetadata(HpcUserDTO user, HpcDataManagementRulesDTO basePathRules, HpcSearch search, String path, List<HpcMetadataEntry> entries) {
		if (entries == null)
			return new ArrayList<>();

		//If userKey is not provided or basePath is not user's basePath, no action
		if(StringUtils.isBlank(search.getUserKey()) || !StringUtils.startsWith(path, user.getDefaultBasePath()))
			return entries;
		
		//Check for encrypted fields for this base path and decrypt	
		for (HpcMetadataEntry entry : entries) {
			if(isEncryptedAttribute(entry.getAttribute(), null, basePathRules.getCollectionMetadataValidationRules())) {
			   try {
				   entry.setValue(HpcEncryptionUtil.decrypt(search.getUserKey(), Base64.getDecoder().decode(entry.getValue())));
			   } catch (Exception e) {
				   //It could not be decrypted using the supplied key, leave it.
			   }
			   
			}
		}
		
		return entries;
	}
	
	private static boolean isEncryptedAttribute(String attribute, String collectionType, List<HpcMetadataValidationRule> rules) {
      for(HpcMetadataValidationRule rule: rules) {
        if (StringUtils.equals(rule.getAttribute(), attribute) && collectionType != null && rule.getCollectionTypes().contains(collectionType))
          return rule.getEncrypted();
        if (StringUtils.equals(rule.getAttribute(), attribute) && collectionType == null)
          return rule.getEncrypted();
      }
      return false;
    }

	private static String getAttributeValue(String attrName, HpcMetadataEntries entries) {
		if (entries == null)
			return null;

		List<HpcMetadataEntry> selfEntries = entries.getSelfMetadataEntries();
		for (HpcMetadataEntry entry : selfEntries) {
			if (entry.getAttribute().equals(attrName))
				return entry.getValue();
		}
		List<HpcMetadataEntry> parentEntries = entries.getParentMetadataEntries();
		for (HpcMetadataEntry entry : parentEntries) {
			if (entry.getAttribute().equals(attrName))
				return entry.getValue();
		}
		return null;
	}

	public static void cacheSelectedRows(HttpSession session, HttpServletRequest request, Model model)
	{
		HpcDownloadDatafile hpcDownloadDatafile = (HpcDownloadDatafile)session.getAttribute("hpcSelectedDatafileList");

		HpcDownloadDatafile	newHpcDownloadDatafile = new HpcDownloadDatafile();

		String selectedPathsStr = request.getParameter("selectedFilePaths");
		if(selectedPathsStr == null)
		{
			model.addAttribute("hpcSelectedDatafileList", hpcDownloadDatafile != null ? hpcDownloadDatafile.getSelectedPaths() : null);
			return;
		}
		
		StringTokenizer tokens = new StringTokenizer(selectedPathsStr, ",");
		while(tokens.hasMoreTokens())
		{
			String path = tokens.nextToken();
			newHpcDownloadDatafile.getSelectedPaths().add(path);
		}
		
		session.setAttribute("hpcSelectedDatafileList", newHpcDownloadDatafile);
		model.addAttribute("hpcSelectedDatafileList", newHpcDownloadDatafile.getSelectedPaths());
	}

	public static void clearCachedSelectedRows(HttpSession session)
	{
		session.removeAttribute("hpcSelectedDatafileList");
	}
}
