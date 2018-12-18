/**
 * HpcS3Connection.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration.s3.impl;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadAccount;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC S3 Connection.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcS3Connection {
  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcS3Connection() {}

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  /**
   * Authenticate an account.
   *
   * @param dataTransferAccount A data transfer account to authenticate.
   * @param s3URL The S3 URL to connect to (This is Cleversafe URL).
   * @return An authenticated TransferManager object, or null if authentication failed.
   */
  public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount, String s3URL) {
    // Create the credential provider based on the configured credentials.
    BasicAWSCredentials cleversafeCredentials =
        new BasicAWSCredentials(
            dataTransferAccount.getUsername(), dataTransferAccount.getPassword());
    AWSStaticCredentialsProvider cleversafeCredentialsProvider =
        new AWSStaticCredentialsProvider(cleversafeCredentials);

    // Setup the endpoint configuration.
    EndpointConfiguration endpointConfiguration = new EndpointConfiguration(s3URL, null);

    // Create and return the transfer manager.
    return TransferManagerBuilder.standard()
        .withS3Client(
            AmazonS3ClientBuilder.standard()
                .withCredentials(cleversafeCredentialsProvider)
                .withEndpointConfiguration(endpointConfiguration)
                .build())
        .build();
  }

  /**
   * Authenticate an account.
   *
   * @param s3Account AWS S3 account.
   * @return TransferManager
   * @throws HpcException if authentication failed
   */
  public Object authenticate(HpcS3DownloadAccount s3Account) throws HpcException {
    try {
      // Create the credential provider based on provided S3 account.
      BasicAWSCredentials awsCredentials =
          new BasicAWSCredentials(s3Account.getAccessKey(), s3Account.getSecretKey());
      AWSStaticCredentialsProvider awsCredentialsProvider =
          new AWSStaticCredentialsProvider(awsCredentials);

      // Create and return the transfer manager.
      return TransferManagerBuilder.standard()
          .withS3Client(
              AmazonS3ClientBuilder.standard()
                  .withRegion(s3Account.getRegion())
                  .withCredentials(awsCredentialsProvider)
                  .build())
          .build();

    } catch (SdkClientException e) {
      throw new HpcException(
          "Failed to authenticate S3 account in region ["
              + s3Account.getRegion()
              + "] - "
              + e.getMessage(),
          HpcErrorType.INVALID_REQUEST_INPUT,
          e);
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
