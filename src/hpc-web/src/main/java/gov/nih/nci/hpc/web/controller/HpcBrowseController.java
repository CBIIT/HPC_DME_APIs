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

import org.apache.commons.lang.StringUtils;
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
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcBrowserEntry;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * HPC Web Browse controller. Builds tree nodes based on user DOC basepath and
 * then builds up the tree based on expanded nodes
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
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;

	/**
	 * GET operation on Browse. Builds initial tree
	 * 
	 * @param q
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String get(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {

		// Verify User session
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (user == null || authToken == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/login?returnPath=browse";
		}

		// Get User DOC model for base path
		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
		if (modelDTO == null) {
			modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
			session.setAttribute("userDOCModel", modelDTO);
		}
		String partial = request.getParameter("partial");
		String path = null;
		if (partial != null)
			return "browsepartial";

		if (request.getParameter("refresh") != null) {
			session.removeAttribute("browserEntry");
		}

		if (path == null || path.isEmpty()) {
			path = request.getParameter("path");
			if (path == null || path.isEmpty()) {
				path = (String) request.getAttribute("path");
			}
		}
		String selectedBrowsePath = (String) session.getAttribute("selectedBrowsePath");
		
		if (selectedBrowsePath != null && (path == null || path.isEmpty()))
			session.removeAttribute("browserEntry");
		if (path == null || path.isEmpty() || request.getParameter("base") != null)
			path = user.getDefaultBasePath();

		// If browser tree nodes are cached, return cached data. If not, query
		// browser tree nodes based on the base path and cache it.
		try {
			if (path != null) {
				path = path.trim();
				session.setAttribute("selectedBrowsePath", path);

				HpcBrowserEntry browserEntry = (HpcBrowserEntry) session.getAttribute("browserEntry");
				if (browserEntry == null) {
					browserEntry = new HpcBrowserEntry();
					browserEntry.setCollection(true);
					browserEntry.setFullPath(path);
					browserEntry.setId(path);
					browserEntry.setName(path);
					browserEntry = getTreeNodes(path, browserEntry, authToken, model, false, true, false);
					if (request.getParameter("base") == null)
						browserEntry = addPathEntries(path, browserEntry);
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
			}
			model.addAttribute("basePath", user.getDefaultBasePath());
			return "browse";
		} catch (Exception e) {
			model.addAttribute("message", "Failed to get tree. Reason: " + e.getMessage());
			e.printStackTrace();
			return "browse";
		}
	}

	private HpcBrowserEntry addPathEntries(String path, HpcBrowserEntry browserEntry) {
		if (path.indexOf("/") != -1) {
			String[] paths = path.split("/");
			for (int i = paths.length - 2; i >= 0; i--) {
				if (paths[i].isEmpty())
					continue;
				browserEntry = addPathEntry(path, paths[i], browserEntry);
			}
		}
		return browserEntry;
	}

	private HpcBrowserEntry addPathEntry(String fullPath, String path, HpcBrowserEntry childEntry) {
		HpcBrowserEntry entry = new HpcBrowserEntry();
		String entryPath = fullPath.substring(0, (fullPath.indexOf("/" + path) + ("/" + path).length()));
		entry.setCollection(true);
		entry.setId(entryPath);
		entry.setFullPath(entryPath);
		entry.setPopulated(true);
		entry.setName(path);
		entry.getChildren().add(childEntry);
		return entry;
	}

	/**
	 * POST Action. When a tree node is expanded, this action fetches its child
	 * nodes
	 * 
	 * @param hpcBrowserEntry
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String browse(@Valid @ModelAttribute("hpcBrowse") HpcBrowserEntry hpcBrowserEntry, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request, HttpServletResponse response,
			final RedirectAttributes redirectAttributes, String refreshNode) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcBrowserEntry browserEntry = (HpcBrowserEntry) session.getAttribute("browserEntry");
		boolean getChildren = false;
		boolean refresh = false;

		if(!StringUtils.isBlank(refreshNode)) {
			getChildren = true;
			refresh = true;
		}
		log.info("refreshNode            : " + refreshNode);
		log.info("browserEntry (session) : " + browserEntry);
		log.info("browserEntry (model)   : " + hpcBrowserEntry);
		log.info("getChildren            : " + getChildren);

		try {
			if (hpcBrowserEntry.getSelectedNodePath() != null) {

				// session.setAttribute("selectedBrowsePath",
				// hpcBrowserEntry.getSelectedNodePath());
				browserEntry = getTreeNodes(hpcBrowserEntry.getSelectedNodePath().trim(), browserEntry, authToken,
						model, getChildren, hpcBrowserEntry.isPartial(), refresh);
				if (hpcBrowserEntry.isPartial())
					browserEntry = addPathEntries(hpcBrowserEntry.getSelectedNodePath().trim(), browserEntry);

				browserEntry = trimPath(browserEntry, browserEntry.getName());
				List<HpcBrowserEntry> entries = new ArrayList<HpcBrowserEntry>();
				entries.add(browserEntry);
				model.addAttribute("browserEntryList", entries);
				model.addAttribute("browserEntry", browserEntry);
				model.addAttribute("scrollLoc", hpcBrowserEntry.getScrollLoc());
				session.setAttribute("browserEntry", browserEntry);
			}
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to update data file: " + e.getMessage());
		} finally {
		}
		return "browse";
	}

	/**
	 * Get child Tree nodes for selected tree node and merge it with cached
	 * nodes
	 * 
	 * @param path
	 * @param browserEntry
	 * @param authToken
	 * @param model
	 * @param getChildren
	 * @param refresh
	 * @return
	 */
	private HpcBrowserEntry getTreeNodes(String path, HpcBrowserEntry browserEntry, String authToken, Model model,
										 boolean getChildren, boolean partial, boolean refresh) {
		path = path.trim();
		HpcBrowserEntry selectedEntry = getSelectedEntry(path, browserEntry);
		log.info("selectedEntry: " + selectedEntry);
		log.info("partial: " + partial);
		log.info("populated: " + selectedEntry.isPopulated());

		if (selectedEntry != null && selectedEntry.isPopulated() && !refresh)
			return partial ? selectedEntry : browserEntry;
		if (selectedEntry != null && selectedEntry.getChildren() != null)
			log.info("clearing children");
			selectedEntry.getChildren().clear();
		if (selectedEntry == null)
			selectedEntry = new HpcBrowserEntry();

		log.info("getting collection at path: " + path);
		HpcCollectionListDTO collections = HpcClientUtil.getCollection(authToken, collectionURL, path, true, false,
				sslCertPath, sslCertPassword);

		for (HpcCollectionDTO collectionDTO : collections.getCollections()) {
			HpcCollection collection = collectionDTO.getCollection();
			selectedEntry.setFullPath(collection.getAbsolutePath());
			selectedEntry.setId(collection.getAbsolutePath());
			selectedEntry.setName(collection.getCollectionName());
			if (getChildren)
				selectedEntry.setPopulated(true);
			else
				selectedEntry.setPopulated(false);
			selectedEntry.setCollection(true);
			for (HpcCollectionListingEntry listEntry : collection.getSubCollections()) {
				HpcBrowserEntry listChildEntry = new HpcBrowserEntry();
				listChildEntry.setCollection(true);
				listChildEntry.setFullPath(listEntry.getPath());
				listChildEntry.setId(listEntry.getPath());
				listChildEntry.setName(listEntry.getPath());
				listChildEntry.setPopulated(false);
				if (getChildren)
					listChildEntry = getTreeNodes(listEntry.getPath(), listChildEntry, authToken, model, false,
							partial, false);
				else {
					HpcBrowserEntry emptyEntry = new HpcBrowserEntry();
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
			if (selectedEntry.getChildren() == null || selectedEntry.getChildren().isEmpty()) {
				HpcBrowserEntry listChildEntry = new HpcBrowserEntry();
				listChildEntry.setCollection(false);
				listChildEntry.setFullPath(" ");
				listChildEntry.setId(" ");
				listChildEntry.setName(" ");
				listChildEntry.setPopulated(true);
				selectedEntry.getChildren().add(listChildEntry);
			}
		}
		return partial ? selectedEntry : browserEntry;
	}

	private HpcBrowserEntry getSelectedEntry(String path, HpcBrowserEntry browserEntry) {
		if (browserEntry == null)
			return null;

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

	private HpcBrowserEntry trimPath(HpcBrowserEntry entry, String parentPath) {
		for (HpcBrowserEntry child : entry.getChildren()) {
			String childPath = child.getFullPath();
			if (childPath == null || childPath.equals(""))
				continue;
			if (childPath.indexOf(parentPath) != -1) {
				String childName = childPath.substring(childPath.indexOf(parentPath) + parentPath.length() + 1);
				child.setName(childName);
			}
			trimPath(child, childPath);
		}
		return entry;
	}

}
