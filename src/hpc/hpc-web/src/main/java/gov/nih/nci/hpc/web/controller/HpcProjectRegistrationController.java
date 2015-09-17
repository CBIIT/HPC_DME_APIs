/**
 * HpcDataRegistrationController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;
import gov.nih.nci.hpc.domain.dataset.HpcFileType;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.metadata.HpcCompressionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcEncryptionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
import gov.nih.nci.hpc.domain.metadata.HpcPHIContent;
import gov.nih.nci.hpc.domain.metadata.HpcPIIContent;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcProjectType;
import gov.nih.nci.hpc.dto.project.HpcProjectRegistrationDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcResponseErrorHandler;
import gov.nih.nci.hpc.web.model.HpcProjectRegistration;
import gov.nih.nci.hpc.web.util.Util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * <p>
 * HPC DM Project registration controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDataRegistrationController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/registerProject")
public class HpcProjectRegistrationController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.project}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.nihfnlcr.name}")
	private String destinationEndpoint;

	/*
	 * Action for project registration page
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(Model model) {
		HpcProjectRegistration hpcRegistration = new HpcProjectRegistration();
		model.addAttribute("hpcProjectRegistration", hpcRegistration);
		Map<String, String> users = Util.getPIs();
		model.addAttribute("piList", users);
		model.addAttribute("userAttrs", Util.getUserDefinedPrimaryMedataAttrs());
		model.addAttribute("projectTypeList", Util.getProjectTypes());
		return "projectRegistration";
	}
	
	/*
	 * Action for Project registration
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String register(
			@Valid @ModelAttribute("hpcProjectRegistration") HpcProjectRegistration registration,
			Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		// TODO: Add error message
		if (user == null) {
			return "index";
		}
		List<HpcMetadataItem> eItems = getMetadataitems(request);
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new HpcResponseErrorHandler());
		HpcProjectRegistrationDTO dto = new HpcProjectRegistrationDTO();
		HpcProjectMetadata metadata = new HpcProjectMetadata();
		metadata.setDescription(registration.getDescription());
		metadata.setInternalProjectId(registration.getInternalProjectId());
		metadata.setLabBranch(registration.getLabBranch());
		metadata.setName(registration.getName());
		String type = registration.getProjectType();
		if(type != null)
		{
			if(type.equals("UMBRELLA"))
				metadata.setType(HpcProjectType.UMBRELLA);
			else if(type.equals("ANALYSIS"))
				metadata.setType(HpcProjectType.ANALYSIS);
			else if(type.equals("SEQUENCING"))
				metadata.setType(HpcProjectType.SEQUENCING);
			else if(type.equals("UNKNOWN"))
				metadata.setType(HpcProjectType.UNKNOWN);

		}
		metadata.setPrincipalInvestigatorNciUserId(registration.getPrincipalInvestigatorNihUserId());
		//TODO: Get DOC dynamically
		metadata.setPrincipalInvestigatorDOC("NCI");
		metadata.setRegistrarDOC("NCI");
		metadata.setRegistrarNciUserId(user.getNciAccount().getUserId());
		metadata.setCreated(Calendar.getInstance());
		dto.setMetadata(metadata);
		try {
			ResponseEntity<HpcExceptionDTO> response = restTemplate.postForEntity(
					serviceURL, dto, HpcExceptionDTO.class);
			if(!response.getStatusCode().equals(HttpStatus.CREATED))
			{
				ObjectError error = new ObjectError("hpcProjectRegistration",
						"Failed to register dataset");
				bindingResult.addError(error);
				model.addAttribute("registrationStatus", false);
				model.addAttribute("error",
						"Failed to register your request due to: " +response.getBody());
				return "projectRegistration";
				
			}

			HttpHeaders headers = response.getHeaders();
			String location = headers.getLocation().toString();
			String id = location.substring(location.lastIndexOf("/") + 1);
			registration.setId(id);
			model.addAttribute("registrationStatus", true);
			model.addAttribute("registration", registration);
		} catch (HttpStatusCodeException e) {
			String errorpayload = e.getResponseBodyAsString();
			ObjectError error = new ObjectError("hpcRegistration",
					"Failed to register: " + errorpayload);
			bindingResult.addError(error);
			model.addAttribute("registrationStatus", false);
			model.addAttribute("error",
					"Failed to register your request due to: " + errorpayload);
			return "projectRegistration";
		} catch (RestClientException e) {
			ObjectError error = new ObjectError("hpcProjectRegistration",
					"Failed to register: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("registrationStatus", false);
			model.addAttribute("error",
					"Failed to register your request due to: " + e.getMessage());
			return "projectRegistration";
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcProjectRegistration",
					"Failed to register: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("registrationStatus", false);
			model.addAttribute("error",
					"Failed to register your request due to: " + e.getMessage());
			return "projectRegistration";
		} finally {
			Map<String, String> users = Util.getPIs();
			model.addAttribute("piList", users);
			model.addAttribute("creatorList", users);
			model.addAttribute("userAttrs", Util.getUserDefinedPrimaryMedataAttrs());
			model.addAttribute("projectTypeList", Util.getProjectTypes());
		}
		return "projectRegistrationResult";
	}

	private List<HpcMetadataItem> getMetadataitems(HttpServletRequest request) {
		List<HpcMetadataItem> items = new ArrayList<HpcMetadataItem>();
		Enumeration<String> names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String attr = names.nextElement();
			if(!attr.startsWith("customAttrName"))
				continue;
			
			String index = attr.substring(attr.indexOf("customAttrName")+14);
			
			HpcMetadataItem item = new HpcMetadataItem();
			item.setKey(request.getParameter(attr));
			item.setValue(request.getParameter("customAttrValue"+index));
			items.add(item);
		}
		return items;
	}
}
