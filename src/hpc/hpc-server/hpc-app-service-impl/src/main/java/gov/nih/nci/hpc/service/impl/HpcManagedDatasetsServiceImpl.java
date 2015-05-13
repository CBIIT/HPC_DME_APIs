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

import gov.nih.nci.hpc.service.HpcManagedDatasetsService;
import gov.nih.nci.hpc.dto.types.HpcDataset;
import gov.nih.nci.hpc.dao.HpcManagedDatasetsDAO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <p>
 * HPC Managed Datasets Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedDatasetsServiceImpl 
             implements HpcManagedDatasetsService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Managed Datasets DAO instance.
    private HpcManagedDatasetsDAO managedDatasetsDAO = null;
    
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
    private HpcManagedDatasetsServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param managedDatasetDAO The managed dataset DAO instance.
     */
    private HpcManagedDatasetsServiceImpl(
    		                  HpcManagedDatasetsDAO managedDatasetsDAO)
                              throws HpcException
    {
    	if(managedDatasetsDAO == null) {
     	   throw new HpcException("Null HpcManagedDatasetsDAO instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.managedDatasetsDAO = managedDatasetsDAO;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcManagedDatasetsService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void add(List<HpcDataset> datasets) throws HpcException
    {
    	// TODO - Implement me
    }
}

 