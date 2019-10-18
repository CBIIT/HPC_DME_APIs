/**
 * HpcIrodsConnection.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration.irods.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcAuthenticationType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccountProperty;
import gov.nih.nci.hpc.exception.HpcException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.irods.jargon.core.connection.AuthScheme;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.auth.AuthResponse;
import org.irods.jargon.core.exception.AuthenticationException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.CollectionAO;
import org.irods.jargon.core.pub.CollectionAndDataObjectListAndSearchAO;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.UserAO;
import org.irods.jargon.core.pub.UserGroupAO;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HPC iRODS connection via Jargon.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcIRODSConnection {
  //---------------------------------------------------------------------//
  // Constants
  //---------------------------------------------------------------------//

  private static final String DEFAULT_STORAGE_RESOURCE_PROPERTY = "DEFAULT_STORAGE_RESOURCE";
  private static final String HOME_DIRECTORY_PROPERTY = "HOME_DIRECTORY";
  private static final String HOST_PROPERTY = "HOST";
  private static final String PROXY_NAME_PROPERTY = "PROXY_NAME";
  private static final String PROXY_ZONE_PROPERTY = "PROXY_ZONE";
  private static final String ZONE_PROPERTY = "ZONE";
  private static final String PORT_PROPERTY = "PORT";

  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // iRODS file system.
  private IRODSFileSystem irodsFileSystem = null;

  // iRODS connection attributes.
  private String irodsHost = null;
  private Integer irodsPort = null;
  private String irodsZone = null;
  private String irodsResource = null;
  private String basePath = null;
  private String key = null;
  private String algorithm = null;
  private Boolean pamAuthentication = null;

  // The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /**
   * Constructor for Spring Dependency Injection.
   *
   * @param irodsHost The iRODS server host name / IP.
   * @param irodsPort The iRODS server port.
   * @param irodsZone The iRODS zone.
   * @param irodsResource The iRODS resource to use.
   * @param basePath The iRODS base path.
   * @param key The configured key.
   * @param algorithm The configured algorithm
   * @param pamAuthentication PAM authentication on/off switch.
   * @throws HpcException If it failed to instantiate the iRODS file system.
   */
  private HpcIRODSConnection(
      String irodsHost, Integer irodsPort, String irodsZone, String irodsResource, String basePath, String key, String algorithm, Boolean pamAuthentication)
      throws HpcException {
    if (irodsHost == null
        || irodsHost.isEmpty()
        || irodsPort == null
        || irodsZone == null
        || irodsZone.isEmpty()
        || irodsResource == null
        || irodsResource.isEmpty()
        || basePath == null
        || basePath.isEmpty()
        || key == null
        || key.isEmpty()
        || algorithm == null
        || algorithm.isEmpty()
        || pamAuthentication == null) {
      throw new HpcException(
          "Null or empty iRODS connection attributes", HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }
    this.irodsHost = irodsHost;
    this.irodsPort = irodsPort;
    this.irodsZone = irodsZone;
    this.irodsResource = irodsResource;
    this.basePath = basePath;
    this.key = key;
    this.algorithm = algorithm;
    this.pamAuthentication = pamAuthentication;

    try {
      irodsFileSystem = IRODSFileSystem.instance();

    } catch (JargonException e) {
      throw new HpcException(
          "Failed to instantiate iRODs file system: " + e.getMessage(),
          HpcErrorType.DATA_MANAGEMENT_ERROR,
          e);
    }
  }

  /**
   * Default Constructor disabled.
   *
   * @throws HpcException Default constructor disabled.
   */
  private HpcIRODSConnection() throws HpcException {
    throw new HpcException(
        "HpcIRODSConnection default constructor disabled", HpcErrorType.SPRING_CONFIGURATION_ERROR);
  }

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  /**
   * Get iRODS file factory instance.
   *
   * @param authenticatedToken An authenticated token.
   * @return The iRODS file factory.
   * @throws HpcException on iRODS failure.
   */
  public IRODSFileFactory getIRODSFileFactory(Object authenticatedToken) throws HpcException {
    try {
      return irodsFileSystem.getIRODSFileFactory(getIrodsAccount(authenticatedToken));

    } catch (JargonException e) {
      throw new HpcException(
          "Failed to get iRODs file factory instance: " + e.getMessage(),
          HpcErrorType.DATA_MANAGEMENT_ERROR,
          e);
    }
  }

  /**
   * Get iRODS Collection AO instance.
   *
   * @param authenticatedToken An authenticated token.
   * @return A collection AO.
   * @throws HpcException on iRODS failure.
   */
  public CollectionAO getCollectionAO(Object authenticatedToken) throws HpcException {
    try {
      return irodsFileSystem
          .getIRODSAccessObjectFactory()
          .getCollectionAO(getIrodsAccount(authenticatedToken));

    } catch (JargonException e) {
      throw new HpcException(
          "Failed to get iRODs Colelction Access Object: " + e.getMessage(),
          HpcErrorType.DATA_MANAGEMENT_ERROR,
          e);
    }
  }

  /**
   * Get iRODS Data Object AO instance.
   *
   * @param authenticatedToken An authenticated token.
   * @return A data object AO.
   * @throws HpcException on iRODS failure.
   */
  public DataObjectAO getDataObjectAO(Object authenticatedToken) throws HpcException {
    try {
      return irodsFileSystem
          .getIRODSAccessObjectFactory()
          .getDataObjectAO(getIrodsAccount(authenticatedToken));

    } catch (JargonException e) {
      throw new HpcException(
          "Failed to get iRODs Data Object Access Object: " + e.getMessage(),
          HpcErrorType.DATA_MANAGEMENT_ERROR,
          e);
    }
  }

  /**
   * Get iRODS Data Object AO instance.
   *
   * @param authenticatedToken An authenticated token.
   * @return A data object AO.
   * @throws HpcException on iRODS failure.
   */
  public CollectionAndDataObjectListAndSearchAO getCollectionAndDataObjectListAndSearchAO(
      Object authenticatedToken) throws HpcException {
    try {
      return irodsFileSystem
          .getIRODSAccessObjectFactory()
          .getCollectionAndDataObjectListAndSearchAO(getIrodsAccount(authenticatedToken));

    } catch (JargonException e) {
      throw new HpcException(
          "Failed to get iRODs Collection & Data Object Listing Access Object: " + e.getMessage(),
          HpcErrorType.DATA_MANAGEMENT_ERROR,
          e);
    }
  }

  /**
   * Get iRODS User AO instance.
   *
   * @param authenticatedToken An authenticated Data Management account
   * @return A user AO.
   * @throws HpcException on iRODS failure.
   */
  public UserAO getUserAO(Object authenticatedToken) throws HpcException {
    try {
      return irodsFileSystem
          .getIRODSAccessObjectFactory()
          .getUserAO(getIrodsAccount(authenticatedToken));

    } catch (JargonException e) {
      throw new HpcException(
          "Failed to get iRODs User Access Object: " + e.getMessage(),
          HpcErrorType.DATA_MANAGEMENT_ERROR,
          e);
    }
  }

  /**
   * Get iRODS User AO instance.
   *
   * @param authenticatedToken An authenticated Data Management account
   * @return A user group AO.
   * @throws HpcException on iRODS failure.
   */
  public UserGroupAO getUserGroupAO(Object authenticatedToken) throws HpcException {
    try {
      return irodsFileSystem
          .getIRODSAccessObjectFactory()
          .getUserGroupAO(getIrodsAccount(authenticatedToken));

    } catch (JargonException e) {
      throw new HpcException(
          "Failed to get iRODs User Group Access Object: " + e.getMessage(),
          HpcErrorType.DATA_MANAGEMENT_ERROR,
          e);
    }
  }

  /**
   * Authenticate an account.
   *
   * @param dataManagementAccount A data management account to authenticate.
   * @param authenticationType The authentication type.
   * @return An authenticated IRODSAccount object, or null if authentication failed.
   * @throws HpcException on iRODS failure.
   */
  public IRODSAccount authenticate(
      HpcIntegratedSystemAccount dataManagementAccount,
      HpcAuthenticationType authenticationType)
      throws HpcException {

    // Determine the authentication scheme to use. PAM if flag is turned on.
    AuthScheme authenticationScheme = this.pamAuthentication ? AuthScheme.PAM : AuthScheme.STANDARD;

    IRODSAccount irodsAccount = null;
    try {
      if (authenticationType.equals(HpcAuthenticationType.TOKEN)
          && !dataManagementAccount.getProperties().isEmpty()) {
        // The data management account was previously authenticated. Return an authenticated iRODS account.
        irodsAccount = toAuthenticatedIrodsAccount(dataManagementAccount, authenticationScheme);

      } else {
    	logger.info("username:" + dataManagementAccount.getUsername());
        // Authenticate the data management account.
        AuthResponse authResponse =
            irodsFileSystem
                .getIRODSAccessObjectFactory()
                .authenticateIRODSAccount(
                    toUnauthenticatedIrodsAccount(dataManagementAccount, authenticationScheme));
        if (authResponse != null) {
          irodsAccount = authResponse.getAuthenticatedIRODSAccount();

          // Update the HPC data management account with iRODS accounts properties
          dataManagementAccount.getProperties().addAll(getIrodsAccountProperties(irodsAccount));

          logger.info(
              "IRODS account authenticated: {} [Scheme requested: {}, Scheme used: {} ]",
              irodsAccount.getUserName(),
              authenticationScheme,
              irodsAccount.getAuthenticationScheme());
        }
      }

      return irodsAccount;

    } catch (AuthenticationException ae) {
      throw new HpcException(
          "iRODS authentication failed: "
              + dataManagementAccount.getUsername()
              + ", "
              + ae.getMessage(),
          HpcErrorType.DATA_MANAGEMENT_ERROR,
          HpcIntegratedSystem.IRODS,
          ae);

    } catch (JargonException e) {
      throw new HpcException(
          "Failed to authenticate an iRODS account: " + e.getMessage(),
          HpcErrorType.DATA_MANAGEMENT_ERROR,
          HpcIntegratedSystem.IRODS,
          e);
    } catch (NoSuchAlgorithmException e) {
      throw new HpcException(
          "Failed to authenticate an iRODS account: " + e.getMessage(),
          HpcErrorType.DATA_MANAGEMENT_ERROR,
          HpcIntegratedSystem.IRODS,
          e);
    }
  }

  /**
   * Close iRODS connection of an account.
   *
   * @param authenticatedToken An authenticated token.
   */
  public void disconnect(Object authenticatedToken) {
    try {
      irodsFileSystem
          .getIRODSAccessObjectFactory()
          .closeSessionAndEatExceptions(getIrodsAccount(authenticatedToken));

    } catch (Exception e) {
      logger.error("Failed to close iRODS session: " + e);
    }
  }

  /**
   * Get the iRODS zone.
   *
   * @return The iRODS zone.
   */
  public String getZone() {
    return irodsZone;
  }

  /**
   * Get the iRODS base path.
   *
   * @return The iRODS base path.
   */
  public String getBasePath() {
    return basePath;
  }

  /**
   * Get the PAM authentication flag.
   *
   * @return The PAM authentication flag.
   */
  public Boolean getPamAuthentication() {
    return pamAuthentication;
  } 

  //---------------------------------------------------------------------//
  // Helper Methods
  //---------------------------------------------------------------------//

  /**
   * Instantiate an IRODSAccount from HpcIntegratedSystemAccount.
   *
   * @param dataManagementAccount The Data Management account.
   * @param authScheme The iRODS authentication scheme (PAM or STANDARD).
   * @return An iRODS account.
   * @throws HpcException on iRODS failure.
   */
  private IRODSAccount toUnauthenticatedIrodsAccount(
      HpcIntegratedSystemAccount dataManagementAccount, AuthScheme authScheme) throws HpcException {
    try {
      IRODSAccount irodsAccount =
          IRODSAccount.instance(
              irodsHost,
              irodsPort,
              dataManagementAccount.getUsername(),
              this.pamAuthentication ? dataManagementAccount.getPassword() : getUserKey(dataManagementAccount.getUsername()),
              "",
              irodsZone,
              irodsResource);
      irodsAccount.setAuthenticationScheme(authScheme);
      return irodsAccount;

    } catch (JargonException e) {
      throw new HpcException(
          "Failed instantiate an iRODS account: " + e.getMessage(),
          HpcErrorType.DATA_MANAGEMENT_ERROR,
          e);
    } catch (NoSuchAlgorithmException e) {
      throw new HpcException(
          "Failed instantiate an iRODS account: " + e.getMessage(),
          HpcErrorType.DATA_MANAGEMENT_ERROR,
          e);
    }
  }

  /**
   * Instantiate an IRODSAccount from HpcIntegratedSystemAccount.
   *
   * @param dataManagementAccount The Data Management account.
   * @param authScheme The iRODS authentication scheme (PAM or STANDARD).
   * @return An iRODS account.
   * @throws JargonException on iRODS failure.
   * @throws NoSuchAlgorithmException if algorithm is not supported.
   */
  private IRODSAccount toAuthenticatedIrodsAccount(
      HpcIntegratedSystemAccount dataManagementAccount, AuthScheme authScheme)
      throws JargonException, NoSuchAlgorithmException {
    Map<String, String> properties = new Hashtable<>();
    for (HpcIntegratedSystemAccountProperty property : dataManagementAccount.getProperties()) {
      properties.put(property.getName(), property.getValue());
    }

    return IRODSAccount.instance(
        properties.get(HOST_PROPERTY),
        Integer.valueOf(properties.get(PORT_PROPERTY)),
        dataManagementAccount.getUsername(),
        pamAuthentication ? dataManagementAccount.getPassword() : getUserKey(dataManagementAccount.getUsername()),
        properties.get(HOME_DIRECTORY_PROPERTY),
        properties.get(ZONE_PROPERTY),
        properties.get(DEFAULT_STORAGE_RESOURCE_PROPERTY),
        authScheme);
  }

  /**
   * Get iRODS Account instance from an authenticated token.
   *
   * @param authenticatedToken An authenticated token.
   * @return An authenticated iRODS account object.
   * @throws HpcException on iRODS failure.
   */
  private IRODSAccount getIrodsAccount(Object authenticatedToken) throws HpcException {
    if (authenticatedToken == null || !(authenticatedToken instanceof IRODSAccount)) {
      throw new HpcException(
          "Invalid iRODS authentication token", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    return (IRODSAccount) authenticatedToken;
  }

  /**
   * Get iRODS Account properties
   *
   * @param irodsAccount An iRODS account.
   * @return An list of account properties
   */
  private List<HpcIntegratedSystemAccountProperty> getIrodsAccountProperties(
      IRODSAccount irodsAccount) {
    List<HpcIntegratedSystemAccountProperty> properties = new ArrayList<>();

    HpcIntegratedSystemAccountProperty defaultStorageResource =
        new HpcIntegratedSystemAccountProperty();
    defaultStorageResource.setName(DEFAULT_STORAGE_RESOURCE_PROPERTY);
    defaultStorageResource.setValue(irodsAccount.getDefaultStorageResource());
    properties.add(defaultStorageResource);

    HpcIntegratedSystemAccountProperty homeDirectory = new HpcIntegratedSystemAccountProperty();
    homeDirectory.setName(HOME_DIRECTORY_PROPERTY);
    homeDirectory.setValue(irodsAccount.getHomeDirectory());
    properties.add(homeDirectory);

    HpcIntegratedSystemAccountProperty host = new HpcIntegratedSystemAccountProperty();
    host.setName(HOST_PROPERTY);
    host.setValue(irodsAccount.getHost());
    properties.add(host);

    HpcIntegratedSystemAccountProperty proxyName = new HpcIntegratedSystemAccountProperty();
    proxyName.setName(PROXY_NAME_PROPERTY);
    proxyName.setValue(irodsAccount.getProxyName());
    properties.add(proxyName);

    HpcIntegratedSystemAccountProperty proxyZone = new HpcIntegratedSystemAccountProperty();
    proxyZone.setName(PROXY_ZONE_PROPERTY);
    proxyZone.setValue(irodsAccount.getProxyZone());
    properties.add(proxyZone);

    HpcIntegratedSystemAccountProperty zone = new HpcIntegratedSystemAccountProperty();
    zone.setName(ZONE_PROPERTY);
    zone.setValue(irodsAccount.getZone());
    properties.add(zone);

    HpcIntegratedSystemAccountProperty port = new HpcIntegratedSystemAccountProperty();
    port.setName(PORT_PROPERTY);
    port.setValue(String.valueOf(irodsAccount.getPort()));
    properties.add(port);

    return properties;
  }
  
  /**
   * Get user key for Standard Authentication
   * 
   * @param user the username
   * @return user key
   * @throws NoSuchAlgorithmException if algorithm is not supported
   */
  String getUserKey(String user) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance(algorithm);
    digest.update(key.getBytes(StandardCharsets.UTF_8));
    byte[] userKey = digest.digest(user.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(userKey);
  }
  
}
