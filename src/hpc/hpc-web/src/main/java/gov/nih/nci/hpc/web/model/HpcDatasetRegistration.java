package gov.nih.nci.hpc.web.model;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.web.multipart.MultipartFile;

public class HpcDatasetRegistration {
	private String id;
	@NotEmpty(message="Dataset name is required")
	private String datasetName;
	@NotEmpty(message="Investigator name is required")
	private String investigatorName;
	@NotEmpty(message="Lab/Branch is required")
	private String branchName;
	@NotEmpty(message="PII is required")
	private String pii;
	@NotEmpty(message="Encrypted is required")
	private String encrypted;
	private String originEndpoint;
	private String originEndpointFilePath;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getDatasetName() {
		return datasetName;
	}
	public void setDatasetName(String datasetName) {
		this.datasetName = datasetName;
	}
	public String getInvestigatorName() {
		return investigatorName;
	}
	public void setInvestigatorName(String investigatorName) {
		this.investigatorName = investigatorName;
	}
	public String getBranchName() {
		return branchName;
	}
	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}
	public String getPii() {
		return pii;
	}
	public void setPii(String pii) {
		this.pii = pii;
	}
	public String getEncrypted() {
		return encrypted;
	}
	public void setEncrypted(String encrypted) {
		this.encrypted = encrypted;
	}
	public String getOriginEndpoint() {
		return originEndpoint;
	}
	public void setOriginEndpoint(String originEndpoint) {
		this.originEndpoint = originEndpoint;
	}
	public String getOriginEndpointFilePath() {
		return originEndpointFilePath;
	}
	public void setOriginEndpointFilePath(String originEndpointFilePath) {
		this.originEndpointFilePath = originEndpointFilePath;
	}

	
	
}
