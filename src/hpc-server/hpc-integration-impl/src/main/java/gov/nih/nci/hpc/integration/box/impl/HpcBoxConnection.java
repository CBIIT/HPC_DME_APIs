/**
 * HpcGoogleConnection.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration.box.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Google Drive Connection.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcBoxConnection {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 */
	private HpcBoxConnection() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	/**
	 * Authenticate a Box account.
	 *
	 * @param dataTransferAccount The Data Transfer account to authenticate.
	 * @param accessToken         Box accessToken.
	 * @param refreshToken        Box refreshToken.
	 * @return An authenticated token, to be used in subsequent calls to data
	 *         transfer.
	 * @throws HpcException on data transfer system failure.
	 */
	public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount, String accessToken, String refreshToken)
			throws HpcException {
		try {
			return new BoxAPIConnection(dataTransferAccount.getUsername(), dataTransferAccount.getPassword(),
					accessToken, refreshToken);

		} catch (BoxAPIException e) {
			throw new HpcException("Failed to authenticate Box w/ auth-code: " + e.getMessage(),
					HpcErrorType.INVALID_REQUEST_INPUT, e);
		}
	}

	/**
	 * Get Box API instance from an authenticated token.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @return A Box API object.
	 * @throws HpcException on invalid authentication token.
	 */
	public BoxAPIConnection getBoxAPIConnection(Object authenticatedToken) throws HpcException {
		if (authenticatedToken == null || !(authenticatedToken instanceof BoxAPIConnection)) {
			throw new HpcException("Invalid Box authentication token", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return (BoxAPIConnection) authenticatedToken;
	}

	public void logBoxApi(BoxAPIConnection boxApi, String msg) {
		logger.error(
				"ERAN BOX connection [{}]- token: {}, can-refresh: {}, connect-timeout: {}, expires: {}, auto-refresh: {}, last-refresh: {}, max-retry-attempts: {}, needs-refresh: {}",
				msg, boxApi.getAccessToken(), boxApi.canRefresh(), boxApi.getConnectTimeout(), boxApi.getExpires(),
				boxApi.getAutoRefresh(), boxApi.getLastRefresh(), boxApi.getMaxRetryAttempts(), boxApi.needsRefresh());
	}
}
