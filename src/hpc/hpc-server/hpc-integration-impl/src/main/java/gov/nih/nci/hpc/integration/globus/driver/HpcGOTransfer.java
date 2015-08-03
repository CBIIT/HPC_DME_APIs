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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.globusonline.nexus.GoauthClient;
import org.globusonline.nexus.exception.NexusClientException;
import org.globusonline.transfer.APIError;
import org.globusonline.transfer.Authenticator;
import org.globusonline.transfer.GoauthAuthenticator;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC GO Transfer. 
 * </p>
 *
 * @author <a href="mailto:mahidhar.narra@nih.gov">Mahidhar Narra</a>
 * @version $Id$
 */

public class HpcGOTransfer 
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The JSONTransferAPIClient client instance.
	private JSONTransferAPIClient transferClient = null;
	private String destinationBaseLocation = null;
	
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
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
     * @param destinationBaseLocation The base location for storing files.
     * 
     * @throws HpcException.
     */
    public HpcGOTransfer(String destinationBaseLocation) throws IOException, JSONException, GeneralSecurityException, APIError,NexusClientException,HpcException
    {
        this.destinationBaseLocation = destinationBaseLocation;
    }

    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    public void setTransferCient(String username, String password)
			throws HpcException, NexusClientException, JSONException,
			KeyManagementException, NoSuchAlgorithmException {
		if(username == null || password == null ) {
    	   throw new HpcException("Null HpcGOTransfer username/password",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    
        GoauthClient cli = new GoauthClient("nexus.api.globusonline.org", "www.globusonline.org", username, password);
		JSONObject accessTokenJSON = cli.getClientOnlyAccessToken();
		String accessToken = accessTokenJSON.getString("access_token");
		logger.debug("Client only access token: " + accessToken);
		cli.validateAccessToken(accessToken);        

        Authenticator authenticator = new GoauthAuthenticator(accessToken);
        transferClient = new JSONTransferAPIClient(username, null, null);
        transferClient.setAuthenticator(authenticator);
	} 

     
	
    public JSONTransferAPIClient getTransferClient() {
		return transferClient;
	}  
    
	public String getDestinationBaseLocation() {
		return destinationBaseLocation;
	}
    
}

 