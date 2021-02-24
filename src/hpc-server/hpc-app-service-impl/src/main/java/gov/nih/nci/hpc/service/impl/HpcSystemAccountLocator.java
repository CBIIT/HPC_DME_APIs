/**
 * HpcSystemAccountLocator.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;


import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import gov.nih.nci.hpc.dao.HpcSystemAccountDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccountProperty;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC System Account Locator.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcSystemAccountLocator {

  protected static class PooledSystemAccountWrapper implements Comparable {

    private HpcIntegratedSystemAccount systemAccount;
    private double utilizationScore;
    private Date lastUsed = new Date(0);

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
      lastUsed = new Date();
      return systemAccount;
    }

    protected void setSystemAccount(HpcIntegratedSystemAccount pAccount) {
      systemAccount = pAccount;
    }

    protected double getUtilizationScore() {
      return utilizationScore;
    }

    protected Date getLastUsed() {
      return lastUsed;
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
        if (retVal == 0) {
          retVal = lastUsed.compareTo(convPsaWrapper.getLastUsed());
        }
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
  private Map<HpcDataTransferType, Map<String, List<PooledSystemAccountWrapper>>>
      multiDataTransferAccounts =
          new ConcurrentHashMap<
              HpcDataTransferType, Map<String, List<PooledSystemAccountWrapper>>>();

  //  private Map<String, Queue<HpcIntegratedSystemAccount>> globusPooledAccounts =
  //      new ConcurrentHashMap<>();

  // System Accounts DAO

  @Autowired HpcSystemAccountDAO systemAccountDAO = null;

  @Autowired HpcDataManagementConfigurationLocator dataMgmtConfigLocator = null;

  //The IAM user in the Secrets Manager
  @Value("${hpc.service.systemAccount.aws.secretName}")
  private String secretName = null;

  //The region of the user
  @Value("${hpc.service.systemAccount.aws.region}")
  private String region = null;

  @Value("${hpc.service.systemAccount.aws.useSecretsManager}")
  private boolean useSecretsManager = false;
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
	if(useSecretsManager) {
		refreshAwsSecret();
	}
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
    } else {
      throw new HpcException(
          String.format(
              "Call to get data for exactly one system account for integrated system of %s could not be resolved.",
              system.value()),
          HpcErrorType.UNEXPECTED_ERROR);
    }

    return retSysAcct;
  }


  /**
   * Get system account by data transfer type and DOC classifier
   *
   * @param dataTransferType The data transfer type associated with the requested system account.
   * @param hpcDataMgmtConfigId The ID of specific data management configuration.
   * @return The system account if found, or null otherwise.
   * @throws HpcException on service failure.
   */
  public HpcIntegratedSystemAccount getSystemAccount(
      HpcDataTransferType dataTransferType, String hpcDataMgmtConfigId) throws HpcException {
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
    logger.info(
        String.format(
            "setGlobusAccountQueueSize: Entered with systemAccountId = %s, queueSize = %s",
            systemAccountId, Integer.toString(queueSize)));
    boolean scoreUpdated = false;
    final Map<String, List<PooledSystemAccountWrapper>> classifier2ListMap =
        multiDataTransferAccounts.get(HpcDataTransferType.GLOBUS);
    if (null == classifier2ListMap || classifier2ListMap.isEmpty()) {
      logger.warn(
          "setGlobusAccountQueueSize: There are no pools of GLOBUS app accounts, so do nothing.");
    } else {
      for (Map.Entry<String, List<PooledSystemAccountWrapper>> mapEntry :
          classifier2ListMap.entrySet()) {
        final List<PooledSystemAccountWrapper> thePool = mapEntry.getValue();
        final String poolClassifier = mapEntry.getKey();
        if (null == thePool) {
          logger.warn(
              String.format(
                  "setGlobusAccountQueueSize: Globus app accounts for classifier \"%s\" is null."),
              poolClassifier);
        } else if (thePool.isEmpty()) {
          logger.warn(
              String.format(
                  "setGlobusAccountQueueSize: Globus app accounts for classifier \"%s\" is empty."),
              poolClassifier);
        } else if (scoreUpdated =
            updateAppAccountUtilizationScore(thePool, systemAccountId, queueSize)) {
          logger.info(
              String.format(
                  "setGlobusAccountQueueSize: Updated Globus app account's utilization score; found it in pool having classifier \"%s\"",
                  poolClassifier));
          break;
        } else {
          // do nothing, pool was neither null nor empty and didn't have the app account
        }
      }
    }
    logger.info(
        "setGlobusAccountQueueSize: About to exit.  Score was "
            + (scoreUpdated ? "" : "NOT")
            + " updated.");
  }

  private boolean updateAppAccountUtilizationScore(
      List<PooledSystemAccountWrapper> pWrappedSysAccounts, String pSysAccountId, int pQueueSize) {
    boolean modifiedScoreFlag = false;
    if (null == pWrappedSysAccounts
        || pWrappedSysAccounts.isEmpty()
        || null == pSysAccountId
        || pSysAccountId.isEmpty()
        || pQueueSize < 0) {
      // do nothing, as one or more inputs are invalid
    } else {
      for (PooledSystemAccountWrapper psaWrapper : pWrappedSysAccounts) {
        final HpcIntegratedSystemAccount sysAccnt = psaWrapper.getSystemAccount();
        if (null != sysAccnt && pSysAccountId.equals(sysAccnt.getUsername())) {
          // Internally, queueSize is treated as a utilization score, higher meaning experiencing
          //  greater utilization
          psaWrapper.setUtilizationScore(Integer.valueOf(pQueueSize).doubleValue());
          modifiedScoreFlag = true;
          break;
        }
      }
    }
    return modifiedScoreFlag;
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
      final List<HpcIntegratedSystemAccount> accounts =
          systemAccountDAO.getSystemAccount(dataTransferType);
      if (accounts != null && accounts.size() == 1) {
        singularDataTransferAccounts.put(dataTransferType, accounts.get(0));
      } else if (accounts != null && accounts.size() > 1) {
        initMultiDataTransferAccountsMap(dataTransferType, accounts);
      }
    }
  }

  // Populate the data transfer accounts map for transfer type that involves shared/pooled
  //  accounts
  private void initMultiDataTransferAccountsMap(
      HpcDataTransferType transferType, List<HpcIntegratedSystemAccount> theAccts) {
    if (null == multiDataTransferAccounts.get(transferType)) {
      multiDataTransferAccounts.put(
          transferType, new ConcurrentHashMap<String, List<PooledSystemAccountWrapper>>());
    }
    final Map<String, List<PooledSystemAccountWrapper>> classifier2ListMap =
        multiDataTransferAccounts.get(transferType);
    for (HpcIntegratedSystemAccount someAcct : theAccts) {
      final String classifierValue = fetchSysAcctPropertyValue(someAcct, PROPERTY_NAME_CLASSIFIER);
      if (null == classifierValue) {
        // no classifier property so do nothing with this system account as classifier indicates which
        //  pool (or bucket) it belongs in
      } else {
        if (null == classifier2ListMap.get(classifierValue)) {
          classifier2ListMap.put(classifierValue, new ArrayList<PooledSystemAccountWrapper>());
        }
        classifier2ListMap.get(classifierValue).add(new PooledSystemAccountWrapper(someAcct));
      }
    }
  }


  private void refreshAwsSecret() throws HpcException {
	  String secret = null;
	  HpcIntegratedSystemAccount account = new HpcIntegratedSystemAccount();

	  //get the current credentials from the HPC System Account table
	  final List<HpcIntegratedSystemAccount> accounts = systemAccountDAO.getSystemAccount(HpcIntegratedSystem.AWS);
      if (accounts == null || accounts.isEmpty()) {
	      String message = "Error retrieving secret from AWS system account ";
	      logger.error(message);
	      throw new HpcException(message, HpcErrorType.UNEXPECTED_ERROR);
      }
      HpcIntegratedSystemAccount systemAccount = accounts.get(0);

      //Get the new credentials from the AWS Secrets Manager

	  //Create the credential provider based on the configured credentials.
	  BasicAWSCredentials s3ArchiveCredentials = new BasicAWSCredentials(systemAccount.getUsername(), systemAccount.getPassword());
	      AWSStaticCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(s3ArchiveCredentials);

      // Create a Secrets Manager client
	  AWSSecretsManager client  = AWSSecretsManagerClientBuilder.standard()
	      .withRegion(region).withCredentials(awsCredentialsProvider).build();

	  try {
	      GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
                   .withSecretId(secretName);
		  GetSecretValueResult getSecretValueResult = client.getSecretValue(getSecretValueRequest);
	      // Decrypt the secret using the associated KMS CMK,
		  //depending on whether the secret is a string or binary
		  if (getSecretValueResult.getSecretString() != null) {
		      secret = getSecretValueResult.getSecretString();
		  }
		  else {
		      secret = new String(Base64.getDecoder().decode(getSecretValueResult.getSecretBinary()).array());
		  }
	  } catch (Exception e) {
	      String message = "Error retrieving secret from AWS Secrets Manager: " + e.getMessage();
	      logger.error(message, e.getStackTrace());
	      throw new HpcException(message, HpcErrorType.UNEXPECTED_ERROR);
	  }

	  try {
		  // extract the AccessKeyID and SecretAccessKey
	      Object obj = new JSONParser().parse(secret);
          JSONObject jsonObj = (JSONObject) obj;
          account.setUsername((String) jsonObj.get("AccessKeyId"));
          account.setPassword((String) jsonObj.get("SecretAccessKey"));
	  } catch (Exception e){
	      String message = "Error extracting credentials from secret for AWS S3";
	      logger.error(message + ": " + secret, e.getStackTrace());
	      throw new HpcException(message, HpcErrorType.UNEXPECTED_ERROR);
	  }

      //Store the latest credentials if different from current one
      if(!systemAccount.getUsername().contentEquals(account.getUsername())) {
          //There is a different key now, so update the existing one
          account.setIntegratedSystem(HpcIntegratedSystem.AWS);
          systemAccountDAO.update(account, null, null);
      }
  }


  // Logic for selecting which "pool" of Globus app accounts to utilize based on HPC Data Mgmt
  // Configuration ID
  private List<PooledSystemAccountWrapper> accessProperPool(String hpcDataMgmtConfigId)
      throws HpcException {
    logger.info(
        String.format(
            "accessProperPool: entered with received hpcDataMgmtConfigId = %s",
            hpcDataMgmtConfigId));
    String docClassifier = null;
    final Map<String, List<PooledSystemAccountWrapper>> classifier2PoolMap =
        multiDataTransferAccounts.get(HpcDataTransferType.GLOBUS);
    final HpcDataManagementConfiguration dmConfig =
        this.dataMgmtConfigLocator.get(hpcDataMgmtConfigId);

    if (null == dmConfig) {
      logger.info(
          String.format(
              "accessProperPool: determined no data mgmt configuration matches, apply default classifier %s",
              DOC_CLASSIFIER_DEFAULT));
      // If no matching Data Mgmt Config, then use default classifier
      docClassifier = DOC_CLASSIFIER_DEFAULT;
    } else {
      // If matching Data Mgmt Config, then determine if that Config's DOC is a key in the map.
      // If so, use the DOC as the classifier key into the map; otherwise, use the default classifier.
      docClassifier =
          classifier2PoolMap.containsKey(dmConfig.getDoc())
              ? dmConfig.getDoc()
              : DOC_CLASSIFIER_DEFAULT;
      logger.info(
          String.format(
              "accessProperPool: DOC is %s, DOC classifier to use is %s",
              dmConfig.getDoc(), docClassifier));
    }
    final List<PooledSystemAccountWrapper> retProperPool = classifier2PoolMap.get(docClassifier);
    logger.info("accessProperPool: about to return");

    return retProperPool;
  }

  private HpcIntegratedSystemAccount obtainPooledGlobusAppAcctInfo(String hpcDataMgmtConfigId)
      throws HpcException {
    logger.info(
        String.format(
            "obtainPooledGlobusAppAcctInfo: received hpcDataMgmtConfigId = %s.",
            hpcDataMgmtConfigId));
    final List<PooledSystemAccountWrapper> theGlobusAcctsPool =
        accessProperPool(hpcDataMgmtConfigId);

    // Choose PooledSystemAccountWrapper instance from pool that has least value according
    //  to natural ordering of PooledSystemAccountWrapper, which is by utilizationScore
    //  property.  Lower score indicates the wrapped system account is being utilized less.
    final PooledSystemAccountWrapper wrapperObj = Collections.min(theGlobusAcctsPool);
    if (null == wrapperObj) {
      throw new HpcException(
          "Unable to obtain Globus app account credentials from pool",
          HpcErrorType.UNEXPECTED_ERROR);
    } else {
      final HpcIntegratedSystemAccount retSysAcct = wrapperObj.getSystemAccount();
      logger.info(
          String.format(
              "obtainPooledGlobusAppAcctInfo: successfully acquired Globus app account having client ID of %s, about to return.",
              retSysAcct.getUsername()));
      return retSysAcct;
    }
  }

  private String fetchSysAcctPropertyValue(
      HpcIntegratedSystemAccount someAcct, String propertyName) {
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
