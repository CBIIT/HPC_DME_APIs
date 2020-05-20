package gov.nih.nci.hpc.integration.googledrive.impl;

import static gov.nih.nci.hpc.util.HpcUtil.toNormalizedPath;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.apache.cxf.common.util.StringUtils;
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
import com.google.api.services.drive.model.FileList;
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

  // Google drive search query for folders.
  private static final String FOLDER_QUERY =
      "mimeType = 'application/vnd.google-apps.folder' and name = '%s' and '%s' in parents";

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
        // Find / Create the folder in Google Drive where we download the file to
        String destinationPath = toNormalizedPath(
            downloadRequest.getGoogleDriveDestination().getDestinationLocation().getFileId());
        int lastSlashIndex = destinationPath.lastIndexOf('/');
        String destinationFolderPath = destinationPath.substring(0, lastSlashIndex);
        String destinationFileName = destinationPath.substring(lastSlashIndex + 1);
        String folderId = getFolderId(googleDrive, destinationFolderPath);

        // Transfer the file to Google Drive, and complete the download task.
        progressListener.transferCompleted(googleDrive.files().create(
            new File().setName(destinationFileName).setParents(Collections.singletonList(folderId)),
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

  // ---------------------------------------------------------------------//
  // Helper Methods
  // ---------------------------------------------------------------------//

  /**
   * Find the ID of a folder in Google Drive. If not found, the folder is created.
   *
   * @param googleDrive A Google drive instance.
   * @param folderPath The folder path to find / create in Google Drive.
   * @return The folder ID.
   * @throws IOException on data transfer system failure.
   */
  private String getFolderId(Drive googleDrive, String folderPath) throws IOException {
    String parentFolderId = "root";
    if (!StringUtils.isEmpty(folderPath)) {
      boolean createNewFolder = false;
      for (String subFolderName : folderPath.split("/")) {
        if(StringUtils.isEmpty(subFolderName)) {
          continue;
        }
        if (!createNewFolder) {
          // Search for the sub-folder.
          FileList result = googleDrive.files().list()
              .setQ(String.format(FOLDER_QUERY, subFolderName, parentFolderId))
              .setFields("files(id)").execute();
          if (!result.getFiles().isEmpty()) {
            // Sub-folder was found. Note: In Google Drive, it's possible to have multiple folders
            // with the same name
            // We simply grab the first one on the list.
            parentFolderId = result.getFiles().get(0).getId();
            continue;

          } else {
            // Sub-folder was not found.
            createNewFolder = true;
          }
        }

        // Creating a new sub folder.
        parentFolderId = googleDrive.files()
            .create(new File().setName(subFolderName)
                .setParents(Collections.singletonList(parentFolderId))
                .setMimeType("application/vnd.google-apps.folder"))
            .setFields("id").execute().getId();
      }
    }

    return parentFolderId;
  }
}
