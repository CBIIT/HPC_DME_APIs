/**
 * HpcMimeMessagePreparator.java
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

import java.util.List;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessagePreparator;

/**
 * <p>
 * HPC MIME Message Preparator.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcMimeMessagePreparator 
{     
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//
	
    // NIH email domain
	private static final String NIH_EMAIL_DOMAIN = "mail.nih.gov";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// The notification formatter.
	@Autowired
	HpcNotificationFormatter notificationFormatter = null;
	
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    /**
     * Instantiate a MIME message preparator for an event.
     *
     * @param userId The recipient user ID
     * @param eventType The event type to generate the message for.
     * @param payloadEntries The payload entries to use for the message text & subject arguments.
     * @return MimeMessagePreparator
     */    
    public MimeMessagePreparator getPreparator(String userId, HpcEventType eventType, 
    		                                   List<HpcEventPayloadEntry> payloadEntries)
    {
        return new MimeMessagePreparator() 
        {
            public void prepare(MimeMessage mimeMessage) throws Exception 
            {
            	mimeMessage.setRecipient(
            			       Message.RecipientType.TO,
                               new InternetAddress(userId + "@" + NIH_EMAIL_DOMAIN));
                mimeMessage.setSubject(notificationFormatter.formatSubject(eventType, payloadEntries));
                mimeMessage.setText(notificationFormatter.formatText(eventType, payloadEntries));
            }
        };
    }
}

 