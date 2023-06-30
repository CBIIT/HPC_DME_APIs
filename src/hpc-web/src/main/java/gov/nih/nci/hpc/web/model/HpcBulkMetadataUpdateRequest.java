package gov.nih.nci.hpc.web.model;
import java.util.List;

public class HpcBulkMetadataUpdateRequest {
	
  protected List<String> selectedFilePaths;
  protected List<HpcMetadataAttrEntry> metadataList;
  protected String metadataName;
  protected String metadataValue;

  public List<String> getSelectedFilePaths(){
    return selectedFilePaths;
  }
  
  public void setSelectedFilePaths(List<String> paths) {
    this.selectedFilePaths = paths;
  }
  
  public List<HpcMetadataAttrEntry> getMetadataList() {
    return this.metadataList;
  }
  public void setMetadataList(List<HpcMetadataAttrEntry> metadataList) {
    this.metadataList = metadataList;
  }
  public String getMetadataValue() {
    return this.metadataValue;
  }
  public void setMetadataValue(String metadataValue) {
    this.metadataValue = metadataValue;
  }
  public String getMetadataName() {
    return this.metadataName;
  }
  public void setMetadataName(String metadataName) {
    this.metadataName = metadataName;
  }
}
