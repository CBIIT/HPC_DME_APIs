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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.annotation.JsonView;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
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

  public static final String NAV_OUTCOME_DOWNLOADTASKS =
    "redirect:/downloadtasks";
  public static final String NAV_OUTCOME_DATAOBJ_DOWNLOADTASK =
    "dataobjectdownloadtask";
  public static final String NAV_OUTCOME_DATAOBJS_DOWNLOADTASK =
    "dataobjectsdownloadtask";

  @Value("${gov.nih.nci.hpc.server.download}")
  private String dataObjectsDownloadServiceURL;

  @Value("${gov.nih.nci.hpc.server.collection.download}")
  private String collectionDownloadServiceURL;

  @Value("${gov.nih.nci.hpc.server.dataObject.download}")
  private String dataObjectDownloadServiceURL;

  @Value("${gov.nih.nci.hpc.server.dataObject}")
  private String dataObjectServiceURL;

  @Value("${gov.nih.nci.hpc.server.collection}")
  private String collectionServiceURL;

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
        return "redirect:/login?returnPath=downloadtask&taskId=" + taskId + "&type=" + type;
      }

      if (taskId == null || type == null)
        return NAV_OUTCOME_DOWNLOADTASKS;

      if (type.equals(HpcDownloadTaskType.COLLECTION.name()))
        return displayCollectionTask(authToken, taskId, model);
      else if (type.equals(HpcDownloadTaskType.DATA_OBJECT.name()))
        return displayDataObjectTask(authToken, taskId, model);
      else if (type.equals(HpcDownloadTaskType.DATA_OBJECT_LIST.name()))
        return diplayDataObjectListTask(authToken, taskId, model);
      else {
        String message = "Data file not found!";
        model.addAttribute("error", message);
        return NAV_OUTCOME_DOWNLOADTASKS;
      }
    } catch (Exception e) {
      model.addAttribute("error", "Failed to get data file: " + e.getMessage());
      e.printStackTrace();
      return NAV_OUTCOME_DOWNLOADTASKS;
    }
  }


  /**
   * POST action to retry failed download files.
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
        return "redirect:/login?returnPath=downloadtask&taskId=" + taskId + "&type=" + taskType;
      }

      model.addAttribute("taskId", taskId);
      model.addAttribute("taskType", taskType);
      HpcBulkDataObjectDownloadRequestDTO dto = new HpcBulkDataObjectDownloadRequestDTO();
      if (taskType.equals(HpcDownloadTaskType.COLLECTION.name())
          || taskType.equals(HpcDownloadTaskType.DATA_OBJECT_LIST.name())) {

        String queryServiceURL = null;
        if (taskType.equals(HpcDownloadTaskType.COLLECTION.name())) {
          queryServiceURL = collectionDownloadServiceURL + "?taskId=" + taskId;
        } else {
          queryServiceURL = dataObjectsDownloadServiceURL + "/" + taskId;
        }

        HpcCollectionDownloadStatusDTO downloadTask = HpcClientUtil
            .getDataObjectsDownloadTask(authToken, queryServiceURL, sslCertPath, sslCertPassword);
        if (downloadTask.getFailedItems() != null && !downloadTask.getFailedItems().isEmpty()) {
          for (HpcCollectionDownloadTaskItem item : downloadTask.getFailedItems()) {
            dto.getDataObjectPaths().add(item.getPath());
          }
          dto.setDestination(downloadTask.getDestinationLocation());
          dto.setDestinationOverwrite(true);
        }
        try {
          HpcBulkDataObjectDownloadResponseDTO downloadDTO =
              (HpcBulkDataObjectDownloadResponseDTO) HpcClientUtil.downloadFiles(authToken,
                  dataObjectsDownloadServiceURL, dto, sslCertPath, sslCertPassword);
          if (downloadDTO != null) {
            result.setMessage(
                "Download request successfull. Task Id: " + downloadDTO.getTaskId());
            model.addAttribute("error",
                "Retry download request is submitted successfully. Task Id: " + downloadDTO
                    .getTaskId());
          }

          model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
          return NAV_OUTCOME_DATAOBJS_DOWNLOADTASK;

        } catch (Exception e) {
          result.setMessage("Download request is not successfull: " + e.getMessage());
          model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
          return NAV_OUTCOME_DATAOBJS_DOWNLOADTASK;
        }

      } else if (taskType.equals(HpcDownloadTaskType.DATA_OBJECT.name())) {
        String queryServiceURL = dataObjectDownloadServiceURL + "?taskId=" + taskId;
        HpcDataObjectDownloadStatusDTO downloadTask = HpcClientUtil
            .getDataObjectDownloadTask(authToken, queryServiceURL, sslCertPath, sslCertPassword);
        final String serviceURL = generateDownloadDataFileServiceURL(downloadTask);
        if (!downloadTask.getResult()) {
          HpcDownloadRequestDTO downloadDTO = new HpcDownloadRequestDTO();
          downloadDTO.setDestination(downloadTask.getDestinationLocation());
          AjaxResponseBody responseBody = HpcClientUtil.downloadDataFile(authToken, serviceURL,
              downloadDTO, sslCertPath, sslCertPassword);
          model.addAttribute("error", responseBody.getMessage());
        }
        model.addAttribute("hpcDataObjectDownloadStatusDTO", downloadTask);
        return NAV_OUTCOME_DATAOBJ_DOWNLOADTASK;
      }
    } catch (HttpStatusCodeException e) {
      return handleExceptionOnDownloadRequest(result, e.getMessage());
    } catch (RestClientException e) {
      return handleExceptionOnDownloadRequest(result, e.getMessage());
    } catch (Exception e) {
      return handleExceptionOnDownloadRequest(result, e.getMessage());
    }
    return NAV_OUTCOME_DOWNLOADTASKS;
  }

  private String handleExceptionOnDownloadRequest(
    AjaxResponseBody result, String message) {
    result.setMessage(
      String.format("Download request is not successful: %s", message));
    return NAV_OUTCOME_DOWNLOADTASKS;
  }

  private String generateDownloadDataFileServiceURL(
    HpcDataObjectDownloadStatusDTO downloadTask) {
    final StringBuilder sb = new StringBuilder(dataObjectServiceURL);
    sb.append(MiscUtil.urlEncodeDmePath(downloadTask.getPath()));
    sb.append("/download");
    final String retServiceURL = sb.toString();
    return retServiceURL;
  }

  private String displayDataObjectTask(String authToken, String taskId, Model model) {
    String queryServiceURL = dataObjectDownloadServiceURL + "?taskId=" + taskId;
    HpcDataObjectDownloadStatusDTO downloadTask = HpcClientUtil.getDataObjectDownloadTask(authToken,
        queryServiceURL, sslCertPath, sslCertPassword);
    model.addAttribute("hpcDataObjectDownloadStatusDTO", downloadTask);
    return NAV_OUTCOME_DATAOBJ_DOWNLOADTASK;
  }

  private String displayCollectionTask(String authToken, String taskId, Model model) {
    String queryServiceURL = collectionDownloadServiceURL + "?taskId=" + taskId;
    HpcCollectionDownloadStatusDTO downloadTask = HpcClientUtil
        .getDataObjectsDownloadTask(authToken, queryServiceURL, sslCertPath, sslCertPassword);
    model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
    return NAV_OUTCOME_DATAOBJS_DOWNLOADTASK;
  }

  private String diplayDataObjectListTask(String authToken, String taskId, Model model) {
    String queryServiceURL = dataObjectsDownloadServiceURL + "/" + taskId;
    HpcCollectionDownloadStatusDTO downloadTask = HpcClientUtil
        .getDataObjectsDownloadTask(authToken, queryServiceURL, sslCertPath, sslCertPassword);
    model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
    return NAV_OUTCOME_DATAOBJS_DOWNLOADTASK;
  }
}
