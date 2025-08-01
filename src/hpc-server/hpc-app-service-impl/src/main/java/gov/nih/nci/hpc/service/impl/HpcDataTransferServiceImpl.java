/**
 * HpcDataTransferServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidAsperaAccount;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileLocation;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidS3Account;
import static gov.nih.nci.hpc.util.HpcUtil.exec;
import static gov.nih.nci.hpc.util.HpcUtil.fromValue;
import static gov.nih.nci.hpc.util.HpcUtil.toIntExact;
import static gov.nih.nci.hpc.util.HpcUtil.toNormalizedPath;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import gov.nih.nci.hpc.dao.HpcDataDownloadDAO;
import gov.nih.nci.hpc.dao.HpcDataRegistrationDAO;
import gov.nih.nci.hpc.dao.HpcGlobusTransferTaskDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathPermissions;
import gov.nih.nci.hpc.domain.datatransfer.HpcAddArchiveObjectMetadataResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveObjectMetadata;
import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaAccount;
import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcBoxDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTaskStatusFilter;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadMethod;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDeepArchiveStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusTransferItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusTransferRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcGoogleDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcPatternType;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcSetArchiveObjectMetadataResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcStreamingUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcSynchronousDownloadFilter;
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadPartETag;
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcUserDownloadRequest;
import gov.nih.nci.hpc.domain.error.HpcDomainValidationResult;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.model.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.model.HpcDataTransferAuthenticatedToken;
import gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration;
import gov.nih.nci.hpc.domain.model.HpcGlobusTransferTask;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemTokens;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.integration.HpcTransferAcceptanceResponse;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcEventService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcNotificationService;
import gov.nih.nci.hpc.service.HpcSecurityService;
import gov.nih.nci.hpc.util.HpcUtil;

/**
 * HPC Data Transfer Service Implementation.
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 */
public class HpcDataTransferServiceImpl implements HpcDataTransferService {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// Data transfer system generated metadata attributes (attach to files in
	// archive)
	private static final String OBJECT_ID_ATTRIBUTE = "uuid";
	private static final String REGISTRAR_ID_ATTRIBUTE = "user_id";

	// Multiple upload source error message.
	private static final String MULTIPLE_UPLOAD_SOURCE_ERROR_MESSAGE = "Multiple upload source and/or generate upload request provided";

	// Credentials are needed download error message.
	private static final String CREDENTIALS_NEEDED_ERROR_MESSAGE = "Credentials are needed";

	// Google Drive 'My Drive' ID.
	private static final String MY_GOOGLE_DRIVE_ID = "MyDrive";

	// Box 'My Box' ID.
	private static final String MY_BOX_ID = "MyBox";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// Map data transfer type to its proxy impl.
	private Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies = new EnumMap<>(
			HpcDataTransferType.class);

	// Map data object IDs to completion %.
	private Map<String, Integer> dataObjectUploadPercentComplete = new HashMap<>();

	// System Accounts locator.
	@Autowired
	private HpcSystemAccountLocator systemAccountLocator = null;

	// Data object download DAO.
	@Autowired
	private HpcDataDownloadDAO dataDownloadDAO = null;

	// Event service.
	@Autowired
	private HpcEventService eventService = null;

	// Metadata service.
	@Autowired
	private HpcMetadataService metadataService = null;

	// Security service.
	@Autowired
	private HpcSecurityService securityService = null;

	// Notification Application Service.
	@Autowired
	private HpcNotificationService notificationService = null;

	// Data Management Service
	@Autowired
	private HpcDataManagementService dataManagementService = null;

	// Data Registration DAO.
	@Autowired
	private HpcDataRegistrationDAO dataRegistrationDAO = null;

	// Globus Transfer DAO
	@Autowired
	HpcGlobusTransferTaskDAO globusTransferDAO = null;

	// Data management configuration locator.
	@Autowired
	private HpcDataManagementConfigurationLocator dataManagementConfigurationLocator = null;

	// The compressed archive extractor. Used to extract files from TAR/TGZ/ZIP.
	@Autowired
	private HpcCompressedArchiveExtractor compressedArchiveExtractor = null;

	// The pattern convenient class to support string pattern matching
	@Autowired
	private HpcPattern pattern = null;

	// Pagination support.
	@Autowired
	@Qualifier("hpcDownloadResultsPagination")
	private HpcPagination pagination = null;

	// The download directory.
	private String downloadDirectory = null;

	// The max sync download file size.
	@Value("${hpc.service.dataTransfer.maxSyncDownloadFileSize}")
	private Long maxSyncDownloadFileSize = null;

	// The Globus collection download using bunching - on/off toggle.
	@Value("${hpc.service.dataTransfer.globusCollectionDownloadBunching}")
	boolean globusCollectionDownloadBunching = true;

	// cancelCollectionDownloadTaskItems() query filter
	private List<HpcDataObjectDownloadTaskStatusFilter> cancelCollectionDownloadTaskItemsFilter = new ArrayList<>();

	@Value("${hpc.service.dataTransfer.globusTokenExpirationPeriod}")
	private int globusTokenExpirationPeriod = 0;

	// Max downloads that the transfer manager can perform
	@Value("${hpc.service.dataTransfer.maxPermittedS3DownloadsForGlobus}")
	private Integer maxPermittedS3DownloadsForGlobus = null;

	// Max total (active) downloads size allowed per user in GB.
	@Value("${hpc.service.dataTransfer.maxPermittedTotalDownloadsSizePerUser}")
	private Double maxPermittedTotalDownloadsSizePerUser = null;

	// A configured ID representing the server performing a download task.
	@Value("${hpc.service.serverId}")
	private String s3DownloadTaskServerId = null;

	// List of authenticated tokens
	private List<HpcDataTransferAuthenticatedToken> dataTransferAuthenticatedTokens = new ArrayList<>();

	// Date formatter to format files last-modified date
	private DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 *
	 * @param dataTransferProxies The data transfer proxies.
	 * @param downloadDirectory   The download directory.
	 * @throws HpcException on spring configuration error.
	 */
	public HpcDataTransferServiceImpl(Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies,
			String downloadDirectory) throws HpcException {
		if (dataTransferProxies == null || dataTransferProxies.isEmpty() || StringUtils.isEmpty(downloadDirectory)) {
			throw new HpcException("Null or empty map of data transfer proxies, or download directory",
					HpcErrorType.SPRING_CONFIGURATION_ERROR);
		}

		this.dataTransferProxies.putAll(dataTransferProxies);
		this.downloadDirectory = downloadDirectory;

		// Instantiate the cancel collection download items query filter.
		// We cancel any collection task item that is in RECEIVED state, or a task that
		// is IN_PROGRESS to GLOBUS or S3 destinations.
		HpcDataObjectDownloadTaskStatusFilter filter = new HpcDataObjectDownloadTaskStatusFilter();
		filter.setStatus(HpcDataTransferDownloadStatus.RECEIVED);
		filter.setDestination(HpcDataTransferType.GLOBUS);
		cancelCollectionDownloadTaskItemsFilter.add(filter);

		filter = new HpcDataObjectDownloadTaskStatusFilter();
		filter.setStatus(HpcDataTransferDownloadStatus.RECEIVED);
		filter.setDestination(HpcDataTransferType.GOOGLE_DRIVE);
		cancelCollectionDownloadTaskItemsFilter.add(filter);

		filter = new HpcDataObjectDownloadTaskStatusFilter();
		filter.setStatus(HpcDataTransferDownloadStatus.RECEIVED);
		filter.setDestination(HpcDataTransferType.GOOGLE_CLOUD_STORAGE);
		cancelCollectionDownloadTaskItemsFilter.add(filter);

		filter = new HpcDataObjectDownloadTaskStatusFilter();
		filter.setStatus(HpcDataTransferDownloadStatus.RECEIVED);
		filter.setDestination(HpcDataTransferType.S_3);
		cancelCollectionDownloadTaskItemsFilter.add(filter);

		filter = new HpcDataObjectDownloadTaskStatusFilter();
		filter.setStatus(HpcDataTransferDownloadStatus.IN_PROGRESS);
		filter.setDestination(HpcDataTransferType.GLOBUS);
		cancelCollectionDownloadTaskItemsFilter.add(filter);

		filter = new HpcDataObjectDownloadTaskStatusFilter();
		filter.setStatus(HpcDataTransferDownloadStatus.IN_PROGRESS);
		filter.setDestination(HpcDataTransferType.S_3);
		cancelCollectionDownloadTaskItemsFilter.add(filter);
	}

