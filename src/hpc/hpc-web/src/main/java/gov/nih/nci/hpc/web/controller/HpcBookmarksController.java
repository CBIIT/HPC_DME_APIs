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
import java.util.Collections;
import java.util.Comparator;
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
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcBookmark;
import gov.nih.nci.hpc.web.model.HpcBrowserEntry;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * HPC Web Bookmarks controller. Get user bookmarks to display
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/bookmarks")
public class HpcBookmarksController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;

	/**
	 * GET operation on bookmarks
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

		try
		{
		// Verify User session
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (user == null || authToken == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}

		@SuppressWarnings("unchecked")
		List<HpcBookmark> bookmarks = (List<HpcBookmark>) session.getAttribute("bookmarks");
			if (bookmarks == null) {
				bookmarks = new ArrayList<HpcBookmark>();
				HpcBookmark mark1 = new HpcBookmark();
				mark1.setName("eran-project");
				mark1.setPath("/FNL_SF_Archive/eran-pi-lab/eran-project");
				bookmarks.add(mark1);
				HpcBookmark mark2 = new HpcBookmark();
				mark2.setName("PI_Ji_Luo");
				mark2.setPath("/FNL_SF_Archive/PI_Ji_Luo");
				bookmarks.add(mark2);
				HpcBookmark mark3 = new HpcBookmark();
				mark3.setName("Sample_CRISPR_HZ_Luo_3_N701");
				mark3.setPath("/FNL_SF_Archive/PI_Ji_Luo/Project_Ji_Luo_Next_Year_4_shRNA_lib_7_6_16/Cell_AN3KP/Sample_CRISPR_HZ_Luo_3_N701");
				bookmarks.add(mark3);
				HpcBookmark mark4 = new HpcBookmark();
				mark4.setName("Cell_AN3KP4");
				mark4.setPath("/FNL_SF_Archive/PI_Ji_Luo/Project_Ji_Luo_Next_Year_4_shRNA_lib_7_6_16/Cell_AN3KP");
				bookmarks.add(mark4);
				HpcBookmark mark5 = new HpcBookmark();
				mark5.setName("Cell_AN3KP5");
				mark5.setPath("/FNL_SF_Archive/PI_Ji_Luo/Project_Ji_Luo_Next_Year_4_shRNA_lib_7_6_16/Cell_AN3KP");
				bookmarks.add(mark5);
				HpcBookmark mark6 = new HpcBookmark();
				mark6.setName("Cell_AN3KP6");
				mark6.setPath("/FNL_SF_Archive/PI_Ji_Luo/Project_Ji_Luo_Next_Year_4_shRNA_lib_7_6_16/Cell_AN3KP");
				bookmarks.add(mark6);
				HpcBookmark mark7 = new HpcBookmark();
				mark7.setName("Cell_AN3KP7");
				mark7.setPath("/FNL_SF_Archive/PI_Ji_Luo/Project_Ji_Luo_Next_Year_4_shRNA_lib_7_6_16/Cell_AN3KP");
				bookmarks.add(mark7);
				HpcBookmark mark8 = new HpcBookmark();
				mark8.setName("Cell_AN3KP8");
				mark8.setPath("/FNL_SF_Archive/PI_Ji_Luo/Project_Ji_Luo_Next_Year_4_shRNA_lib_7_6_16/Cell_AN3KP");
				bookmarks.add(mark8);
				HpcBookmark mark9 = new HpcBookmark();
				mark9.setName("Cell_AN3KP9");
				mark9.setPath("/FNL_SF_Archive/PI_Ji_Luo/Project_Ji_Luo_Next_Year_4_shRNA_lib_7_6_16/Cell_AN3KP");
				bookmarks.add(mark9);
				HpcBookmark mark10 = new HpcBookmark();
				mark10.setName("Cell_AN3KP10");
				mark10.setPath("/FNL_SF_Archive/PI_Ji_Luo/Project_Ji_Luo_Next_Year_4_shRNA_lib_7_6_16/Cell_AN3KP");
				bookmarks.add(mark10);
				bookmarks.sort(Comparator.comparing(HpcBookmark::getName));
			}

			if (bookmarks != null) {
				model.addAttribute("bookmarksList", bookmarks);
			} else
				model.addAttribute("message", "No bookmarks found!");

		} catch (Exception e) {
			model.addAttribute("message", "Failed to get tree. Reason: " + e.getMessage());
			e.printStackTrace();
			return "bookmarks";
		}
		return "bookmarks";
	}

}
