package gov.nih.nci.hpc.cli.domain;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;

@XmlRootElement
public class HPCDataObject{
	private String objectPath;
	private HpcDataObjectRegistrationDTO dto;
	private String basePath;
	private String hpcCertPath;
	private String hpcCertPassword;
	private String userId;
	private String password;
	private String authToken;
	
	public HPCDataObject() {
	}

	public String getObjectPath() {
		return objectPath;
	}

	public void setObjectPath(String objectPath) {
		this.objectPath = objectPath;
	}
	
	public HpcDataObjectRegistrationDTO getDto()
	{
		return dto;
	}
	
	public void setDto(HpcDataObjectRegistrationDTO dto)
	{
		this.dto = dto;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public String getHpcCertPath() {
		return hpcCertPath;
	}

	public void setHpcCertPath(String hpcCertPath) {
		this.hpcCertPath = hpcCertPath;
	}

	public String getHpcCertPassword() {
		return hpcCertPassword;
	}

	public void setHpcCertPassword(String hpcCertPassword) {
		this.hpcCertPassword = hpcCertPassword;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}
	
}
