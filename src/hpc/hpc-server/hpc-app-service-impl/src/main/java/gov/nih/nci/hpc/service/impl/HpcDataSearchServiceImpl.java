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

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidCompoundMetadataQuery;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidMetadataQueryLevelFilter;
import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.dao.HpcUserQueryDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryLevelFilter;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcDataSearchService;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

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

	// Metadata DAO.
	@Autowired
	private HpcMetadataDAO metadataDAO = null;
	
	// User Query DAO.
	@Autowired
	private HpcUserQueryDAO userQueryDAO = null;
	
    // The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
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
     * @throws HpcException on Spring configuration error.
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

    @Override
    public List<String> getCollectionPaths(HpcCompoundMetadataQuery compoundMetadataQuery, int page) 
    		                              throws HpcException
    {
       	if(!isValidCompoundMetadataQuery(compoundMetadataQuery)) {
           throw new HpcException("Invalid or null metadata query", 
        			              HpcErrorType.INVALID_REQUEST_INPUT);
        }
       	
    	// Use the hierarchical metadata views to perform the search.
       	String dataManagementUsername = 
       			   HpcRequestContext.getRequestInvoker().getDataManagementAccount().getUsername();
       	return toRelativePaths(metadataDAO.getCollectionPaths(
       			                              compoundMetadataQuery, dataManagementUsername,
       	 	                                  getOffset(page), searchResultsPageSize,
       			                              defaultCollectionLevelFilter));
    }
    
    @Override
    public List<String> getDataObjectPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                               int page) 
    		                              throws HpcException
    {
       	if(!isValidCompoundMetadataQuery(compoundMetadataQuery)) {
           throw new HpcException("Invalid or null compound metadata query", 
        			              HpcErrorType.INVALID_REQUEST_INPUT);
        }
       	
       	// Use the hierarchical metadata views to perform the search.
       	String dataManagementUsername = 
                   HpcRequestContext.getRequestInvoker().getDataManagementAccount().getUsername();
        return toRelativePaths(metadataDAO.getDataObjectPaths(compoundMetadataQuery, dataManagementUsername,
        		                                              getOffset(page), searchResultsPageSize,
        		                                              defaultDataObjectLevelFilter));
    }
    
    @Override
    public int getSearchResultsPageSize()
    {
    	return searchResultsPageSize;
    }
    
    @Override
    public void saveQuery(String nciUserId,
    		              HpcNamedCompoundMetadataQuery namedCompoundMetadataQuery) 
    		             throws HpcException
    {
    	// Validate the compound query.
       	if(!isValidCompoundMetadataQuery(namedCompoundMetadataQuery.getCompoundQuery())) {
           throw new HpcException("Invalid compound metadata query", 
         			              HpcErrorType.INVALID_REQUEST_INPUT);
        }
       	
       	userQueryDAO.upsertQuery(nciUserId, namedCompoundMetadataQuery);
    }
    
    @Override
    public void deleteQuery(String nciUserId, String queryName) throws HpcException
    {
    	userQueryDAO.deleteQuery(nciUserId, queryName);
    	
    }

    @Override
    public List<HpcNamedCompoundMetadataQuery> getQueries(String nciUserId) throws HpcException
    {
    	return userQueryDAO.getQueries(nciUserId);
    }
    
    @Override
    public HpcNamedCompoundMetadataQuery getQuery(String nciUserId, String queryName) throws HpcException
    {
    	return userQueryDAO.getQuery(nciUserId, queryName);
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Calculate search offset by requested page.
     *
     * @param page The requested page.
     * @return The calculated offset
     * @throws HpcException if the page is invalid.
     * 
     */
    private int getOffset(int page) throws HpcException
    {
    	if(page < 1) {
    	   throw new HpcException("Invalid search results page: " + page,
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return (page - 1) * searchResultsPageSize;
    }
    
    /**
     * Convert a list of absolute paths, to relative paths.
     *
     * @param paths The list of absolute paths.
     * @return List of relative paths.
     * 
     */
    private List<String> toRelativePaths(List<String> paths) 
    {
    	List<String> relativePaths = new ArrayList<>();
    	for(String path : paths) {
    		relativePaths.add(dataManagementProxy.getRelativePath(path));
    	}
    	
    	return relativePaths;
    }
    
    @Override
    public List<String> getCollectionMetadataAttributes(
    		               Integer level, HpcMetadataQueryOperator levelOperator) 
    		               throws HpcException
    {
    	return metadataDAO.getCollectionMetadataAttributes(level, levelOperator);
    }
    
    @Override
    public List<String> getDataObjectMetadataAttributes(
    		               Integer level, HpcMetadataQueryOperator levelOperator) 
    		               throws HpcException
    {
    	return metadataDAO.getDataObjectMetadataAttributes(level, levelOperator);
    }
}