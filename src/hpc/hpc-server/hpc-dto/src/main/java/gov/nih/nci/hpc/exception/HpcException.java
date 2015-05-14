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

/**
 * <p>
 * The HPC exception.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcException extends Exception implements java.io.Serializable
{
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The error type value.
    private HpcErrorType errorType = null;

    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//

    /**
     * Default constructor is disabled.
     */
    private HpcException() 
    {
    }

    /**
     * Constructs a new HpcException with a given message.
     *
     * @param message The message for the exception, normally the cause.
     * @param errorType The type of the error, often the subsystem that is 
     *        the source of the error.
     */
    public HpcException(String message, HpcErrorType errorType) 
    {
        super(message);
        setErrorType(errorType);
    }

    /**
     * Constructs a new HpcException with a given message, and
     * a Throwable cause
     *
     * @param message The message for the exception.
     * @param cause The root cause Throwable.
     */
    public HpcException(String message, Throwable cause) 
    {
        super(message, cause);
        
        // Propagate the error type, if the cause is a HpcException.
        if(cause instanceof HpcException) {
           setErrorType(((HpcException) cause).getErrorType());
        }
    }
    
    /**
     * Constructs a new HpcException with a given message, error type and
     * a Throwable cause
     *
     * @param message The message for the exception.
     * @param errorType The type of the error, often the subsystem that is 
     *        the source of the error.
     * @param cause The root cause Throwable.
     */
    public HpcException(String message, HpcErrorType errorType, 
    		            Throwable cause) 
    {
        super(message, cause);
        setErrorType(errorType);
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    @Override
    public String toString()
    {
        return super.toString() + "[" + errorType + "]";
    }
    
    /**
     * Get the error type
     *
     * @return The error type.
     */
    public HpcErrorType getErrorType()
    {
        return errorType;
    }    
    
    /**
     * Set the error type
     *
     * @param errorType The error type.
     */
    public void setErrorType(HpcErrorType errorType)
    {
        this.errorType = errorType;
    }        
}


