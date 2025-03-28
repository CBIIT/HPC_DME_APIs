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

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.MimeMessageHelper;
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
	 * @param userId         The recipient user ID
	 * @param eventType      The event type to generate the message for.
	 * @param doc			 The doc specific template to be used for this user. (Optional)
	 * @param payloadEntries The payload entries to use for the message text and
	 *                       subject arguments.
	 * @param attachement    The attachment to send. (Optional)
	 * @return MimeMessagePreparator
	 */
	public MimeMessagePreparator getPreparator(String userId, HpcEventType eventType, String doc,
			List<HpcEventPayloadEntry> payloadEntries, String attachment) {
		return mimeMessage -> {
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
			if (attachment != null) {
				FileSystemResource file = new FileSystemResource(attachment);
			    helper.addAttachment(file.getFilename(), file);
			}        
            helper.setTo(new InternetAddress(userId + "@" + NIH_EMAIL_DOMAIN));
            helper.setSubject(notificationFormatter.formatSubject(eventType, doc, payloadEntries));
            helper.setText(notificationFormatter.formatText(eventType, doc, payloadEntries), true);
			String fromDisplay = notificationFormatter.formatFromDisplay(eventType, doc, payloadEntries);
			helper.setFrom(fromDisplay == null || fromDisplay.isEmpty() ? "DME Notification <dme-notification@doNotReply.nih.gov>" : fromDisplay + " <dme-notification@doNotReply.nih.gov>");
		};
	}

	/**
	 * Instantiate a MIME message preparator for an event.
	 *
	 * @param userId           The recipient user ID
	 * @param notificationType The system admin notification type to generate the
	 *                         message for.
	 * @param payloadEntries   The payload entries to use for the message text and
	 *                         subject arguments.
	 * @return MimeMessagePreparator
	 */
	public MimeMessagePreparator getPreparator(String userId, HpcSystemAdminNotificationType notificationType,
			List<HpcEventPayloadEntry> payloadEntries) {
		return (mimeMessage) -> {
			mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(userId + "@" + NIH_EMAIL_DOMAIN));
			mimeMessage.setSubject(notificationFormatter.formatSubject(notificationType, payloadEntries));
			mimeMessage.setText(notificationFormatter.formatText(notificationType, payloadEntries));
		};
	}
}
