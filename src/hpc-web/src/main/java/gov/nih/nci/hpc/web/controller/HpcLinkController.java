/**
 * HpcLinkController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.annotation.JsonView;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcLinkDatafile;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Controller to support creating a symbolic link of a data file
 * </p>
 *
 * @author <a href="mailto:Yuri.Dinh@nih.gov">Yuri Dinh</a>
 * @version $Id: HpcLinkController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/link")
public class HpcLinkController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.v2.dataObject}")
	private String v2dataObjectServiceURL;
	@Value("${gov.nih.nci.hpc.server.dataObject}")
    private String dataObjectServiceURL;
	@Value("${gov.nih.nci.hpc.web.server}")
	private String webServerName;

	/**
	 * Get action to prepare link page. This is invoked when:
	 * - User clicks link icon on the search result page
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
		HpcLinkDatafile hpcLinkDatafile = new HpcLinkDatafile();
		model.addAttribute("hpcLinkDatafile", hpcLinkDatafile);

		String source = request.getParameter("source");
		String type = request.getParameter("type");
		String linkFilePath = request.getParameter("linkFilePath");

		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");

		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/login";
		}

		HpcSearch hpcSearch = (HpcSearch)session.getAttribute("hpcSearch");
		if(hpcSearch != null)
			hpcSearch.setSearchType(type);
		else
			hpcSearch = new HpcSearch();
		model.addAttribute("hpcSearch", hpcSearch);
		session.setAttribute("hpcSearch", hpcSearch);
		model.addAttribute("source", source);
		model.addAttribute("linkFilePath", linkFilePath);
		
		return "link";
	}



	/**
	 * POST action to create symbolic link.
	 * 
	 * @param linkFile
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
	public AjaxResponseBody link(@Valid @ModelAttribute("hpcLinkDatafile") HpcLinkDatafile linkFile,
			Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {
		AjaxResponseBody result = new AjaxResponseBody();
		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (authToken == null) {
				result.setMessage("Invalid user session, expired. Please login again.");
				return result;
			}

            HpcDataObjectRegistrationRequestDTO dto = new HpcDataObjectRegistrationRequestDTO();
			dto.setLinkSourcePath(linkFile.getSourcePath());
			Path sourcePath = Paths.get(linkFile.getSourcePath());
			String destFile = linkFile.getDestinationPath() + "/" + sourcePath.getFileName().toString();
			return HpcClientUtil.linkDatafile(authToken, dataObjectServiceURL, v2dataObjectServiceURL, dto, destFile, sslCertPath, sslCertPassword);
		} catch (HttpStatusCodeException e) {
			result.setMessage("Link request is not successful: " + e.getMessage());
			return result;
		} catch (RestClientException e) {
			result.setMessage("Link request is not successful: " + e.getMessage());
			return result;
		} catch (Exception e) {
			result.setMessage("Link request is not successful: " + e.getMessage());
			return result;
		}
	}
}
