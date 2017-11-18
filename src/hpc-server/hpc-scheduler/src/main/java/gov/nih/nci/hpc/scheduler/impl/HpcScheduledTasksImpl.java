/**
 * HpcScheduledTasksImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.scheduler.impl;

import gov.nih.nci.hpc.bus.HpcSystemBusService;
import gov.nih.nci.hpc.exception.HpcException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * <p>
 * HPC Scheduled tasks implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcScheduledTasksImpl 
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The System Business Service instance.
	@Autowired
    private HpcSystemBusService systemBusService = null;
	
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
    
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
     
    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
    private HpcScheduledTasksImpl() 
    {
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Update the data transfer upload status of all data objects that the transfer is 'in progress'.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.processDataTranferUploadReceived.delay}")
    private void processDataTranferUploadReceivedTask()
    {
    	executeTask("processDataTranferUploadReceivedTask()", 
    			    systemBusService::processDataTranferUploadReceived);
    }
    
    /**
     * Update the data transfer upload status of all data objects that the transfer is 'in progress'.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.processDataTranferUploadInProgress.delay}")
    private void processDataTranferUploadInProgressTask()
    {
    	executeTask("processDataTranferUploadInProgressTask()", 
    			    systemBusService::processDataTranferUploadInProgress);
    }
    
    /**
     * Update the data transfer upload status of all data objects that users are responsible
     * to upload with a generated upload URL.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.processDataTranferUploadInProgressWithGeneratedURL.delay}")
    private void processDataTranferUploadInProgressWithGeneratedURL()
    {
    	executeTask("processDataTranferUploadInProgressWithGeneratedURL()", 
    			    systemBusService::processDataTranferUploadInProgressWithGeneratedURL);
    }

    /**
     * Process data objects in temporary archive task. This tasks transfers data from the temporary
     * archive to the (permanent) archive and complete data object registration.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.processTemporaryArchive.delay}")
    private void processTemporaryArchiveTask()
    {
    	executeTask("processTemporaryArchiveTask()", 
    			    systemBusService::processTemporaryArchive);
    }
    
    /**
     * Complete (and cleanup) Data Transfer Download Files Tasks.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.completeDataObjectDownloadTasks.delay}")
    private void completeDataObjectDownloadTasksTask()
    {
    	executeTask("completeDataObjectDownloadTasks()", 
    			    systemBusService::completeDataObjectDownloadTasks);
    }
    
    /**
     * Process collection download tasks.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.processCollectionDownloadTasks.delay}")
    private void processCollectionDownloadTasksTask()
    {
    	executeTask("processCollectionDownloadTasks()", 
    			    systemBusService::processCollectionDownloadTasks);
    }
    
    /**
     * Complete collection download tasks.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.completeCollectionDownloadTasks.delay}")
    private void completeCollectionDownloadTasksTask()
    {
    	executeTask("completeCollectionDownloadTasks()", 
    			    systemBusService::completeCollectionDownloadTasks);
    }
    
    /**
     * Process data object list registration tasks.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.processDataObjectListRegistrationTasks.delay}")
    private void processDataObjectListRegistrationTasksTask()
    {
    	executeTask("processDataObjectListRegistrationTasks()", 
    			    systemBusService::processDataObjectListRegistrationTasks);
    }
    
    /**
     * Complete data object list registration tasks.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.completeDataObjectListRegistrationTasks.delay}")
    private void completeDataObjectListRegistrationTasksTask()
    {
    	executeTask("completeDataObjectListRegistrationTasks()", 
    			    systemBusService::completeDataObjectListRegistrationTasks);
    }
    
    /**
     * Process Events Task.
     * 
     */  
    @Scheduled(cron = "${hpc.scheduler.cron.summaryreport.delay}")
    private void generateSummaryReport()
    {
    	executeTask("generateSummaryReport()", 
    			    systemBusService::generateSummaryReportEvent);
    }
    
    /**
     * Process Events Task.
     * 
     */  
    @Scheduled(cron = "${hpc.scheduler.cron.weeklysummaryreport.delay}")
    private void generateWeeklySummaryReport()
    {
    	executeTask("generateWeeklySummaryReport()", 
    			    systemBusService::generateWeeklySummaryReportEvent);
    }

     /**
     * Process Events Task.
     * 
     */  
    @Scheduled(cron = "${hpc.scheduler.cron.processevents.delay}")
    private void processEvents()
    {
    	executeTask("processEvents()", systemBusService::processEvents);
    }
    
    /**
     * Refresh Materialized Views.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.refreshMaterializedViews.delay}")
    private void refreshMetadataViewsTask()
    {
    	executeTask("refreshMetadataViewsTask()", systemBusService::refreshMetadataViews);
    }

    /**
     * Refresh Report Views.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.refreshReportViews.delay}")
    private void refreshReportViewsTask()
    {
    	executeTask("refreshReportViewsTask()", systemBusService::refreshReportViews);
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Execute a scheduled task.
     * 
     * @param name The task name.
     * @param task The task to execute.
     */    
    private void executeTask(String name, HpcScheduledTask task)
    {
    	long start = System.currentTimeMillis();
        logger.info("Scheduled task started: " + name);

        try { 
		     task.execute();
		     
        } catch(HpcException e) {
        	    logger.error("Scheduled task failed: " + name, e);
        	    
        } finally {
        	       systemBusService.closeConnection();
        	       long executionTime = System.currentTimeMillis() - start;
			       logger.info("Scheduled task completed: " + name + " - Task execution time: " + 
        	                   executionTime + " milliseconds.");
        }
    }
}

 