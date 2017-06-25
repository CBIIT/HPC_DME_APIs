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

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadCleanup;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.exception.HpcException;

import java.io.File;
import java.util.List;

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
     * @return A data object download response.
     * @throws HpcException on service failure.
     */
	public HpcDataObjectDownloadResponse downloadDataObject(
			                                     String path,
			                                     HpcFileLocation archiveLocation, 
			                                     HpcFileLocation destinationLocation,
			                                     HpcDataTransferType dataTransferType,
			                                     String doc) 
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
     * Get all data object download cleanup entries. 
     *
     * @return A list of data object download cleanup objects.
     * @throws HpcException on service failure.
     */
    public List<HpcDataObjectDownloadCleanup> getDataObjectDownloadCleanups() throws HpcException;
    
    /**
     * Cleanup data object download file. 
     *
     * @param dataObjectDownloadCleanup The info about the file to cleanup.
     * @throws HpcException on service failure.
     */
    public void cleanupDataObjectDownloadFile(
    		           HpcDataObjectDownloadCleanup dataObjectDownloadCleanup) throws HpcException;
}

 