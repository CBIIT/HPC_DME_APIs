/**
 * HpcReportService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.lastaccess.HpcLastAccessBarChartEntry;
import gov.nih.nci.hpc.domain.lastaccess.HpcLastAccessPieChartEntry;
import gov.nih.nci.hpc.domain.report.HpcReport;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * HPC Report Application Service Interface.
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 */
public interface HpcReportService {
  /**
   * Generate HPC report.
   *
   * @param criteria A criteria to generate the report.
   * @return A list reports.
   * @throws HpcException on service failure.
   */
  public List<HpcReport> generateReport(HpcReportCriteria criteria) throws HpcException;

  /**
   * Refresh all reports materialized views.
   *
   * @throws HpcException on service failure.
   */
  public void refreshViews() throws HpcException;
  
  /**
   * Get last access pie chart data for the given base path and current drill-down path.
   *
   * @param basePath    The base path to scope results.
   * @param currentPath The current drill-down path.
   * @param includeAWSBucket Whether to include AWS bucket in the results.
   * @return List of pie chart entries.
   * @throws HpcException on service failure.
   */
  public List<HpcLastAccessPieChartEntry> getLastAccessPieChartData(String basePath, String currentPath, boolean includeAWSBucket)
          throws HpcException;

  /**
   * Get last access bar chart data for immediate subfolders under the current path.
   *
   * @param basePath    The base path to scope results.
   * @param currentPath The current drill-down path.
   * @param includeAWSBucket Whether to include AWS bucket in the results.
   * @return List of bar chart entries.
   * @throws HpcException on service failure.
   */
  public List<HpcLastAccessBarChartEntry> getLastAccessBarChartData(String basePath, String currentPath, boolean includeAWSBucket)
          throws HpcException;
  


}
