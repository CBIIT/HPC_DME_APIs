/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.google.gson.Gson;

import gov.nih.nci.hpc.cli.domain.HPCCollectionRecord;
import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.CsvFileWriter;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcCmdException;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

@Component
public class HPCCmdCollection extends HPCCmdClient {

	public HPCCmdCollection() {
		super();
	}

	protected void initializeLog() {
	}

	protected void createErrorLog() {
		if (logFile == null) {
			logFile = logDir + File.separator + "getCollections_errorLog"
					+ new SimpleDateFormat("yyyyMMdd'.txt'").format(new Date());
			File file1 = new File(logFile);
			try {
				if (!file1.exists()) {
					file1.createNewFile();
				}
				fileLogWriter = new FileWriter(file1, true);
			} catch (IOException e) {
				System.out.println("Failed to initialize Batch process: " + e.getMessage());
			}
		}
	}

	protected void createRecordsLog(String fileName, String type) {
		if (fileName == null || fileName.isEmpty())
			fileName = logDir + File.separator + "getCollections_Records"
					+ new SimpleDateFormat("yyyyMMdd").format(new Date());

		logRecordsFile = fileName;

		if (type != null && type.equalsIgnoreCase("csv"))
			logRecordsFile = logRecordsFile + ".csv";
		else if (type != null && type.equalsIgnoreCase("json"))
			logRecordsFile = logRecordsFile + ".json";
		else
			logRecordsFile = logRecordsFile + ".txt";

		File file2 = new File(logRecordsFile);
		try {
			if (!file2.exists()) {
				file2.createNewFile();
			}
			fileRecordWriter = new FileWriter(file2, false);
		} catch (IOException e) {
			System.out.println("Failed to initialize output file: " + e.getMessage());
		}
	}

	protected String processCmd(String cmd, Map<String, String> criteria, String outputFile, String format,
			String detail, String userId, String password, String authToken) {
		String returnCode = null;

		try {
      final URI uri2Apply = UriComponentsBuilder.fromHttpUrl(hpcServerURL)
        .path(HpcClientUtil.prependForwardSlashIfAbsent(hpcCollectionService))
        .build().encode().toUri();
      String serviceURL = uri2Apply.toURL().toExternalForm();
			if (cmd == null || cmd.isEmpty() || criteria == null || criteria.isEmpty()) {
				System.out.println("Invlaid Command");
				return Constants.CLI_2;
			}

			try {
				if(authToken == null)
					authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath,
						hpcCertPassword);

				Response restResponse = null;
				
				if (cmd.equals("deleteCollection")) {
					Iterator iterator = criteria.keySet().iterator();
					String path = (String) iterator.next();
					restResponse =  processDeleteCmd(serviceURL, path, outputFile, detail, userId, password, authToken);
					if(restResponse == null) {
						return null;
					}
				} else if (cmd.equals("getCollection")) {
					Iterator iterator = criteria.keySet().iterator();
					String path = (String) iterator.next();
					serviceURL = UriComponentsBuilder.fromHttpUrl(serviceURL).path(
						"/{dme-archive-path}").buildAndExpand(path).encode()
						.toUri().toURL().toExternalForm();
					WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath, hpcCertPassword);
					client.header("Authorization", "Bearer " + authToken);
					//	If necessary, add client.type("application/json; charset=UTF-8");
					restResponse = client.get();
					
				}  else if (cmd.equals("getCollections")) {
          serviceURL = UriComponentsBuilder.fromHttpUrl(serviceURL).path(
            "/query").build().encode().toUri().toURL().toExternalForm();
					HpcCompoundMetadataQueryDTO criteriaClause = null;
					try {
						criteriaClause = buildCriteria(criteria);
					} catch (Exception e) {
						createErrorLog();
						String message = "Failed to parse criteria input file: " + criteria + " Error: "
								+ e.getMessage();
						addErrorToLog(message, cmd);
						StringWriter sw = new StringWriter();
						e.printStackTrace(new PrintWriter(sw));
						String exceptionAsString = sw.toString();
						addErrorToLog(exceptionAsString, cmd);
						return Constants.CLI_2;
					}
					WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath, hpcCertPassword);
					client.header("Authorization", "Bearer " + authToken);
					client.type("application/json; charset=UTF-8");
					restResponse = client.post(criteriaClause);
				}

				System.out.println("Executing: " + serviceURL);

