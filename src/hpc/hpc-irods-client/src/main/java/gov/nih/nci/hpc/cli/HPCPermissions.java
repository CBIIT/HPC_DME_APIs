package gov.nih.nci.hpc.cli;

import gov.nih.nci.hpc.cli.domain.HPCBatchCollection;
import gov.nih.nci.hpc.cli.util.HpcConfigProperties;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionResponseListDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.cli.util.BasicAuthRestTemplate;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.commons.codec.binary.Base64;
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
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Component
public class HPCPermissions {
	@Autowired
	private HpcConfigProperties configProperties;
	
	public HPCPermissions() {
		super();
	}

	
	public String parseBatchFile(String fileName) {
		try
		{
			jline.console.ConsoleReader reader = new jline.console.ConsoleReader();
			reader.setExpandEvents(false);
			System.out.println("Enter NCI Login UserId:");
			String userId = reader.readLine();
			
			System.out.println("Enter NCI Login password:");
			String password = reader.readLine(new Character('*'));
			System.out.println("Initiating batch process as NCI Login UserId:" +userId);

			boolean success = readCsvFile(fileName, userId, password);
			if(success)
				return "Batch permission proessing is successful";
			else
				return "Batch permission processing is not Successful. Please error log for the records not processed.";
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return "Failed to run batch registration";
		}
	}

	private boolean readCsvFile(String fileName, String userId, String password) {
		boolean success = true;
		String hpcServerURL = configProperties.getProperty("hpc.server.url");
		String irodsZoneHome = configProperties.getProperty("irods.default.zoneHome");//rods
		String hpcDestinationEndpoint = configProperties.getProperty("hpc.destination.endpoint");
		String hpcDestinationPath = configProperties.getProperty("hpc.destination.path");
		String irodsUsername = configProperties.getProperty("irods.username");
		String hpcDataService = configProperties.getProperty("hpc.dataobject.service");
		FileReader fileReader = null;
		CSVParser csvFileParser = null;
		
		//Create the CSVFormat object with the header mapping
        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withHeader();
        try {
			 List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
			 messageConverters.add(new FormHttpMessageConverter());
			 messageConverters.add(new StringHttpMessageConverter());
			 messageConverters.add(new MappingJackson2HttpMessageConverter());
			 RestTemplate restTemplate = new RestTemplate();
			 restTemplate.setMessageConverters(messageConverters);
            //initialize FileReader object
            fileReader = new FileReader(fileName);
            //initialize CSVParser object
            csvFileParser = new CSVParser(fileReader, csvFileFormat);
            Map<String, Integer> headersMap = csvFileParser.getHeaderMap();
            //Get a list of CSV file records
            List<CSVRecord> csvRecords = csvFileParser.getRecords(); 
            //Read the CSV file records starting from the second record to skip the header
        	Map<String, List<HpcUserPermission>> permissions = new HashMap<String, List<HpcUserPermission>>();
            for (int i = 0; i < csvRecords.size(); i++) {
            	CSVRecord record = csvRecords.get(i);
            	String path = record.get("Path");
            	String ruserId = record.get("UserId");
            	String permission = record.get("Permission");
            	List<HpcUserPermission> pathPermission = permissions.get(path);
            	if(pathPermission == null)
            		pathPermission = new ArrayList<HpcUserPermission>();
            	HpcUserPermission userPermission = new HpcUserPermission();
            	userPermission.setUserId(ruserId);
            	userPermission.setPermission(permission);
            	pathPermission.add(userPermission);
            	permissions.put(path, pathPermission);
            }
            List<HpcEntityPermissionRequestDTO> dtos = new ArrayList<HpcEntityPermissionRequestDTO>();
            
            for (Entry<String, List<HpcUserPermission>> entry : permissions.entrySet()) {
            {
            	String pathStr = entry.getKey();
            	HpcEntityPermissionRequestDTO hpcPermissionDTO = new HpcEntityPermissionRequestDTO();
            	hpcPermissionDTO.setPath(entry.getKey());
            	hpcPermissionDTO.getUserPermissions().addAll(permissions.get(pathStr));
            	dtos.add(hpcPermissionDTO);
            }
			HttpHeaders headers = new HttpHeaders();
			String token =DatatypeConverter.printBase64Binary((userId + ":" + password).getBytes());
			headers.add("Authorization", "Basic " + token);				
				List <MediaType> mediaTypeList = new ArrayList<MediaType>();
				mediaTypeList.add(MediaType.APPLICATION_JSON);
				headers.setAccept(mediaTypeList);
				HttpEntity<?> entity = new HttpEntity<Object>(dtos, headers);
				try
				{
					restTemplate.postForEntity(hpcServerURL +"/acl",  entity , null);
				}
				catch (HttpStatusCodeException e) {
					System.out.println("Cannot set permission Due to " +  e.getMessage());
					//System.out.println("Adding to error log ");
					//message = getErrorMessage(e.getResponseBodyAsString());
					//addToErrorCollection(e.getResponseBodyAsString(),record, headersMap);
					success = false;
					continue;
					
				} catch (RestClientException e) {
					e.printStackTrace();
					success = false;
					//message = "Client error occured while adding collection :" + e.getMessage();
					// System.out.println("message2::"+message);
				} catch (Exception e) {
					e.printStackTrace();
					success = false;
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
        return success;

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

}
