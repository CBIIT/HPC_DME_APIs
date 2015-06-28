/**
 * HpcUserBusServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.bus.HpcUserBusService;

import gov.nih.nci.hpc.service.HpcUserService;
import gov.nih.nci.hpc.dto.user.HpcUserRegistrationDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC User Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcUserBusServiceImpl implements HpcUserBusService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // Application service instances.
    private HpcUserService userService = null;
    
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
    private HpcUserBusServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param userService The user application service.
     * 
     * @throws HpcException If userService is null.
     */
    private HpcUserBusServiceImpl(HpcUserService userService)
                                 throws HpcException
    {
    	if(userService == null) {
     	   throw new HpcException("Null App Service(s) instance",
     			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
     	}
    	
    	this.userService = userService;
    } 
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcUserBusService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void registerUser(HpcUserRegistrationDTO userRegistrationDTO)  
    		                throws HpcException
    {
    	logger.info("Invoking registerDataset(HpcDatasetDTO): " + 
                    userRegistrationDTO);
    	
    	// Input validation.
    	if(userRegistrationDTO == null) {
    	   throw new HpcException("Null HpcUserRegistrationDTO",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Add the user to the managed collection.
    	userService.add(userRegistrationDTO.getNihAccount(), 
    			        userRegistrationDTO.getDataTransferAccount());
    }
    
    @Override
    public HpcUserDTO getUser(String nihUserId) throws HpcException
    {
    	logger.info("Invoking getDataset(String nihUserId): " + nihUserId);
    	
    	// Input validation.
    	if(nihUserId == null) {
    	   throw new HpcException("Null NIH User ID",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Get the managed data domain object.
    	HpcUser user = userService.get(nihUserId);
    	if(user == null) {
    	   return null;
    	}
    	
    	// Map it to the DTO.
    	HpcUserDTO userDTO = new HpcUserDTO();
    	userDTO.setNihAccount(user.getNihAccount());
    	userDTO.setDataTransferAccount(user.getDataTransferAccount());
    	userDTO.setCreated(user.getCreated());
    	userDTO.setLastUpdated(user.getLastUpdated());
    	
    	return userDTO;
    }
}

 