/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.util;

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
import java.util.Arrays;
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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.integration.http.converter.MultipartAwareFormHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;

public class HpcClientUtil {

	public static String[] cipherSuites = new String[]{"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA256", "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256", "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA", "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256", "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA", "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA", "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA", "TLS_EMPTY_RENEGOTIATION_INFO_SCSV", "SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA", "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA", "TLS_KRB5_WITH_3DES_EDE_CBC_SHA", "TLS_KRB5_WITH_3DES_EDE_CBC_MD5", "TLS_KRB5_WITH_DES_CBC_SHA", "TLS_KRB5_WITH_DES_CBC_MD5", "TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA", "TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5"};
	public static WebClient getWebClient(String url, String hpcCertPath, String hpcCertPassword) {
		FileInputStream fis = null;
		WebClient client = null;
		try {
			fis = new java.io.FileInputStream(hpcCertPath);
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(fis, hpcCertPassword.toCharArray());
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, hpcCertPassword.toCharArray());
			KeyManager[] keyManagers = kmf.getKeyManagers();

			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(keyStore);
			TrustManager[] trustManagers = tmf.getTrustManagers();

			client = WebClient.create(url, Collections.singletonList(new JacksonJsonProvider()));
			WebClient.getConfig(client).getRequestContext().put("support.type.as.multipart", "true");
			WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(60000000);
			WebClient.getConfig(client).getHttpConduit().getClient().setConnectionTimeout(60000000);
			HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();

			TLSClientParameters params = conduit.getTlsClientParameters();
			if (params == null) {
				params = new TLSClientParameters();
				params.setCipherSuites(Arrays.asList(cipherSuites));
				params.setSecureSocketProtocol("TLSv1.2");
				conduit.setTlsClientParameters(params);
			}

			// params.setTrustManagers(new TrustManager[] { new
			// TrustAllX509TrustManager() });
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
			//e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Unable to build REST Client: NoSuchAlgorithmException: " + e.getMessage());
		} catch (UnrecoverableKeyException e) {
			System.out.println("Unable to build REST Client: UnrecoverableKeyException: " + e.getMessage());
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

	public static String getAuthenticationToken(String userId, String passwd, String hpcServerURL, String hpcCertPath,
			String hpcCertPassword) {
		JsonParser parser;

		try {
			WebClient client = HpcClientUtil.getWebClient(hpcServerURL + "/authenticate", hpcCertPath, hpcCertPassword);
			String token = DatatypeConverter.printBase64Binary((userId + ":" + passwd).getBytes());
			client.header("Authorization", "Basic " + token);
			Response restResponse = client.get();
			if (restResponse == null)
				return null;
			MappingJsonFactory factory = new MappingJsonFactory();
			parser = factory.createParser((InputStream) restResponse.getEntity());
		} catch (IllegalStateException | IOException e1) {
			// TODO Auto-generated catch block
			System.out.println("Unable to get authenticatio token: " + e1.getMessage());
			return null;
		}
		try {
			HpcAuthenticationResponseDTO dto = parser.readValueAs(HpcAuthenticationResponseDTO.class);
			return dto.getToken();
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			System.out.println("Unable to get authenticatio token: JsonMappingException: " + e.getMessage());
			// e.printStackTrace();
			return null;
		} catch (JsonProcessingException e) {
			// e.printStackTrace();
			System.out.println("Unable to get authenticatio token: JsonProcessingException: " + e.getMessage());
			return null;
		} catch (IOException e) {
			System.out.println("Unable to get authenticatio token: IOException: " + e.getMessage());
			// e.printStackTrace();
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
			System.out.println("Unable to build REST Template: IOException: " + e.getMessage());
		} catch (CertificateException e) {
			System.out.println("Unable to build REST Template: CertificateException: " + e.getMessage());
		} catch (KeyStoreException e) {
			System.out.println("Unable to build REST Template: KeyStoreException: " + e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Unable to build REST Template: NoSuchAlgorithmException: " + e.getMessage());
		} catch (UnrecoverableKeyException e) {
			System.out.println("Unable to build REST Template: UnrecoverableKeyException: " + e.getMessage());
		} catch (KeyManagementException e) {
			System.out.println("Unable to build REST Template: KeyManagementException: " + e.getMessage());
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
