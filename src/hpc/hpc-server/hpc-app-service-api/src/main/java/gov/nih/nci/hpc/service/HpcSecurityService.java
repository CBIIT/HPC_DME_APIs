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
     * @param dataTransferAccount The user's data management account.
     * 
     * @throws HpcException
     */
    public void addUser(HpcNciAccount nciAccount, 
    		            HpcIntegratedSystemAccount dataTransferAccount) 
    		           throws HpcException;
    
    /**
     * Update a user.
     *
     * @param nciUserId The NCI user ID of the user to update.
     * @param firstName The user first name.
     * @param lastName The user last name. 
     * @param doc The user DOC.
     * 
     * @throws HpcException
     */
    public void updateUser(String nciUserId, String firstName, String lastName, String doc) 
    		              throws HpcException;

    /**
     * Get a user.
     *
     * @param nciUserId The registered user NCI ID.
     * @return The registered user.
     * 
     * @throws HpcException
     */
    public HpcUser getUser(String nciUserId) throws HpcException;
    
    /**
     * Get user role.
     *
     * @param nciUserId The registered user NCI ID.
     * @return user role.
     * 
     * @throws HpcException
     */
    public HpcUserRole getUserRole(String nciUserId) throws HpcException;

    /**
     * Get the request invoker (user).
     *
     * @return HpcRequestInvoker
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
     * @throws HpcException
     */
    public void setSystemRequestInvoker() throws HpcException;
    
    /**
     * Authenticate a user (via LDAP).
     *
     * @param userName The user's name.
     * @param password The password.
     * @return true if the user was successfully authenticated, or false otherwise.
     * 
     * @throws HpcException
     */
    public boolean authenticate(String userName, String password) throws HpcException;
    
    /**
     * Add a system account.
     *
     * @param account The system account to be added/updated.
     * @param dataTransferType The data transfer type to associate with the system account.
     * 
     * @throws HpcException
     */
    public void addSystemAccount(HpcIntegratedSystemAccount account, 
	                             HpcDataTransferType dataTransferType) 
	                            throws HpcException;
    
    /**
     * Create an authentication token, so the caller can use it in subsequent calls.
     *
     * @param authenticationTokenClaims The token's claims to put in the token.
     * @return An Authentication token.
     * 
     * @throws HpcException
     */
    public String createAuthenticationToken(HpcAuthenticationTokenClaims authenticationTokenClaims)
    		                               throws HpcException;
    
    /**
     * Parse an authentication token.
     *
     * @param authenticationToken The token to parse
     * @return The token claims if was able to parse, or null otherwise.
     * 
     * @throws HpcException
     */
    public HpcAuthenticationTokenClaims parseAuthenticationToken(String authenticationToken)
    		                                                    throws HpcException;
}

 