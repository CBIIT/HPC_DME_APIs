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
package gov.nih.nci.hpc.integration.googledrive.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Google Drive Connection.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
@SuppressWarnings("deprecation")
public class HpcGoogleDriveConnection {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The (Google) HPC Application name. Users need to provide drive access to this
	// app name.
	@Value("${hpc.integration.googledrive.hpcApplicationName}")
	String hpcApplicationName = null;
	
	@Value("${hpc.integration.googledrive.httpTimeout}")
	private int hpcGoogleDriveHttpTimeout = 20000; // 20 seconds google default

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 */
	private HpcGoogleDriveConnection() {
	}
	
	private HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
        return new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest httpRequest) throws IOException {
                requestInitializer.initialize(httpRequest);
                httpRequest.setConnectTimeout(hpcGoogleDriveHttpTimeout);
                httpRequest.setReadTimeout(hpcGoogleDriveHttpTimeout);
            }
        };
    }
	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	/**
	 * Authenticate a google drive.
	 *
	 * @param credentialsJson Google Drive Credentials Json.
	 * @throws HpcException if authentication failed
	 */
	public Object authenticate(String credentialsJson) throws HpcException {
		Drive drive = null;
		try {
			GoogleCredentials googleCredentials =
					GoogleCredentials.fromStream(IOUtils.toInputStream(credentialsJson, StandardCharsets.UTF_8));
			HttpRequestInitializer credentialsInitializer = new HttpCredentialsAdapter(googleCredentials);
			HttpRequestInitializer initializerWithTimeout = setHttpTimeout(credentialsInitializer);

			drive = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(),
					initializerWithTimeout).setApplicationName(hpcApplicationName).build();

			// Confirm the drive is accessible.
			drive.about().get().setFields("appInstalled").execute();

			return drive;

		} catch (IOException | GeneralSecurityException e) {
			throw new HpcException("Failed to authenticate Google Drive w/ access-token: " + e.getMessage(),
					HpcErrorType.INVALID_REQUEST_INPUT, e);
		}
	}

	/**
	 * Get Drive from an authenticated token.
	 *
	 * @param authenticatedToken An authenticated token.
	 * @return A Drive object.
	 * @throws HpcException on invalid authentication token.
	 */
	public Drive getDrive(Object authenticatedToken) throws HpcException {
		if (authenticatedToken == null || !(authenticatedToken instanceof Drive)) {
			throw new HpcException("Invalid Google Drive authentication token", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return (Drive) authenticatedToken;
	}
}
