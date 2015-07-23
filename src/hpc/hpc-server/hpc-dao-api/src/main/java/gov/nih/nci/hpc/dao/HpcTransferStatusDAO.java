/**
 * HpcTransferStatusDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC HpcTransferStatus DAO Interface.
 * </p>
 *
 * @author <a href="mailto:mahidhar.narra@nih.gov">Mahidhar Narra</a>
 * @version $Id: HpcTransferStatusDAO.java 258 2015-06-28 19:32:24Z narram $
 */

public interface HpcTransferStatusDAO 
{         
    /**
     * Add user to the repository.
     *
     * @param HpcDataTransferRequest The hpcDataTransferRequest to add to the DB.
     * 
     * @throws HpcException
     */
    public void add(HpcDataTransferRequest hpcDataTransferRequest) throws HpcException;
    
    /**
     * Get user from the repository by ID.
     *
     * @param submissionId the Transfer submissionId ID.
     * @return The user if found, or null otherwise.
     * 
     * @throws HpcException
     */
    public HpcDataTransferRequest get(String submissionId) throws HpcException;
}

 