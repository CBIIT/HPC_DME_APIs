package gov.nih.nci.hpc.web.model;
import java.util.List;

public class HpcBulkMetadataUpdateRequest {
	
	protected List<HpcMetadataAttrEntry> metadataList;
	protected String metadataName;
	protected String metadataValue;
	protected String selectedFilePaths;
	protected String globalMetadataSearchText;

	public String getGlobalMetadataSearchText() {
		return globalMetadataSearchText;
	}

	public void setGlobalMetadataSearchText(String globalMetadataSearchText) {
		this.globalMetadataSearchText = globalMetadataSearchText;
	}

	public String getSelectedFilePaths() {
		return selectedFilePaths;
	}

	public void setSelectedFilePaths(String selectedFilePaths) {
		this.selectedFilePaths = selectedFilePaths;
	}

	public List<HpcMetadataAttrEntry> getMetadataList() {
		return this.metadataList;
	}

	public void setMetadataList(List<HpcMetadataAttrEntry> metadataList) {
		this.metadataList = metadataList;
	}

	public String getMetadataValue() {
		return this.metadataValue;
	}

	public void setMetadataValue(String metadataValue) {
		this.metadataValue = metadataValue;
	}

	public String getMetadataName() {
		return this.metadataName;
	}

	public void setMetadataName(String metadataName) {
		this.metadataName = metadataName;
	}
}
