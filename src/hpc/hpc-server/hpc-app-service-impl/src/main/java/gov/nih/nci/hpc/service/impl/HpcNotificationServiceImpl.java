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
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryReceipt;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcNotificationService;

import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    // Constants
    //---------------------------------------------------------------------//
	
    // Event payload entries attributes.
	private static final String DATA_TRANSFER_REQUEST_ID_ATTRIBUTE = 
			                    "DATA_TRANSFER_REQUEST_ID";
	private static final String DATA_TRANSFER_DOWNLOAD_STATUS_ATTRIBUTE = 
                                "DATA_TRANSFER_DOWNLOAD_STATUS";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// Map notification delivery method to its notification sender impl.
	private Map<HpcNotificationDeliveryMethod, HpcNotificationSender> notificationSenders = 
			new EnumMap<>(HpcNotificationDeliveryMethod.class);
	
    // The Notification DAO instance.
	@Autowired
    private HpcNotificationDAO notificationDAO = null;
	
	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
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
    public boolean sendNotification(String userId, HpcEventType eventType, 
                                    List<HpcEventPayloadEntry> payloadEntries,
                                    HpcNotificationDeliveryMethod deliveryMethod) 
    {
    	// Input validation.
    	if(userId == null || eventType == null || deliveryMethod == null) {
    	   return false;
    	}
    	
    	// Locate the notification sender for this delivery method.
    	HpcNotificationSender notificationSender = notificationSenders.get(deliveryMethod);
    	if(notificationSender == null) {
    	   logger.error("Could not locate notification sender for: " + deliveryMethod);
    	   return false;
    	}

    	// Send the notification.
    	try {
    	     notificationSender.sendNotification(userId, eventType, payloadEntries);
    	     
    	} catch(HpcException e) {
    		    logger.error("failed to deliver event", e);
    		    return false;
    	}
    	
    	return true;
    }
    
    @Override
    public void createNotificationDeliveryReceipt(String userId,
    		                                      int eventId, 
                                                  HpcNotificationDeliveryMethod deliveryMethod,
                                                  boolean deliveryStatus)
    {
    	if(userId == null || deliveryMethod == null) {
    	   return;
    	}
    	
    	HpcNotificationDeliveryReceipt deliveryReceipt = new HpcNotificationDeliveryReceipt();
    	deliveryReceipt.setUserId(userId);
    	deliveryReceipt.setEventId(eventId);
    	deliveryReceipt.setNotificationDeliveryMethod(deliveryMethod);
    	deliveryReceipt.setDeliveryStatus(deliveryStatus);
    	deliveryReceipt.setDelivered(Calendar.getInstance());
    	
    	try {
    	     notificationDAO.upsertDeliveryReceipt(deliveryReceipt);
    	     
    	} catch(HpcException e) {
    		    logger.error("Failed to create a delivery receipt", e);
    	}
    }
    
    @Override
    public void archiveEvent(HpcEvent event)
    {
    	if(event == null) {
    	   return;
    	}
    	
    	// Delete the event from the active table and insert to the history.
    	try {
    	     notificationDAO.deleteEvent(event.getId());
    	     notificationDAO.insertEventHistory(event);
    	     
    	} catch(HpcException e) {
    		    logger.error("Failed to archive event", e);
    	}
    }
    
    @Override
    public void addDataTransferDownloadCompletedEvent(
    		       String userId, String dataTransferRequestId,
    		       HpcDataTransferDownloadStatus dataTransferDownloadStatus) throws HpcException
    {
    	// Construct the event.
    	HpcEvent event = new HpcEvent();
    	event.getUserIds().add(userId);
    	event.setType(HpcEventType.DATA_TRANSFER_DOWNLOAD_COMPLETED);
    	event.getPayloadEntries().add(toPayloadEntry(DATA_TRANSFER_REQUEST_ID_ATTRIBUTE, 
    			                                     dataTransferRequestId));
    	event.getPayloadEntries().add(toPayloadEntry(DATA_TRANSFER_DOWNLOAD_STATUS_ATTRIBUTE, 
    			                                     dataTransferDownloadStatus.value()));

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
    	if(event == null || event.getUserIds() == null || event.getUserIds().isEmpty() ||
    	   event.getType() == null) {
    	   throw new HpcException("Invalid event",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	// Set the created timestamp.
    	event.setCreated(Calendar.getInstance());
    	
    	// Persist to DB.
    	notificationDAO.insertEvent(event);	
    }
    
    /**
     * Instantiate a payload entry object.
     * 
     * @param attribute The payload entry attribute.
     * @param value The payload entry value
     */
    
    private HpcEventPayloadEntry toPayloadEntry(String attribute, String value)
    {
		// Construct the event.
		HpcEventPayloadEntry payloadEntry = new HpcEventPayloadEntry();
		payloadEntry.setAttribute(attribute);
		payloadEntry.setValue(attribute);
		
		return payloadEntry;
    }
}

