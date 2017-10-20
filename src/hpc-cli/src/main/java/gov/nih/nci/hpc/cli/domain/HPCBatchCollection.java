/*******************************************************************************
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc.
 *  
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 ******************************************************************************/
package gov.nih.nci.hpc.cli.domain;

public class HPCBatchCollection {
	private String collectionType;
	private String collectionName;
	private String projectType;
	private String internalProjectID;
	private String collectionDescription;
	private String parentCollectionPath;
	private String sourceLabPI;
	private String LabBranchName;
	private String docPI;
	private String originalCreationDate;
	private String registrarName;
	private String registrarDOC;
	private String phiContent;
	private String piiContent;
	private String dataEncryptionStatus;
	private String dataCompressionStatus;
	private String fundingOrganization;
	private String Comments;
	private String datasetFilesFormat;
	private String flowCellID;
	private String runID;
	private String runDate;
	private String sequencingPlatform;
	private String sequencingApplicationType;
	private String libraryID;
	private String libraryName;
	private String libraryType;
	private String libraryProtocol;
	private String readType;
	private String readLength;

	public String getCollectionType() {
		return collectionType;
	}

	public void setCollectionType(String collectionType) {
		this.collectionType = collectionType;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public String getProjectType() {
		return projectType;
	}

	public void setProjectType(String projectType) {
		this.projectType = projectType;
	}

	public String getInternalProjectID() {
		return internalProjectID;
	}

	public void setInternalProjectID(String internalProjectID) {
		this.internalProjectID = internalProjectID;
	}

	public String getCollectionDescription() {
		return collectionDescription;
	}

	public void setCollectionDescription(String collectionDescription) {
		this.collectionDescription = collectionDescription;
	}

	public String getParentCollectionPath() {
		return parentCollectionPath;
	}

	public void setParentCollectionPath(String parentCollectionPath) {
		this.parentCollectionPath = parentCollectionPath;
	}

	public String getDocPI() {
		return docPI;
	}

	public void setDocPI(String docPI) {
		this.docPI = docPI;
	}

	public String getOriginalCreationDate() {
		return originalCreationDate;
	}

	public void setOriginalCreationDate(String originalCreationDate) {
		this.originalCreationDate = originalCreationDate;
	}

	public String getRegistrarName() {
		return registrarName;
	}

	public void setRegistrarName(String registrarName) {
		this.registrarName = registrarName;
	}

	public String getRegistrarDOC() {
		return registrarDOC;
	}

	public void setRegistrarDOC(String registrarDOC) {
		this.registrarDOC = registrarDOC;
	}

	public String getPhiContent() {
		return phiContent;
	}

	public void setPhiContent(String phiContent) {
		this.phiContent = phiContent;
	}

	public String getPiiContent() {
		return piiContent;
	}

	public void setPiiContent(String piiContent) {
		this.piiContent = piiContent;
	}

	public String getDataEncryptionStatus() {
		return dataEncryptionStatus;
	}

	public void setDataEncryptionStatus(String dataEncryptionStatus) {
		this.dataEncryptionStatus = dataEncryptionStatus;
	}

	public String getDataCompressionStatus() {
		return dataCompressionStatus;
	}

	public void setDataCompressionStatus(String dataCompressionStatus) {
		this.dataCompressionStatus = dataCompressionStatus;
	}

	public String getDatasetFilesFormat() {
		return datasetFilesFormat;
	}

	public void setDatasetFilesFormat(String datasetFilesFormat) {
		this.datasetFilesFormat = datasetFilesFormat;
	}

	public String getFlowCellID() {
		return flowCellID;
	}

	public void setFlowCellID(String flowCellID) {
		this.flowCellID = flowCellID;
	}

	public String getRunID() {
		return runID;
	}

	public void setRunID(String runID) {
		this.runID = runID;
	}

	public String getRunDate() {
		return runDate;
	}

	public void setRunDate(String runDate) {
		this.runDate = runDate;
	}

	public String getSequencingPlatform() {
		return sequencingPlatform;
	}

	public void setSequencingPlatform(String sequencingPlatform) {
		this.sequencingPlatform = sequencingPlatform;
	}

	public String getSequencingApplicationType() {
		return sequencingApplicationType;
	}

	public void setSequencingApplicationType(String sequencingApplicationType) {
		this.sequencingApplicationType = sequencingApplicationType;
	}

	public String getLibraryID() {
		return libraryID;
	}

	public void setLibraryID(String libraryID) {
		this.libraryID = libraryID;
	}

	public String getLibraryName() {
		return libraryName;
	}

	public void setLibraryName(String libraryName) {
		this.libraryName = libraryName;
	}

	public String getLibraryType() {
		return libraryType;
	}

	public void setLibraryType(String libraryType) {
		this.libraryType = libraryType;
	}

	public String getLibraryProtocol() {
		return libraryProtocol;
	}

	public void setLibraryProtocol(String libraryProtocol) {
		this.libraryProtocol = libraryProtocol;
	}

	public String getReadType() {
		return readType;
	}

	public void setReadType(String readType) {
		this.readType = readType;
	}

	public String getReadLength() {
		return readLength;
	}

	public void setReadLength(String readLength) {
		this.readLength = readLength;
	}

	public String getSourceLabPI() {
		return sourceLabPI;
	}

	public void setSourceLabPI(String sourceLabPI) {
		this.sourceLabPI = sourceLabPI;
	}

	public String getLabBranchName() {
		return LabBranchName;
	}

	public void setLabBranchName(String labBranchName) {
		LabBranchName = labBranchName;
	}

	public String getFundingOrganization() {
		return fundingOrganization;
	}

	public void setFundingOrganization(String fundingOrganization) {
		this.fundingOrganization = fundingOrganization;
	}

	public String getComments() {
		return Comments;
	}

	public void setComments(String comments) {
		Comments = comments;
	}
}
