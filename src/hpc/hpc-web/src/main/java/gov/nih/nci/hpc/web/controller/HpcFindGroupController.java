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

import java.util.StringTokenizer;

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

import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcWebGroup;

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
@RequestMapping("/findgroup")
public class HpcFindGroupController extends AbstractHpcController {
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
			return "index";
		}
		HpcWebGroup webGroup = new HpcWebGroup();
		model.addAttribute("hpcWebGroup", webGroup);
		session.removeAttribute("selectedGroups");
		session.removeAttribute("selectedUsers");
		return "findgroup";
	}

	/*
	 * Action for Dataset registration
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String findUsers(@Valid @ModelAttribute("hpcGroup") HpcWebGroup hpcWebGroup, BindingResult bindingResult,
			Model model, HttpSession session, HttpServletRequest request) {
		try {
			String path = (String) session.getAttribute("permissionsPath");
			String[] actionType = request.getParameterValues("actionType");
			if (actionType != null && actionType.length > 0 && actionType[0].equals("selected")) {
				String[] selectedGroups = request.getParameterValues("selectedGroups");
				StringBuffer buffer = new StringBuffer();
				for (int i = 0; i < selectedGroups.length; i++) {
					StringTokenizer tokens = new StringTokenizer(selectedGroups[i], ",");
					while (tokens.hasMoreTokens()) {
						buffer.append(tokens.nextToken());
						if (tokens.hasMoreTokens())
							buffer.append(";");
					}
				}
				session.setAttribute("selectedGroups", buffer.toString());
				if (selectedGroups != null && selectedGroups.length > 0)
					return "redirect:/permissions?path=" + path;
			} else if (actionType != null && actionType.length > 0 && actionType[0].equals("cancel")) {
				session.removeAttribute("selectedGroups");
				return "redirect:/permissions?path=" + path;
			}

			String authToken = (String) session.getAttribute("hpcUserToken");
			// HpcGroupListDTO groups = HpcClientUtil.getGroups(authToken,
			// groupServiceURL, hpcWebGroup.getGroupId(),
			// sslCertPath, sslCertPassword);
			// if (groups != null)
			// model.addAttribute("searchresults", groups);
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcDatasetSearch", "Failed to search by name: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search by name: " + e.getMessage());
			return "finduser";
		}
		model.addAttribute("hpcWebGroup", hpcWebGroup);
		return "findgroup";
	}
}
