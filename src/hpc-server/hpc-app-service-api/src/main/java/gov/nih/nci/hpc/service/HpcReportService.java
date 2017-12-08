/**
 * HpcReportService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

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
}
