/**
 * HpcDatasetRegistrationRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.deploy.rs.impl;

import gov.nih.nci.hpc.deploy.rs.HpcDatasetRegistrationRestService;
import gov.nih.nci.hpc.dto.api.HpcDatasetsRegistrationInputDTO;
import gov.nih.nci.hpc.bus.HpcDatasetRegistrationService;
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
 * HPC Dataset Registration REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

@Path("/")
public class HpcDatasetRegistrationRestServiceImpl 
             implements HpcDatasetRegistrationRestService
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Metadata Application Service instance.
    private HpcDatasetRegistrationService registrationService = null;
    
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
    private HpcDatasetRegistrationRestServiceImpl() throws HpcException
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
    private HpcDatasetRegistrationRestServiceImpl(
    		          HpcDatasetRegistrationService registrationService)
                      throws HpcException
    {
    	if(registrationService == null) {
    	   throw new HpcException("Null HpcDatasetRegistrationService instance",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	this.registrationService = registrationService;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDatasetRegistrationRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response registerDataset(
    		        HpcDatasetsRegistrationInputDTO registrationInputDTO)
    {	
		logger.info("Invoking POST /registration");
		try {
			 registrationService.registerDataset(registrationInputDTO);
		} catch(HpcException e) {
			
		}
		UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        URI metadataUri = uriBuilder.path("9988").build();
               
		return Response.created(metadataUri).build();
	}
	
   // @Override
    //public Response postMetadata(HpcMetadataDTO metadata/*, @Context UriInfo uriInfo*/)
    //{
		//logger.info("Invoking POST /metadata");
		
		//metadataService.addMetadata(metadata);
		//UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        //URI metadataUri = uriBuilder.path("9988").build();
               
		//return Response.created(metadataUri).build();
    //}
}

 