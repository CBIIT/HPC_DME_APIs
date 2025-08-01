/**
 * HpcDataManagementServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileLocation;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidS3Account;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_STATUS_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DEEP_ARCHIVE_STATUS_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.LINK_SOURCE_PATH_ATTRIBUTE;
import static gov.nih.nci.hpc.util.HpcUtil.toIntExact;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.CollectionUtils;

import gov.nih.nci.hpc.dao.HpcBulkUpdateAuditDAO;
import gov.nih.nci.hpc.dao.HpcDataManagementAuditDAO;
import gov.nih.nci.hpc.dao.HpcDataRegistrationDAO;
import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcAuditRequestType;
import gov.nih.nci.hpc.domain.datamanagement.HpcBulkDataObjectRegistrationTaskStatus;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObjectRegistrationTaskItem;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermissionForCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadMethod;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDeepArchiveStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcStreamingUploadSource;
import gov.nih.nci.hpc.domain.error.HpcDomainValidationResult;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationItem;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationResult;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationStatus;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationRequest;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationResult;
import gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration;
import gov.nih.nci.hpc.domain.model.HpcStorageRecoveryConfiguration;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcNotificationService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * HPC Data Management Application Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataManagementServiceImpl implements HpcDataManagementService {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Data Management Proxy instance.
	@Autowired
	private HpcDataManagementProxy dataManagementProxy = null;

	// The Data Management Authenticator.
	@Autowired
	private HpcDataManagementAuthenticator dataManagementAuthenticator = null;

	// System Accounts locator.
	@Autowired
	private HpcSystemAccountLocator systemAccountLocator = null;

	// Data Hierarchy Validator.
	@Autowired
	private HpcDataHierarchyValidator dataHierarchyValidator = null;

	// Data Management configuration locator.
	@Autowired
	private HpcDataManagementConfigurationLocator dataManagementConfigurationLocator = null;

	// The Metadata service.
	@Autowired
	private HpcMetadataService metadataService = null;

	// Data Management Audit DAO.
	@Autowired
	private HpcDataManagementAuditDAO dataManagementAuditDAO = null;

	// Data Registration DAO.
	@Autowired
	private HpcDataRegistrationDAO dataRegistrationDAO = null;

	// Bulk Update Audit DAO.
	@Autowired
	private HpcBulkUpdateAuditDAO bulkUpdateAuditDAO = null;

	// Metadata DAO.
	@Autowired
	private HpcMetadataDAO metadataDAO = null;

	// Notification Application Service.
	@Autowired
	private HpcNotificationService notificationService = null;

	// Notification Application Service.
	@Autowired
	private HpcSecurityService securityService = null;

	// Pagination support.
	@Autowired
	@Qualifier("hpcRegistrationResultsPagination")
	private HpcPagination pagination = null;

	// Key Generator.
	@Autowired
	private HpcKeyGenerator keyGenerator = null;

	// Metadata Retriever.
	@Autowired
	private HpcMetadataRetriever metadataRetriever = null;

	// Prepared query to get data objects that have their data transfer in-progress
	// to archive.
	private List<HpcMetadataQuery> dataTransferReceivedQuery = new ArrayList<>();

	// Prepared query to get data objects that have their data transfer in-progress
	// to archive.
	private List<HpcMetadataQuery> dataTransferInProgressToArchiveQuery = new ArrayList<>();

	// Prepared query to get data objects that have their data transfer in-progress
	// to temporary archive.
	private List<HpcMetadataQuery> dataTransferInProgressToTemporaryArchiveQuery = new ArrayList<>();

	// Prepared query to get data objects that have their data transfer upload by
	// users via generated URL.
	private List<HpcMetadataQuery> dataTransferInProgressWithGeneratedURLQuery = new ArrayList<>();

	// Prepared query to get data objects that have their data transfer upload in
	// progress via
	// streaming.
	private List<HpcMetadataQuery> dataTransferStreamingInProgressQuery = new ArrayList<>();

	// Prepared query to get data objects that have their data transfer upload
	// failed via streaming.
	private List<HpcMetadataQuery> dataTransferStreamingFailedQuery = new ArrayList<>();

	// Prepared query to get data objects that have their data transfer upload via
	// streaming has
	// stopped.
	private List<HpcMetadataQuery> dataTransferStreamingStoppedQuery = new ArrayList<>();

	// Prepared query to get data objects that have their data in temporary archive.
	private List<HpcMetadataQuery> dataTransferInTemporaryArchiveQuery = new ArrayList<>();

	// Prepared query to get data objects that have their data placed in file system
	// for an upload.
	private List<HpcMetadataQuery> dataTransferFileSystemReadyQuery = new ArrayList<>();

	// Prepared query to get data objects that have their data placed in file system
	// for an upload.
	private List<HpcMetadataQuery> dataTransferFileSystemInProgressQuery = new ArrayList<>();

	// Prepared query to get data objects that have tier deep archive in-progress
	private List<HpcMetadataQuery> deepArchiveInProgressQuery = new ArrayList<>();

	// Prepared query to get data objects that have data transfer deleted that is
	// older than the retention
	private List<HpcMetadataQuery> deletedDataObjectsQuery = new ArrayList<>();

	// List of subjects (user-id / group-name) that permission update is not
	// allowed.
	private List<String> systemAdminSubjects = new ArrayList<>();

	// Default UI (deep link) URL to check on bulk registration status.
	private String defaultBulkRegistrationStatusUiURL = null;

	// Archive used to support soft deleted collections and data objects.
	private String deletedBasePath = null;

	// The number of days a deleted data object is retained
	private Integer deletedDataObjectRetentionDays = 0;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 *
	 * @param systemAdminSubjects                     The system admin subjects
	 *                                                (which update permissions not
	 *                                                allowed for).
	 * @param defaultBaseUiURL                        The default UI base URL.
	 * @param defaultBulkRegistrationStatusUiDeepLink The default UI deep link to
	 *                                                check on bulk registration
	 *                                                status. This URL need to have
	 *                                                a {taks_id} placeholder to
	 *                                                plug-in the task ID to be
	 *                                                displayed.
	 */
	private HpcDataManagementServiceImpl(String systemAdminSubjects, String defaultBaseUiURL,
			String defaultBulkRegistrationStatusUiDeepLink, String deletedBasePath,
			int deletedDataObjectRetentionDays) {
		// Prepare the query to get data objects in data transfer status of received.
		dataTransferReceivedQuery.add(toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE, HpcMetadataQueryOperator.EQUAL,
				HpcDataTransferUploadStatus.RECEIVED.value()));

		// Prepare the query to get data objects in data transfer in-progress to
		// archive.
		dataTransferInProgressToArchiveQuery.add(toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE,
				HpcMetadataQueryOperator.EQUAL, HpcDataTransferUploadStatus.IN_PROGRESS_TO_ARCHIVE.value()));

		// Prepare the query to get data objects in data transfer in-progress to
		// temporary archive.
		dataTransferInProgressToTemporaryArchiveQuery.add(toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE,
				HpcMetadataQueryOperator.EQUAL, HpcDataTransferUploadStatus.IN_PROGRESS_TO_TEMPORARY_ARCHIVE.value()));

		// Prepared query to get data objects that have their data transfer upload by
		// users via generated URL.
		dataTransferInProgressWithGeneratedURLQuery.add(toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE,
				HpcMetadataQueryOperator.EQUAL, HpcDataTransferUploadStatus.URL_GENERATED.value()));

		// Prepared query to get data objects that have their data transfer upload in
		// progress via
		// streaming
		dataTransferStreamingInProgressQuery.add(toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE,
				HpcMetadataQueryOperator.EQUAL, HpcDataTransferUploadStatus.STREAMING_IN_PROGRESS.value()));

		// Prepared query to get data objects that have their data transfer upload
		// failed via streaming
		dataTransferStreamingFailedQuery.add(toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE,
				HpcMetadataQueryOperator.EQUAL, HpcDataTransferUploadStatus.STREAMING_FAILED.value()));

		// Prepared query to get data objects that have their data transfer upload via
		// streaming
		// stopped.
		dataTransferStreamingStoppedQuery.add(toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE,
				HpcMetadataQueryOperator.EQUAL, HpcDataTransferUploadStatus.STREAMING_STOPPED.value()));

		// Prepare the query to get data objects in temporary archive.
		dataTransferInTemporaryArchiveQuery.add(toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE,
				HpcMetadataQueryOperator.EQUAL, HpcDataTransferUploadStatus.IN_TEMPORARY_ARCHIVE.value()));

		// Prepare the query to get data objects with files staged in file system for
		// upload.
		dataTransferFileSystemReadyQuery.add(toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE,
				HpcMetadataQueryOperator.EQUAL, HpcDataTransferUploadStatus.FILE_SYSTEM_READY.value()));

		// Prepare the query to get data objects with files staged in file system for
		// upload.
		dataTransferFileSystemInProgressQuery.add(toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE,
				HpcMetadataQueryOperator.EQUAL, HpcDataTransferUploadStatus.FILE_SYSTEM_IN_PROGRESS.value()));

		// Prepare the query to get data objects in deep archive status in-progress
		deepArchiveInProgressQuery.add(toMetadataQuery(DEEP_ARCHIVE_STATUS_ATTRIBUTE, HpcMetadataQueryOperator.EQUAL,
				HpcDeepArchiveStatus.IN_PROGRESS.value()));

		// Prepared query to get data objects that have their data transfer delete
		// requested
		deletedDataObjectsQuery.add(toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE, HpcMetadataQueryOperator.EQUAL,
				HpcDataTransferUploadStatus.DELETE_REQUESTED.value()));

		// Populate the list of system admin subjects (user-id / group-name). Set
		// permission is not
		// allowed for these subjects.
		this.systemAdminSubjects.addAll(Arrays.asList(systemAdminSubjects.split("\\s+")));

		defaultBulkRegistrationStatusUiURL = defaultBaseUiURL + '/' + defaultBulkRegistrationStatusUiDeepLink;

		this.deletedBasePath = deletedBasePath;

		this.deletedDataObjectRetentionDays = deletedDataObjectRetentionDays;
	}

	/**
	 * Default Constructor.
	 *
	 * @throws HpcException Constructor is disabled.
	 */
	private HpcDataManagementServiceImpl() throws HpcException {
		throw new HpcException("Default Constructor disabled", HpcErrorType.SPRING_CONFIGURATION_ERROR);
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataManagementService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public boolean createDirectory(String path) throws HpcException {
		Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
		String relativePath = dataManagementProxy.getRelativePath(path);
		// Validate the path is not a configured base path.
		if (dataManagementConfigurationLocator.getBasePaths().contains(relativePath)) {
			throw new HpcException("Invalid collection path: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the path is not root.
		if (relativePath.equals("/")) {
			throw new HpcException("Invalid collection path: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the directory path doesn't exist.
		HpcPathAttributes pathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken, path);
		if (pathAttributes.getExists()) {
			if (pathAttributes.getIsDirectory()) {
				// Directory already exists.
				return false;
			}
			if (pathAttributes.getIsFile()) {
				throw new HpcException("Path already exists as a file: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate the parent directory exists.
		if (!dataManagementProxy.isPathParentDirectory(authenticatedToken, path)) {
			throw new HpcException("Invalid collection path. Parent directory doesn't exist: " + path,
					HpcRequestRejectReason.INVALID_DATA_OBJECT_PATH);
		}

		// Create the directory.
		dataManagementProxy.createCollectionDirectory(authenticatedToken, path);

		// Set the permission inheritance to true, so any collection / data object
		// created under this
		// collection will inherit the permissions of this collection. This must be
		// executed as system
		// account as iRODs not allowing
		// users that are not owners to change the inheritance indicator.
		securityService.executeAsSystemAccount(Optional.empty(), () -> dataManagementProxy
				.setCollectionPermissionInheritace(dataManagementAuthenticator.getAuthenticatedToken(), path, true));

		return true;
	}

	@Override
	public boolean isPathParentDirectory(String path) throws HpcException {
		return dataManagementProxy.isPathParentDirectory(dataManagementAuthenticator.getAuthenticatedToken(), path);
	}

	@Override
	public boolean isPathCollection(String path) throws HpcException {
		if (!metadataRetriever.getCollectionMetadata(path).isEmpty()) {
			return true;
		}

		if (!metadataRetriever.getDataObjectMetadata(path).isEmpty()) {
			return false;
		}

		throw new HpcException("Failed to find item at following path: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
	}

	@Override
	public boolean createFile(String path) throws HpcException {
		Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();

		// Validate the file path.
		HpcPathAttributes pathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken, path);
		if (pathAttributes.getExists()) {
			if (pathAttributes.getIsFile()) {
				// File already exists.
				return false;
			}
			if (pathAttributes.getIsDirectory()) {
				throw new HpcException("Path already exists as a directory: " + path,
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate the parent directory exists.
		if (!dataManagementProxy.isPathParentDirectory(authenticatedToken, path)) {
			throw new HpcException("Invalid data object path. Parent directory doesn't exist: " + path,
					HpcRequestRejectReason.INVALID_DATA_OBJECT_PATH);
		}

		// Create the data object file.
		dataManagementProxy.createDataObjectFile(authenticatedToken, path);
		return true;
	}

	@Override
	public void delete(String path, boolean quiet) throws HpcException {
		try {
			// Delete the data object file.
			dataManagementProxy.delete(dataManagementAuthenticator.getAuthenticatedToken(), path);

			// Delete the data object metadata.
			metadataDAO.deleteDataObjectMetadata(dataManagementProxy.getAbsolutePath(path));

		} catch (HpcException e) {
			if (quiet) {
				logger.error("Failed to delete a file: {}", path, e);
				notificationService.sendNotification(e);

			} else {
				throw (e);
			}
		}
	}

	@Override
	public HpcPathAttributes move(String sourcePath, String destinationPath, Optional<Boolean> pathTypeValidation)
			throws HpcException {
		Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();

		// Validate the source path exists.
		HpcPathAttributes sourcePathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken, sourcePath);
		if (!sourcePathAttributes.getExists()) {
			throw new HpcException("Source path doesn't exist", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Optionally perform path type validation.
		if (pathTypeValidation.isPresent()) {
			if (pathTypeValidation.get()) {
				if (!sourcePathAttributes.getIsDirectory()) {
					throw new HpcException("Source path is not of a collection", HpcErrorType.INVALID_REQUEST_INPUT);
				}
			} else if (!sourcePathAttributes.getIsFile()) {
				throw new HpcException("Source path is not of a data object", HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate the destination path doesn't exist already.
		HpcPathAttributes destinationPathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken,
				destinationPath);
		if (destinationPathAttributes.getExists()) {
			throw new HpcException("Destination path already exists", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the destination parent path exists. i.e. we enforce the move is to
		// an existing
		// collection.
		String destinationParentPath = destinationPath.substring(0, destinationPath.lastIndexOf('/'));
		HpcPathAttributes destinationParentPathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken,
				destinationParentPath);
		if (!destinationParentPathAttributes.getExists()) {
			throw new HpcException("Destination parent path doesn't exist", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Perform the move request.
		dataManagementProxy.move(authenticatedToken, sourcePath, destinationPath);

		// Validate the hierarchy.
		try {
			// Calculate the validation path. It's the collection containing the data object
			// if we move a
			// data object,
			// or the collection itself if we move a collection.
			String hierachyValidationPath = sourcePathAttributes.getIsFile() ? destinationParentPath : destinationPath;
			validateHierarchy(hierachyValidationPath, this.findDataManagementConfigurationId(destinationPath),
					sourcePathAttributes.getIsFile());

		} catch (HpcException e) {
			// Hierarchy is invalid. Revert and rethrow the exception.
			dataManagementProxy.move(authenticatedToken, destinationPath, sourcePath);
			throw (e);
		}

		if (sourcePathAttributes.getIsFile()) {
			// Delete data object metadata entries from table, HPC_DATA_META_MAIN for the
			// source path
			metadataDAO.deleteDataObjectMetadata(dataManagementProxy.getAbsolutePath(sourcePath));

			// Refresh data object metadata entries from table, HPC_DATA_META_MAIN
			metadataDAO.upsertDataObjectMetadata(dataManagementProxy.getAbsolutePath(destinationPath));
		} else {
			// Delete data object metadata entries from table, HPC_DATA_META_MAIN for the
			// source path
			metadataDAO.deleteDataObjectMetadataUnderCollection(dataManagementProxy.getAbsolutePath(sourcePath));

			// Refresh data object metadata entries from table, HPC_DATA_META_MAIN
			metadataDAO.insertDataObjectMetadataUnderCollection(dataManagementProxy.getAbsolutePath(destinationPath));
		}

		// Update the links to this data object to point to the new path.
		getDataObjectLinks(sourcePath).forEach(link -> {
			try {
				metadataService.updateDataObjectSystemGeneratedMetadata(link.getAbsolutePath(), null, null, null, null,
						null, null, null, null, destinationPath, null, null, null, null);
			} catch (HpcException e) {
				logger.error("Failed to point link[{}] to {}", link.getAbsolutePath(), destinationPath);
			}
		});

		return sourcePathAttributes;
	}

	@Override
	public void softDelete(String sourcePath, Optional<Boolean> pathTypeValidation) throws HpcException {
		Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();

		// Validate the source path exists.
		HpcPathAttributes sourcePathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken, sourcePath);
		if (!sourcePathAttributes.getExists()) {
			throw new HpcException("Source path doesn't exist", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Optionally perform path type validation.
		if (pathTypeValidation.isPresent()) {
			if (pathTypeValidation.get()) {
				if (!sourcePathAttributes.getIsDirectory()) {
					throw new HpcException("Source path is not of a collection", HpcErrorType.INVALID_REQUEST_INPUT);
				}
			} else if (!sourcePathAttributes.getIsFile()) {
				throw new HpcException("Source path is not of a data object", HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Construct the destination path.
		String destinationPath = deletedBasePath + sourcePath;
		// Validate the destination path doesn't exist already.
		HpcPathAttributes destinationPathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken,
				destinationPath);
		if (destinationPathAttributes.getExists()) {
			// If destination path already exists, append timestamp to make the path unique.
			String dateFormat = "yyyyMMddHHmmss";
			LocalDateTime date = LocalDateTime.now(ZoneId.of("UTC-04:00"));
			destinationPath = destinationPath + "_" + date.format(DateTimeFormatter.ofPattern(dateFormat));
		}

		// Validate the destination parent path exists.
		// If it doesn't exist, create the parent collection.
		String sourceParentPath = sourcePath.substring(0, sourcePath.lastIndexOf('/'));
		String destinationParentPath = destinationPath.substring(0, destinationPath.lastIndexOf('/'));
		HpcPathAttributes destinationParentPathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken,
				destinationParentPath);
		if (!destinationParentPathAttributes.getExists()) {
			// Create destination parent collection recursively. Copy all metadata and
			// permissions
			copyCollection(authenticatedToken, sourceParentPath, destinationParentPath);
		}

		// Perform the move request.
		dataManagementProxy.move(authenticatedToken, sourcePath, destinationPath);

		// Remove permissions
		List<HpcSubjectPermission> permissions = getDataObjectPermissions(destinationPath);
		HpcIntegratedSystemAccount dataManagementAccount = systemAccountLocator
				.getSystemAccount(HpcIntegratedSystem.IRODS);
		if (dataManagementAccount == null) {
			throw new HpcException("System Data Management Account not configured", HpcErrorType.UNEXPECTED_ERROR);
		}

		for (HpcSubjectPermission permission : permissions) {
			// Exclude system accounts and system admin group
			String subject = permission.getSubject();
			if (!subject.equals(dataManagementAccount.getUsername()) && !systemAdminSubjects.contains(subject)) {
				permission.setPermission(HpcPermission.NONE);
				setDataObjectPermission(destinationPath, permission);
			}
		}

		metadataService.updateDataObjectSystemGeneratedMetadata(destinationPath, null, null, null,
				HpcDataTransferUploadStatus.DELETE_REQUESTED, null, null, null, null, null, null, null, null, null);

		if (sourcePathAttributes.getIsFile()) {
			// Delete data object metadata entries from table, HPC_DATA_META_MAIN for the
			// source path
			metadataDAO.deleteDataObjectMetadata(dataManagementProxy.getAbsolutePath(sourcePath));
		} else {
			metadataDAO.deleteDataObjectMetadataUnderCollection(dataManagementProxy.getAbsolutePath(sourcePath));
			// Refresh data object metadata entries from table, HPC_DATA_META_MAIN for the
			// destination path
			metadataDAO.insertDataObjectMetadataUnderCollection(dataManagementProxy.getAbsolutePath(destinationPath));
		}

	}

	@Override
	public void recover(String sourcePath, Optional<Boolean> pathTypeValidation) throws HpcException {
		Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();

		// Validate the source path exists.
		HpcPathAttributes sourcePathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken, sourcePath);
		if (!sourcePathAttributes.getExists()) {
			throw new HpcException("Source path doesn't exist", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Optionally perform path type validation.
		if (pathTypeValidation.isPresent()) {
			if (pathTypeValidation.get()) {
				if (!sourcePathAttributes.getIsDirectory()) {
					throw new HpcException("Source path is not of a collection", HpcErrorType.INVALID_REQUEST_INPUT);
				}
			} else if (!sourcePathAttributes.getIsFile()) {
				throw new HpcException("Source path is not of a data object", HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Construct the destination path.
		String destinationPath = sourcePath.replace(deletedBasePath, "");
		// Validate the destination path doesn't exist already.
		HpcPathAttributes destinationPathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken,
				destinationPath);
		if (destinationPathAttributes.getExists()) {
			throw new HpcException("Destination path already exists", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the destination parent path exists.
		// If it doesn't exist, create the parent collection.
		String sourceParentPath = sourcePath.substring(0, sourcePath.lastIndexOf('/'));
		String destinationParentPath = destinationPath.substring(0, destinationPath.lastIndexOf('/'));
		HpcPathAttributes destinationParentPathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken,
				destinationParentPath);
		if (!destinationParentPathAttributes.getExists()) {
			// Create destination parent collection recursively. Copy all metadata and
			// permissions
			copyCollection(authenticatedToken, sourceParentPath, destinationParentPath);
		}

		// Perform the move request.
		dataManagementProxy.move(authenticatedToken, sourcePath, destinationPath);

		metadataService.updateDataObjectSystemGeneratedMetadata(destinationPath, null, null, null,
				HpcDataTransferUploadStatus.RECOVER_REQUESTED, null, null, null, null, null, null, null, null, null);
	}

	@Override
	public void addAuditRecord(String path, HpcAuditRequestType requestType, HpcMetadataEntries metadataBefore,
			HpcMetadataEntries metadataAfter, HpcFileLocation archiveLocation, boolean dataManagementStatus,
			Boolean dataTransferStatus, String message, String userId, Long size,
			HpcStorageRecoveryConfiguration storageRecoveryConfiguration) {
		// Input validation.
		String nciUserId = HpcRequestContext.getRequestInvoker().getNciAccount() == null ? userId
				: HpcRequestContext.getRequestInvoker().getNciAccount().getUserId();

		if (path == null || requestType == null || metadataBefore == null || StringUtils.isBlank(nciUserId)) {
			return;
		}

		try {
			dataManagementAuditDAO.insert(nciUserId, path, requestType, metadataBefore, metadataAfter, archiveLocation,
					dataManagementStatus, dataTransferStatus, message, Calendar.getInstance(), size,
					storageRecoveryConfiguration);

		} catch (HpcException e) {
			logger.error("Failed to add an audit record", HpcErrorType.DATABASE_ERROR, e);
		}
	}

	@Override
	public void setCollectionPermission(String path, HpcSubjectPermission subjectPermission) throws HpcException {
		// Validate the permission request - ensure the subject is NOT a system account.
		validatePermissionRequest(subjectPermission);

		dataManagementProxy.setCollectionPermission(dataManagementAuthenticator.getAuthenticatedToken(), path,
				subjectPermission);
	}

	@Override
	public List<HpcSubjectPermission> getCollectionPermissions(String path) throws HpcException {
		return dataManagementProxy.getCollectionPermissions(dataManagementAuthenticator.getAuthenticatedToken(), path);
	}

	@Override
	public HpcSubjectPermission getCollectionPermission(String path, String userId) throws HpcException {
		return dataManagementProxy.getCollectionPermission(dataManagementAuthenticator.getAuthenticatedToken(), path,
				userId);
	}

	@Override
	public HpcSubjectPermission acquireCollectionPermission(String path, String userId) throws HpcException {
		return dataManagementProxy.acquireCollectionPermission(dataManagementAuthenticator.getAuthenticatedToken(),
				path, userId);
	}

	@Override
	public List<HpcPermissionForCollection> acquireChildrenCollectionsPermissionsForUser(String parentPath,
			String userId) throws HpcException {
		return dataManagementProxy.acquireChildrenCollectionsPermissionForUser(
				dataManagementAuthenticator.getAuthenticatedToken(), parentPath, userId);
	}

	@Override
	public HpcSubjectPermission getCollectionPermission(String path) throws HpcException {
		return dataManagementProxy.getCollectionPermission(dataManagementAuthenticator.getAuthenticatedToken(), path,
				HpcRequestContext.getRequestInvoker().getNciAccount().getUserId());
	}

	@Override
	public void setDataObjectPermission(String path, HpcSubjectPermission subjectPermission) throws HpcException {
		// Validate the permission request - ensure the subject is NOT a system account.
		validatePermissionRequest(subjectPermission);

		dataManagementProxy.setDataObjectPermission(dataManagementAuthenticator.getAuthenticatedToken(), path,
				subjectPermission);
	}

	@Override
	public List<HpcSubjectPermission> getDataObjectPermissions(String path) throws HpcException {
		return dataManagementProxy.getDataObjectPermissions(dataManagementAuthenticator.getAuthenticatedToken(), path);
	}

	@Override
	public HpcSubjectPermission getDataObjectPermission(String path, String userId) throws HpcException {
		return dataManagementProxy.getDataObjectPermission(dataManagementAuthenticator.getAuthenticatedToken(), path,
				userId);
	}

	@Override
	public HpcSubjectPermission acquireDataObjectPermission(String path, String userId) throws HpcException {
		return dataManagementProxy.acquireDataObjectPermission(dataManagementAuthenticator.getAuthenticatedToken(),
				path, userId);
	}

	@Override
	public HpcSubjectPermission getDataObjectPermission(String path) throws HpcException {
		return dataManagementProxy.getDataObjectPermission(dataManagementAuthenticator.getAuthenticatedToken(), path,
				HpcRequestContext.getRequestInvoker().getNciAccount().getUserId());
	}

	@Override
	public void setCoOwnership(String path, String userId) throws HpcException {
		HpcIntegratedSystemAccount dataManagementAccount = systemAccountLocator
				.getSystemAccount(HpcIntegratedSystem.IRODS);
		if (dataManagementAccount == null) {
			throw new HpcException("System Data Management Account not configured", HpcErrorType.UNEXPECTED_ERROR);
		}

		// System account ownership request.
		HpcSubjectPermission systemAccountPermissionRequest = new HpcSubjectPermission();
		systemAccountPermissionRequest.setPermission(HpcPermission.OWN);
		systemAccountPermissionRequest.setSubject(dataManagementAccount.getUsername());

		// User ownership request.
		HpcSubjectPermission userPermissionRequest = new HpcSubjectPermission();
		userPermissionRequest.setPermission(HpcPermission.OWN);
		userPermissionRequest.setSubject(userId);

		// Determine if it's a collection or data object.
		Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
		HpcPathAttributes pathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken, path);
		if (pathAttributes.getIsDirectory()) {
			dataManagementProxy.setCollectionPermission(authenticatedToken, path, systemAccountPermissionRequest);
			dataManagementProxy.setCollectionPermission(authenticatedToken, path, userPermissionRequest);
		} else if (pathAttributes.getIsFile()) {
			dataManagementProxy.setDataObjectPermission(authenticatedToken, path, systemAccountPermissionRequest);
			dataManagementProxy.setDataObjectPermission(authenticatedToken, path, userPermissionRequest);
		}
	}

	@Override
	public void validateHierarchy(String path, String configurationId, boolean dataObjectRegistration)
			throws HpcException {

		// Calculate the collection path to validate.
		String validationCollectionPath = dataManagementProxy.getRelativePath(path);
		validationCollectionPath = validationCollectionPath.substring(1, validationCollectionPath.length());

		// Build the collection path types list.
		List<String> collectionPathTypes = new ArrayList<>();
		StringBuilder subCollectionPath = new StringBuilder();
		for (String s : validationCollectionPath.split("/")) {
			subCollectionPath.append("/" + s);
			String collectionType = getCollectionType(subCollectionPath.toString());
			if (collectionType == null) {
				if (!collectionPathTypes.isEmpty()) {
					throw new HpcException("Invalid collection path hierarchy: " + path,
							HpcErrorType.INVALID_REQUEST_INPUT);
				}
			} else {
				collectionPathTypes.add(collectionType);
			}
		}

		// Perform the hierarchy validation.
		dataHierarchyValidator.validateHierarchy(configurationId, collectionPathTypes, dataObjectRegistration);
	}

	@Override
	public boolean collectionExists(String path) throws HpcException {
		return getCollection(path, false, null) != null;
	}

	@Override
	public HpcCollection getCollection(String path, boolean list, String linkSourcePath) throws HpcException {
		Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
		if (dataManagementProxy.getPathAttributes(authenticatedToken, path).getIsDirectory()) {
			return dataManagementProxy.getCollection(authenticatedToken, path, list, linkSourcePath);
		}

		return null;
	}

	@Override
	public HpcCollection getFullCollection(String path, String linkSourcePath) throws HpcException {
		Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
		if (dataManagementProxy.getPathAttributes(authenticatedToken, path).getIsDirectory()) {
			HpcCollection collection = dataManagementProxy.getCollectionChildrenWithPaging(authenticatedToken, path, 0,
					linkSourcePath);

			int totalRecordsFetched = (collection.getSubCollections() == null ? 0
					: collection.getSubCollections().size())
					+ (collection.getDataObjects() == null ? 0 : collection.getDataObjects().size());
			int totalRecords = collection.getSubCollectionsTotalRecords() + collection.getDataObjectsTotalRecords();

			// Check if there are more sub-collections or data objects to be fetched
			while (totalRecordsFetched < totalRecords) {
				HpcCollection collectionContinued = dataManagementProxy
						.getCollectionChildrenWithPaging(authenticatedToken, path, totalRecordsFetched, linkSourcePath);
				collection.getSubCollections().addAll(collectionContinued.getSubCollections());
				collection.getDataObjects().addAll(collectionContinued.getDataObjects());
				totalRecordsFetched = (collection.getSubCollections() == null ? 0
						: collection.getSubCollections().size())
						+ (collection.getDataObjects() == null ? 0 : collection.getDataObjects().size());
			}
			return collection;
		}

		return null;
	}

	@Override
	public HpcCollection getCollectionChildren(String path, String linkSourcePath) throws HpcException {
		Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
		return dataManagementProxy.getCollectionChildren(authenticatedToken, path, linkSourcePath);
	}

	@Override
	public HpcCollection getCollectionChildrenWithPaging(String path, Integer offset, String linkSourcePath)
			throws HpcException {
		Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
		return dataManagementProxy.getCollectionChildrenWithPaging(authenticatedToken, path, offset, linkSourcePath);
	}

	@Override
	public HpcDataObject getDataObject(String path) throws HpcException {
		Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();
		if (dataManagementProxy.getPathAttributes(authenticatedToken, path).getIsFile()) {
			return dataManagementProxy.getDataObject(authenticatedToken, path);
		}

		return null;
	}

	@Override
	public List<HpcDataObject> getDataObjectsUploadReceived() throws HpcException {
		return metadataRetriever.getDataObjects(dataTransferReceivedQuery);
	}

	@Override
	public List<HpcDataObject> getDataObjectsUploadInProgress() throws HpcException {
		List<HpcDataObject> objectsInProgress = new ArrayList<>();
		objectsInProgress.addAll(metadataRetriever.getDataObjects(dataTransferInProgressToArchiveQuery));
		objectsInProgress.addAll(metadataRetriever.getDataObjects(dataTransferInProgressToTemporaryArchiveQuery));

		return objectsInProgress;
	}

	@Override
	public List<HpcDataObject> getDataTranferUploadInProgressWithGeneratedURL() throws HpcException {
		return metadataRetriever.getDataObjects(dataTransferInProgressWithGeneratedURLQuery);
	}

	@Override
	public List<HpcDataObject> getDataTranferUploadStreamingInProgress() throws HpcException {
		return metadataRetriever.getDataObjects(dataTransferStreamingInProgressQuery);
	}

	@Override
	public List<HpcDataObject> getDataTranferUploadStreamingFailed() throws HpcException {
		return metadataRetriever.getDataObjects(dataTransferStreamingFailedQuery);
	}

	@Override
	public List<HpcDataObject> getDataTranferUploadStreamingStopped() throws HpcException {
		return metadataRetriever.getDataObjects(dataTransferStreamingStoppedQuery);
	}

	@Override
	public List<HpcDataObject> getDataObjectsUploadInTemporaryArchive() throws HpcException {
		return metadataRetriever.getDataObjects(dataTransferInTemporaryArchiveQuery);
	}

	@Override
	public List<HpcDataObject> getDataObjectsUploadFileSystemReady() throws HpcException {
		return metadataRetriever.getDataObjects(dataTransferFileSystemReadyQuery);
	}

	@Override
	public List<HpcDataObject> getDataObjectsUploadFileSystemInProgress() throws HpcException {
		return metadataRetriever.getDataObjects(dataTransferFileSystemInProgressQuery);
	}

	@Override
	public List<HpcDataObject> getDataObjectLinks(String path) throws HpcException {
		List<HpcMetadataQuery> dataObjectLinksQuery = new ArrayList<>();
		dataObjectLinksQuery.add(toMetadataQuery(LINK_SOURCE_PATH_ATTRIBUTE, HpcMetadataQueryOperator.EQUAL, path));

		return metadataRetriever.getDataObjects(dataObjectLinksQuery);
	}

	@Override
	public List<HpcDataObject> getDataObjectArchiveFileIdContainsPath(String path) throws HpcException {
		List<HpcMetadataQuery> dataObjectArchiveFileIdContainsPathQuery = new ArrayList<>();
		dataObjectArchiveFileIdContainsPathQuery.add(
				toMetadataQuery(ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE, HpcMetadataQueryOperator.LIKE, "%" + path + "%"));

		return dataManagementProxy.getDataObjects(dataManagementAuthenticator.getAuthenticatedToken(),
				dataObjectArchiveFileIdContainsPathQuery);
	}

	@Override
	public void closeConnection() {
		try {
			if (dataManagementAuthenticator.isAuthenticated()) {
				// Close the connection to iRODS.
				dataManagementProxy.disconnect(dataManagementAuthenticator.getAuthenticatedToken());

				// Clear the token.
				dataManagementAuthenticator.clearToken();
			}

		} catch (HpcException e) {
			// Ignore.
			logger.error("Failed to close data management connection", e);
		}
	}

	@Override
	public String registerDataObjects(String userId, String uiURL,
			Map<String, HpcDataObjectRegistrationRequest> dataObjectRegistrationRequests) throws HpcException {
		// Input validation
		if (StringUtils.isEmpty(userId)) {
			throw new HpcException("Null / Empty userId in registration list request",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (dataObjectRegistrationRequests.isEmpty()) {
			throw new HpcException("Empty registration request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Create a bulk data object registration task.
		HpcBulkDataObjectRegistrationTask bulkDataObjectRegistrationTask = new HpcBulkDataObjectRegistrationTask();
		bulkDataObjectRegistrationTask.setUserId(userId);
		bulkDataObjectRegistrationTask
				.setUiURL(!StringUtils.isEmpty(uiURL) ? uiURL : defaultBulkRegistrationStatusUiURL);
		bulkDataObjectRegistrationTask.setCreated(Calendar.getInstance());
		bulkDataObjectRegistrationTask.setStatus(HpcBulkDataObjectRegistrationTaskStatus.RECEIVED);
		bulkDataObjectRegistrationTask
				.setUploadMethod(toDataTransferUploadMethod(dataObjectRegistrationRequests.values().iterator().next()));

		// Iterate through the individual data object registration requests and add them
		// as items to the
		// list registration task.
		for (String path : dataObjectRegistrationRequests.keySet()) {
			HpcDataObjectRegistrationRequest registrationRequest = dataObjectRegistrationRequests.get(path);
			// Validate registration request.
			validateDataObjectRegistrationRequest(registrationRequest, path);

			// Create a data object registration item.
			HpcBulkDataObjectRegistrationItem registrationItem = new HpcBulkDataObjectRegistrationItem();
			HpcDataObjectRegistrationTaskItem reqistrationTask = new HpcDataObjectRegistrationTaskItem();
			reqistrationTask.setPath(path);
			registrationItem.setTask(reqistrationTask);
			registrationItem.setRequest(registrationRequest);

			bulkDataObjectRegistrationTask.getItems().add(registrationItem);
		}

		// Persist the registration request.
		dataRegistrationDAO.upsertBulkDataObjectRegistrationTask(bulkDataObjectRegistrationTask);
		return bulkDataObjectRegistrationTask.getId();
	}

	@Override
	public List<HpcBulkDataObjectRegistrationTask> getBulkDataObjectRegistrationTasks(
			HpcBulkDataObjectRegistrationTaskStatus status) throws HpcException {
		return dataRegistrationDAO.getBulkDataObjectRegistrationTasks(status);
	}

	@Override
	public void updateBulkDataObjectRegistrationTask(HpcBulkDataObjectRegistrationTask registrationTask)
			throws HpcException {
		dataRegistrationDAO.upsertBulkDataObjectRegistrationTask(registrationTask);
	}

	@Override
	public void completeBulkDataObjectRegistrationTask(HpcBulkDataObjectRegistrationTask registrationTask,
			boolean result, String message, Calendar completed) throws HpcException {
		// Input validation
		if (registrationTask == null) {
			throw new HpcException("Invalid data object list registration task", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Cleanup the DB record.
		dataRegistrationDAO.deleteBulkDataObjectRegistrationTask(registrationTask.getId());

		// Create a registration result object.
		HpcBulkDataObjectRegistrationResult registrationResult = new HpcBulkDataObjectRegistrationResult();
		registrationResult.setId(registrationTask.getId());
		registrationResult.setUserId(registrationTask.getUserId());
		registrationResult.setResult(result);
		registrationResult.setMessage(message);
		registrationResult.setCreated(registrationTask.getCreated());
		registrationResult.setCompleted(completed);
		registrationResult.setUploadMethod(registrationTask.getUploadMethod());
		registrationResult.getItems().addAll(registrationTask.getItems());

		// Calculate the effective transfer speed (Bytes per second). This is done by
		// averaging the
		// effective transfer speed
		// of all successful registration items.
		int effectiveTransferSpeed = 0;
		int completedItems = 0;
		for (HpcBulkDataObjectRegistrationItem item : registrationTask.getItems()) {
			if (Boolean.TRUE.equals(item.getTask().getResult()) && item.getRequest().getLinkSourcePath() == null
					&& item.getTask().getEffectiveTransferSpeed() != null) {
				effectiveTransferSpeed += item.getTask().getEffectiveTransferSpeed();
				completedItems++;
			}
		}
		registrationResult
				.setEffectiveTransferSpeed(completedItems > 0 ? effectiveTransferSpeed / completedItems : null);

		// For each registration item from AWS S3, mask the (user provided) S3 account
		// information
		// before storing in the DB.
		registrationResult.getItems()
				.forEach(bulkDataObjectRegistrationItem -> Optional.of(bulkDataObjectRegistrationItem.getRequest())
						.map(HpcDataObjectRegistrationRequest::getS3UploadSource)
						.map(HpcStreamingUploadSource::getAccount).ifPresent(s3Account -> {
							s3Account.setAccessKey("****");
							s3Account.setSecretKey("****");
						}));

		// Persist to DB.
		dataRegistrationDAO.upsertBulkDataObjectRegistrationResult(registrationResult);
	}

	@Override
	public HpcBulkDataObjectRegistrationStatus getBulkDataObjectRegistrationTaskStatus(String taskId)
			throws HpcException {
		if (StringUtils.isEmpty(taskId)) {
			throw new HpcException("Null / Empty task id", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcBulkDataObjectRegistrationStatus taskStatus = new HpcBulkDataObjectRegistrationStatus();
		HpcBulkDataObjectRegistrationResult taskResult = dataRegistrationDAO
				.getBulkDataObjectRegistrationResult(taskId);
		if (taskResult != null) {
			// Task completed or failed. Return the result.
			taskStatus.setInProgress(false);
			taskStatus.setResult(taskResult);
			return taskStatus;
		}

		// Task still in-progress.
		taskStatus.setInProgress(true);
		HpcBulkDataObjectRegistrationTask task = dataRegistrationDAO.getBulkDataObjectRegistrationTask(taskId);
		if (task != null) {
			taskStatus.setTask(task);
			return taskStatus;
		}

		// Task not found.
		return null;
	}

	@Override
	public List<HpcBulkDataObjectRegistrationTask> getRegistrationTasks(String userId, String doc) throws HpcException {
		List<HpcBulkDataObjectRegistrationTask> registrationTasks = null;
		if (doc == null) {
			registrationTasks = dataRegistrationDAO.getBulkDataObjectRegistrationTasks(userId);
		} else if (doc.equals("ALL")) {
			registrationTasks = dataRegistrationDAO.getAllBulkDataObjectRegistrationTasks();
		} else {
			registrationTasks = dataRegistrationDAO.getBulkDataObjectRegistrationTasksForDoc(doc);
		}
		return registrationTasks;
	}

	@Override
	public List<HpcBulkDataObjectRegistrationResult> getRegistrationResults(String userId, int page, String doc,
			int activeRequestsOffset) throws HpcException {
		List<HpcBulkDataObjectRegistrationResult> registrationResults = null;
		if (doc == null) {
			registrationResults = dataRegistrationDAO.getBulkDataObjectRegistrationResults(userId,
					pagination.getOffset(page), pagination.getPageSize() - activeRequestsOffset);
		} else if (doc.equals("ALL")) {
			registrationResults = dataRegistrationDAO.getAllBulkDataObjectRegistrationResults(
					pagination.getOffset(page), pagination.getPageSize() - activeRequestsOffset);
		} else {
			registrationResults = dataRegistrationDAO.getBulkDataObjectRegistrationResultsForDoc(doc,
					pagination.getOffset(page), pagination.getPageSize() - activeRequestsOffset);
		}
		return registrationResults;
	}

	@Override
	public int getRegistrationResultsCount(String userId, String doc) throws HpcException {
		int count = 0;
		if (doc == null) {
			count = dataRegistrationDAO.getBulkDataObjectRegistrationResultsCount(userId);
		} else if (doc.equals("ALL")) {
			count = dataRegistrationDAO.getAllBulkDataObjectRegistrationResultsCount();
		} else {
			count = dataRegistrationDAO.getBulkDataObjectRegistrationResultsCountForDoc(doc);
		}
		return count;
	}

	@Override
	public int getRegistrationResultsPageSize() {
		return pagination.getPageSize();
	}

	@Override
	public String getCollectionType(String path) throws HpcException {
		for (HpcMetadataEntry metadataEntry : metadataRetriever.getCollectionMetadata(path)) {
			if (metadataEntry.getAttribute().equals(HpcMetadataValidator.COLLECTION_TYPE_ATTRIBUTE)) {
				return metadataEntry.getValue();
			}
		}

		return null;
	}

	@Override
	public List<HpcDataManagementConfiguration> getDataManagementConfigurations() {
		return new ArrayList<>(dataManagementConfigurationLocator.values());
	}

	@Override
	public String findDataManagementConfigurationId(String path) {
		if (StringUtils.isEmpty(path)) {
			return null;
		}

		String relativePath = dataManagementProxy.getRelativePath(path);
		for (HpcDataManagementConfiguration dataManagementConfiguration : dataManagementConfigurationLocator.values()) {
			if (relativePath.startsWith(dataManagementConfiguration.getBasePath())) {
				return dataManagementConfiguration.getId();
			}
		}

		return null;
	}

	@Override
	public String getDataManagementConfigurationId(String basePath) {
		return dataManagementConfigurationLocator.getConfigurationId(basePath);
	}

	@Override
	public HpcDataManagementConfiguration getDataManagementConfiguration(String id) {
		return dataManagementConfigurationLocator.get(id);
	}

	@Override
	public void addDataObjectRegistrationResult(String path, HpcSystemGeneratedMetadata systemGeneratedMetadata,
			boolean result, String message) throws HpcException {
		if (systemGeneratedMetadata == null) {
			logger.error("Null system-generated-metadata");
			return;
		}

		HpcDataObjectRegistrationResult dataObjectRegistrationResult = new HpcDataObjectRegistrationResult();
		Calendar now = Calendar.getInstance();
		dataObjectRegistrationResult.setId(systemGeneratedMetadata.getObjectId());
		dataObjectRegistrationResult.setPath(path);
		dataObjectRegistrationResult.setResult(result);
		dataObjectRegistrationResult.setMessage(message);
		dataObjectRegistrationResult.setUploadMethod(systemGeneratedMetadata.getDataTransferMethod());
		dataObjectRegistrationResult.setUserId(systemGeneratedMetadata.getRegistrarId());
		dataObjectRegistrationResult
				.setCreated(Optional.ofNullable(systemGeneratedMetadata.getDataTransferStarted()).orElse(now));
		dataObjectRegistrationResult
				.setCompleted(Optional.ofNullable(systemGeneratedMetadata.getDataTransferCompleted()).orElse(now));
		dataObjectRegistrationResult.setDataTransferRequestId(systemGeneratedMetadata.getDataTransferRequestId());
		dataObjectRegistrationResult.setSourceLocation(systemGeneratedMetadata.getSourceLocation());

		addDataObjectRegistrationResult(path, dataObjectRegistrationResult, systemGeneratedMetadata.getSourceSize());
	}

	@Override
	public void addDataObjectRegistrationResult(String path,
			HpcDataObjectRegistrationResult dataObjectRegistrationResult, Long size) throws HpcException {
		if (StringUtils.isEmpty(path)) {
			logger.error("Null / Empty path");
			return;
		}

		if (StringUtils.isEmpty(dataObjectRegistrationResult.getId())) {
			// In case of some failure scenarios, the data object UUID was not created, so
			// we
			// create it here
			// just to record a valid ID in the DB table.
			dataObjectRegistrationResult.setId(keyGenerator.generateKey());
		}

		Calendar created = dataObjectRegistrationResult.getCreated();
		Calendar completed = dataObjectRegistrationResult.getCompleted();

		// Calculate the effective transfer speed (Bytes per second). Note: This is N/A
		// for SYNC
		// uploads.
		HpcDataTransferUploadMethod uploadMethod = dataObjectRegistrationResult.getUploadMethod();
		if (uploadMethod != null && !uploadMethod.equals(HpcDataTransferUploadMethod.SYNC) && size != null
				&& created != null && completed != null) {
			long transferTime = completed.getTimeInMillis() - created.getTimeInMillis();
			if (transferTime > 0) {
				dataObjectRegistrationResult.setEffectiveTransferSpeed(toIntExact(size * 1000 / (transferTime)));
			}
		}

		dataRegistrationDAO.insertDataObjectRegistrationResult(dataObjectRegistrationResult);
	}

	@Override
	public HpcDataTransferConfiguration getS3ArchiveConfiguration(String s3ArchiveConfigurationId) throws HpcException {
		return dataManagementConfigurationLocator.getS3ArchiveConfiguration(s3ArchiveConfigurationId);
	}

	@Override
	public boolean hasDataObjects(HpcCollection collection) throws HpcException {

		if (!CollectionUtils.isEmpty(collection.getDataObjects()))
			return true;

		for (HpcCollectionListingEntry subCollection : collection.getSubCollections()) {
			HpcCollection childCollection = getCollection(subCollection.getPath(), true, null);
			if (hasDataObjects(childCollection))
				return true;
		}
		return false;
	}

	@Override
	public List<HpcDataObject> getDataObjectsDeepArchiveInProgress() throws HpcException {
		return metadataRetriever.getDataObjects(deepArchiveInProgressQuery);
	}

	@Override
	public List<HpcDataObject> getDeletedDataObjects() throws HpcException {
		return metadataRetriever.getDataObjects(deletedDataObjectsQuery);
	}

	@Override
	public boolean deletedDataObjectExpired(Calendar deletedDate) {
		if (deletedDate == null) {
			return false;
		}

		// Check if the deleted date is older than the retention period
		deletedDate.add(Calendar.DAY_OF_MONTH, deletedDataObjectRetentionDays);
		// If expired, return true
		return deletedDate.before(Calendar.getInstance());
	}

	@Override
	public String getOriginalPathForDeletedDataObject(String path, HpcFileLocation archiveLocation) throws HpcException {
	    // Validate archiveLocation is not null
	    if (archiveLocation == null) {
	        throw new HpcException("archiveLocation is null. Cannot determine the original path for the deleted data object.", HpcErrorType.DATA_MANAGEMENT_ERROR);
	    }
	  
	    // Check if it is a deleted archive record
	    if (path == null || !path.startsWith(deletedBasePath)) {
	        throw new HpcException("The deleted path is not from DME_Deleted_Archive: " + path, HpcErrorType.DATA_MANAGEMENT_ERROR);
	    }
	    
	    // Get original file name from the archive file id
	    String originalFileName = StringUtils.substringAfterLast(archiveLocation.getFileId(), "/");
	    
	    // Check if the record has a timestamp appended to the end by comparing with the original file name
	    if (!path.endsWith(originalFileName)) {
	      
	        // Make sure the timestamp is what we are removing
	        String timestampString = StringUtils.substringAfterLast(path, "_");
	        
	        try {
	          String dateFormat = "yyyyMMddHHmmss";
	          DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
	          LocalDateTime.parse(timestampString, formatter);
	          path = StringUtils.substringBeforeLast(path, "_");
	          
	        } catch (DateTimeParseException e) {
	          throw new HpcException("The original path can't be determined for deleted file: " + path, HpcErrorType.DATA_MANAGEMENT_ERROR);
            }
	    }
	  
	    return StringUtils.substringAfter(path, deletedBasePath);
    	    
	}
	
	@Override
	public void addBulkUpdateAuditRecord(String userId, HpcCompoundMetadataQuery query,
			HpcCompoundMetadataQueryType queryType, List<HpcMetadataEntry> metadataEntries) {
		// Input validation.
		if (userId == null || query == null || queryType == null || StringUtils.isBlank(userId)) {
			return;
		}

		for (HpcMetadataEntry entry : metadataEntries) {
			try {
				bulkUpdateAuditDAO.insert(userId, query, queryType, entry.getAttribute(), entry.getValue(),
						Calendar.getInstance());

			} catch (HpcException e) {
				logger.error("Failed to add a bulk update audit record", HpcErrorType.DATABASE_ERROR, e);
			}
		}
	}
	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Generate a metadata query.
	 *
	 * @param attribute The metadata entry attribute.
	 * @param operator  The query operator.
	 * @param value     The metadata entry value.
	 * @return HpcMetadataEntry instance
	 */
	private HpcMetadataQuery toMetadataQuery(String attribute, HpcMetadataQueryOperator operator, String value) {
		HpcMetadataQuery query = new HpcMetadataQuery();
		query.setAttribute(attribute);
		query.setOperator(operator);
		query.setValue(value);

		return query;
	}

	/**
	 * Validate that the subject is not system account. A system account is either
	 * the HPC system account or other 'system admin' accounts that are configured
	 * to disallow permission change.
	 *
	 * @param subjectPermission The permission request.
	 * @throws HpcException If the request is to change permission of a system
	 *                      account
	 */
	private void validatePermissionRequest(HpcSubjectPermission subjectPermission) throws HpcException {
		HpcIntegratedSystemAccount dataManagementAccount = systemAccountLocator
				.getSystemAccount(HpcIntegratedSystem.IRODS);
		if (dataManagementAccount == null) {
			throw new HpcException("System Data Management Account not configured", HpcErrorType.UNEXPECTED_ERROR);
		}

		String subject = subjectPermission.getSubject();
		if (subject.equals(dataManagementAccount.getUsername()) || systemAdminSubjects.contains(subject)) {
			throw new HpcException("Changing permission of admin account/group is not allowed: " + subject,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
	}

	/**
	 * Validate a data object registration request (that is part of bulk
	 * registration)
	 *
	 * @param registrationRequest The registration request.
	 * @param path                The registration path.
	 * @throws HpcException If the request is invalid.
	 */
	private void validateDataObjectRegistrationRequest(HpcDataObjectRegistrationRequest registrationRequest,
			String path) throws HpcException {
		// Validate exactly one upload source provided - Globus, S3 or a link.
		int uploadSourceCount = 0;
		if (registrationRequest.getGlobusUploadSource() != null) {
			uploadSourceCount++;
		}
		if (registrationRequest.getS3UploadSource() != null) {
			uploadSourceCount++;
		}
		if (registrationRequest.getGoogleDriveUploadSource() != null) {
			uploadSourceCount++;
		}
		if (registrationRequest.getGoogleCloudStorageUploadSource() != null) {
			uploadSourceCount++;
		}
		if (registrationRequest.getFileSystemUploadSource() != null) {
			uploadSourceCount++;
		}
		if (registrationRequest.getLinkSourcePath() != null) {
			uploadSourceCount++;
		}
		if (registrationRequest.getArchiveLinkSource() != null) {
			uploadSourceCount++;
		}
		if (uploadSourceCount > 1) {
			throw new HpcException(
					"Multiple (Globus/S3/Google Drive/Google Storage/File System/Link) upload source provided for: "
							+ path,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (uploadSourceCount == 0) {
			throw new HpcException(
					"No Globus/S3/Google Drive/Google Storage/File System/Link/Archive Link upload source provided for: "
							+ path,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (registrationRequest.getGlobusUploadSource() != null
				&& !isValidFileLocation(registrationRequest.getGlobusUploadSource().getSourceLocation())) {
			throw new HpcException("Invalid Globus upload source in registration request for: " + path,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (registrationRequest.getS3UploadSource() != null) {
			if (!isValidFileLocation(registrationRequest.getS3UploadSource().getSourceLocation())) {
				throw new HpcException("Invalid S3 upload source in registration request for: " + path,
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			HpcDomainValidationResult validationResult = isValidS3Account(
					registrationRequest.getS3UploadSource().getAccount());
			if (!validationResult.getValid()) {
				throw new HpcException("Invalid S3 account [ " + validationResult.getMessage()
						+ "] in registration request for: " + path, HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		if (registrationRequest.getGoogleDriveUploadSource() != null) {
			if (!isValidFileLocation(registrationRequest.getGoogleDriveUploadSource().getSourceLocation())) {
				throw new HpcException("Invalid Google Drive upload source in registration request for: " + path,
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (StringUtils.isEmpty(registrationRequest.getGoogleDriveUploadSource().getAccessToken())) {
				throw new HpcException("Invalid Google Drive token in registration request for: " + path,
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		if (registrationRequest.getGoogleCloudStorageUploadSource() != null) {
			if (!isValidFileLocation(registrationRequest.getGoogleCloudStorageUploadSource().getSourceLocation())) {
				throw new HpcException(
						"Invalid Google Cloud Storage upload source in registration request for: " + path,
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (StringUtils.isEmpty(registrationRequest.getGoogleCloudStorageUploadSource().getAccessToken())) {
				throw new HpcException("Invalid Google Cloud Storage token in registration request for: " + path,
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		if (registrationRequest.getFileSystemUploadSource() != null
				&& !isValidFileLocation(registrationRequest.getFileSystemUploadSource().getSourceLocation())) {
			throw new HpcException("Invalid File System upload source in registration request for: " + path,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (registrationRequest.getArchiveLinkSource() != null) {
			if (!isValidFileLocation(registrationRequest.getArchiveLinkSource().getSourceLocation())) {
				throw new HpcException("Invalid archive link location in registration request for: " + path,
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			if (getDataManagementConfiguration(findDataManagementConfigurationId(path)).getArchiveDataTransferType()
					.equals(HpcDataTransferType.S_3)) {
				if (StringUtils.isEmpty(registrationRequest.getS3ArchiveConfigurationId())) {
					throw new HpcException("Empty s3ArchiveConfigurationId in registration request for: " + path,
							HpcErrorType.INVALID_REQUEST_INPUT);
				}

				String s3ArchiveConfigurationFileContainerId = getS3ArchiveConfiguration(
						registrationRequest.getS3ArchiveConfigurationId()).getBaseArchiveDestination().getFileLocation()
						.getFileContainerId();
				if (!registrationRequest.getArchiveLinkSource().getSourceLocation().getFileContainerId()
						.equals(s3ArchiveConfigurationFileContainerId)) {
					throw new HpcException("The archive link source bucket ["
							+ registrationRequest.getArchiveLinkSource().getSourceLocation().getFileContainerId()
							+ "] doesn't match the s3ArchiveConfiguration bucket ["
							+ s3ArchiveConfigurationFileContainerId + "]", HpcErrorType.INVALID_REQUEST_INPUT);
				}
			}
		}
	}

	/**
	 * Map a registration request to upload method.
	 *
	 * @param registrationRequest The registration request.
	 * @return the upload method
	 */
	private HpcDataTransferUploadMethod toDataTransferUploadMethod(
			HpcDataObjectRegistrationRequest registrationRequest) {
		if (registrationRequest.getFileSystemUploadSource() != null) {
			return HpcDataTransferUploadMethod.FILE_SYSTEM;
		}

		if (registrationRequest.getGlobusUploadSource() != null) {
			return HpcDataTransferUploadMethod.GLOBUS;
		}

		if (registrationRequest.getGoogleCloudStorageUploadSource() != null) {
			return HpcDataTransferUploadMethod.GOOGLE_CLOUD_STORAGE;
		}

		if (registrationRequest.getGoogleDriveUploadSource() != null) {
			return HpcDataTransferUploadMethod.GOOGLE_DRIVE;
		}

		if (registrationRequest.getS3UploadSource() != null) {
			return HpcDataTransferUploadMethod.S_3;
		}

		if (registrationRequest.getArchiveLinkSource() != null) {
			return HpcDataTransferUploadMethod.ARCHIVE_LINK;
		}

		return null;

	}

	private void copyCollection(Object authenticatedToken, String sourcePath, String destinationPath)
			throws HpcException {
		// Validate the destination parent path exists.
		// If it doesn't exist, create the parent collection.
		String sourceParentPath = sourcePath.substring(0, sourcePath.lastIndexOf('/'));
		String destinationParentPath = destinationPath.substring(0, destinationPath.lastIndexOf('/'));
		HpcPathAttributes destinationParentPathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken,
				destinationParentPath);
		if (!destinationParentPathAttributes.getExists()) {
			// Create destination parent collection recursively. Copy all metadata and
			// permissions
			copyCollection(authenticatedToken, sourceParentPath, destinationParentPath);
		}
		// Create directory
		boolean created = createDirectory(destinationPath);

		// Attach the metadata.
		if (created) {
			boolean copyCompleted = false;
			try {
				// Copy user and extracted and system metadata
				HpcMetadataEntries metadataEntries = metadataService.getCollectionMetadataEntries(sourcePath);
				metadataService.copyMetadataToCollection(destinationPath, metadataEntries.getSelfMetadataEntries());
				copyCompleted = true;
			} finally {
				if (!copyCompleted) {
					// Collection registration failed. Remove it from Data Management.
					delete(destinationPath, true);
				}
			}

		}
	}
}
