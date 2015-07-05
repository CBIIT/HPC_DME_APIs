/**
 * HpcDatasetServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileUploadRequest;
import gov.nih.nci.hpc.dao.HpcDatasetDAO;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.dataset.HpcFileSet;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadata;
import gov.nih.nci.hpc.domain.model.HpcDataset;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDatasetService;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Dataset Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDatasetServiceImpl implements HpcDatasetService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Managed Dataset DAO instance.
    private HpcDatasetDAO datasetDAO = null;
    
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
    private HpcDatasetServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param datasetDAO The dataset DAO instance.
     */
    private HpcDatasetServiceImpl(HpcDatasetDAO datasetDAO)
    		                     throws HpcException
    {
    	if(datasetDAO == null) {
     	   throw new HpcException("Null DatasetDAO",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.datasetDAO = datasetDAO;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDatasetService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public String add(String name, String description, String comments,
		              List<HpcFileUploadRequest> uploadRequests) 
		             throws HpcException
    {
    	// Input validation.
    	if(name == null || description == null ||
    	   uploadRequests == null || uploadRequests.size() == 0) {
    	   throw new HpcException("Invalid add dataset input", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	// Create the Dataset domain object.
    	HpcDataset dataset = new HpcDataset();
    	
    	// Generate and set its ID.
    	dataset.setId(UUID.randomUUID().toString());
    	
    	// Associate the FileSet.
    	HpcFileSet fileSet = new HpcFileSet();

    	// Populate attributes.
    	fileSet.setName(name);
    	fileSet.setDescription(description);
    	fileSet.setComments(comments);
    	fileSet.setCreated(Calendar.getInstance());
    	
       	// Attach the files to this dataset.
    	for(HpcFileUploadRequest uploadRequest : uploadRequests) {
    		// Validate the upload file request.
    		if(!isValidFileUploadRequest(uploadRequest)) {
    		   throw new HpcException("Invalid file upload request: " + 
    		                          uploadRequest, 
		                              HpcErrorType.INVALID_REQUEST_INPUT);
    		}
    		
    		// Map the upload request to a managed file instance.
    		HpcFile file = new HpcFile();
    		file.setId(UUID.randomUUID().toString());
    		file.setType(uploadRequest.getType());
    		file.setSize(0);
    		file.setSource(uploadRequest.getLocations().getSource());
    		file.setLocation(uploadRequest.getLocations().getDestination());
    		
    		// Set the metadata.
    		HpcFileMetadata metadata = new HpcFileMetadata();
    		metadata.setPrimaryMetadata(uploadRequest.getMetadata());
    		file.setMetadata(metadata);
    		
    		// Add the managed file to the dataset.
    		fileSet.getFiles().add(file);
    	}
    	dataset.setFileSet(fileSet);
    	
    	// Persist to Mongo.
    	datasetDAO.add(dataset);
    	logger.debug("Dataset added: " + dataset);
    	
    	return dataset.getId();
    }
    
    @Override
    public HpcDataset getDataset(String id) throws HpcException
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
    	
    	return datasetDAO.getDataset(id);
    }
    
    @Override
    public List<HpcDataset> getDatasets(String userId, HpcDatasetUserAssociation association) 
 	                                   throws HpcException
 	{
    	return datasetDAO.getDatasets(userId, association);
 	}
    
    @Override
    public List<HpcDataset> getDatasets(String name) throws HpcException
 	{
    	return datasetDAO.getDatasets(name);
 	}
}

 