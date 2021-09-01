package Download.steps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import Download.pojos.DownloadSyncLocal;
import Download.pojos.DownloadToGlobusType;
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

public class DownloadDataObjectToGlobusSteps {
	
	String tokenStr = "eyJhbGciOiJIUzI1NiJ9.eyJEYXRhTWFuYWdlbWVudEFjY291bnQiOiJ7XCJwYXNzd29yZFwiOlwiZGVhcmRhc2hzcGFya3l0YXJzXCIsXCJpbnRlZ3JhdGVkU3lzdGVtXCI6XCJJUk9EU1wiLFwicHJvcGVydGllc1wiOntcIlBST1hZX05BTUVcIjpcIlwiLFwiUE9SVFwiOlwiMTI0N1wiLFwiREVGQVVMVF9TVE9SQUdFX1JFU09VUkNFXCI6XCJkZW1vUmVzY1wiLFwiSE9NRV9ESVJFQ1RPUllcIjpcIlwiLFwiWk9ORVwiOlwidGVtcFpvbmVcIixcIkhPU1RcIjpcImZzZG1lbC1pcm9kczAxZC5uY2lmY3JmLmdvdlwiLFwiUFJPWFlfWk9ORVwiOlwiXCJ9LFwidXNlcm5hbWVcIjpcInNjaGludGFsXCJ9IiwiVXNlck5hbWUiOiJzY2hpbnRhbCIsIkRhdGFNYW5hZ2VtZW50QWNjb3VudEV4cGlyYXRpb24iOjE2MjcwMTE2MjQyNTYsImV4cCI6MTgwNjk4MjgyNH0.fjnw6bvqcsGtHBLBhDMCF_dTOVV2F_0HCgH4pM5RjsM";
	DownloadToGlobusType downloadToGlobus = new DownloadToGlobusType();
	
	
	@Given("a GlobusFileContainerId")
	public void a_globus_file_container_id() {
		
	}

	@Given("a fileId")
	public void a_file_id() {
	   
	}

	@Given("a destinationOverwrite field")
	public void a_destination_overwrite_field() {
	   
	}



}
