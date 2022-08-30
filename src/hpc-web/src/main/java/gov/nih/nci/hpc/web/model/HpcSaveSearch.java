package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.List;

public class HpcSaveSearch {
	private String criteriaName;
	private List<String> selectedColumns;

	public String getCriteriaName() {
		return criteriaName;
	}

	public void setCriteriaName(String criteriaName) {
		this.criteriaName = criteriaName;
	}

	public List<String> getSelectedColumns() {
		if(selectedColumns == null)
			selectedColumns = new ArrayList<String>();
		return selectedColumns;
	}

	public void setSelectedColumns(List<String> selectedColumns) {
		this.selectedColumns = selectedColumns;
	}

}
