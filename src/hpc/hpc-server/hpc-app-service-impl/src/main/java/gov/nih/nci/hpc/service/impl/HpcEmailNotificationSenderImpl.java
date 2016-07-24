/**
 * HpcEmailNotificationSenderImpl.java
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * <p>
 * HPC Email Notification Sender
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcEmailNotificationSenderImpl implements HpcNotificationSender
{     
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// Mail Sender.
	@Autowired
	JavaMailSender mailSender = null;
	
	// MIME Message Preparator.
	@Autowired
	HpcMimeMessagePreparator messagePreparator = null;
	
    //---------------------------------------------------------------------//
    // HpcNotificationSender Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void sendNotification(HpcEvent event) throws HpcException
    {
        try {
             mailSender.send(messagePreparator.getPreparator(event));
             
        } catch(MailException e) {
                throw new HpcException(e.getMessage(), e);
        }
    }
}

 