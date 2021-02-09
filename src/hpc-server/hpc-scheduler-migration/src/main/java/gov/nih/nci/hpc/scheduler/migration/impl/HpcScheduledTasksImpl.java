/**
 * HpcScheduledTasksImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.scheduler.migration.impl;

import static gov.nih.nci.hpc.util.HpcScheduledTask.execute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import gov.nih.nci.hpc.bus.HpcDataMigrationBusService;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Scheduled tasks implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcScheduledTasksImpl {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The System Business Service instance.
	@Autowired
	private HpcDataMigrationBusService dataMigrationBusService = null;

	// The Logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcScheduledTasksImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	/** Process received data object migration tasks. */
	@Scheduled(cron = "${hpc.scheduler.migration.cron.processDataObjectMigrationReceived.delay}")
	private void processDataObjectMigrationReceivedTask() {
		execute("processDataObjectMigrationReceivedTask()", dataMigrationBusService::processDataObjectMigrationReceived,
				logger);
	}

	/** Process received collection migration tasks. */
	@Scheduled(cron = "${hpc.scheduler.migration.cron.processCollectionMigrationReceived.delay}")
	private void processCollectionMigrationReceivedTask() {
		execute("processCollectionMigrationReceivedTask()", dataMigrationBusService::processCollectionMigrationReceived,
				logger);
	}

	/**
	 * Called by Spring dependency injection. Reset all active S3 upload/download in
	 * progress tasks, so they are restarted following a server restart.
	 */
	@SuppressWarnings("unused")
	private void init() {
		try {
			// All active data migration tasks needs to be restarted.
			dataMigrationBusService.restartDataObjectMigrationTasks();

		} catch (HpcException e) {
			logger.error("Migration Scheduler failed to initialize", e);
		}
	}
}
