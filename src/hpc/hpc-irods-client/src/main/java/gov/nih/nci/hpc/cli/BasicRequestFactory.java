package gov.nih.nci.hpc.cli;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

public class BasicRequestFactory extends HttpComponentsClientHttpRequestFactory {

public BasicRequestFactory(HttpClient httpClient) {
    super(httpClient);
}

@Override
protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
    HttpHost targetHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
    AuthCache authCache = new BasicAuthCache();
    BasicScheme basicAuth = new BasicScheme();
    authCache.put(targetHost, basicAuth);
    BasicHttpContext localContext = new BasicHttpContext();
    localContext.setAttribute(ClientContext.AUTH_CACHE, authCache);
    return localContext;
}

private static HttpClient createSecureClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).useTLS().build();
    SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, new AllowAllHostnameVerifier());
    return HttpClientBuilder.create().setSSLSocketFactory(connectionFactory).build();
}

private static HttpClient createSecureClient(String username, String password) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
    SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).useTLS().build();
    SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, new AllowAllHostnameVerifier());
    return HttpClientBuilder.create().setSSLSocketFactory(connectionFactory).setDefaultCredentialsProvider(credentialsProvider).build();
}

public static RestTemplate createSSLTemplate(String username, String password) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    RestTemplate template = new RestTemplate(new BasicRequestFactory(createSecureClient(username, password)));
    template.setErrorHandler(new NopResponseErrorHandler());
    return template;
}

public static RestTemplate createTemplate(String username, String password) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    RestTemplate template = new RestTemplate(new BasicRequestFactory(createSecureClient(username, password)));
    template.setErrorHandler(new NopResponseErrorHandler());
    return template;
}

public static RestTemplate createTemplate() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    RestTemplate template = new RestTemplate(new BasicRequestFactory(createSecureClient()));
    template.setErrorHandler(new NopResponseErrorHandler());
    return template;
}

private static class NopResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse chr) throws IOException {
        return false;
    }

    @Override
    public void handleError(ClientHttpResponse chr) throws IOException {
    }
}

}