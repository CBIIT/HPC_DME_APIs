package gov.nih.nci.hpc.integration.box.impl;

import static gov.nih.nci.hpc.util.HpcUtil.toNormalizedPath;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxCollection;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.box.sdk.BoxSearch;
import com.box.sdk.BoxSearchParameters;
import com.box.sdk.PartialCollection;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
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
	/*
	 * // Google drive folder mime-type. private static final String
	 * FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
	 * 
	 * // Google drive search query for a folder. private static final String
	 * FOLDER_QUERY = "mimeType = '" + FOLDER_MIME_TYPE +
	 * "' and name = '%s' and '%s' in parents";
	 * 
	 * // Google drive search query for a file / folder. private static final String
	 * PATH_QUERY = "name = '%s' and '%s' in parents";
	 * 
	 * // Google drive query for all files under a folder. private static final
	 * String DIRECTORY_SCAN_QUERY = "'%s' in parents";
	 */
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

	/*
	 * // The maximum size of individual chunks that will get uploaded by single
	 * HTTP // request.
	 * 
	 * @Value("${hpc.integration.googledrive.chunkSize}") int chunkSize = -1;
	 * 
	 * // Locks to synchronize threads executing on path. private Striped<Lock>
	 * pathLocks = Striped.lock(127);
	 * 
	 * // Date formatter to format files last-modified date private DateFormat
	 * dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
	 */

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
	public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount, String authCode) throws HpcException {
		if (dataTransferAccount == null) {
			throw new HpcException("Box System account not registered", HpcErrorType.UNEXPECTED_ERROR);
		}

		return boxConnection.authenticate(dataTransferAccount, authCode);
	}

	@Override
	public String downloadDataObject(Object authenticatedToken, HpcDataObjectDownloadRequest downloadRequest,
			HpcArchive baseArchiveDestination, HpcDataTransferProgressListener progressListener,
			Boolean encryptedTransfer) throws HpcException {
		logger.error("ERAN 1");
		// Input validation
		if (progressListener == null) {
			throw new HpcException("[Box] No progress listener provided for a download to Box destination",
					HpcErrorType.UNEXPECTED_ERROR);
		}
		logger.error("ERAN 2");
		// Authenticate the Box access token.
		final BoxAPIConnection boxApi = boxConnection.getBoxAPIConnection(authenticatedToken);

		logger.error("ERAN 3");

		// Stream the file to Box.
		CompletableFuture<Void> boxDownloadFuture = CompletableFuture.runAsync(() -> {
			try {
				// Find the first 10 files matching "taxes"
				long offsetValue = 0;
				long limitValue = 10;
				BoxSearch boxSearch = new BoxSearch(boxApi);
				BoxSearchParameters searchParams = new BoxSearchParameters();
				searchParams.setQuery("eran_pi_cli_test/Project_test");
				searchParams.setType("folder");
				PartialCollection<BoxItem.Info> searchResults = boxSearch.searchRange(offsetValue, limitValue, searchParams);
				searchResults.forEach(info -> logger.error("ERAN 3.5 - {} - {} - {}", info.getID(), info.getName(), info.getPathCollection()));
				
				logger.error("ERAN 4");
				// Find / Create the folder in Box where we download the file to
				String destinationPath = toNormalizedPath(
						downloadRequest.getBoxDestination().getDestinationLocation().getFileId());
				int lastSlashIndex = destinationPath.lastIndexOf('/');
				String destinationFolderPath = destinationPath.substring(0, lastSlashIndex);
				String destinationFileName = destinationPath.substring(lastSlashIndex + 1);
				
				//BoxCollection favorites = null;
				for (BoxCollection.Info info : BoxCollection.getAllCollections(boxApi)) {
					logger.error("ERAN 4.5 - {} - {} - {}", info.getID(), info.getName(), info.getCollectionType());
				  //  if (info.getCollectionType().equals("favorites")) {
				    //    favorites = info.getResource();
				      //  break;
				    //}
				}

				logger.error("ERAN 5");

				BoxCollection boxCollection = new BoxCollection(boxApi,
						downloadRequest.getBoxDestination().getDestinationLocation().getFileContainerId());
				logger.error("ERAN 5, {}", boxCollection != null ? boxCollection.getID() : "nil");
				
				boxCollection = new BoxCollection(boxApi, "Download");
				logger.error("ERAN 5.5 , {}", boxCollection != null ? boxCollection.getID() : "nil");
				
				logger.error("ERAN 6");
				BoxFolder boxFolder = new BoxFolder(boxApi, destinationFolderPath);
				boxFolder = BoxFolder.getRootFolder(boxApi);
				
				for (BoxItem.Info itemInfo : boxFolder) {
				    if (itemInfo instanceof BoxFile.Info) {
				        BoxFile.Info fileInfo = (BoxFile.Info) itemInfo;
				        logger.error("ERAN 6.5 - {} - {}", fileInfo.getID(), fileInfo.getName());
				        // Do something with the file.
				    } else if (itemInfo instanceof BoxFolder.Info) {
				        BoxFolder.Info folderInfo = (BoxFolder.Info) itemInfo;
				        logger.error("ERAN 6.6 - {} - {}", folderInfo.getID(), folderInfo.getName());
				        // Do something with the folder.
				    }
				}
				
				logger.error("ERAN 7");
				//boxFolder.setCollections(boxCollection);
				logger.error("ERAN 8 - {}", destinationFileName);

				// Transfer the file to Box, and complete the download task.
				//BoxFile.Info fileInfo = boxFolder.uploadLargeFile(
				//		new URL(downloadRequest.getArchiveLocationURL()).openStream(), destinationFileName,
				//		downloadRequest.getSize());
				
				
				BoxFile.Info fileInfo = boxFolder.uploadFile(
						new URL(downloadRequest.getArchiveLocationURL()).openStream(), destinationFileName);
				
				logger.error("ERAN 9 - {}, - {} - {}", fileInfo.getID(), fileInfo.getName(), fileInfo.getSize());

				progressListener.transferCompleted(fileInfo.getSize());
				logger.error("ERAN 10");

			} catch (BoxAPIException | /*InterruptedException | */IOException e) {
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
	 * Find the ID of a folder in Google Drive. If not found, the folder is created.
	 *
	 * @param drive      A Google drive instance.
	 * @param folderPath The folder path to find / create in Google Drive.
	 * @param create     If true, the folder will be created if not found.
	 * @return The folder ID or null if not found.
	 * @throws IOException on data transfer system failure.
	 */
	/*
	 * private String getFolderId(Drive drive, String folderPath, boolean create)
	 * throws IOException { String parentFolderId = "root"; if
	 * (!StringUtils.isEmpty(folderPath)) { // If the request is to create a folder
	 * if not found, we lock the path. Lock lock = pathLocks.get(folderPath); if
	 * (create) { lock.lock(); }
	 * 
	 * try { boolean createNewFolder = false; for (String subFolderName :
	 * folderPath.split("/")) { if (StringUtils.isEmpty(subFolderName)) { continue;
	 * } if (!createNewFolder) { // Search for the sub-folder. FileList result =
	 * drive.files().list() .setQ(String.format(FOLDER_QUERY, subFolderName,
	 * parentFolderId)).setFields("files(id)") .execute(); if
	 * (!result.getFiles().isEmpty()) { // Sub-folder was found. Note: In Google
	 * Drive, it's possible to have multiple // folders // with the same name // We
	 * simply grab the first one on the list. parentFolderId =
	 * result.getFiles().get(0).getId(); continue;
	 * 
	 * } else { // Sub-folder was not found. if (!create) { // Requested to not
	 * create folders. return null; } createNewFolder = true; } }
	 * 
	 * // Creating a new sub folder. parentFolderId = drive.files() .create(new
	 * File().setName(subFolderName)
	 * .setParents(Collections.singletonList(parentFolderId))
	 * .setMimeType(FOLDER_MIME_TYPE)) .setFields("id").execute().getId(); } }
	 * finally { if (create) { lock.unlock(); } } }
	 * 
	 * return parentFolderId; }
	 */

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
