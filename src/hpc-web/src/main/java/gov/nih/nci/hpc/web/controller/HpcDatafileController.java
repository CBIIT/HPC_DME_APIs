/**
 * HpcDatafileController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import gov.nih.nci.hpc.web.util.MiscUtil;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.annotation.JsonView;

import gov.nih.nci.hpc.domain.databrowse.HpcBookmark;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcDatafileModel;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcMetadataAttrEntry;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Controller to display data file details and update metadata.
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/datafile")
public class HpcDatafileController extends HpcCreateCollectionDataFileController {
	@Value("${gov.nih.nci.hpc.server.dataObject}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;

	/**
	 * Get operation to display data file details and its metadata
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
	public String home(@RequestBody(required = false) String body, @RequestParam String path,
			@RequestParam String action, Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		try {
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			
			String authToken = (String) session.getAttribute(ATTR_USER_TOKEN);
			if (user == null || authToken == null) {
				ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
				bindingResult.addError(error);
				HpcLogin hpcLogin = new HpcLogin();
				model.addAttribute("hpcLogin", hpcLogin);
				final Map<String, String> qParams = new HashMap<>();
				qParams.put("returnPath", "datafile");
				qParams.put("action", action);
				qParams.put("path", path);
				return "redirect:/login?".concat(MiscUtil.generateEncodedQueryString(
          qParams));
			}

			if (path == null)
				return RET_DASHBOARD;

			HpcDataObjectListDTO datafiles = HpcClientUtil.getDatafiles(authToken, serviceURL, path, false, true, 
					sslCertPath, sslCertPassword);
			if (datafiles != null && datafiles.getDataObjects() != null && !datafiles.getDataObjects().isEmpty()) {
				HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute(ATTR_USER_DOC_MODEL);
				if (modelDTO == null) {
					modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
					session.setAttribute(ATTR_USER_DOC_MODEL, modelDTO);
				}
				String basePath = path.substring(0, StringUtils.ordinalIndexOf(path, "/", 2) < 0 ? path.length() : StringUtils.ordinalIndexOf(path, "/", 2));
                HpcDataManagementRulesDTO basePathRules = HpcClientUtil.getBasePathManagementRules(modelDTO, basePath);
                
				HpcDataObjectDTO dataFile = datafiles.getDataObjects().get(0);
				
				if (dataFile.getPermission().equals(HpcPermission.NONE)) {
					throw new HpcWebException(
				            "You do not have READ access to " + path + ".");
				}
				
				HpcDatafileModel hpcDatafile = buildHpcDataObject(dataFile,
						modelDTO.getDataObjectSystemGeneratedMetadataAttributeNames(), basePathRules.getCollectionMetadataValidationRules());
				hpcDatafile.setPath(path);
				model.addAttribute("hpcDatafile", hpcDatafile);
				model.addAttribute("attributeNames", getMetadataAttributeNames(dataFile));
								
				if (action != null && action.equals("edit")) {
					String collectionType = getParentCollectionType(dataFile);
					populateFormAttributes(request, session, model, basePath,
							collectionType, false, true);
					List<HpcMetadataAttrEntry> userMetadataEntries = (List<HpcMetadataAttrEntry>) session.getAttribute("metadataEntries");
					List<HpcMetadataAttrEntry> mergedMetadataEntries = mergeMatadataEntries(hpcDatafile.getSelfMetadataEntries(), userMetadataEntries);
					hpcDatafile.setSelfMetadataEntries(mergedMetadataEntries);
					model.addAttribute("hpcDatafile", hpcDatafile);
					model.addAttribute("action", "edit");
				}
				
				if (user.getUserRole().equals("SYSTEM_ADMIN") || user.getUserRole().equals("GROUP_ADMIN")) {
					if (dataFile.getPermission().equals(HpcPermission.OWN))
						model.addAttribute(ATTR_USER_PERMISSION, "DELETE");
					else
						model.addAttribute(ATTR_USER_PERMISSION, dataFile.getPermission().toString());
				} else
					model.addAttribute(ATTR_USER_PERMISSION, dataFile.getPermission().toString());
			} else {
				String message = "Data file not found!";
				model.addAttribute(ATTR_ERROR, message);
				return RET_DASHBOARD;
			}
		} catch (HpcWebException e) {
			model.addAttribute(ATTR_ERROR, e.getMessage());
			return "datafile";
		}
		return "datafile";
	}



	private String getParentCollectionType(HpcDataObjectDTO dataFile) {
		if (dataFile != null && dataFile.getMetadataEntries() != null
				&& dataFile.getMetadataEntries().getParentMetadataEntries() != null) {
			for (HpcMetadataEntry entry : dataFile.getMetadataEntries().getParentMetadataEntries()) {
				if (entry.getLevel() == 2 && entry.getAttribute().equals("collection_type"))
					return entry.getValue();
			}
		}
		return null;
	}
	
	private List<String> getMetadataAttributeNames(HpcDataObjectDTO dataFile) {
		List<String> names = new ArrayList<>();
		if (dataFile == null || dataFile.getMetadataEntries() == null
				|| dataFile.getMetadataEntries().getSelfMetadataEntries() == null
				|| dataFile.getMetadataEntries().getParentMetadataEntries() == null)
			return names;
		for (HpcMetadataEntry entry : dataFile.getMetadataEntries().getSelfMetadataEntries())
			names.add(entry.getAttribute());
		return names;
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
	public String updateCollection(@Valid @ModelAttribute("hpcDatafile") HpcDatafileModel hpcDatafile, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request, HttpServletResponse response,
			final RedirectAttributes redirectAttributes) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		String[] action = request.getParameterValues("action");
		if (action != null && action.length > 0 && action[0].equals("cancel")) {
		  final Map<String, String> qParams = new HashMap<>();
		  qParams.put("path", hpcDatafile.getPath());
		  qParams.put("action", "view");
      return "redirect:/datafile?".concat(MiscUtil.generateEncodedQueryString(
        qParams));
    }
		else if (action != null && action.length > 0 && action[0].equals("delete")) {
			boolean deleted = HpcClientUtil.deleteDatafile(authToken, serviceURL, hpcDatafile.getPath(), sslCertPath,
					sslCertPassword);
			if (deleted) {
				redirectAttributes.addFlashAttribute("error", "Data file " + hpcDatafile.getPath() + " is deleted!");
				session.removeAttribute("selectedUsers");
			}
		}

		try {
			if (hpcDatafile.getPath() == null || hpcDatafile.getPath().trim().length() == 0)
				model.addAttribute("error", "Invald Data file path");

			HpcDataObjectRegistrationRequestDTO registrationDTO = constructRequest(request);

			boolean updated = HpcClientUtil.updateDatafile(authToken, serviceURL, registrationDTO,
					hpcDatafile.getPath(), sslCertPath, sslCertPassword);
			if (updated) {
				redirectAttributes.addFlashAttribute("error", "Data file " + hpcDatafile.getPath() + " is Updated!");
				session.removeAttribute("selectedUsers");
			}
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to update data file: " + e.getMessage());
		} finally {
			model.addAttribute("hpcDatafile", hpcDatafile);
		}
		final Map<String, String> queryMap = new HashMap<>();
		queryMap.put("path", hpcDatafile.getPath());
		queryMap.put("action", "view");
		return "redirect:/datafile?".concat(MiscUtil.generateEncodedQueryString(
      queryMap));
	}

	@JsonView(Views.Public.class)
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public AjaxResponseBody deleteObject(@Valid @ModelAttribute("hpcDatafile") HpcDatafileModel hpcDatafile,
			Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {
		AjaxResponseBody result = new AjaxResponseBody();
		String authToken = (String) session.getAttribute("hpcUserToken");
		try {
			String path = request.getParameter("deletepath");
			if (path == null) {
				result.setMessage("Invaliad Data object path!");
				return result;
			}
			boolean deleted = HpcClientUtil.deleteDatafile(authToken, serviceURL, path, sslCertPath, sslCertPassword);
			if (deleted)
				result.setMessage("Data object deleted!");
		} catch (Exception e) {
			result.setMessage("Failed to delete object: " + e.getMessage());
		} finally {
			model.addAttribute("hpcDatafile", hpcDatafile);
		}
		return result;
	}

	private HpcDatafileModel buildHpcDataObject(HpcDataObjectDTO datafile, List<String> systemAttrs, List<HpcMetadataValidationRule> rules) {
		HpcDatafileModel model = new HpcDatafileModel();
		systemAttrs.add("collection_type");
		model.setDataObject(datafile.getDataObject());
		for (HpcMetadataEntry entry : datafile.getMetadataEntries().getSelfMetadataEntries()) {
			HpcMetadataAttrEntry attrEntry = new HpcMetadataAttrEntry();
			attrEntry.setAttrName(entry.getAttribute());
			if(entry.getAttribute().equalsIgnoreCase("source_file_size"))
			  attrEntry.setAttrValue(addHumanReadableSize(entry.getValue()));
			else
			  attrEntry.setAttrValue(entry.getValue());
			attrEntry.setAttrUnit(entry.getUnit());
			if (systemAttrs.contains(entry.getAttribute())) {
				attrEntry.setSystemAttr(true);
				model.getSelfSystemMetadataEntries().add(attrEntry);
			}
			else {
				attrEntry.setSystemAttr(false);
				model.getSelfMetadataEntries().add(attrEntry);
			}
			
		}

		for (HpcMetadataEntry entry : datafile.getMetadataEntries().getParentMetadataEntries()) {
			HpcMetadataAttrEntry attrEntry = new HpcMetadataAttrEntry();
			attrEntry.setAttrName(entry.getAttribute());
			attrEntry.setAttrValue(entry.getValue());
			attrEntry.setAttrUnit(entry.getUnit());
			attrEntry.setLevelLabel(entry.getLevelLabel());
			attrEntry.setSystemAttr(systemAttrs.contains(entry.getAttribute()));
            attrEntry.setEncrypted(isEncryptedAttribute(entry.getAttribute(), null, rules));
           
			if(!attrEntry.isEncrypted())
			    model.getParentMetadataEntries().add(attrEntry);
		}
		return model;
	}

    private HpcDataObjectRegistrationRequestDTO constructRequest(HttpServletRequest request) {
		Enumeration<String> params = request.getParameterNames();
		HpcDataObjectRegistrationRequestDTO dto = new HpcDataObjectRegistrationRequestDTO();
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
			} else if (paramName.startsWith("addAttrName")) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				String attrId = paramName.substring("addAttrName".length());
				String[] attrName = request.getParameterValues(paramName);
				String[] attrValue = request.getParameterValues("addAttrValue" + attrId);
				if (attrName.length > 0 && !attrName[0].isEmpty()) {
					entry.setAttribute(attrName[0]);
					if (attrValue.length > 0 && !attrValue[0].isEmpty())
						entry.setValue(attrValue[0]);
					else
						throw new HpcWebException("Invalid metadata attribute value. Empty value is not valid!");
				} else if (attrValue.length > 0 && !attrValue[0].isEmpty()) {
					throw new HpcWebException("Invalid metadata attribute name. Empty value is not valid!");
				} else {
					//If both attrName and attrValue are empty, then we just
					//ignore it and move to the next element
					continue;
				}

				metadataEntries.add(entry);
			}
		}
		dto.getMetadataEntries().addAll(metadataEntries);
		dto.setSource(null);
		return dto;
	}

    private String addHumanReadableSize(String value) {
        String humanReadableSize = FileUtils.byteCountToDisplaySize(Long.parseLong(value));
        return value + " (" + humanReadableSize + ")";
    }
    
    private boolean isEncryptedAttribute(String attribute, String collectionType, List<HpcMetadataValidationRule> rules) {
      for(HpcMetadataValidationRule rule: rules) {
        if (StringUtils.equals(rule.getAttribute(), attribute) && collectionType != null && rule.getCollectionTypes().contains(collectionType))
          return rule.getEncrypted();
        if (StringUtils.equals(rule.getAttribute(), attribute) && collectionType == null)
          return rule.getEncrypted();
      }
      return false;
    }
}
