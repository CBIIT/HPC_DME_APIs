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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcDownloadDatafile;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.MiscUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

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

  public static final int BUFFER_SIZE = 1000000;

  public static final String ATTACHMENT_FILENAME_EQUALS =
    "attachment; filename=";

  public static final String DATA_TRANSFER_TYPE = "DATA_TRANSFER_TYPE";

  public static final String DOWNLOAD_COMPLETED_SUCCESSFULLY =
    "Download completed successfully!";

  public static final String DOWNLOAD_URI_PATH_PART = "/download";

  public static final String PLEASE_LOGIN_AGAIN_MSG =
    "Invalid user session, expired. Please login again.";

  @Value("${gov.nih.nci.hpc.server.dataObject}")
  private String dataObjectServiceURL;

  /**
   * POST action for sync download
   */
  @RequestMapping(method = RequestMethod.POST,
    produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @ResponseBody
  public Resource download(
      @Valid @ModelAttribute("hpcDownloadDatafile")
        HpcDownloadDatafile downloadFile,
      Model model,
      HttpSession session,
      HttpServletResponse response) {
    Resource retResource = null;
    try {
      final String authToken = (String) session.getAttribute("hpcUserToken");
      if (null == authToken) {
        model.addAttribute(PLEASE_LOGIN_AGAIN_MSG);
      }
      final Response restResponse =
        makeDownloadCallToDmeApi(downloadFile, authToken);
      if (HttpStatus.OK.value() == restResponse.getStatus()) {
        performDownloadProcessing(downloadFile, model, response, restResponse);
      } else {
        retResource = handleNonOkRestResponse(model, restResponse);
      }
    } catch (Exception e) {
      retResource = handleExceptionOnDownloadFailure(model, e.getMessage());
    }
    return retResource;
  }


  public void downloadToUrl(
    String urlStr,
    int bufferSize,
    String fileName,
    HttpServletResponse response)
  throws HpcWebException {
    try {
      final Response restResponse =
        HpcClientUtil.getWebClient(urlStr, null, null)
                     .invoke(HttpMethod.GET, null);
      response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
      response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
    ATTACHMENT_FILENAME_EQUALS + fileName);
      IOUtils.copy((InputStream) restResponse.getEntity(),
        response.getOutputStream());
    } catch (IOException e) {
      throw new HpcWebException(e);
    }
  }


  private boolean checkIfRequestInvolvedS3(Response argResponse) {
    boolean retFlag = false;
    if (null != argResponse &&
        null != argResponse.getMetadata() &&
        null != argResponse.getMetadata().get(DATA_TRANSFER_TYPE)) {
      final List<Object> dataTransferTypes = (List<Object>)
          argResponse.getMetadata().get(DATA_TRANSFER_TYPE);
      if (null != dataTransferTypes && !dataTransferTypes.isEmpty() &&
          HpcDataTransferType.S_3.value().equals(dataTransferTypes.get(0))) {
        retFlag = true;
      }
    }

    return retFlag;
  }


  private String generateDatafileDownloadURL(
    HpcDownloadDatafile argDownloadFile)
  {
    final StringBuilder sb = new StringBuilder(dataObjectServiceURL);
    sb.append(MiscUtil.urlEncodeDmePath(argDownloadFile.getDestinationPath()))
      .append(DOWNLOAD_URI_PATH_PART);
    final String retUrl = sb.toString();

    return retUrl;
  }


  private Resource handleExceptionOnDownloadFailure(
      Model model, String message)
  {
    final String errMsg = String.format("Failed to download: %s", message);
    model.addAttribute("message", errMsg);
    return new ByteArrayResource(errMsg.getBytes());
  }


  private Resource handleNonOkRestResponse(Model model, Response restResponse)
  throws IOException
  {
    Resource retResource;
    final JsonParser parser = produceJsonParser(restResponse);
    try {
      final HpcExceptionDTO exception =
        parser.readValueAs(HpcExceptionDTO.class);
      retResource =
        handleExceptionOnDownloadFailure(model, exception.getMessage());
    } catch (Exception e) {
      retResource =
        handleExceptionOnDownloadFailure(model, e.getMessage());
    }

    return retResource;
  }


  private Response makeDownloadCallToDmeApi(
      @Valid @ModelAttribute("hpcDownloadDatafile") HpcDownloadDatafile
        downloadFile,
      String authToken)
  {
    final HpcDownloadRequestDTO dto = new HpcDownloadRequestDTO();
    dto.setGenerateDownloadRequestURL(true);
    final String serviceURL = generateDatafileDownloadURL(downloadFile);
    final WebClient client =
        HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
    client.header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken);
    final Response retResponse = client.invoke(HttpMethod.POST, dto);

    return  retResponse;
  }


  private void performDownloadProcessing(
      @Valid @ModelAttribute("hpcDownloadDatafile") HpcDownloadDatafile
        downloadFile,
      Model model,
      HttpServletResponse response,
      Response restResponse)
  throws IOException
  {
    if (checkIfRequestInvolvedS3(restResponse)) {
      final HpcDataObjectDownloadResponseDTO downloadDTO =
          (HpcDataObjectDownloadResponseDTO) HpcClientUtil.getObject(
              restResponse, HpcDataObjectDownloadResponseDTO.class);
      if (null != downloadDTO) {
        final String downloadRequestURL =
          downloadDTO.getDownloadRequestURL();
        downloadToUrl(downloadRequestURL, BUFFER_SIZE,
          downloadFile.getDownloadFileName(), response);
      }
    } else {
      response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
      response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
    ATTACHMENT_FILENAME_EQUALS + downloadFile.getDownloadFileName());
      IOUtils.copy((InputStream) restResponse.getEntity(),
        response.getOutputStream());
    }

    model.addAttribute("message", DOWNLOAD_COMPLETED_SUCCESSFULLY);
  }


  private JsonParser produceJsonParser(Response argResponse)
  throws IOException
  {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.setAnnotationIntrospector(new AnnotationIntrospectorPair(
        new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
        new JacksonAnnotationIntrospector())
    );
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    final JsonParser retParser =
      new MappingJsonFactory(mapper).createParser(
        (InputStream) argResponse.getEntity());

    return retParser;
  }

}
