package gov.nih.nci.hpc.integration.googledrive.impl;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
  // Instance members
  // ---------------------------------------------------------------------//

  // The Google Drive download executor.
  @Autowired
  @Qualifier("hpcGoogleDriveDownloadExecutor")
  Executor googleDriveExecutor = null;

  // The (Google) HPC Application name. Users need to provide drive access to this app name.
  @Value("${hpc.integration.googledrive.hpcApplicationName}")
  String hpcApplicationName = null;

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
    // Input validation
    if (progressListener == null) {
      throw new HpcException(
          "[Google Drive] No progress listener provided for a download to Google Drive destination",
          HpcErrorType.UNEXPECTED_ERROR);
    }

    // Connect to Google Drive.
    Drive drive = null;
    try {
      drive = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(),
          JacksonFactory.getDefaultInstance(),
          new GoogleCredential()
              .setAccessToken(downloadRequest.getGoogleDriveDestination().getAccessToken()))
                  .setApplicationName(hpcApplicationName).build();

      // Confirm the drive is accessible.
      drive.about().get().setFields("appInstalled").execute();

    } catch (IOException | GeneralSecurityException e) {
      throw new HpcException("Failed to access Google Drive: " + e.getMessage(),
          HpcErrorType.INVALID_REQUEST_INPUT, e);
    }

    // Stream the file to Google Drive.
    final Drive googleDrive = drive;
    CompletableFuture<Void> googleDriveDownloadFuture = CompletableFuture.runAsync(() -> {
      try {
        progressListener.transferCompleted(googleDrive.files()
            .create(
                new File().setName(downloadRequest.getGoogleDriveDestination()
                    .getDestinationLocation().getFileId()),
                new InputStreamContent("application/octet-stream",
                    new URL(downloadRequest.getArchiveLocationURL()).openStream()))
            .setFields("size").execute().getSize());

      } catch (IOException e) {
        String message = "[GoogleDrive] Failed to download object: " + e.getMessage();
        logger.error(message, HpcErrorType.DATA_TRANSFER_ERROR, e);
        progressListener.transferFailed(message);
      }

    }, googleDriveExecutor);

    return String.valueOf(googleDriveDownloadFuture.hashCode());
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
}
