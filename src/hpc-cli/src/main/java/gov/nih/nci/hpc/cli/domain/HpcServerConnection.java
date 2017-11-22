package gov.nih.nci.hpc.cli.domain;

import gov.nih.nci.hpc.cli.util.HpcBatchException;

public class HpcServerConnection {
	String hpcServerURL;
	String hpcServerProxyURL;
	String hpcServerProxyPort;
	String authToken;
	String hpcCertPath;
	String hpcCertPassword;
	int bufferSize;
	
	public HpcServerConnection(String hpcServerURL, String hpcServerProxyURL, String hpcServerProxyPort,
			String authToken, String hpcCertPath, String hpcCertPassword) throws HpcBatchException
	{
		if(hpcServerURL == null || hpcServerURL.isEmpty())
			throw new HpcBatchException("Invalid value for hpcServerURL: "+hpcServerURL);
		this.hpcServerURL = hpcServerURL;

		if(hpcCertPath == null || hpcCertPath.isEmpty())
			throw new HpcBatchException("Invalid value for hpcCertPath: "+hpcCertPath);
		this.hpcCertPath = hpcCertPath;

		if(hpcCertPassword == null || hpcCertPassword.isEmpty())
			throw new HpcBatchException("Invalid value for hpcCertPassword: "+hpcCertPassword);
		this.hpcCertPassword = hpcCertPassword;
		
		if(authToken == null || authToken.isEmpty())
			throw new HpcBatchException("Invalid value for authToken: "+authToken);
		this.authToken = authToken;
		
		this.hpcServerProxyURL = hpcServerProxyURL;
		this.hpcServerProxyPort = hpcServerProxyPort;
	}

	
	public int getBufferSize() {
		return bufferSize;
	}


	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}


	public String getHpcServerURL() {
		return hpcServerURL;
	}

	public String getHpcServerProxyURL() {
		return hpcServerProxyURL;
	}

	public String getHpcServerProxyPort() {
		return hpcServerProxyPort;
	}

	public String getAuthToken() {
		return authToken;
	}

	public String getHpcCertPath() {
		return hpcCertPath;
	}

	public String getHpcCertPassword() {
		return hpcCertPassword;
	}
}
