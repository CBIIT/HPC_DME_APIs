/**
 * HpcSystemAccountLocator.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.dao.HpcDataManagementConfigurationDAO;
import gov.nih.nci.hpc.dao.HpcSystemAccountDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccountProperty;
import gov.nih.nci.hpc.exception.HpcException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * HPC System Account Locator.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */
public class HpcSystemAccountLocator {

  protected static class PooledSystemAccountWrapper implements Comparable {

    private HpcIntegratedSystemAccount systemAccount;
    private double utilizationScore;

    protected PooledSystemAccountWrapper(HpcIntegratedSystemAccount pAccount, double pScore) {
      this.systemAccount = pAccount;
      this.utilizationScore = pScore;
    }

    protected PooledSystemAccountWrapper(HpcIntegratedSystemAccount pAccount) {
      this(pAccount, 0.0);
    }

    protected PooledSystemAccountWrapper() {
      this(null, 0.0);
    }

    protected HpcIntegratedSystemAccount getSystemAccount() {
      return systemAccount;
    }

    protected void setSystemAccount(HpcIntegratedSystemAccount pAccount) {
      systemAccount = pAccount;
    }

    protected double getUtilizationScore() {
      return utilizationScore;
    }

    protected void setUtilizationScore(double pScore) {
      utilizationScore = pScore;
    }

    @Override
    public int compareTo(Object o) {
      int retVal = -1;
      if (o instanceof PooledSystemAccountWrapper) {
        PooledSystemAccountWrapper convPsaWrapper = (PooledSystemAccountWrapper) o;
        retVal = Double.valueOf(utilizationScore).compareTo(convPsaWrapper.getUtilizationScore());
      }
      return retVal;
    }

  }

  private static final String DOC_CLASSIFIER_DEFAULT = "DEFAULT";
  private static final String PROPERTY_NAME_CLASSIFIER = "classifier";

  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//
  // The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  // singularSystemAccounts contains data about system accounts for which there is only 1
  //   account per system
  private Map<HpcIntegratedSystem, HpcIntegratedSystemAccount> singularSystemAccounts =
      new ConcurrentHashMap<HpcIntegratedSystem, HpcIntegratedSystemAccount>();

  // Map data transfer type to a 'credentials' structure.

  // singularDataTransferAccounts contains data about system accounts for data transfer for which
  //   there is only 1 account per data transfer type
  private Map<HpcDataTransferType, HpcIntegratedSystemAccount> singularDataTransferAccounts =
      new ConcurrentHashMap<HpcDataTransferType, HpcIntegratedSystemAccount>();

  // multiDataTransferAccounts contains data about system accounts for data transfer for which
  //   there are more than 1 account per data transfer type
  private Map<HpcDataTransferType, Map<String, List<PooledSystemAccountWrapper>>> multiDataTransferAccounts =
      new ConcurrentHashMap<HpcDataTransferType, Map<String, List<PooledSystemAccountWrapper>>>();

//  private Map<String, Queue<HpcIntegratedSystemAccount>> globusPooledAccounts =
//      new ConcurrentHashMap<>();

  // System Accounts DAO
  @Autowired HpcSystemAccountDAO systemAccountDAO = null;

  @Autowired HpcDataManagementConfigurationLocator dataMgmtConfigLocator = null;

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
    initSystemAccountsData();
    initDataTransferAccountsData();
    this.dataMgmtConfigLocator.reload();
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
    HpcIntegratedSystemAccount retSysAcct = null;

    if (null != singularSystemAccounts.get(system)) {
      retSysAcct = singularSystemAccounts.get(system);
    }
    else {
      throw new HpcException(String.format(
          "Call to get data for exactly one system account for integrated system of %s could not be resolved.",
          system.value()), HpcErrorType.UNEXPECTED_ERROR);
    }

