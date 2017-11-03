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
@RequestMapping("/collection")
public class HpcCollectionController extends AbstractHpcController {
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
	public String home(@RequestBody(required = false) String body, @RequestParam String path,
			@RequestParam String action, Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		try {
			// User Session validation
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			String userId = (String) session.getAttribute("hpcUserId");
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (user == null || authToken == null) {
				ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
				bindingResult.addError(error);
				HpcLogin hpcLogin = new HpcLogin();
				model.addAttribute("hpcLogin", hpcLogin);
				return "redirect:/login?returnPath=collection&action="+action+"&path="+path;
			}

			if (path == null)
				return "dashboard";

			// Get collection
			HpcCollectionListDTO collections = HpcClientUtil.getCollection(authToken, serviceURL, path, false,
					sslCertPath, sslCertPassword);
			if (collections != null && collections.getCollections() != null
					&& collections.getCollections().size() > 0) {
				HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
				if (modelDTO == null) {
					modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
					session.setAttribute("userDOCModel", modelDTO);
				}
				// Get collection permissions to enable Edit, Permission icons
				// on the UI
				HpcUserPermissionDTO permission = HpcClientUtil.getPermissionForUser(authToken, path, userId,
						serviceURL, sslCertPath, sslCertPassword);
				HpcCollectionDTO collection = collections.getCollections().get(0);
				HpcCollectionModel hpcCollection = buildHpcCollection(collection,
						modelDTO.getCollectionSystemGeneratedMetadataAttributeNames());
				model.addAttribute("collection", hpcCollection);
				model.addAttribute("userpermission", (permission != null && permission.getPermission() != null)
						? permission.getPermission().toString() : "null");
				model.addAttribute("attributeNames", getMetadataAttributeNames(collection));
				if (action != null && action.equals("edit"))
					if (permission == null || permission.getPermission().equals(HpcPermission.NONE)
							|| permission.getPermission().equals(HpcPermission.READ)) {
						model.addAttribute("error",
								"No edit permission. Please contact collection owner for write access.");
						model.addAttribute("action", "view");
					} else
						model.addAttribute("action", "edit");
			} else {
				String message = "Collection not found!";
				model.addAttribute("error", message);
				return "dashboard";
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			model.addAttribute("error", "Failed to get Collection: " + e.getMessage());
			e.printStackTrace();
		}
		model.addAttribute("hpcCollection", new HpcCollectionModel());
		return "collection";
	}

	private List<String> getMetadataAttributeNames(HpcCollectionDTO collection) {
		List<String> names = new ArrayList<String>();
		if (collection == null || collection.getMetadataEntries() == null
				|| collection.getMetadataEntries().getSelfMetadataEntries() == null
				|| collection.getMetadataEntries().getParentMetadataEntries() == null
				)
			return names;
		for(HpcMetadataEntry entry : collection.getMetadataEntries().getSelfMetadataEntries())
			names.add(entry.getAttribute());
		return names;
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
				redirectAttributes.addFlashAttribute("error", "Collection " + hpcCollection.getPath() + " is Updated!");
				session.removeAttribute("selectedUsers");
			}
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to update data file: " + e.getMessage());
		}
		return "redirect:/collection?path=" + hpcCollection.getPath() + "&action=view";
	}

	private HpcCollectionModel buildHpcCollection(HpcCollectionDTO collection, List<String> systemAttrs) {
		HpcCollectionModel model = new HpcCollectionModel();
		systemAttrs.add("collection_type");
		model.setCollection(collection.getCollection());
		if (collection.getMetadataEntries() == null)
			return model;

		for (HpcMetadataEntry entry : collection.getMetadataEntries().getSelfMetadataEntries()) {
			HpcMetadataAttrEntry attrEntry = new HpcMetadataAttrEntry();
			attrEntry.setAttrName(entry.getAttribute());
			attrEntry.setAttrValue(entry.getValue());
			attrEntry.setAttrUnit(entry.getUnit());
			if (systemAttrs != null && systemAttrs.contains(entry.getAttribute()))
				attrEntry.setSystemAttr(true);
			else
				attrEntry.setSystemAttr(false);
			model.getSelfMetadataEntries().add(attrEntry);
		}

		for (HpcMetadataEntry entry : collection.getMetadataEntries().getParentMetadataEntries()) {
			HpcMetadataAttrEntry attrEntry = new HpcMetadataAttrEntry();
			attrEntry.setAttrName(entry.getAttribute());
			attrEntry.setAttrValue(entry.getValue());
			attrEntry.setAttrUnit(entry.getUnit());
			if (systemAttrs != null && systemAttrs.contains(entry.getAttribute()))
				attrEntry.setSystemAttr(true);
			else
				attrEntry.setSystemAttr(false);
			model.getParentMetadataEntries().add(attrEntry);
		}
		return model;
	}

	private HpcCollectionRegistrationDTO constructRequest(HttpServletRequest request, HttpSession session, String path)
			throws HpcWebException {
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
			} else if (paramName.startsWith("addAttrName")) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				String attrId = paramName.substring("addAttrName".length());
				String[] attrName = request.getParameterValues(paramName);
				String[] attrValue = request.getParameterValues("addAttrValue" + attrId);
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
		return dto;
	}

}
