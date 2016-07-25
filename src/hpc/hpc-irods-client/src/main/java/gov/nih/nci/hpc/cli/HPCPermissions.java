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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcConfigProperties;
import gov.nih.nci.hpc.domain.datamanagement.HpcGroupPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionResponseListDTO;

@Component
public class HPCPermissions extends HPCBatchClient {
	@Autowired
	private HpcConfigProperties configProperties;

	public HPCPermissions() {
		super();
	}

	protected void initializeLog()
	{
		logFile = logDir + File.separator + "putPermissions_errorLog" + new SimpleDateFormat("yyyyMMddhhmm'.txt'").format(new Date());
		logRecordsFile = logDir + File.separator + "putPermissions_errorRecords"
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
		try {
			// initialize FileReader object
			fileReader = new FileReader(fileName);
			// initialize CSVParser object
			csvFileParser = new CSVParser(fileReader, csvFileFormat);
			Map<String, Integer> headersMap = csvFileParser.getHeaderMap();
			// Get a list of CSV file records
			List<CSVRecord> csvRecords = csvFileParser.getRecords();
			// Read the CSV file records starting from the second record to skip
			// the header
			Map<String, List<HpcUserPermission>> userPermissions = new HashMap<String, List<HpcUserPermission>>();
			Map<String, List<HpcGroupPermission>> groupPermissions = new HashMap<String, List<HpcGroupPermission>>();
			String authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL);
			for (int i = 0; i < csvRecords.size(); i++) {
				CSVRecord record = csvRecords.get(i);
				String path = record.get(Constants.PATH);
				String ruserId = record.get(Constants.USER_ID);
				String rGroupId = record.get(Constants.GROUP_ID);
				String permission = record.get(Constants.PERMISSION);
				if(ruserId != null && !ruserId.isEmpty())
				{
					List<HpcUserPermission> pathPermission = userPermissions.get(path);
					if (pathPermission == null)
						pathPermission = new ArrayList<HpcUserPermission>();
					HpcUserPermission userPermission = new HpcUserPermission();
					userPermission.setUserId(ruserId);
					userPermission.setPermission(permission);
					pathPermission.add(userPermission);
					userPermissions.put(path, pathPermission);
				}
				if(rGroupId != null && !rGroupId.isEmpty())
				{
					List<HpcGroupPermission> pathPermission = groupPermissions.get(path);
					if (pathPermission == null)
						pathPermission = new ArrayList<HpcGroupPermission>();
					HpcGroupPermission groupPermission = new HpcGroupPermission();
					groupPermission.setGroupId(ruserId);
					groupPermission.setPermission(permission);
					pathPermission.add(groupPermission);
					groupPermissions.put(path, pathPermission);
				}
				List<HpcEntityPermissionRequestDTO> dtos = new ArrayList<HpcEntityPermissionRequestDTO>();
				HpcEntityPermissionRequestDTO hpcPermissionDTO = new HpcEntityPermissionRequestDTO();
				hpcPermissionDTO.setPath(path);
				if(userPermissions.get(path) != null && userPermissions.get(path).size() > 0)
					hpcPermissionDTO.getUserPermissions().addAll(userPermissions.get(path));
				if(groupPermissions.get(path) != null && groupPermissions.get(path).size() > 0)
					hpcPermissionDTO.getGroupPermissions().addAll(groupPermissions.get(path));
				dtos.add(hpcPermissionDTO);
				//System.out.println(dtos);
				RestTemplate restTemplate = HpcClientUtil.getRestTemplate(hpcCertPath, hpcCertPassword);
				HttpHeaders headers = new HttpHeaders();
				String token = DatatypeConverter.printBase64Binary((userId + ":" + password).getBytes());
				//headers.add("Authorization", "Basic " + token);
				headers.add("Authorization", "Bearer " + authToken);
				List<MediaType> mediaTypeList = new ArrayList<MediaType>();
				mediaTypeList.add(MediaType.APPLICATION_JSON);
				headers.setAccept(mediaTypeList);
				HttpEntity<?> entity = new HttpEntity<Object>(dtos, headers);
				try {
					//HttpEntity<HpcEntityPermissionResponseListDTO> response = restTemplate.postForEntity(hpcServerURL + "/acl", entity, HpcEntityPermissionResponseListDTO.class);
					restTemplate.postForEntity(hpcServerURL + "/acl", entity, null);
				} catch (HttpStatusCodeException e) {
					success = false;
					String message = "Failed to process record due to: " + e.getMessage();
					//System.out.println(message);
					addErrorToLog(message, i + 1);
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					String exceptionAsString = sw.toString();
					addErrorToLog(exceptionAsString, i + 1);
					addRecordToLog(record, headersMap);

				} catch (RestClientException e) {
					success = false;
					String message = "Failed to process record due to: " + e.getMessage();
					//System.out.println(message);
					addErrorToLog(message, i + 1);
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					String exceptionAsString = sw.toString();
					addErrorToLog(exceptionAsString, i + 1);
					addRecordToLog(record, headersMap);
				} catch (Exception e) {
					success = false;
					String message = "Failed to process record due to: " + e.getMessage();
					//System.out.println(message);
					addErrorToLog(message, i + 1);
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					String exceptionAsString = sw.toString();
					addErrorToLog(exceptionAsString, i + 1);
					addRecordToLog(record, headersMap);
				}
			}
		} catch (Exception e) {
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
