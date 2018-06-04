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
import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionsRequestDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionsResponseDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcNotificationRestService;

import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Notification REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
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
    public Response subscribeNotifications(
      HpcNotificationSubscriptionsRequestDTO notificationSubscriptions) {
			try {
        HpcNotificationSubscriptionsResponseDTO serviceResponseDto =
          notificationBusService.subscribeNotifications(
          notificationSubscriptions);
        return okResponse(serviceResponseDto, false);
			} catch(HpcException e) {
			    return errorResponse(e);
			}
	  }

    @Override
    public Response getNotificationSubscriptions()
    {
		HpcNotificationSubscriptionListDTO subscriptions = null;
		try {
			 subscriptions = notificationBusService.getNotificationSubscriptions();
		 
		} catch(HpcException e) {
		        return errorResponse(e);
		}
	
		return okResponse(subscriptions, true);   	
    }
    
    @Override
    public Response getNotificationDeliveryReceipts(Integer page, Boolean totalCount)
    {
 		HpcNotificationDeliveryReceiptListDTO deliveryReceipts = null;
		try {
			 deliveryReceipts = notificationBusService.getNotificationDeliveryReceipts(
			 		                                      page != null ? page : 1,
					                                      totalCount != null ? totalCount : false);

		} catch(HpcException e) {
			    return errorResponse(e);
		}
	
		return okResponse(!deliveryReceipts.getNotificationDeliveryReceipts().isEmpty() ? 
				          deliveryReceipts : null, true);   	
    }

    @Override
    public Response getNotificationDeliveryReceipt(Integer eventId)
    {
		HpcNotificationDeliveryReceiptListDTO deliveryReceipts = new HpcNotificationDeliveryReceiptListDTO();
		HpcNotificationDeliveryReceiptDTO deliveryReceipt = null;
		try {
			 deliveryReceipt = notificationBusService.getNotificationDeliveryReceipt(eventId);
			 deliveryReceipts.getNotificationDeliveryReceipts().add(deliveryReceipt);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
	
		return okResponse(deliveryReceipts, true);   	
    }
}

 
