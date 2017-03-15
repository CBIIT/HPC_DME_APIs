package gov.nih.nci.hpc.web.model;

import com.fasterxml.jackson.annotation.JsonView;

public class HpcWebUser {

	@JsonView(Views.Public.class)
	private String path;

	@JsonView(Views.Public.class)
	private String type;
	
	@JsonView(Views.Public.class)
	private String nciUserId;

	@JsonView(Views.Public.class)
	private String firstName;

	@JsonView(Views.Public.class)
	private String lastName;

	@JsonView(Views.Public.class)
	private String nciUserPasswd;

	@JsonView(Views.Public.class)
	private String doc;

	public String getDoc() {
		return doc;
	}

	public void setDoc(String doc) {
		this.doc = doc;
	}

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

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
}
