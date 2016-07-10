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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
     * @param nciUserId The NCI user ID.
     * @param notificationSubscriptions The notification subscriptions request.
     * @return Response The REST service response.
     */
	@POST
    @Path("/notification/{nciUserId}")
    @Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response subscribeNotifications(@PathParam("nciUserId") String nciUserId,
    		                               HpcNotificationSubscriptionsRequestDTO notificationSubscriptions);
	
    /**
     * Get Notification Subscriptions.
     *
     * @param nciUserId The NCI user ID.
     * @return Response The REST service response.
     */
	@GET
    @Path("/notification/{nciUserId}")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getNotificationSubscriptions(@PathParam("nciUserId") String nciUserId);
}

