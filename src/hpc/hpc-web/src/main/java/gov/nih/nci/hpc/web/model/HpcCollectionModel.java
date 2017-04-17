package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;

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
		if (selfMetadataEntries == null)
			selfMetadataEntries = new ArrayList<HpcMetadataAttrEntry>();
		Collections.sort(selfMetadataEntries, new Comparator<HpcMetadataAttrEntry>() {
	        @Override
	        public int compare(HpcMetadataAttrEntry entry1, HpcMetadataAttrEntry entry2)
	        {

	            return  entry1.getAttrName().compareTo(entry2.getAttrName());
	        }
	    });		
		return selfMetadataEntries;
	}

	public void setSelfMetadataEntries(List<HpcMetadataAttrEntry> metadata) {
		this.selfMetadataEntries = metadata;
	}

	public List<HpcMetadataAttrEntry> getParentMetadataEntries() {
		if (parentMetadataEntries == null)
			parentMetadataEntries = new ArrayList<HpcMetadataAttrEntry>();
		Collections.sort(parentMetadataEntries, new Comparator<HpcMetadataAttrEntry>() {
	        @Override
	        public int compare(HpcMetadataAttrEntry entry1, HpcMetadataAttrEntry entry2)
	        {

	            return  entry1.getAttrName().compareTo(entry2.getAttrName());
	        }
	    });		
		return parentMetadataEntries;
	}

	public void setParentMetadataEntries(List<HpcMetadataAttrEntry> parentMetadataEntries) {
		this.parentMetadataEntries = parentMetadataEntries;
	}
}
