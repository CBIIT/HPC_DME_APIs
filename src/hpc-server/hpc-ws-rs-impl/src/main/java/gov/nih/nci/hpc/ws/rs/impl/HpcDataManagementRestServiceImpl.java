/**
 * HpcDataManagementRestServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
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
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkRenameRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkRenameResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDeleteResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
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

  // The attribue name to save download file path on the message context.
  public static final String DATA_OBJECT_DOWNLOAD_FILE_MC_ATTRIBUTE =
      "gov.nih.nci.hpc.ws.rs.impl.HpcDataManagementRestService.dataObjectDownloadFile";

  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // The Data Management Business Service instance.
  @Autowired private HpcDataManagementBusService dataManagementBusService = null;

  // The multipart provider.
  @Autowired private HpcMultipartProvider multipartProvider = null;

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
  public Response registerCollection(
      String path, HpcCollectionRegistrationDTO collectionRegistration) {
    boolean collectionCreated = true;
    try {
      collectionCreated =
          dataManagementBusService.registerCollection(
              toNormalizedPath(path), collectionRegistration);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return collectionCreated ? createdResponse(null) : okResponse(null, false);
  }

  @Override
  public Response getCollection(String path, Boolean list) {
    HpcCollectionListDTO collections = new HpcCollectionListDTO();
    try {
      HpcCollectionDTO collection =
          dataManagementBusService.getCollection(toNormalizedPath(path), list);
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

  @Override
  public Response downloadCollection(String path, HpcDownloadRequestDTO downloadRequest) {
    HpcCollectionDownloadResponseDTO downloadResponse = null;
    try {
      downloadResponse =
          dataManagementBusService.downloadCollection(toNormalizedPath(path), downloadRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(downloadResponse, false);
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
  public Response moveCollection(String path, String toPath) {
    try {
      dataManagementBusService.movePath(toNormalizedPath(path), true, toNormalizedPath(toPath));

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(null, false);
  }

  @Override
  public Response setCollectionPermissions(
      String path, HpcEntityPermissionsDTO collectionPermissionsRequest) {
    HpcEntityPermissionsResponseDTO permissionsResponse = null;
    try {
      permissionsResponse =
          dataManagementBusService.setCollectionPermissions(
              toNormalizedPath(path), collectionPermissionsRequest);

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

  @Override
  public Response registerDataObject(
      String path,
      HpcDataObjectRegistrationRequestDTO dataObjectRegistration,
      InputStream dataObjectInputStream) {
    File dataObjectFile = null;
    HpcDataObjectRegistrationResponseDTO responseDTO = null;
    try {
      dataObjectFile = toFile(dataObjectInputStream);
      responseDTO =
          dataManagementBusService.registerDataObject(
              toNormalizedPath(path), dataObjectRegistration, dataObjectFile);

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
      return responseDTO.getUploadRequestURL() != null
          ? createdResponse(null, responseDTO)
          : createdResponse(null);
    } else {
      // Data object metadata was updated. Return 'ok' response.
      return okResponse(responseDTO.getUploadRequestURL() != null ? responseDTO : null, false);
    }
  }

  @Override
  public Response registerDataObjects(
      HpcBulkDataObjectRegistrationRequestDTO bulkDataObjectRegistrationRequest) {
    HpcBulkDataObjectRegistrationResponseDTO registrationResponse = null;
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

  @Override
  public Response getDataObjectsRegistrationStatus(String taskId) {
    HpcBulkDataObjectRegistrationStatusDTO registrationStatus = null;
    try {
      registrationStatus = dataManagementBusService.getDataObjectsRegistrationStatus(taskId);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(registrationStatus, true);
  }

  @Override
  public Response getRegistrationSummary(Integer page, Boolean totalCount) {
    HpcRegistrationSummaryDTO registrationSummary = null;
    try {
      registrationSummary =
          dataManagementBusService.getRegistrationSummary(
              page != null ? page : 1, totalCount != null ? totalCount : false);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(
        registrationSummary.getActiveTasks().isEmpty()
                && registrationSummary.getCompletedTasks().isEmpty()
            ? null
            : registrationSummary,
        true);
  }

  @Override
  public Response getDataObject(String path) {
    HpcDataObjectListDTO dataObjects = new HpcDataObjectListDTO();
    try {
      HpcDataObjectDTO dataObject = dataManagementBusService.getDataObject(toNormalizedPath(path));
      if (dataObject != null) {
        dataObjects.getDataObjects().add(dataObject);
      }

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(!dataObjects.getDataObjects().isEmpty() ? dataObjects : null, true);
  }

  @Override
  public Response downloadDataObject(
      String path, HpcDownloadRequestDTO downloadRequest, MessageContext messageContext) {
    HpcDataObjectDownloadResponseDTO downloadResponse = null;
    try {
      downloadResponse =
          dataManagementBusService.downloadDataObject(toNormalizedPath(path), downloadRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }
    Map<String, String> header = new HashMap<String, String>();
    header.put("DATA_TRANSFER_TYPE", downloadResponse.getDataTransferType());
    return downloadResponse(downloadResponse, messageContext, header);
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
  public Response moveDataObject(String path, String toPath) {
    try {
      dataManagementBusService.movePath(toNormalizedPath(path), false, toNormalizedPath(toPath));

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(null, false);
  }

  @Override
  public Response setDataObjectPermissions(
      String path, HpcEntityPermissionsDTO dataObjectPermissionsRequest) {
    HpcEntityPermissionsResponseDTO permissionsResponse = null;
    try {
      permissionsResponse =
          dataManagementBusService.setDataObjectPermissions(
              toNormalizedPath(path), dataObjectPermissionsRequest);

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

  @Override
  public Response downloadDataObjects(HpcBulkDataObjectDownloadRequestDTO downloadRequest) {
    HpcBulkDataObjectDownloadResponseDTO downloadResponse = null;
    try {
      downloadResponse = dataManagementBusService.downloadDataObjects(downloadRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(downloadResponse, false);
  }

  @Override
  public Response getDataObjectsDownloadStatus(String taskId) {
    HpcCollectionDownloadStatusDTO downloadStatus = null;
    try {
      downloadStatus = dataManagementBusService.getDataObjectsDownloadStatus(taskId);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(downloadStatus, true);
  }

  @Override
  public Response getDownloadSummary(Integer page, Boolean totalCount) {
    HpcDownloadSummaryDTO downloadSummary = null;
    try {
      downloadSummary =
          dataManagementBusService.getDownloadSummary(
              page != null ? page : 1, totalCount != null ? totalCount : false);

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
  public Response getDataManagementModel() {
    HpcDataManagementModelDTO docModel = null;
    try {
      docModel = dataManagementBusService.getDataManagementModel();

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(docModel, true);
  }

  @Override
  public Response renamePaths(HpcBulkRenameRequestDTO bulkRenameRequest) {
    HpcBulkRenameResponseDTO bulkRenameResponse = null;
    try {
      bulkRenameResponse = dataManagementBusService.renamePaths(bulkRenameRequest);

    } catch (HpcException e) {
      return errorResponse(e);
    }

    return okResponse(bulkRenameResponse, true);
  }

  //---------------------------------------------------------------------//
  // Helper Methods
  //---------------------------------------------------------------------//

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
  private Response downloadResponse(
      HpcDataObjectDownloadResponseDTO downloadResponse,
      MessageContext messageContext,
      Map<String, String> header) {
    if (downloadResponse == null) {
      return okResponse(null, false, header);
    }
    Response response = null;
    if (downloadResponse.getDestinationFile() != null) {
      // Put the download file on the message context, so the cleanup interceptor can
      // delete it after the file was received by the caller.
      messageContext.put(
          DATA_OBJECT_DOWNLOAD_FILE_MC_ATTRIBUTE, downloadResponse.getDestinationFile());
      response =
          okResponse(
              downloadResponse.getDestinationFile(),
              MediaType.APPLICATION_OCTET_STREAM_TYPE,
              header);
    } else {
      response = okResponse(downloadResponse, false, header);
    }
    return response;
  }
}
