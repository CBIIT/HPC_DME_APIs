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

import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.notification.HpcNotificationType;
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
     * @param notificationType The notification type to delete.
     * 
     * @throws HpcException
     */
    public void deleteNotificationSubscription(String userId,
    		                                   HpcNotificationType notificationType)
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
     * Add a data transfer download completed event.
     *
     * @param userId The user ID.
     * 
     * @throws HpcException
     */
    public void addDataTransferDownloadCompletedEvent(String userId) throws HpcException;
}

 