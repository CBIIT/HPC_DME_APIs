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
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.easybatch.core.mapper.RecordMappingException;
import org.easybatch.core.processor.RecordProcessingException;
import org.easybatch.core.processor.RecordProcessor;
import org.easybatch.core.record.Record;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcCSVFileWriter;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcLogWriter;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

public class HPCBatchDataFileRecordProcessor implements RecordProcessor {

	@Override
	public Record processRecord(Record record) throws RecordProcessingException {
		// TODO Auto-generated method stub
		InputStream inputStream = null;
		HpcExceptionDTO response = null;
		HPCDataObject hpcObject = (HPCDataObject) record.getPayload();
		HpcDataObjectRegistrationDTO hpcDataObjectRegistrationDTO = hpcObject.getDto();
		List<Attachment> atts = new LinkedList<Attachment>();
		if (hpcDataObjectRegistrationDTO.getSource().getFileContainerId() == null) {
			if (hpcDataObjectRegistrationDTO.getSource().getFileId() == null) {
				HpcCSVFileWriter.getInstance().writeRecord(hpcObject.getErrorRecordsFile(), hpcObject.getCsvRecord(),
						hpcObject.getHeadersMap());
				HpcLogWriter.getInstance().WriteLog(hpcObject.getLogFile(),
						"Record: " + record.getHeader().getNumber() + " with path: " + hpcObject.getObjectPath()
								+ "\n Invalid or missing fileContainerId or/and fileId.");
				throw new RecordMappingException("Invalid or missing file source location");
			} else {

				try {
					inputStream = new BufferedInputStream(
							new FileInputStream(hpcDataObjectRegistrationDTO.getSource().getFileId()));
					ContentDisposition cd2 = new ContentDisposition(
							"attachment;filename=" + hpcDataObjectRegistrationDTO.getSource().getFileId());
					atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObject", inputStream, cd2));
					hpcDataObjectRegistrationDTO.setSource(null);
				} catch (FileNotFoundException e) {
					HpcCSVFileWriter.getInstance().writeRecord(hpcObject.getErrorRecordsFile(),
							hpcObject.getCsvRecord(), hpcObject.getHeadersMap());
					HpcLogWriter.getInstance().WriteLog(hpcObject.getLogFile(),
							"Record: " + record.getHeader().getNumber() + " with path: " + hpcObject.getObjectPath()
									+ "\n Invalid or missing file source location. Message: " + e.getMessage());
					throw new RecordMappingException(
							"Invalid or missing file source location. Message: " + e.getMessage());
				} catch (IOException e) {
					HpcCSVFileWriter.getInstance().writeRecord(hpcObject.getErrorRecordsFile(),
							hpcObject.getCsvRecord(), hpcObject.getHeadersMap());
					HpcLogWriter.getInstance().WriteLog(hpcObject.getLogFile(),
							"Record: " + record.getHeader().getNumber() + " with path: " + hpcObject.getObjectPath()
									+ "\n Invalid or missing file source location. Message: " + e.getMessage());
					throw new RecordMappingException(
							"Invalid or missing file source location. Message: " + e.getMessage());
				}
			}
		} else if (hpcDataObjectRegistrationDTO.getSource().getFileId() == null) {
			HpcCSVFileWriter.getInstance().writeRecord(hpcObject.getErrorRecordsFile(), hpcObject.getCsvRecord(),
					hpcObject.getHeadersMap());
			HpcLogWriter.getInstance().WriteLog(hpcObject.getLogFile(),
					"Record: " + record.getHeader().getNumber() + " with path: " + hpcObject.getObjectPath()
							+ "\n Invalid or missing fileContainerId or/and fileId");
			throw new RecordMappingException("Invalid or missing file source location");
		}

		atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObjectRegistration", "application/json",
				hpcDataObjectRegistrationDTO));
		long start = System.currentTimeMillis();
		WebClient client = HpcClientUtil.getWebClient(hpcObject.getBasePath() + "/" + hpcObject.getObjectPath(), hpcObject.getProxyURL(), hpcObject.getProxyPort(),
				hpcObject.getHpcCertPath(), hpcObject.getHpcCertPassword());
		// String token =
		// DatatypeConverter.printBase64Binary((hpcObject.getUserId() + ":" +
		// hpcObject.getPassword()).getBytes());
		client.header("Authorization", "Bearer " + hpcObject.getAuthToken());
		client.type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON);
		// client.type(MediaType.MULTIPART_FORM_DATA);

		try {
			System.out.println("Processing: " + hpcObject.getBasePath() + "/" + hpcObject.getObjectPath());
			Response restResponse = client.put(new MultipartBody(atts));
			long stop = System.currentTimeMillis();
			// System.out.println("Processing: "+hpcObject.getBasePath() + "/"+
			// hpcObject.getObjectPath() + " time: "+(stop-start)/1000 + "
			// sec");
			// System.out.println("Processing done: "+hpcObject.getBasePath() +
			// hpcObject.getObjectPath() + " Status: " +
			// restResponse.getStatus());
			if (!(restResponse.getStatus() == 201 || restResponse.getStatus() == 200)) {
				System.out.println("Failed!");
				MappingJsonFactory factory = new MappingJsonFactory();
				JsonParser parser = factory.createJsonParser((InputStream) restResponse.getEntity());
				try {
					response = parser.readValueAs(HpcExceptionDTO.class);
				} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
					HpcCSVFileWriter.getInstance().writeRecord(hpcObject.getErrorRecordsFile(),
							hpcObject.getCsvRecord(), hpcObject.getHeadersMap());
					if (restResponse.getStatus() == 401) {

						HpcLogWriter.getInstance()
								.WriteLog(hpcObject.getLogFile(), "Record: " + record.getHeader().getNumber()
										+ " with path: " + hpcObject.getObjectPath()
										+ "\n Unauthorized access: response status is: " + restResponse.getStatus());
						throw new RecordProcessingException(
								"Unauthorized access: response status is: " + restResponse.getStatus());
					} else {
						HpcLogWriter.getInstance().WriteLog(hpcObject.getLogFile(),
								"Record: " + record.getHeader().getNumber() + " with path: " + hpcObject.getObjectPath()
										+ "\n Unalbe process error response: response status is: "
										+ restResponse.getStatus());
						throw new RecordProcessingException(
								"Unalbe process error response: response status is: " + restResponse.getStatus());
					}
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
					HpcCSVFileWriter.getInstance().writeRecord(hpcObject.getErrorRecordsFile(),
							hpcObject.getCsvRecord(), hpcObject.getHeadersMap());
					HpcLogWriter.getInstance().WriteLog(hpcObject.getLogFile(),
							"Record: " + record.getHeader().getNumber() + " with path: " + hpcObject.getObjectPath()
									+ " \n " + buffer.toString());
					throw new RecordProcessingException(buffer.toString());
				} else {
					HpcCSVFileWriter.getInstance().writeRecord(hpcObject.getErrorRecordsFile(),
							hpcObject.getCsvRecord(), hpcObject.getHeadersMap());
					HpcLogWriter.getInstance().WriteLog(hpcObject.getLogFile(),
							"Record: " + record.getHeader().getNumber() + "with path: " + hpcObject.getObjectPath()
									+ "\n Failed to process record due to unknown error. Return code: "
									+ restResponse.getStatus());
					throw new RecordProcessingException(
							"Failed to process record due to unknown error. Return code: " + restResponse.getStatus());
				}
			} else
				System.out.println("Success!");
			System.out.println("---------------------------------");
		} catch (HpcBatchException e) {
			HpcCSVFileWriter.getInstance().writeRecord(hpcObject.getErrorRecordsFile(), hpcObject.getCsvRecord(),
					hpcObject.getHeadersMap());
			String message = "Failed to process record due to: " + e.getMessage();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			HpcLogWriter.getInstance().WriteLog(hpcObject.getLogFile(), "Record: " + record.getHeader().getNumber()
					+ " with path: " + hpcObject.getObjectPath() + "\n" + exceptionAsString);
			throw new RecordProcessingException(exceptionAsString);
		} catch (RestClientException e) {
			HpcCSVFileWriter.getInstance().writeRecord(hpcObject.getErrorRecordsFile(), hpcObject.getCsvRecord(),
					hpcObject.getHeadersMap());
			String message = "Failed to process record due to: " + e.getMessage();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			HpcLogWriter.getInstance().WriteLog(hpcObject.getLogFile(), "Record: " + record.getHeader().getNumber()
					+ " with path: " + hpcObject.getObjectPath() + "\n" + exceptionAsString);
			throw new RecordProcessingException(exceptionAsString);
		} catch (Exception e) {
			HpcCSVFileWriter.getInstance().writeRecord(hpcObject.getErrorRecordsFile(), hpcObject.getCsvRecord(),
					hpcObject.getHeadersMap());
			String message = "Failed to process record due to: " + e.getMessage();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = sw.toString();
			HpcLogWriter.getInstance().WriteLog(hpcObject.getLogFile(), "Record: " + record.getHeader().getNumber()
					+ " with path: " + hpcObject.getObjectPath() + "\n" + exceptionAsString);
			throw new RecordProcessingException(exceptionAsString);
		} finally {
			if (inputStream != null)
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return null;
	}

}
