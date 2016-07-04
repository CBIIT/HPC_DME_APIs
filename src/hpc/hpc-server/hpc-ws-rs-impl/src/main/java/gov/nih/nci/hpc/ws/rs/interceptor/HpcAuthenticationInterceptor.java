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

import java.util.ArrayList;
import java.util.Map;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import gov.nih.nci.hpc.bus.HpcSecurityBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.exception.HpcAuthenticationException;
import gov.nih.nci.hpc.exception.HpcException;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.interceptor.security.SecureAnnotationsInterceptor;
import org.apache.cxf.interceptor.security.SimpleAuthorizingInterceptor;
import org.apache.cxf.jaxrs.ext.MessageContext;
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
        
        // We need to authenticate first, and then authorize.
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
    	// Get and validate the authorization policy set by the caller.
    	AuthorizationPolicy policy = message.get(AuthorizationPolicy.class);
    	//SecurityContext sc1 = message.get(SecurityContext.class);
    	//Map<String, Object> ph = (Map<String, Object> )message.get(Message.PROTOCOL_HEADERS);
    	//ArrayList auth = (ArrayList) ph.get("Authorization");
    	
   
    		
    	
    	
    	
    	
    	
    	String userName = null, password = null;
    	if(policy != null) {
    		userName = policy.getUserName();
    	    password = policy.getPassword();
    	}
    	
    	// Authenticate the caller (if configured to do so) and populate the request context.
        try {
        	 HpcAuthenticationResponseDTO authenticationResponse =
        	    securityBusService.authenticate(userName, password, ldapAuthentication);
             if(!authenticationResponse.getAuthenticated()) {
                throw new HpcAuthenticationException("Invalid NCI user credentials"); 
             }
             
             // Set a security context with the user's role.
             HpcSecurityContext sc = new HpcSecurityContext(authenticationResponse.getUserRole().value());
             message.put(SecurityContext.class, sc);
             
        } catch(HpcException e) {
        	    throw new HpcAuthenticationException(e.getMessage(), e);
        	    
        } catch(HpcAuthenticationException ex) {
    	        throw ex;
        	    
        } catch(Throwable t) {
   	            throw new HpcAuthenticationException("LDAP authentication failed", t);
       }
    }
} 