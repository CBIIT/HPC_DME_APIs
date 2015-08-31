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

import static gov.nih.nci.hpc.bus.impl.HpcBusServiceUtil.associateProjectMetadataLock;
import gov.nih.nci.hpc.bus.HpcDatasetBusService;
import gov.nih.nci.hpc.bus.HpcProjectBusService;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.model.HpcDataset;
import gov.nih.nci.hpc.domain.model.HpcProject;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectAddMetadataItemsDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectAssociateDatasetsDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectCollectionDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectMetadataDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectRegistrationDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDatasetService;
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
	
	@Autowired
    private HpcDatasetService datasetService = null;
	
	// Business service instances.
	@Autowired
    private HpcDatasetBusService datasetBusService = null;
    
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
    public String registerProject(HpcProjectRegistrationDTO projectRegistrationDTO)  
    		                     throws HpcException
    {
    	logger.info("Invoking registerProject(HpcProjectDTO): " + 
    			     projectRegistrationDTO);
    	
    	// Input validation.
    	if(projectRegistrationDTO == null) {
    	   throw new HpcException("Null HpcProjectDTO",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Validate the associated users with this project have valid NIH 
    	// account registered with HPC. In addition, validate the registrar
    	// has a valid data transfer account.
    	validateAssociatedUsers(projectRegistrationDTO);
    	
    	// Create and persist the project.
    	String projectId = 
    		   projectService.addProject(projectRegistrationDTO.getMetadata(), 
    				                     true); 
    	logger.info("Registered projectId  = " + projectId);
    	
    	// Create and associate the datasets in this registration request.
    	if(projectRegistrationDTO.getHpcDatasetRegistrationDTO() != null) {
    	   for(HpcDatasetRegistrationDTO datasetRegistrationDTO : 
    		   projectRegistrationDTO.getHpcDatasetRegistrationDTO()) {
    		   associateProject(datasetRegistrationDTO.getUploadRequests(), 
    				            projectId);
    		   datasetBusService.registerDataset(datasetRegistrationDTO);
    	   }
    	}
    	
    	return projectId;
    }
    
    @Override
    public HpcProjectMetadataDTO 
           addMetadataItems(HpcProjectAddMetadataItemsDTO addMetadataItemsDTO) 
                           throws HpcException
    {
       	logger.info("Invoking addMetadataItems(HpcDatasetAddMetadataItemsDTO): " + 
                                               addMetadataItemsDTO);
    	
       	// Input validation.
       	if(addMetadataItemsDTO == null) {
       	   throw new HpcException("Null HpcProjectAddMetadataItemsDTO",
			                      HpcErrorType.INVALID_REQUEST_INPUT);	
       	}
       	
       	// Locate the dataset.
       	HpcProject project = projectService.getProject(addMetadataItemsDTO.getProjectId());
       	if(project == null) {
       	   throw new HpcException("Project was not found: " + addMetadataItemsDTO.getProjectId(),
       			                  HpcRequestRejectReason.PROJECT_NOT_FOUND);	
       	}
       	
       	// Add metadata items.
    	projectService.addMetadataItems(project, addMetadataItemsDTO.getMetadataItems(), 
    			                        true); 
    	
    	return toDTO(project.getMetadata());
    }
    
    @Override
    public void associateDatasets(
                         HpcProjectAssociateDatasetsDTO associateDatasetsDTO)
                         throws HpcException
    {
	   	logger.info("Invoking associateDatasets(HpcProjectAssociateDatasetsDTO): " + 
	   			    associateDatasetsDTO);

	   	// Input validation.
	   	if(associateDatasetsDTO == null) {
	   	   throw new HpcException("Null HpcProjectAssociateDatasetsDTO",
			                      HpcErrorType.INVALID_REQUEST_INPUT);	
	   	}
	   	
		// Validate the associated datasets included in the association requests.
	   	List<String> datasetIds = associateDatasetsDTO.getDatasetIds();
		validateDatasets(datasetIds);
	   	
	   	// Locate the project.
	   	HpcProject project = 
	   			   projectService.getProject(associateDatasetsDTO.getProjectId());
	   	if(project == null) {
	   	   throw new HpcException("Project was not found: " + 
	   			                  associateDatasetsDTO.getProjectId(),
	   			                  HpcRequestRejectReason.PROJECT_NOT_FOUND);	
	   	}
	   	
	   	// Associate the datasets to the project. This is a bi-directional association.
	   	synchronized(associateProjectMetadataLock(project.getId())) {
	   		for(String datasetId : datasetIds) {
	   			synchronized(associateProjectMetadataLock(datasetId)) {
	   				projectService.associateDataset(project.getId(), datasetId, true);
	   				HpcDataset dataset = datasetService.getDataset(datasetId);
	   				for(HpcFile file : dataset.getFileSet().getFiles()) {
	   					datasetService.associateProject(dataset, file.getId(), 
		    			                                project.getId(), true);
	   				}
	   			}
		    }
		}
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
    	
    	return toDTO(projectService.getProjects(userId, association));
    }
    
    @Override
    public HpcProjectCollectionDTO getProjects(
    		                          HpcProjectMetadataDTO metadataDTO) 
                                      throws HpcException
    {
    	// Input validation.
    	if(metadataDTO == null) {
    	   throw new HpcException("Null metadata query DTO", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	return toDTO(projectService.getProjects(metadataDTO.getMetadata()));
    }    
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Create a project DTO from a domain object.
     * 
     * @param project the domain object.
     *
     * @return The DTO.
     */
    private HpcProjectDTO toDTO(HpcProject project) 
    		                   throws HpcException 
    {
    	if(project == null) {
     	   return null;
     	}
    	
       	HpcProjectDTO dto = new HpcProjectDTO();
    	dto.setId(project.getId());
    	dto.setMetadata(project.getMetadata());
    	dto.setCreated(project.getCreated());
    	dto.setLastUpdated(project.getLastUpdated());
    	List<String> datasetIds = project.getDatasetIds();
    	if(datasetIds != null && !datasetIds.isEmpty()) {
    		List<HpcDatasetDTO> datasets = new ArrayList<HpcDatasetDTO>();
    		for(String datasetId : datasetIds) {
    			try {
    				 HpcDatasetDTO dataset = 
    						       datasetBusService.getDataset(datasetId, true);
    				 if(dataset != null) {
    				 	datasets.add(dataset);
    				 }
    			}
    			catch(HpcException e) {
    				  throw new HpcException("Invalid associated dataset Id: " + 
    			                             datasetId, e);
    			}
    		}
    		if(datasets != null && !datasets.isEmpty()) {
    		   HpcDatasetCollectionDTO collection = new HpcDatasetCollectionDTO();
    		   collection.getHpcDatasetDTO().addAll(datasets);
    		   dto.setHpcDatasetCollectionDTO(collection);
    		}
    	}
    	
    	return dto;
    }  
    
    /**
     * Create a project collection DTO from a list of domain objects.
     * 
     * @param projects the domain object.
     *
     * @return The collection DTO.
     */
    private HpcProjectCollectionDTO toDTO(List<HpcProject> projects) 
    		                             throws HpcException 
    {
    	if(projects == null || projects.isEmpty()) {
    	   return null;
    	}
 	
    	HpcProjectCollectionDTO projectCollectionDTO = 
 			                    new HpcProjectCollectionDTO();
 	    for(HpcProject project : projects) {
 		    projectCollectionDTO.getHpcProjectDTO().add(toDTO(project));
 	    }
 	    
 	    return projectCollectionDTO;
    }
    
    /**
     * Create a project metadata DTO from a domain object.
     * 
     * @param projectMetadata the domain object.
     *
     * @return The DTO.
     */
    private HpcProjectMetadataDTO toDTO(HpcProjectMetadata projectMetadata)
    {
    	if(projectMetadata == null) {
     	   return null;
     	}
    	
    	HpcProjectMetadataDTO projectMetadataDTO = new HpcProjectMetadataDTO();
    	projectMetadataDTO.setMetadata(projectMetadata);
    	
    	return projectMetadataDTO;
    }  
    
    /**
     * Validate the users associated with the upload request are valid.
     * The associated users are - creator, registrar and primary investigator.
     * 
     * @param uploadRequests The upload requests to validate. 
     *
     * @throws HpcException if any validation error found.
     */
    private void validateAssociatedUsers(HpcProjectRegistrationDTO projectDTO)
    		                            throws HpcException
    {
    	if(projectDTO == null) {
    	   return;
    	}
    	
		// Verify PI, Creator and Registrar are registered with HPC.
		validateUser(projectDTO.getMetadata().getPrincipalInvestigatorNihUserId(),
				     HpcDatasetUserAssociation.PRINCIPAL_INVESTIGATOR);
		validateUser(projectDTO.getMetadata().getRegistrarNihUserId(),
		             HpcDatasetUserAssociation.REGISTRAR);
    }
    
    /**
     * Validate a user is registered with HPC.
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
    
    /**
     * Associate a project with the files in a registration file upload requests.
     * 
     * @param uploadRequests The upload requests.
     * @param projectId The project ID.
     */
    private void associateProject(List<HpcFileUploadRequest> uploadRequests, 
    		                      String projectId)
    {
    	if(uploadRequests != null) {
    	   for(HpcFileUploadRequest uploadRequest : uploadRequests) {
    		   uploadRequest.getProjectIds().add(projectId);
    	   }
    	}
    }
    
    /**
     * Validate datasets exist.
     * 
     * @param datasetIds The list of dataset-id to validate.
     *
     * @throws HpcException if any dataset on the list was not found.
     */
    private void validateDatasets(List<String> datasetIds) throws HpcException
    {
    	if(datasetIds == null || datasetIds.isEmpty()) {
    	   throw new HpcException("No dataset IDs attached to this request", 
                                  HpcErrorType.INVALID_REQUEST_INPUT);	
     	}
    	
    	for(String datasetId : datasetIds) {
    		if(datasetService.getDataset(datasetId) == null) {
    		   throw new HpcException("Dataset not found: " + datasetId,
    		                          HpcRequestRejectReason.DATASET_NOT_FOUND);	
    		}
    	}
	}
    		              
}

 