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
     * @param notificationSubscriptions The notification subscriptions request.
     * @throws HpcException on service failure.
     */
    public void subscribeNotifications(HpcNotificationSubscriptionsRequestDTO notificationSubscriptions)
                                      throws HpcException;
    
    /**
     * Get notification subscriptions of a user.
     *
     * @return A list of notification subscriptions.
     * @throws HpcException on service failure.
     */
    public HpcNotificationSubscriptionListDTO getNotificationSubscriptions() throws HpcException;
}

 