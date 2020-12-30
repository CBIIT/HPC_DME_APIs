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

import org.springframework.beans.factory.annotation.Value;

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

	// A list of S3 3rd party providers that require connection w/ path-style
	// enabled.
	private Set<HpcIntegratedSystem> pathStyleAccessEnabledProviders = new HashSet<>();

	// The multipart upload minimum part size.
	@Value("${hpc.integration.s3.minimumUploadPartSize}")
	Long minimumUploadPartSize = null;

	// The multipart upload threshold.
	@Value("${hpc.integration.s3.multipartUploadThreshold}")
	Long multipartUploadThreshold = null;

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 * @param pathStyleAccessEnabledProviders A list of S3 3rd party providers that
	 *                                        require connection w/ path-style
	 *                                        enabled.
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
	 * @param s3URLorRegion       The S3 URL if authenticating with a 3rd party S3
	 *                            Provider (Cleversafe, Cloudian, etc), or Region if
	 *                            authenticating w/ AWS.
	 * @return An authenticated TransferManager object, or null if authentication
	 *         failed.
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
		BasicAWSCredentials s3ArchiveCredentials = new BasicAWSCredentials(dataTransferAccount.getUsername(),
				dataTransferAccount.getPassword());
		AWSStaticCredentialsProvider s3ArchiveCredentialsProvider = new AWSStaticCredentialsProvider(
				s3ArchiveCredentials);

		// Setup the endpoint configuration.
		EndpointConfiguration endpointConfiguration = new EndpointConfiguration(s3URLorRegion, null);

		// Determine if this S3 provider require path-style enabled.
		boolean pathStyleEnabled = pathStyleAccessEnabledProviders.contains(dataTransferAccount.getIntegratedSystem());

		// Create and return the S3 transfer manager.
		HpcS3TransferManager s3TransferManager = new HpcS3TransferManager();
		s3TransferManager.transferManager = TransferManagerBuilder.standard()
				.withS3Client(AmazonS3ClientBuilder.standard().withCredentials(s3ArchiveCredentialsProvider)
						.withPathStyleAccessEnabled(pathStyleEnabled).withEndpointConfiguration(endpointConfiguration)
						.build())
				.withMinimumUploadPartSize(minimumUploadPartSize).withMultipartUploadThreshold(multipartUploadThreshold)
				.build();
		s3TransferManager.s3Provider = dataTransferAccount.getIntegratedSystem();
		return s3TransferManager;
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
			BasicAWSCredentials awsCredentials = new BasicAWSCredentials(s3Account.getAccessKey(),
					s3Account.getSecretKey());
			AWSStaticCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);

			// Create and return the S3 transfer manager.
			HpcS3TransferManager s3TransferManager = new HpcS3TransferManager();
			s3TransferManager.transferManager = TransferManagerBuilder.standard().withS3Client(AmazonS3ClientBuilder
					.standard().withRegion(s3Account.getRegion()).withCredentials(awsCredentialsProvider).build())
					.build();
			s3TransferManager.s3Provider = HpcIntegratedSystem.AWS;
			return s3TransferManager;

		} catch (SdkClientException e) {
			throw new HpcException(
					"Failed to authenticate S3 account in region [" + s3Account.getRegion() + "] - " + e.getMessage(),
					HpcErrorType.INVALID_REQUEST_INPUT, e);
		}
	}

	/**
	 * Get S3 Transfer Manager from an authenticated token.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @return A transfer manager object.
	 * @throws HpcException on invalid authentication token.
	 */
	public TransferManager getTransferManager(Object authenticatedToken) throws HpcException {
		if (authenticatedToken == null || !(authenticatedToken instanceof HpcS3TransferManager)) {
			throw new HpcException("Invalid S3 authentication token", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return ((HpcS3TransferManager) authenticatedToken).transferManager;
	}

	/**
	 * Get S3 Provider from an authenticated token.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @return A transfer manager object.
	 * @throws HpcException on invalid authentication token.
	 */
	public HpcIntegratedSystem getS3Provider(Object authenticatedToken) throws HpcException {
		if (authenticatedToken == null || !(authenticatedToken instanceof HpcS3TransferManager)) {
			return null;
		}

		return ((HpcS3TransferManager) authenticatedToken).s3Provider;
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	private class HpcS3TransferManager {
		private TransferManager transferManager = null;
		private HpcIntegratedSystem s3Provider = null;
	}
}
