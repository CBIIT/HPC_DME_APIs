/**
 * HpcFindGroupController.java
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
import org.springframework.web.bind.annotation.RequestParam;

import gov.nih.nci.hpc.dto.security.HpcGroupListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcWebGroup;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Controller to find groups when assigning permissions to a collection or data
 * file
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/findgroup")
public class HpcFindGroupController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.group}")
	private String groupServiceURL;

	/**
	 * GET action to display find groups page
	 * 
	 * @param q
	 * @param path
	 * @param type
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q, @RequestParam String path, @RequestParam String type,
			Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}
		HpcWebGroup webGroup = new HpcWebGroup();
		webGroup.setPath(path);
		webGroup.setType(type);
		model.addAttribute("hpcWebGroup", webGroup);
		session.removeAttribute("selectedGroups");
		session.removeAttribute("selectedUsers");
		return "findgroup";
	}

	/**
	 * POST action to find Groups. If groups are selected, put them user session
	 * and redirect back to source page
	 * 
	 * @param hpcWebGroup
	 * @param bindingResult
	 * @param model
	 * @param session
	 * @param request
	 * @return
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
					return "redirect:/permissions?assignType=Group&path=" + hpcWebGroup.getPath() + "&type="
							+ hpcWebGroup.getType();
			} else if (actionType != null && actionType.length > 0 && actionType[0].equals("cancel")) {
				session.removeAttribute("selectedGroups");
				return "redirect:/permissions?assignType=Group&path=" + hpcWebGroup.getPath() + "&type="
						+ hpcWebGroup.getType();
			}

			String groupName = null;
			if (hpcWebGroup.getGroupName() != null && hpcWebGroup.getGroupName().trim().length() > 0)
				groupName = hpcWebGroup.getGroupName();

			String authToken = (String) session.getAttribute("hpcUserToken");
			HpcGroupListDTO groups = HpcClientUtil.getGroups(authToken, groupServiceURL, groupName, sslCertPath,
					sslCertPassword);
			if (groups != null && groups.getGroups() != null && groups.getGroups().size() > 0)
				model.addAttribute("searchresults", groups.getGroups());
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcDatasetSearch", "Failed to search by name: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search by name: " + e.getMessage());
			return "findgroup";
		}
		model.addAttribute("hpcWebGroup", hpcWebGroup);
		return "findgroup";
	}
}
