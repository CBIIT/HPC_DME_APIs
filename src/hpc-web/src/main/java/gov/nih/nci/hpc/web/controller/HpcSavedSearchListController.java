/**
 * HpcSavedSearchListController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.annotation.JsonView;

import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcNamedQuery;
import gov.nih.nci.hpc.web.model.HpcSaveSearch;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Controller to get list of saved search names. This list is displayed on
 * Dashboard
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/savedSearchList")
public class HpcSavedSearchListController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.query}")
	private String queryServiceURL;
	
	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	/**
	 * GET action to query user saved searches
	 * 
	 * @param search
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@JsonView(Views.Public.class)
	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public List<HpcNamedQuery> get(@Valid @ModelAttribute("hpcSaveSearch") HpcSaveSearch search, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		String userId = (String) session.getAttribute("hpcUserId");
		List<HpcNamedQuery> result = new ArrayList<HpcNamedQuery>();
		try {
			HpcNamedCompoundMetadataQueryListDTO queries = HpcClientUtil.getSavedSearches(authToken, queryServiceURL,
					sslCertPath, sslCertPassword);
			if (queries != null && queries.getNamedCompoundQueries() != null
					&& queries.getNamedCompoundQueries().size() > 0) {
				SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm");
				for (HpcNamedCompoundMetadataQuery query : queries.getNamedCompoundQueries()) {
					HpcNamedQuery namedQuery = new HpcNamedQuery();
					namedQuery.setSearchName(URLEncoder.encode(query.getName(), "UTF-8"));
					namedQuery.setSearchType(query.getCompoundQueryType().value());
					namedQuery.setCreatedOn(format.format(query.getCreated().getTime()));
					namedQuery.setDelete(URLEncoder.encode(query.getName(), "UTF-8"));
					namedQuery.setEdit(URLEncoder.encode(query.getName(), "UTF-8"));
					result.add(namedQuery);
				}
			}
			return result;
		} catch (Exception e) {
			logger.error("Unable to retrieve saved searches for user " + userId, e);
			return result;
		}
	}
}
