/**
 * HpcProfileInterceptor.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.interceptor;

import javax.servlet.http.HttpServletRequest;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Authentication Interceptor.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcProfileInterceptor 
             extends AbstractPhaseInterceptor<Message> 
{
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//   
	
	// The attribue name to save the service invoke time.
	private static String SERVIVE_INVOKE_TIME_MC_ATTRIBUTE = 
			              "gov.nih.nci.hpc.ws.rs.interceptor.HpcProfileInterceptor.rerviceInvokeTime";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The interceptor phase
    private String phase = null;
    
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

    /**
     * Default Constructor disabled.
     * 
     * @throws HpcException Constructor disabled.
     */
    private HpcProfileInterceptor() throws HpcException
    {
    	super(Phase.RECEIVE);
        throw new HpcException("Constructor disabled",
        	                   HpcErrorType.SPRING_CONFIGURATION_ERROR); 
    }
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param phase The CXF phase. It is expected to have 2 instances of this interceptor.
     *        The first with Phase.RECEIVE to capture the start time of a RS call. The second instance 
     *        with Phase.SEND_ENDING to stop the timer and log performance result of the RS service call.
     * @throws HpcException If provided phase is not Phase.RECEIVE or Phase.SEND_ENDING.
     */
    private HpcProfileInterceptor(String phase) throws HpcException
    {
        super(phase);
        this.phase = phase;
        
        if(phase.equals(Phase.RECEIVE)) {
           // Start the service profiling before we authenticate the caller.
           getBefore().add(HpcAuthenticationInterceptor.class.getName());
        } else if(phase.equals(Phase.SEND_ENDING)) {
        	      // Stop the service profiling after we cleaned up resources post service call.
        	      getAfter().add(HpcCleanupInterceptor.class.getName());
        } else {
        	    throw new HpcException("Invalid CXF phase: " + phase,
	                                   HpcErrorType.SPRING_CONFIGURATION_ERROR); 
        }
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
    	HttpServletRequest request = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
    	
    	if(phase.equals(Phase.RECEIVE)) {
    	   logger.error("ERAN: in " + request.getMethod() );
    	   message.getExchange().put(SERVIVE_INVOKE_TIME_MC_ATTRIBUTE, System.currentTimeMillis());
    	} else if(phase.equals(Phase.SEND_ENDING)) {
    		      Long startTime = (Long) message.getExchange().get(SERVIVE_INVOKE_TIME_MC_ATTRIBUTE);
    		      if(startTime != null) {
    		    	 logger.error("Eran: out " + (System.currentTimeMillis() - startTime));
    		      }
    	}
    }
} 
