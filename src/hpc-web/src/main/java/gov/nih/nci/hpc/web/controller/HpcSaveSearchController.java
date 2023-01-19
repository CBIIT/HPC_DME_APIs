/**
 * HpcSaveSearchController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.dto.datasearch.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcSaveSearch;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * <p>
 * Controller to save search criteria by a name
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/savesearch")
public class HpcSaveSearchController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.query}")
	private String queryServiceURL;
	@Value("${gov.nih.nci.hpc.server.query}")
	private String queryURL;
	private String hpcMetadataAttrsURL;

	/**
	 * GET action to prepare save search page
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
		HpcSaveSearch hpcSaveSearch = (HpcSaveSearch) session.getAttribute("hpcSaveSearch");
		if(hpcSaveSearch == null)
			hpcSaveSearch = new HpcSaveSearch();
		String queryName = request.getParameter("queryName");
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/login";
		}
		if(StringUtils.isNotBlank(queryName)) {
			HpcNamedCompoundMetadataQueryDTO query = null;
			query = HpcClientUtil.getQuery(authToken, queryURL, queryName, sslCertPath, sslCertPassword);
			session.setAttribute("namedCompoundQuery", query.getNamedCompoundQuery());
			hpcSaveSearch.setCriteriaName(query.getNamedCompoundQuery().getName());
			hpcSaveSearch.getSelectedColumns().clear();
			hpcSaveSearch.getSelectedColumns().addAll(query.getNamedCompoundQuery().getSelectedColumns());
			hpcSaveSearch.setFrequency(query.getNamedCompoundQuery().getFrequency());
		}
		model.addAttribute("hpcSaveSearch", hpcSaveSearch);
		session.setAttribute("hpcSaveSearch", hpcSaveSearch);
		return "savesearch";
	}

	/**
	 * POST action to save search criteria by name. Last executed search
	 * criteria is pulled from user session to save. Only "-" and "_" are
	 * allowed as special chars in search
	 * 
	 * @param search
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@JsonView(Views.Public.class)
	@RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public AjaxResponseBody search(@Valid @ModelAttribute("hpcSaveSearch") HpcSaveSearch search, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
		
		HpcSaveSearch hpcPrevSaveSearch = (HpcSaveSearch) session.getAttribute("hpcSaveSearch");
		
		AjaxResponseBody result = new AjaxResponseBody();
		try {
			HpcCompoundMetadataQueryDTO compoundQuery = null;
			if (session.getAttribute("compoundQuery") != null)
				compoundQuery = (HpcCompoundMetadataQueryDTO) session.getAttribute("compoundQuery");

			if (compoundQuery == null) {
				HpcNamedCompoundMetadataQuery namedCompoundQuery = null;
				if (session.getAttribute("namedCompoundQuery") != null)
					namedCompoundQuery = (HpcNamedCompoundMetadataQuery) session.getAttribute("namedCompoundQuery");

				if (namedCompoundQuery == null) {
					result.setCode("400");
					result.setMessage("Invalid Search");
					return result;
				}
				compoundQuery = new HpcCompoundMetadataQueryDTO();
				compoundQuery.setCompoundQuery(namedCompoundQuery.getCompoundQuery());
				compoundQuery.setCompoundQueryType(namedCompoundQuery.getCompoundQueryType());
				compoundQuery.setDetailedResponse(namedCompoundQuery.getDetailedResponse());
				compoundQuery.setTotalCount(namedCompoundQuery.getTotalCount());
			}
			compoundQuery.getSelectedColumns().clear();
			compoundQuery.getSelectedColumns().addAll(search.getSelectedColumns());
			compoundQuery.setFrequency(search.getFrequency());

			if (search.getCriteriaName() == null || search.getCriteriaName().isEmpty()) {
				result.setCode("400");
				result.setMessage("Invalid criteria name");
				return result;
			}

			String authToken = (String) session.getAttribute("hpcUserToken");
			//If previous criteria name is provided, delete the saved criteria to re-save the new query
			if(hpcPrevSaveSearch != null && StringUtils.equals(hpcPrevSaveSearch.getCriteriaName(), search.getCriteriaName())) {
				boolean deleted = HpcClientUtil.deleteSearch(authToken, queryServiceURL, search.getCriteriaName(), sslCertPath,
						sslCertPassword);
				if (!deleted) {
					result.setMessage("Failed to save criteria! Reason: Failed to delete existing criteria");
					return result;
				}
			}
			
			final String serviceURL = UriComponentsBuilder.fromHttpUrl(
        this.queryServiceURL).pathSegment(search.getCriteriaName()).build()
				.encode().toUri().toURL().toExternalForm();
			WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);

			Response restResponse = client.invoke("PUT", compoundQuery);
			if (restResponse.getStatus() == 201) {
				result.setCode("201");
				result.setMessage("Saved criteria successfully!");
				HpcSaveSearch hpcSaveSearch = new HpcSaveSearch();
				hpcSaveSearch.setCriteriaName(search.getCriteriaName());
				session.setAttribute("hpcSaveSearch", hpcSaveSearch);
				return result;
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
				result.setCode("400");
				result.setMessage("Failed to save criteria! Reason: " + exception.getMessage());
				return result;
			}
		} catch (HttpStatusCodeException e) {
			result.setCode("400");
			result.setMessage("Failed to save criteria: " + e.getMessage());
			return result;
		} catch (RestClientException e) {
			result.setCode("400");
			result.setMessage("Failed to save criteria: " + e.getMessage());
			return result;
		} catch (Exception e) {
			result.setCode("400");
			result.setMessage("Failed to save criteria: " + e.getMessage());
			return result;
		}
	}
}
