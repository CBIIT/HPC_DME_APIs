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

import gov.nih.nci.hpc.exception.HpcAuthenticationException;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class HpcAuthenticationInterceptor 
             extends AbstractPhaseInterceptor<Message> {

    /**
     * Default Constructor disabled.
     * 
     */
    public HpcAuthenticationInterceptor() 
    {
        super(Phase.RECEIVE);
    }
    
    //---------------------------------------------------------------------//
    // AbstractPhaseInterceptor<Message> Interface Implementation
    //---------------------------------------------------------------------//  

    @Override
    public void handleMessage(Message message) 
    {
        AuthorizationPolicy policy = message.get(AuthorizationPolicy.class);

    if (policy == null) {
        throw new HpcAuthenticationException("No user credentials provided");
    }

    //String user = policy.getUserName();

    }
    
} 