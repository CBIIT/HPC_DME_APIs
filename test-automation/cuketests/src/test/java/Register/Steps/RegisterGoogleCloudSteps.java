package Register.Steps;

import static io.restassured.RestAssured.when;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;

import java.io.*;
import Register.Pojo.GooglePojo;
import Register.Pojo.ParentMetadataPojo;
import Register.Pojo.RegisterGoogleCloudPojo;
import Register.Pojo.RegisterPojo;
import Register.Pojo.SourceLocationPojo;
import common.JsonHelper;
import dataProviders.ConfigFileReader;
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


public class RegisterGoogleCloudSteps {
  ConfigFileReader configFileReader= new ConfigFileReader();
  RegisterGoogleCloudPojo registerBody = new RegisterGoogleCloudPojo();
  SourceLocationPojo sourceLocation  = new SourceLocationPojo();
  List<RegisterGoogleCloudPojo> files = new ArrayList<RegisterGoogleCloudPojo>();
  GooglePojo googleObj = new GooglePojo();
  String token;
  String accessToken;
  int statusCode;
 
	
	@Given("I am a valid gc_user with token")
	public void i_am_a_valid_gc_user_with_token() {
	  configFileReader= new ConfigFileReader();
      this.token = configFileReader.getToken();
      this.accessToken =  configFileReader.getGoogleCloudToken();
	}

	@Given("I add gc_base_path as {string}")
	public void i_add_gc_base_path_as(String path) {
	  registerBody.setPath(path);	  
	}

	@Given("I add a google cloud bucket as {string}")
	public void i_add_a_google_cloud_bucket_as(String bucket) {
	    //registerBody.getGoogleCloudStorageUploadSource().getSourceLocation();
	    
	  sourceLocation.setFileContainerId(bucket);
	}

	@Given("I add a google cloud location as {string}")
	public void i_add_a_google_cloud_location_as(String file) {
	  sourceLocation.setFileId(file);
	  googleObj.setSourceLocation(sourceLocation);
	}

	@Given("I have a refresh token")
	public void i_have_a_refresh_token() {
	  String refreshTokenStr = configFileReader.getGoogleCloudToken();
	  googleObj.setAccessToken(refreshTokenStr);
	  registerBody.setGoogleCloudStorageUploadSource(googleObj);
	}



	@When("I click Register for the Google Cloud Upload")
	public void i_click_register_for_the_google_cloud_upload() {
	  System.out.println("----------------------------------------------------------");
	  System.out.println("Test Google Cloud Upload")
	  configFileReader= new ConfigFileReader();
      String token = configFileReader.getToken();
      files.add(registerBody);
      String registerBodyJson = new JsonHelper().getPrettyJson((Object)files);
      RestAssured.baseURI = "https://fsdmel-dsapi01d.ncifcrf.gov/";
      RestAssured.port = 7738;
      RequestSpecification request = RestAssured.given().
          relaxedHTTPSValidation().
          header("Accept", "application/json").
          header("Authorization", "Bearer "+ token).  
          multiPart("dataObjectRegistration",registerBodyJson, "application/json");
         // multiPart("dataObject", new File(this.registerBody.getGoogleCloudStorageUploadSource().), "application/octet-stream"); 
      
      Response response = request.put("/hpc-server/v2/dataobject" + this.registerBody.getPath());
      //System.out.println(response.asString());
      //System.out.println(response.getBody());
     
      
      this.statusCode = response.getStatusCode();
      if (this.statusCode == 200 || this.statusCode == 201) {
        System.out.println("This test was a success");
        System.out.println("StatusCode = " + response.getStatusCode());
        //assert(true);
      } else {
        System.out.println("This test was a failure");
        System.out.println(response.asString());
      }
      System.out.println("----------------------------------------------------------");
      System.out.println("");    	  
	    
	}

	@Then("I get a response of success for the Google Cloud Upload")
	public void i_get_a_response_of_success_for_the_google_cloud_upload() {
	    
	}



	
}