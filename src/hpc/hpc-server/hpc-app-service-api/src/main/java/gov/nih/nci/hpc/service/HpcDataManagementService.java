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
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.model.HpcDataObjectSystemGeneratedMetadata;
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
     * @param createParentPathDirectory If set to true, create the directory for the file.
     * @return true if the data object file was created, or false if it already exists.
     * 
     * @throws HpcException
     */
    public boolean createFile(String path, boolean createParentPathDirectory) 
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
     *
     * @param path The collection path.
     * 
     * @throws HpcException
     */
    public void addSystemGeneratedMetadataToCollection(String path) throws HpcException; 
    
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
     *      9. Data Transfer Request ID. (Optional)
     *      10. Data Transfer Status.
     *      11. Data Transfer Type.
     *      11. Data Object File(s) size. (Optional)
     *
     * @param path The data object path.
     * @param archiveLocation The physical file archive location.
     * @param sourceLocation (Optional) The source location of the file.
     * @param dataTransferRequestId (Optional) The data transfer request ID.
     * @param dataTransferStatus The data transfer status.
     * @param dataTransferType The data transfer type.
     * @param dataTransferRequestId (Optional) The data transfer request ID.
     * 
     * 
     * @throws HpcException
     */
    public void addSystemGeneratedMetadataToDataObject(String path, 
    		                                           HpcFileLocation archiveLocation,
    		                                           HpcFileLocation sourceLocation,
    		                                           String dataTransferRequestId,
    		                                           HpcDataTransferStatus dataTransferStatus,
    		                                           HpcDataTransferType dataTransferType,
    		                                           Long sourceSize) 
    		                                          throws HpcException; 
    
    /**
     * Get the system generated metadata of a data object.
     *
     * @param path The data object path.
     * @return HpcDataObjectSystemGeneratedMetadata The system generated metadata
     * 
     * @throws HpcException
     */
    public HpcDataObjectSystemGeneratedMetadata 
              getDataObjectSystemGeneratedMetadata(String path) throws HpcException; 
    
    /**
     * Get the system generated metadata of a data object.
     *
     * @param dataObjectMetadata The data object metadata.
     * 
     * @throws HpcException
     */
    public HpcDataObjectSystemGeneratedMetadata 
              getDataObjectSystemGeneratedMetadata(List<HpcMetadataEntry> dataObjectMetadata) 
            		                              throws HpcException; 
    
    /** 
     * Set a data transfer status of a data object.
     * 
     * @param path The data object (logical) path.
     * @param HpcDataTransferStatus The data transfer status to set
     * 
     * @throws HpcException
     */
	public void setDataTransferStatus(String path, 
                                      HpcDataTransferStatus dataTransferStatus) 
                                     throws HpcException;
	
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
     * @return HpcCollection list.
     * 
     * @throws HpcException
     */
    public List<HpcCollection> getCollections(
    		    List<HpcMetadataQuery> metadataQueries) throws HpcException;
    
    /**
     * Get metadata of a collection.
     *
     * @param path The collection path.
     * @return HpcMetadataEntry collection.
     * 
     * @throws HpcException
     */
    public List<HpcMetadataEntry> getCollectionMetadata(String path) throws HpcException;
    
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
     * @return HpcDataObject list.
     * 
     * @throws HpcException
     */
    public List<HpcDataObject> getDataObjects(
    		    List<HpcMetadataQuery> metadataQueries) throws HpcException;
    
    /**
     * Get data objects that have their data transfer in-progress
     *
     * @return HpcDataObject list.
     * 
     * @throws HpcException
     */
    public List<HpcDataObject> getDataObjectsInProgress() throws HpcException;
    
    /**
     * Get metadata of a data object.
     *
     * @param path The collection path.
     * @return HpcMetadataEntry collection.
     * 
     * @throws HpcException
     */
    public List<HpcMetadataEntry> getDataObjectMetadata(String path) throws HpcException;
    
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
    		                               HpcUserPermission permissionRequest) 
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
}

 