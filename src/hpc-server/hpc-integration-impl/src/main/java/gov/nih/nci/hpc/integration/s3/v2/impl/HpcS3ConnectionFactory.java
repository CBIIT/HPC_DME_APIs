/**
 * HpcS3ConnectionFactory.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration.s3.v2.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * Factory that instantiates the configured {@link HpcS3Connection}
 * implementation. The concrete implementation (AWS CRT or Netty-NIO based) is
 * selected via the {@code hpc.integration.s3.asyncClient} property.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcS3ConnectionFactory {
	// ---------------------------------------------------------------------//
	// Enum Types
	// ---------------------------------------------------------------------//

	/**
	 * The supported S3 async client implementations.
	 */
	public enum HpcS3AsyncClient {
		CRT, NETTYNIO
	}

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Default constructor disabled - this is a static factory.
	 */
	private HpcS3ConnectionFactory() {
	}

	// ---------------------------------------------------------------------//
	// Factory Methods
	// ---------------------------------------------------------------------//

	/**
	 * Create a {@link HpcS3Connection} of the configured implementation. Invoked by
	 * Spring as a factory-method; the returned instance remains a Spring-managed
	 * bean, so its {@code @Value} fields are injected and its lifecycle callbacks
	 * are honored.
	 *
	 * @param asyncClient                      The S3 async client implementation to
	 *                                         use (CRT or NETTYNIO,
	 *                                         case-insensitive).
	 * @param pathStyleAccessEnabledProviders  A list of S3 3rd party providers that
	 *                                         require connection w/ path-style
	 *                                         enabled.
	 * @param awsTransferManagerThreadPoolSize The thread pool size to be used for
	 *                                         AWS transfer manager.
	 * @return A concrete {@link HpcS3Connection} instance.
	 * @throws HpcException If the configured async client value is invalid.
	 */
	public static HpcS3Connection create(String asyncClient, String pathStyleAccessEnabledProviders,
			int awsTransferManagerThreadPoolSize) throws HpcException {
		HpcS3AsyncClient implementation;
		try {
			implementation = HpcS3AsyncClient.valueOf(asyncClient.trim().toUpperCase());

		} catch (IllegalArgumentException | NullPointerException e) {
			throw new HpcException("Invalid hpc.integration.s3.asyncClient [" + asyncClient
					+ "]. Valid values are CRT, NETTYNIO", HpcErrorType.INVALID_REQUEST_INPUT, e);
		}

		switch (implementation) {
		case NETTYNIO:
			return new HpcS3ConnectionNettyNioAsyncClient(pathStyleAccessEnabledProviders,
					awsTransferManagerThreadPoolSize);
		case CRT:
		default:
			return new HpcS3ConnectionCrtAsyncClient(pathStyleAccessEnabledProviders, awsTransferManagerThreadPoolSize);
		}
	}
}
