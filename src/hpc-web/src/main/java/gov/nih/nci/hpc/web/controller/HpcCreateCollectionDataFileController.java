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
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import gov.nih.nci.hpc.domain.datamanagement.HpcDataHierarchy;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationItemDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDirectoryScanRegistrationItemDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcCollectionModel;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcMetadataAttrEntry;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * 
 * 
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcCollectionController.java
 */

@EnableAutoConfiguration
public abstract class HpcCreateCollectionDataFileController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;

	protected String login(Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
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
		return null;
	}

	protected void clearSessionAttrs(HttpSession session) {
		session.removeAttribute("datafilePath");
		session.removeAttribute("collection_type");
		session.removeAttribute("basePathSelected");
		session.removeAttribute("GlobusEndpoint");
		session.removeAttribute("GlobusEndpointPath");
		session.removeAttribute("GlobusEndpointFiles");
		session.removeAttribute("GlobusEndpointFolders");
		session.removeAttribute("parentCollection");
		session.removeAttribute("metadataEntries");
		session.removeAttribute("parent");
	}

	protected void populateBasePaths(HttpServletRequest request, HttpSession session, Model model, String path)
			throws HpcWebException {
		String authToken = (String) session.getAttribute("hpcUserToken");
		Set<String> basePaths = (Set<String>) session.getAttribute("basePaths");
		String userId = (String) session.getAttribute("hpcUserId");
		if (basePaths == null || basePaths.isEmpty()) {
			HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
			if (modelDTO == null) {
				modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
				session.setAttribute("userDOCModel", modelDTO);
			}
			HpcClientUtil.populateBasePaths(session, model, modelDTO, authToken, userId, serviceURL, sslCertPath,
					sslCertPassword);
			basePaths = (Set<String>) session.getAttribute("basePaths");
		}

		String selectedBasePath = HpcClientUtil.getBasePath(request);
		if (selectedBasePath == null)
			selectedBasePath = (String) session.getAttribute("basePathSelected");
		else
			session.setAttribute("basePathSelected", selectedBasePath);
		model.addAttribute("basePathSelected", selectedBasePath);

		setCollectionPath(model, request, path);
		model.addAttribute("basePaths", basePaths);
	}

	protected void setGlobusParameters(Model model, HttpServletRequest request, HttpSession session, String path,
			String parent, String source, boolean refresh) {
		String endPoint = request.getParameter("endpoint_id");
		String globusPath = request.getParameter("path");
		List<String> fileNames = new ArrayList<String>();
		List<String> folderNames = new ArrayList<String>();
		Enumeration<String> names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String paramName = names.nextElement();
			if (paramName.startsWith("file"))
				fileNames.add(request.getParameter(paramName));
			else if (paramName.startsWith("folder"))
				folderNames.add(request.getParameter(paramName));
		}
		if (endPoint == null)
			endPoint = (String) session.getAttribute("GlobusEndpoint");
		else
			session.setAttribute("GlobusEndpoint", endPoint);

		model.addAttribute("endpoint_id", endPoint);

		if (refresh || globusPath == null)
			globusPath = (String) session.getAttribute("GlobusEndpointPath");
		else
			session.setAttribute("GlobusEndpointPath", globusPath);

		model.addAttribute("endpoint_path", globusPath);

		if (fileNames.isEmpty())
			fileNames = (List<String>) session.getAttribute("GlobusEndpointFiles");
		else
			session.setAttribute("GlobusEndpointFiles", fileNames);

		if (folderNames.isEmpty())
			folderNames = (List<String>) session.getAttribute("GlobusEndpointFolders");
		else
			session.setAttribute("GlobusEndpointFolders", folderNames);

		if (endPoint != null)
			model.addAttribute("async", true);

		if (fileNames != null && !fileNames.isEmpty())
			model.addAttribute("fileNames", fileNames);

		if (folderNames != null && !folderNames.isEmpty())
			model.addAttribute("folderNames", folderNames);

