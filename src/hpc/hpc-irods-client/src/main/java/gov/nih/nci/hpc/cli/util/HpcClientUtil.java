package gov.nih.nci.hpc.cli.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

public class HpcClientUtil {

	public static RestTemplate getRestTemplate(String userId, String password, String hpcCertPath, String hpcCertPassword) {

		RestTemplate restTemplate = null;
		FileInputStream  fis = null;
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

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagers, trustManagers, null);

			CloseableHttpClient httpClient = HttpClients.custom().setSSLHostnameVerifier(new NoopHostnameVerifier())
					.setSSLContext(sslContext).build();
			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
			requestFactory.setHttpClient(httpClient);
			restTemplate = new RestTemplate(requestFactory);
			List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
			messageConverters.add(new FormHttpMessageConverter());
			messageConverters.add(new StringHttpMessageConverter());
			messageConverters.add(new MappingJackson2HttpMessageConverter());

			restTemplate.setMessageConverters(messageConverters);
			
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
	
	public static String encode(String strVal)
	{
		if(strVal == null)
			return null;
		else if(strVal.indexOf("/") == -1)
			return strVal;
		else
		{
			StringBuffer encodedStr = new StringBuffer();
			StringTokenizer tokens = new StringTokenizer(strVal, "/");
			while(tokens.hasMoreTokens())
			{
				String token = tokens.nextToken();
				encodedStr.append(URLEncoder.encode(token));
				if(tokens.hasMoreTokens())
					encodedStr.append("/");
			}
			return encodedStr.toString();
		}
	}
}
