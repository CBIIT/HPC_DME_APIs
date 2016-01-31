/**
 * HpcUserServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidIntegratedSystemAccount;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidNciAccount;
import gov.nih.nci.hpc.dao.HpcUserDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcLdapAuthenticationProxy;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcUserService;

import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * <p>
 * HPC User Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcUserServiceImpl implements HpcUserService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The User DAO instance.
	@Autowired
    private HpcUserDAO userDAO = null;
    
    // The Data Transfer Service instance.
	@Autowired
    private HpcDataTransferService dataTransferService = null;
	
	// The LDAP authenticator instance.
	@Autowired
	HpcLdapAuthenticationProxy ldapAuthenticationProxy = null;
	
    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcUserServiceImpl() throws HpcException
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcUserService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void addUser(HpcNciAccount nciAccount, 
	                    HpcIntegratedSystemAccount dataTransferAccount,
	                    HpcIntegratedSystemAccount dataManagementAccount) 
	                   throws HpcException
    {
    	// Input validation.
    	if(!isValidNciAccount(nciAccount) ||
    	   !isValidIntegratedSystemAccount(dataTransferAccount) ||
    	   !isValidIntegratedSystemAccount(dataManagementAccount)) {	
    	   throw new HpcException("Invalid add user input", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	// Check if the user already exists.
    	if(getUser(nciAccount.getUserId()) != null) {
    	   throw new HpcException("User already exists: nciUserId = " + 
    	                          nciAccount.getUserId(), 
    	                          HpcRequestRejectReason.USER_ALREADY_EXISTS);	
    	}
    	
    	// Validate the data transfer account.
    	if(!dataTransferService.validateDataTransferAccount(dataTransferAccount)) {
    	   throw new HpcException(
    			        "Invalid Data Transfer Account: username = " + 
    			        dataTransferAccount.getUsername(), 
                        HpcRequestRejectReason.INVALID_DATA_TRANSFER_ACCOUNT);	
    	}
    	
    	// Create the User domain object.
    	HpcUser user = new HpcUser();

    	user.setNciAccount(nciAccount);
    	user.setDataTransferAccount(dataTransferAccount);
    	user.setDataManagementAccount(dataManagementAccount);
    	user.setCreated(Calendar.getInstance());
    	
    	// Persist to the DB.
    	persist(user);
    	
    	logger.debug("User Created: " + user);
    }
    
    @Override
    public HpcUser getUser(String nciUserId) throws HpcException
    {
    	// Input validation.
    	if(nciUserId == null) {
    	   throw new HpcException("Null NCI user ID", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return userDAO.getUser(nciUserId);
    }
    
    @Override
    public HpcRequestInvoker getRequestInvoker()
    {
    	return HpcRequestContext.getRequestInvoker();
    }
    
    @Override
    public void setRequestInvoker(HpcUser user)
    {
    	HpcRequestInvoker invoker = new HpcRequestInvoker();
    	if(user != null) {
    	   invoker.setNciAccount(user.getNciAccount());
    	   invoker.setDataTransferAccount(user.getDataTransferAccount());
    	   invoker.setDataManagementAccount(user.getDataManagementAccount());
    	   invoker.setDataManagementAuthenticatedToken(null);
    	}
    	
    	HpcRequestContext.setRequestInvoker(invoker);
    }
    
    @Override
	public boolean authenticate(String userName, String password) throws HpcException
	{
    	// Input validation.
		if(userName == null || userName.trim().length() == 0) {
		   throw new HpcException("User name cannot be null or empty", 
				                  HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if(password == null || password.trim().length() == 0) {
		   throw new HpcException("Password cannot be null or empty", 
				                  HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		return ldapAuthenticationProxy.authenticate(userName, password);
	}
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
    
    /**
     * Persist user to the DB.
     *
     * @param user The user to be persisted.
     * 
     * @throws HpcException
     */
    private void persist(HpcUser user) throws HpcException
    {
    	if(user != null) {
    	   user.setLastUpdated(Calendar.getInstance());
    	   userDAO.upsert(user);
    	}
    }
}

 