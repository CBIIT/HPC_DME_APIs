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

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.hpc.dao.HpcDataRegistrationDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcSecurityService;

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

	// The data object ID that is being uploaded.
	private String dataObjectId = null;

	// The data object size that is being uploaded.
	private long size = -1;

	// Metadata service.
	private HpcMetadataService metadataService = null;

	// Security service.
	private HpcSecurityService securityService = null;

	// Data object registration DAO
	private HpcDataRegistrationDAO dataRegistrationDAO = null;

	// Data Transfer service.
	private HpcDataTransferService dataTransferService = null;

	// Indicator whether this is an upload from Google Drive / Cloud Storage.
	private boolean googleUpload = false;

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
	 * @param dataObjectId        The data object ID that is being uploaded.
	 * @param size                The size of the data object being uploaded
	 * @param metadataService     Metadata service.
	 * @param securityService     Security service.
	 * @param dataTransferService Data transfer service.
	 * @param googleUpload        An indicator whether this is an upload from Google
	 *                            Drive / Cloud Storage
	 * @param dataRegistrationDAO Data object registration DAO.
	 */
	public HpcStreamingUpload(String path, String dataObjectId, long size, HpcMetadataService metadataService,
			HpcSecurityService securityService, HpcDataRegistrationDAO dataRegistrationDAO,
			HpcDataTransferService dataTransferService, boolean googleUpload) {
		this.path = path;
		this.dataObjectId = dataObjectId;
		this.size = size;
		this.metadataService = metadataService;
		this.securityService = securityService;
		this.dataRegistrationDAO = dataRegistrationDAO;
		this.dataTransferService = dataTransferService;
		this.googleUpload = googleUpload;
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
		dataTransferService.updateDataObjectUploadProgress(dataObjectId, 100);

		// Note: Completing the upload process is handled by
		// processDataTranferUploadStreamingInProgress() scheduled task.
	}

	@Override
	public void transferFailed(String message) {
		logger.error(
				"Streaming (AWS / 3rd Party S3 Provider / Google Drive / Google Cloud Storage) upload failed for: {} - {}",
				path, message);
		deleteGoogleAccessToken();

		try {
			securityService.executeAsSystemAccount(Optional.empty(),
					() -> metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null,
							HpcDataTransferUploadStatus.STREAMING_FAILED, null, null, null, null, null, null, null,
							null));

		} catch (HpcException e) {
			logger.error("Failed to update metadata for AWS / 3rd Party S3 Provider / Google Drive streaming failure",
					e);
		}

		// Note: Completing the upload process is handled by
		// processDataTranferUploadStreamingFailed() scheduled task.
	}

	@Override
	public void transferProgressed(long bytesTransferred) {
		dataTransferService.updateDataObjectUploadProgress(dataObjectId,
				Math.round(100 * (float) bytesTransferred / size));
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Delete a google access token, if one was persisted.
	 */
	private void deleteGoogleAccessToken() {
		if (googleUpload) {
			try {
				dataRegistrationDAO.deleteGoogleAccessToken(dataObjectId);

			} catch (HpcException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
}
