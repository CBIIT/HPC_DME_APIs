package Register.Pojo;

import java.util.List;
import java.util.Map;

public class GoogleCloudUploadPojo {
	
	SourceLocationPojo sourceLocation;
	String accessToken;
	
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

}
