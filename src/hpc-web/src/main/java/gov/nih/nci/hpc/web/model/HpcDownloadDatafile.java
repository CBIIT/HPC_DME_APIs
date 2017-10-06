package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.List;

public class HpcDownloadDatafile {

	private String endPointName;
	private String endPointLocation;
	private String destinationPath;
	private String downloadFileName;
	private String searchType;
	private String downloadType;
	private List<String> selectedPaths;

	
	public List<String> getSelectedPaths() {
		if(selectedPaths == null)
			selectedPaths = new ArrayList<String>();
		return selectedPaths;
	}

	public void setSelectedPaths(List<String> selectedPaths) {
		this.selectedPaths = selectedPaths;
	}

	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	public String getDownloadFileName() {
		return downloadFileName;
	}

	public void setDownloadFileName(String downloadFileName) {
		this.downloadFileName = downloadFileName;
	}

	public String getEndPointName() {
		return endPointName;
	}

	public void setEndPointName(String endPointName) {
		this.endPointName = endPointName;
	}

	public String getEndPointLocation() {
		return endPointLocation;
	}

	public void setEndPointLocation(String endPointLocation) {
		this.endPointLocation = endPointLocation;
	}

	public String getDestinationPath() {
		return destinationPath;
	}

	public void setDestinationPath(String destinationPath) {
		this.destinationPath = destinationPath;
	}

	public String getDownloadType() {
		return downloadType;
	}

	public void setDownloadType(String downloadType) {
		this.downloadType = downloadType;
	}
}
