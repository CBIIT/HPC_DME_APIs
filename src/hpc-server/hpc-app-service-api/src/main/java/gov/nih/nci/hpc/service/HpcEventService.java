/**
 * HpcEventService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import java.util.Calendar;
import java.util.List;

import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationItem;
import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Event Application Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcEventService {
	/**
	 * Get all (active) events.
	 *
	 * @return A list of events.
	 * @throws HpcException on service failure.
	 */
	public List<HpcEvent> getEvents() throws HpcException;

	/**
	 * Archive an event. Move it from the active table to the history table.
	 *
	 * @param event The event to archive
	 */
	public void archiveEvent(HpcEvent event);

	/**
	 * Get an archived event.
	 *
	 * @param eventId The archived event ID to get.
	 * @return The archived event.
	 * @throws HpcException on service failure.
	 */
	public HpcEvent getArchivedEvent(int eventId) throws HpcException;

	/**
	 * Add a data transfer download completed event.
	 *
	 * @param userId                The user ID.
	 * @param path                  The data object path.
	 * @param downloadTaskType      The download task type.
	 * @param downloadTaskId        The download task ID.
	 * @param destinationLocation   The data transfer destination location.
	 * @param dataTransferCompleted The time the data download completed.
	 * @throws HpcException on service failure.
	 */
	public void addDataTransferDownloadCompletedEvent(String userId, String path, HpcDownloadTaskType downloadTaskType,
			String downloadTaskId, HpcFileLocation destinationLocation, Calendar dataTransferCompleted)
			throws HpcException;

	/**
	 * Add a data transfer download failed event.
	 *
	 * @param userId                The user ID.
	 * @param path                  The data object path.
	 * @param downloadTaskType      The download task type.
	 * @param downloadResult        The download task result.
	 * @param downloadTaskId        The download task ID.
	 * @param destinationLocation   The data transfer destination location.
	 * @param dataTransferCompleted The time the data download failed.
	 * @param errorMessage          The download failed error message.
	 * @throws HpcException on service failure.
	 */
	public void addDataTransferDownloadFailedEvent(String userId, String path, HpcDownloadTaskType downloadTaskType,
			HpcDownloadResult downloadTaskResult, String downloadTaskId, HpcFileLocation destinationLocation,
			Calendar dataTransferCompleted, String errorMessage) throws HpcException;

	/**
	 * Add a data transfer upload in temporary archive event.
	 *
	 * @param userId The user ID.
	 * @param path   The data object path.
	 * @throws HpcException on service failure.
	 */
	public void addDataTransferUploadInTemporaryArchiveEvent(String userId, String path) throws HpcException;

	/**
	 * Add a data transfer upload archived event.
	 *
	 * @param userId                The user ID.
	 * @param path                  The data object path.
	 * @param sourceLocation        The data transfer source location.
	 * @param dataTransferCompleted The time the data upload completed.
	 * @param presignURL            The presigned download URL.
	 * @param size                  The data size.
	 * @param doc                   The DOC.
	 * @throws HpcException on service failure.
	 */
	public void addDataTransferUploadArchivedEvent(String userId, String path, HpcFileLocation sourceLocation,
			Calendar dataTransferCompleted, String presignURL, String size, String doc) throws HpcException;

	/**
	 * Add a data transfer upload failed event.
	 *
	 * @param userId                The user ID.
	 * @param path                  The data object path.
	 * @param sourceLocation        The data transfer source location.
	 * @param dataTransferCompleted The time the data upload completed.
	 * @param errorMessage          the upload failed error message.
	 * @throws HpcException on service failure.
	 */
	public void addDataTransferUploadFailedEvent(String userId, String path, HpcFileLocation sourceLocation,
			Calendar dataTransferCompleted, String errorMessage) throws HpcException;

	/**
	 * Add a data transfer upload URL expired event.
	 *
	 * @param userId The user ID.
	 * @param path   The data object path.
	 * @throws HpcException on service failure.
	 */
	public void addDataTransferUploadURLExpiredEvent(String userId, String path) throws HpcException;

	/**
	 * Add a bulk data object registration completed event.
	 *
	 * @param userId             The user ID.
	 * @param registrationTaskId The data registration task ID.
	 * @param registrationItems  The data registration items.
	 * @param completed          The time the bulk registration task completed.
	 * @throws HpcException on service failure.
	 */
	public void addBulkDataObjectRegistrationCompletedEvent(String userId, String registrationTaskId,
			List<HpcBulkDataObjectRegistrationItem> registrationItems, Calendar completed) throws HpcException;

	/**
	 * Add a bulk data object registration failed event.
	 *
	 * @param userId             The user ID.
	 * @param registrationTaskId The data registration task ID.
	 * @param completed          The time the data registration task failed.
	 * @param errorMessage       the upload failed error message.
	 * @throws HpcException on service failure.
	 */
	public void addBulkDataObjectRegistrationFailedEvent(String userId, String registrationTaskId, Calendar completed,
			String errorMessage) throws HpcException;

	/**
	 * Generate reports event.
	 *
	 * @param userIds  The list of user ids to generate the events for.
	 * @param criteria The report criteria.
	 * @throws HpcException on service failure.
	 */
	public void generateReportsEvents(List<String> userIds, HpcReportCriteria criteria) throws HpcException;

	/**
	 * Add a collection updated event.
	 *
	 * @param path   The collection path.
	 * @param userId The user ID who initiated the action resulted in collection
	 *               update event.
	 * @throws HpcException on service failure.
	 */
	public void addCollectionUpdatedEvent(String path, String userId) throws HpcException;

	/**
	 * Add a collection registration event.
	 *
	 * @param path   The collection path.
	 * @param userId The user ID who initiated the action resulted in collection
	 *               update event.
	 * @throws HpcException on service failure.
	 */
	public void addCollectionRegistrationEvent(String path, String userId) throws HpcException;

	/**
	 * Add a data object registration event.
	 *
	 * @param collectionPath The collection path to which the data object was
	 *                       registered under.
	 * @param userId         The user ID who initiated the action resulted in data
	 *                       object registration event.
	 * @param presignURL     The presigned download URL.(Optional)
	 * @param size           The data size.(Optional)
	 * @param dataObjectPath The data object path.
	 * @param doc            The doc.
	 * @throws HpcException on service failure.
	 */
	public void addDataObjectRegistrationEvent(String collectionPath, String userId, String presignURL, String size,
			String dataObjectPath, String doc) throws HpcException;

	/**
	 * Add a restore request completed event.
	 *
	 * @param userId        The user ID.
	 * @param restoreTaskId The data registration task ID.
	 * @param path          The restore request path.
	 * @param completed     The time the bulk registration task completed.
	 * @throws HpcException on service failure.
	 */
	public void addRestoreRequestCompletedEvent(String userId, String restoreTaskId, String path, Calendar completed)
			throws HpcException;

	/**
	 * Add a restore request failed event.
	 *
	 * @param userId        The user ID.
	 * @param restoreTaskId The data registration task ID.
	 * @param path          The restore request path.
	 * @param completed     The time the data registration task failed.
	 * @param errorMessage  the upload failed error message.
	 * @throws HpcException on service failure.
	 */
	public void addRestoreRequestFailedEvent(String userId, String restoreTaskId, String path, Calendar completed,
			String errorMessage) throws HpcException;

}
