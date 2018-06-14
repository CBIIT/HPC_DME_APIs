/**
 * HpcDataManagementBusService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus;

import java.io.File;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDeleteResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcFileSizeUpdateDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcPermsForCollectionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcRegistrationSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermsForCollectionsDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Management Business Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcDataManagementBusService {

  public static final String EXCEPTION_MESSAGE_TEMPLATE__PRESIGNED_URL_S3_ONLY =
    "Presigned URL for download is supported on S3 based destination archive" +
    " only.  Requested path is archived on a POSIX based file system: ";


  /**
   * Determine if path refers to collection or data file.
   *
   * @param path The path.
   * @return A boolean result that is true if path refers to collection
   *         or false if it refers to data file.
   * @throws HpcException on service failure.
   */
  public boolean interrogatePathRef(String path) throws HpcException;

  /**
   * Register a Collection.
   *
   * @param path The collection's path.
   * @param collectionRegistration A DTO containing a list of metadata entries to attach to the
   *     collection.
   * @return true if a new collection was registered, false if the collection already exists and its
   *     metadata got updated.
   * @throws HpcException on service failure.
   */
  public boolean registerCollection(
      String path, HpcCollectionRegistrationDTO collectionRegistration) throws HpcException;

  /**
   * Register a Collection. In this overloaded method, the user-id, user Name, and DOC are
   * explicitly provided.
   *
   * @param path The collection's path.
   * @param collectionRegistration A DTO containing a list of metadata entries to attach to the
   *     collection.
   * @param userId The registrar user-id.
   * @param userName The registrar name.
   * @param configurationId The data management configuration ID.
   * @return true if a new collection was registered, false if the collection already exists and its
   *     metadata got updated.
   * @throws HpcException on service failure.
   */
  public boolean registerCollection(
      String path,
      HpcCollectionRegistrationDTO collectionRegistration,
      String userId,
      String userName,
      String configurationId)
      throws HpcException;

  /**
   * Get Collection.
   *
   * @param path The collection's path.
   * @param list An indicator to list sub-collections and data-objects.
   * @return A Collection DTO.
   * @throws HpcException on service failure.
   */
  public HpcCollectionDTO getCollection(String path, Boolean list) throws HpcException;

  /**
   * Get Collection children. NO collection metadata will be returned
   *
   * @param path The collection's path.
   * @return A Collection DTO.
   * @throws HpcException on service failure.
   */
  public HpcCollectionDTO getCollectionChildren(String path) throws HpcException;

  /**
   * Download a collection tree.
   *
   * @param path The collection path.
   * @param downloadRequest The download request DTO.
   * @return Download Response DTO.
   * @throws HpcException on service failure.
   */
  public HpcCollectionDownloadResponseDTO downloadCollection(
      String path, HpcDownloadRequestDTO downloadRequest) throws HpcException;

  /**
   * Download data objects.
   *
   * @param downloadRequest The download request DTO.
   * @return Download Response DTO.
   * @throws HpcException on service failure.
   */
  public HpcBulkDataObjectDownloadResponseDTO downloadDataObjects(
      HpcBulkDataObjectDownloadRequestDTO downloadRequest) throws HpcException;

  /**
   * Get collection download task status.
   *
   * @param taskId The collection download task ID.
   * @return A collection download status DTO. Null if the task could not be found.
   */
  public HpcCollectionDownloadStatusDTO getCollectionDownloadStatus(String taskId)
      throws HpcException;

  /**
   * Delete collection.
   *
   * @param path The collection path.
   * @throws HpcException on service failure.
   */
  public void deleteCollection(String path, Boolean recursive) throws HpcException;

  /**
   * Get data objects download task status.
   *
   * @param taskId The download task ID.
   * @return A collection download status DTO. Null if the task could not be found.
   */
  public HpcCollectionDownloadStatusDTO getDataObjectsDownloadStatus(String taskId)
      throws HpcException;

  /**
   * Get download summary. Note: the summary is for the request invoker.
   *
   * @param page The requested results page.
   * @param totalCount If set to true, return the total count of completed tasks. All active tasks
   *     are always returned.
   * @return A summary of download tasks for the request invoker
   */
  public HpcDownloadSummaryDTO getDownloadSummary(int page, boolean totalCount) throws HpcException;

  /**
   * Set collection permissions.
   *
   * @param path The collection path.
   * @param collectionPermissionsRequest Request to set collection permissions.
   * @return Permissions request response.
   * @throws HpcException on service failure.
   */
  public HpcEntityPermissionsResponseDTO setCollectionPermissions(
      String path, HpcEntityPermissionsDTO collectionPermissionsRequest) throws HpcException;

  /**
   * Get collection permissions.
   *
   * @param path The path of the collection.
   * @return A list of users/groups and their permission on the collection.
   * @throws HpcException on service failure.
   */
  public HpcEntityPermissionsDTO getCollectionPermissions(String path) throws HpcException;

  /**
   * Get collection permission for a given user.
   *
   * @param path The path of the collection.
   * @param userId The user to get permissions for.
   * @return permission on the collection.
   * @throws HpcException on service failure.
   */
  public HpcUserPermissionDTO getCollectionPermission(String path, String userId)
      throws HpcException;

  /**
   * Given a user and some collection paths, get the user's permissions on those collections.
   *
   * @param collectionPaths The collections' paths.
   * @param userId The user of interest.
   * @return permissions of the user on the specified collections as <code>
   *     HpcUserPermsOnManyCollectionsDTO</code> instance.
   * @throws HpcException on service failure.
   */
  public HpcUserPermsForCollectionsDTO getUserPermissionsOnCollections(
      String[] collectionPaths, String userId) throws HpcException;

  /**
   * Given some collection paths, get all permissions on those collections across all users.
   *
   * <p>Note that there is no representation of a user having no permission on a collection. In
   * other words, that scenario is represented by nothing/void/nil.
   *
   * @param collectionPaths The collections' paths.
   * @return permissions on the specified collections encompassing all users, as a <code>
   *     HpcPermsForCollectionsDTO</code> instance.
   * @throws HpcException on service failure.
   */
  public HpcPermsForCollectionsDTO getAllPermissionsOnCollections(String[] collectionPaths)
      throws HpcException;

  /**
   * Register a Data object.
   *
   * @param path The data object's path.
   * @param dataObjectRegistration A DTO contains the metadata and data transfer locations.
   * @param dataObjectFile (Optional) The data object file. 2 options are available to upload the
   *     data - Specify a source in 'dataObjectRegistrationDTO' or provide this file. The caller is
   *     expected to provide one and only one option.
   * @return A DTO with an indicator whether the data object was registered and an upload URL if one
   *     was requested.
   * @throws HpcException on service failure.
   */
  public HpcDataObjectRegistrationResponseDTO registerDataObject(
      String path, HpcDataObjectRegistrationRequestDTO dataObjectRegistration, File dataObjectFile)
      throws HpcException;

  /**
   * Register a Data object. In this overloaded method, the user-id, user Name, and DOC are
   * explicitly provided.
   *
   * @param path The data object's path.
   * @param dataObjectRegistration A DTO contains the metadata and data transfer locations.
   * @param dataObjectFile (Optional) The data object file. 2 options are available to upload the
   *     data - Specify a source in 'dataObjectRegistrationDTO' or provide this file. The caller is
   *     expected to provide one and only one option.
   * @param userId The registrar user-id.
   * @param userName The registrar name.
   * @param configurationId The data management configuration ID.
   * @param registrationCompletionEvent If set to true, an event will be generated when registration
   *     is completed or failed.
   * @return A DTO with an indicator whether the data object was registered and an upload URL if one
   *     was requested.
   * @throws HpcException on service failure.
   */
  public HpcDataObjectRegistrationResponseDTO registerDataObject(
      String path,
      HpcDataObjectRegistrationRequestDTO dataObjectRegistration,
      File dataObjectFile,
      String userId,
      String userName,
      String configurationId,
      boolean registrationCompletionEvent)
      throws HpcException;

  /**
   * Bulk Data object registration.
   *
   * @param bulkDataObjectRegistrationRequest The bulk registration request.
   * @return A registration response DTO.
   * @throws HpcException on service failure.
   */
  public HpcBulkDataObjectRegistrationResponseDTO registerDataObjects(
      HpcBulkDataObjectRegistrationRequestDTO bulkDataObjectRegistrationRequest)
      throws HpcException;

  /**
   * Get data objects registration task status.
   *
   * @param taskId The registration task ID.
   * @return A data object list registration status DTO. Null if the task could not be found.
   */
  public HpcBulkDataObjectRegistrationStatusDTO getDataObjectsRegistrationStatus(String taskId)
      throws HpcException;

  /**
   * Get data objects registration summary. Note: the summary is for the request invoker.
   *
   * @param page The requested results page.
   * @param totalCount If set to true, return the total count of completed tasks. All active tasks
   *     are always returned.
   * @return A summary of registration tasks for the request invoker
   */
  public HpcRegistrationSummaryDTO getRegistrationSummary(int page, boolean totalCount)
      throws HpcException;

  /**
   * Get Data Object.
   *
   * @param path The data object's path.
   * @return A Data Object DTO.
   * @throws HpcException on service failure.
   */
  public HpcDataObjectDTO getDataObject(String path) throws HpcException;

  /**
   * Download Data Object. In this overloaded method, the request invoker will be notified (if
   * subscribed) when the download is complete. To specify a different user-id and turn off
   * notification, use the other overloaded method.
   *
   * @param path The data object path.
   * @param downloadRequest The download request DTO.
   * @return Download ResponseDTO
   * @throws HpcException on service failure.
   */
  public HpcDataObjectDownloadResponseDTO downloadDataObject(
      String path, HpcDownloadRequestDTO downloadRequest) throws HpcException;

  /**
   * Download Data Object.
   *
   * @param path The data object path.
   * @param downloadRequest The download request DTO.
   * @param userId The user submitting the request.
   * @param completionEvent If true, an event will be added when async download is complete.
   * @return Download ResponseDTO
   * @throws HpcException on service failure.
   */
  public HpcDataObjectDownloadResponseDTO downloadDataObject(
      String path, HpcDownloadRequestDTO downloadRequest, String userId, boolean completionEvent)
      throws HpcException;

  /**
   * Get Data object download task status.
   *
   * @param taskId The data object download task ID.
   * @return A data object download status DTO. Null if the task could not be found.
   */
  public HpcDataObjectDownloadStatusDTO getDataObjectDownloadStatus(String taskId)
      throws HpcException;

  /**
   * Delete Data Object.
   *
   * @param path The data object path.
   * @return A response DTO with detailed statuses.
   * @throws HpcException on service failure.
   */
  public HpcDataObjectDeleteResponseDTO deleteDataObject(String path) throws HpcException;

  /**
   * Set data object permissions.
   *
   * @param path The data object path.
   * @param dataObjectPermissionsRequest Request to set data object permissions.
   * @return Permissions request response.
   * @throws HpcException on service failure.
   */
  public HpcEntityPermissionsResponseDTO setDataObjectPermissions(
      String path, HpcEntityPermissionsDTO dataObjectPermissionsRequest) throws HpcException;

  /**
   * Get data object permissions.
   *
   * @param path The path of the data object.
   * @return A list of users/groups and their permission on the data object.
   * @throws HpcException on service failure.
   */
  public HpcEntityPermissionsDTO getDataObjectPermissions(String path) throws HpcException;

  /**
   * Get data object permission for user.
   *
   * @param path The path of the data object.
   * @param userId The user ID to get permission for.
   * @return permission on the data object.
   * @throws HpcException on service failure.
   */
  public HpcUserPermissionDTO getDataObjectPermission(String path, String userId)
      throws HpcException;

  /**
   * Get the Data Management Model (Metadata validation rules and hierarchy definitions)
   *
   * @return Data Management Model DTO.
   * @throws HpcException on service failure.
   */
  public HpcDataManagementModelDTO getDataManagementModel() throws HpcException;

  // TODO - Remove after file size was updated in prod
  public HpcFileSizeUpdateDTO updateFileSize(HpcFileSizeUpdateDTO fileSizeUpdate) throws HpcException;
}
