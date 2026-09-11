/**
 * HpcS3ConnectionNettyNioAsyncClient.java
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
import java.time.Duration;
import java.util.function.Consumer;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;

import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * HPC S3 Connection - Netty-NIO based implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcS3ConnectionNettyNioAsyncClient extends HpcS3Connection implements DisposableBean {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The maximum number of concurrent S3 requests, i.e. the size of the HTTP
	// connection pool. The AWS SDK default is 50.
	@Value("${hpc.integration.s3.maxConnections}")
	private Integer maxConnections = null;

	// The maximum number of multipart parts in flight per transfer. Bounded below the
	// AWS SDK default of 50, which on its own can consume the entire HTTP connection
	// pool and starve every other transfer sharing it. Keep it at or below
	// maxConnections divided by the number of concurrent transfers
	// (hpc.integration.s3.executorThreadPoolSize), leaving headroom for the requests
	// that are not part uploads/downloads.
	@Value("${hpc.integration.s3.nettynio.maxInFlightParts:8}")
	private Integer maxInFlightParts = null;

	// The time to wait for a connection from the pool before failing the request, in
	// milliseconds. Raised from the AWS SDK default of 10 seconds, which a request
	// queued behind an in flight part is likely to exceed - waiting for the pool is
	// preferable to failing the transfer.
	@Value("${hpc.integration.s3.nettynio.connectionAcquisitionTimeout:60000}")
	private Long connectionAcquisitionTimeout = null;

	// The socket timeout - the max time to wait for data to be transferred over an
	// established, open connection, in milliseconds. Maps to the Netty client's read
	// and write timeouts. Shares the property with the v1 client since the behavior
	// is the same.
	@Value("${hpc.integration.s3.socketTimeout}")
	private Integer socketTimeout = null;

	// The TCP keep alive setting. Shares the property with the v1 client since the
	// behavior is the same.
	@Value("${hpc.integration.s3.useTcpKeepAlive}")
	private Boolean useTcpKeepAlive = false;

	// A single Netty-NIO HTTP client, shared by all the S3 async clients created by
	// this connection. Netty allocates an event loop group and a connection pool per
	// HTTP client instance, so a client per S3 connection would accumulate threads.
	// Note that the AWS SDK treats an explicitly provided HTTP client as
	// 'non-managed', i.e. it does not close it when the S3 client is closed, so this
	// client remains usable for the lifetime of this connection and is closed on
	// bean destruction.
	private volatile SdkAsyncHttpClient httpClient = null;

	// The lock guarding the lazy instantiation of the shared HTTP client. The
	// client can't be instantiated by the constructor because it is configured w/
	// property values that are injected after construction.
	private final Object httpClientLock = new Object();

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
	HpcS3ConnectionNettyNioAsyncClient(String pathStyleAccessEnabledProviders, int awsTransferManagerThreadPoolSize) {
		super(pathStyleAccessEnabledProviders, awsTransferManagerThreadPoolSize);
	}

	// ---------------------------------------------------------------------//
	// HpcS3Connection Abstract Class Implementation
	// ---------------------------------------------------------------------//

	@Override
	protected S3AsyncClient buildS3ProviderAsyncClient(StaticCredentialsProvider credentialsProvider, URI endpoint,
			boolean pathStyleAccessEnabled, long thresholdInBytes) {
		// Instantiate a S3 async client (Netty-NIO based) w/ multipart enabled.
		return S3AsyncClient.builder().credentialsProvider(credentialsProvider).forcePathStyle(pathStyleAccessEnabled)
				.endpointOverride(endpoint).multipartEnabled(true)
				.multipartConfiguration(multipartConfiguration(thresholdInBytes))
				.overrideConfiguration(overrideConfiguration -> overrideConfiguration.retryPolicy(retryPolicy()))
				.httpClient(getNettyNioHttpClient()).build();
	}

	@Override
	protected S3AsyncClient buildAwsAsyncClient(StaticCredentialsProvider credentialsProvider, String region) {
		// Instantiate a S3 async client (Netty-NIO based) w/ multipart enabled.
		return S3AsyncClient.builder().credentialsProvider(credentialsProvider).region(Region.of(region))
				.multipartEnabled(true).multipartConfiguration(multipartConfiguration(multipartUploadThreshold))
				.overrideConfiguration(overrideConfiguration -> overrideConfiguration.retryPolicy(retryPolicy()))
				.httpClient(getNettyNioHttpClient()).build();
	}

	// ---------------------------------------------------------------------//
	// DisposableBean Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void destroy() {
		synchronized (httpClientLock) {
			if (httpClient != null) {
				logger.info("Closing the shared Netty-NIO HTTP client");
				httpClient.close();
				httpClient = null;
			}
		}
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Get the multipart configuration to be applied to a S3 async client.
	 *
	 * @param thresholdInBytes The multipart upload threshold in bytes.
	 * @return A multipart configuration consumer.
	 */
	private Consumer<MultipartConfiguration.Builder> multipartConfiguration(long thresholdInBytes) {
		return builder -> builder.minimumPartSizeInBytes(minimumUploadPartSize).thresholdInBytes(thresholdInBytes)
				.parallelConfiguration(
						parallelConfigurationBuilder -> parallelConfigurationBuilder.maxInFlightParts(maxInFlightParts));
	}

	/**
	 * Get the retry policy to be applied to a S3 async client. Preserves the AWS SDK
	 * default retry conditions and backoff, overriding only the number of retries.
	 *
	 * @return A retry policy.
	 */
	private RetryPolicy retryPolicy() {
		return RetryPolicy.defaultRetryPolicy().toBuilder().numRetries(maxErrorRetries).build();
	}

	/**
	 * Get the shared Netty-NIO based async HTTP client, instantiating it on first
	 * use.
	 *
	 * @return A Netty-NIO async HTTP client.
	 */
	private SdkAsyncHttpClient getNettyNioHttpClient() {
		SdkAsyncHttpClient nettyNioHttpClient = httpClient;
		if (nettyNioHttpClient == null) {
			synchronized (httpClientLock) {
				nettyNioHttpClient = httpClient;
				if (nettyNioHttpClient == null) {
					nettyNioHttpClient = buildNettyNioHttpClient();
					httpClient = nettyNioHttpClient;
				}
			}
		}

		return nettyNioHttpClient;
	}

	/**
	 * Build a Netty-NIO based async HTTP client, honoring the disableCertChecking
	 * config.
	 *
	 * @return A Netty-NIO async HTTP client.
	 */
	private SdkAsyncHttpClient buildNettyNioHttpClient() {
		// Note that the options set on the builder take precedence over the defaults
		// provided to buildWithDefaults(), so the disableCertChecking path below keeps
		// them.
		NettyNioAsyncHttpClient.Builder nettyNioHttpClientBuilder = NettyNioAsyncHttpClient.builder()
				.maxConcurrency(maxConnections)
				.connectionAcquisitionTimeout(Duration.ofMillis(connectionAcquisitionTimeout))
				.connectionTimeout(Duration.ofMillis(connectionTimeout))
				.readTimeout(Duration.ofMillis(socketTimeout)).writeTimeout(Duration.ofMillis(socketTimeout))
				.tcpKeepAlive(useTcpKeepAlive);

		logger.info(
				"Building the shared Netty-NIO HTTP client. maxConcurrency: {}, connectionAcquisitionTimeout: {}ms, "
						+ "connectionTimeout: {}ms, socketTimeout(read/write): {}ms, tcpKeepAlive: {}",
				maxConnections, connectionAcquisitionTimeout, connectionTimeout, socketTimeout, useTcpKeepAlive);

		if (Boolean.TRUE.equals(disableCertChecking)) {
			logger.warn(
					"hpc.integration.s3.disableCertChecking property is set to true. Netty-NIO cert validation is off");
			return nettyNioHttpClientBuilder.buildWithDefaults(
					AttributeMap.builder().put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true).build());
		}

		return nettyNioHttpClientBuilder.build();
	}
}
