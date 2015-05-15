/**
 * HpcDatasetsRegistrationRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.deploy.rs.impl;

import gov.nih.nci.hpc.deploy.rs.HpcDatasetsRegistrationRestService;
import gov.nih.nci.hpc.dto.api.HpcDatasetsRegistrationInputDTO;
import gov.nih.nci.hpc.bus.HpcDatasetsRegistrationService;
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

import gov.nih.nci.hpc.dto.types.HpcDataset;
import gov.nih.nci.hpc.dto.types.HpcDatasetLocation;
import gov.nih.nci.hpc.dto.types.HpcDataCenter;
import gov.nih.nci.hpc.dto.types.HpcDatasetType;

/**
 * <p>
 * HPC Datasets Registration REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

@Path("/")
public class HpcDatasetsRegistrationRestServiceImpl 
             implements HpcDatasetsRegistrationRestService
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Datasets Registration Service instance.
    private HpcDatasetsRegistrationService registrationService = null;
    
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
    private HpcDatasetsRegistrationRestServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }  
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param registrationService The datasets registration service.
     * 
     * @throws HpcException If the app service is not provided by Spring.
     */
    private HpcDatasetsRegistrationRestServiceImpl(
    		           HpcDatasetsRegistrationService registrationService)
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
    public Response registerDatasets(
    		        HpcDatasetsRegistrationInputDTO registrationInputDTO)
    {	
		logger.info("Invoking RS: POST /registration");
		try {
			 registrationService.registerDatasets(registrationInputDTO);
		} catch(HpcException e) {
			    logger.error("RS: POST /registration failed:", e);
			    return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		// TODO : Implement
		UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        URI metadataUri = uriBuilder.path("9988").build();
               
		return Response.created(metadataUri).build();
	}
    
    @Override
    public HpcDatasetsRegistrationInputDTO getRegistration(String id)
    {
    	HpcDatasetsRegistrationInputDTO dto = new HpcDatasetsRegistrationInputDTO();
    	
    	HpcDataset ds = new HpcDataset();
    	HpcDatasetLocation loc = new HpcDatasetLocation();
    	loc.setDataCenter(HpcDataCenter.SHADY_GROVE);
    	loc.setPath("/usr/local/datasets");
    	ds.setLocation(loc);
    	ds.setName("SEQUENCING file name");
    	ds.setType(HpcDatasetType.RAW_SEQUENCING);
    	dto.getDatasets().add(ds);
    	return dto;
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

 