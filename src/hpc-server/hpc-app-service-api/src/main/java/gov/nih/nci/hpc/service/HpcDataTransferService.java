/**
 * HpcDataTransferService.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanPatternType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcGoogleDriveDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3UploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcUserDownloadRequest;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Transfer Service Interface.
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 */
public interface HpcDataTransferService {
  /**
   * Upload a data object. Caller must provide one of the following 1. A source file. 2. A request
   * to generate upload URL. 3. A Globus source location. 4. An AWS S3 source location.
   *
   * @param globusUploadSource (Optional) The Globus upload source.
   * @param s3UploadSource (Optional) The S3 upload source.
   * @param sourceFile (Optional) The source file.
   * @param generateUploadRequestURL Generate an upload URL (so caller can directly upload file into
   *        archive).
   * @param uploadRequestURLChecksum A checksum provided by the caller to be attached to the upload
   *        request URL.
   * @param path The data object registration path.
   * @param userId The user-id who requested the data upload.
   * @param callerObjectId The caller's provided data object ID.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *        config).
   * @return A data object upload response.
   * @throws HpcException on service failure.
   */
  public HpcDataObjectUploadResponse uploadDataObject(HpcGlobusUploadSource globusUploadSource,
      HpcS3UploadSource s3UploadSource, File sourceFile, boolean generateUploadRequestURL,
      String uploadRequestURLChecksum, String path, String userId, String callerObjectId,
      String configurationId) throws HpcException;

  /**
   * Download a data object file.
   *
   * @param path The data object path.
   * @param archiveLocation The archive file location.
   * @param globusDownloadDestination The user requested Glopbus download destination.
   * @param s3DownloadDestination The user requested S3 download destination.
   * @param googleDriveDownloadDestination The user requested Google Drive download destination.
   * @param archiveDataTransferType The data transfer type used to upload the data object into the
   *        archive
   * @param configurationId The configuration ID (needed to determine the archive connection
   *        config).
   * @param userId The user ID submitting the download request.
   * @param completionEvent If true, an event will be added when async download is complete.
   * @param size The data object's size in bytes.
   * @return A data object download response.
   * @throws HpcException on service failure.
   */
  public HpcDataObjectDownloadResponse downloadDataObject(String path,
      HpcFileLocation archiveLocation, HpcGlobusDownloadDestination globusDownloadDestination,
      HpcS3DownloadDestination s3DownloadDestination,
      HpcGoogleDriveDownloadDestination googleDriveDownloadDestination,
      HpcDataTransferType archiveDataTransferType, String configurationId, String userId,
      boolean completionEvent, long size) throws HpcException;

  /**
   * Generate a (pre-signed) download URL for a data object file.
   *
   * @param path The data object path.
   * @param archiveLocation The archive file location.
   * @param dataTransferType The data transfer type.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *        config).
   * @return The download URL.
   * @throws HpcException on service failure.
   */
  public String generateDownloadRequestURL(String path, HpcFileLocation archiveLocation,
      HpcDataTransferType dataTransferType, String configurationId) throws HpcException;

  /**
   * Add system generated metadata to the data object in the archive.
   *
   * @param archiveFileLocation The file location in the archive.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *        config).
   * @param objectId The data object id from the data management system (UUID).
   * @param registrarId The user-id of the data registrar.
   * @param archiveDataTransferType The data transfer type used to upload the file.
   * @return The checksum of the data object object.
   * @throws HpcException on service failure.
   */
  public String addSystemGeneratedMetadataToDataObject(HpcFileLocation archiveFileLocation,
      String configurationId, String objectId, String registrarId,
      HpcDataTransferType archiveDataTransferType) throws HpcException;

  /**
   * Delete a data object file.
   *
   * @param fileLocation The file location.
   * @param dataTransferType The data transfer type.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *        config).
   * @throws HpcException on service failure.
   */
  public void deleteDataObject(HpcFileLocation fileLocation, HpcDataTransferType dataTransferType,
      String configurationId) throws HpcException;

  /**
   * Get a data transfer upload request status.
   *
   * @param dataTransferType The data transfer type.
   * @param dataTransferRequestId The data transfer request ID.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *        config).
   * @return The data transfer upload request status.
   * @throws HpcException on service failure.
   */
  public HpcDataTransferUploadReport getDataTransferUploadStatus(
      HpcDataTransferType dataTransferType, String dataTransferRequestId, String configurationId)
      throws HpcException;

