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
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryLevelFilter;
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
     * Get collection Paths by compound metadata query. 
     * Only collection Paths accessible to the user are returned. 
     *
     * @param compoundMetadataQuery The compound metadata query.
     * @param dataManagementUsername The Data Management user name. 
     * @param offset Skip that many path in the returned results.
     * @param limit No more than 'limit' paths will be returned.
     * @param defaultLevelFilter A default level filter to use if not provided in the query.
     * @return List of collection Paths.
     * @throws HpcException on database error.
     */
    public List<String> getCollectionPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                               String dataManagementUsername,
    		                               int offset, int limit,
    		                               HpcMetadataQueryLevelFilter defaultLevelFilter) 
    		                              throws HpcException;
    
    /**
     * Get data object Paths by compound metadata query. Only data object Paths accessible to the user are returned. 
     *
     * @param compoundMetadataQuery The compound metadata query.
     * @param dataManagementUsername The Data Management user name. 
     * @param offset Skip that many path in the returned results.
     * @param limit No more than 'limit' paths will be returned.
     * @param defaultLevelFilter A default level filter to use if not provided in the query.
     * @return List of data object Paths.
     * @throws HpcException on database error.
     */
    public List<String> getDataObjectPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                               String dataManagementUsername,
    		                               int offset, int limit,
    		                               HpcMetadataQueryLevelFilter defaultLevelFilter) 
    		                              throws HpcException;
    
    /**
     * Get collection hierarchical metadata entries.
     *
     * @param path The collection's path.
     * @param minLevel The minimum level in the hierarchy to return.
     * @return List of HpcMetadataEntry.
     * @throws HpcException on database error.
     */
    public List<HpcMetadataEntry> getCollectionMetadata(String path, int minLevel) throws HpcException;
    
    /**
     * Get data object hierarchical metadata entries.
     *
     * @param path The data object's path.
     * @param minLevel The minimum level in the hierarchy to return.
     * @return List of HpcHierarchicalMetadataEntry.
     * @throws HpcException on database error.
     */
    public List<HpcMetadataEntry> getDataObjectMetadata(String path, int minLevel) throws HpcException;
    
    /**
     * Get a list of collection metadata attributes currently registered.
     *
     * @param level Filter the results by level. (Optional).
     * @param levelOperator The operator to use in the level filter. (Optional).
     * @param dataManagementUsername The Data Management user name. 
     * @return A list of metadata attributes.
     * @throws HpcException on database error.
     */
	public List<String> getCollectionMetadataAttributes(
			               Integer level, HpcMetadataQueryOperator levelOperator,
			               String dataManagementUsername) 
			               throws HpcException;
	
    /**
     * Get a list of data object metadata attributes currently registered.
     *
     * @param level Filter the results by level. (Optional).
     * @param levelOperator The operator to use in the level filter. (Optional).
     * @param dataManagementUsername The Data Management user name. 
     * @return A list of metadata attributes.
     * @throws HpcException on database error.
     */
	public List<String> getDataObjectMetadataAttributes(
			               Integer level, HpcMetadataQueryOperator levelOperator,
			               String dataManagementUsername) 
			               throws HpcException;
    
    /**
     * Refresh all materialized views.
     * @throws HpcException on database error.
     */
    public void refreshViews() throws HpcException;
}

 