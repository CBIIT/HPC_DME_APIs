/**
 * HpcDataRegistrationServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.bus.HpcDataRegistrationService;
import gov.nih.nci.hpc.service.HpcManagedDataService;
import gov.nih.nci.hpc.dto.HpcDataRegistrationInput;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Data Registration Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataRegistrationServiceImpl 
             implements HpcDataRegistrationService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Managed Data application service instance.
    private HpcManagedDataService managedDataService = null;
    
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
    private HpcDataRegistrationServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param managedDatService The managed data application service.
     * 
     * @throws HpcException If managedDatasetService is null.
     */
    private HpcDataRegistrationServiceImpl(
    		       HpcManagedDataService managedDataService)
                   throws HpcException
    {
    	if(managedDataService == null) {
     	   throw new HpcException("Null HpcManagedDataService instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.managedDataService = managedDataService;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataRegistrationService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public String registerData(
                          HpcDataRegistrationInput registrationInput)
                          throws HpcException
    {
    	logger.info("Invoking registerData()");
    	
    	// Input validation.
    	if(registrationInput == null) {
    	   throw new HpcException("Null HpcDatasetsRegistrationInputDTO",
    			                  HpcErrorType.INVALID_INPUT);	
    	}
    	
    	// Add the datasets to the managed collection.
    	String managedDataId = 
    		   managedDataService.add(registrationInput.getType(),
    			                      registrationInput.getDatasets());
    	
    	// Transfer the datasets to their destination.
    	// TODO - implement.
    	
    	return managedDataId;
    }
}

 