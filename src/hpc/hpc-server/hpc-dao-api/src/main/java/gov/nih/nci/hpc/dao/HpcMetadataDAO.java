/**
 * HpcMetadataDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcHierarchicalMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Metadata DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcMetadataDAO 
{    
    /**
     * Get collection Paths by metadata query. Only collection Paths accessible to the user are returned. 
     *
     * @param metadataQueries The metadata entries to query for.
     * @param dataManagementUsername The Data Management user name. 
     * @param offset Skip that many path in the returned results.
     * @param limit No more than 'limit' paths will be returned.
     * @return List of collection Paths.
     * 
     * @throws HpcException
     */
    public List<String> getCollectionPaths(List<HpcMetadataQuery> metadataQueries,
    		                               String dataManagementUsername,
    		                               int offset, int limit) 
    		                              throws HpcException;
    
    /**
     * Get collection Paths by compound metadata query. Only collection Paths accessible to the user are returned. 
     *
     * @param compoundMetadataQuery The compound metadata query.
     * @param dataManagementUsername The Data Management user name. 
     * @param offset Skip that many path in the returned results.
     * @param limit No more than 'limit' paths will be returned.
     * @return List of collection Paths.
     * 
     * @throws HpcException
     */
    public List<String> getCollectionPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                               String dataManagementUsername,
    		                               int offset, int limit) 
    		                              throws HpcException;
    
    /**
     * Get data object Paths by metadata query. Only data object Paths accessible to the user are returned. 
     *
     * @param metadataQueries The metadata entries to query for.
     * @param dataManagementUsername The Data Management user name. 
     * @param offset Skip that many path in the returned results.
     * @param limit No more than 'limit' paths will be returned.
     * @return List of data object Paths.
     * 
     * @throws HpcException
     */
    public List<String> getDataObjectPaths(List<HpcMetadataQuery> metadataQueries,
    		                               String dataManagementUsername,
    		                               int offset, int limit) 
    		                              throws HpcException;
    
    /**
     * Get data object Paths by compound metadata query. Only data object Paths accessible to the user are returned. 
     *
     * @param compoundMetadataQuery The compound metadata query.
     * @param dataManagementUsername The Data Management user name. 
     * @param offset Skip that many path in the returned results.
     * @param limit No more than 'limit' paths will be returned.
     * @return List of data object Paths.
     * 
     * @throws HpcException
     */
    public List<String> getDataObjectPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                               String dataManagementUsername,
    		                               int offset, int limit) 
    		                              throws HpcException;
    
    /**
     * Get collection hierarchical metadata entries.
     *
     * @param path The collection's path
     * @return List of HpcHierarchicalMetadataEntry.
     * 
     * @throws HpcException
     */
    public List<HpcHierarchicalMetadataEntry> getCollectionMetadata(String path) throws HpcException;
    
    /**
     * Get data object hierarchical metadata entries.
     *
     * @param path The data object's path
     * @return List of HpcHierarchicalMetadataEntry.
     * 
     * @throws HpcException
     */
    public List<HpcHierarchicalMetadataEntry> getDataObjectMetadata(String path) throws HpcException;
    
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
    
    /**
     * Refresh all materialized views.
     *
     * @throws HpcException
     */
    public void refreshViews() throws HpcException;
}

 