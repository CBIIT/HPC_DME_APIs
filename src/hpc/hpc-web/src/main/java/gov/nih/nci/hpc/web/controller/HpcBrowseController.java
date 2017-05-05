/**
 * HpcLoginController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementTreeDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementTreeEntry;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcBrowserEntry;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * HPC Web Browse controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcBrowseController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/browse")
public class HpcBrowseController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionURL;
	@Value("${gov.nih.nci.hpc.server.tree}")
	private String treeServiceURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String docServiceURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;

	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String get(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (user == null || authToken == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}
		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
		if (modelDTO == null) {
			modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, user.getDoc(), sslCertPath, sslCertPassword);
			session.setAttribute("userDOCModel", modelDTO);
		}
		try {
			HpcBrowserEntry browserEntry = (HpcBrowserEntry) session.getAttribute("browserEntry");
			if (browserEntry == null) {
				browserEntry = new HpcBrowserEntry();
				browserEntry.setCollection(true);
				String basePath = modelDTO.getBasePath();
				browserEntry.setFullPath(basePath);
				browserEntry.setId(basePath);
				browserEntry.setName(basePath);
				browserEntry = getTreeNodes(basePath, browserEntry, authToken, model, false);
				browserEntry = trimPath(browserEntry, browserEntry.getName());
				session.setAttribute("browserEntry", browserEntry);
			}

			if (browserEntry != null) {
				List<HpcBrowserEntry> entries = new ArrayList<HpcBrowserEntry>();
				entries.add(browserEntry);
				model.addAttribute("browserEntryList", entries);
				model.addAttribute("browserEntry", browserEntry);
			} else
				model.addAttribute("message", "No collections found!");
			model.addAttribute("basePath", modelDTO.getBasePath());
			return "browse";
		} catch (Exception e) {
			e.printStackTrace();
			return "browse";
		}
	}

	private HpcBrowserEntry getTreeNodes(String path, HpcBrowserEntry browserEntry, String authToken, Model model,
			boolean getChildren) {
		HpcBrowserEntry selectedEntry = getSelectedEntry(path, browserEntry);
		if (selectedEntry == null) {
			return browserEntry;
		} else if (selectedEntry.isPopulated())
			return browserEntry;
		if (selectedEntry.getChildren() != null)
			selectedEntry.getChildren().clear();
		HpcCollectionListDTO collections = HpcClientUtil.getCollection(authToken, collectionURL, path, true, false,
				sslCertPath, sslCertPassword);

		for (HpcCollectionDTO collectionDTO : collections.getCollections()) {
			HpcCollection collection = collectionDTO.getCollection();
			boolean isFolder = false;
			selectedEntry.setFullPath(collection.getAbsolutePath());
			selectedEntry.setId(collection.getAbsolutePath());
			selectedEntry.setName(collection.getCollectionName());
			if (getChildren)
				selectedEntry.setPopulated(true);
			else
				selectedEntry.setPopulated(false);
			for (HpcCollectionListingEntry listEntry : collection.getSubCollections()) {
				selectedEntry.setCollection(true);
				HpcBrowserEntry listChildEntry = new HpcBrowserEntry();
				listChildEntry.setCollection(true);
				listChildEntry.setFullPath(listEntry.getPath());
				listChildEntry.setId(listEntry.getPath());
				listChildEntry.setName(listEntry.getPath());
				listChildEntry.setPopulated(false);
				if (getChildren)
					listChildEntry = getTreeNodes(listEntry.getPath(), listChildEntry, authToken, model, false);
				else {
					HpcBrowserEntry emptyEntry = new HpcBrowserEntry();
					// emptyEntry.setName("<div style=\"display:block;\"><img
					// width=\"50\" height=\"50\" src=\"img/spinner.gif\"
					// alt=\"Wait\" /></div>");
					emptyEntry.setName("");
					listChildEntry.getChildren().add(emptyEntry);
				}
				selectedEntry.getChildren().add(listChildEntry);
			}
			for (HpcCollectionListingEntry listEntry : collection.getDataObjects()) {
				selectedEntry.setCollection(true);
				HpcBrowserEntry listChildEntry = new HpcBrowserEntry();
				listChildEntry.setCollection(false);
				listChildEntry.setFullPath(listEntry.getPath());
				listChildEntry.setId(listEntry.getPath());
				listChildEntry.setName(listEntry.getPath());
				listChildEntry.setPopulated(true);
				selectedEntry.getChildren().add(listChildEntry);
			}
		}
		return browserEntry;
	}

	private HpcBrowserEntry getSelectedEntry(String path, HpcBrowserEntry browserEntry) {
		if (browserEntry.getFullPath() != null && browserEntry.getFullPath().equals(path))
			return browserEntry;

		for (HpcBrowserEntry childEntry : browserEntry.getChildren()) {
			if (childEntry.getFullPath() != null && childEntry.getFullPath().equals(path))
				return childEntry;
			else {
				HpcBrowserEntry entry = getSelectedEntry(path, childEntry);
				if (entry != null)
					return entry;
			}
		}
		return null;
	}

	private HpcBrowserEntry populateBrowserEntries(HpcBrowserEntry browserEntry, String doc, String authToken,
			Model model) {
		HpcDataManagementTreeDTO tree = HpcClientUtil.getDOCTree(authToken, treeServiceURL, doc, true, sslCertPath,
				sslCertPassword);
		if (tree == null) {
			model.addAttribute("message", "No Heirarchy!");
			return null;
		}
		HpcDataManagementTreeEntry basePath = tree.getBasePath();
		browserEntry.setCollection(true);
		browserEntry.setFullPath(basePath.getPath());
		browserEntry.setId(basePath.getPath());
		browserEntry.setName(basePath.getPath());
		buildTree(browserEntry, basePath);
		return browserEntry;
	}

	private HpcBrowserEntry buildTree(HpcBrowserEntry browseEntry, HpcDataManagementTreeEntry treeEntry) {
		if (treeEntry == null)
			return browseEntry;

		List<String> dataObjects = treeEntry.getDataObjects();
		for (String dataObject : dataObjects) {
			HpcBrowserEntry entry = new HpcBrowserEntry();
			entry.setFullPath(dataObject);
			entry.setId(dataObject);
			entry.setName(dataObject);
			entry.setCollection(false);
			browseEntry.getChildren().add(entry);
		}
		List<HpcDataManagementTreeEntry> collections = treeEntry.getSubCollections();
		for (HpcDataManagementTreeEntry collection : collections) {
			HpcBrowserEntry entry = new HpcBrowserEntry();
			entry.setFullPath(collection.getPath());
			entry.setId(collection.getPath());
			entry.setCollection(true);
			entry.setName(collection.getPath());
			browseEntry.getChildren().add(entry);
			buildTree(entry, collection);
		}
		return browseEntry;
	}

	private HpcBrowserEntry trimPath(HpcBrowserEntry entry, String parentPath) {
		String path = entry.getName();
		for (HpcBrowserEntry child : entry.getChildren()) {
			String childPath = child.getFullPath();
			if (childPath == null || childPath.equals(""))
				continue;
			if (childPath.indexOf(parentPath) != -1) {
				String childName = childPath.substring(parentPath.length() + 1);
				child.setName(childName);
			}
			trimPath(child, childPath);
		}
		return entry;
	}

	/*
	 * Action for browse
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String browse(@Valid @ModelAttribute("hpcBrowse") HpcBrowserEntry hpcBrowserEntry, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request, HttpServletResponse response,
			final RedirectAttributes redirectAttributes) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcBrowserEntry browserEntry = (HpcBrowserEntry) session.getAttribute("browserEntry");

		try {
			if (hpcBrowserEntry.getSelectedNodePath() != null) {
				browserEntry = getTreeNodes(hpcBrowserEntry.getSelectedNodePath(), browserEntry, authToken, model,
						true);
				browserEntry = trimPath(browserEntry, browserEntry.getName());
				List<HpcBrowserEntry> entries = new ArrayList<HpcBrowserEntry>();
				entries.add(browserEntry);
				model.addAttribute("browserEntryList", entries);
				model.addAttribute("browserEntry", browserEntry);
				session.setAttribute("browserEntry", browserEntry);
			}
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to update data file: " + e.getMessage());
		} finally {
		}
		return "browse";
	}
}
