/**
 * HpcUserService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC User Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcUserService 
{         
    /**
     * Add a user.
     *
     * @param nciAccount The user's NCI account.
     * @param dataTransferAccount The user's data transfer account.
     * @param dataTransferAccount The user's data management account.
     * 
     * @throws HpcException
     */
    public void addUser(HpcNciAccount nciAccount, 
    		            HpcIntegratedSystemAccount dataTransferAccount,
    		            HpcIntegratedSystemAccount dataManagementAccount) 
    		           throws HpcException;
    
    /**
     * Update a user.
     *
     * @param nciUserId The NCI user ID of the user to update.
     * @param firstName The user first name.
     * @param lastName The user last name. 
     * @param DOC The user DOC.
     * @param dataTransferAccount The data transfer account to update.
     * 
     * @throws HpcException
     */
    public void updateUser(String nciUserId, String firstName, String lastName,
    		               String DOC, HpcIntegratedSystemAccount dataTransferAccount) 
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
     * Get the request invoker (user).
     *
     * @return HpcRequestInvoker
     */
    public HpcRequestInvoker getRequestInvoker();
    
    /**
     * Set the service call invoker in the request context.
     *
     * @param user The HPC user to set as the invoker.
     * @param ldapAuthenticated An indicator whether the user was authenticated via LDAP
     */
    public void setRequestInvoker(HpcUser user, boolean ldapAuthenticated);
    
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
}

 