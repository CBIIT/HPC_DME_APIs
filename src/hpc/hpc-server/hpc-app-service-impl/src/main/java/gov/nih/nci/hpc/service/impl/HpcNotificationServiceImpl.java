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
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryReceipt;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
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
     * @throws HpcException Constructor disabled.
     *
     */
    private HpcNotificationServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Dosabled",
    			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }
    
    /**
     * Constructor for Spring Dependency Injection.
     *
     * @param notificationSenders The notification senders.
     */
    private HpcNotificationServiceImpl(
    		   Map<HpcNotificationDeliveryMethod, HpcNotificationSender> notificationSenders) 
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
    public void addUpdateNotificationSubscription(HpcNotificationSubscription notificationSubscription)
                                                 throws HpcException
    {
    	// Input validation.
    	if(!isValidNotificationSubscription(notificationSubscription)) {
    	   throw new HpcException("Invalid add/update notification subscription request",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	// Validate subscription for usage summary report is allowed for system admin only 
    	if(!HpcRequestContext.getRequestInvoker().getUserRole().equals(HpcUserRole.SYSTEM_ADMIN)) {
    	   if(notificationSubscription.getEventType().equals(HpcEventType.USAGE_SUMMARY_REPORT) || 
    	      notificationSubscription.getEventType().equals(HpcEventType.USAGE_SUMMARY_BY_WEEKLY_REPORT) ||
    	      notificationSubscription.getEventType().equals(HpcEventType.USAGE_SUMMARY_BY_DOC_REPORT) ||
    	      notificationSubscription.getEventType().equals(HpcEventType.USAGE_SUMMARY_BY_DOC_BY_WEEKLY_REPORT) ||
    	      notificationSubscription.getEventType().equals(HpcEventType.USAGE_SUMMARY_BY_USER_REPORT) ||
    	      notificationSubscription.getEventType().equals(HpcEventType.USAGE_SUMMARY_BY_USER_BY_WEEKLY_REPORT)) {
    	      throw new HpcException("Not authorizated to subscribe to the report. Please contact system administrator",
    	       	                     HpcRequestRejectReason.NOT_AUTHORIZED);
    	   }
    	}
    	
       	// Get the service invoker.
       	HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
       	if(invoker == null) {
       	   throw new HpcException("Unknown service invoker", 
		                          HpcErrorType.UNEXPECTED_ERROR);
       	}

    	// Upsert to DB.
    	notificationDAO.upsertSubscription(invoker.getNciAccount().getUserId(), 
    			                           notificationSubscription);
    }
    
    @Override
    public void deleteNotificationSubscription(HpcEventType eventType)
                                              throws HpcException
    {
    	// Input validation.
    	if(eventType == null) {
    	   throw new HpcException("Null event type",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
       	// Get the service invoker.
       	HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
       	if(invoker == null) {
       	   throw new HpcException("Unknown service invoker", 
		                          HpcErrorType.UNEXPECTED_ERROR);
       	}

    	// Delete from DB.
    	notificationDAO.deleteSubscription(invoker.getNciAccount().getUserId(), eventType);
    }
    
    @Override
    public List<HpcNotificationSubscription> getNotificationSubscriptions() throws HpcException
    {
       	// Get the service invoker.
       	HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
       	if(invoker == null) {
       	   throw new HpcException("Unknown service invoker", 
		                          HpcErrorType.UNEXPECTED_ERROR);
       	}

    	// Query the DB.
    	return notificationDAO.getSubscriptions(invoker.getNciAccount().getUserId());
    }
    
    @Override
    public List<String> getNotificationSubscribedUsers(HpcEventType eventType) throws HpcException
    {
    	return notificationDAO.getSubscribedUsers(eventType);
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
}

