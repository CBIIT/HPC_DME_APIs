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

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileLocation;
import static gov.nih.nci.hpc.util.HpcUtil.toNormalizedPath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.bus.HpcDataMigrationBusService;
import gov.nih.nci.hpc.domain.datamanagement.HpcAuditRequestType;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcDirectoryScanPathMap;
import gov.nih.nci.hpc.domain.datamanagement.HpcGroupPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcMetadataUpdateItem;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathPermissions;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermissionForCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermissionsForCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectType;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataType;
import gov.nih.nci.hpc.domain.datatransfer.HpcAddArchiveObjectMetadataResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveObjectMetadata;
import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcBoxDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadMethod;
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
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcUserDownloadRequest;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcBulkMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcBulkMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryType;
import gov.nih.nci.hpc.domain.metadata.HpcGroupedMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationItem;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationResult;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationStatus;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationRequest;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationResult;
import gov.nih.nci.hpc.domain.model.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.model.HpcDistinguishedNameSearch;
import gov.nih.nci.hpc.domain.model.HpcDistinguishedNameSearchResult;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcStorageRecoveryConfiguration;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.report.HpcReport;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.domain.report.HpcReportEntryAttribute;
import gov.nih.nci.hpc.domain.report.HpcReportType;
import gov.nih.nci.hpc.domain.user.HpcAuthenticationType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.dto.datamanagement.HpcArchiveDirectoryPermissionsRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcArchivePermissionResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcArchivePermissionResultDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcArchivePermissionsRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcArchivePermissionsResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkMetadataUpdateRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkMetadataUpdateResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkMoveRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkMoveResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionResultSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompleteMultipartUploadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompleteMultipartUploadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementPermissionResultDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDeleteResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDocDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRetryRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcGroupPermissionResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMoveRequestItemDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMoveResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMoveResponseItemDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcPermsForCollectionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermsForCollectionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationTaskDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcRegistrationSummaryDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementSecurityService;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataSearchService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcEventService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcReportService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * HPC Data Management Business Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataManagementBusServiceImpl implements HpcDataManagementBusService {

	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// A list of invalid characters in a path (not acceptable by IRODS).
	private static final List<String> INVALID_PATH_CHARACTERS = Arrays.asList("\\", ";", "?");

	// Archive permissions setting
	private static final int ARCHIVE_FILE_PERMISSIONS_MODE = 440;
	private static final int ARCHIVE_DIRECTORY_PERMISSIONS_MODE = 550;

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//
	// The Data Management Application Service Instance.
	@Autowired
	private HpcDataManagementService dataManagementService = null;

	// The Data Management Security Service Instance.
	@Autowired
	private HpcDataManagementSecurityService dataManagementSecurityService = null;

	// The Data Transfer Application Service Instance.
	@Autowired
	private HpcDataTransferService dataTransferService = null;

	// Security Application Service Instance.
	@Autowired
	private HpcSecurityService securityService = null;

	// The Metadata Application Service Instance.
	@Autowired
	private HpcMetadataService metadataService = null;

	// TheEvent Application Service Instance.
	@Autowired
	private HpcEventService eventService = null;

	// Report Application Service Instance.
	@Autowired
	private HpcReportService reportService = null;

	// Migration Business Service Instance.
	@Autowired
	private HpcDataMigrationBusService migrationBusService;

	// Data Search Application Service instance.
	@Autowired
	private HpcDataSearchService dataSearchService = null;

	// The limit on total number of collections and data objects in a single bulk
	// metadata update request
	@Value("${hpc.bus.bulkMetadataDataUpdateLimit}")
	private int bulkMetadataDataUpdateLimit = 0;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcDataManagementBusServiceImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataManagementBusService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public boolean interrogatePathRef(String path) throws HpcException {
		return dataManagementService.isPathCollection(path);
	}

	@Override
	public boolean registerCollection(String path, HpcCollectionRegistrationDTO collectionRegistration)
			throws HpcException {
		// Determine the data management configuration to use based on the path.
		String configurationId = dataManagementService.findDataManagementConfigurationId(path);
		if (StringUtils.isEmpty(configurationId)) {
			throw new HpcException("Failed to determine data management configuration for: " + path,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcNciAccount invokerNciAccount = securityService.getRequestInvoker().getNciAccount();
		return registerCollection(path, collectionRegistration, invokerNciAccount.getUserId(),
				invokerNciAccount.getFirstName() + " " + invokerNciAccount.getLastName(), configurationId);
	}

	@Override
	public boolean registerCollection(String path, HpcCollectionRegistrationDTO collectionRegistration, String userId,
			String userName, String configurationId) throws HpcException {
		// Input validation.
		validatePath(path);

		if (collectionRegistration == null || collectionRegistration.getMetadataEntries().isEmpty()) {
			throw new HpcException("Null or empty metadata entries", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Create parent collections if requested to.
		createParentCollections(path, collectionRegistration.getCreateParentCollections(),
				collectionRegistration.getParentCollectionsBulkMetadataEntries(), userId, userName, configurationId);
		boolean created = false;
		synchronized (this) {
			// Create a collection directory.
			created = dataManagementService.createDirectory(path);

			// Attach the metadata.
			if (created) {
				boolean registrationCompleted = false;
				try {
					// The below setCoOwnership method adds the system account and the creator of
					// this
					// collection as an additional owner of this collection.See HPCDATAMGM-1882 for
					// details on why this method was created. This now gives error in irods 4.3.1
					// because
					// a user with modify_object permission can now no longer set privileges to a
					// higher
					// level on itself nor assign higher level privileges to other accounts.
					// However,
					// in the newer DME operating mode, we assign the service account ownership to
					// all
					// Archives at the root level plus inheritance is always enabled. Also, there is
					// no
					// use case presently for a user with modify_object (write) only privileges to
					// assign
					// permissions to others. Hence for now the call to this method is being
					// commented
					// OUT. Please do not uncomment this method without reading HPCDATAMGM-1882.
					// dataManagementService.setCoOwnership(path, userId);

					if (!StringUtils.isEmpty(collectionRegistration.getLinkSourcePath())) {
						// This is a registration request w/ link to an existing collection.
						linkCollection(path, collectionRegistration, userId, userName, configurationId);
					} else {
						// Add user provided metadata.
						metadataService.addMetadataToCollection(path, collectionRegistration.getMetadataEntries(),
								configurationId);

						// Generate system metadata and attach to the collection.
						metadataService.addSystemGeneratedMetadataToCollection(path, userId, userName, configurationId,
								null);
					}

					// Validate the collection hierarchy.
					securityService.executeAsSystemAccount(Optional.empty(),
							() -> dataManagementService.validateHierarchy(path, configurationId, false));

					// Add collection update event.
					addCollectionUpdatedEvent(path, true, false, userId, null, null, null);

					registrationCompleted = true;

				} catch (Exception e) {
					logger.error("Unable to complete collection creation: " + e.getMessage(), e);
					throw e;

				} finally {
					if (!registrationCompleted) {
						logger.error("Collection registration failed, deleting new collection: {}", path);
						// Collection registration failed. Remove it from Data Management.
						dataManagementService.delete(path, true);
					}
				}

			}
		}

		if (!created) {
			if (!StringUtils.isEmpty(collectionRegistration.getLinkSourcePath())) {
				throw new HpcException("Link source path provided. Only metadata can be updated.",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			updateCollection(path, collectionRegistration.getMetadataEntries(), userId);
		}

		return created;
	}

	@Override
	public HpcCollectionDTO getCollection(String path, Boolean list, Boolean includeAcl) throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException("Null collection path", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the metadata.
		HpcMetadataEntries metadataEntries = metadataService.getCollectionMetadataEntries(path);
		HpcSystemGeneratedMetadata metadata = metadataService
				.toSystemGeneratedMetadata(metadataEntries.getSelfMetadataEntries());

		// Get the collection.
		HpcCollection collection = dataManagementService.getCollection(path, list != null ? list : false,
				metadata.getLinkSourcePath());
		if (collection == null) {
			throw new HpcException("Collection doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Build the response DTO.
		HpcCollectionDTO collectionDTO = new HpcCollectionDTO();
		collectionDTO.setCollection(collection);
		if (metadataEntries != null && (!metadataEntries.getParentMetadataEntries().isEmpty()
				|| !metadataEntries.getSelfMetadataEntries().isEmpty())) {
			collectionDTO.setMetadataEntries(metadataEntries);
		}

		// Get the total size
		HpcReport report = getTotalSizeReport(path, true);
		if (report != null) {
			collectionDTO.getReports().add(report);
		}

		// Set the permission if requested
		if (Boolean.TRUE.equals(includeAcl)) {
			HpcSubjectPermission subjectPermission = dataManagementService.getCollectionPermission(path);
			if (subjectPermission == null) {
				logger.error("Received null collection permission for: {}", path);
			} else {
				collectionDTO.setPermission(subjectPermission.getPermission());
			}
		}

		return collectionDTO;
	}

	@Override
	public HpcCollectionDTO getCollectionChildren(String path) throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException("Null collection path", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the System generated metadata.
		HpcSystemGeneratedMetadata metadata = metadataService.getCollectionSystemGeneratedMetadata(path);

		HpcCollection collection = dataManagementService.getCollectionChildren(path, metadata.getLinkSourcePath());
		if (collection == null) {
			return null;
		}

		List<Integer> ids = new ArrayList<>();
		for (HpcCollectionListingEntry subCollection : collection.getSubCollections()) {
			ids.add(subCollection.getId());
		}
		for (HpcCollectionListingEntry dataObject : collection.getDataObjects()) {
			ids.add(dataObject.getId());
		}
		if (!ids.isEmpty()) {
			Map<Integer, HpcCollectionListingEntry> childMetadataMap = metadataService.getMetadataForBrowseByIds(ids);
			for (HpcCollectionListingEntry subCollection : collection.getSubCollections()) {
				HpcCollectionListingEntry entry = childMetadataMap.get(subCollection.getId());
				if (entry != null) {
					subCollection.setDataSize(entry.getDataSize());
					// Compute the total size of the collection - this is being temporarily disabled
					/*
					 * HpcReport report = getTotalSizeReport(subCollection.getPath(), true);
					 * if(report != null && !CollectionUtils.isEmpty(report.getReportEntries())) {
					 * String collectionSize = report.getReportEntries().get(0).getValue();
					 * subCollection.setDataSize(Long.parseLong(collectionSize)); }
					 */
					subCollection.setCreatedAt(entry.getCreatedAt());
				}
			}
			for (HpcCollectionListingEntry dataObject : collection.getDataObjects()) {
				HpcCollectionListingEntry entry = childMetadataMap.get(dataObject.getId());
				if (entry != null) {
					dataObject.setDataSize(entry.getDataSize());
					dataObject.setCreatedAt(entry.getCreatedAt());
				}
			}
		}

		HpcCollectionDTO collectionDTO = new HpcCollectionDTO();
		collectionDTO.setCollection(collection);
		collectionDTO.getReports().add(getTotalSizeReport(path, true));

		return collectionDTO;
	}

	@Override
	public HpcCollectionDTO getCollectionChildrenWithPaging(String path, Integer offset, Boolean report)
			throws HpcException {
		// Input validation.
		if (path == null || offset < 0) {
			throw new HpcException("Null collection path or invalid offset", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the System generated metadata.
		HpcSystemGeneratedMetadata metadata = metadataService.getCollectionSystemGeneratedMetadata(path);

		HpcCollection collection = dataManagementService.getCollectionChildrenWithPaging(path, offset,
				metadata.getLinkSourcePath());
		if (collection == null) {
			return null;
		}

		List<Integer> ids = new ArrayList<>();
		for (HpcCollectionListingEntry subCollection : collection.getSubCollections()) {
			ids.add(subCollection.getId());
		}
		for (HpcCollectionListingEntry dataObject : collection.getDataObjects()) {
			ids.add(dataObject.getId());
		}
		if (!ids.isEmpty()) {
			Map<Integer, HpcCollectionListingEntry> childMetadataMap = metadataService.getMetadataForBrowseByIds(ids);
			for (HpcCollectionListingEntry subCollection : collection.getSubCollections()) {
				HpcCollectionListingEntry entry = childMetadataMap.get(subCollection.getId());
				if (entry != null) {
					subCollection.setDataSize(entry.getDataSize());
					subCollection.setCreatedAt(entry.getCreatedAt());
				}
			}
			for (HpcCollectionListingEntry dataObject : collection.getDataObjects()) {
				HpcCollectionListingEntry entry = childMetadataMap.get(dataObject.getId());
				if (entry != null) {
					dataObject.setDataSize(entry.getDataSize());
					dataObject.setCreatedAt(entry.getCreatedAt());
				}
			}
		}

		HpcCollectionDTO collectionDTO = new HpcCollectionDTO();
		collectionDTO.setCollection(collection);
		if (report)
			collectionDTO.getReports().add(getTotalSizeReport(path, true));

		return collectionDTO;
	}

	private HpcReport getTotalSizeReport(String path, boolean isMachineReadable) throws HpcException {

		HpcReport totalSizeReport = null;
		// Get the total size of the collection
		HpcReportCriteria criteria = new HpcReportCriteria();
		criteria.setType(HpcReportType.USAGE_SUMMARY_BY_PATH);
		criteria.setPath(path);
		criteria.setIsMachineReadable(isMachineReadable);
		criteria.getAttributes().add(HpcReportEntryAttribute.TOTAL_DATA_SIZE);
		List<HpcReport> reports = reportService.generateReport(criteria);
		if (!CollectionUtils.isEmpty(reports)) {
			totalSizeReport = reports.get(0);
		}

		return totalSizeReport;
	}

	@Override
	public HpcCollectionDownloadResponseDTO downloadCollection(String path, HpcDownloadRequestDTO downloadRequest)
			throws HpcException {
		// Input validation.
		if (path == null || downloadRequest == null) {
			throw new HpcException("Null path or download request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the System generated metadata.
		HpcSystemGeneratedMetadata metadata = metadataService.getCollectionSystemGeneratedMetadata(path);

		// Validate collection exists.
		HpcCollection collection = dataManagementService.getCollection(path, true, metadata.getLinkSourcePath());
		if (collection == null) {
			throw new HpcException("Collection doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Verify data objects found under this collection.
		if (!dataManagementService.hasDataObjects(collection)) {
			// No data objects found under this collection.
			throw new HpcException("No data objects found under collection" + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (downloadRequest.getAppendPathToDownloadDestination() == null) {
			// Default to false - i.e. don't use the absolute data object path in the
			// download destination.
			downloadRequest.setAppendPathToDownloadDestination(false);
		}

		if (downloadRequest.getAppendCollectionNameToDownloadDestination() == null) {
			// Default to false - i.e. don't use the collection name in the download
			// destination.
			downloadRequest.setAppendCollectionNameToDownloadDestination(false);
		}

		if (downloadRequest.getAppendPathToDownloadDestination()
				&& downloadRequest.getAppendCollectionNameToDownloadDestination()) {
			throw new HpcException("Both append indicators are set: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Submit a collection download task.
		HpcCollectionDownloadTask collectionDownloadTask = dataTransferService.downloadCollection(path,
				downloadRequest.getGlobusDownloadDestination(), downloadRequest.getS3DownloadDestination(),
				downloadRequest.getGoogleDriveDownloadDestination(),
				downloadRequest.getGoogleCloudStorageDownloadDestination(),
				downloadRequest.getAsperaDownloadDestination(), downloadRequest.getBoxDownloadDestination(),
				securityService.getRequestInvoker().getNciAccount().getUserId(), metadata.getConfigurationId(),
				downloadRequest.getAppendPathToDownloadDestination(),
				downloadRequest.getAppendCollectionNameToDownloadDestination());

		// Create and return a DTO with the request receipt.
		HpcCollectionDownloadResponseDTO responseDTO = new HpcCollectionDownloadResponseDTO();
		responseDTO.setTaskId(collectionDownloadTask.getId());
		responseDTO.setDestinationLocation(getDestinationLocation(collectionDownloadTask));

		return responseDTO;
	}

	@Override
	public HpcBulkDataObjectDownloadResponseDTO downloadDataObjectsOrCollections(
			HpcBulkDataObjectDownloadRequestDTO downloadRequest) throws HpcException {
		// Input validation.
		if (downloadRequest == null) {
			throw new HpcException("Null download request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (downloadRequest.getDataObjectPaths().isEmpty() && downloadRequest.getCollectionPaths().isEmpty()) {
			throw new HpcException("No data object or collection paths", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (!downloadRequest.getDataObjectPaths().isEmpty() && !downloadRequest.getCollectionPaths().isEmpty()) {
			throw new HpcException("Both data object and collection paths provided",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (downloadRequest.getAppendPathToDownloadDestination() == null) {
			// Default to true - i.e. use the absolute data object path in the download
			// destination.
			downloadRequest.setAppendPathToDownloadDestination(true);
		}
		if (downloadRequest.getAppendCollectionNameToDownloadDestination() == null) {
			// Default to false - i.e. use the collection name in the download destination.
			downloadRequest.setAppendCollectionNameToDownloadDestination(false);
		}
		if (downloadRequest.getAppendPathToDownloadDestination()
				&& downloadRequest.getAppendCollectionNameToDownloadDestination()) {
			throw new HpcException("Both append indicators are set", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcCollectionDownloadTask collectionDownloadTask = null;
		List<String> errors = new ArrayList<>();
		if (!downloadRequest.getDataObjectPaths().isEmpty()) {
			// Submit a request to download a list of data objects.

			// Validate all data object paths requested exist.
			for (String path : downloadRequest.getDataObjectPaths()) {
				if (dataManagementService.getDataObject(path) == null) {
					errors.add(path);
				}
			}
			if (!errors.isEmpty()) {
				String errorMessage = "Data object doesn't exist: " + StringUtils.join(errors, ", ");
				throw new HpcException(errorMessage, HpcErrorType.INVALID_REQUEST_INPUT);
			}

			// Get configuration ID of the first data object. It will be used to validate
			// the download destination.
			String configurationId = metadataService
					.getDataObjectSystemGeneratedMetadata(downloadRequest.getDataObjectPaths().iterator().next())
					.getConfigurationId();

			// Submit a data objects download task.
			collectionDownloadTask = dataTransferService.downloadDataObjects(downloadRequest.getDataObjectPaths(),
					downloadRequest.getGlobusDownloadDestination(), downloadRequest.getS3DownloadDestination(),
					downloadRequest.getGoogleDriveDownloadDestination(),
					downloadRequest.getGoogleCloudStorageDownloadDestination(),
					downloadRequest.getAsperaDownloadDestination(), downloadRequest.getBoxDownloadDestination(),
					securityService.getRequestInvoker().getNciAccount().getUserId(), configurationId,
					downloadRequest.getAppendPathToDownloadDestination(),
					downloadRequest.getAppendCollectionNameToDownloadDestination());
		} else {
			// Submit a request to download a list of collections.

			// Validate all data object paths requested exist.
			boolean dataObjectExist = false;
			for (String path : downloadRequest.getCollectionPaths()) {
				// Get the System generated metadata.
				HpcSystemGeneratedMetadata metadata = metadataService.getCollectionSystemGeneratedMetadata(path);

				HpcCollection collection = dataManagementService.getCollection(path, true,
						metadata.getLinkSourcePath());
				if (collection == null) {
					errors.add(path);
				}
				// Verify at least one data object found under these collection.
				else if (!dataObjectExist && dataManagementService.hasDataObjects(collection)) {
					dataObjectExist = true;
				}
			}
			if (!errors.isEmpty()) {
				String errorMessage = "Collection doesn't exist: " + StringUtils.join(errors, ", ");
				throw new HpcException(errorMessage, HpcErrorType.INVALID_REQUEST_INPUT);
			}

			if (!dataObjectExist) {
				// No data objects found under the list of collection.
				throw new HpcException("No data objects found under collections", HpcErrorType.INVALID_REQUEST_INPUT);
			}

			// Get configuration ID of the first collection. It will be used to validate
			// the download destination.
			String configurationId = metadataService
					.getCollectionSystemGeneratedMetadata(downloadRequest.getCollectionPaths().iterator().next())
					.getConfigurationId();

			// Submit a data objects download task.
			collectionDownloadTask = dataTransferService.downloadCollections(downloadRequest.getCollectionPaths(),
					downloadRequest.getGlobusDownloadDestination(), downloadRequest.getS3DownloadDestination(),
					downloadRequest.getGoogleDriveDownloadDestination(),
					downloadRequest.getGoogleCloudStorageDownloadDestination(),
					downloadRequest.getAsperaDownloadDestination(), downloadRequest.getBoxDownloadDestination(),
					securityService.getRequestInvoker().getNciAccount().getUserId(), configurationId,
					downloadRequest.getAppendPathToDownloadDestination(),
					downloadRequest.getAppendCollectionNameToDownloadDestination());
		}

		// Create and return a DTO with the request receipt.
		HpcBulkDataObjectDownloadResponseDTO responseDTO = new HpcBulkDataObjectDownloadResponseDTO();
		responseDTO.setTaskId(collectionDownloadTask.getId());
		responseDTO.setDestinationLocation(getDestinationLocation(collectionDownloadTask));

		return responseDTO;
	}

	@Override
	public HpcCollectionDownloadStatusDTO getCollectionDownloadStatus(String taskId) throws HpcException {
		return getCollectionDownloadStatus(taskId, HpcDownloadTaskType.COLLECTION);
	}

	@Override
	public void cancelCollectionDownloadTask(String taskId) throws HpcException {
		// Input validation.
		HpcDownloadTaskStatus taskStatus = dataTransferService.getDownloadTaskStatus(taskId,
				HpcDownloadTaskType.COLLECTION);
		if (taskStatus == null) {
			throw new HpcException("Collection download task not found: " + taskId, HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (!taskStatus.getInProgress()) {
			throw new HpcException("Collection download task not in-progress: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		dataTransferService.cancelCollectionDownloadTask(taskStatus.getCollectionDownloadTask());
	}

	public HpcCollectionDownloadResponseDTO retryCollectionDownloadTask(String taskId,
			HpcDownloadRetryRequestDTO downloadRetryRequest) throws HpcException {
		// Input validation.
		HpcDownloadTaskStatus taskStatus = dataTransferService.getDownloadTaskStatus(taskId,
				HpcDownloadTaskType.COLLECTION);
		if (taskStatus == null) {
			throw new HpcException("Collection download task not found: " + taskId, HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (taskStatus.getInProgress() || taskStatus.getResult() == null) {
			throw new HpcException("Collection download task in-progress: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Submit the download retry request.
		HpcCollectionDownloadTask collectionDownloadTask = dataTransferService.retryCollectionDownloadTask(
				taskStatus.getResult(), downloadRetryRequest.getDestinationOverwrite(),
				downloadRetryRequest.getS3Account(), downloadRetryRequest.getGoogleAccessToken(),
				downloadRetryRequest.getAsperaAccount(), downloadRetryRequest.getBoxAccessToken(),
				downloadRetryRequest.getBoxRefreshToken(),
				securityService.getRequestInvoker().getNciAccount().getUserId());

		// Create and return a DTO with the request receipt.
		HpcCollectionDownloadResponseDTO responseDTO = new HpcCollectionDownloadResponseDTO();
		responseDTO.setTaskId(collectionDownloadTask.getId());
		responseDTO.setDestinationLocation(getDestinationLocation(collectionDownloadTask));
		return responseDTO;
	}

	@Override
	public void deleteCollection(String path, Boolean recursive, Boolean force) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(path)) {
			throw new HpcException("Null / empty path", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the metadata.
		HpcMetadataEntries metadataEntries = metadataService.getCollectionMetadataEntries(path);
		HpcSystemGeneratedMetadata metadata = metadataService
				.toSystemGeneratedMetadata(metadataEntries.getSelfMetadataEntries());

		// Validate the collection exists.
		HpcCollection collection = dataManagementService.getCollection(path, true, metadata.getLinkSourcePath());
		if (collection == null) {
			throw new HpcException("Collection doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the collection is empty if recursive flag is set to false.
		if (!recursive && StringUtils.isEmpty(metadata.getLinkSourcePath())
				&& (!collection.getSubCollections().isEmpty() || !collection.getDataObjects().isEmpty())) {
			throw new HpcException("Collection is not empty: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the collection link is empty if recursive flag is set to true.
		if (!StringUtils.isEmpty(metadata.getLinkSourcePath()) && recursive
				&& (!collection.getSubCollections().isEmpty() || !collection.getDataObjects().isEmpty())) {
			throw new HpcException("Collection link is not empty: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the invoker is the owner of the data object.
		HpcPermission permission = dataManagementService.getCollectionPermission(path).getPermission();
		if (!permission.equals(HpcPermission.OWN)) {
			throw new HpcException(
					"Collection can only be deleted by its owner. Your permission: " + permission.value(),
					HpcRequestRejectReason.DATA_OBJECT_PERMISSION_DENIED);
		}

		// If the invoker is a GroupAdmin, then ensure that for recursive delete:
		// 1. The collection is less than 90 days old
		HpcRequestInvoker invoker = securityService.getRequestInvoker();
		if (recursive && HpcUserRole.GROUP_ADMIN.equals(invoker.getUserRole())) {
			Calendar cutOffDate = Calendar.getInstance();
			cutOffDate.add(Calendar.DAY_OF_YEAR, -90);
			if (collection.getCreatedAt().before(cutOffDate)) {
				String message = "The collection at " + path + " is not eligible for deletion";
				logger.error(message);
				throw new HpcException(message, HpcRequestRejectReason.NOT_AUTHORIZED);
			}
		}

		// Delete the collection.
		boolean deleted = true;
		String message = null;
		Long totalSize = null;
		try {
			if (recursive) {
				// Delete all the data objects in this hierarchy first
				totalSize = deleteDataObjectsInCollections(path, force);
			}

			dataManagementService.delete(path, false);

		} catch (HpcException e) {
			// Collection deletion failed. Capture this in the audit record.
			deleted = false;
			message = e.getMessage();
			throw (e);

		} finally {
			// Add an audit record of this deletion attempt.
			dataManagementService.addAuditRecord(path, HpcAuditRequestType.DELETE_COLLECTION, metadataEntries, null,
					null, deleted, null, message, null, totalSize, null);
		}
	}

	@Override
	public HpcCollectionDownloadStatusDTO getDataObjectsOrCollectionsDownloadStatus(String taskId) throws HpcException {
		HpcCollectionDownloadStatusDTO downloadStatus = getCollectionDownloadStatus(taskId,
				HpcDownloadTaskType.DATA_OBJECT_LIST);
		return downloadStatus != null ? downloadStatus
				: getCollectionDownloadStatus(taskId, HpcDownloadTaskType.COLLECTION_LIST);
	}

	@Override
	public void cancelDataObjectsOrCollectionsDownloadTask(String taskId) throws HpcException {
		// Input validation.
		HpcDownloadTaskStatus taskStatus = dataTransferService.getDownloadTaskStatus(taskId,
				HpcDownloadTaskType.COLLECTION_LIST);
		if (taskStatus == null) {
			taskStatus = dataTransferService.getDownloadTaskStatus(taskId, HpcDownloadTaskType.DATA_OBJECT_LIST);
		}
		if (taskStatus == null) {
			throw new HpcException("Collection / data-object list download task not found: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (!taskStatus.getInProgress()) {
			throw new HpcException("Collection / data-object list download task not in-progress: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		dataTransferService.cancelCollectionDownloadTask(taskStatus.getCollectionDownloadTask());
	}

	public HpcBulkDataObjectDownloadResponseDTO retryDataObjectsOrCollectionsDownloadTask(String taskId,
			HpcDownloadRetryRequestDTO downloadRetryRequest) throws HpcException {

		// Input validation.
		HpcDownloadTaskStatus taskStatus = dataTransferService.getDownloadTaskStatus(taskId,
				HpcDownloadTaskType.COLLECTION_LIST);
		if (taskStatus == null) {
			taskStatus = dataTransferService.getDownloadTaskStatus(taskId, HpcDownloadTaskType.DATA_OBJECT_LIST);
		}
		if (taskStatus == null) {
			throw new HpcException("Collection / data-object list download task not found: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (taskStatus.getInProgress() || taskStatus.getResult() == null) {
			throw new HpcException("Collection / data-object list download task in-progress: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Submit the download retry request.
		HpcCollectionDownloadTask collectionDownloadTask = dataTransferService.retryCollectionDownloadTask(
				taskStatus.getResult(), downloadRetryRequest.getDestinationOverwrite(),
				downloadRetryRequest.getS3Account(), downloadRetryRequest.getGoogleAccessToken(),
				downloadRetryRequest.getAsperaAccount(), downloadRetryRequest.getBoxAccessToken(),
				downloadRetryRequest.getBoxRefreshToken(),
				securityService.getRequestInvoker().getNciAccount().getUserId());

		// Create and return a DTO with the request receipt.
		HpcBulkDataObjectDownloadResponseDTO responseDTO = new HpcBulkDataObjectDownloadResponseDTO();
		responseDTO.setTaskId(collectionDownloadTask.getId());
		responseDTO.setDestinationLocation(getDestinationLocation(collectionDownloadTask));

		return responseDTO;
	}

	@Override
	public HpcDownloadSummaryDTO getDownloadSummary(int page, boolean totalCount, boolean allUsers, Integer pageSize)
			throws HpcException {
		// Get the request invoker user-id.
		String userId = securityService.getRequestInvoker().getNciAccount().getUserId();
		String doc = null;
		if (allUsers) {
			if (securityService.getRequestInvoker().getUserRole().equals(HpcUserRole.SYSTEM_ADMIN))
				doc = "ALL";
			else
				doc = securityService.getRequestInvoker().getNciAccount().getDoc();
		}
		int limit = dataTransferService.getDownloadResultsPageSize();
		if (pageSize != null && pageSize != 0) {
			limit = pageSize;
		}

		// Populate the DTO with active and completed download requests for this user.
		HpcDownloadSummaryDTO downloadSummary = new HpcDownloadSummaryDTO();
		List<HpcUserDownloadRequest> activeRequests = dataTransferService.getDownloadRequests(userId, doc);

		int pageSizeOffset = 0;
		int resultsCount = dataTransferService.getDownloadResultsCount(userId, doc);
		List<HpcUserDownloadRequest> activeRequestsInPage = null;
		if (activeRequests != null && !activeRequests.isEmpty()) {
			if (activeRequests.size() > limit * page) {
				// The active requests to be displayed are more than the size of the page,
				// so restrict the activeRequests displayed to the page limit
				activeRequestsInPage = activeRequests.subList(limit * (page - 1), limit + limit * (page - 1));
			} else if (activeRequests.size() > limit * (page - 1)) {
				// The active requests to be displayed on this page are less than the size of
				// the page
				// so display the remaining activeRequests
				activeRequestsInPage = activeRequests.subList(limit * (page - 1), activeRequests.size());
			}
			if (activeRequestsInPage != null) {
				downloadSummary.getActiveTasks().addAll(activeRequestsInPage);
			}

			// Determine how many rows have been taken up by the activeRequests, these will
			// be
			// subtracted from the page limit later to determine how many rows are available
			// to display
			// the completed requests.
			if (activeRequestsInPage != null && activeRequestsInPage.size() + resultsCount > limit) {
				pageSizeOffset = activeRequestsInPage.size();
			}
		}

		List<HpcUserDownloadRequest> downloadResults = dataTransferService.getDownloadResults(userId, page, doc,
				pageSizeOffset, pageSize);

		downloadSummary.getCompletedTasks().addAll(downloadResults);

		downloadSummary.setPage(page);
		downloadSummary.setLimit(limit);

		if (totalCount) {
			int count = downloadSummary.getCompletedTasks().size() + downloadSummary.getActiveTasks().size();
			downloadSummary.setTotalCount((page == 1 && count < limit) ? count : resultsCount);
		}

		return downloadSummary;
	}

	@Override
	public HpcEntityPermissionsResponseDTO setCollectionPermissions(String path,
			HpcEntityPermissionsDTO collectionPermissionsRequest) throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		validatePermissionsRequest(collectionPermissionsRequest);

		// Validate the collection exists.
		if (!dataManagementService.collectionExists(path)) {
			throw new HpcException("Collection doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcEntityPermissionsResponseDTO permissionsResponse = new HpcEntityPermissionsResponseDTO();

		// Execute all user permission requests for this collection.
		permissionsResponse.getUserPermissionResponses()
				.addAll(setEntityPermissionForUsers(path, true, collectionPermissionsRequest.getUserPermissions()));

		// Execute all group permission requests for this collection.
		permissionsResponse.getGroupPermissionResponses()
				.addAll(setEntityPermissionForGroups(path, true, collectionPermissionsRequest.getGroupPermissions()));

		return permissionsResponse;
	}

	@Override
	public HpcEntityPermissionsDTO getCollectionPermissions(String path) throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the collection exists.
		if (!dataManagementService.collectionExists(path)) {
			return null;
		}

		boolean excludeSysAdminGroup = securityService.getRequestInvoker().getUserRole()
				.equals(HpcUserRole.METADATA_ONLY)
				|| securityService.getRequestInvoker().getUserRole().equals(HpcUserRole.USER);

		return toEntityPermissionsDTO(dataManagementService.getCollectionPermissions(path), excludeSysAdminGroup);
	}

	@Override
	public HpcUserPermissionDTO getCollectionPermission(String path, String userId) throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (userId == null) {
			throw new HpcException("Null userId", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the collection exists.
		if (!dataManagementService.collectionExists(path)) {
			return null;
		}
		HpcSubjectPermission permission = dataManagementService.getCollectionPermission(path, userId);

		return toUserPermissionDTO(permission);
	}

	@Override
	public HpcUserPermsForCollectionsDTO getUserPermissionsOnCollections(String[] collectionPaths, String userId)
			throws HpcException {
		HpcUserPermsForCollectionsDTO usrPrmsOnClltcns = null;
		if (dataManagementSecurityService.userExists(userId)) {
			usrPrmsOnClltcns = new HpcUserPermsForCollectionsDTO();
			usrPrmsOnClltcns.setUserId(userId);
			for (String someCollectionPath : collectionPaths) {
				HpcPermissionForCollection permForColl = fetchCollectionPermission(someCollectionPath, userId);
				if (null != permForColl) {
					usrPrmsOnClltcns.getPermissionsForCollections().add(permForColl);
				}
			}
		} else {
			throw new HpcException("User not found: " + userId, HpcRequestRejectReason.INVALID_NCI_ACCOUNT);
		}
		return usrPrmsOnClltcns;
	}

	@Override
	public HpcUserPermsForCollectionsDTO getUserPermissionsOnChildCollections(String parentPath, String userId)
			throws HpcException {
		HpcUserPermsForCollectionsDTO userPermissionsOnCollections = null;
		if (dataManagementSecurityService.userExists(userId)) {
			userPermissionsOnCollections = new HpcUserPermsForCollectionsDTO();
			userPermissionsOnCollections.setUserId(userId);
			userPermissionsOnCollections.getPermissionsForCollections()
					.addAll(dataManagementService.acquireChildrenCollectionsPermissionsForUser(parentPath, userId));
		} else {
			throw new HpcException("User not found: " + userId, HpcRequestRejectReason.INVALID_NCI_ACCOUNT);
		}
		return userPermissionsOnCollections;
	}

	@Override
	public HpcPermsForCollectionsDTO getAllPermissionsOnCollections(String[] collectionPaths) throws HpcException {
		HpcPermsForCollectionsDTO resultDto = new HpcPermsForCollectionsDTO();
		for (String somePath : collectionPaths) {
			HpcPermissionsForCollection perms4Coll = new HpcPermissionsForCollection();
			perms4Coll.setCollectionPath(somePath);
			perms4Coll.getCollectionPermissions().addAll(dataManagementService.getCollectionPermissions(somePath));
			resultDto.getCollectionPermissions().add(perms4Coll);
		}

		return resultDto;
	}

	@Override
	public HpcDataObjectRegistrationResponseDTO registerDataObject(String path,
			HpcDataObjectRegistrationRequestDTO dataObjectRegistration, File dataObjectFile) throws HpcException {
		// Determine the data management configuration to use based on the path.
		String configurationId = dataManagementService.findDataManagementConfigurationId(path);
		if (StringUtils.isEmpty(configurationId)) {
			throw new HpcException("Failed to determine data management configuration for: " + path,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcNciAccount invokerNciAccount = securityService.getRequestInvoker().getNciAccount();
		return registerDataObject(path, dataObjectRegistration, dataObjectFile, invokerNciAccount.getUserId(),
				invokerNciAccount.getFirstName() + " " + invokerNciAccount.getLastName(), configurationId, true);
	}

	@Override
	public HpcDataObjectRegistrationResponseDTO registerDataObject(String path,
			HpcDataObjectRegistrationRequestDTO dataObjectRegistration, File dataObjectFile, String userId,
			String userName, String configurationId, boolean registrationEventRequired) throws HpcException {
		taskProfilingLog("Registration", path, "Registration started", null);

		// Input validation.
		validatePath(path);

		if (dataObjectRegistration == null) {
			throw new HpcException("Null dataObjectRegistrationDTO", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Checksum validation (if requested by caller).
		validateChecksum(dataObjectFile, dataObjectRegistration.getChecksum());

		// Create parent collections if requested to.
		long timeBefore = System.currentTimeMillis();
		createParentCollections(path, dataObjectRegistration.getCreateParentCollections(),
				dataObjectRegistration.getParentCollectionsBulkMetadataEntries(), userId, userName, configurationId);
		taskProfilingLog("Registration", path, "Parent collections created", System.currentTimeMillis() - timeBefore);

		// Create a data object file (in the data management system).
		HpcDataObjectRegistrationResponseDTO responseDTO = new HpcDataObjectRegistrationResponseDTO();
		timeBefore = System.currentTimeMillis();
		responseDTO.setRegistered(dataManagementService.createFile(path));
		taskProfilingLog("Registration", path, "File created in iRODS", System.currentTimeMillis() - timeBefore);

		// Get the collection type containing the data object.
		String collectionPath = path.substring(0, path.lastIndexOf('/'));
		String collectionType = dataManagementService.getCollectionType(collectionPath);

		// Generate upload URL is defaulted to false. Parts are defaulted to 1 (i.e. no
		// multipart upload)
		boolean generateUploadRequestURL = Optional.ofNullable(dataObjectRegistration.getGenerateUploadRequestURL())
				.orElse(false);
		// Edit Metadata flag is defaulted to true.
		boolean editMetadata = Optional.ofNullable(dataObjectRegistration.getEditMetadata())
						.orElse(true);

		if (responseDTO.getRegistered()) {
			HpcDataObjectUploadResponse uploadResponse = null;
			try {
				// Validate the new data object complies with the hierarchy definition.
				securityService.executeAsSystemAccount(Optional.empty(),
						() -> dataManagementService.validateHierarchy(collectionPath, configurationId, true));

				if (!StringUtils.isEmpty(dataObjectRegistration.getLinkSourcePath())) {
					// This is a registration request w/ link to an existing data object. no data
					// upload is performed. Performing a data management (iRODS) linking.
					linkDataObject(path, dataObjectRegistration, dataObjectFile, userId, userName, configurationId,
							collectionType);

				} else {
					// This is a registration request w/ data upload or archive source linking.
					boolean extractMetadata = Optional.ofNullable(dataObjectRegistration.getExtractMetadata())
							.orElse(false);

					// Attach the user provided metadata.
					timeBefore = System.currentTimeMillis();
					HpcMetadataEntry dataObjectIdMetadataEntry = metadataService.addMetadataToDataObject(path,
							dataObjectRegistration.getMetadataEntries(), configurationId, collectionType);
					taskProfilingLog("Registration", path, "User metadata set in iRODS",
							System.currentTimeMillis() - timeBefore);

					// Attach the user provided extracted metadata (from the physical file)
					if (!dataObjectRegistration.getExtractedMetadataEntries().isEmpty()) {
						if (extractMetadata) {
							throw new HpcException("Extracted metadata provided w/ request to auto extract: " + path,
									HpcErrorType.INVALID_REQUEST_INPUT);
						}
						metadataService.addExtractedMetadataToDataObject(path,
								dataObjectRegistration.getExtractedMetadataEntries(), configurationId, collectionType);
					}

					if (dataObjectRegistration.getArchiveLinkSource() != null) {
						// This is a registration request w/ linking to an existing data object in the
						// archive.
						linkArchiveSource(path, dataObjectRegistration, configurationId, dataObjectIdMetadataEntry,
								userId, userName, registrationEventRequired);

					} else {
						// This is a registration request w/ data upload. Initiate the upload.
						uploadResponse = uploadDataObject(path, dataObjectRegistration, configurationId,
								dataObjectIdMetadataEntry, userId, userName, registrationEventRequired, responseDTO,
								dataObjectFile, generateUploadRequestURL, extractMetadata, collectionType);
					}
				}

			} catch (Exception e) {
				// Data object registration failed. Remove it from Data Management.
				dataManagementService.delete(path, true);

				// Record a data object registration result.
				dataManagementService.addDataObjectRegistrationResult(path,
						toFailedDataObjectRegistrationResult(path, userId, uploadResponse, e.getMessage()), null);

				throw e;
			}

		} else {
			// The data object is already registered. Validate that data or data endpoint
			// is not attached to the request.
			if (dataObjectFile != null || dataObjectRegistration.getGlobusUploadSource() != null
					|| dataObjectRegistration.getS3UploadSource() != null
					|| dataObjectRegistration.getGoogleDriveUploadSource() != null
					|| dataObjectRegistration.getGoogleCloudStorageUploadSource() != null
					|| dataObjectRegistration.getLinkSourcePath() != null) {
				throw new HpcException(
						"A data file by that name already exists in this collection. Only updating metadata is allowed.",
						HpcErrorType.REQUEST_REJECTED);
			}

			// If editMetadata flag is true, update metadata and optionally re-generate upload URL (if data was not
			// uploaded yet).
			HpcDataObjectUploadResponse uploadResponse = Optional.ofNullable(updateDataObject(path,
					dataObjectRegistration.getMetadataEntries(), collectionType, generateUploadRequestURL,
					dataObjectRegistration.getUploadParts(), dataObjectRegistration.getUploadCompletion(),
					dataObjectRegistration.getChecksum(), userId, dataObjectRegistration.getCallerObjectId(),
					editMetadata))
					.orElse(new HpcDataObjectUploadResponse());
			responseDTO.setUploadRequestURL(uploadResponse.getUploadRequestURL());
			responseDTO.setMultipartUpload(uploadResponse.getMultipartUpload());
		}

		taskProfilingLog("Registration", path, "Registration finished", null);
		return responseDTO;
	}

	@Override
	public boolean completeS3Upload(String path, HpcSystemGeneratedMetadata systemGeneratedMetadata)
			throws HpcException {
		// Lookup the archive for this data object.
		HpcPathAttributes archivePathAttributes = dataTransferService.getPathAttributes(
				systemGeneratedMetadata.getDataTransferType(), systemGeneratedMetadata.getArchiveLocation(), true,
				systemGeneratedMetadata.getConfigurationId(), systemGeneratedMetadata.getS3ArchiveConfigurationId());

		if (archivePathAttributes.getExists() && archivePathAttributes.getIsFile()) {
			// The data object is found in archive. i.e. upload was completed successfully.

			// Update the archive data object's system-metadata.
			HpcArchiveObjectMetadata objectMetadata = dataTransferService
					.addSystemGeneratedMetadataToDataObject(systemGeneratedMetadata.getArchiveLocation(),
							systemGeneratedMetadata.getDataTransferType(), systemGeneratedMetadata.getConfigurationId(),
							systemGeneratedMetadata.getS3ArchiveConfigurationId(),
							systemGeneratedMetadata.getObjectId(), systemGeneratedMetadata.getRegistrarId())
					.getArchiveObjectMetadata();

			// Update the data management (iRODS) data object's system-metadata.
			Calendar dataTransferCompleted = Calendar.getInstance();

			Calendar deepArchiveDate = objectMetadata.getDeepArchiveStatus() != null
					&& objectMetadata.getDeepArchiveStatus().equals(HpcDeepArchiveStatus.IN_PROGRESS)
							? Calendar.getInstance()
							: null;
			metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, objectMetadata.getChecksum(),
					HpcDataTransferUploadStatus.ARCHIVED, null, null, dataTransferCompleted,
					archivePathAttributes.getSize(), null, null, objectMetadata.getDeepArchiveStatus(), deepArchiveDate,
					null);

			// Add an event if needed.
			if (Boolean.TRUE.equals(systemGeneratedMetadata.getRegistrationEventRequired())) {
				HpcDataManagementConfiguration dataManagementConfiguration = dataManagementService
						.getDataManagementConfiguration(systemGeneratedMetadata.getConfigurationId());
				eventService.addDataTransferUploadArchivedEvent(systemGeneratedMetadata.getRegistrarId(), path,
						systemGeneratedMetadata.getSourceLocation(), dataTransferCompleted, null, null,
						dataManagementConfiguration.getDoc());
			}

			// Record a registration result.
			systemGeneratedMetadata.setDataTransferCompleted(dataTransferCompleted);
			dataManagementService.addDataObjectRegistrationResult(path, systemGeneratedMetadata, true, null);

			return true;
		}

		return false;
	}

	@Override
	public HpcCompleteMultipartUploadResponseDTO completeUrlUpload(String path,
			HpcCompleteMultipartUploadRequestDTO completeMultipartUploadRequest) throws HpcException {
		// input validation.
		if (completeMultipartUploadRequest == null) {
			throw new HpcException("Invalid / Empty multipart completion request" + path,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the System generated metadata.
		HpcSystemGeneratedMetadata metadata = metadataService.getDataObjectSystemGeneratedMetadata(path);

		if (!HpcDataTransferUploadStatus.URL_GENERATED.equals(metadata.getDataTransferStatus())) {
			throw new HpcException("URL upload completion request is invalid for data transfer status: "
					+ metadata.getDataTransferStatus(), HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Upload w/ URL completion is only for multipart or single-part-with-completion
		// uploads.
		HpcDataTransferUploadMethod dataTransferMethod = metadata.getDataTransferMethod();
		if (!HpcDataTransferUploadMethod.URL_MULTI_PART.equals(dataTransferMethod)
				&& !HpcDataTransferUploadMethod.URL_SINGLE_PART_WITH_COMPLETION.equals(dataTransferMethod)) {
			throw new HpcException("URL upload completion request is invalid for upload method: " + dataTransferMethod,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcCompleteMultipartUploadResponseDTO responseDTO = new HpcCompleteMultipartUploadResponseDTO();
		responseDTO.setRegistrationCompletion(true);

		// Complete the multipart upload into the archive.
		if (HpcDataTransferUploadMethod.URL_MULTI_PART.equals(metadata.getDataTransferMethod())) {
			responseDTO.setChecksum(dataTransferService.completeMultipartUpload(metadata.getArchiveLocation(),
					metadata.getDataTransferType(), metadata.getConfigurationId(),
					metadata.getS3ArchiveConfigurationId(), completeMultipartUploadRequest.getMultipartUploadId(),
					completeMultipartUploadRequest.getUploadPartETags()));
		}

		// Complete the data object registration w/ the data management system.
		if (!completeS3Upload(path, metadata)) {
			throw new HpcException("Upload is incomplete. File not found in archive",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return responseDTO;
	}

	@Override
	public HpcBulkDataObjectRegistrationResponseDTO registerDataObjects(
			HpcBulkDataObjectRegistrationRequestDTO bulkDataObjectRegistrationRequest) throws HpcException {
		// Input validation.
		if (bulkDataObjectRegistrationRequest == null
				|| (bulkDataObjectRegistrationRequest.getDataObjectRegistrationItems().isEmpty()
						&& bulkDataObjectRegistrationRequest.getDirectoryScanRegistrationItems().isEmpty())) {
			throw new HpcException("Null / Empty registration request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Scan all directories in the registration request and create individual data
		// object registration
		// requests for all files found. Add these individual data object requests to
		// the list provided by the caller.
		if (!bulkDataObjectRegistrationRequest.getDirectoryScanRegistrationItems().isEmpty()) {
			bulkDataObjectRegistrationRequest.getDataObjectRegistrationItems().addAll(toDataObjectRegistrationItems(
					bulkDataObjectRegistrationRequest.getDirectoryScanRegistrationItems()));
		} else {
			// Single file validation
			checkDataObjectRegistrationDestinationPaths(
					bulkDataObjectRegistrationRequest.getDataObjectRegistrationItems());
		}
		// Normalize the path of the data object registration items (i.e. remove
		// redundant '/', etc).
		bulkDataObjectRegistrationRequest.getDataObjectRegistrationItems()
				.forEach(item -> item.setPath(toNormalizedPath(item.getPath())));

		// If dry-run was requested, simply return the entire list of individual data
		// object registrations.
		// This is so that caller can see the result of the directories scan.
		HpcBulkDataObjectRegistrationResponseDTO responseDTO = new HpcBulkDataObjectRegistrationResponseDTO();
		if (bulkDataObjectRegistrationRequest.getDryRun() != null && bulkDataObjectRegistrationRequest.getDryRun()) {
			responseDTO.getDataObjectRegistrationItems()
					.addAll(bulkDataObjectRegistrationRequest.getDataObjectRegistrationItems());
			return responseDTO;
		}

		validateDataObjectRegistrationDestinationPaths(
				bulkDataObjectRegistrationRequest.getDataObjectRegistrationItems());

		// Break the DTO into a map of registration requests and ensure no duplication
		// of registration paths.
		Map<String, HpcDataObjectRegistrationRequest> dataObjectRegistrationRequests = new HashMap<>();
		for (HpcDataObjectRegistrationItemDTO dataObjectRegistrationItem : bulkDataObjectRegistrationRequest
				.getDataObjectRegistrationItems()) {
			HpcDataObjectRegistrationRequest dataObjectRegistrationRequest = new HpcDataObjectRegistrationRequest();
			dataObjectRegistrationRequest.setCallerObjectId(dataObjectRegistrationItem.getCallerObjectId());
			dataObjectRegistrationRequest
					.setCreateParentCollections(dataObjectRegistrationItem.getCreateParentCollections());
			dataObjectRegistrationRequest.setGlobusUploadSource(dataObjectRegistrationItem.getGlobusUploadSource());
			dataObjectRegistrationRequest.setS3UploadSource(dataObjectRegistrationItem.getS3UploadSource());
			dataObjectRegistrationRequest
					.setGoogleDriveUploadSource(dataObjectRegistrationItem.getGoogleDriveUploadSource());
			dataObjectRegistrationRequest
					.setGoogleCloudStorageUploadSource(dataObjectRegistrationItem.getGoogleCloudStorageUploadSource());
			dataObjectRegistrationRequest
					.setFileSystemUploadSource(dataObjectRegistrationItem.getFileSystemUploadSource());
			dataObjectRegistrationRequest.setArchiveLinkSource(dataObjectRegistrationItem.getArchiveLinkSource());
			dataObjectRegistrationRequest.setLinkSourcePath(dataObjectRegistrationItem.getLinkSourcePath());
			dataObjectRegistrationRequest.getMetadataEntries()
					.addAll(dataObjectRegistrationItem.getDataObjectMetadataEntries());
			dataObjectRegistrationRequest.setParentCollectionsBulkMetadataEntries(
					dataObjectRegistrationItem.getParentCollectionsBulkMetadataEntries());
			dataObjectRegistrationRequest
					.setS3ArchiveConfigurationId(dataObjectRegistrationItem.getS3ArchiveConfigurationId());

			String path = dataObjectRegistrationItem.getPath();
			if (StringUtils.isEmpty(path)) {
				throw new HpcException("Null / Empty path in registration request", HpcErrorType.INVALID_REQUEST_INPUT);
			}

			// Validate no multiple registration requests for the same path.
			if (dataObjectRegistrationRequests.put(path, dataObjectRegistrationRequest) != null) {
				throw new HpcException("Duplicated path in registration requests list: " + path,
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		HpcNciAccount invokerNciAccount = securityService.getRequestInvoker().getNciAccount();

		// Submit a data objects registration task and return a response DTO.
		responseDTO.setTaskId(dataManagementService.registerDataObjects(invokerNciAccount.getUserId(),
				bulkDataObjectRegistrationRequest.getUiURL(), dataObjectRegistrationRequests));

		return responseDTO;
	}

	@Override
	public HpcBulkDataObjectRegistrationStatusDTO getDataObjectsRegistrationStatus(String taskId) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(taskId)) {
			throw new HpcException("Null / Empty registration task ID", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the bulk registration task status.
		HpcBulkDataObjectRegistrationStatus taskStatus = dataManagementService
				.getBulkDataObjectRegistrationTaskStatus(taskId);
		if (taskStatus == null) {
			return null;
		}

		// Map the task status to DTO.
		HpcBulkDataObjectRegistrationStatusDTO registrationStatus = new HpcBulkDataObjectRegistrationStatusDTO();
		registrationStatus.setInProgress(taskStatus.getInProgress());
		if (taskStatus.getInProgress()) {
			// Registration in progress. Populate the DTO accordingly.
			registrationStatus.setTask(toBulkDataObjectRegistrationTaskDTO(taskStatus.getTask(), false));

		} else {
			// Download completed or failed. Populate the DTO accordingly.
			registrationStatus.setTask(toBulkDataObjectRegistrationTaskDTO(taskStatus.getResult(), false));
		}

		return registrationStatus;
	}

	@Override
	public HpcRegistrationSummaryDTO getRegistrationSummary(int page, boolean totalCount, boolean allUsers)
			throws HpcException {
		// Get the request invoker user-id.
		String userId = securityService.getRequestInvoker().getNciAccount().getUserId();
		String doc = null;
		if (allUsers) {
			if (securityService.getRequestInvoker().getUserRole().equals(HpcUserRole.SYSTEM_ADMIN))
				doc = "ALL";
			else
				doc = securityService.getRequestInvoker().getNciAccount().getDoc();
		}

		// Populate the DTO with active and completed registration requests for this
		// user.
		final boolean addUserId = doc == null ? false : true;
		HpcRegistrationSummaryDTO registrationSummary = new HpcRegistrationSummaryDTO();
		List<HpcBulkDataObjectRegistrationTask> activeRequests = dataManagementService.getRegistrationTasks(userId,
				doc);

		int pageSizeOffset = 0;
		int limit = dataManagementService.getRegistrationResultsPageSize();

		int resultsCount = dataManagementService.getRegistrationResultsCount(userId, doc);
		List<HpcBulkDataObjectRegistrationTask> activeRequestsInPage = null;
		if (activeRequests != null && !activeRequests.isEmpty()) {
			if (activeRequests.size() > limit * page) {
				// The active requests to be displayed are more than the size of the page,
				// so restrict the activeRequests displayed to the page limit
				activeRequestsInPage = activeRequests.subList(limit * (page - 1), limit + limit * (page - 1));
			} else if (activeRequests.size() > limit * (page - 1)) {
				// The active requests to be displayed on this page are less than the size of
				// the page
				// so display the remaining activeRequests
				activeRequestsInPage = activeRequests.subList(limit * (page - 1), activeRequests.size());
			}
			if (activeRequestsInPage != null) {
				for (HpcBulkDataObjectRegistrationTask task : activeRequestsInPage)
					registrationSummary.getActiveTasks().add(toBulkDataObjectRegistrationTaskDTO(task, addUserId));
			}

			// Determine how many rows have been taken up by the activeRequests, these will
			// be
			// subtracted from the page limit later to determine how many rows are available
			// to display
			// the completed requests.
			if (activeRequestsInPage != null && activeRequestsInPage.size() + resultsCount > limit) {
				pageSizeOffset = activeRequestsInPage.size();
			}
		}

		dataManagementService.getRegistrationResults(userId, page, doc, pageSizeOffset)
				.forEach(result -> registrationSummary.getCompletedTasks()
						.add(toBulkDataObjectRegistrationTaskDTO(result, addUserId)));

		registrationSummary.setPage(page);
		registrationSummary.setLimit(limit);

		if (totalCount) {
			int count = registrationSummary.getCompletedTasks().size() + registrationSummary.getActiveTasks().size();
			registrationSummary.setTotalCount((page == 1 && count < limit) ? count : resultsCount);
		}

		return registrationSummary;
	}

	@Deprecated
	@Override
	public HpcDataObjectDTO getDataObjectV1(String path, boolean includeAcl, boolean excludeAttributes,
			boolean excludeParentMetadata) throws HpcException {
		taskProfilingLog("Retrieval", path, "DataObject Retrieval started", null);
		// Input validation.
		if (path == null) {
			throw new HpcException("Null data object path", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the data object.
		HpcDataObject dataObject = null;
		if (!excludeAttributes) {
			long timeBefore = System.currentTimeMillis();
			dataObject = dataManagementService.getDataObject(path);
			taskProfilingLog("Retrieval", path, "Retrieved dataObject", System.currentTimeMillis() - timeBefore);
			if (dataObject == null) {
				throw new HpcException("Data object doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Get the metadata for this data object.
		long timeBefore = System.currentTimeMillis();
		HpcMetadataEntries metadataEntries = metadataService.getDataObjectMetadataEntries(path, excludeParentMetadata);
		if (metadataEntries == null) {
			throw new HpcException("Data object doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}
		taskProfilingLog("Retrieval", path, "Retrieved metadata entries", System.currentTimeMillis() - timeBefore);

		HpcDataObjectDTO dataObjectDTO = new HpcDataObjectDTO();
		dataObjectDTO.setDataObject(dataObject);
		dataObjectDTO.setMetadataEntries(metadataEntries);
		timeBefore = System.currentTimeMillis();
		dataObjectDTO.setPercentComplete(dataTransferService.getDataObjectUploadProgress(
				metadataService.toSystemGeneratedMetadata(metadataEntries.getSelfMetadataEntries())));
		taskProfilingLog("Retrieval", path, "Retrieved upload progress", System.currentTimeMillis() - timeBefore);

		if (includeAcl) {
			// Set the permission.
			timeBefore = System.currentTimeMillis();
			HpcSubjectPermission subjectPermission = dataManagementService.getDataObjectPermission(path);
			taskProfilingLog("Retrieval", path, "Retrieved dataObject permission",
					System.currentTimeMillis() - timeBefore);
			dataObjectDTO
					.setPermission(subjectPermission != null ? subjectPermission.getPermission() : HpcPermission.NONE);
		}

		taskProfilingLog("Retrieval", path, "DataObject Retrieval finished", null);
		return dataObjectDTO;
	}

	@Override
	public gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectDTO getDataObject(String path, boolean includeAcl,
			boolean excludeAttributes, boolean excludeParentMetadata) throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException("Null data object path", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the data object.
		HpcDataObject dataObject = null;
		if (!excludeAttributes) {
			dataObject = dataManagementService.getDataObject(path);
			if (dataObject == null) {
				throw new HpcException("Data object doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Get the metadata for this data object.
		HpcGroupedMetadataEntries metadataEntries = metadataService.getDataObjectGroupedMetadataEntries(path,
				excludeParentMetadata);
		if (metadataEntries == null) {
			throw new HpcException("Data object doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}
		gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectDTO dataObjectDTO = new gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectDTO();
		dataObjectDTO.setDataObject(dataObject);
		dataObjectDTO.setMetadataEntries(metadataEntries);
		dataObjectDTO.setPercentComplete(dataTransferService.getDataObjectUploadProgress(metadataService
				.toSystemGeneratedMetadata(metadataEntries.getSelfMetadataEntries().getSystemMetadataEntries())));

		if (includeAcl) {
			// Set the permission.
			HpcSubjectPermission subjectPermission = dataManagementService.getDataObjectPermission(path);
			dataObjectDTO
					.setPermission(subjectPermission != null ? subjectPermission.getPermission() : HpcPermission.NONE);
		}

		return dataObjectDTO;
	}

	@Override
	public String getDataType(String path) throws HpcException {
		// Check if path associated with Collection
		if (dataManagementService.isPathCollection(path)) {
			return HpcDataType.COLLECTION.toString();
		} else {
			// Check if path is a DataObject path
			// Get DataObject metadata attributes for this path
			boolean excludeParentMetadata = true;
			HpcGroupedMetadataEntries dataObjectMetadataEntries = metadataService
					.getDataObjectGroupedMetadataEntries(path, excludeParentMetadata);
			if (dataObjectMetadataEntries != null) {
				return HpcDataType.DATAOBJECT.toString();
			} else {
				// Return null if path does not associated with a Collection or DataObject
				return null;
			}
		}
	}

	@Override
	public HpcDataObjectDownloadResponseDTO downloadDataObject(String path, HpcDownloadRequestDTO downloadRequest)
			throws HpcException {
		return downloadDataObject(path, downloadRequest, null,
				securityService.getRequestInvoker().getNciAccount().getUserId(), null, true, null);
	}

	@Override
	public HpcDataObjectDownloadResponseDTO downloadDataObject(String path, HpcDownloadRequestDTO downloadRequest,
			String retryTaskId, String userId, String retryUserId, boolean completionEvent,
			String collectionDownloadTaskId) throws HpcException {
		// Input validation.
		if (downloadRequest == null) {
			throw new HpcException("Null download request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Append path/collection-name is only for collection download request.
		if (downloadRequest.getAppendPathToDownloadDestination() != null
				|| downloadRequest.getAppendCollectionNameToDownloadDestination() != null) {
			throw new HpcException("Append path/collection-name is not applicable", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the following:
		// 1. Path is not empty.
		// 2. Data Object exists.
		// 3. Download to Google Drive / Google Cloud Storage / Aspera / Box destination
		// is supported only from S3 archive.
		// 4. Data Object is archived (i.e. registration completed).
		HpcSystemGeneratedMetadata metadata = validateDataObjectDownloadRequest(path,
				downloadRequest.getGoogleDriveDownloadDestination() != null
						|| downloadRequest.getGoogleCloudStorageDownloadDestination() != null
						|| downloadRequest.getAsperaDownloadDestination() != null
						|| downloadRequest.getBoxDownloadDestination() != null,
				false);

		// Download the data object.
		HpcDataObjectDownloadResponse downloadResponse = dataTransferService.downloadDataObject(path,
				metadata.getArchiveLocation(), downloadRequest.getGlobusDownloadDestination(),
				downloadRequest.getS3DownloadDestination(), downloadRequest.getGoogleDriveDownloadDestination(),
				downloadRequest.getGoogleCloudStorageDownloadDestination(),
				downloadRequest.getAsperaDownloadDestination(), downloadRequest.getBoxDownloadDestination(),
				downloadRequest.getSynchronousDownloadFilter(), metadata.getDataTransferType(),
				metadata.getConfigurationId(), metadata.getS3ArchiveConfigurationId(), retryTaskId, userId, retryUserId,
				completionEvent, collectionDownloadTaskId,
				metadata.getSourceSize() != null ? metadata.getSourceSize() : 0, metadata.getDataTransferStatus(),
				metadata.getDeepArchiveStatus());

		// Construct and return a DTO.
		return toDownloadResponseDTO(downloadResponse.getDestinationLocation(), downloadResponse.getDestinationFile(),
				downloadResponse.getDownloadTaskId(), null, downloadResponse.getRestoreInProgress(),
				metadata.getSourceSize());
	}

	@Override
	public HpcDataObjectDownloadStatusDTO getDataObjectDownloadStatus(String taskId) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(taskId)) {
			throw new HpcException("Null / Empty data object download task ID", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the data object download task status.
		HpcDownloadTaskStatus taskStatus = dataTransferService.getDownloadTaskStatus(taskId,
				HpcDownloadTaskType.DATA_OBJECT);
		if (taskStatus == null) {
			return null;
		}

		// Map the task status to DTO.
		HpcDataObjectDownloadStatusDTO downloadStatus = new HpcDataObjectDownloadStatusDTO();
		downloadStatus.setInProgress(taskStatus.getInProgress());
		if (taskStatus.getInProgress()) {
			// Download in progress. Populate the DTO accordingly.
			downloadStatus.setPath(taskStatus.getDataObjectDownloadTask().getPath());
			downloadStatus.setCreated(taskStatus.getDataObjectDownloadTask().getCreated());
			downloadStatus.setDataTransferRequestId(taskStatus.getDataObjectDownloadTask().getDataTransferRequestId());
			if (taskStatus.getDataObjectDownloadTask().getGlobusDownloadDestination() != null) {
				downloadStatus.setDestinationLocation(
						taskStatus.getDataObjectDownloadTask().getGlobusDownloadDestination().getDestinationLocation());
			} else if (taskStatus.getDataObjectDownloadTask().getS3DownloadDestination() != null) {
				downloadStatus.setDestinationLocation(
						taskStatus.getDataObjectDownloadTask().getS3DownloadDestination().getDestinationLocation());
			} else if (taskStatus.getDataObjectDownloadTask().getGoogleDriveDownloadDestination() != null) {
				downloadStatus.setDestinationLocation(taskStatus.getDataObjectDownloadTask()
						.getGoogleDriveDownloadDestination().getDestinationLocation());
			} else if (taskStatus.getDataObjectDownloadTask().getGoogleCloudStorageDownloadDestination() != null) {
				downloadStatus.setDestinationLocation(taskStatus.getDataObjectDownloadTask()
						.getGoogleCloudStorageDownloadDestination().getDestinationLocation());
			} else if (taskStatus.getDataObjectDownloadTask().getAsperaDownloadDestination() != null) {
				downloadStatus.setDestinationLocation(
						taskStatus.getDataObjectDownloadTask().getAsperaDownloadDestination().getDestinationLocation());
			} else if (taskStatus.getDataObjectDownloadTask().getBoxDownloadDestination() != null) {
				downloadStatus.setDestinationLocation(
						taskStatus.getDataObjectDownloadTask().getBoxDownloadDestination().getDestinationLocation());
			}
			downloadStatus.setDestinationType(taskStatus.getDataObjectDownloadTask().getDestinationType());
			downloadStatus.setPercentComplete(taskStatus.getDataObjectDownloadTask().getPercentComplete());
			downloadStatus.setSize(taskStatus.getDataObjectDownloadTask().getSize());
			downloadStatus.setRestoreInProgress(taskStatus.getDataObjectDownloadTask().getDataTransferStatus()
					.equals(HpcDataTransferDownloadStatus.RESTORE_REQUESTED));
			downloadStatus.setStagingInProgress(Boolean.FALSE.equals(downloadStatus.getRestoreInProgress())
					&& (HpcDataTransferType.GLOBUS.equals(taskStatus.getDataObjectDownloadTask().getDestinationType())
							|| HpcDataTransferType.ASPERA
									.equals(taskStatus.getDataObjectDownloadTask().getDestinationType()))
					&& HpcDataTransferType.S_3.equals(taskStatus.getDataObjectDownloadTask().getDataTransferType())
							? true
							: null);
				downloadStatus
						.setStagingPercentComplete(taskStatus.getDataObjectDownloadTask().getStagingPercentComplete());

			downloadStatus.setRetryUserId(taskStatus.getDataObjectDownloadTask().getRetryUserId());
			downloadStatus.setRetryTaskId(taskStatus.getDataObjectDownloadTask().getRetryTaskId());
		} else {
			// Download completed or failed. Populate the DTO accordingly.
			downloadStatus.setPath(taskStatus.getResult().getPath());
			downloadStatus.setCreated(taskStatus.getResult().getCreated());
			downloadStatus.setDataTransferRequestId(taskStatus.getResult().getDataTransferRequestId());
			downloadStatus.setDestinationLocation(taskStatus.getResult().getDestinationLocation());
			downloadStatus.setDestinationType(taskStatus.getResult().getDestinationType());
			downloadStatus.setCompleted(taskStatus.getResult().getCompleted());
			downloadStatus.setMessage(taskStatus.getResult().getMessage());
			downloadStatus.setResult(taskStatus.getResult().getResult());
			downloadStatus.setEffectiveTrasnsferSpeed(taskStatus.getResult().getEffectiveTransferSpeed() > 0
					? taskStatus.getResult().getEffectiveTransferSpeed()
					: null);
			downloadStatus.setSize(taskStatus.getResult().getSize());
			downloadStatus.setRetryUserId(taskStatus.getResult().getRetryUserId());
			downloadStatus.setRetryTaskId(taskStatus.getResult().getRetryTaskId());
		}

		return downloadStatus;
	}

	@Override
	public HpcDataObjectDownloadResponseDTO retryDataObjectDownloadTask(String taskId,
			HpcDownloadRetryRequestDTO downloadRetryRequest) throws HpcException {
		// Input validation.
		HpcDownloadTaskStatus taskStatus = dataTransferService.getDownloadTaskStatus(taskId,
				HpcDownloadTaskType.DATA_OBJECT);
		if (taskStatus == null) {
			throw new HpcException("Data-object download task not found: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (taskStatus.getInProgress() || taskStatus.getResult() == null) {
			throw new HpcException("Data-object download task in-progress: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (taskStatus.getResult() != null && taskStatus.getResult().getResult().equals(HpcDownloadResult.COMPLETED)) {
			throw new HpcException("Data-object download task already completed: " + taskId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		HpcDownloadTaskResult downloadTask = taskStatus.getResult();
		HpcDownloadRequestDTO downloadRequest = createDownloadRequestDTO(downloadTask, downloadRetryRequest);
		downloadTask.setRetryUserId(securityService.getRequestInvoker().getNciAccount().getUserId());
		return downloadDataObject(downloadTask.getPath(), downloadRequest, downloadTask.getId(),
				downloadTask.getUserId(), downloadTask.getRetryUserId(), true, null);
	}

	@Override
	public HpcDataObjectDownloadResponseDTO generateDownloadRequestURL(String path) throws HpcException {
		// Validate the following:
		// 1. Path is not empty.
		// 2. Data Object exists.
		// 3. Download to S3 destination is supported only from S3 archive.
		// 4. Data Object is archived (i.e. registration completed).
		// 5. Data Object is not in deep archive or in deep archive but restored
		HpcSystemGeneratedMetadata metadata = validateDataObjectDownloadRequest(path, false, true);
		if (metadata.getDeepArchiveStatus() != null) {
			// Get the data object metadata to check for restoration status
			HpcArchiveObjectMetadata objectMetadata = dataTransferService.getDataObjectMetadata(
					metadata.getArchiveLocation(), metadata.getDataTransferType(), metadata.getConfigurationId(),
					metadata.getS3ArchiveConfigurationId());

			String restorationStatus = objectMetadata.getRestorationStatus();

			// 1. If restoration is not requested, storage class is Glacier or deep archive
			// for Cloudian and AWS
			// 2. If restoration is ongoing, storage class is null for Cloudian but remains
			// same for AWS.
			// 3. If restoration is completed, storage class is null for Cloudian but
			// remains same for AWS.
			if ((objectMetadata.getDeepArchiveStatus() != null
					&& (restorationStatus == null || !restorationStatus.equals("success")))
					|| (objectMetadata.getDeepArchiveStatus() == null && restorationStatus != null
							&& !restorationStatus.equals("success"))) {
				throw new HpcException("Object is in deep archived state. Download request URL cannot be generated.",
						HpcRequestRejectReason.FILE_NOT_FOUND);
			}

		}

		// Get the user requesting the download URL.
		HpcNciAccount invokerNciAccount = securityService.getRequestInvoker().getNciAccount();

		// Generate a download URL for the data object, and return it in a DTO.
		return toDownloadResponseDTO(null, null, null,
				dataTransferService.generateDownloadRequestURL(path,
						invokerNciAccount != null ? invokerNciAccount.getUserId() : metadata.getRegistrarId(),
						metadata.getArchiveLocation(), metadata.getDataTransferType(), metadata.getSourceSize(),
						metadata.getConfigurationId(), metadata.getS3ArchiveConfigurationId()),
				metadata.getSourceSize());
	}

	@Override
	public HpcDataObjectDeleteResponseDTO deleteDataObject(String path, Boolean force,
			HpcStorageRecoveryConfiguration storageRecoveryConfiguration) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(path)) {
			throw new HpcException("Null / empty path", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcDataObject dataObject = dataManagementService.getDataObject(path);

		// Validate the data object exists in iRODs.
		if (dataObject == null) {
			throw new HpcException("Data object doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the metadata for this data object.
		HpcMetadataEntries metadataEntries = metadataService.getDataObjectMetadataEntries(path, false);
		HpcSystemGeneratedMetadata systemGeneratedMetadata = metadataService
				.toSystemGeneratedMetadata(metadataEntries.getSelfMetadataEntries());
		boolean registeredLink = systemGeneratedMetadata.getLinkSourcePath() != null;
		if (!registeredLink && systemGeneratedMetadata.getDataTransferStatus() == null) {
			throw new HpcException("Unknown data transfer status", HpcErrorType.UNEXPECTED_ERROR);
		}

		// If it is a softlink, always perform a hard delete
		force = registeredLink ? true: force;
		
		// Validate the data object exists in the Archive. If it is not a softlink.
		if (!registeredLink) {
			HpcPathAttributes archivePathAttributes = dataTransferService.getPathAttributes(
					systemGeneratedMetadata.getDataTransferType(), systemGeneratedMetadata.getArchiveLocation(), true,
					systemGeneratedMetadata.getConfigurationId(), systemGeneratedMetadata.getS3ArchiveConfigurationId());
			if (!archivePathAttributes.getExists() || !archivePathAttributes.getIsFile()) {
				throw new HpcException("The data object was not found in the archive. S3 Archive ID: "
						+ systemGeneratedMetadata.getS3ArchiveConfigurationId() + ". Archive location: "
						+ systemGeneratedMetadata.getArchiveLocation().getFileContainerId() + ":"
						+ systemGeneratedMetadata.getArchiveLocation().getFileId(), HpcErrorType.UNEXPECTED_ERROR);
			}
		}

		// Validate the invoker is the owner of the data object.
		HpcRequestInvoker invoker = securityService.getRequestInvoker();
		if (!invoker.getAuthenticationType().equals(HpcAuthenticationType.SYSTEM_ACCOUNT)) {
			HpcPermission permission = dataManagementService.getDataObjectPermission(path).getPermission();
			if (!permission.equals(HpcPermission.OWN)) {
				throw new HpcException(
						"Data object can only be deleted by its owner. Your permission: " + permission.value(),
						HpcRequestRejectReason.DATA_OBJECT_PERMISSION_DENIED);
			}
		}

		// If this is a GroupAdmin, then ensure that:
		// 1. The file is less than 90 days old (Only soft delete is allowed if the doc
		// config allows deletion after 90 days.)

		if (!registeredLink && HpcUserRole.GROUP_ADMIN.equals(invoker.getUserRole())) {
			Calendar cutOffDate = Calendar.getInstance();
			cutOffDate.add(Calendar.DAY_OF_YEAR, -90);
			boolean deletionAllowed = dataManagementService
					.getDataManagementConfiguration(systemGeneratedMetadata.getConfigurationId()).getDeletionAllowed();
			deletionAllowed = deletionAllowed & !force;
			if (!deletionAllowed && dataObject.getCreatedAt().before(cutOffDate)) {
				String message = "The data object at " + path
						+ " is not eligible for deletion because the file is at least 90 days old.";
				logger.error(message);
				throw new HpcException(message, HpcRequestRejectReason.NOT_AUTHORIZED);
			}
		}

		// Instantiate a response DTO
		HpcDataObjectDeleteResponseDTO dataObjectDeleteResponse = new HpcDataObjectDeleteResponseDTO();
		dataObjectDeleteResponse.setSize(systemGeneratedMetadata.getSourceSize());
		boolean abort = false;

		// Delete all the links to this data object.
		List<HpcDataObject> links = dataManagementService.getDataObjectLinks(path);
		if (!links.isEmpty()) {
			dataObjectDeleteResponse.setLinksDeleteStatus(true);
			for (HpcDataObject link : links) {
				try {
					dataManagementService.delete(link.getAbsolutePath(), false);

				} catch (HpcException e) {
					logger.error("Failed to delete file from datamanagement", e);
					dataObjectDeleteResponse.setLinksDeleteStatus(false);
					dataObjectDeleteResponse.setMessage(e.getMessage());
					abort = true;
					break;
				}
			}
		}

		// Delete the file from the archive (if it's archived and not a link and it is a
		// hard delete).
		if (!registeredLink && force) {
			if (!abort) {
				switch (systemGeneratedMetadata.getDataTransferStatus()) {
				case ARCHIVED:
				case DELETE_REQUESTED:
				case DELETE_FAILED:
					deleteDataObjectFromArchive(path, systemGeneratedMetadata, dataObjectDeleteResponse);
					break;

				default:
					dataObjectDeleteResponse.setArchiveDeleteStatus(false);
					dataObjectDeleteResponse.setMessage("Object is not in archived state. It is in "
							+ systemGeneratedMetadata.getDataTransferStatus().value() + " state");
					break;
				}
				abort = Boolean.FALSE.equals(dataObjectDeleteResponse.getArchiveDeleteStatus());

			} else {
				dataObjectDeleteResponse.setArchiveDeleteStatus(false);
			}
		}

		// Remove the file from data management.
		if (!abort && force) {
			try {
				dataManagementService.delete(path, false);
				dataObjectDeleteResponse.setDataManagementDeleteStatus(true);

			} catch (HpcException e) {
				logger.error("Failed to delete file from datamanagement", e);
				dataObjectDeleteResponse.setDataManagementDeleteStatus(false);
				dataObjectDeleteResponse.setMessage(e.getMessage());
			}
		} else {
			if (!abort) {
				try {
					securityService.executeAsSystemAccount(Optional.empty(),
							() -> dataManagementService.softDelete(path, Optional.of(false)));
					dataObjectDeleteResponse.setDataManagementDeleteStatus(true);
					dataObjectDeleteResponse.setArchiveDeleteStatus(true);
				} catch (HpcException e) {
					logger.error("Failed to soft delete file from datamanagement", e);
					dataObjectDeleteResponse.setDataManagementDeleteStatus(false);
					dataObjectDeleteResponse.setArchiveDeleteStatus(false);
					dataObjectDeleteResponse.setMessage(e.getMessage());
				}
			} else
				dataObjectDeleteResponse.setDataManagementDeleteStatus(false);
		}

		// Add an audit record of this deletion attempt.
		HpcAuditRequestType auditRequestType = storageRecoveryConfiguration == null
				? HpcAuditRequestType.DELETE_DATA_OBJECT
				: HpcAuditRequestType.STORAGE_RECOVERY;
		String userId = storageRecoveryConfiguration == null ? null : "storage-recovery-task";
		dataManagementService.addAuditRecord(path, auditRequestType, metadataEntries, null,
				systemGeneratedMetadata.getArchiveLocation(), dataObjectDeleteResponse.getDataManagementDeleteStatus(),
				dataObjectDeleteResponse.getArchiveDeleteStatus(), dataObjectDeleteResponse.getMessage(), userId,
				systemGeneratedMetadata.getSourceSize(), storageRecoveryConfiguration);

		return dataObjectDeleteResponse;
	}

	@Override
	public HpcEntityPermissionsResponseDTO setDataObjectPermissions(String path,
			HpcEntityPermissionsDTO dataObjectPermissionsRequest) throws HpcException {
		// Input Validation.
		if (path == null) {
			throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		validatePermissionsRequest(dataObjectPermissionsRequest);

		// Validate the data object exists.
		if (dataManagementService.getDataObject(path) == null) {
			throw new HpcException("Data object doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcEntityPermissionsResponseDTO permissionsResponse = new HpcEntityPermissionsResponseDTO();

		// Execute all user permission requests for this data object.
		permissionsResponse.getUserPermissionResponses()
				.addAll(setEntityPermissionForUsers(path, false, dataObjectPermissionsRequest.getUserPermissions()));

		// Execute all group permission requests for this data object.
		permissionsResponse.getGroupPermissionResponses()
				.addAll(setEntityPermissionForGroups(path, false, dataObjectPermissionsRequest.getGroupPermissions()));

		return permissionsResponse;
	}

	@Override
	public HpcEntityPermissionsDTO getDataObjectPermissions(String path) throws HpcException {
		// Input Validation.
		if (path == null) {
			throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		boolean excludeSysAdminGroup = securityService.getRequestInvoker().getUserRole()
				.equals(HpcUserRole.METADATA_ONLY)
				|| securityService.getRequestInvoker().getUserRole().equals(HpcUserRole.USER);

		return toEntityPermissionsDTO(dataManagementService.getDataObjectPermissions(path), excludeSysAdminGroup);
	}

	@Override
	public HpcUserPermissionDTO getDataObjectPermission(String path, String userId) throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (userId == null) {
			throw new HpcException("Null userId", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcSubjectPermission permission = dataManagementService.getDataObjectPermission(path, userId);

		return toUserPermissionDTO(permission);
	}

	@Override
	public HpcArchivePermissionsResponseDTO setArchivePermissions(String path,
			HpcArchivePermissionsRequestDTO archivePermissionsRequest) throws HpcException {
		// Request Validation.
		HpcSystemGeneratedMetadata metadata = validateArchivePermissionsRequest(path, archivePermissionsRequest);

		HpcArchivePermissionsResponseDTO response = new HpcArchivePermissionsResponseDTO();

		// Set the data file archive permissions.
		HpcArchivePermissionResponseDTO dataObjectResponse = new HpcArchivePermissionResponseDTO();
		dataObjectResponse.setPath(path);

		String fileId = metadata.getArchiveLocation().getFileId();
		HpcPathPermissions dataObjectArchivePermissions = new HpcPathPermissions();
		dataObjectArchivePermissions.setPermissionsMode(ARCHIVE_FILE_PERMISSIONS_MODE);
		if (Optional.ofNullable(archivePermissionsRequest.getSetArchivePermissionsFromSource()).orElse(false)) {
			dataObjectArchivePermissions.setOwner(metadata.getSourcePermissions().getOwner());
			dataObjectArchivePermissions.setGroup(metadata.getSourcePermissions().getGroup());
		} else {
			dataObjectArchivePermissions.setOwner(archivePermissionsRequest.getDataObjectPermissions().getOwner());
			dataObjectArchivePermissions.setGroup(archivePermissionsRequest.getDataObjectPermissions().getGroup());
		}

		HpcArchivePermissionResultDTO dataObjectArchivePermissionResult = new HpcArchivePermissionResultDTO();
		dataObjectArchivePermissionResult.setArchivePermissions(dataObjectArchivePermissions);
		dataObjectArchivePermissionResult.setResult(true);

		try {
			dataTransferService.setArchivePermissions(metadata.getConfigurationId(),
					metadata.getS3ArchiveConfigurationId(), metadata.getDataTransferType(), fileId,
					dataObjectArchivePermissions);

		} catch (HpcException e) {
			logger.error("Failed to set archive permissions for {}", path, e);
			dataObjectArchivePermissionResult.setResult(false);
			dataObjectArchivePermissionResult.setMessage(e.getMessage());
		}

		dataObjectResponse.setArchivePermissionResult(dataObjectArchivePermissionResult);

		// Set the data file data management (iRODS) permissions.
		boolean setDataManagementPermissions = Optional
				.ofNullable(archivePermissionsRequest.getSetDataManagementPermissions()).orElse(false);
		if (setDataManagementPermissions) {
			// Perform a DN search and update owner/group if found.
			HpcPathPermissions dnDataObjectArchivePermissions = performDistinguishedNameSearch(
					metadata.getSourceLocation(), dataObjectArchivePermissions);

			HpcDataManagementPermissionResultDTO dataManagementArchivePermissionResult = new HpcDataManagementPermissionResultDTO();
			// if the owner group is 'root', we skip the data management permissions.
			if (!dnDataObjectArchivePermissions.getOwner().equalsIgnoreCase("root")
					&& !dnDataObjectArchivePermissions.getOwner().equalsIgnoreCase("0")) {
				dataManagementArchivePermissionResult.setUserPermissionResult(setEntityPermissionForUser(path, false,
						dnDataObjectArchivePermissions.getOwner(), HpcPermission.OWN));
			}
			if (!dnDataObjectArchivePermissions.getGroup().equalsIgnoreCase("root")
					&& !dnDataObjectArchivePermissions.getGroup().equalsIgnoreCase("0")) {
				dataManagementArchivePermissionResult.setGroupPermissionResult(setEntityPermissionForGroup(path, false,
						dnDataObjectArchivePermissions.getGroup(), HpcPermission.READ));
			}

			dataObjectResponse.setDataManagementArchivePermissionResult(dataManagementArchivePermissionResult);
		}

		response.setDataObjectPermissionsStatus(dataObjectResponse);

		// Set the directories archive permissions
		for (HpcArchiveDirectoryPermissionsRequestDTO archiveDirectoryPermissionsRequest : archivePermissionsRequest
				.getDirectoryPermissions()) {
			HpcPathPermissions directoryArchivePermissions = new HpcPathPermissions();
			directoryArchivePermissions.setPermissionsMode(ARCHIVE_DIRECTORY_PERMISSIONS_MODE);
			directoryArchivePermissions.setOwner(archiveDirectoryPermissionsRequest.getPermissions().getOwner());
			directoryArchivePermissions.setGroup(archiveDirectoryPermissionsRequest.getPermissions().getGroup());
			String directoryPath = archiveDirectoryPermissionsRequest.getPath();
			String directoryId = fileId.substring(0, fileId.indexOf(directoryPath) + directoryPath.length());

			HpcArchivePermissionResponseDTO directoryResponse = new HpcArchivePermissionResponseDTO();
			directoryResponse.setPath(directoryPath);

			HpcArchivePermissionResultDTO directoryArchivePermissionResult = new HpcArchivePermissionResultDTO();
			directoryArchivePermissionResult.setArchivePermissions(directoryArchivePermissions);
			directoryArchivePermissionResult.setResult(true);

			try {
				dataTransferService.setArchivePermissions(metadata.getConfigurationId(),
						metadata.getS3ArchiveConfigurationId(), metadata.getDataTransferType(), directoryId,
						directoryArchivePermissions);

			} catch (HpcException e) {
				logger.error("Failed to set archive permissions for {}", directoryPath, e);
				directoryArchivePermissionResult.setResult(false);
				directoryArchivePermissionResult.setMessage(e.getMessage());
			}

			directoryResponse.setArchivePermissionResult(directoryArchivePermissionResult);

			// Set the directory data management (iRODS) permissions.
			if (setDataManagementPermissions) {
				// Perform a DN search and update owner/group if found.
				HpcPathPermissions dnDirectoryArchivePermissions = performDistinguishedNameSearch(
						metadata.getSourceLocation(), directoryArchivePermissions);

				HpcDataManagementPermissionResultDTO dataManagementArchivePermissionResult = new HpcDataManagementPermissionResultDTO();
				// if the owner group is 'root', we skip the data management permissions.
				if (!dnDirectoryArchivePermissions.getOwner().equalsIgnoreCase("root")
						&& !dnDirectoryArchivePermissions.getOwner().equalsIgnoreCase("0")) {
					dataManagementArchivePermissionResult.setUserPermissionResult(setEntityPermissionForUser(
							directoryPath, true, dnDirectoryArchivePermissions.getOwner(), HpcPermission.OWN));
				}
				if (!dnDirectoryArchivePermissions.getGroup().equalsIgnoreCase("root")
						&& !dnDirectoryArchivePermissions.getGroup().equalsIgnoreCase("0")) {
					dataManagementArchivePermissionResult.setGroupPermissionResult(setEntityPermissionForGroup(
							directoryPath, true, dnDirectoryArchivePermissions.getGroup(), HpcPermission.READ));
				}

				directoryResponse.setDataManagementArchivePermissionResult(dataManagementArchivePermissionResult);
			}

			response.getDirectoryPermissionsStatus().add(directoryResponse);
		}

		return response;
	}

	@Override
	public HpcDataManagementModelDTO getDataManagementModels(Boolean metadataRules) throws HpcException {
		// Create a map DOC -> HpcDocDataManagementRulesDTO
		Map<String, HpcDocDataManagementRulesDTO> docsRules = new HashMap<>();
		for (HpcDataManagementConfiguration dataManagementConfiguration : dataManagementService
				.getDataManagementConfigurations()) {
			String doc = dataManagementConfiguration.getDoc();
			HpcDocDataManagementRulesDTO docRules = docsRules.containsKey(doc) ? docsRules.get(doc)
					: new HpcDocDataManagementRulesDTO();

			HpcDataManagementRulesDTO rules = getDataManagementRules(dataManagementConfiguration, metadataRules);
			docRules.setDoc(doc);
			docRules.getRules().add(rules);
			docsRules.put(doc, docRules);
		}

		// Construct and return the DTO
		HpcDataManagementModelDTO dataManagementModel = new HpcDataManagementModelDTO();
		dataManagementModel.getCollectionSystemGeneratedMetadataAttributeNames()
				.addAll(metadataService.getCollectionSystemMetadataAttributeNames());
		dataManagementModel.getDataObjectSystemGeneratedMetadataAttributeNames()
				.addAll(metadataService.getDataObjectSystemMetadataAttributeNames());
		dataManagementModel.getDocRules().addAll(docsRules.values());

		return dataManagementModel;
	}

	@Override
	public HpcDataManagementModelDTO getDataManagementModel(String basePath, Boolean metadataRules)
			throws HpcException {

		// Construct and return the DTO
		HpcDataManagementModelDTO dataManagementModel = new HpcDataManagementModelDTO();

		for (HpcDataManagementConfiguration dataManagementConfiguration : dataManagementService
				.getDataManagementConfigurations()) {
			if (dataManagementConfiguration.getBasePath().equals(basePath)) {
				HpcDataManagementRulesDTO rules = getDataManagementRules(dataManagementConfiguration, metadataRules);
				HpcDocDataManagementRulesDTO docRules = new HpcDocDataManagementRulesDTO();
				docRules.setDoc(dataManagementConfiguration.getDoc());
				docRules.getRules().add(rules);
				dataManagementModel.getDocRules().add(docRules);
				break;
			}
		}

		if (dataManagementModel.getDocRules().isEmpty()) {
			throw new HpcException("Could not obtain Data Management Model for basePath " + basePath,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		dataManagementModel.getCollectionSystemGeneratedMetadataAttributeNames()
				.addAll(metadataService.getCollectionSystemMetadataAttributeNames());
		dataManagementModel.getDataObjectSystemGeneratedMetadataAttributeNames()
				.addAll(metadataService.getDataObjectSystemMetadataAttributeNames());

		return dataManagementModel;
	}

	private HpcDataManagementRulesDTO getDataManagementRules(HpcDataManagementConfiguration dataManagementConfiguration,
			Boolean metadataRules) {
		HpcDataManagementRulesDTO rules = new HpcDataManagementRulesDTO();
		rules.setId(dataManagementConfiguration.getId());
		rules.setBasePath(dataManagementConfiguration.getBasePath());
		rules.setDataHierarchy(dataManagementConfiguration.getDataHierarchy());

		if (metadataRules) {
			rules.getCollectionMetadataValidationRules()
					.addAll(dataManagementConfiguration.getCollectionMetadataValidationRules());
			rules.getDataObjectMetadataValidationRules()
					.addAll(dataManagementConfiguration.getDataObjectMetadataValidationRules());
		}

		return rules;
	}

	@Override
	public HpcMoveResponseDTO movePath(String path, boolean pathType, String destinationPath, Boolean alignArchivePath)
			throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(path) || StringUtils.isEmpty(destinationPath)) {
			throw new HpcException("Empty path or destinationPath in move request: [path: " + path
					+ "] [destinationPath: " + destinationPath + "]", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Move the path in iRODs.
		dataManagementService.move(path, destinationPath, Optional.of(pathType));

		// Optionally align the archive path (also done by default if caller did not
		// specify).
		HpcMoveResponseDTO moveResponse = null;
		if (Optional.ofNullable(alignArchivePath).orElse(true)) {
			moveResponse = new HpcMoveResponseDTO();
			if (pathType) {
				moveResponse.setTaskId(migrationBusService.migrateCollection(destinationPath, null, true).getTaskId());
			} else {
				moveResponse.setTaskId(migrationBusService.migrateDataObject(destinationPath, null, true).getTaskId());
			}
		}

		return moveResponse;
	}

	@Override
	public HpcBulkMoveResponseDTO movePaths(HpcBulkMoveRequestDTO bulkMoveRequest) throws HpcException {
		// Input validation.
		if (bulkMoveRequest == null) {
			throw new HpcException("Null move request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (bulkMoveRequest.getMoveRequests().isEmpty()) {
			throw new HpcException("No move request items in bulk request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the move requests.
		for (HpcMoveRequestItemDTO moveRequestItem : bulkMoveRequest.getMoveRequests()) {
			if (StringUtils.isEmpty(moveRequestItem.getSourcePath())
					|| StringUtils.isEmpty(moveRequestItem.getDestinationPath())) {
				throw new HpcException(
						"Empty source/destination path in move request: [path: " + moveRequestItem.getSourcePath()
								+ "] [Name: " + moveRequestItem.getDestinationPath() + "]",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Perform the move requests.
		HpcBulkMoveResponseDTO bulkMoveResponse = new HpcBulkMoveResponseDTO();
		bulkMoveResponse.setResult(true);

		bulkMoveRequest.getMoveRequests().forEach(moveRequestItem -> {
			// Normalize the paths.
			moveRequestItem.setSourcePath(toNormalizedPath(moveRequestItem.getSourcePath()));
			moveRequestItem.setDestinationPath(toNormalizedPath(moveRequestItem.getDestinationPath()));

			// Create a response for this move request.
			HpcMoveResponseItemDTO moveResponseItem = new HpcMoveResponseItemDTO();
			moveResponseItem.setRequest(moveRequestItem);

			try {
				HpcPathAttributes sourcePathAttributes = dataManagementService.move(moveRequestItem.getSourcePath(),
						moveRequestItem.getDestinationPath(), Optional.ofNullable(null));

				// Move request is successful.
				moveResponseItem.setResult(true);

				// Optionally align the archive path for the file or collection that
				// successfully moved in iRODS.
				if (Optional.ofNullable(bulkMoveRequest.getAlignArchivePath()).orElse(true)) {
					if (sourcePathAttributes.getIsDirectory()) {
						moveResponseItem.setTaskId(migrationBusService
								.migrateCollection(moveRequestItem.getDestinationPath(), null, true).getTaskId());
					} else {
						moveResponseItem.setTaskId(migrationBusService
								.migrateDataObject(moveRequestItem.getDestinationPath(), null, true).getTaskId());
					}
				}

			} catch (HpcException e) {
				// Move request failed.
				moveResponseItem.setResult(false);
				moveResponseItem.setMessage(e.getMessage());

				// If at least one request failed, we consider the entire bulk request to be
				// failed
				bulkMoveResponse.setResult(false);
			}

			// Add this response to the bulk response.
			bulkMoveResponse.getMoveResponses().add(moveResponseItem);
		});

		return bulkMoveResponse;
	}

	@Override
	public void recoverDataObject(String path) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(path)) {
			throw new HpcException("Empty path in recover request: [path: " + path + "]",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the metadata for this data object.
		HpcMetadataEntries metadataEntries = metadataService.getDataObjectMetadataEntries(path, false);
		HpcSystemGeneratedMetadata systemGeneratedMetadata = metadataService
				.toSystemGeneratedMetadata(metadataEntries.getSelfMetadataEntries());

		if (systemGeneratedMetadata.getDataTransferStatus() == null) {
			throw new HpcException("Unknown data transfer status", HpcErrorType.UNEXPECTED_ERROR);
		}

		dataManagementService.recover(path, Optional.of(false));

	}

	@Override
	public void recoverCollection(String path) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(path)) {
			throw new HpcException("Empty path in recover request: [path: " + path + "]",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the collection exists.
		if (!dataManagementService.collectionExists(path)) {
			throw new HpcException("Collection doesn't exist: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Recover the collection.
		recoverDataObjectsFromCollections(path);
		dataManagementService.delete(path, false);

	}

	@Override
	public HpcBulkMetadataUpdateResponseDTO updateMetadata(HpcBulkMetadataUpdateRequestDTO bulkMetadataUpdateRequest)
			throws HpcException {
		// Input Validation
		if (bulkMetadataUpdateRequest == null || (bulkMetadataUpdateRequest.getDataObjectPaths().size() == 0
				&& bulkMetadataUpdateRequest.getCollectionPaths().size() == 0
				&& bulkMetadataUpdateRequest.getDataObjectCompoundQuery() == null
				&& bulkMetadataUpdateRequest.getCollectionCompoundQuery() == null)) {
			throw new HpcException("No data object / collection paths or queries in metadata update request",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (bulkMetadataUpdateRequest.getMetadataEntries().size() == 0) {
			throw new HpcException("No metadata entries in metadata update request",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Perform the collection and data objects queries to get paths to include in
		// the bulk metadata update request.
		Set<String> collectionPaths = new HashSet<>();
		if (bulkMetadataUpdateRequest.getCollectionCompoundQuery() != null) {
			collectionPaths.addAll(dataSearchService.getCollectionPaths(
					bulkMetadataUpdateRequest.getCollectionCompoundQuery(), 1, bulkMetadataDataUpdateLimit + 1));
		}
		Set<String> dataObjectPaths = new HashSet<>();
		if (bulkMetadataUpdateRequest.getDataObjectCompoundQuery() != null) {
			dataObjectPaths.addAll(dataSearchService.getDataObjectPaths(
					bulkMetadataUpdateRequest.getDataObjectCompoundQuery(), 1, bulkMetadataDataUpdateLimit + 1));
		}

		// Add the collection and data object paths the user requested explicitly to the
		// bulk update paths list.
		bulkMetadataUpdateRequest.getCollectionPaths()
				.forEach(collectionPath -> collectionPaths.add(toNormalizedPath(collectionPath)));
		bulkMetadataUpdateRequest.getDataObjectPaths()
				.forEach(dataObjectPath -> dataObjectPaths.add(toNormalizedPath(dataObjectPath)));
		if (collectionPaths.size() + dataObjectPaths.size() > bulkMetadataDataUpdateLimit) {
			throw new HpcException(
					"Number of collection/data-object path in bulk metadata request exceeds the maximum of "
							+ bulkMetadataDataUpdateLimit,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Perform the individual path metadata update and capture the result in a
		// response DTO.
		HpcBulkMetadataUpdateResponseDTO bulkMetadataUpdateResponse = new HpcBulkMetadataUpdateResponseDTO();
		bulkMetadataUpdateResponse.setResult(true);
		String userId = securityService.getRequestInvoker().getNciAccount().getUserId();

		// Data objects bulk metadata updates.

		// Add an audit record if this is a query update
		if (bulkMetadataUpdateRequest.getDataObjectCompoundQuery() != null) {
			dataManagementService.addBulkUpdateAuditRecord(userId,
					bulkMetadataUpdateRequest.getDataObjectCompoundQuery(), HpcCompoundMetadataQueryType.DATA_OBJECT,
					bulkMetadataUpdateRequest.getMetadataEntries());
		}

		dataObjectPaths.forEach(path -> {
			HpcMetadataUpdateItem item = new HpcMetadataUpdateItem();
			item.setPath(path);
			item.setResult(true);
			try {
				updateDataObject(path, bulkMetadataUpdateRequest.getMetadataEntries(),
						dataManagementService.getCollectionType(path.substring(0, path.lastIndexOf('/'))), false, null,
						null, null, userId, null, true);
			} catch (HpcException e) {
				logger.error("Failed to update data object metadata in a bulk request: {}", path, e);
				item.setResult(false);
				item.setMessage(e.getMessage());
				bulkMetadataUpdateResponse.setResult(false);
			}

			if (item.getResult()) {
				bulkMetadataUpdateResponse.getCompletedItems().add(item);
			} else {
				bulkMetadataUpdateResponse.getFailedItems().add(item);
			}
		});

		// Collections bulk metadata updates.

		// Add an audit record if this is a query update
		if (bulkMetadataUpdateRequest.getCollectionCompoundQuery() != null) {
			dataManagementService.addBulkUpdateAuditRecord(userId,
					bulkMetadataUpdateRequest.getCollectionCompoundQuery(), HpcCompoundMetadataQueryType.COLLECTION,
					bulkMetadataUpdateRequest.getMetadataEntries());
		}

		collectionPaths.forEach(path -> {
			HpcMetadataUpdateItem item = new HpcMetadataUpdateItem();
			item.setPath(path);
			item.setResult(true);

			try {
				updateCollection(path, bulkMetadataUpdateRequest.getMetadataEntries(), userId);
			} catch (HpcException e) {
				logger.error("Failed to update data object metadata in a bulk request: {}", path, e);
				item.setResult(false);
				item.setMessage(e.getMessage());
				bulkMetadataUpdateResponse.setResult(false);
			}

			if (item.getResult()) {
				bulkMetadataUpdateResponse.getCompletedItems().add(item);
			} else {
				bulkMetadataUpdateResponse.getFailedItems().add(item);
			}
		});

		return bulkMetadataUpdateResponse;
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Validate permissions request.
	 *
	 * @param entityPermissionsRequest The request to validate.
	 * @throws HpcException if found an invalid request in the list.
	 */
	private void validatePermissionsRequest(HpcEntityPermissionsDTO entityPermissionsRequest) throws HpcException {
		if (entityPermissionsRequest == null || (entityPermissionsRequest.getUserPermissions().isEmpty()
				&& entityPermissionsRequest.getGroupPermissions().isEmpty())) {
			throw new HpcException("Null or empty permissions request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		Set<String> userIds = new HashSet<>();
		for (HpcUserPermission userPermissionRequest : entityPermissionsRequest.getUserPermissions()) {
			String userId = userPermissionRequest.getUserId();
			HpcPermission permission = userPermissionRequest.getPermission();
			if (StringUtils.isEmpty(userId)) {
				throw new HpcException("Null or empty userId in a permission request",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (!userIds.add(userId)) {
				throw new HpcException("Duplicate userId in a permission request: " + userId,
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (securityService.getUser(userId) == null) {
				throw new HpcException("User not found: " + userId, HpcRequestRejectReason.INVALID_NCI_ACCOUNT);
			}
			if (permission == null) {
				throw new HpcException("Null or empty permission in a permission request. Valid values are ["
						+ Arrays.asList(HpcPermission.values()) + "]", HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate the group permission requests for this path.
		Set<String> groupNames = new HashSet<>();
		for (HpcGroupPermission groupPermissionRequest : entityPermissionsRequest.getGroupPermissions()) {
			String groupName = groupPermissionRequest.getGroupName();
			HpcPermission permission = groupPermissionRequest.getPermission();
			if (StringUtils.isEmpty(groupName)) {
				throw new HpcException("Null or empty group name in a permission request",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (!groupNames.add(groupName)) {
				throw new HpcException("Duplicate group name in a permission request: " + groupName,
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (permission == null) {
				throw new HpcException("Null or empty permission in a permission request. Valid values are ["
						+ Arrays.asList(HpcPermission.values()) + "]", HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}
	}

	/**
	 * Add collection update event.
	 *
	 * @param path                 The path of the entity trigger this event.
	 * @param collectionRegistered An indicator if a collection was registered.
	 * @param dataObjectRegistered An indicator if a data object was registered.
	 * @param userId               The user ID who initiated the action resulted in
	 *                             collection update event.
	 * @param presignURL           The presigned download URL.(Optional)
	 * @param size                 The data size.(Optional)
	 * @param doc                  The DOC (Optional)
	 */
	private void addCollectionUpdatedEvent(String path, boolean collectionRegistered, boolean dataObjectRegistered,
			String userId, String presignURL, String size, String doc) {
		try {
			if (!collectionRegistered && !dataObjectRegistered) {
				// Add collection metadata updated event.
				eventService.addCollectionUpdatedEvent(path, userId);
				return;
			}

			// A collection or data object registered, so we add an event for the parent
			// collection.
			String parentCollection = org.springframework.util.StringUtils.trimTrailingCharacter(path, '/');
			int parentCollectionIndex = parentCollection.lastIndexOf('/');
			parentCollection = parentCollectionIndex <= 0 ? "/" : parentCollection.substring(0, parentCollectionIndex);

			if (collectionRegistered) {
				eventService.addCollectionRegistrationEvent(parentCollection, userId);
			} else {
				eventService.addDataObjectRegistrationEvent(parentCollection, userId, presignURL, size, path, doc);
			}

		} catch (HpcException e) {
			logger.error("Failed to add collection update event", e);
		}
	}

	/**
	 * Construct a download response DTO object.
	 *
	 * @param destinationLocation The destination file location.
	 * @param destinationFile     The destination file.
	 * @param taskId              The data object download task ID.
	 * @param downloadRequestURL  A URL to use to download the data object.
	 * @param sourceSize          The size of the file.
	 * @return A download response DTO object
	 */
	private HpcDataObjectDownloadResponseDTO toDownloadResponseDTO(HpcFileLocation destinationLocation,
			File destinationFile, String taskId, String downloadRequestURL, Long sourceSize) {
		// Construct and return a DTO
		HpcDataObjectDownloadResponseDTO downloadResponse = new HpcDataObjectDownloadResponseDTO();
		downloadResponse.setDestinationFile(destinationFile);
		downloadResponse.setDestinationLocation(destinationLocation);
		downloadResponse.setTaskId(taskId);
		downloadResponse.setDownloadRequestURL(downloadRequestURL);
		downloadResponse.setSize(sourceSize);

		return downloadResponse;
	}

	/**
	 * Construct a download response DTO object.
	 *
	 * @param destinationLocation The destination file location.
	 * @param destinationFile     The destination file.
	 * @param taskId              The data object download task ID.
	 * @param downloadRequestURL  A URL to use to download the data object.
	 * @param restoreInProgress   The flag to indicate if restoration is in
	 *                            progress.
	 * @param sourceSize          The size of the file.
	 * @return A download response DTO object
	 */
	private HpcDataObjectDownloadResponseDTO toDownloadResponseDTO(HpcFileLocation destinationLocation,
			File destinationFile, String taskId, String downloadRequestURL, Boolean restoreInProgress,
			Long sourceSize) {
		// Construct and return a DTO
		HpcDataObjectDownloadResponseDTO downloadResponse = new HpcDataObjectDownloadResponseDTO();
		downloadResponse.setDestinationFile(destinationFile);
		downloadResponse.setDestinationLocation(destinationLocation);
		downloadResponse.setTaskId(taskId);
		downloadResponse.setDownloadRequestURL(downloadRequestURL);
		downloadResponse.setRestoreInProgress(restoreInProgress);
		downloadResponse.setSize(sourceSize);

		return downloadResponse;
	}

	/**
	 * Create parent collections.
	 *
	 * @param path                                 The data object's path.
	 * @param createParentCollections              The indicator whether to create
	 *                                             parent collections.
	 * @param parentCollectionsBulkMetadataEntries The parent collections bulk
	 *                                             metadata entries.
	 * @param userId                               The registrar user-id.
	 * @param userName                             The registrar name.
	 * @param configurationId                      The data management configuration
	 *                                             ID.
	 * @throws HpcException on service failure.
	 */
	private void createParentCollections(String path, Boolean createParentCollections,
			HpcBulkMetadataEntries parentCollectionsBulkMetadataEntries, String userId, String userName,
			String configurationId) throws HpcException {
		// Create parent collections if requested and needed to.
		if (createParentCollections != null && createParentCollections) {
			String parentCollectionPath = path.substring(0, path.lastIndexOf('/'));
			if (!dataManagementService.isPathParentDirectory(path) || parentCollectionMetadataEntriesExist(
					parentCollectionPath, parentCollectionsBulkMetadataEntries)) {
				// If parent collection does not exist, or parent collection
				// exists, but parent collection metadata is supplied
				// Create a parent collection registration request DTO.
				HpcCollectionRegistrationDTO collectionRegistration = new HpcCollectionRegistrationDTO();
				collectionRegistration.getMetadataEntries().addAll(
						getParentCollectionMetadataEntries(parentCollectionPath, parentCollectionsBulkMetadataEntries));
				collectionRegistration.setParentCollectionsBulkMetadataEntries(parentCollectionsBulkMetadataEntries);
				collectionRegistration.setCreateParentCollections(true);

				// Register the parent collection.
				registerCollection(parentCollectionPath, collectionRegistration, userId, userName, configurationId);
			}
		}
	}

	/**
	 * Perform user permission requests on an entity (collection or data object)
	 *
	 * @param path                   The entity path (collection or data object).
	 * @param collection             True if the path is a collection, False if the
	 *                               path is a data object.
	 * @param userPermissionRequests The list of user permissions requests.
	 * @return A list of responses to the permission requests.
	 */
	private List<HpcUserPermissionResponseDTO> setEntityPermissionForUsers(String path, boolean collection,
			List<HpcUserPermission> userPermissionRequests) {
		List<HpcUserPermissionResponseDTO> permissionResponses = new ArrayList<>();

		// Execute all user permission requests for this entity.
		HpcSubjectPermission subjectPermissionRequest = new HpcSubjectPermission();
		subjectPermissionRequest.setSubjectType(HpcSubjectType.USER);
		for (HpcUserPermission userPermissionRequest : userPermissionRequests) {
			HpcUserPermissionResponseDTO userPermissionResponse = new HpcUserPermissionResponseDTO();
			userPermissionResponse.setUserId(userPermissionRequest.getUserId());
			userPermissionResponse.setResult(true);
			subjectPermissionRequest.setPermission(userPermissionRequest.getPermission());
			subjectPermissionRequest.setSubject(userPermissionRequest.getUserId());
			try {
				// Set the entity permission.
				if (collection) {
					dataManagementService.setCollectionPermission(path, subjectPermissionRequest);
				} else {
					dataManagementService.setDataObjectPermission(path, subjectPermissionRequest);
				}

			} catch (HpcException e) {
				// Request failed. Record the message and keep going.
				userPermissionResponse.setResult(false);
				userPermissionResponse.setMessage(e.getMessage());
			}

			// Add this user permission response to the list.
			permissionResponses.add(userPermissionResponse);
		}

		return permissionResponses;
	}

	/**
	 * Recursively delete all the data objects from the specified collection and
	 * from it's sub-collections.
	 *
	 * @param path  The path at the root of the hierarchy to delete from.
	 * @param force If true, perform hard delete. return The total size of the data
	 *              objects.
	 * @throws HpcException if it failed to delete any object in this collection.
	 */
	private long deleteDataObjectsInCollections(String path, Boolean force) throws HpcException {
		HpcCollectionDTO collectionDto = getCollectionChildren(path);
		long totalSize = 0;

		if (collectionDto.getCollection() != null) {
			List<HpcCollectionListingEntry> dataObjects = collectionDto.getCollection().getDataObjects();
			if (!CollectionUtils.isEmpty(dataObjects)) {
				// Delete data objects in this collection
				for (HpcCollectionListingEntry entry : dataObjects) {
					totalSize += Optional.ofNullable(deleteDataObject(entry.getPath(), force, null).getSize())
							.orElse(0L);
				}
			}

			List<HpcCollectionListingEntry> subCollections = collectionDto.getCollection().getSubCollections();
			if (!CollectionUtils.isEmpty(subCollections)) {
				// Recursively delete data objects from this sub-collection and
				// it's sub-collections
				for (HpcCollectionListingEntry entry : subCollections) {
					totalSize += deleteDataObjectsInCollections(entry.getPath(), force);
				}
			}
		}

		return totalSize;
	}

	/**
	 * Perform group permission requests on an entity (collection or data object)
	 *
	 * @param path                    The entity path (collection or data object).
	 * @param collection              True if the path is a collection, False if the
	 *                                path is a data object.
	 * @param groupPermissionRequests The list of group permissions requests.
	 * @return A list of responses to the permission requests.
	 */
	private List<HpcGroupPermissionResponseDTO> setEntityPermissionForGroups(String path, boolean collection,
			List<HpcGroupPermission> groupPermissionRequests) {
		List<HpcGroupPermissionResponseDTO> permissionResponses = new ArrayList<>();

		// Execute all user permission requests for this entity.
		HpcSubjectPermission subjectPermissionRequest = new HpcSubjectPermission();
		subjectPermissionRequest.setSubjectType(HpcSubjectType.GROUP);
		for (HpcGroupPermission groupPermissionRequest : groupPermissionRequests) {
			HpcGroupPermissionResponseDTO groupPermissionResponse = new HpcGroupPermissionResponseDTO();
			groupPermissionResponse.setGroupName(groupPermissionRequest.getGroupName());
			groupPermissionResponse.setResult(true);
			subjectPermissionRequest.setPermission(groupPermissionRequest.getPermission());
			subjectPermissionRequest.setSubject(groupPermissionRequest.getGroupName());
			try {
				// Set the entity permission.
				if (collection) {
					dataManagementService.setCollectionPermission(path, subjectPermissionRequest);
				} else {
					dataManagementService.setDataObjectPermission(path, subjectPermissionRequest);
				}

			} catch (HpcException e) {
				// Request failed. Record the message and keep going.
				groupPermissionResponse.setResult(false);
				groupPermissionResponse.setMessage(e.getMessage());
			}

			// Add this user permission response to the list.
			permissionResponses.add(groupPermissionResponse);
		}

		return permissionResponses;
	}

	/**
	 * Construct entity permissions DTO out of a list of subject permissions.
	 *
	 * @param subjectPermissions A list of subject permissions.
	 * @param excludeSysAdmins   true if the user is a sys admin.
	 * @return Entity permissions DTO
	 * @throws HpcException on service failure.
	 */
	private HpcEntityPermissionsDTO toEntityPermissionsDTO(List<HpcSubjectPermission> subjectPermissions,
			boolean excludeSysAdmins) throws HpcException {
		if (subjectPermissions == null || subjectPermissions.isEmpty()) {
			return null;
		}

		HpcEntityPermissionsDTO entityPermissions = new HpcEntityPermissionsDTO();
		for (HpcSubjectPermission subjectPermission : subjectPermissions) {
			if (subjectPermission.getSubjectType().equals(HpcSubjectType.USER)) {
				if (!(excludeSysAdmins && (subjectPermission.getSubject()
						.contentEquals(securityService.getSystemAccount(HpcIntegratedSystem.IRODS).getUsername())
						|| subjectPermission.getSubject().contentEquals("rods")))) {
					HpcUserPermission userPermission = new HpcUserPermission();
					userPermission.setPermission(subjectPermission.getPermission());
					userPermission.setUserId(subjectPermission.getSubject());
					entityPermissions.getUserPermissions().add(userPermission);
				}
			} else {
				if (!(excludeSysAdmins && subjectPermission.getSubject().contentEquals("SYSTEM_ADMIN_GROUP"))) {
					HpcGroupPermission groupPermission = new HpcGroupPermission();
					groupPermission.setPermission(subjectPermission.getPermission());
					groupPermission.setGroupName(subjectPermission.getSubject());
					entityPermissions.getGroupPermissions().add(groupPermission);
				}
			}
		}

		return entityPermissions;
	}

	/**
	 * Construct user permission DTO out of subject permission.
	 *
	 * @param subjectPermission A subject permission.
	 * @return user permission DTO.
	 */
	private HpcUserPermissionDTO toUserPermissionDTO(HpcSubjectPermission subjectPermission) {
		if (subjectPermission == null) {
			return null;
		}

		HpcUserPermissionDTO userPermission = new HpcUserPermissionDTO();
		userPermission.setPermission(subjectPermission.getPermission());
		userPermission.setUserId(subjectPermission.getSubject());

		return userPermission;
	}

	/**
	 * Get collection download task status.
	 *
	 * @param taskId   The collection download task ID.
	 * @param taskType COLLECTION or DATA_OBJECT_LIST.
	 * @return A collection download status DTO. Null if the task could not be
	 *         found.
	 * @throws HpcException on service failure.
	 */
	private HpcCollectionDownloadStatusDTO getCollectionDownloadStatus(String taskId, HpcDownloadTaskType taskType)
			throws HpcException {
		// Input validation.
		if (taskId == null) {
			throw new HpcException("Null collection download task ID", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the download task status.
		HpcDownloadTaskStatus taskStatus = dataTransferService.getDownloadTaskStatus(taskId, taskType);
		if (taskStatus == null) {
			return null;
		}
		// Map the task status to DTO.
		HpcCollectionDownloadStatusDTO downloadStatus = new HpcCollectionDownloadStatusDTO();
		downloadStatus.setInProgress(taskStatus.getInProgress());
		if (taskStatus.getInProgress()) {
			logger.info("Transfer of file is in progress: " + taskStatus.getCollectionDownloadTask().getPath());
			// Download in progress. Populate the DTO accordingly.
			if (taskType.equals(HpcDownloadTaskType.COLLECTION)) {
				downloadStatus.setPath(taskStatus.getCollectionDownloadTask().getPath());
			} else if (taskType.equals(HpcDownloadTaskType.DATA_OBJECT_LIST)) {
				downloadStatus.getDataObjectPaths().addAll(taskStatus.getCollectionDownloadTask().getDataObjectPaths());
			}
			downloadStatus.setCreated(taskStatus.getCollectionDownloadTask().getCreated());
			downloadStatus.setTaskStatus(taskStatus.getCollectionDownloadTask().getStatus());
			downloadStatus.setRetryTaskId(taskStatus.getCollectionDownloadTask().getRetryTaskId());
			downloadStatus.setRetryUserId(taskStatus.getCollectionDownloadTask().getRetryUserId());
			if (taskStatus.getCollectionDownloadTask().getS3DownloadDestination() != null) {
				downloadStatus.setDestinationLocation(
						taskStatus.getCollectionDownloadTask().getS3DownloadDestination().getDestinationLocation());
				downloadStatus.setDestinationType(HpcDataTransferType.S_3);
			} else if (taskStatus.getCollectionDownloadTask().getGlobusDownloadDestination() != null) {
				downloadStatus.setDestinationLocation(
						taskStatus.getCollectionDownloadTask().getGlobusDownloadDestination().getDestinationLocation());
				downloadStatus.setDestinationType(HpcDataTransferType.GLOBUS);
			} else if (taskStatus.getCollectionDownloadTask().getGoogleDriveDownloadDestination() != null) {
				downloadStatus.setDestinationLocation(taskStatus.getCollectionDownloadTask()
						.getGoogleDriveDownloadDestination().getDestinationLocation());
				downloadStatus.setDestinationType(HpcDataTransferType.GOOGLE_DRIVE);
			} else if (taskStatus.getCollectionDownloadTask().getAsperaDownloadDestination() != null) {
				downloadStatus.setDestinationLocation(
						taskStatus.getCollectionDownloadTask().getAsperaDownloadDestination().getDestinationLocation());
				downloadStatus.setDestinationType(HpcDataTransferType.ASPERA);
			} else if (taskStatus.getCollectionDownloadTask().getBoxDownloadDestination() != null) {
				downloadStatus.setDestinationLocation(
						taskStatus.getCollectionDownloadTask().getBoxDownloadDestination().getDestinationLocation());
				downloadStatus.setDestinationType(HpcDataTransferType.BOX);
			}

			// Get the status of the individual data object download tasks if the collection
			// status is not yet
			// ACTIVE, because the collection items field does not get populated before that
			List<HpcCollectionDownloadTaskItem> items = taskStatus.getCollectionDownloadTask().getItems();
			if (!HpcCollectionDownloadTaskStatus.ACTIVE.equals(taskStatus.getCollectionDownloadTask().getStatus())
					&& CollectionUtils.isEmpty(items)) {
				logger.info("Retrieving download tasks in collection {} with status {}",
						taskStatus.getCollectionDownloadTask().getPath(),
						taskStatus.getCollectionDownloadTask().getStatus());
				for (HpcDataObjectDownloadTask dataObjectDownloadTask : dataTransferService
						.getDataObjectDownloadTasksByCollectionDownloadTaskId(taskId)) {
					HpcCollectionDownloadTaskItem downloadItem = new HpcCollectionDownloadTaskItem();
					downloadItem.setDataObjectDownloadTaskId(dataObjectDownloadTask.getId());
					downloadItem.setPath(dataObjectDownloadTask.getPath());
					downloadItem.setPercentComplete(dataObjectDownloadTask.getPercentComplete());
					downloadItem.setSize(dataObjectDownloadTask.getSize());
					HpcFileLocation destinationLocation = null;
					if (dataObjectDownloadTask.getGlobusDownloadDestination() != null) {
						destinationLocation = dataObjectDownloadTask.getGlobusDownloadDestination()
								.getDestinationLocation();
					} else if (dataObjectDownloadTask.getS3DownloadDestination() != null) {
						destinationLocation = dataObjectDownloadTask.getS3DownloadDestination()
								.getDestinationLocation();
					} else if (dataObjectDownloadTask.getGoogleDriveDownloadDestination() != null) {
						destinationLocation = dataObjectDownloadTask.getGoogleDriveDownloadDestination()
								.getDestinationLocation();
					} else if (dataObjectDownloadTask.getGoogleCloudStorageDownloadDestination() != null) {
						destinationLocation = dataObjectDownloadTask.getGoogleCloudStorageDownloadDestination()
								.getDestinationLocation();
					}
					downloadItem.setDestinationLocation(destinationLocation);
					if (HpcDataTransferDownloadStatus.RECEIVED.equals(dataObjectDownloadTask.getDataTransferStatus())
							|| HpcDataTransferDownloadStatus.IN_PROGRESS
									.equals(dataObjectDownloadTask.getDataTransferStatus())) {
						downloadItem.setResult(null);
						downloadItem.setRestoreInProgress(dataObjectDownloadTask.getRestoreRequested());
					}
					downloadItem.setStagingInProgress(
							(HpcDataTransferType.GLOBUS.equals(dataObjectDownloadTask.getDestinationType())
									|| HpcDataTransferType.ASPERA.equals(dataObjectDownloadTask.getDestinationType()))
									&& HpcDataTransferType.S_3.equals(dataObjectDownloadTask.getDataTransferType())
											? true
											: null);
					items.add(downloadItem);
				}
			}
			downloadStatus.setPercentComplete(
					calculateCollectionDownloadPercentComplete(taskStatus.getCollectionDownloadTask()));
			populateDownloadItems(downloadStatus, taskStatus.getCollectionDownloadTask().getItems());

		} else {
			// Download completed, canceled or failed. Populate the DTO accordingly.
			if (taskType.equals(HpcDownloadTaskType.COLLECTION)) {
				downloadStatus.setPath(taskStatus.getResult().getPath());
			}
			downloadStatus.setCreated(taskStatus.getResult().getCreated());
			downloadStatus.setDestinationLocation(taskStatus.getResult().getDestinationLocation());
			downloadStatus.setDestinationType(taskStatus.getResult().getDestinationType());
			downloadStatus.setCompleted(taskStatus.getResult().getCompleted());
			downloadStatus.setMessage(taskStatus.getResult().getMessage());
			downloadStatus.setResult(taskStatus.getResult().getResult());
			downloadStatus.setRetryTaskId(taskStatus.getResult().getRetryTaskId());
			downloadStatus.setRetryUserId(taskStatus.getResult().getRetryUserId());
			downloadStatus.setEffectiveTrasnsferSpeed(taskStatus.getResult().getEffectiveTransferSpeed() > 0
					? taskStatus.getResult().getEffectiveTransferSpeed()
					: null);
			populateDownloadItems(downloadStatus, taskStatus.getResult().getItems());
			if (taskType.equals(HpcDownloadTaskType.COLLECTION_LIST)) {
				populateCollectionListResultSummary(downloadStatus, taskStatus.getResult().getCollectionPaths(),
						taskStatus.getResult().getItems());
			}
		}

		return downloadStatus;
	}

	/**
	 * Split the list of download items into completed, failed and in-progress
	 * buckets.
	 *
	 * @param downloadStatus The download status to populate the items into.
	 * @param items          The collection / bulk download items.
	 */
	private void populateDownloadItems(HpcCollectionDownloadStatusDTO downloadStatus,
			List<HpcCollectionDownloadTaskItem> items) {
		for (HpcCollectionDownloadTaskItem item : items) {
			HpcDownloadResult result = item.getResult();
			if (result == null) {
				if (Boolean.TRUE.equals(item.getRestoreInProgress())) {
					downloadStatus.getRestoreInProgressItems().add(item);
				} else if (Optional.ofNullable(item.getStagingInProgress()).orElse(false)) {
					downloadStatus.getStagingInProgressItems().add(item);
				} else {
					downloadStatus.getInProgressItems().add(item);
				}
			} else if (result.equals(HpcDownloadResult.COMPLETED)) {
				item.setPercentComplete(null);
				downloadStatus.getCompletedItems().add(item);
			} else if (result.equals(HpcDownloadResult.CANCELED)) {
				item.setPercentComplete(null);
				downloadStatus.getCanceledItems().add(item);
			} else {
				item.setPercentComplete(null);
				downloadStatus.getFailedItems().add(item);
			}
		}
	}

	/**
	 * Iterate through the download list items of collection list task, and prepare
	 * a summary per each collection.
	 *
	 * @param downloadStatus  The download status to populate the items into.
	 * @param collectionPaths The list of collection paths in this download task.
	 * @param items           The collection / bulk download items.
	 */
	private void populateCollectionListResultSummary(HpcCollectionDownloadStatusDTO downloadStatus,
			List<String> collectionPaths, List<HpcCollectionDownloadTaskItem> items) {
		// Instantiate the collection summary map.
		HashMap<String, HpcCollectionResultSummaryDTO> collectionsResultSummary = new HashMap<>();
		collectionPaths.forEach(path -> {
			HpcCollectionResultSummaryDTO collectionResultSummary = new HpcCollectionResultSummaryDTO();
			collectionResultSummary.setProcessed(false);
			collectionResultSummary.setPath(path);
			collectionsResultSummary.put(path, collectionResultSummary);
		});

		// Iterate through the download items list and update the collection summary
		// accordingly
		for (HpcCollectionDownloadTaskItem item : items) {
			HpcDownloadResult result = item.getResult();
			String path = item.getCollectionPath();
			HpcCollectionResultSummaryDTO collectionResultSummary = collectionsResultSummary.get(path);
			if (result == null || StringUtils.isEmpty(path) || collectionResultSummary == null) {
				// The collection path was not captured for this task, or it's still in
				// progress.
				logger.info("Could not create collections download summary - {}, {}, {}", path, result,
						collectionPaths);
				return;
			}

			collectionResultSummary.setProcessed(true);

			// Update completed/failed/canceled count, and update overall collection result:
			// COMPLETED - all items completed
			// CANCELED - at least 1 item canceled
			// FAILED - at least 1 item failed and no item canceled.
			if (result.equals(HpcDownloadResult.COMPLETED)) {
				collectionResultSummary.setCompletedCount(
						Optional.ofNullable(collectionResultSummary.getCompletedCount()).orElse(0) + 1);
				if (collectionResultSummary.getResult() == null) {
					collectionResultSummary.setResult(HpcDownloadResult.COMPLETED);
				}
			} else if (result.equals(HpcDownloadResult.CANCELED)) {
				collectionResultSummary.setCanceledCount(
						Optional.ofNullable(collectionResultSummary.getCanceledCount()).orElse(0) + 1);
				collectionResultSummary.setResult(HpcDownloadResult.CANCELED);
			} else {
				collectionResultSummary
						.setFailedCount(Optional.ofNullable(collectionResultSummary.getFailedCount()).orElse(0) + 1);
				if (!Optional.ofNullable(collectionResultSummary.getResult()).orElse(HpcDownloadResult.COMPLETED)
						.equals(HpcDownloadResult.CANCELED)) {
					collectionResultSummary.setResult(HpcDownloadResult.FAILED);
				}
			}
		}

		downloadStatus.getCollectionListResultSummary().addAll(collectionsResultSummary.values());
	}

	/**
	 * Delete a data object from the archive.
	 *
	 * @param path                     The data object path.
	 * @param systemGeneratedMetadata  The system generated metadata.
	 * @param dataObjectDeleteResponse The deletion response DTO.
	 */
	private void deleteDataObjectFromArchive(String path, HpcSystemGeneratedMetadata systemGeneratedMetadata,
			HpcDataObjectDeleteResponseDTO dataObjectDeleteResponse) {
		updateDataTransferUploadStatus(path, HpcDataTransferUploadStatus.DELETE_REQUESTED);

		try {
			dataTransferService.deleteDataObject(systemGeneratedMetadata.getArchiveLocation(),
					systemGeneratedMetadata.getDataTransferType(), systemGeneratedMetadata.getConfigurationId(),
					systemGeneratedMetadata.getS3ArchiveConfigurationId());

			dataObjectDeleteResponse.setArchiveDeleteStatus(true);
			updateDataTransferUploadStatus(path, HpcDataTransferUploadStatus.DELETED);

		} catch (HpcException e) {
			logger.error("Failed to delete file from archive", e);
			updateDataTransferUploadStatus(path, HpcDataTransferUploadStatus.DELETE_FAILED);
			dataObjectDeleteResponse.setArchiveDeleteStatus(false);
			dataObjectDeleteResponse.setMessage(e.getMessage());
		}
	}

	/**
	 * Attempt to update data object upload status. No exception thrown is failed.
	 *
	 * @param path               The data object path.
	 * @param dataTransferStatus The data transfer upload system generetaed
	 *                           metadata.
	 */
	private void updateDataTransferUploadStatus(String path, HpcDataTransferUploadStatus dataTransferStatus) {
		try {
			metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null, dataTransferStatus, null,
					null, null, null, null, null, null, null, null);

		} catch (HpcException e) {
			logger.error("Failed to update system metadata: " + path + ". Data transfer status: " + dataTransferStatus,
					HpcErrorType.UNEXPECTED_ERROR, e);
		}
	}

	/**
	 * Calculate MD5 checksum of the file and compare it to the value provided.
	 *
	 * @param file     The file to validate checksum.
	 * @param checksum The checksum value provided by the caller.
	 * @throws HpcException If the calculated checksum doesn't match the provided
	 *                      value.
	 */
	@SuppressWarnings("deprecation")
	private void validateChecksum(File file, String checksum) throws HpcException {
		if (file == null || StringUtils.isEmpty(checksum)) {
			return;
		}

		try {
			if (!checksum.equals(Files.hash(file, Hashing.md5()).toString())) {
				throw new HpcException("Checksum validation failed", HpcErrorType.INVALID_REQUEST_INPUT);
			}

		} catch (IOException e) {
			throw new HpcException("Failed calculate checksum", HpcErrorType.UNEXPECTED_ERROR, e);
		}
	}

	/**
	 * Scan the directories in the registration requests and prepare a list of
	 * individual data object registration items for all the files found.
	 *
	 * @param directoryScanRegistrationItems The directory scan registration items.
	 * @return A list of data object registration requests for all the files found
	 *         after scanning the directories.
	 * @throws HpcException On service failure.
	 */
	private List<HpcDataObjectRegistrationItemDTO> toDataObjectRegistrationItems(
			List<HpcDirectoryScanRegistrationItemDTO> directoryScanRegistrationItems) throws HpcException {
		List<HpcDataObjectRegistrationItemDTO> dataObjectRegistrationItems = new ArrayList<>();
		for (HpcDirectoryScanRegistrationItemDTO directoryScanRegistrationItem : directoryScanRegistrationItems) {
			// Validate and normalize the registration items base path.
			String basePath = toNormalizedPath(directoryScanRegistrationItem.getBasePath());
			if (StringUtils.isEmpty(basePath)) {
				throw new HpcException("Null / Empty base path in directory scan registration request",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			// Validate a scan directory was provided.
			int scanDirectoryCount = 0;
			if (directoryScanRegistrationItem.getGlobusScanDirectory() != null) {
				scanDirectoryCount++;
			}
			if (directoryScanRegistrationItem.getS3ScanDirectory() != null) {
				scanDirectoryCount++;
			}
			if (directoryScanRegistrationItem.getGoogleDriveScanDirectory() != null) {
				scanDirectoryCount++;
			}
			if (directoryScanRegistrationItem.getGoogleCloudStorageScanDirectory() != null) {
				scanDirectoryCount++;
			}
			if (directoryScanRegistrationItem.getFileSystemScanDirectory() != null) {
				scanDirectoryCount++;
			}
			if (directoryScanRegistrationItem.getArchiveScanDirectory() != null) {
				scanDirectoryCount++;
			}
			if (scanDirectoryCount == 0) {
				throw new HpcException("No scan directory provided", HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (scanDirectoryCount > 1) {
				throw new HpcException(
						"Multiple (Globus / S3 / Google Drive / Google Cloud Storage / FileSystem / Archive) scan directory provided",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			// Validate folder map.
			HpcDirectoryScanPathMap pathMap = directoryScanRegistrationItem.getPathMap();
			if (pathMap != null) {
				if (StringUtils.isEmpty(pathMap.getFromPath()) || StringUtils.isEmpty(pathMap.getToPath())) {
					throw new HpcException("Null / Empty from/to folder in directory scan folder map",
							HpcErrorType.INVALID_REQUEST_INPUT);
				}
				if (pathMap.getRegexPathMap() == null) {
					pathMap.setRegexPathMap(false);
				}

				if (!Boolean.TRUE.equals(pathMap.getRegexPathMap())) {
					pathMap.setFromPath(toNormalizedPath(pathMap.getFromPath()));
					if (directoryScanRegistrationItem.getS3ScanDirectory() != null
							|| directoryScanRegistrationItem.getGoogleCloudStorageScanDirectory() != null
							|| directoryScanRegistrationItem.getArchiveScanDirectory() != null) {
						// The 'path' in S3 and Google Cloud Storage(which is really object key) don't
						// start with a '/', so need to remove it after normalization.
						pathMap.setFromPath(pathMap.getFromPath().substring(1));
					}
					pathMap.setToPath(toNormalizedPath(pathMap.getToPath()));
				}
			}

			// Get the configuration ID.
			String configurationId = dataManagementService.findDataManagementConfigurationId(basePath);
			if (StringUtils.isEmpty(configurationId)) {
				throw new HpcException("Can't determine configuration id for path: " + basePath,
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			// Validate the directory to scan exists and accessible.
			HpcPathAttributes pathAttributes = null;
			HpcFileLocation scanDirectoryLocation = null;
			HpcDataTransferType dataTransferType = null;
			HpcS3Account s3Account = null;
			String googleAccessToken = null;

			if (directoryScanRegistrationItem.getGlobusScanDirectory() != null) {
				// It is a request to scan a Globus endpoint.
				dataTransferType = HpcDataTransferType.GLOBUS;
				scanDirectoryLocation = directoryScanRegistrationItem.getGlobusScanDirectory().getDirectoryLocation();
				pathAttributes = dataTransferService.getPathAttributes(HpcDataTransferType.GLOBUS,
						scanDirectoryLocation, false, configurationId, null);
			} else if (directoryScanRegistrationItem.getS3ScanDirectory() != null) {
				// It is a request to scan an S3 directory.
				dataTransferType = HpcDataTransferType.S_3;
				s3Account = directoryScanRegistrationItem.getS3ScanDirectory().getAccount();
				scanDirectoryLocation = directoryScanRegistrationItem.getS3ScanDirectory().getDirectoryLocation();
				pathAttributes = dataTransferService.getPathAttributes(s3Account, scanDirectoryLocation, false);
			} else if (directoryScanRegistrationItem.getGoogleDriveScanDirectory() != null) {
				// It is a request to scan a Google Drive directory.
				dataTransferType = HpcDataTransferType.GOOGLE_DRIVE;
				googleAccessToken = directoryScanRegistrationItem.getGoogleDriveScanDirectory().getAccessToken();
				scanDirectoryLocation = directoryScanRegistrationItem.getGoogleDriveScanDirectory()
						.getDirectoryLocation();
				pathAttributes = dataTransferService.getPathAttributes(dataTransferType, googleAccessToken,
						scanDirectoryLocation, false);
			} else if (directoryScanRegistrationItem.getGoogleCloudStorageScanDirectory() != null) {
				// It is a request to scan a Google Cloud Storage directory.
				dataTransferType = HpcDataTransferType.GOOGLE_CLOUD_STORAGE;
				googleAccessToken = directoryScanRegistrationItem.getGoogleCloudStorageScanDirectory().getAccessToken();
				scanDirectoryLocation = directoryScanRegistrationItem.getGoogleCloudStorageScanDirectory()
						.getDirectoryLocation();
				pathAttributes = dataTransferService.getPathAttributes(dataTransferType, googleAccessToken,
						scanDirectoryLocation, false);
			} else if (directoryScanRegistrationItem.getFileSystemScanDirectory() != null) {
				// It is a request to scan a File System directory (local DME server NAS).
				scanDirectoryLocation = directoryScanRegistrationItem.getFileSystemScanDirectory()
						.getDirectoryLocation();
				pathAttributes = dataTransferService.getPathAttributes(scanDirectoryLocation);
			} else if (directoryScanRegistrationItem.getArchiveScanDirectory() != null) {
				// It is a request to scan an S3 Archive directory. Validate the S3 archive
				// configuration ID is provided.
				if (StringUtils.isEmpty(directoryScanRegistrationItem.getS3ArchiveConfigurationId())) {
					throw new HpcException("S3 Archive configuration ID not provided in scan archive request",
							HpcErrorType.INVALID_REQUEST_INPUT);
				}

				dataTransferType = HpcDataTransferType.S_3;
				scanDirectoryLocation = directoryScanRegistrationItem.getArchiveScanDirectory().getDirectoryLocation();
				try {
					pathAttributes = dataTransferService.getPathAttributes(dataTransferType, scanDirectoryLocation,
							false, configurationId, directoryScanRegistrationItem.getS3ArchiveConfigurationId());
				} catch (HpcException e) {
					throw new HpcException("Failed to access Archive S3 bucket: "
							+ scanDirectoryLocation.getFileContainerId() + " [" + e.getMessage() + "] ",
							HpcErrorType.INVALID_REQUEST_INPUT, e);
				}
			}

			if (pathAttributes == null) {
				throw new HpcException("Failed to get path attributes: " + scanDirectoryLocation.getFileContainerId()
						+ ":" + scanDirectoryLocation.getFileId(), HpcErrorType.UNEXPECTED_ERROR);
			}

			if (!pathAttributes.getExists()) {
				throw new HpcException("Endpoint or path doesn't exist: " + scanDirectoryLocation.getFileContainerId()
						+ ":" + scanDirectoryLocation.getFileId(), HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (!pathAttributes.getIsAccessible()) {
				throw new HpcException("Endpoint is not accessible: " + scanDirectoryLocation.getFileContainerId() + ":"
						+ scanDirectoryLocation.getFileId(), HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (!pathAttributes.getIsDirectory()) {
				throw new HpcException("Endpoint is not a directory: " + scanDirectoryLocation.getFileContainerId()
						+ ":" + scanDirectoryLocation.getFileId(), HpcErrorType.INVALID_REQUEST_INPUT);
			}

			// Default pattern type to SIMPLE if not provided by the caller.
			HpcPatternType patternType = directoryScanRegistrationItem.getPatternType();
			if (patternType == null) {
				patternType = HpcPatternType.SIMPLE;
			}

			final String fileContainerId = scanDirectoryLocation.getFileContainerId();
			final HpcS3Account fs3Account = s3Account;
			final String fgoogleAccessToken = googleAccessToken;
			final HpcDataTransferType fdataTransferType = dataTransferType;
			dataTransferService
					.scanDirectory(dataTransferType, s3Account, googleAccessToken, scanDirectoryLocation,
							configurationId, directoryScanRegistrationItem.getS3ArchiveConfigurationId(),
							directoryScanRegistrationItem.getIncludePatterns(),
							directoryScanRegistrationItem.getExcludePatterns(), patternType)
					.forEach(scanItem -> dataObjectRegistrationItems.add(toDataObjectRegistrationItem(scanItem,
							basePath, fileContainerId, directoryScanRegistrationItem.getCallerObjectId(),
							directoryScanRegistrationItem.getBulkMetadataEntries(), pathMap, fdataTransferType,
							fs3Account, fgoogleAccessToken,
							directoryScanRegistrationItem.getS3ArchiveConfigurationId())));
		}

		return dataObjectRegistrationItems;
	}

	private void checkDataObjectRegistrationDestinationPaths(
			List<HpcDataObjectRegistrationItemDTO> dataObjectRegistrationItems) throws HpcException {

		HpcPathAttributes pathAttributes = null;
		String filename = "";
		String fileContainerId = "";
		String source = "";
		for (HpcDataObjectRegistrationItemDTO singleFile : dataObjectRegistrationItems) {
			HpcStreamingUploadSource singleFileSource = null;
			if (singleFile.getGoogleCloudStorageUploadSource() != null) {
				// It is a request to for a Google Cloud Storage file
				source = "Google Cloud";
				singleFileSource = singleFile.getGoogleCloudStorageUploadSource();
				filename = singleFileSource.getSourceLocation().getFileId();
				fileContainerId = singleFileSource.getSourceLocation().getFileContainerId();
				pathAttributes = dataTransferService.getPathAttributes(HpcDataTransferType.GOOGLE_CLOUD_STORAGE,
						singleFileSource.getAccessToken(), singleFileSource.getSourceLocation(), false);
			} else if (singleFile.getS3UploadSource() != null) {
				// It is a request for a S3 file
				source = "S3";
				singleFileSource = singleFile.getS3UploadSource();
				filename = singleFileSource.getSourceLocation().getFileId();
				fileContainerId = singleFileSource.getSourceLocation().getFileContainerId();
				pathAttributes = dataTransferService.getPathAttributes(singleFileSource.getAccount(),
						singleFileSource.getSourceLocation(), false);
			} else if (singleFile.getGoogleDriveUploadSource() != null) {
				// It is a request for a Google Drive file
				source = "Google Drive";
				singleFileSource = singleFile.getGoogleDriveUploadSource();
				filename = singleFileSource.getSourceLocation().getFileId();
				fileContainerId = singleFileSource.getSourceLocation().getFileContainerId();
				pathAttributes = dataTransferService.getPathAttributes(HpcDataTransferType.GOOGLE_DRIVE,
						singleFileSource.getAccessToken(), singleFileSource.getSourceLocation(), false);
			} else if (singleFile.getGlobusUploadSource() != null) {
				// It is a request for a Globus file
				source = "Globus";
				HpcUploadSource singleGlobusFileSource = singleFile.getGlobusUploadSource();
				String directoryPath = singleFile.getPath().substring(0, singleFile.getPath().lastIndexOf("/"));
				filename = singleGlobusFileSource.getSourceLocation().getFileId();
				fileContainerId = singleGlobusFileSource.getSourceLocation().getFileContainerId();
				// Get the configuration ID.
				String configurationId = dataManagementService.findDataManagementConfigurationId(directoryPath);
				if (StringUtils.isEmpty(configurationId)) {
					throw new HpcException("Can't determine configuration id for path: " + directoryPath,
							HpcErrorType.INVALID_REQUEST_INPUT);
				}
				pathAttributes = dataTransferService.getPathAttributes(HpcDataTransferType.GLOBUS,
						singleGlobusFileSource.getSourceLocation(), false, configurationId, null);
			} else {
				continue;
			}

			if (!pathAttributes.getExists()) {
				throw new HpcException(source + " file does not exist: " + fileContainerId + ":" + filename,
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (!pathAttributes.getIsAccessible()) {
				throw new HpcException(source + " file is not accessible: " + fileContainerId + ":" + filename,
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (pathAttributes.getIsDirectory()) {
				throw new HpcException(
						source + " endpoint is a directory, not a file: " + fileContainerId + ":" + filename,
						HpcErrorType.INVALID_REQUEST_INPUT);

			}
		}
	}

	/**
	 * Create a data object registration DTO out of a directory scan item.
	 *
	 * @param scanItem                 The scan item.
	 * @param basePath                 The base path to register the scan item with.
	 * @param sourceFileContainerId    The container ID containing the scan item.
	 * @param callerObjectId           The caller's object ID.
	 * @param bulkMetadataEntries      metadata entries for this data object
	 *                                 registration and parent collections.
	 * @param pathMap                  Replace 'fromPath' (found in scanned
	 *                                 directory) with 'toPath'.
	 * @param dataTransferType         (Optional) The data transfer type performed
	 *                                 the scan. Null means it's a DME server file
	 *                                 system scan
	 * @param s3Account                (Optional) Provided if this is a registration
	 *                                 item from S3 source, otherwise null.
	 * @param googleAccessToken        (Optional) Provided if this is a registration
	 *                                 item from Google Drive or Google Cloud
	 *                                 Storage source, otherwise null.
	 * @param s3ArchiveConfigurationId (Optional) S3 archive configuration ID.
	 * @return data object registration DTO.
	 */
	private HpcDataObjectRegistrationItemDTO toDataObjectRegistrationItem(HpcDirectoryScanItem scanItem,
			String basePath, String sourceFileContainerId, String callerObjectId,
			HpcBulkMetadataEntries bulkMetadataEntries, HpcDirectoryScanPathMap pathMap,
			HpcDataTransferType dataTransferType, HpcS3Account s3Account, String googleAccessToken,
			String s3ArchiveConfigurationId) {
		// If pathMap provided - use the map to replace scanned path with user provided
		// path (or part of
		// path).
		String scanItemFilePath = scanItem.getFilePath();
		if (pathMap != null) {
			scanItemFilePath = Boolean.TRUE.equals(pathMap.getRegexPathMap())
					? scanItemFilePath.replaceAll(pathMap.getFromPath(), pathMap.getToPath())
					: scanItemFilePath.replace(pathMap.getFromPath(), pathMap.getToPath());
		}

		// Calculate the data object path to register.
		String path = toNormalizedPath(basePath + '/' + scanItemFilePath);

		// Construct the registration DTO.
		HpcDataObjectRegistrationItemDTO dataObjectRegistration = new HpcDataObjectRegistrationItemDTO();
		dataObjectRegistration.setPath(path);
		dataObjectRegistration.setCreateParentCollections(true);
		dataObjectRegistration.setS3ArchiveConfigurationId(s3ArchiveConfigurationId);

		// Set data object metadata entries.
		if (bulkMetadataEntries != null) {
			for (HpcBulkMetadataEntry bulkMetadataEntry : bulkMetadataEntries.getPathsMetadataEntries()) {
				if (path.equals(toNormalizedPath(bulkMetadataEntry.getPath()))) {
					dataObjectRegistration.getDataObjectMetadataEntries()
							.addAll(bulkMetadataEntry.getPathMetadataEntries());
					break;
				}
			}
		}
		if (dataObjectRegistration.getDataObjectMetadataEntries().isEmpty()) {
			// Could not find user provided data object metadata. Use default.
			dataObjectRegistration.getDataObjectMetadataEntries()
					.addAll(metadataService.getDefaultDataObjectMetadataEntries(scanItem));
		}

		// Set the metadata to create the parent collections.
		dataObjectRegistration.setParentCollectionsBulkMetadataEntries(bulkMetadataEntries);

		// Set the data object source to upload from.
		HpcFileLocation source = new HpcFileLocation();
		source.setFileContainerId(sourceFileContainerId);
		source.setFileId(scanItem.getFilePath());
		if (dataTransferType == null) {
			HpcUploadSource fileSystemUploadSource = new HpcUploadSource();
			fileSystemUploadSource.setSourceLocation(source);
			dataObjectRegistration.setFileSystemUploadSource(fileSystemUploadSource);
		} else if (dataTransferType.equals(HpcDataTransferType.S_3)) {
			if (s3Account != null) {
				// It's a registration item w/ upload from S3 source.
				HpcStreamingUploadSource s3UploadSource = new HpcStreamingUploadSource();
				s3UploadSource.setSourceLocation(source);
				s3UploadSource.setAccount(s3Account);
				dataObjectRegistration.setS3UploadSource(s3UploadSource);
			} else {
				// It's a registration item w/ link to data already in the S3 archive.
				HpcUploadSource archiveLinkSource = new HpcUploadSource();
				archiveLinkSource.setSourceLocation(source);
				dataObjectRegistration.setArchiveLinkSource(archiveLinkSource);
			}
		} else if (dataTransferType.equals(HpcDataTransferType.GOOGLE_DRIVE)) {
			HpcStreamingUploadSource googleDriveUploadSource = new HpcStreamingUploadSource();
			googleDriveUploadSource.setSourceLocation(source);
			googleDriveUploadSource.setAccessToken(googleAccessToken);
			dataObjectRegistration.setGoogleDriveUploadSource(googleDriveUploadSource);
		} else if (dataTransferType.equals(HpcDataTransferType.GOOGLE_CLOUD_STORAGE)) {
			HpcStreamingUploadSource googleCloudStorageUploadSource = new HpcStreamingUploadSource();
			googleCloudStorageUploadSource.setSourceLocation(source);
			googleCloudStorageUploadSource.setAccessToken(googleAccessToken);
			dataObjectRegistration.setGoogleCloudStorageUploadSource(googleCloudStorageUploadSource);
		} else {
			HpcUploadSource globusUploadSource = new HpcUploadSource();
			globusUploadSource.setSourceLocation(source);
			dataObjectRegistration.setGlobusUploadSource(globusUploadSource);
		}

		dataObjectRegistration.setCallerObjectId(callerObjectId);

		return dataObjectRegistration;
	}

	/**
	 * Update collection metadata.
	 *
	 * @param path            The data object path.
	 * @param metadataEntries The list of metadata entries to update.
	 * @param userId          The userId updating the data object.
	 * @throws HpcException on service failure.
	 */
	private void updateCollection(String path, List<HpcMetadataEntry> metadataEntries, String userId)
			throws HpcException {
		// Get the metadata for this collection.
		HpcMetadataEntries metadataBefore = metadataService.getCollectionMetadataEntries(path);
		HpcSystemGeneratedMetadata systemGeneratedMetadata = metadataService
				.toSystemGeneratedMetadata(metadataBefore.getSelfMetadataEntries());

		// Update the metadata.
		boolean updated = true;
		String message = null;
		try {
			if (!metadataContained(metadataEntries, metadataBefore.getSelfMetadataEntries())) {
				synchronized (this) {
					metadataService.updateCollectionMetadata(path, metadataEntries,
							systemGeneratedMetadata.getConfigurationId());
				}
			} else {
				logger.info(
						"Collection metadata update skipped - request contains no metadata updates to current state: {}",
						path);
			}

		} catch (HpcException e) {
			// Collection metadata update failed. Capture this in the audit record.
			updated = false;
			message = e.getMessage();
			throw (e);

		} finally {
			// Add an audit record of this update collection attempt.
			dataManagementService.addAuditRecord(path, HpcAuditRequestType.UPDATE_COLLECTION, metadataBefore,
					metadataService.getCollectionMetadataEntries(path), null, updated, null, message, userId, null,
					null);
		}

		addCollectionUpdatedEvent(path, false, false, userId, null, null, null);
	}

	/**
	 * Update data object metadata and optionally re-generate upload request URL.
	 *
	 * @param path                       The data object path.
	 * @param metadataEntries            The list of metadata entries to update.
	 * @param collectionType             The type of collection containing the data
	 *                                   object.
	 * @param generateUploadRequestURL   Indicator whether to re-generate the
	 *                                   request upload URL.
	 * @param uploadParts(Optional)      The number of parts when generating upload
	 *                                   request URL.
	 * @param uploadCompletion(Optional) An indicator whether the user will call an
	 *                                   API to complete the upload.
	 * @param checksum                   The data object checksum provided to check
	 *                                   upload integrity.
	 * @param userId                     The userId updating the data object.
	 * @param callerObjectId             The caller's object ID.
	 * @return HpcDataObjectUploadResponse w/generated URL or multipart upload URLs
	 *         if such generated, otherwise null.
	 * @throws HpcException on service failure.
	 */
	private HpcDataObjectUploadResponse updateDataObject(String path, List<HpcMetadataEntry> metadataEntries,
			String collectionType, boolean generateUploadRequestURL, Integer uploadParts, Boolean uploadCompletion,
			String checksum, String userId, String callerObjectId, boolean editMetadata) throws HpcException {
		// Get the metadata for this data object.
		HpcMetadataEntries metadataBefore = metadataService.getDataObjectMetadataEntries(path, false);
		HpcSystemGeneratedMetadata systemGeneratedMetadata = metadataService
				.toSystemGeneratedMetadata(metadataBefore.getSelfMetadataEntries());

		if (systemGeneratedMetadata.getDataTransferStatus().equals(HpcDataTransferUploadStatus.ARCHIVED) && !editMetadata) {
			throw new HpcException(
					"Data object at " + path + " already archived and edit metadata is set to false.",
					HpcErrorType.REQUEST_REJECTED).withSuppressStackTraceLogging(true);
		}
		
		// Update the metadata.
		boolean updated = true;
		String message = null;
		try {
			metadataService.updateDataObjectMetadata(path, metadataEntries,
					systemGeneratedMetadata.getConfigurationId(), collectionType, false);

		} catch (HpcException e) {
			// Data object metadata update failed. Capture this in the audit record.
			updated = false;
			message = e.getMessage();
			throw (e);

		} finally {
			// Add an audit record of this deletion attempt.
			dataManagementService.addAuditRecord(path, HpcAuditRequestType.UPDATE_DATA_OBJECT, metadataBefore,
					metadataService.getDataObjectMetadataEntries(path, false),
					systemGeneratedMetadata.getArchiveLocation(), updated, null, message, null, null, null);
		}

		// Optionally re-generate the upload request URL.
		if (generateUploadRequestURL) {
			// Validate the data is not archived yet.
			HpcSystemGeneratedMetadata metadata = metadataService.getDataObjectSystemGeneratedMetadata(path);
			if (metadata.getDataTransferStatus().equals(HpcDataTransferUploadStatus.ARCHIVED)) {
				throw new HpcException(
						"Upload URL re-generation not allowed. Data object at " + path + " already archived",
						HpcErrorType.REQUEST_REJECTED).withSuppressStackTraceLogging(true);
			}

			// Re-generate the upload request URL.
			HpcDataObjectUploadResponse uploadResponse = dataTransferService.uploadDataObject(null, null, null, null,
					null, null, true, uploadParts, uploadCompletion, checksum, path,
					systemGeneratedMetadata.getObjectId(), userId, callerObjectId,
					systemGeneratedMetadata.getConfigurationId(),
					systemGeneratedMetadata.getS3ArchiveConfigurationId());

			// Update data-transfer-status system metadata accordingly.
			metadataService.updateDataObjectSystemGeneratedMetadata(path, uploadResponse.getArchiveLocation(), null,
					null, HpcDataTransferUploadStatus.URL_GENERATED, null, uploadResponse.getDataTransferStarted(),
					null, null, null, null, null, null, uploadResponse.getDataTransferMethod());

			return uploadResponse;
		}

		return null;
	}

	/**
	 * Convert a bulk registration task DTO from a registration task domain object.
	 *
	 * @param task      bulk registration task domain object to convert The data
	 *                  object path.
	 * @param addUserId flag to populate userId
	 * @return a bulk registration task DTO.
	 */
	private HpcBulkDataObjectRegistrationTaskDTO toBulkDataObjectRegistrationTaskDTO(
			HpcBulkDataObjectRegistrationTask task, boolean addUserId) {
		HpcBulkDataObjectRegistrationTaskDTO taskDTO = new HpcBulkDataObjectRegistrationTaskDTO();
		if (addUserId)
			taskDTO.setUserId(task.getUserId());
		taskDTO.setTaskId(task.getId());
		taskDTO.setCreated(task.getCreated());
		taskDTO.setTaskStatus(task.getStatus());
		taskDTO.setPercentComplete(calculateDataObjectBulkRegistrationPercentComplete(task));
		taskDTO.setUploadMethod(task.getUploadMethod());
		populateRegistrationItems(taskDTO, task.getItems());
		return taskDTO;
	}

	/**
	 * Return a bulk registration task DTO from a registration result domain object.
	 *
	 * @param result    bulk registration result domain object to convert to DTO.
	 * @param addUserId flag to populate userId
	 * @return a bulk registration task DTO.
	 */
	private HpcBulkDataObjectRegistrationTaskDTO toBulkDataObjectRegistrationTaskDTO(
			HpcBulkDataObjectRegistrationResult result, boolean addUserId) {
		HpcBulkDataObjectRegistrationTaskDTO taskDTO = new HpcBulkDataObjectRegistrationTaskDTO();
		if (addUserId)
			taskDTO.setUserId(result.getUserId());
		taskDTO.setTaskId(result.getId());
		taskDTO.setCreated(result.getCreated());
		taskDTO.setCompleted(result.getCompleted());
		taskDTO.setMessage(result.getMessage());
		taskDTO.setResult(result.getResult());
		Integer effectiveTransferSpeed = result.getEffectiveTransferSpeed();
		taskDTO.setEffectiveTransferSpeed(
				effectiveTransferSpeed != null && effectiveTransferSpeed > 0 ? effectiveTransferSpeed : null);
		taskDTO.setUploadMethod(result.getUploadMethod());
		populateRegistrationItems(taskDTO, result.getItems());
		return taskDTO;
	}

	/**
	 * Split the list of registration items into completed, failed and in-progress
	 * buckets.
	 *
	 * @param taskDTO The registration task DTO to populate the items into.
	 * @param items   The registration items.
	 */
	private void populateRegistrationItems(HpcBulkDataObjectRegistrationTaskDTO taskDTO,
			List<HpcBulkDataObjectRegistrationItem> items) {
		for (HpcBulkDataObjectRegistrationItem item : items) {
			Boolean result = item.getTask().getResult();
			item.getTask().setSize(null);
			if (result == null) {
				taskDTO.getInProgressItems().add(item.getTask());
			} else if (result) {
				item.getTask().setPercentComplete(null);
				taskDTO.getCompletedItems().add(item.getTask());
			} else {
				item.getTask().setPercentComplete(null);
				taskDTO.getFailedItems().add(item.getTask());
				taskDTO.getFailedItemsRequest()
						.add(dataObjectRegistrationRequestToDTO(item.getRequest(), item.getTask().getPath()));
			}
		}
	}

	/**
	 * Validate a download request.
	 *
	 * @param path                                 The data object path.
	 * @param downloadDestinationFromS3ArchiveOnly True if the download destination
	 *                                             supports download from S3
	 *                                             archives only (Google Drive,
	 *                                             Google Cloud Storage, Aspera,
	 *                                             Box)
	 * @param generateDownloadURL                  True if this is a request to
	 *                                             generate a download URL.
	 * @return The system generated metadata
	 * @throws HpcException If the request is invalid.
	 */
	private HpcSystemGeneratedMetadata validateDataObjectDownloadRequest(String path,
			boolean downloadDestinationFromS3ArchiveOnly, boolean generateDownloadURL) throws HpcException {

		// Input validation.
		if (StringUtils.isEmpty(path)) {
			throw new HpcException("Null / Empty path for download", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the System generated metadata.
		HpcSystemGeneratedMetadata metadata = metadataService.getDataObjectSystemGeneratedMetadata(path);

		// If this is a link, we will used the link source system-generated-metadata.
		if (metadata.getLinkSourcePath() != null) {
			return validateDataObjectDownloadRequest(metadata.getLinkSourcePath(), downloadDestinationFromS3ArchiveOnly,
					generateDownloadURL);
		}

		if (metadata.getS3ArchiveConfigurationId() == null) {
			logger.error("Could not locate data object: {}", path);
			throw new HpcException("Could not locate data object path " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the file is stored in S3 archive if the download destination
		// supports downloading from S3 only.
		if (downloadDestinationFromS3ArchiveOnly && (metadata.getDataTransferType() == null
				|| !metadata.getDataTransferType().equals(HpcDataTransferType.S_3))) {
			throw new HpcException("Download destination is not supported for POSIX based file system archive",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Generate download URL is supported only from S3 archive.
		if (generateDownloadURL && (metadata.getDataTransferType() == null
				|| !metadata.getDataTransferType().equals(HpcDataTransferType.S_3))) {
			throw new HpcException("Download URL request is not supported for POSIX based file system archive",
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
	 * Calculate the overall % complete of a collection download task
	 *
	 * @param downloadTask The collection download task. return The % complete.
	 * @return The overall % complete of the collection download task.
	 */
	private int calculateCollectionDownloadPercentComplete(HpcCollectionDownloadTask downloadTask) {
		long totalDownloadSize = 0;
		long totalEstimatedDownloadSize = 0;
		long totalBytesTransferred = 0;

		// Sum the total download size and bytes transferred from the task's items.
		for (HpcCollectionDownloadTaskItem item : downloadTask.getItems()) {
			totalDownloadSize += item.getSize() != null ? item.getSize() : 0;
			totalBytesTransferred += item.getPercentComplete() != null && item.getSize() != null
					? ((double) item.getPercentComplete() / 100) * item.getSize()
					: 0;
		}

		// Create a logging prefix.
		StringBuffer logPrefix = new StringBuffer("Bulk download task: [taskId={}] - Bytes transferred for ");
		String logPrefixValue = null;
		switch (downloadTask.getType()) {
		case COLLECTION:
			logPrefix.append("collection {}");
			logPrefixValue = downloadTask.getPath();
			break;

		case COLLECTION_LIST:
			logPrefix.append("collections {}");
			logPrefixValue = StringUtils.join(downloadTask.getCollectionPaths(), ',');
			break;

		case DATA_OBJECT_LIST:
			logPrefix.append("data-objects {}");
			logPrefixValue = StringUtils.join(downloadTask.getDataObjectPaths(), ',');
			break;

		default:
			break;
		}

		// Get the estimated total download size for the collection from the reports.
		if (!downloadTask.getStatus().equals(HpcCollectionDownloadTaskStatus.ACTIVE)) {
			// The bulk download task is not active yet, i.e. still broken down to file
			// download tasks.

			// // Get estimated total size of the download to calculate percent completion
			switch (downloadTask.getType()) {
			case COLLECTION:
				totalEstimatedDownloadSize = getEstimatedCollectionSize(downloadTask.getPath());
				break;

			case COLLECTION_LIST:
				for (String path : downloadTask.getCollectionPaths()) {
					totalEstimatedDownloadSize += getEstimatedCollectionSize(path);
				}
				break;

			default:
				break;
			}

			// Add the the bytes transferred of completed files.
			totalBytesTransferred += Optional.ofNullable(downloadTask.getTotalBytesTransferred()).orElse(0L);

			logger.info(logPrefix
					+ " is {}, total size = {}, total estimated size = {}, bytes transferred of completed files = {}",
					downloadTask.getId(), logPrefixValue, totalBytesTransferred, totalDownloadSize,
					totalEstimatedDownloadSize, downloadTask.getTotalBytesTransferred());
		} else {
			logger.info(logPrefix + " is {}, total size = {}", downloadTask.getId(), logPrefixValue,
					totalBytesTransferred, totalDownloadSize);
		}

		// Use the estimated download size while the collection is being broken down.
		if (totalDownloadSize < totalEstimatedDownloadSize) {
			totalDownloadSize = totalEstimatedDownloadSize;
		}

		if (totalDownloadSize > 0 && totalBytesTransferred <= totalDownloadSize) {
			float percentComplete = (float) 100 * totalBytesTransferred / totalDownloadSize;
			int percent = Math.round(percentComplete);
			logger.info("Percent complete for collection {} is {}", downloadTask.getPath(), percent);
			return Math.round(percentComplete);
		}

		return 0;
	}

	/**
	 * Get an estimated collection size from the report. This is estimated because
	 * links (sub-collection or data-objects) are not included.
	 *
	 * @param path the collection path.
	 * @return The estimated size if the report was available, or 0 otherwise.
	 */
	private long getEstimatedCollectionSize(String path) {
		long estimatedSize = 0;
		try {
			HpcReport report = getTotalSizeReport(path, true);
			if (report != null && report.getReportEntries().size() == 1) {
				estimatedSize = Long.valueOf(report.getReportEntries().get(0).getValue());
			}

		} catch (HpcException e) {
			logger.error("Failed to get size report for collection {}", path, e);
		}

		return estimatedSize;
	}

	/**
	 * Calculate the overall % complete of a bulk data object registration task.
	 *
	 * @param task The bulk registration task.
	 * @return The % complete of the bulk registration task..
	 */
	private int calculateDataObjectBulkRegistrationPercentComplete(HpcBulkDataObjectRegistrationTask task) {

		long totalUploadSize = 0;
		long totalBytesTransferred = 0;
		for (HpcBulkDataObjectRegistrationItem item : task.getItems()) {
			totalUploadSize += item.getTask().getSize() != null ? item.getTask().getSize() : 0;
			totalBytesTransferred += Optional.ofNullable(item.getTask().getPercentComplete()).orElse(0) > 0
					? ((double) item.getTask().getPercentComplete() / 100) * item.getTask().getSize()
					: 0;
		}

		if (totalUploadSize > 0 && totalBytesTransferred <= totalUploadSize) {
			float percentComplete = (float) 100 * totalBytesTransferred / totalUploadSize;
			return Math.round(percentComplete);
		}

		return 0;
	}

	/**
	 * Get metadata entries for a parent collection registration. The metadata
	 * entries are searched in the following manner: 1. Metadata entries found for
	 * the specific path in the bulk metadata entries. 2. The default metadata
	 * entries if provided in the bulk metadata entries. 3. 'System' default
	 * metadata entries.
	 *
	 * @param parentCollectionPath The parent collection path to provide metadata
	 *                             entries for.
	 * @param bulkMetadataEntries  Bulk metadata entries to search in.
	 * @return Metadata entries for the parent collection path
	 */
	private List<HpcMetadataEntry> getParentCollectionMetadataEntries(String parentCollectionPath,
			HpcBulkMetadataEntries bulkMetadataEntries) {
		if (bulkMetadataEntries != null) {
			// Search for the parent collection metadata entries by path.
			for (HpcBulkMetadataEntry bulkMetadataEntry : bulkMetadataEntries.getPathsMetadataEntries()) {
				if (parentCollectionPath.equals(toNormalizedPath(bulkMetadataEntry.getPath()))) {
					return bulkMetadataEntry.getPathMetadataEntries();
				}
			}

			// Not found by path. Return default if provided.
			if (!bulkMetadataEntries.getDefaultCollectionMetadataEntries().isEmpty()) {
				return bulkMetadataEntries.getDefaultCollectionMetadataEntries();
			}
		}

		// Return 'system' default.
		return metadataService.getDefaultCollectionMetadataEntries();
	}

	/**
	 * Check if specific path in the bulk metadata entries is exists.
	 *
	 * @param parentCollectionPath The parent collection path to provide metadata
	 *                             entries for.
	 * @param bulkMetadataEntries  Bulk metadata entries to search in.
	 * @return true if the path exists in the bulk metadata entries.
	 */
	private boolean parentCollectionMetadataEntriesExist(String parentCollectionPath,
			HpcBulkMetadataEntries bulkMetadataEntries) {
		if (bulkMetadataEntries != null) {
			// Search for the parent collection metadata entries by path.
			for (HpcBulkMetadataEntry bulkMetadataEntry : bulkMetadataEntries.getPathsMetadataEntries()) {
				if (parentCollectionPath.equals(toNormalizedPath(bulkMetadataEntry.getPath()))) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Validate a path. 1. Not null or empty string. 2. Contains no characters that
	 * are invalid (i.e. not acceptable by IRODS).
	 *
	 * @param path The path to validate.
	 * @throws HpcException if the path is invalid.
	 */
	private void validatePath(String path) throws HpcException {
		if (StringUtils.isEmpty(path)) {
			throw new HpcException("Null or empty path", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		for (String invalidCharacter : INVALID_PATH_CHARACTERS) {
			if (path.contains(invalidCharacter)) {
				throw new HpcException("Invalid character [" + invalidCharacter + "] in path [" + path + "]",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}
	}

	/**
	 * Validate all paths in bulk registration request.
	 *
	 * @param dataObjectRegistrationItems The registration items.
	 * @throws HpcException if the any path is invalid.
	 */
	private void validateDataObjectRegistrationDestinationPaths(
			List<HpcDataObjectRegistrationItemDTO> dataObjectRegistrationItems) throws HpcException {
		for (HpcDataObjectRegistrationItemDTO registrationItem : dataObjectRegistrationItems) {
			validatePath(registrationItem.getPath());
		}
	}

	/**
	 * Convert HpcDataObjectRegistrationRequest to HpcDataObjectRegistrationItemDTO.
	 *
	 * @param request The registration request.
	 * @param path    The registered path
	 * @return a HpcDataObjectRegistrationItemDTO object
	 */
	private HpcDataObjectRegistrationItemDTO dataObjectRegistrationRequestToDTO(
			HpcDataObjectRegistrationRequest request, String path) {
		HpcDataObjectRegistrationItemDTO dto = new HpcDataObjectRegistrationItemDTO();
		dto.setCallerObjectId(request.getCallerObjectId());
		dto.setCreateParentCollections(request.getCreateParentCollections());
		dto.setPath(path);
		dto.setGlobusUploadSource(request.getGlobusUploadSource());
		dto.setS3UploadSource(request.getS3UploadSource());
		dto.getDataObjectMetadataEntries().addAll(request.getMetadataEntries());
		dto.setParentCollectionsBulkMetadataEntries(request.getParentCollectionsBulkMetadataEntries());
		return dto;
	}

	/**
	 * Link an existing data object to a new path.
	 *
	 * @param path                   The data object's path.
	 * @param dataObjectRegistration A DTO contains the metadata and data transfer
	 *                               locations.
	 * @param dataObjectFile         (Optional) The data object file attachment
	 * @param userId                 The registrar user-id.
	 * @param userName               The registrar name.
	 * @param configurationId        The data management configuration ID.
	 * @param collectionType         The collection type to contain the registered
	 *                               data object.
	 * @throws HpcException on service failure.
	 */
	private void linkDataObject(String path, HpcDataObjectRegistrationRequestDTO dataObjectRegistration,
			File dataObjectFile, String userId, String userName, String configurationId, String collectionType)
			throws HpcException {
		// Input validation

		// Validate that no upload source of any kind provided w/ the link request
		if (dataObjectFile != null || dataObjectRegistration.getGenerateUploadRequestURL() != null
				|| dataObjectRegistration.getGlobusUploadSource() != null
				|| dataObjectRegistration.getS3UploadSource() != null
				|| dataObjectRegistration.getFileSystemUploadSource() != null
				|| dataObjectRegistration.getCallerObjectId() != null) {
			throw new HpcException(
					"S3 / Globus / File System / Generate Upload URL / data attachment provided with link registration request",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the link source data object exist.
		String linkSourcePath = toNormalizedPath(dataObjectRegistration.getLinkSourcePath());
		List<HpcMetadataEntry> linkSourceMetadataEntries = metadataService
				.getDataObjectMetadataEntries(linkSourcePath, false).getSelfMetadataEntries();
		HpcSystemGeneratedMetadata linkSourceSystemGeneratedMetadata = metadataService
				.toSystemGeneratedMetadata(linkSourceMetadataEntries);

		// Validate the link source is not a link itself. Linking to a link is not
		// allowed.
		if (linkSourceSystemGeneratedMetadata.getLinkSourcePath() != null) {
			throw new HpcException("link source can't be a link: " + linkSourcePath,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the link source is archived.
		HpcDataTransferUploadStatus linkSourceDataTransferUploadStatus = linkSourceSystemGeneratedMetadata
				.getDataTransferStatus();
		if (linkSourceDataTransferUploadStatus == null
				|| !linkSourceDataTransferUploadStatus.equals(HpcDataTransferUploadStatus.ARCHIVED)) {
			throw new HpcException("link source is not archived yet: " + linkSourcePath,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Attach user provided metadata. We combine the user provided metadata from the
		// link source w/
		// what
		// the user provided in the registration request.
		Map<String, HpcMetadataEntry> metadataEntries = new HashMap<>();
		metadataService.toUserProvidedMetadataEntries(linkSourceMetadataEntries)
				.forEach(metadataEntry -> metadataEntries.put(metadataEntry.getAttribute(), metadataEntry));
		dataObjectRegistration.getMetadataEntries()
				.forEach(metadataEntry -> metadataEntries.put(metadataEntry.getAttribute(), metadataEntry));
		HpcMetadataEntry dataObjectIdMetadataEntry = metadataService.addMetadataToDataObject(path,
				new ArrayList<HpcMetadataEntry>(metadataEntries.values()), configurationId, collectionType);

		// Generate data management (iRODS) system metadata and attach to the data
		// object.
		metadataService.addSystemGeneratedMetadataToDataObject(path, dataObjectIdMetadataEntry, userId, userName,
				configurationId, linkSourcePath);
	}

	/**
	 * Link an existing data object to a new path.
	 *
	 * @param path                   The collection's path.
	 * @param collectionRegistration A DTO contains the metadata.
	 * @param userId                 The registrar user-id.
	 * @param userName               The registrar name.
	 * @param configurationId        The data management configuration ID.
	 * @throws HpcException on service failure.
	 */
	private void linkCollection(String path, HpcCollectionRegistrationDTO collectionRegistration, String userId,
			String userName, String configurationId) throws HpcException {
		// Input validation

		// Validate the link source data object exist.
		String linkSourcePath = toNormalizedPath(collectionRegistration.getLinkSourcePath());

		List<HpcMetadataEntry> linkSourceMetadataEntries = metadataService.getCollectionMetadataEntries(linkSourcePath)
				.getSelfMetadataEntries();
		HpcSystemGeneratedMetadata linkSourceSystemGeneratedMetadata = metadataService
				.toSystemGeneratedMetadata(linkSourceMetadataEntries);
		if (StringUtils.isEmpty(linkSourceSystemGeneratedMetadata.getObjectId())) {
			throw new HpcException("link source doesn't exist: " + linkSourcePath, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the link source is not a link itself. Linking to a link is not
		// allowed.
		if (linkSourceSystemGeneratedMetadata.getLinkSourcePath() != null) {
			throw new HpcException("link source can't be a link: " + linkSourcePath,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Attach user provided metadata. We combine the user provided metadata from the
		// link source w/
		// what
		// the user provided in the registration request.
		Map<String, HpcMetadataEntry> metadataEntries = new HashMap<>();
		metadataService.toUserProvidedMetadataEntries(linkSourceMetadataEntries)
				.forEach(metadataEntry -> metadataEntries.put(metadataEntry.getAttribute(), metadataEntry));
		collectionRegistration.getMetadataEntries()
				.forEach(metadataEntry -> metadataEntries.put(metadataEntry.getAttribute(), metadataEntry));

		// Add user provided metadata.
		metadataService.addMetadataToCollection(path, new ArrayList<HpcMetadataEntry>(metadataEntries.values()),
				configurationId);

		// Generate system metadata and attach to the collection.
		metadataService.addSystemGeneratedMetadataToCollection(path, userId, userName, configurationId, linkSourcePath);
	}

	/**
	 * Get a destination location from collection download task
	 *
	 * @param collectionDownloadTask The collection download task. return The
	 *                               destination location.
	 * @return a HpcFileLocation instance
	 */
	private HpcFileLocation getDestinationLocation(HpcCollectionDownloadTask collectionDownloadTask) {
		HpcFileLocation destinationLocation = null;
		if (collectionDownloadTask.getS3DownloadDestination() != null) {
			destinationLocation = collectionDownloadTask.getS3DownloadDestination().getDestinationLocation();
		} else if (collectionDownloadTask.getGlobusDownloadDestination() != null) {
			destinationLocation = collectionDownloadTask.getGlobusDownloadDestination().getDestinationLocation();
		} else if (collectionDownloadTask.getGoogleDriveDownloadDestination() != null) {
			destinationLocation = collectionDownloadTask.getGoogleDriveDownloadDestination().getDestinationLocation();
		} else if (collectionDownloadTask.getGoogleCloudStorageDownloadDestination() != null) {
			destinationLocation = collectionDownloadTask.getGoogleCloudStorageDownloadDestination()
					.getDestinationLocation();
		} else if (collectionDownloadTask.getAsperaDownloadDestination() != null) {
			destinationLocation = collectionDownloadTask.getAsperaDownloadDestination().getDestinationLocation();
		} else if (collectionDownloadTask.getBoxDownloadDestination() != null) {
			destinationLocation = collectionDownloadTask.getBoxDownloadDestination().getDestinationLocation();
		}

		return destinationLocation;
	}

	/**
	 * Create a download request from download task result and download retry
	 * request
	 *
	 * @param downloadTaskResult   The download task result.
	 * @param downloadRetryRequest The download retry request.
	 * @return a HpcDownloadRequestDTO instance
	 */
	private HpcDownloadRequestDTO createDownloadRequestDTO(HpcDownloadTaskResult downloadTaskResult,
			HpcDownloadRetryRequestDTO downloadRetryRequest) {
		HpcDownloadRequestDTO downloadRequest = new HpcDownloadRequestDTO();
		if (downloadTaskResult.getDestinationType().equals(HpcDataTransferType.S_3)) {
			HpcS3DownloadDestination s3DownloadDestination = new HpcS3DownloadDestination();
			s3DownloadDestination.setAccount(downloadRetryRequest.getS3Account());
			s3DownloadDestination.setDestinationLocation(downloadTaskResult.getDestinationLocation());
			downloadRequest.setS3DownloadDestination(s3DownloadDestination);
		} else if (downloadTaskResult.getDestinationType().equals(HpcDataTransferType.GLOBUS)) {
			HpcGlobusDownloadDestination globusDownloadDestination = new HpcGlobusDownloadDestination();
			globusDownloadDestination.setDestinationLocation(downloadTaskResult.getDestinationLocation());
			downloadRequest.setGlobusDownloadDestination(globusDownloadDestination);
			downloadRequest.getGlobusDownloadDestination()
					.setDestinationOverwrite(downloadRetryRequest.getDestinationOverwrite());
		} else if (downloadTaskResult.getDestinationType().equals(HpcDataTransferType.GOOGLE_DRIVE)) {
			HpcGoogleDownloadDestination googleDriveDownloadDestination = new HpcGoogleDownloadDestination();
			googleDriveDownloadDestination.setAccessToken(downloadRetryRequest.getGoogleAccessToken());
			googleDriveDownloadDestination.setDestinationLocation(downloadTaskResult.getDestinationLocation());
			downloadRequest.setGoogleDriveDownloadDestination(googleDriveDownloadDestination);
		} else if (downloadTaskResult.getDestinationType().equals(HpcDataTransferType.GOOGLE_CLOUD_STORAGE)) {
			HpcGoogleDownloadDestination googleCloudStorageDownloadDestination = new HpcGoogleDownloadDestination();
			googleCloudStorageDownloadDestination.setAccessToken(downloadRetryRequest.getGoogleAccessToken());
			googleCloudStorageDownloadDestination.setDestinationLocation(downloadTaskResult.getDestinationLocation());
			downloadRequest.setGoogleCloudStorageDownloadDestination(googleCloudStorageDownloadDestination);
		} else if (downloadTaskResult.getDestinationType().equals(HpcDataTransferType.ASPERA)) {
			HpcAsperaDownloadDestination asperaDownloadDestination = new HpcAsperaDownloadDestination();
			asperaDownloadDestination.setAccount(downloadRetryRequest.getAsperaAccount());
			asperaDownloadDestination.setDestinationLocation(downloadTaskResult.getDestinationLocation());
			downloadRequest.setAsperaDownloadDestination(asperaDownloadDestination);
		} else if (downloadTaskResult.getDestinationType().equals(HpcDataTransferType.BOX)) {
			HpcBoxDownloadDestination boxDownloadDestination = new HpcBoxDownloadDestination();
			boxDownloadDestination.setAccessToken(downloadRetryRequest.getBoxAccessToken());
			boxDownloadDestination.setRefreshToken(downloadRetryRequest.getBoxRefreshToken());
			boxDownloadDestination.setDestinationLocation(downloadTaskResult.getDestinationLocation());
			downloadRequest.setBoxDownloadDestination(boxDownloadDestination);
		}

		return downloadRequest;
	}

	/**
	 * Create a failed data object registration result object
	 *
	 * @param path           The data object path
	 * @param userId         The registrar user id.
	 * @param uploadResponse The upload request (to Globus/S3, etc) response
	 * @param message        The error message.
	 * @return a HpcDataObjectRegistrationResult instance
	 */
	private HpcDataObjectRegistrationResult toFailedDataObjectRegistrationResult(String path, String userId,
			HpcDataObjectUploadResponse uploadResponse, String message) {

		HpcDataObjectRegistrationResult dataObjectRegistrationResult = new HpcDataObjectRegistrationResult();
		Calendar now = Calendar.getInstance();
		dataObjectRegistrationResult.setCreated(now);
		dataObjectRegistrationResult.setCompleted(now);
		dataObjectRegistrationResult.setResult(false);
		dataObjectRegistrationResult.setMessage(message);
		dataObjectRegistrationResult.setUserId(userId);
		dataObjectRegistrationResult.setPath(path);
		if (uploadResponse != null) {
			dataObjectRegistrationResult.setDataTransferRequestId(uploadResponse.getDataTransferRequestId());
			dataObjectRegistrationResult.setSourceLocation(uploadResponse.getUploadSource());
		}

		return dataObjectRegistrationResult;
	}

	/**
	 * Validate an archive permissions request.
	 *
	 * @param path                      The data object path
	 * @param archivePermissionsRequest The archive permissions request.
	 * @return The data object system metadata
	 * @throws HpcException if the request is invalid
	 */
	private HpcSystemGeneratedMetadata validateArchivePermissionsRequest(String path,
			HpcArchivePermissionsRequestDTO archivePermissionsRequest) throws HpcException {
		// Input Validation
		if (StringUtils.isEmpty(path) || archivePermissionsRequest == null) {
			throw new HpcException("Null / Empty data object path or request DTO", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the data object archive permissions request.
		boolean setArchivePermissionsFromSource = Optional
				.ofNullable(archivePermissionsRequest.getSetArchivePermissionsFromSource()).orElse(false);
		HpcPathPermissions dataObjectPermissions = archivePermissionsRequest.getDataObjectPermissions();
		if (dataObjectPermissions == null && !setArchivePermissionsFromSource) {
			throw new HpcException("data object permissions not provided and set-from-source indicator is false",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (dataObjectPermissions != null && setArchivePermissionsFromSource) {
			throw new HpcException("data object permissions provided and set-from-source indicator is true",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (dataObjectPermissions != null && (StringUtils.isEmpty(dataObjectPermissions.getOwner())
				|| StringUtils.isEmpty(dataObjectPermissions.getGroup()))) {
			throw new HpcException("data object permissions provided w/o owner or group",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the archive directories permissions request.
		for (HpcArchiveDirectoryPermissionsRequestDTO archiveDirectoryPermissionsRequest : archivePermissionsRequest
				.getDirectoryPermissions()) {
			if (StringUtils.isEmpty(archiveDirectoryPermissionsRequest.getPath())) {
				throw new HpcException("Null / Empty path in archive directory permission request",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			archiveDirectoryPermissionsRequest.setPath(toNormalizedPath(archiveDirectoryPermissionsRequest.getPath()));

			HpcPathPermissions directoryPermissions = archiveDirectoryPermissionsRequest.getPermissions();
			if (StringUtils.isEmpty(directoryPermissions.getOwner())
					|| StringUtils.isEmpty(directoryPermissions.getGroup())) {
				throw new HpcException("Directory permissions provided w/o owner or group for: "
						+ archiveDirectoryPermissionsRequest.getPath(), HpcErrorType.INVALID_REQUEST_INPUT);
			}

			if (!path.startsWith(archiveDirectoryPermissionsRequest.getPath())) {
				throw new HpcException("Directory path is not a parent of the data object:  "
						+ archiveDirectoryPermissionsRequest.getPath(), HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Get the System generated metadata.
		HpcSystemGeneratedMetadata metadata = metadataService.getDataObjectSystemGeneratedMetadata(path);

		if (!StringUtils.isEmpty(metadata.getLinkSourcePath())) {
			throw new HpcException("Archive permission request is not supported for soft-links",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Archive permission request supported only from POSIX archive.
		if (metadata.getDataTransferType() == null
				|| !metadata.getDataTransferType().equals(HpcDataTransferType.GLOBUS)) {
			throw new HpcException("Archive permission request is not supported for S3 archive",
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

		// If we set permissions from source. Validate the permissions on the source are
		// available
		if (setArchivePermissionsFromSource) {
			HpcPathPermissions sourcePermissions = metadata.getSourcePermissions();
			if (sourcePermissions == null || StringUtils.isEmpty(sourcePermissions.getOwner())
					|| StringUtils.isEmpty(sourcePermissions.getGroup())) {
				throw new HpcException(
						"Request to set permissions from source, but no source permissions metadata found",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		return metadata;
	}

	/**
	 * Perform user data management (iRODS) permission request on an entity
	 * (collection or data object)
	 *
	 * @param path       The entity path (collection or data object).
	 * @param collection True if the path is a collection, False if the path is a
	 *                   data object.
	 * @param userId     The user ID.
	 * @param permission The permission
	 * @return A user permission response.
	 */
	private HpcUserPermissionResponseDTO setEntityPermissionForUser(String path, boolean collection, String userId,
			HpcPermission permission) {
		HpcUserPermission userPermission = new HpcUserPermission();
		userPermission.setUserId(userId);
		userPermission.setPermission(permission);
		List<HpcUserPermission> userPermissions = new ArrayList<>();
		userPermissions.add(userPermission);

		return setEntityPermissionForUsers(path, collection, userPermissions).get(0);
	}

	/**
	 * Perform group data management (iRODS) permission request on an entity
	 * (collection or data object)
	 *
	 * @param path       The entity path (collection or data object).
	 * @param collection True if the path is a collection, False if the path is a
	 *                   data object.
	 * @param groupName  The group name.
	 * @param permission The permission
	 * @return A group permission response.
	 */
	private HpcGroupPermissionResponseDTO setEntityPermissionForGroup(String path, boolean collection, String groupName,
			HpcPermission permission) {
		HpcGroupPermission groupPermission = new HpcGroupPermission();
		groupPermission.setGroupName(groupName);
		groupPermission.setPermission(permission);
		List<HpcGroupPermission> groupPermissions = new ArrayList<>();
		groupPermissions.add(groupPermission);

		return setEntityPermissionForGroups(path, collection, groupPermissions).get(0);
	}

	/**
	 * Perform a distinguished name search.
	 *
	 * @param sourceLocation The source location. Used to get the DN user/group
	 *                       search base from configuration.
	 * @param permissions    The owner/group to search for DN and map to NIH LDAP
	 *                       names
	 * @return A permissions object w/ replaced DN if found. If not, same
	 *         permissions object is returned
	 * @throws HpcException on DN search failure
	 */
	private HpcPathPermissions performDistinguishedNameSearch(HpcFileLocation sourceLocation,
			HpcPathPermissions permissions) throws HpcException {
		HpcPathPermissions dnPermissions = new HpcPathPermissions();
		dnPermissions.setUserId(permissions.getUserId());
		dnPermissions.setOwner(permissions.getOwner());
		dnPermissions.setGroupId(permissions.getGroupId());
		dnPermissions.setGroup(permissions.getGroup());
		dnPermissions.setPermissions(permissions.getPermissions());
		dnPermissions.setPermissionsMode(permissions.getPermissionsMode());

		// Perform a DN search and update owner/group if found.
		if (sourceLocation != null) {
			HpcDistinguishedNameSearch distinguishedSearchName = securityService
					.findDistinguishedNameSearch(sourceLocation.getFileId());
			if (distinguishedSearchName != null) {
				HpcDistinguishedNameSearchResult userDistinguishedNameSearchResult = securityService
						.getUserDistinguishedName(permissions.getOwner(), distinguishedSearchName.getUserSearchBase());
				if (userDistinguishedNameSearchResult != null
						&& !StringUtils.isEmpty(userDistinguishedNameSearchResult.getNihCommonName())) {
					dnPermissions.setOwner((userDistinguishedNameSearchResult.getNihCommonName()));
				}

				HpcDistinguishedNameSearchResult groupDistinguishedNameSearchResult = securityService
						.getGroupDistinguishedName(permissions.getGroup(),
								distinguishedSearchName.getGroupSearchBase());
				if (groupDistinguishedNameSearchResult != null
						&& !StringUtils.isEmpty(groupDistinguishedNameSearchResult.getNihCommonName())) {
					dnPermissions.setGroup((groupDistinguishedNameSearchResult.getNihCommonName()));
				}
			}
		}

		return dnPermissions;
	}

	private HpcPermissionForCollection fetchCollectionPermission(String path, String userId) throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (userId == null) {
			throw new HpcException("Null userId", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		// Validate the collection exists.
		if (!dataManagementService.collectionExists(path)) {
			return null;
		}
		HpcSubjectPermission hsPerm = dataManagementService.acquireCollectionPermission(path, userId);
		HpcPermissionForCollection resultHpcPermForColl = new HpcPermissionForCollection();
		resultHpcPermForColl.setCollectionPath(path);
		resultHpcPermForColl.setPermission(hsPerm.getPermission());
		return resultHpcPermForColl;
	}

	/**
	 * Recursively recover all the data objects from the specified collection and
	 * from it's sub-collections.
	 *
	 * @param path The path at the root of the hierarchy to recover from.
	 * @throws HpcException if it failed to recover any object in this collection.
	 */
	private void recoverDataObjectsFromCollections(String path) throws HpcException {

		HpcCollectionDTO collectionDto = getCollectionChildren(path);

		if (collectionDto.getCollection() != null) {
			List<HpcCollectionListingEntry> dataObjects = collectionDto.getCollection().getDataObjects();
			if (!CollectionUtils.isEmpty(dataObjects)) {
				// Delete data objects in this collection
				for (HpcCollectionListingEntry entry : dataObjects) {
					recoverDataObject(entry.getPath());
				}
			}

			List<HpcCollectionListingEntry> subCollections = collectionDto.getCollection().getSubCollections();
			if (!CollectionUtils.isEmpty(subCollections)) {
				// Recursively delete data objects from this sub-collection and
				// it's sub-collections
				for (HpcCollectionListingEntry entry : subCollections) {
					recoverDataObjectsFromCollections(entry.getPath());
				}
			}
		}
	}

	/**
	 * Helper method to log profiling messages in data registration
	 *
	 * @param path The data object path
	 * @param time execution time
	 */
	private void taskProfilingLog(String type, String path, String message, Long time) {
		logger.info("{} Profiling [{}]: {}{}", type, path, message, time != null ? "[" + time.toString() + " ms]" : "");
	}

	/**
	 * Check if a sublist of metadata is fully contained within a list.
	 *
	 * @param sublist sublist of metadata entries
	 * @param list    full list of metadata
	 * @return true if the sublist if fully contained - all metadata attribute and
	 *         value match
	 */
	private boolean metadataContained(List<HpcMetadataEntry> sublist, List<HpcMetadataEntry> list) {
		if ((sublist == null || sublist.size() == 0) && (list == null || list.size() == 0)) {
			return true;
		}

		Map<String, String> metadataMap = metadataService.toMap(list);
		for (HpcMetadataEntry metadataEntryInSublist : sublist) {
			String attribute = metadataEntryInSublist.getAttribute();
			if (attribute == null) {
				attribute = "";
			}
			String value = metadataMap.get(attribute);
			if (value == null || !value.equals(metadataEntryInSublist.getValue())) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Link an Archive source to a a file in iRODs, as part of data registration
	 * process.
	 *
	 * @param path                      The path of the data object (in iRODs) to
	 *                                  link to the archive source.
	 * @param dataObjectRegistration    The data object registration request.
	 * @param configurationId           The data management configuration ID.
	 * @param dataObjectIdMetadataEntry The object-id metadata entry.
	 * @param userId                    The registrar user-id.
	 * @param userName                  The registrar name.
	 * @param registrationEventRequired If set to true, an event will be generated
	 *                                  when registration is completed or failed.
	 * @throws HpcException If failed to link the archive source.
	 */
	private void linkArchiveSource(String path, HpcDataObjectRegistrationRequestDTO dataObjectRegistration,
			String configurationId, HpcMetadataEntry dataObjectIdMetadataEntry, String userId, String userName,
			boolean registrationEventRequired) throws HpcException {
		// Input Validation
		if (dataObjectRegistration.getArchiveLinkSource() == null) {
			throw new HpcException("Null archive link source in registration request: " + path,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (!isValidFileLocation(dataObjectRegistration.getArchiveLinkSource().getSourceLocation())) {
			throw new HpcException("Invalid archive link location in registration request for: " + path,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (StringUtils.isEmpty(dataObjectRegistration.getS3ArchiveConfigurationId())) {
			throw new HpcException("Empty s3ArchiveConfigurationId in registration request for: " + path,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		String s3ArchiveConfigurationFileContainerId = dataManagementService
				.getS3ArchiveConfiguration(dataObjectRegistration.getS3ArchiveConfigurationId())
				.getBaseArchiveDestination().getFileLocation().getFileContainerId();
		if (!dataObjectRegistration.getArchiveLinkSource().getSourceLocation().getFileContainerId()
				.equals(s3ArchiveConfigurationFileContainerId)) {
			throw new HpcException("The archive link source bucket ["
					+ dataObjectRegistration.getArchiveLinkSource().getSourceLocation().getFileContainerId()
					+ "] doesn't match the s3ArchiveConfiguration bucket [" + s3ArchiveConfigurationFileContainerId
					+ "]", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Lookup the archive for this data object.
		HpcPathAttributes archivePathAttributes = dataTransferService.getPathAttributes(HpcDataTransferType.S_3,
				dataObjectRegistration.getArchiveLinkSource().getSourceLocation(), true, configurationId,
				dataObjectRegistration.getS3ArchiveConfigurationId());

		// Validate the file exists in the archive.
		String archiveSource = dataObjectRegistration.getArchiveLinkSource().getSourceLocation().getFileContainerId()
				+ ":" + dataObjectRegistration.getArchiveLinkSource().getSourceLocation().getFileId();
		if (!archivePathAttributes.getExists() || !archivePathAttributes.getIsFile()) {
			throw new HpcException(
					"Archive file [" + archiveSource + "] was not found in registration request: " + path,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// The data object is found in archive. Set system metadata in iRODs.
		Calendar dataTransferCompleted = Calendar.getInstance();
		HpcSystemGeneratedMetadata systemGeneratedMetadata = metadataService.addSystemGeneratedMetadataToDataObject(
				path, dataObjectIdMetadataEntry, dataObjectRegistration.getArchiveLinkSource().getSourceLocation(),
				dataObjectRegistration.getArchiveLinkSource().getSourceLocation(), null,
				HpcDataTransferUploadStatus.ARCHIVED, HpcDataTransferUploadMethod.ARCHIVE_LINK, HpcDataTransferType.S_3,
				dataTransferCompleted, dataTransferCompleted, archivePathAttributes.getSize(), null, null,
				dataObjectRegistration.getCallerObjectId(), userId, userName, configurationId,
				dataObjectRegistration.getS3ArchiveConfigurationId(), registrationEventRequired);

		// Add metadata to the file in the archive.
		HpcAddArchiveObjectMetadataResponse addArchiveObjectMetadataResponse = dataTransferService
				.addSystemGeneratedMetadataToDataObject(systemGeneratedMetadata.getArchiveLocation(),
						systemGeneratedMetadata.getDataTransferType(), systemGeneratedMetadata.getConfigurationId(),
						systemGeneratedMetadata.getS3ArchiveConfigurationId(), systemGeneratedMetadata.getObjectId(),
						systemGeneratedMetadata.getRegistrarId());
		if (!addArchiveObjectMetadataResponse.getMetadataAdded()) {
			throw new HpcException("Archive file [" + archiveSource
					+ "] already linked to another file in registration request: " + path,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Update the data management (iRODS) data object's system-metadata.
		HpcArchiveObjectMetadata objectMetadata = addArchiveObjectMetadataResponse.getArchiveObjectMetadata();
		Calendar deepArchiveDate = objectMetadata.getDeepArchiveStatus() != null
				&& objectMetadata.getDeepArchiveStatus().equals(HpcDeepArchiveStatus.IN_PROGRESS)
						? Calendar.getInstance()
						: null;
		metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, objectMetadata.getChecksum(), null,
				null, null, null, null, null, null, objectMetadata.getDeepArchiveStatus(), deepArchiveDate, null);

		// Add an event if needed.
		if (Boolean.TRUE.equals(systemGeneratedMetadata.getRegistrationEventRequired())) {
			HpcDataManagementConfiguration dataManagementConfiguration = dataManagementService
					.getDataManagementConfiguration(systemGeneratedMetadata.getConfigurationId());
			eventService.addDataTransferUploadArchivedEvent(systemGeneratedMetadata.getRegistrarId(), path,
					systemGeneratedMetadata.getSourceLocation(), dataTransferCompleted, null, null,
					dataManagementConfiguration.getDoc());
		}

		// Record a registration result.
		systemGeneratedMetadata.setDataTransferCompleted(dataTransferCompleted);
		dataManagementService.addDataObjectRegistrationResult(path, systemGeneratedMetadata, true, null);
	}

	/**
	 * Upload a data object, as part of data registration process.
	 *
	 * @param path                      The path of the data object (in iRODs) to
	 *                                  link to the archive source.
	 * @param dataObjectRegistration    The data object registration request.
	 * @param configurationId           The data management configuration ID.
	 * @param dataObjectIdMetadataEntry The object-id metadata entry.
	 * @param userId                    The registrar user-id.
	 * @param userName                  The registrar name.
	 * @param registrationEventRequired If set to true, an event will be generated
	 *                                  when registration is completed or failed.
	 * @param responseDTO               The response DTO to set.
	 * @param dataObjectFile            (Optional) The data object file attachment.
	 * @param generateUploadRequestURL  An indicator if the upload is done by
	 *                                  requesting a pre-signed URL.
	 * @param extractMetadata           Indicator whether metadata extraction is
	 *                                  requested.
	 * @param collectionType            The type of collection containing the data
	 *                                  object.
	 * @return upload request response object
	 * @throws HpcException If failed to link the archive source.
	 */
	private HpcDataObjectUploadResponse uploadDataObject(String path,
			HpcDataObjectRegistrationRequestDTO dataObjectRegistration, String configurationId,
			HpcMetadataEntry dataObjectIdMetadataEntry, String userId, String userName,
			boolean registrationEventRequired, HpcDataObjectRegistrationResponseDTO responseDTO, File dataObjectFile,
			boolean generateUploadRequestURL, boolean extractMetadata, String collectionType) throws HpcException {

		// This is a registration request w/ data upload.

		// Use default S3 archive configuration ID if not provided.
		String s3ArchiveConfigurationId = !StringUtils.isEmpty(dataObjectRegistration.getS3ArchiveConfigurationId())
				? dataObjectRegistration.getS3ArchiveConfigurationId()
				: dataManagementService.getDataManagementConfiguration(configurationId).getS3UploadConfigurationId();

		// This is a registration request w/ data upload. Transfer the data file.
		long timeBefore = System.currentTimeMillis();
		HpcDataObjectUploadResponse uploadResponse = dataTransferService.uploadDataObject(
				dataObjectRegistration.getGlobusUploadSource(), dataObjectRegistration.getS3UploadSource(),
				dataObjectRegistration.getGoogleDriveUploadSource(),
				dataObjectRegistration.getGoogleCloudStorageUploadSource(),
				dataObjectRegistration.getFileSystemUploadSource(), dataObjectFile, generateUploadRequestURL,
				dataObjectRegistration.getUploadParts(), dataObjectRegistration.getUploadCompletion(),
				generateUploadRequestURL ? dataObjectRegistration.getChecksum() : null, path,
				dataObjectIdMetadataEntry.getValue(), userId, dataObjectRegistration.getCallerObjectId(),
				configurationId, s3ArchiveConfigurationId);
		taskProfilingLog("Registration", path, "Upload request / URL generation completed",
				System.currentTimeMillis() - timeBefore);

		// Set the upload request URL / Multipart upload URLs (if one was generated).
		responseDTO.setUploadRequestURL(uploadResponse.getUploadRequestURL());
		responseDTO.setMultipartUpload(uploadResponse.getMultipartUpload());

		// Generate data management (iRODS) system metadata and attach to the data
		// object.
		timeBefore = System.currentTimeMillis();
		HpcSystemGeneratedMetadata systemGeneratedMetadata = metadataService.addSystemGeneratedMetadataToDataObject(
				path, dataObjectIdMetadataEntry, uploadResponse.getArchiveLocation(), uploadResponse.getUploadSource(),
				uploadResponse.getDataTransferRequestId(), uploadResponse.getDataTransferStatus(),
				uploadResponse.getDataTransferMethod(), uploadResponse.getDataTransferType(),
				uploadResponse.getDataTransferStarted(), uploadResponse.getDataTransferCompleted(),
				uploadResponse.getSourceSize(), uploadResponse.getSourceURL(), uploadResponse.getSourcePermissions(),
				dataObjectRegistration.getCallerObjectId(), userId, userName, configurationId, s3ArchiveConfigurationId,
				registrationEventRequired);
		taskProfilingLog("Registration", path, "System metadata set in iRODS", System.currentTimeMillis() - timeBefore);

		// Generate S3 archive system generated metadata. Note: This is only
		// performed for synchronous data registration.
		if (dataObjectFile != null) {
			HpcArchiveObjectMetadata objectMetadata = dataTransferService
					.addSystemGeneratedMetadataToDataObject(systemGeneratedMetadata.getArchiveLocation(),
							systemGeneratedMetadata.getDataTransferType(), systemGeneratedMetadata.getConfigurationId(),
							systemGeneratedMetadata.getS3ArchiveConfigurationId(),
							systemGeneratedMetadata.getObjectId(), systemGeneratedMetadata.getRegistrarId())
					.getArchiveObjectMetadata();

			// Update system-generated checksum metadata in iRODS w/ checksum value provided
			// from
			// the archive.
			Calendar deepArchiveDate = objectMetadata.getDeepArchiveStatus() != null
					&& objectMetadata.getDeepArchiveStatus().equals(HpcDeepArchiveStatus.IN_PROGRESS)
							? Calendar.getInstance()
							: null;
			metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, objectMetadata.getChecksum(),
					null, null, null, null, null, null, null, objectMetadata.getDeepArchiveStatus(), deepArchiveDate,
					null);

			// Automatically extract metadata from the file itself and add to iRODs.
			if (extractMetadata) {
				metadataService.addMetadataToDataObjectFromFile(path, dataObjectFile, configurationId, collectionType,
						true);
			}

			// Record data object registration result
			dataManagementService.addDataObjectRegistrationResult(path, systemGeneratedMetadata, true, null);

			// Generate the download URL.
			HpcDataObjectDownloadResponseDTO downloadRequestURL = null;
			try {
				downloadRequestURL = generateDownloadRequestURL(path);
			} catch (HpcException e) {
				logger.error("registerDataObject: {} - Failed to generate presigned download URL for {}", path, e);
			}

			// Add collection update event.
			addCollectionUpdatedEvent(path, false, true, userId,
					downloadRequestURL != null ? downloadRequestURL.getDownloadRequestURL() : null,
					downloadRequestURL != null ? downloadRequestURL.getSize().toString() : null,
					dataManagementService.getDataManagementConfiguration(configurationId).getDoc());
		}

		return uploadResponse;
	}

}
