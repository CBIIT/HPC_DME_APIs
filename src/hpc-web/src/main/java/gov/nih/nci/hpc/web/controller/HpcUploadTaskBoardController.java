/**
 * HpcDashBoardController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationTaskDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcRegistrationSummaryDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcTask;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcIdentityUtil;
import gov.nih.nci.hpc.web.util.HpcSearchUtil;

/**
 * <p>
 * Upload task controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDashBoardController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/uploadtasks")
public class HpcUploadTaskBoardController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionURL;
	@Value("${gov.nih.nci.hpc.server.query}")
	private String queryURL;
	@Value("${gov.nih.nci.hpc.server.bulkregistration}")
	private String queryServiceURL;
	@Value("${gov.nih.nci.hpc.server.bulkregistration.all}")
	private String queryAllServiceURL;

	/**
	 * GET action to display dashboard page
	 * 
	 * @param q
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String home(@RequestBody(required = false) String q, @RequestParam(required = false) String queryAll, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		model.addAttribute("queryURL", queryURL);
		model.addAttribute("collectionURL", collectionURL);
		model.addAttribute("queryAll", queryAll == null ? false : true);
		model.addAttribute("canQueryAll", HpcIdentityUtil.iUserSystemAdminOrGroupAdmin(session));
		String authToken = (String) session.getAttribute("hpcUserToken");

		if (authToken == null) {
			return "redirect:/";
		}
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/";
		}
		List<HpcTask> result = new ArrayList<HpcTask>();
		try {
			int page = 1;
			String pageStr = request.getParameter("page");
			if(pageStr != null && !pageStr.isEmpty())
			{
				page = Integer.parseInt(pageStr);
			}
			
      final MultiValueMap<String,String> paramsMap = new LinkedMultiValueMap<>();
      paramsMap.set("page", Integer.toString(page));
      paramsMap.set("totalCount", Boolean.TRUE.toString());
			HpcRegistrationSummaryDTO registrations = HpcClientUtil
        .getRegistrationSummary(authToken, (queryAll == null ? queryServiceURL : queryAllServiceURL), paramsMap,
        this.sslCertPath, this.sslCertPassword);
			SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm");
			if (registrations.getActiveTasks() != null && !registrations.getActiveTasks().isEmpty())
				for (HpcBulkDataObjectRegistrationTaskDTO registration : registrations.getActiveTasks()) {
					HpcTask task = new HpcTask();
					task.setUserId(registration.getUserId());
					task.setTaskId(registration.getTaskId());
					if(registration.getTaskStatus() != null)
						task.setStatus(registration.getTaskStatus().value());
					task.setCreated(
							registration.getCreated() != null ? format.format(registration.getCreated().getTime()) : "");
					task.setCompleted(
							registration.getCompleted() != null ? format.format(registration.getCompleted().getTime()) : "");
					String transferResult = "In Process";
					if (registration.getResult() != null) {
						if (registration.getResult())
							transferResult = "Completed";
						else
							transferResult = "Failed";
					}
					task.setResult(transferResult);
					result.add(task);
				}
			for (HpcBulkDataObjectRegistrationTaskDTO registration : registrations.getCompletedTasks()) {
				HpcTask task = new HpcTask();
				task.setUserId(registration.getUserId());
				task.setTaskId(registration.getTaskId());
				if(registration.getTaskStatus() != null)
					task.setStatus(registration.getTaskStatus().value());
				task.setCreated(registration.getCreated() != null ? format.format(registration.getCreated().getTime()) : "");
				task.setCompleted(
						registration.getCompleted() != null ? format.format(registration.getCompleted().getTime()) : "");
				String transferResult = "In Process";
				if (registration.getResult() != null) {
					if (registration.getResult())
						transferResult = "Completed";
					else
						transferResult = "Failed";
				}
				task.setResult(transferResult);
				result.add(task);
			}
			model.addAttribute("currentPage", Integer.toString(page));
			model.addAttribute("totalCount", registrations.getTotalCount());
			model.addAttribute("totalPages", HpcSearchUtil.getTotalPages(registrations.getTotalCount(), registrations.getLimit()));
			model.addAttribute("currentPageSize", result.size());
			model.addAttribute("results", result);
		} catch (Exception e) {
			model.addAttribute("error", "Failed to get registration tasks: "+e.getMessage());
		}
		return "uploadtaskboard";
	}
}
