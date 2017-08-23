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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcCollectionModel;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcMetadataAttrEntry;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Collection controller. Gets selected collection details. Updates collection
 * metadata.
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcCollectionController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/addCollection")
public class HpcCreateCollectionController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String serviceURL;
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
			String collectionType = request.getParameter("type");
			if(path != null)
				model.addAttribute("parentPath", path);

			if(collectionType != null)
				model.addAttribute("collectionType", collectionType);
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
			HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO)session.getAttribute("userDOCModel"); 
			if (modelDTO == null)
			{
				modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, user.getDoc(),
					sslCertPath, sslCertPassword);
				session.setAttribute("userDOCModel", modelDTO);
			}
			List<String> collectionTypes = getValidCollectionTypes(modelDTO);
			model.addAttribute("collectionTypes", collectionTypes);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			model.addAttribute("error", "Failed to add Collection: " + e.getMessage());
			e.printStackTrace();
		}
		model.addAttribute("hpcCollection", new HpcCollectionModel());
		return "addcollection";
	}
	
	private String getCollectionType(String path)
	{
		return null;
		
	}

	private List<String> intializeFormAttributes(HpcDataManagementModelDTO modelDTO, Model model)
	{
		List<String> collectionTypes = new ArrayList<String>();
		List<String> validValues = new ArrayList<String>();
		List<String> defaultValue = new ArrayList<String>();
		
		List<HpcMetadataValidationRule> rules = modelDTO.getCollectionMetadataValidationRules();
		for(HpcMetadataValidationRule rule: rules)
		{
			if(rule.getRuleEnabled() && rule.getAttribute().equals("collection_type"))
			{
				collectionTypes.addAll(rule.getValidValues());
				for(String collType : rule.getValidValues())
				{
					List<HpcMetadataValidationRule> collectionTypeAttrs = setCollectionTypeAttributes(collType);
				}
			}
		}
		model.addAttribute("collectionTypes", collectionTypes);
		return types;
		
	}
	
	private List<String> setCollectionTypeAttributes(HpcDataManagementModelDTO modelDTO, String type, Model model)
	{
		List<String> attributes = new ArrayList<String>();
		List<String> requiredAttributes = new ArrayList<String>();
		
		List<HpcMetadataValidationRule> rules = modelDTO.getCollectionMetadataValidationRules();
		for(HpcMetadataValidationRule rule: rules)
		{
			if(rule.getRuleEnabled() && rule.getCollectionTypes().contains(type))
			{
				attributes.add(rule.getAttribute());
				if(rule.getMandatory())
					requiredAttributes.add(rule.getAttribute());
			}
			
		}
		return types;
		
	}

	/**
	 * Update collection
	 * 
	 * @param hpcCollection
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String updateCollection(@Valid @ModelAttribute("hpcGroup") HpcCollectionModel hpcCollection, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request, HttpServletResponse response,
			final RedirectAttributes redirectAttributes) {
		String[] action = request.getParameterValues("action");
		if (action != null && action.length > 0 && action[0].equals("cancel"))
			return "redirect:/collection?path=" + hpcCollection.getPath() + "&action=view";

		String authToken = (String) session.getAttribute("hpcUserToken");
		try {
			if (hpcCollection.getPath() == null || hpcCollection.getPath().trim().length() == 0)
				model.addAttribute("error", "Invald collection path");

			HpcCollectionRegistrationDTO registrationDTO = constructRequest(request, session, hpcCollection.getPath());

			boolean updated = HpcClientUtil.updateCollection(authToken, serviceURL, registrationDTO,
					hpcCollection.getPath(), sslCertPath, sslCertPassword);
			if (updated) {
				redirectAttributes.addFlashAttribute("error", "Collection " + hpcCollection.getPath() + " is created!");
				session.removeAttribute("selectedUsers");
			}
		} catch (Exception e) {
			//redirectAttributes.addFlashAttribute("error", "Failed to create collection: " + e.getMessage());
			model.addAttribute("error", "Failed to create collection: " + e.getMessage());
			return "addcollection";
		}
		return "redirect:/collection?path=" + hpcCollection.getPath() + "&action=view";
	}

	private HpcCollectionRegistrationDTO constructRequest(HttpServletRequest request, HttpSession session,
			String path) throws HpcWebException{
		Enumeration<String> params = request.getParameterNames();
		HpcCollectionRegistrationDTO dto = new HpcCollectionRegistrationDTO();
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
			} else if(paramName.startsWith("addAttrName"))
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
		return dto;
	}

}
