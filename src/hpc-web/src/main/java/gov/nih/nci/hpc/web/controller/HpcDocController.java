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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDocDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcDocModel;
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
	private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

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

	@GetMapping("/model")
	public String model(@RequestParam(value = "basePath", required = false) String basePath, Model model, HttpSession session) {
		if (!validateSession(model, session)) {
			return "login";
		}
		final String authToken = (String) session.getAttribute("hpcUserToken");
		final String userId = (String) session.getAttribute("hpcUserId");
		try {
			HpcDataManagementModelDTO modelDTO = getModelDTO(session);
			HpcPermission[] hpcPermissions = {HpcPermission.OWN, HpcPermission.WRITE, HpcPermission.READ};
			populateUserBasePaths(modelDTO, authToken, userId, hpcPermissions, "basePaths",
					sslCertPath, sslCertPassword, session, hpcModelBuilder);
			Set<String> basePaths = (Set<String>) session.getAttribute("basePaths");
			String effectiveBasePath = resolveEffectiveBasePath(basePath, session);
			HpcDocModel docModel = toDocModel(modelDTO, effectiveBasePath);
			if (basePaths == null) {
				basePaths = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			}
			if (basePaths.isEmpty() && StringUtils.hasText(docModel.getBasePath())) {
				basePaths.add(docModel.getBasePath());
			}
			model.addAttribute("docModel", docModel);
			model.addAttribute("basePaths", basePaths);
		} catch (Exception e) {
			model.addAttribute("docModel", new HpcDocModel());
			model.addAttribute("basePaths", new ArrayList<String>());
			model.addAttribute("error", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return "docmodel";
	}

	@PostMapping("/model")
	public String updateModel(@Valid @ModelAttribute("docModel") HpcDocModel docModel,
			Model model, BindingResult bindingResult, HttpSession session) {
		if (!validateSession(model, bindingResult, session)) {
			return "login";
		}
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null || !"SYSTEM_ADMIN".equals(user.getUserRole())) {
			model.addAttribute("error", "Only SYSTEM_ADMIN users can update models.");
			model.addAttribute("basePaths", new ArrayList<String>());
			return "docmodel";
		}
		final String authToken = (String) session.getAttribute("hpcUserToken");
		final String userId = (String) session.getAttribute("hpcUserId");
		try {
			HpcClientUtil.updateDOCModel(authToken, this.hpcModelURL, docModel.getBasePath(), docModel.getDataHierarchy(),
					docModel.getCollectionMetadataValidationRules(), docModel.getDataObjectMetadataValidationRules(),
					this.sslCertPath, this.sslCertPassword);
			hpcModelBuilder.updateDOCModel(authToken, this.hpcModelURL, this.sslCertPath, this.sslCertPassword);
			HpcDataManagementModelDTO modelDTO = HpcClientUtil.getDOCModel(authToken, this.hpcModelURL,
					this.sslCertPath, this.sslCertPassword);
			session.setAttribute("userDOCModel", modelDTO);
			HpcPermission[] hpcPermissions = {HpcPermission.OWN, HpcPermission.WRITE, HpcPermission.READ};
			populateUserBasePaths(modelDTO, authToken, userId, hpcPermissions, "basePaths",
					sslCertPath, sslCertPassword, session, hpcModelBuilder);
			Set<String> basePaths = (Set<String>) session.getAttribute("basePaths");
			if (basePaths == null) {
				basePaths = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			}
			if (basePaths.isEmpty() && StringUtils.hasText(docModel.getBasePath())) {
				basePaths.add(docModel.getBasePath());
			}
			model.addAttribute("docModel", toDocModel(modelDTO, docModel.getBasePath()));
			model.addAttribute("basePaths", basePaths);
			model.addAttribute("success", "Model updated successfully.");
		} catch (Exception e) {
			model.addAttribute("docModel", docModel);
			model.addAttribute("basePaths", new ArrayList<String>());
			model.addAttribute("error", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return "docmodel";
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

	private boolean validateSession(Model model, BindingResult bindingResult, HttpSession session) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			model.addAttribute("hpcLogin", new HpcLogin());
			return false;
		}
		return true;
	}

	private boolean validateSession(Model model, HttpSession session) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			model.addAttribute("hpcLogin", new HpcLogin());
			return false;
		}
		return true;
	}

	private HpcDocModel toDocModel(HpcDataManagementModelDTO modelDTO, String basePath) throws Exception {
		if (modelDTO == null || modelDTO.getDocRules() == null || modelDTO.getDocRules().isEmpty()) {
			throw new HpcWebException("No data management model found for base path: " + basePath);
		}
		HpcDocDataManagementRulesDTO docRules = null;
		HpcDataManagementRulesDTO rules = null;
		for (HpcDocDataManagementRulesDTO docRule : modelDTO.getDocRules()) {
			if (docRule == null || docRule.getRules() == null) {
				continue;
			}
			for (HpcDataManagementRulesDTO rule : docRule.getRules()) {
				if (rule == null) {
					continue;
				}
				if (!StringUtils.hasText(basePath) || basePath.equals(rule.getBasePath())) {
					docRules = docRule;
					rules = rule;
					break;
				}
			}
			if (rules != null) {
				break;
			}
		}
		if (rules == null || docRules == null) {
			throw new HpcWebException("No data management model found for base path: " + basePath);
		}
		HpcDocModel docModel = new HpcDocModel();
		docModel.setDoc(docRules.getDoc());
		docModel.setBasePath(rules.getBasePath());
		docModel.setDataHierarchy(objectMapper.writeValueAsString(rules.getDataHierarchy()));
		docModel.setCollectionMetadataValidationRules(
				objectMapper.writeValueAsString(rules.getCollectionMetadataValidationRules()));
		docModel.setDataObjectMetadataValidationRules(
				objectMapper.writeValueAsString(rules.getDataObjectMetadataValidationRules()));
		return docModel;
	}

	private String resolveEffectiveBasePath(String basePath, HttpSession session) {
		if (StringUtils.hasText(basePath)) {
			return basePath;
		}
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user != null && StringUtils.hasText(user.getDefaultBasepath())) {
			return user.getDefaultBasepath();
		}
		return null;
	}
	

}
