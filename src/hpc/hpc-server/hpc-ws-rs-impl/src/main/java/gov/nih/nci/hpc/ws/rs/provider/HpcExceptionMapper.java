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

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
/**
 * <p>
 * Mapping HPC Exceptions to REST response.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcExceptionMapper 
             implements ExceptionMapper<HpcAuthenticationException>
{
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // Enable/Disable stack trace print to exception DTO.
    private boolean stackTraceEnabled = false;
    
    // HTTP headers context instance.
	@Context
    private HttpHeaders headers;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
     
    /**
     * Default Constructor disabled.
     * 
     */
	private HpcExceptionMapper() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }  
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param stackTraceEnabled If set to true, stack trace will be attached to
     *                          exception DTO.
     */
    private HpcExceptionMapper(boolean stackTraceEnabled)
    {
    	this.stackTraceEnabled = stackTraceEnabled;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Return an error REST response instance. 
     * Map HpcException to the appropriate HTTP code.
     *
     * @param e The HpcException
     * @return The REST response object.
     */
    public Response.ResponseBuilder toResponse(HpcException e)
    {
    	// Map the exception to a DTO.
    	HpcExceptionDTO exceptionDTO = new HpcExceptionDTO();
    	exceptionDTO.setMessage(e.getMessage());
    	exceptionDTO.setErrorType(e.getErrorType());
    	exceptionDTO.setRequestRejectReason(e.getRequestRejectReason());
    	
    	if(stackTraceEnabled) {
    	   exceptionDTO.setStackTrace(e.getStackTraceString());
    	}
    	
    	Response.ResponseBuilder responseBuilder = null;
		switch(e.getErrorType()) {
		       case INVALID_REQUEST_INPUT:
		       case REQUEST_REJECTED:
		    	   responseBuilder = 
		    	           Response.status(Response.Status.BAD_REQUEST);	
		    	   break;
		    	   
		       case UNAUTHORIZED_REQUEST:
		       case REQUEST_AUTHENTICATION_FAILED:
		    	    responseBuilder = 
    	                    Response.status(Response.Status.UNAUTHORIZED);
		    	    break;
		    	    
		       default:
		    	   responseBuilder = 
                   Response.status(Response.Status.INTERNAL_SERVER_ERROR);
		}
		
    	return responseBuilder.entity(exceptionDTO);
    }
    
    //---------------------------------------------------------------------//
    // ExceptionMapper<HpcAuthenticationException>
    //---------------------------------------------------------------------//  
    
    @Override
    public Response toResponse(HpcAuthenticationException exception) 
    {
        HpcExceptionDTO exceptionDTO = new HpcExceptionDTO();
    	exceptionDTO.setMessage("Access Denied: " + exception.getMessage());
    	exceptionDTO.setErrorType(HpcErrorType.REQUEST_AUTHENTICATION_FAILED);
    	
    	if(stackTraceEnabled) {
     	   exceptionDTO.setStackTrace(exception.getStackTraceString());
     	}
    	
        return Response.status(Response.Status.UNAUTHORIZED).entity(exceptionDTO).
        	   type(getAcceptedMediaType()).build();
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Get accepted media type.
     *
     * @return A The dataset Mongo collection.
     */
    private MediaType getAcceptedMediaType()
    {
    	return headers != null && headers.getAcceptableMediaTypes() != null &&
  			   !headers.getAcceptableMediaTypes().isEmpty() &&
  			   headers.getAcceptableMediaTypes().get(0).equals(MediaType.APPLICATION_XML_TYPE) ?
  			   MediaType.APPLICATION_XML_TYPE : MediaType.APPLICATION_JSON_TYPE;
    }
}
