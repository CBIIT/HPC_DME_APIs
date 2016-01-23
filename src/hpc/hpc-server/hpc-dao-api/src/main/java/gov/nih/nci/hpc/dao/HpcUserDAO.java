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
     * 
     * @throws HpcException
     */
    public void upsert(HpcUser user) throws HpcException;
    
    /**
     * Get user from the repository by ID.
     *
     * @param nciUserId the user NCI ID.
     * @return The user if found, or null otherwise.
     * 
     * @throws HpcException
     */
    public HpcUser getUser(String nciUserId) throws HpcException;
}

 