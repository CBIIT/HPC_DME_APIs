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

import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcUser;
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
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
    // The Data Transfer Proxy.
	@Autowired
    private HpcDataTransferProxy dataTransferProxy = null;
    
    // The Data Transfer Account Validator Provider.
	@Autowired
    private HpcDataTransferAccountValidatorProvider 
                                  dataTransferAccountValidatorProvider = null;
    
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcDataTransferServiceImpl() throws HpcException
    {
    }   
    
    //---------------------------------------------------------------------//
    // HpcDataTransferService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public HpcDataTransferReport 
                  transferDataset(HpcDataTransferLocations dataTransferLocations,
                		          HpcUser user, String datasetId) 
	                             throws HpcException
    {   
    	// Input validation.
    	if(!HpcDomainValidator.isValidDataTransferLocations(dataTransferLocations) ||
    	   !HpcDomainValidator.isValidUser(user)) {	
    	   throw new HpcException("Invalid data transfer request input", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	dataTransferLocations.getDestination().setPath("/"+datasetId);
        	
  	    return dataTransferProxy.transferDataset(dataTransferLocations, user);
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
       getTransferRequestStatus(String dataTransferId, 
     	                        HpcDataTransferAccount dataTransferAccount) 
		                       throws HpcException
	   {	
		   try {
	            return dataTransferProxy.getTaskStatusReport(dataTransferId, dataTransferAccount);
	        	 
	       } catch(Exception ex) {
	    	       throw new HpcException("Error while retriving status",
	    		     		              HpcErrorType.DATA_TRANSFER_ERROR);
	       }
	   }	
}
 