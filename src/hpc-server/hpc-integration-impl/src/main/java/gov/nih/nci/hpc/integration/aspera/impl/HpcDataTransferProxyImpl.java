package gov.nih.nci.hpc.integration.aspera.impl;

import static gov.nih.nci.hpc.util.HpcUtil.exec;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaDownloadDestination;
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

	// Aspera connect private key file.
	@Value("${hpc.integration.aspera.privateKeyFile}")
	private String privateKeyFile = null;

	// Aspera connect ascp command path.
	@Value("${hpc.integration.aspera.ascp}")
	private String ascp = null;

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
		if (StringUtils.isEmpty(downloadRequest.getArchiveLocationFilePath())) {
			throw new HpcException("[Aspera] No archive file set", HpcErrorType.UNEXPECTED_ERROR);
		}
		if (downloadRequest.getAsperaDestination() == null) {
			throw new HpcException("[Aspera] Null destination", HpcErrorType.UNEXPECTED_ERROR);
		}

		// Upload the file to Aspera.
		HpcAsperaDownloadDestination asperaDestination = downloadRequest.getAsperaDestination();
		File archiveLocationDirectory = new File(downloadRequest.getArchiveLocationFilePath().substring(0,
				downloadRequest.getArchiveLocationFilePath().lastIndexOf('/')));
		String[] envp = new String[] { "ASPERA_SCP_PASS=" + asperaDestination.getAccount().getPassword() };

		CompletableFuture<Void> asperaDownloadFuture = CompletableFuture.runAsync(() -> {
			try {

				exec("ln -s " + downloadRequest.getArchiveLocationFilePath() + " "
						+ asperaDestination.getDestinationLocation().getFileId(), null, envp, archiveLocationDirectory);

				exec(ascp + " -i " + privateKeyFile + " -Q -l 1000m -k 1 "
						+ asperaDestination.getDestinationLocation().getFileId() + " "
						+ asperaDestination.getAccount().getUser() + "@" + asperaDestination.getAccount().getHost()
						+ ":" + asperaDestination.getDestinationLocation().getFileContainerId(), null, envp,
						archiveLocationDirectory);

				progressListener.transferCompleted(downloadRequest.getSize());

			} catch (HpcException e) {
				String message = "[Aspera] Failed to download object: " + e.getMessage();
				logger.error(message, HpcErrorType.DATA_TRANSFER_ERROR, e);
				progressListener.transferFailed(message);

			} finally {
				try {
					exec("rm -f " + downloadRequest.getArchiveLocationFilePath(), null, envp, archiveLocationDirectory);

				} catch (HpcException e) {
					logger.error("Failed to delete sym link for Aspera download", e);
				}
			}

		}, asperaExecutor);

		return String.valueOf(asperaDownloadFuture.hashCode());
	}

}
