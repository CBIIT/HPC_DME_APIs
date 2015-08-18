package gov.nih.nci.hpc.web.model;

public class HpcDatasetSearch {
	private String id;
	private String datasetName;
	private String description;
	private String comments;
	private String investigatorId;
	private String registarId;
	private String creatorId;
	private String branchName;
	private String pii;
	private String phi;
	private String encrypted;
	private String compressed;
	private String fundingOrganization;
	private String dataCreationFacility;
	private String projectId;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRegistarId() {
		return registarId;
	}

	public void setRegistarId(String registarId) {
		this.registarId = registarId;
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

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}
}
