/**
 * HpcSavedSearchListController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.annotation.JsonView;

import gov.nih.nci.hpc.web.model.HpcSaveSearch;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Controller to get list of saved search names. This list is displayed on
 * Dashboard
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/deleteSearch")
public class HpcDeleteSearchController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.query}")
	private String queryServiceURL;

	/**
	 * GET action to delete user saved search
	 * 
	 * @param search
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@JsonView(Views.Public.class)
	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String get(@Valid @ModelAttribute("hpcSaveSearch") HpcSaveSearch search, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			final RedirectAttributes redirectAttributes) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		try {
			String searchName = request.getParameter("queryName");

			boolean deleted = HpcClientUtil.deleteSearch(authToken, queryServiceURL, searchName, sslCertPath,
					sslCertPassword);
			if (deleted) {
				redirectAttributes.addFlashAttribute("error", "Search " + searchName + " is deleted!");
			}
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to update data file: " + e.getMessage());
		}
		return "dashboard";
	}
}
