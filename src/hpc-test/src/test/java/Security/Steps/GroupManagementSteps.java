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

	/* @Given("I want to create a group named {string} in a System Admin role")
	public void i_want_to_create_a_group_named_in_a_system_admin_role(String groupNameString) {
		createGroupUrl = CREATE_GROUP_URL + "/" + groupNameString;
		role = UserRole.SYSTEM_ADMIN_ROLE;
	}

	@Given("I want to create a group named {string} in a Group Admin role")
	public void i_want_to_create_a_group_named_in_a_group_admin_role(String groupNameString) {
		createGroupUrl = CREATE_GROUP_URL + "/" + groupNameString;
		role = UserRole.GROUP_ADMIN_ROLE;
	}

	@Given("I want to create a group named {string} in a User role")
	public void i_want_to_create_a_group_named_in_a_user_role(String groupNameString) {
		createGroupUrl = CREATE_GROUP_URL + "/" + groupNameString;
		role = UserRole.USER_ROLE;
	}*/

	@Given("I add users to the group")
	public void i_add_users_to_the_group(io.cucumber.datatable.DataTable dataTable) {
		dto.getAddUserIds().addAll(dataTable.rows(1).asList()); // ignore the head of column
	}
	@Given("I click create group")
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
		verifyResponse(expectedResponseString);
	}

	// Update Group Steps
	@Given("I want to update a group named {string}")
	public void i_want_to_update_a_group_named(String groupNameString) {
		createGroupUrl = CREATE_GROUP_URL + "/" + groupNameString;
	}
	@Given("I delete users from the group")
	public void i_delete_users_from_the_group(io.cucumber.datatable.DataTable dataTable) {
		dto.getDeleteUserIds().addAll(dataTable.rows(1).asList()); // ignore the head of column
	}

	@Given("I click update group")
	public void i_click_update_group() {
	    // Write code here that turns the phrase above into concrete actions
		result = taskHelper.submitRequest("POST", gson.toJson(dto), createGroupUrl, role);
	}
	@Then("I verify the status of success of updating a group")
	public void i_verify_the_status_of_success_of_updating_a_group() {
		if(!result.equals(testSuccessString)) {
			Assert.assertEquals((Object) testSuccessString, (Object) result);
		}
	}
	
	// Search Group Steps
	@Given("I want to search a group named {string}")
	public void i_want_to_search_a_group_named(String groupSearchString) {
		createGroupUrl = CREATE_GROUP_URL + "?groupPattern=" + groupSearchString;
		System.out.println(createGroupUrl);
	}
	@Then("I verify the status of success in searching the group")
	public void i_verify_the_status_of_success_in_searching_the_group() {
		testSuccess = taskHelper.submitRequestBoolean("GET", gson.toJson(dto), createGroupUrl);
		System.out.println(testSuccess);
	}

	// Get Group Steps
	@Given("I want to get a group named {string}")
	public void i_want_to_get_a_group_named(String groupNameString) {
		createGroupUrl = CREATE_GROUP_URL + "/" + groupNameString;
	}

	@Then("I verify the status of success in getting the group and its users")
	public void i_verify_the_status_of_success_in_getting_the_group_and_its_users() {
		testSuccess = taskHelper.submitRequestBoolean("GET", gson.toJson(dto), createGroupUrl);
		System.out.println(testSuccess);
	}

	// Delete Group Steps
	@Given("I want to delete a group named {string} in a System Admin role")
	public void i_want_to_delete_a_group_named_in_a_system_admin_role(String groupNameString) {
		createGroupUrl = CREATE_GROUP_URL + "/" + groupNameString;
		role = UserRole.SYSTEM_ADMIN_ROLE;
		result = taskHelper.submitRequest("DELETE", gson.toJson(dto), createGroupUrl, role);
	}

	@Given("I want to delete a group named {string} in a Group Admin role")
	public void i_want_to_delete_a_group_named_in_a_group_admin_role(String groupNameString) {
	    // Write code here that turns the phrase above into concrete actions
		createGroupUrl = CREATE_GROUP_URL + "/" + groupNameString;
		role = UserRole.GROUP_ADMIN_ROLE;
		result = taskHelper.submitRequest("DELETE", gson.toJson(dto), createGroupUrl, role);
	}


	/*@Given("I want to delete a group named {string}")
	public void i_want_to_delete_a_group_named(String groupNameString) {
		createGroupUrl = CREATE_GROUP_URL + "/" + groupNameString;
		result = taskHelper.submitRequest("DELETE", gson.toJson(dto), createGroupUrl, role);
	}*/

	@Then("I verify the status of success in group deletion")
	public void i_verify_the_status_of_success_in_group_deletion() {
		String responseStr = result.errorType;
		if(!responseStr.equals(testSuccessString)) {
			String error = result.stackTrace;
			Assert.assertEquals((Object) testSuccessString, (Object) error );
		}
	}

	@Then("I verify the status of failure of actions in a User role")
	public void i_verify_the_status_of_failure_of_actions_in_a_user_role() {
	    // Write code here that turns the phrase above into concrete actions
	    throw new io.cucumber.java.PendingException();
	}

	@Given("I want to delete a group named {string} in a {string} role")
	public void i_want_to_delete_a_group_named_in_a_role(String groupNameString, String roleString) {
		createGroupUrl = CREATE_GROUP_URL + "/" + groupNameString;
		System.out.println("roleString=" + roleString);
		role = getRole(roleString);
		result = taskHelper.submitRequest("DELETE", gson.toJson(dto), createGroupUrl, role);
	}

	@Then("I verify the status of {string} in group deletion")
	public void i_verify_the_status_of_in_group_deletion(String expectedResponseString) {
		verifyResponse(expectedResponseString);
	}

	private void verifyResponse(String expectedResponseString) {
		String responseType = result.errorType;
		if (expectedResponseString.equals(testSuccessString)) {
			if (!responseType.equals(testSuccessString)) {
				if (responseType.equals("REQUEST_REJECTED")) {
					String msg = responseType + "; " + result.message;
					Assert.assertEquals((Object) testSuccessString, (Object) msg);
				} else {
					Assert.assertEquals((Object) testSuccessString, (Object) result.stackTrace);
				}
			} else {
				Assert.assertEquals((Object) testSuccessString, (Object) responseType);
			}
		} else {
			String errorType = "REQUEST_AUTHENTICATION_FAILED";
			if (responseType.equals("REQUEST_AUTHENTICATION_FAILED")) {
				Assert.assertEquals((Object) responseType, (Object) "REQUEST_AUTHENTICATION_FAILED");
			} else {
				Assert.assertEquals((Object) responseType, (Object) result.stackTrace);
			}
		}
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
