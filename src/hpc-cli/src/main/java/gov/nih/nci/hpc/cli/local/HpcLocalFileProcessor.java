package gov.nih.nci.hpc.cli.local;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import gov.nih.nci.hpc.cli.domain.HPCDataObject;
import gov.nih.nci.hpc.cli.domain.HpcServerConnection;
import gov.nih.nci.hpc.cli.util.HpcBatchException;
import gov.nih.nci.hpc.cli.util.HpcClientUtil;
import gov.nih.nci.hpc.cli.util.HpcCmdException;
import gov.nih.nci.hpc.cli.util.HpcLogWriter;
import gov.nih.nci.hpc.cli.util.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcMultipartUpload;
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadPartETag;
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadPartURL;
import gov.nih.nci.hpc.domain.metadata.HpcBulkMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompleteMultipartUploadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompleteMultipartUploadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.easybatch.core.processor.RecordProcessingException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

public class HpcLocalFileProcessor extends HpcLocalEntityProcessor {

    private RetryTemplate retryTemplate;
	String logFile;
	String recordFile;
	int maxAttempts = 1;
	long backOffPeriod = 0;
	int multipartPoolSize = 1;
	protected long multipartThreshold = 5373952000L;
	protected long multipartChunksize = 5368709120L;

	public HpcLocalFileProcessor(HpcServerConnection connection) throws IOException {
		super(connection);
		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(1);
  
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(0L);
  
        retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
	}
	
	public HpcLocalFileProcessor(HPCDataObject dataObject) throws IOException {
		super(dataObject.getConnection());
		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxAttempts);
  
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(backOffPeriod);
  
        retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        this.maxAttempts = dataObject.getMaxAttempts();
        this.backOffPeriod = dataObject.getBackOffPeriod();
        this.multipartPoolSize = dataObject.getMultipartPoolSize();
        this.multipartThreshold = dataObject.getMultipartThreshold();
        this.multipartChunksize = dataObject.getMultipartChunksize();
	}

  @Override
  public boolean process(HpcPathAttributes entity, String filePath, String filePathBaseName,
      String destinationBasePath,
      String logFile, String recordFile, boolean metadataOnly, boolean directUpload,
      boolean checksum)
      throws RecordProcessingException {
    logger.debug("HpcPathAttributes {}", entity);
    logger.debug("filePath {}", filePath);
    logger.debug("filePathBaseName {}", filePathBaseName);
    logger.debug("destinationBasePath {}", destinationBasePath);
    logger.debug("logFile {}", logFile);
    logger.debug("recordFile {}", recordFile);
    logger.debug("metadataOnly {}", metadataOnly);
    logger.debug("directUpload {}", directUpload);
    logger.debug("checksum {}", checksum);
    
    HpcDataObjectRegistrationRequestDTO dataObject = new HpcDataObjectRegistrationRequestDTO();
    HpcBulkMetadataEntries bulkMetadataEntries = new HpcBulkMetadataEntries();
    dataObject.setParentCollectionsBulkMetadataEntries(bulkMetadataEntries);
    
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
      logger.error(e.getMessage(), e);
      String message =
          "Failed to process file: " + entity.getAbsolutePath() + " Reaon: " + e.getMessage();
      System.out.println(message);
      HpcClientUtil.writeException(e, message, null, logFile);
      HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
      return false;
    }
    logger.debug("metadataEntries {}", metadataEntries);

    dataObject.getMetadataEntries().addAll(metadataEntries);
    if (!metadataOnly) {
      dataObject.setCreateParentCollections(true);
      List<HpcMetadataEntry> parentCollectionMetadataEntries = new ArrayList<HpcMetadataEntry>();
      List<HpcMetadataEntry> parentMetadataEntries = getParentCollectionMetadata(entity,
          metadataOnly);
      if (parentMetadataEntries != null) {
        logger.debug("parentMetadataEntries {}", parentMetadataEntries);
        parentCollectionMetadataEntries.addAll(parentMetadataEntries);
      }
      dataObject.getParentCollectionsBulkMetadataEntries().getDefaultCollectionMetadataEntries().addAll(parentCollectionMetadataEntries);
    }
    HpcFileLocation fileLocation = new HpcFileLocation();
    fileLocation.setFileId(entity.getAbsolutePath());
    dataObject.setCallerObjectId(null);
    String objectPath = getObjectPath(filePath, filePathBaseName, entity.getPath());
    logger.debug("objectPath {}", objectPath);

    performObjectPathValidation(objectPath, entity.getAbsolutePath(), filePath);

		File file = new File(entity.getAbsolutePath());
    if (!file.exists()) {
      String message = "File does not exist. Skipping: " + entity.getAbsolutePath();
      HpcClientUtil.writeException(new HpcBatchException(message), message, null, logFile);
      HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
      throw new RecordProcessingException(message);
    }
    logger.debug("directUpload {}", directUpload);
    boolean multipartUpload = false;
    if (directUpload && !metadataOnly) {
    	//Check whether it requires multi-part upload (default > 5GB)
        
        long multipartThresholdSize = this.multipartThreshold;
    	multipartThresholdSize = multipartThresholdSize < (5 * 1024 * 1025) ? (5 * 1024 * 1025) : multipartThresholdSize;
        if(file.length() > multipartThresholdSize) {
        	multipartUpload = true;
        	
        }
        
    	if(multipartUpload) {
    		processS3Record(entity, dataObject, filePath, destinationBasePath, objectPath,
    					checksum, true);
    	} else {
    		retryTemplate.execute(new RetryCallback<Void, RecordProcessingException>() {
    	        @Override
    	        public Void doWithRetry(RetryContext context) throws RecordProcessingException {
    	        	processS3Record(entity, dataObject, filePath, destinationBasePath, objectPath,
        			checksum, false);
    	            return null;
    	        }
    	      });
    	}
    } else {
      retryTemplate.execute(new RetryCallback<Void, RecordProcessingException>() {
        @Override
        public Void doWithRetry(RetryContext context) throws RecordProcessingException {
            processRecord(entity, dataObject, filePath, destinationBasePath, objectPath, metadataOnly,
            checksum);
            return null;
        }
      });
    }
    return true;
  }

	private void processRecord(HpcPathAttributes entity,
			HpcDataObjectRegistrationRequestDTO hpcDataObjectRegistrationDTO, String filePath, String basePath, String objectPath,
			boolean metadataOnly, boolean checksum) throws RecordProcessingException {
		InputStream inputStream = null;
		InputStream checksumStream = null;
		List<Attachment> atts = new LinkedList<>();
		logger.debug("processRecord");
		try {
			if (!metadataOnly) {
				inputStream = new BufferedInputStream(
						new FileInputStream(entity.getAbsolutePath()));
				checksumStream = new FileInputStream(entity.getAbsolutePath());
				ContentDisposition cd2 = new ContentDisposition(
						"attachment;filename=" + entity.getAbsolutePath());
				atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObject", inputStream, cd2));
				if(checksum)
				{
					HashCode hash = com.google.common.io.Files
							.hash(new File(entity.getAbsolutePath()), Hashing.md5());
					String md5 = hash.toString();
					logger.debug("md5 {}", md5);
					hpcDataObjectRegistrationDTO.setChecksum(md5);
				}
			}
		} catch (Exception e) {
			String message = "Failed to process record due to: " + e.getMessage();
			HpcClientUtil.writeException(e, message, null, logFile);
			HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
			throw new RecordProcessingException(message);
		}

		atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment(
			"dataObjectRegistration", "application/json; charset=UTF-8",
      hpcDataObjectRegistrationDTO));
		objectPath = objectPath.replace("//", "/");
		objectPath = objectPath.replace("\\", "/");
		if (objectPath.charAt(0) != File.separatorChar)
			objectPath = "/" + objectPath;
		System.out.println("Processing: " + basePath + objectPath + " | checksum: "+hpcDataObjectRegistrationDTO.getChecksum());

    String apiUrl2Apply;
    try {
			apiUrl2Apply = UriComponentsBuilder.fromHttpUrl(
				connection.getHpcServerURL())
				.path("/dataObject/{base-path}/{object-path}")
        .buildAndExpand(basePath, objectPath).encode()
        .toUri().toURL().toExternalForm();
		} catch (MalformedURLException mue) {
      final String pathFromServerUrl = HpcClientUtil.constructPathString(
        "dataObject", basePath, objectPath);
      final String informativeMsg = new StringBuilder("Error in attempt to")
					.append(" build URL for making REST service call.\nBase server URL [")
					.append(connection.getHpcServerURL()).append("].\nPath under base")
					.append(" server URL [").append(pathFromServerUrl).append("].\n")
					.toString();
      HpcClientUtil.writeException(mue, informativeMsg, null, logFile);
      HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
      throw new RecordProcessingException(informativeMsg, mue);
		}
		WebClient client = HpcClientUtil.getWebClient(apiUrl2Apply,
      connection.getHpcServerProxyURL(), connection.getHpcServerProxyPort(),
      connection.getHpcCertPath(), connection.getHpcCertPassword());
		client.header("Authorization", "Bearer " + connection.getAuthToken());
		client.header("Connection", "Keep-Alive");
    client.type(MediaType.MULTIPART_FORM_DATA).accept(
      "application/json; charset=UTF-8");

		try {

			Response restResponse = client.put(new MultipartBody(atts));
			logger.debug("restResponse.getStatus() {}", restResponse.getStatus());
			if (!(restResponse.getStatus() == 201 || restResponse.getStatus() == 200)) {
				processException(restResponse, hpcDataObjectRegistrationDTO, basePath, objectPath);
			} else {
				System.out.println("Success! ");
			}
		} catch (HpcBatchException | RestClientException e) {
		    logger.error(e.getMessage(), e);
			String message = "Failed to process record due to: " + e.getMessage();
			HpcClientUtil.writeException(e, message, null, logFile);
			HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
			throw new RecordProcessingException(message);
		} catch (Exception e) {
		    logger.error(e.getMessage(), e);
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
	  HpcDataObjectRegistrationRequestDTO hpcDataObjectRegistrationDTO, String filePath,
      String basePath, String objectPath,
      boolean checksum, boolean multipartUpload) throws RecordProcessingException {
    InputStream inputStream = null;
    InputStream checksumStream = null;
    logger.debug("processS3Record ");
    List<Attachment> atts = new LinkedList<>();

    final File file = new File(entity.getAbsolutePath());
    long partSize = this.multipartChunksize;
	int parts = 0;
    long contentLength = file.length();
    
    partSize = partSize < (5 * 1024 * 1024) ? (5 * 1024 * 1024) : partSize;
	//Adjust part size to be a multiple of 1024
	partSize = (new Double(round((double) partSize, 1024))).longValue();
    parts = (int) Math.ceil((double) contentLength / partSize);
    //Parts can not exceed 10000
    if(parts > 10000) {
    	//Adjust the part chunk size so that the number of parts is less than 10000
    	partSize = (new Double(round((double) contentLength / 10000, 1024))).longValue();
    	parts = (int) Math.ceil((double) contentLength / partSize);
    }
    
    hpcDataObjectRegistrationDTO.setGenerateUploadRequestURL(true);
    if(multipartUpload) {
    	hpcDataObjectRegistrationDTO.setUploadParts(parts);
    }
    atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment(
      "dataObjectRegistration", "application/json; charset=UTF-8",
      hpcDataObjectRegistrationDTO));
    objectPath = objectPath.replace("//", "/");
    objectPath = objectPath.replace("\\", "/");
    if (objectPath.charAt(0) != File.separatorChar) {
      objectPath = "/" + objectPath;
    }
    String md5Checksum = null;
    if (checksum) {
      HashCode hash;
      try {
        logger.debug("entity.getAbsolutePath() {}", entity.getAbsolutePath());
        hash = com.google.common.io.Files.hash(file, Hashing.md5());
        md5Checksum = hash.toString();
        logger.debug("md5Checksum {}", md5Checksum);
        hpcDataObjectRegistrationDTO.setChecksum(md5Checksum);
        System.out.println("Processing: " + basePath + objectPath + " | Checksum: " + md5Checksum);
      } catch (IOException e) {
        String message = "Failed to calculate checksum due to: " + e.getMessage();
        HpcClientUtil.writeException(e, message, null, logFile);
        HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
        throw new RecordProcessingException(message);
      }
    }
    logger.debug("connection.getHpcServerURL() {}", connection.getHpcServerURL());
    logger.debug("connection.getHpcServerProxyURL() {}", connection.getHpcServerProxyURL());
    logger.debug("connection.getHpcCertPath() {}", connection.getHpcCertPath());
    logger.debug("connection.getHpcCertPassword() {}", connection.getHpcCertPassword());

    String apiUrl2Apply;
    try {
      apiUrl2Apply = UriComponentsBuilder.fromHttpUrl(connection
        .getHpcServerURL()).path("/v2/dataObject/{base-path}/{object-path}")
        .buildAndExpand(basePath, objectPath).encode().toUri()
        .toURL().toExternalForm();
    } catch (MalformedURLException mue) {
      final String pathFromServerUrl = HpcClientUtil.constructPathString(
        "dataObject", basePath, objectPath);
      final String informativeMsg = new StringBuilder("Error in attempt to")
        .append(" build URL for making REST service call.\nBase server URL [")
        .append(connection.getHpcServerURL()).append("].\nPath under base")
        .append(" server URL [").append(pathFromServerUrl).append("].\n")
        .toString();
      HpcClientUtil.writeException(mue, informativeMsg, null, logFile);
      HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
      throw new RecordProcessingException(informativeMsg, mue);
    }

    WebClient client = HpcClientUtil.getWebClient(apiUrl2Apply,
      connection.getHpcServerProxyURL(), connection.getHpcServerProxyPort(),
      connection.getHpcCertPath(), connection.getHpcCertPassword());
    client.header("Authorization", "Bearer " + connection.getAuthToken());
    client.header("Connection", "Keep-Alive");
    client.type(MediaType.MULTIPART_FORM_DATA).accept(
      "application/json; charset=UTF-8");
    try {

      Response restResponse = client.put(new MultipartBody(atts));
      logger.debug("restResponse.getStatus() {}", restResponse.getStatus());
      if (!(restResponse.getStatus() == 201 || restResponse.getStatus() == 200)) {
        processException(restResponse, hpcDataObjectRegistrationDTO, basePath, objectPath);
      } else {
        HpcDataObjectRegistrationResponseDTO responseDTO = (HpcDataObjectRegistrationResponseDTO) HpcClientUtil
            .getObject(restResponse, HpcDataObjectRegistrationResponseDTO.class);
        if (!multipartUpload && responseDTO != null && responseDTO.getUploadRequestURL() != null) {
          uploadToUrl(responseDTO.getUploadRequestURL(), new File(entity.getAbsolutePath()),
              connection.getBufferSize(), md5Checksum);
        } else if (multipartUpload && responseDTO != null && responseDTO.getMultipartUpload() != null) {
            uploadToUrls(HpcClientUtil.constructPathString(basePath, objectPath), responseDTO.getMultipartUpload(), new File(entity.getAbsolutePath()),
                connection.getBufferSize(), md5Checksum, partSize);
         } else {
          System.out.println("Failed to get signed URL for: " + basePath + objectPath);
        }
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      String message = "Failed to process record due to: " + e.getMessage();
      HpcClientUtil.writeException(e, message, null, logFile);
      HpcClientUtil.writeRecord(filePath, entity.getAbsolutePath(), recordFile);
      throw new RecordProcessingException(message);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          System.out
              .println("HpcLocalFileProcessor - Failed to close input stream: " + e.getMessage());
        }
      }
      if (checksumStream != null) {
        try {
          checksumStream.close();
        } catch (IOException e) {
          System.out
              .println("HpcLocalFileProcessor - Failed to close input stream: " + e.getMessage());
        }
      }
    }
  }

	private void processException(Response restResponse,
			HpcDataObjectRegistrationRequestDTO hpcDataObjectRegistrationDTO, String basePath, String objectPath)
			throws RecordProcessingException {
		MappingJsonFactory factory = new MappingJsonFactory();
		HpcExceptionDTO response = null;
		String jsonInString = null;
		try (JsonParser parser = factory.createParser((InputStream) restResponse.getEntity())){
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
							+ "Unable to process error response: response status is: " + restResponse.getStatus());
					throw new RecordProcessingException(
							"Unable to process error response: response status is: " + restResponse.getStatus());
				}
			} catch (JsonProcessingException e1) {
				HpcLogWriter.getInstance().WriteLog(logFile, basePath + "/" + objectPath + "|"
						+ "Unable to process error response: response status is: " + restResponse.getStatus());
				throw new RecordProcessingException(
						"Unable to process error response: response status is: " + restResponse.getStatus());
			}
		} catch (IOException e) {
			HpcLogWriter.getInstance().WriteLog(logFile, basePath + "/" + objectPath + "|"
					+ "Unable to process error response: response status is: " + restResponse.getStatus());
			throw new RecordProcessingException(
					"Unable to process error response: response status is: " + restResponse.getStatus());
		}

	}

	private String getObjectPath(String filePath, String filePathBaseName, String objectPath) {
	  File fullFile = new File(filePath);
	  String fullFilePathName = null;
	  try {
      fullFilePathName = fullFile.getCanonicalPath();
    } catch (IOException e) {
      System.out.println("Failed to read file path: "+filePath);
    }
	  objectPath = objectPath.replace('\\', '/');
	  filePath = filePath.replace('\\', '/');
	  fullFilePathName = fullFilePathName.replace('\\', '/');
	  if(filePath.startsWith("."))
	    filePath = filePath.substring(1,  filePath.length());
	  if(objectPath.equals(fullFilePathName))
	    return objectPath;
	  if(filePathBaseName != null && !filePathBaseName.isEmpty())
	  {
	  String name = filePathBaseName + "/";
		if (filePath.indexOf(name) != -1)
			return filePath.substring(filePath.indexOf(name));
	  }
	  else
	  {
        if (objectPath.indexOf(fullFilePathName) != -1)
          return objectPath.substring(objectPath.indexOf(fullFilePathName)+fullFilePathName.length()+1);
	  }
	    return objectPath;
	}


  private void performObjectPathValidation(
    String archiveDestDataObjPath,
    String srcLocalFileAbsPath,
    String srcLocalFilePath) throws RecordProcessingException {
	  try {
	    HpcClientUtil.validateDmeArchivePath(archiveDestDataObjPath);
    } catch (HpcCmdException e) {
      HpcClientUtil.writeRecord(srcLocalFilePath, srcLocalFileAbsPath,
        this.recordFile);
      final String errorMsg = "Failed to register Data File at DME archive" +
        " destination path '" + archiveDestDataObjPath + "'.\n     " +
        e.getMessage();
	    HpcLogWriter.getInstance().WriteLog(this.logFile, errorMsg);
	    System.out.println("...  ERROR!\n" + errorMsg);
	    System.out.println("--------------------------------------------------");

      throw new RecordProcessingException(errorMsg, e);
    }
  }


	public void uploadToUrl(String urlStr, File file, int bufferSize, String checksum) throws HpcBatchException {

		HttpURLConnection httpConnection;
		try {
		  
		  if(connection.getHpcServerProxyURL() != null && !connection.getHpcServerProxyURL().isEmpty() && 
		      connection.getHpcServerProxyPort() != null && !connection.getHpcServerProxyPort().isEmpty())
		  {
    		  Properties systemProperties = System.getProperties();
    		  systemProperties.setProperty("http.proxyHost",connection.getHpcServerProxyURL());
    		  systemProperties.setProperty("http.proxyPort",connection.getHpcServerProxyPort());
		  }
		  logger.debug("uploadToUrl {}", urlStr);
		  logger.debug("bufferSize {}", bufferSize);
		  logger.debug("checksum {}", checksum);
		  logger.debug("file {}", file.getAbsolutePath());
		  URL url = new URL(urlStr);
		  try (InputStream inputStream = new FileInputStream(file)) {
			httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setDoOutput(true);
			httpConnection.setRequestMethod("PUT");

			httpConnection.setFixedLengthStreamingMode(file.length());
			httpConnection.setDoInput(true);
			httpConnection.setDoOutput(true);
			httpConnection.setConnectTimeout(99999999);
			httpConnection.setReadTimeout(99999999);
			
			if(checksum != null)
				httpConnection.addRequestProperty("md5chksum", checksum);
			OutputStream out = httpConnection.getOutputStream();
			

			byte[] buf = new byte[1024];
			int count;
			long total = 0;
			long fileSize = file.length();

			while ((count = inputStream.read(buf)) != -1) {
				if (Thread.interrupted()) {
					throw new InterruptedException();
				}
				 out.write(buf, 0, count);
				 total += count;
				 int pctComplete = new Double(new Double(total) / new
				 Double(fileSize) * 100).intValue();
				 logger.debug("pctComplete {}", pctComplete);
			}
			out.close();
		  }

			int responseCode = httpConnection.getResponseCode();
			logger.debug("responseCode {}", responseCode);
			if (responseCode == 200) {
				System.out.println("Successfully registered " + file.getAbsolutePath());
			}
			else
			{
				BufferedReader br = new BufferedReader(new InputStreamReader((httpConnection.getErrorStream())));
				StringBuilder sb = new StringBuilder();
				String output;
				while ((output = br.readLine()) != null) 
					sb.append(output);
				System.out.println("Failed to register - " + file.getAbsolutePath() + ". Check error log for details.");		
				throw new HpcBatchException("Failed to register - " + file.getAbsolutePath() + " Response: "+sb.toString());
			}
		} catch (IOException | InterruptedException e) {
			throw new HpcBatchException(e.getMessage(), e);
		}
	}
	
	private int uploadToUrls(String destinationPath, HpcMultipartUpload multipartUpload, File file, int bufferSize, String checksum, long partSize) throws InterruptedException {
		
		HpcCompleteMultipartUploadRequestDTO dto = new HpcCompleteMultipartUploadRequestDTO();
		dto.setMultipartUploadId(multipartUpload.getId());
		logger.info("fileId {}", multipartUpload.getId());
		
		int poolSize = this.multipartPoolSize;
		ExecutorService executorService = Executors.newFixedThreadPool(poolSize);
		
		List<Callable<HpcUploadPartETag>> callableTasks = new ArrayList<>();
		for (HpcUploadPartURL uploadPartUrl : multipartUpload.getParts()) {
			callableTasks.add(new HPCLocalFileMultipartUploadProcessor(uploadPartUrl, file, partSize,
					connection.getHpcServerProxyURL(), connection.getHpcServerProxyPort(),
					this.maxAttempts, this.backOffPeriod));
		}
		List<Future<HpcUploadPartETag>> futures = null;
		try {
			futures = executorService.invokeAll(callableTasks);
			for (Future<HpcUploadPartETag> future : futures) {
				HpcUploadPartETag result = future.get();
				dto.getUploadPartETags().add(result);
				logger.info("part {}: eTag: {}", result.getPartNumber(), result.getETag());
			}
		} catch (InterruptedException e) {
			throw e;
		} catch (ExecutionException e) {
			throw new HpcBatchException(e);
		}
		
		executorService.shutdown();
		
		return completeMultipartUpload(destinationPath, dto);
	}
  
	private int completeMultipartUpload(String destinationPath, HpcCompleteMultipartUploadRequestDTO dto) {

		Response restResponse;
		
		try {
			// Call completeMultipartUpload API
			String completeUrl = UriComponentsBuilder.fromHttpUrl(connection.getHpcServerURL())
					.path("/dataObject".concat(destinationPath).concat("/completeMultipartUpload"))
					.build().encode().toUri().toURL().toExternalForm();

			WebClient client = HpcClientUtil.getWebClient(completeUrl,
				      connection.getHpcServerProxyURL(), connection.getHpcServerProxyPort(),
				      connection.getHpcCertPath(), connection.getHpcCertPassword());
			client.header("Authorization", "Bearer " + connection.getAuthToken());
			client.header("Connection", "Keep-Alive");
			client.type(MediaType.APPLICATION_JSON).accept(
				      "application/json; charset=UTF-8");
			
			restResponse = client.post(dto);

			if (restResponse.getStatus() == 200) {
				MappingJsonFactory factory = new MappingJsonFactory();
				HpcCompleteMultipartUploadResponseDTO response = null;
				try (JsonParser parser = factory.createParser((InputStream) restResponse.getEntity())){
					
					response = parser.readValueAs(HpcCompleteMultipartUploadResponseDTO.class);
					if (response != null) {
					  //We will not compute the final eTag to compare against the returned eTag since checksum of each part is verified.
					  logger.info("Complete multipart upload successful: eTag {}", response.getChecksum());
					}
				} catch (Exception e) {
					HpcLogWriter.getInstance().WriteLog(logFile, destinationPath + "|"
							+ "Unable to process complete multipart upload response: response status is: " + restResponse.getStatus());
					throw new HpcBatchException(
							"Unable to process complete multipart upload response: response status is: " + restResponse.getStatus());
				}
				
			} else {
				MappingJsonFactory factory = new MappingJsonFactory();
				HpcExceptionDTO response = null;
				try (JsonParser parser = factory.createParser((InputStream) restResponse.getEntity())){
					
					response = parser.readValueAs(HpcExceptionDTO.class);
					if (response != null) {
						System.out.println("Failed to complete multipart upload: " + destinationPath + " Reason: " + response.getMessage());
						throw new HpcBatchException(
								"Failed to complete multipart upload: " + destinationPath + " Reason: " + response.getMessage());
					}
				} catch (Exception e) {
					HpcLogWriter.getInstance().WriteLog(logFile, destinationPath + "|"
							+ "Unable to process error response: response status is: " + restResponse.getStatus());
					throw new HpcBatchException(
							"Unable to process error response: response status is: " + restResponse.getStatus());
				}
			}
		} catch (Exception e) {
			throw new HpcBatchException("Complete multipart upload failed - " + e.getMessage(), e);
		}

		return restResponse.getStatus();
	}
	

	private double round(double num, int multipleOf) {
		return Math.floor((num + multipleOf / 2.0) / multipleOf) * multipleOf;
	}

}
