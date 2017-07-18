/**
 * HpcDataDownloadDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import java.util.Calendar;
import java.util.List;

import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTaskResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Download Cleanup DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcDataDownloadDAO 
{    
    /**
     * Store a new data object download task (if dataObjectDownloadTask.getId() is provided NULL), 
     * or update an existing task.
     * Note: If a new task is inserted, dataObjectDownloadTask.getId() will be updated with the generated ID.
     *
     * @param dataObjectDownloadTask The data object download task to persist.
     * @throws HpcException on database error.
     */
    public void upsertDataObjectDownloadTask(HpcDataObjectDownloadTask dataObjectDownloadTask) 
    		                                throws HpcException;
    
    /**
     * Get a data object download task.
     *
     * @param id The data object download task ID.
     * @return The download task object, or null if not found.
     * @throws HpcException on database error.
     */
    public HpcDataObjectDownloadTask getDataObjectDownloadTask(int id)  throws HpcException;
    
    /**
     * Delete a data object download task.
     *
     * @param id The data object download task ID.
     * @throws HpcException on database error.
     */
    public void deleteDataObjectDownloadTask(int id) throws HpcException;
    
    /**
     * Get data object download tasks. 
     *
     * @param dataTransferType The data transfer type to get download tasks for.
     * @return A list of data object download tasks.
     * @throws HpcException on database error.
     */
    public List<HpcDataObjectDownloadTask> getDataObjectDownloadTasks(HpcDataTransferType dataTransferType) 
    		                                                         throws HpcException;
    
    /**
     * Store a new data object download task result, or updated an existing task result.
     *
     * @param dataObjectDownloadTask The data object download task result to persist.
     * @throws HpcException on database error.
     */
    public void upsertDataObjectDownloadResult(HpcDataObjectDownloadTask dataObjectDownloadTask,
    		                                   boolean result, String message, Calendar completed) 
    		                                  throws HpcException;
    
    /**
     * Get a data object download task result.
     *
     * @param id The data object download task ID.
     * @return The download task result object, or null if not found.
     * @throws HpcException on database error.
     */
    public HpcDataObjectDownloadTaskResult getDataObjectDownloadTaskResult(int id)  throws HpcException;
    
    /**
     * Store a new collection download task (if collectionDownloadRequest.getId() is provided NULL), 
     * or update an existing request.
     * Note: If a new request is inserted, collectionDownloadRequest.getId() will be updated with the generated ID.
     *
     * @param collectionDownloadtask The collection download task to persist.
     * @throws HpcException on database error.
     */
    public void upsertCollectionDownloadTask(HpcCollectionDownloadTask collectionDownloadtask) 
    		                                throws HpcException;
    
    /**
     * Get collection download requests. 
     *
     * @param status Get requests in this status.
     * @return A list of collection download requests.
     * @throws HpcException on database error.
     */
    public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(
    		                                            HpcCollectionDownloadTaskStatus status) 
    		                                            throws HpcException;
}

 