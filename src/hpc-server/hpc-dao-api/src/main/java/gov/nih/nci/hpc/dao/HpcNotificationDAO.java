/**
 * HpcNotificationDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryReceipt;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Notification DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcNotificationDAO 
{    
    /**
     * Store a new notification subscription to the repository or update it if it exists.
     *
     * @param userId The user ID.
     * @param notificationSubscription The notification subscription to be added/updated.
     * @throws HpcException on database error.
     */
    public void upsertSubscription(
    		          String userId,
    		          HpcNotificationSubscription notificationSubscription) throws HpcException;
    
    /**
     * Delete a notification subscription.
     *
     * @param userId The user ID.
     * @param eventType The event type.
     * @throws HpcException on database error.
     */
    public void deleteSubscription(String userId, HpcEventType eventType) 
    		                      throws HpcException;
    
    /**
     * Get notification subscriptions of a user.
     *
     * @param userId The user ID.
     * @return <code>List&lt;HpcNotificationSubscription&gt;</code>
     * @throws HpcException on database error.
     */
    public List<HpcNotificationSubscription> 
           getSubscriptions(String userId) throws HpcException;

    /**
     * Get users subscribed to an event.
     *
     * @param eventType The event type.
     * @return <code>List&lt;String&gt;</code> list of userIds
     * @throws HpcException on database error.
     */
    public List<String> 
           getSubscribedUsers(HpcEventType eventType) throws HpcException;
    
    /**
     * Get users subscribed to an event w/ a notification trigger.
     *
     * @param eventType The event type.
     * @param eventPayloadEntries The event payload entries. Note: all user's subscribed triggers must be met for the 
     *                            user to be included in  the returned list. 
     * @return <code>List&lt;String&gt;</code> list of userIds
     * @throws HpcException on database error.
     */
    public List<String> 
           getSubscribedUsers(HpcEventType eventType, List<HpcEventPayloadEntry> eventPayloadEntries) 
        		             throws HpcException;
    
    /**
     * Get notification subscription
     *
     * @param userId The user ID.
     * @param eventType The event type.
     * @return HpcNotificationSubscription
     * @throws HpcException on database error.
     */
    public HpcNotificationSubscription getSubscription(String userId, 
    		                                           HpcEventType eventType) 
    		                                          throws HpcException;
    
    /**
     * Store a new notification delivery receipt to the repository or update it if it exists.
     *
     * @param deliveryReceipt The notification delivery receipt to be added/updated.
     * @throws HpcException on database error.
     */
    public void upsertDeliveryReceipt(HpcNotificationDeliveryReceipt deliveryReceipt) 
    		                         throws HpcException;
    
    /**
     * Get Notification Delivery Receipts for a user.
     *
     * @param userId The user ID to get the notification delivery receipts for. 
     * @param offset Skip that many receipts in the returned results.
     * @param limit No more than 'limit' receipts will be returned.
     * @return A list of notification delivery receipts.
     * @throws HpcException on database failure.
     */
    public List<HpcNotificationDeliveryReceipt> 
           getDeliveryReceipts(String userId, int offset, int limit) throws HpcException;
    
    /**
     * Get Notification Delivery Receipt for a user by eventId.
     *
     * @param userId The user ID to get the notification delivery receipts for. 
     * @param eventId The delivery receipt eventId.
     * @return delivery receipt.
     * @throws HpcException on database failure.
     */
    public HpcNotificationDeliveryReceipt 
           getDeliveryReceipt(String userId, int eventId) throws HpcException;

    /**
     * Get Notification Delivery Receipts count for a user.
     *
     * @param userId The user ID to get the notification delivery receipts for. 
     * @return The count of notification delivery receipts.
     * @throws HpcException on database failure.
     */
    public int getDeliveryReceiptsCount(String userId) throws HpcException;
}

 