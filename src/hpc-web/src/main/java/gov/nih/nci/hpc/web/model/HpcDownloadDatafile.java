package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.List;

public class HpcDownloadDatafile {

	private String endPointName;
	private String endPointLocation;
	private String destinationPath;
	private String downloadFileName;
	private String bucketName;
	private String s3Path;
	private String drivePath;
	private String googleCloudBucketName;
	private String googleCloudPath;
	private String accessKey;
	private String secretKey;
	private String region;
	private String asperaBucketName;
	private String asperaPath;
	private String asperaUser;
	private String asperaPassword;
	private String asperaHost;
	private String searchType;
	private String downloadType;
	private String downloadDestinationType;
	private List<String> selectedPaths;
	private String globalMetadataSearchText;
	private String boxContainerName;
	private String boxPath;	

	public String getGlobalMetadataSearchText() {
		return globalMetadataSearchText;
	}

	public void setGlobalMetadataSearchText(String globalMetadataSearchText) {
		this.globalMetadataSearchText = globalMetadataSearchText;
	}

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

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getS3Path() {
		return s3Path;
	}

	public void setS3Path(String s3Path) {
		this.s3Path = s3Path;
	}

	public String getDrivePath() {
      return drivePath;
    }

    public void setDrivePath(String drivePath) {
        this.drivePath = drivePath;
    }

	public String getGoogleCloudPath() {
		return googleCloudPath;
	}

	public void setGoogleCloudPath(String googleCloudPath) {
		this.googleCloudPath = googleCloudPath;
	}

	public String getGoogleCloudBucketName() {
		return googleCloudBucketName;
	}

	public void setGoogleCloudBucketName(String googleCloudBucketName) {
		this.googleCloudBucketName = googleCloudBucketName;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getAsperaUser() {
		return asperaUser;
	}

	public void setAsperaUser(String asperaUser) {
		this.asperaUser = asperaUser;
	}

	public String getAsperaPassword() {
		return asperaPassword;
	}

	public void setAsperaPassword(String asperaPassword) {
		this.asperaPassword = asperaPassword;
	}

	public String getAsperaHost() {
		return asperaHost;
	}

	public void setAsperaHost(String asperaHost) {
		this.asperaHost = asperaHost;
	}

	public String getAsperaBucketName() {
		return asperaBucketName;
	}

	public void setAsperaBucketName(String asperaBucketName) {
		this.asperaBucketName = asperaBucketName;
	}

	public String getAsperaPath() {
		return asperaPath;
	}

	public void setAsperaPath(String asperaPath) {
		this.asperaPath = asperaPath;
	}

	public String getDownloadDestinationType() {
		return downloadDestinationType;
	}

	public void setDownloadDestinationType(String downloadDestinationType) {
		this.downloadDestinationType = downloadDestinationType;
	}

	public String getBoxContainerName() {
		return boxContainerName;
	}

	public void setBoxContainerName(String boxContainerName) {
		this.boxContainerName = boxContainerName;
	}

	public String getBoxPath() {
		return boxPath;
	}

	public void setBoxPath(String boxPath) {
		this.boxPath = boxPath;
	}
	
}
