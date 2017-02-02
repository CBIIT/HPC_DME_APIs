package gov.nih.nci.hpc.web.model;

public class HpcDataColumn {
	private String field;
	private String width;
	private String displayName;
	private String cellTemplate;

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getCellTemplate() {
		return cellTemplate;
	}

	public void setCellTemplate(String cellTemplate) {
		this.cellTemplate = cellTemplate;
	}

}
