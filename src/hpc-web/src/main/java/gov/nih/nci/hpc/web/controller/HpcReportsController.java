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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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
import gov.nih.nci.hpc.domain.report.HpcReportEntryAttribute;
import gov.nih.nci.hpc.dto.report.HpcReportRequestDTO;
import gov.nih.nci.hpc.dto.report.HpcReportsDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.dto.security.HpcUserListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserListEntry;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcReportRequest;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.MiscUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import org.springframework.web.util.UriComponentsBuilder;


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
  private Gson gson = new Gson();

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
    // Based on the role, the user can see their docs or all docs
    if (user.getUserRole().equals("GROUP_ADMIN") || user.getUserRole().equals("USER")) {
      model.addAttribute("canSeeAllDocs", false);
    } else {
       model.addAttribute("canSeeAllDocs", true);
    }
    // Populate Docs
    List<String> docs = new ArrayList<>();
    if (user.getUserRole().equals("GROUP_ADMIN") || user.getUserRole().equals("USER")) {
      docs.add(user.getDoc());
      model.addAttribute("docs", docs);
    } else if (user.getUserRole().equals("SYSTEM_ADMIN")) {
      if(!model.containsAttribute("docs")) {
        docs.addAll(HpcClientUtil.getDOCs(authToken, hpcModelURL, sslCertPath, sslCertPassword, session));
        docs.sort(String.CASE_INSENSITIVE_ORDER);
        model.addAttribute("docs", docs);
      }
    }
    // Populate Basepaths
    if (!model.containsAttribute("basepaths")) {
      List<String> basepaths = new ArrayList<>();
      for (HpcDocDataManagementRulesDTO docRule : getModelDTO(session).getDocRules()) {
        if (docs.contains(docRule.getDoc())){
          for (HpcDataManagementRulesDTO rule : docRule.getRules()) {
            basepaths.add(rule.getBasePath());
          }
        }
      }
      basepaths.sort(String.CASE_INSENSITIVE_ORDER);
      model.addAttribute("basepaths", basepaths);
    }
   return "reports";
  }
  
  private HpcReportRequestDTO setReportColumnsForIndividualReports(HpcReportRequestDTO requestDTO, boolean showArchiveSummaryForNonGrid) {
    if (!requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH)
            && !requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH_BY_DATE_RANGE)) {
      requestDTO.getReportColumns().add(HpcReportEntryAttribute.TOTAL_NUM_OF_REGISTERED_USERS);
    }
    requestDTO.getReportColumns().add(HpcReportEntryAttribute.TOTAL_DATA_SIZE);
    requestDTO.getReportColumns().add(HpcReportEntryAttribute.LARGEST_FILE_SIZE);
    requestDTO.getReportColumns().add(HpcReportEntryAttribute.AVERAGE_FILE_SIZE);
    requestDTO.getReportColumns().add(HpcReportEntryAttribute.TOTAL_NUM_OF_DATA_OBJECTS);
    requestDTO.getReportColumns().add(HpcReportEntryAttribute.TOTAL_NUM_OF_COLLECTIONS);
    requestDTO.getReportColumns().add(HpcReportEntryAttribute.FILE_SIZES);
    if (showArchiveSummaryForNonGrid){
      requestDTO.getReportColumns().add(HpcReportEntryAttribute.ARCHIVE_SUMMARY);
    }
    return requestDTO;
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
      HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
      requestDTO.setType(HpcReportType.fromValue(reportRequest.getReportType()));
      if ( (reportRequest.getDoc() != null && !reportRequest.getDoc().equals("-1")) &&
        (requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC)
            || requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))) {
        requestDTO.getDoc().add(reportRequest.getDoc());
        if (!(reportRequest.getDoc().toString().equals("All"))){
          requestDTO = setReportColumnsForIndividualReports(requestDTO, reportRequest.getShowArchiveSummary());
        }
      }
      if (requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATA_OWNER)) {
          requestDTO.setPath("ALL");
      }
      if (requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE)) {
          requestDTO = setReportColumnsForIndividualReports(requestDTO, reportRequest.getShowArchiveSummary());
      }
      if ((reportRequest.getBasepath() != null && !reportRequest.getBasepath().equals("-1")) &&
        (requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH)
            || requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE))) {
        requestDTO.setPath(reportRequest.getBasepath());
        if (!reportRequest.getBasepath().toString().equals("All")){
          requestDTO = setReportColumnsForIndividualReports(requestDTO, reportRequest.getShowArchiveSummary());
        }
        //Also populate DOC from Model for display purposes
        requestDTO.getDoc().add(HpcClientUtil.getDocByBasePath(getModelDTO(session), reportRequest.getBasepath()));
      }
      else if ( (reportRequest.getPath() != null && !reportRequest.getPath().equals("-1")) &&
        (requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH)
            || requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH_BY_DATE_RANGE))) {
        requestDTO = setReportColumnsForIndividualReports(requestDTO, reportRequest.getShowArchiveSummary());
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
        if ((requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATA_OWNER))) {
          model.addAttribute("reports", translateDataOwnerReports(reports.getReports()));
        } else {
          model.addAttribute("reports", translate(reports.getReports()));
        }
        if ((requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE)
            && reportRequest.getDoc().equals("All")) || (requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE)
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
      // The list of docs for the UI dropdown is repopulated with the docs from hidden variables in request.
      // This is to prevent another query for retrieving the list of docs
      if (user.getUserRole().equals("SYSTEM_ADMIN")){
        model.addAttribute("docs", reportRequest.getDocs());
      }
      // The list of basepaths for the UI dropdown is repopulated with the basepaths from hidden variables in request.
      // This is to prevent another query for retrieving the list of basepaths
     model.addAttribute("basepaths", reportRequest.getBasepaths());

    } catch (HttpStatusCodeException e) {
        model.addAttribute(ATTR_MESSAGE, "Failed to generate report: " + e.getMessage());
        logger.info("Failed to generate report {} for path {}", requestDTO.getType(), requestDTO.getPath(), e);
    } finally {
      return init(model, bindingResult, session);
    }
  }

  private class HpcArchiveSummaryReport {
    String doc;
    String vault;
    String bucket;
    long count;
    long size;
  }

  private HpcReportEntryDTO setArchiveSummary(HpcReportEntryDTO entry, String separator){
    if (entry.getValue().equals("0")) {
      entry.setValue("NA");
    } else {
      Type empMapType = new TypeToken<List<HpcArchiveSummaryReport>>() {}.getType();
      List<HpcArchiveSummaryReport> assaySummaryList = gson.fromJson(entry.getValue(), empMapType);
      String assaySummaryString = "";
      for(int i=0 ; i < assaySummaryList.size(); i++) {
        String size = String.valueOf(assaySummaryList.get(i).size);
        String hrSize = MiscUtil.getHumanReadableSize(size , true);
        assaySummaryString = assaySummaryString + assaySummaryList.get(i).vault + ", " +
            assaySummaryList.get(i).bucket + ", " +
            hrSize + separator;
      }
      entry.setValue(assaySummaryString);
    }
    return entry;
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
      // Loop through to add the entries for the Human Readable fields
      int index = 0;
      int total_data_size_index = 0;
      int largest_file_size_index = 0;
      String total_data_size = "";
      String largest_file_size = "";
      for (HpcReportEntryDTO entry : entries) {
        if (env.getProperty(entry.getAttribute()) != null) {
          entry.setAttribute(env.getProperty(entry.getAttribute()));
          if (entry.getAttribute().equals(env.getProperty("TOTAL_NUM_OF_COLLECTIONS"))) {
              entry.setValue(entry.getValue().replaceAll("[\\[\\]{]","").replaceAll("}","<br/>"));
          }
          if (entry.getAttribute().equals(env.getProperty("ARCHIVE_SUMMARY"))) {
            entry = setArchiveSummary(entry,  " <br/>");
          }
          if ((dto.getType().equals("USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE") ||
              dto.getType().equals("USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE")) && reports.size() > 1 ){
            // In Grid Reports, the TOTAL_DATA_SIZE contains only the Byte value
            if (entry.getAttribute().equals(env.getProperty("TOTAL_DATA_SIZE"))) {
              total_data_size_index = index;
              total_data_size = String.format("%.2f", Double.parseDouble(entry.getValue()));
              entry.setAttribute(env.getProperty("TOTAL_DATA_SIZE_HUMAN_READABLE_FOR_GRID"));
              entry.setValue(MiscUtil.getHumanReadableSize(entry.getValue(), true));
            }
            // In Grid Reports, the LARGEST_FILE_SIZE contains only the Byte value
            if (entry.getAttribute().equals(env.getProperty("LARGEST_FILE_SIZE"))) {
              largest_file_size_index = index;
              largest_file_size = String.format("%.2f", Double.parseDouble(entry.getValue()));
              entry.setAttribute(env.getProperty("LARGEST_FILE_SIZE_HUMAN_READABLE_FOR_GRID"));
              entry.setValue(MiscUtil.getHumanReadableSize(entry.getValue(), true));
            }
          } else {
            // For rest of the Single Reports, the value displayed is always a human readable value.
            if (entry.getAttribute().equals(env.getProperty("TOTAL_DATA_SIZE"))
                  || entry.getAttribute().equals(env.getProperty("LARGEST_FILE_SIZE"))
                  || entry.getAttribute().equals(env.getProperty("AVERAGE_FILE_SIZE"))) {
                  entry.setValue(MiscUtil.addHumanReadableSize(entry.getValue(), true));
            }
          }
        }
        index = index + 1;
      }
      if ((dto.getType().equals("USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE") ||
        dto.getType().equals("USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE")) && reports.size() > 1  ){
        HpcReportEntryDTO newEntry = new HpcReportEntryDTO();
        newEntry.setAttribute(env.getProperty("TOTAL_DATA_SIZE_VALUE_ONLY_FOR_GRID"));
        newEntry.setValue(total_data_size);
        dto.getReportEntries().add(total_data_size_index, newEntry);
        newEntry = new HpcReportEntryDTO();
        newEntry.setAttribute(env.getProperty("LARGEST_FILE_SIZE_VALUE_ONLY_FOR_GRID"));
        newEntry.setValue(largest_file_size);
        dto.getReportEntries().add(largest_file_size_index + 1, newEntry);
        newEntry = new HpcReportEntryDTO();
        newEntry.setAttribute(env.getProperty("ARCHIVE_SUMMARY"));
        newEntry.setValue("0");
        dto.getReportEntries().add(newEntry);
        newEntry = new HpcReportEntryDTO();
        newEntry.setAttribute(env.getProperty("ARCHIVE_SUMMARY_VALUES"));
        newEntry.setValue("");
        dto.getReportEntries().add(newEntry);
      }
      tReports.add(dto);
    }
    return tReports;
  }

  private List<HpcReportDTO> translateDataOwnerReports(List<HpcReportDTO> reports) {
    List<HpcReportDTO> tReports = new ArrayList<>();
    boolean firstReport = true;
    String collectionSizeProp = env.getProperty("COLLECTION_SIZE");
    String collectionSizeHrProp = env.getProperty("COLLECTION_SIZE_HUMAN_READABLE");
    String archSummaryProp = env.getProperty("ARCHIVE_SUMMARY");
    String archSummaryValuesProp = env.getProperty("ARCHIVE_SUMMARY_VALUES");
    String[] headerList = new String[reports.size()];
    for (HpcReportDTO dto : reports) {
      List<HpcReportEntryDTO> entries = dto.getReportEntries();
      // Loop through to add the entries for the Human Readable fields
      int index = 0;
      int total_data_size_index = 0;
      String total_data_size = "";
      for (HpcReportEntryDTO entry : entries) {
        String entryAttributeRaw = entry.getAttribute();
        if(firstReport){
          String entryAttribute = env.getProperty(entry.getAttribute());
          headerList[index] = entryAttribute;
          if(entryAttribute == null) {
            continue;
          }
          entry.setAttribute(entryAttribute);
        } else {
          if (headerList[index] != null) {
            entry.setAttribute(headerList[index]);
          } else {
            continue;
          }
        }
        if (entryAttributeRaw.equals("TOTAL_DATA_SIZE")) {
          total_data_size_index = index;
          total_data_size = String.format("%.2f", Double.parseDouble(entry.getValue()));
          entry.setAttribute(collectionSizeHrProp);
          entry.setValue(MiscUtil.getHumanReadableSize(entry.getValue(), true));
        }
        index = index + 1;
      }
      // A new column will be added to the Data Owner grid next to the Collection size column showing the Human readable value
      HpcReportEntryDTO newEntry = new HpcReportEntryDTO();
      newEntry.setAttribute(collectionSizeProp);
      newEntry.setValue(total_data_size);
      dto.getReportEntries().add(total_data_size_index, newEntry);
      newEntry = new HpcReportEntryDTO();
      newEntry.setAttribute(archSummaryProp);
      newEntry.setValue("0");
      dto.getReportEntries().add(newEntry);
      newEntry = new HpcReportEntryDTO();
      newEntry.setAttribute(env.getProperty(archSummaryValuesProp));
      newEntry.setValue("");
      dto.getReportEntries().add(newEntry);
      tReports.add(dto);
      if (firstReport) {
        firstReport = false;
      }
    }
    return tReports;
  }

  private String getReportName(String type) {
    if (env.getProperty(type) != null)
      return env.getProperty(type);
    else
      return type;
  }

  /**
    * GET operation responding to an AJAX request to retrieve the Archive Summary based on the path
    *
    * @param path
    * @param session
    * @param request
    * @return The Archive summary value
  */
  @GetMapping(value = "/getArchiveSummary")
  @ResponseBody
  public AjaxResponseBody getArchiveSummary(@RequestParam("path") String path, @RequestParam("reportType") String reportType, @RequestParam("fromDate") String fromDate,  @RequestParam("toDate") String toDate, HttpSession session, HttpServletRequest request) {
      String authToken = (String) session.getAttribute("hpcUserToken");
      AjaxResponseBody result = new AjaxResponseBody();
      HpcReportRequestDTO requestDTO = new HpcReportRequestDTO();
      switch (reportType) {
            case "USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE":
                    requestDTO.setType(HpcReportType.USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE);
                    requestDTO.setPath(path);
                    requestDTO.setFromDate(fromDate);
                    requestDTO.setToDate(toDate);
                    break;
            case "USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE":
                    requestDTO.setType(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE);
                    requestDTO.getDoc().add(path);
                    requestDTO.setFromDate(fromDate);
                    requestDTO.setToDate(toDate);
                    break;
            case "USAGE_SUMMARY_BY_DATA_OWNER":
                    requestDTO.setType(HpcReportType.USAGE_SUMMARY_BY_PATH);
                    requestDTO.setPath(path);
                    break;
      }
      requestDTO.getReportColumns().add(HpcReportEntryAttribute.ARCHIVE_SUMMARY);
      try {
        UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(serviceURL);
        if (ucBuilder == null) {
          return null;
        }
        final String requestURL = ucBuilder.build().encode().toUri().toURL().toExternalForm();
        WebClient client = HpcClientUtil.getWebClient(requestURL, sslCertPath, sslCertPassword);
        client.header("Authorization", "Bearer " + authToken);
        Response restResponse = client.invoke("POST", requestDTO);
        if (restResponse.getStatus() == 200) {
          result.setCode(Integer.toString(200));
          MappingJsonFactory factory = new MappingJsonFactory();
          JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
          HpcReportsDTO reports = parser.readValueAs(HpcReportsDTO.class);
          List <HpcReportDTO> dto = reports.getReports();
          List<HpcReportEntryDTO> entries = dto.get(0).getReportEntries();
          for (HpcReportEntryDTO entry : entries) {
            if (env.getProperty(entry.getAttribute()) != null) {
              if (entry.getAttribute().equals("ARCHIVE_SUMMARY")) {
                 // setArchiveSummary(entry, ",");
                  entry = setArchiveSummary(entry,  "    <br/>");
                  result.setMessage(entry.getValue());
                  break;
              }
            }
          }
        } else { // restResponse.getStatus() != 200
            result.setMessage("");
            result.setCode(Integer.toString(400));
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
      return result;
    }

private class HpcUserLite {
    String userId;
    String displayName;
  }

 /**
    * GET operation responding to an AJAX request to retrieve all the Users
    * @param session
    * @param request
    * @return result with serialized list of users
  */
  @GetMapping(value = "/getUsers")
  @ResponseBody
  public AjaxResponseBody getUsers(HttpSession session, HttpServletRequest request) {
      String authToken = (String) session.getAttribute("hpcUserToken");
      HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
      HpcUserListDTO users = HpcClientUtil.getUsers(authToken, activeUsersServiceURL, null, null,
          null, user.getUserRole().equals("SYSTEM_ADMIN") ? null : user.getDoc(), sslCertPath,
          sslCertPassword);
      Comparator<HpcUserListEntry> firstLastComparator = Comparator.comparing(HpcUserListEntry::getFirstName, String.CASE_INSENSITIVE_ORDER)
          .thenComparing(HpcUserListEntry::getLastName, String.CASE_INSENSITIVE_ORDER);
      users.getUsers().sort(firstLastComparator);

      List<HpcUserLite> userLitelist = new ArrayList<>();
      for (HpcUserListEntry userx : users.getUsers()) {
        HpcUserLite u = new HpcUserLite();
        u.userId = userx.getUserId();
        u.displayName = userx.getFirstName() + ' ' + userx.getLastName();
        userLitelist.add(u);
      }
      AjaxResponseBody result = new AjaxResponseBody();
      result.setMessage(gson.toJson(userLitelist));
      logger.info(gson.toJson(userLitelist));
      return result;
    }

  }
