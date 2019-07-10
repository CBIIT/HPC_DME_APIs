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

import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.annotation.JsonView;

import gov.nih.nci.hpc.domain.databrowse.HpcBookmark;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkListDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.Views;
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
	@Value("${gov.nih.nci.hpc.server.bookmark}")
	private String bookmarkServiceURL;
	
	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

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
		
		try {
			// Verify User session
 			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (user == null || authToken == null) {
				ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
				bindingResult.addError(error);
				HpcLogin hpcLogin = new HpcLogin();
				model.addAttribute("hpcLogin", hpcLogin);
				return "redirect:/login";
			}

			@SuppressWarnings("unchecked")
			List<HpcBookmark> bookmarks = (List<HpcBookmark>) session.getAttribute("bookmarks");
			if (bookmarks == null) {
				HpcBookmarkListDTO dto = HpcClientUtil.getBookmarks(authToken, bookmarkServiceURL, sslCertPath,
						sslCertPassword);
				bookmarks = dto.getBookmarks();
				bookmarks.sort(Comparator.comparing(HpcBookmark::getName));
				session.setAttribute("bookmarks", bookmarks);
			}

			model.addAttribute("bookmarksList", bookmarks);

		} catch (Exception e) {
			String errMsg = "Failed to retrieve bookmarks. Reason: " + e.getMessage();
			model.addAttribute("message", errMsg);
			logger.error(errMsg, e);
			return "bookmarks";
		}
		return "bookmarks";
	}

	/**
	 * Delete Bookmark POST action
	 * 
	 * @param hpcWebUser
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @return
	 */
	@JsonView(Views.Public.class)
	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public AjaxResponseBody bookmark(@Valid @ModelAttribute("hpcBookmark") HpcBookmark hpcBookmark, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		AjaxResponseBody result = new AjaxResponseBody();

		try {
			if (hpcBookmark.getName() == null) {
				model.addAttribute("message", "Invald user input");
			} else {
				HpcBookmarkRequestDTO dto = new HpcBookmarkRequestDTO();
				dto.setPath(hpcBookmark.getPath().trim());
				boolean deleted = HpcClientUtil.deleteBookmark(authToken, bookmarkServiceURL, hpcBookmark.getName(),
						sslCertPath, sslCertPassword);
				if (deleted) {
					result.setMessage("Bookmark deleted!");
					HpcBookmarkListDTO bookmarksDTO = HpcClientUtil.getBookmarks(authToken, bookmarkServiceURL,
							sslCertPath, sslCertPassword);
					List<HpcBookmark> bookmarks = bookmarksDTO.getBookmarks();
					bookmarks.sort(Comparator.comparing(HpcBookmark::getName));
					model.addAttribute("bookmarksList", bookmarks);
					session.setAttribute("bookmarks", bookmarks);
				}
			}
		} catch (Exception e) {
			String errMsg = "Failed to delete bookmark: " + e.getMessage();
			result.setMessage(errMsg);
			logger.error(errMsg, e);
		}
		return result;
	}
}
