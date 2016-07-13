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

import java.util.List;

import gov.nih.nci.hpc.dao.HpcNotificationDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationEvent;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.notification.HpcNotificationType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcNotificationService;

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
                                               HpcNotificationType notificationType)
                                              throws HpcException
    {
    	// Input validation.
    	if(userId == null || notificationType == null) {
    	   throw new HpcException("Invalid delete notification subscription request",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	// Delete from DB.
    	notificationDAO.deleteSubscription(userId, notificationType);
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
    public void addDataTransferDownloadCompletedEvent(String userId) throws HpcException
    {
    	// Construct the event.
    	HpcNotificationEvent event = new HpcNotificationEvent();
    	event.setUserId(userId);
    	event.setNotificationType(HpcNotificationType.DATA_TRANSFER_DOWNLOAD_COMPLETED);
    	
    	// Persist to DB.
    	addNotificationEvent(event);
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
    private void addNotificationEvent(HpcNotificationEvent notificationEvent) throws HpcException
    {
    	// Input validation.
    	if(notificationEvent == null || notificationEvent.getUserId() == null ||
    	   notificationEvent.getNotificationType() == null) {
    	   throw new HpcException("Invalid notification event",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	// Persist to DB.
    	notificationDAO.insertEvent(notificationEvent);	
    }
}

