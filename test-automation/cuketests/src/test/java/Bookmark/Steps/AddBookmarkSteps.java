package Bookmark.Steps;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import Bookmark.Pojo.BookmarkPojo;
import dataProviders.ConfigFileReader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.messages.internal.com.google.gson.Gson;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;


public class AddBookmarkSteps {
	
    ConfigFileReader configFileReader;
	BookmarkPojo bookmark = new BookmarkPojo();
	String bookmarkName;
	int statusCode;
	
	
	@Given("I have a path of {string}")
	public void i_have_a_path_of(String path) {
		this.bookmark.setPath(path);
	}

	@Given("userId of {string}")
	public void user_id_of(String userId) {
	    this.bookmark.setUserId(userId);
	}

	@Given("permission of {string}")
	public void permission_of(String permission) {
	    this.bookmark.setPermission(permission);
	}

	@Given("bookmarkName of {string}")
	public void bookmark_name_of(String bookmarkNameStr) {
	    this.bookmarkName = "/hpc-server/bookmark/" + bookmarkNameStr;
	}

	@When("I add the bookmark")
	public void i_add_the_bookmark() {
	    configFileReader= new ConfigFileReader();
	    String token = configFileReader.getToken();
		RestAssured.baseURI = "https://fsdmel-dsapi01d.ncifcrf.gov/";
	    RestAssured.port = 7738;
	    RequestSpecification request = RestAssured.given().relaxedHTTPSValidation();
		request.header("Accept", "application/json");
		request.header("Authorization", "Bearer "+ token);
		request.header("Content-Type", "application/json");
	    
		 Gson gson = new Gson();
		 String bookmarkJson = gson.toJson(this.bookmark);
		 System.out.println(bookmarkJson);


		 System.out.println("Sending request to add a bookmark!!");
		 Response response = request.body(bookmarkJson).put(this.bookmarkName);
		 //System.out.println(response.asString());
		 //System.out.println(response.getBody());
		 System.out.println(response.getStatusCode());
		 this.statusCode = response.getStatusCode();

		this.statusCode = response.getStatusCode();
	    
}

	@Then("I verify the status of {string}")
	public void i_verify_the_status_of(String status) {
		org.junit.Assert.assertEquals(201, this.statusCode);
		if (this.statusCode == 200 || this.statusCode == 201) {
          System.out.println("This test was a success");
          System.out.println("StatusCode = " + this.statusCode);
        } else {
          System.out.println("This test was a failure");
          System.out.println("StatusCode = " + this.statusCode);
        }
	}

}
