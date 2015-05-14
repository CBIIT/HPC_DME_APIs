/**
 * HpcDatasetsRegistrationServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.bus.HpcDatasetsRegistrationService;
import gov.nih.nci.hpc.service.HpcManagedDatasetsService;
import gov.nih.nci.hpc.dto.api.HpcDatasetsRegistrationInputDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Datasets Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDatasetsRegistrationServiceImpl 
             implements HpcDatasetsRegistrationService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Managed Datasets application service instance.
    private HpcManagedDatasetsService managedDatasetsService = null;
    
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
    private HpcDatasetsRegistrationServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param managedDatasetService The managed dataset application service.
     * 
     * @throws HpcException If managedDatasetService is null.
     */
    private HpcDatasetsRegistrationServiceImpl(
    		           HpcManagedDatasetsService managedDatasetsService)
                       throws HpcException
    {
    	if(managedDatasetsService == null) {
     	   throw new HpcException("Null HpcManagedDatasetService instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.managedDatasetsService = managedDatasetsService;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDatasetRegistrationService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void registerDatasets(
                        HpcDatasetsRegistrationInputDTO registrationInputDTO)
                        throws HpcException
    {
    	logger.info("Invoking registerDatasets()");
    	
    	// Input validation.
    	if(registrationInputDTO == null || 
    	   registrationInputDTO.getDatasets() == null || 
    	   registrationInputDTO.getDatasets().size() == 0) {
    	   throw new HpcException("Invalid HpcDatasetsRegistrationInputDTO",
    			                  HpcErrorType.INVALID_INPUT);	
    	}
    	
    	// Add the datasets to the managed collection.
    	managedDatasetsService.add(registrationInputDTO.getDatasets());
    	
    	// Transfer the datasets to their destination.
    	// TODO - implement.
    }
}

 