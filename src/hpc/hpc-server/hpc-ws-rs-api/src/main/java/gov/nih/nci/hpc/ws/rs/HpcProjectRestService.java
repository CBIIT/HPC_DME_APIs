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

import gov.nih.nci.hpc.dto.project.HpcProjectAddMetadataItemsDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectMetadataQueryDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectRegistrationDTO;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * <p>
 * HPC Project REST Service Interface.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Path("/")
public interface HpcProjectRestService
{   
    /**
     * POST Project registration request.
     *
     * @param projectRegistrationDTOt The project registration input DTO.
     */
    @POST
    @Path("/project")
    @Consumes("application/json,application/xml")
    public Response registerProject(HpcProjectRegistrationDTO projectRegistrationDTO);
    
    /**
     * POST add metadata items to a registered project.
     *
     * @param addMetadataItemsDTO The add-metadata-items request DTO.
     */
    @POST
    @Path("/project/metadata")
    @Consumes("application/json,application/xml")
    public Response addMetadataItems(HpcProjectAddMetadataItemsDTO addMetadataItemsDTO);
    
    /**
     * GET project by ID.
     *
     * @param id The Project ID.
     * @return gov.nih.nci.hpc.dto.project.HpcProjectDTO entity.
     */
    @GET
    @Path("/project/{id}")
    @Produces("application/json,application/xml")
    public Response getProject(@PathParam("id") String id); 
    
    /**
     * GET Projects by Registrar ID.
     *
     * @param creatorId Get projects associated with this Registrar ID.
     * @return gov.nih.nci.hpc.dto.project.HpcProjectCollectionDTO entity.
     */
    @GET
    @Path("/project/query/registrar/{id}")
    @Produces("application/json,application/xml")
    public Response getProjectsByRegistrarId(@PathParam("id") String registrarId); 
    
    /**
     * GET Projects by Primary Investigator ID.
     *
     * @param inverstigatorId Get projects associated with this primary investigator ID.
     * @return gov.nih.nci.hpc.dto.project.HpcProjectCollectionDTO entity.
     */
    @GET
    @Path("/project/query/principalInvestigator/{id}")
    @Produces("application/json,application/xml")
    public Response getProjectsByPrincipalInvestigatorId(
    		           @PathParam("id") String principalInvestigatorNihUserId); 
    
    /**
     * POST Projects by primary metadata.
     *
     * @param metadataQueryDTO Get projects that match the metadata search criteria.
     * @return gov.nih.nci.hpc.dto.project.HpcProjectCollectionDTO entity.
     */
    @POST
    @Path("/project/query/metadata")
    @Produces("application/json,application/xml")
    public Response getProjectsByMetadata(HpcProjectMetadataQueryDTO metadataQueryDTO);
}

 