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
import java.util.Date;
import java.util.List;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathPermissions;
import gov.nih.nci.hpc.domain.datatransfer.HpcAccessTokenType;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveObjectMetadata;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDeepArchiveStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcGoogleDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcPatternType;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcStreamingUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcSynchronousDownloadFilter;
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadPartETag;
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcUserDownloadRequest;
import gov.nih.nci.hpc.domain.model.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Transfer Service Interface.
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 */
public interface HpcDataTransferService {
	/**
	 * Upload a data object. Caller must provide one of the following 1. A source
	 * file. 2. A request to generate upload URL. 3. A Globus source location. 4. An
	 * AWS S3 source location.
	 *
	 * @param globusUploadSource             (Optional) The Globus upload source.
	 * @param s3UploadSource                 (Optional) The S3 upload source.
	 * @param googleDriveUploadSource        (Optional) The Google Drive upload
	 *                                       source.
	 * @param googleCloudStorageUploadSource (Optional) The Google Cloud Storage
	 *                                       upload source.
	 * @param fileSystemUploadSource         (Optional) The File System (DME Server
	 *                                       NAS) upload source.
	 * @param sourceFile                     (Optional) The source file.
	 * @param generateUploadRequestURL       Generate an upload URL (so caller can
	 *                                       directly upload file into archive).
	 * @param uploadParts                    (Optional) The number of parts when
	 *                                       generating upload request URL.
	 * @param uploadRequestURLChecksum       A checksum provided by the caller to be
	 *                                       attached to the upload request URL.
	 * @param path                           The data object registration path.
	 * @param dataObjectId                   The data object ID.
	 * @param userId                         The user-id who requested the data
	 *                                       upload.
	 * @param callerObjectId                 The caller's provided data object ID.
	 * @param configurationId                The configuration ID (needed to
	 *                                       determine the archive connection
	 *                                       config).
	 * @return A data object upload response.
	 * @throws HpcException on service failure.
	 */
	public HpcDataObjectUploadResponse uploadDataObject(HpcUploadSource globusUploadSource,
			HpcStreamingUploadSource s3UploadSource, HpcStreamingUploadSource googleDriveUploadSource,
			HpcStreamingUploadSource googleCloudStorageUploadSource, HpcUploadSource fileSystemUploadSource,
			File sourceFile, boolean generateUploadRequestURL, Integer uploadParts, String uploadRequestURLChecksum,
			String path, String dataObjectId, String userId, String callerObjectId, String configurationId)
			throws HpcException;

	/**
	 * Complete a multipart upload.
	 *
	 * @param archiveLocation          The archive location.
	 * @param dataTransferType         The data transfer type.
	 * @param configurationId          The configuration ID (needed to determine the
	 *                                 archive connection config).
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 *                                 Used to identify the S3 archive the
	 *                                 data-object is stored in. This is only
	 *                                 applicable for S3 archives, not POSIX.
	 * @param multipartUploadId        The multipart upload ID generated when the
	 *                                 multipart upload was initiated.
	 * @param uploadPartETags          A list of ETag for each part uploaded.
	 * @return The checksum of the data object as upload completed.
	 * @throws HpcException on service failure.
	 */
	public String completeMultipartUpload(HpcFileLocation archiveLocation, HpcDataTransferType dataTransferType,
			String configurationId, String s3ArchiveConfigurationId, String multipartUploadId,
			List<HpcUploadPartETag> uploadPartETags) throws HpcException;

