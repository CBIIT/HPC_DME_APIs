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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * HPC S3 Connection.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcS3Connection {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// TODO: cleanup - not used
	// 5GB in bytes
	// private static final long FIVE_GB = 5368709120L;

	// TODO: cleanup - not used
	// Google Storage S3 URL.
	// private static final String GOOGLE_STORAGE_URL =
	// "https://storage.googleapis.com";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// A list of S3 3rd party providers that require connection w/ path-style
	// enabled.
	private Set<HpcIntegratedSystem> pathStyleAccessEnabledProviders = new HashSet<>();

	// The multipart upload minimum part size.
	@Value("${hpc.integration.s3.minimumUploadPartSize}")
	private Long minimumUploadPartSize = null;

	// TODO: Cleanup - not used
	// The multipart upload threshold.
	// @Value("${hpc.integration.s3.multipartUploadThreshold}")
	// private Long multipartUploadThreshold = null;

	// TODO: Cleanup - not used
	// The socket timeout.
	// @Value("${hpc.integration.s3.socketTimeout}")
	// private Integer socketTimeout = null;

	// The executor service to be used by AWSTransferManager
	private ExecutorService executorService = null;

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 * @param pathStyleAccessEnabledProviders  A list of S3 3rd party providers that
	 *                                         require connection w/ path-style
	 *                                         enabled.
	 * @param awsTransferManagerThreadPoolSize The thread pool size to be used for
	 *                                         AWS transfer manager
	 */
	private HpcS3Connection(String pathStyleAccessEnabledProviders, int awsTransferManagerThreadPoolSize) {
		for (String s3Provider : pathStyleAccessEnabledProviders.split(",")) {
			this.pathStyleAccessEnabledProviders.add(HpcIntegratedSystem.fromValue(s3Provider));
		}

		// Instantiate the executor service for AWS transfer manager.
		executorService = Executors.newFixedThreadPool(awsTransferManagerThreadPoolSize,
				Executors.defaultThreadFactory());
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
	 * @param encryptionAlgorithm (Optional) The encryption algorithm.
	 * @param encryptionKey       (Optional) The encryption key.
	 * @return An authenticated TransferManager object, or null if authentication
	 *         failed.
	 * @throws HpcException if authentication failed
	 */
	public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount, String s3URLorRegion,
			String encryptionAlgorithm, String encryptionKey) throws HpcException {
		if (dataTransferAccount.getIntegratedSystem().equals(HpcIntegratedSystem.AWS)) {
			return authenticateAWS(dataTransferAccount.getUsername(), dataTransferAccount.getPassword(), s3URLorRegion,
					encryptionAlgorithm, encryptionKey);
		} else {
			// Determine if this S3 provider require path-style enabled.
			boolean pathStyleAccessEnabled = pathStyleAccessEnabledProviders
					.contains(dataTransferAccount.getIntegratedSystem());

			return authenticateS3Provider(dataTransferAccount.getUsername(), dataTransferAccount.getPassword(),
					s3URLorRegion, pathStyleAccessEnabled, dataTransferAccount.getIntegratedSystem(),
					encryptionAlgorithm, encryptionKey);
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
			return authenticateAWS(s3Account.getAccessKey(), s3Account.getSecretKey(), s3Account.getRegion(), null,
					null);
		} else {
			// Default S3 provider require path-style enabled to true if not provided by the
			// user.
			boolean pathStyleAccessEnabled = Optional.ofNullable(s3Account.getPathStyleAccessEnabled()).orElse(true);

			return authenticateS3Provider(s3Account.getAccessKey(), s3Account.getSecretKey(), s3Account.getUrl(),
					pathStyleAccessEnabled, HpcIntegratedSystem.USER_S_3_PROVIDER, null, null);
		}
	}

	/**
	 * Get S3 Transfer Manager from an authenticated token.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @return A transfer manager object.
	 * @throws HpcException on invalid authentication token.
	 */
	public S3TransferManager getTransferManager(Object authenticatedToken) throws HpcException {
		if (!(authenticatedToken instanceof HpcS3)) {
			throw new HpcException("Invalid S3 authentication token", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return ((HpcS3) authenticatedToken).transferManager;
	}

	/**
	 * Get S3 Client from an authenticated token.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @return A S3 client object.
	 * @throws HpcException on invalid authentication token.
	 */
	public S3AsyncClient getClient(Object authenticatedToken) throws HpcException {
		if (!(authenticatedToken instanceof HpcS3)) {
			throw new HpcException("Invalid S3 authentication token", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return ((HpcS3) authenticatedToken).client;
	}

	/**
	 * Get S3 Presigner from an authenticated token.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @return A S3 presigner object.
	 * @throws HpcException on invalid authentication token.
	 */
	public S3Presigner getPresigner(Object authenticatedToken) throws HpcException {
		if (!(authenticatedToken instanceof HpcS3)) {
			throw new HpcException("Invalid S3 authentication token", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return ((HpcS3) authenticatedToken).presigner;
	}

	/**
	 * Get S3 Provider from an authenticated token.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @return A transfer manager object.
	 * @throws HpcException on invalid authentication token.
	 */
	public HpcIntegratedSystem getS3Provider(Object authenticatedToken) throws HpcException {
		if (!(authenticatedToken instanceof HpcS3)) {
			return null;
		}

		return ((HpcS3) authenticatedToken).provider;
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	private class HpcS3 {
		private S3TransferManager transferManager = null;
		private S3AsyncClient client = null;
		private S3Presigner presigner = null;
		private HpcIntegratedSystem provider = null;
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
	 * @param encryptionAlgorithm    (Optional) The encryption algorithm.
	 * @param encryptionKey          (Optional) The encryption key.
	 * @return TransferManager
	 * @throws HpcException if authentication failed
	 */
	private Object authenticateS3Provider(String username, String password, String url, boolean pathStyleAccessEnabled,
			HpcIntegratedSystem s3Provider, String encryptionAlgorithm, String encryptionKey) throws HpcException {
		// Create the credential provider based on the configured credentials.
		AwsBasicCredentials s3ProviderCredentials = AwsBasicCredentials.create(username, password);
		StaticCredentialsProvider s3ProviderCredentialsProvider = StaticCredentialsProvider
				.create(s3ProviderCredentials);

		// Create URI to the S3 provider endpoint
		URI uri = null;
		try {
			uri = new URI(url);

		} catch (URISyntaxException e) {
			throw new HpcException("Invalid URL: " + url, HpcErrorType.DATA_TRANSFER_ERROR, e);
		}

		HpcS3 s3 = new HpcS3();
		s3.provider = s3Provider;

		// TODO: clean up the Client side encryption code. Not supported
		/*
		 * 
		 * ClientConfiguration config = new ClientConfiguration();
		 * config.setSocketTimeout(socketTimeout);
		 * 
		 * if (!StringUtils.isEmpty(encryptionAlgorithm) &&
		 * !StringUtils.isEmpty(encryptionKey)) { s3Client =
		 * AmazonS3EncryptionClientV2Builder.standard().withCryptoConfiguration(new
		 * CryptoConfigurationV2()
		 * .withCryptoMode(CryptoMode.AuthenticatedEncryption).withRangeGetMode(
		 * CryptoRangeGetMode.ALL)) .withEncryptionMaterialsProvider(new
		 * StaticEncryptionMaterialsProvider(new EncryptionMaterials( new
		 * SecretKeySpec(Base64.getDecoder().decode(encryptionKey),
		 * encryptionAlgorithm))))
		 * .withCredentials(s3ArchiveCredentialsProvider).withPathStyleAccessEnabled(
		 * pathStyleAccessEnabled)
		 * .withEndpointConfiguration(endpointConfiguration).withClientConfiguration(
		 * config).build(); } else {
		 */

		// TODO: clean up - Instantiate a S3 client. Using standard builder - note
		// different config options available, e.g no min-part-size
		// S3AsyncClient s3Client =
		// S3AsyncClient.builder().credentialsProvider(s3ArchiveCredentialsProvider)
		// .forcePathStyle(pathStyleAccessEnabled).endpointOverride(uri)
		// .overrideConfiguration(config ->
		// config.apiCallAttemptTimeout(Duration.ofMillis(socketTimeout)))
		// .build();

		// TODO: Review crt client builder config options -
		// https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/S3CrtAsyncClientBuilder.html#checksumValidationEnabled(java.lang.Boolean)

		try {
			// Instantiate a S3 async client.
			s3.client = S3AsyncClient.crtBuilder().credentialsProvider(s3ProviderCredentialsProvider)
					.forcePathStyle(pathStyleAccessEnabled).endpointOverride(uri)
					.minimumPartSizeInBytes(minimumUploadPartSize).checksumValidationEnabled(true)
					// TODO: remove checksumValidation as it is true by default
					/* TODO: consider using this config - .maxConcurrency(someValue) */
					.build(); //

			// Instantiate the S3 transfer manager.
			s3.transferManager = S3TransferManager.builder().s3Client(s3.client).executor(executorService).build();

			// Instantiate the S3 presigner.
			s3.presigner = S3Presigner.builder().credentialsProvider(s3ProviderCredentialsProvider)
					.endpointOverride(uri).serviceConfiguration(
							S3Configuration.builder().pathStyleAccessEnabled(pathStyleAccessEnabled).build())
					.build();

			// TODO: Note that since we can't set multipart upload threshold - GCP
			// storage/archive via S3 can no longer be supported. Need to clean it up

			/*
			 * TODO: These options from V1 don't have equivalent options in V2
			 * 
			 * Note that Google Storage doesn't support multipart upload, so we override the
			 * configured threshold w/ the max size of 5GB.
			 * 
			 * .withAlwaysCalculateMultipartMd5(true).withMultipartUploadThreshold(
			 * url.equalsIgnoreCase(GOOGLE_STORAGE_URL) ? FIVE_GB :
			 * multipartUploadThreshold)
			 * .withDisableParallelDownloads(true).withShutDownThreadPools(false).build();
			 */

			return s3;

		} catch (SdkException e) {
			throw new HpcException(
					"[S3] Failed to authenticate S3 Provider: " + s3Provider.value() + "] - " + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, e);
		}
	}

	/**
	 * Authenticate an AWS S3 account.
	 *
	 * @param accessKey           The AWS account access key.
	 * @param secretKey           The AWS account secret key.
	 * @param region              The AWS account region.
	 * @param encryptionAlgorithm (Optional) The encryption algorithm.
	 * @param encryptionKey       (Optional) The encryption key.
	 * @return TransferManager
	 * @throws HpcException if authentication failed
	 */
	private Object authenticateAWS(String accessKey, String secretKey, String region, String encryptionAlgorithm,
			String encryptionKey) throws HpcException {
		// Create the credential provider based on provided AWS S3 account.
		AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
		StaticCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(awsCredentials);

		HpcS3 s3 = new HpcS3();
		s3.provider = HpcIntegratedSystem.AWS;

		/*
		 * TODO: cleanup as client side encruption not available AmazonS3 s3Client =
		 * null; if (!StringUtils.isEmpty(encryptionAlgorithm) &&
		 * !StringUtils.isEmpty(encryptionKey)) { s3Client =
		 * AmazonS3EncryptionClientV2Builder.standard() .withCryptoConfiguration( new
		 * CryptoConfigurationV2().withCryptoMode(CryptoMode.AuthenticatedEncryption)
		 * .withRangeGetMode(CryptoRangeGetMode.ALL))
		 * .withEncryptionMaterialsProvider(new StaticEncryptionMaterialsProvider(new
		 * EncryptionMaterials( new
		 * SecretKeySpec(Base64.getDecoder().decode(encryptionKey),
		 * encryptionAlgorithm))))
		 * .withRegion(region).withCredentials(awsCredentialsProvider).build(); } else {
		 */

		try {
			// Instantiate a S3 async client.
			s3.client = S3AsyncClient.crtBuilder().credentialsProvider(awsCredentialsProvider).region(Region.of(region))
					.minimumPartSizeInBytes(minimumUploadPartSize).checksumValidationEnabled(true)
					// TODO: remove checksumValidation as it is true by default
					// TODO: consider using this config - .maxConcurrency(someValue)
					.build(); //

			// Instantiate the S3 transfer manager.
			s3.transferManager = S3TransferManager.builder().s3Client(s3.client).executor(executorService).build();

			/*
			 * TODO: These options from V1 don't have equivalent options in V2
			 * .withAlwaysCalculateMultipartMd5(true).
			 * .withMultipartUploadThreshold(multipartUploadThreshold).
			 * withDisableParallelDownloads(true) .withShutDownThreadPools(false).build();
			 */

			// Instantiate the S3 presigner.
			s3.presigner = S3Presigner.builder().credentialsProvider(awsCredentialsProvider).region(Region.of(region))
					.build();

			return s3;
			/*
			 * } catch (SdkClientException e) { throw new
			 * HpcException("Failed to authenticate S3 account in region [" + region +
			 * "] - " + e.getMessage(), HpcErrorType.INVALID_REQUEST_INPUT, e); }
			 */

		} catch (SdkException e) {
			throw new HpcException("[S3] Failed to authenticate S3 in region " + region + "] - " + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, e);
		}
	}
}
