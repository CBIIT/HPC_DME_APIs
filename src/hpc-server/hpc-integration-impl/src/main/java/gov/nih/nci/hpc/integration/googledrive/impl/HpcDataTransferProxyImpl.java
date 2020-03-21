package gov.nih.nci.hpc.integration.googledrive.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;

/**
 * HPC Data Transfer Proxy Google Drive Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataTransferProxyImpl implements HpcDataTransferProxy {
  // ---------------------------------------------------------------------//
  // Constants
  // ---------------------------------------------------------------------//

  // The expiration of streaming data request from Cleversafe to AWS S3.
  private static final int S3_STREAM_EXPIRATION = 96;

  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // The S3 download executor.
  @Autowired
  Executor s3Executor = null;

  // The logger instance.
  private final Logger logger = LoggerFactory.getLogger(getClass().getName());

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
  public HpcDataObjectUploadResponse uploadDataObject(Object authenticatedToken,
      HpcDataObjectUploadRequest uploadRequest, HpcArchive baseArchiveDestination,
      Integer uploadRequestURLExpiration, HpcDataTransferProgressListener progressListener,
      List<HpcMetadataEntry> metadataEntries) throws HpcException {
    throw new HpcException("uploadDataObject() not supported", HpcErrorType.UNEXPECTED_ERROR);
  }

  @Override
  public String downloadDataObject(Object authenticatedToken,
      HpcDataObjectDownloadRequest downloadRequest, HpcArchive baseArchiveDestination,
      HpcDataTransferProgressListener progressListener) throws HpcException {

    try {
      Drive service = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(),
          JacksonFactory.getDefaultInstance(), new GoogleCredential().setAccessToken(""))
              .setApplicationName("").build();

      File fileMetadata = new File();
      fileMetadata.setName("ERAN.jpg");
      InputStream sourceInputStream = new URL(downloadRequest.getArchiveLocationURL()).openStream();
      service.files().create(fileMetadata,
          new InputStreamContent("application/octet-stream", sourceInputStream));

    } catch (IOException | GeneralSecurityException e) {
      throw new HpcException("[GoogleDrive] Failed to list objects: " + e.getMessage(),
          HpcErrorType.DATA_TRANSFER_ERROR, e);
    }
    /*
     * if (progressListener == null) { throw new HpcException(
     * "[S3] No progress listener provided for a download to AWS S3 destination",
     * HpcErrorType.UNEXPECTED_ERROR); }
     * 
     * CompletableFuture<Void> s3TransferManagerDownloadFuture = CompletableFuture.runAsync( () -> {
     * try { // Create source URL and open a connection to it. InputStream sourceInputStream = new
     * URL(sourceURL).openStream();
     * 
     * // Create a S3 upload request. ObjectMetadata metadata = new ObjectMetadata();
     * metadata.setContentLength(fileSize); PutObjectRequest request = new PutObjectRequest(
     * destinationLocation.getFileContainerId(), destinationLocation.getFileId(), sourceInputStream,
     * metadata);
     * 
     * // Set the read limit on the request to avoid AWSreset exceptions.
     * request.getRequestClientOptions().setReadLimit(getReadLimit(fileSize));
     * 
     * // Upload asynchronously. AWS transfer manager will perform the upload in its own managed
     * thread. Upload s3Upload =
     * s3Connection.getTransferManager(s3AccountAuthenticatedToken).upload(request);
     * 
     * // Attach a progress listener. String sourceDestinationLogMessage = "download to " +
     * destinationLocation.getFileContainerId() + ":" + destinationLocation.getFileId();
     * s3Upload.addProgressListener( new HpcS3ProgressListener(progressListener,
     * sourceDestinationLogMessage));
     * 
     * logger.info(
     * "S3 download Cleversafe->AWS [{}] started. Source size - {} bytes. Read limit - {}",
     * sourceDestinationLogMessage, fileSize, request.getRequestClientOptions().getReadLimit());
     * 
     * // Wait for the result. This ensures the input stream to the URL remains opened and connected
     * until the download is complete. // Note that this wait for AWS transfer manager completion is
     * done in a separate thread (from s3Executor pool), so callers to // the API don't wait.
     * s3Upload.waitForUploadResult();
     * 
     * } catch (AmazonClientException | HpcException | IOException e) { logger.error(
     * "[S3] Failed to downloadload to AWS S3 destination: " + e.getMessage(), e);
     * progressListener.transferFailed(e.getMessage());
     * 
     * } catch (InterruptedException ie) { Thread.currentThread().interrupt(); } }, s3Executor);
     * 
     * return String.valueOf(s3TransferManagerDownloadFuture.hashCode());
     */
    return null;
  }

  @Override
  public HpcPathAttributes getPathAttributes(Object authenticatedToken,
      HpcFileLocation fileLocation, boolean getSize) throws HpcException {
    throw new HpcException("getPathAttributes() not supported", HpcErrorType.UNEXPECTED_ERROR);
  }

  @Override
  public List<HpcDirectoryScanItem> scanDirectory(Object authenticatedToken,
      HpcFileLocation directoryLocation) throws HpcException {
    throw new HpcException("scanDirectory() not supported", HpcErrorType.UNEXPECTED_ERROR);

  }

  @Override
  public String getFileContainerName(Object authenticatedToken, String fileContainerId)
      throws HpcException {
    return fileContainerId;
  }

  // ---------------------------------------------------------------------//
  // Helper Methods
  // ---------------------------------------------------------------------//

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
}
