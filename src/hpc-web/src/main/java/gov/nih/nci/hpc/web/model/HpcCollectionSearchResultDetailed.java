package gov.nih.nci.hpc.web.model;

import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;

public class HpcCollectionSearchResultDetailed {

	private String path;
	private String uuid;
	private String registeredBy;
	private String collectionType;
	private String createdOn;
	private String permission;
	private String download;
	HpcMetadataEntries metadataEntries;

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getRegisteredBy() {
		return registeredBy;
	}

	public void setRegisteredBy(String registeredBy) {
		this.registeredBy = registeredBy;
	}

	public String getCollectionType() {
		return collectionType;
	}

	public void setCollectionType(String collectionType) {
		this.collectionType = collectionType;
	}

	public String getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(String createdOn) {
		this.createdOn = createdOn;
	}

	public String getDownload() {
		return download;
	}

	public void setDownload(String download) {
		this.download = download;
	}

	public HpcMetadataEntries getMetadataEntries() {
		return metadataEntries;
	}

	public void setMetadataEntries(HpcMetadataEntries metadataEntries) {
		this.metadataEntries = metadataEntries;
	}
}
