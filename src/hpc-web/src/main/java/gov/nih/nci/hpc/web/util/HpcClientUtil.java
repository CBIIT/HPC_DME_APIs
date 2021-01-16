package gov.nih.nci.hpc.web.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermissionForCollection;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkListDTO;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationStatusDTO;
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
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMetadataAttributesListDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcRegistrationSummaryDTO;
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
import gov.nih.nci.hpc.web.HpcAuthorizationException;
import gov.nih.nci.hpc.web.HpcResponseErrorHandler;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
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
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import javax.xml.transform.Source;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
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
import org.codehaus.jackson.map.SerializationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.integration.http.converter.MultipartAwareFormHttpMessageConverter;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

public class HpcClientUtil {

  private static final String ERR_MSG_TEMPLATE__FAILED_GET_PATH_ELEM_TYPE =
    "Failed to determine type of DME entity at path, %s." +
    "  Exception message: %s.";

  private static final String JSON_RESPONSE_ATTRIB__ELEMENT_TYPE =
      "elementType";
  
  //The logger instance.
  private final static Logger logger = LoggerFactory.getLogger(HpcClientUtil.class);


  public static WebClient getWebClient(String url, String hpcCertPath,
    String hpcCertPassword) {

    org.codehaus.jackson.map.ObjectMapper mapper = new org.codehaus.jackson.map.ObjectMapper();
    SerializationConfig config = mapper.getSerializationConfig();
    config.set(SerializationConfig.Feature.WRITE_NULL_PROPERTIES, false);
    mapper.setSerializationConfig(config);
    
    WebClient client = WebClient.create(url, Collections.singletonList(
      new JacksonJsonProvider(mapper)));

    ClientConfiguration clientConfig = WebClient.getConfig(client);
    clientConfig.getRequestContext().put("support.type.as.multipart", "true");
    configureWebClientConduit(clientConfig);

    return client;
  }


