/**
 * ReportsController.java
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.reports.ws.rest.impl;

import gov.nih.nci.hpc.reports.model.NewVaultSummary;
import gov.nih.nci.hpc.reports.model.VaultSummary;
import gov.nih.nci.hpc.reports.model.VaultSummaryResponse;
import gov.nih.nci.hpc.reports.service.ReportsService;
import gov.nih.nci.hpc.reports.ws.rest.ReportsApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@CrossOrigin
@RestController
class ReportsController implements ReportsApi {

    // ---------------------------------------------------------------------//
    // Instance members
    // ---------------------------------------------------------------------//

    // The reports service.
    @Autowired
    private ReportsService reportsService;

    // ---------------------------------------------------------------------//
    // Methods
    // ---------------------------------------------------------------------//

    // ---------------------------------------------------------------------//
    // ReportsApi  Interface Implementation
    // ---------------------------------------------------------------------//

    @RequestMapping("/vaultsummary")
    public VaultSummary[] getVaultSummary() throws Exception {
        return reportsService.getVaultSummary();
    }

    @Override
    public ResponseEntity<VaultSummaryResponse> getNewVaultSummary() {
        VaultSummaryResponse response = new VaultSummaryResponse();
        NewVaultSummary vs = new NewVaultSummary();
        vs.setName("Test");
        vs.setCapacity(new BigDecimal(123456));
        response.add(vs);
        return ResponseEntity.ok(response);
    }
}
