/**
 * HpcDatafileController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import com.google.gson.Gson;

import gov.nih.nci.hpc.domain.datamanagement.HpcMetadataUpdateItem;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermissionForCollection;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkMetadataUpdateRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkMetadataUpdateResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDocDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermsForCollectionsDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcBulkMetadataUpdateRequest;
import gov.nih.nci.hpc.web.model.HpcDownloadDatafile;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcSearchUtil;


/**
 * <p>
 * Controller to manage bulk metadata updates
 * </p>
 *
 * @author <a href="mailto:Sarada.Chintala@nih.gov">Sarada Chintala</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
public class HpcBulkMetadataController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.user}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.metadata}")
	private String serviceMetadataURL;
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String serverCollectionURL;
	@Value("${gov.nih.nci.hpc.server.dataObject}")
	private String serverDataObjectURL;
	@Value("${hpc.serviceaccount}")
	private String serviceAccount;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;
	@Value("${gov.nih.nci.hpc.server.childCollections.acl.user}")
	private String childCollectionsAclURL;

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	private Gson gson = new Gson();
	private String failureErrorMessage = "Error: Unable to update bulk metadata. ";
	/**
	 * POST action to render bulk meta data update page
	 * 
	 * @param body
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@PostMapping(value = "/bulkupdatemetadata")
	public String home(@RequestBody(required = false) String body, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		// Check if session valid
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (user == null || authToken == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "dashboard";
		}

		// Get metadata attributes from session
		List<String> metadataAttributesList  = getAllUserMetadataAttributes(authToken, session);
		model.addAttribute("metadataAttributesList", metadataAttributesList);
		logger.info("metadataAttributesList size ="+ gson.toJson(metadataAttributesList.size()));

		// Set paths to be displayed on the updatemetadatabulk page
		String selectedPathsStr = request.getParameter("selectedFilePaths");
		String globalMetadataSearchText = request.getParameter("globalMetadataSearchText");
		selectedPathsStr = selectedPathsStr.replaceAll("\\[", "").replaceAll("\\]","");
		logger.info("selectedPathsStr ="+ gson.toJson(selectedPathsStr));
		logger.debug("globalMetadataSearchText ="+ globalMetadataSearchText);
		String updateAll = request.getParameter("updateAll");
		String totalCount = request.getParameter("totalCount");
		List<HpcPathGridEntry> pathDetails = new ArrayList<>();
		HpcBulkMetadataUpdateRequest bulkMetadataUpdateRequest = new HpcBulkMetadataUpdateRequest();
		HpcDownloadDatafile hpcDownloadDatafile = new HpcDownloadDatafile();
		if(StringUtils.equals(updateAll, "true")) {
			model.addAttribute("updateAll", "true");
			model.addAttribute("totalCount", totalCount);
		}
		else if (selectedPathsStr.isEmpty()) {
			model.addAttribute("error", "Data file list is missing!");
		} else {
			StringTokenizer tokens = new StringTokenizer(selectedPathsStr, ",");
			List<String> paths = new ArrayList<>();
			while (tokens.hasMoreTokens()) {
				String pathStr = tokens.nextToken();
				String path = pathStr.substring(pathStr.lastIndexOf(":") + 1);
				paths.add(path);
				hpcDownloadDatafile.getSelectedPaths().add(path);
				HpcPathGridEntry pathGridEntry = new HpcPathGridEntry();
				pathGridEntry.path = path;
				pathGridEntry.result = "";
				pathDetails.add(pathGridEntry);
			}
			model.addAttribute("pathDetails", pathDetails);
			bulkMetadataUpdateRequest.setSelectedFilePaths(selectedPathsStr); //TODO
		}
		model.addAttribute("globalMetadataSearchText", globalMetadataSearchText);
		bulkMetadataUpdateRequest.setGlobalMetadataSearchText(globalMetadataSearchText);
		model.addAttribute("bulkMetadataUpdateRequest", bulkMetadataUpdateRequest);

		// Set all session and model attributes to retain data/state of the previous page(collectionsearchresult.html or collectionsearchresultdetail.html)
		HpcSearchUtil.cacheSelectedRows(session, request, model);
		String downloadType = request.getParameter("downloadType");
		hpcDownloadDatafile.setDownloadType(downloadType);
		model.addAttribute("downloadType", downloadType);
		model.addAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
		session.setAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
		HpcSearch hpcSaveSearch = new HpcSearch();
		String pageNumber = request.getParameter("pageNumber");
		hpcSaveSearch.setPageNumber(pageNumber != null ? Integer.parseInt(pageNumber) : 1);
		String pageSize = request.getParameter("pageSize");
		if (StringUtils.isNotBlank(pageSize))
			hpcSaveSearch.setPageSize(Integer.parseInt(pageSize));
		hpcSaveSearch.setQueryName(request.getParameter("queryName"));
		hpcSaveSearch.setSearchType(request.getParameter("searchType"));
		String[] deselectedColumns =  request.getParameterValues("deselectedColumns");
		if(deselectedColumns != null && StringUtils.isNotEmpty(deselectedColumns[0]))
			hpcSaveSearch.getDeselectedColumns().addAll((Collection<? extends String>) org.springframework.util.CollectionUtils.arrayToList(deselectedColumns[0].split(",")));
		hpcSaveSearch.setTotalSize(StringUtils.isNotBlank(request.getParameter("totalSize")) ? Long.parseLong(request.getParameter("totalSize")) : 0);
		model.addAttribute("hpcSearch", hpcSaveSearch);
		session.setAttribute("hpcSavedSearch", hpcSaveSearch);
		model.addAttribute("result", "false");
		model.addAttribute("errorStatusMessage", "");
		return "updatemetadatabulk";
	}


	/**
	 * POST action to add/update metadata in bulk(multiple paths). On update, redirect
	 * back to bulk permissions page with results
	 * 
	 * @param bulkMetadataUpdateRequest
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param redirectAttrs
	 * @return
	 */
	@PostMapping(value = "/assignbulkmetadata")
	public String setBulkMetadata(@Valid @ModelAttribute("bulkMetadataUpdateRequest") HpcBulkMetadataUpdateRequest bulkMetadataUpdateRequest, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			RedirectAttributes redirectAttrs) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		List<HpcPathGridEntry> pathDetails = new ArrayList<>();
		String downloadType = request.getParameter("downloadType");
		HpcDownloadDatafile hpcDownloadDatafile = new HpcDownloadDatafile();
		hpcDownloadDatafile.setDownloadType(downloadType);
		model.addAttribute("downloadType", downloadType);
		model.addAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
		session.setAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
		logger.info("Enter /assignbulkmetadata bulkMetadataUpdateRequest= " + gson.toJson(bulkMetadataUpdateRequest));
		String globalMetadataSearchText = bulkMetadataUpdateRequest.getGlobalMetadataSearchText();
		model.addAttribute("globalMetadataSearchText", globalMetadataSearchText);
		
		List<HpcMetadataEntry> userMetadataList = new ArrayList<HpcMetadataEntry>();
		Enumeration<String> params = request.getParameterNames();
		HpcMetadataEntry metadataEntry = new HpcMetadataEntry();
		metadataEntry.setAttribute(bulkMetadataUpdateRequest.getMetadataName());
		metadataEntry.setValue(bulkMetadataUpdateRequest.getMetadataValue());
		userMetadataList.add(metadataEntry);
		while (params.hasMoreElements()) {
			String paramName = params.nextElement();
			if (paramName.startsWith("addAttrName")) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				String attrId = paramName.substring("addAttrName".length());
				String[] attrName = request.getParameterValues(paramName);
				String[] attrValue = request.getParameterValues("addAttrValue" + attrId);
				if (attrName.length > 0 && !attrName[0].isEmpty()) {
					entry.setAttribute(attrName[0]);
					if (attrValue.length > 0 && !attrValue[0].isEmpty()) {
						entry.setValue(attrValue[0]);
					} else {
						entry.setValue("");
					}
				} else {
					if (attrValue.length > 0 && !attrValue[0].isEmpty()) {
						throw new HpcWebException("Invalid metadata attribute name for value " + attrValue[0] + ": Name cannot be empty");
					} else {
					//If both attrName and attrValue are empty, then we just
					//ignore it and move to the next element
					continue;
					}
				}
				userMetadataList.add(entry);
			}
		}
		model.addAttribute("userMetadataList", userMetadataList);
		model.addAttribute("metadataName", bulkMetadataUpdateRequest.getMetadataName());
		model.addAttribute("metadataValue", bulkMetadataUpdateRequest.getMetadataValue());

		HpcSearch hpcSaveSearch = (HpcSearch) session.getAttribute("hpcSavedSearch");
		model.addAttribute("hpcSearch", hpcSaveSearch);
		String result = "";
		HpcBulkMetadataUpdateRequestDTO req =  new HpcBulkMetadataUpdateRequestDTO();
		String updateAll = request.getParameter("updateAll");
		String totalCount = request.getParameter("totalCount");
		if(StringUtils.equals(updateAll, "true")) {
			HpcCompoundMetadataQueryDTO compoundQuery = null;
			if (session.getAttribute("compoundQuery") != null)
				compoundQuery = (HpcCompoundMetadataQueryDTO) session.getAttribute("compoundQuery");
			if (compoundQuery == null) {
				HpcNamedCompoundMetadataQuery namedCompoundQuery = null;
				if (session.getAttribute("namedCompoundQuery") != null)
					namedCompoundQuery = (HpcNamedCompoundMetadataQuery) session.getAttribute("namedCompoundQuery");
				compoundQuery = new HpcCompoundMetadataQueryDTO();
				compoundQuery.setCompoundQuery(namedCompoundQuery.getCompoundQuery());
			}
			model.addAttribute("updateAll", "true");
			model.addAttribute("totalCount", totalCount);
			req.setCollectionCompoundQuery(compoundQuery.getCompoundQuery());
		} else {
			List<String> paths = new ArrayList<>();
			String selectedPathsStr = bulkMetadataUpdateRequest.getSelectedFilePaths();
			StringTokenizer tokens = new StringTokenizer(selectedPathsStr, ",");
			while (tokens.hasMoreTokens()) {
				String pathStr = tokens.nextToken();
				String path = pathStr.substring(pathStr.lastIndexOf(":") + 1);
				paths.add(path);
			}
			bulkMetadataUpdateRequest.setSelectedFilePaths(selectedPathsStr);
			req.getCollectionPaths().addAll(paths);
		}
		req.getMetadataEntries().addAll(userMetadataList);
		try {
			WebClient client = HpcClientUtil.getWebClient(serviceMetadataURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);
			logger.info("Bulk metadata update request is="+gson.toJson(req));
			Response restResponse = client.invoke("POST", req);
			if (restResponse.getStatus() == 200) {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JakartaXmlBindAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcBulkMetadataUpdateResponseDTO bulkUpdateResponseDTO = parser.readValueAs(HpcBulkMetadataUpdateResponseDTO.class);
				List<HpcMetadataUpdateItem> completedItems =  bulkUpdateResponseDTO.getCompletedItems();
				List<HpcMetadataUpdateItem> failedItems =  bulkUpdateResponseDTO.getFailedItems();
				logger.info("The Bulk Update Response is: " + gson.toJson(bulkUpdateResponseDTO));
				for (HpcMetadataUpdateItem item : bulkUpdateResponseDTO.getCompletedItems()) {
					HpcPathGridEntry pathGridEntry = new HpcPathGridEntry();
					pathGridEntry.path = item.getPath();
					pathGridEntry.result = "Success";
					pathDetails.add(pathGridEntry);
				}
				for (HpcMetadataUpdateItem item : bulkUpdateResponseDTO.getFailedItems()) {
					HpcPathGridEntry pathGridEntry = new HpcPathGridEntry();
					pathGridEntry.path = item.getPath();
					pathGridEntry.result = item.getMessage();
					pathDetails.add(pathGridEntry);
				}
				model.addAttribute("pathDetails", pathDetails);
				model.addAttribute("errorStatusMessage", "");
			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JakartaXmlBindAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				model.addAttribute("errorStatusMessage", failureErrorMessage + exception.getMessage());
				logger.info("Failed to update metadata for: ", failureErrorMessage + exception.getMessage());
			}
		} catch (HttpStatusCodeException e) {
			model.addAttribute("errorStatusMessage", failureErrorMessage + e.getMessage());
			logger.info("Failed to update metadata: ", failureErrorMessage + e.getMessage());
		} catch (RestClientException e) {
			model.addAttribute("errorStatusMessage", failureErrorMessage + e.getMessage());
			logger.info("Failed to update metadata: ", failureErrorMessage + e.getMessage());
		} catch (Exception e) {
			model.addAttribute("errorStatusMessage", failureErrorMessage + e.getMessage());
			logger.info("Failed to update metadata: ", failureErrorMessage + e.getMessage());
		}
		// Get metadata attributes from session for Autocomplete
		List<String> metadataAttributesList  = getAllUserMetadataAttributes(authToken, session);
		model.addAttribute("metadataAttributesList", metadataAttributesList);
		logger.info("metadataAttributesList size="+ gson.toJson(metadataAttributesList.size()));
		model.addAttribute("result", "true");
        return "updatemetadatabulk";
	}
	// Get User metadata attributes from session
	public List<String> getAllUserMetadataAttributes(String authToken,  HttpSession session ) {
		List<String> metadataAttributesList = new ArrayList<>();
		// Get metadata attributes from session
		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute(ATTR_USER_DOC_MODEL);
		if (modelDTO == null) {
			modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
			session.setAttribute(ATTR_USER_DOC_MODEL, modelDTO);
		}

		HpcUserPermsForCollectionsDTO permissions = (HpcUserPermsForCollectionsDTO) session
				.getAttribute("userDOCPermissions");

		if (permissions == null) {
			HpcUserDTO user = HpcClientUtil.getUser(authToken, serviceURL, sslCertPath, sslCertPassword);

			permissions = HpcClientUtil.getPermissionsForBasePaths(modelDTO, authToken,
					session.getAttribute("hpcUserId").toString(), childCollectionsAclURL, sslCertPath, sslCertPassword);
		}

		Set<String> userBasePaths = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		// Extract the base paths that this user has READ, WRITE or OWN permissions to
		if (permissions != null && !CollectionUtils.isEmpty(permissions.getPermissionsForCollections())) {
			for (HpcPermissionForCollection collectionPermission : permissions.getPermissionsForCollections()) {
				if (collectionPermission != null && !HpcPermission.NONE.equals(collectionPermission.getPermission())) {
					userBasePaths.add(collectionPermission.getCollectionPath());
				}
			}
		}
		logger.info("userBasePaths =" + gson.toJson(userBasePaths));

		for (HpcDocDataManagementRulesDTO docRule : modelDTO.getDocRules()) {
			if (userBasePaths.contains(docRule.getRules().get(0).getBasePath())) {
				for (HpcDataManagementRulesDTO rule : docRule.getRules()) {
					for (HpcMetadataValidationRule basepathCollectionRule : rule
							.getCollectionMetadataValidationRules()) {
						if (basepathCollectionRule.getAttribute().equals("collection_type")) {
							continue;
						}
						metadataAttributesList.add(basepathCollectionRule.getAttribute());
					}
				}
			}
		}
		logger.info("getAllUserMetadataAttributes metadataAttributesList size ="+ metadataAttributesList.size());
		HashSet<String> hset = new HashSet<String>(metadataAttributesList);
		List<String> uniqueMetadataAttributeNames = new ArrayList<>(hset);
		Collections.sort(uniqueMetadataAttributeNames, String.CASE_INSENSITIVE_ORDER);
		return uniqueMetadataAttributeNames;
	}

	private class HpcPathGridEntry {
		public String path;
		public String result;
	}
}
