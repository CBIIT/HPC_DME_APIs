/**
 * HpcSyncDownloadController.java
 *
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for
 * details.
 */
package gov.nih.nci.hpc.web.controller;

import java.io.IOException;
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
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcDownloadDatafile;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import org.springframework.web.util.UriComponentsBuilder;

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
	 * Generate presign download URL
	 * 
	 * @param downloadFile
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @return ajax reponse
	 */
	@RequestMapping("/url")
	@ResponseBody
	public AjaxResponseBody getDownloadUrl(
			@Valid @ModelAttribute("hpcDownloadDatafile") HpcDownloadDatafile downloadFile, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {

		AjaxResponseBody result = new AjaxResponseBody();
		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (authToken == null) {
				result.setCode("error");
				result.setMessage("Invalid user session, expired. Please login again.");
				return result;
			}
			final String serviceURL = UriComponentsBuilder.fromHttpUrl(this.dataObjectServiceURL)
					.path("/{dme-archive-path}/download").buildAndExpand(downloadFile.getDestinationPath()).encode()
					.toUri().toURL().toExternalForm();

			final HpcDownloadRequestDTO dto = new HpcDownloadRequestDTO();
			dto.setGenerateDownloadRequestURL(true);

			WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);

			Response restResponse = client.invoke("POST", dto);

			if (restResponse.getStatus() == 200) {
				HpcDataObjectDownloadResponseDTO downloadDTO = (HpcDataObjectDownloadResponseDTO) HpcClientUtil
						.getObject(restResponse, HpcDataObjectDownloadResponseDTO.class);
				result.setCode("success");
				result.setMessage(downloadDTO.getDownloadRequestURL());
			} else {
				HpcExceptionDTO exceptionDTO = (HpcExceptionDTO) HpcClientUtil.getObject(restResponse,
						HpcExceptionDTO.class);
				result.setCode("error");
				result.setMessage(exceptionDTO.getMessage());
			}
		} catch (RestClientException | IOException e) {
			result.setCode("error");
			result.setMessage("Failed to download: " + e.getMessage());
		}
		return result;
	}

	/**
	 * Sync download ajax call
	 * 
	 * @param downloadFile
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @return ajax response
	 */
	@RequestMapping("/sync")
	@ResponseBody
	public AjaxResponseBody syncDownload(@Valid @ModelAttribute("hpcDownloadDatafile") HpcDownloadDatafile downloadFile,
			Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {

		AjaxResponseBody result = new AjaxResponseBody();
		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (authToken == null) {
				result.setCode("error");
				result.setMessage("Invalid user session, expired. Please login again.");
				return result;
			}
			final String serviceURL = UriComponentsBuilder.fromHttpUrl(this.dataObjectServiceURL)
					.path("/{dme-archive-path}/download").buildAndExpand(downloadFile.getDestinationPath()).encode()
					.toUri().toURL().toExternalForm();

			final HpcDownloadRequestDTO dto = new HpcDownloadRequestDTO();
			dto.setGenerateDownloadRequestURL(false);

			WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);
			Response restResponse = client.invoke("POST", dto);
			if (restResponse.getStatus() == 200) {
				try {
					HpcDataObjectDownloadResponseDTO downloadDTO = (HpcDataObjectDownloadResponseDTO) HpcClientUtil
						.getObject(restResponse, HpcDataObjectDownloadResponseDTO.class);
					if (downloadDTO == null) {
						result.setCode("success");
					} else {
						// Treat it as error to display message for restoration
						// requests.
						result.setCode("error");
						result.setMessage(
								"Object restoration requested. You will recieve an email when it is available for download.");
					}
				} catch (HpcWebException e) {
					// If file is returned, fails to parse record so return success.
					result.setCode("success");
				}
			} else {
				HpcExceptionDTO exceptionDTO = (HpcExceptionDTO) HpcClientUtil.getObject(restResponse,
						HpcExceptionDTO.class);
				result.setCode("error");
				result.setMessage(exceptionDTO.getMessage());
			}

		} catch (IOException e) {
			result.setCode("error");
			result.setMessage("Failed to download: " + e.getMessage());
		}
		return result;
	}

	/**
	 * Download file from presigned URL
	 * 
	 * @param downloadFile
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(path = "/urldownload", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public Resource downloadFromUrl(@Valid @ModelAttribute("hpcDownloadDatafile") HpcDownloadDatafile downloadFile,
			Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {

		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (authToken == null) {
				response.setStatus(400);
				response.getWriter().write("Invalid user session, expired. Please login again.");
				return null;
			}

			downloadToUrl(downloadFile.getS3Path(), 1000000, downloadFile.getDownloadFileName(), response);
			model.addAttribute("message", "Download completed successfully!");
		} catch (IOException e) {
			response.setStatus(400);
			try {
				response.getWriter().write("Failed to download: " + e.getMessage());
			} catch (IOException ex) {
				log.error(ex.getMessage());
			}
		}
		return null;
	}

	/**
	 * Streaming download
	 * 
	 * @param downloadFile
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(path = "/syncdownload", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public Resource downloadFromSync(@Valid @ModelAttribute("hpcDownloadDatafile") HpcDownloadDatafile downloadFile,
			Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {

		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (authToken == null) {
				response.setStatus(400);
				response.getWriter().write("Invalid user session, expired. Please login again.");
				return null;
			}
			final String serviceURL = UriComponentsBuilder.fromHttpUrl(this.dataObjectServiceURL)
					.path("/{dme-archive-path}/download").buildAndExpand(downloadFile.getDestinationPath()).encode()
					.toUri().toURL().toExternalForm();

			final HpcDownloadRequestDTO dto = new HpcDownloadRequestDTO();
			dto.setGenerateDownloadRequestURL(false);

			WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);
			Response restResponse = client.invoke("POST", dto);

			// TODO: The API is no longer returning DATA_TRANSFER_TYPE as header
			// parameter.
			// Regardless the implementation of sync download from POSIX archive
			// is broken
			// Need to implement sync download from POSIX archive.

			if (restResponse.getStatus() == 200) {
				handleStreamingDownloadData(downloadFile, response, restResponse);
			} else {
				handleDownloadProblem(model, restResponse, response);
			}

		} catch (RestClientException | IOException e) {
			response.setStatus(400);
			try {
				response.getWriter().write("Failed to download: " + e.getMessage());
			} catch (IOException ex) {
				log.error(ex.getMessage());
			}
		}
		return null;
	}

  public void downloadToUrl(String urlStr, int bufferSize, String fileName,
      HttpServletResponse response) throws HpcWebException {
    try {
      WebClient client = HpcClientUtil.getWebClient(urlStr, null, null);
      Response restResponse = client.invoke("GET", null);
      response.setContentType("application/octet-stream");
      response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
      IOUtils.copy((InputStream) restResponse.getEntity(), response.getOutputStream());
    } catch (IOException e) {
      throw new HpcWebException(e);
    }
  }

  private void handleDownloadProblem(Model model, Response restResponse, HttpServletResponse response)
    throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
        new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
        new JacksonAnnotationIntrospector());
    mapper.setAnnotationIntrospector(intr);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    MappingJsonFactory factory = new MappingJsonFactory(mapper);
    JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

    HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
    response.setStatus(400);
    response.getWriter().write("Failed to download: " + exception.getMessage());
  }


  private void handleStreamingDownloadData(
    @Valid @ModelAttribute("hpcDownloadDatafile") HpcDownloadDatafile
      downloadFile,
    HttpServletResponse response,
    Response restResponse) throws IOException
  {
    response.setContentType("application/octet-stream");
    response.setHeader("Content-Disposition", "attachment; filename=" +
      downloadFile.getDownloadFileName());
    IOUtils.copy((InputStream) restResponse.getEntity(),
      response.getOutputStream());
  }

}
