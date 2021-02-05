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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
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
import com.fasterxml.jackson.annotation.JsonView;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcGoogleDriveDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
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
    @Autowired
    private HpcAuthorizationService hpcAuthorizationService;
    
    @Value("${gov.nih.nci.hpc.server.dataObject}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.v2.dataObject}")
	private String dataObjectServiceURL;
	@Value("${gov.nih.nci.hpc.server.v2.collection}")
	private String collectionServiceURL;
	@Value("${gov.nih.nci.hpc.web.server}")
	private String webServerName;

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

		String action = request.getParameter("actionType");
		String endPointName = request.getParameter("endpoint_id");
		String downloadType = request.getParameter("type");
		String source = request.getParameter("source");
		String downloadFilePath = null;
		
		String code = request.getParameter("code");
        if (code != null) {
            //Return from Google Drive Authorization
            downloadFilePath = (String)session.getAttribute("downloadFilePath");
            downloadType = (String)session.getAttribute("downloadType");
            source = (String)session.getAttribute("downloadSource");
            final String returnURL = this.webServerName + "/download";
            try {
              String accessToken = hpcAuthorizationService.getToken(code, returnURL);
              session.setAttribute("accessToken", accessToken);
              model.addAttribute("accessToken", accessToken);
            } catch (Exception e) {
              model.addAttribute("error", "Failed to redirect to Google for authorization: " + e.getMessage());
              e.printStackTrace();
            }
            model.addAttribute("searchType", "drive");
            model.addAttribute("transferType", "drive");
            model.addAttribute("authorized", "true");
            
        }
        else if(action == null && endPointName == null) {
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

		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");

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
		
		if (action != null && action.equals("Drive")) {
  		  
  		    session.setAttribute("downloadType", downloadType);
            session.setAttribute("downloadSource", source);
            downloadFilePath = request.getParameter("downloadFilePath");
            session.setAttribute("downloadFilePath", downloadFilePath);
          
  	        downloadFilePath = request.getParameter("downloadFilePath");
  	        String returnURL = this.webServerName + "/download";
  	        try {
              return "redirect:" + hpcAuthorizationService.authorize(returnURL);
            } catch (Exception e) {
              model.addAttribute("error", "Failed to redirect to Google for authorization: " + e.getMessage());
              e.printStackTrace();
            }
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
		if(downloadType.equals("datafile")) {
			String authToken = (String) session.getAttribute(ATTR_USER_TOKEN);
			HpcDataObjectListDTO datafiles = HpcClientUtil.getDatafiles(authToken, serviceURL, downloadFilePath, false, false, 
					sslCertPath, sslCertPassword);
			if (datafiles != null && datafiles.getDataObjects() != null && !datafiles.getDataObjects().isEmpty()) {
				HpcDataObjectDTO dataFile = datafiles.getDataObjects().get(0);
				for(HpcMetadataEntry entry : dataFile.getMetadataEntries().getSelfMetadataEntries()) {
					if(entry.getAttribute().equals("deep_archive_status") && !entry.getValue().equals("IN_PROGRESS")) {
						model.addAttribute("restoreMsg", "This object is in deep archive. " +
								"Download will take additional time to restore from deep archive.");
						break;
					}
				}
				
			}
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
				HpcGlobusDownloadDestination destination = new HpcGlobusDownloadDestination();
				HpcFileLocation location = new HpcFileLocation();
				location.setFileContainerId(downloadFile.getEndPointName());
				location.setFileId(downloadFile.getEndPointLocation().trim());
				destination.setDestinationLocation(location);
				dto.setGlobusDownloadDestination(destination);
			} else if (downloadFile.getSearchType() != null && downloadFile.getSearchType().equals("s3")) {
				HpcS3DownloadDestination destination = new HpcS3DownloadDestination();
				HpcFileLocation location = new HpcFileLocation();
				location.setFileContainerId(downloadFile.getBucketName());
				location.setFileId(downloadFile.getS3Path().trim());
				destination.setDestinationLocation(location);
				HpcS3Account account = new HpcS3Account();
				account.setAccessKey(downloadFile.getAccessKey());
				account.setSecretKey(downloadFile.getSecretKey());
				account.setRegion(downloadFile.getRegion());
				destination.setAccount(account);
				dto.setS3DownloadDestination(destination);
			} else if (downloadFile.getSearchType() != null && downloadFile.getSearchType().equals("drive")) {
  			    String accessToken = (String)session.getAttribute("accessToken");
  			    HpcGoogleDriveDownloadDestination destination = new HpcGoogleDriveDownloadDestination();
                HpcFileLocation location = new HpcFileLocation();
                location.setFileContainerId("MyDrive");
                location.setFileId(downloadFile.getDrivePath().trim());
                destination.setDestinationLocation(location);
                destination.setAccessToken(accessToken);
                dto.setGoogleDriveDownloadDestination(destination);
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
