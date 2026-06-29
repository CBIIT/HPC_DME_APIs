/**
 * HpcReportBusService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.dto.report.HpcLastAccessBarChartDTO;
import gov.nih.nci.hpc.dto.report.HpcLastAccessPieChartDTO;
import gov.nih.nci.hpc.dto.report.HpcReportRequestDTO;
import gov.nih.nci.hpc.dto.report.HpcReportsDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Report Business Service Interface.
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */
public interface HpcReportBusService {
  /**
   * Generate HPC report.
   *
   * @param criteria to generate the report
   * @return Reports DTO
   * @throws HpcException on service failure.
   */
  public HpcReportsDTO generateReport(HpcReportRequestDTO criteria) throws HpcException;
  
  /**
   * Get last access files pie chart data for the given base path and current drill-down path.
   *
   * @param basePath    The base path to scope results.
   * @param currentPath The current drill-down path.
   * @return Pie chart DTO.
   * @throws HpcException on service failure.
   */
  public HpcLastAccessPieChartDTO getLastAccessPieChartData(String basePath, String currentPath)
          throws HpcException;

  /**
   * Get last access files bar chart data for immediate subfolders under the current path.
   *
   * @param basePath    The base path to scope results.
   * @param currentPath The current drill-down path.
   * @return Bar chart DTO.
   * @throws HpcException on service failure.
   */
  public HpcLastAccessBarChartDTO getLastAccessBarChartData(String basePath, String currentPath)
          throws HpcException;
}
