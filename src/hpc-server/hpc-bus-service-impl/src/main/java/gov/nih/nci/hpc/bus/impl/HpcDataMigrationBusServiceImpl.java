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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import gov.nih.nci.hpc.bus.HpcDataMigrationBusService;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
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
		// Input validation.
		HpcSystemGeneratedMetadata metadata = validateDataObjectMigrationRequest(path, migrationRequest);

		// Create a migration task.
		HpcMigrationResponseDTO migrationResponse = new HpcMigrationResponseDTO();
		migrationResponse.setTaskId(dataMigrationService.createDataObjectMigrationTask(path,
				securityService.getRequestInvoker().getNciAccount().getUserId(), metadata.getConfigurationId(),
				metadata.getS3ArchiveConfigurationId(), migrationRequest.getS3ArchiveConfigurationId()).getId());

		return migrationResponse;
	}

	@Override
	public HpcMigrationResponseDTO migrateCollection(String path, HpcMigrationRequestDTO migrationRequest)
			throws HpcException {
		HpcMigrationResponseDTO response = new HpcMigrationResponseDTO();
		// response.setTaskId(migrationRequest.getConfigurationId());
		return response;
		/*
		 * // Input validation. if (path == null || downloadRequest == null) { throw new
		 * HpcException("Null path or download request",
		 * HpcErrorType.INVALID_REQUEST_INPUT); }
		 * 
		 * // Validate collection exists. HpcCollection collection =
		 * dataManagementService.getCollection(path, true); if (collection == null) {
		 * throw new HpcException("Collection doesn't exist: " + path,
		 * HpcErrorType.INVALID_REQUEST_INPUT); }
		 * 
		 * // Verify data objects found under this collection. if
		 * (!hasDataObjectsUnderPath(collection)) { // No data objects found under this
		 * collection. throw new HpcException("No data objects found under collection" +
		 * path, HpcErrorType.INVALID_REQUEST_INPUT); }
		 * 
		 * // Get the System generated metadata. HpcSystemGeneratedMetadata metadata =
		 * metadataService.getCollectionSystemGeneratedMetadata(path);
		 * 
		 * // Submit a collection download task. HpcCollectionDownloadTask
		 * collectionDownloadTask = dataTransferService.downloadCollection(path,
		 * downloadRequest.getGlobusDownloadDestination(),
		 * downloadRequest.getS3DownloadDestination(),
		 * downloadRequest.getGoogleDriveDownloadDestination(),
		 * securityService.getRequestInvoker().getNciAccount().getUserId(),
		 * metadata.getConfigurationId());
		 * 
		 * // Create and return a DTO with the request receipt.
		 * HpcCollectionDownloadResponseDTO responseDTO = new
		 * HpcCollectionDownloadResponseDTO();
		 * responseDTO.setTaskId(collectionDownloadTask.getId());
		 * responseDTO.setDestinationLocation(getDestinationLocation(
		 * collectionDownloadTask));
		 * 
		 * return responseDTO;
		 */
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
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
			throw new HpcException("S3 archive configuration ID not found", HpcErrorType.INVALID_REQUEST_INPUT);
		}

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

}
