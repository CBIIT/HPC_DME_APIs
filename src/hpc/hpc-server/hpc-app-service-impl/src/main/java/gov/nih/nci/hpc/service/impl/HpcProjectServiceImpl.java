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

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidMetadataItems;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidProjectMetadata;
import gov.nih.nci.hpc.dao.HpcProjectDAO;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.model.HpcProject;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcProjectService;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Project Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

public class HpcProjectServiceImpl implements HpcProjectService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Managed Project DAO instance.
	@Autowired
    private HpcProjectDAO projectDAO = null;
	
    // Key Generator.
	@Autowired
    private HpcKeyGenerator keyGenerator = null;
    
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
    public String addProject(HpcProjectMetadata metadata,
		                     boolean persist) 
		                    throws HpcException
    {
       	// Input validation.
       	if(!isValidProjectMetadata(metadata)) {
       	   throw new HpcException("Invalid Project Metadata", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	

       	// Create the Project domain object.
    	HpcProject project = new HpcProject();
    	
    	// Generate and set its ID.
    	project.setId(keyGenerator.generateKey());
    	project.setMetadata(metadata);
    	
    	// Persist to Mongo.
    	if(persist) {
    	   projectDAO.upsert(project);
    	}
    	logger.debug("Project added: " + project);
    	
    	return project.getId();
    }
    
    @Override
    public void associateDataset(String projectId, String datasetId,
                                   boolean persist) throws HpcException
    {
    	HpcProject project = getProject(projectId);
    	if(project == null) {
    	   throw new HpcException("Project not found: " + projectId, 
    			                  HpcRequestRejectReason.PROJECT_NOT_FOUND);
    	}
    	
    	// Associate dataset if not already associated.
    	if(!project.getDatasetIds().contains(datasetId)) {
    		project.getDatasetIds().add(datasetId);
    		
    		// Persist to Mongo.
        	if(persist) {
        	   projectDAO.upsert(project);
        	}
    	}
    }
    
    @Override
    public List<HpcMetadataItem>  
           addMetadataItems(HpcProject project, 
                            List<HpcMetadataItem> metadataItems,
                            boolean persist) 
                           throws HpcException
    {
       	// Input validation.
       	if(project == null || 
       	   !isValidMetadataItems(metadataItems)) {
       	   throw new HpcException("Invalid add metadata items input", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	// Add the metadata items.
       	project.getMetadata().getMetadataItems().addAll(metadataItems);
       	
		// Persist if requested.
    	if(persist) {
     	   projectDAO.upsert(project);
     	}
    	
    	return project.getMetadata().getMetadataItems();
    }
    
    @Override
    public HpcProject getProject(String id) throws HpcException
    {
    	// Input validation.
    	if(!keyGenerator.validateKey(id)) {
    	   throw new HpcException("Invalid Project ID: " + id, 
    	                          HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return projectDAO.getProject(id);
    }
    
    @Override
    public List<HpcProject> getProjects(String userId, HpcDatasetUserAssociation association) 
 	                                   throws HpcException
 	{
    	return projectDAO.getProjects(userId, association);
 	}
    
    @Override
    public List<HpcProject> getProjects(HpcProjectMetadata metadata) 
                                       throws HpcException
    {
    	// Input Validation. At least one metadata element needs to be provided
    	// to query for.
    	if(metadata == null ||
    	   (metadata.getExperimentId() == null && 	
    		metadata.getInternalProjectId() == null &&
    		metadata.getName() == null &&
    		metadata.getFundingOrganization() == null && 
    		metadata.getPrimaryInvestigatorNihUserId() == null &&
    		metadata.getRegistrarNihUserId() == null &&
    		metadata.getDescription() == null &&
    		metadata.getLabBranch() == null &&
    		metadata.getDoc() == null &&
    		(metadata.getMetadataItems() == null || 
    		 metadata.getMetadataItems().size() == 0))) {
    		throw new HpcException("Invalid project metadata", 
                                   HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	// Validate metada items if not null.
    	if(metadata.getMetadataItems() != null &&
    	   !isValidMetadataItems(metadata.getMetadataItems())) {
    	   throw new HpcException("Invalid metadata items", 
                                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return projectDAO.getProjects(metadata);
    }
}

 