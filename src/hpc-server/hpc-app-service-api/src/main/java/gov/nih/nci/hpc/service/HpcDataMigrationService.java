/**
 * HpcDataSearchService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import java.util.List;

import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationResult;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationStatus;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationType;
import gov.nih.nci.hpc.domain.model.HpcDataMigrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataMigrationTaskStatus;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Migration Application Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcDataMigrationService {
	/**
	 * Create a data object migration task.
	 *
	 * @param path                         The data object path.
	 * @param userId                       The user Id requested the migration.
	 * @param configurationId              The The data object configuration ID.
	 * @param fromS3ArchiveConfigurationId The migration source S3 archive
	 *                                     configuration ID.
	 * @param toS3ArchiveConfigurationId   The migration target S3 archive
	 *                                     configuration ID.
	 * @param collectionMigrationTaskId    (Optional) The collection migration task
	 *                                     ID that is associated w/ this data object
	 *                                     migration task
	 * @param alignArchivePath             If true, the file is moved within its
	 *                                     current archive to align w/ the iRODs
	 *                                     path.
	 * @param size                         The data object size.
	 * @param retryTaskId                  The previous task ID if this is a retry
	 *                                     request.
	 * @param retryUserId                  The user retrying the request if this is
	 *                                     a retry request.
	 * @return A migration task ID.
	 * @throws HpcException on service failure.
	 */
	public HpcDataMigrationTask createDataObjectMigrationTask(String path, String userId, String configurationId,
			String fromS3ArchiveConfigurationId, String toS3ArchiveConfigurationId, String collectionMigrationTaskId,
			boolean alignArchivePath, long size, String retryTaskId, String retryUserId) throws HpcException;

	/**
	 * Get a list of migration tasks in specific status and type.
	 *
	 * @param status The data migration status to query for.
	 * @param type   The data migration type to query for.
	 * @return A List of data migration tasks
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataMigrationTask> getDataMigrationTasks(HpcDataMigrationStatus status, HpcDataMigrationType type)
			throws HpcException;

	/**
	 * Assign data migration tasks to servers
	 *
	 * @throws HpcException on service failure.
	 */
	public void assignDataMigrationTasks() throws HpcException;

	/**
	 * Migrate a data object.
	 *
	 * @param dataObjectMigrationTask The data migration details.
	 * @throws HpcException on service failure.
	 */
	public void migrateDataObject(HpcDataMigrationTask dataObjectMigrationTask) throws HpcException;

	/**
	 * Complete a data object migration task.
	 *
	 * @param dataObjectMigrationTask The data migration task.
	 * @param result                  The data migration result.
	 * @param message                 (Optional) An error message in case the
	 *                                migration failed.
	 * @param fromS3ArchiveAuthToken  The from S3 archive token
	 * @param toS3ArchiveAuthToken    The to S3 archive token
	 * @throws HpcException on service failure.
	 */
	public void completeDataObjectMigrationTask(HpcDataMigrationTask dataObjectMigrationTask,
			HpcDataMigrationResult result, String message, Object fromS3ArchiveAuthToken, Object toS3ArchiveAuthToken)
			throws HpcException;

	/**
	 * Complete a bulk migration task (Collection, Data Object List or Collection
	 * List).
	 *
	 * @param bulkMigrationTask The bulk migration task.
	 * @param message           (Optional) An error message in case the migration
	 *                          failed
	 * @throws HpcException on service failure.
	 */
	public void completeBulkMigrationTask(HpcDataMigrationTask bulkMigrationTask, String message) throws HpcException;

	/**
	 * Reset migration tasks that are in-progress
	 *
	 * @throws HpcException on service failure.
	 **/
	public void resetMigrationTasksInProcess() throws HpcException;

	/**
	 * Create a collection migration task.
	 *
	 * @param path                       The collection path.
	 * @param userId                     The user Id requested the migration.
	 * @param configurationId            The The data object configuration ID.
	 * @param toS3ArchiveConfigurationId The migration target S3 archive
	 *                                   configuration ID.
	 * @param alignArchivePath           If true, the file is moved within its
	 *                                   current archive to align w/ the iRODs path.
	 * @return A migration task ID.
	 * @throws HpcException on service failure.
	 */
	public HpcDataMigrationTask createCollectionMigrationTask(String path, String userId, String configurationId,
			String toS3ArchiveConfigurationId, boolean alignArchivePath) throws HpcException;

	/**
	 * Create a data object list migration task.
	 *
	 * @param dataObjectPaths            The data object paths.
	 * @param userId                     The user Id requested the migration.
	 * @param configurationId            The The data object configuration ID.
	 * @param toS3ArchiveConfigurationId The migration target S3 archive
	 *                                   configuration ID.
	 * @return A migration task ID.
	 * @throws HpcException on service failure.
	 */
	public HpcDataMigrationTask createDataObjectsMigrationTask(List<String> dataObjectPaths, String userId,
			String configurationId, String toS3ArchiveConfigurationId) throws HpcException;

	/**
	 * Create a collection list migration task.
	 *
	 * @param dataObjectPaths            The collection paths.
	 * @param userId                     The user Id requested the migration.
	 * @param configurationId            The The data object configuration ID.
	 * @param toS3ArchiveConfigurationId The migration target S3 archive
	 *                                   configuration ID.
	 * @return A migration task ID.
	 * @throws HpcException on service failure.
	 */
	public HpcDataMigrationTask createCollectionsMigrationTask(List<String> collectionPaths, String userId,
			String configurationId, String toS3ArchiveConfigurationId) throws HpcException;

	/**
	 * Update a migration task.
	 *
	 * @param dataMigrationTask The migration task.
	 * @throws HpcException on service failure.
	 */
	public void updateDataMigrationTask(HpcDataMigrationTask dataMigrationTask) throws HpcException;

	/**
	 * Update a migration task progress (percent complete)
	 *
	 * @param dataMigrationTask The migration task.
	 * @param bytesTransferred  The bytes transferred so far.
	 * @throws HpcException on service failure.
	 */
	public void updateDataMigrationTaskProgress(HpcDataMigrationTask dataMigrationTask, long bytesTransferred)
			throws HpcException;

	/**
	 * Mark a migration task that is in a RECEIVED state 'in-process', so that the
	 * task can be started. This enables multiple threads to read off of the
	 * migration task
	 *
	 * @param dataObjectMigrationTask The data migration task to mark
	 * @param inProcess               Indicator whether the task is being actively
	 *                                processed.
	 * @return true if the inProcess value was actually updated in the DB.
	 * @throws HpcException on service failure.
	 */
	public boolean markInProcess(HpcDataMigrationTask dataObjectMigrationTask, boolean inProcess) throws HpcException;

	/**
	 * Get migration task status.
	 *
	 * @param taskId   The migration task ID.
	 * @param taskType The migration task type (data-object or collection).
	 * @return A migration status object, or null if the task can't be found. Note:
	 *         The returned object is associated with a 'task' object if the
	 *         migration is in-progress. If the migration completed or failed, the
	 *         returned object is associated with a 'result' object.
	 * @throws HpcException on service failure.
	 */
	public HpcDataMigrationTaskStatus getMigrationTaskStatus(String taskId, HpcDataMigrationType taskType)
			throws HpcException;

}
