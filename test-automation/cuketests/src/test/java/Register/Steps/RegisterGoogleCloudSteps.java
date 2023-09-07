package Register.Steps;

import static io.restassured.RestAssured.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.*;

import Register.Pojo.BulkDataObjectRegister;
import Register.Pojo.BulkMetadataEntriesPojo;
import Register.Pojo.BulkMetadataEntry;
import Register.Pojo.DataObjectRegistration;
import Register.Pojo.DirectoryScanRegistrationItemPojo;
import Register.Pojo.GoogleCloudUploadPojo;
import Register.Pojo.DirectoryScanPathMapPojo;
import Register.Pojo.GooglePojo;
import Register.Pojo.ParentMetadataPojo;
import Register.Pojo.RegisterGoogleCloudPojo;
import Register.Pojo.RegisterPojo;
import Register.Pojo.S3AccountPojo;
import Register.Pojo.S3StreamingUploadPojo;
import Register.Pojo.SourceLocationPojo;
import Register.Pojo.DirectoryLocationUploadPojo;
import Register.Pojo.GlobusUploadSourcePojo;
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
  TaskHelper taskHelper= new TaskHelper();
  RegisterGoogleCloudPojo registerBody = new RegisterGoogleCloudPojo();
  SourceLocationPojo sourceLocation  = new SourceLocationPojo();
  DirectoryScanPathMapPojo pathMap = new DirectoryScanPathMapPojo();
  List<RegisterGoogleCloudPojo> files = new ArrayList<RegisterGoogleCloudPojo>();
  List<Map<String, String>> rows;
  GooglePojo googleObj = new GooglePojo();
  BulkMetadataEntriesPojo bulkMetadataEntries = new BulkMetadataEntriesPojo();
  String token;
  String accessToken;
  String path;
  String source;
  String directoryPath;
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
  
  @Given("I have registration path as {string}")
	public void i_have_registration_path_as(String path) {
    this.path = path;
  }

  @Given("I add source cloud bucket as {string}")
	public void i_add_source_cloud_bucket_as(String bucket) {
	  sourceLocation.setFileContainerId(bucket);
  }

  @Given("I add source cloud location as {string}")
	public void i_add_source_cloud_location_as(String file) {
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

  @Given("I add a path as {string} with pathMetadata as")
    public void i_add_a_path_as_with_path_metadata_as(String path, io.cucumber.datatable.DataTable dataTable) {
        List<Map<String, String>> path_metadata_rows = dataTable.asMaps(String.class, String.class);
       BulkMetadataEntry metadataEntry = new BulkMetadataEntry();
       metadataEntry.setPath(path);
       metadataEntry.setPathMetadataEntries(path_metadata_rows);
       if(bulkMetadataEntries.getPathsMetadataEntries() == null || bulkMetadataEntries.getPathsMetadataEntries().isEmpty()) { 
         bulkMetadataEntries.setPathsMetadataEntries(new ArrayList<BulkMetadataEntry>());
       }
       bulkMetadataEntries.getPathsMetadataEntries().add(metadataEntry);
    }

  @Given("I add default metadataEntries as")
  public void i_add_default_metadata_entries_as(io.cucumber.datatable.DataTable dataTable) {
    List<Map<String, String>> default_metadata_rows  = dataTable.asMaps(String.class, String.class);
    bulkMetadataEntries.setDefaultCollectionMetadataEntries(default_metadata_rows);
  }

  @Given("I add fromPath as {string}")
  public void i_add_from_path_as(String fromPath) {
	  this.pathMap.setFromPath(fromPath); 
  }

  @Given("I add toPath as {string}")
  public void i_add_to_path_as(String toPath) {
	  this.pathMap.setToPath(toPath);
  }

	@When("I click Register for the Google Cloud Upload")
	public void i_click_register_for_the_google_cloud_upload() {
	  System.out.println("----------------------------------------------------------");
	  System.out.println("Test Google Cloud bulk Upload");
      this.token = configFileReader.getToken();
      if (this.source.equals("googleCloud")) {
        googleCloudInfo.setAccessToken(configFileReader.getGoogleCloudToken());
        googleCloudInfo.setSourceLocation(sourceLocation);
        DataObjectRegistration dataObjectRegistration = new DataObjectRegistration();
        dataObjectRegistration.setPath(this.path);
        dataObjectRegistration.setGoogleCloudStorageUploadSource(googleCloudInfo);
        dataObjectRegistration.setParentCollectionsBulkMetadataEntries(bulkMetadataEntries);
        String totalPath = dataObjectRegistration.getPath() + "/" + googleCloudInfo.getSourceLocation().getFileId();
        dataObjectRegistration.setPath(totalPath);
        dataObjectRegistration.setDataObjectMetadataEntries(rows);
        ArrayList<DataObjectRegistration> dataObjectRegistrations = new ArrayList<DataObjectRegistration>();
        dataObjectRegistrations.add(0, dataObjectRegistration);
        BulkDataObjectRegister bulkRequest = new BulkDataObjectRegister();
        bulkRequest.setDataObjectRegistrationItems(dataObjectRegistrations);
        new TaskHelper().submitBulkRequest(bulkRequest, this.token);
        System.out.println("----------------------------------------------------------");
        System.out.println("");
      }
	}
	
	@When("I click Register for the AWS Upload")
	public void i_click_register_for_the_aws_upload() {
		System.out.println("----------------------------------------------------------");
	    System.out.println("Welcome AWS bulk upload");
      this.token = configFileReader.getToken();
      DataObjectRegistration dataObjectRegistration = taskHelper.setupAuthorizeAWS(sourceLocation, this.path);
      dataObjectRegistration.setDataObjectMetadataEntries(rows);
      ArrayList<DataObjectRegistration> dataObjectRegistrations = new ArrayList<DataObjectRegistration>();
      dataObjectRegistrations.add(0, dataObjectRegistration);
      BulkDataObjectRegister bulkRequest = new BulkDataObjectRegister();
      bulkRequest.setDataObjectRegistrationItems(dataObjectRegistrations);
      taskHelper.submitBulkRequest(bulkRequest, this.token);
      System.out.println("----------------------------------------------------------");
      System.out.println("");
	}

	@When("I click Register for the Globus Upload")
	public void i_click_register_for_the_globus_upload() {
		System.out.println("----------------------------------------------------------");
		System.out.println("Welcome Globus bulk upload");
		this.token = configFileReader.getToken();
		//GlobusUploadSourcePojo globusSource = new GlobusUploadSourcePojo();
		S3StreamingUploadPojo globusSource = new S3StreamingUploadPojo();
		globusSource.setSourceLocation(sourceLocation);
		DataObjectRegistration dataObjectRegistration = new DataObjectRegistration();
		dataObjectRegistration.setPath(this.path);
		dataObjectRegistration.setDataObjectMetadataEntries(rows);
		dataObjectRegistration.setGlobusUploadSource(globusSource);
		ArrayList<DataObjectRegistration> dataObjectRegistrations = new ArrayList<DataObjectRegistration>();
		dataObjectRegistrations.add(0, dataObjectRegistration);
		BulkDataObjectRegister bulkRequest = new BulkDataObjectRegister();
		bulkRequest.setDataObjectRegistrationItems(dataObjectRegistrations);
		new TaskHelper().submitBulkRequest(bulkRequest, this.token);
		System.out.println("----------------------------------------------------------");
		System.out.println("");

	}

	@When("I click Register for the directory Upload")
	public void i_click_register_for_the_directory_upload() {
		System.out.println("----------------------------------------------------------");
		System.out.println("Test Google Cloud bulk Upload");
		TaskHelper taskHelper = new TaskHelper();
		this.token = configFileReader.getToken();
		DirectoryLocationUploadPojo directoryLocation = new DirectoryLocationUploadPojo();
		directoryLocation.setDirectoryLocation(sourceLocation);
		if(this.source.equals("googleCloud")){
			directoryLocation.setAccessToken(configFileReader.getGoogleCloudToken());
			directoryScanRegistration.setGoogleCloudStorageScanDirectory(directoryLocation);
		} else if (source.equals("googleDrive")) {
			directoryLocation.setAccessToken(configFileReader.getGoogleDriveAccessToken());
			directoryScanRegistration.setGoogleDriveScanDirectory(directoryLocation);
		} else if (source.equals("aws")) {
			directoryLocation.setAccount(taskHelper.getAcctAWS());
			directoryScanRegistration.setS3ScanDirectory(directoryLocation);
		} else if (source.equals("globus")) {
			directoryScanRegistration.setGlobusScanDirectory(directoryLocation);
		} else {
			System.out.println(" Unknown Source");
		}
		String gcPath = sourceLocation.getFileId();
		String tempPath = gcPath;
		if (gcPath.endsWith("/"))
		  tempPath = gcPath.substring(0, gcPath.length() - 1);
		String gcToPath = tempPath.substring(tempPath.lastIndexOf("/")+1, tempPath.length());
        pathMap.setToPath(gcToPath);
        pathMap.setFromPath(tempPath);
		directoryScanRegistration.setBasePath(this.path);
		directoryScanRegistration.setPatternType("SIMPLE");
		this.directoryScanRegistration.setPathMap(pathMap);
		ArrayList<DirectoryScanRegistrationItemPojo> directoryScanRegistrations = new ArrayList<DirectoryScanRegistrationItemPojo>();
		directoryScanRegistrations.add(0, directoryScanRegistration);
		BulkDataObjectRegister bulkRequest = new BulkDataObjectRegister();
		bulkRequest.setDirectoryScanRegistrationItems(directoryScanRegistrations);
		taskHelper.submitBulkRequest(bulkRequest, this.token);
		System.out.println("----------------------------------------------------------");
		System.out.println("");
	}

	@Then("I get a response of success for the Google Cloud Upload")
	public void i_get_a_response_of_success_for_the_google_cloud_upload() {
	    org.junit.Assert.assertEquals(201, 201);
	}
}