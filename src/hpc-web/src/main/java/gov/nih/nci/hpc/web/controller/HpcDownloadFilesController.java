/**
 * HpcDatafileController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcDatafileModel;
import gov.nih.nci.hpc.web.model.HpcDownloadDatafile;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcMetadataAttrEntry;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcSearchUtil;

/**
 * <p>
 * Controller to display task details
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/downloadfiles")
public class HpcDownloadFilesController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.download}")
	private String dataObjectsDownloadServiceURL;

	@Value("${gov.nih.nci.hpc.server.collection.download}")
	private String collectionDownloadServiceURL;
	
	@Value("${gov.nih.nci.hpc.server.dataObject.download}")
	private String dataObjectDownloadServiceURL;

	/**
	 * POST operation to display download task details and its metadata
	 * 
	 * @param body
	 * @param taskId
	 * @param type
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String home(@RequestBody(required = false) String body,
			Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
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
			HpcDownloadDatafile hpcDownloadDatafile = new HpcDownloadDatafile();

			String searchType = request.getParameter("searchType");
			String selectedPathsStr = request.getParameter("selectedFilePaths");
			if(selectedPathsStr.isEmpty())
			{
				model.addAttribute("error", "Data file list is missing!");
			}
			else
			{
				StringTokenizer tokens = new StringTokenizer(selectedPathsStr, ",");
				while(tokens.hasMoreTokens())
				{
					String pathStr = tokens.nextToken();
					hpcDownloadDatafile.getSelectedPaths().add(pathStr.substring(pathStr.lastIndexOf(":")+1));
				}
			}
			model.addAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
			HpcSearch hpcSearch = new HpcSearch();
			String pageNumber = request.getParameter("pageNumber");
			hpcSearch.setPageNumber(pageNumber != null ? Integer.parseInt(pageNumber) : 1);
			hpcSearch.setQueryName(request.getParameter("queryName"));
			hpcSearch.setSearchType(request.getParameter("searchType"));
			model.addAttribute("hpcSearch", hpcSearch);
		} catch (Exception e) {
			model.addAttribute("error", "Failed to get selected data file: " + e.getMessage());
			e.printStackTrace();
			return "downloadfiles";
		}
		return "downloadfiles";
	}
	
	/**
	 * POST action to initiate asynchronous download.
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
	@RequestMapping(method = RequestMethod.PUT)
	@ResponseBody
	public AjaxResponseBody download(@Valid @ModelAttribute("hpcDownloadDatafile") HpcDownloadDatafile downloadFile,
			Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {
		AjaxResponseBody result = new AjaxResponseBody();
		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (authToken == null) {
				result.setMessage("Invalid user session, expired. Please login again.");
				return result;
			}

			HpcDataObjectListDownloadRequestDTO dto = new HpcDataObjectListDownloadRequestDTO();
			String selectedPathsStr = request.getParameter("selectedFilePaths");
			if(selectedPathsStr.isEmpty())
			{
				model.addAttribute("error", "Data file list is missing!");
			}
			else
			{
				selectedPathsStr = selectedPathsStr.substring(1, selectedPathsStr.length()-1);
				StringTokenizer tokens = new StringTokenizer(selectedPathsStr, ",");
				while(tokens.hasMoreTokens())
					dto.getDataObjectPaths().add(tokens.nextToken().trim());
			}

			HpcFileLocation location = new HpcFileLocation();
			location.setFileContainerId(downloadFile.getEndPointName());
			location.setFileId(downloadFile.getEndPointLocation());
			dto.setDestination(location);

				try {
					HpcDataObjectListDownloadResponseDTO downloadDTO = (HpcDataObjectListDownloadResponseDTO) HpcClientUtil.downloadFiles(authToken, dataObjectsDownloadServiceURL, dto, sslCertPath, sslCertPassword);
					if(downloadDTO != null)
					result.setMessage("Download request successfull.<br/> Task Id: " + downloadDTO.getTaskId());
					return result;
				} catch (Exception e) {
					result.setMessage("Download request is not successfull: " + e.getMessage());
					return result;
				}
		} catch (HttpStatusCodeException e) {
			result.setMessage("Download request is not successfull: " + e.getMessage());
			return result;
		} catch (RestClientException e) {
			result.setMessage("Download request is not successfull: " + e.getMessage());
			return result;
		} catch (Exception e) {
			result.setMessage("Download request is not successfull: " + e.getMessage());
			return result;
		}
	}	
}
