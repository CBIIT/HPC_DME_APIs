/**
 * HpcUserRegistrationRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import gov.nih.nci.hpc.transfer.HpcDataTransfer;
import gov.nih.nci.hpc.transfer.impl.GlobusOnlineDataTranfer;
import gov.nih.nci.hpc.ws.rs.HpcUserRegistrationRestService;
import gov.nih.nci.hpc.dto.userregistration.HpcUserDTO;
import gov.nih.nci.hpc.domain.user.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Context;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC User Registration REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcUserRegistrationRestServiceImpl extends HpcRestServiceImpl
             implements HpcUserRegistrationRestService
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Registration Business Service instance.
    //private HpcDataRegistrationService registrationBusService = null;
    
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
    private HpcUserRegistrationRestServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }  
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param registrationBusService The registration business service.
     * 
     * @throws HpcException If the bus service is not provided by Spring.
     */
    private HpcUserRegistrationRestServiceImpl(
    		       String registrationBusService)
                   throws HpcException
    {
    	if(registrationBusService == null) {
    	   throw new HpcException("Null HpcDataRegistrationService instance",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	//this.registrationBusService = registrationBusService;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataRegistrationRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response getUser(String id)
    {
		logger.info("Invoking RS: GET /user/{id}");
		
		HpcUserDTO output = new HpcUserDTO();
		HpcUser user = new HpcUser();
		user.setNihUserId("u1c056");
		user.setFirstName("Prasad");
		user.setLastName("Konka");
		HpcDataTransferAccount dta = new HpcDataTransferAccount();
		dta.setUsername("globus-user");
		dta.setPassword("***REMOVED***");
		user.setDataTransferAccount(dta);
		output.setUser(user);
		
		
		/*try {
			 registrationOutput = registrationBusService.getRegisteredData(id);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /registration failed:", e);
			    return toResponse(e);
		}*/
		
		return toOkResponse(output);
	}
    
    @Override
    public Response registerUser(HpcUserDTO userDTO)
    {	
		logger.info("Invoking RS: POST /registration");
		
		String registeredDataId = "Mock-User-ID";
		/*try {
			 registeredDataId = 
		     registrationBusService.registerData(registrationInput);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /registration failed:", e);
			    return toResponse(e);
		}*/
		
		return toCreatedResponse(registeredDataId);
	}
    
    @Override
    public boolean validateUser(HpcUserDTO userDTO)
    {	
		logger.info("Invoking RS: POST /validateUser");
    	HpcDataTransfer hdt = new GlobusOnlineDataTranfer(); 
		return hdt.validateUserAccount(userDTO.getUser().getDataTransferAccount().getUsername(), userDTO.getUser().getDataTransferAccount().getPassword());
	}    
}

 