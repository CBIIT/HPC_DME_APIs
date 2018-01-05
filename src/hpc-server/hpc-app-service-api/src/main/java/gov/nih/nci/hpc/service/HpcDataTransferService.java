/**
 * HpcDataTransferService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
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
import gov.nih.nci.hpc.domain.datatransfer.HpcUserDownloadRequest;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Transfer Service Interface.
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 */
public interface HpcDataTransferService {
  /**
   * Upload data. Either upload from the input stream or submit a transfer request for the source.
   *
   * @param sourceLocation The source for data transfer.
   * @param sourceFile The source file.
   * @param generateUploadRequestURL Generate an upload URL (so caller can directly upload file into
   *     archive).
   * @param uploadRequestURLChecksum A checksum provided by the caller to be attached to the upload
   *     request URL.
   * @param path The data object registration path.
   * @param userId The user-id who requested the data upload.
   * @param callerObjectId The caller's provided data object ID.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *     config).
   * @return A data object upload response.
   * @throws HpcException on service failure.
   */
  public HpcDataObjectUploadResponse uploadDataObject(
      HpcFileLocation sourceLocation,
      File sourceFile,
      boolean generateUploadRequestURL,
      String uploadRequestURLChecksum,
      String path,
      String userId,
      String callerObjectId,
      String configurationId)
      throws HpcException;

  /**
   * Download a data object file.
   *
   * @param path The data object path.
   * @param archiveLocation The archive file location.
   * @param destinationLocation The user requested file destination.
   * @param generateDownloadRequestURL If true, S3 presigned URL will be generated to download.
   * @param destinationOverwrite If true, the requested destination location will be overwritten if
   *     it exists.
   * @param dataTransferType The data transfer type.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *     config).
   * @param userId The user ID submitting the download request.
   * @param completionEvent If true, an event will be added when async download is complete.
   * @return A data object download response.
   * @throws HpcException on service failure.
   */
  public HpcDataObjectDownloadResponse downloadDataObject(
      String path,
      HpcFileLocation archiveLocation,
      HpcFileLocation destinationLocation,
      boolean generateDownloadRequestURL,
      boolean destinationOverwrite,
      HpcDataTransferType dataTransferType,
      String configurationId,
      String userId,
      boolean completionEvent)
      throws HpcException;

  /**
   * Add system generated metadata to the data object in the archive.
   *
   * @param fileLocation The file location.
   * @param dataTransferType The data transfer type.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *     config).
   * @param objectId The data object id from the data management system (UUID).
   * @param registrarId The user-id of the data registrar.
   * @return The checksum of the data object object.
   * @throws HpcException on service failure.
   */
  public String addSystemGeneratedMetadataToDataObject(
      HpcFileLocation fileLocation,
      HpcDataTransferType dataTransferType,
      String configurationId,
      String objectId,
      String registrarId)
      throws HpcException;

  /**
   * Delete a data object file.
   *
   * @param fileLocation The file location.
   * @param dataTransferType The data transfer type.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *     config).
   * @throws HpcException on service failure.
   */
  public void deleteDataObject(
      HpcFileLocation fileLocation, HpcDataTransferType dataTransferType, String configurationId)
      throws HpcException;

  /**
   * Get a data transfer upload request status.
   *
   * @param dataTransferType The data transfer type.
   * @param dataTransferRequestId The data transfer request ID.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *     config).
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
   *     config).
   * @return The data transfer download request status.
   * @throws HpcException on service failure.
   */
  public HpcDataTransferDownloadReport getDataTransferDownloadStatus(
      HpcDataTransferType dataTransferType, String dataTransferRequestId, String configurationId)
      throws HpcException;

  /**
   * Get the size of the data transferred of a specific request.
   *
   * @param dataTransferType The data transfer type.
   * @param dataTransferRequestId The data transfer request ID.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *     config).
   * @return The size of the data transferred in bytes.
   * @throws HpcException on service failure.
   */
  public long getDataTransferSize(
      HpcDataTransferType dataTransferType, String dataTransferRequestId, String configurationId)
      throws HpcException;

