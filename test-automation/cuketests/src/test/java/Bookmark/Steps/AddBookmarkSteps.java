package Bookmark.Steps;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import Bookmark.Pojo.BookmarkPojo;
import common.TaskHelper;
import dataProviders.ConfigFileReader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class AddBookmarkSteps {

	ConfigFileReader configFileReader = new ConfigFileReader();
	Gson gson = new Gson();
	TaskHelper taskHelper = new TaskHelper();
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

	@Given("bookmark name of {string}")
	public void bookmark_name_of(String bookmarkNameStr) {
		this.bookmarkName = "/hpc-server/bookmark/" + bookmarkNameStr;
	}

	@When("I add the bookmark")
	public void i_add_the_bookmark() {
		System.out.println("----------------------------------------------------------");
		System.out.println("Test Add Bookmark");
		taskHelper.submitRequest("PUT", gson.toJson(bookmark), this.bookmarkName);
		System.out.println("----------------------------------------------------------");
		System.out.println("");
	}

	@When("I update the bookmark")
	public void i_update_the_bookmark() {
		System.out.println("----------------------------------------------------------");
		System.out.println("Test Update Bookmark");
		taskHelper.submitRequest("POST", gson.toJson(bookmark), this.bookmarkName);
		System.out.println("----------------------------------------------------------");
		System.out.println("");
	}

	@When("I delete the bookmark")
	public void i_delete_the_bookmark() {
		System.out.println("----------------------------------------------------------");
		System.out.println("Test delete Bookmark");
		taskHelper.submitBulkRequest("DELETE", gson.toJson(bookmark), this.bookmarkName);
		System.out.println("----------------------------------------------------------");
		System.out.println("");
	}

	@Then("I verify the status of {string}")
	public void i_verify_the_status_of(String status) {
		// org.junit.Assert.assertEquals(201, this.statusCode);
	}

}
