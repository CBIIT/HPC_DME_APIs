/**
 * HpcDataManagementBusServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.domain.dataset.HpcDataManagementEntity;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.dataset.HpcDataManagementEntitiesDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcUserService;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Data Management Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataManagementBusServiceImpl implements HpcDataManagementBusService
{  
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // Application service instances.
	
	@Autowired
    private HpcUserService userService = null;
	
	@Autowired
    private HpcDataTransferService dataTransferService = null;
	
	@Autowired
    private HpcDataManagementService dataManagementService = null;
	
    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
    private HpcDataManagementBusServiceImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcCollectionBusService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void registerCollection(String path,
    					           List<HpcMetadataEntry> metadataEntries)  
    		                      throws HpcException
    {
    	logger.info("Invoking registerCollection(List<HpcMetadataEntry>): " + 
    			    metadataEntries);
    	
    	// Input validation.
    	if(path == null || metadataEntries == null) {
    	   throw new HpcException("Null path or metadata entries",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Create a collection directory.
    	dataManagementService.createDirectory(path);
    	
    	// Attach the metadata.
    	dataManagementService.addMetadataToCollection(path, metadataEntries);
    }
    
    @Override
    public HpcDataManagementEntitiesDTO getCollections(
    		                               String userId,
                                           List<HpcMetadataEntry> metadataEntryQueries) 
                                           throws HpcException
    {
    	logger.info("Invoking getCollections(List<HpcMetadataEntry>): " + 
    			    metadataEntryQueries);
    	
    	// Input validation.
    	if(metadataEntryQueries == null) {
    	   throw new HpcException("Null metadata entry queries",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	return toDTO(dataManagementService.getCollections(metadataEntryQueries));
    }
    
    @Override
    public void registerDataObject(String path,
    		                       HpcDataObjectRegistrationDTO dataObjectRegistrationDTO)  
    		                      throws HpcException
    {
    	logger.info("Invoking registerDataObject(HpcDataObjectRegistrationDTO): " + 
    			    dataObjectRegistrationDTO);
    	
    	// Input validation.
    	if(path == null || dataObjectRegistrationDTO == null) {
    	   throw new HpcException("Null path or dataObjectRegistrationDTO",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Append the path to the destination's base path (if a base path provided).
    	if(dataObjectRegistrationDTO.getLocations() != null &&
    	   dataObjectRegistrationDTO.getLocations().getDestination() != null) {
    	   String basePath = dataObjectRegistrationDTO.getLocations().getDestination().getPath();	
    	   dataObjectRegistrationDTO.getLocations().getDestination().setPath(
    		                                           basePath != null ? basePath + path : path);
    	}
    	
    	// Create a data object file (in the data management system).
    	dataManagementService.createFile(path);
    	
		// Transfer the file. 
        dataTransferService.transferData(dataObjectRegistrationDTO.getLocations());				 
    	
    	// Attach the user provided metadata.
    	dataManagementService.addMetadataToDataObject(
    			                 path, 
    			                 dataObjectRegistrationDTO.getMetadataEntries());
    	
    	// Create and attach the file source and location metadata.
    	dataManagementService.addFileLocationsMetadataToDataObject(
    			                 path, 
    			                 dataObjectRegistrationDTO.getLocations().getDestination(),
    			                 dataObjectRegistrationDTO.getLocations().getSource()); 
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Create a data management entities DTO from a domain object.
     * 
     * @param dataManagementEntities the domain object.
     *
     * @return The DTO.
     */
    private HpcDataManagementEntitiesDTO toDTO(List<HpcDataManagementEntity> entities) 
    {
    	HpcDataManagementEntitiesDTO entitiesDTO = new HpcDataManagementEntitiesDTO();
    	entitiesDTO.getEntities().addAll(entities);
    	return entitiesDTO;
    }
}

 