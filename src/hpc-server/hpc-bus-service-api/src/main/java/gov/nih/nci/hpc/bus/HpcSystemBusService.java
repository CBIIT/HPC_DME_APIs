/**
 * HpcSystemBusService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC System Business Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcSystemBusService 
{       
    /**
     * Submit a data transfer requests for the the upload requests that are queued (because
     * the data-transfer system was busy).
     *
     * @throws HpcException on service failure.
     */
	public void processDataTranferUploadReceived() throws HpcException;
	
    /**
     * Update the data transfer upload status of all data objects that the transfer is 'in progress'.
     *
     * @throws HpcException on service failure.
     */
	public void processDataTranferUploadInProgress() throws HpcException;
	
    /**
     * Transfer data objects currently in temporary archive to the (permanent) archive, 
     * and complete the registration process.
     *
     * @throws HpcException on service failure.
     */
	public void processTemporaryArchive() throws HpcException;
	
    /**
     * Check status of all active data objects download tasks and complete these that are no longer in progress.
     *
     * @throws HpcException on service failure.
     */
	public void completeDataObjectDownloadTasks() throws HpcException;
	
    /**
     * Process collection download tasks that received. i.e. kick off the download of individual data objects
     * under each requested collection.
     *
     * @throws HpcException on service failure.
     */
	public void processCollectionDownloadTasks() throws HpcException;
	
    /**
     * Check status of all active collection download tasks and complete these that are no longer in progress.
     *
     * @throws HpcException on service failure.
     */
	public void completeCollectionDownloadTasks() throws HpcException;
	
    /**
     * Process all (active) events.
     *
     * @throws HpcException on service failure.
     */
	public void processEvents() throws HpcException;

	/**
     * Generate summary report event.
     *
     * @throws HpcException on service failure.
     */
	public void generateSummaryReportEvent() throws HpcException;

	/**
     * Generate weekly summary report event.
     *
     * @throws HpcException on service failure.
     */
	public void generateWeeklySummaryReportEvent() throws HpcException;

	/**
     * Generate summary by DOC report event.
     *
     * @throws HpcException on service failure.
     */
	public void generateDocReportEvent() throws HpcException;

	/**
     * Generate summary by weekly DOC reports events.
     *
     * @throws HpcException on service failure.
     */
	public void generateWeeklyDocReportEvent() throws HpcException;

	/**
     * Generate user summary report event.
     *
     * @throws HpcException on service failure.
     */
	public void generateUserReportEvent() throws HpcException;

	/**
     * Generate weekly user summary report event.
     *
     * @throws HpcException on service failure.
     */
	public void generateWeeklyUserReportEvent() throws HpcException;
	
    /**
     * Refresh the metadata materialized views.
     *
     * @throws HpcException on service failure.
     */
	public void refreshMetadataViews() throws HpcException;
	
	/**
     * Refresh the reports materialized views.
     *
     * @throws HpcException on service failure.
     */
	public void refreshReportViews() throws HpcException;

	/**
	 * Close connection to Data Management system for the current service call.
	 */
	public void closeConnection();
}

 