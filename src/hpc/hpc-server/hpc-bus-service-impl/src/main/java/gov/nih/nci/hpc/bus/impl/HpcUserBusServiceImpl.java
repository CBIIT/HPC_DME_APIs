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
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.dto.user.HpcAuthenticationRequestDTO;
import gov.nih.nci.hpc.dto.user.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcUserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
    // Constants
    //---------------------------------------------------------------------//    
    
    // 'Not-Registered' user role.
	private final static String NOT_REGISTERED_ROLE = "notregistered";
	
    // Default user role.
	private final static String DEFAULT_ROLE = "rodsuser";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // Application service instances.
	
	@Autowired
    private HpcUserService userService = null;
	
	@Autowired
    private HpcDataManagementService dataManagementService = null;
	
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
    private HpcUserBusServiceImpl() throws HpcException
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcUserBusService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void registerUser(HpcUserDTO userRegistrationDTO)  
    		                throws HpcException
    {
    	logger.info("Invoking registerDataset(HpcUserDTO): " + 
                    userRegistrationDTO);
    	
    	// Input validation.
    	if(userRegistrationDTO == null) {
    	   throw new HpcException("Null HpcUserRegistrationDTO",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Create data management account if not provided.
    	if(userRegistrationDTO.getDataManagementAccount() == null) {
    	   // Create a data management account.
    	   dataManagementService.addUser(
    			         userRegistrationDTO.getNciAccount(),
    			         userRegistrationDTO.getDataManagementUserType() != null ? 
    			         userRegistrationDTO.getDataManagementUserType() : DEFAULT_ROLE);
    	   
    	   // Add the new account to the DTO.
    	   HpcIntegratedSystemAccount dataManagementAccount = 
    			                      new HpcIntegratedSystemAccount();
    	   dataManagementAccount.setUsername(userRegistrationDTO.getNciAccount().getUserId());
    	   dataManagementAccount.setPassword("N/A - LDAP Authenticated");
    	   dataManagementAccount.setIntegratedSystem(HpcIntegratedSystem.IRODS);
    	   userRegistrationDTO.setDataManagementAccount(dataManagementAccount);
    	}
    	
    	// Add the user to the managed collection.
    	userService.addUser(userRegistrationDTO.getNciAccount(), 
    			            userRegistrationDTO.getDataTransferAccount(),
    			            userRegistrationDTO.getDataManagementAccount());
    }
    
    @Override
    public HpcUserDTO getUser(String nciUserId) throws HpcException
    {
    	logger.info("Invoking getDataset(String nciUserId): " + nciUserId);
    	
    	// Input validation.
    	if(nciUserId == null) {
    	   throw new HpcException("Null NCI User ID",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Get the managed data domain object.
    	HpcUser user = userService.getUser(nciUserId);
    	if(user == null) {
    	   return null;
    	}
    	
    	// Map it to the DTO.
    	HpcUserDTO userDTO = new HpcUserDTO();
    	userDTO.setNciAccount(user.getNciAccount());
    	userDTO.setDataTransferAccount(user.getDataTransferAccount());
    	userDTO.setDataManagementAccount(user.getDataManagementAccount());
    	
    	// Mask passwords.
    	maskPasswords(userDTO);
    	
    	return userDTO;
    }
    
    @Override
    public HpcAuthenticationResponseDTO authenticate(
    		                HpcAuthenticationRequestDTO authenticationRequest, 
    		                boolean ldapAuthentication) 
                            throws HpcException
    {
    	// Input Validation
    	if(authenticationRequest == null) {
      	   throw new HpcException("Null authentication request",
      			                  HpcErrorType.INVALID_REQUEST_INPUT); 	
      	}
    	
    	// LDAP authentication.
    	boolean userAuthenticated =
    			    ldapAuthentication ? 
    			        userService.authenticate(authenticationRequest.getUserName(), 
    			                         		 authenticationRequest.getPassword()) : 
    			        false;
                    
    	// Get the HPC user.
    	HpcUser user = null;
    	try {
    	     user = userService.getUser(authenticationRequest.getUserName());
			
    	} catch(HpcException e) {
    	}
    	
    	if(user == null) {
    	   // This is a request from a user that is not registered with HPC.
    	   logger.info("Service call for a user that is not registered with HPC. NCI User-id: " + 
    	               authenticationRequest.getUserName());
    	
    	   user = new HpcUser();
 		   HpcNciAccount nciAccount = new HpcNciAccount();
 		   nciAccount.setUserId("Unknown-NCI-User-ID");
     	   user.setNciAccount(nciAccount);
     	   user.setDataManagementAccount(null);
     	   user.setDataTransferAccount(null);
        }
    	
    	// If the user was authenticated w/ LDAP, then we use the NCI credentials to access
    	// Data Management (iRODS).
    	if(userAuthenticated) {
    	   HpcIntegratedSystemAccount dataManagementAccount = new HpcIntegratedSystemAccount();
    	   dataManagementAccount.setIntegratedSystem(HpcIntegratedSystem.IRODS);
    	   dataManagementAccount.setUsername(authenticationRequest.getUserName());
    	   dataManagementAccount.setPassword(authenticationRequest.getPassword());
    	   user.setDataManagementAccount(dataManagementAccount);
    	}
    	
    	// Populate the request context with the HPC user.
    	userService.setRequestInvoker(user);
    	
    	// Prepare and return a response DTO.
    	HpcAuthenticationResponseDTO authenticationResponse = new HpcAuthenticationResponseDTO();
    	authenticationResponse.setAuthenticated(ldapAuthentication ? userAuthenticated : true);	                
    	authenticationResponse.setUserRole(
    			      user.getDataManagementAccount() != null ? 
    			      dataManagementService.getUserType(user.getDataManagementAccount().getUsername()) : 
    			      NOT_REGISTERED_ROLE);
    	
    	return authenticationResponse;
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Mask account passwords
     * 
     * @param userDTO the user DTO to have passwords masked.
     */
    
    private void maskPasswords(HpcUserDTO userDTO)
    {
    	if(userDTO.getDataManagementAccount() != null) {
    	   userDTO.getDataManagementAccount().setPassword("*****");
    	}
    	if(userDTO.getDataTransferAccount() != null) {
    	   userDTO.getDataTransferAccount().setPassword("*****");
    	}
    }
}

 