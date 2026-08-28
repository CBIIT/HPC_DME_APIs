/**
 * HpcS3ConnectionCrtAsyncClient.java
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
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;

/**
 * HPC S3 Connection - AWS CRT based implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcS3ConnectionCrtAsyncClient extends HpcS3Connection {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// The CRT log level to fall back to if the configured value is invalid.
	private static final Log.LogLevel DEFAULT_CRT_LOG_LEVEL = Log.LogLevel.Info;

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The CRT log file (Optional).
	@Value("${hpc.integration.s3.crtLogFile:#{null}}")
	private String crtLogFile = null;

	// The CRT log level (used when crtLogFile is configured).
	@Value("${hpc.integration.s3.crtLogLevel:Info}")
	private String crtLogLevel = null;

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
	HpcS3ConnectionCrtAsyncClient(String pathStyleAccessEnabledProviders, int awsTransferManagerThreadPoolSize) {
		super(pathStyleAccessEnabledProviders, awsTransferManagerThreadPoolSize);
	}

	// ---------------------------------------------------------------------//
	// HpcS3Connection Abstract Class Implementation
	// ---------------------------------------------------------------------//

	@Override
	protected S3AsyncClient buildS3ProviderAsyncClient(StaticCredentialsProvider credentialsProvider, URI endpoint,
			boolean pathStyleAccessEnabled, long thresholdInBytes) {
		try {
			// If configured, start the AWS CRT logger.
			initCrtLogging();

			// Instantiate a S3 async client.
			S3CrtAsyncClientBuilder crtAsyncClientBuilder = S3AsyncClient.crtBuilder()
					.credentialsProvider(credentialsProvider).forcePathStyle(pathStyleAccessEnabled)
					.endpointOverride(endpoint).minimumPartSizeInBytes(minimumUploadPartSize)
					.thresholdInBytes(thresholdInBytes);

			if (trustAllCerts) {
				crtAsyncClientBuilder.httpConfiguration(builder -> builder.trustAllCertificatesEnabled(true));
				logger.warn("hpc.integration.s3.trustAllCerts property is set to true. CRT cert validation is off");
			}

			return crtAsyncClientBuilder.build();

		} catch (CrtRuntimeException e) {
			throw SdkException.create(e.getMessage(), e);
		}
	}

	@Override
	protected S3AsyncClient buildAwsAsyncClient(StaticCredentialsProvider credentialsProvider, String region) {
		try {
			// If configured, start the AWS CRT logger.
			initCrtLogging();

			// Instantiate a S3 async client.
			return S3AsyncClient.crtBuilder().credentialsProvider(credentialsProvider).region(Region.of(region))
					.minimumPartSizeInBytes(minimumUploadPartSize).thresholdInBytes(multipartUploadThreshold).build();

		} catch (CrtRuntimeException e) {
			throw SdkException.create(e.getMessage(), e);
		}
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * If configured, start the AWS CRT logger. An invalid log level is logged and
	 * defaulted rather than raised, so that a bad logging configuration doesn't fail
	 * the S3 authentication.
	 */
	private void initCrtLogging() {
		if (StringUtils.isEmpty(crtLogFile)) {
			return;
		}

		Log.LogLevel logLevel = DEFAULT_CRT_LOG_LEVEL;
		try {
			logLevel = Log.LogLevel.valueOf(crtLogLevel);

		} catch (IllegalArgumentException e) {
			logger.warn("Invalid hpc.integration.s3.crtLogLevel [{}]. Valid values are {}. Defaulting to [{}]",
					crtLogLevel, Arrays.toString(Log.LogLevel.values()), DEFAULT_CRT_LOG_LEVEL);
		}

		Log.initLoggingToFile(logLevel, crtLogFile);
	}
}
