package gov.nih.nci.hpc.web.model;

import java.util.List;

public class HpcMetadataAttrEntry {
	private String attrName;
	private String attrValue;
	private String attrUnit;
	private boolean systemAttr;
	private boolean mandatory;
	private List<String> validValues = null;
	private String defaultValue = null;
	private String description;

	
	public List<String> getValidValues() {
		return validValues;
	}

	public void setValidValues(List<String> validValues) {
		this.validValues = validValues;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the mandatory
	 */
	public boolean isMandatory() {
		return mandatory;
	}

	/**
	 * @param mandatory the mandatory to set
	 */
	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

}
