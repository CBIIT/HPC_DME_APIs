package gov.nih.nci.hpc.integration.s3.v2.impl;

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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

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
import gov.nih.nci.hpc.domain.datatransfer.HpcSetArchiveObjectMetadataResponse;
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
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ExpirationStatus;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GlacierJobParameters;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.RestoreObjectRequest;
import software.amazon.awssdk.services.s3.model.RestoreRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.Tier;
import software.amazon.awssdk.services.s3.model.Transition;
import software.amazon.awssdk.services.s3.model.TransitionStorageClass;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;
import software.amazon.awssdk.transfer.s3.model.CompletedCopy;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.CopyRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

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

	// The S3 executor.
	@Autowired
	@Qualifier("hpcS3Executor")
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
		return s3Connection.authenticate(dataTransferAccount, urlOrRegion);
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
				baseArchiveDestination.getType(), this, authenticatedToken);

		if (uploadRequest.getGenerateUploadRequestURL()) {
			int uploadParts = Optional.ofNullable(uploadRequest.getUploadParts()).orElse(1);
			if (uploadParts == 1) {
				// Generate an upload request URL for the caller to use to upload directly.
				return generateUploadRequestURL(authenticatedToken, archiveDestinationLocation,
						uploadRequestURLExpiration, metadataEntries, uploadRequest.getUploadRequestURLChecksum(),
						storageClass, Optional.ofNullable(uploadRequest.getUploadCompletion()).orElse(false));
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
		if (downloadRequest.getArchiveLocation() == null) {
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
		if (archiveSourceLocation == null) {
			throw new HpcException("Null archive location", HpcErrorType.UNEXPECTED_ERROR);
		}

		try {
			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
					.bucket(archiveSourceLocation.getFileContainerId()).key(archiveSourceLocation.getFileId()).build();

			GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
					.signatureDuration(Duration.ofHours(downloadRequestURLExpiration))
					.getObjectRequest(getObjectRequest).build();

			PresignedGetObjectRequest presignedGetObjectRequest = s3Connection.getPresigner(authenticatedToken)
					.presignGetObject(getObjectPresignRequest);
			return presignedGetObjectRequest.url().toString();

		} catch (SdkException e) {
			throw new HpcException(
					"[S3] Failed to generate presigned download URL" + archiveSourceLocation.getFileContainerId() + ":"
							+ archiveSourceLocation.getFileId() + " - " + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, e);
		}
	}

	@Override
	public HpcSetArchiveObjectMetadataResponse setDataObjectMetadata(Object authenticatedToken,
			HpcFileLocation fileLocation, HpcArchive baseArchiveDestination, List<HpcMetadataEntry> metadataEntries,
			String storageClass) throws HpcException {
		HpcSetArchiveObjectMetadataResponse response = new HpcSetArchiveObjectMetadataResponse();

		// Check if the metadata was already set on the data-object in the S3 archive.
		try {
			HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(fileLocation.getFileContainerId())
					.key(fileLocation.getFileId()).build();
			HeadObjectResponse headObjectResponse = s3Connection.getClient(authenticatedToken)
					.headObject(headObjectRequest).join();

			Map<String, String> s3Metadata = headObjectResponse.metadata();
			boolean metadataAlreadySet = true;
			for (HpcMetadataEntry metadataEntry : metadataEntries) {
				if (!s3Metadata.containsKey(metadataEntry.getAttribute())) {
					metadataAlreadySet = false;
					break;
				}
			}

			if (metadataAlreadySet) {
				logger.info("System metadata in S3 archive already set for [{}]. No need to copy-object in archive",
						fileLocation.getFileId());
				response.setChecksum(headObjectResponse.eTag().replaceAll("\"", ""));
				response.setMetadataAdded(false);
				return response;
			}

		} catch (CompletionException e) {
			throw new HpcException(
					"[S3] Failed to get object metadata: " + fileLocation.getFileContainerId() + ":"
							+ fileLocation.getFileId() + " - " + e.getCause().getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, e.getCause());
		}

		// We set S3 metadata by copying the data-object to itself w/ attached metadata.
		CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
				.sourceBucket(fileLocation.getFileContainerId()).sourceKey(fileLocation.getFileId())
				.destinationBucket(fileLocation.getFileContainerId()).destinationKey(fileLocation.getFileId())
				.storageClass(storageClass).metadata(toS3Metadata(metadataEntries))
				.metadataDirective(MetadataDirective.REPLACE).build();

		CopyRequest copyRequest = CopyRequest.builder().copyObjectRequest(copyObjectRequest).build();

		try {
			Copy copy = s3Connection.getTransferManager(authenticatedToken).copy(copyRequest);

			CompletedCopy completedCopy = copy.completionFuture().join();
			response.setChecksum(completedCopy.response().copyObjectResult().eTag().replaceAll("\"", ""));
			response.setMetadataAdded(true);
			return response;

		} catch (CompletionException e) {
			throw new HpcException("[S3] Failed to copy file: " + copyRequest, HpcErrorType.DATA_TRANSFER_ERROR,
					s3Connection.getS3Provider(authenticatedToken), e.getCause());
		}

	}

	@Override
	public void deleteDataObject(Object authenticatedToken, HpcFileLocation fileLocation,
			HpcArchive baseArchiveDestination) throws HpcException {

		// Create a S3 delete request.
		DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket(fileLocation.getFileContainerId())
				.key(fileLocation.getFileId()).build();

		try {
			s3Connection.getClient(authenticatedToken).deleteObject(deleteRequest).join();

		} catch (CompletionException e) {
			throw new HpcException("[S3] Failed to delete file: " + deleteRequest, HpcErrorType.DATA_TRANSFER_ERROR,
					s3Connection.getS3Provider(authenticatedToken), e.getCause());
		}
	}

	@Override
	public HpcPathAttributes getPathAttributes(Object authenticatedToken, HpcFileLocation fileLocation, boolean getSize)
			throws HpcException {

		HpcPathAttributes pathAttributes = new HpcPathAttributes();
		HeadObjectResponse headObjectResponse = null;

		// Look for the file.
		try {
			pathAttributes.setIsAccessible(true);
			HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(fileLocation.getFileContainerId())
					.key(fileLocation.getFileId()).build();
			headObjectResponse = s3Connection.getClient(authenticatedToken).headObject(headObjectRequest)
					.exceptionally(e -> {
						Throwable cause = e.getCause();
						if (cause instanceof SdkServiceException) {
							if (((SdkServiceException) cause).statusCode() == 403) {
								pathAttributes.setIsAccessible(false);
							}
						} else if (!(cause instanceof NoSuchKeyException)) {
							logger.error("[S3] Failed to get head object request: " + cause.getClass().toString()
									+ " * " + cause.getMessage(), cause);
						}
						return null;
					}).join();

		} catch (CompletionException e) {
			throw new HpcException("[S3] Failed to get head object request: " + e.getCause().getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e.getCause());

		}

		if (!pathAttributes.getIsAccessible()) {
			return pathAttributes;
		}
		if (headObjectResponse != null) {
			// This is a file.
			pathAttributes.setIsFile(true);
			pathAttributes.setIsDirectory(false);
			pathAttributes.setExists(true);

		} else {
			pathAttributes.setIsFile(false);

			// Check if this is a directory.
			boolean directoryExists = isDirectory(authenticatedToken, fileLocation);
			pathAttributes.setIsDirectory(directoryExists);
			pathAttributes.setExists(directoryExists);
		}

		// Optionally get the file size. We currently don't support getting file size
		// for a directory.
		if (getSize && headObjectResponse != null) {
			pathAttributes.setSize(headObjectResponse.contentLength());
		}

		return pathAttributes;
	}

	@Override
	public List<HpcDirectoryScanItem> scanDirectory(Object authenticatedToken, HpcFileLocation directoryLocation)
			throws HpcException {
		List<HpcDirectoryScanItem> directoryScanItems = new ArrayList<>();

		try {
			ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
					.bucket(directoryLocation.getFileContainerId()).prefix(directoryLocation.getFileId()).build();

			ListObjectsV2Response listObjectsResponse = s3Connection.getClient(authenticatedToken)
					.listObjectsV2(listObjectsRequest).join();

			List<S3Object> s3Objects = listObjectsResponse.contents();

			// Paginate through all results.
			while (listObjectsResponse.isTruncated()) {
				String continuationToken = listObjectsResponse.nextContinuationToken();

				listObjectsRequest = listObjectsRequest.toBuilder().continuationToken(continuationToken).build();
				listObjectsResponse = s3Connection.getClient(authenticatedToken).listObjectsV2(listObjectsRequest)
						.join();

				if (continuationToken.equals(listObjectsResponse.nextContinuationToken())) {
					// Pagination over list objects is not working w/ Cleversafe storage, we keep
					// getting the same set of results. This code is to protect against infinite
					// loop.
					break;
				}

				s3Objects.addAll(listObjectsResponse.contents());
			}

			s3Objects.forEach(s3Object -> {
				if (s3Object.size() > 0) {
					HpcDirectoryScanItem directoryScanItem = new HpcDirectoryScanItem();
					directoryScanItem.setFilePath(s3Object.key());
					directoryScanItem.setFileName(FilenameUtils.getName(s3Object.key()));
					directoryScanItem.setLastModified(dateFormat.format(Date.from(s3Object.lastModified())));
					directoryScanItems.add(directoryScanItem);
				}
			});

			return directoryScanItems;

		} catch (CompletionException e) {
			throw new HpcException("[S3] Failed to list objects: " + e.getCause().getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e.getCause());
		}
	}

	@Override
	public String completeMultipartUpload(Object authenticatedToken, HpcFileLocation archiveLocation,
			String multipartUploadId, List<HpcUploadPartETag> uploadPartETags) throws HpcException {

		// Create AWS part ETags from the HPC model.
		List<CompletedPart> parts = new ArrayList<>();
		uploadPartETags.forEach(uploadPartETag -> parts.add(CompletedPart.builder()
				.partNumber(uploadPartETag.getPartNumber()).eTag(uploadPartETag.getETag()).build()));
		CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder().parts(parts).build();

		CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
				.bucket(archiveLocation.getFileContainerId()).key(archiveLocation.getFileId())
				.uploadId(multipartUploadId).multipartUpload(completedMultipartUpload).build();

		try {
			return s3Connection.getClient(authenticatedToken).completeMultipartUpload(completeMultipartUploadRequest)
					.join().eTag();

		} catch (CompletionException e) {
			throw new HpcException(
					"[S3] Failed to complete a multipart upload to " + archiveLocation.getFileContainerId() + ":"
							+ archiveLocation.getFileId() + ". multi-part-upload-id = " + multipartUploadId
							+ ", number-of-parts = " + uploadPartETags.size() + " - " + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e.getCause());
		}
	}

	@Override
	public void restoreDataObject(Object authenticatedToken, HpcFileLocation archiveLocation) throws HpcException {
		try {
			// Create and submit a request to restore an object from Glacier.
			RestoreRequest restoreRequest = RestoreRequest.builder().days(restoreNumDays)
					.glacierJobParameters(GlacierJobParameters.builder().tier(Tier.STANDARD).build()).build();

			RestoreObjectRequest restoreObjectRequest = RestoreObjectRequest.builder()
					.bucket(archiveLocation.getFileContainerId()).key(archiveLocation.getFileId())
					.restoreRequest(restoreRequest).build();
			s3Connection.getClient(authenticatedToken).restoreObject(restoreObjectRequest).join();

		} catch (CompletionException e) {
			throw new HpcException(
					"[S3] Failed to restore data object" + archiveLocation.getFileContainerId() + ":"
							+ archiveLocation.getFileId() + " - " + e.getCause().getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e.getCause());
		}
	}

	@Override
	public boolean existsTieringPolicy(Object authenticatedToken, HpcFileLocation archiveLocation) throws HpcException {
		try {
			// Retrieve the configuration.
			GetBucketLifecycleConfigurationResponse bucketLifeCycleConfigurationResponse = s3Connection
					.getClient(authenticatedToken)
					.getBucketLifecycleConfiguration(builder -> builder.bucket(archiveLocation.getFileContainerId()))
					.join();

			if (bucketLifeCycleConfigurationResponse != null) {
				for (LifecycleRule rule : bucketLifeCycleConfigurationResponse.rules()) {
					// Look through filter prefix applied to lifecycle policy
					boolean hasTransition = false;

					if (rule.hasTransitions()) {
						for (Transition transition : rule.transitions()) {
							if (!StringUtils.isEmpty(transition.storageClassAsString())) {
								hasTransition = true;
							}
						}
					}

					if (hasTransition && rule.filter() != null && rule.filter().prefix() != null) {
						if (archiveLocation.getFileId().contains(rule.filter().prefix())) {
							return true;
						}
					} else if (hasTransition) {
						// This is a transition without prefix applies to entire bucket.
						return true;
					}
				}
			}

			return false;

		} catch (CompletionException e) {
			throw new HpcException(
					"[S3] Failed to retrieve life cycle policy on bucket: " + archiveLocation.getFileContainerId()
							+ "- " + e.getCause().getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e.getCause());
		}
	}

	@Override
	public HpcArchiveObjectMetadata getDataObjectMetadata(Object authenticatedToken, HpcFileLocation fileLocation)
			throws HpcException {
		HpcArchiveObjectMetadata objectMetadata = new HpcArchiveObjectMetadata();
		String s3ObjectName = fileLocation.getFileContainerId() + ":" + fileLocation.getFileId();

		// Get metadata for the data-object in the S3 archive.
		try {
			HeadObjectRequest headObjectRequest = HeadObjectRequest.builder().bucket(fileLocation.getFileContainerId())
					.key(fileLocation.getFileId()).build();
			HeadObjectResponse headObjectResponse = s3Connection.getClient(authenticatedToken)
					.headObject(headObjectRequest).join();

			// x-amz-storage-class is not returned for standard S3 object
			logger.info("[S3] Storage class [{}] - {}", s3ObjectName, headObjectResponse.storageClass());

			if (headObjectResponse.storageClass() != null) {
				objectMetadata.setDeepArchiveStatus(
						HpcDeepArchiveStatus.fromValue(headObjectResponse.storageClassAsString()));
				logger.info("[S3] Deep Archive Status [{}] - {}", s3ObjectName, objectMetadata.getDeepArchiveStatus());
			}

			// Check the restoration status of the object.
			String restoreHeader = headObjectResponse.restore();
			logger.info("[S3] Restore Header [{}] - {}", s3ObjectName, restoreHeader);

			if (StringUtils.isEmpty(restoreHeader)) {
				// the x-amz-restore header is not present on the response from the service
				// (e.g. no restore request has been received).
				objectMetadata.setRestorationStatus("not in progress");

			} else if (restoreHeader.contains("ongoing-request=\"true\"")) {
				// the x-amz-restore header is present and has a value of true (e.g. a restore
				// request was received and is currently ongoing).
				objectMetadata.setRestorationStatus("in progress");

			} else if (restoreHeader.contains("ongoing-request=\"false\"")) {
				// the x-amz-restore header is present and has a value of false (e.g the object
				// has been restored and can currently be read from S3).
				objectMetadata.setRestorationStatus("success");
			}
			logger.info("[S3] Restoration Status [{}] - {}", s3ObjectName, objectMetadata.getRestorationStatus());

			objectMetadata.setChecksum(headObjectResponse.eTag().replaceAll("\"", ""));
			logger.info("[S3] Checksum [{}] - {}", s3ObjectName, objectMetadata.getChecksum());

		} catch (CompletionException e) {
			throw new HpcException(
					"[S3] Failed to get object metadata [" + s3ObjectName + "] - " + e.getCause().getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e.getCause());
		}

		return objectMetadata;
	}

	@SuppressWarnings("deprecation")
	@Override
	public synchronized void setTieringPolicy(Object authenticatedToken, HpcFileLocation archiveLocation, String prefix,
			String tieringBucket, String tieringProtocol) throws HpcException {

		try {
			// Create a list of life cycle rules
			List<LifecycleRule> lifeCycleRules = new ArrayList<>();

			// Add the new rule.
			Transition transition = Transition.builder().days(0).storageClass(TransitionStorageClass.GLACIER).build();
			lifeCycleRules.add(LifecycleRule.builder().id(prefix).transitions(transition)
					.filter(builder -> builder.prefix(prefix)).status(ExpirationStatus.ENABLED).build());

			// Retrieve the configuration
			GetBucketLifecycleConfigurationResponse bucketLifeCycleConfigurationResponse = s3Connection
					.getClient(authenticatedToken)
					.getBucketLifecycleConfiguration(builder -> builder.bucket(archiveLocation.getFileContainerId()))
					.join();

			// Add the existing rules to the list.
			if (bucketLifeCycleConfigurationResponse != null) {
				for (LifecycleRule lifeCycleRule : bucketLifeCycleConfigurationResponse.rules()) {
					// Rules existing in Cloudian is retrieved with the prefix
					// set to the same value as filter.
					// Removing since it fails if this value is provided.
					lifeCycleRules.add(lifeCycleRule.toBuilder().prefix(null).build());
				}
			}

			// Add Cloudian custom tiering header, no impact to AWS S3 requests.
			String customHeader = tieringProtocol + "|EndPoint:"
					+ URLEncoder.encode(tieringEndpoint, StandardCharsets.UTF_8.toString()) + ",TieringBucket:"
					+ tieringBucket;
			String encodedCustomHeader = URLEncoder.encode(customHeader, StandardCharsets.UTF_8.toString());

			BucketLifecycleConfiguration lifeCycleConfiguration = BucketLifecycleConfiguration.builder()
					.rules(lifeCycleRules).build();
			AwsRequestOverrideConfiguration requestOverrideConfiguration = AwsRequestOverrideConfiguration.builder()
					.putHeader(CLOUDIAN_TIERING_INFO_HEADER, encodedCustomHeader).build();

			s3Connection.getClient(authenticatedToken)
					.putBucketLifecycleConfiguration(builder -> builder.bucket(archiveLocation.getFileContainerId())
							.lifecycleConfiguration(lifeCycleConfiguration)
							.overrideConfiguration(requestOverrideConfiguration))
					.join();

		} catch (UnsupportedEncodingException e) {
			throw new HpcException(
					"[S3] Failed to add a new rule to life cycle policy on bucket "
							+ archiveLocation.getFileContainerId() + ":" + prefix + " - " + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e);

		} catch (CompletionException e) {
			throw new HpcException(
					"[S3] Failed to add a new rule to life cycle policy on bucket "
							+ archiveLocation.getFileContainerId() + ":" + prefix + " - " + e.getCause().getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e.getCause());
		}
	}

	@Override
	public void shutdown(Object authenticatedToken) throws HpcException {
		try {
			s3Connection.getTransferManager(authenticatedToken).close();
			s3Connection.getPresigner(authenticatedToken).close();
			s3Connection.getClient(authenticatedToken).close();

		} catch (Exception e) {
			throw new HpcException("[S3] Failed to shutdown AWS TransferManager/Client/Presigner: " + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, e);
		}
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
		// Create a S3 upload file request.
		HpcS3ProgressListener listener = new HpcS3ProgressListener(progressListener,
				"upload staged file [" + sourceFile.getAbsolutePath() + "] to "
						+ archiveDestinationLocation.getFileContainerId() + ":"
						+ archiveDestinationLocation.getFileId());

		UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
				.putObjectRequest(b -> b.bucket(archiveDestinationLocation.getFileContainerId())
						.key(archiveDestinationLocation.getFileId()).metadata(toS3Metadata(metadataEntries))
						.storageClass(storageClass))
				.addTransferListener(listener).source(sourceFile).build();

		// Upload the data.
		FileUpload fileUpload = null;
		Calendar dataTransferStarted = Calendar.getInstance();
		Calendar dataTransferCompleted = null;
		try {
			fileUpload = s3Connection.getTransferManager(authenticatedToken).uploadFile(uploadFileRequest);
			progressListener.setCompletableFuture(fileUpload.completionFuture());
			fileUpload.completionFuture().join();

			dataTransferCompleted = Calendar.getInstance();

		} catch (CompletionException e) {
			throw new HpcException("[S3] Failed to upload file.", HpcErrorType.DATA_TRANSFER_ERROR,
					s3Connection.getS3Provider(authenticatedToken), e.getCause());

		}

		// Upload completed. Create and populate the response object.
		HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
		uploadResponse.setArchiveLocation(archiveDestinationLocation);
		uploadResponse.setDataTransferType(HpcDataTransferType.S_3);
		uploadResponse.setDataTransferStarted(dataTransferStarted);
		uploadResponse.setDataTransferCompleted(dataTransferCompleted);
		uploadResponse.setDataTransferRequestId(String.valueOf(fileUpload.hashCode()));
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
			throw new HpcException(
					"[S3] No progress listener provided for a upload from AWS S3 / S3 Provider / Google Drive / Google Cloud Storage",
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

		if (s3UploadSource != null) { // Upload by streaming from AWS or S3 Provider.
			uploadMethod = HpcDataTransferUploadMethod.S_3;
			sourceLocation = s3UploadSource.getSourceLocation();

			// If not provided, generate a download pre-signed URL for the requested data
			// file from AWS // (using the provided S3 account).
			sourceURL = StringUtils.isEmpty(s3UploadSource.getSourceURL())
					? generateDownloadRequestURL(s3Connection.authenticate(s3UploadSource.getAccount()), sourceLocation,
							baseArchiveDestination, S3_STREAM_EXPIRATION)
					: s3UploadSource.getSourceURL();

		} else if (googleDriveUploadSource != null) { // Upload by streaming from Google Drive
			uploadMethod = HpcDataTransferUploadMethod.GOOGLE_DRIVE;
			sourceLocation = googleDriveUploadSource.getSourceLocation();

		} else if (googleCloudStorageUploadSource != null) { // Upload by streaming from Google Cloud Storage
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

				HpcS3ProgressListener listener = new HpcS3ProgressListener(progressListener,
						sourceDestinationLogMessage);

				// Create a S3 upload request.
				BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(size);
				Upload streamUpload = s3Connection.getTransferManager(authenticatedToken)
						.upload(builder -> builder
								.putObjectRequest(
										request -> request.bucket(archiveDestinationLocation.getFileContainerId())
												.key(archiveDestinationLocation.getFileId())
												.metadata(toS3Metadata(metadataEntries)).storageClass(storageClass))
								.requestBody(body).addTransferListener(listener));

				// Stream the data.
				body.writeInputStream(sourceInputStream);
				progressListener.setCompletableFuture(streamUpload.completionFuture());
				streamUpload.completionFuture().join();

			} catch (CompletionException | HpcException | IOException e) {
				logger.error("[S3] Failed to upload from AWS S3 destination: " + e.getCause().getMessage(), e);
				progressListener.transferFailed(e.getCause().getMessage());

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
	 * @param metadataEntries            The metadata entries to attach to the
	 *                                   data-object in S3 archive.
	 * @param uploadRequestURLChecksum   An optional user provided checksum value to
	 *                                   attach to the generated url.
	 * @param storageClass               (Optional) The storage class to upload the
	 *                                   file.
	 * @param uploadCompletion           An indicator whether the user will call an
	 *                                   API to complete the registration once the
	 *                                   file is uploaded via the generated URL.
	 * @return A data object upload response containing the upload request URL.
	 * @throws HpcException on data transfer system failure.
	 */
	private HpcDataObjectUploadResponse generateUploadRequestURL(Object authenticatedToken,
			HpcFileLocation archiveDestinationLocation, int uploadRequestURLExpiration,
			List<HpcMetadataEntry> metadataEntries, String uploadRequestURLChecksum, String storageClass,
			boolean uploadCompletion) throws HpcException {
		PutObjectRequest objectRequest = PutObjectRequest.builder()
				.bucket(archiveDestinationLocation.getFileContainerId()).key(archiveDestinationLocation.getFileId())
				// .metadata(toS3Metadata(metadataEntries)) - TODO: setting metadata on the URL
				// cause Cloudian upload w/ URL to fail.
				.storageClass(storageClass).contentMD5(uploadRequestURLChecksum).build();
		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(Duration.ofHours(uploadRequestURLExpiration)).putObjectRequest(objectRequest)
				.build();

		PresignedPutObjectRequest presignedRequest = null;
		URL url = null;

		// Generate the upload pre-signed upload URL.
		try {
			presignedRequest = s3Connection.getPresigner(authenticatedToken).presignPutObject(presignRequest);
			url = presignedRequest.url();

		} catch (SdkException e) {
			throw new HpcException("[S3] Failed to create a pre-signed URL", HpcErrorType.DATA_TRANSFER_ERROR,
					s3Connection.getS3Provider(authenticatedToken), e);
		}

		// Create and populate the response object.
		HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
		uploadResponse.setArchiveLocation(archiveDestinationLocation);
		uploadResponse.setDataTransferType(HpcDataTransferType.S_3);
		uploadResponse.setDataTransferStarted(Calendar.getInstance());
		uploadResponse.setDataTransferCompleted(null);
		uploadResponse.setDataTransferRequestId(String.valueOf(presignedRequest.hashCode()));
		uploadResponse.setUploadRequestURL(url.toString());
		uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.URL_GENERATED);
		uploadResponse
				.setDataTransferMethod(uploadCompletion ? HpcDataTransferUploadMethod.URL_SINGLE_PART_WITH_COMPLETION
						: HpcDataTransferUploadMethod.URL_SINGLE_PART);

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
		CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
				.bucket(archiveDestinationLocation.getFileContainerId()).key(archiveDestinationLocation.getFileId())
				.metadata(toS3Metadata(metadataEntries)).storageClass(storageClass).build();

		try {
			multipartUpload.setId(s3Connection.getClient(authenticatedToken)
					.createMultipartUpload(createMultipartUploadRequest).join().uploadId());

		} catch (CompletionException e) {
			throw new HpcException("[S3] Failed to create a multipart upload: " + createMultipartUploadRequest,
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e.getCause());
		}

		// Generate the parts pre-signed upload URLs.
		UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
				.bucket(archiveDestinationLocation.getFileContainerId()).key(archiveDestinationLocation.getFileId())
				.uploadId(multipartUpload.getId()).build();
		for (int partNumber = 1; partNumber <= uploadParts; partNumber++) {
			HpcUploadPartURL uploadPartURL = new HpcUploadPartURL();
			uploadPartURL.setPartNumber(partNumber);

			// Create a UploadPartPresignRequest to specify the signature duration
			UploadPartPresignRequest uploadPartPresignRequest = UploadPartPresignRequest.builder()
					.signatureDuration(Duration.ofHours(uploadRequestURLExpiration))
					.uploadPartRequest(uploadPartRequest.toBuilder().partNumber(partNumber).build()).build();

			try {
				uploadPartURL.setPartUploadRequestURL(s3Connection.getPresigner(authenticatedToken)
						.presignUploadPart(uploadPartPresignRequest).url().toString());

			} catch (SdkException e) {
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
		uploadResponse.setDataTransferRequestId(String.valueOf(createMultipartUploadRequest.hashCode()));
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
	private Map<String, String> toS3Metadata(List<HpcMetadataEntry> metadataEntries) {

		Map<String, String> objectMetadata = new HashMap<>();
		if (metadataEntries != null) {
			metadataEntries.forEach(
					metadataEntry -> objectMetadata.put(metadataEntry.getAttribute(), metadataEntry.getValue()));
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
		DownloadFileRequest.Builder downloadFileRequestBuilder = DownloadFileRequest.builder()
				.getObjectRequest(b -> b.bucket(archiveLocation.getFileContainerId()).key(archiveLocation.getFileId()))
				.destination(destinationLocation);
		if (progressListener != null) {
			downloadFileRequestBuilder.addTransferListener(new HpcS3ProgressListener(progressListener,
					"download from " + archiveLocation.getFileContainerId() + ":" + archiveLocation.getFileId()));
		}

		FileDownload downloadFile = null;
		try {
			downloadFile = s3Connection.getTransferManager(authenticatedToken)
					.downloadFile(downloadFileRequestBuilder.build());

			if (progressListener == null) {
				// Download synchronously.
				downloadFile.completionFuture().join();
			} else {
				progressListener.setCompletableFuture(downloadFile.completionFuture());
			}

		} catch (CompletionException | SdkException e) {
			throw new HpcException("[S3] Failed to download file: [" + e.getCause().getMessage() + "]",
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e.getCause());

		}

		return String.valueOf(downloadFile.hashCode());
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
				String sourceDestinationLogMessage = "download to " + destinationLocation.getFileContainerId() + ":"
						+ destinationLocation.getFileId();
				HpcS3ProgressListener listener = new HpcS3ProgressListener(progressListener,
						sourceDestinationLogMessage);

				// Create a S3 upload request.
				BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(fileSize);
				Upload streamUpload = s3Connection.getTransferManager(s3AccountAuthenticatedToken)
						.upload(builder -> builder
								.putObjectRequest(request -> request.bucket(destinationLocation.getFileContainerId())
										.key(destinationLocation.getFileId()))
								.requestBody(body).addTransferListener(listener));

				logger.info("S3 download Archive->AWS/S3 Provider [{}] started. Source size - {} bytes",
						sourceDestinationLogMessage, fileSize);

				// Stream the data.
				body.writeInputStream(sourceInputStream);
				progressListener.setCompletableFuture(streamUpload.completionFuture());
				streamUpload.completionFuture().join();

			} catch (CompletionException | HpcException | IOException e) {
				logger.error("[S3] Failed to download to S3 destination: " + e.getCause().getMessage(), e);
				progressListener.transferFailed(e.getCause().getMessage());

			}

		}, s3Executor);

		return String.valueOf(s3TransferManagerDownloadFuture.hashCode());
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
			try { // Check if this is a directory. Use V2 listObjects API.
				ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
						.bucket(fileLocation.getFileContainerId()).prefix(fileLocation.getFileId() + "/").build();

				ListObjectsV2Response listObjectsV2Response = s3Connection.getClient(authenticatedToken)
						.listObjectsV2(listObjectsV2Request).join();

				return listObjectsV2Response.keyCount() > 0 || !listObjectsV2Response.contents().isEmpty();

			} catch (CompletionException e) {
				if (e.getCause() instanceof SdkServiceException
						&& ((SdkServiceException) e.getCause()).statusCode() == 400) { // V2 not supported. Use V1
																						// listObjects API.
					ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
							.bucket(fileLocation.getFileContainerId()).prefix(fileLocation.getFileId()).build();

					return !s3Connection.getClient(authenticatedToken).listObjects(listObjectsRequest).join().contents()
							.isEmpty();
				} else {
					throw e;
				}
			}

		} catch (CompletionException e) {
			throw new HpcException("[S3] Failed to list object: " + e.getCause().getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, s3Connection.getS3Provider(authenticatedToken), e.getCause());
		}
	}
}