  private static void configureWebClientConduit(
    ClientConfiguration clientConfig) {
    HTTPConduit conduit = clientConfig.getHttpConduit();

    TLSClientParameters tlsParams = conduit.getTlsClientParameters();
    if (null == tlsParams) {
      conduit.setTlsClientParameters(new TLSClientParameters());
      tlsParams = conduit.getTlsClientParameters();
    }
    tlsParams.setDisableCNCheck(true);
    tlsParams.setTrustManagers(new TrustManager[] { new
      TrustAllX509TrustManager() });

    conduit.getClient().setReceiveTimeout(60000000);
    conduit.getClient().setConnectionTimeout(60000000);
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
//      if (configurationId != null) {
        if (modelDTO != null) {
          // TODO
          for (HpcDocDataManagementRulesDTO rulesDTO : modelDTO.getDocRules()) {
            for (HpcDataManagementRulesDTO rule : rulesDTO.getRules()) {
              if ((configurationId != null && rule.getId().equals(configurationId)) || rule.getBasePath().equals(parent))
                return rule.getBasePath();
            }
          }
        }
 //     }
    }
    return null;

  }

  public static String getAuthenticationToken(String userId, String passwd, String hpcServerURL)
      throws HpcWebException {

    WebClient client = HpcClientUtil.getWebClient(hpcServerURL, null, null);
    String token = DatatypeConverter.printBase64Binary((userId + ":" + passwd).getBytes());
    client.header("Authorization", "Basic " + token);
    Response restResponse = client.get();
    try {

      if (restResponse.getStatus() != 200) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
        throw new HpcAuthorizationException("Authentication failed: " + exception.getMessage());
      }
      MappingJsonFactory factory = new MappingJsonFactory();
      JsonParser parser;
      parser = factory.createParser((InputStream) restResponse.getEntity());
      HpcAuthenticationResponseDTO dto = parser.readValueAs(HpcAuthenticationResponseDTO.class);
      return dto.getToken();
    } catch (IllegalStateException e1) {
      e1.printStackTrace();
      throw new HpcWebException("Failed to get auth token: " + e1.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get auth token: " + e.getMessage());
    }
  }
  
  public static String getAuthenticationTokenSso(String userId, String smSession, String hpcServerURL)
	      throws HpcWebException {
	  
	    WebClient client = HpcClientUtil.getWebClient(hpcServerURL, null, null);
	    Response restResponse = client.header("SM_USER", userId).header("NIHSMSESSION", smSession).get();
	    try {

	      if (restResponse.getStatus() != 200) {
	        ObjectMapper mapper = new ObjectMapper();
	        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
	            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
	            new JacksonAnnotationIntrospector());
	        mapper.setAnnotationIntrospector(intr);
	        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	        MappingJsonFactory factory = new MappingJsonFactory(mapper);
	        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

	        HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
	        throw new HpcAuthorizationException("Authentication failed: " + exception.getMessage());
	      }
	      MappingJsonFactory factory = new MappingJsonFactory();
	      JsonParser parser;
	      parser = factory.createParser((InputStream) restResponse.getEntity());
	      HpcAuthenticationResponseDTO dto = parser.readValueAs(HpcAuthenticationResponseDTO.class);
	      return dto.getToken();
	    } catch (IllegalStateException e1) {
	      e1.printStackTrace();
	      throw new HpcWebException("Failed to get auth token: " + e1.getMessage());
	    } catch (IOException e) {
	      e.printStackTrace();
	      throw new HpcWebException("Failed to get auth token: " + e.getMessage());
	    }
	  }

  public static List<HpcDataManagementRulesDTO> getUserDOCManagementRules(
      HpcDataManagementModelDTO docModelDto, String userDoc) {
    if (docModelDto == null || docModelDto.getDocRules() == null)
      return null;

    for (HpcDocDataManagementRulesDTO docDTO : docModelDto.getDocRules()) {
      if (docDTO.getDoc().equals(userDoc))
        return docDTO.getRules();
    }
    return null;
  }


  public static HpcDataManagementRulesDTO getBasePathManagementRules(
      HpcDataManagementModelDTO docModelDto, String basePath) {
    if (docModelDto == null || docModelDto.getDocRules() == null || basePath == null)
      return null;

    for (HpcDocDataManagementRulesDTO docDTO : docModelDto.getDocRules()) {
      for (HpcDataManagementRulesDTO rules : docDTO.getRules()) {
        if (rules.getBasePath().equals(basePath))
          return rules;
      }
    }
    return null;
  }
  
  public static String getDocByBasePath(
      HpcDataManagementModelDTO docModelDto, String basePath) {
    if (docModelDto == null || docModelDto.getDocRules() == null || basePath == null)
      return null;

    for (HpcDocDataManagementRulesDTO docDTO : docModelDto.getDocRules()) {
      for (HpcDataManagementRulesDTO rules : docDTO.getRules()) {
        if (rules.getBasePath().equals(basePath))
          return docDTO.getDoc();
      }
    }
    return null;
  }
  
  public static HpcDataManagementModelDTO getDOCModel(String token, String hpcModelURL,
      String hpcCertPath, String hpcCertPassword) {

    WebClient client = HpcClientUtil.getWebClient(hpcModelURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();

    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    MappingJsonFactory factory = new MappingJsonFactory();
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get DOC Model due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcDataManagementModelDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get DOC Model due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get DOC Model due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get DOC Model due to: " + e.getMessage());
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
      if (modelDTO != null)
        session.setAttribute("userDOCModel", modelDTO);
    }

    for (HpcDocDataManagementRulesDTO docDTO : modelDTO.getDocRules())
      docs.add(docDTO.getDoc());
    return docs;
  }

  public static String getBasePath(HttpServletRequest request) {
    String[] basePathValues = request.getParameterValues("basePath");
    String basePath = null;
    if (basePathValues == null || basePathValues.length == 0)
      basePath = (String) request.getAttribute("basePath");
    else
      basePath = basePathValues[0];
    if (basePath != null && basePath.equals("_select_null"))
      return null;
    return basePath;
  }


  public static Optional<String> getPathElementType(
      String argAuthToken, String argServiceUrlPrefix, String argItemPath,
      String argSslCertPath, String argSslCertPasswd)
      throws HpcWebException {
    Optional<String> elemType = Optional.empty();
    try {
      String theItemPath = argItemPath.trim();
      final String hpcServiceUrl = UriComponentsBuilder.fromHttpUrl(
        argServiceUrlPrefix).path("/{dme-archive-path}").buildAndExpand(
        theItemPath).encode().toUri().toURL().toExternalForm();

      final WebClient client = HpcClientUtil.getWebClient(hpcServiceUrl,
                                argSslCertPath, argSslCertPasswd);
//      client.header(HttpHeaders.AUTHORIZATION, "Basic " + argAuthToken);
      client.header("Authorization", "Bearer " + argAuthToken);
      final Response restResponse = client.get();
      if (restResponse.getStatus() == HttpServletResponse.SC_OK) {
        elemType = extractElementTypeFromResponse(restResponse);
      } else {
        final String extractedErrMsg =
            genHpcExceptionDtoOnNonOkRestResponse(restResponse).getMessage();
        throw new HpcWebException(
          extractedErrMsg
        );
      }

      return elemType;
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      final String msgForHpcWebException = String.format(
        ERR_MSG_TEMPLATE__FAILED_GET_PATH_ELEM_TYPE,
        argItemPath,
        e.getMessage()
      );
      throw new HpcWebException(msgForHpcWebException);
    }
  }


  public static HpcCollectionListDTO getCollection(String token, String hpcCollectionlURL,
	  String path, boolean list, String hpcCertPath, String hpcCertPassword) {
	return getCollection(token, hpcCollectionlURL, path, false, list, false, hpcCertPath, hpcCertPassword);
  }
	  
  
  public static HpcCollectionListDTO getCollection(String token, String hpcCollectionlURL,
      String path, boolean children, boolean list, String hpcCertPath, String hpcCertPassword) {
    return getCollection(token, hpcCollectionlURL, path, children, list, false, hpcCertPath, hpcCertPassword);
  }
  

  public static HpcCollectionListDTO getCollection(String token, String hpcCollectionlURL,
      String path, boolean children, boolean list, boolean includeAcl, String hpcCertPath, String hpcCertPassword) {
	  
	  try {
      final UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(
        hpcCollectionlURL).path("/{dme-archive-path}");
      if (children) {
        ucBuilder.pathSegment("children");
      } else {
        ucBuilder.queryParam("list", Boolean.toString(list));
      }
      ucBuilder.queryParam("includeAcl", Boolean.toString(includeAcl));
      final String serviceURL = ucBuilder.buildAndExpand(path).encode().toUri()
        .toURL().toExternalForm();
	  
      WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.invoke("GET", null);
      
      if (restResponse.getStatus() == 200) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        return parser.readValueAs(HpcCollectionListDTO.class);
      } else {
    	logger.error("Failed to get collection " + path);
        throw new HpcWebException("Failed to get collection " + path);
      }
    } catch (Exception e) {
      logger.error("Failed to get collection " + path + ": ", e);
      throw new HpcWebException("Failed to get collection " + path + ": " + e.getMessage());
    }
  }
  
  
  public static HpcDataObjectListDTO getDatafiles(String token, String hpcDatafileURL, String path,
	      boolean list, String hpcCertPath, String hpcCertPassword) {
	  return getDatafiles(token, hpcDatafileURL, path,
		      list, false, hpcCertPath,hpcCertPassword);
  }
  

  public static HpcDataObjectListDTO getDatafiles(String token, String hpcDatafileURL, String path,
    boolean list, boolean includeAcl, String hpcCertPath, String hpcCertPassword) {
    try {
      final String url2Apply = UriComponentsBuilder.fromHttpUrl(hpcDatafileURL)
        .path("/{dme-archive-path}").queryParam("list", Boolean.valueOf(list))
        .queryParam("includeAcl", Boolean.valueOf(includeAcl))
        .buildAndExpand(path).encode().toUri().toURL().toExternalForm();
      WebClient client = HpcClientUtil.getWebClient(url2Apply, hpcCertPath,
        hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("GET", null);
      // System.out.println("restResponse.getStatus():"
      // +restResponse.getStatus());
      if (restResponse.getStatus() == 200) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcDataObjectListDTO datafiles = parser.readValueAs(HpcDataObjectListDTO.class);
        return datafiles;
      } else {
        throw new HpcWebException(
            "File does not exist or you do not have READ access.");
      }

    } catch (Exception e) {
      logger.error("Failed to get data file for path " + path + ": ", e);
      throw new HpcWebException("Failed to get data file for path " + path + ": " + e.getMessage());
    }
  }

  public static HpcUserListDTO getUsers(String token, String hpcUserURL, String userId,
      String firstName, String lastName, String doc, String hpcCertPath, String hpcCertPassword) {
    try {
      final UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(
        hpcUserURL);
      if (null != userId && !userId.trim().isEmpty()) {
        ucBuilder.queryParam("nciUserId", userId.trim());
      }
      if (null != firstName && !firstName.trim().isEmpty()) {
        ucBuilder.queryParam("firstNamePattern", firstName.trim());
      }
      if (null != lastName && !lastName.trim().isEmpty()) {
        ucBuilder.queryParam("lastNamePattern", lastName.trim());
      }
      if (null != doc && !doc.trim().isEmpty()) {
        ucBuilder.queryParam("doc", doc.trim());
      }
      final String url2Apply = ucBuilder.build().encode().toUri().toURL()
        .toExternalForm();
      WebClient client =
          HpcClientUtil.getWebClient(url2Apply, hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("GET", null);
      if (restResponse.getStatus() == 200) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcUserListDTO users = parser.readValueAs(HpcUserListDTO.class);
        return users;
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get Users due to: " + e.getMessage());
    }
    return null;
  }

  public static HpcUserDTO getUser(String token, String hpcUserURL, String hpcCertPath,
      String hpcCertPassword) {
    try {

      WebClient client = HpcClientUtil.getWebClient(hpcUserURL, hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("GET", null);
      if (restResponse.getStatus() == 200) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        return parser.readValueAs(HpcUserDTO.class);
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
        throw new HpcWebException("Failed to get user: " + exception.getMessage());
      }
    } catch (HpcWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get User due to: " + e.getMessage());
    }
  }

  public static HpcUserDTO getUserByAdmin(String token, String hpcUserURL, String userId,
      String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcUserURL).pathSegment(userId).build().encode().toUri()
        .toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("GET", null);
      if (restResponse.getStatus() == 200) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        return parser.readValueAs(HpcUserDTO.class);
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
        throw new HpcWebException("Failed to get user: " + exception.getMessage());
      }
    } catch (HpcWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get User due to: " + e.getMessage());
    }
  }

  public static boolean createUser(String token, String hpcUserURL, HpcUserRequestDTO userDTO,
      String userId, String hpcCertPath, String hpcCertPassword) throws JsonParseException, IOException {
    try {
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcUserURL).pathSegment(userId).build().encode().toUri()
        .toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("PUT", userDTO);
      if (restResponse.getStatus() == 201) {
        return true;
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
        throw new HpcWebException(exception.getMessage());
      }
    } catch (Exception e) {
      logger.error("Exception creating user " + userId + ";", e.getStackTrace());
      throw e;
    }
  }

  public static boolean createBookmark(String token, String hpcBookmarkURL,
      HpcBookmarkRequestDTO hpcBookmark, String hpcBookmarkName, String hpcCertPath,
      String hpcCertPassword) {
    try {
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcBookmarkURL).pathSegment(hpcBookmarkName).build()
        .encode().toUri().toURL().toExternalForm(), hpcCertPath,
        hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("PUT", hpcBookmark);
      if (restResponse.getStatus() == 201) {
        return true;
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
        logger.error("Failed to create bookmark " + hpcBookmarkName, exception);
        throw new HpcWebException(exception.getMessage());
      }
    } catch (Exception e) {
      logger.error("Failed to create bookmark " + hpcBookmarkName, e);
      throw new HpcWebException(e.getMessage());
    }
  }

  public static boolean deleteBookmark(String token, String hpcBookmarkURL, String hpcBookmarkName,
      String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcBookmarkURL).pathSegment(hpcBookmarkName).build()
        .encode().toUri().toURL().toExternalForm(), hpcCertPath,
        hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.delete();
      if (restResponse.getStatus() == 200) {
        return true;
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
        logger.error("Failed to delete bookmark " + hpcBookmarkName, exception);
        throw new HpcWebException(exception.getMessage());
      }
    } catch (Exception e) {
    	logger.error("Failed to delete bookmark " + hpcBookmarkName, e);
      throw new HpcWebException(e.getMessage());
    }
  }

  public static boolean deleteSearch(String token, String hpcSavedSearchURL, String searchName,
      String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcSavedSearchURL).pathSegment(searchName).build().encode()
        .toUri().toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.delete();
      if (restResponse.getStatus() == 200) {
        return true;
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
        throw new HpcWebException("Failed to delete saved search: " + exception.getMessage());
      }
    } catch (HpcWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to delete saved search due to: " + e.getMessage());
    }
  }

  public static HpcBookmarkListDTO getBookmarks(String token, String hpcBookmarkURL,
      String hpcCertPath, String hpcCertPassword) {
    WebClient client = HpcClientUtil.getWebClient(hpcBookmarkURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();

    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    MappingJsonFactory factory = new MappingJsonFactory();
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get Bookmarks due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcBookmarkListDTO.class);
    } catch (Exception e) {
      logger.error("Failed to get bookmarks: ", e);
      throw new HpcWebException(e.getMessage());
    }
  }

  public static HpcGroupMembersResponseDTO createGroup(String token, String hpcUserURL,
      HpcGroupMembersRequestDTO groupDTO, String groupName, String hpcCertPath,
      String hpcCertPassword) {
    HpcGroupMembersResponseDTO response = null;
    try {
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcUserURL).pathSegment(groupName).build().encode().toUri()
        .toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("PUT", groupDTO);
      if (restResponse.getStatus() == 201) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        response = parser.readValueAs(HpcGroupMembersResponseDTO.class);

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
        throw new HpcWebException("Failed to create group: " + exception.getMessage());
      }
    } catch (HpcWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to create group due to: " + e.getMessage());
    }
    return response;
  }

  public static HpcGroupMembersResponseDTO updateGroup(String token, String hpcUserURL,
      HpcGroupMembersRequestDTO groupDTO, String groupName, String hpcCertPath,
      String hpcCertPassword) {
    HpcGroupMembersResponseDTO response = null;
    try {
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcUserURL).pathSegment(groupName).build().encode().toUri()
        .toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.invoke("POST", groupDTO);
      if (restResponse.getStatus() == 200) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        response = parser.readValueAs(HpcGroupMembersResponseDTO.class);

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
        throw new HpcWebException("Failed to update group: " + exception.getMessage());
      }
    } catch (HpcWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to update group due to: " + e.getMessage());
    }
    return response;
  }

  public static boolean deleteGroup(String token, String hpcUserURL, String groupName,
      String hpcCertPath, String hpcCertPassword) {
    HpcGroupMembersResponseDTO response = null;
    try {
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcUserURL).pathSegment(groupName).build().encode().toUri()
        .toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.invoke("DELETE", null);
      if (restResponse.getStatus() == 200) {
        return true;
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
        throw new HpcWebException("Failed to delete group: " + exception.getMessage());
      }
    } catch (HpcWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to delete group due to: " + e.getMessage());
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

      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcCollectionURL).path("/{dme-archive-path}")
        .buildAndExpand(path).encode().toUri().toURL().toExternalForm(),
        hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("PUT", collectionDTO);
      if (restResponse.getStatus() == 201) {
        return true;
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
        throw new HpcWebException(exception.getMessage());
      }
    } catch (HpcWebException e) {
      logger.error("Error creating collection: ", e.getStackTrace());
      throw e;
    } catch (Exception e) {
    	logger.error("Exception creating collection: ", e.getStackTrace());
      throw new HpcWebException(e.getMessage());
    }
  }

  public static boolean updateCollection(String token, String hpcCollectionURL,
      HpcCollectionRegistrationDTO collectionDTO, String path, String hpcCertPath,
      String hpcCertPassword) {
    try {
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcCollectionURL).path("/{dme-archive-path}")
        .buildAndExpand(path).encode().toUri().toURL().toExternalForm(),
        hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("PUT", collectionDTO);
      if (restResponse.getStatus() == 200 || restResponse.getStatus() == 201) {
        return true;
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
        throw new HpcWebException(exception.getMessage());
      }
    } catch (HpcWebException e) {
      logger.error("Error creating collection: ", e.getStackTrace());
      throw e;
    } catch (Exception e) {
      logger.error("Exception creating collection: ", e.getStackTrace());
      throw new HpcWebException(e.getMessage());
    }
  }

  public static boolean deleteCollection(String token, String hpcCollectionURL,
      String collectionPath, String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcCollectionURL).path("/{dme-archive-path}")
        .buildAndExpand(collectionPath).encode().toUri().toURL()
        .toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.delete();
      if (restResponse.getStatus() == 200) {
        return true;
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
        throw new HpcWebException("Failed to delete collection: " + exception.getMessage());
      }
    } catch (HpcWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to delete collection due to: " + e.getMessage());
    }
  }


  public static boolean refreshDOCModels(String token, String hpcRefreshModelURL,
	      String hpcCertPath, String hpcCertPassword) {

	  WebClient client = HpcClientUtil.getWebClient(hpcRefreshModelURL, hpcCertPath, hpcCertPassword);
	    client.header("Authorization", "Bearer " + token);

	  Response restResponse = client.invoke("POST", "{}");
	  if (restResponse.getStatus() == 200) {
		  return true;
	  } else {
		  try {
			  ObjectMapper mapper = new ObjectMapper();
			  AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
				new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
				new JacksonAnnotationIntrospector());
			  mapper.setAnnotationIntrospector(intr);
			  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			  MappingJsonFactory factory = new MappingJsonFactory(mapper);
			  JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

			  HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
			  logger.error("Failed to refresh models: " + exception.getMessage(), exception);
			  throw new HpcWebException("Failed to refresh models: " + exception.getMessage());
		  } catch (IOException e) {
			  logger.error("Failed to refresh models: " + e.getMessage(), e);
			  throw new HpcWebException("Failed to refresh models: " + e.getMessage());
		  }
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

      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcDatafileURL).path("/{dme-archive-path}").buildAndExpand(
        path).encode().toUri().toURL().toExternalForm(), hpcCertPath,
        hpcCertPassword);
      client.type(MediaType.MULTIPART_FORM_DATA_VALUE).accept(MediaType.APPLICATION_JSON_VALUE);
      List<Attachment> atts = new LinkedList<Attachment>();
      atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObjectRegistration",
          "application/json", datafileDTO));
      // InputStream inputStream = new BufferedInputStream(
      // new FileInputStream(datafileDTO.getSource().getFileId()));
      ContentDisposition cd2 =
          new ContentDisposition("attachment;filename=" + hpcDatafile.getName());
      atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObject",
          hpcDatafile.getInputStream(), cd2));

      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.put(new MultipartBody(atts));
      if (restResponse.getStatus() == 201) {
        return true;
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
        throw new HpcWebException(exception.getMessage());
      }
    } catch (HpcWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException(e.getMessage());
    }
  }

  public static HpcBulkDataObjectRegistrationResponseDTO registerBulkDatafiles(String token,
      String hpcDatafileURL, HpcBulkDataObjectRegistrationRequestDTO datafileDTO,
      String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = HpcClientUtil.getWebClient(hpcDatafileURL, hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("PUT", datafileDTO);
      if (restResponse.getStatus() == 201 || restResponse.getStatus() == 200) {
        return (HpcBulkDataObjectRegistrationResponseDTO) HpcClientUtil.getObject(restResponse,
            HpcBulkDataObjectRegistrationResponseDTO.class);
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
        throw new HpcWebException("Failed to bulk register data files: " + exception.getMessage());
      }
    } catch (HpcWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to bulk register data files due to: " + e.getMessage());
    }
  }
  
  public static gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationResponseDTO registerBulkDatafiles(String token,
	      String hpcDatafileURL, gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO datafileDTO,
	      String hpcCertPath, String hpcCertPassword) {
	    try {
	      WebClient client = HpcClientUtil.getWebClient(hpcDatafileURL, hpcCertPath, hpcCertPassword);
	      client.header("Authorization", "Bearer " + token);

	      Response restResponse = client.invoke("PUT", datafileDTO);
	      if (restResponse.getStatus() == 201 || restResponse.getStatus() == 200) {
	        return (gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationResponseDTO) HpcClientUtil.getObject(restResponse,
	        		gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationResponseDTO.class);
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
	        throw new HpcWebException("Failed to bulk register data files: " + exception.getMessage());
	      }
	    } catch (HpcWebException e) {
	      throw e;
	    } catch (Exception e) {
	      e.printStackTrace();
	      throw new HpcWebException("Failed to bulk register data files due to: " + e.getMessage());
	    }
	  }

  public static boolean updateDatafile(String token, String hpcDatafileURL,
      HpcDataObjectRegistrationRequestDTO datafileDTO, String path, String hpcCertPath,
      String hpcCertPassword) {
    try {
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcDatafileURL).path("/{dme-archive-path}").buildAndExpand(
        path).encode().toUri().toURL().toExternalForm(), hpcCertPath,
        hpcCertPassword);
      client.type(MediaType.MULTIPART_FORM_DATA_VALUE).accept(MediaType.APPLICATION_JSON_VALUE);
      List<Attachment> atts = new LinkedList<Attachment>();
      atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObjectRegistration",
          "application/json", datafileDTO));

      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.put(new MultipartBody(atts));
      if (restResponse.getStatus() == 200) {
        return true;
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
        throw new HpcWebException("Failed to update data file: " + exception.getMessage());
      }
    } catch (HpcWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to update data file due to: " + e.getMessage());
    }
  }

  public static boolean deleteDatafile(String token, String hpcDatafileURL, String path,
      String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcDatafileURL).path("/{dme-archive-path}").buildAndExpand(
        path).encode().toUri().toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.delete();
      if (restResponse.getStatus() == 200) {
        return true;
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
        throw new HpcWebException(exception.getMessage());
      }
    } catch (HpcWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException(e.getMessage());
    }
  }

  public static AjaxResponseBody linkDatafile(String token, String hpcDatafileURL, String hpcV2DatafileURL, 
      gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationRequestDTO datafileDTO, String path,
      String hpcCertPath, String hpcCertPassword) {
    AjaxResponseBody result = new AjaxResponseBody();
    try {
      try {
        HpcDataObjectListDTO datafile =
            getDatafiles(token, hpcDatafileURL, path, false, hpcCertPath, hpcCertPassword);
        if (datafile != null && !CollectionUtils.isEmpty(datafile.getDataObjects())) {
          result.setMessage("Failed to link. Data file already exists: " + path);
          return result;
        }
      } catch (HpcWebException e) {
        // data file does not exist
      }

      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcV2DatafileURL).path("/{dme-archive-path}").buildAndExpand(
        path).encode().toUri().toURL().toExternalForm(), hpcCertPath,
        hpcCertPassword);
      client.type(MediaType.MULTIPART_FORM_DATA_VALUE).accept(MediaType.APPLICATION_JSON_VALUE);
      
      List<Attachment> atts = new LinkedList<Attachment>();
      atts.add(new org.apache.cxf.jaxrs.ext.multipart.Attachment("dataObjectRegistration",
          "application/json", datafileDTO));

      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.put(new MultipartBody(atts));
      if (restResponse.getStatus() == 201) {
        result.setMessage(
            "Link creation was successful.");
        return result;
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
        throw new HpcWebException(exception.getMessage());
      }
    } catch (HpcWebException e) {
      result.setMessage(e.getMessage());
      return result;
    } catch (Exception e) {
      e.printStackTrace();
      result.setMessage("Link creation failed.");
      return result;
    }
  }
  
  public static gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationResponseDTO linkDatafiles(String token,
      String hpcDatafileURL, gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO datafileDTO,
      String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = HpcClientUtil.getWebClient(hpcDatafileURL, hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("PUT", datafileDTO);
      if (restResponse.getStatus() == 201 || restResponse.getStatus() == 200) {
        return (gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationResponseDTO) HpcClientUtil.getObject(restResponse,
            gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationResponseDTO.class);
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
        throw new HpcWebException("Failed to link data files: " + exception.getMessage());
      }
    } catch (HpcWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to link data files due to: " + e.getMessage());
    }
  }
  
  public static boolean updateUser(String token, String hpcUserURL, HpcUserRequestDTO userDTO,
      String userId, String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcUserURL).pathSegment(userId).build().encode().toUri()
        .toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("POST", userDTO);
      if (restResponse.getStatus() == 200) {
        return true;
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
        throw new HpcWebException("Failed to update user: " + exception.getMessage());
      }
    } catch (HpcWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to update user due to: " + e.getMessage());
    }
  }

  public static HpcGroupListDTO getGroups(String token, String hpcGroupURL, String groupName,
      String hpcCertPath, String hpcCertPassword) {
    try {
      final UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(
        hpcGroupURL);
      if (null != groupName && !groupName.trim().isEmpty()) {
        ucBuilder.queryParam("groupPattern", groupName.trim());
      }
      WebClient client = HpcClientUtil.getWebClient(ucBuilder.build().encode()
        .toUri().toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);

      Response restResponse = client.invoke("GET", null);
      if (restResponse.getStatus() == 200) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcGroupListDTO groups = parser.readValueAs(HpcGroupListDTO.class);
        return groups;
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get Users due to: " + e.getMessage());
    }
    return null;
  }

  public static HpcGroupListDTO getUserGroup(String token, String hpcUserGroupURL,
	      String hpcCertPath, String hpcCertPassword) {
	    try {
	      WebClient client = HpcClientUtil.getWebClient(hpcUserGroupURL, hpcCertPath, hpcCertPassword);
	      client.header("Authorization", "Bearer " + token);

	      Response restResponse = client.invoke("GET", null);
	      if (restResponse.getStatus() == 200) {
	        ObjectMapper mapper = new ObjectMapper();
	        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
	            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
	            new JacksonAnnotationIntrospector());
	        mapper.setAnnotationIntrospector(intr);
	        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	        MappingJsonFactory factory = new MappingJsonFactory(mapper);
	        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

	        return parser.readValueAs(HpcGroupListDTO.class);	       
	      }

	    } catch (Exception e) {
	      logger.error("failed to get user's groups: ", e);
	      throw new HpcWebException("Failed to get user's groups: " + e.getMessage());
	    }
	    return null;
  }
  
  
  public static HpcNamedCompoundMetadataQueryDTO getQuery(String token, String hpcQueryURL,
      String queryName, String hpcCertPath, String hpcCertPassword) {
    HpcNamedCompoundMetadataQueryDTO retVal = null;
    try {
      final String serviceURL = UriComponentsBuilder.fromHttpUrl(hpcQueryURL)
        .pathSegment(queryName).build().encode().toUri().toURL()
        .toExternalForm();
      WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcCertPath,
          hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.get();
      if (null != restResponse && 200 == restResponse.getStatus()) {
        JsonParser parser = new MappingJsonFactory().createParser((InputStream)
            restResponse.getEntity());
        retVal = parser.readValueAs(HpcNamedCompoundMetadataQueryDTO.class);
      }
      return retVal;
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get Query for: " + queryName +
        " due to: " + e.getMessage());
    }
  }

  public static HpcNamedCompoundMetadataQueryListDTO getSavedSearches(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {
    WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();
    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    MappingJsonFactory factory = new MappingJsonFactory();
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());

    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get saved queries due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcNamedCompoundMetadataQueryListDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get saved queries due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get saved queries due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get saved queries due to: " + e.getMessage());
    }
  }

  public static HpcNotificationDeliveryReceiptListDTO getNotificationReceipts(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {

    WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();
    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    MappingJsonFactory factory = new MappingJsonFactory();
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcNotificationDeliveryReceiptListDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
    }
  }

  public static HpcEntityPermissionsDTO getPermissions(String token, String hpcServiceURL,
      String hpcCertPath, String hpcCertPassword) {

    WebClient client = HpcClientUtil.getWebClient(hpcServiceURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();
    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    MappingJsonFactory factory = new MappingJsonFactory();
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcEntityPermissionsDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get notification receipts due to: " + e.getMessage());
    }
  }

  public static HpcUserPermissionDTO getPermissionForUser(String token, String path, String userId,
      String hpcServiceURL, String hpcCertPath, String hpcCertPassword) {
    try {
      final Map<String, String> templateVarValues = new HashMap<>();
      templateVarValues.put("dme-archive-path", path);
      templateVarValues.put("user-id", userId);
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcServiceURL)
        .path("/{dme-archive-path}/acl/user/{user-id}")
        .buildAndExpand(templateVarValues).encode().toUri().toURL()
        .toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.get();
      HpcUserPermissionDTO retVal = null;
      if (null != restResponse && 200 == restResponse.getStatus()) {
        retVal = new MappingJsonFactory().createParser((InputStream)
          restResponse.getEntity()).readValueAs(HpcUserPermissionDTO.class);
      }
      return retVal;
    } catch (IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get permission due to: " + e.getMessage());
    }
  }

  public static HpcUserPermsForCollectionsDTO getPermissionForCollections(
      String token, String hpcServiceURL, String userId, Object[] basePaths,
      String hpcCertPath, String hpcCertPassword) {
    try {
      UriComponentsBuilder ucBuilder = UriComponentsBuilder.fromHttpUrl(
        hpcServiceURL).pathSegment(userId);
      if (null != basePaths && 0 < basePaths.length) {
        ucBuilder.queryParam("collectionPath", basePaths);
      }
      WebClient client = HpcClientUtil.getWebClient(ucBuilder.build().encode()
        .toUri().toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.get();
      HpcUserPermsForCollectionsDTO retVal = null;
      if (null != restResponse && 200 == restResponse.getStatus()) {
        retVal = new MappingJsonFactory().createParser((InputStream)
          restResponse.getEntity()).readValueAs(
          HpcUserPermsForCollectionsDTO.class);
      }
      return retVal;
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get permission due to: " +
        e.getMessage());
    }
  }


  public static HpcNotificationSubscriptionListDTO getUserNotifications(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {

    WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();

    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    MappingJsonFactory factory = new MappingJsonFactory();
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new HpcWebException(
          "Failed to get notification subscriptions due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcNotificationSubscriptionListDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new HpcWebException(
          "Failed to get notification subscriptions due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new HpcWebException(
          "Failed to get notification subscriptions due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new HpcWebException(
          "Failed to get notification subscriptions due to: " + e.getMessage());
    }
  }

  public static HpcDownloadSummaryDTO getDownloadSummary(String token, String hpcQueryURL,
      String hpcCertPath, String hpcCertPassword) {

    WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();

    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    try {
      ObjectMapper mapper = new ObjectMapper();
      AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
          new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
          new JacksonAnnotationIntrospector());
      mapper.setAnnotationIntrospector(intr);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      MappingJsonFactory factory = new MappingJsonFactory(mapper);
      JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

      return parser.readValueAs(HpcDownloadSummaryDTO.class);
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get download tasks list due to: " + e.getMessage());
    }
  }

  public static HpcRegistrationSummaryDTO getRegistrationSummary(String token,
    String hpcQueryURL, MultiValueMap<String, String> queryParamsMap, String
    hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
          .fromHttpUrl(hpcQueryURL).queryParams(queryParamsMap).build().encode()
          .toUri().toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.get();
      if (restResponse == null || restResponse.getStatus() != 200) {
        return null;
      }
      ObjectMapper mapper = new ObjectMapper();
      AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
        new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
        new JacksonAnnotationIntrospector());
      mapper.setAnnotationIntrospector(intr);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
      false);
      mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
      //mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
      MappingJsonFactory factory = new MappingJsonFactory(mapper);
      JsonParser parser = factory.createParser((InputStream) restResponse
        .getEntity());
      return parser.readValueAs(HpcRegistrationSummaryDTO.class);
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get registration tasks list due to: " + e.getMessage());
    }
  }

  public static HpcBulkDataObjectDownloadResponseDTO downloadFiles(String token, String hpcQueryURL,
      HpcBulkDataObjectDownloadRequestDTO dto, String hpcCertPath, String hpcCertPassword) {
    HpcBulkDataObjectDownloadResponseDTO response = null;
    try {
      WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.invoke("POST", dto);
      if (restResponse.getStatus() == 200) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
        response = parser.readValueAs(HpcBulkDataObjectDownloadResponseDTO.class);
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
        throw new HpcWebException("Failed to submit download request: " + exception.getMessage());
      }
    } catch (HpcWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to submit download request: " + e.getMessage());
    }
    return response;
  }
  
  public static AjaxResponseBody downloadDataFile(String token, String serviceURL,
      HpcDownloadRequestDTO dto, String downloadType, String hpcCertPath, String hpcCertPassword)
      throws JsonParseException, IOException {
    AjaxResponseBody result = new AjaxResponseBody();
    WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.invoke("POST", dto);
    if (restResponse.getStatus() == 200) {
      HpcDataObjectDownloadResponseDTO downloadDTO =
          (HpcDataObjectDownloadResponseDTO) HpcClientUtil.getObject(restResponse,
              HpcDataObjectDownloadResponseDTO.class);
      String taskId = "Unknown";
      if (downloadDTO != null)
        taskId = downloadDTO.getTaskId();
      result.setMessage(
              "Asynchronous download request is submitted successfully! Task Id: <a href='downloadtask?type=" + downloadType + "&taskId=" + taskId +"'>"+taskId+"</a>");
      return result;
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
        result.setMessage("Download request is not successful: " + exception.getMessage());
        return result;
      } catch (Exception e) {
        result.setMessage("Download request is not successful: " + e.getMessage());
        return result;
      }
    }
  }

  public static HpcBulkDataObjectRegistrationStatusDTO
    getDataObjectRegistrationTask(String token, String hpcQueryURL, String
    taskId, String hpcCertPath, String hpcCertPassword) {
    try {
      WebClient client = HpcClientUtil.getWebClient(UriComponentsBuilder
        .fromHttpUrl(hpcQueryURL).pathSegment(taskId).build().encode().toUri()
        .toURL().toExternalForm(), hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      Response restResponse = client.get();

      if (restResponse == null || restResponse.getStatus() != 200) {
        return null;
      }
      
      ObjectMapper mapper = new ObjectMapper();
      AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
        new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
        new JacksonAnnotationIntrospector());
      mapper.setAnnotationIntrospector(intr);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
      false);
      mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
      //mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
      MappingJsonFactory factory = new MappingJsonFactory(mapper);
      JsonParser parser = factory.createParser((InputStream) restResponse
        .getEntity());
      return parser.readValueAs(HpcBulkDataObjectRegistrationStatusDTO.class);
      
      
//      JsonParser parser = new MappingJsonFactory().createParser((InputStream)
//        restResponse.getEntity());
//      return parser.readValueAs(HpcBulkDataObjectRegistrationStatusDTO.class);
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new HpcWebException(
          "Failed to get data object registration tasks details due to: " + e.getMessage());
    }
  }

  public static HpcDataObjectDownloadStatusDTO getDataObjectDownloadTask(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {

    WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();

    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    
    ObjectMapper mapper = new ObjectMapper();
    AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
      new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
      new JacksonAnnotationIntrospector());
    mapper.setAnnotationIntrospector(intr);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
    false);
    mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    MappingJsonFactory factory = new MappingJsonFactory(mapper);
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new HpcWebException(
          "Failed to get data object download tasks details due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcDataObjectDownloadStatusDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new HpcWebException(
          "Failed to get data object download tasks details due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new HpcWebException(
          "Failed to get data object download tasks details due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new HpcWebException(
          "Failed to get data object download tasks details due to: " + e.getMessage());
    }
  }

  public static HpcCollectionDownloadStatusDTO getDataObjectsDownloadTask(String token,
      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {

    WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();

    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    
    ObjectMapper mapper = new ObjectMapper();
    AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
      new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
      new JacksonAnnotationIntrospector());
    mapper.setAnnotationIntrospector(intr);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
    false);
    mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    MappingJsonFactory factory = new MappingJsonFactory(mapper);
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new HpcWebException(
          "Failed to get data objects download tasks details due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcCollectionDownloadStatusDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new HpcWebException(
          "Failed to get data objects download tasks details due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new HpcWebException(
          "Failed to get data objects download tasks details due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new HpcWebException(
          "Failed to get data objects download tasks details due to: " + e.getMessage());
    }
  }

  public static boolean cancelDownloadTask(String token,
	      String hpcQueryURL, String hpcCertPath, String hpcCertPassword) {
    try {

        WebClient client = HpcClientUtil.getWebClient(hpcQueryURL, hpcCertPath, hpcCertPassword);
        client.header("Authorization", "Bearer " + token);

        Response restResponse = client.invoke("POST", "{}");

      if (restResponse.getStatus() == 200) {
        return true;
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
        throw new HpcWebException("Failed to cancel download task: " + exception.getMessage());
      }
    } catch (HpcWebException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to cancel download task due to: " + e.getMessage());
    }
  }

  public static HpcMetadataAttributesListDTO getMetadataAttrNames(String token,
      String hpcMetadataAttrsURL, String hpcCertPath, String hpcCertPassword) {

    String url = hpcMetadataAttrsURL;

    WebClient client = HpcClientUtil.getWebClient(url, hpcCertPath, hpcCertPassword);
    client.header("Authorization", "Bearer " + token);

    Response restResponse = client.get();

    if (restResponse == null || restResponse.getStatus() != 200)
      return null;
    MappingJsonFactory factory = new MappingJsonFactory();
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) restResponse.getEntity());
    } catch (IllegalStateException | IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get Metadata attributes: due to: " + e.getMessage());
    }
    try {
      return parser.readValueAs(HpcMetadataAttributesListDTO.class);
    } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get Metadata attributes: due to: " + e.getMessage());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get Metadata attributes: due to: " + e.getMessage());
    } catch (IOException e) {
      e.printStackTrace();
      throw new HpcWebException("Failed to get Metadata attributes: due to: " + e.getMessage());
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

  public static String encode(String strVal) {
    if (strVal == null)
      return null;
    else if (strVal.indexOf("/") == -1)
      return strVal;
    else {
      StringBuffer encodedStr = new StringBuffer();
      StringTokenizer tokens = new StringTokenizer(strVal, "/");
      while (tokens.hasMoreTokens()) {
        String token = tokens.nextToken();
        encodedStr.append(URLEncoder.encode(token));
        if (tokens.hasMoreTokens())
          encodedStr.append("/");
      }
      return encodedStr.toString();
    }
  }

  public static <T> Object getObject(Response response, Class<T> objectClass) {
    ObjectMapper mapper = new ObjectMapper();
    AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
        new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
        new JacksonAnnotationIntrospector());
    mapper.setAnnotationIntrospector(intr);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    MappingJsonFactory factory = new MappingJsonFactory(mapper);
    JsonParser parser;
    try {
      parser = factory.createParser((InputStream) response.getEntity());
      return parser.readValueAs(objectClass);
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      throw new HpcWebException("Failed to parse object: " + e1.getMessage());
    } catch (Exception e) {
      throw new HpcWebException("Failed to parse object: " + e.getMessage());
    }
  }

  public static void populateBasePaths(HttpSession session, Model model,
      HpcDataManagementModelDTO modelDTO, String authToken, String userId, String collectionURL,
      String sslCertPath, String sslCertPassword) throws HpcWebException {

    Set<String> basePaths = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    final List<String> docRulesBasePaths = new ArrayList<>();
    for (HpcDocDataManagementRulesDTO docRule : modelDTO.getDocRules()) {
      for (HpcDataManagementRulesDTO rule : docRule.getRules()) {
        docRulesBasePaths.add(rule.getBasePath());
      }
    }
    final HpcUserPermsForCollectionsDTO permissions = HpcClientUtil
      .getPermissionForCollections(authToken, collectionURL, userId,
      docRulesBasePaths.toArray(), sslCertPath, sslCertPassword);
    if (permissions != null) {
      for (HpcPermissionForCollection permission : permissions.getPermissionsForCollections()) {
        if (permission != null && permission.getPermission() != null
            && (permission.getPermission().equals(HpcPermission.WRITE)
                || permission.getPermission().equals(HpcPermission.OWN)))
          basePaths.add(permission.getCollectionPath());
      }
    }
    session.setAttribute("basePaths", basePaths);
  }


