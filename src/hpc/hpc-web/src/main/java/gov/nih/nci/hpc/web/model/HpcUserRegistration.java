package gov.nih.nci.hpc.web.model;

public class HpcUserRegistration {
	private String nihUserId;
	private String globusUserId;
	private String globusPasswd;
	
	public String getNihUserId() {
		return nihUserId;
	}
	public void setNihUserId(String nihUserId) {
		this.nihUserId = nihUserId;
	}
	public String getGlobusUserId() {
		return globusUserId;
	}
	public void setGlobusUserId(String globusUserId) {
		this.globusUserId = globusUserId;
	}
	public String getGlobusPasswd() {
		return globusPasswd;
	}
	public void setGlobusPasswd(String globusPasswd) {
		this.globusPasswd = globusPasswd;
	}

	
}
