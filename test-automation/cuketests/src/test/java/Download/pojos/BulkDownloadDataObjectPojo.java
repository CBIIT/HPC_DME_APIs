package Download.pojos;

import java.util.List;
import java.util.Map;


public class BulkDownloadDataObjectPojo {
	private final static long serialVersionUID = 1L;
    List<String> dataObjectPaths;
    List<String> collectionPaths;
    protected DestinationLocationPojo globusDownloadDestination;
    protected DestinationLocationPojo s3DownloadDestination;
    protected DestinationLocationPojo googleDriveDownloadDestination;
    protected DestinationLocationPojo googleCloudStorageDownloadDestination;
    protected DestinationLocationPojo asperaDownloadDestination;
	protected boolean appendPathToDownloadDestination;
	
  	public List<String> getDataObjectPaths() {
		return dataObjectPaths;
	}
	public void setDataObjectPaths(List<String> dataObjectPaths) {
		this.dataObjectPaths = dataObjectPaths;
	}
	public List<String> getCollectionPaths() {
		return collectionPaths;
	}
	public void setCollectionPaths(List<String> collectionPaths) {
		this.collectionPaths = collectionPaths;
	}
	public DestinationLocationPojo getGlobusDownloadDestination() {
		return globusDownloadDestination;
	}
	public void setGlobusDownloadDestination(DestinationLocationPojo globusDownloadDestination) {
		this.globusDownloadDestination = globusDownloadDestination;
	}
	public DestinationLocationPojo getS3DownloadDestination() {
		return s3DownloadDestination;
	}
	public void setS3DownloadDestination(DestinationLocationPojo s3DownloadDestination) {
		this.s3DownloadDestination = s3DownloadDestination;
	}
	public DestinationLocationPojo getGoogleDriveDownloadDestination() {
		return googleDriveDownloadDestination;
	}
	public void setGoogleDriveDownloadDestination(DestinationLocationPojo googleDriveDownloadDestination) {
		this.googleDriveDownloadDestination = googleDriveDownloadDestination;
	}
	public DestinationLocationPojo getGoogleCloudStorageDownloadDestination() {
		return googleCloudStorageDownloadDestination;
	}
	public void setGoogleCloudStorageDownloadDestination(DestinationLocationPojo googleCloudStorageDownloadDestination) {
		this.googleCloudStorageDownloadDestination = googleCloudStorageDownloadDestination;
	}
	public DestinationLocationPojo getAsperaDownloadDestination() {
		return asperaDownloadDestination;
	}
	public void setAsperaDownloadDestination(DestinationLocationPojo asperaDownloadDestination) {
		this.asperaDownloadDestination = asperaDownloadDestination;
	}
	public boolean getAppendPathToDownloadDestination() {
		return appendPathToDownloadDestination;
	}
	public void setAppendPathToDownloadDestination(boolean appendPathToDownloadDestination) {
		this.appendPathToDownloadDestination = appendPathToDownloadDestination;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	
}
