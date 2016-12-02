/**
 * HpcDataManagementSecurityService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.model.HpcDataManagementAccount;
import gov.nih.nci.hpc.domain.model.HpcGroup;
import gov.nih.nci.hpc.domain.user.HpcGroupResponse;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Data Management Security Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDataManagementSecurityService 
{   
    /**
     * Add a user.
     *
     * @param nciAccount The NCI account of the user to be added to data management.
     * @param userRole The HPC user role to assign to the new user.
     * @throws HpcException on service failure.
     */
    public void addUser(HpcNciAccount nciAccount, HpcUserRole userRole) 
    		           throws HpcException;
    
    /**
     * Update a user.
     *
     * @param nciUserId The NCI user ID of the user to update.
     * @param firstName The user first name.
     * @param lastName The user last name. 
     * @param userRole The HPC user role to update for the user.
     * @throws HpcException on service failure.
     */
    public void updateUser(String nciUserId, String firstName, String lastName,
                           HpcUserRole userRole) 
    		              throws HpcException;
    
    /**
     * Delete a user.
     *
     * @param nciUserId The user-id to delete.
     * @throws HpcException on service failure.
     */
    public void deleteUser(String nciUserId) throws HpcException;
    
    /**
     * Get the role of a given user's name.
     *
     * @param username The user's name.
     * @return HpcUserRole The user's role.
     * @throws HpcException on service failure.
     */
    public HpcUserRole getUserRole(String username) throws HpcException;  
    
    /**
     * Create User group.
     * 
     * @param group group name.
     * @param addUserId List of userIds to add to the group.
     * @param removeUserId List of userIds to remove from the group.
     * @return A group response.
     * @throws HpcException on service failure.
     */
    public HpcGroupResponse setGroup(HpcGroup group, List<String> addUserId, List<String> removeUserId) 
    		                        throws HpcException;
    
    /**
     * Create HPC data management account from proxy account object. 
     * This is to cache proxy data management account for better performance
     * @param proxyAccount The proxy account.
     * @return HpcDataManagementAccount to tokenize 
     * @throws HpcException on service failure.
     */
    public HpcDataManagementAccount getHpcDataManagementAccount(Object proxyAccount) throws HpcException;
    
    /**
     * Create Proxy data management account from cached HPC data management account 
     * @param managementAccount The management account.
     * @return Tokenized account
     * @throws HpcException on service failure.
     */
    public Object getProxyManagementAccount(HpcDataManagementAccount managementAccount) throws HpcException;
}

 