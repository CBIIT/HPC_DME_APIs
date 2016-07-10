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
import gov.nih.nci.hpc.exception.HpcException;

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
     * Add/Update notification subscription for a user.
     *
     * @param userId The user ID.
     * @param notificationSubscription The notification subscription to add/update.
     * 
     * @throws HpcException
     */
    public void addUpdateNotificationSubscription(String userId,
    		                                      HpcNotificationSubscription notificationSubscription)
                                                 throws HpcException;
}

 