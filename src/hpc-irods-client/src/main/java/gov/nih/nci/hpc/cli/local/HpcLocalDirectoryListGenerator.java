/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.local;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.easybatch.core.processor.RecordProcessingException;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.hpc.cli.domain.HpcMetadataAttributes;
import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcCmdException;
import gov.nih.nci.hpc.cli.util.HpcLogWriter;
import gov.nih.nci.hpc.cli.util.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

public class HpcLocalDirectoryListGenerator {

	private Properties properties = new Properties();
	private String hpcCertPath;
	private String hpcCertPassword;
	private String authToken;
	private String logFile;
	private String recordFile;
	String hpcServerURL;
	String hpcServerProxyURL;
	String hpcServerProxyPort;

	public HpcLocalDirectoryListGenerator(String configProps) throws IOException, FileNotFoundException {
		InputStream input = new FileInputStream(configProps);
		properties.load(input);
	}

	public HpcLocalDirectoryListGenerator(String hpcServerURL, String hpcServerProxyURL, String hpcServerProxyPort, String authToken, String hpcCertPath,
			String hpcCertPassword) throws IOException, FileNotFoundException {
		this.hpcCertPath = hpcCertPath;
		this.hpcCertPassword = hpcCertPassword;
		this.authToken = authToken;
		this.hpcServerURL = hpcServerURL;
		this.hpcServerProxyPort = hpcServerProxyPort;
		this.hpcServerProxyURL = hpcServerProxyURL;
	}

	public boolean run(String filePath, String excludePatternFile, String includePatternFile, String filePathBaseName,
			String destinationBasePath, String logFile, String recordFile, boolean testRun) {
		this.logFile = logFile;
		this.recordFile = recordFile;
		boolean success = true;
		Pattern pattern = null;
		HpcLocalDirectoryListQuery impl = new HpcLocalDirectoryListQuery();
		try {
			// authenticatedToken = impl.authenticate(userId, password);
//			List<Pattern> excludePatterns = readPatternsfromFile(excludePatternFile);
//			List<Pattern> includePatterns = readPatternsfromFile(includePatternFile);
			List<String> excludePatterns = readPatternStringsfromFile(excludePatternFile);
			List<String> includePatterns = readPatternStringsfromFile(includePatternFile);
			List<HpcPathAttributes> files = impl.getPathAttributes(filePath, excludePatterns, includePatterns);
			if (files != null && !testRun) {
				for (HpcPathAttributes file : files) {
					HpcDataObjectRegistrationDTO dataObject = new HpcDataObjectRegistrationDTO();
					List<HpcMetadataEntry> metadataEntries = null;
					try {
						metadataEntries = getMetadata(file);
					} catch (HpcCmdException e) {
						String message = "Failed to process file: " + file.getAbsolutePath() + " Reaon: "
								+ e.getMessage();
						System.out.println(message);
						writeException(e, message, null);
						writeRecord(file.getAbsolutePath());
						continue;
					}

					dataObject.getMetadataEntries().addAll(metadataEntries);
					dataObject.setCreateParentCollections(true);
					List<HpcMetadataEntry> parentCollectionMetadataEntries = new ArrayList<HpcMetadataEntry>();
					HpcMetadataEntry typeEntry = new HpcMetadataEntry();
					typeEntry.setAttribute("collection_type");
					typeEntry.setValue("Folder");
					parentCollectionMetadataEntries.add(typeEntry);
					dataObject.getParentCollectionMetadataEntries().addAll(parentCollectionMetadataEntries);
					HpcFileLocation fileLocation = new HpcFileLocation();
					fileLocation.setFileId(file.getAbsolutePath());
					dataObject.setSource(fileLocation);
					dataObject.setCallerObjectId(null);
					try {
						processRecord(dataObject, destinationBasePath, getObjectPath(filePathBaseName, file.getPath()));
					} catch (RecordProcessingException e) {
						String message = "Failed to process cmd due to: " + e.getMessage();
						writeException(e, message, null);
						success = false;
					}
				}
			}
		} catch (HpcCmdException e) {
			String message = "Failed to process cmd due to: " + e.getMessage();
			writeException(e, message, null);
			success = false;
		} catch (RestClientException e) {
			String message = "Failed to process cmd due to: " + e.getMessage();
			writeException(e, message, null);
			success = false;
		} catch (Exception e) {
			String message = "Failed to process cmd due to: " + e.getMessage();
			writeException(e, message, null);
			success = false;
		}
		return success;
	}

