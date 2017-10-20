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

import gov.nih.nci.hpc.domain.datamanagement.HpcDataHierarchy;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
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
	public String home(@RequestBody(required = false) String body, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		try {
			String parent = request.getParameter("parent");
			if (parent != null)
				model.addAttribute("parent", parent);
			String path = request.getParameter("path");

			String source = request.getParameter("source");
			if (source == null || source.isEmpty())
				source = (String) request.getAttribute("source");
			if (source == null || source.isEmpty())
				source = "dashboard";

			model.addAttribute("source", source);

			// User Session validation
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (user == null || authToken == null) {
				ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
				bindingResult.addError(error);
				HpcLogin hpcLogin = new HpcLogin();
				model.addAttribute("hpcLogin", hpcLogin);
				return "index";
			}
			populateBasePaths(request, session, model, path);
			setCollectionPath(model, request, parent);
		} catch (Exception e) {
			model.addAttribute("error", "Failed to add Collection: " + e.getMessage());
			e.printStackTrace();
		}
		model.addAttribute("hpcCollection", new HpcCollectionModel());
		return "addcollection";
	}

	private void populateBasePaths(HttpServletRequest request, HttpSession session, Model model, String path) throws HpcWebException {
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");

		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
		if (modelDTO == null) {
			modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
			session.setAttribute("userDOCModel", modelDTO);
		}

		Set<String> basePaths = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		List<HpcDataManagementRulesDTO> docRules = HpcClientUtil.getUserDOCManagementRules(modelDTO, user.getDoc());
		for (HpcDataManagementRulesDTO docRule : docRules) {
			basePaths.add(docRule.getBasePath());
		}
		model.addAttribute("basePathSelected", HpcClientUtil.getBasePath(request));
		setCollectionPath(model, request, path);
		model.addAttribute("basePaths", basePaths);
	}
	
	private void populateCollectionTypes(HttpSession session, Model model, String basePath, String parent) throws HpcWebException {
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");

		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
		if (modelDTO == null) {
			modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
			session.setAttribute("userDOCModel", modelDTO);
		}

		String collectionType = null;
		Set<String> collectionTypesSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		List<HpcDataManagementRulesDTO> docRules = HpcClientUtil.getUserDOCManagementRules(modelDTO, user.getDoc());
		HpcCollectionListDTO collections = null;
		if(parent != null)
			collections = HpcClientUtil.getCollection(authToken, serviceURL, parent, false,
				sslCertPath, sslCertPassword);
		for (HpcDataManagementRulesDTO docRule : docRules) {
			if(!docRule.getBasePath().equals(basePath))
				continue;
			List<HpcMetadataValidationRule> rules = docRule.getCollectionMetadataValidationRules();
			// Parent name is given
			if (parent != null) {
				if (collections != null && collections.getCollections() != null
						&& collections.getCollections().size() > 0) {
					HpcCollectionDTO collection = collections.getCollections().get(0);
					if (collection.getMetadataEntries() == null) {
						if (docRule.getDataHierarchy() != null)
							collectionTypesSet.add(docRule.getDataHierarchy().getCollectionType());
						else
							collectionTypesSet.add("Folder");
					} else {
						for (HpcMetadataEntry entry : collection.getMetadataEntries().getSelfMetadataEntries()) {
							if (entry.getAttribute().equals("collection_type")) {
								collectionType = entry.getValue();
								break;
							}
						}
					}
				} else {
					// Collection not found
					// Populate all collection types
					for(String type : getCollectionTypes(rules))
						collectionTypesSet.add(type);
				}

				if (collectionType != null) {
					List<String> subCollections = getSubCollectionTypes(collectionType, docRule.getDataHierarchy());
					if ((subCollections == null || subCollections.isEmpty()) && !rules.isEmpty())
						throw new HpcWebException("Adding a sub collection is not allowed with: " + parent);
					for(String type : subCollections)
						collectionTypesSet.add(type);
				}
			}

			if (collectionType == null && collectionTypesSet.isEmpty()) {
				for (HpcMetadataValidationRule rule : rules) {
					if (rule.getMandatory() && rule.getAttribute().equals("collection_type"))
					{
						for(String type : rule.getValidValues())
							collectionTypesSet.add(type);
					}
				}
			}

			if (rules.isEmpty() && collectionTypesSet.isEmpty())
				collectionTypesSet.add("Folder");
		}
		model.addAttribute("collectionTypes", collectionTypesSet);
	}

	private List<String> getCollectionTypes(List<HpcMetadataValidationRule> rules) {
		List<String> collectionTypesSet = new ArrayList<String>();
		for (HpcMetadataValidationRule rule : rules) {
			if (rule.getMandatory() && rule.getAttribute().equals("collection_type"))
				collectionTypesSet.addAll(rule.getValidValues());
		}
		return collectionTypesSet;
	}

	private List<String> getSubCollectionTypes(String collectionType, HpcDataHierarchy dataHierarchy) {
		List<String> types = new ArrayList<String>();
		if (dataHierarchy == null || dataHierarchy.getSubCollectionsHierarchies() == null)
			return types;
		if (dataHierarchy.getCollectionType().equals(collectionType)) {
			List<HpcDataHierarchy> subs = dataHierarchy.getSubCollectionsHierarchies();
			for (HpcDataHierarchy sub : subs)
				types.add(sub.getCollectionType());
		} else {
			List<HpcDataHierarchy> subs = dataHierarchy.getSubCollectionsHierarchies();
			for (HpcDataHierarchy sub : subs)
				return getSubCollectionTypes(collectionType, sub);
		}

		return types;
	}

	private void populateFormAttributes(HttpServletRequest request, HttpSession session, Model model, String basePath, String path,
			String collectionType, boolean refresh) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");

		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
		if (modelDTO == null) {
			modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
			session.setAttribute("userDOCModel", modelDTO);
		}
		List<HpcDataManagementRulesDTO> docRules = HpcClientUtil.getUserDOCManagementRules(modelDTO, user.getDoc());
		List<HpcMetadataValidationRule> rules = null;
		for(HpcDataManagementRulesDTO docRule : docRules)
		{
			if(docRule.getBasePath().equals(basePath))
				rules = docRule.getCollectionMetadataValidationRules();
		}

		if (collectionType == null)
			collectionType = "Folder";

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
			if (rule.getMandatory() && rule.getCollectionTypes().contains(collectionType)) {
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
		}

		// Handle custom attributes. If refresh, ignore them
		if (!refresh) {
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
			model.addAttribute("collectionAttrs", metadataEntries);
		if (!collectionType.isEmpty())
			model.addAttribute("collection_type", collectionType);

//		if (!path.isEmpty())
//			model.addAttribute("collectionPath", path);
		model.addAttribute("basePath", basePath);
	}

	private String getFormAttributeValue(HttpServletRequest request, String attributeName) {
		String[] attrValue = request.getParameterValues(attributeName);
		if (attrValue != null)
			return attrValue[0];
		else
			return null;
	}

	// Get Collection type attributes
	// Get selected collection type
	// Get given path
	private String updateView(HttpSession session, HttpServletRequest request, Model model, String basePath, String path, String parent,
			boolean refresh) {
		populateBasePaths(request, session, model, path);
		populateCollectionTypes(session, model, basePath, parent);
		String collectionType = getFormAttributeValue(request, "zAttrStr_collection_type");
		populateFormAttributes(request, session, model, basePath, path, collectionType, refresh);

		return "addcollection";
	}

	/**
	 * Create collection
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
	public String createCollection(@Valid @ModelAttribute("hpcCollection") HpcCollectionModel hpcCollection,
			Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response, final RedirectAttributes redirectAttributes) {
		String[] action = request.getParameterValues("actionType");
		String[] parent = request.getParameterValues("parent");
		String originPath = null;
		String[] sourceParam = request.getParameterValues("source");
		String source = "Dashboard";
		if (sourceParam == null || sourceParam.length == 0)
			source = (String) request.getAttribute("source");
		else {
			if (sourceParam.length > 1) {
				source = sourceParam[0].isEmpty() ? sourceParam[1] : sourceParam[0];
			} else
				source = sourceParam[0];
		}
		if (source == null || source.isEmpty())
			source = "dashboard";

		model.addAttribute("source", source);
		String basePath = HpcClientUtil.getBasePath(request);

		if (parent != null && !parent[0].isEmpty())
			originPath = parent[0];

		if (originPath != null)
			model.addAttribute("parent", originPath);

		String collectionType = getFormAttributeValue(request, "zAttrStr_collection_type");

		if (action != null && action.length > 0 && action[0].equals("cancel"))
			return "redirect:/" + source;
		else if (action != null && action.length > 0 && action[0].equals("refresh"))
			return updateView(session, request, model, basePath, hpcCollection.getPath(), originPath, true);

		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcCollectionRegistrationDTO registrationDTO = constructRequest(request, session, hpcCollection.getPath(),
				hpcCollection);

		// Validate parent path
		String parentPath = null;
		try {
			hpcCollection.setPath(hpcCollection.getPath().trim());
			if (hpcCollection.getPath().lastIndexOf("/") != -1)
				parentPath = hpcCollection.getPath().substring(0, hpcCollection.getPath().lastIndexOf("/"));
			else
				parentPath = hpcCollection.getPath();
			HpcClientUtil.getCollection(authToken, serviceURL, parentPath, true, sslCertPath, sslCertPassword);
		} catch (HpcWebException e) {
			model.addAttribute("hpcCollection", hpcCollection);
			setCollectionPath(model, request, originPath);
			model.addAttribute("error", "Invalid parent collection: " + e.getMessage());
			return updateView(session, request, model, basePath, hpcCollection.getPath(), originPath, false);
		}

		// Validate Collection path
		try {
			HpcCollectionListDTO collection = HpcClientUtil.getCollection(authToken, serviceURL,
					hpcCollection.getPath(), false, true, sslCertPath, sslCertPassword);
			if (collection != null && collection.getCollections() != null && collection.getCollections().size() > 0) {
				model.addAttribute("hpcCollection", hpcCollection);
				setCollectionPath(model, request, originPath);
				model.addAttribute("error", "Collection already exists: " + hpcCollection.getPath());
				return updateView(session, request, model, basePath, hpcCollection.getPath(), originPath, false);
			}
		} catch (HpcWebException e) {
			// Collection does not exist. We will create the collection
		}

		try {
			if (hpcCollection.getPath() == null || hpcCollection.getPath().trim().length() == 0)
				model.addAttribute("error", "Invald collection path");

			boolean created = HpcClientUtil.updateCollection(authToken, serviceURL, registrationDTO,
					hpcCollection.getPath(), sslCertPath, sslCertPassword);
			if (created) {
				redirectAttributes.addFlashAttribute("error", "Collection " + hpcCollection.getPath() + " is created!");
				session.removeAttribute("selectedUsers");
			}
		} catch (Exception e) {
			model.addAttribute("error", "Failed to create collection: " + e.getMessage());
			return "addcollection";
		} finally {
			populateBasePaths(request, session, model, hpcCollection.getPath());
			populateCollectionTypes(session, model, basePath, originPath);
			populateFormAttributes(request, session, model, basePath, request.getParameter("path"), collectionType, false);
			model.addAttribute("hpcCollection", hpcCollection);
			setCollectionPath(model, request, originPath);		}
		return "redirect:/collection?path=" + hpcCollection.getPath() + "&action=view";
	}

	private void setCollectionPath(Model model, HttpServletRequest request, String parentPath)
	{
		String path = request.getParameter("path");
		if(path != null && !path.isEmpty())
			model.addAttribute("collectionPath", request.getParameter("path"));
		
		if(parentPath == null || parentPath.isEmpty())
		{
			String[] basePathValues = request.getParameterValues("basePath");
			String basePath = null;
			if (basePathValues == null || basePathValues.length == 0)
				basePath = (String) request.getAttribute("basePath");
			else 
				basePath = basePathValues[0];
			model.addAttribute("collectionPath", basePath);
		}
		else
			model.addAttribute("collectionPath", parentPath);
	}
	
	private HpcCollectionRegistrationDTO constructRequest(HttpServletRequest request, HttpSession session, String path,
			HpcCollectionModel hpcCollection) throws HpcWebException {
		Enumeration<String> params = request.getParameterNames();
		HpcCollectionRegistrationDTO dto = new HpcCollectionRegistrationDTO();
		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
		List<HpcMetadataAttrEntry> selfMetadataEntries = new ArrayList<>();
		while (params.hasMoreElements()) {
			String paramName = params.nextElement();
			if (paramName.startsWith("zAttrStr_")) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				HpcMetadataAttrEntry attrEntry = new HpcMetadataAttrEntry();
				String attrName = paramName.substring("zAttrStr_".length());
				String[] attrValue = request.getParameterValues(paramName);
				entry.setAttribute(attrName);
				entry.setValue(attrValue[0]);
				metadataEntries.add(entry);
				attrEntry.setAttrName(attrName);
				attrEntry.setAttrValue(attrValue[0]);
				attrEntry.setSystemAttr(false);
				selfMetadataEntries.add(attrEntry);
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
				HpcMetadataAttrEntry attrEntry = new HpcMetadataAttrEntry();
				attrEntry.setAttrName(attrName[0]);
				attrEntry.setAttrValue(attrValue[0]);
				attrEntry.setSystemAttr(false);
				selfMetadataEntries.add(attrEntry);
			}
		}
		hpcCollection.setSelfMetadataEntries(selfMetadataEntries);
		dto.getMetadataEntries().addAll(metadataEntries);
		return dto;
	}

}
