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
import org.springframework.web.bind.annotation.RequestParam;

import gov.nih.nci.hpc.dto.security.HpcGroup;
import gov.nih.nci.hpc.dto.security.HpcGroupListDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersRequestDTO;
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
@RequestMapping("/updategroup")
public class HpcUpdateGroupController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.group}")
	private String groupServiceURL;
	@Value("${gov.nih.nci.hpc.server.docs}")
	private String docsServiceURL;

	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q, @RequestParam String groupName, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (user == null || authToken == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}
		session.removeAttribute("updategroup");
		initialize(model, authToken, groupName, session);
		return "updategroup";
	}

	private void initialize(Model model, String authToken, String groupName, HttpSession session) {
		HpcWebGroup webGroup = new HpcWebGroup();
		webGroup.setGroupName(groupName);
		model.addAttribute("hpcWebGroup", webGroup);
		model.addAttribute("assignedNames", new ArrayList<String>());
		String selectedUsers = (String) session.getAttribute("selectedUsers");
		model.addAttribute("selectedUsers", selectedUsers);
		HpcGroupListDTO groupList = HpcClientUtil.getGroups(authToken, groupServiceURL, groupName, sslCertPath,
				sslCertPassword);
		if (groupList == null || groupList.getGroups() == null || groupList.getGroups().isEmpty()) {
			model.addAttribute("message", "Group " + groupName + " not found");
			return;
		}
		HpcGroup group = groupList.getGroups().get(0);
		model.addAttribute("group", group);
		session.setAttribute("updategroup", group);
	}

	/*
	 * Action for User registration
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String updateGroup(@Valid @ModelAttribute("hpcGroup") HpcWebGroup hpcWebGroup, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		try {
			if (hpcWebGroup.getGroupId() == null || hpcWebGroup.getGroupId().trim().length() == 0)
				model.addAttribute("message", "Invald user input");

			HpcGroupMembersRequestDTO dto = constructRequest(request, session, hpcWebGroup.getGroupId());

			boolean created = HpcClientUtil.updateGroup(authToken, groupServiceURL, dto, hpcWebGroup.getGroupId(),
					sslCertPath, sslCertPassword);
			if (created) {
				model.addAttribute("message", "Group " + hpcWebGroup.getGroupName() + " is Updated!");
				session.removeAttribute("selectedUsers");
			}
		} catch (Exception e) {
			model.addAttribute("message", "Failed to update group: " + e.getMessage());
		} finally {
			model.addAttribute("hpcWebGroup", hpcWebGroup);
			initialize(model, authToken, hpcWebGroup.getGroupName(), session);
		}
		return "updategroup";
	}

	private HpcGroupMembersRequestDTO constructRequest(HttpServletRequest request, HttpSession session,
			String groupName) {
		Enumeration<String> params = request.getParameterNames();
		HpcGroupMembersRequestDTO dto = new HpcGroupMembersRequestDTO();
		List<String> addusers = new ArrayList<String>();
		List<String> deleteusers = new ArrayList<String>();
		while (params.hasMoreElements()) {
			String paramName = params.nextElement();
			if (paramName.startsWith("userId")) {
				String index = paramName.substring("userId".length());
				String[] userId = request.getParameterValues("userId" + index);
				String[] userName = request.getParameterValues("userName" + index);
				addusers.add(userName[0]);
			}
		}
		if (addusers.size() > 0)
			dto.getAddUserIds().addAll(addusers);
		setRemoveUserId(dto, addusers, session);
		return dto;
	}

	private void setRemoveUserId(HpcGroupMembersRequestDTO dto, List<String> addUsers, HttpSession session) {
		HpcGroup group = (HpcGroup) session.getAttribute("updategroup");
		List<String> removeUserIds = new ArrayList<String>();
		List<String> users = group.getUserIds();
		for (String userId : users) {
			if (!addUsers.contains(userId))
				removeUserIds.add(userId);
		}
		if (removeUserIds.size() > 0)
			dto.getDeleteUserIds().addAll(removeUserIds);
	}
}
