/**
 * HpcSearchCriteriaController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import gov.nih.nci.hpc.domain.datamanagement.HpcDataHierarchy;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataLevelAttributes;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryAttributeMatch;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryLevelFilter;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDocDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMetadataAttributesListDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.security.HpcGroup;
import gov.nih.nci.hpc.dto.security.HpcGroupListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcCompoundQuery;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcMetadataHierarchy;
import gov.nih.nci.hpc.web.model.HpcSaveSearch;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcCompoundSearchBuilder;
import gov.nih.nci.hpc.web.util.HpcEncryptionUtil;
import gov.nih.nci.hpc.web.util.HpcSearchUtil;

/**
 * <p>
 * Controller to search collections or data file based on search criteria input
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/criteria")
public class HpcSearchCriteriaController extends AbstractHpcController {
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
	@Value("${gov.nih.nci.hpc.server.user.group}")
    private String userGroupServiceURL;
	
	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	/**
	 * GET action to display criteria page. Populate levels, metadata attributes
	 * and operators to display search page
	 * 
	 * @param q
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		HpcSearch hpcSearch = new HpcSearch();
		HpcSaveSearch hpcSaveSearch = new HpcSaveSearch();
		hpcSearch.setSearchType("collection");
		model.addAttribute("hpcSearch", hpcSearch);
		model.addAttribute("hpcSaveSearch", hpcSaveSearch);
		session.removeAttribute("compoundQuery");
		session.removeAttribute("hpcSearch");
		session.removeAttribute("hpcSaveSearch");
		session.removeAttribute("namedCompoundQuery");
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String userId = (String) session.getAttribute("hpcUserId");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/login?returnPath=criteria";
		}
		
		
		HpcSearchUtil.clearCachedSelectedRows(session);
		logger.info("Getting search criteria for user: " + user.getFirstName() + " " + user.getLastName());
		populateHierarchy(session, model, authToken, user);
		populateMetadata(model, authToken, user, "collection", session);
		populateOperators(model);
		populateLevelOperators(model);
		checkSecGroup(userId, session, model, authToken);
		return "criteria";
	}

	/**
	 * POST action to search for collections or data files by given criteria.
	 * Build simple or complex search criteria based on given input. Parse
	 * complex search equation and build compound search criteria.
	 * 
	 * @param search
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param redirectAttrs
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String search(@Valid @ModelAttribute("hpcSearch") HpcSearch search, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request, RedirectAttributes redirectAttrs) {
		if (search.getActionType() != null && search.getActionType().equals("refresh")) {
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			String userId = (String) session.getAttribute("hpcUserId");
			String authToken = (String) session.getAttribute("hpcUserToken");
			populateHierarchy(session, model, authToken, user);
			populateMetadata(model, authToken, user, search.getSearchType(), session);
			populateOperators(model);
			populateLevelOperators(model);
			checkSecGroup(userId, session, model, authToken);
			return "criteria";
		}

		HpcSearch hpcSearch = null;

		if (search == null || (search.getActionType() != null && search.getActionType().equals("pagination"))) {
			HpcSearchUtil.cacheSelectedRows(session, request, model);
			hpcSearch = (HpcSearch) session.getAttribute("hpcSearch");
			hpcSearch.setPageNumber(search.getPageNumber());
			hpcSearch.setPageSize(search.getPageSize());
			hpcSearch.setTotalSize(search.getTotalSize());
			search = hpcSearch;
		}

		model.addAttribute("source", "criteria");
		model.addAttribute("pageNumber", search.getPageNumber());
		model.addAttribute("pageSize", search.getPageSize());
		model.addAttribute("totalSize", search.getTotalSize());
		boolean success = false;
		try {

			@SuppressWarnings("unchecked")
			Map<String, String> hierarchy = (Map<String, String>) session.getAttribute("hierarchies");

			HpcCompoundMetadataQueryDTO compoundQuery = constructCriteria(hierarchy,
					hpcSearch != null ? hpcSearch : search);
			if (search.isDetailed())
				compoundQuery.setDetailedResponse(true);

			String authToken = (String) session.getAttribute("hpcUserToken");
			String serviceURL = compoundDataObjectSearchServiceURL;
			if (search.getSearchType() != null && search.getSearchType().equals("collection"))
				serviceURL = compoundCollectionSearchServiceURL;

			WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);

			Response restResponse = client.invoke("POST", compoundQuery);
			if (restResponse.getStatus() == 200) {
				HpcSearchUtil.processResponseResults(hpcSearch != null ? hpcSearch : search, restResponse, model, session);
				success = true;
				session.setAttribute("compoundQuery", compoundQuery);
				session.setAttribute("hpcSearch", hpcSearch != null ? hpcSearch : search);
			} else {
				String message = "No matching results!";
				ObjectError error = new ObjectError("hpcSearch", message);
				bindingResult.addError(error);
				model.addAttribute("error", message);
				return "criteria";
			}
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			log.error(e.getMessage(), e);
			ObjectError error = new ObjectError("hpcLogin", "Failed to project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "criteria";
		} catch (HttpStatusCodeException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "criteria";
		} catch (RestClientException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "criteria";
		} catch (Exception e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "criteria";
		} finally {
			if (!success) {
				HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
				String authToken = (String) session.getAttribute("hpcUserToken");
				String userId = (String) session.getAttribute("hpcUserId");
				model.addAttribute("hpcSearch", search);
				populateHierarchy(session, model, authToken, user);
				populateMetadata(model, authToken, user, search.getSearchType(), session);
				populateOperators(model);
				populateLevelOperators(model);
				checkSecGroup(userId, session, model, authToken);
			}
		}

		if (search.getSearchType().equals("collection"))
			return search.isDetailed() ? "collectionsearchresultdetail" : "collectionsearchresult";
		else
			return search.isDetailed() ? "dataobjectsearchresultdetail" : "dataobjectsearchresult";
	}

	@RequestMapping(value = "/export", method = RequestMethod.POST)
	@SuppressWarnings("unchecked")
	public String export(@Valid @ModelAttribute("hpcSearch") HpcSearch search, Model model, HttpSession session,
			HttpServletRequest request, HttpServletResponse response) {

		HpcSearch hpcSearch = null;
		if (search == null || (search.getActionType() != null && search.getActionType().equals("pagination"))) {
			HpcSearchUtil.cacheSelectedRows(session, request, model);
			hpcSearch = (HpcSearch) session.getAttribute("hpcSearch");
			hpcSearch.setPageNumber(search.getPageNumber());
			hpcSearch.setPageSize(search.getPageSize());
			search = hpcSearch;
		}
		try {
			session.removeAttribute("searchresults");
			Map<String, String> hierarchy = (Map<String, String>) session.getAttribute("hierarchies");

			HpcCompoundMetadataQueryDTO compoundQuery = constructCriteria(hierarchy,
					hpcSearch != null ? hpcSearch : search);
			if (search.isDetailed())
				compoundQuery.setDetailedResponse(true);

			String authToken = (String) session.getAttribute("hpcUserToken");
			String serviceURL = compoundDataObjectSearchServiceURL;
			if (search.getSearchType() != null && search.getSearchType().equals("collection"))
				serviceURL = compoundCollectionSearchServiceURL;

			WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);

			HpcSearch exportSearch = hpcSearch != null ? hpcSearch : search;
			int pageNumber = 1;
			int totalPages = 1;
			do {
				compoundQuery.setPage(pageNumber++);
				compoundQuery.setPageSize(100);
				Response restResponse = client.invoke("POST", compoundQuery);
				if (restResponse.getStatus() == 200) {
					HpcSearchUtil.processResponseResults(hpcSearch != null ? hpcSearch : search, restResponse, model, session);
					totalPages = (int) session.getAttribute("totalPages");
				}
			} while (pageNumber <= totalPages);
			HpcSearchUtil.exportResponseResults(exportSearch.getSearchType(), session, request, response);
			
		} catch (Exception e) {
			e.printStackTrace();
			return "forward:/criteria";
		}
		return null;
	}
	
	private HpcCompoundMetadataQueryDTO constructCriteria(Map<String, String> hierarchy, HpcSearch search) {
		HpcCompoundMetadataQueryDTO dto = new HpcCompoundMetadataQueryDTO();
		dto.setTotalCount(true);
		HpcCompoundMetadataQuery query = null;
		if (search.getAdvancedCriteria() != null && !search.getAdvancedCriteria().isEmpty())
			query = buildAdvancedSearch(hierarchy, search);
		else
			query = buildSimpleSearch(hierarchy, search);

		dto.setCompoundQuery(query);
		dto.setDetailedResponse(search.isDetailed());
		if (search.getSearchType().equals("collection"))
			dto.setCompoundQueryType(HpcCompoundMetadataQueryType.COLLECTION);
		else
			dto.setCompoundQueryType(HpcCompoundMetadataQueryType.DATA_OBJECT);
		dto.setDetailedResponse(search.isDetailed());
		dto.setPage(search.getPageNumber());
		dto.setPageSize(search.getPageSize());
		return dto;
	}

	private HpcCompoundMetadataQuery buildSimpleSearch(Map<String, String> hierarchy, HpcSearch search) {
		HpcCompoundMetadataQuery query = new HpcCompoundMetadataQuery();
		query.setOperator(HpcCompoundMetadataQueryOperator.AND);
		Map<String, HpcMetadataQuery> queriesMap = getQueries(hierarchy, search);
		List<HpcMetadataQuery> queries = new ArrayList<HpcMetadataQuery>();
		Iterator<String> iter = queriesMap.keySet().iterator();
		while (iter.hasNext())
			queries.add(queriesMap.get(iter.next()));

		query.getQueries().addAll(queries);
		return query;
	}

	private Map<String, HpcMetadataQuery> getQueries(Map<String, String> hierarchy, HpcSearch search) {
		Map<String, HpcMetadataQuery> queries = new HashMap<String, HpcMetadataQuery>();
		for (int i = 0; i < search.getAttrName().length; i++) {
			String rowId = search.getRowId()[i];
			String attrName = search.getAttrName()[i];
			String attrValue = search.getAttrValue()[i];
			String operator = search.getOperator()[i];
			boolean selfOnly = search.getSelfAttributeOnly()[i];
			boolean encrypted = (search.getEncrypted() != null && search.getEncrypted().length > 0 ? search.getEncrypted()[i] : false);
			if(encrypted) {
			    try {
			      attrValue = Base64.getEncoder().encodeToString(HpcEncryptionUtil.encrypt(search.getUserKey(), attrValue));
                } catch (Exception e) {
                  // To user, it will indicate no results found.
                  logger.error("Search with encrypted field failed due to ", e);
                } 
			}
			String level = search.getLevel()[i];
			if (!attrName.isEmpty() && !attrValue.isEmpty() && !operator.isEmpty()) {
				HpcMetadataQuery criteria = new HpcMetadataQuery();
				if (attrName.equals("-1") || attrName.equals("ANY"))
					criteria.setAttributeMatch(HpcMetadataQueryAttributeMatch.ANY);
				else
					criteria.setAttribute(attrName);
				criteria.setValue(attrValue);
				criteria.setOperator(HpcMetadataQueryOperator.fromValue(operator));
				//If its a timestamp operator, specify the format
				if (operator.startsWith("TIMESTAMP_GREATER")) {
					criteria.setValue(attrValue.concat(" 00:00:00").replace("/", "-"));
					criteria.setFormat("MM-DD-YYYY HH24:MI:SS");
				}
				if (operator.startsWith("TIMESTAMP_LESS")) {
					criteria.setValue(attrValue.concat(" 23:59:59").replace("/", "-"));
					criteria.setFormat("MM-DD-YYYY HH24:MI:SS");
				}
				if (level != null) {
					HpcMetadataQueryLevelFilter levelFilter = new HpcMetadataQueryLevelFilter();
					if (selfOnly) {
					    levelFilter.setLevel(1);
					    levelFilter.setOperator(HpcMetadataQueryOperator.EQUAL);
					}
					else if (level.equals("ANY")) {
						levelFilter.setLevel(1);
						levelFilter.setOperator(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL);
					} else {
						if (level.equals("Data file") || level.equals("DataObject"))
							levelFilter.setLevel(1);
						else
							levelFilter.setLabel(level);
						levelFilter.setOperator(HpcMetadataQueryOperator.EQUAL);
					}
					criteria.setLevelFilter(levelFilter);
				}
				queries.put(rowId, criteria);
			}
		}
		return queries;
	}

	private HpcCompoundMetadataQuery buildAdvancedSearch(Map<String, String> hierarchy, HpcSearch search) {
		Map<String, HpcMetadataQuery> queries = getQueries(hierarchy, search);
		HpcCompoundQuery compoundQuery = new HpcCompoundQuery();
		compoundQuery.setQueries(queries);
		compoundQuery.setCriteria(search.getAdvancedCriteria());
		return new HpcCompoundSearchBuilder(compoundQuery).build();
	}

	private String getAttributeValue(String attrName, HpcMetadataEntries entries) {
		if (entries == null)
			return null;

		List<HpcMetadataEntry> selfEntries = entries.getSelfMetadataEntries();
		for (HpcMetadataEntry entry : selfEntries) {
			if (StringUtils.equals(entry.getAttribute(), attrName))
				return entry.getValue();
		}
		List<HpcMetadataEntry> parentEntries = entries.getParentMetadataEntries();
		for (HpcMetadataEntry entry : parentEntries) {
			if (StringUtils.equals(entry.getAttribute(), attrName))
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
		entries.put("NUM_GREATER_THAN", ">");
		entries.put("NUM_GREATER_OR_EQUAL", ">=");
		entries.put("LIKE", "LIKE");
		entries.put("TIMESTAMP_LESS_OR_EQUAL", "Date less than or equal to");
		entries.put("TIMESTAMP_GREATER_OR_EQUAL", "Date greater than or equal to");
		Map<String, String> sortedEntries = new TreeMap<String, String>(entries);
		model.addAttribute("operators", sortedEntries);
	}

	private void populateLevelOperators(Model model) {
		Map<String, String> entries = new HashMap<String, String>();
		entries.put("EQUAL", "=");
		entries.put("NOT_EQUAL", "!=");
		entries.put("NUM_LESS_THAN", "<");
		entries.put("NUM_LESS_OR_EQUAL", "<=");
		entries.put("NUM_GREATER_THAN", ">");
		entries.put("NUM_GREATER_OR_EQUAL", ">=");
		entries.put("LIKE", "LIKE");
		entries.put("TIMESTAMP_LESS_OR_EQUAL", "Date less than or equal to");
		entries.put("TIMESTAMP_GREATER_OR_EQUAL", "Date greater than or equal to");
		Map<String, String> sortedEntries = new TreeMap<String, String>(entries);
		model.addAttribute("leveloperators", sortedEntries);
	}

	private void populateMetadata(Model model, String authToken, HpcUserDTO user, String type, HttpSession session) {
		model.addAttribute("doc", user.getDoc());
		if (session.getAttribute("hierarchy") != null) {
			model.addAttribute("hierarchy", session.getAttribute("hierarchy"));
			return;
		}

		HpcMetadataHierarchy dataHierarchy = new HpcMetadataHierarchy();
		List<String> collectionLevels = new ArrayList<String>();
		List<String> dataobjectLevels = new ArrayList<String>();

		List<String> attrs = new ArrayList<String>();
		HpcMetadataAttributesListDTO dto = HpcClientUtil.getMetadataAttrNames(authToken, hpcMetadataAttrsURL,
				sslCertPath, sslCertPassword);
		
		if (dto != null && dto.getCollectionMetadataAttributes() != null) {
			for (HpcMetadataLevelAttributes levelAttrs : dto.getCollectionMetadataAttributes()) {
				String label = levelAttrs.getLevelLabel();
				if (label == null)
					continue;
				// label = "Data file";
				collectionLevels.add(label);
				dataHierarchy.getCollectionAttrsSet().addAll(levelAttrs.getMetadataAttributes());
				for (String name : levelAttrs.getMetadataAttributes())
					attrs.add(label + ":collection:" + name);
			}
		}

		if (dto != null && dto.getDataObjectMetadataAttributes() != null) {
			for (HpcMetadataLevelAttributes levelAttrs : dto.getDataObjectMetadataAttributes()) {
				String label = levelAttrs.getLevelLabel();
				if (label == null)
					continue;

				dataobjectLevels.add(label);
				dataHierarchy.getDataobjectAttrsSet().addAll(levelAttrs.getMetadataAttributes());
				for (String name : levelAttrs.getMetadataAttributes())
					attrs.add(label + ":datafile:" + name);
			}
		}

		dataHierarchy.setCollectionLevels(collectionLevels);
		dataHierarchy.setDataobjectLevels(dataobjectLevels);
		dataHierarchy.setAllAttributes(attrs);
		model.addAttribute("hierarchy", dataHierarchy);
		session.setAttribute("hierarchy", dataHierarchy);
	}

	private void populateHierarchy(HttpSession session, Model model, String authToken, HpcUserDTO user) {
		try {
			if (session.getAttribute("hierarchies") == null)
				session.setAttribute("hierarchies", getHierarchy(authToken, user, session));
			model.addAttribute("doc", user.getDoc());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Map<String, String> getHierarchy(String authToken, HpcUserDTO user, HttpSession session) {
		Map<String, String> hierarchiesMap = new HashMap<String, String>();
		try {
			HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
			if (modelDTO == null) {	
				modelDTO = HpcClientUtil.getDOCModel(authToken, modelServiceURL, sslCertPath, sslCertPassword);
				session.setAttribute("userDOCModel", modelDTO);
			}

			List<HpcDocDataManagementRulesDTO> docRules = modelDTO.getDocRules();
			List<String> hierarchies = new ArrayList<String>();

			for (HpcDocDataManagementRulesDTO docDto : docRules) {
				if (docDto.getDoc().equals(user.getDoc())) {
					for (HpcDataManagementRulesDTO rulesDto : docDto.getRules())
						getHierarchies(rulesDto.getDataHierarchy(), hierarchies);
				}
			}
			int count = 1;
			for (String name : hierarchies)
				hierarchiesMap.put(name, ("" + count++));
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
	
	private void checkSecGroup(String userId, HttpSession session, Model model, String authToken) {
	//Find out if user belongs to any SEC_GROUP
      HpcGroupListDTO groups = (HpcGroupListDTO) session.getAttribute("hpcSecGroup");
      if (groups == null) {
          groups = HpcClientUtil.getUserGroup(authToken, userGroupServiceURL, sslCertPath, sslCertPassword);
          session.setAttribute("hpcSecGroup", groups);
      }
      boolean userInSecGroup = false;
      if(groups != null && CollectionUtils.isNotEmpty(groups.getGroups())) {
        for (HpcGroup group : groups.getGroups()) {
          if (group.getGroupName().contains("_SEC_GROUP")) {
            userInSecGroup = true;
            break;
          }
        }
      }
      model.addAttribute("userInSecGroup", userInSecGroup);
	}

}
