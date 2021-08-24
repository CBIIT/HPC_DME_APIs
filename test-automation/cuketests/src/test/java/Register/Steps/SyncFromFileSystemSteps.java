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


public class SyncFromFileSystemSteps {
	
	RegisterPojo registerBody = new RegisterPojo();
	String datafile;
	String tokenStr = "xyz";

	@Given("I am a valid user with token")
	public void i_am_a_valid_user_with_token() {
	    // Write code here that turns the phrase above into concrete actions
	    //throw new io.cucumber.java.PendingException();
		//registerBody = new RegisterPojo();
	}

	@Given("I add base_path as {string}")
	public void i_add_base_path_as(String base_path) {
	    //request.setCallerObjectId(base_path);
		registerBody.setCallerObjectId(base_path);
	}

	@Given("I add collection_type as {string}")
	public void i_add_collection_type_as(String collection_type) {
	}

	@Given("I add a checksum of {string}")
	public void i_add_a_checksum_of(String checksum) {
	    registerBody.setChecksum(checksum);
	}
	
	@Given("I add data_file_path as {string}")
	public void i_add_data_file_path_as(String datafile) {
	    this.datafile = datafile;
	}

	@Given("I add metadataEntries as")
	public void i_add_metadata_entries_as(io.cucumber.datatable.DataTable dataTable) {
	    
		//List<List<String>> rawDetails = details.asLists(String.class);
	    //String path = rawDetails.get(1).get(0);
	    
	    List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
	    for (Map<String, String> columns : rows) {
	      String attribute = columns.get("attribute");
	      String val = columns.get("value");
	      System.out.println("attribute=" + attribute);
	      System.out.println("value=" + val);
	    }
	    registerBody.setMetadataEntries(rows);
	}
	
	@Given("I add defaultCollectionMetadataEntries as")
	public void i_add_defaultCollectionMetadataEntries_as(io.cucumber.datatable.DataTable dataTable) {
		List<Map<String, String>> tablerows = dataTable.asMaps(String.class, String.class);
		ParentMetadataPojo p = new ParentMetadataPojo();
		p.setDefaultCollectionMetadataEntries(tablerows);
		registerBody.setParentCollectionsBulkMetadataEntries(p);
	}
	
	
	@When("I click Register")
	public void i_click_register() {
	    // Write code here that turns the phrase above into concrete actions
	    String token = this.tokenStr;
		RestAssured.baseURI = "https://fsdmel-dsapi01d.ncifcrf.gov/";
		//RestAssured.baseURI = "https://localhost/";
	    RestAssured.port = 7738;
	    RequestSpecification request = RestAssured.given().relaxedHTTPSValidation();
		request.header("Accept", "application/json");
		request.header("Authorization", "Bearer "+ token);
		//request.header("Content-Type", "application/json");
		request.multiPart("file", new File(this.datafile));
	     Gson gson = new Gson();
		 String registerBodyJson = gson.toJson(registerBody);
		 System.out.println("Final JSON");
		 System.out.println(registerBodyJson);
	     
		Response response = request.body(registerBodyJson).put("hpc-server/v2/dataObject");
	    System.out.println(response.asString());
	    System.out.println(response.getBody());
	    System.out.println(response.getStatusCode());
	    
	    int statuscode = response.getStatusCode();
	    
	    
	}

	@Then("I get a response of success")
	public void i_get_a_response_of_success() {
	    // Write code here that turns the phrase above into concrete actions
	    //throw new io.cucumber.java.PendingException();
	}
	

}
