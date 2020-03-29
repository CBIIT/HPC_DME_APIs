/**
 * HpcSystemAcountDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import java.util.List;

/**
 * <p>
 * HPC System Account DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcSystemAccountDAO 
{    
    /**
     * Store a new system account.
     *
     * @param account The system account to be added.
     * @param dataTransferType The data transfer type to associate with the system account.
     * @param classifier The classifier to offer finer grained detail than system or transfer type
     * @throws HpcException on database error.
     */
    public void upsert(HpcIntegratedSystemAccount account, 
    		           HpcDataTransferType dataTransferType,
                   String classifier)
    		          throws HpcException;



    /**
     * Update a system account if it exists.
     *
     * @param account The system account to be updated.
     * @param dataTransferType The data transfer type to associate with the system account.
     * @param classifier The classifier to offer finer grained detail than system or transfer type
     * @throws HpcException on database error.
     */
    public void update(HpcIntegratedSystemAccount account,
    		           HpcDataTransferType dataTransferType,
                   String classifier)
    		          throws HpcException;

    
    /**
     * Get system accounts.
     *
     * @param system The system to get the accounts for
     * @return The system accounts if found, or null otherwise.
     * @throws HpcException on database error.
     */
    public List<HpcIntegratedSystemAccount> getSystemAccount(HpcIntegratedSystem system)
    		                                          throws HpcException;
    
    /**
     * Get system accounts by data transfer type
     *
     * @param dataTransferType The data transfer type associated with the requested system accounts.
     * @return The system accounts if found, or null otherwise.
     * @throws HpcException on database error.
     */
    public List<HpcIntegratedSystemAccount> getSystemAccount(HpcDataTransferType dataTransferType)
    		                                          throws HpcException;
}

 