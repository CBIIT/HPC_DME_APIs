/**
 * HpcSystemBusServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.CollectionUtils;

import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.bus.HpcSystemBusService;
import gov.nih.nci.hpc.bus.aspect.HpcExecuteAsSystemAccount;
import gov.nih.nci.hpc.domain.datamanagement.HpcBulkDataObjectRegistrationTaskStatus;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObjectRegistrationTaskItem;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadMethod;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDeepArchiveStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcGoogleDriveDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3ObjectMetadata;
import gov.nih.nci.hpc.domain.datatransfer.HpcStreamingUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadSource;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationItem;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationRequest;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.domain.report.HpcReportType;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcEventService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcNotificationService;
import gov.nih.nci.hpc.service.HpcReportService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * HPC System Business Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcSystemBusServiceImpl implements HpcSystemBusService {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// Security Application Service Instance.
	@Autowired
	private HpcSecurityService securityService = null;

	// Data Transfer Application Service Instance.
	@Autowired
	private HpcDataTransferService dataTransferService = null;

	// Data Management Application Service Instance.
	@Autowired
	private HpcDataManagementService dataManagementService = null;

	// Data Management Bus Service Instance.
	@Autowired
	private HpcDataManagementBusService dataManagementBusService = null;

	// Notification Application Service Instance.
	@Autowired
	private HpcNotificationService notificationService = null;

	// Event Application Service Instance.
	@Autowired
	private HpcEventService eventService = null;

	// Metadata Application Service Instance
	@Autowired
	private HpcMetadataService metadataService = null;

	// Reports Application Service Instance
	@Autowired
	private HpcReportService reportService = null;

	// The collection download task executor.
	@Autowired
	@Qualifier("hpcCollectionDownloadTaskExecutor")
	Executor collectionDownloadTaskExecutor = null;

	// The collection download task executor.
	@Autowired
	@Qualifier("hpcDataObjectDownloadTaskExecutor")
	Executor dataObjectDownloadTaskExecutor = null;

	
	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcSystemBusServiceImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcSystemBusService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	@HpcExecuteAsSystemAccount
	public void processDataTranferUploadReceived() throws HpcException {
		// Iterate through the data objects that their data transfer is in-progress.
		List<HpcDataObject> dataObjectsReceived = dataManagementService.getDataObjectsUploadReceived();
		for (HpcDataObject dataObject : dataObjectsReceived) {
			String path = dataObject.getAbsolutePath();
			logger.info("Processing data object upload received: {}", path);
			try {
				// Get the system metadata.
				HpcSystemGeneratedMetadata systemGeneratedMetadata = metadataService
						.getDataObjectSystemGeneratedMetadata(path);

				// Transfer the data file.
				HpcDataObjectUploadResponse uploadResponse = dataTransferService.uploadDataObject(
						toGlobusUploadSource(systemGeneratedMetadata.getSourceLocation()), null, null, null, null,
						false, null, null, path, systemGeneratedMetadata.getObjectId(),
						systemGeneratedMetadata.getRegistrarId(), systemGeneratedMetadata.getCallerObjectId(),
						systemGeneratedMetadata.getConfigurationId());

				// Update system metadata of the data object.
				metadataService.updateDataObjectSystemGeneratedMetadata(path, uploadResponse.getArchiveLocation(),
						uploadResponse.getDataTransferRequestId(), null, uploadResponse.getDataTransferStatus(),
						uploadResponse.getDataTransferType(), null, uploadResponse.getDataTransferCompleted(), null,
						null, null, null);

			} catch (HpcException e) {
				logger.error("Failed to process queued data transfer upload :" + path, e);

				// Process the data object registration failure.
				processDataObjectRegistrationFailure(path, e.getMessage());
			}
		}
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processDataTranferUploadInProgress() throws HpcException {
		// Iterate through the data objects that their data transfer is in-progress.
		List<HpcDataObject> dataObjectsInProgress = dataManagementService.getDataObjectsUploadInProgress();
		for (HpcDataObject dataObject : dataObjectsInProgress) {
			String path = dataObject.getAbsolutePath();

			logger.info("Processing data object upload in-progress: {}", path);
			try {
				// Get the system metadata.
				HpcSystemGeneratedMetadata systemGeneratedMetadata = metadataService
						.getDataObjectSystemGeneratedMetadata(path);

				// Get the data transfer upload request status.
				HpcDataTransferUploadReport dataTransferUploadReport = dataTransferService.getDataTransferUploadStatus(
						systemGeneratedMetadata.getDataTransferType(),
						systemGeneratedMetadata.getDataTransferRequestId(),
						systemGeneratedMetadata.getConfigurationId(),
						systemGeneratedMetadata.getS3ArchiveConfigurationId());

				HpcDataTransferUploadStatus dataTransferStatus = dataTransferUploadReport.getStatus();
				Calendar dataTransferCompleted = null;
				switch (dataTransferStatus) {
				case ARCHIVED:
					// Data object is archived. Note: This is a configured filesystem archive.

					// Generate archive (File System) system generated metadata.
					HpcS3ObjectMetadata objectMetadata = dataTransferService.addSystemGeneratedMetadataToDataObject(
							systemGeneratedMetadata.getArchiveLocation(), systemGeneratedMetadata.getDataTransferType(),
							systemGeneratedMetadata.getConfigurationId(),
							systemGeneratedMetadata.getS3ArchiveConfigurationId(),
							systemGeneratedMetadata.getObjectId(), systemGeneratedMetadata.getRegistrarId());

					// Update data management w/ data transfer status, checksum and completion time.
					dataTransferCompleted = Calendar.getInstance();
					Calendar deepArchiveDate = objectMetadata.getDeepArchiveStatus() != null && objectMetadata.getDeepArchiveStatus().equals(HpcDeepArchiveStatus.IN_PROGRESS) ? Calendar.getInstance() : null;
					metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, objectMetadata.getChecksum(),
							dataTransferStatus, null, null, dataTransferCompleted, null, null, objectMetadata.getDeepArchiveStatus(), deepArchiveDate);

					// Record data object registration result.
					systemGeneratedMetadata.setDataTransferCompleted(dataTransferCompleted);
					dataManagementService.addDataObjectRegistrationResult(path, systemGeneratedMetadata, true, null);

					break;

				case IN_TEMPORARY_ARCHIVE:
					// Data object is in temporary archive (This is a scratch space on DME server used for 2-hop upload)
					// Globus completed transfer to the temporary archive. File will be uploaded to S3 Archive next when
					// the processTemporaryArchive() scheduled task is called.

					// Update data transfer status.
					metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null, dataTransferStatus,
							null, null, null, null, null, null, null);
					break;

				case FAILED:
					// Data transfer failed.

					throw new HpcException("Data transfer failed: " + dataTransferUploadReport.getMessage(),
							HpcErrorType.DATA_TRANSFER_ERROR);

				default:
					// Transfer is still in progress.
					continue;
				}

				// Data transfer upload completed (successfully or failed). Add an event if
				// needed.
				if (systemGeneratedMetadata.getRegistrationCompletionEvent()) {
					addDataTransferUploadEvent(systemGeneratedMetadata.getRegistrarId(), path, dataTransferStatus,
							systemGeneratedMetadata.getSourceLocation(), dataTransferCompleted,
							systemGeneratedMetadata.getDataTransferType(), systemGeneratedMetadata.getConfigurationId(),
							HpcDataTransferType.GLOBUS);
				}

			} catch (HpcException e) {
				logger.error("Failed to process data transfer upload in progress:" + path, e);

				// Process the data object registration failure.
				processDataObjectRegistrationFailure(path, e.getMessage());
			}
		}

	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processDataTranferUploadInProgressWithGeneratedURL() throws HpcException {
		// Iterate through the data objects that their data transfer is in-progress.
		List<HpcDataObject> dataObjectsInProgress = dataManagementService
				.getDataTranferUploadInProgressWithGeneratedURL();
		for (HpcDataObject dataObject : dataObjectsInProgress) {
			String path = dataObject.getAbsolutePath();
			logger.info("Processing data object uploaded via URL: {}", path);

			// Get the system metadata.
			HpcSystemGeneratedMetadata systemGeneratedMetadata = metadataService
					.getDataObjectSystemGeneratedMetadata(path);

			try {
				if (!updateS3UploadStatus(path, systemGeneratedMetadata) && dataTransferService.uploadURLExpired(
						systemGeneratedMetadata.getDataTransferStarted(), systemGeneratedMetadata.getConfigurationId(),
						systemGeneratedMetadata.getS3ArchiveConfigurationId())) {
					// The data object was not found in archive. i.e. user did not complete the
					// upload and the upload URL has expired.

					// Delete the data object.
					dataManagementService.delete(path, true);

					// Add event.
					addDataTransferUploadEvent(systemGeneratedMetadata.getRegistrarId(), path,
							HpcDataTransferUploadStatus.URL_GENERATED, null, null,
							systemGeneratedMetadata.getDataTransferType(), systemGeneratedMetadata.getConfigurationId(),
							HpcDataTransferType.S_3);

					// Record a registration result.
					dataManagementService.addDataObjectRegistrationResult(path, systemGeneratedMetadata, false,
							"Presigned upload URL expired");
				}

			} catch (HpcException e) {
				logger.error("Failed to process data transfer upload in progress with URL:" + path, e);

				// This exception prevented us from verifying whether the file was uploaded or
				// not by the
				// time the URL expired, so we cannot delete the metadata. This method will be
				// invoked
				// the next time around this task is executed, so if this exception was due to a
				// temp
				// reason such as S3 archive not being accessible, it should be resolved within
				// the next
				// or a subsequent round, and we should then be able to do proper processing
				// without
				// getting into this catch block.
				// deleteDataObject(path);

			}
		}
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processDataTranferUploadStreamingInProgress(boolean streamingStopped) throws HpcException {
		// Iterate through the data objects that their data transfer is in-progress.
		List<HpcDataObject> dataObjectsInProgress = dataManagementService.getDataTranferUploadStreamingInProgress();
		for (HpcDataObject dataObject : dataObjectsInProgress) {
			String path = dataObject.getAbsolutePath();
			logger.info("Processing data object uploaded via Streaming: {}", path);
			try {
				if (!streamingStopped) {
					// Get the system metadata.
					HpcSystemGeneratedMetadata systemGeneratedMetadata = metadataService
							.getDataObjectSystemGeneratedMetadata(path);

					// Check if the S3 upload completed, and update upload status accordingly.
					updateS3UploadStatus(path, systemGeneratedMetadata);
				} else {
					// Streaming stopped (server shutdown). We just update the status accordingly.
					logger.info("Upload streaming stopped for: {}", path);
					metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null,
							HpcDataTransferUploadStatus.STREAMING_STOPPED, null, null, null, null, null, null, null);
				}

			} catch (HpcException e) {
				logger.error("Failed to process data transfer upload streaming in progress:" + path, e);

				// Process the data object registration failure.
				processDataObjectRegistrationFailure(path, e.getMessage());
			}
		}
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processDataTranferUploadStreamingInProgress() throws HpcException {
		processDataTranferUploadStreamingInProgress(false);
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processDataTranferUploadStreamingStopped() throws HpcException {
		// Iterate through the data objects that their data transfer (S3 / Google Drive
		// streaming) has
		// stopped.
		List<HpcDataObject> dataObjectsStreamingStopped = dataManagementService.getDataTranferUploadStreamingStopped();
		for (HpcDataObject dataObject : dataObjectsStreamingStopped) {
			String path = dataObject.getAbsolutePath();
			logger.info("Processing restart upload streaming for data object: {}", path);
			try {
				// Get the system metadata.
				HpcSystemGeneratedMetadata systemGeneratedMetadata = metadataService
						.getDataObjectSystemGeneratedMetadata(path);

				// Transfer the data file.
				HpcDataObjectUploadResponse uploadResponse = dataTransferService.uploadDataObject(
						null,
						toS3UploadSource(
								systemGeneratedMetadata.getDataTransferMethod(),
								systemGeneratedMetadata.getSourceLocation(), systemGeneratedMetadata.getSourceURL(),
								systemGeneratedMetadata.getSourceSize()),
						toGoogleDriveUploadSource(systemGeneratedMetadata.getDataTransferMethod(),
								systemGeneratedMetadata.getSourceLocation(), systemGeneratedMetadata.getSourceURL(),
								systemGeneratedMetadata.getSourceSize()),
						null, null, false, null, null, path, systemGeneratedMetadata.getObjectId(),
						systemGeneratedMetadata.getRegistrarId(), systemGeneratedMetadata.getCallerObjectId(),
						systemGeneratedMetadata.getConfigurationId());

				// Update the transfer status and request id.
				metadataService.updateDataObjectSystemGeneratedMetadata(path, null,
						uploadResponse.getDataTransferRequestId(), null, uploadResponse.getDataTransferStatus(), null,
						null, null, null, null, null, null);

			} catch (HpcException e) {
				logger.error("Failed to process restart upload streaming for data object:" + path, e);

				// Process the data object registration failure.
				processDataObjectRegistrationFailure(path, e.getMessage());
			}
		}
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processTemporaryArchive() throws HpcException {
		// Iterate through the data objects that their data is in temporary archive.
		List<HpcDataObject> dataObjectsInTemporaryArchive = dataManagementService
				.getDataObjectsUploadInTemporaryArchive();
		logger.info("{} Data Objects Upload In Temporary Archive: {}", dataObjectsInTemporaryArchive.size(),
				dataObjectsInTemporaryArchive);
		uploadDataObjectsFile(dataObjectsInTemporaryArchive, true);
				}

	@Override
	@HpcExecuteAsSystemAccount
	public void processFileSystemUpload() throws HpcException {
		// Iterate through the data objects that their data staged in file system for an
		// upload.
		List<HpcDataObject> dataObjectsInFileSystem = dataManagementService.getDataObjectsUploadInFileSystem();
		logger.info("{} Data Objects Upload from File System: {}", dataObjectsInFileSystem.size(),
				dataObjectsInFileSystem);
		uploadDataObjectsFile(dataObjectsInFileSystem, false);
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void startGlobusDataObjectDownloadTasks() throws HpcException {
		// Iterate through all the data object download tasks that are received and type
		// is GLOBUS.
		processDataObjectDownloadTasks(HpcDataTransferDownloadStatus.RECEIVED, HpcDataTransferType.GLOBUS);
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void startS3DataObjectDownloadTasks() throws HpcException {
		// Iterate through all the data object download tasks that are received and type
		// is S3.
		processDataObjectDownloadTasks(HpcDataTransferDownloadStatus.RECEIVED, HpcDataTransferType.S_3);
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void startGoogleDriveDataObjectDownloadTasks() throws HpcException {
		// Iterate through all the data object download tasks that are received and type
		// is
		// GOOGLE_DRIVE.
		processDataObjectDownloadTasks(HpcDataTransferDownloadStatus.RECEIVED, HpcDataTransferType.GOOGLE_DRIVE);
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void completeInProgressDataObjectDownloadTasks() throws HpcException {
		// Iterate through all the data object download tasks that are in-progress.
		processDataObjectDownloadTasks(HpcDataTransferDownloadStatus.IN_PROGRESS, null);
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void completeCanceledDataObjectDownloadTasks() throws HpcException {
		// Iterate through all the data object download tasks that are in-progress.
		processDataObjectDownloadTasks(HpcDataTransferDownloadStatus.CANCELED, null);
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void restartDataObjectDownloadTasks() throws HpcException {
		// Iterate through all the data object download tasks that are in-progress w/ S3
		// transfer.
		for (HpcDataObjectDownloadTask downloadTask : dataTransferService.getDataObjectDownloadTasks()) {
			try {
				if ((downloadTask.getDataTransferType().equals(HpcDataTransferType.S_3)
						|| downloadTask.getDataTransferType().equals(HpcDataTransferType.GOOGLE_DRIVE))
						&& downloadTask.getDataTransferStatus().equals(HpcDataTransferDownloadStatus.IN_PROGRESS)) {
					logger.info("Resetting download task: {}", downloadTask.getId());
					dataTransferService.resetDataObjectDownloadTask(downloadTask);
				}

			} catch (HpcException e) {
				logger.error("Failed to restart data-object download task: " + downloadTask.getId(), e);
			}
		}

		// Set all in-process indicator to false;
		dataTransferService.resetDataObjectDownloadTasksInProcess();
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void restartCollectionDownloadTasks() throws HpcException {
		// Iterate through all the collection download tasks that are in-process
		dataTransferService.getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus.RECEIVED, true)
				.forEach(downloadTask -> {
					try {
						dataTransferService.resetCollectionDownloadTaskInProgress(downloadTask.getId());

					} catch (HpcException e) {
						logger.error("Failed to restart collection download task: " + downloadTask.getId(), e);
					}
				});
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processCollectionDownloadTasks() throws HpcException {
		// Iterate through all the collection download requests that were submitted (not
		// processed yet).
		for (HpcCollectionDownloadTask downloadTask : dataTransferService
				.getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus.RECEIVED, false)) {
			logger.info("collection download task: {} - started processing [{}]", downloadTask.getId(),
					downloadTask.getType());

			if (dataTransferService.getCollectionDownloadTaskCancellationRequested(downloadTask.getId())) {
				// User requested to cancel this collection download task.
				completeCollectionDownloadTask(downloadTask, HpcDownloadResult.CANCELED, "Download request canceled");
				continue;
			}

			// We limit to one collection download task processing per user at a time.
			int tasksInProgressCount = dataTransferService.getCollectionDownloadTasksCount(downloadTask.getUserId(),
					HpcCollectionDownloadTaskStatus.RECEIVED, true);
			if (tasksInProgressCount > 0) {
				// Another collection download tasks in in-process (other thread) for this user.
				logger.info(
						"collection download task: {} - Not processing at this time. {} tasks in-process for user: {}",
						downloadTask.getId(), tasksInProgressCount, downloadTask.getUserId());
				continue;
			}

			// Mark this collection download task in-process.
			dataTransferService.setCollectionDownloadTaskInProgress(downloadTask.getId(), true);

			// Process this collection download task async.
			CompletableFuture.runAsync(() -> {
				try {
					// Since this is executed in a separate thread. Need to get system-account
					// execution again.
					securityService.executeAsSystemAccount(Optional.empty(), () -> {
						try {
							List<HpcCollectionDownloadTaskItem> downloadItems = null;
							HpcCollectionDownloadBreaker collectionDownloadBreaker = new HpcCollectionDownloadBreaker(
									downloadTask.getId());

							if (downloadTask.getType().equals(HpcDownloadTaskType.COLLECTION)) {
								// Get the collection to be downloaded.
								HpcCollection collection = dataManagementService.getCollection(downloadTask.getPath(),
										true);
								if (collection == null) {
									throw new HpcException("Collection not found", HpcErrorType.INVALID_REQUEST_INPUT);
								}

								// Download all files under this collection.
								downloadItems = downloadCollection(collection,
										downloadTask.getGlobusDownloadDestination(),
										downloadTask.getS3DownloadDestination(),
										downloadTask.getGoogleDriveDownloadDestination(),
										downloadTask.getAppendPathToDownloadDestination(), downloadTask.getUserId(),
										collectionDownloadBreaker);

							} else if (downloadTask.getType().equals(HpcDownloadTaskType.DATA_OBJECT_LIST)) {
								downloadItems = downloadDataObjects(downloadTask.getDataObjectPaths(),
										downloadTask.getGlobusDownloadDestination(),
										downloadTask.getS3DownloadDestination(),
										downloadTask.getGoogleDriveDownloadDestination(),
										downloadTask.getAppendPathToDownloadDestination(), downloadTask.getUserId());

							} else if (downloadTask.getType().equals(HpcDownloadTaskType.COLLECTION_LIST)) {
								downloadItems = new ArrayList<>();
								for (String path : downloadTask.getCollectionPaths()) {
									HpcCollection collection = dataManagementService.getCollection(path, true);
									if (collection == null) {
										throw new HpcException("Collection not found",
												HpcErrorType.INVALID_REQUEST_INPUT);
									}
									downloadItems.addAll(
											downloadCollection(collection, downloadTask.getGlobusDownloadDestination(),
													downloadTask.getS3DownloadDestination(),
													downloadTask.getGoogleDriveDownloadDestination(),
													downloadTask.getAppendPathToDownloadDestination(),
													downloadTask.getUserId(), collectionDownloadBreaker));
								}
							}

							// Verify data objects found under this collection.
							if (downloadItems == null || downloadItems.isEmpty()) {
								// No data objects found under this collection.
								throw new HpcException("No data objects found under collection",
										HpcErrorType.INVALID_REQUEST_INPUT);
							}

							// 'Activate' the collection download request.
							downloadTask.setStatus(HpcCollectionDownloadTaskStatus.ACTIVE);
							downloadTask.getItems().addAll(downloadItems);

							// Persist the collection download task.
							dataTransferService.updateCollectionDownloadTask(downloadTask);

							logger.info("collection download task: {} - finished processing [{}]", downloadTask.getId(),
									downloadTask.getType());

						} catch (HpcException e) {
							logger.error("Failed to process a collection download: " + downloadTask.getId(), e);
							try {
								completeCollectionDownloadTask(downloadTask, HpcDownloadResult.FAILED, e.getMessage());

							} catch (HpcException ex) {
								logger.error("Failed to complete collection download as failed {}",
										downloadTask.getId(), ex);
							}
						}
					});
				} catch (HpcException e) {
					logger.error("Failed to execute collection download task as system account", e);
				}

			}, collectionDownloadTaskExecutor);
		}
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void completeCollectionDownloadTasks() throws HpcException {
		// Iterate through all the active collection download requests.
		for (HpcCollectionDownloadTask downloadTask : dataTransferService
				.getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus.ACTIVE)) {
			boolean downloadCompleted = true;

			// Update status of individual download items in this collection download task.
			for (HpcCollectionDownloadTaskItem downloadItem : downloadTask.getItems()) {
				try {
					if (downloadItem.getResult() == null) {
						// This download item in progress - check its status.
						HpcDownloadTaskStatus downloadItemStatus = downloadItem.getDataObjectDownloadTaskId() != null
								? dataTransferService.getDownloadTaskStatus(downloadItem.getDataObjectDownloadTaskId(),
										HpcDownloadTaskType.DATA_OBJECT)
								: null;

						if (downloadItemStatus == null) {
							throw new HpcException("Data object download task status is unknown",
									HpcErrorType.UNEXPECTED_ERROR);
						}

						if (!downloadItemStatus.getInProgress()) {
							// This download item is now complete. Update the result.
							downloadItem.setResult(downloadItemStatus.getResult().getResult());
							downloadItem.setMessage(downloadItemStatus.getResult().getMessage());
							downloadItem.setPercentComplete(
									downloadItemStatus.getResult().getResult().equals(HpcDownloadResult.COMPLETED) ? 100
											: 0);
							downloadItem.setEffectiveTransferSpeed(
									downloadItemStatus.getResult().getEffectiveTransferSpeed() > 0
											? downloadItemStatus.getResult().getEffectiveTransferSpeed()
											: null);

							if (downloadItem.getResult().equals(HpcDownloadResult.FAILED_PERMISSION_DENIED)) {
								// This item failed because of permission denied.
								// Cancel any pending download items (i.e. items in RECEIVED state).
								dataTransferService.cancelCollectionDownloadTask(downloadTask);
								logger.info("collection download task: {} - detected permission denied [{}]",
										downloadTask.getId(), downloadTask.getType().value());
							}

							if (downloadItem.getResult().equals(HpcDownloadResult.FAILED_CREDENTIALS_NEEDED)) {
								// This item failed because of credentials are needed.
								// Cancel any pending download items (i.e. items in RECEIVED state).
								dataTransferService.cancelCollectionDownloadTask(downloadTask);
								logger.info("collection download task: {} - detected credentials are needed [{}]",
										downloadTask.getId(), downloadTask.getType().value());
							}

						} else {
							// Update the progress on this download item.
							downloadItem.setSize(downloadItemStatus.getDataObjectDownloadTask().getSize());
							downloadItem.setPercentComplete(
									downloadItemStatus.getDataObjectDownloadTask().getPercentComplete());

							// This item still in progress, so overall download not completed just yet.
							downloadCompleted = false;
						}
					}

				} catch (HpcException e) {
					logger.error("Failed to check collection download item status", e);
					downloadItem.setResult(HpcDownloadResult.FAILED);
					downloadItem.setMessage(e.getMessage());
				}
			}

			// Update the collection download task.
			if (downloadCompleted) {
				// The collection download task finished. Determine if the collection download
				// was successful.
				// It is successful if and only if all items (data objects under the collection)
				// were completed successfully.
				int completedItemsCount = 0;
				int canceledItemsCount = 0;
				for (HpcCollectionDownloadTaskItem downloadItem : downloadTask.getItems()) {
					if (downloadItem.getResult().equals(HpcDownloadResult.COMPLETED)) {
						completedItemsCount++;
					} else if (downloadItem.getResult().equals(HpcDownloadResult.CANCELED)) {
						canceledItemsCount++;
					}
				}

				// Determine the collection download result.
				int itemsCount = downloadTask.getItems().size();
				HpcDownloadResult result = null;
				String message = null;

				if (canceledItemsCount > 0
						|| dataTransferService.getCollectionDownloadTaskCancellationRequested(downloadTask.getId())) {
					result = HpcDownloadResult.CANCELED;
					message = "Download request canceled. " + completedItemsCount
							+ " items downloaded successfully out of " + itemsCount;
				} else if (completedItemsCount == itemsCount) {
					result = HpcDownloadResult.COMPLETED;
				} else {
					result = HpcDownloadResult.FAILED;
					message = completedItemsCount + " items downloaded successfully out of " + itemsCount;
				}

				completeCollectionDownloadTask(downloadTask, result, message);

			} else {
				dataTransferService.updateCollectionDownloadTask(downloadTask);
			}
		}
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processBulkDataObjectRegistrationTasks() throws HpcException {
		// Iterate through all the bulk data object registration requests that were
		// submitted (not processed yet).
		dataManagementService.getBulkDataObjectRegistrationTasks(HpcBulkDataObjectRegistrationTaskStatus.RECEIVED)
				.forEach(bulkRegistrationTask -> {
					try {
						// 'Activate' the registration task.
						bulkRegistrationTask.setStatus(HpcBulkDataObjectRegistrationTaskStatus.ACTIVE);

						// Register all items in this bulk registration task.
						bulkRegistrationTask.getItems()
								.forEach(item -> registerDataObject(item, bulkRegistrationTask.getUserId()));

						// Persist the bulk data object registration task.
						dataManagementService.updateBulkDataObjectRegistrationTask(bulkRegistrationTask);

					} catch (HpcException e) {
						logger.error(
								"Failed to process a bulk data object registration: " + bulkRegistrationTask.getId(),
								e);
						completeBulkDataObjectRegistrationTask(bulkRegistrationTask, false, e.getMessage());
					}
				});
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void completeBulkDataObjectRegistrationTasks() throws HpcException {
		// Iterate through all the bulk data object registration requests that are
		// active.
		dataManagementService.getBulkDataObjectRegistrationTasks(HpcBulkDataObjectRegistrationTaskStatus.ACTIVE)
				.forEach(bulkRegistrationTask -> {
					// Update status of items in this bulk registration task.
					bulkRegistrationTask.getItems().forEach(this::updateRegistrationItemStatus);

					// Check if registration task completed.
					int completedItemsCount = 0;
					for (HpcBulkDataObjectRegistrationItem registrationItem : bulkRegistrationTask.getItems()) {
						if (registrationItem.getTask().getResult() == null) {
							// Task still in progress. Update progress.
							try {
								dataManagementService.updateBulkDataObjectRegistrationTask(bulkRegistrationTask);

							} catch (HpcException e) {
								logger.error("Failed to update data object list task: " + bulkRegistrationTask.getId());
							}
							return;
						}

						if (registrationItem.getTask().getResult()) {
							completedItemsCount++;
						}
					}

					// Bulk registration task completed.
					int itemsCount = bulkRegistrationTask.getItems().size();
					boolean result = completedItemsCount == itemsCount;
					completeBulkDataObjectRegistrationTask(bulkRegistrationTask, result, result ? null
							: completedItemsCount + " items registered successfully out of " + itemsCount);
				});
	}

	@Override
	public void completeSynchronousDataObjectDownloadTask(String taskId, HpcDownloadResult result) throws HpcException {
		HpcDownloadTaskStatus taskStatus = dataTransferService.getDownloadTaskStatus(taskId,
				HpcDownloadTaskType.DATA_OBJECT);
		if (taskStatus == null || taskStatus.getInProgress()) {
			throw new HpcException("Invalid sync download task-id / status: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		dataTransferService.completeSynchronousDataObjectDownloadTask(taskStatus.getResult(), result,
				Calendar.getInstance());
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processEvents() throws HpcException {
		// Get and process the pending notification events.
		for (HpcEvent event : eventService.getEvents()) {
			// Notify all users associated with this event.
			try {
				for (String userId : event.getUserIds()) {
					try {
						// Get the subscription.
						HpcEventType eventType = event.getType();
						HpcNotificationSubscription subscription = notificationService
								.getNotificationSubscription(userId, eventType);
						// If event type is restoration, send out notification regardless of subscription.
						if (eventType.equals(HpcEventType.RESTORE_REQUEST_COMPLETED)
								|| eventType.equals(HpcEventType.RESTORE_REQUEST_FAILED)) {
							subscription = new HpcNotificationSubscription();
							subscription.getNotificationDeliveryMethods().add(HpcNotificationDeliveryMethod.EMAIL);
						}
						if (subscription != null) {
							// Iterate through all the delivery methods the user is subscribed to.
							for (HpcNotificationDeliveryMethod deliveryMethod : subscription
									.getNotificationDeliveryMethods()) {
								// Send notification via this delivery method.
								boolean notificationSent = notificationService.sendNotification(userId, eventType,
										event.getPayloadEntries(), deliveryMethod);

								// Create a delivery receipt for this delivery method.
								notificationService.createNotificationDeliveryReceipt(userId, event.getId(),
										deliveryMethod, notificationSent);
							}
						}

					} catch (Exception e) {
						logger.error("Failed to deliver notifications to: " + userId);
					}
				}

			} finally {
				eventService.archiveEvent(event);
			}
		}
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void generateSummaryReportEvent() throws HpcException {
		List<String> summaryReportUsers = notificationService
				.getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_REPORT);
		if (summaryReportUsers != null && !summaryReportUsers.isEmpty()) {
			HpcReportCriteria criteria = new HpcReportCriteria();
			criteria.setType(HpcReportType.USAGE_SUMMARY);
			eventService.generateReportsEvents(summaryReportUsers, criteria);
		}
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void generateWeeklySummaryReportEvent() throws HpcException {
		List<String> summaryReportByDateUsers = notificationService
				.getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_BY_WEEKLY_REPORT);
		if (summaryReportByDateUsers != null && !summaryReportByDateUsers.isEmpty()) {
			HpcReportCriteria criteria = new HpcReportCriteria();
			criteria.setType(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE);
			Calendar today = Calendar.getInstance();
			Calendar oneWeekbefore = Calendar.getInstance();
			oneWeekbefore.add(Calendar.DATE, -7);
			criteria.setFromDate(oneWeekbefore);
			criteria.setToDate(today);
			eventService.generateReportsEvents(summaryReportByDateUsers, criteria);
		}
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void generateDocReportEvent() throws HpcException {
		List<String> summaryReportUsers = notificationService
				.getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_BY_DOC_REPORT);
		if (summaryReportUsers != null && !summaryReportUsers.isEmpty()) {
			HpcReportCriteria criteria = new HpcReportCriteria();
			criteria.setType(HpcReportType.USAGE_SUMMARY_BY_DOC);
			eventService.generateReportsEvents(summaryReportUsers, criteria);
		}
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void generateWeeklyDocReportEvent() throws HpcException {
		List<String> summaryReportByDateUsers = notificationService
				.getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_BY_DOC_BY_WEEKLY_REPORT);
		if (summaryReportByDateUsers != null && !summaryReportByDateUsers.isEmpty()) {
			HpcReportCriteria criteria = new HpcReportCriteria();
			criteria.setType(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE);
			Calendar today = Calendar.getInstance();
			Calendar oneWeekbefore = Calendar.getInstance();
			oneWeekbefore.add(Calendar.DATE, -7);
			criteria.setFromDate(oneWeekbefore);
			criteria.setToDate(today);
			eventService.generateReportsEvents(summaryReportByDateUsers, criteria);
		}
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void generateUserReportEvent() throws HpcException {
		List<String> summaryReportUsers = notificationService
				.getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_BY_USER_REPORT);
		if (summaryReportUsers != null && !summaryReportUsers.isEmpty()) {
			HpcReportCriteria criteria = new HpcReportCriteria();
			criteria.setType(HpcReportType.USAGE_SUMMARY_BY_USER);
			eventService.generateReportsEvents(summaryReportUsers, criteria);
		}
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void generateWeeklyUserReportEvent() throws HpcException {
		List<String> summaryReportByDateUsers = notificationService
				.getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_BY_USER_BY_WEEKLY_REPORT);
		if (summaryReportByDateUsers != null && !summaryReportByDateUsers.isEmpty()) {
			HpcReportCriteria criteria = new HpcReportCriteria();
			criteria.setType(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE);
			Calendar today = Calendar.getInstance();
			Calendar oneWeekbefore = Calendar.getInstance();
			oneWeekbefore.add(Calendar.DATE, -7);
			criteria.setFromDate(oneWeekbefore);
			criteria.setToDate(today);
			eventService.generateReportsEvents(summaryReportByDateUsers, criteria);
		}
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void refreshMetadataViews() throws HpcException {
		metadataService.refreshViews();
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void refreshReportViews() throws HpcException {
		reportService.refreshViews();
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void completeDeepArchiveInProgress() throws HpcException {
		
		// Iterate through all the deep archive status in progress objects
		List<HpcDataObject> dataObjectsDeepArchiveInProgress = dataManagementService.getDataObjectsDeepArchiveInProgress();
		dataObjectsDeepArchiveInProgress.forEach(this::updateDeepArchiveStatus);
	}
	
	@Override
	@HpcExecuteAsSystemAccount
	public void completeRestoreRequest() throws HpcException {
		
		// Iterate through all the data object download tasks with RESTORE_REQUESTED status
		List<HpcDataObjectDownloadTask> downloadTasks = dataTransferService.getDataObjectDownloadTaskByStatus(HpcDataTransferDownloadStatus.RESTORE_REQUESTED);

		for (HpcDataObjectDownloadTask downloadTask : downloadTasks) {
			try {
				logger.info(
						"complete restore task: {} - completing restore_requested",
						downloadTask.getId());
				completeRestoreRequestedDataObjectDownloadTask(downloadTask);

			} catch (HpcException e) {
				logger.error("restore task: {} - Failed to process",
						downloadTask.getId(),
						e);
			}
		}
		
	}
	
	@Override
	public void closeConnection() {
		dataManagementService.closeConnection();
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * process data object download task in process or received
	 * 
	 * @param dataTransferStatus The data transfer status to process from the
	 *                           download task
	 * @param dataTransferType   The data transfer type to process from the download
	 *                           task
	 * @throws HpcException on service failure.
	 */
	private void processDataObjectDownloadTasks(HpcDataTransferDownloadStatus dataTransferStatus,
			HpcDataTransferType dataTransferType) throws HpcException {
		// Iterate through all the data object download tasks that are in-progress.
		List<HpcDataObjectDownloadTask> downloadTasks = null;
		Date runTimestamp = new Date();
		do {
			downloadTasks = dataTransferService.getNextDataObjectDownloadTask(dataTransferStatus, dataTransferType,
					runTimestamp);
			if (!CollectionUtils.isEmpty(downloadTasks)) {
				HpcDataObjectDownloadTask downloadTask = downloadTasks.get(0);
				try {
					// First mark the task as picked up in this run so we don't pick up the same
					// record. For tasks in RECEIVED status (which are processed concurrently in
					// separate threads),
					// we set their in-process indicator to true so they are not picked up by
					// another thread.
					boolean inProcess = Optional.ofNullable(downloadTask.getInProcess()).orElse(false);
					dataTransferService.markProcessedDataObjectDownloadTask(downloadTask, true);

					switch (downloadTask.getDataTransferStatus()) {
					case RECEIVED:
						if (inProcess) {
							// This task is in-process in another thread. Skip it
							logger.info(
									"download task: {} - continuing in-process in another thread [transfer-type={}, destination-type={}]",
									downloadTask.getId(), downloadTask.getDataTransferType(),
									downloadTask.getDestinationType());
							break;
						}

						CompletableFuture.runAsync(() -> {
							try {
								// Since this is executed in a separate thread. Need to get system-account
								// execution again.
								securityService.executeAsSystemAccount(Optional.empty(), () -> {
									try {
										logger.info(
												"download task: {} - continuing [transfer-type={}, destination-type={}]",
												downloadTask.getId(), downloadTask.getDataTransferType(),
												downloadTask.getDestinationType());
										dataTransferService.continueDataObjectDownloadTask(downloadTask);

									} catch (HpcException e) {
										logger.error(
												"download task: {} - Failed to process [transfer-type={}, destination-type={}]",
												downloadTask.getId(), downloadTask.getDataTransferType(),
												downloadTask.getDestinationType(), e);
									}
								});
							} catch (HpcException e) {
								logger.error(
										"download task: {} - Failed to execute as system account [transfer-type={}, destination-type={}]",
										downloadTask.getId(), downloadTask.getDataTransferType(),
										downloadTask.getDestinationType(), e);
							} finally {
								try {
									dataTransferService.markProcessedDataObjectDownloadTask(downloadTask, false);

								} catch (HpcException e) {
									logger.error(
											"download task: {} - Failed to reset in-process indicator [transfer-type={}, destination-type={}]",
											downloadTask.getId(), downloadTask.getDataTransferType(),
											downloadTask.getDestinationType(), e);
								}
							}

						}, dataObjectDownloadTaskExecutor);
						break;

					case IN_PROGRESS:
						logger.info(
								"download task: {} - completing in-progress [transfer-type={}, destination-type={}]",
								downloadTask.getId(), downloadTask.getDataTransferType(),
								downloadTask.getDestinationType());
						completeInProgressDataObjectDownloadTask(downloadTask);
						break;

					case CANCELED:
						logger.info(
								"download task: {} - completing cancelation [transfer-type={}, destination-type={}]",
								downloadTask.getId(), downloadTask.getDataTransferType(),
								downloadTask.getDestinationType());
						completeCanceledDataObjectDownloadTask(downloadTask);
						break;

					default:
						throw new HpcException("Unexpected data transfer download status ["
								+ downloadTask.getDataTransferStatus() + "] for task: " + downloadTask.getId(),
								HpcErrorType.UNEXPECTED_ERROR);
					}

				} catch (HpcException e) {
					logger.error("download task: {} - Failed to process [transfer-type={}, destination-type={}]",
							downloadTask.getId(), downloadTask.getDataTransferType(), downloadTask.getDestinationType(),
							e);
				}
			}
		} while (!CollectionUtils.isEmpty(downloadTasks));
	}

	/**
	 * add data transfer upload event.
	 *
	 * @param userId                 The user ID.
	 * @param path                   The data object path.
	 * @param dataTransferStatus     The data transfer upload status.
	 * @param sourceLocation         (Optional) The data transfer source location.
	 * @param dataTransferCompleted  (Optional) The time the data upload completed.
	 * @param dataTransferType       The type of data transfer last used to upload
	 *                               (Globus, S3, etc).
	 * @param configurationId        The data management configuration ID.
	 * @param sourceDataTransferType The type of source the file was uploaded from
	 *                               (S3 or Globus)
	 */
	private void addDataTransferUploadEvent(String userId, String path, HpcDataTransferUploadStatus dataTransferStatus,
			HpcFileLocation sourceLocation, Calendar dataTransferCompleted, HpcDataTransferType dataTransferType,
			String configurationId, HpcDataTransferType sourceDataTransferType) {
		setFileContainerName(sourceDataTransferType, configurationId, sourceLocation);
		try {
			switch (dataTransferStatus) {
			case ARCHIVED:
				eventService.addDataTransferUploadArchivedEvent(userId, path, sourceLocation, dataTransferCompleted);
				break;

			case IN_TEMPORARY_ARCHIVE:
				eventService.addDataTransferUploadInTemporaryArchiveEvent(userId, path);
				break;

			case FAILED:
				eventService.addDataTransferUploadFailedEvent(userId, path, sourceLocation, dataTransferCompleted,
						dataTransferType.value() + " failure");
				break;

			case URL_GENERATED:
				eventService.addDataTransferUploadURLExpiredEvent(userId, path);
				break;

			default:
				logger.error("Unexpected data transfer status: {}", dataTransferStatus);
			}

		} catch (HpcException e) {
			logger.error("Failed to add a data transfer upload event", e);
		}
	}

	/**
	 * add bulk data object registration event.
	 *
	 * @param registrationTask The bulk registration task.
	 * @param result           The bulk registration result.
	 * @param message          A failure message.
	 * @param completed        The completion time.
	 */
	private void addBulkDataObjectRegistrationEvent(HpcBulkDataObjectRegistrationTask registrationTask, boolean result,
			String message, Calendar completed) {

		// Format the task ID. If the caller provided a UI URL, then use it to construct
		// a URL link to view this task on UI.
		String taskId = registrationTask.getId();
		if (!StringUtils.isEmpty(registrationTask.getUiURL())) {
			taskId = "<a href=\"" + registrationTask.getUiURL().replaceAll("\\{task_id\\}", taskId) + "\">" + taskId
					+ "</a>";
		}

		try {
			if (result) {
				// Update the source's file container name on all registration items (so that it
				// will be displayed in the notification).
				registrationTask.getItems().forEach(item -> {
					String configurationId = dataManagementService
							.findDataManagementConfigurationId(item.getTask().getPath());
					HpcFileLocation fileLocation = null;
					HpcDataTransferType dataTransferType = null;
					if (item.getRequest().getGlobusUploadSource() != null) {
						fileLocation = item.getRequest().getGlobusUploadSource().getSourceLocation();
						dataTransferType = HpcDataTransferType.GLOBUS;
					} else if (item.getRequest().getS3UploadSource() != null) {
						fileLocation = item.getRequest().getS3UploadSource().getSourceLocation();
						dataTransferType = HpcDataTransferType.S_3;
					}
					setFileContainerName(dataTransferType, configurationId, fileLocation);
				});

				eventService.addBulkDataObjectRegistrationCompletedEvent(registrationTask.getUserId(), taskId,
						registrationTask.getItems(), completed);
			} else {
				eventService.addBulkDataObjectRegistrationFailedEvent(registrationTask.getUserId(), taskId, completed,
						message);
			}

		} catch (HpcException e) {
			logger.error("Failed to add a data transfer upload event", e);
		}
	}

	/**
	 * add data transfer download event.
	 *
	 * @param userId                The user ID.
	 * @param path                  The collection or data objection path.
	 * @param downloadTaskType      The download task type.
	 * @param downloadTaskId        The download task ID.
	 * @param dataTransferType      The data transfer type,
	 * @param configurationId       The data management configuration ID.
	 * @param result                The download result (completed, failed or
	 *                              canceled).
	 * @param message               A failure message.
	 * @param destinationLocation   The download destination location.
	 * @param dataTransferCompleted The download completion time.
	 */
	private void addDataTransferDownloadEvent(String userId, String path, HpcDownloadTaskType downloadTaskType,
			String downloadTaskId, HpcDataTransferType dataTransferType, String configurationId,
			HpcDownloadResult result, String message, HpcFileLocation destinationLocation,
			Calendar dataTransferCompleted) {
		setFileContainerName(dataTransferType, configurationId, destinationLocation);
		try {
			if (result.equals(HpcDownloadResult.COMPLETED)) {
				eventService.addDataTransferDownloadCompletedEvent(userId, path, downloadTaskType, downloadTaskId,
						destinationLocation, dataTransferCompleted);
			} else {
				eventService.addDataTransferDownloadFailedEvent(userId, path, downloadTaskType, downloadTaskId,
						destinationLocation, dataTransferCompleted, message);
			}

		} catch (HpcException e) {
			logger.error("Failed to add a data transfer download event", e);
		}
	}

	/**
	 * Download a collection. Traverse the collection tree and submit download
	 * request to all files in the tree.
	 *
	 * @param collection                      The collection to download.
	 * @param globusDownloadDestination       The user requested Glopbus download
	 *                                        destination.
	 * @param s3DownloadDestination           The user requested S3 download
	 *                                        destination.
	 * @param googleDriveDownloadDestination  The user requested Google Drive
	 *                                        download destination.
	 * @param appendPathToDownloadDestination If true, the (full) object path will
	 *                                        be used in the destination path,
	 *                                        otherwise just the object name will be
	 *                                        used.
	 * @param userId                          The user ID who requested the
	 *                                        collection download.
	 * @param collectionDownloadBreaker       A collection download breaker
	 *                                        instance.
	 * @return The download task items (each item represent a data-object download
	 *         under the collection).
	 * @throws HpcException on service failure.
	 */
	private List<HpcCollectionDownloadTaskItem> downloadCollection(HpcCollection collection,
			HpcGlobusDownloadDestination globusDownloadDestination, HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDriveDownloadDestination googleDriveDownloadDestination, boolean appendPathToDownloadDestination,
			String userId, HpcCollectionDownloadBreaker collectionDownloadBreaker) throws HpcException {
		List<HpcCollectionDownloadTaskItem> downloadItems = new ArrayList<>();

		// Iterate through the data objects in the collection and download them.
		for (HpcCollectionListingEntry dataObjectEntry : collection.getDataObjects()) {
			HpcCollectionDownloadTaskItem downloadItem = downloadDataObject(dataObjectEntry.getPath(),
					globusDownloadDestination, s3DownloadDestination, googleDriveDownloadDestination,
					appendPathToDownloadDestination, userId);
			downloadItems.add(downloadItem);
			if (collectionDownloadBreaker.abortDownload(downloadItem)) {
				// Need to abort collection download processing. Cancel and return the items
				// processed so
				// far.
				dataTransferService.cancelCollectionDownloadTaskItems(downloadItems);
				logger.info("Processing collection download task [{}] aborted", collection.getAbsolutePath());
				return downloadItems;
			}
		}

		// Iterate through the sub-collections and download them.
		for (HpcCollectionListingEntry subCollectionEntry : collection.getSubCollections()) {
			String subCollectionPath = subCollectionEntry.getPath();
			HpcCollection subCollection = dataManagementService.getCollection(subCollectionPath, true);
			if (subCollection != null) {
				// Download this sub-collection.
				downloadItems.addAll(downloadCollection(subCollection,
						calculateGlobusDownloadDestination(globusDownloadDestination, subCollectionPath,
								appendPathToDownloadDestination ? null : false),
						calculateS3DownloadDestination(s3DownloadDestination, subCollectionPath,
								appendPathToDownloadDestination ? null : false),
						calculateGoogleDriveDownloadDestination(googleDriveDownloadDestination, subCollectionPath,
								appendPathToDownloadDestination ? null : false),
						appendPathToDownloadDestination, userId, collectionDownloadBreaker));
			}
		}

		return downloadItems;
	}

	/**
	 * Download a list of data objects.
	 *
	 * @param dataObjectPaths                 The list of data object path to
	 *                                        download.
	 * @param globusDownloadDestination       The user requested Glopbus download
	 *                                        destination.
	 * @param s3DownloadDestination           The user requested S3 download
	 *                                        destination.
	 * @param googleDriveDownloadDestination  The user requested Google Drive
	 *                                        download destination.
	 * @param appendPathToDownloadDestination If true, the (full) object path will
	 *                                        be used in the destination path,
	 *                                        otherwise just the object name will be
	 *                                        used.
	 * @param userId                          The user ID who requested the
	 *                                        collection download.
	 * @return The download task items (each item represent a data-object download
	 *         from the requested list).
	 * @throws HpcException on service failure.
	 */
	private List<HpcCollectionDownloadTaskItem> downloadDataObjects(List<String> dataObjectPaths,
			HpcGlobusDownloadDestination globusDownloadDestination, HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDriveDownloadDestination googleDriveDownloadDestination, boolean appendPathToDownloadDestination,
			String userId) throws HpcException {
		List<HpcCollectionDownloadTaskItem> downloadItems = new ArrayList<>();

		// Iterate through the data objects in the collection and download them.
		for (String dataObjectPath : dataObjectPaths) {
			HpcCollectionDownloadTaskItem downloadItem = downloadDataObject(dataObjectPath, globusDownloadDestination,
					s3DownloadDestination, googleDriveDownloadDestination, appendPathToDownloadDestination, userId);
			downloadItems.add(downloadItem);
		}

		return downloadItems;
	}

	/**
	 * Download a data object.
	 *
	 * @param path                            The data object path.
	 * @param globusDownloadDestination       The user requested Glopbus download
	 *                                        destination.
	 * @param s3DownloadDestination           The user requested S3 download
	 *                                        destination.
	 * @param googleDriveDownloadDestination  The user requested Google Drive
	 *                                        download destination.
	 * @param appendPathToDownloadDestination If true, the (full) object path will
	 *                                        be used in the destination path,
	 *                                        otherwise just the object name will be
	 *                                        used.
	 * @param userId                          The user ID who requested the
	 *                                        collection download.
	 * @return The download task item.
	 */
	private HpcCollectionDownloadTaskItem downloadDataObject(String path,
			HpcGlobusDownloadDestination globusDownloadDestination, HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDriveDownloadDestination googleDriveDownloadDestination, boolean appendPathToDownloadDestination,
			String userId) {
		HpcDownloadRequestDTO dataObjectDownloadRequest = new HpcDownloadRequestDTO();
		dataObjectDownloadRequest.setGlobusDownloadDestination(
				calculateGlobusDownloadDestination(globusDownloadDestination, path, appendPathToDownloadDestination));
		dataObjectDownloadRequest.setS3DownloadDestination(
				calculateS3DownloadDestination(s3DownloadDestination, path, appendPathToDownloadDestination));
		dataObjectDownloadRequest.setGoogleDriveDownloadDestination(calculateGoogleDriveDownloadDestination(
				googleDriveDownloadDestination, path, appendPathToDownloadDestination));

		// Instantiate a download item for this data object.
		HpcCollectionDownloadTaskItem downloadItem = new HpcCollectionDownloadTaskItem();
		downloadItem.setPath(path);

		// Download this data object.
		try {
			HpcDataObjectDownloadResponseDTO dataObjectDownloadResponse = dataManagementBusService
					.downloadDataObject(path, dataObjectDownloadRequest, userId, false);

			downloadItem.setDataObjectDownloadTaskId(dataObjectDownloadResponse.getTaskId());
			downloadItem.setDestinationLocation(dataObjectDownloadResponse.getDestinationLocation());
			downloadItem.setRestoreInProgress(dataObjectDownloadResponse.getRestoreInProgress());

		} catch (HpcException e) {
			// Data object download failed.
			logger.error("Failed to download data object in a collection", e);

			downloadItem.setResult(HpcDownloadResult.FAILED);
			HpcFileLocation destinationLocation = null;
			if (globusDownloadDestination != null) {
				destinationLocation = globusDownloadDestination.getDestinationLocation();
			} else if (s3DownloadDestination != null) {
				destinationLocation = s3DownloadDestination.getDestinationLocation();
			} else if (googleDriveDownloadDestination != null) {
				destinationLocation = googleDriveDownloadDestination.getDestinationLocation();
			}
			downloadItem.setDestinationLocation(destinationLocation);
			downloadItem.setMessage(e.getMessage());
		}

		return downloadItem;
	}

	/**
	 * Calculate a Globus download destination path for a collection entry under a
	 * collection.
	 *
	 * @param collectionDestination           The Globus collection destination.
	 * @param collectionListingEntryPath      The entry path under the collection to
	 *                                        calculate the destination location
	 *                                        for.
	 * @param appendPathToDownloadDestination If true, the (full) object path will
	 *                                        be used in the destination path,
	 *                                        otherwise just the object name will be
	 *                                        used. If null - not used.
	 * @return A calculated destination location.
	 */
	private HpcGlobusDownloadDestination calculateGlobusDownloadDestination(
			HpcGlobusDownloadDestination collectionDestination, String collectionListingEntryPath,
			Boolean appendPathToDownloadDestination) {
		if (collectionDestination == null) {
			return null;
		}

		HpcGlobusDownloadDestination calcGlobusDestination = new HpcGlobusDownloadDestination();
		HpcFileLocation calcDestination = new HpcFileLocation();
		calcDestination.setFileContainerId(collectionDestination.getDestinationLocation().getFileContainerId());
		String fileId = collectionDestination.getDestinationLocation().getFileId();
		if (appendPathToDownloadDestination != null) {
			fileId = fileId + (appendPathToDownloadDestination ? collectionListingEntryPath
					: collectionListingEntryPath.substring(collectionListingEntryPath.lastIndexOf('/')));
		}
		calcDestination.setFileId(fileId);

		calcGlobusDestination.setDestinationLocation(calcDestination);
		calcGlobusDestination.setDestinationOverwrite(collectionDestination.getDestinationOverwrite());

		return calcGlobusDestination;
	}

	/**
	 * Calculate a S3 download destination path for a collection entry under a
	 * collection.
	 *
	 * @param collectionDestination           The S3 collection destination.
	 * @param collectionListingEntryPath      The entry path under the collection to
	 *                                        calculate the destination location
	 *                                        for.
	 * @param appendPathToDownloadDestination If true, the (full) object path will
	 *                                        be used in the destination path,
	 *                                        otherwise just the object name will be
	 *                                        used. If null - not used.
	 * 
	 * @return A calculated destination location.
	 */
	private HpcS3DownloadDestination calculateS3DownloadDestination(HpcS3DownloadDestination collectionDestination,
			String collectionListingEntryPath, Boolean appendPathToDownloadDestination) {
		if (collectionDestination == null) {
			return null;
		}

		HpcS3DownloadDestination calcS3Destination = new HpcS3DownloadDestination();
		HpcFileLocation calcDestination = new HpcFileLocation();
		calcDestination.setFileContainerId(collectionDestination.getDestinationLocation().getFileContainerId());
		String fileId = collectionDestination.getDestinationLocation().getFileId();
		if (appendPathToDownloadDestination != null) {
			fileId = fileId + (appendPathToDownloadDestination ? collectionListingEntryPath
					: collectionListingEntryPath.substring(collectionListingEntryPath.lastIndexOf('/')));
		}
		calcDestination.setFileId(fileId);

		calcS3Destination.setDestinationLocation(calcDestination);
		calcS3Destination.setAccount(collectionDestination.getAccount());

		return calcS3Destination;
	}

	/**
	 * Calculate a Google Drive download destination path for a collection entry
	 * under a collection.
	 *
	 * @param collectionDestination           The Google Drive collection
	 *                                        destination.
	 * @param collectionListingEntryPath      The entry path under the collection to
	 *                                        calculate the destination location
	 *                                        for.
	 * @param appendPathToDownloadDestination If true, the (full) object path will
	 *                                        be used in the destination path,
	 *                                        otherwise just the object name will be
	 *                                        used. If null - not used.
	 * 
	 * @return A calculated destination location.
	 */
	private HpcGoogleDriveDownloadDestination calculateGoogleDriveDownloadDestination(
			HpcGoogleDriveDownloadDestination collectionDestination, String collectionListingEntryPath,
			Boolean appendPathToDownloadDestination) {
		if (collectionDestination == null) {
			return null;
		}

		HpcGoogleDriveDownloadDestination calcGoogleDriveDestination = new HpcGoogleDriveDownloadDestination();
		HpcFileLocation calcDestination = new HpcFileLocation();
		calcDestination.setFileContainerId(collectionDestination.getDestinationLocation().getFileContainerId());
		String fileId = collectionDestination.getDestinationLocation().getFileId();
		if (appendPathToDownloadDestination != null) {
			fileId = fileId + (appendPathToDownloadDestination ? collectionListingEntryPath
					: collectionListingEntryPath.substring(collectionListingEntryPath.lastIndexOf('/')));
		}
		calcDestination.setFileId(fileId);

		calcGoogleDriveDestination.setDestinationLocation(calcDestination);
		calcGoogleDriveDestination.setAccessToken(collectionDestination.getAccessToken());

		return calcGoogleDriveDestination;
	}

	/**
	 * Complete a collection download task. 1. Update task info in DB with results
	 * info. 2. Send an event.
	 *
	 * @param downloadTask The download task to complete.
	 * @param result       The result of the task (completed, failed or canceled).
	 * @param message      (Optional) If the task failed, a message describing the
	 *                     failure.
	 * @throws HpcException on service failure.
	 */
	private void completeCollectionDownloadTask(HpcCollectionDownloadTask downloadTask, HpcDownloadResult result,
			String message) throws HpcException {
		Calendar completed = Calendar.getInstance();
		dataTransferService.completeCollectionDownloadTask(downloadTask, result, message, completed);

		// Set the payload with either the collection path or the list of data object
		// paths.
		String path = "";
		if (downloadTask.getType().equals(HpcDownloadTaskType.COLLECTION)) {
			path = downloadTask.getPath();
		} else if (downloadTask.getType().equals(HpcDownloadTaskType.DATA_OBJECT_LIST)) {
			path = StringUtils.join(downloadTask.getDataObjectPaths(), ", ");
		} else if (downloadTask.getType().equals(HpcDownloadTaskType.COLLECTION_LIST)) {
			path = StringUtils.join(downloadTask.getCollectionPaths(), ", ");
		}

		// Send download completed/failed event.
		HpcFileLocation destinationLocation = null;
		HpcDataTransferType dataTransferType = null;
		if (downloadTask.getS3DownloadDestination() != null) {
			destinationLocation = downloadTask.getS3DownloadDestination().getDestinationLocation();
			dataTransferType = HpcDataTransferType.S_3;
		} else if (downloadTask.getGlobusDownloadDestination() != null) {
			destinationLocation = downloadTask.getGlobusDownloadDestination().getDestinationLocation();
			dataTransferType = HpcDataTransferType.GLOBUS;
		} else if (downloadTask.getGoogleDriveDownloadDestination() != null) {
			destinationLocation = downloadTask.getGoogleDriveDownloadDestination().getDestinationLocation();
			dataTransferType = HpcDataTransferType.GOOGLE_DRIVE;
		}

		addDataTransferDownloadEvent(downloadTask.getUserId(), path, downloadTask.getType(), downloadTask.getId(),
				dataTransferType, downloadTask.getConfigurationId(), result, message, destinationLocation, completed);

		logger.info("collection download task: {} - completed as {} [{}]", downloadTask.getId(), result.value(),
				downloadTask.getType().value());

	}

	/**
	 * Complete a data object download task that is in-progress (Globus transfer is
	 * in-progress). 1. Check the status of Globus transfer. 2. If completed
	 * (completed or failed), record the result.
	 *
	 * @param downloadTask The download task to complete.
	 * @throws HpcException on service failure.
	 */
	private void completeInProgressDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask) throws HpcException {
		if (downloadTask.getDataTransferType().equals(HpcDataTransferType.S_3)) {
			// Checking transfer status is done for active Globus downloads only.
			logger.info("download task: {} - still in-progress [transfer-type={}, destination-type={}]",
					downloadTask.getId(), downloadTask.getDataTransferType(), downloadTask.getDestinationType());
			return;
		}

		// Get the data transfer download status.
		HpcDataTransferDownloadReport dataTransferDownloadReport = dataTransferService.getDataTransferDownloadStatus(
				downloadTask.getDataTransferType(), downloadTask.getDataTransferRequestId(),
				downloadTask.getConfigurationId(), downloadTask.getS3ArchiveConfigurationId());

		// Check the status of the data transfer.
		HpcDataTransferDownloadStatus dataTransferDownloadStatus = dataTransferDownloadReport.getStatus();
		if (!dataTransferDownloadStatus.equals(HpcDataTransferDownloadStatus.IN_PROGRESS)) {
			// This download task is no longer in-progress - complete it.

			// Determine the download result.
			HpcDownloadResult result = null;
			if (dataTransferDownloadStatus.equals(HpcDataTransferDownloadStatus.COMPLETED)) {
				result = HpcDownloadResult.COMPLETED;
			} else {
				if (Boolean.TRUE.equals(dataTransferDownloadReport.getPermissionDenied())) {
					result = HpcDownloadResult.FAILED_PERMISSION_DENIED;
				} else {
					result = HpcDownloadResult.FAILED;
				}
			}

			String message = result.equals(HpcDownloadResult.COMPLETED) ? null
					: downloadTask.getDataTransferType() + " transfer failed ["
							+ dataTransferDownloadReport.getMessage() + "].";
			Calendar completed = Calendar.getInstance();
			dataTransferService.completeDataObjectDownloadTask(downloadTask, result, message, completed,
					dataTransferDownloadReport.getBytesTransferred());

			// Send a download completion event (if requested to).
			if (downloadTask.getCompletionEvent()) {
				addDataTransferDownloadEvent(downloadTask.getUserId(), downloadTask.getPath(),
						HpcDownloadTaskType.DATA_OBJECT, downloadTask.getId(), downloadTask.getDataTransferType(),
						downloadTask.getConfigurationId(), result, message,
						downloadTask.getGlobusDownloadDestination().getDestinationLocation(), completed);
			}
		} else {
			// Download is still in progress. Update the progress (percent complete).
			dataTransferService.updateDataObjectDownloadTask(downloadTask,
					dataTransferDownloadReport.getBytesTransferred());

			logger.info("download task: {} - still in-progress [transfer-type={}, destination-type={}]",
					downloadTask.getId(), downloadTask.getDataTransferType(), downloadTask.getDestinationType());
		}
	}

	/**
	 * Complete a data object download task that got canceled.
	 * 
	 * @param downloadTask The download task to complete.
	 * @throws HpcException on service failure.
	 */
	private void completeCanceledDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask) throws HpcException {
		Calendar completed = Calendar.getInstance();
		dataTransferService.completeDataObjectDownloadTask(downloadTask, HpcDownloadResult.CANCELED, null, completed,
				0);

		// Send a download completion event (if requested to).
		if (downloadTask.getCompletionEvent()) {
			addDataTransferDownloadEvent(downloadTask.getUserId(), downloadTask.getPath(),
					HpcDownloadTaskType.DATA_OBJECT, downloadTask.getId(), downloadTask.getDataTransferType(),
					downloadTask.getConfigurationId(), HpcDownloadResult.CANCELED, null,
					downloadTask.getGlobusDownloadDestination().getDestinationLocation(), completed);
		}
	}

	/**
	 * Set the file container name.
	 *
	 * @param dataTransferType The data transfer type.
	 * @param configurationId  The data management configuration ID.
	 * @param fileLocation     The file location.
	 */
	private void setFileContainerName(HpcDataTransferType dataTransferType, String configurationId,
			HpcFileLocation fileLocation) {
		if (fileLocation == null) {
			return;
		}

		try {
			// Get the file container ID name.
			fileLocation.setFileContainerName(dataTransferService.getFileContainerName(dataTransferType,
					configurationId, fileLocation.getFileContainerId()));

		} catch (HpcException e) {
			logger.error("Failed to get file container name: " + fileLocation.getFileContainerId(), e);
		}
	}

	/**
	 * Process a data object registration failure. 1. Delete object from iRODS. 2.
	 * Send an event 3. Record the result in the DB
	 *
	 * @param path    The data object path.
	 * @param message an error message.
	 */
	private void processDataObjectRegistrationFailure(String path, String message) {
		// Update the data transfer status. This is needed in case the actual deletion
		// failed.
		HpcSystemGeneratedMetadata systemGeneratedMetadata = null;
		try {
			metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null,
					HpcDataTransferUploadStatus.FAILED, null, null, null, null, null, null, null);

			systemGeneratedMetadata = metadataService.getDataObjectSystemGeneratedMetadata(path);

		} catch (HpcException e) {
			logger.error("Failed to update system metadata: " + path, HpcErrorType.UNEXPECTED_ERROR, e);
		}

		// Delete the data object.
		try {
			dataManagementService.delete(path, true);

		} catch (HpcException e) {
			logger.error("Failed to delete data object: " + path, HpcErrorType.UNEXPECTED_ERROR, e);
		}

		// Send an an event if needed.
		if (systemGeneratedMetadata != null && systemGeneratedMetadata.getRegistrationCompletionEvent()) {
			addDataTransferUploadEvent(systemGeneratedMetadata.getRegistrarId(), path,
					systemGeneratedMetadata.getDataTransferStatus(), systemGeneratedMetadata.getSourceLocation(),
					systemGeneratedMetadata.getDataTransferCompleted(), systemGeneratedMetadata.getDataTransferType(),
					systemGeneratedMetadata.getConfigurationId(), systemGeneratedMetadata.getDataTransferType());
		}

		// Record a data object registration result.
		try {
			dataManagementService.addDataObjectRegistrationResult(path, systemGeneratedMetadata, false, message);

		} catch (HpcException e) {
			logger.error("Failed to record registration result: " + path, HpcErrorType.UNEXPECTED_ERROR, e);
		}
	}

	/**
	 * Register a data object.
	 *
	 * @param registrationItem The data object registration item (one in a list).
	 * @param userId           The registrar user-id.
	 */
	private void registerDataObject(HpcBulkDataObjectRegistrationItem registrationItem, String userId) {
		HpcDataObjectRegistrationRequest registrationRequest = registrationItem.getRequest();
		HpcDataObjectRegistrationTaskItem registrationTask = registrationItem.getTask();

		// Get the user name.
		HpcUser user = null;
		try {
			user = securityService.getUser(userId);
		} catch (HpcException e) {
			logger.error("Failed to get user: " + userId);
		}
		String userName = user != null ? user.getNciAccount().getFirstName() + " " + user.getNciAccount().getLastName()
				: "UNKNOWN";

		// Map request to a DTO.
		HpcDataObjectRegistrationRequestDTO registrationDTO = new HpcDataObjectRegistrationRequestDTO();
		registrationDTO.setCallerObjectId(registrationRequest.getCallerObjectId());
		registrationDTO.setCreateParentCollections(registrationRequest.getCreateParentCollections());
		registrationDTO.setGlobusUploadSource(registrationRequest.getGlobusUploadSource());
		registrationDTO.setS3UploadSource(registrationRequest.getS3UploadSource());
		registrationDTO.setGoogleDriveUploadSource(registrationRequest.getGoogleDriveUploadSource());
		registrationDTO.setFileSystemUploadSource(registrationRequest.getFileSystemUploadSource());
		registrationDTO.setLinkSourcePath(registrationRequest.getLinkSourcePath());
		registrationDTO.getMetadataEntries().addAll(registrationRequest.getMetadataEntries());
		registrationDTO
				.setParentCollectionsBulkMetadataEntries(registrationRequest.getParentCollectionsBulkMetadataEntries());

		try {
			// Determine the data management configuration to use based on the path.
			String configurationId = dataManagementService
					.findDataManagementConfigurationId(registrationTask.getPath());
			if (StringUtils.isEmpty(configurationId)) {
				throw new HpcException("Failed to determine data management configuration.",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			dataManagementBusService.registerDataObject(registrationTask.getPath(), registrationDTO, null, userId,
					userName, configurationId, false);

		} catch (HpcException e) {
			// Data object registration failed. Update the task accordingly.
			registrationTask.setResult(false);
			registrationTask.setMessage(e.getMessage());
			registrationTask.setCompleted(Calendar.getInstance());
		}
	}

	/**
	 * Complete a bulk data object registration task. 1. Update task info in DB with
	 * results info. 2. Send an event.
	 *
	 * @param registrationTask The registration task to complete.
	 * @param result           The result of the task (true is successful, false is
	 *                         failed).
	 * @param message          (Optional) If the task failed, a message describing
	 *                         the failure.
	 */
	private void completeBulkDataObjectRegistrationTask(HpcBulkDataObjectRegistrationTask registrationTask,
			boolean result, String message) {
		Calendar completed = Calendar.getInstance();

		try {
			dataManagementService.completeBulkDataObjectRegistrationTask(registrationTask, result, message, completed);

		} catch (HpcException e) {
			logger.error("Failed to complete data object list registration request", e);
		}

		// Send an event.
		addBulkDataObjectRegistrationEvent(registrationTask, result, message, completed);
	}

	/**
	 * Check and update status of a data object registration item
	 *
	 * @param registrationItem The registration item to check.
	 */
	private void updateRegistrationItemStatus(HpcBulkDataObjectRegistrationItem registrationItem) {
		HpcDataObjectRegistrationTaskItem registrationTask = registrationItem.getTask();
		try {
			if (registrationTask.getResult() == null) {
				// This registration item in progress - check its status.

				// If the data object doesn't exist, it means the upload failed and it was
				// removed.
				if (dataManagementService.getDataObject(registrationTask.getPath()) == null) {
					registrationTask.setResult(false);
					registrationTask.setMessage("Data object upload failed");
					registrationTask.setCompleted(Calendar.getInstance());
					registrationTask.setPercentComplete(0);
				}

				// Get the System generated metadata.
				HpcSystemGeneratedMetadata metadata = metadataService
						.getDataObjectSystemGeneratedMetadata(registrationTask.getPath());
				registrationTask.setSize(metadata.getSourceSize());

				// Check the upload status.
				if (metadata.getLinkSourcePath() != null) {
					// Registration w/ link completed.
					registrationTask.setResult(true);

				} else if (metadata.getDataTransferStatus().equals(HpcDataTransferUploadStatus.ARCHIVED)) {
					// Registration completed successfully for this item.
					registrationTask.setResult(true);
					registrationTask.setCompleted(metadata.getDataTransferCompleted());
					registrationTask.setPercentComplete(100);

					// Calculate the effective transfer speed. Note: there is no transfer in
					// registration w/
					// link
					registrationTask.setEffectiveTransferSpeed(Math.toIntExact(
							metadata.getSourceSize() * 1000 / (metadata.getDataTransferCompleted().getTimeInMillis()
									- metadata.getDataTransferStarted().getTimeInMillis())));

				} else {
					// Registration still in progress. Update % complete.
					registrationTask
							.setPercentComplete(dataTransferService.calculateDataObjectUploadPercentComplete(metadata));
				}
			}

		} catch (HpcException e) {
			logger.error("Failed to check data object registration item status", e);
			registrationTask.setResult(false);
			registrationTask.setMessage(e.getMessage());
			registrationTask.setPercentComplete(null);
		}
	}

	/**
	 * Package a source location into a Globus upload source object.
	 *
	 * @param sourceLocation The source location to package.
	 * @return The packaged Globus upload source.
	 */
	private HpcUploadSource toGlobusUploadSource(HpcFileLocation sourceLocation) {
		HpcUploadSource globusUploadSource = new HpcUploadSource();
		globusUploadSource.setSourceLocation(sourceLocation);
		return globusUploadSource;
	}

	/**
	 * Package a source location into a S3 upload source object.
	 *
	 * @param uploadMethod   The method of upload. If not S_3, null will be
	 *                       returned.
	 * @param sourceLocation The source location to package.
	 * @param sourceURL      The source URL to stream from.
	 * @param sourceSize     The source file size.
	 * @return The packaged S3 upload source.
	 */
	private HpcStreamingUploadSource toS3UploadSource(HpcDataTransferUploadMethod uploadMethod,
			HpcFileLocation sourceLocation, String sourceURL, Long sourceSize) {
		if (!uploadMethod.equals(HpcDataTransferUploadMethod.S_3)) {
			return null;
		}

		HpcStreamingUploadSource s3UploadSource = new HpcStreamingUploadSource();
		s3UploadSource.setSourceLocation(sourceLocation);
		s3UploadSource.setSourceURL(sourceURL);
		s3UploadSource.setSourceSize(sourceSize);
		return s3UploadSource;
	}

	/**
	 * Package a source location into a Google Drive upload source object.
	 *
	 * @param uploadMethod   The method of upload. If not GOOGLE_DRIVE, null will be
	 *                       returned
	 * @param sourceLocation The source location to package.
	 * @param sourceURL      The source URL to stream from.
	 * @param sourceSize     The source file size.
	 * @return The packaged S3 upload source.
	 */
	private HpcStreamingUploadSource toGoogleDriveUploadSource(HpcDataTransferUploadMethod uploadMethod,
			HpcFileLocation sourceLocation, String sourceURL, Long sourceSize) {
		if (!uploadMethod.equals(HpcDataTransferUploadMethod.GOOGLE_DRIVE)) {
			return null;
		}

		HpcStreamingUploadSource googleDriveUploadSource = new HpcStreamingUploadSource();
		googleDriveUploadSource.setSourceLocation(sourceLocation);
		// For Google drive, we persisted the access-token as source-url.
		googleDriveUploadSource.setAccessToken(sourceURL);
		googleDriveUploadSource.setSourceSize(sourceSize);
		return googleDriveUploadSource;
	}

	/**
	 * Check if an upload from S3 (either via URL upload or streaming) has
	 * completed.
	 *
	 * @param path                    The path of the data object to check if an
	 *                                upload from S3 completed.
	 * @param systemGeneratedMetadata The system generated metadata for the data
	 *                                object.
	 * @return true if the uploaded completed, or false otherwise.
	 * @throws HpcException If failed to check/update upload status.
	 */
	private boolean updateS3UploadStatus(String path, HpcSystemGeneratedMetadata systemGeneratedMetadata)
			throws HpcException {
		logger.info("Before updateS3UploadStatus(): {}", path);

		// Lookup the archive for this data object.
		logger.info("Before getPathAttributes(): {}", path);
		HpcPathAttributes archivePathAttributes = dataTransferService.getPathAttributes(
				systemGeneratedMetadata.getDataTransferType(), systemGeneratedMetadata.getArchiveLocation(), true,
				systemGeneratedMetadata.getConfigurationId(), systemGeneratedMetadata.getS3ArchiveConfigurationId());
		logger.info("After getPathAttributes(): {}", path);

		if (archivePathAttributes.getExists() && archivePathAttributes.getIsFile()) {
			// The data object is found in archive. i.e. upload was completed successfully.

			// Update the archive data object's system-metadata.
			logger.info("Before addSystemGeneratedMetadataToDataObject(): {}", path);
			HpcS3ObjectMetadata objectMetadata = dataTransferService.addSystemGeneratedMetadataToDataObject(
					systemGeneratedMetadata.getArchiveLocation(), systemGeneratedMetadata.getDataTransferType(),
					systemGeneratedMetadata.getConfigurationId(), systemGeneratedMetadata.getS3ArchiveConfigurationId(),
					systemGeneratedMetadata.getObjectId(), systemGeneratedMetadata.getRegistrarId());
			logger.info("After addSystemGeneratedMetadataToDataObject(): {}", path);

			// Update the data management (iRODS) data object's system-metadata.
			Calendar dataTransferCompleted = Calendar.getInstance();

			logger.info("Before updateDataObjectSystemGeneratedMetadata(): {}", path);
			Calendar deepArchiveDate = objectMetadata.getDeepArchiveStatus() != null && objectMetadata.getDeepArchiveStatus().equals(HpcDeepArchiveStatus.IN_PROGRESS) ? Calendar.getInstance() : null;
			metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, objectMetadata.getChecksum(),
					HpcDataTransferUploadStatus.ARCHIVED, null, null, dataTransferCompleted,
					archivePathAttributes.getSize(), null, objectMetadata.getDeepArchiveStatus(), deepArchiveDate);
			logger.info("After updateDataObjectSystemGeneratedMetadata(): {}", path);

			// Add an event if needed.
			if (systemGeneratedMetadata.getRegistrationCompletionEvent()) {
				addDataTransferUploadEvent(systemGeneratedMetadata.getRegistrarId(), path,
						HpcDataTransferUploadStatus.ARCHIVED, systemGeneratedMetadata.getSourceLocation(),
						dataTransferCompleted, systemGeneratedMetadata.getDataTransferType(),
						systemGeneratedMetadata.getConfigurationId(), HpcDataTransferType.S_3);
			}

			// Record a registration result.
			systemGeneratedMetadata.setDataTransferCompleted(dataTransferCompleted);
			dataManagementService.addDataObjectRegistrationResult(path, systemGeneratedMetadata, true, null);

			logger.info("After updateS3UploadStatus(): upload-completed {}", path);
			return true;
		}

		logger.info("After updateS3UploadStatus(): upload-in-progress {}", path);
		return false;
	}

	/**
	 * Upload a data object file that is located on the local DME server file system
	 * to the archive.
	 *
	 * @param dataObjects           The list of data objects to upload the
	 *                              associated file to the archive object.
	 * @param deleteFileAfterUpload Indicator whether to delete the (source) file
	 *                              after upload
	 * @throws HpcException If failed to upload a file
	 */
	private void uploadDataObjectsFile(List<HpcDataObject> dataObjects, boolean deleteFileAfterUpload)
			throws HpcException {
		for (HpcDataObject dataObject : dataObjects) {
			String path = dataObject.getAbsolutePath();
			logger.info("Upload data from Temporary Archive / File System for: {}", path);
			HpcSystemGeneratedMetadata systemGeneratedMetadata = null;
			try {
				// Get the data object system generated metadata.
				systemGeneratedMetadata = metadataService.getDataObjectSystemGeneratedMetadata(path);

				File file = null;
				if (systemGeneratedMetadata.getDataTransferStatus()
						.equals(HpcDataTransferUploadStatus.IN_TEMPORARY_ARCHIVE)) {
					// Get the file associated with the data object in the temporary archive.
					file = dataTransferService.getArchiveFile(systemGeneratedMetadata.getConfigurationId(),
							systemGeneratedMetadata.getS3ArchiveConfigurationId(),
							systemGeneratedMetadata.getDataTransferType(),
							systemGeneratedMetadata.getArchiveLocation().getFileId());
				} else if (systemGeneratedMetadata.getDataTransferStatus()
						.equals(HpcDataTransferUploadStatus.IN_FILE_SYSTEM)) {
					file = new File(systemGeneratedMetadata.getSourceLocation().getFileId());
				} else {
					throw new HpcException("Unexpected data object upload status for file upload task: "
							+ systemGeneratedMetadata.getDataTransferStatus(), HpcErrorType.UNEXPECTED_ERROR);
				}

				// Transfer the data file from the temporary archive into the archive.
				HpcDataObjectUploadResponse uploadResponse = dataTransferService.uploadDataObject(null, null, null,
						null, file, false, null, null, path, systemGeneratedMetadata.getObjectId(),
						systemGeneratedMetadata.getRegistrarId(), systemGeneratedMetadata.getCallerObjectId(),
						systemGeneratedMetadata.getConfigurationId());

				// Generate archive system generated metadata.
				HpcS3ObjectMetadata objectMetadata = dataTransferService.addSystemGeneratedMetadataToDataObject(
						uploadResponse.getArchiveLocation(), uploadResponse.getDataTransferType(),
						systemGeneratedMetadata.getConfigurationId(),
						systemGeneratedMetadata.getS3ArchiveConfigurationId(), systemGeneratedMetadata.getObjectId(),
						systemGeneratedMetadata.getRegistrarId());

				// Delete the file.
				if (deleteFileAfterUpload && !FileUtils.deleteQuietly(file)) {
					logger.error("Failed to delete file: " + systemGeneratedMetadata.getArchiveLocation().getFileId());
				}

				// Update system metadata of the data object.
				Calendar deepArchiveDate = objectMetadata.getDeepArchiveStatus() != null && objectMetadata.getDeepArchiveStatus().equals(HpcDeepArchiveStatus.IN_PROGRESS) ? Calendar.getInstance() : null;
				metadataService.updateDataObjectSystemGeneratedMetadata(path, uploadResponse.getArchiveLocation(),
						uploadResponse.getDataTransferRequestId(), objectMetadata.getChecksum(), uploadResponse.getDataTransferStatus(),
						uploadResponse.getDataTransferType(), null, uploadResponse.getDataTransferCompleted(), null,
						null, objectMetadata.getDeepArchiveStatus(), deepArchiveDate);

				// Data transfer upload completed successfully. Add an event if needed.
				if (systemGeneratedMetadata.getRegistrationCompletionEvent()) {
					addDataTransferUploadEvent(systemGeneratedMetadata.getRegistrarId(), path,
							uploadResponse.getDataTransferStatus(), systemGeneratedMetadata.getSourceLocation(),
							uploadResponse.getDataTransferCompleted(), uploadResponse.getDataTransferType(),
							systemGeneratedMetadata.getConfigurationId(), HpcDataTransferType.GLOBUS);
				}

				// Record a registration result.
				systemGeneratedMetadata.setDataTransferCompleted(uploadResponse.getDataTransferCompleted());
				dataManagementService.addDataObjectRegistrationResult(path, systemGeneratedMetadata, true, null);

			} catch (HpcException e) {
				logger.error("Failed to transfer data from temporary archive / file system: " + path, e);

				// Process the data object registration failure.
				processDataObjectRegistrationFailure(path, e.getMessage());
			}
		}
	}
	
	/**
	 * add restore request event.
	 *
	 * @param restoreTask	   The restore task.
	 * @param result           The restore task result.
	 * @param message          A failure message.
	 * @param completed        The completion time.
	 */
	private void addRestoreRequestEvent(HpcDataObjectDownloadTask restoreTask, HpcDownloadResult result,
			String message, Calendar completed) {

		// Format the task ID. If the caller provided a UI URL, then use it to construct
		// a URL link to view this task on UI.
		String taskId = restoreTask.getId();

		try {
			if (result.equals(HpcDownloadResult.COMPLETED)) {
				eventService.addRestoreRequestCompletedEvent(restoreTask.getUserId(), taskId,
						restoreTask.getPath(), completed);
			} else {
				eventService.addRestoreRequestFailedEvent(restoreTask.getUserId(), taskId, restoreTask.getPath(), completed,
						message);
			}

		} catch (HpcException e) {
			logger.error("Failed to add a restore request event", e);
		}
	}

	/**
	 * Complete a data object download task that is RESTORE_REQUESTED
	 * 1. Check the status of restoration. 2. If completed
	 * (completed or failed), resume the download or record failure.
	 *
	 * @param downloadTask The download task to complete.
	 * @throws HpcException on service failure.
	 */
	private void completeRestoreRequestedDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask) throws HpcException {

		// Get the System generated metadata.
		HpcSystemGeneratedMetadata systemGeneratedMetadata = metadataService
				.getDataObjectSystemGeneratedMetadata(downloadTask.getPath());

		// Get the data object metadata to check for restoration status
		HpcS3ObjectMetadata objectMetadata = dataTransferService.getDataObjectMetadata(
				systemGeneratedMetadata.getArchiveLocation(), systemGeneratedMetadata.getDataTransferType(), 
				systemGeneratedMetadata.getConfigurationId(), systemGeneratedMetadata.getS3ArchiveConfigurationId());
		
		String restorationStatus = objectMetadata.getRestorationStatus();
		
		if (restorationStatus != null && restorationStatus.equals("in progress")) {
			logger.info("restore request task: {} - still in-progress",
					downloadTask.getId());
		} else {
			// Determine the download result.
			HpcDownloadResult result = HpcDownloadResult.FAILED;
			if (restorationStatus.equals("success")) {	
				result = HpcDownloadResult.COMPLETED;
			} else if (restorationStatus.equals("not in progress")) {
				//Error condition, either restore request was not successful or failure upon restore.
				result = HpcDownloadResult.FAILED;
			}

			String message = null;
			if (result.equals(HpcDownloadResult.FAILED)
					|| downloadTask.getS3DownloadDestination() != null && downloadTask.getS3DownloadDestination()
							.getDestinationLocation().getFileContainerId().equals("Synchronous Download")) {
				// toggle the status to RESTORED and populate HPC_EVENT to notify the user for sync download
				message = result.equals(HpcDownloadResult.COMPLETED) ? null
						: downloadTask.getDataTransferType() + " transfer failed ["
								+ "Restoration request failed" + "].";
				Calendar completed = Calendar.getInstance();
				dataTransferService.completeDataObjectDownloadTask(downloadTask, result, message, completed, 0);
				
				if (downloadTask.getCompletionEvent()) {
					// Send a restoration completion event
					addRestoreRequestEvent(downloadTask, result, message, completed);
				}
			} else {
				// toggle the status to RECEIVED for async download
				dataTransferService.resetDataObjectDownloadTask(downloadTask);
			}
		}
	}
	
	/**
	 * Check and update status of data object that is deep archive in progress
	 *
	 * @param dataObject The data object to check.
	 */
	private void updateDeepArchiveStatus(HpcDataObject dataObject) {
		try {
			// This deep archive status in progress - check if tiered.
			String path = dataObject.getAbsolutePath();
			
			// Get the System generated metadata.
			HpcSystemGeneratedMetadata systemGeneratedMetadata = metadataService
					.getDataObjectSystemGeneratedMetadata(path);
			
			// Get the data object metadata.
			HpcS3ObjectMetadata objectMetadata = dataTransferService.getDataObjectMetadata(
					systemGeneratedMetadata.getArchiveLocation(), systemGeneratedMetadata.getDataTransferType(), 
					systemGeneratedMetadata.getConfigurationId(), systemGeneratedMetadata.getS3ArchiveConfigurationId());
			HpcDeepArchiveStatus deepArchiveStatus = objectMetadata.getDeepArchiveStatus();

			// Check the storage class.
			if (deepArchiveStatus != null) {

				// Add/Update system generated metadata to iRODs
				metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null,
						null, null, null, null, null, null, deepArchiveStatus, null);
			} else {
				// Check if there is a delay and toggle deep archive status to delayed
				if(dataTransferService.deepArchiveDelayed(systemGeneratedMetadata.getDeepArchiveDate())) {
					metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null,
							null, null, null, null, null, null, HpcDeepArchiveStatus.DELAYED, null);
				}
			}

		} catch (HpcException e) {
			logger.error("Failed to check deep archive status", e);
		}
	}

	// Collection download breaker. This class is used to determine if processing
	// of collection download should be aborted because the first item in the
	// collection had
	// permission denied
	// to download.
	private class HpcCollectionDownloadBreaker {
		// ---------------------------------------------------------------------//
		// Constructors
		// ---------------------------------------------------------------------//

		public HpcCollectionDownloadBreaker(String taskId) {
			this.taskId = taskId;
		}

		// ---------------------------------------------------------------------//
		// Instance members
		// ---------------------------------------------------------------------//

		// The collection download task ID.
		private String taskId = null;

		// The first download item task ID.
		private String firstDownloadItemTaskId = null;

		// The download items (processed) count.
		private int downloadItemsCount = 0;

		// The collection abort indicator.
		private Boolean abortCollection = null;

		// ---------------------------------------------------------------------//
		// Methods
		// ---------------------------------------------------------------------//

		/**
		 * Check if processing a collection download task needs to be aborted.
		 *
		 * @param downloadItem The last download item processed by the collection
		 *                     download task
		 * @return true if collection download task needs to be aborted.
		 * @throws HpcException If failed to check first download item status.
		 */
		public boolean abortDownload(HpcCollectionDownloadTaskItem downloadItem) throws HpcException {
			if ((abortCollection == null || !abortCollection)
					&& dataTransferService.getCollectionDownloadTaskCancellationRequested(taskId)) {
				// A user request to cancel the collection download was received.
				abortCollection = true;
			}

			if (abortCollection != null) {
				// The decision to abort or not was made.
				return abortCollection;
			}

			if (firstDownloadItemTaskId == null) {
				// Keep track of the first item in the collection download.
				// If this item faces permission denied, we'll abort the entire collection
				// download
				// processing.
				firstDownloadItemTaskId = downloadItem.getDataObjectDownloadTaskId();
			}

			downloadItemsCount++;
			if (downloadItemsCount % 10 == 0) {
				// We check on the first download task item every 10 items, until confirmed.
				HpcDownloadTaskStatus downloadItemStatus = dataTransferService
						.getDownloadTaskStatus(firstDownloadItemTaskId, HpcDownloadTaskType.DATA_OBJECT);
				if (downloadItemStatus != null && !downloadItemStatus.getInProgress()) {
					// First download item completed. Set the abort indicator.
					abortCollection = downloadItemStatus.getResult().getResult()
							.equals(HpcDownloadResult.FAILED_PERMISSION_DENIED)
							|| downloadItemStatus.getResult().getResult()
									.equals(HpcDownloadResult.FAILED_CREDENTIALS_NEEDED);
					return abortCollection;
				}
			}

			return false;
		}
	}

}
