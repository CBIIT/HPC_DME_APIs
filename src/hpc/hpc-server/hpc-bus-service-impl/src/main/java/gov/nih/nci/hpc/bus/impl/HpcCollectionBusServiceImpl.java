/**
 * HpcCollectionBusServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.bus.HpcCollectionBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcCollectionService;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Collection Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcCollectionBusServiceImpl implements HpcCollectionBusService
{         
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // Application service instances.
	
	@Autowired
    private HpcCollectionService collectionService = null;
	
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
    private HpcCollectionBusServiceImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcCollectionBusService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public String registerCollection(
    					  String path,
    					  List<HpcMetadataEntry> metadataEntries)  
    		              throws HpcException
    {
    	logger.info("Invoking registerCollection(List<HpcMetadataEntry>): " + 
    			    metadataEntries);
    	
    	// Input validation.
    	if(path == null || metadataEntries == null) {
    	   throw new HpcException("Null path or HpcCollectionRegistrationDTO",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Create a collection directory.
    	collectionService.createDirectory(path);
    	
    	// Attach the metadata.
    	collectionService.addMetadata(path, metadataEntries);
    	
    	return "collection-id";
    }
}

 