	private List<Pattern> readPatternsfromFile(String fileName) {
		if (fileName == null || fileName.isEmpty())
			return null;
		BufferedReader reader = null;
		List<Pattern> patterns = new ArrayList<Pattern>();
		try {
			reader = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = reader.readLine()) != null) {
				Pattern pattern = Pattern.compile(line);
				patterns.add(pattern);
			}
			
		} catch (IOException e) {
			throw new HpcCmdException("Failed to read include/exclude pattern file due to: " + e.getMessage());
		}
		finally
		{
			if(reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return patterns;
	}

	private List<String> readPatternStringsfromFile(String fileName) {
		if (fileName == null || fileName.isEmpty())
			return null;
		BufferedReader reader = null;
		List<String> patterns = new ArrayList<String>();
		try {
			reader = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = reader.readLine()) != null) {
				patterns.add(line);
			}
			
		} catch (IOException e) {
			throw new HpcCmdException("Failed to read include/exclude pattern file due to: " + e.getMessage());
		}
		finally
		{
			if(reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return patterns;
	}

	private List<HpcMetadataEntry> getMetadata(HpcPathAttributes file) throws HpcCmdException {
		String fullPath = file.getAbsolutePath();
		File metadataFile = new File(fullPath + ".metadata.json");
		List<HpcMetadataEntry> metadataEntries = new ArrayList<HpcMetadataEntry>();
		if (metadataFile.exists()) {
			MappingJsonFactory factory = new MappingJsonFactory();
			JsonParser parser;
			try {
				parser = factory.createParser(new FileInputStream(metadataFile));
				HpcMetadataAttributes metadataAttributes = parser.readValueAs(HpcMetadataAttributes.class);
				metadataEntries = metadataAttributes.getMetadataEntries();
				// metadataEntries = parser.readValueAs(new
				// TypeReference<List<HpcMetadataEntry>>() {
				// });
			} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
				throw new HpcCmdException(
						"Failed to read JSON metadata file: " + file.getAbsolutePath() + " Reason: " + e.getMessage());
			} catch (IOException e) {
				throw new HpcCmdException(
						"Failed to read JSON metadata file: " + file.getAbsolutePath() + " Reason: " + e.getMessage());
			}
		} else {
			HpcMetadataEntry nameEntry = new HpcMetadataEntry();
			nameEntry.setAttribute("name");
			nameEntry.setValue(file.getName());
			metadataEntries.add(nameEntry);
			HpcMetadataEntry dateEntry = new HpcMetadataEntry();
			dateEntry.setAttribute("modified_date");
			dateEntry.setValue(file.getUpdatedDate());
			metadataEntries.add(dateEntry);
		}
		return metadataEntries;
	}

	private String getObjectPath(String filePathBaseName, String filePath) {
		String name = filePathBaseName + File.separatorChar;
		if (filePath.indexOf(name) != -1)
			return filePath.substring(filePath.indexOf(name));
		else
			return filePath;
	}

	private void writeException(Exception e, String message, String exceptionAsString) {
		HpcLogWriter.getInstance().WriteLog(logFile, message);
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		if (exceptionAsString == null)
			exceptionAsString = sw.toString();
		HpcLogWriter.getInstance().WriteLog(logFile, exceptionAsString);
	}

	private void writeRecord(String filePath) {
		HpcLogWriter.getInstance().WriteLog(recordFile, filePath);
	}

	public void processRecord(HpcDataObjectRegistrationDTO hpcDataObjectRegistrationDTO, String basePath,
			String objectPath) throws RecordProcessingException {
		InputStream inputStream = null;
		HpcExceptionDTO response = null;
		String jsonInString = null;
		List<Attachment> atts = new LinkedList<Attachment>();
		atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObjectRegistration", "application/json",
				hpcDataObjectRegistrationDTO));
		try {
			inputStream = new BufferedInputStream(
					new FileInputStream(hpcDataObjectRegistrationDTO.getSource().getFileId()));
			ContentDisposition cd2 = new ContentDisposition(
					"attachment;filename=" + hpcDataObjectRegistrationDTO.getSource().getFileId());
			atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObject", inputStream, cd2));
			hpcDataObjectRegistrationDTO.setSource(null);
		} catch (FileNotFoundException e) {
			String message = "Failed to process record due to: " + e.getMessage();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = basePath + "/" + objectPath + "|" + jsonInString + "\n" + sw.toString();
			writeException(e, message, null);
			throw new RecordProcessingException(exceptionAsString);
		} catch (Exception e) {
			String message = "Failed to process record due to: " + e.getMessage();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = basePath + "/" + objectPath + "|" + jsonInString + "\n" + sw.toString();
			writeException(e, message, null);
			throw new RecordProcessingException(exceptionAsString);
		}
		objectPath = objectPath.replace("//", "/");
		objectPath = objectPath.replace("\\", "/");
		if (objectPath.charAt(0) != File.separatorChar)
			objectPath = "/" + objectPath;
		WebClient client = HpcClientUtil.getWebClient(hpcServerURL + "/dataObject/" + basePath + objectPath, hpcServerProxyURL, hpcServerProxyPort,
				hpcCertPath, hpcCertPassword);
		client.header("Authorization", "Bearer " + authToken);
		client.type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON);

		try {
			System.out.println("Processing: " + basePath + "/" + objectPath);
			Response restResponse = client.put(new MultipartBody(atts));
			if (restResponse.getStatus() != 201) {
				MappingJsonFactory factory = new MappingJsonFactory();
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
				try {
					response = parser.readValueAs(HpcExceptionDTO.class);
				} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
					ObjectMapper mapper = new ObjectMapper();
					jsonInString = mapper.writeValueAsString(hpcDataObjectRegistrationDTO);
					System.out.println("Failed to process: " + basePath + "/" + objectPath);
					HpcLogWriter.getInstance().WriteLog(logFile, basePath + "/" + objectPath + "|" + jsonInString);
					// HpcCSVFileWriter.getInstance().writeRecord(recordFile,
					// hpcObject.getCsvRecord(), hpcObject.getHeadersMap());
					if (restResponse.getStatus() == 401) {
						HpcLogWriter.getInstance().WriteLog(logFile, basePath + "/" + objectPath + "|"
								+ "Unauthorized access: response status is: " + restResponse.getStatus());
						throw new RecordProcessingException(
								"Unauthorized access: response status is: " + restResponse.getStatus());
					} else {
						HpcLogWriter.getInstance().WriteLog(logFile, basePath + "/" + objectPath + "|"
								+ "Unalbe process error response: response status is: " + restResponse.getStatus());
						throw new RecordProcessingException(
								"Unalbe process error response: response status is: " + restResponse.getStatus());
					}
				}

				if (response != null) {
					// System.out.println(response);
					StringBuffer buffer = new StringBuffer();
					if (response.getMessage() != null) {
						buffer.append("Failed to process record due to: " + response.getMessage());
						System.out.println("Failed to process record due to: " + response.getMessage());
					} else {
						buffer.append("Failed to process record due to unkown reason");
						System.out.println("Failed to process record due to unkown reason");
					}
					if (response.getErrorType() != null)
						buffer.append(" Error Type:" + response.getErrorType().value());

					if (response.getRequestRejectReason() != null)
						buffer.append(" Request reject reason:" + response.getRequestRejectReason().value());
					HpcLogWriter.getInstance().WriteLog(logFile,
							basePath + "/" + objectPath + "|" + jsonInString + " \n " + buffer.toString());
					throw new RecordProcessingException(buffer.toString());
				} else {
					HpcLogWriter.getInstance().WriteLog(logFile, basePath + "/" + objectPath + "|" + jsonInString);
					throw new RecordProcessingException(
							"Failed to process record due to unknown error. Return code: " + restResponse.getStatus());
				}
			} else
				System.out.println("Success! ");
		} catch (HpcBatchException e) {
			String message = "Failed to process record due to: " + e.getMessage();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = basePath + "/" + objectPath + "|" + jsonInString + "\n" + sw.toString();
			writeException(e, message, null);
			throw new RecordProcessingException(exceptionAsString);
		} catch (RestClientException e) {
			String message = "Failed to process record due to: " + e.getMessage();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = basePath + "/" + objectPath + "|" + jsonInString + "\n" + sw.toString();
			writeException(e, message, null);
			throw new RecordProcessingException(exceptionAsString);
		} catch (Exception e) {
			String message = "Failed to process record due to: " + e.getMessage();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = basePath + "/" + objectPath + "|" + jsonInString + "\n" + sw.toString();
			writeException(e, message, null);
			throw new RecordProcessingException(exceptionAsString);
		} finally {
			if (inputStream != null)
				try {
					inputStream.close();
				} catch (IOException e) {
					// e.printStackTrace();
				}
		}
	}
}
