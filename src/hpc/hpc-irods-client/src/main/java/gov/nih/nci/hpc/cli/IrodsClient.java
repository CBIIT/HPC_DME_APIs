package gov.nih.nci.hpc.cli;

import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.util.HpcConfigProperties;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.collection.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.DatatypeConverter;










//import org.codehaus.jackson.map.ObjectMapper;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.OverwriteException;
import org.irods.jargon.core.pub.BulkAVUOperationResponse;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.domain.AvuData;
import org.irods.jargon.core.pub.io.IRODSFile;
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
import org.springframework.validation.ObjectError;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
@Component
public class IrodsClient implements HPCClient{
	@Autowired
	private HpcConfigProperties configProperties;
	
	private IRODSFileSystem irodsFileSystem = null;
	private IRODSAccount account = null;
	private HPCDataObject hpcDataObject;

	
	public IrodsClient() throws JargonException
	{
		//initialize
		irodsFileSystem = IRODSFileSystem.instance();
		
	}


	
	@Override
	public String processDataObject() {
		String message = null;
			try{
			putUploadFileJargon();
			}catch(DataNotFoundException dnfe){
				message = "Input collection/dataset folder not found";
			}catch(OverwriteException oe){
				message = "Input collection/dataset already at the target location";
			}catch(JargonException je){
				message = "Can not process " + je.getMessage();
			}
			
			//addMetadataToObject();
			if (isDirectory(hpcDataObject.getFilename()))
				validateAddMetadataToObject();
			System.out.println("message:outside::" + message);
			if (message == null)
				message = "Collection "+hpcDataObject.getFilename()+" added to archive";
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
		final String irodsZoneHome = configProperties.getProperty("irods.default.zoneHome");
		final String irodsUsername = configProperties.getProperty("irods.username");	 
		
         if(hpcDataObject.getLocation() != null)
        {
        	targetLocation=irodsZoneHome+"/"+irodsUsername+"/"+hpcDataObject.getLocation();	
        }else
        {
        	targetLocation=irodsZoneHome+"/"+irodsUsername;
        }		
		
	 	List<HpcMetadataEntry> listOfhpcCollection = getListOfAVUs();
		RestTemplate restTemplate = new RestTemplate();
		  HashMap<String, String > urlMap = new HashMap<String, String>(){{
		        put("path",irodsZoneHome+"/"+irodsUsername);
		    }};
		HpcCollectionRegistrationDTO hpcCollectionRegistrationDTO = new HpcCollectionRegistrationDTO();
		hpcCollectionRegistrationDTO.getMetadataEntries().addAll(listOfhpcCollection);
		
	try{
		//restTemplate.put(
			//	hpcServerURL+"/"+hpcCollection+irodsZoneHome+"/"+irodsUsername+"/"+hpcDataObject.getFilename(),hpcCollectionRegistrationDTO,urlMap);

        HttpHeaders headers = new HttpHeaders();
		List <MediaType> mediaTypeList = new ArrayList<MediaType>();
		mediaTypeList.add(MediaType.APPLICATION_JSON);
		headers.setAccept(mediaTypeList);
        //headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<HpcCollectionRegistrationDTO> entity = new HttpEntity<HpcCollectionRegistrationDTO>(hpcCollectionRegistrationDTO, headers);
        
		ResponseEntity<HpcExceptionDTO> response = restTemplate.exchange(hpcServerURL+"/"+hpcCollection+targetLocation+"/"+hpcDataObject.getFilename(), HttpMethod.PUT,entity , HpcExceptionDTO.class);

		
		}catch (HttpStatusCodeException e) {
			message = getErrorMessage(e.getResponseBodyAsString());	
		} catch (RestClientException e) {
			e.printStackTrace();
			message = "Client error occured while adding collection :" + e.getMessage();
		} catch (Exception e) {
			e.printStackTrace();
			message = "Exception occured while adding metadata :" + e.getMessage();
		}
	return message;		
	}



private List<HpcMetadataEntry> getListOfAVUs() {
 	JSONObject jsonObject = readMetadataJsonFromFile();
	JSONArray jsonArray = (JSONArray) jsonObject.get("metadataEntries");
	
	List<HpcMetadataEntry> listOfhpcCollection = new ArrayList<HpcMetadataEntry>();
	AvuData avuData = null;
	for (int i = 0; i < jsonArray.size(); i++) {
		JSONObject attrObj=(JSONObject) jsonArray.get(i);
	    //System.out.println(attrObj.get("attribute"));
	    HpcMetadataEntry hpcMetadataEntry = new HpcMetadataEntry();
	    hpcMetadataEntry.setAttribute((String) attrObj.get("attribute"));
	    hpcMetadataEntry.setValue((String) attrObj.get("value"));
	    hpcMetadataEntry.setUnit((String) attrObj.get("unit"));
	    listOfhpcCollection.add(hpcMetadataEntry);
	}
	return listOfhpcCollection;
}



private String getErrorMessage(String errorpayload) {
	JSONParser parser = new JSONParser();
	try 
	{
		JSONObject jsonObject = (JSONObject) parser.parse(errorpayload);
		JSONObject exceptioDTO = (JSONObject) jsonObject.get("gov.nih.nci.hpc.dto.error.HpcExceptionDTO");
		return (String) exceptioDTO.get("message");	
	} catch (Exception ex) 
	{			
		return "Cannot parse the error message";
	}
}



public JSONObject readMetadataJsonFromFile() {

	JSONParser parser = new JSONParser();
	try {
			JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(hpcDataObject.getMetadataFile()));	
			//JSONArray jsonObject = (JSONArray) obj;
			System.out.println(jsonObject.toJSONString());
			//return (JSONObject) jsonObject.get("metadataEntries");
			return jsonObject;
	
	} catch (FileNotFoundException e) 
	{
			e.printStackTrace();
	} catch (IOException e) 
	{
			e.printStackTrace();
	} catch (ParseException e) 
	{
			e.printStackTrace();
	}
	return null;
}
	
	 public void addMetadataToObject() throws Exception
	 {
		 	JSONObject jsonObject = readMetadataJsonFromFile();
		 	/*
			if("COLLECTION".equalsIgnoreCase(hpcDataObject.getObjectType()))
			{
				CollectionAO collectionAO = irodsFileSystem.getIRODSAccessObjectFactory()
						.getCollectionAO(account);
				List<AvuData> listOfAvuData = new ArrayList<AvuData>();
				AvuData avuData = null;
				for (int i = 0; i < jsonObject.size(); i++) {
					JSONObject attrObj=(JSONObject) jsonObject.get(i);
				    System.out.println(attrObj.get("attribute"));
				    avuData = new AvuData((String) attrObj.get("attribute"),(String) attrObj.get("value"),(String) attrObj.get("unit"));
				    listOfAvuData.add(avuData);
				}
			
				collectionAO.addBulkAVUMetadataToCollection(configProperties.getProperty("irods.default.zoneHome")+"/"+configProperties.getProperty("irods.username")+"/"+hpcDataObject.getFilename(),
						listOfAvuData);
			}

			if("DATAOBJECT".equalsIgnoreCase(hpcDataObject.getObjectType()))
			{
*/
				DataObjectAO dataObjectAO = irodsFileSystem
						.getIRODSAccessObjectFactory().getDataObjectAO(account);
				JSONArray jsonArray = (JSONArray) jsonObject.get("metadataEntries");

				List<AvuData> listOfAvuData = new ArrayList<AvuData>();
				AvuData avuData = null;
				for (int i = 0; i < jsonArray.size(); i++) {
					JSONObject attrObj=(JSONObject) jsonArray.get(i);
				    avuData = new AvuData((String) attrObj.get("attribute"),(String) attrObj.get("value"),(String) attrObj.get("unit"));
				    listOfAvuData.add(avuData);
				}

				List<BulkAVUOperationResponse> response = dataObjectAO.addBulkAVUMetadataToDataObject(configProperties.getProperty("irods.default.zoneHome")+"/"+configProperties.getProperty("irods.username")+"/"+hpcDataObject.getFilename(), listOfAvuData);
				
//			}			

	 }
 

   public void putUploadFileJargon() throws DataNotFoundException,JargonException,OverwriteException
   {
	    IRODSFile destFile = null;
	    String irodsZoneHome = configProperties.getProperty("irods.default.zoneHome");
		String irodsUsername = configProperties.getProperty("irods.username");		
        File localFile=new File(hpcDataObject.getFilename());
        String targetLocation = hpcDataObject.getLocation();
        if(targetLocation != null)
        {
        	destFile=irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFile(irodsZoneHome+"/"+irodsUsername+"/"+hpcDataObject.getLocation());	
        }else
        {
        	destFile=irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFileUserHomeDir(irodsUsername);
        }
        DataTransferOperations dto=irodsFileSystem.getIRODSAccessObjectFactory().getDataTransferOperations(account);        
        dto.putOperation(localFile,destFile,null,null);        
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
		String irodsPassword = new String(DatatypeConverter.parseBase64Binary(configProperties.getProperty("irods.password")));
		String irodsResource = configProperties.getProperty("irods.resource");		
        account = IRODSAccount.instance(irodsHostName, Integer.parseInt(irodsPort),irodsUsername , irodsPassword,
        		irodsZoneHome+"/"+irodsUsername, irodsZone, irodsResource);
	}	
}

