/**
 * HpcDataSearchServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidMetadataQueryLevelFilter;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryLevelFilter;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataSearchService;

/**
 * <p>
 * HPC Data Search Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataSearchServiceImpl implements HpcDataSearchService
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// The max page size of search results.
	private int searchResultsPageSize = 0;
	
	// Default level filters for collection and data object search.
	HpcMetadataQueryLevelFilter defaultCollectionLevelFilter = null;
    HpcMetadataQueryLevelFilter defaultDataObjectLevelFilter = null;

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param searchResultsPageSize The max page size of search results.
     * @param defaultCollectionLevelFilter The default collection search level filter.
     * @param defaultDataObjectLevelFilter The default data-object search level filter.
     * @throws HpcException
     */
    private HpcDataSearchServiceImpl(int searchResultsPageSize,
    		                         HpcMetadataQueryLevelFilter defaultCollectionLevelFilter,
    		                         HpcMetadataQueryLevelFilter defaultDataObjectLevelFilter) 
    		                         throws HpcException
    {
    	// Input Validation.
    	if(!isValidMetadataQueryLevelFilter(defaultCollectionLevelFilter) ||
    	   !isValidMetadataQueryLevelFilter(defaultDataObjectLevelFilter)) {
    	   throw new HpcException("Invalid default collection/data object level filter",
	                              HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	this.defaultCollectionLevelFilter = defaultCollectionLevelFilter;
    	this.defaultDataObjectLevelFilter = defaultDataObjectLevelFilter;
    	this.searchResultsPageSize = searchResultsPageSize;
    }   
    
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
	private HpcDataSearchServiceImpl() throws HpcException
    {
    	throw new HpcException("Default Constructor disabled",
    			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataSearchService Interface Implementation
    //---------------------------------------------------------------------//  

    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
}