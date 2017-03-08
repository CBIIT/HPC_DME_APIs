package gov.nih.nci.hpc.web.model;

public class HpcCompoundQueryCondition {
	private String queryPart;
	private String name;

	public HpcCompoundQueryCondition(String name, String queryPart) {
		this.queryPart = queryPart;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getQueryPart() {
		return queryPart;
	}

	public void setQueryPart(String queryPart) {
		this.queryPart = queryPart;
	}
}
