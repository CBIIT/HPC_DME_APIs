/**
 * HpcProjectRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import gov.nih.nci.hpc.bus.HpcProjectBusService;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.dto.project.HpcProjectAddMetadataItemsDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectCollectionDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectMetadataQueryDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectRegistrationDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcProjectRestService;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Project REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

public class HpcProjectRestServiceImpl extends HpcRestServiceImpl
             implements HpcProjectRestService
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Project Registration Business Service instance.
	@Autowired
    private HpcProjectBusService projectBusService = null;
    
    // The URI Info context instance.
    private @Context UriInfo uriInfo;
    
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
    
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcProjectRestServiceImpl() throws HpcException
    {
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDatasetRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response registerProject(HpcProjectRegistrationDTO projectRegistrationDTO)
    {	
		logger.info("Invoking RS: POST /project: " + projectRegistrationDTO);
		
		String projectId = null;
		try {
			 projectId = projectBusService.registerProject(projectRegistrationDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /project failed:", e);
			    return errorResponse(e);
		}
		
		return createdResponse(projectId);
	}
    
    @Override
    public Response addMetadataItems(HpcProjectAddMetadataItemsDTO addMetadataItemsDTO)
    {
		logger.info("Invoking RS: POST /project/metadata: " + addMetadataItemsDTO);
		
		try {
			 projectBusService.addMetadataItems(addMetadataItemsDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /project/metadata failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(null, false);     	
    }
    
    @Override
    public Response getProject(String id)
    {
		logger.info("Invoking RS: GET /project/{id}: " + id);
		
		HpcProjectDTO projectDTO = null;
		try {
			projectDTO = projectBusService.getProject(id);
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/{id}: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(projectDTO, true);
	}
    
    @Override
    public Response getProjectsByRegistrarId(String registrarId)
    {
    	logger.info("Invoking RS: GET /project/query/registrar/{id}: " + registrarId);
    	
		HpcProjectCollectionDTO projectCollectionDTO = null;
		try {
			projectCollectionDTO = projectBusService.getProjects(
										   registrarId, 
						                   HpcDatasetUserAssociation.REGISTRAR); 
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/query/registrar/{id}: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(projectCollectionDTO, true);
    }

    @Override
    public Response getProjectsByPrincipalInvestigatorId(String principalInvestigatorNihUserId)
    {
    	logger.info("Invoking RS: GET /project/query/principalInvestigator/{id}: " + 
                    principalInvestigatorNihUserId);
    	
		HpcProjectCollectionDTO projectCollectionDTO = null;
		try {
			 projectCollectionDTO = projectBusService.getProjects(
					                       principalInvestigatorNihUserId, 
						                   HpcDatasetUserAssociation.PRINCIPAL_INVESTIGATOR); 
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /project/query/principalInvestigator/{id}: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(projectCollectionDTO, true);
    }
    
    @Override
    public Response getProjectsByMetadata(
    		           HpcProjectMetadataQueryDTO metadataQueryDTO)
	{
    	logger.info("Invoking RS: POST /project/query/metadata: " + 
    			    metadataQueryDTO);
    	
		HpcProjectCollectionDTO projectCollectionDTO = null;
		try {
			 projectCollectionDTO = 
					projectBusService.getProjects(metadataQueryDTO); 
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /project/query/metadata: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(projectCollectionDTO, true);
	}
}

 