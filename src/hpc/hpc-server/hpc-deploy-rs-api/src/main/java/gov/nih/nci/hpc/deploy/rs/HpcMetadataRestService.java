/**
 * HpcMetadataRestService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.deploy.rs;

import gov.nih.nci.hpc.dto.metadata.HpcMetadataDTO;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context;

/**
 * <p>
 * HPC Metadata REST Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcMetadataRestService
{    
    /**
     * GET Metadata.
     *
     * @param id The metadata ID.
     * @return The Metada object if found, otherwise returns null.
     */
    @GET
    @Path("/metadata/{id}")
    @Produces("application/json,application/xml")
    public HpcMetadataDTO getMetadata(@PathParam("id") String id); 
    
    /**
     * POST metadata.
     *
     * @param metadata The metadata to add.
     */
    @POST
    @Path("/metadata")
    @Consumes("application/json,application/xml")
    public Response postMetadata(HpcMetadataDTO metadata/*, @Context UriInfo uriInfo*/);
}

 