package gov.nih.nci.hpc.web.model;

import com.fasterxml.jackson.annotation.JsonView;

public class HpcWebGroup {

	@JsonView(Views.Public.class)
	private String path;

	@JsonView(Views.Public.class)
	private String type;
	
	@JsonView(Views.Public.class)
	private String groupName;

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
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
