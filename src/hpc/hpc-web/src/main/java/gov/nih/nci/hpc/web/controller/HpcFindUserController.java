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
import org.springframework.web.bind.annotation.RequestParam;

import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.dto.security.HpcUserListDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcWebUser;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

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
@RequestMapping("/finduser")
public class HpcFindUserController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.user}")
	private String userServiceURL;

	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q, @RequestParam String path, @RequestParam String type,  Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}
		HpcWebUser webUser = new HpcWebUser();
		webUser.setPath(path);
		webUser.setType(type);
		model.addAttribute("hpcWebUser", webUser);
		session.removeAttribute("selectedGroups");
		session.removeAttribute("selectedUsers");
		return "finduser";
	}

	/*
	 * Action for Dataset registration
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String findUsers(@Valid @ModelAttribute("hpcUser") HpcWebUser hpcWebUser, BindingResult bindingResult,
			Model model, HttpSession session, HttpServletRequest request) {
		try {
			String path = (String) session.getAttribute("permissionsPath");
			String[] actionType = request.getParameterValues("actionType");
			if (actionType != null && actionType.length > 0 && actionType[0].equals("selected")) {
				String[] selectedUsers = request.getParameterValues("selectedUsers");
				StringBuffer buffer = new StringBuffer();
				for (int i = 0; i < selectedUsers.length; i++) {
					StringTokenizer tokens = new StringTokenizer(selectedUsers[i], ",");
					while (tokens.hasMoreTokens()) {
						buffer.append(tokens.nextToken());
						if (tokens.hasMoreTokens())
							buffer.append(";");
					}
				}
				session.setAttribute("selectedUsers", buffer.toString());
				if (selectedUsers != null && selectedUsers.length > 0)
					return "redirect:/permissions?assignType=User&path=" + hpcWebUser.getPath()  + "&type="+hpcWebUser.getType();
			} else if (actionType != null && actionType.length > 0 && actionType[0].equals("cancel")) {
				session.removeAttribute("selectedUsers");
				return "redirect:/permissions?assignType=User&path=" + hpcWebUser.getPath() + "&type="+hpcWebUser.getType();
			}

			String userId = null;
			String firstName = null;
			String lastName = null;
			if(hpcWebUser.getNciUserId() != null && hpcWebUser.getNciUserId().trim().length() > 0)
				userId = hpcWebUser.getNciUserId();
			if(hpcWebUser.getFirstName() != null && hpcWebUser.getFirstName().trim().length() > 0)
				firstName = hpcWebUser.getFirstName();
			if(hpcWebUser.getLastName() != null && hpcWebUser.getLastName().trim().length() > 0)
				lastName = hpcWebUser.getLastName();
			
			String authToken = (String) session.getAttribute("hpcUserToken");
			HpcUserListDTO users = HpcClientUtil.getUsers(authToken, userServiceURL, userId,
					firstName, lastName, sslCertPath, sslCertPassword);
			if (users != null && users.getNciAccounts() != null && users.getNciAccounts().size() > 0)
				model.addAttribute("searchresults", users.getNciAccounts());
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcDatasetSearch", "Failed to search by name: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search by name: " + e.getMessage());
			return "finduser";
		}
		model.addAttribute("hpcWebUser", hpcWebUser);
		return "finduser";
	}
}
