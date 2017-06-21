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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryType;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcSearchUtil;

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
	public String home(@RequestBody(required = false) String body, @RequestParam String queryName, @RequestParam String page, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
		HpcNamedCompoundMetadataQueryDTO query = null;
		try {
			String authToken = (String) session.getAttribute("hpcUserToken");

			query = HpcClientUtil.getQuery(authToken, queryURL, queryName, sslCertPath, sslCertPassword);

			String requestURL;
			if (query != null && query.getNamedCompoundQuery().getCompoundQueryType()
					.equals(HpcCompoundMetadataQueryType.COLLECTION))
				requestURL = compoundCollectionSearchServiceURL + "/" + queryName + "?totalCount=true&page="+page;
			else if (query != null && query.getNamedCompoundQuery().getCompoundQueryType()
					.equals(HpcCompoundMetadataQueryType.DATA_OBJECT))
				requestURL = compoundDataObjectSearchServiceURL + "/" + queryName + "?totalCount=true&page="+page;
			else
				return "dashboard";
			
			session.setAttribute("namedCompoundQuery", query.getNamedCompoundQuery());

			if (query.getNamedCompoundQuery().getDetailedResponse())
				requestURL = requestURL + "&detailedResponse=true";

			WebClient client = HpcClientUtil.getWebClient(requestURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);

			Response restResponse = client.invoke("GET", null);
			if (restResponse.getStatus() == 200) {
				HpcSearch search = new HpcSearch();
				search.setSearchType(query.getNamedCompoundQuery().getCompoundQueryType().value());
				search.setDetailed(query.getNamedCompoundQuery().getDetailedResponse());
				HpcSearchUtil.processResponseResults(search, restResponse, model);
			} else {
				String message = "No matching results!";
				ObjectError error = new ObjectError("hpcSearch", message);
				bindingResult.addError(error);
				model.addAttribute("error", message);
				return "dashboard";
			}
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
}
