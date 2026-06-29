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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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
  
  /**
   * Get last access files pie chart data for the given base path and current drill-down path.
   *
   * @param basePath    The base path to scope results.
   * @param currentPath The current drill-down path.
   * @return The REST service response w/ HpcLastAccessPieChartDTO entity.
   */
  @GET
  @Path("/report/lastAccessPieChart")
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response getLastAccessPieChartData(
          @QueryParam("basePath") String basePath,
          @QueryParam("currentPath") String currentPath);

  /**
   * Get last access files bar chart data for immediate subfolders under the current path.
   *
   * @param basePath    The base path to scope results.
   * @param currentPath The current drill-down path.
   * @return The REST service response w/ HpcLastAccessBarChartDTO entity.
   */
  @GET
  @Path("/report/lastAccessBarChart")
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response getLastAccessBarChartData(
          @QueryParam("basePath") String basePath,
          @QueryParam("currentPath") String currentPath);
  
}
