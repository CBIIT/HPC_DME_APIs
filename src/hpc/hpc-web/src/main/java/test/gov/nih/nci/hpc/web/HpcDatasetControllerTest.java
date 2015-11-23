package test.gov.nih.nci.hpc.web;

import gov.nih.nci.hpc.domain.dataset.HpcFileType;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcCompressionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcEncryptionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
import gov.nih.nci.hpc.domain.metadata.HpcPHIContent;
import gov.nih.nci.hpc.domain.metadata.HpcPIIContent;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.dataset.HpcFilePrimaryMetadataDTO;
import gov.nih.nci.hpc.web.Application;

import java.util.Calendar;
import java.util.List;
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
public class HpcDatasetControllerTest {

	@Value("${local.server.port}")
	private int port;
	private final String baseurl = "http://fr-s-hpcdm-gp-d.ncifcrf.gov:7737/hpc-server/dataset";
	//private final String baseurl = "http://localhost:7737/hpc-server/dataset";

	private RestTemplate template;

	@Before
	public void setUp() throws Exception {
		template = new TestRestTemplate();
	}

	@Test
	public void register() throws Exception {

		//Dataset registration object
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
					.target(baseurl+"/e133f045-f874-44d0-bd8b-794fedca464d");
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
	public void getDatasetByProjectId() throws Exception {
		try {
			Client client = ClientBuilder.newClient().register(
					ClientResponseLoggingFilter.class);
			WebTarget resourceTarget = client
					.target(baseurl+"/query/project/aa94bc29-f414-48a4-85ec-97a26bc3b35d");
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
	public void getDatasetsByPI() throws Exception {
		try {
			Client client = ClientBuilder.newClient().register(
					ClientResponseLoggingFilter.class);
			WebTarget resourceTarget = client
					.target(baseurl+"/query/pi/konkapv");
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
	public void getDatasetsByRegistrar() throws Exception {
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
					.target(baseurl+"/query/name/Experiment2015");
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
		HpcFilePrimaryMetadataDTO dto = new HpcFilePrimaryMetadataDTO();
		HpcFilePrimaryMetadata metadata = new HpcFilePrimaryMetadata();
		metadata.setDataContainsPII(HpcPIIContent.PII_NOT_PRESENT);
		metadata.setFundingOrganization("NCI");
		metadata.setPrincipalInvestigatorNciUserId("konkapv");
		metadata.setLabBranch("CCR");

		dto.setMetadata(metadata);
		try {
			Client client = ClientBuilder.newClient().register(
					ClientResponseLoggingFilter.class);
			Response response = client
					.target(baseurl+"/query/primaryMetadata")
					.request()
					.post(Entity.entity(dto, MediaType.APPLICATION_XML));
			int status = response.getStatus();

			if (Response.Status.OK.getStatusCode() == status) {
				System.out.println("Got respomse: "+response);
/*
				HpcDatasetCollectionDTO obj = response.readEntity(HpcDatasetCollectionDTO.class);
				ObjectMapper mapper = new ObjectMapper();
				System.out.println(mapper.writeValueAsString(obj));
*/
			} 
			JAXBContext jaxbContext = JAXBContext
					.newInstance(HpcFilePrimaryMetadataDTO.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			jaxbMarshaller.marshal(dto, System.out);
			
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
					.target(baseurl+"/b5d46f5f-af8d-453c-ba2e-9a66fb7da68d");
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
}