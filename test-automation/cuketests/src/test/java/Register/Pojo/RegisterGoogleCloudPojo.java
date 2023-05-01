package Register.Pojo;

import java.util.List;
import java.util.Map;

public class RegisterGoogleCloudPojo {
    
    String path;
    GooglePojo googleCloudStorageUploadSource;
    
    public String getPath() {
      return path;
    }
    public void setPath(String path) {
      this.path = path;
    }
    public GooglePojo getGoogleCloudStorageUploadSource() {
      return googleCloudStorageUploadSource;
    }
    public void setGoogleCloudStorageUploadSource(GooglePojo googleCloudStorageUploadSource) {
      this.googleCloudStorageUploadSource = googleCloudStorageUploadSource;
    }

}
