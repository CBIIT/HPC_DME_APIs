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

import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Notification Sender Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcNotificationSender 
{         
    /**
     * Send a notification.
     *
     * @param event The event to notify.
     * @throws HpcException if the delivery failed.
     */
    public void sendNotification(HpcEvent event) throws HpcException;
}

 