	/**
	 * Download a data object file.
	 *
	 * @param path                                  The data object path.
	 * @param archiveLocation                       The archive file location.
	 * @param globusDownloadDestination             The user requested Glopbus
	 *                                              download destination.
	 * @param s3DownloadDestination                 The user requested S3 download
	 *                                              destination.
	 * @param googleDriveDownloadDestination        The user requested Google Drive
	 *                                              download destination.
	 * @param googleCloudStorageDownloadDestination The user requested Google Cloud
	 *                                              Storage download destination.
	 * @param synchronousDownloadFilter             (Optional) synchronous download
	 *                                              filter to extract specific files
	 *                                              from a data object that is
	 *                                              'compressed archive' such as
	 *                                              ZIP.
	 * @param dataTransferType                      The data transfer type.
	 * @param configurationId                       The configuration ID (needed to
	 *                                              determine the archive connection
	 *                                              config).
	 * @param s3ArchiveConfigurationId              (Optional) The S3 Archive
	 *                                              configuration ID. Used to
	 *                                              identify the S3 archive the
	 *                                              data-object is stored in. This
	 *                                              is only applicable for S3
	 *                                              archives, not POSIX.
	 * @param userId                                The user ID submitting the
	 *                                              download request.
	 * @param completionEvent                       If true, an event will be added
	 *                                              when async download is complete.
	 * @param collectionDownloadTaskId              (Optional) The collection
	 *                                              download task ID if this request
	 *                                              is part of a collection download
	 *                                              task
	 * @param size                                  The data object's size in bytes.
	 * @param downloadDataObject                    The data transfer status of the
	 *                                              data object
	 * @param deepArchiveStatus                     The deep archive status of the
	 *                                              data object
	 * @return A data object download response.
	 * @throws HpcException on service failure.
	 */
	public HpcDataObjectDownloadResponse downloadDataObject(String path, HpcFileLocation archiveLocation,
			HpcGlobusDownloadDestination globusDownloadDestination, HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDownloadDestination googleDriveDownloadDestination,
			HpcGoogleDownloadDestination googleCloudStorageDownloadDestination,
			HpcSynchronousDownloadFilter synchronousDownloadFilter, HpcDataTransferType dataTransferType,
			String configurationId, String s3ArchiveConfigurationId, String userId, boolean completionEvent,
			String collectionDownloadTaskId, long size, HpcDataTransferUploadStatus downloadDataObject,
			HpcDeepArchiveStatus deepArchiveStatus) throws HpcException;

	/**
	 * Generate a (pre-signed) download URL for a data object file.
	 *
	 * @param path                     The data object path.
	 * @param userId                   The user-id requesting to generate the
	 *                                 download URL.
	 * @param archiveLocation          The archive file location.
	 * @param dataTransferType         The data transfer type.
	 * @param size                     The data object's size in bytes.
	 * @param configurationId          The configuration ID (needed to determine the
	 *                                 archive connection config).
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 *                                 Used to identify the S3 archive the
	 *                                 data-object is stored in. This is only
	 *                                 applicable for S3 archives, not POSIX.
	 * @return The download URL.
	 * @throws HpcException on service failure.
	 */
	public String generateDownloadRequestURL(String path, String userId, HpcFileLocation archiveLocation,
			HpcDataTransferType dataTransferType, long size, String configurationId, String s3ArchiveConfigurationId)
			throws HpcException;

	/**
	 * Add system generated metadata to the data object in the archive.
	 *
	 * @param fileLocation             The file location.
	 * @param dataTransferType         The data transfer type.
	 * @param configurationId          The configuration ID (needed to determine the
	 *                                 archive connection config).
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 *                                 Used to identify the S3 archive the
	 *                                 data-object is stored in. This is only
	 *                                 applicable for S3 archives, not POSIX.
	 * @param objectId                 The data object id from the data management
	 *                                 system (UUID).
	 * @param registrarId              The user-id of the data registrar.
	 * @return The checksum and deepArchiveStatus of the data object as
	 *         HpcArchiveObjectMetadata
	 * @throws HpcException on service failure.
	 */
	public HpcArchiveObjectMetadata addSystemGeneratedMetadataToDataObject(HpcFileLocation fileLocation,
			HpcDataTransferType dataTransferType, String configurationId, String s3ArchiveConfigurationId,
			String objectId, String registrarId) throws HpcException;

	/**
	 * Delete a data object file.
	 *
	 * @param fileLocation             The file location.
	 * @param dataTransferType         The data transfer type.
	 * @param configurationId          The configuration ID (needed to determine the
	 *                                 archive connection config).
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 *                                 Used to identify the S3 archive the
	 *                                 data-object is stored in. This is only
	 *                                 applicable for S3 archives, not POSIX.
	 * @throws HpcException on service failure.
	 */
	public void deleteDataObject(HpcFileLocation fileLocation, HpcDataTransferType dataTransferType,
			String configurationId, String s3ArchiveConfigurationId) throws HpcException;

