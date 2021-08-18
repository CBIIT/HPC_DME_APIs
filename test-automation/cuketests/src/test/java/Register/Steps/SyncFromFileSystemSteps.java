package Register.Steps;

import static io.restassured.RestAssured.when;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;

import java.io.*;  

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
	
	RegisterPojo registerBody;

	@Given("I am a valid user with token")
	public void i_am_a_valid_user_with_token() {
	    // Write code here that turns the phrase above into concrete actions
	    //throw new io.cucumber.java.PendingException();
		registerBody = new RegisterPojo();
	}

	@Given("I add base_path as {string}")
	public void i_add_base_path_as(String base_path) {
	    //request.setCallerObjectId(base_path);
		registerBody.setCallerObjectId(base_path);
	}

	@Given("I add collection_type as {string}")
	public void i_add_collection_type_as(String collection_type) {
		/*“parentCollectionsBulkMetadataEntries” :  {
		     “defaultCollectionMetadataEntries” : [
		              {
		      	    "attribute": "collection_type",
		      	    "value": "Folder"
		   	  }
			  ]
		   }*/
		
		//x = request.getParentCollectionsBulkMetadataEntries();
	
	}

	@Given("I add a checksum of {string}")
	public void i_add_a_checksum_of(String checksum) {
	    registerBody.setChecksum(checksum);
	}
	
	@Given("I add data_file_path as {string}")
	public void i_add_data_file_path_as(String string) {
	    // Write code here that turns the phrase above into concrete actions
	    //throw new io.cucumber.java.PendingException();
	}

	@Given("I add meta_data as")
	public void i_add_meta_data_as(io.cucumber.datatable.DataTable dataTable) {
	    // Write code here that turns the phrase above into concrete actions
	    // For automatic transformation, change DataTable to one of
	    // E, List<E>, List<List<E>>, List<Map<K,V>>, Map<K,V> or
	    // Map<K, List<V>>. E,K,V must be a String, Integer, Float,
	    // Double, Byte, Short, Long, BigInteger or BigDecimal.
	    //
	    // For other transformations you can register a DataTableType.
	    //throw new io.cucumber.java.PendingException();
		
		//List<List<String>> rawDetails = details.asLists(String.class);
	    //String path = rawDetails.get(1).get(0);
	    
	    List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
	    for (Map<String, String> columns : rows) {
	      String attribute = columns.get("attribute");
	      String val = columns.get("value");
	      System.out.println("attribute=" + attribute);
	      System.out.println("value=" + val);
	    } 
	}
	
	
	@When("I click Register")
	public void i_click_register() {
	    // Write code here that turns the phrase above into concrete actions
		String token = "xyz";
		RequestSpecification request = RestAssured.given();
		request.body("");
		request.header("Accept", "application/json");
		request.header("Authorization", "Bearer "+ token);
		request.header("Content-Type", "application/json");
	    //Response response = RestAssured.get("http://www.example.com");
		 Gson gson = new Gson();
		 String registerBodyJson = gson.toJson(registerBody);
		 System.out.println(registerBodyJson);
	     RestAssured.baseURI = "https://fsdmel-dsapi01d.ncifcrf.gov:7738";
		 File file = new File("/Users/schintal/git/HPC_DME_APIs/test-automation/cuketests/src/test/java/Register/Steps/a.json");
		 
		 System.out.println("PRinting file contents");
		 System.out.println(file);
		Response response = request.body(file).post("");
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
