package gov.nih.nci.hpc.web.model;

import com.fasterxml.jackson.annotation.JsonView;

public class HpcNotificationReceipt {
	@JsonView(Views.Public.class)
	private String eventId;

	@JsonView(Views.Public.class)
	private String eventType;

	@JsonView(Views.Public.class)
	private String eventCreated;

	@JsonView(Views.Public.class)
	private String notificationDeliveryMethod;

	@JsonView(Views.Public.class)
	private String deliveryStatus;

	@JsonView(Views.Public.class)
	private String delivered;

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getEventCreated() {
		return eventCreated;
	}

	public void setEventCreated(String eventCreated) {
		this.eventCreated = eventCreated;
	}

	public String getNotificationDeliveryMethod() {
		return notificationDeliveryMethod;
	}

	public void setNotificationDeliveryMethod(String notificationDeliveryMethod) {
		this.notificationDeliveryMethod = notificationDeliveryMethod;
	}

	public String getDeliveryStatus() {
		return deliveryStatus;
	}

	public void setDeliveryStatus(String deliveryStatus) {
		this.deliveryStatus = deliveryStatus;
	}

	public String getDelivered() {
		return delivered;
	}

	public void setDelivered(String delivered) {
		this.delivered = delivered;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}
}
