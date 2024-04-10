package gov.nih.nci.hpc.integration.box.impl;

import static gov.nih.nci.hpc.util.HpcUtil.toNormalizedPath;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxAPIResponseException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemTokens;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;

/**
 * HPC Data Transfer Proxy Box Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataTransferProxyImpl implements HpcDataTransferProxy {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// The file size in which we can use the 'transfer large file in chunks' API.
	private static final Long BOX_LARGE_FILE_TRANSFER_THRESHOLD = 20000000L;

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Box download executor.
	@Autowired
	@Qualifier("hpcBoxDownloadExecutor")
	Executor boxExecutor = null;

	// The Box connection instance.
	@Autowired
	private HpcBoxConnection boxConnection = null;

	// In memory local cache of access and refresh tokens
	private final HashMap<String, String> tokensMap = new HashMap<>();

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
	public synchronized Object authenticate(HpcIntegratedSystemAccount dataTransferAccount, String accessToken,
			String refreshToken) throws HpcException {
		if (dataTransferAccount == null) {
			throw new HpcException("Box System account not registered", HpcErrorType.UNEXPECTED_ERROR);
		}

		Object token = boxConnection.authenticate(dataTransferAccount, accessToken,
				tokensMap.containsKey(accessToken) ? tokensMap.get(accessToken) : refreshToken);

		// Refresh the token.
		BoxAPIConnection boxApi = boxConnection.getBoxAPIConnection(token);
		try {
			boxApi.refresh();

		} catch (BoxAPIResponseException e) {
			// Token expired.
			throw new HpcException("[Box] Invalid token: " + e.getMessage(), HpcErrorType.INVALID_REQUEST_INPUT, e);

		} catch (BoxAPIException e) {
			throw new HpcException("[Box] Failed to refresh token: " + e.getMessage(), HpcErrorType.DATA_TRANSFER_ERROR,
					e);
		}

		// Update the tokens in-memory cache w/ the freshly created refresh-token.
		tokensMap.put(accessToken, boxApi.getRefreshToken());

		return token;
	}

	@Override
	public HpcIntegratedSystemTokens getIntegratedSystemTokens(Object authenticatedToken) throws HpcException {
		// Get the Box connection.
		BoxAPIConnection boxApi = boxConnection.getBoxAPIConnection(authenticatedToken);

		HpcIntegratedSystemTokens boxTokens = new HpcIntegratedSystemTokens();
		boxTokens.setAccessToken(boxApi.getAccessToken());
		boxTokens.setRefreshToken(boxApi.getRefreshToken());

		return boxTokens;
	}

	@Override
	public String downloadDataObject(Object authenticatedToken, HpcDataObjectDownloadRequest downloadRequest,
			HpcArchive baseArchiveDestination, HpcDataTransferProgressListener progressListener,
			Boolean encryptedTransfer) throws HpcException {
		// Input validation
		if (progressListener == null) {
			throw new HpcException("[Box] No progress listener provided for a download to Box destination",
					HpcErrorType.UNEXPECTED_ERROR);
		}

		// Get the Box connection.
		final BoxAPIConnection boxApi = boxConnection.getBoxAPIConnection(authenticatedToken);

		// Stream the file to Box.
		CompletableFuture<Void> boxDownloadFuture = CompletableFuture.runAsync(() -> {
			try {
				// Find / Create the folder in Box where we download the file to
				String destinationPath = toNormalizedPath(
						downloadRequest.getBoxDestination().getDestinationLocation().getFileId());
				int lastSlashIndex = destinationPath.lastIndexOf('/');
				String destinationFolderPath = destinationPath.substring(0, lastSlashIndex);
				String destinationFileName = destinationPath.substring(lastSlashIndex + 1);
				BoxFolder boxFolder = getFolder(boxApi, destinationFolderPath, true);

				// Transfer the file to Box.
				BoxFile.Info fileInfo = null;
				if (downloadRequest.getSize() > BOX_LARGE_FILE_TRANSFER_THRESHOLD) {
					// Use the 'upload large file in chunks' API.
					fileInfo = boxFolder.uploadLargeFile(new URL(downloadRequest.getArchiveLocationURL()).openStream(),
							destinationFileName, downloadRequest.getSize());
				} else {
					fileInfo = boxFolder.uploadFile(new URL(downloadRequest.getArchiveLocationURL()).openStream(),
							destinationFileName);
				}

				progressListener.transferCompleted(fileInfo.getSize());

			} catch (BoxAPIException | InterruptedException | IOException | HpcException e) {
				String message = "[Box] Failed to download object: " + e.getMessage();
				logger.error(message, HpcErrorType.DATA_TRANSFER_ERROR, e);
				progressListener.transferFailed(message);
			}

		}, boxExecutor);

		return String.valueOf(boxDownloadFuture.hashCode());

	}

	@Override
	public HpcPathAttributes getPathAttributes(Object authenticatedToken, HpcFileLocation fileLocation, boolean getSize)
			throws HpcException {

		// Drive drive = googleDriveConnection.getDrive(authenticatedToken);

		HpcPathAttributes pathAttributes = new HpcPathAttributes();
		/*
		 * pathAttributes.setIsAccessible(true);
		 * 
		 * try { File file = getFile(drive, fileLocation.getFileId()); if (file == null)
		 * { pathAttributes.setExists(false); } else { pathAttributes.setExists(true);
		 * if (file.getMimeType().equals(FOLDER_MIME_TYPE)) {
		 * pathAttributes.setIsDirectory(true); pathAttributes.setIsFile(false); } else
		 * { pathAttributes.setIsDirectory(false); pathAttributes.setIsFile(true); if
		 * (getSize) { pathAttributes.setSize(file.getSize()); } } }
		 * 
		 * } catch (GoogleJsonResponseException e) { if (e.getStatusCode() == 401) {
		 * pathAttributes.setIsAccessible(false); } else { throw new
		 * HpcException("[Google Drive] Failed to get file: " + e.getMessage(),
		 * HpcErrorType.DATA_TRANSFER_ERROR, e); } } catch (IOException e) { throw new
		 * HpcException("[Google Drive] Failed to get file: " + e.getMessage(),
		 * HpcErrorType.DATA_TRANSFER_ERROR, e); }
		 */
		return pathAttributes;
	}

	/*
	 * @Override public InputStream generateDownloadInputStream(Object
	 * authenticatedToken, HpcFileLocation fileLocation) throws HpcException { Drive
	 * drive = googleDriveConnection.getDrive(authenticatedToken); try { File file =
	 * getFile(drive, fileLocation.getFileId()); if (file == null ||
	 * file.getMimeType().equals(FOLDER_MIME_TYPE)) { throw new
	 * HpcException("Google drive file not found", HpcErrorType.UNEXPECTED_ERROR); }
	 * 
	 * return drive.files().get(file.getId()).executeMediaAsInputStream();
	 * 
	 * } catch (IOException e) { throw new
	 * HpcException("[Google Drive] Failed to generate download InputStream: " +
	 * e.getMessage(), HpcErrorType.DATA_TRANSFER_ERROR, e); } }
	 * 
	 * @Override public List<HpcDirectoryScanItem> scanDirectory(Object
	 * authenticatedToken, HpcFileLocation directoryLocation) throws HpcException {
	 * Drive drive = googleDriveConnection.getDrive(authenticatedToken);
	 * List<HpcDirectoryScanItem> directoryScanItems = new ArrayList<>();
	 * 
	 * try { // Get the id and path of the folder to be scanned. // Note:
	 * directoryLocation.getFileId() can be either the Google Drive ID of the //
	 * folder, or a // path. File folder = getFile(drive,
	 * directoryLocation.getFileId()); if (folder != null &&
	 * folder.getMimeType().contentEquals(FOLDER_MIME_TYPE)) { String folderId =
	 * folder.getId(); String folderPath =
	 * folderId.equals(directoryLocation.getFileId()) ? getFolderPath(drive, folder)
	 * : toNormalizedPath(directoryLocation.getFileId()); scanDirectory(drive,
	 * directoryScanItems, folderPath, folderId); } } catch (IOException e) { throw
	 * new HpcException("[Google Drive] Failed to generate download InputStream: " +
	 * e.getMessage(), HpcErrorType.DATA_TRANSFER_ERROR, e); }
	 * 
	 * return directoryScanItems; }
	 */
	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Find a folder in Box.
	 *
	 * @param boxApi     A Box API instance.
	 * @param folderPath The folder path to find in Box.
	 * @param create     If true, the folder will be created if not found.
	 * @return The folder ID or null if not found.
	 * @throws HpcException on data transfer system failure.
	 */

	private BoxFolder getFolder(BoxAPIConnection boxApi, String folderPath, boolean create) throws HpcException {
		// Get the root folder if requested.
		BoxFolder boxFolder = null;
		try {
			boxFolder = BoxFolder.getRootFolder(boxApi);

		} catch (BoxAPIException e) {
			throw new HpcException("[Box] Failed to get root folder: " + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, e);
		}

		// Go through the folder path items and find/create the child folder.
		for (String childFolderName : folderPath.split("/")) {
			if (!StringUtils.isEmpty(childFolderName) && boxFolder != null) {
				boxFolder = getChildFolder(boxApi, boxFolder, childFolderName, create);
			}
		}

		return boxFolder;
	}

	/**
	 * Find a Child in Box.
	 *
	 * @param boxApi          A Box API instance.
	 * @param boxFolder       The box folder to search in for a child folder.
	 * @param childFolderName The child folder name.
	 * @param create          If true, the child folder will be created if not
	 *                        found.
	 * @return The folder ID or null if not found.
	 * @throws HpcException on data transfer system failure.
	 */

	private BoxFolder getChildFolder(BoxAPIConnection boxApi, BoxFolder boxFolder, String childFolderName,
			boolean create) throws HpcException {
		try {
			for (BoxItem.Info itemInfo : boxFolder) {
				if (itemInfo instanceof BoxFolder.Info && itemInfo.getName().equals(childFolderName)) {
					// Found the child folder.
					return ((BoxFolder.Info) itemInfo).getResource();
				}
			}

			// Child folder not found. Create if requested.
			if (create) {
				return boxFolder.createFolder(childFolderName).getResource();
			}

			return null;

		} catch (BoxAPIException e) {
			throw new HpcException("[Box] Failed to get child folder: " + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, e);
		}
	}

	/**
	 * Get a file/folder by ID or path.
	 *
	 * @param drive    A Google drive instance.
	 * @param idOrPath The file/folder ID or path to get.
	 * @return A File instance or null if not found
	 * @throws IOException on data transfer system failure.
	 */
	/*
	 * private File getFile(Drive drive, String idOrPath) throws IOException { //
	 * Search by ID. try { return
	 * drive.files().get(idOrPath).setFields("id,name,mimeType,size,parents").
	 * execute();
	 * 
	 * } catch (GoogleJsonResponseException e) { if (e.getStatusCode() != 404) {
	 * throw e; } }
	 * 
	 * // File not found by ID, search by path String filePath =
	 * toNormalizedPath(idOrPath); int lastSlashIndex = filePath.lastIndexOf('/');
	 * String folderId = getFolderId(drive, filePath.substring(0, lastSlashIndex),
	 * false); if (folderId == null) { // folder not found. return null; }
	 * 
	 * String fileName = filePath.substring(lastSlashIndex + 1); FileList result =
	 * drive.files().list().setQ(String.format(PATH_QUERY, fileName, folderId))
	 * .setFields("files(id,name,mimeType,size,parents)").execute(); if
	 * (!result.getFiles().isEmpty()) { // Note: In Google Drive, it's possible to
	 * have multiple files with the same // name // We simply grab the first one on
	 * the list. return result.getFiles().get(0); }
	 * 
	 * return null; }
	 */

	/**
	 * Scan a directory recursively.
	 *
	 * @param drive              A Google drive instance.
	 * @param directoryScanItems Add scan items to this collection.
	 * @param folderPath         The folder path scanned.
	 * @param folderId           The folder ID scanned.
	 * @throws IOException on data transfer system failure.
	 */
	/*
	 * private void scanDirectory(Drive drive, List<HpcDirectoryScanItem>
	 * directoryScanItems, String folderPath, String folderId) throws IOException {
	 * 
	 * for (File file :
	 * drive.files().list().setQ(String.format(DIRECTORY_SCAN_QUERY, folderId))
	 * .setFields("files(id,name,mimeType,modifiedTime)").execute().getFiles()) {
	 * String filePath = folderPath + "/" + file.getName(); if
	 * (file.getMimeType().equals(FOLDER_MIME_TYPE)) { // It's a sub-folder. Scan
	 * it. scanDirectory(drive, directoryScanItems, filePath, file.getId());
	 * 
	 * } else { // It's a file. Add to the list HpcDirectoryScanItem
	 * directoryScanItem = new HpcDirectoryScanItem();
	 * directoryScanItem.setFilePath(filePath);
	 * directoryScanItem.setFileName(file.getName());
	 * directoryScanItem.setLastModified(dateFormat.format(new
	 * Date(file.getModifiedTime().getValue())));
	 * directoryScanItems.add(directoryScanItem); } } }
	 */

	/**
	 * Get the folder path of a Google Drive folder.
	 *
	 * @param drive  A Google drive instance.
	 * @param folder The folder to get the path for.
	 * @return The folder path.
	 * @throws IOException on data transfer system failure.
	 */
	/*
	 * private String getFolderPath(Drive drive, File folder) throws IOException {
	 * 
	 * String rootId = drive.files().get("root").execute().getId(); String
	 * folderPath = folder.getName(); while (!folder.getParents().contains(rootId))
	 * { folder = getFile(drive, folder.getParents().get(0)); folderPath =
	 * folder.getName() + "/" + folderPath; }
	 * 
	 * return toNormalizedPath(folderPath); }
	 */
}
