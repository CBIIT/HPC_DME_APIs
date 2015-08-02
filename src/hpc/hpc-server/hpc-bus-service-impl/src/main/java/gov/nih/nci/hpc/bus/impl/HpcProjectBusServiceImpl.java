/**
 * HpcProjectBusServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.bus.HpcDatasetBusService;
import gov.nih.nci.hpc.bus.HpcProjectBusService;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.model.HpcProject;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectCollectionDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectRegistrationDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcProjectService;
import gov.nih.nci.hpc.service.HpcUserService;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Project Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

public class HpcProjectBusServiceImpl implements HpcProjectBusService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // Application service instances.
	
	@Autowired
    private HpcProjectService projectService = null;
	
	@Autowired
    private HpcUserService userService = null;
	
	// Dataset business service instance.
	@Autowired
    private HpcDatasetBusService datasetService = null;
    
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
    private HpcProjectBusServiceImpl() throws HpcException
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcProjectBusService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public String registerProject(HpcProjectRegistrationDTO projectDTO)  
    		                     throws HpcException
    {
    	logger.info("Invoking registerProject(HpcProjectDTO): " + 
    			projectDTO);
    	
    	// Input validation.
    	if(projectDTO == null) {
    	   throw new HpcException("Null HpcProjectDTO",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Validate the associated users with this project have valid NIH 
    	// account registered with HPC. In addition, validate the registrator
    	// has a valid data transfer account.
    	validateAssociatedUsers(projectDTO);
    	validateMetadata(projectDTO);
    	

    	List<gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO> datasets = projectDTO.getHpcDatasetRegistrationDTO();
    	List<String> datasetIds = new ArrayList<String>();
    	if(datasets != null)
    	{
    		for(HpcDatasetRegistrationDTO dataset : datasets)
    		{
    			String datasetId = datasetService.registerDataset(dataset);
    			datasetIds.add(datasetId);
    		}
    	}
    	
    	String projectId = 
    		   projectService.add(
    				   projectDTO.getMetadata(), 
    				   datasetIds);
    	logger.info("Registered projectId  = " + projectId);
    	
    	return projectId;
    }
    
    @Override
    public HpcProjectDTO getProject(String id) throws HpcException
    {
    	logger.info("Invoking getProject(String id): " + id);
    	
    	// Input validation.
    	if(id == null) {
    	   throw new HpcException("Null Project ID",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Get the project domain object and return it as DTO.
    	HpcProject project = projectService.getProject(id);
    	return toDTO(project);
    }
    
    @Override
    public HpcProjectCollectionDTO getProjects(String userId, 
                                      HpcDatasetUserAssociation association) 
                                      throws HpcException
    {
    	// Input validation.
    	if(userId == null || association == null) {
    	   throw new HpcException("Null user-id or association",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	return toCollectionDTO(projectService.getProjects(userId, association));
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Create a dataset DTO from a domain object.
     * 
     * @param dataset the domain object.
     *
     * @return The DTO.
     */
    private HpcProjectDTO toDTO(HpcProject project) throws HpcException 
    {
    	if(project == null) {
     	   return null;
     	}
    	
       	HpcProjectDTO dto = new HpcProjectDTO();
    	dto.setId(project.getId());
    	dto.setMetadata(project.getMetadata());
    	List<String> datasetIds = project.getDatasetIds();
    	if(datasetIds != null && datasetIds.size() > 0)
    	{
    		List<HpcDatasetDTO> datasets = new ArrayList<HpcDatasetDTO>();
    		for(String datasetId : datasetIds)
    		{
    			try
    			{
    				HpcDatasetDTO dataset = datasetService.getDataset(datasetId, true);
    				if(dataset != null)
    					datasets.add(dataset);
    			}
    			catch(HpcException e)
    			{
    				throw new HpcException("Invalid associated dataset Id: "+datasetId, e);
    			}
    		}
    		if(datasets != null && datasets.size() > 0)
    		{
    			HpcDatasetCollectionDTO collection = new HpcDatasetCollectionDTO();
    			collection.getHpcDatasetDTO().addAll(datasets);
    			dto.setHpcDatasetCollectionDTO(collection);
    		}
    	}
    	
    	return dto;
    }  
    
    /**
     * Create a dataset collection DTO from a list of domain objects.
     * 
     * @param projects the domain object.
     *
     * @return The collection DTO.
     */
    private HpcProjectCollectionDTO toCollectionDTO(List<HpcProject> projects) throws HpcException 
    {
    	if(projects == null || projects.size() == 0) {
    	   return null;
    	}
 	
    	HpcProjectCollectionDTO projectCollectionDTO = 
 			                    new HpcProjectCollectionDTO();
 	    for(HpcProject project : projects) {
 		    projectCollectionDTO.getHpcProjectDTO().add(toDTO(project));
 	    }
 	    
 	    return projectCollectionDTO;
    }
    
    private void validateMetadata(HpcProjectRegistrationDTO projectDTO)
            throws HpcException
    {
    	if(projectDTO.getMetadata() == null)
    	   	   throw new HpcException("Required metadata is missing for project",
    	   			HpcErrorType.INVALID_REQUEST_INPUT);	
    	HpcProjectMetadata metadata = projectDTO.getMetadata();
    	if(metadata.getName() == null)
 	   	   throw new HpcException("Name value is missing for project",
 	   			HpcErrorType.INVALID_REQUEST_INPUT);	

    	if(metadata.getName() == null)
  	   	   throw new HpcException("Name value is missing for project",
  	   			HpcErrorType.INVALID_REQUEST_INPUT);	

    	if(metadata.getPrimaryInvestigatorNihUserId() == null)
  	   	   throw new HpcException("PI Id value is missing for project",
  	   			HpcErrorType.INVALID_REQUEST_INPUT);	

    	if(metadata.getRegistratorNihUserId() == null)
  	   	   throw new HpcException("Registrator Id value is missing for project",
  	   			HpcErrorType.INVALID_REQUEST_INPUT);	

    	if(metadata.getLabBranch() == null)
  	   	   throw new HpcException("Lab/Branch value is missing for project",
  	   			HpcErrorType.INVALID_REQUEST_INPUT);	

    	if(metadata.getDoc() == null)
  	   	   throw new HpcException("DOCn value is missing for project",
  	   			HpcErrorType.INVALID_REQUEST_INPUT);	

    	if(metadata.getInternalProjectId() == null)
   	   	   throw new HpcException("Internal Project Id value is missing for project",
   	   			HpcErrorType.INVALID_REQUEST_INPUT);	

    	if(metadata.getFundingOrganization() == null)
   	   	   throw new HpcException("Funding Organization value is missing for project",
   	   			HpcErrorType.INVALID_REQUEST_INPUT);	

    	if(metadata.getExperimentId() == null)
   	   	   throw new HpcException("Experiment Id value is missing for project",
   	   			HpcErrorType.INVALID_REQUEST_INPUT);	
    	
    }
    
    /**
     * Validate the users associated with the upload request are valid.
     * The associated users are - creator, registrator and primary investigator.
     * 
     * @param uploadRequests The upload requests to validate. 
     *
     * @throws HpcException if any validation error found.
     */
    private void validateAssociatedUsers(
    		HpcProjectRegistrationDTO projectDTO)
    		             throws HpcException
    {
    	if(projectDTO == null) {
    	   return;
    	}
    	
		// Verify PI, Creator and Registrator are registered with HPC.
		validateUser(projectDTO.getMetadata().getPrimaryInvestigatorNihUserId(),
				     HpcDatasetUserAssociation.PRIMARY_INVESTIGATOR);
		validateUser(projectDTO.getMetadata().getRegistratorNihUserId(),
		             HpcDatasetUserAssociation.REGISTRAR);
    }
    
    /**
     * Validate a user is registered with HPC
     * 
     * @param nihUserId The NIH User ID.
     * @param userAssociation The user's association to the project.
     * 
     * @return The HpcUser.
     *
     * @throws HpcException if the user is not registered with HPC.
     */
    private HpcUser validateUser(String nihUserId, 
    		                     HpcDatasetUserAssociation userAssociation)
    		                    throws HpcException
    {
    	HpcUser user = userService.getUser(nihUserId);
    	if(user == null) {
    	   throw new HpcException("Could not find "+ userAssociation +
    				               " user with nihUserID = " + nihUserId,
    		                       HpcRequestRejectReason.INVALID_NIH_ACCOUNT);	
    	}
    	
    	return user;
    }
}

 