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

import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3EncryptionClientV2Builder;
import com.amazonaws.services.s3.model.CryptoConfigurationV2;
import com.amazonaws.services.s3.model.CryptoMode;
import com.amazonaws.services.s3.model.CryptoRangeGetMode;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.StaticEncryptionMaterialsProvider;
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
	// Constants
	// ---------------------------------------------------------------------//

	// 5GB in bytes
	private static final long FIVE_GB = 5368709120L;

	// Google Storage S3 URL.
	private static final String GOOGLE_STORAGE_URL = "https://storage.googleapis.com";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// A list of S3 3rd party providers that require connection w/ path-style
	// enabled.
	private Set<HpcIntegratedSystem> pathStyleAccessEnabledProviders = new HashSet<>();

	// The multipart upload minimum part size.
	@Value("${hpc.integration.s3.minimumUploadPartSize}")
	private Long minimumUploadPartSize = null;

	// The multipart upload threshold.
	@Value("${hpc.integration.s3.multipartUploadThreshold}")
	private Long multipartUploadThreshold = null;

	// The Logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

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
	 * Authenticate a (system) data transfer account to S3 (AWS or 3rd Party
	 * Provider)
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
			return authenticateAWS(dataTransferAccount.getUsername(), dataTransferAccount.getPassword(), s3URLorRegion);
		} else {
			// Determine if this S3 provider require path-style enabled.
			boolean pathStyleAccessEnabled = pathStyleAccessEnabledProviders
					.contains(dataTransferAccount.getIntegratedSystem());

			return authenticateS3Provider(dataTransferAccount.getUsername(), dataTransferAccount.getPassword(),
					s3URLorRegion, pathStyleAccessEnabled, dataTransferAccount.getIntegratedSystem());
		}
	}

	/**
	 * Authenticate a (user) S3 account (AWS or 3rd Party Provider)
	 *
	 * @param s3Account AWS S3 account.
	 * @return TransferManager
	 * @throws HpcException if authentication failed
	 */
	public Object authenticate(HpcS3Account s3Account) throws HpcException {
		if (!StringUtils.isEmpty(s3Account.getRegion())) {
			return authenticateAWS(s3Account.getAccessKey(), s3Account.getSecretKey(), s3Account.getRegion());
		} else {
			// Default S3 provider require path-style enabled to true if not provided by the
			// user.
			boolean pathStyleAccessEnabled = Optional.ofNullable(s3Account.getPathStyleAccessEnabled()).orElse(true);

			return authenticateS3Provider(s3Account.getAccessKey(), s3Account.getSecretKey(), s3Account.getUrl(),
					pathStyleAccessEnabled, HpcIntegratedSystem.USER_S_3_PROVIDER);
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

	/**
	 * Authenticate a 'S3 3rd Party Provider' account.
	 *
	 * @param username               The S3 account user name.
	 * @param password               The S3 account password.
	 * @param url                    The S3 3rd party provider URL.
	 * @param pathStyleAccessEnabled true if the S3 3rd Party provider supports path
	 *                               style access.
	 * @param s3Provider             The 3rd party provider.
	 * @return TransferManager
	 * @throws HpcException if authentication failed
	 */
	private Object authenticateS3Provider(String username, String password, String url, boolean pathStyleAccessEnabled,
			HpcIntegratedSystem s3Provider) throws HpcException {
		// Create the credential provider based on the configured credentials.
		BasicAWSCredentials s3ArchiveCredentials = new BasicAWSCredentials(username, password);
		AWSStaticCredentialsProvider s3ArchiveCredentialsProvider = new AWSStaticCredentialsProvider(
				s3ArchiveCredentials);

		// Setup the endpoint configuration.
		EndpointConfiguration endpointConfiguration = new EndpointConfiguration(url, null);

		SecretKey secretKey = new SecretKeySpec(
				Base64.getDecoder().decode("eZaQqQPhsUUpawan5W4TjRPhLbq95dMzJ6pHg3M09p8="), "AES");

		AmazonS3 s3EncryptionClient = AmazonS3EncryptionClientV2Builder.standard()
				.withCryptoConfiguration(new CryptoConfigurationV2().withCryptoMode(CryptoMode.AuthenticatedEncryption)
						.withRangeGetMode(CryptoRangeGetMode.ALL))
				.withEncryptionMaterialsProvider(
						new StaticEncryptionMaterialsProvider(new EncryptionMaterials(secretKey)))
				.withCredentials(s3ArchiveCredentialsProvider).withPathStyleAccessEnabled(pathStyleAccessEnabled)
				.withEndpointConfiguration(endpointConfiguration).build();

		AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(s3ArchiveCredentialsProvider)
				.withPathStyleAccessEnabled(pathStyleAccessEnabled).withEndpointConfiguration(endpointConfiguration)
				.build();

		// Create and return the S3 transfer manager. Note that Google Storage doesn't
		// support multipart upload,
		// so we override the configured threshold w/ the max size of 5GB.
		HpcS3TransferManager s3TransferManager = new HpcS3TransferManager();
		s3TransferManager.transferManager = TransferManagerBuilder.standard().withS3Client(s3EncryptionClient)
				.withMinimumUploadPartSize(minimumUploadPartSize).withMultipartUploadThreshold(
						url.equalsIgnoreCase(GOOGLE_STORAGE_URL) ? FIVE_GB : multipartUploadThreshold)
				.build();
		s3TransferManager.s3Provider = s3Provider;
		return s3TransferManager;
	}

	/**
	 * Authenticate an AWS S3 account.
	 *
	 * @param accessKey The AWS account access key.
	 * @param secretKey The AWS account secret key.
	 * @param region    The AWS account region.
	 * @return TransferManager
	 * @throws HpcException if authentication failed
	 */
	private Object authenticateAWS(String accessKey, String secretKey, String region) throws HpcException {
		try {
			// Create the credential provider based on provided S3 account.
			BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
			AWSStaticCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);

			// Create and return the S3 transfer manager.
			HpcS3TransferManager s3TransferManager = new HpcS3TransferManager();
			s3TransferManager.transferManager = TransferManagerBuilder.standard().withS3Client(
					AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(awsCredentialsProvider).build())
					.build();
			s3TransferManager.s3Provider = HpcIntegratedSystem.AWS;
			return s3TransferManager;

		} catch (SdkClientException e) {
			throw new HpcException("Failed to authenticate S3 account in region [" + region + "] - " + e.getMessage(),
					HpcErrorType.INVALID_REQUEST_INPUT, e);
		}
	}
}