    return retSysAcct;
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
    return getSystemAccount(dataTransferType, DOC_CLASSIFIER_DEFAULT);
  }

  /**
   * Get system account by data transfer type and DOC classifier
   *
   * @param dataTransferType The data transfer type associated with the requested system account.
   * @param docClassifier The indicator of which DOC bucket
   * @return The system account if found, or null otherwise.
   * @throws HpcException on service failure.
   */
  public HpcIntegratedSystemAccount getSystemAccount(HpcDataTransferType dataTransferType,
      String hpcDataMgmtConfigId)
      throws HpcException {
    HpcIntegratedSystemAccount retSysAcct = null;

    if (null != singularDataTransferAccounts.get(dataTransferType)) {
      retSysAcct = singularDataTransferAccounts.get(dataTransferType);
    } else if (null != multiDataTransferAccounts.get(dataTransferType)) {
      // Assumption: GLOBUS is 1 and only data transfer type having multiple system accounts supporting it
      retSysAcct = obtainPooledGlobusAppAcctInfo(hpcDataMgmtConfigId);
    }

    return retSysAcct;
  }

  /**
   * Sets account queue size for particular Globus system account.
   *
   * @param systemAccountId The ID of the Globus system account (should be client ID in UUID format)
   * @param queueSize Size of the transfer queue of the Globus system account
   */
  public void setGlobusAccountQueueSize(String systemAccountId, int queueSize) {
    final Map<String, List<PooledSystemAccountWrapper>> classifier2ListMap = multiDataTransferAccounts
        .get(HpcDataTransferType.GLOBUS);
    outer:
    for (Map.Entry<String, List<PooledSystemAccountWrapper>> mapEntry : classifier2ListMap
        .entrySet()) {
      final List<PooledSystemAccountWrapper> thePool = mapEntry.getValue();
      inner:
      for (PooledSystemAccountWrapper psaWrapper : thePool) {
        if (psaWrapper.getSystemAccount().getUsername().equals(systemAccountId)) {
          // Internally, queueSize is treated as a utilization score, higher meaning experiencing
          //  greater utilization
          psaWrapper.setUtilizationScore(Integer.valueOf(queueSize).doubleValue());
          break outer;
        }
      }
    }
  }

  // Populate the system accounts maps.
  private void initSystemAccountsData() throws HpcException {
    for (HpcIntegratedSystem system : HpcIntegratedSystem.values()) {
      // Get the data transfer system account.
      final List<HpcIntegratedSystemAccount> accounts = systemAccountDAO.getSystemAccount(system);
      if (accounts != null && accounts.size() == 1) {
        singularSystemAccounts.put(system, accounts.get(0));
      }
    }
  }

  // Populate the data transfer accounts maps.
  private void initDataTransferAccountsData() throws HpcException {
    for (HpcDataTransferType dataTransferType : HpcDataTransferType.values()) {
      // Get the data transfer system account.
      final List<HpcIntegratedSystemAccount> accounts = systemAccountDAO
          .getSystemAccount(dataTransferType);
      if (accounts != null && accounts.size() == 1) {
        singularDataTransferAccounts.put(dataTransferType, accounts.get(0));
      } else if (accounts != null && accounts.size() > 1) {
        initMultiDataTransferAccountsMap(dataTransferType, accounts);
      }
    }
  }

  // Populate the data transfer accounts map for transfer type that involves shared/pooled
  //  accounts
  private void initMultiDataTransferAccountsMap(HpcDataTransferType transferType,
      List<HpcIntegratedSystemAccount> theAccts) {
    if (null == multiDataTransferAccounts.get(transferType)) {
      multiDataTransferAccounts
          .put(transferType,
              new ConcurrentHashMap<String, List<PooledSystemAccountWrapper>>());
    }
    final Map<String, List<PooledSystemAccountWrapper>> classifier2ListMap = multiDataTransferAccounts
        .get(transferType);
    for (HpcIntegratedSystemAccount someAcct : theAccts) {
      final String classifierValue = fetchSysAcctPropertyValue(someAcct, PROPERTY_NAME_CLASSIFIER);
      if (null == classifier2ListMap.get(classifierValue)) {
        classifier2ListMap
            .put(classifierValue, new ArrayList<PooledSystemAccountWrapper>());
      }
      classifier2ListMap.get(classifierValue).add(new PooledSystemAccountWrapper(someAcct));
    }
  }


  // Logic for selecting which "pool" of Globus app accounts to utilize based on HPC Data Mgmt
  // Configuration ID
  private List<PooledSystemAccountWrapper> accessProperPool(String hpcDataMgmtConfigId)
      throws HpcException {
    logger.debug(
        String.format("accessProperPool: entered with received hpcDataMgmtConfigId = ",
            hpcDataMgmtConfigId));
    String docClassifier = null;
    final Map<String, List<PooledSystemAccountWrapper>> classifier2PoolMap = multiDataTransferAccounts
        .get(HpcDataTransferType.GLOBUS);
    final HpcDataManagementConfiguration dmConfig = this.dataMgmtConfigLocator
        .get(hpcDataMgmtConfigId);
    if (null == dmConfig) {
      logger.debug(
          String.format(
              "accessProperPool: determined no data mgmt configuration matches, apply default classifier %s",
              DOC_CLASSIFIER_DEFAULT));
      // If no matching Data Mgmt Config, then use default classifier
      docClassifier = DOC_CLASSIFIER_DEFAULT;
    } else {
      // If matching Data Mgmt Config, then determine if that Config's DOC is a key in the map.
      // If so, use the DOC as the classifier key into the map; otherwise, use the default classifier.
      docClassifier = classifier2PoolMap.containsKey(dmConfig.getDoc()) ? dmConfig.getDoc()
          : DOC_CLASSIFIER_DEFAULT;
      logger.debug(
          String
              .format("accessProperPool: DOC is %s, DOC classifier to use is %s", dmConfig.getDoc(),
                  DOC_CLASSIFIER_DEFAULT));
    }
    final List<PooledSystemAccountWrapper> retProperPool = classifier2PoolMap.get(docClassifier);
    logger.debug("accessProperPool: about to return");

    return retProperPool;
  }


  private HpcIntegratedSystemAccount obtainPooledGlobusAppAcctInfo(String hpcDataMgmtConfigId)
      throws HpcException {
    logger.debug(String
        .format(
            "obtainPooledGlobusAppAcctInfo: received hpcDataMgmtConfigId = %s.",
            hpcDataMgmtConfigId));
    final List<PooledSystemAccountWrapper> theGlobusAcctsPool = accessProperPool(
        hpcDataMgmtConfigId);

    // Choose PooledSystemAccountWrapper instance from pool that has least value according
    //  to natural ordering of PooledSystemAccountWrapper, which is by utilizationScore
    //  property.  Lower score indicates the wrapped system account is being utilized less.
    final PooledSystemAccountWrapper wrapperObj = Collections.min(theGlobusAcctsPool);
    if (null == wrapperObj) {
      throw new HpcException("Unable to obtain Globus app account credentials from pool",
          HpcErrorType.UNEXPECTED_ERROR);
    } else {
      final HpcIntegratedSystemAccount retSysAcct = wrapperObj.getSystemAccount();
      logger.debug(String.format(
          "obtainPooledGlobusAppAcctInfo: successfully acquired Globus app account having client ID of %s, about to return.",
          retSysAcct.getUsername()));
      return retSysAcct;
    }
  }

  private String fetchSysAcctPropertyValue(HpcIntegratedSystemAccount someAcct,
      String propertyName) {
    String retPropVal = null;
    final List<HpcIntegratedSystemAccountProperty> allProps = someAcct.getProperties();
    for (HpcIntegratedSystemAccountProperty someProp : allProps) {
      if (propertyName.equals(someProp.getName())) {
        retPropVal = someProp.getValue();
        break;
      }
    }
    return retPropVal;
  }

}
