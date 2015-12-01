package gov.nih.nci.hpc.cli;

import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.util.HpcConfigProperties;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

//import org.codehaus.jackson.map.ObjectMapper;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.OverwriteException;
import org.irods.jargon.core.packinstr.TransferOptions;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.domain.AvuData;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.transfer.TransferControlBlock;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class IrodsClient implements HPCClient {
	@Autowired
	private HpcConfigProperties configProperties;

	private IRODSFileSystem irodsFileSystem = null;
	private IRODSAccount account = null;
	private HPCDataObject hpcDataObject;

	public IrodsClient() throws JargonException {
		// initialize
		irodsFileSystem = IRODSFileSystem.instance();

	}

	@Override
	public String processDataObject() {
		String message = validateAddMetadataToObject();

		if (message == null) {
			try {
				if (hpcDataObject.getSource() != null && !hpcDataObject.getCollection().isEmpty())
					putUploadFileJargon();
			} catch (DataNotFoundException dnfe) {
				message = "Input collection/dataset folder not found";
			} catch (OverwriteException oe) {
				message = "Input collection/dataset already at the target location";
			} catch (JargonException je) {
				message = "Can not process " + je.getMessage();
			}
		}

		if (message == null)
			message = "Collection " + hpcDataObject.getCollection() + " added to the archive";
		return message;
	}

	private boolean isDirectory(String filename) {
		if (Files.exists(Paths.get(filename)) && Files.isDirectory(Paths.get(filename)))
			return true;
		else
			return false;

	}

	private String validateAddMetadataToObject() {
		String message = null;
		String targetLocation = null;
		String hpcServerURL = configProperties.getProperty("hpc.server.url");
		String hpcCollection = configProperties.getProperty("hpc.collection.service");
		String hpcDataObjectService = configProperties.getProperty("hpc.dataobject.service");
		final String irodsZoneHome = configProperties.getProperty("irods.default.zoneHome");
		final String irodsUsername = configProperties.getProperty("irods.username");

		HpcCollectionDTO hpcCollectionRegistrationDTO = getCollectionRegistrationDTO();

		setCollectionName(hpcCollectionRegistrationDTO);

		targetLocation = getTargetLocation(irodsZoneHome, irodsUsername);

		// System.out.println("Collection name::"+
		// hpcDataObject.getCollection());
		try {
			if (hpcDataObject.getCollection() != null && !hpcDataObject.getCollection().isEmpty()) {
				putCollectionDataObjectHPCMetadata(targetLocation, hpcServerURL, hpcCollection,
						hpcCollectionRegistrationDTO);
			} else {
				putUploadFileJargon();
				putCollectionDataObjectHPCMetadata(targetLocation, hpcServerURL, hpcDataObjectService,
						hpcCollectionRegistrationDTO);
			}
		} catch (HttpStatusCodeException e) {
			System.out.println("message1::" + e.getMessage());
			message = getErrorMessage(e.getResponseBodyAsString());
			//
		} catch (RestClientException e) {
			e.printStackTrace();
			message = "Client error occured while adding collection :" + e.getMessage();
			// System.out.println("message2::"+message);
		} catch (Exception e) {
			e.printStackTrace();
			message = "Exception occured while adding metadata :" + e.getMessage();
			// System.out.println("message3::"+message);
		}
		return message;
	}

private void putCollectionDataObjectHPCMetadata(String targetLocation,
		String hpcServerURL, String hpcCollection,
		HpcCollectionDTO hpcCollectionRegistrationDTO) {
	String targetCollection;
	File localFile = null;
    String collection = hpcDataObject.getCollection();        
	String irodsUsername = configProperties.getProperty("irods.username");		
    String irodsZoneHome = configProperties.getProperty("irods.default.zoneHome");

	String parentCollection = getParentCollection();
	String collName = hpcDataObject.getCollection();
	if(parentCollection != null)
		collName = parentCollection + "/" + collName;

	if(hpcDataObject.getSource() != null)
		localFile=new File(hpcDataObject.getSource());
	
	if(localFile != null)
		targetCollection  = irodsZoneHome+"/"+irodsUsername+"/"+collName+"/"+localFile.getName();
    else
    	targetCollection = irodsZoneHome+"/"+irodsUsername+"/"+collName;

	RestTemplate restTemplate = new RestTemplate();
	HttpHeaders headers = new HttpHeaders();
	List <MediaType> mediaTypeList = new ArrayList<MediaType>();
	mediaTypeList.add(MediaType.APPLICATION_JSON);
	headers.setAccept(mediaTypeList);
	//headers.setContentType(MediaType.APPLICATION_JSON);
	HttpEntity<HpcCollectionDTO> entity = new HttpEntity<HpcCollectionDTO>(hpcCollectionRegistrationDTO, headers);
	System.out.println("Adding Metadata to .."+ hpcServerURL+"/"+hpcCollection+targetCollection);
    if(localFile != null && localFile.isFile())
    {
	 	ResponseEntity<HpcExceptionDTO> response = restTemplate.exchange(hpcServerURL+"/"+hpcCollection+targetCollection, HttpMethod.PUT,entity , HpcExceptionDTO.class);
    }
    else if(localFile != null)
    {
   	 List<String> files = new ArrayList<String>();
   	 getListofFiles (localFile, files);
   	 for(String filePath : files)
   	 {
   		 int startIndex = filePath.indexOf(hpcDataObject.getSource());
   		 
   		 String relativePath = filePath.substring(startIndex);
   		relativePath = relativePath.replace("\\", "/");
   		 String path = irodsZoneHome+"/"+irodsUsername+"/"+collName+relativePath;
 
   		ResponseEntity<HpcExceptionDTO> response = restTemplate.exchange(hpcServerURL+"/"+hpcCollection + path, HttpMethod.PUT,entity , HpcExceptionDTO.class);
   	 }
    }
    else
    	restTemplate.exchange(hpcServerURL+"/"+hpcCollection+targetCollection, HttpMethod.PUT,entity , HpcExceptionDTO.class);
}

	private HpcCollectionDTO getCollectionRegistrationDTO() {
		List<HpcMetadataEntry> listOfhpcCollection = getListOfAVUs();

		HpcCollectionDTO hpcCollectionRegistrationDTO = new HpcCollectionDTO();
		hpcCollectionRegistrationDTO.getMetadataEntries().addAll(listOfhpcCollection);
		return hpcCollectionRegistrationDTO;
	}

	private void setCollectionName(HpcCollectionDTO hpcCollectionRegistrationDTO) {
		String collectionType = getAttributeValueByName(configProperties.getProperty("hpc.collection.type"),
				hpcCollectionRegistrationDTO);
		if (collectionType != null && collectionType.equalsIgnoreCase("dataset"))
			hpcDataObject.setCollection(getAttributeValueByName(configProperties.getProperty("hpc.dataset.name"),
					hpcCollectionRegistrationDTO));
		else if (collectionType != null && collectionType.equalsIgnoreCase("project"))
			hpcDataObject.setCollection(getAttributeValueByName(configProperties.getProperty("hpc.project.name"),
					hpcCollectionRegistrationDTO));
		else
			hpcDataObject.setCollection("");
	}

	private String getAttributeValueByName(String collectionType,
			HpcCollectionDTO hpcCollectionRegistrationDTO) {
		for (HpcMetadataEntry hpcMetadataEntry : hpcCollectionRegistrationDTO.getMetadataEntries()) {
			if (collectionType.equalsIgnoreCase(hpcMetadataEntry.getAttribute()))
				return hpcMetadataEntry.getValue();
		}
		return null;
	}

	private String getTargetLocation(final String irodsZoneHome, final String irodsUsername) {
		String targetLocation = null;
		if (hpcDataObject.getCollection() != null) {
			String parentCollection = getParentCollection();
			String collName = hpcDataObject.getCollection();
			if (parentCollection != null)
				collName = parentCollection + "/" + collName;
			if (hpcDataObject.getSource() != null) {
				File localFile = new File(hpcDataObject.getSource());
				if (localFile.isFile())
					targetLocation = irodsZoneHome + "/" + irodsUsername + "/" + collName + "/" + localFile.getName();
				else
					targetLocation = irodsZoneHome + "/" + irodsUsername + "/" + collName;
			} else
				targetLocation = irodsZoneHome + "/" + irodsUsername + "/" + collName;
		} else {
			targetLocation = irodsZoneHome + "/" + irodsUsername;
		}
		return targetLocation;
	}

	private List<HpcMetadataEntry> getListOfAVUs() {
		JSONObject jsonObject = readMetadataJsonFromFile();
		JSONArray jsonArray = (JSONArray) jsonObject.get("metadataEntries");

		List<HpcMetadataEntry> listOfhpcCollection = new ArrayList<HpcMetadataEntry>();
		AvuData avuData = null;
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject attrObj = (JSONObject) jsonArray.get(i);
			// System.out.println(attrObj.get("attribute"));
			HpcMetadataEntry hpcMetadataEntry = new HpcMetadataEntry();
			hpcMetadataEntry.setAttribute((String) attrObj.get("attribute"));
			hpcMetadataEntry.setValue((String) attrObj.get("value"));
			hpcMetadataEntry.setUnit((String) attrObj.get("unit"));
			listOfhpcCollection.add(hpcMetadataEntry);
		}
		return listOfhpcCollection;
	}

	private String getParentCollection() {
		JSONObject jsonObject = readMetadataJsonFromFile();
		JSONArray jsonArray = (JSONArray) jsonObject.get("metadataEntries");

		List<HpcMetadataEntry> listOfhpcCollection = new ArrayList<HpcMetadataEntry>();
		AvuData avuData = null;
		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject attrObj = (JSONObject) jsonArray.get(i);
			// System.out.println(attrObj.get("attribute"));
			String attr = (String) attrObj.get("attribute");
			if (!attr.equalsIgnoreCase("Parent Collection"))
				continue;
			else
				return (String) attrObj.get("value");
		}
		return null;
	}

	private String getErrorMessage(String errorpayload) {
		JSONParser parser = new JSONParser();
		try {
			System.out.println(errorpayload);
			JSONObject jsonObject = (JSONObject) parser.parse(errorpayload);
			JSONObject exceptioDTO = (JSONObject) jsonObject.get("gov.nih.nci.hpc.dto.error.HpcExceptionDTO");

			return (String) exceptioDTO.get("message");
		} catch (Exception ex) {
			return "Cannot parse the error message";
		}
	}

	public JSONObject readMetadataJsonFromFile() {

		JSONParser parser = new JSONParser();
		try {
			JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(hpcDataObject.getMetadata()));
			// JSONArray jsonObject = (JSONArray) obj;
			// System.out.println(jsonObject.toJSONString());
			// return (JSONObject) jsonObject.get("metadataEntries");
			return jsonObject;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void putUploadFileJargon() throws DataNotFoundException, JargonException, OverwriteException {
		if (hpcDataObject.getSource() == null)
			return;

		IRODSFile destFile = null;
		String targetCollection = null;

		String irodsZoneHome = configProperties.getProperty("irods.default.zoneHome");
		String irodsUsername = configProperties.getProperty("irods.username");
		File localFile = new File(hpcDataObject.getSource());
		String collection = hpcDataObject.getCollection();

		String parentCollection = getParentCollection();
		String collName = hpcDataObject.getCollection();
		if (parentCollection != null)
			collName = parentCollection + "/" + collName;

		if (localFile != null && localFile.isFile())
			targetCollection = irodsZoneHome + "/" + irodsUsername + "/" + collName + "/" + localFile.getName();
		else
			targetCollection = irodsZoneHome + "/" + irodsUsername + "/" + collName;

		destFile = irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFile(targetCollection);

		DataTransferOperations dto = irodsFileSystem.getIRODSAccessObjectFactory().getDataTransferOperations(account);
		
        
		TransferOptions transferOptions = new TransferOptions();
		transferOptions.setComputeAndVerifyChecksumAfterTransfer(true);
		transferOptions.setMaxThreads(0);
		transferOptions.setUseParallelTransfer(false);        

		TransferControlBlock tcb = irodsFileSystem.getIrodsSession()
				.buildDefaultTransferControlBlockBasedOnJargonProperties();
		tcb.setTransferOptions(transferOptions);
		
		dto.putOperation(localFile, destFile, null, tcb);
		
	}

	public void setHPCDataObject(HPCDataObject hpcDataObject) {
		this.hpcDataObject = hpcDataObject;
	}

	public void setHPCAccount() throws NumberFormatException, JargonException {
		String irodsHostName = configProperties.getProperty("irods.default.host");
		String irodsPort = configProperties.getProperty("irods.default.port");
		String irodsZone = configProperties.getProperty("irods.default.zone");
		String irodsZoneHome = configProperties.getProperty("irods.default.zoneHome");
		String irodsUsername = configProperties.getProperty("irods.username");
		String irodsPassword = new String(
				DatatypeConverter.parseBase64Binary(configProperties.getProperty("irods.password")));
		String irodsResource = configProperties.getProperty("irods.resource");
		account = IRODSAccount.instance(irodsHostName, Integer.parseInt(irodsPort), irodsUsername, irodsPassword,
				irodsZoneHome + "/" + irodsUsername, irodsZone, irodsResource);
	}

	private List<String> getListofFiles(File dir, List<String> fileList) {
		try {
			File[] files = dir.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					getListofFiles(file, fileList);
				} else {
					fileList.add(file.getCanonicalPath());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileList;
	}
}
