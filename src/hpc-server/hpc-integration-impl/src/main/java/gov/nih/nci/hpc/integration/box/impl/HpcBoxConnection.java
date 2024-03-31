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

import java.util.HashMap;

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

	// In memory local cache of access and refresh tokens
	private final HashMap<String, String> tokensMap = new HashMap<>();

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
			synchronized (accessToken) {
				// Establish the Box API connection
				BoxAPIConnection boxApi = new BoxAPIConnection(dataTransferAccount.getUsername(),
						dataTransferAccount.getPassword(), accessToken,
						tokensMap.containsKey(accessToken) ? tokensMap.get(accessToken) : refreshToken);

				// Update the tokens in-memory cache w/ the freshly created refresh-token.
				tokensMap.put(accessToken, boxApi.getRefreshToken());

				// Temporarily disable auto-refresh.
				logger.error("ERAN tokens cache - {} {}", refreshToken, boxApi.getRefreshToken());

				// boxApi.setAutoRefresh(false);
				return boxApi;
			}

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
				"ERAN BOX connection [{}]- token: {}, refresh-token: {}, can-refresh: {}, connect-timeout: {}, expires: {}, auto-refresh: {}, last-refresh: {}, max-retry-attempts: {}, needs-refresh: {}",
				msg, boxApi.getAccessToken(), boxApi.getRefreshToken(), boxApi.canRefresh(), boxApi.getConnectTimeout(),
				boxApi.getExpires(), boxApi.getAutoRefresh(), boxApi.getLastRefresh(), boxApi.getMaxRetryAttempts(),
				boxApi.needsRefresh());
	}
}
