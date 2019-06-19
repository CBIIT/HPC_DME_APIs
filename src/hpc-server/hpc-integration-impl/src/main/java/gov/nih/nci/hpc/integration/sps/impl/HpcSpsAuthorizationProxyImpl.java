package gov.nih.nci.hpc.integration.sps.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcSpsAuthorizationProxy;

import java.util.Collections;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * SPS Authorization Proxy Implementation.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */

public class HpcSpsAuthorizationProxyImpl implements HpcSpsAuthorizationProxy {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// SPS Authorize URL
	String url = null;

	// Resource
	String resource = null;

	// Domain
	String domain = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for spring injection.
	 * 
	 * @param url The Authorize SPS URL.
	 * @param username The account to access the SPS web service.
	 * @param password The password to access the SPS web service.
	 * @param resource The SPS web service resource.
	 * @param domain The basic auth domain.
	 */
	private HpcSpsAuthorizationProxyImpl(String url, String resource, String domain) {
		this.url = url;
		this.resource = resource;
		this.domain = domain;

	}

	/**
	 * Default Constructor is disabled
	 * 
	 * @throws HpcException
	 *             Constructor is disabled.
	 */
	private HpcSpsAuthorizationProxyImpl() throws HpcException {
		throw new HpcException("Default Constructor Disabled", HpcErrorType.SPRING_CONFIGURATION_ERROR);
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcSpsAuthorizationProxy Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public boolean authorize(String session, String username, String password) throws HpcException {

		WebClient client = null;
		try {
			String credential = username + ":" + password;
			client = WebClient.create(url + resource, Collections.singletonList(new JAXBElementProvider()));

			String authorizationHeader = "Basic "
					+ org.apache.cxf.common.util.Base64Utility.encode(credential.getBytes());
			client.header("Authorization", authorizationHeader);
			client.header(HttpHeaders.COOKIE, "NIHSMCHALLENGE=YES");
			client.type("application/xml").accept("application/xml");

			Authorize authorize = new Authorize();
			authorize.setAction("POST");
			authorize.setResource("nihuser");
			authorize.setSessionToken(session);
			Response response = client.post(authorize);
			if (response.getStatus() == 200) {
				AuthorizationResult authorizationResult = response.readEntity(AuthorizationResult.class);
				if (authorizationResult.getResultCode().equals(AuthorizationResultCodes.AUTHORIZED))
					return true;
			} else {
				throw new HpcException("SPS Authroize not successful, response=" + response.getStatus(),
						HpcErrorType.REQUEST_AUTHENTICATION_FAILED);
			}

			return false;

		} catch (Exception e) {
			throw new HpcException("SPS Authroize not successful", e);
		}
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

}
