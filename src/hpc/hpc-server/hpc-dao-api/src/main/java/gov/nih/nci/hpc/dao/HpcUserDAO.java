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
 * @version $Id$
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
     * @param firstNamePattern (Optional) The first-name pattern to search for (In the form of SQL 'LIKE' pattern, 
     *                         using case insensitive matching).
     * @param lastNamePattern (Optional) The last-name pattern to search for (In the form of SQL 'LIKE' pattern, 
     *                        using case insensitive matching).
     * @param doc User DOC                       
     * @param active If set to true, only active users are searched. Otherwise, all users (active and inactive) are searched.
     * @return A list of users.
     * @throws HpcException on service failure.
     */
    public List<HpcUser> getUsers(String nciUserId, String firstNamePattern, String lastNamePattern, String doc, boolean active) 
    		                     throws HpcException;
}

 