	/**
	 * Get a data transfer upload request status.
	 *
	 * @param dataTransferType         The data transfer type.
	 * @param dataTransferRequestId    The data transfer request ID.
	 * @param configurationId          The configuration ID (needed to determine the
	 *                                 archive connection config).
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 *                                 Used to identify the S3 archive the
	 *                                 data-object is stored in. This is only
	 *                                 applicable for S3 archives, not POSIX.
	 * @return The data transfer upload request status.
	 * @throws HpcException on service failure.
	 */
	public HpcDataTransferUploadReport getDataTransferUploadStatus(HpcDataTransferType dataTransferType,
			String dataTransferRequestId, String configurationId, String s3ArchiveConfigurationId) throws HpcException;

	/**
	 * Get a data transfer download request status.
	 *
	 * @param dataTransferType         The data transfer type.
	 * @param dataTransferRequestId    The data transfer request ID.
	 * @param configurationId          The configuration ID (needed to determine the
	 *                                 archive connection config).
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 *                                 Used to identify the S3 archive the
	 *                                 data-object is stored in. This is only
	 *                                 applicable for S3 archives, not POSIX.
	 * @return The data transfer download request status.
	 * @throws HpcException on service failure.
	 */
	public HpcDataTransferDownloadReport getDataTransferDownloadStatus(HpcDataTransferType dataTransferType,
			String dataTransferRequestId, String configurationId, String s3ArchiveConfigurationId) throws HpcException;

	/**
	 * Get path attributes for a given file in Globus or S3 archive (using system
	 * account).
	 *
	 * @param dataTransferType         The data transfer type.
	 * @param fileLocation             The endpoint/path to get attributes for.
	 * @param getSize                  If set to true, the file/directory size will
	 *                                 be returned.
	 * @param configurationId          The configuration ID (needed to determine the
	 *                                 archive connection config).
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 *                                 Used to identify the S3 archive the
	 *                                 data-object is stored in. This is only
	 *                                 applicable for S3 archives, not POSIX.
	 * @return The path attributes.
	 * @throws HpcException on service failure.
	 */
	public HpcPathAttributes getPathAttributes(HpcDataTransferType dataTransferType, HpcFileLocation fileLocation,
			boolean getSize, String configurationId, String s3ArchiveConfigurationId) throws HpcException;

	/**
	 * Get path attributes for a given file in AWS S3 (using user provided S3
	 * account).
	 *
	 * @param s3Account    The user provided S3 account.
	 * @param fileLocation The bucket/object-id to get attributes for.
	 * @param getSize      If set to true, the file/directory size will be returned.
	 * @return The path attributes.
	 * @throws HpcException on service failure.
	 */
	public HpcPathAttributes getPathAttributes(HpcS3Account s3Account, HpcFileLocation fileLocation, boolean getSize)
			throws HpcException;

	/**
	 * Get path attributes for a given file in Google Drive or Google Cloud
	 * Storage(using user provided Google Drive token).
	 *
	 * @param accessToken      Google Drive / Storage access token.
	 * @param accessTokenToken Access token type (user account / system account).
	 * @param fileLocation     The file to get attributes for.
	 * @param getSize          If set to true, the file/directory size will be
	 *                         returned.
	 * @return The path attributes.
	 * @throws HpcException on service failure.
	 */
	public HpcPathAttributes getPathAttributes(HpcDataTransferType dataTransferType, String accessToken,
			HpcAccessTokenType accessTokenType, HpcFileLocation fileLocation, boolean getSize) throws HpcException;

	/**
	 * Get path attributes of local file (on the DME server file system)
	 *
	 * @param fileLocation The local file location.
	 * @return The path attributes.
	 * @throws HpcException on service failure.
	 */
	public HpcPathAttributes getPathAttributes(HpcFileLocation fileLocation) throws HpcException;

