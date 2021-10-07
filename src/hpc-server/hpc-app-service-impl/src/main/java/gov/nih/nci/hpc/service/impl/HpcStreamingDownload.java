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

import java.util.Calendar;

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
 * A data transfer listener for async streaming downloads. The streaming is done
 * from the S3 archive to AWS or Google Drive.
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
		// This callback method is called when streaming download failed.
		completeDownloadTask(HpcDownloadResult.FAILED, message, 0);
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
		downloadTask.setArchiveLocation(downloadRequest.getArchiveLocation());
		downloadTask.setS3DownloadDestination(downloadRequest.getS3Destination());
		downloadTask.setGoogleDriveDownloadDestination(downloadRequest.getGoogleDriveDestination());
		downloadTask.setGoogleCloudStorageDownloadDestination(downloadRequest.getGoogleCloudStorageDestination());
		downloadTask.setCreated(Calendar.getInstance());
		downloadTask.setPercentComplete(0);
		downloadTask.setSize(downloadRequest.getSize());

		if (downloadTask.getS3DownloadDestination() != null) {
			downloadTask.setDataTransferType(HpcDataTransferType.S_3);
			downloadTask.setDestinationType(HpcDataTransferType.S_3);
		} else if (downloadTask.getGoogleDriveDownloadDestination() != null) {
			downloadTask.setDataTransferType(HpcDataTransferType.GOOGLE_DRIVE);
			downloadTask.setDestinationType(HpcDataTransferType.GOOGLE_DRIVE);
		} else if (downloadTask.getGoogleCloudStorageDownloadDestination() != null) {
			downloadTask.setDataTransferType(HpcDataTransferType.GOOGLE_CLOUD_STORAGE);
			downloadTask.setDestinationType(HpcDataTransferType.GOOGLE_CLOUD_STORAGE);
		}

		dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
	}

	/**
	 * Update a download task for a streaming download (to AWS S3 or Google Drive).
	 *
	 * @param downloadTask The download task.
	 * @throws HpcException If it failed to persist the task.
	 */
	private void updateDownloadTask(HpcDataObjectDownloadTask downloadTask) throws HpcException {
		this.downloadTask.setId(downloadTask.getId());
		this.downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.IN_PROGRESS);
		this.downloadTask.setDownloadFilePath(null);
		this.downloadTask.setUserId(downloadTask.getUserId());
		this.downloadTask.setPath(downloadTask.getPath());
		this.downloadTask.setConfigurationId(downloadTask.getConfigurationId());
		this.downloadTask.setS3ArchiveConfigurationId(downloadTask.getS3ArchiveConfigurationId());
		this.downloadTask.setCompletionEvent(downloadTask.getCompletionEvent());
		this.downloadTask.setArchiveLocation(downloadTask.getArchiveLocation());
		this.downloadTask.setS3DownloadDestination(downloadTask.getS3DownloadDestination());
		this.downloadTask.setGoogleDriveDownloadDestination(downloadTask.getGoogleDriveDownloadDestination());
		this.downloadTask.setCreated(downloadTask.getCreated());
		this.downloadTask.setPercentComplete(0);
		this.downloadTask.setSize(downloadTask.getSize());

		if (this.downloadTask.getS3DownloadDestination() != null) {
			this.downloadTask.setDataTransferType(HpcDataTransferType.S_3);
			this.downloadTask.setDestinationType(HpcDataTransferType.S_3);
		} else if (this.downloadTask.getGoogleDriveDownloadDestination() != null) {
			this.downloadTask.setDataTransferType(HpcDataTransferType.GOOGLE_DRIVE);
			this.downloadTask.setDestinationType(HpcDataTransferType.GOOGLE_DRIVE);
		}

		dataDownloadDAO.upsertDataObjectDownloadTask(this.downloadTask);
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
			Calendar completed = Calendar.getInstance();
			dataTransferService.completeDataObjectDownloadTask(downloadTask, result, message, completed,
					bytesTransferred);

			HpcFileLocation destinationLocation = null;
			if (downloadTask.getS3DownloadDestination() != null) {
				destinationLocation = downloadTask.getS3DownloadDestination().getDestinationLocation();
			} else if (downloadTask.getGoogleDriveDownloadDestination() != null) {
				destinationLocation = downloadTask.getGoogleDriveDownloadDestination().getDestinationLocation();
			} else if (downloadTask.getGoogleCloudStorageDownloadDestination() != null) {
				destinationLocation = downloadTask.getGoogleCloudStorageDownloadDestination().getDestinationLocation();
			}

			// Send a download completion or failed event (if requested to).
			if (downloadTask.getCompletionEvent()) {
				if (result.equals(HpcDownloadResult.COMPLETED)) {
					eventService.addDataTransferDownloadCompletedEvent(downloadTask.getUserId(), downloadTask.getPath(),
							HpcDownloadTaskType.DATA_OBJECT, downloadTask.getId(), destinationLocation, completed);
				} else {
					eventService.addDataTransferDownloadFailedEvent(downloadTask.getUserId(), downloadTask.getPath(),
							HpcDownloadTaskType.DATA_OBJECT, downloadTask.getId(), destinationLocation, completed,
							message);
				}
			}

		} catch (HpcException e) {
			logger.error("Failed to complete Streaming download task", e);
		}
	}
}
