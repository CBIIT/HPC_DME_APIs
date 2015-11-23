package gov.nih.nci.hpc.cli;

import gov.nih.nci.hpc.cli.domain.HPCBatchCollection;
import gov.nih.nci.hpc.cli.util.HpcConfigProperties;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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
public class HPCCSVFile {
	@Autowired
	private HpcConfigProperties configProperties;
	
	public HPCCSVFile() {
		super();
	}

	
	public String parseBatchFile(String fileName) {
		readCsvFile(fileName);
		return "Batch Upload Successful";
	}


	
	private void readCsvFile(String fileName) {
		String hpcServerURL = configProperties.getProperty("hpc.server.url");
		String irodsZoneHome = configProperties.getProperty("irods.default.zoneHome");//rods
		String hpcDestinationEndpoint = configProperties.getProperty("hpc.destination.endpoint");
		String hpcDestinationPath = configProperties.getProperty("hpc.destination.path");
		String irodsUsername = configProperties.getProperty("irods.username");
		
		FileReader fileReader = null;
		
		CSVParser csvFileParser = null;
		String hpcDataService = null;
		
		//Create the CSVFormat object with the header mapping
        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withHeader();
     
        try {
        	            
            //initialize FileReader object
            fileReader = new FileReader(fileName);
           
            //initialize CSVParser object
            csvFileParser = new CSVParser(fileReader, csvFileFormat);
            
            Map<String, Integer> headersMap = csvFileParser.getHeaderMap();
    
            //Get a list of CSV file records
            List<CSVRecord> csvRecords = csvFileParser.getRecords(); 
            
            //Read the CSV file records starting from the second record to skip the header
            for (int i = 0; i < csvRecords.size(); i++) {
            	CSVRecord record = csvRecords.get(i);
            	String[] fields=new String[record.size()];
            	String collName = null;

            	List<HpcMetadataEntry> listOfhpcCollection = new ArrayList<HpcMetadataEntry>();
              	for (Entry<String, Integer> entry : headersMap.entrySet()) { 
              			String cellVal = record.get( entry.getKey());
            			HpcMetadataEntry hpcMetadataEntry = new HpcMetadataEntry();
            			hpcMetadataEntry.setAttribute(entry.getKey());
            			hpcMetadataEntry.setValue(cellVal);
            			if (StringUtils.isNotBlank(cellVal))
            				listOfhpcCollection.add(hpcMetadataEntry);
            		}            		              	  
            	  	
              	
				HpcDataObjectRegistrationDTO hpcDataObjectRegistrationDTO = new HpcDataObjectRegistrationDTO();
				hpcDataObjectRegistrationDTO.getMetadataEntries().addAll(listOfhpcCollection);

				String collectionName = getAttributeValueByName("Collection name",hpcDataObjectRegistrationDTO); 

				if(collectionName != null)
				{
					System.out.println("Registering Collection " + collectionName);
					collName = collectionName;
					hpcDataService = configProperties.getProperty("hpc.collection.service");
				}
				else
				{
	    			HpcDataTransferLocations locations = new HpcDataTransferLocations();
	    			HpcFileLocation source = new HpcFileLocation();
	    			source.setEndpoint(getAttributeValueByName("File Source (Globus Origin endpoint)",hpcDataObjectRegistrationDTO));
	    			String filePath = getAttributeValueByName("Source File Path",hpcDataObjectRegistrationDTO) + "/"+getAttributeValueByName("File name",hpcDataObjectRegistrationDTO);
	        		System.out.println("Adding file from " + filePath);
	    			source.setPath(filePath);
	    			HpcFileLocation destination = new HpcFileLocation();
	    			destination.setEndpoint(hpcDestinationEndpoint);
	    			destination.setPath(hpcDestinationPath);
	    			locations.setDestination(destination);
	    			locations.setSource(source);
	    			hpcDataObjectRegistrationDTO.setLocations(locations);
	    		    collName = getAttributeValueByName("File name",hpcDataObjectRegistrationDTO);	    		    
	    		    hpcDataService = configProperties.getProperty("hpc.dataobject.service");
				}             	
              	

				
				
				String parentCollection = getAttributeValueByName("Parent Collection Path (Logical Path)",hpcDataObjectRegistrationDTO);
				
				
				if(parentCollection != null)
					collName = parentCollection + "/" + collName;


				RestTemplate restTemplate = new RestTemplate();
				HttpHeaders headers = new HttpHeaders();
				List <MediaType> mediaTypeList = new ArrayList<MediaType>();
				mediaTypeList.add(MediaType.APPLICATION_JSON);
				headers.setAccept(mediaTypeList);
				//headers.setContentType(MediaType.APPLICATION_JSON);
				HttpEntity<HpcDataObjectRegistrationDTO> entity = new HttpEntity<HpcDataObjectRegistrationDTO>(hpcDataObjectRegistrationDTO, headers);
				//System.out.println("Adding Metadata to .."+ hpcServerURL+"/"+hpcCollection+targetCollection);
				try
				{
					ResponseEntity<HpcExceptionDTO> response = restTemplate.exchange(hpcServerURL + "/"+ hpcDataService +"/" +irodsZoneHome+"/"+irodsUsername+"/"+collName, HttpMethod.PUT,entity , HpcExceptionDTO.class);
				}
				catch (HttpStatusCodeException e) {
					System.out.println("Cannot add Dataobject/Collection " + collName + "Due to " +  e.getMessage());
					//System.out.println("Adding to error log ");
					//message = getErrorMessage(e.getResponseBodyAsString());
					addToErrorCollection(e.getResponseBodyAsString(),record, headersMap);
					continue;
					
				} catch (RestClientException e) {
					e.printStackTrace();
					//message = "Client error occured while adding collection :" + e.getMessage();
					// System.out.println("message2::"+message);
				} catch (Exception e) {
					e.printStackTrace();
					//message = "Exception occured while adding metadata :" + e.getMessage();
					// System.out.println("message3::"+message);
				}
              	
			}
            
        } 
        catch (Exception e) {
        	System.out.println("Cannot read the input file");
            e.printStackTrace();
        } finally {
            try {
                fileReader.close();
                csvFileParser.close();
            } catch (IOException e) {
            	System.out.println("Error while closing fileReader/csvFileParser !!!");
                e.printStackTrace();
            }
        }

	}
	
