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
package gov.nih.nci.hpc.integration.googlecloudstorage.impl;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import gov.nih.nci.hpc.domain.datatransfer.HpcAccessTokenType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Google Drive Connection.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcGoogleCloudStorageConnection {

	// Temp toggle to use refresh-token
	@Value("${hpc.integration.googlecloudstorage.refreshToken}")
	boolean refreshToken = false;

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 */
	private HpcGoogleCloudStorageConnection() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	/**
	 * Authenticate google cloud storage.
	 *
	 * @param accessToken      Google Cloud Storage token.
	 * @param accessTokenToken Google Cloud Storage token type.
	 * @throws HpcException if authentication failed.
	 */
	public Object authenticate(String accessToken, HpcAccessTokenType accessTokenType) throws HpcException {
		try {
			return StorageOptions.newBuilder()
					.setCredentials(accessTokenType.equals(HpcAccessTokenType.SERVICE_ACCOUNT)
							? GoogleCredentials.fromStream(IOUtils.toInputStream(accessToken, StandardCharsets.UTF_8))
									.createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"))
							: refreshToken ? GoogleCredentials/*UserCredentials*/.fromStream(IOUtils.toInputStream(accessToken, StandardCharsets.UTF_8))
									: GoogleCredentials.create(new AccessToken(accessToken, null)))
					.build().getService();

		} catch (Exception e) {
			// Catching all as runtime exception will be thrown if the token is not a valid
			// JSON.
			throw new HpcException("Failed to authenticate Google Cloud Storage w/ access-token: " + e.getMessage(),
					HpcErrorType.INVALID_REQUEST_INPUT, e);
		}
	}

	/**
	 * Get Storage from an authenticated token.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @return A Storage object.
	 * @throws HpcException on invalid authentication token.
	 */
	public Storage getStorage(Object authenticatedToken) throws HpcException {
		if (!(authenticatedToken instanceof Storage)) {
			throw new HpcException("Invalid Google Cloud Storage authentication token",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return (Storage) authenticatedToken;
	}
}
