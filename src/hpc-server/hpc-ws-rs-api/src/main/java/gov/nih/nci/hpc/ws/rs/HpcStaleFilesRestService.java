/**
 * HpcStaleFilesRestService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

/**
 * HPC Stale Files REST Service Interface.
 *
 * @author <a href="mailto:NCIDataVault@mail.nih.gov">NCI Data Vault</a>
 */
@Path("/")
public interface HpcStaleFilesRestService {

    /**
     * Get stale files pie chart data for the given base path and current drill-down path.
     *
     * @param basePath    The base path to scope results.
     * @param currentPath The current drill-down path.
     * @return The REST service response w/ HpcStalePieChartDTO entity.
     */
    @GET
    @Path("/staleFiles/pieChart")
    @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
    public Response getPieChartData(
            @QueryParam("basePath") String basePath,
            @QueryParam("currentPath") String currentPath);

    /**
     * Get stale files bar chart data for immediate subfolders under the current path.
     *
     * @param basePath    The base path to scope results.
     * @param currentPath The current drill-down path.
     * @return The REST service response w/ HpcStaleBarChartDTO entity.
     */
    @GET
    @Path("/staleFiles/barChart")
    @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
    public Response getBarChartData(
            @QueryParam("basePath") String basePath,
            @QueryParam("currentPath") String currentPath);
}
