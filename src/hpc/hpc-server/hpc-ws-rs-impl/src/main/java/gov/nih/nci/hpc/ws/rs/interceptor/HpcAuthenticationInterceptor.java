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

import gov.nih.nci.hpc.bus.HpcUserBusService;
import gov.nih.nci.hpc.dto.user.HpcUserCredentialsDTO;
import gov.nih.nci.hpc.exception.HpcAuthenticationException;
import gov.nih.nci.hpc.exception.HpcException;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
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

    // The User Business Service instance.
	@Autowired
    private HpcUserBusService userBusService = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

    /**
     * Default Constructor.
     * 
     */
    public HpcAuthenticationInterceptor() 
    {
        super(Phase.RECEIVE);
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
    	Map<String, List<String>> headers = (Map<String, List<String>>)message.get(Message.PROTOCOL_HEADERS);
        List<String> auth = headers.get("authorization");
        if(auth == null || auth.size() == 0)
        	throw new HpcAuthenticationException("No user credentials provided");
        try {
            String authString = auth.get(0);
            String[] authParts = authString.split("\\s+");
            String authInfo = authParts[1];
            // Decode the data back to original string
             byte[] bytes = Base64.getDecoder().decode(authInfo);
	        String decodedAuth = new String(bytes);
	        String[] loginParts = authString.split(":");
	        String userId = loginParts[0];
	        String password = loginParts[1];
        
	        // Authenticate the client.
        	HpcUserCredentialsDTO dto = new HpcUserCredentialsDTO();
        	dto.setUserName(userId);
        	dto.setPassword(password);
        	 if(!userBusService.authenticate(dto)) {
    		    throw new HpcAuthenticationException("Invalid user credentials");
    	     }
    		
        } catch(HpcException e) {
    	        throw new HpcAuthenticationException("Authentication failed", e);  
        }
    }
} 