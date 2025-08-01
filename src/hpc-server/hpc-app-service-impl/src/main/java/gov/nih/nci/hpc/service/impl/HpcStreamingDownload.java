/**
 * HpcStreamingDownload.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.hpc.dao.HpcDataDownloadDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcEventService;

/**
 * A data transfer listener for async downloads. The streaming is done from the
 * S3 archive to AWS or Google Drive. This listener is also used for downloading
 * to Aspera.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcStreamingDownload implements HpcDataTransferProgressListener {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// A download data object task (keeps track of the async streaming download
	// end-to-end.
	private HpcDataObjectDownloadTask downloadTask = new HpcDataObjectDownloadTask();

	// Data object download DAO.
	private HpcDataDownloadDAO dataDownloadDAO = null;

	// Event service.
	private HpcEventService eventService = null;

	// Data Transfer service.
	private HpcDataTransferService dataTransferService = null;

	// Indicator whether task has removed / cancelled
	private boolean taskCancelled = false;

	// The CompletableFuture instance that is executing the transfer. Used to cancel
	// if needed.
	private CompletableFuture<?> completableFuture = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructs a Streaming download object (to keep track of async processing)
	 *
	 * @param downloadRequest     The download request.
	 * @param dataDownloadDAO     data download DAO.
	 * @param eventService        event service.
	 * @param dataTransferService data transfer service.
	 * @throws HpcException If it failed to create a download task.
	 */
	public HpcStreamingDownload(HpcDataObjectDownloadRequest downloadRequest, HpcDataDownloadDAO dataDownloadDAO,
			HpcEventService eventService, HpcDataTransferService dataTransferService) throws HpcException {
		// Create an persist a download task. This object tracks the download request
		// through completion
		this.dataDownloadDAO = dataDownloadDAO;
		this.eventService = eventService;
		this.dataTransferService = dataTransferService;
		createDownloadTask(downloadRequest);
	}

	/**
	 * Constructs a streaming download object (to keep track of async processing).
	 * This constructor is used when a streaming download is restarted, i.e. there
	 * is an existing task, and the transfer needs to be restarted.
	 *
	 * @param downloadTask        The download task.
	 * @param dataDownloadDAO     data download DAO.
	 * @param eventService        event service.
	 * @param dataTransferService data transfer service.
	 * @throws HpcException If it failed to update a download task.
	 */
	public HpcStreamingDownload(HpcDataObjectDownloadTask downloadTask, HpcDataDownloadDAO dataDownloadDAO,
			HpcEventService eventService, HpcDataTransferService dataTransferService) throws HpcException {
		// Update an persist a download task. This object tracks the download request
		// through completion
		this.dataDownloadDAO = dataDownloadDAO;
		this.eventService = eventService;
		this.dataTransferService = dataTransferService;
		updateDownloadTask(downloadTask);
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	/**
	 * Return the download task.
	 *
	 * @return The download task being tracked.
	 */
	public HpcDataObjectDownloadTask getDownloadTask() {
		return downloadTask;
	}

	// ---------------------------------------------------------------------//
	// HpcDataTransferProgressListener Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void transferCompleted(Long bytesTransferred) {
		// This callback method is called when the Streaming download completed.
		completeDownloadTask(HpcDownloadResult.COMPLETED, null, bytesTransferred);
	}

	@Override
	public void transferFailed(String message) {
		if (taskCancelled) {
			// The task was cancelled / removed from the DB. Do some cleanup.
			FileUtils.deleteQuietly(new File(downloadTask.getDownloadFilePath()));

			logger.info("download task: [taskId={}] - cancelled/removed - {} [transfer-type={}, destination-type={}]",
					downloadTask.getId(), downloadTask.getPercentComplete(), downloadTask.getDataTransferType(),
					downloadTask.getDestinationType());
			return;
		}

		// This callback method is called when streaming download failed.
		completeDownloadTask(HpcDownloadResult.FAILED, message, 0);
	}

	@Override
	public void transferProgressed(long bytesTransferred) {
		try {
			dataTransferService.updateDataObjectDownloadTask(downloadTask, bytesTransferred);

		} catch (HpcException e) {

		}

		try {
			if (!dataTransferService.updateDataObjectDownloadTask(downloadTask, bytesTransferred)) {
				// The task was cancelled / removed from the DB. Stop download thread.
				logger.info("download task: {} - Task cancelled", downloadTask.getId());
				taskCancelled = true;

				// Stopping the transfer thread.
				if (completableFuture != null) {
					boolean cancelStatus = completableFuture.cancel(true);
					logger.info("download task: {} - transfer cancellation result: {}", downloadTask.getId(),
							cancelStatus);
				}
			}

		} catch (HpcException e) {
			logger.error("Failed to update Streaming download task progress", e);
		}
	}

	@Override
	public void setCompletableFuture(CompletableFuture<?> completableFuture) {
		this.completableFuture = completableFuture;
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Create a download task for a streaming download (to AWS S3 or Google Drive).
	 *
	 * @param downloadRequest The download request.
	 * @throws HpcException If it failed to persist the task.
	 */
	private void createDownloadTask(HpcDataObjectDownloadRequest downloadRequest) throws HpcException {

		downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.IN_PROGRESS);
		downloadTask.setDownloadFilePath(null);
		downloadTask.setUserId(downloadRequest.getUserId());
		downloadTask.setPath(downloadRequest.getPath());
		downloadTask.setConfigurationId(downloadRequest.getConfigurationId());
		downloadTask.setS3ArchiveConfigurationId(downloadRequest.getS3ArchiveConfigurationId());
		downloadTask.setCompletionEvent(downloadRequest.getCompletionEvent());
		downloadTask.setCollectionDownloadTaskId(downloadRequest.getCollectionDownloadTaskId());
		downloadTask.setArchiveLocation(downloadRequest.getArchiveLocation());
		downloadTask.setS3DownloadDestination(downloadRequest.getS3Destination());
		downloadTask.setGoogleDriveDownloadDestination(downloadRequest.getGoogleDriveDestination());
		downloadTask.setGoogleCloudStorageDownloadDestination(downloadRequest.getGoogleCloudStorageDestination());
		downloadTask.setAsperaDownloadDestination(downloadRequest.getAsperaDestination());
		downloadTask.setBoxDownloadDestination(downloadRequest.getBoxDestination());
		downloadTask.setCreated(Calendar.getInstance());
		downloadTask.setPercentComplete(0);
		downloadTask.setSize(downloadRequest.getSize());
		downloadTask.setFirstHopRetried(false);
		downloadTask.setRetryTaskId(downloadRequest.getRetryTaskId());
		downloadTask.setRetryUserId(downloadRequest.getRetryUserId());

		if (downloadTask.getS3DownloadDestination() != null) {
			downloadTask.setDataTransferType(HpcDataTransferType.S_3);
			downloadTask.setDestinationType(HpcDataTransferType.S_3);
		} else if (downloadTask.getGoogleDriveDownloadDestination() != null) {
			downloadTask.setDataTransferType(HpcDataTransferType.GOOGLE_DRIVE);
			downloadTask.setDestinationType(HpcDataTransferType.GOOGLE_DRIVE);
		} else if (downloadTask.getGoogleCloudStorageDownloadDestination() != null) {
			downloadTask.setDataTransferType(HpcDataTransferType.GOOGLE_CLOUD_STORAGE);
			downloadTask.setDestinationType(HpcDataTransferType.GOOGLE_CLOUD_STORAGE);
		} else if (downloadTask.getBoxDownloadDestination() != null) {
			downloadTask.setDataTransferType(HpcDataTransferType.BOX);
			downloadTask.setDestinationType(HpcDataTransferType.BOX);
		}

		dataDownloadDAO.createDataObjectDownloadTask(downloadTask);
	}

	/**
	 * Update a download task for a streaming download (to AWS S3 or Google Drive or
	 * Aspera).
	 *
	 * @param downloadTask The download task.
	 * @throws HpcException If it failed to persist the task.
	 */
	private void updateDownloadTask(HpcDataObjectDownloadTask downloadTask) throws HpcException {
		this.downloadTask.setId(downloadTask.getId());
		this.downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.IN_PROGRESS);
		this.downloadTask.setDownloadFilePath(downloadTask.getDownloadFilePath());
		this.downloadTask.setUserId(downloadTask.getUserId());
		this.downloadTask.setPath(downloadTask.getPath());
		this.downloadTask.setConfigurationId(downloadTask.getConfigurationId());
		this.downloadTask.setS3ArchiveConfigurationId(downloadTask.getS3ArchiveConfigurationId());
		this.downloadTask.setCompletionEvent(downloadTask.getCompletionEvent());
		this.downloadTask.setCollectionDownloadTaskId(downloadTask.getCollectionDownloadTaskId());
		this.downloadTask.setArchiveLocation(downloadTask.getArchiveLocation());
		this.downloadTask.setS3DownloadDestination(downloadTask.getS3DownloadDestination());
		this.downloadTask.setGoogleDriveDownloadDestination(downloadTask.getGoogleDriveDownloadDestination());
		this.downloadTask
				.setGoogleCloudStorageDownloadDestination(downloadTask.getGoogleCloudStorageDownloadDestination());
		this.downloadTask.setAsperaDownloadDestination(downloadTask.getAsperaDownloadDestination());
		this.downloadTask.setBoxDownloadDestination(downloadTask.getBoxDownloadDestination());
		this.downloadTask.setCreated(downloadTask.getCreated());
		this.downloadTask.setPercentComplete(0);
		this.downloadTask.setSize(downloadTask.getSize());
		this.downloadTask.setFirstHopRetried(downloadTask.getFirstHopRetried());
		this.downloadTask.setRetryTaskId(downloadTask.getRetryTaskId());
		this.downloadTask.setRetryUserId(downloadTask.getRetryUserId());

		if (this.downloadTask.getS3DownloadDestination() != null) {
			this.downloadTask.setDataTransferType(HpcDataTransferType.S_3);
			this.downloadTask.setDestinationType(HpcDataTransferType.S_3);
		} else if (this.downloadTask.getGoogleDriveDownloadDestination() != null) {
			this.downloadTask.setDataTransferType(HpcDataTransferType.GOOGLE_DRIVE);
			this.downloadTask.setDestinationType(HpcDataTransferType.GOOGLE_DRIVE);
		} else if (this.downloadTask.getGoogleCloudStorageDownloadDestination() != null) {
			this.downloadTask.setDataTransferType(HpcDataTransferType.GOOGLE_CLOUD_STORAGE);
			this.downloadTask.setDestinationType(HpcDataTransferType.GOOGLE_CLOUD_STORAGE);
		} else if (this.downloadTask.getAsperaDownloadDestination() != null) {
			this.downloadTask.setDataTransferType(HpcDataTransferType.ASPERA);
			this.downloadTask.setDestinationType(HpcDataTransferType.ASPERA);
		} else if (this.downloadTask.getBoxDownloadDestination() != null) {
			this.downloadTask.setDataTransferType(HpcDataTransferType.BOX);
			this.downloadTask.setDestinationType(HpcDataTransferType.BOX);
		}

		dataDownloadDAO.updateDataObjectDownloadTask(this.downloadTask);
	}

	/**
	 * Complete this streaming download task, and send event.
	 *
	 * @param result           The download task result.
	 * @param message          The message to include in the download failed event.
	 * @param bytesTransferred Total bytes transfered in this download task.
	 */
	private void completeDownloadTask(HpcDownloadResult result, String message, long bytesTransferred) {
		try {
			if (!StringUtils.isEmpty(downloadTask.getDownloadFilePath())) {
				// The task was cancelled / removed from the DB. Do some cleanup.
				FileUtils.deleteQuietly(new File(downloadTask.getDownloadFilePath()));
			}

			Calendar completed = Calendar.getInstance();
			dataTransferService.completeDataObjectDownloadTask(downloadTask, result, message, completed,
					bytesTransferred);

			HpcFileLocation destinationLocation = null;
			HpcDataTransferType destinationType = null;
			if (downloadTask.getS3DownloadDestination() != null) {
				destinationLocation = downloadTask.getS3DownloadDestination().getDestinationLocation();
				destinationType = HpcDataTransferType.S_3;
			} else if (downloadTask.getGoogleDriveDownloadDestination() != null) {
				destinationLocation = downloadTask.getGoogleDriveDownloadDestination().getDestinationLocation();
				destinationType = HpcDataTransferType.GOOGLE_DRIVE;
			} else if (downloadTask.getGoogleCloudStorageDownloadDestination() != null) {
				destinationLocation = downloadTask.getGoogleCloudStorageDownloadDestination().getDestinationLocation();
				destinationType = HpcDataTransferType.GOOGLE_CLOUD_STORAGE;
			} else if (downloadTask.getAsperaDownloadDestination() != null) {
				destinationLocation = downloadTask.getAsperaDownloadDestination().getDestinationLocation();
				destinationType = HpcDataTransferType.ASPERA;
			} else if (downloadTask.getBoxDownloadDestination() != null) {
				destinationLocation = downloadTask.getBoxDownloadDestination().getDestinationLocation();
				destinationType = HpcDataTransferType.BOX;
			}

			// Send a download completion or failed event (if requested to).
			if (downloadTask.getCompletionEvent()) {
				if (result.equals(HpcDownloadResult.COMPLETED)) {
					eventService.addDataTransferDownloadCompletedEvent(downloadTask.getUserId(), downloadTask.getPath(),
							HpcDownloadTaskType.DATA_OBJECT, downloadTask.getId(), destinationLocation, completed,
							destinationType);
				} else {
					eventService.addDataTransferDownloadFailedEvent(downloadTask.getUserId(), downloadTask.getPath(),
							HpcDownloadTaskType.DATA_OBJECT, result, downloadTask.getId(), destinationLocation,
							completed, message, destinationType);
				}
			}

		} catch (HpcException e) {
			logger.error("Failed to complete Streaming download task", e);
		}
	}
}
