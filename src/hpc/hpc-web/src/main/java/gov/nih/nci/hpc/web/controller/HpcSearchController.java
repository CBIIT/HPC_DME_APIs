/**
 * HpcSearchProjectController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcNamedCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcCollectionSearchResultDetailed;
import gov.nih.nci.hpc.web.model.HpcDatafileSearchResultDetailed;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.model.HpcSearchResult;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * HPC DM Project Search controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDataRegistrationController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/search")
public class HpcSearchController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionServiceURL;
	@Value("${gov.nih.nci.hpc.server.search.collection.compound}")
	private String compoundCollectionSearchServiceURL;
	@Value("${gov.nih.nci.hpc.server.search.dataobject.compound}")
	private String compoundDataObjectSearchServiceURL;
	@Value("${gov.nih.nci.hpc.server.dataObject}")
	private String datafileServiceURL;
	@Value("${gov.nih.nci.hpc.server.query}")
	private String queryURL;
	@Value("${gov.nih.nci.hpc.server.metadataattributes}")
	private String hpcMetadataAttrsURL;

	/*
	 * Action for Datset registration page
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String body,  @RequestParam String queryName, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		HpcNamedCompoundMetadataQueryDTO query = null;
		try {
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			String userPasswdToken = (String) session.getAttribute("userpasstoken");

			String authToken = (String) session.getAttribute("hpcUserToken");

			query = HpcClientUtil.getQuery(userPasswdToken, queryURL, queryName, sslCertPath, sslCertPassword);
			String serviceURL = collectionServiceURL;
			
			String requestURL;
			if(query != null && query.getNamedCompoundQuery().getCompoundQueryType().equals(HpcCompoundMetadataQueryType.COLLECTION))
				requestURL = compoundCollectionSearchServiceURL+ "/query/"+queryName;
			else if(query != null && query.getNamedCompoundQuery().getCompoundQueryType().equals(HpcCompoundMetadataQueryType.DATA_OBJECT))
				requestURL = compoundDataObjectSearchServiceURL+ "/"+queryName;
			else
				return "dashboard";
			
			session.setAttribute("namedCompoundQuery", query.getNamedCompoundQuery());
			
			if(query.getNamedCompoundQuery().getDetailedResponse())
				requestURL = requestURL + "?detailedResponse=true&totalCount=false";
			
			WebClient client = HpcClientUtil.getWebClient(requestURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);

			Response restResponse = client.invoke("GET", serviceURL);
			if (restResponse.getStatus() == 200) {
				HpcSearch search = new HpcSearch();
				search.setSearchType(query.getNamedCompoundQuery().getCompoundQueryType().value());
				search.setDetailed(query.getNamedCompoundQuery().getDetailedResponse());
				processResponseResults(search, restResponse, model);
			} else {
				String message = "No matching results!";
				ObjectError error = new ObjectError("hpcSearch", message);
				bindingResult.addError(error);
				model.addAttribute("error", message);
				return "dashboard";
			}
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search projects due to: " + e.getMessage());
			return "dashboard";
		} catch (HttpStatusCodeException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search projects due to: " + e.getMessage());
			return "dashboard";
		} catch (RestClientException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search projects due to: " + e.getMessage());
			return "dashboard";
		} catch (Exception e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search projects due to: " + e.getMessage());
			return "dashboard";
		}
		
		if(query == null)
			return "dashboard";
		else if(query.getNamedCompoundQuery().getCompoundQueryType().equals(HpcCompoundMetadataQueryType.COLLECTION) && query.getNamedCompoundQuery().getDetailedResponse())
			return "collectionsearchresultdetail";
		else if(query.getNamedCompoundQuery().getCompoundQueryType().equals(HpcCompoundMetadataQueryType.COLLECTION) && !query.getNamedCompoundQuery().getDetailedResponse())
			return "collectionsearchresult";
		else if(query.getNamedCompoundQuery().getCompoundQueryType().equals(HpcCompoundMetadataQueryType.DATA_OBJECT) && query.getNamedCompoundQuery().getDetailedResponse())
			return "dataobjectsearchresultdetail";
		else if(query.getNamedCompoundQuery().getCompoundQueryType().equals(HpcCompoundMetadataQueryType.DATA_OBJECT) && !query.getNamedCompoundQuery().getDetailedResponse())
			return "dataobjectsearchresult";
		else
			return "dashboard";
	}

	private void processResponseResults(HpcSearch search, Response restResponse, Model model)
			throws JsonParseException, IOException {
		if (search.getSearchType().equals(HpcCompoundMetadataQueryType.COLLECTION.value()))
			processCollectionResults(search, restResponse, model);
		else
			processDataObjectResults(search, restResponse, model);
	}

	private void processCollectionResults(HpcSearch search, Response restResponse, Model model)
			throws JsonParseException, IOException {
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
		HpcCollectionListDTO collections = parser.readValueAs(HpcCollectionListDTO.class);
		
		if (!search.isDetailed()) {
			List<String> searchResults = collections.getCollectionPaths();
			List<HpcSearchResult> returnResults = new ArrayList<HpcSearchResult>();
			for (String result : searchResults) {
				HpcSearchResult returnResult = new HpcSearchResult();
				returnResult.setPath(result);
				returnResults.add(returnResult);

			}
			model.addAttribute("searchresults", returnResults);
			model.addAttribute("searchType", "collection");
		} else {
			List<HpcCollectionDTO> searchResults = collections.getCollections();
			List<HpcCollectionSearchResultDetailed> returnResults = new ArrayList<HpcCollectionSearchResultDetailed>();
			for (HpcCollectionDTO result : searchResults) {
				HpcCollectionSearchResultDetailed returnResult = new HpcCollectionSearchResultDetailed();
				returnResult.setPath(result.getCollection().getCollectionName());
				returnResult.setUuid(getAttributeValue("uuid", result.getMetadataEntries()));
				returnResult.setRegisteredBy(getAttributeValue("registered_by", result.getMetadataEntries()));
				returnResult.setCreatedOn(getAttributeValue("original_date_created", result.getMetadataEntries()));
				returnResult.setCollectionType(getAttributeValue("collection_type", result.getMetadataEntries()));
				returnResults.add(returnResult);

			}
			model.addAttribute("searchresults", returnResults);
			model.addAttribute("detailed", "yes");
			model.addAttribute("searchType", "collection");
		}
	}

	private void processDataObjectResults(HpcSearch search, Response restResponse, Model model)
			throws JsonParseException, IOException {
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
		HpcDataObjectListDTO dataObjects = parser.readValueAs(HpcDataObjectListDTO.class);
		if (!search.isDetailed()) {
			List<String> searchResults = dataObjects.getDataObjectPaths();
			List<HpcSearchResult> returnResults = new ArrayList<HpcSearchResult>();
			for (String result : searchResults) {
				HpcSearchResult returnResult = new HpcSearchResult();
				returnResult.setPath(result);
				returnResult.setDownload(result);
				returnResults.add(returnResult);

			}
			model.addAttribute("searchresults", returnResults);
			model.addAttribute("searchType", "datafile");
		} else {
			List<HpcDataObjectDTO> searchResults = dataObjects.getDataObjects();
			List<HpcDatafileSearchResultDetailed> returnResults = new ArrayList<HpcDatafileSearchResultDetailed>();
			for (HpcDataObjectDTO result : searchResults) {
				HpcDatafileSearchResultDetailed returnResult = new HpcDatafileSearchResultDetailed();
				returnResult.setPath(result.getDataObject().getDataName());
				returnResult.setUuid(getAttributeValue("uuid", result.getMetadataEntries()));
				returnResult.setRegisteredBy(getAttributeValue("registered_by", result.getMetadataEntries()));
				returnResult.setCreatedOn(getAttributeValue("original_date_created", result.getMetadataEntries()));
				returnResult.setDownload(result.getDataObject().getDataName());
				returnResults.add(returnResult);

			}
			model.addAttribute("searchresults", returnResults);
			model.addAttribute("detailed", "yes");
			model.addAttribute("searchType", "datafile");
		}
	}


	private String getAttributeValue(String attrName, HpcMetadataEntries entries) {
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



}
