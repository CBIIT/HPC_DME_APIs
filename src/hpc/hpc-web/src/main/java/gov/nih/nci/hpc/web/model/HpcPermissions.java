package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.List;

public class HpcPermissions {
	private String path;
	private String type;
	private List<HpcPermissionEntry> entries;

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

	public List<HpcPermissionEntry> getEntries() {
		if (entries == null)
			entries = new ArrayList<HpcPermissionEntry>();
		return entries;
	}

	public void setEntries(List<HpcPermissionEntry> entries) {
		this.entries = entries;
	}
}
