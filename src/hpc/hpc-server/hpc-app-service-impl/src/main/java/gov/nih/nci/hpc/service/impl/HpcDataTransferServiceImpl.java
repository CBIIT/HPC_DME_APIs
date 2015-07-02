/**
 * HpcDataTransferServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.service.HpcDataTransferService;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.integration.HpcDataTransferAccountValidatorProxy;
import gov.nih.nci.hpc.dao.HpcUserDAO;
import gov.nih.nci.hpc.exception.HpcException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Data Transfer Service Implementation.
 * </p>
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 * @version $Id$
 */

public class HpcDataTransferServiceImpl implements HpcDataTransferService
{            
    // The Data Transfer Proxy.
    private HpcDataTransferProxy dataTransferProxy = null;
    
    // The Data Transfer Account Validator Proxy.
    private HpcDataTransferAccountValidatorProxy 
                                  dataTransferAccountValidatorProxy = null;
    
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
    private HpcDataTransferServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param dataTransferProxy The data transfer proxy instance.
     * @param dataTransferAccountValidatorProxy 
     *        The data transfer account validator proxy instance.
     */
    private HpcDataTransferServiceImpl(
    		   HpcDataTransferProxy dataTransferProxy,
    		   HpcDataTransferAccountValidatorProxy dataTransferAccountValidatorProxy) 
    		   throws HpcException
    {
    	if(dataTransferProxy == null || 
    	   dataTransferAccountValidatorProxy == null) {
     	   throw new HpcException("Null Integration beans",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.dataTransferProxy = dataTransferProxy;
    	this.dataTransferAccountValidatorProxy = 
    			                         dataTransferAccountValidatorProxy;
    }      
     
    //---------------------------------------------------------------------//
    // HpcDataTransferService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public HpcDataTransferReport 
                  transferDataset(HpcDataTransferLocations dataTransferLocations,
	                              HpcDataTransferAccount dataTransferAccount) 
	                             throws HpcException
    {   
    	// Input validation.
    	if(!HpcDomainValidator.isValidDataTransferLocations(dataTransferLocations) ||
    	   !HpcDomainValidator.isValidDataTransferAccount(dataTransferAccount)) {	
    	   throw new HpcException("Invalid data transfer request input", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	try {
        	 return dataTransferProxy.transferDataset(
        			                          dataTransferLocations,
        			                          dataTransferAccount.getUsername(), 
        			                          dataTransferAccount.getPassword());
        	 
    	} catch(Exception ex) {
    		    throw new HpcException("Error while transfer",
    		    		               HpcErrorType.DATA_TRANSFER_ERROR);
    	}
    }
    
	@Override
	public boolean validateDataTransferAccount(
                                       HpcDataTransferAccount dataTransferAccount)
                                       throws HpcException
    {
    	// Input validation.
    	if(!HpcDomainValidator.isValidDataTransferAccount(dataTransferAccount)) {	
    	   throw new HpcException("Invalid data transfer account", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
		return dataTransferAccountValidatorProxy.validateDataTransferAccount(
				                                         dataTransferAccount);
	}  

}
 