/**
 * HpcSecurityRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import gov.nih.nci.hpc.bus.HpcSecurityBusService;
import gov.nih.nci.hpc.dto.datamanagement.HpcGroupRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcGroupResponseDTO;
import gov.nih.nci.hpc.dto.user.HpcAuthenticationRequestDTO;
import gov.nih.nci.hpc.dto.user.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.dto.user.HpcUpdateUserRequestDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcSecurityRestService;

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
 * @version $Id: HpcSecurityRestServiceImpl.java 1015 2016-03-27 14:44:36Z rosenbergea $
 */

public class HpcSecurityRestServiceImpl extends HpcRestServiceImpl
             implements HpcSecurityRestService
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The User Business Service instance.
	@Autowired
    private HpcSecurityBusService securityBusService = null;
    
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
    private HpcSecurityRestServiceImpl() throws HpcException
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
		logger.info("Invoking RS: PUT /user: " + userRegistrationDTO);
		
		try {
			securityBusService.registerUser(userRegistrationDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: PUT /user failed:", e);
			    return errorResponse(e);
		}
		
		return createdResponse(userRegistrationDTO.getNciAccount().getUserId());
	}
    
    @Override
    public Response updateUser(String nciUserId,
                               HpcUpdateUserRequestDTO updateUserRequestDTO)
    {	
		logger.info("Invoking RS: POST /user: " + updateUserRequestDTO);
		
		try {
			securityBusService.updateUser(nciUserId, updateUserRequestDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /user failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(null, false);
	}   
    
    @Override
    public Response getUser(String nciUserId)
    {
		logger.info("Invoking RS: GET /user/{nciUserId}: " + nciUserId);
		
		HpcUserDTO userDTO = null;
		try {
			 userDTO = securityBusService.getUser(nciUserId);
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /user/{nciUserId} failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(userDTO, true);
	}
    
    @Override
    public Response authenticateUser(HpcAuthenticationRequestDTO authenticationRequest)
    {
		logger.info("Invoking RS: POST /user/authenticate: " + authenticationRequest.getUserName());
		
		HpcAuthenticationResponseDTO authenticationResponse = null;
		try {
			 authenticationResponse = securityBusService.authenticate(authenticationRequest, true); 
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /user/authenticate failed: " + 
		                     authenticationRequest.getUserName(), e);
			    return errorResponse(e);
		}
		
		return okResponse(authenticationResponse, false);
	}    

    @Override
    public Response setGroup(HpcGroupRequestDTO groupRequest)
    {
    	logger.info("Invoking RS: POST /group: " + groupRequest);
    	
    	HpcGroupResponseDTO groupResponse = null;
		try {
			 groupResponse = securityBusService.setGroup(groupRequest);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /group: " + groupResponse + " failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(groupResponse, false);
    }    
}

 