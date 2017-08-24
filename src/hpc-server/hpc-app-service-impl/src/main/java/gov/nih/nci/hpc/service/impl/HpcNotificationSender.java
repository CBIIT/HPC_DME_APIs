/**
 * HpcNotificationSender.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcSystemAdminNotificationType;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Notification Sender Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcNotificationSender 
{         
    /**
     * Send a user notification.
     *
     * @param userId The user to send the notification to.
     * @param eventType The event type to notify.
     * @param payloadEntries The payload entries to use for the notification message.
     * @throws HpcException if the delivery failed.
     */
    public void sendNotification(String userId, HpcEventType eventType, 
    		                     List<HpcEventPayloadEntry> payloadEntries) 
    		                    throws HpcException;
    
    /**
     * Send a system admin notification.
     *
     * @param userId The user to send the notification to.
     * @param notificationType The system admin notification type to notify.
     * @param payloadEntries The payload entries to use for the notification message.
     * @throws HpcException if the delivery failed.
     */
    public void sendNotification(String userId, HpcSystemAdminNotificationType notificationType, 
    		                     List<HpcEventPayloadEntry> payloadEntries) 
    		                    throws HpcException;
}

 