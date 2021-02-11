/**
 * HpcDataTieringService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import java.util.Calendar;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.model.HpcBulkTierRequest;
import gov.nih.nci.hpc.exception.HpcException;


/**
 * HPC Data Tiering Application Service Interface.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public interface HpcDataTieringService {
	/**
	 * Create task to tier data object to Glacier
	 * 
	 * @param userId	The userId of the requester for auditing
	 * @param path The data object path
	 * @param hpcFileLocation The data object location
	 * @param dataTransferType The data transfer type
	 * @param configurationId The configuration id
	 * @throws HpcException on data transfer system failure.
	 */
	public void tierDataObject(String userId, String path, HpcFileLocation hpcFileLocation, HpcDataTransferType dataTransferType, String configurationId) throws HpcException;

	/**
	 * Create task to tier collection to Glacier
	 * 
	 * @param userId	The userId of the requester for auditing
	 * @param path The collection path
	 * @param dataTransferType The data transfer type
	 *  @param configurationId The configuration id
	 * @throws HpcException on data transfer system failure.
	 */
	public void tierCollection(String userId, String path, HpcDataTransferType dataTransferType, String configurationId) throws HpcException;

	/**
	 * Create task to tier data objects list to Glacier
	 * 
	 * @param userId	The userId of the requester for auditing
	 * @param bulkTierRequest The list of data objects and config id
	 * @param dataTransferType The data transfer type
	 * @throws HpcException on data transfer system failure.
	 */
	public void tierDataObjects(String userId, HpcBulkTierRequest bulkTierRequest, HpcDataTransferType dataTransferType) throws HpcException;

	/**
	 * Create task to tier collection list to Glacier
	 * 
	 * @param userId	The userId of the requester for auditing
	 * @param bulkTierRequest The list of collections and config id
	 * @param dataTransferType The data transfer type
	 * @throws HpcException on data transfer system failure.
	 */
	public void tierCollections(String userId, HpcBulkTierRequest bulkTierRequest, HpcDataTransferType dataTransferType) throws HpcException;

	/**
	 * Check if the archive provider supports tiering.
	 * @param configurationId The configuration ID
	 * @param s3ArchiveConfigurationId (Optional) The S3 configuration ID.
	 * @param dataTransferType (S3 or Globus)
	 * @return true if tiering is supported by this provider.
	 * @throws HpcException on data transfer system failure.
	 */
	public boolean isTieringSupported(String configurationId, String s3ArchiveConfigurationId,
			HpcDataTransferType dataTransferType) throws HpcException;

	/**
	 * Check if there is a delay in transition to deep archive.
	 *
	 * @param deepArchiveDate          The data/time the deep archive status went to in-progress
	 * @return True if it is delayed, or false otherwise.
	 */
	boolean deepArchiveDelayed(Calendar deepArchiveDate);
}
