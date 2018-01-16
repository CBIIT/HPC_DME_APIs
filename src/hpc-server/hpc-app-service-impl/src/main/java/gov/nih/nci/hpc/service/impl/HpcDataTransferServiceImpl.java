/**
 * HpcDataTransferServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileLocation;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import gov.nih.nci.hpc.dao.HpcDataDownloadDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanPatternType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcUserDownloadRequest;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcDataTransferAuthenticatedToken;
import gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcEventService;

/**
 * HPC Data Transfer Service Implementation.
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 */
public class HpcDataTransferServiceImpl implements HpcDataTransferService {
  // ---------------------------------------------------------------------//
  // Constants
  // ---------------------------------------------------------------------//

  // Data transfer system generated metadata attributes (attach to files in
  // archive)
  private static final String OBJECT_ID_ATTRIBUTE = "uuid";
  private static final String REGISTRAR_ID_ATTRIBUTE = "user_id";

  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // Map data transfer type to its proxy impl.
  private Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies =
      new EnumMap<>(HpcDataTransferType.class);

  // System Accounts locator.
  @Autowired private HpcSystemAccountLocator systemAccountLocator = null;

  // Data object download DAO.
  @Autowired private HpcDataDownloadDAO dataDownloadDAO = null;

  // Event service.
  @Autowired private HpcEventService eventService = null;

  // Data management configuration locator.
  @Autowired
  private HpcDataManagementConfigurationLocator dataManagementConfigurationLocator = null;

  // Pagination support.
  @Autowired
  @Qualifier("hpcDownloadResultsPagination")
  private HpcPagination pagination = null;

  // The download directory.
  private String downloadDirectory = null;

