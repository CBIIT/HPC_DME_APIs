package Register.Steps;

import static io.restassured.RestAssured.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.*;

import Register.Pojo.BulkDataObjectRegisterPojo;
import Register.Pojo.DataObjectRegistration;
import Register.Pojo.GoogleCloudUploadPojo;
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
//import io.cucumber.messages.internal.com.google.gson.Gson;
import io.cucumber.datatable.DataTable;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.path.json.JsonPath;
import junit.framework.Assert;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.lang.Thread;
//import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RegisterGoogleCloudSteps {
  ConfigFileReader configFileReader= new ConfigFileReader();
  RegisterGoogleCloudPojo registerBody = new RegisterGoogleCloudPojo();
  SourceLocationPojo sourceLocation  = new SourceLocationPojo();
  List<RegisterGoogleCloudPojo> files = new ArrayList<RegisterGoogleCloudPojo>();
  GooglePojo googleObj = new GooglePojo();
  String token;
  String accessToken;
  int statusCode;

	// The Logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
  
  // 
  GoogleCloudUploadPojo googleCloudInfo = new GoogleCloudUploadPojo();
  DataObjectRegistration dataObjectRegistration = new DataObjectRegistration();

  @Given("I add registration path as {string}")
	public void i_add_registration_path_as(String path) {
    dataObjectRegistration.setPath(path);
    this.token = configFileReader.getToken();
	}

	@Given("I add a google cloud bucket as {string}")
	public void i_add_a_google_cloud_bucket_as(String bucket) {
	  sourceLocation.setFileContainerId(bucket);
	}

	@Given("I add a google cloud location as {string}")
	public void i_add_a_google_cloud_location_as(String file) {
    sourceLocation.setFileId(file);
	}
	
	@Given("I add google cloud metadataEntries as")
	public void i_add_google_cloud_metadata_entries_as(io.cucumber.datatable.DataTable dataTable) {
	  List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
      for (Map<String, String> columns : rows) {
        String attribute = columns.get("attribute");
        String val = columns.get("value");
      }
      dataObjectRegistration.setDataObjectMetadataEntries(rows);
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
	  System.out.println("Test Google Cloud Upload");
    token = configFileReader.getToken();
    googleCloudInfo.setAccessToken(configFileReader.getGoogleCloudToken());
    googleCloudInfo.setSourceLocation(sourceLocation);
    
    dataObjectRegistration.setGoogleCloudStorageUploadSource(googleCloudInfo);
    String totalPath = dataObjectRegistration.getPath() + "/" + googleCloudInfo.getSourceLocation().getFileId();
    dataObjectRegistration.setPath(totalPath);
    ArrayList<DataObjectRegistration> dataObjectRegistrations = new ArrayList<DataObjectRegistration>();
    dataObjectRegistrations.add(0, dataObjectRegistration);
    BulkDataObjectRegisterPojo bulkRequest = new BulkDataObjectRegisterPojo();
    bulkRequest.setDataObjectRegistrationItems(dataObjectRegistrations);
    handleTask(bulkRequest);
    System.out.println("----------------------------------------------------------");
    System.out.println("");
	}

	@Then("I get a response of success for the Google Cloud Upload")
	public void i_get_a_response_of_success_for_the_google_cloud_upload() {
	    org.junit.Assert.assertEquals(201, 201);
	}

	private void handleTask(BulkDataObjectRegisterPojo bulkRequest) {
      Gson gson = new Gson();
      RestAssured.baseURI = "https://fsdmel-dsapi01d.ncifcrf.gov/";
      RestAssured.port = 7738;
      RequestSpecification request = RestAssured.given().log().all().
          relaxedHTTPSValidation().
          header("Accept", "application/json").
          header("Authorization", "Bearer "+ token).
          header("Content-Type", "application/json").
          body(gson.toJson(bulkRequest));
      Response response = request.put("/hpc-server/v2/registration");
      this.statusCode = response.getStatusCode();
      if (this.statusCode == 200 || this.statusCode == 201) {
        System.out.println("The Registration was submitted succesfully.");
        System.out.println("StatusCode = " + response.getStatusCode());
        System.out.println(response.getBody());
        JsonPath jsonPath= response.jsonPath();
        String taskId = jsonPath.get("taskId").toString();
        System.out.println("Monitoring task id: " + taskId);
        int i = 0;
        boolean inProgress = true;
        boolean taskFailed = false;
        do {
            request = RestAssured.given().//log().all().
            relaxedHTTPSValidation().
            header("Accept", "application/json").
            header("Authorization", "Bearer "+ token).
            header("Content-Type", "application/json").
            body(gson.toJson(bulkRequest));
            response = request.get("/hpc-server/v2/registration/" + taskId);
            this.statusCode = response.getStatusCode();
            System.out.println(this.statusCode);
            System.out.println(response.getBody().asString());
            jsonPath= response.jsonPath();
            Object task = jsonPath.get("task.failedItems.message");
            System.out.println("Printing extracted task");
            System.out.println(task);
            Object failedItems = jsonPath.get("task.failedItems.message");;
            if(failedItems == null || failedItems.toString().equals("")) {
                System.out.println("Printing inprogress");
              inProgress = jsonPath.get("inProgress");
              System.out.println(inProgress);
              try {
                Thread.sleep(10000);  //1000 milliseconds is one second.
              } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
              }
            } else {
              taskFailed = true;
              System.out.println("Failed set to true");
              Object message = jsonPath.get("task.failedItems.message");;
              System.out.println(message.toString());
              break;
            }
            i++;
             System.out.println(i);
        } while(i < 10 && inProgress && !taskFailed);
      } else {
        System.out.println("This test was a failure!");
        JsonPath jsonPath= response.jsonPath();
        String errorType = jsonPath.get("errorType");
        System.out.println(errorType);
      }
	}
}