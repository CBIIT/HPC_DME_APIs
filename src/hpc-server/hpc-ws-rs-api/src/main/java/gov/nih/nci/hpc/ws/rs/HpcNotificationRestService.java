/**
 * HpcNotificationRestService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs;

import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionsRequestDTO;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * <p>
 * HPC Notification REST Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

@Path("/")
public interface HpcNotificationRestService
{
    /**
     * Subscribe to notifications.
     *
     * @param notificationSubscriptions The notification subscriptions request.
     * @return The REST service response.
     */
	@POST
    @Path("/notification")
    @Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
    @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
    public Response subscribeNotifications(HpcNotificationSubscriptionsRequestDTO notificationSubscriptions);
	
    /**
     * Get Notification Subscriptions.
     *
     * @return The REST service response w/ HpcNotificationSubscriptionListDTO entity.
     */
	@GET
    @Path("/notification")
    @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
    public Response getNotificationSubscriptions();
	
    /**
     * Get notification delivery receipts.
     *
     * @param page The requested results page.
     * @param totalCount If set to true, return the total count of collections matching the query
     *                   regardless of the limit on returned entities.
     * @return The REST service response w/ HpcNotificationDeliveryReceiptListDTO entity.
     */
	@GET
    @Path("/notification/deliveryReceipts")
    @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
    public Response getNotificationDeliveryReceipts(@QueryParam("page") Integer page,
                                                    @QueryParam("totalCount") Boolean totalCount);
	
	/**
     * Get Notification Delivery Receipt.
     *
     * @param eventId The requested delivery receipt event Id.
     * @return The REST service response w/ HpcNotificationDeliveryReceiptListDTO entity.
     */
	@GET
    @Path("/notification/deliveryReceipt")
    @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
    public Response getNotificationDeliveryReceipt(@QueryParam("eventId") Integer eventId);
}

