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
import gov.nih.nci.hpc.domain.user.HpcNihAccount;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
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
     * Add user.
     *
     * @param nihAccount The user's NIH account.
     * @param dataTransferAccount The user's data transfer account.
     * 
     * @throws HpcException
     */
    public void add(HpcNihAccount nihAccount, 
    		        HpcDataTransferAccount dataTransferAccount) 
    		       throws HpcException;
    
    /**
     * Get user.
     *
     * @param nihUserId The managed user NIH ID.
     * @return The managed user.
     * 
     * @throws HpcException
     */
    public HpcUser get(String nihUserId) throws HpcException;
}

 