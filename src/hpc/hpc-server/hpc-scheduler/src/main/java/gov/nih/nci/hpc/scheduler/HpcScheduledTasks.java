/**
 * HpcScheduledTasks.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.scheduler;

import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
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

public class HpcScheduledTasks 
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Business Service instance.
	@Autowired
    private HpcDataManagementBusService dataManagementBusService = null;
	
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
    private HpcScheduledTasks() 
    {
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Update Data Transfer Status Task.
     * 
     */    
    @Scheduled(fixedDelay = 60000)
    private void updateDataTransferUploadStatusTask()
    {
        logger.info("Starting Update Data Transfer Upload Status Task...");

        try { 
		     systemBusService.updateDataTransferUploadStatus();
		     
        } catch(HpcException e) {
        	    logger.error("Update Data Transfer Upload Status task failed", e);
        	    
        } finally {
        	       dataManagementBusService.closeConnection();
        	       logger.info("Completed Update Data Transfer Upload Status Task...");	
        }
    }
    
    /**
     * Process data objects in temporary archive task. This tasks transfers data from the temporary
     * archive to the (permanent) archive and complete data object registration.
     * 
     */    
    @Scheduled(fixedDelay = 50000)
    private void processTemporaryArchiveTask()
    {
        logger.info("Starting Process Temporary Archive Task...");

        try { 
		     systemBusService.processTemporaryArchive();
		     
        } catch(HpcException e) {
        	    logger.error("Process Temporary Archive task failed", e);
        	    
        } finally {
        	       dataManagementBusService.closeConnection();
        	       logger.info("Completed Process Temporary Archive Task...");	
        }
    }
    
    /**
     * Cleanup Data Transfer Download Files Task.
     * 
     */    
    @Scheduled(fixedDelay = 90000)
    private void cleanupDataTransferDownloadFilesTask()
    {
        logger.info("Starting Cleanup Data Transfer Download Task...");

        try { 
		     systemBusService.cleanupDataTransferDownloadFiles();
		     
        } catch(HpcException e) {
        	    logger.error("Cleanup Data Transfer Download task failed", e);
        	    
        } finally {
        	       // TODO - make this AOP.
        	       dataManagementBusService.closeConnection();
        	       logger.info("Completed Cleanup Data Transfer Download Task...");	
        }
    }
    
    /**
     * Process Events Task.
     * 
     */    
    /*
    @Scheduled(fixedDelay = 30000)
    private void processEvents()
    {
        logger.info("Starting Process Events Task...");

        try { 
		     systemBusService.processEvents();
		     
        } catch(HpcException e) {
        	    logger.error("Process Events task failed", e);
        	    
        } finally {
        	       // TODO - make this AOP.
        	       dataManagementBusService.closeConnection();
        	       logger.info("Completed Process Events Task...");	
        }
    }*/
}

 