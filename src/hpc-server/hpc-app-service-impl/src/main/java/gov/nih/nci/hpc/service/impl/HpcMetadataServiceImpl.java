/**
 * HpcMetadataServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileLocation;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidMetadataEntries;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.CALLER_OBJECT_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.CHECKSUM_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.CONFIGURATION_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_COMPLETED_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_METHOD_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_REQUEST_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_STARTED_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_STATUS_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_TYPE_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DEEP_ARCHIVE_DATE_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DEEP_ARCHIVE_STATUS_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DME_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.EXTRACTED_METADATA_ATTRIBUTES_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.LINK_SOURCE_PATH_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.METADATA_UPDATED_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.REGISTRAR_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.REGISTRAR_NAME_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.REGISTRATION_COMPLETION_EVENT_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.S3_ARCHIVE_CONFIGURATION_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_FILE_GROUP_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_FILE_GROUP_DN_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_FILE_GROUP_NIH_DN_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_FILE_NIH_GROUP_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_FILE_NIH_OWNER_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_FILE_NIH_USER_DN_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_FILE_OWNER_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_FILE_PERMISSIONS_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_FILE_SIZE_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_FILE_URL_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_FILE_USER_DN_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_LOCATION_FILE_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DELETED_DATE_ATTRIBUTE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.IOUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathPermissions;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadMethod;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDeepArchiveStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDirectoryScanItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcDomainValidationResult;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcGroupedMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcSelfMetadataEntries;
import gov.nih.nci.hpc.domain.model.HpcDistinguishedNameSearch;
import gov.nih.nci.hpc.domain.model.HpcDistinguishedNameSearchResult;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * HPC Data Management Application Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcMetadataServiceImpl implements HpcMetadataService {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	private static final String INVALID_PATH_MSG = "Invalid collection or object path";
	private static final String INVALID_METADATA_MSG = "Invalid metadata entry in request";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Data Management Service
	@Autowired
	private HpcDataManagementService dataManagementService = null;

	// The Security Service
	@Autowired
	private HpcSecurityService securityService = null;

	// The Data Management Proxy instance.
	@Autowired
	private HpcDataManagementProxy dataManagementProxy = null;

	// The Data Management Authenticator.
	@Autowired
	private HpcDataManagementAuthenticator dataManagementAuthenticator = null;

	// Metadata Validator.
	@Autowired
	private HpcMetadataValidator metadataValidator = null;

	// Key Generator.
	@Autowired
	private HpcKeyGenerator keyGenerator = null;

	// Metadata DAO.
	@Autowired
	private HpcMetadataDAO metadataDAO = null;

	// Date formatter to format metadata entries of type Calendar (like data
	// transfer start/completion time).
	private DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

	// Default collection metadata.
	private List<HpcMetadataEntry> defaultCollectionMetadataEntries = new ArrayList<>();

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 *
	 */
	private HpcMetadataServiceImpl() {
		// Set the default collection metadata.
		defaultCollectionMetadataEntries.add(toMetadataEntry("collection_type", "Folder"));
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcMetadataService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void addMetadataToCollection(String path, List<HpcMetadataEntry> metadataEntries, String configurationId)
			throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException(INVALID_PATH_MSG, HpcErrorType.INVALID_REQUEST_INPUT);
		} else {
			HpcDomainValidationResult validationResult = isValidMetadataEntries(metadataEntries, false);
			if (!validationResult.getValid()) {
				if (StringUtils.isEmpty(validationResult.getMessage())) {
					throw new HpcException(INVALID_METADATA_MSG, HpcErrorType.INVALID_REQUEST_INPUT);
				} else {
					throw new HpcException(validationResult.getMessage(), HpcErrorType.INVALID_REQUEST_INPUT);
				}
			}
		}

		// Validate Metadata.
		metadataValidator.validateCollectionMetadata(configurationId, null, metadataEntries);

		// Add Metadata to the DM system.
		dataManagementProxy.addMetadataToCollection(dataManagementAuthenticator.getAuthenticatedToken(), path,
				metadataEntries);
	}

	@Override
	public void updateCollectionMetadata(String path, List<HpcMetadataEntry> metadataEntries, String configurationId)
			throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException(INVALID_PATH_MSG, HpcErrorType.INVALID_REQUEST_INPUT);
		} else {
			HpcDomainValidationResult validationResult = isValidMetadataEntries(metadataEntries, true);
			if (!validationResult.getValid()) {
				if (StringUtils.isEmpty(validationResult.getMessage())) {
					throw new HpcException(INVALID_METADATA_MSG, HpcErrorType.INVALID_REQUEST_INPUT);
				} else {
					throw new HpcException(validationResult.getMessage(), HpcErrorType.INVALID_REQUEST_INPUT);
				}
			}
		}

		// Validate collection type is not in the update request.
		List<HpcMetadataEntry> existingMetadataEntries = dataManagementProxy
				.getCollectionMetadata(dataManagementAuthenticator.getAuthenticatedToken(), path);
		validateCollectionTypeUpdate(existingMetadataEntries, metadataEntries);

		// Validate the metadata.
		metadataValidator.validateCollectionMetadata(configurationId, existingMetadataEntries, metadataEntries);

		// Update the 'metadata updated' system-metadata to record the time of this
		// metadata update.
		metadataEntries.add(generateMetadataUpdatedMetadata());

		// Update the metadata.
		dataManagementProxy.updateCollectionMetadata(dataManagementAuthenticator.getAuthenticatedToken(), path,
				metadataEntries);
	}

	@Override
	public void copyMetadataToCollection(String path, List<HpcMetadataEntry> metadataEntries)
			throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException(INVALID_PATH_MSG, HpcErrorType.INVALID_REQUEST_INPUT);
		} else {
			HpcDomainValidationResult validationResult = isValidMetadataEntries(metadataEntries, false);
			if (!validationResult.getValid()) {
				if (StringUtils.isEmpty(validationResult.getMessage())) {
					throw new HpcException(INVALID_METADATA_MSG, HpcErrorType.INVALID_REQUEST_INPUT);
				} else {
					throw new HpcException(validationResult.getMessage(), HpcErrorType.INVALID_REQUEST_INPUT);
				}
			}
		}

		// Add Metadata to the DM system.
		dataManagementProxy.addMetadataToCollection(dataManagementAuthenticator.getAuthenticatedToken(), path,
				metadataEntries);
	}
	
	@Override
	public HpcSystemGeneratedMetadata addSystemGeneratedMetadataToCollection(String path, String userId,
			String userName, String configurationId) throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException(INVALID_PATH_MSG, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();

		// Generate a UUID and add it as metadata.
		metadataEntries.add(generateIdMetadata());

		// Generate and add DME ID metadata.
		metadataEntries.add(generateDmeIdMetadata(dataManagementService.getCollection(path, false).getCollectionId()));

		// Create the Metadata-Updated metadata.
		metadataEntries.add(generateMetadataUpdatedMetadata());

		// Create and add the registrar ID, name and data management configuration
		// metadata.
		metadataEntries.addAll(generateRegistrarMetadata(userId, userName, configurationId));

		// Add Metadata to the DM system.
		dataManagementProxy.addMetadataToCollection(dataManagementAuthenticator.getAuthenticatedToken(), path,
				metadataEntries);

		return toSystemGeneratedMetadata(metadataEntries);
	}

	@Override
	public HpcSystemGeneratedMetadata getCollectionSystemGeneratedMetadata(String path) throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException(INVALID_PATH_MSG, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return toSystemGeneratedMetadata(
				dataManagementProxy.getCollectionMetadata(dataManagementAuthenticator.getAuthenticatedToken(), path));
	}

	@Override
	public HpcSystemGeneratedMetadata toSystemGeneratedMetadata(List<HpcMetadataEntry> systemGeneratedMetadataEntries)
			throws HpcException {
		// Extract the system generated data-object metadata entries from the entire
		// set.
		Map<String, String> metadataMap = toMap(systemGeneratedMetadataEntries);
		HpcSystemGeneratedMetadata systemGeneratedMetadata = new HpcSystemGeneratedMetadata();
		systemGeneratedMetadata.setObjectId(metadataMap.get(ID_ATTRIBUTE));
		systemGeneratedMetadata.setDmeDataId(metadataMap.get(DME_ID_ATTRIBUTE));
		systemGeneratedMetadata.setRegistrarId(metadataMap.get(REGISTRAR_ID_ATTRIBUTE));
		systemGeneratedMetadata.setRegistrarName(metadataMap.get(REGISTRAR_NAME_ATTRIBUTE));
		systemGeneratedMetadata.setConfigurationId(metadataMap.get(CONFIGURATION_ID_ATTRIBUTE));
		systemGeneratedMetadata.setS3ArchiveConfigurationId(metadataMap.get(S3_ARCHIVE_CONFIGURATION_ID_ATTRIBUTE));
		systemGeneratedMetadata.setLinkSourcePath(metadataMap.get(LINK_SOURCE_PATH_ATTRIBUTE));
		systemGeneratedMetadata
				.setExtractedMetadataAttributes(metadataMap.get(EXTRACTED_METADATA_ATTRIBUTES_ATTRIBUTE));

		HpcFileLocation archiveLocation = new HpcFileLocation();
		archiveLocation.setFileContainerId(metadataMap.get(ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE));
		archiveLocation.setFileId(metadataMap.get(ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE));
		if (archiveLocation.getFileContainerId() != null || archiveLocation.getFileId() != null) {
			systemGeneratedMetadata.setArchiveLocation(archiveLocation);
		}

		HpcFileLocation sourceLocation = new HpcFileLocation();
		sourceLocation.setFileContainerId(metadataMap.get(SOURCE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE));
		sourceLocation.setFileId(metadataMap.get(SOURCE_LOCATION_FILE_ID_ATTRIBUTE));
		if (sourceLocation.getFileContainerId() != null || sourceLocation.getFileId() != null) {
			systemGeneratedMetadata.setSourceLocation(sourceLocation);
		}

		systemGeneratedMetadata.setDataTransferRequestId(metadataMap.get(DATA_TRANSFER_REQUEST_ID_ATTRIBUTE));
		if (metadataMap.get(DATA_TRANSFER_STATUS_ATTRIBUTE) != null) {
			try {
				systemGeneratedMetadata.setDataTransferStatus(
						HpcDataTransferUploadStatus.fromValue(metadataMap.get(DATA_TRANSFER_STATUS_ATTRIBUTE)));

			} catch (Exception e) {
				logger.error(
						"Unable to determine data transfer status: " + metadataMap.get(DATA_TRANSFER_STATUS_ATTRIBUTE),
						e);
				systemGeneratedMetadata.setDataTransferStatus(HpcDataTransferUploadStatus.FAILED);
			}
		}

		if (metadataMap.get(DATA_TRANSFER_METHOD_ATTRIBUTE) != null) {
			try {
				systemGeneratedMetadata.setDataTransferMethod(
						HpcDataTransferUploadMethod.fromValue(metadataMap.get(DATA_TRANSFER_METHOD_ATTRIBUTE)));

			} catch (Exception e) {
				logger.error(
						"Unable to determine data transfer method: " + metadataMap.get(DATA_TRANSFER_METHOD_ATTRIBUTE),
						e);
				systemGeneratedMetadata.setDataTransferMethod(null);
			}
		}

		if (metadataMap.get(DATA_TRANSFER_TYPE_ATTRIBUTE) != null) {
			try {
				systemGeneratedMetadata.setDataTransferType(
						HpcDataTransferType.fromValue(metadataMap.get(DATA_TRANSFER_TYPE_ATTRIBUTE)));

			} catch (Exception e) {
				logger.error("Unable to determine data transfer type: " + metadataMap.get(DATA_TRANSFER_TYPE_ATTRIBUTE),
						e);
				systemGeneratedMetadata.setDataTransferType(null);
			}
		}

		if (metadataMap.get(DATA_TRANSFER_STARTED_ATTRIBUTE) != null) {
			systemGeneratedMetadata
					.setDataTransferStarted(toCalendar(metadataMap.get(DATA_TRANSFER_STARTED_ATTRIBUTE)));
		}
		if (metadataMap.get(DATA_TRANSFER_COMPLETED_ATTRIBUTE) != null) {
			systemGeneratedMetadata
					.setDataTransferCompleted(toCalendar(metadataMap.get(DATA_TRANSFER_COMPLETED_ATTRIBUTE)));
		}

		systemGeneratedMetadata.setSourceSize(metadataMap.get(SOURCE_FILE_SIZE_ATTRIBUTE) != null
				? Long.valueOf(metadataMap.get(SOURCE_FILE_SIZE_ATTRIBUTE))
				: null);
		systemGeneratedMetadata.setSourceURL(metadataMap.get(SOURCE_FILE_URL_ATTRIBUTE));
		systemGeneratedMetadata.setCallerObjectId(metadataMap.get(CALLER_OBJECT_ID_ATTRIBUTE));
		systemGeneratedMetadata.setChecksum(metadataMap.get(CHECKSUM_ATTRIBUTE));

		if (metadataMap.get(METADATA_UPDATED_ATTRIBUTE) != null) {
			systemGeneratedMetadata.setMetadataUpdated(toCalendar(metadataMap.get(METADATA_UPDATED_ATTRIBUTE)));
		}

		if (metadataMap.get(REGISTRATION_COMPLETION_EVENT_ATTRIBUTE) != null) {
			systemGeneratedMetadata.setRegistrationCompletionEvent(
					Boolean.valueOf(metadataMap.get(REGISTRATION_COMPLETION_EVENT_ATTRIBUTE)));
		}

		if (metadataMap.get(DEEP_ARCHIVE_STATUS_ATTRIBUTE) != null) {
			try {
				systemGeneratedMetadata.setDeepArchiveStatus(
						HpcDeepArchiveStatus.fromValue(metadataMap.get(DEEP_ARCHIVE_STATUS_ATTRIBUTE)));

			} catch (Exception e) {
				logger.error(
						"Unable to determine deep archive status: " + metadataMap.get(DEEP_ARCHIVE_STATUS_ATTRIBUTE),
						e);
			}
		}

		if (metadataMap.get(DEEP_ARCHIVE_DATE_ATTRIBUTE) != null) {
			systemGeneratedMetadata.setDeepArchiveDate(toCalendar(metadataMap.get(DEEP_ARCHIVE_DATE_ATTRIBUTE)));
		}
		
		if (metadataMap.get(DELETED_DATE_ATTRIBUTE) != null) {
			systemGeneratedMetadata.setDeletedDate(toCalendar(metadataMap.get(DELETED_DATE_ATTRIBUTE)));
		}

		HpcPathPermissions sourcePermissions = new HpcPathPermissions();
		sourcePermissions.setPermissions(metadataMap.get(SOURCE_FILE_PERMISSIONS_ATTRIBUTE));
		sourcePermissions.setOwner(metadataMap.get(SOURCE_FILE_NIH_OWNER_ATTRIBUTE));
		sourcePermissions.setGroup(metadataMap.get(SOURCE_FILE_NIH_GROUP_ATTRIBUTE));

		String userId = metadataMap.get(SOURCE_FILE_OWNER_ATTRIBUTE);
		if (!StringUtils.isEmpty(userId)) {
			sourcePermissions.setUserId(Integer.valueOf(userId));
		}
		String groupId = metadataMap.get(SOURCE_FILE_GROUP_ATTRIBUTE);
		if (!StringUtils.isEmpty(groupId)) {
			sourcePermissions.setGroupId(Integer.valueOf(groupId));
		}

		if (sourcePermissions.getPermissions() != null || sourcePermissions.getOwner() != null
				|| sourcePermissions.getGroup() != null || sourcePermissions.getUserId() != null
				|| sourcePermissions.getGroupId() != null) {
			systemGeneratedMetadata.setSourcePermissions(sourcePermissions);
		}

		return systemGeneratedMetadata;
	}

	@Override
	public List<HpcMetadataEntry> toUserProvidedMetadataEntries(List<HpcMetadataEntry> metadataEntries) {
		List<HpcMetadataEntry> userProdivedMetadataEntries = new ArrayList<>();
		if (metadataEntries != null) {
			Set<String> systemGeneratedMetadataAttributes = metadataValidator.getSystemGeneratedMetadataAttributes();
			metadataEntries.forEach(metadataEntry -> {
				if (!systemGeneratedMetadataAttributes.contains(metadataEntry.getAttribute())) {
					userProdivedMetadataEntries.add(metadataEntry);
				}
			});
		}
		return userProdivedMetadataEntries;
	}

	@Override
	public Map<String, String> toMap(List<HpcMetadataEntry> metadataEntries) {
		Map<String, String> metadataMap = new HashMap<>();
		for (HpcMetadataEntry metadataEntry : metadataEntries) {
			metadataMap.put(metadataEntry.getAttribute(), metadataEntry.getValue());
		}

		return metadataMap;
	}

	@Override
	public HpcMetadataEntries getCollectionMetadataEntries(String path) throws HpcException {
		HpcMetadataEntries metadataEntries = new HpcMetadataEntries();

		// Get the metadata associated with the collection itself.
		metadataEntries.getSelfMetadataEntries().addAll(
				dataManagementProxy.getCollectionMetadata(dataManagementAuthenticator.getAuthenticatedToken(), path));

		// Get the hierarchical metadata.
		metadataEntries.getParentMetadataEntries()
				.addAll(metadataDAO.getCollectionMetadata(dataManagementProxy.getAbsolutePath(path), 2));

		return metadataEntries;
	}

	@Override
	public Map<Integer, HpcCollectionListingEntry> getMetadataForBrowseByIds(List<Integer> ids) throws HpcException {
		Map<Integer, HpcCollectionListingEntry> map = new HashMap<>();
		List<HpcCollectionListingEntry> entries = metadataDAO.getBrowseMetadataByIds(ids);
		for (HpcCollectionListingEntry entry : entries) {
			map.put(entry.getId(), entry);
		}
		return map;
	}

	@Override
	public List<HpcMetadataEntry> getDefaultDataObjectMetadataEntries(HpcDirectoryScanItem scanItem) {
		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
		metadataEntries.add(toMetadataEntry("name", scanItem != null ? scanItem.getFileName() : "N/A"));

		return metadataEntries;
	}

	@Override
	public List<HpcMetadataEntry> getDefaultCollectionMetadataEntries() {
		return defaultCollectionMetadataEntries;
	}

	@Override
	public HpcMetadataEntry addMetadataToDataObject(String path, List<HpcMetadataEntry> metadataEntries,
			String configurationId, String collectionType) throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException(INVALID_PATH_MSG, HpcErrorType.INVALID_REQUEST_INPUT);
		} else {
			HpcDomainValidationResult validationResult = isValidMetadataEntries(metadataEntries, false);
			if (!validationResult.getValid()) {
				if (StringUtils.isEmpty(validationResult.getMessage())) {
					throw new HpcException(INVALID_METADATA_MSG, HpcErrorType.INVALID_REQUEST_INPUT);
				} else {
					throw new HpcException(validationResult.getMessage(), HpcErrorType.INVALID_REQUEST_INPUT);
				}
			}
		}

		// Validate Metadata.
		metadataValidator.validateDataObjectMetadata(configurationId, null, metadataEntries, collectionType);

		// Add Metadata to the DM system.
		dataManagementProxy.addMetadataToDataObject(dataManagementAuthenticator.getAuthenticatedToken(), path,
				metadataEntries);

		return generateIdMetadata();
	}

	@Override
	public void addMetadataToDataObjectFromFile(String path, InputStream dataObjectInputStream, String configurationId,
			String collectionType, boolean closeInputStream) throws HpcException {
		Parser parser = new AutoDetectParser();
		Metadata extractedMetadata = new Metadata();

		try {
			// Extract metadata from the file.
			parser.parse(dataObjectInputStream, new BodyContentHandler(), extractedMetadata, new ParseContext());

			// Map the Tika extracted metadata to HPC metadata entry list.
			List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
			for (String name : extractedMetadata.names()) {
				HpcMetadataEntry metadataEntry = new HpcMetadataEntry();
				metadataEntry.setAttribute(name);
				metadataEntry.setValue(extractedMetadata.get(name));
				metadataEntries.add(metadataEntry);
			}

			// Update the data object's metadata w/ the extracted entries.
			addExtractedMetadataToDataObject(path, metadataEntries, configurationId, collectionType);

		} catch (IOException | SAXException | TikaException e) {
			throw new HpcException("Failed to extract metadata from file", HpcErrorType.INVALID_REQUEST_INPUT, e);

		} finally {
			if (closeInputStream) {
				// Close the input stream if asked to.
				IOUtils.closeQuietly(dataObjectInputStream);
			}
		}
	}

	@Override
	public void addMetadataToDataObjectFromFile(String path, File dataObjectFile, String configurationId,
			String collectionType, boolean closeInputStream) throws HpcException {
		try {
			addMetadataToDataObjectFromFile(path, new FileInputStream(dataObjectFile), configurationId, collectionType,
					true);

		} catch (FileNotFoundException e) {
			throw new HpcException("File to extract metadata from not found", HpcErrorType.UNEXPECTED_ERROR, e);
		}
	}

	@Override
	public void addExtractedMetadataToDataObject(String path, List<HpcMetadataEntry> extractedMetadataEntries,
			String configurationId, String collectionType) throws HpcException {
		// Update the data object's metadata.
		updateDataObjectMetadata(path, new ArrayList<HpcMetadataEntry>(extractedMetadataEntries), configurationId,
				collectionType, true);

		// Set the extracted-metadata-attributes system generated metadata to have all
		// the attributes
		// we extracted.
		// This is done, so that we can differentiate between user-provided and
		// extracted metadata.
		StringBuffer extractedMetadataAttributes = new StringBuffer();
		extractedMetadataEntries
				.forEach(metadataEntry -> extractedMetadataAttributes.append(metadataEntry.getAttribute() + "|"));

		List<HpcMetadataEntry> systemGeneratedMetadataEntries = new ArrayList<>();
		addMetadataEntry(systemGeneratedMetadataEntries,
				toMetadataEntry(EXTRACTED_METADATA_ATTRIBUTES_ATTRIBUTE, extractedMetadataAttributes.toString()));
		dataManagementProxy.addMetadataToDataObject(dataManagementAuthenticator.getAuthenticatedToken(), path,
				systemGeneratedMetadataEntries);
	}

	@Override
	public HpcSystemGeneratedMetadata addSystemGeneratedMetadataToDataObject(String path,
			HpcMetadataEntry dataObjectIdMetadataEntry, HpcFileLocation archiveLocation, HpcFileLocation sourceLocation,
			String dataTransferRequestId, HpcDataTransferUploadStatus dataTransferStatus,
			HpcDataTransferUploadMethod dataTransferMethod, HpcDataTransferType dataTransferType,
			Calendar dataTransferStarted, Calendar dataTransferCompleted, Long sourceSize, String sourceURL,
			HpcPathPermissions sourcePermissions, String callerObjectId, String userId, String userName,
			String configurationId, String s3ArchiveConfigurationId, boolean registrationCompletionEvent)
			throws HpcException {
		// Input validation.
		if (path == null || dataTransferStatus == null || dataTransferType == null || dataTransferMethod == null
				|| dataTransferStarted == null || dataObjectIdMetadataEntry == null) {
			throw new HpcException(INVALID_PATH_MSG, HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if ((archiveLocation != null && !isValidFileLocation(archiveLocation))
				|| (sourceLocation != null && !isValidFileLocation(sourceLocation))) {
			throw new HpcException("Invalid source/archive location", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();

		// Generate a data-object ID and add it as metadata.
		metadataEntries.add(dataObjectIdMetadataEntry);

		// Create the Metadata-Updated metadata.
		metadataEntries.add(generateMetadataUpdatedMetadata());

		// Create and add the registrar ID, name and data management configuration
		// metadata.
		metadataEntries.addAll(generateRegistrarMetadata(userId, userName, configurationId));

		// Create and add the source permissions metadata.
		metadataEntries.addAll(generateSourcePermissionsMetadata(sourcePermissions, sourceLocation));

		if (sourceLocation != null) {
			// Create the source location file-container-id metadata.
			addMetadataEntry(metadataEntries,
					toMetadataEntry(SOURCE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE, sourceLocation.getFileContainerId()));

			// Create the source location file-id metadata.
			addMetadataEntry(metadataEntries,
					toMetadataEntry(SOURCE_LOCATION_FILE_ID_ATTRIBUTE, sourceLocation.getFileId()));
		}

		if (archiveLocation != null) {
			// Create the archive location file-container-id metadata.
			addMetadataEntry(metadataEntries, toMetadataEntry(ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE,
					archiveLocation.getFileContainerId()));

			// Create the archive location file-id metadata.
			addMetadataEntry(metadataEntries,
					toMetadataEntry(ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE, archiveLocation.getFileId()));
		}

		// Create the Data Transfer Request ID metadata.
		addMetadataEntry(metadataEntries, toMetadataEntry(DATA_TRANSFER_REQUEST_ID_ATTRIBUTE, dataTransferRequestId));

		// Create the Data Transfer Status metadata.
		addMetadataEntry(metadataEntries, toMetadataEntry(DATA_TRANSFER_STATUS_ATTRIBUTE, dataTransferStatus.value()));

		// Create the Data Transfer Method metadata.
		addMetadataEntry(metadataEntries, toMetadataEntry(DATA_TRANSFER_METHOD_ATTRIBUTE, dataTransferMethod.value()));

		// Create the Data Transfer Type metadata.
		addMetadataEntry(metadataEntries, toMetadataEntry(DATA_TRANSFER_TYPE_ATTRIBUTE, dataTransferType.value()));

		// Create the Data Transfer Started metadata.
		addMetadataEntry(metadataEntries,
				toMetadataEntry(DATA_TRANSFER_STARTED_ATTRIBUTE, dateFormat.format(dataTransferStarted.getTime())));

		// Create the Data Transfer Completed metadata.
		addMetadataEntry(metadataEntries, toMetadataEntry(DATA_TRANSFER_COMPLETED_ATTRIBUTE,
				dataTransferCompleted != null ? dateFormat.format(dataTransferCompleted.getTime()) : null));

		// Create the Source File Size metadata.
		addMetadataEntry(metadataEntries, toMetadataEntry(SOURCE_FILE_SIZE_ATTRIBUTE, sourceSize));

		// Create the Source File URL metadata.
		addMetadataEntry(metadataEntries, toMetadataEntry(SOURCE_FILE_URL_ATTRIBUTE, sourceURL));

		// Create the Caller Object ID metadata.
		addMetadataEntry(metadataEntries, toMetadataEntry(CALLER_OBJECT_ID_ATTRIBUTE, callerObjectId));

		// Create the Registration Completion Event Indicator metadata.
		addMetadataEntry(metadataEntries, toMetadataEntry(REGISTRATION_COMPLETION_EVENT_ATTRIBUTE,
				Boolean.toString(registrationCompletionEvent)));

		// Create the S3 Archive Configuration ID.
		addMetadataEntry(metadataEntries,
				toMetadataEntry(S3_ARCHIVE_CONFIGURATION_ID_ATTRIBUTE, s3ArchiveConfigurationId));

		// Add Metadata to the DM system.
		dataManagementProxy.addMetadataToDataObject(dataManagementAuthenticator.getAuthenticatedToken(), path,
				metadataEntries);

		return toSystemGeneratedMetadata(metadataEntries);
	}

	@Override
	public void addSystemGeneratedMetadataToDataObject(String path, HpcMetadataEntry dataObjectIdMetadataEntry,
			String userId, String userName, String configurationId, String linkSourcePath) throws HpcException {
		// Input validation.
		if (path == null || configurationId == null || linkSourcePath == null || dataObjectIdMetadataEntry == null) {
			throw new HpcException(INVALID_PATH_MSG, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();

		// Generate a data-object ID and add it as metadata.
		metadataEntries.add(dataObjectIdMetadataEntry);

		// Create the Metadata-Updated metadata.
		metadataEntries.add(generateMetadataUpdatedMetadata());

		// Create and add the registrar ID, name and data management configuration
		// metadata.
		metadataEntries.addAll(generateRegistrarMetadata(userId, userName, configurationId));

		// Create the link source path metadata..
		addMetadataEntry(metadataEntries, toMetadataEntry(LINK_SOURCE_PATH_ATTRIBUTE, linkSourcePath));

		// Add Metadata to the DM system.
		dataManagementProxy.addMetadataToDataObject(dataManagementAuthenticator.getAuthenticatedToken(), path,
				metadataEntries);
	}

	@Override
	public HpcSystemGeneratedMetadata getDataObjectSystemGeneratedMetadata(String path) throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException(INVALID_PATH_MSG, HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return toSystemGeneratedMetadata(
				dataManagementProxy.getDataObjectMetadata(dataManagementAuthenticator.getAuthenticatedToken(), path));
	}

	@Override
	public void updateDataObjectSystemGeneratedMetadata(String path, HpcFileLocation archiveLocation,
			String dataTransferRequestId, String checksum, HpcDataTransferUploadStatus dataTransferStatus,
			HpcDataTransferType dataTransferType, Calendar dataTransferStarted, Calendar dataTransferCompleted,
			Long sourceSize, String linkSourcePath, String s3ArchiveConfigurationId,
			HpcDeepArchiveStatus deepArchiveStatus, Calendar deepArchiveDate) throws HpcException {
		// Input validation.
		if (path == null || (archiveLocation != null && !isValidFileLocation(archiveLocation))) {
			throw new HpcException("Invalid updated system generated metadata for data object",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();

		if (archiveLocation != null) {
			// Update the archive location file-container-id metadata.
			addMetadataEntry(metadataEntries, toMetadataEntry(ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE,
					archiveLocation.getFileContainerId()));

			// Update the archive location file-id metadata.
			addMetadataEntry(metadataEntries,
					toMetadataEntry(ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE, archiveLocation.getFileId()));
		}

		if (dataTransferRequestId != null) {
			// Update the Data Transfer Request ID metadata.
			addMetadataEntry(metadataEntries,
					toMetadataEntry(DATA_TRANSFER_REQUEST_ID_ATTRIBUTE, dataTransferRequestId));
		}

		if (checksum != null) {
			// Update the Checksum metadata.
			addMetadataEntry(metadataEntries, toMetadataEntry(CHECKSUM_ATTRIBUTE, checksum));
		}

		if (dataTransferStatus != null) {
			// Update the Data Transfer Status metadata.
			if (dataTransferStatus.equals(HpcDataTransferUploadStatus.RECOVER_REQUESTED)) {
				metadataEntries.add(toMetadataEntry(DELETED_DATE_ATTRIBUTE, ""));
				addMetadataEntry(metadataEntries,
						toMetadataEntry(DATA_TRANSFER_STATUS_ATTRIBUTE, HpcDataTransferUploadStatus.ARCHIVED.value()));
			} else {
				addMetadataEntry(metadataEntries,
						toMetadataEntry(DATA_TRANSFER_STATUS_ATTRIBUTE, dataTransferStatus.value()));
				if(dataTransferStatus.equals(HpcDataTransferUploadStatus.DELETE_REQUESTED)) {
						addMetadataEntry(metadataEntries,
								toMetadataEntry(DELETED_DATE_ATTRIBUTE, dateFormat.format(Calendar.getInstance().getTime())));
				}
			}
		}

		if (dataTransferType != null) {
			// Update the Data Transfer Type metadata.
			addMetadataEntry(metadataEntries, toMetadataEntry(DATA_TRANSFER_TYPE_ATTRIBUTE, dataTransferType.value()));
		}

		if (dataTransferStarted != null) {
			// Update the Data Transfer Started metadata.
			addMetadataEntry(metadataEntries,
					toMetadataEntry(DATA_TRANSFER_STARTED_ATTRIBUTE, dateFormat.format(dataTransferStarted.getTime())));
		}

		if (dataTransferCompleted != null) {
			// Update the Data Transfer Completed metadata.
			addMetadataEntry(metadataEntries, toMetadataEntry(DATA_TRANSFER_COMPLETED_ATTRIBUTE,
					dateFormat.format(dataTransferCompleted.getTime())));
		}

		if (sourceSize != null) {
			// Update the Source File Size metadata.
			addMetadataEntry(metadataEntries, toMetadataEntry(SOURCE_FILE_SIZE_ATTRIBUTE, sourceSize));
		}

		if (linkSourcePath != null) {
			// Update the link source path metadata.
			addMetadataEntry(metadataEntries, toMetadataEntry(LINK_SOURCE_PATH_ATTRIBUTE, linkSourcePath));
		}

		if (s3ArchiveConfigurationId != null) {
			// Update the S3 archive configuration ID metadata.
			addMetadataEntry(metadataEntries,
					toMetadataEntry(S3_ARCHIVE_CONFIGURATION_ID_ATTRIBUTE, s3ArchiveConfigurationId));
		}

		if (deepArchiveStatus != null) {
			// Update the deep archive date metadata.
			addMetadataEntry(metadataEntries,
					toMetadataEntry(DEEP_ARCHIVE_STATUS_ATTRIBUTE, deepArchiveStatus.value()));
		}

		if (deepArchiveDate != null) {
			// Update the deep archive date metadata.
			addMetadataEntry(metadataEntries,
					toMetadataEntry(DEEP_ARCHIVE_DATE_ATTRIBUTE, dateFormat.format(deepArchiveDate.getTime())));
		}

		if (!metadataEntries.isEmpty()) {
			dataManagementProxy.updateDataObjectMetadata(dataManagementAuthenticator.getAuthenticatedToken(), path,
					metadataEntries);
		}
	}

	@Override
	public void updateDataObjectMetadata(String path, List<HpcMetadataEntry> metadataEntries, String configurationId,
			String collectionType, boolean extractedMetadata) throws HpcException {
		// Input validation.
		if (path == null) {
			throw new HpcException(INVALID_PATH_MSG, HpcErrorType.INVALID_REQUEST_INPUT);
		} else {
			HpcDomainValidationResult validationResult = isValidMetadataEntries(metadataEntries, true);
			if (!validationResult.getValid()) {
				if (StringUtils.isEmpty(validationResult.getMessage())) {
					throw new HpcException(INVALID_METADATA_MSG, HpcErrorType.INVALID_REQUEST_INPUT);
				} else {
					throw new HpcException(validationResult.getMessage(), HpcErrorType.INVALID_REQUEST_INPUT);
				}
			}
		}

		// Validate the metadata.
		metadataValidator.validateDataObjectMetadata(configurationId,
				dataManagementProxy.getDataObjectMetadata(dataManagementAuthenticator.getAuthenticatedToken(), path),
				metadataEntries, collectionType);

		// Update the 'metadata updated' system-metadata to record the time of this
		// metadata update. This is skipped for updated the data object w/ extracted
		// metadata (performed during registration).
		if (!extractedMetadata) {
			metadataEntries.add(generateMetadataUpdatedMetadata());
		}

		// Update the metadata.
		dataManagementProxy.updateDataObjectMetadata(dataManagementAuthenticator.getAuthenticatedToken(), path,
				metadataEntries);
	}

	@Override
	public HpcMetadataEntries getDataObjectMetadataEntries(String path) throws HpcException {
		HpcMetadataEntries metadataEntries = new HpcMetadataEntries();

		// Get the metadata associated with the data object itself.
		metadataEntries.getSelfMetadataEntries().addAll(
				dataManagementProxy.getDataObjectMetadata(dataManagementAuthenticator.getAuthenticatedToken(), path));

		// Get the hierarchical metadata.
		metadataEntries.getParentMetadataEntries()
				.addAll(metadataDAO.getDataObjectMetadata(dataManagementProxy.getAbsolutePath(path), 2));

		return metadataEntries;
	}

	@Override
	public HpcGroupedMetadataEntries getDataObjectGroupedMetadataEntries(String path) throws HpcException {
		HpcGroupedMetadataEntries groupedMetadataEntries = new HpcGroupedMetadataEntries();
		HpcSelfMetadataEntries selfMetadataEntries = new HpcSelfMetadataEntries();

		// Get the metadata associated with the data object itself.
		List<HpcMetadataEntry> metadataEntries = dataManagementProxy
				.getDataObjectMetadata(dataManagementAuthenticator.getAuthenticatedToken(), path);

		// Get the system metadata attributes.
		List<String> systemMetadataAttributeNames = metadataValidator
				.getDataObjectSystemGeneratedMetadataAttributeNames();

		// Get the extracted metadata attributes.

		List<String> extractedMetadataAttributeNames = Arrays
				.asList(Optional.ofNullable(toSystemGeneratedMetadata(metadataEntries).getExtractedMetadataAttributes())
						.orElse("").split("\\|"));

		// Get the metadata associated with the data object itself, and place in the
		// correct group
		metadataEntries.forEach(metadataEntry -> {
			if (systemMetadataAttributeNames.contains(metadataEntry.getAttribute())) {
				selfMetadataEntries.getSystemMetadataEntries().add(metadataEntry);
			} else if (extractedMetadataAttributeNames.contains(metadataEntry.getAttribute())) {
				selfMetadataEntries.getExtractedMetadataEntries().add(metadataEntry);
			} else {
				selfMetadataEntries.getUserMetadataEntries().add(metadataEntry);
			}
		});

		groupedMetadataEntries.setSelfMetadataEntries(selfMetadataEntries);

		// Get the hierarchical metadata.
		groupedMetadataEntries.getParentMetadataEntries()
				.addAll(metadataDAO.getDataObjectMetadata(dataManagementProxy.getAbsolutePath(path), 2));

		return groupedMetadataEntries;
	}

	@Override
	public void refreshViews() throws HpcException {
		metadataDAO.refreshViews();
	}

	@Override
	public List<String> getCollectionSystemMetadataAttributeNames() throws HpcException {
		return metadataValidator.getCollectionSystemGeneratedMetadataAttributeNames();
	}

	@Override
	public List<String> getDataObjectSystemMetadataAttributeNames() throws HpcException {
		return metadataValidator.getDataObjectSystemGeneratedMetadataAttributeNames();
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Generate ID Metadata.
	 *
	 * @return The Generated ID metadata.
	 */
	private HpcMetadataEntry generateIdMetadata() {
		return toMetadataEntry(ID_ATTRIBUTE, keyGenerator.generateKey());
	}

	/**
	 * Generate the global DME ID metadata.
	 *
	 * @param collectionId The (iRODs) collection ID.
	 * @return The Generated global DME ID metaData
	 */
	private HpcMetadataEntry generateDmeIdMetadata(int collectionId) {
		return toMetadataEntry(DME_ID_ATTRIBUTE, "NCI-DME-MS01-" + collectionId);
	}

	/**
	 * Generate Metadata Updated Metadata.
	 *
	 * @return The Generated ID metadata.
	 */
	private HpcMetadataEntry generateMetadataUpdatedMetadata() {
		return toMetadataEntry(METADATA_UPDATED_ATTRIBUTE, dateFormat.format(Calendar.getInstance().getTime()));
	}

	/**
	 * Generate registrar ID, name and DOC metadata.
	 *
	 * @param userId          The user ID.
	 * @param userName        The user name.
	 * @param configurationId The data management configuration ID..
	 * @return A List of the 3 metadata.
	 * @throws HpcException if the service invoker is unknown.
	 */
	private List<HpcMetadataEntry> generateRegistrarMetadata(String userId, String userName, String configurationId)
			throws HpcException {
		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();

		// Create the registrar user-id metadata.
		addMetadataEntry(metadataEntries, toMetadataEntry(REGISTRAR_ID_ATTRIBUTE, userId));

		// Create the registrar name metadata.
		addMetadataEntry(metadataEntries, toMetadataEntry(REGISTRAR_NAME_ATTRIBUTE, userName));

		// Create the data management configuration ID metadata.
		addMetadataEntry(metadataEntries, toMetadataEntry(CONFIGURATION_ID_ATTRIBUTE, configurationId.toString()));

		return metadataEntries;
	}

	/**
	 * Generate source permissions metadata.
	 *
	 * @param sourcePermissions The source permissions.
	 * @param sourceLocation    The source location.
	 * @return A List of the up to 7 metadata.
	 * @throws HpcException if the service invoker is unknown.
	 */
	private List<HpcMetadataEntry> generateSourcePermissionsMetadata(HpcPathPermissions sourcePermissions,
			HpcFileLocation sourceLocation) throws HpcException {
		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();

		if (sourcePermissions != null && sourceLocation != null) {
			// Create the permissions metadata.
			if (!StringUtils.isEmpty(sourcePermissions.getPermissions())) {
				addMetadataEntry(metadataEntries,
						toMetadataEntry(SOURCE_FILE_PERMISSIONS_ATTRIBUTE, sourcePermissions.getPermissions()));
			}

			// Create the (numeric) source file owner metadata.
			if (sourcePermissions.getUserId() != null) {
				addMetadataEntry(metadataEntries,
						toMetadataEntry(SOURCE_FILE_OWNER_ATTRIBUTE, sourcePermissions.getUserId().toString()));
			}

			// Find the configured DN search bases for user and group.
			HpcDistinguishedNameSearch distinguishedNameSearch = Optional
					.ofNullable(securityService.findDistinguishedNameSearch(sourceLocation.getFileId()))
					.orElse(new HpcDistinguishedNameSearch());

			String owner = sourcePermissions.getOwner();
			if (sourcePermissions.getUserId() != null
					&& !StringUtils.isEmpty(distinguishedNameSearch.getUserSearchBase())) {
				// Search LDAP for user distinguished names and create metadata if found.
				HpcDistinguishedNameSearchResult dnSearchResult = securityService.getUserDistinguishedName(
						String.valueOf(sourcePermissions.getUserId()), distinguishedNameSearch.getUserSearchBase());
				if (!StringUtils.isEmpty(dnSearchResult.getDistinguishedName())) {
					addMetadataEntry(metadataEntries,
							toMetadataEntry(SOURCE_FILE_USER_DN_ATTRIBUTE, dnSearchResult.getDistinguishedName()));
				}
				if (!StringUtils.isEmpty(dnSearchResult.getNihDistinguishedName())) {
					addMetadataEntry(metadataEntries, toMetadataEntry(SOURCE_FILE_NIH_USER_DN_ATTRIBUTE,
							dnSearchResult.getNihDistinguishedName()));
				}
				if (!StringUtils.isEmpty(dnSearchResult.getNihCommonName())) {
					owner = dnSearchResult.getNihCommonName();
				}
			}

			// Create the source owner metadata.
			if (!StringUtils.isEmpty(owner)) {
				addMetadataEntry(metadataEntries, toMetadataEntry(SOURCE_FILE_NIH_OWNER_ATTRIBUTE, owner));
			}

			// Create the (numeric) source file group metadata.
			if (sourcePermissions.getGroupId() != null) {
				addMetadataEntry(metadataEntries,
						toMetadataEntry(SOURCE_FILE_GROUP_ATTRIBUTE, sourcePermissions.getGroupId().toString()));
			}

			String group = sourcePermissions.getGroup();
			if (sourcePermissions.getGroupId() != null
					&& !StringUtils.isEmpty(distinguishedNameSearch.getGroupSearchBase())) {
				// Search LDAP for group distinguished names and create metadata if found.
				HpcDistinguishedNameSearchResult dnSearchResult = securityService.getGroupDistinguishedName(
						String.valueOf(sourcePermissions.getGroupId()), distinguishedNameSearch.getGroupSearchBase());
				if (!StringUtils.isEmpty(dnSearchResult.getDistinguishedName())) {
					addMetadataEntry(metadataEntries,
							toMetadataEntry(SOURCE_FILE_GROUP_DN_ATTRIBUTE, dnSearchResult.getDistinguishedName()));
				}
				if (!StringUtils.isEmpty(dnSearchResult.getNihDistinguishedName())) {
					addMetadataEntry(metadataEntries, toMetadataEntry(SOURCE_FILE_GROUP_NIH_DN_ATTRIBUTE,
							dnSearchResult.getNihDistinguishedName()));
				}
				if (!StringUtils.isEmpty(dnSearchResult.getNihCommonName())) {
					group = dnSearchResult.getNihCommonName();
				}
			}

			// Create the source group metadata.
			if (!StringUtils.isEmpty(group)) {
				addMetadataEntry(metadataEntries, toMetadataEntry(SOURCE_FILE_NIH_GROUP_ATTRIBUTE, group));
			}
		}

		return metadataEntries;
	}

	/**
	 * Add a metadata entry to a list.
	 *
	 * @param metadataEntries list of metadata entries.
	 * @param entry           A metadata entry.
	 */
	private void addMetadataEntry(List<HpcMetadataEntry> metadataEntries, HpcMetadataEntry entry) {
		if (entry.getAttribute() != null && (entry.getValue() != null && !entry.getValue().isEmpty())) {
			metadataEntries.add(entry);
		}
	}

	/**
	 * Generate a metadata entry from attribute/value pair.
	 *
	 * @param attribute The metadata entry attribute.
	 * @param value     The metadata entry value.
	 * @return The metadata entry.
	 */
	private HpcMetadataEntry toMetadataEntry(String attribute, String value) {
		HpcMetadataEntry entry = new HpcMetadataEntry();
		entry.setAttribute(attribute);
		entry.setValue(value);
		entry.setUnit("");
		return entry;
	}

	/**
	 * Generate a metadata entry from attribute/value pair.
	 *
	 * @param attribute The metadata entry attribute.
	 * @param value     The metadata entry value.
	 * @return HpcMetadataEntry instance
	 */
	private HpcMetadataEntry toMetadataEntry(String attribute, Long value) {
		return toMetadataEntry(attribute, value != null ? String.valueOf(value) : null);
	}

	/**
	 * Validate that collection type is not updated
	 *
	 * @param existingMetadataEntries Existing collection metadata entries.
	 * @param metadataEntries         Updated collection metadata entries.
	 * @throws HpcException If the user tries to update the collection type
	 *                      metadata.
	 */
	private void validateCollectionTypeUpdate(List<HpcMetadataEntry> existingMetadataEntries,
			List<HpcMetadataEntry> metadataEntries) throws HpcException {
		// Get the current collection type.
		String collectionType = null;
		for (HpcMetadataEntry metadataEntry : existingMetadataEntries) {
			if (metadataEntry.getAttribute().equals(HpcMetadataValidator.COLLECTION_TYPE_ATTRIBUTE)) {
				collectionType = metadataEntry.getValue();
				break;
			}
		}

		// Validate it's not getting updated.
		if (collectionType == null) {
			return;
		}

		for (HpcMetadataEntry metadataEntry : metadataEntries) {
			if (metadataEntry.getAttribute().equals(HpcMetadataValidator.COLLECTION_TYPE_ATTRIBUTE)) {
				if (!metadataEntry.getValue().equals(collectionType)) {
					throw new HpcException("Collection type can't be updated", HpcErrorType.INVALID_REQUEST_INPUT);
				}
				break;
			}
		}
	}

	/**
	 * Instantiate a Calendar object from string.
	 *
	 * @param calendarStr The calendar as a string.
	 * @return The Calendar instance.
	 */
	private Calendar toCalendar(String calendarStr) {
		Calendar cal = Calendar.getInstance();
		try {
			cal.setTime(dateFormat.parse(calendarStr));

		} catch (ParseException e) {
			logger.error("Failed to parse calendar string: " + calendarStr);
			return null;
		}

		return cal;
	}
}
