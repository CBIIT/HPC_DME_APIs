package test.gov.nih.nci.hpc.web;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;
import gov.nih.nci.hpc.domain.dataset.HpcFileType;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.dataset.HpcPrimaryMetadataQueryDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;
import gov.nih.nci.hpc.dto.user.HpcUserRegistrationDTO;
import gov.nih.nci.hpc.web.Application;

import java.net.URL;
import java.util.StringTokenizer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
public class HpcDatasetControllerTest {

    @Value("${local.server.port}")
    private int port;

	private URL base;
	private RestTemplate template;

	@Before
	public void setUp() throws Exception {
		this.base = new URL("http://localhost:7737/hpc-server/dataset");
		template = new TestRestTemplate();
	}

	@Test
	public void register() throws Exception {
		
		  HpcDatasetRegistrationDTO dto = new HpcDatasetRegistrationDTO();
		  dto.setName("Set1");
		  dto.setDescription("Set1 description");
		  dto.setComments("Set1 comments");
		  
		  //TODO: Lookup Id
		  String files = "test1.txt,test2.txt";
		  StringTokenizer tokens = new StringTokenizer(files, ",");
		  while(tokens.hasMoreTokens())
		  {
			  HpcFileUploadRequest upload = new HpcFileUploadRequest();
			  HpcFileType fileType;
			  HpcDataTransferLocations locations = new HpcDataTransferLocations();
			  HpcFileLocation source = new HpcFileLocation();
			  source.setEndpoint("pkonka#hpc1234");
			  String filePath = tokens.nextToken();
			  source.setPath(filePath);
			  HpcFileLocation destination = new HpcFileLocation();
			  destination.setEndpoint("nihfnlcr#gridftp1");
			  destination.setPath(filePath);
			  locations.setDestination(destination);
			  locations.setSource(source);
			  upload.setLocations(locations);
			  //TODO: Identify file type
			  upload.setType(HpcFileType.UNKONWN);

			  //TODO: Metadata funding organization
			  HpcFilePrimaryMetadata metadata = new HpcFilePrimaryMetadata();
			  metadata.setDataEncrypted(false);
			  metadata.setDataContainsPII(false);
			  metadata.setDataCompressed(false);
			  metadata.setDataContainsPHI(false);
			  metadata.setFundingOrganization("NCI");
			  metadata.setPrimaryInvestigatorNihUserId("konkapv");
			  metadata.setRegistrarNihUserId("konkapv");
			  metadata.setDescription("Description");
			  //TODO: ID Lookup
			  metadata.setCreatorName("PRasad Konka");
			  metadata.setLabBranch("CCR");
			  upload.setMetadata(metadata);
			  dto.getUploadRequests().add(upload);
		  }		
			Client client = ClientBuilder.newClient().register(ClientResponseLoggingFilter.class);
			Response res = client
					.target("http://localhost:7737/hpc-server/dataset")
					.request()
					.post(Entity.entity(dto, MediaType.APPLICATION_XML));
			if (res.getStatus() != 201) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ res.getStatus());
			}						
			String datasetId = (String)res.getEntity();	
			System.out.println("datasetId: "+datasetId);
	}
	
	@Test
	public void getDatasetById() throws Exception {
			Client client = ClientBuilder.newClient().register(ClientResponseLoggingFilter.class);
			WebTarget resourceTarget = client.target("http://localhost:7737/hpc-server/dataset/abcd");
			Invocation invocation = resourceTarget.request(MediaType.APPLICATION_XML).buildGet();
			HpcDatasetDTO response = invocation.invoke(HpcDatasetDTO.class);
			System.out.println(response);		  
	}	
	
	@Test
	public void getDatasetByRegistrator() throws Exception {
		Client client = ClientBuilder.newClient().register(ClientResponseLoggingFilter.class);
		WebTarget resourceTarget = client.target("http://localhost:7737/hpc-server/dataset/registrar/konkapv");
		Invocation invocation = resourceTarget.request(MediaType.APPLICATION_XML).buildGet();
		HpcDatasetDTO response = invocation.invoke(HpcDatasetDTO.class);
		System.out.println(response);		  
	}
	
	@Test
	public void getDatasetByMetadata() throws Exception {
		  HpcPrimaryMetadataQueryDTO dto = new HpcPrimaryMetadataQueryDTO();
		  HpcFilePrimaryMetadata metadata = new HpcFilePrimaryMetadata();
		  metadata.setDataEncrypted(false);
		  metadata.setDataContainsPII(false);
		  metadata.setFundingOrganization("NCI");
		  metadata.setPrimaryInvestigatorNihUserId("konkapv");
		  metadata.setRegistrarNihUserId("konkapv");
		  metadata.setDescription("Description");
		  //TODO: ID Lookup
		  metadata.setCreatorName("PRasad Konka");
		  metadata.setLabBranch("CCR");
		  
		Client client = ClientBuilder.newClient().register(ClientResponseLoggingFilter.class);
		WebTarget resourceTarget = client.target("http://localhost:7737/hpc-server/dataset/query/primaryMetadata").register(dto);
		Invocation invocation = resourceTarget.request(MediaType.APPLICATION_XML).buildGet();
		HpcDatasetDTO response = invocation.invoke(HpcDatasetDTO.class);
		System.out.println(response);		  
	}	
	
	@Test
	public void getDatasetTransferStatus() throws Exception {
		Client client = ClientBuilder.newClient().register(ClientResponseLoggingFilter.class);
		WebTarget resourceTarget = client.target("http://localhost:7737/hpc-server/dataset/abcd");
		Invocation invocation = resourceTarget.request(MediaType.APPLICATION_XML).buildGet();
		HpcDatasetDTO response = invocation.invoke(HpcDatasetDTO.class);
		System.out.println(response);		
	}	
}