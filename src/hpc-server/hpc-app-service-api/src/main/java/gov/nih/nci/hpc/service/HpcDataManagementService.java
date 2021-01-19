/**
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import gov.nih.nci.hpc.domain.datamanagement.HpcAuditRequestType;
import gov.nih.nci.hpc.domain.datamanagement.HpcBulkDataObjectRegistrationTaskStatus;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationResult;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationStatus;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationRequest;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationResult;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Management Application Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcDataManagementService {
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
	 * Determine if path refers to collection or data file.
	 *
	 * @param path The path.
	 * @return Boolean to convey whether path refers to collection or data file;
	 *         true for collection and false for data file.
	 * @throws HpcException on data management system failure
	 */
	public boolean interrogatePathRef(String path) throws HpcException;

	/**
	 * Create a data object's file.
	 *
	 * @param path The data object path.
	 * @return true if the data object file was created, or false if it already
	 *         exists.
	 * @throws HpcException on service failure.
	 */
	public boolean createFile(String path) throws HpcException;

	/**
	 * Delete a path (data object or collection).
	 *
	 * @param path  The path to delete.
	 * @param quiet If set to true, no exception is thrown in case of a failure.
	 * @throws HpcException on service failure (unless 'quiet' is set to 'true').
	 */
	public void delete(String path, boolean quiet) throws HpcException;

	/**
	 * Move a path (data object or collection).
	 *
	 * @param sourcePath         The data object or collection path to move.
	 * @param destinationPath    The destination path to move to.
	 * @param pathTypeValidation (Optional) True to validate if the path is a
	 *                           collection, False to validate if it is a data
	 *                           object. If null - no path type validation is
	 *                           performed.
	 * @throws HpcException on service failure.
	 */
	public void move(String sourcePath, String destinationPath, Optional<Boolean> pathTypeValidation)
			throws HpcException;

	/**
	 * Add an audit record in the DB Note: Currently, there is no 'audit trail'
	 * functionality implemented. iRODS has this capability, and there is a plan to
	 * use it. This is a temporary solution to have a record of user attempts to
	 * delete or update data objects and collections. When the permanent solution is
	 * implemented (using iRODS capability) this API method and the DAO behind it
	 * should be retired.
	 *
	 * @param path                 The data object or collection path.
	 * @param requestType          The request being recorded for audit.
	 * @param metadataBefore       The collection or data object metadata before the
	 *                             request.
	 * @param metadataAfter        (Optional) The collection or data object metadata
	 *                             after the request.
	 * @param archiveLocation      (Optional) The location of the file in the
	 *                             archive (prior to deletion).
	 * @param dataManagementStatus Data management (iRODS) request completion
	 *                             status.
	 * @param dataTransferStatus   (Optional) Data transfer (archive) request
	 *                             completion status.
	 * @param message              (Optional) Error message if the request failed.
	 * @param userId               (Optional) userId if not executed in a user
	 *                             thread.
	 */
	public void addAuditRecord(String path, HpcAuditRequestType requestType, HpcMetadataEntries metadataBefore,
			HpcMetadataEntries metadataAfter, HpcFileLocation archiveLocation, boolean dataManagementStatus,
			Boolean dataTransferStatus, String message, String userId);

	/**
	 * Set collection permission for a subject (user or group).
	 *
	 * @param path              The collection path.
	 * @param subjectPermission The subject permission request.
	 * @throws HpcException on service failure.
	 */
	public void setCollectionPermission(String path, HpcSubjectPermission subjectPermission) throws HpcException;

	/**
	 * Get collection permissions.
	 *
	 * @param path The collection path.
	 * @return A list of permissions on the collection.
	 * @throws HpcException on service failure.
	 */
	public List<HpcSubjectPermission> getCollectionPermissions(String path) throws HpcException;

	/**
	 * Get collection permissions for a given user.
	 *
	 * @param path   The collection path.
	 * @param userId The user-id to get permissions for.
	 * @return permission on the collection.
	 * @throws HpcException on service failure.
	 */
	public HpcSubjectPermission getCollectionPermission(String path, String userId) throws HpcException;

	/**
	 * Acquire permission of given user on a given collection. Ensures that even if
	 * given user has no permission on given collection, an instance of the return
	 * type is returned indicating permission set to level of "NONE".
	 *
	 * @param path   Path of given collection.
	 * @param userId UserId of given user.
	 * @return <code>HpcSubjectPermission</code> representing permission that given
	 *         user has on given collection.
	 * @throws HpcException on service failure.
	 */
	public HpcSubjectPermission acquireCollectionPermission(String path, String userId) throws HpcException;

	/**
	 * Get collection permission (for the request invoker)
	 *
	 * @param path The data object path.
	 * @return permission on the data object.
	 * @throws HpcException on service failure.
	 */
	public HpcSubjectPermission getCollectionPermission(String path) throws HpcException;

	/**
	 * Set data object permission for a subject (user or group).
	 *
	 * @param path              The data object path.
	 * @param subjectPermission The subject permission request.
	 * @throws HpcException on service failure.
	 */
	public void setDataObjectPermission(String path, HpcSubjectPermission subjectPermission) throws HpcException;

	/**
	 * Get data object permissions.
	 *
	 * @param path The data object path.
	 * @return A list of permissions on the data object.
	 * @throws HpcException on service failure.
	 */
	public List<HpcSubjectPermission> getDataObjectPermissions(String path) throws HpcException;

	/**
	 * Acquire permission of given user on a given DataObject. Ensures that even if
	 * given user has no permission on given collection, an instance of the return
	 * type is returned indicating permission set to level of "NONE".
	 *
	 * @param path   Path of given DataObject.
	 * @param userId UserId of given user.
	 * @return <code>HpcSubjectPermission</code> representing permission that given
	 *         user has on given collection.
	 * @throws HpcException on service failure.
	 */
	public HpcSubjectPermission acquireDataObjectPermission(String path, String userId) throws HpcException;

	/**
	 * Get data object permission for a given user.
	 *
	 * @param path   The data object path.
	 * @param userId The user-id to get permissions for.
	 * @return permission on the data object.
	 * @throws HpcException on service failure.
	 */
	public HpcSubjectPermission getDataObjectPermission(String path, String userId) throws HpcException;

	/**
	 * Get data object permission (for the request invoker)
	 *
	 * @param path The data object path.
	 * @return permission on the data object.
	 * @throws HpcException on service failure.
	 */
	public HpcSubjectPermission getDataObjectPermission(String path) throws HpcException;

	/**
	 * Set a co-ownership on an entity. Both the user-id and the system account will
	 * be assigned as owners.
	 *
	 * @param path   The entity path.
	 * @param userId The user-id
	 * @throws HpcException on service failure.
	 */
	public void setCoOwnership(String path, String userId) throws HpcException;

	/**
	 * Validate a path against a hierarchy definition.
	 *
	 * @param path                   The collection path.
	 * @param configurationId        Use validation rules of this data management
	 *                               configuration.
	 * @param dataObjectRegistration If true, the service validates if data object
	 *                               registration is allowed in this collection.
	 * @throws HpcException If the hierarchy is invalid.
	 */
	public void validateHierarchy(String path, String configurationId, boolean dataObjectRegistration)
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
	 * Get data objects that have their data transfer upload in-progress.
	 *
	 * @return A list of data objects.
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataObject> getDataObjectsUploadInProgress() throws HpcException;

	/**
	 * Get data objects that have their data transfer upload by users via URL.
	 *
	 * @return A list of data objects.
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataObject> getDataTranferUploadInProgressWithGeneratedURL() throws HpcException;

	/**
	 * Get data objects that have their data transfer upload in progress via
	 * streaming.
	 *
	 * @return A list of data objects.
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataObject> getDataTranferUploadStreamingInProgress() throws HpcException;

	/**
	 * Get data objects that have their data transfer upload via streaming has
	 * stopped.
	 *
	 * @return A list of data objects.
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataObject> getDataTranferUploadStreamingStopped() throws HpcException;

	/**
	 * Get data objects that have their data stored in temporary archive.
	 *
	 * @return A list of data objects.
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataObject> getDataObjectsUploadInTemporaryArchive() throws HpcException;

	/**
	 * Get data objects that have their data placed in DME server file system for an
	 * upload.
	 *
	 * @return A list of data objects.
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataObject> getDataObjectsUploadInFileSystem() throws HpcException;

	/**
	 * Get all the links to a data object path.
	 *
	 * @param path The data object path to get links to.
	 * @return A list of data objects linked to the path
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataObject> getDataObjectLinks(String path) throws HpcException;

	/** Close connection to Data Management system for the current service call. */
	public void closeConnection();

	/**
	 * Data objects registration.
	 *
	 * @param userId                         The user ID requested the registration.
	 * @param uiURL                          (Optional) A URL for viewing the
	 *                                       registration task on a UI. Note:
	 *                                       {task_id} in this URL will be replaced
	 *                                       with actual task ID value.
	 * @param dataObjectRegistrationRequests The data object registration requests.
	 * @return The task ID created to register the data objects and can be used to
	 *         track status
	 * @throws HpcException on service failure.
	 */
	public String registerDataObjects(String userId, String uiURL,
			Map<String, HpcDataObjectRegistrationRequest> dataObjectRegistrationRequests) throws HpcException;

	/**
	 * Get bulk object registration tasks.
	 *
	 * @param status Get tasks in this status.
	 * @return A list of data object list registration tasks.
	 * @throws HpcException on service failure.
	 */
	public List<HpcBulkDataObjectRegistrationTask> getBulkDataObjectRegistrationTasks(
			HpcBulkDataObjectRegistrationTaskStatus status) throws HpcException;

	/**
	 * Update a bulk data object registration task.
	 *
	 * @param registrationTask The registration task to update.
	 * @throws HpcException on service failure.
	 */
	public void updateBulkDataObjectRegistrationTask(HpcBulkDataObjectRegistrationTask registrationTask)
			throws HpcException;

	/**
	 * Complete a bulk data object registration task: 1. Update task info in DB with
	 * results info.
	 *
	 * @param registrationTask The registration task to complete.
	 * @param result           The result of the task (true is successful, false is
	 *                         failed).
	 * @param message          (Optional) If the task failed, a message describing
	 *                         the failure.
	 * @param completed        (Optional) The download task completion timestamp.
	 * @throws HpcException on service failure.
	 */
	public void completeBulkDataObjectRegistrationTask(HpcBulkDataObjectRegistrationTask registrationTask,
			boolean result, String message, Calendar completed) throws HpcException;

	/**
	 * Get bulk data object registration task status.
	 *
	 * @param taskId The registration task ID.
	 * @return A download status object, or null if the task can't be found. Note:
	 *         The returned object is associated with a 'task' object if the
	 *         registration is in-progress. If the registration completed or failed,
	 *         the returned object is associated with a 'result' object.
	 * @throws HpcException on service failure.
	 */
	public HpcBulkDataObjectRegistrationStatus getBulkDataObjectRegistrationTaskStatus(String taskId)
			throws HpcException;

	/**
	 * Get active registration tasks for a user.
	 *
	 * @param userId The user ID to query for.
	 * @param doc    doc of group admin or all for system administrators
	 * @return A list of active registration requests.
	 * @throws HpcException on service failure.
	 */
	public List<HpcBulkDataObjectRegistrationTask> getRegistrationTasks(String userId, String doc) throws HpcException;

	/**
	 * Get registration results (all completed registration) for a user.
	 *
	 * @param userId The user ID to query for.
	 * @param page   The requested results page.
	 * @param doc    doc of group admin or all for system administrators
	 * @return A list of completed download requests.
	 * @throws HpcException on service failure.
	 */
	public List<HpcBulkDataObjectRegistrationResult> getRegistrationResults(String userId, int page, String doc)
			throws HpcException;

	/**
	 * Get registration results (all completed download requests) count for a user.
	 *
	 * @param userId The user ID to query for.
	 * @param doc    doc of group admin or all for system administrators
	 * @return A total count of completed registration requests.
	 * @throws HpcException on service failure.
	 */
	public int getRegistrationResultsCount(String userId, String doc) throws HpcException;

	/**
	 * Get the registration results page size.
	 *
	 * @return The download results page size.
	 */
	public int getRegistrationResultsPageSize();

	/**
	 * Get a collection type of a path.
	 *
	 * @param path The collection path.
	 * @return The collection type.
	 * @throws HpcException on service failure.
	 */
	public String getCollectionType(String path) throws HpcException;

	/**
	 * Get all Data Management Configurations.
	 *
	 * @return Data management configuration.
	 */
	public List<HpcDataManagementConfiguration> getDataManagementConfigurations();

	/**
	 * Find data management configuration ID for a given path. This is searched by
	 * matching the given path to all configuration base paths. If any configuration
	 * base path is found to be the 'base path' of the given path, then the
	 * corresponding configuration ID is returned
	 *
	 * @param path the path to find a config ID for.
	 * @return A configuration ID if matched by base path, or null otherwise
	 */
	public String findDataManagementConfigurationId(String path);

	/**
	 * Get data management configuration ID by base path.
	 *
	 * @param basePath The base path to get the config for.
	 * @return A configuration ID, or null if not found.
	 */
	public String getDataManagementConfigurationId(String basePath);

	/**
	 * Get data management configuration by ID.
	 *
	 * @param id The configuration ID.
	 * @return A configuration ID, or null if not found.
	 */
	public HpcDataManagementConfiguration getDataManagementConfiguration(String id);

	/**
	 * Add a data object registration request result to the DB.
	 *
	 * @param path                    The data object path.
	 * @param systemGeneratedMetadata The data object system generated metadata.
	 * @param result                  the registration request result (success/fail
	 *                                indicator).
	 * @param message                 (Optional) If the registration failed, a
	 *                                message describing the failure.
	 * @throws HpcException on service failure.
	 */
	public void addDataObjectRegistrationResult(String path, HpcSystemGeneratedMetadata systemGeneratedMetadata,
			boolean result, String message) throws HpcException;

	/**
	 * Add a data object registration request result to the DB.
	 *
	 * @param path                         The data object path.
	 * @param dataObjectRegistrationResult The data object registration result.
	 * @param size                         The data object size
	 * @throws HpcException on service failure.
	 */
	public void addDataObjectRegistrationResult(String path,
			HpcDataObjectRegistrationResult dataObjectRegistrationResult, Long size) throws HpcException;
}
