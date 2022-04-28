/**
 * HpcReportsController.java
 *
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for
 * details.
 */
package gov.nih.nci.hpc.web.controller;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import gov.nih.nci.hpc.domain.report.HpcReportType;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDocDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.report.HpcReportDTO;
import gov.nih.nci.hpc.dto.report.HpcReportEntryDTO;
import gov.nih.nci.hpc.dto.report.HpcReportRequestDTO;
import gov.nih.nci.hpc.dto.report.HpcReportsDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.dto.security.HpcUserListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserListEntry;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcReportRequest;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.MiscUtil;

/**
 * <p>
 * Controller to generate usage reports
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/reports")
public class HpcReportsController extends AbstractHpcController {
  @Value("${gov.nih.nci.hpc.server.report}")
  private String serviceURL;
  @Value("${gov.nih.nci.hpc.server.user.active}")
  private String activeUsersServiceURL;
  @Value("${gov.nih.nci.hpc.server.model}")
  private String hpcModelURL;
  @Value("${gov.nih.nci.hpc.server.collection}")
  private String hpcCollectionlURL;

  @Autowired
  private Environment env;

  //The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  /**
   * GET Operation to prepare reports page
   * 
   * @param q
   * @param model
   * @param bindingResult
   * @param session
   * @param request
   * @return
   */
  @RequestMapping(method = RequestMethod.GET)
  public String home(@RequestBody(required = false) String q, Model model,
      BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
    model.addAttribute("reportRequest", new HpcReportRequest());
    return init(model, bindingResult, session);
  }


  private String init(Model model, BindingResult bindingResult, HttpSession session) {
    String authToken = (String) session.getAttribute("hpcUserToken");
    if (authToken == null) {
      return "redirect:/login?returnPath=reports";
    }
    HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
    if (user == null) {
      ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
      bindingResult.addError(error);
      HpcLogin hpcLogin = new HpcLogin();
      model.addAttribute("hpcLogin", hpcLogin);
      return "redirect:/login?returnPath=reports";
    }

    model.addAttribute("userRole", user.getUserRole());
    model.addAttribute("userDOC", user.getDoc());
    HpcUserListDTO users = HpcClientUtil.getUsers(authToken, activeUsersServiceURL, null, null,
        null, user.getUserRole().equals("SYSTEM_ADMIN") ? null : user.getDoc(), sslCertPath,
        sslCertPassword);
    Comparator<HpcUserListEntry> firstLastComparator = Comparator.comparing(HpcUserListEntry::getFirstName, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(HpcUserListEntry::getLastName, String.CASE_INSENSITIVE_ORDER);
    users.getUsers().sort(firstLastComparator);
    model.addAttribute("docUsers", users.getUsers());
    List<String> docs = new ArrayList<>();

    if (user.getUserRole().equals("GROUP_ADMIN") || user.getUserRole().equals("USER"))
      docs.add(user.getDoc());
    else if (user.getUserRole().equals("SYSTEM_ADMIN")) {
      docs.addAll(
          HpcClientUtil.getDOCs(authToken, hpcModelURL, sslCertPath, sslCertPassword, session));
    }
    docs.sort(String.CASE_INSENSITIVE_ORDER);
    model.addAttribute("docs", docs);

    List<String> basepaths = new ArrayList<>();
    for (HpcDocDataManagementRulesDTO docRule : getModelDTO(session).getDocRules()) {
      for (HpcDataManagementRulesDTO rule : docRule.getRules())
        basepaths.add(rule.getBasePath());
    }
    basepaths.sort(String.CASE_INSENSITIVE_ORDER);
    model.addAttribute("basepaths", basepaths);
    return "reports";
  }

  /**
   * POST operation to generate report based on given request input
   * 
   * @param reportRequest
   * @param model
   * @param bindingResult
   * @param session
   * @param request
   * @return
   */
  @SuppressWarnings("finally")
  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public String generate(@Valid @ModelAttribute("reportRequest") HpcReportRequest reportRequest,
      Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request) {

	HpcReportRequestDTO requestDTO = new HpcReportRequestDTO();
    try {
      model.addAttribute("reportRequest", reportRequest);
      String authToken = (String) session.getAttribute("hpcUserToken");
      requestDTO.setType(HpcReportType.fromValue(reportRequest.getReportType()));
      if ( (reportRequest.getDoc() != null && !reportRequest.getDoc().equals("-1")) &&
        (requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC)
            || requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))) {
        requestDTO.getDoc().add(reportRequest.getDoc());
      }
      if (requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATA_OWNER)) {
          requestDTO.setPath("ALL");
      }
      if ((reportRequest.getBasepath() != null && !reportRequest.getBasepath().equals("-1")) &&
        (requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH)
            || requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE))) {
        requestDTO.setPath(reportRequest.getBasepath());
          //Also populate DOC from Model for display purposes
        requestDTO.getDoc().add(HpcClientUtil.getDocByBasePath(getModelDTO(session), reportRequest.getBasepath()));
      }
      else if ( (reportRequest.getPath() != null && !reportRequest.getPath().equals("-1")) &&
        (requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH)
            || requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH_BY_DATE_RANGE))) {
        try {
          HpcClientUtil.getCollection(authToken, hpcCollectionlURL, reportRequest.getPath(), true,
               sslCertPath, sslCertPassword);
        } catch (HpcWebException e) {
          model.addAttribute(ATTR_MESSAGE, "Invalid collection path: " + reportRequest.getPath() +". Please re-enter valid path.");
          return init(model, bindingResult, session);
        }
        requestDTO.setPath(reportRequest.getPath());
      }

      if (reportRequest.getUser() != null && !reportRequest.getUser().equals("-1")) {
        requestDTO.getUser().add(reportRequest.getUser());
      }
      if (reportRequest.getFromDate() != null && !reportRequest.getFromDate().isEmpty()) {
        requestDTO.setFromDate(reportRequest.getFromDate());
      }
      if (reportRequest.getToDate() != null && !reportRequest.getToDate().isEmpty()) {
        requestDTO.setToDate(reportRequest.getToDate());
      }
      WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
      client.header("Authorization", "Bearer " + authToken);

      Response restResponse = client.invoke("POST", requestDTO);
      if (restResponse.getStatus() == 200) {
        MappingJsonFactory factory = new MappingJsonFactory();
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
        HpcReportsDTO reports = parser.readValueAs(HpcReportsDTO.class);
        model.addAttribute("reports", translate(reports.getReports()));
        if ((reportRequest.getReportType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE)
            && reportRequest.getDoc().equals("All")) || (reportRequest.getReportType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE)
                && reportRequest.getBasepath().equals("All"))) {
          model.addAttribute("reportName", getReportName(reportRequest.getReportType() + "_GRID"));
        }
        else {
          model.addAttribute("reportName", getReportName(reportRequest.getReportType()));
        }     
      } else {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        model.addAttribute(ATTR_MESSAGE, "Failed to generate report: " + exception.getMessage());
        logger.info("Failed to generate report {} for {}: {}", requestDTO.getType(), requestDTO.getPath(), exception);
      }
    } catch (HttpStatusCodeException e) {
        model.addAttribute(ATTR_MESSAGE, "Failed to generate report: " + e.getMessage());
        logger.info("Failed to generate report {} for path {}", requestDTO.getType(), requestDTO.getPath(), e);
    } finally {
      return init(model, bindingResult, session);
    }
  }


  private List<HpcReportDTO> translate(List<HpcReportDTO> reports) {
    List<HpcReportDTO> tReports = new ArrayList<>();
    for (HpcReportDTO dto : reports) {
      if(dto.getFromDate() != null) {
        DateTimeFormatter dtoFormat = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
        LocalDate fromDate = LocalDate.parse(dto.getFromDate(), dtoFormat);
        LocalDate toDate = LocalDate.parse(dto.getToDate(), dtoFormat);
        dto.setFromDate(formatter.format(fromDate));
        dto.setToDate(formatter.format(toDate.minusDays(1)));
      }
      List<HpcReportEntryDTO> entries = dto.getReportEntries();
      for (HpcReportEntryDTO entry : entries) {
        if (env.getProperty(entry.getAttribute()) != null) {
          // Data Owner Grid: we are reusing an existing attribute TOTAL_DATA_SIZE, therefore we are using the proper property for this Grid
           if (dto.getType().equals("USAGE_SUMMARY_BY_DATA_OWNER")) {
              if (entry.getAttribute().equals("TOTAL_DATA_SIZE") || dto.getType().equals("LARGEST_FILE_SIZE")) {
                entry.setAttribute(env.getProperty("COLLECTION_SIZE"));
                entry.setValue(MiscUtil.addHumanReadableSize(entry.getValue(), true));
              } else if (entry.getAttribute().equals("LARGEST_FILE_SIZE")) {
                entry.setAttribute(env.getProperty("COLLECTION_SIZE2"));
                entry.setValue(MiscUtil.addHumanReadableSize(entry.getValue(), true));        
              } else {
                entry.setAttribute(env.getProperty(entry.getAttribute()));
              }
           } else {
              entry.setAttribute(env.getProperty(entry.getAttribute()));
              if (entry.getAttribute().equals(env.getProperty("TOTAL_NUM_OF_COLLECTIONS"))) {
            	  entry.setValue(entry.getValue().replaceAll("[\\[\\]{]","").replaceAll("}","<br>"));
              }
              if (entry.getAttribute().equals(env.getProperty("TOTAL_DATA_SIZE"))
                  || entry.getAttribute().equals(env.getProperty("LARGEST_FILE_SIZE"))
                  || entry.getAttribute().equals(env.getProperty("AVERAGE_FILE_SIZE"))){
                  entry.setValue(MiscUtil.addHumanReadableSize(entry.getValue(), true));
              }
           }
        }
      }
      tReports.add(dto);
    }
    return tReports;
  }


  private String getReportName(String type) {
    if (env.getProperty(type) != null)
      return env.getProperty(type);
    else
      return type;
  }
}
