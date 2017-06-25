package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.List;

public class HpcNotificationTriggerModel {

	private List<HpcNotificationTriggerModelEntry> entries;

	public List<HpcNotificationTriggerModelEntry> getEntries() {
		if (entries == null)
			entries = new ArrayList<HpcNotificationTriggerModelEntry>();
		return entries;
	}

	public void setEntries(List<HpcNotificationTriggerModelEntry> entries) {
		this.entries = entries;
	}

}
