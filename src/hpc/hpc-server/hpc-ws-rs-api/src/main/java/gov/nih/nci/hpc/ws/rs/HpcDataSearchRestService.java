/**
 * HpcDataSearchRestService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs;

import gov.nih.nci.hpc.dto.datamanagement.HpcCompoundMetadataQueryDTO;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * <p>
 * HPC Data Search REST Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

@Path("/")
public interface HpcDataSearchRestService
{
    /**
     * POST Collections query.
     *
     * @param compoundMetadataQueryDTO A compund metadata query DTO.
     * @return The REST service response.
     */
	@POST
	@Path("/collection/query/compound")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response queryCollections(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO);
	
    /**
     * GET Collections by named query.
     *
     * @param queryName A named query.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @param page The requested results page.
     * @return The REST service response.
     */
	@GET
	@Path("/collection/query/compound/{queryName}")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response queryCollections(@PathParam("queryName") String queryName,
			                         @QueryParam("detailedResponse") Boolean detailedResponse,
			                         @QueryParam("page") Integer page);
	
    /**
     * POST Data objects query.
     *
     * @param compoundMetadataQueryDTO A compund metadata query DTO.
     * @return The REST service response.
     */
	@POST
	@Path("/dataObject/query/compound")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response queryDataObjects(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO);
	
    /**
     * GET Data objects by named query.
     *
     * @param queryName A named query.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @param page The requested results page.
     * @return The REST service response.
     */
	@GET
	@Path("/dataObject/query/compound/{queryName}")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response queryDataObjects(@PathParam("queryName") String queryName,
			                         @QueryParam("detailedResponse") Boolean detailedResponse,
			                         @QueryParam("page") Integer page);

    /**
     * Save a query.
     *
     * @param queryName The query name.
     * @param compoundMetadataQueryDTO The compound metadata query DTO..
     * @return The REST service response.
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
     * @return The REST service response.
     */
    @DELETE
    @Path("/query/{queryName}")
    public Response deleteQuery(@PathParam("queryName") String queryName);

    /**
     * Get all saved queries for a user.
     *
     * @return The REST service response.
     */
    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getQueries();
    
	
  /**
   * GET A list of metadata attributes currently registered.
   *
   * @param level Filter the results by level.
   * @param levelOperatorStr The operator to use in the level filter.
   * @return The REST service response.
   */
	@GET
	@Path("/metadataAttributes")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getMetadataAttributes(
			           @QueryParam("level") Integer level,
			           @QueryParam("levelOperator") String levelOperatorStr);
	
    /**
     * Refresh the metadata views.
     *
     * @return The REST service response.
     */
    @POST
    @Path("/refreshMetadataViews")
    public Response refreshMetadataViews();
}

