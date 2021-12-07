/**
 * HpcDataSearchRestService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import gov.nih.nci.hpc.dto.catalog.HpcCatalogRequestDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcCompoundMetadataQueryDTO;

/**
 * HPC Data Search REST Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
@Path("/")
public interface HpcDataSearchRestService {
  /**
   * Get collections by compound metadata query..
   *
   * @param compoundMetadataQuery A compund metadata query DTO.
   * @return The REST service response w/ HpcCollectionListDTO entity.
   */
  @POST
  @Path("/collection/query")
  @Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response queryCollections(HpcCompoundMetadataQueryDTO compoundMetadataQuery);

  /**
   * Get collections by named query.
   *
   * @param queryName A named query.
   * @param detailedResponse If set to true, return entity details (attributes + metadata).
   * @param page The requested results page.
   * @param pageSize The requested page size.
   * @param totalCount If set to true, return the total count of collections matching the query
   *     regardless of the limit on returned entities.
   * @return The REST service response w/ HpcCollectionListDTO entity.
   */
  @GET
  @Path("/collection/query/{queryName}")
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response queryCollections(
      @PathParam("queryName") String queryName,
      @QueryParam("detailedResponse") Boolean detailedResponse,
      @QueryParam("page") Integer page,
      @QueryParam("pageSize") Integer pageSize,
      @QueryParam("totalCount") Boolean totalCount);


  /**
   * Get data objects by compound metadata query.
   *
   * @param returnParent If true, return the parent collections instead
   * 					 of the data objects themselves.
   * @param compoundMetadataQuery A compound metadata query DTO.
   * @return The REST service response w/ HpcDataObjectListDTO entity.
   */
  @POST
  @Path("/dataObject/query")
  @Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response queryDataObjects(@QueryParam("returnParent") Boolean returnParent,
		  HpcCompoundMetadataQueryDTO compoundMetadataQuery);


  /**
   * Get data objects within the requested path by compound metadata query.
   *
   * @param path The path to search in.
   * @param returnParent If true, return the parent collections instead the
   * 					 of data objects themselves.
   * @param compoundMetadataQuery A compound metadata query DTO.
   * @return The REST service response w/ HpcDataObjectListDTO entity.
   */
  @POST
  @Path("/dataObject/query/{path:.*}")
  @Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response queryDataObjectsInPath(
	  @PathParam("path") String path,
	  @QueryParam("returnParent") Boolean returnParent,
      HpcCompoundMetadataQueryDTO compoundMetadataQuery);
  
  /**
   * Get all data objects.
   *
   * @param page The requested results page.
   * @param pageSize The requested results page size.
   * @param totalCount If set to true, return the total count.
   * @return The REST service response w/ HpcDataObjectListDTO entity.
   */
  @GET
  @Path("/dataObject/query/all")
  @Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response queryAllDataObjects(
	  @QueryParam("page") Integer page,
	  @QueryParam("pageSize") Integer pageSize,
	  @QueryParam("totalCount") Boolean totalCount);
  
  /**
   * Get all data objects within the requested path.
   *
   * @param path The path to search in.
   * @param page The requested results page.
   * @param pageSize The requested results page size.
   * @param totalCount If set to true, return the total count.
   * @return The REST service response w/ HpcDataObjectListDTO entity.
   */
  @GET
  @Path("/dataObject/query/all/{path:.*}")
  @Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response queryAllDataObjectsInPath(
	  @PathParam("path") String path,
	  @QueryParam("page") Integer page,
	  @QueryParam("pageSize") Integer pageSize,
	  @QueryParam("totalCount") Boolean totalCount);
  

  /**
   * Get data objects by named query.
   *
   * @param queryName A named query.
   * @param detailedResponse If set to true, return entity details (attributes + metadata).
   * @param page The requested results page.
   * @param pageSize The requested page size.
   * @param totalCount If set to true, return the total count of collections matching
   *                   the query regardless of the limit on returned entities.
   * @param returnParent If true, return the parent collections instead of the
   * 					 data objects themselves.
   * @return The REST service response w/ HpcDataObjectListDTO entity.
   */
  @GET
  @Path("/dataObject/query/{queryName}")
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response queryDataObjects(
      @PathParam("queryName") String queryName,
      @QueryParam("detailedResponse") Boolean detailedResponse,
      @QueryParam("page") Integer page,
      @QueryParam("pageSize") Integer pageSize,
      @QueryParam("totalCount") Boolean totalCount,
      @QueryParam("returnParent") Boolean returnParent);
  
  

  /**
   * Add a new named query.
   *
   * @param queryName The query name.
   * @param compoundMetadataQuery The compound metadata query DTO.
   * @return The REST service response.
   */
  @PUT
  @Path("/query/{queryName}")
  @Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response addQuery(
      @PathParam("queryName") String queryName, HpcCompoundMetadataQueryDTO compoundMetadataQuery);



  /**
   * Add a new named query.
   *
   * @param queryName The query name.
   * @param userId The user id.
   * @param compoundMetadataQuery The compound metadata query DTO.
   * @return The REST service response.
   */
  @PUT
  @Path("/query/{queryName:.*}/{userId:.*}")
  @Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response addQueryByUser(
      @PathParam("queryName") String queryName, @PathParam("userId") String userId, HpcCompoundMetadataQueryDTO compoundMetadataQuery);



  /**
   * Update a named query.
   *
   * @param queryName The query name.
   * @param compoundMetadataQuery The compound metadata query DTO.
   * @return The REST service response.
   */
  @POST
  @Path("/query/{queryName}")
  @Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response updateQuery(
      @PathParam("queryName") String queryName, HpcCompoundMetadataQueryDTO compoundMetadataQuery);

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
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response getQuery(@PathParam("queryName") String queryName);

  /**
   * Get all saved queries.
   *
   * @return The REST service response w/ HpcNamedCompoundMetadataQueryListDTO entity.
   */
  @GET
  @Path("/query")
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response getQueries();

  /**
   * Get a list of metadata attributes currently registered.
   *
   * @param levelLabel Filter the results by level label (Optional).
   * @return The REST service response w/ HpcMetadataAttributesListDTO entity.
   */
  @GET
  @Path("/metadataAttributes")
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response getMetadataAttributes(@QueryParam("levelLabel") String levelLabel);

  /**
   * Query catalog metadata.
   *
   * @param catalogRequest A catalog request DTO.
   * @return The REST service response w/ HpcCatalogsDTO entity.
   */
  @POST
  @Path("/catalog/query")
  @Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response queryCatalog(HpcCatalogRequestDTO catalogRequest);
  
  /**
   * Refresh the metadata views.
   *
   * @return The REST service response.
   */
  @POST
  @Path("/refreshMetadataViews")
  public Response refreshMetadataViews();
}
