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

import gov.nih.nci.hpc.domain.dataset.HpcFileType;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcCompressionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcEncryptionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
import gov.nih.nci.hpc.domain.metadata.HpcPHIContent;
import gov.nih.nci.hpc.domain.metadata.HpcPIIContent;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcResponseErrorHandler;
import gov.nih.nci.hpc.web.model.HpcDatasetRegistration;
import gov.nih.nci.hpc.web.util.Util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
 * HPC DM Dataset registration controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/registerDataset")
public class HpcDataRegistrationController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.dataset}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.nihfnlcr.name}")
	private String destinationEndpoint;

	/*
	 * Action for Datset registration page
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(Model model) {
		HpcDatasetRegistration hpcRegistration = new HpcDatasetRegistration();
		model.addAttribute("hpcRegistration", hpcRegistration);
		Map<String, String> users = Util.getPIs();
		model.addAttribute("piList", users);
		model.addAttribute("creatorList", users);
		model.addAttribute("userAttrs", Util.getUserDefinedPrimaryMedataAttrs());
		return "datasetRegistration";
	}
	
	/*
	 * Action for Dataset registration
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String register(
			@Valid @ModelAttribute("hpcRegistration") HpcDatasetRegistration registration,
			Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		// TODO: Add error message
		if (user == null) {
			return "index";
		}
		
		if(registration.getFilesChecked() == null || registration.getFilesChecked().length == 0)
		{
			ObjectError error = new ObjectError("hpcRegistration",
					"Failed to register dataset. Dataset file(s) missing. Please add file(s) to dataset.");
			bindingResult.addError(error);
			model.addAttribute("registrationStatus", false);
			model.addAttribute("error",
					"Failed to register your request due to missing file(s)");
			model.addAttribute("piList", Util.getPIs());
			model.addAttribute("userAttrs", Util.getUserDefinedPrimaryMedataAttrs());
			return "datasetRegistration";
		}
		List<HpcMetadataItem> eItems = getMetadataitems(request);
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new HpcResponseErrorHandler());
		HpcDatasetRegistrationDTO dto = new HpcDatasetRegistrationDTO();
		dto.setName(registration.getDatasetName());
		dto.setDescription(registration.getDescription());
		dto.setComments(registration.getComments());

		// TODO: Lookup Id
		String filesSel[] =  registration.getFilesChecked();
		//String files = registration.getOriginEndpointFilePath();
		//StringTokenizer tokens = new StringTokenizer(files, ",");
		//while (tokens.hasMoreTokens()) {
		for (String file: filesSel) {           

			HpcFileUploadRequest upload = new HpcFileUploadRequest();
			HpcFileType fileType;
			HpcDataTransferLocations locations = new HpcDataTransferLocations();
			HpcFileLocation source = new HpcFileLocation();
			source.setEndpoint(registration.getOriginEndpoint());
			String filePath = "~/"+file;
			source.setPath(filePath);
			HpcFileLocation destination = new HpcFileLocation();
			destination.setEndpoint(destinationEndpoint);
			destination.setPath(filePath);
			locations.setDestination(destination);
			locations.setSource(source);
			upload.setLocations(locations);
			// TODO: Identify file type
			upload.setType(HpcFileType.UNKONWN);

			if(registration.getProjectIds() != null && registration.getProjectIds().indexOf(",") != -1)
			{
				StringTokenizer tokens = new StringTokenizer(registration.getProjectIds(), ",");
				while(tokens.hasMoreTokens())
					upload.getProjectIds().add(tokens.nextToken());
			}
			else if(registration.getProjectIds() != null && registration.getProjectIds().trim().length() > 0)
				upload.getProjectIds().add(registration.getProjectIds());
				
			// TODO: Metadata funding organization
			HpcFilePrimaryMetadata metadata = new HpcFilePrimaryMetadata();
			if (registration.getEncrypted().equals("ENCRYPTED"))
				metadata.setDataEncrypted(HpcEncryptionStatus.ENCRYPTED);
			else if (registration.getEncrypted().equals("NOT_ENCRYPTED"))
				metadata.setDataEncrypted(HpcEncryptionStatus.NOT_ENCRYPTED);
			else if (registration.getEncrypted().equals("NOT_SPECIFIED"))
				metadata.setDataEncrypted(HpcEncryptionStatus.NOT_SPECIFIED);

			if (registration.getPii().equals("PII_NOT_PRESENT"))
				metadata.setDataContainsPII(HpcPIIContent.PII_NOT_PRESENT);
			else if (registration.getPii().equals("PII_PRESENT"))
				metadata.setDataContainsPII(HpcPIIContent.PII_PRESENT);
			else if (registration.getPii().equals("NOT_SPECIFIED"))
				metadata.setDataContainsPII(HpcPIIContent.NOT_SPECIFIED);

			if (registration.getCompressed().equals("COMPRESSED"))
				metadata.setDataCompressed(HpcCompressionStatus.COMPRESSED);
			else if (registration.getCompressed().equals("NOT_COMPRESSED"))
				metadata.setDataCompressed(HpcCompressionStatus.NOT_COMPRESSED);
			else if (registration.getCompressed().equals("NOT_SPECIFIED"))
				metadata.setDataCompressed(HpcCompressionStatus.NOT_SPECIFIED);

			if (registration.getPhi().equals("PHI_NOT_PRESENT"))
				metadata.setDataContainsPHI(HpcPHIContent.PHI_NOT_PRESENT);
			else if (registration.getPhi().equals("PHI_PRESENT"))
				metadata.setDataContainsPHI(HpcPHIContent.PHI_PRESENT);
			else if (registration.getPhi().equals("NOT_SPECIFIED"))
				metadata.setDataContainsPHI(HpcPHIContent.NOT_SPECIFIED);

			String fundingOrg = registration.getFundingOrganization();
			if(fundingOrg != null && fundingOrg.trim().length() > 0)
				metadata.setFundingOrganization("Not_Specified");
			else
				metadata.setFundingOrganization(registration
						.getFundingOrganization());
			metadata.setPrincipalInvestigatorNciUserId(registration
					.getInvestigatorId());
			metadata.setRegistrarNciUserId(user.getNciAccount().getUserId());
			metadata.setDescription(registration.getDescription());
			// TODO: ID Lookup
			metadata.setCreatorName(registration.getCreatorId());
			metadata.setLabBranch(registration.getBranchName());
			metadata.getMetadataItems().addAll(eItems);
			String createdOn = registration.getCreatedOn();
			if(createdOn == null || createdOn.trim().length() == 0)
				metadata.setOriginallyCreated(Calendar.getInstance());
			else
			{
				DateFormat formatter = new SimpleDateFormat("mm/dd/yyyy");
				Date date;
				try {
					date = formatter.parse(createdOn);
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(date);
					metadata.setOriginallyCreated(calendar);
				} catch (ParseException e) {
					e.printStackTrace();
					ObjectError error = new ObjectError("hpcRegistration",
							"Failed to parse date");
					bindingResult.addError(error);
					model.addAttribute("registrationStatus", false);
					model.addAttribute("error",
							"Failed to parse date "+createdOn);
					return "datasetRegistration";
					
				}
			}
			//TODO: Pull this information based on NCI UserId
			metadata.setPrincipalInvestigatorDOC("NCI/CANCER GENETICS BRANCH");
			metadata.setRegistrarDOC("NCI/CANCER GENETICS BRANCH");
			upload.setMetadata(metadata);
			dto.getUploadRequests().add(upload);
		}

		try {
			ResponseEntity<HpcExceptionDTO> response = restTemplate.postForEntity(
					serviceURL, dto, HpcExceptionDTO.class);
			if(!response.getStatusCode().equals(HttpStatus.CREATED))
			{
				ObjectError error = new ObjectError("hpcRegistration",
						"Failed to register dataset");
				bindingResult.addError(error);
				model.addAttribute("registrationStatus", false);
				model.addAttribute("error",
						"Failed to register your request due to: " +response.getBody());
				return "datasetRegistration";
				
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
			return "datasetRegistration";
		} catch (RestClientException e) {
			ObjectError error = new ObjectError("hpcRegistration",
					"Failed to register: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("registrationStatus", false);
			model.addAttribute("error",
					"Failed to register your request due to: " + e.getMessage());
			return "datasetRegistration";
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcRegistration",
					"Failed to register: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("registrationStatus", false);
			model.addAttribute("error",
					"Failed to register your request due to: " + e.getMessage());
			return "datasetRegistration";
		} finally {
			Map<String, String> users = Util.getPIs();
			model.addAttribute("piList", users);
			model.addAttribute("userAttrs", Util.getUserDefinedPrimaryMedataAttrs());
		}
		return "datasetRegisterResult";
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
