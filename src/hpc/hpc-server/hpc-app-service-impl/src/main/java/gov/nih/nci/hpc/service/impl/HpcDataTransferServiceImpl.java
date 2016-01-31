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

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferAccountValidatorProvider;
import gov.nih.nci.hpc.integration.HpcDataTransferAccountValidatorProxy;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.service.HpcDataTransferService;

import org.springframework.beans.factory.annotation.Autowired;

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
                  transferData(HpcFileLocation source, HpcFileLocation destination) 
	                          throws HpcException
    {   
    	// Input validation.
    	if(!HpcDomainValidator.isValidFileLocation(source) || 
    	   !HpcDomainValidator.isValidFileLocation(destination)	) {	
    	   throw new HpcException("Invalid data transfer request input", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
        	
  	    return dataTransferProxy.transferData(getDataTransferAccount(), 
  	    		                              source, destination);
    }
    
	@Override
	public boolean validateDataTransferAccount(
                                       HpcIntegratedSystemAccount dataTransferAccount)
                                       throws HpcException
    {
    	// Input validation.
    	if(!HpcDomainValidator.isValidIntegratedSystemAccount(dataTransferAccount)) {	
    	   throw new HpcException("Invalid data transfer account", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
    	// Get the validator for this account type.
    	HpcDataTransferAccountValidatorProxy validator = 
    	   dataTransferAccountValidatorProvider.get(
    			                       dataTransferAccount.getIntegratedSystem());
    	if(validator == null) {
    	   throw new HpcException("Could not locate a validator for: " +
    			                  dataTransferAccount.getIntegratedSystem(), 
	                              HpcErrorType.UNEXPECTED_ERROR);
    	}
		return validator.validateDataTransferAccount(dataTransferAccount);
	}  

	@Override   
	public HpcDataTransferReport 
	       getTransferRequestStatus(String dataTransferId) 
		                           throws HpcException
    {	
		try {
             return dataTransferProxy.getTaskStatusReport(getDataTransferAccount(), 
            		                                     dataTransferId);
        	 
		} catch(Exception ex) {
    	        throw new HpcException("Error while retriving status",
    		         		           HpcErrorType.DATA_TRANSFER_ERROR);
        }
    }	
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Get the data transfer account from the request context.
     *
     * @throws HpcException If the account is not set or invalid.
     */
    private HpcIntegratedSystemAccount getDataTransferAccount() throws HpcException
    {
    	HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
    	if(invoker == null || 
    	   !isValidIntegratedSystemAccount(invoker.getDataTransferAccount())) {
	       throw new HpcException("Unknown user or invalid data transfer account",
			                      HpcRequestRejectReason.INVALID_DATA_MANAGEMENT_ACCOUNT);
    	}
    	
    	return invoker.getDataTransferAccount();
    }
}
 