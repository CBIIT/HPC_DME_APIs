package gov.nih.nci.hpc.web.model;

public class HpcRegistration {
	private String id;
	private String projectName;
	private String investigatorName;
	private String originDataendpoint;
	private String originDataLocation;

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
	public String getInvestigatorName() {
		return investigatorName;
	}
	public void setInvestigatorName(String investigatorName) {
		this.investigatorName = investigatorName;
	}
	public String getOriginDataendpoint() {
		return originDataendpoint;
	}
	public void setOriginDataendpoint(String originDataendpoint) {
		this.originDataendpoint = originDataendpoint;
	}
	public String getOriginDataLocation() {
		return originDataLocation;
	}
	public void setOriginDataLocation(String originDataLocation) {
		this.originDataLocation = originDataLocation;
	}
	
	
}
