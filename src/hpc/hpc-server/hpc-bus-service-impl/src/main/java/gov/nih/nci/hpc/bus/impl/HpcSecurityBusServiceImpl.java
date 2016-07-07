/**
 * HpcSecurityBusServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.bus.HpcSecurityBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcAuthenticationTokenClaims;
import gov.nih.nci.hpc.domain.model.HpcGroup;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcGroupResponse;
import gov.nih.nci.hpc.domain.user.HpcGroupUserResponse;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcSystemAccountDTO;
import gov.nih.nci.hpc.dto.security.HpcUpdateUserRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.dto.security.HpcUserGroupResponseDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcSecurityService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC User Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id: HpcSecurityBusServiceImpl.java 1064 2016-04-14 19:43:41Z konkapv $
 */

public class HpcSecurityBusServiceImpl implements HpcSecurityBusService
{      
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // Application service instances.
	
	@Autowired
    private HpcSecurityService securityService = null;
	
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
    private HpcSecurityBusServiceImpl() throws HpcException
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
    	logger.info("Invoking registerUser(HpcUserDTO): " + 
                    userRegistrationDTO);
    	
    	// Input validation.
    	if(userRegistrationDTO == null) {
    	   throw new HpcException("Null HpcUserRegistrationDTO",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
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
    	     securityService.addUser(userRegistrationDTO.getNciAccount(), 
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
    	HpcUser user = securityService.getUser(nciUserId);
    	if(user == null) {
    	   throw new HpcException("User not found: " + nciUserId, 
    			                  HpcRequestRejectReason.INVALID_NCI_ACCOUNT);	
    	}
    	
    	HpcUserRole requestUserRole = dataManagementService.getUserRole(nciUserId);
    	
    	if(requestUserRole.equals(HpcUserRole.SYSTEM_ADMIN))
    	{
    		if(updateUserRequestDTO.getUserRole() != null && !updateUserRequestDTO.getUserRole().equals(HpcUserRole.SYSTEM_ADMIN.value()))
           			throw new HpcException("Not authorizated to downgrade self role. Please contact system administrator",
         	                  HpcRequestRejectReason.NOT_AUTHORIZED);
    				
    	}
    	else
    	{
    		if(updateUserRequestDTO.getFirstName() != null ||
       			 updateUserRequestDTO.getLastName() != null ||
       			 updateUserRequestDTO.getDOC() != null ||
       			 updateUserRequestDTO.getUserRole() != null)
       		{
       			throw new HpcException("Not authorizated to update frist name, last name, DOC, role. Please contact system administrator",
   	                  HpcRequestRejectReason.NOT_AUTHORIZED);
       		}

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
    		                     requestUserRole;
        // GROUP_ADMIN not supported by current Jargon API version. Respond with a workaround.
  	    if(updateRole == HpcUserRole.GROUP_ADMIN) {
  		   throw new HpcException("GROUP_ADMIN currently not supported by the API. " +
  	                              "Run 'iadmin moduser' command to change the user's role to GROUP_ADMIN",
  				                  HpcRequestRejectReason.API_NOT_SUPPORTED);
  	    }
    		                     
     	dataManagementService.updateUser(nciUserId, updateFirstName,
     			                         updateLastName, updateRole);
    	
	     // Update User.
	     securityService.updateUser(nciUserId, updateFirstName, 
	    		                updateLastName, updateDOC);
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
    	HpcUser user = securityService.getUser(nciUserId);
    	if(user == null) {
    	   return null;
    	}
    	
    	// Map it to the DTO.
    	HpcUserDTO userDTO = new HpcUserDTO();
    	userDTO.setNciAccount(user.getNciAccount());
    	userDTO.setDataManagementAccount(user.getDataManagementAccount());
    	userDTO.setUserRole(dataManagementService.getUserRole(nciUserId).value());
    	
    	// Mask passwords.
    	maskPasswords(userDTO);
    	
    	return userDTO;
    }
    
    @Override
    public HpcAuthenticationResponseDTO 
           authenticate(String userName, String password, 
    		            boolean ldapAuthentication) 
                       throws HpcException
    {
    	// LDAP authentication.
    	boolean userAuthenticated =
    			ldapAuthentication ? 
    			    securityService.authenticate(userName, password) : false;
    			    		
    	// Generate an authentication token.
        HpcAuthenticationTokenClaims authenticationTokenClaims = new HpcAuthenticationTokenClaims();
        authenticationTokenClaims.setUserName(userName);
        authenticationTokenClaims.setPassword(password);
        authenticationTokenClaims.setUserAuthenticated(userAuthenticated);
        authenticationTokenClaims.setLdapAuthentication(ldapAuthentication);
    	String authenticatioToken = securityService.createAuthenticationToken(authenticationTokenClaims);
    	
        // Set the request invoker.
    	return setRequestInvoker(userName, password, userAuthenticated, ldapAuthentication, 
    			                 authenticatioToken);   
    }
    
    public HpcAuthenticationResponseDTO authenticate(String authenticationToken) 
    		                                        throws HpcException
    {
    	HpcAuthenticationTokenClaims authenticationTokenClaims = 
    			                     securityService.parseAuthenticationToken(authenticationToken);
    	if(authenticationTokenClaims == null) {
    	   throw new HpcException("Invalid or Expired Authentication token", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
        // Set the request invoker.
    	return setRequestInvoker(authenticationTokenClaims.getUserName(), 
    			                 authenticationTokenClaims.getPassword(), 
    			                 authenticationTokenClaims.getUserAuthenticated(), 
    			                 authenticationTokenClaims.getLdapAuthentication(), 
    			                 authenticationToken); 	
    }
    
    @Override
    public HpcAuthenticationResponseDTO getAuthenticationResponse() throws HpcException
    {
    	HpcAuthenticationResponseDTO authenticationResponse = new HpcAuthenticationResponseDTO();
    	HpcRequestInvoker requestInvoker = securityService.getRequestInvoker();
    	authenticationResponse.setAuthenticated(requestInvoker.getLdapAuthenticated());
    	authenticationResponse.setToken(requestInvoker.getAuthenticationToken());
    	authenticationResponse.setUserRole(requestInvoker.getUserRole());
    	
    	return authenticationResponse;
    }
    
    @Override
    public void registerSystemAccount(HpcSystemAccountDTO systemAccountRegistrationDTO)  
    		                         throws HpcException
    {
    	logger.info("Invoking registerSystemAccount(HpcSystemAccountDTO)");
    	
    	// Input validation.
    	if(systemAccountRegistrationDTO == null) {
    	   throw new HpcException("Null HpcSystemAccountDTO",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}

    	// Add the user to the managed collection.
    	securityService.addSystemAccount(systemAccountRegistrationDTO.getAccount(), 
    			                         systemAccountRegistrationDTO.getDataTransferType());
    }
    
	@Override
	public HpcGroupResponseDTO setGroup(HpcGroupRequestDTO groupRequest) throws HpcException {
		if (groupRequest == null || groupRequest.getGroup() == null)
			throw new HpcException("Null Group request", HpcErrorType.INVALID_REQUEST_INPUT);
		HpcGroup hpcGroup = new HpcGroup();
		hpcGroup.setGroupName(groupRequest.getGroup());

		HpcGroupResponseDTO dto = new HpcGroupResponseDTO();

		try {
			HpcGroupResponse response = dataManagementService.setGroup(hpcGroup, groupRequest.getAddUserIds(),
					groupRequest.getDeleteUserIds());
			dto.setGroup(groupRequest.getGroup());
			dto.setResult(response.getResult());
			dto.setMessage(response.getMessage());
			if (response.getGroupuser() != null && response.getGroupuser().size() > 0) {
				List<HpcUserGroupResponseDTO> userGroupResponse = new ArrayList<HpcUserGroupResponseDTO>();
				for (HpcGroupUserResponse gResponse : response.getGroupuser()) {
					HpcUserGroupResponseDTO userGroupResponseDTO = new HpcUserGroupResponseDTO();
					userGroupResponseDTO.setUserId(gResponse.getUserId());
					userGroupResponseDTO.setResult(gResponse.getResult());
					userGroupResponseDTO.setMessage(gResponse.getMessage());
					userGroupResponse.add(userGroupResponseDTO);
				}
				dto.getUserGroupResponses().addAll(userGroupResponse);
			}
		} catch (HpcException e) {
			dto.setResult(false);
			dto.setMessage("Group is not created due to: " + e.getMessage());
			throw e;
		}
		return dto;
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
    
    /**
     * Set the Request invoker and return an authentication request DTO
     * 
     * @param authenticationRequest The authentication request.
     * @param userAuthenticated User authenticated indicator.
     * @param ldapAuthentication LDAP authenticated user indicator.
     * @param authenticationToken The authentication token.
     * @return The HpcAuthenticationResponseDTO.
     * @throws HpcException 
     */
    private HpcAuthenticationResponseDTO setRequestInvoker(
    		   String userName, String password,
    		   boolean userAuthenticated,
    		   boolean ldapAuthentication, 
    		   String authenticationToken) throws HpcException
    {
		// Get the HPC user.
		HpcUser user = null;
		try {
		     user = securityService.getUser(userName);
			
		} catch(HpcException e) {
			    logger.error("Failed to get user: " +  userName);
		}
		
		if(user == null) {
		   // This is a request from a user that is not registered with HPC.
		   logger.info("Service call for a user that is not registered with HPC. NCI User-id: " + 
		               userName);
		
		   user = new HpcUser();
		   HpcNciAccount nciAccount = new HpcNciAccount();
		   nciAccount.setUserId("Unknown-NCI-User-ID");
	 	   user.setNciAccount(nciAccount);
	 	   user.setDataManagementAccount(null);
	    }
		
		// If the user was authenticated w/ LDAP, then we use the NCI credentials to access
		// Data Management (iRODS).
		if(userAuthenticated) {
		   HpcIntegratedSystemAccount dataManagementAccount = new HpcIntegratedSystemAccount();
		   dataManagementAccount.setIntegratedSystem(HpcIntegratedSystem.IRODS);
		   dataManagementAccount.setUsername(userName);
		   dataManagementAccount.setPassword(password);
		   user.setDataManagementAccount(dataManagementAccount);
		}
		
		// Populate the request invoker context with the HPC user data.
		securityService.setRequestInvoker(user, userAuthenticated);
		
		// Prepare and return a response DTO.
		HpcAuthenticationResponseDTO authenticationResponse = new HpcAuthenticationResponseDTO();
		authenticationResponse.setAuthenticated(ldapAuthentication ? userAuthenticated : true);	
		authenticationResponse.setToken(authenticationToken);
		authenticationResponse.setUserRole(
				      user.getDataManagementAccount() != null ? 
				      dataManagementService.getUserRole(user.getDataManagementAccount().getUsername()) : 
				      HpcUserRole.NOT_REGISTERED);
    	
    	// Update the request invoker instance.
		HpcRequestInvoker requestInvoker = securityService.getRequestInvoker();
		requestInvoker.setAuthenticationToken(authenticationToken);
		requestInvoker.setUserRole(authenticationResponse.getUserRole());
		
		return authenticationResponse;
    }    
}

 