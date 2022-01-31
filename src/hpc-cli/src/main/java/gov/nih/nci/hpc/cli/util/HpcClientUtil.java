/*******************************************************************************
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc.
 * 
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import java.util.List;
import java.util.StringTokenizer;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import javax.xml.transform.Source;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.integration.http.converter.MultipartAwareFormHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;

public class HpcClientUtil {

  private static final String FORBIDDEN_CHARACTERS_FOR_DME_ARCHIVE_PATHS = "?;\\";

  private static final String FORBIDDEN_CHARS_DISPLAY_STRING;
  static {
    StringBuilder sb = new StringBuilder();
    char[] fcArr = FORBIDDEN_CHARACTERS_FOR_DME_ARCHIVE_PATHS.toCharArray();
    String itemsSeparator = " ";
    for (char c : fcArr) {
      if (sb.length() > 0) {
        sb.append(itemsSeparator);
      }
      sb.append(c);
    }
    FORBIDDEN_CHARS_DISPLAY_STRING = sb.toString();
  }

  // public static WebClient getWebClient(String url, String hpcCertPath, String hpcCertPassword) {
  // return getWebClient(url, null, null, hpcCertPath, hpcCertPassword);
  // }

  public static WebClient getWebClient(String url, String proxyURL, String proxyPort,
      String hpcCertPath, String hpcCertPassword) {
    FileInputStream fis = null;
    WebClient client = null;
    String[] cipherSuites = new String[] {"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256", "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA", "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
        "SSL_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
        "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA", "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
        "SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA", "SSL_DHE_DSS_WITH_DES_CBC_SHA",
        "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
        "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", "TLS_KRB5_WITH_3DES_EDE_CBC_SHA",
        "TLS_KRB5_WITH_3DES_EDE_CBC_MD5", "TLS_KRB5_WITH_DES_CBC_SHA", "TLS_KRB5_WITH_DES_CBC_MD5",
        "TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA", "TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5"};
    try {
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

      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(MapperFeature.USE_STD_BEAN_NAMING, true);
      mapper.setPropertyNamingStrategy(new CustomLowerCamelCase());
      mapper.setSerializationInclusion(Include.NON_NULL);
      
      client = WebClient.create(url, Collections.singletonList(new com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider(mapper)));
      WebClient.getConfig(client).getRequestContext().put("support.type.as.multipart", "true");
      WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(60000000);
      WebClient.getConfig(client).getHttpConduit().getClient().setConnectionTimeout(60000000);
      HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
      if (proxyURL != null && proxyPort != null) {
        // System.out.println("Setting proxy settings.." +proxyURL+":"+proxyPort);
        HTTPClientPolicy policy = conduit.getClient();
        policy.setProxyServer(proxyURL);
        policy.setProxyServerPort(new Integer(proxyPort).intValue());
      }

      TLSClientParameters params = conduit.getTlsClientParameters();
      if (params == null) {
        params = new TLSClientParameters();
        conduit.setTlsClientParameters(params);
      }

      // params.setTrustManagers(new TrustManager[] { new
      // TrustAllX509TrustManager() });
      // params.setCipherSuites(Arrays.asList(cipherSuites));
      // params.setSecureSocketProtocol("TLSv1.2");
      // System.out.println("Using TLSv1.2");
      params.setTrustManagers(trustManagers);
      params.setDisableCNCheck(true);
      conduit.setTlsClientParameters(params);
    } catch (IOException e) {
      System.out.println("Unable to build REST Client: IOException: " + e.getMessage());
      // e.printStackTrace();
    } catch (CertificateException e) {
      System.out.println("Unable to build REST Client: CertificateException: " + e.getMessage());
      // e.printStackTrace();
    } catch (KeyStoreException e) {
      System.out.println("Unable to build REST Client: KeyStoreException: " + e.getMessage());
      // e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      System.out
          .println("Unable to build REST Client: NoSuchAlgorithmException: " + e.getMessage());
    } catch (UnrecoverableKeyException e) {
      System.out
          .println("Unable to build REST Client: UnrecoverableKeyException: " + e.getMessage());
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          System.out.println("Unable to build REST Client: IOException: " + e.getMessage());
        }
      }
    }
    return client;
  }

  public static String getAuthenticationToken(String userId, String passwd, String hpcServerURL,
      String proxyURL, String proxyPort, String hpcCertPath, String hpcCertPassword) {
    JsonParser parser = null;

    try {
      final String apiUrl2Apply = UriComponentsBuilder.fromHttpUrl(hpcServerURL)
        .path("/authenticate").build().encode().toUri().toURL()
        .toExternalForm();
      WebClient client = HpcClientUtil.getWebClient(apiUrl2Apply, proxyURL,
          proxyPort, hpcCertPath, hpcCertPassword);
      String token = DatatypeConverter.printBase64Binary((userId + ":" + passwd).getBytes());
      client.header("Authorization", "Basic " + token);
      // If necessary, here add "Content-Type" header set to "application/json; charset=UTF-8" via client.type("application/json; charset=UTF-8")
      Response restResponse = client.get();
      if (restResponse == null)
        return null;
      MappingJsonFactory factory = new MappingJsonFactory();
      parser = factory.createParser((InputStream) restResponse.getEntity());
      if (restResponse.getStatus() != 200) {
        try {
          HpcExceptionDTO response = parser.readValueAs(HpcExceptionDTO.class);
          System.out.println("Unable to get authenticatio token: " + response.getMessage());
          return null;
        } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
          System.out.println("Unable to get authenticatio token: " + e.getMessage());
          return null;
        }
      } else {
        if (parser != null) {
          HpcAuthenticationResponseDTO dto = parser.readValueAs(HpcAuthenticationResponseDTO.class);
          return dto.getToken();
        } else
          return null;
      }
    } catch (IllegalStateException | IOException e1) {
      // TODO Auto-generated catch block
      System.out.println("Unable to get authenticatio token: " + e1.getMessage());
      return null;
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
      System.out.println("Unable to build REST Template: IOException: " + e.getMessage());
    } catch (CertificateException e) {
      System.out.println("Unable to build REST Template: CertificateException: " + e.getMessage());
    } catch (KeyStoreException e) {
      System.out.println("Unable to build REST Template: KeyStoreException: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      System.out
          .println("Unable to build REST Template: NoSuchAlgorithmException: " + e.getMessage());
    } catch (UnrecoverableKeyException e) {
      System.out
          .println("Unable to build REST Template: UnrecoverableKeyException: " + e.getMessage());
    } catch (KeyManagementException e) {
      System.out
          .println("Unable to build REST Template: KeyManagementException: " + e.getMessage());
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          System.out.println("Unable to build REST Template: IOException: " + e.getMessage());
        }
      }
    }

    return restTemplate;
  }

  public static HpcCollectionListDTO getCollection(String token, String hpcCollectionURL,
      String proxyURL, String proxyPort, String hpcCertPath, String hpcCertPassword) {
    try {
      String serviceURL = UriComponentsBuilder.fromHttpUrl(hpcCollectionURL)
        .queryParam("list", Boolean.FALSE).build().encode().toUri().toURL()
        .toExternalForm();

      WebClient client =
          getWebClient(serviceURL, proxyURL, proxyPort, hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      // If necessary, here add "Content-Type" header set to "application/json; charset=UTF-8" via client.type("application/json; charset=UTF-8")
      Response restResponse = client.invoke("GET", null);
      // System.out.println("restResponse.getStatus():"
      // +restResponse.getStatus());
      if (restResponse.getStatus() == 200) {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
            new JacksonAnnotationIntrospector());
        mapper.setAnnotationIntrospector(intr);
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJsonFactory factory = new MappingJsonFactory(mapper);
        JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

        HpcCollectionListDTO collections = parser.readValueAs(HpcCollectionListDTO.class);
        return collections;
      } else {
        throw new HpcBatchException("Collection not found!");
      }

    } catch (Exception e) {
      throw new HpcBatchException("Failed to get Collection due to: " + e.getMessage());
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

  public static HpcDataObjectListDTO getDatafiles(String token, String hpcDatafileURL,
      String proxyURL, String proxyPort, String path, boolean list, String hpcCertPath,
      String hpcCertPassword) {
    try {
      final String apiUrl2Apply = UriComponentsBuilder.fromHttpUrl(
        hpcDatafileURL).path("/{dme-archive-path}").queryParam("list", Boolean
        .valueOf(list)).buildAndExpand(path).encode().toUri().toURL()
        .toExternalForm();
      WebClient client = getWebClient(apiUrl2Apply, proxyURL, proxyPort,
        hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      // If necessary, here add "Content-Type" header set to "application/json; charset=UTF-8" via client.type("application/json; charset=UTF-8")
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

        HpcDataObjectListDTO datafiles = parser.readValueAs(HpcDataObjectListDTO.class);
        return datafiles;
      } else {
        System.out.println("Failed to get data file: " + path);
        throw new HpcBatchException("Failed to get data file: " + path);
      }

    } catch (Exception e) {
      System.out.println("Failed to get data file: " + path);
      throw new HpcBatchException("Failed to get data file: " + path);
    }
  }

  public static void writeException(Exception e, String message, String exceptionAsString,
      String logFile) {
    HpcLogWriter.getInstance().WriteLog(logFile, message);
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    if (exceptionAsString == null)
      exceptionAsString = sw.toString();
    HpcLogWriter.getInstance().WriteLog(logFile, exceptionAsString);
  }

  public static void writeRecord(String fileBasePath, String absolutePath, String logFile) {
    File fullFile = new File(fileBasePath);
    String fullFilePathName = null;
    try {
    fullFilePathName = fullFile.getCanonicalPath();
  } catch (IOException e) {
    System.out.println("Failed to read file path: "+fileBasePath);
  }
    fullFilePathName = fullFilePathName.replace('\\', '/');
    fileBasePath = fileBasePath.replace('\\', '/');
    absolutePath = absolutePath.replace('\\', '/');
    String objectPath = absolutePath;
    if (absolutePath.indexOf(fullFilePathName) != -1)
      objectPath =
          absolutePath.substring(absolutePath.indexOf(fullFilePathName) + fullFilePathName.length() + 1);
    HpcLogWriter.getInstance().WriteLog(logFile, objectPath);
  }

  public static <T> Object getObject(Response response, Class<T> objectClass)
      throws HpcBatchException {
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
      throw new HpcBatchException("Failed to parse object: " + e1.getMessage());
    } catch (Exception e) {
      throw new HpcBatchException("Failed to parse object: " + e.getMessage());
    }
  }

  public static boolean containsWhiteSpace(final String testCode) {
    if (testCode != null) {
      for (int i = 0; i < testCode.length(); i++) {
        if (Character.isWhitespace(testCode.charAt(i))) {
          return true;
        }
      }
    }
    return false;
  }


  public static HpcBulkDataObjectRegistrationResponseDTO registerBulkDatafiles(String token,
      String hpcDatafileURL, HpcBulkDataObjectRegistrationRequestDTO datafileDTO,
      String hpcCertPath, String hpcCertPassword, String proxyURL, String proxyPort) {
    try {

      WebClient client =
          getWebClient(hpcDatafileURL, proxyURL, proxyPort, hpcCertPath, hpcCertPassword);
      client.header("Authorization", "Bearer " + token);
      client.type("application/json; charset=UTF-8");
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
        throw new HpcCmdException("Failed to bulk register data files: " + exception.getMessage());
      }
    } catch (HpcCmdException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new HpcCmdException("Failed to bulk register data files due to: " + e.getMessage());
    }
  }


  public static String constructPathString(String ... pathSegments) {
    final StringBuilder sb = new StringBuilder();
    String effSegment;
    for (String someSegment : pathSegments) {
      effSegment = someSegment.trim();
      if (effSegment.startsWith("/")) {
        effSegment = effSegment.substring(1);
      }
      sb.append("/").append(effSegment);
    }
    return sb.toString();
  }

  public static String prependForwardSlashIfAbsent(String arg) {
    String result;
    if (null == arg || arg.isEmpty()) {
      result = "/";
    } else {
      result = arg.trim();
      if (!result.startsWith("/")) {
        result = "/".concat(result);
      }
    }

    return result;
  }


  public static void validateDmeArchivePath(String path) {
    char[] fcArr = FORBIDDEN_CHARACTERS_FOR_DME_ARCHIVE_PATHS.toCharArray();
    for (char fc : fcArr) {
      if (path.contains(String.valueOf(fc))) {
        throw new HpcCmdException("Specified DME archive path '" + path +
          "' contains invalid character(s), which are: {" +
          FORBIDDEN_CHARS_DISPLAY_STRING + "}.");
      }
    }
  }

}
