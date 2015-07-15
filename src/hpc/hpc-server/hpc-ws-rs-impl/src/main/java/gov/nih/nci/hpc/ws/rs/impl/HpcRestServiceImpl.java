/**
 * HpcRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.exception.HpcException;

import java.net.URI;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * <p>
 * HPC Datasets Registration REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public abstract class HpcRestServiceImpl implements HpcRestServiceContext
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The URI Info context instance.
    private UriInfo uriInfo;
    
    // Enable/Disable stack trace print to exception DTO.
    boolean stackTraceEnabled = false;
    
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Default Constructor disabled.
     * 
     */
    @SuppressWarnings("unused")
	private HpcRestServiceImpl()
    {
    }  
    
    /**
     * Constructor.
     * 
     * @param stackTraceEnabled If set to true, stack trace will be attached to
     *                          exception DTO.
     */
    protected HpcRestServiceImpl(boolean stackTraceEnabled)
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
    protected Response errorResponse(HpcException e)
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
		    	    responseBuilder = 
    	                    Response.status(Response.Status.UNAUTHORIZED);
		    	    break;
		    	    
		       case REQUEST_AUTHENTICATION_FAILED:
		    	    responseBuilder = 
                            Response.status(Response.Status.FORBIDDEN);
   	    break;
		    	   
		       default:
		    	   responseBuilder = 
                   Response.status(Response.Status.INTERNAL_SERVER_ERROR);
		}
		
    	return responseBuilder.entity(exceptionDTO).build();
    }
    
    /**
     * Build a created (HTTP 201) REST response instance.
     *
     * @param id the entity id of the created resource.
     * @return The REST response object.
     */
    protected Response createdResponse(String id)
    {
		UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        URI uri = uriBuilder.path(id).build();
               
		return Response.created(uri).build();
    }
    
    /**
     * Build an 'ok' (HTTP 200) REST response instance.
     *
     * @param entity The entity to attach to the response.
     * @param nullEntityAsNotFound If set to 'true', and entity is null - a 
     *                             'not found' (HTTP 404) will be returned.
     * @return The REST response object.
     */
    protected Response okResponse(Object entity, boolean nullEntityAsNotFound)
    {
		if(entity != null) {
           return Response.ok(entity).
        		           header("Access-Control-Allow-Origin", "*").
        		           header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").
        		           build();
		} else if (nullEntityAsNotFound) {
			       return Response.status(Response.Status.NOT_FOUND).build();
		} else {
			    return Response.ok().build();
		}
    }
    
    //---------------------------------------------------------------------//
    // HpcRestServiceContext Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void setUriInfo(UriInfo uriInfo)
    {
    	this.uriInfo = uriInfo;
    }
}

 