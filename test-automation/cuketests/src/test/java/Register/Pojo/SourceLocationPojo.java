package Register.Pojo;

/*
    This is the equivalent of HpcFileLocation in domain-types/dataTransfer
 */
public class SourceLocationPojo {

 String fileContainerId;
 String fileId;
 
  public String getFileContainerId() {
    return fileContainerId;
  }
  public void setFileContainerId(String fileContainerId) {
    this.fileContainerId = fileContainerId;
  }
  public String getFileId() {
    return fileId;
  }
  public void setFileId(String fileId) {
    this.fileId = fileId;
  }

}