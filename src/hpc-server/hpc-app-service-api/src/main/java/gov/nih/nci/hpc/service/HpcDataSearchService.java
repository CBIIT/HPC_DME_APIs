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
   * @param The path to search in if specified.
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
   * @param The path in search in if specified.
   * @param compoundMetadataQuery The compound metadata query.
   * @param page The requested results page.
   * @param pageSize The page size specified by the user or 0 for default.
   * @return A list of HpcSearchMetadataEntry.
   * @throws HpcException on service failure.
   */
  public List<HpcSearchMetadataEntry> getDetailedDataObjectPaths(String path, HpcCompoundMetadataQuery compoundMetadataQuery, 
			int page, int pageSize) throws HpcException;
  
  /**
   * Get count of data object matching a compound metadata query.
   *
   * @param compoundMetadataQuery The compound metadata query.
   * @return The count of data objects matching the query.
   * @throws HpcException on service failure.
   */
  public int getDataObjectCount(HpcCompoundMetadataQuery compoundMetadataQuery) throws HpcException;

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
}
