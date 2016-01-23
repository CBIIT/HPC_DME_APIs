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
     * Get a user.
     *
     * @param nciUserId The managed user NCI ID.
     * @return The managed user.
     * 
     * @throws HpcException
     */
    public HpcUser getUser(String nciUserId) throws HpcException;
    
    /**
     * Get the request invoker (user).
     *
     * @return HpcUser
     */
    public HpcUser getRequestInvoker();
    
    /**
     * Set the service call invoker (user) in the request context.
     *
     * @param user The user to set as the service call invoker.
     */
    public void setRequestInvoker(HpcUser user);
    
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

 