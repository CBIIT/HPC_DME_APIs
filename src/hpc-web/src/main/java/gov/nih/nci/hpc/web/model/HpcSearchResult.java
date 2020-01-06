package gov.nih.nci.hpc.web.model;

public class HpcSearchResult {

	private String path;
	private String download;
	private String permission;
	private String link;

	public String getPermission() {
		return permission;
	}

	public void setPermission(String permission) {
		this.permission = permission;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getDownload() {
		return download;
	}

	public void setDownload(String download) {
		this.download = download;
	}

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }
}
