package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;

public class HpcDatafileModel {
	private String path;
	private HpcDataObject dataObject;
	private List<HpcMetadataAttrEntry> selfMetadataEntries;
	private List<HpcMetadataAttrEntry> selfSystemMetadataEntries;
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
		Collections.sort(selfMetadataEntries, new Comparator<HpcMetadataAttrEntry>() {
			@Override
			public int compare(HpcMetadataAttrEntry entry1, HpcMetadataAttrEntry entry2) {

				return entry1.getAttrName().compareTo(entry2.getAttrName());
			}
		});

		return selfMetadataEntries;
	}

	public void setSelfMetadataEntries(List<HpcMetadataAttrEntry> metadata) {
		this.selfMetadataEntries = metadata;
	}

	public List<HpcMetadataAttrEntry> getSelfSystemMetadataEntries() {
		if (selfSystemMetadataEntries == null)
			selfSystemMetadataEntries = new ArrayList<HpcMetadataAttrEntry>();
		Collections.sort(selfSystemMetadataEntries, new Comparator<HpcMetadataAttrEntry>() {
			@Override
			public int compare(HpcMetadataAttrEntry entry1, HpcMetadataAttrEntry entry2) {

				return entry1.getAttrName().compareTo(entry2.getAttrName());
			}
		});

		return selfSystemMetadataEntries;
	}

	public void setSelfSytemMetadataEntries(List<HpcMetadataAttrEntry> metadata) {
		this.selfSystemMetadataEntries = metadata;
	}
	
	public List<HpcMetadataAttrEntry> getParentMetadataEntries() {
		if (parentMetadataEntries == null)
			parentMetadataEntries = new ArrayList<HpcMetadataAttrEntry>();
		Collections.sort(parentMetadataEntries, new Comparator<HpcMetadataAttrEntry>() {
			@Override
			public int compare(HpcMetadataAttrEntry entry1, HpcMetadataAttrEntry entry2) {
				if (entry1.getAttrName() != null && entry2.getAttrName() != null)
					return entry1.getAttrName().compareTo(entry2.getAttrName());
				else
					return -1;
			}
		});

		return parentMetadataEntries;
	}

	public void setParentMetadataEntries(List<HpcMetadataAttrEntry> parentMetadataEntries) {
		this.parentMetadataEntries = parentMetadataEntries;
	}

}
