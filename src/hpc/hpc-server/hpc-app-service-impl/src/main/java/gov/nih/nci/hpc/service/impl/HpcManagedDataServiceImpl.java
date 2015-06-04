/**
 * HpcManagedDatasetsServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.service.HpcManagedDataService;
import gov.nih.nci.hpc.domain.HpcDataset;
import gov.nih.nci.hpc.domain.HpcManagedDataType;
import gov.nih.nci.hpc.domain.HpcManagedData;
import gov.nih.nci.hpc.dao.HpcManagedDataDAO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.Calendar;

/**
 * <p>
 * HPC Managed Data Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedDataServiceImpl implements HpcManagedDataService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Managed Data DAO instance.
    private HpcManagedDataDAO managedDataDAO = null;
    
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
    private HpcManagedDataServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param managedDatDAO The managed dat DAO instance.
     */
    private HpcManagedDataServiceImpl(HpcManagedDataDAO managedDataDAO)
                                     throws HpcException
    {
    	if(managedDataDAO == null) {
     	   throw new HpcException("Null HpcManagedDataDAO instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.managedDataDAO = managedDataDAO;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcManagedDataService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public String add(HpcManagedDataType type,
    		          List<HpcDataset> datasets) throws HpcException
    {
    	// Input validation.
    	if(type == null || datasets == null || datasets.size() == 0) {
    	   throw new HpcException("Invalid add managed-data input", 
    			                  HpcErrorType.INVALID_INPUT);
    	}
    	logger.info("in manageddata service ()");
    	// Create the domain object.
    	HpcManagedData managedData = new HpcManagedData();
    	logger.info("in manageddata service set uuid ()" );
    	// Generate and set an ID.
    	managedData.setId(UUID.randomUUID().toString());
    	logger.info("in manageddata service set type ()" + type );
    	// Populate type and datasets
    	managedData.setType(type);
    	managedData.setCreated(Calendar.getInstance());
    	for(HpcDataset dataset : datasets) {
    		dataset.setId(UUID.randomUUID().toString());
    		dataset.setSize(0);
    		logger.info("in manageddata service validating dataset"  );
    		validate(dataset);
    		logger.info("in manageddata service Adding dataset"  );
    		managedData.getDatasets().add(dataset);
    	}
    	
    	// Persist to Mongo.
    	managedDataDAO.add(managedData);
    	
    	return managedData.getId();
    }
    
    @Override
    public HpcManagedData get(String id) throws HpcException
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
    	
    	return managedDataDAO.get(id);

    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Validate a dataset object.
     *
     * @param dataset the object to be validated
     * @throws HpcException If the dataset object is invalid.
     */
    private void validate(HpcDataset dataset) throws HpcException
    {
    	if(dataset.getName() == null || dataset.getType() == null ||
    	   dataset.getLocation() == null || 
    	   dataset.getLocation().getFacility() == null ||
    	   dataset.getLocation().getEndpoint() == null ||
    	   dataset.getLocation().getDataTransfer() == null) {
     	   throw new HpcException("Invalid dataset input", 
	                              HpcErrorType.INVALID_INPUT);    	   
    	}
    }  
}

 