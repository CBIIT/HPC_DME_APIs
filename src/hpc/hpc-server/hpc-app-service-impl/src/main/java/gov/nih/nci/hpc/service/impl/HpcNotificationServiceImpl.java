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
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcNotificationService;

import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

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

	// Map notification delivery method to its notification sender impl.
	private Map<HpcNotificationDeliveryMethod, HpcNotificationSender> notificationSenders = 
			new EnumMap<>(HpcNotificationDeliveryMethod.class);
	
    // The Notification DAO instance.
	@Autowired
    private HpcNotificationDAO notificationDAO = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

    /**
     * Default constructor disabled.
     *
     */
    private HpcNotificationServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Dosabled",
    			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }
    
    /**
     * Default constructor disabled.
     *
     * @param notificationSenders The notification senders.
     */
    private HpcNotificationServiceImpl(
    		   Map<HpcNotificationDeliveryMethod, HpcNotificationSender> notificationSenders) 
    		   throws HpcException
    {
    	this.notificationSenders.putAll(notificationSenders);
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
    public void sendNotification(HpcEvent event, 
                                 HpcNotificationDeliveryMethod deliveryMethod) 
                                throws HpcException
    {
    	// Locate the notification sender for this delivery method.
    	HpcNotificationSender notificationSender = notificationSenders.get(deliveryMethod);
    	if(notificationSender == null) {
    	   throw new HpcException("Could not locate notification sender for: " + deliveryMethod,
    			                  HpcErrorType.UNEXPECTED_ERROR);
    	}

    	notificationSender.sendNotification(event);
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
    	HpcEventPayloadEntry payloadEntry = new HpcEventPayloadEntry();
    	payloadEntry.setAttribute("DATA_TRANSFER_REQUEST_ID");
    	payloadEntry.setValue(dataTransferRequestId);
    	
    	HpcEvent event = new HpcEvent();
    	event.setUserId(userId);
    	event.setType(HpcEventType.DATA_TRANSFER_DOWNLOAD_COMPLETED);
    	event.getPayloadEntries().add(payloadEntry);
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

