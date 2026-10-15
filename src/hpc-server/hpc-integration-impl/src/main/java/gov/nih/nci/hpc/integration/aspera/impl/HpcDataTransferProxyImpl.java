package gov.nih.nci.hpc.integration.aspera.impl;

import static gov.nih.nci.hpc.util.HpcUtil.exec;
import static gov.nih.nci.hpc.util.HpcUtil.toNormalizedPath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.apache.commons.io.FileUtils;
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

		String archiveLocationDirectoryPath = downloadRequest.getArchiveLocationFilePath().substring(0,
				downloadRequest.getArchiveLocationFilePath().lastIndexOf('/'));

		String transferDirectoryName = "aspera-" + UUID.randomUUID().toString();
		String transferDirectoryPath = archiveLocationDirectoryPath + "/" + transferDirectoryName;
		File transferDirectory = new File(transferDirectoryPath);
		String fileId = toNormalizedPath(asperaDestination.getDestinationLocation().getFileId());
		String transferFileName = fileId.substring(fileId.lastIndexOf('/') + 1);
		String transferFilePath = transferDirectoryPath + "/" + transferFileName;
		String asperaTarget = asperaDestination.getAccount().getUser() + "@" + asperaDestination.getAccount().getHost()
				+ ":" + asperaDestination.getDestinationLocation().getFileContainerId();

		// ascp would interpret an argument starting with '-' as an option.
		if (StringUtils.isEmpty(transferFileName) || transferFileName.startsWith("-") || asperaTarget.startsWith("-")) {
			throw new HpcException("[Aspera] Invalid destination: " + asperaTarget + "/" + transferFileName,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		logger.info("[Aspera] - staged transfer file: {}", transferFilePath);

		Map<String, String> environment = Map.of("ASPERA_SCP_PASS",
				Objects.toString(asperaDestination.getAccount().getPassword(), ""));
		CompletableFuture<Void> asperaDownloadFuture = CompletableFuture.runAsync(() -> {
			try {
				Files.createDirectory(transferDirectory.toPath());
				Files.createSymbolicLink(Paths.get(transferFilePath),
						Paths.get(downloadRequest.getArchiveLocationFilePath()));

				String ascpResponse = exec(Arrays.asList(ascp, "-i", privateKeyFile, "-Q", "-l", "1000m", "-k", "0",
						transferFileName, asperaTarget), null, environment, transferDirectory);

				logger.info("[Aspera] successfully completed download of {} to {}:{}/{}. ascp response: {}",
						downloadRequest.getPath(), asperaDestination.getAccount().getHost(),
						asperaDestination.getDestinationLocation().getFileContainerId(),
						asperaDestination.getDestinationLocation().getFileId(), ascpResponse);

				progressListener.transferCompleted(downloadRequest.getSize());

			} catch (HpcException | IOException e) {
				String message = "[Aspera] Failed to download object: " + e.getMessage();
				logger.error(message, HpcErrorType.DATA_TRANSFER_ERROR, e);
				progressListener.transferFailed(message);

			} finally {
				try {
					// The staged file is a symbolic link - deleting the directory doesn't
					// delete the archive file.
					FileUtils.deleteDirectory(transferDirectory);
					logger.info("[Aspera] - Deleted staged transfer directory: {}", transferDirectoryPath);

				} catch (IOException e) {
					logger.error("Failed to delete staged transfer directory: {}", transferDirectoryPath, e);
				}
			}

		}, asperaExecutor);

		return String.valueOf(asperaDownloadFuture.hashCode());
	}

}
