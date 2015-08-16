package test.gov.nih.nci.hpc.web;

import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccountType;
import gov.nih.nci.hpc.domain.user.HpcNihAccount;
import gov.nih.nci.hpc.dto.user.HpcUserRegistrationDTO;
import gov.nih.nci.hpc.web.Application;
import gov.nih.nci.hpc.web.HpcResponseErrorHandler;

import java.util.Arrays;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=8080" })
public class HpcUserControllerTest {

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
	public void register() throws Exception {
		HpcUserRegistrationDTO dto = new HpcUserRegistrationDTO();
		HpcNihAccount account = new HpcNihAccount();
		account.setFirstName("Prasad");
		account.setLastName("Konka");
		account.setUserId("konkapv3");
		dto.setNihAccount(account);
		HpcDataTransferAccount trAccount = new HpcDataTransferAccount();
		trAccount.setAccountType(HpcDataTransferAccountType.GLOBUS);
		trAccount.setUsername("pkonka");
		trAccount.setPassword("IpgSvg12!@");
		dto.setDataTransferAccount(trAccount);
		ObjectMapper mapper = new ObjectMapper();
		System.out.println(mapper.writeValueAsString(dto));
		writeXML(dto);

		 RestTemplate restTemplate = new RestTemplate();
	     restTemplate.setErrorHandler(new HpcResponseErrorHandler());
	    HttpHeaders headers = new HttpHeaders();
	    headers.setAccept(Arrays.asList(MediaType.APPLICATION_XML));
	    try{
	    	HttpEntity<String> entity = restTemplate.postForEntity(baseurl, dto, String.class);

		    System.out.println(entity);
	    }
	    catch(org.springframework.web.client.HttpServerErrorException e)
	    {

	    }
	    catch(Exception e)
	    {
	    	e.printStackTrace();
	    }
		/*
		Client client = ClientBuilder.newClient().register(
				ClientResponseLoggingFilter.class);
		Response res = client.target(baseurl).request()
				.post(Entity.entity(dto, MediaType.APPLICATION_XML));
		if (res.getStatus() != 201){
			HpcExceptionDTO errorDTO = res.readEntity(HpcExceptionDTO.class);
			throw new RuntimeException("Failed : HTTP error code : "
					+ res.getStatus());
		}


		Client client = ClientBuilder.newClient();
		WebTarget target = client.target(baseurl);

		Response response =
		target.request(MediaType.APPLICATION_XML_TYPE)
		    .post(Entity.entity(dto,MediaType.APPLICATION_XML_TYPE));

		//Client client = Client.create();
		//javax.ws.rs.client.Client client = ClientBuilder.newBuilder().register(HpcExceptionMessageReader.class).build();
		//WebResource webResource = client
		//   .resource(baseurl);

		//ClientResponse response = webResource.type(MediaType.APPLICATION_XML)
		//		   .post(ClientResponse.class, dto);

		if (response.getStatus() != 200) {
			//HpcExceptionDTO output = response.getEntity(HpcExceptionDTO.class);
			System.out.println("Output from Server .... \n");
			//System.out.println(output);

		   throw new RuntimeException("Failed : HTTP error code : "
			+ response.getStatus());
		}




		// HpcUserRegistrationDTO entity = (HpcUserRegistrationDTO) res
		// .getEntity();
		// ObjectMapper mapper = new ObjectMapper();
		// System.out.println(mapper.writeValueAsString(entity));

	}

	@Test
	public void getUser() throws Exception {
/*
		Client client = ClientBuilder.newClient().register(
				ClientResponseLoggingFilter.class);
		WebTarget resourceTarget = client.target(baseurl + "/konkapv2");
		Invocation invocation = resourceTarget.request(
				MediaType.APPLICATION_XML).buildGet();
		HpcUserDTO response = invocation.invoke(HpcUserDTO.class);
		ObjectMapper mapper = new ObjectMapper();
		System.out.println(mapper.writeValueAsString(response));
*/
	}

	private void writeXML(HpcUserRegistrationDTO dto) {
		try {

			JAXBContext jaxbContext = JAXBContext
					.newInstance(HpcUserRegistrationDTO.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			jaxbMarshaller.marshal(dto, System.out);

		} catch (JAXBException e) {
			e.printStackTrace();
		}

	}
}