//		if (parent == null)
//			model.addAttribute("datafilePath", session.getAttribute("datafilePath"));
//
//		if (path == null && parent == null)
//			model.addAttribute("datafilePath", session.getAttribute("datafilePath"));

		if (source == null)
			model.addAttribute("source", session.getAttribute("source"));

	}

	protected HpcBulkDataObjectRegistrationRequestDTO constructBulkRequest(HttpServletRequest request,
			HttpSession session, String path) {
		HpcBulkDataObjectRegistrationRequestDTO dto = new HpcBulkDataObjectRegistrationRequestDTO();
		String datafilePath = (String) session.getAttribute("datafilePath");
		String globusEndpoint = (String) session.getAttribute("GlobusEndpoint");
		String selectedBasePath = (String) session.getAttribute("basePathSelected");
		String globusEndpointPath = (String) session.getAttribute("GlobusEndpointPath");
		List<String> globusEndpointFiles = (List<String>) session.getAttribute("GlobusEndpointFiles");
		List<String> globusEndpointFolders = (List<String>) session.getAttribute("GlobusEndpointFolders");

		if (globusEndpointFiles != null) {
			List<HpcDataObjectRegistrationItemDTO> files = new ArrayList<HpcDataObjectRegistrationItemDTO>();
			for (String fileName : globusEndpointFiles) {
				HpcDataObjectRegistrationItemDTO file = new HpcDataObjectRegistrationItemDTO();
				HpcFileLocation source = new HpcFileLocation();
				source.setFileContainerId(globusEndpoint);
				source.setFileId(globusEndpointPath + fileName);
				file.setSource(source);
				file.setCreateParentCollections(true);
				file.setPath(path + "/" + fileName);
				System.out.println(path + "/" + fileName);
				// file.getParentCollectionMetadataEntries().addAll(metadataEntries);
				files.add(file);
			}
			dto.getDataObjectRegistrationItems().addAll(files);
		}

		if (globusEndpointFolders != null) {
			List<HpcDirectoryScanRegistrationItemDTO> folders = new ArrayList<HpcDirectoryScanRegistrationItemDTO>();
			for (String folderName : globusEndpointFolders) {
				HpcDirectoryScanRegistrationItemDTO folder = new HpcDirectoryScanRegistrationItemDTO();
				HpcFileLocation source = new HpcFileLocation();
				source.setFileContainerId(globusEndpoint);
				source.setFileId(globusEndpointPath.endsWith("/") ?  globusEndpointPath + folderName : globusEndpointPath + "/" + folderName);
				folder.setBasePath(datafilePath);
				folder.setScanDirectoryLocation(source);
				folders.add(folder);
			}
			dto.getDirectoryScanRegistrationItems().addAll(folders);
		}

		return dto;
	}

	protected List<HpcMetadataEntry> getMetadataEntries(HttpServletRequest request, HttpSession session, String path) {
		Enumeration<String> params = request.getParameterNames();
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
		return metadataEntries;
	}

	protected void populateCollectionTypes(HttpSession session, Model model, String basePath, String parent)
			throws HpcWebException {
		String authToken = (String) session.getAttribute("hpcUserToken");

		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
		if (modelDTO == null) {
			modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
			session.setAttribute("userDOCModel", modelDTO);
		}

		HpcDataManagementRulesDTO basePathRules = HpcClientUtil.getBasePathManagementRules(modelDTO, basePath);
		String collectionType = null;
		Set<String> collectionTypesSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

		HpcCollectionListDTO collections = null;
		HpcCollectionDTO collection = null;
		if (parent != null && !parent.isEmpty()) {
			collection = (HpcCollectionDTO) session.getAttribute("parentCollection");
			if (collection == null) {
				collections = HpcClientUtil.getCollection(authToken, serviceURL, parent, false, sslCertPath,
						sslCertPassword);
				if (collections != null && collections.getCollections() != null
						&& collections.getCollections().size() > 0) {
					collection = collections.getCollections().get(0);
				}
			}
		}
		if (basePathRules != null) {
			List<HpcMetadataValidationRule> rules = basePathRules.getCollectionMetadataValidationRules();
			// Parent name is given
			if (parent != null) {
				if (collection != null) {
					if (collection.getMetadataEntries() == null) {
						if (basePathRules.getDataHierarchy() != null)
							collectionTypesSet.add(basePathRules.getDataHierarchy().getCollectionType());
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
					for (String type : getCollectionTypes(rules))
						collectionTypesSet.add(type);
				}

				if (collectionType != null) {
					List<String> subCollections = getSubCollectionTypes(collectionType,
							basePathRules.getDataHierarchy());
					if ((subCollections == null || subCollections.isEmpty()) && !rules.isEmpty())
						throw new HpcWebException("Adding a sub collection is not allowed with: " + parent);
					for (String type : subCollections)
						collectionTypesSet.add(type);
				}
			}

			if (collectionType == null && collectionTypesSet.isEmpty()) {
				for (HpcMetadataValidationRule validationrule : rules) {
					if (validationrule.getMandatory() && validationrule.getAttribute().equals("collection_type")) {
						for (String type : validationrule.getValidValues())
							collectionTypesSet.add(type);
					}
				}
			}
		}
		if (collectionTypesSet.isEmpty())
			collectionTypesSet.add("Folder");
		model.addAttribute("collectionTypes", collectionTypesSet);
	}

	protected void checkParent(String parent, HttpSession session) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (parent != null && !parent.isEmpty()) {
			HpcCollectionListDTO collections = HpcClientUtil.getCollection(authToken, serviceURL, parent, false,
					sslCertPath, sslCertPassword);
			if (collections != null && collections.getCollections() != null && collections.getCollections().size() > 0)
				session.setAttribute("parentCollection", collections.getCollections().get(0));
		}

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

	protected List<HpcMetadataAttrEntry> populateFormAttributes(HttpServletRequest request, HttpSession session,
			Model model, String basePath, String collectionType, boolean refresh, boolean datafile) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");

		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
		if (modelDTO == null) {
			modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
			session.setAttribute("userDOCModel", modelDTO);
		}
		List<HpcMetadataValidationRule> rules = null;
		HpcDataManagementRulesDTO basePathRules = HpcClientUtil.getBasePathManagementRules(modelDTO, basePath);
		if (basePathRules != null) {
			if (datafile)
				rules = basePathRules.getDataObjectMetadataValidationRules();
			else
				rules = basePathRules.getCollectionMetadataValidationRules();
		}

		HpcCollectionDTO collectionDTO = (HpcCollectionDTO) session.getAttribute("parentCollection");
		List<HpcMetadataAttrEntry> cachedEntries = (List<HpcMetadataAttrEntry>) session.getAttribute("metadataEntries");
		// if (collectionType == null)
		// collectionType = "Folder";

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
		if (rules != null && !rules.isEmpty()) {
			for (HpcMetadataValidationRule rule : rules) {
				if ((rule.getMandatory()
						&& (rule.getCollectionTypes().contains(collectionType) || rule.getCollectionTypes().isEmpty()))
						&& !rule.getAttribute().equals("collection_type")) {
					HpcMetadataAttrEntry entry = new HpcMetadataAttrEntry();
					entry.setAttrName(rule.getAttribute());
					attributeNames.add(rule.getAttribute());
					entry.setAttrValue(
							getFormAttributeValue(request, "zAttrStr_" + rule.getAttribute(), cachedEntries));
					if (entry.getAttrValue() == null) {
						if (!refresh)
							entry.setAttrValue(getCollectionAttrValue(collectionDTO, rule.getAttribute()));
						else
							entry.setAttrValue(rule.getDefaultValue());
					}
					if (rule.getValidValues() != null && !rule.getValidValues().isEmpty()) {
						List<String> validValues = new ArrayList<String>();
						for (String value : rule.getValidValues())
							validValues.add(value);
						entry.setValidValues(validValues);
					}
					metadataEntries.add(entry);
				}
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
		if (collectionType != null && !collectionType.isEmpty())
			model.addAttribute("collection_type", collectionType);
		else
			model.addAttribute("collection_type", "Folder");

		// if (!path.isEmpty())
		// model.addAttribute("collectionPath", path);
		model.addAttribute("basePath", basePath);
		session.setAttribute("metadataEntries", metadataEntries);
		return metadataEntries;
	}

	private String getCollectionAttrValue(HpcCollectionDTO collectionDTO, String attrName) {
		if (collectionDTO == null || collectionDTO.getMetadataEntries() == null
				|| collectionDTO.getMetadataEntries().getSelfMetadataEntries() == null)
			return null;

		for (HpcMetadataEntry entry : collectionDTO.getMetadataEntries().getSelfMetadataEntries()) {
			if (entry.getAttribute().equals(attrName))
				return entry.getValue();
		}
		return null;
	}

	private String getFormAttributeValue(HttpServletRequest request, String attributeName,
			List<HpcMetadataAttrEntry> cachedEntries) {
		String[] attrValue = request.getParameterValues(attributeName);
		if (attrValue != null)
			return attrValue[0];
		else {
			if (cachedEntries == null || cachedEntries.size() == 0)
				return null;
			for (HpcMetadataAttrEntry entry : cachedEntries) {
				if (attributeName.equals("zAttrStr_" + entry.getAttrName()))
					return entry.getAttrValue();
			}
		}
		return null;
	}

	private void setCollectionPath(Model model, HttpServletRequest request, String parentPath) {
		String path = request.getParameter("path");
		if (path != null && !path.isEmpty()) {
			model.addAttribute("collectionPath", request.getParameter("path"));
			return;
		}

		if (parentPath == null || parentPath.isEmpty()) {
			String[] basePathValues = request.getParameterValues("basePath");
			String basePath = null;
			if (basePathValues == null || basePathValues.length == 0)
				basePath = (String) request.getAttribute("basePath");
			else
				basePath = basePathValues[0];
			model.addAttribute("collectionPath", basePath);
		} else
			model.addAttribute("collectionPath", parentPath);
	}

	protected HpcCollectionRegistrationDTO constructRequest(HttpServletRequest request, HttpSession session,
			String path, HpcCollectionModel hpcCollection) throws HpcWebException {
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
		if (hpcCollection != null)
			hpcCollection.setSelfMetadataEntries(selfMetadataEntries);
		dto.getMetadataEntries().addAll(metadataEntries);
		return dto;
	}

}
