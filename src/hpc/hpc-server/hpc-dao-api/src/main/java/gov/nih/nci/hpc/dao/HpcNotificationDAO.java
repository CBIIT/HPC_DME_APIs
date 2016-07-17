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

import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
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
     * @param eventType The event type.
     * 
     * @throws HpcException
     */
    public void deleteSubscription(String userId, HpcEventType eventType) 
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
     * @param eventType The event type.
     * @return HpcNotificationSubscription
     * 
     * @throws HpcException
     */
    public HpcNotificationSubscription getSubscription(String userId, 
    		                                           HpcEventType eventType) 
    		                                          throws HpcException;
    
    /**
     * Store a new notification event to the repository.
     *
     * @param event The event to be added/updated.
     * 
     * @throws HpcException
     */
    public void insertEvent(HpcEvent event) throws HpcException;
    
    /**
     * Get all (active) events.
     *
     * @return List<HpcEvent>
     * 
     * @throws HpcException
     */
    public List<HpcEvent> getEvents() throws HpcException;
    
    /**
     * Delete an event
     *
     * @param eventId The eventId to delete.
     * 
     * @throws HpcException
     */
    public void deleteEvent(int eventId) throws HpcException;
}

 