package gov.nih.nci.hpc.web.model;

import org.hibernate.validator.constraints.NotEmpty;

public class HpcLogin {

	@NotEmpty(message="NIH User Id is required")
	String userId;
	String passwd;

	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getPasswd() {
		return passwd;
	}
	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}
}
