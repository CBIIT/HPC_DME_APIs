/**
 * HpcDatasetBusServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.bus.HpcDatasetBusService;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcDataset;
import gov.nih.nci.hpc.domain.model.HpcProject;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetAddFilesDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.dataset.HpcFileDTO;
import gov.nih.nci.hpc.dto.dataset.HpcPrimaryMetadataQueryDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcDatasetService;
import gov.nih.nci.hpc.service.HpcProjectService;
import gov.nih.nci.hpc.service.HpcTransferStatusService;
import gov.nih.nci.hpc.service.HpcUserService;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Dataset Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:mahidhar.narra@nih.gov">Mahidhar Narra</a>
 * @version $Id$
 */

public class HpcDatasetBusServiceImpl implements HpcDatasetBusService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // Application service instances.
    private HpcDatasetService datasetService = null;
    private HpcUserService userService = null;
    private HpcDataTransferService dataTransferService = null;
    private HpcProjectService projectService = null;
    
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
    private HpcDatasetBusServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param dataService The dataset application service.
     * @param userService The user application service.
     * @param dataTransferService The data transfer application service.
     * 
     * @throws HpcException If any application service provided is null.
     */
    private HpcDatasetBusServiceImpl(
    		          HpcDatasetService datasetService,
    		          HpcUserService userService,
    		          HpcDataTransferService dataTransferService,
    		          HpcProjectService projectService)
                      throws HpcException
    {
    	if(datasetService == null || userService == null ||
    	   dataTransferService == null) {
     	   throw new HpcException("Null App Service(s) instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.datasetService = datasetService;
    	this.userService = userService;
    	this.dataTransferService = dataTransferService;
    	this.projectService = projectService;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDatasetBusService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public String registerDataset(HpcDatasetRegistrationDTO datasetRegistrationDTO)  
    		                     throws HpcException
    {
    	logger.info("Invoking registerDataset(HpcDatasetRegistrationDTO): " + 
                    datasetRegistrationDTO);
    	
    	// Input validation.
    	if(datasetRegistrationDTO == null) {
    	   throw new HpcException("Null HpcDatasetRegistrationDTO",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Validate there is at least one file attached.
    	validateUploadRequests(datasetRegistrationDTO.getUploadRequests());
    	
    	// Validate the associated users with this dataset have valid NIH 
    	// account registered with HPC. In addition, validate the registrar
    	// has a valid data transfer account.
    	validateAssociatedUsers(datasetRegistrationDTO.getUploadRequests());

    	// TODO - document.
    	validateAssociatedProjects(datasetRegistrationDTO.getUploadRequests());
    	
    	// Validate this registrar has not already registered a dataset with the same name.
    	validateDatasetName(datasetRegistrationDTO.getName(),
    			            datasetRegistrationDTO.getUploadRequests());

    	// Add the dataset to the repository.
    	HpcDataset dataset = 
    		   datasetService.addDataset(
    				  datasetRegistrationDTO.getName(), 
    				  datasetRegistrationDTO.getDescription(),
    				  datasetRegistrationDTO.getComments(),
    				  false);
    	
    	// Process the file upload requests.
    	uploadFiles(dataset, datasetRegistrationDTO.getUploadRequests(), true);
    	
    	logger.info("Registered dataset id = " + dataset.getId());
    	return dataset.getId();
    }
    
    @Override
    public void addFiles(HpcDatasetAddFilesDTO addFilesDTO) throws HpcException
    {
       	logger.info("Invoking addFiles(HpcDatasetAddFilesDTO): " + addFilesDTO);
	
       	// Input validation.
       	if(addFilesDTO == null) {
       	   throw new HpcException("Null HpcDatasetRegistrationDTO",
			                      HpcErrorType.INVALID_REQUEST_INPUT);	
       	}
       	
    	// Validate there is at least one file attached.
    	validateUploadRequests(addFilesDTO.getUploadRequests());
       	
       	// Locate the dataset.
       	HpcDataset dataset = datasetService.getDataset(addFilesDTO.getDatasetId());
       	if(dataset == null) {
       	   throw new HpcException("Dataset was not found: " + addFilesDTO.getDatasetId(),
                                   HpcErrorType.INVALID_REQUEST_INPUT);	
       	}
       	
       	// Process the file upload requests.
    	uploadFiles(dataset, addFilesDTO.getUploadRequests(), true);
    	
    }
    
    @Override
    public HpcDatasetDTO getDataset(String id, boolean skipDataTransferStatusUpdate) 
    		                       throws HpcException
    {
    	logger.info("Invoking getDataset(String id): " + id + 
    			    ", skipDataTransferStatusCheck: " + 
    			    skipDataTransferStatusUpdate);
    	
    	// Input validation.
    	if(id == null) {
    	   throw new HpcException("Null Dataset ID",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}

    	// Locate the dataset.
    	HpcDataset dataset = datasetService.getDataset(id);

    	// Update the data transfer requests status.
    	if(!skipDataTransferStatusUpdate) {
    	   updateDataTransferRequestsStatus(dataset);
    	}
    	
    	return toDTO(dataset);
    }
    
    @Override
    public HpcFileDTO getFile(String id) throws HpcException
    {
    	logger.info("Invoking getFile(String id): " + id);
    	
    	// Input validation.
    	if(id == null) {
    	   throw new HpcException("Null File ID",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Get the file domain object and return it as DTO.
    	return toDTO(datasetService.getFile(id));
    }
    
    @Override
    public HpcDatasetCollectionDTO getDatasets(List<String> userIds, 
                                      HpcDatasetUserAssociation association) 
                                      throws HpcException
    {
    	// Input validation.
    	if(userIds == null || userIds.size() == 0 || association == null) {
    	   throw new HpcException("Null user-ids or association",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	return toCollectionDTO(datasetService.getDatasets(userIds, association));
    }
    
    public HpcDatasetCollectionDTO getDatasets(String firstName, String lastName, 
	                                           HpcDatasetUserAssociation association) 
	                                           throws HpcException
    {
    	// Input validation.
    	if(firstName == null || lastName == null || association == null) {
           throw new HpcException("Null first/last name or association",
   			                      HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Search for users that match the first/last name.
    	List<HpcUser> users = userService.getUsers(firstName, lastName);
    	if(users == null || users.size() == 0) {
    	   return null;
    	}
    	
    	List<String> userIds = new ArrayList<String>();
    	for(HpcUser user : users) {
    		userIds.add(user.getNihAccount().getUserId());
    	}
    		
    	return toCollectionDTO(datasetService.getDatasets(userIds, association));
   }
    
    @Override
    public HpcDatasetCollectionDTO getDatasets(String name) throws HpcException
    {
    	// Input validation.
    	if(name == null) {
    	   throw new HpcException("Null name", HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	return toCollectionDTO(datasetService.getDatasets(name));
    }
    
    @Override
    public HpcDatasetCollectionDTO getDatasets(
    		         HpcPrimaryMetadataQueryDTO primaryMetadataQueryDTO) 
                     throws HpcException
    {
    	// Input validation.
    	if(primaryMetadataQueryDTO == null) {
    	   throw new HpcException("Null primary metadata query DTO", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	return toCollectionDTO(datasetService.getDatasets(
    			                      primaryMetadataQueryDTO.getMetadata()));
    }    

    @Override
	public HpcDatasetCollectionDTO getDatasetsByStatus(String transferStatus) throws HpcException{
    	// Input validation.
    	if(transferStatus == null) {
    	   throw new HpcException("Null transferStatus", HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	return toCollectionDTO(datasetService.getDatasetsByStatus(transferStatus));
	}
    
	@Override
	public HpcDatasetCollectionDTO getDatasetsByProjectId(String projectId)
	 throws HpcException
	{
   	// Input validation.
	if(projectId == null) {
	   throw new HpcException("Null projectId", HpcErrorType.INVALID_REQUEST_INPUT);	
	}
	
	return toCollectionDTO(datasetService.getDatasetsByProjectId(projectId));	
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
    private HpcDatasetDTO toDTO(HpcDataset dataset)
    {
    	if(dataset == null) {
     	   return null;
     	}
    	
    	HpcDatasetDTO datasetDTO = new HpcDatasetDTO();
    	datasetDTO.setId(dataset.getId());
    	datasetDTO.setFileSet(dataset.getFileSet());
		for(HpcDataTransferRequest uploadRequest : dataset.getUploadRequests())
		{			
			datasetDTO.getUploadRequests().add(uploadRequest);
		}    		
    	
    	return datasetDTO;
    }  
    
    /**
     * Create a dataset DTO from a domain object.
     * 
     * @param dataset the domain object.
     *
     * @return The DTO.
     */
    private HpcFileDTO toDTO(HpcFile file)
    {
    	if(file == null) {
     	   return null;
     	}
    	
    	HpcFileDTO fileDTO = new HpcFileDTO();
    	fileDTO.setFile(file);
    	
    	return fileDTO;
    }  
    
    /**
     * Create a dataset collection DTO from a list of domain objects.
     * 
     * @param datasets the domain object.
     *
     * @return The collection DTO.
     */
    private HpcDatasetCollectionDTO toCollectionDTO(List<HpcDataset> datasets)
    {
    	if(datasets == null || datasets.size() == 0) {
    	   return null;
    	}
 	
    	HpcDatasetCollectionDTO datasetCollectionDTO = 
 			                    new HpcDatasetCollectionDTO();
 	    for(HpcDataset dataset : datasets) {
 		    datasetCollectionDTO.getHpcDatasetDTO().add(toDTO(dataset));
 	    }
 	    
 	    return datasetCollectionDTO;
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
    		             List<HpcFileUploadRequest> uploadRequests)
    		             throws HpcException
    {
    	if(uploadRequests == null) {
    	   return;
    	}
    	
    	for(HpcFileUploadRequest uploadRequest : uploadRequests) {
    		if(uploadRequest.getMetadata() == null) {
    		   continue;	
    		}
    		
    		// Verify PI and Registrar are registered with HPC.
    		validateUser(uploadRequest.getMetadata().getPrimaryInvestigatorNihUserId(),
    				     HpcDatasetUserAssociation.PRIMARY_INVESTIGATOR);
    		HpcUser registrar =
    		validateUser(uploadRequest.getMetadata().getRegistrarNihUserId(),
			             HpcDatasetUserAssociation.REGISTRAR);
    		
    		// Validate the registrator Data Transfer Account.
        	if(!dataTransferService.validateDataTransferAccount(
        	   registrar.getDataTransferAccount())) {
         	   throw new HpcException(
         			        "Invalid Data Transfer Account: username = " + 
         			        registrar.getDataTransferAccount().getUsername(), 
                            HpcRequestRejectReason.INVALID_DATA_TRANSFER_ACCOUNT);	
         	}
    	}
    }
    
    /**
     * Validate the projects associated with the upload request are valid.
     * 
     * @param uploadRequests The upload requests to validate. 
     *
     * @throws HpcException if any validation error found.
     */
    private void validateAssociatedProjects(
    		             List<HpcFileUploadRequest> uploadRequests)
    		             throws HpcException
    {
    	if(uploadRequests == null) {
    	   return;
    	}
    	
    	for(HpcFileUploadRequest uploadRequest : uploadRequests) {
    		if(uploadRequest.getProjectIds() == null || uploadRequest.getProjectIds().size() == 0) {
    		   continue;	
    		}
    		
    		List<String> projectIds = uploadRequest.getProjectIds();
    		if(projectIds != null && projectIds.size() > 0)
    		{
    			for(String projectId : projectIds)
    				validateProject(projectId);
    		}
    	}
    }    
    
    /**
     * Validate this registrar has not already registered a dataset with the same name.
     * 
     * @param name The dataset name.
     * @param uploadRequests The upload requests to validate. 
     *
     * @throws HpcException if any validation error found.
     */
    private void validateDatasetName(String name, 
    		                         List<HpcFileUploadRequest> uploadRequests)
    		                        throws HpcException
    {
    	if(uploadRequests == null || name == null) {
    	   return;
    	}
    	
    	for(HpcFileUploadRequest uploadRequest : uploadRequests) {
    		if(uploadRequest.getMetadata() == null || 
    		   uploadRequest.getMetadata().getRegistrarNihUserId() == null) {
    		   continue;	
    		}
    		
    		if(datasetService.exists(name, uploadRequest.getMetadata().getRegistrarNihUserId(), 
    				                 HpcDatasetUserAssociation.REGISTRAR)) {
         	   throw new HpcException(
         			        "Dataset name <" + name + "> already registered by " + 
         			        uploadRequest.getMetadata().getRegistrarNihUserId(),
                            HpcRequestRejectReason.DATASET_NAME_ALREADY_EXISTS);	
         	}
    	}
    }
 
    /**
     * Validate a project is registered with HPC.
     * 
     * @param projectId
     * 
     * @return The HpcProject
     *
     * @throws HpcException if the user is not registered with HPC.
     */
    private HpcProject validateProject(String projectId)  throws HpcException
    {
    	HpcProject project = projectService.getProject(projectId);
    	if(project == null) {
    	   throw new HpcException("Could not find Project with projectId = " + projectId,
    		                       HpcRequestRejectReason.INVALID_PROJECT_ID);	
    	}
    	
    	return project;
    }
    
    /**
     * Validate a user is registered with HPC.
     * 
     * @param nihUserId The NIH User ID.
     * @param userAssociation The user's association to the dataset.
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
     * Validate upload requests are not empty
     * 
     * @param uploadRequests The upload requests.
     *
     * @throws HpcException if any validation error found.
     */
    private void validateUploadRequests(List<HpcFileUploadRequest> uploadRequests)
                                       throws HpcException
    {
    	// Validating there is at least one file attached.
    	if(uploadRequests == null || uploadRequests.size() == 0) {
 	       throw new HpcException("No files attached to this request", 
 			                      HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    }
    
    /**
     * Update a dataset's upload and download data transfer requests with a 
     * current status polled from data-transfer.
     * 
     * @param dataset The dataset to update the transfer requests status
     *
     * @throws HpcException 
     */
    private void updateDataTransferRequestsStatus(HpcDataset dataset)
    		                                     throws HpcException
    {
    	// Update both upload and download transfer requests.
    	boolean transferStatusChanged = 
    			updateDataTransferRequestsStatus(dataset.getUploadRequests());
    	transferStatusChanged |= 
    			updateDataTransferRequestsStatus(dataset.getDownloadRequests());
    	
    	// Persist if any status has changed.
    	if(transferStatusChanged) {
    	   datasetService.persist(dataset);	
    	}
    }
    
    /**
     * Update data transfer requests with current status polled from data-transfer.
     * 
     * @param dataTransferRequests A collection of transfer requests to update.
     * @return True if at least one request had a change of status.
     *
     * @throws HpcException 
     */
    private boolean updateDataTransferRequestsStatus(
    		              List<HpcDataTransferRequest> dataTransferRequests)
    		              throws HpcException
    {
    	if(dataTransferRequests == null) {
    		return false;
    	}
    	
    	boolean transferStatusChanged = false;
		for(HpcDataTransferRequest dataTransferRequest : dataTransferRequests)
		{
			switch(dataTransferRequest.getStatus()) {
				   case IN_PROGRESS:
			       case FAILED:
			       case INITIATED:
				        // Get the data transfer account to use in checking status.
    		            HpcDataTransferAccount dataTransferAccount = 
 	    		               userService.getUser(dataTransferRequest.getRequesterNihUserId()).
 	    		                           getDataTransferAccount();
    		
    		            // Get updated report from data transfer.
			            HpcDataTransferReport dataTransferReport = 
			            	   dataTransferService.getTransferRequestStatus(
			            			                  dataTransferRequest.getDataTransferId(),
			            				              dataTransferAccount);
			            
			            // Update the upload request.
			            transferStatusChanged |= 
			            		datasetService.setDataTransferRequestStatus(dataTransferRequest, 
			            		                                            dataTransferReport);
			            
			            break;
			
			       default:
			    	   break;
			}
		}
		
		return transferStatusChanged;
    }
    
    /**
     * Process upload files request.
     * 
     * @param uploadRequests The file upload requests.
     * @param persist If set to true, the dataset will be persisted.
     *
     * @throws HpcException 
     */
    private void uploadFiles(HpcDataset dataset,
		                     List<HpcFileUploadRequest> uploadRequests,
		                     boolean persist) 
		                     throws HpcException
    {
    	// Upload files and attach them to the dataset
    	for(HpcFileUploadRequest uploadRequest : uploadRequests) {
    		logger.debug("Handling upload request: "+ uploadRequest);
    		
    		// Add a new file to the domain model
    		HpcFile file = datasetService.addFile(dataset, uploadRequest, false);
    		logger.debug("New File: " + file);
			
			// Transfer the file. 
    		String registrarNihId = uploadRequest.getMetadata().getRegistrarNihUserId();
			HpcDataTransferAccount dataTransferAccount = 
		                   userService.getUser(registrarNihId).getDataTransferAccount();
    		
			logger.debug("Submitting data transfer request: " + 
			             uploadRequest.getLocations());
			HpcDataTransferReport dataTransferReport = 
				   dataTransferService.transferDataset(uploadRequest.getLocations(), 
					                                   dataTransferAccount,
					                                   registrarNihId);

			// Attach an upload data transfer request to the dataset.
			HpcDataTransferRequest transferRequest = 
			   datasetService.addDataTransferUploadRequest(
					          dataset, registrarNihId, file.getId(), 
					          uploadRequest.getLocations(), dataTransferReport, 
					          false);
			logger.debug("Data Transfer Request: " + transferRequest);
    	}
    	
    	// Persist if requested.
    	if(persist) {
    	   datasetService.persist(dataset);
    	}
	}
    
}

 