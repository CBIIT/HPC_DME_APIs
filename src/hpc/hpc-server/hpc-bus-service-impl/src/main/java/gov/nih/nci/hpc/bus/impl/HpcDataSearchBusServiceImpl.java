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
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcNamedCompoundMetadataQueryListDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataSearchService;
import gov.nih.nci.hpc.service.HpcSecurityService;

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
	
	// Security Application Service instance.
	@Autowired
    private HpcSecurityService securityService = null;
	
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
    public HpcCollectionListDTO getCollections(List<HpcMetadataQuery> metadataQueries,
    		                                   boolean detailedResponse, int page) 
                                              throws HpcException
    {
    	logger.info("Invoking getCollections(List<HpcMetadataQuery>, boolean): " + 
    			    metadataQueries);
    	
    	// Input validation.
    	if(metadataQueries == null) {
    	   throw new HpcException("Null metadata queries",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Create a Compound query from the simple query.
    	HpcCompoundMetadataQuery compoundMetadataQuery = new HpcCompoundMetadataQuery();
    	compoundMetadataQuery.setOperator(HpcCompoundMetadataQueryOperator.AND);
    	compoundMetadataQuery.getQueries().addAll(metadataQueries);
    	
    	HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO = new HpcCompoundMetadataQueryDTO();
    	compoundMetadataQueryDTO.setCompoundQuery(compoundMetadataQuery);
    	compoundMetadataQueryDTO.setPage(page);
    	compoundMetadataQueryDTO.setDetailedResponse(detailedResponse);
    	
    	return getCollections(compoundMetadataQueryDTO);
    }
    
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
    
    @Override
    public HpcCollectionListDTO getCollections(String queryName, boolean detailedResponse, int page) 
                                              throws HpcException
    {
    	logger.info("Invoking getCollections(string,boolean): " + queryName);
    	
    	return getCollections(toCompoundMetadataQueryDTO(queryName, detailedResponse, page));
    }
    
    @Override
    public void saveQuery(String queryName,
    		              HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
    		             throws HpcException
    {
    	logger.info("Invoking saveQuery(String, HpcCompoundMetadataQueryDTO)");
    	
    	// Input validation.
    	if(queryName == null || queryName.isEmpty() ||
           compoundMetadataQueryDTO == null) {
    	   throw new HpcException("Null or empty queryName / compoundMetadataQueryDTO", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	HpcNamedCompoundMetadataQuery namedCompoundMetadataQuery = new HpcNamedCompoundMetadataQuery();
    	namedCompoundMetadataQuery.setName(queryName);
    	namedCompoundMetadataQuery.setCompoundQuery(compoundMetadataQueryDTO.getCompoundQuery());
    	
    	// Save the query.
    	dataSearchService.saveQuery(securityService.getRequestInvoker().getNciAccount().getUserId(), 
    			                    namedCompoundMetadataQuery);
    }
    
    @Override
    public void deleteQuery(String queryName) throws HpcException
    {
    	logger.info("Invoking deleteQuery(String)");
    	
    	// Input validation.
    	if(queryName == null || queryName.isEmpty())  {
    	   throw new HpcException("Null or empty nciUserId / queryName", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Delete the query.
    	dataSearchService.deleteQuery(securityService.getRequestInvoker().getNciAccount().getUserId(), 
    			                      queryName);
    }

    @Override
    public HpcNamedCompoundMetadataQueryListDTO getQueries() throws HpcException
    {
    	logger.info("Invoking getQueries()");
    	
    	HpcNamedCompoundMetadataQueryListDTO queriesList = new HpcNamedCompoundMetadataQueryListDTO();
    	queriesList.getQueries().addAll(
    			       dataSearchService.getQueries(
    			           securityService.getRequestInvoker().getNciAccount().getUserId()));
    	
    	return queriesList;
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
    
    /**
     * Construct a HpcCompoundMetadataQueryDTO from a named query.
     *
     * @param queryName The user query.
     * @param detailedResponse The detailed response indicator.
     * @param The requested results page
     * @return HpcCompoundMetadataQueryDTO
     * 
     * @throws HpcException If the user query was not found
     */
    private HpcCompoundMetadataQueryDTO 
               toCompoundMetadataQueryDTO(String queryName, boolean detailedResponse, 
            		                      int page)
                                         throws HpcException
    {
    	// Input validation.
    	if(queryName == null || queryName.isEmpty()) {
    	   throw new HpcException("Null or empty query name",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
		// Get the user query.
		HpcNamedCompoundMetadataQuery namedCompoundQuery = 
		   dataSearchService.getQuery(
	                            securityService.getRequestInvoker().getNciAccount().getUserId(), 
	                            queryName);
		if(namedCompoundQuery == null || namedCompoundQuery.getCompoundQuery() == null) {
		   throw new HpcException("User query not found: " + queryName,
				                  HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		// Construct the query DTO.
		HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO = new HpcCompoundMetadataQueryDTO();
		compoundMetadataQueryDTO.setCompoundQuery(namedCompoundQuery.getCompoundQuery());
		compoundMetadataQueryDTO.setDetailedResponse(detailedResponse);
		compoundMetadataQueryDTO.setPage(page);
		
		return compoundMetadataQueryDTO;
    }
}

 