/**
 * HpcReviewController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/blob/master/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryLevelFilter;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.domain.review.HpcReviewEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.review.HpcReviewDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcReviewModel;
import gov.nih.nci.hpc.web.model.HpcReviewSearchResult;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcIdentityUtil;

/**
 * <p>
 * Controller to manage annual review
 * </p>
 *
 * @author <a href="mailto:Yuri.Dinh@nih.gov">Yuri Dinh</a>
 * @version $Id: HpcReviewController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/review")
public class HpcReviewController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server}")
	private String serverURL;
	@Value("${gov.nih.nci.hpc.server.user}")
	private String serviceUserURL;
	@Value("${gov.nih.nci.hpc.server.search.collection.compound}")
	private String compoundCollectionSearchServiceURL;
	@Value("${gov.nih.nci.hpc.server.review.query}")
	private String reviewSearchServiceURL;
	@Value("${gov.nih.nci.hpc.server.review.reminder}")
	private String reviewReminderServiceURL;
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionServiceURL;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	@GetMapping
	public String home(@RequestBody(required = false) String body, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
		}
		final String authToken = (String) session.getAttribute("hpcUserToken");
		final String userId = (String) session.getAttribute("hpcUserId");
		log.info("userId: {}", userId);

		return "review";
	}

	@GetMapping(value = "/search")
	public ResponseEntity<?> search(HttpSession session, @RequestHeader HttpHeaders headers, HttpServletRequest request,
			@RequestParam(required = false) String displayAll) throws HpcWebException {

		String authToken = (String) session.getAttribute("hpcUserToken");
		final String userId = (String) session.getAttribute("hpcUserId");
		
		boolean includeCompleted = displayAll != null && displayAll.equals("true");
		List<HpcReviewSearchResult> results = new ArrayList<>();

		try {

			UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(reviewSearchServiceURL);

			if (ucBuilder == null) {
				return null;
			}
			
			if(!includeCompleted)
				ucBuilder.queryParam("projectStatus", "Active");
			if(!HpcIdentityUtil.isUserSystemAdmin(session))
				ucBuilder.queryParam("dataCurator", userId);

			final String requestURL = ucBuilder.build().encode().toUri().toURL().toExternalForm();

			WebClient client = HpcClientUtil.getWebClient(requestURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);
			Response restResponse = client.invoke("POST", null);

			if (restResponse.getStatus() == 200) {
				results = processResponseResults(restResponse);
				return new ResponseEntity<>(results, HttpStatus.OK);
			} else if (restResponse.getStatus() == 204) {
				return new ResponseEntity<>(results, HttpStatus.OK);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);

		}
		return new ResponseEntity<>(results, HttpStatus.NO_CONTENT);

	}
	
	@GetMapping(value = "/searchCompoundQuery")
	public ResponseEntity<?> searchCompoundQuery(HttpSession session, @RequestHeader HttpHeaders headers, HttpServletRequest request,
			HpcSearch search) throws HpcWebException {

		String authToken = (String) session.getAttribute("hpcUserToken");

		List<HpcReviewSearchResult> results = new ArrayList<>();

		try {
			HpcCompoundMetadataQueryDTO compoundQuery = constructCriteria(search);
			compoundQuery.setDetailedResponse(true);
			log.info("search compund query" + compoundQuery);

			UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(compoundCollectionSearchServiceURL);

			if (ucBuilder == null) {
				return null;
			}

			
			final String requestURL = ucBuilder.build().encode().toUri().toURL().toExternalForm();

			WebClient client = HpcClientUtil.getWebClient(requestURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);
			Response restResponse = client.invoke("POST", compoundQuery);

			if (restResponse.getStatus() == 200) {
				results = processResponseResults(restResponse);
				return new ResponseEntity<>(results, HttpStatus.OK);
			} else if (restResponse.getStatus() == 204) {
				return new ResponseEntity<>(results, HttpStatus.OK);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);

		}
		return new ResponseEntity<>(results, HttpStatus.NO_CONTENT);

	}

	@PostMapping(value = "/update")
	@ResponseBody
	public AjaxResponseBody updateReview(@Valid @ModelAttribute("hpcReviewModel") HpcReviewModel hpcReviewModel, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		AjaxResponseBody result = new AjaxResponseBody();

		try {
			boolean updated = true;
			if (hpcReviewModel.getPath() == null || hpcReviewModel.getPath().isEmpty()) {
				result.setMessage("Invald collection path. ");
				return result;
			}

			for (String path: hpcReviewModel.getPath()) {
				HpcCollectionRegistrationDTO registrationDTO = constructRequest(hpcReviewModel);
	
				updated &= HpcClientUtil.updateCollection(authToken, collectionServiceURL, registrationDTO,
						path, sslCertPath, sslCertPassword);
			}
			if (updated) {
				result.setMessage("Successfully updated.");
			} else {
				result.setMessage("Failed to update some records, please retry.");
			}
		} catch (Exception e) {
			result.setMessage("Failed to update review. " + e.getMessage());
		}
		return result;
	}
	
	@PostMapping(value = "/send")
	@ResponseBody
	public AjaxResponseBody sendReviewReminder(@RequestBody(required = false) String body, @RequestParam("userId") String userId, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		AjaxResponseBody result = new AjaxResponseBody();

		try {
			if (userId == null) {
				result.setMessage("User not supplied. ");
				return result;
			}

			UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(reviewReminderServiceURL);

			if (ucBuilder == null) {
				return null;
			}

			final String requestURL = ucBuilder.build().encode().toUri().toURL().toExternalForm();

			WebClient client = HpcClientUtil.getWebClient(requestURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);
			Response restResponse = client.invoke("POST", userId);

			if (restResponse.getStatus() == 200) {
				result.setMessage("Successfully sent reminder notification.");
			} else {
				result.setMessage("Failed to send reminder notfication.");
			}
		} catch (Exception e) {
			result.setMessage("Failed to send reminder notfication. " + e.getMessage());
		}
		return result;
	}
	
	private HpcCompoundMetadataQueryDTO constructCriteria(HpcSearch search) {
		HpcCompoundMetadataQueryDTO dto = new HpcCompoundMetadataQueryDTO();
		dto.setTotalCount(true);
		HpcCompoundMetadataQuery query = buildSimpleSearch(search);
		dto.setCompoundQuery(query);
		dto.setDetailedResponse(search.isDetailed());
		dto.setCompoundQueryType(HpcCompoundMetadataQueryType.COLLECTION);
		dto.setPage(search.getPageNumber());
		dto.setPageSize(search.getPageSize());
		return dto;
	}

	private HpcCompoundMetadataQuery buildSimpleSearch(HpcSearch search) {

		HpcCompoundMetadataQuery query = new HpcCompoundMetadataQuery();
		query.setOperator(HpcCompoundMetadataQueryOperator.AND);
		List<HpcMetadataQuery> queries = new ArrayList<HpcMetadataQuery>();

		// perform AND operation for data_curator and start_date
		HpcMetadataQuery q1 = new HpcMetadataQuery();
		HpcMetadataQueryLevelFilter levelFilter1 = new HpcMetadataQueryLevelFilter();
		levelFilter1.setLabel("PI_Lab");
		levelFilter1.setOperator(HpcMetadataQueryOperator.EQUAL);
		q1.setLevelFilter(levelFilter1);
		q1.setAttribute("data_curator"); //The view column name remains data_curator but pulling data_generator_userid attribute.
		q1.setValue("%");
		q1.setOperator(HpcMetadataQueryOperator.LIKE);
		queries.add(q1);

		HpcMetadataQuery q2 = new HpcMetadataQuery();
		HpcMetadataQueryLevelFilter levelFilter2 = new HpcMetadataQueryLevelFilter();
		//levelFilter2.setLabel("Project");
		levelFilter2.setOperator(HpcMetadataQueryOperator.EQUAL);
		levelFilter2.setLevel(1);
		q2.setLevelFilter(levelFilter2);
		q2.setAttribute("collection_type");
		q2.setValue("Project");
		q2.setOperator(HpcMetadataQueryOperator.EQUAL);
		queries.add(q2);
		
		query.getQueries().addAll(queries);

		return query;
	}

	private List<HpcReviewSearchResult> processResponseResults(Response restResponse) throws IOException {

		List<HpcReviewSearchResult> returnResults = new ArrayList<HpcReviewSearchResult>();

		returnResults = processReviewResults(restResponse);

		return returnResults;

	}

	private List<HpcReviewSearchResult> processReviewResults(Response restResponse) throws IOException {
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
		HpcReviewDTO reviewDTO = parser.readValueAs(HpcReviewDTO.class);

		List<HpcReviewEntry> searchResults = reviewDTO.getReviewEntries();
		List<HpcReviewSearchResult> returnResults = new ArrayList<>();

		for (HpcReviewEntry result : searchResults) {

			HpcReviewSearchResult returnResult = new HpcReviewSearchResult();
			returnResult.setPath(result.getPath());
			returnResult.setProjectTitle(result.getProjectTitle());
			returnResult.setProjectDescription(result.getProjectDescription());
			returnResult.setStartDate(result.getProjectStartDate());
			returnResult.setDataOwner(result.getDataOwner());
			returnResult.setDataCurator(result.getDataCurator());
			returnResult.setDataCuratorName(StringUtils.isNotBlank(result.getDataCuratorName())
					? result.getDataCuratorName() : result.getDataCurator());
			returnResult.setProjectStatus(result.getProjectStatus());
			returnResult.setPublications(result.getPublications());
			returnResult.setDeposition(result.getDeposition());
			returnResult.setSunsetDate(result.getSunsetDate());
			returnResult.setRetentionYears(result.getRetentionYears());
			returnResult.setCompletedDate(result.getProjectCompletedDate());
			returnResult.setLastReviewed(result.getLastReviewed());
			returnResult.setReviewSent(result.getReviewSent());
			returnResult.setReminderSent(result.getReminderSent());

			returnResults.add(returnResult);

		}

		return returnResults;
	}
	
	private static String getAttributeValue(String attrName, HpcMetadataEntries entries) {
		if (entries == null)
			return null;

		List<HpcMetadataEntry> selfEntries = entries.getSelfMetadataEntries();
		for (HpcMetadataEntry entry : selfEntries) {
			if (entry.getAttribute() != null && entry.getAttribute().equals(attrName))
				return entry.getValue();
		}
		List<HpcMetadataEntry> parentEntries = entries.getParentMetadataEntries();
		for (HpcMetadataEntry entry : parentEntries) {
			if (entry.getAttribute() != null && entry.getAttribute().equals(attrName))
				return entry.getValue();
		}
		return null;
	}
	
	private HpcCollectionRegistrationDTO constructRequest(HpcReviewModel hpcReviewEntry) {

		HpcCollectionRegistrationDTO dto = new HpcCollectionRegistrationDTO();
		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();

		if(StringUtils.isNotEmpty(hpcReviewEntry.getProjectStatus())) {
			HpcMetadataEntry status = new HpcMetadataEntry();
			status.setAttribute("project_status");
			status.setValue(hpcReviewEntry.getProjectStatus());
			metadataEntries.add(status);
		}
		
		if(StringUtils.isNotEmpty(hpcReviewEntry.getPublications())) {
			HpcMetadataEntry publication = new HpcMetadataEntry();
			publication.setAttribute("publications");
			publication.setValue(hpcReviewEntry.getPublications());
			metadataEntries.add(publication);
		}
		
		if(StringUtils.isNotEmpty(hpcReviewEntry.getDeposition())) {
			HpcMetadataEntry deposition = new HpcMetadataEntry();
			deposition.setAttribute("deposition");
			deposition.setValue(hpcReviewEntry.getDeposition());
			metadataEntries.add(deposition);
		}
		
		if(StringUtils.isNotEmpty(hpcReviewEntry.getRetentionYears())) {
			HpcMetadataEntry retentionYearsEntry  = new HpcMetadataEntry();
			retentionYearsEntry.setAttribute("retention_years");
			retentionYearsEntry.setValue(hpcReviewEntry.getRetentionYears());
			metadataEntries.add(retentionYearsEntry);
		}
		
		HpcMetadataEntry reviewDate = new HpcMetadataEntry();
		reviewDate.setAttribute("last_reviewed");
		reviewDate.setValue(hpcReviewEntry.getLastReviewed());
		metadataEntries.add(reviewDate);
		
		if (StringUtils.isNotEmpty(hpcReviewEntry.getProjectStatus()) && hpcReviewEntry.getProjectStatus().equals("Completed")) {
			HpcMetadataEntry completedDateEntry = new HpcMetadataEntry();
			Calendar now = Calendar.getInstance();
		    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			String completedDate = format.format(now.getTime());
			completedDateEntry.setAttribute("project_completed_date");
			completedDateEntry.setValue(completedDate);
			metadataEntries.add(completedDateEntry);
			
			int years = Integer.parseInt(hpcReviewEntry.getRetentionYears());
		    now.add(Calendar.YEAR, years);
			String sunsetDate = format.format(now.getTime());
			HpcMetadataEntry sunsetDateEntry = new HpcMetadataEntry();
			sunsetDateEntry.setAttribute("sunset_date");
			sunsetDateEntry.setValue(sunsetDate);
			metadataEntries.add(sunsetDateEntry);
		}	

		dto.getMetadataEntries().addAll(metadataEntries);
		return dto;
	}
	
}
