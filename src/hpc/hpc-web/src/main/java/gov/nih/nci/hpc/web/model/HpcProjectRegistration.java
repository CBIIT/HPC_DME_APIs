package gov.nih.nci.hpc.web.model;

public class HpcProjectRegistration {
	private String id;
	private String name;
	private String projectType;
	private String internalProjectId;
	private String principalInvestigatorNihUserId;
	private String principalInvestigatorDOC;
	private String registrarNihUserId;
	private String registrarDOC;
	private String labBranch;
	private String description;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProjectType() {
		return projectType;
	}

	public void setProjectType(String projectType) {
		this.projectType = projectType;
	}

	public String getInternalProjectId() {
		return internalProjectId;
	}

	public void setInternalProjectId(String internalProjectId) {
		this.internalProjectId = internalProjectId;
	}

	public String getPrincipalInvestigatorNihUserId() {
		return principalInvestigatorNihUserId;
	}

	public void setPrincipalInvestigatorNihUserId(
			String principalInvestigatorNihUserId) {
		this.principalInvestigatorNihUserId = principalInvestigatorNihUserId;
	}

	public String getPrincipalInvestigatorDOC() {
		return principalInvestigatorDOC;
	}

	public void setPrincipalInvestigatorDOC(String principalInvestigatorDOC) {
		this.principalInvestigatorDOC = principalInvestigatorDOC;
	}

	public String getRegistrarNihUserId() {
		return registrarNihUserId;
	}

	public void setRegistrarNihUserId(String registrarNihUserId) {
		this.registrarNihUserId = registrarNihUserId;
	}

	public String getRegistrarDOC() {
		return registrarDOC;
	}

	public void setRegistrarDOC(String registrarDOC) {
		this.registrarDOC = registrarDOC;
	}

	public String getLabBranch() {
		return labBranch;
	}

	public void setLabBranch(String labBranch) {
		this.labBranch = labBranch;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
