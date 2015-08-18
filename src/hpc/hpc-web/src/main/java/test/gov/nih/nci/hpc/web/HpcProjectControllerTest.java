package test.gov.nih.nci.hpc.web;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;
import gov.nih.nci.hpc.domain.dataset.HpcFileType;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.metadata.HpcCompressionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcEncryptionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcPHIContent;
import gov.nih.nci.hpc.domain.metadata.HpcPIIContent;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcProjectType;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectCollectionDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectRegistrationDTO;
import gov.nih.nci.hpc.web.Application;

import java.util.StringTokenizer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

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
public class HpcProjectControllerTest {

	@Value("${local.server.port}")
	private int port;
	//private final String baseurl = "http://fr-s-hpcdm-gp-d.ncifcrf.gov:8080/hpc-server/project";
	private final String baseurl = "http://localhost:7737/hpc-server/project";

	private RestTemplate template;

	@Before
	public void setUp() throws Exception {
		template = new TestRestTemplate();
	}

	@Test
	public void register() throws Exception {
		HpcProjectRegistrationDTO dto = new HpcProjectRegistrationDTO();

		HpcProjectMetadata metadata = new HpcProjectMetadata();
		metadata.setName("Project1004");
		metadata.setDescription("Description");
		metadata.setDoc("Division");
		metadata.setType(HpcProjectType.SEQUENCING);
		metadata.setInternalProjectId("Iproject");
		metadata.setLabBranch("CCR");
		metadata.setOrganizationalStructure("Org");
		metadata.setPrimaryInvestigatorNihUserId("konkapv");
		metadata.setRegistrarNihUserId("konkapv");
		dto.setMetadata(metadata);
		dto.getHpcDatasetRegistrationDTO().add(getDataset());
		ObjectMapper mapper = new ObjectMapper();
		 System.out.println(mapper.writeValueAsString(dto));		
		Client client = ClientBuilder.newClient().register(
				ClientResponseLoggingFilter.class);
		Response res = client
				.target(baseurl).request()
				.post(Entity.entity(dto, MediaType.APPLICATION_XML));
		if (res.getStatus() != 201) {
			throw new RuntimeException("Failed : HTTP error code : "
					+ res.getStatus());
		}
		JAXBContext jaxbContext = JAXBContext
				.newInstance(HpcProjectRegistrationDTO.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

		// output pretty printed
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		jaxbMarshaller.marshal(dto, System.out);		
	}

	private HpcDatasetRegistrationDTO getDataset()
	{
		HpcDatasetRegistrationDTO dto = new HpcDatasetRegistrationDTO();
		dto.setName("Data1002-Set2");
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
			metadata.setDataEncrypted(HpcEncryptionStatus.NOT_ENCRYPTED);
			metadata.setDataContainsPII(HpcPIIContent.PII_NOT_PRESENT);
			metadata.setDataCompressed(HpcCompressionStatus.NOT_COMPRESSED);
			metadata.setDataContainsPHI(HpcPHIContent.PHI_NOT_PRESENT);
			metadata.setFundingOrganization("NCI");
			metadata.setPrimaryInvestigatorNihUserId("konkapv");
			metadata.setRegistrarNihUserId("konkapv");
			metadata.setDescription("Description");
			// TODO: ID Lookup
			metadata.setCreatorName("Prasad Konka");
			metadata.setLabBranch("CCR");
			upload.setMetadata(metadata);
			dto.getUploadRequests().add(upload);
		}		
		return dto;
	}
	@Test
	public void getProjectById() throws Exception {
		try {
			Client client = ClientBuilder.newClient().register(
					ClientResponseLoggingFilter.class);
			WebTarget resourceTarget = client
					.target(baseurl+"/1815ee84-cfc9-4681-bc7e-bfafac05057d");
			Invocation invocation = resourceTarget.request(
					MediaType.APPLICATION_XML).buildGet();
			HpcProjectDTO response = invocation.invoke(HpcProjectDTO.class);
			 ObjectMapper mapper = new ObjectMapper();
			 System.out.println(mapper.writeValueAsString(response));

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to get Project by Registrator");
		}
	}

	@Test
	public void getProjectsByRegistrar() throws Exception {
		try {
			Client client = ClientBuilder.newClient().register(
					ClientResponseLoggingFilter.class);
			WebTarget resourceTarget = client
					.target(baseurl+"/query/registrar/konkapv");
			Invocation invocation = resourceTarget.request(
					MediaType.APPLICATION_XML).buildGet();
			HpcProjectCollectionDTO response = invocation.invoke(HpcProjectCollectionDTO.class);
			 ObjectMapper mapper = new ObjectMapper();
			 System.out.println(mapper.writeValueAsString(response));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to get Project by Registrator");
		}
	}

	@Test
	public void getProjectsByInvestigator() throws Exception {
		try {
			Client client = ClientBuilder.newClient().register(
					ClientResponseLoggingFilter.class);
			WebTarget resourceTarget = client
					.target(baseurl+"/query/pi/konkapv");
			Invocation invocation = resourceTarget.request(
					MediaType.APPLICATION_XML).buildGet();
			HpcProjectCollectionDTO response = invocation.invoke(HpcProjectCollectionDTO.class);
			 ObjectMapper mapper = new ObjectMapper();
			 System.out.println(mapper.writeValueAsString(response));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to get Project by Registrator");
		}
	}
}