package gov.nih.nci.hpc.web.model;

public class HpcDownloadDatafile {

	private String endPointName;
	private String endPointLocation;
	private String destinationPath;
	private String downloadFileName;
	
	
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
	
}
