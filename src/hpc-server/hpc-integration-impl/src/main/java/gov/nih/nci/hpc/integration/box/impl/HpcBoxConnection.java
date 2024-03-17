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
	 * @param authCode            Box auth code.
	 * @return An authenticated token, to be used in subsequent calls to data
	 *         transfer.
	 * @throws HpcException on data transfer system failure.
	 */
	public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount, String authCode) throws HpcException {
		try {
			return new BoxAPIConnection(dataTransferAccount.getUsername(), dataTransferAccount.getPassword(), authCode);

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
}
