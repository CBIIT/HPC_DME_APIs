/**
 * HpcDataManagementRestService.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkMoveRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompleteMultipartUploadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;

/**
 * HPC Data Management REST Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
@Path("/")
public interface HpcDataManagementRestService {
	/**
	 * Examine whether path refers to collection or data file.
	 *
	 * @param path The path.
	 * @return The REST service response.
	 */
	@GET
	@Path("/pathRefType/{path:.*}")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response interrogatePathRef(@PathParam("path") String path);

	/**
	 * Collection registration.
	 *
	 * @param path                   The collection path.
	 * @param collectionRegistration A DTO contains the list of metadata entries to
	 *                               attach to the collection.
	 * @return The REST service response.
	 */
	@PUT
	@Path("/collection/{path:.*}")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response registerCollection(@PathParam("path") String path,
			HpcCollectionRegistrationDTO collectionRegistration);

	/**
	 * Get a collection.
	 *
	 * @param path       The collection path.
	 * @param list       An indicator to list sub-collections and data-objects.
	 * @param includeAcl Flag to include ACL.
	 * @return The REST service response w/ HpcCollectionListDTO entity.
	 * @see gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO
	 */
	@GET
	@Path("/collection/{path:.*}")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getCollection(@PathParam("path") String path, @QueryParam("list") Boolean list,
			@QueryParam("includeAcl") Boolean includeAcl);

	/**
	 * Get a collection children. Collection metadata will not be returned
	 *
	 * @param path The collection path.
	 * @return The REST service response w/ HpcCollectionListDTO entity.
	 * @see gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO
	 */
	@GET
	@Path("/collection/{path:.*}/children")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getCollectionChildren(@PathParam("path") String path);

	/**
	 * Download a collection.
	 *
	 * @deprecated
	 * @param path            The collection path.
	 * @param downloadRequest The download request.
	 * @return The REST service response w/ HpcCollectionDownloadResponseDTO entity.
	 */
	@Deprecated
	@POST
	@Path("/collection/{path:.*}/download")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response downloadCollection(@PathParam("path") String path, HpcDownloadRequestDTO downloadRequest);

	/**
	 * Download a collection.
	 *
	 * @param path            The collection path.
	 * @param downloadRequest The download request.
	 * @return The REST service response w/ HpcCollectionDownloadResponseDTO entity.
	 */
	@POST
	@Path("/v2/collection/{path:.*}/download")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response downloadCollection(@PathParam("path") String path,
			gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO downloadRequest);

	/**
	 * Get collection download task status.
	 *
	 * @param taskId The collection download task ID.
	 * @return The REST service response w/ HpcCollectionDownloadStatusDTO entity.
	 */
	@Deprecated
	@GET
	@Path("/collection/download")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getCollectionDownloadStatusV1(@QueryParam("taskId") String taskId);

	/**
	 * Get collection download task status.
	 *
	 * @param taskId The collection download task ID.
	 * @return The REST service response w/ HpcCollectionDownloadStatusDTO entity.
	 */
	@GET
	@Path("/collection/download/{taskId}")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getCollectionDownloadStatus(@PathParam("taskId") String taskId);

	/**
	 * Cancel a collection download task status.
	 *
	 * @param taskId The collection download task ID.
	 * @return The REST service response w/o entity.
	 */
	@POST
	@Path("/collection/download/{taskId}/cancel")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response cancelCollectionDownloadTask(@PathParam("taskId") String taskId);

	/**
	 * Delete a collection.
	 *
	 * @param path      The collection path.
	 * @param recursive If true, delete all sub collections and data objects in this
	 *                  collection.
	 * @return The REST service response.
	 */
	@DELETE
	@Path("/collection/{path:.*}")
	public Response deleteCollection(@PathParam("path") String path, @QueryParam("recursive") Boolean recursive);

	/**
	 * Move a collection.
	 *
	 * @param path            The collection path.
	 * @param destinationPath The destination path to move to.
	 * @return The REST service response.
	 */
	@POST
	@Path("/collection/{path:.*}/move/{destinationPath}")
	public Response moveCollection(@PathParam("path") String path,
			@PathParam("destinationPath") String destinationPath);

	/**
	 * Set a collection's permissions.
	 *
	 * @param path                         The collection path.
	 * @param collectionPermissionsRequest Request to set collection permissions.
	 * @return The REST service response w/ HpcEntityPermissionsResponseDTO entity.
	 */
	@POST
	@Path("/collection/{path:.*}/acl")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response setCollectionPermissions(@PathParam("path") String path,
			HpcEntityPermissionsDTO collectionPermissionsRequest);

	/**
	 * Get a collection's permissions.
	 *
	 * @param path The collection path.
	 * @return The REST service response w/ HpcEntityPermissionsDTO entity.
	 */
	@GET
	@Path("/collection/{path:.*}/acl")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getCollectionPermissions(@PathParam("path") String path);

	/**
	 * Get a collection's permission for given a user.
	 *
	 * @param path   The collection path.
	 * @param userId The user id to get permissions for.
	 * @return The REST service response w/ HpcUserPermissionDTO entity.
	 */
	@GET
	@Path("/collection/{path:.*}/acl/user/{userId:.*}")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getCollectionPermission(@PathParam("path") String path, @PathParam("userId") String userId);

	/**
	 * Get the permissions that a given user has on a given set of collections.
	 *
	 * <p>
	 * The userId is embedded in the URI, but the collections' paths are expected to
	 * be received as multiple query string parameters, one per collection and each
	 * named collectionPath.
	 *
	 * @param collectionsPaths The collections' paths.
	 * @param userId           The user id to get permissions for.
	 * @return The REST service response with
	 *         HpcUserPermissionsOnMultipleCollectionsDTO instance.
	 */
	@GET
	@Path("/collection/acl/user/{userId:.*}")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getPermissionsOnCollectionsForUser(@QueryParam("collectionPath") String[] collectionsPaths,
			@PathParam("userId") String userId);

	/**
	 * Get all permissions that are assigned for a given set of collections.
	 *
	 * @param collectionPaths The collections' paths.
	 * @return The REST service response based on
	 *         <code>HpcPermsForCollectionsDTO</code> instance.
	 */
	@GET
	@Path("/collection/acl")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getAllPermissionsOnCollections(@QueryParam("collectionPath") String[] collectionPaths);

	/**
	 * Data object registration.
	 *
	 * @deprecated
	 * @param path                   The data object path.
	 * @param dataObjectRegistration A DTO contains the metadata and data transfer
	 *                               locations.
	 * @param dataObjectInputStream  The data object input stream.
	 * @return The REST service response.
	 */
	@Deprecated
	@PUT
	@Path("/dataObject/{path:.*}")
	@Consumes("multipart/form-data")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response registerDataObject(@PathParam("path") String path,
			@Multipart(value = "dataObjectRegistration", type = "application/json") HpcDataObjectRegistrationRequestDTO dataObjectRegistration,
			@Multipart(value = "dataObject", type = "application/octet-stream", required = false) InputStream dataObjectInputStream);

	/**
	 * Data object registration.
	 *
	 * @param path                   The data object path.
	 * @param dataObjectRegistration A DTO contains the metadata and data transfer
	 *                               locations.
	 * @param dataObjectInputStream  The data object input stream.
	 * @return The REST service response.
	 */
	@PUT
	@Path("/v2/dataObject/{path:.*}")
	@Consumes("multipart/form-data")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response registerDataObject(@PathParam("path") String path,
			@Multipart(value = "dataObjectRegistration", type = "application/json") gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationRequestDTO dataObjectRegistration,
			@Multipart(value = "dataObject", type = "application/octet-stream", required = false) InputStream dataObjectInputStream);

	/**
	 * Complete S3 multipart upload for a data object.
	 *
	 * @param path                           The data object path to complete the
	 *                                       multipart upload for for.
	 * @param completeMultipartUploadRequest The multipart upload completion
	 *                                       request.
	 * @return The REST service response w/o entity.
	 */
	@POST
	@Path("/dataObject/{path:.*}/completeMultipartUpload")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response completeMultipartUpload(@PathParam("path") String path,
			HpcCompleteMultipartUploadRequestDTO completeMultipartUploadRequest);

	/**
	 * Data objects registration.
	 *
	 * @deprecated
	 * @param bulkDataObjectRegistrationRequest The bulk registration request.
	 * @return The REST service response w/ HpcBulkDataObjectRegistrationResponseDTO
	 *         entity.
	 */
	@Deprecated
	@PUT
	@Path("/registration")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response registerDataObjects(HpcBulkDataObjectRegistrationRequestDTO bulkDataObjectRegistrationRequest);

	/**
	 * Data objects registration.
	 *
	 * @param bulkDataObjectRegistrationRequest The bulk registration request.
	 * @return The REST service response w/ HpcBulkDataObjectRegistrationResponseDTO
	 *         entity.
	 */
	@PUT
	@Path("/v2/registration")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response registerDataObjects(
			gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO bulkDataObjectRegistrationRequest);

	/**
	 * Get bulk data object registration task status.
	 *
	 * @deprecated
	 * @param taskId The registration task ID.
	 * @return The REST service response w/ HpcDataObjectListRegistrationStatusDTO
	 *         entity.
	 */
	@Deprecated
	@GET
	@Path("/registration/{taskId}")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getDataObjectsRegistrationStatusV1(@PathParam("taskId") String taskId);

	/**
	 * Get bulk data object registration task status.
	 *
	 * @param taskId The registration task ID.
	 * @return The REST service response w/ HpcDataObjectListRegistrationStatusDTO
	 *         entity.
	 */
	@GET
	@Path("/v2/registration/{taskId}")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getDataObjectsRegistrationStatus(@PathParam("taskId") String taskId);

	/**
	 * Get data objects registration summary (for the request invoker).
	 *
	 * @deprecated
	 * @param page       The requested results page.
	 * @param totalCount If set to true, return the total count of completed tasks.
	 *                   All active tasks are always returned.
	 * @return The REST service response w/ HpcBulkDataObjectRegistrationSummaryDTO
	 *         entity.
	 */
	@Deprecated
	@GET
	@Path("/registration")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getRegistrationSummaryV1(@QueryParam("page") Integer page,
			@QueryParam("totalCount") Boolean totalCount);

	/**
	 * Get data objects registration summary (for the request invoker).
	 *
	 * @param page       The requested results page.
	 * @param totalCount If set to true, return the total count of completed tasks.
	 *                   All active tasks are always returned.
	 * @return The REST service response w/ HpcBulkDataObjectRegistrationSummaryDTO
	 *         entity.
	 */
	@GET
	@Path("/v2/registration")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getRegistrationSummary(@QueryParam("page") Integer page,
			@QueryParam("totalCount") Boolean totalCount);

	/**
	 * Get a data object.
	 *
	 * @deprecated
	 * @param path       The data object path.
	 * @param includeAcl Flag to include ACL.
	 * @return The REST service response w/ HpcDataObjectListDTO entity.
	 */
	@Deprecated
	@GET
	@Path("/dataObject/{path:.*}")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getDataObjectV1(@PathParam("path") String path, @QueryParam("includeAcl") Boolean includeAcl);

	/**
	 * Get a data object.
	 *
	 * @param path       The data object path.
	 * @param includeAcl Flag to include ACL.
	 * @return The REST service response w/ HpcDataObjectListDTO entity.
	 */
	@GET
	@Path("/v2/dataObject/{path:.*}")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getDataObject(@PathParam("path") String path, @QueryParam("includeAcl") Boolean includeAcl);

	/**
	 * Download a data object.
	 *
	 * @deprecated
	 * @param path            The data object path.
	 * @param downloadRequest The download request.
	 * @param mc              The message context.
	 * @return The REST service response w/ either a file attached or
	 *         HpcDataObjectDownloadResponseDTO entity.
	 */
	@Deprecated
	@POST
	@Path("/dataObject/{path:.*}/download")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8, application/octet-stream")
	public Response downloadDataObject(@PathParam("path") String path, HpcDownloadRequestDTO downloadRequest,
			@Context MessageContext mc);

	/**
	 * Generate a download request URL.
	 *
	 * @param path The data object path to generate the download URL for.
	 * @return The REST service response w/ HpcDataObjectDownloadResponseDTO entity.
	 */
	@POST
	@Path("/dataObject/{path:.*}/generateDownloadRequestURL")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response generateDownloadRequestURL(@PathParam("path") String path);

	/**
	 * Download a data object.
	 *
	 * @param path            The data object path.
	 * @param downloadRequest The download request.
	 * @param mc              The message context.
	 * @return The REST service response w/ either a file attached or
	 *         HpcDataObjectDownloadResponseDTO entity.
	 */
	@POST
	@Path("/v2/dataObject/{path:.*}/download")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8, application/octet-stream")
	public Response downloadDataObject(@PathParam("path") String path,
			gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO downloadRequest, @Context MessageContext mc);

	/**
	 * Get Data object download task status.
	 *
	 * @param taskId The data object download task ID.
	 * @return The REST service response w/ HpcDataObjectDownloadStatusDTO entity.
	 */
	@Deprecated
	@GET
	@Path("/dataObject/download")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getDataObjectDownloadStatusV1(@QueryParam("taskId") String taskId);

	/**
	 * Get Data object download task status.
	 *
	 * @param taskId The data object download task ID.
	 * @return The REST service response w/ HpcDataObjectDownloadStatusDTO entity.
	 */
	@GET
	@Path("/dataObject/download/{taskId}")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getDataObjectDownloadStatus(@PathParam("taskId") String taskId);

	/**
	 * Delete a data object.
	 *
	 * @param path The data object path.
	 * @return The REST service response.
	 */
	@DELETE
	@Path("/dataObject/{path:.*}")
	public Response deleteDataObject(@PathParam("path") String path);

	/**
	 * Move a data object.
	 *
	 * @param path            The data object path.
	 * @param destinationPath The destination path to move to.
	 * @return The REST service response.
	 */
	@POST
	@Path("/dataObject/{path:.*}/move/{destinationPath}")
	public Response moveDataObject(@PathParam("path") String path,
			@PathParam("destinationPath") String destinationPath);

	/**
	 * Set a data object's permissions.
	 *
	 * @param path                         The data object path.
	 * @param dataObjectPermissionsRequest Request to set data object permissions.
	 * @return The REST service response w/ HpcEntityPermissionsResponseDTO entity.
	 */
	@POST
	@Path("/dataObject/{path:.*}/acl")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response setDataObjectPermissions(@PathParam("path") String path,
			HpcEntityPermissionsDTO dataObjectPermissionsRequest);

	/**
	 * Get a data object's permissions.
	 *
	 * @param path The data object path.
	 * @return The REST service response w/ HpcEntityPermissionsDTO entity.
	 */
	@GET
	@Path("/dataObject/{path:.*}/acl")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getDataObjectPermissions(@PathParam("path") String path);

	/**
	 * Get a data object's permission for userId.
	 *
	 * @param path   The data object path.
	 * @param userId The userId to get the permission for.
	 * @return The REST service response w/ HpcUserPermissionDTO entity.
	 */
	@GET
	@Path("/dataObject/{path:.*}/acl/user/{userId:.*}")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getDataObjectPermission(@PathParam("path") String path, @PathParam("userId") String userId);

	/**
	 * Download a list of data objects.
	 *
	 * @deprecated
	 * @param downloadRequest The download request.
	 * @return The REST service response w/ HpcDataObjectsDownloadResponseDTO
	 *         entity.
	 */
	@Deprecated
	@POST
	@Path("/download")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response downloadDataObjects(HpcBulkDataObjectDownloadRequestDTO downloadRequest);

	/**
	 * Download a list of data objects or a list of collections.
	 *
	 * @param downloadRequest The download request.
	 * @return The REST service response w/ HpcDataObjectsDownloadResponseDTO
	 *         entity.
	 */
	@POST
	@Path("/v2/download")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response downloadDataObjectsOrCollections(
			gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectDownloadRequestDTO downloadRequest);

	/**
	 * Get download task status of a list of data objects or a list of collections.
	 *
	 * @param taskId The download task ID.
	 * @return The REST service response w/ HpcCollectionDownloadStatusDTO entity.
	 */
	@GET
	@Path("/download/{taskId}")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getDataObjectsOrCollectionsDownloadStatus(@PathParam("taskId") String taskId);

	/**
	 * Cancel download task of a list of data objects or a list of collections.
	 *
	 * @param taskId The download task ID.
	 * @return The REST service response w/o entity.
	 */
	@POST
	@Path("/download/{taskId}/cancel")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response cancelDataObjectsOrCollectionsDownloadTask(@PathParam("taskId") String taskId);

	/**
	 * Get download summary (for the request invoker).
	 *
	 * @param page       The requested results page.
	 * @param totalCount If set to true, return the total count of completed tasks.
	 *                   All active tasks are always returned.
	 * @return The REST service response w/ HpcDownloadSummaryDTO entity.
	 */
	@GET
	@Path("/download")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getDownloadSummary(@QueryParam("page") Integer page, @QueryParam("totalCount") Boolean totalCount);

	/**
	 * Get data management model. This includes all rules.
	 *
	 * @return The REST service response w/ HpcDataManagementModelDTO entity.
	 */
	@GET
	@Path("/dm/model")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response getDataManagementModels();

	/**
	 * Get data management model. This includes all rules.
	 *
	 * @param basePath The base path.
	 * @return The REST service response w/ HpcDataManagementModelDTO entity.
	 */
	@GET
	@Path("/dm/model/{basePath}")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	Response getDataManagementModel(@PathParam("basePath") String basePath);

	/**
	 * Move a list of data objects and/or collections.
	 *
	 * @param bulkMoveRequest The bulk data objects / collections move request.
	 * @return The REST service response w/ HpcBulkMoveResponseDTO entity.
	 */
	@POST
	@Path("/move")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response movePaths(HpcBulkMoveRequestDTO bulkMoveRequest);
}
