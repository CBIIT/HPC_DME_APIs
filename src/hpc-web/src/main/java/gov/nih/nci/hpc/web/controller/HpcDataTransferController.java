/**
 * HpcDataTransferController.java
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.gson.Gson;

import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaAccount;
import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcDownloadDatafile;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.service.HpcAuthorizationService;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.MiscUtil;

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
@RequestMapping("/datatransfer")
public class HpcDataTransferController extends AbstractHpcController {
    @Autowired
    private HpcAuthorizationService hpcAuthorizationService;
    
    @Value("${gov.nih.nci.hpc.server.dataObject}")
	private String serviceURL;
    @Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionServiceURL;
	@Value("${gov.nih.nci.hpc.server.v2.dataObject}")
	private String dataObjectDownloadServiceURL;
	@Value("${gov.nih.nci.hpc.server.v2.collection}")
	private String collectionDownloadServiceURL;
	@Value("${gov.nih.nci.hpc.web.server}")
	private String webServerName;

	private Logger logger = LoggerFactory.getLogger(HpcCreateCollectionDataFileController.class);
	private Gson gson = new Gson();

	/**
	 * Get action to prepare download page. This is invoked when:
	 * - User clicks download icon on the detail view page
	 * - User clicks the 'Select Globus Endpoint UUID and Destination' link
	 * - Invoked from Globus site when user presses Submit button after selecting Globus Endpoint and Path.
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

		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String userId = (String) session.getAttribute("hpcUserId");
		String authToken = (String) session.getAttribute(ATTR_USER_TOKEN);
		
		String action = request.getParameter("actionType");
		String endPointName = request.getParameter("endpoint_id");
		String downloadType = request.getParameter("type");
		String source = request.getParameter("source");
		String downloadFilePath = null;
		
        if(action == null && endPointName == null) {
			//User clicked the download icon on the detail view page
			if("collection".equals(downloadType)) {
				model.addAttribute("searchType", "async");
				model.addAttribute("transferType", "globus");
				downloadFilePath = request.getParameter("path");
			} else {
				downloadFilePath = request.getParameter("downloadFilePath");
			}
			session.removeAttribute("downloadType");
			session.removeAttribute("downloadSource");
			session.removeAttribute("downloadFilePath");
		}

		if (downloadFilePath != null) {
			String fileName = downloadFilePath;
			int index = downloadFilePath.lastIndexOf("/");

			if (index == -1)
				index = downloadFilePath.lastIndexOf("//");

			if (index != -1)
				fileName = downloadFilePath.substring(index + 1);
			model.addAttribute("downloadFilePathName", fileName);
		}

		//HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");

		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/login";
		}

		if (action != null && action.equals("Globus")) {
			session.setAttribute("downloadType", downloadType);
			session.setAttribute("downloadSource", source);
			downloadFilePath = request.getParameter("downloadFilePath");
			session.setAttribute("downloadFilePath", downloadFilePath);

			model.addAttribute("useraction", "globus");
			session.removeAttribute("GlobusEndpoint");
			session.removeAttribute("GlobusEndpointPath");
			session.removeAttribute("GlobusEndpointFiles");
			session.removeAttribute("GlobusEndpointFolders");

			final String percentEncodedReturnURL = MiscUtil.performUrlEncoding(
					this.webServerName) + "/download";
			return "redirect:https://app.globus.org/file-manager?method=GET&" +
	        "action=" + percentEncodedReturnURL;
		}
		
		if(endPointName != null) {
			//This is return from Globus site
			model.addAttribute("endPointName", endPointName);
			String endPointLocation = request.getParameter("path");
			model.addAttribute("endPointLocation", endPointLocation);
			model.addAttribute("transferType", "globus");

			downloadFilePath = (String)session.getAttribute("downloadFilePath");
			downloadType = (String)session.getAttribute("downloadType");
			source = (String)session.getAttribute("downloadSource");
		}

		model.addAttribute("downloadFilePath", downloadFilePath);
		model.addAttribute("downloadType", downloadType);

		HpcSearch hpcSearch = (HpcSearch)session.getAttribute("hpcSearch");
		if(hpcSearch != null)
			hpcSearch.setSearchType(downloadType);
		else
			hpcSearch = new HpcSearch();
		model.addAttribute("hpcSearch", hpcSearch);
		session.setAttribute("hpcSearch", hpcSearch);
		model.addAttribute("source", source);
		model.addAttribute("deselectedColumns", hpcSearch.getDeselectedColumns());
		model.addAttribute(ATTR_CAN_DOWNLOAD, Boolean.TRUE.toString());
		if(downloadType.equals("datafile")) {
			HpcDataObjectDTO datafile = HpcClientUtil.getDatafilesWithoutAttributes(authToken, dataObjectDownloadServiceURL, downloadFilePath, false, false, 
					sslCertPath, sslCertPassword);
			if (datafile != null && datafile.getMetadataEntries() != null) {
				for(HpcMetadataEntry entry : datafile.getMetadataEntries().getSelfMetadataEntries().getSystemMetadataEntries()) {
					if(entry.getAttribute().equals("deep_archive_status") && !entry.getValue().equals("IN_PROGRESS")) {
                        model.addAttribute("restoreMsg", "The data may be in deep archive. " +
                        "If so, when you click Download, you will receive a notification after the system has made the data available. Otherwise, the download begins when you click Download.");
                        break;
					}
				}
			}
		} else {
			//Disable the download button if the collection size exceeds the limit
			Long collectionSize = 0L;
			//Get the total size of the collection
			HpcCollectionListDTO collections = HpcClientUtil.getCollection(authToken, collectionServiceURL, downloadFilePath, false,
								false, false, sslCertPath, sslCertPassword);
			//Get the collection size if present
			collectionSize = getCollectionSizeFromReport(collections.getCollections().get(0));
			
			boolean canDownloadFlag = determineIfDataSetCanBeDownloaded(collectionSize);
			model.addAttribute(ATTR_CAN_DOWNLOAD, Boolean.toString(canDownloadFlag));
			if(!canDownloadFlag) {
				String contactEmail = (String) session.getAttribute("contactEmail");
				model.addAttribute(ATTR_MAX_DOWNLOAD_SIZE_EXCEEDED_MSG,
						"Collection size exceeds the maximum permitted download limit of "
								+ MiscUtil.humanReadableByteCount(Double.parseDouble(maxAllowedDownloadSize.toString()), true)
								+ ". Contact <a href='mailto:" + contactEmail + "'>" + contactEmail + "</a> for support.");	
			}
		}
		
		return "datatransfer";
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
	public AjaxResponseBody datatransfer(@Valid @ModelAttribute("hpcDownloadDatafile") HpcDownloadDatafile downloadFile,
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
        .getDownloadType()) ? this.collectionDownloadServiceURL :
        this.dataObjectDownloadServiceURL;
      final String serviceURL = UriComponentsBuilder.fromHttpUrl(basisURL)
        .path("/{dme-archive-path}/download").buildAndExpand(downloadFile
        .getDestinationPath()).encode().toUri().toURL().toExternalForm();
			HpcDownloadRequestDTO dto = new HpcDownloadRequestDTO();
			HpcAsperaDownloadDestination destination = new HpcAsperaDownloadDestination();
			HpcFileLocation location = new HpcFileLocation();
			location.setFileContainerId(downloadFile.getAsperaBucketName());
			location.setFileId(downloadFile.getAsperaPath().trim());
			destination.setDestinationLocation(location);
			HpcAsperaAccount account = new HpcAsperaAccount();
			account.setUser(downloadFile.getAsperaUser());
			account.setPassword(downloadFile.getAsperaPassword());
			account.setHost(downloadFile.getAsperaHost());
			destination.setAccount(account);
			dto.setAsperaDownloadDestination(destination);
			logger.info("JSON for Aspera Download: " + gson.toJson(dto));
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