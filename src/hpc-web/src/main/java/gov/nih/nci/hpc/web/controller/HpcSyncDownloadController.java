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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.io.ByteArrayResource;
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
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.web.HpcWebException;
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
  public Resource download(
      @Valid @ModelAttribute("hpcDownloadDatafile") HpcDownloadDatafile downloadFile, Model model,
      BindingResult bindingResult, HttpSession session, HttpServletRequest request,
      HttpServletResponse response) {
    try {
      String authToken = (String) session.getAttribute("hpcUserToken");
      if (authToken == null) {
        model.addAttribute("Invalid user session, expired. Please login again.");
        return null;
      }
      final String serviceURL = UriComponentsBuilder.fromHttpUrl(
        this.dataObjectServiceURL).path("/{dme-archive-path}/download")
        .buildAndExpand(downloadFile.getDestinationPath()).encode().toUri()
        .toURL().toExternalForm();

      final HpcDownloadRequestDTO dto = new HpcDownloadRequestDTO();
      dto.setGenerateDownloadRequestURL(true);

      WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
      client.header("Authorization", "Bearer " + authToken);

      Response restResponse = client.invoke("POST", dto);
      
      
      // TODO: The API is no longer returning DATA_TRANSFER_TYPE as header parameter.
      // Regardless the implementation of sync download from POSIX archive is broken
      // Need to implement sync download from POSIX archive.
      
      //MultivaluedMap<String, Object> respHeaders = restResponse.getMetadata();
      //List dataTransferTypes = (List)respHeaders.get("DATA_TRANSFER_TYPE");
      
      if (restResponse.getStatus() == 200) {
          //if (null != dataTransferTypes &&
            //  "S_3".equals(dataTransferTypes.get(0))) {
            HpcDataObjectDownloadResponseDTO downloadDTO =
              (HpcDataObjectDownloadResponseDTO) HpcClientUtil.getObject(
              restResponse, HpcDataObjectDownloadResponseDTO.class);
            downloadToUrl(downloadDTO.getDownloadRequestURL(), 1000000,
              downloadFile.getDownloadFileName(), response);
          //} else {
            //handleStreamingDownloadData(downloadFile, response, restResponse);
          //}
          model.addAttribute("message", "Download completed successfully!");
      } else if (restResponse.getStatus() == 400) {
        // Bad request so assume that request can be retried without any state
        //  to indicate S3-presigned-URL desired (or other such special
        //  handling)
        dto.setGenerateDownloadRequestURL(false);
        restResponse = client.invoke("POST", dto);
        if (restResponse.getStatus() == 200) {
          handleStreamingDownloadData(downloadFile, response, restResponse);
        } else {
          response.setHeader("Content-Disposition", "attachment; filename=" + downloadFile.getDownloadFileName() + ".error");
          return handleDownloadProblem(model, restResponse);
        }
      } else {
        response.setHeader("Content-Disposition", "attachment; filename=" + downloadFile.getDownloadFileName() + ".error");
        return handleDownloadProblem(model, restResponse);
      }
    } catch (HttpStatusCodeException e) {
      model.addAttribute("message", "Failed to download: " + e.getMessage());
      response.setHeader("Content-Disposition", "attachment; filename=" + downloadFile.getDownloadFileName() + ".error");
      return new ByteArrayResource(("Failed to download: " +
        e.getMessage()).getBytes());
    } catch (RestClientException e) {
      model.addAttribute("message", "Failed to download: " + e.getMessage());
      response.setHeader("Content-Disposition", "attachment; filename=" + downloadFile.getDownloadFileName() + ".error");
      return new ByteArrayResource(("Failed to download: " +
        e.getMessage()).getBytes());
    } catch (Exception e) {
      model.addAttribute("message", "Failed to download: " + e.getMessage());
      response.setHeader("Content-Disposition", "attachment; filename=" + downloadFile.getDownloadFileName() + ".error");
      return new ByteArrayResource(("Failed to download: " +
        e.getMessage()).getBytes());
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


  private Resource handleDownloadProblem(Model model, Response restResponse)
    throws IOException {
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
      model.addAttribute("message", "Failed to download: " +
        exception.getMessage());
      return new ByteArrayResource(("Failed to download: " +
        exception.getMessage()).getBytes());
    } catch (Exception e) {
      model.addAttribute("message", "Failed to download: " + e.getMessage());
      return new ByteArrayResource(("Failed to download: " +
        e.getMessage()).getBytes());
    }
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
