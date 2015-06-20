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
import gov.nih.nci.hpc.exception.HpcErrorType;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Context;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private @Context UriInfo uriInfo;
    
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
    
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Default Constructor.
     * 
     */
    public HpcRestServiceImpl()
    {
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Map HpcException to REST Response
     *
     * @param e The HpcException
     * @return The REST response object.
     */
    protected Response toResponse(HpcException e)
    {
    	if(e.getErrorType() == HpcErrorType.INVALID_INPUT) {
		   return Response.status(Response.Status.BAD_REQUEST).build();
		}
    	
    	return Response.serverError().build();
    }
    
    /**
     * Build a 'created' REST response instance.
     *
     * @param id the entity id of the created resource.
     * @return The REST response object.
     */
    protected Response toCreatedResponse(String id)
    {
		UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        URI uri = uriBuilder.path(id).build();
               
		return Response.created(uri).build();
    }
    
    /**
     * Build an 'ok' REST response instance.
     *
     * @param entity The entity to attach to the response.
     * @return The REST response object.
     */
    protected Response toOkResponse(Object entity)
    {
		if(entity != null) {
           return Response.ok(entity).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT").build();
		} else {
				return Response.noContent().build();
		}
    }
}

 