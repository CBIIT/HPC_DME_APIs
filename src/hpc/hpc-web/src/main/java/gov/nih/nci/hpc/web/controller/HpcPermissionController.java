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
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionRequestDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionsRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcNotificationRequest;
import gov.nih.nci.hpc.web.model.HpcPermissionEntry;
import gov.nih.nci.hpc.web.model.HpcPermissionEntryType;
import gov.nih.nci.hpc.web.model.HpcPermissions;
import gov.nih.nci.hpc.web.model.HpcPermissionsRequest;
import gov.nih.nci.hpc.web.model.HpcWebUser;
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
	@Value("${gov.nih.nci.hpc.server}")
	private String serverURL;
	@Value("${gov.nih.nci.hpc.server.user}")
	private String serviceURL;

	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String body, @RequestParam String path, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}
		populatePermissions(model, path);
		return "permission";
	}

	private void populatePermissions(Model model, String path)
	{
		//Get path permissions
		List<String> assignedNames = new ArrayList<String>();
		HpcPermissions permissions = new HpcPermissions();
		permissions.setPath(path);
		HpcPermissionEntry entry = new HpcPermissionEntry();
		entry.setName("konkapv");
		entry.setType(HpcPermissionEntryType.USER);
		entry.setRead(true);
		permissions.getEntries().add(entry);
		model.addAttribute("permissions", permissions);
		assignedNames.add("konkapv");
		model.addAttribute("names", assignedNames);
	}
	
	@RequestMapping(method = RequestMethod.POST)
	public String search(@Valid @ModelAttribute("permissions") HpcPermissions permissionsRequest,
			Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			RedirectAttributes redirectAttrs) {

		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			List<HpcEntityPermissionRequestDTO> subscriptionsRequestDTO = constructRequest(request);

			WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
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

			populatePermissions(model, permissionsRequest.getPath());
		}

		return "permission";
	}
	
	private List<HpcEntityPermissionRequestDTO> constructRequest(HttpServletRequest request)
	{
		Enumeration<String> params = request.getParameterNames();
		while(params.hasMoreElements())
		{
			String paramName = params.nextElement();
			if(paramName.startsWith("permissionName"))
			{
				String index = paramName.substring("permissionName".length());
				String[] permissionName = request.getParameterValues(paramName);
				String[] permissionType = request.getParameterValues("permissionType"+index);
				
			}
		}
		List<HpcEntityPermissionRequestDTO> dto = new ArrayList<HpcEntityPermissionRequestDTO>();
		return dto;
	}
}
