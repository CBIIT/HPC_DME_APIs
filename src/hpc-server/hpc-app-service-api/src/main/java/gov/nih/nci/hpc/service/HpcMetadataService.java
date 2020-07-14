/**
 * HpcMetadataService.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import java.io.File;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadMethod;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Metadata Application Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcMetadataService {
  /**
   * Add metadata to a collection.
   *
   * @param path The collection path.
   * @param metadataEntries The metadata entries to add.
   * @param configurationId The configuration to apply validation rules. Metadata validation rules
   *        are configuration specific.
   * @throws HpcException on service failure.
   */
  public void addMetadataToCollection(String path, List<HpcMetadataEntry> metadataEntries,
      String configurationId) throws HpcException;

  /**
   * Update a collection's metadata.
   *
   * @param path The collection path.
   * @param metadataEntries The metadata entries to update.
   * @param configurationId The configuration to apply validation rules. Metadata validation rules
   *        are configuration specific.
   * @throws HpcException on service failure.
   */
  public void updateCollectionMetadata(String path, List<HpcMetadataEntry> metadataEntries,
      String configurationId) throws HpcException;

  /**
   * Generate system metadata and attach to a collection. System generated metadata is: 1. UUID. 2.
   * Registrar user ID. 3. Registrar name. 4. Configuration ID.
   *
   * @param path The collection path.
   * @param userId The user ID.
   * @param userName The user name.
   * @param configurationId The configuration ID.
   * @return The system generated metadata.
   * @throws HpcException on service failure.
   */
  public HpcSystemGeneratedMetadata addSystemGeneratedMetadataToCollection(String path,
      String userId, String userName, String configurationId) throws HpcException;

  /**
   * Get the system generated metadata of a collection.
   *
   * @param path The collection path.
   * @return HpcSystemGeneratedMetadata The system generated metadata.
   * @throws HpcException on service failure.
   */
  public HpcSystemGeneratedMetadata getCollectionSystemGeneratedMetadata(String path)
      throws HpcException;

  /**
   * Get the system generated metadata object from a list of entries.
   *
   * @param systemGeneratedMetadataEntries The system generated metadata entries.
   * @return The system generated metadata.
   * @throws HpcException on service failure.
   */
  public HpcSystemGeneratedMetadata toSystemGeneratedMetadata(
      List<HpcMetadataEntry> systemGeneratedMetadataEntries) throws HpcException;

  /**
   * Filter out system generated metadata from a list of metadata entries
   *
   * @param metadataEntries The list of metadata entries.
   * @return A list of metadata entries that contains no system generated metadata
   */
  public List<HpcMetadataEntry> toUserProvidedMetadataEntries(
      List<HpcMetadataEntry> metadataEntries);

  /**
   * convert a list of metadata entries to Map&lt;attribute, value&gt;
   *
   * @param metadataEntries The list of metadata entries
   * @return A metadata attribute to value map.
   */
  public Map<String, String> toMap(List<HpcMetadataEntry> metadataEntries);

  /**
   * Get metadata of a collection.
   *
   * @param path The collection's path.
   * @return The collection's metadata entries.
   * @throws HpcException on service failure.
   */
  public HpcMetadataEntries getCollectionMetadataEntries(String path) throws HpcException;

  /**
   * Create (default) metadata entries for a file found in a directory scan.
   *
   * @param scanItem The directory scan item to generate the default metadata for.
   * @return The generated metadata entries
   */
  public List<HpcMetadataEntry> getDefaultDataObjectMetadataEntries(HpcDirectoryScanItem scanItem);

  /**
   * Get 'system' default metadata entries for collections.
   *
   * @return The system default collection metadata entries.
   */
  public List<HpcMetadataEntry> getDefaultCollectionMetadataEntries();

  /**
   * Add metadata to a data object.
   *
   * @param path The data object path.
   * @param metadataEntries The metadata entries to add.
   * @param configurationId The configuration to apply validation rules. Metadata validation rules
   *        are configuration specific.
   * @param collectionType The collection type containing the data object.
   * @return The object-id (system generated) metadata entry. Note - the metadata itself is not
   *         added to the object in iRODS. The actual addition of the object-id system metadata to
   *         iRODS is done by addSystemGeneratedMetadataToDataObject()
   * @throws HpcException on service failure.
   */
  public HpcMetadataEntry addMetadataToDataObject(String path,
      List<HpcMetadataEntry> metadataEntries, String configurationId, String collectionType)
      throws HpcException;

  /**
   * Extract metadata from a file and add to a data object.
   *
   * @param path The data object path.
   * @param dataObjectInputStream The file (input stream) to extract metadata from.
   * @param configurationId The configuration to apply validation rules. Metadata validation rules
   *        are configuration specific.
   * @param collectionType The collection type containing the data object.
   * @param closeInputStream If true, the method will close the input stream.
   * @throws HpcException on service failure.
   */
  public void addMetadataToDataObjectFromFile(String path, InputStream dataObjectInputStream,
      String configurationId, String collectionType, boolean closeInputStream) throws HpcException;

  /**
   * Extract metadata from a file and add to a data object.
   *
   * @param path The data object path.
   * @param dataObjectFile The file to extract metadata from.
   * @param configurationId The configuration to apply validation rules. Metadata validation rules
   *        are configuration specific.
   * @param collectionType The collection type containing the data object.
   * @param closeInputStream If true, the method will close the input stream.
   * @throws HpcException on service failure.
   */
  public void addMetadataToDataObjectFromFile(String path, File dataObjectFile,
      String configurationId, String collectionType, boolean closeInputStream) throws HpcException;

  /**
   * Add extracted metadata to a data object.
   *
   * @param path The data object path.
   * @param extractedMetadataEntries The extracted metadata (from the physical file) entries to add.
   * @param configurationId The configuration to apply validation rules. Metadata validation rules
   *        are configuration specific.
   * @param collectionType The collection type containing the data object.
   * @throws HpcException on service failure.
   */
  public void addExtractedMetadataToDataObject(String path,
      List<HpcMetadataEntry> extractedMetadataEntries, String configurationId,
      String collectionType) throws HpcException;

  /**
   * Generate system metadata and attach to the data object.
   *
   * @param path The data object path.
   * @param dataObjectIdMetadataEntry The object-id metadata entry (generated by
   *        addMetadataToDataObject())
   * @param archiveLocation The physical file archive location.
   * @param sourceLocation (Optional) The source location of the file.
   * @param dataTransferRequestId (Optional) The data transfer request ID.
   * @param dataTransferStatus The data transfer upload status.
   * @param dataTransferMethod The data transfer upload method.
   * @param dataTransferType The data transfer type.
   * @param dataTransferStarted The time data transfer started.
   * @param dataTransferCompleted (Optional) The time data transfer completed.
   * @param sourceSize (Optional) The data source size in bytes.
   * @param sourceURL (Optional) The data object source URL.
   * @param callerObjectId (Optional) The caller object ID.
   * @param userId The user ID.
   * @param userName The user name.
   * @param configurationId The data management configuration ID.
   * @param s3ArchiveConfigurationId (Optional) The S3 archive configuration ID.
   * @param registrationCompletionEvent If set to true, an event will be generated when registration
   *        is completed or failed.
   * @return The system generated metadata.
   * @throws HpcException on service failure.
   */
  public HpcSystemGeneratedMetadata addSystemGeneratedMetadataToDataObject(String path,
      HpcMetadataEntry dataObjectIdMetadataEntry, HpcFileLocation archiveLocation,
      HpcFileLocation sourceLocation, String dataTransferRequestId,
      HpcDataTransferUploadStatus dataTransferStatus,
      HpcDataTransferUploadMethod dataTransferMethod, HpcDataTransferType dataTransferType,
      Calendar dataTransferStarted, Calendar dataTransferCompleted, Long sourceSize,
      String sourceURL, String callerObjectId, String userId, String userName,
      String configurationId, String s3ArchiveConfigurationId, boolean registrationCompletionEvent)
      throws HpcException;

  /**
   * Generate system metadata and attach to the data object in a registration w/ link to another
   * data object
   *
   * @param path The data object path.
   * @param dataObjectIdMetadataEntry The object-id metadata entry (generated by
   *        addMetadataToDataObject())
   * @param userId The user ID.
   * @param userName The user name.
   * @param configurationId The data management configuration ID.
   * @param linkSourcePath linkSourcePath.
   * @throws HpcException on service failure.
   */
  public void addSystemGeneratedMetadataToDataObject(String path,
      HpcMetadataEntry dataObjectIdMetadataEntry, String userId, String userName,
      String configurationId, String linkSourcePath) throws HpcException;

  /**
   * Get the system generated metadata of a data object.
   *
   * @param path The data object path.
   * @return The system generated metadata.
   * @throws HpcException on service failure.
   */
  public HpcSystemGeneratedMetadata getDataObjectSystemGeneratedMetadata(String path)
      throws HpcException;

  /**
   * Update system generated metadata of a data object.
   *
   * @param path The data object path.
   * @param archiveLocation (Optional) The physical file archive location.
   * @param dataTransferRequestId (Optional) The data transfer request ID.
   * @param checksum (Optional) The data checksum.
   * @param dataTransferStatus (Optional) The data transfer upload status.
   * @param dataTransferType (Optional) The data transfer type.
   * @param dataTransferStarted (Optional) The time data transfer started.
   * @param dataTransferCompleted (Optional) The time data transfer completed.
   * @param sourceSize (Optional) The data source size in bytes.
   * @param linkSourcePath (Optional) linkSourcePath.
   * @throws HpcException on service failure.
   */
  public void updateDataObjectSystemGeneratedMetadata(String path, HpcFileLocation archiveLocation,
      String dataTransferRequestId, String checksum, HpcDataTransferUploadStatus dataTransferStatus,
      HpcDataTransferType dataTransferType, Calendar dataTransferStarted,
      Calendar dataTransferCompleted, Long sourceSize, String linkSourcePath) throws HpcException;

  /**
   * Update a data object's metadata.
   *
   * @param path The data object path.
   * @param metadataEntries The metadata entries to update.
   * @param configurationId The configuration to apply validation rules. Metadata validation rules
   *        are configuration specific.
   * @param collectionType The collection type containing the data object.
   * @throws HpcException on service failure.
   */
  public void updateDataObjectMetadata(String path, List<HpcMetadataEntry> metadataEntries,
      String configurationId, String collectionType) throws HpcException;

  /**
   * Get metadata of a data object.
   *
   * @param path The data object's path.
   * @return HpcMetadataEntries The data object's metadata entries.
   * @throws HpcException on service failure.
   */
  public HpcMetadataEntries getDataObjectMetadataEntries(String path) throws HpcException;

  /**
   * Refresh all metadata materialized views.
   *
   * @throws HpcException on service failure.
   */
  public void refreshViews() throws HpcException;

  /**
   * Get collection system metadata attribute names.
   *
   * @return A list of system metadata attribute names.
   * @throws HpcException on service failure.
   */
  public List<String> getCollectionSystemMetadataAttributeNames() throws HpcException;

  /**
   * Get data object system metadata attribute names.
   *
   * @return A list of system metadata attribute names.
   * @throws HpcException on service failure.
   */
  public List<String> getDataObjectSystemMetadataAttributeNames() throws HpcException;
}
