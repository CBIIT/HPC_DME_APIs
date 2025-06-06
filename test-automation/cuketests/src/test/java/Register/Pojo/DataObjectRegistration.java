package Register.Pojo;

//hpc-dto/target/generated/src/main/java/gov/nih/nci/hpc/dto/datamanagement/v2/HpcDataObjectRegistrationRequestDTO.java

import java.util.List;
import java.util.Map;

public class DataObjectRegistration {

	List<Map<String, String>> dataObjectMetadataEntries;
	S3StreamingUploadPojo googleCloudStorageUploadSource;
	S3StreamingUploadPojo s3UploadSource;
	S3StreamingUploadPojo globusUploadSource;
	S3StreamingUploadPojo googleDriveUploadSource;
	S3StreamingUploadPojo fileUploadSource;
	String path;
	String linkSourcePath;
	String callerObjectId;
	BulkMetadataEntriesPojo parentCollectionsBulkMetadataEntries;
	String s3ArchiveConfigurationId;
	boolean createParentCollections;

	public List<Map<String, String>> getDataObjectMetadataEntries() {
		return dataObjectMetadataEntries;
	}

	public void setDataObjectMetadataEntries(List<Map<String, String>> metadataEntries) {
		this.dataObjectMetadataEntries = metadataEntries;
	}

	public S3StreamingUploadPojo getGoogleCloudStorageUploadSource() {
		return googleCloudStorageUploadSource;
	}

	public void setGoogleCloudStorageUploadSource(S3StreamingUploadPojo googleCloudStorageUploadSource) {
		this.googleCloudStorageUploadSource = googleCloudStorageUploadSource;
	}

	public String getPath() {
		return this.path;
	}

	public String setPath(String path) {
		return this.path = path;
	}

	public S3StreamingUploadPojo getS3UploadSource() {
		return s3UploadSource;
	}

	public void setS3UploadSource(S3StreamingUploadPojo s3UploadSource) {
		this.s3UploadSource = s3UploadSource;
	}

	public S3StreamingUploadPojo getGlobusUploadSource() {
		return globusUploadSource;
	}

	public void setGlobusUploadSource(S3StreamingUploadPojo globusUploadSource) {
		this.globusUploadSource = globusUploadSource;
	}

	public S3StreamingUploadPojo getGoogleDriveUploadSource() {
		return googleDriveUploadSource;
	}

	public void setGoogleDriveUploadSource(S3StreamingUploadPojo googleDriveUploadSource) {
		this.googleDriveUploadSource = googleDriveUploadSource;
	}

	public BulkMetadataEntriesPojo getParentCollectionsBulkMetadataEntries() {
		return parentCollectionsBulkMetadataEntries;
	}

	public void setParentCollectionsBulkMetadataEntries(BulkMetadataEntriesPojo parentCollectionsBulkMetadataEntries) {
		this.parentCollectionsBulkMetadataEntries = parentCollectionsBulkMetadataEntries;
	}

	public boolean isCreateParentCollections() {
		return createParentCollections;
	}

	public void setCreateParentCollections(boolean createParentCollections) {
		this.createParentCollections = createParentCollections;
	}

	public S3StreamingUploadPojo getFileUploadSource() {
		return fileUploadSource;
	}

	public void setFileUploadSource(S3StreamingUploadPojo fileUploadSource) {
		this.fileUploadSource = fileUploadSource;
	}

	public String getLinkSourcePath() {
		return linkSourcePath;
	}

	public void setLinkSourcePath(String linkSourcePath) {
		this.linkSourcePath = linkSourcePath;
	}

	public String getCallerObjectId() {
		return callerObjectId;
	}

	public void setCallerObjectId(String callerObjectId) {
		this.callerObjectId = callerObjectId;
	}

	public String getS3ArchiveConfigurationId() {
		return s3ArchiveConfigurationId;
	}

	public void setS3ArchiveConfigurationId(String s3ArchiveConfigurationId) {
		this.s3ArchiveConfigurationId = s3ArchiveConfigurationId;
	}
}
