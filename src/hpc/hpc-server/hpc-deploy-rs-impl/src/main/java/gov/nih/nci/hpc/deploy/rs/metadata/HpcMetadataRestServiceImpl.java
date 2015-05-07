/**
 * HpcMetadataRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.deploy.rs.metadata;

import gov.nih.nci.hpc.deploy.rs.HpcMetadataRestService;
import gov.nih.nci.hpc.dto.metadata.HpcMetadataDTO;
import gov.nih.nci.hpc.service.HpcMetadataService;
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
 * HPC Metadata REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

@Path("/")
public class HpcMetadataRestServiceImpl implements HpcMetadataRestService
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Metadata Application Service instance.
    private HpcMetadataService metadataService = null;
    
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
     * @throws HpcException Constructor is disabled.
     */
    private HpcMetadataRestServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }  
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param metadataService The metadata application service instance.
     * 
     * @throws HpcException If the app service is not provided by Spring.
     */
    private HpcMetadataRestServiceImpl(HpcMetadataService metadataService)
                                      throws HpcException
    {
    	if(metadataService == null) {
    	   throw new HpcException("Null HpcMetadataService instance",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	this.metadataService = metadataService;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcMetadataRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public HpcMetadataDTO getMetadata(String id)
    {	
		logger.info("Invoking GET /metadata/{id}");
		return metadataService.getMetadata(id);
	}
	
    @Override
    public Response postMetadata(HpcMetadataDTO metadata/*, @Context UriInfo uriInfo*/)
    {
		logger.info("Invoking POST /metadata");
		
		metadataService.addMetadata(metadata);
		UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        URI metadataUri = uriBuilder.path("9988").build();
               
		return Response.created(metadataUri).build();
    }
}

 