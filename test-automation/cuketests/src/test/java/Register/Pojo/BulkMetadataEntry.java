package Register.Pojo;


import java.util.List;
import java.util.Map;

/*
  This is the equivalent of HpcBulkMetadataEntry in domain-types/metadata
*/
public class BulkMetadataEntry {

    String path;
    List<Map<String, String>> pathMetadataEntries;
    public String getPath() {
      return path;
    }
    public void setPath(String path) {
      this.path = path;
    }
    public List<Map<String, String>> getPathMetadataEntries() {
      return pathMetadataEntries;
    }
    public void setPathMetadataEntries(List<Map<String, String>> pathMetadataEntries) {
      this.pathMetadataEntries = pathMetadataEntries;
    }
    
}
