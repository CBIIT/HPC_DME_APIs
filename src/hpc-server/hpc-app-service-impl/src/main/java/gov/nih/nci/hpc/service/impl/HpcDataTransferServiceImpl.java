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

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileLocation;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidTierItems;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidS3Account;
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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import gov.nih.nci.hpc.dao.HpcDataDownloadDAO;
import gov.nih.nci.hpc.dao.HpcLifecycleDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTaskStatusFilter;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
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
import gov.nih.nci.hpc.domain.datatransfer.HpcGoogleDriveDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcLifecycleRequestType;
import gov.nih.nci.hpc.domain.datatransfer.HpcPatternType;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3ObjectMetadata;
import gov.nih.nci.hpc.domain.datatransfer.HpcStreamingUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcSynchronousDownloadFilter;
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadPartETag;
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcUserDownloadRequest;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcBulkTierItem;
import gov.nih.nci.hpc.domain.model.HpcBulkTierRequest;
import gov.nih.nci.hpc.domain.model.HpcDataTransferAuthenticatedToken;
import gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.integration.HpcTransferAcceptanceResponse;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcEventService;
import gov.nih.nci.hpc.service.HpcMetadataService;

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

	//Credentials are needed download error message.
	private static final String CREDENTIALS_NEEDED_ERROR_MESSAGE = "Credentials are needed";
	
	// Google Drive 'My Drive' ID.
	private static final String MY_GOOGLE_DRIVE_ID = "MyDrive";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// Map data transfer type to its proxy impl.
	private Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies = new EnumMap<>(
			HpcDataTransferType.class);

	// System Accounts locator.
	@Autowired
	private HpcSystemAccountLocator systemAccountLocator = null;

	// Data object download DAO.
	@Autowired
	private HpcDataDownloadDAO dataDownloadDAO = null;
	
	// Audit DAO.
	@Autowired
	private HpcLifecycleDAO lifecycleDAO = null;

	// Event service.
	@Autowired
	private HpcEventService eventService = null;

	// Metadata service.
	@Autowired
	private HpcMetadataService metadataService = null;

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

	// The max number of days to keep deep archive in progress
	@Value("${hpc.service.dataTransfer.maxDeepArchiveInProgressDays}")
	private Integer maxDeepArchiveInProgressDays = null;

	// cancelCollectionDownloadTaskItems() query filter
	private List<HpcDataObjectDownloadTaskStatusFilter> cancelCollectionDownloadTaskItemsFilter = new ArrayList<>();

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
		// is IN_PROGRESS
		// to GLOBUS destination.
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
		filter.setDestination(HpcDataTransferType.S_3);
		cancelCollectionDownloadTaskItemsFilter.add(filter);

		filter = new HpcDataObjectDownloadTaskStatusFilter();
		filter.setStatus(HpcDataTransferDownloadStatus.IN_PROGRESS);
		filter.setDestination(HpcDataTransferType.GLOBUS);
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
			HpcUploadSource fileSystemUploadSource, File sourceFile, boolean generateUploadRequestURL,
			Integer uploadParts, String uploadRequestURLChecksum, String path, String dataObjectId, String userId,
			String callerObjectId, String configurationId) throws HpcException {
		// Input Validation. One and only one of the first 5 parameters is expected to
		// be provided.

		// Validate data-object-id provided.
		if (StringUtils.isEmpty(dataObjectId)) {
			throw new HpcException("No data object ID in upload request", HpcErrorType.UNEXPECTED_ERROR);
		}

		// Validate an upload source was provided.
		if (globusUploadSource == null && s3UploadSource == null && googleDriveUploadSource == null
				&& fileSystemUploadSource == null && sourceFile == null && !generateUploadRequestURL) {
			throw new HpcException("No data transfer source or data attachment provided or upload URL requested",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate Globus upload source.
		if (globusUploadSource != null) {
			if (s3UploadSource != null || googleDriveUploadSource != null || fileSystemUploadSource != null
					|| sourceFile != null || generateUploadRequestURL) {
				throw new HpcException(MULTIPLE_UPLOAD_SOURCE_ERROR_MESSAGE, HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (!isValidFileLocation(globusUploadSource.getSourceLocation())) {
				throw new HpcException("Invalid Globus upload source", HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate S3 upload source.
		if (s3UploadSource != null) {
			if (googleDriveUploadSource != null || fileSystemUploadSource != null || sourceFile != null
					|| generateUploadRequestURL) {
				throw new HpcException(MULTIPLE_UPLOAD_SOURCE_ERROR_MESSAGE, HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (!isValidFileLocation(s3UploadSource.getSourceLocation())) {
				throw new HpcException("Invalid S3 upload source location", HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (StringUtils.isEmpty(s3UploadSource.getSourceURL()) && !isValidS3Account(s3UploadSource.getAccount())) {
				throw new HpcException("Invalid S3 upload account", HpcErrorType.INVALID_REQUEST_INPUT);
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
			if (fileSystemUploadSource != null || sourceFile != null || generateUploadRequestURL) {
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
				throw new HpcException("Invalid Google Drive access token", HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (googleDriveUploadSource.getAccount() != null) {
				throw new HpcException("AWS S3 account provided in Google Drive upload source location",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
		}

		// Validate Google Drive upload source.
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
			if (globusUploadSource != null || s3UploadSource != null || sourceFile != null) {
				throw new HpcException(MULTIPLE_UPLOAD_SOURCE_ERROR_MESSAGE, HpcErrorType.INVALID_REQUEST_INPUT);
			}
			if (uploadParts != null && uploadParts < 1) {
				throw new HpcException("Invalid upload parts: " + uploadParts, HpcErrorType.INVALID_REQUEST_INPUT);
			}
		} else if (uploadParts != null) {
			throw new HpcException("Upload parts provided w/o request to generate upload URL",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate source location exists and accessible.
		Long sourceSize = validateUploadSourceFileLocation(globusUploadSource, s3UploadSource, googleDriveUploadSource,
				fileSystemUploadSource, sourceFile, configurationId);

		// Create an upload request.
		HpcDataObjectUploadRequest uploadRequest = new HpcDataObjectUploadRequest();
		uploadRequest.setPath(path);
		uploadRequest.setDataObjectId(dataObjectId);
		uploadRequest.setUserId(userId);
		uploadRequest.setCallerObjectId(callerObjectId);
		uploadRequest.setGlobusUploadSource(globusUploadSource);
		uploadRequest.setS3UploadSource(s3UploadSource);
		uploadRequest.setGoogleDriveUploadSource(googleDriveUploadSource);
		uploadRequest.setFileSystemUploadSource(fileSystemUploadSource);
		uploadRequest.setSourceFile(sourceFile);
		uploadRequest.setUploadRequestURLChecksum(uploadRequestURLChecksum);
		uploadRequest.setGenerateUploadRequestURL(generateUploadRequestURL);
		uploadRequest.setUploadParts(uploadParts);
		uploadRequest.setSourceSize(sourceSize);

		// Upload the data object file.
		return uploadDataObject(uploadRequest, configurationId);
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
	public HpcDataObjectDownloadResponse downloadDataObject(String path, HpcFileLocation archiveLocation,
			HpcGlobusDownloadDestination globusDownloadDestination, HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDriveDownloadDestination googleDriveDownloadDestination,
			HpcSynchronousDownloadFilter synchronousDownloadFilter, HpcDataTransferType dataTransferType,
			String configurationId, String s3ArchiveConfigurationId, String userId, boolean completionEvent, long size, 
			HpcDataTransferUploadStatus dataTransferStatus, HpcDeepArchiveStatus deepArchiveStatus)
			throws HpcException {
		// Input Validation.
		if (dataTransferType == null || !isValidFileLocation(archiveLocation)) {
			throw new HpcException("Invalid data transfer request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the destination.
		validateDownloadDestination(globusDownloadDestination, s3DownloadDestination, googleDriveDownloadDestination,
				synchronousDownloadFilter, configurationId, false);

		// Create a download request.
		HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
		downloadRequest.setDataTransferType(dataTransferType);
		downloadRequest.setArchiveLocation(archiveLocation);
		downloadRequest.setGlobusDestination(globusDownloadDestination);
		downloadRequest.setS3Destination(s3DownloadDestination);
		downloadRequest.setGoogleDriveDestination(googleDriveDownloadDestination);
		downloadRequest.setPath(path);
		downloadRequest.setConfigurationId(configurationId);
		downloadRequest.setS3ArchiveConfigurationId(s3ArchiveConfigurationId);
		downloadRequest.setUserId(userId);
		downloadRequest.setCompletionEvent(completionEvent);
		downloadRequest.setSize(size);

		// Create a download response.
		HpcDataObjectDownloadResponse response = new HpcDataObjectDownloadResponse();

		// Get the base archive destination.
		HpcArchive baseArchiveDestination = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType)
				.getBaseArchiveDestination();

		// There are 5 methods of downloading data object:
		// 0. Data is in deep archive, restoration is required. Supported by S3(Cloudian)
		// archive.
		// 1. Synchronous download via REST API. Supported by S3 & POSIX
		// archives.
		// 2. Asynchronous download using Globus. Supported by S3 (in a 2-hop
		// solution) & POSIX archives.
		// 3. Asynchronous download via streaming data object from S3 Archive to user
		// provided S3 bucket. Supported by S3 archive only.
		// 4. Asynchronous download via streaming data object from S3 Archive to user
		// provided Google Drive. Supported by S3 archive only.
		if(deepArchiveStatus != null) {
			// If status is DEEP_ARCHIVE, and object is not restored, submit a restore request
			// and create a dataObjectDownloadTask with status RESTORE_REQUESTED
			HpcS3ObjectMetadata objectMetadata = dataTransferProxies.get(HpcDataTransferType.S_3).getDataObjectMetadata(
					getAuthenticatedToken(HpcDataTransferType.S_3, downloadRequest.getConfigurationId(),
							downloadRequest.getS3ArchiveConfigurationId()),
					downloadRequest.getArchiveLocation());
			
			String restorationStatus = objectMetadata.getRestorationStatus();
			
			if (restorationStatus != null && !restorationStatus.equals("success")) {
				requestObjectRestore(downloadRequest, response, restorationStatus);
				return response;
			}		
		} 

		if (globusDownloadDestination == null && s3DownloadDestination == null
				&& googleDriveDownloadDestination == null) {
			// This is a synchronous download request.
			performSynchronousDownload(downloadRequest, response, baseArchiveDestination, synchronousDownloadFilter);

		} else if (dataTransferType.equals(HpcDataTransferType.GLOBUS) && globusDownloadDestination != null) {
			// This is an asynchronous download request from a file system archive to a
			// Globus destination.
			// Note: this can also be a 2nd hop download from temporary file-system archive
			// to a Globus destination (after the 1st hop completed).
			performGlobusAsynchronousDownload(downloadRequest, response);

		} else if (dataTransferType.equals(HpcDataTransferType.S_3) && globusDownloadDestination != null) {
			// This is an asynchronous download request from a S3 archive to a
			// Globus destination. It is performed in 2-hops.
			perform2HopDownload(downloadRequest, response, baseArchiveDestination);

		} else if (dataTransferType.equals(HpcDataTransferType.S_3) && s3DownloadDestination != null) {
			// This is an asynchronous download request from a S3 archive to a AWS
			// S3 destination.
			performS3AsynchronousDownload(downloadRequest, response, baseArchiveDestination);

		} else if (dataTransferType.equals(HpcDataTransferType.S_3) && googleDriveDownloadDestination != null) {
			// This is an asynchronous download request from a S3 archive to a
			// Google Drive destination.
			performGoogleDriveAsynchronousDownload(downloadRequest, response, baseArchiveDestination);

		} else {
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
		taskResult.setResult(HpcDownloadResult.COMPLETED);
		taskResult.setType(HpcDownloadTaskType.DATA_OBJECT);
		taskResult.setCompletionEvent(false);
		taskResult.setCreated(now);
		taskResult.setSize(size);
		taskResult.setCompleted(now);

		// Persist to the DB.
		dataDownloadDAO.upsertDownloadTaskResult(taskResult);

		return downloadRequestURL;
	}

	@Override
	public HpcS3ObjectMetadata addSystemGeneratedMetadataToDataObject(HpcFileLocation fileLocation,
			HpcDataTransferType dataTransferType, String configurationId, String s3ArchiveConfigurationId,
			String objectId, String registrarId) throws HpcException {
		// Add metadata is done by copying the object to itself w/ attached metadata.
		// Check that the data transfer system can accept transfer requests.
		boolean globusSyncUpload = dataTransferType.equals(HpcDataTransferType.GLOBUS) && fileLocation != null;

		String checksum = dataTransferProxies.get(dataTransferType).setDataObjectMetadata(
				!globusSyncUpload ? getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId)
						: null,
				fileLocation,
				dataManagementConfigurationLocator
						.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType)
						.getBaseArchiveDestination(),
				generateMetadata(objectId, registrarId));
		
		HpcS3ObjectMetadata objectMetadata = new HpcS3ObjectMetadata();
		objectMetadata.setChecksum(checksum);
		
		if (dataTransferType.equals(HpcDataTransferType.S_3)
				&& dataTransferProxies.get(dataTransferType).existsLifecyclePolicy(
						getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId),
						fileLocation)) {
			// Add deep_archive_status in progress
			objectMetadata.setDeepArchiveStatus(HpcDeepArchiveStatus.IN_PROGRESS);
		}
		return objectMetadata;
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
			String dataTransferRequestId, String configurationId, String s3ArchiveConfigurationId) throws HpcException {
		// Input validation.
		if (dataTransferRequestId == null) {
			throw new HpcException("Null data transfer request ID", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the data transfer configuration.
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);

		return dataTransferProxies.get(dataTransferType).getDataTransferUploadStatus(
				getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId),
				dataTransferRequestId, dataTransferConfiguration.getBaseArchiveDestination());
	}

	@Override
	public HpcDataTransferDownloadReport getDataTransferDownloadStatus(HpcDataTransferType dataTransferType,
			String dataTransferRequestId, String configurationId, String s3ArchiveConfigurationId) throws HpcException {
		// Input validation.
		if (dataTransferRequestId == null) {
			throw new HpcException("Null data transfer request ID", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return dataTransferProxies.get(dataTransferType).getDataTransferDownloadStatus(
				getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId),
				dataTransferRequestId);
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
		if (!isValidS3Account(s3Account)) {
			throw new HpcException("Invalid S3 Account", HpcErrorType.INVALID_REQUEST_INPUT);
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
	public HpcPathAttributes getPathAttributes(String accessToken, HpcFileLocation fileLocation, boolean getSize)
			throws HpcException {
		// Input validation.
		if (!isValidFileLocation(fileLocation)) {
			throw new HpcException("Invalid file location", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (StringUtils.isEmpty(accessToken)) {
			throw new HpcException("Invalid Google Drive access token", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// This is Google Drive only functionality, so we get the Google Drive
		// data-transfer-proxy.
		HpcDataTransferProxy dataTransferProxy = dataTransferProxies.get(HpcDataTransferType.GOOGLE_DRIVE);
		try {
			return dataTransferProxy.getPathAttributes(dataTransferProxy.authenticate(accessToken), fileLocation,
					getSize);

		} catch (HpcException e) {
			throw new HpcException("Failed to access Google Drive: [" + e.getMessage() + "] " + fileLocation,
					HpcErrorType.INVALID_REQUEST_INPUT, e);
		}
	}

	@Override
	public HpcPathAttributes getPathAttributes(HpcFileLocation fileLocation) throws HpcException {
		// Input validation.
		if (!isValidFileLocation(fileLocation)) {
			throw new HpcException("Invalid file location", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		try {
			HpcPathAttributes pathAttributes = new HpcPathAttributes();
			pathAttributes.setIsDirectory(false);
			pathAttributes.setIsFile(false);
			pathAttributes.setSize(0);
			pathAttributes.setIsAccessible(true);

			Path path = FileSystems.getDefault().getPath(fileLocation.getFileId());
			pathAttributes.setExists(Files.exists(path));
			if (pathAttributes.getExists()) {
				pathAttributes.setIsAccessible(Files.isReadable(path));
				pathAttributes.setIsDirectory(Files.isDirectory(path));
				pathAttributes.setIsFile(Files.isRegularFile(path));
			}
			if (pathAttributes.getIsFile()) {
				pathAttributes.setSize(Files.size(path));
			}

			return pathAttributes;

		} catch (IOException e) {
			throw new HpcException("Failed to get local file attributes: [" + e.getMessage() + "] " + fileLocation,
					HpcErrorType.INVALID_REQUEST_INPUT, e);
		}
	}

	@Override
	public List<HpcDirectoryScanItem> scanDirectory(HpcDataTransferType dataTransferType, HpcS3Account s3Account,
			String googleDriveAccessToken, HpcFileLocation directoryLocation, String configurationId,
			String s3ArchiveConfigurationId, List<String> includePatterns, List<String> excludePatterns,
			HpcPatternType patternType) throws HpcException {
		// Input validation.
		if (!HpcDomainValidator.isValidFileLocation(directoryLocation)) {
			throw new HpcException("Invalid directory location", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (dataTransferType == null) {
			if ((!StringUtils.isEmpty(googleDriveAccessToken) || s3Account != null
					|| !StringUtils.isEmpty(s3ArchiveConfigurationId))) {
				throw new HpcException("S3 account / Google Drive token provided File System scan",
						HpcErrorType.UNEXPECTED_ERROR);
			}
		} else {
			if (!dataTransferType.equals(HpcDataTransferType.S_3) && s3Account != null) {
				throw new HpcException("S3 account provided for Non S3 data transfer", HpcErrorType.UNEXPECTED_ERROR);
			}
			if (!dataTransferType.equals(HpcDataTransferType.GOOGLE_DRIVE)
					&& !StringUtils.isEmpty(googleDriveAccessToken)) {
				throw new HpcException("Google Drive access token provided for Non Google Drive data transfer",
						HpcErrorType.UNEXPECTED_ERROR);
			}
		}
		List<HpcDirectoryScanItem> scanItems = null;
		if (dataTransferType == null) {
			// Scan a directory in local DME server NAS.
			scanItems = scanDirectory(directoryLocation);
		} else {
			// Globus / S3 / Google Drive scan.

			// If an S3 account or Google Drive access token was provided, then we use it to
			// get authenticated token, otherwise, we use a system account token.
			Object authenticatedToken = null;
			if (s3Account != null) {
				authenticatedToken = dataTransferProxies.get(dataTransferType).authenticate(s3Account);
			} else if (!StringUtils.isEmpty(googleDriveAccessToken)) {
				authenticatedToken = dataTransferProxies.get(dataTransferType).authenticate(googleDriveAccessToken);
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
		if (fileId == null) {
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
	public boolean getCollectionDownloadTaskCancellationRequested(String taskId) {
		try {
			return dataDownloadDAO.getCollectionDownloadTaskCancellationRequested(taskId);

		} catch (HpcException e) {
			logger.error("Failed to get cancellation request for task ID: " + taskId, e);
			// If it can not find the collection download task, it is cancelled and removed
			// from the table.
			return true;
		}
	}

	@Override
	public void completeDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask, HpcDownloadResult result,
			String message, Calendar completed, long bytesTransferred) throws HpcException {
		// Input validation
		if (downloadTask == null) {
			throw new HpcException("Invalid data object download task", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		logger.info("download task: {} - completed - {} [transfer-type={}, destination-type={}]", downloadTask.getId(),
				result.value(), downloadTask.getDataTransferType(), downloadTask.getDestinationType());

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
				logger.info("download task: {} - cancelled Globus task [transfer-type={}, destination-type={}]",
						downloadTask.getId(), downloadTask.getDataTransferType(), downloadTask.getDestinationType());

			} catch (HpcException e) {
				logger.error("Failed to cancel Globus task for user[{}], path: {}", e, downloadTask.getUserId(),
						downloadTask.getPath());
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
			logger.info("download task: {} - Delete file at scratch space: {}", downloadTask.getId(),
					downloadTask.getDownloadFilePath());
			if (!FileUtils.deleteQuietly(new File(downloadTask.getDownloadFilePath()))) {
				logger.error("Failed to delete file: {}", downloadTask.getDownloadFilePath());
			}
		}

		// Cleanup the DB record.
		dataDownloadDAO.deleteDataObjectDownloadTask(downloadTask.getId());

		// Create a task result object.
		HpcDownloadTaskResult taskResult = new HpcDownloadTaskResult();
		taskResult.setId(downloadTask.getId());
		taskResult.setUserId(downloadTask.getUserId());
		taskResult.setPath(downloadTask.getPath());
		taskResult.setDataTransferRequestId(downloadTask.getDataTransferRequestId());
		taskResult.setDataTransferType(downloadTask.getDataTransferType());
		taskResult.setDestinationType(downloadTask.getDestinationType());
		if (downloadTask.getGlobusDownloadDestination() != null) {
			taskResult.setDestinationLocation(downloadTask.getGlobusDownloadDestination().getDestinationLocation());
		} else if (downloadTask.getS3DownloadDestination() != null) {
			taskResult.setDestinationLocation(downloadTask.getS3DownloadDestination().getDestinationLocation());
		} else if (downloadTask.getGoogleDriveDownloadDestination() != null) {
			taskResult
					.setDestinationLocation(downloadTask.getGoogleDriveDownloadDestination().getDestinationLocation());
		}
		taskResult.setDestinationType(downloadTask.getDestinationType());
		taskResult.setResult(result);
		taskResult.setType(HpcDownloadTaskType.DATA_OBJECT);
		taskResult.setMessage(message);
		taskResult.setCompletionEvent(downloadTask.getCompletionEvent());
		taskResult.setCreated(downloadTask.getCreated());
		taskResult.setSize(downloadTask.getSize());
		taskResult.setCompleted(completed);
		taskResult.setRestoreRequested(downloadTask.getRestoreRequested());

		// Calculate the effective transfer speed (Bytes per second).
		taskResult.setEffectiveTransferSpeed(Math.toIntExact(bytesTransferred * 1000
				/ (taskResult.getCompleted().getTimeInMillis() - taskResult.getCreated().getTimeInMillis())));

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
			taskResult.setEffectiveTransferSpeed(Math.toIntExact(taskResult.getSize() * 1000
					/ (taskResult.getCompleted().getTimeInMillis() - taskResult.getCreated().getTimeInMillis())));
		}

		dataDownloadDAO.upsertDownloadTaskResult(taskResult);
	}

	@Override
	public void continueDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask) throws HpcException {
		// Check if transfer requests can be acceptable at this time.
		if (!acceptsTransferRequests(downloadTask.getDataTransferType(), downloadTask.getConfigurationId(),
				downloadTask.getS3ArchiveConfigurationId())) {
			logger.info(
					"download task: {} - transfer requests not accepted at this time [transfer-type={}, destination-type={}]",
					downloadTask.getId(), downloadTask.getDataTransferType(), downloadTask.getDestinationType());
			return;
		}

		// Recreate the download request from the task (that was persisted).
		HpcDataTransferProgressListener progressListener = null;
		HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
		downloadRequest.setArchiveLocation(downloadTask.getArchiveLocation());
		downloadRequest.setCompletionEvent(downloadTask.getCompletionEvent());
		downloadRequest.setDataTransferType(downloadTask.getDataTransferType());
		downloadRequest.setConfigurationId(downloadTask.getConfigurationId());
		downloadRequest.setS3ArchiveConfigurationId(downloadTask.getS3ArchiveConfigurationId());
		downloadRequest.setPath(downloadTask.getPath());
		downloadRequest.setUserId(downloadTask.getUserId());
		downloadRequest.setGlobusDestination(downloadTask.getGlobusDownloadDestination());
		downloadRequest.setS3Destination(downloadTask.getS3DownloadDestination());
		downloadRequest.setGoogleDriveDestination(downloadTask.getGoogleDriveDownloadDestination());

		// Get the data transfer configuration.
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(downloadRequest.getConfigurationId(),
						downloadRequest.getS3ArchiveConfigurationId(), downloadRequest.getDataTransferType());

		// If the destination is Globus and the data transfer is S3, then we need to
		// restart a 2-hop download.
		if (downloadTask.getDestinationType().equals(HpcDataTransferType.GLOBUS)
				&& downloadTask.getDataTransferType().equals(HpcDataTransferType.S_3)) {
			// Create a listener that will kick off the 2nd hop when the first one is done,
			// and update the download task accordingly.
			HpcSecondHopDownload secondHopDownload = new HpcSecondHopDownload(downloadTask);
			progressListener = secondHopDownload;

			// Validate that the 2-hop download can be performed at this time.
			if (!canPerfom2HopDownload(secondHopDownload)) {
				logger.info(
						"download task: {} - 2 Hop download can't be restarted. Low screatch space [transfer-type={}, destination-type={}]",
						downloadTask.getId(), downloadTask.getDataTransferType(), downloadTask.getDestinationType());
				return;
			}

			// Set the first hop transfer to be from S3 Archive to the DME server's Globus
			// mounted file system.
			downloadRequest.setArchiveLocation(metadataService
					.getDataObjectSystemGeneratedMetadata(downloadRequest.getPath()).getArchiveLocation());
			downloadRequest.setFileDestination(secondHopDownload.getSourceFile());
		}

		// If the destination is AWS S3 or Google Drive, we need to restart the
		// download.
		if (downloadTask.getDestinationType().equals(HpcDataTransferType.S_3)
				|| downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_DRIVE)) {
			// Create a listener that will complete the download task when it is done.
			progressListener = new HpcStreamingDownload(downloadTask, dataDownloadDAO, eventService, this);
		}

		// If the destination is Google Drive, we need to generate a download URL from
		// the archive.
		if (downloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_DRIVE)) {
			downloadRequest.setArchiveLocationURL(generateDownloadRequestURL(downloadRequest.getPath(),
					downloadRequest.getArchiveLocation(), HpcDataTransferType.S_3, downloadRequest.getConfigurationId(),
					downloadRequest.getS3ArchiveConfigurationId()));
		}

		// Submit a transfer request.
		try {
			downloadTask.setDataTransferRequestId(
					dataTransferProxies.get(downloadRequest.getDataTransferType()).downloadDataObject(
							downloadRequest.getDataTransferType().equals(HpcDataTransferType.GOOGLE_DRIVE) ? null
									: getAuthenticatedToken(downloadRequest.getDataTransferType(),
											downloadRequest.getConfigurationId(),
											downloadRequest.getS3ArchiveConfigurationId()),
							downloadRequest, dataTransferConfiguration.getBaseArchiveDestination(), progressListener));

		} catch (HpcException e) {
			// Failed to submit a transfer request. Cleanup the download task.
			completeDataObjectDownloadTask(downloadTask, HpcDownloadResult.FAILED, e.getMessage(),
					Calendar.getInstance(), 0);
			return;
		}

		// Persist the download task. Note: In case we used a progress listener - it
		// took care of that already. If the task was cancelled (by another task), we
		// don't persist.
		if (progressListener == null && !Optional
				.ofNullable(dataDownloadDAO.getDataObjectDownloadTaskStatus(downloadTask.getId()))
				.orElse(HpcDataTransferDownloadStatus.CANCELED).equals(HpcDataTransferDownloadStatus.CANCELED)) {
			downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.IN_PROGRESS);
			dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
		}

		logger.info("download task: {} - continued  - archive file-id = {} [transfer-type={}, destination-type={}]",
				downloadTask.getId(), downloadRequest.getArchiveLocation().getFileId(),
				downloadTask.getDataTransferType(), downloadTask.getDestinationType());
	}

	@Override
	public void resetDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask) throws HpcException {
		downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.RECEIVED);
		downloadTask.setInProcess(false);
		if (!StringUtils.isEmpty(downloadTask.getDownloadFilePath())) {
			FileUtils.deleteQuietly(new File(downloadTask.getDownloadFilePath()));
			downloadTask.setDownloadFilePath(null);
		}

		dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
	}

	@Override
	public void resetDataObjectDownloadTasksInProcess() throws HpcException {
		dataDownloadDAO.resetDataObjectDownloadTaskInProcess();
	}

	@Override
	public void markProcessedDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask, boolean inProcess)
			throws HpcException {
		// Only set in-process to true if this task in a RECEIVED status, and the
		// in-process not already true.
		if (!inProcess || (inProcess && !downloadTask.getInProcess()
				&& downloadTask.getDataTransferStatus().equals(HpcDataTransferDownloadStatus.RECEIVED))) {
			dataDownloadDAO.setDataObjectDownloadTaskInProcess(downloadTask.getId(), inProcess);
		}

		Calendar processed = Calendar.getInstance();
		dataDownloadDAO.setDataObjectDownloadTaskProcessed(downloadTask.getId(), processed);

		downloadTask.setProcessed(processed);
		downloadTask.setInProcess(inProcess);
	}

	@Override
	public void updateDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask, long bytesTransferred)
			throws HpcException {
		// Input validation. Note: we only expect this to be called while Globus
		// transfer is in-progress
		// Currently, bytesTransferred are not available while S3 download is in
		// progress.
		if (downloadTask == null || downloadTask.getSize() <= 0 || bytesTransferred <= 0
				|| bytesTransferred > downloadTask.getSize()
				|| downloadTask.getDataTransferType().equals(HpcDataTransferType.S_3)) {
			return;
		}

		// Calculate the percent complete.
		float percentComplete = 100 * (float) bytesTransferred / downloadTask.getSize();
		if (dataManagementConfigurationLocator
				.getDataTransferConfiguration(downloadTask.getConfigurationId(),
						downloadTask.getS3ArchiveConfigurationId(), downloadTask.getDataTransferType())
				.getBaseArchiveDestination().getType().equals(HpcArchiveType.TEMPORARY_ARCHIVE)) {
			// This is a 2-hop download, and S_3 is complete. Our base % complete is 50%.
			downloadTask.setPercentComplete(50 + Math.round(percentComplete) / 2);
		} else {
			// This is a one-hop Globus download from archive to user destination (currently
			// only supported by file system archive).
			downloadTask.setPercentComplete(Math.round(percentComplete));
		}

		dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
	}

	@Override
	public HpcCollectionDownloadTask downloadCollection(String path,
			HpcGlobusDownloadDestination globusDownloadDestination, HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDriveDownloadDestination googleDriveDownloadDestination, String userId, String configurationId)
			throws HpcException {
		// Validate the download destination.
		validateDownloadDestination(globusDownloadDestination, s3DownloadDestination, googleDriveDownloadDestination,
				null, configurationId, true);

		// Create a new collection download task.
		HpcCollectionDownloadTask downloadTask = new HpcCollectionDownloadTask();
		downloadTask.setCreated(Calendar.getInstance());
		downloadTask.setGlobusDownloadDestination(globusDownloadDestination);
		downloadTask.setS3DownloadDestination(s3DownloadDestination);
		downloadTask.setGoogleDriveDownloadDestination(googleDriveDownloadDestination);
		downloadTask.setPath(path);
		downloadTask.setUserId(userId);
		downloadTask.setType(HpcDownloadTaskType.COLLECTION);
		downloadTask.setStatus(HpcCollectionDownloadTaskStatus.RECEIVED);
		downloadTask.setConfigurationId(configurationId);
		downloadTask.setAppendPathToDownloadDestination(false);

		// Persist the request.
		dataDownloadDAO.upsertCollectionDownloadTask(downloadTask);

		return downloadTask;
	}

	@Override
	public HpcCollectionDownloadTask downloadCollections(List<String> collectionPaths,
			HpcGlobusDownloadDestination globusDownloadDestination, HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDriveDownloadDestination googleDriveDownloadDestination, String userId, String configurationId,
			boolean appendPathToDownloadDestination) throws HpcException {
		// Validate the download destination.
		validateDownloadDestination(globusDownloadDestination, s3DownloadDestination, googleDriveDownloadDestination,
				null, configurationId, true);

		// Create a new collection download task.
		HpcCollectionDownloadTask downloadTask = new HpcCollectionDownloadTask();
		downloadTask.setCreated(Calendar.getInstance());
		downloadTask.setGlobusDownloadDestination(globusDownloadDestination);
		downloadTask.setS3DownloadDestination(s3DownloadDestination);
		downloadTask.setGoogleDriveDownloadDestination(googleDriveDownloadDestination);
		downloadTask.getCollectionPaths().addAll(collectionPaths);
		downloadTask.setUserId(userId);
		downloadTask.setType(HpcDownloadTaskType.COLLECTION_LIST);
		downloadTask.setStatus(HpcCollectionDownloadTaskStatus.RECEIVED);
		downloadTask.setConfigurationId(configurationId);
		downloadTask.setAppendPathToDownloadDestination(appendPathToDownloadDestination);

		// Persist the request.
		dataDownloadDAO.upsertCollectionDownloadTask(downloadTask);

		return downloadTask;
	}

	@Override
	public HpcCollectionDownloadTask downloadDataObjects(List<String> dataObjectPaths,
			HpcGlobusDownloadDestination globusDownloadDestination, HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDriveDownloadDestination googleDriveDownloadDestination, String userId, String configurationId,
			boolean appendPathToDownloadDestination) throws HpcException {
		// Validate the requested destination location. Note: we use the configuration
		// ID of one data object path. At this time, there is no need to validate for
		// all
		// configuration IDs.
		validateDownloadDestination(globusDownloadDestination, s3DownloadDestination, googleDriveDownloadDestination,
				null, configurationId, true);

		// Create a new collection download task.
		HpcCollectionDownloadTask downloadTask = new HpcCollectionDownloadTask();
		downloadTask.setCreated(Calendar.getInstance());
		downloadTask.setGlobusDownloadDestination(globusDownloadDestination);
		downloadTask.setS3DownloadDestination(s3DownloadDestination);
		downloadTask.setGoogleDriveDownloadDestination(googleDriveDownloadDestination);
		downloadTask.getDataObjectPaths().addAll(dataObjectPaths);
		downloadTask.setUserId(userId);
		downloadTask.setType(HpcDownloadTaskType.DATA_OBJECT_LIST);
		downloadTask.setStatus(HpcCollectionDownloadTaskStatus.RECEIVED);
		downloadTask.setConfigurationId(configurationId);
		downloadTask.setAppendPathToDownloadDestination(appendPathToDownloadDestination);

		// Persist the request.
		dataDownloadDAO.upsertCollectionDownloadTask(downloadTask);

		return downloadTask;
	}

	@Override
	public void updateCollectionDownloadTask(HpcCollectionDownloadTask downloadTask) throws HpcException {
		dataDownloadDAO.upsertCollectionDownloadTask(downloadTask);
	}

	@Override
	public void cancelCollectionDownloadTask(HpcCollectionDownloadTask downloadTask) throws HpcException {
		if (downloadTask.getStatus().equals(HpcCollectionDownloadTaskStatus.RECEIVED)) {
			dataDownloadDAO.setCollectionDownloadTaskCancellationRequested(downloadTask.getId(), true);
		}

		cancelCollectionDownloadTaskItems(downloadTask.getItems());
	}

	@Override
	public void cancelCollectionDownloadTaskItems(List<HpcCollectionDownloadTaskItem> downloadItems)
			throws HpcException {
		for (HpcCollectionDownloadTaskItem downloadItem : downloadItems) {
			dataDownloadDAO.updateDataObjectDownloadTaskStatus(downloadItem.getDataObjectDownloadTaskId(),
					cancelCollectionDownloadTaskItemsFilter, HpcDataTransferDownloadStatus.CANCELED);
		}
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

		// Create a task result object.
		HpcDownloadTaskResult taskResult = new HpcDownloadTaskResult();
		taskResult.setId(downloadTask.getId());
		taskResult.setUserId(downloadTask.getUserId());
		taskResult.setPath(downloadTask.getPath());
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
		}
		taskResult.setResult(result);
		taskResult.setType(downloadTask.getType());
		taskResult.setMessage(message);
		taskResult.setCompletionEvent(true);
		taskResult.setCreated(downloadTask.getCreated());
		taskResult.setCompleted(completed);
		taskResult.getItems().addAll(downloadTask.getItems());

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
			downloadRequests = dataDownloadDAO.getDataObjectDownloadRequestsForDoc(doc);
			downloadRequests.addAll(dataDownloadDAO.getCollectionDownloadRequestsForDoc(doc));
		}
		return downloadRequests;
	}

	@Override
	public List<HpcUserDownloadRequest> getDownloadResults(String userId, int page, String doc) throws HpcException {
		List<HpcUserDownloadRequest> downloadResults = null;
		if (doc == null) {
			downloadResults = dataDownloadDAO.getDownloadResults(userId, pagination.getOffset(page),
					pagination.getPageSize());
		} else if (doc.equals("ALL")) {
			downloadResults = dataDownloadDAO.getAllDownloadResults(pagination.getOffset(page),
					pagination.getPageSize());
		} else {
			downloadResults = dataDownloadDAO.getDownloadResultsForDoc(doc, pagination.getOffset(page),
					pagination.getPageSize());
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
			count = dataDownloadDAO.getDownloadResultsCountForDoc(doc);
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
				.getFileContainerName(dataTransferType.equals(HpcDataTransferType.GOOGLE_DRIVE) ? null
						: getAuthenticatedToken(dataTransferType, configurationId, null), fileContainerId);
	}

	/**
	 * Calculate a data object upload % complete. Note: if upload not in progress,
	 * null is returned.
	 *
	 * @param systemGeneratedMetadata The system generated metadata of the data
	 *                                object.
	 * @return The transfer % completion if transfer is in progress, or null
	 *         otherwise.
	 */
	@Override
	public Integer calculateDataObjectUploadPercentComplete(HpcSystemGeneratedMetadata systemGeneratedMetadata) {
		// Get the data transfer info from the metadata entries.
		HpcDataTransferUploadStatus dataTransferStatus = systemGeneratedMetadata.getDataTransferStatus();
		HpcDataTransferType dataTransferType = systemGeneratedMetadata.getDataTransferType();
		if (dataTransferStatus == null || dataTransferType == null) {
			return null;
		}

		if (dataTransferStatus.equals(HpcDataTransferUploadStatus.IN_TEMPORARY_ARCHIVE)) {
			// Transfer is exactly half way in a 2-hop upload.
			return 50;
		}

		if (dataTransferStatus.equals(HpcDataTransferUploadStatus.IN_PROGRESS_TO_TEMPORARY_ARCHIVE)
				|| dataTransferStatus.equals(HpcDataTransferUploadStatus.IN_PROGRESS_TO_ARCHIVE)) {
			// Data transfer is in progress.
			if (dataTransferType.equals(HpcDataTransferType.S_3)) {
				// We don't have visibility into S3 transfer. Return a 50.
				return 50;
			}

			String dataTransferRequestId = systemGeneratedMetadata.getDataTransferRequestId();
			String configurationId = systemGeneratedMetadata.getConfigurationId();
			String s3ArchiveConfigurationId = systemGeneratedMetadata.getS3ArchiveConfigurationId();
			Long sourceSize = systemGeneratedMetadata.getSourceSize();
			if (configurationId == null || dataTransferRequestId == null || sourceSize == null || sourceSize <= 0) {
				return null;
			}

			// Get the size of the data transferred so far.
			long transferSize = 0;
			try {
				// Get the data transfer configuration.
				HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
						.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);

				transferSize = dataTransferProxies.get(dataTransferType)
						.getDataTransferUploadStatus(
								getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId),
								dataTransferRequestId, dataTransferConfiguration.getBaseArchiveDestination())
						.getBytesTransferred();

			} catch (HpcException e) {
				logger.error("Failed to get data transfer upload status: " + dataTransferRequestId, e);
				return null;
			}

			float percentComplete = (float) 100 * transferSize / sourceSize;
			if (dataTransferStatus.equals(HpcDataTransferUploadStatus.IN_PROGRESS_TO_TEMPORARY_ARCHIVE)) {
				// Transfer is in 1st hop of 2-hop upload. The Globus % complete is half of the
				// overall upload.
				percentComplete /= 2;
			}

			return Math.round(percentComplete);
		}

		return null;
	}

	@Override
	public boolean uploadURLExpired(Calendar urlCreated, String configurationId, String s3ArchiveConfigurationId) {
		if (urlCreated == null || StringUtils.isEmpty(configurationId)) {
			return true;
		}

		// Get the URL expiration period (in hours) from the configuration.
		Integer uploadRequestURLExpiration = null;
		try {
			uploadRequestURLExpiration = dataManagementConfigurationLocator
					.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, HpcDataTransferType.S_3)
					.getUploadRequestURLExpiration();

		} catch (HpcException e) {
			logger.error("Failed to get URL expiration from config", e);
			return true;
		}

		// Calculate the expiration time.
		Date expiration = new Date();
		expiration.setTime(urlCreated.getTimeInMillis() + 1000 * 60 * 60 * uploadRequestURLExpiration);

		// Return true if the current time is passed the expiration time.
		return expiration.before(new Date());
	}

	@Override
	public List<HpcDataObjectDownloadTask> getDataObjectDownloadTaskByStatus(
			HpcDataTransferDownloadStatus dataTransferStatus)
			throws HpcException {
		return dataDownloadDAO.getDataObjectDownloadTaskByStatus(dataTransferStatus);
	}
	
	@Override
	public void tierDataObject(String userId, String path, HpcFileLocation hpcFileLocation, HpcDataTransferType dataTransferType, String configurationId) throws HpcException {
		// Input Validation.
		if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(path) || StringUtils.isEmpty(configurationId)
				|| !isValidFileLocation(hpcFileLocation)) {
			throw new HpcException("Invalid tiering request", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the S3 archive configuration ID.
		String s3ArchiveConfigurationId = dataManagementConfigurationLocator.get(configurationId)
				.getS3UploadConfigurationId();
			
		// Get the data transfer configuration.
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);
			
		String prefix = hpcFileLocation.getFileId();
		// Create life cycle policy with this data object
		dataTransferProxies.get(dataTransferType).putLifecyclePolicy(
				getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId),
				hpcFileLocation, prefix, dataTransferConfiguration.getTieringBucket());
		// Add a record of the lifecycle rule
		lifecycleDAO.insert(userId, HpcLifecycleRequestType.TIER_DATA_OBJECT, s3ArchiveConfigurationId,
				Calendar.getInstance(), prefix);
	}

	@Override
	public void tierCollection(String userId, String path, HpcDataTransferType dataTransferType, String configurationId) throws HpcException {
		
		// Input Validation.
		if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(path) || StringUtils.isEmpty(configurationId)) {
			throw new HpcException("Invalid tiering request", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		// Get the S3 archive configuration ID.
		String s3ArchiveConfigurationId = dataManagementConfigurationLocator.get(configurationId)
				.getS3UploadConfigurationId();
		
		// Get the data transfer configuration.
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);
		
		HpcFileLocation hpcFileLocation = dataTransferConfiguration.getBaseArchiveDestination().getFileLocation();
				
		String prefix = hpcFileLocation.getFileId() + "/" + path + "/";
		// Create life cycle policy with this collection
		dataTransferProxies.get(dataTransferType).putLifecyclePolicy(
				getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId),
				hpcFileLocation, prefix, dataTransferConfiguration.getTieringBucket());
		// Add a record of the lifecycle rule
		lifecycleDAO.insert(userId, HpcLifecycleRequestType.TIER_COLLECTION, s3ArchiveConfigurationId,
				Calendar.getInstance(), prefix);
	}

	@Override
	public void tierDataObjects(String userId, HpcBulkTierRequest bulkTierRequest, HpcDataTransferType dataTransferType) throws HpcException {
		// Input Validation.
		if (StringUtils.isEmpty(userId) || !isValidTierItems(bulkTierRequest)) {
			throw new HpcException("Invalid tiering request", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		// Create life cycle policy with these data objects
		for (HpcBulkTierItem item : bulkTierRequest.getItems()) {
			// Get the S3 archive configuration ID.
			String s3ArchiveConfigurationId = dataManagementConfigurationLocator.get(item.getConfigurationId())
					.getS3UploadConfigurationId();
			
			// Get the data transfer configuration.
			HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
					.getDataTransferConfiguration(item.getConfigurationId(), s3ArchiveConfigurationId, dataTransferType);
			
			HpcFileLocation hpcFileLocation = dataTransferConfiguration.getBaseArchiveDestination().getFileLocation();
			
			String prefix = item.getPath();
			// Create life cycle policy with this collection
			dataTransferProxies.get(dataTransferType).putLifecyclePolicy(
					getAuthenticatedToken(dataTransferType, item.getConfigurationId(), s3ArchiveConfigurationId),
					hpcFileLocation, prefix, dataTransferConfiguration.getTieringBucket());
			
			// Add a record of the lifecycle rule
			lifecycleDAO.insert(userId, HpcLifecycleRequestType.TIER_DATA_OBJECT, s3ArchiveConfigurationId,
					Calendar.getInstance(), prefix);
		}
	}

	@Override
	public void tierCollections(String userId, HpcBulkTierRequest bulkTierRequest, HpcDataTransferType dataTransferType) throws HpcException {
		// Input Validation.
		if (StringUtils.isEmpty(userId) || !isValidTierItems(bulkTierRequest)) {
			throw new HpcException("Invalid archive request", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		// Create life cycle policy with these data objects
		for (HpcBulkTierItem item : bulkTierRequest.getItems()) {
			// Get the S3 archive configuration ID.
			String s3ArchiveConfigurationId = dataManagementConfigurationLocator.get(item.getConfigurationId())
					.getS3UploadConfigurationId();
			
			// Get the data transfer configuration.
			HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
					.getDataTransferConfiguration(item.getConfigurationId(), s3ArchiveConfigurationId, dataTransferType);
			
			HpcFileLocation hpcFileLocation = dataTransferConfiguration.getBaseArchiveDestination().getFileLocation();
			
			String prefix = hpcFileLocation.getFileId() + item.getPath() + "/";
			// Create life cycle policy with this collection
			dataTransferProxies.get(dataTransferType).putLifecyclePolicy(
					getAuthenticatedToken(dataTransferType, item.getConfigurationId(), s3ArchiveConfigurationId),
					hpcFileLocation, prefix, dataTransferConfiguration.getTieringBucket());
			// Add a record of the lifecycle rule
			lifecycleDAO.insert(userId, HpcLifecycleRequestType.TIER_COLLECTION, s3ArchiveConfigurationId,
					Calendar.getInstance(), prefix);
		}
	}
	
	@Override
	public HpcS3ObjectMetadata getDataObjectMetadata(HpcFileLocation fileLocation,
			HpcDataTransferType dataTransferType, String configurationId, String s3ArchiveConfigurationId) throws HpcException {

		return dataTransferProxies.get(dataTransferType).getDataObjectMetadata(
				getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId),
				fileLocation);
	}

	@Override
	public boolean isTieringSupported(String configurationId, String s3ArchiveConfigurationId,
			HpcDataTransferType dataTransferType) throws HpcException {
		// Get the data transfer configuration (Globus or S3).
		HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType);


		return HpcIntegratedSystem.CLOUDIAN.equals(dataTransferConfiguration.getArchiveProvider()) || HpcIntegratedSystem.AWS.equals(dataTransferConfiguration.getArchiveProvider());
	}

	@Override
	public boolean deepArchiveDelayed(Calendar deepArchiveDate) {
		if (deepArchiveDate == null) {
			return true;
		}

		// Check if there is a delay in toggling the status
		Date expiration = new Date();
		expiration.setTime(deepArchiveDate.getTimeInMillis() + 1000 * 60 * 60 * maxDeepArchiveInProgressDays * 24);
		// If delayed, return true
		return expiration.before(new Date());
	}
	
	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Get the data transfer authenticated token from the request context. If it's
	 * not in the context, get a token by authenticating.
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

		// Search for an existing token.
		for (HpcDataTransferAuthenticatedToken authenticatedToken : invoker.getDataTransferAuthenticatedTokens()) {
			if (authenticatedToken.getDataTransferType().equals(dataTransferType)
					&& authenticatedToken.getConfigurationId().equals(configurationId)
					&& (authenticatedToken.getS3ArchiveConfigurationId() == null || authenticatedToken
							.getS3ArchiveConfigurationId().equals(dataTransferConfiguration.getId()))) {
				return authenticatedToken.getDataTransferAuthenticatedToken();
			}
		}

		// No authenticated token found for this request. Create one.
		HpcIntegratedSystemAccount dataTransferSystemAccount = null;
		if (dataTransferType.equals(HpcDataTransferType.GLOBUS)) {
			dataTransferSystemAccount = systemAccountLocator.getSystemAccount(dataTransferType, configurationId);
		} else if (dataTransferType.equals(HpcDataTransferType.S_3)) {
			dataTransferSystemAccount = systemAccountLocator
					.getSystemAccount(dataTransferConfiguration.getArchiveProvider());
		}

		if (dataTransferSystemAccount == null) {
			throw new HpcException("System account not registered for " + dataTransferType.value(),
					HpcErrorType.UNEXPECTED_ERROR);
		}

		// Authenticate with the data transfer system.
		Object token = dataTransferProxies.get(dataTransferType).authenticate(dataTransferSystemAccount,
				dataTransferConfiguration.getUrlOrRegion());
		if (token == null) {
			throw new HpcException("Invalid data transfer account credentials", HpcErrorType.DATA_TRANSFER_ERROR,
					dataTransferSystemAccount.getIntegratedSystem());
		}

		// Store token on the request context.
		HpcDataTransferAuthenticatedToken authenticatedToken = new HpcDataTransferAuthenticatedToken();
		authenticatedToken.setDataTransferAuthenticatedToken(token);
		authenticatedToken.setDataTransferType(dataTransferType);
		authenticatedToken.setConfigurationId(configurationId);
		authenticatedToken.setS3ArchiveConfigurationId(dataTransferConfiguration.getId());
		authenticatedToken.setSystemAccountId(dataTransferSystemAccount.getUsername());
		invoker.getDataTransferAuthenticatedTokens().add(authenticatedToken);
		HpcRequestContext.setRequestInvoker(invoker);

		return token;
	}

	/**
	 * Generate metadata to attach to the data object in the archive: 1. UUID - the
	 * data object UUID in the DM system (iRODS) 2. User ID - the user id that
	 * registered the data object.
	 *
	 * @param objectId    The data object UUID.
	 * @param registrarId The user-id uploaded the data.
	 * @return a List of the 2 metadata.
	 */
	private List<HpcMetadataEntry> generateMetadata(String objectId, String registrarId) {
		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();

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

		return metadataEntries;
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
	 * @param uploadRequest   The data upload request.
	 * @param configurationId The data management configuration ID.
	 * @return A data object upload response.
	 * @throws HpcException on service failure.
	 */
	private HpcDataObjectUploadResponse uploadDataObject(HpcDataObjectUploadRequest uploadRequest,
			String configurationId) throws HpcException {
		// Determine the data transfer type to use in this upload request (i.e. Globus
		// or S3).
		HpcDataTransferType dataTransferType = getUploadDataTransferType(uploadRequest, configurationId);

		// Get the S3 archive configuration ID.
		String s3ArchiveConfigurationId = dataManagementConfigurationLocator.get(configurationId)
				.getS3UploadConfigurationId();

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
			// The upload from a file system source is done in a scheduled task
			HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
			uploadResponse.setDataTransferType(dataTransferType);
			uploadResponse.setSourceSize(uploadRequest.getSourceSize());
			uploadResponse.setUploadSource(uploadRequest.getFileSystemUploadSource().getSourceLocation());
			uploadResponse.setDataTransferStarted(Calendar.getInstance());
			uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.IN_FILE_SYSTEM);
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
			progressListener = new HpcStreamingUpload(uploadRequest.getPath(), uploadRequest.getUserId(),
					uploadRequest.getS3UploadSource().getSourceLocation(), eventService);
		}

		// Get a source InputStream and instantiate a progress listener for upload from
		// Google Drive.
		if (uploadRequest.getGoogleDriveUploadSource() != null) {
			HpcDataTransferProxy dataTransferProxy = dataTransferProxies.get(HpcDataTransferType.GOOGLE_DRIVE);
			uploadRequest.getGoogleDriveUploadSource()
					.setSourceInputStream(dataTransferProxy.generateDownloadInputStream(
							dataTransferProxy.authenticate(uploadRequest.getGoogleDriveUploadSource().getAccessToken()),
							uploadRequest.getGoogleDriveUploadSource().getSourceLocation()));
			progressListener = new HpcStreamingUpload(uploadRequest.getPath(), uploadRequest.getUserId(),
					uploadRequest.getGoogleDriveUploadSource().getSourceLocation(), eventService);
		}

		// Upload the data object using the appropriate data transfer system proxy and
		// archive
		// configuration.
		return dataTransferProxies.get(dataTransferType).uploadDataObject(
				!globusSyncUpload ? getAuthenticatedToken(dataTransferType, configurationId, s3ArchiveConfigurationId)
						: null,
				uploadRequest, dataTransferConfiguration.getBaseArchiveDestination(),
				dataTransferConfiguration.getUploadRequestURLExpiration(), progressListener,
				generateMetadata(uploadRequest.getDataObjectId(), uploadRequest.getUserId()));
	}

	/**
	 * Validate upload source file location.
	 *
	 * @param globusUploadSource      The Globus source to validate.
	 * @param s3UploadSource          The S3 source to validate.
	 * @param googleDriveUploadSource The Google Drive source to validate.
	 * @param fileSystemUploadSource  The File System (DME server NAS) source to
	 *                                validate.
	 * @param sourceFile              The source file to validate.
	 * @param configurationId         The configuration ID (needed to determine the
	 *                                archive connection config).
	 * @return the upload source file size.
	 * @throws HpcException if the upload source location doesn't exist, or not
	 *                      accessible, or it's a directory.
	 */
	private Long validateUploadSourceFileLocation(HpcUploadSource globusUploadSource,
			HpcStreamingUploadSource s3UploadSource, HpcStreamingUploadSource googleDriveUploadSource,
			HpcUploadSource fileSystemUploadSource, File sourceFile, String configurationId) throws HpcException {
		if (sourceFile != null) {
			return sourceFile.length();
		}

		HpcPathAttributes pathAttributes = null;
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
				return s3UploadSource.getSourceSize();
			}
			sourceFileLocation = s3UploadSource.getSourceLocation();
			pathAttributes = getPathAttributes(s3UploadSource.getAccount(), s3UploadSource.getSourceLocation(), true);

		} else if (googleDriveUploadSource != null) {
			sourceFileLocation = googleDriveUploadSource.getSourceLocation();
			pathAttributes = getPathAttributes(googleDriveUploadSource.getAccessToken(),
					googleDriveUploadSource.getSourceLocation(), true);

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

		return pathAttributes.getSize();
	}

	/**
	 * Validate download destination.
	 *
	 * @param globusDownloadDestination      The user requested Glopbus download
	 *                                       destination.
	 * @param s3DownloadDestination          The user requested S3 download
	 *                                       destination.
	 * @param googleDriveDownloadDestination The user requested Google Drive
	 *                                       download destination.
	 * @param synchronousDownloadFilter      (Optional) synchronous download filter
	 *                                       to extract specific files from a data
	 *                                       object that is 'compressed archive'
	 *                                       such as ZIP.
	 * @param configurationId                The configuration ID.
	 * @param bulkDownload                   True if this is a request to download a
	 *                                       list of files or a collection, or false
	 *                                       if this is a request to download a
	 *                                       single data object.
	 * @throws HpcException if the download destination is invalid
	 */
	private void validateDownloadDestination(HpcGlobusDownloadDestination globusDownloadDestination,
			HpcS3DownloadDestination s3DownloadDestination,
			HpcGoogleDriveDownloadDestination googleDriveDownloadDestination,
			HpcSynchronousDownloadFilter synchronousDownloadFilter, String configurationId, boolean bulkDownload)
			throws HpcException {
		// Validate the destination (if provided) is either Globus, S3, or Google Drive.
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
		if (destinations > 1) {
			throw new HpcException("Multiple download destinations provided (Globus, S3, Google Drive)",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate an optional synchronous download filter is provided only for a
		// synchronous download request.
		if (destinations == 1 && synchronousDownloadFilter != null) {
			throw new HpcException(
					"A download destination (Globus, S3, Google Drive) provided w/ synchronous download filter",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the destination for collection/bulk download is provided.
		if (bulkDownload && destinations == 0) {
			throw new HpcException("No download destination provided (Globus, S3, Google Drive)",
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

			if (!isValidS3Account(s3DownloadDestination.getAccount())) {
				throw new HpcException("Invalid S3 account", HpcErrorType.INVALID_REQUEST_INPUT);
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

		if (uploadRequest.getGoogleDriveUploadSource() != null) {
			// It's an asynchronous upload request from Google Drive. This is only supported
			// S3 archive.
			if (archiveDataTransferType.equals(HpcDataTransferType.GLOBUS)) {
				throw new HpcException("Google Drive upload source not supported by POSIX archive",
						HpcErrorType.INVALID_REQUEST_INPUT);
			}

			// We use the S_3 data transfer proxy to stream files from Google Drive to an S3
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
	 * @param baseArchiveDestination    The base archive destination of the
	 *                                  requested data object.
	 * @param synchronousDownloadFilter (Optional) synchronous download filter to
	 *                                  extract specific files from a data object
	 *                                  that is 'compressed archive' such as ZIP.
	 * @throws HpcException on service failure.
	 */
	private void performSynchronousDownload(HpcDataObjectDownloadRequest downloadRequest,
			HpcDataObjectDownloadResponse response, HpcArchive baseArchiveDestination,
			HpcSynchronousDownloadFilter synchronousDownloadFilter) throws HpcException {
		// Validate max file size not exceeded.
		if (maxSyncDownloadFileSize != null && downloadRequest.getSize() > maxSyncDownloadFileSize) {
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

		// Perform the synchronous download.
		dataTransferProxies.get(downloadRequest.getDataTransferType())
				.downloadDataObject(
						getAuthenticatedToken(downloadRequest.getDataTransferType(),
								downloadRequest.getConfigurationId(), downloadRequest.getS3ArchiveConfigurationId()),
						downloadRequest, baseArchiveDestination, null);

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

		// Persist to the DB.
		dataDownloadDAO.upsertDownloadTaskResult(taskResult);
	}

	/**
	 * Perform a globus asynchronous download. This method submits a download task.
	 *
	 * @param downloadRequest The data object download request.
	 * @param response        The download response object. This method sets
	 *                        download task id and destination location on the
	 *                        response.
	 * @throws HpcException on service failure.
	 */
	private void performGlobusAsynchronousDownload(HpcDataObjectDownloadRequest downloadRequest,
			HpcDataObjectDownloadResponse response) throws HpcException {
		// Create a download task.
		HpcDataObjectDownloadTask downloadTask = new HpcDataObjectDownloadTask();
		downloadTask.setArchiveLocation(downloadRequest.getArchiveLocation());
		downloadTask.setCompletionEvent(downloadRequest.getCompletionEvent());
		downloadTask.setConfigurationId(downloadRequest.getConfigurationId());
		downloadTask.setS3ArchiveConfigurationId(downloadRequest.getS3ArchiveConfigurationId());
		downloadTask.setCreated(Calendar.getInstance());
		downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.RECEIVED);
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
		downloadTask.setUserId(downloadRequest.getUserId());

		// Persist the download task. The download will be performed by a scheduled task
		// picking up
		// this task in its next scheduled run.
		dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
		response.setDownloadTaskId(downloadTask.getId());
		response.setDestinationLocation(downloadTask.getGlobusDownloadDestination().getDestinationLocation());
	}

	/**
	 * Perform a download request to user's provided AWS S3 destination from S3
	 * archive.
	 *
	 * @param downloadRequest        The data object download request.
	 * @param response               The download response object. This method sets
	 *                               download task id and destination location on
	 *                               the response.
	 * @param baseArchiveDestination The base archive destination of the requested
	 *                               data object.
	 * @throws HpcException on service failure.
	 */
	private void performS3AsynchronousDownload(HpcDataObjectDownloadRequest downloadRequest,
			HpcDataObjectDownloadResponse response, HpcArchive baseArchiveDestination) throws HpcException {

		HpcStreamingDownload s3Download = new HpcStreamingDownload(downloadRequest, dataDownloadDAO, eventService,
				this);

		// Perform the S3 download (From S3 Archive to User's AWS S3 bucket).
		try {
			dataTransferProxies.get(HpcDataTransferType.S_3).downloadDataObject(
					getAuthenticatedToken(HpcDataTransferType.S_3, downloadRequest.getConfigurationId(),
							downloadRequest.getS3ArchiveConfigurationId()),
					downloadRequest, baseArchiveDestination, s3Download);

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
	 * @param downloadRequest        The data object download request.
	 * @param response               The download response object. This method sets
	 *                               download task id and destination location on
	 *                               the response.
	 * @param baseArchiveDestination The base archive destination of the requested
	 *                               data object.
	 * @throws HpcException on service failure.
	 */
	private void performGoogleDriveAsynchronousDownload(HpcDataObjectDownloadRequest downloadRequest,
			HpcDataObjectDownloadResponse response, HpcArchive baseArchiveDestination) throws HpcException {

		HpcStreamingDownload googleDriveDownload = new HpcStreamingDownload(downloadRequest, dataDownloadDAO,
				eventService, this);

		// Generate a download URL from the S3 archive.
		downloadRequest.setArchiveLocationURL(generateDownloadRequestURL(downloadRequest.getPath(),
				downloadRequest.getArchiveLocation(), HpcDataTransferType.S_3, downloadRequest.getConfigurationId(),
				downloadRequest.getS3ArchiveConfigurationId()));

		// Perform the download (From S3 Archive to User's Google Drive).
		try {
			dataTransferProxies.get(HpcDataTransferType.GOOGLE_DRIVE).downloadDataObject(null, downloadRequest,
					baseArchiveDestination, googleDriveDownload);

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
	 * Perform a 2 hop download. i.e. store the data to a local GLOBUS endpoint, and
	 * submit a transfer request to the caller's GLOBUS destination. Both first and
	 * second hop downloads are performed asynchronously.
	 *
	 * @param downloadRequest        The data object download request.
	 * @param response               The download response object. This method sets
	 *                               download task id and destination location on
	 *                               the response. it exists.
	 * @param baseArchiveDestination The base archive destination of the requested
	 *                               data object.
	 * @throws HpcException on service failure.
	 */
	private void perform2HopDownload(HpcDataObjectDownloadRequest downloadRequest,
			HpcDataObjectDownloadResponse response, HpcArchive baseArchiveDestination) throws HpcException {

		HpcSecondHopDownload secondHopDownload = new HpcSecondHopDownload(downloadRequest, HpcDataTransferDownloadStatus.IN_PROGRESS);

		// Set the first hop file destination to be the source file of the second hop.
		downloadRequest.setFileDestination(secondHopDownload.getSourceFile());

		// Populate the response object.
		response.setDownloadTaskId(secondHopDownload.getDownloadTask().getId());
		response.setDestinationLocation(
				secondHopDownload.getDownloadTask().getGlobusDownloadDestination().getDestinationLocation());

		// Perform the first hop download (From S3 Archive to DME Server local file
		// system).
		try {
			if (canPerfom2HopDownload(secondHopDownload)) {
				dataTransferProxies.get(HpcDataTransferType.S_3).downloadDataObject(
						getAuthenticatedToken(HpcDataTransferType.S_3, downloadRequest.getConfigurationId(),
								downloadRequest.getS3ArchiveConfigurationId()),
						downloadRequest, baseArchiveDestination, secondHopDownload);

				logger.info("download task: {} - 1st hop started. [transfer-type={}, destination-type={}]",
						secondHopDownload.downloadTask.getId(), secondHopDownload.downloadTask.getDataTransferType(),
						secondHopDownload.downloadTask.getDestinationType());
			} else {
				// Can't perform the 2-hop download at this time. Reset the task
				resetDataObjectDownloadTask(secondHopDownload.getDownloadTask());

				logger.info(
						"download task: {} - 2 Hop download can't be initiated. Low screatch space [transfer-type={}, destination-type={}]",
						secondHopDownload.downloadTask.getId(), secondHopDownload.downloadTask.getDataTransferType(),
						secondHopDownload.downloadTask.getDestinationType());
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
	private boolean canPerfom2HopDownload(HpcSecondHopDownload secondHopDownload) {
		try {
			long freeSpace = Files
					.getFileStore(FileSystems.getDefault().getPath(secondHopDownload.getSourceFile().getAbsolutePath()))
					.getUsableSpace();
			if (secondHopDownload.getDownloadTask().getSize() > freeSpace) {
				// Not enough space disk space to perform the first hop download. Log an error
				// and reset the
				// task.
				logger.error("Insufficient disk space to download {}. Free Space: {} bytes. File size: {} bytes",
						secondHopDownload.getDownloadTask().getPath(), freeSpace,
						secondHopDownload.getDownloadTask().getSize());
				return false;
			}

		} catch (IOException e) {
			// Failed to check free disk space. We'll try the download.
			logger.error("Failed to determine free space", e);
		}

		return true;
	}

	/**
	 * Generate a (pre-signed) download URL for a data object file.
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

		final List<HpcDataTransferAuthenticatedToken> invokerTokens = HpcRequestContext.getRequestInvoker()
				.getDataTransferAuthenticatedTokens();
		String globusClientId = null;

		logger.info("checkIfTransferCanBeLaunched: searching for token within invoker state");

		for (HpcDataTransferAuthenticatedToken someToken : invokerTokens) {
			if (someToken.getDataTransferType().equals(dataTransferType)
					&& someToken.getConfigurationId().equals(configurationId)) {
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

		this.systemAccountLocator.setGlobusAccountQueueSize(globusClientId, transferAcceptanceResponse.getQueueSize());

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
	 * @param downloadRequest        The data object download request.
	 * @param response               The download response object. This method sets
	 *                               download task id and destination location on
	 *                               the response.
	 * @param restorationStatus      The restoration status.
	 * @throws HpcException on service failure.
	 */
	private void requestObjectRestore(HpcDataObjectDownloadRequest downloadRequest,
			HpcDataObjectDownloadResponse response, String restorationStatus) throws HpcException {

		try {
				
			//Submit Restore request if there is no ongoing restore request
			if (restorationStatus != null && restorationStatus.equals("not in progress")) {
				dataTransferProxies.get(HpcDataTransferType.S_3).restoreDataObject(
						getAuthenticatedToken(HpcDataTransferType.S_3, downloadRequest.getConfigurationId(),
								downloadRequest.getS3ArchiveConfigurationId()),
						downloadRequest.getArchiveLocation());
			} 
			
			// Create a download task.
			HpcDataObjectDownloadTask downloadTask = new HpcDataObjectDownloadTask();
			downloadTask.setArchiveLocation(downloadRequest.getArchiveLocation());
			downloadTask.setCompletionEvent(downloadRequest.getCompletionEvent());
			downloadTask.setConfigurationId(downloadRequest.getConfigurationId());
			downloadTask.setS3ArchiveConfigurationId(downloadRequest.getS3ArchiveConfigurationId());
			downloadTask.setCreated(Calendar.getInstance());
			downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.RESTORE_REQUESTED);
			downloadTask.setInProcess(false);
			downloadTask.setPercentComplete(0);
			downloadTask.setSize(downloadRequest.getSize());
			downloadTask.setPath(downloadRequest.getPath());
			downloadTask.setUserId(downloadRequest.getUserId());
			downloadTask.setDataTransferType(downloadRequest.getDataTransferType());
			downloadTask.setGlobusDownloadDestination(downloadRequest.getGlobusDestination());
			downloadTask.setS3DownloadDestination(downloadRequest.getS3Destination());
			downloadTask.setGoogleDriveDownloadDestination(downloadRequest.getGoogleDriveDestination());
			downloadTask.setRestoreRequested(true);
			
			if (downloadTask.getS3DownloadDestination() != null) {
				downloadTask.setDataTransferType(HpcDataTransferType.S_3);
				downloadTask.setDestinationType(HpcDataTransferType.S_3);
				downloadTask.setId(UUID.randomUUID().toString());
				dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
				response.setDestinationLocation(downloadTask.getS3DownloadDestination().getDestinationLocation());
		    } else if (downloadTask.getGoogleDriveDownloadDestination() != null) {
		    	downloadTask.setDataTransferType(HpcDataTransferType.GOOGLE_DRIVE);
		    	downloadTask.setDestinationType(HpcDataTransferType.GOOGLE_DRIVE);
		    	downloadTask.setId(UUID.randomUUID().toString());
		    	dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
		    	response.setDestinationLocation(
		    			downloadTask.getGoogleDriveDownloadDestination().getDestinationLocation());
		    } else if (downloadRequest.getGlobusDestination() != null) {
		    	downloadTask.setDestinationType(HpcDataTransferType.GLOBUS);
		    	HpcSecondHopDownload secondHopDownload = new HpcSecondHopDownload(downloadRequest, HpcDataTransferDownloadStatus.RESTORE_REQUESTED);
		    	downloadTask.setId(secondHopDownload.getDownloadTask().getId());
				response.setDestinationLocation(downloadTask.getGlobusDownloadDestination().getDestinationLocation());
		    } else {
		    	downloadTask.setDataTransferType(HpcDataTransferType.S_3);
				downloadTask.setDestinationType(HpcDataTransferType.S_3);
		    	downloadTask.setId(UUID.randomUUID().toString());
		    	HpcFileLocation destinationLocation = new HpcFileLocation();
				destinationLocation.setFileContainerId("Synchronous Download");
				destinationLocation.setFileId("Synchronous Download");
				HpcGlobusDownloadDestination dummyGlobusDownloadDestination = new HpcGlobusDownloadDestination();
				dummyGlobusDownloadDestination.setDestinationLocation(destinationLocation);
				downloadTask.setGlobusDownloadDestination(dummyGlobusDownloadDestination);
				dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
				response.setDestinationLocation(destinationLocation);
		    }
			
			// Populate the response object.
			response.setDownloadTaskId(downloadTask.getId());
			response.setRestoreInProgress(true);
			
		} catch (HpcException e) {
			throw (e);
		}
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

		// ---------------------------------------------------------------------//
		// Constructors
		// ---------------------------------------------------------------------//

		/**
		 * Constructs a 2nd Hop download object (to keep track of async processing)
		 *
		 * @param firstHopDownloadRequest The first hop download request.
		 * @param dataTransferDownloadStatus The download status.
		 * @throws HpcException If it failed to create a download task.
		 */
		public HpcSecondHopDownload(HpcDataObjectDownloadRequest firstHopDownloadRequest, HpcDataTransferDownloadStatus dataTransferDownloadStatus) throws HpcException {
			// Create the second-hop archive location and destination
			HpcFileLocation secondHopArchiveLocation = getDownloadSourceLocation(
					firstHopDownloadRequest.getConfigurationId(), firstHopDownloadRequest.getS3ArchiveConfigurationId(),
					HpcDataTransferType.GLOBUS);
			HpcGlobusDownloadDestination secondHopGlobusDestination = new HpcGlobusDownloadDestination();
			secondHopGlobusDestination.setDestinationLocation(calculateGlobusDownloadDestinationFileLocation(
					firstHopDownloadRequest.getGlobusDestination().getDestinationLocation(),
					firstHopDownloadRequest.getGlobusDestination().getDestinationOverwrite(),
					HpcDataTransferType.GLOBUS, firstHopDownloadRequest.getPath(),
					firstHopDownloadRequest.getConfigurationId()));
			secondHopGlobusDestination
					.setDestinationOverwrite(firstHopDownloadRequest.getGlobusDestination().getDestinationOverwrite());

			// Get the data transfer configuration.
			HpcDataTransferConfiguration dataTransferConfiguration = dataManagementConfigurationLocator
					.getDataTransferConfiguration(firstHopDownloadRequest.getConfigurationId(),
							firstHopDownloadRequest.getS3ArchiveConfigurationId(), HpcDataTransferType.GLOBUS);

			// Create the source file for the second hop download.
			sourceFile = createFile(getFilePath(secondHopArchiveLocation.getFileId(),
					dataTransferConfiguration.getBaseDownloadSource()));

			// Create and persist a download task. This object tracks the download request
			// through the 2-hop async download requests.
			createDownloadTask(firstHopDownloadRequest, secondHopArchiveLocation, secondHopGlobusDestination, dataTransferDownloadStatus);

			logger.info("download task: {} - 2 Hop download created. Path at scratch space: {}", downloadTask.getId(),
					sourceFile.getAbsolutePath());
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
			HpcFileLocation secondHopArchiveLocation = getDownloadSourceLocation(downloadTask.getConfigurationId(),
					downloadTask.getS3ArchiveConfigurationId(), HpcDataTransferType.GLOBUS);

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

			logger.info("download task: {} - 2 Hop download created. Path at scratch space: {}", downloadTask.getId(),
					sourceFile.getAbsolutePath());
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
							"download task: {} - 1st Hop completed but task was cancelled [transfer-type={}, destination-type={}]",
							downloadTask.getId(), downloadTask.getDataTransferType(),
							downloadTask.getDestinationType());

				} else {
					// Update the download task to reflect 1st hop transfer completed, and second
					// received.
					downloadTask.setDataTransferType(HpcDataTransferType.GLOBUS);
					downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.RECEIVED);
					downloadTask.setInProcess(false);

					// Persist the download task.
					dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
				}

				logger.info("download task: {} - 1st hop completed. Path at scratch space: {}", downloadTask.getId(),
						sourceFile.getAbsolutePath());

			} catch (HpcException e) {
				logger.error("Failed to update download task", e);
				downloadFailed(e.getMessage());
			}
		}

		// This callback method is called when the first hop download failed.
		@Override
		public void transferFailed(String message) {
			logger.info("download task: {} - 1 Hop download Failed. Path at scratch space: {}", downloadTask.getId(),
					sourceFile.getAbsolutePath());
			downloadFailed("Failed to get data from archive via S3: " + message);
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
		 * @throws HpcException If it failed to persist the task.
		 */
		private void createDownloadTask(HpcDataObjectDownloadRequest firstHopDownloadRequest,
				HpcFileLocation secondHopArchiveLocation, HpcGlobusDownloadDestination secondHopGlobusDestination,
				HpcDataTransferDownloadStatus dataTransferDownloadStatus)
				throws HpcException {

			downloadTask.setDataTransferType(HpcDataTransferType.S_3);
			downloadTask.setDataTransferStatus(dataTransferDownloadStatus);
			downloadTask.setDownloadFilePath(sourceFile.getAbsolutePath());
			downloadTask.setUserId(firstHopDownloadRequest.getUserId());
			downloadTask.setPath(firstHopDownloadRequest.getPath());
			downloadTask.setConfigurationId(firstHopDownloadRequest.getConfigurationId());
			downloadTask.setS3ArchiveConfigurationId(firstHopDownloadRequest.getS3ArchiveConfigurationId());
			downloadTask.setCompletionEvent(firstHopDownloadRequest.getCompletionEvent());
			downloadTask.setArchiveLocation(secondHopArchiveLocation);
			downloadTask.setGlobusDownloadDestination(secondHopGlobusDestination);
			downloadTask.setDestinationType(HpcDataTransferType.GLOBUS);
			downloadTask.setCreated(Calendar.getInstance());
			downloadTask.setPercentComplete(0);
			downloadTask.setSize(firstHopDownloadRequest.getSize());
			if(HpcDataTransferDownloadStatus.RESTORE_REQUESTED.equals(dataTransferDownloadStatus))
				downloadTask.setRestoreRequested(true);

			dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
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
			this.downloadTask.setDestinationType(HpcDataTransferType.GLOBUS);
			this.downloadTask.setCompletionEvent(downloadTask.getCompletionEvent());
			this.downloadTask.setCreated(downloadTask.getCreated());
			this.downloadTask.setPercentComplete(0);
			this.downloadTask.setSize(downloadTask.getSize());

			dataDownloadDAO.upsertDataObjectDownloadTask(this.downloadTask);
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
							HpcDownloadTaskType.DATA_OBJECT, downloadTask.getId(),
							downloadTask.getGlobusDownloadDestination().getDestinationLocation(),
							transferFailedTimestamp, message);
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

		/**
		 * Get download source location.
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
		private HpcFileLocation getDownloadSourceLocation(String configurationId, String s3ArchiveConfigurationId,
				HpcDataTransferType dataTransferType) throws HpcException {
			// Get the data transfer configuration.
			HpcArchive baseDownloadSource = dataManagementConfigurationLocator
					.getDataTransferConfiguration(configurationId, s3ArchiveConfigurationId, dataTransferType)
					.getBaseDownloadSource();

			// Create a source location. (This is a local GLOBUS endpoint).
			HpcFileLocation sourceLocation = new HpcFileLocation();
			sourceLocation.setFileContainerId(baseDownloadSource.getFileLocation().getFileContainerId());
			sourceLocation
					.setFileId(baseDownloadSource.getFileLocation().getFileId() + "/" + UUID.randomUUID().toString());

			return sourceLocation;
		}
	}

}
