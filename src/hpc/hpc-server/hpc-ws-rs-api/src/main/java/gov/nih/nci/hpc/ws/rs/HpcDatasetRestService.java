/**
 * HpcDatasetRegistrationRestService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs;

import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * <p>
 * HPC Dataset REST Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

@Path("/")
public interface HpcDatasetRestService
{    
    /**
     * GET Dataset by ID.
     *
     * @param id The dataset ID.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO entity.
     */
    @GET
    @Path("/dataset/{id}")
    @Produces("application/json,application/xml")
    public Response getDataset(@PathParam("id") String id); 
    
    /**
     * GET Datasets associated with a specific creator.
     *
     * @param creatorId The creator user ID.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO entity.
     */
    @GET
    @Path("/dataset")
    @Produces("application/json,application/xml")
    public Response getDatasets(@QueryParam("creatorId") String creatorId); 
    
    /**
     * POST registration request.
     *
     * @param registrationInput The data registration input DTO.
     */
    @POST
    @Path("/dataset")
    @Consumes("application/json,application/xml")
    public Response registerDataset(HpcDatasetRegistrationDTO datasetRegistrationDTO);
    
    /**
     * GET Configurable items by ID.
     *
     * @param id The Configurable items ID.
     * @return The registered data.
     */
    @GET
    @Path("/checkDataTransferStatus/{type}")
    @Produces("application/json")
    public Response checkDataTransferStatus(@PathParam("type") String id);
    
   /**
     * GET Configurable items by ID.
     *
     * @param id The Configurable items ID.
     * @return The registered data.
     */
    @GET
    @Path("/getPrimaryConfigurableDataFields/{type}")
    @Produces("application/json")
    public String getPrimaryConfigurableDataFields(@PathParam("type") String id, @QueryParam("callback") String callback);   	
}

 