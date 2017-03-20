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
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementTreeDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementTreeEntry;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcBrowserEntry;
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
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.tree}")
	private String treeServiceURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String docServiceURL;

	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String get(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			model.addAttribute("message", "Invalid user session!");
			return "hpcLogin";
		}

		try {
			HpcBrowserEntry browserEntry = new HpcBrowserEntry();
			browserEntry.setFolder(true);
			browserEntry = populateBrowserEntries(browserEntry, user.getDoc(), authToken, model);
			browserEntry = trimPath(browserEntry, browserEntry.getName());
			if (browserEntry != null) {
				List<HpcBrowserEntry> entries = new ArrayList<HpcBrowserEntry>();
				entries.add(browserEntry);
				model.addAttribute("browserEntry", entries);
			} else
				model.addAttribute("message", "No collections found!");
			return "browse";
			
//			HpcDataManagementModelDTO docModel = HpcClientUtil.getDOCModel(authToken, docServiceURL,
//					user.getNciAccount().getDoc(), sslCertPath, sslCertPassword);
//			if (docModel == null) {
//				model.addAttribute("message", "Invalid DOC base path!");
//				return "browse";
//			} else {
//				HpcBrowserEntry browserEntry = new HpcBrowserEntry();
//				browserEntry.setFolder(true);
//				browserEntry = populateBrowserEntries(browserEntry, user.getNciAccount().getDoc(), authToken, model);
//				//browserEntry = trimPath(browserEntry, docModel.getBasePath());
//				if (browserEntry != null) {
//					List<HpcBrowserEntry> entries = new ArrayList<HpcBrowserEntry>();
//					entries.add(browserEntry);
//					model.addAttribute("browserEntry", entries);
//				} else
//					model.addAttribute("message", "No collections found!");
//				return "browse";
//			}
		} catch (Exception e) {
			e.printStackTrace();
			return "browse";
		}
	}

	private HpcBrowserEntry populateBrowserEntries(HpcBrowserEntry browserEntry, String doc,
			String authToken, Model model) {
		HpcDataManagementTreeDTO tree = HpcClientUtil.getDOCTree(authToken, treeServiceURL, doc, true,
				sslCertPath, sslCertPassword);
		if (tree == null) {
			model.addAttribute("message", "No Heirarchy!");
			return null;
		}
		HpcDataManagementTreeEntry basePath = tree.getBasePath();
		browserEntry.setFolder(true);
		browserEntry.setFullPath(basePath.getPath());
		browserEntry.setName(basePath.getPath());
		buildTree(browserEntry, basePath);
		return browserEntry;
	}

	private HpcBrowserEntry buildTree(HpcBrowserEntry browseEntry, HpcDataManagementTreeEntry treeEntry)
	{
		if(treeEntry == null)
			return browseEntry;
		
		List<String> dataObjects = treeEntry.getDataObjects();
		for(String dataObject : dataObjects)
		{
			HpcBrowserEntry entry = new HpcBrowserEntry();
			entry.setFullPath(dataObject);
			entry.setName(dataObject);
			entry.setFolder(false);
			browseEntry.getChildren().add(entry);
		}
		List<HpcDataManagementTreeEntry> collections = treeEntry.getSubCollections();
		for(HpcDataManagementTreeEntry collection : collections)
		{
			HpcBrowserEntry entry = new HpcBrowserEntry();
			entry.setFullPath(collection.getPath());
			entry.setFolder(true);
			entry.setName(collection.getPath());
			browseEntry.getChildren().add(entry);
			buildTree(entry, collection);
		}
		return browseEntry;
	}

	private HpcBrowserEntry trimPath(HpcBrowserEntry entry, String parentPath) {
		String path = entry.getName();
		for (HpcBrowserEntry child : entry.getChildren()) {
			String childPath = child.getName();
			String childName = childPath.substring(parentPath.length() + 1);
			child.setName(childName);
			trimPath(child, childPath);
		}
		return entry;
	}

}
