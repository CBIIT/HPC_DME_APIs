package gov.nih.nci.hpc.integration.oidc.impl;

import java.io.InputStream;
import jakarta.ws.rs.core.Response;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcOidcAuthorizationProxy;

/**
 * <p>
 * OIDC Authorization Proxy Implementation.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */

public class HpcOidcAuthorizationProxyImpl implements HpcOidcAuthorizationProxy {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// OIDC URL
	String url = null;

	// JWK set public keys Endpoint
	String jwksEndpoint = null;
	
	// JWK set public keys
	String jwks = null;
		
	// User Info Endpoint
	String userinfoEndpoint = null;
	
	// User Info Attribute
	String userinfoAttribute = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for spring injection.
	 * 
	 * @param url      The OIDC URL.
	 * @param jwksEndpoint The OIDC JWK set endpoint.
	 * @param userinfoEndpoint The OIDC user info endpoint.
	 * @param userinfoEndpoint The OIDC user attribute.
	 * @throws HpcException If it failed to instantiate the OIDC Proxy
	 */
	private HpcOidcAuthorizationProxyImpl(String url, String jwksEndpoint, String userinfoEndpoint, String userinfoAttribute) throws HpcException {
		this.url = url;
		this.jwksEndpoint = jwksEndpoint;
		this.userinfoEndpoint = userinfoEndpoint;
		this.userinfoAttribute = userinfoAttribute;
		
		try {
	        WebClient client = WebClient.create(url + jwksEndpoint);
	        Response response = client.accept("application/json").get();
	        if (response.getStatus() == 200) {
	        	jwks = IOUtils.toString((InputStream)response.getEntity());
				
			} else {
				throw new HpcException("OIDC JWK Set endpoint access not successful, response=" + response.getStatus(),
						HpcErrorType.UNEXPECTED_ERROR);
			}
	        
		} catch (Exception e) {
			throw new HpcException("OIDC JWK Set endpoint access not successful", e);
		}
	}

	/**
	 * Default Constructor is disabled
	 * 
	 * @throws HpcException Constructor is disabled.
	 */
	private HpcOidcAuthorizationProxyImpl() throws HpcException {
		throw new HpcException("Default Constructor Disabled", HpcErrorType.SPRING_CONFIGURATION_ERROR);
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcOidcAuthorizationProxy Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public String getUsername(String accessToken) throws HpcException {

		WebClient client = null;
		String userId;
		try {      

	        client = WebClient.create(url + userinfoEndpoint).query("access_token", accessToken);
	        Response response = client.accept("application/json").get();
			if (response.getStatus() == 200) {
				String jsonString = IOUtils.toString((InputStream)response.getEntity());
				JSONObject resp = (JSONObject) (new JSONParser().parse(jsonString));
				userId = resp.get(userinfoAttribute).toString();
				
			} else {
				logger.error("OIDC User info endpoint access not successful, response=" + response.getStatus());
				throw new HpcException("OIDC User info endpoint access not successful, response=" + response.getStatus(),
						HpcErrorType.REQUEST_AUTHENTICATION_FAILED);
			}

			return userId;

		} catch (Exception e) {
			logger.error("OIDC User info endpoint access not successful", e);
			throw new HpcException("OIDC User info endpoint access not successful", e);
		}
	}

	@Override
	public String getJWKSet() {
		return jwks;
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

}
