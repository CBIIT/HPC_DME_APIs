/**
 * HpcGlobusConnection.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration.globus.impl;

import java.util.Arrays;
import org.globusonline.transfer.JSONTransferAPIClient;
import com.google.api.client.auth.oauth2.ClientCredentialsTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * Globus connection via client credentials token.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcGlobusConnection {
  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // Globus connection attributes.
  private String globusAuthScope = null;

  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /**
   * Constructor for Spring Dependency Injection.
   *
   * @param globusAuthUrl The Globus auth/token URL.
   * @param globusAuthScope The Globus authentication scope.
   */
  public HpcGlobusConnection(String globusAuthScope) {
    this.globusAuthScope = globusAuthScope;
  }

  /**
   * Default Constructor.
   *
   * @throws HpcException Constructor is disabled.
   */
  @SuppressWarnings("unused")
  private HpcGlobusConnection() throws HpcException {
    throw new HpcException("Constructor Disabled", HpcErrorType.SPRING_CONFIGURATION_ERROR);
  }

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  /**
   * Authenticate an account.
   *
   * @param globusAuthUrl The Globus authentication URL.
   * @param dataTransferAccount A data transfer account to authenticate.
   * @return An authenticated JSONTransferAPIClient object, or null if authentication failed.
   * @throws HpcException if authentication failed.
   */
  public Object authenticate(String globusAuthUrl, HpcIntegratedSystemAccount dataTransferAccount)
      throws HpcException {
    BasicAuthentication authentication =
        new BasicAuthentication(
            dataTransferAccount.getUsername(), dataTransferAccount.getPassword());

    // Instantiate a client credentials token request.
    ClientCredentialsTokenRequest tokenRequest =
        new ClientCredentialsTokenRequest(
            new NetHttpTransport(), new JacksonFactory(), new GenericUrl(globusAuthUrl));
    tokenRequest.setClientAuthentication(authentication);
    tokenRequest.setScopes(Arrays.asList(globusAuthScope));

    try {
      // Obtain a Globus access token.
      TokenResponse tokenResponse = tokenRequest.execute();

      // Instantiate a transfer client w/ token authorization.
      JSONTransferAPIClient transferClient =
          new JSONTransferAPIClient(dataTransferAccount.getUsername());
      final String token = "Bearer " + tokenResponse.getAccessToken();
      transferClient.setAuthenticator(
          urlConnection -> urlConnection.setRequestProperty("Authorization", token));
      return transferClient;

    } catch (Exception e) {
      throw new HpcException(
          "[GLOBUS] Failed to authenticate account ["
              + dataTransferAccount.getUsername()
              + "]: "
              + e.getMessage(),
          HpcErrorType.DATA_TRANSFER_ERROR,
          e);
    }
  }

  /**
   * Get Globus transfer client from an authenticated token.
   *
   * @param authenticatedToken An authenticated token.
   * @return A transfer client object.
   * @throws HpcException @throws HpcException on data transfer system failure.
   */
  public JSONTransferAPIClient getTransferClient(Object authenticatedToken) throws HpcException {
    if (authenticatedToken == null || !(authenticatedToken instanceof JSONTransferAPIClient)) {
      throw new HpcException(
          "Invalid Globus authentication token", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    return (JSONTransferAPIClient) authenticatedToken;
  }
}
