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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.service.HpcEventService;

/**
 * A data transfer listener for async streaming uploads. The streaming is done
 * from AWS, 3rd Party S3 Provider or Google Drive to S3 archive.
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

	// Event service.
	private HpcEventService eventService = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructs a S3 upload object (to keep track of async processing)
	 *
	 * @param path           The data object path that is being uploaded.
	 * @param userId         The user ID requested the upload.
	 * @param sourceLocation The upload file location.
	 * @param eventService   event service.
	 */
	public HpcStreamingUpload(String path, String userId, HpcFileLocation sourceLocation,
			HpcEventService eventService) {
		this.path = path;
		this.userId = userId;
		this.sourceLocation = sourceLocation;
		this.eventService = eventService;
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataTransferProgressListener Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void transferCompleted(Long bytesTransferred) {
		logger.info("AWS / 3rd Party S3 Provider / Google Drive upload completed for: {}", path);
	}

	@Override
	public void transferFailed(String message) {
		logger.error("Streaming (AWS / 3rd Party S3 Provider / Google Drive) upload failed for: {} - {}", path,
				message);
		try {
			eventService.addDataTransferUploadFailedEvent(userId, path, sourceLocation, Calendar.getInstance(),
					message);
		} catch (HpcException e) {
			logger.error("Failed to send upload failed event for AWS / 3rd Party S3 Provider / Google Drive streaming",
					e);
		}
	}
}
