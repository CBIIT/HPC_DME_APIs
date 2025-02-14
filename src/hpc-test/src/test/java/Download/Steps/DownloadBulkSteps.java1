package gov.nih.nci.hpc.test.Download.Steps;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import java.util.Iterator;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaAccount;
import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcGoogleDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;

import gov.nih.nci.hpc.test.common.TaskHelper;
import gov.nih.nci.hpc.test.common.FileHelper;
import gov.nih.nci.hpc.test.dataProviders.ConfigFileReader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class DownloadBulkSteps {
	static final String BULK_DOWNLOAD_URL = "/hpc-server/v2/download";
	static final String BULK_DOWNLOAD_COLLECTION_URL = "/hpc-server/v2/collection";
	ConfigFileReader configFileReader = new ConfigFileReader();
	TaskHelper taskHelper = new TaskHelper();
	String path;
	String bucket;
	String source;
	String downloadUrl;
	List<String> collectionPathsList = new ArrayList<String>();
	List<String> dataObjectPathsList = new ArrayList<String>();
	HpcFileLocation sourceLocation = new HpcFileLocation();
	boolean appendPathToDownloadDestination;
	boolean testSuccess = false;
	HpcFileLocation destinationLocationObj = new HpcFileLocation();
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
		System.out.println("Submit Download");

		HpcBulkDataObjectDownloadRequestDTO downloadRequestBody = new HpcBulkDataObjectDownloadRequestDTO();
		String totalPath = this.path + "/" + sourceLocation.getFileId();
		//downloadRequestBody.setPath(totalPath);
		destinationLocationObj= sourceLocation;
		if (source.toUpperCase().equals("GOOGLECLOUD")) {
            HpcGoogleDownloadDestination googleCloudDestination = new HpcGoogleDownloadDestination();
            googleCloudDestination.setDestinationLocation(destinationLocationObj);
            googleCloudDestination.setAccessToken(configFileReader.getGoogleCloudToken());
			downloadRequestBody.setGoogleCloudStorageDownloadDestination(googleCloudDestination);
		} else if (source.toUpperCase().equals("AWS")) {
			HpcS3DownloadDestination destination = new HpcS3DownloadDestination();
			destination.setDestinationLocation(destinationLocationObj);
			destination.setAccount(taskHelper.getAcctAWS());
			downloadRequestBody.setS3DownloadDestination(destination);
			gson = new GsonBuilder().setFieldNamingStrategy(new AccountS3RenameStrategy()).create();
		} else if (source.toUpperCase().equals("GLOBUS")) {
			HpcGlobusDownloadDestination globusDownloadDestination = new HpcGlobusDownloadDestination();
			globusDownloadDestination.setDestinationLocation(destinationLocationObj);
			downloadRequestBody.setGlobusDownloadDestination(globusDownloadDestination);
		} else if (source.toUpperCase().equals("GOOGLEDRIVE")) {
			 HpcGoogleDownloadDestination destination = new HpcGoogleDownloadDestination();
			destination.setAccessToken(configFileReader.getGoogleDriveAccessToken());
			downloadRequestBody.setGoogleDriveDownloadDestination(destination);
		} else if (source.toUpperCase().equals("ASPERA")) {
			HpcAsperaDownloadDestination destination = new HpcAsperaDownloadDestination();
			destination.setAccount(taskHelper.getAcctAspera());
			destination.setDestinationLocation(destinationLocationObj);
			downloadRequestBody.setAsperaDownloadDestination(destination);
			gson = new GsonBuilder().setFieldNamingStrategy(new AccountAsperaRenameStrategy()).create();
		} else {
			System.out.println("Unknown Source");
			return;
		}
		if (!collectionPathsList.isEmpty()) {
			Iterator it = collectionPathsList.iterator();
			while(it.hasNext()){
				downloadRequestBody.getCollectionPaths().add(it.next().toString());
			}
		}
		if (!dataObjectPathsList.isEmpty()) {
			Iterator it = dataObjectPathsList.iterator();
			while(it.hasNext()){
			  downloadRequestBody.getDataObjectPaths().add(it.next().toString());
			}
		}
		//downloadRequestBody.setAppendPathToDownloadDestination(true);
		System.out.println(gson.toJson(downloadRequestBody));
		testSuccess = taskHelper.submitRequestBoolean("POST", gson.toJson(downloadRequestBody), downloadUrl);
	}

	@Then("I get a response of success for the Download")
	public void i_get_a_response_of_success_for_the_download() {
		System.out.println("Print testSuccess  1");
		System.out.println(testSuccess);
		org.junit.Assert.assertEquals(testSuccess, true);
		System.out.println("----------------------------------------------------------");
		System.out.println("");
	}

	@Then("I get a response of {string} for the Download")
	public void i_get_a_response_of_for_the_download(String result) {
		if (result.equals("success")) {
			org.junit.Assert.assertEquals(testSuccess, true);
		} else {
			org.junit.Assert.assertEquals(testSuccess, false);
		}
		System.out.println("----------------------------------------------------------");
		System.out.println("");
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

