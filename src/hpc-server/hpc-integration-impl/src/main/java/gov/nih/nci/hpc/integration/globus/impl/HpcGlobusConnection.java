/**
 * HpcGlobusConnection.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration.globus.impl;

import java.io.IOException;

import org.globusonline.nexus.GoauthClient;
import org.globusonline.nexus.exception.InvalidCredentialsException;
import org.globusonline.transfer.Authenticator;
import org.globusonline.transfer.GoauthAuthenticator;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * Globus connection via Nexus API. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcGlobusConnection 
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// Globus connection attributes.
	private String nexusAPIURL = null;
	private String globusURL = null;
	
    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param nexusAPIURL The Nexus API endpoint URL.
     * @param globusURL The Globus Online endpoint URL.
     */
    private HpcGlobusConnection(String nexusAPIURL, String globusURL)
    {
        this.nexusAPIURL = nexusAPIURL;
        this.globusURL = globusURL;
    }
    
	/**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
	private HpcGlobusConnection() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Authenticate an account.
     *
     * @param dataTransferAccount A data transfer account to authenticate.
     * @return An authenticated JSONTransferAPIClient object, or null if authentication failed.
     * @throws HpcException if authentication failed.
     */
    public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount)
			                  throws HpcException
    {
    	GoauthClient authClient = new GoauthClient(nexusAPIURL,  
    			                                   globusURL, 
                                                   dataTransferAccount.getUsername(), 
                                                   dataTransferAccount.getPassword());
    	TokenResponse response = null;
    	try {
    	      response =
    	          new RefreshTokenRequest(new NetHttpTransport(), new JacksonFactory(), new GenericUrl(
    	              "https://auth.globus.org/v2/oauth2/token"), "AQEAAAAAAAWNqC4mbxMJY0FSSPm356YTO70Q13vwyKh-wzxfLI2GRjDCDIsbr3UelFZeTLfFUhYeiZI2Z69W")
    	              .setClientAuthentication(
    	                  new BasicAuthentication("025ff462-07e1-483b-8dbb-1fc26c7eb17e", "Os6M69M+vuaxLvl4XnhMeoqIBu2a84e8d5P7upo7aUw=")).execute();
    	      logger.error("ERAN - Access token: " + response.getAccessToken());
    	    } catch (TokenResponseException e) {
    	      if (e.getDetails() != null) {
    	        logger.error("ERAN - Error: " + e.getDetails().getError());
    	        if (e.getDetails().getErrorDescription() != null) {
    	          logger.error("ERAN - " + e.getDetails().getErrorDescription());
    	        }
    	        if (e.getDetails().getErrorUri() != null) {
    	          logger.error("ERAN - " + e.getDetails().getErrorUri());
    	        }
    	      } else {
    	        logger.error("ERAN -" + e.getMessage());
    	      }
    	    } catch(IOException ioe) {
    	    	    logger.error("ERAN - " + ioe.getMessage());
    	    }
		try {
			 //JSONObject accessTokenJSON = authClient.getClientOnlyAccessToken();
			 //String accessToken = accessTokenJSON.getString("access_token");
			 //authClient.validateAccessToken(accessToken);        
			
			 //Authenticator authenticator = new GoauthAuthenticator(accessToken);
			 JSONTransferAPIClient transferClient = 
			     new JSONTransferAPIClient(dataTransferAccount.getUsername());
			 //transferClient.setAuthenticator(authenticator);
			 final String token = "Bearer " + response.getAccessToken();
			 transferClient.setAuthenticator(urlConnection -> urlConnection.setRequestProperty("Authorization", token)); 
			 return transferClient;
		
		//} catch(InvalidCredentialsException ice) {
			//    logger.error("Invalid Globus credentials: " + dataTransferAccount.getUsername(), ice);
		      //  return null;
		    
		} catch(Exception e) {
    	        throw new HpcException("[GLOBUS] Failed to authenticate account: " +
    	        		               dataTransferAccount.getUsername(),
    	    		                   HpcErrorType.DATA_TRANSFER_ERROR, e);
		}
	} 
    
    /**
     * Authenticate a token.
     *
     * @param username The Globus username.
     * @param accessToken The Globus username.
     * @return An authenticated JSONTransferAPIClient object, or null if authentication failed.
     * @throws HpcException If authentication failed.
     */
    public Object authenticate(String username, String accessToken)
			                  throws HpcException
    {
    	GoauthClient authClient = new GoauthClient();
    	authClient.setNexusApiHost(nexusAPIURL);
    	authClient.setGlobusOnlineHost(globusURL);
    	
		try {
			 authClient.validateAccessToken(accessToken);        
			
			 Authenticator authenticator = new GoauthAuthenticator(accessToken);
			 JSONTransferAPIClient transferClient = new JSONTransferAPIClient(username);
			 transferClient.setAuthenticator(authenticator);
			 return transferClient;
		
		} catch(InvalidCredentialsException ice) {
		        return null;
		    
		} catch(Exception e) {
    	        throw new HpcException("[GLOBUS] Failed to authenticate account: " +
    	        		               username,
    	    		                   HpcErrorType.DATA_TRANSFER_ERROR, e);
		}
	} 
	
    /**
     * Get Globus transfer client from an authenticated token.
     * 
     * @param authenticatedToken An authenticated token.
     * @return A transfer client object.
     * @throws HpcException @throws HpcException on data transfer system failure.
     */
    public JSONTransferAPIClient getTransferClient(Object authenticatedToken) 
    		                                      throws HpcException
    {
    	if(authenticatedToken == null ||
    	   !(authenticatedToken instanceof JSONTransferAPIClient)) {
    	   throw new HpcException("Invalid Globus authentication token",
    	    			          HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	    	
    	return (JSONTransferAPIClient) authenticatedToken;
	}  
}

 