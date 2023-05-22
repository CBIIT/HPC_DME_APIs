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
import dataProviders.ConfigFileReader;
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
	
    ConfigFileReader configFileReader;
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
