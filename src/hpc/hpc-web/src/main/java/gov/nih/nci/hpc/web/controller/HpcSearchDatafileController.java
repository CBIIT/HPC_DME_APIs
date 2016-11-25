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

import java.util.HashMap;
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
import org.springframework.web.client.RestTemplate;

import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.web.HpcResponseErrorHandler;
import gov.nih.nci.hpc.web.model.HpcDatafileSearch;
import gov.nih.nci.hpc.web.util.Util;

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
@RequestMapping("/searchdatafile")
public class HpcSearchDatafileController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.dataObject}")
	private String serviceURL;

	/*
	 * Action for Datset registration page
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(Model model) {
		init(model);
		return "searchdataset";
	}

	private void init(Model model) {
		HpcDatafileSearch hpcDatasetSearch = new HpcDatafileSearch();
		model.addAttribute("hpcDatasetSearch", hpcDatasetSearch);
		Map<String, String> users = Util.getPIs();
		model.addAttribute("piList", users);
		model.addAttribute("creatorList", users);
	}

	/*
	 * Action for Dataset registration
	 */
	@RequestMapping(value = "/name", method = RequestMethod.POST)
	public String searchByName(@Valid @ModelAttribute("hpcDatasetSearch") HpcDatafileSearch search, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new HpcResponseErrorHandler());

		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put("name", search.getDatasetName());
			HttpEntity<HpcDataObjectListDTO> response = restTemplate.getForEntity(serviceURL,
					HpcDataObjectListDTO.class, params);
			HpcDataObjectListDTO results = response.getBody();
			List<String> searchResults = results.getDataObjectPaths();
			model.addAttribute("datasetsearchresults", searchResults);
			model.addAttribute("datasetURL", serviceURL);
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcDatasetSearch", "Failed to search by name: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search by name: " + e.getMessage());
			return "searchdataset";
		} finally {
			init(model);
		}
		return "searchdatasetresult";
	}

}
