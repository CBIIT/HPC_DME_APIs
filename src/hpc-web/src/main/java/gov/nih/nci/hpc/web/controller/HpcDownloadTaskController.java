/**
 * HpcDatafileController.java
 *
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for
 * details.
 */
package gov.nih.nci.hpc.web.controller;

import gov.nih.nci.hpc.web.util.MiscUtil;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.annotation.JsonView;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

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
@RequestMapping("/downloadtask")
public class HpcDownloadTaskController extends AbstractHpcController {
  @Value("${gov.nih.nci.hpc.server.download}")
  private String dataObjectsDownloadServiceURL;

  @Value("${gov.nih.nci.hpc.server.collection.download}")
  private String collectionDownloadServiceURL;

  @Value("${gov.nih.nci.hpc.server.dataObject.download}")
  private String dataObjectDownloadServiceURL;

  @Value("${gov.nih.nci.hpc.server.v2.dataObject}")
  private String dataObjectServiceURL;

  @Value("${gov.nih.nci.hpc.server.download}")
  private String downloadServiceURL;
  
  @Value("${gov.nih.nci.hpc.server.v2.download}")
  private String downloadServiceURL2;

  /**
   * Get operation to display download task details and its metadata
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
  @RequestMapping(method = RequestMethod.GET)
  public String home(@RequestBody(required = false) String body, @RequestParam String taskId,
      @RequestParam String type, Model model, BindingResult bindingResult, HttpSession session,
      HttpServletRequest request) {
    try {
      model.addAttribute("taskId", taskId);
      model.addAttribute("taskType", type);
      HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
      String userId = (String) session.getAttribute("hpcUserId");
      String authToken = (String) session.getAttribute("hpcUserToken");
      if (user == null || authToken == null) {
        ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
        bindingResult.addError(error);
        HpcLogin hpcLogin = new HpcLogin();
        model.addAttribute("hpcLogin", hpcLogin);
        final Map<String, String> qParams = new HashMap<>();
        qParams.put("returnPath", "downloadtask");
        qParams.put("taskId", taskId);
        qParams.put("type", type);
        return "redirect:/login?".concat(MiscUtil.generateEncodedQueryString(
          qParams));
      }

      if (taskId == null || type == null)
        return "redirect:/downloadtasks";

      if (type.equals(HpcDownloadTaskType.COLLECTION.name()))
        return displayCollectionTask(authToken, taskId, model);
      else if (type.equals(HpcDownloadTaskType.DATA_OBJECT.name()))
        return displayDataObjectTask(authToken, taskId, model);
      else if (type.equals(HpcDownloadTaskType.DATA_OBJECT_LIST.name()))
        return diplayDataObjectListTask(authToken, taskId, model);
      else if (type.equals(HpcDownloadTaskType.COLLECTION_LIST.name()))
          return displayCollectionListTask(authToken, taskId, model);
      else {
        String message = "Data file not found!";
        model.addAttribute("error", message);
        return "redirect:/downloadtasks";
      }
    } catch (Exception e) {
      model.addAttribute("error", "Failed to get data file: " + e.getMessage());
      e.printStackTrace();
      return "redirect:/downloadtasks";
    }
  }


  /**
   * POST action to retry failed download files.
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
  public String retryDownload(@RequestBody(required = false) String body,
      @RequestParam String taskId, @RequestParam String taskType, Model model,
      BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
    AjaxResponseBody result = new AjaxResponseBody();
    try {
      String authToken = (String) session.getAttribute("hpcUserToken");
      if (authToken == null) {
        result.setMessage("Invalid user session, expired. Please login again.");
        final Map<String, String> qParams = new HashMap<>();
        qParams.put("returnPath", "downloadtask");
        qParams.put("taskId", taskId);
        qParams.put("type", taskType);
        return "redirect:/login?".concat(MiscUtil.generateEncodedQueryString(
          qParams));
      }

      model.addAttribute("taskId", taskId);
      model.addAttribute("taskType", taskType);
      if (taskType.equals(HpcDownloadTaskType.DATA_OBJECT_LIST.name())
          || taskType.equals(HpcDownloadTaskType.COLLECTION_LIST.name())) {
        
        String queryServiceURL = dataObjectsDownloadServiceURL + "/" + taskId + "/retry";
            
        HpcCollectionDownloadStatusDTO downloadTask = HpcClientUtil
                .getDataObjectsDownloadTask(authToken, dataObjectsDownloadServiceURL + "/" + taskId, sslCertPath, sslCertPassword);
		try {
			HpcBulkDataObjectDownloadResponseDTO downloadDTO = HpcClientUtil.retryBulkDataObjectDownloadTask(authToken, queryServiceURL, sslCertPath,
					sslCertPassword);
			if (downloadDTO != null) {
				result.setMessage("Retry bulk download request successful. Task Id: " + downloadDTO.getTaskId());
				model.addAttribute("message", "Retry bulk download request successful. Task Id: <a href='downloadtask?type="+ taskType +"&taskId=" + downloadDTO.getTaskId()+"'>"+downloadDTO.getTaskId()+"</a>");
			}

			model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
	        return "dataobjectsdownloadtask";
	          
		} catch (Exception e) {
			result.setMessage("Retry bulk download request is not successful: " + e.getMessage());
			model.addAttribute("message", "Retry bulk download request is not successful. Task Id: " + taskId);
			model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
	        return "dataobjectsdownloadtask";
		}

      } else if (taskType.equals(HpcDownloadTaskType.COLLECTION.name())) {
    	String queryServiceURL = collectionDownloadServiceURL + "/" + taskId + "/retry";
    	HpcCollectionDownloadStatusDTO downloadTask = HpcClientUtil
                .getDataObjectsDownloadTask(authToken, collectionDownloadServiceURL + "?taskId=" + taskId, sslCertPath, sslCertPassword);
		try {
			HpcCollectionDownloadResponseDTO downloadDTO = HpcClientUtil.retryCollectionDownloadTask(authToken, queryServiceURL, sslCertPath,
					sslCertPassword);
			if (downloadDTO != null) {
				result.setMessage("Retry request successful. Task Id: " + downloadDTO.getTaskId());
				model.addAttribute("message", "Retry collection download request successful. Task Id: <a href='downloadtask?type="+ taskType +"&taskId=" + downloadDTO.getTaskId()+"'>"+downloadDTO.getTaskId()+"</a>");
			}
			model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
			return "dataobjectsdownloadtask";
		} catch (Exception e) {
			result.setMessage("Retry request is not successful: " + e.getMessage());
			model.addAttribute("message", "Retry request is not successful. Task Id: " + taskId);
			model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
			return "dataobjectsdownloadtask";
		}
      } else if (taskType.equals(HpcDownloadTaskType.DATA_OBJECT.name())) {
        String queryServiceURL = dataObjectDownloadServiceURL + "?taskId=" + taskId;
        HpcDataObjectDownloadStatusDTO downloadTask = HpcClientUtil
            .getDataObjectDownloadTask(authToken, queryServiceURL, sslCertPath, sslCertPassword);
        String serviceURL = dataObjectServiceURL + downloadTask.getPath() + "/download";
        if (!downloadTask.getResult().equals(HpcDownloadResult.COMPLETED)) {
          HpcDownloadRequestDTO downloadDTO = new HpcDownloadRequestDTO();
          HpcGlobusDownloadDestination destination = new HpcGlobusDownloadDestination();
		  HpcFileLocation location = downloadTask.getDestinationLocation();
		  destination.setDestinationLocation(location);
          downloadDTO.setGlobusDownloadDestination(destination);
          AjaxResponseBody responseBody = HpcClientUtil.downloadDataFile(authToken, serviceURL,
              downloadDTO, taskType, sslCertPath, sslCertPassword);
          model.addAttribute("error", responseBody.getMessage());
        }
        model.addAttribute("hpcDataObjectDownloadStatusDTO", downloadTask);
        return "dataobjectdownloadtask";
      }
    } catch (HttpStatusCodeException e) {
      result.setMessage("Download request is not successful: " + e.getMessage());
      return "redirect:/downloadtasks";
    } catch (RestClientException e) {
      result.setMessage("Download request is not successful: " + e.getMessage());
      return "redirect:/downloadtasks";
    } catch (Exception e) {
      result.setMessage("Download request is not successful: " + e.getMessage());
      return "redirect:/downloadtasks";
    }
    return "redirect:/downloadtasks";
  }

  /**
   * POST action to cancel download files.
   * @param body
   * @param taskId
   * @param taskType
   * @param model
   * @param bindingResult
   * @param session
   * @param request
   * @return
   */
@JsonView(Views.Public.class)
  @PostMapping(value = "/cancel")
  public String cancelDownload(@RequestBody(required = false) String body,
      @RequestParam String taskId, @RequestParam String taskType, Model model,
      BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
    AjaxResponseBody result = new AjaxResponseBody();
    try {
      String authToken = (String) session.getAttribute("hpcUserToken");
      if (authToken == null) {
        result.setMessage("Invalid user session, expired. Please login again.");
        final Map<String, String> qParams = new HashMap<>();
        qParams.put("returnPath", "downloadtask");
        qParams.put("taskId", taskId);
        qParams.put("type", taskType);
        return "redirect:/login?".concat(MiscUtil.generateEncodedQueryString(
          qParams));
      }

      model.addAttribute("taskId", taskId);
      model.addAttribute("taskType", taskType);
      if (taskType.equals(HpcDownloadTaskType.COLLECTION.name())
          || taskType.equals(HpcDownloadTaskType.DATA_OBJECT_LIST.name())
          || taskType.equals(HpcDownloadTaskType.COLLECTION_LIST.name())) {
        
        String queryServiceURL = "";
        if(taskType.equals(HpcDownloadTaskType.COLLECTION.name()))
        	queryServiceURL = collectionDownloadServiceURL + "/" + taskId + "/cancel";
        else
        	queryServiceURL = downloadServiceURL + "/" + taskId + "/cancel";

        try {
          boolean cancelled = HpcClientUtil
            .cancelDownloadTask(authToken, queryServiceURL, sslCertPath, sslCertPassword);
          if (cancelled)
          {
            result.setMessage(
                "Cancel request successful. Task Id: " + taskId);
            model.addAttribute("message",
            		"Cancel request successful. Task Id: " + taskId);
          }

        } catch (Exception e) {
          result.setMessage("Cancel request is not successful: " + e.getMessage());
          model.addAttribute("message",
          		"Cancel request is not successful. Task Id: " + taskId);
        }

      }
      if (taskType.equals(HpcDownloadTaskType.COLLECTION.name()))
          return displayCollectionTask(authToken, taskId, model);
        else if (taskType.equals(HpcDownloadTaskType.DATA_OBJECT.name()))
          return displayDataObjectTask(authToken, taskId, model);
        else if (taskType.equals(HpcDownloadTaskType.DATA_OBJECT_LIST.name()))
          return diplayDataObjectListTask(authToken, taskId, model);
        else if (taskType.equals(HpcDownloadTaskType.COLLECTION_LIST.name()))
            return displayCollectionListTask(authToken, taskId, model);
      
    } catch (HttpStatusCodeException e) {
      result.setMessage("Cancel request is not successful: " + e.getMessage());
      return "redirect:/downloadtasks";
    } catch (RestClientException e) {
      result.setMessage("Cancel request is not successful: " + e.getMessage());
      return "redirect:/downloadtasks";
    } catch (Exception e) {
      result.setMessage("Cancel request is not successful: " + e.getMessage());
      return "redirect:/downloadtasks";
    }
    return "redirect:/downloadtasks";
  }
  