	private void addToErrorCollection(String responseBodyAsString,CSVRecord record,Map<String, Integer> headers) {
		String logDir = configProperties.getProperty("hpc.error-log.dir");
		
		JSONParser parser = new JSONParser();
		JSONObject hpcException = null;
	    FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");		

			 try 
			 {
				String logFile = logDir +"/"+"errorLog"+  new SimpleDateFormat("yyyyMMddhhmm'.csv'").format(new Date());
	    		File file =new File(logFile);
	    		if(!file.exists()){
	    			file.createNewFile();
	    		}				 
				 fileWriter = new FileWriter(file,true);
			 	 csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			 	 Object [] headerArray =  new ArrayList<Object>(headers.keySet()).toArray();
			 	 if(!checkIfHeaderExists(logFile))
			 		 csvFilePrinter.printRecord(headerArray);
			 	 else
			 		 csvFilePrinter.println();
			 	 for (Entry<String, Integer> entry : headers.entrySet()) { 			 		
			 		csvFilePrinter.print(record.get(entry.getKey()));		                 							 		
			 	 } 
				 System.out.println(responseBodyAsString);
				 JSONObject jsonObject = (JSONObject) parser.parse(responseBodyAsString);
				 hpcException = (JSONObject) jsonObject.get("gov.nih.nci.hpc.dto.error.HpcExceptionDTO");
				 if(hpcException == null)
					csvFilePrinter.print(jsonObject.get("message"));
				 else
					csvFilePrinter.print(hpcException.get("message"));			 	
			 	System.out.println("Log entry successfull");
			  } 
			 catch (Exception e) 
			 {
			      System.out.println("Error in Writing the error log !!!");
			      e.printStackTrace();
			 }
			 finally 
			 {			     
                  try 
                  {
                      fileWriter.flush();
                      fileWriter.close();			   
                      csvFilePrinter.close();			   
                  }
                  catch (IOException e) {		      
                      System.out.println("Error while flushing/closing fileWriter/csvPrinter !!!");		     
                      e.printStackTrace();		      
                  }
			 }
			//return (String) exceptioDTO.get("message");
	}


	private boolean checkIfHeaderExists(String logFile) throws IOException {
        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withHeader();
        FileReader fileReader = new FileReader(logFile);
        CSVParser csvFileParser = new CSVParser(fileReader, csvFileFormat);
        Map<String, Integer> headersMap = csvFileParser.getHeaderMap();
        //System.out.println("HEADER MAP::" + headersMap);
        if(headersMap != null &&  !headersMap.isEmpty())
        	return true;
        else
        	return false;
	}


	private List<HpcMetadataEntry> getListOfMetadataElements(HPCBatchCollection batchCollection) throws Exception {
		List<HpcMetadataEntry> listOfhpcCollection = new ArrayList<HpcMetadataEntry>();
		for(PropertyDescriptor propertyDescriptor : 
		    Introspector.getBeanInfo(batchCollection.getClass()).getPropertyDescriptors()){

		    System.out.println(propertyDescriptor.getReadMethod().invoke(batchCollection, null));
		}
		
		return listOfhpcCollection;
	}
	
	
	private String getAttributeValueByName(String collectionType,
			HpcDataObjectRegistrationDTO hpcDataObjectRegistrationDTO) {
		for (HpcMetadataEntry hpcMetadataEntry : hpcDataObjectRegistrationDTO.getMetadataEntries()) {
			if (collectionType.equalsIgnoreCase(hpcMetadataEntry.getAttribute()))
				return hpcMetadataEntry.getValue();
		}
		return null;
	}
}
