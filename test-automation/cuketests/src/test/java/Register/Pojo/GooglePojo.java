package Register.Pojo;

import java.util.List;
import java.util.Map;

public class GooglePojo {
	
	SourceLocationPojo sourceLocation;
	String accessToken;
	List<Map<String, String>> metadataEntries;
	Boolean createParentCollections;
	
	ParentMetadataPojo parentCollectionsBulkMetadataEntries;

  public SourceLocationPojo getSourceLocation() {
    return sourceLocation;
  }

  public void setSourceLocation(SourceLocationPojo sourceLocation) {
    this.sourceLocation = sourceLocation;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public List<Map<String, String>> getMetadataEntries() {
    return metadataEntries;
  }

  public void setMetadataEntries(List<Map<String, String>> metadataEntries) {
    this.metadataEntries = metadataEntries;
  }

  public Boolean getCreateParentCollections() {
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
  }

}
