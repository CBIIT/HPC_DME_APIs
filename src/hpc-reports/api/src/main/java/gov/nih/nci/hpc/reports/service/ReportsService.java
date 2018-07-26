/**
 * ReportsService.java
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.reports.service;

import gov.nih.nci.hpc.reports.model.VaultSummary;

/**
 * Reports Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface ReportsService {

    /**
     * Get summary report for all vaults.
     *
     * @return An array of vault summary.
     * @throws Exception on cloud storage management system failure.
     */
    public VaultSummary[] getVaultSummary() throws Exception;
}
