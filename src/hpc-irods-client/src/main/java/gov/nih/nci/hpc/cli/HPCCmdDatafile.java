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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.google.gson.Gson;

import gov.nih.nci.hpc.cli.domain.HPCDataFileRecord;
import gov.nih.nci.hpc.cli.util.CsvFileWriter;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcCmdException;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcCompoundMetadataQueryDTO;

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
				e.printStackTrace();
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
			e.printStackTrace();
		}
	}

	protected boolean processCmd(String cmd, Map<String, String> criteria, String outputFile, String format,
			String detail, String userId, String password, String authToken) {
		boolean success = true;

		try {
			String serviceURL = hpcServerURL + "/" + hpcDataService;

			if (cmd == null || cmd.isEmpty() || criteria == null || criteria.isEmpty()) {
				System.out.println("Invlaid Command");
				return false;
			}

			try {
				if(authToken == null)
					authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath,
						hpcCertPassword);

				Response restResponse = null;
				if (cmd.equals("getDatafile")) {
					Iterator iterator = criteria.keySet().iterator();
					String path = (String) iterator.next();
					serviceURL = serviceURL + path;
					WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath, hpcCertPassword);
					client.header("Authorization", "Bearer " + authToken);
					restResponse = client.get();
				} else if (cmd.equals("getDatafiles")) {
					serviceURL = serviceURL + "/query";
					HpcCompoundMetadataQueryDTO criteriaClause = null;
					try {
						criteriaClause = buildCriteria(criteria);
					} catch (Exception e) {
						createErrorLog();
						success = false;
						String message = "Failed to parse criteria input file: " + criteria + " Error: "
								+ e.getMessage();
						addErrorToLog(message, cmd);
						StringWriter sw = new StringWriter();
						e.printStackTrace(new PrintWriter(sw));
						String exceptionAsString = sw.toString();
						addErrorToLog(exceptionAsString, cmd);
						return false;
					}
					WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcServerProxyURL, hpcServerProxyPort, hpcCertPath, hpcCertPassword);
					client.header("Authorization", "Bearer " + authToken);
					restResponse = client.post(criteriaClause);
				}

				System.out.println("Executing: " + serviceURL);

				if (restResponse.getStatus() != 204) {
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
						if (detail != null && detail.equalsIgnoreCase("no")) {
							JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
							try {
								HpcDataObjectListDTO dataobjects = parser.readValueAs(HpcDataObjectListDTO.class);
								String[] header = { "dataObjectPaths" };
								CsvFileWriter.writePathsCsvFile(logRecordsFile, header,
										dataobjects.getDataObjectPaths());
								System.out.println("Wrote results into " + logRecordsFile);
							} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
								createErrorLog();
								success = false;
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
								success = false;
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

				logRecordsFile = null;
			} catch (HpcCmdException e) {
				createErrorLog();
				success = false;
				String message = "Failed to process cmd due to: " + e.getMessage();
				// System.out.println(message);
				addErrorToLog(message, cmd);
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				addErrorToLog(exceptionAsString, cmd);
			} catch (RestClientException e) {
				createErrorLog();
				success = false;
				String message = "Failed to process cmd due to: " + e.getMessage();
				addErrorToLog(message, cmd);
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				addErrorToLog(exceptionAsString, cmd);
			} catch (Exception e) {
				createErrorLog();
				success = false;
				String message = "Failed to process cmd due to: " + e.getMessage();
				// System.out.println(message);
				addErrorToLog(message, cmd);
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String exceptionAsString = sw.toString();
				addErrorToLog(exceptionAsString, cmd);
			}

		} catch (Exception e) {
			System.out.println("Cannot read the input file");
			e.printStackTrace();
		}
		return success;
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
				e.printStackTrace();
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
