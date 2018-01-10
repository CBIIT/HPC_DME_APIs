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
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccountProperty;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * HPC System Account Locator.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */
public class HpcSystemAccountLocator {

  private static final long GLOBUS_ACCT_POOL_RETRY_INTERVAL_MS = 5000l;

  private static final String DOC_CLASSIFIER_DEFAULT = "DEFAULT";
  private static final String PROPERTY_NAME_CLASSIFIER = "classifier";

  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

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
  //   there are more than 1 account per data transfer typeHpcIntegratedSystemAccount polledAcct = theQueue.poll();
  private Map<HpcDataTransferType, Map<String, Queue<HpcIntegratedSystemAccount>>> multiDataTransferAccounts =
      new ConcurrentHashMap<HpcDataTransferType, Map<String, Queue<HpcIntegratedSystemAccount>>>();

//  private Map<String, Queue<HpcIntegratedSystemAccount>> globusPooledAccounts =
//      new ConcurrentHashMap<>();

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
    initSystemAccountsData();
    initDataTransferAccountsData();
//    loadGlobusPooledAccounts();
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
    final Map<String, Queue<HpcIntegratedSystemAccount>> classifier2QueueMap = multiDataTransferAccounts
        .get(transferType);
    for (HpcIntegratedSystemAccount someAcct : theAccts) {
      final String classifierValue = fetchSysAcctPropertyValue(someAcct, PROPERTY_NAME_CLASSIFIER);
      if (null == classifier2QueueMap.get(classifierValue)) {
        classifier2QueueMap
            .put(classifierValue, new ConcurrentLinkedQueue<HpcIntegratedSystemAccount>());
      }
      classifier2QueueMap.get(classifierValue).add(someAcct);
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
      String docClassifier)
      throws HpcException {
    HpcIntegratedSystemAccount retSysAcct = null;

    if (null != singularDataTransferAccounts.get(dataTransferType)) {
      retSysAcct = singularDataTransferAccounts.get(dataTransferType);
    } else if (null != multiDataTransferAccounts.get(dataTransferType)) {
      // Assumption: P_GLOBUS is 1 and only data transfer type having multiple system accounts supporting it
      retSysAcct = obtainPooledGlobusAppAcctInfo(docClassifier);
    }

    return retSysAcct;
  }

  /**
   * Return a shared system account's data to pool of such system accounts data.
   *
   * @param someSharedSysAcct The shared system account
   */
  public void returnSharedSystemAccount(HpcIntegratedSystemAccount someSharedSysAcct) {
    // Assumption: P_GLOBUS is 1 and only data transfer type having multiple system accounts supporting it
    final String classifierValue = fetchSysAcctPropertyValue(someSharedSysAcct,
        PROPERTY_NAME_CLASSIFIER);
    Queue<HpcIntegratedSystemAccount> acctsQueue = multiDataTransferAccounts
        .get(HpcDataTransferType.P_GLOBUS).get(classifierValue);
    if (null != acctsQueue) {
      acctsQueue.add(someSharedSysAcct);
    }
  }

  private HpcIntegratedSystemAccount obtainPooledGlobusAppAcctInfo(String docClassifier) {
    HpcIntegratedSystemAccount retSysAcct = null;
    final Map<String, Queue<HpcIntegratedSystemAccount>> classifier2QueueMap = multiDataTransferAccounts
        .get(HpcDataTransferType.P_GLOBUS);
    final Queue<HpcIntegratedSystemAccount> theQueue =
        (null == classifier2QueueMap.get(docClassifier)) ?
            classifier2QueueMap.get(DOC_CLASSIFIER_DEFAULT)
            : classifier2QueueMap.get(docClassifier);
    boolean polledSuccessfully = false;
    while (!polledSuccessfully) {
      try {
        retSysAcct = theQueue.poll();
        polledSuccessfully = true;
      } catch (NoSuchElementException nseEx) {
        sleepWithoutExceptionHandling(GLOBUS_ACCT_POOL_RETRY_INTERVAL_MS);
        polledSuccessfully = false;
      }
    }
    return retSysAcct;
  }

