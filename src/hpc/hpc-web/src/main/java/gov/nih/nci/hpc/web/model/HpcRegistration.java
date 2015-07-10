package gov.nih.nci.hpc.web.model;

import java.util.HashMap;

public class HpcRegistration {
	private String id;
	private String projectName;
	private String investigatorName;
	private String originDataendpoint;
	private String originDataLocation;
	private HashMap dynamicFieldMap;

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
	public HashMap getDynamicFieldMap() {
		return dynamicFieldMap;
	}
	public void getDynamicFieldMap(HashMap dynamicFieldMap) {
		this.dynamicFieldMap = dynamicFieldMap;
	}	
	
}
