/**
 * HpcS3ProgressListener.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration.s3.v2.impl;

import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import software.amazon.awssdk.transfer.s3.model.ObjectTransfer;
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

	// Indicator whether the transfer completion / failure was reported to the HPC
	// progress listener. It is reported once - by the AWS SDK callback or the
	// transfer's completion future, whichever comes first.
	private final AtomicBoolean transferEnded = new AtomicBoolean(false);

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

	/**
	 * Report the transfer completion / failure when the transfer's completion
	 * future completes. The AWS SDK does not call transferComplete() for a file
	 * download w/ a multipart enabled Java based (Netty-NIO) S3 client, so the
	 * completion future is used to report it.
	 *
	 * @param transfer The transfer to report its completion / failure.
	 */
	public void registerCompletion(ObjectTransfer transfer) {
		transfer.completionFuture().whenComplete((completedTransfer, exception) -> {
			// Exceptions thrown here are swallowed by the completion future, so log them.
			try {
				long transferredBytes = transfer.progress().snapshot().transferredBytes();
				if (exception == null) {
					reportTransferCompleted(transferredBytes);
				} else {
					reportTransferFailed(transferredBytes,
							exception instanceof CompletionException && exception.getCause() != null
									? exception.getCause()
									: exception);
				}
			} catch (RuntimeException e) {
				logger.error("S3 transfer [{}] - failed to report transfer completion / failure",
						transferSourceDestination, e);
			}
		});
	}

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
		reportTransferCompleted(context.progressSnapshot().transferredBytes());
	}

	@Override
	public void transferFailed(TransferListener.Context.TransferFailed context) {
		reportTransferFailed(context.progressSnapshot().transferredBytes(), context.exception());
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Report the transfer completion to the HPC progress listener, unless the
	 * transfer completion / failure was already reported.
	 *
	 * @param transferredBytes The bytes transferred.
	 */
	private void reportTransferCompleted(long transferredBytes) {
		if (!transferEnded.compareAndSet(false, true)) {
			return;
		}
		bytesTransferred.getAndSet(transferredBytes);

		logger.info("S3 transfer [{}] completed. {} bytes transferred", transferSourceDestination, bytesTransferred.get());
		progressListener.transferCompleted(bytesTransferred.get());
	}

	/**
	 * Report the transfer failure to the HPC progress listener, unless the
	 * transfer completion / failure was already reported.
	 *
	 * @param transferredBytes The bytes transferred.
	 * @param exception        The transfer failure.
	 */
	private void reportTransferFailed(long transferredBytes, Throwable exception) {
		if (!transferEnded.compareAndSet(false, true)) {
			return;
		}
		bytesTransferred.getAndSet(transferredBytes);

		logger.error("S3 transfer [{}] failed. {}MB transferred.", transferSourceDestination,
				bytesTransferred.get() / MB, exception);
		progressListener.transferFailed("S3 transfer failed:  " + exception.getMessage());
	}
}
