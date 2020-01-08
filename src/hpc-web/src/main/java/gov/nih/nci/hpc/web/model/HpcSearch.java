package gov.nih.nci.hpc.web.model;

public class HpcSearch {

	private String[] rowId;
	private String[] level;
	private String[] levelOperator;
	private boolean[] selfAttributeOnly;
	private String[] attrName;
	private String[] operator;
	private String[] attrValue;
	private boolean detailed;
	private String searchType;
	private String advancedCriteria;
	private String actionType;
	private int pageNumber=1;
	private int pageSize=100;
	private String queryName;
	
	public String getQueryName() {
		return queryName;
	}

	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}

	public String[] getRowId() {
		return rowId;
	}

	public void setRowId(String[] rowId) {
		this.rowId = rowId;
	}

	public String getActionType() {
		return actionType;
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}

	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	public String[] getLevelOperator() {
		return levelOperator;
	}

	public void setLevelOperator(String[] levelOperator) {
		this.levelOperator = levelOperator;
	}

	public void setLevel(String[] level) {
		this.level = level;
	}

	public String[] getLevel() {
		return level;
	}

	public String[] getAttrName() {
		return attrName;
	}

	public void setAttrName(String[] attrName) {
		this.attrName = attrName;
	}

	public String[] getOperator() {
		return operator;
	}

	public void setOperator(String[] operator) {
		this.operator = operator;
	}

	public String[] getAttrValue() {
		return attrValue;
	}

	public void setAttrValue(String[] attrValue) {
		this.attrValue = attrValue;
	}

	public boolean isDetailed() {
		return detailed;
	}

	public void setDetailed(boolean detailed) {
		this.detailed = detailed;
	}

	public String getAdvancedCriteria() {
		return advancedCriteria;
	}

	public void setAdvancedCriteria(String advancedCriteria) {
		this.advancedCriteria = advancedCriteria;
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

    public int getPageSize() {
      return pageSize;
    }

    public void setPageSize(int pageSize) {
      this.pageSize = pageSize;
    }

    public boolean[] getSelfAttributeOnly() {
      return selfAttributeOnly;
    }

    public void setSelfAttributeOnly(boolean[] selfAttributeOnly) {
      this.selfAttributeOnly = selfAttributeOnly;
    }
}
