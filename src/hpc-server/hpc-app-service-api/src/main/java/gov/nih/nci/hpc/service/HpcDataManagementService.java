/**
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObjectListRegistrationTaskStatus;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataObjectListRegistrationStatus;
import gov.nih.nci.hpc.domain.model.HpcDataObjectListRegistrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationRequest;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Management Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcDataManagementService 
{   
    /**
     * Create a collection's directory.
     *
     * @param path The collection path.
     * @return true if the directory was created, or false if it already exists.
     * @throws HpcException on service failure.
     */
    public boolean createDirectory(String path) throws HpcException;
    
    /**
     * Check if the path's parent is a directory.
     *
     * @param path The path.
     * @return true if the parent path is a directory, or false otherwise.
     * @throws HpcException on data management system failure.
     */
    public boolean isPathParentDirectory(String path) throws HpcException;   
    
    /**
     * Create a data object's file.
     *
     * @param path The data object path.
     * @return true if the data object file was created, or false if it already exists.
     * @throws HpcException on service failure.
     */
    public boolean createFile(String path) throws HpcException;
    
    /**
     * Delete a path (data object or directory). 
     *
     * @param path The path to delete.
     * @param quiet If set to true, no exception is thrown in case of a failure.
     * @throws HpcException on service failure (unless 'quiet' is set to 'true').
     */
    public void delete(String path, boolean quiet) throws HpcException;
    
    /**
     * Save a record of the deletion in the DB.
     * Note: Currently, there is no 'audit trail' functionality implemented. iRODS has this capability, and there
     * is a plan to use it. This is a temporary solution to have a record of what data objects deleted, by
     * who and when. When the permanent solution is implemented (using iRODS capability) this API method and the
     * DAO behind it should be retired.
     *
     * @param path The path to delete.
     * @param archiveLocation The physical file location in the archive.
     * @param archiveDeleteStatus True if the physical file was successfully removed from archive.
     * @param metadataEntries The metadata associated with this path.
     * @param dataManagementDeleteStatus True if data object was removed from the data management system.
     * @param message (Optional) Error message received in case the deletion request failed. 
     * @throws HpcException on service failure.
     */
    public void saveDataObjectDeletionRequest(String path, HpcFileLocation archiveLocation, 
    		                                  boolean archiveDeleteStatus, HpcMetadataEntries metadataEntries,
    		                                  boolean dataManagementDeleteStatus, String message) 
    		                                 throws HpcException;
    
    /**
     * Set collection permission for a subject (user or group). 
     *
     * @param path The collection path.
     * @param subjectPermission The subject permission request.
     * @throws HpcException on service failure.
     */
    public void setCollectionPermission(String path, HpcSubjectPermission subjectPermission) 
    		                           throws HpcException;
    
    /**
     * Get collection permissions. 
     *
     * @param path The collection path.
     * @return A list of permissions on the collection.
     * @throws HpcException on service failure.
     */
    public List<HpcSubjectPermission> getCollectionPermissions(String path) throws HpcException;
    
    /**
     * Get collection permissions for userId. 
     *
     * @param path The collection path.
     * @param userId The user-id to get permissions for.
     * @return permission on the collection.
     * @throws HpcException on service failure.
     */
    public HpcSubjectPermission getCollectionPermissionForUser(String path, String userId) throws HpcException;

    /**
     * Set data object permission for a subject (user or group). 
     *
     * @param path The data object path.
     * @param subjectPermission The subject permission request.
     * @throws HpcException on service failure.
     */
    public void setDataObjectPermission(String path, HpcSubjectPermission subjectPermission) 
    		                           throws HpcException;
    
    /**
     * Get data object permissions. 
     *
     * @param path The data object path.
     * @return A list of permissions on the data object.
     * @throws HpcException on service failure.
     */
    public List<HpcSubjectPermission> getDataObjectPermissions(String path) throws HpcException;
    
    /**
     * Get data object permission by userId. 
     *
     * @param path The data object path.
     * @param userId The user-id to get permissions for.
     * @return permission on the data object.
     * @throws HpcException on service failure.
     */
    public HpcSubjectPermission getDataObjectPermissionForUser(String path, String userId) throws HpcException;
    
    /**
     * Get data object permission (for the request invoker) 
     *
     * @param path The data object path.
     * @return permission on the data object.
     * @throws HpcException on service failure.
     */
    public HpcSubjectPermission getDataObjectPermission(String path) throws HpcException;

    /**
     * Set a co-ownership on an entity. Both the user-id and the system account will be assigned
     * as owners.
     *
     * @param path The entity path.
     * @param userId The user-id
     * @throws HpcException on service failure.
     */
    public void setCoOwnership(String path, String userId) throws HpcException;
    
    /**
     * Validate a path against a hierarchy definition.
     *
     * @param path The collection path.
     * @param configurationId Use validation rules of this data management configuration.
     * @param dataObjectRegistration If true, the service validates if data object registration is allowed 
     *                               in this collection.
     * @throws HpcException If the hierarchy is invalid.
     */
    public void validateHierarchy(String path, String configurationId,
    		                      boolean dataObjectRegistration) 
    		                     throws HpcException;
    
    /**
     * Get collection by its path.
     *
     * @param path The collection's path.
     * @param list An indicator to list sub-collections and data-objects.
     * @return A collection.
     * @throws HpcException on service failure.
     */
    public HpcCollection getCollection(String path, boolean list) throws HpcException;
    
    /**
     * Get collection children by its path. No collection metadata is returned.
     *
     * @param path The collection's path.
     * @return A collection.
     * @throws HpcException on service failure.
     */
    public HpcCollection getCollectionChildren(String path) throws HpcException;

    /**
     * Get data object by its path.
     *
     * @param path The data object's path.
     * @return A data object.
     * @throws HpcException on service failure.
     */
    public HpcDataObject getDataObject(String path) throws HpcException;
    
    /**
     * Get data objects that have their data transfer received (i.e. queued).
     *
     * @return A list of data objects.
     * @throws HpcException on service failure.
     */
    public List<HpcDataObject> getDataObjectsUploadReceived() throws HpcException;
    
    /**
     * Get data objects that have their data transfer in-progress.
     *
     * @return A list of data objects.
     * @throws HpcException on service failure.
     */
    public List<HpcDataObject> getDataObjectsUploadInProgress() throws HpcException;
    
    /**
     * Get data objects that have their data stored in temporary archive.
     *
     * @return A list of data objects.
     * @throws HpcException on service failure.
     */
    public List<HpcDataObject> getDataObjectsUploadInTemporaryArchive() throws HpcException;
    
    /**
     * Close connection to Data Management system for the current service call.
     */
    public void closeConnection();
    
    /**
     * Get all Data Management Configurations.
     * 
     * @return Data management configuration.
     */
    public List<HpcDataManagementConfiguration> getDataManagementConfigurations();

    /**
     * Data objects registration.
     *
     * @param userId The user ID requested the registration.
     * @param doc The registrar DOC.
     * @param dataObjectRegistrationRequests The data object registration requests.
     * @return The task ID created to register the data objects and can be used to track status
     * @throws HpcException on service failure.
     */
    public String registerDataObjects(String userId, String doc, 
    		                          Map<String, HpcDataObjectRegistrationRequest> dataObjectRegistrationRequests)
    				                 throws HpcException;
    
    /**
     * Get data object list registration tasks. 
     *
     * @param status Get tasks in this status.
     * @return A list of data object list registration tasks.
     * @throws HpcException on service failure.
     */
    public List<HpcDataObjectListRegistrationTask> getDataObjectListRegistrationTasks(
    		                                              HpcDataObjectListRegistrationTaskStatus status) 
    		                                              throws HpcException;
    
    /** 
     * Update a data object list registration task.
     * 
     * @param registrationTask The registration task to update.
     * @throws HpcException on service failure.
     */
    public void updateDataObjectListRegistrationTask(HpcDataObjectListRegistrationTask registrationTask)
                                                    throws HpcException;
    
    /**
     * Complete a data object list registration task:
     * 1. Update task info in DB with results info.
     *
     * @param registrationTask The registration task to complete.
     * @param result The result of the task (true is successful, false is failed).
     * @param message (Optional) If the task failed, a message describing the failure.
     * @param completed (Optional) The download task completion timestamp.
     * @throws HpcException on service failure.
     */
    public void completeDataObjectListRegistrationTask(HpcDataObjectListRegistrationTask registrationTask,
    		                                           boolean result, String message, Calendar completed) 
    		                                          throws HpcException;
    
    /**
     * Get data object list task status. 
     *
     * @param taskId The registration task ID.
     * @return A download status object, or null if the task can't be found.
     *         Note: The returned object is associated with a 'task' object if the registration 
     *         is in-progress. If the registration completed or failed, the returned object is associated with a 
     *         'result' object. 
     * @throws HpcException on service failure.
     */
    public HpcDataObjectListRegistrationStatus getDataObjectListRegistrationTaskStatus(String taskId) 
    		                                                                          throws HpcException;
    
    /**
     * Get a collection type of a path.
     *
     * @param path The collection path.
     * @return The collection type.
     * @throws HpcException on service failure.
     */
    public String getCollectionType(String path) throws HpcException;
    
	/**
     * Get data management configuration ID for a given path. This is calculated
     * based on matching configuration base path to the given path.
     *
     * @param path the path to get a config ID for.
     * @return A configuration ID if matched by base path, or null otherwise
     */
	public String getConfigurationId(String path);
}

 