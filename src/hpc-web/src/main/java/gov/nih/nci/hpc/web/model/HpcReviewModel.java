package gov.nih.nci.hpc.web.model;

import java.util.List;

public class HpcReviewModel {

	private List<String> path;
	private String projectStatus;
	private String publication;
	private String retentionYears;
	private String lastReviewed;

	public List<String> getPath() {
		return path;
	}

	public void setPath(List<String> path) {
		this.path = path;
	}

	public String getProjectStatus() {
		return projectStatus;
	}

	public void setProjectStatus(String projectStatus) {
		this.projectStatus = projectStatus;
	}

	public String getPublication() {
		return publication;
	}

	public void setPublication(String publication) {
		this.publication = publication;
	}

	public String getRetentionYears() {
		return retentionYears;
	}

	public void setRetentionYears(String retentionYears) {
		this.retentionYears = retentionYears;
	}

	public String getLastReviewed() {
		return lastReviewed;
	}

	public void setLastReviewed(String lastReviewed) {
		this.lastReviewed = lastReviewed;
	}

}
