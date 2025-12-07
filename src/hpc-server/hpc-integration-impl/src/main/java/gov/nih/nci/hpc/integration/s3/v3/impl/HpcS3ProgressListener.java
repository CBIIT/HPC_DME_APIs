/**
 * HpcS3ProgressListener.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration.s3.v3.impl;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

/**
 * HPC S3 Progress Listener.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcS3ProgressListener implements TransferListener {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// The transfer progress report rate (in bytes). Log and send transfer progress
	// notifications every 100MB.
	private static final long TRANSFER_PROGRESS_REPORTING_RATE = 1024L * 1024 * 100;
	private static final long MB = 1024L * 1024;

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// HPC progress listener
	private HpcDataTransferProgressListener progressListener = null;

	// Bytes transferred and logged.
	private AtomicLong bytesTransferred = new AtomicLong(0);
	private long bytesTransferredReported = 0;

	// Transfer source and/or destination (for logging purposed)
	private String transferSourceDestination = null;

	// Logger
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor.
	 *
	 * @param progressListener          The HPC progress listener.
	 * @param transferSourceDestination The transfer source and destination (for
	 *                                  logging progress).
	 * @throws HpcException if no progress listener provided.
	 */
	public HpcS3ProgressListener(HpcDataTransferProgressListener progressListener, String transferSourceDestination)
			throws HpcException {
		if (progressListener == null) {
			throw new HpcException("Null progress listener", HpcErrorType.UNEXPECTED_ERROR);
		}
		this.progressListener = progressListener;
		this.transferSourceDestination = transferSourceDestination;
	}

	/**
	 * Default Constructor.
	 *
	 * @throws HpcException Constructor is disabled.
	 */
	@SuppressWarnings("unused")
	private HpcS3ProgressListener() throws HpcException {
		throw new HpcException("Constructor Disabled", HpcErrorType.UNEXPECTED_ERROR);
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// ProgressListener Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void transferInitiated(TransferListener.Context.TransferInitiated context) {
		bytesTransferred.getAndSet(context.progressSnapshot().transferredBytes());
		bytesTransferredReported = bytesTransferred.get();
		logger.info("S3 transfer [{}] started. {} bytes transferred so far", transferSourceDestination,
				bytesTransferredReported);
	}

	@Override
	public void bytesTransferred(TransferListener.Context.BytesTransferred context) {
		bytesTransferred.getAndSet(context.progressSnapshot().transferredBytes());

		if (bytesTransferred.get() - bytesTransferredReported >= TRANSFER_PROGRESS_REPORTING_RATE) {
			bytesTransferredReported = bytesTransferred.get();
			logger.info("S3 transfer [{}] in progress. {}MB transferred so far", transferSourceDestination,
					bytesTransferredReported / MB);

			progressListener.transferProgressed(bytesTransferredReported);
		}
	}

	@Override
	public void transferComplete(TransferListener.Context.TransferComplete context) {
		bytesTransferred.getAndSet(context.progressSnapshot().transferredBytes());

		logger.info("S3 transfer [{}] completed. {} bytes transferred", transferSourceDestination, bytesTransferred.get());
		progressListener.transferCompleted(bytesTransferred.get());
	}

	@Override
	public void transferFailed(TransferListener.Context.TransferFailed context) {
		bytesTransferred.getAndSet(context.progressSnapshot().transferredBytes());

		logger.error("S3 transfer [{}] failed. {}MB transferred.", transferSourceDestination,
				bytesTransferred.get() / MB, context.exception());
		progressListener.transferFailed("S3 transfer failed:  " + context.exception().getMessage());
	}
}
