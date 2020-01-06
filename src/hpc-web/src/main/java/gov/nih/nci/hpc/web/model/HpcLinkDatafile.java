package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.List;

public class HpcLinkDatafile {

  private String sourcePath;
  private String destinationPath;
  private List<String> selectedPaths;

  public String getSourcePath() {
    return sourcePath;
  }

  public void setSourcePath(String sourcePath) {
    this.sourcePath = sourcePath;
  }

  public String getDestinationPath() {
    return destinationPath;
  }

  public void setDestinationPath(String destinationPath) {
    this.destinationPath = destinationPath;
  }

  public List<String> getSelectedPaths() {
    if (selectedPaths == null) selectedPaths = new ArrayList<String>();
    return selectedPaths;
  }

  public void setSelectedPaths(List<String> selectedPaths) {
    this.selectedPaths = selectedPaths;
  }
}
