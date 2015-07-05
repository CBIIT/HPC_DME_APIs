/**
 * HpcMongoDB.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration.globus.driver;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.globusonline.nexus.GoauthClient;
import org.globusonline.nexus.exception.NexusClientException;
import org.globusonline.transfer.APIError;
import org.globusonline.transfer.Authenticator;
import org.globusonline.transfer.GoauthAuthenticator;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>
 * HPC MongoDB. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcGOTransfer 
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The JSONTransferAPIClient client instance.
	private JSONTransferAPIClient transferClient = null;
	

	/**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    @SuppressWarnings("unused")
	private HpcGOTransfer() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param mongoHost The Mongo hostname.
     * @param hpcCodecProvider The HPC codec provider instance.
     * 
     * @throws HpcException If a HpcCodecProvider instance was not provided.
     */
    public HpcGOTransfer(String username, String password) throws IOException, JSONException, GeneralSecurityException, APIError,NexusClientException,HpcException
    {
    	if(username == null || password == null ) {
    	   throw new HpcException("Null HpcGOTransfer username/password",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    
        GoauthClient cli = new GoauthClient("nexus.api.globusonline.org", "www.globusonline.org", username, password);
		JSONObject accessTokenJSON = cli.getClientOnlyAccessToken();
		String accessToken = accessTokenJSON.getString("access_token");
		System.out.println("Client only access token: " + accessToken);
		cli.validateAccessToken(accessToken);        

        Authenticator authenticator = new GoauthAuthenticator(accessToken);
        transferClient = new JSONTransferAPIClient(username, null, null);
        transferClient.setAuthenticator(authenticator);
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
	
    public JSONTransferAPIClient getTransferClient() {
		return transferClient;
	}       
}

 