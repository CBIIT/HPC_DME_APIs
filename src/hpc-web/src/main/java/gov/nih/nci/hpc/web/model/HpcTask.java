package gov.nih.nci.hpc.web.model;

import com.fasterxml.jackson.annotation.JsonView;

public class HpcTask {
	@JsonView(Views.Public.class)
    protected String userId;
	@JsonView(Views.Public.class)
    protected String taskId;
	@JsonView(Views.Public.class)
    protected String path;
	@JsonView(Views.Public.class)
    protected String type;
	@JsonView(Views.Public.class)
    protected String created;
	@JsonView(Views.Public.class)
    protected String completed;
	@JsonView(Views.Public.class)
    protected String result;
	@JsonView(Views.Public.class)
    protected String status;

	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getTaskId() {
		return taskId;
	}
	public void setTaskId(String taskId) {
		this.taskId = taskId;
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
	public String getCreated() {
		return created;
	}
	public void setCreated(String created) {
		this.created = created;
	}
	public String getCompleted() {
		return completed;
	}
	public void setCompleted(String completed) {
		this.completed = completed;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
}
