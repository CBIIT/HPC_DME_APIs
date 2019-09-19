package gov.nih.nci.hpc.web.model;

import com.fasterxml.jackson.annotation.JsonView;

public class HpcNamedQuery {
	@JsonView(Views.Public.class)
	private String searchName;

	@JsonView(Views.Public.class)
	private String searchType;

	@JsonView(Views.Public.class)
	private String createdOn;

	@JsonView(Views.Public.class)
	private String delete;
	
	@JsonView(Views.Public.class)
	private String edit;

	public String getSearchName() {
		return searchName;
	}

	public void setSearchName(String searchName) {
		this.searchName = searchName;
	}

	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	public String getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(String createdOn) {
		this.createdOn = createdOn;
	}

	public String getDelete() {
		return delete;
	}

	public void setDelete(String delete) {
		this.delete = delete;
	}

	public String getEdit() {
		return edit;
	}

	public void setEdit(String edit) {
		this.edit = edit;
	}
}
