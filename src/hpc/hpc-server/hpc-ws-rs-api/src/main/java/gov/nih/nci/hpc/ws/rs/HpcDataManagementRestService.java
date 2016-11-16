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

import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionRequestDTO;
import gov.nih.nci.hpc.dto.metadata.HpcMetadataQueryParam;

import java.io.InputStream;
import java.util.List;

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

/**
 * <p>
 * HPC Data Management REST Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

@Path("/")
public interface HpcDataManagementRestService
{
    /**
     * PUT Collection registration request.
     *
     * @param path The collection path.
     * @param metadataEntries A list of metadata entries to attach to the collection.
     * @return Response The REST service response.
     */
	@PUT
	@Path("/collection/{path:.*}")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response registerCollection(
			           @PathParam("path") String path,
			           List<HpcMetadataEntry> metadataEntries);

    /**
     * GET Collection.
     *
     * @param path The collection path.
     * @return Response The REST service response.
     */
	@GET
	@Path("/collection/{path:.*}")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getCollection(@PathParam("path") String path);

    /**
     * GET Collections by metadata query.
     *
     * @param metadataQueries A list of metadata entries to query for.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @param page The requested results page.
     * @return Response The REST service response.
     */
	@GET
	@Path("/collection")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getCollections(
			           @QueryParam("metadataQuery") List<HpcMetadataQueryParam> metadataQueries,
			           @QueryParam("detailedResponse") Boolean detailedResponse,
			           @QueryParam("page") Integer page);
	
    /**
     * POST Collections query.
     *
     * @param metadataQueries A list of metadata entries to query for. 
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @param page The requested results page.
     * @return Response The REST service response.
     */
	@POST
	@Path("/collection/query/simple")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response queryCollections(List<HpcMetadataQuery> metadataQueries,
			                         @QueryParam("detailedResponse") Boolean detailedResponse,
			                         @QueryParam("page") Integer page);
	
    /**
     * POST Collections query.
     *
     * @param compoundMetadataQueryDTO A compund metadata query DTO.
     * @return Response The REST service response.
     */
	@POST
	@Path("/collection/query/compound")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response queryCollections(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO);
	
    /**
     * GET Collections query.
     *
     * @param name A named query.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @param page The requested results page.
     * @return Response The REST service response.
     */
	@GET
	@Path("/collection/query/compound/{queryName}")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response queryCollections(@PathParam("queryName") String queryName,
			                         @QueryParam("detailedResponse") Boolean detailedResponse,
			                         @QueryParam("page") Integer page);

    /**
     * PUT Data object registration request.
     *
     * @param path The data object path.
     * @param dataObjectRegistration A DTO contains the metadata and data transfer locations.
     * @param dataObjectInputStream The data object input stream.
     * @return Response The REST service response.
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
     * GET Data Object.
     *
     * @param path The data object path.
     * @return Response The REST service response.
     */
	@GET
	@Path("/dataObject/{path:.*}")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getDataObject(@PathParam("path") String path);

    /**
     * GET Data objects by metadata query.
     *
     * @param metadataQueries A list of metadata entries to query for.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @param page The requested results page.
     * @return Response The REST service response.
     */
	@GET
	@Path("/dataObject")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getDataObjects(
			           @QueryParam("metadataQuery") List<HpcMetadataQueryParam> metadataQueries,
			           @QueryParam("detailedResponse") Boolean detailedResponse,
			           @QueryParam("page") Integer page);
	
    /**
     * POST Data objects query.
     *
     * @param metadataQueries A list of metadata entries to query for.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @param page The requested results page.
     * @return Response The REST service response.
     */
	@POST
	@Path("/dataObject/query/simple")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response queryDataObjects(List<HpcMetadataQuery> metadataQueries,
			                         @QueryParam("detailedResponse") Boolean detailedResponse,
			                         @QueryParam("page") Integer page);
	
    /**
     * POST Data objects query.
     *
     * @param compoundMetadataQueryDTO A compund metadata query DTO.
     * @return Response The REST service response.
     */
	@POST
	@Path("/dataObject/query/compound")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response queryDataObjects(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO);
	
    /**
     * GET Data objects query.
     *
     * @param name A named query
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @param page The requested results page.
     * @return Response The REST service response.
     */
	@GET
	@Path("/dataObject/query/compound/{queryName}")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response queryDataObjects(@PathParam("queryName") String queryName,
			                         @QueryParam("detailedResponse") Boolean detailedResponse,
			                         @QueryParam("page") Integer page);

    /**
     * POST Download Data Object.
     *
     * @param path The data object path.
     * @param downloadRequest The download request.
     * @return Response The REST service response.
     */
	@POST
	@Path("/dataObject/{path:.*}/download")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML + "," +
			  MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadDataObject(@PathParam("path") String path,
			                           HpcDataObjectDownloadRequestDTO downloadRequest,
			                           @Context MessageContext mc);

    /**
     * POST Set permissions.
     *
     * @param entityPermissionRequests Requests to set entities (Collections or Data Objects) permissions.
     * @return Response The REST service response.
     */
	@POST
	@Path("/acl")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response setPermissions(List<HpcEntityPermissionRequestDTO> entityPermissionRequests);
	
    /**
     * GET Data Management Model (Metadata validation rules and hierarchy definition).
     *
     * @param doc The DOC to get the model for.
     * @return Response The REST service response.
     */
	@GET
	@Path("/model/{doc}")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getDataManagementModel(@PathParam("doc") String doc);
	
    /**
     * GET A list of metadata attributes currently registered.
     *
     * @param level Filter the results by level.
     * @param levelOperatorStr The operator to use in the level filter.
     * @return Response The REST service response.
     */
	@GET
	@Path("/metadataAttributes")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getMetadataAttributes(
			           @QueryParam("level") Integer level,
			           @QueryParam("levelOperator") String levelOperatorStr);
	
    /**
     * Save a query.
     *
     * @param queryName The query name.
     * @param updateUserRequestDTO The update request DTO.
     * @return Response The REST service response.
     */
    @POST
    @Path("/query/{queryName}")
    @Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response saveQuery(@PathParam("queryName") String queryName,
    		                  HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO);
    
    /**
     * Delete a query.
     *
     * @param queryName The query name.
     * @return Response The REST service response.
     */
    @DELETE
    @Path("/query/{queryName}")
    public Response deleteQuery(@PathParam("queryName") String queryName);

    /**
     * Get all saved queries for a user.
     *
     * @return Response The REST service response.
     */
    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getQueries();
}

