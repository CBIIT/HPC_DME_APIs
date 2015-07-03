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

import gov.nih.nci.hpc.service.HpcUserService;

import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcNihAccount;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.domain.user.HpcDataTransferType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.dao.HpcUserDAO;
import gov.nih.nci.hpc.exception.HpcException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

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
    private HpcUserDAO userDAO = null;
    
    // The Data Transfer Service instance.
    private HpcDataTransferService dataTransferService = null;
    
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
    private HpcUserServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param userDAO The user DAO instance.
     * @param dataTransferService The data transfer service instance.
     */
    private HpcUserServiceImpl(HpcUserDAO userDAO,
    		                   HpcDataTransferService dataTransferService) 
    		                  throws HpcException
    {
    	if(userDAO == null || dataTransferService == null) {
     	   throw new HpcException("Null UserDAO or DataTransferService instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.userDAO = userDAO;
    	this.dataTransferService = dataTransferService;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcUserService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void add(HpcNihAccount nihAccount, 
	                HpcDataTransferAccount dataTransferAccount) 
	               throws HpcException
    {
    	// Input validation.
    	if(!HpcDomainValidator.isValidNihAccount(nihAccount) ||
    	   !HpcDomainValidator.isValidDataTransferAccount(dataTransferAccount)) {	
    	   throw new HpcException("Invalid add user input", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	// Check if the user already exists.
    	if(get(nihAccount.getUserId()) != null) {
    	   throw new HpcException("User already exists: nihUserId = " + 
    	                          nihAccount.getUserId(), 
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

    	user.setNihAccount(nihAccount);
    	user.setDataTransferAccount(dataTransferAccount);
    	
    	Calendar now = Calendar.getInstance();
    	user.setCreated(now);
    	user.setLastUpdated(now);
    	
    	// Persist to Mongo.
    	userDAO.add(user);
    }
    
    @Override
    public HpcUser get(String nihUserId) throws HpcException
    {
    	// Input validation.
    	if(nihUserId == null) {
    	   throw new HpcException("Null NIH user ID", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return userDAO.get(nihUserId);
    }
}

 