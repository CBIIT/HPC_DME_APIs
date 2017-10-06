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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.annotation.JsonView;

import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcDatafileModel;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcMetadataAttrEntry;
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
@RequestMapping("/task")
public class HpcDownloadTaskController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.download}")
	private String dataObjectsDownloadServiceURL;

	@Value("${gov.nih.nci.hpc.server.collection.download}")
	private String collectionDownloadServiceURL;
	
	@Value("${gov.nih.nci.hpc.server.dataObject.download}")
	private String dataObjectDownloadServiceURL;

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
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			String userId = (String) session.getAttribute("hpcUserId");
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (user == null || authToken == null) {
				ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
				bindingResult.addError(error);
				HpcLogin hpcLogin = new HpcLogin();
				model.addAttribute("hpcLogin", hpcLogin);
				return "redirect:/login?returnPath=task&taskId="+taskId+"&type="+type;
			}

			if (taskId == null || type == null)
				return "redirect:/downloadtasks";
			
			if(type.equals(HpcDownloadTaskType.COLLECTION.name()))
				return displayCollectionTask(authToken, taskId, model);
			else if(type.equals(HpcDownloadTaskType.DATA_OBJECT.name()))
				return displayDataObjectTask(authToken, taskId, model);
			else if(type.equals(HpcDownloadTaskType.DATA_OBJECT_LIST.name()))
				return diplayDataObjectListTask(authToken, taskId, model);
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

	private String displayDataObjectTask(String authToken, String taskId, Model model)
	{
		String queryServiceURL = dataObjectDownloadServiceURL + "?taskId="+taskId;
		HpcDataObjectDownloadStatusDTO downloadTask = HpcClientUtil.getDataObjectDownloadTask(authToken, queryServiceURL,
				sslCertPath, sslCertPassword);
		model.addAttribute("hpcDataObjectDownloadStatusDTO", downloadTask);
		return "dataobjectdownloadtask";
	}
	
	private String displayCollectionTask(String authToken, String taskId, Model model)
	{
		String queryServiceURL = collectionDownloadServiceURL + "?taskId="+taskId;
		HpcCollectionDownloadStatusDTO downloadTask = HpcClientUtil.getDataObjectsDownloadTask(authToken, queryServiceURL,
				sslCertPath, sslCertPassword);
		model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
		return "dataobjectsdownloadtask";
	}

	private String diplayDataObjectListTask(String authToken, String taskId, Model model)
	{
		String queryServiceURL = dataObjectsDownloadServiceURL + "/"+taskId;
		HpcCollectionDownloadStatusDTO downloadTask = HpcClientUtil.getDataObjectsDownloadTask(authToken, queryServiceURL,
				sslCertPath, sslCertPassword);
		model.addAttribute("hpcDataObjectsDownloadStatusDTO", downloadTask);
		return "dataobjectsdownloadtask";
	}
}
