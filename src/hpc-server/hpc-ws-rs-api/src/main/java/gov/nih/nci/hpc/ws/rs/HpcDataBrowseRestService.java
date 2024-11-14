/**
 * HpcDataBrowseRestService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkRequestDTO;

/**
 * HPC Data Browse REST Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
@Path("/")
public interface HpcDataBrowseRestService {
  /**
   * Add a new bookmark.
   *
   * @param bookmarkName The bookmark name.
   * @param bookmarkRequest The bookmark request DTO.
   * @return The REST service response.
   */
  @PUT
  @Path("/bookmark/{bookmarkName}")
  @Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response addBookmark(
      @PathParam("bookmarkName") String bookmarkName, HpcBookmarkRequestDTO bookmarkRequest);

  /**
   * Update a bookmark.
   *
   * @param bookmarkName The bookmark name.
   * @param bookmarkRequest The bookmark request DTO.
   * @return The REST service response.
   */
  @POST
  @Path("/bookmark/{bookmarkName}")
  @Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response updateBookmark(
      @PathParam("bookmarkName") String bookmarkName, HpcBookmarkRequestDTO bookmarkRequest);

  /**
   * Delete a bookmark.
   *
   * @param bookmarkName The bookmark name.
   * @return The REST service response.
   */
  @DELETE
  @Path("/bookmark/{bookmarkName}")
  public Response deleteBookmark(@PathParam("bookmarkName") String bookmarkName);

  /**
   * Get a bookmark.
   *
   * @param bookmarkName The bookmark name.
   * @return The REST service response w/ HpcBookmarkDTO entity.
   */
  @GET
  @Path("/bookmark/{bookmarkName}")
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response getBookmark(@PathParam("bookmarkName") String bookmarkName);

  /**
   * Get all saved bookmarks.
   *
   * @return The REST service response w/ HpcBookmarkListDTO entity.
   */
  @GET
  @Path("/bookmark")
  @Produces("application/json;charset=UTF-8, application/xml;charset=UTF-8")
  public Response getBookmarks();
}
