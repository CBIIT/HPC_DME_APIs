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
import common.JsonHelper;
import dataProviders.ConfigFileReader;


public class SyncFromFileSystemSteps {
    
    ConfigFileReader configFileReader;
	RegisterPojo registerBody = new RegisterPojo();
	String datafile;
	String sourcePath;
	String destinationPath;
	int statusCode;
	
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
	    
	    configFileReader= new ConfigFileReader();
        String token = configFileReader.getToken();
		String registerBodyJson = new JsonHelper().getPrettyJson((Object)registerBody);
		RestAssured.baseURI = "https://fsdmel-dsapi01d.ncifcrf.gov/";
	    RestAssured.port = 7738;
	    RequestSpecification request = RestAssured.given().
			relaxedHTTPSValidation().
			header("Accept", "application/json").
			header("Authorization", "Bearer "+ token).	
			multiPart("dataObjectRegistration",registerBodyJson, "application/json").
			multiPart("dataObject", new File(this.sourcePath), "application/octet-stream"); 
		
		Response response = request.put("/hpc-server/v2/dataObject"+ this.destinationPath);
	    //System.out.println(response.asString());
	    //System.out.println(response.getBody());
	   
	    
	    this.statusCode = response.getStatusCode();
	    if (this.statusCode == 200 || this.statusCode == 201) {
	      System.out.println("This test was a success");
		  System.out.println("StatusCode = " + response.getStatusCode());
	    } else {
	      System.out.println("This test was a failure");
	      System.out.println("StatusCode = " + response.getStatusCode());
	    }
	   
	}

	@Then("I get a response of success")
	public void i_get_a_response_of_success() {
	    org.junit.Assert.assertEquals(200, this.statusCode);
	}
	

}
