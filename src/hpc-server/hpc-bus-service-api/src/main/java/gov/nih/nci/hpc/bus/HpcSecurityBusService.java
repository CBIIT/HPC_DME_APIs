/**
 * HpcSecurityBusService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
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
 * HPC Security Business Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcSecurityBusService {
  /**
   * Register a User.
   *
   * @param nciUserId The user ID to register.
   * @param userRegistrationRequest The user registration request DTO.
   * @throws HpcException on service failure.
   */
  public void registerUser(String nciUserId, HpcUserRequestDTO userRegistrationRequest)
      throws HpcException;

  /**
   * Update a User.
   *
   * @param nciUserId The user ID to update.
   * @param userUpdateRequest The user update request DTO. Note: only fields that are provided in
   *     the DTO are updated. If 'defaultBasePath' is provided as empty string, then it is removed.
   * @throws HpcException on service failure.
   */
  public void updateUser(String nciUserId, HpcUserRequestDTO userUpdateRequest) throws HpcException;

  /**
   * Get a user by its NCI user id.
   *
   * @param nciUserId (Optional) The user's NCI user id. If null, then the invoker user is returned.
   * @return The registered user DTO or null if not found.
   * @throws HpcException on service failure.
   */
  public HpcUserDTO getUser(String nciUserId) throws HpcException;

  /**
   * Get users by search criterias. Note: only active users are returned.
   *
   * @param nciUserId (Optional) The user ID to search for (using case insensitive comparison).
   * @param firstNamePattern (Optional) The first-name pattern to search for (using case insensitive
   *     matching). SQL LIKE wildcards ('%', '_') are supported.
   * @param lastNamePattern (Optional) The last-name pattern to search for (using case insensitive
   *     matching). SQL LIKE wildcards ('%', '_') are supported.
   * @param doc (Optional) The DOC to search for.
   * @param defaultBasePath (Optional) The default base path to search for.
   * @param active If set to true, only active users are searched. Otherwise, all users (active and
   *     inactive) are searched.
   * @return A list of users.
   * @throws HpcException on service failure.
   */
  public HpcUserListDTO getUsers(
      String nciUserId,
      String firstNamePattern,
      String lastNamePattern,
      String doc,
      String defaultBasePath,
      boolean active)
      throws HpcException;

  /**
   * Authenticate user.
   *
   * @param nciUserId The user's ID.
   * @param password The user's password.
   * @throws HpcException If user authentication failed.
   */
  public void authenticate(String nciUserId, String password) throws HpcException;

  /**
   * Authenticate user.
   *
   * @param authenticationToken An Authentication token.
   * @throws HpcException If user authentication failed.
   */
  public void authenticate(String authenticationToken) throws HpcException;

  /**
   * Authenticate SSO smSession.
   *
   * @param nciUserId The user's ID.
   * @param smSession The NIHSMSESSION.
   * @throws HpcException If user authentication failed.
   */
  public void authenticateSso(String nciUserId, String smSession) throws HpcException;

  /**
   * Get the authentication response for the current request invoker.
   *
   * @param generateToken If set to true, an authentication token will get generated and included in
   *     response DTO.
   * @return HpcAuthenticationResponseDTO.
   * @throws HpcException on service failure.
   */
  public HpcAuthenticationResponseDTO getAuthenticationResponse(boolean generateToken)
      throws HpcException;

  /**
   * Register a group
   *
   * @param groupName The group name.
   * @param groupMembersRequest (Optional) request to add users to the registered group.
   * @return A list of responses to the add members requests.
   * @throws HpcException on service failure.
   */
  public HpcGroupMembersResponseDTO registerGroup(
      String groupName, HpcGroupMembersRequestDTO groupMembersRequest) throws HpcException;

  /**
   * Group update.
   *
   * @param groupName The group name.
   * @param groupMembersRequest Request to add/remove users to/from a group.
   * @return A list of responses to the add/delete members requests.
   * @throws HpcException on service failure.
   */
  public HpcGroupMembersResponseDTO updateGroup(
      String groupName, HpcGroupMembersRequestDTO groupMembersRequest) throws HpcException;

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
   * @param groupPattern (Optional) The group pattern to search for (using case insensitive
   *     matching). SQL LIKE wildcards ('%', '_') are supported. If not provided, then all groups
   *     are returned.
   * @return A list of groups and their members.
   * @throws HpcException on service failure.
   */
  public HpcGroupListDTO getGroups(String groupPattern) throws HpcException;

  /**
   * Delete a group.
   *
   * @param groupName The group name.
   * @throws HpcException on service failure.
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
  
  
  /**
   * Refresh data management configurations.
   *
   * @throws HpcException on service failure.
   */
  public void refreshDataManagementConfigurations()
      throws HpcException;
}
