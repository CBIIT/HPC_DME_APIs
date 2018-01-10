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
     * Store a new system account or update it if it exists.
     *
     * @param account The system account to be added/updated.
     * @param dataTransferType The data transfer type to associate with the system account.
     * @throws HpcException on database error.
     */
    public void upsert(HpcIntegratedSystemAccount account, 
    		           HpcDataTransferType dataTransferType,
                   String classifier)
    		          throws HpcException;
    
    /**
     * Get system account. 
     *
     * @param system The system to get the account for
     * @return The system account if found, or null otherwise.
     * @throws HpcException on database error.
     */
    public HpcIntegratedSystemAccount getSystemAccount(HpcIntegratedSystem system) 
    		                                          throws HpcException;
    
    /**
     * Get system account by data transfer type
     *
     * @param dataTransferType The data transfer type associated with the requested system account.
     * @return The system account if found, or null otherwise.
     * @throws HpcException on database error.
     */
    public HpcIntegratedSystemAccount getSystemAccount(HpcDataTransferType dataTransferType) 
    		                                          throws HpcException;


  /**
   * Get system accounts representing Globus pooled/shared application accounts.
   *
   * @return Set containing system accounts info as HpcIntegratedSystemAccount instances
   * @throws HpcException on error
   */
  public List<HpcIntegratedSystemAccount> getGlobusPooledAccounts() throws HpcException;

}

 