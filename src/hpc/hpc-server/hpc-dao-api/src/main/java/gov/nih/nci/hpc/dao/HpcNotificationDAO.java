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

import gov.nih.nci.hpc.domain.notification.HpcNotificationEvent;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.notification.HpcNotificationType;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Notification DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcNotificationDAO 
{    
    /**
     * Store a new notification subscription to the repository or update it if it exists.
     *
     * @param userId The user ID.
     * @param notificationSubscription The notification subscription to be added/updated.
     * 
     * @throws HpcException
     */
    public void upsertSubscription(
    		          String userId,
    		          HpcNotificationSubscription notificationSubscription) throws HpcException;
    
    /**
     * Delete a notification subscription.
     *
     * @param userId The user ID.
     * @param notificationType The notification type.
     * 
     * @throws HpcException
     */
    public void deleteSubscription(String userId, HpcNotificationType notificationType) 
    		                      throws HpcException;
    
    /**
     * Get notification subscriptions of a user.
     *
     * @param userId The user ID.
     * @return List<HpcNotificationSubscription>
     * 
     * @throws HpcException
     */
    public List<HpcNotificationSubscription> 
           getSubscriptions(String userId) throws HpcException;
    
    /**
     * Get notification subscription
     *
     * @param userId The user ID.
     * @param notificationType The notification type.
     * @return HpcNotificationSubscription
     * 
     * @throws HpcException
     */
    public HpcNotificationSubscription getSubscription(String userId, 
    		                                           HpcNotificationType notificationType) 
    		                                          throws HpcException;
    
    /**
     * Store a new notification event to the repository.
     *
     * @param notificationEvent The notification event to be added/updated.
     * 
     * @throws HpcException
     */
    public void insertEvent(HpcNotificationEvent notificationEvent) throws HpcException;
    
    /**
     * Get all notification events.
     *
     * @return List<HpcNotificationEvent>
     * 
     * @throws HpcException
     */
    public List<HpcNotificationEvent> getEvents() throws HpcException;
    
    /**
     * Delete an event
     *
     * @param eventId The eventId to delete.
     * 
     * @throws HpcException
     */
    public void deleteEvent(String eventId) throws HpcException;
}

 