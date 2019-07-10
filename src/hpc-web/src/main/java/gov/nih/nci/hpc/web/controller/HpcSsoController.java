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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.hpc.web.util.HpcClientUtil;

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

	@RequestMapping(method = RequestMethod.GET)
	public String home(@CookieValue(value = "NIHSMSESSION", required = false) String smSession, Model model,
			@RequestParam(value = "redirect_uri", required = true) String redirectURL, HttpSession session,
			HttpServletRequest request) {
		String userId = (String) session.getAttribute("hpcUserId");
		String authToken = "";
		if (!StringUtils.isBlank(userId)) {
			try {
				authToken = HpcClientUtil.getAuthenticationTokenSso(userId, smSession, authenticateURL);
				session.setAttribute("hpcUserToken", authToken);
			} catch (Exception e) {
				logger.error("Authentication failed. " + e.getMessage());
			}
		}
		model.addAttribute("redirectURL", redirectURL);
		model.addAttribute("token", authToken);
		return "sso";
	}

}
