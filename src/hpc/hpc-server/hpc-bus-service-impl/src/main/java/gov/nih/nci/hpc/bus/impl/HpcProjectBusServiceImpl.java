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

import gov.nih.nci.hpc.bus.HpcProjectBusService;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcProject;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectCollectionDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectRegistrationDTO;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcProjectService;
import gov.nih.nci.hpc.service.HpcUserService;
import gov.nih.nci.hpc.service.HpcDatasetService;
import gov.nih.nci.hpc.bus.impl.HpcDatasetBusServiceImpl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Project Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id:  $
 */

public class HpcProjectBusServiceImpl implements HpcProjectBusService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // Application service instances.
    private HpcProjectService projectService = null;
    private HpcUserService userService = null;
    private HpcDatasetBusServiceImpl datasetService = null;
    
    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcProjectBusServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param dataService The project application service.
     * @param userService The user application service.
     * @param datasetService The dataset application service.
     * 
     * @throws HpcException If any application service provided is null.
     */
    private HpcProjectBusServiceImpl(
    		          HpcProjectService projectService,
    		          HpcUserService userService,
    		          HpcDatasetBusServiceImpl datasetService)
                      throws HpcException
    {
    	if(projectService == null || userService == null ||
    			datasetService == null) {
     	   throw new HpcException("Null App Service(s) instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.projectService = projectService;
    	this.userService = userService;
    	this.datasetService = datasetService;
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
    				HpcDatasetDTO dataset = datasetService.getDataset(datasetId);
    				if(dataset != null)
    					datasets.add(dataset);
    			}
    			catch(HpcException e)
    			{
    				throw new HpcException("Invalid associated dataset Id: "+datasetId, e);
    			}
    		}
    		if(datasets.size() > 0)
    			dto.getHpcDatasetCollectionDTO().getHpcDatasetDTO().addAll(datasets);
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

    	if(metadata.getDivision() == null)
  	   	   throw new HpcException("Division value is missing for project",
  	   			HpcErrorType.INVALID_REQUEST_INPUT);	

    	if(metadata.getCenter() == null)
  	   	   throw new HpcException("Center value is missing for project",
  	   			HpcErrorType.INVALID_REQUEST_INPUT);	

    	if(metadata.getInternalProjectId() == null)
   	   	   throw new HpcException("Internal Project Id value is missing for project",
   	   			HpcErrorType.INVALID_REQUEST_INPUT);	

    	if(metadata.getOrganization() == null)
   	   	   throw new HpcException("Organization value is missing for project",
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
    		HpcUser registrator =
    		validateUser(projectDTO.getMetadata().getRegistratorNihUserId(),
			             HpcDatasetUserAssociation.REGISTRATOR);
    		
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
    	HpcUser user = userService.get(nihUserId);
    	if(user == null) {
    	   throw new HpcException("Could not find "+ userAssociation +
    				               " user with nihUserID = " + nihUserId,
    		                       HpcRequestRejectReason.INVALID_NIH_ACCOUNT);	
    	}
    	
    	return user;
    }
}

 