package gov.nih.nci.hpc.integration.globus.impl;

import static gov.nih.nci.hpc.integration.HpcDataTransferProxy.getArchiveDestinationLocation;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.globusonline.transfer.APIError;
import org.globusonline.transfer.BaseTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient.Result;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcArchiveDataTransferConfiguration;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.integration.HpcTransferAcceptanceResponse;

/**
 * HPC Data Transfer Proxy Globus Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataTransferProxyImpl implements HpcDataTransferProxy {

  class HpcGlobusTransferAcceptanceResponse implements HpcTransferAcceptanceResponse {
    private boolean acceptTransferFlag;
    private int queueLength;

    HpcGlobusTransferAcceptanceResponse(boolean canAccept, int sizeOfQueue) {
      this.acceptTransferFlag = canAccept;
      this.queueLength = sizeOfQueue;
    }

    @Override
    public boolean canAcceptTransfer() {
      return acceptTransferFlag;
    }

    @Override
    public int getQueueSize() {
      return queueLength;
    }
  }

  // ---------------------------------------------------------------------//
  // Constants
  // ---------------------------------------------------------------------//

  // Globus transfer status strings.
  private static final String FAILED_STATUS = "FAILED";
  private static final String INACTIVE_STATUS = "INACTIVE";
  private static final String SUCCEEDED_STATUS = "SUCCEEDED";
  private static final String PERMISSION_DENIED_STATUS = "PERMISSION_DENIED";
  private static final String OK_STATUS = "OK";
  private static final String QUEUED_STATUS = "Queued";
  private static final String TIMEOUT_STATUS = "TIMEOUT";

  private static final String NOT_DIRECTORY_GLOBUS_CODE =
      "ExternalError.DirListingFailed.NotDirectory";

  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // The Globus connection instance.
  @Autowired
  private HpcGlobusConnection globusConnection = null;

  // The Globus directory browser instance.
  @Autowired
  private HpcGlobusDirectoryBrowser globusDirectoryBrowser = null;

  // Retry template. Used to automatically retry Globus service calls.
  @Autowired
  private RetryTemplate retryTemplate = null;

  // The Globus active tasks queue size.
  @Value("${hpc.integration.globus.queueSize}")
  private int globusQueueSize = 0;

  // The Logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcDataTransferProxyImpl() {}

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  // ---------------------------------------------------------------------//
  // HpcDataTransferProxy Interface Implementation
  // ---------------------------------------------------------------------//

  @Override
  public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount, String url)
      throws HpcException {
    return globusConnection.authenticate(url, dataTransferAccount);
  }

  @Override
  public HpcTransferAcceptanceResponse acceptsTransferRequests(Object authenticatedToken)
      throws HpcException {

    logger.info(String.format(
        "acceptsTransferRequests: entered with received authenticatedToken parameter = %s",
        authenticatedToken.toString()));

    JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);
    return retryTemplate.execute(arg0 -> {
      try {
        JSONObject jsonTasksLists =
            client.getResult("/task_list?filter=status:ACTIVE,INACTIVE").document;
        logger.info(String.format(
            "acceptsTransferRequests: Made request to Globus for transfer tasks, resulting JSON is \n[\n%s\n]\n",
            jsonTasksLists.toString()));
        final int qSize = jsonTasksLists.getInt("total");
        final boolean underCap = qSize < globusQueueSize;
        logger.info(String.format(
            "acceptsTransferRequests: from JSON response, determined that qSize = %s and underCap = %s",
            Integer.toString(qSize), Boolean.toString(underCap)));
        final HpcTransferAcceptanceResponse transferAcceptanceResponse =
            new HpcGlobusTransferAcceptanceResponse(underCap, qSize);
        logger.info("acceptsTransferRequests: About to return");
        return transferAcceptanceResponse;
      } catch (Exception e) {
        logger.error("acceptsTransferRequests: About to throw exception", e);
        throw new HpcException("[GLOBUS] Failed to determine active tasks count",
            HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
      }
    });
  }

  @Override
  public HpcDataObjectUploadResponse uploadDataObject(Object authenticatedToken,
      HpcDataObjectUploadRequest uploadRequest,
      HpcArchiveDataTransferConfiguration archiveDataTransferConfiguration,
      Integer uploadRequestURLExpiration, HpcDataTransferProgressListener progressListener)
      throws HpcException {
    // Progress listener not supported.
    if (progressListener != null) {
      throw new HpcException("Globus data transfer doesn't support progress listener",
          HpcErrorType.UNEXPECTED_ERROR);
    }

    if (uploadRequest.getS3UploadSource() != null) {
      throw new HpcException("Invalid upload source", HpcErrorType.UNEXPECTED_ERROR);
    }

    // Generating upload URL or direct file upload not supported.
    if (uploadRequest.getGenerateUploadRequestURL()) {
      throw new HpcException("Globus data transfer doesn't support upload URL",
          HpcErrorType.UNEXPECTED_ERROR);
    }

    // Calculate the archive destination.
    HpcFileLocation archiveDestinationLocation =
        getArchiveDestinationLocation(archiveDataTransferConfiguration.getArchiveFileLocation(),
            uploadRequest.getPath(), uploadRequest.getCallerObjectId(), false);

    if (uploadRequest.getSourceFile() != null) {
      // This is a synchronous upload request. Simply store the data to the file-system.
      // No Globus action is required here.
      return saveFile(uploadRequest.getSourceFile(), archiveDestinationLocation,
          archiveDataTransferConfiguration);
    }

    // If the archive destination file exists, generate a new archive destination w/ unique path.
    if (getPathAttributes(authenticatedToken, archiveDestinationLocation, false).getExists()) {
      archiveDestinationLocation =
          getArchiveDestinationLocation(archiveDataTransferConfiguration.getArchiveFileLocation(),
              uploadRequest.getPath(), uploadRequest.getCallerObjectId(), true);
    }

    // Submit a request to Globus to transfer the data.
    String requestId = transferData(globusConnection.getTransferClient(authenticatedToken),
        uploadRequest.getGlobusUploadSource().getSourceLocation(), archiveDestinationLocation);

    // Package and return the response.
    HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
    uploadResponse.setArchiveLocation(archiveDestinationLocation);
    uploadResponse.setDataTransferRequestId(requestId);
    uploadResponse.setDataTransferType(HpcDataTransferType.GLOBUS);
    uploadResponse.setDataTransferStarted(Calendar.getInstance());
    uploadResponse.setDataTransferCompleted(null);
    uploadResponse.setUploadSource(uploadRequest.getGlobusUploadSource().getSourceLocation());
    uploadResponse.setSourceSize(uploadRequest.getSourceSize());
    uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.IN_PROGRESS);
    return uploadResponse;
  }

  @Override
  public String downloadDataObject(Object authenticatedToken,
      HpcDataObjectDownloadRequest downloadRequest,
      HpcArchiveDataTransferConfiguration archiveDataTransferConfiguration,
      HpcDataTransferProgressListener progressListener) throws HpcException {
    // Progress listener not supported.
    if (progressListener != null) {
      throw new HpcException("Globus data transfer doesn't support progress listener",
          HpcErrorType.UNEXPECTED_ERROR);
    }

    if (downloadRequest.getFileDestination() != null) {
      // This is a synchronous download request.
      String archiveFilePath = downloadRequest.getArchiveLocation().getFileId().replaceFirst(
          archiveDataTransferConfiguration.getArchiveFileLocation().getFileId(),
          archiveDataTransferConfiguration.getArchiveDirectory());
      try {
        // Copy the file to the dowmload stage area.
        FileUtils.copyFile(new File(archiveFilePath), downloadRequest.getFileDestination());
      } catch (IOException e) {
        throw new HpcException("Failed to stage file from file system archive: " + archiveFilePath,
            HpcErrorType.DATA_TRANSFER_ERROR, e);
      }

      return String.valueOf(downloadRequest.getFileDestination().hashCode());

    } else {
      // This is an asynchrnous download request. Submit a request to Globus to transfer the data.
      return transferData(globusConnection.getTransferClient(authenticatedToken),
          downloadRequest.getArchiveLocation(),
          downloadRequest.getGlobusDestination().getDestinationLocation());
    }
  }

  @Override
  public String copyDataObject(Object authenticatedToken, HpcFileLocation sourceFile,
      HpcFileLocation destinationFile,
      HpcArchiveDataTransferConfiguration archiveDataTransferConfiguration,
      List<HpcMetadataEntry> metadataEntries) throws HpcException {
    if (sourceFile.getFileContainerId().equals(destinationFile.getFileContainerId())
        && sourceFile.getFileId().equals(destinationFile.getFileId())) {
      // We currently support a 'copy of file to itself', in which don't copy the file but rather
      // generate and store
      // metadata and return a calculated checksum.
      String archiveFilePath = destinationFile.getFileId().replaceFirst(
          archiveDataTransferConfiguration.getArchiveFileLocation().getFileId(),
          archiveDataTransferConfiguration.getArchiveDirectory());

      try {
        // Creating the metadata file.
        List<String> metadata = new ArrayList<>();
        metadataEntries.forEach(metadataEntry -> metadata
            .add(metadataEntry.getAttribute() + "=" + metadataEntry.getValue()));
        FileUtils.writeLines(getMetadataFile(archiveFilePath), metadata);

        // Returning a calculated checksum.
        return Files.hash(new File(archiveFilePath), Hashing.md5()).toString();

      } catch (IOException e) {
        throw new HpcException("Failed calculate checksum", HpcErrorType.UNEXPECTED_ERROR, e);
      }
    }

    throw new HpcException("Copy data object not supported", HpcErrorType.UNEXPECTED_ERROR);
  }

  @Override
  public void deleteDataObject(Object authenticatedToken, HpcFileLocation fileLocation,
      HpcArchiveDataTransferConfiguration archiveDataTransferConfiguration) throws HpcException {
    String archiveFilePath = fileLocation.getFileId().replaceFirst(
        archiveDataTransferConfiguration.getArchiveFileLocation().getFileId(),
        archiveDataTransferConfiguration.getArchiveDirectory());
    // Delete the archive file.
    if (!FileUtils.deleteQuietly(new File(archiveFilePath))) {
      logger.error("Failed to delete file: {}", archiveFilePath);
    }
    // Delete the metadata file.
    if (!FileUtils.deleteQuietly(getMetadataFile(archiveFilePath))) {
      logger.error("Failed to delete metadata for file: {}", archiveFilePath);
    }
  }

  @Override
  public HpcDataTransferUploadReport getDataTransferUploadStatus(Object authenticatedToken,
      String dataTransferRequestId,
      HpcArchiveDataTransferConfiguration archiveDataTransferConfiguration) throws HpcException {
    HpcGlobusDataTransferReport report =
        getDataTransferReport(authenticatedToken, dataTransferRequestId);

    HpcDataTransferUploadReport statusReport = new HpcDataTransferUploadReport();
    statusReport.setMessage(report.niceStatusDescription);
    statusReport.setBytesTransferred(report.bytesTransferred);

    if (report.status.equals(SUCCEEDED_STATUS)) {
      statusReport.setStatus(HpcDataTransferUploadStatus.ARCHIVED);

    } else if (transferFailed(authenticatedToken, dataTransferRequestId, report)) {
      // Upload failed.
      statusReport.setStatus(HpcDataTransferUploadStatus.FAILED);

    } else {
      statusReport.setStatus(HpcDataTransferUploadStatus.IN_PROGRESS);
    }

    return statusReport;
  }

  @Override
  public HpcDataTransferDownloadReport getDataTransferDownloadStatus(Object authenticatedToken,
      String dataTransferRequestId) throws HpcException {
    HpcGlobusDataTransferReport report =
        getDataTransferReport(authenticatedToken, dataTransferRequestId);

    HpcDataTransferDownloadReport statusReport = new HpcDataTransferDownloadReport();
    statusReport.setMessage(report.niceStatusDescription);
    statusReport.setBytesTransferred(report.bytesTransferred);

    if (report.status.equals(SUCCEEDED_STATUS)) {
      // Download completed successfully.
      statusReport.setStatus(HpcDataTransferDownloadStatus.COMPLETED);

    } else if (transferFailed(authenticatedToken, dataTransferRequestId, report)) {
      // Download failed.
      statusReport.setStatus(HpcDataTransferDownloadStatus.FAILED);
      if (report.niceStatus.equals(PERMISSION_DENIED_STATUS)) {
        statusReport.setMessage(report.niceStatusDescription
            + ". Check HPC-DM system-account granted write access to the destination endpoint");
      }

    } else {
      // Download still in progress.
      statusReport.setStatus(HpcDataTransferDownloadStatus.IN_PROGRESS);
    }

    return statusReport;
  }

  @Override
  public HpcPathAttributes getPathAttributes(Object authenticatedToken,
      HpcFileLocation fileLocation, boolean getSize) throws HpcException {
    JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);
    autoActivate(fileLocation.getFileContainerId(), client);
    return getPathAttributes(fileLocation, client, getSize);
  }

  @Override
  public List<HpcDirectoryScanItem> scanDirectory(Object authenticatedToken,
      HpcFileLocation directoryLocation) throws HpcException {
    JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);
    autoActivate(directoryLocation.getFileContainerId(), client);

    // Invoke the Globus directory scan service.
    HpcGlobusDirectoryScanFileVisitor directoryScanFileVisitor =
        new HpcGlobusDirectoryScanFileVisitor();
    try {
      globusDirectoryBrowser.scan(globusDirectoryBrowser.list(directoryLocation, client), client,
          directoryScanFileVisitor);
      return directoryScanFileVisitor.getScanItems();

    } catch (Exception e) {
      throw new HpcException("[GLOBUS] Failed to scan a directory: " + directoryLocation,
          HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
    }
  }

  @Override
  public String getFileContainerName(Object authenticatedToken, String fileContainerId)
      throws HpcException {
    JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);

    return retryTemplate.execute(arg0 -> {
      try {
        JSONObject jsonEndpoint = client.getResult("/endpoint/" + fileContainerId).document;
        return jsonEndpoint.getString("display_name");

      } catch (Exception e) {
        throw new HpcException("[GLOBUS] Failed to get endpoint display name",
            HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
      }
    });
  }

  // ---------------------------------------------------------------------//
  // Helper Methods
  // ---------------------------------------------------------------------//

  /**
   * Submit a data transfer request.
   *
   * @param client Client API instance.
   * @param source The source endpoint.
   * @param destination The destination endpoint.
   * @return The data transfer request ID.
   * @throws HpcException on data transfer system failure.
   */
  private String transferData(JSONTransferAPIClient client, HpcFileLocation source,
      HpcFileLocation destination) throws HpcException {
    // Activate endpoints.
    autoActivate(source.getFileContainerId(), client);
    autoActivate(destination.getFileContainerId(), client);

    // Submit transfer request.
    return retryTemplate.execute(arg0 -> {
      try {
        JSONTransferAPIClient.Result r;
        r = client.getResult("/transfer/submission_id");
        String submissionId = r.document.getString("value");
        JSONObject transfer = new JSONObject();
        transfer.put("DATA_TYPE", "transfer");
        transfer.put("submission_id", submissionId);
        transfer.put("verify_checksum", true);
        transfer.put("delete_destination_extra", false);
        transfer.put("preserve_timestamp", false);
        transfer.put("encrypt_data", false);

        JSONObject item = setJSONItem(source, destination, client);
        transfer.append("DATA", item);

        r = client.postResult("/transfer", transfer, null);
        String taskId = r.document.getString("task_id");
        logger.debug("Transfer task id :" + taskId);

        return taskId;

      } catch (APIError error) {
        logger.error("Error while submitting transfer request to Globus for" + " Source " + source
            + " and Destination " + destination + ": " + error.message, error);
        throw new HpcException(
            "[GLOBUS] Failed to transfer: " + error.message + ". Source: " + source
                + ". Destination: " + destination,
            HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, error);

      } catch (Exception e) {
        logger.error("Failed to submit transfer request to Globus for" + " Source " + source
            + " and Destination " + destination + ": " + e.getMessage(), e);
        throw new HpcException(
            "[GLOBUS] Failed to transfer: " + e.getMessage() + ". Source: " + source
                + ". Destination: " + destination,
            HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
      }
    });
  }

  private JSONObject setJSONItem(HpcFileLocation source, HpcFileLocation destination,
      JSONTransferAPIClient client) throws HpcException {
    JSONObject item = new JSONObject();
    try {
      item.put("DATA_TYPE", "transfer_item");
      item.put("source_endpoint", source.getFileContainerId());
      item.put("source_path", source.getFileId());
      item.put("destination_endpoint", destination.getFileContainerId());
      item.put("destination_path", destination.getFileId());
      item.put("recursive", getPathAttributes(source, client, false).getIsDirectory());
      return item;

    } catch (JSONException e) {
      throw new HpcException("[GLOBUS] Failed to create JSON: " + source + ", " + destination,
          HpcErrorType.DATA_TRANSFER_ERROR, e);
    }
  }

  private void autoActivate(String endpointName, JSONTransferAPIClient client) throws HpcException {
    retryTemplate.execute(arg0 -> {
      try {
        String resource =
            BaseTransferAPIClient.endpointPath(endpointName) + "/autoactivate?if_expires_in=100";
        client.postResult(resource, null, null);
        return null;

      } catch (APIError error) {
        HpcIntegratedSystem integratedSystem =
            error.statusCode >= 500 ? HpcIntegratedSystem.GLOBUS : null;
        String message = "";
        switch (error.statusCode) {
          case 404:
            message = "[GLOBUS] Endpoint doesn't exist. Make sure the endpoint name "
                + "is correct and active: " + endpointName;
            break;

          case 403:
            message = "[GLOBUS] Endpoint permission denied: " + endpointName;
            break;

          case 503:
            message = "[GLOBUS] Service is down for maintenance";
            break;

          default:
            message = "[GLOBUS] Failed to activate endpoint: " + endpointName;
            break;
        }

        throw new HpcException(message, HpcErrorType.DATA_TRANSFER_ERROR, integratedSystem, error);

      } catch (Exception e) {
        throw new HpcException("[GLOBUS] Failed to activate endpoint: " + endpointName,
            HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
      }
    });
  }

  private class HpcGlobusDataTransferReport {
    private String status = null;
    private String niceStatus = null;
    private long bytesTransferred = 0;
    private String niceStatusDescription = null;
    private String rawError = null;
  }

  /**
   * Get a data transfer report.
   *
   * @param authenticatedToken An authenticated token.
   * @param dataTransferRequestId The data transfer request ID.
   * @return The data transfer report for the request.
   * @throws HpcException on data transfer system failure.
   */
  private HpcGlobusDataTransferReport getDataTransferReport(Object authenticatedToken,
      String dataTransferRequestId) throws HpcException {
    JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);

    return retryTemplate.execute(arg0 -> {
      try {
        JSONObject jsonReport =
            client.getResult("/endpoint_manager/task/" + dataTransferRequestId).document;

        HpcGlobusDataTransferReport report = new HpcGlobusDataTransferReport();
        report.status = jsonReport.getString("status");
        report.niceStatus = jsonReport.getString("nice_status");
        report.bytesTransferred = jsonReport.getLong("bytes_transferred");
        report.niceStatusDescription = jsonReport.getString("nice_status_short_description");
        report.rawError = jsonReport.getString("nice_status_details");

        return report;

      } catch (Exception e) {
        throw new HpcException(
            "[GLOBUS] Failed to get task report for task: " + dataTransferRequestId,
            HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
      }
    });
  }

  /**
   * Get attributes of a file/directory.
   *
   * @param fileLocation The endpoint/path to check.
   * @param client Globus client API instance.
   * @param getSize If set to true, the file/directory size will be returned.
   * @return The path attributes.
   * @throws HpcException on data transfer system failure.
   */
  private HpcPathAttributes getPathAttributes(HpcFileLocation fileLocation,
      JSONTransferAPIClient client, boolean getSize) throws HpcException {
    HpcPathAttributes pathAttributes = new HpcPathAttributes();
    pathAttributes.setExists(false);
    pathAttributes.setIsDirectory(false);
    pathAttributes.setIsFile(false);
    pathAttributes.setSize(0);
    pathAttributes.setIsAccessible(true);

    // Invoke the Globus directory listing service.
    try {
      Result dirContent = globusDirectoryBrowser.list(fileLocation, client);
      pathAttributes.setExists(true);
      pathAttributes.setIsDirectory(true);
      pathAttributes.setSize(getSize ? getDirectorySize(dirContent, client) : -1);

    } catch (APIError error) {
      if (error.statusCode == 502) {
        if (error.code.equals(NOT_DIRECTORY_GLOBUS_CODE)) {
          // Path exists as a single file
          pathAttributes.setExists(true);
          pathAttributes.setIsFile(true);
          pathAttributes.setSize(getSize ? getFileSize(fileLocation, client) : -1);
        } else {
          throw new HpcException("Invalid file location:" + fileLocation,
              HpcErrorType.INVALID_REQUEST_INPUT, error);
        }
      } else if (error.statusCode == 403) {
        // Permission denied.
        pathAttributes.setExists(true);
        pathAttributes.setIsAccessible(false);
      }
      // else path was not found.

    } catch (Exception e) {
      throw new HpcException("[GLOBUS] Failed to get path attributes: " + fileLocation,
          HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
    }

    return pathAttributes;
  }

  /**
   * Get file size.
   *
   * @param fileLocation The file endpoint/path.
   * @param client Globus client API instance.
   * @return The file size in bytes.
   */
  private long getFileSize(HpcFileLocation fileLocation, JSONTransferAPIClient client) {
    // Get the directory location of the file.
    HpcFileLocation dirLocation = new HpcFileLocation();
    dirLocation.setFileContainerId(fileLocation.getFileContainerId());
    int fileNameIndex = fileLocation.getFileId().lastIndexOf('/');
    if (fileNameIndex != -1) {
      dirLocation.setFileId(fileLocation.getFileId().substring(0, fileNameIndex));
    } else {
      dirLocation.setFileId(fileLocation.getFileId());
    }

    // Extract the file name from the path.
    String fileName = fileLocation.getFileId().substring(fileNameIndex + 1);

    // List the directory content.
    try {
      Result dirContent = globusDirectoryBrowser.list(dirLocation, client);
      JSONArray jsonFiles = dirContent.document.getJSONArray("DATA");
      if (jsonFiles != null) {
        // Iterate through the directory files, and locate the file we look for.
        int filesNum = jsonFiles.length();
        for (int i = 0; i < filesNum; i++) {
          JSONObject jsonFile = jsonFiles.getJSONObject(i);
          String jsonFileName = jsonFile.getString("name");
          if (jsonFileName != null && jsonFileName.equals(fileName)) {
            // The file was found. Return its size
            return jsonFile.getLong("size");
          }
        }
      }

    } catch (Exception e) {
      // Unexpected error. Eat this.
      logger.error("Failed to calculate file size", e);
    }

    // File not found, or exception was caught.
    return 0;
  }

  /**
   * Get directory size. Sums up the size of all the files in this directory recursively.
   *
   * @param dirContent The directory content.
   * @param client Globus client API instance.
   * @return The directory size in bytes.
   */
  private long getDirectorySize(Result dirContent, JSONTransferAPIClient client) {
    HpcGlobusDirectorySizeFileVisitor fileSizeVisitor = new HpcGlobusDirectorySizeFileVisitor();
    try {
      globusDirectoryBrowser.scan(dirContent, client, fileSizeVisitor);
      return fileSizeVisitor.getSize();

    } catch (Exception e) {
      // Unexpected error. Eat this.
      logger.error("Failed to calculate directory size", e);
    }

    // Directory not found, or exception was caught.
    return 0;
  }

  /**
   * Check if a Globus transfer request failed. It is also canceling the request if needed.
   *
   * @param authenticatedToken An authenticated token.
   * @param dataTransferRequestId The globus task ID.
   * @param report The Globus transfer report.
   * @return True if the transfer failed, or false otherwise
   */
  private boolean transferFailed(Object authenticatedToken, String dataTransferRequestId,
      HpcGlobusDataTransferReport report) {
    if (report.status.equals(FAILED_STATUS)) {
      return true;
    }

    if (report.status.equals(INACTIVE_STATUS) || (!StringUtils.isEmpty(report.niceStatus)
        && !report.niceStatus.equals(OK_STATUS) && !report.niceStatus.equals(QUEUED_STATUS)
        && !report.niceStatus.equals(TIMEOUT_STATUS))) {
      // Globus task requires some manual intervention. We cancel it and consider it a
      // failure.
      logger.error(
          "Globus transfer deemed failed: task-id: {} [status: {}, rawError: {}, niceStatus: {}]",
          dataTransferRequestId, report.status, report.rawError, report.niceStatus);
      try {
        cancelTransferRequest(authenticatedToken, dataTransferRequestId);

      } catch (HpcException e) {
        logger.error("Failed to cancel task", e);
      }

      return true;
    }

    return false;
  }

  /**
   * Cancel a transfer request.
   *
   * @param authenticatedToken An authenticated token.
   * @param dataTransferRequestId The globus task ID.
   * @throws HpcException on data transfer system failure.
   */
  private void cancelTransferRequest(Object authenticatedToken, String dataTransferRequestId)
      throws HpcException {
    JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);

    retryTemplate.execute(arg0 -> {
      try {
        client.postResult("/task/" + dataTransferRequestId + "/cancel", null);
        return null;

      } catch (Exception e) {
        throw new HpcException("[GLOBUS] Failed to cancel task: " + dataTransferRequestId,
            HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
      }
    });
  }

  /**
   * Save a file to the local file system archive
   *
   * @param sourceFile The source file to store.
   * @param archiveDestinationLocation The archive destination location.
   * @param archiveDataTransferConfiguration The archive's data transfer configuration.
   * @return A data object upload response object.
   * @throws HpcException on IO exception.
   */
  private HpcDataObjectUploadResponse saveFile(File sourceFile,
      HpcFileLocation archiveDestinationLocation,
      HpcArchiveDataTransferConfiguration archiveDataTransferConfiguration) throws HpcException {
    Calendar transferStarted = Calendar.getInstance();
    String archiveFilePath = archiveDestinationLocation.getFileId().replaceFirst(
        archiveDataTransferConfiguration.getArchiveFileLocation().getFileId(),
        archiveDataTransferConfiguration.getArchiveDirectory());
    try {
      FileUtils.moveFile(sourceFile, new File(archiveFilePath));
    } catch (IOException e) {
      throw new HpcException("Failed to move file to file system storage: " + archiveFilePath,
          HpcErrorType.DATA_TRANSFER_ERROR, e);
    }

    // Package and return the response.
    HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
    uploadResponse.setArchiveLocation(archiveDestinationLocation);
    uploadResponse.setDataTransferRequestId(String.valueOf(archiveDestinationLocation.hashCode()));
    uploadResponse.setDataTransferType(HpcDataTransferType.GLOBUS);
    uploadResponse.setDataTransferStarted(transferStarted);
    uploadResponse.setDataTransferCompleted(Calendar.getInstance());
    uploadResponse.setSourceSize(sourceFile.length());
    uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.ARCHIVED);

    return uploadResponse;
  }

  /**
   * Return a metadata file for a given path.
   *
   * @param archiveFilePath The file path in the file system archive.
   * @return The metadata file associated with this path.
   */
  private File getMetadataFile(String archiveFilePath) {
    int lastSlashIndex = archiveFilePath.lastIndexOf('/');
    return new File(archiveFilePath.substring(0, lastSlashIndex) + "/."
        + archiveFilePath.substring(lastSlashIndex + 1));
  }
}
