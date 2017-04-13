/**
 * HpcSecurityService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
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

/**
 * <p>
 * HPC User Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id: HpcSecurityService.java 1013 2016-03-26 23:06:30Z rosenbergea $
 */

public interface HpcSecurityService 
{         
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
     * @param doc The user DOC.
     * @param active The active indicator.
     * @throws HpcException on service failure.
     */
    public void updateUser(String nciUserId, String firstName, String lastName, String doc, boolean active) 
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
     * @param firstNamePattern (Optional) The first-name pattern to search for (In the form of SQL 'LIKE' pattern, 
     *                         using case insensitive matching).
     * @param lastNamePattern (Optional) The last-name pattern to search for (In the form of SQL 'LIKE' pattern, 
     *                        using case insensitive matching).
     * @param active If set to true, only active users are searched. Otherwise, all users (active and inactive) are searched.
     * @param doc User DOC
     * @return A list of users.
     * @throws HpcException on service failure.
     */
    public List<HpcUser> getUsers(String nciUserId, String firstNamePattern, String lastNamePattern, String doc, boolean active) 
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
     * @param authenticationType The method the user was authenticated.
     * @param dataManagementAccount The data management account to be used when invoking data management actions on behalf
     *                              of the invoker.
     * @throws HpcException on service failure.
     */
    public void setRequestInvoker(HpcNciAccount nciAccount, HpcAuthenticationType authenticationType,
    		                      HpcIntegratedSystemAccount dataManagementAccount) throws HpcException;
    
    /**
     * Set the service call invoker in the request context using system account.
     * 
     * @throws HpcException on service failure.
     */
    public void setSystemRequestInvoker() throws HpcException;
    
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
     * Add a system account.
     *
     * @param account The system account to be added/updated.
     * @param dataTransferType The data transfer type to associate with the system account.
     * @throws HpcException on service failure.
     */
    public void addSystemAccount(HpcIntegratedSystemAccount account, 
	                             HpcDataTransferType dataTransferType) 
	                            throws HpcException;
    
    /**
     * Create an authentication token, so the caller can use it in subsequent calls.
     *
     * @param authenticationTokenClaims The token's claims to put in the token.
     * @return An Authentication token.
     * @throws HpcException on service failure.
     */
    public String createAuthenticationToken(HpcAuthenticationTokenClaims authenticationTokenClaims)
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
    
}

 