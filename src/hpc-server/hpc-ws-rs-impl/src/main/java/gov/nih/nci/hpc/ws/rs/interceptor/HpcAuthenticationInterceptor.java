/**
 * HpcAuthentcationInterceptor.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.interceptor;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.interceptor.security.SecureAnnotationsInterceptor;
import org.apache.cxf.interceptor.security.SimpleAuthorizingInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.bus.HpcSecurityBusService;
import gov.nih.nci.hpc.exception.HpcAuthenticationException;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Authentication Interceptor.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcAuthenticationInterceptor extends AbstractPhaseInterceptor<Message> {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// HTTP header attributes.
	private static final String AUTHORIZATION_HEADER = "Authorization";

	// Authorization Types
	private static final String BASIC_AUTHORIZATION = "Basic";
	private static final String TOKEN_AUTHORIZATION = "Bearer";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Security Business Service instance.
	@Autowired
	private HpcSecurityBusService securityBusService = null;

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	public HpcAuthenticationInterceptor() {
		super(Phase.RECEIVE);

		// We need to authenticate first (this interceptor), and then authorize (other 3
		// interceptors).
		getBefore().add(HpcIPAddressRestrictionInterceptor.class.getName());
		getBefore().add(SecureAnnotationsInterceptor.class.getName());
		getBefore().add(SimpleAuthorizingInterceptor.class.getName());
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// AbstractPhaseInterceptor<Message> Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void handleMessage(Message message) {
		// Authenticate the caller (if configured to do so) and populate the request
		// context.
		try {
			authenticate(message);

			// Set a security context with the user's role.
			HpcSecurityContext sc = new HpcSecurityContext(
					securityBusService.getAuthenticationResponse(false).getUserRole().value());
			message.put(SecurityContext.class, sc);

		} catch (HpcException e) {
			throw new HpcAuthenticationException(e.getMessage(), e);

		} catch (HpcAuthenticationException ex) {
			throw ex;

		} catch (Exception e) {
			throw new HpcAuthenticationException("Authentication failed", e);
		}
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Authenticate the caller.
	 *
	 * @param message The RS message.
	 * @throws HpcException If the caller is not authenticated.
	 */
	private void authenticate(Message message) throws HpcException {
		String smSession = getSmSession(message);
		if (StringUtils.isNotBlank(smSession)) {
			// SM session cookie is available
			String smUser = getSmUser(message);
			authenticate(smUser, smSession);
		} else {
			String[] authorization = getAuthorization(message);
			String authorizationType = authorization[0];

			// Authenticate the caller (if configured to do so) and populate the request
			// context.
			if (authorizationType.equals(BASIC_AUTHORIZATION)) {
				authenticate(message.get(AuthorizationPolicy.class));
			} else if (authorizationType.equals(TOKEN_AUTHORIZATION)) {
				authenticate(authorization[1]);
			} else {
				throw new HpcAuthenticationException("Invalid Authorization Type: " + authorizationType);
			}
		}
	}

	/**
	 * Perform a basic authentication w/ user-name and password.
	 *
	 * @param policy The policy holding user name anf password.
	 * @throws HpcException on authentication failure.
	 */
	private void authenticate(AuthorizationPolicy policy) throws HpcException {
		String userName = null;
		String password = null;
		if (policy != null) {
			userName = policy.getUserName();
			password = policy.getPassword();
		}

		securityBusService.authenticate(userName, password);
	}

	/**
	 * Perform a token authentication (JWT).
	 *
	 * @param token The JWT token.
	 * @throws HpcException on authentication failure.
	 */
	private void authenticate(String token) throws HpcException {
		securityBusService.authenticate(token);
	}

	/**
	 * Perform a SPS authentication w/ SM_USER and NIHSMSESSION.
	 *
	 * @param user      The user name
	 * @param smSession The SM session cookie
	 * @throws HpcException on authentication failure.
	 */
	private void authenticate(String user, String smSession) throws HpcException {

		securityBusService.authenticateSso(user, smSession);
	}

	/**
	 * Get Authorization array from a message.
	 *
	 * @param message The RS message.
	 * @return The authorization type of the message.
	 * @throws HpcAuthenticationException on invalid authorization header. This is a
	 *                                    runtime exception.
	 */
	private String[] getAuthorization(Message message) {
		// Determine the authorization type.
		@SuppressWarnings("unchecked")
		Map<String, Object> protocolHeaders = (Map<String, Object>) message.get(Message.PROTOCOL_HEADERS);
		if (protocolHeaders == null) {
			throw new HpcAuthenticationException("Invalid Protocol Headers");
		}

		@SuppressWarnings("unchecked")
		ArrayList<String> authorization = (ArrayList<String>) protocolHeaders.get(AUTHORIZATION_HEADER);
		if (authorization == null || authorization.isEmpty()) {
			throw new HpcAuthenticationException("Invalid Authorization Header");
		}

		return authorization.get(0).split(" ");
	}

	/**
	 * Get SM_USER header from a message.
	 *
	 * @param message The RS message.
	 * @return The the SM_USER header.
	 * @throws HpcAuthenticationException on invalid authorization header. This is a
	 *                                    runtime exception.
	 */
	private String getSmUser(Message message) {
		// Determine the authorization type.
		@SuppressWarnings("unchecked")
		Map<String, Object> protocolHeaders = (Map<String, Object>) message.get(Message.PROTOCOL_HEADERS);
		if (protocolHeaders == null) {
			throw new HpcAuthenticationException("Invalid Protocol Headers");
		}

		@SuppressWarnings("unchecked")
		ArrayList<String> header = (ArrayList<String>) protocolHeaders.get("SM_USER");
		if (header == null || header.isEmpty()) {
			return "";
		}

		return header.get(0);
	}

	/**
	 * Get NIHSMSESSION header from a message.
	 *
	 * @param message The RS message.
	 * @return The the NIHSMSESSION header.
	 * @throws HpcAuthenticationException on invalid authorization header. This is a
	 *                                    runtime exception.
	 */
	private String getSmSession(Message message) {
		// Determine the authorization type.
		@SuppressWarnings("unchecked")
		Map<String, Object> protocolHeaders = (Map<String, Object>) message.get(Message.PROTOCOL_HEADERS);
		if (protocolHeaders == null) {
			throw new HpcAuthenticationException("Invalid Protocol Headers");
		}

		@SuppressWarnings("unchecked")
		ArrayList<String> header = (ArrayList<String>) protocolHeaders.get("NIHSMSESSION");
		if (header == null || header.isEmpty()) {
			return "";
		}

		return header.get(0);
	}
}
