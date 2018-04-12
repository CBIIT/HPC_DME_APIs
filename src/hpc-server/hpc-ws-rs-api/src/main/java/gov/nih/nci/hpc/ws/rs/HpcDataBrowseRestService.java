/**
 * HpcDataBrowseRestService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkRequestDTO;

/**
 * <p>
 * HPC Data Browse REST Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

@Path("/")
public interface HpcDataBrowseRestService
{
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
    public Response addBookmark(@PathParam("bookmarkName") String bookmarkName,
    		                   HpcBookmarkRequestDTO bookmarkRequest);
    
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
    public Response updateBookmark(@PathParam("bookmarkName") String bookmarkName,
    		                      HpcBookmarkRequestDTO bookmarkRequest);
    
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

