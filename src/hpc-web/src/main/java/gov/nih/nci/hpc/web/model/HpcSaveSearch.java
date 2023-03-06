package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.List;

import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryFrequency;

public class HpcSaveSearch {
	private String criteriaName;
	private List<String> deselectedColumns;
	private HpcCompoundMetadataQueryFrequency frequency;

	public String getCriteriaName() {
		return criteriaName;
	}

	public void setCriteriaName(String criteriaName) {
		this.criteriaName = criteriaName;
	}

	public List<String> getDeselectedColumns() {
		if(deselectedColumns == null)
			deselectedColumns = new ArrayList<String>();
		return deselectedColumns;
	}

	public void setDeselectedColumns(List<String> deselectedColumns) {
		this.deselectedColumns = deselectedColumns;
	}

	public HpcCompoundMetadataQueryFrequency getFrequency() {
		return frequency;
	}

	public void setFrequency(HpcCompoundMetadataQueryFrequency frequency) {
		this.frequency = frequency;
	}

}
