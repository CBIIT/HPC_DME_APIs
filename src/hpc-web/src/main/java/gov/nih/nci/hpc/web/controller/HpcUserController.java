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

import java.util.ArrayList;
import java.util.List;

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
import gov.nih.nci.hpc.dto.security.HpcUserListDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcWebUser;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * User controller to search users
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/user")
public class HpcUserController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.user.all}")
	private String allUsersServiceURL;
	@Value("${gov.nih.nci.hpc.server.user.active}")
	private String activeUsersServiceURL;
	

	/**
	 * Get Action to prepare Manage User page
	 * 
	 * @param q
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/login?returnPath=manageuser";
		}
		HpcWebUser webUser = new HpcWebUser();
		model.addAttribute("hpcWebUser", webUser);
		String authToken = (String) session.getAttribute("hpcUserToken");
		populateDOCs(model, authToken, user, session);
		
		return "manageuser";
	}

	/**
	 * POST action to search for users based on search criteria (UserId,
	 * FirstName, LastName)
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
		
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		try {

			String userId = null;
			String firstName = null;
			String lastName = null;
			String doc = null;
			if (hpcWebUser.getNciUserId() != null && hpcWebUser.getNciUserId().trim().length() > 0)
				userId = hpcWebUser.getNciUserId();
			if (hpcWebUser.getFirstName() != null && hpcWebUser.getFirstName().trim().length() > 0)
				firstName = hpcWebUser.getFirstName();
			if (hpcWebUser.getLastName() != null && hpcWebUser.getLastName().trim().length() > 0)
				lastName = hpcWebUser.getLastName();
			if (hpcWebUser.getDoc() != null && hpcWebUser.getDoc().trim().length() > 0)
				doc = hpcWebUser.getDoc();

			
			String serviceUrl = null;
			// System Admin can view all Users including inactive users
			if (user.getUserRole().equals(SYSTEM_ADMIN))
				serviceUrl = allUsersServiceURL;
			else
				serviceUrl = activeUsersServiceURL;

			HpcUserListDTO users = HpcClientUtil.getUsers(authToken, serviceUrl, userId, firstName, lastName, doc,
					sslCertPath, sslCertPassword);
			if (users != null && users.getUsers() != null && users.getUsers().size() > 0)
				model.addAttribute("searchresults", users.getUsers());
		} catch (Exception e) {
			ObjectError error = new ObjectError("hpcDatasetSearch", "Failed to search by name: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search by name: " + e.getMessage());
			return "manageuser";
		} finally {
			model.addAttribute("hpcWebUser", hpcWebUser);
			populateDOCs(model, authToken, user, session);
		}
		return "manageuser";
	}
}
