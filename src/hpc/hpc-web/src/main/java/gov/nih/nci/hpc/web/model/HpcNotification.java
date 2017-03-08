package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.List;

public class HpcNotification {
	private String eventType;
	private boolean subscribed;
	private String displayName;
	private List<HpcNotificationTriggerModel> triggers;

	public List<HpcNotificationTriggerModel> getTriggers() {
		if (triggers == null)
			triggers = new ArrayList<HpcNotificationTriggerModel>();
		return triggers;
	}

	public void setTriggers(List<HpcNotificationTriggerModel> triggers) {
		this.triggers = triggers;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public boolean isSubscribed() {
		return subscribed;
	}

	public void setSubscribed(boolean subscribed) {
		this.subscribed = subscribed;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
}
