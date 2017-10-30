/**
 * HpcException.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * <p>
 * The HPC authentication exception.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcAuthenticationException extends RuntimeException implements java.io.Serializable
{
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// UID.
	private static final long serialVersionUID = 1L;
	
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//

    /**
     * Default constructor is disabled.
     */
    @SuppressWarnings("unused")
	private HpcAuthenticationException() 
    {
    }

    /**
     * Constructs a new HpcAuthentcationException with a given message.
     *
     * @param message The message for the exception, normally the cause.
     */
    public HpcAuthenticationException(String message) 
    {
        super(message);
    }
    
    /**
     * Constructs a new HpcAuthenticationException with a given message, and
     * a Throwable cause
     *
     * @param message The message for the exception.
     * @param cause The root cause Throwable.
     */
    public HpcAuthenticationException(String message, Throwable cause) 
    {
        super(message, cause);
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Get the stack trace.
     *
     * @return The stack trace.
     */
    public String getStackTraceString() 
    {
    	StringWriter writer = new StringWriter();
    	printStackTrace(new PrintWriter(writer));
    	return writer.toString();
    }
}


