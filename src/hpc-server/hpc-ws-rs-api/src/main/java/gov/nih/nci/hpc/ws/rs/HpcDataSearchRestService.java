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

import gov.nih.nci.hpc.dto.datasearch.HpcCompoundMetadataQueryDTO;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
 */

@Path("/")
public interface HpcDataSearchRestService
{
    /**
     * Get collections by compound metadata query..
     *
     * @param compoundMetadataQuery A compund metadata query DTO.
     * @return The REST service response w/ HpcCollectionListDTO entity.
     */
	@POST
	@Path("/collection/query")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response queryCollections(HpcCompoundMetadataQueryDTO compoundMetadataQuery);
	
    /**
     * Get collections by named query.
     *
     * @param queryName A named query.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @param page The requested results page.
     * @param totalCount If set to true, return the total count of collections matching the query
     *                   regardless of the limit on returned entities.
     * @return The REST service response w/ HpcCollectionListDTO entity.
     */
	@GET
	@Path("/collection/query/{queryName}")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response queryCollections(@PathParam("queryName") String queryName,
			                         @QueryParam("detailedResponse") Boolean detailedResponse,
			                         @QueryParam("page") Integer page,
			                         @QueryParam("totalCount") Boolean totalCount);
	
    /**
     * Get data objects by compound metadata query.
     *
     * @param compoundMetadataQuery A compund metadata query DTO.
     * @return The REST service response w/ HpcDataObjectListDTO entity.
     */
	@POST
	@Path("/dataObject/query")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response queryDataObjects(HpcCompoundMetadataQueryDTO compoundMetadataQuery);
	
    /**
     * Get data objects by named query.
     *
     * @param queryName A named query.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @param page The requested results page.
     * @param totalCount If set to true, return the total count of collections matching the query
     *                   regardless of the limit on returned entities.
     * @return The REST service response w/ HpcDataObjectListDTO entity.
     */
	@GET
	@Path("/dataObject/query/{queryName}")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response queryDataObjects(@PathParam("queryName") String queryName,
			                         @QueryParam("detailedResponse") Boolean detailedResponse,
			                         @QueryParam("page") Integer page,
			                         @QueryParam("totalCount") Boolean totalCount);

    /**
     * Add a new named query.
     *
     * @param queryName The query name.
     * @param compoundMetadataQuery The compound metadata query DTO.
     * @return The REST service response.
     */
    @PUT
    @Path("/query/{queryName}")
    @Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response addQuery(@PathParam("queryName") String queryName,
    		                 HpcCompoundMetadataQueryDTO compoundMetadataQuery);
    
    /**
     * Update a named query.
     *
     * @param queryName The query name.
     * @param compoundMetadataQuery The compound metadata query DTO.
     * @return The REST service response.
     */
    @POST
    @Path("/query/{queryName}")
    @Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response updateQuery(@PathParam("queryName") String queryName,
    		                    HpcCompoundMetadataQueryDTO compoundMetadataQuery);
    
    /**
     * Delete a named query.
     *
     * @param queryName The query name.
     * @return The REST service response.
     */
    @DELETE
    @Path("/query/{queryName}")
    public Response deleteQuery(@PathParam("queryName") String queryName);

    /**
     * Get a named query.
     *
     * @param queryName The query name.
     * @return The REST service response w/ HpcNamedCompoundMetadataQueryDTO entity. 
     */
    @GET
    @Path("/query/{queryName}")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getQuery(@PathParam("queryName") String queryName);
    
    /**
     * Get all saved queries.
     *
     * @return The REST service response w/ HpcNamedCompoundMetadataQueryListDTO entity.
     */
    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getQueries();
	
  /**
   * Get a list of metadata attributes currently registered.
   *
   * @param levelLabel Filter the results by level label (Optional).
   * @return The REST service response w/ HpcMetadataAttributesListDTO entity.
   */
	@GET
	@Path("/metadataAttributes")
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response getMetadataAttributes(@QueryParam("levelLabel") String levelLabel);
	
    /**
     * Refresh the metadata views.
     *
     * @return The REST service response.
     */
    @POST
    @Path("/refreshMetadataViews")
    public Response refreshMetadataViews();
}

