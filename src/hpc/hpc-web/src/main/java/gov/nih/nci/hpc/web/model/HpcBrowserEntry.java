package gov.nih.nci.hpc.web.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;

public class HpcBrowserEntry {
	@JsonView(Views.Public.class)
	private String name;

	@JsonView(Views.Public.class)
	private String fullPath;

	@JsonView(Views.Public.class)
	private List<HpcBrowserEntry> children;

	@JsonView(Views.Public.class)
	private boolean isFolder;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<HpcBrowserEntry> getChildren() {
		if (children == null)
			children = new ArrayList<HpcBrowserEntry>();
		return children;
	}

	public void setChildren(List<HpcBrowserEntry> children) {
		this.children = children;
	}

	public boolean isFolder() {
		return isFolder;
	}

	public void setFolder(boolean isFolder) {
		this.isFolder = isFolder;
	}

	public String getFullPath() {
		return fullPath;
	}

	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}
}
