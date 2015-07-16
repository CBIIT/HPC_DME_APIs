package test.gov.nih.nci.hpc.web;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;
import gov.nih.nci.hpc.domain.dataset.HpcFileType;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.dataset.HpcPrimaryMetadataQueryDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;
import gov.nih.nci.hpc.dto.user.HpcUserRegistrationDTO;
import gov.nih.nci.hpc.web.Application;

import java.net.URL;
import java.util.List;
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

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=8080" })
public class HpcDatasetControllerTest {

	@Value("${local.server.port}")
	private int port;
	//private final String baseurl = "http://fr-s-hpcdm-gp-d.ncifcrf.gov:8080/hpc-server/dataset";
	private final String baseurl = "http://localhost:7737/hpc-server/dataset";

	private RestTemplate template;

	@Before
	public void setUp() throws Exception {
		template = new TestRestTemplate();
	}

	@Test
	public void register() throws Exception {

		HpcDatasetRegistrationDTO dto = new HpcDatasetRegistrationDTO();
		dto.setName("Set1");
		dto.setDescription("Set1 description");
		dto.setComments("Set1 comments");

		// TODO: Lookup Id
		String files = "/~/C/globus-data/test2.fastq,/~/C/globus-data/test.fastq.docx";
		StringTokenizer tokens = new StringTokenizer(files, ",");
		while (tokens.hasMoreTokens()) {
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
			// TODO: Identify file type
			upload.setType(HpcFileType.UNKONWN);

			// TODO: Metadata funding organization
			HpcFilePrimaryMetadata metadata = new HpcFilePrimaryMetadata();
			metadata.setDataEncrypted(false);
			metadata.setDataContainsPII(false);
			metadata.setDataCompressed(false);
			metadata.setDataContainsPHI(false);
			metadata.setFundingOrganization("NCI");
			metadata.setPrimaryInvestigatorNihUserId("konkapv");
			metadata.setRegistrarNihUserId("konkapv");
			metadata.setDescription("Description");
			// TODO: ID Lookup
			metadata.setCreatorName("PRasad Konka");
			metadata.setLabBranch("CCR");
			upload.setMetadata(metadata);
			dto.getUploadRequests().add(upload);
		}
		try {
			Client client = ClientBuilder.newClient().register(
					ClientResponseLoggingFilter.class);
			Response res = client
					.target(baseurl)
					.request()
					.post(Entity.entity(dto, MediaType.APPLICATION_XML));
			if (res.getStatus() != 201) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ res.getStatus());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Test
	public void getDatasetById() throws Exception {
		try {
			Client client = ClientBuilder.newClient().register(
					ClientResponseLoggingFilter.class);
			WebTarget resourceTarget = client
					.target(baseurl+"/9be37e99-3736-4d76-b721-7da25958e00f");
			Invocation invocation = resourceTarget.request(
					MediaType.APPLICATION_XML).buildGet();
			HpcDatasetDTO response = invocation.invoke(HpcDatasetDTO.class);
			ObjectMapper mapper = new ObjectMapper();
			System.out.println(mapper.writeValueAsString(response));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Test
	public void getDatasetsByRegistrator() throws Exception {
		try {
			Client client = ClientBuilder.newClient().register(
					ClientResponseLoggingFilter.class);
			WebTarget resourceTarget = client
					.target(baseurl+"/query/registrar/konkapv");
			Invocation invocation = resourceTarget.request(
					MediaType.APPLICATION_XML).buildGet();
			HpcDatasetCollectionDTO response = invocation.invoke(HpcDatasetCollectionDTO.class);
			ObjectMapper mapper = new ObjectMapper();
			System.out.println(mapper.writeValueAsString(response));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Test
	public void getDatasetByName() throws Exception {
		try {
			Client client = ClientBuilder.newClient().register(
					ClientResponseLoggingFilter.class);
			WebTarget resourceTarget = client
					.target(baseurl+"/query/name/Set1");
			Invocation invocation = resourceTarget.request(
					MediaType.APPLICATION_XML).buildGet();
			HpcDatasetCollectionDTO response = invocation.invoke(HpcDatasetCollectionDTO.class);
			ObjectMapper mapper = new ObjectMapper();
			System.out.println(mapper.writeValueAsString(response));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	
	@Test
	public void getDatasetsByTransferStatus() throws Exception {
		try {
			Client client = ClientBuilder.newClient().register(
					ClientResponseLoggingFilter.class);
			WebTarget resourceTarget = client
					.target(baseurl+"/query/transferStatus/IN_PROGRESS");
			Invocation invocation = resourceTarget.request(
					MediaType.APPLICATION_XML).buildGet();
			HpcDatasetCollectionDTO response = invocation.invoke(HpcDatasetCollectionDTO.class);
			ObjectMapper mapper = new ObjectMapper();
			System.out.println(mapper.writeValueAsString(response));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Test
	public void getDatasetsByPrimaryMetadata() throws Exception {
		HpcPrimaryMetadataQueryDTO dto = new HpcPrimaryMetadataQueryDTO();
		HpcFilePrimaryMetadata metadata = new HpcFilePrimaryMetadata();
		metadata.setFundingOrganization("NCI");
		metadata.setPrimaryInvestigatorNihUserId("konkapv");
		metadata.setLabBranch("CCR");
		dto.setMetadata(metadata);
		try {
			Client client = ClientBuilder.newClient().register(
					ClientResponseLoggingFilter.class);
			HpcDatasetCollectionDTO response = client
					.target(baseurl+"/query/primaryMetadata")
					.request()
					.post(Entity.entity(dto, MediaType.APPLICATION_XML), HpcDatasetCollectionDTO.class);
			
			ObjectMapper mapper = new ObjectMapper();
			System.out.println(mapper.writeValueAsString(response));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Test
	public void getDatasetTransferStatus() throws Exception {
		try {
			Client client = ClientBuilder.newClient().register(
					ClientResponseLoggingFilter.class);
			WebTarget resourceTarget = client
					.target(baseurl+"/8e687827-4906-45e9-9e3e-2cb682362280");
			Invocation invocation = resourceTarget.request(
					MediaType.APPLICATION_XML).buildGet();
			HpcDatasetDTO response = invocation.invoke(HpcDatasetDTO.class);
			List<HpcDataTransferRequest> uploadRequests = response.getUploadRequests();
			ObjectMapper mapper = new ObjectMapper();
			System.out.println(mapper.writeValueAsString(uploadRequests));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}