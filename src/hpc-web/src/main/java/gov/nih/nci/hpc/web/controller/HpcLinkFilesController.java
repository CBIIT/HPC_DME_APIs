/**
 * HpcLinkFilesController.java
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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.apache.commons.lang.StringUtils;
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
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcLinkDatafile;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcSearchUtil;

/**
 * <p>
 * Controller to create symbolic links for a list of data files
 * </p>
 *
 * @author <a href="mailto:Yuri.Dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/linkfiles")
public class HpcLinkFilesController extends AbstractHpcController {
    @Value("${gov.nih.nci.hpc.server.v2.bulkregistration}")
    private String v2bulkRegistrationURL;
	@Value("${gov.nih.nci.hpc.web.server}")
	private String webServerName;

	/**
	 * POST operation to display a page for creating a symbolic link for a selected list of files
	 * Invoked from the search results page when user clicks the 
	 * 'Link Selected Collections' link.
	 * @param body
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String home(@RequestBody(required = false) String body, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		try {
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			String userId = (String) session.getAttribute("hpcUserId");
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (user == null || authToken == null) {
				ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
				bindingResult.addError(error);
				HpcLogin hpcLogin = new HpcLogin();
				model.addAttribute("hpcLogin", hpcLogin);
				return "dashboard";
			}
			HpcSearchUtil.cacheSelectedRows(session, request, model);
			HpcLinkDatafile hpcLinkDatafile = new HpcLinkDatafile();

			String selectedPathsStr = request.getParameter("selectedFilePaths");

			if (selectedPathsStr.isEmpty()) {
				model.addAttribute("error", "Data file list is missing!");
			} else {
				StringTokenizer tokens = new StringTokenizer(selectedPathsStr, ",");
				while (tokens.hasMoreTokens()) {
					String pathStr = tokens.nextToken();
					hpcLinkDatafile.getSelectedPaths().add(pathStr.substring(pathStr.lastIndexOf(":") + 1));
				}
			}

			model.addAttribute("hpcLinkDatafile", hpcLinkDatafile);
			session.setAttribute("hpcLinkDatafile", hpcLinkDatafile);

			HpcSearch hpcSaveSearch = new HpcSearch();
			String pageNumber = request.getParameter("pageNumber");
			hpcSaveSearch.setPageNumber(pageNumber != null ? Integer.parseInt(pageNumber) : 1);
			String pageSize = request.getParameter("pageSize");
			if (StringUtils.isNotBlank(pageSize))
				hpcSaveSearch.setPageSize(Integer.parseInt(pageSize));
			hpcSaveSearch.setQueryName(request.getParameter("queryName"));
			hpcSaveSearch.setSearchType(request.getParameter("searchType"));
			model.addAttribute("hpcSearch", hpcSaveSearch);
		} catch (Exception e) {
			model.addAttribute("error", "Failed to get selected data file: " + e.getMessage());
			e.printStackTrace();
			return "linkfiles";
		}
		return "linkfiles";
	}


	/**
	 * POST action to create symbolic link on selected list of files.
	 * 
	 * @param downloadFile
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @return
	 */
	@JsonView(Views.Public.class)
	@RequestMapping(value = "/link", method = RequestMethod.POST)
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

	        String selectedPathsStr = request.getParameter("selectedFilePaths");
	        if (selectedPathsStr.isEmpty()) {
	            model.addAttribute("error", "Data file list is missing!");
	            return result;
	        }
			HpcBulkDataObjectRegistrationRequestDTO registrationDTO = constructBulkRequest(request, session,
			    selectedPathsStr, linkFile.getDestinationPath());
			
			try {
				HpcBulkDataObjectRegistrationResponseDTO downloadDTO = null;
				downloadDTO = (HpcBulkDataObjectRegistrationResponseDTO) HpcClientUtil
					.linkDatafiles(authToken, v2bulkRegistrationURL, registrationDTO, sslCertPath, sslCertPassword);
				if (downloadDTO != null) {
					result.setMessage("Link creation request submitted.");
				}
				return result;
			} catch (Exception e) {
				result.setMessage("Link creation was not successful: " + e.getMessage());
				return result;
			}
		} catch (HttpStatusCodeException e) {
			result.setMessage("Link creation was not successful: " + e.getMessage());
			return result;
		} catch (RestClientException e) {
			result.setMessage("Link creation was not successful: " + e.getMessage());
			return result;
		} catch (Exception e) {
			result.setMessage("Link creation was not successful: " + e.getMessage());
			return result;
		}
	}
	
	protected HpcBulkDataObjectRegistrationRequestDTO constructBulkRequest(HttpServletRequest request,
        HttpSession session, String selectedPathsStr, String destinationPath) {
        HpcBulkDataObjectRegistrationRequestDTO dto = new HpcBulkDataObjectRegistrationRequestDTO();
        
        selectedPathsStr = selectedPathsStr.substring(1, selectedPathsStr.length() - 1);
        StringTokenizer tokens = new StringTokenizer(selectedPathsStr, ",");
        
        List<HpcDataObjectRegistrationItemDTO> files = new ArrayList<HpcDataObjectRegistrationItemDTO>();
        while (tokens.hasMoreTokens()) {
            HpcDataObjectRegistrationItemDTO file = new HpcDataObjectRegistrationItemDTO();
            String sourceFile = tokens.nextToken().trim();
            file.setLinkSourcePath(sourceFile);
            file.setCreateParentCollections(false);
            Path sourcePath = Paths.get(sourceFile);
            String destFile = destinationPath + "/" + sourcePath.getFileName().toString();
            file.setPath(destFile);
            files.add(file);
        }
        dto.getDataObjectRegistrationItems().addAll(files);
        
        return dto;
    }

}
