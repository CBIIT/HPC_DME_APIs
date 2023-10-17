package gov.nih.nci.hpc.integration.aspera.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;

/**
 * HPC Data Transfer Proxy Aspera Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataTransferProxyImpl implements HpcDataTransferProxy {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Aspera download executor.
	@Autowired
	@Qualifier("hpcAsperaDownloadExecutor")
	Executor asperaExecutor = null;

	// The maximum size of individual chunks that will get uploaded by single HTTP
	// request.
	@Value("${hpc.integration.googledrive.chunkSize}")
	int chunkSize = -1;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for spring injection. */
	private HpcDataTransferProxyImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataTransferProxy Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public String downloadDataObject(Object authenticatedToken, HpcDataObjectDownloadRequest downloadRequest,
			HpcArchive baseArchiveDestination, HpcDataTransferProgressListener progressListener,
			Boolean encryptedTransfer) throws HpcException {
		// Input validation
		if (progressListener == null) {
			throw new HpcException("[Aspera] No progress listener provided for a download to Aspera destination",
					HpcErrorType.UNEXPECTED_ERROR);
		}

		// Upload the file to Aspera.
		CompletableFuture<Void> googleDriveDownloadFuture = CompletableFuture.runAsync(() -> {
			try {
				

				progressListener.transferCompleted(downloadRequest.getSize());

			} catch (Exception e) {
				String message = "[Aspera] Failed to download object: " + e.getMessage();
				logger.error(message, HpcErrorType.DATA_TRANSFER_ERROR, e);
				progressListener.transferFailed(message);
			}

		}, asperaExecutor);

		return String.valueOf(googleDriveDownloadFuture.hashCode());
	}

}
