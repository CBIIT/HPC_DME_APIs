/**
 * HpcDataDownloadDAO.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTaskStatusFilter;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcUserDownloadRequest;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Download DAO Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcDataDownloadDAO {
	/**
	 * Store a new data object download task (if dataObjectDownloadTask.getId() is
	 * provided NULL), or update an existing task. Note: If a new task is inserted,
	 * dataObjectDownloadTask.getId() will be updated with the generated ID.
	 *
	 * @param dataObjectDownloadTask The data object download task to persist.
	 * @throws HpcException on database error.
	 */
	public void upsertDataObjectDownloadTask(HpcDataObjectDownloadTask dataObjectDownloadTask) throws HpcException;

	/**
	 * Get a data object download task.
	 *
	 * @param id The data object download task ID.
	 * @return The download task object, or null if not found.
	 * @throws HpcException on database error.
	 */
	public HpcDataObjectDownloadTask getDataObjectDownloadTask(String id) throws HpcException;

	/**
	 * Delete a data object download task.
	 *
	 * @param id The data object download task ID.
	 * @throws HpcException on database error.
	 */
	public void deleteDataObjectDownloadTask(String id) throws HpcException;

	/**
	 * Update data object download task status.
	 *
	 * @param id       The data object download task ID to update.
	 * @param filters  list of query filters (combined w/ 'or').
	 * @param toStatus status to update to.
	 * @return true if the status was updated.
	 * @throws HpcException on database error.
	 */
	public boolean updateDataObjectDownloadTaskStatus(String id, List<HpcDataObjectDownloadTaskStatusFilter> filters,
			HpcDataTransferDownloadStatus toStatus) throws HpcException;

	/**
	 * Get data object download task status.
	 *
	 * @param id The data object download task ID to get status for.
	 * @return The task status or null if not found.
	 * @throws HpcException on database error.
	 */
	public HpcDataTransferDownloadStatus getDataObjectDownloadTaskStatus(String id) throws HpcException;

	/**
	 * Get data object download tasks.
	 *
	 * @return A list of data object download tasks.
	 * @throws HpcException on database error.
	 */
	public List<HpcDataObjectDownloadTask> getDataObjectDownloadTasks() throws HpcException;

	/**
	 * Get all data object download task by data transfer status
	 *
	 * @param dataTransferStatus The data object download task data transfer status.
	 * @return A data object download tasks.
	 * @throws HpcException on database error.
	 */
	public List<HpcDataObjectDownloadTask> getDataObjectDownloadTaskByStatus(
			HpcDataTransferDownloadStatus dataTransferStatus)
			throws HpcException;

	/**
	 * Get next data object download task by data transfer status and data transfer
	 * type.
	 *
	 * @param dataTransferStatus The data object download task data transfer status.
	 * @param dataTransferType   The data object download task data transfer type.
	 * @param processed          The processed date to pick up only records that
	 *                           have not yet been processed in this run.
	 * @return A data object download tasks.
	 * @throws HpcException on database error.
	 */
	public List<HpcDataObjectDownloadTask> getNextDataObjectDownloadTask(
			HpcDataTransferDownloadStatus dataTransferStatus, HpcDataTransferType dataTransferType, Date processed)
			throws HpcException;

	/**
	 * Set a data object download task in-process value.
	 *
	 * @param id        The data object download task ID.
	 * @param inProcess The value to set.
	 * @throws HpcException on database error.
	 */
	public void setDataObjectDownloadTaskInProcess(String id, boolean inProcess) throws HpcException;

	/**
	 * Reset all data object download tasks in-process value to false.
	 *
	 * @throws HpcException on database error.
	 */
	public void resetDataObjectDownloadTaskInProcess() throws HpcException;

	/**
	 * Set a data object download task processed value.
	 *
	 * @param id         The data object download task ID.
	 * @param processed The processed time.
	 * @throws HpcException on database error.
	 */
	public void setDataObjectDownloadTaskProcessed(String id, Calendar processed) throws HpcException;

	/**
	 * Store a new download task result, or updated an existing task result.
	 *
	 * @param downloadTaskResult The download task result to persist.
	 * @throws HpcException on database error.
	 */
	public void upsertDownloadTaskResult(HpcDownloadTaskResult downloadTaskResult) throws HpcException;

	/**
	 * Get a download task result.
	 *
	 * @param id       The download task ID.
	 * @param taskType The download task type (data-object or collection).
	 * @return The download task result object, or null if not found.
	 * @throws HpcException on database error.
	 */
	public HpcDownloadTaskResult getDownloadTaskResult(String id, HpcDownloadTaskType taskType) throws HpcException;

	/**
	 * Store a new collection download task (if collectionDownloadRequest.getId() is
	 * provided NULL), or update an existing request. Note: If a new request is
	 * inserted, collectionDownloadRequest.getId() will be updated with the
	 * generated ID.
	 *
	 * @param collectionDownloadtask The collection download task to persist.
	 * @throws HpcException on database error.
	 */
	public void upsertCollectionDownloadTask(HpcCollectionDownloadTask collectionDownloadtask) throws HpcException;

	/**
	 * Get a collection download task.
	 *
	 * @param id The collection download task ID.
	 * @return The download task object, or null if not found.
	 * @throws HpcException on database error.
	 */
	public HpcCollectionDownloadTask getCollectionDownloadTask(String id) throws HpcException;

	/**
	 * Delete a collection download task.
	 *
	 * @param id The collection download task ID.
	 * @throws HpcException on database error.
	 */
	public void deleteCollectionDownloadTask(String id) throws HpcException;

	/**
	 * Get collection download requests.
	 *
	 * @param status Get requests in this status.
	 * @return A list of collection download requests.
	 * @throws HpcException on database error.
	 */
	public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus status)
			throws HpcException;

	/**
	 * Get collection download requests.
	 *
	 * @param status    Get requests in this status.
	 * @param inProcess Indicator whether the task is being actively processed.
	 * @return A list of collection download requests.
	 * @throws HpcException on database error.
	 */
	public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus status,
			boolean inProcess) throws HpcException;

	/**
	 * Get collection download tasks count for a user w/ specific status and
	 * in-process indicator
	 *
	 * @param userId    The user ID to query for.
	 * @param status    Get requests in this status.
	 * @param inProcess Indicator whether the task is being actively processed.
	 * @return Count of collection download requests.
	 * @throws HpcException on database error.
	 */
	public int getCollectionDownloadTasksCount(String userId, HpcCollectionDownloadTaskStatus status, boolean inProcess)
			throws HpcException;

	/**
	 * Set a collection download task in-process value.
	 *
	 * @param id        The collection download task ID.
	 * @param inProcess The value to set.
	 * @throws HpcException on database error.
	 */
	public void setCollectionDownloadTaskInProcess(String id, boolean inProcess) throws HpcException;

	/**
	 * Reset a collection download task in-process value.
	 *
	 * @param id        The collection download task ID.
	 * @throws HpcException on database error.
	 */
	public void resetCollectionDownloadTaskInProcess(String id) throws HpcException;

	/**
	 * Set a collection download task cancellation request.
	 *
	 * @param id                  The collection download task ID.
	 * @param cancellationRequest The value to set.
	 * @throws HpcException on database error.
	 */
	public void setCollectionDownloadTaskCancellationRequested(String id, boolean cancellationRequest)
			throws HpcException;

	/**
	 * Get a collection download task cancellation request.
	 *
	 * @param id The collection download task ID.
	 * @return The value of 'cancellation requested' column for this collection
	 *         download task.
	 * @throws HpcException on database error.
	 */
	public boolean getCollectionDownloadTaskCancellationRequested(String id) throws HpcException;

	/**
	 * Get data object download requests for a user.
	 *
	 * @param userId The user ID to query for.
	 * @return A list of active data object download requests.
	 * @throws HpcException on database error.
	 */
	public List<HpcUserDownloadRequest> getDataObjectDownloadRequests(String userId) throws HpcException;

	/**
	 * Get collection download requests for a user.
	 *
	 * @param userId The user ID to query for.
	 * @return A list of active collection download requests.
	 * @throws HpcException on database error.
	 */
	public List<HpcUserDownloadRequest> getCollectionDownloadRequests(String userId) throws HpcException;

	/**
	 * Get download results for a user.
	 *
	 * @param userId The user ID to query for.
	 * @param offset Skip that many download-results in the returned results.
	 * @param limit  No more than 'limit' download-results will be returned.
	 * @return A list of completed download requests.
	 * @throws HpcException on database error.
	 */
	public List<HpcUserDownloadRequest> getDownloadResults(String userId, int offset, int limit) throws HpcException;

	/**
	 * Get download results count for a user.
	 *
	 * @param userId The user ID to query for.
	 * @return A total count of completed download requests.
	 * @throws HpcException on database error.
	 */
	public int getDownloadResultsCount(String userId) throws HpcException;

	/**
	 * Get data object download requests for a doc.
	 *
	 * @param doc The user ID to query for.
	 * @return A list of active data object download requests.
	 * @throws HpcException on database error.
	 */
	public List<HpcUserDownloadRequest> getDataObjectDownloadRequestsForDoc(String doc) throws HpcException;

	/**
	 * Get all data object download requests.
	 *
	 * @return A list of active data object download requests.
	 * @throws HpcException on database error.
	 */
	public List<HpcUserDownloadRequest> getAllDataObjectDownloadRequests() throws HpcException;

	/**
	 * Get collection download requests for a doc.
	 *
	 * @param doc The doc to query for.
	 * @return A list of active collection download requests.
	 * @throws HpcException on database error.
	 */
	public List<HpcUserDownloadRequest> getCollectionDownloadRequestsForDoc(String doc) throws HpcException;

	/**
	 * Get all collection download requests.
	 *
	 * @return A list of active collection download requests.
	 * @throws HpcException on database error.
	 */
	public List<HpcUserDownloadRequest> getAllCollectionDownloadRequests() throws HpcException;

	/**
	 * Get download results for a doc.
	 *
	 * @param doc    The doc to query for.
	 * @param offset Skip that many download-results in the returned results.
	 * @param limit  No more than 'limit' download-results will be returned.
	 * @return A list of completed download requests.
	 * @throws HpcException on database error.
	 */
	public List<HpcUserDownloadRequest> getDownloadResultsForDoc(String doc, int offset, int limit) throws HpcException;

	/**
	 * Get all download results.
	 *
	 * @param offset Skip that many download-results in the returned results.
	 * @param limit  No more than 'limit' download-results will be returned.
	 * @return A list of completed download requests.
	 * @throws HpcException on database error.
	 */
	public List<HpcUserDownloadRequest> getAllDownloadResults(int offset, int limit) throws HpcException;

	/**
	 * Get download results count for a doc.
	 *
	 * @param doc The doc to query for.
	 * @return A total count of completed download requests.
	 * @throws HpcException on database error.
	 */
	public int getDownloadResultsCountForDoc(String doc) throws HpcException;

	/**
	 * Get all download results count.
	 *
	 * @return A total count of completed download requests.
	 * @throws HpcException on database error.
	 */
	public int getAllDownloadResultsCount() throws HpcException;
}
