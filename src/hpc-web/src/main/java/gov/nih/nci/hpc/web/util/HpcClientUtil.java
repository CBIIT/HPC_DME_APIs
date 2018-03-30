package gov.nih.nci.hpc.web.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkListDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDocDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMetadataAttributesListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcPermissionForCollection;
import gov.nih.nci.hpc.dto.datamanagement.HpcRegistrationSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermsForCollectionsDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryListDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionListDTO;
import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupListDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.dto.security.HpcUserListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserRequestDTO;
import gov.nih.nci.hpc.web.HpcResponseErrorHandler;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import javax.xml.transform.Source;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.integration.http.converter.MultipartAwareFormHttpMessageConverter;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

public class HpcClientUtil {

  private static final MappingJsonFactory MY_CONFIGURED_JSON_MAPPING_FACTORY;
  static {
    ObjectMapper mapper = new ObjectMapper();
    AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
        new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
        new JacksonAnnotationIntrospector());
    mapper.setAnnotationIntrospector(intr);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    MY_CONFIGURED_JSON_MAPPING_FACTORY = new MappingJsonFactory(mapper);
  }

  private static final MappingJsonFactory MY_PLAIN_JSON_MAPPING_FACTORY =
      new MappingJsonFactory();

  private static final String BEARER = "Bearer ";
  private static final String ELEM_TYPE__DATA_FILE = "data file";
  private static final String ERR_MSG_TEMPLATE__FAILED_GET_PATH_ELEM_TYPE =
    "Failed to determine type of DME entity at path, %s." +
    "  Exception message: %s.";
  private static final String JSON_RESPONSE_ATTRIB__ELEMENT_TYPE =
      "elementType";


  public static String getAuthenticationToken(String userId, String passwd, String hpcServerURL)
      throws HpcWebException {
    WebClient client = HpcClientUtil.getWebClient(hpcServerURL, null, null);
    String token = DatatypeConverter.printBase64Binary((userId + ":" + passwd).getBytes());
    client.header(HttpHeaders.AUTHORIZATION, "Basic " + token);
    Response restResponse = client.get();
    try {
      if (!isResponseOk(restResponse)) {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Authentication failed: "));
      }
      HpcAuthenticationResponseDTO dto = (HpcAuthenticationResponseDTO)
          parsePlainly((InputStream) restResponse.getEntity(),
              HpcAuthenticationResponseDTO.class);
      return dto.getToken();
    } catch (IllegalStateException|IOException e) {
      throw dumpStackTraceAndWrapException(
        e, Optional.of("Failed to get auth token: "));
    }
  }


  public static String getBasePath(HttpServletRequest request) {
    final String[] basePathValues = request.getParameterValues("basePath");
    String basePath = null;
    if (basePathValues == null || basePathValues.length == 0) {
      basePath = (String) request.getAttribute("basePath");
    }
    else {
      basePath = basePathValues[0];
    }
    if (basePath != null && "_select_null".equals(basePath)) {
      return null;
    }
    return basePath;
  }


  public static String getBasePath(String authToken, String serviceURL, String parent,
      String sslCertPath, String sslCertPassword, HpcDataManagementModelDTO modelDTO) {
    HpcCollectionListDTO collectionListDTO = HpcClientUtil.getCollection(authToken, serviceURL,
        parent, true, sslCertPath, sslCertPassword);
    if (collectionListDTO != null && collectionListDTO.getCollections() != null) {
      HpcCollectionDTO collection = collectionListDTO.getCollections().get(0);
      String configurationId = null;
      if (collection != null) {
        if (collection.getMetadataEntries() != null
            && collection.getMetadataEntries().getSelfMetadataEntries() != null) {
          for (HpcMetadataEntry entry : collection.getMetadataEntries().getSelfMetadataEntries())
            if (entry.getAttribute().equals("configuration_id")) {
              configurationId = entry.getValue();
              break;
            }
        }
      }
      if (configurationId != null) {
        if (modelDTO != null) {
          // TODO
          for (HpcDocDataManagementRulesDTO rulesDTO : modelDTO.getDocRules()) {
            for (HpcDataManagementRulesDTO rule : rulesDTO.getRules()) {
              if (rule.getId().equals(configurationId)) {
                return rule.getBasePath();
              }
            }
          }
        }
      }
    }
    return null;
  }


  public static HpcDataManagementRulesDTO getBasePathManagementRules(
      HpcDataManagementModelDTO docModelDto, String basePath) {
    if (docModelDto == null || docModelDto.getDocRules() == null ||
        basePath == null) {
      return null;
    }
    for (HpcDocDataManagementRulesDTO docDTO : docModelDto.getDocRules()) {
      for (HpcDataManagementRulesDTO rules : docDTO.getRules()) {
        if (rules.getBasePath().equals(basePath)) {
          return rules;
        }
      }
    }
    return null;
  }


  public static HpcCollectionListDTO getCollection(String token, String hpcCollectionlURL,
      String path, boolean list, String hpcCertPath, String hpcCertPassword) {
    return getCollection(token, hpcCollectionlURL, path, false, list, hpcCertPath, hpcCertPassword);
  }


  public static HpcCollectionListDTO getCollection(String token, String hpcCollectionlURL,
      String path, boolean children, boolean list, String hpcCertPath, String hpcCertPassword) {
    try {
      String theUrl = buildServiceUrl(hpcCollectionlURL, path);
      if (children) {
        theUrl = buildServiceUrl(theUrl, new String[] { "children" });
      }
      else {
        theUrl = buildServiceUrlWithQueryString(theUrl, "list",
          Boolean.valueOf(list));
      }
      WebClient client = HpcClientUtil.getWebClient(theUrl, hpcCertPath,
        hpcCertPassword, Optional.of(token));
      Response restResponse = client.get();
      // System.out.println("restResponse.getStatus():"
      // +restResponse.getStatus());
      if (isResponseOk(restResponse)) {
        HpcCollectionListDTO collections = (HpcCollectionListDTO)
            parseBasedOnConf((InputStream) restResponse.getEntity(),
                HpcCollectionListDTO.class);
        return collections;
      } else {
        throw new HpcWebException("Failed to get collection! No READ access!");
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(path.concat(":")));
    }
  }


  public static boolean createBookmark(String token, String hpcBookmarkURL,
      HpcBookmarkRequestDTO hpcBookmark, String hpcBookmarkName, String hpcCertPath,
      String hpcCertPassword) {
    try {
      final String url2Invoke = buildServiceUrl(hpcBookmarkURL, new String[]
        {hpcBookmarkName});
      WebClient client = HpcClientUtil.getWebClient(url2Invoke, hpcCertPath,
        hpcCertPassword, Optional.of(token));
      Response restResponse = client.put(hpcBookmark);
      if (isResponseCreated(restResponse)) {
        return true;
      } else {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Failed to create bookmark: "));
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(
        e, Optional.of("Failed to create bookmark due to: "));
    }
  }


  public static boolean createCollection(String token, String hpcCollectionURL,
      HpcCollectionRegistrationDTO collectionDTO, String path, String hpcCertPath,
      String hpcCertPassword) {
    try {
      HpcCollectionListDTO collection =
          getCollection(token, hpcCollectionURL, path, false, hpcCertPath, hpcCertPassword);
      if (collection != null && collection.getCollectionPaths() != null
          && collection.getCollectionPaths().size() > 0)
        throw new HpcWebException("Failed to create. Collection already exists: " + path);
      WebClient client =
        HpcClientUtil.getWebClient(buildServiceUrl(hpcCollectionURL, path),
        hpcCertPath, hpcCertPassword, Optional.of(token));
      Response restResponse = client.put(collectionDTO);
      if (isResponseCreated(restResponse)) {
        return true;
      } else {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Failed to create collection: "));
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e,
        Optional.of("Failed to create collection due to: "));
    }
  }


  public static HpcGroupMembersResponseDTO createGroup(String token, String hpcGroupURL,
      HpcGroupMembersRequestDTO groupDTO, String groupName, String hpcCertPath,
      String hpcCertPassword) {
    HpcGroupMembersResponseDTO response = null;
    try {
      final String url2Invoke = buildServiceUrl(hpcGroupURL, new String[]
        {groupName});
      WebClient client = HpcClientUtil.getWebClient(url2Invoke, hpcCertPath,
        hpcCertPassword, Optional.of(token));
      Response restResponse = client.put(groupDTO);
      if (isResponseCreated(restResponse)) {
        response = (HpcGroupMembersResponseDTO) parseBasedOnConf(
            (InputStream) restResponse.getEntity(),
            HpcGroupMembersResponseDTO.class);
      } else {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Failed to create group: "));
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e,
        Optional.of("Failed to create group due to: "));
    }
    return response;
  }


  public static boolean createUser(String token, String hpcUserURL, HpcUserRequestDTO userDTO,
      String userId, String hpcCertPath, String hpcCertPassword) {
    try {
      final String url2Invoke = buildServiceUrl(hpcUserURL, new String[]
        {userId});
      WebClient client = HpcClientUtil.getWebClient(url2Invoke, hpcCertPath,
        hpcCertPassword, Optional.of(token));
      Response restResponse = client.put(userDTO);
      if (isResponseCreated(restResponse)) {
        return true;
      } else {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Failed to create user: "));
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e,
        Optional.of("Failed to create User due to: "));
    }
  }


  public static boolean deleteBookmark(String token, String hpcBookmarkURL, String hpcBookmarkName,
      String hpcCertPath, String hpcCertPassword) {
    try {
      final String url2Invoke = buildServiceUrl(hpcBookmarkURL, new String[]
        {hpcBookmarkName});
      WebClient client = HpcClientUtil.getWebClient(url2Invoke,
          hpcCertPath, hpcCertPassword, Optional.of(token));
      Response restResponse = client.delete();
      if (isResponseOk(restResponse)) {
        return true;
      } else {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Failed to delete bookmark: "));
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e,
        Optional.of("Failed to delete bookmark due to: "));
    }
  }


  public static boolean deleteCollection(String token, String hpcCollectionURL,
      String collectionPath, String hpcCertPath, String hpcCertPassword) {
    try {
      final String url2Invoke = buildServiceUrl(hpcCollectionURL, collectionPath);
      WebClient client = HpcClientUtil.getWebClient(url2Invoke, hpcCertPath,
        hpcCertPassword, Optional.of(token));
      Response restResponse = client.delete();
      if (isResponseOk(restResponse)) {
        return true;
      } else {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Failed to delete collection: "));
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e,
        Optional.of("Failed to delete collection due to: "));
    }
  }


  public static boolean deleteDatafile(String token, String hpcDatafileURL, String path,
      String hpcCertPath, String hpcCertPassword) {
    try {
      final String url2Invoke = buildServiceUrl(hpcDatafileURL, path);
      WebClient client =
        HpcClientUtil.getWebClient(url2Invoke, hpcCertPath, hpcCertPassword,
        Optional.of(token));
      Response restResponse = client.delete();
      if (isResponseOk(restResponse)) {
        return true;
      } else {
        throw genHpcWebExceptionFromResponse(restResponse, Optional.empty());
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e, Optional.empty());
    }
  }


  public static boolean deleteGroup(String token, String hpcUserURL, String groupName,
      String hpcCertPath, String hpcCertPassword) {
    try {
      final String url2Invoke = buildServiceUrl(hpcUserURL, new String[]
        {groupName});
      WebClient client =
        HpcClientUtil.getWebClient(url2Invoke, hpcCertPath, hpcCertPassword,
        Optional.of(token));
      Response restResponse = client.delete();
      if (isResponseOk(restResponse)) {
        return true;
      } else {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Failed to delete group: "));
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e,
        Optional.of("Failed to delete group due to: "));
    }
  }


  public static boolean deleteSearch(String token, String hpcSavedSearchURL, String searchName,
      String hpcCertPath, String hpcCertPassword) {
    try {
      final String theUrl = buildServiceUrl(hpcSavedSearchURL, new String[]
        {searchName});
      WebClient client = HpcClientUtil.getWebClient(theUrl, hpcCertPath,
        hpcCertPassword, Optional.of(token));
      Response restResponse = client.delete();
      if (isResponseOk(restResponse)) {
        return true;
      } else {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Failed to delete saved search: "));
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e,
        Optional.of("Failed to delete saved search due to: "));
    }
  }


  public static AjaxResponseBody downloadDataFile(String token, String serviceURL,
      HpcDownloadRequestDTO dto, String hpcCertPath, String hpcCertPassword)
      throws JsonParseException, IOException {
    AjaxResponseBody result = new AjaxResponseBody();
    WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcCertPath,
      hpcCertPassword, Optional.of(token));
    Response restResponse = client.post(dto);
    if (isResponseOk(restResponse)) {
      HpcDataObjectDownloadResponseDTO downloadDTO =
          (HpcDataObjectDownloadResponseDTO) HpcClientUtil.getObject(restResponse,
              HpcDataObjectDownloadResponseDTO.class);
      String taskId = "Unknown";
      if (downloadDTO != null)
        taskId = downloadDTO.getTaskId();
      result.setMessage(
          "<strong>Asynchronous download request is submitted successfully! <br>TaskId: " + taskId
              + "<strong>");
      return result;
    } else {
      try {
        HpcExceptionDTO exception = (HpcExceptionDTO) parseBasedOnConf(
            (InputStream) restResponse.getEntity(), HpcExceptionDTO.class);
        result.setMessage("Download request is not successfull: " + exception.getMessage());
        return result;
      } catch (Exception e) {
        result.setMessage("Download request is not successfull: " + e.getMessage());
        return result;
      }
    }
  }


  public static HpcBulkDataObjectDownloadResponseDTO downloadFiles(String token, String hpcQueryURL,
      HpcBulkDataObjectDownloadRequestDTO dto, String hpcCertPath, String hpcCertPassword) {
    HpcBulkDataObjectDownloadResponseDTO response = null;
    try {
      WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath,
        hpcCertPassword, Optional.of(token));
      Response restResponse = client.post(dto);
      if (isResponseOk(restResponse)) {
        response = (HpcBulkDataObjectDownloadResponseDTO) parseBasedOnConf(
            (InputStream) restResponse.getEntity(),
            HpcBulkDataObjectDownloadResponseDTO.class);
      } else {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Failed to submit download request: "));
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e,
        Optional.of("Failed to submit download request: "));
    }
    return response;
  }


  public static String encode(String strVal) throws UnsupportedEncodingException {
    if (strVal == null) {
      return null;
    }
    else if (strVal.indexOf("/") == -1) {
      return strVal;
    }
    else {
      final StringBuffer encodedStr = new StringBuffer();
      final StringTokenizer tokens = new StringTokenizer(strVal, "/");
      while (tokens.hasMoreTokens()) {
        String token = tokens.nextToken();
        encodedStr.append(MiscUtil.performUrlEncoding(token));
        if (tokens.hasMoreTokens()) {
          encodedStr.append("/");
        }
      }
      return encodedStr.toString();
    }
  }


  public static HpcBookmarkListDTO getBookmarks(String token, String hpcBookmarkURL,
      String hpcCertPath, String hpcCertPassword) {
    WebClient client = HpcClientUtil.getWebClient(hpcBookmarkURL, hpcCertPath,
      hpcCertPassword, Optional.of(token));
    Response restResponse = client.get();
    if (isResponseNullOrNotOk(restResponse)) {
      return null;
    }
    try {
      HpcBookmarkListDTO bookmarkListDto = (HpcBookmarkListDTO)
          parsePlainly((InputStream) restResponse.getEntity(),
              HpcBookmarkListDTO.class);
      return bookmarkListDto;
    } catch (IOException e) {
      throw dumpStackTraceAndWrapException(e,
        Optional.of("Failed to get Bookmarks due to: "));
    }
  }


  public static HpcDataObjectListDTO getDatafiles(String token, String hpcDatafileURL, String path,
      boolean list, String hpcCertPath, String hpcCertPassword) {
    try {
      final String apiServiceUrl = buildServiceUrlWithQueryString(
        buildServiceUrl(hpcDatafileURL, path), "list", Boolean.valueOf(list));
      WebClient client = HpcClientUtil.getWebClient(apiServiceUrl, hpcCertPath,
        hpcCertPassword, Optional.of(token));
      Response restResponse = client.get();
      // System.out.println("restResponse.getStatus():"
      // +restResponse.getStatus());
      if (isResponseOk(restResponse)) {
        HpcDataObjectListDTO datafiles = (HpcDataObjectListDTO)
            parseBasedOnConf((InputStream) restResponse.getEntity(),
                HpcDataObjectListDTO.class);
        return datafiles;
      } else {
        throw new HpcWebException(
            "Failed to get Data file! It could be because you don't have READ access!");
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e,
          Optional.of(path.concat(" : ")));
    }
  }


  public static HpcDataObjectDownloadStatusDTO getDataObjectDownloadTask(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {
    WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath,
      hpcCertPassword, Optional.of(token));
    Response restResponse = client.get();
    if (isResponseNullOrNotOk(restResponse)) {
      return null;
    }
    try {
      return (HpcDataObjectDownloadStatusDTO) parsePlainly(
          (InputStream) restResponse.getEntity(),
          HpcDataObjectDownloadStatusDTO.class);
    } catch (IOException e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get data object download tasks details due to: "));
    }
  }


  public static HpcCollectionDownloadStatusDTO getDataObjectsDownloadTask(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {
    WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath,
      hpcCertPassword, Optional.of(token));
    Response restResponse = client.get();
    if (isResponseNullOrNotOk(restResponse)) {
      return null;
    }
    try {
      return (HpcCollectionDownloadStatusDTO) parsePlainly(
          (InputStream) restResponse.getEntity(),
          HpcCollectionDownloadStatusDTO.class);
    } catch (IOException e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get data objects download tasks details due to: "));
    }
  }


  public static HpcBulkDataObjectRegistrationStatusDTO getDataObjectRegistrationTask(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {
    WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath,
      hpcCertPassword, Optional.of(token));
    Response restResponse = client.get();
    if (isResponseNullOrNotOk(restResponse)) {
      return null;
    }
    try {
      return (HpcBulkDataObjectRegistrationStatusDTO) parsePlainly(
          (InputStream) restResponse.getEntity(),
          HpcBulkDataObjectRegistrationStatusDTO.class);
    } catch (IOException e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get data object registration tasks details due to: "));
    }
  }


  public static HpcDataManagementModelDTO getDOCModel(String token, String hpcModelURL,
      String hpcCertPath, String hpcCertPassword) {
    WebClient client = HpcClientUtil.getWebClient(hpcModelURL, hpcCertPath,
      hpcCertPassword, Optional.of(token));
    Response restResponse = client.get();
    if (isResponseNullOrNotOk(restResponse)) {
      return null;
    }
    try {
      HpcDataManagementModelDTO dto = (HpcDataManagementModelDTO) parsePlainly(
          (InputStream) restResponse.getEntity(),
          HpcDataManagementModelDTO.class);
      return dto;
    } catch (IOException e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get DOC Model due to: "));
    }
  }


  public static List<String> getDOCs(String token, String hpcModelURL, String hpcCertPath,
      String hpcCertPassword, HttpSession session) {
    List<String> docs = new ArrayList<String>();
    HpcDataManagementModelDTO modelDTO =
        (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
    if (modelDTO == null) {
      HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
      modelDTO = HpcClientUtil.getDOCModel(token, hpcModelURL, hpcCertPath, hpcCertPassword);
      if (modelDTO != null) {
        session.setAttribute("userDOCModel", modelDTO);
      }
    }
    for (HpcDocDataManagementRulesDTO docDTO : modelDTO.getDocRules()) {
      docs.add(docDTO.getDoc());
    }
    return docs;
  }


  public static HpcDownloadSummaryDTO getDownloadSummary(String token, String hpcQueryURL,
      String hpcCertPath, String hpcCertPassword) {
    WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath,
      hpcCertPassword, Optional.of(token));
    Response restResponse = client.get();
    if (isResponseNullOrNotOk(restResponse)) {
      return null;
    }
    try {
      return (HpcDownloadSummaryDTO) parseBasedOnConf(
          (InputStream) restResponse.getEntity(), HpcDownloadSummaryDTO.class);
    } catch (IllegalStateException | IOException e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get download tasks list due to: "));
    }
  }


  public static HpcGroupListDTO getGroups(String token, String hpcGroupURL, String groupName,
      String hpcCertPath, String hpcCertPassword) {
    try {
      final String apiServiceUrl = isStringBlank(groupName) ? hpcGroupURL :
        buildServiceUrlWithQueryString(hpcGroupURL, "groupPattern", groupName);
      WebClient client =
        HpcClientUtil.getWebClient(apiServiceUrl, hpcCertPath, hpcCertPassword,
        Optional.of(token));
      Response restResponse = client.get();
      if (isResponseOk(restResponse)) {
        HpcGroupListDTO groups = (HpcGroupListDTO) parseBasedOnConf(
            (InputStream) restResponse.getEntity(), HpcGroupListDTO.class);
        return groups;
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get Users due to: "));
    }
    return null;
  }


  public static HpcMetadataAttributesListDTO getMetadataAttrNames(String token,
      String hpcMetadataAttrsURL, String hpcCertPath, String hpcCertPassword) {
    String url = hpcMetadataAttrsURL;
    WebClient client = HpcClientUtil.getWebClient(url, hpcCertPath,
      hpcCertPassword, Optional.of(token));
    Response restResponse = client.get();
    if (isResponseNullOrNotOk(restResponse)) {
      return null;
    }
    try {
      return (HpcMetadataAttributesListDTO) parsePlainly(
          (InputStream) restResponse.getEntity(),
          HpcMetadataAttributesListDTO.class);
    } catch (IOException e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get Metadata attributes: due to: "));
    }
  }


  public static HpcNotificationDeliveryReceiptListDTO getNotificationReceipts(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {
    WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath,
      hpcCertPassword, Optional.of(token));
    Response restResponse = client.get();
    if (isResponseNullOrNotOk(restResponse)) {
      return null;
    }
    try {
      return (HpcNotificationDeliveryReceiptListDTO) parsePlainly(
          (InputStream) restResponse.getEntity(),
          HpcNotificationDeliveryReceiptListDTO.class);
    } catch (IOException e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get notification receipts due to: "));
    }
  }


  public static <T> Object getObject(Response response, Class<T> objectClass) {
    JsonParser parser;
    try {
      parser = MY_CONFIGURED_JSON_MAPPING_FACTORY.createParser(
          (InputStream) response.getEntity());
      return parser.readValueAs(objectClass);
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to parse object: "));
    }
  }


  public static Optional<String> getPathElementType(
      String argAuthToken, String argServiceUrlPrefix, String argItemPath,
      String argSslCertPath, String argSslCertPasswd)
      throws HpcWebException {
    Optional<String> elemType = Optional.empty();
    try {
      String theItemPath = argItemPath.trim();
      final String hpcServiceUrl = buildServiceUrl(argServiceUrlPrefix,
        theItemPath);
      final WebClient client = HpcClientUtil.getWebClient(hpcServiceUrl,
          argSslCertPath, argSslCertPasswd, Optional.of(argAuthToken));
      final Response restResponse = client.get();
      if (isResponseOk(restResponse)) {
        elemType = extractElementTypeFromResponse(restResponse);
      } else {
        final String extractedErrMsg =
            genHpcExceptionDtoFromResponse(restResponse).getMessage();
        throw new HpcWebException(String.format(
            ERR_MSG_TEMPLATE__FAILED_GET_PATH_ELEM_TYPE,
            theItemPath,
            extractedErrMsg
        ));
      }

      return elemType;
    } catch (IllegalStateException | IOException e) {
      final String msgForHpcWebException = String.format(
          ERR_MSG_TEMPLATE__FAILED_GET_PATH_ELEM_TYPE,
          argItemPath,
          e.getMessage()
      );
      throw dumpStackTraceAndWrapException(e, Optional.of(
        msgForHpcWebException));
    }
  }


  public static HpcUserPermissionDTO getPermissionForUser(String token, String path, String userId,
      String hpcServiceURL, String hpcCertPath, String hpcCertPassword) {
    final String apiServiceUrl = buildServiceUrl(buildServiceUrl(hpcServiceURL,
      path), "acl", "user", userId);
    WebClient client = HpcClientUtil.getWebClient(apiServiceUrl, hpcCertPath,
      hpcCertPassword, Optional.of(token));
    Response restResponse = client.get();
    if (isResponseNullOrNotOk(restResponse))
      return null;
    try {
      return (HpcUserPermissionDTO) parsePlainly(
          (InputStream) restResponse.getEntity(), HpcUserPermissionDTO.class);
    } catch (IOException e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get permission due to: "));
    }
  }


  public static HpcUserPermsForCollectionsDTO getPermissionForCollections(String token,
      String hpcServiceURL, String hpcCertPath, String hpcCertPassword) {
    WebClient client =
      HpcClientUtil.getWebClient(hpcServiceURL, hpcCertPath, hpcCertPassword,
      Optional.of(token));
    Response restResponse = client.get();
    if (isResponseNullOrNotOk(restResponse))
      return null;
    try {
      return (HpcUserPermsForCollectionsDTO) parsePlainly((InputStream)
          restResponse.getEntity(), HpcUserPermsForCollectionsDTO.class);
    } catch (IOException e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get permission due to: "));
    }
  }


  public static HpcEntityPermissionsDTO getPermissions(String token, String hpcServiceURL,
      String hpcCertPath, String hpcCertPassword) {
    WebClient client = HpcClientUtil.getWebClient(hpcServiceURL, hpcCertPath,
      hpcCertPassword, Optional.of(token));
    Response restResponse = client.get();
    if (isResponseNullOrNotOk(restResponse))
      return null;
    try {
      return (HpcEntityPermissionsDTO) parsePlainly(
          (InputStream) restResponse.getEntity(), HpcEntityPermissionsDTO.class);
    } catch (IOException e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get notification receipts due to: "));
    }
  }


  public static HpcNamedCompoundMetadataQueryDTO getQuery(String token, String hpcQueryURL,
      String queryName, String hpcCertPath, String hpcCertPassword) {
    final String serviceURL = buildServiceUrl(hpcQueryURL, new String[]
      {queryName});
    WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcCertPath,
      hpcCertPassword, Optional.of(token));
    Response restResponse = client.get();
    if (isResponseNullOrNotOk(restResponse))
      return null;
    try {
      return (HpcNamedCompoundMetadataQueryDTO) parsePlainly(
          (InputStream) restResponse.getEntity(),
          HpcNamedCompoundMetadataQueryDTO.class);
    } catch (IOException e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get Query for: " + queryName + " due to: "));
    }
  }


  public static HpcRegistrationSummaryDTO getRegistrationSummary(String token, String hpcQueryURL,
      String hpcCertPath, String hpcCertPassword) {
    WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath,
      hpcCertPassword, Optional.of(token));
    Response restResponse = client.get();
    if (isResponseNullOrNotOk(restResponse))
      return null;
    try {
      return (HpcRegistrationSummaryDTO) parseBasedOnConf(
          (InputStream) restResponse.getEntity(),
          HpcRegistrationSummaryDTO.class);
    } catch (IllegalStateException | IOException e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get registration tasks list due to: "));
    }
  }


  public static RestTemplate getRestTemplate(String hpcCertPath, String hpcCertPassword) {

    RestTemplate restTemplate = null;
    FileInputStream fis = null;
    try {
      if (hpcCertPath != null && hpcCertPassword != null) {
        fis = new java.io.FileInputStream(hpcCertPath);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(fis, hpcCertPassword.toCharArray());
        KeyManagerFactory kmf =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, hpcCertPassword.toCharArray());
        KeyManager[] keyManagers = kmf.getKeyManagers();

        TrustManagerFactory tmf =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        TrustManager[] trustManagers = tmf.getTrustManagers();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);

        CloseableHttpClient httpClient = HttpClients.custom()
            .setSSLHostnameVerifier(new NoopHostnameVerifier()).setSSLContext(sslContext).build();

        HttpComponentsClientHttpRequestFactory requestFactory =
            new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        restTemplate = new RestTemplate(requestFactory);
      } else {
        @SuppressWarnings("deprecation")
        SSLContextBuilder builder = new SSLContextBuilder();
        builder.loadTrustMaterial(null, new TrustStrategy() {
          @Override
          public boolean isTrusted(X509Certificate[] chain, String authType)
              throws CertificateException {
            return true;
          }
        });

        SSLConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(builder.build(),
            SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslSF).build();
        HttpComponentsClientHttpRequestFactory requestFactory =
            new HttpComponentsClientHttpRequestFactory(httpClient);
        restTemplate = new RestTemplate(requestFactory);
      }
      List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
      messageConverters.add(new FormHttpMessageConverter());
      messageConverters.add(new SourceHttpMessageConverter<Source>());
      messageConverters.add(new StringHttpMessageConverter());
      messageConverters.add(new MappingJackson2HttpMessageConverter());
      messageConverters.add(new MultipartAwareFormHttpMessageConverter());
      restTemplate.setMessageConverters(messageConverters);

      restTemplate.setErrorHandler(new HpcResponseErrorHandler());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (CertificateException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (KeyStoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (UnrecoverableKeyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (KeyManagementException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

    return restTemplate;
  }


  public static HpcNamedCompoundMetadataQueryListDTO getSavedSearches(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {
    WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath,
      hpcCertPassword, Optional.of(token));
    Response restResponse = client.get();
    if (isResponseNullOrNotOk(restResponse))
      return null;
    try {
      return (HpcNamedCompoundMetadataQueryListDTO) parsePlainly(
          (InputStream) restResponse.getEntity(),
          HpcNamedCompoundMetadataQueryListDTO.class);
    } catch (IOException e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get saved queries due to: "));
    }
  }


  public static List<HpcDataManagementRulesDTO> getUserDOCManagementRules(
      HpcDataManagementModelDTO docModelDto, String userDoc) {
    if (docModelDto == null || docModelDto.getDocRules() == null) {
      return null;
    }
    for (HpcDocDataManagementRulesDTO docDTO : docModelDto.getDocRules()) {
      if (docDTO.getDoc().equals(userDoc)) {
        return docDTO.getRules();
      }
    }
    return null;
  }


  public static HpcUserDTO getUser(String token, String hpcUserURL, String hpcCertPath,
      String hpcCertPassword) {
    try {
      WebClient client = HpcClientUtil.getWebClient(hpcUserURL, hpcCertPath,
        hpcCertPassword, Optional.of(token));
      Response restResponse = client.get();
      if (isResponseOk(restResponse)) {
        HpcUserDTO userDto = (HpcUserDTO) parseBasedOnConf(
            (InputStream) restResponse.getEntity(), HpcUserDTO.class);
        return userDto;
      } else {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Failed to get user: "));
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get User due to: "));
    }
  }


  public static HpcUserDTO getUserByAdmin(String token, String hpcUserURL, String userId,
      String hpcCertPath, String hpcCertPassword) {
    try {
      final String apiServiceUrl = buildServiceUrl(hpcUserURL, new String[]
        {userId});
      WebClient client =
        HpcClientUtil.getWebClient(apiServiceUrl, hpcCertPath, hpcCertPassword,
        Optional.of(token));
      Response restResponse = client.get();
      if (isResponseOk(restResponse)) {
        HpcUserDTO userDto = (HpcUserDTO) parseBasedOnConf(
            (InputStream) restResponse.getEntity(), HpcUserDTO.class);
        return userDto;
      } else {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Failed to get user: "));
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get User due to: "));
    }
  }


  public static HpcUserListDTO getUsers(String token, String hpcUserURL, String userId,
      String firstName, String lastName, String doc, String hpcCertPath, String hpcCertPassword) {
    try {
      final MultiValueMap<String, String> myQueryParams = new
        LinkedMultiValueMap<>();
      if (!isStringBlank(userId)) {
        myQueryParams.set("nciUserId", userId);
      }
      if (!isStringBlank(firstName)) {
        myQueryParams.set("firstNamePattern", firstName);
      }
      if (!isStringBlank(lastName)) {
        myQueryParams.set("lastNamePattern", lastName);
      }
      if (!isStringBlank(doc)) {
        myQueryParams.set("doc", doc);
      }
      final String apiServiceUrl = buildServiceUrlWithQueryString(hpcUserURL,
        myQueryParams);
      WebClient client =
        HpcClientUtil.getWebClient(apiServiceUrl, hpcCertPath, hpcCertPassword,
        Optional.of(token));
      Response restResponse = client.get();
      if (isResponseOk(restResponse)) {
        HpcUserListDTO users = (HpcUserListDTO) parseBasedOnConf(
            (InputStream) restResponse.getEntity(), HpcUserListDTO.class);
        return users;
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get Users due to: "));
    }
    return null;
  }


  public static void populateBasePaths(HttpSession session, Model model,
      HpcDataManagementModelDTO modelDTO, String authToken, String userId, String collectionURL,
      String sslCertPath, String sslCertPassword) throws HpcWebException {
    Set<String> basePaths = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    final MultiValueMap<String, String> paramsMap = new LinkedMultiValueMap<>();
    for (HpcDocDataManagementRulesDTO docRule : modelDTO.getDocRules()) {
      for (HpcDataManagementRulesDTO rule : docRule.getRules()) {
        paramsMap.set("collectionPath", rule.getBasePath());
      }
    }
    final String apiServiceUrl = buildServiceUrlWithQueryString(collectionURL,
      paramsMap);
    HpcUserPermsForCollectionsDTO permissions =
      HpcClientUtil.getPermissionForCollections(authToken, apiServiceUrl,
      sslCertPath, sslCertPassword);
    if (permissions != null) {
      for (HpcPermissionForCollection permission : permissions.getPermissionsForCollections()) {
        if (permission != null && permission.getPermission() != null
            && (HpcPermission.WRITE.equals(permission.getPermission())
            || HpcPermission.OWN.equals(permission.getPermission())))
          basePaths.add(permission.getCollectionPath());
      }
    }
    session.setAttribute("basePaths", basePaths);
  }


  public static HpcBulkDataObjectRegistrationResponseDTO registerBulkDatafiles(String token,
      String hpcDatafileURL, HpcBulkDataObjectRegistrationRequestDTO datafileDTO,
      String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = HpcClientUtil.getWebClient(hpcDatafileURL, hpcCertPath,
        hpcCertPassword, Optional.of(token));
      Response restResponse = client.put(datafileDTO);
      if (isResponseCreated(restResponse) || isResponseOk(restResponse)) {
        return (HpcBulkDataObjectRegistrationResponseDTO) HpcClientUtil.getObject(restResponse,
            HpcBulkDataObjectRegistrationResponseDTO.class);
      } else {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Failed to bulk register data files: "));
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to bulk register data files due to: "));
    }
  }


  public static boolean registerDatafile(String token, MultipartFile hpcDatafile,
      String hpcDatafileURL, HpcDataObjectRegistrationRequestDTO datafileDTO, String path,
      String hpcCertPath, String hpcCertPassword) {
    try {
      try {
        HpcDataObjectListDTO datafile =
            getDatafiles(token, hpcDatafileURL, path, false, hpcCertPath, hpcCertPassword);
        if (datafile != null && datafile.getDataObjectPaths() != null
            && datafile.getDataObjectPaths().size() > 0)
          throw new HpcWebException("Failed to create. Data file already exists: " + path);
      } catch (HpcWebException e) {
        // Data file is not there!
      }
      final String apiServiceUrl = buildServiceUrl(hpcDatafileURL, path);
      WebClient client =
        HpcClientUtil.getWebClient(apiServiceUrl, hpcCertPath, hpcCertPassword,
        Optional.of(token));
      client.type(MediaType.MULTIPART_FORM_DATA_VALUE).accept(MediaType.APPLICATION_JSON_VALUE);
      List<Attachment> atts = new LinkedList<Attachment>();
      atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObjectRegistration",
          MediaType.APPLICATION_JSON.toString(), datafileDTO));
      // InputStream inputStream = new BufferedInputStream(
      // new FileInputStream(datafileDTO.getSource().getFileId()));
      ContentDisposition cd2 =
          new ContentDisposition("attachment;filename=" + hpcDatafile.getName());
      atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObject",
          hpcDatafile.getInputStream(), cd2));
      Response restResponse = client.put(new MultipartBody(atts));
      if (isResponseCreated(restResponse)) {
        return true;
      } else {
        throw genHpcWebExceptionFromResponse(restResponse, Optional.empty());
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e, Optional.empty());
    }
  }


  public static boolean updateCollection(String token, String hpcCollectionURL,
      HpcCollectionRegistrationDTO collectionDTO, String path, String hpcCertPath,
      String hpcCertPassword) {
    try {
      final String apiServiceUrl = buildServiceUrl(hpcCollectionURL, path);
      WebClient client =
        HpcClientUtil.getWebClient(apiServiceUrl, hpcCertPath, hpcCertPassword,
        Optional.of(token));
      Response restResponse = client.put(collectionDTO);
      if (isResponseOk(restResponse) || isResponseCreated(restResponse)) {
        return true;
      } else {
        throw genHpcWebExceptionFromResponse(restResponse, Optional.empty());
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e, Optional.empty());
    }
  }


  public static boolean updateDatafile(String token, String hpcDatafileURL,
      HpcDataObjectRegistrationRequestDTO datafileDTO, String path, String hpcCertPath,
      String hpcCertPassword) {
    try {
      WebClient client =
        HpcClientUtil.getWebClient(buildServiceUrl(hpcDatafileURL, path),
        hpcCertPath, hpcCertPassword, Optional.of(token));
      client.type(MediaType.MULTIPART_FORM_DATA_VALUE).accept(MediaType.APPLICATION_JSON_VALUE);
      List<Attachment> atts = new LinkedList<Attachment>();
      atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObjectRegistration",
          MediaType.APPLICATION_JSON.toString(), datafileDTO));
      Response restResponse = client.put(new MultipartBody(atts));
      if (isResponseOk(restResponse)) {
        return true;
      } else {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Failed to update data file: "));
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to update data file due to: "));
    }
  }


  public static HpcGroupMembersResponseDTO updateGroup(String token, String hpcUserURL,
      HpcGroupMembersRequestDTO groupDTO, String groupName, String hpcCertPath,
      String hpcCertPassword) {
    HpcGroupMembersResponseDTO response = null;
    try {
      WebClient client =
        HpcClientUtil.getWebClient(buildServiceUrl(hpcUserURL, new String[]
        {groupName}), hpcCertPath, hpcCertPassword, Optional.of(token));
      Response restResponse = client.post(groupDTO);
      if (isResponseOk(restResponse)) {
        response = (HpcGroupMembersResponseDTO) parseBasedOnConf(
            (InputStream) restResponse.getEntity(),
            HpcGroupMembersResponseDTO.class);
      } else {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Failed to update group: "));
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to update group due to: "));
    }
    return response;
  }


  public static boolean updateUser(String token, String hpcUserURL, HpcUserRequestDTO userDTO,
      String userId, String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client =
        HpcClientUtil.getWebClient(buildServiceUrl(hpcUserURL, new String[]
        {userId}), hpcCertPath, hpcCertPassword, Optional.of(token));
      Response restResponse = client.post(userDTO);
      if (isResponseOk(restResponse)) {
        return true;
      } else {
        throw genHpcWebExceptionFromResponse(
            restResponse, Optional.of("Failed to update user: "));
      }
    } catch (Exception e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to update user due to: "));
    }
  }


  public static HpcNotificationSubscriptionListDTO getUserNotifications(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {
    WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath,
      hpcCertPassword, Optional.of(token));
    Response restResponse = client.get();
    if (isResponseNullOrNotOk(restResponse))
      return null;
    try {
      return (HpcNotificationSubscriptionListDTO) parsePlainly(
          (InputStream) restResponse.getEntity(),
          HpcNotificationSubscriptionListDTO.class);
    } catch (IOException e) {
      throw dumpStackTraceAndWrapException(e, Optional.of(
        "Failed to get notification subscriptions due to: "));
    }
  }


  public static WebClient getWebClient(String url, String hpcCertPath, String hpcCertPassword) {
    WebClient client = WebClient.create(url, Collections.singletonList(new JacksonJsonProvider()));
    WebClient.getConfig(client).getRequestContext().put("support.type.as.multipart", "true");
    WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(60000000);
    WebClient.getConfig(client).getHttpConduit().getClient().setConnectionTimeout(60000000);
    HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();

    TLSClientParameters params = conduit.getTlsClientParameters();
    if (params == null) {
      params = new TLSClientParameters();
      conduit.setTlsClientParameters(params);
    }

    params.setTrustManagers(new TrustManager[] {new TrustAllX509TrustManager()});
    conduit.setTlsClientParameters(params);

    params.setDisableCNCheck(true);
    return client;
  }


  public static WebClient getWebClient(String url, String hpcCertPath,
    String hpcCertPassword, Optional<String> hpcAuthToken) {
    final WebClient retWebClientObj = getWebClient(url, hpcCertPath,
      hpcCertPassword);
    if (hpcAuthToken.isPresent()) {
      retWebClientObj.header(HttpHeaders.AUTHORIZATION,
        BEARER + hpcAuthToken.get());
    }
    return retWebClientObj;
  }


  private static String buildServiceUrl(String argBaseUrl,
      String argPathFromBase) {
    final UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(
        argBaseUrl);
    ucBuilder.path(argPathFromBase);
    return ucBuilder.toUriString();
  }


  private static String buildServiceUrl(String argBaseUrl,
      String ... argPathSegmensFromBase) {
    final UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(
        argBaseUrl);
    ucBuilder.pathSegment(argPathSegmensFromBase);
    return ucBuilder.toUriString();
  }


  private static String buildServiceUrlWithQueryString(String argBaseUrl,
      MultiValueMap<String,String> argParamsMap) {
    final UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(
        argBaseUrl);
    ucBuilder.queryParams(argParamsMap);
    return ucBuilder.toUriString();
  }


  private static String buildServiceUrlWithQueryString(String argBaseUrl,
    String argQueryParam, Object argParamVal) {
    final UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(
      argBaseUrl);
    ucBuilder.queryParam(argQueryParam, argParamVal);
    return ucBuilder.toUriString();
  }


  private static HpcWebException dumpStackTraceAndWrapException(
      Exception argException, Optional<String> argMsgStart) {
    if (argException instanceof HpcWebException) {
      return (HpcWebException) argException;
    } else {
      argException.printStackTrace();
      return new HpcWebException(
          argMsgStart.orElse("").concat(argException.getMessage()),
          argException);
    }
  }


  private static void enableSSL() {
    TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}

      public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
    }};

    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    } catch (Exception e) {
    }
  }


  private static Optional<String> extractElementTypeFromResponse(
      Response restResponse) throws IOException {
    Optional<String> retVal = Optional.empty();
    final JsonParser parser = MY_PLAIN_JSON_MAPPING_FACTORY.createParser(
      (InputStream) restResponse.getEntity());
    while (null != parser.nextValue()) {
      if (JSON_RESPONSE_ATTRIB__ELEMENT_TYPE.equals(parser.getCurrentName())) {
        retVal = Optional.of(parser.getValueAsString());
        break;
      }
    }

    return retVal;
  }


  private static String generateQueryString(HpcDataManagementModelDTO argModelDTO) {
    final StringBuilder sb = new StringBuilder("?");
    boolean firstItemFlag = true;
    for (HpcDocDataManagementRulesDTO docRule : argModelDTO.getDocRules()) {
      for (HpcDataManagementRulesDTO rule : docRule.getRules()) {
        if (firstItemFlag) {
          firstItemFlag = false;
        } else {
          sb.append("&");
        }
        sb.append("collectionPath=")
          .append(MiscUtil.urlEncodeDmePath(rule.getBasePath()));
      }
    }
    final String queryParams = sb.toString(); //sb.substring(0, sb.length() - 1);
    return queryParams;
  }


  private static HpcExceptionDTO genHpcExceptionDtoFromResponse(
    Response restResponse) throws IOException {
    final HpcExceptionDTO hpcExceptionDto = (HpcExceptionDTO) parseBasedOnConf(
      (InputStream) restResponse.getEntity(), HpcExceptionDTO.class);
    return hpcExceptionDto;
  }


  private static HpcWebException genHpcWebExceptionFromResponse(
    Response restResponse, Optional<String> errMsgStart) throws IOException {
    final HpcExceptionDTO theDto = genHpcExceptionDtoFromResponse(restResponse);
    final String msg = errMsgStart.orElse("").concat(theDto.getMessage());

    return new HpcWebException(msg);
  }


  private static boolean isResponseNullOrNotOk(Response argResponse) {
    return (null == argResponse ||
      HttpServletResponse.SC_OK != argResponse.getStatus());
  }


  private static boolean isResponseCreated(Response argResponse) {
    return HttpServletResponse.SC_CREATED == argResponse.getStatus();
  }


  private static boolean isResponseOk(Response argResponse) {
    return HttpServletResponse.SC_OK == argResponse.getStatus();
  }


  private static boolean isStringBlank(String argString) {
    return (null == argString || argString.trim().isEmpty());
  }


  private static <T extends Serializable> Serializable parseBasedOnConf(
      InputStream argInputStream, Class<T> argClass) throws IOException {
    final Serializable parsingResult =
      MY_CONFIGURED_JSON_MAPPING_FACTORY.createParser(argInputStream)
      .readValueAs(argClass);

    return parsingResult;
  }


  private static <T extends Serializable> Serializable parsePlainly(
    InputStream argInputStream, Class<T> argClass) throws IOException {
    final Serializable parsingResult =
      MY_PLAIN_JSON_MAPPING_FACTORY.createParser(argInputStream)
      .readValueAs(argClass);

    return parsingResult;
  }

}
