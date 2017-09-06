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
	public String home(@RequestBody(required = false) String body, 
			Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		try {
			String path = request.getParameter("path");
			String parent = request.getParameter("parent");
			if (parent != null)
				model.addAttribute("parent", parent);
			if (path != null)
				model.addAttribute("datafilePath", path);
			else
				model.addAttribute("datafilePath", parent);
			
			// User Session validation
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			String userId = (String) session.getAttribute("hpcUserId");
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (user == null || authToken == null) {
				ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
				bindingResult.addError(error);
				HpcLogin hpcLogin = new HpcLogin();
				model.addAttribute("hpcLogin", hpcLogin);
				return "index";
			}
			populateFormAttributes(request, session, model);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			model.addAttribute("error", "Failed to add data file: " + e.getMessage());
			e.printStackTrace();
		}
		model.addAttribute("hpcDatafile", new HpcDatafileModel());
		return "adddatafile";
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
	public String createDatafile(@Valid @ModelAttribute("hpcDatafile") HpcDatafileModel hpcDataModel, @RequestParam("hpcDatafile") MultipartFile hpcDatafile, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request, HttpServletResponse response,
			final RedirectAttributes redirectAttributes) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		String[] action = request.getParameterValues("action");
		String parent = request.getParameter("parent");
		if (parent != null)
			model.addAttribute("parent", parent);
		String path = request.getParameter("path");
		if (path != null)
			model.addAttribute("datafilePath", path);
		else
			model.addAttribute("datafilePath", parent);
		
		if (action != null && action.length > 0 && action[0].equals("cancel"))
			return "redirect:/datafile?path=" + hpcDataModel.getPath() + "&action=view";

		if(hpcDatafile.isEmpty())
		{
			model.addAttribute("hpcDataModel", hpcDataModel);
			model.addAttribute("dataFilePath", request.getParameter("path"));
			model.addAttribute("error", "Data file missing!");
			populateFormAttributes(request, session, model);
			return "adddatafile";
		}

		try {
			if (hpcDataModel.getPath() == null || hpcDataModel.getPath().trim().length() == 0)
				model.addAttribute("error", "Invald Data file path");
			// Validate parent path
			String parentPath = null;
			try {
				parentPath = hpcDataModel.getPath().substring(0, hpcDataModel.getPath().lastIndexOf("/"));
				HpcClientUtil.getCollection(authToken, collectionServiceURL, parentPath, true, sslCertPath, sslCertPassword);
			} catch (HpcWebException e) {
				model.addAttribute("hpcDataModel", hpcDataModel);
				model.addAttribute("dataFilePath", request.getParameter("path"));
				model.addAttribute("error", "Invalid parent collection: " + e.getMessage());
				populateFormAttributes(request, session, model);
				return "adddatafile";
			}

			HpcDataObjectRegistrationDTO registrationDTO = constructRequest(request, session, hpcDataModel.getPath());

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
			populateFormAttributes(request, session, model);
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
			}else if(paramName.startsWith("addAttrName"))
			{
				HpcMetadataEntry entry = new HpcMetadataEntry();
				String attrId = paramName.substring("addAttrName".length());
				String[] attrName = request.getParameterValues(paramName);
				String[] attrValue = request.getParameterValues("addAttrValue"+attrId);
				if(attrName.length > 0 && !attrName[0].isEmpty())
					entry.setAttribute(attrName[0]);
				else
					throw new HpcWebException("Invalid metadata attribute name. Empty value is not valid!");
				if(attrValue.length > 0 && !attrValue[0].isEmpty())
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

	private void populateFormAttributes(HttpServletRequest request, HttpSession session, Model model) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");

		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
		if (modelDTO == null) {
			modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, user.getDoc(), sslCertPath, sslCertPassword);
			session.setAttribute("userDOCModel", modelDTO);
		}
		List<HpcMetadataValidationRule> rules = modelDTO.getDataObjectMetadataValidationRules();

		// For each collection type, get required attributes
		// Build list as type1:attribute1, type1:attribute2, type2:attribute2,
		// type2:attribute3

		// For each attribute, get valid values
		// Build list as type1:attribute1:value1, type1:attribute1:value2

		// For each attribute, get default value
		// Build list as type1:attribute1:defaultValue,
		// type2:attribute2:defaultValue
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

		//Handle custom attributes
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
		
		if (!attributeNames.isEmpty())
			model.addAttribute("attributeNames", attributeNames);
		if (!metadataEntries.isEmpty())
			model.addAttribute("datafileAttrs", metadataEntries);
	}
	
	private String getFormAttributeValue(HttpServletRequest request, String attributeName) {
		String[] attrValue = request.getParameterValues(attributeName);
		if (attrValue != null)
			return attrValue[0];
		else
			return null;
	}
	
}
