/**
 * HpcNotificationServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidNotificationSubscription;
import gov.nih.nci.hpc.dao.HpcNotificationDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcNotificationService;

import java.util.Calendar;
import java.util.List;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

/**
 * <p>
 * HPC Notification Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcNotificationServiceImpl implements HpcNotificationService
{
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Notification DAO instance.
	@Autowired
    private HpcNotificationDAO notificationDAO = null;
	
	@Autowired
	JavaMailSender mailSender = null;
	
	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

    /**
     * Default constructor disabled.
     *
     */
    private HpcNotificationServiceImpl()
    {
    }

    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//

    //---------------------------------------------------------------------//
    // HpcNotificationService Interface Implementation
    //---------------------------------------------------------------------//

    @Override
    public void addUpdateNotificationSubscription(String userId,
    		                                      HpcNotificationSubscription notificationSubscription)
                                                 throws HpcException
    {
    	// Input validation.
    	if(userId == null || 
    	   !isValidNotificationSubscription(notificationSubscription)) {
    	   throw new HpcException("Invalid add/update notification subscription request",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	// Upsert to DB.
    	notificationDAO.upsertSubscription(userId, notificationSubscription);
    }
    
    @Override
    public void deleteNotificationSubscription(String userId,
                                               HpcEventType eventType)
                                              throws HpcException
    {
    	// Input validation.
    	if(userId == null || eventType == null) {
    	   throw new HpcException("Invalid delete notification subscription request",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	// Delete from DB.
    	notificationDAO.deleteSubscription(userId, eventType);
    }
    
    @Override
    public List<HpcNotificationSubscription> getNotificationSubscriptions(String userId) 
    		                                                             throws HpcException
    {
    	// Input validation.
    	if(userId == null) {
    	   throw new HpcException("Invalid user ID",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	// Query the DB.
    	return notificationDAO.getSubscriptions(userId);
    }
    
    @Override
    public HpcNotificationSubscription getNotificationSubscription(String userId,
                                                                   HpcEventType eventType) 
                                                                  throws HpcException
    {
    	// Input validation.
    	if(userId == null || eventType == null) {
    	   throw new HpcException("Invalid user ID or event type",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	// Query the DB.
    	return notificationDAO.getSubscription(userId, eventType);
    }
    
    @Override
    public List<HpcEvent> getEvents() throws HpcException
    {
    	return notificationDAO.getEvents();
    }
    
    @Override
    public void deliverNotification(HpcEvent event, 
                                    HpcNotificationDeliveryMethod deliveryMethod) 
                                   throws HpcException
    {
    	// TODO: Invoke the notification event sender for the delivery method.
        MimeMessagePreparator preparator = new MimeMessagePreparator() {

            public void prepare(MimeMessage mimeMessage) throws Exception {

                mimeMessage.setRecipient(Message.RecipientType.TO,
                        new InternetAddress("eran.rosenberg@nih.gov"));
                mimeMessage.setFrom(new InternetAddress("HPC_SERVER@nih.gov"));
                mimeMessage.setSubject("HPC Data Transfer Request Completed Successfully!!");
                mimeMessage.setText("Data Transfer Completed. Task Id: " +
                                    event.getNotificationPayloadEntries().get(0).getValue());
            }
        };

        try {
            mailSender.send(preparator);
        }
        catch (MailException ex) {
            // simply log it and go on...
            logger.error("Failed to send email: ", ex);
        }
    	
    }
    
    @Override
    public void createDeliveryReceipts(HpcEvent event) throws HpcException
    {
    	// TODO: Create delivery receipts, then delete the event.
    	notificationDAO.deleteEvent(event.getId());
    	
    }
    
    @Override
    public void addDataTransferDownloadCompletedEvent(String userId,
    		                                          String dataTransferRequestId) throws HpcException
    {
    	// Construct the event.
    	HpcNotificationPayloadEntry payloadEntry = new HpcNotificationPayloadEntry();
    	payloadEntry.setAttribute("DATA_TRANSFER_REQUEST_ID");
    	payloadEntry.setValue(dataTransferRequestId);
    	
    	HpcEvent event = new HpcEvent();
    	event.setUserId(userId);
    	event.setType(HpcEventType.DATA_TRANSFER_DOWNLOAD_COMPLETED);
    	event.getNotificationPayloadEntries().add(payloadEntry);
    	event.setCreated(Calendar.getInstance());

    	// Persist to DB.
    	addEvent(event);
    }
    
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
    
    /**
     * Add a notification event.
     * 
     * @param notificationEvent The event to add.
     * 
     * @throws HpcException if validation failed.
     */
    private void addEvent(HpcEvent event) throws HpcException
    {
    	// Input validation.
    	if(event == null || event.getUserId() == null ||
    	   event.getType() == null || event.getCreated() == null) {
    	   throw new HpcException("Invalid event",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	// Persist to DB.
    	notificationDAO.insertEvent(event);	
    }
}

