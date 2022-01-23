package Register.Steps;

import static io.restassured.RestAssured.when;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;

import java.io.*;

import Register.Pojo.ParentMetadataPojo;
import Register.Pojo.RegisterPojo;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.messages.internal.com.google.gson.Gson;
import io.cucumber.datatable.DataTable;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import junit.framework.Assert;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;


public class GoogleCloudSteps {
	
	@Given("I am a valid gc_user with token")
	public void i_am_a_valid_gc_user_with_token() {
	}

	@Given("I add gc_base_path as {string}")
	public void i_add_gc_base_path_as(String string) {
	}

	@Given("I add gc_collection_type as {string}")
	public void i_add_gc_collection_type_as(String string) {
	}

	@Given("I add gc_checksum of {string}")
	public void i_add_gc_checksum_of(String string) {
	}

	@Given("I add gc_data_file_path as {string}")
	public void i_add_gc_data_file_path_as(String string) {
	}

	@Given("I add gc_metadataEntries as")
	public void i_add_gc_metadata_entries_as(io.cucumber.datatable.DataTable dataTable) {
	    
	}

	@Given("I add gc_defaultCollectionMetadataEntries as")
	public void i_add_gc_default_collection_metadata_entries_as(io.cucumber.datatable.DataTable dataTable) {
	    
	}

	@When("I click Register for the Google Cloud Upload")
	public void i_click_register_for_the_google_cloud_upload() {
	    
	}

	@Then("I get a response of success for the Google Cloud Upload")
	public void i_get_a_response_of_success_for_the_google_cloud_upload() {
	    
	}



	
}