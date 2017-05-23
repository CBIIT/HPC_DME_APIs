/**
 * HpcFindUserController.java
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
 * Controller to find users when assigning permissions to a collection or data
 * file
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/finduser")
public class HpcFindUserController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.user.all}")
	private String allUsersServiceURL;
	@Value("${gov.nih.nci.hpc.server.user.active}")
	private String activeUsersServiceURL;

	/**
	 * GET action to display find user page
	 * 
	 * @param q
	 * @param source
	 * @param path
	 * @param type
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q, @RequestParam String source, @RequestParam String path,
			@RequestParam String type, Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
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
		webUser.setSource(source);
		model.addAttribute("hpcWebUser", webUser);
		session.removeAttribute("selectedGroups");
		session.removeAttribute("selectedUsers");
		return "finduser";
	}

	/**
	 * POST action to find users. If users are selected, put them user session
	 * and redirect back to source page
	 * 
	 * @param hpcWebUser
	 * @param bindingResult
	 * @param model
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String findUsers(@Valid @ModelAttribute("hpcUser") HpcWebUser hpcWebUser, BindingResult bindingResult,
			Model model, HttpSession session, HttpServletRequest request) {
		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");

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
					if (hpcWebUser.getType() != null && hpcWebUser.getType().equals("group"))
						return "redirect:/" + hpcWebUser.getSource() + "?assignType=User&groupName="
								+ hpcWebUser.getPath() + "&type=" + hpcWebUser.getType();
					else
						return "redirect:/" + hpcWebUser.getSource() + "?assignType=User&path=" + hpcWebUser.getPath()
								+ "&type=" + hpcWebUser.getType();
			} else if (actionType != null && actionType.length > 0 && actionType[0].equals("cancel")) {
				session.removeAttribute("selectedUsers");
				return "redirect:/" + hpcWebUser.getSource() + "?assignType=User&path=" + hpcWebUser.getPath()
						+ "&type=" + hpcWebUser.getType();
			}

			String userId = null;
			String firstName = null;
			String lastName = null;
			if (hpcWebUser.getNciUserId() != null && hpcWebUser.getNciUserId().trim().length() > 0)
				userId = hpcWebUser.getNciUserId();
			if (hpcWebUser.getFirstName() != null && hpcWebUser.getFirstName().trim().length() > 0)
				firstName = hpcWebUser.getFirstName();
			if (hpcWebUser.getLastName() != null && hpcWebUser.getLastName().trim().length() > 0)
				lastName = hpcWebUser.getLastName();

			String serviceUrl = null;
			if (user.getUserRole().equals(SYSTEM_ADMIN))
				serviceUrl = allUsersServiceURL;
			else
				serviceUrl = activeUsersServiceURL;

			HpcUserListDTO users = HpcClientUtil.getUsers(authToken, serviceUrl, userId, firstName, lastName, null,
					sslCertPath, sslCertPassword);
			if (users != null && users.getUsers() != null && users.getUsers().size() > 0)
				model.addAttribute("searchresults", users.getUsers());
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
