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

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	@Value("${gov.nih.nci.hpc.server.collection.acl.user}")
	private String collectionAclURL;
	
	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
	
	@RequestMapping(method = RequestMethod.GET)
	public String home(@CookieValue(value="NIHSMSESSION", required = false) String smSession, Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
		session.setAttribute("callerPath", getCallerPath(request));
		String userId = (String) session.getAttribute("hpcUserId");
		if (StringUtils.isBlank(userId) || StringUtils.isBlank(smSession)) {
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			model.addAttribute("queryURL", queryURL);
			model.addAttribute("collectionURL", collectionURL);
			session.setAttribute("callerPath", getCallerPath(request));
			return "index";
		}
		try {
			String authToken = HpcClientUtil.getAuthenticationTokenSso(userId, smSession,
					authenticateURL);
			session.setAttribute("hpcUserToken", authToken);
			try {
				HpcUserDTO user = HpcClientUtil.getUser(authToken, serviceUserURL, sslCertPath, sslCertPassword);
				if (user == null)
					throw new HpcWebException("Invlaid User");
				session.setAttribute("hpcUser", user);
				logger.info("getting DOCModel for user: " + user.getFirstName() + " " + user.getLastName());			
				HpcDataManagementModelDTO modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath,
						sslCertPassword);
				
				if (modelDTO != null)
					session.setAttribute("userDOCModel", modelDTO);
				HpcClientUtil.populateBasePaths(session, model, modelDTO, authToken, userId,
						collectionAclURL, sslCertPath, sslCertPassword);
						
				
			} catch (HpcWebException e) {
				model.addAttribute("loginStatus", false);
				model.addAttribute("loginOutput", "Invalid login");
				ObjectError error = new ObjectError("hpcLogin", "Authentication failed. " + e.getMessage());
				return "notauthorized";
			}
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("loginStatus", false);
			model.addAttribute("loginOutput", "Invalid login" + e.getMessage());
			ObjectError error = new ObjectError("hpcLogin", "Authentication failed. " + e.getMessage());
			return "notauthorized";
		}
		model.addAttribute("queryURL", queryURL);
		String callerPath = (String) session.getAttribute("callerPath");
		session.removeAttribute("callerPath");
		
		if (callerPath == null)
			return "dashboard";
		else
			return "redirect:/" + callerPath;
	}

	@RequestMapping(method = RequestMethod.POST)
	public String login(@Valid @ModelAttribute("hpcLogin") HpcLogin hpcLogin, BindingResult bindingResult, Model model,
			HttpSession session, HttpServletRequest request) {
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
				logger.info("getting DOCModel for user: " + user.getFirstName() + " " + user.getLastName());			
				HpcDataManagementModelDTO modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath,
						sslCertPassword);
				
				if (modelDTO != null)
					session.setAttribute("userDOCModel", modelDTO);
				HpcClientUtil.populateBasePaths(session, model, modelDTO, authToken, hpcLogin.getUserId(),
						collectionAclURL, sslCertPath, sslCertPassword);
						
				
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
		String callerPath = (String) session.getAttribute("callerPath");
		session.removeAttribute("callerPath");
		if (callerPath == null)
			return "dashboard";
		else
			return "redirect:/" + callerPath;
	}

	private String getCallerPath(HttpServletRequest request) {
		Map<String, String[]> params = request.getParameterMap();
		StringBuffer buffer = new StringBuffer();
		if (params.isEmpty())
			return null;
		else {
			String[] returnPath = params.get("returnPath");
			if (returnPath == null || returnPath.length == 0)
				return null;
			else
				buffer.append(returnPath[0]);

			Iterator<String> iter = params.keySet().iterator();
			boolean first = true;
			while (iter.hasNext()) {
				String key = iter.next();
				if (key.equals("returnPath"))
					continue;
				String[] value = params.get(key);
				if (first) {
					buffer.append("?" + key + "=" + value[0]);
					first = false;
				} else
					buffer.append("&" + key + "=" + value[0]);
			}
		}
		return buffer.toString();
	}
}
