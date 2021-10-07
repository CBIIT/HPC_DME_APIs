/**
 * HpcAuthorizationServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/blob/master/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.service.impl;

import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.storage.StorageScopes;
import gov.nih.nci.hpc.web.service.HpcAuthorizationService;

@Service
public class HpcAuthorizationServiceImpl implements HpcAuthorizationService {

  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final String TOKENS_DIRECTORY_PATH = "tokens";
  private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
  private static final List<String> SCOPES_CLOUD = Collections.singletonList(StorageScopes.DEVSTORAGE_READ_ONLY);
  private static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

  @Value("${gov.nih.nci.hpc.drive.clientid}")
  private String clientId;
  @Value("${gov.nih.nci.hpc.drive.clientsecret}")
  private String clientSecret;

  private Logger logger = LoggerFactory.getLogger(HpcAuthorizationServiceImpl.class);
  private GoogleAuthorizationCodeFlow flow;
  private GoogleAuthorizationCodeFlow flowCloud;


  @PostConstruct
  public void init() throws Exception {

    // Build flow and trigger user authorization request for Google Drive
    flow =
        new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientId, clientSecret, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build();
    
 // Build flow and trigger user authorization request Google Cloud
    flowCloud =
          new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientId, clientSecret, SCOPES_CLOUD)
              .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
              .setAccessType("offline")
              .build();
  }

  @Override
  public String authorize(String redirectUri, ResourceType resourceType ) throws Exception {
    String redirectUrl="";
    if(resourceType == ResourceType.DRIVE ) {
      GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
      redirectUrl = url.setRedirectUri(redirectUri).setAccessType("offline").build();
      logger.debug("Google Drive redirectUrl, " + redirectUrl);
    } else if (resourceType == ResourceType.GOOGLECLOUD) {
      GoogleAuthorizationCodeRequestUrl url = flowCloud.newAuthorizationUrl();
      redirectUrl = url.setRedirectUri(redirectUri).setAccessType("offline").build();
      logger.debug("Google Cloud redirectUrl, " + redirectUrl);
    }
    return redirectUrl;
  }
    
  public String getToken(String code, String redirectUri, ResourceType resourceType) throws Exception {
    GoogleTokenResponse tokenResponse = new GoogleTokenResponse();
    // exchange the code against the access token and refresh token
    if(resourceType == ResourceType.DRIVE) {
      tokenResponse =
          flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
    } else if (resourceType == ResourceType.GOOGLECLOUD) {
      tokenResponse =
          flowCloud.newTokenRequest(code).setRedirectUri(redirectUri).execute();
    }
    return tokenResponse.getAccessToken();
  }
}
