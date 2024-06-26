package Download.steps;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import Download.pojos.BulkDownloadDataObjectPojo;
import Download.pojos.DestinationLocationPojo;
import Register.Pojo.SourceLocationPojo;
import common.TaskHelper;
import dataProviders.ConfigFileReader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class DownloadBulkSteps {
	static final String BULK_DOWNLOAD_URL = "/hpc-server/v2/download";
	static final String BULK_DOWNLOAD_COLLECTION_URL = "/hpc-server/v2/collection";
	ConfigFileReader configFileReader = new ConfigFileReader();
	TaskHelper taskHelper = new TaskHelper();
	BulkDownloadDataObjectPojo downloadRequestBody;
	String path;
	String bucket;
	String source;
	String downloadUrl;
	List<String> collectionPathsList = new ArrayList<String>();
	List<String> dataObjectPathsList = new ArrayList<String>();
	SourceLocationPojo sourceLocation = new SourceLocationPojo();
	boolean appendPathToDownloadDestination;
	boolean testSuccess = false;
	DestinationLocationPojo destinationLocationObj = new DestinationLocationPojo();
	Gson gson = new Gson();

	@Given("I have a data source {string} for download")
	public void i_have_a_data_source_for_download(String sourceStr) {
		source = sourceStr;
	}

	@Given("I have download path as {string}")
	public void i_have_download_path_as(String pathStr) {
		path = pathStr;
	}

	@Given("I add download cloud bucket as {string}")
	public void i_add_download_cloud_bucket_as(String bucketName) {
		sourceLocation.setFileContainerId(bucketName);
	}

	@Given("I add download cloud location as {string}")
	public void i_add_download_cloud_location_as(String location) {
		sourceLocation.setFileId(location);
	}

	@Given("I have a download path as {string}")
	public void i_have_a_download_path_as(String downloadPath) {
		downloadUrl = "/hpc-server/v2/dataObject" + downloadPath + "/download";
		downloadUrl.replaceAll("(?<!\\w+:/?)//+", "/");
	}

	@Given("I have a download collection path as {string}")
	public void i_have_a_download_collection_path_as(String downloadPath) {
		downloadUrl = "/hpc-server/v2/collection" + downloadPath + "/download";
		downloadUrl.replaceAll("(?<!\\w+:/?)//+", "/");
	}

	@Given("I have a download dataObject path as {string}")
	public void i_have_a_download_data_object_path_as(String downloadPath) {
		downloadUrl = "/hpc-server/v2/dataObject" + downloadPath + "/download";
		downloadUrl.replaceAll("(?<!\\w+:/?)//+", "/");
	}

	@Given("I have multiple download collection paths as")
	public void i_have_multiple_download_collection_paths_as(io.cucumber.datatable.DataTable dataTable) {
		downloadUrl = BULK_DOWNLOAD_URL;
		collectionPathsList = dataTable.rows(1).asList(); // ignore the head of column
	}

	@Given("I have multiple download dataObject paths as")
	public void i_have_multiple_download_data_object_paths_as(io.cucumber.datatable.DataTable dataTable) {
		downloadUrl = BULK_DOWNLOAD_URL;
		dataObjectPathsList = dataTable.rows(1).asList(); // ignore the head of column
	}

	@Given("I set appendPathToDownloadDestination as {string}")
	public void i_set_append_path_to_download_destination_as(String flag) {
		appendPathToDownloadDestination = (flag == "true") ? true : false;
	}

	@When("I click Download")
	public void i_click_download() {
		System.out.println("----------------------------------------------------------");
		System.out.println("Test Download");
		BulkDownloadDataObjectPojo downloadRequestBody = new BulkDownloadDataObjectPojo();
		String totalPath = this.path + "/" + sourceLocation.getFileId();
		// downloadRequestBody.setPath(totalPath);
		destinationLocationObj.setDestinationLocation(sourceLocation);
		if (source.toUpperCase().equals("GOOGLECLOUD")) {
			destinationLocationObj.setAccessToken(configFileReader.getGoogleCloudToken());
			downloadRequestBody.setGoogleCloudStorageDownloadDestination(destinationLocationObj);
		} else if (source.toUpperCase().equals("AWS")) {
			destinationLocationObj.setS3Account(taskHelper.getAcctAWS());
			downloadRequestBody.setS3DownloadDestination(destinationLocationObj);
			gson = new GsonBuilder().setFieldNamingStrategy(new AccountS3RenameStrategy()).create();
		} else if (source.toUpperCase().equals("GLOBUS")) {
			downloadRequestBody.setGlobusDownloadDestination(destinationLocationObj);
		} else if (source.toUpperCase().equals("GOOGLEDRIVE")) {
			destinationLocationObj.setAccessToken(configFileReader.getGoogleDriveAccessToken());
			downloadRequestBody.setGoogleDriveDownloadDestination(destinationLocationObj);
		} else if (source.toUpperCase().equals("ASPERA")) {
			destinationLocationObj.setAsperaAccount(taskHelper.getAcctAspera());
			downloadRequestBody.setAsperaDownloadDestination(destinationLocationObj);
			gson = new GsonBuilder().setFieldNamingStrategy(new AccountAsperaRenameStrategy()).create();
		} else {
			System.out.println("Unknown Source");
			return;
		}
		if (!collectionPathsList.isEmpty()) {
			downloadRequestBody.setCollectionPaths(collectionPathsList);
		}
		if (!dataObjectPathsList.isEmpty()) {
			downloadRequestBody.setDataObjectPaths(dataObjectPathsList);
		}
		downloadRequestBody.setAppendPathToDownloadDestination(true);
		System.out.println(gson.toJson(downloadRequestBody));
		testSuccess = taskHelper.submitRequest("POST", gson.toJson(downloadRequestBody), downloadUrl);
		System.out.println("----------------------------------------------------------");
		System.out.println("");
	}

	@Then("I get a response of success for the Download")
	public void i_get_a_response_of_success_for_the_download() {
		org.junit.Assert.assertEquals(testSuccess, true);
	}

	@Then("I get a response of {string} for the Download")
	public void i_get_a_response_of_for_the_download(String result) {
		if (result == "success") {
			org.junit.Assert.assertEquals(testSuccess, true);
		} else {
			org.junit.Assert.assertEquals(testSuccess, false);
		}
	}

	class AccountAsperaRenameStrategy implements FieldNamingStrategy {
	     @Override
	     public String translateName(Field field) {
	         if (field.getName().equals("asperaAccount")) {
	             return "account";
	         }
	         return field.getName();
	    }
	}

	class AccountS3RenameStrategy implements FieldNamingStrategy {
	     @Override
	     public String translateName(Field field) {
	         if (field.getName().equals("s3Account")) {
	             return "account";
	         }
	         return field.getName();
	    }
	}

}
