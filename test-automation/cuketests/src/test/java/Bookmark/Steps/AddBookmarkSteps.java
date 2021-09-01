package Bookmark.Steps;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import Bookmark.Pojo.BookmarkPojo;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.messages.internal.com.google.gson.Gson;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;


public class AddBookmarkSteps {
	
	BookmarkPojo bookmark = new BookmarkPojo();
	String bookmarkName;
	
	
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
	    String token = "xyz";
		RestAssured.baseURI = "https://fsdmel-dsapi01d.ncifcrf.gov/";
	    RestAssured.port = 7738;
	    RequestSpecification request = RestAssured.given().relaxedHTTPSValidation();
		request.header("Accept", "application/json");
		request.header("Authorization", "Bearer "+ token);
		request.header("Content-Type", "application/json");
	    
		 Gson gson = new Gson();
		 String bookmarkJson = gson.toJson(this.bookmark);
		 System.out.println(bookmarkJson);


		 System.out.println("Sending request!!");
		 Response response = request.body(bookmarkJson).put(this.bookmarkName);
		 System.out.println(response.asString());
		 System.out.println(response.getBody());
		 System.out.println(response.getStatusCode());
		 int statuscode = response.getStatusCode();
}

	@Then("I verify the status of {string}")
	public void i_verify_the_status_of(String string) {
	    // Write code here that turns the phrase above into concrete actions
	    //throw new io.cucumber.java.PendingException();
	}

	
	
	/*@Given("I have the bookmark data and add a bookmark")
	public void i_have_the_bookmark_data_and_add_a_bookmark() {
	    // Write code here that turns the phrase above into concrete actions
	    throw new io.cucumber.java.PendingException();
	}

	@Then("I verify the status")
	public void i_verify_the_status(io.cucumber.datatable.DataTable dataTable) {
	    // Write code here that turns the phrase above into concrete actions
	    // For automatic transformation, change DataTable to one of
	    // E, List<E>, List<List<E>>, List<Map<K,V>>, Map<K,V> or
	    // Map<K, List<V>>. E,K,V must be a String, Integer, Float,
	    // Double, Byte, Short, Long, BigInteger or BigDecimal.
	    //
	    // For other transformations you can register a DataTableType.
	    throw new io.cucumber.java.PendingException();
	}*/

}
