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

import gov.nih.nci.hpc.dto.security.HpcGroupMemberResponse;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.dto.security.HpcUserRequestDTO;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


public class UserManagementSteps {
	Gson gson = new Gson();
	ConfigFileReader configFileReader = new ConfigFileReader();
	TaskHelper taskHelper = new TaskHelper();
	static final String CREATE_USER_URL = "/hpc-server/user";
	boolean firstRegistration = true;
	boolean testSuccess = false;
	String createUserUrl;
	int statusCode;
	HpcUserRequestDTO dto = new HpcUserRequestDTO();

	// The Logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	@Given("I want to create a user named {string}")
	public void i_want_to_create_a_user_named(String userNameString) {
		createUserUrl = CREATE_USER_URL + "/" + userNameString;
	}
	@Given("I want to assign a role of {string}")
	public void i_want_to_assign_a_role_of(String userRole) {
		dto.setUserRole(userRole);
		dto.setDoc("FNLCR");
		dto.setLastName("NoLastName");
		dto.setFirstName("NoFirstName");
	}
	@When("I create the user")
	public void i_create_the_user() {
		testSuccess = taskHelper.submitRequestBoolean("PUT", gson.toJson(dto), createUserUrl);
	}
	@Then("I verify the status of success in user creation")
	public void i_verify_the_status_of_success_in_user_creation() {
		org.junit.Assert.assertTrue(testSuccess==true);
	}
}