				if (restResponse.getStatus() == 200) {
					MappingJsonFactory factory = new MappingJsonFactory();
					createRecordsLog(outputFile, format);
					if (format != null && format.equalsIgnoreCase("json")) {
						BufferedReader br = new BufferedReader(
								new InputStreamReader(((InputStream) restResponse.getEntity())));
						String output = null;
						while ((output = br.readLine()) != null) {
							JSONObject json = new JSONObject(output);
							fileRecordWriter.write(json.toString(4));
						}
						System.out.println("Wrote results into " + logRecordsFile);
						br.close();

					} else if (format != null && format.equalsIgnoreCase("csv")) {
						if (detail == null || detail.equalsIgnoreCase("no")) {
							JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
							try {
								HpcCollectionListDTO collections = parser.readValueAs(HpcCollectionListDTO.class);
								String[] header = { "collectionPaths" };
								CsvFileWriter.writePathsCsvFile(logRecordsFile, header,
										collections.getCollectionPaths());
								System.out.println("Wrote results into " + logRecordsFile);
							} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
								createErrorLog();
								returnCode = Constants.CLI_5;
								String message = "Failed to process cmd due to: " + e.getMessage();
								// System.out.println(message);
								addErrorToLog(message, cmd);
								StringWriter sw = new StringWriter();
								e.printStackTrace(new PrintWriter(sw));
								String exceptionAsString = sw.toString();
								addErrorToLog(exceptionAsString, cmd);
							}

						} else {
							JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
							try {
								HpcCollectionListDTO collections = parser.readValueAs(HpcCollectionListDTO.class);

								CsvFileWriter.writeCollectionsCsvFile(logRecordsFile,
										getHeader(collections.getCollections()),
										buildCollection(collections.getCollections()));
								System.out.println("Wrote results into " + logRecordsFile);
							} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
								createErrorLog();
								returnCode = Constants.CLI_5;
								String message = "Failed to process cmd due to: " + e.getMessage();
								// System.out.println(message);
								addErrorToLog(message, cmd);
								StringWriter sw = new StringWriter();
								e.printStackTrace(new PrintWriter(sw));
								String exceptionAsString = sw.toString();
								addErrorToLog(exceptionAsString, cmd);
							}
						}
					} else {
						BufferedReader br = new BufferedReader(
								new InputStreamReader(((InputStream) restResponse.getEntity())));
						String output = null;
						while ((output = br.readLine()) != null) {
							fileRecordWriter.write(output);
						}
						System.out.println("Wrote results into " + logRecordsFile);
						br.close();
					}
				}
				else if(restResponse.getStatus() != 204)
				{
			        ObjectMapper mapper = new ObjectMapper();
			        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
			            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
			            new JacksonAnnotationIntrospector());
			        mapper.setAnnotationIntrospector(intr);
			        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			        MappingJsonFactory factory = new MappingJsonFactory(mapper);
			        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

			        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
			        throw new HpcCmdException(exception.getMessage());
				}

				logRecordsFile = null;
			} catch (HpcCmdException e) {
				createErrorLog();
				returnCode = Constants.CLI_5;
				String message = "Failed to process cmd due to: " + e.getMessage();
				// System.out.println(message);
				addErrorToLog(message, cmd);
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				addErrorToLog(exceptionAsString, cmd);
			} catch (RestClientException e) {
				createErrorLog();
				returnCode = Constants.CLI_5;
				String message = "Failed to process cmd due to: " + e.getMessage();
				addErrorToLog(message, cmd);
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				addErrorToLog(exceptionAsString, cmd);
			} catch (Exception e) {
				createErrorLog();
				returnCode = Constants.CLI_5;
				String message = "Failed to process cmd due to: " + e.getMessage();
				// System.out.println(message);
				addErrorToLog(message, cmd);
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				addErrorToLog(exceptionAsString, cmd);
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return returnCode;
	}

	private HpcCompoundMetadataQueryDTO buildCriteria(Map<String, String> criteria)
			throws UnsupportedEncodingException, FileNotFoundException {
		Iterator iterator = criteria.keySet().iterator();
		String fileName = (String) iterator.next();

		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		Gson gson = new Gson();
		HpcCompoundMetadataQueryDTO dto = gson.fromJson(reader, HpcCompoundMetadataQueryDTO.class);
		return dto;
	}

	private String[] getHeader(List<HpcCollectionDTO> dtoCollection) {
		List<String> header = new ArrayList<String>();
		header.add("collectionId");
		header.add("absolutePath");
		header.add("collectionParentName");
		header.add("createdAt");
		header.add("modifiedAt");

		for (HpcCollectionDTO dto : dtoCollection) {
			List<HpcMetadataEntry> entries = dto.getMetadataEntries().getSelfMetadataEntries();
			for (HpcMetadataEntry entry : entries) {
				String attr = entry.getAttribute();
				if (!header.contains(attr))
					header.add(entry.getAttribute());
			}
		}
		return (String[]) header.toArray(new String[0]);
	}

	private List<HPCCollectionRecord> buildCollection(List<HpcCollectionDTO> dtoCollection) {
		List<HPCCollectionRecord> collections = new ArrayList<HPCCollectionRecord>();
		for (HpcCollectionDTO dto : dtoCollection) {
			HPCCollectionRecord collection = new HPCCollectionRecord();
			collection.setCollectionId(Integer.toString(dto.getCollection().getCollectionId()));
			collection.setAbsolutePath(dto.getCollection().getAbsolutePath());
			collection.setCollectionParentName(dto.getCollection().getCollectionParentName());
			SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
			try {
				collection.setCreatedAt(dateFormat.format(dto.getCollection().getCreatedAt().getTime()));
//				collection.setModifiedAt(dateFormat.format(dto.getCollection().getModifiedAt().getTime()));
			} catch (Exception e) {
				System.out.println("Failed to covert date: "+dto.getCollection().getCreatedAt().getTime());
			}
			Map<String, HpcMetadataEntry> mapEntries = new HashMap<String, HpcMetadataEntry>();
			List<HpcMetadataEntry> entries = dto.getMetadataEntries().getSelfMetadataEntries();
			for (HpcMetadataEntry entry : entries) {
				String attr = entry.getAttribute();
				mapEntries.put(attr, entry);
			}
			collection.setMetadataAttrs(mapEntries);
			collections.add(collection);
		}
		return collections;
	}
	
	
	private Response processDeleteCmd(String serviceURL, String path, String outputFile, 
			String recursive, String userId, String password, String authToken) throws
		JsonParseException, IOException {
		
		recursive = recursive != null ? recursive : "false";
		jline.console.ConsoleReader reader;
		reader = new jline.console.ConsoleReader();
		reader.setExpandEvents(false);
		String confirm = null;
		//Obtain confirmation from the user - multiple levels of confirmation for recursive delete
		if(recursive.equalsIgnoreCase("true")) {
			
			System.out.println("WARNING: You have requested recursive delete of the collection. This will delete all files and sub-collections within it recursively. Are you sure you want to proceed? (Y/N):");
			confirm = reader.readLine();
			if (confirm == null || !"Y".equalsIgnoreCase(confirm)) {
				System.out.println("Skipped deleting collections");
				return null;
			}
			System.out.println("Would you like to see the list of files to delete ?");
			confirm = reader.readLine();
			if (confirm == null || !"N".equalsIgnoreCase(confirm)) {
				int fileCount = 0;
				System.out.println("The following collections and files will be deleted from the Archive:");				
				fileCount = getDataObjectsPaths(serviceURL, path, authToken, true, fileCount);
				System.out.println("A total of " + fileCount + " files are marked for deletion. Proceed with deletion ? (Y/N):");
					
			} else {
				System.out.println("The collection " + path + " and all files and sub-collections within it will be recursively deleted. Proceed with deletion ? (Y/N):");
			}
		} else {
			System.out.println("The collection " + path + " will be deleted. Proceed with deletion ? (Y/N):");
		}
			
		confirm = reader.readLine();
		if (confirm == null || !"Y".equalsIgnoreCase(confirm)) {
			System.out.println("Skipped deleting collections");
			return null;
		}			
		
		//Invoke delete API if user confirms
//		serviceURL = serviceURL + path + "/?recursive=" + recursive;
		serviceURL = UriComponentsBuilder.fromHttpUrl(serviceURL)
      .path("/{dme-archive-path}")
      .queryParam("recursive", Boolean.valueOf(recursive).toString())
      .buildAndExpand(path)
      .encode().toUri().toURL().toExternalForm();
		WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath, hpcCertPassword);
		client.header("Authorization", "Bearer " + authToken);
		//	If necessary, add	client.type("application/json; charset=UTF-8");
		return client.delete();
		
	}
	
	
	private int getDataObjectsPaths(String serviceURL, String path, String authToken, 
			boolean printFilePath, int fileCount) 
	throws JsonParseException, IOException {
	System.out.println("\n" + path);
	  String servicePath = UriComponentsBuilder.fromHttpUrl(serviceURL).path(
      "/{dme-archive-path}/children").buildAndExpand(path).encode().toUri()
      .toURL().toExternalForm();
 		WebClient client = HpcClientUtil.getWebClient(servicePath, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath, hpcCertPassword);
		client.header("Authorization", "Bearer " + authToken);
		// if necessary, add client.type("application/json; charset=UTF-8");
		Response restResponse = client.get();
		MappingJsonFactory factory = new MappingJsonFactory();
		
		if (restResponse.getStatus() == 200) {
			JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
			HpcCollectionListDTO collections = parser.readValueAs(HpcCollectionListDTO.class);
			HpcCollectionDTO collectionDto = collections.getCollections().get(0);
			HpcCollection collection = collectionDto.getCollection();
			if(CollectionUtils.isNotEmpty(collection.getDataObjects())) {
				fileCount = fileCount + collection.getDataObjects().size();
				if(printFilePath) {
					for(HpcCollectionListingEntry dataObject:collection.getDataObjects()) {
						System.out.println(dataObject.getPath());
					}
				}
			}
			if(CollectionUtils.isNotEmpty(collection.getSubCollections())) {
				for(HpcCollectionListingEntry subCollection: collection.getSubCollections()) {
					fileCount = getDataObjectsPaths(serviceURL, subCollection.getPath(), authToken, printFilePath, fileCount);
				}
			}
			return fileCount;
		}  else {
	        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
	        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
	        throw new HpcCmdException("Failed to list the files under: " + path + exception.getMessage());
	    }
	
	}

}
