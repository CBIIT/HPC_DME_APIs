/**
 * HpcDownloadController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import gov.nih.nci.hpc.web.util.MiscUtil;
import java.io.InputStream;

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
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
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
 * Controller to support asynchronous download of a data file or collection
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDownloadController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/download")
public class HpcDownloadController extends AbstractHpcController {

  private static final String ASYNC = "async";
  private static final String DOWNLOAD_URI_SEGMENT = "download";
  private static final String ERROR_MSG_INVALID_SESSION_SHORT =
    "Invalid user session!";
  private static final String ERROR_MSG_INVALID_SESSION =
    "Invalid user session, expired. Please login again.";
  private static final String FORWARD_SLASH = "/";
  private static final String NAV_OUTCOME_DOWNLOAD = "download";
  private static final String NAV_OUTCOME_INDEX = "index";

  private static final String ERROR_MSG_TEMPLATE =
    "Download request is not successful: %s";

  @Value("${gov.nih.nci.hpc.server.dataObject}")
	private String dataObjectServiceURL;
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionServiceURL;

	/**
	 * Get action to prepare download page
	 * 
	 * @param q
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(
    @RequestBody(required = false)
      String q,
    Model model,
    BindingResult bindingResult,
    HttpSession session,
    HttpServletRequest request) {
    final String downloadType = request.getParameter("type");
    final String downloadFilePath = request.getParameter("path");
    final String fileName = extractFileNameFromFilePath(downloadFilePath);
    model.addAttribute("hpcDownloadDatafile", new HpcDownloadDatafile());
    model.addAttribute("downloadType", downloadType);
		model.addAttribute("downloadFilePath", downloadFilePath);
		if (null != fileName) {
			model.addAttribute("downloadFilePathName", fileName);
		}
		final HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String navOutcome;
		if (user == null) {
      bindingResult.addError(new
        ObjectError("hpcLogin", ERROR_MSG_INVALID_SESSION_SHORT));
      model.addAttribute("hpcLogin", new HpcLogin());
			navOutcome = NAV_OUTCOME_INDEX;
		} else {
		  navOutcome = NAV_OUTCOME_DOWNLOAD;
    }

		return navOutcome;
	}

	/**
	 * POST action to initiate asynchronous download.
	 */
  @JsonView(Views.Public.class)
  @RequestMapping(method = RequestMethod.POST)
  @ResponseBody
  public AjaxResponseBody download(
      @Valid @ModelAttribute("hpcDownloadDatafile")
        HpcDownloadDatafile downloadFile,
      HttpSession session) {
    AjaxResponseBody result = new AjaxResponseBody();
    final String authToken = (String) session.getAttribute("hpcUserToken");
    if (authToken == null) {
      result.setMessage(ERROR_MSG_INVALID_SESSION);
    } else {
      try {
        final String serviceURL = generateServiceURL(downloadFile);
        final HpcDownloadRequestDTO dto = new HpcDownloadRequestDTO();
        if (null != downloadFile.getSearchType() &&
            ASYNC.equals(downloadFile.getSearchType())) {
          final HpcFileLocation location = new HpcFileLocation();
          location.setFileContainerId(downloadFile.getEndPointName());
          location.setFileId(downloadFile.getEndPointLocation());
          dto.setDestination(location);
        }
        result = HpcClientUtil.downloadDataFile(authToken, serviceURL, dto,
            sslCertPath, sslCertPassword);
      } catch (HttpStatusCodeException e) {
        result.setMessage(String.format(ERROR_MSG_TEMPLATE, e.getMessage()));
      } catch (RestClientException e) {
        result.setMessage(String.format(ERROR_MSG_TEMPLATE, e.getMessage()));
      } catch (Exception e) {
        result.setMessage(String.format(ERROR_MSG_TEMPLATE, e.getMessage()));
      }
    }

    return result;
  }


  private String extractFileNameFromFilePath(String argFilePath) {
    String retFileName;
    if (null == argFilePath) {
      retFileName = null;
    } else {
      int index = argFilePath.lastIndexOf(FORWARD_SLASH);
      if (-1 == index) {
        index = argFilePath.lastIndexOf(FORWARD_SLASH.concat(FORWARD_SLASH));
      }
      retFileName = (-1 == index) ? argFilePath :
                                      argFilePath.substring(1 + index);
    }

    return retFileName;
  }


	private String generateServiceURL(
			@Valid @ModelAttribute("hpcDownloadDatafile")
        HpcDownloadDatafile downloadFile) {
		final StringBuilder sb = new StringBuilder();
		if ("collection".equals(downloadFile.getDownloadType())) {
			sb.append(collectionServiceURL);
		} else {
		  sb.append(dataObjectServiceURL);
		}
    sb.append(MiscUtil.urlEncodeDmePath(downloadFile.getDestinationPath()))
      .append(FORWARD_SLASH)
      .append(DOWNLOAD_URI_SEGMENT);
		final String serviceURL = sb.toString();

		return serviceURL;
	}
}
