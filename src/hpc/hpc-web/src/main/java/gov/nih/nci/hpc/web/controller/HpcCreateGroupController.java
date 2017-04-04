/**
 * HpcSearchProjectController.java
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import gov.nih.nci.hpc.dto.security.HpcGroup;
import gov.nih.nci.hpc.dto.security.HpcGroupMemberResponse;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcWebGroup;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * HPC DM Project Search controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDataRegistrationController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/creategroup")
public class HpcCreateGroupController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.group}")
	private String groupServiceURL;
	@Value("${gov.nih.nci.hpc.server.docs}")
	private String docsServiceURL;

	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (user == null || authToken == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}
		initialize(model, authToken, user, session);
		return "creategroup";
	}

	private void initialize(Model model, String authToken, HpcUserDTO user, HttpSession session) {
		HpcWebGroup webGroup = new HpcWebGroup();
		model.addAttribute("hpcWebGroup", webGroup);
		model.addAttribute("assignedNames", new ArrayList<String>());
		String selectedUsers = (String) session.getAttribute("selectedUsers");
		model.addAttribute("selectedUsers", selectedUsers);
	}

	/*
	 * Action for User registration
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String createGroup(@Valid @ModelAttribute("hpcGroup") HpcWebGroup hpcWebGroup, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response, final RedirectAttributes redirectAttributes) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (authToken == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}

		try {
			if (hpcWebGroup.getActionType() != null && hpcWebGroup.getActionType().endsWith("cancel"))
			{
				redirectAttributes.addFlashAttribute("return", "true");
				return "redirect:group?return=true";
			}
			
			if (hpcWebGroup.getGroupName() == null || hpcWebGroup.getGroupName().trim().length() == 0)
				model.addAttribute("message", "Invald user input");

			HpcGroupMembersRequestDTO dto = constructRequest(request, hpcWebGroup.getGroupName());

			HpcGroupMembersResponseDTO createResponse = HpcClientUtil.createGroup(authToken, groupServiceURL, dto, hpcWebGroup.getGroupName(),
					sslCertPath, sslCertPassword);
			boolean success = constructReponseMessages(createResponse, model);
			if(success)
				session.removeAttribute("selectedUsers");
		} catch (Exception e) {
			model.addAttribute("message", "Failed to create group: " + e.getMessage());
		} finally {
			model.addAttribute("hpcWebGroup", hpcWebGroup);
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			initialize(model, authToken, user, session);
		}
		return "creategroup";
	}

	private boolean constructReponseMessages(HpcGroupMembersResponseDTO updateResponse, Model model)
	{
		if(updateResponse == null)
			return false;
		List<String> messages = new ArrayList<String>();
		boolean success = true;
		if(updateResponse.getAddGroupMemberResponses() != null)
		{
			for(HpcGroupMemberResponse response : updateResponse.getAddGroupMemberResponses())
			{
				if(response.getResult())
					messages.add("Adding " + response.getUserId()+ ": Successful");
				else
				{
					messages.add("Adding " + response.getUserId()+ ": failed due to " + response.getMessage());
					success = false;
				}
			}
		}
		if(updateResponse.getDeleteGroupMemberResponses() != null)
		{
			for(HpcGroupMemberResponse response : updateResponse.getDeleteGroupMemberResponses())
			{
				if(response.getResult())
					messages.add("Removing " + response.getUserId()+ ": Successful");
				else
				{
					messages.add("Removing " + response.getUserId()+ ": failed due to " + response.getMessage());
					success = false;
				}
			}
		}
		model.addAttribute("messages", messages);
		return success;
	}
	
	private HpcGroupMembersRequestDTO constructRequest(HttpServletRequest request,
			String groupName) {
		Enumeration<String> params = request.getParameterNames();
		HpcGroupMembersRequestDTO dto = new HpcGroupMembersRequestDTO();
		List<String> addusers = new ArrayList<String>();
		List<String> submittedUsers = new ArrayList<String>();
		while (params.hasMoreElements()) {
			String paramName = params.nextElement();
			if (paramName.startsWith("userId")) {
				String index = paramName.substring("userId".length());
				String[] userId = request.getParameterValues("userId" + index);
				String[] userName = request.getParameterValues("userName" + index);
				if(userId != null && userId[0].equalsIgnoreCase("on"))
					submittedUsers.add(userName[0]);
			}
		}
		if (addusers.size() > 0)
			dto.getAddUserIds().addAll(addusers);
		return dto;
	}
}
