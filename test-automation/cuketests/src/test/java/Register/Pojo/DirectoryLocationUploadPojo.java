package Register.Pojo;

public class DirectoryLocationUploadPojo {

    SourceLocationPojo directoryLocation;
	S3AccountPojo account;
	String accessToken; // Used by Google Cloud and Google Drive

    public SourceLocationPojo getDirectoryLocation() {
		return directoryLocation;
	}
	public void setDirectoryLocation(SourceLocationPojo directoryLocation) {
		this.directoryLocation = directoryLocation;
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
