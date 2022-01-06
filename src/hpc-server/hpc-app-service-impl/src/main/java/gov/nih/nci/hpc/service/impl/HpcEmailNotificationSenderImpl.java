/**
 * HpcEmailNotificationSenderImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;

import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcSystemAdminNotificationType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Email Notification Sender
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcEmailNotificationSenderImpl implements HpcNotificationSender {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// Mail Sender.
	@Autowired
	JavaMailSender mailSender = null;

	// MIME Message Preparator.
	@Autowired
	HpcMimeMessagePreparator messagePreparator = null;

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcNotificationSender Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void sendNotification(String userId, HpcEventType eventType, String doc, List<HpcEventPayloadEntry> payloadEntries)
			throws HpcException {
		try {
			mailSender.send(messagePreparator.getPreparator(userId, eventType, doc, payloadEntries));

		} catch (MailException e) {
			throw new HpcException(e.getMessage(), e);
		}
	}

	@Override
	public void sendNotification(String userId, HpcSystemAdminNotificationType notificationType,
			List<HpcEventPayloadEntry> payloadEntries) throws HpcException {
		try {
			mailSender.send(messagePreparator.getPreparator(userId, notificationType, payloadEntries));

		} catch (MailException e) {
			throw new HpcException(e.getMessage(), e);
		}
	}
}
