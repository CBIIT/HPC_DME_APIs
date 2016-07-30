/**
 * HpcNotificationService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
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
     * Notify a user of an event.
     *
     * @param userId The user to send the notification to.
     * @param eventType The event type to notify.
     * @param payloadEntries The payload entries to use for the notification message.
     * @param deliveryMethod The delivery method.
     * @return If the notification was delivered successfully, or false otherwise.
     */
    public boolean sendNotification(String userId, HpcEventType eventType, 
                                    List<HpcEventPayloadEntry> payloadEntries,
    		                        HpcNotificationDeliveryMethod deliveryMethod);
    
    /**
     * Create a notification delivery receipt for this event.
     *
     * @param userId The user ID.
     * @param eventId The event ID.
     * @param deliveryMethod The delivery method.
     * @param deliveryStatus The delivery status.
     */
    public void createNotificationDeliveryReceipt(String userId, int eventId, 
    		                                      HpcNotificationDeliveryMethod deliveryMethod,
    		                                      boolean deliveryStatus);
}

 