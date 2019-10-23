/**
 * HpcDownloadController.java
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
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
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

import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcBrowserEntry;
import gov.nih.nci.hpc.web.model.HpcDatafileModel;
import gov.nih.nci.hpc.web.model.HpcDownloadDatafile;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.MiscUtil;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * <p>
 * Controller to support asynchronous download of a data file or collection
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDownloadController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/download")
public class HpcDownloadController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.dataObject}")
	private String dataObjectServiceURL;
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionServiceURL;
	@Value("${gov.nih.nci.hpc.web.server}")
	private String webServerName;

	/**
	 * Get action to prepare download page. This is invoked when:
	 * - User clicks download icon on the detail view page
	 * - User clicks link to Globus endpoint and path on the download page
	 * - Invoked from Globus site when user presses submit
	 * 
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
		HpcDownloadDatafile hpcDownloadDatafile = new HpcDownloadDatafile();
		model.addAttribute("hpcDownloadDatafile", hpcDownloadDatafile);

		String action = request.getParameter("actionType");
		String endPointName = request.getParameter("endpoint_id");
		String downloadType = request.getParameter("type");
		String downloadFilePath = null;

		if(action == null && endPointName == null) {

			if("collection".equals(downloadType)) {
				model.addAttribute("searchType", "async");
				downloadFilePath = request.getParameter("path");
			} else {
				downloadFilePath = request.getParameter("downloadFilePath");
			}
			session.setAttribute("downloadFilePath", downloadFilePath);
			session.setAttribute("downloadType", downloadType);
		}

		if(downloadFilePath == null || downloadFilePath.isEmpty()) {
			//We could be here from Globus site, so get downloadFilePath from session
			downloadFilePath = (String)session.getAttribute("downloadFilePath");
			downloadType = (String)session.getAttribute("downloadType");
		}
		model.addAttribute("downloadFilePath", downloadFilePath);

		if (downloadFilePath != null) {
			String fileName = downloadFilePath;
			int index = downloadFilePath.lastIndexOf("/");

			if (index == -1)
				index = downloadFilePath.lastIndexOf("//");

			if (index != -1)
				fileName = downloadFilePath.substring(index + 1);
			model.addAttribute("downloadFilePathName", fileName);
		}
		model.addAttribute("downloadType", downloadType);
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");

		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/login";
		}

		if (action != null && action.equals("Globus")) {
			//We are going to Globus site, so save the downloadFilePath
			//to retrieve when we come back
			//session.setAttribute("basePathSelected", basePath);
			model.addAttribute("useraction", "globus");
			session.removeAttribute("GlobusEndpoint");
			session.removeAttribute("GlobusEndpointPath");
			session.removeAttribute("GlobusEndpointFiles");
			session.removeAttribute("GlobusEndpointFolders");

			final String percentEncodedReturnURL = MiscUtil.performUrlEncoding(
					this.webServerName) + "/download?downloadFilePath=" + downloadFilePath;
			return "redirect:https://app.globus.org/file-manager?method=GET&" +
	        "action=" + percentEncodedReturnURL;

		}

		if(endPointName != null) {
			//This is return from Globus site
			model.addAttribute("endPointName", endPointName);
			String endPointLocation = request.getParameter("path");
			//Remove the last trailing slash if the path ends with that
			if(endPointLocation.lastIndexOf('/') == endPointLocation.length() - 1) {
				endPointLocation = endPointLocation.substring(0, endPointLocation.length()-1);
			}
			model.addAttribute("endPointLocation", endPointLocation);
			model.addAttribute("searchType", "async");
		}

		return "download";
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
	@RequestMapping(method = RequestMethod.POST)
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
			final String basisURL = "collection".equals(downloadFile
        .getDownloadType()) ? this.collectionServiceURL :
        this.dataObjectServiceURL;
      final String serviceURL = UriComponentsBuilder.fromHttpUrl(basisURL)
        .path("/{dme-archive-path}/download").buildAndExpand(downloadFile
        .getDestinationPath()).encode().toUri().toURL().toExternalForm();
			HpcDownloadRequestDTO dto = new HpcDownloadRequestDTO();
			if (downloadFile.getSearchType() != null && downloadFile.getSearchType().equals("async")) {
				HpcFileLocation location = new HpcFileLocation();
				location.setFileContainerId(downloadFile.getEndPointName());
				location.setFileId(downloadFile.getEndPointLocation());
				dto.setDestination(location);
			}
            final String downloadTaskType = "collection".equals(downloadFile.
                    getDownloadType()) ? HpcDownloadTaskType.COLLECTION.name() :
                        HpcDownloadTaskType.DATA_OBJECT.name();
			return HpcClientUtil.downloadDataFile(authToken, serviceURL, dto, downloadTaskType, sslCertPath, sslCertPassword);
		} catch (HttpStatusCodeException e) {
			result.setMessage("Download request is not successful: " + e.getMessage());
			return result;
		} catch (RestClientException e) {
			result.setMessage("Download request is not successful: " + e.getMessage());
			return result;
		} catch (Exception e) {
			result.setMessage("Download request is not successful: " + e.getMessage());
			return result;
		}
	}
}
