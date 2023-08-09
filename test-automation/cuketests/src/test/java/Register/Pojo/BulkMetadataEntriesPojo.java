package Register.Pojo;


import java.util.List;
import java.util.Map;


/*
  This is the equivalent of HpcBulkMetadataEntries in domain-types/metadata
*/
public class BulkMetadataEntriesPojo {
    List<Map<String, String>> defaultCollectionMetadataEntries;
    List<BulkMetadataEntry> pathsMetadataEntries;

    public List<Map<String, String>> getDefaultCollectionMetadataEntries() {
        return defaultCollectionMetadataEntries;
    }
    public void setDefaultCollectionMetadataEntries(
    List<Map<String, String>> defaultCollectionMetadataEntries) {
        this.defaultCollectionMetadataEntries = defaultCollectionMetadataEntries;
    }
    public List<BulkMetadataEntry> getPathsMetadataEntries() {
        return pathsMetadataEntries;
    }
    public void setPathsMetadataEntries(List<BulkMetadataEntry> pathsMetadataEntries) {
        this.pathsMetadataEntries = pathsMetadataEntries;
    }
}
