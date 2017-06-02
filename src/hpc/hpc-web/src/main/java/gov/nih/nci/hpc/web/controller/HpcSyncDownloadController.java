/**
 * HpcSyncDownloadController.java
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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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

import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.web.model.HpcDownloadDatafile;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Controller to manage synchronous download of a data file
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/downloadsync")
public class HpcSyncDownloadController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.dataObject}")
	private String dataObjectServiceURL;

	/**
	 * POST action for sync download
	 * 
	 * @param downloadFile
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public Resource download(@Valid @ModelAttribute("hpcDownloadDatafile") HpcDownloadDatafile downloadFile,
			Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (authToken == null) {
				model.addAttribute("Invalid user session, expired. Please login again.");
				return null;
			}

			String serviceURL = dataObjectServiceURL + downloadFile.getDestinationPath() + "/download";
			HpcDownloadRequestDTO dto = new HpcDownloadRequestDTO();

			WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);

			Response restResponse = client.invoke("POST", dto);
			if (restResponse.getStatus() == 200) {
				response.setContentType("application/octet-stream");
				response.setHeader("Content-Disposition", "attachment; filename=" + downloadFile.getDownloadFileName());
				IOUtils.copy((InputStream) restResponse.getEntity(), response.getOutputStream());
				model.addAttribute("message", "Download completed successfully!");
			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				try {
					HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
					model.addAttribute("message", "Failed to download: " + exception.getMessage());
				} catch (Exception e) {
					model.addAttribute("message", "Failed to download: " + e.getMessage());
				}
			}
		} catch (HttpStatusCodeException e) {
			model.addAttribute("message", "Failed to download: " + e.getMessage());
			e.printStackTrace();
		} catch (RestClientException e) {
			model.addAttribute("message", "Failed to download: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			model.addAttribute("message", "Failed to download: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
}
