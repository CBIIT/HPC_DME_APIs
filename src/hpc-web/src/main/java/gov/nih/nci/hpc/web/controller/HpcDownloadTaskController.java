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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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

import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaAccount;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadStatusDTO;
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
      model.addAttribute("asperaUser", "asp-dbgap");
      model.addAttribute("asperaHost", "gap-submit.ncbi.nlm.nih.gov");
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
      @RequestParam String taskId, @RequestParam String taskType, 
      @RequestParam String accessKey, @RequestParam String secretKey, @RequestParam String region,
      @RequestParam String asperaUser, @RequestParam String asperaPassword,  @RequestParam String asperaHost,
      Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
    AjaxResponseBody result = new AjaxResponseBody();
    try {
      HpcUserDTO retryUser = (HpcUserDTO) session.getAttribute("hpcUser");
      String retryUserId = (String) session.getAttribute("hpcUserId");
      String authToken = (String) session.getAttribute("hpcUserToken");
      if (authToken == null) {
        result.setMessage("Invalid user session, expired. Please login again.");
        final Map<String, String> qParams = new HashMap<>();
        qParams.put("returnPath", "downloadtask");
        qParams.put("taskId", taskId);
        qParams.put("type", taskType);
        qParams.put("accessKey", accessKey);
        qParams.put("secretKey", secretKey);
        qParams.put("region", region);
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

        HpcS3Account s3Account = null;
        if (downloadTask.getDestinationType() != null && downloadTask.getDestinationType().equals(HpcDataTransferType.S_3)) {
			s3Account = new HpcS3Account();
			s3Account.setAccessKey(accessKey);
			s3Account.setSecretKey(secretKey);
			s3Account.setRegion(region);
		}
        HpcAsperaAccount asperaAccount = null;
        if (downloadTask.getDestinationType() != null && downloadTask.getDestinationType().equals(HpcDataTransferType.ASPERA)) {
			asperaAccount = new HpcAsperaAccount();
			asperaAccount.setUser(asperaUser);
			asperaAccount.setPassword(asperaPassword);
			asperaAccount.setHost(asperaHost);
		}
        
		try {
			HpcBulkDataObjectDownloadResponseDTO downloadDTO = HpcClientUtil.retryBulkDataObjectDownloadTask(authToken, queryServiceURL, sslCertPath,
					sslCertPassword, s3Account, asperaAccount);
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
    	
    	//Get the list of items from the original task which is being retried.
    	List<HpcCollectionDownloadStatusDTO> previousTasks = retrieveOrigCollectionTaskItems(authToken, taskId, model, downloadTask,
    			new ArrayList<HpcCollectionDownloadStatusDTO>());
    	
        HpcS3Account s3Account = null;
        if (downloadTask.getDestinationType() != null && downloadTask.getDestinationType().equals(HpcDataTransferType.S_3)) {
			s3Account = new HpcS3Account();
			s3Account.setAccessKey(accessKey);
			s3Account.setSecretKey(secretKey);
			s3Account.setRegion(region);
		}
        HpcAsperaAccount asperaAccount = null;
        if (downloadTask.getDestinationType() != null && downloadTask.getDestinationType().equals(HpcDataTransferType.ASPERA)) {
			asperaAccount = new HpcAsperaAccount();
			asperaAccount.setUser(asperaUser);
			asperaAccount.setPassword(asperaPassword);
			asperaAccount.setHost(asperaHost);
		}
        
		try {
			HpcCollectionDownloadResponseDTO downloadDTO = HpcClientUtil.retryCollectionDownloadTask(authToken, queryServiceURL, sslCertPath,
					sslCertPassword, s3Account, asperaAccount);
			if (downloadDTO != null) {
				result.setMessage("Retry request successful. Task Id: " + downloadDTO.getTaskId());
				model.addAttribute("message", "Retry collection download request successful. Task Id: <a href='downloadtask?type="+ taskType +"&taskId=" + downloadDTO.getTaskId()+"'>"+downloadDTO.getTaskId()+"</a>");
			}
			downloadTask.setRetryUserId(downloadDTO.getRetryUserId());
			model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
			model.addAttribute("hpcOrigDataObjectsDownloadStatusDTOs", previousTasks);
			return "dataobjectsdownloadtask";
		} catch (Exception e) {
			result.setMessage(e.getMessage());
			model.addAttribute("error", e.getMessage());
			model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
			return "dataobjectsdownloadtask";
		}
      } else if (taskType.equals(HpcDownloadTaskType.DATA_OBJECT.name())) {
    	String queryServiceURL = dataObjectDownloadServiceURL + "?taskId=" + taskId;
    	HpcDataObjectDownloadStatusDTO downloadTask = HpcClientUtil
    	            .getDataObjectDownloadTask(authToken, queryServiceURL, sslCertPath, sslCertPassword);
    	String serviceURL = dataObjectDownloadServiceURL + "/" + taskId + "/retry";
    	    
    	HpcS3Account s3Account = null;
        if (downloadTask.getDestinationType() != null && downloadTask.getDestinationType().equals(HpcDataTransferType.S_3)) {
			s3Account = new HpcS3Account();
			s3Account.setAccessKey(accessKey);
			s3Account.setSecretKey(secretKey);
			s3Account.setRegion(region);
		}
        HpcAsperaAccount asperaAccount = null;
        if (downloadTask.getDestinationType() != null && downloadTask.getDestinationType().equals(HpcDataTransferType.ASPERA)) {
			asperaAccount = new HpcAsperaAccount();
			asperaAccount.setUser(asperaUser);
			asperaAccount.setPassword(asperaPassword);
			asperaAccount.setHost(asperaHost);
		}
        
        try {
			HpcDataObjectDownloadResponseDTO downloadDTO = HpcClientUtil.retryDataObjectDownloadTask(authToken, serviceURL, sslCertPath,
					sslCertPassword, s3Account, asperaAccount);
			if (downloadDTO != null) {
				result.setMessage("Retry request successful. Task Id: " + downloadDTO.getTaskId());
				model.addAttribute("message", "Retry data object download request successful. Task Id: <a href='downloadtask?type="+ taskType +"&taskId=" + downloadDTO.getTaskId()+"'>"+downloadDTO.getTaskId()+"</a>");
			}
		} catch (Exception e) {
			result.setMessage(e.getMessage());
			model.addAttribute("error", e.getMessage());
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
          if (downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_CLOUD_STORAGE)
              || downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_DRIVE))
            retry = false;
		}
	}
	else {
		//No retry button if download is in progress or successfully completed
		retry = false;
	}
	
	model.addAttribute("hpcBulkDataObjectDownloadRetry", retry);
    return "dataobjectdownloadtask";
  }


  private String displayCollectionTask(String authToken, String taskId, Model model) {
    String queryServiceURL = collectionDownloadServiceURL + "?taskId=" + taskId;
    HpcCollectionDownloadStatusDTO downloadTask = HpcClientUtil
        .getDataObjectsDownloadTask(authToken, queryServiceURL, sslCertPath, sslCertPassword);

    //Indicates whether retry icon should be set or not.
	boolean retry = true;
	if(downloadTask.getResult() != null && (!CollectionUtils.isEmpty(downloadTask.getFailedItems()) || !CollectionUtils.isEmpty(downloadTask.getCanceledItems())))
	{
        if (downloadTask.getDestinationType() != null
            && (downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_CLOUD_STORAGE)
                || downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_DRIVE)
                || downloadTask.getDestinationType().equals(HpcDataTransferType.BOX)));
           retry = false;
	} else {
		//No retry button if download is in progress or has no failed or cancelled items
		retry = false;
	}

	//Retrieve the list of items from the original request, if we are displaying a retry transaction.
	List<HpcCollectionDownloadStatusDTO> previousTasks = retrieveOrigCollectionTaskItems(authToken, taskId, model, downloadTask,
			new ArrayList<HpcCollectionDownloadStatusDTO>());

	long completedItemsSize = getCompletedItemsSize(downloadTask, previousTasks);
	int completedItemsCount = 0;
	int totalItemsCount = 0;
	//If previousTasks is not empty, update the message to include the items in it
	if(!previousTasks.isEmpty()) {
		completedItemsCount = getCompletedItemsCount(downloadTask, previousTasks);
		totalItemsCount = getTotalItemsCount(downloadTask, previousTasks);
		//Override the server message
		String message = completedItemsCount + " items downloaded successfully out of " + totalItemsCount;
		downloadTask.setMessage(message);
	}

	//If status is RECEIVED but staging has begun, or status is RECEIVED in between the time when
	//staging completed and  2nd hop has not begun, then display it as ACTIVE
	if(downloadTask.getTaskStatus() == HpcCollectionDownloadTaskStatus.RECEIVED) {
		if(!downloadTask.getStagingInProgressItems().isEmpty()
				|| !downloadTask.getInProgressItems().isEmpty()) {
			downloadTask.setTaskStatus(HpcCollectionDownloadTaskStatus.ACTIVE);
		}
	}

	model.addAttribute("hpcBulkDataObjectDownloadRetry", retry);
    model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
    model.addAttribute("hpcOrigDataObjectsDownloadStatusDTOs", previousTasks);
    model.addAttribute("hpcDataObjectsDownloadBytesTransferred", MiscUtil.addHumanReadableSize(Long.toString(completedItemsSize), true));
    model.addAttribute("pendingItemsCount", totalItemsCount - completedItemsCount);
    return "dataobjectsdownloadtask";
  }


  private List<HpcCollectionDownloadStatusDTO> retrieveOrigCollectionTaskItems(String authToken, String taskId, Model model,
		  HpcCollectionDownloadStatusDTO downloadTask, List<HpcCollectionDownloadStatusDTO> previousTasks) {

	  if(downloadTask!= null && downloadTask.getRetryTaskId() != null) {
		    String queryServiceURL = collectionDownloadServiceURL + "?taskId=" + downloadTask.getRetryTaskId();
			HpcCollectionDownloadStatusDTO origDownloadTask = HpcClientUtil
			        .getDataObjectsDownloadTask(authToken, queryServiceURL, sslCertPath, sslCertPassword);
			if(origDownloadTask != null && origDownloadTask.getCompletedItems() != null && origDownloadTask.getCompletedItems().size() > 0) {
				previousTasks.add(origDownloadTask);
			}
			retrieveOrigCollectionTaskItems(authToken, taskId, model, origDownloadTask, previousTasks);
	  }
	  return previousTasks;
  }


  private String diplayDataObjectListTask(String authToken, String taskId, Model model) {
    String queryServiceURL = dataObjectsDownloadServiceURL + "/" + taskId;
    HpcCollectionDownloadStatusDTO downloadTask = HpcClientUtil
        .getDataObjectsDownloadTask(authToken, queryServiceURL, sslCertPath, sslCertPassword);
	boolean retry = true;
	if(downloadTask != null && (!CollectionUtils.isEmpty(downloadTask.getFailedItems()) || !CollectionUtils.isEmpty(downloadTask.getCanceledItems())))
	{
      if (downloadTask.getDestinationType() != null
          && (downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_CLOUD_STORAGE)
              || downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_DRIVE)))
        retry = false;
	} else {
		//No retry button if download is in progress or has no failed or cancelled items
		retry = false;
	}


	List<HpcCollectionDownloadStatusDTO> previousTasks = retrieveOrigCollectionTaskItems(authToken, taskId, model, downloadTask,
			new ArrayList<HpcCollectionDownloadStatusDTO>());

	long completedItemsSize = getCompletedItemsSize(downloadTask, previousTasks);
	int completedItemsCount = 0;
	int totalItemsCount = 0;
	//If previousTasks is not empty, update the message to include the items in it
	if(!previousTasks.isEmpty()) {
		completedItemsCount = getCompletedItemsCount(downloadTask, previousTasks);
		totalItemsCount = getTotalItemsCount(downloadTask, previousTasks);
		//Override the server message
		String message = completedItemsCount + " items downloaded successfully out of " + totalItemsCount;
		downloadTask.setMessage(message);
	}

	model.addAttribute("hpcBulkDataObjectDownloadRetry", retry);
    model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
    model.addAttribute("hpcOrigDataObjectsDownloadStatusDTOs", previousTasks);
    model.addAttribute("hpcDataObjectsDownloadBytesTransferred", MiscUtil.addHumanReadableSize(Long.toString(completedItemsSize), true));
    model.addAttribute("pendingItemsCount", totalItemsCount - completedItemsCount);
    return "dataobjectsdownloadtask";
  }


  private String displayCollectionListTask(String authToken, String taskId, Model model) {
	    String queryServiceURL = dataObjectsDownloadServiceURL + "/" + taskId;
	    HpcCollectionDownloadStatusDTO downloadTask = HpcClientUtil
	        .getDataObjectsDownloadTask(authToken, queryServiceURL, sslCertPath, sslCertPassword);
		boolean retry = true;
		if(downloadTask.getResult() != null && (!CollectionUtils.isEmpty(downloadTask.getFailedItems()) || !CollectionUtils.isEmpty(downloadTask.getCanceledItems())))
		{
            if (downloadTask.getDestinationType() != null
                && (downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_CLOUD_STORAGE)
                    || downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_DRIVE)))
               retry = false;
		} else {
			//No retry button if download is in progress or has no failed or cancelled items
			retry = false;
		}

		List<HpcCollectionDownloadStatusDTO> previousTasks = retrieveOrigCollectionTaskItems(authToken, taskId, model, downloadTask,
				new ArrayList<HpcCollectionDownloadStatusDTO>());

		long completedItemsSize = getCompletedItemsSize(downloadTask, previousTasks);
		int completedItemsCount = 0;
		int totalItemsCount = 0;
		//If previousTasks is not empty, update the message to include the items in it
		if(!previousTasks.isEmpty()) {
			completedItemsCount = getCompletedItemsCount(downloadTask, previousTasks);
			totalItemsCount = getTotalItemsCount(downloadTask, previousTasks);
			//Override the server message
			String message = completedItemsCount + " items downloaded successfully out of " + totalItemsCount;
			downloadTask.setMessage(message);
		}

		model.addAttribute("hpcBulkDataObjectDownloadRetry", retry);
	    model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
	    model.addAttribute("hpcOrigDataObjectsDownloadStatusDTOs", previousTasks);
	    model.addAttribute("hpcDataObjectsDownloadBytesTransferred", MiscUtil.addHumanReadableSize(Long.toString(completedItemsSize), true));
	    model.addAttribute("pendingItemsCount", totalItemsCount - completedItemsCount);
	    return "dataobjectsdownloadtask";
  }


  private int getTotalItemsCount(HpcCollectionDownloadStatusDTO downloadTask, List<HpcCollectionDownloadStatusDTO> previousTasks) {
	  int totalDownloadTaskItemsCount = 0;
	  List<HpcCollectionDownloadStatusDTO> parentPreviousTasks = null;
	  if(!previousTasks.isEmpty()) {
		  HpcCollectionDownloadStatusDTO previousTask = previousTasks.get(0);
		  totalDownloadTaskItemsCount +=
				  (previousTask.getCanceledItems() != null ? previousTask.getCanceledItems().size() : 0)
				  + (previousTask.getFailedItems() != null ? previousTask.getFailedItems().size() : 0);
		  parentPreviousTasks = new ArrayList<>(previousTasks);
		  parentPreviousTasks.remove(0);
	  }

	  return totalDownloadTaskItemsCount + getCompletedItemsCount(previousTasks.get(0), parentPreviousTasks);
  }


  private int getCompletedItemsCount(HpcCollectionDownloadStatusDTO downloadTask, List<HpcCollectionDownloadStatusDTO> previousTasks) {
	  int completedItemsCount = 0;
	  if(downloadTask != null) {
		  completedItemsCount = downloadTask.getCompletedItems() != null ? downloadTask.getCompletedItems().size() : 0;
		  if(!previousTasks.isEmpty()) {
			  for(HpcCollectionDownloadStatusDTO previousTask: previousTasks) {
				  completedItemsCount += previousTask.getCompletedItems() != null ? previousTask.getCompletedItems().size() : 0;
			  }
		  }
	  }
	  return completedItemsCount;
  }

  private long getCompletedItemsSize(HpcCollectionDownloadStatusDTO downloadTask, List<HpcCollectionDownloadStatusDTO> previousTasks) {
	  long completedItemsSize = 0;
	  if(downloadTask != null) {
		  for(HpcCollectionDownloadTaskItem completedItem: downloadTask.getCompletedItems())
			  completedItemsSize += completedItem.getSize() != null ? completedItem.getSize() : 0;
		  if(previousTasks != null && !previousTasks.isEmpty()) {
			  for(HpcCollectionDownloadStatusDTO previousTask: previousTasks) {
				  for(HpcCollectionDownloadTaskItem prevCompletedItem: previousTask.getCompletedItems())
					  completedItemsSize += prevCompletedItem.getSize() != null ? prevCompletedItem.getSize() : 0;
			  }
		  }
		  //Also add in progress item current bytes transferred
		  for (HpcCollectionDownloadTaskItem inProgressItem : downloadTask.getInProgressItems())
			  completedItemsSize += inProgressItem.getSize() != null && inProgressItem.getPercentComplete() != null
						? inProgressItem.getSize() * (inProgressItem.getPercentComplete() / 100.0) : 0;
	  }
	  return completedItemsSize;
  }

}
