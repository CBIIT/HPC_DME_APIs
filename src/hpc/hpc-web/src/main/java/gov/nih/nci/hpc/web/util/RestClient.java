package gov.nih.nci.hpc.web.util;

import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.client.HttpClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

public class RestClient {

	public static  HttpComponentsClientHttpRequestFactory getSSLRequestFactory() {
		HttpComponentsClientHttpRequestFactory requestFactory = null;
		try {
			requestFactory = new HttpComponentsClientHttpRequestFactory();
			HttpClient httpClient =  requestFactory
					.getHttpClient();
			TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
				@Override
				public boolean isTrusted(X509Certificate[] certificate,
						String authType) {
					return true;
				}
			};
			SSLSocketFactory sf = new SSLSocketFactory(acceptingTrustStrategy,
					ALLOW_ALL_HOSTNAME_VERIFIER);
			httpClient.getConnectionManager().getSchemeRegistry()
					.register(new Scheme("https", 7737, sf));

		} catch (java.security.NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		catch (java.security.KeyManagementException e) {
			e.printStackTrace();
		}
		catch (java.security.KeyStoreException e) {
			e.printStackTrace();
		}
		catch (java.security.UnrecoverableKeyException e) {
			e.printStackTrace();
		}
		return requestFactory;
	}
}
