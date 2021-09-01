package Download.steps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import Download.pojos.DownloadSyncLocal;
import Download.pojos.SyncType;

import java.io.*;

import Register.Pojo.ParentMetadataPojo;
import Register.Pojo.RegisterPojo;
import common.JsonHelper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.datatable.DataTable;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import junit.framework.Assert;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;


public class SyncDownloadSteps {
	
	String tokenStr = "eyJhbGciOiJIUzI1NiJ9.eyJEYXRhTWFuYWdlbWVudEFjY291bnQiOiJ7XCJwYXNzd29yZFwiOlwiZGVhcmRhc2hzcGFya3l0YXJzXCIsXCJpbnRlZ3JhdGVkU3lzdGVtXCI6XCJJUk9EU1wiLFwicHJvcGVydGllc1wiOntcIlBST1hZX05BTUVcIjpcIlwiLFwiUE9SVFwiOlwiMTI0N1wiLFwiREVGQVVMVF9TVE9SQUdFX1JFU09VUkNFXCI6XCJkZW1vUmVzY1wiLFwiSE9NRV9ESVJFQ1RPUllcIjpcIlwiLFwiWk9ORVwiOlwidGVtcFpvbmVcIixcIkhPU1RcIjpcImZzZG1lbC1pcm9kczAxZC5uY2lmY3JmLmdvdlwiLFwiUFJPWFlfWk9ORVwiOlwiXCJ9LFwidXNlcm5hbWVcIjpcInNjaGludGFsXCJ9IiwiVXNlck5hbWUiOiJzY2hpbnRhbCIsIkRhdGFNYW5hZ2VtZW50QWNjb3VudEV4cGlyYXRpb24iOjE2MjcwMTE2MjQyNTYsImV4cCI6MTgwNjk4MjgyNH0.fjnw6bvqcsGtHBLBhDMCF_dTOVV2F_0HCgH4pM5RjsM";
	DownloadSyncLocal downloadSyncBody = new DownloadSyncLocal();
	SyncType syncType = new SyncType();

	@Given("I have a compressedArchiveType as {string}")
	public void i_have_a_compressed_archive_type_as(String ca_type) {
		this.syncType.setCompressedArchiveType(ca_type);
		downloadSyncBody.setSynchronousDownloadFilter(this.syncType);
	}
	
	@Given("I have includePatterns as")
	public void i_have_include_patterns_as(io.cucumber.datatable.DataTable dataTable) {
		List<String> tablerows = dataTable.asList(String.class);
		//List<String> list=new ArrayList<String>();  
		//list.add("sampelnotes.rtfd");
	   this.syncType.setIncludePatterns(tablerows);
	   downloadSyncBody.setSynchronousDownloadFilter(this.syncType);
	}
	
	@When("I click Download")
	public void i_click_download() {
	    		
		
		String token = this.tokenStr;
		String downloadBodyJson = new JsonHelper().getPrettyJson((Object)this.downloadSyncBody);
		RestAssured.baseURI = "https://fsdmel-dsapi01t.ncifcrf.gov/";
		//https://fsdmel-dsapi01t.ncifcrf.gov/
	    RestAssured.port = 7738;
	    RequestSpecification request = RestAssured.given().log().all().
			relaxedHTTPSValidation().
			header("Accept", "application/json").
			header("Authorization", "Bearer "+ token);
	    
	     request.header("Content-Type", "application/json");
		 System.out.println("Sending request!!");
		 Response response = request.body(downloadBodyJson).log().all().post("/hpc-server/v2/dataObject/FNL_SF_Archive/udittest/Uditproject/FlowcellUdit/2_27.zip/download");
			
		 System.out.println(response.asString());
		 System.out.println(response.getBody());
		 System.out.println(response.getStatusCode());
		 int statuscode = response.getStatusCode();
	    
	}

}

