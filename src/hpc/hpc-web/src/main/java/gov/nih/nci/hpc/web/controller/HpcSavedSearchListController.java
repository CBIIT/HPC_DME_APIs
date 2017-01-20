/**
 * HpcSearchProjectController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

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
import gov.nih.nci.hpc.dto.datamanagement.HpcNamedCompoundMetadataQueryListDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcQuery;
import gov.nih.nci.hpc.web.model.HpcSaveSearch;
import gov.nih.nci.hpc.web.model.HpcSavedQueries;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * HPC DM Saved Search controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcQuerySavedSearchController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/savedSearchList")
public class HpcSavedSearchListController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.query}")
	private String queryServiceURL;

	@JsonView(Views.Public.class)
	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public List<AjaxResponseBody>  get(@Valid @ModelAttribute("hpcSaveSearch") HpcSaveSearch search, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		String userPasswdToken = (String) session.getAttribute("userpasstoken");
		List<AjaxResponseBody> result = new ArrayList<AjaxResponseBody>();
		try {
			HpcNamedCompoundMetadataQueryListDTO queries = 
					HpcClientUtil.getSavedSearches(userPasswdToken, queryServiceURL, sslCertPath, sslCertPassword);
			if(queries == null || queries.getNamedCompoundQueries() == null || queries.getNamedCompoundQueries().size() == 0)
			{
				AjaxResponseBody body = new AjaxResponseBody();
				body.setMessage("No Saved Searches");
				result.add(body);
			}
			else
			{
				for(HpcNamedCompoundMetadataQuery query : queries.getNamedCompoundQueries())
				{
					AjaxResponseBody body = new AjaxResponseBody();
					body.setMessage(query.getName());
					result.add(body);
				}
			}
			//savedQueries.setQueries(result);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return result;
		}
	}
}
