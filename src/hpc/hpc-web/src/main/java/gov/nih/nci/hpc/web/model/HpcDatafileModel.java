package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.List;

import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;

public class HpcDatafileModel {
	private String path;
	private HpcDataObject dataObject;
	private List<HpcMetadataAttrEntry> selfMetadataEntries;
	private List<HpcMetadataAttrEntry> parentMetadataEntries;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public HpcDataObject getDataObject() {
		return dataObject;
	}

	public void setDataObject(HpcDataObject dataObject) {
		this.dataObject = dataObject;
	}

	public List<HpcMetadataAttrEntry> getSelfMetadataEntries() {
		if (selfMetadataEntries == null)
			selfMetadataEntries = new ArrayList<HpcMetadataAttrEntry>();
		return selfMetadataEntries;
	}

	public void setSelfMetadataEntries(List<HpcMetadataAttrEntry> metadata) {
		this.selfMetadataEntries = metadata;
	}

	public List<HpcMetadataAttrEntry> getParentMetadataEntries() {
		if (parentMetadataEntries == null)
			parentMetadataEntries = new ArrayList<HpcMetadataAttrEntry>();
		return parentMetadataEntries;
	}

	public void setParentMetadataEntries(List<HpcMetadataAttrEntry> parentMetadataEntries) {
		this.parentMetadataEntries = parentMetadataEntries;
	}

}
