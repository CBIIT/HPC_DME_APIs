package gov.nih.nci.hpc.web.model;

import com.fasterxml.jackson.annotation.JsonView;

public class HpcWebGroup {

	@JsonView(Views.Public.class)
	private String path;

	@JsonView(Views.Public.class)
	private String source;

	@JsonView(Views.Public.class)
	private String type;

	@JsonView(Views.Public.class)
	private String groupName;

	@JsonView(Views.Public.class)
	private String groupId;

	@JsonView(Views.Public.class)
	private String actionType;

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

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getActionType() {
		return actionType;
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}
}
