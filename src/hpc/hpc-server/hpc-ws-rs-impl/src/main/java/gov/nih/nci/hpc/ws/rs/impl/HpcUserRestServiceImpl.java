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

import gov.nih.nci.hpc.bus.HpcUserBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.dto.user.HpcUserCredentialsDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcUserRestService;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
	@Autowired
    private HpcUserBusService userBusService = null;
    
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
    
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcUserRestServiceImpl() throws HpcException
    {
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataRegistrationRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response registerUser(HpcUserDTO userRegistrationDTO)
    {	
		logger.info("Invoking RS: POST /user: " + userRegistrationDTO);
		
		try {
			 userBusService.registerUser(userRegistrationDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /user failed:", e);
			    return errorResponse(e);
		}
		
		return createdResponse(userRegistrationDTO.getNciAccount().getUserId());
	}
    
    @Override
    public Response getUser(String nciUserId)
    {
		logger.info("Invoking RS: GET /user/{nciUserId}: " + nciUserId);
		
		HpcUserDTO userDTO = null;
		try {
			 userDTO = userBusService.getUser(nciUserId);
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /user/{nciUserId} failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(userDTO, true);
	}
    
    @Override
    public Response authenticate(HpcUserCredentialsDTO credentials)
    {
		logger.info("Invoking RS: POST /user/authenticate: " + credentials.getUserName());
		boolean valid = false;
		try {
			 valid = userBusService.authenticate(credentials); 
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /user/{nciUserId} failed:", e);
			    return errorResponse(e);
		}
		if(valid)
			return okResponse(valid, true);
		else
		{
			HpcException ex = new HpcException("Invalid login credentials", HpcErrorType.REQUEST_AUTHENTICATION_FAILED);
			return errorResponse(ex);
			
		}
	}    
}

 