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
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcGroupRequestDTO;
import gov.nih.nci.hpc.dto.metadata.HpcMetadataQueryParam;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
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
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getCollection(@PathParam("path") String path);

    /**
     * GET Collections by metadata query.
     *
     * @param metadataEntryQueries A list of metadata entries to query for.
     * @return Response The REST service response.
     */
	@GET
	@Path("/collection")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getCollections(
			           @QueryParam("metadataQuery")
			           List<HpcMetadataQueryParam> metadataQueries);

    /**
     * PUT Data object registration request.
     *
     * @param path The data object path.
     * @param dataObjectRegistration A DTO contains the metadata and data transfer locations.
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
	                   Attachment dataObject);

    /**
     * GET Data Object.
     *
     * @param path The data object path.
     * @return Response The REST service response.
     */
	@GET
	@Path("/dataObject/{path:.*}")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getDataObject(@PathParam("path") String path);

    /**
     * GET Data objects by metadata query.
     *
     * @param metadataEntryQueries A list of metadata entries to query for.
     */
	@GET
	@Path("/dataObject")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getDataObjects(
			           @QueryParam("metadataQuery")
			           List<HpcMetadataQueryParam> metadataQueries);

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
			                           HpcDataObjectDownloadRequestDTO downloadRequest);

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
}

