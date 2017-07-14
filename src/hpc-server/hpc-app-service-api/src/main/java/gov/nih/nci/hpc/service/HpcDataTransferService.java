/**
 * HpcDataTransferService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import java.io.File;
import java.util.List;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Transfer Service Interface.
 * </p>
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 */

public interface HpcDataTransferService 
{    
    /** 
     * Upload data. Either upload from the input stream or submit a transfer request for the source.
     * 
     * @param sourceLocation The source for data transfer.
     * @param sourceFile The source file.
     * @param path The data object registration path.
     * @param userId The user-id who requested the data upload.
     * @param callerObjectId The caller's provided data object ID.
     * @param doc The doc (needed to determine the archive connection config).
     * @return A data object upload response.
     * @throws HpcException on service failure.
     */
	public HpcDataObjectUploadResponse uploadDataObject(HpcFileLocation sourceLocation, 
                                                        File sourceFile, 
                                                        String path, String userId,
                                                        String callerObjectId, String doc)
                                                       throws HpcException;
    
    /** 
     * Download a data object file.
     * 
     * @param path The data object path.
     * @param archiveLocation The archive file location.
     * @param destinationLocation The user requested file destination.
     * @param dataTransferType The data transfer type.
     * @param doc The doc (needed to determine the archive connection config).
     * @param collectionDownload True if this download request is part of a collection download request.
     * @return A data object download response.
     * @throws HpcException on service failure.
     */
	public HpcDataObjectDownloadResponse downloadDataObject(
			                                     String path,
			                                     HpcFileLocation archiveLocation, 
			                                     HpcFileLocation destinationLocation,
			                                     HpcDataTransferType dataTransferType,
			                                     String doc, boolean collectionDownload) 
			                                     throws HpcException;
	
    /** 
     * Delete a data object file.
     * 
     * @param fileLocation The file location.
     * @param dataTransferType The data transfer type.
     * @param doc The doc (needed to determine the archive connection config).
     * @throws HpcException on service failure.
     */
	public void deleteDataObject(HpcFileLocation fileLocation, 
                                 HpcDataTransferType dataTransferType,
                                 String doc) 
                                throws HpcException;
	
    /**
     * Get a data transfer upload request status.
     *
     * @param dataTransferType The data transfer type.
     * @param dataTransferRequestId The data transfer request ID.
     * @param doc The doc (needed to determine the archive connection config).
     * @return The data transfer upload request status.
     * @throws HpcException on service failure.
     */
    public HpcDataTransferUploadStatus getDataTransferUploadStatus(HpcDataTransferType dataTransferType,
    		                                                       String dataTransferRequestId,
    		                                                       String doc) 
    		                                                      throws HpcException;
    
    /**
     * Get a data transfer download request status.
     *
     * @param dataTransferType The data transfer type.
     * @param dataTransferRequestId The data transfer request ID.
     * @param doc The doc (needed to determine the archive connection config).
     * @return The data transfer download request status.
     * @throws HpcException on service failure.
     */
    public HpcDataTransferDownloadStatus getDataTransferDownloadStatus(HpcDataTransferType dataTransferType,
    		                                                           String dataTransferRequestId, String doc) 
    		                                                          throws HpcException;
    
    /**
     * Get the size of the data transferred of a specific request.
     *
     * @param dataTransferType The data transfer type.
     * @param dataTransferRequestId The data transfer request ID.
     * @param doc The doc (needed to determine the archive connection config).
     * @return The size of the data transferred in bytes.
     * @throws HpcException on service failure.
     */
    public long getDataTransferSize(HpcDataTransferType dataTransferType,
    		                        String dataTransferRequestId, String doc) 
    		                       throws HpcException;
    
    /**
     * Get endpoint/path attributes .
     *
     * @param dataTransferType The data transfer type.
     * @param fileLocation The endpoint/path to get attributes for.
     * @param getSize If set to true, the file/directory size will be returned. 
     * @param doc The doc (needed to determine the archive connection config).
     * @return The path attributes.
     * @throws HpcException on service failure.
     */
    public HpcPathAttributes getPathAttributes(HpcDataTransferType dataTransferType,
    		                                   HpcFileLocation fileLocation,
    		                                   boolean getSize, String doc) 
    		                                  throws HpcException;
    
    /**
     * Get a file from the archive.
     *
     * @param dataTransferType The data transfer type.
     * @param fileId The file ID.
     * @return The requested file from the archive.
     * @throws HpcException on service failure.
     */
    public File getArchiveFile(HpcDataTransferType dataTransferType,
    		                   String fileId)  
    		                  throws HpcException;
    
    /**
     * Get all data object download tasks that are in progress for a given data-transfer type
     *
     * @param dataTransferType The data-transfer type to query
     * @return A list of data object download tasks.
     * @throws HpcException on service failure.
     */
    public List<HpcDataObjectDownloadTask> getDataObjectDownloadTasks(
    		                                  HpcDataTransferType dataTransferType) throws HpcException;
    
    /**
     * Complete a data object download task:
     * 1. Cleanup any files staged in the file system for download.
     * 2. Update task info in DB with results info
     *
     * @param downloadTask The download task to complete.
     * @param result The result of the task (true is successful, false is failed).
     * @param message (Optional) If the task failed, a message describing the failure.
     * @throws HpcException on service failure.
     */
    public void completeDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask,
    		                                   boolean result, String message) 
    		                                  throws HpcException;
}

 