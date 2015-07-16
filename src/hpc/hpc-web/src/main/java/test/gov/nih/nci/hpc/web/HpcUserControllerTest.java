package test.gov.nih.nci.hpc.web;

import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccountType;
import gov.nih.nci.hpc.domain.user.HpcNihAccount;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectRegistrationDTO;
import gov.nih.nci.hpc.dto.user.HpcUserCredentialsDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;
import gov.nih.nci.hpc.dto.user.HpcUserRegistrationDTO;
import gov.nih.nci.hpc.web.Application;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.client.ClientBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
@IntegrationTest({ "server.port=8080" })
public class HpcUserControllerTest {

	@Value("${local.server.port}")
	private int port;
	private final String baseurl = "http://fr-s-hpcdm-gp-d.ncifcrf.gov:8080/hpc-server/user";

	private RestTemplate template;

	@Before
	public void setUp() throws Exception {
		template = new TestRestTemplate();
	}

	@Test
	public void register() throws Exception {
		HpcUserRegistrationDTO dto = new HpcUserRegistrationDTO();
		HpcNihAccount account = new HpcNihAccount();
		account.setFirstName("Prasad");
		account.setLastName("Konka");
		account.setUserId("pkonka3");
		dto.setNihAccount(account);
		HpcDataTransferAccount trAccount = new HpcDataTransferAccount();
		trAccount.setAccountType(HpcDataTransferAccountType.GLOBUS);
		trAccount.setUsername("pkonka");
		trAccount.setPassword("IpgSvg12!@");
		dto.setDataTransferAccount(trAccount);

		Client client = ClientBuilder.newClient().register(
				ClientResponseLoggingFilter.class);
		Response res = client.target(baseurl).request()
				.post(Entity.entity(dto, MediaType.APPLICATION_XML));
		if (res.getStatus() != 201) {
			throw new RuntimeException("Failed : HTTP error code : "
					+ res.getStatus());
		}
		HpcUserRegistrationDTO entity = (HpcUserRegistrationDTO) res
				.getEntity();
		ObjectMapper mapper = new ObjectMapper();
		System.out.println(mapper.writeValueAsString(entity));

	}

	@Test
	public void getUser() throws Exception {

		Client client = ClientBuilder.newClient().register(
				ClientResponseLoggingFilter.class);
		WebTarget resourceTarget = client.target(baseurl + "/konkapv");
		Invocation invocation = resourceTarget.request(
				MediaType.APPLICATION_XML).buildGet();
		HpcUserDTO response = invocation.invoke(HpcUserDTO.class);
		ObjectMapper mapper = new ObjectMapper();
		System.out.println(mapper.writeValueAsString(response));

	}

}