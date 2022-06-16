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
	 * @return A migration task ID.
	 * @throws HpcException on service failure.
	 */
	public HpcDataMigrationTask createDataObjectMigrationTask(String path, String userId, String configurationId,
			String fromS3ArchiveConfigurationId, String toS3ArchiveConfigurationId, String collectionMigrationTaskId)
			throws HpcException;

	/**
	 * Get a list of data object migration tasks in specific status and type.
	 *
	 * @param status The data migration status to query for.
	 * @param type   The data migration type to query for.
	 * @return A List of data migration tasks
	 * @throws HpcException on service failure.
	 */
	public List<HpcDataMigrationTask> getDataMigrationTasks(HpcDataMigrationStatus status, HpcDataMigrationType type)
			throws HpcException;

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
	 *                                migration failed
	 * @throws HpcException on service failure.
	 */
	public void completeDataObjectMigrationTask(HpcDataMigrationTask dataObjectMigrationTask,
			HpcDataMigrationResult result, String message) throws HpcException;

	/**
	 * Complete a bulk migration task (Collection, Data Object List or Collection List).
	 *
	 * @param bulkMigrationTask The bulk migration task.
	 * @param message                 (Optional) An error message in case the
	 *                                migration failed
	 * @throws HpcException on service failure.
	 */
	public void completeBulkMigrationTask(HpcDataMigrationTask bulkMigrationTask, String message)
			throws HpcException;

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
	 * @return A migration task ID.
	 * @throws HpcException on service failure.
	 */
	public HpcDataMigrationTask createCollectionMigrationTask(String path, String userId, String configurationId,
			String toS3ArchiveConfigurationId) throws HpcException;

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

}
