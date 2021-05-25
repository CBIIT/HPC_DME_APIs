/**
 * HpcUserDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import java.util.List;

import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC User DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcUserDAO 
{    
    /**
     * Store a new user to the repository or update it if it exists.
     *
     * @param user The user to be added/updated.
     * @throws HpcException on database error.
     */
    public void upsertUser(HpcUser user) throws HpcException;


    /**
     * Delete the user from the repository.
     *
     * @param userId The user to be deleted.
     * @throws HpcException on database error.
     */
    void deleteUser(String userId) throws HpcException;


    /**
     * Get user from the repository by ID.
     *
     * @param nciUserId the user NCI ID.
     * @return The user if found, or null otherwise.
     * @throws HpcException on database error.
     */
    public HpcUser getUser(String nciUserId) throws HpcException;
    
    /**
     * Get users by search criterias.
     *
     * @param nciUserId (Optional) The user ID to search for (using case insensitive comparison).
     * @param firstNamePattern (Optional) The first-name pattern to search for (using case sensitive matching).
     *                                    SQL LIKE wildcards ('%', '_') are supported. 
     * @param lastNamePattern (Optional) The last-name pattern to search for (using case sensitive matching).
     *                                   SQL LIKE wildcards ('%', '_') are supported. 
     * @param doc (Optional) The doc.  
     * @param defaultConfigurationId (Optional) The default data management configuration ID.                    
     * @param active If set to true, only active users are searched. Otherwise, all users (active and inactive) are searched.
     * @return A list of users.
     * @throws HpcException on service failure.
     */
    public List<HpcUser> getUsers(String nciUserId, String firstNamePattern, String lastNamePattern, 
    		                      String doc, String defaultConfigurationId, boolean active) 
    		                     throws HpcException;



    /**
      * Get users from the repository by role.
      *
      * @param role The role to filter by
      * @param doc (Optional) The doc to filter by.
      * @param defaultConfigurationId (Optional) The default data management configuration ID.
      * @param active If set to true, only active users are searched. Otherwise, all users (active and inactive) are searched.
      * @return A list of users.
      * @return The user if found, or null otherwise.
      * @throws HpcException on database error.
      */
    public List<HpcUser> getUsersByRole(String role, String doc,
                String defaultConfigurationId, boolean active)
               throws HpcException;

    /**
     * Query users by search criteria.
     *
     * @param nciUserIdPattern (Optional) The user ID pattern to search for (using case insensitive comparison).
     * @param firstNamePattern (Optional) The first-name pattern to search for (using case sensitive matching).
     *                                    SQL LIKE wildcards ('%', '_') are supported. 
     * @param lastNamePattern (Optional) The last-name pattern to search for (using case sensitive matching).
     *                                   SQL LIKE wildcards ('%', '_') are supported. 
     * @param doc (Optional) The doc.  
     * @param defaultConfigurationId (Optional) The default data management configuration ID.                    
     * @param active If set to true, only active users are searched. Otherwise, all users (active and inactive) are searched.
     * @return A list of users.
     * @throws HpcException on service failure.
     */
    public List<HpcUser> queryUsers(String nciUserIdPattern, String firstNamePattern, String lastNamePattern, String doc,
			String defaultConfigurationId, boolean active) throws HpcException;


    /**
     * Check if user is a data curator
     * 
     * @param nciUserId The user ID
     * @return true if user is data curator
     * @throws HpcException on service failure.
     */
    public boolean isUserDataCurator(String nciUserId) throws HpcException;
}

 