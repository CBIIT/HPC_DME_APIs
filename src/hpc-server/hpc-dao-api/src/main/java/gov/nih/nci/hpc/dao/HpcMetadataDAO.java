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

import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataLevelAttributes;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryLevelFilter;
import gov.nih.nci.hpc.domain.metadata.HpcSearchMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcSearchMetadataEntryForCollection;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.Calendar;
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
     * Get detailed collection Paths by compound metadata query. 
     * Only collection Paths accessible to the user are returned. 
     *
     * @param compoundMetadataQuery The compound metadata query.
     * @param dataManagementUsername The Data Management user name. 
     * @param offset Skip that many path in the returned results.
     * @param limit No more than 'limit' paths will be returned.
     * @param defaultLevelFilter A default level filter to use if not provided in the query.
     * @return List of HpcSearchMetadataEntryForCollection.
     * @throws HpcException on database error.
     */
    public List<HpcSearchMetadataEntryForCollection> getDetailedCollectionPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                               String dataManagementUsername,
    		                               int offset, int limit,
    		                               HpcMetadataQueryLevelFilter defaultLevelFilter) 
    		                              throws HpcException;
    
    /**
     * Get count of collections matching a compound metadata query. 
     * Only collections accessible to the user are included in the count. 
     *
     * @param compoundMetadataQuery The compound metadata query.
     * @param dataManagementUsername The Data Management user name. 
     * @param defaultLevelFilter A default level filter to use if not provided in the query.
     * @return Count of collections matching the query.
     * @throws HpcException on database error.
     */
    public int getCollectionCount(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                      String dataManagementUsername,
                                  HpcMetadataQueryLevelFilter defaultLevelFilter) 
                                 throws HpcException;
    
    /**
     * Get data object Paths by compound metadata query. 
     * Only data object Paths accessible to the user are returned. 
     *
     * @param searchPath The path to search in if specified.
     * @param compoundMetadataQuery The compound metadata query.
     * @param dataManagementUsername The Data Management user name. 
     * @param offset Skip that many path in the returned results.
     * @param limit No more than 'limit' paths will be returned.
     * @param defaultLevelFilter A default level filter to use if not provided in the query.
     * @return List of data object Paths.
     * @throws HpcException on database error.
     */
    public List<String> getDataObjectPaths(String searchPath,
                                           HpcCompoundMetadataQuery compoundMetadataQuery,
    		                               String dataManagementUsername,
    		                               int offset, int limit,
    		                               HpcMetadataQueryLevelFilter defaultLevelFilter) 
    		                              throws HpcException;
    
    /**
     * Get detailed data object Paths by compound metadata query. 
     * Only data object Paths accessible to the user are returned. 
     *
     * @param path The path to search in if specified.
     * @param compoundMetadataQuery The compound metadata query.
     * @param dataManagementUsername The Data Management user name. 
     * @param offset Skip that many path in the returned results.
     * @param limit No more than 'limit' paths will be returned.
     * @param defaultLevelFilter A default level filter to use if not provided in the query.
     * @return List of HpcSearchMetadataEntry
     * @throws HpcException on database error.
     */
    public List<HpcSearchMetadataEntry> getDetailedDataObjectPaths(String path,
    									   HpcCompoundMetadataQuery compoundMetadataQuery,
    		                               String dataManagementUsername,
    		                               int offset, int limit,
    		                               HpcMetadataQueryLevelFilter defaultLevelFilter) 
    		                              throws HpcException;


    /**
     * Get parent collection paths of data objects searched by compound metadata query.
     * Only data object Paths accessible to the user are returned.
     *
     * @param path The path to search in if specified.
     * @param compoundMetadataQuery The compound metadata query.
     * @param dataManagementUsername The Data Management user name.
     * @param offset Skip that many path in the returned results.
     * @param limit No more than 'limit' paths will be returned.
     * @param defaultLevelFilter A default level filter to use if not provided in the query.
     * @return List of data object Paths.
     * @throws HpcException on database error.
     */
	public List<String> getDataObjectParentPaths(
			String path, HpcCompoundMetadataQuery compoundMetadataQuery,
			String dataManagementUsername, int offset, int limit,
			HpcMetadataQueryLevelFilter defaultLevelFilter) throws HpcException;


	/**
     * Get detailed parent collection and attributes of data objects searched by compound
     * metadata query. Only data object Paths accessible to the user are returned.
     *
     * @param path The path to search in if specified.
     * @param compoundMetadataQuery The compound metadata query.
     * @param dataManagementUsername The Data Management user name.
     * @param offset Skip that many path in the returned results.
     * @param limit No more than 'limit' paths will be returned.
     * @param defaultLevelFilter A default level filter to use if not provided in the query.
     * @return List of HpcSearchMetadataEntry
     * @throws HpcException on database error.
     */
	 public List<HpcSearchMetadataEntryForCollection> getDetailedDataObjectParentPaths(
			String path, HpcCompoundMetadataQuery compoundMetadataQuery,
			String dataManagementUsername, int offset, int limit,
			HpcMetadataQueryLevelFilter defaultLevelFilter) throws HpcException;


	 /**
     * Get a count of data objects matching a compound metadata query. 
     * Only data object accessible to the user are included in the count. 
     *
     * @param compoundMetadataQuery The compound metadata query.
     * @param dataManagementUsername The Data Management user name. 
     * @param defaultLevelFilter A default level filter to use if not provided in the query.
     * @return Count of data objects matching the count.
     * @throws HpcException on database error.
     */
    public int getDataObjectCount(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                      String dataManagementUsername,
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
     * @param levelLabel Filter the results by level label. (Optional).
     * @param dataManagementUsername The Data Management user name. 
     * @return A list of metadata attributes for each level.
     * @throws HpcException on database error.
     */
	public List<HpcMetadataLevelAttributes> 
	getCollectionMetadataAttributes(String levelLabel, String dataManagementUsername) 
			                       throws HpcException;
	
    /**
     * Get a list of data object metadata attributes currently registered.
     *
     * @param levelLabel Filter the results by level label. (Optional).
     * @param dataManagementUsername The Data Management user name. 
     * @return A list of metadata attributes for each level.
     * @throws HpcException on database error.
     */
	public List<HpcMetadataLevelAttributes> 
	getDataObjectMetadataAttributes(String levelLabel, String dataManagementUsername) 
			                       throws HpcException;
	
    /**
     * Get the latest metadata modification time of a given collection or data-object.
     *
     * @param id The collection or data-object ID.
     * @return The date the last metadata update was done on the collection or data-object
     * @throws HpcException on database error.
     */
	public Calendar getMetadataModifiedAt(int id) throws HpcException;
    
	/**
     * Get the latest metadata for browse for specified list of ids.
     *
     * @param ids The list of collection or data-object ID.
     * @return A list of HpcCollectionListingEntry
     * @throws HpcException on database error.
     */
	public List<HpcCollectionListingEntry> getBrowseMetadataByIds(List<Integer> ids) throws HpcException;
	
    /**
     * Refresh all materialized views.
     * @throws HpcException on database error.
     */
    public void refreshViews() throws HpcException;

}

 