/**
 * HpcManagedUserService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.model.HpcManagedUser;
import gov.nih.nci.hpc.domain.user.HpcUser;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Managed User Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcManagedUserService 
{         
    /**
     * Add managed user.
     *
     * @param user The user to add.
     * @return The registered user ID.
     * 
     * @throws HpcException
     */
    public String add(HpcUser user) throws HpcException;
    
    /**
     * Get managed user.
     *
     * @param nihUserId The managed user NIH ID.
     * @return The managed user.
     * 
     * @throws HpcException
     */
    public HpcManagedUser get(String nihUserId) throws HpcException;
}

 