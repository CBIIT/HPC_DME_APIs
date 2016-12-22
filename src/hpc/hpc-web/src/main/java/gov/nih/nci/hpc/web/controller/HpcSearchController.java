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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import gov.nih.nci.hpc.domain.datamanagement.HpcDataHierarchy;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataLevelAttributes;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryLevelFilter;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMetadataAttributesListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcMetadataHierarchy;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.model.HpcSearchResult;
import gov.nih.nci.hpc.web.model.HpcCollectionSearchResultDetailed;
import gov.nih.nci.hpc.web.model.HpcDataColumn;
import gov.nih.nci.hpc.web.model.HpcDatafileSearchResultDetailed;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.Util;

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
	@Value("${gov.nih.nci.hpc.server.model}")
	private String modelServiceURL;
	@Value("${gov.nih.nci.hpc.server.metadataattributes}")
	private String hpcMetadataAttrsURL;

	/*
	 * Action for Datset registration page
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		HpcSearch hpcSearch = new HpcSearch();
		hpcSearch.setSearchType("collection");
		model.addAttribute("hpcSearch", hpcSearch);
		String authToken = (String) session.getAttribute("hpcUserToken");
		String userPasswdToken = (String) session.getAttribute("userpasstoken");
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}
		//populateHierarchy(model, userPasswdToken, user);
		populateMetadata(model, userPasswdToken, user, "collection", session);
		populateOperators(model);
		populateLevelOperators(model);
		return "search";
	}

	/*
	 * Action for Project registration
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String search(@Valid @ModelAttribute("hpcSearch") HpcSearch search, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		if (search.getActionType() != null && search.getActionType().equals("refresh")) {
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			String userPasswdToken = (String) session.getAttribute("userpasstoken");
			//populateHierarchy(model, userPasswdToken, user);
			populateMetadata(model, userPasswdToken, user, search.getSearchType(), session);
			populateOperators(model);
			populateLevelOperators(model);
			return "search";
		}

		boolean success = false;
		try {

			// String criteria = getCriteria();
			HpcCompoundMetadataQueryDTO compoundQuery = constructCriteria(search);
			if (search.isDetailed())
				compoundQuery.setDetailedResponse(true);
			// criteria = criteria + "&detailedResponse=true";
			String authToken = (String) session.getAttribute("hpcUserToken");
			String serviceURL = compoundDataObjectSearchServiceURL;
			if (search.getSearchType() != null && search.getSearchType().equals("collection"))
				serviceURL = compoundCollectionSearchServiceURL;

			WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);

			Response restResponse = client.invoke("POST", compoundQuery);
			if (restResponse.getStatus() == 200) {
				processResponseResults(search, restResponse, model);
				success = true;
				// model.addAttribute("searchresultscolumns",getColumnDefs(search.isDetailed()));
			} else {
				String message = "No matching results!";
				ObjectError error = new ObjectError("hpcSearch", message);
				bindingResult.addError(error);
				model.addAttribute("error", message);
				return "search";
			}
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search projects due to: " + e.getMessage());
			return "search";
		} catch (HttpStatusCodeException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search projects due to: " + e.getMessage());
			return "search";
		} catch (RestClientException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search projects due to: " + e.getMessage());
			return "search";
		} catch (Exception e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search projects due to: " + e.getMessage());
			return "search";
		} finally {
			if (!success) {
				HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
				String userPasswdToken = (String) session.getAttribute("userpasstoken");
				//populateHierarchy(model, userPasswdToken, user);
				populateMetadata(model, userPasswdToken, user, search.getSearchType(), session);
				populateOperators(model);
				populateLevelOperators(model);
			}
		}

		if (search.getSearchType().equals("collection"))
			return search.isDetailed() ? "collectionsearchresultdetail" : "collectionsearchresult";
		else
			return search.isDetailed() ? "dataobjectsearchresultdetail" : "dataobjectsearchresult";
	}

	private void processResponseResults(HpcSearch search, Response restResponse, Model model)
			throws JsonParseException, IOException {
		if (search.getSearchType().equals("collection"))
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
				returnResults.add(returnResult);

			}
			model.addAttribute("searchresults", returnResults);
		} else {
			List<HpcDataObjectDTO> searchResults = dataObjects.getDataObjects();
			List<HpcDatafileSearchResultDetailed> returnResults = new ArrayList<HpcDatafileSearchResultDetailed>();
			for (HpcDataObjectDTO result : searchResults) {
				HpcDatafileSearchResultDetailed returnResult = new HpcDatafileSearchResultDetailed();
				returnResult.setPath(result.getDataObject().getDataName());
				returnResult.setUuid(getAttributeValue("uuid", result.getMetadataEntries()));
				returnResult.setRegisteredBy(getAttributeValue("registered_by", result.getMetadataEntries()));
				returnResult.setCreatedOn(getAttributeValue("original_date_created", result.getMetadataEntries()));
				returnResults.add(returnResult);

			}
			model.addAttribute("searchresults", returnResults);
			model.addAttribute("detailed", "yes");
		}
	}

	private HpcCompoundMetadataQueryDTO constructCriteria(HpcSearch search) {
		HpcCompoundMetadataQueryDTO dto = new HpcCompoundMetadataQueryDTO();
		HpcCompoundMetadataQuery query = null;
		if (search.getAdvancedCriteria() != null && !search.getAdvancedCriteria().isEmpty())
			query = buildAdvancedSearch(search);
		else
			query = buildSimpleSearch(search);

		dto.setCompoundQuery(query);
		dto.setDetailedResponse(search.isDetailed());
		return dto;
	}

	private HpcCompoundMetadataQuery buildSimpleSearch(HpcSearch search) {
		HpcCompoundMetadataQuery query = new HpcCompoundMetadataQuery();
		List<HpcMetadataQuery> queries = new ArrayList<HpcMetadataQuery>();
		query.setOperator(HpcCompoundMetadataQueryOperator.AND);
		for (int i = 0; i < search.getAttrName().length; i++) {
			String attrName = search.getAttrName()[i];
			String attrValue = search.getAttrValue()[i];
			String operator = search.getOperator()[i];
			String level = search.getLevel()[i];
			if (!attrName.isEmpty() && !attrValue.isEmpty() && !operator.isEmpty()) {
				HpcMetadataQuery criteria = new HpcMetadataQuery();
				criteria.setAttribute(attrName);
				criteria.setValue(attrValue);
				criteria.setOperator(HpcMetadataQueryOperator.fromValue(operator));
				if (level != null && !level.equals("-1")) {
					HpcMetadataQueryLevelFilter levelFilter = new HpcMetadataQueryLevelFilter();
					if(level.equals("datafile"))
						level = "1";
					levelFilter.setLevel(new Integer(level));
					levelFilter.setOperator(HpcMetadataQueryOperator.EQUAL);
					criteria.setLevelFilter(levelFilter);
				}
				queries.add(criteria);
			}
		}
		query.getQueries().addAll(queries);
		System.out.println("query....." + query);
		return query;
	}

	private HpcCompoundMetadataQuery buildAdvancedSearch(HpcSearch search) {
		HpcCompoundMetadataQuery query = new HpcCompoundMetadataQuery();
		return query;
	}

	private String getCriteria() {
		return "metadataQuery=" + URLEncoder.encode("{\"a\":\"name\",\"v\":\"prasad ui project%\",\"o\":\"LIKE\"}");
	}

	// private List<HpcDataColumn> getColumnDefs(boolean detailed)
	// {
	// if(detailed)
	// {
	// List<HpcDataColumn> columns = new ArrayList<HpcDataColumn>();
	// HpcDataColumn idColumn = new HpcDataColumn();
	// idColumn.setDisplayName("UUID");
	// idColumn.setField("uuid");
	// idColumn.setWidth("300");
	// idColumn.setCellTemplate("'<div class=\"ui-grid-cell-contents\"
	// title=\"TOOLTIP\"><a href=\"../project?id={{COL_FIELD
	// CUSTOM_FILTERS}}\">{{COL_FIELD CUSTOM_FILTERS}}</a></div>'");
	// columns.add(idColumn);
	//
	//
	// HpcDataColumn registeredBy = new HpcDataColumn();
	// registeredBy.setDisplayName("Registered by");
	// registeredBy.setField("registered_by");
	// registeredBy.setWidth("300");
	// columns.add(registeredBy);
	//
	// HpcDataColumn colType = new HpcDataColumn();
	// colType.setDisplayName("Collection Type");
	// colType.setField("collection_type");
	// colType.setWidth("300");
	// columns.add(colType);
	//
	// HpcDataColumn createdOn = new HpcDataColumn();
	// createdOn.setDisplayName("Created On");
	// createdOn.setField("original_date_created");
	// createdOn.setWidth("300");
	// columns.add(createdOn);
	//
	// HpcDataColumn path = new HpcDataColumn();
	// path.setDisplayName("Path");
	// path.setField("path");
	// path.setWidth("300");
	// columns.add(path);
	//
	// return columns;
	// }
	// else
	// {
	// List<HpcDataColumn> columns = new ArrayList<HpcDataColumn>();
	// HpcDataColumn path = new HpcDataColumn();
	// path.setDisplayName("Path");
	// path.setField("path");
	// path.setWidth("500");
	// path.setCellTemplate("'<div><div ng-if=\"!col.grouping ||
	// col.grouping.groupPriority === undefined || col.grouping.groupPriority
	// === null || ( row.groupHeader &amp;&amp; col.grouping.groupPriority ===
	// row.treeLevel )\" class=\"ui-grid-cell-contents\" title=\"TOOLTIP\"><a
	// href=\"../dataset?id={{grid.appScope.getId(grid, row)}}\">{{COL_FIELD
	// CUSTOM_FILTERS}}</a></div></div>'");
	// columns.add(path);
	//
	// return columns;
	// }
	// }

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

	private void populateOperators(Model model) {
		Map<String, String> entries = new HashMap<String, String>();
		entries.put("EQUAL", "=");
		entries.put("NOT_EQUAL", "!=");
		entries.put("NUM_LESS_THAN", "<");
		entries.put("NUM_LESS_OR_EQUAL", "<=");
		entries.put("NUM_GREATER_OR_EQUAL", ">=");
		entries.put("LIKE", "LIKE");
		model.addAttribute("operators", entries);
	}

	private void populateLevelOperators(Model model) {
		Map<String, String> entries = new HashMap<String, String>();
		entries.put("EQUAL", "=");
		entries.put("NOT_EQUAL", "!=");
		entries.put("NUM_LESS_THAN", "<");
		entries.put("NUM_LESS_OR_EQUAL", "<=");
		entries.put("NUM_GREATER_OR_EQUAL", ">=");
		model.addAttribute("leveloperators", entries);
	}

	private void populateMetadata(Model model, String authToken, HpcUserDTO user, String type, HttpSession session) {
		model.addAttribute("doc", user.getNciAccount().getDoc());
		if(session.getAttribute("hierarchy") != null)
		{
			model.addAttribute("hierarchy", session.getAttribute("hierarchy"));
			return;
		}
		
		HpcMetadataHierarchy dataHierarchy = new HpcMetadataHierarchy();
		long start = System.currentTimeMillis();
		Map<String, String> hierarchy = getHierarchy(authToken, user);
		long stop = System.currentTimeMillis();
		List<String> attrs = new ArrayList<String>();
		System.out.println("time: "+(start-stop)/1000);
		
			HpcMetadataAttributesListDTO dto = HpcClientUtil.getMetadataAttrNames(authToken,
					hpcMetadataAttrsURL, sslCertPath, sslCertPassword);
			stop = System.currentTimeMillis();
			System.out.println("time1: "+(start-stop)/1000);
			if (dto != null && dto.getCollectionMetadataAttributes() != null) {
				for(HpcMetadataLevelAttributes levelAttrs : dto.getCollectionMetadataAttributes()) 
				{
					dataHierarchy.getCollectionAttrsSet().addAll(levelAttrs.getMetadataAttributes());
					for(String name : levelAttrs.getMetadataAttributes())
						attrs.add(levelAttrs.getLevel()+":collection:"+name);
				}
			}

			if (dto != null && dto.getDataObjectMetadataAttributes() != null) {
				for(HpcMetadataLevelAttributes levelAttrs : dto.getDataObjectMetadataAttributes()) 
				{
					dataHierarchy.getDataobjectAttrsSet().addAll(levelAttrs.getMetadataAttributes());
					for(String name : levelAttrs.getMetadataAttributes())
						attrs.add(levelAttrs.getLevel()+":datafile:"+name);
				}
			}

		hierarchy.put(("datafile"), "Data file");
		dataHierarchy.setLevels(hierarchy);
		dataHierarchy.setAllAttributes(attrs);
		model.addAttribute("hierarchy", dataHierarchy);
		session.setAttribute("hierarchy", dataHierarchy);
	}

	private void populateHierarchy(Model model, String authToken, HpcUserDTO user) {
		try {

			model.addAttribute("doc", user.getNciAccount().getDoc());
			model.addAttribute("hierarchies", getHierarchy(authToken, user));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Map<String, String> getHierarchy(String authToken, HpcUserDTO user) {
		Map<String, String> hierarchiesMap = new HashMap<String, String>();
		try {
			HpcDataManagementModelDTO docDTO = HpcClientUtil.getDOCModel(authToken, modelServiceURL,
					user.getNciAccount().getDoc(), sslCertPath, sslCertPassword);
			HpcDataHierarchy hierarchy = docDTO.getDataHierarchy();

			List<String> hierarchies = new ArrayList<String>();
			getHierarchies(hierarchy, hierarchies);

			
			int count = hierarchies.size();
			for (String name : hierarchies)
				hierarchiesMap.put(("" + count--), name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return hierarchiesMap;
	}

	private List<String> getHierarchies(HpcDataHierarchy hierarchy, List<String> hierarchies) {
		try {
			if (hierarchy == null)
				return hierarchies;
			if (hierarchy.getCollectionType() != null)
				hierarchies.add(hierarchy.getCollectionType());
			List<HpcDataHierarchy> subHierarchies = hierarchy.getSubCollectionsHierarchies();
			if (subHierarchies == null || subHierarchies.isEmpty())
				return hierarchies;
			else {
				for (HpcDataHierarchy sub : subHierarchies)
					getHierarchies(sub, hierarchies);
			}
			return hierarchies;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
