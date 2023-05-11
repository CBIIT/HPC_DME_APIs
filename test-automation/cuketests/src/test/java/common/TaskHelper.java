package common;

import static io.restassured.RestAssured.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.*;

import Register.Pojo.BulkDataObjectRegisterPojo;
import common.JsonHelper;
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

public class TaskHelper {
  
  public void submitBulkRequest(BulkDataObjectRegisterPojo bulkRequest, String token) {
    Gson gson = new Gson();
    RestAssured.baseURI = "https://fsdmel-dsapi01d.ncifcrf.gov/";
    RestAssured.port = 7738;
    RequestSpecification request = RestAssured.given().log().all().
        relaxedHTTPSValidation().
        header("Accept", "application/json").
        header("Authorization", "Bearer "+ token).
        header("Content-Type", "application/json").
        body(gson.toJson(bulkRequest));
    Response response = request.put("/hpc-server/v2/registration");
    int statusCode = response.getStatusCode();
    if (statusCode == 200 || statusCode == 201) {
      System.out.println("The Registration was submitted succesfully.");
      System.out.println("StatusCode = " + response.getStatusCode());
      System.out.println(response.getBody());
      JsonPath jsonPath= response.jsonPath();
      String taskId = jsonPath.get("taskId").toString();
      System.out.println("Monitoring task id: " + taskId);
      int i = 0;
      boolean inProgress = true;
      boolean taskFailed = false;
      do {
          request = RestAssured.given().//log().all().
          relaxedHTTPSValidation().
          header("Accept", "application/json").
          header("Authorization", "Bearer "+ token).
          header("Content-Type", "application/json").
          body(gson.toJson(bulkRequest));
          response = request.get("/hpc-server/v2/registration/" + taskId);
          statusCode = response.getStatusCode();
          System.out.println(statusCode);
          System.out.println(response.getBody().asString());
          jsonPath= response.jsonPath();
          Object task = jsonPath.get("task.failedItems.message");
          System.out.println("Printing extracted task");
          System.out.println(task);
          Object failedItems = jsonPath.get("task.failedItems.message");;
          if(failedItems == null || failedItems.toString().equals("")) {
              System.out.println("Printing inprogress");
            inProgress = jsonPath.get("inProgress");
            System.out.println(inProgress);
            try {
              Thread.sleep(10000);  //1000 milliseconds is one second.
            } catch(InterruptedException ex) {
              Thread.currentThread().interrupt();
            }
          } else {
            taskFailed = true;
            System.out.println("Failed set to true");
            Object message = jsonPath.get("task.failedItems.message");;
            System.out.println(message.toString());
            break;
          }
          i++;
           System.out.println(i);
      } while(i < 10 && inProgress && !taskFailed);
    } else {
      System.out.println("This test was a failure!");
      JsonPath jsonPath= response.jsonPath();
System.out.println(statusCode);
System.out.println(response.getBody());
      String errorType = jsonPath.get("errorType");
      System.out.println(errorType);
    }
  }
}