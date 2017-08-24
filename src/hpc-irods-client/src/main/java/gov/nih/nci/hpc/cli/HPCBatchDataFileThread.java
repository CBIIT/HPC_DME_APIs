/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcCSVFileWriter;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcLogWriter;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

public class HPCBatchDataFileThread implements Runnable {
	private Thread t;
	private String threadName;
	Map<String, Integer> headersMap;
	CSVRecord record;
	String basePath;
	int recordId = 0;
	String fileLogWriter;
	String csvFilePrinter;
	String hpcCertPath;
	String hpcCertPassword;
	String userId;
	String password;
	String proxyURL;
	String proxyPort;

	HPCBatchDataFileThread(String name, Map<String, Integer> headersMap, int recordId, CSVRecord record,
			String basePath, String proxyURL, String proxyPort, String fileLogWriter, String csvFilePrinter, String hpcCertPath, String hpcCertPassword,
			String userId, String password) {
		threadName = name;
		this.headersMap = headersMap;
		this.record = record;
		this.basePath = basePath;
		this.recordId = recordId;
		this.fileLogWriter = fileLogWriter;
		this.csvFilePrinter = csvFilePrinter;
		this.hpcCertPath = hpcCertPath;
		this.hpcCertPassword = hpcCertPassword;
		this.userId = userId;
		this.password = password;
		this.proxyPort = proxyPort;
		this.proxyURL = proxyURL;

		System.out.println("Creating " + threadName);
	}

