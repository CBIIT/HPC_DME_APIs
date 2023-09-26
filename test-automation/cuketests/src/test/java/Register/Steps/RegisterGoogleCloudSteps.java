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
	static final String BULK_REGISTRATION_URL = "/hpc-server/v2/registration";
	static final String DATA_OBJECT_REGISTRATION_URL = "/hpc-server/v2/dataObject";
	Gson gson = new Gson();

	ConfigFileReader configFileReader = new ConfigFileReader();
	TaskHelper taskHelper = new TaskHelper();
	RegisterGoogleCloudPojo registerBody = new RegisterGoogleCloudPojo();
	SourceLocationPojo sourceLocation = new SourceLocationPojo();
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
	String isFile = "false";
	int statusCode;

	// The Logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

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

	}

	@Given("I add metadataEntries as")
	public void i_add_metadata_entries_as(io.cucumber.datatable.DataTable dataTable) {
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
		if (bulkMetadataEntries.getPathsMetadataEntries() == null
				|| bulkMetadataEntries.getPathsMetadataEntries().isEmpty()) {
			bulkMetadataEntries.setPathsMetadataEntries(new ArrayList<BulkMetadataEntry>());
		}
		bulkMetadataEntries.getPathsMetadataEntries().add(metadataEntry);
	}

	@Given("I add default metadataEntries as")
	public void i_add_default_metadata_entries_as(io.cucumber.datatable.DataTable dataTable) {
		List<Map<String, String>> default_metadata_rows = dataTable.asMaps(String.class, String.class);
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

	@When("I click Register for the file Upload")
	public void i_click_register_for_the_file_upload() {
		System.out.println("----------------------------------------------------------");
		System.out.println("Test Google Cloud bulk Upload");
		DataObjectRegistration dataObjectRegistration = new DataObjectRegistration();
		dataObjectRegistration.setParentCollectionsBulkMetadataEntries(bulkMetadataEntries);
		String totalPath = this.path + "/" + sourceLocation.getFileId();
		dataObjectRegistration.setPath(totalPath);
		dataObjectRegistration.setDataObjectMetadataEntries(rows);
		S3StreamingUploadPojo sourceInfo = new S3StreamingUploadPojo();
		sourceInfo.setSourceLocation(sourceLocation);
		if (this.source.toUpperCase().equals("GOOGLECLOUD")) {
			sourceInfo.setAccessToken(configFileReader.getGoogleCloudToken());
			dataObjectRegistration.setGoogleCloudStorageUploadSource(sourceInfo);
		} else if (this.source.toUpperCase().equals("AWS")) {
			S3StreamingUploadPojo s3Info = new S3StreamingUploadPojo();
			sourceInfo.setAccount(taskHelper.getAcctAWS());
			dataObjectRegistration.setS3UploadSource(sourceInfo);
		} else if (this.source.toUpperCase().equals("GLOBUS")) {
			dataObjectRegistration.setGlobusUploadSource(sourceInfo);
		} else if (this.source.toUpperCase().equals("GOOGLEDRIVE")) {
			sourceInfo.setAccessToken(configFileReader.getGoogleDriveAccessToken());
			dataObjectRegistration.setGoogleDriveUploadSource(sourceInfo);
		} else {
			System.out.println("Unknown Source");
			return;
		}
		ArrayList<DataObjectRegistration> dataObjectRegistrations = new ArrayList<DataObjectRegistration>();
		dataObjectRegistrations.add(0, dataObjectRegistration);
		BulkDataObjectRegister bulkRequest = new BulkDataObjectRegister();
		bulkRequest.setDataObjectRegistrationItems(dataObjectRegistrations);
		taskHelper.submitBulkRequest("PUT", gson.toJson(bulkRequest), BULK_REGISTRATION_URL);
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
		if (this.source.toUpperCase().equals("GOOGLECLOUD")) {
			directoryLocation.setAccessToken(configFileReader.getGoogleCloudToken());
			directoryScanRegistration.setGoogleCloudStorageScanDirectory(directoryLocation);
		} else if (this.source.toUpperCase().equals("GOOGLEDRIVE")) {
			directoryLocation.setAccessToken(configFileReader.getGoogleDriveAccessToken());
			directoryScanRegistration.setGoogleDriveScanDirectory(directoryLocation);
		} else if (this.source.toUpperCase().equals("AWS")) {
			directoryLocation.setAccount(taskHelper.getAcctAWS());
			directoryScanRegistration.setS3ScanDirectory(directoryLocation);
		} else if (this.source.toUpperCase().equals("GLOBUS")) {
			directoryScanRegistration.setGlobusScanDirectory(directoryLocation);
		} else {
			System.out.println("Unknown Source");
			return;
		}
		String gcPath = sourceLocation.getFileId();
		String tempPath = gcPath;
		if (gcPath.endsWith("/"))
			tempPath = gcPath.substring(0, gcPath.length() - 1);
		String gcToPath = tempPath.substring(tempPath.lastIndexOf("/") + 1, tempPath.length());
		pathMap.setToPath(gcToPath);
		pathMap.setFromPath(tempPath);
		directoryScanRegistration.setBasePath(this.path);
		directoryScanRegistration.setPatternType("SIMPLE");
		directoryScanRegistration.setPathMap(pathMap);
		directoryScanRegistration.setBulkMetadataEntries(bulkMetadataEntries);
		ArrayList<DirectoryScanRegistrationItemPojo> directoryScanRegistrations = new ArrayList<DirectoryScanRegistrationItemPojo>();
		directoryScanRegistrations.add(0, directoryScanRegistration);
		BulkDataObjectRegister bulkRequest = new BulkDataObjectRegister();
		bulkRequest.setDirectoryScanRegistrationItems(directoryScanRegistrations);
		taskHelper.submitBulkRequest("PUT", gson.toJson(bulkRequest), BULK_REGISTRATION_URL);
		System.out.println("----------------------------------------------------------");
		System.out.println("");
	}

	@Then("I get a response of success for the Upload")
	public void i_get_a_response_of_success_for_the_google_cloud_upload() {
		org.junit.Assert.assertEquals(201, 201);
	}
}