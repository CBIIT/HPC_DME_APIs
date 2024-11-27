/**
 * HpcDocController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fasterxml.jackson.annotation.JsonView;

import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcWebUser;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcModelBuilder;

/**
 * <p>
 * Controller to get DOC details
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDocController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/doc")
public class HpcDocController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server}")
	private String serverURL;
	@Value("${gov.nih.nci.hpc.server.user}")
	private String serviceUserURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;
	@Value("${gov.nih.nci.hpc.server.refresh.model}")
	private String hpcRefreshModelURL;
	@Value("${gov.nih.nci.hpc.server.refresh.investigator}")
	private String hpcRefreshInvestigatorURL;
	@Value("${gov.nih.nci.hpc.server.collection.acl}")
	private String collectionAclsURL;


	@Autowired
	private HpcModelBuilder hpcModelBuilder;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
		}
        final String authToken = (String) session.getAttribute("hpcUserToken");
	    final String userId = (String) session.getAttribute("hpcUserId");
	    log.info("userId: " + userId);

	    HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO)
	        session.getAttribute("userDOCModel");
	      if (modelDTO == null) {
	          modelDTO = HpcClientUtil.getDOCModel(authToken, this.hpcModelURL,
	            this.sslCertPath, this.sslCertPassword);
	          session.setAttribute("userDOCModel", modelDTO);
	      }
	      model.addAttribute("userDOCModel", modelDTO);

		return "doc";
	}

	@RequestMapping(method = RequestMethod.POST)
	public String update(@Valid @ModelAttribute("hpcUser") HpcWebUser hpcUser,
			Model model, BindingResult bindingResult, HttpSession session) {
		final String authToken = (String) session.getAttribute("hpcUserToken");
		session.setAttribute("hpcUserToken", authToken);
		final String userId = (String) session.getAttribute("hpcUserId");
		logger.info("Upgdating DOCModel for user: " + userId);

		//Refresh DOC Models on the server
		HpcClientUtil.refreshDOCModels(authToken, this.hpcRefreshModelURL,
	            this.sslCertPath, this.sslCertPassword);

		//Reload DOC Models
		HpcDataManagementModelDTO modelDTO =
            hpcModelBuilder.updateDOCModel(authToken, this.hpcModelURL, this.sslCertPath, this.sslCertPassword);
		session.setAttribute("userDOCModel", modelDTO);
		model.addAttribute("userDOCModel", modelDTO);

		return "doc";
	}
	
	@JsonView(Views.Public.class)
	@PostMapping(value = "/refreshInvestigators")
	@ResponseBody
	public AjaxResponseBody refreshInvestigators(@Valid @ModelAttribute("hpcUser") HpcWebUser hpcUser,
			Model model, BindingResult bindingResult, HttpSession session) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		AjaxResponseBody result = new AjaxResponseBody();

		try {
			//Refresh Investigators on the server
			boolean refreshed = HpcClientUtil.refreshInvestigators(authToken, this.hpcRefreshInvestigatorURL,
		            this.sslCertPath, this.sslCertPassword);
			
			if (refreshed) {
				result.setMessage("Investigators refreshed!");
				result.setCode("success");
			}

		} catch (Exception e) {
			result.setMessage(e.getMessage());
			logger.error(e.getMessage(), e);
		} 
		return result;
	}
	

}
