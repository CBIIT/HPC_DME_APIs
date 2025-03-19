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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcUserDownloadRequest;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadSummaryDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcTask;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcIdentityUtil;
import gov.nih.nci.hpc.web.util.HpcSearchUtil;

/**
 * <p>
 * HPC Web Dashboard controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDashBoardController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/downloadtasks")
public class HpcDownloadTaskBoardController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionURL;
	@Value("${gov.nih.nci.hpc.server.query}")
	private String queryURL;
	@Value("${gov.nih.nci.hpc.server.download}")
	private String queryServiceURL;
	@Value("${gov.nih.nci.hpc.server.download.all}")
	private String queryAllServiceURL;
	
	private Logger logger = LoggerFactory.getLogger(HpcCreateCollectionDataFileController.class);

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
	public String home(@RequestBody(required = false) String q,  @RequestParam(required = false) String queryAll, @RequestParam(required = false) String pageSize, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		model.addAttribute("queryURL", queryURL);
		model.addAttribute("collectionURL", collectionURL);
		boolean canQueryAll = HpcIdentityUtil.iUserSystemAdminOrGroupAdmin(session);
		boolean queryAllOption = (queryAll == null || queryAll.contentEquals("true")) && canQueryAll ? true : false;
		model.addAttribute("queryAll", queryAllOption);
		model.addAttribute("canQueryAll", canQueryAll);
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
			if(pageStr != null && !pageStr.isEmpty()){
				page = Integer.parseInt(pageStr);
			}
			if(pageSize == null) {
				pageSize="200";
			}
			int ipageSize =  Integer.valueOf(pageSize);
			String serviceURL = (queryAllOption == false ? queryServiceURL : queryAllServiceURL) + "?page=" + page + "&totalCount=true" + "&pageSize=" + ipageSize;
			logger.info("serviceURL = " + serviceURL);
			HpcDownloadSummaryDTO downloads = HpcClientUtil.getDownloadSummary(authToken, serviceURL, sslCertPath,
					sslCertPassword);
			logger.info("downloads retrieved size=" + downloads.getCompletedTasks().size());
			SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm");
			SimpleDateFormat sortFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
			if (downloads.getActiveTasks() != null && !downloads.getActiveTasks().isEmpty())
				for (HpcUserDownloadRequest download : downloads.getActiveTasks()) {
					HpcTask task = new HpcTask();
					task.setUserId(download.getUserId());
					task.setTaskId(download.getTaskId());
					task.setPath(download.getPath());
					task.setType(download.getType().name());
					task.setDestinationType(download.getDestinationType() != null ? download.getDestinationType().name() : "");
					task.setCreated(
							download.getCreated() != null ? format.format(download.getCreated().getTime()) : "");
					task.setSortCreated(
							download.getCreated() != null ? sortFormat.format(download.getCreated().getTime()) : "");
					task.setCompleted(
							download.getCompleted() != null ? format.format(download.getCompleted().getTime()) : "");
					task.setSortCompleted(
							download.getCompleted() != null ? sortFormat.format(download.getCompleted().getTime()) : "");
					task.setResult(getResultDisplayText(download.getResult()));
					task.setRetryUserId(download.getRetryUserId() != null ? download.getRetryUserId() : "");
					task.setDisplayPath(download.getPath()); // For display purpose only. The above path gets modified to a link
					if(download.getStatus() != null) {
					    task.setStatus(download.getStatus().value());
					}
					result.add(task);
				}
			for (HpcUserDownloadRequest download : downloads.getCompletedTasks()) {
				HpcTask task = new HpcTask();
				if(!download.getResult().equals(HpcDownloadResult.COMPLETED)) {
					if(CollectionUtils.isNotEmpty(download.getItems())) {
						HpcCollectionDownloadTaskItem item = download.getItems().stream()
			            .filter(t -> !t.getResult().equals(HpcDownloadResult.COMPLETED))
			            .findFirst()
			            .orElse(null);
						if(item != null)
							task.setError(item.getMessage() != null ? item.getMessage().replaceAll("'", "\\\\'") : "");
						else
							task.setError(download.getMessage() != null? download.getMessage().replaceAll("'", "\\\\'") : "");
					} else {
						task.setError(download.getMessage() != null ? download.getMessage().replaceAll("'", "\\\\'") : "");
					}
				}
	
				task.setUserId(download.getUserId());
				task.setTaskId(download.getTaskId());
				task.setPath(download.getPath());
				task.setType(download.getType().name());
				task.setDestinationType(download.getDestinationType() != null ? download.getDestinationType().name() : "");
				task.setCreated(download.getCreated() != null ? format.format(download.getCreated().getTime()) : "");
				task.setCompleted(
						download.getCompleted() != null ? format.format(download.getCompleted().getTime()) : "");
				task.setSortCreated(
						download.getCreated() != null ? sortFormat.format(download.getCreated().getTime()) : "");
				task.setSortCompleted(
						download.getCompleted() != null ? sortFormat.format(download.getCompleted().getTime()) : "");task.setResult(getResultDisplayText(download.getResult()));
				task.setRetryUserId(download.getRetryUserId() != null ? download.getRetryUserId() : "");
				task.setDisplayPath(download.getPath());// For display purpose only. The above path gets modified to a link in the display
				
				if(download.getResult() != null) {
					task.setStatus(download.getResult().value());
				}
				
				result.add(task);
			}
			
			
			model.addAttribute("currentPage", Integer.toString(page));
			model.addAttribute("totalCount", downloads.getTotalCount());
			model.addAttribute("totalPages", HpcSearchUtil.getTotalPages(downloads.getTotalCount(), downloads.getLimit()));
			model.addAttribute("currentPageSize", result.size());
			model.addAttribute("results", result);
			model.addAttribute("pageSize", pageSize);
		} catch (Exception e) {
			model.addAttribute("error", "Failed to get download tasks: "+e.getMessage());
		}
		logger.info("all size=" + result.size());
		return "downloadtaskboard";
	}
	
	private String getResultDisplayText(HpcDownloadResult result) {
	  if(result == null) {
	    return "In Process";
	  }
	  
      switch(result) {
        case COMPLETED:
          return "Completed";
        
        case CANCELED:
          return "Canceled";
          
        default:
          return "Failed";
      }
	}
}
