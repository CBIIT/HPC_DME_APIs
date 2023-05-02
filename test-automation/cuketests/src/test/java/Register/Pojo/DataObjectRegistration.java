package Register.Pojo;

import java.util.List;
import java.util.Map;

public class DataObjectRegistration {
	
	List<Map<String, String>> dataObjectMetadataEntries;	
	//ParentMetadataPojo parentCollectionsBulkMetadataEntries;
  GoogleCloudUploadPojo googleCloudStorageUploadSource;
  String path;


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
}

  /* public Boolean getCreateParentCollections() {
    return createParentCollections;
  }

  public void setCreateParentCollections(Boolean createParentCollections) {
    this.createParentCollections = createParentCollections;
  }

  public ParentMetadataPojo getParentCollectionsBulkMetadataEntries() {
    return parentCollectionsBulkMetadataEntries;
  }

  public void setParentCollectionsBulkMetadataEntries(
      ParentMetadataPojo parentCollectionsBulkMetadataEntries) {
    this.parentCollectionsBulkMetadataEntries = parentCollectionsBulkMetadataEntries;
  } */


