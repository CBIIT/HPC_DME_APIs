/**
 * HpcReportRestService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import gov.nih.nci.hpc.dto.report.HpcReportRequestDTO;

/**
 * HPC Notification REST Service Interface.
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 */
@Path("/")
public interface HpcReportRestService {
  /**
   * Generate report.
   *
   * @param reportRequest The report request DTO.
   * @return The REST service response w/ HpcReportsDTO entity.
   */
  @POST
  @Path("/report")
  @Consumes("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response generateReport(HpcReportRequestDTO reportRequest);
}
