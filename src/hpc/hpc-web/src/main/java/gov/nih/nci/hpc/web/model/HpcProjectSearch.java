package gov.nih.nci.hpc.web.model;

import gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO;

import java.util.Date;
import java.util.List;

public class HpcProjectSearch {
	private String id;
	private String projectName;
	private String projectType;
	private String description;
	private String internalProjectId;
	private String investigatorId;
	private String registarId;
	private String branchName;
	private String searchDatasetId;
	private HpcDatasetCollectionDTO hpcDatasetCollectionDTO;
	private String createdOn;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getProjectName() {
		return projectName;
	}
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public String getProjectType() {
		return projectType;
	}
	public void setProjectType(String projectType) {
		this.projectType = projectType;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getInternalProjectId() {
		return internalProjectId;
	}
	public void setInternalProjectId(String internalProjectId) {
		this.internalProjectId = internalProjectId;
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
	public String getBranchName() {
		return branchName;
	}
	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}
	public String getSearchDatasetId() {
		return searchDatasetId;
	}
	public void setSearchDatasetId(String searchDatasetId) {
		this.searchDatasetId = searchDatasetId;
	}
	public HpcDatasetCollectionDTO getHpcDatasetCollectionDTO() {
		return hpcDatasetCollectionDTO;
	}
	public void setHpcDatasetCollectionDTO(
			HpcDatasetCollectionDTO hpcDatasetCollectionDTO) {
		this.hpcDatasetCollectionDTO = hpcDatasetCollectionDTO;
	}
	public String getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(String createdOn) {
		this.createdOn = createdOn;
	}
	
}
