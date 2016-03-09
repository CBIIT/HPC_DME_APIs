package gov.nih.nci.hpc.cli;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.DatatypeConverter;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcConfigProperties;
import gov.nih.nci.hpc.cli.util.HpcResponseErrorHandler;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

@Component
public class HPCDatafiles extends HPCBatchClient {
	@Autowired
	private HpcConfigProperties configProperties;

	public HPCDatafiles() {
		super();
	}

	protected void initializeLog()
	{
		logFile = logDir + File.separator + "putDatafiles_errorLog" + new SimpleDateFormat("yyyyMMddhhmm'.txt'").format(new Date());
		logRecordsFile = logDir + File.separator + "putDatafiles_errorRecords"
				+ new SimpleDateFormat("yyyyMMddhhmm'.csv'").format(new Date());
		File file1 = new File(logFile);
		File file2 = new File(logRecordsFile);
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
		try {
			if (!file1.exists()) {
				file1.createNewFile();
			}
			fileLogWriter = new FileWriter(file1, true);

			if (!file2.exists()) {
				file2.createNewFile();
			}
			fileRecordWriter = new FileWriter(file2, true);
			csvFilePrinter = new CSVPrinter(fileRecordWriter, csvFileFormat);
		} catch (IOException e) {
			System.out.println("Failed to initialize Batch process: " + e.getMessage());
			e.printStackTrace();
		}
		
	}	
	
