package test.gov.nih.nci.hpc.web;

import static org.junit.Assert.assertThat;
import gov.nih.nci.hpc.domain.HpcDataset;
import gov.nih.nci.hpc.domain.HpcDatasetLocation;
import gov.nih.nci.hpc.dto.HpcDataRegistrationInput;
import gov.nih.nci.hpc.web.Application;

import java.net.URL;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest({"server.port=8080"})
public class HpcDataRegistrationControllerTest {

    @Value("${local.server.port}")
    private int port;

	private URL base;
	private RestTemplate template;

	@Before
	public void setUp() throws Exception {
		this.base = new URL("http://localhost:" + port + "/registration");
		template = new TestRestTemplate();
	}

	@Test
	public void register() throws Exception {
		  HpcDataRegistrationInput input = new HpcDataRegistrationInput();
		  input.setInvestigatorName("PI1");
		  input.setProjectName("Project 1");
		  HpcDataset dataset = new HpcDataset();
		  HpcDatasetLocation source = new HpcDatasetLocation();
		  source.setEndpoint("testendpoint");
		  source.setFilePath("testpath");
		  dataset.setSource(source);
		  List<HpcDataset> sets = input.getDatasets();
		  sets.add(dataset);
		  try
		  {
			  String response = template.postForObject( base.toString(), input, String.class);
		  }
		  catch(Exception e)
		  {
			  e.printStackTrace();
		  }
	}
}