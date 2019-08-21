/**
 * HpcSecurityService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.model.HpcAuthenticationTokenClaims;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcAuthenticationType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;
import java.util.Optional;

/**
 * HPC User Application Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcSecurityService {
  /**
   * Add a user.
   *
   * @param nciAccount The user's NCI account.
   * @throws HpcException on service failure.
   */
  public void addUser(HpcNciAccount nciAccount) throws HpcException;

  /**
   * Update a user.
   *
   * @param nciUserId The NCI user ID of the user to update.
   * @param firstName The user first name.
   * @param lastName The user last name.
   * @param doc The DOC.
   * @param defaultConfigurationId The default configuration ID for this user.
   * @param active The active indicator.
   * @throws HpcException on service failure.
   */
  public void updateUser(
      String nciUserId,
      String firstName,
      String lastName,
      String doc,
      String defaultConfigurationId,
      boolean active)
      throws HpcException;

  /**
   * Get a user.
   *
   * @param nciUserId The registered user NCI ID.
   * @return The registered user.
   * @throws HpcException on service failure.
   */
  public HpcUser getUser(String nciUserId) throws HpcException;

  /**
   * Get users by search criterias. Note: only active users are returned.
   *
   * @param nciUserId (Optional) The user ID to search for (using case insensitive comparison).
   * @param firstNamePattern (Optional) The first-name pattern to search for (using case insensitive
   *     matching). SQL LIKE wildcards ('%', '_') are supported.
   * @param lastNamePattern (Optional) The last-name pattern to search for (using case insensitive
   *     matching). SQL LIKE wildcards ('%', '_') are supported.
   * @param active If set to true, only active users are searched. Otherwise, all users (active and
   *     inactive) are searched.
   * @param doc The DOC.
   * @param defaultConfigurationId The default configuration ID.
   * @return A list of users.
   * @throws HpcException on service failure.
   */
  public List<HpcUser> getUsers(
      String nciUserId,
      String firstNamePattern,
      String lastNamePattern,
      String doc,
      String defaultConfigurationId,
      boolean active)
      throws HpcException;

  /**
   * Get user role.
   *
   * @param nciUserId The registered user NCI ID.
   * @return user role.
   * @throws HpcException on service failure.
   */
  public HpcUserRole getUserRole(String nciUserId) throws HpcException;

  /**
   * Get the request invoker (user).
   *
   * @return The request invoker.
   */
  public HpcRequestInvoker getRequestInvoker();

  /**
   * Set the service call invoker in the request context.
   *
   * @param nciAccount The invoker's (HPC user) NCI account.
   * @param ldapAuthentication Indicator whether LDAP authentication is turned on/off.
   * @param authenticationType The method the user was authenticated.
   * @param dataManagementAccount The data management account to be used when invoking data
   *     management actions on behalf of the invoker.
   * @throws HpcException on service failure.
   */
  public void setRequestInvoker(
      HpcNciAccount nciAccount,
      boolean ldapAuthentication,
      HpcAuthenticationType authenticationType,
      HpcIntegratedSystemAccount dataManagementAccount)
      throws HpcException;

  /**
   * Perform a function using system account.
   *
   * @param ldapAuthentication (Optional) If true - authenticate the system account via LDAP.
   * @param systemAccountFunction The function to perform as system account.
   * @return The functional interface return type
   * @throws HpcException thrown by the function.
   */
  public <T> T executeAsSystemAccount(
      Optional<Boolean> ldapAuthentication, HpcSystemAccountFunction<T> systemAccountFunction)
      throws HpcException;
  
  /**
   * Perform a function using system account, but no return (generic) type.
   *
   * @param ldapAuthentication (Optional) If true - authenticate the system account via LDAP.
   * @param systemAccountFunction The function to perform as system account.
   * @throws HpcException thrown by the function.
   */
  public void executeAsSystemAccount(
      Optional<Boolean> ldapAuthentication, HpcSystemAccountFunctionNoReturn systemAccountFunction)
      throws HpcException;

  /**
   * Authenticate a user (via LDAP).
   *
   * @param userName The user's name.
   * @param password The password.
   * @return true if the user was successfully authenticated, or false otherwise.
   * @throws HpcException on service failure.
   */
  public boolean authenticate(String userName, String password) throws HpcException;


 /**
  * Authenticate a user (via SPS).
  * 
  * @param nciUserId The user id.
  * @param smSession The NIHSMSESSION
  * @return true if the user was successfully authenticated and smSession is still valid, or false otherwise.
  * @throws HpcException on service failure.
  */
public boolean authenticateSso(String nciUserId, String smSession) throws HpcException;
   
  /**
   * Add a system account.
   *
   * @param account The system account to be added/updated.
   * @param dataTransferType The data transfer type to associate with the system account.
   * @param classifier The classifier to provide finer grained detail than system
   * @throws HpcException on service failure.
   */
  public void addSystemAccount(
      HpcIntegratedSystemAccount account, HpcDataTransferType dataTransferType, String classifier)
      throws HpcException;

  /**
   * Create an authentication token, so the caller can use it in subsequent calls.
   *
   * @param authenticationType The authentication type.
   * @param authenticationTokenClaims The token's claims to put in the token.
   * @return An Authentication token.
   * @throws HpcException on service failure.
   */
  public String createAuthenticationToken(
		  HpcAuthenticationType authenticationType, HpcAuthenticationTokenClaims authenticationTokenClaims)
      throws HpcException;

  /**
   * Parse an authentication token.
   *
   * @param authenticationToken The token to parse
   * @return The token claims if was able to parse, or null otherwise.
   * @throws HpcException on service failure.
   */
  public HpcAuthenticationTokenClaims parseAuthenticationToken(String authenticationToken)
      throws HpcException;
  
  
  /**
   * Reload data management configurations from the database
   * 
   * @throws HpcException as service failure
   */
  public void refreshDataManagementConfigurations() throws HpcException;
   
}
