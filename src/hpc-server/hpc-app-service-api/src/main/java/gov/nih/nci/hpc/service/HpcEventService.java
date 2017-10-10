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

import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.Calendar;
import java.util.List;

/**
 * <p>
 * HPC Event Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcEventService 
{         
    /**
     * Get all (active) events.
     *
     * @return A list of events.
     * @throws HpcException on service failure.
     */
    public List<HpcEvent> getEvents() throws HpcException;
    
    /**
     * Archive an event. Move it from the active table to the history table.
     *
     * @param event The event to archive
     */
    public void archiveEvent(HpcEvent event);
    
    /**
     * Get an archived event. 
     *
     * @param eventId The archived event ID to get.
     * @return The archived event.
     * @throws HpcException on service failure.
     */
    public HpcEvent getArchivedEvent(int eventId) throws HpcException;
    
    /**
     * Add a data transfer download completed event.
     *
     * @param userId The user ID.
     * @param path The data object path.
     * @param downloadTaskType The download task type.
     * @param downloadTaskId The download task ID.
     * @param destinationLocation The data transfer destination location.
     * @param dataTransferCompleted The time the data download completed.
     * @throws HpcException on service failure.
     */
    public void addDataTransferDownloadCompletedEvent(String userId, String path, 
    		                                          HpcDownloadTaskType downloadTaskType,
    		                                          String downloadTaskId,
    		                                          HpcFileLocation destinationLocation, 
    		                                          Calendar dataTransferCompleted) 
    		                                         throws HpcException;
    
    /**
     * Add a data transfer download failed event.
     *
     * @param userId The user ID.
     * @param path The data object path.
     * @param downloadTaskType The download task type.
     * @param downloadTaskId The download task ID.
     * @param destinationLocation The data transfer destination location.
     * @param dataTransferCompleted The time the data download failed.
     * @param errorMessage The download failed error message.
     * @throws HpcException on service failure.
     */
    public void addDataTransferDownloadFailedEvent(String userId, String path, 
    		                                       HpcDownloadTaskType downloadTaskType,
                                                   String downloadTaskId,
                                                   HpcFileLocation destinationLocation, 
                                                   Calendar dataTransferCompleted, String errorMessage) 
    		                                      throws HpcException;
    
    /**
     * Add a data transfer upload in temporary archive event.
     *
     * @param userId The user ID.
     * @param path The data object path.
     * @throws HpcException on service failure.
     */
    public void addDataTransferUploadInTemporaryArchiveEvent(String userId, String path) 
                                                            throws HpcException;
    
    /**
     * Add a data transfer upload archived event.
     *
     * @param userId The user ID.
     * @param path The data object path.
     * @param registrationTaskId (Optional) The data registration task ID.
     * @param sourceLocation The data transfer source location.
     * @param dataTransferCompleted The time the data upload completed.
     * @throws HpcException on service failure.
     */
    public void addDataTransferUploadArchivedEvent(String userId, String path, String registrationTaskId, 
    		                                       HpcFileLocation sourceLocation, Calendar dataTransferCompleted) 
                                                  throws HpcException;
    
    /**
     * Add a data transfer upload failed event.
     *
     * @param userId The user ID.
     * @param path The data object path.
     * @param registrationTaskId (Optional) The data registration task ID.
     * @param sourceLocation The data transfer source location.
     * @param dataTransferCompleted The time the data upload completed.
     * @param errorMessage the upload failed error message.
     * @throws HpcException on service failure.
     */
    public void addDataTransferUploadFailedEvent(String userId, String path, String registrationTaskId,
    		                                     HpcFileLocation sourceLocation, 
    		                                     Calendar dataTransferCompleted, String errorMessage) 
                                                throws HpcException;

    /**
     * Generate reports event.
     *
     * @param userIds The list of user ids to generate the events for.
     * @param criteria The report criteria.
     * @throws HpcException on service failure.
     */
    public void generateReportsEvents(List<String> userIds, 
    		                          HpcReportCriteria criteria) 
    		                         throws HpcException;
    
    /**
     * Add a collection updated event.
     *
     * @param path The collection path.
     * @throws HpcException on service failure.
     */
    public void addCollectionUpdatedEvent(String path) throws HpcException;
    
    /**
     * Add a collection registration event.
     *
     * @param path The collection path.
     * @throws HpcException on service failure.
     */
    public void addCollectionRegistrationEvent(String path) throws HpcException;
    
    /**
     * Add a data object registration event.
     *
     * @param collectionPath The collection path to which the data object was registered under.
     * @throws HpcException on service failure.
     */
    public void addDataObjectRegistrationEvent(String collectionPath) throws HpcException;
}

 