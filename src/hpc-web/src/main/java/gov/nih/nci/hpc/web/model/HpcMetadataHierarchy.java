package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class HpcMetadataHierarchy {
	private Map<String, String> levels;
	private List<String> collectionLevels;
	private List<String> dataobjectLevels;
	private List<String> allAttributes;
	private List<String> collectionAttrs;
	private List<String> dataobjectAttrs;
	private TreeSet<String> collectionAttrsSet;
	private TreeSet<String> dataobjectAttrsSet;

	public List<String> getCollectionLevels() {
		return collectionLevels;
	}

	public void setCollectionLevels(List<String> collectionLevels) {
		this.collectionLevels = collectionLevels;
	}

	public List<String> getDataobjectLevels() {
		return dataobjectLevels;
	}

	public void setDataobjectLevels(List<String> dataobjectLevels) {
		this.dataobjectLevels = dataobjectLevels;
	}

	public TreeSet<String> getCollectionAttrsSet() {
		if (collectionAttrsSet == null)
			collectionAttrsSet = new TreeSet<String>();
		return collectionAttrsSet;
	}

	public void setCollectionAttrsSet(TreeSet<String> collectionAttrsSet) {
		this.collectionAttrsSet = collectionAttrsSet;
	}

	public TreeSet<String> getDataobjectAttrsSet() {
		if (dataobjectAttrsSet == null)
			dataobjectAttrsSet = new TreeSet<String>();
		return dataobjectAttrsSet;
	}

	public void setDataobjectAttrsSet(TreeSet<String> dataobjectAttrsSet) {
		this.dataobjectAttrsSet = dataobjectAttrsSet;
	}

	public List<String> getCollectionAttrs() {
		if (collectionAttrs == null)
			collectionAttrs = new ArrayList<String>();
		return collectionAttrs;
	}

	public void setCollectionAttrs(List<String> collectionAttrs) {
		this.collectionAttrs = collectionAttrs;
	}

	public List<String> getDataobjectAttrs() {
		if (dataobjectAttrs == null)
			dataobjectAttrs = new ArrayList<String>();
		return dataobjectAttrs;
	}

	public void setDataobjectAttrs(List<String> dataobjectAttrs) {
		this.dataobjectAttrs = dataobjectAttrs;
	}

	public List<String> getAllAttributes() {
		return allAttributes;
	}

	public void setAllAttributes(List<String> allAttributes) {
		this.allAttributes = allAttributes;
	}

	public Map<String, String> getLevels() {
		return levels;
	}

	public void setLevels(Map<String, String> levels) {
		this.levels = levels;
	}

	public void addLevel(String level, String name) {
		if (levels == null)
			levels = new HashMap<String, String>();
		levels.put(level, name);
	}

}
