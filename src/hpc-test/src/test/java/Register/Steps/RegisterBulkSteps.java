package gov.nih.nci.hpc.test.Register.Steps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationItemDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDirectoryScanRegistrationItemDTO;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataHierarchy;
import gov.nih.nci.hpc.domain.datamanagement.HpcDirectoryScanPathMap;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGoogleScanDirectory;
import gov.nih.nci.hpc.domain.datatransfer.HpcPatternType;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3ScanDirectory;
import gov.nih.nci.hpc.domain.datatransfer.HpcScanDirectory;
import gov.nih.nci.hpc.domain.datatransfer.HpcStreamingUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadSource;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcBulkMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcBulkMetadataEntry;
import gov.nih.nci.hpc.test.common.TaskHelper;
import gov.nih.nci.hpc.test.dataProviders.ConfigFileReader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class RegisterBulkSteps {
	static final String BULK_REGISTRATION_URL = "/hpc-server/v2/registration";
	static final String DATA_OBJECT_REGISTRATION_URL = "/hpc-server/v2/dataObject";
	Gson gson = new Gson();
	ConfigFileReader configFileReader = new ConfigFileReader();
	TaskHelper taskHelper = new TaskHelper();
	HpcFileLocation sourceLocation = new HpcFileLocation();
	HpcDirectoryScanPathMap pathDTO = new HpcDirectoryScanPathMap();
	List<Map<String, String>> rows;
	HpcBulkMetadataEntries bulkMetadataEntries = new HpcBulkMetadataEntries();
	gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO bulkRequest = new gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO();
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
		HpcBulkMetadataEntry bulkMetadataEntry = new HpcBulkMetadataEntry();
		bulkMetadataEntry.setPath(path);
		bulkMetadataEntry.getPathMetadataEntries().addAll(convertMetadata(path_metadata_rows));
		bulkMetadataEntries.getPathsMetadataEntries().add(bulkMetadataEntry);
	}

	@Given("I add default metadataEntries as")
	public void i_add_default_metadata_entries_as(io.cucumber.datatable.DataTable dataTable) {
		List<Map<String, String>> default_metadata_rows = dataTable.asMaps(String.class, String.class);
		bulkMetadataEntries.getDefaultCollectionMetadataEntries().addAll(convertMetadata(default_metadata_rows));
	}

	@Given("I add fromPath as {string}")
	public void i_add_from_path_as(String fromPath) {
		this.pathDTO.setFromPath(fromPath);
	}

	@Given("I add toPath as {string}")
	public void i_add_to_path_as(String toPath) {
		this.pathDTO.setToPath(toPath);
	}

	@When("I click Register for the file Upload")
	public void i_click_register_for_the_file_upload() {
		System.out.println("----------------------------------------------------------");
		System.out.println("Test Google Cloud bulk Upload");
		System.out.println("----------------------------------------------------------");
		System.out.println("Test Google Cloud bulk Upload");
		setupDataObjectRegistration();
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
		//bulkRequest.setDirectoryScanRegistrationItems(directoryScanRegistrations);
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
		testSuccess = taskHelper.submitBulkRequest("PUT", gson.toJson(bulkRequest), BULK_REGISTRATION_URL);
		System.out.println("----------------------------------------------------------");
		System.out.println("");
	}

	@Then("I get a response of success for the Upload")
	public void i_get_a_response_of_success_for_the_google_cloud_upload() {
		org.junit.Assert.assertEquals(testSuccess, true);
	}

	// Convert to Metadata structures
	private ArrayList<HpcMetadataEntry> convertMetadata(List<Map<String, String>> datarows){
		ArrayList<HpcMetadataEntry> hpcMetadataEntries = new ArrayList<HpcMetadataEntry>();
		for (Map<String, String> columns : datarows) {
			HpcMetadataEntry metadataEntry = new HpcMetadataEntry();
			metadataEntry.setAttribute(columns.get("attribute"));
			metadataEntry.setValue(columns.get("value"));
			hpcMetadataEntries.add(metadataEntry);
		}
		return hpcMetadataEntries;
	}

	private void setupDataObjectRegistration() {
		List<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO> files = new ArrayList<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO>();
		gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO file = new gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO();

		//dataObjectRegistration = new DataObjectRegistration();
		//dataObjectRegistration.setParentCollectionsBulkMetadataEntries(bulkMetadataEntries);
		String totalPath = this.path + "/" + sourceLocation.getFileId();
		//dataObjectRegistration.setPath(totalPath);
		//dataObjectRegistration.setDataObjectMetadataEntries(convertMetaData(rows));
		if (this.source.toUpperCase().equals("GOOGLECLOUD")) {
			HpcStreamingUploadSource googleCloudSource = new HpcStreamingUploadSource();
			googleCloudSource.setSourceLocation(sourceLocation);
			googleCloudSource.setAccessToken(configFileReader.getGoogleCloudToken());
            file.setGoogleCloudStorageUploadSource(googleCloudSource);
			//Path gcFilePath = Paths.get(totalPath);
			//file.setPath(path + "/" + gcFilePath.getFileName());
            file.setPath(path);
            files.add(file);
		} else if (this.source.toUpperCase().equals("AWS")) {
			HpcStreamingUploadSource s3UploadSource = new HpcStreamingUploadSource();
			HpcS3Account s3Account = taskHelper.getAcctAWS();
			s3UploadSource.setAccount(s3Account);
			s3UploadSource.setSourceLocation(sourceLocation);
			file.setS3UploadSource(s3UploadSource);
			file.setCreateParentCollections(true);
			//Path s3FilePath = Paths.get(path);
			//file.setPath(path + "/" + s3FilePath.getFileName());
			file.setPath(path);
			//logger.info(path + "/" + s3FilePath.getFileName());
			files.add(file);
		} else if (this.source.toUpperCase().equals("GLOBUS")) {
			//file.setGlobusUploadSource(sourceLocation);
		
		} else if (this.source.toUpperCase().equals("GOOGLEDRIVE")) {
			sourceLocation.setFileContainerId("MyDrive");
			//Path filePath = Paths.get(globusEndpointFiles.get(googleDriveFileIds.indexOf(fileId)));
			//String fileName = filePath.getFileName().toString();
			HpcStreamingUploadSource googleDriveSource = new HpcStreamingUploadSource();
			googleDriveSource.setSourceLocation(sourceLocation);
			googleDriveSource.setAccessToken(configFileReader.getGoogleDriveAccessToken());
			file.setGoogleDriveUploadSource(googleDriveSource);
			file.setCreateParentCollections(true);
			//file.setPath(path + "/" + fileName);
			file.setPath(path);
			//logger.info(path + "/" + fileName);
			files.add(file);
		} else {
			System.out.println("Unknown Source");
			return;
		}
		bulkRequest.getDataObjectRegistrationItems().addAll(files);
	}

	private void setupDirectoryScanRegistration() {
		System.out.println("----------------------------------------------------------");
		System.out.println("Test Google Cloud bulk Upload");
		this.token = configFileReader.getToken();
        List<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO> folders = new ArrayList<gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO>();  
        gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO folder = new gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO();
		if (this.source.toUpperCase().equals("GOOGLECLOUD")) {
            HpcGoogleScanDirectory googleCloudSource = new HpcGoogleScanDirectory();
            googleCloudSource.setDirectoryLocation(sourceLocation);
            googleCloudSource.setAccessToken(configFileReader.getGoogleCloudToken());
            folder.setGoogleCloudStorageScanDirectory(googleCloudSource);
            folder.setBasePath(path);
			//folders.add(folder);
		} else if (this.source.toUpperCase().equals("GOOGLEDRIVE")) {
			HpcGoogleScanDirectory googleDriveDirectory = new HpcGoogleScanDirectory();
			googleDriveDirectory.setDirectoryLocation(sourceLocation);
			googleDriveDirectory.setAccessToken(configFileReader.getGoogleDriveAccessToken());
            folder.setGoogleDriveScanDirectory(googleDriveDirectory);
            folder.setBasePath(path);
		} else if (this.source.toUpperCase().equals("AWS")) {
			HpcS3ScanDirectory s3Directory = new HpcS3ScanDirectory();
			s3Directory.setAccount(taskHelper.getAcctAWS());
			folder.setS3ScanDirectory(s3Directory);
		} else if (this.source.toUpperCase().equals("GLOBUS")) {
			HpcScanDirectory globusDirectory = new HpcScanDirectory();
			globusDirectory.setDirectoryLocation(sourceLocation);
			folder.setGlobusScanDirectory(globusDirectory);
		} else {
			System.out.println("Unknown Source");
			return;
		}
		String gcPath = sourceLocation.getFileId();
		HpcDirectoryScanPathMap pathDTO = new HpcDirectoryScanPathMap();
		pathDTO.setFromPath(gcPath);
		//Extract the last subdirectory. If there are no subdirectories, FromPath and ToPath will be the same
		String tempPath = gcPath;
		if (gcPath.endsWith("/"))
		  tempPath = gcPath.substring(0, gcPath.length() - 1);
		String gcToPath = tempPath.substring(tempPath.lastIndexOf("/")+1, tempPath.length());
		pathDTO.setToPath(gcToPath);
		folder.setPathMap(pathDTO);

//		if(criteriaType != null && criteriaType.equals("Simple"))
//			folder.setPatternType(HpcPatternType.SIMPLE);
//		else
//			folder.setPatternType(HpcPatternType.REGEX);
//		if(exclude.size() > 0)
//			folder.getExcludePatterns().addAll(exclude);
//		if(include.size() > 0)
//			folder.getIncludePatterns().addAll(include);
//	

		folder.setBulkMetadataEntries(bulkMetadataEntries);
		folders.add(folder);
		bulkRequest.getDirectoryScanRegistrationItems().addAll(folders);
	}

}