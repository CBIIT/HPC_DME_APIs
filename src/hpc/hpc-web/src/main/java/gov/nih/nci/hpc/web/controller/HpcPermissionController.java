/**
 * HpcUserRegistrationController.java
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
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.hpc.domain.datamanagement.HpcGroupPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcPermissionEntry;
import gov.nih.nci.hpc.web.model.HpcPermissionEntryType;
import gov.nih.nci.hpc.web.model.HpcPermissions;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * HPC DM User registration controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcUserRegistrationController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/permissions")
public class HpcPermissionController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.acl}")
	private String serverAclURL;
	@Value("${gov.nih.nci.hpc.server.user}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String serverCollectionURL;
	@Value("${gov.nih.nci.hpc.server.dataObject}")
	private String serverDataObjectURL;

	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String body, @RequestParam String path, @RequestParam String type,
			@RequestParam String assignType, Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		session.removeAttribute("permissions");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}

		if (assignType == null || assignType.trim().length() == 0)
			assignType = "User";

		if (path == null || type == null || path.trim().length() == 0 || type.trim().length() == 0)
			model.addAttribute("updateStatus", "Invalid request! Path or Path type is missing");

		String selectedUsers = (String) session.getAttribute("selectedUsers");
		model.addAttribute("selectedUsers", selectedUsers);
		String selectedGroups = (String) session.getAttribute("selectedGroups");
		model.addAttribute("selectedGroups", selectedGroups);
		populatePermissions(model, path, type, assignType, authToken, session);
		return "permission";
	}

	private String getServiceURL(Model model, String path, String type) {
		String serviceAPIUrl = null;
		if (type.equals("collection"))
			serviceAPIUrl = serverCollectionURL + path + "/acl";
		else if (type.equals("dataObject"))
			serviceAPIUrl = serverDataObjectURL + path + "/acl";
		else {
			model.addAttribute("updateStatus", "Invalid path type. Valid values are collection/dataObject");
			return null;
		}
		return serviceAPIUrl;
	}

	private void populatePermissions(Model model, String path, String type, String assignType, String token,
			HttpSession session) {
		String serviceAPIUrl = getServiceURL(model, path, type);
		if (serviceAPIUrl == null)
			return;

		HpcEntityPermissionsDTO permissionsDTO = HpcClientUtil.getPermissions(token, serviceAPIUrl, sslCertPath,
				sslCertPassword);
		// Get path permissions
		List<String> assignedNames = new ArrayList<String>();
		HpcPermissions permissions = new HpcPermissions();
		permissions.setPath(path);
		permissions.setType(type);
		permissions.setAssignType(assignType);
		if (permissionsDTO != null) {
			List<HpcUserPermission> userPermissions = permissionsDTO.getUserPermissions();
			for (HpcUserPermission permission : userPermissions) {
				if (permission.getUserId().equals("rods") || permission.getUserId().equals("ncif-hpcdm-svc"))
					continue;
				HpcPermissionEntry entry = new HpcPermissionEntry();
				entry.setName(permission.getUserId());
				entry.setType(HpcPermissionEntryType.USER);
				if (permission.getPermission().equals("READ"))
					entry.setRead(true);
				else if (permission.getPermission().equals("WRITE"))
					entry.setWrite(true);
				else if (permission.getPermission().equals("OWN"))
					entry.setOwn(true);

				permissions.getEntries().add(entry);
				assignedNames.add(permission.getUserId());
			}
			List<HpcGroupPermission> groupPermissions = permissionsDTO.getGroupPermissions();
			for (HpcGroupPermission permission : groupPermissions) {
				if (permission.getGroupName().equals("rodsadmin"))
					continue;
				HpcPermissionEntry entry = new HpcPermissionEntry();
				entry.setName(permission.getGroupName());
				entry.setType(HpcPermissionEntryType.GROUP);
				if (permission.getPermission().equals("READ"))
					entry.setRead(true);
				else if (permission.getPermission().equals("WRITE"))
					entry.setWrite(true);
				else if (permission.getPermission().equals("OWN"))
					entry.setOwn(true);

				permissions.getEntries().add(entry);
				assignedNames.add(permission.getGroupName());
			}

		}
		session.setAttribute("permissions", permissions);
		model.addAttribute("permissions", permissions);
		model.addAttribute("names", assignedNames);
	}

	@RequestMapping(method = RequestMethod.POST)
	public String setPermissions(@Valid @ModelAttribute("permissions") HpcPermissions permissionsRequest, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			RedirectAttributes redirectAttrs) {

		String path = (String) session.getAttribute("permissionsPath");
		String serviceAPIUrl = getServiceURL(model, permissionsRequest.getPath(), permissionsRequest.getType());
		if (serviceAPIUrl == null)
			return "permission";

		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			HpcEntityPermissionsDTO subscriptionsRequestDTO = constructRequest(request, permissionsRequest.getPath());
			setChangedPermissions(session, subscriptionsRequestDTO);
			WebClient client = HpcClientUtil.getWebClient(serviceAPIUrl, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);

			Response restResponse = client.invoke("POST", subscriptionsRequestDTO);
			if (restResponse.getStatus() == 200) {
				model.addAttribute("updateStatus", "Updated successfully");
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
				model.addAttribute("updateStatus", "Failed to save criteria! Reason: " + exception.getMessage());
			}
		} catch (HttpStatusCodeException e) {
			model.addAttribute("updateStatus", "Failed to update changes! " + e.getMessage());
			e.printStackTrace();
		} catch (RestClientException e) {
			model.addAttribute("updateStatus", "Failed to update changes! " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			model.addAttribute("updateStatus", "Failed to update changes! " + e.getMessage());
			e.printStackTrace();
		} finally {
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (authToken == null) {
				return "redirect:/";
			}
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			if (user == null) {
				ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
				bindingResult.addError(error);
				HpcLogin hpcLogin = new HpcLogin();
				model.addAttribute("hpcLogin", hpcLogin);
				return "redirect:/";
			}

			populatePermissions(model, permissionsRequest.getPath(), permissionsRequest.getType(),
					permissionsRequest.getAssignType(), authToken, session);
		}

		return "permission";
	}

	private void setChangedPermissions(HttpSession session, HpcEntityPermissionsDTO subscriptionsRequestDTO) {
		HpcPermissions permissions = (HpcPermissions) session.getAttribute("permissions");
		if (permissions == null)
			return;
		List<HpcUserPermission> updatedUserPermissions = new ArrayList<HpcUserPermission>();
		List<HpcGroupPermission> updatedGroupPermissions = new ArrayList<HpcGroupPermission>();
		TreeSet<HpcPermissionEntry> permissionEntires = permissions.getEntries();
		List<String> updatedUsers = new ArrayList<String>();
		List<String> updatedgroups = new ArrayList<String>();
		List<HpcUserPermission> userPermissions = subscriptionsRequestDTO.getUserPermissions();
		List<HpcGroupPermission> groupPermissions = subscriptionsRequestDTO.getGroupPermissions();

		for (HpcUserPermission userPermission : subscriptionsRequestDTO.getUserPermissions()) {
			boolean found = false;
			for (HpcPermissionEntry entry : permissionEntires) {
				HpcPermissionEntryType type = entry.getType();
				if (type.equals(HpcPermissionEntryType.USER) && userPermission.getUserId().equals(entry.getName())) {
					found = true;
					if (!userPermission.getPermission().equals(entry.getPermission()))
						updatedUserPermissions.add(userPermission);
					break;
				}
			}
			if (!found)
				updatedUserPermissions.add(userPermission);
		}

		for (HpcGroupPermission groupPermission : subscriptionsRequestDTO.getGroupPermissions()) {
			boolean found = false;
			for (HpcPermissionEntry entry : permissionEntires) {
				HpcPermissionEntryType type = entry.getType();
				if (type.equals(HpcPermissionEntryType.GROUP)
						&& groupPermission.getGroupName().equals(entry.getName())) {
					found = true;
					if (!groupPermission.getPermission().equals(entry.getPermission()))
						updatedGroupPermissions.add(groupPermission);
					break;
				}
			}
			if (!found)
				updatedGroupPermissions.add(groupPermission);
		}

		subscriptionsRequestDTO.getUserPermissions().clear();
		subscriptionsRequestDTO.getGroupPermissions().clear();
		subscriptionsRequestDTO.getGroupPermissions().addAll(updatedGroupPermissions);
		subscriptionsRequestDTO.getUserPermissions().addAll(updatedUserPermissions);
	}

	private HpcEntityPermissionsDTO constructRequest(HttpServletRequest request, String path) {
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
						userPermission.setPermission("OWN");
					else if (permission[0].equals("read"))
						userPermission.setPermission("READ");
					else if (permission[0].equals("write"))
						userPermission.setPermission("WRITE");
					else if (permission[0].equals("none"))
						userPermission.setPermission("NONE");
					userPermissions.add(userPermission);
				} else {
					HpcGroupPermission groupPermission = new HpcGroupPermission();
					groupPermission.setGroupName(permissionName[0]);

					String[] permission = request.getParameterValues("permission" + index);
					if (permission[0].equals("own"))
						groupPermission.setPermission("OWN");
					else if (permission[0].equals("read"))
						groupPermission.setPermission("READ");
					else if (permission[0].equals("write"))
						groupPermission.setPermission("WRITE");
					else if (permission[0].equals("none"))
						groupPermission.setPermission("NONE");
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
}
