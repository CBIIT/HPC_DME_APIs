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

import java.util.Arrays;

import gov.nih.nci.hpc.bus.HpcUserBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.dto.user.HpcAuthenticationRequestDTO;
import gov.nih.nci.hpc.dto.user.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.dto.user.HpcUpdateUserRequestDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
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
    // Instance members
    //---------------------------------------------------------------------//

    // Application service instances.
	
	@Autowired
    private HpcUserService userService = null;
	
	@Autowired
    private HpcDataManagementService dataManagementService = null;
	
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
    	
    	// Validate the data transfer account.
    	if(!dataTransferService.validateDataTransferAccount(
    			                        userRegistrationDTO.getDataTransferAccount())) {
    	   throw new HpcException("Invalid Data Transfer Account", 
                                  HpcRequestRejectReason.INVALID_DATA_TRANSFER_ACCOUNT);	
    	}
    	
    	// Create data management account if not provided.
    	if(userRegistrationDTO.getDataManagementAccount() == null) {
    	   // Determine the user role to create. If not provided, default to USER.
    	   HpcUserRole role = userRegistrationDTO.getUserRole() != null ?
    			              roleFromString(userRegistrationDTO.getUserRole()) : 
    			              HpcUserRole.USER;
    			              
           // GROUP_ADMIN not supported by current Jargon API version. Respond with a workaround.
    	   if(role == HpcUserRole.GROUP_ADMIN) {
    		  throw new HpcException("GROUP_ADMIN currently not supported by the API. " +
    	                             "Create the account with a USER role, and then run " +
    				                 "'iadmin moduser' command to change the user's role to GROUP_ADMIN",
    				                 HpcRequestRejectReason.API_NOT_SUPPORTED);
    	   }
    			           
    	   // Create the data management account.
    	   dataManagementService.addUser(
    			         userRegistrationDTO.getNciAccount(), role);
    	   
    	   // Add the new account to the DTO.
    	   HpcIntegratedSystemAccount dataManagementAccount = 
    			                      new HpcIntegratedSystemAccount();
    	   dataManagementAccount.setUsername(userRegistrationDTO.getNciAccount().getUserId());
    	   dataManagementAccount.setPassword("N/A - LDAP Authenticated");
    	   dataManagementAccount.setIntegratedSystem(HpcIntegratedSystem.IRODS);
    	   userRegistrationDTO.setDataManagementAccount(dataManagementAccount);
    	}
    	
    	boolean registrationCompleted = false;
    	try {
    	     // Add the user to the managed collection.
    	     userService.addUser(userRegistrationDTO.getNciAccount(), 
    		                     userRegistrationDTO.getDataTransferAccount(),
    			                 userRegistrationDTO.getDataManagementAccount());
    	     registrationCompleted = true;
    	     
    	} finally {
    		       if(!registrationCompleted) {
    		    	  // Registration failed. Remove the data management account.
    		    	  dataManagementService.deleteUser(
    		    		  userRegistrationDTO.getNciAccount().getUserId());
    		       }
    	}
    }
    
    @Override
    public void updateUser(String nciUserId, 
                           HpcUpdateUserRequestDTO updateUserRequestDTO) 
                          throws HpcException
    {
    	logger.info("Invoking updateUser(HpcUserDTO): " + updateUserRequestDTO);
    	
    	// Input validation.
    	if(updateUserRequestDTO == null) {
    	   throw new HpcException("Null HpcUpdateUserRequestDTO",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Get the user.
    	HpcUser user = userService.getUser(nciUserId);
    	if(user == null) {
    	   throw new HpcException("User not found: " + nciUserId, 
    			                  HpcRequestRejectReason.INVALID_NCI_ACCOUNT);	
    	}
    	
    	// Validate the data transfer account if provided.
    	HpcIntegratedSystemAccount updateDataTransferAccount = 
    			                   updateUserRequestDTO.getDataTransferAccount();
    	if(updateDataTransferAccount != null) {
	       if(!dataTransferService.validateDataTransferAccount(updateDataTransferAccount)) {
	    	  throw new HpcException("Invalid Data Transfer Account", 
	                                 HpcRequestRejectReason.INVALID_DATA_TRANSFER_ACCOUNT);	
	    	}
    	} else {
    		    updateDataTransferAccount = user.getDataTransferAccount();
    	}
    	
    	// Determine update values.
    	String updateFirstName = updateUserRequestDTO.getFirstName() != null ?
    			                 updateUserRequestDTO.getFirstName() :
    			                 user.getNciAccount().getFirstName();
        String updateLastName = updateUserRequestDTO.getLastName() != null ?
    	    			        updateUserRequestDTO.getLastName() :
    	    			        user.getNciAccount().getLastName();
    	String updateDOC = updateUserRequestDTO.getDOC() != null ?
    	    	           updateUserRequestDTO.getDOC() :
    	    	    	   user.getNciAccount().getDOC();
    	HpcUserRole updateRole = updateUserRequestDTO.getUserRole() != null ?
    		                     roleFromString(updateUserRequestDTO.getUserRole()) : 
    		                     dataManagementService.getUserRole(nciUserId);
        // GROUP_ADMIN not supported by current Jargon API version. Respond with a workaround.
  	    if(updateRole == HpcUserRole.GROUP_ADMIN) {
  		   throw new HpcException("GROUP_ADMIN currently not supported by the API. " +
  	                              "Run 'iadmin moduser' command to change the user's role to GROUP_ADMIN",
  				                  HpcRequestRejectReason.API_NOT_SUPPORTED);
  	    }
    		                     
     	dataManagementService.updateUser(nciUserId, updateFirstName,
     			                         updateLastName, updateRole);
    	
	     // Update User.
	     userService.updateUser(nciUserId, updateFirstName, 
	    		                updateLastName, updateDOC,
	    		                updateDataTransferAccount);
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
    	userDTO.setUserRole(dataManagementService.getUserRole(nciUserId).value());
    	
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
    	userService.setRequestInvoker(user, userAuthenticated);
    	
    	// Prepare and return a response DTO.
    	HpcAuthenticationResponseDTO authenticationResponse = new HpcAuthenticationResponseDTO();
    	authenticationResponse.setAuthenticated(ldapAuthentication ? userAuthenticated : true);	                
    	authenticationResponse.setUserRole(
    			      user.getDataManagementAccount() != null ? 
    			      dataManagementService.getUserRole(user.getDataManagementAccount().getUsername()) : 
    			      HpcUserRole.NOT_REGISTERED);
    	
    	return authenticationResponse;
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Mask account passwords.
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
    
    /**
     * Convert a user role from string to enum.
     * 
     * @param roleStr The role string.
     * @return The enum value.
     * @throws HpcException If the enum value is invalid
     */
    private HpcUserRole roleFromString(String roleStr) throws HpcException
    {
    	try {
    	     return HpcUserRole.fromValue(roleStr);
    	     
    	} catch(IllegalArgumentException e) {
    		    throw new HpcException("Invalid user role: " + roleStr + 
    		    		               ". Valid values: " +  Arrays.asList(HpcUserRole.values()),
    		    		               HpcErrorType.INVALID_REQUEST_INPUT, e);
    	}
    }
}

 