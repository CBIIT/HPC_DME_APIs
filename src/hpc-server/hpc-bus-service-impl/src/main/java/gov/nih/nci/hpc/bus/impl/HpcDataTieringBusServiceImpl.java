/**
 * HpcDataTieringBusServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus.impl;

import static gov.nih.nci.hpc.util.HpcUtil.toNormalizedPath;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import gov.nih.nci.hpc.bus.HpcDataTieringBusService;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDeepArchiveStatus;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcBulkTierItem;
import gov.nih.nci.hpc.domain.model.HpcBulkTierRequest;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.dto.datatiering.HpcBulkDataObjectTierRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTieringService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * HPC Data Tiering Business Service Implementation.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcDataTieringBusServiceImpl implements HpcDataTieringBusService {

	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Data Tiering Application Service Instance.
	@Autowired
	private HpcDataTieringService dataTieringService = null;

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
	private HpcDataTieringBusServiceImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataTieringBusService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void tierDataObject(String path) throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException("Null path for archive request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the data object exists.
		if (dataManagementService.getDataObject(path) == null) {
			throw new HpcException("Data object doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the System generated metadata.
		HpcSystemGeneratedMetadata metadata = metadataService.getDataObjectSystemGeneratedMetadata(path);

		// Validate links.
		if (metadata.getLinkSourcePath() != null && !metadata.getLinkSourcePath().isEmpty()) {
			throw new HpcException("This is a link to a data object " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the file is archived.
		HpcDataTransferUploadStatus dataTransferStatus = metadata.getDataTransferStatus();
		if (dataTransferStatus == null) {
			throw new HpcException("Unknown upload data transfer status: " + path, HpcErrorType.UNEXPECTED_ERROR);
		}
		if (!dataTransferStatus.equals(HpcDataTransferUploadStatus.ARCHIVED)) {
			throw new HpcException(
					"Object is not in archived state. It is in " + metadata.getDataTransferStatus().value() + " state",
					HpcRequestRejectReason.FILE_NOT_ARCHIVED);
		}

		// Validate the request if tiering is supported for the provider.
		if (!dataTieringService.isTieringSupported(metadata.getConfigurationId(),
				metadata.getS3ArchiveConfigurationId(), HpcDataTransferType.S_3)) {
			throw new HpcException("The tiering API is not supported for this archive provider." + path,
					HpcRequestRejectReason.API_NOT_SUPPORTED);
		}

		List<String> paths = new ArrayList<>();
		paths.add(path);

		HpcNciAccount invokerNciAccount = securityService.getRequestInvoker().getNciAccount();
		// Submit a tiering request.
		dataTieringService.tierDataObject(invokerNciAccount.getUserId(), path, metadata.getArchiveLocation(),
				HpcDataTransferType.S_3, metadata.getConfigurationId());

		// Update deep_archive_status.
		metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null, null, null, null, null, null,
				null, null, HpcDeepArchiveStatus.IN_PROGRESS, Calendar.getInstance());
	}

	@Override
	public void tierCollection(String path) throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException("Null path for tiering request", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		path = toNormalizedPath(path);

		// Validate collection exists.
		HpcCollection collection = dataManagementService.getCollection(path, true);
		if (collection == null) {
			throw new HpcException("Collection doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Verify data objects found under this collection.
		if (!hasDataObjectsUnderPath(collection)) {
			// No data objects found under this collection.
			throw new HpcException("No data objects found under collection" + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Verify that this collection does not contain a data object that
		// belongs to another collection
		verifyArchiveOnlyContainsDataObjectsUnderPath(path);

		List<String> paths = getDataObjectsUnderPathForTiering(collection);

		// Get the System generated metadata.
		HpcSystemGeneratedMetadata metadata = metadataService.getCollectionSystemGeneratedMetadata(path);

		HpcNciAccount invokerNciAccount = securityService.getRequestInvoker().getNciAccount();
		// Submit a tier transfer request.
		dataTieringService.tierCollection(invokerNciAccount.getUserId(), path, HpcDataTransferType.S_3,
				metadata.getConfigurationId());

		// Iterate through the individual data object paths and update
		// deep_archive_status to in_progress
		for (String dataObjectPath : paths) {
			metadataService.updateDataObjectSystemGeneratedMetadata(dataObjectPath, null, null, null, null, null, null,
					null, null, null, null, HpcDeepArchiveStatus.IN_PROGRESS, Calendar.getInstance());
		}

	}

	@Override
	public void tierDataObjectsOrCollections(HpcBulkDataObjectTierRequestDTO tierRequest) throws HpcException {
		// Input validation.
		if (tierRequest == null) {
			throw new HpcException("Null tiering request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (tierRequest.getDataObjectPaths().isEmpty() && tierRequest.getCollectionPaths().isEmpty()) {
			throw new HpcException("No data object or collection paths", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (!tierRequest.getDataObjectPaths().isEmpty() && !tierRequest.getCollectionPaths().isEmpty()) {
			throw new HpcException("Both data object and collection paths provided",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcNciAccount invokerNciAccount = securityService.getRequestInvoker().getNciAccount();

		List<String> paths = new ArrayList<>();
		HpcBulkTierRequest bulkTierRequest = new HpcBulkTierRequest();
		if (!tierRequest.getDataObjectPaths().isEmpty()) {
			// Submit a request to archive a list of data objects.

			// Validate all data object paths requested exist.
			for (String path : tierRequest.getDataObjectPaths()) {
				if (dataManagementService.getDataObject(path) == null) {
					throw new HpcException("Data object doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
				}
				if (StringUtils.isEmpty(path)) {
					throw new HpcException("Null / Empty path in registration request",
							HpcErrorType.INVALID_REQUEST_INPUT);
				}

				// Validate no multiple registration requests for the same path.
				if (paths.stream().anyMatch(s -> path.equals(s))) {
					throw new HpcException("Duplicated path in tiering request list: " + path,
							HpcErrorType.INVALID_REQUEST_INPUT);
				}

				// Get the System generated metadata.
				HpcSystemGeneratedMetadata metadata = metadataService.getDataObjectSystemGeneratedMetadata(path);

				// Validate links.
				if (metadata.getLinkSourcePath() != null && !metadata.getLinkSourcePath().isEmpty()) {
					throw new HpcException("This is a link to a data object " + path,
							HpcErrorType.INVALID_REQUEST_INPUT);
				}
				// Validate the file is archived.
				HpcDataTransferUploadStatus dataTransferStatus = metadata.getDataTransferStatus();
				if (dataTransferStatus == null) {
					logger.error("Unknown upload data transfer status: " + path, HpcErrorType.UNEXPECTED_ERROR);
					continue;
				}
				if (!dataTransferStatus.equals(HpcDataTransferUploadStatus.ARCHIVED)) {
					logger.error("Object is not in archived state. It is in " + metadata.getDataTransferStatus().value()
							+ " state" + path, HpcRequestRejectReason.FILE_NOT_ARCHIVED);
					continue;
				}
				// Check if any data objects archived where tiering is not supported.
				if (!dataTieringService.isTieringSupported(metadata.getConfigurationId(),
						metadata.getS3ArchiveConfigurationId(), HpcDataTransferType.S_3)) {
					logger.info("The tiering API is not supported for this archive provider." + path,
							HpcRequestRejectReason.API_NOT_SUPPORTED);
					continue;
				}

				paths.add(path);
				HpcBulkTierItem item = new HpcBulkTierItem();
				item.setPath(metadata.getArchiveLocation().getFileId());
				item.setConfigurationId(metadata.getConfigurationId());
				bulkTierRequest.getItems().add(item);
			}

			// Submit a data objects tiering task.
			dataTieringService.tierDataObjects(invokerNciAccount.getUserId(), bulkTierRequest, HpcDataTransferType.S_3);

			// Iterate through the individual data object paths and update
			// deep_archive_status to in_progress
			for (String dataObjectPath : paths) {
				metadataService.updateDataObjectSystemGeneratedMetadata(dataObjectPath, null, null, null, null, null,
						null, null, null, null, null, HpcDeepArchiveStatus.IN_PROGRESS, Calendar.getInstance());
			}

		} else {
			// Submit a request to archive a list of collections.

			// Validate all data object paths requested exist.
			boolean dataObjectExist = false;
			for (String collectionPath : tierRequest.getCollectionPaths()) {
				String path = toNormalizedPath(collectionPath);
				HpcCollection collection = dataManagementService.getCollection(path, true);
				if (collection == null) {
					throw new HpcException("Collection doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
				}
				// Verify at least one data object found under these collection.
				if (!dataObjectExist && hasDataObjectsUnderPath(collection)) {
					dataObjectExist = true;
				}

				// Validate no multiple registration requests for the same path.
				if (paths.stream().anyMatch(s -> path.equals(s))) {
					throw new HpcException("Duplicated path in tiering request list: " + path,
							HpcErrorType.INVALID_REQUEST_INPUT);
				}

				// Verify that this collection does not contain a data object that
				// belongs to another collection
				verifyArchiveOnlyContainsDataObjectsUnderPath(path);

				paths.addAll(getDataObjectsUnderPathForTiering(collection));

				HpcBulkTierItem item = new HpcBulkTierItem();
				String configurationId = metadataService.getCollectionSystemGeneratedMetadata(path)
						.getConfigurationId();
				item.setPath(path);
				item.setConfigurationId(configurationId);
				bulkTierRequest.getItems().add(item);
			}

			if (!dataObjectExist) {
				// No data objects found under the list of collection.
				throw new HpcException("No data objects found under collections", HpcErrorType.INVALID_REQUEST_INPUT);
			}

			// Submit a data objects tiering task.
			dataTieringService.tierCollections(invokerNciAccount.getUserId(), bulkTierRequest, HpcDataTransferType.S_3);

			// Iterate through the individual data object paths and update
			// deep_archive_status to in_progress
			for (String dataObjectPath : paths) {
				metadataService.updateDataObjectSystemGeneratedMetadata(dataObjectPath, null, null, null, null, null,
						null, null, null, null, null, HpcDeepArchiveStatus.IN_PROGRESS, Calendar.getInstance());
			}
		}

	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	private List<String> getDataObjectsUnderPathForTiering(HpcCollection collection) throws HpcException {
		List<String> dataObjectPaths = new ArrayList<>();

		// Iterate through the data objects in the collection and add to list.
		for (HpcCollectionListingEntry dataObjectEntry : collection.getDataObjects()) {
			// Get the System generated metadata.
			HpcSystemGeneratedMetadata metadata = metadataService
					.getDataObjectSystemGeneratedMetadata(dataObjectEntry.getPath());
			// Check if this object is a link.
			if (metadata.getLinkSourcePath() != null && !metadata.getLinkSourcePath().isEmpty()) {
				throw new HpcException("Collection contains a link to data object " + dataObjectEntry.getPath(),
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			// Check if it is in archived state.
			if (metadata.getDataTransferStatus() == null) {
				throw new HpcException("Collection contains object with unknown upload data transfer status: "
						+ dataObjectEntry.getPath(), HpcErrorType.UNEXPECTED_ERROR);
			}
			if (!metadata.getDataTransferStatus().equals(HpcDataTransferUploadStatus.ARCHIVED)) {
				throw new HpcException(
						"Collection contains object not in archived state. It is in "
								+ metadata.getDataTransferStatus().value() + " state" + dataObjectEntry.getPath(),
						HpcRequestRejectReason.FILE_NOT_ARCHIVED);
			}
			// Check if any data objects archived where tiering is not supported.
			if (dataTieringService.isTieringSupported(metadata.getConfigurationId(),
					metadata.getS3ArchiveConfigurationId(), HpcDataTransferType.S_3)) {
				// Check if the archive file id contains the same logical path
				if (metadata.getArchiveLocation().getFileId().contains(collection.getAbsolutePath()))
					dataObjectPaths.add(dataObjectEntry.getPath());
				else
					throw new HpcException("Data object in collection is located in a different archive path "
							+ dataObjectEntry.getPath(), HpcErrorType.INVALID_REQUEST_INPUT);
			} else {
				logger.info("The tiering API is not supported for this archive provider." + dataObjectEntry.getPath(),
						HpcRequestRejectReason.API_NOT_SUPPORTED);
			}

		}

		// Iterate through the sub-collections and add them.
		for (HpcCollectionListingEntry subCollectionEntry : collection.getSubCollections()) {
			String subCollectionPath = subCollectionEntry.getPath();
			HpcCollection subCollection = dataManagementService.getCollection(subCollectionPath, true);
			if (subCollection != null) {
				// add this sub-collection.
				dataObjectPaths.addAll(getDataObjectsUnderPathForTiering(subCollection));
			}
		}

		return dataObjectPaths;
	}

	private boolean hasDataObjectsUnderPath(HpcCollection collection) throws HpcException {

		if (!CollectionUtils.isEmpty(collection.getDataObjects()))
			return true;

		for (HpcCollectionListingEntry subCollection : collection.getSubCollections()) {
			HpcCollection childCollection = dataManagementService.getCollection(subCollection.getPath(), true);
			if (hasDataObjectsUnderPath(childCollection))
				return true;
		}
		return false;
	}

	private void verifyArchiveOnlyContainsDataObjectsUnderPath(String path) throws HpcException {
		List<HpcDataObject> dataObjectsInArchive = dataManagementService.getDataObjectArchiveFileIdContainsPath(path);
		for (HpcDataObject dataObject : dataObjectsInArchive) {
			if (!dataObject.getAbsolutePath().contains(path)) {
				throw new HpcException("Data object in the archive belongs to a different collection path "
						+ dataObject.getAbsolutePath(), HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}
	}

}
