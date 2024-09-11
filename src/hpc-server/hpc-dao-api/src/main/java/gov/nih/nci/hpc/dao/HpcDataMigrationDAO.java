/**
 * HpcDataDownloadDAO.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationResult;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationStatus;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationType;
import gov.nih.nci.hpc.domain.model.HpcDataMigrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataMigrationTaskResult;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Download DAO Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcDataMigrationDAO {
	/**
	 * Upsert a data migration task. Note: If a new task is inserted,
	 * dataMigrationTask.getId() will be updated with the generated ID.
	 *
	 * @param dataMigrationTask The data migration task to persist.
	 * @throws HpcException on database error.
	 */
	public void upsertDataMigrationTask(HpcDataMigrationTask dataMigrationTask) throws HpcException;

	/**
	 * Get a list of data migration tasks in specific status and type.
	 *
	 * @param status   The data migration status to query for.
	 * @param type     The data migration type to query for.
	 * @param serverId The server ID to get tasks to be processed.
	 * @return A List of data migration tasks
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataMigrationTask> getDataMigrationTasks(HpcDataMigrationStatus status, HpcDataMigrationType type,
			String serverId) throws HpcException;

	/**
	 * Get a list of unassigned data migration tasks.
	 *
	 * @return A List of data migration tasks
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataMigrationTask> getUnassignedDataMigrationTasks() throws HpcException;

	/**
	 * Get a list of data object migration tasks that associated with specific a
	 * collection migration task
	 *
	 * @param collectionMigrationTaskId The collection migration task id.
	 * @return A List of data object migration tasks
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataMigrationTask> getDataObjectMigrationTasks(String collectionMigrationTaskId) throws HpcException;

	/**
	 * Get a data object migration task that associated with specific a collection
	 * migration task and path.
	 *
	 * @param collectionMigrationTaskId The collection migration task id.
	 * @param path                      The data object path.
	 * @return A data object migration task if found, or null otherwise.
	 * @throws HpcException on service failure.
	 */
	public HpcDataMigrationTask getDataObjectMigrationTask(String collectionMigrationTaskId, String path)
			throws HpcException;

	/**
	 * Get a data object migration task.
	 *
	 * @param id   The migration task ID.
	 * @param type The migration task type (data-object or collection).
	 * @return A data object migration task if found, or null otherwise.
	 * @throws HpcException on service failure.
	 */
	public HpcDataMigrationTask getDataMigrationTask(String id, HpcDataMigrationType type) throws HpcException;

	/**
	 * Get a data object migration task result ID that associated with specific a
	 * collection migration task and path
	 *
	 * @param collectionMigrationTaskId The collection migration task id.
	 * @param path                      The data object path.
	 * @return A data object migration task result ID if found, or null otherwise.
	 * @throws HpcException on service failure.
	 */
	public String getDataObjectMigrationTaskResultId(String collectionMigrationTaskId, String path) throws HpcException;

	/**
	 * Get a migration task result.
	 *
	 * @param id   The migration task ID.
	 * @param type The migration task type (data-object or collection).
	 * @return The migration task result object, or null if not found.
	 * @throws HpcException on database error.
	 */
	public HpcDataMigrationTaskResult getDataMigrationTaskResult(String id, HpcDataMigrationType type)
			throws HpcException;

	/**
	 * Get a list of migration tasks in specific status and type.
	 *
	 * @param parentId The task parent Id to query for.
	 * @param result   The task result to query for.
	 * @return A List of data migration task results.
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataMigrationTaskResult> getDataMigrationResults(String parentId, HpcDataMigrationResult result)
			throws HpcException;

	/**
	 * Delete a data Migration task.
	 *
	 * @param id The data migration task ID.
	 * @throws HpcException on database error.
	 */
	public void deleteDataMigrationTask(String id) throws HpcException;

	/**
	 * Upsert a data migration task result.
	 *
	 * @param dataMigrationTask The data migration task to persist.
	 * @param completed         The time the migration task completed.
	 * @param result            The migration result
	 * @param message           (Optional) error message in case of failure
	 * @throws HpcException on database error.
	 */
	public void upsertDataMigrationTaskResult(HpcDataMigrationTask dataMigrationTask, Calendar completed,
			HpcDataMigrationResult result, String message) throws HpcException;

	/**
	 * Update the status of all data migration tasks.
	 *
	 * @param fromStatus      Only update tasks in this status.
	 * @param inProcess       The in-process value to set.
	 * @param serverId        The server ID to update task status for.
	 * @param percentComplete The percent complete.
	 * @param toStatus        The status to set
	 * @throws HpcException on database error.
	 */
	public void setDataMigrationTasksStatus(HpcDataMigrationStatus fromStatus, boolean inProcess, String serverId,
			int percentComplete, HpcDataMigrationStatus toStatus) throws HpcException;

	/**
	 * Get result counts for items in a collection migration task
	 *
	 * @param collectionMigrationTaskId The collection migration task id.
	 * @return Map of migration result values to counts.
	 * @throws HpcException on database error.
	 */
	public Map<HpcDataMigrationResult, Integer> getCollectionMigrationResultCount(String collectionMigrationTaskId)
			throws HpcException;

	/**
	 * Set a migration task in-process value.
	 *
	 * @param id        The data migration task ID.
	 * @param inProcess The value to set.
	 * @return True if the value of inProcess was actually updated.
	 * @throws HpcException on database error.
	 */
	public boolean setDataMigrationTaskInProcess(String id, boolean inProcess) throws HpcException;

	/**
	 * Set a migration task server-id.
	 *
	 * @param id       The data migration task ID.
	 * @param serverId The server ID to work this task.
	 * @throws HpcException on database error.
	 */
	public void setDataMigrationTaskServerId(String id, String serverId) throws HpcException;

	/**
	 * Update a bulk data migration precent complete.
	 *
	 * @param id The bulk data migration task ID (collection, list of objects, list
	 *           of collections)
	 * @throws HpcException on database error.
	 */
	public void updateBulkDataMigrationTaskPercentComplete(String id) throws HpcException;

	/**
	 * Cleanup migration tasks that are completed w/ result.
	 *
	 * @return The number of records cleand up.
	 * @throws HpcException on database error.
	 */
	public int cleanupDataMigrationTasks() throws HpcException;
}
