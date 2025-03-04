package gov.nih.nci.hpc.test.common;

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
import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaAccount;
import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcStreamingUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadSource;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.test.common.ErrorMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

//import Register.Pojo.DataObjectRegistration;
//import Register.Pojo.S3AccountPojo;
//import Register.Pojo.S3StreamingUploadPojo;
//import Register.Pojo.SourceLocationPojo;
import gov.nih.nci.hpc.test.dataProviders.ConfigFileReader;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import org.junit.Assert;

public class TaskHelper {
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	public HpcDataObjectRegistrationItemDTO setupAuthorizeAWS(HpcFileLocation sourceLocation, String path) {
		ConfigFileReader configFileReader = new ConfigFileReader();
		HpcStreamingUploadSource s3Info = new HpcStreamingUploadSource();
		s3Info.setAccount(getAcctAWS());
		s3Info.setSourceLocation(sourceLocation);
		HpcDataObjectRegistrationItemDTO dataObjectRegistration = new HpcDataObjectRegistrationItemDTO();
		dataObjectRegistration.setPath(path + "/" + s3Info.getSourceLocation().getFileId());
		dataObjectRegistration.setSource(sourceLocation);
		return dataObjectRegistration;
	}

	public HpcS3Account getAcctAWS() {
		ConfigFileReader configFileReader = new ConfigFileReader();
		HpcS3Account s3Account = new HpcS3Account();
		s3Account.setAccessKey(configFileReader.getAwsAccessKey());
		s3Account.setSecretKey(configFileReader.getAwsSecretKey());
		s3Account.setRegion(configFileReader.getAwsRegion());
		return s3Account;
	}

	public HpcAsperaAccount getAcctAspera() {
		ConfigFileReader configFileReader = new ConfigFileReader();
		HpcAsperaAccount asperaAccount = new HpcAsperaAccount();
		asperaAccount.setUser(configFileReader.getAsperaUser());
		asperaAccount.setPassword(configFileReader.getAsperaPassword());
		asperaAccount.setHost(configFileReader.getAsperaHost());
		return asperaAccount;
	}

