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

import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.beans.factory.annotation.Value;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Google Drive Connection.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcGoogleCloudStorageConnection {
  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // The (Google) HPC Application name. Users need to provide drive access to this app name.
  @Value("${hpc.integration.googledrive.hpcApplicationName}")
  String hpcApplicationName = null;

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /**
   * Constructor for Spring Dependency Injection.
   * 
   */
  private HpcGoogleCloudStorageConnection() {}

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  /**
   * Authenticate a google drive.
   *
   * @param accessToken Google Drive Access Token.
   * @throws HpcException if authentication failed
   */
  public Object authenticate(String accessToken) throws HpcException {
    Drive drive = null;
    try {
      drive = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(),
          JacksonFactory.getDefaultInstance(), new GoogleCredential().setAccessToken(accessToken))
              .setApplicationName(hpcApplicationName).build();

      // Confirm the drive is accessible.
      drive.about().get().setFields("appInstalled").execute();

      return drive;

    } catch (IOException | GeneralSecurityException e) {
      throw new HpcException(
          "Failed to authenticate Google Drive w/ access-token: " + e.getMessage(),
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
      throw new HpcException("Invalid Google Drive authentication token",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    return (Drive) authenticatedToken;
  }
}
