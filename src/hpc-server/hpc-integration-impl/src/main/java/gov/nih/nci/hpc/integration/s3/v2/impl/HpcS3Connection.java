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
package gov.nih.nci.hpc.integration.s3.v2.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * HPC S3 Connection base class. Holds all logic shared between the concrete S3
 * connection implementations. The only behavior that differs between
 * implementations is how the {@link S3AsyncClient} is built (e.g. AWS CRT vs
 * Netty-NIO), which is delegated to the abstract build methods.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public abstract class HpcS3Connection {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// 5GB in bytes
	protected static final long FIVE_GB = 5368709120L;

	// Google Storage S3 URL.
	protected static final String GOOGLE_STORAGE_URL = "https://storage.googleapis.com";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// A list of S3 3rd party providers that require connection w/ path-style
	// enabled.
	private Set<HpcIntegratedSystem> pathStyleAccessEnabledProviders = new HashSet<>();

	// The multipart upload minimum part size.
	@Value("${hpc.integration.s3.minimumUploadPartSize}")
	protected Long minimumUploadPartSize = null;

	// The multipart upload threshold.
	@Value("${hpc.integration.s3.multipartUploadThreshold}")
	protected Long multipartUploadThreshold = null;

	// The connection timeout - the time to wait to establish a new connection, in
	// milliseconds. Same behavior across all S3 client implementations, so the
	// property is shared.
	@Value("${hpc.integration.s3.connectionTimeout}")
	protected Integer connectionTimeout = null;

	// The max number of error retries. Same behavior across all S3 client
	// implementations, so the property is shared.
	@Value("${hpc.integration.s3.maxErrorRetries}")
	protected Integer maxErrorRetries = null;

	// Disable SSL certificate checking (for development/testing only). Shares the
	// property with the v1 client since the behavior is the same.
	@Value("${hpc.integration.s3.disableCertChecking:false}")
	protected Boolean disableCertChecking = null;

	// The executor service to be used by AWSTransferManager
	private ExecutorService executorService = null;

	// The logger instance.
	protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

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
	protected HpcS3Connection(String pathStyleAccessEnabledProviders, int awsTransferManagerThreadPoolSize) {
		for (String s3Provider : pathStyleAccessEnabledProviders.split(",")) {
			this.pathStyleAccessEnabledProviders.add(HpcIntegratedSystem.fromValue(s3Provider));
		}

		// Instantiate the executor service for AWS transfer manager.
		executorService = Executors.newFixedThreadPool(awsTransferManagerThreadPoolSize,
				Executors.defaultThreadFactory());
	}

	// ---------------------------------------------------------------------//
	// Abstract Methods
	// ---------------------------------------------------------------------//

	/**
	 * Build a S3 async client for a 'S3 3rd Party Provider' account.
	 *
	 * @param credentialsProvider    The S3 credentials provider.
	 * @param endpoint               The S3 provider endpoint.
	 * @param pathStyleAccessEnabled true if the S3 3rd Party provider supports path
	 *                               style access.
	 * @param thresholdInBytes       The multipart upload threshold in bytes.
	 * @return A S3 async client.
	 */
	protected abstract S3AsyncClient buildS3ProviderAsyncClient(StaticCredentialsProvider credentialsProvider,
			URI endpoint, boolean pathStyleAccessEnabled, long thresholdInBytes);

	/**
	 * Build a S3 async client for an AWS S3 account.
	 *
	 * @param credentialsProvider The AWS credentials provider.
	 * @param region              The AWS account region.
	 * @return A S3 async client.
	 */
	protected abstract S3AsyncClient buildAwsAsyncClient(StaticCredentialsProvider credentialsProvider, String region);

	// ---------------------------------------------------------------------//
	// Public Methods
	// ---------------------------------------------------------------------//

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

	public S3TransferManager getTransferManager(Object authenticatedToken) throws HpcException {
		if (!(authenticatedToken instanceof HpcS3)) {
			throw new HpcException("Invalid S3 authentication token", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return ((HpcS3) authenticatedToken).transferManager;
	}

	public S3AsyncClient getClient(Object authenticatedToken) throws HpcException {
		if (!(authenticatedToken instanceof HpcS3)) {
			throw new HpcException("Invalid S3 authentication token", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return ((HpcS3) authenticatedToken).client;
	}

	public S3Presigner getPresigner(Object authenticatedToken) throws HpcException {
		if (!(authenticatedToken instanceof HpcS3)) {
			throw new HpcException("Invalid S3 authentication token", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return ((HpcS3) authenticatedToken).presigner;
	}

	public HpcIntegratedSystem getS3Provider(Object authenticatedToken) throws HpcException {
		if (!(authenticatedToken instanceof HpcS3)) {
			return null;
		}

		return ((HpcS3) authenticatedToken).provider;
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	protected class HpcS3 {
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
	 * @return HpcS3 instance
	 * @throws HpcException if authentication failed
	 */
	private Object authenticateS3Provider(String username, String password, String url, boolean pathStyleAccessEnabled,
			HpcIntegratedSystem s3Provider) throws HpcException {
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

		long thresholdInBytes = url.equalsIgnoreCase(GOOGLE_STORAGE_URL) ? FIVE_GB : multipartUploadThreshold;

		try {
			// Instantiate a S3 async client (implementation specific).
			s3.client = buildS3ProviderAsyncClient(s3ProviderCredentialsProvider, uri, pathStyleAccessEnabled,
					thresholdInBytes);

			// Instantiate the S3 transfer manager.
			s3.transferManager = S3TransferManager.builder().s3Client(s3.client).executor(executorService).build();

			// Instantiate the S3 presigner.
			s3.presigner = S3Presigner.builder().credentialsProvider(s3ProviderCredentialsProvider)
					.endpointOverride(uri).serviceConfiguration(
							S3Configuration.builder().pathStyleAccessEnabled(pathStyleAccessEnabled).build())
					.build();

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
	 * @param accessKey The AWS account access key.
	 * @param secretKey The AWS account secret key.
	 * @param region    The AWS account region.
	 * @return TransferManager
	 * @throws HpcException if authentication failed
	 */
	private Object authenticateAWS(String accessKey, String secretKey, String region) throws HpcException {
		// Create the credential provider based on provided AWS S3 account.
		AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
		StaticCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(awsCredentials);

		HpcS3 s3 = new HpcS3();
		s3.provider = HpcIntegratedSystem.AWS;

		try {
			// Instantiate a S3 async client (implementation specific).
			s3.client = buildAwsAsyncClient(awsCredentialsProvider, region);

			// Instantiate the S3 transfer manager.
			s3.transferManager = S3TransferManager.builder().s3Client(s3.client).executor(executorService).build();

			// Instantiate the S3 presigner.
			s3.presigner = S3Presigner.builder().credentialsProvider(awsCredentialsProvider).region(Region.of(region))
					.build();

			return s3;

		} catch (SdkException e) {
			throw new HpcException("[S3] Failed to authenticate S3 in region " + region + "] - " + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, e);
		}
	}
}
