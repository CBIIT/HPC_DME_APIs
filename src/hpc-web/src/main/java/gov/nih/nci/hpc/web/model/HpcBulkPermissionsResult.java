package gov.nih.nci.hpc.web.model;

public class HpcBulkPermissionsResult {
	private String path;
	private String status;
	private String error;
	protected String globalMetadataSearchText;

	public String getGlobalMetadataSearchText() {
		return globalMetadataSearchText;
	}

	public void setGlobalMetadataSearchText(String globalMetadataSearchText) {
		this.globalMetadataSearchText = globalMetadataSearchText;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

}
