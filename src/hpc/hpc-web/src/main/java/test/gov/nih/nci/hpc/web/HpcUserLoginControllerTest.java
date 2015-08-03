package test.gov.nih.nci.hpc.web;

import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectRegistrationDTO;
import gov.nih.nci.hpc.dto.user.HpcUserCredentialsDTO;
import gov.nih.nci.hpc.dto.user.HpcUserRegistrationDTO;
import gov.nih.nci.hpc.web.Application;

import java.net.URL;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
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
public class HpcUserLoginControllerTest {

    @Value("${local.server.port}")
    private int port;

	//private final String baseurl = "http://fr-s-hpcdm-gp-d.ncifcrf.gov:8080/hpc-server/user";
    private final String baseurl = "http://localhost:7737/hpc-server/user";
    
	private RestTemplate template;

	@Before
	public void setUp() throws Exception {
		template = new TestRestTemplate();
	}

	@Test
	public void login() throws Exception {
		  HpcUserCredentialsDTO dto = new HpcUserCredentialsDTO();
		  dto.setUserName("konkapv");
		  dto.setPassword("xyz");

		  
		  try
		  {
				Client client = ClientBuilder.newClient().register(ClientResponseLoggingFilter.class);
				Response res = client
						.target(baseurl+"/authenticate")
						.request()
						.post(Entity.entity(dto, MediaType.APPLICATION_XML));
				if (res.getStatus() != 200) {
					throw new RuntimeException("Failed : HTTP error code : "
							+ res.getStatus());
				}						
		  }
		  catch(Exception e)
		  {
			  e.printStackTrace();
		  }
	}
}