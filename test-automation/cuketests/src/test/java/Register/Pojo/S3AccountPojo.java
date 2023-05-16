
package Register.Pojo;

/*
  This is the equivalent of HpcS3Account in domain-types/dataTransfer
*/
public class S3AccountPojo {

    String accessKey;
    String secretKey;
    String region;
    String url;
    //boolean pathStyleAccessEnabled;
    
    public String getAccessKey() {
      return accessKey;
    }
    public void setAccessKey(String accessKey) {
      this.accessKey = accessKey;
    }
    public String getSecretKey() {
      return secretKey;
    }
    public void setSecretKey(String secretKey) {
      this.secretKey = secretKey;
    }
    public String getRegion() {
      return region;
    }
    public void setRegion(String region) {
      this.region = region;
    }
    public String getUrl() {
      return url;
    }
    public void setUrl(String url) {
      this.url = url;
    }
}