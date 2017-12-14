package gov.nih.nci.hpc.integration.s3.impl;

import static gov.nih.nci.hpc.integration.HpcDataTransferProxy.getArchiveDestinationLocation;

import java.io.File;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.Upload;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
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
  // Instance members
  // ---------------------------------------------------------------------//

  // The S3 connection instance.
  @Autowired private HpcS3Connection s3Connection = null;

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /** Constructor for spring injection. */
  private HpcDataTransferProxyImpl() {}

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  // ---------------------------------------------------------------------//
  // HpcDataTransferProxy Interface Implementation
  // ---------------------------------------------------------------------//

  @Override
  public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount, String url)
      throws HpcException {
    return s3Connection.authenticate(dataTransferAccount, url);
  }

  @Override
  public HpcDataObjectUploadResponse uploadDataObject(
      Object authenticatedToken,
      HpcDataObjectUploadRequest uploadRequest,
      HpcArchive baseArchiveDestination,
      Integer uploadRequestURLExpiration,
      HpcDataTransferProgressListener progressListener)
      throws HpcException {
    // Upload from remote endpoint not supported.
    if (uploadRequest.getSourceLocation() != null) {
      throw new HpcException(
          "S3 data transfer doesn't support upload from remote endpoint",
          HpcErrorType.UNEXPECTED_ERROR);
    }

    // Calculate the archive destination.
    HpcFileLocation archiveDestinationLocation =
        getArchiveDestinationLocation(
            baseArchiveDestination.getFileLocation(),
            uploadRequest.getPath(),
            uploadRequest.getCallerObjectId(),
            baseArchiveDestination.getType());

    if (uploadRequest.getGenerateUploadRequestURL()) {
      // Generate an upload request URL for the caller to use to upload directly.
      return generateUploadRequestURL(
          authenticatedToken, archiveDestinationLocation, uploadRequestURLExpiration);

    } else {
      // Upload a file
      return uploadDataObject(
          authenticatedToken,
          uploadRequest.getSourceFile(),
          archiveDestinationLocation,
          progressListener,
          baseArchiveDestination.getType());
    }
  }

  @Override
  public HpcDataObjectDownloadResponse downloadDataObject(
      Object authenticatedToken,
      HpcDataObjectDownloadRequest downloadRequest,
      HpcDataTransferProgressListener progressListener)
      throws HpcException {
    // Create a S3 download request.
    GetObjectRequest request =
        new GetObjectRequest(
            downloadRequest.getArchiveLocation().getFileContainerId(),
            downloadRequest.getArchiveLocation().getFileId());

    // Download the file via S3.
    Download s3Download = null;
    try {
      s3Download =
          s3Connection
              .getTransferManager(authenticatedToken)
              .download(request, downloadRequest.getDestinationFile());
      if (progressListener == null) {
        // Download synchronously.
        s3Download.waitForCompletion();
      } else {
        // Download asynchronously.
        s3Download.addProgressListener(new HpcS3ProgressListener(progressListener));
      }

    } catch (AmazonClientException ace) {
      throw new HpcException(
          "[S3] Failed to download file: [" + ace.getMessage() + "]",
          HpcErrorType.DATA_TRANSFER_ERROR,
          HpcIntegratedSystem.CLEVERSAFE,
          ace);

    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }

    HpcDataObjectDownloadResponse downloadResponse = new HpcDataObjectDownloadResponse();
    downloadResponse.setDataTransferRequestId(String.valueOf(s3Download.hashCode()));
    downloadResponse.setDestinationFile(downloadRequest.getDestinationFile());

    return downloadResponse;
  }

  @Override
  public String copyDataObject(
      Object authenticatedToken,
      HpcFileLocation sourceFile,
      HpcFileLocation destinationFile,
      HpcArchive baseArchiveDestination,
      List<HpcMetadataEntry> metadataEntries)
      throws HpcException {

    // Create a S3 update request w/ new metadata. Copy the file to itself.
    CopyObjectRequest copyRequest =
        new CopyObjectRequest(
                sourceFile.getFileContainerId(),
                sourceFile.getFileId(),
                destinationFile.getFileContainerId(),
                destinationFile.getFileId())
            .withNewObjectMetadata(toS3Metadata(metadataEntries));

    try {
      CopyObjectResult copyResult =
          s3Connection
              .getTransferManager(authenticatedToken)
              .getAmazonS3Client()
              .copyObject(copyRequest);
      return copyResult != null ? copyResult.getETag() : null;

    } catch (AmazonServiceException ase) {
      throw new HpcException(
          "[S3] Failed to copy file: " + copyRequest,
          HpcErrorType.DATA_TRANSFER_ERROR,
          HpcIntegratedSystem.CLEVERSAFE,
          ase);

    } catch (AmazonClientException ace) {
      throw new HpcException(
          "[S3] Failed to copy file: " + copyRequest,
          HpcErrorType.DATA_TRANSFER_ERROR,
          HpcIntegratedSystem.CLEVERSAFE,
          ace);
    }
  }

  @Override
  public void deleteDataObject(
      Object authenticatedToken, HpcFileLocation fileLocation, HpcArchive baseArchiveDestination)
      throws HpcException {
    // Create a S3 delete request.
    DeleteObjectRequest deleteRequest =
        new DeleteObjectRequest(fileLocation.getFileContainerId(), fileLocation.getFileId());
    try {
      s3Connection
          .getTransferManager(authenticatedToken)
          .getAmazonS3Client()
          .deleteObject(deleteRequest);

    } catch (AmazonServiceException ase) {
      throw new HpcException(
          "[S3] Failed to delete file: " + deleteRequest,
          HpcErrorType.DATA_TRANSFER_ERROR,
          HpcIntegratedSystem.CLEVERSAFE,
          ase);

    } catch (AmazonClientException ace) {
      throw new HpcException(
          "[S3] Failed to delete file: " + deleteRequest,
          HpcErrorType.DATA_TRANSFER_ERROR,
          HpcIntegratedSystem.CLEVERSAFE,
          ace);
    }
  }

  @Override
  public HpcPathAttributes getPathAttributes(
      Object authenticatedToken, HpcFileLocation fileLocation, boolean getSize)
      throws HpcException {
    HpcPathAttributes pathAttributes = new HpcPathAttributes();
    GetObjectMetadataRequest request = null;

    try {
      boolean objectExists =
          s3Connection
              .getTransferManager(authenticatedToken)
              .getAmazonS3Client()
              .doesObjectExist(fileLocation.getFileContainerId(), fileLocation.getFileId());

      pathAttributes.setIsAccessible(true);
      pathAttributes.setIsDirectory(false);
      pathAttributes.setExists(objectExists);
      pathAttributes.setIsFile(objectExists);

      // Optionally get the file size.
      if (getSize && objectExists) {
        // Create a S3 metadata request.
        request =
            new GetObjectMetadataRequest(
                fileLocation.getFileContainerId(), fileLocation.getFileId());
        pathAttributes.setSize(
            s3Connection
                .getTransferManager(authenticatedToken)
                .getAmazonS3Client()
                .getObjectMetadata(request)
                .getContentLength());
      }

    } catch (AmazonServiceException ase) {
      throw new HpcException(
          "[S3] Failed to get metadata: " + request,
          HpcErrorType.DATA_TRANSFER_ERROR,
          HpcIntegratedSystem.CLEVERSAFE,
          ase);

    } catch (AmazonClientException ace) {
      throw new HpcException(
          "[S3] Failed to get metadata: " + request,
          HpcErrorType.DATA_TRANSFER_ERROR,
          HpcIntegratedSystem.CLEVERSAFE,
          ace);
    }

    return pathAttributes;
  }

  // ---------------------------------------------------------------------//
  // Helper Methods
  // ---------------------------------------------------------------------//

  /**
   * Upload a data object file.
   *
   * @param authenticatedToken An authenticated token.
   * @param sourceFile The file to upload.
   * @param archiveDestinationLocation The archive destination location.
   * @param progressListener (Optional) a progress listener for async notification on transfer
   *     completion.
   * @param archiveType The archive type.
   * @return A data object upload response.
   * @throws HpcException on data transfer system failure.
   */
  private HpcDataObjectUploadResponse uploadDataObject(
      Object authenticatedToken,
      File sourceFile,
      HpcFileLocation archiveDestinationLocation,
      HpcDataTransferProgressListener progressListener,
      HpcArchiveType archiveType)
      throws HpcException {
    // Create a S3 upload request.
    PutObjectRequest request =
        new PutObjectRequest(
            archiveDestinationLocation.getFileContainerId(),
            archiveDestinationLocation.getFileId(),
            sourceFile);

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
        s3Upload.addProgressListener(new HpcS3ProgressListener(progressListener));
      }

    } catch (AmazonClientException ace) {
      throw new HpcException(
          "[S3] Failed to upload file.",
          HpcErrorType.DATA_TRANSFER_ERROR,
          HpcIntegratedSystem.CLEVERSAFE,
          ace);

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
    if (archiveType.equals(HpcArchiveType.ARCHIVE)) {
      uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.ARCHIVED);
    } else {
      uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.IN_TEMPORARY_ARCHIVE);
    }

    return uploadResponse;
  }

  /**
   * Generate an upload request URL.
   *
   * @param authenticatedToken An authenticated token.
   * @param archiveDestinationLocation The archive destination location.
   * @param objectMetadata The S3 object metadata.
   * @param uploadRequestURLExpiration The URL expiration (in hours).
   * @return A data object upload response containing the upload request URL.
   * @throws HpcException on data transfer system failure.
   */
  private HpcDataObjectUploadResponse generateUploadRequestURL(
      Object authenticatedToken,
      HpcFileLocation archiveDestinationLocation,
      int uploadRequestURLExpiration)
      throws HpcException {

    // Calculate the URL expiration date.
    Date expiration = new Date();
    expiration.setTime(expiration.getTime() + 1000 * 60 * 60 * uploadRequestURLExpiration);

    // Create a URL generation request.
    GeneratePresignedUrlRequest generatePresignedUrlRequest =
        new GeneratePresignedUrlRequest(
                archiveDestinationLocation.getFileContainerId(),
                archiveDestinationLocation.getFileId())
            .withMethod(HttpMethod.PUT)
            .withExpiration(expiration);

    // Generate the pre-signed URL.
    URL url =
        s3Connection
            .getTransferManager(authenticatedToken)
            .getAmazonS3Client()
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
}
