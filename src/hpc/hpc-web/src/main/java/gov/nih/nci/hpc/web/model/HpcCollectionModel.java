package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;

public class HpcCollectionModel {
	private String path;
	private HpcCollection collection;
	private List<HpcMetadataAttrEntry> selfMetadataEntries;
	private List<HpcMetadataAttrEntry> parentMetadataEntries;

	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public HpcCollection getCollection() {
		return collection;
	}

	public void setCollection(HpcCollection collection) {
		this.collection = collection;
	}

	public List<HpcMetadataAttrEntry> getSelfMetadataEntries() {
		if(selfMetadataEntries == null)
			selfMetadataEntries = new ArrayList<HpcMetadataAttrEntry>();
		return selfMetadataEntries;
	}

	public void setSelfMetadataEntries(List<HpcMetadataAttrEntry> metadata) {
		this.selfMetadataEntries = metadata;
	}

	public List<HpcMetadataAttrEntry> getParentMetadataEntries() {
		if(parentMetadataEntries == null)
			parentMetadataEntries = new ArrayList<HpcMetadataAttrEntry>();
		return parentMetadataEntries;
	}

	public void setParentMetadataEntries(List<HpcMetadataAttrEntry> parentMetadataEntries) {
		this.parentMetadataEntries = parentMetadataEntries;
	}

}
