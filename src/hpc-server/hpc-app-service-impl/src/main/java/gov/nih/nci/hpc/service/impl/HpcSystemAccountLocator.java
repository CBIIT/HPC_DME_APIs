/**
 * HpcSystemAccountLocator.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.dao.HpcSystemAccountDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * HPC System Account Locator.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */
public class HpcSystemAccountLocator {
  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // Map data transfer type to a 'credentials' structure.
  private Map<HpcIntegratedSystem, HpcIntegratedSystemAccount> systemAccounts =
      new ConcurrentHashMap<HpcIntegratedSystem, HpcIntegratedSystemAccount>();
  private Map<HpcDataTransferType, HpcIntegratedSystemAccount> dataTransferAccounts =
      new ConcurrentHashMap<HpcDataTransferType, HpcIntegratedSystemAccount>();

  // System Accounts DAO
  @Autowired HpcSystemAccountDAO systemAccountDAO = null;

  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /** Default Constructor for spring dependency injection. */
  private HpcSystemAccountLocator() {}

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  /**
   * Reload the system accounts from DB. Called by Spring as init-method.
   *
   * @throws HpcException on service failure.
   */
  public void reload() throws HpcException {
    // Populate the system accounts map.
    for (HpcIntegratedSystem system : HpcIntegratedSystem.values()) {
      // Get the data transfer system account.
      HpcIntegratedSystemAccount account = systemAccountDAO.getSystemAccount(system);
      if (account != null) {
        systemAccounts.put(system, account);
      }
    }

    // Populate the data transfer accounts map.
    for (HpcDataTransferType dataTransferType : HpcDataTransferType.values()) {
      // Get the data transfer system account.
      HpcIntegratedSystemAccount account = systemAccountDAO.getSystemAccount(dataTransferType);
      if (account != null) {
        dataTransferAccounts.put(dataTransferType, account);
      }
    }
  }

  /**
   * Get system account.
   *
   * @param system The system to get the account for
   * @return The system account if found, or null otherwise.
   * @throws HpcException on service failure.
   */
  public HpcIntegratedSystemAccount getSystemAccount(HpcIntegratedSystem system)
      throws HpcException {
    return systemAccounts.get(system);
  }

  /**
   * Get system account by data transfer type
   *
   * @param dataTransferType The data transfer type associated with the requested system account.
   * @return The system account if found, or null otherwise.
   * @throws HpcException on service failure.
   */
  public HpcIntegratedSystemAccount getSystemAccount(HpcDataTransferType dataTransferType)
      throws HpcException {
    return dataTransferAccounts.get(dataTransferType);
  }
}
