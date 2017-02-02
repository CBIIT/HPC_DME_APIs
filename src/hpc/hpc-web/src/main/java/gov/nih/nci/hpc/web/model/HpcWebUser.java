package gov.nih.nci.hpc.web.model;

import org.hibernate.validator.constraints.NotEmpty;

public class HpcWebUser {
	private String id;

	@NotEmpty(message = "NCI User Id is required")
	private String nciUserId;

	@NotEmpty(message = "First name is required")
	private String firstName;

	@NotEmpty(message = "Last name is required")
	private String lastName;

	private String nciUserPasswd;

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

	public String getNciUserId() {
		return nciUserId;
	}

	public String getNciUserPasswd() {
		return nciUserPasswd;
	}

	public void setNciUserPasswd(String nciUserPasswd) {
		this.nciUserPasswd = nciUserPasswd;
	}

	public void setNciUserId(String nciUserId) {
		this.nciUserId = nciUserId;
	}
}
