/**
 * HpcDataManagementAuthenticator.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Authenticate to Data Management based on invoker credentials.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataManagementAuthenticator
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     */
	private HpcDataManagementAuthenticator()
    {
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Get the data management authenticated token from the request context.
     * If it's not in the context, get a token by authenticating.
     *
     * @return A data management authenticated token.
     * @throws HpcException If it failed to obtain an authentication token.
     */
    public Object getAuthenticatedToken() throws HpcException
    {
    	HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
    	if(invoker == null) {
	       throw new HpcException("Unknown user",
			                      HpcRequestRejectReason.INVALID_DATA_MANAGEMENT_ACCOUNT);
    	}
    	
    	if(invoker.getDataManagementAuthenticatedToken() != null) {
    	   return invoker.getDataManagementAuthenticatedToken();
    	}
    	
    	// No authenticated token found in the request token. Authenticate the invoker.
    	HpcIntegratedSystemAccount dataManagementAccount = invoker.getDataManagementAccount();
    	if(dataManagementAccount == null) {
    	   throw new HpcException("Unknown data management account",
                                  HpcRequestRejectReason.INVALID_DATA_MANAGEMENT_ACCOUNT);
    	}
    	
    	// Authenticate w/ data management.
    	
    	// TODO - workaround to login w/ Token - Test and fix this.
    	//dataManagementAccount.getProperties().clear();
    	
    	Object token = dataManagementProxy.authenticate(dataManagementAccount);
    	if(token == null) {
    	   throw new HpcException("Invalid data management account credentials",
                                  HpcRequestRejectReason.INVALID_DATA_MANAGEMENT_ACCOUNT);
    	}
    	
    	// Store token on the request context.
    	invoker.setDataManagementAuthenticatedToken(token);
    	return token;
    } 
}