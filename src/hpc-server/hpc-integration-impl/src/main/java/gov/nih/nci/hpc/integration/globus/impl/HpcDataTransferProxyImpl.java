package gov.nih.nci.hpc.integration.globus.impl;

import static gov.nih.nci.hpc.integration.HpcDataTransferProxy.getArchiveDestinationLocation;
import static gov.nih.nci.hpc.util.HpcUtil.exec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.globusonline.transfer.APIError;
import org.globusonline.transfer.BaseTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient.Result;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadMethod;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusTransferItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusTransferRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcSetArchiveObjectMetadataResponse;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.model.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.integration.HpcTransferAcceptanceResponse;
import gov.nih.nci.hpc.util.HpcUtil;

/**
 * HPC Data Transfer Proxy Globus Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataTransferProxyImpl implements HpcDataTransferProxy {

	private class HpcGlobusTransferAcceptanceResponse implements HpcTransferAcceptanceResponse {
		private boolean acceptTransferFlag;
		private int queueLength;

		HpcGlobusTransferAcceptanceResponse(boolean canAccept, int sizeOfQueue) {
			this.acceptTransferFlag = canAccept;
			this.queueLength = sizeOfQueue;
		}

		@Override
		public boolean canAcceptTransfer() {
			return acceptTransferFlag;
		}

		@Override
		public int getQueueSize() {
			return queueLength;
		}
	}

	class HpcGlobusTransferStatusTimestamp {
		String niceStatus = null;
		Date timestamp;
	}

	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// Globus transfer status strings.
	private static final String FAILED_STATUS = "FAILED";
	private static final String INACTIVE_STATUS = "INACTIVE";
	private static final String SUCCEEDED_STATUS = "SUCCEEDED";
	private static final String PERMISSION_DENIED_STATUS = "PERMISSION_DENIED";
	private static final String OK_STATUS = "OK";
	private static final String NOT_DIRECTORY_GLOBUS_CODE = "ExternalError.DirListingFailed.NotDirectory";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Globus connection instance.
	@Autowired
	private HpcGlobusConnection globusConnection = null;

	// The Globus directory browser instance.
	@Autowired
	private HpcGlobusDirectoryBrowser globusDirectoryBrowser = null;

	// Retry template. Used to automatically retry Globus service calls.
	@Autowired
	private RetryTemplate retryTemplate = null;

	// The Globus active tasks queue size.
	@Value("${hpc.integration.globus.queueSize}")
	private int globusQueueSize = 0;

	// The list of Globus transfer nice_statuses (comma separated) to exclude from a
	// deemed 'transfer failure'.
	@Value("${hpc.integration.globus.excludeFromTransferFailureStatuses}")
	private String excludeFromTransferFailureStatuses = null;

	// The list of Globus transfer nice_statuses (comma separated) that are
	// considered 'recoverable failures
	@Value("${hpc.integration.globus.recoverableTransferFailureStatuses}")
	private String recoverableFromTransferFailureStatuses = null;

	// The time in minutes allowed for a 'recoverable failure' to recover.
	@Value("${hpc.integration.globus.recoverableFailureTimeout}")
	private int recoverableFailureTimeout = 0;

	// A map that keeps track of transfer tasks that are in 'recoverable failure'
	// status.
	Map<String, HpcGlobusTransferStatusTimestamp> recoverableFailureTasks = new HashMap<>();

	// The Logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcDataTransferProxyImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataTransferProxy Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount, String url, String encryptionAlgorithm,
			String encryptionKey) throws HpcException {
		return globusConnection.authenticate(url, dataTransferAccount);
	}

	@Override
	public HpcTransferAcceptanceResponse acceptsTransferRequests(Object authenticatedToken) throws HpcException {

		logger.info("acceptsTransferRequests: entered with received authenticatedToken parameter = {}",
				authenticatedToken);

		JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);
		return retryTemplate.execute(arg0 -> {
			try {
				JSONObject jsonTasksLists = client.getResult("/task_list?filter=status:ACTIVE,INACTIVE").document;
				logger.info(
						"acceptsTransferRequests: Made request to Globus for transfer tasks, resulting JSON is \n[\n{}\n]\n",
						jsonTasksLists);
				final int qSize = jsonTasksLists.getInt("total");
				final boolean underCap = qSize < globusQueueSize;
				logger.info(String.format(
						"acceptsTransferRequests: from JSON response, determined that qSize = %s and underCap = %s",
						Integer.toString(qSize), Boolean.toString(underCap)));
				final HpcTransferAcceptanceResponse transferAcceptanceResponse = new HpcGlobusTransferAcceptanceResponse(
						underCap, qSize);
				logger.info("acceptsTransferRequests: About to return");
				return transferAcceptanceResponse;
			} catch (Exception e) {
				logger.error("acceptsTransferRequests: About to throw exception", e);
				throw new HpcException("[GLOBUS] Failed to determine active tasks count",
						HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
			}
		});
	}

	@Override
	public HpcDataObjectUploadResponse uploadDataObject(Object authenticatedToken,
			HpcDataObjectUploadRequest uploadRequest, HpcArchive baseArchiveDestination,
			Integer uploadRequestURLExpiration, HpcDataTransferProgressListener progressListener,
			List<HpcMetadataEntry> metadataEntries, Boolean encryptedTransfer, String storageClass)
			throws HpcException {
		// Progress listener not supported.
		if (progressListener != null) {
			throw new HpcException("Globus data transfer doesn't support progress listener",
					HpcErrorType.UNEXPECTED_ERROR);
		}

		if (uploadRequest.getS3UploadSource() != null) {
			throw new HpcException("Invalid upload source", HpcErrorType.UNEXPECTED_ERROR);
		}

		// Generating upload URL or direct file upload not supported.
		if (uploadRequest.getGenerateUploadRequestURL()) {
			throw new HpcException("Globus data transfer doesn't support upload URL", HpcErrorType.UNEXPECTED_ERROR);
		}

		// Calculate the archive destination.
		HpcFileLocation archiveDestinationLocation = getArchiveDestinationLocation(
				baseArchiveDestination.getFileLocation(), uploadRequest.getPath(), uploadRequest.getCallerObjectId(),
				baseArchiveDestination.getType(), this, authenticatedToken);

		if (uploadRequest.getSourceFile() != null) {
			// This is a synchronous upload request. Simply store the data to the
			// file-system.
			// No Globus action is required here.
			return saveFile(uploadRequest.getSourceFile(), archiveDestinationLocation, baseArchiveDestination,
					uploadRequest.getSudoPassword(), uploadRequest.getSystemAccountName());
		}

		// Build a Globus transfer request.
		HpcGlobusTransferItem transferItem = new HpcGlobusTransferItem();
		transferItem.setSourcePath(uploadRequest.getGlobusUploadSource().getSourceLocation().getFileId());
		transferItem.setDestinationPath(archiveDestinationLocation.getFileId());
		HpcGlobusTransferRequest transferRequest = new HpcGlobusTransferRequest();
		transferRequest
				.setSourceEndpoint(uploadRequest.getGlobusUploadSource().getSourceLocation().getFileContainerId());
		transferRequest.setDestinationEndpoint(archiveDestinationLocation.getFileContainerId());
		transferRequest.getItems().add(transferItem);

		// Submit a request to Globus to transfer the data.
		String requestId = transferData(globusConnection.getTransferClient(authenticatedToken), transferRequest,
				encryptedTransfer);

		// Package and return the response.
		HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
		uploadResponse.setArchiveLocation(archiveDestinationLocation);
		uploadResponse.setDataTransferRequestId(requestId);
		uploadResponse.setDataTransferType(HpcDataTransferType.GLOBUS);
		uploadResponse.setDataTransferStarted(Calendar.getInstance());
		uploadResponse.setDataTransferCompleted(null);
		uploadResponse.setUploadSource(uploadRequest.getGlobusUploadSource().getSourceLocation());
		uploadResponse.setSourceSize(uploadRequest.getSourceSize());
		uploadResponse.setDataTransferMethod(HpcDataTransferUploadMethod.GLOBUS);
		if (baseArchiveDestination.getType().equals(HpcArchiveType.TEMPORARY_ARCHIVE)) {
			uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.IN_PROGRESS_TO_TEMPORARY_ARCHIVE);
		} else {
			uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.IN_PROGRESS_TO_ARCHIVE);
		}
		return uploadResponse;
	}

	@Override
	public String downloadDataObject(Object authenticatedToken, HpcDataObjectDownloadRequest downloadRequest,
			HpcArchive baseArchiveDestination, HpcDataTransferProgressListener progressListener,
			Boolean encryptedTransfer) throws HpcException {
		// Progress listener not supported.
		if (progressListener != null) {
			throw new HpcException("Globus data transfer doesn't support progress listener",
					HpcErrorType.UNEXPECTED_ERROR);
		}

		if (downloadRequest.getFileDestination() != null) {
			// This is a synchronous download request.
			String archiveFilePath = downloadRequest.getArchiveLocation().getFileId().replaceFirst(
					baseArchiveDestination.getFileLocation().getFileId(), baseArchiveDestination.getDirectory());

			try {
				exec("cp " + archiveFilePath + " " + downloadRequest.getFileDestination(),
						downloadRequest.getSudoPassword(), null, null);

			} catch (HpcException e) {
				throw new HpcException(
						"Failed to copy file from POSIX archive: " + archiveFilePath + "[" + e.getMessage() + "]",
						HpcErrorType.DATA_TRANSFER_ERROR, e);
			}

			return String.valueOf(downloadRequest.getFileDestination().hashCode());

		} else {
			// This is an asynchrnous download request to be performed by Globus

			// Build a Globus transfer request.
			HpcGlobusTransferItem transferItem = new HpcGlobusTransferItem();
			transferItem.setSourcePath(downloadRequest.getArchiveLocation().getFileId());
			transferItem
					.setDestinationPath(downloadRequest.getGlobusDestination().getDestinationLocation().getFileId());
			HpcGlobusTransferRequest transferRequest = new HpcGlobusTransferRequest();
			transferRequest.setSourceEndpoint(downloadRequest.getArchiveLocation().getFileContainerId());
			transferRequest.setDestinationEndpoint(
					downloadRequest.getGlobusDestination().getDestinationLocation().getFileContainerId());
			transferRequest.getItems().add(transferItem);

			// Submit the transfer request
			return transferData(globusConnection.getTransferClient(authenticatedToken), transferRequest,
					encryptedTransfer);
		}
	}

	@Override
	public String generateDownloadRequestURL(Object authenticatedToken, HpcFileLocation archiveLocation,
			HpcArchive baseArchiveDestination, Integer downloadRequestURLExpiration) throws HpcException {
		try {
			return Paths
					.get(archiveLocation.getFileId().replaceFirst(baseArchiveDestination.getFileLocation().getFileId(),
							baseArchiveDestination.getDirectory()))
					.toUri().toURL().toString();

		} catch (IOException e) {
			throw new HpcException("Failed to generate download URL", HpcErrorType.UNEXPECTED_ERROR, e);
		}
	}

	@Override
	public HpcSetArchiveObjectMetadataResponse setDataObjectMetadata(Object authenticatedToken,
			HpcFileLocation fileLocation, HpcArchive baseArchiveDestination, List<HpcMetadataEntry> metadataEntries,
			String sudoPassword, String storageClass) throws HpcException {
		String archiveFilePath = fileLocation.getFileId().replaceFirst(
				baseArchiveDestination.getFileLocation().getFileId(), baseArchiveDestination.getDirectory());

		HpcSetArchiveObjectMetadataResponse response = new HpcSetArchiveObjectMetadataResponse();
		response.setChecksum(exec("md5sum " + archiveFilePath, sudoPassword, null, null).split("\\s+")[0]);

		File metadataFile = getMetadataFile(archiveFilePath);
		if (metadataFile.exists()) {
			logger.info("System metadata in POSIX archive already set for [{}]. No need to re-create in archive",
					fileLocation.getFileId());
			response.setMetadataAdded(false);
			return response;
		}

		try {
			// Creating the metadata file.
			if (!metadataEntries.isEmpty()) {
				List<String> metadata = new ArrayList<>();
				metadataEntries.forEach(
						metadataEntry -> metadata.add(metadataEntry.getAttribute() + "=" + metadataEntry.getValue()));
				FileUtils.writeLines(metadataFile, metadata);
			}

			response.setMetadataAdded(true);
			return response;

		} catch (IOException e) {
			throw new HpcException("Failed to set POSIX archive metadata", HpcErrorType.UNEXPECTED_ERROR, e);
		}
	}

	@Override
	public void deleteDataObject(Object authenticatedToken, HpcFileLocation fileLocation,
			HpcArchive baseArchiveDestination, String sudoPassword) throws HpcException {
		String archiveFilePath = fileLocation.getFileId().replaceFirst(
				baseArchiveDestination.getFileLocation().getFileId(), baseArchiveDestination.getDirectory());
		// Delete the archive file.
		try {
			exec("rm " + archiveFilePath, sudoPassword, null, null);

		} catch (HpcException e) {
			logger.error("Failed to delete file: {}", archiveFilePath, e);
		}
		// Delete the metadata file.
		try {
			exec("rm " + getMetadataFile(archiveFilePath).getAbsolutePath(), sudoPassword, null, null);

		} catch (HpcException e) {
			logger.error("Failed to delete metadata for file: {}", archiveFilePath, e);
		}
	}

	@Override
	public HpcDataTransferUploadReport getDataTransferUploadStatus(Object authenticatedToken,
			String dataTransferRequestId, HpcArchive baseArchiveDestination, String loggingPrefix) throws HpcException {
		HpcGlobusDataTransferReport report = getDataTransferReport(authenticatedToken, dataTransferRequestId,
				loggingPrefix);

		HpcDataTransferUploadReport statusReport = new HpcDataTransferUploadReport();
		statusReport.setMessage(report.niceStatusDescription);
		statusReport.setBytesTransferred(report.bytesTransferred);

		if (report.status.equals(SUCCEEDED_STATUS)) {
			// Upload completed successfully.

			// Clear this task from the recoverable failure list in case it's there.
			recoverableFailureTasks.remove(dataTransferRequestId);

			// Return status based on the archive type.
			if (baseArchiveDestination.getType().equals(HpcArchiveType.TEMPORARY_ARCHIVE)) {
				statusReport.setStatus(HpcDataTransferUploadStatus.IN_TEMPORARY_ARCHIVE);
			} else {
				statusReport.setStatus(HpcDataTransferUploadStatus.ARCHIVED);
			}

		} else if (transferFailed(authenticatedToken, dataTransferRequestId, report, loggingPrefix)) {
			// Upload failed.
			statusReport.setStatus(HpcDataTransferUploadStatus.FAILED);
			statusReport.setMessage(report.niceStatusDescription + ". " + report.errorMessage);

		} else if (baseArchiveDestination.getType().equals(HpcArchiveType.TEMPORARY_ARCHIVE)) {
			// Upload is in progress. Return status based on the archive type.
			statusReport.setStatus(HpcDataTransferUploadStatus.IN_PROGRESS_TO_TEMPORARY_ARCHIVE);

			// Clear tracking of recoverable failure if needed.
			transferRecovered(dataTransferRequestId, report, loggingPrefix);

		} else {
			statusReport.setStatus(HpcDataTransferUploadStatus.IN_PROGRESS_TO_ARCHIVE);

			// Clear tracking of recoverable failure if needed.
			transferRecovered(dataTransferRequestId, report, loggingPrefix);
		}

		return statusReport;
	}

	@Override
	public HpcDataTransferDownloadReport getDataTransferDownloadStatus(Object authenticatedToken,
			String dataTransferRequestId, boolean successfulItems, String loggingPrefix) throws HpcException {
		HpcGlobusDataTransferReport report = getDataTransferReport(authenticatedToken, dataTransferRequestId,
				loggingPrefix);

		HpcDataTransferDownloadReport statusReport = new HpcDataTransferDownloadReport();
		statusReport.setMessage(report.niceStatusDescription);
		statusReport.setBytesTransferred(report.bytesTransferred);

		if (report.status.equals(SUCCEEDED_STATUS)) {
			// Download completed successfully.

			// Clear this task from the recoverable failure list in case it's there.
			recoverableFailureTasks.remove(dataTransferRequestId);

			statusReport.setStatus(HpcDataTransferDownloadStatus.COMPLETED);
			if (successfulItems) {
				statusReport.getSuccessfulItems()
						.addAll(getSuccessfulTransfers(authenticatedToken, dataTransferRequestId, null));
			}

		} else if (transferFailed(authenticatedToken, dataTransferRequestId, report, loggingPrefix)) {
			// Download failed.
			statusReport.setStatus(HpcDataTransferDownloadStatus.FAILED);
			if (successfulItems) {
				statusReport.getSuccessfulItems()
						.addAll(getSuccessfulTransfers(authenticatedToken, dataTransferRequestId, null));
			}
			if (report.niceStatus.equals(PERMISSION_DENIED_STATUS)) {
				statusReport.setPermissionDenied(true);
				statusReport.setMessage(report.niceStatusDescription
						+ ". Change the destination endpoint permissions to give write access to the system Globus Group");
			} else {
				statusReport.setMessage(report.niceStatusDescription + ". " + report.errorMessage);
			}

		} else {
			// Download still in progress.
			statusReport.setStatus(HpcDataTransferDownloadStatus.IN_PROGRESS);

			// Clear tracking of recoverable failure if needed.
			transferRecovered(dataTransferRequestId, report, loggingPrefix);
		}

		return statusReport;
	}

	@Override
	public HpcPathAttributes getPathAttributes(Object authenticatedToken, HpcFileLocation fileLocation, boolean getSize)
			throws HpcException {
		if (authenticatedToken != null) {
			// Use Globus to get path attributes.
			JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);
			autoActivate(fileLocation.getFileContainerId(), client);
			return getPathAttributes(fileLocation, client, getSize);
		} else {
			// Get POSIX path attributes
			return HpcUtil.getPathAttributes(fileLocation);
		}
	}

	@Override
	public List<HpcDirectoryScanItem> scanDirectory(Object authenticatedToken, HpcFileLocation directoryLocation)
			throws HpcException {
		JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);
		autoActivate(directoryLocation.getFileContainerId(), client);

		// Invoke the Globus directory scan service.
		HpcGlobusDirectoryScanFileVisitor directoryScanFileVisitor = new HpcGlobusDirectoryScanFileVisitor();
		try {
			globusDirectoryBrowser.scan(globusDirectoryBrowser.list(directoryLocation, client), client,
					directoryScanFileVisitor);
			return directoryScanFileVisitor.getScanItems();

		} catch (Exception e) {
			throw new HpcException("[GLOBUS] Failed to scan a directory: " + directoryLocation,
					HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
		}
	}

	@Override
	public String getFileContainerName(Object authenticatedToken, String fileContainerId) throws HpcException {
		JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);

		return retryTemplate.execute(arg0 -> {
			try {
				JSONObject jsonEndpoint = client.getResult("/endpoint/" + fileContainerId).document;
				return jsonEndpoint.getString("display_name");

			} catch (Exception e) {
				throw new HpcException("[GLOBUS] Failed to get endpoint display name", HpcErrorType.DATA_TRANSFER_ERROR,
						HpcIntegratedSystem.GLOBUS, e);
			}
		});
	}

	@Override
	public void cancelTransferRequest(Object authenticatedToken, String dataTransferRequestId, String message)
			throws HpcException {
		JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);

		retryTemplate.execute(arg0 -> {
			try {
				JSONObject cancel = new JSONObject();
				cancel.put("task_id_list", Arrays.asList(dataTransferRequestId));
				cancel.put("message", message);
				client.postResult("/endpoint_manager/admin_cancel", cancel, null);
				return null;

			} catch (Exception e) {
				throw new HpcException("[GLOBUS] Failed to cancel transfer task: " + dataTransferRequestId,
						HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
			}
		});
	}

	@Override
	public String transferData(Object authenticatedToken, HpcGlobusTransferRequest transferRequest,
			Boolean encryptedTransfer) throws HpcException {
		JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);

		// Activate endpoints.
		autoActivate(transferRequest.getSourceEndpoint(), client);
		autoActivate(transferRequest.getDestinationEndpoint(), client);

		// Submit transfer request.
		return retryTemplate.execute(arg0 -> {
			try {
				JSONTransferAPIClient.Result result = client.getResult("/transfer/submission_id");
				String submissionId = result.document.getString("value");
				JSONObject transfer = new JSONObject();
				transfer.put("DATA_TYPE", "transfer");
				transfer.put("submission_id", submissionId);
				transfer.put("verify_checksum", true);
				transfer.put("delete_destination_extra", false);
				transfer.put("preserve_timestamp", false);
				transfer.put("encrypt_data", Optional.ofNullable(encryptedTransfer).orElse(false));
				transfer.put("source_endpoint", transferRequest.getSourceEndpoint());
				transfer.put("destination_endpoint", transferRequest.getDestinationEndpoint());
				transfer.put("DATA", toTransferItemsJson(transferRequest.getItems()));

				result = client.postResult("/transfer", transfer, null);
				return result.document.getString("task_id");

			} catch (APIError error) {
				logger.error("Error while submitting transfer request to Globus for" + " Source "
						+ transferRequest.getSourceEndpoint() + " and Destination "
						+ transferRequest.getDestinationEndpoint() + ": [category=" + error.category + ", resoutce="
						+ error.resource + ", code=" + error.code + ", requestId=" + error.requestId + ", status="
						+ error.statusCode + " " + error.statusMessage + "] - " + error.message, error);
				throw new HpcException(
						"[GLOBUS] Failed to transfer: " + error.message + ". Source: "
								+ transferRequest.getSourceEndpoint() + ". Destination: "
								+ transferRequest.getDestinationEndpoint(),
						HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, error);

			} catch (Exception e) {
				logger.error("Failed to submit transfer request to Globus for" + " Source "
						+ transferRequest.getSourceEndpoint() + " and Destination "
						+ transferRequest.getDestinationEndpoint() + ": " + e.getMessage(), e);
				throw new HpcException(
						"[GLOBUS] Failed to transfer: " + e.getMessage() + ". Source: "
								+ transferRequest.getSourceEndpoint() + ". Destination: "
								+ transferRequest.getDestinationEndpoint(),
						HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
			}
		});
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Create a 'transfer item' JSON
	 *
	 * @param items List of transfer items
	 * @return Transfer items JSON.
	 * @throws HpcException on data transfer system failure.
	 */
	private JSONArray toTransferItemsJson(List<HpcGlobusTransferItem> items) throws HpcException {
		JSONArray itemsJson = new JSONArray();
		for (HpcGlobusTransferItem item : items) {
			JSONObject itemJson = new JSONObject();
			try {
				itemJson.put("DATA_TYPE", "transfer_item");
				itemJson.put("source_path", item.getSourcePath());
				itemJson.put("destination_path", item.getDestinationPath());
				itemJson.put("recursive", false);
				itemsJson.put(itemJson);

			} catch (JSONException e) {
				throw new HpcException("[GLOBUS] Failed to create transter item JSON: " + item.getSourcePath() + " -> "
						+ item.getDestinationPath(), HpcErrorType.DATA_TRANSFER_ERROR, e);
			}
		}

		return itemsJson;
	}

	private void autoActivate(String endpointName, JSONTransferAPIClient client) throws HpcException {
		retryTemplate.execute(arg0 -> {
			try {
				String resource = BaseTransferAPIClient.endpointPath(endpointName) + "/autoactivate?if_expires_in=100";
				client.postResult(resource, null, null);
				return null;

			} catch (APIError error) {
				HpcIntegratedSystem integratedSystem = error.statusCode >= 500 ? HpcIntegratedSystem.GLOBUS : null;
				String message = "";
				switch (error.statusCode) {
				case 404:
					message = "[GLOBUS] Endpoint doesn't exist or is inactive. Make sure the endpoint UUID "
							+ "is correct and active: " + endpointName;
					break;

				case 403:
					message = "[GLOBUS] Endpoint permission denied: " + endpointName;
					break;

				case 503:
					message = "[GLOBUS] Service is down for maintenance";
					break;

				default:
					message = "[GLOBUS] Failed to activate endpoint: " + endpointName;
					break;
				}

				throw new HpcException(message, HpcErrorType.DATA_TRANSFER_ERROR, integratedSystem, error);

			} catch (Exception e) {
				throw new HpcException("[GLOBUS] Failed to activate endpoint: " + endpointName,
						HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
			}
		});
	}

	private class HpcGlobusDataTransferReport {
		private String status = null;
		private String niceStatus = null;
		private long bytesTransferred = 0;
		private String niceStatusDescription = null;
		private String errorMessage = null;
	}

	/**
	 * Get a data transfer report.
	 *
	 * @param authenticatedToken    An authenticated token.
	 * @param dataTransferRequestId The data transfer request ID.
	 * @param loggingPrefix         Contextual (download/upload tasks etc) logging
	 *                              text to prefix in logging.
	 * @return The data transfer report for the request.
	 * @throws HpcException on data transfer system failure.
	 */
	private HpcGlobusDataTransferReport getDataTransferReport(Object authenticatedToken, String dataTransferRequestId,
			String loggingPrefix) throws HpcException {
		JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);

		return retryTemplate.execute(arg0 -> {

			HpcGlobusDataTransferReport report = new HpcGlobusDataTransferReport();
			try {
				JSONObject jsonReport = client.getResult("/endpoint_manager/task/" + dataTransferRequestId).document;

				report.status = jsonReport.getString("status");
				report.niceStatus = jsonReport.getString("nice_status");
				report.bytesTransferred = jsonReport.getLong("bytes_transferred");
				report.niceStatusDescription = jsonReport.getString("nice_status_short_description");

			} catch (Exception e) {
				throw new HpcException(
						loggingPrefix + "[GLOBUS] Failed to get task report for task: " + dataTransferRequestId,
						HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
			}

			if (FAILED_STATUS.equals(report.status)) {
				// Transfer failed. Get the error message from the latest failed event
				try {
					JSONObject jsonEventList = client.getResult("/endpoint_manager/task/" + dataTransferRequestId
							+ "/event_list?filter_is_error=1&limit=1").document;

					JSONArray jsonEventArray = jsonEventList.getJSONArray("DATA");
					if (jsonEventArray != null && jsonEventArray.length() == 1) {
						JSONObject jsonErrorEvent = jsonEventArray.getJSONObject(0);
						String description = jsonErrorEvent.getString("description");
						String details = jsonErrorEvent.getString("details");
						if (!StringUtils.isEmpty(description) || !StringUtils.isEmpty(details)) {
							report.errorMessage = description + ": " + details;
						}
					}

					if (report.errorMessage == null) {
						logger.error(loggingPrefix
								+ "[GLOBUS] Failed to get list of events for failed task [{}]. Globus API response: {}",
								dataTransferRequestId, jsonEventList.toString());
					}

				} catch (Exception e) {
					logger.error(loggingPrefix + "[GLOBUS] Failed to get list of events for failed task [{}] - {}",
							dataTransferRequestId, e.getMessage(), e);
				}
			}

			logger.info(loggingPrefix
					+ "[GLOBUS] transfer report. globus-task-id: {}, status: {}, niceStatus: {}, niceStatusDescription: {}, bytesTransferred: {}, errorMessage: {}",
					dataTransferRequestId, report.status, report.niceStatus, report.niceStatusDescription,
					report.bytesTransferred, report.errorMessage);

			return report;
		});
	}

	/**
	 * Get a data transfer report.
	 *
	 * @param authenticatedToken    An authenticated token.
	 * @param dataTransferRequestId The data transfer request ID.
	 * @return The data transfer report for the request.
	 * @throws HpcException on data transfer system failure.
	 */
	private List<HpcGlobusTransferItem> getSuccessfulTransfers(Object authenticatedToken, String dataTransferRequestId,
			Integer nextMarker) throws HpcException {
		JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);
		List<HpcGlobusTransferItem> items = new ArrayList<>();

		return retryTemplate.execute(arg0 -> {
			try {
				JSONObject jsonSuccessfulTransfers = client.getResult("/endpoint_manager/task/" + dataTransferRequestId
						+ "/successful_transfers" + (nextMarker != null ? "?marker=" + nextMarker : "")).document;

				JSONArray jsonItems = jsonSuccessfulTransfers.getJSONArray("DATA");
				if (jsonItems != null) {
					// Iterate through the directory files, and locate the file we look for.
					int itemsNum = jsonItems.length();
					for (int i = 0; i < itemsNum; i++) {
						JSONObject jsonItem = jsonItems.getJSONObject(i);
						HpcGlobusTransferItem item = new HpcGlobusTransferItem();
						item.setSourcePath(jsonItem.getString("source_path"));
						item.setDestinationPath(jsonItem.getString("destination_path"));
						items.add(item);
					}
				}

				// Check if there additional results to page.
				if (!jsonSuccessfulTransfers.isNull("next_marker")) {
					items.addAll(getSuccessfulTransfers(authenticatedToken, dataTransferRequestId,
							jsonSuccessfulTransfers.getInt("next_marker")));
				}

				return items;

			} catch (Exception e) {
				throw new HpcException("[GLOBUS] Failed to get task successful transfers: " + dataTransferRequestId,
						HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
			}
		});
	}

	/**
	 * Get attributes of a file/directory.
	 *
	 * @param fileLocation The endpoint/path to check.
	 * @param client       Globus client API instance.
	 * @param getSize      If set to true, the file/directory size will be returned.
	 * @return The path attributes.
	 * @throws HpcException on data transfer system failure.
	 */
	private HpcPathAttributes getPathAttributes(HpcFileLocation fileLocation, JSONTransferAPIClient client,
			boolean getSize) throws HpcException {
		HpcPathAttributes pathAttributes = new HpcPathAttributes();
		pathAttributes.setExists(false);
		pathAttributes.setIsDirectory(false);
		pathAttributes.setIsFile(false);
		pathAttributes.setSize(0);
		pathAttributes.setIsAccessible(true);

		// Invoke the Globus directory listing service.
		try {
			Result dirContent = globusDirectoryBrowser.list(fileLocation, client);
			pathAttributes.setExists(true);
			pathAttributes.setIsDirectory(true);
			pathAttributes.setSize(getSize ? getDirectorySize(dirContent, client) : -1);

		} catch (APIError error) {
			if (error.statusCode == 502) {
				if (error.code.equals(NOT_DIRECTORY_GLOBUS_CODE)) {
					// Path exists as a single file
					pathAttributes.setExists(true);
					pathAttributes.setIsFile(true);
					pathAttributes.setSize(getSize ? getFileSize(fileLocation, client) : -1);
				} else {
					throw new HpcException("Error at Globus endpoint " + fileLocation.getFileContainerId()
							+ ", file location: " + fileLocation.getFileId() + ": [category=" + error.category
							+ ", resoutce=" + error.resource + ", code=" + error.code + ", requestId=" + error.requestId
							+ ", status=" + error.statusCode + " " + error.statusMessage + "] - " + error.message,
							HpcErrorType.DATA_TRANSFER_ERROR, error);

				}
			} else if (error.statusCode == 403) {
				// Permission denied.
				pathAttributes.setExists(true);
				pathAttributes.setIsAccessible(false);
			}
			// else path was not found.

		} catch (Exception e) {
			throw new HpcException("[GLOBUS] Failed to get path attributes: " + fileLocation,
					HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
		}

		return pathAttributes;
	}

	/**
	 * Get file size.
	 *
	 * @param fileLocation The file endpoint/path.
	 * @param client       Globus client API instance.
	 * @return The file size in bytes.
	 */
	private long getFileSize(HpcFileLocation fileLocation, JSONTransferAPIClient client) {
		// Get the directory location of the file.
		HpcFileLocation dirLocation = new HpcFileLocation();
		dirLocation.setFileContainerId(fileLocation.getFileContainerId());
		int fileNameIndex = fileLocation.getFileId().lastIndexOf('/');
		if (fileNameIndex != -1) {
			dirLocation.setFileId(fileLocation.getFileId().substring(0, fileNameIndex));
		} else {
			dirLocation.setFileId(fileLocation.getFileId());
		}

		// Extract the file name from the path.
		String fileName = fileLocation.getFileId().substring(fileNameIndex + 1);

		// List the directory content.
		try {
			Result dirContent = globusDirectoryBrowser.list(dirLocation, client);
			JSONArray jsonFiles = dirContent.document.getJSONArray("DATA");
			if (jsonFiles != null) {
				// Iterate through the directory files, and locate the file we look for.
				int filesNum = jsonFiles.length();
				for (int i = 0; i < filesNum; i++) {
					JSONObject jsonFile = jsonFiles.getJSONObject(i);
					String jsonFileName = jsonFile.getString("name");
					if (jsonFileName != null && jsonFileName.equals(fileName)) {
						// The file was found. Return its size
						return jsonFile.getLong("size");
					}
				}
			}

		} catch (Exception e) {
			// Unexpected error. Eat this.
			logger.error("Failed to calculate file size", e);
		}

		// File not found, or exception was caught.
		return 0;
	}

	/**
	 * Get directory size. Sums up the size of all the files in this directory
	 * recursively.
	 *
	 * @param dirContent The directory content.
	 * @param client     Globus client API instance.
	 * @return The directory size in bytes.
	 */
	private long getDirectorySize(Result dirContent, JSONTransferAPIClient client) {
		HpcGlobusDirectorySizeFileVisitor fileSizeVisitor = new HpcGlobusDirectorySizeFileVisitor();
		try {
			globusDirectoryBrowser.scan(dirContent, client, fileSizeVisitor);
			return fileSizeVisitor.getSize();

		} catch (Exception e) {
			// Unexpected error. Eat this.
			logger.error("Failed to calculate directory size", e);
		}

		// Directory not found, or exception was caught.
		return 0;
	}

	/**
	 * Check if a Globus transfer request failed. It is also canceling the request
	 * if needed.
	 *
	 * @param authenticatedToken    An authenticated token.
	 * @param dataTransferRequestId The globus task ID.
	 * @param report                The Globus transfer report.
	 * @param loggingPrefix         Contextual (download/upload tasks etc) logging
	 *                              text to prefix in logging.
	 * @return True if the transfer failed, or false otherwise
	 */
	private boolean transferFailed(Object authenticatedToken, String dataTransferRequestId,
			HpcGlobusDataTransferReport report, String loggingPrefix) {
		if (report.status.equals(FAILED_STATUS)) {
			recoverableFailureTasks.remove(dataTransferRequestId);
			return true;
		}

		if (report.status.equals(INACTIVE_STATUS) || (!StringUtils.isEmpty(report.niceStatus)
				&& !StringUtils.containsIgnoreCase(excludeFromTransferFailureStatuses, report.niceStatus))) {
			// Globus task requires some manual intervention. If it's a recoverable failure,
			// we give it some time to recover before we cancel.
			// For errors not recoverable, we cancel immediately.
			if (!StringUtils.isEmpty(report.niceStatus)
					&& StringUtils.containsIgnoreCase(recoverableFromTransferFailureStatuses, report.niceStatus)) {
				// This task status is one of the recoverable failure statuses. Check if it's
				// time to cancel it.
				if (!recoverableFailureTasks.containsKey(dataTransferRequestId)) {
					// First detection of the recoverable failure. Keep track of it.
					HpcGlobusTransferStatusTimestamp transfaerStatusTimestamp = new HpcGlobusTransferStatusTimestamp();
					transfaerStatusTimestamp.niceStatus = report.niceStatus;
					transfaerStatusTimestamp.timestamp = new Date();
					recoverableFailureTasks.put(dataTransferRequestId, transfaerStatusTimestamp);
					logger.error(loggingPrefix
							+ "[GLOBUS] new recoverable transfer failure. recovery period timer startred: globus-task-id: {}, status: {}, niceStatus: {}",
							dataTransferRequestId, report.status, report.niceStatus);
					return false;
				}

				HpcGlobusTransferStatusTimestamp transfaerStatusTimestamp = recoverableFailureTasks
						.get(dataTransferRequestId);
				if (transfaerStatusTimestamp.niceStatus.equals(report.niceStatus)) {
					if (new Date().getTime() - transfaerStatusTimestamp.timestamp.getTime() < recoverableFailureTimeout
							* 1000 * 60) {
						// The transfer is still experiencing the 'recoverable failure', but we are
						// within the recovery period, so we give it more time to recover.
						logger.error(loggingPrefix
								+ "[GLOBUS] tracked recoverable transfer failure. within recovery period: globus-task-id: {}, status: {}, niceStatus: {}",
								dataTransferRequestId, report.status, report.niceStatus);
						return false;
					}

				} else {
					// This task is now facing a new recoverable failure. Reset the timestamp.
					transfaerStatusTimestamp.niceStatus = report.niceStatus;
					transfaerStatusTimestamp.timestamp = new Date();
					logger.error(loggingPrefix
							+ "[GLOBUS] new (and different) recoverable transfer failure. period timer re-started: globus-task-id: {}, status: {}, niceStatus: {}",
							dataTransferRequestId, report.status, report.niceStatus);
					return false;
				}
			}

			// The transfer task is deemed failed and needs to be cancelled.
			String transferErrorMessage = recoverableFailureTasks.remove(dataTransferRequestId) != null
					? "Transfer error recovery period expired [" + recoverableFailureTimeout + " minutes]"
					: "Transfer error";

			logger.error(
					loggingPrefix
							+ "[GLOBUS} {}. transferred deemed failed: globus-task-id: {}, status: {}, niceStatus: {}",
					transferErrorMessage, dataTransferRequestId, report.status, report.niceStatus);
			try {
				cancelTransferRequest(authenticatedToken, dataTransferRequestId, "HPC-DME deemed task failed");
				logger.info(loggingPrefix + "[GLOBUS] transfer successfully canceled: globus-task-id: {}",
						dataTransferRequestId);
				report.errorMessage = transferErrorMessage + ". Globus task cancelled";

			} catch (HpcException e) {
				logger.error(loggingPrefix + "[GLOBUS] Failed to cancel task", e);
				report.errorMessage = transferErrorMessage + ". Globus task failed to cancel";
			}

			return true;
		}

		return false;
	}

	/**
	 * Check if a Globus transfer request recovered, and cleared the failed task
	 * tracking.
	 *
	 * @param dataTransferRequestId The globus task ID.
	 * @param report                The Globus transfer report.
	 * @param loggingPrefix         Contextual (download/upload tasks etc) logging
	 *                              text to prefix in logging.
	 */
	private void transferRecovered(String dataTransferRequestId, HpcGlobusDataTransferReport report,
			String loggingPrefix) {
		if (StringUtils.isEmpty(report.niceStatus) || report.niceStatus.equals(OK_STATUS)) {
			HpcGlobusTransferStatusTimestamp transfaerStatusTimestamp = recoverableFailureTasks
					.remove(dataTransferRequestId);
			// Clear this task from the recoverable failure list in case it's there.
			if (transfaerStatusTimestamp != null) {
				// The transfer task recovered from a failure
				logger.error(loggingPrefix
						+ "[GLOBUS} transfer recovered. globus-task-id: {}, status: {}, niceStatus: {}, recoveredNiceStatus: {}",
						recoverableFailureTimeout, dataTransferRequestId, report.status, report.niceStatus,
						transfaerStatusTimestamp.niceStatus);
			}
		}
	}

	/**
	 * Save a file to the local file system (POSIX) archive
	 *
	 * @param sourceFile                 The source file to store.
	 * @param archiveDestinationLocation The archive destination location.
	 * @param baseArchiveDestination     The base archive destination.
	 * @param sudoPassword               (Optional) a sudo password to perform the
	 *                                   copy to the POSIX archive.
	 * @param systemAccount              (Optional) system account to perform the
	 *                                   copy to the POSIX archive and keep system
	 *                                   account as owner
	 * 
	 * @return A data object upload response object.
	 * @throws HpcException on IO exception.
	 */
	private HpcDataObjectUploadResponse saveFile(File sourceFile, HpcFileLocation archiveDestinationLocation,
			HpcArchive baseArchiveDestination, String sudoPassword, String systemAccount) throws HpcException {
		Calendar transferStarted = Calendar.getInstance();
		String archiveFilePath = archiveDestinationLocation.getFileId().replaceFirst(
				baseArchiveDestination.getFileLocation().getFileId(), baseArchiveDestination.getDirectory());
		String archiveDirectory = archiveFilePath.substring(0, archiveFilePath.lastIndexOf('/'));

		try {
			exec("install -d -o " + systemAccount + " " + archiveDirectory, sudoPassword, null, null);
			exec("chown -R " + systemAccount + " " + baseArchiveDestination.getDirectory(), sudoPassword, null, null);
			exec("cp " + sourceFile.getAbsolutePath() + " " + archiveFilePath, sudoPassword, null, null);
			exec("chown " + systemAccount + " " + archiveFilePath, sudoPassword, null, null);

		} catch (HpcException e) {
			throw new HpcException(
					"Failed to copy file to POSIX archive: " + archiveFilePath + " - [" + e.getMessage() + "]",
					HpcErrorType.DATA_TRANSFER_ERROR, e);
		}

		// Package and return the response.
		HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
		uploadResponse.setArchiveLocation(archiveDestinationLocation);
		uploadResponse.setDataTransferRequestId(String.valueOf(archiveDestinationLocation.hashCode()));
		uploadResponse.setDataTransferType(HpcDataTransferType.GLOBUS);
		uploadResponse.setDataTransferStarted(transferStarted);
		uploadResponse.setDataTransferCompleted(Calendar.getInstance());
		uploadResponse.setSourceSize(sourceFile.length());
		uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.ARCHIVED);
		uploadResponse.setDataTransferMethod(HpcDataTransferUploadMethod.SYNC);

		return uploadResponse;
	}

	/**
	 * Return a metadata file for a given path.
	 *
	 * @param archiveFilePath The file path in the file system archive.
	 * @return The metadata file associated with this path.
	 */
	private File getMetadataFile(String archiveFilePath) {
		int lastSlashIndex = archiveFilePath.lastIndexOf('/');
		return new File(
				archiveFilePath.substring(0, lastSlashIndex) + "/." + archiveFilePath.substring(lastSlashIndex + 1));
	}
}
