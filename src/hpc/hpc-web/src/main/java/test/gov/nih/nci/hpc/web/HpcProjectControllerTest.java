package test.gov.nih.nci.hpc.web;

import gov.nih.nci.hpc.domain.dataset.HpcFileType;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcCompressionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcEncryptionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
import gov.nih.nci.hpc.domain.metadata.HpcPHIContent;
import gov.nih.nci.hpc.domain.metadata.HpcPIIContent;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcProjectType;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectAddMetadataItemsDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectCollectionDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectRegistrationDTO;
import gov.nih.nci.hpc.web.Application;

import java.util.Calendar;
import java.util.StringTokenizer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
		metadata.setPrincipalInvestigatorDOC("Division");
		metadata.setType(HpcProjectType.SEQUENCING);
		metadata.setInternalProjectId("Iproject");
		metadata.setLabBranch("CCR");
		metadata.setRegistrarDOC("Org");
		metadata.setPrincipalInvestigatorNciUserId("konkapv");
		metadata.setRegistrarNciUserId("konkapv");
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
		dto.setName("Dataset2015-7");
		dto.setDescription("Set1 description");
		dto.setComments("Set1 comments");

		// Registering two files
		String files = "/~/C/globus-data/SRR062635.filt.fastq,/~/C/globus-data/test2.fastq";
		StringTokenizer tokens = new StringTokenizer(files, ",");
		while (tokens.hasMoreTokens()) {
			//Create upload request
			HpcFileUploadRequest upload = new HpcFileUploadRequest();
			HpcFileType fileType;
			HpcDataTransferLocations locations = new HpcDataTransferLocations();
			//Set file origin end point
			HpcFileLocation source = new HpcFileLocation();
			source.setEndpoint("pkonka#hpc1234");
			String filePath = tokens.nextToken();
			source.setPath(filePath);
			//Set file destination end point
			HpcFileLocation destination = new HpcFileLocation();
			//destination.setEndpoint("nihnci#NIH-NCI-TRANSFER1");
			destination.setEndpoint("nihfnlcr#gridftp1");
			destination.setPath(filePath);
			locations.setDestination(destination);
			locations.setSource(source);
			upload.setLocations(locations);
			upload.setType(HpcFileType.UNKONWN);

			//Set file metadata
			HpcFilePrimaryMetadata metadata = new HpcFilePrimaryMetadata();
			metadata.setDataEncrypted(HpcEncryptionStatus.NOT_ENCRYPTED);
			metadata.setDataContainsPII(HpcPIIContent.PII_NOT_PRESENT);
			metadata.setDataCompressed(HpcCompressionStatus.NOT_COMPRESSED);
			metadata.setDataContainsPHI(HpcPHIContent.PHI_NOT_PRESENT);
			metadata.setFundingOrganization("NCI");
			metadata.setPrincipalInvestigatorNciUserId("konkapv");
			metadata.setPrincipalInvestigatorDOC("test");
			metadata.setRegistrarNciUserId("konkapv");
			metadata.setDescription("Description");			// TODO: ID Lookup
			metadata.setCreatorName("PRasad Konka");
			metadata.setLabBranch("CCR");
			metadata.setOriginallyCreated(Calendar.getInstance());
			metadata.setRegistrarDOC("test");
			
			upload.setMetadata(metadata);
			
			//Set file custom metadata
			HpcMetadataItem metadataItem1 = new HpcMetadataItem();
			metadataItem1.setKey("CCBR Name");
			metadataItem1.setValue("value1");
			HpcMetadataItem metadataItem2 = new HpcMetadataItem();
			metadataItem2.setKey("key2");
			metadataItem2.setValue("value2");
			metadata.getMetadataItems().add(metadataItem1);
			metadata.getMetadataItems().add(metadataItem2);
			dto.getUploadRequests().add(upload);
			writeXML(dto);
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
	private void writeXML(HpcDatasetRegistrationDTO dto) {
		try {

			JAXBContext jaxbContext = JAXBContext
					.newInstance(HpcDatasetRegistrationDTO.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			jaxbMarshaller.marshal(dto, System.out);

		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void getProjectsByPrimaryMetadata() throws Exception {
		HpcProjectAddMetadataItemsDTO dto = new HpcProjectAddMetadataItemsDTO();
		dto.setProjectId("17e501ad-a0eb-4a21-853c-ba7b363d7d5b");
		HpcMetadataItem item1 = new HpcMetadataItem();
		item1.setKey("key1");
		item1.setValue("value1");
		HpcMetadataItem item2 = new HpcMetadataItem();
		item2.setKey("key2");
		item2.setValue("value2");
		
		try {
			Client client = ClientBuilder.newClient().register(
					ClientResponseLoggingFilter.class);
			Response response = client
					.target(baseurl+"/query/metadata")
					.request()
					.post(Entity.entity(dto, MediaType.APPLICATION_XML));
			int status = response.getStatus();

			if (Response.Status.OK.getStatusCode() == status) {
				System.out.println("Got respomse: "+response);
			} 
			JAXBContext jaxbContext = JAXBContext
					.newInstance(HpcProjectDTO.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			jaxbMarshaller.marshal(dto, System.out);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}	
}