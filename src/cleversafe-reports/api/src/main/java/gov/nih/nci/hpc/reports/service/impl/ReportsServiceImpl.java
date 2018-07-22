/**
 * ReportsServiceImpl.java
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.reports.service.impl;

import gov.nih.nci.hpc.reports.integration.CloudStorageManagementBroker;
import gov.nih.nci.hpc.reports.model.VaultSummary;
import gov.nih.nci.hpc.reports.service.ReportsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Reports Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
@Component
class ReportsServiceImpl implements ReportsService {

    // ---------------------------------------------------------------------//
    // Instance members
    // ---------------------------------------------------------------------//

    // The cloud storage management broker.
    @Autowired
    private CloudStorageManagementBroker cloudStorageManagementBroker;

    // The Logger instance.
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    // ---------------------------------------------------------------------//
    // Constructors
    // ---------------------------------------------------------------------//

    /**
     * Constructor for Spring Dependency Injection.
     */
    private ReportsServiceImpl() {}

    // ---------------------------------------------------------------------//
    // Methods
    // ---------------------------------------------------------------------//

    // ---------------------------------------------------------------------//
    // ReportsService Interface Implementation
    // ---------------------------------------------------------------------//

    @Override
    public VaultSummary[] getVaultSummary() throws Exception {

        // Get the vaults summary report.
        Collection<VaultSummary> vaultsSummary = cloudStorageManagementBroker.getVaultsSummary();
        logger.info("Vault Summary Report size: {}", vaultsSummary.size());

        // Return an array.
        VaultSummary[] vaultSummariesArray = new VaultSummary[vaultsSummary.size()];
        return vaultsSummary.toArray(vaultSummariesArray);
    }

}
