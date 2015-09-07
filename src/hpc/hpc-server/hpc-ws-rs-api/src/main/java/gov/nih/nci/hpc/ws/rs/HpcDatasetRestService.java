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

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferStatus;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetAddFilesDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetAddMetadataItemsDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetAssociateFileProjectsDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetUpdateFilePrimaryMetadataDTO;
import gov.nih.nci.hpc.dto.dataset.HpcFilePrimaryMetadataDTO;

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
     * @param datasetRegistrationDTO The dataset registration request DTO.
     * @return The new datset URI.
     */
    @POST
    @Path("/dataset")
    @Consumes("application/json,application/xml")
    public Response registerDataset(HpcDatasetRegistrationDTO datasetRegistrationDTO);
    
    /**
     * POST add files to a registered dataset.
     *
     * @param addFilesDTO The add-files request DTO.
     */
    @POST
    @Path("/dataset/files")
    @Consumes("application/json,application/xml")
    public Response addFiles(HpcDatasetAddFilesDTO addFilesDTO);
    
    /**
     * POST Associate projects with file in a registered dataset.
     *
     * @param associateFileProjectsDTO The projects to file association request DTO.
     */
    @POST
    @Path("/dataset/projects")
    @Consumes("application/json,application/xml")
    public Response associateProjects(
    		        HpcDatasetAssociateFileProjectsDTO associateFileProjectsDTO);
    
    /**
     * POST add metadata items to a file primary metadata in a registered dataset.
     *
     * @param addMetadataItemsDTO The add-metadata-items request DTO.
     * @return gov.nih.nci.hpc.dto.dataset.HpcFilePrimaryMetadataDTO entity.
     */
    @POST
    @Path("/dataset/metadata/primary/items")
    @Consumes("application/json,application/xml")
    public Response addPrimaryMetadataItems(HpcDatasetAddMetadataItemsDTO addMetadataItemsDTO);
    
    /**
     * POST update primary metadata of a file in a registered dataset.
     *
     * @param addMetadataItemsDTO The add-metadata-items request DTO.
     * @return gov.nih.nci.hpc.dto.dataset.HpcFilePrimaryMetadataDTO entity.
     */
    @POST
    @Path("/dataset/metadata/primary")
    @Consumes("application/json,application/xml")
    public Response updatePrimaryMetadata(HpcDatasetUpdateFilePrimaryMetadataDTO updateMetadataDTO);
    
    /**
     * GET Dataset by ID.
     *
     * @param id The dataset ID.
     * @param skipDataTransferStatusUpdate If set to true, the service will not poll
     *                                     Data Transfer for an updated status on transfer
     *                                     requests in-flight. 
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO entity.
     */
    @GET
    @Path("/dataset/{id}")
    @Produces("application/json,application/xml")
    public Response getDataset(@PathParam("id") String id,
    		                   @QueryParam("skipDataTransferStatusUpdate") 
                               Boolean skipDataTransferStatusUpdate); 
    
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
     * GET Datasets by Registrar ID.
     *
     * @param registrarNihUserId Get datasets associated with this registrar.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @GET
    @Path("/dataset/query/registrar/{id}")
    @Produces("application/json,application/xml")
    public Response getDatasetsByRegistrarId(
    		           @PathParam("id") String registrarNihUserId); 
    
   /** 
   * GET Datasets by Primary Investigator ID.
     *
     * @param principalInvestigatorNihUserId Get datasets associated with this principal investigator.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @GET
    @Path("/dataset/query/principalInvestigator/{id}")
    @Produces("application/json,application/xml")
    public Response getDatasetsByPrincipalInvestigatorId(
    		           @PathParam("id") String principalInvestigatorNihUserId); 
    
    /**
     * GET Datasets by Principal Investigator's first and last name.
     *
     * @param firstName The principal investigator first name.
     * @param lastName The principal investigator last name.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @GET
    @Path("/dataset/query/principalInvestigator")
    @Produces("application/json,application/xml")
    public Response getDatasetsByPrincipalInvestigatorName(
    		                   @QueryParam("firstName") String firstName,
    		                   @QueryParam("lastName") String lastName); 
    
    /**
     * GET Datasets by Registrar's first and last name.
     *
     * @param firstName The registrar first name.
     * @param lastName The registrar last name.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @GET
    @Path("/dataset/query/registrar")
    @Produces("application/json,application/xml")
    public Response getDatasetsByRegistrarName(
    		                   @QueryParam("firstName") String firstName,
    		                   @QueryParam("lastName") String lastName); 
 
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
     * GET Datasets by name.
     *
     * @param name Get datasets which 'name' is contained in their name.
     * @param regex If set to true, the 'name' will be queried as a regular expression. 
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @GET
    @Path("/dataset/query/name/{name}")
    @Produces("application/json,application/xml")
    public Response getDatasetsByName(@PathParam("name") String name,
    		                          @QueryParam("regex") Boolean regex); 
    
    /**
     * POST Datasets by primary metadata. 
     *
     * @param primaryMetadataQueryDTO Get datasets that match the primary 
     *        metadata search criteria. All datasets will be returned if empty
     *        metadata is provided.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @POST
    @Path("/dataset/query/primaryMetadata")
    @Produces("application/json,application/xml")
    public Response getDatasetsByPrimaryMetadata(
    		           HpcFilePrimaryMetadataDTO primaryMetadataDTO);
    
    /**
     * GET Datasets by data transfer status.
     *
     * @param dataTransferStatus The data transfer status to query for.
     * @param uploadRequests Search the upload data transfer requests.
     * @param downloadRequests Search the download data transfer requests.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @GET
    @Path("/dataset/query/dataTransferStatus/{status}")
    @Produces("application/json,application/xml")
    public Response getDatasetsByDataTransferStatus(
    		           @PathParam("status") HpcDataTransferStatus dataTransferStatus,
    		           @QueryParam("uploadRequests") Boolean uploadRequests, 
    		           @QueryParam("downloadRequests") Boolean downloadRequests);
    
   /**
     * GET Configurable items by ID.
     *
     * @param id The Configurable items ID.
     * @return The registered data.
     */
    @GET
    @Path("/getPrimaryConfigurableDataFields/{type}")
    @Produces("application/json")
    public String getPrimaryConfigurableDataFields(@PathParam("type") String id, 
    		                                       @QueryParam("callback") String callback);   	
}

 