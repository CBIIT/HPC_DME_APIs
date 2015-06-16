package gov.nih.nci.hpc.web.model;

import org.hibernate.validator.constraints.NotEmpty;

public class HpcWebUser {
	private String id;
	

	@NotEmpty(message="NIH User Id is required")
	private String nihUserId;
	
	@NotEmpty(message="First name is required")
	private String firstName;
	
	@NotEmpty(message="Last name is required")
	private String lastName;
	
	private String nihUserPasswd;
	
	@NotEmpty(message="Globus User Id is required")
	private String globusUserId;
	
	@NotEmpty(message="Globus password is required")
	private String globusPasswd;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getNihUserId() {
		return nihUserId;
	}
	
	public String getNihUserPasswd() {
		return nihUserPasswd;
	}
	public void setNihUserPasswd(String nihUserPasswd) {
		this.nihUserPasswd = nihUserPasswd;
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
