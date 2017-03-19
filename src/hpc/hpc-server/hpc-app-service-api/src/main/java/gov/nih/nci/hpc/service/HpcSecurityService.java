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
    public void addUser(HpcNciAccount nciAccount) 
    		           throws HpcException;
    
    /**
     * Update a user.
     *
     * @param nciUserId (Optional) The NCI user ID of the user to update. 
     *                  If null then the request invoker is updated
     * @param firstName The user first name.
     * @param lastName The user last name. 
     * @param doc The user DOC.
     * @throws HpcException on service failure.
     */
    public void updateUser(String nciUserId, String firstName, String lastName, String doc) 
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
     * Get users by search criterias.
     *
     * @param nciUserId (Optional) The user ID to search for.
     * @param firstName (Optional) The first name to search for.
     * @param lastName (Optional) The last name to search for.
     * @return A list of users.
     * @throws HpcException on service failure.
     */
    public List<HpcUser> getUsers(String nciUserId, String firstName, String lastName) 
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
     * @param user The HPC user to set as the invoker.
     * @param ldapAuthenticated An indicator whether the user was authenticated via LDAP.
     */
    public void setRequestInvoker(HpcUser user, boolean ldapAuthenticated);
    
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
    
    /**
     * Authorize a service request with a user-id as an input parameter.
     * System admin are authorized to call with any user-id. Other users are only
     * authorized to invoke with their own user-id.
     *
     * @param nciUserId The user-id to authorize.
     * @throws HpcException If the call is not authorized.
     */
    public void authorizeUserService(String nciUserId) throws HpcException;
}

 