/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.local;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import gov.nih.nci.hpc.cli.domain.HpcMetadataAttributes;
import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcCmdException;
import gov.nih.nci.hpc.cli.util.HpcLogWriter;
import gov.nih.nci.hpc.cli.util.HpcPathAttributes;
import gov.nih.nci.hpc.cli.util.Paths;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcBulkMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
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
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.easybatch.core.processor.RecordProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

public class HpcLocalDirectoryListGenerator {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());
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

	public HpcLocalDirectoryListGenerator(String hpcServerURL, String hpcServerProxyURL, String hpcServerProxyPort,
			String authToken, String hpcCertPath, String hpcCertPassword)
			throws IOException, FileNotFoundException {
		this.hpcCertPath = hpcCertPath;
		this.hpcCertPassword = hpcCertPassword;
		this.authToken = authToken;
		this.hpcServerURL = hpcServerURL;
		this.hpcServerProxyPort = hpcServerProxyPort;
		this.hpcServerProxyURL = hpcServerProxyURL;
	}

	public boolean run(String filePath, String excludePatternFile, String includePatternFile, String filePathBaseName,
			String destinationBasePath, String logFile, String recordFile, boolean testRun, boolean confirmation,
			boolean metadataOnly) {
	    logger.debug("run: filePath "+filePath);
	    logger.debug("run: filePathBaseName "+filePathBaseName);
	    logger.debug("run: destinationBasePath "+destinationBasePath);
	    logger.debug("run: logFile "+logFile);
	    logger.debug("run: recordFile "+recordFile);
	    logger.debug("run: testRun "+testRun);
	    logger.debug("run: confirmation "+confirmation);
	    logger.debug("run: metadataOnly "+metadataOnly);
		this.logFile = logFile;
		this.recordFile = recordFile;
		boolean success = true;
		HpcLocalDirectoryListQuery impl = new HpcLocalDirectoryListQuery();
		try {
		    logger.debug("excludePatternFile "+excludePatternFile);
		    logger.debug("includePatternFile "+includePatternFile);
		    List<String> excludePatterns = readPatternStringsfromFile(excludePatternFile);
			List<String> includePatterns = readPatternStringsfromFile(includePatternFile);
			logger.debug("excludePatterns "+excludePatterns);
			logger.debug("includePatterns "+includePatterns);
			List<HpcPathAttributes> files = impl.getPathAttributes(filePath, excludePatterns, includePatterns);
			logger.debug("files "+files);
			if (files != null && !testRun) {
				Collections.sort(files);
				for (HpcPathAttributes file : files) {
					try {
						File fileAbsolutePath = new File(file.getAbsolutePath());
//						File fileAbsolutePath = new File(Paths.generateFileSystemResourceUri(file.getAbsolutePath()));
						if (!fileAbsolutePath.isDirectory()) {
							HpcDataObjectRegistrationRequestDTO dataObject = new HpcDataObjectRegistrationRequestDTO();						
							HpcBulkMetadataEntries bulkMetadataEntries = new HpcBulkMetadataEntries();
						    dataObject.setParentCollectionsBulkMetadataEntries(bulkMetadataEntries);
							List<HpcMetadataEntry> metadataEntries = null;
							try {
								metadataEntries = getMetadata(file, metadataOnly);
								if (metadataEntries == null) {
									System.out.println(
											"No metadata file found. Skipping file: " + file.getAbsolutePath());
									continue;
								}
							} catch (HpcCmdException e) {
							    logger.error(e.getMessage(), e);
								String message = "Failed to process file: " + file.getAbsolutePath() + " Reaon: "
										+ e.getMessage();
								System.out.println(message);
								writeException(e, message, null);
								writeRecord(file.getAbsolutePath());
								continue;
							}

							dataObject.getMetadataEntries().addAll(metadataEntries);
							if (!metadataOnly) {
								dataObject.setCreateParentCollections(true);
								List<HpcMetadataEntry> parentCollectionMetadataEntries = new ArrayList<HpcMetadataEntry>();
								List<HpcMetadataEntry> parentMetadataEntries = getParentCollectionMetadata(file,
										metadataOnly);
								if (parentMetadataEntries != null)
									parentCollectionMetadataEntries.addAll(parentMetadataEntries);
								dataObject.getParentCollectionsBulkMetadataEntries().getDefaultCollectionMetadataEntries().addAll(parentCollectionMetadataEntries);
							}
							HpcFileLocation fileLocation = new HpcFileLocation();
							fileLocation.setFileId(file.getAbsolutePath());
							dataObject.setSource(fileLocation);
							dataObject.setCallerObjectId(null);
							processRecord(dataObject, destinationBasePath,
									getObjectPath(filePathBaseName, file.getPath()), metadataOnly, confirmation);
						} else {
							processCollection(file, destinationBasePath,
									getCollectionPath(filePathBaseName, file.getPath()), confirmation);
						}

					} catch (RecordProcessingException e) {
					    logger.debug(e.getMessage(), e);
						String message = "Failed to process cmd due to: " + e.getMessage();
						writeException(e, message, null);
						success = false;
					}
				}
			}
		} catch (HpcCmdException e) {
		    logger.error(e.getMessage(), e);
			String message = "Failed to process cmd due to: " + e.getMessage();
			writeException(e, message, null);
			success = false;
		} catch (RestClientException e) {
		    logger.error(e.getMessage(), e);
			String message = "Failed to process cmd due to: " + e.getMessage();
			writeException(e, message, null);
			success = false;
		} catch (Exception e) {
		    logger.error(e.getMessage(), e);
			String message = "Failed to process cmd due to: " + e.getMessage();
			writeException(e, message, null);
			success = false;
		}
		return success;
	}

	private List<HpcMetadataEntry> getParentCollectionMetadata(HpcPathAttributes file, boolean metadataOnly) {
		List<HpcMetadataEntry> parentCollectionMetadataEntries = new ArrayList<HpcMetadataEntry>();
		HpcPathAttributes pathAttributes = new HpcPathAttributes();
		String filePath = file.getPath().replace("\\", "/");

		int pathIndex = filePath.lastIndexOf("/");

		String parentPath = filePath.substring(0, pathIndex == -1 ? filePath.length() : pathIndex);
		int nameIndex = parentPath.lastIndexOf(File.separator);
		String parentName = parentPath.substring(nameIndex != -1 ? nameIndex : 0, parentPath.length());
		pathAttributes.setPath(parentPath.replace("/", File.separator));
		pathAttributes.setName(parentName);
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		pathAttributes.setUpdatedDate(sdf.format(new Date()));
		pathAttributes.setAbsolutePath(parentPath.replace("/", File.separator));
		List<HpcMetadataEntry> metadata = getMetadata(pathAttributes, metadataOnly);

		if (metadata == null)
			return null;
		else
			parentCollectionMetadataEntries.addAll(metadata);

		boolean collectionType = false;
		for (HpcMetadataEntry entry : parentCollectionMetadataEntries) {
			if (entry.getAttribute().equals("collection_type"))
				collectionType = true;
		}

		if (!collectionType) {
			HpcMetadataEntry typeEntry = new HpcMetadataEntry();
			typeEntry.setAttribute("collection_type");
			typeEntry.setValue("Folder");
			parentCollectionMetadataEntries.add(typeEntry);
		}

		return parentCollectionMetadataEntries;
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
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
				}
		}
		return patterns;
	}

	private List<HpcMetadataEntry> getMetadata(HpcPathAttributes file, boolean metadataOnly) throws HpcCmdException {
		String fullPath = file.getAbsolutePath();
    File metadataFile = new File(fullPath + ".metadata.json");
//    final String fullPath = file.getAbsolutePath().concat(".metadata.json");
//		File metadataFile = new File(Paths.generateFileSystemResourceUri(fullPath));
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
			    logger.error(e.getMessage(), e);
				throw new HpcCmdException(
						"Failed to read JSON metadata file: " + file.getAbsolutePath() + " Reason: " + e.getMessage());
			} catch (IOException e) {
			    logger.error(e.getMessage(), e);
				throw new HpcCmdException(
						"Failed to read JSON metadata file: " + file.getAbsolutePath() + " Reason: " + e.getMessage());
			}
		} else {
			if (metadataOnly)
				return null;
			HpcMetadataEntry nameEntry = new HpcMetadataEntry();
			nameEntry.setAttribute("name");
			nameEntry.setValue(file.getName());
			metadataEntries.add(nameEntry);
			HpcMetadataEntry dateEntry = new HpcMetadataEntry();
			dateEntry.setAttribute("modified_date");
			if (file.getUpdatedDate() != null)
				dateEntry.setValue(file.getUpdatedDate());
			else {
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
				dateEntry.setValue(sdf.format(new Date()));
			}
			metadataEntries.add(dateEntry);
			if (file.getIsDirectory()) {
				HpcMetadataEntry typeEntry = new HpcMetadataEntry();
				typeEntry.setAttribute("collection_type");
				typeEntry.setValue("Folder");
				metadataEntries.add(typeEntry);
			}
		}
		return metadataEntries;
	}

	private String getObjectPath(String filePathBaseName, String filePath) {
		filePath = filePath.replace('\\', '/');
		String name = filePathBaseName + "/";
		if (filePath.indexOf(name) != -1)
			return filePath.substring(filePath.indexOf(name));
		else
			return filePath;
	}

	private String getCollectionPath(String collectionPathBaseName, String collectionPath) {

		collectionPath = collectionPath.replace("\\", "/");
		String name = "/" + collectionPathBaseName;
		if (collectionPath.indexOf(name) != -1)
			return collectionPath.substring(collectionPath.indexOf(name) + 1);
		else
			return collectionPath;
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

	public void processRecord(HpcDataObjectRegistrationRequestDTO hpcDataObjectRegistrationDTO, String basePath,
			String objectPath, boolean metadataOnly, boolean confirmation) throws RecordProcessingException {
		InputStream inputStream = null;
		InputStream checksumStream = null;
		HpcExceptionDTO response = null;
		String jsonInString = null;
		List<Attachment> atts = new LinkedList<Attachment>();
		try {
		    logger.debug("metadataOnly "+metadataOnly);
			if (!metadataOnly) {
				inputStream = new BufferedInputStream(
						new FileInputStream(hpcDataObjectRegistrationDTO.getSource().getFileId()));
				checksumStream = new FileInputStream(hpcDataObjectRegistrationDTO.getSource().getFileId());
				ContentDisposition cd2 = new ContentDisposition(
						"attachment;filename=" + hpcDataObjectRegistrationDTO.getSource().getFileId());
				atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObject", inputStream, cd2));
				HashCode hash = com.google.common.io.Files
						.hash(new File(hpcDataObjectRegistrationDTO.getSource().getFileId()), Hashing.md5());
				String md5 = hash.toString();
				hpcDataObjectRegistrationDTO.setChecksum(md5);
			}
			hpcDataObjectRegistrationDTO.setSource(null);
		} catch (FileNotFoundException e) {
		    logger.error(e.getMessage(), e);
			String message = "Failed to process record due to: " + e.getMessage();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = basePath + "/" + objectPath + "|" + jsonInString + "\n" + sw.toString();
			writeException(e, message, null);
			throw new RecordProcessingException(exceptionAsString);
		} catch (Exception e) {
		    logger.error(e.getMessage(), e);
			String message = "Failed to process record due to: " + e.getMessage();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = basePath + "/" + objectPath + "|" + jsonInString + "\n" + sw.toString();
			writeException(e, message, null);
			throw new RecordProcessingException(exceptionAsString);
		}

		atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment(
      "dataObjectRegistration", "application/json; charset=UTF-8",
      hpcDataObjectRegistrationDTO));
		objectPath = objectPath.replace("//", "/");
		objectPath = objectPath.replace("\\", "/");
		if (objectPath.charAt(0) != File.separatorChar)
			objectPath = "/" + objectPath;
		System.out.println("Processing: " + basePath + objectPath);
		System.out.println("checksum: " + hpcDataObjectRegistrationDTO.getChecksum());
		if (confirmation) {
			jline.console.ConsoleReader reader;
			try {
				reader = new jline.console.ConsoleReader();
				reader.setExpandEvents(false);
				System.out.println("Are you sure you want to register ? (Y/N):");
				String confirm = reader.readLine();
				if (confirm != null && !confirm.equalsIgnoreCase("Y")) {
					System.out.println("Skipped registering data file " + objectPath);
					return;
				}
			} catch (IOException e) {
				throw new HpcBatchException("Failed to get confirmation " + e.getMessage());
			}

		}

    String apiUrl2Apply;
    try {
      apiUrl2Apply = UriComponentsBuilder.fromHttpUrl(hpcServerURL).path(
        "/dataObject/{base-path}/{object-path}").buildAndExpand(basePath,
				objectPath).encode().toUri().toURL().toExternalForm();
    } catch (MalformedURLException mue) {
      final String pathUnderServerUrl = HpcClientUtil.constructPathString(
        "dataObject", basePath, objectPath);
      final String informativeMsg = new StringBuilder("Error in attempt to")
          .append(" build URL for making REST service call.\nBase server URL [")
          .append(hpcServerURL).append("].\nPath under base")
          .append(" serve URL [").append(pathUnderServerUrl).append("].\n")
          .toString();
      throw new RecordProcessingException(informativeMsg, mue);
    }
		WebClient client = HpcClientUtil.getWebClient(apiUrl2Apply,
      hpcServerProxyURL, hpcServerProxyPort, hpcCertPath, hpcCertPassword);
		client.header("Authorization", "Bearer " + authToken);
		client.header("Connection", "Keep-Alive");
//		client.type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON);
    client.type(MediaType.MULTIPART_FORM_DATA).accept(
      "application/json; charset=UTF-8");

		try {

			Response restResponse = client.put(new MultipartBody(atts));
			if (!(restResponse.getStatus() == 201 || restResponse.getStatus() == 200)) {
				MappingJsonFactory factory = new MappingJsonFactory();
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
				try {
					response = parser.readValueAs(HpcExceptionDTO.class);
				} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
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
				} else {
					HpcLogWriter.getInstance().WriteLog(logFile, basePath + "/" + objectPath + "|" + jsonInString);
					throw new RecordProcessingException(
							"Failed to process record due to unknown error. Return code: " + restResponse.getStatus());
				}
			} else {
				System.out.println("Success! ");
			}
		} catch (HpcBatchException e) {
		    logger.error(e.getMessage(), e);
			String message = "Failed to process record due to: " + e.getMessage();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = basePath + "/" + objectPath + "|" + jsonInString + "\n" + sw.toString();
			writeException(e, message, null);
			throw new RecordProcessingException(exceptionAsString);
		} catch (RestClientException e) {
		    logger.error(e.getMessage(), e);
			String message = "Failed to process record due to: " + e.getMessage();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String exceptionAsString = basePath + "/" + objectPath + "|" + jsonInString + "\n" + sw.toString();
			writeException(e, message, null);
			throw new RecordProcessingException(exceptionAsString);
		} catch (Exception e) {
		    logger.error(e.getMessage(), e);
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
			if (checksumStream != null)
				try {
					checksumStream.close();
				} catch (IOException e) {
					// e.printStackTrace();
				}
		}
	}

	public void processCollection(HpcPathAttributes file, String basePath, String collectionPath, boolean confirmation)
			throws RecordProcessingException {
	    logger.debug("processCollection: file "+file);
	    logger.debug("processCollection: basePath "+basePath);
	    logger.debug("processCollection: collectionPath "+collectionPath);
	    logger.debug("processCollection: confirmation "+confirmation);
		collectionPath = collectionPath.replace("//", "/");
		collectionPath = collectionPath.replace("\\", "/");
		List<HpcMetadataEntry> metadataList = getMetadata(file, false);

		if (metadataList == null || metadataList.isEmpty()) {
			throw new HpcBatchException("No metadata at add. Skipping collection: " + file.getAbsolutePath());
		}

		HpcCollectionRegistrationDTO collectionDTO = new HpcCollectionRegistrationDTO();
		HpcBulkMetadataEntries bulkMetadataEntries = new HpcBulkMetadataEntries();
	    collectionDTO.setParentCollectionsBulkMetadataEntries(bulkMetadataEntries);
		collectionDTO.getMetadataEntries().addAll(metadataList);
		List<HpcMetadataEntry> parentMetadataList = getParentCollectionMetadata(file, false);
		collectionDTO.setCreateParentCollections(true);
		collectionDTO.getParentCollectionsBulkMetadataEntries().getDefaultCollectionMetadataEntries().addAll(parentMetadataList);

		System.out.println("Registering Collection " + collectionPath);

		if (confirmation) {
			jline.console.ConsoleReader reader;
			try {
				reader = new jline.console.ConsoleReader();
				reader.setExpandEvents(false);
				System.out.println("Are you sure you want to register? (Y/N):");
				String confirm = reader.readLine();
				if (confirm != null && !confirm.equalsIgnoreCase("Y")) {
					System.out.println("Skipped registering Collection " + collectionPath);
					return;
				}
			} catch (IOException e) {
				throw new HpcBatchException("Failed to get confirmation " + e.getMessage());
			}

		}

    String apiUrl2Apply;
		try {
      apiUrl2Apply = UriComponentsBuilder.fromHttpUrl(hpcServerURL).path(
        "/collection/{base-path}/{collection-path}").buildAndExpand(basePath,
        collectionPath).encode().toUri().toURL().toExternalForm();
    } catch (MalformedURLException mue) {
      String pathFromBaseServerUrl = HpcClientUtil.constructPathString(
      "collection", basePath, collectionPath);
      final String informativeMsg = new StringBuilder("Error in attempt to")
        .append(" build URL for making REST service call.\nBase server URL [")
        .append(hpcServerURL).append("].\nPath under base")
        .append(" server URL [").append(pathFromBaseServerUrl).append("].\n")
        .toString();
      throw new HpcBatchException(informativeMsg, mue);
    }
//		WebClient client = HpcClientUtil.getWebClient(hpcServerURL + "/collection" + basePath + "/" + collectionPath,
//				hpcServerProxyURL, hpcServerProxyPort, hpcCertPath, hpcCertPassword);
		WebClient client = HpcClientUtil.getWebClient(apiUrl2Apply,
      hpcServerProxyURL, hpcServerProxyPort, hpcCertPath, hpcCertPassword);
		client.header("Authorization", "Bearer " + authToken);
		client.header("Connection", "Keep-Alive");

		Response restResponse = client.invoke("PUT", collectionDTO);
		if (restResponse.getStatus() == 201 || restResponse.getStatus() == 200) {
			System.out.println("Success!");
			return;
		} else {
			ObjectMapper mapper = new ObjectMapper();
			AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
					new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()), new JacksonAnnotationIntrospector());
			mapper.setAnnotationIntrospector(intr);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			MappingJsonFactory factory = new MappingJsonFactory(mapper);
			JsonParser parser;
			try {
				parser = factory.createParser((InputStream) restResponse.getEntity());
				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				throw new RecordProcessingException(exception.getMessage());
			} catch (IllegalStateException | IOException e) {
			    logger.error(e.getMessage(), e);
				throw new RecordProcessingException(e.getMessage());
			}
		}
	}
}
