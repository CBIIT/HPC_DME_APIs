package Download.pojos;

import Register.Pojo.S3AccountPojo;
import Register.Pojo.SourceLocationPojo;

public class DestinationLocationPojo {

	SourceLocationPojo destinationLocation;
	S3AccountPojo account;
	String accessToken; // Used by Google Cloud and Google Drive

	public SourceLocationPojo getDestinationLocation() {
		return destinationLocation;
	}

	public void setDestinationLocation(SourceLocationPojo destinationLocation) {
		this.destinationLocation = destinationLocation;
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