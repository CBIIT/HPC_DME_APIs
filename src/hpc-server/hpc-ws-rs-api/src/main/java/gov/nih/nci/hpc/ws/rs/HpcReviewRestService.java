/**
 * HpcReviewRestService.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

/**
 * HPC Review REST Service Interface.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
@Path("/")
public interface HpcReviewRestService {

	/**
	 * Query review entries.
	 *
	 * @param projectStatus project status
	 * @param dataCurator   data curator
	 * @return The REST service response w/ HpcReviewDTO entity.
	 */
	@POST
	@Path("/review/query")
	@Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response queryReview(@QueryParam("projectStatus") String projectStatus,
			@QueryParam("dataCurator") String dataCurator);

	/**
	 * Send review reminder notification.
	 *
	 * @param nciUserId The user NCI ID.
	 * @return The REST service response.
	 */
	@POST
	@Path("/review/sendReminder")
	@Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
	public Response sendReminder(String nciUserId);

}
