/**
 * HpcGroupController.java
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import gov.nih.nci.hpc.dto.security.HpcGroupListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcWebGroup;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Group home controller. Support search Group and display results.
 * 
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/group")
public class HpcGroupController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.group}")
	private String groupServiceURL;

	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/login?returnPath=group";
		}

		String returnToHome = request.getParameter("return");
		HpcWebGroup webGroup = new HpcWebGroup();
		model.addAttribute("hpcWebGroup", webGroup);
		session.removeAttribute("selectedUsers");
		if (returnToHome != null) {
			String groupName = (String) session.getAttribute("groupName");
			if (groupName != null) {
				webGroup.setGroupName(groupName);
				populateSearch(webGroup, bindingResult, model, session, request);
			}
		}
		return "managegroup";
	}

	/**
	 * POST action to search for group
	 * 
	 * @param hpcWebGroup
	 * @param bindingResult
	 * @param model
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String findGroups(@Valid @ModelAttribute("hpcGroup") HpcWebGroup hpcWebGroup, BindingResult bindingResult,
			Model model, HttpSession session, HttpServletRequest request) {
		model.addAttribute("hpcWebGroup", hpcWebGroup);
		return populateSearch(hpcWebGroup, bindingResult, model, session, request);
	}

	private String populateSearch(HpcWebGroup hpcWebGroup, BindingResult bindingResult, Model model,
			HttpSession session, HttpServletRequest request) {
		try {
			String groupName = null;
			if (hpcWebGroup.getGroupName() != null && hpcWebGroup.getGroupName().trim().length() > 0)
				groupName = hpcWebGroup.getGroupName();

			String authToken = (String) session.getAttribute("hpcUserToken");
			HpcGroupListDTO groups = HpcClientUtil.getGroups(authToken, groupServiceURL, groupName, sslCertPath,
					sslCertPassword);
			if (groups != null && groups.getGroups() != null && groups.getGroups().size() > 0) {
				session.setAttribute("groupName", hpcWebGroup.getGroupName());
				model.addAttribute("searchresults", groups.getGroups());
			} else
				model.addAttribute("error", "No matching results!");
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcDatasetSearch", "Failed to search by name: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search by name: " + e.getMessage());
			return "managegroup";
		}
		model.addAttribute("hpcWebGroup", hpcWebGroup);
		return "managegroup";
	}
}
