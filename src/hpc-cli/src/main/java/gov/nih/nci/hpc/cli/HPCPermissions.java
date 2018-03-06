/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli;

import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcConfigProperties;
import gov.nih.nci.hpc.cli.util.Paths;
import gov.nih.nci.hpc.domain.datamanagement.HpcGroupPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class HPCPermissions extends HPCBatchClient {

  private static final SimpleDateFormat DATE_TIME_FORMAT =
      new SimpleDateFormat("yyyyMMddhhmm");

  private static final String FILE_EXTENSION_CSV = ".csv";
  private static final String FILE_EXTENSION_TXT = ".txt";
  private static final String LOG_FILE_NAME_PREFIX = "putPermissions_errorLog";
  private static final String LOG_RECORDS_FILE_NAME_PREFIX = "putPermissions_errorRecords";

  private static String generateDateTimeStampString() {
    return DATE_TIME_FORMAT.format(new Date());
  }

  private static String generateLogFileName() {
    final StringBuilder sbLogFileNameBuilder = new StringBuilder();
    sbLogFileNameBuilder.append(LOG_FILE_NAME_PREFIX).append(generateDateTimeStampString())
        .append(FILE_EXTENSION_TXT);
    return sbLogFileNameBuilder.toString();
  }

  private static String generateLogRecordsFileName() {
    final StringBuilder sbLogRecordsFileNameBuilder = new StringBuilder();
    sbLogRecordsFileNameBuilder.append(LOG_RECORDS_FILE_NAME_PREFIX)
        .append(generateDateTimeStampString())
        .append(FILE_EXTENSION_CSV);
    return sbLogRecordsFileNameBuilder.toString();
  }

  @Autowired
	private HpcConfigProperties configProperties;

	public HPCPermissions() {
		super();
	}

  protected void initializeLog() {
    try {
      File theLogFile = new File(
          Paths.generateFileSystemResourceUri(logDir, generateLogFileName()));
      logFile = theLogFile.getPath();
      if (!theLogFile.exists()) {
        theLogFile.createNewFile();
      }
      fileLogWriter = new FileWriter(theLogFile, true);

      File theLogRecordsFile = new File(
          Paths.generateFileSystemResourceUri(logDir, generateLogRecordsFileName()));
      logRecordsFile = theLogRecordsFile.getPath();
      if (!theLogRecordsFile.exists()) {
        theLogRecordsFile.createNewFile();
      }
      fileRecordWriter = new FileWriter(theLogRecordsFile, true);
      csvFilePrinter = new CSVPrinter(fileRecordWriter,
          CSVFormat.DEFAULT.withRecordSeparator("\n"));
    } catch (IOException e) {
      System.out.println("Failed to initialize Batch process: " + e.getMessage());
    }
  }

	protected String processFile(String fileName, String userId, String password, String authToken) {
		boolean success = true;
		FileReader fileReader = null;
		CSVParser csvFileParser = null;

		if (authToken == null && (userId == null || userId.trim().length() == 0 || password == null || password.trim().length() == 0)) {
			System.out.println("Invalid login credentials");
			return Constants.CLI_1;
		}

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
			Map<String, CSVRecord> records = new HashMap<String, CSVRecord>();
			Map<String, String> pathTypes = new HashMap<String, String>();
			if(authToken == null)
				authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath,
					hpcCertPassword);
			for (int i = 0; i < csvRecords.size(); i++) {
				CSVRecord record = csvRecords.get(i);
				String path = record.get(Constants.PATH);
				String ruserId = record.get(Constants.USER_ID);
				String type = record.get(Constants.TYPE);
				String rGroupId = record.get(Constants.GROUP_ID);
				String permission = record.get(Constants.PERMISSION);
				pathTypes.put(path, type);
				records.put(path, record);
				if (ruserId != null && !ruserId.isEmpty()) {
					List<HpcUserPermission> pathPermission = userPermissions.get(path);
					if (pathPermission == null)
						pathPermission = new ArrayList<HpcUserPermission>();
					HpcUserPermission userPermission = new HpcUserPermission();
					userPermission.setUserId(ruserId);
					userPermission.setPermission(HpcPermission.valueOf(permission));
					pathPermission.add(userPermission);
					userPermissions.put(path, pathPermission);
				}
				if (rGroupId != null && !rGroupId.isEmpty()) {
					List<HpcGroupPermission> pathPermission = groupPermissions.get(path);
					if (pathPermission == null)
						pathPermission = new ArrayList<HpcGroupPermission>();
					HpcGroupPermission groupPermission = new HpcGroupPermission();
					groupPermission.setGroupName(rGroupId);
					groupPermission.setPermission(HpcPermission.valueOf(permission));
					pathPermission.add(groupPermission);
					groupPermissions.put(path, pathPermission);
				}
			}
			Iterator permissionsIterator = userPermissions.keySet().iterator();
			while (permissionsIterator.hasNext()) {
				String path = (String) permissionsIterator.next();
				updatePermissions(pathTypes.get(path), path, authToken, userPermissions.get(path),
						groupPermissions.get(path), records.get(path), headersMap);
				groupPermissions.remove(path);
			}
			Iterator groupPermissionsIterator = groupPermissions.keySet().iterator();
			while (groupPermissionsIterator.hasNext()) {
				String path = (String) groupPermissionsIterator.next();
				updatePermissions(pathTypes.get(path), path, authToken, null, groupPermissions.get(path),
						records.get(path), headersMap);
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
			return Constants.CLI_5;
		} finally {
			try {
				fileReader.close();
				csvFileParser.close();
			} catch (IOException e) {
				System.out.println("Error while closing fileReader/csvFileParser !!!");
			}
		}
		return Constants.CLI_0;

	}


	private String updatePermissions(String type, String path, String authToken,
			List<HpcUserPermission> userPermissions, List<HpcGroupPermission> groupPermissions, CSVRecord record,
			Map<String, Integer> headersMap) throws IOException {
		String returnCode = null;
		HpcEntityPermissionsDTO dto = new HpcEntityPermissionsDTO();
		if (userPermissions != null && userPermissions.size() > 0)
			dto.getUserPermissions().addAll(userPermissions);
		if (groupPermissions != null && groupPermissions.size() > 0)
			dto.getGroupPermissions().addAll(groupPermissions);
		RestTemplate restTemplate = HpcClientUtil.getRestTemplate(hpcCertPath, hpcCertPassword);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + authToken);
		List<MediaType> mediaTypeList = new ArrayList<MediaType>();
		mediaTypeList.add(MediaType.APPLICATION_JSON);
		headers.setAccept(mediaTypeList);
		HttpEntity<?> entity = new HttpEntity<Object>(dto, headers);
		try {
			String url = hpcServerURL + (type.equalsIgnoreCase("collection") ? "/collection" : "/dataObject") + path
					+ "/acl";
			restTemplate.postForEntity(url, entity, null);
		} catch (HttpStatusCodeException e) {
			returnCode = Constants.CLI_5;
			String message = "Failed to process record due to: " + e.getMessage();
			// System.out.println(message);
			addErrorToLog(path, message);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			addErrorToLog(path, exceptionAsString);
			addRecordToLog(record, headersMap);

		} catch (RestClientException e) {
			returnCode = Constants.CLI_5;
			String message = "Failed to process record due to: " + e.getMessage();
			// System.out.println(message);
			addErrorToLog(path, message);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			addErrorToLog(path, exceptionAsString);
			addRecordToLog(record, headersMap);
		} catch (Exception e) {
			returnCode = Constants.CLI_5;
			String message = "Failed to process record due to: " + e.getMessage();
			// System.out.println(message);
			addErrorToLog(path, message);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			addErrorToLog(path, exceptionAsString);
			addRecordToLog(record, headersMap);
		}
		return returnCode;
	}

}
