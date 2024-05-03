/**
 * HpcBoxProgressListener.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration.box.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.box.sdk.ProgressListener;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;

/**
 * HPC Box Progress Listener.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcBoxProgressListener implements ProgressListener {
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
	private long bytesTransferredReported = 0;

	// Transfer source and/or destination (for logging purposed)
	private String transferSourceDestination = null;
	
	// Transfer size (for logging purposes)
	private long size = 0;

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
	 *                                  @param size The file size
	 * @throws HpcException if no progress listener provided.
	 */
	public HpcBoxProgressListener(HpcDataTransferProgressListener progressListener, String transferSourceDestination, long size)
			throws HpcException {
		if (progressListener == null) {
			throw new HpcException("Null progress listener", HpcErrorType.UNEXPECTED_ERROR);
		}
		this.progressListener = progressListener;
		this.transferSourceDestination = transferSourceDestination;
		this.size = size / MB;
	}

	/**
	 * Default Constructor.
	 *
	 * @throws HpcException Constructor is disabled.
	 */
	@SuppressWarnings("unused")
	private HpcBoxProgressListener() throws HpcException {
		throw new HpcException("Constructor Disabled", HpcErrorType.UNEXPECTED_ERROR);
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// ProgressListener Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void onProgressChanged(long bytesTransferred, long totalBytes) {
		if (bytesTransferred > 0) {
			if (bytesTransferredReported == 0) {
				bytesTransferredReported = bytesTransferred;
				logger.info("Box transfer [{}] started. {} bytes transferred so far ", transferSourceDestination,
						bytesTransferredReported);
			} else if (bytesTransferred - bytesTransferredReported >= TRANSFER_PROGRESS_REPORTING_RATE) {
				bytesTransferredReported = bytesTransferred;
				logger.info("Box transfer [{}] in progress. {}MB / {}MB transferred so far", transferSourceDestination,
						bytesTransferredReported / MB, size);

				progressListener.transferProgressed(bytesTransferredReported);
			}
		}
	}
}
