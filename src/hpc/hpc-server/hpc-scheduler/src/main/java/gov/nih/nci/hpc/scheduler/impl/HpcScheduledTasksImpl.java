/**
 * HpcScheduledTasks.java
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
 * HPC Scheduled tasks.
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
    public void updateDataTransferUploadStatusTask()
    {
        logger.info("Starting Update Data Transfer Upload Status Task...");

        try { 
		     systemBusService.updateDataTransferUploadStatus();
		     
        } catch(HpcException e) {
        	    logger.error("Update Data Transfer Upload Status task failed", e);
        	    
        } finally {
        	       systemBusService.closeConnection();
        	       logger.info("Completed Update Data Transfer Upload Status Task...");	
        }
    }
    
    /**
     * Process data objects in temporary archive task. This tasks transfers data from the temporary
     * archive to the (permanent) archive and complete data object registration.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.processTemporaryArchive.delay}")
    public void processTemporaryArchiveTask()
    {
        logger.info("Starting Process Temporary Archive Task...");

        try { 
		     systemBusService.processTemporaryArchive();
		     
        } catch(HpcException e) {
        	    logger.error("Process Temporary Archive task failed", e);
        	    
        } finally {
        	       systemBusService.closeConnection();
        	       logger.info("Completed Process Temporary Archive Task...");	
        }
    }
    
    /**
     * Cleanup Data Transfer Download Files Task.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.cleanupDataTransferDownloadFiles.delay}")
    public void cleanupDataTransferDownloadFilesTask()
    {
        logger.info("Starting Cleanup Data Transfer Download Task...");

        try { 
		     systemBusService.cleanupDataTransferDownloadFiles();
		     
        } catch(HpcException e) {
        	    logger.error("Cleanup Data Transfer Download task failed", e);
        	    
        } finally {
        	       // TODO - make this AOP.
        	       systemBusService.closeConnection();
        	       logger.info("Completed Cleanup Data Transfer Download Task...");	
        }
    }
    
    /**
     * Process Events Task.
     * 
     */  
    @Scheduled(cron = "${hpc.scheduler.cron.summaryreport.delay}")
    public void generateSummaryReport()
    {
        logger.info("Generating reports...");

        try { 
		     systemBusService.generateSummaryReportEvent();
		     
        } catch(HpcException e) {
        	    logger.error("Process Events task failed", e);
        	    
        } finally {
        	       // TODO - make this AOP.
        	       systemBusService.closeConnection();
        	       logger.info("Completed Process Events Task...");	
        }
    }
    
    /**
     * Process Events Task.
     * 
     */  
    @Scheduled(cron = "${hpc.scheduler.cron.weeklysummaryreport.delay}")
    public void generateWeeklySummaryReport()
    {
        logger.info("Generating weekly summary reports...");

        try { 
		     systemBusService.generateWeeklySummaryReportEvent();
		     
        } catch(HpcException e) {
        	    logger.error("Process Events task failed", e);
        	    
        } finally {
        	       // TODO - make this AOP.
        	       systemBusService.closeConnection();
        	       logger.info("Completed Process Events Task...");	
        }
    }

     /**
     * Process Events Task.
     * 
     */  
    @Scheduled(cron = "${hpc.scheduler.cron.processevents.delay}")
    public void processEvents()
    {
        logger.info("Starting Process Events Task...");

        try { 
		     systemBusService.processEvents();
		     
        } catch(HpcException e) {
        	    logger.error("Process Events task failed", e);
        	    
        } finally {
        	       // TODO - make this AOP.
        	       systemBusService.closeConnection();
        	       logger.info("Completed Process Events Task...");	
        }
    }
    
    /**
     * Refresh Materialized Views.
     * 
     */    
    @Scheduled(cron = "${hpc.scheduler.cron.refreshMaterializedViews.delay}")
    public void refreshMetadataViewsTask()
    {
        logger.info("Starting Refreshing Metadata Materialized Views Task...");

        try { 
		     systemBusService.refreshMetadataViews();
		     
        } catch(HpcException e) {
        	    logger.error("Update Data Transfer Upload Status task failed", e);
        	    
        } finally {
        	       systemBusService.closeConnection();
        	       logger.info("Completed Refreshing Materialized Views Task...");	
        }
    }
}

 