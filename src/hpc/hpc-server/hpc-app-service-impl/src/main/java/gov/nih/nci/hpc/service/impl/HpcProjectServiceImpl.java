/**
 * HpcProjectServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.dao.HpcProjectDAO;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.model.HpcProject;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcProjectService;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Project Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id: $
 */

public class HpcProjectServiceImpl implements HpcProjectService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Managed Project DAO instance.
	@Autowired
    private HpcProjectDAO projectDAO = null;
    
    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcProjectServiceImpl() throws HpcException
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcProjectService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public String add(HpcProjectMetadata metadata,
		              List<String> datasetIds) 
		             throws HpcException
    {
    	// Input validation.
    	if(metadata == null) {
    	   throw new HpcException("Required metadata is missing for the project", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	// Create the Project domain object.
    	HpcProject project = new HpcProject();
    	
    	// Generate and set its ID.
    	project.setId(UUID.randomUUID().toString());
    	project.setMetadata(metadata);
    	if(datasetIds != null && datasetIds.size() > 0)
    		project.getDatasetIds().addAll(datasetIds);
    	
    	// Persist to Mongo.
    	projectDAO.add(project);
    	logger.debug("Project added: " + project);
    	
    	return project.getId();
    }
    
    @Override
    public HpcProject getProject(String id) throws HpcException
    {
    	// Input validation.
    	try {
    	     if(id == null || UUID.fromString(id) == null) {
    	        throw new HpcException("Invalid dataset ID: " + id, 
    			                       HpcErrorType.INVALID_REQUEST_INPUT);
    	     }
    	} catch(IllegalArgumentException e) {
    		    throw new HpcException("Invalid UUID: " + id, 
                                       HpcErrorType.INVALID_REQUEST_INPUT, e);
    	}
    	
    	return projectDAO.getProject(id);
    }
    
    @Override
    public List<HpcProject> getProjects(String userId, HpcDatasetUserAssociation association) 
 	                                   throws HpcException
 	{
    	return projectDAO.getProjects(userId, association);
 	}
}

 