/**
 * HpcDatasetsRegistrationRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import gov.nih.nci.hpc.ws.rs.HpcDataRegistrationRestService;
import gov.nih.nci.hpc.dto.service.HpcDataRegistrationInputDTO;
import gov.nih.nci.hpc.dto.service.HpcDataRegistrationOutputDTO;
import gov.nih.nci.hpc.dto.types.HpcManagedDataType;
import gov.nih.nci.hpc.bus.HpcDataRegistrationService;
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
import gov.nih.nci.hpc.dto.types.HpcFacility;
import gov.nih.nci.hpc.dto.types.HpcDataTransfer;
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
public class HpcDataRegistrationRestServiceImpl 
             implements HpcDataRegistrationRestService
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Registration Business Service instance.
    private HpcDataRegistrationService registrationBusService = null;
    
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
    private HpcDataRegistrationRestServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }  
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param registrationBusService The registration business service.
     * 
     * @throws HpcException If the bus service is not provided by Spring.
     */
    private HpcDataRegistrationRestServiceImpl(
    		       HpcDataRegistrationService registrationBusService)
                   throws HpcException
    {
    	if(registrationBusService == null) {
    	   throw new HpcException("Null HpcDataRegistrationService instance",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	this.registrationBusService = registrationBusService;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataRegistrationRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public HpcDataRegistrationOutputDTO getRegisterdData(String id)
    {
    	HpcDataRegistrationOutputDTO dto = new HpcDataRegistrationOutputDTO();
    	
    	HpcDataset ds = new HpcDataset();
    	HpcDatasetLocation loc = new HpcDatasetLocation();
    	loc.setFacility(HpcFacility.SHADY_GROVE);
    	loc.setEndpoint("nihfnlcr#gridftp1");
    	loc.setDataTransfer(HpcDataTransfer.GLOBUS);
    	ds.setLocation(loc);
    	ds.setName("SEQUENCING file name");
    	ds.setType(HpcDatasetType.RAW_SEQUENCING);
    	dto.getDatasets().add(ds);
    	dto.setType(HpcManagedDataType.EXPERIMENT);
    	return dto;
    }
    
    @Override
    public Response registerData(
    		        HpcDataRegistrationInputDTO registrationInputDTO)
    {	
		logger.info("Invoking RS: POST /registration");
		try {
			 registrationBusService.registerData(registrationInputDTO);
		} catch(HpcException e) {
			    logger.error("RS: POST /registration failed:", e);
			    return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		// TODO : Implement
		UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        URI metadataUri = uriBuilder.path("9988").build();
               
		return Response.created(metadataUri).build();
	}
}

 