/**
 * HpcCollectionController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcCollectionModel;
import gov.nih.nci.hpc.web.model.HpcDatafileModel;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcMetadataAttrEntry;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Add data file controller.
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcCreateDatafileController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/addDatafile")
public class HpcCreateDatafileController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.dataObject}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionServiceURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;

	/**
	 * Get selected collection details from its path
	 *
	 * @param body
	 * @param path
	 * @param action
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String body, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		try {
			String path = request.getParameter("path");
			String parent = request.getParameter("parent");
			if (parent != null)
				model.addAttribute("parent", parent);
			if (path != null)
				model.addAttribute("datafilePath", path);
			else
				model.addAttribute("datafilePath", parent);

			String source = request.getParameter("source");
			if (source == null || source.isEmpty())
				source = (String) request.getAttribute("source");
			if (source == null || source.isEmpty())
				source = "dashboard";

			model.addAttribute("source", source);
			String authToken = (String) session.getAttribute("hpcUserToken");

			HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
			if (modelDTO == null) {
				modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
				session.setAttribute("userDOCModel", modelDTO);
			}

			String basePath = null;
			if(parent != null)
				basePath = HpcClientUtil.getBasePath(authToken, collectionServiceURL, parent, sslCertPath, sslCertPassword, modelDTO);
			else
				basePath = HpcClientUtil.getBasePath(request);
			// User Session validation

			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			String userId = (String) session.getAttribute("hpcUserId");
			if (user == null || authToken == null) {
				ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
				bindingResult.addError(error);
				HpcLogin hpcLogin = new HpcLogin();
				model.addAttribute("hpcLogin", hpcLogin);
				return "index";
			}
			if(parent == null || basePath == null)
				populateBasePaths(request, session, model, path);
			else
				setDatafilePath(model, request, parent);
			populateFormAttributes(request, session, model, basePath, false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			model.addAttribute("error", "Failed to initialize add data file: " + e.getMessage());
			e.printStackTrace();
		}
		model.addAttribute("hpcDatafile", new HpcDatafileModel());
		return "adddatafile";
	}

	private void populateBasePaths(HttpServletRequest request, HttpSession session, Model model, String path) throws HpcWebException {
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		Set<String> basePaths = (Set<String>) session.getAttribute("basePaths");
		String userId = (String) session.getAttribute("hpcUserId");
		if(basePaths == null || basePaths.isEmpty())
		{
			HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
			if (modelDTO == null) {
				modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
				session.setAttribute("userDOCModel", modelDTO);
			}

			basePaths = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			List<HpcDataManagementRulesDTO> docRules = HpcClientUtil.getUserDOCManagementRules(modelDTO, user.getDoc());
			for (HpcDataManagementRulesDTO docRule : docRules) {
				HpcUserPermissionDTO permission = HpcClientUtil.getPermissionForUser(authToken, docRule.getBasePath(),  userId,
						collectionServiceURL, sslCertPath, sslCertPassword);
				if(permission != null && permission.getPermission() != null && (permission.getPermission().equals(HpcPermission.WRITE) || permission.getPermission().equals(HpcPermission.OWN)))
					basePaths.add(docRule.getBasePath());
			}
			session.setAttribute("basePaths", basePaths);
		}

		String selectedBasePath = HpcClientUtil.getBasePath(request);
		if (selectedBasePath == null)
			selectedBasePath = (String) session.getAttribute("basePathSelected");
		else
			session.setAttribute("basePathSelected", selectedBasePath);
		model.addAttribute("basePathSelected", selectedBasePath);

		setDatafilePath(model, request, path);
		model.addAttribute("basePaths", basePaths);
	}

	private void setDatafilePath(Model model, HttpServletRequest request, String parentPath)
	{
		String path = request.getParameter("path");
		if(path != null && !path.isEmpty())
			model.addAttribute("datafilePath", request.getParameter("path"));

		if(parentPath == null || parentPath.isEmpty())
		{
			String[] basePathValues = request.getParameterValues("basePath");
			String basePath = null;
			if (basePathValues == null || basePathValues.length == 0)
				basePath = (String) request.getAttribute("basePath");
			else
				basePath = basePathValues[0];
			model.addAttribute("datafilePath", basePath);
		}
		else
			model.addAttribute("datafilePath", parentPath);
	}


	/**
	 * Post operation to update metadata
	 *
	 * @param hpcDatafile
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String createDatafile(@Valid @ModelAttribute("hpcDatafile") HpcDatafileModel hpcDataModel,
			@RequestParam("hpcDatafile") MultipartFile hpcDatafile, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request, HttpServletResponse response,
			final RedirectAttributes redirectAttributes) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		String[] action = request.getParameterValues("actionType");
		String parent = request.getParameter("parent");
		if (parent != null)
			model.addAttribute("parent", parent);
		String path = request.getParameter("path");
		if (path != null)
			model.addAttribute("datafilePath", path);
		else
			model.addAttribute("datafilePath", parent);

		String basePath = HpcClientUtil.getBasePath(request);
		String source = request.getParameter("source");
		if (source == null || source.isEmpty())
			source = (String) request.getAttribute("source");

		if (source == null || source.isEmpty())
			source = "dashboard";
		model.addAttribute("source", source);

		String checksum = request.getParameter("checksum");
		if (checksum == null || checksum.isEmpty())
			checksum = (String) request.getAttribute("checksum");

		model.addAttribute("checksum", checksum);

		if (action != null && action.length > 0 && action[0].equals("cancel"))
			return "redirect:/" + source;
		else if (action != null && action.length > 0 && action[0].equals("refresh"))
			return updateView(session, request, model, basePath, hpcDataModel.getPath(), true);

		if (hpcDatafile.isEmpty()) {
			model.addAttribute("hpcDataModel", hpcDataModel);
			model.addAttribute("dataFilePath", request.getParameter("path"));
			model.addAttribute("error", "Data file missing!");
			return updateView(session, request, model, basePath, hpcDataModel.getPath(), true);
		}

		try {
			if (hpcDataModel.getPath() == null || hpcDataModel.getPath().trim().length() == 0)
				model.addAttribute("error", "Invald Data file path");
			// Validate parent path
			String parentPath = null;
			hpcDataModel.setPath(hpcDataModel.getPath().trim());
			try {
				parentPath = hpcDataModel.getPath().substring(0, hpcDataModel.getPath().lastIndexOf("/"));
				HpcClientUtil.getCollection(authToken, collectionServiceURL, parentPath, true, sslCertPath,
						sslCertPassword);
			} catch (HpcWebException e) {
				model.addAttribute("hpcDataModel", hpcDataModel);
				model.addAttribute("dataFilePath", request.getParameter("path"));
				model.addAttribute("error", "Invalid parent collection: " + e.getMessage());
				populateFormAttributes(request, session, model, basePath, false);
				return "adddatafile";
			}

			HpcDataObjectRegistrationDTO registrationDTO = constructRequest(request, session, hpcDataModel.getPath());

			registrationDTO.setChecksum(checksum);
			boolean updated = HpcClientUtil.registerDatafile(authToken, hpcDatafile, serviceURL, registrationDTO,
					hpcDataModel.getPath(), sslCertPath, sslCertPassword);
			if (updated) {
				redirectAttributes.addFlashAttribute("error", "Data file " + hpcDataModel.getPath() + " is created!");
				session.removeAttribute("selectedUsers");
			}
		} catch (Exception e) {
			model.addAttribute("error", "Failed to create data file: " + e.getMessage());
			return "adddatafile";
		} finally {
			model.addAttribute("hpcDatafile", hpcDatafile);
			model.addAttribute("dataFilePath", request.getParameter("path"));
			populateFormAttributes(request, session, model, basePath, false);
		}
		return "redirect:/datafile?path=" + hpcDataModel.getPath() + "&action=view";
	}

	private HpcDataObjectRegistrationDTO constructRequest(HttpServletRequest request, HttpSession session,
			String path) {
		Enumeration<String> params = request.getParameterNames();
		HpcDataObjectRegistrationDTO dto = new HpcDataObjectRegistrationDTO();
		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();

		while (params.hasMoreElements()) {
			String paramName = params.nextElement();
			if (paramName.startsWith("zAttrStr_")) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				String attrName = paramName.substring("zAttrStr_".length());
				String[] attrValue = request.getParameterValues(paramName);
				entry.setAttribute(attrName);
				entry.setValue(attrValue[0]);
				metadataEntries.add(entry);
			} else if (paramName.startsWith("_addAttrName")) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				String attrId = paramName.substring("_addAttrName".length());
				String[] attrName = request.getParameterValues(paramName);
				String[] attrValue = request.getParameterValues("_addAttrValue" + attrId);
				if (attrName.length > 0 && !attrName[0].isEmpty())
					entry.setAttribute(attrName[0]);
				else
					throw new HpcWebException("Invalid metadata attribute name. Empty value is not valid!");
				if (attrValue.length > 0 && !attrValue[0].isEmpty())
					entry.setValue(attrValue[0]);
				else
					throw new HpcWebException("Invalid metadata attribute value. Empty value is not valid!");
				metadataEntries.add(entry);
			}
		}
		dto.getMetadataEntries().addAll(metadataEntries);
		dto.setSource(null);
		return dto;
	}

	// Get Collection type attributes
	// Get selected collection type
	// Get given path
	private String updateView(HttpSession session, HttpServletRequest request, Model model, String basePath, String path,
			boolean refresh) {
		populateBasePaths(request, session, model, path);
		if(path != null && !path.equals(basePath))
			model.addAttribute("datafilePath", basePath);
		populateFormAttributes(request, session, model, basePath, refresh);

		return "adddatafile";
	}

	private void populateFormAttributes(HttpServletRequest request, HttpSession session, Model model, String basePath, boolean refresh) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");

		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
		if (modelDTO == null) {
			modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
			session.setAttribute("userDOCModel", modelDTO);
		}
		List<HpcDataManagementRulesDTO> docRules = HpcClientUtil.getUserDOCManagementRules(modelDTO, user.getDoc());

		List<HpcMetadataValidationRule> rules = null;
		for (HpcDataManagementRulesDTO docRule : docRules) {
			if (docRule.getBasePath().equals(basePath))
				rules = docRule.getDataObjectMetadataValidationRules();
		}

		// For each collection type, get required attributes
		// Build list as type1:attribute1, type1:attribute2, type2:attribute2,
		// type2:attribute3

		// For each attribute, get valid values
		// Build list as type1:attribute1:value1, type1:attribute1:value2

		// For each attribute, get default value
		// Build list as type1:attribute1:defaultValue,
		// type2:attribute2:defaultValue
		if (rules != null && !rules.isEmpty()) {
			List<HpcMetadataAttrEntry> metadataEntries = new ArrayList<HpcMetadataAttrEntry>();
			List<String> attributeNames = new ArrayList<String>();
			for (HpcMetadataValidationRule rule : rules) {
				HpcMetadataAttrEntry entry = new HpcMetadataAttrEntry();
				entry.setAttrName(rule.getAttribute());
				attributeNames.add(rule.getAttribute());
				entry.setAttrValue(getFormAttributeValue(request, "zAttrStr_" + rule.getAttribute()));
				if (entry.getAttrValue() == null)
					entry.setAttrValue(rule.getDefaultValue());
				if (rule.getValidValues() != null && !rule.getValidValues().isEmpty()) {
					List<String> validValues = new ArrayList<String>();
					for (String value : rule.getValidValues())
						validValues.add(value);
					entry.setValidValues(validValues);
				}
				metadataEntries.add(entry);
			}

			if (!refresh) {
			// Handle custom attributes
			Enumeration<String> params = request.getParameterNames();
			while (params.hasMoreElements()) {
				String paramName = params.nextElement();
				if (paramName.startsWith("_addAttrName")) {
					HpcMetadataAttrEntry entry = new HpcMetadataAttrEntry();
					String[] attrName = request.getParameterValues(paramName);
					String attrId = paramName.substring("_addAttrName".length());
					String[] attrValue = request.getParameterValues("_addAttrValue" + attrId);
					if (attrName.length > 0 && !attrName[0].isEmpty())
						entry.setAttrName(attrName[0]);
					if (attrValue.length > 0 && !attrValue[0].isEmpty())
						entry.setAttrValue(attrValue[0]);
					metadataEntries.add(entry);
				}
			}
			}
		if (!attributeNames.isEmpty())
			model.addAttribute("attributeNames", attributeNames);
		if (!metadataEntries.isEmpty())
			model.addAttribute("datafileAttrs", metadataEntries);
		}
	}

	private String getFormAttributeValue(HttpServletRequest request, String attributeName) {
		String[] attrValue = request.getParameterValues(attributeName);
		if (attrValue != null)
			return attrValue[0];
		else
			return null;
	}

}
