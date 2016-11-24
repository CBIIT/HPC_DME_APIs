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

import gov.nih.nci.hpc.bus.HpcDataManagementNewBusService;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementNewService;
import gov.nih.nci.hpc.service.HpcMetadataService;

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

public class HpcDataManagementNewBusServiceImpl implements HpcDataManagementNewBusService
{  
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// The Data Management Application Service Instance.
	@Autowired
    private HpcDataManagementNewService dataManagementService = null;
	
	// The Metadata Application Service Instance.
	@Autowired
    private HpcMetadataService metadataService = null;
	
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
    private HpcDataManagementNewBusServiceImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataManagementBusService Interface Implementation
    //---------------------------------------------------------------------//  

    @Override
    public boolean registerCollection(String path,
    				                  List<HpcMetadataEntry> metadataEntries)  
    		                         throws HpcException
    {
    	logger.info("Invoking registerCollection(List<HpcMetadataEntry>): " + 
    			    metadataEntries);
    	
    	// Input validation.
    	if(path == null || path.isEmpty() || metadataEntries == null || metadataEntries.size() == 0) {
    	   throw new HpcException("Null path or metadata entries",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Create a collection directory.
    	boolean created = dataManagementService.createDirectory(path);
    	
    	// Attach the metadata.
    	if(created) {
    	   boolean registrationCompleted = false; 
    	   try {
    		    // Assign system account as an additional owner of the collection.
    		    dataManagementService.assignSystemAccountPermission(path);
    		    
    		    // Add user provided metadata.
    	        metadataService.addMetadataToCollection(path, metadataEntries);
    	        
    	        // Generate system metadata and attach to the collection.
       	        metadataService.addSystemGeneratedMetadataToCollection(path);
       	        
       	        // Validate the collection hierarchy.
       	        String doc = metadataService.getCollectionSystemGeneratedMetadata(path).getRegistrarDOC();
       	        dataManagementService.validateHierarchy(path, doc, false);
       	        
       	        registrationCompleted = true;
       	        
    	   } finally {
			          if(!registrationCompleted) {
				         // Collection registration failed. Remove it from Data Management.
				         dataManagementService.delete(path);
			          }
	       }
       	   
    	} else {
    		    metadataService.updateCollectionMetadata(path, metadataEntries);
    	}
    	
    	return created;
    }
    
    @Override
    public HpcCollectionDTO getCollection(String path) throws HpcException
    {
    	logger.info("Invoking getCollection(String): " + path);
    	
    	// Input validation.
    	if(path == null) {
    	   throw new HpcException("Null collection path",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	HpcCollection collection = dataManagementService.getCollection(path);
    	if(collection == null) {
      	   return null;
      	}
     	
     	// Get the metadata.
     	HpcMetadataEntries metadataEntries = 
     	   metadataService.getCollectionMetadataEntries(collection.getAbsolutePath());
 		
     	HpcCollectionDTO collectionDTO = new HpcCollectionDTO();
     	collectionDTO.setCollection(collection);
        collectionDTO.setMetadataEntries(metadataEntries);
     	
     	return collectionDTO;
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
}

 