package Register.Pojo;

/*
    This is the equivalent of HpcStreamingUploadSource in domain-types/dataTransfer
 */
public class S3StreamingUploadPojo {

    SourceLocationPojo sourceLocation;
    S3AccountPojo account;
	String accessToken; // Used by Google Cloud and Google Drive

    public SourceLocationPojo getSourceLocation() {
      return sourceLocation;
    }
    public void setSourceLocation(SourceLocationPojo sourceLocation) {
      this.sourceLocation = sourceLocation;
    }
    public S3AccountPojo getAccount() {
      return account;
    }
    public void setAccount(S3AccountPojo account) {
      this.account = account;
    }
	public String getAccessToken() {
		return accessToken;
	}
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}    
}
