/**
 * HpcDataSearchRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import gov.nih.nci.hpc.bus.HpcDataSearchBusService;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDataSearchRestService;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Data Search REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataSearchRestServiceImpl extends HpcRestServiceImpl
             implements HpcDataSearchRestService
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//   
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Business Service instance.
	@Autowired
    private HpcDataSearchBusService dataSearchBusService = null;
	
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
    private HpcDataSearchRestServiceImpl() 
    {
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataSearchRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response queryCollections(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO)
    {
    	long start = System.currentTimeMillis();
    	logger.info("Invoking RS: POST /collection/query/compound" + compoundMetadataQueryDTO);
    	
    	HpcCollectionListDTO collections = null;
		try {
			 collections = dataSearchBusService.getCollections(compoundMetadataQueryDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /collection/query/compound" + compoundMetadataQueryDTO + 
			    		     " failed:", e);
			    return errorResponse(e);
		}
		long stop = System.currentTimeMillis();
		logger.info((stop-start) + " getCollections: Total time - " + compoundMetadataQueryDTO);
		
		return okResponse(!collections.getCollections().isEmpty() ||
				          !collections.getCollectionPaths().isEmpty() ? collections : null , true);
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
}

 
