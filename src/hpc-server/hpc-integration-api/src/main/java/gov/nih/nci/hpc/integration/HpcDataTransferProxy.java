/**
 * HpcDataTransferProxy.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration;

import java.util.List;
import java.util.UUID;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcPermTempArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcArchiveDataTransferConfiguration;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
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
   * @param url The archive URL to connect to.
   * @return An authenticated token, to be used in subsequent calls to data transfer. It returns
   *     null if the account is not authenticated.
   * @throws HpcException on data transfer system failure.
   */
  public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount, String url)
      throws HpcException;

  /**
   * Authenticate a AWS S3 account.
   *
   * @param s3Account S3 account.
   * @return An authenticated token, to be used in subsequent calls to data transfer.
   * @throws HpcException on data transfer system failure.
   */
  public default Object authenticate(HpcS3Account s3Account) throws HpcException {
    throw new HpcException("authenticate(s3Account) not supported", HpcErrorType.UNEXPECTED_ERROR);
  }

  /**
   * Check if upload/download requests are accepted at the moment.
   *
   * @param authenticatedToken An authenticated token.
   * @return HpcTransferAcceptanceResponse for which: 1. the method canAcceptTransfer() returns true
   *     if upload/download requests are accepted or false if the data-transfer system is too busy.
   *     2. the method getQueueSize() returns int that is size of data transfer queue.
   * @throws HpcException on data transfer system failure.
   */
  public default HpcTransferAcceptanceResponse acceptsTransferRequests(Object authenticatedToken)
      throws HpcException {
    return new HpcTransferAcceptanceResponse() {};
  }

  /**
   * Upload a data object file.
   *
   * @param authenticatedToken An authenticated token.
   * @param uploadRequest The data upload request
   * @param baseArchiveDestination The archive's base destination location.
   * @param uploadRequestURLExpiration The expiration period (in days) to set when generating upload
   *     URL.
   * @param progressListener (Optional) a progress listener for async notification on transfer
   *     completion.
   * @return A data object upload response.
   * @throws HpcException on data transfer system failure.
   */
  public HpcDataObjectUploadResponse uploadDataObject(
      Object authenticatedToken,
      HpcDataObjectUploadRequest uploadRequest,
      HpcArchive baseArchiveDestination,
      Integer uploadRequestURLExpiration,
      HpcDataTransferProgressListener progressListener)
      throws HpcException;

  /**
   * Download a data object file.
   *
   * @param authenticatedToken An authenticated token.
   * @param downloadRequest The data object download request.
   * @param archiveDataTransferConfiguration The archive's data transfer configuration.
   * @param progressListener (Optional) a progress listener for async notification on transfer
   *     completion.
   * @return A data transfer request Id.
   * @throws HpcException on data transfer system failure.
   */
  public String downloadDataObject(
      Object authenticatedToken,
      HpcDataObjectDownloadRequest downloadRequest,
      HpcArchiveDataTransferConfiguration archiveDataTransferConfiguration,
      HpcDataTransferProgressListener progressListener)
      throws HpcException;

  /**
   * Generate a (pre-signed) download URL for a data object file.
   *
   * @param authenticatedToken An authenticated token.
   * @param archiveLocation The data object's archive location.
   * @param downloadRequestURLExpiration The expiration period (in days) to set when generating
   *     download URL.
   * @return The download URL
   * @throws HpcException on data transfer system failure.
   */
  public default String generateDownloadRequestURL(
      Object authenticatedToken,
      HpcFileLocation archiveLocation,
      Integer downloadRequestURLExpiration)
      throws HpcException {
    throw new HpcException(
        "Generating download URL is not supported", HpcErrorType.UNEXPECTED_ERROR);
  }

  /**
   * Copy a data object file.
   *
   * @param authenticatedToken An authenticated token.
   * @param sourceFile The source file.
   * @param destinationFile The destination file.
   * @param baseArchiveDestination The archive's base destination location.
   * @param metadataEntries The metadata to attach.
   * @return The copied object checksum.
   * @throws HpcException on data transfer system failure.
   */
  public String copyDataObject(
      Object authenticatedToken,
      HpcFileLocation sourceFile,
      HpcFileLocation destinationFile,
      HpcArchive baseArchiveDestination,
      List<HpcMetadataEntry> metadataEntries)
      throws HpcException;

  /**
   * Delete a data object file.
   *
   * @param authenticatedToken An authenticated token.
   * @param fileLocation The file location.
   * @param baseArchiveDestination The archive's base destination location.
   * @throws HpcException on data transfer system failure.
   */
  public void deleteDataObject(
      Object authenticatedToken, HpcFileLocation fileLocation, HpcArchive baseArchiveDestination)
      throws HpcException;

  /**
   * Get a data transfer upload request status.
   *
   * @param authenticatedToken An authenticated token.
   * @param dataTransferRequestId The data transfer request ID.
   * @param baseArchiveDestination The archive's base destination location.
   * @return The data transfer request status.
   * @throws HpcException on data transfer system failure.
   */
  public default HpcDataTransferUploadReport getDataTransferUploadStatus(
      Object authenticatedToken, String dataTransferRequestId, HpcArchive baseArchiveDestination)
      throws HpcException {
    throw new HpcException(
        "getDataTransferUploadStatus() not supported", HpcErrorType.UNEXPECTED_ERROR);
  }

  /**
   * Get a data transfer download request status.
   *
   * @param authenticatedToken An authenticated token.
   * @param dataTransferRequestId The data transfer request ID.
   * @return The data transfer request status.
   * @throws HpcException on data transfer system failure.
   */
  public default HpcDataTransferDownloadReport getDataTransferDownloadStatus(
      Object authenticatedToken, String dataTransferRequestId) throws HpcException {
    throw new HpcException(
        "getDataTransferDownloadStatus() not supported", HpcErrorType.UNEXPECTED_ERROR);
  }

  /**
   * Get attributes of a file/directory.
   *
   * @param authenticatedToken An authenticated token.
   * @param fileLocation The endpoint/path to check.
   * @param getSize If set to true, the file/directory size will be returned.
   * @return The path attributes.
   * @throws HpcException on data transfer system failure.
   */
  public HpcPathAttributes getPathAttributes(
      Object authenticatedToken, HpcFileLocation fileLocation, boolean getSize) throws HpcException;

  /**
   * Scan a directory (recursively) and return a list of all its files.
   *
   * @param authenticatedToken An authenticated token.
   * @param directoryLocation The endpoint/path to scan.
   * @return A list of files found.
   * @throws HpcException on data transfer system failure.
   */
  public List<HpcDirectoryScanItem> scanDirectory(
      Object authenticatedToken, HpcFileLocation directoryLocation) throws HpcException;

  /**
   * Get a file container name.
   *
   * @param authenticatedToken An authenticated token.
   * @param fileContainerId The file container ID.
   * @return The file container name.
   * @throws HpcException on data transfer system failure.
   */
  public String getFileContainerName(Object authenticatedToken, String fileContainerId)
      throws HpcException;

  /**
   * Calculate data transfer destination to deposit a data object.
   *
   * @param baseArchiveDestination The base (archive specific) destination.
   * @param path The data object (logical) path.
   * @param callerObjectId The caller's objectId.
   * @param permTempArchiveType The type of the archive.
   * @param unique If true, the a UUID will be appended to the end of the destination path ensuring
   *     it is unique. Otherwise, no UUID is appended
   * @return The calculated data transfer deposit destination.
   */
  public static HpcFileLocation getArchiveDestinationLocation(
      HpcFileLocation baseArchiveDestination,
      String path,
      String callerObjectId,
      HpcPermTempArchiveType permTempArchiveType,
      boolean unique) {
    // Calculate the data transfer destination absolute path as the following:
    StringBuilder destinationPath = new StringBuilder();
    destinationPath.append(baseArchiveDestination.getFileId());

    if (permTempArchiveType.equals(HpcPermTempArchiveType.ARCHIVE)) {
      // For Archive destination, destination path is:
      // 'base path' / 'caller's object-id / 'logical path'_'generated UUID' (note: generated UUID is optional)
      if (callerObjectId != null && !callerObjectId.isEmpty()) {
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
      if (unique) {
        destinationPath.append('_' + UUID.randomUUID().toString());
      }

    } else {
      // For Temporary Archive, destination path is:
      // 'base path' / generated UUID.
      destinationPath.append('/' + UUID.randomUUID().toString());
    }

    HpcFileLocation archiveDestination = new HpcFileLocation();
    archiveDestination.setFileContainerId(baseArchiveDestination.getFileContainerId());
    archiveDestination.setFileId(destinationPath.toString());

    return archiveDestination;
  }
}
