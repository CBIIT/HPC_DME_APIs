package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HpcNotificationTriggerModel {
	
	private List<HpcNotificationTriggerModelEntry> entries;

	public List<HpcNotificationTriggerModelEntry> getEntries() {
		if(entries == null)
			entries = new ArrayList<HpcNotificationTriggerModelEntry>();
		return entries;
	}

	public void setEntries(List<HpcNotificationTriggerModelEntry> entries) {
		this.entries = entries;
	}

}
