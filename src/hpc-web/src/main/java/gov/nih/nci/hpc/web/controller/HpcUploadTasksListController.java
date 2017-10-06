/**
 * HpcSavedSearchListController.java
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
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.annotation.JsonView;

import gov.nih.nci.hpc.domain.datatransfer.HpcUserDownloadRequest;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadSummaryDTO;
import gov.nih.nci.hpc.web.model.HpcSaveSearch;
import gov.nih.nci.hpc.web.model.HpcTask;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Controller to get list of running tasks for the user. This list is displayed on
 * Task board
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/uploadTasksList")
public class HpcUploadTasksListController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.download}")
	private String queryServiceURL;

	/**
	 * GET action to query user saved searches
	 * 
	 * @param search
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@JsonView(Views.Public.class)
	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public List<HpcTask> get(@Valid @ModelAttribute("hpcSaveSearch") HpcSaveSearch search, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		List<HpcTask> result = new ArrayList<HpcTask>();
		try {
			HpcDownloadSummaryDTO downloads = HpcClientUtil.getDownloadSummary(authToken, queryServiceURL,
					sslCertPath, sslCertPassword);
			
				SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm");
				if(downloads.getActiveTasks() != null && !downloads.getActiveTasks().isEmpty())
				for (HpcUserDownloadRequest download : downloads.getActiveTasks()) {
					HpcTask task = new HpcTask();
					task.setTaskId(download.getTaskId());
					task.setPath(download.getPath());
					task.setType(download.getType().name());
					task.setCreated(download.getCreated() != null ? format.format(download.getCreated().getTime()) : "");
					task.setCompleted(download.getCompleted() != null ? format.format(download.getCompleted().getTime()) : "");
					String transferResult = "In Process";
					if(download.getResult() != null)
					{
						if(download.getResult())
							transferResult = "Completed";
						else
							transferResult = "Failed";
					}
					task.setResult(transferResult);
					result.add(task);
			}
				for (HpcUserDownloadRequest download : downloads.getCompletedTasks()) {
					HpcTask task = new HpcTask();
					task.setTaskId(download.getTaskId());
					task.setPath(download.getPath());
					task.setType(download.getType().name());
					task.setCreated(download.getCreated() != null ? format.format(download.getCreated().getTime()) : "");
					task.setCompleted(download.getCompleted() != null ? format.format(download.getCompleted().getTime()) : "");
					String transferResult = "In Process";
					if(download.getResult() != null)
					{
						if(download.getResult())
							transferResult = "Completed";
						else
							transferResult = "Failed";
					}
					task.setResult(transferResult);
					result.add(task);
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return result;
		}
	}
}