	/**
	 * Scan a directory (recursively) and return a list of all its files.
	 *
	 * @param dataTransferType         (Optional) The data transfer type. If null, a
	 *                                 directory on the local DME server NAS is
	 *                                 scanned.
	 * @param s3Account                (Optional) S3 account to use.
	 * @param googleAccessToken        (Optional) Google Drive/Storage access-token
	 *                                 to use.
	 * @param googleAccessTokenType    (Optional) Google Drive/Storage access-token
	 *                                 type.
	 * @param directoryLocation        The endpoint/directory to scan and get a list
	 *                                 of files for.
	 * @param configurationId          The configuration ID (needed to determine the
	 *                                 archive connection config).
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 *                                 Used to identify the S3 archive the
	 *                                 data-object is stored in. This is only
	 *                                 applicable for S3 archives, not POSIX.
	 * @param includePatterns          The patterns to use to include files in the
	 *                                 scan results.
	 * @param excludePatterns          The patterns to use to exclude files from the
	 *                                 scan results.
	 * @param patternType              The type of the patterns provided.
	 * @return A list of files found.
	 * @throws HpcException on service failure.
	 */
	public List<HpcDirectoryScanItem> scanDirectory(HpcDataTransferType dataTransferType, HpcS3Account s3Account,
			String googleAccessToken, HpcAccessTokenType googleAccessTokenType, HpcFileLocation directoryLocation,
			String configurationId, String s3ArchiveConfigurationId, List<String> includePatterns,
			List<String> excludePatterns, HpcPatternType patternType) throws HpcException;

	/**
	 * Get a file from the archive.
	 *
	 * @param configurationId          The data management configuration ID.
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 *                                 Used to identify the S3 archive the
	 *                                 data-object is stored in. This is only
	 *                                 applicable for S3 archives, not POSIX.
	 * @param dataTransferType         The data transfer type.
	 * @param fileId                   The file ID.
	 * @return The requested file from the archive.
	 * @throws HpcException on service failure.
	 */
	public File getArchiveFile(String configurationId, String s3ArchiveConfigurationId,
			HpcDataTransferType dataTransferType, String fileId) throws HpcException;

	/**
	 * Get all (active) data object download tasks.
	 *
	 * @return A list of data object download tasks.
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataObjectDownloadTask> getDataObjectDownloadTasks() throws HpcException;

	/**
	 * Get next data object download task to process given data transfer status and
	 * data transfer type.
	 *
	 * @param dataTransferStatus The data object download task data transfer status.
	 * @param dataTransferType   The data object download task data transfer type.
	 * @param processed          The processed date to pick up only records that
	 *                           have not yet been processed in this run.
	 * @return Data object download task.
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataObjectDownloadTask> getNextDataObjectDownloadTask(
			HpcDataTransferDownloadStatus dataTransferStatus, HpcDataTransferType dataTransferType, Date processed)
			throws HpcException;

	/**
	 * Get download task status.
	 *
	 * @param taskId   The download task ID.
	 * @param taskType The download task type (data-object or collection).
	 * @return A download status object, or null if the task can't be found. Note:
	 *         The returned object is associated with a 'task' object if the
	 *         download is in-progress. If the download completed or failed, the
	 *         returned object is associated with a 'result' object.
	 * @throws HpcException on service failure.
	 */
	public HpcDownloadTaskStatus getDownloadTaskStatus(String taskId, HpcDownloadTaskType taskType) throws HpcException;

	/**
	 * Get a collection download task cancellation request.
	 *
	 * @param taskId The collection download task ID.
	 * @return The value of 'cancellation requested' column for this collection
	 *         download task. False on any error (task doesn't exist, etc)
	 */
	public boolean getCollectionDownloadTaskCancellationRequested(String taskId);

