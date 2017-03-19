/**
 * HpcSecurityBusService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupListDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcSystemAccountDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.dto.security.HpcUserListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Security Business Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id: HpcSecurityBusService.java 1013 2016-03-26 23:06:30Z rosenbergea $
 */

public interface HpcSecurityBusService 
{         
    /**
     * Register a User. 
     *
     * @param nciUserId The user ID to register.
     * @param userRegistrationRequest The user registration request DTO.
     * @throws HpcException on service failure.
     */
    public void registerUser(String nciUserId,
    		                 HpcUserRequestDTO userRegistrationRequest) 
    		                throws HpcException;
    
    /**
     * Update a User.
     *
     * @param nciUserId The user ID to update.
     * @param userUpdateRequest The user update request DTO.
     * @throws HpcException on service failure.
     */
    public void updateUser(String nciUserId, 
    		               HpcUserRequestDTO userUpdateRequest)  
    		              throws HpcException;

    /**
     * Get a user by its NCI user id.
     *
     * @param nciUserId The user's NCI user id.
     * @return The registered user DTO or null if not found.
     * @throws HpcException on service failure.
     */
    public HpcUserDTO getUser(String nciUserId) throws HpcException;
    
    /**
     * Get users by search criterias.
     *
     * @param nciUserId (Optional) The user ID to search for.
     * @param firstName (Optional) The first name to search for.
     * @param lastName (Optional) The last name to search for.
     * @return A list of users.
     * @throws HpcException on service failure.
     */
    public HpcUserListDTO getUsers(String nciUserId, String firstName, String lastName) 
    		                      throws HpcException;
    
    /**
     * Authenticate user.
     *
     * @param userName The user's name.
     * @param password The user's password.
     * @param ldapAuthentication Perform LDAP authentication indicator.
     * @return Authentication Response DTO.
     * @throws HpcException on service failure.
     */
    public HpcAuthenticationResponseDTO 
           authenticate(String userName, String password, 
        		        boolean ldapAuthentication) throws HpcException;  
    
    /**
     * Authenticate user.
     *
     * @param authenticationToken An Authentication token.
     * @return HpcAuthenticationResponseDTO.
     * @throws HpcException on service failure.
     */
    public HpcAuthenticationResponseDTO authenticate(String authenticationToken) 
    		                                         throws HpcException;  
    
    /**
     * Get the authentication response for the current request invoker.
     *
     * @return HpcAuthenticationResponseDTO.
     * @throws HpcException on service failure.
     */
    public HpcAuthenticationResponseDTO getAuthenticationResponse() throws HpcException; 
    
    /**
     * Register a group
     *
     * @param groupName The group name.
     * @param groupMembersRequest (Optional) request to add users to the registered group.
     * @return A list of responses to the add members requests.
     * @throws HpcException on service failure.
     */
	public HpcGroupMembersResponseDTO registerGroup(String groupName,
			                                        HpcGroupMembersRequestDTO groupMembersRequest) 
			                                       throws HpcException;
	
    /**
     * Group update.
     *
     * @param groupName The group name.
     * @param groupMembersRequest Request to add/remove users to/from a group.
     * @return A list of responses to the add/delete members requests.
     * @throws HpcException on service failure.
     */
	public HpcGroupMembersResponseDTO updateGroup(String groupName,
			                                      HpcGroupMembersRequestDTO groupMembersRequest)
			                                     throws HpcException;

	/**
     * Get a group by name.
     *
     * @param groupName The group name.
     * @return A list of group members if the group exists, otherwise null.
     * @throws HpcException on service failure.
     */
    public HpcGroupMembersDTO getGroup(String groupName) throws HpcException;
    
    /**
     * Get groups by search criteria.
     *
     * @param groupSearchCriteria (Optional) The group search criteria (In the form of SQL 'LIKE').
     *                            If not provided, then all groups are returned.
     * @return A list of groups and their members.
     * @throws HpcException on service failure.
     */
    public HpcGroupListDTO getGroups(String groupSearchCriteria) throws HpcException;
    
    /**
     * Delete a group.
     *
     * @param groupName The group name.
     */
    public void deleteGroup(String groupName) throws HpcException;
	
    /**
     * Register a System Account.
     *
     * @param systemAccountRegistrationDTO The system account registration DTO.
     * @throws HpcException on service failure.
     */
    public void registerSystemAccount(HpcSystemAccountDTO systemAccountRegistrationDTO) 
    		                         throws HpcException;
}

 