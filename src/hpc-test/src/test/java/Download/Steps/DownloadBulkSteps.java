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
import gov.nih.nci.hpc.domain.datatransfer.HpcBoxDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectDownloadRequestDTO;
//import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;

import gov.nih.nci.hpc.test.common.TaskHelper;
import gov.nih.nci.hpc.test.common.FileHelper;
import gov.nih.nci.hpc.test.dataProviders.ConfigFileReader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class DownloadBulkSteps {
	static final String BULK_DOWNLOAD_URL = "/hpc-server/v2/download";
	static final String DOWNLOAD_DATAOBJECT_URL_PREFIX = "/hpc-server/v2/dataObject";
	static final String DOWNLOAD_COLLECTION_URL_PREFIX = "/hpc-server/v2/collection";
	static final String DOWNLOAD_EXTERNAL_DATAOBJECT_URL_PREFIX = "/hpc-server/ext/dataObject";
	ConfigFileReader configFileReader = new ConfigFileReader();
	TaskHelper taskHelper = new TaskHelper();
	String path;
	String bucket;
	String source;
	String downloadUrl;
	boolean bulkDownloadFlag = false;
	boolean externalArchiveDownloadFlag = false;
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

	@Given("I have a data source {string} for download from external archive")
	public void i_have_a_data_source_for_download_from_external_archive(String sourceStr) {
		source = sourceStr;
		externalArchiveDownloadFlag = true;
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
		if(externalArchiveDownloadFlag) {
			downloadUrl = DOWNLOAD_EXTERNAL_DATAOBJECT_URL_PREFIX + downloadPath + "/download";
		} else {
			 downloadUrl = DOWNLOAD_DATAOBJECT_URL_PREFIX + downloadPath + "/download";
		}
		downloadUrl = downloadUrl.replaceAll("(?<!\\w+:/?)//+", "/");
	}

	@Given("I have a download collection path as {string}")
	public void i_have_a_download_collection_path_as(String downloadPath) {
		downloadUrl = "/hpc-server/v2/collection" + downloadPath + "/download";
		downloadUrl.replaceAll("(?<!\\w+:/?)//+", "/");
	}

	@Given("I have a download dataObject path as {string}")
	public void i_have_a_download_data_object_path_as(String downloadPath) {
		if(externalArchiveDownloadFlag) {
			downloadUrl = DOWNLOAD_EXTERNAL_DATAOBJECT_URL_PREFIX + downloadPath + "/download";
		} else {
			 downloadUrl = DOWNLOAD_DATAOBJECT_URL_PREFIX + downloadPath + "/download";
		}
		downloadUrl = downloadUrl.replaceAll("(?<!\\w+:/?)//+", "/");
	}

	@Given("I have multiple download collection paths as")
	public void i_have_multiple_download_collection_paths_as(io.cucumber.datatable.DataTable dataTable) {
		downloadUrl = BULK_DOWNLOAD_URL;
		bulkDownloadFlag = true;
		collectionPathsList = dataTable.rows(1).asList(); // ignore the head of column
	}

	@Given("I have multiple download dataObject paths as")
	public void i_have_multiple_download_data_object_paths_as(io.cucumber.datatable.DataTable dataTable) {
		downloadUrl = BULK_DOWNLOAD_URL;
		bulkDownloadFlag = true;
		dataObjectPathsList = dataTable.rows(1).asList(); // ignore the head of column
	}

	@Given("I set appendPathToDownloadDestination as {string}")
	public void i_set_append_path_to_download_destination_as(String flag) {
		appendPathToDownloadDestination = (flag == "true") ? true : false;
	}

	@When("I click Download")
	public void i_click_download() {
		System.out.println("----------------------------------------------------------");
		System.out.println("Download URL: " + downloadUrl);
		System.out.println(": externalArchiveDownloadFlag " + externalArchiveDownloadFlag);
		System.out.println("Submit Download");
		HpcBulkDataObjectDownloadRequestDTO downloadBulkRequestBody = new HpcBulkDataObjectDownloadRequestDTO();
		HpcDownloadRequestDTO downloadRequestBody = new HpcDownloadRequestDTO();
		String totalPath = this.path + "/" + sourceLocation.getFileId();
		destinationLocationObj= sourceLocation;
		if (source.toUpperCase().equals("GOOGLECLOUD")) {
            HpcGoogleDownloadDestination googleCloudDestination = new HpcGoogleDownloadDestination();
            googleCloudDestination.setDestinationLocation(destinationLocationObj);
            googleCloudDestination.setAccessToken(configFileReader.getGoogleCloudToken());
			//googleCloudDestination.setDestinationOverwrite(true);
			if(bulkDownloadFlag) {
				downloadBulkRequestBody.setGoogleCloudStorageDownloadDestination(googleCloudDestination);
			} else {
				downloadRequestBody.setGoogleCloudStorageDownloadDestination(googleCloudDestination);
			}
		} else if (source.toUpperCase().equals("AWS")) {
			HpcS3DownloadDestination destination = new HpcS3DownloadDestination();
			destination.setDestinationLocation(destinationLocationObj);
			destination.setAccount(taskHelper.getAcctAWS());
			//destination.setDestinationOverwrite(true);
			if(bulkDownloadFlag) {
				downloadBulkRequestBody.setS3DownloadDestination(destination);
			} else {
				downloadRequestBody.setS3DownloadDestination(destination);
				gson = new GsonBuilder().setFieldNamingStrategy(new AccountS3RenameStrategy()).create();
			}
		} else if (source.toUpperCase().equals("GLOBUS")) {
			HpcGlobusDownloadDestination globusDownloadDestination = new HpcGlobusDownloadDestination();
			globusDownloadDestination.setDestinationLocation(destinationLocationObj);
			globusDownloadDestination.setDestinationOverwrite(true);
			if(bulkDownloadFlag) {
				downloadBulkRequestBody.setGlobusDownloadDestination(globusDownloadDestination);
			} else {
				downloadRequestBody.setGlobusDownloadDestination(globusDownloadDestination);
			}
		} else if (source.toUpperCase().equals("GOOGLEDRIVE")) {
			 HpcGoogleDownloadDestination destination = new HpcGoogleDownloadDestination();
			//destination.setDestinationOverwrite(true);
			destination.setAccessToken(configFileReader.getGoogleDriveAccessToken());
			if(bulkDownloadFlag) {
				downloadBulkRequestBody.setGoogleDriveDownloadDestination(destination);
			} else {
				downloadRequestBody.setGoogleDriveDownloadDestination(destination);
			}
		} else if (source.toUpperCase().equals("BOX")) {
			 HpcBoxDownloadDestination destination = new HpcBoxDownloadDestination();
			 destination.setDestinationLocation(destinationLocationObj);
			//destination.setDestinationOverwrite(true);
			destination.setAccessToken(configFileReader.getBoxAccessToken());
			destination.setRefreshToken(configFileReader.getBoxRefreshToken());
			if(bulkDownloadFlag) {
				downloadBulkRequestBody.setBoxDownloadDestination(destination);
			} else {
				downloadRequestBody.setBoxDownloadDestination(destination);
			}
		} else if (source.toUpperCase().equals("ASPERA")) {
			HpcAsperaDownloadDestination destination = new HpcAsperaDownloadDestination();
			destination.setAccount(taskHelper.getAcctAspera());
			destination.setDestinationLocation(destinationLocationObj);
			//destination.setDestinationOverwrite(true);
			if(bulkDownloadFlag) {
				downloadBulkRequestBody.setAsperaDownloadDestination(destination);
			} else {
				downloadRequestBody.setAsperaDownloadDestination(destination);
				gson = new GsonBuilder().setFieldNamingStrategy(new AccountAsperaRenameStrategy()).create();
			}
		} else {
			System.out.println("Unknown Source");
			return;
		}
		if (!collectionPathsList.isEmpty()) {
			Iterator it = collectionPathsList.iterator();
			while(it.hasNext()){
				downloadBulkRequestBody.getCollectionPaths().add(it.next().toString());
			}
		}
		if (!dataObjectPathsList.isEmpty()) {
			Iterator it = dataObjectPathsList.iterator();
			while(it.hasNext()){
			  downloadBulkRequestBody.getDataObjectPaths().add(it.next().toString());
			}
		}
		//downloadRequestBody.setAppendPathToDownloadDestination(true);
		if(bulkDownloadFlag) {
			//downloadBulkRequestBody.setAppendPathToDownloadDestination(appendPathToDownloadDestination);
			System.out.println(gson.toJson(downloadBulkRequestBody));
			testSuccess = taskHelper.submitRequestBoolean("POST", gson.toJson(downloadBulkRequestBody), downloadUrl);
		} else {
			System.out.println(gson.toJson(downloadRequestBody));
			testSuccess = taskHelper.submitExternalRequest("POST", gson.toJson(downloadRequestBody), downloadUrl);
		}
	}


	@When("I click External Download for single dataobject")
	public void i_click_external_download_for_single_dataobject() {
	HpcDataObjectDownloadResponseDTO downloadRequestBody = new HpcDataObjectDownloadResponseDTO();
    // Write code here that turns the phrase above into concrete actions
    throw new io.cucumber.java.PendingException();
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