  // The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /**
   * Constructor for Spring Dependency Injection.
   *
   * @param dataTransferProxies The data transfer proxies.
   * @param downloadDirectory The download directory.
   * @throws HpcException on spring configuration error.
   */
  public HpcDataTransferServiceImpl(
      Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies, String downloadDirectory)
      throws HpcException {
    if (dataTransferProxies == null
        || dataTransferProxies.isEmpty()
        || downloadDirectory == null
        || downloadDirectory.isEmpty()) {
      throw new HpcException(
          "Null or empty map of data transfer proxies, or download directory",
          HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }

    this.dataTransferProxies.putAll(dataTransferProxies);
    this.downloadDirectory = downloadDirectory;
  }

  /**
   * Default Constructor.
   *
   * @throws HpcException Constructor is disabled.
   */
  @SuppressWarnings("unused")
  private HpcDataTransferServiceImpl() throws HpcException {
    throw new HpcException("Default Constructor disabled", HpcErrorType.SPRING_CONFIGURATION_ERROR);
  }

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  // ---------------------------------------------------------------------//
  // HpcDataTransferService Interface Implementation
  // ---------------------------------------------------------------------//

  @Override
  public HpcDataObjectUploadResponse uploadDataObject(
      HpcFileLocation sourceLocation,
      File sourceFile,
      boolean generateUploadRequestURL,
      String uploadRequestURLChecksum,
      String path,
      String userId,
      String callerObjectId,
      String configurationId)
      throws HpcException {
    // Validate one and only one data source is provided, or request for an upload
    // URL.
    if (sourceLocation == null && sourceFile == null && !generateUploadRequestURL) {
      throw new HpcException(
          "No data transfer source or data attachment provided or upload URL requested",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }
    if (sourceLocation != null && sourceFile != null) {
      throw new HpcException(
          "Both data transfer source and data attachment provided",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }
    if (generateUploadRequestURL && (sourceLocation != null || sourceFile != null)) {
      throw new HpcException(
          "Both data transfer source/file and URL upload request provided",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Create an upload request.
    HpcDataObjectUploadRequest uploadRequest = new HpcDataObjectUploadRequest();
    uploadRequest.setPath(path);
    uploadRequest.setCallerObjectId(callerObjectId);
    uploadRequest.setSourceLocation(sourceLocation);
    uploadRequest.setSourceFile(sourceFile);
    uploadRequest.setUploadRequestURLChecksum(uploadRequestURLChecksum);
    uploadRequest.setGenerateUploadRequestURL(generateUploadRequestURL);

    // Upload the data object file.
    return uploadDataObject(uploadRequest, configurationId);
  }

  @Override
  public HpcDataObjectDownloadResponse downloadDataObject(
      String path,
      HpcFileLocation archiveLocation,
      HpcFileLocation destinationLocation,
      boolean generateDownloadRequestURL,
      boolean destinationOverwrite,
      HpcDataTransferType dataTransferType,
      String configurationId,
      String userId,
      boolean completionEvent,
      long size)
      throws HpcException {
    // Input Validation.
    if (dataTransferType == null || !isValidFileLocation(archiveLocation)) {
      throw new HpcException("Invalid data transfer request", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    if (generateDownloadRequestURL && destinationLocation != null) {
      throw new HpcException(
          "Both data transfer destination and Presigned URL request provided",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Create a download request.
    HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
    downloadRequest.setDataTransferType(dataTransferType);
    downloadRequest.setArchiveLocation(archiveLocation);
    downloadRequest.setDestinationLocation(destinationLocation);
    downloadRequest.setDestinationOverwrite(destinationOverwrite);
    downloadRequest.setPath(path);
    downloadRequest.setConfigurationId(configurationId);
    downloadRequest.setUserId(userId);
    downloadRequest.setCompletionEvent(completionEvent);
    downloadRequest.setGenerateDownloadRequestURL(generateDownloadRequestURL);
    downloadRequest.setSize(size);
    // Create a download response.
    HpcDataObjectDownloadResponse response = new HpcDataObjectDownloadResponse();

    // Get the base archive destination.
    HpcArchive baseArchiveDestination =
        dataManagementConfigurationLocator
            .getDataTransferConfiguration(configurationId, dataTransferType)
            .getBaseArchiveDestination();

    // Get the data transfer configuration.
    HpcDataTransferConfiguration dataTransferConfiguration =
        dataManagementConfigurationLocator.getDataTransferConfiguration(
            configurationId, dataTransferType);

    if (destinationLocation == null) {
      // This is a synchronous download request. Create a destination file on the local file system.
      downloadRequest.setDestinationFile(createDownloadFile());
      response.setDestinationFile(downloadRequest.getDestinationFile());

      // Perform the synchronous download.
      String presignedURL =
          dataTransferProxies
              .get(dataTransferType)
              .downloadDataObject(
                  getAuthenticatedToken(dataTransferType, downloadRequest.getConfigurationId()),
                  downloadRequest,
                  baseArchiveDestination,
                  dataTransferConfiguration.getUploadRequestURLExpiration(),
                  null);

      releaseSystemAccountInRequestContext();

      if (generateDownloadRequestURL) {
        response.setDestinationFile(null);
        response.setDownloadRequestURL(presignedURL);
        return response;
      }
    } else if (dataTransferType.equals(HpcDataTransferType.GLOBUS)) {
      // This is an asynchronous download request from a file system archive.

      // Create a download task.
      HpcDataObjectDownloadTask downloadTask = new HpcDataObjectDownloadTask();
      downloadTask.setArchiveLocation(archiveLocation);
      downloadTask.setCompletionEvent(completionEvent);
      downloadTask.setConfigurationId(configurationId);
      downloadTask.setCreated(Calendar.getInstance());
      downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.RECEIVED);
      downloadTask.setDataTransferType(dataTransferType);
      downloadTask.setPercentComplete(0);
      downloadTask.setSize(size);
      downloadTask.setDestinationLocation(
          calculateDownloadDestinationFileLocation(
              destinationLocation,
              destinationOverwrite,
              dataTransferType,
              archiveLocation.getFileId(),
              configurationId));
      downloadTask.setPath(path);
      downloadTask.setUserId(userId);

      // Persist the download task. The download will be performed by a scheduled task picking up
      // this task in its next scheduled run.
      dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
      response.setDownloadTaskId(downloadTask.getId());
      response.setDestinationLocation(downloadTask.getDestinationLocation());

    } else if (dataTransferType.equals(HpcDataTransferType.S_3)) {
      // This is an asynchronous download request from a Cleversafe archive to Globus destination.
      // We need to perform a 2 hop download. i.e. store the data to a local GLOBUS endpoint,
      // and submit a transfer request to the caller's GLOBUS destination.
      // Both first and second hop downloads are performed asynchronously.
      HpcSecondHopDownload secondHopDownload = new HpcSecondHopDownload(downloadRequest);

      // Set the first hop file destination to be the source file of the second hop.
      downloadRequest.setDestinationFile(secondHopDownload.getSourceFile());

      // Perform the first hop download (From Cleversafe to local file system).
      try {
        dataTransferProxies
            .get(dataTransferType)
            .downloadDataObject(
                getAuthenticatedToken(dataTransferType, downloadRequest.getConfigurationId()),
                downloadRequest,
                baseArchiveDestination,
                dataTransferConfiguration.getUploadRequestURLExpiration(),
                secondHopDownload);

        // Populate the response object.
        response.setDownloadTaskId(secondHopDownload.getDownloadTask().getId());
        response.setDestinationLocation(
            secondHopDownload.getDownloadTask().getDestinationLocation());

        releaseSystemAccountInRequestContext();
      } catch (HpcException e) {
        // Cleanup the download task and rethrow.
        completeDataObjectDownloadTask(
            secondHopDownload.getDownloadTask(), false, e.getMessage(), Calendar.getInstance(), 0);

        throw (e);
      }
    }

    return response;
  }

  @Override
  public String addSystemGeneratedMetadataToDataObject(
      HpcFileLocation fileLocation,
      HpcDataTransferType dataTransferType,
      String configurationId,
      String objectId,
      String registrarId)
      throws HpcException {
    // Add metadata is done by copying the object to itself w/ attached metadata.
    final String retVal =
        dataTransferProxies
            .get(dataTransferType)
            .copyDataObject(
                getAuthenticatedToken(dataTransferType, configurationId),
                fileLocation,
                fileLocation,
                dataManagementConfigurationLocator
                    .getDataTransferConfiguration(configurationId, dataTransferType)
                    .getBaseArchiveDestination(),
                generateMetadata(objectId, registrarId));

    releaseSystemAccountInRequestContext();

    return retVal;
  }

  @Override
  public void deleteDataObject(
      HpcFileLocation fileLocation, HpcDataTransferType dataTransferType, String configurationId)
      throws HpcException {
    // Input validation.
    if (!HpcDomainValidator.isValidFileLocation(fileLocation)) {
      throw new HpcException("Invalid file location", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    dataTransferProxies
        .get(dataTransferType)
        .deleteDataObject(
            getAuthenticatedToken(dataTransferType, configurationId),
            fileLocation,
            dataManagementConfigurationLocator
                .getDataTransferConfiguration(configurationId, dataTransferType)
                .getBaseArchiveDestination());

    releaseSystemAccountInRequestContext();
  }

  @Override
  public HpcDataTransferUploadReport getDataTransferUploadStatus(
      HpcDataTransferType dataTransferType, String dataTransferRequestId, String configurationId)
      throws HpcException { // Input Validation.
    if (dataTransferRequestId == null) {
      throw new HpcException("Null data transfer request ID", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Get the data transfer configuration.
    HpcDataTransferConfiguration dataTransferConfiguration =
        dataManagementConfigurationLocator.getDataTransferConfiguration(
            configurationId, dataTransferType);

    final HpcDataTransferUploadReport retUploadReport =
      dataTransferProxies
        .get(dataTransferType)
        .getDataTransferUploadStatus(
            getAuthenticatedToken(dataTransferType, configurationId),
            dataTransferRequestId,
            dataTransferConfiguration.getBaseArchiveDestination());

    releaseSystemAccountInRequestContext();

    return retUploadReport;
  }

  @Override
  public HpcDataTransferDownloadReport getDataTransferDownloadStatus(
      HpcDataTransferType dataTransferType, String dataTransferRequestId, String configurationId)
      throws HpcException { // Input Validation.
    if (dataTransferRequestId == null) {
      throw new HpcException("Null data transfer request ID", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    final HpcDataTransferDownloadReport retDownloadReportObj =
        dataTransferProxies
            .get(dataTransferType)
            .getDataTransferDownloadStatus(
                getAuthenticatedToken(dataTransferType, configurationId), dataTransferRequestId);

    releaseSystemAccountInRequestContext();

    return retDownloadReportObj;
  }

  @Override
  public long getDataTransferSize(
      HpcDataTransferType dataTransferType, String dataTransferRequestId, String configurationId)
      throws HpcException { // Input Validation.
    if (dataTransferRequestId == null) {
      throw new HpcException("Null data transfer request ID", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    final long retVal =
        dataTransferProxies
            .get(dataTransferType)
            .getDataTransferSize(
                getAuthenticatedToken(dataTransferType, configurationId), dataTransferRequestId);

    releaseSystemAccountInRequestContext();

    return retVal;
  }

  @Override
  public HpcPathAttributes getPathAttributes(
      HpcDataTransferType dataTransferType,
      HpcFileLocation fileLocation,
      boolean getSize,
      String configurationId)
      throws HpcException {
    // Input validation.
    if (!HpcDomainValidator.isValidFileLocation(fileLocation)) {
      throw new HpcException("Invalid file location", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    final HpcPathAttributes retPathAttribs =
        dataTransferProxies
        .get(dataTransferType)
        .getPathAttributes(
            getAuthenticatedToken(dataTransferType, configurationId), fileLocation, getSize);

    releaseSystemAccountInRequestContext();

    return retPathAttribs;
  }

  public List<HpcDirectoryScanItem> scanDirectory(
      HpcDataTransferType dataTransferType,
      HpcFileLocation directoryLocation,
      String configurationId,
      List<String> includePatterns,
      List<String> excludePatterns,
      HpcDirectoryScanPatternType patternType)
      throws HpcException {
    // Input validation.
    if (!HpcDomainValidator.isValidFileLocation(directoryLocation)) {
      throw new HpcException("Invalid directory location", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Scan the directory to get a list of all files.
    List<HpcDirectoryScanItem> scanItems =
        dataTransferProxies
            .get(dataTransferType)
            .scanDirectory(
                getAuthenticatedToken(dataTransferType, configurationId), directoryLocation);

    // Filter the list based on provided patterns.
    filterScanItems(scanItems, includePatterns, excludePatterns, patternType);

    releaseSystemAccountInRequestContext();

    return scanItems;
  }

  @Override
  public File getArchiveFile(
      String configurationId, HpcDataTransferType dataTransferType, String fileId)
      throws HpcException {
    // Input validation.
    if (fileId == null) {
      throw new HpcException("Invalid file id", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Get the data transfer configuration.
    HpcDataTransferConfiguration dataTransferConfiguration =
        dataManagementConfigurationLocator.getDataTransferConfiguration(
            configurationId, dataTransferType);

    // Instantiate the file.
    File file =
        new File(getFilePath(fileId, dataTransferConfiguration.getBaseArchiveDestination()));
    if (!file.exists()) {
      throw new HpcException(
          "Archive file could not be found: " + file.getAbsolutePath(),
          HpcRequestRejectReason.FILE_NOT_FOUND);
    }

    return file;
  }

  @Override
  public List<HpcDataObjectDownloadTask> getDataObjectDownloadTasks(
      HpcDataTransferType dataTransferType) throws HpcException {
    return dataDownloadDAO.getDataObjectDownloadTasks(dataTransferType);
  }

  @Override
  public HpcDownloadTaskStatus getDownloadTaskStatus(String taskId, HpcDownloadTaskType taskType)
      throws HpcException {
    if (taskType == null) {
      throw new HpcException("Null download task type", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    HpcDownloadTaskStatus taskStatus = new HpcDownloadTaskStatus();
    HpcDownloadTaskResult taskResult = dataDownloadDAO.getDownloadTaskResult(taskId, taskType);
    if (taskResult != null) {
      // Task completed or failed. Return the result.
      taskStatus.setInProgress(false);
      taskStatus.setResult(taskResult);
      return taskStatus;
    }

    // Task still in-progress. Return either the data-object or the collection
    // active download task.
    taskStatus.setInProgress(true);

    if (taskType.equals(HpcDownloadTaskType.DATA_OBJECT)) {
      HpcDataObjectDownloadTask task = dataDownloadDAO.getDataObjectDownloadTask(taskId);
      if (task != null) {
        taskStatus.setDataObjectDownloadTask(task);
        return taskStatus;
      }
    }

    if (taskType.equals(HpcDownloadTaskType.COLLECTION)
        || taskType.equals(HpcDownloadTaskType.DATA_OBJECT_LIST)) {
      HpcCollectionDownloadTask task = dataDownloadDAO.getCollectionDownloadTask(taskId);
      if (task != null) {
        taskStatus.setCollectionDownloadTask(task);
        return taskStatus;
      }
    }

    // Task not found.
    return null;
  }

  @Override
  public void completeDataObjectDownloadTask(
      HpcDataObjectDownloadTask downloadTask,
      boolean result,
      String message,
      Calendar completed,
      long bytesTransferred)
      throws HpcException {
    // Input validation
    if (downloadTask == null) {
      throw new HpcException(
          "Invalid data object download task", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Delete the staged download file.
    if (downloadTask.getDownloadFilePath() != null
        && !FileUtils.deleteQuietly(new File(downloadTask.getDownloadFilePath()))) {
      logger.error("Failed to delete file: " + downloadTask.getDownloadFilePath());
    }

    // Cleanup the DB record.
    dataDownloadDAO.deleteDataObjectDownloadTask(downloadTask.getId());

    // Create a task result object.
    HpcDownloadTaskResult taskResult = new HpcDownloadTaskResult();
    taskResult.setId(downloadTask.getId());
    taskResult.setUserId(downloadTask.getUserId());
    taskResult.setPath(downloadTask.getPath());
    taskResult.setDataTransferRequestId(downloadTask.getDataTransferRequestId());
    taskResult.setDataTransferType(downloadTask.getDataTransferType());
    taskResult.setDestinationLocation(downloadTask.getDestinationLocation());
    taskResult.setResult(result);
    taskResult.setType(HpcDownloadTaskType.DATA_OBJECT);
    taskResult.setMessage(message);
    taskResult.setCompletionEvent(downloadTask.getCompletionEvent());
    taskResult.setCreated(downloadTask.getCreated());
    taskResult.setCompleted(completed);

    // Calculate the effective transfer speed (Bytes per second).
    taskResult.setEffectiveTransferSpeed(
        Math.toIntExact(
            bytesTransferred
                * 1000
                / (taskResult.getCompleted().getTimeInMillis()
                    - taskResult.getCreated().getTimeInMillis())));

    // Persist to the DB.
    dataDownloadDAO.upsertDownloadTaskResult(taskResult);
  }

  @Override
  public void continueDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask)
      throws HpcException {
    // Check if Globus accepts transfer requests at this time.
    if (dataTransferProxies
        .get(downloadTask.getDataTransferType())
        .acceptsTransferRequests(
            getAuthenticatedToken(
                downloadTask.getDataTransferType(), downloadTask.getConfigurationId()))) {

      // Globus accepts requests - submit the async download (to Globus).
      HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
      downloadRequest.setArchiveLocation(downloadTask.getArchiveLocation());
      downloadRequest.setCompletionEvent(downloadTask.getCompletionEvent());
      downloadRequest.setDataTransferType(downloadTask.getDataTransferType());
      downloadRequest.setDestinationLocation(downloadTask.getDestinationLocation());
      downloadRequest.setConfigurationId(downloadTask.getConfigurationId());
      downloadRequest.setPath(downloadTask.getPath());
      downloadRequest.setUserId(downloadTask.getUserId());

      // Get the data transfer configuration.
      HpcDataTransferConfiguration dataTransferConfiguration =
          dataManagementConfigurationLocator.getDataTransferConfiguration(
              downloadRequest.getConfigurationId(), downloadRequest.getDataTransferType());

      // Update the download task with the Globus request ID.
      downloadTask.setDataTransferRequestId(
          dataTransferProxies
              .get(downloadRequest.getDataTransferType())
              .downloadDataObject(
                  getAuthenticatedToken(
                      downloadRequest.getDataTransferType(), downloadRequest.getConfigurationId()),
                  downloadRequest,
                  dataTransferConfiguration.getBaseArchiveDestination(),
                  dataTransferConfiguration.getUploadRequestURLExpiration(),
                  null));

      // Persist the download task.
      downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.IN_PROGRESS);
      dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);

      releaseSystemAccountInRequestContext();
    }
  }

  @Override
  public void updateDataObjectDownloadTask(
      HpcDataObjectDownloadTask downloadTask, long bytesTransferred) throws HpcException {
    // Input validation. Note: we only expect this to be called while Globus transfer is in-progress
    // Currently, bytesTransferred are not available while S3 download is in progress.
    if (downloadTask == null
        || downloadTask.getSize() <= 0
        || bytesTransferred <= 0
        || bytesTransferred > downloadTask.getSize()
        || downloadTask.getDataTransferType().equals(HpcDataTransferType.S_3)) {
      return;
    }

    // Calculate the percent complete.
    float percentComplete = 100 * (float) bytesTransferred / downloadTask.getSize();
    if (dataManagementConfigurationLocator
        .getDataTransferConfiguration(
            downloadTask.getConfigurationId(), downloadTask.getDataTransferType())
        .getBaseArchiveDestination()
        .getType()
        .equals(HpcArchiveType.TEMPORARY_ARCHIVE)) {
      // This is a 2-hop download, and S_3 is complete. Our base % complete is 50%.
      downloadTask.setPercentComplete(50 + Math.round(percentComplete) / 2);
    } else {
      // This is a one-hop Globus download from archive to user destination (currently only supported by file system archive).
      downloadTask.setPercentComplete(Math.round(percentComplete));
    }

    dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
  }

  @Override
  public HpcCollectionDownloadTask downloadCollection(
      String path,
      HpcFileLocation destinationLocation,
      boolean destinationOverwrite,
      String userId,
      String configurationId)
      throws HpcException {
    // Validate the requested destination location.
    validateDownloadDestinationFileLocation(
        HpcDataTransferType.GLOBUS, destinationLocation, false, true, configurationId);

    // Create a new collection download task.
    HpcCollectionDownloadTask downloadTask = new HpcCollectionDownloadTask();
    downloadTask.setCreated(Calendar.getInstance());
    downloadTask.setDestinationLocation(destinationLocation);
    downloadTask.setDestinationOverwrite(destinationOverwrite);
    downloadTask.setPath(path);
    downloadTask.setUserId(userId);
    downloadTask.setType(HpcDownloadTaskType.COLLECTION);
    downloadTask.setStatus(HpcCollectionDownloadTaskStatus.RECEIVED);
    downloadTask.setConfigurationId(configurationId);

    // Persist the request.
    dataDownloadDAO.upsertCollectionDownloadTask(downloadTask);

    return downloadTask;
  }

  @Override
  public HpcCollectionDownloadTask downloadDataObjects(
      Map<String, String> dataObjectPathsMap,
      HpcFileLocation destinationLocation,
      boolean destinationOverwrite,
      String userId)
      throws HpcException {
    // Validate the requested destination location. Note: we use the configuration
    // ID of the
    // first data object path. At this time, there is no need to validate for all
    // configuration IDs.
    String configurationId = dataObjectPathsMap.values().iterator().next();
    validateDownloadDestinationFileLocation(
        HpcDataTransferType.GLOBUS, destinationLocation, false, true, configurationId);

    // Create a new collection download task.
    HpcCollectionDownloadTask downloadTask = new HpcCollectionDownloadTask();
    downloadTask.setCreated(Calendar.getInstance());
    downloadTask.setDestinationLocation(destinationLocation);
    downloadTask.setDestinationOverwrite(destinationOverwrite);
    downloadTask.getDataObjectPaths().addAll(dataObjectPathsMap.keySet());
    downloadTask.setUserId(userId);
    downloadTask.setType(HpcDownloadTaskType.DATA_OBJECT_LIST);
    downloadTask.setStatus(HpcCollectionDownloadTaskStatus.RECEIVED);
    downloadTask.setConfigurationId(configurationId);

    // Persist the request.
    dataDownloadDAO.upsertCollectionDownloadTask(downloadTask);

    return downloadTask;
  }

  @Override
  public void updateCollectionDownloadTask(HpcCollectionDownloadTask downloadTask)
      throws HpcException {
    dataDownloadDAO.upsertCollectionDownloadTask(downloadTask);
  }

  @Override
  public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(
      HpcCollectionDownloadTaskStatus status) throws HpcException {
    return dataDownloadDAO.getCollectionDownloadTasks(status);
  }

  @Override
  public void completeCollectionDownloadTask(
      HpcCollectionDownloadTask downloadTask, boolean result, String message, Calendar completed)
      throws HpcException {
    // Input validation
    if (downloadTask == null) {
      throw new HpcException(
          "Invalid collection download task", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Cleanup the DB record.
    dataDownloadDAO.deleteCollectionDownloadTask(downloadTask.getId());

    // Create a task result object.
    HpcDownloadTaskResult taskResult = new HpcDownloadTaskResult();
    taskResult.setId(downloadTask.getId());
    taskResult.setUserId(downloadTask.getUserId());
    taskResult.setPath(downloadTask.getPath());
    taskResult.setDestinationLocation(downloadTask.getDestinationLocation());
    taskResult.setResult(result);
    taskResult.setType(downloadTask.getType());
    taskResult.setMessage(message);
    taskResult.setCompletionEvent(true);
    taskResult.setCreated(downloadTask.getCreated());
    taskResult.setCompleted(completed);
    taskResult.getItems().addAll(downloadTask.getItems());

    // Calculate the effective transfer speed (Bytes per second). This is done by averaging the effective transfer speed
    // of all successful download items.
    int effectiveTransferSpeed = 0;
    int completedItems = 0;
    for (HpcCollectionDownloadTaskItem item : downloadTask.getItems()) {
      if (item.getResult()) {
        effectiveTransferSpeed += item.getEffectiveTransferSpeed();
        completedItems++;
      }
    }
    taskResult.setEffectiveTransferSpeed(
        completedItems > 0 ? effectiveTransferSpeed / completedItems : null);

    // Persist to DB.
    dataDownloadDAO.upsertDownloadTaskResult(taskResult);
  }

  @Override
  public List<HpcUserDownloadRequest> getDownloadRequests(String userId) throws HpcException {
    List<HpcUserDownloadRequest> downloadRequests =
        dataDownloadDAO.getDataObjectDownloadRequests(userId);
    downloadRequests.addAll(dataDownloadDAO.getCollectionDownloadRequests(userId));
    return downloadRequests;
  }

  @Override
  public List<HpcUserDownloadRequest> getDownloadResults(String userId, int page)
      throws HpcException {
    return dataDownloadDAO.getDownloadResults(
        userId, pagination.getOffset(page), pagination.getPageSize());
  }

  @Override
  public int getDownloadResultsCount(String userId) throws HpcException {
    return dataDownloadDAO.getDownloadResultsCount(userId);
  }

  @Override
  public int getDownloadResultsPageSize() {
    return pagination.getPageSize();
  }

  @Override
  public String getFileContainerName(
      HpcDataTransferType dataTransferType, String configurationId, String fileContainerId)
      throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(fileContainerId)) {
      throw new HpcException("Null / Empty file container ID.", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    final String retVal =
        dataTransferProxies
            .get(dataTransferType)
            .getFileContainerName(
                getAuthenticatedToken(dataTransferType, configurationId), fileContainerId);

    releaseSystemAccountInRequestContext();

    return retVal;
  }

  /**
   * Calculate a data object upload % complete. Note: if upload not in progress, null is returned.
   *
   * @param systemGeneratedMetadata The system generated metadata of the data object.
   * @return The transfer % completion if transfer is in progress, or null otherwise.
   */
  @Override
  public Integer calculateDataObjectUploadPercentComplete(
      HpcSystemGeneratedMetadata systemGeneratedMetadata) {
    // Get the data transfer info from the metadata entries.
    HpcDataTransferUploadStatus dataTransferStatus =
        systemGeneratedMetadata.getDataTransferStatus();
    HpcDataTransferType dataTransferType = systemGeneratedMetadata.getDataTransferType();
    if (dataTransferStatus == null || dataTransferType == null) {
      return null;
    }

    if (dataTransferStatus.equals(HpcDataTransferUploadStatus.IN_TEMPORARY_ARCHIVE)) {
      // Transfer is exactly half way in a 2-hop upload.
      return 50;
    }

    if (dataTransferStatus.equals(HpcDataTransferUploadStatus.IN_PROGRESS_TO_TEMPORARY_ARCHIVE)
        || dataTransferStatus.equals(HpcDataTransferUploadStatus.IN_PROGRESS_TO_ARCHIVE)) {
      // Data transfer is in progress.
      if (dataTransferType.equals(HpcDataTransferType.S_3)) {
        // We don't have visibility into S3 transfer. Return a 50.
        return 50;
      }

      String dataTransferRequestId = systemGeneratedMetadata.getDataTransferRequestId();
      String configurationId = systemGeneratedMetadata.getConfigurationId();
      Long sourceSize = systemGeneratedMetadata.getSourceSize();
      if (configurationId == null
          || dataTransferRequestId == null
          || sourceSize == null
          || sourceSize <= 0) {
        return null;
      }

      // Get the size of the data transferred so far.
      long transferSize = 0;
      try {
        // Get the data transfer configuration.
        HpcDataTransferConfiguration dataTransferConfiguration =
            dataManagementConfigurationLocator.getDataTransferConfiguration(
                configurationId, dataTransferType);

        transferSize =
            dataTransferProxies
                .get(dataTransferType)
                .getDataTransferUploadStatus(
                    getAuthenticatedToken(dataTransferType, configurationId),
                    dataTransferRequestId,
                    dataTransferConfiguration.getBaseArchiveDestination())
                .getBytesTransferred();

      } catch (HpcException e) {
        logger.error("Failed to get data transfer upload status: " + dataTransferRequestId, e);
        return null;
      }

      float percentComplete = (float) 100 * transferSize / sourceSize;
      if (dataTransferStatus.equals(HpcDataTransferUploadStatus.IN_PROGRESS_TO_TEMPORARY_ARCHIVE)) {
        // Transfer is in 1st hop of 2-hop upload. The Globus % complete is half of the overall upload.
        percentComplete /= 2;
      }

      return Math.round(percentComplete);
    }

    return null;
  }

  // ---------------------------------------------------------------------//
  // Helper Methods
  // ---------------------------------------------------------------------//

  /**
   * Get the data transfer authenticated token from the request context. If it's not in the context,
   * get a token by authenticating.
   *
   * @param dataTransferType The data transfer type.
   * @param configurationId The data management configuration ID.
   * @return A data transfer authenticated token.
   * @throws HpcException If it failed to obtain an authentication token.
   */
  private Object getAuthenticatedToken(HpcDataTransferType dataTransferType, String configurationId)
      throws HpcException {
    HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
    if (invoker == null) {
      throw new HpcException("Unknown user", HpcErrorType.UNEXPECTED_ERROR);
    }

    // Search for an existing token.
    for (HpcDataTransferAuthenticatedToken authenticatedToken :
        invoker.getDataTransferAuthenticatedTokens()) {
      if (authenticatedToken.getDataTransferType().equals(dataTransferType)
          && authenticatedToken.getConfigurationId().equals(configurationId)) {
        return authenticatedToken.getDataTransferAuthenticatedToken();
      }
    }

    // No authenticated token found for this request. Create one.
    HpcIntegratedSystemAccount dataTransferSystemAccount =
        systemAccountLocator.getSystemAccount(dataTransferType, invoker.getNciAccount().getDoc());
    HpcRequestContext.setRequestIntegratedSysAcct(dataTransferSystemAccount);
//    HpcIntegratedSystemAccount dataTransferSystemAccount =
//        systemAccountLocator.getSystemAccount(dataTransferType);

    if (dataTransferSystemAccount == null) {
      throw new HpcException(
          "System account not registered for " + dataTransferType.value(),
          HpcErrorType.UNEXPECTED_ERROR);
    }

    // Authenticate with the data transfer system.
    Object token =
        dataTransferProxies
            .get(dataTransferType)
            .authenticate(
                dataTransferSystemAccount,
                dataManagementConfigurationLocator
                    .getDataTransferConfiguration(configurationId, dataTransferType)
                    .getUrl());
    if (token == null) {
      throw new HpcException(
          "Invalid data transfer account credentials",
          HpcErrorType.DATA_TRANSFER_ERROR,
          dataTransferSystemAccount.getIntegratedSystem());
    }

    // Store token on the request context.
    HpcDataTransferAuthenticatedToken authenticatedToken = new HpcDataTransferAuthenticatedToken();
    authenticatedToken.setDataTransferAuthenticatedToken(token);
    authenticatedToken.setDataTransferType(dataTransferType);
    authenticatedToken.setConfigurationId(configurationId);
    invoker.getDataTransferAuthenticatedTokens().add(authenticatedToken);
    HpcRequestContext.setRequestInvoker(invoker);

    return token;
  }

  /**
   * Generate metadata to attach to the data object in the archive: 1. UUID - the data object UUID
   * in the DM system (iRODS) 2. User ID - the user id that registered the data object.
   *
   * @param objectId The data object UUID.
   * @param registrarId The user-id uploaded the data.
   * @return a List of the 2 metadata.
   */
  private List<HpcMetadataEntry> generateMetadata(String objectId, String registrarId) {
    List<HpcMetadataEntry> metadataEntries = new ArrayList<>();

    // Create the user-id metadata.
    HpcMetadataEntry entry = new HpcMetadataEntry();
    entry.setAttribute(REGISTRAR_ID_ATTRIBUTE);
    entry.setValue(registrarId);
    metadataEntries.add(entry);

    // Create the path metadata.
    entry = new HpcMetadataEntry();
    entry.setAttribute(OBJECT_ID_ATTRIBUTE);
    entry.setValue(objectId);
    metadataEntries.add(entry);

    return metadataEntries;
  }

  /**
   * Create an empty file.
   *
   * @param filePath The file's path
   * @return The created file.
   * @throws HpcException on service failure.
   */
  private File createFile(String filePath) throws HpcException {
    File file = new File(filePath);
    try {
      FileUtils.touch(file);

    } catch (IOException e) {
      throw new HpcException(
          "Failed to create a file: " + filePath, HpcErrorType.DATA_TRANSFER_ERROR, e);
    }

    return file;
  }

  /**
   * Create an empty file placed in the download folder.
   *
   * @return The created file.
   * @throws HpcException on service failure.
   */
  private File createDownloadFile() throws HpcException {
    return createFile(downloadDirectory + "/" + UUID.randomUUID().toString());
  }

  /**
   * Upload a data object file.
   *
   * @param uploadRequest The data upload request.
   * @param configurationId The data management configuration ID.
   * @return A data object upload response.
   * @throws HpcException on service failure.
   */
  private HpcDataObjectUploadResponse uploadDataObject(
      HpcDataObjectUploadRequest uploadRequest, String configurationId) throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(uploadRequest.getPath())) {
      throw new HpcException("Null/Empty data object path", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Determine the data transfer type to use in this upload request (i.e. Globus or S3).
    HpcDataTransferType dataTransferType =
        getUploadDataTransferType(uploadRequest, configurationId);

    // Validate source location exists and accessible.
    validateUploadSourceFileLocation(
        dataTransferType, uploadRequest.getSourceLocation(), configurationId);

    // Check that the data transfer system can accept transfer requests.
    Object authenticatedToken = getAuthenticatedToken(dataTransferType, configurationId);
    if (!dataTransferProxies.get(dataTransferType).acceptsTransferRequests(authenticatedToken)) {
      // The data transfer system is busy. Queue the request (upload status set to
      // 'RECEIVED'),
      // and the upload will be performed later by a scheduled task.
      HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
      uploadResponse.setDataTransferType(dataTransferType);
      uploadResponse.setDataTransferStarted(Calendar.getInstance());
      uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.RECEIVED);

      releaseSystemAccountInRequestContext();

      return uploadResponse;
    }

    // Get the data transfer configuration.
    HpcDataTransferConfiguration dataTransferConfiguration =
        dataManagementConfigurationLocator.getDataTransferConfiguration(
            configurationId, dataTransferType);

    // Upload the data object using the appropriate data transfer system proxy.
    final HpcDataObjectUploadResponse retUploadResponse =
        dataTransferProxies
        .get(dataTransferType)
        .uploadDataObject(
            authenticatedToken,
            uploadRequest,
            dataTransferConfiguration.getBaseArchiveDestination(),
            dataTransferConfiguration.getUploadRequestURLExpiration(),
            null);

    releaseSystemAccountInRequestContext();

    return retUploadResponse;
  }

  /**
   * Validate upload source file location.
   *
   * @param dataTransferType The data transfer type.
   * @param sourceFileLocation The file location to validate. If null, no validation is performed.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *     config).
   * @throws HpcException if the upload source location doesn't exist, or not accessible, or it's a
   *     directory.
   */
  private void validateUploadSourceFileLocation(
      HpcDataTransferType dataTransferType,
      HpcFileLocation sourceFileLocation,
      String configurationId)
      throws HpcException {

    if (sourceFileLocation == null) {
      return;
    }

    HpcPathAttributes pathAttributes =
        getPathAttributes(dataTransferType, sourceFileLocation, false, configurationId);

    // Validate source file accessible
    if (!pathAttributes.getIsAccessible()) {
      throw new HpcException(
          "Source file location not accessible: "
              + sourceFileLocation.getFileContainerId()
              + ":"
              + sourceFileLocation.getFileId(),
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate source file exists.
    if (!pathAttributes.getExists()) {
      throw new HpcException(
          "Source file location doesn't exist: "
              + sourceFileLocation.getFileContainerId()
              + ":"
              + sourceFileLocation.getFileId(),
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate source file is not a directory.
    if (pathAttributes.getIsDirectory()) {
      throw new HpcException(
          "Source file location is a directory: "
              + sourceFileLocation.getFileContainerId()
              + ":"
              + sourceFileLocation.getFileId(),
          HpcErrorType.INVALID_REQUEST_INPUT);
    }
  }

  /**
   * Validate download destination file location.
   *
   * @param dataTransferType The data transfer type.
   * @param destinationLocation The file location to validate.
   * @param validateExistsAsDirectory If true, an exception will thrown if the path is an existing
   *     directory.
   * @param validateExistsAsFile If true, an exception will thrown if the path is an existing file.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *     config).
   * @return The path attributes.
   * @throws HpcException if the destination location not accessible or exist as a file.
   */
  private HpcPathAttributes validateDownloadDestinationFileLocation(
      HpcDataTransferType dataTransferType,
      HpcFileLocation destinationLocation,
      boolean validateExistsAsDirectory,
      boolean validateExistsAsFile,
      String configurationId)
      throws HpcException {
    HpcPathAttributes pathAttributes =
        getPathAttributes(dataTransferType, destinationLocation, false, configurationId);

    // Validate destination file accessible.
    if (!pathAttributes.getIsAccessible()) {
      throw new HpcException(
          "Destination file location not accessible: "
              + destinationLocation.getFileContainerId()
              + ":"
              + destinationLocation.getFileId(),
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate destination file doesn't exist as a file.
    if (validateExistsAsFile && pathAttributes.getIsFile()) {
      throw new HpcException(
          "A file already exists with the same destination path: "
              + destinationLocation.getFileContainerId()
              + ":"
              + destinationLocation.getFileId(),
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate destination file doesn't exist as a directory.
    if (validateExistsAsDirectory && pathAttributes.getIsDirectory()) {
      throw new HpcException(
          "A directory already exists with the same destination path: "
              + destinationLocation.getFileContainerId()
              + ":"
              + destinationLocation.getFileId(),
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    return pathAttributes;
  }

  /**
   * Filter scan items based on include/exclude patterns.
   *
   * @param scanItems The list of items to filter (items not matched will be removed from this
   *     list).
   * @param includePatterns The include patterns.
   * @param excludePatterns The exclude patterns.
   * @param patternType The type of patterns provided.
   * @throws HpcException on service failure
   */
  private void filterScanItems(
      List<HpcDirectoryScanItem> scanItems,
      List<String> includePatterns,
      List<String> excludePatterns,
      HpcDirectoryScanPatternType patternType)
      throws HpcException {
    if (includePatterns.isEmpty() && excludePatterns.isEmpty()) {
      // No patterns provided.
      return;
    }

    // Validate pattern is provided.
    if (patternType == null) {
      throw new HpcException(
          "Null directory scan pattern type", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Compile include regex expressions.
    List<Pattern> includeRegex = new ArrayList<>();
    includePatterns.forEach(
        pattern ->
            includeRegex.add(
                Pattern.compile(
                    patternType.equals(HpcDirectoryScanPatternType.SIMPLE)
                        ? toRegex(pattern)
                        : pattern)));

    // Compile exclude regex expressions.
    List<Pattern> excludeRegex = new ArrayList<>();
    excludePatterns.forEach(
        pattern ->
            excludeRegex.add(
                Pattern.compile(
                    patternType.equals(HpcDirectoryScanPatternType.SIMPLE)
                        ? toRegex(pattern)
                        : pattern)));

    // Match the items against the patterns.
    ListIterator<HpcDirectoryScanItem> iter = scanItems.listIterator();
    while (iter.hasNext()) {
      // Get the path of this data object registration item.
      String path = iter.next().getFilePath();

      // Match the patterns.
      if (!((includeRegex.isEmpty() || matches(includeRegex, path))
          && (excludeRegex.isEmpty() || !matches(excludeRegex, path)))) {
        iter.remove();
      }
    }
  }

  /**
   * Regex matching on a list of patterns.
   *
   * @param patterns A list of patterns to match.
   * @param input The string to match.
   * @return true if the input matched at least one of the patterns, or false otherwise.
   */
  private boolean matches(List<Pattern> patterns, String input) {
    for (Pattern pattern : patterns) {
      if (pattern.matcher(input).matches()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Convert pattern from SIMPLE to REGEX.
   *
   * @param pattern The SIMPLE pattern.
   * @return The REGEX pattern.
   */
  private String toRegex(String pattern) {
    // Ensure the pattern starts with '/'.
    String regex = !pattern.startsWith("/") ? "/" + pattern : pattern;

    // Convert the '**' to regex.
    regex = regex.replaceAll(Pattern.quote("**"), ".*");

    // Convert the '*' to regex.
    regex = regex.replaceAll("([^\\.])\\*", "$1[^/]*");

    // Convert the '?' to regex.
    return regex.replaceAll(Pattern.quote("?"), ".");
  }

  /**
   * Get a file path for a given file ID and baseArchive.
   *
   * @param fileId The file ID.
   * @param baseArchive The base archive.
   * @return a file path.
   */
  private String getFilePath(String fileId, HpcArchive baseArchive) {
    return fileId.replaceFirst(
        baseArchive.getFileLocation().getFileId(), baseArchive.getDirectory());
  }

  /**
   * Determine the data transfer type to use on an upload request.
   *
   * @param uploadRequest The upload request to determine the data transfer type to use.
   * @param configurationId The data management configuration ID.
   * @return The appropriate data transfer type to use in this upload request.
   * @throws HpcException on service failure.
   */
  private HpcDataTransferType getUploadDataTransferType(
      HpcDataObjectUploadRequest uploadRequest, String configurationId) throws HpcException {
    // Determine the data transfer type to use in this upload request (i.e. Globus or S3).
    HpcDataTransferType archiveDataTransferType =
        dataManagementConfigurationLocator.getArchiveDataTransferType(configurationId);
    if (uploadRequest.getSourceLocation() != null) {
      // It's an asynchronous upload request - always performed with Globus.
      return HpcDataTransferType.GLOBUS;
    }
    if (uploadRequest.getSourceFile() != null) {
      // It's a synchrnous upload request - use the configured archive (S3/Cleversafe or Globus/Isilon).
      return archiveDataTransferType;

    } else if (uploadRequest.getGenerateUploadRequestURL()) {
      // It's a request to generate upload URL - Only supported by S3.
      if (archiveDataTransferType.equals(HpcDataTransferType.GLOBUS)) {
        throw new HpcException(
            "Generate upload URL not supported by " + HpcDataTransferType.GLOBUS.value(),
            HpcErrorType.INVALID_REQUEST_INPUT);
      }
      return HpcDataTransferType.S_3;

    } else {
      // Could not determine data transfer type.
      throw new HpcException(
          "Could not determine data transfer type", HpcErrorType.UNEXPECTED_ERROR);
    }
  }

  /**
   * Calculate and validate download destination location.
   *
   * @param destinationLocation The destination location requested by the caller.
   * @param destinationOverwrite If true, the requested destination location will be overwritten if
   *     it exists.
   * @param dataTransferType The data transfer type to create the request.
   * @param sourcePath The source path.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *     config).
   * @return The calculated destination file location. The source file name is added if the caller
   *     provided a directory destination.
   * @throws HpcException on service failure.
   */
  private HpcFileLocation calculateDownloadDestinationFileLocation(
      HpcFileLocation destinationLocation,
      boolean destinationOverwrite,
      HpcDataTransferType dataTransferType,
      String sourcePath,
      String configurationId)
      throws HpcException {
    // Validate the download destination location.
    HpcPathAttributes pathAttributes =
        validateDownloadDestinationFileLocation(
            dataTransferType, destinationLocation, false, !destinationOverwrite, configurationId);

    // Calculate the destination.
    if (pathAttributes.getIsDirectory()) {
      // Caller requested to download to a directory. Append the source file name.
      HpcFileLocation calcDestination = new HpcFileLocation();
      calcDestination.setFileContainerId(destinationLocation.getFileContainerId());
      calcDestination.setFileId(
          destinationLocation.getFileId() + sourcePath.substring(sourcePath.lastIndexOf('/')));

      // Validate the calculated download destination.
      validateDownloadDestinationFileLocation(
          dataTransferType, calcDestination, true, !destinationOverwrite, configurationId);

      return calcDestination;

    } else {
      return destinationLocation;
    }
  }

  // Second hop download.
  private class HpcSecondHopDownload implements HpcDataTransferProgressListener {
    // ---------------------------------------------------------------------//
    // Instance members
    // ---------------------------------------------------------------------//

    // The second hop download request.
    HpcDataObjectDownloadRequest secondHopDownloadRequest = null;

    // A download data object task (keeps track of the async 2-hop download
    // end-to-end.
    HpcDataObjectDownloadTask downloadTask = new HpcDataObjectDownloadTask();

    // The second hop download's source file.
    File sourceFile = null;

    // The data object path.
    String path = null;

    // ---------------------------------------------------------------------//
    // Constructors
    // ---------------------------------------------------------------------//

    public HpcSecondHopDownload(HpcDataObjectDownloadRequest firstHopDownloadRequest)
        throws HpcException {
      // Get the service invoker.
      HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
      if (invoker == null) {
        throw new HpcException("Unknown service invoker", HpcErrorType.UNEXPECTED_ERROR);
      }
      path = firstHopDownloadRequest.getPath();

      // Create the 2nd hop download request.
      secondHopDownloadRequest =
          toSecondHopDownloadRequest(
              calculateDownloadDestinationFileLocation(
                  firstHopDownloadRequest.getDestinationLocation(),
                  firstHopDownloadRequest.getDestinationOverwrite(),
                  HpcDataTransferType.GLOBUS,
                  firstHopDownloadRequest.getArchiveLocation().getFileId(),
                  firstHopDownloadRequest.getConfigurationId()),
              HpcDataTransferType.GLOBUS,
              firstHopDownloadRequest.getPath(),
              firstHopDownloadRequest.getConfigurationId(),
              firstHopDownloadRequest.getUserId(),
              firstHopDownloadRequest.getCompletionEvent(),
              firstHopDownloadRequest.getSize());

      // Get the data transfer configuration.
      HpcDataTransferConfiguration dataTransferConfiguration =
          dataManagementConfigurationLocator.getDataTransferConfiguration(
              firstHopDownloadRequest.getConfigurationId(), HpcDataTransferType.GLOBUS);

      // Create the source file for the second hop download.
      sourceFile =
          createFile(
              getFilePath(
                  secondHopDownloadRequest.getArchiveLocation().getFileId(),
                  dataTransferConfiguration.getBaseDownloadSource()));

      // Create an persist a download task. This object tracks the download request
      // through the 2-hop async
      // download requests.
      createDownloadTask();
    }

    // ---------------------------------------------------------------------//
    // Methods
    // ---------------------------------------------------------------------//

    /**
     * Get the second hop download source file.
     *
     * @return The second hop source file.
     */
    public File getSourceFile() {
      return sourceFile;
    }

    /**
     * Return the download task associated with this 2nd hop download.
     *
     * @return The 2nd hop download task.
     */
    public HpcDataObjectDownloadTask getDownloadTask() {
      return downloadTask;
    }

    // ---------------------------------------------------------------------//
    // HpcDataTransferProgressListener Interface Implementation
    // ---------------------------------------------------------------------//

    @Override
    public void transferCompleted() {
      // This callback method is called when the first hop (S3) download completed.

      try {
        // Update the download task to reflect 1st hop transfer completed.
        downloadTask.setDataTransferType(secondHopDownloadRequest.getDataTransferType());
        downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.RECEIVED);

        // Persist the download task.
        dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);

      } catch (HpcException e) {
        logger.error("Failed to update download task", e);
        transferFailed(e.getMessage());
      }
    }

    // This callback method is called when the first hop download failed.
    @Override
    public void transferFailed() {
      transferFailed("Failed to get data from archive via S3");
    }

    // ---------------------------------------------------------------------//
    // Helper Methods
    // ---------------------------------------------------------------------//

    /**
     * Create a download request for a 2nd hop download from local file (as Globus endpoint) to
     * caller's Globus endpoint destination.
     *
     * @param destinationLocation The caller's destination.
     * @param dataTransferType The data transfer type to create the request
     * @param path The data object logical path.
     * @param configurationId The data management configuration ID..
     * @param userId The user ID submitting the request.
     * @param completionEvent If true, an event will be added when async download is complete.
     * @param size The data object size in bytes.
     * @return Data object download request.
     * @throws HpcException If it failed to obtain an authentication token.
     */
    private HpcDataObjectDownloadRequest toSecondHopDownloadRequest(
        HpcFileLocation destinationLocation,
        HpcDataTransferType dataTransferType,
        String path,
        String configurationId,
        String userId,
        boolean completionEvent,
        long size)
        throws HpcException {
      // Create and return a download request, from the local GLOBUS endpoint, to the caller's destination.
      HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
      downloadRequest.setArchiveLocation(
          getDownloadSourceLocation(configurationId, dataTransferType));
      downloadRequest.setDestinationLocation(destinationLocation);
      downloadRequest.setDataTransferType(dataTransferType);
      downloadRequest.setPath(path);
      downloadRequest.setConfigurationId(configurationId);
      downloadRequest.setUserId(userId);
      downloadRequest.setCompletionEvent(completionEvent);
      downloadRequest.setSize(size);
      return downloadRequest;
    }

    /**
     * Create a download task for a 2-hop download.
     *
     * @throws HpcException If it failed to persist the task.
     */
    private void createDownloadTask() throws HpcException {
      downloadTask.setDataTransferType(HpcDataTransferType.S_3);
      downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.IN_PROGRESS);
      downloadTask.setDownloadFilePath(sourceFile.getAbsolutePath());
      downloadTask.setUserId(secondHopDownloadRequest.getUserId());
      downloadTask.setPath(secondHopDownloadRequest.getPath());
      downloadTask.setConfigurationId(secondHopDownloadRequest.getConfigurationId());
      downloadTask.setCompletionEvent(secondHopDownloadRequest.getCompletionEvent());
      downloadTask.setArchiveLocation(secondHopDownloadRequest.getArchiveLocation());
      downloadTask.setDestinationLocation(secondHopDownloadRequest.getDestinationLocation());
      downloadTask.setCreated(Calendar.getInstance());
      downloadTask.setPercentComplete(0);
      downloadTask.setSize(secondHopDownloadRequest.getSize());

      dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
    }

    /**
     * Handle the case when transfer failed. Send a download failed event and cleanup the download
     * task.
     *
     * @param message The message to include in the download failed event.
     */
    private void transferFailed(String message) {
      Calendar transferFailedTimestamp = Calendar.getInstance();
      try {
        // Record a download failed event if requested to.
        if (secondHopDownloadRequest.getCompletionEvent()) {
          eventService.addDataTransferDownloadFailedEvent(
              secondHopDownloadRequest.getUserId(),
              path,
              HpcDownloadTaskType.DATA_OBJECT,
              downloadTask.getId(),
              secondHopDownloadRequest.getDestinationLocation(),
              transferFailedTimestamp,
              message);
        }

      } catch (HpcException e) {
        logger.error("Failed to add data transfer download failed event", e);
      }

      // Cleanup the download task.
      try {
        completeDataObjectDownloadTask(downloadTask, false, message, transferFailedTimestamp, 0);

      } catch (HpcException ex) {
        logger.error("Failed to cleanup download task", ex);
      }
    }

    /**
     * Get download source location.
     *
     * @param configurationId The data management configuration ID.
     * @param dataTransferType The data transfer type.
     * @return The download source location.
     * @throws HpcException on data transfer system failure.
     */
    private HpcFileLocation getDownloadSourceLocation(
        String configurationId, HpcDataTransferType dataTransferType) throws HpcException {
      // Get the data transfer configuration.
      HpcArchive baseDownloadSource =
          dataManagementConfigurationLocator
              .getDataTransferConfiguration(configurationId, dataTransferType)
              .getBaseDownloadSource();

      // Create a source location. (This is a local GLOBUS endpoint).
      HpcFileLocation sourceLocation = new HpcFileLocation();
      sourceLocation.setFileContainerId(baseDownloadSource.getFileLocation().getFileContainerId());
      sourceLocation.setFileId(
          baseDownloadSource.getFileLocation().getFileId() + "/" + UUID.randomUUID().toString());

      return sourceLocation;
    }
  }

  /*
     Release system account from the HpcRequestContext, if applicable.

     This is originally intended for returning system account credentials for Globus representing
     a pooled app account.  The credentials are returned to system account locator component so
     that it can recycle them for future usage.
   */
  private void releaseSystemAccountInRequestContext() {
    final HpcIntegratedSystemAccount sysAcct = HpcRequestContext.getRequestIntegratedSysAcct();
    if (null != sysAcct) {
      systemAccountLocator.returnSharedSystemAccount(sysAcct);
      HpcRequestContext.setRequestIntegratedSysAcct(null);
    }
  }

/*
  private HpcDataTransferType rerouteGlobus2P_Globus(HpcDataTransferType dtType) {
    final HpcDataTransferType retDtType =
        (HpcDataTransferType.GLOBUS.equals(dtType)) ? HpcDataTransferType.P_GLOBUS : dtType;
    return retDtType;
  }
*/

}
