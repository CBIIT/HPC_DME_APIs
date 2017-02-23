package gov.nih.nci.hpc.web.model;

import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;

public class HpcDatafile {
	private String path;
	private HpcMetadataEntries metadata;
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public HpcMetadataEntries getMetadata() {
		return metadata;
	}
	public void setMetadata(HpcMetadataEntries metadata) {
		this.metadata = metadata;
	}
}
