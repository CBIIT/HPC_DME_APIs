/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.easybatch.core.processor.RecordProcessingException;
import org.easybatch.core.processor.RecordProcessor;
import org.easybatch.core.record.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.domain.HpcServerConnection;
import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcLogWriter;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

public class HPCBatchCollectionDownloadRecordProcessor implements RecordProcessor {
	protected final Logger logger = LoggerFactory.getLogger(getClass().getName());
	protected HpcServerConnection connection;
	String logFile;
	String recordFile;
	Map<String, String> criteriaMap;

	@Override
	public Record processRecord(Record record) throws RecordProcessingException {
		HPCDataObject hpcObject = (HPCDataObject) record.getPayload();
		String objectPath = hpcObject.getBasePath();
		criteriaMap = hpcObject.getCriteriaMap();
		String sourceArchivePath = criteriaMap.get("sourceArchivePath");
		String destinationPath = criteriaMap.get("destinationPath");
		connection = hpcObject.getConnection();
		logFile = hpcObject.getLogFile();
		recordFile = hpcObject.getErrorRecordsFile();
		logger.debug("processRecord " + record.toString());
		logger.debug("sourceArchivePath " + sourceArchivePath);
		logger.debug("objectPath " + objectPath);
		logger.debug("destinationPath " + destinationPath);
		System.out.println("Processing: " + objectPath);
		Path downloadPath = null;
		Path downloadPathTemp = null;
				
		// Get the relative path from the base path and create the destination folder if
		// it doesn't exist
		try {
			Path objectPathPath = Paths.get(objectPath);
			Path sourceArchivePathPath = Paths.get(sourceArchivePath).getParent();
			Path relativePath = sourceArchivePathPath.relativize(objectPathPath);
			downloadPath = Paths.get(destinationPath, relativePath.toString());
			downloadPathTemp = Paths.get(downloadPath.toString() + "_filepart");
		} catch (Exception e) {
			System.out.println("Failed to create download path for " + objectPath);
			HpcClientUtil.writeException(e, "Failed to create download path for " + objectPath, null, logFile);
			HpcLogWriter.getInstance().WriteLog(recordFile, objectPath);
		}
		if (!Files.exists(downloadPath.getParent())) {
			try {
				Files.createDirectories(downloadPath.getParent());
			} catch (IOException e1) {
				// can be ignored.
				logger.debug("Supressed exception: ", e1);
			}
		}
		
		try {
			Files.deleteIfExists(downloadPathTemp);
		} catch (IOException e) {
			System.out.println("Previous partial file cannot be deleted " + downloadPathTemp.toString());
			HpcClientUtil.writeException(e, e.getMessage(), null, logFile);
			HpcLogWriter.getInstance().WriteLog(recordFile, objectPath);
		}

		try {
			final String serviceURL = UriComponentsBuilder.fromHttpUrl(connection.getHpcServerURL())
					.path("/dataObject/{dme-archive-path}/download").buildAndExpand(objectPath).encode().toUri().toURL()
					.toExternalForm();

			final HpcDownloadRequestDTO dto = new HpcDownloadRequestDTO();
			dto.setGenerateDownloadRequestURL(true);

			WebClient client = HpcClientUtil.getWebClient(serviceURL, connection.getHpcServerProxyURL(),
					connection.getHpcServerProxyPort(), connection.getHpcCertPath(), connection.getHpcCertPassword());
			client.header("Authorization", "Bearer " + connection.getAuthToken());
			Response restResponse = client.invoke("POST", dto);

			if (restResponse.getStatus() == 200) {

				HpcDataObjectDownloadResponseDTO downloadDTO = (HpcDataObjectDownloadResponseDTO) HpcClientUtil
						.getObject(restResponse, HpcDataObjectDownloadResponseDTO.class);
				downloadToUrl(downloadDTO.getDownloadRequestURL(), downloadPathTemp.toString());

			} else if (restResponse.getStatus() == 400) {
				// Bad request so assume that request can be retried without any state
				// to indicate S3-presigned-URL desired (or other such special
				// handling)
				dto.setGenerateDownloadRequestURL(false);
				restResponse = client.invoke("POST", dto);
				if (restResponse.getStatus() == 200) {
					handleStreamingDownloadData(downloadPathTemp.toString(), restResponse);
				} else {
					handleDownloadProblem(restResponse);
				}
			} else {
				handleDownloadProblem(restResponse);
			}
			//rename to original file name
			Files.move(downloadPathTemp, downloadPath, StandardCopyOption.REPLACE_EXISTING);
			
			System.out.println("Successfully downloaded: " + objectPath);
			return record;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			String message = "Failed to download record due to: " + e.getMessage();
			System.out.println(message);
			HpcClientUtil.writeException(new HpcBatchException(message), message, null, logFile);
			HpcLogWriter.getInstance().WriteLog(recordFile, objectPath);
			throw new RecordProcessingException(message);
		}
	}

	private void downloadToUrl(String urlStr, String filePath) throws RecordProcessingException {
		try {
			WebClient client = HpcClientUtil.getWebClient(urlStr, connection.getHpcServerProxyURL(),
					connection.getHpcServerProxyPort(), connection.getHpcCertPath(), connection.getHpcCertPassword());

			Response restResponse = client.invoke("GET", null);
			FileOutputStream fos = new FileOutputStream(new File(filePath));

			IOUtils.copy((InputStream) restResponse.getEntity(), fos);
			fos.close();
		} catch (IOException e) {
			throw new RecordProcessingException(e.getMessage(), e);
		}
	}

	private void handleDownloadProblem(Response restResponse) throws IOException, RecordProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
				new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()), new JacksonAnnotationIntrospector());
		mapper.setAnnotationIntrospector(intr);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		MappingJsonFactory factory = new MappingJsonFactory(mapper);
		JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

		try {
			HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
			throw new RecordProcessingException("Failed to download: " + exception.getMessage());
		} catch (Exception e) {
			throw new RecordProcessingException(e);
		}
	}

	private void handleStreamingDownloadData(String filePath, Response restResponse) throws IOException {
		FileOutputStream fos = new FileOutputStream(new File(filePath));
		IOUtils.copy((InputStream) restResponse.getEntity(), fos);
		fos.close();
	}

}
