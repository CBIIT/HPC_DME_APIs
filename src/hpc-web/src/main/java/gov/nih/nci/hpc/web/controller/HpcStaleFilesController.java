/**
 * HpcStaleFilesController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDocDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Controller for the Stale Files Dashboard.
 * </p>
 *
 * @author <a href="mailto:NCIDataVault@mail.nih.gov">NCI Data Vault</a>
 */
@Controller
@EnableAutoConfiguration
@RequestMapping("/staleFiles")
public class HpcStaleFilesController extends AbstractHpcController {

    @Value("${gov.nih.nci.hpc.server.stalefiles}")
    private String staleFilesServiceURL;

    @Value("${gov.nih.nci.hpc.server.model}")
    private String hpcModelURL;

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /**
     * GET action to display the stale files dashboard page.
     */
    @RequestMapping(method = RequestMethod.GET)
    public String home(@RequestBody(required = false) String q, Model model,
            BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
        String authToken = (String) session.getAttribute("hpcUserToken");
        if (authToken == null) {
            return "redirect:/login?returnPath=staleFiles";
        }
        HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
        if (user == null) {
            ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
            bindingResult.addError(error);
            HpcLogin hpcLogin = new HpcLogin();
            model.addAttribute("hpcLogin", hpcLogin);
            return "redirect:/login?returnPath=staleFiles";
        }

        model.addAttribute("userRole", user.getUserRole());

        // Populate basepaths available to this user.
        List<String> basepaths = new ArrayList<>();
        for (HpcDocDataManagementRulesDTO docRule : getModelDTO(session).getDocRules()) {
            if (user.getUserRole().equals("SYSTEM_ADMIN")
                    || docRule.getDoc().equals(user.getDoc())) {
                for (HpcDataManagementRulesDTO rule : docRule.getRules()) {
                    basepaths.add(rule.getBasePath());
                }
            }
        }
        basepaths.sort(String.CASE_INSENSITIVE_ORDER);
        model.addAttribute("basepaths", basepaths);

        return "stalefiles";
    }

    /**
     * AJAX GET endpoint to retrieve pie chart data from the hpc-server.
     *
     * @param basePath    The base path selected by the user.
     * @param currentPath The current drill-down path.
     * @param session     The HTTP session.
     * @param request     The HTTP request.
     * @return JSON string of HpcStalePieChartDTO, or error JSON.
     */
    @GetMapping(value = "/pieChartData", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getPieChartData(
            @RequestParam("basePath") String basePath,
            @RequestParam("currentPath") String currentPath,
            HttpSession session, HttpServletRequest request) {
        String authToken = (String) session.getAttribute("hpcUserToken");
        if (authToken == null) {
            return "{\"error\":\"Unauthorized\"}";
        }
        try {
            String requestURL = UriComponentsBuilder
                    .fromHttpUrl(staleFilesServiceURL + "/pieChart")
                    .queryParam("basePath", basePath)
                    .queryParam("currentPath", currentPath)
                    .build().encode().toUri().toURL().toExternalForm();

            WebClient client = HpcClientUtil.getWebClient(requestURL, sslCertPath, sslCertPassword);
            client.header("Authorization", "Bearer " + authToken);
            Response restResponse = client.invoke("GET", null);

            if (restResponse.getStatus() == 200) {
                MappingJsonFactory factory = new MappingJsonFactory();
                JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                return mapper.writeValueAsString(parser.readValueAs(Object.class));
            } else {
                logger.error("Stale files pie chart request failed with status: {}",
                        restResponse.getStatus());
                return "{\"error\":\"Failed to retrieve pie chart data\"}";
            }
        } catch (Exception e) {
            logger.error("Error retrieving stale files pie chart data: {}", e.getMessage(), e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * AJAX GET endpoint to retrieve bar chart data from the hpc-server.
     *
     * @param basePath    The base path selected by the user.
     * @param currentPath The current drill-down path.
     * @param session     The HTTP session.
     * @param request     The HTTP request.
     * @return JSON string of HpcStaleBarChartDTO, or error JSON.
     */
    @GetMapping(value = "/barChartData", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getBarChartData(
            @RequestParam("basePath") String basePath,
            @RequestParam("currentPath") String currentPath,
            HttpSession session, HttpServletRequest request) {
        String authToken = (String) session.getAttribute("hpcUserToken");
        if (authToken == null) {
            return "{\"error\":\"Unauthorized\"}";
        }
        try {
            String requestURL = UriComponentsBuilder
                    .fromHttpUrl(staleFilesServiceURL + "/barChart")
                    .queryParam("basePath", basePath)
                    .queryParam("currentPath", currentPath)
                    .build().encode().toUri().toURL().toExternalForm();

            WebClient client = HpcClientUtil.getWebClient(requestURL, sslCertPath, sslCertPassword);
            client.header("Authorization", "Bearer " + authToken);
            Response restResponse = client.invoke("GET", null);

            if (restResponse.getStatus() == 200) {
                MappingJsonFactory factory = new MappingJsonFactory();
                JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                return mapper.writeValueAsString(parser.readValueAs(Object.class));
            } else {
                logger.error("Stale files bar chart request failed with status: {}",
                        restResponse.getStatus());
                return "{\"error\":\"Failed to retrieve bar chart data\"}";
            }
        } catch (Exception e) {
            logger.error("Error retrieving stale files bar chart data: {}", e.getMessage(), e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
