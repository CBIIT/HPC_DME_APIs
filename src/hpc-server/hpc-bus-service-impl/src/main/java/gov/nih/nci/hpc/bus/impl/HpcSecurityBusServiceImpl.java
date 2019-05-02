/**
 * HpcSecurityBusServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import gov.nih.nci.hpc.bus.HpcSecurityBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcAuthenticationTokenClaims;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcAuthenticationType;
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
import gov.nih.nci.hpc.dto.security.HpcUserListEntry;
import gov.nih.nci.hpc.dto.security.HpcUserRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementSecurityService;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcSecurityService;
import gov.nih.nci.hpc.service.HpcSystemAccountFunction;
import gov.nih.nci.hpc.service.HpcSystemAccountFunctionNoReturn;

/**
 * HPC Security Business Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcSecurityBusServiceImpl implements HpcSecurityBusService {
  //---------------------------------------------------------------------//
  // Constants
  //---------------------------------------------------------------------//

  // Invalid base path error message.
  private static final String INVALID_DEFAULT_BASE_PATH_ERROR_MESSAGE =
      "Invalid default base path: ";

  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // The security service instance.
  @Autowired private HpcSecurityService securityService = null;

  // The data management (iRODS) security service.
  @Autowired private HpcDataManagementSecurityService dataManagementSecurityService = null;

  // The data management (iRODS) service.
  @Autowired private HpcDataManagementService dataManagementService = null;

  // LDAP authentication on/off switch.
  @Value("${hpc.bus.ldapAuthentication}")
  private Boolean ldapAuthentication = null;

  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcSecurityBusServiceImpl() {}

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  //---------------------------------------------------------------------//
  // HpcUserBusService Interface Implementation
  //---------------------------------------------------------------------//

  @Override
  public void registerUser(String nciUserId, HpcUserRequestDTO userRegistrationRequest)
      throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(nciUserId) || userRegistrationRequest == null) {
      throw new HpcException(
          "Null NCI user ID or user registation request", HpcErrorType.INVALID_REQUEST_INPUT);
    }
    if (userRegistrationRequest.getActive() != null) {
      throw new HpcException(
          "Activation/Deactivation indicator is not allowed in user registration",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }
    if (securityService.getUser(nciUserId) != null) {
      throw new HpcException(
          "User already exists: " + nciUserId, HpcRequestRejectReason.USER_ALREADY_EXISTS);
    }

    // Get the configuration ID associated with the default base path.
    String configurationId = null;
    String defaultBasePath = userRegistrationRequest.getDefaultBasePath();
    if (!StringUtils.isEmpty(defaultBasePath)) {
      configurationId = dataManagementService.getDataManagementConfigurationId(defaultBasePath);
      if (StringUtils.isEmpty(configurationId)) {
        throw new HpcException(
            INVALID_DEFAULT_BASE_PATH_ERROR_MESSAGE + userRegistrationRequest.getDefaultBasePath(),
            HpcErrorType.INVALID_REQUEST_INPUT);
      }
    }

    // Instantiate an NCI account domain object.
    HpcNciAccount nciAccount = new HpcNciAccount();
    nciAccount.setUserId(nciUserId);
    nciAccount.setFirstName(userRegistrationRequest.getFirstName());
    nciAccount.setLastName(userRegistrationRequest.getLastName());
    nciAccount.setDoc(userRegistrationRequest.getDoc());
    nciAccount.setDefaultConfigurationId(configurationId);

    // HPC-DM is integrated with a data management system (IRODS). When registering a user with HPC-DM,
    // this service creates an account for the user with the data management system, unless an account
    // already established for the user.
    if (!dataManagementSecurityService.userExists(nciUserId)) {
      // Determine the user role to create. If not provided, default to USER.
      HpcUserRole role =
          userRegistrationRequest.getUserRole() != null
              ? roleFromString(userRegistrationRequest.getUserRole())
              : HpcUserRole.USER;

      // GROUP_ADMIN not supported by current Jargon API version. Respond with a workaround.
      if (role == HpcUserRole.GROUP_ADMIN) {
        throw new HpcException(
            "GROUP_ADMIN currently not supported by the API. "
                + "Create the account with a USER role, and then run "
                + "'iadmin moduser' command to change the user's role to GROUP_ADMIN",
            HpcRequestRejectReason.API_NOT_SUPPORTED);
      }

      // Create the data management (IRODS) account.
      executeGroupAdminAsSystemAccount(
          () -> dataManagementSecurityService.addUser(nciAccount, role));
    }

    boolean registrationCompleted = false;
    try {
      // Add the user to the system.
      securityService.addUser(nciAccount);

      registrationCompleted = true;

    } finally {
      if (!registrationCompleted) {
        // Registration failed. Remove the data management account.
        dataManagementSecurityService.deleteUser(nciUserId);
      }
    }
  }

  @Override
  public void updateUser(String nciUserId, HpcUserRequestDTO userUpdateRequest)
      throws HpcException {
    // Input validation.
    validateUserUpdateRequest(nciUserId, userUpdateRequest);

    // Get the user.
    HpcUser user = securityService.getUser(nciUserId);
    if (user == null) {
      throw new HpcException(
          "User not found: " + nciUserId, HpcRequestRejectReason.INVALID_NCI_ACCOUNT);
    }

    // Get the current user role.
    HpcUserRole currentUserRole = dataManagementSecurityService.getUserRole(nciUserId);

    // Determine update values.
    String updateFirstName =
        !StringUtils.isEmpty(userUpdateRequest.getFirstName())
            ? userUpdateRequest.getFirstName()
            : user.getNciAccount().getFirstName();
    String updateLastName =
        !StringUtils.isEmpty(userUpdateRequest.getLastName())
            ? userUpdateRequest.getLastName()
            : user.getNciAccount().getLastName();
    String updateDoc =
        !StringUtils.isEmpty(userUpdateRequest.getDoc())
            ? userUpdateRequest.getDoc()
            : user.getNciAccount().getDoc();
    String updateDefaultConfigurationId = user.getNciAccount().getDefaultConfigurationId();
    if (userUpdateRequest.getDefaultBasePath() != null) {
      if (!userUpdateRequest.getDefaultBasePath().isEmpty())
        updateDefaultConfigurationId =
            dataManagementService.getDataManagementConfigurationId(
                userUpdateRequest.getDefaultBasePath());
      if (StringUtils.isEmpty(updateDefaultConfigurationId)) {
        throw new HpcException(
            INVALID_DEFAULT_BASE_PATH_ERROR_MESSAGE + userUpdateRequest.getDefaultBasePath(),
            HpcErrorType.INVALID_REQUEST_INPUT);
      }
    } else {
      // The caller would like to remove default base path.
      updateDefaultConfigurationId = null;
    }

    HpcUserRole updateRole =
        !StringUtils.isEmpty(userUpdateRequest.getUserRole())
            ? roleFromString(userUpdateRequest.getUserRole())
            : currentUserRole;
    boolean active =
        userUpdateRequest.getActive() != null ? userUpdateRequest.getActive() : user.getActive();

    // GROUP_ADMIN not supported by current Jargon API version. Respond with a workaround.
    if (updateRole == HpcUserRole.GROUP_ADMIN) {
      throw new HpcException(
          "GROUP_ADMIN currently not supported by the API. "
              + "Run 'iadmin moduser' command to change the user's role to GROUP_ADMIN",
          HpcRequestRejectReason.API_NOT_SUPPORTED);
    }

    // Update the data management (IRODS) account.
    dataManagementSecurityService.updateUser(
        nciUserId, updateFirstName, updateLastName, updateRole);

    // Update User.
    securityService.updateUser(
        nciUserId,
        updateFirstName,
        updateLastName,
        updateDoc,
        updateDefaultConfigurationId,
        active);
  }

  @Override
  public HpcUserDTO getUser(String nciUserId) throws HpcException {
    // nciUserId is optional. If null, get the request invoker.
    String userId = nciUserId;
    if (userId == null) {
      HpcRequestInvoker invoker = securityService.getRequestInvoker();
      if (invoker == null) {
        throw new HpcException("Null request invoker", HpcErrorType.UNEXPECTED_ERROR);
      }
      userId = invoker.getNciAccount().getUserId();
    }

    // Get the managed data domain object.
    HpcUser user = securityService.getUser(userId);
    if (user == null) {
      return null;
    }

    // Get the default data management configuration for this user.
    HpcDataManagementConfiguration dataManagementConfiguration =
        dataManagementService.getDataManagementConfiguration(
            user.getNciAccount().getDefaultConfigurationId());

    // Map it to the DTO.
    HpcUserDTO userDTO = new HpcUserDTO();
    userDTO.setFirstName(user.getNciAccount().getFirstName());
    userDTO.setLastName(user.getNciAccount().getLastName());
    userDTO.setDoc(user.getNciAccount().getDoc());
    userDTO.setDefaultBasePath(
        dataManagementConfiguration != null ? dataManagementConfiguration.getBasePath() : null);
    userDTO.setUserRole(dataManagementSecurityService.getUserRole(userId).value());
    userDTO.setActive(user.getActive());
    return userDTO;
  }

  @Override
  public HpcUserListDTO getUsers(
      String nciUserId,
      String firstNamePattern,
      String doc,
      String lastNamePattern,
      String defaultBasePath,
      boolean active)
      throws HpcException {
    // Get the users based on search criteria.
    HpcUserListDTO users = new HpcUserListDTO();

    // If search by 'default base path' requested, get the data management configuration ID.
    String defaultConfigurationId = null;
    if (!StringUtils.isEmpty(defaultBasePath)) {
      defaultConfigurationId =
          dataManagementService.getDataManagementConfigurationId(defaultBasePath);
      if (StringUtils.isEmpty(defaultConfigurationId)) {
        throw new HpcException(
            INVALID_DEFAULT_BASE_PATH_ERROR_MESSAGE + defaultBasePath,
            HpcErrorType.INVALID_REQUEST_INPUT);
      }
    }

    // Perform the search and construct the return DTO.
    for (HpcUser user :
        securityService.getUsers(
            nciUserId, firstNamePattern, doc, lastNamePattern, defaultConfigurationId, active)) {
      // Get the default data management configuration for this user.
      HpcDataManagementConfiguration dataManagementConfiguration =
          dataManagementService.getDataManagementConfiguration(
              user.getNciAccount().getDefaultConfigurationId());

      // Add user entry into the return list.
      HpcUserListEntry userListEntry = new HpcUserListEntry();
      userListEntry.setUserId(user.getNciAccount().getUserId());
      userListEntry.setFirstName(user.getNciAccount().getFirstName());
      userListEntry.setLastName(user.getNciAccount().getLastName());
      userListEntry.setDoc(user.getNciAccount().getDoc());
      userListEntry.setDefaultBasePath(
          dataManagementConfiguration != null ? dataManagementConfiguration.getBasePath() : null);
      if (!active) {
        // Set the active flag if the search is for all users.
        userListEntry.setActive(user.getActive());
      }
      users.getUsers().add(userListEntry);
    }

    return users;
  }

  @Override
  public void authenticate(String nciUserId, String password) throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(nciUserId) || StringUtils.isEmpty(password)) {
      throw new HpcException("Null NCI user ID or password", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Authenticate w/ LDAP (optionally).
    if (ldapAuthentication && !securityService.authenticate(nciUserId, password)) {
      throw new HpcException("LDAP authentidaction failed", HpcErrorType.UNAUTHORIZED_REQUEST);
    }

    // Set the request invoker (in thread local).
    setRequestInvoker(
        nciUserId,
        ldapAuthentication ? HpcAuthenticationType.LDAP : HpcAuthenticationType.NONE,
        toDataManagementAccount(nciUserId, password));
  }

  @Override
  public void authenticateSso(String nciUserId, String password) throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(nciUserId)) {
      throw new HpcException("Null NCI user ID", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Set the request invoker (in thread local).
    setRequestInvokerSso(
        nciUserId,
        HpcAuthenticationType.SAML,
        toDataManagementAccount(nciUserId, password));
  }
  
  @Override
  public void authenticate(String authenticationToken) throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(authenticationToken)) {
      throw new HpcException("Null authentication token", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    HpcAuthenticationTokenClaims authenticationTokenClaims =
        securityService.parseAuthenticationToken(authenticationToken);
    if (authenticationTokenClaims == null) {
      throw new HpcException(
          "Invalid or Expired Authentication token", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Set the request invoker.
    setRequestInvoker(
        authenticationTokenClaims.getUserId(),
        HpcAuthenticationType.TOKEN,
        authenticationTokenClaims.getDataManagementAccount());
  }

  @Override
  public HpcAuthenticationResponseDTO getAuthenticationResponse(boolean generateToken)
      throws HpcException {
    // At the time this service is called, the user is already authenticated and the request
    // invoker is set with the authenticated user data.
    HpcRequestInvoker requestInvoker = securityService.getRequestInvoker();
    if (requestInvoker == null) {
      throw new HpcException("Null request invoker", HpcErrorType.UNEXPECTED_ERROR);
    }

    // Construct and return an authentication response DTO.
    HpcAuthenticationResponseDTO authenticationResponse = new HpcAuthenticationResponseDTO();
    authenticationResponse.setAuthenticationType(requestInvoker.getAuthenticationType());
    if (requestInvoker.getNciAccount() != null) {
      authenticationResponse.setUserId(requestInvoker.getNciAccount().getUserId());
    }
    authenticationResponse.setUserRole(requestInvoker.getUserRole());

    // Generate an authentication token. The user can use this token in subsequent calls
    // until the token expires.
    if (generateToken) {
      HpcAuthenticationTokenClaims authenticationTokenClaims = new HpcAuthenticationTokenClaims();
      authenticationTokenClaims.setUserId(requestInvoker.getNciAccount().getUserId());
      authenticationTokenClaims.setDataManagementAccount(requestInvoker.getDataManagementAccount());
      authenticationResponse.setToken(
          securityService.createAuthenticationToken(authenticationTokenClaims));
    }

    return authenticationResponse;
  }

  @Override
  public void registerSystemAccount(HpcSystemAccountDTO systemAccountRegistrationDTO)
      throws HpcException {
    // Input validation.
    if (systemAccountRegistrationDTO == null) {
      throw new HpcException("Null HpcSystemAccountDTO", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Add the user to the managed collection.
    securityService.addSystemAccount(
        systemAccountRegistrationDTO.getAccount(),
        systemAccountRegistrationDTO.getDataTransferType(),
        systemAccountRegistrationDTO.getClassifier());
  }

  @Override
  public HpcGroupMembersResponseDTO registerGroup(
      String groupName, HpcGroupMembersRequestDTO groupMembersRequest) throws HpcException {
    // Input validation.
    if (groupName == null) {
      throw new HpcException("Null group name", HpcErrorType.INVALID_REQUEST_INPUT);
    }
    if (groupMembersRequest != null && !groupMembersRequest.getDeleteUserIds().isEmpty()) {
      throw new HpcException(
          "Delete users is invalid in group registration request",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    return executeGroupAdminAsSystemAccount(
        () -> {
          // Add the group.
          dataManagementSecurityService.addGroup(groupName);

          // Optionally add members.
          return updateGroupMembers(groupName, groupMembersRequest);
        });
  }

  public HpcGroupMembersResponseDTO updateGroup(
      String groupName, HpcGroupMembersRequestDTO groupMembersRequest) throws HpcException {
    // Input validation.
    if (groupName == null) {
      throw new HpcException("Null group name", HpcErrorType.INVALID_REQUEST_INPUT);
    }
    if (!dataManagementSecurityService.groupExists(groupName)) {
      throw new HpcException(
          "Group doesn't exist: " + groupName, HpcErrorType.INVALID_REQUEST_INPUT);
    }
    if (groupMembersRequest == null
        || (groupMembersRequest.getDeleteUserIds().isEmpty()
            && groupMembersRequest.getAddUserIds().isEmpty())) {
      throw new HpcException(
          "Null or empty requests to add/delete members to group",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    return executeGroupAdminAsSystemAccount(
        () ->
            // Add/Delete group members.
            updateGroupMembers(groupName, groupMembersRequest));
  }

  @Override
  public HpcGroupMembersDTO getGroup(String groupName) throws HpcException {
    // Input validation.
    if (groupName == null) {
      throw new HpcException("Null group name", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Get the group members.
    List<String> userIds = dataManagementSecurityService.getGroupMembers(groupName);

    // Construct and return the group members DTO.
    HpcGroupMembersDTO groupMembers = new HpcGroupMembersDTO();
    if (userIds != null) {
      groupMembers.getUserIds().addAll(userIds);
    }

    return groupMembers;
  }

  @Override
  public HpcGroupListDTO getGroups(String groupPattern) throws HpcException {
    // Search for groups.
    List<String> groupNames =
        dataManagementSecurityService.getGroups(groupPattern != null ? groupPattern : "%");
    if (groupNames == null || groupNames.isEmpty()) {
      return null;
    }

    // Construct the DTO to return.
    HpcGroupListDTO groups = new HpcGroupListDTO();
    for (String groupName : groupNames) {
      HpcGroup group = new HpcGroup();
      group.setGroupName(groupName);

      // Get members of this groups.
      List<String> userIds = dataManagementSecurityService.getGroupMembers(groupName);
      if (userIds != null) {
        group.getUserIds().addAll(userIds);
      }

      groups.getGroups().add(group);
    }

    return groups;
  }

  @Override
  public void deleteGroup(String groupName) throws HpcException {
    // Input validation.
    if (groupName == null) {
      throw new HpcException("Null group name", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Delete the group.
    executeGroupAdminAsSystemAccount(() -> dataManagementSecurityService.deleteGroup(groupName));
  }
  
  
  @Override
  public void refreshDataManagementConfigurations() throws HpcException {
    
    securityService.refreshDataManagementConfigurations();
  }

  //---------------------------------------------------------------------//
  // Helper Methods
  //---------------------------------------------------------------------//

  /**
   * When using the command line, a group-admin user can execute account security actions. It is not
   * supported by the Jargon API. As a workaround, if the invoker is a group-admin then we use the
   * HPC-DM system account. This workaround should be removed once the Jargon API allows group-admin
   * to create accounts. If the invoker is not a group-admin, then its own credentials are used.
   *
   * @param systemAccountFunction The functional interface to execute as system account (no return
   *     value)
   * @throws HpcException If the enum value is invalid.
   */
  private void executeGroupAdminAsSystemAccount(
      HpcSystemAccountFunctionNoReturn systemAccountFunction) throws HpcException {
    executeGroupAdminAsSystemAccount(
        () -> {
          systemAccountFunction.execute();
          return null;
        });
  }

  /**
   * When using the command line, a group-admin user can execute account security actions. It is not
   * supported by the Jargon API. As a workaround, if the invoker is a group-admin then we use the
   * HPC-DM system account. This workaround should be removed once the Jargon API allows group-admin
   * to create accounts. If the invoker is not a group-admin, then its own credentials are used.
   *
   * @param systemAccountFunction The functional interface to execute as system account with return
   *     value.
   * @param <T> A generic returned type.
   * @return A generic returned type.
   * @throws HpcException If it failed to identify the request invoker, or the function raised an exception.
   */
  private <T> T executeGroupAdminAsSystemAccount(HpcSystemAccountFunction<T> systemAccountFunction)
      throws HpcException {
    HpcRequestInvoker invoker = securityService.getRequestInvoker();
    if (invoker == null) {
      throw new HpcException("Null request invoker", HpcErrorType.UNEXPECTED_ERROR);
    }

    if (invoker.getUserRole().equals(HpcUserRole.GROUP_ADMIN)) {
      return securityService.executeAsSystemAccount(Optional.empty(), systemAccountFunction);
    } else {
      return systemAccountFunction.execute();
    }
  }

  /**
   * Convert a user role from string to enum.
   *
   * @param roleStr The role string.
   * @return The enum value.
   * @throws HpcException If the enum value is invalid.
   */
  private HpcUserRole roleFromString(String roleStr) throws HpcException {
    try {
      return HpcUserRole.fromValue(roleStr);

    } catch (IllegalArgumentException e) {
      throw new HpcException(
          "Invalid user role: "
              + roleStr
              + ". Valid values: "
              + Arrays.asList(HpcUserRole.values()),
          HpcErrorType.INVALID_REQUEST_INPUT,
          e);
    }
  }

  /**
   * Set the Request invoker (in thread local).
   *
   * @param userId The user ID.
   * @param authenticationType The method the user was authenticated authentication (LDAP or Token).
   * @param dataManagementAccount The data management account.
   * @throws HpcException on service failure.
   */
  private void setRequestInvoker(
      String userId,
      HpcAuthenticationType authenticationType,
      HpcIntegratedSystemAccount dataManagementAccount)
      throws HpcException {
    // Get the HPC user and validate the account is active.
    HpcUser user = securityService.getUser(userId);
    if (user == null) {
      throw new HpcException(
          "User is not registered with HPC-DM: " + userId, HpcErrorType.UNAUTHORIZED_REQUEST);
    }
    if (!user.getActive()) {
      throw new HpcException(
          "User is inactive. Please contact system administrator to activate account: " + userId,
          HpcErrorType.UNAUTHORIZED_REQUEST);
    }

    // Instantiate a request invoker and set it on thread local.
    securityService.setRequestInvoker(
        user.getNciAccount(), ldapAuthentication, authenticationType, dataManagementAccount);

    // Get the user role and update the request invoker.
    securityService
        .getRequestInvoker()
        .setUserRole(
            dataManagementSecurityService.getUserRole(dataManagementAccount.getUsername()));
  }

  /**
   * Set the Request invoker for SSO (in thread local).
   *
   * @param userId The user ID.
   * @param authenticationType The method the user was authenticated authentication (SAML).
   * @param dataManagementAccount The data management account.
   * @throws HpcException on service failure.
   */
  private void setRequestInvokerSso(
      String userId,
      HpcAuthenticationType authenticationType,
      HpcIntegratedSystemAccount dataManagementAccount)
      throws HpcException {
    // Get the HPC user and validate the account is active.
    HpcUser user = securityService.getUser(userId);
    if (user == null) {
      throw new HpcException(
          "User is not registered with HPC-DM: " + userId, HpcErrorType.UNAUTHORIZED_REQUEST);
    }
    if (!user.getActive()) {
      throw new HpcException(
          "User is inactive. Please contact system administrator to activate account: " + userId,
          HpcErrorType.UNAUTHORIZED_REQUEST);
    }

    // Instantiate a request invoker and set it on thread local.
    securityService.setRequestInvoker(
        user.getNciAccount(), false, authenticationType, dataManagementAccount);

    // Get the user role and update the request invoker.
    securityService
        .getRequestInvoker()
        .setUserRole(
            dataManagementSecurityService.getUserRole(dataManagementAccount.getUsername()));
  }
  
  /**
   * Update group members of a group.
   *
   * @param groupName The group name.
   * @param groupMembersRequest A list of users to add and delete from the group.
   * @return A DTO containing the results of each add/delete member request.
   * @throws HpcException on service failure.
   */
  private HpcGroupMembersResponseDTO updateGroupMembers(
      String groupName, HpcGroupMembersRequestDTO groupMembersRequest) throws HpcException {
    if (groupMembersRequest == null) {
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
    if (!userIds.isEmpty()) {
      throw new HpcException(
          "User Id(s) found in both add and delete lists: " + userIds,
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Add group members.
    for (String userId : addUserIds) {
      HpcGroupMemberResponse addGroupMemberResponse = new HpcGroupMemberResponse();
      addGroupMemberResponse.setUserId(userId);
      addGroupMemberResponse.setResult(true);
      try {
        dataManagementSecurityService.addGroupMember(groupName, userId);

      } catch (HpcException e) {
        // Request failed. Record the message and keep going.
        addGroupMemberResponse.setResult(false);
        addGroupMemberResponse.setMessage(e.getMessage());
      }

      // Add this user add group member response to the list.
      groupMembersResponses.getAddGroupMemberResponses().add(addGroupMemberResponse);
    }

    // Delete group members.
    for (String userId : deleteUserIds) {
      HpcGroupMemberResponse deleteGroupMemberResponse = new HpcGroupMemberResponse();
      deleteGroupMemberResponse.setUserId(userId);
      deleteGroupMemberResponse.setResult(true);
      try {
        dataManagementSecurityService.deleteGroupMember(groupName, userId);

      } catch (HpcException e) {
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
      throws HpcException {
    // Input validation.
    if (StringUtils.isEmpty(nciUserId) || userUpdateRequest == null) {
      throw new HpcException(
          "Null NCI user ID or user update request", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate that at least one user attribute (out of firstName, lastName, default base path,
    //                                            DOC, role, active) is updated.
    List<Boolean> updateItems =
        new ArrayList<>(
            Arrays.asList(
                !StringUtils.isEmpty(userUpdateRequest.getFirstName()),
                !StringUtils.isEmpty(userUpdateRequest.getLastName()),
                !StringUtils.isEmpty(userUpdateRequest.getDefaultBasePath()),
                !StringUtils.isEmpty(userUpdateRequest.getDoc()),
                !StringUtils.isEmpty(userUpdateRequest.getUserRole()),
                userUpdateRequest.getActive() != null));
    if (!updateItems.contains(true)) {
      throw new HpcException(
          "Invalid update user request. Please provide either firstName, lastName, "
              + "default-base-path, doc, userRole or active to update",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Validate an administrator is not downgrading self role.
    HpcRequestInvoker invoker = securityService.getRequestInvoker();
    if (invoker == null) {
      throw new HpcException("Null request invoker", HpcErrorType.UNEXPECTED_ERROR);
    }
    if (invoker.getUserRole().equals(HpcUserRole.SYSTEM_ADMIN)
        && nciUserId.equals(invoker.getNciAccount().getUserId())
        && userUpdateRequest.getUserRole() != null
        && !userUpdateRequest.getUserRole().equals(HpcUserRole.SYSTEM_ADMIN.value())) {
      throw new HpcException(
          "Not authorizated to downgrade self role. Please contact system administrator",
          HpcRequestRejectReason.NOT_AUTHORIZED);
    }
  }

  /**
   * Instantiate a data management account object from NCI account credentials.
   *
   * @param nciUserId The NCI user ID.
   * @param password The NCI user's password.
   * @return A data management account with the user's credentials.
   */
  private HpcIntegratedSystemAccount toDataManagementAccount(String nciUserId, String password) {
    // Instantiate a Data Management account.
    HpcIntegratedSystemAccount dataManagementAccount = new HpcIntegratedSystemAccount();
    dataManagementAccount.setIntegratedSystem(HpcIntegratedSystem.IRODS);
    dataManagementAccount.setUsername(nciUserId);

    // Need to escape special characters in password when authenticating this account w/ iRODS.
    String dataManagementPassword = password;
    dataManagementPassword = dataManagementPassword.replace("=", "\\=");
    dataManagementPassword = dataManagementPassword.replace(";", "\\;");
    dataManagementPassword = dataManagementPassword.replace("&", "\\&");
    dataManagementPassword = dataManagementPassword.replace("@", "\\@");
    dataManagementAccount.setPassword(dataManagementPassword);

    return dataManagementAccount;
  }
}
