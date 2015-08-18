package gov.nih.nci.hpc.web.model;

import java.util.List;

public class HpcDatasetSearchResult {
	private String id;
	private String datasetName;
	private String description;
	private String investigatorId;
	private String registarId;
	private String fundingOrganization;
	private String fileId;
	private String fileType;
	private List<String> projectIds;
	private String creatorName;
    private String createdOn;
    private String labBranch;

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
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getInvestigatorId() {
		return investigatorId;
	}
	public void setInvestigatorId(String investigatorId) {
		this.investigatorId = investigatorId;
	}
	public String getRegistarId() {
		return registarId;
	}
	public void setRegistarId(String registarId) {
		this.registarId = registarId;
	}
	public String getFundingOrganization() {
		return fundingOrganization;
	}
	public void setFundingOrganization(String fundingOrganization) {
		this.fundingOrganization = fundingOrganization;
	}
	public String getFileId() {
		return fileId;
	}
	public void setFileId(String fileId) {
		this.fileId = fileId;
	}
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	public List<String> getProjectIds() {
		return projectIds;
	}
	public void setProjectIds(List<String> projectIds) {
		this.projectIds = projectIds;
	}
	public String getCreatorName() {
		return creatorName;
	}
	public void setCreatorName(String creatorName) {
		this.creatorName = creatorName;
	}
	public String getLabBranch() {
		return labBranch;
	}
	public void setLabBranch(String labBranch) {
		this.labBranch = labBranch;
	}
	public String getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(String createdOn) {
		this.createdOn = createdOn;
	}

}
