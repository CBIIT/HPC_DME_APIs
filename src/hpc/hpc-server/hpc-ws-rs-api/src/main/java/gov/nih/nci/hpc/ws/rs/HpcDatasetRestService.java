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
import gov.nih.nci.hpc.dto.dataset.HpcDatasetQueryType;
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
    @Path("/datasets")
    @Consumes("application/json,application/xml")
    public Response registerDataset(HpcDatasetRegistrationDTO datasetRegistrationDTO);
    
    /**
     * POST add files to a registered dataset.
     *
     * @param addFilesDTO The add-files request DTO.
     */
    @POST
    @Path("/datasets/files")
    @Consumes("application/json,application/xml")
    public Response addFiles(HpcDatasetAddFilesDTO addFilesDTO);
    
    /**
     * POST Associate projects with file in a registered dataset.
     *
     * @param associateFileProjectsDTO The projects to file association request DTO.
     */
    @POST
    @Path("/datasets/projects")
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
    @Path("/datasets/metadata/primary/items")
    @Consumes("application/json,application/xml")
    public Response addPrimaryMetadataItems(HpcDatasetAddMetadataItemsDTO addMetadataItemsDTO);
    
    /**
     * POST update primary metadata of a file in a registered dataset.
     *
     * @param addMetadataItemsDTO The add-metadata-items request DTO.
     * @return gov.nih.nci.hpc.dto.dataset.HpcFilePrimaryMetadataDTO entity.
     */
    @POST
    @Path("/datasets/metadata/primary")
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
    @Path("/datasets/{id}")
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
    @Path("/files/{id}")
    @Produces("application/json,application/xml")
    public Response getFile(@PathParam("id") String id);
    
    /**
     * GET datasets.
     *
     * @param queryType The query type.
     * @param nciUserId NCI user id.
     * @param firstName User's first name.
     * @param lastName User's last name.
     * @param projectId Project ID.
     * @param name Dataset name.
     * @param regex true to query name as regex. false as literal. 
     * @param dataTransferStatus Data transfer status.
     * @param uploadRequests true to include upload requests in query.
     * @param downloadRequests true to include download requests in query.
     * @param from From date.
     * @param to To date.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @GET
    @Path("/datasets")
    @Produces("application/json,application/xml")
    public Response getDatasets(
    		           @QueryParam("queryType") HpcDatasetQueryType queryType,
    		           @QueryParam("nciUserId") String nciUserId,
    		           @QueryParam("firstName") String firstName,
    		           @QueryParam("lastName") String lastName,
    		           @QueryParam("projectId") String projectId,
    		           @QueryParam("name") String name,
                       @QueryParam("regex") Boolean regex,
                       @QueryParam("dataTransferStatus") HpcDataTransferStatus dataTransferStatus,
    		           @QueryParam("uploadRequests") Boolean uploadRequests, 
    		           @QueryParam("downloadRequests") Boolean downloadRequests,
    				   @QueryParam("from") String from,
    				   @QueryParam("to") String to);
    
    /**
     * POST Search Datasets by primary metadata. 
     *
     * @param primaryMetadataQueryDTO Get datasets that match the primary 
     *        metadata search criteria. All datasets will be returned if empty
     *        metadata is provided.
     * @return gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO entity.
     */
    @POST
    @Path("/datasets/query/primaryMetadata")
    @Produces("application/json,application/xml")
    public Response getDatasetsByPrimaryMetadata(
    		           HpcFilePrimaryMetadataDTO primaryMetadataDTO);
    
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
    
    /*
    // S3 prototype
    @GET
    @Path("/s3/{path}")
    @Produces("application/json,application/xml")
    public Response s3Upload(@PathParam("path") String path);
    
    */
    // Jargon prototype
    @GET
    @Path("/jargon/{path}")
    @Produces("application/json,application/xml")
    public Response irodsUpload(@PathParam("path") String inputPath);
    
}

 