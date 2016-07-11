/**
 * HpcNotificationBusService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionsRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Notification Business Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcNotificationBusService 
{         
    /**
     * Subscribe to notifications.
     *
     * @param userId The user ID to subscribe notifications for.
     * @param notificationSubscriptions The notification subscriptions request.
     * 
     * @throws HpcException
     */
    public void subscribeNotifications(String userId,
    		                           HpcNotificationSubscriptionsRequestDTO notificationSubscriptions)
                                      throws HpcException;
    
    /**
     * Get notification subscriptions of a user.
     *
     * @param userId The user ID.
     * @return HpcNotificationSubscriptionListDTO A list of notification subscriptions.
     * 
     * @throws HpcException
     */
    public HpcNotificationSubscriptionListDTO getNotificationSubscriptions(String userId) 
    		                                                             throws HpcException;
}

 