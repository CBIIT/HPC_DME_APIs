package gov.nih.nci.hpc.cli.domain;

public class HPCDataObject {
	private String source;
	private String collection;
	private String metadata;

	public HPCDataObject(String source, String metadata) {
		this.source = source;
		this.metadata = metadata;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getCollection() {
		return collection;
	}

	public void setCollection(String collection) {
		this.collection = collection;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}
}
