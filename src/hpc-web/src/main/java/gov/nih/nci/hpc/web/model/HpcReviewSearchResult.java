package gov.nih.nci.hpc.web.model;

public class HpcReviewSearchResult {

	private String path;
	private String projectTitle;
	private String projectDescription;
	private String startDate;
	private String dataOwner;
	private String dataCurator;
	private String dataCuratorName;
	private String projectStatus;
	private String publications;
	private String deposition;
	private String sunsetDate;
	private String retentionYears;
	private String completedDate;
	private String lastReviewed;
	private String reviewSent;
	private String reminderSent;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getProjectTitle() {
		return projectTitle;
	}

	public void setProjectTitle(String projectTitle) {
		this.projectTitle = projectTitle;
	}

	public String getProjectDescription() {
		return projectDescription;
	}

	public void setProjectDescription(String projectDescription) {
		this.projectDescription = projectDescription;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getDataOwner() {
		return dataOwner;
	}

	public void setDataOwner(String dataOwner) {
		this.dataOwner = dataOwner;
	}

	public String getDataCurator() {
		return dataCurator;
	}

	public void setDataCurator(String dataCurator) {
		this.dataCurator = dataCurator;
	}
	
	public String getDataCuratorName() {
		return dataCuratorName;
	}

	public void setDataCuratorName(String dataCuratorName) {
		this.dataCuratorName = dataCuratorName;
	}

	public String getProjectStatus() {
		return projectStatus;
	}

	public void setProjectStatus(String projectStatus) {
		this.projectStatus = projectStatus;
	}

	public String getPublications() {
		return publications;
	}

	public void setPublications(String publications) {
		this.publications = publications;
	}

	public String getSunsetDate() {
		return sunsetDate;
	}

	public void setSunsetDate(String sunsetDate) {
		this.sunsetDate = sunsetDate;
	}

	public String getRetentionYears() {
		return retentionYears;
	}

	public void setRetentionYears(String retentionYears) {
		this.retentionYears = retentionYears;
	}

	public String getCompletedDate() {
		return completedDate;
	}

	public void setCompletedDate(String completedDate) {
		this.completedDate = completedDate;
	}

	public String getLastReviewed() {
		return lastReviewed;
	}

	public void setLastReviewed(String lastReviewed) {
		this.lastReviewed = lastReviewed;
	}

	public String getReviewSent() {
		return reviewSent;
	}

	public void setReviewSent(String reviewSent) {
		this.reviewSent = reviewSent;
	}

	public String getReminderSent() {
		return reminderSent;
	}

	public void setReminderSent(String reminderSent) {
		this.reminderSent = reminderSent;
	}

	public String getDeposition() {
		return deposition;
	}

	public void setDeposition(String deposition) {
		this.deposition = deposition;
	}

}
