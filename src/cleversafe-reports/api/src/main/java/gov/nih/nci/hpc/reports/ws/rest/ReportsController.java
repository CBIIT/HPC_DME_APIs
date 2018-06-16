package gov.nih.nci.hpc.reports.ws.rest;

import gov.nih.nci.hpc.reports.model.VaultSummary;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
public class ReportsController {

    @RequestMapping("/vaultsummary")
    public VaultSummary[] getVaultSummary() {
        VaultSummary[] report = new VaultSummary[2];

        report[0] = new VaultSummary();
        report[1] = new VaultSummary();

        return report;
    }
}
