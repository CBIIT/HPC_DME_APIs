/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.domain;

import java.util.Map;

public class HPCCollectionRecord {
	private String collectionId;
	private String absolutePath;
	private String collectionParentName;
	private String createdAt;
	private String modifiedAt;

	private Map<String, gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry> metadataAttrs;

	public String getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(String collectionId) {
		this.collectionId = collectionId;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public void setAbsolutePath(String absolutePath) {
		this.absolutePath = absolutePath;
	}

	public String getCollectionParentName() {
		return collectionParentName;
	}

	public void setCollectionParentName(String collectionParentName) {
		this.collectionParentName = collectionParentName;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getModifiedAt() {
		return modifiedAt;
	}

	public void setModifiedAt(String modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	public Map<String, gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry> getMetadataAttrs() {
		return metadataAttrs;
	}

	public void setMetadataAttrs(Map<String, gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry> metadataAttrs) {
		this.metadataAttrs = metadataAttrs;
	}

}
