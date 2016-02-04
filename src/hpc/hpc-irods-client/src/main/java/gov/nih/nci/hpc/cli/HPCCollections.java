package gov.nih.nci.hpc.cli;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

@Component
public class HPCCollections extends HPCBatchClient {
	
	public HPCCollections() {
		super();
	}

	
	protected boolean processFile(String fileName, String userId, String password) {
		boolean success = true;
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
            String collectionPath = null;
            ResponseEntity<HpcExceptionDTO> response = null;
            //Read the CSV file records starting from the second record to skip the header
            for (int i = 0; i < csvRecords.size(); i++) {
            	boolean processedRecordFlag = true;
            	CSVRecord record = csvRecords.get(i);
            	List<HpcMetadataEntry> listOfhpcCollection = new ArrayList<HpcMetadataEntry>();
              	for (Entry<String, Integer> entry : headersMap.entrySet()) { 
              			String cellVal = record.get( entry.getKey());
              			if(entry.getKey().equals(Constants.COLLECTION_PATH))
              				collectionPath = cellVal;
            			HpcMetadataEntry hpcMetadataEntry = new HpcMetadataEntry();
            			hpcMetadataEntry.setAttribute(entry.getKey());
            			hpcMetadataEntry.setValue(cellVal);
            			if (StringUtils.isNotBlank(cellVal))
            				listOfhpcCollection.add(hpcMetadataEntry);
            		}            		              	  
            	  	
				System.out.println((i+1) +": Registering Collection " + collectionPath);
				
				 RestTemplate restTemplate = HpcClientUtil.getRestTemplate(userId, password, hpcCertPath, hpcCertPassword);

				HttpHeaders headers = new HttpHeaders();
				String token =DatatypeConverter.printBase64Binary((userId + ":" + password).getBytes());
				headers.add("Authorization", "Basic " + token);				
				List <MediaType> mediaTypeList = new ArrayList<MediaType>();
				mediaTypeList.add(MediaType.APPLICATION_JSON);
				headers.setAccept(mediaTypeList);
				HttpEntity<List<HpcMetadataEntry>> entity = new HttpEntity<List<HpcMetadataEntry>>(listOfhpcCollection, headers);
				try
				{
					if (!collectionPath.startsWith("/"))
						collectionPath = "/" + collectionPath;

					System.out.println(hpcServerURL + "/"+ hpcCollectionService + collectionPath);
					response = restTemplate.exchange(hpcServerURL + "/"+ hpcCollectionService + collectionPath, HttpMethod.PUT,entity , HpcExceptionDTO.class);
					if(response != null)
					{
						HpcExceptionDTO exception = response.getBody();
						if(exception != null)
						{
							String message = "Failed to process record due to: "+exception.getMessage() + ": Error Type:"+exception.getErrorType().value() + ": Request reject reason: "+exception.getRequestRejectReason().value();
							addErrorToLog(message, i+1);
							success = false;
							processedRecordFlag = false;
							addRecordToLog(record, headersMap);
						}else if(!(response.getStatusCode().equals(HttpStatus.CREATED) || response.getStatusCode().equals(HttpStatus.OK)))
						{
							addErrorToLog("Failed to process record due to unknown error. Return code: " + response.getStatusCode(), i+1);
							success = false;
							processedRecordFlag = false;
							addRecordToLog(record, headersMap);
						}
					}
				} catch (HpcBatchException e) {
					success = false;
					processedRecordFlag = false;
					String message = "Failed to process record due to: "+e.getMessage();
					System.out.println(message);
					addErrorToLog(message, i+1);
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					String exceptionAsString = sw.toString();
					addErrorToLog(exceptionAsString, i+1);
					addRecordToLog(record, headersMap);
				} catch (RestClientException e) {
					//e.printStackTrace();
					success = false;
					processedRecordFlag = false;
					String message = "Failed to process record due to: "+e.getMessage();
					System.out.println(message);
					addErrorToLog(message, i+1);	
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					String exceptionAsString = sw.toString();
					addErrorToLog(exceptionAsString, i+1);
					addRecordToLog(record, headersMap);
				} catch (Exception e) {
					//e.printStackTrace();
					success = false;
					processedRecordFlag = false;
					String message = "Failed to process record due to: "+e.getMessage();
					System.out.println(message);
					addErrorToLog(message, i+1);	
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					String exceptionAsString = sw.toString();
					addErrorToLog(exceptionAsString, i+1);
					addRecordToLog(record, headersMap);
				}
              	if(processedRecordFlag)
              		System.out.println("Success!");
              	else
              		System.out.println("Failure!");
          		System.out.println("---------------------------------");
          		
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
        return success;

	}
}
