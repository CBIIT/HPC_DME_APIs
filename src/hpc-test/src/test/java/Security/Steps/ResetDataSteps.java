package gov.nih.nci.hpc.test.Security.Steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import com.google.gson.Gson;
import gov.nih.nci.hpc.test.common.TaskHelper;
import gov.nih.nci.hpc.test.common.UserRole;
import gov.nih.nci.hpc.test.common.ErrorMessage;

import gov.nih.nci.hpc.dto.security.HpcGroupMemberResponse;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersResponseDTO;


public class ResetDataSteps {
	TaskHelper taskHelper = new TaskHelper();
	Gson gson = new Gson();
	ErrorMessage result;
	static final String CREATE_GROUP_URL = "/hpc-server/group";
	
	@Given("I want to delete all data")
	public void i_want_to_delete_all_data() {
		// Delete Group before @createGroup
		result = taskHelper.submitRequest("DELETE", gson.toJson(new HpcGroupMembersRequestDTO()), CREATE_GROUP_URL + "/test_group_sa", UserRole.SYSTEM_ADMIN_ROLE);
		result = taskHelper.submitRequest("DELETE", gson.toJson(new HpcGroupMembersRequestDTO()), CREATE_GROUP_URL + "/test_group_ga", UserRole.SYSTEM_ADMIN_ROLE);
		result = taskHelper.submitRequest("DELETE", gson.toJson(new HpcGroupMembersRequestDTO()), CREATE_GROUP_URL + "/test_group_user", UserRole.SYSTEM_ADMIN_ROLE);
		result = taskHelper.submitRequest("DELETE", gson.toJson(new HpcGroupMembersRequestDTO()), CREATE_GROUP_URL + "/test_group_delete_me", UserRole.SYSTEM_ADMIN_ROLE);
	}	
}