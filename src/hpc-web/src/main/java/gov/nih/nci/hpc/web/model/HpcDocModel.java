package gov.nih.nci.hpc.web.model;

public class HpcDocModel {

	private String doc;
	private String basePath;
	private String dataHierarchy;
	private String collectionMetadataValidationRules;
	private String dataObjectMetadataValidationRules;

	public String getDoc() {
		return doc;
	}

	public void setDoc(String doc) {
		this.doc = doc;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public String getDataHierarchy() {
		return dataHierarchy;
	}

	public void setDataHierarchy(String dataHierarchy) {
		this.dataHierarchy = dataHierarchy;
	}

	public String getCollectionMetadataValidationRules() {
		return collectionMetadataValidationRules;
	}

	public void setCollectionMetadataValidationRules(String collectionMetadataValidationRules) {
		this.collectionMetadataValidationRules = collectionMetadataValidationRules;
	}

	public String getDataObjectMetadataValidationRules() {
		return dataObjectMetadataValidationRules;
	}

	public void setDataObjectMetadataValidationRules(String dataObjectMetadataValidationRules) {
		this.dataObjectMetadataValidationRules = dataObjectMetadataValidationRules;
	}
}
