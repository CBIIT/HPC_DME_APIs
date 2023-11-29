package common;

/*
  This is the equivalent of HpcAsperaAccount in domain-types/dataTransfer
*/
public class AsperaAccountPojo {
    String user;
    String password;
    String host;
    
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
    
}