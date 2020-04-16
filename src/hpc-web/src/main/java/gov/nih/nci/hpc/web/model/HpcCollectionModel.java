package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;

public class HpcCollectionModel {
	private String path;
	private HpcCollection collection;
	private List<HpcMetadataAttrEntry> selfMetadataEntries;
	private List<HpcMetadataAttrEntry> selfSystemMetadataEntries;
	private List<HpcMetadataAttrEntry> parentMetadataEntries;
	private Map<String, String> attributes = new HashMap<String, String>();
	private String defaultValue;
	private List<String> validValues;
	
	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public List<String> getValidValues() {
		return validValues;
	}

	public void setValidValues(List<String> validValues) {
		this.validValues = validValues;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public void addAttribute(String attribute, String value) {
		this.attributes.put(attribute, value);
	}

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

	public void setSelfSystemMetadataEntries(List<HpcMetadataAttrEntry> metadata) {
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
