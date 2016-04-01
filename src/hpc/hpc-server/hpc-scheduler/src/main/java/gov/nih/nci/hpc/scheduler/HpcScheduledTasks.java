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
import gov.nih.nci.hpc.bus.HpcUserBusService;
import gov.nih.nci.hpc.dto.user.HpcAuthenticationRequestDTO;
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

	// The authentication request for executing the scheduled tasks.
	@Autowired
	private HpcAuthenticationRequestDTO batchAuthenticationRequest = null;
	
    // The Data Management Business Service instance.
	@Autowired
    private HpcDataManagementBusService dataManagementBusService = null;
	
    // The User Business Service instance.
	@Autowired
    private HpcUserBusService userBusService = null;
	
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
     * Update Data Transfer Status Task
     * 
     */
    @Scheduled(fixedDelay = 30000)
    private void updateDataTransferStatusTask()
    {
        logger.info("Starting Update Data Transfer Status Task...");

        try { 
		     userBusService.authenticate(batchAuthenticationRequest, false);
		     dataManagementBusService.updateDataTransferStatus();
		     
        } catch(HpcException e) {
        	    logger.error("Update Data Transfer Status task failed", e);
        	    
        } finally {
        	       dataManagementBusService.closeConnection();
        	       logger.info("Completed Update Data Transfer Status Task...");	
        }
    }
}

 