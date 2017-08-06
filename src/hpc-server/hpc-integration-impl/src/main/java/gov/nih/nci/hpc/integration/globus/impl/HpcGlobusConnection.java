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

import java.util.Arrays;

import org.globusonline.transfer.JSONTransferAPIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.ClientCredentialsTokenRequest;
import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
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
	private String globusAuthUrl = null;
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
     * @param globusAuthUrl The Globus auth/token URL.
     * @param globusURL The Globus Online endpoint URL.
     */
    private HpcGlobusConnection(String globusAuthUrl, String globusURL)
    {
        this.globusAuthUrl = globusAuthUrl;
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
    	BasicAuthentication authentication = 
    		                new BasicAuthentication(dataTransferAccount.getUsername(), 
    		                                        dataTransferAccount.getPassword());
    	RefreshTokenRequest tokenRequest = 
    			            new RefreshTokenRequest(new NetHttpTransport(), new JacksonFactory(), 
                                                    new GenericUrl(globusAuthUrl), 
                "AQEAAAAAAAWNqC4mbxMJY0FSSPm356YTO70Q13vwyKh-wzxfLI2GRjDCDIsbr3UelFZeTLfFUhYeiZI2Z69W");
    	tokenRequest.setClientAuthentication(authentication);
    	
    	ClientCredentialsTokenRequest tokenRequest1 =
    	          new ClientCredentialsTokenRequest(new NetHttpTransport(), new JacksonFactory(),
    	                                            new GenericUrl(globusAuthUrl));
    	tokenRequest1.setClientAuthentication(authentication);
    	tokenRequest1.setScopes(Arrays.asList("urn:globus:auth:scope:transfer.api.globus.org:all"));
    	
    	try {
    		 logger.error("ERAN: Globus uid: " + dataTransferAccount.getUsername());
    		 logger.error("ERAN: Globus pwd: " + dataTransferAccount.getPassword());
    		 
    		 TokenResponse tokenResponse = tokenRequest.execute();
    		 logger.error("ERAN RT tok:" + tokenResponse.getAccessToken());
    		 
    		 TokenResponse tokenResponse1 = tokenRequest1.execute();
    		 logger.error("ERAN CC tok:" + tokenResponse1.getAccessToken());
    		 
			 JSONTransferAPIClient transferClient =  new JSONTransferAPIClient(dataTransferAccount.getUsername());
			 final String token = "Bearer " + tokenResponse.getAccessToken();
			 transferClient.setAuthenticator(
					 urlConnection -> urlConnection.setRequestProperty("Authorization", token)); 
			 return transferClient;
			
		} catch(Exception e) {
    	        throw new HpcException("[GLOBUS] Failed to authenticate account [" +
    	        		               dataTransferAccount.getUsername() + "]: " + e.getMessage(),
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

 