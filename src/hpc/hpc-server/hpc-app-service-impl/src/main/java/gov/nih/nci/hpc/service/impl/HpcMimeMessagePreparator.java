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

import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.exception.HpcException;

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
     * @param event The event to instantiate the MIME message preparator for.
     * @return MimeMessagePreparator
     * @throws HpcException
     */    
    public MimeMessagePreparator getPreparator(HpcEvent event) throws HpcException
    {
        return new MimeMessagePreparator() 
        {
            public void prepare(MimeMessage mimeMessage) throws Exception 
            {
            	mimeMessage.setRecipient(Message.RecipientType.TO,
                                         new InternetAddress("eran.rosenberg@nih.gov"));
                mimeMessage.setFrom(new InternetAddress("HPC_SERVER@nih.gov"));
                mimeMessage.setSubject(notificationFormatter.formatSubject(event));
                mimeMessage.setText(notificationFormatter.formatText(event));
            }
        };
    }
}

 