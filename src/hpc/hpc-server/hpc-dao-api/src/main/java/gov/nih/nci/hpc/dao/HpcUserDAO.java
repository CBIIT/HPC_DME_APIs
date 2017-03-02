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
    public void upsert(HpcUser user) throws HpcException;
    
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
     * @param nciUserId (Optional) The user ID to search for.
     * @param firstName (Optional) The first name to search for.
     * @param lastName (Optional) The last name to search for.
     * @return A list of users.
     * @throws HpcException on service failure.
     */
    public List<HpcUser> getUsers(String nciUserId, String firstName, String lastName) 
    		                     throws HpcException;
}

 