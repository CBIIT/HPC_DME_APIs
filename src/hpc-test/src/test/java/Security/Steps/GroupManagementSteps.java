package gov.nih.nci.hpc.test.Security.Steps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import gov.nih.nci.hpc.test.common.TaskHelper;
import gov.nih.nci.hpc.test.dataProviders.ConfigFileReader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMemberResponse;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersResponseDTO;
import gov.nih.nci.hpc.test.common.UserRole;
import gov.nih.nci.hpc.test.common.ErrorMessage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;

public class GroupManagementSteps {
	Gson gson = new Gson();
	ConfigFileReader configFileReader = new ConfigFileReader();
	TaskHelper taskHelper = new TaskHelper();
	static final String CREATE_GROUP_URL = "/hpc-server/group";
	static final String REQUEST_AUTHENTICATION_FAILED = "REQUEST_AUTHENTICATION_FAILED";
	boolean firstRegistration = true;
	boolean testSuccess = false;
	String testSuccessString = "success";
	String testFailedString = "failure";
	String createGroupUrl;
	int statusCode;
	ErrorMessage result;
	String role;

	// The Logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	HpcGroupMembersRequestDTO dto = new HpcGroupMembersRequestDTO();

	@Given("I want to create a group named {string} in a {string} role")
	public void i_want_to_create_a_group_named_in_a_role(String groupNameString, String roleString) {
		createGroupUrl = CREATE_GROUP_URL + "/" + groupNameString;
		System.out.println("roleString=" + roleString);
		role = getRole(roleString);
	}

	@Given("I add users to the group")
	public void i_add_users_to_the_group(io.cucumber.datatable.DataTable dataTable) {
		dto.getAddUserIds().addAll(dataTable.rows(1).asList()); // ignore the head of column
	}

	@When("I click create group")
	public void i_click_create_group() {
		try {
			result = taskHelper.submitRequest("PUT", gson.toJson(dto), createGroupUrl, role);
		} catch (Exception e) {
			String errMsg = "Failed to Create Group. Reason: " + e.getMessage();
			System.out.println(errMsg);
		}
	}

	@Then("I verify the status of {string} in group creation")
	public void i_verify_the_status_of_in_group_creation(String expectedResponseString) {
		taskHelper.verifyResponse(expectedResponseString, result);
	}

	// Update Group Steps
	@Given("I want to update a group named {string} in a {string} role")
	public void i_want_to_update_a_group_named_in_a_role(String groupNameString, String roleString) {
		createGroupUrl = CREATE_GROUP_URL + "/" + groupNameString;
		role = getRole(roleString);
	}

	@Given("I delete users from the group")
	public void i_delete_users_from_the_group(io.cucumber.datatable.DataTable dataTable) {
		dto.getDeleteUserIds().addAll(dataTable.rows(1).asList()); // ignore the head of column
	}

	@When("I click update group")
	public void i_click_update_group() {
		result = taskHelper.submitRequest("POST", gson.toJson(dto), createGroupUrl, role);
	}

	@Then("I verify the status of {string} of updating a group")
	public void i_verify_the_status_of_of_updating_a_group(String expectedResponseString) {
		taskHelper.verifyResponse(expectedResponseString, result);
	}

	// Search Group(s)
	@Given("I want to search a group named {string} in a {string} role")
	public void i_want_to_search_a_group_named_in_a_role(String groupSearchString, String roleString) {
		createGroupUrl = CREATE_GROUP_URL + "?groupPattern=" + groupSearchString;
		role = getRole(roleString);
	}

	@When("I click search group")
	public void i_click_search_group() {
		result = taskHelper.submitRequest("GET", gson.toJson(dto), createGroupUrl, role);
	}

	@Then("I verify the status of {string} in searching the group")
	public void i_verify_the_status_of_in_searching_the_group(String expectedResponseString) {
		taskHelper.verifyResponse(expectedResponseString, result);
	}

	// Get Group Steps
	@Given("I want to get a group named {string} in a {string} role")
	public void i_want_to_get_a_group_named_in_a_role(String groupNameString,  String roleString) {
		createGroupUrl = CREATE_GROUP_URL + "/" + groupNameString;
		role = getRole(roleString);
	}

	@When("I click get group")
	public void i_click_get_group() {
		result = taskHelper.submitRequest("GET", gson.toJson(dto), createGroupUrl, role);
	}

	@Then("I verify the status of {string} in getting the group and its users")
	public void i_verify_the_status_of_in_getting_the_group_and_its_users(String expectedResponseString) {
		taskHelper.verifyResponse(expectedResponseString, result);
	}

	// Delete Group Steps
	@Given("I want to delete a group named {string} in a {string} role")
	public void i_want_to_delete_a_group_named_in_a_role(String groupNameString, String roleString) {
		createGroupUrl = CREATE_GROUP_URL + "/" + groupNameString;
		System.out.println("roleString=" + roleString);
		role = getRole(roleString);
	}

	@When("I click delete group")
	public void i_click_delete_group() {
		try {
			result = taskHelper.submitRequest("DELETE", gson.toJson(dto), createGroupUrl, role);
		} catch (Exception e) {
			String errMsg = "Failed to Delete Group. Reason: " + e.getMessage();
			System.out.println(errMsg);
		}
	}

	@Then("I verify the status of {string} in group deletion")
	public void i_verify_the_status_of_in_group_deletion(String expectedResponseString) {
		taskHelper.verifyResponse(expectedResponseString, result);
	}

	private String getRole(String roleString) {
		if(roleString.equals("System Admin")) {
			role = UserRole.SYSTEM_ADMIN_ROLE;
		} else if(roleString.equals("Group Admin")) {
			role = UserRole.GROUP_ADMIN_ROLE;
		} else if(roleString.equals("User")) {
			role = UserRole.USER_ROLE;
		} else {
			role = "Unknown";
		}
		return role;
	}
}
