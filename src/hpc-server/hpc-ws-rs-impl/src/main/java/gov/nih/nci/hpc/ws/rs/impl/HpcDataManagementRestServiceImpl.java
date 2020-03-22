/**
 * HpcDataManagementRestServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.impl;

import static gov.nih.nci.hpc.util.HpcUtil.toNormalizedPath;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusScanDirectory;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusUploadSource;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationTaskDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkMoveRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkMoveResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompleteMultipartUploadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompleteMultipartUploadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDeleteResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationItemDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcPermsForCollectionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcRegistrationSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermsForCollectionsDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDataManagementRestService;
import gov.nih.nci.hpc.ws.rs.provider.HpcMultipartProvider;

/**
 * HPC Data Management REST Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataManagementRestServiceImpl extends HpcRestServiceImpl
    implements HpcDataManagementRestService {
  // ---------------------------------------------------------------------//
  // Constants
  // ---------------------------------------------------------------------//

  // The attribute name to save download file path on the message context.
  public static final String DATA_OBJECT_DOWNLOAD_FILE_MC_ATTRIBUTE =
      "gov.nih.nci.hpc.ws.rs.impl.HpcDataManagementRestService.dataObjectDownloadFile";

  // The attribute name to save download file path on the message context.
  public static final String DATA_OBJECT_DOWNLOAD_TASK_ID_MC_ATTRIBUTE =
      "gov.nih.nci.hpc.ws.rs.impl.HpcDataManagementRestService.dataObjectDownloadTaskId";

  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // The Data Management Business Service instance.
  @Autowired
  private HpcDataManagementBusService dataManagementBusService = null;

  // The multipart provider.
  @Autowired
  private HpcMultipartProvider multipartProvider = null;

  // ---------------------------------------------------------------------//
  // constructors
  // ---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcDataManagementRestServiceImpl() {}

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  // ---------------------------------------------------------------------//
  // HpcDataManagementRestService Interface Implementation
  // ---------------------------------------------------------------------//

  @Override
  public Response interrogatePathRef(String path) {
    try {
      final String pathElemType =
          dataManagementBusService.interrogatePathRef(path) ? "collection" : "data file";
      final Map<String, String> responseMap = new HashMap<>();
      responseMap.put("path", path);
      responseMap.put("elementType", pathElemType);
      try {
        final String jsonResponseStr = new ObjectMapper().writeValueAsString(responseMap);
        return okResponse(jsonResponseStr, true);
      } catch (JsonProcessingException jpe) {
        // (String message, HpcErrorType errorType, Throwable cause)
        final String errMsg =
            String.format("Failure during conversion of Map to JSON: %s", responseMap.toString());
        throw new HpcException(errMsg, HpcErrorType.UNEXPECTED_ERROR, jpe);
      }
    } catch (HpcException e) {
      return errorResponse(e);
    }
  }

  @Override
  public Response registerCollection(String path,
      HpcCollectionRegistrationDTO collectionRegistration) {
    boolean collectionCreated = true;
    try {
      collectionCreated = dataManagementBusService.registerCollection(toNormalizedPath(path),
          collectionRegistration);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return collectionCreated ? createdResponse(null) : okResponse(null, false);
  }

  @Override
  public Response getCollection(String path, Boolean list, Boolean includeAcl) {
    HpcCollectionListDTO collections = new HpcCollectionListDTO();
    try {
      HpcCollectionDTO collection = dataManagementBusService.getCollection(toNormalizedPath(path),
          list, includeAcl != null ? includeAcl : false);
      if (collection != null) {
        collections.getCollections().add(collection);
      }

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(!collections.getCollections().isEmpty() ? collections : null, true);
  }


  @Override
  public Response getCollectionChildren(String path) {
    HpcCollectionListDTO collections = new HpcCollectionListDTO();
    try {
      HpcCollectionDTO collection =
          dataManagementBusService.getCollectionChildren(toNormalizedPath(path));
      if (collection != null) {
        collections.getCollections().add(collection);
      }

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(!collections.getCollections().isEmpty() ? collections : null, true);
  }

  @Deprecated
  @Override
  public Response downloadCollection(String path, HpcDownloadRequestDTO downloadRequest) {
    // This API is deprecated and replaced by a new download API (which supports additional S3
    // download destination). This API should be removed in the future.
    return downloadCollection(path, toV2(downloadRequest));
  }

  @Override
  public Response downloadCollection(String path,
      gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO downloadRequest) {
    HpcCollectionDownloadResponseDTO downloadResponse = null;
    try {
      downloadResponse =
          dataManagementBusService.downloadCollection(toNormalizedPath(path), downloadRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(downloadResponse, false);
  }

  @Deprecated
  @Override
  public Response getCollectionDownloadStatusV1(String taskId) {
    return getCollectionDownloadStatus(taskId);
  }

  @Override
  public Response getCollectionDownloadStatus(String taskId) {
    HpcCollectionDownloadStatusDTO downloadStatus = null;
    try {
      downloadStatus = dataManagementBusService.getCollectionDownloadStatus(taskId);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(downloadStatus, true);
  }

  @Override
  public Response cancelCollectionDownloadTask(String taskId) {
    try {
      dataManagementBusService.cancelCollectionDownloadTask(taskId);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(null, false);
  }

  @Override
  public Response deleteCollection(String path, Boolean recursive) {
    try {
      recursive = recursive != null ? recursive : false;
      dataManagementBusService.deleteCollection(toNormalizedPath(path), recursive);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(null, false);
  }

  @Override
  public Response moveCollection(String path, String destinationPath) {
    try {
      dataManagementBusService.movePath(toNormalizedPath(path), true,
          toNormalizedPath(destinationPath));

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(null, false);
  }

  @Override
  public Response setCollectionPermissions(String path,
      HpcEntityPermissionsDTO collectionPermissionsRequest) {
    HpcEntityPermissionsResponseDTO permissionsResponse = null;
    try {
      permissionsResponse = dataManagementBusService
          .setCollectionPermissions(toNormalizedPath(path), collectionPermissionsRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(permissionsResponse, false);
  }

  @Override
  public Response getCollectionPermissions(String path) {
    HpcEntityPermissionsDTO entityPermissions = null;
    try {
      entityPermissions = dataManagementBusService.getCollectionPermissions(toNormalizedPath(path));

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(entityPermissions, true);
  }

  @Override
  public Response getCollectionPermission(String path, String userId) {
    HpcUserPermissionDTO hpcUserPermissionDTO = null;
    try {
      hpcUserPermissionDTO =
          dataManagementBusService.getCollectionPermission(toNormalizedPath(path), userId);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(hpcUserPermissionDTO, true);
  }

  @Override
  public Response getPermissionsOnCollectionsForUser(String[] collectionPaths, String userId) {
    HpcUserPermsForCollectionsDTO hpcUserPermsOnCollsDTO = null;
    try {
      hpcUserPermsOnCollsDTO =
          dataManagementBusService.getUserPermissionsOnCollections(collectionPaths, userId);
    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(hpcUserPermsOnCollsDTO, true);
  }

  @Override
  public Response getAllPermissionsOnCollections(String[] collectionPaths) {
    HpcPermsForCollectionsDTO resultDto = null;
    try {
      resultDto = dataManagementBusService.getAllPermissionsOnCollections(collectionPaths);
    } catch (HpcException he) {
      return errorResponse(he);
    }

    return okResponse(resultDto, true);
  }

  @Deprecated
  @Override
  public Response registerDataObject(String path,
      HpcDataObjectRegistrationRequestDTO dataObjectRegistration,
      InputStream dataObjectInputStream) {
    return registerDataObject(path, toV2(dataObjectRegistration), dataObjectInputStream);
  }

  @Override
  public Response registerDataObject(String path,
      gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationRequestDTO dataObjectRegistration,
      InputStream dataObjectInputStream) {
    File dataObjectFile = null;
    HpcDataObjectRegistrationResponseDTO responseDTO = null;
    try {
      dataObjectFile = toFile(dataObjectInputStream);
      responseDTO = dataManagementBusService.registerDataObject(toNormalizedPath(path),
          dataObjectRegistration, dataObjectFile);

    } catch (HpcException e) {
      return errorResponse(e);

    } finally {
      // Delete the temporary file (if provided).
      FileUtils.deleteQuietly(dataObjectFile);
    }

    boolean registered = responseDTO.getRegistered() != null && responseDTO.getRegistered();

    // Remove this indicator from the DTO returned to the caller. The response type
    // (OK or
    // CREATED will provide this information to the caller).
    responseDTO.setRegistered(null);

    if (registered) {
      // Data object was registered. Return a 'created' response.
      return responseDTO.getUploadRequestURL() != null || responseDTO.getMultipartUpload() != null
          ? createdResponse(null, responseDTO)
          : createdResponse(null);
    } else {
      // Data object metadata was updated. Return 'ok' response.
      return okResponse(
          responseDTO.getUploadRequestURL() != null || responseDTO.getMultipartUpload() != null
              ? responseDTO
              : null,
          false);
    }
  }

  @Override
  public Response completeMultipartUpload(String path,
      HpcCompleteMultipartUploadRequestDTO completeMultipartUploadRequest) {
    HpcCompleteMultipartUploadResponseDTO responseDTO = null;

    try {
      responseDTO = dataManagementBusService.completeMultipartUpload(toNormalizedPath(path),
          completeMultipartUploadRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(responseDTO, true);
  }

  @Deprecated
  @Override
  public Response registerDataObjects(
      HpcBulkDataObjectRegistrationRequestDTO bulkDataObjectRegistrationRequest) {
    HpcBulkDataObjectRegistrationResponseDTO registrationResponse = toV1(
        (gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationResponseDTO) registerDataObjects(
            toV2(bulkDataObjectRegistrationRequest)).getEntity());

    return !StringUtils.isEmpty(registrationResponse.getTaskId())
        ? createdResponse(registrationResponse.getTaskId(), registrationResponse)
        : okResponse(registrationResponse, false);
  }

  @Override
  public Response registerDataObjects(
      gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO bulkDataObjectRegistrationRequest) {
    gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationResponseDTO registrationResponse =
        null;
    try {
      registrationResponse =
          dataManagementBusService.registerDataObjects(bulkDataObjectRegistrationRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return !StringUtils.isEmpty(registrationResponse.getTaskId())
        ? createdResponse(registrationResponse.getTaskId(), registrationResponse)
        : okResponse(registrationResponse, false);
  }

  @Deprecated
  @Override
  public Response getDataObjectsRegistrationStatusV1(String taskId) {
    Response response = getDataObjectsRegistrationStatus(taskId);
    if (response.getEntity() == null) {
      return response;
    }

    return okResponse(
        toV1((gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationStatusDTO) response
            .getEntity()),
        true);
  }

  @Override
  public Response getDataObjectsRegistrationStatus(String taskId) {
    gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationStatusDTO registrationStatus =
        null;
    try {
      registrationStatus = dataManagementBusService.getDataObjectsRegistrationStatus(taskId);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(registrationStatus, true);
  }

  @Deprecated
  @Override
  public Response getRegistrationSummaryV1(Integer page, Boolean totalCount) {
    Response response = getRegistrationSummary(page, totalCount);
    if (response.getEntity() == null) {
      return response;
    }

    return okResponse(
        toV1(
            (gov.nih.nci.hpc.dto.datamanagement.v2.HpcRegistrationSummaryDTO) response.getEntity()),
        true);
  }

  @Override
  public Response getRegistrationSummary(Integer page, Boolean totalCount) {
    gov.nih.nci.hpc.dto.datamanagement.v2.HpcRegistrationSummaryDTO registrationSummary = null;
    try {
      registrationSummary = dataManagementBusService.getRegistrationSummary(page != null ? page : 1,
          totalCount != null ? totalCount : false);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(
        registrationSummary.getActiveTasks().isEmpty()
            && registrationSummary.getCompletedTasks().isEmpty() ? null : registrationSummary,
        true);
  }

  @Override
  public Response getDataObject(String path, Boolean includeAcl) {
    HpcDataObjectListDTO dataObjects = new HpcDataObjectListDTO();
    try {
      HpcDataObjectDTO dataObject = dataManagementBusService.getDataObject(toNormalizedPath(path),
          includeAcl != null ? includeAcl : false);
      if (dataObject != null) {
        dataObjects.getDataObjects().add(dataObject);
      }

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(!dataObjects.getDataObjects().isEmpty() ? dataObjects : null, true);
  }


  @Deprecated
  @Override
  public Response downloadDataObject(String path, HpcDownloadRequestDTO downloadRequest,
      MessageContext messageContext) {
    // This API is deprecated and replaced by a new download API (which supports additional S3
    // download destination) and
    // a dedicated API to generate download URL. This API should be removed in the future.
    if (downloadRequest != null && downloadRequest.getGenerateDownloadRequestURL() != null
        && downloadRequest.getGenerateDownloadRequestURL()) {
      if (downloadRequest.getDestination() != null
          || downloadRequest.getDestinationOverwrite() != null) {
        return errorResponse(new HpcException(
            "Invalid download request. Request must have a destination or generateDownloadRequestURL",
            HpcErrorType.INVALID_REQUEST_INPUT));
      }
      return generateDownloadRequestURL(path);
    }
    return downloadDataObject(path, toV2(downloadRequest), messageContext);
  }

  @Override
  public Response generateDownloadRequestURL(String path) {
    HpcDataObjectDownloadResponseDTO downloadResponse = null;
    try {
      downloadResponse =
          dataManagementBusService.generateDownloadRequestURL(toNormalizedPath(path));

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(downloadResponse, false);
  }

  @Override
  public Response downloadDataObject(String path,
      gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO downloadRequest,
      MessageContext messageContext) {
    HpcDataObjectDownloadResponseDTO downloadResponse = null;
    try {
      downloadResponse =
          dataManagementBusService.downloadDataObject(toNormalizedPath(path), downloadRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return downloadResponse(downloadResponse, messageContext);
  }

  @Deprecated
  @Override
  public Response getDataObjectDownloadStatusV1(String taskId) {
    return getDataObjectDownloadStatus(taskId);
  }

  @Override
  public Response getDataObjectDownloadStatus(String taskId) {
    HpcDataObjectDownloadStatusDTO downloadStatus = null;
    try {
      downloadStatus = dataManagementBusService.getDataObjectDownloadStatus(taskId);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(downloadStatus, true);
  }

  @Override
  public Response deleteDataObject(String path) {
    HpcDataObjectDeleteResponseDTO dataObjectDeleteResponse = null;
    try {
      dataObjectDeleteResponse = dataManagementBusService.deleteDataObject(toNormalizedPath(path));

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(dataObjectDeleteResponse, false);
  }

  @Override
  public Response moveDataObject(String path, String destinationPath) {
    try {
      dataManagementBusService.movePath(toNormalizedPath(path), false,
          toNormalizedPath(destinationPath));

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(null, false);
  }

  @Override
  public Response setDataObjectPermissions(String path,
      HpcEntityPermissionsDTO dataObjectPermissionsRequest) {
    HpcEntityPermissionsResponseDTO permissionsResponse = null;
    try {
      permissionsResponse = dataManagementBusService
          .setDataObjectPermissions(toNormalizedPath(path), dataObjectPermissionsRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(permissionsResponse, false);
  }

  @Override
  public Response getDataObjectPermissions(String path) {
    HpcEntityPermissionsDTO entityPermissions = null;
    try {
      entityPermissions = dataManagementBusService.getDataObjectPermissions(toNormalizedPath(path));

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(entityPermissions, true);
  }

  @Override
  public Response getDataObjectPermission(String path, String userId) {
    HpcUserPermissionDTO hpcUserPermissionDTO = null;
    try {
      hpcUserPermissionDTO =
          dataManagementBusService.getDataObjectPermission(toNormalizedPath(path), userId);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(hpcUserPermissionDTO, true);
  }

  @Deprecated
  @Override
  public Response downloadDataObjects(HpcBulkDataObjectDownloadRequestDTO downloadRequest) {
    return downloadDataObjectsOrCollections(toV2(downloadRequest));
  }

  @Override
  public Response downloadDataObjectsOrCollections(
      gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectDownloadRequestDTO downloadRequest) {
    HpcBulkDataObjectDownloadResponseDTO downloadResponse = null;
    try {
      downloadResponse = dataManagementBusService.downloadDataObjectsOrCollections(downloadRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(downloadResponse, false);
  }

  @Override
  public Response getDataObjectsOrCollectionsDownloadStatus(String taskId) {
    HpcCollectionDownloadStatusDTO downloadStatus = null;
    try {
      downloadStatus = dataManagementBusService.getDataObjectsOrCollectionsDownloadStatus(taskId);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(downloadStatus, true);
  }

  @Override
  public Response cancelDataObjectsOrCollectionsDownloadTask(String taskId) {
    try {
      dataManagementBusService.cancelDataObjectsOrCollectionsDownloadTask(taskId);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(null, false);
  }

  @Override
  public Response getDownloadSummary(Integer page, Boolean totalCount) {
    HpcDownloadSummaryDTO downloadSummary = null;
    try {
      downloadSummary = dataManagementBusService.getDownloadSummary(page != null ? page : 1,
          totalCount != null ? totalCount : false);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(
        downloadSummary.getActiveTasks().isEmpty() && downloadSummary.getCompletedTasks().isEmpty()
            ? null
            : downloadSummary,
        true);
  }

  @Override
  public Response getDataManagementModels() {
    HpcDataManagementModelDTO docModel = null;
    try {
      docModel = dataManagementBusService.getDataManagementModels();

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(docModel, true);
  }

  @Override
  public Response getDataManagementModel(String basePath) {
    HpcDataManagementModelDTO docModel = null;
    try {
      docModel = dataManagementBusService.getDataManagementModel(toNormalizedPath(basePath));

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(docModel, true);
  }

  @Override
  public Response movePaths(HpcBulkMoveRequestDTO bulkMoveRequest) {
    HpcBulkMoveResponseDTO bulkMoveResponse = null;
    try {
      bulkMoveResponse = dataManagementBusService.movePaths(bulkMoveRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return bulkMoveResponse.getResult() ? okResponse(bulkMoveResponse, true)
        : errorResponse(bulkMoveResponse);
  }

  // TODO - Remove HPCDATAMGM-1189 code
  @Override
  public Response updateFileContainerName() {
    try {
      dataManagementBusService.updateFileContainerName();
    } catch (HpcException e) {
      return errorResponse(e);
    }
    return okResponse(null, false);

  }

  // ---------------------------------------------------------------------//
  // Helper Methods
  // ---------------------------------------------------------------------//

  /**
   * Copy input stream to File and close the input stream.
   *
   * @param dataObjectInputStream The input stream.
   * @return File
   * @throws HpcException if copy of input stream failed.
   */
  private File toFile(InputStream dataObjectInputStream) throws HpcException {
    if (dataObjectInputStream == null) {
      return null;
    }

    File dataObjectFile =
        FileUtils.getFile(multipartProvider.getTempDirectory(), UUID.randomUUID().toString());
    try {
      FileUtils.copyInputStreamToFile(dataObjectInputStream, dataObjectFile);

    } catch (IOException e) {
      throw new HpcException("Failed to copy input stream", HpcErrorType.UNEXPECTED_ERROR, e);
    }

    return dataObjectFile;
  }

  /**
   * Create a Response object out of the DTO. Also set the download file path on the message
   * context, so that the cleanup interceptor can remove it after requested file reached the caller.
   *
   * @param downloadResponse The download response.
   * @param messageContext The message context.
   * @return an OK response.
   */
  private Response downloadResponse(HpcDataObjectDownloadResponseDTO downloadResponse,
      MessageContext messageContext) {
    if (downloadResponse == null) {
      return okResponse(null, false);
    }
    Response response = null;
    if (downloadResponse.getDestinationFile() != null) {
      // Put the download file on the message context, so the cleanup interceptor can
      // delete it after the file was received by the caller.
      messageContext.put(DATA_OBJECT_DOWNLOAD_FILE_MC_ATTRIBUTE,
          downloadResponse.getDestinationFile());
      messageContext.put(DATA_OBJECT_DOWNLOAD_TASK_ID_MC_ATTRIBUTE, downloadResponse.getTaskId());
      response = okResponse(downloadResponse.getDestinationFile(),
          MediaType.APPLICATION_OCTET_STREAM_TYPE);
    } else {
      response = okResponse(downloadResponse, false);
    }
    return response;
  }

  /**
   * Convert v1 of HpcDownloadRequest to v2 of the API.
   *
   * @param downloadRequest download request (v1).
   * @return The download request in v2 form.
   */
  private gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO toV2(
      HpcDownloadRequestDTO downloadRequest) {
    if (downloadRequest == null) {
      return null;
    }

    gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO v2DownloadRequest =
        new gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO();
    if (downloadRequest.getDestination() != null) {
      HpcGlobusDownloadDestination globusDownloadDestination = new HpcGlobusDownloadDestination();
      globusDownloadDestination.setDestinationLocation(downloadRequest.getDestination());
      globusDownloadDestination.setDestinationOverwrite(downloadRequest.getDestinationOverwrite());
      v2DownloadRequest.setGlobusDownloadDestination(globusDownloadDestination);
    }

    return v2DownloadRequest;
  }

  /**
   * Convert v1 of HpcBulkDataObjectDownloadRequestDTO to v2 of the API.
   *
   * @param downloadRequest download request (v1).
   * @return The download request in v2 form.
   */
  private gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectDownloadRequestDTO toV2(
      HpcBulkDataObjectDownloadRequestDTO downloadRequest) {
    if (downloadRequest == null) {
      return null;
    }

    gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectDownloadRequestDTO v2DownloadRequest =
        new gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectDownloadRequestDTO();
    v2DownloadRequest.getDataObjectPaths().addAll(downloadRequest.getDataObjectPaths());
    if (downloadRequest.getDestination() != null) {
      HpcGlobusDownloadDestination globusDownloadDestination = new HpcGlobusDownloadDestination();
      globusDownloadDestination.setDestinationLocation(downloadRequest.getDestination());
      globusDownloadDestination.setDestinationOverwrite(downloadRequest.getDestinationOverwrite());
      v2DownloadRequest.setGlobusDownloadDestination(globusDownloadDestination);
    }

    return v2DownloadRequest;
  }

  /**
   * Convert v1 of HpcDataObjectRegistrationRequestDTO to v2 of the API.
   *
   * @param dataObjectRegistration data object registration (v1).
   * @return The registration request in v2 form.
   */
  private gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationRequestDTO toV2(
      HpcDataObjectRegistrationRequestDTO dataObjectRegistration) {
    if (dataObjectRegistration == null) {
      return null;
    }

    gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationRequestDTO v2DataObjectRegistration =
        new gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationRequestDTO();
    v2DataObjectRegistration.setCallerObjectId(dataObjectRegistration.getCallerObjectId());
    v2DataObjectRegistration.setChecksum(dataObjectRegistration.getChecksum());
    v2DataObjectRegistration
        .setCreateParentCollections(dataObjectRegistration.getCreateParentCollections());
    v2DataObjectRegistration.setParentCollectionsBulkMetadataEntries(
        dataObjectRegistration.getParentCollectionsBulkMetadataEntries());
    v2DataObjectRegistration
        .setGenerateUploadRequestURL(dataObjectRegistration.getGenerateUploadRequestURL());
    v2DataObjectRegistration.getMetadataEntries()
        .addAll(dataObjectRegistration.getMetadataEntries());

    if (dataObjectRegistration.getSource() != null
        && !StringUtils.isEmpty(dataObjectRegistration.getSource().getFileContainerId())
        && !StringUtils.isEmpty(dataObjectRegistration.getSource().getFileId())) {
      HpcGlobusUploadSource globusUploadSource = new HpcGlobusUploadSource();
      globusUploadSource.setSourceLocation(dataObjectRegistration.getSource());
      v2DataObjectRegistration.setGlobusUploadSource(globusUploadSource);
    }

    return v2DataObjectRegistration;
  }

  /**
   * Convert v1 of HpcBulkDataObjectRegistrationRequestDTO to v2 of the API.
   *
   * @param bulkDataObjectRegistrationRequest bulk registration request (v1).
   * @return The bulk registration in v2 form.
   */
  private gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO toV2(
      HpcBulkDataObjectRegistrationRequestDTO bulkDataObjectRegistrationRequest) {
    if (bulkDataObjectRegistrationRequest == null) {
      return null;
    }

    gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO v2BulkDataObjectRegistrationRequest =
        new gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO();
    v2BulkDataObjectRegistrationRequest.setDryRun(bulkDataObjectRegistrationRequest.getDryRun());
    v2BulkDataObjectRegistrationRequest.setUiURL(bulkDataObjectRegistrationRequest.getUiURL());

    bulkDataObjectRegistrationRequest.getDataObjectRegistrationItems()
        .forEach(dataObjectRegistrationItem -> {
          gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO v2DataObjectRegistrationItem =
              new gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO();
          v2DataObjectRegistrationItem
              .setCallerObjectId(dataObjectRegistrationItem.getCallerObjectId());
          v2DataObjectRegistrationItem
              .setCreateParentCollections(dataObjectRegistrationItem.getCreateParentCollections());
          v2DataObjectRegistrationItem.setParentCollectionsBulkMetadataEntries(
              dataObjectRegistrationItem.getParentCollectionsBulkMetadataEntries());
          v2DataObjectRegistrationItem.setPath(dataObjectRegistrationItem.getPath());
          v2DataObjectRegistrationItem.getDataObjectMetadataEntries()
              .addAll(dataObjectRegistrationItem.getDataObjectMetadataEntries());
          HpcGlobusUploadSource globusUploadSource = new HpcGlobusUploadSource();
          globusUploadSource.setSourceLocation(dataObjectRegistrationItem.getSource());
          v2DataObjectRegistrationItem.setGlobusUploadSource(globusUploadSource);
          v2BulkDataObjectRegistrationRequest.getDataObjectRegistrationItems()
              .add(v2DataObjectRegistrationItem);
        });

    bulkDataObjectRegistrationRequest.getDirectoryScanRegistrationItems()
        .forEach(directoryScanRegistrationItem -> {
          gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO v2DirectoryScanRegistrationItem =
              new gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO();
          v2DirectoryScanRegistrationItem.setBasePath(directoryScanRegistrationItem.getBasePath());
          v2DirectoryScanRegistrationItem
              .setBulkMetadataEntries(directoryScanRegistrationItem.getBulkMetadataEntries());
          v2DirectoryScanRegistrationItem
              .setCallerObjectId(directoryScanRegistrationItem.getCallerObjectId());
          v2DirectoryScanRegistrationItem.setPathMap(directoryScanRegistrationItem.getPathMap());
          v2DirectoryScanRegistrationItem
              .setPatternType(directoryScanRegistrationItem.getPatternType());
          v2DirectoryScanRegistrationItem.getExcludePatterns()
              .addAll(directoryScanRegistrationItem.getExcludePatterns());
          v2DirectoryScanRegistrationItem.getIncludePatterns()
              .addAll(directoryScanRegistrationItem.getIncludePatterns());
          HpcGlobusScanDirectory globusScanDirectory = new HpcGlobusScanDirectory();
          globusScanDirectory
              .setDirectoryLocation(directoryScanRegistrationItem.getScanDirectoryLocation());
          v2DirectoryScanRegistrationItem.setGlobusScanDirectory(globusScanDirectory);
          v2BulkDataObjectRegistrationRequest.getDirectoryScanRegistrationItems()
              .add(v2DirectoryScanRegistrationItem);
        });

    return v2BulkDataObjectRegistrationRequest;
  }

  /**
   * Convert v2 of HpcBulkDataObjectRegistrationResponseDTO to v1 of the API.
   *
   * @param v2BulkDataObjectRegistrationResponse bulk registration response (v2).
   * @return The bulk registration in v1 form.
   */
  private HpcBulkDataObjectRegistrationResponseDTO toV1(
      gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationResponseDTO v2BulkDataObjectRegistrationResponse) {
    if (v2BulkDataObjectRegistrationResponse == null) {
      return null;
    }

    HpcBulkDataObjectRegistrationResponseDTO bulkDataObjectRegistrationResponse =
        new HpcBulkDataObjectRegistrationResponseDTO();

    bulkDataObjectRegistrationResponse.setTaskId(v2BulkDataObjectRegistrationResponse.getTaskId());
    v2BulkDataObjectRegistrationResponse.getDataObjectRegistrationItems()
        .forEach(v2DataObjectRegistrationItem -> {
          HpcDataObjectRegistrationItemDTO dataObjectRegistrationItem =
              new HpcDataObjectRegistrationItemDTO();
          dataObjectRegistrationItem
              .setCallerObjectId(v2DataObjectRegistrationItem.getCallerObjectId());
          dataObjectRegistrationItem.setCreateParentCollections(
              v2DataObjectRegistrationItem.getCreateParentCollections());
          dataObjectRegistrationItem.setParentCollectionsBulkMetadataEntries(
              v2DataObjectRegistrationItem.getParentCollectionsBulkMetadataEntries());
          dataObjectRegistrationItem.setPath(v2DataObjectRegistrationItem.getPath());
          dataObjectRegistrationItem.getDataObjectMetadataEntries()
              .addAll(v2DataObjectRegistrationItem.getDataObjectMetadataEntries());
          if (v2DataObjectRegistrationItem.getGlobusUploadSource() != null) {
            dataObjectRegistrationItem.setSource(
                v2DataObjectRegistrationItem.getGlobusUploadSource().getSourceLocation());
          }
          bulkDataObjectRegistrationResponse.getDataObjectRegistrationItems()
              .add(dataObjectRegistrationItem);
        });

    return bulkDataObjectRegistrationResponse;
  }

  /**
   * Convert v2 of HpcBulkDataObjectRegistrationStatusDTO to v1 of the API.
   *
   * @param v2BulkDataObjectRegistrationStatus bulk registration status (v2).
   * @return The bulk registration status in v1 form.
   */
  private HpcBulkDataObjectRegistrationStatusDTO toV1(
      gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationStatusDTO v2BulkDataObjectRegistrationStatus) {
    if (v2BulkDataObjectRegistrationStatus == null) {
      return null;
    }

    HpcBulkDataObjectRegistrationStatusDTO bulkDataObjectRegistrationStatus =
        new HpcBulkDataObjectRegistrationStatusDTO();

    bulkDataObjectRegistrationStatus
        .setInProgress(v2BulkDataObjectRegistrationStatus.getInProgress());
    bulkDataObjectRegistrationStatus.setTask(toV1(v2BulkDataObjectRegistrationStatus.getTask()));

    return bulkDataObjectRegistrationStatus;
  }

  /**
   * Convert v2 of HpcBulkDataObjectRegistrationTaskDTO to v1 of the API.
   *
   * @param v2BulkDataObjectRegistrationTask bulk registration task (v2).
   * @return The bulk registration task in v1 form.
   */
  private HpcBulkDataObjectRegistrationTaskDTO toV1(
      gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationTaskDTO v2BulkDataObjectRegistrationTask) {
    if (v2BulkDataObjectRegistrationTask == null) {
      return null;
    }

    HpcBulkDataObjectRegistrationTaskDTO bulkDataObjectRegistrationTask =
        new HpcBulkDataObjectRegistrationTaskDTO();

    bulkDataObjectRegistrationTask.setTaskId(v2BulkDataObjectRegistrationTask.getTaskId());
    bulkDataObjectRegistrationTask.setTaskStatus(v2BulkDataObjectRegistrationTask.getTaskStatus());
    bulkDataObjectRegistrationTask.setResult(v2BulkDataObjectRegistrationTask.getResult());
    bulkDataObjectRegistrationTask.getCompletedItems()
        .addAll(v2BulkDataObjectRegistrationTask.getCompletedItems());
    bulkDataObjectRegistrationTask.getFailedItems()
        .addAll(v2BulkDataObjectRegistrationTask.getFailedItems());
    bulkDataObjectRegistrationTask.getInProgressItems()
        .addAll(v2BulkDataObjectRegistrationTask.getInProgressItems());
    bulkDataObjectRegistrationTask.setMessage(v2BulkDataObjectRegistrationTask.getMessage());
    bulkDataObjectRegistrationTask
        .setEffectiveTransferSpeed(v2BulkDataObjectRegistrationTask.getEffectiveTransferSpeed());
    bulkDataObjectRegistrationTask
        .setPercentComplete(v2BulkDataObjectRegistrationTask.getPercentComplete());
    bulkDataObjectRegistrationTask.setCreated(v2BulkDataObjectRegistrationTask.getCreated());
    bulkDataObjectRegistrationTask.setCompleted(v2BulkDataObjectRegistrationTask.getCompleted());
    v2BulkDataObjectRegistrationTask.getFailedItemsRequest()
        .forEach(v2DataObjectRegistrationItem -> {
          HpcDataObjectRegistrationItemDTO dataObjectRegistrationItem =
              new HpcDataObjectRegistrationItemDTO();
          dataObjectRegistrationItem
              .setCallerObjectId(v2DataObjectRegistrationItem.getCallerObjectId());
          dataObjectRegistrationItem.setCreateParentCollections(
              v2DataObjectRegistrationItem.getCreateParentCollections());
          dataObjectRegistrationItem.setParentCollectionsBulkMetadataEntries(
              v2DataObjectRegistrationItem.getParentCollectionsBulkMetadataEntries());
          dataObjectRegistrationItem.setPath(v2DataObjectRegistrationItem.getPath());
          dataObjectRegistrationItem.getDataObjectMetadataEntries()
              .addAll(v2DataObjectRegistrationItem.getDataObjectMetadataEntries());
          dataObjectRegistrationItem
              .setSource(v2DataObjectRegistrationItem.getGlobusUploadSource() != null
                  ? v2DataObjectRegistrationItem.getGlobusUploadSource().getSourceLocation()
                  : null);
          bulkDataObjectRegistrationTask.getFailedItemsRequest().add(dataObjectRegistrationItem);
        });

    return bulkDataObjectRegistrationTask;
  }

  /**
   * Convert v2 of HpcRegistrationSummaryDTO to v1 of the API.
   *
   * @param v2RegistrationSummary The registration summary (v2).
   * @return The registration summary in v1 form.
   */
  private HpcRegistrationSummaryDTO toV1(
      gov.nih.nci.hpc.dto.datamanagement.v2.HpcRegistrationSummaryDTO v2RegistrationSummary) {
    if (v2RegistrationSummary == null) {
      return null;
    }

    HpcRegistrationSummaryDTO registrationSummary = new HpcRegistrationSummaryDTO();
    registrationSummary.setLimit(v2RegistrationSummary.getLimit());
    registrationSummary.setPage(v2RegistrationSummary.getPage());
    registrationSummary.setTotalCount(v2RegistrationSummary.getTotalCount());
    v2RegistrationSummary.getActiveTasks()
        .forEach(v2BulkDataObjectRegistrationTask -> registrationSummary.getActiveTasks()
            .add(toV1(v2BulkDataObjectRegistrationTask)));
    v2RegistrationSummary.getCompletedTasks()
        .forEach(v2BulkDataObjectRegistrationTask -> registrationSummary.getCompletedTasks()
            .add(toV1(v2BulkDataObjectRegistrationTask)));

    return registrationSummary;
  }
}
