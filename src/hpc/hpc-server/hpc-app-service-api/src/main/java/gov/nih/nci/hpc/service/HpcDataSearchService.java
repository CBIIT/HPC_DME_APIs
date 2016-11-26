/**
 * HpcDataSearchService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;


/**
 * <p>
 * HPC Data Search Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDataSearchService 
{  
    /**
     * Get collection paths by compound metadata query.
     *
     * @param compoundMetadataQuery The compound metadata query.
     * @param page The requested results page.
     * @return Collection path list.
     * 
     * @throws HpcException
     */
    public List<String> getCollectionPaths(HpcCompoundMetadataQuery compoundMetadataQuery, int page) 
    		                              throws HpcException;
    
    /**
     * Get data object paths by compound metadata query.
     *
     * @param compoundMetadataQuery The compound metadata query.
     * @param page The requested results page.
     * @return Data Object path list.
     * 
     * @throws HpcException
     */
    public List<String> getDataObjectPaths(HpcCompoundMetadataQuery compoundMetadataQuery, int page) 
    		                              throws HpcException;
    
    /**
     * Get the search results page size.
     *
     * @return The search results page size.
     */
    public int getSearchResultsPageSize();
    
    /**
     * Save a query for a user.
     *
     * @param nciUserId The user ID save the query for.
     * @param namedCompoundMetadataQuery The compound query.
     * @throws HpcException
     */
    public void saveQuery(String nciUserId, 
    		              HpcNamedCompoundMetadataQuery namedCompoundMetadataQuery) 
    		             throws HpcException;
    
    /**
     * Delete a query for a user.
     *
     * @param nciUserId The user ID save the query for.
     * @param queryName The query name.
     * @throws HpcException
     */
    public void deleteQuery(String nciUserId, String queryName) throws HpcException;

    /**
     * Get all saved queries for a user.
     *
     * @param nciUserId The registered user ID.
     * @return List<HpcNamedCompoundMetadataQuery>
     * @throws HpcException
     */
    public List<HpcNamedCompoundMetadataQuery> getQueries(String nciUserId) throws HpcException;
    
    /**
     * Get a saved query by name for a user.
     *
     * @param nciUserId The registered user ID.
     * @param queryName The query name.
     * @return HpcNamedCompoundMetadataQuery
     * @throws HpcException
     */
    public HpcNamedCompoundMetadataQuery getQuery(String nciUserId, String queryName) throws HpcException;
    
    /**
     * Get a list of collection metadata attributes currently registered.
     *
     * @param level Filter the results by level. (Optional).
     * @param levelOperator The operator to use in the level filter. (Optional).
     * @return A list of metadata attributes.
     */
	public List<String> getCollectionMetadataAttributes(
			               Integer level, HpcMetadataQueryOperator levelOperator) 
			               throws HpcException;
	
    /**
     * Get a list of data object metadata attributes currently registered.
     *
     * @param level Filter the results by level. (Optional).
     * @param levelOperator The operator to use in the level filter. (Optional).
     * @return A list of metadata attributes.
     */
	public List<String> getDataObjectMetadataAttributes(
			               Integer level, HpcMetadataQueryOperator levelOperator) 
			               throws HpcException;
}

 