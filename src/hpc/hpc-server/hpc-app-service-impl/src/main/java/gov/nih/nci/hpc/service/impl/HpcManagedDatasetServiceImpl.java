/**
 * HpcManagedDatasetServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.service.HpcManagedDatasetService;
import gov.nih.nci.hpc.domain.model.HpcManagedDataset;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.dataset.HpcFileType;
import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.metadata.HpcDatasetMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcDatasetPrimaryMetadata;
import gov.nih.nci.hpc.dao.HpcManagedDatasetDAO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.Calendar;

/**
 * <p>
 * HPC Managed Dataset Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedDatasetServiceImpl implements HpcManagedDatasetService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Managed Dataset DAO instance.
    private HpcManagedDatasetDAO managedDatasetDAO = null;
    
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
    private HpcManagedDatasetServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param managedDatasetDAO The managed dataset DAO instance.
     */
    private HpcManagedDatasetServiceImpl(HpcManagedDatasetDAO managedDatasetDAO)
                                        throws HpcException
    {
    	if(managedDatasetDAO == null) {
     	   throw new HpcException("Null HpcManagedDatasetDAO instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.managedDatasetDAO = managedDatasetDAO;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcManagedDatasetService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public String add(String name, String primaryInvestigatorId,
		              String creatorId, String registratorId,
		              String labBranch, List<HpcFileUploadRequest> uploadRequests) 
		             throws HpcException
    {
    	// Input validation.
    	if(name == null || primaryInvestigatorId == null || 
    	   creatorId == null || registratorId == null || labBranch == null ||
    	   uploadRequests == null || uploadRequests.size() == 0) {
    	   throw new HpcException("Invalid add managed-dataset input", 
    			                  HpcErrorType.INVALID_INPUT);
    	}
    	// Create the ManagedDataset domain object.
    	HpcManagedDataset managedDataset = new HpcManagedDataset();

    	// Generate and set its ID.
    	managedDataset.setId(UUID.randomUUID().toString());

    	// Populate attributes.
    	managedDataset.setName(name);
    	managedDataset.setPrimaryInvestigatorId(primaryInvestigatorId);
    	managedDataset.setCreatorId(creatorId);
    	managedDataset.setRegistratorId(registratorId);
    	managedDataset.setLabBranch(labBranch);
    	managedDataset.setCreated(Calendar.getInstance());
    	
    	// Attach the files to this dataset.
    	for(HpcFileUploadRequest uploadRequest : uploadRequests) {
    		// Validate the upload file request.
    		if(!isValidFileUploadRequest(uploadRequest)) {
    		   throw new HpcException("Invalid file upload request", 
		                              HpcErrorType.INVALID_INPUT);
    		}
    		
    		// Map the upload request to a managed file instance.
    		HpcFile file = new HpcFile();
    		file.setId(UUID.randomUUID().toString());
    		file.setType(uploadRequest.getType());
    		file.setSize(0);
    		file.setSource(uploadRequest.getLocations().getSource());
    		file.setLocation(uploadRequest.getLocations().getDestination());
    		
    		// Set the metadata.
    		HpcDatasetMetadata metadata = new HpcDatasetMetadata();
    		metadata.setPrimaryMetadata(uploadRequest.getMetadata());
    		file.setMetadata(metadata);
    		
    		// Add the managed file to the dataset.
    		managedDataset.getFiles().add(file);
    	}
    	
    	// Persist to Mongo.
    	managedDatasetDAO.add(managedDataset);
    	
    	return managedDataset.getId();
    }
    
    @Override
    public HpcManagedDataset get(String id) throws HpcException
    {
    	// Input validation.
    	try {
    	     if(id == null || UUID.fromString(id) == null) {
    	        throw new HpcException("Invalid managed date ID", 
    			                       HpcErrorType.INVALID_INPUT);
    	     }
    	} catch(IllegalArgumentException e) {
    		    throw new HpcException("Invalid managed date ID", 
                                       HpcErrorType.INVALID_INPUT, e);
    	}
    	
    	return managedDatasetDAO.get(id);

    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Validate a file upload request object.
     *
     * @param file the object to be validated.
     * @return true if valid, false otherwise.
     */
    private boolean isValidFileUploadRequest(HpcFileUploadRequest request) 
    {
    	if(request.getType() == null || request.getLocations() == null ||
    	   !isValidFileLocation(request.getLocations().getSource()) ||
    	   !isValidFileLocation(request.getLocations().getDestination()) ||
    	   !isValidDatasetPrimaryMetadata(request.getMetadata())) {
    	   logger.info("Invalid File Upload Request");
    	   return false;
    	}
    	return true;
    }  
    
    /**
     * Validate a file location object.
     *
     * @param location the object to be validated.
     * @return true if valid, false otherwise.
     */
    private boolean isValidFileLocation(HpcFileLocation location) 
    {
    	if(location == null ||
    	   location.getEndpoint() == null ||
    	   location.getPath() == null) {
     	   logger.info("Invalid File Location");
     	   return false;
    	}
    	return true;
    }  
    
    /**
     * Validate a dataset primary metadata object.
     *
     * @param metadata the object to be validated.
     * @return true if valid, false otherwise.
     */
    private boolean isValidDatasetPrimaryMetadata(HpcDatasetPrimaryMetadata metadata) 
    {
    	if(metadata == null ||
    	   metadata.getFundingOrganization() == null) {
    	   logger.info("Invalid Dataset Primary Metadata");
     	   return false;
    	}
    	return true;
    }  
    
    /*
    
    @Override
    public boolean transferDataset(HpcManagedFile file,String username, String password) throws HpcException
    {   
    	try{
        	HpcDataTransfer hdt = new GlobusOnlineDataTranfer();
        	return hdt.transferDataset(file,username, password);    		
    	}catch(Exception ex)
    	{
    		throw new HpcException("Error while transfer",HpcErrorType.INVALID_INPUT);
    	}

    }*/
}

 