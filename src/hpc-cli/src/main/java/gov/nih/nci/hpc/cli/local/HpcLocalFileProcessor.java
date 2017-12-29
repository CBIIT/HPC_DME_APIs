package gov.nih.nci.hpc.cli.local;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.easybatch.core.processor.RecordProcessingException;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import gov.nih.nci.hpc.cli.domain.HpcServerConnection;
import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcCmdException;
import gov.nih.nci.hpc.cli.util.HpcLogWriter;
import gov.nih.nci.hpc.cli.util.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationResponseDTO;



public class HpcLocalFileProcessor extends HpcLocalEntityProcessor {

	String logFile;
	String recordFile;

	public HpcLocalFileProcessor(HpcServerConnection connection) throws IOException, FileNotFoundException {
		super(connection);
	}

	@Override
	public boolean process(HpcPathAttributes entity, String filePath, String filePathBaseName, String destinationBasePath,
			String logFile, String recordFile, boolean metadataOnly, boolean directUpload, boolean checksum)
			throws RecordProcessingException {
		HpcDataObjectRegistrationRequestDTO dataObject = new HpcDataObjectRegistrationRequestDTO();
		this.logFile = logFile;
		this.recordFile = recordFile;

		List<HpcMetadataEntry> metadataEntries = null;
		try {
			metadataEntries = getMetadata(entity, metadataOnly);
			if (metadataEntries == null) {
				System.out.println("No metadata file found. Skipping file: " + entity.getAbsolutePath());
				return true;
			}
		} catch (HpcCmdException e) {
			String message = "Failed to process file: " + entity.getAbsolutePath() + " Reaon: " + e.getMessage();
			System.out.println(message);
			HpcClientUtil.writeException(e, message, null, logFile);
			HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
			return false;
		}

		dataObject.getMetadataEntries().addAll(metadataEntries);
		if (!metadataOnly) {
			dataObject.setCreateParentCollections(true);
			List<HpcMetadataEntry> parentCollectionMetadataEntries = new ArrayList<HpcMetadataEntry>();
			List<HpcMetadataEntry> parentMetadataEntries = getParentCollectionMetadata(entity, metadataOnly);
			if (parentMetadataEntries != null)
				parentCollectionMetadataEntries.addAll(parentMetadataEntries);
			dataObject.getParentCollectionMetadataEntries().addAll(parentCollectionMetadataEntries);
		}
		HpcFileLocation fileLocation = new HpcFileLocation();
		fileLocation.setFileId(entity.getAbsolutePath());
		dataObject.setSource(fileLocation);
		dataObject.setCallerObjectId(null);
		String objectPath = getObjectPath(filePath, filePathBaseName, entity.getPath());
		if(HpcClientUtil.containsWhiteSpace(objectPath))
		{
			System.out.println("White space in the file path "+ objectPath + " is replaced with underscore _ ");
			objectPath = HpcClientUtil.replaceWhiteSpaceWithUnderscore(objectPath);
		}
		File file = new File(entity.getAbsolutePath());
		
		if(!file.exists())
		{
			String message = "File does not exist. Skipping: " + entity.getAbsolutePath();
			HpcClientUtil.writeException(new HpcBatchException(message), message, null, logFile);
			HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
			throw new RecordProcessingException(message);
		}
		if (directUpload && !metadataOnly)
			processS3Record(entity, dataObject, filePath, destinationBasePath, objectPath, metadataOnly, checksum);
		else
			processRecord(entity, dataObject, filePath, destinationBasePath, objectPath, metadataOnly, checksum);
		return true;
	}

