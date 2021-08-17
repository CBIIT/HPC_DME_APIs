/**
 * HpcSSOController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gov.nih.nci.hpc.web.util.HpcClientUtil;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * SSO controller to provide HPC token back to a caller
 * </p>
 *
 * @author <a href="mailto:Yuri.Dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/sso")
public class HpcSsoController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.user}")
	private String serviceUserURL;
	@Value("${gov.nih.nci.hpc.server.user.authenticate}")
	private String authenticateURL;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	@GetMapping
	public String home(@CookieValue(value = "NIHSMSESSION", required = false) String smSession, Model model,
			@RequestParam(value = "redirect_uri", required = true) String redirectURL,
			@RequestParam(value = "urlencoded", required = false) Boolean urlencoded,
			@RequestParam(value = "json", required = false) Boolean json, HttpSession session,
			HttpServletRequest request) {
		// Default to text/plain enctype instead of application/x-www-form-urlencoded
		if(urlencoded == null)
			urlencoded = false;
		// Default json to true if urlencoded is false
		if(Boolean.FALSE.equals(urlencoded) && json == null)
			json = true;
		String userId = (String) session.getAttribute("hpcUserId");
		String authToken = "";
		if (!StringUtils.isBlank(userId)) {
			try {
				authToken = HpcClientUtil.getAuthenticationTokenSso(userId, smSession, authenticateURL);
				session.setAttribute("hpcUserToken", authToken);
			} catch (Exception e) {
				logger.error("Authentication failed. {}", e.getMessage());
			}
		}
		// This is to support posting the token in format, {"token": "ABC123", "ignoreme": "="} using text/plain encoding.
		String formattedToken = "<input type='hidden' name='{\"token\": \"" + authToken + "\", \"ignoreme\": \"' value='\"}'/>";
		String jsonToken = "{\"token\": \"" + authToken + "\"}";
		model.addAttribute("formattedToken", formattedToken);
		model.addAttribute("jsonToken", jsonToken);
		model.addAttribute("redirectURL", redirectURL);
		model.addAttribute("token", authToken);
		model.addAttribute("urlencoded", urlencoded);
		model.addAttribute("json", json);
		return "sso";
	}

}
