/**
 * HpcLoginController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Login controller to authenticate user and initialize user session
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/login")
public class HpcLoginController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.user}")
	private String serviceUserURL;
	@Value("${gov.nih.nci.hpc.server.user.authenticate}")
	private String authenticateURL;
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionURL;
	@Value("${gov.nih.nci.hpc.server.query}")
	private String queryURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;

	@RequestMapping(method = RequestMethod.GET)
	public String home(Model model) {
		HpcLogin hpcLogin = new HpcLogin();
		model.addAttribute("hpcLogin", hpcLogin);
		model.addAttribute("queryURL", queryURL);
		model.addAttribute("collectionURL", collectionURL);

		return "index";
	}

	@RequestMapping(method = RequestMethod.POST)
	public String login(@Valid @ModelAttribute("hpcLogin") HpcLogin hpcLogin, BindingResult bindingResult, Model model,
			HttpSession session) {
		if (bindingResult.hasErrors()) {
			return "index";
		}
		try {
			String authToken = HpcClientUtil.getAuthenticationToken(hpcLogin.getUserId(), hpcLogin.getPasswd(),
					authenticateURL);
			session.setAttribute("hpcUserToken", authToken);
			try {
				HpcUserDTO user = HpcClientUtil.getUser(authToken, serviceUserURL, sslCertPath, sslCertPassword);
				if (user == null)
					throw new HpcWebException("Invlaid User");
				session.setAttribute("hpcUser", user);
				session.setAttribute("hpcUserId", hpcLogin.getUserId());
				HpcDataManagementModelDTO modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, user.getDoc(),
						sslCertPath, sslCertPassword);
				if (modelDTO != null)
					session.setAttribute("userDOCModel", modelDTO);
			} catch (HpcWebException e) {
				model.addAttribute("loginStatus", false);
				model.addAttribute("loginOutput", "Invalid login");
				ObjectError error = new ObjectError("hpcLogin", "Authentication failed. " + e.getMessage());
				bindingResult.addError(error);
				model.addAttribute("hpcLogin", hpcLogin);
				return "index";
			}
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("loginStatus", false);
			model.addAttribute("loginOutput", "Invalid login" + e.getMessage());
			ObjectError error = new ObjectError("hpcLogin", "Authentication failed. " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}
		model.addAttribute("hpcLogin", hpcLogin);
		model.addAttribute("queryURL", queryURL);

		return "dashboard";
	}
}
