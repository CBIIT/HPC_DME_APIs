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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;

public class GroupManagementSteps {
	Gson gson = new Gson();
	ConfigFileReader configFileReader = new ConfigFileReader();
	TaskHelper taskHelper = new TaskHelper();
	static final String CREATE_GROUP_URL = "/hpc-server/group";
	boolean firstRegistration = true;
	boolean testSuccess = false;
	String testSuccessString = "success";
	String createGroupUrl;
	int statusCode;
	String result;
	String role;

	// The Logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	HpcGroupMembersRequestDTO dto = new HpcGroupMembersRequestDTO();
	
	@Given("I login as a System Admin")
	public void i_login_as_a_system_admin() {
		role = UserRole.SYSTEM_ADMIN_ROLE;
	}

	// Create Group Steps
	@Given("I want to create a group named {string}")
	public void i_want_to_create_a_group_named(String groupNameString) {
		createGroupUrl = CREATE_GROUP_URL + "/" + groupNameString;
	}

	@Given("I add users to the group")
	public void i_add_users_to_the_group(io.cucumber.datatable.DataTable dataTable) {
		dto.getAddUserIds().addAll(dataTable.rows(1).asList()); // ignore the head of column
	}
	@Given("I click create group")
	public void i_click_create_group() {
		try {
			result = taskHelper.submitRequest("PUT", gson.toJson(dto), createGroupUrl, role);
		} catch (Exception e) {
			String errMsg = "Failed to retrieve bookmarks. Reason: " + e.getMessage();
			System.out.println(errMsg);
		}
	}	
	
	@Then("I verify the status of success in group creation")
	public void i_verify_the_status_of_success_in_group_creation() {
		if(!result.equals(testSuccessString)) {
			Assert.assertEquals((Object) testSuccessString, (Object) result);
		}
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
	@Given("I want to delete a group named {string}")
	public void i_want_to_delete_a_group_named(String groupNameString) {
		createGroupUrl = CREATE_GROUP_URL + "/" + groupNameString;
		result = taskHelper.submitRequest("DELETE", gson.toJson(dto), createGroupUrl, role);
	}

	@Then("I verify the status of success in group deletion")
	public void i_verify_the_status_of_success_in_group_deletion() {
		if(!result.equals(testSuccessString)) {
			Assert.assertEquals((Object) testSuccessString, (Object) result);
		}
	}
	
}

