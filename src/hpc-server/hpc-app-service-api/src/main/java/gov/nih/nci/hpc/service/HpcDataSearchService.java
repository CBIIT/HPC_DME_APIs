/**
 * HpcDataSearchService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryFrequency;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataLevelAttributes;
import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcSearchMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcSearchMetadataEntryForCollection;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * HPC Data Search Application Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */
public interface HpcDataSearchService {
  /**
   * Get collection paths by compound metadata query.
   *
   * @param compoundMetadataQuery The compound metadata query.
   * @param page The requested results page.
   * @param pageSize The page size specified by the user or 0 for default.
   * @return A list of collection paths.
   * @throws HpcException on service failure.
   */
  public List<String> getCollectionPaths(HpcCompoundMetadataQuery compoundMetadataQuery, int page, int pageSize)
      throws HpcException;

  /**
   * Get detailed collection paths and attributes by compound metadata query.
   *
   * @param compoundMetadataQuery The compound metadata query.
   * @param page The requested results page.
   * @param pageSize The page size specified by the user or 0 for default.
   * @return A list of HpcSearchMetadataEntryForCollection.
   * @throws HpcException on service failure.
   */
  public List<HpcSearchMetadataEntryForCollection> getDetailedCollectionPaths(HpcCompoundMetadataQuery compoundMetadataQuery, int page, int pageSize)
      throws HpcException;
  
  /**
   * Get count of collections matching a compound metadata query.
   *
   * @param compoundMetadataQuery The compound metadata query.
   * @return The count of collections matching the query.
   * @throws HpcException on service failure.
   */
  public int getCollectionCount(HpcCompoundMetadataQuery compoundMetadataQuery) throws HpcException;

  /**
   * Get data object paths by compound metadata query.
   *
   * @param path The path to search in if specified.
   * @param compoundMetadataQuery The compound metadata query.
   * @param page The requested results page.
   * @param pageSize The page size specified by the user or 0 for default.
   * @return A list of Data Object paths.
   * @throws HpcException on service failure.
   */
  public List<String> getDataObjectPaths(String path, HpcCompoundMetadataQuery compoundMetadataQuery, int page, int pageSize)
      throws HpcException;

  /**
   * Get detailed data object and attributes by compound metadata query.
   * 
   * @param path The path in search in if specified.
   * @param compoundMetadataQuery The compound metadata query.
   * @param page The requested results page.
   * @param pageSize The page size specified by the user or 0 for default.
   * @return A list of HpcSearchMetadataEntry.
   * @throws HpcException on service failure.
   */
  public List<HpcSearchMetadataEntry> getDetailedDataObjectPaths(String path, HpcCompoundMetadataQuery compoundMetadataQuery, 
			int page, int pageSize) throws HpcException;

  /**
   * Get all data object and user attributes under the specified path.
   * 
   * @param dataManagementUsername The user who requested the search
   * @param path The path in search in if specified.
   * @param offset The offset to retrieve the data.
   * @param pageSize The page size specified by the user or 0 for default.
   * @return A list of HpcSearchMetadataEntry.
   * @throws HpcException on service failure.
   */
  public List<HpcSearchMetadataEntry> getAllDataObjectPaths(String dataManagementUsername, String path, int offset, int pageSize) throws HpcException;

  /**
   * Get count of all data object under a specified path.
   *
   * @param path The path
   * @return The count of data objects under the path.
   * @throws HpcException on service failure.
   */
  public int getAllDataObjectCount(String path) throws HpcException;


  /**
   * Get parent collection paths of data objects searched by compound metadata query.
   *
   * @param path The path to search in if specified.
   * @param compoundMetadataQuery The compound metadata query.
   * @param page The requested results page.
   * @param pageSize The page size specified by the user or 0 for default.
   * @return A list of Data Object paths.
   * @throws HpcException on service failure.
   */
  public List<String> getDataObjectParentPaths(String path, HpcCompoundMetadataQuery compoundMetadataQuery,
			int page, int pageSize) throws HpcException;


  /**
   * Get detailed parent collection and attributes of data objects searched by compound metadata query.
   *
   * @param path The path in search in if specified.
   * @param compoundMetadataQuery The compound metadata query.
   * @param page The requested results page.
   * @param pageSize The page size specified by the user or 0 for default.
   * @return A list of HpcSearchMetadataEntry.
   * @throws HpcException on service failure.
   */
  public List<HpcSearchMetadataEntryForCollection> getDetailedDataObjectParentPaths(String path,
			HpcCompoundMetadataQuery compoundMetadataQuery, int page, int pageSize) throws HpcException;


  /**
   * Get count of data object matching a compound metadata query.
   *
   * @param path The path (Optional)
   * @param compoundMetadataQuery The compound metadata query.
   * @return The count of data objects matching the query.
   * @throws HpcException on service failure.
   */
  public int getDataObjectCount(String path, HpcCompoundMetadataQuery compoundMetadataQuery) throws HpcException;

  /**
   * Get the search results page size.
   *
   * @param pageSize The pageSize specified or 0 if default.
   * @return The search results page size.
   */
  public int getSearchResultsPageSize(int pageSize);

  /**
   * Save a query for a user.
   *
   * @param nciUserId The user ID to save the query for.
   * @param namedCompoundMetadataQuery The compound query.
   * @throws HpcException on service failure.
   */
  public void saveQuery(String nciUserId, HpcNamedCompoundMetadataQuery namedCompoundMetadataQuery)
      throws HpcException;

  /**
   * Delete a query for a user.
   *
   * @param nciUserId The user ID to delete the query for.
   * @param queryName The query name.
   * @throws HpcException on service failure.
   */
  public void deleteQuery(String nciUserId, String queryName) throws HpcException;

  /**
   * Get all saved queries for a user.
   *
   * @param nciUserId The registered user ID.
   * @return A list of named compound metadata queries.
   * @throws HpcException on service failure.
   */
  public List<HpcNamedCompoundMetadataQuery> getQueries(String nciUserId) throws HpcException;

  /**
   * Get a saved query by name for a user.
   *
   * @param nciUserId The registered user ID.
   * @param queryName The query name.
   * @return A named compound metadata query.
   * @throws HpcException on service failure.
   */
  public HpcNamedCompoundMetadataQuery getQuery(String nciUserId, String queryName)
      throws HpcException;

  /**
   * Get a list of collection metadata attributes currently registered.
   *
   * @param levelLabel Filter the results by level label. (Optional).
   * @return A list of metadata attributes.
   * @throws HpcException on service failure.
   */
  public List<HpcMetadataLevelAttributes> getCollectionMetadataAttributes(String levelLabel)
      throws HpcException;

  /**
   * Get a list of data object metadata attributes currently registered.
   *
   * @param levelLabel Filter the results by level label. (Optional).
   * @return A list of metadata attributes.
   * @throws HpcException on service failure.
   */
  public List<HpcMetadataLevelAttributes> getDataObjectMetadataAttributes(String levelLabel)
      throws HpcException;

  /**
   * Get all saved queries with the scheduled frequency.
   *
   * @param HpcCompoundMetadataQueryFrequency The scheduled frequency.
   * @return A list of queries saved by the user with this frequency.
   * @throws HpcException on service failure.
   */
  public List<HpcNamedCompoundMetadataQuery> getQueriesByFrequency(HpcCompoundMetadataQueryFrequency frequency)
		throws HpcException;

}
