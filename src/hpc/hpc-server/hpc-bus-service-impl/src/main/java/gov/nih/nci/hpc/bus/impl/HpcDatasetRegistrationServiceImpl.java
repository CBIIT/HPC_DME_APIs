/**
 * HpcDatasetRegistrationServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.bus.HpcDatasetRegistrationService;
import gov.nih.nci.hpc.service.HpcManagedDatasetService;
import gov.nih.nci.hpc.dto.api.HpcDatasetsRegistrationInputDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Dataset Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDatasetRegistrationServiceImpl 
             implements HpcDatasetRegistrationService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Managed Dataset application service instance.
    private HpcManagedDatasetService managedDatasetService = null;
    
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
    private HpcDatasetRegistrationServiceImpl() throws HpcException
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
    private HpcDatasetRegistrationServiceImpl(
    		          HpcManagedDatasetService managedDatasetService)
                      throws HpcException
    {
    	if(managedDatasetService == null) {
     	   throw new HpcException("Null HpcManagedDatasetService instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.managedDatasetService = managedDatasetService;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDatasetRegistrationService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void registerDataset(
                        HpcDatasetsRegistrationInputDTO registrationInputDTO)
                        throws HpcException
    {
    	// TODO: managedDatasetService.
    }
}

 