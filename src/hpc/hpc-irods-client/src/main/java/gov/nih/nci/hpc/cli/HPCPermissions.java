package gov.nih.nci.hpc.cli;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
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
			Map<String, List<HpcUserPermission>> permissions = new HashMap<String, List<HpcUserPermission>>();
			for (int i = 0; i < csvRecords.size(); i++) {
				CSVRecord record = csvRecords.get(i);
				String path = record.get(Constants.PATH);
				String ruserId = record.get(Constants.USER_ID);
				String permission = record.get(Constants.PERMISSION);
				List<HpcUserPermission> pathPermission = permissions.get(path);
				if (pathPermission == null)
					pathPermission = new ArrayList<HpcUserPermission>();
				HpcUserPermission userPermission = new HpcUserPermission();
				userPermission.setUserId(ruserId);
				userPermission.setPermission(permission);
				pathPermission.add(userPermission);
				permissions.put(path, pathPermission);

				List<HpcEntityPermissionRequestDTO> dtos = new ArrayList<HpcEntityPermissionRequestDTO>();
				HpcEntityPermissionRequestDTO hpcPermissionDTO = new HpcEntityPermissionRequestDTO();
				hpcPermissionDTO.setPath(path);
				hpcPermissionDTO.getUserPermissions().addAll(permissions.get(path));
				dtos.add(hpcPermissionDTO);
				//System.out.println(dtos);
				RestTemplate restTemplate = HpcClientUtil.getRestTemplate(userId, password, hpcCertPath, hpcCertPassword);
				HttpHeaders headers = new HttpHeaders();
				String token = DatatypeConverter.printBase64Binary((userId + ":" + password).getBytes());
				headers.add("Authorization", "Basic " + token);
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
					System.out.println(message);
					addErrorToLog(message, i + 1);
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					String exceptionAsString = sw.toString();
					addErrorToLog(exceptionAsString, i + 1);
					addRecordToLog(record, headersMap);

				} catch (RestClientException e) {
					success = false;
					String message = "Failed to process record due to: " + e.getMessage();
					System.out.println(message);
					addErrorToLog(message, i + 1);
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					String exceptionAsString = sw.toString();
					addErrorToLog(exceptionAsString, i + 1);
					addRecordToLog(record, headersMap);
				} catch (Exception e) {
					success = false;
					String message = "Failed to process record due to: " + e.getMessage();
					System.out.println(message);
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
