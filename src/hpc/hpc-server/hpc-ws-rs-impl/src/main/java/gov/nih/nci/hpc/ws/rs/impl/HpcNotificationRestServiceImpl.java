/**
 * HpcNotificationRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import gov.nih.nci.hpc.bus.HpcNotificationBusService;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionsRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcNotificationRestService;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Notification REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcNotificationRestServiceImpl extends HpcRestServiceImpl
             implements HpcNotificationRestService
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Notification Business Service instance.
	@Autowired
    private HpcNotificationBusService notificationBusService = null;
    
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
    
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcNotificationRestServiceImpl() throws HpcException
    {
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcNotificationRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response subscribeNotifications(HpcNotificationSubscriptionsRequestDTO notificationSubscriptions)
    {	
		logger.info("Invoking RS: POST /notification: " + notificationSubscriptions);
		
		try {
			 notificationBusService.subscribeNotifications(notificationSubscriptions);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /notification failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(null, false);
	}
    
    @Override
    public Response getNotificationSubscriptions()
    {
		logger.info("Invoking RS: GET /notification");
	
		HpcNotificationSubscriptionListDTO subscriptions = null;
		try {
			 subscriptions = notificationBusService.getNotificationSubscriptions();
		 
		} catch(HpcException e) {
		        logger.error("RS: GET /notification failed:", e);
		        return errorResponse(e);
		}
	
		return okResponse(subscriptions, true);   	
    }
}

 