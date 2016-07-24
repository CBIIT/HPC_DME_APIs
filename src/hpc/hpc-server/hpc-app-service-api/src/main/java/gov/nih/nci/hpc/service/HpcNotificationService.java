/**
 * HpcSecurityService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Notification Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcNotificationService 
{         
	/**
     * Add/Update a notification subscription for a user.
     *
     * @param userId The user ID.
     * @param notificationSubscription The notification subscription to add/update.
     * 
     * @throws HpcException
     */
    public void addUpdateNotificationSubscription(String userId,
    		                                      HpcNotificationSubscription notificationSubscription)
                                                 throws HpcException;
    
	/**
     * Delete a notification subscription for a user.
     *
     * @param userId The user ID.
     * @param eventType The event type to delete.
     * 
     * @throws HpcException
     */
    public void deleteNotificationSubscription(String userId,
    		                                   HpcEventType eventType)
                                              throws HpcException;
    
    /**
     * Get notification subscriptions of a user.
     *
     * @param userId The user ID.
     * @return List<HpcNotificationSubscription>
     * 
     * @throws HpcException
     */
    public List<HpcNotificationSubscription> getNotificationSubscriptions(String userId) 
    		                                                             throws HpcException;
    
    /**
     * Get notification subscription.
     *
     * @param userId The user ID.
     * @param eventType The event type.
     * @return HpcNotificationSubscription or null if not found.
     * 
     * @throws HpcException
     */
    public HpcNotificationSubscription getNotificationSubscription(String userId,
    		                                                       HpcEventType eventType) 
    		                                                      throws HpcException;
    
    /**
     * Get all (active) events.
     *
     * @return List<HpcEvent>
     * 
     * @throws HpcException
     */
    public List<HpcEvent> getEvents() throws HpcException;
    
    /**
     * Archive an event - move it from active table to history table. 
     *
     * @param eventId The event ID.
     * 
     * @throws HpcException
     */
    //public void archiveEvent(int EventId) throws HpcException;
    
    /**
     * Send an event notification.
     *
     * @param event The event.
     * @param deliveryMethod The delivery method.
     * 
     * @throws HpcException
     */
    public void sendNotification(HpcEvent event, 
    		                     HpcNotificationDeliveryMethod deliveryMethod) 
    		                    throws HpcException;
    
    /**
     * Create delivery receipts for this event and delete it.
     *
     * @param event The notification event.
     * 
     * @throws HpcException
     */
    public void createDeliveryReceipts(HpcEvent event) throws HpcException;
    
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
    

}

 