  /**
   * Get a data transfer download request status.
   *
   * @param dataTransferType The data transfer type.
   * @param dataTransferRequestId The data transfer request ID.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *        config).
   * @return The data transfer download request status.
   * @throws HpcException on service failure.
   */
  public HpcDataTransferDownloadReport getDataTransferDownloadStatus(
      HpcDataTransferType dataTransferType, String dataTransferRequestId, String configurationId)
      throws HpcException;

  /**
   * Get path attributes for a given file in Globus or Cleversafe (using system account).
   *
   * @param dataTransferType The data transfer type.
   * @param fileLocation The endpoint/path to get attributes for.
   * @param getSize If set to true, the file/directory size will be returned.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *        config).
   * @return The path attributes.
   * @throws HpcException on service failure.
   */
  public HpcPathAttributes getPathAttributes(HpcDataTransferType dataTransferType,
      HpcFileLocation fileLocation, boolean getSize, String configurationId) throws HpcException;

  /**
   * Get path attributes for a given file in AWS S3 (using user provided S3 account).
   *
   * @param s3Account The user provided S3 account.
   * @param fileLocation The bucket/object-id to get attributes for.
   * @param getSize If set to true, the file/directory size will be returned.
   * @return The path attributes.
   * @throws HpcException on service failure.
   */
  public HpcPathAttributes getPathAttributes(HpcS3Account s3Account, HpcFileLocation fileLocation,
      boolean getSize) throws HpcException;

  /**
   * Scan a directory (recursively) and return a list of all its files.
   *
   * @param dataTransferType The data transfer type.
   * @param s3Account (Optional) S3 account to use. If null, then system account for the data
   *        transfer type is used.
   * @param directoryLocation The endpoint/directory to scan and get a list of files for.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *        config).
   * @param includePatterns The patterns to use to include files in the scan results.
   * @param excludePatterns The patterns to use to exclude files from the scan results.
   * @param patternType The type of the patterns provided.
   * @return A list of files found.
   * @throws HpcException on service failure.
   */
  public List<HpcDirectoryScanItem> scanDirectory(HpcDataTransferType dataTransferType,
      HpcS3Account s3Account, HpcFileLocation directoryLocation, String configurationId,
      List<String> includePatterns, List<String> excludePatterns,
      HpcDirectoryScanPatternType patternType) throws HpcException;

  /**
   * Get a file from the archive.
   *
   * @param configurationId The data management configuration ID.
   * @param dataTransferType The data transfer type.
   * @param fileId The file ID.
   * @return The requested file from the archive.
   * @throws HpcException on service failure.
   */
  public File getArchiveFile(String configurationId, HpcDataTransferType dataTransferType,
      String fileId) throws HpcException;

  /**
   * Get all (active) data object download tasks.
   *
   * @return A list of data object download tasks.
   * @throws HpcException on service failure.
   */
  public List<HpcDataObjectDownloadTask> getDataObjectDownloadTasks() throws HpcException;

  /**
   * Get download task status.
   *
   * @param taskId The download task ID.
   * @param taskType The download task type (data-object or collection).
   * @return A download status object, or null if the task can't be found. Note: The returned object
   *         is associated with a 'task' object if the download is in-progress. If the download
   *         completed or failed, the returned object is associated with a 'result' object.
   * @throws HpcException on service failure.
   */
  public HpcDownloadTaskStatus getDownloadTaskStatus(String taskId, HpcDownloadTaskType taskType)
      throws HpcException;

