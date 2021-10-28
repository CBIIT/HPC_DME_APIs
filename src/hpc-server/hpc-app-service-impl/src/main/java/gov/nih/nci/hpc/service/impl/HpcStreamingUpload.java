/**
 * HpcStreamingUpload.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.util.Calendar;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.hpc.dao.HpcDataRegistrationDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.service.HpcEventService;

/**
 * A data transfer listener for async streaming uploads. The streaming is done
 * from AWS, 3rd Party S3 Provider, Google Drive, or Google Cloud Storage to S3
 * archive.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcStreamingUpload implements HpcDataTransferProgressListener {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The data object path that is being uploaded.
	private String path = null;

	// The user id registering the data object.
	private String userId = null;

	// The upload source location.
	private HpcFileLocation sourceLocation = null;

	// The data object ID that is being uploaded.
	private String dataObjectId = null;

	// Event service.
	private HpcEventService eventService = null;

	// Data object registration DAO
	private HpcDataRegistrationDAO dataRegistrationDAO = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructs a Streaming upload object (to keep track of async streaming
	 * upload)
	 *
	 * @param path                The data object path that is being uploaded.
	 * @param dataObjectId        (Optional) The data object ID that is being
	 *                            uploaded.
	 * @param userId              The user ID requested the upload.
	 * @param sourceLocation      The upload file location.
	 * @param eventService        event service.
	 * @param dataRegistrationDAO (Optional) Data object registration DAO.
	 */
	public HpcStreamingUpload(String path, String dataObjectId, String userId, HpcFileLocation sourceLocation,
			HpcEventService eventService, HpcDataRegistrationDAO dataRegistrationDAO) {
		this.path = path;
		this.dataObjectId = dataObjectId;
		this.userId = userId;
		this.sourceLocation = sourceLocation;
		this.eventService = eventService;
		this.dataRegistrationDAO = dataRegistrationDAO;
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataTransferProgressListener Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void transferCompleted(Long bytesTransferred) {
		logger.info("AWS / 3rd Party S3 Provider / Google Drive / Google Cloud Storage upload completed for: {}", path);
		deleteGoogleAccessToken();
	}

	@Override
	public void transferFailed(String message) {
		logger.error("Streaming (AWS / 3rd Party S3 Provider / Google Drive / Google Cloud Storage) upload failed for: {} - {}", path,
				message);
		deleteGoogleAccessToken();
		
		try {
			eventService.addDataTransferUploadFailedEvent(userId, path, sourceLocation, Calendar.getInstance(),
					message);
		} catch (HpcException e) {
			logger.error("Failed to send upload failed event for AWS / 3rd Party S3 Provider / Google Drive streaming",
					e);
		}
	}

	/**
	 * Delete a google access token, if one was persisted.
	 */
	private void deleteGoogleAccessToken() {
		if (!StringUtils.isEmpty(dataObjectId) && dataRegistrationDAO != null) {
			try {
				dataRegistrationDAO.deleteGoogleAccessToken(dataObjectId);

			} catch (HpcException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
}
