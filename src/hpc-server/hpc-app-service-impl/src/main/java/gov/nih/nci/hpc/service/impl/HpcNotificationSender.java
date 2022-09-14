/**
 * HpcNotificationSender.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcSystemAdminNotificationType;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * HPC Notification Sender Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcNotificationSender {
  /**
   * Send a user notification.
   *
   * @param userId The user to send the notification to.
   * @param eventType The event type to notify.
   * @param doc The user's doc if doc specific template is available.
   * @param payloadEntries The payload entries to use for the notification message.
   * @param attachment The attachment to send.
   * @throws HpcException if the delivery failed.
   */
  public void sendNotification(
      String userId, HpcEventType eventType, String doc, List<HpcEventPayloadEntry> payloadEntries, String attachment)
      throws HpcException;

  /**
   * Send a system admin notification.
   *
   * @param userId The user to send the notification to.
   * @param notificationType The system admin notification type to notify.
   * @param payloadEntries The payload entries to use for the notification message.
   * @throws HpcException if the delivery failed.
   */
  public void sendNotification(
      String userId,
      HpcSystemAdminNotificationType notificationType,
      List<HpcEventPayloadEntry> payloadEntries)
      throws HpcException;
}
