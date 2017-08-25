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
import java.util.Calendar;
import java.util.List;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
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
     * @param userId The user ID submitting the download request.
     * @param completionEvent If true, an event will be added when async download is complete.
     * @return A data object download response.
     * @throws HpcException on service failure.
     */
	public HpcDataObjectDownloadResponse downloadDataObject(
			                                     String path,
			                                     HpcFileLocation archiveLocation, 
			                                     HpcFileLocation destinationLocation,
			                                     HpcDataTransferType dataTransferType,
			                                     String doc, String userId, 
			                                     boolean completionEvent) 
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
    public HpcDataTransferUploadReport getDataTransferUploadStatus(HpcDataTransferType dataTransferType,
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
    public HpcDataTransferDownloadReport getDataTransferDownloadStatus(HpcDataTransferType dataTransferType,
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
     * Get all data object download tasks for a given data-transfer type.
     *
     * @param dataTransferType The data-transfer type to query.
     * @return A list of data object download tasks.
     * @throws HpcException on service failure.
     */
    public List<HpcDataObjectDownloadTask> getDataObjectDownloadTasks(
    		                                  HpcDataTransferType dataTransferType) throws HpcException;
    
    /**
     * Get download task status. 
     *
     * @param taskId The download task ID.
     * @param taskType The download task type (data-object or collection).
     * @return A download status object, or null if the task can't be found.
     *         Note: The returned object is associated with a 'task' object if the download 
     *         is in-progress. If the download completed or failed, the returned object is associated with a 
     *         'result' object. 
     * @throws HpcException on service failure.
     */
    public HpcDownloadTaskStatus getDownloadTaskStatus(String taskId, HpcDownloadTaskType taskType) 
    		                                          throws HpcException;
    
    /**
     * Complete a data object download task:
     * 1. Cleanup any files staged in the file system for download.
     * 2. Update task info in DB with results info.
     *
     * @param downloadTask The download task to complete.
     * @param result The result of the task (true is successful, false is failed).
     * @param message (Optional) If the task failed, a message describing the failure.
     * @param completed (Optional) The download task completion timestamp.
     * @throws HpcException on service failure.
     */
    public void completeDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask,
    		                                   boolean result, String message, Calendar completed) 
    		                                  throws HpcException;
    
    /**
     * Continue a data object download task that was queued. 
     * Note: If Globus is still busy, the download task will remain queued.
     * 
     * @param downloadTask The download task to submit to Globus.
     * @throws HpcException on service failure.
     */
    public void continueDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask) 
                                              throws HpcException;
    
    /** 
     * Submit a request to download a collection.
     * 
     * @param path The collection path.
     * @param destinationLocation The user requested destination.
     * @param userId The user ID submitting the download request.
     * @param doc the DOC.
     * @return The submitted collection download task.
     * @throws HpcException on service failure.
     */
	public HpcCollectionDownloadTask downloadCollection(String path,
			                                            HpcFileLocation destinationLocation,
			                                            String userId, String doc)
			                                           throws HpcException;
	
    /** 
     * Submit a request to download data objects.
     * 
     * @param dataObjectPaths The list of data objects to download.
     * @param destinationLocation The user requested destination.
     * @param userId The user ID submitting the download request.
     * @param doc the DOC.
     * @return The submitted request download task.
     * @throws HpcException on service failure.
     */
	public HpcCollectionDownloadTask downloadDataObjects(List<String> dataObjectPaths,
			                                             HpcFileLocation destinationLocation,
			                                             String userId, String doc)
			                                            throws HpcException;
	
    /** 
     * Update a collection download request.
     * 
     * @param downloadRequest The collection download request to update.
     * @throws HpcException on service failure.
     */
	public void updateCollectionDownloadTask(HpcCollectionDownloadTask downloadTask)
			                                throws HpcException;
	
    /**
     * Get collection download tasks. 
     *
     * @param status Get tasks in this status.
     * @return A list of collection download tasks.
     * @throws HpcException on database error.
     */
    public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(
    		                                            HpcCollectionDownloadTaskStatus status) 
    		                                            throws HpcException;
    
    /**
     * Complete a collection download task:
     * 1. Update task info in DB with results info.
     *
     * @param downloadTask The download task to complete.
     * @param result The result of the task (true is successful, false is failed).
     * @param message (Optional) If the task failed, a message describing the failure.
     * @param completed (Optional) The download task completion timestamp.
     * @throws HpcException on service failure.
     */
    public void completeCollectionDownloadTask(HpcCollectionDownloadTask downloadTask,
    		                                   boolean result, String message, Calendar completed) 
    		                                  throws HpcException;
    
    /**
     * Get a file container name.
     *
     * @param dataTransferType The data transfer type.
     * @param doc The doc (needed to determine the archive connection config).
     * @param fileContainerId The file container ID.
     * @throws HpcException on data transfer system failure.
     */
    public String getFileContainerName(HpcDataTransferType dataTransferType,
                                       String doc, String fileContainerId) 
    		                          throws HpcException;
}

 