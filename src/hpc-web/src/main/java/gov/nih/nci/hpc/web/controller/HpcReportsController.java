/**
 * HpcReportsController.java
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
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
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementDocListDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.report.HpcReportDTO;
import gov.nih.nci.hpc.dto.report.HpcReportEntryDTO;
import gov.nih.nci.hpc.dto.report.HpcReportRequestDTO;
import gov.nih.nci.hpc.dto.report.HpcReportsDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.dto.security.HpcUserListDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcReportRequest;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

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
	@Value("${gov.nih.nci.hpc.server.docs}")
	private String docsServiceURL;

	@Autowired
	private Environment env;

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
	public String home(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		return init(model, bindingResult, session, request);
	}

	private String init(Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
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
		model.addAttribute("reportRequest", new HpcReportRequest());
		HpcUserListDTO users = HpcClientUtil.getUsers(authToken, activeUsersServiceURL, null, null, null,
				user.getUserRole().equals("SYSTEM_ADMIN") ? null : user.getDoc(), sslCertPath, sslCertPassword);
		model.addAttribute("docUsers", users.getUsers());
		List<String> docs = new ArrayList<String>();
		if (user.getUserRole().equals("GROUP_ADMIN") || user.getUserRole().equals("USER"))
			docs.add(user.getDoc());
		else if (user.getUserRole().equals("SYSTEM_ADMIN")) {
			HpcDataManagementDocListDTO dto = HpcClientUtil.getDOCs(authToken, docsServiceURL, sslCertPath,
					sslCertPassword);
			if (dto != null)
				docs.addAll(dto.getDocs());
		}
		model.addAttribute("docs", docs);
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
	@RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String generate(@Valid @ModelAttribute("reportRequest") HpcReportRequest reportRequest, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
		try {
			HpcReportRequestDTO requestDTO = new HpcReportRequestDTO();
			requestDTO.setType(HpcReportType.fromValue(reportRequest.getReportType()));
			if (reportRequest.getDoc() != null && !reportRequest.getDoc().equals("-1")) {
				if (requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC)
						|| requestDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
					requestDTO.getDoc().add(reportRequest.getDoc());
			}
			if (reportRequest.getUser() != null && !reportRequest.getUser().equals("-1"))
				requestDTO.getUser().add(reportRequest.getUser());
			if (reportRequest.getFromDate() != null && !reportRequest.getFromDate().isEmpty())
				requestDTO.setFromDate(reportRequest.getFromDate());
			if (reportRequest.getToDate() != null && !reportRequest.getToDate().isEmpty())
				requestDTO.setToDate(reportRequest.getToDate());

			String authToken = (String) session.getAttribute("hpcUserToken");

			WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);

			Response restResponse = client.invoke("POST", requestDTO);
			if (restResponse.getStatus() == 200) {
				MappingJsonFactory factory = new MappingJsonFactory();
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
				HpcReportsDTO reports = parser.readValueAs(HpcReportsDTO.class);
				model.addAttribute("reports", translate(reports.getReports()));
				model.addAttribute("reportName", getReportName(reportRequest.getReportType()));
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
				model.addAttribute("message", "Failed to generate report: " + exception.getMessage());
				System.out.println(exception);
			}
		} catch (HttpStatusCodeException e) {
			model.addAttribute("message", "Failed to generate report: " + e.getMessage());
		} catch (RestClientException e) {
			model.addAttribute("message", "Failed to generate report: " + e.getMessage());
		} catch (Exception e) {
			model.addAttribute("message", "Failed to generate report: " + e.getMessage());
		} finally {
			return init(model, bindingResult, session, request);
		}
	}

	private List<HpcReportDTO> translate(List<HpcReportDTO> reports) {
		List<HpcReportDTO> tReports = new ArrayList<HpcReportDTO>();
		for (HpcReportDTO dto : reports) {
			List<HpcReportEntryDTO> entries = dto.getReportEntries();
			for (HpcReportEntryDTO entry : entries) {
				if (env.getProperty(entry.getAttribute()) != null)
					entry.setAttribute(env.getProperty(entry.getAttribute()));
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
