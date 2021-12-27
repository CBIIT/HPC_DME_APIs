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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.service.HpcDataTransferService;

/**
 * A data transfer listener for staged files uploads. (Sync, Globus 2nd Hop,
 * File System) to S3 archive.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcStagedFileUpload implements HpcDataTransferProgressListener {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The data object path that is being uploaded.
	private String path = null;

	// The data object ID that is being uploaded.
	private String dataObjectId = null;

	// The data object size that is being uploaded.
	private long size = -1;

	// Data Transfer service.
	private HpcDataTransferService dataTransferService = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructs a Staged file upload object to keep track of uploading a staged
	 * file (sync, Globus 2nd hop and FileSystem).
	 *
	 * @param path                The data object path that is being uploaded.
	 * @param dataObjectId        The data object ID that is being uploaded.
	 * @param size                The size of the data object being uploaded
	 * @param dataTransferService Data transfer service.
	 */
	public HpcStagedFileUpload(String path, String dataObjectId, long size,
			HpcDataTransferService dataTransferService) {
		this.path = path;
		this.dataObjectId = dataObjectId;
		this.size = size;
		this.dataTransferService = dataTransferService;
	}
	
	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataTransferProgressListener Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void transferCompleted(Long bytesTransferred) {
		logger.info("Staged file upload completed for: {}", path);
		dataTransferService.updateDataObjectUploadProgress(dataObjectId, 100);

		// Note: Completing the upload process is handled by a scheduled task.
	}

	@Override
	public void transferFailed(String message) {
		logger.error("Staged file upload failed for: {} - {}", path, message);

		// Note: Completing the upload process is handled by a scheduled task.
	}

	@Override
	public void transferProgressed(long bytesTransferred) {
		dataTransferService.updateDataObjectUploadProgress(dataObjectId,
				Math.round(100 * (float) bytesTransferred / size));
	}
}
