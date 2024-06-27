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
    protected String destinationType;
	@JsonView(Views.Public.class)
    protected String sourceType;
	@JsonView(Views.Public.class)
    protected String created;
	@JsonView(Views.Public.class)
    protected String sortCreated;
	@JsonView(Views.Public.class)
    protected String completed;
	@JsonView(Views.Public.class)
    protected String sortCompleted;
	@JsonView(Views.Public.class)
    protected String result;
	@JsonView(Views.Public.class)
    protected String status;
    @JsonView(Views.Public.class)
    protected String retryUserId;
    @JsonView(Views.Public.class)
    protected String error;
    @JsonView(Views.Public.class)
    protected String displayPath;

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
	public String getDestinationType() {
		return destinationType;
	}
	public void setDestinationType(String destinationType) {
		this.destinationType = destinationType;
	}
	public String getSourceType() {
		return sourceType;
	}
	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
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
	public String getSortCreated() {
		return sortCreated;
	}
	public void setSortCreated(String sortCreated) {
		this.sortCreated = sortCreated;
	}
	public String getSortCompleted() {
		return sortCompleted;
	}
	public void setSortCompleted(String sortCompleted) {
		this.sortCompleted = sortCompleted;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
    public String getRetryUserId() {
      return retryUserId;
    }
    public void setRetryUserId(String retryUserId) {
      this.retryUserId = retryUserId;
    }
    public String getError() {
      return error;
    }
    public void setError(String error) {
      this.error = error;
    }
	public String getDisplayPath() {
		return displayPath;
	}
	public void setDisplayPath(String displayPath) {
		this.displayPath = displayPath;
	}
}
