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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import gov.nih.nci.hpc.bus.HpcDataMigrationBusService;
import gov.nih.nci.hpc.bus.aspect.HpcExecuteAsSystemAccount;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationResult;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationStatus;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcDataMigrationTask;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.dto.datamigration.HpcMigrationRequestDTO;
import gov.nih.nci.hpc.dto.datamigration.HpcMigrationResponseDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataMigrationService;
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
	public HpcMigrationResponseDTO migrateDataObject(String path, HpcMigrationRequestDTO migrationRequest)
			throws HpcException {
		return migrateDataObject(path, securityService.getRequestInvoker().getNciAccount().getUserId(), null,
				migrationRequest);
	}

	@Override
	public HpcMigrationResponseDTO migrateCollection(String path, HpcMigrationRequestDTO migrationRequest)
			throws HpcException {
		// Input validation.
		HpcSystemGeneratedMetadata metadata = validateCollectionMigrationRequest(path, migrationRequest);

		// Create a migration task.
		HpcMigrationResponseDTO migrationResponse = new HpcMigrationResponseDTO();
		migrationResponse.setTaskId(dataMigrationService
				.createCollectionMigrationTask(path, securityService.getRequestInvoker().getNciAccount().getUserId(),
						metadata.getConfigurationId(), migrationRequest.getS3ArchiveConfigurationId())
				.getId());

		return migrationResponse;
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processDataObjectMigrationReceived() throws HpcException {
		dataMigrationService.getDataMigrationTasks(HpcDataMigrationStatus.RECEIVED, HpcDataMigrationType.DATA_OBJECT)
				.forEach(dataObjectMigrationTask -> {
					try {
						logger.info("Migrating Data Object: task - {}, path - {}", dataObjectMigrationTask.getId(),
								dataObjectMigrationTask.getPath());
						dataMigrationService.migrateDataObject(dataObjectMigrationTask);

					} catch (HpcException e) {
						logger.error("Failed to migrate data object: task - {}, path - {}",
								dataObjectMigrationTask.getId(), dataObjectMigrationTask.getPath(), e);
						try {
							dataMigrationService.completeDataObjectMigrationTask(dataObjectMigrationTask,
									HpcDataMigrationResult.FAILED, e.getMessage());

						} catch (HpcException ex) {
							logger.error("Failed to complete data object migration: task - {}, path - {}",
									dataObjectMigrationTask.getId(), dataObjectMigrationTask.getPath(), ex);
						}
					}
				});
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void processCollectionMigrationReceived() throws HpcException {
		dataMigrationService.getDataMigrationTasks(HpcDataMigrationStatus.RECEIVED, HpcDataMigrationType.COLLECTION)
				.forEach(collectionMigrationTask -> {
					try {
						logger.info("Migrating Collection: task - {}, path - {}", collectionMigrationTask.getId(),
								collectionMigrationTask.getPath());

						// Get the collection to be migrated.
						HpcCollection collection = dataManagementService
								.getCollection(collectionMigrationTask.getPath(), true);
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
							dataMigrationService.completeCollectionMigrationTask(collectionMigrationTask,
									e.getMessage());

						} catch (HpcException ex) {
							logger.error("Failed to complete collection migration: task - {}, path - {}",
									collectionMigrationTask.getId(), collectionMigrationTask.getPath(), ex);
						}
					}
				});
	}

	@Override
	@HpcExecuteAsSystemAccount
	public void completeCollectionMigrationInProgress() throws HpcException {
		dataMigrationService.getDataMigrationTasks(HpcDataMigrationStatus.IN_PROGRESS, HpcDataMigrationType.COLLECTION)
				.forEach(collectionMigrationTask -> {
					try {
						logger.info("Completing Collection Migration: task - {}, path - {}",
								collectionMigrationTask.getId(), collectionMigrationTask.getPath());

						dataMigrationService.completeCollectionMigrationTask(collectionMigrationTask, null);

					} catch (HpcException e) {
						logger.error("Failed to complete collection migration: task - {}, path - {}",
								collectionMigrationTask.getId(), collectionMigrationTask.getPath(), e);
						try {
							dataMigrationService.completeCollectionMigrationTask(collectionMigrationTask,
									e.getMessage());

						} catch (HpcException ex) {
							logger.error("Failed to complete collection migration: task - {}, path - {}",
									collectionMigrationTask.getId(), collectionMigrationTask.getPath(), ex);
						}
					}
				});
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
	 * @param path             The data object path.
	 * @param migrationRequest The migration request. Google Drive.
	 * @return The data object system metadata
	 * @throws HpcException If the request is invalid.
	 */
	private HpcSystemGeneratedMetadata validateDataObjectMigrationRequest(String path,
			HpcMigrationRequestDTO migrationRequest) throws HpcException {

		// Validate the following:
		// 1. Path is not empty.
		// 2. Migration request is not empty.
		// 2. Data Object exists.
		// 3. Migration is supported only from S3 archive to S3 Archive.
		// 4. Data Object is archived (i.e. registration completed).

		validateMigrationRequest(path, migrationRequest);

		// Validate that data object exists.
		if (dataManagementService.getDataObject(path) == null) {
			throw new HpcException("Data object doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the System generated metadata.
		HpcSystemGeneratedMetadata metadata = metadataService.getDataObjectSystemGeneratedMetadata(path);

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
		if (!dataTransferStatus.equals(HpcDataTransferUploadStatus.ARCHIVED)) {
			throw new HpcException("Object is not in archived state yet. It is in "
					+ metadata.getDataTransferStatus().value() + " state", HpcRequestRejectReason.FILE_NOT_ARCHIVED);
		}

		return metadata;
	}

	/**
	 * Validate a collection migration request
	 *
	 * @param path             The data object path.
	 * @param migrationRequest The migration request. Google Drive.
	 * @return The data object system metadata
	 * @throws HpcException If the request is invalid.
	 */
	private HpcSystemGeneratedMetadata validateCollectionMigrationRequest(String path,
			HpcMigrationRequestDTO migrationRequest) throws HpcException {

		// Validate the following:
		// 1. Path is not empty.
		// 2. Migration request is not empty.
		// 2. Collection exists.
		// 3. Collection is not empty

		validateMigrationRequest(path, migrationRequest);

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
	 * Validate a migration request
	 *
	 * @param path             The data object path.
	 * @param migrationRequest The migration request. Google Drive.
	 * @throws HpcException If the request is invalid.
	 */
	private void validateMigrationRequest(String path, HpcMigrationRequestDTO migrationRequest) throws HpcException {

		// Validate the following:
		// 1. Path is not empty.
		// 2. Migration request is not empty.
		// 3. Target S3 archive exists.
		//

		// Validate the path,
		if (StringUtils.isEmpty(path)) {
			throw new HpcException("Null / Empty path for migration", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the migration target S3 archive configuration.
		if (migrationRequest == null || StringUtils.isEmpty(migrationRequest.getS3ArchiveConfigurationId())) {
			throw new HpcException("Null / Empty migration request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		try {
			// If target S3 archive configuration not found, an exception will be raised.
			dataManagementService.getS3ArchiveConfiguration(migrationRequest.getS3ArchiveConfigurationId());
		} catch (HpcException e) {
			throw new HpcException(
					"S3 archive configuration ID not found: " + migrationRequest.getS3ArchiveConfigurationId(),
					HpcErrorType.INVALID_REQUEST_INPUT);
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
	 * @throws HpcException If failed to process the request.
	 */
	private HpcMigrationResponseDTO migrateDataObject(String path, String userId, String collectionMigrationTaskId,
			HpcMigrationRequestDTO migrationRequest) throws HpcException {
		// Input validation.
		HpcSystemGeneratedMetadata metadata = validateDataObjectMigrationRequest(path, migrationRequest);

		// Create a migration task.
		HpcMigrationResponseDTO migrationResponse = new HpcMigrationResponseDTO();
		migrationResponse.setTaskId(dataMigrationService.createDataObjectMigrationTask(path, userId,
				metadata.getConfigurationId(), metadata.getS3ArchiveConfigurationId(),
				migrationRequest.getS3ArchiveConfigurationId(), collectionMigrationTaskId).getId());

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

		// Iterate through the data objects in the collection and download them.
		for (HpcCollectionListingEntry dataObjectEntry : collection.getDataObjects()) {
			// Iterate through the data objects directly under this collection and submit a
			// migration task for each
			HpcMigrationRequestDTO migrationRequest = new HpcMigrationRequestDTO();
			migrationRequest.setS3ArchiveConfigurationId(collectionMigrationTask.getToS3ArchiveConfigurationId());
			migrateDataObject(dataObjectEntry.getPath(), collectionMigrationTask.getUserId(),
					collectionMigrationTask.getId(), migrationRequest);
		}

		// Iterate through the sub-collections and migrate them.
		for (HpcCollectionListingEntry subCollectionEntry : collection.getSubCollections()) {
			String subCollectionPath = subCollectionEntry.getPath();
			HpcCollection subCollection = dataManagementService.getCollection(subCollectionPath, true);
			if (subCollection != null) {
				// Migrate this sub-collection.
				migrateCollection(subCollection, collectionMigrationTask);
			}
		}
	}
}
