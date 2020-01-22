/**
 * HpcS3Connection.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration.s3.impl;

import java.util.HashSet;
import java.util.Set;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC S3 Connection.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcS3Connection {
  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // A list of S3 3rd party providers that require connection w/ path-style enabled.
  private Set<HpcIntegratedSystem> pathStyleAccessEnabledProviders = new HashSet<>();

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /**
   * Constructor for Spring Dependency Injection.
   * 
   * @param pathStyleAccessEnabledProviders A list of S3 3rd party providers that require connection
   *        w/ path-style enabled.
   */
  private HpcS3Connection(String pathStyleAccessEnabledProviders) {
    for (String s3Provider : pathStyleAccessEnabledProviders.split(",")) {
      this.pathStyleAccessEnabledProviders.add(HpcIntegratedSystem.fromValue(s3Provider));
    }
  }

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  /**
   * Authenticate an account
   *
   * @param dataTransferAccount A data transfer account to authenticate.
   * @param s3URLorRegion The S3 URL if authenticating with a 3rd party S3 Provider (Cleversafe,
   *        Cloudian, etc), or Region if authenticating w/ AWS.
   * @return An authenticated TransferManager object, or null if authentication failed.
   * @throws HpcException if authentication failed
   */
  public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount, String s3URLorRegion)
      throws HpcException {
    if (dataTransferAccount.getIntegratedSystem().equals(HpcIntegratedSystem.AWS)) {
      // AWS authentication required. Invoke the overloaded method w/ S3 account
      HpcS3Account s3Account = new HpcS3Account();
      s3Account.setAccessKey(dataTransferAccount.getUsername());
      s3Account.setSecretKey(dataTransferAccount.getPassword());
      s3Account.setRegion(s3URLorRegion);
      return authenticate(s3Account);
    }

    // 3rd Party S3 Provider authentication.

    // Create the credential provider based on the configured credentials.
    BasicAWSCredentials cleversafeCredentials = new BasicAWSCredentials(
        dataTransferAccount.getUsername(), dataTransferAccount.getPassword());
    AWSStaticCredentialsProvider cleversafeCredentialsProvider =
        new AWSStaticCredentialsProvider(cleversafeCredentials);

    // Setup the endpoint configuration.
    EndpointConfiguration endpointConfiguration = new EndpointConfiguration(s3URLorRegion, null);

    // Determine if this S3 provider require path-style enabled.
    boolean pathStyleEnabled =
        pathStyleAccessEnabledProviders.contains(dataTransferAccount.getIntegratedSystem());

    // Create and return the transfer manager.
    return TransferManagerBuilder.standard().withS3Client(AmazonS3ClientBuilder.standard()
        .withCredentials(cleversafeCredentialsProvider).withPathStyleAccessEnabled(pathStyleEnabled)
        .withEndpointConfiguration(endpointConfiguration).build()).build();
  }

  /**
   * Authenticate an AWS S3 account.
   *
   * @param s3Account AWS S3 account.
   * @return TransferManager
   * @throws HpcException if authentication failed
   */
  public Object authenticate(HpcS3Account s3Account) throws HpcException {
    try {
      // Create the credential provider based on provided S3 account.
      BasicAWSCredentials awsCredentials =
          new BasicAWSCredentials(s3Account.getAccessKey(), s3Account.getSecretKey());
      AWSStaticCredentialsProvider awsCredentialsProvider =
          new AWSStaticCredentialsProvider(awsCredentials);

      // Create and return the transfer manager.
      return TransferManagerBuilder
          .standard().withS3Client(AmazonS3ClientBuilder.standard()
              .withRegion(s3Account.getRegion()).withCredentials(awsCredentialsProvider).build())
          .build();

    } catch (SdkClientException e) {
      throw new HpcException("Failed to authenticate S3 account in region [" + s3Account.getRegion()
          + "] - " + e.getMessage(), HpcErrorType.INVALID_REQUEST_INPUT, e);
    }
  }

  /**
   * Get S3 Transfer Manager an authenticated token.
   *
   * @param authenticatedToken An authenticated token.
   * @return A transfer manager object.
   * @throws HpcException on invalid authentication token.
   */
  public TransferManager getTransferManager(Object authenticatedToken) throws HpcException {
    if (authenticatedToken == null || !(authenticatedToken instanceof TransferManager)) {
      throw new HpcException("Invalid S3 authentication token", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    return (TransferManager) authenticatedToken;
  }
}
