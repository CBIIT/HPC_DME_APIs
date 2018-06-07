/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.globus;

import java.io.BufferedReader;
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
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.easybatch.core.processor.RecordProcessingException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.hpc.cli.domain.HpcServerConnection;
import gov.nih.nci.hpc.cli.util.Constants;
import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcCmdException;
import gov.nih.nci.hpc.cli.util.HpcLogWriter;
import gov.nih.nci.hpc.cli.util.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanPatternType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationItemDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDirectoryScanRegistrationItemDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

public class HpcGlobusDirectoryListProcessor {

	private Properties properties = new Properties();
	private String logFile;
	private String recordFile;
	HpcServerConnection connection;

	public HpcGlobusDirectoryListProcessor(String configProps) throws IOException, FileNotFoundException {
		InputStream input = new FileInputStream(configProps);
		properties.load(input);
	}

	public HpcGlobusDirectoryListProcessor(HpcServerConnection connection) throws IOException, FileNotFoundException {
		this.connection = connection;
	}

	public String run(Map<String, String> criteriaMap, String basePath, String logFile, String recordFile) {
		this.logFile = logFile;
		String returnCode = null;
		String globusEndpoint = null;
		String globusPath = null;
		HpcFileLocation fileLocation = new HpcFileLocation();
		fileLocation.setFileId(globusPath);
		fileLocation.setFileContainerId(globusEndpoint);
		Object authenticatedToken = null;
		try {
			HpcBulkDataObjectRegistrationRequestDTO registrationDTO = constructBulkRequest(criteriaMap);
			final String apiUrl2Apply = UriComponentsBuilder.fromHttpUrl(connection
        .getHpcServerURL()).path("/registration").build().encode()
        .toUri().toURL().toExternalForm();
			HpcBulkDataObjectRegistrationResponseDTO responseDTO = HpcClientUtil
        .registerBulkDatafiles(connection.getAuthToken(), apiUrl2Apply,
        registrationDTO, connection.getHpcCertPath(),
        connection.getHpcCertPassword(), connection.getHpcServerProxyURL(),
        connection.getHpcServerProxyPort());
			if (responseDTO != null) {
				StringBuffer info = new StringBuffer();
				if (registrationDTO.getDryRun()) {
					System.out.println("Dryrun results:");
					for (HpcDataObjectRegistrationItemDTO responseItem : responseDTO.getDataObjectRegistrationItems()) {
						System.out.println(responseItem.getPath());
					}
				} else {
					System.out.println(
							"Bulk Data file registration request is submmited! Task Id: " + responseDTO.getTaskId());
				}
			}

		} catch (HpcCmdException e) {
			String message = "Failed to process Globus registration: " + e.getMessage();
			writeException(e, message, null);
			returnCode = Constants.CLI_5;
		} catch (RestClientException e) {
			String message = "Failed to process Globus registration: " + e.getMessage();
			writeException(e, message, null);
			returnCode = Constants.CLI_5;
		} catch (Exception e) {
			String message = "Failed to process Globus registration: " + e.getMessage();
			writeException(e, message, null);
			returnCode = Constants.CLI_5;
		}
		return returnCode;
	}

	private void writeException(Exception e, String message, String exceptionAsString) {
		HpcLogWriter.getInstance().WriteLog(logFile, message);
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		if (exceptionAsString == null)
			exceptionAsString = sw.toString();
		HpcLogWriter.getInstance().WriteLog(logFile, exceptionAsString);
	}

	protected HpcBulkDataObjectRegistrationRequestDTO constructBulkRequest(Map<String, String> criteriaMap) {
		HpcBulkDataObjectRegistrationRequestDTO dto = new HpcBulkDataObjectRegistrationRequestDTO();
		String globusEndpoint = criteriaMap.get("globusEndpoint");
		String globusEndpointPath = criteriaMap.get("globusPath");
		String destinationBasePath = criteriaMap.get("basePath");
		String includePatternFile = criteriaMap.get("includePatternFile");
		String excludePatternFile = criteriaMap.get("excludePatternFile");
		String dryRun = criteriaMap.get("dryRun");
		String criteriaType = criteriaMap.get("patternType");

		List<String> excludePatterns = readPatternStringsfromFile(excludePatternFile);
		List<String> includePatterns = readPatternStringsfromFile(includePatternFile);

		List<HpcDirectoryScanRegistrationItemDTO> folders = new ArrayList<HpcDirectoryScanRegistrationItemDTO>();
		HpcDirectoryScanRegistrationItemDTO folder = new HpcDirectoryScanRegistrationItemDTO();
		HpcFileLocation source = new HpcFileLocation();
		source.setFileContainerId(globusEndpoint);
		source.setFileId(globusEndpointPath);
		folder.setBasePath(destinationBasePath);
		folder.setScanDirectoryLocation(source);
		folders.add(folder);
		if (criteriaType != null && criteriaType.equals("Simple"))
			folder.setPatternType(HpcDirectoryScanPatternType.SIMPLE);
		else
			folder.setPatternType(HpcDirectoryScanPatternType.REGEX);
		if (excludePatterns != null && excludePatterns.size() > 0)
			folder.getExcludePatterns().addAll(excludePatterns);
		if (includePatterns != null && includePatterns.size() > 0)
			folder.getIncludePatterns().addAll(includePatterns);
		dto.getDirectoryScanRegistrationItems().addAll(folders);
		dto.setDryRun(dryRun != null && dryRun.equals("true"));
		return dto;
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

}
