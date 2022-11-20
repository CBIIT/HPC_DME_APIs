/**
 * HpcPermissionController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import gov.nih.nci.hpc.domain.datamanagement.HpcGroupPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.security.HpcGroup;
import gov.nih.nci.hpc.dto.security.HpcGroupListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.dto.security.HpcUserListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserListEntry;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.AutoCompleteResponseBody;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcPermissionEntry;
import gov.nih.nci.hpc.web.model.HpcPermissionEntryType;
import gov.nih.nci.hpc.web.model.HpcPermissions;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcModelBuilder;
import gov.nih.nci.hpc.web.util.MiscUtil;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * <p>
 * Controller to manage user permissions on a collection or data file
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
	@Value("${gov.nih.nci.hpc.server.user.query}")
	private String activeUsersServiceURL;
	@Value("${gov.nih.nci.hpc.server.group}")
	private String groupServiceURL;
	@Value("${hpc.serviceaccount}")
	private String serviceAccount;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;
	@Value("${gov.nih.nci.hpc.server.collection.acl}")
	private String collectionAclsURL;

	@Autowired
	private HpcModelBuilder hpcModelBuilder;

	//The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	/**
	 * GET action to populate user permissions
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
	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String body, @RequestParam String path, @RequestParam String type,
			@RequestParam String assignType, Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String userId = (String) session.getAttribute("hpcUserId");
		String authToken = (String) session.getAttribute("hpcUserToken");
		session.removeAttribute("permissions");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/login";
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
		HpcUserPermissionDTO userPermission = HpcClientUtil.getPermissionForUser(authToken, path, userId,
				(type != null && type.equals("collection")) ? serverCollectionURL : serverDataObjectURL, sslCertPath,
				sslCertPassword);
		model.addAttribute("ownpermission",
				(userPermission != null && userPermission.getPermission().equals(HpcPermission.OWN)) ? true : false);

		return "permission";
	}

	/**
	 * POST action to assign or update user permissions. On update, redirect
	 * back to permissions page with resulted message
	 * 
	 * @param permissionsRequest
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param redirectAttrs
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String setPermissions(@Valid @ModelAttribute("permissions") HpcPermissions permissionsRequest, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			RedirectAttributes redirectAttrs) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (authToken == null) {
			return "redirect:/";
		}

		String serviceAPIUrl = getServiceURL(model, permissionsRequest.getPath(), permissionsRequest.getType());
		if (serviceAPIUrl == null)
			return "permission";

		try {
			HpcEntityPermissionsDTO subscriptionsRequestDTO = constructRequest(request, permissionsRequest.getPath());
			setChangedPermissions(session, subscriptionsRequestDTO);
			if (CollectionUtils.isNotEmpty(subscriptionsRequestDTO.getUserPermissions())
					|| CollectionUtils.isNotEmpty(subscriptionsRequestDTO.getGroupPermissions())) {
				WebClient client = HpcClientUtil.getWebClient(serviceAPIUrl, sslCertPath, sslCertPassword);
				client.header("Authorization", "Bearer " + authToken);

				Response restResponse = client.invoke("POST", subscriptionsRequestDTO);
				if (restResponse.getStatus() == 200) {
					redirectAttrs.addFlashAttribute("updateStatus", "Updated successfully");

					//Reload the base path permissions if this is a base path permission change

					String path = permissionsRequest.getPath();
					String basePath = path.substring(0, StringUtils.ordinalIndexOf(path, "/", 2) < 0 ? path.length() : StringUtils.ordinalIndexOf(path, "/", 2));
				     if(basePath.contentEquals(path)) {

				        //Get DOC Models, goes to server only if cache does not have it
				        HpcDataManagementModelDTO modelDTO =
				        hpcModelBuilder.getDOCModel(authToken, this.hpcModelURL, this.sslCertPath, this.sslCertPassword);
				        if(modelDTO != null) {
				            //Overwrite session attribute
			                session.setAttribute("userDOCModel", modelDTO);

			                //Reload the base paths permissions
			                hpcModelBuilder.updateModelPermissions(modelDTO, authToken, collectionAclsURL,
			                this.sslCertPath, this.sslCertPassword);
				        }
				     }

					return "redirect:/permissions?assignType=User&type="
							+ MiscUtil.performUrlEncoding(permissionsRequest.getType()) + "&path="
							+ MiscUtil.performUrlEncoding(permissionsRequest.getPath());
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
			}
		} catch (Exception e) {
			model.addAttribute("updateStatus", "Failed to update changes! " + e.getMessage());
			logger.error(String.format("Failed to update permissions for path %s",permissionsRequest.getPath()), e);
		} finally {
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			if (user == null) {
				ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
				bindingResult.addError(error);
				HpcLogin hpcLogin = new HpcLogin();
				model.addAttribute("hpcLogin", hpcLogin);
				return "redirect:/";
			}
		}
		String userId = (String) session.getAttribute("hpcUserId");
		populatePermissions(model, permissionsRequest.getPath(), permissionsRequest.getType(),
				permissionsRequest.getAssignType(), authToken, session);
		HpcUserPermissionDTO userPermission = HpcClientUtil.getPermissionForUser(authToken, permissionsRequest.getPath(), userId,
				(permissionsRequest.getType() != null && permissionsRequest.getType().equals("collection")) ? serverCollectionURL : serverDataObjectURL, sslCertPath,
				sslCertPassword);
		model.addAttribute("ownpermission",
				(userPermission != null && userPermission.getPermission().equals(HpcPermission.OWN)) ? true : false);


		return "permission";
	}
	
	/**
	 * POST Action. This AJAX action is invoked for user search
	 * 
	 * @param term
	 * @return
	 */
	@RequestMapping(value = "/users", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody List<AutoCompleteResponseBody> searchUsers(@RequestParam("term") String term,
			HttpSession session) {
		List<AutoCompleteResponseBody> entries = new ArrayList<AutoCompleteResponseBody>();
		String authToken = (String) session.getAttribute("hpcUserToken");
		String serviceUrl = activeUsersServiceURL;
		String[] name = term.split(",");
		String lastNameOrUserId = name[0] + "%";
		String firstName = name[0] + "%";
		if (name.length > 1)
			firstName = name[1] + "%";
		HpcUserListDTO users = HpcClientUtil.getUsers(authToken, serviceUrl, lastNameOrUserId, firstName,
				lastNameOrUserId, null, sslCertPath, sslCertPassword);
		if (users != null && users.getUsers() != null && !users.getUsers().isEmpty()) {
			for (HpcUserListEntry user : users.getUsers()) {
				AutoCompleteResponseBody entry = new AutoCompleteResponseBody();
				entry.setValue(user.getUserId());
				entry.setLabel(user.getFirstName() + " " + user.getLastName() + " [" + user.getUserId() + "] - "
						+ user.getDoc());
				entries.add(entry);
			}
		}
		return entries;
	}

	/**
	 * POST Action. This AJAX action is invoked for group search
	 * 
	 * @param term
	 * @return
	 */
	@RequestMapping(value = "/groups", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody List<AutoCompleteResponseBody> searchGroups(@RequestParam("term") String term,
			HttpSession session) {
		List<AutoCompleteResponseBody> entries = new ArrayList<AutoCompleteResponseBody>();
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcGroupListDTO groups = HpcClientUtil.getGroups(authToken, groupServiceURL, "%" + term + "%", sslCertPath,
				sslCertPassword);
		if (groups != null && groups.getGroups() != null && !groups.getGroups().isEmpty()) {
			for (HpcGroup group : groups.getGroups()) {
				AutoCompleteResponseBody entry = new AutoCompleteResponseBody();
				entry.setValue(group.getGroupName());
				entry.setLabel(group.getGroupName());
				entries.add(entry);
			}
		}
		return entries;
	}
	
	private String getServiceURL(Model model, String path, String type) {
    try {
      String basisUrl = null;
      if ("collection".equals(type)) {
        basisUrl = this.serverCollectionURL;
      } else if ("dataObject".equals(type)) {
        basisUrl = this.serverDataObjectURL;
      } else {
        model.addAttribute("updateStatus", "Invalid path type. Valid values" +
          " are collection/dataObject");
      }
      if (null == basisUrl) {
        return null;
      }
			final String serviceAPIUrl = UriComponentsBuilder.fromHttpUrl(basisUrl)
				.path("/{dme-archive-path}/acl").buildAndExpand(path).encode().toUri()
        .toURL().toExternalForm();
      return serviceAPIUrl;
    } catch (MalformedURLException e) {
      throw new HpcWebException("Unable to generate URL to invoke for ACL on" +
        " " + type + " " + path + ".", e);
    }
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
				if (permission.getUserId().equals("rods") || permission.getUserId().equals(serviceAccount))
					continue;
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
				if (permission.getGroupName().equals("rodsadmin"))
					continue;
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
		session.setAttribute("permissions", permissions);
		model.addAttribute("permissions", permissions);
		model.addAttribute("names", assignedNames);
	}

	private void setChangedPermissions(HttpSession session, HpcEntityPermissionsDTO subscriptionsRequestDTO) {
		HpcPermissions permissions = (HpcPermissions) session.getAttribute("permissions");
		if (permissions == null)
			return;
		List<HpcUserPermission> updatedUserPermissions = new ArrayList<HpcUserPermission>();
		List<HpcGroupPermission> updatedGroupPermissions = new ArrayList<HpcGroupPermission>();
		TreeSet<HpcPermissionEntry> permissionEntires = permissions.getEntries();

		for (HpcUserPermission userPermission : subscriptionsRequestDTO.getUserPermissions()) {
			boolean found = false;
			for (HpcPermissionEntry entry : permissionEntires) {
				HpcPermissionEntryType type = entry.getType();
				if (type.equals(HpcPermissionEntryType.USER) && userPermission.getUserId().equals(entry.getName())) {
					found = true;
					if (!userPermission.getPermission().value().equals(entry.getPermission()))
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
					if (!groupPermission.getPermission().value().equals(entry.getPermission()))
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
}
