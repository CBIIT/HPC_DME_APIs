/**
 * HpcDatafileController.java
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
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import gov.nih.nci.hpc.domain.datamanagement.HpcMetadataUpdateItem;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkMetadataUpdateRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkMetadataUpdateResponseDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcBulkMetadataUpdateRequest;
import gov.nih.nci.hpc.web.model.HpcDownloadDatafile;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcSearch;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcSearchUtil;

/**
 * <p>
 * Controller to manage bulk metadata updates
 * </p>
 *
 * @author <a href="mailto:Sarada.Chintala@nih.gov">Sarada Chintala</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
public class HpcBulkMetadataController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.user}")
	private String serviceURL;
    @Value("${gov.nih.nci.hpc.server.metadata}")
    private String serviceMetadataURL;
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String serverCollectionURL;
	@Value("${gov.nih.nci.hpc.server.dataObject}")
	private String serverDataObjectURL;
	@Value("${hpc.serviceaccount}")
	private String serviceAccount;
	/**
	 * POST action to render bulk meta data update page
	 * 
	 * @param body
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@PostMapping(value = "/bulkupdatemetadata")
	public String home(@RequestBody(required = false) String body, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {

		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (user == null || authToken == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "dashboard";
		}
		HpcSearchUtil.cacheSelectedRows(session, request, model);
		HpcDownloadDatafile hpcDownloadDatafile = new HpcDownloadDatafile();

		String downloadType = request.getParameter("downloadType");
		hpcDownloadDatafile.setDownloadType(downloadType);
		String selectedPathsStr = request.getParameter("selectedFilePaths");
		List<HpcPathGridEntry> pathDetails = new ArrayList<>();

		if (selectedPathsStr.isEmpty()) {
			model.addAttribute("error", "Data file list is missing!");
		} else {
			StringTokenizer tokens = new StringTokenizer(selectedPathsStr, ",");
			List<String> paths = new ArrayList<>();
			while (tokens.hasMoreTokens()) {
				String pathStr = tokens.nextToken();
				String path = pathStr.substring(pathStr.lastIndexOf(":") + 1);
				paths.add(path);
				hpcDownloadDatafile.getSelectedPaths().add(path);
				HpcPathGridEntry pathGridEntry = new HpcPathGridEntry();
				pathGridEntry.path = path;
				pathGridEntry.result = "";
			}
	        model.addAttribute("paths", paths);
		}
		model.addAttribute("downloadType", downloadType);

		model.addAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
		session.setAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
			
		HpcSearch hpcSaveSearch = new HpcSearch();
		String pageNumber = request.getParameter("pageNumber");
		hpcSaveSearch.setPageNumber(pageNumber != null ? Integer.parseInt(pageNumber) : 1);
		String pageSize = request.getParameter("pageSize");
		if (StringUtils.isNotBlank(pageSize))
			hpcSaveSearch.setPageSize(Integer.parseInt(pageSize));
		hpcSaveSearch.setQueryName(request.getParameter("queryName"));
		hpcSaveSearch.setSearchType(request.getParameter("searchType"));
		String[] deselectedColumns =  request.getParameterValues("deselectedColumns");
		if(deselectedColumns != null && StringUtils.isNotEmpty(deselectedColumns[0]))
			hpcSaveSearch.getDeselectedColumns().addAll(org.springframework.util.CollectionUtils.arrayToList(deselectedColumns[0].split(",")));
		hpcSaveSearch.setTotalSize(StringUtils.isNotBlank(request.getParameter("totalSize")) ? Long.parseLong(request.getParameter("totalSize")) : 0);
		model.addAttribute("hpcSearch", hpcSaveSearch);
		session.setAttribute("hpcSavedSearch", hpcSaveSearch);

		//model.addAttribute("bulkUpdateRequest", new HpcBulkMetadataUpdateRequest());
		
		model.addAttribute("bulkMetadataUpdateRequest", new HpcBulkMetadataUpdateRequest());
		
		return "updatemetadatabulk";
	}


	/**
	 * POST action to add/update metadata in bulk(multiple paths). On update, redirect
	 * back to bulk permissions page with results
	 * 
	 * @param bulkMetadataUpdateRequest
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param redirectAttrs
	 * @return
	 */
	@PostMapping(value = "/assignbulkmetadata")
	public String setBulkMetadata(@Valid @ModelAttribute("bulkMetadataUpdateRequest") HpcBulkMetadataUpdateRequest bulkMetadataUpdateRequest, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			RedirectAttributes redirectAttrs) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		List<HpcPathGridEntry> pathDetails = new ArrayList<>();
		//if (authToken == null) {
		//	return "redirect:/";
		//}
		//String selectedPathsStr = request.getParameter("selectedFilePaths");
		String downloadType = request.getParameter("downloadType");
		HpcDownloadDatafile hpcDownloadDatafile = new HpcDownloadDatafile();
		hpcDownloadDatafile.setDownloadType(downloadType);
		
		List<HpcMetadataEntry> metadataList = new ArrayList<HpcMetadataEntry>();
		HpcMetadataEntry metadataEntry = new HpcMetadataEntry();
		metadataEntry.setAttribute(bulkMetadataUpdateRequest.getMetadataName());
		metadataEntry.setValue(bulkMetadataUpdateRequest.getMetadataValue());
		metadataList.add(metadataEntry);
	    model.addAttribute("downloadType", downloadType);
	    model.addAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
	    session.setAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
	            

		List<String> paths = new ArrayList<>();
		for(String path: bulkMetadataUpdateRequest.getSelectedFilePaths()) {
		  path = path.replaceAll("\\[", "").replaceAll("\\]","");
		  paths.add(path);
		}
		HpcSearch hpcSaveSearch = (HpcSearch) session.getAttribute("hpcSavedSearch");
		model.addAttribute("hpcSearch", hpcSaveSearch);
		String result = "";
		HpcBulkMetadataUpdateRequestDTO req =  new HpcBulkMetadataUpdateRequestDTO();
		req.getCollectionPaths().addAll(paths);
		req.getMetadataEntries().addAll(metadataList);
		 try {

            if (true) {
                WebClient client = HpcClientUtil.getWebClient(serviceMetadataURL, sslCertPath, sslCertPassword);
                client.header("Authorization", "Bearer " + authToken);
                Response restResponse = client.invoke("POST", req);
				if (restResponse.getStatus() == 200) {
					ObjectMapper mapper = new ObjectMapper();
					AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
							new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
							new JacksonAnnotationIntrospector());
					mapper.setAnnotationIntrospector(intr);
					mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

					MappingJsonFactory factory = new MappingJsonFactory(mapper);
					JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

					HpcBulkMetadataUpdateResponseDTO bulkUpdateResponseDTO = parser.readValueAs(HpcBulkMetadataUpdateResponseDTO.class);
					List<HpcMetadataUpdateItem> completedItems =  bulkUpdateResponseDTO.getCompletedItems();
					List<HpcMetadataUpdateItem> failedItems =  bulkUpdateResponseDTO.getFailedItems();
					for (HpcMetadataUpdateItem item : bulkUpdateResponseDTO.getCompletedItems()) {
						HpcPathGridEntry pathGridEntry = new HpcPathGridEntry();
						pathGridEntry.path = item.getPath();
						pathGridEntry.result = "success";
						pathDetails.add(pathGridEntry);
					}
					for (HpcMetadataUpdateItem item : bulkUpdateResponseDTO.getFailedItems()) {
						HpcPathGridEntry pathGridEntry = new HpcPathGridEntry();
						pathGridEntry.path = item.getPath();
						pathGridEntry.result = item.getMessage();
						pathDetails.add(pathGridEntry);
					}
 					model.addAttribute("pathDetails", pathDetails);
					return "updatemetadatabulk";
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
					throw new Exception(exception.getMessage());
				}
			}
		} catch (HttpStatusCodeException e) {
			//throw e.toString();
			return "";
		} catch (RestClientException e) {
			//throw e;
			return "";
		} catch (Exception e) {
			return "";
			//throw e;
		}
		return result;
	}

	private class HpcPathGridEntry {
		public String path;
		public String result;
	}
}
