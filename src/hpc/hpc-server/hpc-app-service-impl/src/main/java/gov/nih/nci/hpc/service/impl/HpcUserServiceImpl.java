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

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidDataTransferAccount;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidNciAccount;
import gov.nih.nci.hpc.dao.HpcUserDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcUserService;

import java.util.Calendar;
import java.util.List;

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
    public void add(HpcNciAccount nciAccount, 
	                HpcDataTransferAccount dataTransferAccount) 
	               throws HpcException
    {
    	// Input validation.
    	if(!isValidNciAccount(nciAccount) ||
    	   !isValidDataTransferAccount(dataTransferAccount)) {	
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
    	user.setCreated(Calendar.getInstance());
    	
    	// Persist to Mongo.
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
    public List<HpcUser> getUsers(String firstName, String lastName) throws HpcException
    {
    	// Input validation.
    	if(firstName == null && lastName == null) {
    	   throw new HpcException("Null user first or last name", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return userDAO.getUsers(firstName, lastName);
    }
    
    @Override
    public void persist(HpcUser user) throws HpcException
    {
    	if(user != null) {
    	   user.setLastUpdated(Calendar.getInstance());
    	   userDAO.upsert(user);
    	}
    }
}

 