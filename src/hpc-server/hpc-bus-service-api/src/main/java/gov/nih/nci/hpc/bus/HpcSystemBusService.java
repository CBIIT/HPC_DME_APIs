/**
 * HpcSystemBusService.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadResult;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC System Business Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcSystemBusService {
	/**
	 * Submit a data transfer requests for the the upload requests that are queued
	 * (because the data-transfer system was busy).
	 *
	 * @throws HpcException on service failure.
	 */
	public void processDataTranferUploadReceived() throws HpcException;

	/**
	 * Update the data transfer upload status of all data objects that the transfer
	 * is 'in progress' (Globus).
	 *
	 * @throws HpcException on service failure.
	 */
	public void processDataTranferUploadInProgress() throws HpcException;

	/**
	 * Update the data transfer upload status of all data objects that users are
	 * responsible to upload with a generated upload URL.
	 *
	 * @throws HpcException on service failure.
	 */
	public void processDataTranferUploadInProgressWithGeneratedURL() throws HpcException;

	/**
	 * Update the data transfer upload status of all data objects that are currently
	 * streamed (S3).
	 *
	 * @param streamingStopped If true, S3 streaming stopped (because API server
	 *                         shutdown). In this case we set the upload status to
	 *                         STREAMING_STOPPED. Otherwise, we check if the upload
	 *                         completed and update status accordingly.
	 * @throws HpcException on service failure.
	 */
	public void processDataTranferUploadStreamingInProgress(boolean streamingStopped) throws HpcException;

	public void processDataTranferUploadStreamingInProgress() throws HpcException;

	/**
	 * Restart data transfer upload for all streaming from AWS S3 that has stopped
	 * (because of prior server shutdown)
	 *
	 * @throws HpcException on service failure.
	 */
	public void processDataTranferUploadStreamingStopped() throws HpcException;

	/**
	 * Upload data objects currently in temporary archive to the (permanent)
	 * archive, and complete the registration process.
	 *
	 * @throws HpcException on service failure.
	 */
	public void processTemporaryArchive() throws HpcException;

	/**
	 * Upload data objects currently staged on DME server file system (NAS) to the
	 * archive
	 *
	 * @throws HpcException on service failure.
	 */
	public void processDataTranferUploadFileSystemReady() throws HpcException;

	/**
	 * Restart upload of data objects currently staged on DME server file system
	 * (NAS) to the archive
	 *
	 * @throws HpcException on service failure.
	 */
	public void processDataTransferUploadFileSystemInProgress() throws HpcException;

	/**
	 * Start all received data objects download tasks for Globus and process to make
	 * them in progress.
	 *
	 * @throws HpcException on service failure.
	 */
	public void startGlobusDataObjectDownloadTasks() throws HpcException;

	/**
	 * Stage download requests from Hyperfile to a Globus destination (Copy the file
	 * from Hyperfile to a DME Globus endpoint).
	 *
	 * @throws HpcException on service failure.
	 */
	public void stageHyperfileGlobusDataObjectDownloadTasks() throws HpcException;

	/**
	 * Start all received data objects download tasks for S3 and process to make
	 * them in progress.
	 *
	 * @throws HpcException on service failure.
	 */
	public void startS3DataObjectDownloadTasks() throws HpcException;

	/**
	 * Start all received data objects download tasks for Google Drive and process
	 * to make them in progress.
	 *
	 * @throws HpcException on service failure.
	 */
	public void startGoogleDriveDataObjectDownloadTasks() throws HpcException;

	/**
	 * Check status of all in-progress data objects download tasks and complete
	 * these that are no longer in progress.
	 *
	 * @throws HpcException on service failure.
	 */
	public void completeInProgressDataObjectDownloadTasks() throws HpcException;

	/**
	 * Complete all data object download tasks that are marked canceled.
	 *
	 * @throws HpcException on service failure.
	 */
	public void completeCanceledDataObjectDownloadTasks() throws HpcException;

	/**
	 * Restart data object download tasks that are in progress using S3 data
	 * transfer.
	 *
	 * @throws HpcException on service failure.
	 */
	public void restartDataObjectDownloadTasks() throws HpcException;

	/**
	 * Restart collection download tasks that are in-process.
	 *
	 * @throws HpcException on service failure.
	 */
	public void restartCollectionDownloadTasks() throws HpcException;

	/**
	 * Process collection download tasks that received. i.e. kick off the download
	 * of individual data objects under each requested collection.
	 *
	 * @throws HpcException on service failure.
	 */
	public void processCollectionDownloadTasks() throws HpcException;

	/**
	 * Check status of all active collection download tasks and complete these that
	 * are no longer in progress.
	 *
	 * @throws HpcException on service failure.
	 */
	public void completeCollectionDownloadTasks() throws HpcException;

	/**
	 * Process bulk data object registration tasks that received.
	 *
	 * @throws HpcException on service failure.
	 */
	public void processBulkDataObjectRegistrationTasks() throws HpcException;

	/**
	 * Check status of all active bulk data object registration tasks and complete
	 * these that are no longer in progress.
	 *
	 * @throws HpcException on service failure.
	 */
	public void completeBulkDataObjectRegistrationTasks() throws HpcException;

	/**
	 * Complete a synchronous data object download task
	 *
	 * @param taskId The download task ID.
	 * @param result The result of the task (completed or failed).
	 * @throws HpcException on service failure.
	 */
	public void completeSynchronousDataObjectDownloadTask(String taskId, HpcDownloadResult result) throws HpcException;

	/**
	 * Process all (active) events.
	 *
	 * @throws HpcException on service failure.
	 */
	public void processEvents() throws HpcException;

	/**
	 * Generate summary report event.
	 *
	 * @throws HpcException on service failure.
	 */
	public void generateSummaryReportEvent() throws HpcException;

	/**
	 * Generate weekly summary report event.
	 *
	 * @throws HpcException on service failure.
	 */
	public void generateWeeklySummaryReportEvent() throws HpcException;

	/**
	 * Generate summary by DOC report event.
	 *
	 * @throws HpcException on service failure.
	 */
	public void generateDocReportEvent() throws HpcException;

	/**
	 * Generate summary by weekly DOC reports events.
	 *
	 * @throws HpcException on service failure.
	 */
	public void generateWeeklyDocReportEvent() throws HpcException;

	/**
	 * Generate user summary report event.
	 *
	 * @throws HpcException on service failure.
	 */
	public void generateUserReportEvent() throws HpcException;

	/**
	 * Generate weekly user summary report event.
	 *
	 * @throws HpcException on service failure.
	 */
	public void generateWeeklyUserReportEvent() throws HpcException;

	/**
	 * Refresh the metadata materialized views.
	 *
	 * @throws HpcException on service failure.
	 */
	public void refreshMetadataViews() throws HpcException;

	/**
	 * Refresh the reports materialized views.
	 *
	 * @throws HpcException on service failure.
	 */
	public void refreshReportViews() throws HpcException;

	/**
	 * Complete data objects that are deep archive in progress.
	 *
	 * @throws HpcException on service failure.
	 */
	public void completeDeepArchiveInProgress() throws HpcException;

	/**
	 * Complete the restore request tasks.
	 *
	 * @throws HpcException on service failure.
	 */
	public void completeRestoreRequest() throws HpcException;

	/**
	 * Permanently remove deleted objects.
	 *
	 * @throws HpcException on service failure.
	 */
	public void removeDeletedDataObjects() throws HpcException;

	/** Close connection to Data Management system for the current service call. */
	public void closeConnection();
}
