/**
 * HpcNotificationBusServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import java.util.List;

import gov.nih.nci.hpc.bus.HpcNotificationBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.notification.HpcNotificationType;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionsRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcNotificationService;
import gov.nih.nci.hpc.service.HpcSecurityService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Notification Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcNotificationBusServiceImpl implements HpcNotificationBusService
{      
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // Application service instances.
	
	@Autowired
    private HpcNotificationService notificationService = null;
	
	@Autowired
    private HpcSecurityService securityService = null;
	
    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
    private HpcNotificationBusServiceImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcNotificationBusService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void subscribeNotifications(String userId, 
    		                           HpcNotificationSubscriptionsRequestDTO notificationSubscriptions)
                                      throws HpcException
    {
    	logger.info("Invoking subscribeNotifications(List<HpcNotificationSubscription>): " + 
    			    notificationSubscriptions);
    	
    	// Input validation.
    	if(notificationSubscriptions == null) {
    	   throw new HpcException("Null List<HpcNotificationSubscription>",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	if(securityService.getUser(userId) == null) {
    	   throw new HpcException("Invalid user: " + userId,
	                              HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Add/Update subscriptions for the user.
    	if(notificationSubscriptions.getAddUpdateSubscriptions() != null) {
    	   for(HpcNotificationSubscription notificationSubscription : 
    		   notificationSubscriptions.getAddUpdateSubscriptions()) {
    	       notificationService.addUpdateNotificationSubscription(userId, notificationSubscription);
    	   }
    	}
    	
    	// Delete subscriptions for the user.
    	if(notificationSubscriptions.getDeleteSubscriptions() != null) {
    	   for(HpcNotificationType notificationType : 
    		   notificationSubscriptions.getDeleteSubscriptions()) {
    	       notificationService.deleteNotificationSubscription(userId, notificationType);
    	   }
    	}
    }
    
    @Override
    public HpcNotificationSubscriptionListDTO
           getNotificationSubscriptions(String userId) throws HpcException
    {
    	// Input validation.
    	if(securityService.getUser(userId) == null) {
    	   throw new HpcException("Invalid user: " + userId,
	                              HpcErrorType.INVALID_REQUEST_INPUT);	
    	}   
    	
    	// Get the subscriptions for the user.
    	List<HpcNotificationSubscription> subscriptions = 
    		 notificationService.getNotificationSubscriptions(userId);
    	if(subscriptions == null || subscriptions.isEmpty()) {
    	   return null;
    	}
    	
    	// Construct and return a DTO.
    	HpcNotificationSubscriptionListDTO subscriptionsDTO = new HpcNotificationSubscriptionListDTO();
    	subscriptionsDTO.getSubscriptions().addAll(subscriptions);
    	return subscriptionsDTO;
    }
}

 