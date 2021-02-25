/**
 * HpcDataMigration.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationResult;
import gov.nih.nci.hpc.domain.model.HpcDataMigrationTask;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.service.HpcDataMigrationService;

/**
 * A data transfer listener for data object migration.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataMigrationProgressListener implements HpcDataTransferProgressListener {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The data migration task.
	private HpcDataMigrationTask dataObjectMigrationTask = null;

	// The data migration service.
	HpcDataMigrationService dataMigrationService = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructs a data migration listener (to keep track of data migration from
	 * one S3 archive to another)
	 *
	 * @param dataObjectMigrationTask The migration task.
	 * @param dataMigrationService    The data migration service to callback when
	 *                                migration completed/failed
	 */
	public HpcDataMigrationProgressListener(HpcDataMigrationTask dataObjectMigrationTask,
			HpcDataMigrationService dataMigrationService) {
		this.dataObjectMigrationTask = dataObjectMigrationTask;
		this.dataMigrationService = dataMigrationService;
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataTransferProgressListener Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void transferCompleted(Long bytesTransferred) {
		logger.info("Migration completed for: {} [task-id: {}]", dataObjectMigrationTask.getPath(),
				dataObjectMigrationTask.getId());
		try {
			dataMigrationService.completeDataObjectMigrationTask(dataObjectMigrationTask,
					HpcDataMigrationResult.COMPLETED, null);

		} catch (HpcException e) {
			logger.error("Failed to complete a migration task for {} [task-id: {}]", dataObjectMigrationTask.getPath(),
					dataObjectMigrationTask.getId(), e);
		}
	}

	@Override
	public void transferFailed(String message) {
		logger.error("Migration failed for: {} [task-id: {}]", dataObjectMigrationTask.getPath(),
				dataObjectMigrationTask.getId());
		try {
			dataMigrationService.completeDataObjectMigrationTask(dataObjectMigrationTask,
					HpcDataMigrationResult.FAILED, null);

		} catch (HpcException e) {
			logger.error("Failed to complete a migration task for {} [task-id: {}]", dataObjectMigrationTask.getPath(),
					dataObjectMigrationTask.getId(), e);
		}
	}
}
