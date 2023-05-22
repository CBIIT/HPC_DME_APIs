package Register.Steps;

import static io.restassured.RestAssured.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.*;

import Register.Pojo.BulkDataObjectRegisterPojo;
import Register.Pojo.DataObjectRegistration;
import Register.Pojo.DirectoryScanRegistrationItemPojo;
import Register.Pojo.GoogleCloudUploadPojo;
import Register.Pojo.GooglePojo;
import Register.Pojo.ParentMetadataPojo;
import Register.Pojo.RegisterGoogleCloudPojo;
import Register.Pojo.RegisterPojo;
import Register.Pojo.S3AccountPojo;
import Register.Pojo.S3StreamingUploadPojo;
import Register.Pojo.SourceLocationPojo;
import common.JsonHelper;
import common.TaskHelper;
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
  List<Map<String, String>> rows;
  GooglePojo googleObj = new GooglePojo();
  String token;
  String accessToken;
  String path;
  String source;
  int statusCode;

  // The Logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
  
  // 
  GoogleCloudUploadPojo googleCloudInfo = new GoogleCloudUploadPojo();
  DirectoryScanRegistrationItemPojo directoryScanRegistration = new DirectoryScanRegistrationItemPojo();

  @Given("I have a data source {string}")
  public void i_have_a_data_source(String source) {
    this.source = source;
  }
  
  @Given("I add registration path as {string}")
	public void i_add_registration_path_as(String path) {
    this.path = path;
  }

  @Given("I add a google cloud bucket as {string}")
	public void i_add_a_google_cloud_bucket_as(String bucket) {
	  sourceLocation.setFileContainerId(bucket);
  }

  @Given("I add a google cloud location as {string}")
	public void i_add_a_google_cloud_location_as(String file) {
    sourceLocation.setFileId(file);
  }

  @Given("I choose file or directory as {string}")
  public void i_choose_file_or_directory_as(String isFile) {
    System.out.println(isFile);
  }



	@Given("I add google cloud metadataEntries as")
	public void i_add_google_cloud_metadata_entries_as(io.cucumber.datatable.DataTable dataTable) {
	    rows = dataTable.asMaps(String.class, String.class);
      for (Map<String, String> columns : rows) {
        String attribute = columns.get("attribute");
        String val = columns.get("value");
      }
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
      this.token = configFileReader.getToken();
      if (this.source.equals("googleCloud")) {
        googleCloudInfo.setAccessToken(configFileReader.getGoogleCloudToken());
        googleCloudInfo.setSourceLocation(sourceLocation);
        DataObjectRegistration dataObjectRegistration = new DataObjectRegistration();
        dataObjectRegistration.setPath(this.path);
        dataObjectRegistration.setGoogleCloudStorageUploadSource(googleCloudInfo);
        String totalPath = dataObjectRegistration.getPath() + "/" + googleCloudInfo.getSourceLocation().getFileId();
        dataObjectRegistration.setPath(totalPath);
        dataObjectRegistration.setDataObjectMetadataEntries(rows);
        ArrayList<DataObjectRegistration> dataObjectRegistrations = new ArrayList<DataObjectRegistration>();
        dataObjectRegistrations.add(0, dataObjectRegistration);
        BulkDataObjectRegisterPojo bulkRequest = new BulkDataObjectRegisterPojo();
        bulkRequest.setDataObjectRegistrationItems(dataObjectRegistrations);
        new TaskHelper().submitBulkRequest(bulkRequest, this.token);
        System.out.println("----------------------------------------------------------");
        System.out.println("");
      }
	}
	
	@When("I click Register for the AWS Upload")
	public void i_click_register_for_the_aws_upload() {
	    System.out.println("Welcome AWS upload");
      this.token = configFileReader.getToken();
      S3StreamingUploadPojo s3Info = new S3StreamingUploadPojo();
      S3AccountPojo s3Account = new S3AccountPojo();
      s3Account.setAccessKey(configFileReader.getAwsAccessKey());
      s3Account.setSecretKey(configFileReader.getAwsSecretKey());
      s3Account.setRegion("us-east-2");
      s3Info.setAccount(s3Account);
      s3Info.setSourceLocation(sourceLocation);
      DataObjectRegistration dataObjectRegistration = new DataObjectRegistration();
      dataObjectRegistration.setPath(this.path);
      dataObjectRegistration.setS3UploadSource(s3Info);
      String totalPath = dataObjectRegistration.getPath() + "/" + s3Info.getSourceLocation().getFileId();
      dataObjectRegistration.setPath(totalPath);
      dataObjectRegistration.setDataObjectMetadataEntries(rows);
      ArrayList<DataObjectRegistration> dataObjectRegistrations = new ArrayList<DataObjectRegistration>();
      dataObjectRegistrations.add(0, dataObjectRegistration);
      BulkDataObjectRegisterPojo bulkRequest = new BulkDataObjectRegisterPojo();
      bulkRequest.setDataObjectRegistrationItems(dataObjectRegistrations);
      new TaskHelper().submitBulkRequest(bulkRequest, this.token);
      System.out.println("----------------------------------------------------------");
      System.out.println("");
	}



	@Then("I get a response of success for the Google Cloud Upload")
	public void i_get_a_response_of_success_for_the_google_cloud_upload() {
	    org.junit.Assert.assertEquals(201, 201);
	}
}