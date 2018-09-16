/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.csv;

import gov.nih.nci.hpc.cli.util.HpcCmdException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.Optional;
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
import org.springframework.web.util.UriComponentsBuilder;

import gov.nih.nci.hpc.cli.HPCBatchClient;
import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

@Component
public class HPCBatchCollection extends HPCBatchClient {

  public static final CSVFormat CSV_FILE_FORMAT =
      CSVFormat.DEFAULT.withHeader().withQuote(null);

  private static final String ATTRIB_NM_ERR_CODE = "error code";
  private static final String ATTRIB_NM_ERR_TYPE = "error type";
  private static final String ATTRIB_NM_REASON = "reason";
  private static final String ATTRIB_NM_STACKTRACE = "stacktrace";
  private static final String ATTRIB_POS_STR_DIVIDER = "#";
  private static final String COLON = ":";

  private Boolean currentRecCreateParentCollctn;
  private CSVRecord currentCsvRecord;
  //private Exception lastException;
  private List<CSVRecord> csvRecords;
  private Map<String, Integer> headersMap;
  private Optional<String> currFailMsg;
  private RestTemplate restTemplate;
  private String currentRecCollectionPath;


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

	protected String processFile(String fileName, String userId, String password,
    String authToken) throws HpcCmdException {
		if (authToken == null && (userId == null || userId.trim().length() == 0 ||
        password == null || password.trim().length() == 0)) {
			System.out.println("Invalid login credentials");
			return Constants.CLI_1;
		}

		String effToken = authToken;
    if (effToken == null) {
      effToken = HpcClientUtil.getAuthenticationToken(userId, password,
        this.hpcServerURL, this.hpcServerProxyURL, this.hpcServerProxyPort,
        this.hpcCertPath, this.hpcCertPassword);
    }

    String returnCode = null;
    try {
      parseCsvInputFile(fileName);

      this.currFailMsg = Optional.empty();
			for (int i = 0; i < this.csvRecords.size(); i++) {
			  int recordIndex = i + 1;
				this.currentCsvRecord = this.csvRecords.get(i);
        boolean processedRecordFlag = true;
				try {
          System.out.print(recordIndex + ": Registering Collection ");

          HpcCollectionRegistrationDTO collectionDTO = convertCsvRecord2Dto(
              this.currentCsvRecord, recordIndex);

          System.out.print(this.currentRecCollectionPath + "\n");

          Optional<String> resultSignal = performDmeRestServiceCall(effToken,
            collectionDTO, recordIndex);
          if (resultSignal.isPresent()) {
            processedRecordFlag = false;
            returnCode = resultSignal.get();
          } else {
            processedRecordFlag = true;
            returnCode = null;
          }
				} catch (Exception e) {
				  //this.currFailMsg = Optional.of(e.getMessage());
          Optional<Map<String,String>> errorDetails =
            extractErrorDetails(e.getMessage());
          if (errorDetails.isPresent()) {
            String msg = "Failed to process record; see subsequent error" +
              " details." +
              "\n  Error Code: " +  errorDetails.get().get("errorCode") +
              "\n  Error Type: " +  errorDetails.get().get("errorType") +
              "\n  Reason: " +  errorDetails.get().get("reason");
            this.currFailMsg = Optional.of(msg);
          } else {
            this.currFailMsg = Optional.of(e.getMessage());
          }
          putErrorInfoInLogs(this.headersMap, recordIndex,
            this.currentCsvRecord, e);
					processedRecordFlag = false;
          returnCode = Constants.CLI_5;
				}
        System.out.println(processedRecordFlag ? "Success!" : "Failure!");
				if (!processedRecordFlag && this.currFailMsg.isPresent()) {
				  System.out.println(this.currFailMsg.get());
        }
				System.out.println("---------------------------------");
        this.currFailMsg = Optional.empty();
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
      // clean up code here if/as needed
		}

		return returnCode;
	}


  private HpcCollectionRegistrationDTO buildCollectionDTO(
    List<HpcMetadataEntry> metadataList,
    List<HpcMetadataEntry> parentMetadataAttributes) {
		HpcCollectionRegistrationDTO dto = new HpcCollectionRegistrationDTO();
		dto.getMetadataEntries().addAll(metadataList);
		dto.setCreateParentCollections(this.currentRecCreateParentCollctn);
		if (!parentMetadataAttributes.isEmpty())
			dto.getParentCollectionsBulkMetadataEntries().getDefaultMetadataEntries().addAll(parentMetadataAttributes);

		return dto;
	}


  private HttpEntity<HpcCollectionRegistrationDTO>
    buildHttpEntity4CollectionRegistration(
      String authToken, HpcCollectionRegistrationDTO collectionDTO) {
    List<MediaType> mediaTypeList = new ArrayList<MediaType>();
    mediaTypeList.add(new MediaType(MediaType.APPLICATION_JSON.getType(),
      MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8));
    HttpHeaders headers = new HttpHeaders();
    // String token =DatatypeConverter.printBase64Binary((userId + ":" + password).getBytes());
    headers.add("Authorization", "Bearer " + authToken);
    headers.add("Accept", "*/*");
    headers.setAccept(mediaTypeList);
    return new HttpEntity<HpcCollectionRegistrationDTO>(collectionDTO, headers);
  }


  private HpcCollectionRegistrationDTO convertCsvRecord2Dto(CSVRecord record,
      int recordIndex) {
    this.currentRecCollectionPath = null;
    this.currentRecCreateParentCollctn = Boolean.FALSE;
    List<HpcMetadataEntry> metadataAttributes = new
        ArrayList<HpcMetadataEntry>();
    List<HpcMetadataEntry> parentMetadataAttributes = new
        ArrayList<HpcMetadataEntry>();
    for (Entry<String, Integer> entry : this.headersMap.entrySet()) {
      String fieldName = entry.getKey();
      String fieldValue = record.get(fieldName);

      if (Constants.COLLECTION_PATH.equals(fieldName)) {
        this.currentRecCollectionPath = fieldValue.trim();
        try {
          HpcClientUtil.validateDmeArchivePath(this.currentRecCollectionPath);
        } catch (HpcCmdException e) {
          String enhancedMsg = "Failed to process CSV record " + recordIndex +
              " of " + this.csvRecords.size() + " due to: [" + e.getMessage() +
              "].\n" + "  Echo CSV record: [" + record.toString() + "].\n\n";
          this.currFailMsg = Optional.of(enhancedMsg);
          throw new HpcCmdException(enhancedMsg);
        }
      } else if (Constants.CREATE_PARENT_COLLECTION.equals(fieldName)) {
        this.currentRecCreateParentCollctn = Boolean.TRUE.toString()
            .equalsIgnoreCase(fieldValue);
      }

      if (StringUtils.isNotBlank(fieldValue)) {
        HpcMetadataEntry hpcMetadataEntry = new HpcMetadataEntry();
        hpcMetadataEntry.setAttribute(fieldName);
        hpcMetadataEntry.setValue(fieldValue);
        if (fieldName.startsWith(Constants.PARENT_COLLECTION_PREFIX)) {
          parentMetadataAttributes.add(hpcMetadataEntry);
        } else {
          metadataAttributes.add(hpcMetadataEntry);
        }
      }
    }

    return buildCollectionDTO(metadataAttributes, parentMetadataAttributes);
  }


  private Optional<Map<String,String>> extractErrorDetails(String rawErrorMsg) {
    Optional<Map<String,String>> retMap = Optional.empty();

    String msgLowCase = rawErrorMsg.toLowerCase();
    int posErrCode = 0, posErrType = 0, posReason = 0, posStacktrace = 0;
    boolean allDetailsDetected = false;
    posErrCode = msgLowCase.indexOf(ATTRIB_NM_ERR_CODE + COLON);
    if (posErrCode != -1) {
      posErrType = msgLowCase.indexOf(ATTRIB_NM_ERR_TYPE + COLON);
      if (posErrType != -1) {
        posReason = msgLowCase.indexOf(ATTRIB_NM_REASON + COLON);
        if (posReason != -1) {
          posStacktrace = msgLowCase.indexOf(ATTRIB_NM_STACKTRACE + COLON);
          allDetailsDetected = (posStacktrace != -1);
        }
      }
    }
    if (allDetailsDetected) {
      String[] attribPosStrings = {
          (String.format("%010d", posErrCode) + ATTRIB_POS_STR_DIVIDER +
              ATTRIB_NM_ERR_CODE + COLON),
          (String.format("%010d",posErrType) + ATTRIB_POS_STR_DIVIDER +
              ATTRIB_NM_ERR_TYPE + COLON),
          (String.format("%010d", posReason) + ATTRIB_POS_STR_DIVIDER +
              ATTRIB_NM_REASON + COLON),
          (String.format("%010d", posStacktrace) + ATTRIB_POS_STR_DIVIDER +
              ATTRIB_NM_STACKTRACE + COLON)
      };
      Arrays.sort(attribPosStrings);
      String attribValErrCode = null, attribValErrType = null,
          attribValReason = null, priorAttribNm = null;
      int hashPos = -1, priorAttribPos = -1, attribPosStringsPtr = 0;
      while (null == attribValErrCode || null == attribValErrType ||
          null == attribValReason) {
        if (attribPosStringsPtr == 0) {
          hashPos = attribPosStrings[0].indexOf("#");
          priorAttribPos = Integer.valueOf(attribPosStrings[0].substring(0, hashPos));
          priorAttribNm = attribPosStrings[0].substring(hashPos + 1);
          attribPosStringsPtr = 1;
        }
        hashPos = attribPosStrings[attribPosStringsPtr].indexOf("#");
        int currAttribPos = Integer.valueOf(
            attribPosStrings[attribPosStringsPtr].substring(0, hashPos));
        String someAttribVal = rawErrorMsg.substring(priorAttribPos +
            priorAttribNm.length(), currAttribPos).trim();
        if (priorAttribNm.startsWith(ATTRIB_NM_ERR_CODE)) {
          attribValErrCode = someAttribVal;
        } else if (priorAttribNm.startsWith(ATTRIB_NM_ERR_TYPE)) {
          attribValErrType = someAttribVal;
        } else if (priorAttribNm.startsWith(ATTRIB_NM_REASON)) {
          attribValReason = someAttribVal;
        } else {
          // do nothing, don't care about stacktrace detail
        }
        priorAttribPos = currAttribPos;
        priorAttribNm = attribPosStrings[attribPosStringsPtr].substring(hashPos + 1);
        attribPosStringsPtr += 1;
      }
      Map<String,String> detailsMap = new HashMap<>();
      detailsMap.put("errorCode", attribValErrCode);
      detailsMap.put("errorType", attribValErrType);
      detailsMap.put("reason", attribValReason);
      retMap = Optional.of(detailsMap);
    }

    return retMap;
  }


  private void parseCsvInputFile(String fileName) throws HpcBatchException {
    try (FileReader fileReader = new FileReader(fileName)) {
      CSVParser parser = new CSVParser(fileReader, CSV_FILE_FORMAT);
      this.headersMap = parser.getHeaderMap();
      this.csvRecords = parser.getRecords();
    } catch (IOException e) {
      String msg = "Failed to read in CSV file data!";
      throw new HpcBatchException(msg, e);
    }
  }


  private Optional<String> performDmeRestServiceCall(
    String authTokn, HpcCollectionRegistrationDTO collRegDto, int csvRecIndex)
    throws IOException {
    Optional<String> retSignal = Optional.empty();

    final URI uri2Apply = UriComponentsBuilder
      .fromHttpUrl(this.hpcServerURL)
      .path(HpcClientUtil.prependForwardSlashIfAbsent(
        this.hpcCollectionService).concat("/{dme-archive-path}"))
      .buildAndExpand(this.currentRecCollectionPath)
      .encode()
      .toUri();
    final HttpEntity<HpcCollectionRegistrationDTO> entity =
      buildHttpEntity4CollectionRegistration(authTokn, collRegDto);

    if (null == this.restTemplate) {
      this.restTemplate = HpcClientUtil.getRestTemplate(this.hpcCertPath,
          this.hpcCertPassword);
    }
    final ResponseEntity<HpcExceptionDTO> response = this.restTemplate.exchange(
      uri2Apply, HttpMethod.PUT, entity, HpcExceptionDTO.class);

    this.currFailMsg = Optional.empty();
    if (response != null) {
      HpcExceptionDTO exception = response.getBody();
      if (exception != null) {
        String message = "Failed to process record due to: " +
          exception.getMessage() + ": Error Type:" +
          exception.getErrorType().value() + ": Request reject reason: " +
          exception.getRequestRejectReason().value();
        //System.out.println(message);
        this.currFailMsg = Optional.of(message);
        addErrorToLog(message, csvRecIndex);
        addRecordToLog(this.currentCsvRecord, this.headersMap);
        retSignal = Optional.of(Constants.CLI_5);
      } else if (!(response.getStatusCode().equals(HttpStatus.CREATED)
          || response.getStatusCode().equals(HttpStatus.OK))) {
        String message = "Failed to process record due to unknown error." +
          "  Return code: " + response.getStatusCode();
        //System.out.println(message);
        this.currFailMsg = Optional.of(message);
        addErrorToLog(message, csvRecIndex);
        addRecordToLog(this.currentCsvRecord, this.headersMap);
        retSignal = Optional.of(Constants.CLI_5);
      }
    }

    return retSignal;
  }


  private void putErrorInfoInLogs(Map<String, Integer> headersMap,
    int recordIndex, CSVRecord record, Exception e) throws IOException {
    final StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    final String exceptionAsString = sw.toString();
    addErrorToLog("Failed to process record due to: " + e.getMessage(),
      recordIndex);
    addErrorToLog(exceptionAsString, recordIndex);
    addRecordToLog(record, headersMap);
  }

}