	protected boolean processFile(String fileName, String userId, String password) {
		boolean success = true;
		FileReader fileReader = null;

		CSVParser csvFileParser = null;
		// Create the CSVFormat object with the header mapping
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withHeader();
		ResponseEntity<HpcExceptionDTO> response = null;
		try {

			// initialize FileReader object
			fileReader = new FileReader(fileName);

			// initialize CSVParser object
			csvFileParser = new CSVParser(fileReader, csvFileFormat);

			Map<String, Integer> headersMap = csvFileParser.getHeaderMap();

			// Get a list of CSV file records
			List<CSVRecord> csvRecords = csvFileParser.getRecords();
			String collName = null;
			// Read the CSV file records starting from the second record to skip
			// the header
			for (int i = 0; i < csvRecords.size(); i++) {
				CSVRecord record = csvRecords.get(i);
            	boolean processedRecordFlag = true;
				HpcFileLocation source = new HpcFileLocation();

				List<HpcMetadataEntry> listOfhpcCollection = new ArrayList<HpcMetadataEntry>();
				for (Entry<String, Integer> entry : headersMap.entrySet()) {
					String cellVal = record.get(entry.getKey());
					HpcMetadataEntry hpcMetadataEntry = new HpcMetadataEntry();
					hpcMetadataEntry.setAttribute(entry.getKey());
					hpcMetadataEntry.setValue(cellVal);
					if(entry.getKey().equals("source_globus_endpoint"))
					{
						source.setEndpoint(cellVal);
						continue;
					}
					else if(entry.getKey().equals("source_globus_path"))
					{
						source.setPath(cellVal);
						continue;
					}
					else if(entry.getKey().equals("object_path"))
					{
						collName = cellVal;
						continue;
					}
						
					if (StringUtils.isNotBlank(cellVal))
						listOfhpcCollection.add(hpcMetadataEntry);
				}

				HpcDataObjectRegistrationDTO hpcDataObjectRegistrationDTO = new HpcDataObjectRegistrationDTO();
				hpcDataObjectRegistrationDTO.getMetadataEntries().addAll(listOfhpcCollection);

				System.out.println("Adding file from " + source.getPath());
				
				
				hpcDataObjectRegistrationDTO.setSource(source);
				hpcDataObjectRegistrationDTO.setFilePath("/");
				RestTemplate restTemplate = HpcClientUtil.getRestTemplate(hpcCertPath, hpcCertPassword);
				
				HttpHeaders headers = new HttpHeaders();
				String token = DatatypeConverter.printBase64Binary((userId + ":" + password).getBytes());
				headers.add("Authorization", "Basic " + token);
				List<MediaType> mediaTypeList = new ArrayList<MediaType>();
				mediaTypeList.add(MediaType.APPLICATION_JSON);
				headers.setAccept(mediaTypeList);

				HttpEntity<HpcDataObjectRegistrationDTO> entity = new HttpEntity<HpcDataObjectRegistrationDTO>(
						hpcDataObjectRegistrationDTO, headers);
				try {
					if (!collName.startsWith("/"))
						collName = "/" + collName;

					System.out.println(hpcServerURL + "/" + hpcDataService  + collName);
					response = restTemplate.exchange(
							hpcServerURL + "/" + hpcDataService + collName, HttpMethod.PUT,
							entity, HpcExceptionDTO.class);
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
					//System.out.println(message);
					addErrorToLog(message, i+1);
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					String exceptionAsString = sw.toString();
					addErrorToLog(exceptionAsString, i+1);
					addRecordToLog(record, headersMap);
				} catch (RestClientException e) {
					success = false;
					processedRecordFlag = false;
					String message = "Failed to process record due to: "+e.getMessage();
					//System.out.println(message);
					addErrorToLog(message, i+1);
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					String exceptionAsString = sw.toString();
					addErrorToLog(exceptionAsString, i+1);
					addRecordToLog(record, headersMap);
				} catch (Exception e) {
					success = false;
					processedRecordFlag = false;
					String message = "Failed to process record due to: "+e.getMessage();
					//System.out.println(message);
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

		} catch (Exception e) {
			System.out.println("Cannot read the input file");
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			try {
				addErrorToLog(exceptionAsString, 0);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} finally {
			try {
				fileReader.close();
				csvFileParser.close();
			} catch (IOException e) {
				System.out.println("Error while closing fileReader/csvFileParser !!!");
			}
		}
		return success;

	}

	/*
	 * private RestTemplate getRestTemplate(String userId, String password) {
	 * CloseableHttpClient httpClient = HttpClients.custom()
	 * .setSSLHostnameVerifier(new NoopHostnameVerifier()) .build();
	 * HttpComponentsClientHttpRequestFactory requestFactory = new
	 * HttpComponentsClientHttpRequestFactory();
	 * requestFactory.setHttpClient(httpClient); RestTemplate restTemplate = new
	 * RestTemplate(requestFactory); List<HttpMessageConverter<?>>
	 * messageConverters = new ArrayList<HttpMessageConverter<?>>();
	 * messageConverters.add(new FormHttpMessageConverter());
	 * messageConverters.add(new StringHttpMessageConverter());
	 * messageConverters.add(new MappingJackson2HttpMessageConverter());
	 * 
	 * restTemplate.setMessageConverters(messageConverters); return
	 * restTemplate; }
	 */
	private void addToErrorCollection(String message, CSVRecord record, Map<String, Integer> headers) {
		String logDir = configProperties.getProperty("hpc.error-log.dir");

		JSONParser parser = new JSONParser();
		JSONObject hpcException = null;
		FileWriter fileWriter = null;
		CSVPrinter csvFilePrinter = null;
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");

		try {
			String logFile = logDir + "/" + "errorLog" + new SimpleDateFormat("yyyyMMddhhmm'.csv'").format(new Date());
			File file = new File(logFile);
			if (!file.exists()) {
				file.createNewFile();
			}
			fileWriter = new FileWriter(file, true);
			csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
			Object[] headerArray = new ArrayList<Object>(headers.keySet()).toArray();
			if (!checkIfHeaderExists(logFile))
				csvFilePrinter.printRecord(headerArray);
			else
				csvFilePrinter.println();
			for (Entry<String, Integer> entry : headers.entrySet()) {
				csvFilePrinter.print(record.get(entry.getKey()));
			}
			csvFilePrinter.print(message);
		} catch (Exception e) {
			System.out.println("Error in Writing the error log !!!");
			e.printStackTrace();
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();
				csvFilePrinter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter/csvPrinter !!!");
				e.printStackTrace();
			}
		}
		// return (String) exceptioDTO.get("message");
	}

	private boolean checkIfHeaderExists(String logFile) throws IOException {
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withHeader();
		FileReader fileReader = new FileReader(logFile);
		CSVParser csvFileParser = new CSVParser(fileReader, csvFileFormat);
		Map<String, Integer> headersMap = csvFileParser.getHeaderMap();
		// System.out.println("HEADER MAP::" + headersMap);
		if (headersMap != null && !headersMap.isEmpty())
			return true;
		else
			return false;
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
