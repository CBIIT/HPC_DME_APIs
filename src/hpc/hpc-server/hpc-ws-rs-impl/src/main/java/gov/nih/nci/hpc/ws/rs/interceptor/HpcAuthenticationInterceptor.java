/**
 * HpcAuthentcationInterceptor.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.interceptor;

import gov.nih.nci.hpc.bus.HpcSecurityBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcAuthenticationException;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.ArrayList;
import java.util.Map;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.interceptor.security.SecureAnnotationsInterceptor;
import org.apache.cxf.interceptor.security.SimpleAuthorizingInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Authentication Interceptor.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcAuthenticationInterceptor 
             extends AbstractPhaseInterceptor<Message> 
{
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//   
	
	// HTTP header attributes.
	private static String AUTHORIZATION_HEADER = "Authorization";
	
	// Authorization Types
	private static String BASIC_AUTHORIZATION = "Basic";
	private static String TOKEN_AUTHORIZATION = "Bearer";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Security Business Service instance.
	@Autowired
    private HpcSecurityBusService securityBusService = null;
	
	// LDAP authentication on/off switch.
	private boolean ldapAuthentication = true;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

    /**
     * Default Constructor disabled.
     * 
     * @throws HpcException Constructor disabled.
     */
    private HpcAuthenticationInterceptor() throws HpcException
    {
    	super(Phase.RECEIVE);
        throw new HpcException("Constructor disabled",
        	                   HpcErrorType.SPRING_CONFIGURATION_ERROR); 
    }
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param ldapAuthentication Enable LDAP authentication indicator.
     */
    private HpcAuthenticationInterceptor(boolean ldapAuthentication) 
    {
        super(Phase.RECEIVE);
        this.ldapAuthentication = ldapAuthentication;

        // We need to authenticate first (this interceptor), and then authorize (other 2 interceptors).
        getBefore().add(SecureAnnotationsInterceptor.class.getName());
        getBefore().add(SimpleAuthorizingInterceptor.class.getName());
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // AbstractPhaseInterceptor<Message> Interface Implementation
    //---------------------------------------------------------------------//  

    @Override
    public void handleMessage(Message message) 
    {
    	// Authenticate the caller (if configured to do so) and populate the request context.
        try {
        	 authenticate(message);
             
             // Set a security context with the user's role.
             HpcSecurityContext sc = new HpcSecurityContext(securityBusService.getAuthenticationResponse().getUserRole().value());
             message.put(SecurityContext.class, sc);

        } catch(HpcException e) {
        	    throw new HpcAuthenticationException(e.getMessage(), e);
        	    
        } catch(HpcAuthenticationException ex) {
    	        throw ex;
        	    
        } catch(Exception e) {
   	            throw new HpcAuthenticationException("Authentication failed", e);
        }
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Authenticate the caller.
     * 
     * @param message The RS message.
     * @return HpcAuthenticationResponseDTO.
     * @throws HpcException on unsupported authorization type request.
     */
    private void authenticate(Message message) throws HpcException
    {
		String[] authorization = getAuthorization(message);
    	String authorizationType = authorization[0];
    	
    	// Authenticate the caller (if configured to do so) and populate the request context.
        if(authorizationType.equals(BASIC_AUTHORIZATION)) {
           authenticate(message.get(AuthorizationPolicy.class));
        } else if(authorizationType.equals(TOKEN_AUTHORIZATION)) {
                  authenticate(authorization[1]);
        } else {
                throw new HpcAuthenticationException("Invalid Authorization Type: " + authorizationType); 
        }
    }
    
    /**
     * Perform a basic authentication w/ user-name and password.
     * 
     * @param policy The policy holding user name anf password.
     * @throws HpcException on authentication failure.
     */
    private void authenticate(AuthorizationPolicy policy) throws HpcException
    {
    	String userName = null, password = null;
    	if(policy != null) {
    	   userName = policy.getUserName();
    	   password = policy.getPassword();
    	}
    	
    	securityBusService.authenticate(userName, password, ldapAuthentication);
    }
    
    /**
     * Perform a token authentication (JWT).
     * 
     * @param token The JWT token.
     * @throws HpcException on authentication failure.
     */
    private void authenticate(String token) throws HpcException
    {
    	securityBusService.authenticate(token);
    }
    
    /**
     * Get Authorization array from a message.
     * 
     * @param message The RS message.
     * @return The authorzation type of the message.
     * @throws HpcAuthenticationException on invalid authorization header.
     */
    private String[] getAuthorization(Message message) throws HpcAuthenticationException
    {
		// Determine the authorization type.
		@SuppressWarnings("unchecked")
		Map<String, Object> protocolHeaders = 
				            (Map<String, Object>) message.get(Message.PROTOCOL_HEADERS); 
		if(protocolHeaders == null) {
		   throw new HpcAuthenticationException("Invalid Protocol Headers"); 
		}
		
		@SuppressWarnings("unchecked")
		ArrayList<String> authorization = 
				          (ArrayList<String>) protocolHeaders.get(AUTHORIZATION_HEADER);
		if(authorization == null || authorization.isEmpty()) {
		   throw new HpcAuthenticationException("Invalid Authorization Header"); 
		}

		return authorization.get(0).split(" ");
    }
} 
