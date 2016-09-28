package gov.nih.nci.hpc.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import gov.nih.nci.hpc.cli.domain.HPCCollectionRecord;
import gov.nih.nci.hpc.cli.util.CsvFileWriter;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcCmdException;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;

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
				e.printStackTrace();
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
			e.printStackTrace();
		}
	}

	protected boolean processCmd(String cmd, String criteria, String outputFile, String format, String detail,
			String userId, String password) {
		boolean success = true;

		try {
			String serviceURL = hpcServerURL + "/" + hpcCollectionService;

			if (cmd == null || cmd.isEmpty() || criteria == null || criteria.isEmpty()) {
				System.out.println("Invlaid Command");
				return false;
			}

			String criteriaClause = buildCriteria(criteria);
			if (cmd.equals("getCollection"))
				serviceURL = serviceURL + criteria;
			else if (cmd.equals("getCollections")) {
				// serviceURL = serviceURL + "?metadataQuery=" +
				// URLEncoder.encode(criteria, "UTF-8");
				serviceURL = serviceURL + "?" + criteriaClause;
				if (detail == null || detail.isEmpty() || !detail.equalsIgnoreCase("no"))
					serviceURL = serviceURL + "&detailedResponse=true";
			}

			System.out.println("Executing: " + serviceURL);
			try {
				String authToken = HpcClientUtil.getAuthenticationToken(userId, password, hpcServerURL);

				WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcCertPath, hpcCertPassword);
				client.header("Authorization", "Bearer " + authToken);

				Response restResponse = client.invoke("GET", null);
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
							JsonParser parser = factory.createJsonParser((InputStream) restResponse.getEntity());
							try {
								HpcCollectionListDTO collections = parser.readValueAs(HpcCollectionListDTO.class);
								String[] header = { "collectionPaths" };
								CsvFileWriter.writePathsCsvFile(logRecordsFile, header,
										collections.getCollectionPaths());
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
							JsonParser parser = factory.createJsonParser((InputStream) restResponse.getEntity());
							try {
								HpcCollectionListDTO collections = parser.readValueAs(HpcCollectionListDTO.class);

								CsvFileWriter.writeCollectionsCsvFile(logRecordsFile,
										getHeader(collections.getCollections()),
										buildCollection(collections.getCollections()));
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

	private String buildCriteria(String criteria) throws UnsupportedEncodingException {
		int index = criteria.indexOf("&&");
		if (index == -1)
			return "metadataQuery=" + URLEncoder.encode(criteria, "UTF-8");

		StringTokenizer tokens = new StringTokenizer(criteria, "&&");
		StringBuffer buffer = new StringBuffer();
		while (tokens.hasMoreTokens()) {
			buffer.append("metadataQuery=" + URLEncoder.encode(tokens.nextToken(), "UTF-8"));
			if (tokens.hasMoreTokens())
				buffer.append("&");
		}
		return buffer.toString();
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
				collection.setModifiedAt(dateFormat.format(dto.getCollection().getModifiedAt().getTime()));
			} catch (Exception e) {
				e.printStackTrace();
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
}
