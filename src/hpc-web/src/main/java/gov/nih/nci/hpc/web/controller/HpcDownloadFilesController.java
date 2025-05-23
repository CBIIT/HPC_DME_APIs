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

import java.util.Collection;
import java.util.StringTokenizer;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
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

import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaAccount;
import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcGoogleDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcBoxDownloadDestination;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcDownloadDatafile;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.service.HpcAuthorizationService;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcIdentityUtil;
import gov.nih.nci.hpc.web.util.HpcSearchUtil;
import gov.nih.nci.hpc.web.util.MiscUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

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
  
    @Autowired
    private HpcAuthorizationService hpcAuthorizationService;
  
	@Value("${gov.nih.nci.hpc.server.v2.download}")
	private String downloadServiceURL;
	
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionServiceURL;
	
	@Value("${gov.nih.nci.hpc.web.server}")
	private String webServerName;

	@Value("${gov.nih.nci.hpc.asperaBucket}")
	private String asperaBucket;

	private Logger logger = LoggerFactory.getLogger(HpcCreateCollectionDataFileController.class);
	private Gson gson = new Gson();

	/**
	 * POST operation to display download task details and its metadata
	 * Invoked from the search results page when user clicks the 
	 * 'Download Selected Collections' or 'Download Selected Files' link.
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
			HpcDownloadDatafile hpcDownloadDatafile = new HpcDownloadDatafile();

			String downloadType = request.getParameter("downloadType");
			hpcDownloadDatafile.setDownloadType(downloadType);
			String selectedPathsStr = request.getParameter("selectedFilePaths");
			String globalMetadataSearchText = request.getParameter("globalMetadataSearchText");
			hpcDownloadDatafile.setGlobalMetadataSearchText(globalMetadataSearchText);
			model.addAttribute("globalMetadataSearchText", globalMetadataSearchText);

			// Determine if System Admin or Group Admin
			boolean isAdmin =  HpcIdentityUtil.iUserSystemAdminOrGroupAdmin(session);
			model.addAttribute("isAdmin", isAdmin);

			// Setting default values for Aspera variables
			model.addAttribute("asperaHost", "gap-submit.ncbi.nlm.nih.gov");
			model.addAttribute("asperaUser", "asp-dbgap");
			model.addAttribute("asperaPath", "/");
			if(asperaBucket == null || asperaBucket.contains("<") || asperaBucket.contains(">")) {
				model.addAttribute("asperaBucketName", "test");
			} else {
				model.addAttribute("asperaBucketName", asperaBucket);
			}
			if (selectedPathsStr.isEmpty()) {
				model.addAttribute("error", "Data file list is missing!");
			} else {
				StringTokenizer tokens = new StringTokenizer(selectedPathsStr, ",");
				while (tokens.hasMoreTokens()) {
					String pathStr = tokens.nextToken();
					hpcDownloadDatafile.getSelectedPaths().add(pathStr.substring(pathStr.lastIndexOf(":") + 1));
				}
			}
			model.addAttribute("downloadType", downloadType);
			//Set this to globus for initial radio button auto selection
			model.addAttribute("transferType", "globus");

			model.addAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
			session.setAttribute("hpcDownloadDatafile", hpcDownloadDatafile);

			HpcSearch hpcSaveSearch = new HpcSearch();
			String pageNumber = request.getParameter("pageNumber");
			hpcSaveSearch.setPageNumber(pageNumber != null ? Integer.parseInt(pageNumber) : 1);
			String pageSize = request.getParameter("pageSize");
			if (StringUtils.isNotBlank(pageSize))
				hpcSaveSearch.setPageSize(Integer.parseInt(pageSize));
			hpcSaveSearch.setQueryName(request.getParameter("queryName"));
			hpcSaveSearch.setSearchType(request.getParameter("searchType"));
			String[] deselectedColumns =  request.getParameterValues("deselectedColumns");
			if(deselectedColumns != null && StringUtils.isNotEmpty(deselectedColumns[0]))
				hpcSaveSearch.getDeselectedColumns().addAll((Collection<? extends String>) CollectionUtils.arrayToList(deselectedColumns[0].split(",")));
			hpcSaveSearch.setTotalSize(StringUtils.isNotBlank(request.getParameter("totalSize")) ? Long.parseLong(request.getParameter("totalSize")) : 0);

			model.addAttribute(ATTR_CAN_DOWNLOAD, Boolean.TRUE.toString());
			//Disable the download button if the collection size exceeds the limit
			if(downloadType.equals("collections")) {
				Long datasetSize = hpcSaveSearch.getTotalSize();
				if(datasetSize == 0) {
					//Calculate the total of all path
					for(String downloadFilePath: hpcDownloadDatafile.getSelectedPaths()) {
						//Get the total size of the collection
						HpcCollectionListDTO collections = HpcClientUtil.getCollection(authToken, collectionServiceURL, downloadFilePath, false,
											false, false, sslCertPath, sslCertPassword);
						//Get the collection size if present
						datasetSize += getCollectionSizeFromReport(collections.getCollections().get(0));
					}
				}
				boolean canDownloadFlag = determineIfDataSetCanBeDownloaded(datasetSize);
				model.addAttribute(ATTR_CAN_DOWNLOAD, Boolean.toString(canDownloadFlag));
				if(!canDownloadFlag) {
					model.addAttribute(ATTR_MAX_DOWNLOAD_SIZE_EXCEEDED_MSG,
							"Collection list size exceeds the maximum permitted download limit of "
									+ MiscUtil.humanReadableByteCount(Double.parseDouble(maxAllowedDownloadSize.toString()), true)
									+ ". Contact <a href='mailto:" + contactEmail + "'>" + contactEmail + "</a> for support.");				}
			}

			model.addAttribute("hpcSearch", hpcSaveSearch);
			session.setAttribute("hpcSavedSearch", hpcSaveSearch);
		} catch (Exception e) {
			model.addAttribute("error", "Failed to get selected data file: " + e.getMessage());
			logger.error("Failed to get selected data file: ", e);
			return "downloadfiles";
		}
		return "downloadfiles";
	}


	/**
	 * Links to the Globus site to obtain the endpoint UUID and path. Or links to the Google Drive authorization.
	 * Invoked from the download page when 
	 * - user clicks the 'Select Globus Endpoint UUID and Destination Path' link
	 * - user clicks the Submit button on the Globus page after selecting the Globus endpoint and path
	 * - user clicks the 'Authorize DME to access your Google Drive' button
     * - user clicks the Allow button on the Google page granting NCI-HPC-DME permission
	 * @param body
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String getExternalPath(@RequestBody(required = false) String body, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {

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

		String endPointName = request.getParameter("endpoint_id");
		// Determine if System Admin or Group Admin
		boolean isAdmin =  HpcIdentityUtil.iUserSystemAdminOrGroupAdmin(session);
		model.addAttribute("isAdmin", isAdmin);
		String code = request.getParameter("code");
		String transferType = request.getParameter("transferType");
		String downloadType = request.getParameter("downloadType");
		String authorizedActionFrom =(String)session.getAttribute("authorizedActionFrom");
		if (code != null) {
			//Return from Google Drive Authorization
			final String returnURL = this.webServerName + "/downloadfiles";
			try {
				if(authorizedActionFrom.equals(HpcAuthorizationService.GOOGLE_DRIVE_TYPE)){
					String accessToken = hpcAuthorizationService.getToken(code, returnURL, HpcAuthorizationService.ResourceType.GOOGLEDRIVE);
					session.setAttribute("accessToken", accessToken);
					model.addAttribute("accessToken", accessToken);
					model.addAttribute("searchType", HpcAuthorizationService.GOOGLE_DRIVE_TYPE);
					model.addAttribute("transferType", HpcAuthorizationService.GOOGLE_DRIVE_TYPE);
					model.addAttribute("authorized", "true");
				} else if(authorizedActionFrom.equals(HpcAuthorizationService.GOOGLE_CLOUD_TYPE)){
					String refreshTokenDetailsGoogleCloud = hpcAuthorizationService.getRefreshToken(code, returnURL, HpcAuthorizationService.ResourceType.GOOGLECLOUD, userId);
					session.setAttribute("refreshTokenDetailsGoogleCloud", refreshTokenDetailsGoogleCloud);
					model.addAttribute("refreshTokenDetailsGoogleCloud", refreshTokenDetailsGoogleCloud);
					model.addAttribute("searchType", HpcAuthorizationService.GOOGLE_CLOUD_TYPE);
					model.addAttribute("transferType", HpcAuthorizationService.GOOGLE_CLOUD_TYPE);
					model.addAttribute("authorizedGC", "true");
				} else if(authorizedActionFrom.equals(HpcAuthorizationService.BOX_TYPE)){
					logger.info("HpcDownloadFilesController: Box code received from Box=" + code);
					List<String> tokens = hpcAuthorizationService.getBoxToken(code);
					String accessToken = tokens.get(0);
					String refreshToken = tokens.get(1);
					session.setAttribute("accessBoxToken", tokens.get(0));
					model.addAttribute("accessBoxToken", tokens.get(0));
					session.setAttribute("refreshBoxToken", tokens.get(1));
					model.addAttribute("refreshBoxToken", tokens.get(1));
					model.addAttribute("searchType", HpcAuthorizationService.BOX_TYPE);
					model.addAttribute("transferType", HpcAuthorizationService.BOX_TYPE);
					model.addAttribute("authorizedBox", "true");
				}
			} catch (Exception e) {
				model.addAttribute("error", "Failed to redirect to destination endpoint for authorization: " + e.getMessage());
				logger.error("Failed to redirect to destination endpoint for authorization ", e);
			}
			HpcDownloadDatafile hpcDownloadDatafile = (HpcDownloadDatafile)session.getAttribute("hpcDownloadDatafile");
			String globalMetadataSearchText = request.getParameter("globalMetadataSearchText");
			hpcDownloadDatafile.setGlobalMetadataSearchText(globalMetadataSearchText);
			model.addAttribute("globalMetadataSearchText", globalMetadataSearchText);
			model.addAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
			HpcSearch hpcSaveSearch = (HpcSearch)session.getAttribute("hpcSavedSearch");
			model.addAttribute("hpcSearch", hpcSaveSearch);
			model.addAttribute(ATTR_CAN_DOWNLOAD, Boolean.TRUE.toString());
			if(hpcDownloadDatafile.getDownloadType().equals("collections")) {
				// Setting the flag that will determine if the download can be performed.
				// If the total list size is greater than the maximum allowable size for download,
				// this flag is will cause the download button to be disabled for Google Drive/Cloud in the UI
				boolean canDownloadFlag = determineIfDataSetCanBeDownloaded(hpcSaveSearch.getTotalSize());
				model.addAttribute(ATTR_CAN_DOWNLOAD, Boolean.toString(canDownloadFlag));
				if(!canDownloadFlag) {
					model.addAttribute(ATTR_MAX_DOWNLOAD_SIZE_EXCEEDED_MSG,
							"Collection list size exceeds the maximum permitted download limit of "
									+ MiscUtil.humanReadableByteCount(Double.parseDouble(maxAllowedDownloadSize.toString()), true)
									+ ". Contact <a href='mailto:" + contactEmail + "'>" + contactEmail + "</a> for support.");
				}
			}
			return "downloadfiles";
		} else if (transferType != null && transferType.equals(HpcAuthorizationService.GOOGLE_DRIVE_TYPE)) {
			session.setAttribute("downloadType", downloadType);
			session.setAttribute("authorizedActionFrom", HpcAuthorizationService.GOOGLE_DRIVE_TYPE);
			String returnURL = this.webServerName + "/downloadfiles";
			try {
				return "redirect:" + hpcAuthorizationService.authorize(returnURL, HpcAuthorizationService.ResourceType.GOOGLEDRIVE, userId);
			} catch (Exception e) {
				model.addAttribute("error", "Failed to redirect to Google Drive for authorization: " + e.getMessage());
				logger.error("Failed to redirect to Google Drive for authorization: ", e);
			}
		} else if (transferType != null && transferType.equals(HpcAuthorizationService.GOOGLE_CLOUD_TYPE)) {
			session.setAttribute("downloadType", downloadType);
			session.setAttribute("authorizedActionFrom", HpcAuthorizationService.GOOGLE_CLOUD_TYPE);
			String returnURL = this.webServerName + "/downloadfiles";
			try {
				return "redirect:" + hpcAuthorizationService.authorize(returnURL, HpcAuthorizationService.ResourceType.GOOGLECLOUD, userId);
			} catch (Exception e) {
				model.addAttribute("error", "Failed to redirect to Google Cloud for authorization: " + e.getMessage());
				logger.error("Failed to redirect to Google Cloud for authorization: ", e);
			}
		} else if (transferType != null && transferType.equals(HpcAuthorizationService.BOX_TYPE)) {
			session.setAttribute("downloadType", downloadType);
			session.setAttribute("authorizedActionFrom", HpcAuthorizationService.BOX_TYPE);
			String returnURL = this.webServerName + "/downloadfiles";
			try {
				logger.info("HpcDownloadFilesController: returnURL before Auth"+returnURL);
				String redirectUrl = "redirect:" + hpcAuthorizationService.authorizeBox(returnURL);
				logger.info("HpcDownloadFilesController: Box redirectUrl"+redirectUrl);
				return redirectUrl;
			} catch (Exception e) {
				model.addAttribute("error", "Failed to redirect to Box for authorization: " + e.getMessage());
				logger.error("Failed to redirect to Box for authorization: ", e);
			}
		}
		else if(endPointName == null) {
			//We are going to Globus site, so save the downloadType to use on return
			//for determining whether to display the hierarchy options dropdown
			session.setAttribute("downloadType", downloadType);
			final String percentEncodedReturnURL = MiscUtil.performUrlEncoding(
					this.webServerName) + "/downloadfiles";
			return "redirect:https://app.globus.org/file-manager?method=GET&" +
			"action=" + percentEncodedReturnURL;
		}

		//This is return from Globus site
		downloadType = (String)session.getAttribute("downloadType");
		model.addAttribute("downloadType", downloadType);

		model.addAttribute("endPointName", endPointName);
		String endPointLocation = request.getParameter("path");
		//Remove the last trailing slash if the path ends with that
		if(endPointLocation.lastIndexOf('/') == endPointLocation.length() - 1 && endPointLocation.lastIndexOf('/') != 0) {
			endPointLocation = endPointLocation.substring(0, endPointLocation.length()-1);
		}
		model.addAttribute("endPointLocation", endPointLocation);

		HpcSearch hpcSaveSearch = (HpcSearch)session.getAttribute("hpcSavedSearch");
		model.addAttribute("hpcSearch", hpcSaveSearch);

		HpcDownloadDatafile hpcDownloadDatafile = (HpcDownloadDatafile)session.getAttribute("hpcDownloadDatafile");
		model.addAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
		model.addAttribute("transferType", "globus");
		model.addAttribute(ATTR_CAN_DOWNLOAD, Boolean.TRUE.toString());

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
	@RequestMapping(value = "/download", method = RequestMethod.POST)
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

			HpcBulkDataObjectDownloadRequestDTO dto = new HpcBulkDataObjectDownloadRequestDTO();
			String selectedPathsStr = request.getParameter("selectedFilePaths");
			String downloadType = request.getParameter("downloadType");
			if (selectedPathsStr.isEmpty()) {
				model.addAttribute("error", "Data file list is missing!");
			} else {
				selectedPathsStr = selectedPathsStr.substring(1, selectedPathsStr.length() - 1);
				StringTokenizer tokens = new StringTokenizer(selectedPathsStr, ",");
				while (tokens.hasMoreTokens()) {
					if(downloadType.equals("datafiles"))
						dto.getDataObjectPaths().add(tokens.nextToken().trim());
					else
						dto.getCollectionPaths().add(tokens.nextToken().trim());
				}
			}

			
			if (downloadFile.getSearchType() != null && downloadFile.getSearchType().equals("async")) {
				if(isPublicEndpoint(downloadFile.getEndPointName()) != null) {
					result.setMessage(isPublicEndpoint(downloadFile.getEndPointName()));
					return result;
				}
				HpcFileLocation location = new HpcFileLocation();
				location.setFileContainerId(downloadFile.getEndPointName());
				location.setFileId(downloadFile.getEndPointLocation().trim());
				HpcGlobusDownloadDestination globusDownloadDestination = new HpcGlobusDownloadDestination();
				globusDownloadDestination.setDestinationLocation(location);
				dto.setGlobusDownloadDestination(globusDownloadDestination);
			}  else if (downloadFile.getSearchType() != null && downloadFile.getSearchType().equals("s3")) {
				HpcFileLocation location = new HpcFileLocation();
				location.setFileContainerId(downloadFile.getBucketName());
				location.setFileId(downloadFile.getS3Path().trim());
				HpcS3DownloadDestination destination = new HpcS3DownloadDestination();
				destination.setDestinationLocation(location);
				HpcS3Account account = new HpcS3Account();
				account.setAccessKey(downloadFile.getAccessKey());
				account.setSecretKey(downloadFile.getSecretKey());
				account.setRegion(downloadFile.getRegion());
				destination.setAccount(account);
				dto.setS3DownloadDestination(destination);
			} else if (downloadFile.getSearchType() != null && downloadFile.getSearchType().equals("aspera")) {
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
			} else if (downloadFile.getSearchType() != null && downloadFile.getSearchType().equals(HpcAuthorizationService.GOOGLE_DRIVE_TYPE)) {
                String accessToken = (String)session.getAttribute("accessToken");
                HpcGoogleDownloadDestination destination = new HpcGoogleDownloadDestination();
                HpcFileLocation location = new HpcFileLocation();
                location.setFileContainerId("MyDrive");
                location.setFileId(downloadFile.getDrivePath().trim());
                destination.setDestinationLocation(location);
                destination.setAccessToken(accessToken);
                dto.setGoogleDriveDownloadDestination(destination);
            } else if (downloadFile.getSearchType() != null && downloadFile.getSearchType().equals(HpcAuthorizationService.GOOGLE_CLOUD_TYPE)) {
                String refreshTokenDetailsGoogleCloud = (String)session.getAttribute("refreshTokenDetailsGoogleCloud");
                HpcGoogleDownloadDestination googleCloudDestination = new HpcGoogleDownloadDestination();
                HpcFileLocation location = new HpcFileLocation();
                location.setFileContainerId(downloadFile.getGoogleCloudBucketName());
				location.setFileId(downloadFile.getGoogleCloudPath().trim());
				googleCloudDestination.setDestinationLocation(location);
				googleCloudDestination.setAccessToken(refreshTokenDetailsGoogleCloud);
				dto.setGoogleCloudStorageDownloadDestination(googleCloudDestination);
				logger.info("GoogleCloud file download json: " + gson.toJson(dto));
            } else if (downloadFile.getSearchType() != null && downloadFile.getSearchType().equals(HpcAuthorizationService.BOX_TYPE)) {
				String accessBoxToken = (String)session.getAttribute("accessBoxToken");
				String refreshBoxToken = (String)session.getAttribute("refreshBoxToken");
				HpcBoxDownloadDestination boxDestination = new HpcBoxDownloadDestination();
				HpcFileLocation location = new HpcFileLocation();
				location.setFileContainerId(downloadFile.getBoxContainerName());
				location.setFileId(downloadFile.getBoxPath().trim());
				boxDestination.setDestinationLocation(location);
				boxDestination.setAccessToken(accessBoxToken);
				boxDestination.setRefreshToken(refreshBoxToken);
				dto.setBoxDownloadDestination(boxDestination);
				logger.info("HpcDownloadFilesController: boxDestination download json: " + gson.toJson(dto));
            }

			if("collection".equals(downloadFile.getDownloadType())) {
				if (downloadFile.getDownloadDestinationType() != null
						&& downloadFile.getDownloadDestinationType().equals("downloadToDestination")) {
					logger.debug("DownloadDestinationType: downloadToDestination");
					dto.setAppendPathToDownloadDestination(false);
					dto.setAppendCollectionNameToDownloadDestination(false);
				} else if (downloadFile.getDownloadDestinationType() != null
						&& downloadFile.getDownloadDestinationType().equals("createCollectionFolder")) {
					logger.debug(" DownloadDestinationType: createCollectionFolder");
					dto.setAppendPathToDownloadDestination(false);
					dto.setAppendCollectionNameToDownloadDestination(true);
				} else if (downloadFile.getDownloadDestinationType() != null
						&& downloadFile.getDownloadDestinationType().equals("createFullPath")) {
					logger.debug("DownloadDestinationType: createFullPath");
					dto.setAppendPathToDownloadDestination(true);
					dto.setAppendCollectionNameToDownloadDestination(false);
				} else {
					dto.setAppendPathToDownloadDestination(false);
					dto.setAppendCollectionNameToDownloadDestination(false);
				}
			}
			
			try {
				HpcBulkDataObjectDownloadResponseDTO downloadDTO = null;
				downloadDTO = (HpcBulkDataObjectDownloadResponseDTO) HpcClientUtil
					.downloadFiles(authToken, downloadServiceURL, dto, sslCertPath, sslCertPassword);
				if (downloadDTO != null) {
					String taskType = downloadType.equals("datafiles") ? HpcDownloadTaskType.DATA_OBJECT_LIST.name(): HpcDownloadTaskType.COLLECTION_LIST.name();
					result.setCode("success");
					result.setMessage("Download request successful. Task Id: <a href='downloadtask?type="+ taskType +"&taskId=" + downloadDTO.getTaskId()+"'>"+downloadDTO.getTaskId()+"</a>");
				}
				return result;
			} catch (Exception e) {
				result.setMessage("Download request is not successful: " + e.getMessage());
				return result;
			}
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
