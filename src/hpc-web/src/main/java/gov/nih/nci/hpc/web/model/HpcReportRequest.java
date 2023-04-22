package gov.nih.nci.hpc.web.model;
import java.util.List;

public class HpcReportRequest {

	private String reportType;
	private String doc;
	private String user;
	private String basepath;
	private String path;
	private String fromDate;
	private String toDate;
	public boolean showArchiveSummary;
	private List<String> docs;
	private List<String> basepaths;

	public String getReportType() {
		return reportType;
	}

	public void setReportType(String reportType) {
		this.reportType = reportType;
	}

	public String getDoc() {
		return doc;
	}

	public void setDoc(String doc) {
		this.doc = doc;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getFromDate() {
		return fromDate;
	}

	public void setFromDate(String fromDate) {
		this.fromDate = fromDate;
	}

	public String getToDate() {
		return toDate;
	}

	public void setToDate(String toDate) {
		this.toDate = toDate;
	}

  public String getBasepath() {
    return basepath;
  }

  public void setBasepath(String basepath) {
    this.basepath = basepath;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public boolean getShowArchiveSummary() {
		return showArchiveSummary;
  }

  public void setShowArchiveSummary(boolean showArchiveSummary) {
		this.showArchiveSummary = showArchiveSummary;
  }

  public List<String> getDocs() {
    return docs;
  }

  public void setDocs(List<String> docs) {
    this.docs = docs;
  }

  public List<String> getBasepaths() {
	return basepaths;
  }

  public void setBasepaths(List<String> basepaths) {
	this.basepaths = basepaths;
  }
}
