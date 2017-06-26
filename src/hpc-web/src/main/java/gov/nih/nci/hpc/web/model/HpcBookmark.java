package gov.nih.nci.hpc.web.model;

import com.fasterxml.jackson.annotation.JsonView;

public class HpcBookmark {

	@JsonView(Views.Public.class)
	private String name;
	@JsonView(Views.Public.class)
	private String path;
	@JsonView(Views.Public.class)
	private String selectedPath;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getSelectedPath() {
		return selectedPath;
	}
	public void setSelectedPath(String selectedPath) {
		this.selectedPath = selectedPath;
	}
}
