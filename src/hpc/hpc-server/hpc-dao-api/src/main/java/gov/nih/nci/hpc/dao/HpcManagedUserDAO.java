/**
 * HpcManagedUserDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.model.HpcManagedUser;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Managed User DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcManagedUserDAO 
{         
    /**
     * Add managed user to the repository.
     *
     * @param managedUser The managed user to add to the DB.
     * 
     * @throws HpcException
     */
    public void add(HpcManagedUser managedUser) throws HpcException;
    
    /**
     * Get managed user from the repository by ID.
     *
     * @param id the managed user ID.
     * @return The managed user if found, or null otherwise.
     * 
     * @throws HpcException
     */
    public HpcManagedUser get(String id) throws HpcException;
}

 