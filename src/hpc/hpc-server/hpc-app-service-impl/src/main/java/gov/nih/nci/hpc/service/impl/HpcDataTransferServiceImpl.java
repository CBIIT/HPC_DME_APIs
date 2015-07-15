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

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferAccountValidatorProvider;
import gov.nih.nci.hpc.integration.HpcDataTransferAccountValidatorProxy;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.service.HpcDataTransferService;

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
    
    // The Data Transfer Account Validator Provider.
    private HpcDataTransferAccountValidatorProvider 
                                  dataTransferAccountValidatorProvider = null;
    
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
     * @param dataTransferAccountValidatorProvider 
     *        The data transfer account validator provider instance.
     */
    private HpcDataTransferServiceImpl(
    		   HpcDataTransferProxy dataTransferProxy,
    		   HpcDataTransferAccountValidatorProvider dataTransferAccountValidatorProvider) 
    		   throws HpcException
    {
    	if(dataTransferProxy == null || 
    	   dataTransferAccountValidatorProvider == null) {
     	   throw new HpcException("Null Integration beans",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.dataTransferProxy = dataTransferProxy;
    	this.dataTransferAccountValidatorProvider = 
    			                         dataTransferAccountValidatorProvider;
    }      
     
    //---------------------------------------------------------------------//
    // HpcDataTransferService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public HpcDataTransferReport 
                  transferDataset(HpcDataTransferLocations dataTransferLocations,
	                              HpcDataTransferAccount dataTransferAccount,
	                              String nihUsername) 
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
        			                          dataTransferAccount.getPassword(),
        			                          nihUsername);
        	 
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
    	
    	// Get the validator for this account type.
    	HpcDataTransferAccountValidatorProxy validator = 
    	   dataTransferAccountValidatorProvider.get(
    			                       dataTransferAccount.getAccountType());
    	if(validator == null) {
    	   throw new HpcException("Could not locate a validator for: " +
    			                  dataTransferAccount.getAccountType(), 
	                              HpcErrorType.UNEXPECTED_ERROR);
    	}
		return validator.validateDataTransferAccount(dataTransferAccount);
	}  

	
	   @Override
	    public HpcDataTransferReport 
	                  retriveTransferStatus(String taskId) 
		                             throws HpcException
	    {	
	    	try {
	        	 return dataTransferProxy.getTaskStatusReport(taskId);
	        	 
	    	} catch(Exception ex) {
	    		    throw new HpcException("Error while retriving status",
	    		    		               HpcErrorType.DATA_TRANSFER_ERROR);
	    	}
	    }	
}
 