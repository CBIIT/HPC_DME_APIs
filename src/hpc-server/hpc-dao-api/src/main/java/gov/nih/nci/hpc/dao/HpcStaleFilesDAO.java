/**
 * HpcStaleFilesDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.stalefiles.HpcStalePieChartEntry;
import gov.nih.nci.hpc.domain.stalefiles.HpcStaleBarChartEntry;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Stale Files DAO Interface.
 * </p>
 *
 * @author <a href="mailto:NCIDataVault@mail.nih.gov">NCI Data Vault</a>
 */
public interface HpcStaleFilesDAO {

    /**
     * Get stale files pie chart data for the given base path and current path.
     *
     * @param basePath    The base path to scope results (base_path = ?).
     * @param currentPath The current drill-down path (path LIKE currentPath || '/%').
     * @return List of pie chart entries grouped by stale-access bucket.
     * @throws HpcException on database error.
     */
    public List<HpcStalePieChartEntry> getPieChartData(String basePath, String currentPath)
            throws HpcException;

    /**
     * Get stale files bar chart data for immediate subfolders under the current path.
     *
     * @param basePath    The base path to scope results (base_path = ?).
     * @param currentPath The current drill-down path for subfolder extraction.
     * @return List of bar chart entries grouped by subfolder and stale-access bucket.
     * @throws HpcException on database error.
     */
    public List<HpcStaleBarChartEntry> getBarChartData(String basePath, String currentPath)
            throws HpcException;
}
