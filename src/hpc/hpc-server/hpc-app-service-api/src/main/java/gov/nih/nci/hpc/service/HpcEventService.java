/**
 * HpcEventService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Event Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcEventService 
{         
    /**
     * Get all (active) events.
     *
     * @return List<HpcEvent>
     * 
     * @throws HpcException
     */
    public List<HpcEvent> getEvents() throws HpcException;
    
    /**
     * Archive an event. Move it from the active table to the history table.
     *
     * @param event The event to archive
     */
    public void archiveEvent(HpcEvent event);
    
    /**
     * Add a data transfer download completed event.
     *
     * @param userId The user ID.
     * @param dataTransferRequestId The data transfer request ID.
     * 
     * @throws HpcException
     */
    public void addDataTransferDownloadCompletedEvent(String userId, String dataTransferRequestId) 
    		                                         throws HpcException;
    
    /**
     * Add a data transfer download failed event.
     *
     * @param userId The user ID.
     * @param dataTransferRequestId The data transfer request ID.
     * 
     * @throws HpcException
     */
    public void addDataTransferDownloadFailedEvent(String userId, String dataTransferRequestId) 
    		                                      throws HpcException;
    
    /**
     * Add a data transfer upload in temporary archive event.
     *
     * @param userId The user ID.
     * @param path The data object path.
     * 
     * @throws HpcException
     */
    public void addDataTransferUploadInTemporaryArchiveEvent(String userId, String path) 
                                                            throws HpcException;
    
    /**
     * Add a data transfer upload archived event.
     *
     * @param userId The user ID.
     * @param path The data object path.
     * 
     * @throws HpcException
     */
    public void addDataTransferUploadArchivedEvent(String userId, String path) 
                                                  throws HpcException;
    
    /**
     * Add a data transfer upload failed event.
     *
     * @param userId The user ID.
     * @param path The data object path.
     * 
     * @throws HpcException
     */
    public void addDataTransferUploadFailedEvent(String userId, String path) 
                                                throws HpcException;

    /**
     * Generate reports event.
     *
     * @throws HpcException
     */
    public void generateReportsEvents(List<String> userIds, HpcReportCriteria criteria) throws HpcException;
}

 