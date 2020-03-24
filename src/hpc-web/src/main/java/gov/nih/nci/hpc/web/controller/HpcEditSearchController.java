/**
 * HpcEditSearchController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonParseException;

import gov.nih.nci.hpc.domain.datamanagement.HpcDataHierarchy;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataLevelAttributes;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDocDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMetadataAttributesListDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.security.HpcGroup;
import gov.nih.nci.hpc.dto.security.HpcGroupListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcCompoundQuery;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcMetadataHierarchy;
import gov.nih.nci.hpc.web.model.HpcSaveSearch;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcCompoundSearchParser;
import gov.nih.nci.hpc.web.util.HpcSearchUtil;

/**
 * <p>
 * Controller to execute a search search
 * </p>
 *
 * @author <a href="mailto:Yuri.Dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/editSearch")
public class HpcEditSearchController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.query}")
	private String queryURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String modelServiceURL;
	@Value("${gov.nih.nci.hpc.server.metadataattributes}")
	private String hpcMetadataAttrsURL;
	@Value("${gov.nih.nci.hpc.server.user.group}")
    private String userGroupServiceURL;
	
	/**
	 * GET action to edit a saved search
	 * 
	 * @param body
	 * @param queryName
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String body, @RequestParam String queryName, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
		HpcNamedCompoundMetadataQueryDTO query = null;
		HpcSearch hpcSearch = new HpcSearch();
		HpcSaveSearch hpcSaveSearch = new HpcSaveSearch();

		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String userId = (String) session.getAttribute("hpcUserId");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/login";
		}

		try {

			hpcSearch.setQueryName(queryName);
			hpcSaveSearch.setCriteriaName(queryName);
			query = processSearch(hpcSearch, session, request, model, bindingResult);
			if (query == null)
				return "dashboard";

			// Re-populate hpcSearch based on compound query
			hpcSearch = populateSearchCriteria(query.getNamedCompoundQuery().getCompoundQuery());
			if(query.getNamedCompoundQuery().getCompoundQueryType().value().equalsIgnoreCase("collection"))
				hpcSearch.setSearchType("collection");
			else
				hpcSearch.setSearchType("datafile");
				
			hpcSearch.setDetailed(query.getNamedCompoundQuery().getDetailedResponse());

			model.addAttribute("hpcSearch", hpcSearch);
			model.addAttribute("hpcSaveSearch", hpcSaveSearch);
			session.setAttribute("hpcSearch", hpcSearch);
			session.setAttribute("hpcSaveSearch", hpcSaveSearch);
			session.setAttribute("compoundQuery", query);

			populateHierarchy(session, model, authToken, user);
			populateMetadata(model, authToken, user, "collection", session);
			populateOperators(model);
			populateLevelOperators(model);
			checkSecGroup(userId, session, model, authToken);
			return "criteria";

		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		} catch (HttpStatusCodeException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		} catch (RestClientException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		} catch (Exception e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		}

	}

	/**
	 * POST action to edit a search criteria
	 * 
	 * @param body
	 * @param queryName
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String execute(@Valid @ModelAttribute("hpcSearch") HpcSearch search, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			RedirectAttributes redirectAttrs) {

		HpcSaveSearch hpcSaveSearch = (HpcSaveSearch) session.getAttribute("hpcSaveSearch");
		if(hpcSaveSearch == null)
			hpcSaveSearch = new HpcSaveSearch();
		HpcNamedCompoundMetadataQueryDTO query = null;
		HpcSearch hpcSearch = (HpcSearch) session.getAttribute("hpcSearch");

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

		try {
			if (StringUtils.isNotBlank(hpcSearch.getQueryName()) && hpcSearch.getAttrValue() == null) {
				String queryName = hpcSearch.getQueryName();
				query = processSearch(hpcSearch, session, request, model, bindingResult);
				if (query != null) {
					// Re-populate hpcSearch based on compound query
					hpcSearch = populateSearchCriteria(query.getNamedCompoundQuery().getCompoundQuery());
					if(query.getNamedCompoundQuery().getCompoundQueryType().value().equalsIgnoreCase("collection"))
						hpcSearch.setSearchType("collection");
					else
						hpcSearch.setSearchType("datafile");
					hpcSearch.setDetailed(query.getNamedCompoundQuery().getDetailedResponse());
					hpcSaveSearch.setCriteriaName(queryName);
				}
			}
			model.addAttribute("hpcSearch", hpcSearch);
			model.addAttribute("hpcSaveSearch", hpcSaveSearch);
			session.setAttribute("hpcSearch", hpcSearch);
			session.setAttribute("hpcSaveSearch", hpcSaveSearch);
			session.setAttribute("compoundQuery", query);

			HpcSearchUtil.clearCachedSelectedRows(session);
			populateHierarchy(session, model, authToken, user);
			populateMetadata(model, authToken, user, "collection", session);
			populateOperators(model);
			populateLevelOperators(model);
			checkSecGroup(userId, session, model, authToken);
			return "criteria";

		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		} catch (HttpStatusCodeException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		} catch (RestClientException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		} catch (Exception e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		}
	}

	private HpcNamedCompoundMetadataQueryDTO processSearch(HpcSearch search, HttpSession session,
			HttpServletRequest request, Model model, BindingResult bindingResult)
			throws JsonParseException, IOException {
		HpcNamedCompoundMetadataQueryDTO query = null;
		String authToken = (String) session.getAttribute("hpcUserToken");

		query = HpcClientUtil.getQuery(authToken, queryURL, search.getQueryName(), sslCertPath, sslCertPassword);

		return query;
	}

	private HpcSearch populateSearchCriteria(HpcCompoundMetadataQuery compoundMetadataQuery) {
		HpcSearch hpcSearch = new HpcSearch();
		HpcCompoundQuery compoundQuery = new HpcCompoundSearchParser(compoundMetadataQuery).parse();
		List<String> rowIdList = new ArrayList<>();
		List<String> levelList = new ArrayList<>();
		List<String> attrList = new ArrayList<>();
		List<String> operatorList = new ArrayList<>();
		List<String> attrValueList = new ArrayList<>();
		List<Boolean> selfAttributeOnlyList = new ArrayList<>();
		List<Boolean> attributeEncryptedList = new ArrayList<>();
		for (Map.Entry<String, HpcMetadataQuery> query : compoundQuery.getQueries().entrySet()) {
			rowIdList.add(query.getKey());
			if(query.getValue().getLevelFilter() != null) {
			  if(StringUtils.isNotBlank(query.getValue().getLevelFilter().getLabel())) {
			    levelList.add(query.getValue().getLevelFilter().getLabel());
			    selfAttributeOnlyList.add(false);
			  }
			  else {
			    levelList.add("ANY");
			    if(query.getValue().getLevelFilter().getLevel() != null && query.getValue().getLevelFilter().getLevel() == 1)
			      selfAttributeOnlyList.add(query.getValue().getLevelFilter().getOperator().equals(HpcMetadataQueryOperator.EQUAL));
			    else
			      selfAttributeOnlyList.add(false);
			  }
			}
			attrList.add(query.getValue().getAttribute());
			operatorList.add(query.getValue().getOperator().value());
			attrValueList.add(query.getValue().getValue());
			attributeEncryptedList.add(false);
		}
		hpcSearch.setRowId(rowIdList.toArray(new String[rowIdList.size()]));
		hpcSearch.setLevel(levelList.toArray(new String[levelList.size()]));
		hpcSearch.setAttrName(attrList.toArray(new String[attrList.size()]));
		hpcSearch.setOperator(operatorList.toArray(new String[operatorList.size()]));
		hpcSearch.setAttrValue(attrValueList.toArray(new String[attrValueList.size()]));
		hpcSearch.setSelfAttributeOnly(ArrayUtils.toPrimitive(selfAttributeOnlyList.toArray(new Boolean[attrValueList.size()])));
		hpcSearch.setEncrypted(ArrayUtils.toPrimitive(attributeEncryptedList.toArray(new Boolean[attrValueList.size()])));
		if (compoundQuery.getCriteria().contains("OR"))
			hpcSearch.setAdvancedCriteria(compoundQuery.getCriteria());
		return hpcSearch;
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
        groups =
            HpcClientUtil.getUserGroup(
                authToken, userGroupServiceURL, sslCertPath, sslCertPassword);
        session.setAttribute("hpcSecGroup", groups);
      }
      boolean userInSecGroup = false;
      if (groups != null && CollectionUtils.isNotEmpty(groups.getGroups())) {
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
