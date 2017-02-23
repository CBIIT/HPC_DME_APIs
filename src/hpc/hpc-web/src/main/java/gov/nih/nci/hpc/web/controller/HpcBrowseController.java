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
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.annotation.JsonView;

import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcBrowserEntry;
import gov.nih.nci.hpc.web.model.Views;
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
	@Value("${gov.nih.nci.hpc.server.model}")
	private String docServiceURL;

	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String get(@RequestBody(required = false) String q, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcBrowserEntry result = new HpcBrowserEntry();
		result.setFolder(true);
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			model.addAttribute("message", "Invalid user session!");
			return "hpcLogin";
		}

		try {
			HpcDataManagementModelDTO docModel = HpcClientUtil.getDOCModel(authToken, docServiceURL, user.getNciAccount().getDoc(),
					sslCertPath, sslCertPassword);
			if (docModel == null) {
				model.addAttribute("message", "Invalid DOC base path!");
				return "browse";
			} else {
				HpcBrowserEntry browserEntry = new HpcBrowserEntry();
				browserEntry.setFolder(true);
				browserEntry = populateBrowserEntries(browserEntry, docModel.getBasePath(), authToken, model);
				browserEntry = trimPath(browserEntry, docModel.getBasePath());
				if(browserEntry != null)
				{
					List<HpcBrowserEntry> entries = new ArrayList<HpcBrowserEntry>();
					entries.add(browserEntry);
					model.addAttribute("browserEntry", entries);
				}
				else
					model.addAttribute("message", "No collections found!");
				return "browse";
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "browse";
		}
	}
	
	private HpcBrowserEntry populateBrowserEntries(HpcBrowserEntry browserEntry, String collectionPath, String authToken, Model model)
	{
		HpcCollectionListDTO colleciton = HpcClientUtil.getCollection(authToken, serviceURL, collectionPath, true,
				sslCertPath, sslCertPassword);				
		if (colleciton == null) {
			model.addAttribute("message", "No Collections!");
			return null;
		}
		for(HpcCollectionDTO dto : colleciton.getCollections())
		{
			browserEntry.setName(dto.getCollection().getAbsolutePath());
			browserEntry.setFullPath(dto.getCollection().getAbsolutePath());
			for(HpcCollectionListingEntry entry : dto.getCollection().getSubCollections())
			{
				HpcBrowserEntry childEntry = new HpcBrowserEntry();
				childEntry.setFolder(true);
				childEntry = populateBrowserEntries(childEntry, entry.getPath(), authToken, model);
				if(childEntry != null)
					browserEntry.getChildren().add(childEntry);
			}
			for(HpcCollectionListingEntry entry : dto.getCollection().getDataObjects())
			{
				HpcBrowserEntry childEntry = new HpcBrowserEntry();
				childEntry.setFolder(false);
				childEntry.setName(entry.getPath());
				childEntry.setFullPath(entry.getPath());
				browserEntry.getChildren().add(childEntry);
			}
			return browserEntry;
		}
		return null;
	}
	///FNL_SF_Archive/eran-pi-lab/eran-project/eran-flowcell/eran-sample
	
	private HpcBrowserEntry trimPath(HpcBrowserEntry entry, String parentPath)
	{
		String path = entry.getName();
		for(HpcBrowserEntry child : entry.getChildren())
		{
			String childPath = child.getName();
			String childName = childPath.substring(parentPath.length()+1);
			child.setName(childName);
			trimPath(child, childPath);
		}
		return entry;
	}
	
}