  private void sleepWithoutExceptionHandling(long msDuration) {
    try {
      Thread.currentThread().sleep(msDuration);
    } catch (InterruptedException e) {
      // do nothing
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

  // populate another data transfer accounts map for pooled/shared Globus app accounts
//  private void loadGlobusPooledAccounts() throws HpcException {
//    final List<HpcIntegratedSystemAccount> queriedAcctData = systemAccountDAO.getGlobusPooledAccounts();
//    for (HpcIntegratedSystemAccount pooledAcct : queriedAcctData) {
//      final String classifierVal = fetchSysAcctPropertyValue(pooledAcct, PROPERTY_NAME_CLASSIFIER);
//      if (null != classifierVal) {
//        if (null == globusPooledAccounts.get(classifierVal)) {
//          globusPooledAccounts.put(classifierVal, new ConcurrentLinkedQueue<HpcIntegratedSystemAccount>());
//        }
//        globusPooledAccounts.get(classifierVal).add(pooledAcct);
//      }
//    }
//  }

  /*
   * Get system account by data transfer type
   *
   * @param dataTransferType The data transfer type associated with the requested system account.
   * @param docName The name of the DOC for which Globus transfer is desired
   * @return The system account if found, or null otherwise.
   * @throws HpcException on service failure.
   */
//  public HpcIntegratedSystemAccount getSystemAccountV2(HpcDataTransferType dataTransferType, String docName)
//      throws HpcException {
//    //if S3/CleverSafe transfer type or Globus transfer type, then go by original logic
//    if (HpcDataTransferType.S_3.equals(dataTransferType) || HpcDataTransferType.GLOBUS.equals(dataTransferType)) {
//      return singularDataTransferAccounts.get(dataTransferType);
//    }
/*
    //if P_GLOBUS transfer type then obtain one of the shared app accounts from proper pool
    else if (HpcDataTransferType.P_GLOBUS.equals(dataTransferType)) {
      return borrowPooledGlobusAcct(docName);
    }
*/
//    else {
//      return null;
//    }
//  }

  /*
   * Borrow a shared Globus app account from a pool of such accounts corresponding to a DOC bucket.
   *
   * @param whichDoc - indicates which DOC bucket (single DOC or collection of DOCs)
   * @return HpcIntegratedSystemAccount object representing a shared Globus app account
   */
//  public HpcIntegratedSystemAccount borrowPooledGlobusAcct(String whichDoc) {
//    HpcIntegratedSystemAccount retObj = null;
//    final Queue<HpcIntegratedSystemAccount> acctsPool =
//        (null == globusPooledAccounts.get(whichDoc)) ?
//            globusPooledAccounts.get(DOC_CLASSIFIER_DEFAULT) :
//            globusPooledAccounts.get(whichDoc);
//    boolean pollSuceeded = false;
//    do {
//      try {
//        retObj = acctsPool.poll();
//        pollSuceeded = true;
//      } catch (NoSuchElementException nseEx) {
//        sleepWithoutExceptionHandling(GLOBUS_ACCT_POOL_RETRY_INTERVAL_MS);
//        pollSuceeded = false;
//      }
//    } while (!pollSuceeded);
//
//    return retObj;
//  }


  /*
   * Return a shared Globus app account to whichever pool of such accounts it belongs. That pool
   * corresponds to a DOC bucket.
   *
   * @param theAcct HpcIntegratedSystemAccount object representing shared GLobus app account
   */
//  public void returnPooledGlobusAcct(HpcIntegratedSystemAccount theAcct) {
//    final String docBucket = fetchSysAcctPropertyValue(theAcct, PROPERTY_NAME_CLASSIFIER);
//    if (null != docBucket) {
//      if (null == globusPooledAccounts.get(docBucket)) {
//        globusPooledAccounts
//            .put(docBucket, new ConcurrentLinkedQueue<HpcIntegratedSystemAccount>());
//      }
//      globusPooledAccounts.get(docBucket).add(theAcct);
//    }
//  }

}
