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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.domain.datamanagement.HpcGroupPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcDownloadDatafile;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcPermissionEntry;
import gov.nih.nci.hpc.web.model.HpcPermissionEntryType;
import gov.nih.nci.hpc.web.model.HpcPermissions;
import gov.nih.nci.hpc.web.model.HpcBulkPermissionsResult;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcSearchUtil;

/**
 * <p>
 * Controller to manage bulk permissions
 * </p>
 *
 * @author <a href="mailto:Yuri.Dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
public class HpcBulkPermissionsController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.user}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String serverCollectionURL;
	@Value("${gov.nih.nci.hpc.server.dataObject}")
	private String serverDataObjectURL;
	@Value("${hpc.serviceaccount}")
	private String serviceAccount;
	/**
	 * POST action to render bulk permissions page
	 * 
	 * @param body
	 * @param path
	 * @param type
	 * @param assignType
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@PostMapping(value = "/bulkpermissions")
	public String home(@RequestBody(required = false) String body, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {

		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (user == null || authToken == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "dashboard";
		}
		HpcSearchUtil.cacheSelectedRows(session, request, model);
		HpcDownloadDatafile hpcDownloadDatafile = new HpcDownloadDatafile();

		String downloadType = request.getParameter("downloadType");
		hpcDownloadDatafile.setDownloadType(downloadType);
		String selectedPathsStr = request.getParameter("selectedFilePaths");

		if (selectedPathsStr.isEmpty()) {
			model.addAttribute("error", "Data file list is missing!");
		} else {
			StringTokenizer tokens = new StringTokenizer(selectedPathsStr, ",");
			while (tokens.hasMoreTokens()) {
				String pathStr = tokens.nextToken();
				hpcDownloadDatafile.getSelectedPaths().add(pathStr.substring(pathStr.lastIndexOf(":") + 1));
			}
		}
		model.addAttribute("downloadType", downloadType);

		model.addAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
		session.setAttribute("hpcDownloadDatafile", hpcDownloadDatafile);

		HpcPermissions permissions = new HpcPermissions();
		if(downloadType.equalsIgnoreCase("datafiles"))
			permissions.setType("dataObject");
		else 
			permissions.setType("collection");
		model.addAttribute("permissions", permissions);	
		model.addAttribute("ownpermission", true);

			
		HpcSearch hpcSaveSearch = new HpcSearch();
		String pageNumber = request.getParameter("pageNumber");
		hpcSaveSearch.setPageNumber(pageNumber != null ? Integer.parseInt(pageNumber) : 1);
		String pageSize = request.getParameter("pageSize");
		if (StringUtils.isNotBlank(pageSize))
			hpcSaveSearch.setPageSize(Integer.parseInt(pageSize));
		hpcSaveSearch.setQueryName(request.getParameter("queryName"));
		hpcSaveSearch.setSearchType(request.getParameter("searchType"));
		model.addAttribute("hpcSearch", hpcSaveSearch);
		session.setAttribute("hpcSavedSearch", hpcSaveSearch);

		return "permissionbulk";
	}


	/**
	 * POST action to assign or update user permissions. On update, redirect
	 * back to bulk permissions page with results
	 * 
	 * @param permissionsRequest
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param redirectAttrs
	 * @return
	 */
	@PostMapping(value = "/assignbulkpermissions")
	public String setBulkPermissions(@Valid @ModelAttribute("permissions") HpcPermissions permissionsRequest, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			RedirectAttributes redirectAttrs) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (authToken == null) {
			return "redirect:/";
		}

		String selectedPathsStr = request.getParameter("selectedFilePaths");
		String downloadType = request.getParameter("downloadType");
		HpcEntityPermissionsDTO permissionDTO = constructRequest(request);
		
		List<HpcBulkPermissionsResult> bulkResults = new ArrayList<>();
		
		HpcDownloadDatafile hpcDownloadDatafile = new HpcDownloadDatafile();
		hpcDownloadDatafile.setDownloadType(downloadType);
		
		if (selectedPathsStr.isEmpty()) {
			model.addAttribute("updateStatus", "Data file list is missing!");
		} else {
			
			selectedPathsStr = selectedPathsStr.substring(1, selectedPathsStr.length() - 1);
			StringTokenizer tokens = new StringTokenizer(selectedPathsStr, ",");
			while (tokens.hasMoreTokens()) {
				String path = tokens.nextToken().trim();
				hpcDownloadDatafile.getSelectedPaths().add(path.substring(path.lastIndexOf(":") + 1));
				HpcBulkPermissionsResult result = new HpcBulkPermissionsResult();
				result.setPath(path);
				try {
					String serviceAPIUrl = getServiceURL(path, permissionsRequest.getType());
					//Update permissions
					if (setPermissionForPath(serviceAPIUrl, permissionDTO, authToken)) {
						result.setStatus("Success");
						bulkResults.add(result);
					}
				} catch (Exception e) {
					result.setError(e.getMessage());
					result.setStatus("Failure");
					bulkResults.add(result);
				}
			}
		}
		
		model.addAttribute("bulkResults", bulkResults);
		model.addAttribute("downloadType", downloadType);

		model.addAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
		session.setAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
		
		populatePermissions(permissionDTO, model, permissionsRequest.getType());
		model.addAttribute("ownpermission", true);
		
		HpcSearch hpcSaveSearch = (HpcSearch) session.getAttribute("hpcSavedSearch");
		model.addAttribute("hpcSearch", hpcSaveSearch);
		
		return "permissionbulk";
	}
	
	private String getServiceURL(String path, String type) {
	    try {
	      String basisUrl = null;
	      if ("collection".equals(type)) {
	        basisUrl = this.serverCollectionURL;
	      } else if ("dataObject".equals(type)) {
	        basisUrl = this.serverDataObjectURL;
	      } else {
	        throw new HpcWebException("Invalid path type. Valid values" +
	          " are collection/dataObject");
	      }
	      if (null == basisUrl) {
	        return null;
	      }
		
	      return UriComponentsBuilder.fromHttpUrl(basisUrl)
					.path("/{dme-archive-path}/acl").buildAndExpand(path).encode().toUri()
	        .toURL().toExternalForm();
	    } catch (MalformedURLException e) {
	      throw new HpcWebException("Unable to generate URL to invoke for ACL on" +
	        " " + type + " " + path + ".", e);
	    }
		}


	private boolean setPermissionForPath(String serviceAPIUrl, HpcEntityPermissionsDTO permissionDTO, String authToken) throws Exception {

		try {

			if (CollectionUtils.isNotEmpty(permissionDTO.getUserPermissions())
					|| CollectionUtils.isNotEmpty(permissionDTO.getGroupPermissions())) {
				WebClient client = HpcClientUtil.getWebClient(serviceAPIUrl, sslCertPath, sslCertPassword);
				client.header("Authorization", "Bearer " + authToken);

				Response restResponse = client.invoke("POST", permissionDTO);
				if (restResponse.getStatus() == 200) {
					ObjectMapper mapper = new ObjectMapper();
					AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
							new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
							new JacksonAnnotationIntrospector());
					mapper.setAnnotationIntrospector(intr);
					mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

					MappingJsonFactory factory = new MappingJsonFactory(mapper);
					JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

					HpcEntityPermissionsResponseDTO permissionResponseDTO = parser.readValueAs(HpcEntityPermissionsResponseDTO.class);
					for(HpcUserPermissionResponseDTO userPermissionResponse: permissionResponseDTO.getUserPermissionResponses()) {
						if(!userPermissionResponse.getResult()) {
							throw new Exception(userPermissionResponse.getMessage());
						}
					}
					return true;
				} else {
					ObjectMapper mapper = new ObjectMapper();
					AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
							new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
							new JacksonAnnotationIntrospector());
					mapper.setAnnotationIntrospector(intr);
					mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

					MappingJsonFactory factory = new MappingJsonFactory(mapper);
					JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

					HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
					throw new Exception(exception.getMessage());
				}
			}
		} catch (HttpStatusCodeException e) {
			throw e;
		} catch (RestClientException e) {
			throw e;
		} catch (Exception e) {
			throw e;
		}
		return false;
	}
	
	private HpcEntityPermissionsDTO constructRequest(HttpServletRequest request) {
		Enumeration<String> params = request.getParameterNames();

		HpcEntityPermissionsDTO dto = new HpcEntityPermissionsDTO();
		List<HpcUserPermission> userPermissions = new ArrayList<HpcUserPermission>();
		List<HpcGroupPermission> groupPermissions = new ArrayList<HpcGroupPermission>();
		while (params.hasMoreElements()) {
			String paramName = params.nextElement();
			if (paramName.startsWith("permissionName")) {
				String index = paramName.substring("permissionName".length());
				String[] permissionName = request.getParameterValues(paramName);
				String[] permissionType = request.getParameterValues("permissionType" + index);
				if (permissionType[0].equals("USER")) {
					HpcUserPermission userPermission = new HpcUserPermission();
					userPermission.setUserId(permissionName[0]);

					String[] permission = request.getParameterValues("permission" + index);
					if (permission[0].equals("own"))
						userPermission.setPermission(HpcPermission.OWN);
					else if (permission[0].equals("read"))
						userPermission.setPermission(HpcPermission.READ);
					else if (permission[0].equals("write"))
						userPermission.setPermission(HpcPermission.WRITE);
					else if (permission[0].equals("none"))
						userPermission.setPermission(HpcPermission.NONE);
					userPermissions.add(userPermission);
				} else {
					HpcGroupPermission groupPermission = new HpcGroupPermission();
					groupPermission.setGroupName(permissionName[0]);

					String[] permission = request.getParameterValues("permission" + index);
					if (permission[0].equals("own"))
						groupPermission.setPermission(HpcPermission.OWN);
					else if (permission[0].equals("read"))
						groupPermission.setPermission(HpcPermission.READ);
					else if (permission[0].equals("write"))
						groupPermission.setPermission(HpcPermission.WRITE);
					else if (permission[0].equals("none"))
						groupPermission.setPermission(HpcPermission.NONE);
					groupPermissions.add(groupPermission);
				}
			}
		}
		if (userPermissions.size() > 0)
			dto.getUserPermissions().addAll(userPermissions);
		if (groupPermissions.size() > 0)
			dto.getGroupPermissions().addAll(groupPermissions);
		
		return dto;
		
	}
	
	private void populatePermissions(HpcEntityPermissionsDTO permissionsDTO, Model model, String type) {

		List<String> assignedNames = new ArrayList<String>();
		HpcPermissions permissions = new HpcPermissions();
		permissions.setType(type);
		permissions.setAssignType("User");
		if (permissionsDTO != null) {
			List<HpcUserPermission> userPermissions = permissionsDTO.getUserPermissions();
			for (HpcUserPermission permission : userPermissions) {
				HpcPermissionEntry entry = new HpcPermissionEntry();
				entry.setName(permission.getUserId());
				entry.setType(HpcPermissionEntryType.USER);
				if (permission.getPermission().equals(HpcPermission.READ))
					entry.setRead(true);
				else if (permission.getPermission().equals(HpcPermission.WRITE))
					entry.setWrite(true);
				else if (permission.getPermission().equals(HpcPermission.OWN))
					entry.setOwn(true);
				
				permissions.getEntries().add(entry);
				assignedNames.add(permission.getUserId());
			}
			List<HpcGroupPermission> groupPermissions = permissionsDTO.getGroupPermissions();
			for (HpcGroupPermission permission : groupPermissions) {
				HpcPermissionEntry entry = new HpcPermissionEntry();
				entry.setName(permission.getGroupName());
				entry.setType(HpcPermissionEntryType.GROUP);
				if (permission.getPermission().equals(HpcPermission.READ))
					entry.setRead(true);
				else if (permission.getPermission().equals(HpcPermission.WRITE))
					entry.setWrite(true);
				else if (permission.getPermission().equals(HpcPermission.OWN))
					entry.setOwn(true);

				permissions.getEntries().add(entry);
				assignedNames.add(permission.getGroupName());
			}

		}
		model.addAttribute("permissions", permissions);
		model.addAttribute("names", assignedNames);
	}
}