  private String displayDataObjectTask(String authToken, String taskId, Model model) {
    String queryServiceURL = dataObjectDownloadServiceURL + "?taskId=" + taskId;
    HpcDataObjectDownloadStatusDTO downloadTask = HpcClientUtil.getDataObjectDownloadTask(authToken,
        queryServiceURL, sslCertPath, sslCertPassword);
    model.addAttribute("hpcDataObjectDownloadStatusDTO", downloadTask);
	boolean retry = true;
	if(downloadTask.getResult() != null && !downloadTask.getResult().equals(HpcDownloadResult.COMPLETED))
	{
		if(downloadTask != null && downloadTask.getDestinationType() != null)
		{
          if (downloadTask.getDestinationType().equals(HpcDataTransferType.S_3)
              || downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_DRIVE))
            retry = false;
		}
	}
	else
		retry = false;
	
	model.addAttribute("hpcBulkDataObjectDownloadRetry", retry);
    return "dataobjectdownloadtask";
  }

  private String displayCollectionTask(String authToken, String taskId, Model model) {
    String queryServiceURL = collectionDownloadServiceURL + "?taskId=" + taskId;
    HpcCollectionDownloadStatusDTO downloadTask = HpcClientUtil
        .getDataObjectsDownloadTask(authToken, queryServiceURL, sslCertPath, sslCertPassword);
	boolean retry = true;
	if(downloadTask != null && (!CollectionUtils.isEmpty(downloadTask.getFailedItems()) || !CollectionUtils.isEmpty(downloadTask.getCanceledItems())))
	{
        if (downloadTask.getDestinationType() != null
            && (downloadTask.getDestinationType().equals(HpcDataTransferType.S_3)
                || downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_DRIVE)))
           retry = false;
		if(downloadTask.getResult() != null && downloadTask.getResult().equals(HpcDownloadResult.CANCELED))
		   retry = false;
	}
	model.addAttribute("hpcBulkDataObjectDownloadRetry", retry);
    model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
    return "dataobjectsdownloadtask";
  }

  private String diplayDataObjectListTask(String authToken, String taskId, Model model) {
    String queryServiceURL = dataObjectsDownloadServiceURL + "/" + taskId;
    HpcCollectionDownloadStatusDTO downloadTask = HpcClientUtil
        .getDataObjectsDownloadTask(authToken, queryServiceURL, sslCertPath, sslCertPassword);
	boolean retry = true;
	if(downloadTask != null && (!CollectionUtils.isEmpty(downloadTask.getFailedItems()) || !CollectionUtils.isEmpty(downloadTask.getCanceledItems())))
	{
      if (downloadTask.getDestinationType() != null
          && (downloadTask.getDestinationType().equals(HpcDataTransferType.S_3)
              || downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_DRIVE)))
        retry = false;
      if(downloadTask.getResult() != null && downloadTask.getResult().equals(HpcDownloadResult.CANCELED))
		   retry = false;
	}
	model.addAttribute("hpcBulkDataObjectDownloadRetry", retry);
    model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
    return "dataobjectsdownloadtask";
  }
  
  private String displayCollectionListTask(String authToken, String taskId, Model model) {
	    String queryServiceURL = dataObjectsDownloadServiceURL + "/" + taskId;
	    HpcCollectionDownloadStatusDTO downloadTask = HpcClientUtil
	        .getDataObjectsDownloadTask(authToken, queryServiceURL, sslCertPath, sslCertPassword);
		boolean retry = true;
		if(downloadTask != null && (!CollectionUtils.isEmpty(downloadTask.getFailedItems()) || !CollectionUtils.isEmpty(downloadTask.getCanceledItems())))
		{
            if (downloadTask.getDestinationType() != null
                && (downloadTask.getDestinationType().equals(HpcDataTransferType.S_3)
                    || downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_DRIVE)))
               retry = false;
			if(downloadTask.getResult() != null && downloadTask.getResult().equals(HpcDownloadResult.CANCELED))
	           retry = false;
		}
		model.addAttribute("hpcBulkDataObjectDownloadRetry", retry);
	    model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
	    return "dataobjectsdownloadtask";
  }
}
