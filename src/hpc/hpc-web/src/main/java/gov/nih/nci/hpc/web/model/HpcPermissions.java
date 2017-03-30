package gov.nih.nci.hpc.web.model;

import java.util.TreeSet;

public class HpcPermissions {
	private String path;
	private String type;
	private String assignType;
	private TreeSet<HpcPermissionEntry> entries;

	public String getAssignType() {
		return assignType;
	}

	public void setAssignType(String assignType) {
		this.assignType = assignType;
	}

	public boolean isUserType() {
		return (assignType == null || assignType.equals("User"));
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

	public TreeSet<HpcPermissionEntry> getEntries() {
		if (entries == null)
			entries = new TreeSet<HpcPermissionEntry>();
		return entries;
	}

	public void setEntries(TreeSet<HpcPermissionEntry> entries) {
		this.entries = entries;
	}
}
