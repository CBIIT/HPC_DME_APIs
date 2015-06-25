/**
 * HpcManagedUserServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.service.HpcManagedUserService;

import gov.nih.nci.hpc.domain.model.HpcManagedUser;
import gov.nih.nci.hpc.domain.user.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcDataTransferType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.dao.HpcManagedUserDAO;
import gov.nih.nci.hpc.exception.HpcException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.Calendar;

/**
 * <p>
 * HPC Managed User Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcManagedUserServiceImpl implements HpcManagedUserService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Managed User DAO instance.
    private HpcManagedUserDAO managedUserDAO = null;
    
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
    private HpcManagedUserServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param managedUserDAO The managed user DAO instance.
     */
    private HpcManagedUserServiceImpl(HpcManagedUserDAO managedUserDAO)
                                     throws HpcException
    {
    	if(managedUserDAO == null) {
     	   throw new HpcException("Null HpcManagedDatasetDAO instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.managedUserDAO = managedUserDAO;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcManagedUserService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public String add(HpcUser user) throws HpcException
    {
    	// Input validation.
    	if(user == null || user.getNihUserId() == null || 
    	   user.getFirstName() == null || user.getLastName() == null ||
    	   user.getDataTransferAccount() == null ||
    	   user.getDataTransferAccount().getUsername() == null ||
    	   user.getDataTransferAccount().getPassword() == null ||
    	   user.getDataTransferAccount().getDataTransferType() == null) {
    	   throw new HpcException("Invalid add user input", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	// Check if the user already exists.
    	if(get(user.getNihUserId()) != null) {
    	   throw new HpcException("User already exists: nihUserId=" + 
    	                          user.getNihUserId(), 
    	                          HpcRequestRejectReason.USER_ALREADY_EXISTS);	
    	}
    	
    	// Create the ManagedDataset domain object.
    	HpcManagedUser managedUser = new HpcManagedUser();

    	// Generate and set its ID.
    	managedUser.setId(UUID.randomUUID().toString());

    	// Populate attributes.
    	managedUser.setUser(user);
    	Calendar now = Calendar.getInstance();
    	managedUser.setCreated(now);
    	managedUser.setLastUpdated(now);
    	
    	// Persist to Mongo.
    	managedUserDAO.add(managedUser);
   	
    	return managedUser.getId();
    }
    
    @Override
    public HpcManagedUser get(String nihUserId) throws HpcException
    {
    	return managedUserDAO.get(nihUserId);
    }
    

}

 