/*
  private static String[] dividePathStringIntoSegments(String thePathString) {
    String[] segments = new String[0];
    if (StringUtils.hasText(thePathString)) {
      final String[] tokens = thePathString.split(REGEX_SINGLE_FORWARD_SLASH);
      List<String> segs = new ArrayList<>();
      for (String aToken : tokens) {
        if (StringUtils.hasText(aToken)) {
          segs.add(aToken);
        }
      }
      segments = segs.toArray(tokens);
    }
    return segments;
  }
*/


  private static Optional<String> extractElementTypeFromResponse(
      Response restResponse) throws IOException {
    Optional<String> retVal = Optional.empty();
    final JsonParser parser = new MappingJsonFactory().createParser(
      (InputStream) restResponse.getEntity());
    while (null != parser.nextValue()) {
      if (JSON_RESPONSE_ATTRIB__ELEMENT_TYPE.equals(parser.getCurrentName())) {
        retVal = Optional.of(parser.getValueAsString());
        break;
      }
    }

    return retVal;
  }

  private static HpcExceptionDTO genHpcExceptionDtoOnNonOkRestResponse(
    Response restResponse) throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.setAnnotationIntrospector(new AnnotationIntrospectorPair(
      new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
      new JacksonAnnotationIntrospector())
    );
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
      false);
    final JsonParser parser = new MappingJsonFactory(mapper).createParser(
      (InputStream) restResponse.getEntity());
    final HpcExceptionDTO hpcExceptionDto =
      parser.readValueAs(HpcExceptionDTO.class);

    return hpcExceptionDto;
  }

  private static String prependLeadingForwardSlashIfNeeded(String argInputStr) {
    return (null == argInputStr || argInputStr.isEmpty()) ? "/" :
      argInputStr.startsWith("/") ? argInputStr : "/".concat(argInputStr);
  }


  private static Properties appProperties;


  private static void initApplicationProperties() {
    if (null == appProperties) {
      loadApplicationProperties();
    }
  }


  private static void loadApplicationProperties() {
    Properties theProperties = new Properties();
    try {
      theProperties.load(HpcClientUtil.class.getResourceAsStream(
        "/application.properties"));
      if (null == appProperties) {
        appProperties = theProperties;
      } else {
        appProperties.clear();
        appProperties.putAll(theProperties);
      }
    } catch (IOException e) {
      throw new HpcWebException("Unable to load application properties!", e);
    }
  }







}
