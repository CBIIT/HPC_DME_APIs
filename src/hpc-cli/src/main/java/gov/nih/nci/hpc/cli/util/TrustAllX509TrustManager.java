/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.util;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * This is an implementation of {@link X509TrustManager} that assumes all
 * clients and servers are trustworthy. The original motivation for it was to
 * provide a {@link TrustManager} that could accept self-signed certificates.
 */
public class TrustAllX509TrustManager implements X509TrustManager {

	private static final X509Certificate[] EMPTY_CERTIFICATES = new X509Certificate[0];

	public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		// Do nothing, the client is trusted
	}

	public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		// Do nothing, the server is trusted
	}

	public X509Certificate[] getAcceptedIssuers() {
		return EMPTY_CERTIFICATES;
	}

}
