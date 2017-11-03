/**
 * HpcAuthenticationExceptionMapper.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.provider;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.exception.HpcAuthenticationException;
import gov.nih.nci.hpc.exception.HpcException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * <p>
 * Mapping HPC Exceptions to REST response.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcAuthenticationExceptionMapper extends HpcExceptionMapper
             implements ExceptionMapper<HpcAuthenticationException>
{
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
     
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param stackTraceEnabled If set to true, stack trace will be attached to
     *                          exception DTO.
     */
    public HpcAuthenticationExceptionMapper(boolean stackTraceEnabled)
    {
    	super(stackTraceEnabled);
    }  
    
    /**
     * Default Constructor is disabled.
     * 
     * @throws HpcException Constructor is disabled.
     */
    public HpcAuthenticationExceptionMapper() throws HpcException
    {
    	throw new HpcException("Default Constructor Disabled", 
    			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // ExceptionMapper<HpcAuthenticationException>
    //---------------------------------------------------------------------//  
    
    @Override
    public Response toResponse(HpcAuthenticationException exception) 
    {
        HpcExceptionDTO exceptionDTO = new HpcExceptionDTO();
    	exceptionDTO.setMessage("Access Denied: " + exception.getMessage());
    	exceptionDTO.setErrorType(HpcErrorType.REQUEST_AUTHENTICATION_FAILED);
    	
    	if(getStackTraceEnabled()) {
     	   exceptionDTO.setStackTrace(exception.getStackTraceString());
     	}
    	
        return Response.status(Response.Status.UNAUTHORIZED).entity(exceptionDTO).
        	   type(getAcceptedMediaType()).build();
    }
}
