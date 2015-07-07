/**
 * HpcDataTransferAccountValidatorProviderImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccountType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferAccountValidatorProvider;
import gov.nih.nci.hpc.integration.HpcDataTransferAccountValidatorProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * HPC Data Transfer Account Validator Provider. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataTransferAccountValidatorProviderImpl 
             implements HpcDataTransferAccountValidatorProvider
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	private Map<HpcDataTransferAccountType, HpcDataTransferAccountValidatorProxy> 
	        accountValidators = new HashMap<HpcDataTransferAccountType, 
	                                        HpcDataTransferAccountValidatorProxy>();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcDataTransferAccountValidatorProviderImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param accountValidators The data transfer account validators.
     * 
     * @throws HpcException If providers map was not provided
     */
    private HpcDataTransferAccountValidatorProviderImpl(
    		       Map<HpcDataTransferAccountType, 
    		           HpcDataTransferAccountValidatorProxy> accountValidators) 
    		       throws HpcException
    {
    	if(accountValidators == null || accountValidators.size() == 0) {
     	   throw new HpcException("Null or empty account providers map instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.accountValidators.putAll(accountValidators);
    } 
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataTransferAccountValidatorProvider Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override    
    public HpcDataTransferAccountValidatorProxy 
                                 get(HpcDataTransferAccountType accountType) 
    {
    	return accountValidators.get(accountType);
    }                    
}

 