	/**
	 * Complete an async (Globus / S3 / Google Drive) data object download task : 1.
	 * Cleanup any files staged in the file system for download. 2. Update task info
	 * in DB with results info.
	 *
	 * @param downloadTask     The download task to complete.
	 * @param result           The result of the task (completed, failed or
	 *                         canceled).
	 * @param message          (Optional) If the task failed, a message describing
	 *                         the failure.
	 * @param completed        (Optional) The download task completion timestamp.
	 * @param bytesTransferred The total bytes transfered.
	 * @throws HpcException on service failure.
	 */
	public void completeDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask, HpcDownloadResult result,
			String message, Calendar completed, long bytesTransferred) throws HpcException;

	/**
	 * Complete a synchronous data object download task.
	 *
	 * @param taskResult The download task result object.
	 * @param result     The result of the task (completed or failed).
	 * @param completed  The download task completion timestamp.
	 * @throws HpcException on service failure.
	 */
	public void completeSynchronousDataObjectDownloadTask(HpcDownloadTaskResult taskResult, HpcDownloadResult result,
			Calendar completed) throws HpcException;

	/**
	 * Continue a data object download task that was queued.
	 *
	 * @param downloadTask The download task to continue.
	 * @throws HpcException on service failure.
	 */
	public void continueDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask) throws HpcException;

	/**
	 * Stage a data object for download from Hyperfile to Globus. Once staged, set
	 * it's status to RECEIVED.
	 *
	 * @param downloadTask The download task to perform staging from hyperfile
	 *                     archive.
	 * @throws HpcException on service failure.
	 */
	public void stageHyperfileDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask) throws HpcException;

	/**
	 * Reset a data object download task. Set it's status to RECEIVED.
	 *
	 * @param downloadTask The download task to reset.
	 * @throws HpcException on service failure.
	 */
	public void resetDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask) throws HpcException;

	/**
	 * Reset all download tasks in-process indicator to false
	 *
	 * @throws HpcException on service failure.
	 */
	public void resetDataObjectDownloadTasksInProcess() throws HpcException;

	/**
	 * Mark a data object download task as processed by updating the processed time
	 * stamp.
	 *
	 * @param downloadTask The download task to mark processed.
	 * @param inProcess    Indicator whether the task is being actively processed.
	 * @throws HpcException on service failure.
	 */
	public void markProcessedDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask, boolean inProcess)
			throws HpcException;

	/**
	 * Update a data object download task. % Complete is calculated and any change
	 * on the task object will be persisted.
	 *
	 * @param downloadTask     The download task to update progress
	 * @param bytesTransferred The bytes transferred so far.
	 * @throws HpcException on service failure.
	 */
	public void updateDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask, long bytesTransferred)
			throws HpcException;

	/**
	 * Submit a request to download a collection.
	 *
	 * @param path                                  The collection path.
	 * @param globusDownloadDestination             The user requested Glopbus
	 *                                              download destination.
	 * @param s3DownloadDestination                 The user requested S3 download
	 *                                              destination.
	 * @param googleDriveDownloadDestination        The user requested Google Drive
	 *                                              download destination.
	 * @param googleCloudStorageDownloadDestination The user requested Google Cloud
	 *                                              Storage download destination.
	 * @param userId                                The user ID submitting the
	 *                                              download request.
	 * @param configurationId                       The configuration ID (needed to
	 *                                              determine the archive connection
	 *                                              config).
	 * @return The submitted collection download task.
	 * @throws HpcException on service failure.
	 */
	public HpcCollectionDownloadTask downloadCollection(String path,
			HpcGlobusDownloadDestination globusDownloadDestination, HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDownloadDestination googleDriveDownloadDestination,
			HpcGoogleDownloadDestination googleCloudStorageDownloadDestination, String userId, String configurationId)
			throws HpcException;

	/**
	 * Submit a request to download collections.
	 *
	 * @param collectionPaths                       A list of collection paths.
	 * @param globusDownloadDestination             The user requested Glopbus
	 *                                              download destination.
	 * @param s3DownloadDestination                 The user requested S3 download
	 *                                              destination.
	 * @param googleDriveDownloadDestination        The user requested Google Drive
	 *                                              download destination.
	 * @param googleCloudStorageDownloadDestination The user requested Google Cloud
	 *                                              Storage download destination.
	 * @param userId                                The user ID submitting the
	 *                                              download request.
	 * @param configurationId                       A configuration ID used to
	 *                                              validate destination location.
	 *                                              The list of data objects can be
	 *                                              from from different
	 *                                              configurations (DOCs) but we
	 *                                              validate just for one.
	 * @param appendPathToDownloadDestination       If true, the (full) object path
	 *                                              will be used in the destination
	 *                                              path, otherwise just the object
	 *                                              name will be used.
	 * @return The submitted request download task.
	 * @throws HpcException on service failure.
	 */
	public HpcCollectionDownloadTask downloadCollections(List<String> collectionPaths,
			HpcGlobusDownloadDestination globusDownloadDestination, HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDownloadDestination googleDriveDownloadDestination,
			HpcGoogleDownloadDestination googleCloudStorageDownloadDestination, String userId, String configurationId,
			boolean appendPathToDownloadDestination) throws HpcException;

	/**
	 * Submit a request to download data objects.
	 *
	 * @param dataObjectPaths                       A list of data object paths.
	 * @param globusDownloadDestination             The user requested Glopbus
	 *                                              download destination.
	 * @param s3DownloadDestination                 The user requested S3 download
	 *                                              destination.
	 * @param googleDriveDownloadDestination        The user requested Google Drive
	 *                                              download destination.
	 * @param googleCloudStorageDownloadDestination The user requested Google Cloud
	 *                                              Storage download destination.
	 * @param userId                                The user ID submitting the
	 *                                              download request.
	 * @param configurationId                       A configuration ID used to
	 *                                              validate destination location.
	 *                                              The list of data objects can be
	 *                                              from from different
	 *                                              configurations (DOCs) but we
	 *                                              validate just for one.
	 * @param appendPathToDownloadDestination       If true, the (full) object path
	 *                                              will be used in the destination
	 *                                              path, otherwise just the object
	 *                                              name will be used.
	 * @return The submitted request download task.
	 * @throws HpcException on service failure.
	 */
	public HpcCollectionDownloadTask downloadDataObjects(List<String> dataObjectPaths,
			HpcGlobusDownloadDestination globusDownloadDestination, HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDownloadDestination googleDriveDownloadDestination,
			HpcGoogleDownloadDestination googleCloudStorageDownloadDestination, String userId, String configurationId,
			boolean appendPathToDownloadDestination) throws HpcException;

	/**
	 * Update a collection download task.
	 *
	 * @param downloadTask The collection download task to update.
	 * @throws HpcException on service failure.
	 */
	public void updateCollectionDownloadTask(HpcCollectionDownloadTask downloadTask) throws HpcException;

	/**
	 * Process the second hop download to Globus of a collection, by submitting a
	 * single transfer request for the entire bunch.
	 *
	 * @param downloadTask            The collection download task to process.
	 * @param dataObjectDownloadTasks The individual data object download tasks that
	 *                                are to be included in the bunch request to
	 *                                Globus.
	 * @throws HpcException on service failure.
	 */
	public void processCollectionDownloadTaskSecondHopBunch(HpcCollectionDownloadTask downloadTask,
			List<HpcDataObjectDownloadTask> dataObjectDownloadTasks) throws HpcException;

	/**
	 * Cancel a collection download task. This will mark any pending download items
	 * (i.e. items in RECEIVED state) in this collection download task for
	 * cancellation.
	 *
	 * @param downloadTask The collection download task to cancel.
	 * @throws HpcException on service failure.
	 */
	public void cancelCollectionDownloadTask(HpcCollectionDownloadTask downloadTask) throws HpcException;

	/**
	 * Cancel a collection download task items. This will mark any pending download
	 * items (i.e. items in RECEIVED state) in this collection download task for
	 * cancellation.
	 *
	 * @param downloadItems The collection download task items to cancel.
	 * @throws HpcException on service failure.
	 */
	public void cancelCollectionDownloadTaskItems(List<HpcCollectionDownloadTaskItem> downloadItems)
			throws HpcException;

	/**
	 * Retry a collection download task. This will retry all failed download items
	 * on a collection download task.
	 *
	 * @param downloadTaskResult   The collection download task to retry
	 * @param destinationOverwrite (Optional) destination overwrite for Globus
	 *                             destinations.
	 * @param s3Account            (Optional) s3Account for S3 destinations.
	 * @param googleAccessToken    (Optional) access token for Google Drive / Cloud
	 *                             storage destinations. * @param
	 *                             googleAccessTokenType (Optional) access token for
	 *                             Google Cloud storage destinations.
	 * @return The submitted request download task.
	 * @throws HpcException on service failure.
	 */
	public HpcCollectionDownloadTask retryCollectionDownloadTask(HpcDownloadTaskResult downloadTaskResult,
			Boolean destinationOverwrite, HpcS3Account s3Account, String googleAccessToken,
			HpcAccessTokenType googleAccessTokenType) throws HpcException;

	/**
	 * Get collection download tasks.
	 *
	 * @param status Get tasks in this status.
	 * @return A list of collection download tasks.
	 * @throws HpcException on database error.
	 */
	public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus status)
			throws HpcException;

	/**
	 * Get collection download tasks.
	 *
	 * @param status    Get tasks in this status.
	 * @param inProcess Indicator whether the task is being actively processed.
	 * @return A list of collection download tasks.
	 * @throws HpcException on database error.
	 */
	public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus status,
			boolean inProcess) throws HpcException;

	/**
	 * Get collection download tasks count for a user.
	 *
	 * @param userId    The user ID.
	 * @param status    Get tasks in this status.
	 * @param inProcess Indicator whether the task is being actively processed.
	 * @return A list of collection download tasks.
	 * @throws HpcException on database error.
	 */
	public int getCollectionDownloadTasksCount(String userId, HpcCollectionDownloadTaskStatus status, boolean inProcess)
			throws HpcException;

	/**
	 * Get collection download requests count for a path and endpoint.
	 *
	 * @param path   The archive path to download from.
	 * @param status The destination endpoint.
	 * @return Count of collection download requests.
	 * @throws HpcException on database error.
	 */
	public int getCollectionDownloadRequestsCountByPathAndEndpoint(String path, String endpoint) throws HpcException;

	/**
	 * Get collection download tasks count for a specific user and path.
	 *
	 * @userId The userId to query for.
	 * @param path      The archive path to download from.
	 * @param inProcess True for collections that are under processing.
	 * @return Count of collection download tasks.
	 * @throws HpcException on database error.
	 */
	public int getCollectionDownloadTasksCountByUserAndPath(String userId, String path, boolean inProcess)
			throws HpcException;

	/**
	 * Get collection download tasks count for a specific user.
	 *
	 * @userId The userId to query for.
	 * @param inProcess True for collections that are under processing.
	 * @return Count of collection download tasks.
	 * @throws HpcException on database error.
	 */
	public int getCollectionDownloadTasksCountByUser(String userId, boolean inProcess) throws HpcException;

	/**
	 * Set collection download task in-progress
	 *
	 * @param taskId    The collection download task ID.
	 * @param inProcess Indicator whether the task is being actively processed.
	 * @throws HpcException on database error.
	 */
	public void setCollectionDownloadTaskInProgress(String taskId, boolean inProcess) throws HpcException;

	/**
	 * Reset collection download task in-progress
	 *
	 * @param taskId The collection download task ID..
	 * @throws HpcException on database error.
	 */
	public void resetCollectionDownloadTaskInProgress(String taskId) throws HpcException;

	/**
	 * Complete a collection download task: 1. Update task info in DB with results
	 * info.
	 *
	 * @param downloadTask The download task to complete.
	 * @param result       The result of the task (completed, failed or canceled).
	 * @param message      (Optional) If the task failed, a message describing the
	 *                     failure.
	 * @param completed    (Optional) The download task completion timestamp.
	 * @throws HpcException on service failure.
	 */
	public void completeCollectionDownloadTask(HpcCollectionDownloadTask downloadTask, HpcDownloadResult result,
			String message, Calendar completed) throws HpcException;

	/**
	 * Get active download requests for a user.
	 *
	 * @param userId The user ID to query for.
	 * @param doc    doc of group admin or all for system administrators
	 * @return A list of active download requests.
	 * @throws HpcException on service failure.
	 */
	public List<HpcUserDownloadRequest> getDownloadRequests(String userId, String doc) throws HpcException;

	/**
	 * Get download results (all completed download requests) for a user.
	 *
	 * @param userId The user ID to query for.
	 * @param page   The requested results page.
	 * @param doc    doc of group admin or all for system administrators
	 * @return A list of completed download requests.
	 * @throws HpcException on service failure.
	 */
	public List<HpcUserDownloadRequest> getDownloadResults(String userId, int page, String doc) throws HpcException;

	/**
	 * Get download results (all completed download requests) count for a user.
	 *
	 * @param userId The user ID to query for.
	 * @param doc    doc of group admin or all for system administrators
	 * @return A total count of completed download requests.
	 * @throws HpcException on service failure.
	 */
	public int getDownloadResultsCount(String userId, String doc) throws HpcException;

	/**
	 * Get inprocess data object download count.
	 *
	 * @param dataTransferType The data transfer type.
	 * @param destinationType  The destination type.
	 * @return A total count of completed download requests.
	 * @throws HpcException on database error.
	 */
	public int getInProcessDataObjectDownloadTasksCount(HpcDataTransferType dataTransferType,
			HpcDataTransferType destinationType) throws HpcException;

	/**
	 * Get the download results page size.
	 *
	 * @return The download results page size.
	 */
	public int getDownloadResultsPageSize();

	/**
	 * Get a file-container-name from a file-container-id
	 *
	 * @param dataTransferType The data transfer type.
	 * @param configurationId  The configuration ID (needed to determine the archive
	 *                         connection config).
	 * @param fileContainerId  The file container ID.
	 * @return The file container name of the provider id.
	 * @throws HpcException on data transfer system failure.
	 */
	public String getFileContainerName(HpcDataTransferType dataTransferType, String configurationId,
			String fileContainerId) throws HpcException;

	/**
	 * Calculate a data object upload % complete. Note: if upload not in progress,
	 * null is returned.
	 *
	 * @param systemGeneratedMetadata The system generated metadata of the data
	 *                                object.
	 * @return The transfer % completion if transfer is in progress, or null
	 *         otherwise.
	 */
	public Integer calculateDataObjectUploadPercentComplete(HpcSystemGeneratedMetadata systemGeneratedMetadata);

	/**
	 * Check if an upload URL expired.
	 *
	 * @param urlCreated               The data/time the URL was generated
	 * @param configurationId          The data management configuration ID. This is
	 *                                 to get the expiration config.
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 *                                 Used to identify the S3 archive the
	 *                                 data-object is stored in. This is only
	 *                                 applicable for S3 archives, not POSIX.
	 * @return True if the upload URL expired, or false otherwise.
	 */
	public boolean uploadURLExpired(Calendar urlCreated, String configurationId, String s3ArchiveConfigurationId);

	/**
	 * Get all data object download task to process given data transfer status.
	 *
	 * @param dataTransferStatus The data object download task data transfer status.
	 * @return Data object download task.
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataObjectDownloadTask> getDataObjectDownloadTaskByStatus(
			HpcDataTransferDownloadStatus dataTransferStatus) throws HpcException;

	/**
	 * Get data object meta data
	 * 
	 * @param fileLocation             The archive file location.
	 * @param dataTransferType         The data transfer type.
	 * @param configurationId          The configuration ID.
	 * @param s3ArchiveConfigurationId (Optional) The S3 configuration ID.
	 * @return HpcArchiveObjectMetadata The metadata attached to the object.
	 * @throws HpcException on data transfer system failure.
	 */
	public HpcArchiveObjectMetadata getDataObjectMetadata(HpcFileLocation fileLocation,
			HpcDataTransferType dataTransferType, String configurationId, String s3ArchiveConfigurationId)
			throws HpcException;

	/**
	 * Set archive permissions.
	 *
	 * @param configurationId          The data management configuration ID.
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 *                                 Used to identify the S3 archive the
	 *                                 data-object is stored in. This is only
	 *                                 applicable for S3 archives, not POSIX.
	 * @param dataTransferType         The data transfer type.
	 * @param fileId                   The file ID. (Can be a directory)
	 * @throws HpcException on service failure.
	 */
	public void setArchivePermissions(String configurationId, String s3ArchiveConfigurationId,
			HpcDataTransferType dataTransferType, String fileId, HpcPathPermissions permissions) throws HpcException;

}