	public void run() {
		System.out.println("Running " + threadName);
		String collName = null;
		HpcExceptionDTO response = null;
		boolean processedRecordFlag = true;
		HpcFileLocation source = new HpcFileLocation();
		InputStream inputStream = null;
		List<HpcMetadataEntry> listOfhpcCollection = new ArrayList<HpcMetadataEntry>();
		for (Entry<String, Integer> entry : headersMap.entrySet()) {
			String cellVal = record.get(entry.getKey());
			HpcMetadataEntry hpcMetadataEntry = new HpcMetadataEntry();
			hpcMetadataEntry.setAttribute(entry.getKey());
			hpcMetadataEntry.setValue(cellVal);
			if (entry.getKey().equals("fileContainerId")) {
				source.setFileContainerId(cellVal);
				continue;
			} else if (entry.getKey().equals("fileId")) {
				source.setFileId(cellVal);
				continue;
			} else if (entry.getKey().equals("object_path")) {
				collName = cellVal;
				continue;
			}

			if (StringUtils.isNotBlank(cellVal))
				listOfhpcCollection.add(hpcMetadataEntry);
		}

		HpcDataObjectRegistrationDTO hpcDataObjectRegistrationDTO = new HpcDataObjectRegistrationDTO();
		hpcDataObjectRegistrationDTO.getMetadataEntries().addAll(listOfhpcCollection);

		System.out.println("Adding file from " + source.getFileId());
		if (!collName.startsWith("/"))
			collName = "/" + collName;

		hpcDataObjectRegistrationDTO.setSource(source);
		hpcDataObjectRegistrationDTO.setCallerObjectId("/");
		WebClient client = HpcClientUtil.getWebClient(basePath + collName, proxyURL, proxyPort, hpcCertPath, hpcCertPassword);
		List<Attachment> atts = new LinkedList<Attachment>();
		if (hpcDataObjectRegistrationDTO.getSource().getFileContainerId() == null) {
			if (hpcDataObjectRegistrationDTO.getSource().getFileId() == null) {
				addErrorToLog("Invalid or missing file source location ", recordId);
				// success = false;
				processedRecordFlag = false;
				addRecordToLog(record, headersMap);
				return;
			} else {

				try {
					inputStream = new BufferedInputStream(
							new FileInputStream(hpcDataObjectRegistrationDTO.getSource().getFileId()));
					ContentDisposition cd2 = new ContentDisposition(
							"attachment;filename=" + hpcDataObjectRegistrationDTO.getSource().getFileId());
					atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObject", inputStream, cd2));
					hpcDataObjectRegistrationDTO.setSource(null);
				} catch (FileNotFoundException e) {
					addErrorToLog("Invalid or missing file source location ", recordId);
					// success = false;
					processedRecordFlag = false;
					addRecordToLog(record, headersMap);
					return;
				} catch (IOException e) {
					addErrorToLog("Invalid or missing file source location ", recordId);
					// success = false;
					processedRecordFlag = false;
					addRecordToLog(record, headersMap);
					return;
				}
			}
		}
		atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObjectRegistration", "application/json",
				hpcDataObjectRegistrationDTO));

		String token = DatatypeConverter.printBase64Binary((userId + ":" + password).getBytes());
		client.header("Authorization", "Basic " + token);
		client.type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON);

		try {
			System.out.println(basePath + collName);
			Response restResponse = client.put(new MultipartBody(atts));
			System.out.println("Status: " + restResponse.getStatus());
			if (!(restResponse.getStatus() == 201 || restResponse.getStatus() == 200)) {
				MappingJsonFactory factory = new MappingJsonFactory();
				JsonParser parser = factory.createJsonParser((InputStream) restResponse.getEntity());
				try {
					response = parser.readValueAs(HpcExceptionDTO.class);
				} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
					if (restResponse.getStatus() == 401)
						addErrorToLog("Unauthorized access: response status is: " + restResponse.getStatus(), recordId);
					else
						addErrorToLog("Unalbe process error response: response status is: " + restResponse.getStatus(),
								recordId);

				}

				if (response != null) {
					// System.out.println(response);
					StringBuffer buffer = new StringBuffer();
					if (response.getMessage() != null)
						buffer.append("Failed to process record due to: " + response.getMessage());
					else
						buffer.append("Failed to process record due to unkown reason");
					if (response.getErrorType() != null)
						buffer.append(" Error Type:" + response.getErrorType().value());

					if (response.getRequestRejectReason() != null)
						buffer.append(" Request reject reason:" + response.getRequestRejectReason().value());

					addErrorToLog(buffer.toString(), recordId);
					// success = false;
					processedRecordFlag = false;
					addRecordToLog(record, headersMap);
				} else {
					addErrorToLog(
							"Failed to process record due to unknown error. Return code: " + restResponse.getStatus(),
							recordId);
					// success = false;
					processedRecordFlag = false;
					addRecordToLog(record, headersMap);
				}
			}
		} catch (HpcBatchException e) {
			// success = false;
			processedRecordFlag = false;
			String message = "Failed to process record due to: " + e.getMessage();
			// System.out.println(message);
			addErrorToLog(message, recordId);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			addErrorToLog(exceptionAsString, recordId);
			addRecordToLog(record, headersMap);
		} catch (RestClientException e) {
			// success = false;
			processedRecordFlag = false;
			String message = "Failed to process record due to: " + e.getMessage();
			// System.out.println(message);
			addErrorToLog(message, recordId);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			addErrorToLog(exceptionAsString, recordId);
			addRecordToLog(record, headersMap);
		} catch (Exception e) {
			// success = false;
			processedRecordFlag = false;
			String message = "Failed to process record due to: " + e.getMessage();
			// System.out.println(message);
			addErrorToLog(message, recordId);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			addErrorToLog(exceptionAsString, recordId);
			addRecordToLog(record, headersMap);
		}
		if (processedRecordFlag)
			System.out.println("Record line: " + recordId + " is successfully processed!");
		else
			System.out.println("Record line: " + recordId + " is failed processing. Please check the log.");
	}

	public void start() {
		System.out.println("Starting " + threadName);
		if (t == null) {
			t = new Thread(this, threadName);
			t.start();
		}
	}

	protected void addErrorToLog(String error, int recordLineNumber) {
		HpcLogWriter.getInstance().WriteLog(fileLogWriter, recordLineNumber + " : " + error);
	}

	protected void addRecordToLog(CSVRecord record, Map<String, Integer> headers) {
		HpcCSVFileWriter.getInstance().writeRecord(csvFilePrinter, record, headers);
	}

}
