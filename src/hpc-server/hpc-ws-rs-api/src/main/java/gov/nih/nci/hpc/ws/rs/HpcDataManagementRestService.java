/**
 * HpcDataManagementRestService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;

/**
 * <p>
 * HPC Data Management REST Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

@Path("/")
public interface HpcDataManagementRestService
{
    /**
     * Collection registration.
     *
     * @param path The collection path.
     * @param collectionRegistration A DTO contains the list of metadata entries to attach to the collection.
     * @return The REST service response.
     */
	@PUT
	@Path("/collection/{path:.*}")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response registerCollection(@PathParam("path") String path,
			                           HpcCollectionRegistrationDTO collectionRegistration);
	
    /**
     * Get a collection.
     *
     * @param path The collection path.
     * @param list An indicator to list sub-collections and data-objects.
     * @return The REST service response w/ HpcCollectionListDTO entity.
     * @see gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO
     */
	@GET
	@Path("/collection/{path:.*}")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getCollection(@PathParam("path") String path,
			                      @QueryParam("list") Boolean list);
	
	
    /**
     * Get a collection children. Collection metadata will not be returned
     *
     * @param path The collection path.
     * @return The REST service response w/ HpcCollectionListDTO entity.
     * @see gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO
     */
	@GET
	@Path("/collection/{path:.*}/children")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getCollectionChildren(@PathParam("path") String path);

	/**
     * Download a collection.
     *
     * @param path The collection path.
     * @param downloadRequest The download request.
     * @return The REST service response w/ HpcCollectionDownloadResponseDTO entity.
     */
	@POST
	@Path("/collection/{path:.*}/download")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response downloadCollection(@PathParam("path") String path,
			                           HpcDownloadRequestDTO downloadRequest);
	
    /**
     * Get collection download task status.
     *
     * @param taskId The collection download task ID.
     * @return The REST service response w/ HpcCollectionDownloadStatusDTO entity.
     */
	@GET
	@Path("/collection/download")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getCollectionDownloadStatus(@QueryParam("taskId") String taskId);
	
    /**
     * Delete a collection.
     *
     * @param path The collection path.
     * @return The REST service response.
     */
	@DELETE
	@Path("/collection/{path:.*}")
	public Response deleteCollection(@PathParam("path") String path);
	
    /**
     * Set a collection's permissions.
     *
     * @param path The collection path.
     * @param collectionPermissionsRequest Request to set collection permissions.
     * @return The REST service response w/ HpcEntityPermissionsResponseDTO entity.
     */
	@POST
	@Path("/collection/{path:.*}/acl")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
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
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getCollectionPermissions(@PathParam("path") String path);
	
    /**
     * Get a collection's permission for given a user.
     *
     * @param path The collection path.
     * @param userId The user id to get permissions for.
     * @return The REST service response w/ HpcUserPermissionDTO entity.
     */
	@GET
	@Path("/collection/{path:.*}/acl/user/{userId:.*}")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getCollectionPermission(@PathParam("path") String path, 
											@PathParam("userId") String userId);


    /**
     * Get the permissions that a given user has on a given set of collections.
     *
     * The userId is embedded in the URI, but the collections' paths are expected to be received as multiple query
     * string parameters, one per collection and each named collectionPath.
     *
     * @param collectionPaths The collections' paths.
     * @param userId The user id to get permissions for.
     * @return The REST service response with HpcUserPermissionsOnMultipleCollectionsDTO instance.
     */
	@GET
	@Path("/collection/acl/user/{userId:.*}")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getPermissionsOnCollectionsForUser(@QueryParam("collectionPath")  String[] collectionsPaths,
                                                       @PathParam("userId") String userId);

    /**
     * Get all permissions that are assigned for a given set of collections.
     *
     * @param collectionPaths The collections' paths.
     * @return The REST service response based on <code>HpcPermsForCollectionsDTO</code> instance.
     */
    @GET
    @Path("/collection/acl")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getAllPermissionsOnCollections(@QueryParam("collectionPath") String[] collectionPaths);


    /**
     * Data object registration.
     *
     * @param path The data object path.
     * @param dataObjectRegistration A DTO contains the metadata and data transfer locations.
     * @param dataObjectInputStream The data object input stream.
     * @return The REST service response.
     */
	@PUT
	@Path("/dataObject/{path:.*}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response registerDataObject(
			        @PathParam("path") String path,
			        @Multipart(value = "dataObjectRegistration", type = "application/json")
			        HpcDataObjectRegistrationDTO dataObjectRegistration,
			        @Multipart(value = "dataObject", type = "application/octet-stream", required = false)
			        InputStream dataObjectInputStream);
	
    /**
     * Data objects registration.
     *
     * @param bulkDataObjectRegistrationRequest The bulk registration request.
     * @return The REST service response.
     */
	@PUT
	@Path("/registration")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response registerDataObjects(
			                HpcBulkDataObjectRegistrationRequestDTO bulkDataObjectRegistrationRequest);
	
    /**
     * Get data objects list registration task status.
     *
     * @param taskId The registration task ID.
     * @return The REST service response w/ HpcDataObjectListRegistrationStatusDTO entity.
     */
	@GET
	@Path("/registration/{taskId}")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getDataObjectsRegistrationStatus(@PathParam("taskId") String taskId);

    /**
     * Get a data object.
     *
     * @param path The data object path.
     * @return The REST service response w/ HpcDataObjectListDTO entity.
     */
	@GET
	@Path("/dataObject/{path:.*}")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getDataObject(@PathParam("path") String path);
	
    /**
     * Download a data object.
     *
     * @param path The data object path.
     * @param downloadRequest The download request.
     * @param mc The message context.
     * @return The REST service response w/ either a file attached or HpcDownloadResponseDTO entity.
     */
	@POST
	@Path("/dataObject/{path:.*}/download")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML + "," +
			  MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadDataObject(@PathParam("path") String path,
			                           HpcDownloadRequestDTO downloadRequest,
			                           @Context MessageContext mc);
	
    /**
     * Get Data object download task status.
     *
     * @param taskId The data object download task ID.
     * @return The REST service response w/ HpcDataObjectDownloadStatusDTO entity.
     */
	@GET
	@Path("/dataObject/download")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getDataObjectDownloadStatus(@QueryParam("taskId") String taskId);
	
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
     * Set a data object's permissions.
     *
     * @param path The data object path.
     * @param dataObjectPermissionsRequest Request to set data object permissions.
     * @return The REST service response w/ HpcEntityPermissionsResponseDTO entity.
     */
	@POST
	@Path("/dataObject/{path:.*}/acl")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
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
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getDataObjectPermissions(@PathParam("path") String path);
	
    /**
     * Get a data object's permission for userId.
     *
     * @param path The data object path.
     * @param userId The userId to get the permission for.
     * @return The REST service response w/ HpcUserPermissionDTO entity.
     */
	@GET
	@Path("/dataObject/{path:.*}/acl/user/{userId:.*}")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getDataObjectPermission(@PathParam("path") String path, 
											@PathParam("userId") String userId);
	
	/**
     * Download a list of data objects.
     *
     * @param downloadRequest The download request.
     * @return The REST service response w/ HpcDataObjectsDownloadResponseDTO entity.
     */
	@POST
	@Path("/download")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response downloadDataObjects(HpcBulkDataObjectDownloadRequestDTO downloadRequest);
	
    /**
     * Get data objects download task status.
     *
     * @param taskId The download task ID.
     * @return The REST service response w/ HpcCollectionDownloadStatusDTO entity.
     */
	@GET
	@Path("/download/{taskId}")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getDataObjectsDownloadStatus(@PathParam("taskId") String taskId);
	
    /**
     * Get download summary (for the request invoker).
     *
     * @param page The requested results page.
     * @param totalCount If set to true, return the total count of completed tasks. All active tasks
     *                   are always returned.
     * @return The REST service response w/ HpcDownloadSummaryDTO entity.
     */
	@GET
	@Path("/download")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getDownloadSummary(@QueryParam("page") Integer page,
                                       @QueryParam("totalCount") Boolean totalCount);
	
    /**
     * Get data management model. This includes all rules.
     *
     * @return The REST service response w/ HpcDataManagementModelDTO entity.
     */
	@GET
	@Path("/dm/model")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getDataManagementModel();
}

