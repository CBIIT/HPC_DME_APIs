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
import gov.nih.nci.hpc.service.HpcManagedDatasetService;
import gov.nih.nci.hpc.service.HpcManagedUserService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;
import gov.nih.nci.hpc.domain.model.HpcManagedDataset;
import gov.nih.nci.hpc.domain.model.HpcManagedUser;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserType;

import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <p>
 * HPC Dataset Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDatasetBusServiceImpl implements HpcDatasetBusService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // Application service instances.
    private HpcManagedDatasetService managedDatasetService = null;
    private HpcManagedUserService managedUserService = null;
    private HpcDataTransferService hpcDataTransferService = null;
    
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
     * @param managedDataService The managed dataset application service.
     * 
     * @throws HpcException If managedDatasetService is null.
     */
    private HpcDatasetBusServiceImpl(
    		          HpcManagedDatasetService managedDatasetService,
    		          HpcManagedUserService managedUserService,
    		          HpcDataTransferService hpcDataTransferService)
                   throws HpcException
    {
    	if(managedDatasetService == null || managedDatasetService == null ||
    	   hpcDataTransferService ==null) {
     	   throw new HpcException("Null App Service(s) instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.managedDatasetService = managedDatasetService;
    	this.managedUserService = managedUserService;
    	this.hpcDataTransferService = hpcDataTransferService;
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
    	logger.info("Invoking registerDataset(HpcDatasetDTO): " + 
                    datasetRegistrationDTO);
    	
    	// Input validation.
    	if(datasetRegistrationDTO == null) {
    	   throw new HpcException("Null HpcDatasetRegistrationDTO",
    			                  HpcErrorType.INVALID_INPUT);	
    	}
    	
    	// Validate the user has a valid data transfer account..
    	HpcManagedUser managedUser = managedUserService.get(
    			                            datasetRegistrationDTO.getRegistratorId());
    	if(managedUser == null || 
    	   managedUser.getUser().getDataTransferAccount() == null) {
    	   throw new HpcException("Could not find user with nihUserID=" + 
    				              datasetRegistrationDTO.getRegistratorId(),
	                              HpcErrorType.INVALID_INPUT);		
    	}
    	
    	// Add the dataset to the managed collection.
    	String managedDatasetId = 
    		   managedDatasetService.add(
    				  datasetRegistrationDTO.getName(), 
    				  datasetRegistrationDTO.getPrimaryInvestigatorId(),
    				  datasetRegistrationDTO.getCreatorId(), 
    				  datasetRegistrationDTO.getRegistratorId(),
    				  datasetRegistrationDTO.getLabBranch(), 
    				  datasetRegistrationDTO.getDescription(),
    				  datasetRegistrationDTO.getComments(),
    				  datasetRegistrationDTO.getUploadRequests());
    	
    	// Extract the data transfer account credentials.
    	String username  = managedUser.getUser().getDataTransferAccount().getUsername();
    	String password  = managedUser.getUser().getDataTransferAccount().getPassword();
    	
    	// Submit data transfer requests for the files in this dataset 
    	for(HpcFileUploadRequest fileUploadRequest : datasetRegistrationDTO.getUploadRequests()) {  
    		logger.info("Submiting Data Transfer Request: "+ fileUploadRequest.getLocations());
    		boolean transferStatus = 
    				hpcDataTransferService.transferDataset(
    						                       fileUploadRequest.getLocations(), 
    						                       username, password);
    		logger.info("Data Transfer status : " + transferStatus);
    	}
    	return managedDatasetId;
    }
    
    @Override
    public HpcDatasetDTO getDataset(String id) throws HpcException
    {
    	logger.info("Invoking getDataset(String id): " + id);
    	
    	// Input validation.
    	if(id == null) {
    	   throw new HpcException("Null Dataset ID",
    			                  HpcErrorType.INVALID_INPUT);	
    	}
    	
    	// Get the managed data domain object.
    	HpcManagedDataset managedDataset = managedDatasetService.get(id);
    	if(managedDataset == null) {
    	   return null;
    	}
    	
    	// Map it to the DTO.
    	HpcDatasetDTO datasetDTO = new HpcDatasetDTO();
    	datasetDTO.setId(managedDataset.getId());
    	datasetDTO.setName(managedDataset.getName());
    	datasetDTO.setPrimaryInvestigatorId(managedDataset.getPrimaryInvestigatorId());
    	datasetDTO.setCreatorId(managedDataset.getCreatorId());
    	datasetDTO.setRegistratorId(managedDataset.getRegistratorId());
    	datasetDTO.setLabBranch(managedDataset.getLabBranch());
    	datasetDTO.setCreated(managedDataset.getCreated());
    	
    	for(HpcFile file : managedDataset.getFiles()) {
    		datasetDTO.getFiles().add(file);
    	}
    	
    	return datasetDTO;
    }
    
    @Override
    public List<HpcDatasetDTO> getDatasets(String userId, 
                                           HpcDatasetUserType datasetUserType) 
                                          throws HpcException
    {
    	return null;
    }
}

 