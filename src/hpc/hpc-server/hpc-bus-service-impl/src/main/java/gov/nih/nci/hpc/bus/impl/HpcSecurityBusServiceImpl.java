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
import gov.nih.nci.hpc.domain.model.HpcDataManagementAccount;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcGroup;
import gov.nih.nci.hpc.dto.security.HpcGroupListDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMemberResponse;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcSystemAccountDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.dto.security.HpcUserListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementSecurityService;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcSecurityService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Security Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
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
    private HpcDataManagementSecurityService dataManagementSecurityService = null;
	
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
     */
    private HpcSecurityBusServiceImpl() 
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcUserBusService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void registerUser(String nciUserId, HpcUserRequestDTO userRegistrationRequest)  
    		                throws HpcException
    {
    	// Input validation.
    	if(StringUtils.isEmpty(nciUserId) || userRegistrationRequest == null) {
    	   throw new HpcException("Null NCI user ID or user registation request",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Instantiate an NCI account domain object.
 	    HpcNciAccount nciAccount = new HpcNciAccount();
 	    nciAccount.setUserId(nciUserId);
 	    nciAccount.setFirstName(userRegistrationRequest.getFirstName());
 	    nciAccount.setLastName(userRegistrationRequest.getLastName());
 	    nciAccount.setDoc(userRegistrationRequest.getDoc());
    	
    	// HPC-DM is integrated with a data management system (IRODS). When registering a user with HPC-DM, 
 	    // this service (by default) creates an account for the user with the data management system, unless
 	    // asked otherwise by the caller.
    	if(userRegistrationRequest.getCreateDataManagementAccount() == null ||
    	   userRegistrationRequest.getCreateDataManagementAccount()) {
    	   // Determine the user role to create. If not provided, default to USER.
    	   HpcUserRole role = userRegistrationRequest.getUserRole() != null ?
    			              roleFromString(userRegistrationRequest.getUserRole()) : 
    			              HpcUserRole.USER;
    			              
           // GROUP_ADMIN not supported by current Jargon API version. Respond with a workaround.
    	   if(role == HpcUserRole.GROUP_ADMIN) {
    		  throw new HpcException("GROUP_ADMIN currently not supported by the API. " +
    	                             "Create the account with a USER role, and then run " +
    				                 "'iadmin moduser' command to change the user's role to GROUP_ADMIN",
    				                 HpcRequestRejectReason.API_NOT_SUPPORTED);
    	   }
    			           
    	   // Create the data management account.
    	   dataManagementSecurityService.addUser(nciAccount, role);
    	}
    	
    	boolean registrationCompleted = false;
    	try {
    	     // Add the user to the system.
    	     securityService.addUser(nciAccount);
    	     registrationCompleted = true;
    	     
    	} finally {
    		       if(!registrationCompleted) {
    		    	  // Registration failed. Remove the data management account.
    		    	  dataManagementSecurityService.deleteUser(nciUserId);
    		       }
    	}
    }
    
    @Override
    public void updateUser(String nciUserId, HpcUserRequestDTO userUpdateRequest) 
                          throws HpcException
    {
    	// Input validation.
    	validateUserUpdateRequest(nciUserId, userUpdateRequest);
    	
    	// Get the user.
    	HpcUser user = securityService.getUser(nciUserId);
    	if(user == null) {
    	   throw new HpcException("User not found: " + nciUserId, 
    			                  HpcRequestRejectReason.INVALID_NCI_ACCOUNT);	
    	}
    	
    	// Get the current user role.
    	HpcUserRole currentUserRole = dataManagementSecurityService.getUserRole(nciUserId);
    	
    	// Determine update values.
    	String updateFirstName = !StringUtils.isEmpty(userUpdateRequest.getFirstName()) ?
    			                 userUpdateRequest.getFirstName() : user.getNciAccount().getFirstName();
        String updateLastName = !StringUtils.isEmpty(userUpdateRequest.getLastName()) ?
        		                userUpdateRequest.getLastName() : user.getNciAccount().getLastName();
    	String updateDOC = !StringUtils.isEmpty(userUpdateRequest.getDoc()) ?
    			           userUpdateRequest.getDoc() : user.getNciAccount().getDoc();
    	HpcUserRole updateRole = !StringUtils.isEmpty(userUpdateRequest.getUserRole()) ?
    		                     roleFromString(userUpdateRequest.getUserRole()) : currentUserRole;
        // GROUP_ADMIN not supported by current Jargon API version. Respond with a workaround.
  	    if(updateRole == HpcUserRole.GROUP_ADMIN) {
  		   throw new HpcException("GROUP_ADMIN currently not supported by the API. " +
  	                              "Run 'iadmin moduser' command to change the user's role to GROUP_ADMIN",
  				                  HpcRequestRejectReason.API_NOT_SUPPORTED);
  	    }
    		    
  	    // Update the data management (IRODS) account.
  	    dataManagementSecurityService.updateUser(nciUserId, updateFirstName,
     	     		                             updateLastName, updateRole);
    	
	    // Update User.
	    securityService.updateUser(nciUserId, updateFirstName, 
	    		                   updateLastName, updateDOC);
    }

    
    @Override
    public HpcUserDTO getUser(String nciUserId) throws HpcException
    {
    	// Input validation.
    	if(nciUserId == null) {
    	   throw new HpcException("Null NCI User ID",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Authorize calling this service w/ 'nciUserId'.
    	securityService.authorizeUserService(nciUserId);
    	
    	// Get the managed data domain object.
    	HpcUser user = securityService.getUser(nciUserId);
    	if(user == null) {
    	   return null;
    	}
    	
    	// Map it to the DTO.
    	HpcUserDTO userDTO = new HpcUserDTO();
    	userDTO.setFirstName(user.getNciAccount().getFirstName());
    	userDTO.setLastName(user.getNciAccount().getLastName());
    	userDTO.setDoc(user.getNciAccount().getDoc());
    	userDTO.setUserRole(dataManagementSecurityService.getUserRole(nciUserId).value());
    	
    	return userDTO;
    }
    
    public HpcUserListDTO getUsers(String nciUserId, String firstName, String lastName) 
                                  throws HpcException
    {
    	// Get the users based on search criteria.
    	HpcUserListDTO users = new HpcUserListDTO();
    	for(HpcUser user : securityService.getUsers(nciUserId, firstName, lastName)) {
    	    users.getUsers().add(user.getNciAccount());
    	}
    	
    	return users;
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
//    	// Generate an authentication token.
//        HpcAuthenticationTokenClaims authenticationTokenClaims = new HpcAuthenticationTokenClaims();
//        authenticationTokenClaims.setUserName(userName);
//        authenticationTokenClaims.setPassword(password);
//        authenticationTokenClaims.setUserAuthenticated(userAuthenticated);
//        authenticationTokenClaims.setLdapAuthentication(ldapAuthentication);
//    	String authenticatioToken = securityService.createAuthenticationToken(authenticationTokenClaims);
    	
        // Set the request invoker.
    	return setRequestInvoker(userName, password, userAuthenticated, ldapAuthentication, null);    
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
    			                 authenticationTokenClaims.getDataManagementAccount()); 	
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
	public HpcGroupMembersResponseDTO registerGroup(String groupName,
                                                    HpcGroupMembersRequestDTO groupMembersRequest) 
                                                   throws HpcException
    {
		// Input validation.
		if(groupName == null) {
		   throw new HpcException("Null group name", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if(groupMembersRequest != null && !groupMembersRequest.getDeleteUserIds().isEmpty()) {
		   throw new HpcException("Delete users is invalid in group registration request", 
				                  HpcErrorType.INVALID_REQUEST_INPUT);	
		}
		
    	// Validate the group doesn't already exists.
    	if(dataManagementSecurityService.groupExists(groupName)) {
    	   throw new HpcException("Group already exists: " + groupName, 
    	                          HpcRequestRejectReason.GROUP_ALREADY_EXISTS);
    	}
		
		// Add the group.
		dataManagementSecurityService.addGroup(groupName);
		
		// Optionally add members.
		return updateGroupMembers(groupName, groupMembersRequest);
    }
	
	public HpcGroupMembersResponseDTO updateGroup(String groupName,
                                                  HpcGroupMembersRequestDTO groupMembersRequest)
                                                 throws HpcException
    {
		// Input validation.
		if(groupName == null) {
		   throw new HpcException("Null group name", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if(groupMembersRequest == null || 
		   (groupMembersRequest.getDeleteUserIds().isEmpty() && groupMembersRequest.getAddUserIds().isEmpty())) {
		   throw new HpcException("Null or empty requests to add/delete members to group", 
				                  HpcErrorType.INVALID_REQUEST_INPUT);	
		}
		
    	// Validate the group exists.
    	if(!dataManagementSecurityService.groupExists(groupName)) {
    	   throw new HpcException("Group doesn't exist", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
		
		// Optionally add members.
		return updateGroupMembers(groupName, groupMembersRequest);		
    }

	@Override
	public HpcGroupMembersDTO getGroup(String groupName) throws HpcException
	{
		// Input validation.
		if(groupName == null) {
		   throw new HpcException("Null group name", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
    	// Validate the group exists.
    	if(!dataManagementSecurityService.groupExists(groupName)) {
    	   return null;
    	}
		
		// Return the group members.
    	HpcGroupMembersDTO groupMembers = new HpcGroupMembersDTO();
    	List<String> userIds = dataManagementSecurityService.getGroupMembers(groupName);
    	if(userIds != null) {
    	   groupMembers.getUserIds().addAll(userIds);
    	}
    	
    	return groupMembers;
	}
	
	@Override
	public HpcGroupListDTO getGroups(String groupSearchCriteria) throws HpcException
	{
		// Search for groups.
    	List<String> groupNames = dataManagementSecurityService.getGroups(
    			                      groupSearchCriteria != null ? groupSearchCriteria : "%");
    	if(groupNames == null || groupNames.isEmpty()) {
    	   return null;
    	}
    	
    	// Construct the DTO to return.
    	HpcGroupListDTO groups = new HpcGroupListDTO();
    	for(String groupName : groupNames) {
    		HpcGroup group = new HpcGroup();
    		group.setGroupName(groupName);
    		
    		// Get members of this groups. 
    		List<String> userIds = dataManagementSecurityService.getGroupMembers(groupName);
        	if(userIds != null) {
        	   group.getUserIds().addAll(userIds);
        	}
        	
    		groups.getGroups().add(group);
    	}
    	
    	return groups;		
	}
	
	@Override
	public void deleteGroup(String groupName) throws HpcException
	{
		// Input validation.
		if(groupName == null) {
		   throw new HpcException("Null group name", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
    	// Delete the group.
    	if(dataManagementSecurityService.groupExists(groupName)) {
    	   dataManagementSecurityService.deleteGroup(groupName);
    	}
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Convert a user role from string to enum.
     * 
     * @param roleStr The role string.
     * @return The enum value.
     * @throws HpcException If the enum value is invalid.
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
     * @param userName The user name.
     * @param password The password.
     * @param userAuthenticated User authenticated indicator.
     * @param ldapAuthentication LDAP authenticated user indicator.
     * @param dmAccount The data management account.
     * @return The Authentication Response DTO.
     * @throws HpcException on service failure.
     */
    private HpcAuthenticationResponseDTO setRequestInvoker(
    		   String userName, String password,
    		   boolean userAuthenticated,
    		   boolean ldapAuthentication,
    		   HpcDataManagementAccount dmAccount) throws HpcException
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

		HpcRequestInvoker requestInvoker = securityService.getRequestInvoker();
		if(requestInvoker.getDataManagementAuthenticatedToken() == null && dmAccount != null)
			requestInvoker.setDataManagementAuthenticatedToken(dataManagementSecurityService.getProxyManagementAccount(dmAccount));
		
		authenticationResponse.setUserRole(
				      authenticationResponse.getAuthenticated() && user.getDataManagementAccount() != null ? 
				    		  dataManagementSecurityService.getUserRole(user.getDataManagementAccount().getUsername()) : 
				      HpcUserRole.NOT_REGISTERED);
		
    	// Generate an authentication token.
        HpcAuthenticationTokenClaims authenticationTokenClaims = new HpcAuthenticationTokenClaims();
        authenticationTokenClaims.setUserName(userName);
        authenticationTokenClaims.setPassword(password);
        authenticationTokenClaims.setUserAuthenticated(userAuthenticated);
        authenticationTokenClaims.setLdapAuthentication(ldapAuthentication);
        if(dmAccount == null)
        	dmAccount = dataManagementSecurityService.getHpcDataManagementAccount(requestInvoker.getDataManagementAuthenticatedToken());
        authenticationTokenClaims.setDataManagementAccount(dmAccount);
        String authenticationToken = securityService.createAuthenticationToken(authenticationTokenClaims);

    	authenticationResponse.setToken(authenticationToken);
		
    	// Update the request invoker instance.
		
		requestInvoker.setAuthenticationToken(authenticationToken);
		requestInvoker.setUserRole(authenticationResponse.getUserRole());
		if(requestInvoker.getDataManagementAuthenticatedToken() == null)
			requestInvoker.setDataManagementAuthenticatedToken(dataManagementSecurityService.getProxyManagementAccount(dmAccount));
		
		return authenticationResponse;
    }  
    
    /**
     * Update group members of a group.
     * 
     * @param groupName The group name.
     * @param groupMembersRequest A list of users to add and delete from the group.
     * @return A DTO containing the results of each add/delete member request.
     * @throws HpcException on service failure.
     */
    private HpcGroupMembersResponseDTO updateGroupMembers(String groupName,
                                                          HpcGroupMembersRequestDTO groupMembersRequest) 
                                                         throws HpcException
    {
    	if(groupMembersRequest == null) {
    	   return null;	
    	}
    	
    	HpcGroupMembersResponseDTO groupMembersResponses = new HpcGroupMembersResponseDTO();
    	
    	// Remove duplicates from the add/delete user-ids lists. 
    	Set<String> addUserIds = new HashSet<>();
    	addUserIds.addAll(groupMembersRequest.getAddUserIds());
    	Set<String> deleteUserIds = new HashSet<>();
    	deleteUserIds.addAll(groupMembersRequest.getDeleteUserIds());
    	
    	// Validate a user-id is not in both add and delete lists.
    	Set<String> userIds = new HashSet<>();
    	userIds.addAll(addUserIds);
    	userIds.retainAll(deleteUserIds);
    	if(!userIds.isEmpty()) {
    	   throw new HpcException("User Id(s) found in both add and delete lists: " + userIds,
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	// Add group members.
    	for(String userId : addUserIds) {
    		HpcGroupMemberResponse addGroupMemberResponse = new HpcGroupMemberResponse();
    		addGroupMemberResponse.setUserId(userId);
    		addGroupMemberResponse.setResult(true);
    		try {
		    	  dataManagementSecurityService.addGroupMember(groupName, userId);
			     
		    } catch(HpcException e) {
		    	    // Request failed. Record the message and keep going.
		    	    addGroupMemberResponse.setResult(false);
		    	    addGroupMemberResponse.setMessage(e.getMessage());
		    }
    		
    		// Add this user add group member response to the list.
    		groupMembersResponses.getAddGroupMemberResponses().add(addGroupMemberResponse);
    	}
    	
    	// Delete group members.
    	for(String userId : deleteUserIds) {
    		HpcGroupMemberResponse deleteGroupMemberResponse = new HpcGroupMemberResponse();
    		deleteGroupMemberResponse.setUserId(userId);
    		deleteGroupMemberResponse.setResult(true);
    		try {
		    	  dataManagementSecurityService.deleteGroupMember(groupName, userId);
			     
		    } catch(HpcException e) {
		    	    // Request failed. Record the message and keep going.
		    	    deleteGroupMemberResponse.setResult(false);
		         	deleteGroupMemberResponse.setMessage(e.getMessage());
		    }
    		
    		// Add this user add group member response to the list.
    		groupMembersResponses.getDeleteGroupMemberResponses().add(deleteGroupMemberResponse);
    	}
    	
    	return groupMembersResponses;
    }
    
    /**
     * Validate a user update request
     * 
     * @param nciUserId The NCI user ID to be updated
     * @param userUpdateRequest The user update request.
     * @throws HpcException If the user update request is invalid.
     */
    private void validateUserUpdateRequest(String nciUserId, HpcUserRequestDTO userUpdateRequest) 
                                          throws HpcException
    {                                                    
		// Input validation.
		if(StringUtils.isEmpty(nciUserId) || userUpdateRequest == null) {
		   throw new HpcException("Null NCI user ID or user update request",
				                  HpcErrorType.INVALID_REQUEST_INPUT);	
		}
		
		if(StringUtils.isEmpty(userUpdateRequest.getFirstName()) &&
		   StringUtils.isEmpty(userUpdateRequest.getLastName()) &&
		   StringUtils.isEmpty(userUpdateRequest.getDoc()) &&
		   StringUtils.isEmpty(userUpdateRequest.getUserRole())) {
	      throw new HpcException("Invalid update user request. Please provide firstName, lastName, doc or userRole to update.",
	 			                 HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
	    // Validate an administrator is not downgrading self role.
		HpcRequestInvoker invoker = securityService.getRequestInvoker();
		if(invoker == null) {
		   throw new HpcException("Null request invoker", HpcErrorType.UNEXPECTED_ERROR);
		}
		if(invoker.getUserRole().equals(HpcUserRole.SYSTEM_ADMIN) &&
		   nciUserId.equals(invoker.getNciAccount().getUserId()) && 
		   userUpdateRequest.getUserRole() != null && 
		   !userUpdateRequest.getUserRole().equals(HpcUserRole.SYSTEM_ADMIN.value())) {
	       throw new HpcException("Not authorizated to downgrade self role. Please contact system administrator",
	     	                      HpcRequestRejectReason.NOT_AUTHORIZED);
					
		}
    }
}

 
