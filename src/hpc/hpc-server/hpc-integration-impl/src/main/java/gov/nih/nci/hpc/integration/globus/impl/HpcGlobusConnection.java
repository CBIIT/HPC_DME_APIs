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

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

import org.globusonline.nexus.GoauthClient;
import org.globusonline.nexus.exception.InvalidCredentialsException;
import org.globusonline.transfer.Authenticator;
import org.globusonline.transfer.GoauthAuthenticator;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.json.JSONObject;

/**
 * <p>
 * Globus connection via Nexus API. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcGlobusConnection 
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// Globus connection attributes.
	private String nexusAPIURL = null;
	private String globusURL = null;
	
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
     * @throws HpcException if authentication failed..
     */
    public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount)
			                  throws HpcException
    {
    	GoauthClient authClient = new GoauthClient(nexusAPIURL,  
    			                                   globusURL, 
                                                   dataTransferAccount.getUsername(), 
                                                   dataTransferAccount.getPassword());
		try {
			 JSONObject accessTokenJSON = authClient.getClientOnlyAccessToken();
			 String accessToken = accessTokenJSON.getString("access_token");
			 authClient.validateAccessToken(accessToken);        
			
			 Authenticator authenticator = new GoauthAuthenticator(accessToken);
			 JSONTransferAPIClient transferClient = 
			     new JSONTransferAPIClient(dataTransferAccount.getUsername());
			 transferClient.setAuthenticator(authenticator);
			 return transferClient;
		
		} catch(InvalidCredentialsException ice) {
		        return null;
		    
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

 