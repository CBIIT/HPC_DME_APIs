package Register.Pojo;

//hpc-dto/target/generated/src/main/java/gov/nih/nci/hpc/dto/datamanagement/v2/HpcDataObjectRegistrationRequestDTO.java

import java.util.List;
import java.util.Map;

public class DataObjectRegistration {
	
  List<Map<String, String>> dataObjectMetadataEntries;
  GoogleCloudUploadPojo googleCloudStorageUploadSource;
  S3StreamingUploadPojo s3UploadSource;
  GlobusUploadSourcePojo globusUploadSource;
  String path;
  BulkMetadataEntriesPojo parentCollectionsBulkMetadataEntries;
  boolean createParentCollections;

  public List<Map<String, String>> getDataObjectMetadataEntries() {
    return dataObjectMetadataEntries;
  }

  public void setDataObjectMetadataEntries(List<Map<String, String>> metadataEntries) {
    this.dataObjectMetadataEntries = metadataEntries;
  }

  public GoogleCloudUploadPojo getGoogleCloudStorageUploadSource() {
      return googleCloudStorageUploadSource;
  }
  public void setGoogleCloudStorageUploadSource(GoogleCloudUploadPojo googleCloudStorageUploadSource) {
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

  public GlobusUploadSourcePojo getGlobusUploadSource() {
	return globusUploadSource;
  }

  public void setGlobusUploadSource(GlobusUploadSourcePojo globusUploadSource) {
	this.globusUploadSource = globusUploadSource;
  }

  public BulkMetadataEntriesPojo getParentCollectionsBulkMetadataEntries() {
    return parentCollectionsBulkMetadataEntries;
  }

  public void setParentCollectionsBulkMetadataEntries(
      BulkMetadataEntriesPojo parentCollectionsBulkMetadataEntries) {
    this.parentCollectionsBulkMetadataEntries = parentCollectionsBulkMetadataEntries;
  }

  public boolean isCreateParentCollections() {
    return createParentCollections;
  }

  public void setCreateParentCollections(boolean createParentCollections) {
    this.createParentCollections = createParentCollections;
  }
}

