package gov.nih.nci.hpc.cli.domain;

public class HPCDataObject {
	private String filename;
	private String location;
	private String metadataFile;
	private String objectType;

	public HPCDataObject(String filename, String location, String metadataFile, String objectType) {
		this.filename = filename;
		this.location = location;
		this.metadataFile = metadataFile;
		this.objectType = objectType;
	}

	public HPCDataObject(String filename, String location, String metadataFile) {
		this.filename = filename;
		this.location = location;
		this.metadataFile = metadataFile;
	}
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getMetadataFile() {
		return metadataFile;
	}

	public void setMetadataFile(String metadataFile) {
		this.metadataFile = metadataFile;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

}
