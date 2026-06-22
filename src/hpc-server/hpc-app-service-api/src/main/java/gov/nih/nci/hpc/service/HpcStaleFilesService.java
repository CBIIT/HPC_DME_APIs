/**
 * HpcStaleFilesService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.stalefiles.HpcStalePieChartEntry;
import gov.nih.nci.hpc.domain.stalefiles.HpcStaleBarChartEntry;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * HPC Stale Files Application Service Interface.
 *
 * @author <a href="mailto:NCIDataVault@mail.nih.gov">NCI Data Vault</a>
 */
public interface HpcStaleFilesService {

    /**
     * Get stale files pie chart data for the given base path and current drill-down path.
     *
     * @param basePath    The base path to scope results.
     * @param currentPath The current drill-down path.
     * @return List of pie chart entries.
     * @throws HpcException on service failure.
     */
    public List<HpcStalePieChartEntry> getPieChartData(String basePath, String currentPath)
            throws HpcException;

    /**
     * Get stale files bar chart data for immediate subfolders under the current path.
     *
     * @param basePath    The base path to scope results.
     * @param currentPath The current drill-down path.
     * @return List of bar chart entries.
     * @throws HpcException on service failure.
     */
    public List<HpcStaleBarChartEntry> getBarChartData(String basePath, String currentPath)
            throws HpcException;
}
