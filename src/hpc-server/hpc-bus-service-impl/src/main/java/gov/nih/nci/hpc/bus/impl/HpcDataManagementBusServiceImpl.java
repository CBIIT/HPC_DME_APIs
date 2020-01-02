/**
 * HpcDataManagementBusServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus.impl;

import static gov.nih.nci.hpc.util.HpcUtil.toNormalizedPath;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.common.util.concurrent.Striped;
import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.domain.datamanagement.HpcAuditRequestType;
import gov.nih.nci.hpc.domain.datamanagement.HpcBulkDataObjectRegistrationTaskStatus;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcDirectoryScanPathMap;
import gov.nih.nci.hpc.domain.datamanagement.HpcGroupPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermissionForCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermissionsForCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectType;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcPatternType;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3UploadSource;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcBulkMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcBulkMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationItem;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationResult;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationStatus;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationRequest;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkMoveRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkMoveResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDeleteResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDocDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcGroupPermissionResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMoveRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMoveResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcPermsForCollectionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermsForCollectionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationTaskDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcRegistrationSummaryDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementSecurityService;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcEventService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * HPC Data Management Business Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataManagementBusServiceImpl implements HpcDataManagementBusService {

  // ---------------------------------------------------------------------//
  // Constants
  // ---------------------------------------------------------------------//

  // A list of invalid characters in a path (not acceptable by IRODS).
  private static final List<String> INVALID_PATH_CHARACTERS = Arrays.asList("\\", ";", "?");

  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//
  // The Data Management Application Service Instance.
  @Autowired
  private HpcDataManagementService dataManagementService = null;

  // The Data Management Security Service Instance.
  @Autowired
  private HpcDataManagementSecurityService dataManagementSecurityService = null;

  // The Data Transfer Application Service Instance.
  @Autowired
  private HpcDataTransferService dataTransferService = null;

  // Security Application Service Instance.
  @Autowired
  private HpcSecurityService securityService = null;

  // The Metadata Application Service Instance.
  @Autowired
  private HpcMetadataService metadataService = null;

  // TheEvent Application Service Instance.
  @Autowired
  private HpcEventService eventService = null;

  // Locks to synchronize threads executing on path.
  private Striped<Lock> pathLocks = Striped.lock(127);

  // The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcDataManagementBusServiceImpl() {}

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  // ---------------------------------------------------------------------//
  // HpcDataManagementBusService Interface Implementation
  // ---------------------------------------------------------------------//

  @Override
  public boolean interrogatePathRef(String path) throws HpcException {
    return dataManagementService.interrogatePathRef(path);
  }

  @Override
  public boolean registerCollection(String path,
      HpcCollectionRegistrationDTO collectionRegistration) throws HpcException {
    // Determine the data management configuration to use based on the path.
    String configurationId = dataManagementService.findDataManagementConfigurationId(path);
    if (StringUtils.isEmpty(configurationId)) {
      throw new HpcException("Failed to determine data management configuration for: " + path,
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    HpcNciAccount invokerNciAccount = securityService.getRequestInvoker().getNciAccount();
    return registerCollection(path, collectionRegistration, invokerNciAccount.getUserId(),
        invokerNciAccount.getFirstName() + " " + invokerNciAccount.getLastName(), configurationId);
  }

  @Override
  public boolean registerCollection(String path,
      HpcCollectionRegistrationDTO collectionRegistration, String userId, String userName,
      String configurationId) throws HpcException {

    // Locking the path
    Lock lock = pathLocks.get(path);
    lock.lock();
    try {

      // Input validation.
      validatePath(path);

      if (collectionRegistration == null || collectionRegistration.getMetadataEntries().isEmpty()) {
        throw new HpcException("Null or empty metadata entries",
            HpcErrorType.INVALID_REQUEST_INPUT);
      }

      // Create parent collections if requested to.
      createParentCollections(path, collectionRegistration.getCreateParentCollections(),
          collectionRegistration.getParentCollectionsBulkMetadataEntries(), userId, userName,
          configurationId);

      // Create a collection directory.
      boolean created = dataManagementService.createDirectory(path);

      // Attach the metadata.
      if (created) {
        boolean registrationCompleted = false;
        try {
          // Assign system account as an additional owner of the collection.
          dataManagementService.setCoOwnership(path, userId);

          // Add user provided metadata.
          metadataService.addMetadataToCollection(path, collectionRegistration.getMetadataEntries(),
              configurationId);

          // Generate system metadata and attach to the collection.
          metadataService.addSystemGeneratedMetadataToCollection(path, userId, userName,
              configurationId);

          // Validate the collection hierarchy.
          securityService.executeAsSystemAccount(Optional.empty(),
              () -> dataManagementService.validateHierarchy(path, configurationId, false));

          // Add collection update event.
          addCollectionUpdatedEvent(path, true, false, userId);

          registrationCompleted = true;

        } finally {
          if (!registrationCompleted) {
            // Collection registration failed. Remove it from Data Management.
            dataManagementService.delete(path, true);
          }
        }

      } else {
        // Get the metadata for this collection.
        HpcMetadataEntries metadataBefore = metadataService.getCollectionMetadataEntries(path);
        HpcSystemGeneratedMetadata systemGeneratedMetadata =
            metadataService.toSystemGeneratedMetadata(metadataBefore.getSelfMetadataEntries());

        // Update the metadata.
        boolean updated = true;
        String message = null;
        try {
          metadataService.updateCollectionMetadata(path,
              collectionRegistration.getMetadataEntries(),
              systemGeneratedMetadata.getConfigurationId());

        } catch (HpcException e) {
          // Collection metadata update failed. Capture this in the audit record.
          updated = false;
          message = e.getMessage();
          throw (e);

        } finally {
          // Add an audit record of this deletion attempt.
          dataManagementService.addAuditRecord(path, HpcAuditRequestType.UPDATE_COLLECTION,
              metadataBefore, metadataService.getCollectionMetadataEntries(path), null, updated,
              null, message);
        }

        addCollectionUpdatedEvent(path, false, false, userId);
      }

      return created;

    } finally {
      lock.unlock();
    }
  }

  @Override
  public HpcCollectionDTO getCollection(String path, Boolean list, Boolean includeAcl)
      throws HpcException {
    // Input validation.
    if (path == null) {
      throw new HpcException("Null collection path", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    HpcCollection collection =
        dataManagementService.getCollection(path, list != null ? list : false);
    if (collection == null) {
      return null;
    }

    // Get the metadata.
    HpcMetadataEntries metadataEntries =
        metadataService.getCollectionMetadataEntries(collection.getAbsolutePath());

    HpcCollectionDTO collectionDTO = new HpcCollectionDTO();
    collectionDTO.setCollection(collection);
    if (metadataEntries != null && (!metadataEntries.getParentMetadataEntries().isEmpty()
        || !metadataEntries.getSelfMetadataEntries().isEmpty())) {
      collectionDTO.setMetadataEntries(metadataEntries);
    }

    // Set the permission if requested
    if (includeAcl) {
      collectionDTO
          .setPermission(dataManagementService.getCollectionPermission(path).getPermission());
    }

    return collectionDTO;
  }

  @Override
  public HpcCollectionDTO getCollectionChildren(String path) throws HpcException {
    // Input validation.
    if (path == null) {
      throw new HpcException("Null collection path", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    HpcCollection collection = dataManagementService.getCollectionChildren(path);
    if (collection == null) {
      return null;
    }

    HpcCollectionDTO collectionDTO = new HpcCollectionDTO();
    collectionDTO.setCollection(collection);

    return collectionDTO;
  }

  @Override
  public HpcCollectionDownloadResponseDTO downloadCollection(String path,
      HpcDownloadRequestDTO downloadRequest) throws HpcException {
    // Input validation.
    if (path == null || downloadRequest == null) {
      throw new HpcException("Null path or download request", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate collection exists.
    HpcCollection collection = dataManagementService.getCollection(path, true);
    if (collection == null) {
      throw new HpcException("Collection doesn't exist: " + path,
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Verify data objects found under this collection.
    if (!hasDataObjectsUnderPath(collection)) {
      // No data objects found under this collection.
      throw new HpcException("No data objects found under collection" + path,
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Get the System generated metadata.
    HpcSystemGeneratedMetadata metadata =
        metadataService.getCollectionSystemGeneratedMetadata(path);

    // Submit a collection download task.
    HpcCollectionDownloadTask collectionDownloadTask = dataTransferService.downloadCollection(path,
        downloadRequest.getGlobusDownloadDestination(), downloadRequest.getS3DownloadDestination(),
        securityService.getRequestInvoker().getNciAccount().getUserId(),
        metadata.getConfigurationId());

    // Create and return a DTO with the request receipt.
    HpcCollectionDownloadResponseDTO responseDTO = new HpcCollectionDownloadResponseDTO();
    responseDTO.setTaskId(collectionDownloadTask.getId());
    responseDTO.setDestinationLocation(collectionDownloadTask.getS3DownloadDestination() != null
        ? collectionDownloadTask.getS3DownloadDestination().getDestinationLocation()
        : collectionDownloadTask.getGlobusDownloadDestination().getDestinationLocation());

    return responseDTO;
  }

  @Override
  public HpcBulkDataObjectDownloadResponseDTO downloadDataObjectsOrCollections(
      HpcBulkDataObjectDownloadRequestDTO downloadRequest) throws HpcException {
    // Input validation.
    if (downloadRequest == null) {
      throw new HpcException("Null download request", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    if (downloadRequest.getDataObjectPaths().isEmpty()
        && downloadRequest.getCollectionPaths().isEmpty()) {
      throw new HpcException("No data object or collection paths",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }
    if (!downloadRequest.getDataObjectPaths().isEmpty()
        && !downloadRequest.getCollectionPaths().isEmpty()) {
      throw new HpcException("Both data object and collection paths provided",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }
    if (downloadRequest.getAppendPathToDownloadDestination() == null) {
      // Default to true - i.e. use the full data object path in the download
      // destination.
      downloadRequest.setAppendPathToDownloadDestination(true);
    }

    HpcCollectionDownloadTask collectionDownloadTask = null;
    if (!downloadRequest.getDataObjectPaths().isEmpty()) {
      // Submit a request to download a list of data objects.

      // Validate all data object paths requested exist.
      for (String path : downloadRequest.getDataObjectPaths()) {
        if (dataManagementService.getDataObject(path) == null) {
          throw new HpcException("Data object doesn't exist: " + path,
              HpcErrorType.INVALID_REQUEST_INPUT);
        }
      }

      // Get configuration ID of the first data object. It will be used to validate
      // the download destination.
      String configurationId = metadataService.getDataObjectSystemGeneratedMetadata(
          downloadRequest.getDataObjectPaths().iterator().next()).getConfigurationId();

      // Submit a data objects download task.
      collectionDownloadTask = dataTransferService.downloadDataObjects(
          downloadRequest.getDataObjectPaths(), downloadRequest.getGlobusDownloadDestination(),
          downloadRequest.getS3DownloadDestination(),
          securityService.getRequestInvoker().getNciAccount().getUserId(), configurationId,
          downloadRequest.getAppendPathToDownloadDestination());
    } else {
      // Submit a request to download a list of collections.

      // Validate all data object paths requested exist.
      boolean dataObjectExist = false;
      for (String path : downloadRequest.getCollectionPaths()) {
        HpcCollection collection = dataManagementService.getCollection(path, true);
        if (collection == null) {
          throw new HpcException("Collection doesn't exist: " + path,
              HpcErrorType.INVALID_REQUEST_INPUT);
        }
        // Verify at least one data object found under these collection.
        if (!dataObjectExist && hasDataObjectsUnderPath(collection)) {
          dataObjectExist = true;
        }
      }

      if (!dataObjectExist) {
        // No data objects found under the list of collection.
        throw new HpcException("No data objects found under collections",
            HpcErrorType.INVALID_REQUEST_INPUT);
      }

      // Get configuration ID of the first collection. It will be used to validate
      // the download destination.
      String configurationId = metadataService.getCollectionSystemGeneratedMetadata(
          downloadRequest.getCollectionPaths().iterator().next()).getConfigurationId();

      // Submit a data objects download task.
      collectionDownloadTask = dataTransferService.downloadCollections(
          downloadRequest.getCollectionPaths(), downloadRequest.getGlobusDownloadDestination(),
          downloadRequest.getS3DownloadDestination(),
          securityService.getRequestInvoker().getNciAccount().getUserId(), configurationId,
          downloadRequest.getAppendPathToDownloadDestination());
    }

    // Create and return a DTO with the request receipt.
    HpcBulkDataObjectDownloadResponseDTO responseDTO = new HpcBulkDataObjectDownloadResponseDTO();
    responseDTO.setTaskId(collectionDownloadTask.getId());
    responseDTO.setDestinationLocation(collectionDownloadTask.getS3DownloadDestination() != null
        ? collectionDownloadTask.getS3DownloadDestination().getDestinationLocation()
        : collectionDownloadTask.getGlobusDownloadDestination().getDestinationLocation());

    return responseDTO;
  }

  @Override
  public HpcCollectionDownloadStatusDTO getCollectionDownloadStatus(String taskId)
      throws HpcException {
    return getCollectionDownloadStatus(taskId, HpcDownloadTaskType.COLLECTION);
  }

  @Override
  public void deleteCollection(String path, Boolean recursive) throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(path)) {
      throw new HpcException("Null / empty path", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate the data object exists.
    HpcCollection collection = dataManagementService.getCollection(path, true);
    if (collection == null) {
      throw new HpcException("Collection doesn't exist: " + path,
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate the collection is empty if recursive flag is not set.
    if (!recursive
        && (!collection.getSubCollections().isEmpty() || !collection.getDataObjects().isEmpty())) {
      throw new HpcException("Collection is not empty: " + path,
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate the invoker is the owner of the data object.
    HpcPermission permission = dataManagementService.getCollectionPermission(path).getPermission();
    if (!permission.equals(HpcPermission.OWN)) {
      throw new HpcException(
          "Collection can only be deleted by its owner. Your permission: " + permission.value(),
          HpcRequestRejectReason.DATA_OBJECT_PERMISSION_DENIED);
    }

    // Get the metadata.
    HpcMetadataEntries metadataEntries =
        metadataService.getCollectionMetadataEntries(collection.getAbsolutePath());

    // If the invoker is a GroupAdmin, then ensure that for recursive delete:
    // 1. The file is less than 60 days old
    // 2. The invoker uploaded the data originally

    HpcRequestInvoker invoker = securityService.getRequestInvoker();
    if (recursive && HpcUserRole.GROUP_ADMIN.equals(invoker.getUserRole())) {

      Calendar cutOffDate = Calendar.getInstance();
      cutOffDate.add(Calendar.DAY_OF_YEAR, -60);
      if (collection.getCreatedAt().before(cutOffDate)) {
        String message = "The collection at " + path + " is not eligible for deletion";
        logger.error(message);
        throw new HpcException(message, HpcRequestRejectReason.NOT_AUTHORIZED);
      }

      HpcSystemGeneratedMetadata systemGeneratedMetadata =
          metadataService.toSystemGeneratedMetadata(metadataEntries.getSelfMetadataEntries());
      if (!invoker.getNciAccount().getUserId().equals(systemGeneratedMetadata.getRegistrarId())) {
        String message = "The collection at " + path + " can only be deleted by the creator";
        logger.error(message);
        throw new HpcException(message, HpcRequestRejectReason.DATA_OBJECT_PERMISSION_DENIED);
      }
    }

    // Delete the collection.

    boolean deleted = true;
    String message = null;

    try {
      if (recursive) {
        // Delete all the data objects in this hierarchy first
        deleteDataObjectsInCollections(path);
      }

      dataManagementService.delete(path, false);

    } catch (HpcException e) {
      // Collection deletion failed. Capture this in the audit record.
      deleted = false;
      message = e.getMessage();
      throw (e);

    } finally {
      // Add an audit record of this deletion attempt.
      dataManagementService.addAuditRecord(path, HpcAuditRequestType.DELETE_COLLECTION,
          metadataEntries, null, null, deleted, null, message);
    }
  }

  @Override
  public HpcCollectionDownloadStatusDTO getDataObjectsOrCollectionsDownloadStatus(String taskId)
      throws HpcException {
    HpcCollectionDownloadStatusDTO downloadStatus =
        getCollectionDownloadStatus(taskId, HpcDownloadTaskType.DATA_OBJECT_LIST);
    return downloadStatus != null ? downloadStatus
        : getCollectionDownloadStatus(taskId, HpcDownloadTaskType.COLLECTION_LIST);
  }

  @Override
  public HpcDownloadSummaryDTO getDownloadSummary(int page, boolean totalCount)
      throws HpcException {
    // Get the request invoker user-id.
    String userId = securityService.getRequestInvoker().getNciAccount().getUserId();

    // Populate the DTO with active and completed download requests for this user.
    HpcDownloadSummaryDTO downloadSummary = new HpcDownloadSummaryDTO();
    downloadSummary.getActiveTasks().addAll(dataTransferService.getDownloadRequests(userId));
    downloadSummary.getCompletedTasks()
        .addAll(dataTransferService.getDownloadResults(userId, page));

    int limit = dataTransferService.getDownloadResultsPageSize();
    downloadSummary.setPage(page);
    downloadSummary.setLimit(limit);

    if (totalCount) {
      int count = downloadSummary.getCompletedTasks().size();
      downloadSummary.setTotalCount((page == 1 && count < limit) ? count
          : dataTransferService.getDownloadResultsCount(userId));
    }

    return downloadSummary;
  }

  @Override
  public HpcEntityPermissionsResponseDTO setCollectionPermissions(String path,
      HpcEntityPermissionsDTO collectionPermissionsRequest) throws HpcException {
    // Input validation.
    if (path == null) {
      throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    validatePermissionsRequest(collectionPermissionsRequest);

    // Validate the collection exists.
    if (dataManagementService.getCollection(path, false) == null) {
      throw new HpcException("Collection doesn't exist: " + path,
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    HpcEntityPermissionsResponseDTO permissionsResponse = new HpcEntityPermissionsResponseDTO();

    // Execute all user permission requests for this collection.
    permissionsResponse.getUserPermissionResponses().addAll(
        setEntityPermissionForUsers(path, true, collectionPermissionsRequest.getUserPermissions()));

    // Execute all group permission requests for this collection.
    permissionsResponse.getGroupPermissionResponses().addAll(setEntityPermissionForGroups(path,
        true, collectionPermissionsRequest.getGroupPermissions()));

    return permissionsResponse;
  }

  @Override
  public HpcEntityPermissionsDTO getCollectionPermissions(String path) throws HpcException {
    // Input validation.
    if (path == null) {
      throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate the collection exists.
    if (dataManagementService.getCollection(path, false) == null) {
      return null;
    }

    return toEntityPermissionsDTO(dataManagementService.getCollectionPermissions(path));
  }

  @Override
  public HpcUserPermissionDTO getCollectionPermission(String path, String userId)
      throws HpcException {
    // Input validation.
    if (path == null) {
      throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    if (userId == null) {
      throw new HpcException("Null userId", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate the collection exists.
    if (dataManagementService.getCollection(path, false) == null) {
      return null;
    }
    HpcSubjectPermission permission = dataManagementService.getCollectionPermission(path, userId);

    return toUserPermissionDTO(permission);
  }

  @Override
  public HpcUserPermsForCollectionsDTO getUserPermissionsOnCollections(String[] collectionPaths,
      String userId) throws HpcException {
    HpcUserPermsForCollectionsDTO usrPrmsOnClltcns = null;
    if (dataManagementSecurityService.userExists(userId)) {
      usrPrmsOnClltcns = new HpcUserPermsForCollectionsDTO();
      usrPrmsOnClltcns.setUserId(userId);
      for (String someCollectionPath : collectionPaths) {
        HpcPermissionForCollection permForColl =
            fetchCollectionPermission(someCollectionPath, userId);
        if (null != permForColl) {
          usrPrmsOnClltcns.getPermissionsForCollections().add(permForColl);
        }
      }
    } else {
      throw new HpcException("User not found: " + userId,
          HpcRequestRejectReason.INVALID_NCI_ACCOUNT);
    }
    return usrPrmsOnClltcns;
  }

  @Override
  public HpcPermsForCollectionsDTO getAllPermissionsOnCollections(String[] collectionPaths)
      throws HpcException {
    HpcPermsForCollectionsDTO resultDto = new HpcPermsForCollectionsDTO();
    for (String somePath : collectionPaths) {
      HpcPermissionsForCollection perms4Coll = new HpcPermissionsForCollection();
      perms4Coll.setCollectionPath(somePath);
      perms4Coll.getCollectionPermissions()
          .addAll(dataManagementService.getCollectionPermissions(somePath));
      resultDto.getCollectionPermissions().add(perms4Coll);
    }

    return resultDto;
  }

  @Override
  public HpcDataObjectRegistrationResponseDTO registerDataObject(String path,
      HpcDataObjectRegistrationRequestDTO dataObjectRegistration, File dataObjectFile)
      throws HpcException {
    // Determine the data management configuration to use based on the path.
    String configurationId = dataManagementService.findDataManagementConfigurationId(path);
    if (StringUtils.isEmpty(configurationId)) {
      throw new HpcException("Failed to determine data management configuration for: " + path,
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    HpcNciAccount invokerNciAccount = securityService.getRequestInvoker().getNciAccount();
    return registerDataObject(path, dataObjectRegistration, dataObjectFile,
        invokerNciAccount.getUserId(),
        invokerNciAccount.getFirstName() + " " + invokerNciAccount.getLastName(), configurationId,
        true);
  }

  @Override
  public HpcDataObjectRegistrationResponseDTO registerDataObject(String path,
      HpcDataObjectRegistrationRequestDTO dataObjectRegistration, File dataObjectFile,
      String userId, String userName, String configurationId, boolean registrationCompletionEvent)
      throws HpcException {

    // Input validation.
    validatePath(path);

    if (dataObjectRegistration == null) {
      throw new HpcException("Null dataObjectRegistrationDTO", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Checksum validation (if requested by caller).
    validateChecksum(dataObjectFile, dataObjectRegistration.getChecksum());

    // Create parent collections if requested to.
    createParentCollections(path, dataObjectRegistration.getCreateParentCollections(),
        dataObjectRegistration.getParentCollectionsBulkMetadataEntries(), userId, userName,
        configurationId);

    // Create a data object file (in the data management system).
    HpcDataObjectRegistrationResponseDTO responseDTO = new HpcDataObjectRegistrationResponseDTO();
    responseDTO.setRegistered(dataManagementService.createFile(path));

    // Get the collection type containing the data object.
    String collectionPath = path.substring(0, path.lastIndexOf('/'));
    String collectionType = dataManagementService.getCollectionType(collectionPath);

    // Generate upload URL is defaulted to false.
    boolean generateUploadRequestURL =
        Optional.ofNullable(dataObjectRegistration.getGenerateUploadRequestURL()).orElse(false);

    if (responseDTO.getRegistered()) {
      boolean registrationCompleted = false;
      try {
        // Assign system account as an additional owner of the data-object.
        dataManagementService.setCoOwnership(path, userId);

        // Validate the new data object complies with the hierarchy definition.
        securityService.executeAsSystemAccount(Optional.empty(),
            () -> dataManagementService.validateHierarchy(collectionPath, configurationId, true));

        if (!StringUtils.isEmpty(dataObjectRegistration.getLinkSourcePath())) {
          // This is a registration request w/ link to an existing data object. no data upload is
          // performed.
          linkDataObject(path, dataObjectRegistration, dataObjectFile, userId, userName,
              configurationId, collectionType);

        } else {
          // This is a registration request w/ data upload.

          // Attach the user provided metadata.
          metadataService.addMetadataToDataObject(path, dataObjectRegistration.getMetadataEntries(),
              configurationId, collectionType);

          // Transfer the data file.
          HpcDataObjectUploadResponse uploadResponse = dataTransferService.uploadDataObject(
              dataObjectRegistration.getGlobusUploadSource(),
              dataObjectRegistration.getS3UploadSource(), dataObjectFile, generateUploadRequestURL,
              generateUploadRequestURL ? dataObjectRegistration.getChecksum() : null, path, userId,
              dataObjectRegistration.getCallerObjectId(), configurationId);

          // Set the upload request URL (if one was generated).
          responseDTO.setUploadRequestURL(uploadResponse.getUploadRequestURL());

          // Generate data management (iRODS) system metadata and attach to the data
          // object.
          HpcSystemGeneratedMetadata systemGeneratedMetadata =
              metadataService.addSystemGeneratedMetadataToDataObject(path,
                  uploadResponse.getArchiveLocation(), uploadResponse.getUploadSource(),
                  uploadResponse.getDataTransferRequestId(), uploadResponse.getDataTransferStatus(),
                  uploadResponse.getDataTransferType(), uploadResponse.getDataTransferStarted(),
                  uploadResponse.getDataTransferCompleted(), uploadResponse.getSourceSize(),
                  uploadResponse.getSourceURL(), dataObjectRegistration.getCallerObjectId(), userId,
                  userName, configurationId, dataManagementService
                      .getDataManagementConfiguration(configurationId).getS3UploadConfigurationId(),
                  registrationCompletionEvent);

          // Generate archive (Cleversafe) system generated metadata. Note: This is only
          // performed for synchronous data registration.
          if (dataObjectFile != null) {
            // transfered to the archive (Cleversafe), i.e. it is a synchronous data
            // registration.
            String checksum = dataTransferService.addSystemGeneratedMetadataToDataObject(
                systemGeneratedMetadata.getArchiveLocation(),
                systemGeneratedMetadata.getDataTransferType(),
                systemGeneratedMetadata.getConfigurationId(),
                systemGeneratedMetadata.getS3ArchiveConfigurationId(),
                systemGeneratedMetadata.getObjectId(), systemGeneratedMetadata.getRegistrarId());

            metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, checksum,
                null, null, null, null, null);
          }

        }

        // Add collection update event.
        addCollectionUpdatedEvent(path, false, true, userId);

        registrationCompleted = true;

      } finally {
        if (!registrationCompleted) {
          // Data object registration failed. Remove it from Data Management.
          dataManagementService.delete(path, true);
        }
      }

    } else {
      // The data object is already registered. Validate that data or data endpoint
      // is not attached to the request.
      if (dataObjectFile != null || dataObjectRegistration.getGlobusUploadSource() != null
          || dataObjectRegistration.getS3UploadSource() != null) {
        throw new HpcException(
            "A data file by that name already exists in this collection. Only updating metadata is allowed.",
            HpcErrorType.REQUEST_REJECTED);
      }

      // Update metadata and optionally re-generate upload URL (if data was not
      // uploaded yet).
      responseDTO
          .setUploadRequestURL(updateDataObject(path, dataObjectRegistration.getMetadataEntries(),
              collectionType, generateUploadRequestURL, dataObjectRegistration.getChecksum(),
              userId, dataObjectRegistration.getCallerObjectId()));
    }

    return responseDTO;
  }

  @Override
  public HpcBulkDataObjectRegistrationResponseDTO registerDataObjects(
      HpcBulkDataObjectRegistrationRequestDTO bulkDataObjectRegistrationRequest)
      throws HpcException {
    // Input validation.
    if (bulkDataObjectRegistrationRequest == null
        || (bulkDataObjectRegistrationRequest.getDataObjectRegistrationItems().isEmpty()
            && bulkDataObjectRegistrationRequest.getDirectoryScanRegistrationItems().isEmpty())) {
      throw new HpcException("Null / Empty registration request",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Scan all directories in the registration request and create individual data
    // object registration
    // requests for all files found. Add these individual data object requests to
    // the list provided by the caller.
    bulkDataObjectRegistrationRequest.getDataObjectRegistrationItems()
        .addAll(toDataObjectRegistrationItems(
            bulkDataObjectRegistrationRequest.getDirectoryScanRegistrationItems()));

    // Normalize the path of the data object registration items (i.e. remove
    // redundant '/', etc).
    bulkDataObjectRegistrationRequest.getDataObjectRegistrationItems()
        .forEach(item -> item.setPath(toNormalizedPath(item.getPath())));

    // If dry-run was requested, simply return the entire list of individual data
    // object registrations.
    // This is so that caller can see the result of the directories scan.
    HpcBulkDataObjectRegistrationResponseDTO responseDTO =
        new HpcBulkDataObjectRegistrationResponseDTO();
    if (bulkDataObjectRegistrationRequest.getDryRun() != null
        && bulkDataObjectRegistrationRequest.getDryRun()) {
      responseDTO.getDataObjectRegistrationItems()
          .addAll(bulkDataObjectRegistrationRequest.getDataObjectRegistrationItems());
      return responseDTO;
    }

    validateDataObjectRegistrationDestinationPaths(
        bulkDataObjectRegistrationRequest.getDataObjectRegistrationItems());

    // Break the DTO into a map of registration requests and ensure no duplication
    // of registration paths.
    Map<String, HpcDataObjectRegistrationRequest> dataObjectRegistrationRequests = new HashMap<>();
    for (HpcDataObjectRegistrationItemDTO dataObjectRegistrationItem : bulkDataObjectRegistrationRequest
        .getDataObjectRegistrationItems()) {
      HpcDataObjectRegistrationRequest dataObjectRegistrationRequest =
          new HpcDataObjectRegistrationRequest();
      dataObjectRegistrationRequest
          .setCallerObjectId(dataObjectRegistrationItem.getCallerObjectId());
      dataObjectRegistrationRequest
          .setCreateParentCollections(dataObjectRegistrationItem.getCreateParentCollections());
      dataObjectRegistrationRequest
          .setGlobusUploadSource(dataObjectRegistrationItem.getGlobusUploadSource());
      dataObjectRegistrationRequest
          .setS3UploadSource(dataObjectRegistrationItem.getS3UploadSource());
      dataObjectRegistrationRequest.getMetadataEntries()
          .addAll(dataObjectRegistrationItem.getDataObjectMetadataEntries());
      dataObjectRegistrationRequest.setParentCollectionsBulkMetadataEntries(
          dataObjectRegistrationItem.getParentCollectionsBulkMetadataEntries());

      String path = dataObjectRegistrationItem.getPath();
      if (StringUtils.isEmpty(path)) {
        throw new HpcException("Null / Empty path in registration request",
            HpcErrorType.INVALID_REQUEST_INPUT);
      }

      // Validate no multiple registration requests for the same path.
      if (dataObjectRegistrationRequests.put(path, dataObjectRegistrationRequest) != null) {
        throw new HpcException("Duplicated path in registration requests list: " + path,
            HpcErrorType.INVALID_REQUEST_INPUT);
      }
    }

    HpcNciAccount invokerNciAccount = securityService.getRequestInvoker().getNciAccount();

    // Submit a data objects registration task and return a response DTO.
    responseDTO.setTaskId(dataManagementService.registerDataObjects(invokerNciAccount.getUserId(),
        bulkDataObjectRegistrationRequest.getUiURL(), dataObjectRegistrationRequests));

    return responseDTO;
  }

  @Override
  public HpcBulkDataObjectRegistrationStatusDTO getDataObjectsRegistrationStatus(String taskId)
      throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(taskId)) {
      throw new HpcException("Null / Empty registration task ID",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Get the bulk registration task status.
    HpcBulkDataObjectRegistrationStatus taskStatus =
        dataManagementService.getBulkDataObjectRegistrationTaskStatus(taskId);
    if (taskStatus == null) {
      return null;
    }

    // Map the task status to DTO.
    HpcBulkDataObjectRegistrationStatusDTO registrationStatus =
        new HpcBulkDataObjectRegistrationStatusDTO();
    registrationStatus.setInProgress(taskStatus.getInProgress());
    if (taskStatus.getInProgress()) {
      // Registration in progress. Populate the DTO accordingly.
      registrationStatus.setTask(toBulkDataObjectRegistrationTaskDTO(taskStatus.getTask()));

    } else {
      // Download completed or failed. Populate the DTO accordingly.
      registrationStatus.setTask(toBulkDataObjectRegistrationTaskDTO(taskStatus.getResult()));
    }

    return registrationStatus;
  }

  @Override
  public HpcRegistrationSummaryDTO getRegistrationSummary(int page, boolean totalCount)
      throws HpcException {
    // Get the request invoker user-id.
    String userId = securityService.getRequestInvoker().getNciAccount().getUserId();

    // Populate the DTO with active and completed registration requests for this
    // user.
    HpcRegistrationSummaryDTO registrationSummary = new HpcRegistrationSummaryDTO();
    dataManagementService.getRegistrationTasks(userId).forEach(task -> registrationSummary
        .getActiveTasks().add(toBulkDataObjectRegistrationTaskDTO(task)));
    dataManagementService.getRegistrationResults(userId, page).forEach(result -> registrationSummary
        .getCompletedTasks().add(toBulkDataObjectRegistrationTaskDTO(result)));

    int limit = dataManagementService.getRegistrationResultsPageSize();
    registrationSummary.setPage(page);
    registrationSummary.setLimit(limit);

    if (totalCount) {
      int count = registrationSummary.getCompletedTasks().size();
      registrationSummary.setTotalCount((page == 1 && count < limit) ? count
          : dataManagementService.getRegistrationResultsCount(userId));
    }

    return registrationSummary;
  }

  @Override
  public HpcDataObjectDTO getDataObject(String path, Boolean includeAcl) throws HpcException {
    // Input validation.
    if (path == null) {
      throw new HpcException("Null data object path", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Get the data object.
    HpcDataObject dataObject = dataManagementService.getDataObject(path);
    if (dataObject == null) {
      return null;
    }

    // Get the metadata for this data object.
    HpcMetadataEntries metadataEntries =
        metadataService.getDataObjectMetadataEntries(dataObject.getAbsolutePath());
    HpcDataObjectDTO dataObjectDTO = new HpcDataObjectDTO();
    dataObjectDTO.setDataObject(dataObject);
    dataObjectDTO.setMetadataEntries(metadataEntries);
    dataObjectDTO.setPercentComplete(dataTransferService.calculateDataObjectUploadPercentComplete(
        metadataService.toSystemGeneratedMetadata(metadataEntries.getSelfMetadataEntries())));

    if (includeAcl) {
      // Set the permission
      dataObjectDTO
          .setPermission(dataManagementService.getDataObjectPermission(path).getPermission());
    }

    return dataObjectDTO;
  }

  @Override
  public HpcDataObjectDownloadResponseDTO downloadDataObject(String path,
      HpcDownloadRequestDTO downloadRequest) throws HpcException {
    return downloadDataObject(path, downloadRequest,
        securityService.getRequestInvoker().getNciAccount().getUserId(), true);
  }

  /**
   * Download Data Object.
   *
   * @param path The data object path.
   * @param downloadRequest The download request DTO.
   * @param completionEvent If true, an event will be added when async download is complete.
   * @param userId The user submitting the request.
   * @return Download ResponseDTO
   * @throws HpcException on service failure.
   */
  @Override
  public HpcDataObjectDownloadResponseDTO downloadDataObject(String path,
      HpcDownloadRequestDTO downloadRequest, String userId, boolean completionEvent)
      throws HpcException {
    // Input validation.
    if (downloadRequest == null) {
      throw new HpcException("Null download request", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate the following:
    // 1. Path is not empty.
    // 2. Data Object exists.
    // 3. Download to S3 destination is supported only from Cleversafe archive (i.e.
    // not POSIX).
    // 4. Download to Google Drive destination is supported only from Cleversafe
    // archive (i.e. not
    // POSIX).
    // 5. Data Object is archived (i.e. registration completed).
    HpcSystemGeneratedMetadata metadata =
        validateDataObjectDownloadRequest(path, downloadRequest.getS3DownloadDestination() != null,
            downloadRequest.getGoogleDriveDownloadDestination() != null);

    // Download the data object.
    HpcDataObjectDownloadResponse downloadResponse = dataTransferService.downloadDataObject(path,
        metadata.getArchiveLocation(), downloadRequest.getGlobusDownloadDestination(),
        downloadRequest.getS3DownloadDestination(),
        downloadRequest.getGoogleDriveDownloadDestination(),
        downloadRequest.getSynchronousDownloadFilter(), metadata.getDataTransferType(),
        metadata.getConfigurationId(), metadata.getS3ArchiveConfigurationId(), userId,
        completionEvent, metadata.getSourceSize() != null ? metadata.getSourceSize() : 0);

    // Construct and return a DTO.
    return toDownloadResponseDTO(downloadResponse.getDestinationLocation(),
        downloadResponse.getDestinationFile(), downloadResponse.getDownloadTaskId(), null,
        metadata.getDataTransferType().value());
  }

  @Override
  public HpcDataObjectDownloadStatusDTO getDataObjectDownloadStatus(String taskId)
      throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(taskId)) {
      throw new HpcException("Null / Empty data object download task ID",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Get the data object download task status.
    HpcDownloadTaskStatus taskStatus =
        dataTransferService.getDownloadTaskStatus(taskId, HpcDownloadTaskType.DATA_OBJECT);
    if (taskStatus == null) {
      return null;
    }

    // Map the task status to DTO.
    HpcDataObjectDownloadStatusDTO downloadStatus = new HpcDataObjectDownloadStatusDTO();
    downloadStatus.setInProgress(taskStatus.getInProgress());
    if (taskStatus.getInProgress()) {
      // Download in progress. Populate the DTO accordingly.
      downloadStatus.setPath(taskStatus.getDataObjectDownloadTask().getPath());
      downloadStatus.setCreated(taskStatus.getDataObjectDownloadTask().getCreated());
      downloadStatus.setDataTransferRequestId(
          taskStatus.getDataObjectDownloadTask().getDataTransferRequestId());
      downloadStatus
          .setDataTransferType(taskStatus.getDataObjectDownloadTask().getDataTransferType());
      downloadStatus.setDestinationLocation(
          taskStatus.getDataObjectDownloadTask().getGlobusDownloadDestination() != null
              ? taskStatus.getDataObjectDownloadTask().getGlobusDownloadDestination()
                  .getDestinationLocation()
              : taskStatus.getDataObjectDownloadTask().getS3DownloadDestination()
                  .getDestinationLocation());
      downloadStatus
          .setDestinationType(taskStatus.getDataObjectDownloadTask().getDestinationType());
      downloadStatus
          .setPercentComplete(taskStatus.getDataObjectDownloadTask().getPercentComplete());
      downloadStatus.setSize(taskStatus.getDataObjectDownloadTask().getSize());

    } else {
      // Download completed or failed. Populate the DTO accordingly.
      downloadStatus.setPath(taskStatus.getResult().getPath());
      downloadStatus.setCreated(taskStatus.getResult().getCreated());
      downloadStatus.setDataTransferRequestId(taskStatus.getResult().getDataTransferRequestId());
      downloadStatus.setDataTransferType(taskStatus.getResult().getDataTransferType());
      downloadStatus.setDestinationLocation(taskStatus.getResult().getDestinationLocation());
      downloadStatus.setDestinationType(taskStatus.getResult().getDestinationType());
      downloadStatus.setCompleted(taskStatus.getResult().getCompleted());
      downloadStatus.setMessage(taskStatus.getResult().getMessage());
      downloadStatus.setResult(taskStatus.getResult().getResult());
      downloadStatus
          .setEffectiveTrasnsferSpeed(taskStatus.getResult().getEffectiveTransferSpeed() > 0
              ? taskStatus.getResult().getEffectiveTransferSpeed()
              : null);
      downloadStatus.setSize(taskStatus.getResult().getSize());
    }

    return downloadStatus;
  }

  @Override
  public HpcDataObjectDownloadResponseDTO generateDownloadRequestURL(String path)
      throws HpcException {
    // Validate the following:
    // 1. Path is not empty.
    // 2. Data Object exists.
    // 3. Download to S3 destination is supported only from Cleversafe archive (i.e.
    // not POSIX).
    // 4. Data Object is archived (i.e. registration completed).
    HpcSystemGeneratedMetadata metadata = validateDataObjectDownloadRequest(path, true, false);

    // Generate a download URL for the data object, and return it in a DTO.
    return toDownloadResponseDTO(null, null, null,
        dataTransferService.generateDownloadRequestURL(path, metadata.getArchiveLocation(),
            metadata.getDataTransferType(), metadata.getConfigurationId(),
            metadata.getS3ArchiveConfigurationId()),
        metadata.getDataTransferType().value());
  }

  @Override
  public HpcDataObjectDeleteResponseDTO deleteDataObject(String path) throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(path)) {
      throw new HpcException("Null / empty path", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    HpcDataObject dataObject = dataManagementService.getDataObject(path);

    // Validate the data object exists.
    if (dataObject == null) {
      throw new HpcException("Data object doesn't exist: " + path,
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Get the metadata for this data object.
    HpcMetadataEntries metadataEntries = metadataService.getDataObjectMetadataEntries(path);
    HpcSystemGeneratedMetadata systemGeneratedMetadata =
        metadataService.toSystemGeneratedMetadata(metadataEntries.getSelfMetadataEntries());
    if (systemGeneratedMetadata.getDataTransferStatus() == null) {
      throw new HpcException("Unknown data transfer status", HpcErrorType.UNEXPECTED_ERROR);
    }

    // Validate the invoker is the owner of the data object.
    HpcPermission permission = dataManagementService.getDataObjectPermission(path).getPermission();
    if (!permission.equals(HpcPermission.OWN)) {
      throw new HpcException(
          "Data object can only be deleted by its owner. Your permission: " + permission.value(),
          HpcRequestRejectReason.DATA_OBJECT_PERMISSION_DENIED);
    }

    // If this is a GroupAdmin, then ensure that:
    // 1. The file is less than 60 days old
    // 2. The invoker uploaded the data originally

    HpcRequestInvoker invoker = securityService.getRequestInvoker();
    if (HpcUserRole.GROUP_ADMIN.equals(invoker.getUserRole())) {

      Calendar cutOffDate = Calendar.getInstance();
      cutOffDate.add(Calendar.DAY_OF_YEAR, -60);
      if (dataObject.getCreatedAt().before(cutOffDate)) {
        String message = "The data object at " + path + " is not eligible for deletion";
        logger.error(message);
        throw new HpcException(message, HpcRequestRejectReason.NOT_AUTHORIZED);
      }

      if (!invoker.getNciAccount().getUserId().equals(systemGeneratedMetadata.getRegistrarId())) {
        String message = "The data object at " + path + " can only be deleted by the data uploader";
        logger.error(message);
        throw new HpcException(message, HpcRequestRejectReason.DATA_OBJECT_PERMISSION_DENIED);
      }
    }

    // Instantiate a response DTO
    HpcDataObjectDeleteResponseDTO dataObjectDeleteResponse = new HpcDataObjectDeleteResponseDTO();
    dataObjectDeleteResponse.setArchiveDeleteStatus(true);
    dataObjectDeleteResponse.setDataManagementDeleteStatus(true);

    // Delete the file from the archive (if it's archived).
    switch (systemGeneratedMetadata.getDataTransferStatus()) {
      case ARCHIVED:
      case DELETE_REQUESTED:
      case DELETE_FAILED:
        deleteDataObjectFromArchive(path, systemGeneratedMetadata, dataObjectDeleteResponse);
        break;

      case RECEIVED:
      case IN_PROGRESS_TO_TEMPORARY_ARCHIVE:
      case IN_TEMPORARY_ARCHIVE:
      case IN_PROGRESS_TO_ARCHIVE:
      case URL_GENERATED:
        // Data transfer still in progress.
        throw new HpcException(
            "Object is not in archived state yet. It is in "
                + systemGeneratedMetadata.getDataTransferStatus().value() + " state",
            HpcRequestRejectReason.FILE_NOT_ARCHIVED);

      default:
        // The file is not in archive (data transfer failed, URL expired or it was
        // deleted).
        break;
    }

    // If the archive removal was successful, then remove the file from data
    // management.
    try {
      if (dataObjectDeleteResponse.getArchiveDeleteStatus()) {
        dataManagementService.delete(path, false);
      } else {
        dataObjectDeleteResponse.setDataManagementDeleteStatus(false);
      }

    } catch (HpcException e) {
      logger.error("Failed to delete file from datamanagement", e);
      dataObjectDeleteResponse.setDataManagementDeleteStatus(false);
      dataObjectDeleteResponse.setMessage(e.getMessage());
    }

    // Add an audit record of this deletion attempt.
    dataManagementService.addAuditRecord(path, HpcAuditRequestType.DELETE_DATA_OBJECT,
        metadataEntries, null, systemGeneratedMetadata.getArchiveLocation(),
        dataObjectDeleteResponse.getDataManagementDeleteStatus(),
        dataObjectDeleteResponse.getArchiveDeleteStatus(), dataObjectDeleteResponse.getMessage());

    return dataObjectDeleteResponse;
  }

  @Override
  public HpcEntityPermissionsResponseDTO setDataObjectPermissions(String path,
      HpcEntityPermissionsDTO dataObjectPermissionsRequest) throws HpcException {
    // Input Validation.
    if (path == null) {
      throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    validatePermissionsRequest(dataObjectPermissionsRequest);

    // Validate the data object exists.
    if (dataManagementService.getDataObject(path) == null) {
      throw new HpcException("Data object doesn't exist: " + path,
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    HpcEntityPermissionsResponseDTO permissionsResponse = new HpcEntityPermissionsResponseDTO();

    // Execute all user permission requests for this data object.
    permissionsResponse.getUserPermissionResponses().addAll(setEntityPermissionForUsers(path, false,
        dataObjectPermissionsRequest.getUserPermissions()));

    // Execute all group permission requests for this data object.
    permissionsResponse.getGroupPermissionResponses().addAll(setEntityPermissionForGroups(path,
        false, dataObjectPermissionsRequest.getGroupPermissions()));

    return permissionsResponse;
  }

  @Override
  public HpcEntityPermissionsDTO getDataObjectPermissions(String path) throws HpcException {
    // Input Validation.
    if (path == null) {
      throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate the data object exists.
    if (dataManagementService.getDataObject(path) == null) {
      return null;
    }

    return toEntityPermissionsDTO(dataManagementService.getDataObjectPermissions(path));
  }

  @Override
  public HpcUserPermissionDTO getDataObjectPermission(String path, String userId)
      throws HpcException {
    // Input validation.
    if (path == null) {
      throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    if (userId == null) {
      throw new HpcException("Null userId", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate the collection exists.
    if (dataManagementService.getDataObject(path) == null) {
      return null;
    }
    HpcSubjectPermission permission = dataManagementService.getDataObjectPermission(path, userId);

    return toUserPermissionDTO(permission);
  }

  @Override
  public HpcDataManagementModelDTO getDataManagementModels() throws HpcException {
    // Create a map DOC -> HpcDocDataManagementRulesDTO
    Map<String, HpcDocDataManagementRulesDTO> docsRules = new HashMap<>();
    for (HpcDataManagementConfiguration dataManagementConfiguration : dataManagementService
        .getDataManagementConfigurations()) {
      String doc = dataManagementConfiguration.getDoc();
      HpcDocDataManagementRulesDTO docRules =
          docsRules.containsKey(doc) ? docsRules.get(doc) : new HpcDocDataManagementRulesDTO();

      HpcDataManagementRulesDTO rules = getDataManagementRules(dataManagementConfiguration);
      docRules.setDoc(doc);
      docRules.getRules().add(rules);
      docsRules.put(doc, docRules);
    }

    // Construct and return the DTO
    HpcDataManagementModelDTO dataManagementModel = new HpcDataManagementModelDTO();
    dataManagementModel.getCollectionSystemGeneratedMetadataAttributeNames()
        .addAll(metadataService.getCollectionSystemMetadataAttributeNames());
    dataManagementModel.getDataObjectSystemGeneratedMetadataAttributeNames()
        .addAll(metadataService.getDataObjectSystemMetadataAttributeNames());
    dataManagementModel.getDocRules().addAll(docsRules.values());

    return dataManagementModel;
  }

  @Override
  public HpcDataManagementModelDTO getDataManagementModel(String basePath) throws HpcException {

    // Construct and return the DTO
    HpcDataManagementModelDTO dataManagementModel = new HpcDataManagementModelDTO();

    for (HpcDataManagementConfiguration dataManagementConfiguration : dataManagementService
        .getDataManagementConfigurations()) {
      if (dataManagementConfiguration.getBasePath().equals(basePath)) {
        HpcDataManagementRulesDTO rules = getDataManagementRules(dataManagementConfiguration);
        HpcDocDataManagementRulesDTO docRules = new HpcDocDataManagementRulesDTO();
        docRules.setDoc(dataManagementConfiguration.getDoc());
        docRules.getRules().add(rules);
        dataManagementModel.getDocRules().add(docRules);
        break;
      }
    }

    if (dataManagementModel.getDocRules().isEmpty()) {
      throw new HpcException("Could not obtain Data Management Model for basePath " + basePath,
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    dataManagementModel.getCollectionSystemGeneratedMetadataAttributeNames()
        .addAll(metadataService.getCollectionSystemMetadataAttributeNames());
    dataManagementModel.getDataObjectSystemGeneratedMetadataAttributeNames()
        .addAll(metadataService.getDataObjectSystemMetadataAttributeNames());

    return dataManagementModel;
  }

  private HpcDataManagementRulesDTO getDataManagementRules(
      HpcDataManagementConfiguration dataManagementConfiguration) {
    HpcDataManagementRulesDTO rules = new HpcDataManagementRulesDTO();
    rules.setId(dataManagementConfiguration.getId());
    rules.setBasePath(dataManagementConfiguration.getBasePath());
    rules.setDataHierarchy(dataManagementConfiguration.getDataHierarchy());
    rules.getCollectionMetadataValidationRules()
        .addAll(dataManagementConfiguration.getCollectionMetadataValidationRules());
    rules.getDataObjectMetadataValidationRules()
        .addAll(dataManagementConfiguration.getDataObjectMetadataValidationRules());

    return rules;
  }

  @Override
  public void movePath(String path, boolean pathType, String destinationPath) throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(path) || StringUtils.isEmpty(destinationPath)) {
      throw new HpcException("Empty path or destinationPath in move request: [path: " + path
          + "] [destinationPath: " + destinationPath + "]", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Move the path.
    dataManagementService.move(path, destinationPath, Optional.of(pathType));
  }

  @Override
  public HpcBulkMoveResponseDTO movePaths(HpcBulkMoveRequestDTO bulkMoveRequest)
      throws HpcException {
    // Input validation.
    if (bulkMoveRequest == null) {
      throw new HpcException("Null move request", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    if (bulkMoveRequest.getMoveRequests().isEmpty()) {
      throw new HpcException("No move request items in bulk request",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate the move requests.
    for (HpcMoveRequestDTO moveRequest : bulkMoveRequest.getMoveRequests()) {
      if (StringUtils.isEmpty(moveRequest.getSourcePath())
          || StringUtils.isEmpty(moveRequest.getDestinationPath())) {
        throw new HpcException(
            "Empty source/destination path in move request: [path: " + moveRequest.getSourcePath()
                + "] [Name: " + moveRequest.getDestinationPath() + "]",
            HpcErrorType.INVALID_REQUEST_INPUT);
      }
    }

    // Perform the move requests.
    HpcBulkMoveResponseDTO bulkMoveResponse = new HpcBulkMoveResponseDTO();
    bulkMoveResponse.setResult(true);

    bulkMoveRequest.getMoveRequests().forEach(moveRequest -> {
      // Normalize the paths.
      moveRequest.setSourcePath(toNormalizedPath(moveRequest.getSourcePath()));
      moveRequest.setDestinationPath(toNormalizedPath(moveRequest.getDestinationPath()));

      // Create a response for this move request.
      HpcMoveResponseDTO moveResponse = new HpcMoveResponseDTO();
      moveResponse.setRequest(moveRequest);

      try {
        dataManagementService.move(moveRequest.getSourcePath(), moveRequest.getDestinationPath(),
            Optional.ofNullable(null));

        // Move request is successful.
        moveResponse.setResult(true);

      } catch (HpcException e) {
        // Move request failed.
        moveResponse.setResult(false);

        // If at least one request failed, we consider the entire bulk request to be
        // failed
        bulkMoveResponse.setResult(false);

        moveResponse.setMessage(e.getMessage());
      }

      // Add this response to the bulk response.
      bulkMoveResponse.getMoveResponses().add(moveResponse);
    });

    return bulkMoveResponse;
  }

  // ---------------------------------------------------------------------//
  // Helper Methods
  // ---------------------------------------------------------------------//

  /**
   * Validate permissions request.
   *
   * @param entityPermissionsRequest The request to validate.
   * @throws HpcException if found an invalid request in the list.
   */
  private void validatePermissionsRequest(HpcEntityPermissionsDTO entityPermissionsRequest)
      throws HpcException {
    if (entityPermissionsRequest == null || (entityPermissionsRequest.getUserPermissions().isEmpty()
        && entityPermissionsRequest.getGroupPermissions().isEmpty())) {
      throw new HpcException("Null or empty permissions request",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    Set<String> userIds = new HashSet<>();
    for (HpcUserPermission userPermissionRequest : entityPermissionsRequest.getUserPermissions()) {
      String userId = userPermissionRequest.getUserId();
      HpcPermission permission = userPermissionRequest.getPermission();
      if (StringUtils.isEmpty(userId)) {
        throw new HpcException("Null or empty userId in a permission request",
            HpcErrorType.INVALID_REQUEST_INPUT);
      }
      if (!userIds.add(userId)) {
        throw new HpcException("Duplicate userId in a permission request: " + userId,
            HpcErrorType.INVALID_REQUEST_INPUT);
      }
      if (securityService.getUser(userId) == null) {
        throw new HpcException("User not found: " + userId,
            HpcRequestRejectReason.INVALID_NCI_ACCOUNT);
      }
      if (permission == null) {
        throw new HpcException(
            "Null or empty permission in a permission request. Valid values are ["
                + Arrays.asList(HpcPermission.values()) + "]",
            HpcErrorType.INVALID_REQUEST_INPUT);
      }
    }

    // Validate the group permission requests for this path.
    Set<String> groupNames = new HashSet<>();
    for (HpcGroupPermission groupPermissionRequest : entityPermissionsRequest
        .getGroupPermissions()) {
      String groupName = groupPermissionRequest.getGroupName();
      HpcPermission permission = groupPermissionRequest.getPermission();
      if (StringUtils.isEmpty(groupName)) {
        throw new HpcException("Null or empty group name in a permission request",
            HpcErrorType.INVALID_REQUEST_INPUT);
      }
      if (!groupNames.add(groupName)) {
        throw new HpcException("Duplicate group name in a permission request: " + groupName,
            HpcErrorType.INVALID_REQUEST_INPUT);
      }
      if (permission == null) {
        throw new HpcException(
            "Null or empty permission in a permission request. Valid values are ["
                + Arrays.asList(HpcPermission.values()) + "]",
            HpcErrorType.INVALID_REQUEST_INPUT);
      }
    }
  }

  /**
   * Add collection update event.
   *
   * @param path The path of the entity trigger this event.
   * @param collectionRegistered An indicator if a collection was registered.
   * @param dataObjectRegistered An indicator if a data object was registered.
   * @param userId The user ID who initiated the action resulted in collection update event.
   */
  private void addCollectionUpdatedEvent(String path, boolean collectionRegistered,
      boolean dataObjectRegistered, String userId) {
    try {
      if (!collectionRegistered && !dataObjectRegistered) {
        // Add collection metadata updated event.
        eventService.addCollectionUpdatedEvent(path, userId);
        return;
      }

      // A collection or data object registered, so we add an event for the parent
      // collection.
      String parentCollection = StringUtils.trimTrailingCharacter(path, '/');
      int parentCollectionIndex = parentCollection.lastIndexOf('/');
      parentCollection =
          parentCollectionIndex <= 0 ? "/" : parentCollection.substring(0, parentCollectionIndex);

      if (collectionRegistered) {
        eventService.addCollectionRegistrationEvent(parentCollection, userId);
      } else {
        eventService.addDataObjectRegistrationEvent(parentCollection, userId);
      }

    } catch (HpcException e) {
      logger.error("Failed to add collection update event", e);
    }
  }

  /**
   * Construct a download response DTO object.
   *
   * @param destinationLocation The destination file location.
   * @param destinationFile The destination file.
   * @param taskId The data object download task ID.
   * @param downloadRequestURL A URL to use to download the data object.
   * @param dataTransferType The data transfer type performing the download (S3 or Globus).
   * @return A download response DTO object
   */
  private HpcDataObjectDownloadResponseDTO toDownloadResponseDTO(
      HpcFileLocation destinationLocation, File destinationFile, String taskId,
      String downloadRequestURL, String dataTransferType) {
    // Construct and return a DTO
    HpcDataObjectDownloadResponseDTO downloadResponse = new HpcDataObjectDownloadResponseDTO();
    downloadResponse.setDestinationFile(destinationFile);
    downloadResponse.setDestinationLocation(destinationLocation);
    downloadResponse.setTaskId(taskId);
    downloadResponse.setDownloadRequestURL(downloadRequestURL);
    downloadResponse.setDataTransferType(dataTransferType);

    return downloadResponse;
  }

  /**
   * Create parent collections.
   *
   * @param path The data object's path.
   * @param createParentCollections The indicator whether to create parent collections.
   * @param parentCollectionsBulkMetadataEntries The parent collections bulk metadata entries.
   * @param userId The registrar user-id.
   * @param userName The registrar name.
   * @param configurationId The data management configuration ID.
   * @throws HpcException on service failure.
   */
  private void createParentCollections(String path, Boolean createParentCollections,
      HpcBulkMetadataEntries parentCollectionsBulkMetadataEntries, String userId, String userName,
      String configurationId) throws HpcException {
    // Create parent collections if requested and needed to.
    if (createParentCollections != null && createParentCollections) {
      String parentCollectionPath = path.substring(0, path.lastIndexOf('/'));
      if (!dataManagementService.isPathParentDirectory(path)) {
        // Create a parent collection registration request DTO.
        HpcCollectionRegistrationDTO collectionRegistration = new HpcCollectionRegistrationDTO();
        collectionRegistration.getMetadataEntries().addAll(getParentCollectionMetadataEntries(
            parentCollectionPath, parentCollectionsBulkMetadataEntries));
        collectionRegistration
            .setParentCollectionsBulkMetadataEntries(parentCollectionsBulkMetadataEntries);
        collectionRegistration.setCreateParentCollections(true);

        // Register the parent collection.
        registerCollection(parentCollectionPath, collectionRegistration, userId, userName,
            configurationId);
      } else {
        // The parent collection exists, but in case its registration still in-progress, we wait for
        // its completion.
        Lock lock = pathLocks.get(parentCollectionPath);
        lock.lock();
        lock.unlock();
      }
    }
  }

  /**
   * Perform user permission requests on an entity (collection or data object)
   *
   * @param path The entity path (collection or data object).
   * @param collection True if the path is a collection, False if the path is a data object.
   * @param userPermissionRequests The list of user permissions requests.
   * @return A list of responses to the permission requests.
   */
  private List<HpcUserPermissionResponseDTO> setEntityPermissionForUsers(String path,
      boolean collection, List<HpcUserPermission> userPermissionRequests) {
    List<HpcUserPermissionResponseDTO> permissionResponses = new ArrayList<>();

    // Execute all user permission requests for this entity.
    HpcSubjectPermission subjectPermissionRequest = new HpcSubjectPermission();
    subjectPermissionRequest.setSubjectType(HpcSubjectType.USER);
    for (HpcUserPermission userPermissionRequest : userPermissionRequests) {
      HpcUserPermissionResponseDTO userPermissionResponse = new HpcUserPermissionResponseDTO();
      userPermissionResponse.setUserId(userPermissionRequest.getUserId());
      userPermissionResponse.setResult(true);
      subjectPermissionRequest.setPermission(userPermissionRequest.getPermission());
      subjectPermissionRequest.setSubject(userPermissionRequest.getUserId());
      try {
        // Set the entity permission.
        if (collection) {
          dataManagementService.setCollectionPermission(path, subjectPermissionRequest);
        } else {
          dataManagementService.setDataObjectPermission(path, subjectPermissionRequest);
        }

      } catch (HpcException e) {
        // Request failed. Record the message and keep going.
        userPermissionResponse.setResult(false);
        userPermissionResponse.setMessage(e.getMessage());
      }

      // Add this user permission response to the list.
      permissionResponses.add(userPermissionResponse);
    }

    return permissionResponses;
  }

  /**
   * Recursively delete all the data objects from the specified collection and from it's
   * sub-collections.
   *
   * @param path The path at the root of the hierarchy to delete from.
   * @throws HpcException if it failed to delete any object in this collection.
   */
  private void deleteDataObjectsInCollections(String path) throws HpcException {

    HpcCollectionDTO collectionDto = getCollectionChildren(path);

    if (collectionDto.getCollection() != null) {
      List<HpcCollectionListingEntry> dataObjects = collectionDto.getCollection().getDataObjects();
      if (!CollectionUtils.isEmpty(dataObjects)) {
        // Delete data objects in this collection
        for (HpcCollectionListingEntry entry : dataObjects) {
          deleteDataObject(entry.getPath());
        }
      }

      List<HpcCollectionListingEntry> subCollections =
          collectionDto.getCollection().getSubCollections();
      if (!CollectionUtils.isEmpty(subCollections)) {
        // Recursively delete data objects from this sub-collection and
        // it's sub-collections
        for (HpcCollectionListingEntry entry : subCollections) {
          deleteDataObjectsInCollections(entry.getPath());
        }
      }
    }
  }

  /**
   * Perform group permission requests on an entity (collection or data object)
   *
   * @param path The entity path (collection or data object).
   * @param collection True if the path is a collection, False if the path is a data object.
   * @param groupPermissionRequests The list of group permissions requests.
   * @return A list of responses to the permission requests.
   */
  private List<HpcGroupPermissionResponseDTO> setEntityPermissionForGroups(String path,
      boolean collection, List<HpcGroupPermission> groupPermissionRequests) {
    List<HpcGroupPermissionResponseDTO> permissionResponses = new ArrayList<>();

    // Execute all user permission requests for this entity.
    HpcSubjectPermission subjectPermissionRequest = new HpcSubjectPermission();
    subjectPermissionRequest.setSubjectType(HpcSubjectType.GROUP);
    for (HpcGroupPermission groupPermissionRequest : groupPermissionRequests) {
      HpcGroupPermissionResponseDTO groupPermissionResponse = new HpcGroupPermissionResponseDTO();
      groupPermissionResponse.setGroupName(groupPermissionRequest.getGroupName());
      groupPermissionResponse.setResult(true);
      subjectPermissionRequest.setPermission(groupPermissionRequest.getPermission());
      subjectPermissionRequest.setSubject(groupPermissionRequest.getGroupName());
      try {
        // Set the entity permission.
        if (collection) {
          dataManagementService.setCollectionPermission(path, subjectPermissionRequest);
        } else {
          dataManagementService.setDataObjectPermission(path, subjectPermissionRequest);
        }

      } catch (HpcException e) {
        // Request failed. Record the message and keep going.
        groupPermissionResponse.setResult(false);
        groupPermissionResponse.setMessage(e.getMessage());
      }

      // Add this user permission response to the list.
      permissionResponses.add(groupPermissionResponse);
    }

    return permissionResponses;
  }

  /**
   * Construct entity permissions DTO out of a list of subject permissions.
   *
   * @param subjectPermissions A list of subject permissions.
   * @return Entity permissions DTO
   */
  private HpcEntityPermissionsDTO toEntityPermissionsDTO(
      List<HpcSubjectPermission> subjectPermissions) {
    if (subjectPermissions == null) {
      return null;
    }

    HpcEntityPermissionsDTO entityPermissions = new HpcEntityPermissionsDTO();
    for (HpcSubjectPermission subjectPermission : subjectPermissions) {
      if (subjectPermission.getSubjectType().equals(HpcSubjectType.USER)) {
        HpcUserPermission userPermission = new HpcUserPermission();
        userPermission.setPermission(subjectPermission.getPermission());
        userPermission.setUserId(subjectPermission.getSubject());
        entityPermissions.getUserPermissions().add(userPermission);
      } else {
        HpcGroupPermission groupPermission = new HpcGroupPermission();
        groupPermission.setPermission(subjectPermission.getPermission());
        groupPermission.setGroupName(subjectPermission.getSubject());
        entityPermissions.getGroupPermissions().add(groupPermission);
      }
    }

    return entityPermissions;
  }

  /**
   * Construct user permission DTO out of subject permission.
   *
   * @param subjectPermission A subject permission.
   * @return user permission DTO.
   */
  private HpcUserPermissionDTO toUserPermissionDTO(HpcSubjectPermission subjectPermission) {
    if (subjectPermission == null) {
      return null;
    }

    HpcUserPermissionDTO userPermission = new HpcUserPermissionDTO();
    userPermission.setPermission(subjectPermission.getPermission());
    userPermission.setUserId(subjectPermission.getSubject());

    return userPermission;
  }

  /**
   * Get collection download task status.
   *
   * @param taskId The collection download task ID.
   * @param taskType COLLECTION or DATA_OBJECT_LIST.
   * @return A collection download status DTO. Null if the task could not be found.
   * @throws HpcException on service failure.
   */
  private HpcCollectionDownloadStatusDTO getCollectionDownloadStatus(String taskId,
      HpcDownloadTaskType taskType) throws HpcException {
    // Input validation.
    if (taskId == null) {
      throw new HpcException("Null collection download task ID",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Get the download task status.
    HpcDownloadTaskStatus taskStatus = dataTransferService.getDownloadTaskStatus(taskId, taskType);
    if (taskStatus == null) {
      return null;
    }

    // Map the task status to DTO.
    HpcCollectionDownloadStatusDTO downloadStatus = new HpcCollectionDownloadStatusDTO();
    downloadStatus.setInProgress(taskStatus.getInProgress());
    if (taskStatus.getInProgress()) {
      // Download in progress. Populate the DTO accordingly.
      if (taskType.equals(HpcDownloadTaskType.COLLECTION)) {
        downloadStatus.setPath(taskStatus.getCollectionDownloadTask().getPath());
      } else if (taskType.equals(HpcDownloadTaskType.DATA_OBJECT_LIST)) {
        downloadStatus.getDataObjectPaths()
            .addAll(taskStatus.getCollectionDownloadTask().getDataObjectPaths());
      }
      downloadStatus.setPercentComplete(
          calculateCollectionDownloadPercentComplete(taskStatus.getCollectionDownloadTask()));
      downloadStatus.setCreated(taskStatus.getCollectionDownloadTask().getCreated());
      downloadStatus.setTaskStatus(taskStatus.getCollectionDownloadTask().getStatus());
      if (taskStatus.getCollectionDownloadTask().getS3DownloadDestination() != null) {
        downloadStatus.setDestinationLocation(taskStatus.getCollectionDownloadTask()
            .getS3DownloadDestination().getDestinationLocation());
        downloadStatus.setDestinationType(HpcDataTransferType.S_3);
      } else {
        downloadStatus.setDestinationLocation(taskStatus.getCollectionDownloadTask()
            .getGlobusDownloadDestination().getDestinationLocation());
        downloadStatus.setDestinationType(HpcDataTransferType.GLOBUS);
      }
      populateDownloadItems(downloadStatus, taskStatus.getCollectionDownloadTask().getItems());

    } else {
      // Download completed or failed. Populate the DTO accordingly.
      if (taskType.equals(HpcDownloadTaskType.COLLECTION)) {
        downloadStatus.setPath(taskStatus.getResult().getPath());
      }
      downloadStatus.setCreated(taskStatus.getResult().getCreated());
      downloadStatus.setDestinationLocation(taskStatus.getResult().getDestinationLocation());
      downloadStatus.setDestinationType(taskStatus.getResult().getDestinationType());
      downloadStatus.setCompleted(taskStatus.getResult().getCompleted());
      downloadStatus.setMessage(taskStatus.getResult().getMessage());
      downloadStatus.setResult(taskStatus.getResult().getResult());
      downloadStatus
          .setEffectiveTrasnsferSpeed(taskStatus.getResult().getEffectiveTransferSpeed() > 0
              ? taskStatus.getResult().getEffectiveTransferSpeed()
              : null);
      populateDownloadItems(downloadStatus, taskStatus.getResult().getItems());
    }

    return downloadStatus;
  }

  /**
   * Split the list of download items into completed, failed and in-progress buckets.
   *
   * @param downloadStatus The download status to populate the items into.
   * @param items The collection / bulk download items.
   */
  private void populateDownloadItems(HpcCollectionDownloadStatusDTO downloadStatus,
      List<HpcCollectionDownloadTaskItem> items) {
    for (HpcCollectionDownloadTaskItem item : items) {
      item.setSize(null);
      HpcDownloadResult result = item.getResult();
      if (result == null) {
        downloadStatus.getInProgressItems().add(item);
      } else if (result.equals(HpcDownloadResult.COMPLETED)) {
        item.setPercentComplete(null);
        downloadStatus.getCompletedItems().add(item);
      } else if (result.equals(HpcDownloadResult.CANCELED)) {
        item.setPercentComplete(null);
        downloadStatus.getCanceledItems().add(item);
      } else {
        item.setPercentComplete(null);
        downloadStatus.getFailedItems().add(item);
      }
    }
  }

  /**
   * Delete a data object from the archive.
   *
   * @param path The data object path.
   * @param systemGeneratedMetadata The system generated metadata.
   * @param dataObjectDeleteResponse The deletion response DTO.
   */
  private void deleteDataObjectFromArchive(String path,
      HpcSystemGeneratedMetadata systemGeneratedMetadata,
      HpcDataObjectDeleteResponseDTO dataObjectDeleteResponse) {
    updateDataTransferUploadStatus(path, HpcDataTransferUploadStatus.DELETE_REQUESTED);

    try {
      dataTransferService.deleteDataObject(systemGeneratedMetadata.getArchiveLocation(),
          systemGeneratedMetadata.getDataTransferType(),
          systemGeneratedMetadata.getConfigurationId(),
          systemGeneratedMetadata.getS3ArchiveConfigurationId());

    } catch (HpcException e) {
      logger.error("Failed to delete file from archive", e);
      updateDataTransferUploadStatus(path, HpcDataTransferUploadStatus.DELETE_FAILED);
      dataObjectDeleteResponse.setArchiveDeleteStatus(false);
      dataObjectDeleteResponse.setMessage(e.getMessage());
    }

    updateDataTransferUploadStatus(path, HpcDataTransferUploadStatus.DELETED);
  }

  /**
   * Attempt to update data object upload status. No exception thrown is failed.
   *
   * @param path The data object path.
   * @param dataTransferStatus The data transfer upload system generetaed metadata.
   */
  private void updateDataTransferUploadStatus(String path,
      HpcDataTransferUploadStatus dataTransferStatus) {
    try {
      metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null,
          dataTransferStatus, null, null, null, null);

    } catch (HpcException e) {
      logger.error("Failed to update system metadata: " + path + ". Data transfer status: "
          + dataTransferStatus, HpcErrorType.UNEXPECTED_ERROR, e);
    }
  }

  /**
   * Calculate MD5 checksum of the file and compare it to the value provided.
   *
   * @param file The file to validate checksum.
   * @param checksum The checksum value provided by the caller.
   * @throws HpcException If the calculated checksum doesn't match the provided value.
   */
  private void validateChecksum(File file, String checksum) throws HpcException {
    if (file == null || StringUtils.isEmpty(checksum)) {
      return;
    }

    try {
      if (!checksum.equals(Files.hash(file, Hashing.md5()).toString())) {
        throw new HpcException("Checksum validation failed", HpcErrorType.INVALID_REQUEST_INPUT);
      }

    } catch (IOException e) {
      throw new HpcException("Failed calculate checksum", HpcErrorType.UNEXPECTED_ERROR, e);
    }
  }

  /**
   * Scan the directories in the registration requests and prepare a list of individual data object
   * registration items for all the files found.
   *
   * @param directoryScanRegistrationItems The directory scan registration items.
   * @return A list of data object registration requests for all the files found after scanning the
   *         directories.
   * @throws HpcException On service failure.
   */
  private List<HpcDataObjectRegistrationItemDTO> toDataObjectRegistrationItems(
      List<HpcDirectoryScanRegistrationItemDTO> directoryScanRegistrationItems)
      throws HpcException {
    List<HpcDataObjectRegistrationItemDTO> dataObjectRegistrationItems = new ArrayList<>();
    for (HpcDirectoryScanRegistrationItemDTO directoryScanRegistrationItem : directoryScanRegistrationItems) {
      // Validate and normalize the registration items base path.
      String basePath = toNormalizedPath(directoryScanRegistrationItem.getBasePath());
      if (StringUtils.isEmpty(basePath)) {
        throw new HpcException("Null / Empty base path in directory scan registration request",
            HpcErrorType.INVALID_REQUEST_INPUT);
      }

      // Validate a scan directory was provided.
      if (directoryScanRegistrationItem.getGlobusScanDirectory() == null
          && directoryScanRegistrationItem.getS3ScanDirectory() == null) {
        throw new HpcException("No scan directory provided", HpcErrorType.INVALID_REQUEST_INPUT);
      }
      if (directoryScanRegistrationItem.getGlobusScanDirectory() != null
          && directoryScanRegistrationItem.getS3ScanDirectory() != null) {
        throw new HpcException("Both Globus and S3 directory provided",
            HpcErrorType.INVALID_REQUEST_INPUT);
      }

      // Validate folder map.
      HpcDirectoryScanPathMap pathMap = directoryScanRegistrationItem.getPathMap();
      if (pathMap != null) {
        if (StringUtils.isEmpty(pathMap.getFromPath())
            || StringUtils.isEmpty(pathMap.getToPath())) {
          throw new HpcException("Null / Empty from/to folder in directory scan folder map",
              HpcErrorType.INVALID_REQUEST_INPUT);
        }
        pathMap.setFromPath(toNormalizedPath(pathMap.getFromPath()));
        if (directoryScanRegistrationItem.getS3ScanDirectory() != null) {
          // The 'path' in S3 (which are really object key) don't start with a '/', so
          // need to
          // remove it after normalization.
          pathMap.setFromPath(pathMap.getFromPath().substring(1));
        }
        pathMap.setToPath(toNormalizedPath(pathMap.getToPath()));
      }

      // Get the configuration ID.
      String configurationId = dataManagementService.findDataManagementConfigurationId(basePath);
      if (StringUtils.isEmpty(configurationId)) {
        throw new HpcException("Can't determine configuration id for path: " + basePath,
            HpcErrorType.INVALID_REQUEST_INPUT);
      }

      // Validate the directory to scan exists and accessible.
      HpcPathAttributes pathAttributes = null;
      HpcFileLocation scanDirectoryLocation = null;
      HpcDataTransferType dataTransferType = null;
      HpcS3Account s3Account = null;
      if (directoryScanRegistrationItem.getGlobusScanDirectory() != null) {
        dataTransferType = HpcDataTransferType.GLOBUS;
        scanDirectoryLocation =
            directoryScanRegistrationItem.getGlobusScanDirectory().getDirectoryLocation();
        pathAttributes = dataTransferService.getPathAttributes(HpcDataTransferType.GLOBUS,
            scanDirectoryLocation, false, configurationId, null);
      } else {
        // It is a request to scan an S3 directory.
        dataTransferType = HpcDataTransferType.S_3;
        s3Account = directoryScanRegistrationItem.getS3ScanDirectory().getAccount();
        scanDirectoryLocation =
            directoryScanRegistrationItem.getS3ScanDirectory().getDirectoryLocation();
        pathAttributes =
            dataTransferService.getPathAttributes(s3Account, scanDirectoryLocation, false);
      }

      if (!pathAttributes.getExists()) {
        throw new HpcException("Endpoint or path doesn't exist: "
            + scanDirectoryLocation.getFileContainerId() + ":" + scanDirectoryLocation.getFileId(),
            HpcErrorType.INVALID_REQUEST_INPUT);
      }
      if (!pathAttributes.getIsAccessible()) {
        throw new HpcException("Endpoint is not accessible: "
            + scanDirectoryLocation.getFileContainerId() + ":" + scanDirectoryLocation.getFileId(),
            HpcErrorType.INVALID_REQUEST_INPUT);
      }
      if (!pathAttributes.getIsDirectory()) {
        throw new HpcException("Endpoint is not a directory: "
            + scanDirectoryLocation.getFileContainerId() + ":" + scanDirectoryLocation.getFileId(),
            HpcErrorType.INVALID_REQUEST_INPUT);
      }

      // Default pattern type to SIMPLE if not provided by the caller.
      HpcPatternType patternType = directoryScanRegistrationItem.getPatternType();
      if (patternType == null) {
        patternType = HpcPatternType.SIMPLE;
      }

      final String fileContainerId = scanDirectoryLocation.getFileContainerId();
      final HpcS3Account fs3Account = s3Account;
      dataTransferService
          .scanDirectory(dataTransferType, s3Account, scanDirectoryLocation, configurationId, null,
              directoryScanRegistrationItem.getIncludePatterns(),
              directoryScanRegistrationItem.getExcludePatterns(), patternType)
          .forEach(
              scanItem -> dataObjectRegistrationItems.add(toDataObjectRegistrationItem(scanItem,
                  basePath, fileContainerId, directoryScanRegistrationItem.getCallerObjectId(),
                  directoryScanRegistrationItem.getBulkMetadataEntries(), pathMap, fs3Account)));
    }

    return dataObjectRegistrationItems;
  }

  /**
   * Create a data object registration DTO out of a directory scan item.
   *
   * @param scanItem The scan item.
   * @param basePath The base path to register the scan item with.
   * @param sourceFileContainerId The container ID containing the scan item.
   * @param callerObjectId The caller's object ID.
   * @param bulkMetadataEntries metadata entries for this data object registration and parent
   *        collections.
   * @param pathMap Replace 'fromPath' (found in scanned directory) with 'toPath'.
   * @param s3Account (Optional) Provided if this is a registration item from S3 source, otherwise
   *        null.
   * @return data object registration DTO.
   */
  private HpcDataObjectRegistrationItemDTO toDataObjectRegistrationItem(
      HpcDirectoryScanItem scanItem, String basePath, String sourceFileContainerId,
      String callerObjectId, HpcBulkMetadataEntries bulkMetadataEntries,
      HpcDirectoryScanPathMap pathMap, HpcS3Account s3Account) {
    // If pathMap provided - use the map to replace scanned path with user provided
    // path (or part of
    // path).
    String scanItemFilePath = scanItem.getFilePath();
    if (pathMap != null) {
      scanItemFilePath = scanItemFilePath.replace(pathMap.getFromPath(), pathMap.getToPath());
    }

    // Calculate the data object path to register.
    String path = toNormalizedPath(basePath + '/' + scanItemFilePath);

    // Construct the registration DTO.
    HpcDataObjectRegistrationItemDTO dataObjectRegistration =
        new HpcDataObjectRegistrationItemDTO();
    dataObjectRegistration.setPath(path);
    dataObjectRegistration.setCreateParentCollections(true);

    // Set data object metadata entries.
    if (bulkMetadataEntries != null) {
      for (HpcBulkMetadataEntry bulkMetadataEntry : bulkMetadataEntries.getPathsMetadataEntries()) {
        if (path.equals(toNormalizedPath(bulkMetadataEntry.getPath()))) {
          dataObjectRegistration.getDataObjectMetadataEntries()
              .addAll(bulkMetadataEntry.getPathMetadataEntries());
          break;
        }
      }
    }
    if (dataObjectRegistration.getDataObjectMetadataEntries().isEmpty()) {
      // Could not find user provided data object metadata. Use default.
      dataObjectRegistration.getDataObjectMetadataEntries()
          .addAll(metadataService.getDefaultDataObjectMetadataEntries(scanItem));
    }

    // Set the metadata to create the parent collections.
    dataObjectRegistration.setParentCollectionsBulkMetadataEntries(bulkMetadataEntries);

    // Set the data object source to upload from.
    HpcFileLocation source = new HpcFileLocation();
    source.setFileContainerId(sourceFileContainerId);
    source.setFileId(scanItem.getFilePath());
    if (s3Account != null) {
      HpcS3UploadSource s3UploadSource = new HpcS3UploadSource();
      s3UploadSource.setSourceLocation(source);
      s3UploadSource.setAccount(s3Account);
      dataObjectRegistration.setS3UploadSource(s3UploadSource);
    } else {
      HpcGlobusUploadSource globusUploadSource = new HpcGlobusUploadSource();
      globusUploadSource.setSourceLocation(source);
      dataObjectRegistration.setGlobusUploadSource(globusUploadSource);
    }

    dataObjectRegistration.setCallerObjectId(callerObjectId);

    return dataObjectRegistration;
  }

  /**
   * Update data object metadata and optionally re-generate upload request URL.
   *
   * @param path The data object path.
   * @param metadataEntries The list of metadata entries to update.
   * @param collectionType The type of collection containing the data object.
   * @param generateUploadRequestURL Indicator whether to re-generate the request upload URL.
   * @param checksum The data object checksum provided to check upload integrity.
   * @param userId The userId updating the data object.
   * @param callerObjectId The caller's object ID.
   * @return Upload URL if one was requested, otherwise null.
   * @throws HpcException on service failure.
   */
  private String updateDataObject(String path, List<HpcMetadataEntry> metadataEntries,
      String collectionType, boolean generateUploadRequestURL, String checksum, String userId,
      String callerObjectId) throws HpcException {
    // Get the metadata for this data object.
    HpcMetadataEntries metadataBefore = metadataService.getDataObjectMetadataEntries(path);
    HpcSystemGeneratedMetadata systemGeneratedMetadata =
        metadataService.toSystemGeneratedMetadata(metadataBefore.getSelfMetadataEntries());

    // Update the metadata.
    boolean updated = true;
    String message = null;
    try {
      metadataService.updateDataObjectMetadata(path, metadataEntries,
          systemGeneratedMetadata.getConfigurationId(), collectionType);

    } catch (HpcException e) {
      // Data object metadata update failed. Capture this in the audit record.
      updated = false;
      message = e.getMessage();
      throw (e);

    } finally {
      // Add an audit record of this deletion attempt.
      dataManagementService.addAuditRecord(path, HpcAuditRequestType.UPDATE_DATA_OBJECT,
          metadataBefore, metadataService.getDataObjectMetadataEntries(path),
          systemGeneratedMetadata.getArchiveLocation(), updated, null, message);
    }

    // Optionally re-generate the upload request URL.
    if (generateUploadRequestURL) {
      // Validate the data is not archived yet.
      HpcSystemGeneratedMetadata metadata =
          metadataService.getDataObjectSystemGeneratedMetadata(path);
      if (metadata.getDataTransferStatus().equals(HpcDataTransferUploadStatus.ARCHIVED)) {
        throw new HpcException("Upload URL re-generation not allowed. Data object already archived",
            HpcErrorType.REQUEST_REJECTED);
      }

      // Re-generate the upload request URL.
      HpcDataObjectUploadResponse uploadResponse =
          dataTransferService.uploadDataObject(null, null, null, true, checksum, path, userId,
              callerObjectId, systemGeneratedMetadata.getConfigurationId());

      // Update data-transfer-status system metadata accordingly.
      metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null,
          HpcDataTransferUploadStatus.URL_GENERATED, null, uploadResponse.getDataTransferStarted(),
          null, null);

      return uploadResponse.getUploadRequestURL();
    }

    return null;
  }

  /**
   * Convert a bulk registration task DTO from a registration task domain object.
   *
   * @param task bulk registration task domain object to convert The data object path.
   * @return a bulk registration task DTO.
   */
  private HpcBulkDataObjectRegistrationTaskDTO toBulkDataObjectRegistrationTaskDTO(
      HpcBulkDataObjectRegistrationTask task) {
    HpcBulkDataObjectRegistrationTaskDTO taskDTO = new HpcBulkDataObjectRegistrationTaskDTO();
    taskDTO.setTaskId(task.getId());
    taskDTO.setCreated(task.getCreated());
    taskDTO.setTaskStatus(task.getStatus());
    taskDTO.setPercentComplete(calculateDataObjectBulkRegistrationPercentComplete(task));
    populateRegistrationItems(taskDTO, task.getItems());
    return taskDTO;
  }

  /**
   * Return a bulk registration task DTO from a registration result domain object.
   *
   * @param result bulk registration result domain object to convert to DTO.
   * @return a bulk registration task DTO.
   */
  private HpcBulkDataObjectRegistrationTaskDTO toBulkDataObjectRegistrationTaskDTO(
      HpcBulkDataObjectRegistrationResult result) {
    HpcBulkDataObjectRegistrationTaskDTO taskDTO = new HpcBulkDataObjectRegistrationTaskDTO();
    taskDTO.setTaskId(result.getId());
    taskDTO.setCreated(result.getCreated());
    taskDTO.setCompleted(result.getCompleted());
    taskDTO.setMessage(result.getMessage());
    taskDTO.setResult(result.getResult());
    Integer effectiveTransferSpeed = result.getEffectiveTransferSpeed();
    taskDTO.setEffectiveTransferSpeed(
        effectiveTransferSpeed != null && effectiveTransferSpeed > 0 ? effectiveTransferSpeed
            : null);
    populateRegistrationItems(taskDTO, result.getItems());
    return taskDTO;
  }

  /**
   * Split the list of registration items into completed, failed and in-progress buckets.
   *
   * @param taskDTO The registration task DTO to populate the items into.
   * @param items The registration items.
   */
  private void populateRegistrationItems(HpcBulkDataObjectRegistrationTaskDTO taskDTO,
      List<HpcBulkDataObjectRegistrationItem> items) {
    for (HpcBulkDataObjectRegistrationItem item : items) {
      Boolean result = item.getTask().getResult();
      item.getTask().setSize(null);
      if (result == null) {
        taskDTO.getInProgressItems().add(item.getTask());
      } else if (result) {
        item.getTask().setPercentComplete(null);
        taskDTO.getCompletedItems().add(item.getTask());
      } else {
        item.getTask().setPercentComplete(null);
        taskDTO.getFailedItems().add(item.getTask());
        taskDTO.getFailedItemsRequest()
            .add(dataObjectRegistrationRequestToDTO(item.getRequest(), item.getTask().getPath()));
      }
    }
  }

  /**
   * Validate a download request.
   *
   * @param path The data object path.
   * @param s3DownloadDestination True if the download destination is S3.
   * @param googleDriveDownloadDestination True if the download destination is Google Drive.
   * @return The system generated metadata
   * @throws HpcException If the request is invalid.
   */
  private HpcSystemGeneratedMetadata validateDataObjectDownloadRequest(String path,
      boolean s3DownloadDestination, boolean googleDriveDownloadDestination) throws HpcException {

    // Input validation.
    if (StringUtils.isEmpty(path)) {
      throw new HpcException("Null / Empty path for download", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate the data object exists.
    if (dataManagementService.getDataObject(path) == null) {
      throw new HpcException("Data object doesn't exist: " + path,
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Get the System generated metadata.
    HpcSystemGeneratedMetadata metadata =
        metadataService.getDataObjectSystemGeneratedMetadata(path);

    // If this is a link, we will used the link source system-generated-metadata.
    if (metadata.getLinkSourcePath() != null) {
      return validateDataObjectDownloadRequest(metadata.getLinkSourcePath(), s3DownloadDestination,
          googleDriveDownloadDestination);
    }

    // Download to S3 destination is supported only from Cleversafe archive.
    if (s3DownloadDestination && (metadata.getDataTransferType() == null
        || !metadata.getDataTransferType().equals(HpcDataTransferType.S_3))) {
      throw new HpcException(
          "S3 download request is not supported for POSIX based file system archive",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Download to Google Drive destination is supported only from Cleversafe
    // archive.
    if (googleDriveDownloadDestination && (metadata.getDataTransferType() == null
        || !metadata.getDataTransferType().equals(HpcDataTransferType.S_3))) {
      throw new HpcException(
          "Google Drive download request is not supported for POSIX based file system archive",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate the file is archived.
    HpcDataTransferUploadStatus dataTransferStatus = metadata.getDataTransferStatus();
    if (dataTransferStatus == null) {
      throw new HpcException("Unknown upload data transfer status: " + path,
          HpcErrorType.UNEXPECTED_ERROR);
    }
    if (!dataTransferStatus.equals(HpcDataTransferUploadStatus.ARCHIVED)) {
      throw new HpcException(
          "Object is not in archived state yet. It is in "
              + metadata.getDataTransferStatus().value() + " state",
          HpcRequestRejectReason.FILE_NOT_ARCHIVED);
    }

    return metadata;
  }

  /**
   * Calculate the overall % complete of a collection download task
   *
   * @param downloadTask The collection download task. return The % complete.
   * @return The overall % complete of the collection download task.
   */
  private int calculateCollectionDownloadPercentComplete(HpcCollectionDownloadTask downloadTask) {
    if (downloadTask.getStatus().equals(HpcCollectionDownloadTaskStatus.ACTIVE)) {
      long totalDownloadSize = 0;
      long totalBytesTransferred = 0;
      for (HpcCollectionDownloadTaskItem item : downloadTask.getItems()) {
        totalDownloadSize += item.getSize() != null ? item.getSize() : 0;
        totalBytesTransferred += item.getPercentComplete() != null && item.getSize() != null
            ? ((double) item.getPercentComplete() / 100) * item.getSize()
            : 0;
      }

      if (totalDownloadSize > 0 && totalBytesTransferred <= totalDownloadSize) {
        float percentComplete = (float) 100 * totalBytesTransferred / totalDownloadSize;
        return Math.round(percentComplete);
      }
    }

    return 0;
  }

  /**
   * Calculate the overall % complete of a bulk data object registration task.
   *
   * @param task The bulk registration task.
   * @return The % complete of the bulk registration task..
   */
  private int calculateDataObjectBulkRegistrationPercentComplete(
      HpcBulkDataObjectRegistrationTask task) {
    if (task.getStatus().equals(HpcBulkDataObjectRegistrationTaskStatus.ACTIVE)) {
      long totalUploadSize = 0;
      long totalBytesTransferred = 0;
      for (HpcBulkDataObjectRegistrationItem item : task.getItems()) {
        totalUploadSize += item.getTask().getSize() != null ? item.getTask().getSize() : 0;
        totalBytesTransferred += item.getTask().getPercentComplete() != null
            ? ((double) item.getTask().getPercentComplete() / 100) * item.getTask().getSize()
            : 0;
      }

      if (totalUploadSize > 0 && totalBytesTransferred <= totalUploadSize) {
        float percentComplete = (float) 100 * totalBytesTransferred / totalUploadSize;
        return Math.round(percentComplete);
      }
    }

    return 0;
  }

  /**
   * Get metadata entries for a parent collection registration. The metadata entries are searched in
   * the following manner: 1. Metadata entries found for the specific path in the bulk metadata
   * entries. 2. The default metadata entries if provided in the bulk metadata entries. 3. 'System'
   * default metadata entries.
   *
   * @param parentCollectionPath The parent collection path to provide metadata entries for.
   * @param bulkMetadataEntries Bulk metadata entries to search in.
   * @return Metadata entries for the parent collection path
   */
  private List<HpcMetadataEntry> getParentCollectionMetadataEntries(String parentCollectionPath,
      HpcBulkMetadataEntries bulkMetadataEntries) {
    if (bulkMetadataEntries != null) {
      // Search for the parent collection metadata entries by path.
      for (HpcBulkMetadataEntry bulkMetadataEntry : bulkMetadataEntries.getPathsMetadataEntries()) {
        if (parentCollectionPath.equals(toNormalizedPath(bulkMetadataEntry.getPath()))) {
          return bulkMetadataEntry.getPathMetadataEntries();
        }
      }

      // Not found by path. Return default if provided.
      if (!bulkMetadataEntries.getDefaultCollectionMetadataEntries().isEmpty()) {
        return bulkMetadataEntries.getDefaultCollectionMetadataEntries();
      }
    }

    // Return 'system' default.
    return metadataService.getDefaultCollectionMetadataEntries();
  }

  /**
   * Validate a path. 1. Not null or empty string. 2. Contains no white space. 3. Contains no
   * characters that are invalid (i.e. not acceptable by IRODS).
   *
   * @param path The path to validate.
   * @throws HpcException if the path is invalid.
   */
  private void validatePath(String path) throws HpcException {
    if (StringUtils.isEmpty(path)) {
      throw new HpcException("Null or empty path", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    if (StringUtils.containsWhitespace(path)) {
      throw new HpcException("Path contains white space", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    for (String invalidCharacter : INVALID_PATH_CHARACTERS) {
      if (path.contains(invalidCharacter)) {
        throw new HpcException(
            "Invalid character [" + invalidCharacter + "] in path [" + path + "]",
            HpcErrorType.INVALID_REQUEST_INPUT);
      }
    }
  }

  /**
   * Validate all paths in bulk registration request.
   *
   * @param dataObjectRegistrationItems The registration items.
   * @throws HpcException if the any path is invalid.
   */
  private void validateDataObjectRegistrationDestinationPaths(
      List<HpcDataObjectRegistrationItemDTO> dataObjectRegistrationItems) throws HpcException {
    for (HpcDataObjectRegistrationItemDTO registrationItem : dataObjectRegistrationItems) {
      validatePath(registrationItem.getPath());
    }
  }

  /**
   * Convert HpcDataObjectRegistrationRequest to HpcDataObjectRegistrationItemDTO.
   *
   * @param request The registration request.
   * @param path The registered path
   * @return a HpcDataObjectRegistrationItemDTO object
   */
  private HpcDataObjectRegistrationItemDTO dataObjectRegistrationRequestToDTO(
      HpcDataObjectRegistrationRequest request, String path) {
    HpcDataObjectRegistrationItemDTO dto = new HpcDataObjectRegistrationItemDTO();
    dto.setCallerObjectId(request.getCallerObjectId());
    dto.setCreateParentCollections(request.getCreateParentCollections());
    dto.setPath(path);
    dto.setGlobusUploadSource(request.getGlobusUploadSource());
    dto.setS3UploadSource(request.getS3UploadSource());
    dto.getDataObjectMetadataEntries().addAll(request.getMetadataEntries());
    dto.setParentCollectionsBulkMetadataEntries(request.getParentCollectionsBulkMetadataEntries());
    return dto;
  }

  /**
   * Link an existing data object to a new path.
   *
   * @param path The data object's path.
   * @param dataObjectRegistration A DTO contains the metadata and data transfer locations.
   * @param dataObjectFile (Optional) The data object file attachment
   * @param userId The registrar user-id.
   * @param userName The registrar name.
   * @param configurationId The data management configuration ID.
   * @param collectionType The collection type to contain the registered data object.
   * @throws HpcException on service failure.
   */
  private void linkDataObject(String path,
      HpcDataObjectRegistrationRequestDTO dataObjectRegistration, File dataObjectFile,
      String userId, String userName, String configurationId, String collectionType)
      throws HpcException {
    // Input validation

    // Validate that no upload source of any kind provided w/ the link request
    if (dataObjectFile != null || dataObjectRegistration.getGenerateUploadRequestURL() != null
        || dataObjectRegistration.getGlobusUploadSource() != null
        || dataObjectRegistration.getS3UploadSource() != null
        || dataObjectRegistration.getCallerObjectId() != null) {
      throw new HpcException(
          "S3 / Globus / Generate Upload URL / data attachment provided with link registration request",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate the link source data object exist.
    String linkSourcePath = toNormalizedPath(dataObjectRegistration.getLinkSourcePath());
    if (dataManagementService.getDataObject(linkSourcePath) == null) {
      throw new HpcException("link source doesn't exist: " + linkSourcePath,
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate the link source is not a link itself. Linking to a link is not allowed.
    List<HpcMetadataEntry> linkSourceMetadataEntries =
        metadataService.getDataObjectMetadataEntries(linkSourcePath).getSelfMetadataEntries();
    HpcSystemGeneratedMetadata linkSourceSystemGeneratedMetadata =
        metadataService.toSystemGeneratedMetadata(linkSourceMetadataEntries);
    if (linkSourceSystemGeneratedMetadata.getLinkSourcePath() != null) {
      throw new HpcException("link source can't be a link: " + linkSourcePath,
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate the link source is archived.
    HpcDataTransferUploadStatus linkSourceDataTransferUploadStatus =
        linkSourceSystemGeneratedMetadata.getDataTransferStatus();
    if (linkSourceDataTransferUploadStatus == null
        || !linkSourceDataTransferUploadStatus.equals(HpcDataTransferUploadStatus.ARCHIVED)) {
      throw new HpcException("link source is not archived yet: " + linkSourcePath,
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Attach user provided metadata. We combine the user provided metadata from the link source w/
    // what
    // the user provided in the registration request.
    Map<String, HpcMetadataEntry> metadataEntries = new HashMap<>();
    metadataService.toUserProvidedMetadataEntries(linkSourceMetadataEntries)
        .forEach(metadataEntry -> metadataEntries.put(metadataEntry.getAttribute(), metadataEntry));
    dataObjectRegistration.getMetadataEntries()
        .forEach(metadataEntry -> metadataEntries.put(metadataEntry.getAttribute(), metadataEntry));
    metadataService.addMetadataToDataObject(path,
        new ArrayList<HpcMetadataEntry>(metadataEntries.values()), configurationId, collectionType);

    // Generate data management (iRODS) system metadata and attach to the data
    // object.
    metadataService.addSystemGeneratedMetadataToDataObject(path, userId, userName, configurationId,
        linkSourcePath);
  }


  private HpcPermissionForCollection fetchCollectionPermission(String path, String userId)
      throws HpcException {
    // Input validation.
    if (path == null) {
      throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);
    }
    if (userId == null) {
      throw new HpcException("Null userId", HpcErrorType.INVALID_REQUEST_INPUT);
    }
    // Validate the collection exists.
    if (dataManagementService.getCollection(path, false) == null) {
      return null;
    }
    HpcSubjectPermission hsPerm = dataManagementService.acquireCollectionPermission(path, userId);
    HpcPermissionForCollection resultHpcPermForColl = new HpcPermissionForCollection();
    resultHpcPermForColl.setCollectionPath(path);
    resultHpcPermForColl.setPermission(hsPerm.getPermission());
    return resultHpcPermForColl;
  }

  private boolean hasDataObjectsUnderPath(HpcCollection collection) throws HpcException {

    if (!CollectionUtils.isEmpty(collection.getDataObjects()))
      return true;

    for (HpcCollectionListingEntry subCollection : collection.getSubCollections()) {
      HpcCollection childCollection =
          dataManagementService.getCollection(subCollection.getPath(), true);
      if (hasDataObjectsUnderPath(childCollection))
        return true;
    }
    return false;
  }

}