	/**
	 * Default Constructor.
	 *
	 * @throws HpcException Constructor is disabled.
	 */
	@SuppressWarnings("unused")
	private HpcDataTransferServiceImpl() throws HpcException {
		throw new HpcException("Default Constructor disabled", HpcErrorType.SPRING_CONFIGURATION_ERROR);
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataTransferService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public HpcDataObjectUploadResponse uploadDataObject(HpcUploadSource globusUploadSource,
			HpcStreamingUploadSource s3UploadSource, HpcStreamingUploadSource googleDriveUploadSource,
			HpcStreamingUploadSource googleCloudStorageUploadSource, HpcUploadSource fileSystemUploadSource,
			File sourceFile, boolean generateUploadRequestURL, Integer uploadParts, Boolean uploadCompletion,
			String uploadRequestURLChecksum, String path, String dataObjectId, String userId, String callerObjectId,
			String configurationId, String s3ArchiveConfigurationId) throws HpcException {
		// Input Validation. One and only one of the first 6 parameters is expected to
		// be provided.

		// Validate data-object-id provided.
		if (StringUtils.isEmpty(dataObjectId)) {
			throw new HpcException("No data object ID in upload request", HpcErrorType.UNEXPECTED_ERROR);
		}

		// Validate an upload source was provided.
		if (globusUploadSource == null && s3UploadSource == null && googleDriveUploadSource == null
				&& googleCloudStorageUploadSource == null && fileSystemUploadSource == null && sourceFile == null
				&& !generateUploadRequestURL) {
			throw new HpcException("No data transfer source or data attachment provided or upload URL requested",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the S3 archive configuration ID.
		if (!StringUtils.isEmpty(s3ArchiveConfigurationId)) {
			HpcDataTransferConfiguration s3ArchiveConfiguration = dataManagementConfigurationLocator
					.getS3ArchiveConfiguration(s3ArchiveConfigurationId);
			if (!configurationId.equals(s3ArchiveConfiguration.getDataManagementConfigurationId())) {
				throw new HpcException(
						"S3 archive configuration ID [" + s3ArchiveConfigurationId + "] is associated with config ID ["
								+ s3ArchiveConfiguration.getDataManagementConfigurationId()
								+ "] and can't be used w/ config ID [" + configurationId + "]",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate Globus upload source.
		if (globusUploadSource != null) {
			if (s3UploadSource != null || googleDriveUploadSource != null || googleCloudStorageUploadSource != null
					|| fileSystemUploadSource != null || sourceFile != null || generateUploadRequestURL) {
				throw new HpcException(MULTIPLE_UPLOAD_SOURCE_ERROR_MESSAGE, HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (!isValidFileLocation(globusUploadSource.getSourceLocation())) {
				throw new HpcException("Invalid Globus upload source", HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate S3 upload source.
		if (s3UploadSource != null) {
			if (googleDriveUploadSource != null || googleCloudStorageUploadSource != null
					|| fileSystemUploadSource != null || sourceFile != null || generateUploadRequestURL) {
				throw new HpcException(MULTIPLE_UPLOAD_SOURCE_ERROR_MESSAGE, HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (!isValidFileLocation(s3UploadSource.getSourceLocation())) {
				throw new HpcException("Invalid S3 upload source location", HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (StringUtils.isEmpty(s3UploadSource.getSourceURL())) {
				HpcDomainValidationResult validationResult = isValidS3Account(s3UploadSource.getAccount());
				if (!validationResult.getValid()) {
					throw new HpcException("Invalid S3 account: " + validationResult.getMessage(),
							HpcErrorType.INVALID_REQUEST_INPUT);
				}
			}
			if (!StringUtils.isEmpty(s3UploadSource.getSourceURL()) && s3UploadSource.getSourceSize() == null) {
				throw new HpcException("AWS S3 source URL provided without source size",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (!StringUtils.isEmpty(s3UploadSource.getAccessToken())) {
				throw new HpcException("Google Drive access token provided in AWS S3 upload source location",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate Google Drive upload source.
		if (googleDriveUploadSource != null) {
			if (googleCloudStorageUploadSource != null || fileSystemUploadSource != null || sourceFile != null
					|| generateUploadRequestURL) {
				throw new HpcException(MULTIPLE_UPLOAD_SOURCE_ERROR_MESSAGE, HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (!isValidFileLocation(googleDriveUploadSource.getSourceLocation())) {
				throw new HpcException("Invalid Google Drive upload source location",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (!googleDriveUploadSource.getSourceLocation().getFileContainerId().equals(MY_GOOGLE_DRIVE_ID)) {
				// At this point we only support upload from personal drive and expect the
				// container ID to
				// be "MyDrive"
				throw new HpcException("Invalid file container ID. Only 'MyDrive' is supported",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (StringUtils.isEmpty(googleDriveUploadSource.getAccessToken())) {
				String googleAccessToken = dataRegistrationDAO.getGoogleAccessToken(dataObjectId);
				if (googleAccessToken != null) {
					// We are restarting an upload from Google Drive after server restarted.
					// Populate the access token from DB.
					googleDriveUploadSource.setAccessToken(googleAccessToken);
				}
			}
			if (StringUtils.isEmpty(googleDriveUploadSource.getAccessToken())) {
				throw new HpcException("Invalid Google Drive access token", HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (googleDriveUploadSource.getAccount() != null) {
				throw new HpcException("S3 account provided in Google Drive upload source location",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate Google Cloud storage upload source.
		if (googleCloudStorageUploadSource != null) {
			if (fileSystemUploadSource != null || sourceFile != null || generateUploadRequestURL) {
				throw new HpcException(MULTIPLE_UPLOAD_SOURCE_ERROR_MESSAGE, HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (!isValidFileLocation(googleCloudStorageUploadSource.getSourceLocation())) {
				throw new HpcException("Invalid Google Cloud Storage upload source location",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (StringUtils.isEmpty(googleCloudStorageUploadSource.getAccessToken())) {
				String googleAccessToken = dataRegistrationDAO.getGoogleAccessToken(dataObjectId);
				if (googleAccessToken != null) {
					// If we are restarting an upload from Google Cloud Storage after server
					// restarted.
					// Populate the access token from DB.
					googleCloudStorageUploadSource.setAccessToken(googleAccessToken);
				}
			}
			if (StringUtils.isEmpty(googleCloudStorageUploadSource.getAccessToken())) {
				throw new HpcException("Invalid Google Cloud Storage access token", HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (googleCloudStorageUploadSource.getAccount() != null) {
				throw new HpcException("S3 account provided in Google Cloud Storage upload source location",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate File System upload source.
		if (fileSystemUploadSource != null) {
			if (sourceFile != null || generateUploadRequestURL) {
				throw new HpcException(MULTIPLE_UPLOAD_SOURCE_ERROR_MESSAGE, HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (!isValidFileLocation(fileSystemUploadSource.getSourceLocation())) {
				throw new HpcException("Invalid File System upload source location",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate source file.
		if (sourceFile != null && generateUploadRequestURL) {
			throw new HpcException(MULTIPLE_UPLOAD_SOURCE_ERROR_MESSAGE, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate generate upload request URL.
		if (generateUploadRequestURL) {
			if (uploadParts != null && uploadParts < 1) {
				throw new HpcException("Invalid upload parts: " + uploadParts, HpcErrorType.INVALID_REQUEST_INPUT);
			}
		} else if (uploadParts != null || uploadCompletion != null) {
			throw new HpcException("Upload parts or completion provided w/o request to generate upload URL",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate source location exists and accessible.
		HpcPathAttributes pathAttributes = validateUploadSourceFileLocation(globusUploadSource, s3UploadSource,
				googleDriveUploadSource, googleCloudStorageUploadSource, fileSystemUploadSource, sourceFile,
				configurationId);

		// Create an upload request.
		HpcDataObjectUploadRequest uploadRequest = new HpcDataObjectUploadRequest();
		uploadRequest.setPath(path);
		uploadRequest.setDataObjectId(dataObjectId);
		uploadRequest.setUserId(userId);
		uploadRequest.setCallerObjectId(callerObjectId);
		uploadRequest.setGlobusUploadSource(globusUploadSource);
		uploadRequest.setS3UploadSource(s3UploadSource);
		uploadRequest.setGoogleDriveUploadSource(googleDriveUploadSource);
		uploadRequest.setGoogleCloudStorageUploadSource(googleCloudStorageUploadSource);
		uploadRequest.setFileSystemUploadSource(fileSystemUploadSource);
		if (sourceFile != null) {
			uploadRequest.setSourceFile(sourceFile);
			HpcIntegratedSystemAccount systemAccount = systemAccountLocator.getSystemAccount(HpcIntegratedSystem.IRODS);
			uploadRequest.setSystemAccountName(systemAccount.getUsername());
			uploadRequest.setSudoPassword(systemAccount.getPassword());
		}
		uploadRequest.setUploadRequestURLChecksum(uploadRequestURLChecksum);
		uploadRequest.setGenerateUploadRequestURL(generateUploadRequestURL);
		uploadRequest.setUploadParts(uploadParts);
		uploadRequest.setUploadCompletion(uploadCompletion);
		if (pathAttributes != null) {
			uploadRequest.setSourceSize(pathAttributes.getSize());
			uploadRequest.setSourcePermissions(pathAttributes.getPermissions());
		}

		// Upload the data object file.
		return uploadDataObject(uploadRequest, configurationId, s3ArchiveConfigurationId);
	}

	@Override
	public String completeMultipartUpload(HpcFileLocation archiveLocation, HpcDataTransferType dataTransferType,
			String configurationId, String s3ArchiveConfigurationId, String multipartUploadId,
			List<HpcUploadPartETag> uploadPartETags) throws HpcException {
		// Input Validation.
		if (dataTransferType == null || !isValidFileLocation(archiveLocation)) {
			throw new HpcException("Invalid archive location or data-transfer-type",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (StringUtils.isEmpty(multipartUploadId) || uploadPartETags.size() < 2) {
			throw new HpcException("Empty multipartUploadId or less than 2 part ETags provided",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		for (HpcUploadPartETag uploadPartETag : uploadPartETags) {
			if (StringUtils.isEmpty(uploadPartETag.getETag())) {
				throw new HpcException("Empty / null eTag for part number " + uploadPartETag.getPartNumber(),
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Complete the multipart upload.
		return dataTransferProxies.get(dataTransferType).completeMultipartUpload(
				getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId), archiveLocation,
				multipartUploadId, uploadPartETags);
	}

	@Override
	public void updateDataObjectUploadProgress(String dataObjectId, int percentComplete) {
		if (!StringUtils.isEmpty(dataObjectId) && percentComplete >= 0 && percentComplete <= 100) {
			dataObjectUploadPercentComplete.put(dataObjectId, percentComplete);
		}
	}

	@Override
	public Integer getDataObjectUploadProgress(HpcSystemGeneratedMetadata systemGeneratedMetadata) {
		return systemGeneratedMetadata.getDataTransferStatus() != null
				&& systemGeneratedMetadata.getDataTransferStatus().equals(HpcDataTransferUploadStatus.ARCHIVED)
						? null
						: Optional
								.ofNullable(dataObjectUploadPercentComplete.get(systemGeneratedMetadata.getObjectId()))
								.orElse(0);
	}

	@Override
	public HpcDataObjectDownloadResponse downloadDataObject(String path, HpcFileLocation archiveLocation,
			HpcGlobusDownloadDestination globusDownloadDestination, HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDownloadDestination googleDriveDownloadDestination,
			HpcGoogleDownloadDestination googleCloudStorageDownloadDestination,
			HpcAsperaDownloadDestination asperaDownloadDestination, HpcBoxDownloadDestination boxDownloadDestination,
			HpcSynchronousDownloadFilter synchronousDownloadFilter, HpcDataTransferType dataTransferType,
			String configurationId, String s3ArchiveConfigurationId, String retryTaskId, String userId,
			String retryUserId, boolean completionEvent, String collectionDownloadTaskId, long size,
			HpcDataTransferUploadStatus dataTransferStatus, HpcDeepArchiveStatus deepArchiveStatus)
			throws HpcException {
		// Input Validation.
		if (dataTransferType == null || !isValidFileLocation(archiveLocation)) {
			throw new HpcException("Invalid data transfer request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the destination.
		validateDownloadDestination(globusDownloadDestination, s3DownloadDestination, googleDriveDownloadDestination,
				googleCloudStorageDownloadDestination, asperaDownloadDestination, boxDownloadDestination,
				synchronousDownloadFilter, configurationId, false);

		// Create a download request.
		HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
		downloadRequest.setDataTransferType(dataTransferType);
		downloadRequest.setArchiveLocation(archiveLocation);
		downloadRequest.setGlobusDestination(globusDownloadDestination);
		downloadRequest.setS3Destination(s3DownloadDestination);
		downloadRequest.setGoogleDriveDestination(googleDriveDownloadDestination);
		downloadRequest.setGoogleCloudStorageDestination(googleCloudStorageDownloadDestination);
		downloadRequest.setAsperaDestination(asperaDownloadDestination);
		downloadRequest.setBoxDestination(boxDownloadDestination);
		downloadRequest.setPath(path);
		downloadRequest.setConfigurationId(configurationId);
		downloadRequest.setS3ArchiveConfigurationId(s3ArchiveConfigurationId);
		downloadRequest.setRetryTaskId(retryTaskId);
		downloadRequest.setUserId(userId);
		downloadRequest.setRetryUserId(retryUserId);
		downloadRequest.setCompletionEvent(completionEvent);
		downloadRequest.setCollectionDownloadTaskId(collectionDownloadTaskId);
		downloadRequest.setSize(size);

		// Create a download response.
		HpcDataObjectDownloadResponse response = new HpcDataObjectDownloadResponse();

		// Get the base archive destination.
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);

		// There are 8 methods of downloading data object:
		// 1. Data is in deep archive, restoration is required. Supported by
		// S3 (Cloudian)
		// archive.
		// 2. Synchronous download via REST API. Supported by S3 & POSIX
		// archives.
		// 3. Asynchronous download using Globus. Supported by S3 (in a 2-hop
		// solution) & POSIX archives.
		// 4. Asynchronous download via streaming data object from S3 Archive to user
		// provided S3 bucket. Supported by S3 archive only.
		// 5. Asynchronous download via streaming data object from S3 Archive to user
		// provided Google Drive. Supported by S3 archive only.
		// 6. Asynchronous download via streaming data object from S3 Archive to user
		// provided Google Cloud Storage. Supported by S3 archive only.
		// 7. Asynchronous 2-hop download using Aspera Connect (CLI). Supported by S3
		// archive only.
		// 8. Asynchronous download via streaming data object from S3 Archive to user
		// provided Box. Supported by S3 archive only.
		if (deepArchiveStatus != null) {
			// If status is DEEP_ARCHIVE, and object is not restored, submit a restore
			// request
			// and create a dataObjectDownloadTask with status RESTORE_REQUESTED
			HpcArchiveObjectMetadata objectMetadata = dataTransferProxies.get(HpcDataTransferType.S_3)
					.getDataObjectMetadata(
							getAuthenticatedToken(HpcDataTransferType.S_3, downloadRequest.getConfigurationId(),
									downloadRequest.getS3ArchiveConfigurationId()),
							downloadRequest.getArchiveLocation());

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
				performObjectRestore(downloadRequest, response, restorationStatus);
				return response;
			}
		}

		if (globusDownloadDestination == null && s3DownloadDestination == null && googleDriveDownloadDestination == null
				&& googleCloudStorageDownloadDestination == null && asperaDownloadDestination == null
				&& boxDownloadDestination == null) {
			// This is a synchronous download request.
			performSynchronousDownload(downloadRequest, response, dataTransferConfiguration, synchronousDownloadFilter);

		} else if (dataTransferType.equals(HpcDataTransferType.GLOBUS) && globusDownloadDestination != null) {
			// This is an asynchronous download request from a POSIX archive to a
			// Globus destination.
			// Note: this can also be a 2nd hop download from temporary file-system archive
			// to a Globus destination (after the 1st hop completed).
			performGlobusAsynchronousDownload(downloadRequest, response, dataTransferConfiguration);

		} else if (dataTransferType.equals(HpcDataTransferType.S_3)
				&& (globusDownloadDestination != null || asperaDownloadDestination != null)) {
			// This is an asynchronous download request from a S3 archive to a
			// Globus or Aspera destination. It is performed in 2-hops.
			perform2HopDownload(downloadRequest, response, dataTransferConfiguration);

		} else if (s3DownloadDestination != null) {
			// This is an asynchronous download request from a S3 / POSIX archive to a user
			// provided
			// S3 destination.
			// Note: The user S3 destination can be either AWS or 3rd Party S3 provider.
			performS3AsynchronousDownload(downloadRequest, dataTransferType, response, dataTransferConfiguration);

		} else if (dataTransferType.equals(HpcDataTransferType.S_3) && googleDriveDownloadDestination != null) {
			// This is an asynchronous download request from a S3 archive to a
			// Google Drive destination.
			performGoogleDriveAsynchronousDownload(downloadRequest, response, dataTransferConfiguration);

		} else if (dataTransferType.equals(HpcDataTransferType.S_3) && googleCloudStorageDownloadDestination != null) {
			// This is an asynchronous download request from a S3 archive to a Google Cloud
			// Storage destination.
			performGoogleCloudStorageAsynchronousDownload(downloadRequest, response, dataTransferConfiguration);

		} else if (dataTransferType.equals(HpcDataTransferType.S_3) && boxDownloadDestination != null) {
			// This is an asynchronous download request from a S3 archive to a Box
			// destination.
			performBoxAsynchronousDownload(downloadRequest, response, dataTransferConfiguration);

		} else

		{
			throw new HpcException("Invalid download request", HpcErrorType.UNEXPECTED_ERROR);
		}

		return response;
	}

	@Override
	public String generateDownloadRequestURL(String path, String userId, HpcFileLocation archiveLocation,
			HpcDataTransferType dataTransferType, long size, String configurationId, String s3ArchiveConfigurationId)
			throws HpcException {
		// Generate the download URL.
		String downloadRequestURL = generateDownloadRequestURL(path, archiveLocation, dataTransferType, configurationId,
				s3ArchiveConfigurationId);

		// Create a task result object.
		HpcDownloadTaskResult taskResult = new HpcDownloadTaskResult();
		Calendar now = Calendar.getInstance();
		taskResult.setId(UUID.randomUUID().toString());
		taskResult.setUserId(userId);
		taskResult.setPath(path);
		HpcFileLocation destinationLocation = new HpcFileLocation();
		destinationLocation.setFileContainerId("Presigned Download URL");
		destinationLocation.setFileId("");
		taskResult.setDestinationLocation(destinationLocation);
		taskResult.setArchiveLocation(archiveLocation);
		taskResult.setResult(HpcDownloadResult.COMPLETED);
		taskResult.setType(HpcDownloadTaskType.DATA_OBJECT);
		taskResult.setCompletionEvent(false);
		taskResult.setCreated(now);
		taskResult.setSize(size);
		taskResult.setCompleted(now);
		taskResult.setDoc(dataManagementService.getDataManagementConfiguration(configurationId).getDoc());

		// Persist to the DB.
		dataDownloadDAO.upsertDownloadTaskResult(taskResult);

		return downloadRequestURL;
	}

	@Override
	public HpcAddArchiveObjectMetadataResponse addSystemGeneratedMetadataToDataObject(HpcFileLocation fileLocation,
			HpcDataTransferType dataTransferType, String configurationId, String s3ArchiveConfigurationId,
			String objectId, String registrarId) throws HpcException {
		// Add metadata is done by copying the object to itself w/ attached metadata.
		// Check that the data transfer system can accept transfer requests.
		boolean globusSyncUpload = dataTransferType.equals(HpcDataTransferType.GLOBUS) && fileLocation != null;

		// Get the data transfer configuration.
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);

		HpcAddArchiveObjectMetadataResponse response = new HpcAddArchiveObjectMetadataResponse();

		// Set the metadata of the data-object in the archive
		HpcSetArchiveObjectMetadataResponse setMetadataResponse = dataTransferProxies.get(dataTransferType)
				.setDataObjectMetadata(
						!globusSyncUpload
								? getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId)
								: null,
						fileLocation, dataTransferConfiguration.getBaseArchiveDestination(),
						generateArchiveMetadata(configurationId, objectId, registrarId),
						dataTransferConfiguration.getStorageClass());

		HpcArchiveObjectMetadata objectMetadata = new HpcArchiveObjectMetadata();
		objectMetadata.setChecksum(setMetadataResponse.getChecksum());

		if (dataTransferType.equals(HpcDataTransferType.S_3)) {
			if (dataTransferProxies.get(dataTransferType).existsTieringPolicy(
					getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId), fileLocation)) {
				// Add deep_archive_status in progress
				objectMetadata.setDeepArchiveStatus(HpcDeepArchiveStatus.IN_PROGRESS);
			} else {
				HpcDeepArchiveStatus deepArchiveStatus = fromValue(HpcDeepArchiveStatus.class,
						dataTransferConfiguration.getStorageClass());
				if (deepArchiveStatus != null) {
					// Add deep_archive_status set to the storage class of the archive
					objectMetadata.setDeepArchiveStatus(deepArchiveStatus);
				}
			}
		}

		response.setArchiveObjectMetadata(objectMetadata);
		response.setMetadataAdded(setMetadataResponse.getMetadataAdded());
		return response;
	}

	@Override
	public void deleteDataObject(HpcFileLocation fileLocation, HpcDataTransferType dataTransferType,
			String configurationId, String s3ArchiveConfigurationId) throws HpcException {
		// Input validation.
		if (!HpcDomainValidator.isValidFileLocation(fileLocation)) {
			throw new HpcException("Invalid file location", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		dataTransferProxies.get(dataTransferType).deleteDataObject(
				getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId), fileLocation,
				dataManagementConfigurationLocator
						.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType)
						.getBaseArchiveDestination());
	}

	@Override
	public HpcDataTransferUploadReport getDataTransferUploadStatus(HpcDataTransferType dataTransferType,
			String dataTransferRequestId, String configurationId, String s3ArchiveConfigurationId, String loggingPrefix)
			throws HpcException {
		// Input validation.
		if (dataTransferRequestId == null) {
			throw new HpcException("Null data transfer request ID", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the data transfer configuration.
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);

		return dataTransferProxies.get(dataTransferType).getDataTransferUploadStatus(
				getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId),
				dataTransferRequestId, dataTransferConfiguration.getBaseArchiveDestination(), loggingPrefix);
	}

	@Override
	public HpcDataTransferDownloadReport getDataTransferDownloadStatus(HpcDataTransferType dataTransferType,
			String dataTransferRequestId, String configurationId, String s3ArchiveConfigurationId,
			boolean successfulItems, String loggingPrefix) throws HpcException {
		// Input validation.
		if (dataTransferRequestId == null) {
			throw new HpcException("Null data transfer request ID", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return dataTransferProxies.get(dataTransferType).getDataTransferDownloadStatus(
				getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId),
				dataTransferRequestId, successfulItems, loggingPrefix);
	}

	@Override
	public HpcPathAttributes getPathAttributes(HpcDataTransferType dataTransferType, HpcFileLocation fileLocation,
			boolean getSize, String configurationId, String s3ArchiveConfigurationId) throws HpcException {
		// Input validation.
		if (!HpcDomainValidator.isValidFileLocation(fileLocation)) {
			throw new HpcException("Invalid file location", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return dataTransferProxies.get(dataTransferType).getPathAttributes(
				getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId), fileLocation,
				getSize);
	}

	@Override
	public HpcPathAttributes getPathAttributes(HpcS3Account s3Account, HpcFileLocation fileLocation, boolean getSize)
			throws HpcException {
		// Input validation.
		if (!isValidFileLocation(fileLocation)) {
			throw new HpcException("Invalid file location", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcDomainValidationResult validationResult = isValidS3Account(s3Account);
		if (!validationResult.getValid()) {
			throw new HpcException("Invalid S3 account: " + validationResult.getMessage(),
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// This is S3 only functionality, so we get the S3 data-transfer-proxy.
		HpcDataTransferProxy dataTransferProxy = dataTransferProxies.get(HpcDataTransferType.S_3);
		try {
			return dataTransferProxy.getPathAttributes(dataTransferProxy.authenticate(s3Account), fileLocation,
					getSize);

		} catch (HpcException e) {
			throw new HpcException("Failed to access AWS S3 bucket: [" + e.getMessage() + "] " + fileLocation,
					HpcErrorType.INVALID_REQUEST_INPUT, e);
		}
	}

	@Override
	public HpcPathAttributes getPathAttributes(HpcDataTransferType dataTransferType, String accessToken,
			HpcFileLocation fileLocation, boolean getSize) throws HpcException {
		// Input validation.
		if (!dataTransferType.equals(HpcDataTransferType.GOOGLE_CLOUD_STORAGE)
				&& !dataTransferType.equals(HpcDataTransferType.GOOGLE_DRIVE)) {
			throw new HpcException("Unexpected data transfer type: " + dataTransferType.value(),
					HpcErrorType.UNEXPECTED_ERROR);
		}
		if (!isValidFileLocation(fileLocation)) {
			throw new HpcException("Invalid file location", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (StringUtils.isEmpty(accessToken)) {
			throw new HpcException("Invalid Google Drive / Google Cloud storage access token",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcDataTransferProxy dataTransferProxy = dataTransferProxies.get(dataTransferType);
		try {
			return dataTransferProxy.getPathAttributes(dataTransferProxy.authenticate(accessToken), fileLocation,
					getSize);

		} catch (HpcException e) {
			throw new HpcException(
					"Failed to access " + dataTransferType.value() + ": [" + e.getMessage() + "] " + fileLocation,
					HpcErrorType.INVALID_REQUEST_INPUT, e);
		}
	}

	@Override
	public HpcPathAttributes getPathAttributes(HpcFileLocation fileLocation) throws HpcException {
		// Input validation.
		if (!isValidFileLocation(fileLocation)) {
			throw new HpcException("Invalid file location", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return HpcUtil.getPathAttributes(fileLocation);
	}

	@Override
	public List<HpcDirectoryScanItem> scanDirectory(HpcDataTransferType dataTransferType, HpcS3Account s3Account,
			String googleAccessToken, HpcFileLocation directoryLocation, String configurationId,
			String s3ArchiveConfigurationId, List<String> includePatterns, List<String> excludePatterns,
			HpcPatternType patternType) throws HpcException {
		// Input validation.
		if (!HpcDomainValidator.isValidFileLocation(directoryLocation)) {
			throw new HpcException("Invalid directory location", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (dataTransferType == null) {
			if ((!StringUtils.isEmpty(googleAccessToken) || s3Account != null
					|| !StringUtils.isEmpty(s3ArchiveConfigurationId))) {
				throw new HpcException(
						"S3 account / Google Drive / Google Cloud Storage token provided File System scan",
						HpcErrorType.UNEXPECTED_ERROR);
			}
		} else {
			if (!dataTransferType.equals(HpcDataTransferType.S_3) && s3Account != null) {
				throw new HpcException("S3 account provided for Non S3 data transfer", HpcErrorType.UNEXPECTED_ERROR);
			}
			if (!dataTransferType.equals(HpcDataTransferType.GOOGLE_DRIVE)
					&& !dataTransferType.equals(HpcDataTransferType.GOOGLE_CLOUD_STORAGE)
					&& !StringUtils.isEmpty(googleAccessToken)) {
				throw new HpcException(
						"Google access token provided for Non Google Drive  / Cloud Storagedata transfer",
						HpcErrorType.UNEXPECTED_ERROR);
			}
		}
		List<HpcDirectoryScanItem> scanItems = null;
		if (dataTransferType == null) {
			// Scan a directory in local DME server NAS.
			scanItems = scanDirectory(directoryLocation);
		} else {
			// Globus / S3 / Google Drive scan / Google Cloud Storage.

			// If an S3 account or Google Drive access token was provided, then we use it to
			// get authenticated token, otherwise, we use a system account token.
			Object authenticatedToken = null;
			if (s3Account != null) {
				authenticatedToken = dataTransferProxies.get(dataTransferType).authenticate(s3Account);
			} else if (!StringUtils.isEmpty(googleAccessToken)) {
				authenticatedToken = dataTransferProxies.get(dataTransferType).authenticate(googleAccessToken);
			} else {
				authenticatedToken = getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId);
			}

			// Scan the directory to get a list of all files.
			scanItems = dataTransferProxies.get(dataTransferType).scanDirectory(authenticatedToken, directoryLocation);
		}

		// Filter the list based on provided patterns.
		filterScanItems(scanItems, includePatterns, excludePatterns, patternType, dataTransferType);

		return scanItems;
	}

	@Override
	public File getArchiveFile(String configurationId, String s3ArchiveConfigurationId,
			HpcDataTransferType dataTransferType, String fileId) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(fileId)) {
			throw new HpcException("Invalid file id", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the data transfer configuration.
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);

		// Instantiate the file.
		File file = new File(getFilePath(fileId, dataTransferConfiguration.getBaseArchiveDestination()));
		if (!file.exists()) {
			throw new HpcException("Archive file could not be found: " + file.getAbsolutePath(),
					HpcRequestRejectReason.FILE_NOT_FOUND);
		}

		return file;
	}

	@Override
	public List<HpcDataObjectDownloadTask> getDataObjectDownloadTasks() throws HpcException {
		return dataDownloadDAO.getDataObjectDownloadTasks();
	}

	@Override
	public List<HpcDataObjectDownloadTask> getDataObjectDownloadTasksByCollectionDownloadTaskId(String taskId)
			throws HpcException {
		return dataDownloadDAO.getDataObjectDownloadTaskByCollectionDownloadTaskId(taskId);
	}

	@Override
	public List<HpcDataObjectDownloadTask> getNextDataObjectDownloadTask(
			HpcDataTransferDownloadStatus dataTransferStatus, HpcDataTransferType dataTransferType, Date processed)
			throws HpcException {
		return dataDownloadDAO.getNextDataObjectDownloadTask(dataTransferStatus, dataTransferType, processed);
	}

	@Override
	public HpcDownloadTaskStatus getDownloadTaskStatus(String taskId, HpcDownloadTaskType taskType)
			throws HpcException {
		if (taskType == null) {
			throw new HpcException("Null download task type", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcDownloadTaskStatus taskStatus = new HpcDownloadTaskStatus();
		HpcDownloadTaskResult taskResult = dataDownloadDAO.getDownloadTaskResult(taskId, taskType);
		if (taskResult != null) {
			// Task completed or failed. Return the result.
			taskStatus.setInProgress(false);
			taskStatus.setResult(taskResult);
			return taskStatus;
		}

		// Task still in-progress. Return either the data-object or the collection
		// active download task.
		taskStatus.setInProgress(true);

		if (taskType.equals(HpcDownloadTaskType.DATA_OBJECT)) {
			HpcDataObjectDownloadTask task = dataDownloadDAO.getDataObjectDownloadTask(taskId);
			if (task != null) {
				taskStatus.setDataObjectDownloadTask(task);
				return taskStatus;
			}
		}

		if (taskType.equals(HpcDownloadTaskType.COLLECTION) || taskType.equals(HpcDownloadTaskType.DATA_OBJECT_LIST)
				|| taskType.equals(HpcDownloadTaskType.COLLECTION_LIST)) {
			HpcCollectionDownloadTask task = dataDownloadDAO.getCollectionDownloadTask(taskId);
			if (task != null) {
				taskStatus.setCollectionDownloadTask(task);

				return taskStatus;
			}
		}

		// Task not found.
		return null;
	}

	@Override
	public Map<String, HpcDownloadTaskStatus> getDownloadItemsStatus(HpcCollectionDownloadTask downloadTask)
			throws HpcException {
		Map<String, HpcDownloadTaskStatus> downloadItemsStatus = new HashMap<>();

		// Get all the data object download tasks that are in progress.
		dataDownloadDAO.getDataObjectDownloadTaskByCollectionDownloadTaskId(downloadTask.getId())
				.forEach(dataObjectDownloadTask -> {
					HpcDownloadTaskStatus taskStatus = new HpcDownloadTaskStatus();
					taskStatus.setInProgress(true);
					taskStatus.setDataObjectDownloadTask(dataObjectDownloadTask);
					downloadItemsStatus.put(dataObjectDownloadTask.getId(), taskStatus);
				});

		// Get all the data object download tasks that completed but a result was not
		// recorded yet.
		for (HpcCollectionDownloadTaskItem downloadItem : downloadTask.getItems()) {
			if (downloadItem.getResult() == null) {
				String dataObjectDownloadTaskId = downloadItem.getDataObjectDownloadTaskId();
				if (!StringUtils.isEmpty(dataObjectDownloadTaskId)
						&& !downloadItemsStatus.containsKey(dataObjectDownloadTaskId)) {
					HpcDownloadTaskStatus taskStatus = new HpcDownloadTaskStatus();
					HpcDownloadTaskResult taskResult = dataDownloadDAO.getDownloadTaskResult(dataObjectDownloadTaskId,
							HpcDownloadTaskType.DATA_OBJECT);
					if (taskResult != null) {
						// Task completed or failed. Return the result.
						taskStatus.setInProgress(false);
						taskStatus.setResult(taskResult);
						downloadItemsStatus.put(dataObjectDownloadTaskId, taskStatus);
					}
				}
			}
		}

		return downloadItemsStatus;
	}

	@Override
	public boolean getCollectionDownloadTaskCancellationRequested(String taskId) {
		try {
			return dataDownloadDAO.getCollectionDownloadTaskCancellationRequested(taskId);

		} catch (HpcException e) {
			logger.error("Failed to get cancellation request for task ID: " + taskId);
			// If it can not find the collection download task, it is cancelled and removed
			// from the table.
			return true;
		}
	}

	@Override
	public HpcDownloadTaskResult completeDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask,
			HpcDownloadResult result, String message, Calendar completed, long bytesTransferred) throws HpcException {

		// Input validation
		if (downloadTask == null) {
			throw new HpcException("Invalid data object download task", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		logger.info("download task: [taskId={}] - completed - {} [transfer-type={}, destination-type={}]. Message: {}",
				downloadTask.getId(), result.value(), downloadTask.getDataTransferType(),
				downloadTask.getDestinationType(), message);

		// If it's a cancelled data-object download task to a Globus destination,
		// request Globus to
		// cancel.
		if (result.equals(HpcDownloadResult.CANCELED)
				&& downloadTask.getDataTransferType().equals(HpcDataTransferType.GLOBUS)
				&& !StringUtils.isEmpty(downloadTask.getDataTransferRequestId())) {
			try {
				dataTransferProxies.get(downloadTask.getDataTransferType()).cancelTransferRequest(
						getAuthenticatedToken(downloadTask.getDataTransferType(), downloadTask.getConfigurationId(),
								downloadTask.getS3ArchiveConfigurationId()),
						downloadTask.getDataTransferRequestId(), "HPC-DME user requested cancellation");
				logger.info(
						"download task: [taskId={}] - cancelled Globus task [transfer-type={}, destination-type={}]",
						downloadTask.getId(), downloadTask.getDataTransferType(), downloadTask.getDestinationType());

			} catch (HpcException e) {
				logger.error("Failed to cancel Globus task for user[{}], path: {}", downloadTask.getUserId(),
						downloadTask.getPath(), e);
			}
		}

		// If it's a failed data-object download task to a Globus destination,
		// and the error message is credentials are needed, set status so other items
		// can be cancelled.
		if (result.equals(HpcDownloadResult.FAILED)
				&& downloadTask.getDataTransferType().equals(HpcDataTransferType.GLOBUS) && message != null
				&& message.contains(CREDENTIALS_NEEDED_ERROR_MESSAGE)) {
			result = HpcDownloadResult.FAILED_CREDENTIALS_NEEDED;
			message = message + ". Check if guest collection was created on a public endpoint.";
		}

		// Delete the staged download file.
		if (downloadTask.getDownloadFilePath() != null) {
			logger.info("download task: [taskId={}] - Delete file at scratch space: {}", downloadTask.getId(),
					downloadTask.getDownloadFilePath());
			File downloadFile = new File(downloadTask.getDownloadFilePath());
			if (downloadFile.exists() && !FileUtils.deleteQuietly(downloadFile)) {
				logger.error("Failed to delete file: {}", downloadTask.getDownloadFilePath());
			}
		}

		// Create a task result object.
		HpcDownloadTaskResult taskResult = new HpcDownloadTaskResult();
		taskResult.setId(downloadTask.getId());
		taskResult.setUserId(downloadTask.getUserId());
		taskResult.setPath(downloadTask.getPath());
		taskResult.setDataTransferRequestId(downloadTask.getDataTransferRequestId());

		if (!StringUtils.isEmpty(downloadTask.getDownloadFilePath())) {
			securityService.executeAsSystemAccount(Optional.empty(),
					() -> taskResult.setArchiveLocation(getArchiveLocation(downloadTask.getPath())));
		} else {
			taskResult.setArchiveLocation(downloadTask.getArchiveLocation());
		}

		taskResult.setDataTransferType(downloadTask.getDataTransferType());
		taskResult.setDestinationType(downloadTask.getDestinationType());
		if (downloadTask.getGlobusDownloadDestination() != null) {
			taskResult.setDestinationLocation(downloadTask.getGlobusDownloadDestination().getDestinationLocation());
		} else if (downloadTask.getS3DownloadDestination() != null) {
			taskResult.setDestinationLocation(downloadTask.getS3DownloadDestination().getDestinationLocation());
		} else if (downloadTask.getGoogleDriveDownloadDestination() != null) {
			taskResult
					.setDestinationLocation(downloadTask.getGoogleDriveDownloadDestination().getDestinationLocation());
		} else if (downloadTask.getGoogleCloudStorageDownloadDestination() != null) {
			taskResult.setDestinationLocation(
					downloadTask.getGoogleCloudStorageDownloadDestination().getDestinationLocation());
		} else if (downloadTask.getAsperaDownloadDestination() != null) {
			taskResult.setDestinationLocation(downloadTask.getAsperaDownloadDestination().getDestinationLocation());
		} else if (downloadTask.getBoxDownloadDestination() != null) {
			taskResult.setDestinationLocation(downloadTask.getBoxDownloadDestination().getDestinationLocation());
		}
		taskResult.setDestinationType(downloadTask.getDestinationType());
		taskResult.setResult(result);
		taskResult.setType(HpcDownloadTaskType.DATA_OBJECT);
		taskResult.setMessage(message);
		taskResult.setCompletionEvent(downloadTask.getCompletionEvent());
		taskResult.setCollectionDownloadTaskId(downloadTask.getCollectionDownloadTaskId());
		taskResult.setCreated(downloadTask.getCreated());
		taskResult.setSize(downloadTask.getSize());
		taskResult.setCompleted(completed);
		taskResult.setRestoreRequested(downloadTask.getRestoreRequested());
		taskResult.setFirstHopRetried(downloadTask.getFirstHopRetried());
		taskResult.setRetryTaskId(downloadTask.getRetryTaskId());
		taskResult.setRetryUserId(downloadTask.getRetryUserId());

		// Calculate the effective transfer speed (Bytes per second).
		taskResult.setEffectiveTransferSpeed(toIntExact(bytesTransferred * 1000
				/ (taskResult.getCompleted().getTimeInMillis() - taskResult.getCreated().getTimeInMillis())));

		taskResult.setDoc(
				dataManagementService.getDataManagementConfiguration(downloadTask.getConfigurationId()).getDoc());

		// Get the file container name.
		try {
			taskResult.getDestinationLocation().setFileContainerName(
					getFileContainerName(taskResult.getDestinationType(), downloadTask.getConfigurationId(),
							taskResult.getDestinationLocation().getFileContainerId()));

		} catch (HpcException e) {
			logger.error(
					"Failed to get file container name: " + taskResult.getDestinationLocation().getFileContainerId(),
					e);
		}

		// Persist to the DB.
		dataDownloadDAO.upsertDownloadTaskResult(taskResult);

		// Cleanup the DB record.
		dataDownloadDAO.deleteDataObjectDownloadTask(downloadTask.getId());

		// Remove from HPC_GLOBUS_TRANSFER_TASK if Globus request
		if (downloadTask.getDataTransferType().equals(HpcDataTransferType.GLOBUS)
				&& !StringUtils.isEmpty(downloadTask.getDataTransferRequestId()))
			deleteGlobusTransferTask(downloadTask.getDataTransferRequestId());

		// Update the total bytes transferred if this task is part of a bulk download.
		if (result.equals(HpcDownloadResult.COMPLETED)
				&& !StringUtils.isEmpty(downloadTask.getCollectionDownloadTaskId())) {
			dataDownloadDAO.updateTotalBytesTransferred(downloadTask.getCollectionDownloadTaskId(), bytesTransferred);
		}

		return taskResult;
	}

	@Override
	public void completeSynchronousDataObjectDownloadTask(HpcDownloadTaskResult taskResult, HpcDownloadResult result,
			Calendar completed) throws HpcException {
		if (taskResult == null || taskResult.getDestinationType() != null) {
			throw new HpcException("Invalid sync download task-id: " + taskResult.getId(),
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		taskResult.setResult(result);
		taskResult.setCompleted(completed);

		if (result.equals(HpcDownloadResult.COMPLETED) && taskResult.getSize() != null) {
			// Calculate the effective transfer speed (Bytes per second).
			taskResult.setEffectiveTransferSpeed(toIntExact(taskResult.getSize() * 1000
					/ (taskResult.getCompleted().getTimeInMillis() - taskResult.getCreated().getTimeInMillis())));
		}

		dataDownloadDAO.upsertDownloadTaskResult(taskResult);
	}

	@Override
	public boolean continueDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask) throws HpcException {
		// Recreate the download request from the task (that was persisted).
		HpcDataTransferProgressListener progressListener = null;
		HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
		downloadRequest.setArchiveLocation(downloadTask.getArchiveLocation());
		downloadRequest.setCompletionEvent(downloadTask.getCompletionEvent());
		downloadRequest.setCollectionDownloadTaskId(downloadTask.getCollectionDownloadTaskId());
		downloadRequest.setDataTransferType(downloadTask.getDataTransferType());
		downloadRequest.setConfigurationId(downloadTask.getConfigurationId());
		downloadRequest.setS3ArchiveConfigurationId(downloadTask.getS3ArchiveConfigurationId());
		downloadRequest.setPath(downloadTask.getPath());
		downloadRequest.setUserId(downloadTask.getUserId());
		downloadRequest.setGlobusDestination(downloadTask.getGlobusDownloadDestination());
		downloadRequest.setS3Destination(downloadTask.getS3DownloadDestination());
		downloadRequest.setGoogleDriveDestination(downloadTask.getGoogleDriveDownloadDestination());
		downloadRequest.setGoogleCloudStorageDestination(downloadTask.getGoogleCloudStorageDownloadDestination());
		downloadRequest.setAsperaDestination(downloadTask.getAsperaDownloadDestination());
		downloadRequest.setBoxDestination(downloadTask.getBoxDownloadDestination());

		HpcArchive baseArchiveDestination = null;
		Boolean encryptedTransfer = null;
		Object authenticatedToken = null;

		// If this is a download from a POSIX archive to S3 destination, we need to
		// re-generate the
		// URL to the POSIX archive.
		if (downloadTask.getDestinationType().equals(HpcDataTransferType.S_3)
				&& StringUtils.isEmpty(downloadTask.getS3ArchiveConfigurationId())) {
			downloadRequest.setArchiveLocationURL(generateDownloadRequestURL(downloadRequest.getPath(),
					downloadRequest.getArchiveLocation(), HpcDataTransferType.GLOBUS,
					downloadRequest.getConfigurationId(), downloadRequest.getS3ArchiveConfigurationId()));
		} else {
			// For any other download - get the data transfer configuration.
			HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
					.getDataTransferConfiguration(downloadRequest.getConfigurationId(),
							downloadRequest.getS3ArchiveConfigurationId(), downloadRequest.getDataTransferType());
			baseArchiveDestination = dataTransferConfiguration.getBaseArchiveDestination();
			encryptedTransfer = dataTransferConfiguration.getEncryptedTransfer();

			if (downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_DRIVE)
					|| downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_CLOUD_STORAGE)
					|| downloadTask.getDestinationType().equals(HpcDataTransferType.BOX)) {
				// If the destination is Google Drive / Cloud Storage / Box, we need to generate
				// a
				// download URL from the S3 archive.
				downloadRequest.setArchiveLocationURL(generateDownloadRequestURL(downloadRequest.getPath(),
						downloadRequest.getArchiveLocation(), HpcDataTransferType.S_3,
						downloadRequest.getConfigurationId(), downloadRequest.getS3ArchiveConfigurationId()));

			} else if (downloadTask.getDataTransferType().equals(HpcDataTransferType.ASPERA)) {
				// If the destination is Aspera - set the staged file path in the request
				downloadRequest.setArchiveLocationFilePath(downloadTask.getDownloadFilePath());

			} else {
				// Check if transfer requests can be acceptable at this time (Globus only)
				authenticatedToken = getAuthenticatedToken(downloadRequest.getDataTransferType(),
						downloadRequest.getConfigurationId(), downloadRequest.getS3ArchiveConfigurationId());
				// Check if transfer requests can be acceptable at this time.
				if (!acceptsTransferRequests(downloadTask.getDataTransferType(), downloadTask.getConfigurationId(),
						authenticatedToken)) {
					logger.info(
							"download task: [taskId={}] - transfer requests not accepted at this time [transfer-type={}, destination-type={}]",
							downloadTask.getId(), downloadTask.getDataTransferType(),
							downloadTask.getDestinationType());
					return false;
				}
			}
		}

		// If the destination is Globus / Aspera and the data transfer is S3, then we
		// need to
		// restart a 2-hop download.
		if ((downloadTask.getDestinationType().equals(HpcDataTransferType.GLOBUS)
				|| downloadTask.getDestinationType().equals(HpcDataTransferType.ASPERA))
				&& downloadTask.getDataTransferType().equals(HpcDataTransferType.S_3)) {
			// Create a listener that will kick off the 2nd hop when the first one is done,
			// and update the download task accordingly.
			HpcSecondHopDownload secondHopDownload = new HpcSecondHopDownload(downloadTask);
			progressListener = secondHopDownload;

			// Validate that the 2-hop download can be performed at this time.
			HpcCanPerform2HopDownloadResponse canPerfom2HopDownloadResponse = canPerfom2HopDownload(secondHopDownload);
			if (!canPerfom2HopDownloadResponse.value) {
				// Can’t perform the 2-hop download at this time. Reset the task
				resetDataObjectDownloadTask(secondHopDownload.getDownloadTask());
				logger.info(
						"download task: [taskId={}] - 2 Hop download can't be restarted [transfer-type={}, destination-type={},"
								+ " path={}] - {}",
						downloadTask.getId(), downloadTask.getDataTransferType(), downloadTask.getDestinationType(),
						downloadTask.getPath(), canPerfom2HopDownloadResponse.message);
				return false;
			}
			// Set the first hop transfer to be from S3 Archive to the DME server's Globus
			// mounted file system.
			downloadRequest.setArchiveLocation(getArchiveLocation(downloadRequest.getPath()));
			downloadRequest.setFileDestination(secondHopDownload.getSourceFile());
		}

		// If the destination is S3 (AWS or 3rd Party), or Google Drive / Cloud Storage
		// or the data transfer is Aspera, we need to create a progress listener.
		if (downloadTask.getDestinationType().equals(HpcDataTransferType.S_3)
				|| downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_DRIVE)
				|| downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_CLOUD_STORAGE)
				|| downloadTask.getDataTransferType().equals(HpcDataTransferType.ASPERA)
				|| downloadTask.getDataTransferType().equals(HpcDataTransferType.BOX)) {
			// Create a listener that will complete the download task when it is done.
			progressListener = new HpcStreamingDownload(downloadTask, dataDownloadDAO, eventService, this);
		}

		// Submit a data object download request.
		try {
			String dataTransferRequestId = dataTransferProxies.get(downloadRequest.getDataTransferType())
					.downloadDataObject(authenticatedToken, downloadRequest, baseArchiveDestination, progressListener,
							encryptedTransfer);
			downloadTask.setDataTransferRequestId(dataTransferRequestId);

			// If data tranfer type is Globus, add to HPC_GLOBUS_TRANSFER_TASK
			if (downloadTask.getDataTransferType().equals(HpcDataTransferType.GLOBUS)) {
				HpcGlobusTransferTask globusRequest = new HpcGlobusTransferTask();
				globusRequest.setDataTransferRequestId(dataTransferRequestId);
				globusRequest
						.setGlobusAccount(getDataTransferAuthenticatedToken(authenticatedToken).getSystemAccountId());
				globusRequest.setPath(downloadTask.getPath());
				globusRequest.setDownload(true);
				globusTransferDAO.insertRequest(globusRequest);

			}
		} catch (HpcException e) {
			// Failed to submit a transfer request. Cleanup the download task.
			completeDataObjectDownloadTask(downloadTask, HpcDownloadResult.FAILED, e.getMessage(),
					Calendar.getInstance(), 0);
			return true;
		}

		// Persist the download task. Note: In case we used a progress listener - it
		// took care of that already. If the task was cancelled (by another task), we
		// don't persist.
		if (progressListener == null && !Optional
				.ofNullable(dataDownloadDAO.getDataObjectDownloadTaskStatus(downloadTask.getId()))
				.orElse(HpcDataTransferDownloadStatus.CANCELED).equals(HpcDataTransferDownloadStatus.CANCELED)) {
			downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.IN_PROGRESS);
			dataDownloadDAO.updateDataObjectDownloadTask(downloadTask);
		}

		logger.info(
				"download task: [taskId={}] - continued  - archive file-id = {} [transfer-type={}, destination-type={}]",
				downloadTask.getId(), downloadRequest.getArchiveLocation().getFileId(),
				downloadTask.getDataTransferType(), downloadTask.getDestinationType());

		return true;
	}

	@Override
	public void stageHyperfileDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask) throws HpcException {

		HpcFileLocation secondHopArchiveLocation = getSecondHopDownloadSourceLocation(downloadTask.getConfigurationId(),
				downloadTask.getS3ArchiveConfigurationId(), downloadTask.getDataTransferType());

		// Get the data transfer configuration.
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(downloadTask.getConfigurationId(),
						downloadTask.getS3ArchiveConfigurationId(), downloadTask.getDataTransferType());

		// Create a download request from Hyperfile to the DME Globus endpoint.
		HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
		downloadRequest.setArchiveLocation(downloadTask.getArchiveLocation());
		downloadRequest.setCompletionEvent(downloadTask.getCompletionEvent());
		downloadRequest.setCollectionDownloadTaskId(downloadTask.getCollectionDownloadTaskId());
		downloadRequest.setDataTransferType(downloadTask.getDataTransferType());
		downloadRequest.setConfigurationId(downloadTask.getConfigurationId());
		downloadRequest.setS3ArchiveConfigurationId(downloadTask.getS3ArchiveConfigurationId());
		downloadRequest.setPath(downloadTask.getPath());
		downloadRequest.setUserId(downloadTask.getUserId());
		downloadRequest.setFileDestination(createFile(
				getFilePath(secondHopArchiveLocation.getFileId(), dataTransferConfiguration.getBaseDownloadSource())));
		downloadRequest.setSize(downloadTask.getSize());
		downloadRequest.setSudoPassword(systemAccountLocator.getSystemAccount(HpcIntegratedSystem.IRODS).getPassword());

		// Stage the file in DME Globus endpoint.
		dataTransferProxies.get(downloadTask.getDataTransferType()).downloadDataObject(
				getAuthenticatedToken(downloadTask.getDataTransferType(), downloadRequest.getConfigurationId(),
						downloadRequest.getS3ArchiveConfigurationId()),
				downloadRequest, dataTransferConfiguration.getBaseArchiveDestination(), null,
				dataTransferConfiguration.getEncryptedTransfer());

		// Reset the download task, so it continues to download from DME Globus endpoint
		// to the user's Globus endpoint.
		downloadTask.setArchiveLocation(secondHopArchiveLocation);
		downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.RECEIVED);
		downloadTask.setDownloadFilePath(downloadRequest.getFileDestination().getAbsolutePath());
		downloadTask.setInProcess(false);

		// Persist the task.
		dataDownloadDAO.updateDataObjectDownloadTask(downloadTask);
	}

	@Override
	public void resetDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask) throws HpcException {

		logger.debug(
				"download task: [taskId={}] - resetDataObjectDownloadTask called. Setting in-process=false [transfer-type={}, server-id={}]",
				downloadTask.getId(), downloadTask.getDataTransferType(),
				HpcDataTransferType.S_3.equals(downloadTask.getDataTransferType()) ? s3DownloadTaskServerId : null);

		downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.RECEIVED);
		downloadTask.setPercentComplete(0);
		downloadTask.setInProcess(false);
		downloadTask.setS3DownloadTaskServerId(null);
		if (!StringUtils.isEmpty(downloadTask.getDownloadFilePath())) {
			FileUtils.deleteQuietly(new File(downloadTask.getDownloadFilePath()));
			downloadTask.setDownloadFilePath(null);
		}

		if (downloadTask.getDataTransferType().equals(HpcDataTransferType.ASPERA)) {
			downloadTask.setDataTransferType(HpcDataTransferType.S_3);
		}

		dataDownloadDAO.updateDataObjectDownloadTask(downloadTask);
	}

	@Override
	public void resetDataObjectDownloadTasksInProcess() throws HpcException {
		dataDownloadDAO.resetDataObjectDownloadTaskInProcess(s3DownloadTaskServerId);
	}

	@Override
	public boolean markProcessedDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask,
			HpcDataTransferType dataTransferType, boolean inProcess) throws HpcException {
		// Only set in-process to true if this task in a RECEIVED status, and the
		// in-process not already true.
		boolean updated = true;

		logger.debug(
				"download task: [taskId={}] - markProcessedDataObjectDownloadTask called attempting to update to in-process={} [transfer-type={}, server-id={}]",
				downloadTask.getId(), inProcess, downloadTask.getDataTransferType(),
				HpcDataTransferType.S_3.equals(dataTransferType) ? s3DownloadTaskServerId : null);
		if (!inProcess || (!downloadTask.getInProcess()
				&& downloadTask.getDataTransferStatus().equals(HpcDataTransferDownloadStatus.RECEIVED))) {
			String serverId = HpcDataTransferType.S_3.equals(dataTransferType) ? s3DownloadTaskServerId : null;
			updated = dataDownloadDAO.setDataObjectDownloadTaskInProcess(downloadTask.getId(), inProcess, serverId);
			if (updated)
				logger.debug(
						"download task: [taskId={}] - markProcessedDataObjectDownloadTask updated to in-process={} - [transfer-type={}, server-id={}]",
						downloadTask.getId(), inProcess, downloadTask.getDataTransferType(),
						HpcDataTransferType.S_3.equals(dataTransferType) ? s3DownloadTaskServerId : null);
			downloadTask.setS3DownloadTaskServerId(serverId);
		}

		if (updated) {
			Calendar processed = Calendar.getInstance();
			dataDownloadDAO.setDataObjectDownloadTaskProcessed(downloadTask.getId(), processed);
			logger.debug(
					"download task: [taskId={}] - markProcessedDataObjectDownloadTask processed timestamp update only - [transfer-type={}, server-id={}]",
					downloadTask.getId(), downloadTask.getDataTransferType(),
					HpcDataTransferType.S_3.equals(dataTransferType) ? s3DownloadTaskServerId : null);
			downloadTask.setProcessed(processed);
			downloadTask.setInProcess(inProcess);
		}

		return updated;
	}

	@Override
	public boolean updateDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask, long bytesTransferred)
			throws HpcException {
		// Input validation.
		if (downloadTask == null || downloadTask.getSize() <= 0 || bytesTransferred <= 0
				|| bytesTransferred > downloadTask.getSize()) {
			logger.info(
					"download task: [taskId={}] - % complete unchanged. bytesTransferred: {}, fileSize: {} [transfer-type={}, destination-type={}]",
					downloadTask.getId(), bytesTransferred, downloadTask.getSize(), downloadTask.getDataTransferType(),
					downloadTask.getDestinationType());
			return true;
		}

		// Check if the task got cancelled.
		HpcDataObjectDownloadTask task = dataDownloadDAO.getDataObjectDownloadTask(downloadTask.getId());
		if (task != null && HpcDataTransferDownloadStatus.CANCELED.equals(task.getDataTransferStatus())) {
			logger.info("download task: [taskId={}] - cancelled - [transfer-type={}, destination-type={}]",
					downloadTask.getId(), downloadTask.getDataTransferType(), downloadTask.getDestinationType());
			return false;
		}

		// Calculate the percent complete.
		int percentComplete = Math.round(100 * (float) bytesTransferred / downloadTask.getSize());

		if (downloadTask.getDataTransferType().equals(HpcDataTransferType.S_3)
				&& (downloadTask.getDestinationType().equals(HpcDataTransferType.GLOBUS)
						|| downloadTask.getDestinationType().equals(HpcDataTransferType.ASPERA))) {
			// This is a 2-hop download, performing the 1st Hop (i.e. staging the file, not
			// downloading to destination just yet)
			downloadTask.setPercentComplete(0);
			downloadTask.setStagingPercentComplete(percentComplete);

		} else {
			// Any other download.
			downloadTask.setPercentComplete(percentComplete);
			downloadTask.setStagingPercentComplete(null);
		}

		logger.info("download task: [taskId={}] - % complete - {} [transfer-type={}, destination-type={}]",
				downloadTask.getId(), percentComplete, downloadTask.getDataTransferType(),
				downloadTask.getDestinationType());

		return dataDownloadDAO.updateDataObjectDownloadTask(downloadTask);
	}

	@Override
	public HpcCollectionDownloadTask downloadCollection(String path,
			HpcGlobusDownloadDestination globusDownloadDestination, HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDownloadDestination googleDriveDownloadDestination,
			HpcGoogleDownloadDestination googleCloudStorageDownloadDestination,
			HpcAsperaDownloadDestination asperaDownloadDestination, HpcBoxDownloadDestination boxDownloadDestination,
			String userId, String configurationId, boolean appendPathToDownloadDestination,
			boolean appendCollectionNameToDownloadDestination) throws HpcException {

		// Validate the download destination.
		validateDownloadDestination(globusDownloadDestination, s3DownloadDestination, googleDriveDownloadDestination,
				googleCloudStorageDownloadDestination, asperaDownloadDestination, boxDownloadDestination, null,
				configurationId, true);

		if (globusDownloadDestination != null) {
			checkForDuplicateCollectionDownloadRequests(path, globusDownloadDestination);
		}

		// Create a new collection download task.
		HpcCollectionDownloadTask downloadTask = new HpcCollectionDownloadTask();
		downloadTask.setCreated(Calendar.getInstance());
		downloadTask.setGlobusDownloadDestination(globusDownloadDestination);
		downloadTask.setS3DownloadDestination(s3DownloadDestination);
		downloadTask.setGoogleDriveDownloadDestination(googleDriveDownloadDestination);
		downloadTask.setGoogleCloudStorageDownloadDestination(googleCloudStorageDownloadDestination);
		downloadTask.setAsperaDownloadDestination(asperaDownloadDestination);
		downloadTask.setBoxDownloadDestination(boxDownloadDestination);
		downloadTask.setPath(path);
		downloadTask.setUserId(userId);
		downloadTask.setType(HpcDownloadTaskType.COLLECTION);
		downloadTask.setStatus(HpcCollectionDownloadTaskStatus.RECEIVED);
		downloadTask.setConfigurationId(configurationId);
		downloadTask.setDoc(dataManagementService.getDataManagementConfiguration(configurationId).getDoc());
		downloadTask.setAppendPathToDownloadDestination(appendPathToDownloadDestination);
		downloadTask.setAppendCollectionNameToDownloadDestination(appendCollectionNameToDownloadDestination);

		// Persist the request.
		dataDownloadDAO.upsertCollectionDownloadTask(downloadTask);

		return downloadTask;
	}

	@Override
	public HpcCollectionDownloadTask downloadCollections(List<String> collectionPaths,
			HpcGlobusDownloadDestination globusDownloadDestination, HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDownloadDestination googleDriveDownloadDestination,
			HpcGoogleDownloadDestination googleCloudStorageDownloadDestination,
			HpcAsperaDownloadDestination asperaDownloadDestination, HpcBoxDownloadDestination boxDownloadDestination,
			String userId, String configurationId, boolean appendPathToDownloadDestination,
			boolean appendCollectionNameToDownloadDestination) throws HpcException {
		// Validate the download destination.
		validateDownloadDestination(globusDownloadDestination, s3DownloadDestination, googleDriveDownloadDestination,
				googleCloudStorageDownloadDestination, asperaDownloadDestination, boxDownloadDestination, null,
				configurationId, true);

		// Create a new collection download task.
		HpcCollectionDownloadTask downloadTask = new HpcCollectionDownloadTask();
		downloadTask.setCreated(Calendar.getInstance());
		downloadTask.setGlobusDownloadDestination(globusDownloadDestination);
		downloadTask.setS3DownloadDestination(s3DownloadDestination);
		downloadTask.setGoogleDriveDownloadDestination(googleDriveDownloadDestination);
		downloadTask.setGoogleCloudStorageDownloadDestination(googleCloudStorageDownloadDestination);
		downloadTask.setAsperaDownloadDestination(asperaDownloadDestination);
		downloadTask.setBoxDownloadDestination(boxDownloadDestination);
		downloadTask.getCollectionPaths().addAll(collectionPaths);
		downloadTask.setUserId(userId);
		downloadTask.setType(HpcDownloadTaskType.COLLECTION_LIST);
		downloadTask.setStatus(HpcCollectionDownloadTaskStatus.RECEIVED);
		downloadTask.setConfigurationId(configurationId);
		downloadTask.setAppendPathToDownloadDestination(appendPathToDownloadDestination);
		downloadTask.setAppendCollectionNameToDownloadDestination(appendCollectionNameToDownloadDestination);
		downloadTask.setDoc(dataManagementService.getDataManagementConfiguration(configurationId).getDoc());

		// Persist the request.
		dataDownloadDAO.upsertCollectionDownloadTask(downloadTask);

		return downloadTask;
	}

	@Override
	public HpcCollectionDownloadTask downloadDataObjects(List<String> dataObjectPaths,
			HpcGlobusDownloadDestination globusDownloadDestination, HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDownloadDestination googleDriveDownloadDestination,
			HpcGoogleDownloadDestination googleCloudStorageDownloadDestination,
			HpcAsperaDownloadDestination asperaDownloadDestination, HpcBoxDownloadDestination boxDownloadDestination,
			String userId, String configurationId, boolean appendPathToDownloadDestination,
			boolean appendCollectionNameToDownloadDestination) throws HpcException {
		// Validate the requested destination location. Note: we use the configuration
		// ID of one data object path. At this time, there is no need to validate for
		// all
		// configuration IDs.
		validateDownloadDestination(globusDownloadDestination, s3DownloadDestination, googleDriveDownloadDestination,
				googleCloudStorageDownloadDestination, asperaDownloadDestination, boxDownloadDestination, null,
				configurationId, true);

		// Create a new collection download task.
		HpcCollectionDownloadTask downloadTask = new HpcCollectionDownloadTask();
		downloadTask.setCreated(Calendar.getInstance());
		downloadTask.setGlobusDownloadDestination(globusDownloadDestination);
		downloadTask.setS3DownloadDestination(s3DownloadDestination);
		downloadTask.setGoogleDriveDownloadDestination(googleDriveDownloadDestination);
		downloadTask.setGoogleCloudStorageDownloadDestination(googleCloudStorageDownloadDestination);
		downloadTask.setAsperaDownloadDestination(asperaDownloadDestination);
		downloadTask.setBoxDownloadDestination(boxDownloadDestination);
		downloadTask.getDataObjectPaths().addAll(dataObjectPaths);
		downloadTask.setUserId(userId);
		downloadTask.setType(HpcDownloadTaskType.DATA_OBJECT_LIST);
		downloadTask.setStatus(HpcCollectionDownloadTaskStatus.RECEIVED);
		downloadTask.setConfigurationId(configurationId);
		downloadTask.setDoc(dataManagementService.getDataManagementConfiguration(configurationId).getDoc());
		downloadTask.setAppendPathToDownloadDestination(appendPathToDownloadDestination);
		downloadTask.setAppendCollectionNameToDownloadDestination(appendCollectionNameToDownloadDestination);

		// Persist the request.
		dataDownloadDAO.upsertCollectionDownloadTask(downloadTask);

		return downloadTask;
	}

	@Override
	public void updateCollectionDownloadTask(HpcCollectionDownloadTask downloadTask) throws HpcException {
		dataDownloadDAO.upsertCollectionDownloadTask(downloadTask);
	}

	public void processCollectionDownloadTaskSecondHopBunch(HpcCollectionDownloadTask collectionDownloadTask,
			List<HpcDataObjectDownloadTask> dataObjectDownloadTasks) throws HpcException {

		// Create a Globus transfer request for the bunch.
		String configurationId = null;
		String s3ArchiveConfigurationId = null;
		HpcGlobusTransferRequest globusTransferRequest = new HpcGlobusTransferRequest();

		for (HpcDataObjectDownloadTask dataObjectDownloadTask : dataObjectDownloadTasks) {
			String sourceEndpoint = dataObjectDownloadTask.getArchiveLocation().getFileContainerId();
			String destinationEndpoint = dataObjectDownloadTask.getGlobusDownloadDestination().getDestinationLocation()
					.getFileContainerId();
			String sourcePath = dataObjectDownloadTask.getArchiveLocation().getFileId();
			String destinationPath = dataObjectDownloadTask.getGlobusDownloadDestination().getDestinationLocation()
					.getFileId();

			if (StringUtils.isEmpty(globusTransferRequest.getSourceEndpoint())) {
				globusTransferRequest.setSourceEndpoint(sourceEndpoint);
				globusTransferRequest.setDestinationEndpoint(destinationEndpoint);
				configurationId = dataObjectDownloadTask.getConfigurationId();
				s3ArchiveConfigurationId = dataObjectDownloadTask.getS3ArchiveConfigurationId();
			} else if (!globusTransferRequest.getSourceEndpoint().equals(sourceEndpoint)
					|| (!globusTransferRequest.getDestinationEndpoint().equals(destinationEndpoint))) {
				throw new HpcException("Invalid bulk download to Globus, Can't bunch", HpcErrorType.UNEXPECTED_ERROR);
			}
			HpcGlobusTransferItem item = new HpcGlobusTransferItem();
			item.setSourcePath(sourcePath);
			item.setDestinationPath(destinationPath);
			globusTransferRequest.getItems().add(item);
		}

		// Get the base archive destination.
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, HpcDataTransferType.S_3);

		// Submit the transfer request to Globus.
		Object authenticatedToken = getAuthenticatedToken(HpcDataTransferType.GLOBUS, configurationId,
				s3ArchiveConfigurationId);
		String dataTransferRequestId = dataTransferProxies.get(HpcDataTransferType.GLOBUS).transferData(
				authenticatedToken, globusTransferRequest, dataTransferConfiguration.getEncryptedTransfer());
		collectionDownloadTask.setDataTransferRequestId(dataTransferRequestId);
		collectionDownloadTask.setStatus(HpcCollectionDownloadTaskStatus.GLOBUS_BUNCHING);
		collectionDownloadTask.setConfigurationId(configurationId);
		collectionDownloadTask.setDoc(dataManagementService.getDataManagementConfiguration(configurationId).getDoc());

		// Add to HPC_GLOBUS_TRANSFER_TASK
		HpcGlobusTransferTask globusRequest = new HpcGlobusTransferTask();
		globusRequest.setDataTransferRequestId(dataTransferRequestId);
		globusRequest.setGlobusAccount(getDataTransferAuthenticatedToken(authenticatedToken).getSystemAccountId());
		globusRequest.setPath(collectionDownloadTask.getPath());
		globusRequest.setDownload(true);
		globusTransferDAO.insertRequest(globusRequest);
	}

	@Override
	public void cancelCollectionDownloadTask(HpcCollectionDownloadTask downloadTask) throws HpcException {
		if (downloadTask.getStatus().equals(HpcCollectionDownloadTaskStatus.RECEIVED)) {
			dataDownloadDAO.setCollectionDownloadTaskCancellationRequested(downloadTask.getId(), true);
		}

		cancelCollectionDownloadTaskItems(downloadTask.getId());
	}

	@Override
	public void cancelCollectionDownloadTaskItems(String taskId) throws HpcException {
		dataDownloadDAO.updateDataObjectDownloadTasksStatus(taskId, cancelCollectionDownloadTaskItemsFilter,
				HpcDataTransferDownloadStatus.CANCELED);
	}

	@Override
	public HpcCollectionDownloadTask retryCollectionDownloadTask(HpcDownloadTaskResult downloadTaskResult,
			Boolean destinationOverwrite, HpcS3Account s3Account, String googleAccessToken,
			HpcAsperaAccount asperaAccount, String boxAccessToken, String boxRefreshToken, String retryUserId)
			throws HpcException {
		// Validate the task failed with at least one failed item before submitting it
		// for a retry.

		if (!downloadTaskCanBeRetried(downloadTaskResult.getResult())) {
			throw new HpcException("Download task completed: " + downloadTaskResult.getId()
					+ ". Only failed or canceled tasks can be retried", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (downloadTaskResult.getType().equals(HpcDownloadTaskType.DATA_OBJECT_LIST)) {
			// If retry a data object list downlnload task, we validate that at least 1 item
			// failed or canceled in the task.
			boolean failedOrCanceledDownloadItemFound = false;
			for (HpcCollectionDownloadTaskItem downloadItem : downloadTaskResult.getItems()) {
				if (downloadTaskCanBeRetried(downloadItem.getResult())) {
					failedOrCanceledDownloadItemFound = true;
					break;
				}
			}
			if (!failedOrCanceledDownloadItemFound) {
				throw new HpcException(
						"No failed / canceled download item found for task: " + downloadTaskResult.getId(),
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Create a new collection download task.
		HpcCollectionDownloadTask downloadTask = new HpcCollectionDownloadTask();
		downloadTask.setRetryTaskId(downloadTaskResult.getId());
		downloadTask.setRetryUserId(retryUserId);
		downloadTask.setCreated(Calendar.getInstance());
		downloadTask.setStatus(HpcCollectionDownloadTaskStatus.RECEIVED);
		downloadTask.setUserId(downloadTaskResult.getUserId());
		downloadTask.setType(downloadTaskResult.getType());
		downloadTask.setPath(downloadTaskResult.getPath());
		downloadTask.setDoc(downloadTaskResult.getDoc());
		downloadTask.getCollectionPaths().addAll(downloadTaskResult.getCollectionPaths());

		// Set the configuration ID for collection(s) retry.
		String configurationId = null;
		if (!StringUtils.isEmpty(downloadTask.getPath())) {
			configurationId = metadataService.getCollectionSystemGeneratedMetadata(downloadTask.getPath())
					.getConfigurationId();
		} else if (downloadTask.getCollectionPaths().size() > 0) {
			configurationId = metadataService
					.getCollectionSystemGeneratedMetadata(downloadTask.getCollectionPaths().iterator().next())
					.getConfigurationId();
		}
		downloadTask.setConfigurationId(configurationId);

		switch (downloadTaskResult.getDestinationType()) {
		case GLOBUS:
			HpcGlobusDownloadDestination globusDownloadDestination = new HpcGlobusDownloadDestination();
			globusDownloadDestination.setDestinationOverwrite(Optional.ofNullable(destinationOverwrite).orElse(false));
			globusDownloadDestination.setDestinationLocation(downloadTaskResult.getDestinationLocation());
			checkForDuplicateCollectionDownloadRequests(downloadTaskResult.getPath(), globusDownloadDestination);
			downloadTask.setGlobusDownloadDestination(globusDownloadDestination);
			break;

		case S_3:
			// Validate the S3 account.
			HpcDomainValidationResult s3AccountValidationResult = isValidS3Account(s3Account);
			if (!s3AccountValidationResult.getValid()) {
				throw new HpcException("Invalid S3 account: " + s3AccountValidationResult.getMessage(),
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			HpcS3DownloadDestination s3DownloadDestination = new HpcS3DownloadDestination();
			s3DownloadDestination.setAccount(s3Account);
			s3DownloadDestination.setDestinationLocation(downloadTaskResult.getDestinationLocation());
			downloadTask.setS3DownloadDestination(s3DownloadDestination);
			break;

		case GOOGLE_DRIVE:
			// Validate the Google Drive access token.
			if (StringUtils.isEmpty(googleAccessToken)) {
				throw new HpcException("Invalid Google Drive access token", HpcErrorType.INVALID_REQUEST_INPUT);
			}

			HpcGoogleDownloadDestination googleDriveDownloadDestination = new HpcGoogleDownloadDestination();
			googleDriveDownloadDestination.setAccessToken(googleAccessToken);
			googleDriveDownloadDestination.setDestinationLocation(downloadTaskResult.getDestinationLocation());
			downloadTask.setGoogleDriveDownloadDestination(googleDriveDownloadDestination);
			break;

		case GOOGLE_CLOUD_STORAGE:
			// Validate the Google Cloud access token.
			if (StringUtils.isEmpty(googleAccessToken) || googleAccessToken == null) {
				throw new HpcException("Invalid Google Cloud Storage access token / token type",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			HpcGoogleDownloadDestination googleCloudStorageDownloadDestination = new HpcGoogleDownloadDestination();
			googleCloudStorageDownloadDestination.setAccessToken(googleAccessToken);
			googleCloudStorageDownloadDestination.setDestinationLocation(downloadTaskResult.getDestinationLocation());
			downloadTask.setGoogleCloudStorageDownloadDestination(googleCloudStorageDownloadDestination);
			break;

		case ASPERA:
			// Validate the Aspera account.
			HpcDomainValidationResult asperaAccountValidationResult = isValidAsperaAccount(asperaAccount);
			if (!asperaAccountValidationResult.getValid()) {
				throw new HpcException("Invalid Aspera account: " + asperaAccountValidationResult.getMessage(),
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			HpcAsperaDownloadDestination asperaDownloadDestination = new HpcAsperaDownloadDestination();
			asperaDownloadDestination.setAccount(asperaAccount);
			asperaDownloadDestination.setDestinationLocation(downloadTaskResult.getDestinationLocation());
			downloadTask.setAsperaDownloadDestination(asperaDownloadDestination);
			break;

		case BOX:
			// Validate the Box access and refresh tokens.
			if (StringUtils.isEmpty(boxAccessToken) || StringUtils.isEmpty(boxRefreshToken)) {
				throw new HpcException("Invalid Box access/refresh token", HpcErrorType.INVALID_REQUEST_INPUT);
			}

			HpcBoxDownloadDestination boxDownloadDestination = new HpcBoxDownloadDestination();
			boxDownloadDestination.setAccessToken(boxAccessToken);
			boxDownloadDestination.setRefreshToken(boxRefreshToken);
			boxDownloadDestination.setDestinationLocation(downloadTaskResult.getDestinationLocation());
			downloadTask.setBoxDownloadDestination(boxDownloadDestination);
			break;

		default:
			throw new HpcException("Download retry not supported for destination type: "
					+ downloadTaskResult.getDestinationType().value(), HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Persist the request.
		dataDownloadDAO.upsertCollectionDownloadTask(downloadTask);

		return downloadTask;
	}

	@Override
	public List<HpcCollectionDownloadTask> getCollectionDownloadTasksInProcess() throws HpcException {
		return dataDownloadDAO.getCollectionDownloadTasksInProcess();
	}

	@Override
	public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus status)
			throws HpcException {
		return dataDownloadDAO.getCollectionDownloadTasks(status);
	}

	@Override
	public int getCollectionDownloadTasksCount(String userId, HpcCollectionDownloadTaskStatus status, boolean inProcess)
			throws HpcException {
		return dataDownloadDAO.getCollectionDownloadTasksCount(userId, status, inProcess);
	}

	@Override
	public int getCollectionDownloadRequestsCountByPathAndEndpoint(String path, String endpoint) throws HpcException {
		return dataDownloadDAO.getCollectionDownloadRequestsCountByPathAndEndpoint(path, endpoint);
	}

	@Override
	public int getCollectionDownloadTasksCountByUserAndPath(String userId, String path, boolean inProcess)
			throws HpcException {
		return dataDownloadDAO.getCollectionDownloadTasksCountByUserAndPath(userId, path, inProcess);
	}

	@Override
	public int getCollectionDownloadTasksCountByUser(String userId, boolean inProcess) throws HpcException {
		return dataDownloadDAO.getCollectionDownloadTasksCountByUser(userId, inProcess);
	}

	@Override
	public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus status,
			boolean inProcess) throws HpcException {
		return dataDownloadDAO.getCollectionDownloadTasks(status, inProcess);
	}

	@Override
	public void setCollectionDownloadTaskInProgress(String taskId, boolean inProcess) throws HpcException {
		dataDownloadDAO.setCollectionDownloadTaskInProcess(taskId, inProcess);
	}

	@Override
	public void resetCollectionDownloadTaskInProgress(String taskId) throws HpcException {
		dataDownloadDAO.resetCollectionDownloadTaskInProcess(taskId);

		// Cancel any data object that got started, as the collection download will get
		// re-processed.
		for (HpcDataObjectDownloadTask dataObjectDownloadTask : dataDownloadDAO
				.getDataObjectDownloadTaskByCollectionDownloadTaskId(taskId)) {
			completeDataObjectDownloadTask(dataObjectDownloadTask, HpcDownloadResult.CANCELED,
					"Collection download restarted", Calendar.getInstance(), 0);
		}
	}

	@Override
	public void completeCollectionDownloadTask(HpcCollectionDownloadTask downloadTask, HpcDownloadResult result,
			String message, Calendar completed) throws HpcException {
		// Input validation
		if (downloadTask == null) {
			throw new HpcException("Invalid collection download task", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Cleanup the DB record.
		dataDownloadDAO.deleteCollectionDownloadTask(downloadTask.getId());

		// Remove from HPC_GLOBUS_TRANSFER_TASK if Globus request
		if (downloadTask.getDestinationType().equals(HpcDataTransferType.GLOBUS)
				&& !StringUtils.isEmpty(downloadTask.getDataTransferRequestId()))
			deleteGlobusTransferTask(downloadTask.getDataTransferRequestId());

		// Create a task result object.
		HpcDownloadTaskResult taskResult = new HpcDownloadTaskResult();
		taskResult.setId(downloadTask.getId());
		taskResult.setUserId(downloadTask.getUserId());
		taskResult.setPath(downloadTask.getPath());
		taskResult.getCollectionPaths().addAll(downloadTask.getCollectionPaths());
		if (downloadTask.getS3DownloadDestination() != null) {
			taskResult.setDestinationLocation(downloadTask.getS3DownloadDestination().getDestinationLocation());
			taskResult.setDestinationType(HpcDataTransferType.S_3);
		} else if (downloadTask.getGlobusDownloadDestination() != null) {
			taskResult.setDestinationLocation(downloadTask.getGlobusDownloadDestination().getDestinationLocation());
			taskResult.setDestinationType(HpcDataTransferType.GLOBUS);
		} else if (downloadTask.getGoogleDriveDownloadDestination() != null) {
			taskResult
					.setDestinationLocation(downloadTask.getGoogleDriveDownloadDestination().getDestinationLocation());
			taskResult.setDestinationType(HpcDataTransferType.GOOGLE_DRIVE);
		} else if (downloadTask.getGoogleCloudStorageDownloadDestination() != null) {
			taskResult.setDestinationLocation(
					downloadTask.getGoogleCloudStorageDownloadDestination().getDestinationLocation());
			taskResult.setDestinationType(HpcDataTransferType.GOOGLE_CLOUD_STORAGE);
		} else if (downloadTask.getAsperaDownloadDestination() != null) {
			taskResult.setDestinationLocation(downloadTask.getAsperaDownloadDestination().getDestinationLocation());
			taskResult.setDestinationType(HpcDataTransferType.ASPERA);
		} else if (downloadTask.getBoxDownloadDestination() != null) {
			taskResult.setDestinationLocation(downloadTask.getBoxDownloadDestination().getDestinationLocation());
			taskResult.setDestinationType(HpcDataTransferType.BOX);
		}
		taskResult.setResult(result);
		taskResult.setType(downloadTask.getType());
		taskResult.setMessage(message);
		taskResult.setCompletionEvent(true);
		taskResult.setCreated(downloadTask.getCreated());
		taskResult.setCompleted(completed);
		taskResult.getItems().addAll(downloadTask.getItems());
		taskResult.setRetryTaskId(downloadTask.getRetryTaskId());
		taskResult.setRetryUserId(downloadTask.getRetryUserId());
		taskResult.setDataTransferRequestId(downloadTask.getDataTransferRequestId());
		taskResult.setDoc(downloadTask.getDoc());

		// Calculate the effective transfer speed (Bytes per second). This is done by
		// averaging the effective transfer speed
		// of all successful download items.
		int effectiveTransferSpeed = 0;
		int completedItems = 0;
		long totalSize = 0;
		for (HpcCollectionDownloadTaskItem item : downloadTask.getItems()) {
			if (item.getResult() != null && item.getResult().equals(HpcDownloadResult.COMPLETED)
					&& item.getEffectiveTransferSpeed() != null) {
				effectiveTransferSpeed += item.getEffectiveTransferSpeed();
				completedItems++;
			}
			// Compute the total size of the collection
			totalSize = totalSize + Optional.ofNullable(item.getSize()).orElse(0L);
		}
		taskResult.setEffectiveTransferSpeed(completedItems > 0 ? effectiveTransferSpeed / completedItems : null);
		taskResult.setSize(totalSize);

		// Persist to DB.
		dataDownloadDAO.upsertDownloadTaskResult(taskResult);
	}

	@Override
	public List<HpcUserDownloadRequest> getDownloadRequests(String userId, String doc) throws HpcException {
		List<HpcUserDownloadRequest> downloadRequests = null;
		if (doc == null) {
			downloadRequests = dataDownloadDAO.getDataObjectDownloadRequests(userId);
			downloadRequests.addAll(dataDownloadDAO.getCollectionDownloadRequests(userId));
		} else if (doc.equals("ALL")) {
			downloadRequests = dataDownloadDAO.getAllDataObjectDownloadRequests();
			downloadRequests.addAll(dataDownloadDAO.getAllCollectionDownloadRequests());
		} else {
			downloadRequests = dataDownloadDAO.getDataObjectDownloadRequestsForDoc(doc, userId);
			downloadRequests.addAll(dataDownloadDAO.getCollectionDownloadRequestsForDoc(doc, userId));
		}
		return downloadRequests;
	}

	@Override
	public List<HpcUserDownloadRequest> getDownloadResults(String userId, int page, String doc,
			int activeRequestsOffset, Integer pageSize) throws HpcException {
		if (page < 1) {
			throw new HpcException("Invalid search results page: " + page, HpcErrorType.INVALID_REQUEST_INPUT);
		}
		// If pageSize is specified, replace the default defined
		int finalPageSize = pagination.getPageSize();
		if (pageSize != null && pageSize != 0) {
			finalPageSize = pageSize;
		}
		List<HpcUserDownloadRequest> downloadResults = null;
		if (doc == null) {
			downloadResults = dataDownloadDAO.getDownloadResults(userId, (page - 1) * finalPageSize,
					finalPageSize - activeRequestsOffset);
		} else if (doc.equals("ALL")) {
			downloadResults = dataDownloadDAO.getAllDownloadResults((page - 1) * finalPageSize,
					finalPageSize - activeRequestsOffset);
		} else {
			downloadResults = dataDownloadDAO.getDownloadResultsForDoc(doc, userId, (page - 1) * finalPageSize,
					finalPageSize - activeRequestsOffset);
		}
		return downloadResults;
	}

	@Override
	public int getDownloadResultsCount(String userId, String doc) throws HpcException {
		int count = 0;
		if (doc == null) {
			count = dataDownloadDAO.getDownloadResultsCount(userId);
		} else if (doc.equals("ALL")) {
			count = dataDownloadDAO.getAllDownloadResultsCount();
		} else {
			count = dataDownloadDAO.getDownloadResultsCountForDoc(doc, userId);
		}
		return count;
	}

	@Override
	public int getDownloadResultsPageSize() {
		return pagination.getPageSize();
	}

	@Override
	public String getFileContainerName(HpcDataTransferType dataTransferType, String configurationId,
			String fileContainerId) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(fileContainerId)) {
			throw new HpcException("Null / Empty file container ID.", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return dataTransferProxies.get(dataTransferType)
				.getFileContainerName(dataTransferType.equals(HpcDataTransferType.GLOBUS)
						? getAuthenticatedToken(dataTransferType, configurationId, null)
						: null, fileContainerId);
	}

	@Override
	public boolean uploadURLExpired(Calendar urlCreated, String configurationId, String s3ArchiveConfigurationId) {
		if (urlCreated == null || StringUtils.isEmpty(configurationId)) {
			logger.error("url-created {} or config-id {} is null", urlCreated, configurationId);
			return false;
		}

		// Get the URL expiration period (in hours) from the configuration.
		Integer uploadRequestURLExpiration = null;
		try {
			uploadRequestURLExpiration = dataManagementConfigurationLocator
					.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, HpcDataTransferType.S_3)
					.getUploadRequestURLExpiration();

		} catch (HpcException e) {
			logger.error("Failed to get URL expiration from config", e);
			return false;
		}

		// Calculate the expiration time.
		Date expiration = new Date();
		expiration.setTime(urlCreated.getTimeInMillis() + 1000 * 60 * 60 * uploadRequestURLExpiration);

		// Return true if the current time is passed the expiration time.
		return expiration.before(new Date());
	}

	@Override
	public List<HpcDataObjectDownloadTask> getDataObjectDownloadTaskByStatus(
			HpcDataTransferDownloadStatus dataTransferStatus) throws HpcException {
		return dataDownloadDAO.getDataObjectDownloadTaskByStatus(dataTransferStatus);
	}

	@Override
	public HpcArchiveObjectMetadata getDataObjectMetadata(HpcFileLocation fileLocation,
			HpcDataTransferType dataTransferType, String configurationId, String s3ArchiveConfigurationId)
			throws HpcException {

		return dataTransferProxies.get(dataTransferType).getDataObjectMetadata(
				getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId), fileLocation);
	}

	@Override
	public boolean validateDataObjectMetadata(HpcFileLocation fileLocation, HpcDataTransferType dataTransferType,
			String configurationId, String s3ArchiveConfigurationId, String objectId, String registrarId)
			throws HpcException {
		return dataTransferProxies.get(dataTransferType).validateDataObjectMetadata(
				getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId), fileLocation,
				generateArchiveMetadata(configurationId, objectId, registrarId));
	}

	@Override
	public void setArchivePermissions(String configurationId, String s3ArchiveConfigurationId,
			HpcDataTransferType dataTransferType, String fileId, HpcPathPermissions permissions) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(fileId)) {
			throw new HpcException("Invalid file id", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (permissions == null || StringUtils.isEmpty(permissions.getOwner())
				|| StringUtils.isEmpty(permissions.getGroup()) || permissions.getPermissionsMode() == null) {
			throw new HpcException("Invalid permissions object", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the data transfer configuration.
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);

		// Get the archive path.
		String archivePath = getFilePath(fileId, dataTransferConfiguration.getBaseArchiveDestination());

		logger.info("Archive permissions request: {} [{}.{} {}]", archivePath, permissions.getOwner(),
				permissions.getGroup(), permissions.getPermissionsMode());

		// Set Owner, Group and Permissions on the archive path.
		String sudoPassword = systemAccountLocator.getSystemAccount(HpcIntegratedSystem.IRODS).getPassword();

		// TODO: We no longer set the owner permissions, but keep the DME system account
		// to own the file.
		// With this change the 'sudo enabled exec() is no longer needed. This is a
		// temporary fix until code cleanup
		// exec("chown " + permissions.getOwner() + " " + archivePath, sudoPassword);

		exec("chown :" + permissions.getGroup() + " " + archivePath, sudoPassword, null, null);
		exec("chmod " + permissions.getPermissionsMode() + " " + archivePath, sudoPassword, null, null);
	}

	@Override
	public List<HpcMetadataEntry> generateArchiveMetadata(String configurationId, String objectId, String registrarId) {
		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();

		if (dataManagementConfigurationLocator.get(configurationId).getCreateArchiveMetadata()) {
			// Create the user-id metadata.
			HpcMetadataEntry entry = new HpcMetadataEntry();
			entry.setAttribute(REGISTRAR_ID_ATTRIBUTE);
			entry.setValue(registrarId);
			metadataEntries.add(entry);

			// Create the path metadata.
			entry = new HpcMetadataEntry();
			entry.setAttribute(OBJECT_ID_ATTRIBUTE);
			entry.setValue(objectId);
			metadataEntries.add(entry);
		}

		return metadataEntries;
	}

	@Override
	public void deleteGlobusTransferTask(String dataTransferRequestId) throws HpcException {
		globusTransferDAO.deleteRequest(dataTransferRequestId);
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Get the data transfer authenticated token if cached. If it's not cached or
	 * expired, get a token by authenticating.
	 *
	 * @param dataTransferType         The data transfer type.
	 * @param configurationId          The data management configuration ID.
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 *                                 Used to identify the S3 archive the
	 *                                 data-object is stored in. This is only
	 *                                 applicable for S3 archives, not POSIX
	 * @return A data transfer authenticated token.
	 * @throws HpcException If it failed to obtain an authentication token.
	 */
	private Object getAuthenticatedToken(HpcDataTransferType dataTransferType, String configurationId,
			String s3ArchiveConfigurationId) throws HpcException {
		HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
		if (invoker == null) {
			throw new HpcException("Unknown user", HpcErrorType.UNEXPECTED_ERROR);
		}

		// Get the data transfer configuration (Globus or S3).
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);

		HpcIntegratedSystemAccount dataTransferSystemAccount = null;
		if (dataTransferType.equals(HpcDataTransferType.GLOBUS)) {
			// Obtain the system account to be used.
			dataTransferSystemAccount = systemAccountLocator.getSystemAccount(dataTransferType, configurationId);
			// Search for an existing token that is not expired for this account.
			Iterator<HpcDataTransferAuthenticatedToken> tokenItr = dataTransferAuthenticatedTokens.iterator();
			while (tokenItr.hasNext()) {
				HpcDataTransferAuthenticatedToken authenticatedToken = tokenItr.next();
				if (authenticatedToken.getDataTransferType().equals(dataTransferType)
						&& authenticatedToken.getConfigurationId().equals(configurationId)
						&& authenticatedToken.getSystemAccountId().equals(dataTransferSystemAccount.getUsername())) {
					if (authenticatedToken.getExpiry().after(Calendar.getInstance())) {
						logger.info("Using stored globus access token: {}, expires: {}",
								dataTransferSystemAccount.getUsername(),
								dateFormat.format(authenticatedToken.getExpiry().getTime()));
						return authenticatedToken.getDataTransferAuthenticatedToken();
					} else {
						// Token has expired, remove it from the list.
						tokenItr.remove();
					}
				}
			}
		} else {
			// Search for an existing token.
			for (HpcDataTransferAuthenticatedToken authenticatedToken : invoker.getDataTransferAuthenticatedTokens()) {
				if (authenticatedToken.getDataTransferType().equals(dataTransferType)
						&& authenticatedToken.getConfigurationId().equals(configurationId)
						&& (authenticatedToken.getS3ArchiveConfigurationId() == null || authenticatedToken
								.getS3ArchiveConfigurationId().equals(dataTransferConfiguration.getId()))) {
					return authenticatedToken.getDataTransferAuthenticatedToken();
				}
			}
		}

		// No authenticated token found for this request. Create one.
		if (dataTransferType.equals(HpcDataTransferType.S_3)) {
			dataTransferSystemAccount = systemAccountLocator
					.getSystemAccount(dataTransferConfiguration.getArchiveProvider());
		}

		if (dataTransferSystemAccount == null) {
			throw new HpcException("System account not registered for " + dataTransferType.value(),
					HpcErrorType.UNEXPECTED_ERROR);
		}

		// Authenticate with the data transfer system.
		Object token = dataTransferProxies.get(dataTransferType).authenticate(dataTransferSystemAccount,
				dataTransferConfiguration.getUrlOrRegion(), dataTransferConfiguration.getEncryptionAlgorithm(),
				dataTransferConfiguration.getEncryptionKey());
		if (token == null) {
			throw new HpcException("Invalid data transfer account credentials", HpcErrorType.DATA_TRANSFER_ERROR,
					dataTransferSystemAccount.getIntegratedSystem());
		}

		// Store token.
		HpcDataTransferAuthenticatedToken authenticatedToken = new HpcDataTransferAuthenticatedToken();
		authenticatedToken.setDataTransferAuthenticatedToken(token);
		authenticatedToken.setDataTransferType(dataTransferType);
		authenticatedToken.setConfigurationId(configurationId);
		authenticatedToken.setS3ArchiveConfigurationId(dataTransferConfiguration.getId());
		authenticatedToken.setSystemAccountId(dataTransferSystemAccount.getUsername());
		if (dataTransferType.equals(HpcDataTransferType.GLOBUS)) {
			// Calculate the expiration date.
			Calendar tokenExpiration = Calendar.getInstance();
			tokenExpiration.add(Calendar.MINUTE, globusTokenExpirationPeriod);
			authenticatedToken.setExpiry(tokenExpiration);
			// Store token in object
			dataTransferAuthenticatedTokens.add(authenticatedToken);
			logger.info("Obtained new globus access token: {}, expires: {}", dataTransferSystemAccount.getUsername(),
					dateFormat.format(authenticatedToken.getExpiry().getTime()));
		} else {
			// Store token on the request context.
			invoker.getDataTransferAuthenticatedTokens().add(authenticatedToken);
			HpcRequestContext.setRequestInvoker(invoker);
		}

		return token;
	}

	/**
	 * Get the data transfer authenticated token from cache.
	 *
	 * @param token The data transfer authenticated token.
	 * @return A data transfer authenticated token.
	 * @throws HpcException If it failed to find an authentication token.
	 */
	private HpcDataTransferAuthenticatedToken getDataTransferAuthenticatedToken(Object token) throws HpcException {

		// Get the DataTransferAuthenticatedToken for this token
		HpcDataTransferAuthenticatedToken dataTransferAuthenticatedToken = null;

		// Search for stored system account for this token.
		Iterator<HpcDataTransferAuthenticatedToken> tokenItr = dataTransferAuthenticatedTokens.iterator();
		while (tokenItr.hasNext()) {
			HpcDataTransferAuthenticatedToken authenticatedToken = tokenItr.next();
			if (authenticatedToken.getDataTransferAuthenticatedToken().equals(token)) {
				logger.info("Found stored globus access token: {}", authenticatedToken.getSystemAccountId());
				dataTransferAuthenticatedToken = authenticatedToken;
				break;
			}
		}

		if (dataTransferAuthenticatedToken == null) {
			throw new HpcException("System account not found for token " + token.toString(),
					HpcErrorType.UNEXPECTED_ERROR);
		}

		return dataTransferAuthenticatedToken;
	}

	/**
	 * Create an empty file.
	 *
	 * @param filePath The file's path
	 * @return The created file.
	 * @throws HpcException on service failure.
	 */
	private File createFile(String filePath) throws HpcException {
		File file = new File(filePath);
		try {
			FileUtils.touch(file);

		} catch (IOException e) {
			throw new HpcException("Failed to create a file: " + filePath, HpcErrorType.DATA_TRANSFER_ERROR, e);
		}

		return file;
	}

	/**
	 * Create an empty file placed in the download folder.
	 *
	 * @return The created file.
	 * @throws HpcException on service failure.
	 */
	private File createDownloadFile() throws HpcException {
		return createFile(downloadDirectory + "/" + UUID.randomUUID().toString());
	}

	/**
	 * Upload a data object.
	 *
	 * @param uploadRequest            The data upload request.
	 * @param configurationId          The data management configuration ID.
	 * @param s3ArchiveConfigurationId The S3 archive configuration ID.
	 * @return A data object upload response.
	 * @throws HpcException on service failure.
	 */
	private HpcDataObjectUploadResponse uploadDataObject(HpcDataObjectUploadRequest uploadRequest,
			String configurationId, String s3ArchiveConfigurationId) throws HpcException {
		// Determine the data transfer type to use in this upload request (i.e. Globus
		// or S3).
		HpcDataTransferType dataTransferType = getUploadDataTransferType(uploadRequest, configurationId);

		// Confirm that Globus can accept the upload request at this time.
		if (uploadRequest.getGlobusUploadSource() != null
				&& !acceptsTransferRequests(dataTransferType, configurationId, s3ArchiveConfigurationId)) {
			// Globus is busy. Queue the request (upload status set to 'RECEIVED'),
			// and the upload will be performed later by a scheduled task.
			HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
			uploadResponse.setDataTransferType(dataTransferType);
			uploadResponse.setSourceSize(uploadRequest.getSourceSize());
			uploadResponse.setUploadSource(uploadRequest.getGlobusUploadSource().getSourceLocation());
			uploadResponse.setDataTransferStarted(Calendar.getInstance());
			uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.RECEIVED);
			uploadResponse.setDataTransferMethod(HpcDataTransferUploadMethod.GLOBUS);
			return uploadResponse;
		}

		if (uploadRequest.getFileSystemUploadSource() != null) {
			// The upload from a file system source is done in a scheduled task.
			HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
			uploadResponse.setDataTransferType(dataTransferType);
			uploadResponse.setSourceSize(uploadRequest.getSourceSize());
			uploadResponse.setSourcePermissions(uploadRequest.getSourcePermissions());
			uploadResponse.setUploadSource(uploadRequest.getFileSystemUploadSource().getSourceLocation());
			uploadResponse.setDataTransferStarted(Calendar.getInstance());
			uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.FILE_SYSTEM_READY);
			uploadResponse.setDataTransferMethod(HpcDataTransferUploadMethod.FILE_SYSTEM);
			return uploadResponse;
		}

		// Get the data transfer configuration.
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);

		// In the case of sync upload using a Globus data transfer proxy, there is no
		// need to login to Globus
		// since the file is simply stored to the file system. This is to support the
		// scenario of POSIX archive with no
		// Globus use (just sync upload/download capability).
		boolean globusSyncUpload = dataTransferType.equals(HpcDataTransferType.GLOBUS)
				&& uploadRequest.getSourceFile() != null;

		// Instantiate a progress listener for upload from AWS S3.
		HpcDataTransferProgressListener progressListener = null;
		if (uploadRequest.getS3UploadSource() != null) {
			progressListener = new HpcStreamingUpload(uploadRequest.getPath(), uploadRequest.getDataObjectId(),
					uploadRequest.getSourceSize(), metadataService, securityService, dataRegistrationDAO, this, false);
		}

		// // Instantiate a progress listener for uploading a file staged on DME server
		// (Sync, Globus 2nd Hop, FileSystem).
		if (uploadRequest.getSourceFile() != null && !globusSyncUpload) {
			progressListener = new HpcStagedFileUpload(uploadRequest.getPath(), uploadRequest.getDataObjectId(),
					uploadRequest.getSourceSize(), this);
		}

		// For uploads from Google (Drive or Cloud storage), we need to generate an
		// input stream to the source file,
		// create a progress listener and persist the access token in case the upload
		// needs to be restarted following a server restart.
		boolean googleUpload = false;
		HpcDataTransferType inputStreamGenerator = null;
		HpcStreamingUploadSource inputStreamSource = null;
		if (uploadRequest.getGoogleDriveUploadSource() != null) {
			googleUpload = true;
			inputStreamGenerator = HpcDataTransferType.GOOGLE_DRIVE;
			inputStreamSource = uploadRequest.getGoogleDriveUploadSource();
		} else if (uploadRequest.getGoogleCloudStorageUploadSource() != null) {
			googleUpload = true;
			inputStreamGenerator = HpcDataTransferType.GOOGLE_CLOUD_STORAGE;
			inputStreamSource = uploadRequest.getGoogleCloudStorageUploadSource();
		}
		if (googleUpload) {
			HpcDataTransferProxy dataTransferProxy = dataTransferProxies.get(inputStreamGenerator);
			inputStreamSource.setSourceInputStream(dataTransferProxy.generateDownloadInputStream(
					dataTransferProxy.authenticate(inputStreamSource.getAccessToken()),
					inputStreamSource.getSourceLocation()));
			progressListener = new HpcStreamingUpload(uploadRequest.getPath(), uploadRequest.getDataObjectId(),
					uploadRequest.getSourceSize(), metadataService, securityService, dataRegistrationDAO, this, true);
			dataRegistrationDAO.upsertGoogleAccessToken(uploadRequest.getDataObjectId(),
					inputStreamSource.getAccessToken());
		}

		// Upload the data object using the appropriate data transfer system proxy and
		// archive
		// configuration.

		Object authenticatedToken = !globusSyncUpload
				? getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId)
				: null;
		HpcDataObjectUploadResponse dataObjectUploadResponse = dataTransferProxies.get(dataTransferType)
				.uploadDataObject(authenticatedToken, uploadRequest,
						dataTransferConfiguration.getBaseArchiveDestination(),
						dataTransferConfiguration.getUploadRequestURLExpiration(), progressListener,
						generateArchiveMetadata(configurationId, uploadRequest.getDataObjectId(),
								uploadRequest.getUserId()),
						dataTransferConfiguration.getEncryptedTransfer(), dataTransferConfiguration.getStorageClass());

		// If data tranfer type is Globus, add to HPC_GLOBUS_TRANSFER_TASK
		if (dataTransferType.equals(HpcDataTransferType.GLOBUS) && !globusSyncUpload) {
			HpcGlobusTransferTask globusRequest = new HpcGlobusTransferTask();
			globusRequest.setDataTransferRequestId(dataObjectUploadResponse.getDataTransferRequestId());
			globusRequest.setGlobusAccount(getDataTransferAuthenticatedToken(authenticatedToken).getSystemAccountId());
			globusRequest.setPath(uploadRequest.getPath());
			globusRequest.setDownload(false);
			globusTransferDAO.insertRequest(globusRequest);
		}

		return dataObjectUploadResponse;
	}

	/**
	 * Validate upload source file location.
	 *
	 * @param globusUploadSource             The Globus source to validate.
	 * @param s3UploadSource                 The S3 source to validate.
	 * @param googleDriveUploadSource        The Google Drive source to validate.
	 * @param googleCloudStorageUploadSource The Google Cloud Storage source to
	 *                                       validate.
	 * @param fileSystemUploadSource         The File System (DME server NAS) source
	 *                                       to validate.
	 * @param sourceFile                     The source file to validate.
	 * @param configurationId                The configuration ID (needed to
	 *                                       determine the archive connection
	 *                                       config).
	 * @return the upload source attributes.
	 * @throws HpcException if the upload source location doesn't exist, or not
	 *                      accessible, or it's a directory.
	 */
	public HpcPathAttributes validateUploadSourceFileLocation(HpcUploadSource globusUploadSource,
			HpcStreamingUploadSource s3UploadSource, HpcStreamingUploadSource googleDriveUploadSource,
			HpcStreamingUploadSource googleCloudStorageUploadSource, HpcUploadSource fileSystemUploadSource,
			File sourceFile, String configurationId) throws HpcException {
		HpcPathAttributes pathAttributes = null;
		if (sourceFile != null) {
			pathAttributes = new HpcPathAttributes();
			pathAttributes.setSize(sourceFile.length());
			return pathAttributes;
		}

		HpcFileLocation sourceFileLocation = null;
		if (globusUploadSource != null) {
			sourceFileLocation = globusUploadSource.getSourceLocation();
			pathAttributes = getPathAttributes(HpcDataTransferType.GLOBUS, sourceFileLocation, true, configurationId,
					null);
		} else if (s3UploadSource != null) {
			if (s3UploadSource.getSourceSize() != null) {
				// When source URL + size are provided, we skip the source validation because
				// this was done before at the time
				// the URL was generated.
				pathAttributes = new HpcPathAttributes();
				pathAttributes.setSize(s3UploadSource.getSourceSize());
				return pathAttributes;
			}
			sourceFileLocation = s3UploadSource.getSourceLocation();
			pathAttributes = getPathAttributes(s3UploadSource.getAccount(), sourceFileLocation, true);

		} else if (googleDriveUploadSource != null) {
			sourceFileLocation = googleDriveUploadSource.getSourceLocation();
			pathAttributes = getPathAttributes(HpcDataTransferType.GOOGLE_DRIVE,
					googleDriveUploadSource.getAccessToken(), sourceFileLocation, true);

		} else if (googleCloudStorageUploadSource != null) {
			sourceFileLocation = googleCloudStorageUploadSource.getSourceLocation();
			pathAttributes = getPathAttributes(HpcDataTransferType.GOOGLE_CLOUD_STORAGE,
					googleCloudStorageUploadSource.getAccessToken(), sourceFileLocation, true);

		} else if (fileSystemUploadSource != null) {
			sourceFileLocation = fileSystemUploadSource.getSourceLocation();
			pathAttributes = getPathAttributes(sourceFileLocation);

		} else {
			// No source to validate. It's a request to generate an upload URL.
			return null;
		}

		// Validate source file accessible
		if (!pathAttributes.getIsAccessible()) {
			throw new HpcException("Source file location not accessible: " + sourceFileLocation.getFileContainerId()
					+ ":" + sourceFileLocation.getFileId(), HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate source file exists.
		if (!pathAttributes.getExists()) {
			throw new HpcException("Source file location doesn't exist: " + sourceFileLocation.getFileContainerId()
					+ ":" + sourceFileLocation.getFileId(), HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate source file is not a directory.
		if (pathAttributes.getIsDirectory()) {
			throw new HpcException("Source file location is a directory: " + sourceFileLocation.getFileContainerId()
					+ ":" + sourceFileLocation.getFileId(), HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return pathAttributes;
	}

	private void checkForDuplicateCollectionDownloadRequests(String path,
			HpcGlobusDownloadDestination globusDownloadDestination) throws HpcException {

		if (path != null && globusDownloadDestination.getDestinationLocation() != null) {
			String endpoint = globusDownloadDestination.getDestinationLocation().getFileContainerId();

			// Is there an existing transaction for same endpoint and same collection, if so
			// indicate error
			if (endpoint != null) {
				int tasksInProgressCount = getCollectionDownloadRequestsCountByPathAndEndpoint(path, endpoint);
				if (tasksInProgressCount > 0) {
					// Another download task in in-process for downloading the same collection to
					// the same destination endpoint.
					logger.error("collection download task: duplicate request for path {} and endpoint {}", path,
							endpoint);
					throw new HpcException("A download request has already been submitted for downloading " + path
							+ " to endpoint " + endpoint, HpcErrorType.INVALID_REQUEST_INPUT);
				}
			}
		}
	}

	/**
	 * Validate download destination.
	 *
	 * @param globusDownloadDestination             The user requested Glopbus
	 *                                              download destination.
	 * @param s3DownloadDestination                 The user requested S3 download
	 *                                              destination.
	 * @param googleDriveDownloadDestination        The user requested Google Drive
	 *                                              download destination.
	 * @param googleCloudStorageDownloadDestination The user requested Google Cloud
	 *                                              Storage download destination.
	 * @param asperaDownloadDestination             The user requested Aspera
	 *                                              download destination.
	 * @param boxDownloadDestination                The user requested Box download
	 *                                              destination.
	 * @param synchronousDownloadFilter             (Optional) synchronous download
	 *                                              filter to extract specific files
	 *                                              from a data object that is
	 *                                              'compressed archive' such as
	 *                                              ZIP.
	 * @param configurationId                       The configuration ID.
	 * @param bulkDownload                          True if this is a request to
	 *                                              download a list of files or a
	 *                                              collection, or false if this is
	 *                                              a request to download a single
	 *                                              data object.
	 * @throws HpcException if the download destination is invalid
	 */
	private void validateDownloadDestination(HpcGlobusDownloadDestination globusDownloadDestination,
			HpcS3DownloadDestination s3DownloadDestination, HpcGoogleDownloadDestination googleDriveDownloadDestination,
			HpcGoogleDownloadDestination googleCloudStorageDownloadDestination,
			HpcAsperaDownloadDestination asperaDownloadDestination, HpcBoxDownloadDestination boxDownloadDestination,
			HpcSynchronousDownloadFilter synchronousDownloadFilter, String configurationId, boolean bulkDownload)
			throws HpcException {
		// Validate a single destination is provided.
		int destinations = 0;
		if (globusDownloadDestination != null) {
			destinations++;
		}
		if (s3DownloadDestination != null) {
			destinations++;
		}
		if (googleDriveDownloadDestination != null) {
			destinations++;
		}
		if (googleCloudStorageDownloadDestination != null) {
			destinations++;
		}
		if (asperaDownloadDestination != null) {
			destinations++;
		}
		if (boxDownloadDestination != null) {
			destinations++;
		}
		if (destinations > 1) {
			throw new HpcException(
					"Multiple download destinations provided (Globus, S3, Google Drive / Cloud Storage, Aspera, Box)",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate an optional synchronous download filter is provided only for a
		// synchronous download request.
		if (destinations == 1 && synchronousDownloadFilter != null) {
			throw new HpcException(
					"A download destination (Globus, S3, Google Drive / Cloud Storage, Aspera, Box) provided w/ synchronous download filter",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the destination for collection/bulk download is provided.
		if (bulkDownload && destinations == 0) {
			throw new HpcException(
					"No download destination provided (Globus, S3, Google Drive / Cloud Storage, Aspera, Box)",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the Globus destination.
		if (globusDownloadDestination != null) {
			if (!isValidFileLocation(globusDownloadDestination.getDestinationLocation())) {
				throw new HpcException("Invalid Globus download destination", HpcErrorType.INVALID_REQUEST_INPUT);
			}

			// Default overwrite to false if not provided.
			if (globusDownloadDestination.getDestinationOverwrite() == null) {
				// Default destination overwrite is false.
				globusDownloadDestination.setDestinationOverwrite(false);
			}

			// For bulk download - verify the globus file location.
			if (bulkDownload) {
				validateGlobusDownloadDestinationFileLocation(HpcDataTransferType.GLOBUS,
						globusDownloadDestination.getDestinationLocation(), false, true, configurationId);
			}
		}

		// Validate the S3 destination.
		if (s3DownloadDestination != null) {
			if (!isValidFileLocation(s3DownloadDestination.getDestinationLocation())) {
				throw new HpcException("Invalid S3 download destination location", HpcErrorType.INVALID_REQUEST_INPUT);
			}

			HpcDomainValidationResult validationResult = isValidS3Account(s3DownloadDestination.getAccount());
			if (!validationResult.getValid()) {
				throw new HpcException("Invalid S3 account: " + validationResult.getMessage(),
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			HpcDataTransferProxy dataTransferProxy = dataTransferProxies.get(HpcDataTransferType.S_3);
			boolean s3BucketAccessible = true;
			try {
				s3BucketAccessible = dataTransferProxy
						.getPathAttributes(dataTransferProxy.authenticate(s3DownloadDestination.getAccount()),
								s3DownloadDestination.getDestinationLocation(), false)
						.getIsAccessible();
			} catch (HpcException e) {
				throw new HpcException("Failed to locate S3 bucket: "
						+ s3DownloadDestination.getDestinationLocation().getFileContainerId() + " [" + e.getMessage()
						+ "]", HpcErrorType.INVALID_REQUEST_INPUT, e);
			}
			if (!s3BucketAccessible) {
				throw new HpcException(
						"Failed to access S3 bucket: "
								+ s3DownloadDestination.getDestinationLocation().getFileContainerId(),
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate the Google Drive destination.
		if (googleDriveDownloadDestination != null) {
			if (!isValidFileLocation(googleDriveDownloadDestination.getDestinationLocation())) {
				throw new HpcException("Invalid Google Drive download destination location",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			if (!googleDriveDownloadDestination.getDestinationLocation().getFileContainerId()
					.equals(MY_GOOGLE_DRIVE_ID)) {
				// At this point we only support download to personal drive and expect the
				// container ID to
				// be "MyDrive"
				throw new HpcException("Invalid file container ID. Only 'MyDrive' is supported",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			if (StringUtils.isEmpty(googleDriveDownloadDestination.getAccessToken())) {
				throw new HpcException("Invalid Google Drive access token", HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate the Google Cloud Storage destination.
		if (googleCloudStorageDownloadDestination != null) {
			if (!isValidFileLocation(googleCloudStorageDownloadDestination.getDestinationLocation())) {
				throw new HpcException("Invalid Google Cloud Storage download destination location",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			if (StringUtils.isEmpty(googleCloudStorageDownloadDestination.getAccessToken())) {
				throw new HpcException("Invalid Google Cloud Storage access token", HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate the Aspera destination.
		if (asperaDownloadDestination != null) {
			if (!isValidFileLocation(asperaDownloadDestination.getDestinationLocation())) {
				throw new HpcException("Invalid Aspera download destination location",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			HpcDomainValidationResult validationResult = isValidAsperaAccount(asperaDownloadDestination.getAccount());
			if (!validationResult.getValid()) {
				throw new HpcException("Invalid Aspera account: " + validationResult.getMessage(),
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate the Box destination.
		if (boxDownloadDestination != null) {
			if (!isValidFileLocation(boxDownloadDestination.getDestinationLocation())) {
				throw new HpcException("Invalid Box download destination location", HpcErrorType.INVALID_REQUEST_INPUT);
			}

			if (!boxDownloadDestination.getDestinationLocation().getFileContainerId().equals(MY_BOX_ID)) {
				// At this point we only support endpoint of Box to be 'MyBox'
				throw new HpcException("Invalid file container ID. Only 'MyBox' is supported",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			if (StringUtils.isEmpty(boxDownloadDestination.getAccessToken())) {
				throw new HpcException("Invalid Box access token", HpcErrorType.INVALID_REQUEST_INPUT);
			}

			if (StringUtils.isEmpty(boxDownloadDestination.getRefreshToken())) {
				throw new HpcException("Invalid Box refresh token", HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate the synchronous download filter.
		if (synchronousDownloadFilter != null) {
			if (synchronousDownloadFilter.getCompressedArchiveType() == null) {
				throw new HpcException("No / Invalid compressed archive type provided in synchronous download filter",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			if (synchronousDownloadFilter.getIncludePatterns().isEmpty()) {
				throw new HpcException("No patterns provided in synchronous download filter",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			if (synchronousDownloadFilter.getPatternType() == null) {
				// Pattern type is optional and the default is SIMPLE.
				synchronousDownloadFilter.setPatternType(HpcPatternType.SIMPLE);
			}
		}
	}

	/**
	 * Validate Globus download destination file location.
	 *
	 * @param dataTransferType          The data transfer type.
	 * @param destinationLocation       The file location to validate.
	 * @param validateExistsAsDirectory If true, an exception will thrown if the
	 *                                  path is an existing directory.
	 * @param validateExistsAsFile      If true, an exception will thrown if the
	 *                                  path is an existing file.
	 * @param configurationId           The configuration ID (needed to determine
	 *                                  the archive connection config).
	 * @return The path attributes.
	 * @throws HpcException if the destination location not accessible or exist as a
	 *                      file.
	 */
	private HpcPathAttributes validateGlobusDownloadDestinationFileLocation(HpcDataTransferType dataTransferType,
			HpcFileLocation destinationLocation, boolean validateExistsAsDirectory, boolean validateExistsAsFile,
			String configurationId) throws HpcException {
		HpcPathAttributes pathAttributes = getPathAttributes(dataTransferType, destinationLocation, false,
				configurationId, null);

		// Validate destination file accessible.
		if (!pathAttributes.getIsAccessible()) {
			throw new HpcException("Destination file location not accessible: "
					+ destinationLocation.getFileContainerId() + ":" + destinationLocation.getFileId(),
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate destination file doesn't exist as a file.
		if (validateExistsAsFile && pathAttributes.getIsFile()) {
			throw new HpcException("A file already exists with the same destination path: "
					+ destinationLocation.getFileContainerId() + ":" + destinationLocation.getFileId(),
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate destination file doesn't exist as a directory.
		if (validateExistsAsDirectory && pathAttributes.getIsDirectory()) {
			throw new HpcException(
					"A directory already exists with the same destination path: "
							+ destinationLocation.getFileContainerId() + ":" + destinationLocation.getFileId(),
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return pathAttributes;
	}

	/**
	 * Filter scan items based on include/exclude patterns.
	 *
	 * @param scanItems        The list of items to filter (items not matched will
	 *                         be removed from this list).
	 * @param includePatterns  The include patterns.
	 * @param excludePatterns  The exclude patterns.
	 * @param patternType      The type of patterns provided.
	 * @param dataTransferType The data transfer type (Globus or S3) that provided
	 *                         the directory scan items.
	 * @throws HpcException on service failure
	 */
	private void filterScanItems(List<HpcDirectoryScanItem> scanItems, List<String> includePatterns,
			List<String> excludePatterns, HpcPatternType patternType, HpcDataTransferType dataTransferType)
			throws HpcException {
		if (includePatterns.isEmpty() && excludePatterns.isEmpty()) {
			// No patterns provided.
			return;
		}

		// Validate pattern is provided.
		if (patternType == null) {
			throw new HpcException("Null directory scan pattern type", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// If Globus performed the directory scan or scan on local DME server NAS, then
		// all the items start with '/'.
		// We will make sure the pattern also starts with '/'. S3 items don't start with
		// '/'
		boolean prefixPattern = dataTransferType == null || dataTransferType.equals(HpcDataTransferType.GLOBUS);

		// Compile include patterns.
		List<Pattern> compiledIncludePatterns = pattern.compile(includePatterns, patternType, prefixPattern);

		// Compile exclude patterns.
		List<Pattern> compiledExcludePatterns = pattern.compile(excludePatterns, patternType, prefixPattern);

		// Match the items against the patterns.
		ListIterator<HpcDirectoryScanItem> iter = scanItems.listIterator();
		while (iter.hasNext()) {
			// Get the path of this data object registration item.
			String path = iter.next().getFilePath();

			// Match the patterns.
			if (!((compiledIncludePatterns.isEmpty() || pattern.matches(compiledIncludePatterns, path))
					&& (compiledExcludePatterns.isEmpty() || !pattern.matches(compiledExcludePatterns, path)))) {
				iter.remove();
			}
		}
	}

	/**
	 * Get a file path for a given file ID and baseArchive.
	 *
	 * @param fileId      The file ID.
	 * @param baseArchive The base archive.
	 * @return a file path.
	 */
	private String getFilePath(String fileId, HpcArchive baseArchive) {
		return fileId.replaceFirst(baseArchive.getFileLocation().getFileId(), baseArchive.getDirectory());
	}

	/**
	 * Determine the data transfer type to use on an upload request.
	 *
	 * @param uploadRequest   The upload request to determine the data transfer type
	 *                        to use.
	 * @param configurationId The data management configuration ID.
	 * @return The appropriate data transfer type to use in this upload request.
	 * @throws HpcException on service failure.
	 */
	private HpcDataTransferType getUploadDataTransferType(HpcDataObjectUploadRequest uploadRequest,
			String configurationId) throws HpcException {
		// Determine the data transfer type to use in this upload request (i.e. Globus
		// or S3).
		HpcDataTransferType archiveDataTransferType = dataManagementConfigurationLocator
				.getArchiveDataTransferType(configurationId);

		if (uploadRequest.getGlobusUploadSource() != null) {
			// It's an asynchronous upload request w/ Globus. This is supported by both
			// S3 and POSIX archives.
			return HpcDataTransferType.GLOBUS;
		}

		if (uploadRequest.getS3UploadSource() != null) {
			// It's an asynchronous upload request from AWS S3. This is only supported S3
			// archive.
			if (archiveDataTransferType.equals(HpcDataTransferType.GLOBUS)) {
				throw new HpcException("S3 upload source not supported by POSIX archive",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			return HpcDataTransferType.S_3;
		}

		if (uploadRequest.getGoogleDriveUploadSource() != null
				|| uploadRequest.getGoogleCloudStorageUploadSource() != null) {
			// It's an asynchronous upload request from Google Drive or Google Cloud
			// Storage. This is only supported
			// S3 archive.
			if (archiveDataTransferType.equals(HpcDataTransferType.GLOBUS)) {
				throw new HpcException(
						"Google Drive / Google Cloud Storage upload source not supported by POSIX archive",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			// We use the S_3 data transfer proxy to stream files from Google Drive / Google
			// Cloud Storage to an S3
			// archive.
			return HpcDataTransferType.S_3;
		}

		if (uploadRequest.getSourceFile() != null || uploadRequest.getFileSystemUploadSource() != null) {
			// Sync and file system uploads supported by both S3 & POSIX archives. Use the
			// configured archive data transfer.
			return archiveDataTransferType;
		}
		if (uploadRequest.getGenerateUploadRequestURL()) {
			// It's a request to generate upload URL. This is only supported S3 archive.
			if (archiveDataTransferType.equals(HpcDataTransferType.GLOBUS)) {
				throw new HpcException("Generate upload URL not supported by POSIX archive",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			return HpcDataTransferType.S_3;

		} else {
			// Could not determine data transfer type.
			throw new HpcException("Could not determine data transfer type", HpcErrorType.UNEXPECTED_ERROR);
		}
	}

	/**
	 * Calculate and validate a Globus download destination location.
	 *
	 * @param destinationLocation  The destination location requested by the caller.
	 * @param destinationOverwrite If true, the requested destination location will
	 *                             be overwritten if it exists.
	 * @param dataTransferType     The data transfer type to create the request.
	 * @param path                 The data object path.
	 * @param configurationId      The configuration ID (needed to determine the
	 *                             archive connection config).
	 * @return The calculated destination file location. The source file name is
	 *         added if the caller provided a directory destination.
	 * @throws HpcException on service failure.
	 */
	private HpcFileLocation calculateGlobusDownloadDestinationFileLocation(HpcFileLocation destinationLocation,
			boolean destinationOverwrite, HpcDataTransferType dataTransferType, String path, String configurationId)
			throws HpcException {
		// Validate the download destination location.
		HpcPathAttributes pathAttributes = validateGlobusDownloadDestinationFileLocation(dataTransferType,
				destinationLocation, false, !destinationOverwrite, configurationId);

		// Calculate the destination.
		if (pathAttributes.getIsDirectory() || destinationLocation.getFileId().endsWith("/")) {
			// Caller requested to download to a directory. Append the source file name.
			HpcFileLocation calcDestination = new HpcFileLocation();
			calcDestination.setFileContainerId(destinationLocation.getFileContainerId());
			calcDestination.setFileId(
					toNormalizedPath(destinationLocation.getFileId() + path.substring(path.lastIndexOf('/'))));

			// Validate the calculated download destination.
			validateGlobusDownloadDestinationFileLocation(dataTransferType, calcDestination, true,
					!destinationOverwrite, configurationId);

			return calcDestination;

		} else {
			return destinationLocation;
		}
	}

	/**
	 * Perform a synchronous download from either a S3 or POSIX archive.
	 *
	 * @param downloadRequest           The data object download request.
	 * @param response                  The download response object. This method
	 *                                  sets download task id and destination
	 *                                  location on the response.
	 * @param dataTransferConfiguration The data transfer configuration.
	 * @param synchronousDownloadFilter (Optional) synchronous download filter to
	 *                                  extract specific files from a data object
	 *                                  that is 'compressed archive' such as ZIP.
	 * @throws HpcException on service failure.
	 */
	private void performSynchronousDownload(HpcDataObjectDownloadRequest downloadRequest,
			HpcDataObjectDownloadResponse response, HpcDataTransferConfiguration dataTransferConfiguration,
			HpcSynchronousDownloadFilter synchronousDownloadFilter) throws HpcException {
		// Validate max file size not exceeded.
		if (synchronousDownloadFilter == null && maxSyncDownloadFileSize != null
				&& downloadRequest.getSize() > maxSyncDownloadFileSize) {
			logger.error("File size of {} for path {} exceeds the sync download limit", downloadRequest.getSize(),
					downloadRequest.getPath());
			throw new HpcException("File size exceeds the sync download limit",
					HpcRequestRejectReason.INVALID_DOWNLOAD_REQUEST);
		}

		// Capture the time download started.
		Calendar created = Calendar.getInstance();

		// Create a destination file on the local file system for the synchronous
		// download.
		downloadRequest.setFileDestination(createDownloadFile());
		response.setDestinationFile(downloadRequest.getFileDestination());

		// Create a task ID for this download request.
		response.setDownloadTaskId(UUID.randomUUID().toString());

		// Set sudo Password (This is needed in the case of POSIX sync download).
		downloadRequest.setSudoPassword(systemAccountLocator.getSystemAccount(HpcIntegratedSystem.IRODS).getPassword());

		// Perform the synchronous download.
		dataTransferProxies.get(downloadRequest.getDataTransferType()).downloadDataObject(
				getAuthenticatedToken(downloadRequest.getDataTransferType(), downloadRequest.getConfigurationId(),
						downloadRequest.getS3ArchiveConfigurationId()),
				downloadRequest, dataTransferConfiguration.getBaseArchiveDestination(), null,
				dataTransferConfiguration.getEncryptedTransfer());

		// If a filter was requested, the data object is expected to be a compressed
		// archive file (ZIP, TAR, TGZ).
		// We will return a filtered compressed archive based on patterns provided.
		if (synchronousDownloadFilter != null) {
			File filteredCompressedArchive = createDownloadFile();
			try {
				if (compressedArchiveExtractor.extract(downloadRequest.getFileDestination(), filteredCompressedArchive,
						synchronousDownloadFilter.getCompressedArchiveType(),
						synchronousDownloadFilter.getIncludePatterns(),
						synchronousDownloadFilter.getPatternType()) > 0) {
					// Validate max file size not exceeded.
					if (maxSyncDownloadFileSize != null
							&& filteredCompressedArchive.length() > maxSyncDownloadFileSize) {
						logger.error("File size of {} for path {} exceeds the sync download limit",
								downloadRequest.getSize(), downloadRequest.getPath());
						throw new HpcException("File size exceeds the sync download limit",
								HpcRequestRejectReason.INVALID_DOWNLOAD_REQUEST);
					}

					response.setDestinationFile(filteredCompressedArchive);

				} else {
					throw new HpcException("Pattern(s) did not match any entry in compressed archive file",
							HpcErrorType.INVALID_REQUEST_INPUT);
				}

			} catch (HpcException e) {
				FileUtils.deleteQuietly(filteredCompressedArchive);
				throw (e);

			} finally {
				FileUtils.deleteQuietly(downloadRequest.getFileDestination());
			}
		}

		// Create a task result object.
		HpcDownloadTaskResult taskResult = new HpcDownloadTaskResult();
		taskResult.setId(response.getDownloadTaskId());
		taskResult.setUserId(downloadRequest.getUserId());
		taskResult.setPath(downloadRequest.getPath());
		HpcFileLocation destinationLocation = new HpcFileLocation();
		destinationLocation.setFileContainerId("Synchronous Download");
		destinationLocation.setFileId("Synchronous Download");
		taskResult.setDestinationLocation(destinationLocation);
		taskResult.setResult(HpcDownloadResult.COMPLETED);
		taskResult.setType(HpcDownloadTaskType.DATA_OBJECT);
		taskResult.setCompletionEvent(false);
		taskResult.setCreated(created);
		taskResult.setSize(downloadRequest.getSize());
		taskResult.setCompleted(Calendar.getInstance());
		taskResult.setFirstHopRetried(false);
		taskResult.setDoc(
				dataManagementService.getDataManagementConfiguration(downloadRequest.getConfigurationId()).getDoc());

		// Persist to the DB.
		dataDownloadDAO.upsertDownloadTaskResult(taskResult);
	}

	/**
	 * Perform a globus asynchronous download. This method submits a download task.
	 *
	 * @param downloadRequest           The data object download request.
	 * @param response                  The download response object. This method
	 *                                  sets download task id and destination
	 *                                  location on the response.
	 * @param dataTransferConfiguration The data transfer configuration.
	 * @throws HpcException on service failure.
	 */
	private void performGlobusAsynchronousDownload(HpcDataObjectDownloadRequest downloadRequest,
			HpcDataObjectDownloadResponse response, HpcDataTransferConfiguration dataTransferConfiguration)
			throws HpcException {
		// Create a download task.
		HpcDataObjectDownloadTask downloadTask = new HpcDataObjectDownloadTask();
		downloadTask.setArchiveLocation(downloadRequest.getArchiveLocation());
		downloadTask.setCompletionEvent(downloadRequest.getCompletionEvent());
		downloadTask.setCollectionDownloadTaskId(downloadRequest.getCollectionDownloadTaskId());
		downloadTask.setConfigurationId(downloadRequest.getConfigurationId());
		downloadTask.setS3ArchiveConfigurationId(downloadRequest.getS3ArchiveConfigurationId());
		downloadTask.setCreated(Calendar.getInstance());
		downloadTask.setDataTransferStatus(
				Optional.ofNullable(dataTransferConfiguration.getHyperfileArchive()).orElse(false)
						? HpcDataTransferDownloadStatus.HYPERFILE_STAGING
						: HpcDataTransferDownloadStatus.RECEIVED);
		downloadTask.setInProcess(false);
		downloadTask.setDataTransferType(HpcDataTransferType.GLOBUS);
		downloadTask.setPercentComplete(0);
		downloadTask.setSize(downloadRequest.getSize());
		HpcGlobusDownloadDestination globusDestination = new HpcGlobusDownloadDestination();
		globusDestination.setDestinationLocation(calculateGlobusDownloadDestinationFileLocation(
				downloadRequest.getGlobusDestination().getDestinationLocation(),
				downloadRequest.getGlobusDestination().getDestinationOverwrite(), HpcDataTransferType.GLOBUS,
				downloadRequest.getPath(), downloadRequest.getConfigurationId()));
		downloadTask.setGlobusDownloadDestination(globusDestination);
		downloadTask.setDestinationType(HpcDataTransferType.GLOBUS);
		downloadTask.setPath(downloadRequest.getPath());
		downloadTask.setRetryTaskId(downloadRequest.getRetryTaskId());
		downloadTask.setUserId(downloadRequest.getUserId());
		downloadTask.setFirstHopRetried(false);
		downloadTask.setRetryUserId(downloadRequest.getRetryUserId());
		downloadTask.setDoc(
				dataManagementService.getDataManagementConfiguration(downloadRequest.getConfigurationId()).getDoc());

		// Persist the download task. The download will be performed by a scheduled task
		// picking up
		// this task in its next scheduled run.
		dataDownloadDAO.createDataObjectDownloadTask(downloadTask);
		response.setDownloadTaskId(downloadTask.getId());
		response.setDestinationLocation(downloadTask.getGlobusDownloadDestination().getDestinationLocation());
	}

	/**
	 * Perform a download request to user's provided AWS S3 destination from POSIX /
	 * S3 archive.
	 *
	 * @param downloadRequest           The data object download request.
	 * @param dataTransferType          The archive's data transfer type.
	 * @param response                  The download response object. This method
	 *                                  sets download task id and destination
	 *                                  location on the response.
	 * @param dataTransferConfiguration The data transfer configuration.
	 * @throws HpcException on service failure.
	 */
	private void performS3AsynchronousDownload(HpcDataObjectDownloadRequest downloadRequest,
			HpcDataTransferType dataTransferType, HpcDataObjectDownloadResponse response,
			HpcDataTransferConfiguration dataTransferConfiguration) throws HpcException {

		HpcStreamingDownload s3Download = new HpcStreamingDownload(downloadRequest, dataDownloadDAO, eventService,
				this);

		Object authenticatedToken = null;
		if (!dataTransferType.equals(HpcDataTransferType.S_3)) {
			// This is a download from POSIX archive. Generate a download URL from POSIX.
			downloadRequest.setArchiveLocationURL(generateDownloadRequestURL(downloadRequest.getPath(),
					downloadRequest.getArchiveLocation(), dataTransferType, downloadRequest.getConfigurationId(),
					downloadRequest.getS3ArchiveConfigurationId()));
		} else {
			authenticatedToken = getAuthenticatedToken(HpcDataTransferType.S_3, downloadRequest.getConfigurationId(),
					downloadRequest.getS3ArchiveConfigurationId());
		}

		// Perform the S3 download (From S3 Archive to User's S3 bucket in AWS or 3rd
		// party provider).
		try {
			dataTransferProxies.get(HpcDataTransferType.S_3).downloadDataObject(authenticatedToken, downloadRequest,
					dataTransferConfiguration.getBaseArchiveDestination(), s3Download,
					dataTransferConfiguration.getEncryptedTransfer());

			// Populate the response object.
			response.setDownloadTaskId(s3Download.getDownloadTask().getId());
			response.setDestinationLocation(
					s3Download.getDownloadTask().getS3DownloadDestination().getDestinationLocation());

		} catch (HpcException e) {
			// Cleanup the download task and rethrow.
			completeDataObjectDownloadTask(s3Download.getDownloadTask(), HpcDownloadResult.FAILED, e.getMessage(),
					Calendar.getInstance(), 0);

			throw (e);
		}
	}

	/**
	 * Perform a download request to user's provided Google Drive destination from
	 * S3 archive.
	 *
	 * @param downloadRequest           The data object download request.
	 * @param response                  The download response object. This method
	 *                                  sets download task id and destination
	 *                                  location on the response.
	 * @param dataTransferConfiguration The data transfer configuration.
	 * @throws HpcException on service failure.
	 */
	private void performGoogleDriveAsynchronousDownload(HpcDataObjectDownloadRequest downloadRequest,
			HpcDataObjectDownloadResponse response, HpcDataTransferConfiguration dataTransferConfiguration)
			throws HpcException {

		HpcStreamingDownload googleDriveDownload = new HpcStreamingDownload(downloadRequest, dataDownloadDAO,
				eventService, this);

		try {
			// Generate a download URL from the S3 archive.
			downloadRequest.setArchiveLocationURL(generateDownloadRequestURL(downloadRequest.getPath(),
					downloadRequest.getArchiveLocation(), HpcDataTransferType.S_3, downloadRequest.getConfigurationId(),
					downloadRequest.getS3ArchiveConfigurationId()));

			// Perform the download (From S3 Archive to User's Google Drive).
			dataTransferProxies.get(HpcDataTransferType.GOOGLE_DRIVE).downloadDataObject(null, downloadRequest,
					dataTransferConfiguration.getBaseArchiveDestination(), googleDriveDownload,
					dataTransferConfiguration.getEncryptedTransfer());

			// Populate the response object.
			response.setDownloadTaskId(googleDriveDownload.getDownloadTask().getId());
			response.setDestinationLocation(
					googleDriveDownload.getDownloadTask().getGoogleDriveDownloadDestination().getDestinationLocation());

		} catch (HpcException e) {
			// Cleanup the download task and rethrow.
			completeDataObjectDownloadTask(googleDriveDownload.getDownloadTask(), HpcDownloadResult.FAILED,
					e.getMessage(), Calendar.getInstance(), 0);

			throw (e);
		}
	}

	/**
	 * Perform a download request to user's provided Google Cloud Storage
	 * destination from S3 archive.
	 *
	 * @param downloadRequest           The data object download request.
	 * @param response                  The download response object. This method
	 *                                  sets download task id and destination
	 *                                  location on the response.
	 * @param dataTransferConfiguration The data transfer configuration.
	 * @throws HpcException on service failure.
	 */
	private void performGoogleCloudStorageAsynchronousDownload(HpcDataObjectDownloadRequest downloadRequest,
			HpcDataObjectDownloadResponse response, HpcDataTransferConfiguration dataTransferConfiguration)
			throws HpcException {

		HpcStreamingDownload googleCloudStorageDownload = new HpcStreamingDownload(downloadRequest, dataDownloadDAO,
				eventService, this);

		try {
			// Generate a download URL from the S3 archive.
			downloadRequest.setArchiveLocationURL(generateDownloadRequestURL(downloadRequest.getPath(),
					downloadRequest.getArchiveLocation(), HpcDataTransferType.S_3, downloadRequest.getConfigurationId(),
					downloadRequest.getS3ArchiveConfigurationId()));

			// Perform the download (From S3 Archive to User's Google Cloud Storage).
			dataTransferProxies.get(HpcDataTransferType.GOOGLE_CLOUD_STORAGE).downloadDataObject(null, downloadRequest,
					dataTransferConfiguration.getBaseArchiveDestination(), googleCloudStorageDownload,
					dataTransferConfiguration.getEncryptedTransfer());

			// Populate the response object.
			response.setDownloadTaskId(googleCloudStorageDownload.getDownloadTask().getId());
			response.setDestinationLocation(googleCloudStorageDownload.getDownloadTask()
					.getGoogleCloudStorageDownloadDestination().getDestinationLocation());

		} catch (HpcException e) {
			// Cleanup the download task and rethrow.
			completeDataObjectDownloadTask(googleCloudStorageDownload.getDownloadTask(), HpcDownloadResult.FAILED,
					e.getMessage(), Calendar.getInstance(), 0);

			throw (e);
		}
	}

	/**
	 * Perform a download request to user's provided Box destination from S3
	 * archive.
	 *
	 * @param downloadRequest           The data object download request.
	 * @param response                  The download response object. This method
	 *                                  sets download task id and destination
	 *                                  location on the response.
	 * @param dataTransferConfiguration The data transfer configuration.
	 * @throws HpcException on service failure.
	 */
	private void performBoxAsynchronousDownload(HpcDataObjectDownloadRequest downloadRequest,
			HpcDataObjectDownloadResponse response, HpcDataTransferConfiguration dataTransferConfiguration)
			throws HpcException {

		HpcStreamingDownload boxDownload = new HpcStreamingDownload(downloadRequest, dataDownloadDAO, eventService,
				this);

		try {
			// Generate a download URL from the S3 archive.
			downloadRequest.setArchiveLocationURL(generateDownloadRequestURL(downloadRequest.getPath(),
					downloadRequest.getArchiveLocation(), HpcDataTransferType.S_3, downloadRequest.getConfigurationId(),
					downloadRequest.getS3ArchiveConfigurationId()));

			// Get authenticated token to the user's box storage
			Object authenticatedToken = dataTransferProxies.get(HpcDataTransferType.BOX).authenticate(
					systemAccountLocator.getSystemAccount(HpcIntegratedSystem.BOX),
					boxDownload.getDownloadTask().getBoxDownloadDestination().getAccessToken(),
					boxDownload.getDownloadTask().getBoxDownloadDestination().getRefreshToken());

			// Update the download task with the latest refresh token
			HpcIntegratedSystemTokens boxTokens = dataTransferProxies.get(HpcDataTransferType.BOX)
					.getIntegratedSystemTokens(authenticatedToken);
			HpcDataObjectDownloadTask downloadTask = boxDownload.getDownloadTask();
			downloadTask.getBoxDownloadDestination().setRefreshToken(boxTokens.getRefreshToken());
			dataDownloadDAO.updateDataObjectDownloadTask(downloadTask);

			// Perform the download (From S3 Archive to User's Box).
			dataTransferProxies.get(HpcDataTransferType.BOX).downloadDataObject(authenticatedToken, downloadRequest,
					dataTransferConfiguration.getBaseArchiveDestination(), boxDownload,
					dataTransferConfiguration.getEncryptedTransfer());

			// Populate the response object.
			response.setDownloadTaskId(boxDownload.getDownloadTask().getId());
			response.setDestinationLocation(
					boxDownload.getDownloadTask().getBoxDownloadDestination().getDestinationLocation());

		} catch (HpcException e) {
			// Cleanup the download task and rethrow.
			completeDataObjectDownloadTask(boxDownload.getDownloadTask(), HpcDownloadResult.FAILED, e.getMessage(),
					Calendar.getInstance(), 0);

			throw (e);
		}
	}

	/**
	 * Perform a 2 hop download. i.e. first stage the data in local DME server
	 * filesystem, and then submit a transfer request to the caller's GLOBUS, or
	 * ASPERA destination. Both first and second hop downloads are performed
	 * asynchronously.
	 *
	 * @param downloadRequest           The data object download request.
	 * @param response                  The download response object. This method
	 *                                  sets download task id and destination
	 *                                  location on the response. it exists.
	 *                                  requested data object.
	 * @param dataTransferConfiguration The data transfer configuration.
	 * @throws HpcException on service failure.
	 */
	private void perform2HopDownload(HpcDataObjectDownloadRequest downloadRequest,
			HpcDataObjectDownloadResponse response, HpcDataTransferConfiguration dataTransferConfiguration)
			throws HpcException {
		HpcSecondHopDownload secondHopDownload = new HpcSecondHopDownload(downloadRequest,
				HpcDataTransferDownloadStatus.IN_PROGRESS);

		// Set the first hop file destination to be the source file of the second hop.
		downloadRequest.setFileDestination(secondHopDownload.getSourceFile());

		// Populate the response object.
		response.setDownloadTaskId(secondHopDownload.getDownloadTask().getId());
		response.setDestinationLocation(
				secondHopDownload.getDownloadTask().getDestinationType().equals(HpcDataTransferType.GLOBUS)
						? secondHopDownload.getDownloadTask().getGlobusDownloadDestination().getDestinationLocation()
						: secondHopDownload.getDownloadTask().getAsperaDownloadDestination().getDestinationLocation());

		try {
			// Reset the download task, so the 1st-hop is picked up by a scheduled-task.
			resetDataObjectDownloadTask(secondHopDownload.getDownloadTask());

			if (!StringUtils.isEmpty(downloadRequest.getCollectionDownloadTaskId())) {
				logger.info(
						"download task: [taskId={}] - 2 Hop download task created from a collection download request [transfer-type={}, destination-type={},"
								+ " path = {}, collection download task = {}]",
						secondHopDownload.downloadTask.getId(), secondHopDownload.downloadTask.getDataTransferType(),
						secondHopDownload.downloadTask.getDestinationType(), secondHopDownload.downloadTask.getPath(),
						downloadRequest.getCollectionDownloadTaskId());
			} else {
				logger.info(
						"download task: [taskId={}] - 2 Hop download task created from a single-file download request [transfer-type={}, destination-type={},"
								+ " path = {}]",
						secondHopDownload.downloadTask.getId(), secondHopDownload.downloadTask.getDataTransferType(),
						secondHopDownload.downloadTask.getDestinationType(), secondHopDownload.downloadTask.getPath());
			}

		} catch (HpcException e) {
			// Cleanup the download task and rethrow.
			completeDataObjectDownloadTask(secondHopDownload.getDownloadTask(), HpcDownloadResult.FAILED,
					e.getMessage(), Calendar.getInstance(), 0);

			throw (e);
		}
	}

	/*
	 * Check if 2-hop download request can be started. It confirms that the server
	 * has enough disk space to store the file. If there is not enough space, the
	 * download task is reset and will be attempted in the next run of the scheduled
	 * task.
	 *
	 * @param secondHopDownload The second hop download instance to validate.
	 * 
	 * @return True if there is enough disk space to start the 2-hop download.
	 */
	class HpcCanPerform2HopDownloadResponse {
		boolean value = true;
		String message = null;
	}

	private HpcCanPerform2HopDownloadResponse canPerfom2HopDownload(HpcSecondHopDownload secondHopDownload)
			throws HpcException {
		HpcCanPerform2HopDownloadResponse response = new HpcCanPerform2HopDownloadResponse();
		int inProcessS3DownloadsForGlobus = dataDownloadDAO.getDataObjectDownloadTasksCountByStatusAndType(
				HpcDataTransferType.S_3, HpcDataTransferType.GLOBUS, HpcDataTransferDownloadStatus.IN_PROGRESS,
				s3DownloadTaskServerId);

		if (maxPermittedS3DownloadsForGlobus <= 0
				|| inProcessS3DownloadsForGlobus <= maxPermittedS3DownloadsForGlobus) {

			int inProgressDownloadsForUserbyPath = dataDownloadDAO
					.getGlobusDataObjectDownloadTasksCountInProgressForUserByPath(
							secondHopDownload.getDownloadTask().getUserId(),
							secondHopDownload.getDownloadTask().getPath());
			if (inProgressDownloadsForUserbyPath <= 1) {
				try {
					long freeSpace = Files.getFileStore(
							FileSystems.getDefault().getPath(secondHopDownload.getSourceFile().getAbsolutePath()))
							.getUsableSpace();
					if (secondHopDownload.getDownloadTask().getSize() > freeSpace) {
						// Not enough space disk space to perform the first hop download.
						response.value = false;
						response.message = "Insufficient disk space. Free Space: " + freeSpace + " bytes. File size: "
								+ secondHopDownload.getDownloadTask().getSize() + " bytes";
						return response;
					}

				} catch (IOException e) {
					// Failed to check free disk space. We'll try the download.
					logger.error("Failed to determine free space", e);
				}
			} else {
				// A download from this user for this path is already in progress
				response.value = false;
				response.message = "The file is already being downloaded for user "
						+ secondHopDownload.getDownloadTask().getUserId();
				return response;
			}

			logger.info(
					"Transaction count under limit - inProcessS3DownloadsForGlobus: {}, maxPermittedS3DownloadsForGlobus: {}, path: {}",
					inProcessS3DownloadsForGlobus, maxPermittedS3DownloadsForGlobus,
					secondHopDownload.getDownloadTask().getPath());

		} else {
			// We are over the allowed number of transactions.
			response.value = false;
			response.message = "Transaction limit reached - inProcessS3DownloadsForGlobus: "
					+ inProcessS3DownloadsForGlobus + ", maxPermittedS3DownloadsForGlobus: "
					+ maxPermittedS3DownloadsForGlobus;
			return response;
		}

		// Check that the total downloads size for this user doesn't exceed the limit.
		double totalDownloadsSize = dataDownloadDAO.getTotalDownloadsSize(
				secondHopDownload.getDownloadTask().getUserId(), HpcDataTransferDownloadStatus.IN_PROGRESS);

		if (totalDownloadsSize > maxPermittedTotalDownloadsSizePerUser) {
			response.value = false;
			response.message = "The total in-progress downloads size [" + totalDownloadsSize + "GB] for this user ["
					+ secondHopDownload.getDownloadTask().getUserId() + "] exceeds the max permitted ["
					+ maxPermittedTotalDownloadsSizePerUser + "GB]";
			return response;

		} else {
			logger.info("The total in-progress downloads size [{}GB] for this user [{}] under the max permitted [{}GB]",
					totalDownloadsSize, secondHopDownload.getDownloadTask().getUserId(),
					maxPermittedTotalDownloadsSizePerUser);
		}

		return response;
	}

	/**
	 * Generate a download URL for a data object file.
	 *
	 * @param path                     The data object path.
	 * @param archiveLocation          The archive file location.
	 * @param dataTransferType         The data transfer type.
	 * @param configurationId          The configuration ID (needed to determine the
	 *                                 archive connection config).
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 *                                 Used to identify the S3 archive the
	 *                                 data-object is stored in. This is only
	 *                                 applicable for S3 archives, not POSIX.
	 * @return The download URL.
	 * @throws HpcException on service failure.
	 */
	private String generateDownloadRequestURL(String path, HpcFileLocation archiveLocation,
			HpcDataTransferType dataTransferType, String configurationId, String s3ArchiveConfigurationId)
			throws HpcException {
		// Input Validation.
		if (dataTransferType == null || !isValidFileLocation(archiveLocation)) {
			throw new HpcException("Invalid generate download URL request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the data transfer configuration.
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);

		// Generate and return the download URL.
		return dataTransferProxies.get(dataTransferType).generateDownloadRequestURL(
				getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId), archiveLocation,
				dataTransferConfiguration.getBaseArchiveDestination(),
				dataTransferConfiguration.getUploadRequestURLExpiration());
	}

	/*
	 * Determine whether a data transfer request can be initiated at current time.
	 *
	 * @param dataTransferType The type of data transfer.
	 * 
	 * @param configurationId The data management configuration ID.
	 * 
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 * Used to identify the S3 archive the data-object is stored in. This is only
	 * applicable for S3 archives, not POSIX.
	 * 
	 * @return boolean that is true if request can be initiated, false otherwise
	 * 
	 * @throw HpcException On internal error
	 */
	private boolean acceptsTransferRequests(HpcDataTransferType dataTransferType, String configurationId,
			String s3ArchiveConfigurationId) throws HpcException {

		logger.info(String.format(
				"checkIfTransferCanBeLaunched: Entered with parameters of transferType = %s, dataMgmtConfigId = %s",
				dataTransferType.toString(), configurationId));

		if (!dataTransferType.equals(HpcDataTransferType.GLOBUS)) {
			// The 'data transfer acceptance' check is applicable for Globus transfer only.
			return true;
		}

		final Object theAuthToken = getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId);

		logger.info(String.format("checkIfTransferCanBeLaunched: got auth token of %s", token2String(theAuthToken)));

		final HpcTransferAcceptanceResponse transferAcceptanceResponse = this.dataTransferProxies.get(dataTransferType)
				.acceptsTransferRequests(theAuthToken);

		String globusClientId = null;

		logger.info("checkIfTransferCanBeLaunched: searching for token within invoker state");

		for (HpcDataTransferAuthenticatedToken someToken : dataTransferAuthenticatedTokens) {
			if (someToken.getDataTransferType().equals(dataTransferType)
					&& someToken.getConfigurationId().equals(configurationId)
					&& someToken.getDataTransferAuthenticatedToken().equals(theAuthToken)) {
				globusClientId = someToken.getSystemAccountId();

				logger.info(String.format(
						"checkIfTransferCanBeLaunched: found matching token and its system account ID (Globus client ID) is %s",
						globusClientId));

				break;
			}
		}
		if (null == globusClientId) {

			logger.error("checkIfTransferCanBeLaunched: About to throw HpcException");

			final String msg = String.format(
					"Could not find Globus app account client ID for this request, transfer type is %s and data management configuration ID is %s.",
					dataTransferType.toString(), configurationId);
			throw new HpcException(msg, HpcErrorType.UNEXPECTED_ERROR);
		}

		logger.info(String.format(
				"checkIfTransferCanBeLaunched: Update to call system account locator's setGlobusAccountQueueSize passing in (%s, %s)",
				globusClientId, Integer.toString(transferAcceptanceResponse.getQueueSize())));

		logger.info("checkIfTransferCanBeLaunched: About to return");

		return transferAcceptanceResponse.canAcceptTransfer();
	}

	/*
	 * Determine whether a data transfer request can be initiated at current time
	 * given a token.
	 *
	 * @param dataTransferType The type of data transfer.
	 * 
	 * @param configurationId The data management configuration ID.
	 * 
	 * @param theAuthToken The authToken to check
	 * 
	 * @return boolean that is true if request can be initiated, false otherwise
	 * 
	 * @throw HpcException On internal error
	 */
	private boolean acceptsTransferRequests(HpcDataTransferType dataTransferType, String configurationId,
			final Object theAuthToken) throws HpcException {

		logger.info(String.format(
				"checkIfTransferCanBeLaunched: Entered with parameters of transferType = %s, dataMgmtConfigId = %s",
				dataTransferType.toString(), configurationId));

		if (!dataTransferType.equals(HpcDataTransferType.GLOBUS)) {
			// The 'data transfer acceptance' check is applicable for Globus transfer only.
			return true;
		}

		logger.info(String.format("checkIfTransferCanBeLaunched: got auth token of %s", token2String(theAuthToken)));

		final HpcTransferAcceptanceResponse transferAcceptanceResponse = this.dataTransferProxies.get(dataTransferType)
				.acceptsTransferRequests(theAuthToken);

		String globusClientId = null;

		logger.info("checkIfTransferCanBeLaunched: searching for token within invoker state");

		for (HpcDataTransferAuthenticatedToken someToken : dataTransferAuthenticatedTokens) {
			if (someToken.getDataTransferType().equals(dataTransferType)
					&& someToken.getConfigurationId().equals(configurationId)
					&& someToken.getDataTransferAuthenticatedToken().equals(theAuthToken)) {
				globusClientId = someToken.getSystemAccountId();

				logger.info(String.format(
						"checkIfTransferCanBeLaunched: found matching token and its system account ID (Globus client ID) is %s",
						globusClientId));

				break;
			}
		}
		if (null == globusClientId) {

			logger.error("checkIfTransferCanBeLaunched: About to throw HpcException");

			final String msg = String.format(
					"Could not find Globus app account client ID for this request, transfer type is %s and data management configuration ID is %s.",
					dataTransferType.toString(), configurationId);
			throw new HpcException(msg, HpcErrorType.UNEXPECTED_ERROR);
		}

		logger.info(String.format(
				"checkIfTransferCanBeLaunched: Update to call system account locator's setGlobusAccountQueueSize passing in (%s, %s)",
				globusClientId, Integer.toString(transferAcceptanceResponse.getQueueSize())));

		logger.info("checkIfTransferCanBeLaunched: About to return");

		return transferAcceptanceResponse.canAcceptTransfer();
	}

	/*
	 * Scan a directory on a local DME server NAS
	 *
	 * @param directoryLocation The directory to scan.
	 * 
	 * @return a list of scanned directory items.
	 * 
	 * @throw HpcException On internal error
	 */
	private List<HpcDirectoryScanItem> scanDirectory(HpcFileLocation directoryLocation) throws HpcException {
		List<HpcDirectoryScanItem> directoryScanItems = new ArrayList<>();

		try (Stream<Path> pathsStream = Files.walk(Paths.get(directoryLocation.getFileId()))
				.filter(Files::isRegularFile)) {
			Iterator<Path> pathsIter = pathsStream.iterator();
			while (pathsIter.hasNext()) {
				Path path = pathsIter.next();
				HpcDirectoryScanItem directoryScanItem = new HpcDirectoryScanItem();
				directoryScanItem.setFilePath(path.toString());
				directoryScanItem.setFileName(path.getFileName().toString());
				directoryScanItem
						.setLastModified(dateFormat.format(new Date(Files.getLastModifiedTime(path).toMillis())));
				directoryScanItems.add(directoryScanItem);
			}

		} catch (IOException e) {
			throw new HpcException(
					"Failed to scan a directory in the file system: [" + e.getMessage() + "] " + directoryLocation,
					HpcErrorType.UNEXPECTED_ERROR, e);
		}

		return directoryScanItems;
	}

	private String token2String(Object pToken) {
		String retStrRep = null;
		if (pToken instanceof HpcDataTransferAuthenticatedToken) {
			HpcDataTransferAuthenticatedToken dtaToken = (HpcDataTransferAuthenticatedToken) pToken;
			StringBuilder sb = new StringBuilder();
			sb.append("[ dataTransferType = ").append(dtaToken.getDataTransferType().toString());
			sb.append(", dataTransferAuthenticatedToken = ")
					.append(dtaToken.getDataTransferAuthenticatedToken().toString());
			sb.append(", configurationId = ").append(dtaToken.getConfigurationId());
			sb.append(", systemAccountId = ").append(dtaToken.getSystemAccountId()).append(" ]");
			retStrRep = sb.toString();
		} else {
			retStrRep = pToken.toString();
		}
		return retStrRep;
	}

	/**
	 * Request a object restore to restore the file to the archive.
	 *
	 * @param downloadRequest   The data object download request.
	 * @param response          The download response object. This method sets
	 *                          download task id and destination location on the
	 *                          response.
	 * @param restorationStatus The restoration status.
	 * @throws HpcException on service failure.
	 */
	private void performObjectRestore(HpcDataObjectDownloadRequest downloadRequest,
			HpcDataObjectDownloadResponse response, String restorationStatus) throws HpcException {

		try {

			// Submit Restore request if there is no ongoing restore request
			if (restorationStatus != null && restorationStatus.equals("not in progress")) {
				dataTransferProxies.get(HpcDataTransferType.S_3)
						.restoreDataObject(
								getAuthenticatedToken(HpcDataTransferType.S_3, downloadRequest.getConfigurationId(),
										downloadRequest.getS3ArchiveConfigurationId()),
								downloadRequest.getArchiveLocation());
			}

			// Create a download task.
			HpcDataObjectDownloadTask downloadTask = new HpcDataObjectDownloadTask();
			downloadTask.setArchiveLocation(downloadRequest.getArchiveLocation());
			downloadTask.setCompletionEvent(downloadRequest.getCompletionEvent());
			downloadTask.setCollectionDownloadTaskId(downloadRequest.getCollectionDownloadTaskId());
			downloadTask.setConfigurationId(downloadRequest.getConfigurationId());
			downloadTask.setS3ArchiveConfigurationId(downloadRequest.getS3ArchiveConfigurationId());
			downloadTask.setCreated(Calendar.getInstance());
			downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.RESTORE_REQUESTED);
			downloadTask.setInProcess(false);
			downloadTask.setPercentComplete(0);
			downloadTask.setSize(downloadRequest.getSize());
			downloadTask.setPath(downloadRequest.getPath());
			downloadTask.setRetryTaskId(downloadRequest.getRetryTaskId());
			downloadTask.setUserId(downloadRequest.getUserId());
			downloadTask.setRetryUserId(downloadRequest.getRetryUserId());
			downloadTask.setDataTransferType(downloadRequest.getDataTransferType());
			downloadTask.setGlobusDownloadDestination(downloadRequest.getGlobusDestination());
			downloadTask.setS3DownloadDestination(downloadRequest.getS3Destination());
			downloadTask.setGoogleDriveDownloadDestination(downloadRequest.getGoogleDriveDestination());
			downloadTask.setAsperaDownloadDestination(downloadRequest.getAsperaDestination());
			downloadTask.setFirstHopRetried(false);
			downloadTask.setRestoreRequested(true);
			downloadTask.setDoc(dataManagementService
					.getDataManagementConfiguration(downloadRequest.getConfigurationId()).getDoc());

			if (downloadTask.getS3DownloadDestination() != null) {
				downloadTask.setDataTransferType(HpcDataTransferType.S_3);
				downloadTask.setDestinationType(HpcDataTransferType.S_3);
				dataDownloadDAO.createDataObjectDownloadTask(downloadTask);
				response.setDestinationLocation(downloadTask.getS3DownloadDestination().getDestinationLocation());
			} else if (downloadTask.getGoogleDriveDownloadDestination() != null) {
				downloadTask.setDataTransferType(HpcDataTransferType.GOOGLE_DRIVE);
				downloadTask.setDestinationType(HpcDataTransferType.GOOGLE_DRIVE);
				dataDownloadDAO.createDataObjectDownloadTask(downloadTask);
				response.setDestinationLocation(
						downloadTask.getGoogleDriveDownloadDestination().getDestinationLocation());
			} else if (downloadTask.getGoogleCloudStorageDownloadDestination() != null) {
				downloadTask.setDataTransferType(HpcDataTransferType.GOOGLE_CLOUD_STORAGE);
				downloadTask.setDestinationType(HpcDataTransferType.GOOGLE_CLOUD_STORAGE);
				dataDownloadDAO.createDataObjectDownloadTask(downloadTask);
				response.setDestinationLocation(
						downloadTask.getGoogleCloudStorageDownloadDestination().getDestinationLocation());
			} else if (downloadTask.getBoxDownloadDestination() != null) {
				downloadTask.setDataTransferType(HpcDataTransferType.BOX);
				downloadTask.setDestinationType(HpcDataTransferType.BOX);
				dataDownloadDAO.createDataObjectDownloadTask(downloadTask);
				response.setDestinationLocation(downloadTask.getBoxDownloadDestination().getDestinationLocation());
			} else if (downloadTask.getAsperaDownloadDestination() != null) {
				downloadTask.setDestinationType(HpcDataTransferType.ASPERA);
				HpcSecondHopDownload secondHopDownload = new HpcSecondHopDownload(downloadRequest,
						HpcDataTransferDownloadStatus.RESTORE_REQUESTED);
				downloadTask.setId(secondHopDownload.getDownloadTask().getId());
				response.setDestinationLocation(downloadTask.getAsperaDownloadDestination().getDestinationLocation());
			} else if (downloadRequest.getGlobusDestination() != null) {
				downloadTask.setDestinationType(HpcDataTransferType.GLOBUS);
				HpcSecondHopDownload secondHopDownload = new HpcSecondHopDownload(downloadRequest,
						HpcDataTransferDownloadStatus.RESTORE_REQUESTED);
				downloadTask.setId(secondHopDownload.getDownloadTask().getId());
				response.setDestinationLocation(downloadTask.getGlobusDownloadDestination().getDestinationLocation());
			} else {
				downloadTask.setDataTransferType(HpcDataTransferType.S_3);
				downloadTask.setDestinationType(HpcDataTransferType.S_3);
				HpcFileLocation destinationLocation = new HpcFileLocation();
				destinationLocation.setFileContainerId("Synchronous Download");
				destinationLocation.setFileId("Synchronous Download");
				HpcGlobusDownloadDestination dummyGlobusDownloadDestination = new HpcGlobusDownloadDestination();
				dummyGlobusDownloadDestination.setDestinationLocation(destinationLocation);
				downloadTask.setGlobusDownloadDestination(dummyGlobusDownloadDestination);
				dataDownloadDAO.createDataObjectDownloadTask(downloadTask);
				response.setDestinationLocation(destinationLocation);
			}

			// Populate the response object.
			response.setDownloadTaskId(downloadTask.getId());
			response.setRestoreInProgress(true);

		} catch (HpcException e) {
			throw (e);
		}
	}

	/**
	 * Get download source location for a second hop. The second hop source is on
	 * DME Globus endpoint.
	 *
	 * @param configurationId          The data management configuration ID.
	 * @param s3ArchiveConfigurationId (Optional) The S3 Archive configuration ID.
	 *                                 Used to identify the S3 archive the
	 *                                 data-object is stored in. This is only
	 *                                 applicable for S3 archives, not POSIX.
	 * @param dataTransferType         The data transfer type.
	 * @return The download source location.
	 * @throws HpcException on data transfer system failure.
	 */
	private HpcFileLocation getSecondHopDownloadSourceLocation(String configurationId, String s3ArchiveConfigurationId,
			HpcDataTransferType dataTransferType) throws HpcException {
		// Get the data transfer configuration.
		HpcArchive baseDownloadSource = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType)
				.getBaseDownloadSource();

		// Create a source location. (This is a local GLOBUS endpoint).
		HpcFileLocation sourceLocation = new HpcFileLocation();
		sourceLocation.setFileContainerId(baseDownloadSource.getFileLocation().getFileContainerId());
		sourceLocation.setFileId(baseDownloadSource.getFileLocation().getFileId() + "/" + UUID.randomUUID().toString());

		return sourceLocation;
	}

	/**
	 * Determine if a download task can be retried. result is 'failed' or
	 * 'canceled'.
	 *
	 * @param result the download result.
	 * @return true if the download task can be retried, or false otherwise.
	 */
	private boolean downloadTaskCanBeRetried(HpcDownloadResult result) {
		if (result == null) {
			return false;
		}
		switch (result) {
		case FAILED:
		case FAILED_PERMISSION_DENIED:
		case FAILED_CREDENTIALS_NEEDED:
		case CANCELED:
			return true;

		default:
			return false;
		}
	}

	/**
	 * Get archive location of a data object.
	 *
	 * @param path the data object path.
	 * @return The archive location of a data object.
	 * @throws HpcException on metadata service error.
	 */
	HpcFileLocation getArchiveLocation(String path) throws HpcException {

		HpcSystemGeneratedMetadata metadata = metadataService.getDataObjectSystemGeneratedMetadata(path);
		if (StringUtils.isEmpty(metadata.getLinkSourcePath())) {
			return metadata.getArchiveLocation();
		}

		// It's a link - follow it to get the archive location.
		return getArchiveLocation(metadata.getLinkSourcePath());
	}

	// ---------------------------------------------------------------------//
	// Setter Methods to support JUnit Testing (for injecting Mocks)
	// ---------------------------------------------------------------------//

	void setDataManagementConfigurationLocator(
			HpcDataManagementConfigurationLocator dataManagementConfigurationLocator) {
		this.dataManagementConfigurationLocator = dataManagementConfigurationLocator;
	}

	void setSystemAccountLocator(HpcSystemAccountLocator systemAccountLocator) {
		this.systemAccountLocator = systemAccountLocator;
	}

	void setDataDownloadDAO(HpcDataDownloadDAO dataDownloadDAO) {
		this.dataDownloadDAO = dataDownloadDAO;
	}

	void setDataManagementService(HpcDataManagementService dataManagementService) {
		this.dataManagementService = dataManagementService;
	}

	// Second hop download.
	private class HpcSecondHopDownload implements HpcDataTransferProgressListener {
		// ---------------------------------------------------------------------//
		// Instance members
		// ---------------------------------------------------------------------//

		// A download data object task (keeps track of the async 2-hop download
		// end-to-end.
		private HpcDataObjectDownloadTask downloadTask = new HpcDataObjectDownloadTask();

		// The second hop download's source file.
		private File sourceFile = null;

		// Indicator whether task has removed / cancelled
		private boolean taskCancelled = false;

		// The CompletableFuture instance that is executing the transfer. Used to cancel
		// if needed.
		private CompletableFuture<?> completableFuture = null;

		// ---------------------------------------------------------------------//
		// Constructors
		// ---------------------------------------------------------------------//

		/**
		 * Constructs a 2nd Hop download object (to keep track of async processing)
		 *
		 * @param firstHopDownloadRequest    The first hop download request.
		 * @param dataTransferDownloadStatus The download status.
		 * @throws HpcException If it failed to create a download task.
		 */
		public HpcSecondHopDownload(HpcDataObjectDownloadRequest firstHopDownloadRequest,
				HpcDataTransferDownloadStatus dataTransferDownloadStatus) throws HpcException {
			// Create the second-hop archive location and destination
			HpcFileLocation secondHopArchiveLocation = getSecondHopDownloadSourceLocation(
					firstHopDownloadRequest.getConfigurationId(), firstHopDownloadRequest.getS3ArchiveConfigurationId(),
					HpcDataTransferType.GLOBUS);
			HpcGlobusDownloadDestination secondHopGlobusDestination = null;
			HpcDataTransferType destinationType = null;
			if (firstHopDownloadRequest.getGlobusDestination() != null) {
				// For Globus destination, Calculate the file location.
				secondHopGlobusDestination = new HpcGlobusDownloadDestination();
				secondHopGlobusDestination.setDestinationLocation(calculateGlobusDownloadDestinationFileLocation(
						firstHopDownloadRequest.getGlobusDestination().getDestinationLocation(),
						firstHopDownloadRequest.getGlobusDestination().getDestinationOverwrite(),
						HpcDataTransferType.GLOBUS, firstHopDownloadRequest.getPath(),
						firstHopDownloadRequest.getConfigurationId()));
				secondHopGlobusDestination.setDestinationOverwrite(
						firstHopDownloadRequest.getGlobusDestination().getDestinationOverwrite());
				destinationType = HpcDataTransferType.GLOBUS;
			} else {
				destinationType = HpcDataTransferType.ASPERA;
			}

			// Get the data transfer configuration.
			HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
					.getDataTransferConfiguration(firstHopDownloadRequest.getConfigurationId(),
							firstHopDownloadRequest.getS3ArchiveConfigurationId(), HpcDataTransferType.GLOBUS);

			// Create the source file for the second hop download.
			sourceFile = createFile(getFilePath(secondHopArchiveLocation.getFileId(),
					dataTransferConfiguration.getBaseDownloadSource()));

			// Create and persist a download task. This object tracks the download request
			// through the 2-hop async download requests.
			createDownloadTask(firstHopDownloadRequest, secondHopArchiveLocation, secondHopGlobusDestination,
					dataTransferDownloadStatus, destinationType);

			logger.info(
					"download task: [taskId={}] - 2 Hop download to {} destination created. Path at scratch space: {}",
					downloadTask.getId(), downloadTask.getDestinationType(), sourceFile.getAbsolutePath());
		}

		/**
		 * Constructs a 2nd Hop download object (to keep track of async processing).
		 * This constructor is used when a 2-hop download needs to be restarted.
		 *
		 * @param downloadTask An exiting download taskThe first hop download request.
		 * @throws HpcException If it failed to create a download task.
		 */
		public HpcSecondHopDownload(HpcDataObjectDownloadTask downloadTask) throws HpcException {
			// Create the second-hop archive location and destination
			HpcFileLocation secondHopArchiveLocation = getSecondHopDownloadSourceLocation(
					downloadTask.getConfigurationId(), downloadTask.getS3ArchiveConfigurationId(),
					HpcDataTransferType.GLOBUS);

			// Get the data transfer configuration.
			HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
					.getDataTransferConfiguration(downloadTask.getConfigurationId(),
							downloadTask.getS3ArchiveConfigurationId(), HpcDataTransferType.GLOBUS);

			// Create the source file for the second hop download.
			sourceFile = createFile(getFilePath(secondHopArchiveLocation.getFileId(),
					dataTransferConfiguration.getBaseDownloadSource()));

			// Update and persist a download task. This object tracks the download request
			// through the 2-hop async download requests.
			updateDownloadTask(downloadTask, secondHopArchiveLocation);

			logger.info("download task: [taskId={}] - 2 Hop download created. Path at scratch space: {}",
					downloadTask.getId(), sourceFile.getAbsolutePath());
		}

		// ---------------------------------------------------------------------//
		// Methods
		// ---------------------------------------------------------------------//

		/**
		 * Get the second hop download source file.
		 *
		 * @return The second hop source file.
		 */
		public File getSourceFile() {
			return sourceFile;
		}

		/**
		 * Return the download task associated with this 2nd hop download.
		 *
		 * @return The 2nd hop download task.
		 */
		public HpcDataObjectDownloadTask getDownloadTask() {
			return downloadTask;
		}

		// ---------------------------------------------------------------------//
		// HpcDataTransferProgressListener Interface Implementation
		// ---------------------------------------------------------------------//

		@Override
		public void transferCompleted(Long bytesTransferred) {
			// This callback method is called when the first hop (S3) download completed.

			try {
				// Check if the task was cancelled while the first-hop download was performed.
				if (Optional.ofNullable(dataDownloadDAO.getDataObjectDownloadTaskStatus(downloadTask.getId()))
						.orElse(HpcDataTransferDownloadStatus.CANCELED)
						.equals(HpcDataTransferDownloadStatus.CANCELED)) {
					// The task was cancelled. Do some cleanup.
					FileUtils.deleteQuietly(new File(downloadTask.getDownloadFilePath()));

					logger.info(
							"download task: [taskId={}] - 1st Hop completed but task was cancelled [transfer-type={}, destination-type={}]",
							downloadTask.getId(), downloadTask.getDataTransferType(),
							downloadTask.getDestinationType());

				} else {
					// Update the download task to reflect 1st hop transfer completed. The transfer
					// type is updated to Globus / Aspera to perform the second hop
					// For single file download, the status is set to received, and for bulk
					// download, status is set to bunching-received, so we wait for all the files
					// in the bulk request to finish first hop, and then submit a single Globus
					// request for the second hop.
					if (downloadTask.getDestinationType().equals(HpcDataTransferType.GLOBUS)) {
						// 2nd hop to Globus destination.
						downloadTask.setDataTransferType(HpcDataTransferType.GLOBUS);
						downloadTask
								.setDataTransferStatus(StringUtils.isEmpty(downloadTask.getCollectionDownloadTaskId())
										|| !globusCollectionDownloadBunching ? HpcDataTransferDownloadStatus.RECEIVED
												: HpcDataTransferDownloadStatus.GLOBUS_BUNCHING);
					} else {
						// 2nd hop to Aspera destination.
						downloadTask.setDataTransferType(HpcDataTransferType.ASPERA);
						downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.RECEIVED);
					}

					downloadTask.setInProcess(false);
					downloadTask.setProcessed(Calendar.getInstance());
					downloadTask.setPercentComplete(0);
					downloadTask.setStagingPercentComplete(100);

					// Persist the download task.
					dataDownloadDAO.updateDataObjectDownloadTask(downloadTask);
				}

				logger.info("download task: [taskId={}] - 1st hop completed. Path at scratch space: {}",
						downloadTask.getId(), sourceFile.getAbsolutePath());

			} catch (HpcException e) {
				logger.error("Failed to update download task", e);
				downloadFailed(e.getMessage());
			}
		}

		// This callback method is called when the first hop download failed.
		@Override
		public void transferFailed(String message) {
			if (taskCancelled) {
				// The task was cancelled / removed from the DB. Do some cleanup.
				FileUtils.deleteQuietly(new File(downloadTask.getDownloadFilePath()));

				logger.info(
						"download task: [taskId={}] - cancelled/removed - {} [transfer-type={}, destination-type={}]",
						downloadTask.getId(), downloadTask.getPercentComplete(), downloadTask.getDataTransferType(),
						downloadTask.getDestinationType());
				return;
			}

			if (!downloadTask.getFirstHopRetried()) {
				// First hop failed, but was not retried yet. Give it a second chance.
				logger.info(
						"download task: [taskId={}] - 1 Hop download failed and will be retried. Path at scratch space: {}",
						downloadTask.getId(), sourceFile.getAbsolutePath());
				downloadTask.setFirstHopRetried(true);
				try {
					resetDataObjectDownloadTask(downloadTask);
					return;
				} catch (HpcException e) {
					downloadTask.setFirstHopRetried(false);
					logger.error("download task: [taskId={}] - failed to reset", downloadTask.getId(), e);

				}
			}

			logger.info("download task: [taskId={}] - 1 Hop download retry failed. Path at scratch space: {}",
					downloadTask.getId(), sourceFile.getAbsolutePath());

			String errorMessage = "Failed to get data from archive via S3: " + message;
			downloadFailed(errorMessage);

			try {
				HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
						.getDataTransferConfiguration(downloadTask.getConfigurationId(),
								downloadTask.getS3ArchiveConfigurationId(), HpcDataTransferType.S_3);
				HpcFileLocation archiveLocation = dataTransferConfiguration.getBaseArchiveDestination()
						.getFileLocation();
				notificationService.sendNotification(new HpcException(
						message + ", task_id: " + downloadTask.getId() + ", user_id: " + downloadTask.getUserId()
								+ ", archive_file_container_id (bucket): " + archiveLocation.getFileContainerId()
								+ ", archive_file_id (key): " + archiveLocation.getFileId() + downloadTask.getPath(),
						HpcErrorType.DATA_TRANSFER_ERROR, dataTransferConfiguration.getArchiveProvider()), true);

			} catch (HpcException e) {
				logger.error("Failed to send storage admin notification w/ data", e);
				// theoretically, we should never get here
				// Need to specify some value for IntegratedSystem, else notification will not
				// be sent
				notificationService.sendNotification(new HpcException(
						message + ", task_id: " + downloadTask.getId() + ", user_id: " + downloadTask.getUserId()
								+ ", path: " + downloadTask.getPath(),
						HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.CLOUDIAN));

			}
		}

		// This callback method is called when the first hop download progressed.
		@Override
		public void transferProgressed(long bytesTransferred) {
			try {
				if (!updateDataObjectDownloadTask(downloadTask, bytesTransferred)) {
					// The task was cancelled / removed from the DB. Stop 1st hop download thread.
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
				logger.error("Failed to update 1st hop download task progress", e);
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
		 * Create a download task for a 2-hop download.
		 * 
		 * @param firstHopDownloadRequest    The first hop download request.
		 * @param secondHopArchiveLocation   The second hop archive location (on the
		 *                                   server's Globus endpoint).
		 * @param secondHopGlobusDestination The second hop download destination (user's
		 *                                   Globus endpoint).
		 * @param dataTransferDownloadStatus The download status.
		 * @param destinationType            The 2-hop destination type (GLOBUS or
		 *                                   ASPERA).
		 * @throws HpcException If it failed to persist the task.
		 */
		private void createDownloadTask(HpcDataObjectDownloadRequest firstHopDownloadRequest,
				HpcFileLocation secondHopArchiveLocation, HpcGlobusDownloadDestination secondHopGlobusDestination,
				HpcDataTransferDownloadStatus dataTransferDownloadStatus, HpcDataTransferType destinationType)
				throws HpcException {

			downloadTask.setDataTransferType(HpcDataTransferType.S_3);
			downloadTask.setDataTransferStatus(dataTransferDownloadStatus);
			downloadTask.setDownloadFilePath(sourceFile.getAbsolutePath());
			downloadTask.setUserId(firstHopDownloadRequest.getUserId());
			downloadTask.setPath(firstHopDownloadRequest.getPath());
			downloadTask.setConfigurationId(firstHopDownloadRequest.getConfigurationId());
			downloadTask.setS3ArchiveConfigurationId(firstHopDownloadRequest.getS3ArchiveConfigurationId());
			downloadTask.setCompletionEvent(firstHopDownloadRequest.getCompletionEvent());
			downloadTask.setCollectionDownloadTaskId(firstHopDownloadRequest.getCollectionDownloadTaskId());
			downloadTask.setArchiveLocation(secondHopArchiveLocation);
			downloadTask.setGlobusDownloadDestination(secondHopGlobusDestination);
			downloadTask.setAsperaDownloadDestination(firstHopDownloadRequest.getAsperaDestination());
			downloadTask.setDestinationType(destinationType);
			downloadTask.setRetryTaskId(firstHopDownloadRequest.getRetryTaskId());
			downloadTask.setRetryUserId(firstHopDownloadRequest.getRetryUserId());
			downloadTask.setCreated(Calendar.getInstance());
			downloadTask.setPercentComplete(0);
			downloadTask.setSize(firstHopDownloadRequest.getSize());
			downloadTask.setFirstHopRetried(false);
			downloadTask.setS3DownloadTaskServerId(
					dataTransferDownloadStatus.equals(HpcDataTransferDownloadStatus.IN_PROGRESS)
							? s3DownloadTaskServerId
							: null);
			downloadTask.setDoc(dataManagementService
					.getDataManagementConfiguration(firstHopDownloadRequest.getConfigurationId()).getDoc());

			if (HpcDataTransferDownloadStatus.RESTORE_REQUESTED.equals(dataTransferDownloadStatus)) {
				downloadTask.setRestoreRequested(true);
			}
			dataDownloadDAO.createDataObjectDownloadTask(downloadTask);
		}

		/**
		 * Update a download task for a 2-hop download from an existing download task.
		 *
		 * @param downloadTask             The existing download task
		 * @param secondHopArchiveLocation The second hop archive location
		 * @throws HpcException If it failed to persist the task.
		 */
		private void updateDownloadTask(HpcDataObjectDownloadTask downloadTask,
				HpcFileLocation secondHopArchiveLocation) throws HpcException {
			this.downloadTask.setId(downloadTask.getId());
			this.downloadTask.setUserId(downloadTask.getUserId());
			this.downloadTask.setPath(downloadTask.getPath());
			this.downloadTask.setConfigurationId(downloadTask.getConfigurationId());
			this.downloadTask.setS3ArchiveConfigurationId(downloadTask.getS3ArchiveConfigurationId());
			this.downloadTask.setDataTransferRequestId(null);
			this.downloadTask.setDataTransferType(HpcDataTransferType.S_3);
			this.downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.IN_PROGRESS);
			this.downloadTask.setDownloadFilePath(sourceFile.getAbsolutePath());
			this.downloadTask.setArchiveLocation(secondHopArchiveLocation);
			this.downloadTask.setGlobusDownloadDestination(downloadTask.getGlobusDownloadDestination());
			this.downloadTask.setAsperaDownloadDestination(downloadTask.getAsperaDownloadDestination());
			this.downloadTask.setDestinationType(downloadTask.getDestinationType());
			this.downloadTask.setCompletionEvent(downloadTask.getCompletionEvent());
			this.downloadTask.setCollectionDownloadTaskId(downloadTask.getCollectionDownloadTaskId());
			this.downloadTask.setCreated(downloadTask.getCreated());
			this.downloadTask.setPercentComplete(0);
			this.downloadTask.setSize(downloadTask.getSize());
			this.downloadTask.setS3DownloadTaskServerId(downloadTask.getS3DownloadTaskServerId());
			this.downloadTask.setFirstHopRetried(downloadTask.getFirstHopRetried());
			this.downloadTask.setRetryTaskId(downloadTask.getRetryTaskId());
			this.downloadTask.setRetryUserId(downloadTask.getRetryUserId());

			dataDownloadDAO.updateDataObjectDownloadTask(this.downloadTask);
		}

		/**
		 * Handle the case when transfer failed. Send a download failed event and
		 * cleanup the download task.
		 *
		 * @param message The message to include in the download failed event.
		 */
		private void downloadFailed(String message) {
			Calendar transferFailedTimestamp = Calendar.getInstance();
			try {
				// Record a download failed event if requested to.
				if (downloadTask.getCompletionEvent()) {
					eventService.addDataTransferDownloadFailedEvent(downloadTask.getUserId(), downloadTask.getPath(),
							HpcDownloadTaskType.DATA_OBJECT, HpcDownloadResult.FAILED, downloadTask.getId(),
							downloadTask.getGlobusDownloadDestination().getDestinationLocation(),
							transferFailedTimestamp, message, downloadTask.getDestinationType());
				}

			} catch (HpcException e) {
				logger.error("Failed to add data transfer download failed event", e);
			}

			// Cleanup the download task.
			try {
				completeDataObjectDownloadTask(downloadTask, HpcDownloadResult.FAILED, message, transferFailedTimestamp,
						0);

			} catch (HpcException ex) {
				logger.error("Failed to cleanup download task", ex);
			}
		}

	}

}
