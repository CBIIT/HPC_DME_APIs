package BulkMetadataUpdate.Steps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import BulkMetadataUpdate.Pojo.BulkMetadataUpdatePojo;
import common.MetadataEntry;
import common.TaskHelper;
import dataProviders.ConfigFileReader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class BulkUpdateMetadataSteps {
	static final String BULK_REGISTRATION_URL = "/hpc-server/metadata";
	BulkMetadataUpdatePojo requestObj = new BulkMetadataUpdatePojo();
	Gson gson = new Gson();
	ConfigFileReader configFileReader = new ConfigFileReader();
	TaskHelper taskHelper = new TaskHelper();


	@Given("I have multiple metadata attributes to update")
	public void i_have_multiple_metadata_attributes_to_update(io.cucumber.datatable.DataTable dataTable) {
		List<Map<String, String>> metadataRows = dataTable.asMaps(String.class, String.class);
		List<MetadataEntry> metadataEntries = new ArrayList<MetadataEntry>();
		for (Map<String, String> columns : metadataRows) {
			MetadataEntry m = new MetadataEntry();
			m.setAttribute(columns.get("attribute"));
			m.setValue(columns.get("value"));
			metadataEntries.add(m);
			requestObj.setMetadataEntries(metadataEntries);
			System.out.println(gson.toJson(requestObj));
		}
	}

	@Given("I have multiple paths to update metadata as")
	public void i_have_multiple_paths_to_update_metadata_as(io.cucumber.datatable.DataTable dataTable) {
		List<String> pathList = dataTable.asList(String.class);
		requestObj.setCollectionPaths(pathList);
	}

	@When("I submit the bulk metadata update")
	public void i_submit_the_bulk_metadata_update() {
		taskHelper.submitRequest("POST", gson.toJson(requestObj), BULK_REGISTRATION_URL);
		
	}

	@Then("I get a response of success for the bulk metadata update")
	public void i_get_a_response_of_success_for_the_bulk_metadata_update() {
	}
}
