/**
 * HpcBookmarkController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.util.ArrayList;
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
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkListDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcBookmark;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcWebUser;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Create Bookmark controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/bookmark")
public class HpcBookmarkController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.bookmark}")
	private String bookmarkServiceURL;
	
	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	/**
	 * Prepare create bookmark page.
	 * 
	 * @param q
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		List<String> messages = new ArrayList<String>();
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (user == null || authToken == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/login";
		}

		String path = request.getParameter("path");

		//Check if this path is already bookmarked
		boolean bookmarkExists = false;
		List<gov.nih.nci.hpc.domain.databrowse.HpcBookmark> bookmarks = fetchCurrentUserBookmarks(session);
		for(gov.nih.nci.hpc.domain.databrowse.HpcBookmark bookmark: bookmarks) {
			if(bookmark.getPath().contentEquals(path)) {
				bookmarkExists = true;
				messages.add("Bookmark already exists: " + bookmark.getName());
				model.addAttribute("messages", messages);
				break;
			}
		}
		model.addAttribute("bookmarkExists", Boolean.toString(bookmarkExists));

		HpcBookmark bookmark = new HpcBookmark();
		bookmark.setSelectedPath(path != null ? path.trim() : null);
		bookmark.setPath(path != null ? path.trim() : null);
		model.addAttribute("bookmark", bookmark);
		HpcWebUser webUser = new HpcWebUser();
		model.addAttribute("hpcWebUser", webUser);

		return "bookmark";
	}

	/**
	 * Create Bookmark POST action
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
			if (hpcBookmark.getName() == null || hpcBookmark.getPath() == null) {
				model.addAttribute("message", "Invald user input");
			} else {
				HpcBookmarkRequestDTO dto = new HpcBookmarkRequestDTO();
				dto.setPath(hpcBookmark.getPath().trim());
				boolean created = HpcClientUtil.createBookmark(authToken, bookmarkServiceURL, dto,
						hpcBookmark.getName(), sslCertPath, sslCertPassword);
				if (created) {
					result.setMessage("Bookmark saved!");
					HpcBookmarkListDTO bookmarksDTO = HpcClientUtil.getBookmarks(authToken, bookmarkServiceURL,
						sslCertPath, sslCertPassword);
					List<gov.nih.nci.hpc.domain.databrowse.HpcBookmark> bookmarks = bookmarksDTO.getBookmarks();
					bookmarks.sort(Comparator.comparing(gov.nih.nci.hpc.domain.databrowse.HpcBookmark::getName));
					session.setAttribute("bookmarks", bookmarks);
				}
			}
		} catch (Exception e) {
			result.setMessage(e.getMessage());
			logger.error(e.getMessage(), e);
		} 
		return result;
	}
}
