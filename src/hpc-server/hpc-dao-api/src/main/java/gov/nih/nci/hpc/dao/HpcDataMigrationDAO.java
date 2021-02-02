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

import java.util.List;

import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationStatus;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationTask;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Download DAO Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcDataMigrationDAO {
	/**
	 * Store a data migration task. Note: If a new task is inserted,
	 * dataMigrationTask.getId() will be updated with the generated ID.
	 *
	 * @param dataMigrationTask The data migration task to persist.
	 * @throws HpcException on database error.
	 */
	public void upsertDataMigrationTask(HpcDataMigrationTask dataMigrationTask) throws HpcException;

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
	 * Delete a data Migration task.
	 *
	 * @param id The data migration task ID.
	 * @throws HpcException on database error.
	 */
	public void deleteDataMigrationTask(String id) throws HpcException;

}
