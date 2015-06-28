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
     * Add managed user to the repository.
     *
     * @param user The user to add to the DB.
     * 
     * @throws HpcException
     */
    public void add(HpcUser user) throws HpcException;
    
    /**
     * Get user from the repository by ID.
     *
     * @param nihUserId the user NIH ID.
     * @return The user if found, or null otherwise.
     * 
     * @throws HpcException
     */
    public HpcUser get(String nihUserId) throws HpcException;
}

 