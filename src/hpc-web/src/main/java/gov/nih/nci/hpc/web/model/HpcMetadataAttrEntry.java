package gov.nih.nci.hpc.web.model;

public class HpcMetadataAttrEntry {
	private String attrName;
	private String attrValue;
	private String attrUnit;
	private boolean systemAttr;

	public String getAttrName() {
		return attrName;
	}

	public void setAttrName(String attrName) {
		this.attrName = attrName;
	}

	public String getAttrValue() {
		return attrValue;
	}

	public void setAttrValue(String attrValue) {
		this.attrValue = attrValue;
	}

	public String getAttrUnit() {
		return attrUnit;
	}

	public void setAttrUnit(String attrUnit) {
		this.attrUnit = attrUnit;
	}

	public boolean isSystemAttr() {
		return systemAttr;
	}

	public void setSystemAttr(boolean systemAttr) {
		this.systemAttr = systemAttr;
	}

}
