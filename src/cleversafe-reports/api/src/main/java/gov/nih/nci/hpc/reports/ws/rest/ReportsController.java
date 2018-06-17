package gov.nih.nci.hpc.reports.ws.rest;

import gov.nih.nci.hpc.reports.model.VaultSummary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CrossOrigin
@RestController
@RequestMapping("/reports")
public class ReportsController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/vaultsummary")
    public VaultSummary[] getVaultSummary() {

        RestTemplate restTemplate = new RestTemplateBuilder().basicAuthorization("ncifhpcdmsvcp", "").build();

        logger.error("Eran");
        ResponseEntity<String> reportData = restTemplate.getForEntity("https://fr-s-clvrsf-mgr.ncifcrf.gov/manager/api/json/1.0/listVaults.adm", String.class)
        logger.error("Eran: " + reportData);

        VaultSummary[] report = new VaultSummary[2];

        report[0] = new VaultSummary();
        report[1] = new VaultSummary();

        return report;
    }
}
