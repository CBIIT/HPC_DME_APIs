package gov.nih.nci.hpc.web.model;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.web.multipart.MultipartFile;

public class HpcDatasetRegistration {
	private String id;
	@NotEmpty(message="Dataset name is required")
	private String datasetName;
	@NotEmpty(message="Dataset description is required")
	private String description;
	private String comments;
	@NotEmpty(message="Investigator name is required")
	private String investigatorId;
	@NotEmpty(message="Creator name is required")
	private String creatorId;
	@NotEmpty(message="Lab/Branch is required")
	private String branchName;
	@NotEmpty(message="PII is required")
	private String pii;
	@NotEmpty(message="PHI is required")
	private String phi;
	@NotEmpty(message="Encrypted is required")
	private String encrypted;
	@NotEmpty(message="Compressed is required")
	private String compressed;
	@NotEmpty(message="Origin Endpoint is required")
	private String originEndpoint;
	@NotEmpty(message="Origin Endpoint File with Path is required")
	private String originEndpointFilePath;
	@NotEmpty(message="Funding organization is required")
	private String fundingOrganization;
	
	@NotEmpty(message="Data creation facility is required")
	private String dataCreationFacility;
	
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
	public String getInvestigatorId() {
		return investigatorId;
	}
	public void setInvestigatorId(String investigatorId) {
		this.investigatorId = investigatorId;
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
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	public String getCreatorId() {
		return creatorId;
	}
	public void setCreatorId(String creatorId) {
		this.creatorId = creatorId;
	}
	public String getFundingOrganization() {
		return fundingOrganization;
	}
	public void setFundingOrganization(String fundingOrganization) {
		this.fundingOrganization = fundingOrganization;
	}
	public String getDataCreationFacility() {
		return dataCreationFacility;
	}
	public void setDataCreationFacility(String dataCreationFacility) {
		this.dataCreationFacility = dataCreationFacility;
	}
	public String getPhi() {
		return phi;
	}
	public void setPhi(String phi) {
		this.phi = phi;
	}
	public String getCompressed() {
		return compressed;
	}
	public void setCompressed(String compressed) {
		this.compressed = compressed;
	}
}
