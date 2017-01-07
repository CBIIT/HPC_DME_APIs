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
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMetadataAttributesListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcNamedCompoundMetadataQueryDTO;
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
      	boolean totalCount = compoundMetadataQueryDTO.getTotalCount() != null && 
                             compoundMetadataQueryDTO.getTotalCount();
      	
      	// Execute the query and package the results in a DTO.
      	List<String> collectionPaths = dataSearchService.getCollectionPaths(
                                           compoundMetadataQueryDTO.getCompoundQuery(), page);
      	HpcCollectionListDTO collectionsDTO = toCollectionListDTO(collectionPaths, detailedResponse);
      	
      	// Set page, limit and total count.
		collectionsDTO.setPage(page);
		int limit = dataSearchService.getSearchResultsPageSize();
		collectionsDTO.setLimit(limit);
		
		if(totalCount) {
		   int count = collectionPaths.size();
		   collectionsDTO.setTotalCount(count < limit ? count : 
			                            dataSearchService.getCollectionCount(
			                            		             compoundMetadataQueryDTO.getCompoundQuery()));
		}
		
		return collectionsDTO;
    }
    
    @Override
    public HpcCollectionListDTO getCollections(String queryName, boolean detailedResponse, int page,
    		                                   boolean totalCount) 
                                              throws HpcException
    {
    	logger.info("Invoking getCollections(string,boolean): " + queryName);
    	
    	return getCollections(toCompoundMetadataQueryDTO(queryName, detailedResponse, page, totalCount));
    }
    
    @Override
    public HpcDataObjectListDTO getDataObjects(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
                                              throws HpcException
    {
    	logger.info("Invoking getDataObjects(HpcCompoundMetadataQueryDTO): " + compoundMetadataQueryDTO);
    	
    	// Input validation.
    	if(compoundMetadataQueryDTO == null) {
    	   throw new HpcException("Null compound metadata query",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
      	boolean detailedResponse = compoundMetadataQueryDTO.getDetailedResponse() != null && 
                                   compoundMetadataQueryDTO.getDetailedResponse();
      	int page = compoundMetadataQueryDTO.getPage() != null ? compoundMetadataQueryDTO.getPage() : 1;
      	boolean totalCount = compoundMetadataQueryDTO.getTotalCount() != null && 
                             compoundMetadataQueryDTO.getTotalCount();
      	
      	// Execute the query and package the results into a DTO.
      	List<String> dataObjectPaths = dataSearchService.getDataObjectPaths(
                                           compoundMetadataQueryDTO.getCompoundQuery(), page);
      	HpcDataObjectListDTO dataObjectsDTO = toDataObjectListDTO(dataObjectPaths, detailedResponse, page);
      	
      	// Set page, limit and total count.
      	dataObjectsDTO.setPage(page);
      	int limit = dataSearchService.getSearchResultsPageSize();
		dataObjectsDTO.setLimit(limit);
      	
		if(totalCount) {
			   int count = dataObjectPaths.size();
			   dataObjectsDTO.setTotalCount(count < limit ? count : 
				                            dataSearchService.getDataObjectCount(
				                            		             compoundMetadataQueryDTO.getCompoundQuery()));
		}
		
		return dataObjectsDTO;
    }
    
    @Override
    public HpcDataObjectListDTO getDataObjects(String queryName, boolean detailedResponse, 
    		                                   int page, boolean totalCount) 
                                              throws HpcException
    {
    	logger.info("Invoking getDataObjects(string,boolean): " + queryName);
    	
    	return getDataObjects(toCompoundMetadataQueryDTO(queryName, detailedResponse, page, totalCount));
    }
    
    @Override
    public void addQuery(String queryName,
    		             HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
    		            throws HpcException
    {
    	logger.info("Invoking addQuery(String, HpcCompoundMetadataQueryDTO)");
    	
    	// Input validation.
    	if(queryName == null || queryName.isEmpty() ||
           compoundMetadataQueryDTO == null) {
    	   throw new HpcException("Null or empty queryName / compoundMetadataQueryDTO", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Get the user-id of this request invoker.
    	String nciUserId = securityService.getRequestInvoker().getNciAccount().getUserId();
    	
    	if(dataSearchService.getQuery(nciUserId, queryName) != null) {
    	   throw new HpcException("Query name already exists: " + queryName, 
	                              HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	HpcNamedCompoundMetadataQuery namedCompoundMetadataQuery = new HpcNamedCompoundMetadataQuery();
    	namedCompoundMetadataQuery.setName(queryName);
    	namedCompoundMetadataQuery.setCompoundQuery(compoundMetadataQueryDTO.getCompoundQuery());
    	
    	// Save the query.
    	dataSearchService.saveQuery(nciUserId, namedCompoundMetadataQuery);
    }
    
    @Override
    public void updateQuery(String queryName,
    		                HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
    		               throws HpcException
    {
    	logger.info("Invoking updateQuery(String, HpcCompoundMetadataQueryDTO)");
    	
    	// Input validation.
    	if(queryName == null || queryName.isEmpty() ||
           compoundMetadataQueryDTO == null) {
    	   throw new HpcException("Null or empty queryName / compoundMetadataQueryDTO", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Get the user-id of this request invoker.
    	String nciUserId = securityService.getRequestInvoker().getNciAccount().getUserId();
    	HpcNamedCompoundMetadataQuery namedCompoundMetadataQuery = 
    			                      dataSearchService.getQuery(nciUserId, queryName);
    	if(namedCompoundMetadataQuery == null) {
    	   throw new HpcException("Query name doesn't exist: " + queryName, 
	                              HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	namedCompoundMetadataQuery.setCompoundQuery(compoundMetadataQueryDTO.getCompoundQuery());
    	
    	// Save the query.
    	dataSearchService.saveQuery(nciUserId, namedCompoundMetadataQuery);
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
    public HpcNamedCompoundMetadataQueryDTO getQuery(String queryName) throws HpcException
    {
    	logger.info("Invoking getQuery(queryName)");
    	
    	HpcNamedCompoundMetadataQueryDTO queryDTO = new HpcNamedCompoundMetadataQueryDTO();
    	queryDTO.setNamedCompoundQuery(
    			    dataSearchService.getQuery(
    			        securityService.getRequestInvoker().getNciAccount().getUserId(), 
    			        queryName));

    	return queryDTO;
    }
    
    @Override
    public HpcNamedCompoundMetadataQueryListDTO getQueries() throws HpcException
    {
    	logger.info("Invoking getQueries()");
    	
    	HpcNamedCompoundMetadataQueryListDTO queriesList = new HpcNamedCompoundMetadataQueryListDTO();
    	queriesList.getNamedCompoundQueries().addAll(
    			       dataSearchService.getQueries(
    			           securityService.getRequestInvoker().getNciAccount().getUserId()));
    	
    	return queriesList;
    }
    
    @Override
	public HpcMetadataAttributesListDTO 
	          getMetadataAttributes(Integer level, HpcMetadataQueryOperator levelOperator) 
                                   throws HpcException
    {
    	logger.info("Invoking getDataManagementMode(String)");
    	
    	// Input validation. 
    	if((level == null && levelOperator != null) ||
    	   (level != null && levelOperator == null)) {
    	   throw new HpcException("Both level and level-operator need to be provided, or omitted ", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	HpcMetadataAttributesListDTO metadataAttributes = new HpcMetadataAttributesListDTO();
    	metadataAttributes.getCollectionMetadataAttributes().addAll(
    			dataSearchService.getCollectionMetadataAttributes(level, levelOperator));
    	metadataAttributes.getDataObjectMetadataAttributes().addAll(
    			dataSearchService.getDataObjectMetadataAttributes(level, levelOperator));
    	
    	return metadataAttributes;
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Construct a collection list DTO.
     *
     * @param collectionPaths A list of collection paths.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @return A collection list DTO.
     * @throws HpcException on service failure.
     */
    private HpcCollectionListDTO toCollectionListDTO(List<String> collectionPaths,
    		                                         boolean detailedResponse)
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
		
		return collectionsDTO;
    }
    
    /**
     * Construct a data object list DTO.
     *
     * @param dataObjectPaths A list of data object paths.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @param page The requested results page.
     * @return A data object list DTO.
     * @throws HpcException on service failure.
     */
    private HpcDataObjectListDTO toDataObjectListDTO(List<String> dataObjectPaths,
    		                                         boolean detailedResponse, int page)
    		                                        throws HpcException
    {
		HpcDataObjectListDTO dataObjectsDTO = new HpcDataObjectListDTO();
		
		if(detailedResponse) {
		   for(String dataObjectPath : dataObjectPaths) {
			   dataObjectsDTO.getDataObjects().add(dataManagementBusService.getDataObject(dataObjectPath));
    	   }
		} else { 
			    dataObjectsDTO.getDataObjectPaths().addAll(dataObjectPaths);
		}
		
		
		
		return dataObjectsDTO;
    }
    
    /**
     * Construct a HpcCompoundMetadataQueryDTO from a named query.
     *
     * @param queryName The user query.
     * @param detailedResponse The detailed response indicator.
     * @param page The requested results page.
     * @param totalCount The requested total count of results.
     * @return A compound metadata query DTO.
     * @throws HpcException If the user query was not found.
     */
    private HpcCompoundMetadataQueryDTO 
               toCompoundMetadataQueryDTO(String queryName, boolean detailedResponse, 
            		                      int page, boolean totalCount)
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
		compoundMetadataQueryDTO.setTotalCount(totalCount);
		
		return compoundMetadataQueryDTO;
    }
}

 