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

import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.metadata.HpcCompressionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcEncryptionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcPHIContent;
import gov.nih.nci.hpc.domain.metadata.HpcPIIContent;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;
import gov.nih.nci.hpc.dto.dataset.HpcFilePrimaryMetadataDTO;
import gov.nih.nci.hpc.web.HpcResponseErrorHandler;
import gov.nih.nci.hpc.web.model.HpcDatasetSearch;
import gov.nih.nci.hpc.web.model.HpcDatasetSearchResult;
import gov.nih.nci.hpc.web.util.Util;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDateRangeDTO;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
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
 * HPC DM Dataset Search controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDataRegistrationController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/searchdataset")
public class HpcSearchDatasetController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.dataset.query.primarymetadata}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.dataset}")
	private String serviceIdURL;
	@Value("${gov.nih.nci.hpc.nihfnlcr.name}")
	private String destinationEndpoint;
	
	/*
	 * Action for Datset registration page
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(Model model) {
		init(model);
		return "searchdataset";
	}

	private void init(Model model)
	{
		HpcDatasetSearch hpcDatasetSearch = new HpcDatasetSearch();
		model.addAttribute("hpcDatasetSearch", hpcDatasetSearch);
		Map<String, String> users = Util.getPIs();
		model.addAttribute("piList", users);
		model.addAttribute("creatorList", users);
	}
	/*
	 * Action for Dataset registration
	 */
	@RequestMapping(value = "/name", method = RequestMethod.POST)
	public String searchByName(
			@Valid @ModelAttribute("hpcDatasetSearch") HpcDatasetSearch search,
			Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new HpcResponseErrorHandler());

		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put("name", search.getDatasetName());
			HttpEntity<HpcDatasetCollectionDTO> response = restTemplate
					.getForEntity(
							serviceIdURL,
							HpcDatasetCollectionDTO.class, params);
			HpcDatasetCollectionDTO results = response.getBody();
			List<HpcDatasetSearchResult> searchResults = transformResults(results.getHpcDatasetDTO());
			model.addAttribute("datasetsearchresults",
					searchResults);
			model.addAttribute("datasetURL", serviceURL);
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcDatasetSearch",
					"Failed to search by name: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error",
					"Failed to search by name: " + e.getMessage());
			return "searchdataset";
		} finally {
			init(model);
		}
		return "searchdatasetresult";
	}

	/*
	 * Action for Dataset registration
	 */
	@RequestMapping(value = "/date", method = RequestMethod.POST)
	public String searchByDate(
			@Valid @ModelAttribute("hpcDatasetSearch") HpcDatasetSearch search,
			Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new HpcResponseErrorHandler());
		HpcDatasetRegistrationDateRangeDTO dto = new HpcDatasetRegistrationDateRangeDTO();
		Map<String, String> params = new HashMap<String, String>();
		if(search.getFromCreatedDate() != null)
		{
			params.put("from", search.getFromCreatedDate());
		}
		if(search.getToCreatedDate() != null)
		{
			params.put("to", search.getToCreatedDate());
		}

		try {
			HttpEntity<HpcDatasetCollectionDTO> response = restTemplate
					.getForEntity(
							serviceIdURL,
							HpcDatasetCollectionDTO.class, params);
			HpcDatasetCollectionDTO results = response.getBody();
			List<HpcDatasetSearchResult> searchResults = transformResults(results.getHpcDatasetDTO());
			model.addAttribute("datasetsearchresults",
					searchResults);
			model.addAttribute("datasetURL", serviceURL);
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcLogin",
					"Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error",
					"Failed to search due to: " + e.getMessage());
			return "searchdataset";
		} finally {
			init(model);
		}
		return "searchdatasetresult";
	}	
	@RequestMapping(value = "/id", method = RequestMethod.POST)
	public String searchById(
			@Valid @ModelAttribute("hpcDatasetSearch") HpcDatasetSearch search,
			Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new HpcResponseErrorHandler());

		try {
			HttpEntity<HpcDatasetDTO> response = restTemplate.getForEntity(
					serviceIdURL + "/" + search.getId(), HpcDatasetDTO.class);

			HpcDatasetDTO dto = response.getBody();
			List<HpcDatasetDTO> results = new ArrayList<HpcDatasetDTO>();
			results.add(dto);
			List<HpcDatasetSearchResult> searchResults = transformResults(results);
			model.addAttribute("datasetsearchresults", searchResults);
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcLogin",
					"Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error",
					"Failed to search due to: " + e.getMessage());
			return "searchdataset";
		} finally {
			init(model);
		}
		return "searchdatasetresult";
	}

	@RequestMapping(value = "/projectid", method = RequestMethod.POST)
	public String searchByProjectId(
			@Valid @ModelAttribute("hpcDatasetSearch") HpcDatasetSearch search,
			Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new HpcResponseErrorHandler());

		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put("projectId", search.getProjectId());
			
			HttpEntity<HpcDatasetDTO> response = restTemplate.getForEntity(
					serviceURL, HpcDatasetDTO.class, params);

			HpcDatasetDTO dto = response.getBody();
			List<HpcDatasetDTO> results = new ArrayList<HpcDatasetDTO>();
			results.add(dto);
			List<HpcDatasetSearchResult> searchResults = transformResults(results);
			model.addAttribute("datasetsearchresults", searchResults);
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcLogin",
					"Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error",
					"Failed to search due to: " + e.getMessage());
			return "searchdataset";
		} finally {
			init(model);
		}
		return "searchdatasetresult";
	}
	
	/*
	 * Action for Dataset registration
	 */
	@RequestMapping(value = "/primary", method = RequestMethod.POST)
	public String search(
			@Valid @ModelAttribute("hpcDatasetSearch") HpcDatasetSearch search,
			Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new HpcResponseErrorHandler());
		HpcFilePrimaryMetadataDTO queryDTO = new HpcFilePrimaryMetadataDTO();
		HpcFilePrimaryMetadata metadata = new HpcFilePrimaryMetadata();
		if (search.getEncrypted().equals("ENCRYPTED"))
			metadata.setDataEncrypted(HpcEncryptionStatus.ENCRYPTED);
		else if (search.getEncrypted().equals("NOT_ENCRYPTED"))
			metadata.setDataEncrypted(HpcEncryptionStatus.NOT_ENCRYPTED);
		else if (search.getEncrypted().equals("NOT_SPECIFIED"))
			metadata.setDataEncrypted(HpcEncryptionStatus.NOT_SPECIFIED);

		if (search.getPii().equals("PII_NOT_PRESENT"))
			metadata.setDataContainsPII(HpcPIIContent.PII_NOT_PRESENT);
		else if (search.getPii().equals("PII_PRESENT"))
			metadata.setDataContainsPII(HpcPIIContent.PII_PRESENT);
		else if (search.getPii().equals("NOT_SPECIFIED"))
			metadata.setDataContainsPII(HpcPIIContent.NOT_SPECIFIED);

		if (search.getCompressed().equals("COMPRESSED"))
			metadata.setDataCompressed(HpcCompressionStatus.COMPRESSED);
		else if (search.getCompressed().equals("NOT_COMPRESSED"))
			metadata.setDataCompressed(HpcCompressionStatus.NOT_COMPRESSED);
		else if (search.getCompressed().equals("NOT_SPECIFIED"))
			metadata.setDataCompressed(HpcCompressionStatus.NOT_SPECIFIED);

		if (search.getPhi().equals("PHI_NOT_PRESENT"))
			metadata.setDataContainsPHI(HpcPHIContent.PHI_NOT_PRESENT);
		else if (search.getPhi().equals("PHI_PRESENT"))
			metadata.setDataContainsPHI(HpcPHIContent.PHI_PRESENT);
		else if (search.getPhi().equals("NOT_SPECIFIED"))
			metadata.setDataContainsPHI(HpcPHIContent.NOT_SPECIFIED);

		if (search.getFundingOrganization() != null
				&& search.getFundingOrganization().trim().length() > 0)
			metadata.setFundingOrganization(search.getFundingOrganization());
		if (search.getInvestigatorId() != null
				&& search.getInvestigatorId().trim().length() > 0
				&& !search.getInvestigatorId().equals("-1"))
			metadata.setPrincipalInvestigatorNciUserId(search.getInvestigatorId());
		if (search.getRegistarId() != null
				&& search.getRegistarId().trim().length() > 0)
			metadata.setRegistrarNciUserId(search.getRegistarId());
		if (search.getDescription() != null
				&& search.getDescription().trim().length() > 0)
			metadata.setDescription(search.getDescription());
		if (search.getCreatorId() != null
				&& search.getCreatorId().trim().length() > 0)
			metadata.setCreatorName(search.getCreatorId());
		if (search.getBranchName() != null
				&& search.getBranchName().trim().length() > 0
				&& !search.getBranchName().equals("-1"))
			metadata.setLabBranch(search.getBranchName());
		queryDTO.setMetadata(metadata);

		try {
			HttpEntity<HpcDatasetCollectionDTO> response = restTemplate
					.postForEntity(serviceURL, queryDTO,
							HpcDatasetCollectionDTO.class);
			HpcDatasetCollectionDTO results = response.getBody();
			List<HpcDatasetSearchResult> searchResults = transformResults(results.getHpcDatasetDTO());
			model.addAttribute("datasetsearchresults", searchResults);
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcLogin",
					"Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error",
					"Failed to search due to: " + e.getMessage());
			return "searchdataset";
		} finally {
			init(model);
		}
		return "searchdatasetresult";
	}
	
	private List<HpcDatasetSearchResult> transformResults(List<HpcDatasetDTO> results)
	{

		if(results == null || results.size() == 0)
			return null;
		
		List<HpcDatasetSearchResult> searchResults = new ArrayList<HpcDatasetSearchResult>();
		for(HpcDatasetDTO dto : results)
		{
			List<HpcFile> files = dto.getFileSet().getFiles();
			for(HpcFile file : files)
			{
				HpcDatasetSearchResult searchResult = new HpcDatasetSearchResult();
				searchResult.setId(dto.getId());
				searchResult.setDatasetName(dto.getFileSet().getName());
				searchResult.setCreatorName(file.getMetadata().getPrimaryMetadata().getCreatorName());
				//searchResult.setDataCreationFacility(file.getMetadata().getPrimaryMetadata().);
				searchResult.setDescription(dto.getFileSet().getDescription());
				searchResult.setFileId(file.getId());
				searchResult.setFileType(file.getType().value());
				searchResult.setFundingOrganization(file.getMetadata().getPrimaryMetadata().getFundingOrganization());
				searchResult.setInvestigatorId(file.getMetadata().getPrimaryMetadata().getPrincipalInvestigatorNciUserId());
				searchResult.setLabBranch(file.getMetadata().getPrimaryMetadata().getLabBranch());
				searchResult.setRegistarId(file.getMetadata().getPrimaryMetadata().getRegistrarNciUserId());
				searchResult.setProjectIds(file.getProjectIds());
				DateFormat dateFormat = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
				Calendar created = file.getMetadata().getPrimaryMetadata().getOriginallyCreated();
				if(created != null)
				   searchResult.setCreatedOn(dateFormat.format(created.getTime()));
				searchResults.add(searchResult);
			}
			

		}
		return searchResults;
	}
}
