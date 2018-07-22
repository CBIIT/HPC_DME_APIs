/**
 * CloudStorageManagementBrokerImpl.java
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.reports.integration.cleversafe.impl;

import gov.nih.nci.hpc.reports.integration.CloudStorageManagementBroker;
import gov.nih.nci.hpc.reports.model.VaultSummary;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Cloud Storage Management Broker Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
@Component
class CloudStorageManagementBrokerImpl implements CloudStorageManagementBroker {

    // ---------------------------------------------------------------------//
    // Constants
    // ---------------------------------------------------------------------//

    // 1TB in bytes.
    private static final double TERA_BYTES = 1099511627776.0;

    // ---------------------------------------------------------------------//
    // Instance members
    // ---------------------------------------------------------------------//

    // The REST template.
    private RestTemplate restTemplate;

    // The Cleverafe List Vaults endpoint URL.
    @Value("${hpc.reports.integration.cleversafe.list-vaults-endpoint}")
    private String listVaultsUrl;

    // JSON Parser
    private JSONParser jsonParser = new JSONParser();

    // The Logger instance.
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    // ---------------------------------------------------------------------//
    // Constructors
    // ---------------------------------------------------------------------//

    /**
     * Constructor for Spring Dependency Injection.
     */
    @Autowired
    private CloudStorageManagementBrokerImpl(@Value("${hpc.reports.integration.cleversafe.username}") String username,
                                             @Value("${hpc.reports.integration.cleversafe.password}") String password) {
        restTemplate = new RestTemplateBuilder().basicAuthorization(username, password).build();
    }

    // ---------------------------------------------------------------------//
    // Methods
    // ---------------------------------------------------------------------//

    // ---------------------------------------------------------------------//
    // CloudStorageManagementBroker Interface Implementation
    // ---------------------------------------------------------------------//

    @Override
    public Collection<VaultSummary> getVaultsSummary() throws Exception {
        // Invoke Cleversafe list-vaults API.
        ResponseEntity<String> response = restTemplate.getForEntity(listVaultsUrl, String.class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            throw new Exception("Failed to call service: [" + response.getStatusCode() + "] :" + response.getBody());
        }

        // Parse the response JSON.
        Collection<VaultSummary> vaultsSummary = new ArrayList<VaultSummary>();
        JSONObject responseData = (JSONObject) ((JSONObject) jsonParser.parse(response.getBody())).get("responseData");

        JSONArray vaults = (JSONArray) responseData.get("vaults");
        if (vaults != null) {
            // Iterate through the vaults and add a summary object into the collection.
            Iterator<JSONObject> vaultsIterator = vaults.iterator();
            while (vaultsIterator.hasNext()) {
                JSONObject vault = vaultsIterator.next();
                VaultSummary vaultSummary = new VaultSummary();
                vaultSummary.setName((String) vault.get("name"));
                vaultSummary.setDescription((String) vault.get("description"));
                vaultSummary.setCapacity((Long) vault.get("usableSize") / TERA_BYTES);
                vaultSummary.setUsed((Long) vault.get("usedLogicalSizeFromStorage") / TERA_BYTES);
                vaultSummary.setCreationDate(((String) vault.get("creationDate")).substring(0, 16));
                vaultsSummary.add(vaultSummary);
            }
        }

        return vaultsSummary;
    }


}
