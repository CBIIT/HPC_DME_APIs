package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.List;

public class HpcSearch {

	private String[] rowId;
	private String[] level;
	private String[] levelOperator;
	private boolean[] selfAttributeOnly;
	private boolean[] encrypted;
	private String userKey;
	private String[] attrName;
	private String[] operator;
	private String[] attrValue;
	private boolean detailed = true;
	private String searchType;
	private String advancedCriteria;
	private String actionType;
	private int pageNumber=1;
	private int pageSize=500;
	private String queryName;
	private String globalMetadataSearchText;
	private String searchPathOrMetadata;
	private long totalSize=0;
	private List<String> deselectedColumns;

	public String getQueryName() {
		return queryName;
	}

	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}

	public String getGlobalMetadataSearchText() {
		return globalMetadataSearchText;
	}

	public void setGlobalMetadataSearchText(String globalMetadataSearchText) {
		this.globalMetadataSearchText = globalMetadataSearchText;
	}

	public String getSearchPathOrMetadata() {
		return searchPathOrMetadata;
	}

	public void setSearchPathOrMetadata(String searchPathOrMetadata) {
		this.searchPathOrMetadata = searchPathOrMetadata;
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

    public boolean[] getEncrypted() {
      return encrypted;
    }

    public void setEncrypted(boolean[] encrypted) {
      this.encrypted = encrypted;
    }

    public String getUserKey() {
      return userKey;
    }

    public void setUserKey(String userKey) {
      this.userKey = userKey;
    }
    
    public long getTotalSize() {
	  return totalSize;
	}
	
	public void setTotalSize(long totalSize) {
	  this.totalSize = totalSize;
	}

	public List<String> getDeselectedColumns() {
		if(deselectedColumns == null)
			deselectedColumns = new ArrayList<String>();
		return deselectedColumns;
	}

	public void setDeselectedColumns(List<String> deselectedColumns) {
		this.deselectedColumns = deselectedColumns;
	}
}
