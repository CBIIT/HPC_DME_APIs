/**
 * HpcReportRestService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import gov.nih.nci.hpc.dto.report.HpcReportRequestDTO;

/**
 * <p>
 * HPC Notification REST Service Interface.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id: $
 */

@Path("/")
public interface HpcReportRestService {
	/**
	 * Generate report.
	 *
	 * @param nciUserId
	 *            The NCI user ID.
	 * @param notificationSubscriptions
	 *            The notification subscriptions request.
	 * @return Response The REST service response.
	 */
	@POST
	@Path("/report")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response generateReport(HpcReportRequestDTO reportDTO);
}
