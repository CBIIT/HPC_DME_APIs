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

import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.interceptor.HpcAPIVersionInterceptor;
import gov.nih.nci.hpc.ws.rs.provider.HpcExceptionMapper;

import java.net.URI;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Datasets Registration REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public abstract class HpcRestServiceImpl
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The URI Info context instance.
	@Context
    private UriInfo uriInfo = null;
	
	// The exception mapper (Exception to HTTP error code) instance.
	@Autowired
	private HpcExceptionMapper exceptionMapper = null;
	
	// The API version interceptor.
	@Autowired
	private HpcAPIVersionInterceptor apiVersion = null;
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
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
     * @param nullEntityAsNoContent If set to 'true', and entity is null - a 
     *                             'not found' (HTTP 204) will be returned.
     * @return The REST response object.
     */
    protected Response okResponse(Object entity, boolean nullEntityAsNoContent)
    {
		if(entity != null) {
           return Response.ok(entity).
        		           header("Access-Control-Allow-Origin", "*").
        		           header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
		} else if(nullEntityAsNoContent) {
			      return Response.status(Response.Status.NO_CONTENT).build();
		} else {
			    return Response.ok().build();
		}
    }
    
    /**
     * Return an error REST response instance. 
     * Map HpcException to the appropriate HTTP code.
     *
     * @param e The HpcException
     * @return The REST response object.
     */
    protected Response errorResponse(HpcException e)
    {
    	// For some reason, the API Version Interceptor doesn't pick up the error response.
    	// As a workaround - we call it here directly.
    	return apiVersion.header(exceptionMapper.toResponse(e)).build();
    }
    
    /**
     * Convert a path to an absolute 'path' (i.e. it begins with '/')
     *
     * @param path The path.
     * @return The absolute path.
     */
	protected String toAbsolutePath(String path)
	{
		StringBuilder buf = new StringBuilder();

		if(path.isEmpty() || path.charAt(0) != '/') {
		   buf.append('/');
		} 
		buf.append(path);
		return buf.toString();
	}
}

 