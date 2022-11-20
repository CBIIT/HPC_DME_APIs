package gov.nih.nci.hpc.web.model;

import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;

public class HpcDatafileSearchResultDetailed {
	private String path;
	private String uniqueId;
	private String registeredBy;
	private String collectionType;
	private String createdOn;
	private String checksum;
	private String download;
	private String permission;
	private String link;
	HpcMetadataEntries metadataEntries;

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public String getDownload() {
		return download;
	}

	public void setDownload(String download) {
		this.download = download;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
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

	public HpcMetadataEntries getMetadataEntries() {
		return metadataEntries;
	}

	public void setMetadataEntries(HpcMetadataEntries metadataEntries) {
		this.metadataEntries = metadataEntries;
	}

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

}