	public boolean submitBulkRequest(String requestType, String requestBody, String requestUrl) {
		Gson gson = new Gson();
		ConfigFileReader configFileReader = new ConfigFileReader();
		String token = configFileReader.getToken();
		RestAssured.baseURI = configFileReader.getApplicationUrl();
		RestAssured.port = 7738;
		RequestSpecification request = RestAssured.given().log().all().relaxedHTTPSValidation()
				.header("Accept", "application/json").header("Authorization", "Bearer " + token)
				.header("Content-Type", "application/json").body(requestBody);
		Response response = executeRequest(requestType, request, requestUrl);
		int statusCode = response.getStatusCode();
		// Polling loop
		if (statusCode == 200 || statusCode == 201) {
			System.out.println("The Registration was submitted succesfully.");
			System.out.println("StatusCode = " + response.getStatusCode());
			System.out.println("The response is: " + response.asString());
			JsonPath jsonPath = response.jsonPath();
			String taskId = jsonPath.get("taskId").toString();
			System.out.println("Monitoring task id: " + taskId);
			int pollingIteration = 0;
			boolean inProgress = true;
			boolean taskFailed = false;
			do {
				request = RestAssured.given().// log().all().
						relaxedHTTPSValidation().header("Accept", "application/json")
						.header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
						.body(requestBody);
				response = request.get(requestUrl + "/" + taskId);
				statusCode = response.getStatusCode();
				System.out.println(statusCode);
				System.out.println(response.getBody().asString());
				jsonPath = response.jsonPath();
				System.out.println("JSONPath =" + gson.toJson(jsonPath));
				Object task = jsonPath.get("task.failedItems.message");
				System.out.println("Printing extracted task");
				System.out.println(task);
				Object failedItems = jsonPath.get("task.failedItems.message");
				;
				if (failedItems == null || failedItems.toString().equals("")) {
					System.out.println("Printing inprogress");
					inProgress = jsonPath.get("inProgress");
					System.out.println(inProgress);
					try {
						Thread.sleep(10000); // 1000 milliseconds is one second.
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
				} else {
					taskFailed = true;
					System.out.println("Failed set to true");
					Object message = jsonPath.get("task.failedItems.message");
					System.out.println(message.toString());
					return false;
				}
				pollingIteration++;
				System.out.println("Task polling loop iteration: " + pollingIteration);
			} while (pollingIteration < 10 && inProgress && !taskFailed);
		} else {
			JsonPath jsonPath = response.jsonPath();
			System.out.println(jsonPath);
			logger.error("This test was a failure. ErrorType: " + jsonPath.get("errorType") + ", Error Message: " +
				jsonPath.getString("message") + " , Error Status Code: " + response.getStatusCode());
			return false;
		}
		return true;
	}

	public boolean submitRequestBoolean(String requestType, String requestBody, String requestUrl) {
		Gson gson = new Gson();
		ConfigFileReader configFileReader = new ConfigFileReader();
		String token = configFileReader.getToken();
		RestAssured.baseURI = configFileReader.getApplicationUrl();
		RestAssured.port = 7738;
		RequestSpecification request = RestAssured.given().log().all().relaxedHTTPSValidation()
				.header("Accept", "application/json").header("Authorization", "Bearer " + token)
				.header("Content-Type", "application/json").body(requestBody);
		Response response = executeRequest(requestType, request, requestUrl);
		int statusCode = response.getStatusCode();
		System.out.println("The response status code is: " + statusCode);
		JsonPath jsonPath = response.jsonPath();
		System.out.println("JSONPath =" + gson.toJson(jsonPath));
		//System.out.println("taskId =" + gson.toJson(response.getBody()).get());
		System.out.println("The response is: " + response.getBody().asString());
		if (statusCode == 200 || statusCode == 201) {
			return true;
		} else {
			return false;
		}
	}

	public ErrorMessage submitRequest(String requestType, String requestBody, String requestUrl, String role) {
		Gson gson = new Gson();
		ConfigFileReader configFileReader = new ConfigFileReader();
		String token = configFileReader.getTokenByRole(role);
		RestAssured.baseURI = configFileReader.getApplicationUrl();
		RestAssured.port = 7738;
		RequestSpecification request = RestAssured.given().log().all().relaxedHTTPSValidation()
				.header("Accept", "application/json").header("Authorization", "Bearer " + token)
				.header("Content-Type", "application/json").body(requestBody);
		Response response = executeRequest(requestType, request, requestUrl);
		int statusCode = response.getStatusCode();
		System.out.println("The response status code is: " + statusCode);
		JsonPath jsonPath = response.getBody().jsonPath();
		ErrorMessage errorMessage = new ErrorMessage();
		if (statusCode == 200 || statusCode == 201) {
			errorMessage.errorType = "success";
		} else {
			errorMessage.errorType = jsonPath.get("errorType");
			errorMessage.message = jsonPath.get("message");
			errorMessage.stackTrace = jsonPath.get("stackTrace");
			String errorType = jsonPath.get("errorType");
			if (errorType.equals("REQUEST_REJECTED")) {
				String msg = errorType + "; " + jsonPath.get("requestRejectReason") + errorMessage.message;
				System.out.println("The response is: " + msg);
			} else if (errorType.equals("REQUEST_AUTHENTICATION_FAILED")){
				System.out.println("The response is: REQUEST_AUTHENTICATION_FAILED; " +  errorMessage.message);
				System.out.println("The errorType is: " + errorType);
			} else if (errorType.equals("INVALID_REQUEST_INPUT")){
				System.out.println("The response is: INVALID_REQUEST_INPUT; " +  errorMessage.message);
				System.out.println("The errorType is: " + errorType);
			} else {
				System.out.println("The response is: " + response.getBody().asString());
				System.out.println("The errorType is: " + errorType);
			}
		}
		return errorMessage;
	}

	private Response executeRequest(String requestType, RequestSpecification request, String requestUrl) {
		Response response;
		if (requestType.equals("POST")) {
			 response = request.post(requestUrl);
		} else if (requestType.equals("PUT")) {
			 response = request.put(requestUrl);
		} else if (requestType.equals("DELETE")) {
			response  = request.delete(requestUrl);
		} else if (requestType.equals("GET")) {
			response  = request.get(requestUrl);
		} else {
			System.out.println("Unknown http method");
			return null;
		}
		return response;
	}
	
	public void verifyResponse(String expectedResponseString, ErrorMessage result) {
		if (expectedResponseString.equals("success")) {
			if (!result.getErrorType().equals("success")) {
				if (result.getErrorType().equals("REQUEST_REJECTED")) {
					String msg = result.getErrorType() + "; " + result.getMessage();
					Assert.assertEquals((Object) "success", (Object) msg);
				} else {
					Assert.assertEquals((Object) "success", (Object) result.getStackTrace());
				}
			} else {
				Assert.assertEquals((Object) "success", (Object) result.getErrorType());
			}
		} else {
			String errorType = "REQUEST_AUTHENTICATION_FAILED";
			if (result.getErrorType().equals("REQUEST_AUTHENTICATION_FAILED")) {
				Assert.assertEquals((Object) result.getErrorType(), (Object) "REQUEST_AUTHENTICATION_FAILED");
			} else {
				Assert.assertEquals((Object) result.getErrorType(), (Object) result.getStackTrace());
			}
		}
	}

}