/**
 * HpcDataManagementService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataHierarchy;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcEntityPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataOrigin;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.model.HpcDataManagementAccount;
import gov.nih.nci.hpc.domain.model.HpcGroup;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.user.HpcGroupResponse;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * HPC Data Management Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDataManagementService 
{   
    /**
     * Create a collection's directory.
     *
     * @param path The collection path.
     * @return true if the directory was created, or false if it already exists.
     * 
     * @throws HpcException
     */
    public boolean createDirectory(String path) throws HpcException;
    
    /**
     * Create a data object's file.
     *
     * @param path The data object path.
     * @return true if the data object file was created, or false if it already exists.
     * 
     * @throws HpcException
     */
    public boolean createFile(String path) 
    		                 throws HpcException;
    
    /**
     * Delete a path (data object or directory).
     *
     * @param path The path to delete.
     * @throws HpcException
     */
    public void delete(String path) throws HpcException;

    /**
     * Add metadata to a collection.
     *
     * @param path The collection path.
     * @param metadataEntries The metadata entries to add.
     * 
     * @throws HpcException
     */
    public void addMetadataToCollection(String path, 
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException; 
    
    /**
     * Generate system metadata and attach to a collection.
     * System generated metadata is:
     * 		1. UUID.
     * 		2. Registrar user ID.
     * 		3. Registrar name.
     *      4. Registrar DOC.
     *      5. Metadata Origin.
     *
     * @param path The collection path.
     * @param metadataOrigin an object describing the origin of the metadata (parent or self).
     * 
     * @throws HpcException
     */
    public void addSystemGeneratedMetadataToCollection(String path,
    		                                           HpcMetadataOrigin metadataOrigin) 
    		                                          throws HpcException; 
    
    /**
     * Update a collection's metadata.
     *
     * @param path The collection path.
     * @param metadataEntries The metadata entries to update.
     * 
     * @throws HpcException
     */
    public void updateCollectionMetadata(String path, 
    		                             List<HpcMetadataEntry> metadataEntries) 
    		                            throws HpcException; 
    
    /**
     * Add metadata to a data object.
     *
     * @param path The data object path.
     * @param metadataEntries The metadata entries to add.
     * 
     * @throws HpcException
     */
    public void addMetadataToDataObject(String path, 
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException; 
    
    /**
     * Update a data object's metadata.
     *
     * @param path The data object path.
     * @param metadataEntries The metadata entries to update.
     * 
     * @throws HpcException
     */
    public void updateDataObjectMetadata(String path, 
    		                             List<HpcMetadataEntry> metadataEntries) 
    		                            throws HpcException; 
    
    /**
     * Generate system metadata and attach to the data object.
     * System generated metadata is:
     *      1. UUID.
     * 		2. Registrar user ID.
     * 		3. Registrar name.
     *      4. Registrar DOC.
     * 		5. Source location (file-container-id and file-id). (Optional)
     *      6. Archive location (file-container-id and file-id).
     *      7. Data Transfer Request ID. (Optional)
     *      8. Data Transfer Status.
     *      9. Data Transfer Type.
     *      10. Data Object File(s) size. (Optional)
     *      11. Metadata Origin
     *
     * @param path The data object path.
     * @param archiveLocation The physical file archive location.
     * @param sourceLocation (Optional) The source location of the file.
     * @param dataTransferRequestId (Optional) The data transfer request ID.
     * @param dataTransferStatus The data transfer upload status.
     * @param dataTransferType The data transfer type.
     * @param sourceSize (Optional) The data source size in bytes.
     * @param callerObjectId (Optional) The caller object ID.
     * @param metadataOrigin The metadata origin.
     * 
     * @throws HpcException
     */
    public void addSystemGeneratedMetadataToDataObject(String path, 
    		                                           HpcFileLocation archiveLocation,
    		                                           HpcFileLocation sourceLocation,
    		                                           String dataTransferRequestId,
    		                                           HpcDataTransferUploadStatus dataTransferStatus,
    		                                           HpcDataTransferType dataTransferType,
    		                                           Long sourceSize, String callerObjectId,
    		                                           HpcMetadataOrigin metadataOrigin) 
    		                                          throws HpcException; 
    
    /**
     * Update system generated metadata of a data object.
     *
     * @param path The data object path.
     * @param archiveLocation (Optional) The physical file archive location.
     * @param dataTransferRequestId (Optional) The data transfer request ID.
     * @param dataTransferStatus (Optional) The data transfer upload status.
     * @param dataTransferType (Optional) The data transfer type.
     * @param metadataOrigin (Optional) The metadata origin
     * 
     * @throws HpcException
     */
    public void updateDataObjectSystemGeneratedMetadata(String path, 
    		                                            HpcFileLocation archiveLocation,
    		                                            String dataTransferRequestId,
    		                                            HpcDataTransferUploadStatus dataTransferStatus,
    		                                            HpcDataTransferType dataTransferType,
    		                                            HpcMetadataOrigin metadataOrigin) 
    		                                          throws HpcException; 
    
    /**
     * Get the system generated metadata of a data object.
     *
     * @param path The data object path.
     * @return HpcSystemGeneratedMetadata The system generated metadata
     * 
     * @throws HpcException
     */
    public HpcSystemGeneratedMetadata 
              getDataObjectSystemGeneratedMetadata(String path) throws HpcException; 
    
    /**
     * Get the system generated metadata of a collection.
     *
     * @param path The collection path.
     * @return HpcSystemGeneratedMetadata The system generated metadata.
     * 
     * @throws HpcException
     */
    public HpcSystemGeneratedMetadata 
              getCollectionSystemGeneratedMetadata(String path) throws HpcException; 
    
    /**
     * Update system generated metadata of a collection.
     *
     * @param path The data object path.
     * @param metadataOrigin (Optional) The metadata origin
     * 
     * @throws HpcException
     */
    public void updateCollectionSystemGeneratedMetadata(String path, 
    		                                            HpcMetadataOrigin metadataOrigin) 
    		                                           throws HpcException; 
    
    /**
     * Get the system generated metadata object from a list of entries.
     *
     * @param systemGeneratedMetadataEntries The system generated metadata entries.
     * @return HpcSystemGeneratedMetadata The system generated metadata.
     * 
     * @throws HpcException
     */
    public HpcSystemGeneratedMetadata 
              toSystemGeneratedMetadata(List<HpcMetadataEntry> systemGeneratedMetadataEntries) 
            	                       throws HpcException; 
    
    /**
     * Add parent metadata to a either a collection or a data object.
     *
     * @param path The collection or data object path.
     * @return HpcMetadataOrigin An object listing the origin of the collection / data object
     *                           metadata after the change.
     * @throws HpcException
     */
    public HpcMetadataOrigin addParentMetadata(String path) throws HpcException; 
    
    /**
     * Update metadata tree. Propagate metadata changes to 'path' to the tree under it.
     *
     * @param path The collection path.
     * @throws HpcException
     */
    public void updateMetadataTree(String path) throws HpcException; 
    
    /** 
     * Update a MetadataOrigin object with updated entries.
     * 
     * @param metadataOrigin The metadata origin object.
     * @param metadataEntries The updated metadata entries.
     * @return HpcMetadataOrigin An updated metadata origin object.
     */
    public HpcMetadataOrigin updateMetadataOrigin(HpcMetadataOrigin metadataOrigin, 
    		                                      List<HpcMetadataEntry> metadataEntries);
    
    /**
     * Get collection by its path.
     *
     * @param path The collection's path.
     * @return HpcCollection.
     * 
     * @throws HpcException
     */
    public HpcCollection getCollection(String path) throws HpcException;
    
    /**
     * Get collections by metadata query.
     *
     * @param metadataQueries The metadata queries.
     * @param page The requested results page.
     * @return HpcCollection list.
     * 
     * @throws HpcException
     */
    public List<HpcCollection> getCollections(List<HpcMetadataQuery> metadataQueries,
    		                                  int page) 
    		                                 throws HpcException;
    
    /**
     * Get collections by compound metadata query.
     *
     * @param compoundMetadataQuery The compound metadata query.
     * @param page The requested results page.
     * @return HpcCollection list.
     * 
     * @throws HpcException
     */
    public List<HpcCollection> getCollections(HpcCompoundMetadataQuery compoundMetadataQuery,
                                              int page) 
    		                                 throws HpcException;
    
    /**
     * Get collection paths by metadata query.
     *
     * @param metadataQueries The metadata queries.
     * @param page The requested results page.
     * @return Collection path list.
     * 
     * @throws HpcException
     */
    public List<String> getCollectionPaths(List<HpcMetadataQuery> metadataQueries, int page) 
    		                              throws HpcException;
    
    /**
     * Get collection paths by compound metadata query.
     *
     * @param compoundMetadataQuery The compound metadata query.
     * @param page The requested results page.
     * @return Collection path list.
     * 
     * @throws HpcException
     */
    public List<String> getCollectionPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                               int page) 
    		                              throws HpcException;
    
    /**
     * Get metadata of a collection.
     *
     * @param path The collection's path.
     * @return HpcMetadataEntries The collection's metadata entries.
     * 
     * @throws HpcException
     */
    public HpcMetadataEntries getCollectionMetadataEntries(String path) throws HpcException;
    
    /**
     * Get data object by its path.
     *
     * @param path The data object's path.
     * @return HpcDataObject.
     * 
     * @throws HpcException
     */
    public HpcDataObject getDataObject(String path) throws HpcException;
    
    /**
     * Get data objects by metadata query.
     *
     * @param metadataQueries The metadata queries.
     * @param page The requested results page.
     * @return HpcDataObject list.
     * 
     * @throws HpcException
     */
    public List<HpcDataObject> getDataObjects(List<HpcMetadataQuery> metadataQueries,
    		                                  int page) 
    		                                 throws HpcException;
    
    /**
     * Get data objects by compound metadata query.
     *
     * @param compoundMetadataQuery The compound metadata query.
     * @param page The requested results page.
     * @return HpcDataObject list.
     * 
     * @throws HpcException
     */
    public List<HpcDataObject> getDataObjects(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                                  int page) 
    		                                 throws HpcException;
    
    /**
     * Get data object paths by metadata query.
     *
     * @param metadataQueries The metadata queries.
     * @param page The requested results page.
     * @return Data Object path list.
     * 
     * @throws HpcException
     */
    public List<String> getDataObjectPaths(List<HpcMetadataQuery> metadataQueries, int page) 
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
    public List<String> getDataObjectPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                               int page) 
    		                              throws HpcException;
    
    /**
     * Get data objects that have their data transfer in-progress.
     *
     * @return HpcDataObject list.
     * 
     * @throws HpcException
     */
    public List<HpcDataObject> getDataObjectsInProgress() throws HpcException;
    
    /**
     * Get data objects that have their data stored in temporary archive.
     *
     * @return HpcDataObject list.
     * 
     * @throws HpcException
     */
    public List<HpcDataObject> getDataObjectsInTemporaryArchive() throws HpcException;
    
    /**
     * Get metadata of a data object.
     *
     * @param path The data object's path.
     * @return HpcMetadataEntries The data object's metadata entries.
     * 
     * @throws HpcException
     */
    public HpcMetadataEntries getDataObjectMetadataEntries(String path) throws HpcException;
    
    /**
     * Get the role of a given user's name.
     *
     * @param username The user's name.
     * @return HpcUserRole The user's role.
     * 
     * @throws HpcException
     */
    public HpcUserRole getUserRole(String username) throws HpcException;  
    
    /**
     * Add a user.
     *
     * @param nciAccount The NCI account of the user to be added to data management.
     * @param userRole The HPC user role to assign to the new user.
     * 
     * @throws HpcException
     */
    public void addUser(HpcNciAccount nciAccount, HpcUserRole userRole) 
    		           throws HpcException;
    
    /**
     * Update a user.
     *
     * @param nciUserId The NCI user ID of the user to update.
     * @param firstName The user first name.
     * @param lastName The user last name. 
     * @param userRole The HPC user role to update for the user.
     * 
     * @throws HpcException
     */
    public void updateUser(String nciUserId, String firstName, String lastName,
                           HpcUserRole userRole) 
    		              throws HpcException;
    
    /**
     * Delete a user.
     *
     * @param nciUserId The user-id to delete.
     * 
     * @throws HpcException
     */
    public void deleteUser(String nciUserId) throws HpcException;
    
    /**
     * Close connection to Data Management system for the current service call.
     */
    public void closeConnection();
    
    /**
     * Set permission of an entity (collection or data object) for a user. 
     *
     * @param path The entity path.
     * @param permissionRequest The permission request (NCI user ID and permission).
     * @return HpcPathAttributes of the path.
     * 
     * @throws HpcException If it failed to set the entity permission.
     */
    public HpcPathAttributes setPermission(String path,
    		                               HpcEntityPermission permissionRequest) 
    		                              throws HpcException;
    
    /**
     * Assign system account as an additional owner of an entity.
     *
     * @param path The entity path.
     * @return HpcPathAttributes of the path.
     * 
     * @throws HpcException If it failed to set the entity permission.
     */
    public HpcPathAttributes assignSystemAccountPermission(String path) 
    		                                              throws HpcException;
    
    
    /**
     * convert a list of metadata entries to Map<attribute, value>
     *
     * @param metadataEntries The list of metadata entries
     * @return Map<String, String>
     * 
     * @throws HpcException
     */
    public Map<String, String> toMap(List<HpcMetadataEntry> metadataEntries);
    
    /**
     * Create User group and 
     * @param group group name
     * @param addUserId List of userIds to add to the group
     * @param removeUserId List of userIds to remove from the group
     * @throws HpcException
     */
    public HpcGroupResponse setGroup(HpcGroup group, List<String> addUserId, List<String> removeUserId) throws HpcException;
    
    /**
     * Create HPC data management account from proxy account object. 
     * This is to cache proxy data management account for better performance
     * @param proxyAccount
     * @return HpcDataManagementAccount to tokenize 
     * @throws HpcException
     */
    public HpcDataManagementAccount getHpcDataManagementAccount(Object proxyAccount) throws HpcException;
    
    /**
     * Create Proxy data management account from cached HPC data management account 
     * @param managementAccount
     * @return
     * @throws HpcException
     */
    public Object getProxyManagementAccount(HpcDataManagementAccount managementAccount) throws HpcException;
    
    /**
     * Get Base path 
     *
     * @param path The path to build the absolute path to the middleware (iRODS).
     * @return The absolute path.
     */
    public String getBasePath(); 
    
    
    /**
     * Refresh all materialized views.
     *
     * @throws HpcException
     */
    public void refreshViews() throws HpcException;
    
    /**
     * Get colelction metadata validation rules for a DOC.
     * 
     * @param doc The DOC.
     * @return A list of HpcMetadataValidationRule
     *
     * @throws HpcException
     */
    public List<HpcMetadataValidationRule> 
           getCollectionMetadataValidationRules(String doc) throws HpcException;
    
    /**
     * Get data object metadata validation rules for a DOC.
     * 
     * @param doc The DOC.
     * @return A list of HpcMetadataValidationRule
     *
     * @throws HpcException
     */
    public List<HpcMetadataValidationRule> 
           getDataObjectMetadataValidationRules(String doc) throws HpcException;
    
    /**
     * Get data hierarchy of a DOC.
     * 
     * @param doc The DOC.
     * @return HpcDataHierarchy
     *
     * @throws HpcException
     */
    public HpcDataHierarchy getDataHierarchy(String doc) throws HpcException;
    
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
     * Validate a path against a hierarchy definition.
     *
     * @param path The collection path.
     * @param dataObjectRegistration If true, the path is of a data object being registered, otherwise it's 
     *                               a collection registration.
     * 
     * @throws HpcException If the hierarchy is invalid.
     */
    public void validateHierarchy(String path, boolean dataObjectRegistration) throws HpcException;
    
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
     * Get the search results page size.
     *
     * @return The search results page size.
     */
    public int getSearchResultsPageSize();
}

 