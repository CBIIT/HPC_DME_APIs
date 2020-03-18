/**
 * HpcSearchController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonParseException;

import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryType;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.web.model.HpcSaveSearch;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcSearchUtil;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * <p>
 * Controller to execute a search search
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/search")
public class HpcSearchController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.search.collection.compound}")
	private String compoundCollectionSearchServiceURL;
	@Value("${gov.nih.nci.hpc.server.search.dataobject.compound}")
	private String compoundDataObjectSearchServiceURL;
	@Value("${gov.nih.nci.hpc.server.query}")
	private String queryURL;

	/**
	 * GET action to query by saved search name
	 * 
	 * @param body
	 * @param queryName
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String body, @RequestParam String queryName,
			@RequestParam String page, @RequestParam(required = false) String pageSize, Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request) {
		HpcNamedCompoundMetadataQueryDTO query = null;
		HpcSearch search = new HpcSearch();
		HpcSaveSearch hpcSaveSearch = new HpcSaveSearch();
		
		try {
			
			search.setQueryName(queryName);
			hpcSaveSearch.setCriteriaName(queryName);
			search.setPageNumber(Integer.parseInt(page));
			if(StringUtils.isNotEmpty(pageSize))
			  search.setPageSize(Integer.parseInt(pageSize));
			query = processSearch(search, session, request, model, bindingResult);
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		} catch (HttpStatusCodeException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		} catch (RestClientException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		} catch (Exception e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		}

		model.addAttribute("source", "search");
		model.addAttribute("queryName", queryName);
		model.addAttribute("pageNumber", new Integer(page).intValue());
		model.addAttribute("pageSize", search.getPageSize());
		session.setAttribute("hpcSearch", search);
		session.setAttribute("hpcSaveSearch", hpcSaveSearch);
		HpcSearchUtil.cacheSelectedRows(session, request, model);

		if (query == null)
			return "dashboard";
		else if (query.getNamedCompoundQuery().getCompoundQueryType().equals(HpcCompoundMetadataQueryType.COLLECTION)
				&& query.getNamedCompoundQuery().getDetailedResponse())
			return "collectionsearchresultdetail";
		else if (query.getNamedCompoundQuery().getCompoundQueryType().equals(HpcCompoundMetadataQueryType.COLLECTION)
				&& !query.getNamedCompoundQuery().getDetailedResponse())
			return "collectionsearchresult";
		else if (query.getNamedCompoundQuery().getCompoundQueryType().equals(HpcCompoundMetadataQueryType.DATA_OBJECT)
				&& query.getNamedCompoundQuery().getDetailedResponse())
			return "dataobjectsearchresultdetail";
		else if (query.getNamedCompoundQuery().getCompoundQueryType().equals(HpcCompoundMetadataQueryType.DATA_OBJECT)
				&& !query.getNamedCompoundQuery().getDetailedResponse())
			return "dataobjectsearchresult";
		else
			return "dashboard";
	}

	/**
	 * GET action to query by saved search name
	 * 
	 * @param body
	 * @param queryName
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String execute(@Valid @ModelAttribute("hpcSearch") HpcSearch search, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			RedirectAttributes redirectAttrs) {
		HpcNamedCompoundMetadataQueryDTO query = null;

		try {
			query = processSearch(search, session, request, model, bindingResult);
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		} catch (HttpStatusCodeException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		} catch (RestClientException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		} catch (Exception e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search due to: " + e.getMessage());
			return "dashboard";
		}
		HpcSearchUtil.cacheSelectedRows(session, request, model);

		model.addAttribute("source", "search");
		model.addAttribute("queryName", search.getQueryName());
        model.addAttribute("pageNumber", new Integer(search.getPageNumber()).intValue());
		model.addAttribute("pageSize", new Integer(search.getPageSize()).intValue());
		session.setAttribute("hpcSearch", search);

		if (query == null)
			return "dashboard";
		else if (query.getNamedCompoundQuery().getCompoundQueryType().equals(HpcCompoundMetadataQueryType.COLLECTION)
				&& query.getNamedCompoundQuery().getDetailedResponse())
			return "collectionsearchresultdetail";
		else if (query.getNamedCompoundQuery().getCompoundQueryType().equals(HpcCompoundMetadataQueryType.COLLECTION)
				&& !query.getNamedCompoundQuery().getDetailedResponse())
			return "collectionsearchresult";
		else if (query.getNamedCompoundQuery().getCompoundQueryType().equals(HpcCompoundMetadataQueryType.DATA_OBJECT)
				&& query.getNamedCompoundQuery().getDetailedResponse())
			return "dataobjectsearchresultdetail";
		else if (query.getNamedCompoundQuery().getCompoundQueryType().equals(HpcCompoundMetadataQueryType.DATA_OBJECT)
				&& !query.getNamedCompoundQuery().getDetailedResponse())
			return "dataobjectsearchresult";
		else
			return "dashboard";
	}

	private HpcNamedCompoundMetadataQueryDTO processSearch(HpcSearch search, HttpSession session,
			HttpServletRequest request, Model model, BindingResult bindingResult)
			throws JsonParseException, IOException {
		HpcNamedCompoundMetadataQueryDTO query = null;
		String authToken = (String) session.getAttribute("hpcUserToken");

		query = HpcClientUtil.getQuery(authToken, queryURL, search.getQueryName(), sslCertPath, sslCertPassword);
		HpcSearchUtil.cacheSelectedRows(session, request, model);

    UriComponentsBuilder ucBuilder = null;
    if (null != query && null != query.getNamedCompoundQuery()) {
      final HpcCompoundMetadataQueryType cqType = query.getNamedCompoundQuery()
          .getCompoundQueryType();
      if (HpcCompoundMetadataQueryType.COLLECTION.equals(cqType)) {
        ucBuilder = UriComponentsBuilder.fromHttpUrl(
            this.compoundCollectionSearchServiceURL);
      } else if (HpcCompoundMetadataQueryType.DATA_OBJECT.equals(cqType)) {
        ucBuilder = UriComponentsBuilder.fromHttpUrl(
            this.compoundDataObjectSearchServiceURL);
      }
    }

    if (null == ucBuilder) {
      return null;
    }

    session.setAttribute("namedCompoundQuery", query.getNamedCompoundQuery());

    ucBuilder.pathSegment(search.getQueryName()).queryParam("totalCount",
      Boolean.TRUE).queryParam("page", Integer.valueOf(search.getPageNumber())).queryParam("pageSize", Integer.valueOf(search.getPageSize()));

    if (query.getNamedCompoundQuery().getDetailedResponse()) {
      ucBuilder.queryParam("detailedResponse", Boolean.TRUE);
    }

    final String requestURL = ucBuilder.build().encode().toUri().toURL().toExternalForm();

    WebClient client = HpcClientUtil.getWebClient(requestURL, sslCertPath, sslCertPassword);
		client.header("Authorization", "Bearer " + authToken);

		Response restResponse = client.invoke("GET", null);
		if (restResponse.getStatus() == 200) {
			search.setSearchType(query.getNamedCompoundQuery().getCompoundQueryType().value());
			search.setDetailed(query.getNamedCompoundQuery().getDetailedResponse());
			HpcSearchUtil.processResponseResults(search, restResponse, model, session);
		} else {
			String message = "No matching results!";
			ObjectError error = new ObjectError("hpcSearch", message);
			bindingResult.addError(error);
			model.addAttribute("error", message);
			return null;
		}
		return query;
	}
}
