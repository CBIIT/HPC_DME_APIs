/**
 * HpcStaleFilesBusService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.dto.stalefiles.HpcStalePieChartDTO;
import gov.nih.nci.hpc.dto.stalefiles.HpcStaleBarChartDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Stale Files Business Service Interface.
 *
 * @author <a href="mailto:NCIDataVault@mail.nih.gov">NCI Data Vault</a>
 */
public interface HpcStaleFilesBusService {

    /**
     * Get stale files pie chart data for the given base path and current drill-down path.
     *
     * @param basePath    The base path to scope results.
     * @param currentPath The current drill-down path.
     * @return Pie chart DTO.
     * @throws HpcException on service failure.
     */
    public HpcStalePieChartDTO getPieChartData(String basePath, String currentPath)
            throws HpcException;

    /**
     * Get stale files bar chart data for immediate subfolders under the current path.
     *
     * @param basePath    The base path to scope results.
     * @param currentPath The current drill-down path.
     * @return Bar chart DTO.
     * @throws HpcException on service failure.
     */
    public HpcStaleBarChartDTO getBarChartData(String basePath, String currentPath)
            throws HpcException;
}
