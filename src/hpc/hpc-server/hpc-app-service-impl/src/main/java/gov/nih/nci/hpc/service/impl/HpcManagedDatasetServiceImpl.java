/**
 * HpcManagedDatasetServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.service.HpcManagedDatasetService;
import gov.nih.nci.hpc.dto.HpcDatasetRegistrationInputDTO;
import gov.nih.nci.hpc.dao.HpcManagedDatasetDAO;
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
    private HpcManagedDatasetServiceImpl(
    		                        HpcManagedDatasetDAO managedDatasetDAO)
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
    public void add(HpcDatasetRegistrationInputDTO registrationInputDTO)
		           throws HpcException
    {
    	
    }
}

 