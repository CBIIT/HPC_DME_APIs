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
 * @version $Id: HpcScheduledTasks.java 932 2016-03-01 02:05:17Z rosenbergea $
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
     * Update Data Transfer Status Task.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.updateDataTransferUploadStatus.delay}")
    private void updateDataTransferUploadStatusTask()
    {
    	executeTask("updateDataTransferUploadStatusTask()", 
    			    systemBusService::updateDataTransferUploadStatus);
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
     * Cleanup Data Transfer Download Files Task.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.cleanupDataObjectDownloadTasks.delay}")
    private void cleanupDataObjectDownloadTasksTask()
    {
    	executeTask("cleanupDataObjectDownloadTasks()", 
    			    systemBusService::cleanupDataObjectDownloadTasks);
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

 