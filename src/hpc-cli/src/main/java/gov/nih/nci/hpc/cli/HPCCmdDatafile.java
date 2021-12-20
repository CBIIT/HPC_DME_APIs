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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.google.gson.Gson;

import gov.nih.nci.hpc.cli.domain.HPCDataFileRecord;
import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.CsvFileWriter;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcCmdException;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

@Component
public class HPCCmdDatafile extends HPCCmdClient {

	public HPCCmdDatafile() {
		super();
	}

	protected void initializeLog() {
	}

	protected void createErrorLog() {
		if (logFile == null) {
			logFile = logDir + File.separator + "getDatafiles_errorLog"
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
			fileName = logDir + File.separator + "getDatafiles_Records"
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
      String serviceURL = UriComponentsBuilder.fromHttpUrl(hpcServerURL).path(
        HpcClientUtil.prependForwardSlashIfAbsent(hpcDataService)).build()
        .encode().toUri().toURL().toExternalForm();
			if (cmd == null || cmd.isEmpty() || criteria == null || criteria.isEmpty()) {
				System.out.println("Invlaid Command");
				return Constants.CLI_2;
			}

			try {
				if(authToken == null)
					authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath,
						hpcCertPassword);

				Response restResponse = null;
				if (cmd.equals("deleteDatafile")) {
					String path = criteria.get("path");
					String force = criteria.get("force");
					force = force != null ? force : "false";
					serviceURL = UriComponentsBuilder.fromHttpUrl(serviceURL)
						      .path("/{dme-archive-path}")
						      .queryParam("force", force)
						      .buildAndExpand(path)
						      .encode().toUri().toURL().toExternalForm();
					jline.console.ConsoleReader reader;
					reader = new jline.console.ConsoleReader();
					reader.setExpandEvents(false);
					if(force.equalsIgnoreCase("true")) {
						System.out.println("The file " + path + " will be permanently deleted. Are you sure you want to proceed ? (Y/N):");
					} else {
						System.out.println("The file " + path + " will be deleted. Are you sure you want to proceed ? (Y/N):");
					}
					String confirm = reader.readLine();
					if (confirm == null || !"Y".equalsIgnoreCase(confirm)) {
						System.out.println("Skipped deleting file");
						return null;
					}	
					WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath, hpcCertPassword);
					client.header("Authorization", "Bearer " + authToken);
					// if necessary, add client.type("application/json; charset=UTF-8");
					restResponse = client.delete();
				}
				else if (cmd.equals("getDatafile")) {
					Iterator iterator = criteria.keySet().iterator();
					String path = (String) iterator.next();
          serviceURL = UriComponentsBuilder.fromHttpUrl(serviceURL).path(
            "/{dme-archive-path}").buildAndExpand(path).encode().toUri().toURL()
            .toExternalForm();
					WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath, hpcCertPassword);
					client.header("Authorization", "Bearer " + authToken);
					// If necessary, here add "Content-Type" header set to "application/json; charset=UTF-8" via client.type("application/json; charset=UTF-8")
					restResponse = client.get();
				} else if (cmd.equals("getDatafiles")) {
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
								HpcDataObjectListDTO dataobjects = parser.readValueAs(HpcDataObjectListDTO.class);
								String[] header = { "dataObjectPaths" };
								CsvFileWriter.writePathsCsvFile(logRecordsFile, header,
										dataobjects.getDataObjectPaths());
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
								HpcDataObjectListDTO datafiles = parser.readValueAs(HpcDataObjectListDTO.class);
								CsvFileWriter.writeDatafilesCsvFile(logRecordsFile,
										getHeader(datafiles.getDataObjects()),
										buildDatafiles(datafiles.getDataObjects()));
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
		dto.setTotalCount(true);
		return dto;
	}

	private String[] getHeader(List<HpcDataObjectDTO> dtoObjects) {
		List<String> header = new ArrayList<String>();
		header.add("dataFileId");
		header.add("collectionId");
		header.add("absolutePath");
		header.add("collectionName");
		header.add("createdAt");
		header.add("modifiedAt");

		for (HpcDataObjectDTO dto : dtoObjects) {
			List<HpcMetadataEntry> entries = dto.getMetadataEntries().getSelfMetadataEntries();
			for (HpcMetadataEntry entry : entries) {
				String attr = entry.getAttribute();
				if (!header.contains(attr))
					header.add(entry.getAttribute());
			}
		}
		return (String[]) header.toArray(new String[0]);
	}

	private List<HPCDataFileRecord> buildDatafiles(List<HpcDataObjectDTO> dtoFiles) {
		List<HPCDataFileRecord> files = new ArrayList<HPCDataFileRecord>();
		for (HpcDataObjectDTO dto : dtoFiles) {
			HPCDataFileRecord datafile = new HPCDataFileRecord();
			datafile.setDataFileId(Integer.toString(dto.getDataObject().getId()));
			datafile.setCollectionId(Integer.toString(dto.getDataObject().getCollectionId()));
			datafile.setAbsolutePath(dto.getDataObject().getAbsolutePath());
			datafile.setCollectionName(dto.getDataObject().getCollectionName());
			SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
			try {
				datafile.setCreatedAt(dateFormat.format(dto.getDataObject().getCreatedAt().getTime()));
//				datafile.setModifiedAt(dateFormat.format(dto.getDataObject().getUpdatedAt().getTime()));
			} catch (Exception e) {
				System.out.println("Failed to format date: " + dto.getDataObject().getCreatedAt().getTime());
			}
			Map<String, HpcMetadataEntry> mapEntries = new HashMap<String, HpcMetadataEntry>();
			List<HpcMetadataEntry> entries = dto.getMetadataEntries().getSelfMetadataEntries();
			for (HpcMetadataEntry entry : entries) {
				String attr = entry.getAttribute();
				mapEntries.put(attr, entry);
			}
			datafile.setMetadataAttrs(mapEntries);
			files.add(datafile);
		}
		return files;
	}
}