  /**
   * Complete a data object download task: 1. Cleanup any files staged in the file system for
   * download. 2. Update task info in DB with results info.
   *
   * @param downloadTask The download task to complete.
   * @param result The result of the task (true is successful, false is failed).
   * @param message (Optional) If the task failed, a message describing the failure.
   * @param completed (Optional) The download task completion timestamp.
   * @param bytesTransferred The total bytes transfered.
   * @throws HpcException on service failure.
   */
  public void completeDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask, boolean result,
      String message, Calendar completed, long bytesTransferred) throws HpcException;

  /**
   * Continue a data object download task that was queued.
   *
   * @param downloadTask The download task to continue.
   * @throws HpcException on service failure.
   */
  public void continueDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask)
      throws HpcException;

  /**
   * Reset a data object download task. Set it's status to RECEIVED.
   *
   * @param downloadTask The download task to reset.
   * @throws HpcException on service failure.
   */
  public void resetDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask)
      throws HpcException;

  /**
   * Update a data object download task. % Complete is calculated and any change on the task object
   * will be persisted.
   *
   * @param downloadTask The download task to update progress
   * @param bytesTransferred The bytes transferred so far.
   * @throws HpcException on service failure.
   */
  public void updateDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask,
      long bytesTransferred) throws HpcException;

  /**
   * Submit a request to download a collection.
   *
   * @param path The collection path.
   * @param globusDownloadDestination The user requested Glopbus download destination.
   * @param s3DownloadDestination The user requested S3 download destination.
   * @param userId The user ID submitting the download request.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *        config).
   * @return The submitted collection download task.
   * @throws HpcException on service failure.
   */
  public HpcCollectionDownloadTask downloadCollection(String path,
      HpcGlobusDownloadDestination globusDownloadDestination,
      HpcS3DownloadDestination s3DownloadDestination, String userId, String configurationId)
      throws HpcException;

  /**
   * Submit a request to download data objects.
   *
   * @param dataObjectPathsMap A map of data-object-path to its configuration ID.
   * @param globusDownloadDestination The user requested Glopbus download destination.
   * @param s3DownloadDestination The user requested S3 download destination.
   * @param userId The user ID submitting the download request.
   * @return The submitted request download task.
   * @throws HpcException on service failure.
   */
  public HpcCollectionDownloadTask downloadDataObjects(Map<String, String> dataObjectPathsMap,
      HpcGlobusDownloadDestination globusDownloadDestination,
      HpcS3DownloadDestination s3DownloadDestination, String userId) throws HpcException;

  /**
   * Update a collection download task.
   *
   * @param downloadTask The collection download task to update.
   * @throws HpcException on service failure.
   */
  public void updateCollectionDownloadTask(HpcCollectionDownloadTask downloadTask)
      throws HpcException;

  /**
   * Get collection download tasks.
   *
   * @param status Get tasks in this status.
   * @return A list of collection download tasks.
   * @throws HpcException on database error.
   */
  public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(
      HpcCollectionDownloadTaskStatus status) throws HpcException;

  /**
   * Complete a collection download task: 1. Update task info in DB with results info.
   *
   * @param downloadTask The download task to complete.
   * @param result The result of the task (true is successful, false is failed).
   * @param message (Optional) If the task failed, a message describing the failure.
   * @param completed (Optional) The download task completion timestamp.
   * @throws HpcException on service failure.
   */
  public void completeCollectionDownloadTask(HpcCollectionDownloadTask downloadTask, boolean result,
      String message, Calendar completed) throws HpcException;

  /**
   * Get active download requests for a user.
   *
   * @param userId The user ID to query for.
   * @return A list of active download requests.
   * @throws HpcException on service failure.
   */
  public List<HpcUserDownloadRequest> getDownloadRequests(String userId) throws HpcException;

  /**
   * Get download results (all completed download requests) for a user.
   *
   * @param userId The user ID to query for.
   * @param page The requested results page.
   * @return A list of completed download requests.
   * @throws HpcException on service failure.
   */
  public List<HpcUserDownloadRequest> getDownloadResults(String userId, int page)
      throws HpcException;

  /**
   * Get download results (all completed download requests) count for a user.
   *
   * @param userId The user ID to query for.
   * @return A total count of completed download requests.
   * @throws HpcException on service failure.
   */
  public int getDownloadResultsCount(String userId) throws HpcException;

  /**
   * Get the download results page size.
   *
   * @return The download results page size.
   */
  public int getDownloadResultsPageSize();

  /**
   * Get a file-container0name from a file-container-id
   *
   * @param dataTransferType The data transfer type.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *        config).
   * @param fileContainerId The file container ID.
   * @return The file container name of the provider id.
   * @throws HpcException on data transfer system failure.
   */
  public String getFileContainerName(HpcDataTransferType dataTransferType, String configurationId,
      String fileContainerId) throws HpcException;

  /**
   * Calculate a data object upload % complete. Note: if upload not in progress, null is returned.
   *
   * @param systemGeneratedMetadata The system generated metadata of the data object.
   * @return The transfer % completion if transfer is in progress, or null otherwise.
   */
  public Integer calculateDataObjectUploadPercentComplete(
      HpcSystemGeneratedMetadata systemGeneratedMetadata);
}
