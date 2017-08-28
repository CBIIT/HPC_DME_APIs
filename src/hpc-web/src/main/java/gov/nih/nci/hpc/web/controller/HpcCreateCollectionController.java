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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcCollectionModel;
import gov.nih.nci.hpc.web.model.HpcLogin;
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
			if(path != null)
				model.addAttribute("parentPath", path);
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

			populateFormAttributes(session, model);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			model.addAttribute("error", "Failed to add Collection: " + e.getMessage());
			e.printStackTrace();
		}
		model.addAttribute("hpcCollection", new HpcCollectionModel());
		return "addcollection";
	}
	
	private void populateFormAttributes(HttpSession session, Model model)
	{
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");

		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
		if (modelDTO == null) {
			modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, user.getDoc(), sslCertPath, sslCertPassword);
			session.setAttribute("userDOCModel", modelDTO);
		}
		
		Set<String> collectionTypesSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		Set<String> collectionTypeAttrsSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		Set<String> collectionTypeAttrDefaultValuesSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		Set<String> collectionTypeAttrValidValuesSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

		List<HpcMetadataValidationRule> rules = modelDTO.getCollectionMetadataValidationRules();
		for(HpcMetadataValidationRule rule:rules)
		{
			if(rule.getMandatory() && rule.getAttribute().equals("collection_type"))
				collectionTypesSet.addAll(rule.getValidValues());
				
		}
		
		//For each collection type, get required attributes
		//Build list as type1:attribute1, type1:attribute2, type2:attribute2, type2:attribute3

		//For each attribute, get valid values
		//Build list as type1:attribute1:value1, type1:attribute1:value2
		
		//For each attribute, get default value
		//Build list as type1:attribute1:defaultValue, type2:attribute2:defaultValue
		for(String collectionType : collectionTypesSet)
		{
			for(HpcMetadataValidationRule rule:rules)
			{
				if(rule.getMandatory() && rule.getCollectionTypes().contains(collectionType))
					collectionTypeAttrsSet.add(collectionType+":"+rule.getAttribute());
				if(rule.getDefaultValue() != null)
					collectionTypeAttrDefaultValuesSet.add(collectionType+":"+rule.getAttribute()+":"+rule.getDefaultValue());
				if(rule.getValidValues() != null && !rule.getValidValues().isEmpty())
				{
					for(String value : rule.getValidValues())
						collectionTypeAttrValidValuesSet.add(collectionType+":"+rule.getAttribute()+":"+value);
				}
			}
		}
		
		model.addAttribute("collectionTypes", collectionTypesSet);
		model.addAttribute("collectionTypeAttrs", collectionTypeAttrsSet);
		model.addAttribute("collectionTypeAttrDefaultValues", collectionTypeAttrDefaultValuesSet);
		model.addAttribute("collectionTypeAttrValidValues", collectionTypeAttrValidValuesSet);
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

			boolean created = HpcClientUtil.updateCollection(authToken, serviceURL, registrationDTO,
					hpcCollection.getPath(), sslCertPath, sslCertPassword);
			if (created) {
				redirectAttributes.addFlashAttribute("error", "Collection " + hpcCollection.getPath() + " is created!");
				session.removeAttribute("selectedUsers");
			}
		} catch (Exception e) {
			//redirectAttributes.addFlashAttribute("error", "Failed to create collection: " + e.getMessage());
			model.addAttribute("error", "Failed to create collection: " + e.getMessage());
			return "addcollection";
		}finally
		{
			populateFormAttributes(session, model);			
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
