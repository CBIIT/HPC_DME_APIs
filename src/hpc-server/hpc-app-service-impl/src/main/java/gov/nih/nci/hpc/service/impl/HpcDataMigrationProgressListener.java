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
	private HpcDataMigrationService dataMigrationService = null;

	// The from/To archive tokens
	private Object fromS3ArchiveAuthToken = null;
	private Object toS3ArchiveAuthToken = null;

	// Indicator to mark transfer failed event received.
	boolean transferFailed = false;

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
	 *                                migration completed/failed.
	 * @param fromS3ArchiveAuthToken  The from S3 archive token
	 * @param toS3ArchiveAuthToken    The to S3 archive token
	 */
	public HpcDataMigrationProgressListener(HpcDataMigrationTask dataObjectMigrationTask,
			HpcDataMigrationService dataMigrationService, Object fromS3ArchiveAuthToken, Object toS3ArchiveAuthToken) {
		this.dataObjectMigrationTask = dataObjectMigrationTask;
		this.dataMigrationService = dataMigrationService;
		this.fromS3ArchiveAuthToken = fromS3ArchiveAuthToken;
		this.toS3ArchiveAuthToken = toS3ArchiveAuthToken;
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
					HpcDataMigrationResult.COMPLETED, null, fromS3ArchiveAuthToken, toS3ArchiveAuthToken);

		} catch (HpcException e) {
			logger.error("Failed to complete a migration task for {} [task-id: {}]", dataObjectMigrationTask.getPath(),
					dataObjectMigrationTask.getId(), e);
		}
	}

	@Override
	public void transferFailed(String message) {
		synchronized (this) {
			if (transferFailed) {
				// Failure event already received and processed.
				return;
			}
			transferFailed = true;
		}

		logger.error("Migration failed for: {} [task-id: {}]", dataObjectMigrationTask.getPath(),
				dataObjectMigrationTask.getId());
		try {
			dataMigrationService.completeDataObjectMigrationTask(dataObjectMigrationTask, HpcDataMigrationResult.FAILED,
					message, fromS3ArchiveAuthToken, toS3ArchiveAuthToken);

		} catch (HpcException e) {
			logger.error("Failed to complete a migration task for {} [task-id: {}]", dataObjectMigrationTask.getPath(),
					dataObjectMigrationTask.getId(), e);
		}
	}
}