	private void processRecord(HpcPathAttributes entity,
			HpcDataObjectRegistrationRequestDTO hpcDataObjectRegistrationDTO, String filePath, String basePath, String objectPath,
			boolean metadataOnly, boolean checksum) throws RecordProcessingException {
		InputStream inputStream = null;
		InputStream checksumStream = null;
		HpcExceptionDTO response = null;
		String jsonInString = null;
		List<Attachment> atts = new LinkedList<Attachment>();
		try {
			if (!metadataOnly) {
				inputStream = new BufferedInputStream(
						new FileInputStream(hpcDataObjectRegistrationDTO.getSource().getFileId()));
				checksumStream = new FileInputStream(hpcDataObjectRegistrationDTO.getSource().getFileId());
				ContentDisposition cd2 = new ContentDisposition(
						"attachment;filename=" + hpcDataObjectRegistrationDTO.getSource().getFileId());
				atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObject", inputStream, cd2));
				if(checksum)
				{
					HashCode hash = com.google.common.io.Files
							.hash(new File(hpcDataObjectRegistrationDTO.getSource().getFileId()), Hashing.md5());
					String md5 = hash.toString();
					hpcDataObjectRegistrationDTO.setChecksum(md5);
				}
			}
			hpcDataObjectRegistrationDTO.setSource(null);
		} catch (FileNotFoundException e) {
			String message = "Failed to process record due to: " + e.getMessage();
			HpcClientUtil.writeException(e, message, null, logFile);
			HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
			throw new RecordProcessingException(message);
		} catch (Exception e) {
			String message = "Failed to process record due to: " + e.getMessage();
			HpcClientUtil.writeException(e, message, null, logFile);
			HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
			throw new RecordProcessingException(message);
		}

		atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObjectRegistration", "application/json",
				hpcDataObjectRegistrationDTO));
		objectPath = objectPath.replace("//", "/");
		objectPath = objectPath.replace("\\", "/");
		if (objectPath.charAt(0) != File.separatorChar)
			objectPath = "/" + objectPath;
		System.out.println("Processing: " + basePath + objectPath);
		System.out.println("checksum: " + hpcDataObjectRegistrationDTO.getChecksum());

		WebClient client = HpcClientUtil.getWebClient(
				connection.getHpcServerURL() + "/dataObject/" + basePath + objectPath,
				connection.getHpcServerProxyURL(), connection.getHpcServerProxyPort(), connection.getHpcCertPath(),
				connection.getHpcCertPassword());
		client.header("Authorization", "Bearer " + connection.getAuthToken());
		client.header("Connection", "Keep-Alive");
		client.type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON);

		try {

			Response restResponse = client.put(new MultipartBody(atts));
			if (!(restResponse.getStatus() == 201 || restResponse.getStatus() == 200)) {
				processException(restResponse, hpcDataObjectRegistrationDTO, basePath, objectPath);
			} else {
				System.out.println("Success! ");
			}
		} catch (HpcBatchException e) {
			String message = "Failed to process record due to: " + e.getMessage();
			HpcClientUtil.writeException(e, message, null, logFile);
			HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
			throw new RecordProcessingException(message);
		} catch (RestClientException e) {
			String message = "Failed to process record due to: " + e.getMessage();
			HpcClientUtil.writeException(e, message, null, logFile);
			HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
			throw new RecordProcessingException(message);
		} catch (Exception e) {
			String message = "Failed to process record due to: " + e.getMessage();
			HpcClientUtil.writeException(e, message, null, logFile);
			HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
			throw new RecordProcessingException(message);
		} finally {
			if (inputStream != null)
				try {
					inputStream.close();
				} catch (IOException e) {
					System.out.println("HpcLocalFileProcessor - Failed to close input stream: " + e.getMessage());
				}
			if (checksumStream != null)
				try {
					checksumStream.close();
				} catch (IOException e) {
					System.out.println("HpcLocalFileProcessor - Failed to close input stream: " + e.getMessage());
				}
		}
	}

	private void processS3Record(HpcPathAttributes entity,
			HpcDataObjectRegistrationRequestDTO hpcDataObjectRegistrationDTO, String filePath, String basePath, String objectPath,
			boolean metadataOnly, boolean checksum) throws RecordProcessingException {
		InputStream inputStream = null;
		InputStream checksumStream = null;

		String jsonInString = null;
		List<Attachment> atts = new LinkedList<Attachment>();
		hpcDataObjectRegistrationDTO.setGenerateUploadRequestURL(true);
		hpcDataObjectRegistrationDTO.setSource(null);
		atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObjectRegistration", "application/json",
				hpcDataObjectRegistrationDTO));
		objectPath = objectPath.replace("//", "/");
		objectPath = objectPath.replace("\\", "/");
		if (objectPath.charAt(0) != File.separatorChar)
			objectPath = "/" + objectPath;
		// System.out.println("Processing: " + basePath + objectPath);
		String md5Checksum = null;
		if(checksum)
		{
			HashCode hash;
			try {
				hash = com.google.common.io.Files
						.hash(new File(entity.getAbsolutePath()), Hashing.md5());
				md5Checksum = hash.toString();
				hpcDataObjectRegistrationDTO.setChecksum(md5Checksum);
			} catch (IOException e) {
				String message = "Failed to calculate checksum due to: " + e.getMessage();
				HpcClientUtil.writeException(e, message, null, logFile);
				HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
				throw new RecordProcessingException(message);
			}
		}

		WebClient client = HpcClientUtil.getWebClient(
				connection.getHpcServerURL() + "/dataObject/" + basePath + objectPath,
				connection.getHpcServerProxyURL(), connection.getHpcServerProxyPort(), connection.getHpcCertPath(),
				connection.getHpcCertPassword());
		client.header("Authorization", "Bearer " + connection.getAuthToken());
		client.header("Connection", "Keep-Alive");
		client.type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON);

		try {

			Response restResponse = client.put(new MultipartBody(atts));
			if (!(restResponse.getStatus() == 201 || restResponse.getStatus() == 200)) {
				// System.out.println(restResponse.getStatus());
				processException(restResponse, hpcDataObjectRegistrationDTO, basePath, objectPath);
			} else {
				HpcDataObjectRegistrationResponseDTO responseDTO = (HpcDataObjectRegistrationResponseDTO) HpcClientUtil
						.getObject(restResponse, HpcDataObjectRegistrationResponseDTO.class);
				if (responseDTO != null && responseDTO.getUploadRequestURL() != null) {
					// System.out.println("Successfully registered object.
					// Uploading the object to the archive..");
					uploadToUrl(responseDTO.getUploadRequestURL(), new File(entity.getAbsolutePath()),
							connection.getBufferSize(), md5Checksum);
				} else {
					System.out.println("Failed to get signed URL for: " + basePath + objectPath);
				}
				// System.out.println("Success! "+ basePath + objectPath);
			}
		} catch (HpcBatchException e) {
			String message = "Failed to process record due to: " + e.getMessage();
			HpcClientUtil.writeException(e, message, null, logFile);
			HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
			throw new RecordProcessingException(message);
		} catch (RestClientException e) {
			String message = "Failed to process record due to: " + e.getMessage();
			HpcClientUtil.writeException(e, message, null, logFile);
			HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
			throw new RecordProcessingException(message);
		} catch (Exception e) {
			String message = "Failed to process record due to: " + e.getMessage();
			HpcClientUtil.writeException(e, message, null, logFile);
			HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
			throw new RecordProcessingException(message);
		} finally {
			if (inputStream != null)
				try {
					inputStream.close();
				} catch (IOException e) {
					System.out.println("HpcLocalFileProcessor - Failed to close input stream: " + e.getMessage());
				}
			if (checksumStream != null)
				try {
					checksumStream.close();
				} catch (IOException e) {
					System.out.println("HpcLocalFileProcessor - Failed to close input stream: " + e.getMessage());
				}
		}
	}

	private void processException(Response restResponse,
			HpcDataObjectRegistrationRequestDTO hpcDataObjectRegistrationDTO, String basePath, String objectPath)
			throws RecordProcessingException {
		MappingJsonFactory factory = new MappingJsonFactory();
		HpcExceptionDTO response = null;
		String jsonInString = null;
		JsonParser parser = null;
		try {
			parser = factory.createParser((InputStream) restResponse.getEntity());
			response = parser.readValueAs(HpcExceptionDTO.class);
			if (response != null) {
				System.out.println("Failed to register: " + objectPath + " Reason: " + response.getMessage());
				throw new RecordProcessingException(
						"Failed to register: " + objectPath + " Reason: " + response.getMessage());
			}
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				jsonInString = mapper.writeValueAsString(hpcDataObjectRegistrationDTO);
				System.out.println("Failed to process: " + basePath + "/" + objectPath);
				HpcLogWriter.getInstance().WriteLog(logFile, basePath + "/" + objectPath + "|" + jsonInString);
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
			} catch (JsonProcessingException e1) {
				HpcLogWriter.getInstance().WriteLog(logFile, basePath + "/" + objectPath + "|"
						+ "Unalbe process error response: response status is: " + restResponse.getStatus());
				throw new RecordProcessingException(
						"Unalbe process error response: response status is: " + restResponse.getStatus());
			}
		} catch (JsonProcessingException e) {
			HpcLogWriter.getInstance().WriteLog(logFile, basePath + "/" + objectPath + "|"
					+ "Unalbe process error response: response status is: " + restResponse.getStatus());
			throw new RecordProcessingException(
					"Unalbe process error response: response status is: " + restResponse.getStatus());
		} catch (IOException e) {
			HpcLogWriter.getInstance().WriteLog(logFile, basePath + "/" + objectPath + "|"
					+ "Unalbe process error response: response status is: " + restResponse.getStatus());
			throw new RecordProcessingException(
					"Unalbe process error response: response status is: " + restResponse.getStatus());
		}

		if (response != null) {
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
		}
	}

	private String getObjectPath(String filePath, String filePathBaseName, String objectPath) {

	  objectPath = objectPath.replace('\\', '/');
	  filePath = filePath.replace('\\', '/');
	  if(objectPath.equals(filePath))
	    return objectPath;
	  if(filePathBaseName != null && !filePathBaseName.isEmpty())
	  {
	  String name = filePathBaseName + "/";
		if (filePath.indexOf(name) != -1)
			return filePath.substring(filePath.indexOf(name));
	  }
	  else
	  {
        if (objectPath.indexOf(filePath) != -1)
          return objectPath.substring(objectPath.indexOf(filePath)+filePath.length()+1);
	  }
	    return objectPath;
	}

	public void uploadToUrl(String urlStr, File file, int bufferSize, String checksum) throws HpcBatchException {

		HttpURLConnection connection;
		try {
			URL url = new URL(urlStr);
			InputStream inputStream = new FileInputStream(file);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("PUT");
			connection.setChunkedStreamingMode(bufferSize);
			if(checksum != null)
				connection.addRequestProperty("md5chksum", checksum);
			OutputStream out = connection.getOutputStream();
			

			byte[] buf = new byte[1024];
			int count;
			int total = 0;
			long fileSize = file.length();

			while ((count = inputStream.read(buf)) != -1) {
				if (Thread.interrupted()) {
					throw new InterruptedException();
				}
				out.write(buf, 0, count);
				// total += count;
				// int pctComplete = new Double(new Double(total) / new
				// Double(fileSize) * 100).intValue();
				// System.out.print("\r");
				// System.out.print(String.format("PCT Complete: %d of " +
				// fileSize + " bytes", pctComplete));
			}
			out.close();
			inputStream.close();

			int responseCode = connection.getResponseCode();

			if (responseCode == 200) {
				System.out.println("Successfully registered " + file.getAbsolutePath());
			}
			else
			{
				BufferedReader br = new BufferedReader(new InputStreamReader((connection.getErrorStream())));
				StringBuilder sb = new StringBuilder();
				String output;
				while ((output = br.readLine()) != null) 
					sb.append(output);
				System.out.println("Failed to register - " + file.getAbsolutePath() + ". Check error log for details.");		
				throw new HpcBatchException("Failed to register - " + file.getAbsolutePath() + " Response: "+sb.toString());
			}
		} catch (IOException e) {
			throw new HpcBatchException(e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new HpcBatchException(e.getMessage(), e);
		}
	}
}
