package gov.nih.nci.hpc.web.model;

public class HpcNotificationRequest {
	private String userId;
	private String[] eventType;

	public String[] getEventType() {
		return eventType;
	}

	public void setEventType(String[] eventType) {
		this.eventType = eventType;
	}
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

}
