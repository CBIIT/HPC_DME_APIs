/**
 * HpcDatasetService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
import gov.nih.nci.hpc.domain.model.HpcDataset;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Dataset Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDatasetService 
{         
    /**
     * Add dataset.
     *
     * @param name The dataset name.
     * @param description The dataset description.
     * @param comments The dataset comments.
     * @param persist If set to true, the dataset will be persisted.
     * @return The new dataset.
     * 
     * @throws HpcException
     */
    public HpcDataset addDataset(String name, String description, String comments,
    		                     boolean persist) 
    			                throws HpcException;
    
    /**
     * Add a file to a dataset.
     *
     * @param dataset The dataset.
     * @param uploadRequest A file upload request.
     * @param persist If set to true, the dataset will be persisted.
     * @return The new file.
     * 
     * @throws HpcException
     */
    public HpcFile addFile(HpcDataset dataset, HpcFileUploadRequest uploadRequest,
                           boolean persist) 
                          throws HpcException;
    
    /**
     * Add an upload data transfer request to a dataset.
     *
     * @param dataset The dataset.
     * @param requesterNihUserId The user-id initiated the upload.
     * @param fileId The uploaded file ID.
     * @param locations The transfer source and destination.
     * @param report The data transfer report.
     * @param persist If set to true, the dataset will be persisted.
     * @return The new data transfer request
     * 
     * @throws HpcException
     */
    public HpcDataTransferRequest addDataTransferUploadRequest(
    		              HpcDataset dataset, String requesterNihUserId, 
    		              String fileId, HpcDataTransferLocations locations,
                          HpcDataTransferReport report, boolean persist) 
                          throws HpcException;
    
    /**
     * Add a download data transfer request to a dataset.
     *
     * @param dataset The dataset.
     * @param requesterNihUserId The user-id initiated the upload.
     * @param fileId The uploaded file ID.
     * @param locations The transfer source and destination.
     * @param report The data transfer report.
     * @param persist If set to true, the dataset will be persisted.
     * @return The new data transfer request
     * 
     * @throws HpcException
     */
    public HpcDataTransferRequest addDataTransferDownloadRequest(
    		              HpcDataset dataset, String requesterNihUserId, 
    		              String fileId, HpcDataTransferLocations locations,
                          HpcDataTransferReport report, boolean persist) 
                          throws HpcException;
    
    /**
     * Add metadata items to a file primary metadata in a dataset.
     *
     * @param dataset The dataset.
     * @param fileId The file ID to add the metadata items to.
     * @param metadataItems The metadata items to add.
     * @param persist If set to true, the dataset will be persisted.
     * @return The updated file primary metadata domain object.
     * 
     * @throws HpcException
     */
    public HpcFilePrimaryMetadata 
           addPrimaryMetadataItems(HpcDataset dataset, String fileId,
        	                       List<HpcMetadataItem> metadataItems,
                                   boolean persist) 
                                  throws HpcException;
    
    /**
     * Update file primary metadata in a dataset.
     *
     * @param dataset The dataset.
     * @param fileId The file ID to update the metadata for.
     * @param primaryMetadata The metadata update request.
     * @param persist If set to true, the dataset will be persisted.
     * @return The updated file primary metadata domain object.
     * 
     * @throws HpcException
     */
    public HpcFilePrimaryMetadata 
           updatePrimaryMetadata(HpcDataset dataset, String fileId,
        		                 HpcFilePrimaryMetadata primaryMetadata, 
        		                 boolean persist) 
                                throws HpcException;
    
    /**
     * Set a data transfer request status based on a provided data transfer report.
     *
     * @param dataTransferRequest The data transfer request to update.
     * @param dataTransferReport The data transfer report.
     * @return True if the status was updated
     * 
     * @throws HpcException
     */
    public boolean setDataTransferRequestStatus(HpcDataTransferRequest dataTransferRequest, 
                                                HpcDataTransferReport dataTransferReport);
    
    /**
     * Get dataset.
     *
     * @param id The dataset ID.
     * @return The dataset.
     * 
     * @throws HpcException
     */
    public HpcDataset getDataset(String id) throws HpcException;
    
    /**
     * Get file.
     *
     * @param id The file ID.
     * @return The file.
     * 
     * @throws HpcException
     */
    public HpcFile getFile(String id) throws HpcException;
    
    /**
     * Get datasets associated with a specific user(s).
     *
     * @param userIds The list of user ids to match.
     * @param association The association between the dataset and the user.
     * @return HpcDataset collection, or null if no results found.
     * 
     * @throws HpcException
     */
    public List<HpcDataset> getDatasets(List<String> userIds, 
    		                            HpcDatasetUserAssociation association) 
        	                           throws HpcException;
    
    /**
     * Get datasets which has 'name' contained within its name.
     *
     * @param name Get datasets which 'name' is contained in their name.
     * @return HpcDataset collection, or null if no results found.
     * 
     * @throws HpcException
     */
    public List<HpcDataset> getDatasets(String name) throws HpcException;
    
    /**
     * Get datasets by Project Id
     *
     * @param projectId Get datasets by Project Id
     * @return HpcDataset collection, or null if no results found.
     * 
     * @throws HpcException
     */
    public List<HpcDataset> getDatasetsByProjectId(String projectId) throws HpcException;
    
    /**
     * Get datasets by primary metadata.
     *
     * @param primaryMetadata The meatada to match.
     * @return HpcDataset collection, or null if no results found.
     * 
     * @throws HpcException
     */
    public List<HpcDataset> getDatasets(HpcFilePrimaryMetadata primaryMetadata) 
    		                           throws HpcException;
    
    /**
     * Return true if a dataset with a specific name user association exists.
     *
     * @param nihUserId The user id to match.
     * @param association The association between the dataset and the user.
     * @return true if the dataset exists.
     * 
     * @throws HpcException
     */
    public boolean exists(String name, String nihUserId, 
    		              HpcDatasetUserAssociation association) 
        	             throws HpcException;
    
    /**
     * Get datasets with specific data transfer status.
     *
     * @param dataTransferStatus The data transfer status to query for.
     * @param uploadRequests Search the upload data transfer requests.
     * @param downloadRequests Search the download data transfer requests.
     * @return HpcDataset collection, or null if no results found.
     * 
     * @throws HpcException
     */
	public List<HpcDataset> getDatasets(HpcDataTransferStatus dataTransferStatus,
			                            Boolean uploadRequests, 
                                        Boolean downloadRequests) throws HpcException;
	
    /**
     * Persist dataset to the DB.
     *
     * @param dataset The dataset to be persisted.
     * 
     * @throws HpcException
     */
    public void persist(HpcDataset dataset) throws HpcException;
}

 