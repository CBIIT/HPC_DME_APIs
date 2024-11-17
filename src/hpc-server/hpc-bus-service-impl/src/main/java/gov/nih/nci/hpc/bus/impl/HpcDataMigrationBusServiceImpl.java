/**
 * HpcDataManagementBusServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import gov.nih.nci.hpc.bus.HpcDataMigrationBusService;
import gov.nih.nci.hpc.bus.HpcDataSearchBusService;
import gov.nih.nci.hpc.bus.aspect.HpcExecuteAsSystemAccount;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationResult;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationStatus;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.domain.model.HpcDataMigrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataMigrationTaskResult;
import gov.nih.nci.hpc.domain.model.HpcDataMigrationTaskStatus;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamigration.HpcBulkMigrationRequestDTO;
import gov.nih.nci.hpc.dto.datamigration.HpcMetadataMigrationRequestDTO;
import gov.nih.nci.hpc.dto.datamigration.HpcMigrationRequestDTO;
import gov.nih.nci.hpc.dto.datamigration.HpcMigrationResponseDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataMigrationService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * HPC Data Management Business Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataMigrationBusServiceImpl implements HpcDataMigrationBusService {

	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Data Migration Application Service Instance.
	@Autowired
	private HpcDataMigrationService dataMigrationService = null;

	// The Data Management Application Service Instance.
	@Autowired
	private HpcDataManagementService dataManagementService = null;

	// The Metadata Application Service Instance.
	@Autowired
	private HpcMetadataService metadataService = null;

	// The Security Application Service Instance.
	@Autowired
	private HpcSecurityService securityService = null;

	// The Data Transfer Application Service Instance.
	@Autowired
	private HpcDataTransferService dataTransferService = null;

	// The Data Search Business Service Instance.
	@Autowired
	private HpcDataSearchBusService dataSearchBusService = null;

	// The collection download task executor.
	@Autowired
	@Qualifier("hpcDataObjectMetadataUpdateTaskExecutor")
	Executor dataObjectMetadataUpdateTaskExecutor = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcDataMigrationBusServiceImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataMigrationBusService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public HpcMigrationResponseDTO migrateDataObject(String path, HpcMigrationRequestDTO migrationRequest,
			boolean alignArchivePath) throws HpcException {
		return migrateDataObject(path, securityService.getRequestInvoker().getNciAccount().getUserId(), null,
				migrationRequest, alignArchivePath, null, null);
	}

	@Override
	public HpcMigrationResponseDTO migrateCollection(String path, HpcMigrationRequestDTO migrationRequest,
			boolean alignArchivePath) throws HpcException {
		return migrateCollection(path, securityService.getRequestInvoker().getNciAccount().getUserId(),
				migrationRequest, alignArchivePath, null, null, null);
	}

	@Override
	public HpcMigrationResponseDTO migrateDataObjectsOrCollections(HpcBulkMigrationRequestDTO migrationRequest)
			throws HpcException {
		return migrateDataObjectsOrCollections(migrationRequest,
				securityService.getRequestInvoker().getNciAccount().getUserId(), null, null, null);
	}

	@Override
	public HpcMigrationResponseDTO retryDataObjectMigrationTask(String taskId) throws HpcException {
		// Input validation.
		HpcDataMigrationTaskStatus taskStatus = dataMigrationService.getMigrationTaskStatus(taskId,
				HpcDataMigrationType.DATA_OBJECT);
		if (taskStatus == null) {
			throw new HpcException("Data object migration task not found: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (taskStatus.getInProgress() || taskStatus.getResult() == null) {
			throw new HpcException("Data object migration task in-progress: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (taskStatus.getResult() != null
				&& taskStatus.getResult().getResult().equals(HpcDataMigrationResult.COMPLETED)) {
			throw new HpcException("Data-object migration task already completed: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		HpcDataMigrationTaskResult migrationTask = taskStatus.getResult();
		HpcMigrationRequestDTO migrationRequest = new HpcMigrationRequestDTO();
		migrationRequest.setS3ArchiveConfigurationId(migrationTask.getToS3ArchiveConfigurationId());

		return migrateDataObject(migrationTask.getPath(), migrationTask.getUserId(), null, migrationRequest,
				migrationTask.getAlignArchivePath(), taskId,
				securityService.getRequestInvoker().getNciAccount().getUserId());
	}

	@Override
	public HpcMigrationResponseDTO retryCollectionMigrationTask(String taskId, Boolean failedItemsOnly)
			throws HpcException {
		// Input validation.
		HpcDataMigrationTaskStatus taskStatus = dataMigrationService.getMigrationTaskStatus(taskId,
				HpcDataMigrationType.COLLECTION);
		if (taskStatus == null) {
			throw new HpcException("Collection migration task not found: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (taskStatus.getInProgress() || taskStatus.getResult() == null) {
			throw new HpcException("Collection migration task in-progress: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (taskStatus.getResult() != null
				&& taskStatus.getResult().getResult().equals(HpcDataMigrationResult.COMPLETED)) {
			throw new HpcException("Collection migration task already completed: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		HpcDataMigrationTaskResult migrationTask = taskStatus.getResult();
		HpcMigrationRequestDTO migrationRequest = new HpcMigrationRequestDTO();
		migrationRequest.setS3ArchiveConfigurationId(migrationTask.getToS3ArchiveConfigurationId());

		return migrateCollection(migrationTask.getPath(), migrationTask.getUserId(), migrationRequest,
				migrationTask.getAlignArchivePath(), taskId,
				securityService.getRequestInvoker().getNciAccount().getUserId(),
				Optional.ofNullable(failedItemsOnly).orElse(true));
	}

	@Override
	public HpcMigrationResponseDTO retryDataObjectsOrCollectionsMigrationTask(String taskId, Boolean failedItemsOnly)
			throws HpcException {
		// Input validation.
		HpcDataMigrationTaskStatus taskStatus = dataMigrationService.getMigrationTaskStatus(taskId,
				HpcDataMigrationType.DATA_OBJECT_LIST);
		if (taskStatus == null) {
			taskStatus = dataMigrationService.getMigrationTaskStatus(taskId, HpcDataMigrationType.COLLECTION_LIST);
		}
		if (taskStatus == null) {
			throw new HpcException("Bulk migration task not found: " + taskId, HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (taskStatus.getInProgress() || taskStatus.getResult() == null) {
			throw new HpcException("Bulk migration task in-progress: " + taskId, HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (taskStatus.getResult() != null
				&& taskStatus.getResult().getResult().equals(HpcDataMigrationResult.COMPLETED)) {
			throw new HpcException("Bulk migration task already completed: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		HpcDataMigrationTaskResult migrationTask = taskStatus.getResult();
		HpcBulkMigrationRequestDTO migrationRequest = new HpcBulkMigrationRequestDTO();
		migrationRequest.setS3ArchiveConfigurationId(migrationTask.getToS3ArchiveConfigurationId());
		migrationRequest.getDataObjectPaths().addAll(migrationTask.getDataObjectPaths());
		migrationRequest.getCollectionPaths().addAll(migrationTask.getCollectionPaths());

		return migrateDataObjectsOrCollections(migrationRequest, migrationTask.getUserId(), taskId,
				securityService.getRequestInvoker().getNciAccount().getUserId(),
				Optional.ofNullable(failedItemsOnly).orElse(true));
	}

	@Override
	public HpcMigrationResponseDTO migrateMetadata(HpcMetadataMigrationRequestDTO metadataMigrationRequest)
			throws HpcException {
		// Input Validation
		if (metadataMigrationRequest == null) {
			throw new HpcException("Metadata migration request is null", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (StringUtils.isEmpty(metadataMigrationRequest.getFromS3ArchiveConfigurationId())) {
			throw new HpcException("from S3 archive configuration ID is empty", HpcErrorType.INVALID_REQUEST_INPUT);
		} else {
			// Exception will be thrown if the S3 config ID can't be found.
			dataManagementService.getS3ArchiveConfiguration(metadataMigrationRequest.getFromS3ArchiveConfigurationId());
		}

		if (StringUtils.isEmpty(metadataMigrationRequest.getToS3ArchiveConfigurationId())) {
			throw new HpcException("To S3 archive configuration ID is empty", HpcErrorType.INVALID_REQUEST_INPUT);
		} else {
			// Exception will be thrown if the S3 config ID can't be found.
			dataManagementService.getS3ArchiveConfiguration(metadataMigrationRequest.getToS3ArchiveConfigurationId());
		}

		if (metadataMigrationRequest.getFromS3ArchiveConfigurationId()
				.equals(metadataMigrationRequest.getToS3ArchiveConfigurationId())) {
			throw new HpcException("From/To S3 archive configuration ID is identical",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (StringUtils.isEmpty(metadataMigrationRequest.getArchiveFileContainerId())) {
			throw new HpcException("Archive File Container ID is empty", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Create a migration task to perform bulk metadata update.
		HpcMigrationResponseDTO migrationResponse = new HpcMigrationResponseDTO();
		migrationResponse
				.setTaskId(dataMigrationService
						.createMetadataMigrationTask(metadataMigrationRequest.getFromS3ArchiveConfigurationId(),
								metadataMigrationRequest.getToS3ArchiveConfigurationId(),
								metadataMigrationRequest.getArchiveFileContainerId(),
								!StringUtils.isEmpty(metadataMigrationRequest.getArchiveFileIdPattern())
										? metadataMigrationRequest.getArchiveFileIdPattern()
										: "%",
								securityService.getRequestInvoker().getNciAccount().getUserId())
						.getId());

		return migrationResponse;
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processDataObjectMigrationReceived() throws HpcException {
		dataMigrationService.getDataMigrationTasks(HpcDataMigrationStatus.RECEIVED, HpcDataMigrationType.DATA_OBJECT)
				.forEach(dataObjectMigrationTask -> {
					if (markInProcess(dataObjectMigrationTask)) {
						try {
							logger.info("Migrating Data Object: task - {}, path - {}", dataObjectMigrationTask.getId(),
									dataObjectMigrationTask.getPath());
							dataMigrationService.migrateDataObject(dataObjectMigrationTask);

						} catch (HpcException e) {
							logger.error("Failed to migrate data object: task - {}, path - {}",
									dataObjectMigrationTask.getId(), dataObjectMigrationTask.getPath(), e);
							try {
								dataMigrationService.completeDataObjectMigrationTask(dataObjectMigrationTask,
										HpcDataMigrationResult.FAILED, e.getMessage(), null, null);

							} catch (HpcException ex) {
								logger.error("Failed to complete data object migration: task - {}, path - {}",
										dataObjectMigrationTask.getId(), dataObjectMigrationTask.getPath(), ex);
							}
						} finally {
							doneProcessingDataMigrationTask(dataObjectMigrationTask);
						}
					}
				});
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processCollectionMigrationReceived() throws HpcException {
		dataMigrationService.getDataMigrationTasks(HpcDataMigrationStatus.RECEIVED, HpcDataMigrationType.COLLECTION)
				.forEach(collectionMigrationTask -> {
					if (markInProcess(collectionMigrationTask)) {
						try {
							logger.info("Migrating Collection: task - {}, path - {}", collectionMigrationTask.getId(),
									collectionMigrationTask.getPath());

							// Get the collection to be migrated.
							HpcCollection collection = dataManagementService
									.getFullCollection(collectionMigrationTask.getPath());
							if (collection == null) {
								throw new HpcException("Collection not found", HpcErrorType.INVALID_REQUEST_INPUT);
							}

							// Create migration tasks for all data objects under this collection
							migrateCollection(collection, collectionMigrationTask);

							// Mark the collection migration task - in-progress
							collectionMigrationTask.setStatus(HpcDataMigrationStatus.IN_PROGRESS);
							dataMigrationService.updateDataMigrationTask(collectionMigrationTask);

						} catch (HpcException e) {
							logger.error("Failed to migrate collection: task - {}, path - {}",
									collectionMigrationTask.getId(), collectionMigrationTask.getPath(), e);
							try {
								dataMigrationService.completeBulkMigrationTask(collectionMigrationTask, e.getMessage());

							} catch (HpcException ex) {
								logger.error("Failed to complete collection migration: task - {}, path - {}",
										collectionMigrationTask.getId(), collectionMigrationTask.getPath(), ex);
							}
						} finally {
							doneProcessingDataMigrationTask(collectionMigrationTask);
						}
					}
				});
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processDataObjectListMigrationReceived() throws HpcException {
		dataMigrationService
				.getDataMigrationTasks(HpcDataMigrationStatus.RECEIVED, HpcDataMigrationType.DATA_OBJECT_LIST)
				.forEach(dataObjectListMigrationTask -> {
					if (markInProcess(dataObjectListMigrationTask)) {
						try {
							logger.info("Migrating Data Object List: task - {}, path - {}",
									dataObjectListMigrationTask.getId(),
									dataObjectListMigrationTask.getDataObjectPaths());

							if (Optional.ofNullable(dataObjectListMigrationTask.getRetryFailedItemsOnly())
									.orElse(false)) {
								// Retry failed items only.
								migrateFailedItems(dataObjectListMigrationTask);

							} else {
								// Iterate through the data objects in the list and migrate them.
								for (String dataObjectPath : dataObjectListMigrationTask.getDataObjectPaths()) {
									HpcMigrationRequestDTO migrationRequest = new HpcMigrationRequestDTO();
									migrationRequest.setS3ArchiveConfigurationId(
											dataObjectListMigrationTask.getToS3ArchiveConfigurationId());
									migrateDataObject(dataObjectPath, dataObjectListMigrationTask.getUserId(),
											dataObjectListMigrationTask.getId(), migrationRequest,
											dataObjectListMigrationTask.getAlignArchivePath(), null, null);
								}
							}

							// Mark the collection migration task - in-progress
							dataObjectListMigrationTask.setStatus(HpcDataMigrationStatus.IN_PROGRESS);
							dataMigrationService.updateDataMigrationTask(dataObjectListMigrationTask);

						} catch (HpcException e) {
							logger.error("Failed to migrate data object list: task - {}, path - {}",
									dataObjectListMigrationTask.getId(),
									dataObjectListMigrationTask.getDataObjectPaths(), e);
							try {
								dataMigrationService.completeBulkMigrationTask(dataObjectListMigrationTask,
										e.getMessage());

							} catch (HpcException ex) {
								logger.error("Failed to complete data object list migration: task - {}, path - {}",
										dataObjectListMigrationTask.getId(),
										dataObjectListMigrationTask.getDataObjectPaths(), ex);
							}
						} finally {
							doneProcessingDataMigrationTask(dataObjectListMigrationTask);
						}
					}
				});
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processCollectionListMigrationReceived() throws HpcException {
		dataMigrationService
				.getDataMigrationTasks(HpcDataMigrationStatus.RECEIVED, HpcDataMigrationType.COLLECTION_LIST)
				.forEach(collectionListMigrationTask -> {
					if (markInProcess(collectionListMigrationTask)) {
						try {
							logger.info("Migrating Collection list: task - {}, path - {}",
									collectionListMigrationTask.getId(),
									collectionListMigrationTask.getCollectionPaths());

							// Iterate through the collections in the list and migrate them.
							for (String collectionPath : collectionListMigrationTask.getCollectionPaths()) {
								// Get the collection to be migrated.
								HpcCollection collection = dataManagementService.getFullCollection(collectionPath);
								if (collection == null) {
									throw new HpcException("Collection not found", HpcErrorType.INVALID_REQUEST_INPUT);
								}

								// Create migration tasks for all data objects under this collection
								migrateCollection(collection, collectionListMigrationTask);
							}

							// Mark the collection migration task - in-progress
							collectionListMigrationTask.setStatus(HpcDataMigrationStatus.IN_PROGRESS);
							dataMigrationService.updateDataMigrationTask(collectionListMigrationTask);

						} catch (HpcException e) {
							logger.error("Failed to migrate collection list: task - {}, path - {}",
									collectionListMigrationTask.getId(),
									collectionListMigrationTask.getCollectionPaths(), e);
							try {
								dataMigrationService.completeBulkMigrationTask(collectionListMigrationTask,
										e.getMessage());

							} catch (HpcException ex) {
								logger.error("Failed to complete collection list migration: task - {}, path - {}",
										collectionListMigrationTask.getId(), collectionListMigrationTask.getPath(), ex);
							}
						} finally {
							doneProcessingDataMigrationTask(collectionListMigrationTask);
						}
					}
				});
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processBulkMetadataUpdatetMigrationReceived() throws HpcException {
		dataMigrationService
				.getDataMigrationTasks(HpcDataMigrationStatus.RECEIVED, HpcDataMigrationType.BULK_METADATA_UPDATE)
				.forEach(bulkMetadataUpdateTask -> {
					if (markInProcess(bulkMetadataUpdateTask)) {
						try {
							logger.info("Updating bulk metadata: task - {}", bulkMetadataUpdateTask.getId());

							// Create metadata migration tasks for all data objects under this bulk-update
							// task.
							for (String dataObjectPath : getBulkMetadataUpdateDataObjectPaths(bulkMetadataUpdateTask)) {
								updateDataObjectMetadata(dataObjectPath, bulkMetadataUpdateTask);
							}

							// Mark the bulk metadata update task - in-progress
							bulkMetadataUpdateTask.setStatus(HpcDataMigrationStatus.IN_PROGRESS);
							dataMigrationService.updateDataMigrationTask(bulkMetadataUpdateTask);

						} catch (HpcException e) {
							logger.error("Failed to process update-bulk-metadata: task - {}",
									bulkMetadataUpdateTask.getId(), e);
							try {
								dataMigrationService.completeBulkMigrationTask(bulkMetadataUpdateTask, e.getMessage());

							} catch (HpcException ex) {
								logger.error("Failed to complete bulk metadata: task - {}",
										bulkMetadataUpdateTask.getId(), ex);
							}
						} finally {
							doneProcessingDataMigrationTask(bulkMetadataUpdateTask);
						}
					}
				});
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processDataObjectMetadataUpdatetMigrationReceived() throws HpcException {
		dataMigrationService.getDataMigrationTasks(HpcDataMigrationStatus.RECEIVED,
				HpcDataMigrationType.DATA_OBJECT_METADATA_UPDATE).forEach(dataObjectMetadataUpdateTask -> {
					if (markInProcess(dataObjectMetadataUpdateTask)) {
						try {
							logger.info("Updating Data Object Metadata: task - {}, path - {}",
									dataObjectMetadataUpdateTask.getId(), dataObjectMetadataUpdateTask.getPath());
							completeDataObjectMetadataUpdate(dataObjectMetadataUpdateTask);

						} catch (HpcException e) {
							logger.error("Failed to update data object metadata: task - {}, path - {}",
									dataObjectMetadataUpdateTask.getId(), dataObjectMetadataUpdateTask.getPath(), e);
							try {
								dataMigrationService.completeDataObjectMetadataUpdateTask(dataObjectMetadataUpdateTask,
										HpcDataMigrationResult.FAILED, e.getMessage());

							} catch (HpcException ex) {
								logger.error("Failed to complete data object metadata update: task - {}, path - {}",
										dataObjectMetadataUpdateTask.getId(), dataObjectMetadataUpdateTask.getPath(),
										ex);
							}
						} finally {
							doneProcessingDataMigrationTask(dataObjectMetadataUpdateTask);
						}
					}
				});
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void completeBulkMigrationInProgress() throws HpcException {
		List<HpcDataMigrationTask> bulkMigrationTasks = new ArrayList<>();
		bulkMigrationTasks.addAll(dataMigrationService.getDataMigrationTasks(HpcDataMigrationStatus.IN_PROGRESS,
				HpcDataMigrationType.COLLECTION));
		bulkMigrationTasks.addAll(dataMigrationService.getDataMigrationTasks(HpcDataMigrationStatus.IN_PROGRESS,
				HpcDataMigrationType.DATA_OBJECT_LIST));
		bulkMigrationTasks.addAll(dataMigrationService.getDataMigrationTasks(HpcDataMigrationStatus.IN_PROGRESS,
				HpcDataMigrationType.COLLECTION_LIST));
		bulkMigrationTasks.addAll(dataMigrationService.getDataMigrationTasks(HpcDataMigrationStatus.IN_PROGRESS,
				HpcDataMigrationType.BULK_METADATA_UPDATE));

		bulkMigrationTasks.forEach(bulkMigrationTask -> {
			try {
				logger.info("Completing bulk migration: task - {}, type - {}", bulkMigrationTask.getId(),
						bulkMigrationTask.getType());

				dataMigrationService.completeBulkMigrationTask(bulkMigrationTask, null);

			} catch (HpcException e) {
				logger.error("Failed to complete bulk migration: task - {}, type - {}", bulkMigrationTask.getId(),
						bulkMigrationTask.getType(), e);
				try {
					dataMigrationService.completeBulkMigrationTask(bulkMigrationTask, e.getMessage());

				} catch (HpcException ex) {
					logger.error("Failed to complete bulk migration: task - {}, type - {}", bulkMigrationTask.getId(),
							bulkMigrationTask.getType(), ex);
				}
			}
		});
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void assignMigrationServer() throws HpcException {
		dataMigrationService.assignDataMigrationTasks();
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void restartDataMigrationTasks() throws HpcException {
		dataMigrationService.resetMigrationTasksInProcess();
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Validate a data object migration request
	 *
	 * @param path                  The data object path.
	 * @param migrationRequest      The migration request.
	 * @param alignArchivePath      If true, the file is moved within its current
	 *                              archive to align w/ the iRODs path.
	 * @param metadataUpdateRequest true if this is a metadata update request, false
	 *                              if it's data migration request.
	 * @return The data object system metadata
	 * @throws HpcException If the request is invalid.
	 */
	private HpcSystemGeneratedMetadata validateDataObjectMigrationRequest(String path,
			HpcMigrationRequestDTO migrationRequest, boolean alignArchivePath, boolean metadataUpdateRequest)
			throws HpcException {

		// Validate the following:
		// 1. Path is not empty.
		// 2. Migration request is not empty.
		// 3. Data Object exists.
		// 4. Migration is not supported for links.
		// 5. Migration is supported only from S3 archive to S3 Archive.
		// 6. Data Object is archived (i.e. registration completed).
		// 7. The source-size system metadata is available.

		if (!metadataUpdateRequest) {
			validateMigrationRequest(path, migrationRequest, alignArchivePath);
		}

		// Get the System generated metadata.
		HpcSystemGeneratedMetadata metadata = metadataService.getDataObjectSystemGeneratedMetadata(path);

		if (!StringUtils.isEmpty(metadata.getLinkSourcePath())) {
			throw new HpcException("Migration request is not supported for soft-links",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Migration supported only from S3 archive.
		if (metadata.getDataTransferType() == null || !metadata.getDataTransferType().equals(HpcDataTransferType.S_3)) {
			throw new HpcException("Migration request is not supported from POSIX based file system archive",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the file is archived.
		HpcDataTransferUploadStatus dataTransferStatus = metadata.getDataTransferStatus();
		if (dataTransferStatus == null) {
			throw new HpcException("Unknown upload data transfer status: " + path, HpcErrorType.UNEXPECTED_ERROR);
		}
		if (!dataTransferStatus.equals(HpcDataTransferUploadStatus.ARCHIVED)
				&& !dataTransferStatus.equals(HpcDataTransferUploadStatus.DELETE_REQUESTED)) {
			throw new HpcException(
					"Data Object [" + path + "] is not in archived or soft-deleted state. It is in "
							+ metadata.getDataTransferStatus().value() + " state",
					HpcRequestRejectReason.FILE_NOT_ARCHIVED);
		}

		// Validate a source-size system metadata is available.
		if (metadata.getSourceSize() == null) {
			throw new HpcException("Source size system-metadata is missing for data object to be migrated",
					HpcErrorType.UNEXPECTED_ERROR);
		}

		return metadata;
	}

	/**
	 * Validate a collection migration request
	 *
	 * @param path             The data object path.
	 * @param migrationRequest The migration request.
	 * @param alignArchivePath If true, the file is moved within its current archive
	 *                         to align w/ the iRODs path.
	 * @return The data object system metadata
	 * @throws HpcException If the request is invalid.
	 */
	private HpcSystemGeneratedMetadata validateCollectionMigrationRequest(String path,
			HpcMigrationRequestDTO migrationRequest, boolean alignArchivePath) throws HpcException {

		// Validate the following:
		// 1. Path is not empty.
		// 2. Migration is not supported for links.
		// 3. Migration is supported only from S3 archive to S3 Archive.
		// 4. Collection exists.
		// 5. Collection is not empty

		validateMigrationRequest(path, migrationRequest, alignArchivePath);

		// Validate collection exists.
		HpcCollection collection = dataManagementService.getCollection(path, true);
		if (collection == null) {
			throw new HpcException("Collection doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Verify data objects found under this collection.
		if (!dataManagementService.hasDataObjects(collection)) {
			// No data objects found under this collection.
			throw new HpcException("No data objects found under collection" + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the System generated metadata.
		return metadataService.getCollectionSystemGeneratedMetadata(path);
	}

	/**
	 * Validate a bulk migration request
	 *
	 * @param migrationRequest The migration request.
	 * @return The data object system metadata
	 * @throws HpcException If the request is invalid.
	 */
	private HpcSystemGeneratedMetadata validateBulkMigrationRequest(HpcBulkMigrationRequestDTO bulkMigrationRequest)
			throws HpcException {
		if (bulkMigrationRequest == null) {
			throw new HpcException("Null migration request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (bulkMigrationRequest.getDataObjectPaths().isEmpty()
				&& bulkMigrationRequest.getCollectionPaths().isEmpty()) {
			throw new HpcException("No data object or collection paths", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (!bulkMigrationRequest.getDataObjectPaths().isEmpty()
				&& !bulkMigrationRequest.getCollectionPaths().isEmpty()) {
			throw new HpcException("Both data object and collection paths provided",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcMigrationRequestDTO migrationRequest = new HpcMigrationRequestDTO();
		HpcSystemGeneratedMetadata systemGenerateMetadata = null;

		for (String path : bulkMigrationRequest.getDataObjectPaths()) {
			migrationRequest.setS3ArchiveConfigurationId(bulkMigrationRequest.getS3ArchiveConfigurationId());
			systemGenerateMetadata = validateDataObjectMigrationRequest(path, migrationRequest, false, false);
		}

		for (String path : bulkMigrationRequest.getCollectionPaths()) {
			migrationRequest.setS3ArchiveConfigurationId(bulkMigrationRequest.getS3ArchiveConfigurationId());
			systemGenerateMetadata = validateCollectionMigrationRequest(path, migrationRequest, false);
		}

		if (systemGenerateMetadata == null) {
			throw new HpcException("Could not locate system metadata", HpcErrorType.UNEXPECTED_ERROR);
		}

		// Get the System generated metadata.
		return systemGenerateMetadata;
	}

	/**
	 * Validate a migration request
	 *
	 * @param path             The data object path.
	 * @param migrationRequest The migration request.
	 * @param alignArchivePath If true, the file is moved within its current archive
	 *                         to align w/ the iRODs path.
	 * @throws HpcException If the request is invalid.
	 */
	private void validateMigrationRequest(String path, HpcMigrationRequestDTO migrationRequest,
			boolean alignArchivePath) throws HpcException {

		// Validate the following:
		// 1. Path is not empty.
		// 2. Migration request is not empty if alignArchivePath is false
		// 3. Migration request is empty if alignArchivePath is true
		// 3. Target S3 archive configuration exists.

		// Validate the path,
		if (StringUtils.isEmpty(path)) {
			throw new HpcException("Null / Empty path for migration", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (!alignArchivePath) {
			// Validate the migration target S3 archive configuration.
			if (migrationRequest == null || StringUtils.isEmpty(migrationRequest.getS3ArchiveConfigurationId())) {
				throw new HpcException("Null / Empty migration request / s3ArchiveConfigurationId",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			try {
				// If target S3 archive configuration not found, an exception will be raised.
				dataManagementService.getS3ArchiveConfiguration(migrationRequest.getS3ArchiveConfigurationId());
			} catch (HpcException e) {
				throw new HpcException(
						"S3 archive configuration ID not found: " + migrationRequest.getS3ArchiveConfigurationId(),
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		} else {
			if (migrationRequest != null) {
				throw new HpcException("migration request provided w/ alignArchivePath set to true",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}
	}

	/**
	 * Migrate a data object. Validate the request and create a migration task.
	 *
	 * @param path                      The data object path.
	 * @param userId                    The user ID submitted the request.
	 * @param collectionMigrationTaskId (Optional) A collection migration task id
	 *                                  that this data object migration is part of.
	 * @param migrationRequest          The migration request.
	 * @param alignArchivePath          If true, the file is moved within its
	 *                                  current archive to align w/ the iRODs path.
	 * @param retryTaskId               The previous task ID if this is a retry
	 *                                  request.
	 * @param retryUserId               The user retrying the request if this is a
	 *                                  retry request.
	 * @return A data migration response DTO.
	 * @throws HpcException If failed to process the request.
	 */
	private HpcMigrationResponseDTO migrateDataObject(String path, String userId, String collectionMigrationTaskId,
			HpcMigrationRequestDTO migrationRequest, boolean alignArchivePath, String retryTaskId, String retryUserId)
			throws HpcException {
		logger.info("Migrating Data Object: path - {}, align-archive-path - {}", path, alignArchivePath);

		// Input validation.
		HpcSystemGeneratedMetadata metadata = null;
		HpcMigrationResponseDTO migrationResponse = new HpcMigrationResponseDTO();
		try {
			metadata = validateDataObjectMigrationRequest(path, migrationRequest, alignArchivePath, false);

		} catch (Exception e) {
			if (!StringUtils.isEmpty(collectionMigrationTaskId)) {
				// While processing a collection download, if a validation request invalid, we
				// create a task and complete as failed.
				HpcDataMigrationTask dataObjectMigrationTask = dataMigrationService.createDataObjectMigrationTask(path,
						userId, null, null,
						migrationRequest != null ? migrationRequest.getS3ArchiveConfigurationId() : null,
						collectionMigrationTaskId, alignArchivePath, metadata != null ? metadata.getSourceSize() : null,
						retryTaskId, retryUserId, false);
				dataMigrationService.completeDataObjectMigrationTask(dataObjectMigrationTask,
						HpcDataMigrationResult.IGNORED, "Invalid migration request: " + e.getMessage(), null, null);
				migrationResponse.setTaskId(dataObjectMigrationTask.getId());
				return migrationResponse;
			}

			throw e;
		}

		// Create a migration task.
		migrationResponse.setTaskId(dataMigrationService.createDataObjectMigrationTask(path, userId,
				metadata.getConfigurationId(), metadata.getS3ArchiveConfigurationId(),
				migrationRequest != null ? migrationRequest.getS3ArchiveConfigurationId() : null,
				collectionMigrationTaskId, alignArchivePath, metadata.getSourceSize(), retryTaskId, retryUserId, false)
				.getId());

		return migrationResponse;
	}

	/**
	 * Migrate a collection to another archive.
	 *
	 * @param path                 The collection path.
	 * @param userId               The user ID submitted the request.
	 * @param migrationRequest     The migration request DTO.
	 * @param alignArchivePath     If true, the file is moved within its current
	 *                             archive to align w/ the iRODs path.
	 * @param retryTaskId          The previous task ID if this is a retry request.
	 * @param retryUserId          The user retrying the request if this is a retry
	 *                             request.
	 * @param retryFailedItemsOnly if set to true, only failed items of 'taskId'
	 *                             will be retried. Otherwise the collection will be
	 *                             re-scanned for a new migration to include any
	 *                             items added since the previous migration attempt.
	 * @return Migration Response DTO.
	 * @throws HpcException on service failure.
	 */
	private HpcMigrationResponseDTO migrateCollection(String path, String userId,
			HpcMigrationRequestDTO migrationRequest, boolean alignArchivePath, String retryTaskId, String retryUserId,
			Boolean retryFailedItemsOnly) throws HpcException {
		// Input validation.
		HpcSystemGeneratedMetadata metadata = validateCollectionMigrationRequest(path, migrationRequest,
				alignArchivePath);

		// Create a migration task.
		HpcMigrationResponseDTO migrationResponse = new HpcMigrationResponseDTO();
		migrationResponse.setTaskId(
				dataMigrationService.createCollectionMigrationTask(path, userId, metadata.getConfigurationId(),
						migrationRequest != null ? migrationRequest.getS3ArchiveConfigurationId() : null,
						alignArchivePath, retryTaskId, retryUserId, retryFailedItemsOnly).getId());

		return migrationResponse;
	}

	/**
	 * Migrate data objects or collections. Note: API doesn't support mixed, so user
	 * expected to provide a list of data objects or a list of collections, not
	 * both.
	 *
	 * @param migrationRequest     The migration request DTO.
	 * @param userId               The user ID submitted the request.
	 * @param retryTaskId          The previous task ID if this is a retry request.
	 * @param retryUserId          The user retrying the request if this is a retry
	 *                             request.
	 * @param retryFailedItemsOnly if set to true, only failed items of 'taskId'
	 *                             will be retried. Otherwise the collection will be
	 *                             re-scanned for a new migration to include any
	 *                             items added since the previous migration attempt.
	 * @return Migration Response DTO.
	 * @throws HpcException on service failure.
	 */
	private HpcMigrationResponseDTO migrateDataObjectsOrCollections(HpcBulkMigrationRequestDTO migrationRequest,
			String userId, String retryTaskId, String retryUserId, Boolean retryFailedItemsOnly) throws HpcException {
		// Input validation.
		HpcSystemGeneratedMetadata metadata = validateBulkMigrationRequest(migrationRequest);

		HpcDataMigrationTask migrationTask = null;
		if (!migrationRequest.getDataObjectPaths().isEmpty()) {
			// Submit a request to migrate a list of data objects.
			migrationTask = dataMigrationService.createDataObjectsMigrationTask(migrationRequest.getDataObjectPaths(),
					userId, metadata.getConfigurationId(), migrationRequest.getS3ArchiveConfigurationId(), retryTaskId,
					retryUserId, retryFailedItemsOnly);

		} else {
			// Submit a request to migrate a list of collections.
			migrationTask = dataMigrationService.createCollectionsMigrationTask(migrationRequest.getCollectionPaths(),
					userId, metadata.getConfigurationId(), migrationRequest.getS3ArchiveConfigurationId(), retryTaskId,
					retryUserId, retryFailedItemsOnly);
		}
		// Create and return a DTO with the request receipt.
		HpcMigrationResponseDTO migrationResponse = new HpcMigrationResponseDTO();
		migrationResponse.setTaskId(migrationTask.getId());

		return migrationResponse;
	}

	/**
	 * Migrate a collection. Submit a migration task for each data object under the
	 * collection.
	 *
	 * @param collection              The collection to be migrated
	 * @param collectionMigrationTask The migration task.
	 * @throws HpcException If failed to process the request.
	 */
	private void migrateCollection(HpcCollection collection, HpcDataMigrationTask collectionMigrationTask)
			throws HpcException {
		if (Optional.ofNullable(collectionMigrationTask.getRetryFailedItemsOnly()).orElse(false)) {
			migrateFailedItems(collectionMigrationTask);
			return;
		}

		// Iterate through the data objects in the collection and migrate them.
		for (HpcCollectionListingEntry dataObjectEntry : collection.getDataObjects()) {
			// Iterate through the data objects directly under this collection and submit a
			// migration task for each
			HpcMigrationRequestDTO migrationRequest = null;
			if (!collectionMigrationTask.getAlignArchivePath()) {
				migrationRequest = new HpcMigrationRequestDTO();
				migrationRequest.setS3ArchiveConfigurationId(collectionMigrationTask.getToS3ArchiveConfigurationId());
			}
			migrateDataObject(dataObjectEntry.getPath(), collectionMigrationTask.getUserId(),
					collectionMigrationTask.getId(), migrationRequest, collectionMigrationTask.getAlignArchivePath(),
					null, null);
		}

		// Iterate through the sub-collections and migrate them.
		for (HpcCollectionListingEntry subCollectionEntry : collection.getSubCollections()) {
			String subCollectionPath = subCollectionEntry.getPath();
			HpcCollection subCollection = dataManagementService.getFullCollection(subCollectionPath);
			if (subCollection != null) {
				// Migrate this sub-collection.
				migrateCollection(subCollection, collectionMigrationTask);
			}
		}
	}

	/**
	 * Update data object metadata. Validate the request and create a migration
	 * task.
	 *
	 * @param path                   The data object path.
	 * @param bulkMetadataUpdateTask The bulk metadata update task that this data
	 *                               object migration is part of.
	 * @throws HpcException If failed to process the request.
	 */
	private void updateDataObjectMetadata(String path, HpcDataMigrationTask bulkMetadataUpdateTask)
			throws HpcException {
		logger.info("Updating Data Object Metadata: path - {}, bulk-metadata-task-id - {}", path,
				bulkMetadataUpdateTask.getId());

		// Input validation.
		HpcSystemGeneratedMetadata metadata = null;
		try {
			metadata = validateDataObjectMigrationRequest(path, null, false, true);

		} catch (Exception e) {
			// On error / validation failure - we create a data object metadata update task
			// and mark it ignored.
			HpcDataMigrationTask dataObjectMetadataUpdateTask = dataMigrationService.createDataObjectMigrationTask(path,
					bulkMetadataUpdateTask.getUserId(), null, bulkMetadataUpdateTask.getFromS3ArchiveConfigurationId(),
					bulkMetadataUpdateTask.getToS3ArchiveConfigurationId(), bulkMetadataUpdateTask.getId(), false,
					metadata != null ? metadata.getSourceSize() : null, null, null, true);
			dataMigrationService.completeDataObjectMetadataUpdateTask(dataObjectMetadataUpdateTask,
					HpcDataMigrationResult.IGNORED, "Invalid metadata update request: " + e.getMessage());

			logger.error("Failed to create Data Object Metadata update task: path - {}, bulk-metadata-task-id - {}",
					path, bulkMetadataUpdateTask.getId(), e);
			return;
		}

		// Create a migration task.
		dataMigrationService.createDataObjectMigrationTask(path, bulkMetadataUpdateTask.getUserId(),
				metadata.getConfigurationId(), bulkMetadataUpdateTask.getFromS3ArchiveConfigurationId(),
				bulkMetadataUpdateTask.getToS3ArchiveConfigurationId(), bulkMetadataUpdateTask.getId(), false,
				metadata.getSourceSize(), null, null, true);
	}

	/**
	 * Complete data object metadata update migration task. Checks if the data
	 * object is present in the new S3 archive and update the system metadata
	 * accordingly.
	 *
	 * @param dataObjectMetadataUpdateTask The data object metadata update migration
	 *                                     task.
	 * @throws HpcException If failed to update the metadata
	 */
	private void completeDataObjectMetadataUpdate(HpcDataMigrationTask dataObjectMetadataUpdateTask)
			throws HpcException {

		// Update the task and persist.
		dataObjectMetadataUpdateTask.setStatus(HpcDataMigrationStatus.IN_PROGRESS);
		dataMigrationService.updateDataMigrationTask(dataObjectMetadataUpdateTask);

		// TODO - insert thread pool exec here

		// The archive location in the new archive remains the same as the current one.
		// Set it on the task.
		HpcSystemGeneratedMetadata systemGeneratedMetadata = metadataService
				.getDataObjectSystemGeneratedMetadata(dataObjectMetadataUpdateTask.getPath());
		dataObjectMetadataUpdateTask.setFromS3ArchiveLocation(systemGeneratedMetadata.getArchiveLocation());
		dataObjectMetadataUpdateTask.setToS3ArchiveLocation(systemGeneratedMetadata.getArchiveLocation());

		// Validate if metadata update is needed.
		if (systemGeneratedMetadata.getS3ArchiveConfigurationId()
				.equals(dataObjectMetadataUpdateTask.getToS3ArchiveConfigurationId())) {
			// The data object is already in the new archive.
			dataMigrationService.completeDataObjectMetadataUpdateTask(dataObjectMetadataUpdateTask,
					HpcDataMigrationResult.IGNORED, "data object is already in S3 archive ID: "
							+ systemGeneratedMetadata.getS3ArchiveConfigurationId());
			return;
		}

		// Locate the file in the new archive.
		HpcPathAttributes archivePathAttributes = dataTransferService.getPathAttributes(HpcDataTransferType.S_3,
				dataObjectMetadataUpdateTask.getToS3ArchiveLocation(), true,
				dataObjectMetadataUpdateTask.getConfigurationId(),
				dataObjectMetadataUpdateTask.getToS3ArchiveConfigurationId());

		// Validate the data object exists and accessible.
		if (!archivePathAttributes.getExists() || !archivePathAttributes.getIsAccessible()
				|| !archivePathAttributes.getIsFile()) {
			throw new HpcException(
					"Data object not found in new archive location - "
							+ dataObjectMetadataUpdateTask.getToS3ArchiveLocation().getFileContainerId() + ":"
							+ dataObjectMetadataUpdateTask.getToS3ArchiveLocation().getFileId(),
					HpcErrorType.DATA_TRANSFER_ERROR);
		}

		// Validate the file size in the new archive identical to what is in the old
		// archive
		if (!dataObjectMetadataUpdateTask.getSize().equals(archivePathAttributes.getSize())) {
			throw new HpcException("Data object size in new archive (" + archivePathAttributes.getSize()
					+ ") is different than old (" + dataObjectMetadataUpdateTask.getSize() + ")",
					HpcErrorType.DATA_TRANSFER_ERROR);
		}

		// Validate the metadata is set on the data object in the new archive.
		if (!dataTransferService.validateDataObjectMetadata(dataObjectMetadataUpdateTask.getToS3ArchiveLocation(),
				HpcDataTransferType.S_3, dataObjectMetadataUpdateTask.getConfigurationId(),
				dataObjectMetadataUpdateTask.getToS3ArchiveConfigurationId(), systemGeneratedMetadata.getObjectId(),
				systemGeneratedMetadata.getRegistrarId())) {
			throw new HpcException("Data object metadata is not set in new archive", HpcErrorType.DATA_TRANSFER_ERROR);
		}

		// Complete the task by updating the system generated metadata (iRODS).
		dataMigrationService.completeDataObjectMetadataUpdateTask(dataObjectMetadataUpdateTask,
				HpcDataMigrationResult.COMPLETED, null);
	}

	/**
	 * Mark a data migration task in-process to be worked on.
	 *
	 * @param dataMigrationTask The migration task to mark in-process
	 * @return true if the task was claimed, or false otherwise - i.e. another
	 *         process/thread already working on the task.
	 *
	 */
	private boolean markInProcess(HpcDataMigrationTask dataMigrationTask) {
		if (Optional.ofNullable(dataMigrationTask.getInProcess()).orElse(false)) {
			logger.info("Data migration: task - {} already in-process by {}", dataMigrationTask.getId(),
					dataMigrationTask.getServerId());
			return false;
		}

		// Try to claim the task
		try {
			logger.info("Data migration: task - {} attempting to mark in-process.", dataMigrationTask.getId());
			if (!dataMigrationService.markInProcess(dataMigrationTask, true)) {
				logger.info("Data migration: task - {} failed to mark in-process. Already in-process by {}",
						dataMigrationTask.getId(), dataMigrationTask.getServerId());
				return false;
			}
		} catch (HpcException e) {
			logger.error("Data migration: task - {} failed to mark in-process", dataMigrationTask.getId(), e);
			return false;
		}

		logger.info("Data migration: task - {} marked in-process", dataMigrationTask.getId());
		return true;
	}

	/**
	 * Done processing a migration task. Mark in-process to false
	 *
	 * @param dataMigrationTask The migration task to mark done processing.
	 *
	 */
	private void doneProcessingDataMigrationTask(HpcDataMigrationTask dataMigrationTask) {
		try {
			dataMigrationService.markInProcess(dataMigrationTask, false);

		} catch (HpcException e) {
			logger.error("Data migration: task - {} failed to mark done processing", dataMigrationTask.getId(), e);
		}
	}

	/**
	 * Migrate the failed items (files) for retry of collection (bulk) migration
	 * task.
	 *
	 * @param collectionMigrationTask The collection(bulk) migration task.
	 * @throws HpcException If failed to process the request.
	 */
	private void migrateFailedItems(HpcDataMigrationTask collectionMigrationTask) throws HpcException {
		if (StringUtils.isEmpty(collectionMigrationTask.getRetryTaskId())) {
			throw new HpcException("Can't migrate failed items w/o the associated retry migration task",
					HpcErrorType.UNEXPECTED_ERROR);
		}

		for (HpcDataMigrationTaskResult failedTask : dataMigrationService
				.getDataMigrationResults(collectionMigrationTask.getRetryTaskId(), HpcDataMigrationResult.FAILED)) {
			// Iterate through the list of failed items for the bulk migration task that
			// failed, and migrate them.
			HpcMigrationRequestDTO migrationRequest = null;
			if (!collectionMigrationTask.getAlignArchivePath()) {
				migrationRequest = new HpcMigrationRequestDTO();
				migrationRequest.setS3ArchiveConfigurationId(collectionMigrationTask.getToS3ArchiveConfigurationId());
			}
			migrateDataObject(failedTask.getPath(), collectionMigrationTask.getUserId(),
					collectionMigrationTask.getId(), migrationRequest, collectionMigrationTask.getAlignArchivePath(),
					null, null);
		}
	}

	/**
	 * Get a list of data object paths included in this bulk metadata update task.
	 *
	 * @param bulkMetadataUpdateTask The migration task.
	 * @return A list of data object paths
	 * @throws HpcException If failed to process the request.
	 */
	private List<String> getBulkMetadataUpdateDataObjectPaths(HpcDataMigrationTask bulkMetadataUpdateTask)
			throws HpcException {
		// Build the compound query to identify all the data object paths to include in
		// this bulk metadata update task.
		HpcMetadataQuery fromS3ConfigurationIdQuery = new HpcMetadataQuery();
		fromS3ConfigurationIdQuery.setAttribute("s3_archive_configuration_id");
		fromS3ConfigurationIdQuery.setOperator(HpcMetadataQueryOperator.EQUAL);
		fromS3ConfigurationIdQuery.setValue(bulkMetadataUpdateTask.getFromS3ArchiveConfigurationId());

		HpcMetadataQuery archiveFileContainerIdQuery = new HpcMetadataQuery();
		archiveFileContainerIdQuery.setAttribute("archive_file_container_id");
		archiveFileContainerIdQuery.setOperator(HpcMetadataQueryOperator.EQUAL);
		archiveFileContainerIdQuery.setValue(bulkMetadataUpdateTask.getMetadataArchiveFileContainerId());

		HpcMetadataQuery archiveFileIdPatternQuery = new HpcMetadataQuery();
		archiveFileIdPatternQuery.setAttribute("archive_file_id");
		archiveFileIdPatternQuery.setOperator(HpcMetadataQueryOperator.LIKE);
		archiveFileIdPatternQuery.setValue(bulkMetadataUpdateTask.getMetadataArchiveFileIdPattern());

		HpcCompoundMetadataQuery bulkMetadataUpdateCompoundQuery = new HpcCompoundMetadataQuery();
		bulkMetadataUpdateCompoundQuery.setOperator(HpcCompoundMetadataQueryOperator.AND);
		bulkMetadataUpdateCompoundQuery.getQueries().add(fromS3ConfigurationIdQuery);
		bulkMetadataUpdateCompoundQuery.getQueries().add(archiveFileContainerIdQuery);
		bulkMetadataUpdateCompoundQuery.getQueries().add(archiveFileIdPatternQuery);

		HpcCompoundMetadataQueryDTO bulkMetadataUpdateCompoundQueryDTO = new HpcCompoundMetadataQueryDTO();
		bulkMetadataUpdateCompoundQueryDTO.setTotalCount(true);
		bulkMetadataUpdateCompoundQueryDTO.setDetailedResponse(false);
		bulkMetadataUpdateCompoundQueryDTO.setPage(1);
		bulkMetadataUpdateCompoundQueryDTO.setCompoundQuery(bulkMetadataUpdateCompoundQuery);

		// Perform the search for the data objects to include in this metadata update
		// and use pagination to retrieve all the results.
		List<String> dataObjectPaths = new ArrayList<>();
		int totalCount = 0;
		do {
			HpcDataObjectListDTO searchResponseDTO = dataSearchBusService
					.getDataObjects(bulkMetadataUpdateCompoundQueryDTO);
			dataObjectPaths.addAll(searchResponseDTO.getDataObjectPaths());
			bulkMetadataUpdateCompoundQueryDTO.setPage(searchResponseDTO.getPage() + 1);
			totalCount = searchResponseDTO.getTotalCount();
		} while (dataObjectPaths.size() < totalCount);

		return dataObjectPaths;
	}
}
