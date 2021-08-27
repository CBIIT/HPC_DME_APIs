package Register.Steps;

import static io.restassured.RestAssured.when;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

import Register.Pojo.ParentMetadataPojo;
import Register.Pojo.RegisterPojo;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
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
	String sourcePath;
	String destinationPath;
	String tokenStr = "eyJhbGciOiJIUzI1NiJ9.eyJEYXRhTWFuYWdlbWVudEFjY291bnQiOiJ7XCJwYXNzd29yZFwiOlwiZGVhcmRhc2hzcGFya3l0YXJzXCIsXCJpbnRlZ3JhdGVkU3lzdGVtXCI6XCJJUk9EU1wiLFwicHJvcGVydGllc1wiOntcIlBST1hZX05BTUVcIjpcIlwiLFwiUE9SVFwiOlwiMTI0N1wiLFwiREVGQVVMVF9TVE9SQUdFX1JFU09VUkNFXCI6XCJkZW1vUmVzY1wiLFwiSE9NRV9ESVJFQ1RPUllcIjpcIlwiLFwiWk9ORVwiOlwidGVtcFpvbmVcIixcIkhPU1RcIjpcImZzZG1lbC1pcm9kczAxZC5uY2lmY3JmLmdvdlwiLFwiUFJPWFlfWk9ORVwiOlwiXCJ9LFwidXNlcm5hbWVcIjpcInNjaGludGFsXCJ9IiwiVXNlck5hbWUiOiJzY2hpbnRhbCIsIkRhdGFNYW5hZ2VtZW50QWNjb3VudEV4cGlyYXRpb24iOjE2MjcwMTE2MjQyNTYsImV4cCI6MTgwNjk4MjgyNH0.fjnw6bvqcsGtHBLBhDMCF_dTOVV2F_0HCgH4pM5RjsM";

	@Given("I am a valid user with token")
	public void i_am_a_valid_user_with_token() {
	    
	}
	
	@Given("I add source_path as {string}")
	public void i_add_source_path_as(String source_path) {
	    this.sourcePath = source_path;
	}

	@Given("I add destination_path as {string}")
	public void i_add_destination_path_as(String destination_path) {
	    this.destinationPath = destination_path;
	}

	@Given("I add base_path as {string}")
	public void i_add_base_path_as(String base_path) {
	    //request.setCallerObjectId(base_path);
		registerBody.setBasePath(base_path);
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
		//registerBody.setParentCollectionsBulkMetadataEntries(p);
	}
	
	
	@When("I click Register")
	public void i_click_register() {
	    
		String token = this.tokenStr;
		RestAssured.baseURI = "https://localhost/";
	    RestAssured.port = 7738;
	    RequestSpecification request = RestAssured.given().relaxedHTTPSValidation();
		request.header("Accept", "application/json");
		request.header("Authorization", "Bearer "+ token);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String registerBodyJson = gson.toJson(registerBody);
		System.out.println("Final JSON Body");
		System.out.println(registerBodyJson);
		// Setting multipart fields in the request
		System.out.println(this.sourcePath);
		System.out.println(this.destinationPath);
		request.multiPart("dataObjectRegistration",registerBodyJson, "application/json");
		request.multiPart("dataObject", new File(this.sourcePath), "application/octet-stream"); 
		
		Response response = request.put("/hpc-server/v2/dataObject"+ this.destinationPath);
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
