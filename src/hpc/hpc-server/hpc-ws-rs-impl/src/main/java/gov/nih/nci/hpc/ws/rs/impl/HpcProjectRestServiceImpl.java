/**
 * HpcDatasetRestServiceImpl.java
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
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.dto.project.HpcProjectCollectionDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectRegistrationDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcProjectRestService;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Project REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id: $
 */

public class HpcProjectRestServiceImpl extends HpcRestServiceImpl
             implements HpcProjectRestService
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Project Registration Business Service instance.
    private HpcProjectBusService projectBusService = null;
    
    // The URI Info context instance.
    private @Context UriInfo uriInfo;
    
    private String dynamicConfigFile = null;
    
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
    
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcProjectRestServiceImpl() throws HpcException
    {
    	super(false);
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }  
    
   /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param registrationBusService The registration business service.
     * @param stackTraceEnabled If set to true, stack trace will be attached to
     *                          exception DTO.
     * 
     * @throws HpcException If the bus service is not provided by Spring.
     */
    private HpcProjectRestServiceImpl(HpcProjectBusService projectBusService,
    		                          boolean stackTraceEnabled,
    		                          String dynamicConfigFile)
                                     throws HpcException
    {
    	super(stackTraceEnabled);
    	
    	if(projectBusService == null || dynamicConfigFile == null) {
    	   throw new HpcException("Null HpcProjectBusService/confing file",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	this.projectBusService = projectBusService;
		this.dynamicConfigFile = dynamicConfigFile;
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
		logger.info("Invoking RS: POST /dataset: " + projectRegistrationDTO);
		
		String projectId = null;
		try {
			projectId = projectBusService.registerProject(projectRegistrationDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /dataset failed:", e);
			    return errorResponse(e);
		}
		
		return createdResponse(projectId);
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
    public Response getProjectsByRegistratorId(String registratorId)
    {
    	logger.info("Invoking RS: GET /project/query/registrator/{id}: " + registratorId);
    	
		HpcProjectCollectionDTO projectCollectionDTO = null;
		try {
			projectCollectionDTO = projectBusService.getProjects(
										   registratorId, 
						                   HpcDatasetUserAssociation.REGISTRAR); 
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/query/creator/{id}: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(projectCollectionDTO, true);
    }

    @Override
    public Response getProjectsByInvestigatorId(String investigatorId)
    {
    	logger.info("Invoking RS: GET /project/query/registrator/{id}: " + investigatorId);
    	
		HpcProjectCollectionDTO projectCollectionDTO = null;
		try {
			projectCollectionDTO = projectBusService.getProjects(
											investigatorId, 
						                   HpcDatasetUserAssociation.PRIMARY_INVESTIGATOR); 
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/query/creator/{id}: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(projectCollectionDTO, true);
    }
}

 