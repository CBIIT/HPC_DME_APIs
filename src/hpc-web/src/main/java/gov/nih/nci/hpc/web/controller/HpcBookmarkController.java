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
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.annotation.JsonView;

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
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (user == null || authToken == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}
		String path = request.getParameter("path");
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
			if (hpcBookmark.getName() == null || hpcBookmark.getPath() == null) 
			{
				model.addAttribute("message", "Invald user input");
			}
			else
			{
				HpcBookmarkRequestDTO dto = new HpcBookmarkRequestDTO();
				dto.setPath(hpcBookmark.getPath().trim());
				boolean created = HpcClientUtil.createBookmark(authToken, bookmarkServiceURL, dto, hpcBookmark.getName(),
					sslCertPath, sslCertPassword);
				if (created)
					result.setMessage("Bookmark saved!");
			}
		} catch (Exception e) {
			result.setMessage(e.getMessage());
		} finally {
			model.addAttribute("hpcBookmark", hpcBookmark);
		}
		return result;
	}
}
