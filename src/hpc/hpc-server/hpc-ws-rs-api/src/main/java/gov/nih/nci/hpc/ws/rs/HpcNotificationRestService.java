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
    @Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response subscribeNotifications(HpcNotificationSubscriptionsRequestDTO notificationSubscriptions);
	
    /**
     * Get Notification Subscriptions.
     *
     * @return The REST service response.
     */
	@GET
    @Path("/notification")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getNotificationSubscriptions();
	
    /**
     * Get Notification Delivery Receipts.
     *
     * @param page The requested results page.
     * @param totalCount If set to true, return the total count of collections matching the query
     *                   regardless of the limit on returned entities.
     * @return The REST service response.
     */
	@GET
    @Path("/notification/deliveryReceipt")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getNotificationDeliveryReceipts(@QueryParam("page") Integer page,
                                                    @QueryParam("totalCount") Boolean totalCount);
}

