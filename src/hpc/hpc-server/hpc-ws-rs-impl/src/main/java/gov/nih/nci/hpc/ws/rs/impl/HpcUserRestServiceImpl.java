/**
 * HpcUserRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import gov.nih.nci.hpc.ws.rs.HpcUserRestService;

import gov.nih.nci.hpc.bus.HpcUserBusService;
import gov.nih.nci.hpc.transfer.HpcDataTransfer;
import gov.nih.nci.hpc.transfer.impl.GlobusOnlineDataTranfer;
import gov.nih.nci.hpc.dto.user.HpcUserRegistrationDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;
import gov.nih.nci.hpc.domain.user.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Context;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC User REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcUserRestServiceImpl extends HpcRestServiceImpl
             implements HpcUserRestService
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The User Business Service instance.
    private HpcUserBusService userBusService = null;
    
    // The URI Info context instance.
    private @Context UriInfo uriInfo;
    
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
    
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcUserRestServiceImpl() throws HpcException
    {
    	super(false);
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }  
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param userBusService The user business service.
     * @param stackTraceEnabled If set to true, stack trace will be attached to
     *                          exception DTO.
     * 
     * @throws HpcException If parameters not provided by Spring.
     */
    private HpcUserRestServiceImpl(HpcUserBusService userBusService,
    		                       boolean stackTraceEnabled)
                                  throws HpcException
    {
    	super(stackTraceEnabled);
    	
    	if(userBusService == null) {
    	   throw new HpcException("Null HpcUserBusService instance",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	this.userBusService = userBusService;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataRegistrationRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response registerUser(HpcUserRegistrationDTO userRegistrationDTO)
    {	
		logger.info("Invoking RS: POST /user: " + userRegistrationDTO);
		
		try {
			 userBusService.registerUser(userRegistrationDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /user failed:", e);
			    return errorResponse(e);
		}
		
		return createdResponse(userRegistrationDTO.getUser().getNihUserId());
	}
    
    @Override
    public Response getUser(String nihUserId)
    {
		logger.info("Invoking RS: GET /user/{nihUserId}: " + nihUserId);
		
		HpcUserDTO userDTO = null;
		try {
			 userDTO = userBusService.getUser(nihUserId);
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /user/{nihUserId} failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(userDTO, true);
	}
	
	@Override
    public boolean validateUser(HpcUserRegistrationDTO userRegistrationDTO)
    {	
		logger.info("Invoking RS: POST /validateUser");
    	HpcDataTransfer hdt = new GlobusOnlineDataTranfer(); 
		return hdt.validateUserAccount(userRegistrationDTO.getUser().getDataTransferAccount().getUsername(), userRegistrationDTO.getUser().getDataTransferAccount().getPassword());
	}  
}

 