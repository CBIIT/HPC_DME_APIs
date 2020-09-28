/**
 * HpcUpdateGroupController.java
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import gov.nih.nci.hpc.dto.security.HpcGroup;
import gov.nih.nci.hpc.dto.security.HpcGroupListDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMemberResponse;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcWebGroup;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Controller to update and delete a group. Users can be added or removed from
 * the group
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/updategroup")
public class HpcUpdateGroupController extends AbstractHpcController {

	@Value("${gov.nih.nci.hpc.server.group}")
	private String groupServiceURL;

	/**
	 * Populate data for Update Group page
	 * 
	 * @param q
	 * @param groupName
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
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
			return "redirect:/login?returnPath=updategroup";
		}
		session.removeAttribute("updategroup");
		initialize(model, authToken, groupName, session);
		return "updategroup";
	}

	/**
	 * POST action to update or delete a GROUP. Updating Group name is not
	 * allowed. Once the group is updated, redirect back to update group page
	 * with resulted message.
	 * 
	 * @param hpcWebGroup
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String updateGroup(@Valid @ModelAttribute("hpcGroup") HpcWebGroup hpcWebGroup, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request, HttpServletResponse response,
			final RedirectAttributes redirectAttributes) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		List<String> messages = new ArrayList<String>();
		try {
			if (hpcWebGroup.getActionType() == null || hpcWebGroup.getActionType().trim().length() == 0
					|| hpcWebGroup.getActionType().endsWith("cancel")) {
				redirectAttributes.addFlashAttribute("return", "true");
				return "redirect:group?return=true";
			} else if (hpcWebGroup.getActionType().endsWith("delete")) {
				try {
					boolean deleted = HpcClientUtil.deleteGroup(authToken, groupServiceURL, hpcWebGroup.getGroupId(),
							sslCertPath, sslCertPassword);
					if (deleted) {
						messages.add("Successfully deleted group: " + hpcWebGroup.getGroupId());
						model.addAttribute("messages", messages);
						hpcWebGroup.setGroupName("");
						model.addAttribute("hpcWebGroup", hpcWebGroup);
						return "managegroup";
					}
				} catch (HpcWebException e) {
					messages.add(e.getMessage());
					model.addAttribute("messages", messages);
				}
			} else if (hpcWebGroup.getActionType().endsWith("update")) {
				if (hpcWebGroup.getGroupId() == null || hpcWebGroup.getGroupId().trim().length() == 0) {
					messages.add("Invald user input");
					model.addAttribute("messages", messages);
					return "updategroup";
				}

				HpcGroupMembersRequestDTO dto = constructRequest(request, session, hpcWebGroup.getGroupId());

				HpcGroupMembersResponseDTO updateResponse = HpcClientUtil.updateGroup(authToken, groupServiceURL, dto,
						hpcWebGroup.getGroupId(), sslCertPath, sslCertPassword);
				boolean success = constructReponseMessages(updateResponse, model);
				if (success)
					session.removeAttribute("selectedUsers");
			}
		} catch (Exception e) {
			messages.add(e.getMessage());
			model.addAttribute("messages", messages);
		} finally {
			model.addAttribute("hpcWebGroup", hpcWebGroup);
			initialize(model, authToken, hpcWebGroup.getGroupName(), session);
		}
		return "updategroup";
	}

	private boolean constructReponseMessages(HpcGroupMembersResponseDTO updateResponse, Model model) {
		if (updateResponse == null)
			return false;
		List<String> messages = new ArrayList<String>();
		boolean success = true;
		if (updateResponse.getAddGroupMemberResponses() != null) {
			for (HpcGroupMemberResponse response : updateResponse.getAddGroupMemberResponses()) {
				if (response.getResult())
					messages.add("Adding " + response.getUserId() + ": Successful");
				else {
					messages.add("Adding " + response.getUserId() + ": failed due to " + response.getMessage());
					success = false;
				}
			}
		}
		if (updateResponse.getDeleteGroupMemberResponses() != null) {
			for (HpcGroupMemberResponse response : updateResponse.getDeleteGroupMemberResponses()) {
				if (response.getResult())
					messages.add("Removing " + response.getUserId() + ": Successful");
				else {
					messages.add("Removing " + response.getUserId() + ": failed due to " + response.getMessage());
					success = false;
				}
			}
		}
		model.addAttribute("messages", messages);
		return success;
	}

	private HpcGroupMembersRequestDTO constructRequest(HttpServletRequest request, HttpSession session,
			String groupName) {
		HpcGroup group = (HpcGroup) session.getAttribute("updategroup");
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
				if (userId != null && userId[0].equalsIgnoreCase("on")) {
					submittedUsers.add(userName[0]);
					if (!group.getUserIds().contains(userName[0]))
						addusers.add(userName[0]);
				}
			}
		}
		if (addusers.size() > 0)
			dto.getAddUserIds().addAll(addusers);
		setRemoveUserId(dto, submittedUsers, session);
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

	private void initialize(Model model, String authToken, String groupName, HttpSession session) {
		HpcWebGroup webGroup = new HpcWebGroup();
		webGroup.setGroupName(groupName);
		model.addAttribute("hpcWebGroup", webGroup);
		model.addAttribute("assignedNames", new ArrayList<String>());
		String selectedUsers = (String) session.getAttribute("selectedUsers");
		model.addAttribute("selectedUsers", selectedUsers);
		if (groupName != null && groupName.length() > 0) {
			HpcGroupListDTO groupList = HpcClientUtil.getGroups(authToken, groupServiceURL, groupName, sslCertPath,
					sslCertPassword);
			if (groupList == null || groupList.getGroups() == null || groupList.getGroups().isEmpty()) {
				model.addAttribute("message", "Group " + groupName + " not found");
				return;
			}

			for (HpcGroup group : groupList.getGroups()) {
				if (group.getGroupName().equals(groupName)) {
					model.addAttribute("group", group);
					session.setAttribute("updategroup", group);
					break;
				}
			}
		}
	}
}
