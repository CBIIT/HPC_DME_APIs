package test.gov.nih.nci.hpc.web;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;
import gov.nih.nci.hpc.domain.dataset.HpcFileType;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.metadata.HpcDatasetPrimaryMetadata;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.web.Application;

import java.net.URL;
import java.util.List;
import java.util.StringTokenizer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
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
		  HpcDatasetRegistrationDTO dto = new HpcDatasetRegistrationDTO();
		  dto.setName("Dataset1");
		  //TODO: Lookup Id
		  dto.setPrimaryInvestigatorId("Investigator1");
		  dto.setRegistratorId("konkapv");
		  //TODO: ID Lookup
		  dto.setCreatorId("konkapv");
		  dto.setLabBranch("CBIIT");
		  dto.setDescription("description");
		  dto.setComments("Comments");
		  String files = "test1.txt,test2.txt";
		  StringTokenizer tokens = new StringTokenizer(files, ",");
		  while(tokens.hasMoreTokens())
		  {
			  HpcFileUploadRequest upload = new HpcFileUploadRequest();
			  HpcFileType fileType;
			  HpcDataTransferLocations locations = new HpcDataTransferLocations();
			  HpcFileLocation source = new HpcFileLocation();
			  source.setEndpoint("pkonka#hpc123");
			  String filePath = tokens.nextToken();
			  source.setPath(filePath);
			  HpcFileLocation destination = new HpcFileLocation();
			  destination.setEndpoint("nihfnlcr#gridftp1");
			  destination.setPath(filePath);
			  upload.setLocations(locations);
			  //TODO: Identify file type
			  upload.setType(HpcFileType.UNKONWN);

			  //TODO: Metadata funding organization
			  HpcDatasetPrimaryMetadata metadata = new HpcDatasetPrimaryMetadata();
			  metadata.setDataEncrypted(false);
			  metadata.setDataContainsPII(false);
			  metadata.setFundingOrganization("funding1");
			  upload.setMetadata(metadata);
			  dto.getUploadRequests().add(upload);
		  }		  try
		  {
			  String response = template.postForObject( base.toString(), dto, String.class);
		  }
		  catch(Exception e)
		  {
			  e.printStackTrace();
		  }
	}
}