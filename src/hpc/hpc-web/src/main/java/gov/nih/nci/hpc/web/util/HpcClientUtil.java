package gov.nih.nci.hpc.web.util;

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
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.integration.http.converter.MultipartAwareFormHttpMessageConverter;
import org.springframework.validation.ObjectError;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMetadataAttributesListDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datasearch.HpcNamedCompoundMetadataQueryListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionListDTO;
import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.web.HpcResponseErrorHandler;
import gov.nih.nci.hpc.web.HpcWebException;

public class HpcClientUtil {

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

		params.setTrustManagers(new TrustManager[] { new TrustAllX509TrustManager() });
		conduit.setTlsClientParameters(params);

		params.setDisableCNCheck(true);
		return client;
	}

	public static String getAuthenticationToken(String userId, String passwd, String hpcServerURL)
			throws HpcWebException {

		WebClient client = HpcClientUtil.getWebClient(hpcServerURL, null, null);
		String token = DatatypeConverter.printBase64Binary((userId + ":" + passwd).getBytes());
		client.header("Authorization", "Basic " + token);
		Response restResponse = client.get();

		if (restResponse == null || restResponse.getStatus() != 200)
			return null;
		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createParser((InputStream) restResponse.getEntity());
		} catch (IllegalStateException | IOException e1) {
			e1.printStackTrace();
			throw new HpcWebException("Failed to get auth token: " + e1.getMessage());
		}
		try {
			HpcAuthenticationResponseDTO dto = parser.readValueAs(HpcAuthenticationResponseDTO.class);
			return dto.getToken();
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get auth token: " + e.getMessage());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get auth token: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get auth token: " + e.getMessage());
		}
	}

	public static HpcDataManagementModelDTO getDOCModel(String token, String hpcModelURL, String doc,
			String hpcCertPath, String hpcCertPassword) {

		WebClient client = HpcClientUtil.getWebClient(hpcModelURL + "/" + doc, hpcCertPath, hpcCertPassword);
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
			throw new HpcWebException("Failed to get DOC Model for: " + doc + " due to: " + e.getMessage());
		}
		try {
			return parser.readValueAs(HpcDataManagementModelDTO.class);
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get DOC Model for: " + doc + " due to: " + e.getMessage());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get DOC Model for: " + doc + " due to: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get DOC Model for: " + doc + " due to: " + e.getMessage());
		}
	}

	public static HpcCollectionListDTO getCollection(String token, String hpcCollectionlURL, String path, boolean list,
			String hpcCertPath, String hpcCertPassword) {
		try {
			WebClient client = HpcClientUtil.getWebClient(
					hpcCollectionlURL + "/" + path + (list ? "?list=true" : "?list=false"), hpcCertPath,
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

				HpcCollectionListDTO collections = parser.readValueAs(HpcCollectionListDTO.class);
				return collections;
			} else {
				throw new HpcWebException("Collection not found!");
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Collection for: " + path + " due to: " + e.getMessage());
		}
	}

	public static HpcNamedCompoundMetadataQueryDTO getQuery(String token, String hpcQueryURL, String queryName,
			String hpcCertPath, String hpcCertPassword) {

		String serviceURL = hpcQueryURL + "/" + queryName;
		WebClient client = HpcClientUtil.getWebClient(serviceURL, hpcCertPath, hpcCertPassword);
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
			throw new HpcWebException("Failed to get DOC Model for: " + queryName + " due to: " + e.getMessage());
		}
		try {
			return parser.readValueAs(HpcNamedCompoundMetadataQueryDTO.class);
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Query for: " + queryName + " due to: " + e.getMessage());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Query for: " + queryName + " due to: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get Query for: " + queryName + " due to: " + e.getMessage());
		}
	}

	public static HpcNamedCompoundMetadataQueryListDTO getSavedSearches(String token, String hpcQueryURL,
			String hpcCertPath, String hpcCertPassword) {
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

	public static HpcNotificationDeliveryReceiptListDTO getNotificationReceipts(String token, String hpcQueryURL,
			String hpcCertPath, String hpcCertPassword) {

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

	public static HpcNotificationSubscriptionListDTO getUserNotifications(String token, String hpcQueryURL,
			String hpcCertPath, String hpcCertPassword) {

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
			throw new HpcWebException("Failed to get notification subscriptions due to: " + e.getMessage());
		}
		try {
			return parser.readValueAs(HpcNotificationSubscriptionListDTO.class);
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get notification subscriptions due to: " + e.getMessage());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get notification subscriptions due to: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new HpcWebException("Failed to get notification subscriptions due to: " + e.getMessage());
		}
	}

	public static HpcMetadataAttributesListDTO getMetadataAttrNames(String token, String hpcMetadataAttrsURL,
			String hpcCertPath, String hpcCertPassword) {

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
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				kmf.init(keyStore, hpcCertPassword.toCharArray());
				KeyManager[] keyManagers = kmf.getKeyManagers();

				TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				tmf.init(keyStore);
				TrustManager[] trustManagers = tmf.getTrustManagers();

				SSLContext sslContext = SSLContext.getInstance("TLS");
				sslContext.init(keyManagers, trustManagers, null);

				CloseableHttpClient httpClient = HttpClients.custom().setSSLHostnameVerifier(new NoopHostnameVerifier())
						.setSSLContext(sslContext).build();

				HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
				requestFactory.setHttpClient(httpClient);
				restTemplate = new RestTemplate(requestFactory);
			} else {
				@SuppressWarnings("deprecation")
				SSLContextBuilder builder = new SSLContextBuilder();
				builder.loadTrustMaterial(null, new TrustStrategy() {
					@Override
					public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
						return true;
					}
				});

				SSLConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(builder.build(),
						SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
				HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslSF).build();
				HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
						httpClient);
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
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}
		} };

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
}
