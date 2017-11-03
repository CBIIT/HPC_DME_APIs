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
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.dao.HpcUserNamedQueryDAO;
import gov.nih.nci.hpc.domain.error.HpcDomainValidationResult;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataLevelAttributes;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryLevelFilter;
import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcDataSearchService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * <p>
 * HPC Data Search Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcDataSearchServiceImpl implements HpcDataSearchService
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// Metadata DAO.
	@Autowired
	private HpcMetadataDAO metadataDAO = null;
	
	// User Query DAO.
	@Autowired
	private HpcUserNamedQueryDAO userNamedQueryDAO = null;
	
    // The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
	// Pagination support.
	@Autowired
	@Qualifier("hpcDataSearchPagination")
	private HpcPagination pagination = null;
	
	// Default level filters for collection and data object search.
	HpcMetadataQueryLevelFilter defaultCollectionLevelFilter = null;
    HpcMetadataQueryLevelFilter defaultDataObjectLevelFilter = null;

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param defaultCollectionLevelFilter The default collection search level filter.
     * @param defaultDataObjectLevelFilter The default data-object search level filter.
     * @throws HpcException on Spring configuration error.
     */
    private HpcDataSearchServiceImpl(HpcMetadataQueryLevelFilter defaultCollectionLevelFilter,
    		                         HpcMetadataQueryLevelFilter defaultDataObjectLevelFilter) 
    		                         throws HpcException
    {
    	// Input Validation.
    	if(!isValidMetadataQueryLevelFilter(defaultCollectionLevelFilter).getValid() ||
    	   !isValidMetadataQueryLevelFilter(defaultDataObjectLevelFilter).getValid()) {
    	   throw new HpcException("Invalid default collection/data object level filter",
	                              HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	this.defaultCollectionLevelFilter = defaultCollectionLevelFilter;
    	this.defaultDataObjectLevelFilter = defaultDataObjectLevelFilter;
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
    	// Input validation.
    	HpcDomainValidationResult validationResult = isValidCompoundMetadataQuery(compoundMetadataQuery);
       	if(!validationResult.getValid()) {
           throw new HpcException("Invalid compound metadata query: " + validationResult.getMessage(), 
        			              HpcErrorType.INVALID_REQUEST_INPUT);
        }
       	
    	// Use the hierarchical metadata views to perform the search.
       	String dataManagementUsername = 
       			   HpcRequestContext.getRequestInvoker().getDataManagementAccount().getUsername();
       	return toRelativePaths(metadataDAO.getCollectionPaths(
       			                              compoundMetadataQuery, dataManagementUsername,
       	 	                                  pagination.getOffset(page), pagination.getPageSize(),
       			                              defaultCollectionLevelFilter));
    }
    
    @Override
    public int getCollectionCount(HpcCompoundMetadataQuery compoundMetadataQuery) throws HpcException
    {
    	// Input validation.
    	HpcDomainValidationResult validationResult = isValidCompoundMetadataQuery(compoundMetadataQuery);
       	if(!validationResult.getValid()) {
           throw new HpcException("Invalid compound metadata query: " + validationResult.getMessage(), 
        			              HpcErrorType.INVALID_REQUEST_INPUT);
        }
       	
    	// Use the hierarchical metadata views to perform the search.
       	String dataManagementUsername = 
       			   HpcRequestContext.getRequestInvoker().getDataManagementAccount().getUsername();
       	return metadataDAO.getCollectionCount(compoundMetadataQuery, dataManagementUsername,
       			                              defaultCollectionLevelFilter);
    }
    
    @Override
    public List<String> getDataObjectPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                               int page) 
    		                              throws HpcException
    {
    	// Input Validation.
    	HpcDomainValidationResult validationResult = isValidCompoundMetadataQuery(compoundMetadataQuery);
       	if(!validationResult.getValid()) {
           throw new HpcException("Invalid compound metadata query: " + validationResult.getMessage(), 
        			              HpcErrorType.INVALID_REQUEST_INPUT);
        }
       	
       	// Use the hierarchical metadata views to perform the search.
       	String dataManagementUsername = 
                   HpcRequestContext.getRequestInvoker().getDataManagementAccount().getUsername();
        return toRelativePaths(metadataDAO.getDataObjectPaths(compoundMetadataQuery, dataManagementUsername,
        		                                              pagination.getOffset(page), pagination.getPageSize(),
        		                                              defaultDataObjectLevelFilter));
    }
    
    @Override
    public int getDataObjectCount(HpcCompoundMetadataQuery compoundMetadataQuery) throws HpcException
    {
    	// Input Validation.
    	HpcDomainValidationResult validationResult = isValidCompoundMetadataQuery(compoundMetadataQuery);
       	if(!validationResult.getValid()) {
           throw new HpcException("Invalid compound metadata query: " + validationResult.getMessage(), 
        			              HpcErrorType.INVALID_REQUEST_INPUT);
        }
       	
       	// Use the hierarchical metadata views to perform the search.
       	String dataManagementUsername = 
                   HpcRequestContext.getRequestInvoker().getDataManagementAccount().getUsername();
        return metadataDAO.getDataObjectCount(compoundMetadataQuery, dataManagementUsername,
                                              defaultDataObjectLevelFilter);
    }
    
    @Override
    public int getSearchResultsPageSize()
    {
    	return pagination.getPageSize();
    }
    
    @Override
    public void saveQuery(String nciUserId,
    		              HpcNamedCompoundMetadataQuery namedCompoundMetadataQuery) 
    		             throws HpcException
    {
    	// Validate the compound query.
    	HpcDomainValidationResult validationResult = isValidNamedCompoundMetadataQuery(namedCompoundMetadataQuery);
       	if(!validationResult.getValid()) {
           throw new HpcException("Invalid named compound metadata query: " + validationResult.getMessage(), 
         			              HpcErrorType.INVALID_REQUEST_INPUT);
        }
       	
       	// Set the update timestamp.
       	namedCompoundMetadataQuery.setUpdated(Calendar.getInstance());
       	
       	// Upsert the named query.
       	userNamedQueryDAO.upsertQuery(nciUserId, namedCompoundMetadataQuery);
    }
    
    @Override
    public void deleteQuery(String nciUserId, String queryName) throws HpcException
    {
    	// Input validation.
    	if(getQuery(nciUserId, queryName) == null) {
    	   throw new HpcException("Query doesn't exist: " + queryName, 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	userNamedQueryDAO.deleteQuery(nciUserId, queryName);
    }

    @Override
    public List<HpcNamedCompoundMetadataQuery> getQueries(String nciUserId) throws HpcException
    {
    	return userNamedQueryDAO.getQueries(nciUserId);
    }
    
    @Override
    public HpcNamedCompoundMetadataQuery getQuery(String nciUserId, String queryName) throws HpcException
    {
    	return userNamedQueryDAO.getQuery(nciUserId, queryName);
    }
    
    @Override
    public List<HpcMetadataLevelAttributes> getCollectionMetadataAttributes(String levelLabel) 
    		                                                               throws HpcException
    {
    	String dataManagementUsername = 
               HpcRequestContext.getRequestInvoker().getDataManagementAccount().getUsername();
    	
    	return metadataDAO.getCollectionMetadataAttributes(levelLabel, dataManagementUsername);
    }
    
    @Override
    public List<HpcMetadataLevelAttributes> getDataObjectMetadataAttributes(String levelLabel) 
    		                                                               throws HpcException
    {
    	String dataManagementUsername = 
               HpcRequestContext.getRequestInvoker().getDataManagementAccount().getUsername();
    	
    	return metadataDAO.getDataObjectMetadataAttributes(levelLabel, dataManagementUsername);
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
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
}