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

import gov.nih.nci.hpc.bus.HpcReviewBusService;
import gov.nih.nci.hpc.bus.HpcSystemBusService;
import gov.nih.nci.hpc.exception.HpcException;
import static gov.nih.nci.hpc.util.HpcScheduledTask.execute;

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

	// The Review Business Service instance.
	@Autowired
	private HpcReviewBusService reviewBusService = null;
		
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
		execute("processDataTranferUploadReceivedTask()", systemBusService::processDataTranferUploadReceived, logger);
	}

	/**
	 * Update the data transfer upload status of all data objects that the transfer
	 * is 'in progress'.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.processDataTranferUploadInProgress.delay}")
	private void processDataTranferUploadInProgressTask() {
		execute("processDataTranferUploadInProgressTask()", systemBusService::processDataTranferUploadInProgress,
				logger);
	}

	/**
	 * Update the data transfer upload status of all data objects that users are
	 * responsible to upload with a generated upload URL.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.processDataTranferUploadInProgressWithGeneratedURL.delay}")
	private void processDataTranferUploadInProgressWithGeneratedURLTask() {
		execute("processDataTranferUploadInProgressWithGeneratedURLTask()",
				systemBusService::processDataTranferUploadInProgressWithGeneratedURL, logger);
	}

	/**
	 * Update the data transfer upload status of all data objects that are streamed
	 * from AWS S3.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.processDataTranferUploadStreamingInProgress.delay}")
	private void processDataTranferUploadStreamingInProgress() {
		execute("processDataTranferUploadStreamingInProgress()",
				systemBusService::processDataTranferUploadStreamingInProgress, logger);
	}

	/**
	 * Update the data transfer upload status of all data objects that are streamed
	 * from AWS S3.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.processDataTranferUploadStreamingStopped.delay}")
	private void processDataTranferUploadStreamingStopped() {
		execute("processDataTranferUploadStreamingStopped()",
				systemBusService::processDataTranferUploadStreamingStopped, logger);
	}

	/**
	 * Process data objects in temporary archive task. This tasks uploads data
	 * objects from the temporary archive to the (permanent) archive and complete
	 * data object registration.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.processTemporaryArchive.delay}")
	private void processTemporaryArchiveTask() {
		execute("processTemporaryArchiveTask()", systemBusService::processTemporaryArchive, logger);
	}

	/**
	 * Process data objects in file system task. This tasks uploads data objects
	 * from the local DME file system (NAS) to the archive and complete data object
	 * registration.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.processDataTranferUploadFileSystemReady.delay}")
	private void processDataTranferUploadFileSystemReadyTask() {
		execute("processFileSystemUploadTask()", systemBusService::processDataTranferUploadFileSystemReady, logger);
	}

	/**
	 * Start Data Object Download Tasks that are in RECEIVED state for Globus
	 * transfer.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.startGlobusDataObjectDownloadTasks.delay}")
	private void startGlobusDataObjectDownloadTasks() {
		execute("startGlobusDataObjectDownloadTasks()", systemBusService::startGlobusDataObjectDownloadTasks, logger);
	}

	/**
	 * Start Data Object Download Tasks that are in RECEIVED state for S3 transfer.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.startS3DataObjectDownloadTasks.delay}")
	private void startS3DataObjectDownloadTasks() {
		execute("startS3DataObjectDownloadTasks()", systemBusService::startS3DataObjectDownloadTasks, logger);
	}

	/**
	 * Start Data Object Download Tasks that are in RECEIVED state for GOOGLE_DRIVE
	 * transfer.
	 */
	@Scheduled(cron = "${hpc.scheduler.cron.startGoogleDriveDataObjectDownloadTasks.delay}")
	private void startGoogleDriveDataObjectDownloadTasks() {
		execute("startGoogleDriveDataObjectDownloadTasks()", systemBusService::startGoogleDriveDataObjectDownloadTasks,
				logger);
	}

	/** Complete In-Progress Data Object Download Tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.completeInProgressDataObjectDownloadTasks.delay}")
	private void completeInProgressDataObjectDownloadTasksTask() {
		execute("completeDataObjectDownloadTasksTask()", systemBusService::completeInProgressDataObjectDownloadTasks,
				logger);
	}

	/** Complete Canceled Data Object Download Tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.completeCanceledDataObjectDownloadTasks.delay}")
	private void completeCanceledDataObjectDownloadTasksTask() {
		execute("completeDataObjectDownloadTasksTask()", systemBusService::completeCanceledDataObjectDownloadTasks,
				logger);
	}

	/** Process collection download tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.processCollectionDownloadTasks.delay}")
	private void processCollectionDownloadTasksTask() {
		execute("processCollectionDownloadTasksTask()", systemBusService::processCollectionDownloadTasks, logger);
	}

	/** Complete collection download tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.completeCollectionDownloadTasks.delay}")
	private void completeCollectionDownloadTasksTask() {
		execute("completeCollectionDownloadTasksTask()", systemBusService::completeCollectionDownloadTasks, logger);
	}

	/** Process bulk data object registration tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.processBulkDataObjectRegistrationTasks.delay}")
	private void processDataObjectListRegistrationTasksTask() {
		execute("processDataObjectListRegistrationTasksTask()",
				systemBusService::processBulkDataObjectRegistrationTasks, logger);
	}

	/** Complete bulk data object registration tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.completeBulkDataObjectRegistrationTasks.delay}")
	private void completeBulkDataObjectRegistrationTasksTask() {
		execute("completeBulkDataObjectRegistrationTasksTask()",
				systemBusService::completeBulkDataObjectRegistrationTasks, logger);
	}

	/** Process events and send notifications. */
	@Scheduled(cron = "${hpc.scheduler.cron.processevents.delay}")
	private void processEventsTask() {
		execute("processEventsTask()", systemBusService::processEvents, logger);
	}

	/** Generate a summary report event. */
	@Scheduled(cron = "${hpc.scheduler.cron.summaryreport.delay}")
	private void generateSummaryReportTask() {
		execute("generateSummaryReportTask()", systemBusService::generateSummaryReportEvent, logger);
	}

	/** Generate a weekly summary report event. */
	@Scheduled(cron = "${hpc.scheduler.cron.weeklysummaryreport.delay}")
	private void generateWeeklySummaryReportTask() {
		execute("generateWeeklySummaryReportTask()", systemBusService::generateWeeklySummaryReportEvent, logger);
	}

	/** Refresh the metadata materialized Views. */
	@Scheduled(cron = "${hpc.scheduler.cron.refreshMetadataViews.delay}")
	private void refreshMetadataViewsTask() {
		execute("refreshMetadataViewsTask()", systemBusService::refreshMetadataViews, logger);
	}

	/** Refresh the report materizalized views. */
	@Scheduled(cron = "${hpc.scheduler.cron.refreshReportViews.delay}")
	private void refreshReportViewsTask() {
		execute("refreshReportViewsTask()", systemBusService::refreshReportViews, logger);
	}
	
	/** Complete tiering request tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.completeDeepArchiveInProgress.delay}")
	private void completeDeepArchiveInProgressTask() {
		execute("completeDeepArchiveInProgressTask()",
				systemBusService::completeDeepArchiveInProgress, logger);
	}
	
	/** Complete restore request tasks. */
	@Scheduled(cron = "${hpc.scheduler.cron.completeRestoreRequest.delay}")
	private void completeRestoreRequestTask() {
		execute("completeRestoreRequestTask()",
				systemBusService::completeRestoreRequest, logger);
	}

	/** Send Annual review emails. */
	@Scheduled(cron = "${hpc.scheduler.cron.sendAnnualReview.delay}")
	private void sendAnnualReviewTask() {
		execute("sendAnnualReviewTask()", reviewBusService::sendAnnualReview, logger);
	}
	
	/** Send Annual review emails. */
	@Scheduled(cron = "${hpc.scheduler.cron.sendAnnualReviewReminder.delay}")
	private void sendAnnualReviewReminderTask() {
		execute("sendAnnualReviewReminderTask()", reviewBusService::sendAnnualReviewReminder, logger);
	}
	
	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Called by Spring dependency injection. Reset all active S3 upload/download in
	 * progress tasks, so they are restarted following a server restart.
	 */
	@SuppressWarnings("unused")
	private void init() {
		try {
			// All active S3 upload tasks should be marked stopped (so they get restarted)
			systemBusService.processDataTranferUploadStreamingInProgress(true);
			
			// All active file system uploads should be restarted.
			systemBusService.processDataTransferUploadFileSystemInProgress();

			// All active S3 download tasks needs to be restarted.
			systemBusService.restartDataObjectDownloadTasks();

			// All in-process collection download tasks needs to be restarted.
			systemBusService.restartCollectionDownloadTasks();

		} catch (HpcException e) {
			logger.error("Scheduler failed to initialize", e);
		}
	}
}
