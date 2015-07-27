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
import gov.nih.nci.hpc.dto.dataset.HpcPrimaryMetadataQueryDTO;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
     * POST registration request.
     *
     * @param registrationInput The data registration input DTO.
     */
    @POST
    @Path("/dataset")
    @Consumes("application/json,application/xml")
    public Response registerDataset(HpcDatasetRegistrationDTO datasetRegistrationDTO);
    
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
     * GET file by ID.
     *
     * @param id The file ID.
     * @return gov.nih.nci.hpc.dto.dataset.HpcFileDTO entity.
     */
    @GET
    @Path("/file/{id}")
    @Produces("application/json,application/xml")
    public Response getFile(@PathParam("id") String id);
    
    /**
     * GET Datasets by creator ID.
     *
     * @param registrarNihUserId Get datasets associated with this registrar.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @GET
    @Path("/dataset/query/registrar/{id}")
    @Produces("application/json,application/xml")
    public Response getDatasetsByRegistrarId(@PathParam("id") String registrarNihUserId); 
    
   /** 
   * GET Datasets by Primary Investigator ID.
     *
     * @param primaryInvestigatorUserId Get datasets associated with this primary investigator.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @GET
    @Path("/dataset/query/pi/{id}")
    @Produces("application/json,application/xml")
    public Response getDatasetsByPrimaryInvestigatorId(@PathParam("id") String primaryInvestigatorUserId); 
 
    /**
     * GET Datasets by Project ID.
     *
     * @param projectId Get datasets associated with this project Id.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @GET
    @Path("/dataset/query/project/{id}")
    @Produces("application/json,application/xml")
    public Response getDatasetsByProjectId(@PathParam("id") String projectId); 
     
    /**
     * GET Datasets by Primary Investigator's first and last name..
     *
     * @param firstName The primary investigator first name.
     * @param lastName The primary investigator last name.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @GET
    @Path("/dataset/query/primary-investigator")
    @Produces("application/json,application/xml")
    public Response getDatasetsByPrimaryInvestigatorName(
    		                            @QueryParam("firstName") String firstName,
    		                            @QueryParam("lastName") String lastName); 
    
    /**
     * GET Datasets by name.
     *
     * @param name Get datasets which 'name' is contained in their name.
     * 
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @GET
    @Path("/dataset/query/name/{name}")
    @Produces("application/json,application/xml")
    public Response getDatasetsByName(@PathParam("name") String name); 
    
    /**
     * POST Datasets by primary metadata.
     *
     * @param primaryMetadataQueryDTO Get datasets that matches the primary 
     *                                metadata search criteria.
     * 
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @POST
    @Path("/dataset/query/primaryMetadata")
    @Produces("application/json,application/xml")
    public Response getDatasetsByPrimaryMetadata(
    		           HpcPrimaryMetadataQueryDTO primaryMetadataQueryDTO);
    
    /**
     * GET Configurable items by ID.
     *
     * @param id The Configurable items ID.
     * @return The registered data.
     */
    @GET
    @Path("/dataset/query/transferStatus/{type}")
    @Produces("application/json,application/xml")
    public Response getDatasetsByTransferStatus(@PathParam("type") String transferStatus);
    
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

 