  /**
   * Get endpoint/path attributes.
   *
   * @param dataTransferType The data transfer type.
   * @param fileLocation The endpoint/path to get attributes for.
   * @param getSize If set to true, the file/directory size will be returned.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *     config).
   * @return The path attributes.
   * @throws HpcException on service failure.
   */
  public HpcPathAttributes getPathAttributes(
      HpcDataTransferType dataTransferType,
      HpcFileLocation fileLocation,
      boolean getSize,
      String configurationId)
      throws HpcException;

  /**
   * Scan a directory (recursively) and return a list of all its files.
   *
   * @param dataTransferType The data transfer type.
   * @param directoryLocation The endpoint/directory to scan and get a list of files for.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *     config).
   * @param includePatterns The patterns to use to include files in the scan results.
   * @param excludePatterns The patterns to use to exclude files from the scan results.
   * @param patternType The type of the patterns provided.
   * @return A list of files found.
   * @throws HpcException on service failure.
   */
  public List<HpcDirectoryScanItem> scanDirectory(
      HpcDataTransferType dataTransferType,
      HpcFileLocation directoryLocation,
      String configurationId,
      List<String> includePatterns,
      List<String> excludePatterns,
      HpcDirectoryScanPatternType patternType)
      throws HpcException;

  /**
   * Get a file from the archive.
   *
   * @param configurationId The data management configuration ID.
   * @param dataTransferType The data transfer type.
   * @param fileId The file ID.
   * @return The requested file from the archive.
   * @throws HpcException on service failure.
   */
  public File getArchiveFile(
      String configurationId, HpcDataTransferType dataTransferType, String fileId)
      throws HpcException;

  /**
   * Get all data object download tasks for a given data-transfer type.
   *
   * @param dataTransferType The data-transfer type to query.
   * @return A list of data object download tasks.
   * @throws HpcException on service failure.
   */
  public List<HpcDataObjectDownloadTask> getDataObjectDownloadTasks(
      HpcDataTransferType dataTransferType) throws HpcException;

  /**
   * Get download task status.
   *
   * @param taskId The download task ID.
   * @param taskType The download task type (data-object or collection).
   * @return A download status object, or null if the task can't be found. Note: The returned object
   *     is associated with a 'task' object if the download is in-progress. If the download
   *     completed or failed, the returned object is associated with a 'result' object.
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
  public void completeDataObjectDownloadTask(
      HpcDataObjectDownloadTask downloadTask,
      boolean result,
      String message,
      Calendar completed,
      long bytesTransferred)
      throws HpcException;

  /**
   * Continue a data object download task that was queued. Note: If Globus is still busy, the
   * download task will remain queued.
   *
   * @param downloadTask The download task to submit to Globus.
   * @throws HpcException on service failure.
   */
  public void continueDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask)
      throws HpcException;

  /**
   * Submit a request to download a collection.
   *
   * @param path The collection path.
   * @param destinationLocation The user requested destination.
   * @param destinationOverwrite If true, the requested destination location will be overwritten if
   *     it exists.
   * @param userId The user ID submitting the download request.
   * @param configurationId The configuration ID (needed to determine the archive connection
   *     config).
   * @return The submitted collection download task.
   * @throws HpcException on service failure.
   */
  public HpcCollectionDownloadTask downloadCollection(
      String path,
      HpcFileLocation destinationLocation,
      boolean destinationOverwrite,
      String userId,
      String configurationId)
      throws HpcException;

  /**
   * Submit a request to download data objects.
   *
   * @param dataObjectPathsMap A map of data-object-path to its configuration ID.
   * @param destinationLocation The user requested destination.
   * @param destinationOverwrite If true, the requested destination location will be overwritten if
   *     it exists.
   * @param userId The user ID submitting the download request.
   * @return The submitted request download task.
   * @throws HpcException on service failure.
   */
  public HpcCollectionDownloadTask downloadDataObjects(
      Map<String, String> dataObjectPathsMap,
      HpcFileLocation destinationLocation,
      boolean destinationOverwrite,
      String userId)
      throws HpcException;

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
  public void completeCollectionDownloadTask(
      HpcCollectionDownloadTask downloadTask, boolean result, String message, Calendar completed)
      throws HpcException;

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
   *     config).
   * @param fileContainerId The file container ID.
   * @return The file container name of the provider id.
   * @throws HpcException on data transfer system failure.
   */
  public String getFileContainerName(
      HpcDataTransferType dataTransferType, String configurationId, String fileContainerId)
      throws HpcException;
}
