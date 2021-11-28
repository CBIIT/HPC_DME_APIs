package gov.nih.nci.hpc.integration.s3.impl;

import static gov.nih.nci.hpc.integration.HpcDataTransferProxy.getArchiveDestinationLocation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Rule;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Transition;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.RestoreObjectRequest;
import com.amazonaws.services.s3.model.SetBucketLifecycleConfigurationRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilterPredicate;
import com.amazonaws.services.s3.model.lifecycle.LifecyclePrefixPredicate;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.Upload;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveObjectMetadata;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadMethod;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDeepArchiveStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcMultipartUpload;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcStreamingUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadPartETag;
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadPartURL;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.model.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;

/**
 * HPC Data Transfer Proxy S3 Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataTransferProxyImpl implements HpcDataTransferProxy {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// The expiration of streaming data request from 3rd Party S3 Archive
	// (Cleversafe, Cloudian, etc) to AWS S3.
	private static final int S3_STREAM_EXPIRATION = 96;

	// Cloudian tiering info header required when adding tiering rule
	private static final String CLOUDIAN_TIERING_INFO_HEADER = "x-gmt-tieringinfo";

	// Number of days the restored data object will be available.
	@Value("${hpc.integration.s3.tieringEndpoint}")
	private String tieringEndpoint = null;

	// Number of days the restored data object will be available.
	@Value("${hpc.integration.s3.restoreNumDays}")
	private int restoreNumDays = 2;

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The S3 connection instance.
	@Autowired
	private HpcS3Connection s3Connection = null;

	// The S3 download executor.
	@Autowired
	@Qualifier("hpcS3DownloadExecutor")
	Executor s3Executor = null;

	// Date formatter to format files last-modified date
	private DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for spring injection. */
	private HpcDataTransferProxyImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataTransferProxy Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount, String urlOrRegion,
			String encryptionAlgorithm, String encryptionKey) throws HpcException {
		return s3Connection.authenticate(dataTransferAccount, urlOrRegion, encryptionAlgorithm, encryptionKey);
	}

	@Override
	public Object authenticate(HpcS3Account s3Account) throws HpcException {
		return s3Connection.authenticate(s3Account);
	}

	@Override
	public HpcDataObjectUploadResponse uploadDataObject(Object authenticatedToken,
			HpcDataObjectUploadRequest uploadRequest, HpcArchive baseArchiveDestination,
			Integer uploadRequestURLExpiration, HpcDataTransferProgressListener progressListener,
			List<HpcMetadataEntry> metadataEntries, Boolean encryptedTransfer, String storageClass)
			throws HpcException {
		if (uploadRequest.getGlobusUploadSource() != null) {
			throw new HpcException("Invalid upload source", HpcErrorType.UNEXPECTED_ERROR);
		}

		// Calculate the archive destination.
		HpcFileLocation archiveDestinationLocation = getArchiveDestinationLocation(
				baseArchiveDestination.getFileLocation(), uploadRequest.getPath(), uploadRequest.getCallerObjectId(),
				baseArchiveDestination.getType(), false);

		// If the archive destination file exists, generate a new archive destination w/
		// unique path.
		if (getPathAttributes(authenticatedToken, archiveDestinationLocation, false).getExists()) {
			archiveDestinationLocation = getArchiveDestinationLocation(baseArchiveDestination.getFileLocation(),
					uploadRequest.getPath(), uploadRequest.getCallerObjectId(), baseArchiveDestination.getType(), true);
		}

		if (uploadRequest.getGenerateUploadRequestURL()) {
			int uploadParts = Optional.ofNullable(uploadRequest.getUploadParts()).orElse(1);
			if (uploadParts == 1) {
				// Generate an upload request URL for the caller to use to upload directly.
				return generateUploadRequestURL(authenticatedToken, archiveDestinationLocation,
						uploadRequestURLExpiration, uploadRequest.getUploadRequestURLChecksum());
			} else {
				return generateMultipartUploadRequestURLs(authenticatedToken, archiveDestinationLocation,
						uploadRequestURLExpiration, uploadParts, metadataEntries, storageClass);
			}
		} else if (uploadRequest.getSourceFile() != null) {
			// Upload a file
			return uploadDataObject(authenticatedToken, uploadRequest.getSourceFile(), archiveDestinationLocation,
					progressListener, baseArchiveDestination.getType(), metadataEntries, storageClass);
		} else {
			// Upload by streaming from AWS, 3rd Party S3 Provider, Google Drive or Google
			// Cloud Storage source.
			return uploadDataObject(authenticatedToken, uploadRequest.getS3UploadSource(),
					uploadRequest.getGoogleDriveUploadSource(), uploadRequest.getGoogleCloudStorageUploadSource(),
					archiveDestinationLocation, baseArchiveDestination, uploadRequest.getSourceSize(), progressListener,
					metadataEntries, storageClass);
		}
	}

	@Override
	public String downloadDataObject(Object authenticatedToken, HpcDataObjectDownloadRequest downloadRequest,
			HpcArchive baseArchiveDestination, HpcDataTransferProgressListener progressListener,
			Boolean encryptedTransfer) throws HpcException {
		if(downloadRequest.getArchiveLocation() == null) {
			throw new HpcException("Null archive location", HpcErrorType.UNEXPECTED_ERROR);
		}
		
		if (downloadRequest.getFileDestination() != null) {
			// This is a download request to a local file.
			return downloadDataObject(authenticatedToken, downloadRequest.getArchiveLocation(),
					downloadRequest.getFileDestination(), progressListener);
		} else {
			// This is a download to S3 destination (either AWS or 3rd Party Provider).
			return downloadDataObject(authenticatedToken, downloadRequest.getArchiveLocation(),
					downloadRequest.getArchiveLocationURL(), baseArchiveDestination, downloadRequest.getS3Destination(),
					progressListener, downloadRequest.getSize());
		}
	}

	@Override
	public String generateDownloadRequestURL(Object authenticatedToken, HpcFileLocation archiveSourceLocation,
			HpcArchive baseArchiveDestination, Integer downloadRequestURLExpiration) throws HpcException {

		// Calculate the URL expiration date.
		Date expiration = new Date();
		expiration.setTime(expiration.getTime() + 1000 * 60 * 60 * downloadRequestURLExpiration);

		// Create a URL generation request.
		GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
				archiveSourceLocation.getFileContainerId(), archiveSourceLocation.getFileId())
						.withMethod(HttpMethod.GET).withExpiration(expiration);

		// Generate the pre-signed URL.
		URL url = s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client()
				.generatePresignedUrl(generatePresignedUrlRequest);

		return url.toString();
	}

	@Override
	public String setDataObjectMetadata(Object authenticatedToken, HpcFileLocation fileLocation,
			HpcArchive baseArchiveDestination, List<HpcMetadataEntry> metadataEntries, String sudoPassword,
			String storageClass) throws HpcException {

		// Check if the metadata was already set on the data-object in the S3 archive.
		try {
			ObjectMetadata s3Metadata = s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client()
					.getObjectMetadata(fileLocation.getFileContainerId(), fileLocation.getFileId());
			boolean metadataAlreadySet = true;
			for (HpcMetadataEntry metadataEntry : metadataEntries) {
				if (s3Metadata.getUserMetaDataOf(metadataEntry.getAttribute()) == null) {
					metadataAlreadySet = false;
					break;
				}
			}

			if (metadataAlreadySet) {
				logger.info("System metadata in S3 archive already set for [{}]. No need to copy-object in archive",
						fileLocation.getFileId());
				return s3Metadata.getETag();
			}

		} catch (AmazonClientException ace) {
			throw new HpcException("[S3] Failed to get object metadata: " + ace.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, ace);
		}

		// We set S3 metadata by copying the data-object to itself w/ attached metadata.
		CopyObjectRequest copyRequest = new CopyObjectRequest(fileLocation.getFileContainerId(),
				fileLocation.getFileId(), fileLocation.getFileContainerId(), fileLocation.getFileId())
						.withNewObjectMetadata(toS3Metadata(metadataEntries)).withStorageClass(storageClass);

		try {
			CopyObjectResult copyResult = s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client()
					.copyObject(copyRequest);
			return copyResult != null ? copyResult.getETag() : null;

		} catch (AmazonServiceException ase) {
			throw new HpcException("[S3] Failed to copy file: " + copyRequest, HpcErrorType.DATA_TRANSFER_ERROR,
					s3Connection.getS3Provider(authenticatedToken), ase);

		} catch (AmazonClientException ace) {
			throw new HpcException("[S3] Failed to copy file: " + copyRequest, HpcErrorType.DATA_TRANSFER_ERROR,
					s3Connection.getS3Provider(authenticatedToken), ace);
		}
	}

	@Override
	public void deleteDataObject(Object authenticatedToken, HpcFileLocation fileLocation,
			HpcArchive baseArchiveDestination, String sudoPassword) throws HpcException {
		// Create a S3 delete request.
		DeleteObjectRequest deleteRequest = new DeleteObjectRequest(fileLocation.getFileContainerId(),
				fileLocation.getFileId());
		try {
			s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client().deleteObject(deleteRequest);

		} catch (AmazonServiceException ase) {
			throw new HpcException("[S3] Failed to delete file: " + deleteRequest, HpcErrorType.DATA_TRANSFER_ERROR,
					s3Connection.getS3Provider(authenticatedToken), ase);

		} catch (AmazonClientException ace) {
			throw new HpcException("[S3] Failed to delete file: " + deleteRequest, HpcErrorType.DATA_TRANSFER_ERROR,
					s3Connection.getS3Provider(authenticatedToken), ace);
		}
	}

	@Override
	public HpcPathAttributes getPathAttributes(Object authenticatedToken, HpcFileLocation fileLocation, boolean getSize)
			throws HpcException {
		HpcPathAttributes pathAttributes = new HpcPathAttributes();
		ObjectMetadata metadata = null;
		Boolean fileExists = null;

		// Look for the file.
		try {
			pathAttributes.setIsAccessible(true);
			metadata = s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client()
					.getObjectMetadata(fileLocation.getFileContainerId(), fileLocation.getFileId());
			fileExists = true;
			if (metadata == null) {
				logger.error("[S3] Received null object metadata for {}:{}", fileLocation.getFileContainerId(),
						fileLocation.getFileId());
			}

		} catch (AmazonServiceException ase) {
			if (ase.getStatusCode() == 404) {
				fileExists = false;
			} else if (ase.getStatusCode() == 403) {
				pathAttributes.setIsAccessible(false);
				return pathAttributes;
			} else {
				throw new HpcException("[S3] Failed to get object metadata: " + ase.getMessage(),
						HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), ase);
			}

		} catch (AmazonClientException ace) {
			throw new HpcException("[S3] Failed to get object metadata: " + ace.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), ace);
		}

		if (fileExists.booleanValue()) {
			// This is a file.
			pathAttributes.setIsDirectory(false);
			pathAttributes.setExists(true);
			pathAttributes.setIsFile(true);

		} else {
			pathAttributes.setIsFile(false);

			// Check if this is a directory.
			boolean directoryExists = isDirectory(authenticatedToken, fileLocation);
			pathAttributes.setIsDirectory(directoryExists);
			pathAttributes.setExists(directoryExists);
		}

		// Optionally get the file size. We currently don't support getting file size
		// for a directory.
		if (getSize && fileExists.booleanValue() && metadata != null) {
			pathAttributes.setSize(metadata.getContentLength());
		}

		return pathAttributes;
	}

	@Override
	public List<HpcDirectoryScanItem> scanDirectory(Object authenticatedToken, HpcFileLocation directoryLocation)
			throws HpcException {
		List<HpcDirectoryScanItem> directoryScanItems = new ArrayList<>();

		try {
			// List all the files and directories (including nested) under this directory.
			ListObjectsV2Request listObjectsRequest = new ListObjectsV2Request()
					.withBucketName(directoryLocation.getFileContainerId()).withPrefix(directoryLocation.getFileId());
			s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client().listObjectsV2(listObjectsRequest)
					.getObjectSummaries().forEach(s3ObjectSummary -> {
						if (s3ObjectSummary.getSize() > 0) {
							HpcDirectoryScanItem directoryScanItem = new HpcDirectoryScanItem();
							directoryScanItem.setFilePath(s3ObjectSummary.getKey());
							directoryScanItem.setFileName(FilenameUtils.getName(s3ObjectSummary.getKey()));
							directoryScanItem.setLastModified(dateFormat.format(s3ObjectSummary.getLastModified()));
							directoryScanItems.add(directoryScanItem);
						}
					});

			return directoryScanItems;

		} catch (AmazonServiceException ase) {
			throw new HpcException("[S3] Failed to list objects: " + ase.getMessage(), HpcErrorType.DATA_TRANSFER_ERROR,
					s3Connection.getS3Provider(authenticatedToken), ase);

		} catch (AmazonClientException ace) {
			throw new HpcException("[S3] Failed to list objects: " + ace.getMessage(), HpcErrorType.DATA_TRANSFER_ERROR,
					s3Connection.getS3Provider(authenticatedToken), ace);
		}
	}

	@Override
	public String completeMultipartUpload(Object authenticatedToken, HpcFileLocation archiveLocation,
			String multipartUploadId, List<HpcUploadPartETag> uploadPartETags) throws HpcException {
		// Create AWS part ETags from the HPC model.
		List<PartETag> partETags = new ArrayList<PartETag>();
		uploadPartETags.forEach(uploadPartETag -> partETags
				.add(new PartETag(uploadPartETag.getPartNumber(), uploadPartETag.getETag())));

		CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(
				archiveLocation.getFileContainerId(), archiveLocation.getFileId(), multipartUploadId, partETags);
		try {
			return s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client()
					.completeMultipartUpload(completeMultipartUploadRequest).getETag();

		} catch (AmazonClientException e) {
			throw new HpcException(
					"[S3] Failed to complete a multipart upload to " + archiveLocation.getFileContainerId() + ":"
							+ archiveLocation.getFileId() + ". multi-part-upload-id = " + multipartUploadId
							+ ", number-of-parts = " + uploadPartETags.size() + " - " + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e);
		}
	}

	@Override
	public HpcArchiveObjectMetadata getDataObjectMetadata(Object authenticatedToken, HpcFileLocation fileLocation)
			throws HpcException {

		HpcArchiveObjectMetadata objectMetadata = new HpcArchiveObjectMetadata();
		// Get metadata for the data-object in the S3 archive.
		try {
			ObjectMetadata s3Metadata = s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client()
					.getObjectMetadata(fileLocation.getFileContainerId(), fileLocation.getFileId());
			HpcMetadataEntry entry = new HpcMetadataEntry();
			entry.setAttribute("storage_class");
			// x-amz-storage-class is not returned for standard S3 object
			logger.debug("Storage class " + s3Metadata.getStorageClass());
			if (s3Metadata.getStorageClass() != null)
				objectMetadata.setDeepArchiveStatus(HpcDeepArchiveStatus.fromValue(s3Metadata.getStorageClass()));

			// Check the restoration status of the object.
			Boolean restoreFlag = s3Metadata.getOngoingRestore();
			if (s3Metadata.getOngoingRestore() == null) {
				// the x-amz-restore header is not present on the response from the service (eg.
				// no restore request has been received).
				// Failed.
				objectMetadata.setRestorationStatus("not in progress");
			} else if (s3Metadata.getOngoingRestore() != null && s3Metadata.getOngoingRestore()) {
				// the x-amz-restore header is present and has a value of true (eg. a restore
				// operation was received and is currently ongoing).
				// Ongoing
				objectMetadata.setRestorationStatus("in progress");
			} else if (s3Metadata.getOngoingRestore() != null && !s3Metadata.getOngoingRestore()
					&& s3Metadata.getRestoreExpirationTime() != null) {
				// the x-amz-restore header is present and has a value of false (eg the object
				// has been restored and can currently be read from S3).
				// Completed. Success.
				objectMetadata.setRestorationStatus("success");
			}

			if (restoreFlag != null)
				logger.info("Restoration status: {}",
						restoreFlag ? "in progress" : "not in progress (finished or failed)");

			objectMetadata.setChecksum(s3Metadata.getETag());

		} catch (AmazonClientException ace) {
			throw new HpcException("[S3] Failed to get object metadata: " + ace.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, ace);
		}

		return objectMetadata;
	}

	@SuppressWarnings("deprecation")
	@Override
	public synchronized void setTieringPolicy(Object authenticatedToken, HpcFileLocation archiveLocation, String prefix,
			String tieringBucket, String tieringProtocol) throws HpcException {
		// Create a rule to archive objects with the prefix to Glacier
		// immediately.
		BucketLifecycleConfiguration.Rule newRule = new BucketLifecycleConfiguration.Rule().withId(prefix)
				.addTransition(new Transition().withDays(0).withStorageClass(StorageClass.Glacier))
				.withFilter(new LifecycleFilter(new LifecyclePrefixPredicate(prefix)))
				.withStatus(BucketLifecycleConfiguration.ENABLED);

		try {

			AmazonS3 s3Client = s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client();

			// Retrieve the configuration.
			BucketLifecycleConfiguration configuration = s3Client
					.getBucketLifecycleConfiguration(archiveLocation.getFileContainerId());

			List<Rule> rules = new ArrayList<Rule>();
			rules.add(newRule);

			if (configuration != null) {
				for (Rule rule : configuration.getRules()) {
					// Rules existing in Cloudian is retrieved with the prefix
					// set to the same value as filter.
					// Removing since it fails if this value is provided.
					rule.setPrefix(null);
					rules.add(rule);
				}
			} else
				configuration = new BucketLifecycleConfiguration();

			// Add a new rule
			configuration.setRules(rules);

			// Save the configuration.
			SetBucketLifecycleConfigurationRequest request = new SetBucketLifecycleConfigurationRequest(
					archiveLocation.getFileContainerId(), configuration);

			// Add Cloudian custom tiering header, no impact to AWS S3 requests
			String customHeader = tieringProtocol + "|EndPoint:"
					+ URLEncoder.encode(tieringEndpoint, StandardCharsets.UTF_8.toString()) + ",TieringBucket:"
					+ tieringBucket;
			String encodedCustomHeader = URLEncoder.encode(customHeader, StandardCharsets.UTF_8.toString());
			request.putCustomRequestHeader(CLOUDIAN_TIERING_INFO_HEADER, encodedCustomHeader);

			s3Client.setBucketLifecycleConfiguration(request);

		} catch (UnsupportedEncodingException e) {
			throw new HpcException(
					"[S3] Failed to add a new rule to life cycle policy on bucket "
							+ archiveLocation.getFileContainerId() + ":" + prefix + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e);
		} catch (AmazonServiceException e) {
			throw new HpcException(
					"[S3] Failed to add a new rule to life cycle policy on bucket "
							+ archiveLocation.getFileContainerId() + ":" + prefix + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e);
		} catch (AmazonClientException e) {
			throw new HpcException(
					"[S3] Failed to add a new rule to life cycle policy on bucket "
							+ archiveLocation.getFileContainerId() + ":" + prefix + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e);
		}
	}

	@Override
	public void restoreDataObject(Object authenticatedToken, HpcFileLocation archiveLocation) throws HpcException {

		try {
			AmazonS3 s3Client = s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client();

			// Create and submit a request to restore an object from Glacier for configured
			// number of days.
			RestoreObjectRequest requestRestore = new RestoreObjectRequest(archiveLocation.getFileContainerId(),
					archiveLocation.getFileId(), restoreNumDays);
			s3Client.restoreObjectV2(requestRestore);

		} catch (AmazonServiceException e) {
			throw new HpcException(
					"[S3] Failed to restore data object " + archiveLocation.getFileContainerId() + ":"
							+ archiveLocation.getFileId() + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e);
		} catch (AmazonClientException e) {
			throw new HpcException(
					"[S3] Failed to restore data object " + archiveLocation.getFileContainerId() + ":"
							+ archiveLocation.getFileId() + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e);
		}
	}

	@Override
	public boolean existsTieringPolicy(Object authenticatedToken, HpcFileLocation archiveLocation) throws HpcException {

		try {
			AmazonS3 s3Client = s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client();

			// Retrieve the configuration.
			BucketLifecycleConfiguration configuration = s3Client
					.getBucketLifecycleConfiguration(archiveLocation.getFileContainerId());

			if (configuration != null) {
				for (Rule rule : configuration.getRules()) {
					// Look through filter prefix applied to lifecycle policy
					boolean hasTransition = false;

					if (rule.getTransitions() != null) {
						for (Transition transition : rule.getTransitions()) {
							if (transition.getStorageClassAsString() != null
									&& !transition.getStorageClassAsString().isEmpty())
								hasTransition = true;
						}
					}

					if (hasTransition && rule.getFilter() != null && rule.getFilter().getPredicate() != null) {
						LifecycleFilterPredicate predicate = rule.getFilter().getPredicate();
						if (predicate instanceof LifecyclePrefixPredicate) {
							LifecyclePrefixPredicate prefixPredicate = (LifecyclePrefixPredicate) predicate;
							if (archiveLocation.getFileId().contains(prefixPredicate.getPrefix()))
								return true;
						}
					} else if (hasTransition) {
						// This is a transition without prefix applies to entire bucket.
						return true;
					}
				}
			}
		} catch (AmazonServiceException e) {
			throw new HpcException(
					"[S3] Failed to retrieve life cycle policy on bucket " + archiveLocation.getFileContainerId()
							+ e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e);
		}
		return false;
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Upload a data object file.
	 *
	 * @param authenticatedToken         An authenticated token.
	 * @param sourceFile                 The file to upload.
	 * @param archiveDestinationLocation The archive destination location.
	 * @param progressListener           (Optional) a progress listener for async
	 *                                   notification on transfer completion.
	 * @param archiveType                The archive type.
	 * @param metadataEntries            The metadata entries to attach to the
	 *                                   data-object in S3 archive.
	 * @param storageClass               (Optional) The storage class to upload the
	 *                                   file.
	 * @return A data object upload response.
	 * @throws HpcException on data transfer system failure.
	 */
	private HpcDataObjectUploadResponse uploadDataObject(Object authenticatedToken, File sourceFile,
			HpcFileLocation archiveDestinationLocation, HpcDataTransferProgressListener progressListener,
			HpcArchiveType archiveType, List<HpcMetadataEntry> metadataEntries, String storageClass)
			throws HpcException {
		// Create a S3 upload request.
		PutObjectRequest request = new PutObjectRequest(archiveDestinationLocation.getFileContainerId(),
				archiveDestinationLocation.getFileId(), sourceFile).withMetadata(toS3Metadata(metadataEntries))
						.withStorageClass(storageClass);

		// Upload the data.
		Upload s3Upload = null;
		Calendar dataTransferStarted = Calendar.getInstance();
		Calendar dataTransferCompleted = null;
		try {
			s3Upload = s3Connection.getTransferManager(authenticatedToken).upload(request);
			if (progressListener == null) {
				// Upload synchronously.
				s3Upload.waitForUploadResult();
				dataTransferCompleted = Calendar.getInstance();
			} else {
				// Upload asynchronously.
				s3Upload.addProgressListener(new HpcS3ProgressListener(progressListener,
						"upload to" + archiveDestinationLocation.getFileContainerId() + ":"
								+ archiveDestinationLocation.getFileId()));
			}

		} catch (AmazonClientException ace) {
			throw new HpcException("[S3] Failed to upload file.", HpcErrorType.DATA_TRANSFER_ERROR,
					s3Connection.getS3Provider(authenticatedToken), ace);

		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}

		// Upload completed. Create and populate the response object.
		HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
		uploadResponse.setArchiveLocation(archiveDestinationLocation);
		uploadResponse.setDataTransferType(HpcDataTransferType.S_3);
		uploadResponse.setDataTransferStarted(dataTransferStarted);
		uploadResponse.setDataTransferCompleted(dataTransferCompleted);
		uploadResponse.setDataTransferRequestId(String.valueOf(s3Upload.hashCode()));
		uploadResponse.setSourceSize(sourceFile.length());
		uploadResponse.setDataTransferMethod(HpcDataTransferUploadMethod.SYNC);
		if (archiveType.equals(HpcArchiveType.ARCHIVE)) {
			uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.ARCHIVED);
		} else {
			uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.IN_TEMPORARY_ARCHIVE);
		}

		return uploadResponse;
	}

	/**
	 * Upload a data object file from AWS, 3rd Party S3 Provider or Google Drive
	 * source.
	 *
	 * @param authenticatedToken             An authenticated token.
	 * @param s3UploadSource                 The S3 upload source (AWS or 3rd party
	 *                                       provider)
	 * @param googleDriveUploadSource        The Google Drive upload source.
	 * @param googleCloudStorageUploadSource The Google Cloud Storage upload source.
	 * @param archiveDestinationLocation     The archive destination location.
	 * @param baseArchiveDestination         The archive's base destination
	 *                                       location.
	 * @param size                           the size of the file to upload.
	 * @param progressListener               (Optional) a progress listener for
	 *                                       async notification on transfer
	 *                                       completion.
	 * @param metadataEntries                The metadata entries to attach to the
	 *                                       data-object in S3 archive.
	 * @param storageClass                   (Optional) The storage class to upload
	 *                                       the file.
	 * @return A data object upload response.
	 * @throws HpcException on data transfer system failure.
	 */
	private HpcDataObjectUploadResponse uploadDataObject(Object authenticatedToken,
			HpcStreamingUploadSource s3UploadSource, HpcStreamingUploadSource googleDriveUploadSource,
			HpcStreamingUploadSource googleCloudStorageUploadSource, HpcFileLocation archiveDestinationLocation,
			HpcArchive baseArchiveDestination, Long size, HpcDataTransferProgressListener progressListener,
			List<HpcMetadataEntry> metadataEntries, String storageClass) throws HpcException {
		if (progressListener == null) {
			throw new HpcException("[S3] No progress listener provided for a upload from AWS S3 destination",
					HpcErrorType.UNEXPECTED_ERROR);
		}
		if (size == null) {
			throw new HpcException(
					"[S3] File size not provided for an upload from AWS / S3 Provider / Google Drive / Google Cloud Storage",
					HpcErrorType.UNEXPECTED_ERROR);
		}

		HpcDataTransferUploadMethod uploadMethod = null;
		String sourceURL = null;
		HpcFileLocation sourceLocation = null;

		if (s3UploadSource != null) {
			// Upload by streaming from AWS or S3 Provider.
			uploadMethod = HpcDataTransferUploadMethod.S_3;
			sourceLocation = s3UploadSource.getSourceLocation();

			// If not provided, generate a download pre-signed URL for the requested data
			// file from AWS
			// (using the provided S3 account).
			sourceURL = StringUtils.isEmpty(s3UploadSource.getSourceURL())
					? generateDownloadRequestURL(s3Connection.authenticate(s3UploadSource.getAccount()), sourceLocation,
							baseArchiveDestination, S3_STREAM_EXPIRATION)
					: s3UploadSource.getSourceURL();

		} else if (googleDriveUploadSource != null) {
			// Upload by streaming from Google Drive.
			uploadMethod = HpcDataTransferUploadMethod.GOOGLE_DRIVE;
			sourceLocation = googleDriveUploadSource.getSourceLocation();

		} else if (googleCloudStorageUploadSource != null) {
			// Upload by streaming from Google Drive.
			uploadMethod = HpcDataTransferUploadMethod.GOOGLE_CLOUD_STORAGE;
			sourceLocation = googleCloudStorageUploadSource.getSourceLocation();

		} else {
			throw new HpcException("Unexpected upload source", HpcErrorType.UNEXPECTED_ERROR);
		}

		final String url = sourceURL;
		final String sourceDestinationLogMessage = "upload from " + sourceLocation.getFileContainerId() + ":"
				+ sourceLocation.getFileId();
		Calendar dataTransferStarted = Calendar.getInstance();

		CompletableFuture<Void> s3TransferManagerUploadFuture = CompletableFuture.runAsync(() -> {
			try {
				// Open a connection to the input stream of the file to be uploaded.
				InputStream sourceInputStream = null;
				if (googleDriveUploadSource != null) {
					sourceInputStream = googleDriveUploadSource.getSourceInputStream();
				} else if (googleCloudStorageUploadSource != null) {
					sourceInputStream = googleCloudStorageUploadSource.getSourceInputStream();
				} else {
					sourceInputStream = new URL(url).openStream();
				}

				// Create a S3 upload request.
				ObjectMetadata metadata = toS3Metadata(metadataEntries);
				metadata.setContentLength(size);
				PutObjectRequest request = new PutObjectRequest(archiveDestinationLocation.getFileContainerId(),
						archiveDestinationLocation.getFileId(), sourceInputStream, metadata)
								.withStorageClass(storageClass);

				// Set the read limit on the request to avoid AWSreset exceptions.
				request.getRequestClientOptions().setReadLimit(getReadLimit(size));

				// Upload asynchronously. AWS transfer manager will perform the upload in its
				// own managed
				// thread.
				Upload s3Upload = s3Connection.getTransferManager(authenticatedToken).upload(request);

				// Attach a progress listener.
				s3Upload.addProgressListener(new HpcS3ProgressListener(progressListener, sourceDestinationLogMessage));

				logger.info("S3 upload AWS/S3 Provider->{} [{}] started. Source size - {} bytes. Read limit - {}",
						s3Connection.getS3Provider(authenticatedToken), sourceDestinationLogMessage, size,
						request.getRequestClientOptions().getReadLimit());

				// Wait for the result. This ensures the input stream to the URL remains opened
				// and
				// connected until the upload is complete.
				// Note that this wait for AWS transfer manager completion is done in a separate
				// thread
				// (from s3Executor pool), so callers to
				// the API don't wait.
				s3Upload.waitForUploadResult();

			} catch (AmazonClientException | HpcException | IOException e) {
				logger.error("[S3] Failed to upload from AWS S3 destination: " + e.getMessage(), e);
				progressListener.transferFailed(e.getMessage());

			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}, s3Executor);

		// Create and populate the response object.
		HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
		uploadResponse.setArchiveLocation(archiveDestinationLocation);
		uploadResponse.setDataTransferType(HpcDataTransferType.S_3);
		uploadResponse.setDataTransferStarted(dataTransferStarted);
		uploadResponse.setUploadSource(sourceLocation);
		uploadResponse.setDataTransferRequestId(String.valueOf(s3TransferManagerUploadFuture.hashCode()));
		uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.STREAMING_IN_PROGRESS);
		uploadResponse.setSourceURL(sourceURL);
		uploadResponse.setSourceSize(size);
		uploadResponse.setDataTransferMethod(uploadMethod);

		return uploadResponse;
	}

	/**
	 * Generate an upload request URL.
	 *
	 * @param authenticatedToken         An authenticated token.
	 * @param archiveDestinationLocation The archive destination location.
	 * @param uploadRequestURLExpiration The URL expiration (in hours).
	 * @param uploadRequestURLChecksum   An optional user provided checksum value to
	 *                                   attach to the generated url.
	 * @return A data object upload response containing the upload request URL.
	 * @throws HpcException on data transfer system failure.
	 */
	private HpcDataObjectUploadResponse generateUploadRequestURL(Object authenticatedToken,
			HpcFileLocation archiveDestinationLocation, int uploadRequestURLExpiration, String uploadRequestURLChecksum)
			throws HpcException {

		// Calculate the URL expiration date.
		Date expiration = new Date();
		expiration.setTime(expiration.getTime() + 1000 * 60 * 60 * uploadRequestURLExpiration);

		// Create a URL generation request.
		GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
				archiveDestinationLocation.getFileContainerId(), archiveDestinationLocation.getFileId())
						.withMethod(HttpMethod.PUT).withExpiration(expiration);

		// Optionally add a checksum header.
		if (!StringUtils.isEmpty(uploadRequestURLChecksum)) {
			generatePresignedUrlRequest.setContentMd5(uploadRequestURLChecksum);
		}

		// Generate the pre-signed URL.
		URL url = s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client()
				.generatePresignedUrl(generatePresignedUrlRequest);

		// Create and populate the response object.
		HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
		uploadResponse.setArchiveLocation(archiveDestinationLocation);
		uploadResponse.setDataTransferType(HpcDataTransferType.S_3);
		uploadResponse.setDataTransferStarted(Calendar.getInstance());
		uploadResponse.setDataTransferCompleted(null);
		uploadResponse.setDataTransferRequestId(String.valueOf(generatePresignedUrlRequest.hashCode()));
		uploadResponse.setUploadRequestURL(url.toString());
		uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.URL_GENERATED);
		uploadResponse.setDataTransferMethod(HpcDataTransferUploadMethod.URL_SINGLE_PART);

		return uploadResponse;
	}

	/**
	 * Generate upload request multipart URLs.
	 *
	 * @param authenticatedToken         An authenticated token.
	 * @param archiveDestinationLocation The archive destination location.
	 * @param uploadRequestURLExpiration The URL expiration (in hours).
	 * @param uploadParts                How many parts to generate upload URLs for.
	 * @param metadataEntries            The metadata entries to attach to the
	 *                                   data-object in S3 archive.
	 * @param storageClass               (Optional) The storage class to upload the
	 *                                   file.
	 * @return A data object upload response containing the upload request URL.
	 * @throws HpcException on data transfer system failure.
	 */
	private HpcDataObjectUploadResponse generateMultipartUploadRequestURLs(Object authenticatedToken,
			HpcFileLocation archiveDestinationLocation, int uploadRequestURLExpiration, int uploadParts,
			List<HpcMetadataEntry> metadataEntries, String storageClass) throws HpcException {
		// Initiate the multipart upload.
		HpcMultipartUpload multipartUpload = new HpcMultipartUpload();
		InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(
				archiveDestinationLocation.getFileContainerId(), archiveDestinationLocation.getFileId(),
				toS3Metadata(metadataEntries)).withStorageClass(storageClass);

		try {
			multipartUpload.setId(s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client()
					.initiateMultipartUpload(initiateMultipartUploadRequest).getUploadId());
		} catch (AmazonClientException e) {
			throw new HpcException("[S3] Failed to initiate a multipart upload: " + initiateMultipartUploadRequest,
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e);
		}

		// Calculate the URL expiration date.
		Date expiration = new Date();
		expiration.setTime(expiration.getTime() + 1000 * 60 * 60 * uploadRequestURLExpiration);

		// Generate the parts pre-signed upload URLs.
		for (int partNumber = 1; partNumber <= uploadParts; partNumber++) {
			HpcUploadPartURL uploadPartURL = new HpcUploadPartURL();
			uploadPartURL.setPartNumber(partNumber);

			GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
					archiveDestinationLocation.getFileContainerId(), archiveDestinationLocation.getFileId())
							.withMethod(HttpMethod.PUT).withExpiration(expiration);
			generatePresignedUrlRequest.addRequestParameter("partNumber", String.valueOf(partNumber));
			generatePresignedUrlRequest.addRequestParameter("uploadId", multipartUpload.getId());

			try {
				uploadPartURL.setPartUploadRequestURL(s3Connection.getTransferManager(authenticatedToken)
						.getAmazonS3Client().generatePresignedUrl(generatePresignedUrlRequest).toString());

			} catch (AmazonClientException e) {
				throw new HpcException("[S3] Failed to create a pre-signed URL for part: " + partNumber,
						HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e);
			}

			multipartUpload.getParts().add(uploadPartURL);
		}

		// Create and populate the response object.
		HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
		uploadResponse.setArchiveLocation(archiveDestinationLocation);
		uploadResponse.setDataTransferType(HpcDataTransferType.S_3);
		uploadResponse.setDataTransferStarted(Calendar.getInstance());
		uploadResponse.setDataTransferCompleted(null);
		uploadResponse.setDataTransferRequestId(String.valueOf(initiateMultipartUploadRequest.hashCode()));
		uploadResponse.setMultipartUpload(multipartUpload);
		uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.URL_GENERATED);
		uploadResponse.setDataTransferMethod(HpcDataTransferUploadMethod.URL_MULTI_PART);

		return uploadResponse;
	}

	/**
	 * Convert HPC metadata entries into S3 metadata object
	 *
	 * @param metadataEntries The metadata entries to convert
	 * @return A S3 metadata object
	 */
	private ObjectMetadata toS3Metadata(List<HpcMetadataEntry> metadataEntries) {
		ObjectMetadata objectMetadata = new ObjectMetadata();
		if (metadataEntries != null) {
			for (HpcMetadataEntry metadataEntry : metadataEntries) {
				objectMetadata.addUserMetadata(metadataEntry.getAttribute(), metadataEntry.getValue());
			}
		}

		return objectMetadata;
	}

	/**
	 * Download a data object to a local file.
	 *
	 * @param authenticatedToken  An authenticated token.
	 * @param archiveLocation     The data object archive location.
	 * @param destinationLocation The local file destination.
	 * @param progressListener    (Optional) a progress listener for async
	 *                            notification on transfer completion.
	 * @return A data transfer request Id.
	 * @throws HpcException on data transfer system failure.
	 */
	private String downloadDataObject(Object authenticatedToken, HpcFileLocation archiveLocation,
			File destinationLocation, HpcDataTransferProgressListener progressListener) throws HpcException {
		// Create a S3 download request.
		
		GetObjectRequest request = new GetObjectRequest(archiveLocation.getFileContainerId(),
				archiveLocation.getFileId());

		
		// Download the file via S3.
		Download s3Download = null;
		try {
			s3Download = s3Connection.getTransferManager(authenticatedToken).download(request, destinationLocation);
			if (progressListener == null) {
				// Download synchronously.
				s3Download.waitForCompletion();
			} else {
				// Download asynchronously.
				s3Download.addProgressListener(new HpcS3ProgressListener(progressListener,
						"download from " + archiveLocation.getFileContainerId() + ":" + archiveLocation.getFileId()));
			}

		} catch (AmazonClientException ace) {
			throw new HpcException("[S3] Failed to download file: [" + ace.getMessage() + "]",
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), ace);

		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			
		} catch(Exception ge) {
		}

		return String.valueOf(s3Download.hashCode());
	}

	/**
	 * Download a data object to S3 destination (either AWS or 3rd Party Provider).
	 *
	 * @param authenticatedToken     An authenticated token.
	 * @param archiveLocation        The data object archive location.
	 * @param archiveLocationURL     (Optional) The data object archive location
	 *                               URL.
	 * @param baseArchiveDestination The archive's base destination location.
	 * @param s3Destination          The S3 destination.
	 * @param progressListener       (Optional) a progress listener for async
	 *                               notification on transfer completion.
	 * @param fileSize               The size of the file to download.
	 * @return A data transfer request Id.
	 * @throws HpcException on data transfer failure.
	 */
	private String downloadDataObject(Object authenticatedToken, HpcFileLocation archiveLocation,
			String archiveLocationURL, HpcArchive baseArchiveDestination, HpcS3DownloadDestination s3Destination,
			HpcDataTransferProgressListener progressListener, long fileSize) throws HpcException {
		// Authenticate the S3 account.
		Object s3AccountAuthenticatedToken = s3Connection.authenticate(s3Destination.getAccount());

		// Confirm the S3 bucket is accessible.
		boolean s3BucketAccessible = true;
		try {
			s3BucketAccessible = getPathAttributes(s3AccountAuthenticatedToken, s3Destination.getDestinationLocation(),
					false).getIsAccessible();
		} catch (HpcException e) {
			s3BucketAccessible = false;
			logger.error("Failed to get S3 path attributes: " + e.getMessage(), e);
		}
		if (!s3BucketAccessible) {
			throw new HpcException(
					"Failed to access AWS S3 bucket: " + s3Destination.getDestinationLocation().getFileContainerId(),
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		String sourceURL = null;
		long size = fileSize;
		if (StringUtils.isEmpty(archiveLocationURL)) {
			// Downloading from S3 archive -> S3 destination.
			sourceURL = generateDownloadRequestURL(authenticatedToken, archiveLocation, baseArchiveDestination,
					S3_STREAM_EXPIRATION);
			if (size == 0) {
				size = getPathAttributes(authenticatedToken, archiveLocation, true).getSize();
			}
		} else {
			// Downloading from POSIX archive -> S3 destination.
			sourceURL = archiveLocationURL;
			if (size == 0) {
				try {
					size = Files.size(Paths.get(URI.create(archiveLocationURL)));
				} catch (IOException e) {
					throw new HpcException(
							"Failed to determine data object size in a POSIX archive: " + archiveLocationURL,
							HpcErrorType.UNEXPECTED_ERROR);
				}
			}
		}

		// Use AWS transfer manager to download the file.
		return downloadDataObject(s3AccountAuthenticatedToken, sourceURL, s3Destination.getDestinationLocation(), size,
				progressListener);
	}

	/**
	 * Download a data object to a user's AWS / S3 Provider by using AWS transfer
	 * Manager.
	 *
	 * @param s3AccountAuthenticatedToken An authenticated token to the user's AWS
	 *                                    S3 account.
	 * @param sourceURL                   The download source URL.
	 * @param destinationLocation         The destination location.
	 * @param fileSize                    The size of the file to download.
	 * @param progressListener            A progress listener for async notification
	 *                                    on transfer completion.
	 * @return A data transfer request Id.
	 * @throws HpcException on data transfer failure.
	 */
	private String downloadDataObject(Object s3AccountAuthenticatedToken, String sourceURL,
			HpcFileLocation destinationLocation, long fileSize, HpcDataTransferProgressListener progressListener)
			throws HpcException {
		if (progressListener == null) {
			throw new HpcException("[S3] No progress listener provided for a download to AWS S3 destination",
					HpcErrorType.UNEXPECTED_ERROR);
		}

		CompletableFuture<Void> s3TransferManagerDownloadFuture = CompletableFuture.runAsync(() -> {
			try {
				// Create source URL and open a connection to it.
				InputStream sourceInputStream = new URL(sourceURL).openStream();

				// Create a S3 upload request.
				ObjectMetadata metadata = new ObjectMetadata();
				metadata.setContentLength(fileSize);
				PutObjectRequest request = new PutObjectRequest(destinationLocation.getFileContainerId(),
						destinationLocation.getFileId(), sourceInputStream, metadata);

				// Set the read limit on the request to avoid AWSreset exceptions.
				request.getRequestClientOptions().setReadLimit(getReadLimit(fileSize));

				// Upload asynchronously. AWS transfer manager will perform the upload in its
				// own managed
				// thread.
				Upload s3Upload = s3Connection.getTransferManager(s3AccountAuthenticatedToken).upload(request);

				// Attach a progress listener.
				String sourceDestinationLogMessage = "download to " + destinationLocation.getFileContainerId() + ":"
						+ destinationLocation.getFileId();
				s3Upload.addProgressListener(new HpcS3ProgressListener(progressListener, sourceDestinationLogMessage));

				logger.info(
						"S3 download Archive->AWS/S3 Provider [{}] started. Source size - {} bytes. Read limit - {}",
						sourceDestinationLogMessage, fileSize, request.getRequestClientOptions().getReadLimit());

				// Wait for the result. This ensures the input stream to the URL remains opened
				// and
				// connected until the download is complete.
				// Note that this wait for AWS transfer manager completion is done in a separate
				// thread
				// (from s3Executor pool), so callers to
				// the API don't wait.
				s3Upload.waitForUploadResult();

			} catch (AmazonClientException | HpcException | IOException e) {
				logger.error("[S3] Failed to downloadload to AWS / S3 Provider destination: " + e.getMessage(), e);
				progressListener.transferFailed(e.getMessage());

			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}, s3Executor);

		return String.valueOf(s3TransferManagerDownloadFuture.hashCode());
	}

	/**
	 * Get buffer read limit for a given file size
	 *
	 * @param fileSize The file size.
	 * @return read limit
	 */
	private int getReadLimit(long fileSize) {
		try {
			return Math.toIntExact(fileSize + 1);

		} catch (ArithmeticException e) {
			return Integer.MAX_VALUE;
		}
	}

	/**
	 * Check if a path is a directory on S3 bucket.
	 *
	 * @param authenticatedToken An authenticated token to S3.
	 * @param fileLocation       the file location.
	 * @return true if it's a directory, or false otherwise.
	 * @throws HpcException on failure invoke AWS S3 api.
	 */
	private boolean isDirectory(Object authenticatedToken, HpcFileLocation fileLocation) throws HpcException {
		try {
			try {
				// Check if this is a directory. Use V2 listObjects API.
				ListObjectsV2Request listObjectsRequest = new ListObjectsV2Request()
						.withBucketName(fileLocation.getFileContainerId()).withPrefix(fileLocation.getFileId());
				return s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client()
						.listObjectsV2(listObjectsRequest).getKeyCount() > 0;

			} catch (AmazonServiceException ase) {
				if (ase.getStatusCode() == 400) {
					// V2 not supported. Use V1 listObjects API.
					ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
							.withBucketName(fileLocation.getFileContainerId()).withPrefix(fileLocation.getFileId());
					return !s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client()
							.listObjects(listObjectsRequest).getObjectSummaries().isEmpty();

				} else {
					throw ase;
				}

			}

		} catch (AmazonClientException ace) {
			throw new HpcException("[S3] Failed to list object: " + ace.getMessage(), HpcErrorType.DATA_TRANSFER_ERROR,
					s3Connection.getS3Provider(authenticatedToken), ace);
		}
	}
}
