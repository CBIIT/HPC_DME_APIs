/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.util;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class BasicAuthRestTemplate extends RestTemplate {

	public BasicAuthRestTemplate(String username, String password) {
		addAuthentication(username, password);
	}

	private void addAuthentication(String username, String password) {
		if (username == null) {
			return;
		}
		List<ClientHttpRequestInterceptor> interceptors = Collections
				.<ClientHttpRequestInterceptor> singletonList(new BasicAuthorizationInterceptor(username, password));
		setRequestFactory(new InterceptingClientHttpRequestFactory(getRequestFactory(), interceptors));
	}

	private static class BasicAuthorizationInterceptor implements ClientHttpRequestInterceptor {

		private final String username;

		private final String password;

		public BasicAuthorizationInterceptor(String username, String password) {
			this.username = username;
			this.password = (password == null ? "" : password);
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
				throws IOException {
			String token = DatatypeConverter.printBase64Binary((this.username + ":" + this.password).getBytes());
			// byte[] token = Base64.getEncoder().encode(
			// (this.username + ":" + this.password).getBytes());
			request.getHeaders().add("Authorization", "Basic " + token);
			return execution.execute(request, body);
		}
	}

}
