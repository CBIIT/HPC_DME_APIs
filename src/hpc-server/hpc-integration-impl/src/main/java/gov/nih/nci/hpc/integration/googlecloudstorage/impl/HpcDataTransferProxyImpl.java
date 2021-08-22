package gov.nih.nci.hpc.integration.googlecloudstorage.impl;

import java.io.InputStream;
import java.nio.channels.Channels;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;

/**
 * HPC Data Transfer Proxy Google Drive Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataTransferProxyImpl implements HpcDataTransferProxy {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Google Drive connection instance.
	@Autowired
	private HpcGoogleCloudStorageConnection googleCloudStorageConnection = null;

	// Date formatter to format files last-modified date
	private DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

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
	public Object authenticate(String accessToken) throws HpcException {
		return googleCloudStorageConnection.authenticate(accessToken);
	}

	@Override
	public String downloadDataObject(Object authenticatedToken, HpcDataObjectDownloadRequest downloadRequest,
			HpcArchive baseArchiveDestination, HpcDataTransferProgressListener progressListener,
			Boolean encryptedTransfer) throws HpcException {
		throw new HpcException("Download from Google Cloud Storage not supported", HpcErrorType.UNEXPECTED_ERROR);
	}

	@Override
	public HpcPathAttributes getPathAttributes(Object authenticatedToken, HpcFileLocation fileLocation, boolean getSize)
			throws HpcException {
		Storage storage = googleCloudStorageConnection.getStorage(authenticatedToken);
		HpcPathAttributes pathAttributes = new HpcPathAttributes();
		pathAttributes.setIsAccessible(true);
		try {
			Blob blob = storage.get(fileLocation.getFileContainerId(), fileLocation.getFileId(),
					Storage.BlobGetOption.fields(Storage.BlobField.values()));
			if (blob == null) {
				pathAttributes.setExists(false);
			} else {
				pathAttributes.setExists(true);
				if (blob.isDirectory() || blob.getSize() == 0) {
					pathAttributes.setIsDirectory(true);
					pathAttributes.setIsFile(false);
				} else {
					pathAttributes.setIsDirectory(false);
					pathAttributes.setIsFile(true);
					if (getSize) {
						pathAttributes.setSize(blob.getSize());
					}
				}
			}

		} catch (StorageException e) {
			if (e.getCode() == 401 || e.getCode() == 403) {
				pathAttributes.setIsAccessible(false);
			} else {
				throw new HpcException("[Google Cloud Storage] Failed to get file: " + e.getMessage(),
						HpcErrorType.DATA_TRANSFER_ERROR, e);
			}
		}

		return pathAttributes;
	}

	@Override
	public InputStream generateDownloadInputStream(Object authenticatedToken, HpcFileLocation fileLocation)
			throws HpcException {
		Storage storage = googleCloudStorageConnection.getStorage(authenticatedToken);
		try {
			return Channels.newInputStream(storage.reader(fileLocation.getFileContainerId(), fileLocation.getFileId()));

		} catch (StorageException e) {
			throw new HpcException("[Google Cloud Storage] Failed to generate download InputStream: " + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, e);
		}
	}

	@Override
	public List<HpcDirectoryScanItem> scanDirectory(Object authenticatedToken, HpcFileLocation directoryLocation)
			throws HpcException {
		Storage storage = googleCloudStorageConnection.getStorage(authenticatedToken);
		List<HpcDirectoryScanItem> directoryScanItems = new ArrayList<>();

		try {
			storage.list(directoryLocation.getFileContainerId(),
					Storage.BlobListOption.prefix(directoryLocation.getFileId())).iterateAll().forEach(blob -> {
						if (blob.getSize() > 0) {
							HpcDirectoryScanItem directoryScanItem = new HpcDirectoryScanItem();
							directoryScanItem.setFilePath(blob.getName());
							directoryScanItem.setFileName(FilenameUtils.getName(blob.getName()));
							directoryScanItem.setLastModified(dateFormat.format(blob.getUpdateTime()));
							directoryScanItems.add(directoryScanItem);
						}
					});

			return directoryScanItems;

		} catch (StorageException e) {
			throw new HpcException("[Google Cloud Storage] Failed to list objects: " + e.getMessage(),
					HpcErrorType.DATA_TRANSFER_ERROR, e);
		}
	}
}
