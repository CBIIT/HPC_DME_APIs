/**
 * HpcScheduledTasksImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.scheduler.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import gov.nih.nci.hpc.bus.HpcSystemBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
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
	private HpcSystemBusService systemBusService = null;

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

	/** Process data objects that the data transfer upload status is 'received'. */
	@Scheduled(cron = "${hpc.scheduler.cron.processDataTranferUploadReceived.delay}")
	private void processDataTranferUploadReceivedTask() {
		executeTask("processDataTranferUploadReceivedTask()", systemBusService::processDataTranferUploadReceived);
	}

	/**
	 * Update the data transfer upload status of all data objects that the transfer
	 * is 'in progress'.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.processDataTranferUploadInProgress.delay}")
	private void processDataTranferUploadInProgressTask() {
		executeTask("processDataTranferUploadInProgressTask()", systemBusService::processDataTranferUploadInProgress);
	}

	/**
	 * Update the data transfer upload status of all data objects that users are
	 * responsible to upload with a generated upload URL.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.processDataTranferUploadInProgressWithGeneratedURL.delay}")
	private void processDataTranferUploadInProgressWithGeneratedURLTask() {
		executeTask("processDataTranferUploadInProgressWithGeneratedURLTask()",
				systemBusService::processDataTranferUploadInProgressWithGeneratedURL);
	}

	/**
	 * Update the data transfer upload status of all data objects that are streamed
	 * from AWS S3.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.processDataTranferUploadStreamingInProgress.delay}")
	private void processDataTranferUploadStreamingInProgress() {
		executeTask("processDataTranferUploadStreamingInProgress()",
				systemBusService::processDataTranferUploadStreamingInProgress);
	}

	/**
	 * Update the data transfer upload status of all data objects that are streamed
	 * from AWS S3.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.processDataTranferUploadStreamingStopped.delay}")
	private void processDataTranferUploadStreamingStopped() {
		executeTask("processDataTranferUploadStreamingStopped()",
				systemBusService::processDataTranferUploadStreamingStopped);
	}

	/**
	 * Process data objects in temporary archive task. This tasks uploads data objects
	 * from the temporary archive to the (permanent) archive and complete data
	 * object registration.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.processTemporaryArchive.delay}")
	private void processTemporaryArchiveTask() {
		executeTask("processTemporaryArchiveTask()", systemBusService::processTemporaryArchive);
	}

	/**
	 * Process data objects in file system task. This tasks uploads data objects
	 * from the local DME file system (NAS) to the archive and complete data
	 * object registration.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.processFileSystemUpload.delay}")
	private void processFileSystemUploadTask() {
		executeTask("processFileSystemUploadTask()", systemBusService::processFileSystemUpload);
	}
	
	/**
	 * Start Data Object Download Tasks that are in RECEIVED state for Globus
	 * transfer.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.startGlobusDataObjectDownloadTasks.delay}")
	private void startGlobusDataObjectDownloadTasks() {
		executeTask("startGlobusDataObjectDownloadTasks()", systemBusService::startGlobusDataObjectDownloadTasks);
	}

	/**
	 * Start Data Object Download Tasks that are in RECEIVED state for S3 transfer.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.startS3DataObjectDownloadTasks.delay}")
	private void startS3DataObjectDownloadTasks() {
		executeTask("startS3DataObjectDownloadTasks()", systemBusService::startS3DataObjectDownloadTasks);
	}

	/**
	 * Start Data Object Download Tasks that are in RECEIVED state for GOOGLE_DRIVE
	 * transfer.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.startGoogleDriveDataObjectDownloadTasks.delay}")
	private void startGoogleDriveDataObjectDownloadTasks() {
		executeTask("startGoogleDriveDataObjectDownloadTasks()",
				systemBusService::startGoogleDriveDataObjectDownloadTasks);
	}

	/** Complete In-Progress Data Object Download Tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.completeInProgressDataObjectDownloadTasks.delay}")
	private void completeInProgressDataObjectDownloadTasksTask() {
		executeTask("completeDataObjectDownloadTasksTask()",
				systemBusService::completeInProgressDataObjectDownloadTasks);
	}

	/** Complete Canceled Data Object Download Tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.completeCanceledDataObjectDownloadTasks.delay}")
	private void completeCanceledDataObjectDownloadTasksTask() {
		executeTask("completeDataObjectDownloadTasksTask()", systemBusService::completeCanceledDataObjectDownloadTasks);
	}

	/** Process collection download tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.processCollectionDownloadTasks.delay}")
	private void processCollectionDownloadTasksTask() {
		executeTask("processCollectionDownloadTasksTask()", systemBusService::processCollectionDownloadTasks);
	}

	/** Complete collection download tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.completeCollectionDownloadTasks.delay}")
	private void completeCollectionDownloadTasksTask() {
		executeTask("completeCollectionDownloadTasksTask()", systemBusService::completeCollectionDownloadTasks);
	}

	/** Process bulk data object registration tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.processBulkDataObjectRegistrationTasks.delay}")
	private void processDataObjectListRegistrationTasksTask() {
		executeTask("processDataObjectListRegistrationTasksTask()",
				systemBusService::processBulkDataObjectRegistrationTasks);
	}

	/** Complete bulk data object registration tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.completeBulkDataObjectRegistrationTasks.delay}")
	private void completeBulkDataObjectRegistrationTasksTask() {
		executeTask("completeBulkDataObjectRegistrationTasksTask()",
				systemBusService::completeBulkDataObjectRegistrationTasks);
	}

	/** Process events and send notifications. */
	@Scheduled(cron = "${hpc.scheduler.cron.processevents.delay}")
	private void processEventsTask() {
		executeTask("processEventsTask()", systemBusService::processEvents);
	}

	/** Generate a summary report event. */
	@Scheduled(cron = "${hpc.scheduler.cron.summaryreport.delay}")
	private void generateSummaryReportTask() {
		executeTask("generateSummaryReportTask()", systemBusService::generateSummaryReportEvent);
	}

	/** Generate a weekly summary report event. */
	@Scheduled(cron = "${hpc.scheduler.cron.weeklysummaryreport.delay}")
	private void generateWeeklySummaryReportTask() {
		executeTask("generateWeeklySummaryReportTask()", systemBusService::generateWeeklySummaryReportEvent);
	}

	/** Refresh the metadata materialized Views. */
	@Scheduled(cron = "${hpc.scheduler.cron.refreshMetadataViews.delay}")
	private void refreshMetadataViewsTask() {
		executeTask("refreshMetadataViewsTask()", systemBusService::refreshMetadataViews);
	}

	/** Refresh the report materizalized views. */
	@Scheduled(cron = "${hpc.scheduler.cron.refreshReportViews.delay}")
	private void refreshReportViewsTask() {
		executeTask("refreshReportViewsTask()", systemBusService::refreshReportViews);
	}
	
	/** Complete tiering request tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.completeDeepArchiveInProgress.delay}")
	private void completeDeepArchiveInProgressTask() {
		executeTask("completeDeepArchiveInProgressTask()",
				systemBusService::completeDeepArchiveInProgress);
	}
	
	/** Complete restore request tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.completeRestoreRequest.delay}")
	private void completeRestoreRequestTask() {
		executeTask("completeRestoreRequestTask()",
				systemBusService::completeRestoreRequest);
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Execute a scheduled task.
	 *
	 * @param name The task name.
	 * @param task The task to execute.
	 */
	private void executeTask(String name, HpcScheduledTask task) {
		long start = System.currentTimeMillis();
		logger.info("Scheduled task started: {}", name);

		try {
			task.execute();

		} catch (HpcException e) {
			logger.error("Scheduled task failed: " + name, e);

		} finally {
			long executionTime = System.currentTimeMillis() - start;
			logger.info("Scheduled task completed: {} - Task execution time: {} milliseconds", name, executionTime);
		}
	}

	/**
	 * Called by Spring dependency injection. Reset all active S3 upload/download in
	 * progress tasks, so they are restarted following a server restart.
	 */
	@SuppressWarnings("unused")
	private void init() {
		try {
			// All active S3 upload tasks should be marked stopped (so they get restarted)
			systemBusService.processDataTranferUploadStreamingInProgress(true);

			// All active S3 download tasks needs to be restarted.
			systemBusService.restartDataObjectDownloadTasks();

			// All in-process collection download tasks needs to be restarted.
			systemBusService.restartCollectionDownloadTasks();

		} catch (HpcException e) {
			logger.error("Failed to update upload status for current S3 streaming tasks", HpcErrorType.UNEXPECTED_ERROR,
					e);
		}
	}
}
