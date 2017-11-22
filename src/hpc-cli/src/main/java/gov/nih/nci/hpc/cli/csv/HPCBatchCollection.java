/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.csv;

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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
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

import gov.nih.nci.hpc.cli.HPCBatchClient;
import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

@Component
public class HPCBatchCollection extends HPCBatchClient {

	public HPCBatchCollection() {
		super();
	}

	protected void initializeLog() {
		logFile = logDir + File.separator + "putCollections_errorLog"
				+ new SimpleDateFormat("yyyyMMddhhmm'.txt'").format(new Date());
		logRecordsFile = logDir + File.separator + "putCollections_errorRecords"
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
		}

	}

	protected boolean processFile(String fileName, String userId, String password) {
		boolean success = true;
		FileReader fileReader = null;
		CSVParser csvFileParser = null;
		String hpcDataService = null;

		// Create the CSVFormat object with the header mapping
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withHeader();

		try {

			// initialize FileReader object
			fileReader = new FileReader(fileName);

			// initialize CSVParser object
			csvFileParser = new CSVParser(fileReader, csvFileFormat);

			Map<String, Integer> headersMap = csvFileParser.getHeaderMap();

			// Get a list of CSV file records
			List<CSVRecord> csvRecords = csvFileParser.getRecords();
			String collectionPath = null;
			ResponseEntity<HpcExceptionDTO> response = null;
			// Read the CSV file records starting from the second record to skip
			// the header
			String authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath,
					hpcCertPassword);
			for (int i = 0; i < csvRecords.size(); i++) {
				boolean processedRecordFlag = true;
				boolean createParentCollection = false;
				CSVRecord record = csvRecords.get(i);
				List<HpcMetadataEntry> metadataAttributes = new ArrayList<HpcMetadataEntry>();
				List<HpcMetadataEntry> parentMetadataAttributes = new ArrayList<HpcMetadataEntry>();
				for (Entry<String, Integer> entry : headersMap.entrySet()) {
					String cellVal = record.get(entry.getKey());
					if (entry.getKey().equals(Constants.COLLECTION_PATH))
						collectionPath = cellVal;
					if (entry.getKey().equals(Constants.CREATE_PARENT_COLLECTION))
						createParentCollection = entry.getKey().equalsIgnoreCase("true");
					HpcMetadataEntry hpcMetadataEntry = new HpcMetadataEntry();
					hpcMetadataEntry.setAttribute(entry.getKey());
					hpcMetadataEntry.setValue(cellVal);
					createParentCollection = entry.getKey().equalsIgnoreCase("true");
					if (StringUtils.isNotBlank(cellVal)) {
						if (entry.getKey().startsWith(Constants.PARENT_COLLECTION_PREFIX))
							parentMetadataAttributes.add(hpcMetadataEntry);
						else
							metadataAttributes.add(hpcMetadataEntry);
					}
				}
				HpcCollectionRegistrationDTO collectionDTO = buildCollectionDTO(metadataAttributes,
						parentMetadataAttributes, createParentCollection);
				
				if(HpcClientUtil.containsWhiteSpace(collectionPath.trim()))
				{
					throw new HpcBatchException("Whitespace is not allowed in collection path");
				}
				
				System.out.println((i + 1) + ": Registering Collection " + collectionPath);

				RestTemplate restTemplate = HpcClientUtil.getRestTemplate(hpcCertPath, hpcCertPassword);
				if (authToken == null) {
					System.out.println("Invalid authenticaiton. Aborting the batch processing.");
					return false;
				}
				// System.out.println("token: "+authToken);
				HttpHeaders headers = new HttpHeaders();
				headers.add("Accept", "*/*");
				// String token =DatatypeConverter.printBase64Binary((userId +
				// ":" + password).getBytes());
				headers.add("Authorization", "Bearer " + authToken);
				List<MediaType> mediaTypeList = new ArrayList<MediaType>();
				mediaTypeList.add(MediaType.APPLICATION_JSON);
				headers.setAccept(mediaTypeList);
				HttpEntity<HpcCollectionRegistrationDTO> entity = new HttpEntity<HpcCollectionRegistrationDTO>(
						collectionDTO, headers);
				try {
					if (!collectionPath.startsWith("/"))
						collectionPath = "/" + collectionPath;

					System.out.println(hpcServerURL + "/" + hpcCollectionService + collectionPath);
					response = restTemplate.exchange(hpcServerURL + "/" + hpcCollectionService + collectionPath,
							HttpMethod.PUT, entity, HpcExceptionDTO.class);
					if (response != null) {
						HpcExceptionDTO exception = response.getBody();
						if (exception != null) {
							String message = "Failed to process record due to: " + exception.getMessage()
									+ ": Error Type:" + exception.getErrorType().value() + ": Request reject reason: "
									+ exception.getRequestRejectReason().value();
							addErrorToLog(message, i + 1);
							success = false;
							processedRecordFlag = false;
							addRecordToLog(record, headersMap);
						} else if (!(response.getStatusCode().equals(HttpStatus.CREATED)
								|| response.getStatusCode().equals(HttpStatus.OK))) {
							addErrorToLog("Failed to process record due to unknown error. Return code: "
									+ response.getStatusCode(), i + 1);
							success = false;
							processedRecordFlag = false;
							addRecordToLog(record, headersMap);
						}
					}
				} catch (HpcBatchException e) {
					success = false;
					processedRecordFlag = false;
					String message = "Failed to process record due to: " + e.getMessage();
					// System.out.println(message);
					addErrorToLog(message, i + 1);
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					String exceptionAsString = sw.toString();
					addErrorToLog(exceptionAsString, i + 1);
					addRecordToLog(record, headersMap);
				} catch (RestClientException e) {
					// e.printStackTrace();
					success = false;
					processedRecordFlag = false;
					String message = "Failed to process record due to: " + e.getMessage();
					// System.out.println(message);
					addErrorToLog(message, i + 1);
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					String exceptionAsString = sw.toString();
					addErrorToLog(exceptionAsString, i + 1);
					addRecordToLog(record, headersMap);
				} catch (Exception e) {
					// e.printStackTrace();
					success = false;
					processedRecordFlag = false;
					String message = "Failed to process record due to: " + e.getMessage();
					// System.out.println(message);
					addErrorToLog(message, i + 1);
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					String exceptionAsString = sw.toString();
					addErrorToLog(exceptionAsString, i + 1);
					addRecordToLog(record, headersMap);
				}
				if (processedRecordFlag)
					System.out.println("Success!");
				else
					System.out.println("Failure!");
				System.out.println("---------------------------------");

			}

		} catch (Exception e) {
			System.out.println("Cannot read the input file: "+e.getMessage());
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

	private HpcCollectionRegistrationDTO buildCollectionDTO(List<HpcMetadataEntry> metadataList,
			List<HpcMetadataEntry> parentMetadataAttributes, boolean createParentCollection) {
		HpcCollectionRegistrationDTO dto = new HpcCollectionRegistrationDTO();
		dto.getMetadataEntries().addAll(metadataList);
		dto.setCreateParentCollections(createParentCollection);
		if (!parentMetadataAttributes.isEmpty())
			dto.getParentCollectionMetadataEntries().addAll(parentMetadataAttributes);
		return dto;
	}
}
