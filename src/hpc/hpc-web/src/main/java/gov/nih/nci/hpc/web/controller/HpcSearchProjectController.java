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

import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.metadata.HpcCompressionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcEncryptionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcPHIContent;
import gov.nih.nci.hpc.domain.metadata.HpcPIIContent;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcProjectType;
import gov.nih.nci.hpc.dto.project.HpcProjectCollectionDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectMetadataDTO;
import gov.nih.nci.hpc.dto.dataset.HpcFilePrimaryMetadataDTO;
import gov.nih.nci.hpc.web.HpcResponseErrorHandler;
import gov.nih.nci.hpc.web.model.HpcProjectSearch;
import gov.nih.nci.hpc.web.util.Util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpEntity;
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
 * HPC DM Project Search controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDataRegistrationController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/projectsearch")
public class HpcSearchProjectController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.project.query.primarymetadata}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.project.query.id}")
	private String serviceIdURL;
	@Value("${gov.nih.nci.hpc.server.project.query.datasetid}")
	private String serviceDatasetIdURL;

	/*
	 * Action for Datset registration page
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(Model model) {
		HpcProjectSearch hpcProjectSearch = new HpcProjectSearch();
		model.addAttribute("hpcProjectSearch", hpcProjectSearch);
		Map<String, String> users = Util.getPIs();
		model.addAttribute("piList", users);

		return "searchproject";
	}

	@RequestMapping(value = "/id", method = RequestMethod.POST)
	public String searchById(
			@Valid @ModelAttribute("hpcProjectSearch") HpcProjectSearch search,
			Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new HpcResponseErrorHandler());

		try {
			HttpEntity<HpcProjectDTO> response = restTemplate.getForEntity(
					serviceIdURL + "/" + search.getId(), HpcProjectDTO.class);

			HpcProjectDTO dto = response.getBody();
			List<HpcProjectDTO> results = new ArrayList<HpcProjectDTO>();
			results.add(dto);
			List<HpcProjectSearch> searchResults = transformResults(results);
			model.addAttribute("projectsearchresults", searchResults);
		} catch (HttpStatusCodeException e) {
			ObjectError error = new ObjectError("hpcLogin",
					"Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error",
					"Failed to search projects due to: " + e.getMessage());
			return "searchprojectresult";
		} catch (RestClientException e) {
			ObjectError error = new ObjectError("hpcLogin",
					"Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error",
					"Failed to search projects due to: " + e.getMessage());
			return "searchprojectresult";
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcLogin",
					"Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error",
					"Failed to search projects due to: " + e.getMessage());
			return "searchprojectresult";
		} finally {
			Map<String, String> users = Util.getPIs();
			model.addAttribute("piList", users);
		}
		return "searchprojectresult";
	}

	@RequestMapping(value = "/datasetid", method = RequestMethod.POST)
	public String searchByProjectId(
			@Valid @ModelAttribute("hpcProjectSearch") HpcProjectSearch search,
			Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new HpcResponseErrorHandler());

		try {
			HttpEntity<HpcProjectDTO> response = restTemplate.getForEntity(
					serviceDatasetIdURL + "/" + search.getSearchDatasetId(), HpcProjectDTO.class);

			HpcProjectDTO dto = response.getBody();
			List<HpcProjectDTO> results = new ArrayList<HpcProjectDTO>();
			results.add(dto);
			List<HpcProjectSearch> searchResults = transformResults(results);
			model.addAttribute("projectsearchresults", searchResults);
		} catch (HttpStatusCodeException e) {
			ObjectError error = new ObjectError("hpcLogin",
					"Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error",
					"Failed to search projects due to: " + e.getMessage());
			return "searchprojectresult";
		} catch (RestClientException e) {
			ObjectError error = new ObjectError("hpcLogin",
					"Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error",
					"Failed to search projects due to: " + e.getMessage());
			return "searchprojectresult";
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcLogin",
					"Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error",
					"Failed to search projects due to: " + e.getMessage());
			return "searchprojectresult";
		} finally {
			Map<String, String> users = Util.getPIs();
			model.addAttribute("piList", users);
		}
		return "searchprojectresult";
	}
	
	/*
	 * Action for Project registration
	 */
	@RequestMapping(value = "/metadata", method = RequestMethod.POST)
	public String search(
			@Valid @ModelAttribute("hpcProjectSearch") HpcProjectSearch search,
			Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new HpcResponseErrorHandler());
		HpcProjectMetadataDTO queryDTO = new HpcProjectMetadataDTO();
		HpcProjectMetadata metadata = new HpcProjectMetadata();
		if(search.getProjectName() != null && search.getProjectName().trim().length() > 0)
			metadata.setName(search.getProjectName());
		if(search.getDescription() != null && search.getDescription().trim().length() > 0)
			metadata.setDescription(search.getDescription());
		if(search.getInternalProjectId() != null && search.getInternalProjectId().trim().length() > 0)
			metadata.setInternalProjectId(search.getInternalProjectId());
		if(search.getBranchName() != null && search.getBranchName().trim().length() > 0)
			metadata.setLabBranch(search.getBranchName());
		if(search.getProjectName() != null && search.getProjectName().trim().length() > 0)
			metadata.setName(search.getProjectName());
		String type = search.getProjectType();
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
		if(search.getInvestigatorId() != null && !search.getInvestigatorId().equals("-1") && search.getInvestigatorId().trim().length() > 0)
			metadata.setPrincipalInvestigatorNciUserId(search.getInvestigatorId());
		if(search.getRegistarId() != null && search.getRegistarId().trim().length() > 0)
			metadata.setRegistrarNciUserId(search.getRegistarId());
		queryDTO.setMetadata(metadata);

		try {
			HttpEntity<HpcProjectCollectionDTO> response = restTemplate
					.postForEntity(serviceURL, queryDTO,
							HpcProjectCollectionDTO.class);
			HpcProjectCollectionDTO results = response.getBody();
			List<HpcProjectSearch> searchResults = transformResults(results.getHpcProjectDTO());
			model.addAttribute("projectsearchresults", searchResults);
		} catch (HttpStatusCodeException e) {
			ObjectError error = new ObjectError("hpcLogin",
					"Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error",
					"Failed to search projects due to: " + e.getMessage());
			return "searchprojectresult";
		} catch (RestClientException e) {
			ObjectError error = new ObjectError("hpcLogin",
					"Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error",
					"Failed to search projects due to: " + e.getMessage());
			return "searchprojectresult";
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcLogin",
					"Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error",
					"Failed to search projects due to: " + e.getMessage());
			return "searchprojectresult";
		}  finally {
			Map<String, String> users = Util.getPIs();
			model.addAttribute("piList", users);
			model.addAttribute("creatorList", users);
		}
		return "searchprojectresult";
	}
	
	private List<HpcProjectSearch> transformResults(List<HpcProjectDTO> results)
	{

		if(results == null || results.size() == 0)
			return null;
		
		List<HpcProjectSearch> searchResults = new ArrayList<HpcProjectSearch>();
		for(HpcProjectDTO dto : results)
		{
				HpcProjectSearch searchResult = new HpcProjectSearch();
				searchResult.setId(dto.getId());
				searchResult.setProjectName(dto.getMetadata().getName());
				searchResult.setDescription(dto.getMetadata().getDescription());
				searchResult.setProjectType(dto.getMetadata().getType().value());
				searchResult.setInvestigatorId(dto.getMetadata().getPrincipalInvestigatorNciUserId());
				searchResult.setBranchName(dto.getMetadata().getLabBranch());
				searchResult.setRegistarId(dto.getMetadata().getRegistrarNciUserId());
				searchResult.setHpcDatasetCollectionDTO(dto.getHpcDatasetCollectionDTO());
				DateFormat dateFormat = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
				Calendar created = dto.getCreated();
				if(created != null)
				   searchResult.setCreatedOn(dateFormat.format(created.getTime()));
				searchResults.add(searchResult);
		}
		return searchResults;
	}
}
