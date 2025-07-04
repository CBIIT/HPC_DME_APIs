/**
 * HpcDataTransferProxy.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveObjectMetadata;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusTransferRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcSetArchiveObjectMetadataResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcUploadPartETag;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.model.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemTokens;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Transfer Proxy Interface.
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 */
public interface HpcDataTransferProxy {

	/**
	 * Authenticate the invoker w/ the data transfer system.
	 *
	 * @param dataTransferAccount The Data Transfer account to authenticate.
	 * @param urlOrRegion         The archive URL to connect to for 3rd party S3
	 *                            provider and Globus, or the AWS region.
	 * @param encryptionAlgorithm (Optional) The encryption algorithm.
	 * @param encryptionKey       (Optional) The encryption key.
	 * @return An authenticated token, to be used in subsequent calls to data
	 *         transfer. It returns null if the account is not authenticated.
	 * 
	 * @throws HpcException on data transfer system failure.
	 */
	// TODO: clean up encryption as no longer supported w/ V2
	public default Object authenticate(HpcIntegratedSystemAccount dataTransferAccount, String urlOrRegion,
			String encryptionAlgorithm, String encryptionKey) throws HpcException {
		throw new HpcException("authenticate(dataTransferAccount, String url) not supported",
				HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Authenticate a AWS S3 account.
	 *
	 * @param s3Account S3 account.
	 * @return An authenticated token, to be used in subsequent calls to data
	 *         transfer.
	 * @throws HpcException on data transfer system failure.
	 */
	public default Object authenticate(HpcS3Account s3Account) throws HpcException {
		throw new HpcException("authenticate(s3Account) not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Authenticate a Google Drive / Google Cloud Storage account.
	 *
	 * @param accessToken Google Drive / Storage access token.
	 * @return An authenticated token, to be used in subsequent calls to data
	 *         transfer.
	 * @throws HpcException on data transfer system failure.
	 */
	public default Object authenticate(String accessToken) throws HpcException {
		throw new HpcException("authenticate(Google access-token) not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Authenticate a Box account.
	 *
	 * @param dataTransferAccount The Data Transfer account to authenticate.
	 * @param accessToken         Box access token.
	 * @param refreshToken        Box refresh token.
	 * @return An authenticated token, to be used in subsequent calls to data
	 *         transfer.
	 * @throws HpcException on data transfer system failure.
	 */
	public default Object authenticate(HpcIntegratedSystemAccount dataTransferAccount, String accessToken,
			String refreshToken) throws HpcException {
		throw new HpcException("authenticate(Box auth-code) not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Get integrated system tokens (access & refresh) out of the abstracted token.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @return Integrated system tokens (access & refresh)
	 * @throws HpcException on data transfer system failure.
	 */
	public default HpcIntegratedSystemTokens getIntegratedSystemTokens(Object authenticatedToken) throws HpcException {
		throw new HpcException("getIntegratedSystemTokens() not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Check if upload/download requests are accepted at the moment.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @return HpcTransferAcceptanceResponse for which: 1. the method
	 *         canAcceptTransfer() returns true if upload/download requests are
	 *         accepted or false if the data-transfer system is too busy. 2. the
	 *         method getQueueSize() returns int that is size of data transfer
	 *         queue.
	 * @throws HpcException on data transfer system failure.
	 */
	public default HpcTransferAcceptanceResponse acceptsTransferRequests(Object authenticatedToken)
			throws HpcException {
		throw new HpcException("acceptsTransferRequests() is not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Upload a data object file.
	 *
	 * @param authenticatedToken         An authenticated token.
	 * @param uploadRequest              The data upload request
	 * @param baseArchiveDestination     The archive's base destination location.
	 * @param uploadRequestURLExpiration The expiration period (in days) to set when
	 *                                   generating upload URL.
	 * @param progressListener           (Optional) a progress listener for async
	 *                                   notification on transfer completion.
	 * @param metadataEntries            The metadata entries to attach to the
	 *                                   data-object in the archive.
	 * @param encryptedTransfer          (Optional) encrypted transfer indicator.
	 * @param storageClass               (Optional) The storage class to use when
	 *                                   uploading the data object. Applicable for
	 *                                   S3 archives only.
	 * @return A data object upload response.
	 * @throws HpcException on data transfer system failure.
	 */
	public default HpcDataObjectUploadResponse uploadDataObject(Object authenticatedToken,
			HpcDataObjectUploadRequest uploadRequest, HpcArchive baseArchiveDestination,
			Integer uploadRequestURLExpiration, HpcDataTransferProgressListener progressListener,
			List<HpcMetadataEntry> metadataEntries, Boolean encryptedTransfer, String storageClass)
			throws HpcException {
		throw new HpcException("uploadDataObject() is not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Download a data object file.
	 *
	 * @param authenticatedToken     An authenticated token.
	 * @param downloadRequest        The data object download request.
	 * @param baseArchiveDestination The archive's base destination location.
	 * @param progressListener       (Optional) a progress listener for async
	 *                               notification on transfer completion.
	 * @param encryptedTransfer      (Optional) encrypted transfer indicator
	 * @return A data transfer request Id.
	 * @throws HpcException on data transfer system failure.
	 */
	public String downloadDataObject(Object authenticatedToken, HpcDataObjectDownloadRequest downloadRequest,
			HpcArchive baseArchiveDestination, HpcDataTransferProgressListener progressListener,
			Boolean encryptedTransfer) throws HpcException;

	/**
	 * Generate a download URL for a data object file.
	 *
	 * @param authenticatedToken           An authenticated token.
	 * @param archiveLocation              The data object's archive location.
	 * @param baseArchiveDestination       The archive's base destination location.
	 * @param downloadRequestURLExpiration The expiration period (in days) to set
	 *                                     when generating download URL.
	 * @return The download URL
	 * @throws HpcException on data transfer system failure.
	 */
	public default String generateDownloadRequestURL(Object authenticatedToken, HpcFileLocation archiveLocation,
			HpcArchive baseArchiveDestination, Integer downloadRequestURLExpiration) throws HpcException {
		throw new HpcException("Generating download URL is not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Generate an input stream for a data object file.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @param archiveLocation    The data object's archive location.
	 * @return Input stream to the file.
	 * @throws HpcException on data transfer system failure.
	 */
	public default InputStream generateDownloadInputStream(Object authenticatedToken, HpcFileLocation archiveLocation)
			throws HpcException {
		throw new HpcException("Generating download InputStream is not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Set metadata for object file in the archive.
	 *
	 * @param authenticatedToken     An authenticated token.
	 * @param fileLocation           The file location.
	 * @param baseArchiveDestination The archive's base destination location.
	 * @param metadataEntries        The metadata to set.
	 * @param storageClass           (Optional) The storage class to use when
	 *                               setting the data object metadata. Applicable
	 *                               for S3 archives only.
	 * @return A response w/ the object checksum, and an indicator whether metadata
	 *         got added or already existed.
	 * @throws HpcException on data transfer system failure.
	 */
	public default HpcSetArchiveObjectMetadataResponse setDataObjectMetadata(Object authenticatedToken,
			HpcFileLocation fileLocation, HpcArchive baseArchiveDestination, List<HpcMetadataEntry> metadataEntries,
			String storageClass) throws HpcException {
		throw new HpcException("setDataObjectMetadata() is not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Validate metadata for object file in the archive.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @param fileLocation       The file location.
	 * @param metadataEntries    The metadata to validate exists in the archive
	 * @return True if the metadata exists, or false otherwise
	 * @throws HpcException on data transfer system failure.
	 */
	public default boolean validateDataObjectMetadata(Object authenticatedToken, HpcFileLocation fileLocation,
			List<HpcMetadataEntry> metadataEntries) throws HpcException {
		throw new HpcException("validateDataObjectMetadata() is not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Delete a data object file.
	 *
	 * @param authenticatedToken     An authenticated token.
	 * @param fileLocation           The file location.
	 * @param baseArchiveDestination The archive's base destination location.
	 * @throws HpcException on data transfer system failure.
	 */
	public default void deleteDataObject(Object authenticatedToken, HpcFileLocation fileLocation,
			HpcArchive baseArchiveDestination) throws HpcException {
		throw new HpcException("deleteDataObject is not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Get a data transfer upload request status.
	 *
	 * @param authenticatedToken     An authenticated token.
	 * @param dataTransferRequestId  The data transfer request ID.
	 * @param baseArchiveDestination The archive's base destination location.
	 * @param loggingPrefix          Contextual (download/upload tasks etc) logging
	 *                               text to prefix in logging.
	 * @return The data transfer request status.
	 * @throws HpcException on data transfer system failure.
	 */
	public default HpcDataTransferUploadReport getDataTransferUploadStatus(Object authenticatedToken,
			String dataTransferRequestId, HpcArchive baseArchiveDestination, String loggingPrefix) throws HpcException {
		throw new HpcException("getDataTransferUploadStatus() not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Get a data transfer download request status.
	 *
	 * @param authenticatedToken    An authenticated token.
	 * @param dataTransferRequestId The data transfer request ID.
	 * @param successfulItems       True to include successful items in the
	 *                              response.
	 * @param loggingPrefix         Contextual (download/upload tasks etc) logging
	 *                              text to prefix in logging.
	 * @return The data transfer request status.
	 * @throws HpcException on data transfer system failure.
	 */
	public default HpcDataTransferDownloadReport getDataTransferDownloadStatus(Object authenticatedToken,
			String dataTransferRequestId, boolean successfulItems, String loggingPrefix) throws HpcException {
		throw new HpcException("getDataTransferDownloadStatus() not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Get attributes of a file/directory.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @param fileLocation       The endpoint/path to check.
	 * @param getSize            If set to true, the file/directory size will be
	 *                           returned.
	 * @return The path attributes.
	 * @throws HpcException on data transfer system failure.
	 */
	public default HpcPathAttributes getPathAttributes(Object authenticatedToken, HpcFileLocation fileLocation,
			boolean getSize) throws HpcException {
		throw new HpcException("getPathAttributes() not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Scan a directory (recursively) and return a list of all its files.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @param directoryLocation  The endpoint/path to scan.
	 * @return A list of files found.
	 * @throws HpcException on data transfer system failure.
	 */
	public default List<HpcDirectoryScanItem> scanDirectory(Object authenticatedToken,
			HpcFileLocation directoryLocation) throws HpcException {
		throw new HpcException("scanDirectory() not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Get a file container name.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @param fileContainerId    The file container ID.
	 * @return The file container name.
	 * @throws HpcException on data transfer system failure.
	 */
	public default String getFileContainerName(Object authenticatedToken, String fileContainerId) throws HpcException {
		return fileContainerId;
	}

	/**
	 * Complete a multipart upload.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @param archiveLocation    The archive location.
	 * @param multipartUploadId  The multipart upload ID generated when the
	 *                           multipart upload was initiated.
	 * @param uploadPartETags    A list of ETag for each part uploaded.
	 * @return The checksum of the object at upload completion.
	 * @throws HpcException on data transfer system failure.
	 */
	public default String completeMultipartUpload(Object authenticatedToken, HpcFileLocation archiveLocation,
			String multipartUploadId, List<HpcUploadPartETag> uploadPartETags) throws HpcException {
		throw new HpcException("completeMultipartUpload() not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Cancel a transfer request.
	 *
	 * @param authenticatedToken    An authenticated token.
	 * @param dataTransferRequestId The globus task ID.
	 * @param message               The message to attach to the cancellation
	 *                              request.
	 * @throws HpcException on data transfer system failure.
	 */
	public default void cancelTransferRequest(Object authenticatedToken, String dataTransferRequestId, String message)
			throws HpcException {
		throw new HpcException("cancelTransferRequest() not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Calculate archive file location to upload a data object.
	 *
	 * @param baseArchiveDestination The base (archive specific) destination.
	 * @param path                   The data object (logical) path.
	 * @param callerObjectId         The caller's objectId.
	 * @param archiveType            The type of the archive.
	 * @param dataTransferProxy      A concrete instance of data transfer proxy.
	 *                               authenticatedToken An authenticated token.
	 * @return The calculated data transfer upload archive destination that is
	 *         unique.
	 */
	public static HpcFileLocation getArchiveDestinationLocation(HpcFileLocation baseArchiveDestination, String path,
			String callerObjectId, HpcArchiveType archiveType, HpcDataTransferProxy dataTransferProxy,
			Object authenticatedToken) throws HpcException {
		// Calculate the data transfer destination absolute path as the following:
		StringBuilder destinationPath = new StringBuilder();
		destinationPath.append(baseArchiveDestination.getFileId());

		HpcFileLocation archiveDestination = new HpcFileLocation();
		archiveDestination.setFileContainerId(baseArchiveDestination.getFileContainerId());

		if (archiveType.equals(HpcArchiveType.ARCHIVE)) {
			// For Archive destination, destination path is:
			// 'base path' / 'caller's object-id / 'logical path'_'generated UUID' (note:
			// generated UUID is optional)
			if (!StringUtils.isEmpty(callerObjectId)) {
				if (callerObjectId.charAt(0) != '/') {
					destinationPath.append('/');
				}
				destinationPath.append(callerObjectId);
			}

			if (path.charAt(0) != '/') {
				destinationPath.append('/');
			}
			if (destinationPath.charAt(destinationPath.length() - 1) == '/' && path.charAt(0) == '/') {
				destinationPath.append(path.substring(1));
			} else {
				destinationPath.append(path);
			}
			archiveDestination.setFileId(destinationPath.toString());

			// If the generated archive path exists, inject a numeric folder into the path
			// before the file name.
			for (int i = 1; dataTransferProxy.getPathAttributes(authenticatedToken, archiveDestination, false)
					.getExists(); i++) {
				StringBuffer uniquePath = new StringBuffer();
				String currentPath = archiveDestination.getFileId();
				String currentCollection = currentPath.substring(0, currentPath.lastIndexOf('/'));
				if (i == 1) {
					uniquePath.append(currentCollection + "/" + i);
				} else {
					uniquePath.append(currentCollection.substring(0, currentCollection.lastIndexOf('/')) + "/" + i);
				}

				uniquePath.append(currentPath.substring(currentPath.lastIndexOf('/')));
				archiveDestination.setFileId(uniquePath.toString());
			}

		} else {
			// For Temporary Archive, destination path is:
			// 'base path' / generated UUID.
			destinationPath.append('/' + UUID.randomUUID().toString());
			archiveDestination.setFileId(destinationPath.toString());
		}

		return archiveDestination;
	}

	/**
	 * Get metadata for object file in the archive.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @param fileLocation       The file location.
	 * @return metadataEntries
	 * @throws HpcException on data transfer system failure.
	 */
	public default HpcArchiveObjectMetadata getDataObjectMetadata(Object authenticatedToken,
			HpcFileLocation fileLocation) throws HpcException {
		throw new HpcException("getDataObjectMetadata is not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Create/Replace Tiering Policy on a bucket
	 *
	 * @param authenticatedToken An authenticated token.
	 * @param archiveLocation    The archive location.
	 * @param prefix             The prefix to add .
	 * @param tieringBucket      The bucket name to tier to. (Optional)
	 * @param tieringProtocol    The tiering protocol used. (Optional)
	 * @throws HpcException on data transfer system failure.
	 */
	public default void setTieringPolicy(Object authenticatedToken, HpcFileLocation archiveLocation, String prefix,
			String tieringBucket, String tieringProtocol) throws HpcException {
		throw new HpcException("setTieringPolicy() not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Restore a data object file.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @param fileLocation       The file location.
	 * @throws HpcException on data transfer system failure.
	 */
	public default void restoreDataObject(Object authenticatedToken, HpcFileLocation fileLocation) throws HpcException {
		throw new HpcException("restoreDataObject is not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Check if Tiering Policy exists for an archive location
	 *
	 * @param authenticatedToken An authenticated token.
	 * @param archiveLocation    The archive location.
	 * @throws HpcException on data transfer system failure.
	 */
	public default boolean existsTieringPolicy(Object authenticatedToken, HpcFileLocation archiveLocation)
			throws HpcException {
		throw new HpcException("existsTieringPolicy() not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Submit a data transfer request to Globus.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @param transferRequest    The Globus transfer request
	 * @param encryptedTransfer  (Optional) encrypted transfer indicator
	 * @return The data transfer request ID.
	 * @throws HpcException on data transfer system failure.
	 */
	public default String transferData(Object authenticatedToken, HpcGlobusTransferRequest transferRequest,
			Boolean encryptedTransfer) throws HpcException {
		throw new HpcException("transferData() not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	/**
	 * Shutdown a transfer manager
	 *
	 * @param authenticatedToken An authenticated token.
	 * @throws HpcException on data transfer system failure.
	 */
	public default void shutdown(Object authenticatedToken) throws HpcException {
		throw new HpcException("shutdown(authenticatedToken) not supported", HpcErrorType.UNEXPECTED_ERROR);
	}
}
