package Register.Steps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import Register.Pojo.BulkDataObjectRegister;
import Register.Pojo.BulkMetadataEntriesPojo;
import Register.Pojo.BulkMetadataEntry;
import Register.Pojo.DataObjectRegistration;
import Register.Pojo.DirectoryLocationUploadPojo;
import Register.Pojo.DirectoryScanPathMapPojo;
import Register.Pojo.DirectoryScanRegistrationItemPojo;
import Register.Pojo.RegisterGoogleCloudPojo;
import Register.Pojo.S3StreamingUploadPojo;
import Register.Pojo.SourceLocationPojo;
import common.TaskHelper;
import dataProviders.ConfigFileReader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class RegisterBulkSteps {
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
	BulkMetadataEntriesPojo bulkMetadataEntries = new BulkMetadataEntriesPojo();
	ArrayList<DataObjectRegistration> dataObjectRegistrations = new ArrayList<DataObjectRegistration>();
	ArrayList<DirectoryScanRegistrationItemPojo> directoryScanRegistrations = new ArrayList<DirectoryScanRegistrationItemPojo>();
	BulkDataObjectRegister bulkRequest = new BulkDataObjectRegister();
	DataObjectRegistration dataObjectRegistration = new DataObjectRegistration();
	DirectoryScanRegistrationItemPojo directoryScanRegistration = new DirectoryScanRegistrationItemPojo();
	String token;
	String accessToken;
	String path;
	String source;
	String directoryPath;
	boolean isBulkDataObjectRegistration; // true if data object Registration, false if directory Registration
	boolean firstRegistration = true;
	boolean testSuccess = false;
	int statusCode;

	// The Logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	@Given("I need set up a Bulk Data Object Registration")
	public void i_need_set_up_a_bulk_data_object_registration() {
		if (firstRegistration) {
			firstRegistration = false;
			isBulkDataObjectRegistration = true;
		} else {
			if (isBulkDataObjectRegistration) {
				setupDataObjectRegistration();
			} else {
				setupDirectoryScanRegistration();
			}
		}
		isBulkDataObjectRegistration = true;
	}

	@Given("I need set up a Bulk Directory Registration")
	public void i_need_set_up_a_bulk_directory_registration() {
		if (firstRegistration) {
			firstRegistration = false;
		} else {
			if (isBulkDataObjectRegistration) {
				setupDataObjectRegistration();
			} else {
				setupDirectoryScanRegistration();
			}
		}
		isBulkDataObjectRegistration = false;
	}

	@Then("I get a response of <response> for the Upload")
	public void i_get_a_response_of_response_for_the_upload() {
		// Write code here that turns the phrase above into concrete actions
		throw new io.cucumber.java.PendingException();
	}

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

	@Given("I add metadataEntries as")
	public void i_add_metadata_entries_as(io.cucumber.datatable.DataTable dataTable) {
		//Sync...
		rows = dataTable.asMaps(String.class, String.class);
		for (Map<String, String> columns : rows) {
			String attribute = columns.get("attribute");
			String val = columns.get("value");
		}
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
		System.out.println("----------------------------------------------------------");
		System.out.println("Test Google Cloud bulk Upload");
		setupDataObjectRegistration();
		bulkRequest.setDataObjectRegistrationItems(dataObjectRegistrations);
		taskHelper.submitBulkRequest("PUT", gson.toJson(bulkRequest), BULK_REGISTRATION_URL);
		System.out.println("----------------------------------------------------------");
		System.out.println("");
	}

	@When("I click Register for the directory Upload")
	public void i_click_register_for_the_directory_upload() {
		System.out.println("----------------------------------------------------------");
		System.out.println("Test Google Cloud bulk Upload");
		System.out.println("----------------------------------------------------------");
		System.out.println("Test Google Cloud bulk Upload");
		setupDirectoryScanRegistration();
		bulkRequest.setDirectoryScanRegistrationItems(directoryScanRegistrations);
		taskHelper.submitBulkRequest("PUT", gson.toJson(bulkRequest), BULK_REGISTRATION_URL);
		System.out.println("----------------------------------------------------------");
		System.out.println("");
	}

	@When("I click Register for Bulk Upload")
	public void i_click_register_for_bulk_upload() {
		if (isBulkDataObjectRegistration) {
			System.out.println("----------------------------------------------------------");
			System.out.println("Test Google Cloud bulk Upload");
			setupDataObjectRegistration();
		} else {
			System.out.println("----------------------------------------------------------");
			System.out.println("Test Google Cloud bulk Upload");
			setupDirectoryScanRegistration();
		}
		bulkRequest.setDataObjectRegistrationItems(dataObjectRegistrations);
		bulkRequest.setDirectoryScanRegistrationItems(directoryScanRegistrations);
		testSuccess = taskHelper.submitBulkRequest("PUT", gson.toJson(bulkRequest), BULK_REGISTRATION_URL);
		System.out.println("----------------------------------------------------------");
		System.out.println("");
	}

	@Then("I get a response of success for the Upload")
	public void i_get_a_response_of_success_for_the_google_cloud_upload() {
		org.junit.Assert.assertEquals(testSuccess, true);
	}

	private void setupDataObjectRegistration() {
		dataObjectRegistration = new DataObjectRegistration();
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
		dataObjectRegistrations.add(dataObjectRegistration);
		bulkMetadataEntries = new BulkMetadataEntriesPojo();
		sourceLocation = new SourceLocationPojo();
	}

	private void setupDirectoryScanRegistration() {
		System.out.println("----------------------------------------------------------");
		System.out.println("Test Google Cloud bulk Upload");
		this.token = configFileReader.getToken();
		directoryScanRegistration = new DirectoryScanRegistrationItemPojo();
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
		directoryScanRegistrations.add(directoryScanRegistration);
		bulkMetadataEntries = new BulkMetadataEntriesPojo();
		sourceLocation = new SourceLocationPojo();
	}

}