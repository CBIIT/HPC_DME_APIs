package gov.nih.nci.hpc.cli;

import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.util.HpcConfigProperties;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.collection.HpcCollectionRegistrationDTO;

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
import org.springframework.stereotype.Component;
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

			try{
			putUploadFileJargon();
			}catch(DataNotFoundException dnfe){
				return "Input collection/dataset folder not found";
			}catch(OverwriteException oe){
				return "Input collection/dataset already at the target location";
			}catch(JargonException je){
				return "Can not process " + je.getMessage();
			}
			
			//addMetadataToObject();
			if (isDirectory(hpcDataObject.getFilename()))
				validateAddMetadataToObject();
			
		
		return "Collection "+hpcDataObject.getFilename()+" added to archive";
	}
	

 private boolean isDirectory(String filename) {
	 	if (Files.exists(Paths.get(filename)) && Files.isDirectory(Paths.get(filename)))
	 		return true;
	 	else
	 		return false;
		
	}



private void validateAddMetadataToObject() {
		String hpcServerURL = configProperties.getProperty("hpc.server.url");
		String hpcCollection = configProperties.getProperty("hpc.collection.service");
		final String irodsZoneHome = configProperties.getProperty("irods.default.zoneHome");
		final String irodsUsername = configProperties.getProperty("irods.username");	 
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
		RestTemplate restTemplate = new RestTemplate();
		  HashMap<String, String > urlMap = new HashMap<String, String>(){{
		        put("path",irodsZoneHome+"/"+irodsUsername);
		    }};
		HpcCollectionRegistrationDTO hpcCollectionRegistrationDTO = new HpcCollectionRegistrationDTO();
		hpcCollectionRegistrationDTO.getMetadataEntries().addAll(listOfhpcCollection);

		
		restTemplate.put(
				hpcServerURL+"/"+hpcCollection+irodsZoneHome+"/"+irodsUsername+"/"+hpcDataObject.getFilename(),hpcCollectionRegistrationDTO,urlMap);
	
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
		String irodsUsername = configProperties.getProperty("irods.username");		
        File localFile=new File(hpcDataObject.getFilename());
        DataTransferOperations dto=irodsFileSystem.getIRODSAccessObjectFactory().getDataTransferOperations(account);
        IRODSFile destFile=irodsFileSystem.getIRODSFileFactory(account).instanceIRODSFileUserHomeDir(irodsUsername);
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

