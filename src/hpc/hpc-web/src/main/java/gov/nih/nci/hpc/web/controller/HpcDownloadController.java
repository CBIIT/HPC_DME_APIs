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

import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

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

import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcDownloadDatafile;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * HPC DM Project Search controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDataRegistrationController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/download")
public class HpcDownloadController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.dataObject}")
	private String dataObjectServiceURL;

	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		HpcDownloadDatafile hpcDownloadDatafile = new HpcDownloadDatafile();
		model.addAttribute("hpcDownloadDatafile", hpcDownloadDatafile);
		String downloadFilePath = request.getParameter("path");
		model.addAttribute("downloadFilePath", downloadFilePath);
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}
		return "download";
	}

	/**
	 * 
	 * @RequestMapping(method = RequestMethod.POST, produces =
	 *                        MediaType.APPLICATION_OCTET_STREAM_VALUE)
	 * @ResponseBody public Resource
	 *               download(@Valid @ModelAttribute("hpcDownloadDatafile")
	 *               HpcDownloadDatafile downloadFile, Model model,
	 *               BindingResult bindingResult, HttpSession session,
	 *               HttpServletRequest request, HttpServletResponse response) {
	 *               try { // String criteria = getCriteria();
	 * 
	 *               String authToken = (String)
	 *               session.getAttribute("hpcUserToken"); String serviceURL =
	 *               dataObjectServiceURL
	 *               +downloadFile.getDestinationPath()+"/download";
	 *               HpcDataObjectDownloadRequestDTO dto = new
	 *               HpcDataObjectDownloadRequestDTO();
	 * 
	 *               WebClient client = HpcClientUtil.getWebClient(serviceURL,
	 *               sslCertPath, sslCertPassword);
	 *               client.header("Authorization", "Bearer " + authToken);
	 * 
	 *               Response restResponse = client.invoke("POST", dto); if
	 *               (restResponse.getStatus() == 200) { return new
	 *               FileSystemResource(new
	 *               File("C:\\DEV\\temp\\keystore.jks")); } else { ObjectMapper
	 *               mapper = new ObjectMapper(); AnnotationIntrospectorPair
	 *               intr = new AnnotationIntrospectorPair( new
	 *               JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
	 *               new JacksonAnnotationIntrospector() );
	 *               mapper.setAnnotationIntrospector(intr);
	 *               mapper.configure(DeserializationFeature.
	 *               FAIL_ON_UNKNOWN_PROPERTIES, false);
	 * 
	 *               MappingJsonFactory factory = new
	 *               MappingJsonFactory(mapper); JsonParser parser =
	 *               factory.createParser((InputStream)
	 *               restResponse.getEntity());
	 * 
	 *               HpcExceptionDTO exception =
	 *               parser.readValueAs(HpcExceptionDTO.class); //return null; }
	 *               } catch (HttpStatusCodeException e) { e.printStackTrace();
	 *               } catch (RestClientException e) { e.printStackTrace(); }
	 *               catch (Exception e) { e.printStackTrace(); } return null; }
	 */

	@JsonView(Views.Public.class)
	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public AjaxResponseBody download(@Valid @ModelAttribute("hpcDownloadDatafile") HpcDownloadDatafile downloadFile,
			Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {
		AjaxResponseBody result = new AjaxResponseBody();
		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			String serviceURL = dataObjectServiceURL + downloadFile.getDestinationPath() + "/download";
			HpcDownloadRequestDTO dto = new HpcDownloadRequestDTO();
			boolean asyncDownload = false;
			if (downloadFile.getSearchType() != null && downloadFile.getSearchType().equals("async")) {
				HpcFileLocation location = new HpcFileLocation();
				location.setFileContainerId(downloadFile.getEndPointName());
				location.setFileId(downloadFile.getEndPointLocation());
				dto.setDestination(location);
				asyncDownload = true;
			}

			WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);

			Response restResponse = client.invoke("POST", dto);
			if (restResponse.getStatus() == 200) {
				if (asyncDownload) {
					result.setCode("200");
					result.setMessage("Asynchronous download request is submitted successfully!");
					return result;
				} else {
					response.setContentType("application/octet-stream");
					response.setHeader("Content-Disposition",
							String.format("inline; filename=" + downloadFile.getDownloadFileName()));
					InputStream stream = (InputStream) restResponse.getEntity();
					OutputStream outStream = response.getOutputStream();
					int len = 0;
					byte[] buffer = new byte[4096];
					while ((len = stream.read(buffer)) != -1) {
						outStream.write(buffer, 0, len);
					}
					// FileCopyUtils.copy(stream, response.getOutputStream());
					outStream.flush();
					stream.close();
					outStream.close();
					result.setCode("200");
					result.setMessage("Synchronous download is successful!");
					return result;
				}

				// return new FileSystemResource(new
				// File("C:\\DEV\\temp\\keystore.jks"));

				// return new InputStreamResource ((InputStream)
				// restResponse.getEntity());
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
				result.setMessage("Download request is not successfull: " + exception.getMessage());
				return result;
			}
		} catch (HttpStatusCodeException e) {
			result.setCode("400");
			result.setMessage("Download request is not successfull: " + e.getMessage());
			return result;
		} catch (RestClientException e) {
			result.setCode("400");
			result.setMessage("Download request is not successfull: " + e.getMessage());
			return result;
		} catch (Exception e) {
			result.setCode("400");
			result.setMessage("Download request is not successfull: " + e.getMessage());
			return result;
		}
	}
}
