package gov.nih.nci.hpc.web.model;

public enum HpcPermissionEntryType {
	USER, GROUP;

	public String value() {
		return name();
	}

	public static HpcPermissionEntryType fromValue(String v) {
		return valueOf(v);
	}
}
