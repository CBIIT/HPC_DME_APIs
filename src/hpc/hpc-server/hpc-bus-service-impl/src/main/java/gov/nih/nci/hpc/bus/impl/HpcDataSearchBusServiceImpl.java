/**
 * HpcDataSearchBusServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.bus.HpcDataSearchBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataSearchService;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Data Search Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataSearchBusServiceImpl implements HpcDataSearchBusService
{  
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// Data Search Application Service instance.
	@Autowired
    private HpcDataSearchService dataSearchService = null;
	
	// Data Management Bus Service instance.
	@Autowired
    private HpcDataManagementBusService dataManagementBusService = null;
	
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
    private HpcDataSearchBusServiceImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataSearchBusService Interface Implementation
    //---------------------------------------------------------------------//  
    	
    @Override
    public HpcCollectionListDTO getCollections(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
                                              throws HpcException
    {
    	logger.info("Invoking getCollections(HpcCompoundMetadataQueryDTO): " + compoundMetadataQueryDTO);
    	
    	// Input validation.
    	if(compoundMetadataQueryDTO == null) {
    	   throw new HpcException("Null compound metadata query",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
      	boolean detailedResponse = compoundMetadataQueryDTO.getDetailedResponse() != null && 
                                   compoundMetadataQueryDTO.getDetailedResponse();
      	int page = compoundMetadataQueryDTO.getPage() != null ? compoundMetadataQueryDTO.getPage() : 1;
      	
      	// Execute the query and package the results.
      	return toCollectionListDTO(dataSearchService.getCollectionPaths(
      			                       compoundMetadataQueryDTO.getCompoundQuery(), page), 
      			                       detailedResponse, page);
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Construct a collection list DTO.
     *
     * @param collectionPaths A list of collection paths.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @param page The requested results page.
     */
    private HpcCollectionListDTO toCollectionListDTO(List<String> collectionPaths,
    		                                         boolean detailedResponse, int page)
    		                                        throws HpcException
    {
		HpcCollectionListDTO collectionsDTO = new HpcCollectionListDTO();
		
		if(detailedResponse) {
		   for(String collectionPath : collectionPaths) {
			   collectionsDTO.getCollections().add(dataManagementBusService.getCollection(collectionPath));
    	   }
		} else { 
			    collectionsDTO.getCollectionPaths().addAll(collectionPaths);
		}
		
		collectionsDTO.setPage(page);
		collectionsDTO.setLimit(dataSearchService.getSearchResultsPageSize());
		
		return collectionsDTO;
    }
}

 