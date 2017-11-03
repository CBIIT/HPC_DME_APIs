/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.util;

import java.io.Serializable;
import java.util.Comparator;

public class HpcPathAttributes implements Serializable, Comparable<HpcPathAttributes> {

	private final static long serialVersionUID = 1L;
	protected boolean exists;
	protected boolean isFile;
	protected boolean isDirectory;
	protected long size;
	protected boolean isAccessible;
	protected String updatedDate;
	protected String path;
	protected String absolutePath;
	protected String name;

	public String getAbsolutePath() {
		return absolutePath;
	}

	public void setAbsolutePath(String absolutePath) {
		this.absolutePath = absolutePath;
	}

	public String getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(String updatedDate) {
		this.updatedDate = updatedDate;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the value of the exists property. This getter has been renamed from
	 * isExists() to getExists() by cxf-xjc-boolean plugin.
	 * 
	 */
	public boolean getExists() {
		return exists;
	}

	/**
	 * Sets the value of the exists property.
	 * 
	 */
	public void setExists(boolean value) {
		this.exists = value;
	}

	/**
	 * Gets the value of the isFile property. This getter has been renamed from
	 * isIsFile() to getIsFile() by cxf-xjc-boolean plugin.
	 * 
	 */
	public boolean getIsFile() {
		return isFile;
	}

	/**
	 * Sets the value of the isFile property.
	 * 
	 */
	public void setIsFile(boolean value) {
		this.isFile = value;
	}

	/**
	 * Gets the value of the isDirectory property. This getter has been renamed
	 * from isIsDirectory() to getIsDirectory() by cxf-xjc-boolean plugin.
	 * 
	 */
	public boolean getIsDirectory() {
		return isDirectory;
	}

	/**
	 * Sets the value of the isDirectory property.
	 * 
	 */
	public void setIsDirectory(boolean value) {
		this.isDirectory = value;
	}

	/**
	 * Gets the value of the size property.
	 * 
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Sets the value of the size property.
	 * 
	 */
	public void setSize(long value) {
		this.size = value;
	}

	/**
	 * Gets the value of the isAccessible property. This getter has been renamed
	 * from isIsAccessible() to getIsAccessible() by cxf-xjc-boolean plugin.
	 * 
	 */
	public boolean getIsAccessible() {
		return isAccessible;
	}

	/**
	 * Sets the value of the isAccessible property.
	 * 
	 */
	public void setIsAccessible(boolean value) {
		this.isAccessible = value;
	}

	public static Comparator<HpcPathAttributes> pathComparator = new Comparator<HpcPathAttributes>() {

		public int compare(HpcPathAttributes path1, HpcPathAttributes path2) {

			String absolutePath1 = path1.getAbsolutePath().toUpperCase();
			String absolutePath2 = path1.getAbsolutePath().toUpperCase();

			// ascending order
			return absolutePath1.compareTo(absolutePath2);
		}
	};

	@Override
	public int compareTo(HpcPathAttributes o) {
		return this.absolutePath.compareTo(o.getAbsolutePath());
	}
}
