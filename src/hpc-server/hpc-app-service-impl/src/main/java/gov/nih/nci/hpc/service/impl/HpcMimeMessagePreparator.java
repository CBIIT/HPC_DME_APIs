/**
 * HpcMimeMessagePreparator.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.util.List;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessagePreparator;
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcSystemAdminNotificationType;

/**
 * HPC MIME Message Preparator.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcMimeMessagePreparator {
  // ---------------------------------------------------------------------//
  // Constants
  // ---------------------------------------------------------------------//

  // NIH email domain
  private static final String NIH_EMAIL_DOMAIN = "mail.nih.gov";

  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // The notification formatter.
  @Autowired
  HpcNotificationFormatter notificationFormatter = null;

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  /**
   * Instantiate a MIME message preparator for an event.
   *
   * @param userId The recipient user ID
   * @param eventType The event type to generate the message for.
   * @param payloadEntries The payload entries to use for the message text and subject arguments.
   * @return MimeMessagePreparator
   */
  public MimeMessagePreparator getPreparator(String userId, HpcEventType eventType,
      List<HpcEventPayloadEntry> payloadEntries) {
    return (mimeMessage) -> {
      mimeMessage.setRecipient(Message.RecipientType.TO,
          new InternetAddress(userId + "@" + NIH_EMAIL_DOMAIN));
      mimeMessage.setSubject(notificationFormatter.formatSubject(eventType, payloadEntries));
      mimeMessage.setText(notificationFormatter.formatText(eventType, payloadEntries), "UTF-8",
          "html");
      mimeMessage.setSender(new InternetAddress("Do Not Reply <doNotReply@doNotReply@nih.gov>"));
    };
  }

  /**
   * Instantiate a MIME message preparator for an event.
   *
   * @param userId The recipient user ID
   * @param notificationType The system admin notification type to generate the message for.
   * @param payloadEntries The payload entries to use for the message text and subject arguments.
   * @return MimeMessagePreparator
   */
  public MimeMessagePreparator getPreparator(String userId,
      HpcSystemAdminNotificationType notificationType, List<HpcEventPayloadEntry> payloadEntries) {
    return (mimeMessage) -> {
      mimeMessage.setRecipient(Message.RecipientType.TO,
          new InternetAddress(userId + "@" + NIH_EMAIL_DOMAIN));
      mimeMessage.setSubject(notificationFormatter.formatSubject(notificationType, payloadEntries));
      mimeMessage.setText(notificationFormatter.formatText(notificationType, payloadEntries));
    };
  }
}
