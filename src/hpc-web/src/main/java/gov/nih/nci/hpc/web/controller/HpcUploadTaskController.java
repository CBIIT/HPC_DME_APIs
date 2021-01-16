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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.MiscUtil;

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
@RequestMapping("/uploadtask")
public class HpcUploadTaskController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.v2.bulkregistration}")
	private String registrationServiceURL;

	/**
	 * Get operation to display registration task details and its metadata
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
				final Map<String, String> queryParams = new HashMap<>();
				queryParams.put("returnPath", "uploadtask");
				queryParams.put("taskId", taskId);
				queryParams.put("type", type);
				return "redirect:/login?".concat(MiscUtil.generateEncodedQueryString(queryParams));
			}

			if (taskId == null || type == null)
				return "redirect:/uploadtasks";

			HpcBulkDataObjectRegistrationStatusDTO uploadTask = HpcClientUtil.getDataObjectRegistrationTask(authToken,
					this.registrationServiceURL, taskId, this.sslCertPath, this.sslCertPassword);
			boolean retry = false;
			if (uploadTask != null && uploadTask.getTask() != null) {
				List<HpcDataObjectRegistrationItemDTO> failedRequests = uploadTask.getTask().getFailedItemsRequest();
				if (failedRequests != null && !failedRequests.isEmpty())
					retry = true;

				for (HpcDataObjectRegistrationItemDTO dto : failedRequests) {
					// Retry is not available for S3 based bulk requests
					if (dto.getGlobusUploadSource() == null ) {
						retry = false;
						break;
					}
				}
			}
			model.addAttribute("hpcBulkDataObjectRegistrationTaskDTO", uploadTask.getTask());
			model.addAttribute("hpcBulkDataObjectRegistrationRetry", retry);
		} catch (Exception e) {
			model.addAttribute("error", "Failed to get registration status: " + e.getMessage());
			return "redirect:/uploadtasks";
		}
		return "dataobjectsuploadtask";
	}

	/**
	 * POST action to retry failed upload files.
	 * 
	 * @param uploadFile
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @return
	 */
	@JsonView(Views.Public.class)
	@RequestMapping(method = RequestMethod.POST)
	public String retryUpload(@RequestBody(required = false) String body, @RequestParam String taskId, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
		AjaxResponseBody result = new AjaxResponseBody();
		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (authToken == null) {
				result.setMessage("Invalid user session, expired. Please login again.");
				final Map<String, String> qParams = new HashMap<>();
				qParams.put("returnPath", "uploadtask");
				qParams.put("taskId", taskId);
				return "redirect:/login?".concat(MiscUtil.generateEncodedQueryString(qParams));
			}

			model.addAttribute("taskId", taskId);
			HpcBulkDataObjectRegistrationStatusDTO uploadTask = HpcClientUtil.getDataObjectRegistrationTask(authToken,
					this.registrationServiceURL, taskId, this.sslCertPath, this.sslCertPassword);

			HpcBulkDataObjectRegistrationRequestDTO registrationDTO = constructBulkRequest(request, session,
					uploadTask);

			HpcBulkDataObjectRegistrationResponseDTO responseDTO = HpcClientUtil.registerBulkDatafiles(authToken,
					registrationServiceURL, registrationDTO, sslCertPath, sslCertPassword);
			if (responseDTO != null) {
				StringBuffer info = new StringBuffer();
				for (HpcDataObjectRegistrationItemDTO responseItem : responseDTO.getDataObjectRegistrationItems()) {
					info.append(responseItem.getPath()).append("<br/>");
				}
				model.addAttribute("error",
						"Bulk Data file registration request is submitted! Task Id: <a href='uploadtask?type=&taskId="
								+ responseDTO.getTaskId() + "'>" + responseDTO.getTaskId() + "</a>");
			}
			model.addAttribute("hpcBulkDataObjectRegistrationTaskDTO", uploadTask.getTask());

		} catch (HttpStatusCodeException e) {
			result.setMessage("Upload request is not successful: " + e.getMessage());
			return "dataobjectsuploadtask";
		} catch (RestClientException e) {
			result.setMessage("Upload request is not successful: " + e.getMessage());
			return "dataobjectsuploadtask";
		} catch (Exception e) {
			result.setMessage("Upload request is not successful: " + e.getMessage());
			return "dataobjectsuploadtask";
		}
		return "dataobjectsuploadtask";
	}

	protected HpcBulkDataObjectRegistrationRequestDTO constructBulkRequest(HttpServletRequest request,
			HttpSession session, HpcBulkDataObjectRegistrationStatusDTO uploadTask) {
		HpcBulkDataObjectRegistrationRequestDTO dto = new HpcBulkDataObjectRegistrationRequestDTO();
		dto.getDataObjectRegistrationItems().addAll(uploadTask.getTask().getFailedItemsRequest());
		return dto;
	}
}
