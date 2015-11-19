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
import gov.nih.nci.hpc.dto.dataset.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.metadata.HpcMetadataQueryParam;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

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
     */
	@PUT
	@Path("/collection/{path:.*}")
	@Consumes("application/json,application/xml")
	@Produces("application/json,application/xml")
	public Response registerCollection(
			           @PathParam("path") String path,
			           List<HpcMetadataEntry> metadataEntries);
	
    /**
     * GET Collections by metadata query.
     *
     * @param metadataEntryQueries A list of metadata entries to query for.
     */
	@GET
	@Path("/collection")
	@Produces("application/json,application/xml")
	public Response getCollections(
			           @QueryParam("metadataQuery")
			           List<HpcMetadataQueryParam> metadataQueries);
	
    /**
     * PUT Data object registration request.
     *
     * @param path The data object path.
     * @param dataObjectRegistrationDTO A DTO contains the metadata and data transfer locations.
     */
	@PUT
	@Path("/dataObject/{path:.*}")
	@Consumes("application/json,application/xml")
	@Produces("application/json,application/xml")
	public Response registerDataObject(
			           @PathParam("path") String path,
			           HpcDataObjectRegistrationDTO dataObjectRegistrationDTO);
}

 