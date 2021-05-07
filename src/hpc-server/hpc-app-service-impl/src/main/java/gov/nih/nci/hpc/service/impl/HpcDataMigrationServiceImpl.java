/**
 * HpcDataSearchServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import gov.nih.nci.hpc.dao.HpcDataMigrationDAO;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationResult;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationStatus;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationType;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveObjectMetadata;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDeepArchiveStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcStreamingUploadSource;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcDataMigrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.service.HpcDataMigrationService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * HPC Data Search Application Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataMigrationServiceImpl implements HpcDataMigrationService {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// Data Migration DAO.
	@Autowired
	private HpcDataMigrationDAO dataMigrationDAO = null;

	// S3 data transfer proxy.
	@Autowired
	@Qualifier("hpcS3DataTransferProxy")
	private HpcDataTransferProxy s3DataTransferProxy = null;

	// System Accounts locator.
	@Autowired
	private HpcSystemAccountLocator systemAccountLocator = null;

	// Data management configuration locator.
	@Autowired
	private HpcDataManagementConfigurationLocator dataManagementConfigurationLocator = null;

	// The Metadata Application Service Instance.
	@Autowired
	private HpcMetadataService metadataService = null;

	// The Data Transfer Application Service Instance.
	@Autowired
	private HpcDataTransferService dataTransferService = null;

	// The Security Application Service Instance.
	@Autowired
	private HpcSecurityService securityService = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Default Constructor for Spring Dependency Injection.
	 *
	 * @throws HpcException Constructor is disabled.
	 */
	private HpcDataMigrationServiceImpl() throws HpcException {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataMigrationService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public HpcDataMigrationTask createDataObjectMigrationTask(String path, String userId, String configurationId,
			String fromS3ArchiveConfigurationId, String toS3ArchiveConfigurationId, String collectionMigrationTaskId)
			throws HpcException {
		// Create and persist a migration task.
		HpcDataMigrationTask migrationTask = new HpcDataMigrationTask();
		migrationTask.setPath(path);
		migrationTask.setUserId(userId);
		migrationTask.setConfigurationId(configurationId);
		migrationTask.setFromS3ArchiveConfigurationId(fromS3ArchiveConfigurationId);
		migrationTask.setToS3ArchiveConfigurationId(toS3ArchiveConfigurationId);
		migrationTask.setCreated(Calendar.getInstance());
		migrationTask.setStatus(HpcDataMigrationStatus.RECEIVED);
		migrationTask.setType(HpcDataMigrationType.DATA_OBJECT);
		migrationTask.setParentId(collectionMigrationTaskId);

		// Persist the task.
		dataMigrationDAO.upsertDataMigrationTask(migrationTask);
		return migrationTask;
	}

	@Override
	public List<HpcDataMigrationTask> getDataMigrationTasks(HpcDataMigrationStatus status, HpcDataMigrationType type)
			throws HpcException {
		return dataMigrationDAO.getDataMigrationTasks(status, type);
	}

	@Override
	public void migrateDataObject(HpcDataMigrationTask dataObjectMigrationTask) throws HpcException {
		if (dataObjectMigrationTask.getToS3ArchiveConfigurationId()
				.equals(dataObjectMigrationTask.getFromS3ArchiveConfigurationId())) {
			// Migration not needed.
			completeDataObjectMigrationTask(dataObjectMigrationTask, HpcDataMigrationResult.IGNORED, null);
			return;
		}

		// Get the data transfer configuration for both source and target S3 archives in
		// this migration task.
		HpcDataTransferConfiguration fromS3ArchiveDataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(dataObjectMigrationTask.getConfigurationId(),
						dataObjectMigrationTask.getFromS3ArchiveConfigurationId(), HpcDataTransferType.S_3);
		if (StringUtils.isEmpty(dataObjectMigrationTask.getFromS3ArchiveConfigurationId())) {
			dataObjectMigrationTask.setFromS3ArchiveConfigurationId(fromS3ArchiveDataTransferConfiguration.getId());
		}

		HpcDataTransferConfiguration toS3ArchiveDataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(dataObjectMigrationTask.getConfigurationId(),
						dataObjectMigrationTask.getToS3ArchiveConfigurationId(), HpcDataTransferType.S_3);

		// Get the data transfer system accounts to access both source and target
		// S3 archives in
		// this migration task.
		HpcIntegratedSystemAccount fromS3ArchiveDataTransferSystemAccount = systemAccountLocator
				.getSystemAccount(fromS3ArchiveDataTransferConfiguration.getArchiveProvider());
		HpcIntegratedSystemAccount toS3ArchiveDataTransferSystemAccount = systemAccountLocator
				.getSystemAccount(toS3ArchiveDataTransferConfiguration.getArchiveProvider());

		// Obtain authenticated tokens to both source/target archives of this migration
		// task.
		Object fromS3ArchiveAuthToken = s3DataTransferProxy.authenticate(fromS3ArchiveDataTransferSystemAccount,
				fromS3ArchiveDataTransferConfiguration.getUrlOrRegion(),
				fromS3ArchiveDataTransferConfiguration.getEncryptionAlgorithm(),
				fromS3ArchiveDataTransferConfiguration.getEncryptionKey());
		Object toS3ArchiveAuthToken = s3DataTransferProxy.authenticate(toS3ArchiveDataTransferSystemAccount,
				toS3ArchiveDataTransferConfiguration.getUrlOrRegion(),
				toS3ArchiveDataTransferConfiguration.getEncryptionAlgorithm(),
				toS3ArchiveDataTransferConfiguration.getEncryptionKey());

		// Get the System generated metadata of the data object in migration.
		HpcSystemGeneratedMetadata metadata = metadataService
				.getDataObjectSystemGeneratedMetadata(dataObjectMigrationTask.getPath());
		dataObjectMigrationTask.setDataObjectId(metadata.getObjectId());
		dataObjectMigrationTask.setRegistrarId(metadata.getRegistrarId());
		dataObjectMigrationTask.setFromS3ArchiveLocation(metadata.getArchiveLocation());

		// Generate a URL to the data object in archive we are migrating from.
		String sourceURL = s3DataTransferProxy.generateDownloadRequestURL(fromS3ArchiveAuthToken,
				metadata.getArchiveLocation(), fromS3ArchiveDataTransferConfiguration.getBaseArchiveDestination(),
				fromS3ArchiveDataTransferConfiguration.getUploadRequestURLExpiration());

		// Create an S3 upload source object out of the data object in the S3 archive we
		// are migrating from.
		HpcStreamingUploadSource uploadSource = new HpcStreamingUploadSource();
		uploadSource.setSourceURL(sourceURL);
		uploadSource.setSourceSize(metadata.getSourceSize());
		uploadSource.setSourceLocation(metadata.getArchiveLocation());

		// Create an upload request to the S3 archive we are migrating into
		HpcDataObjectUploadRequest uploadRequest = new HpcDataObjectUploadRequest();
		uploadRequest.setPath(dataObjectMigrationTask.getPath());
		uploadRequest.setCallerObjectId(metadata.getCallerObjectId());
		uploadRequest.setDataObjectId(metadata.getObjectId());
		uploadRequest.setSourceSize(metadata.getSourceSize());
		uploadRequest.setUserId(metadata.getRegistrarId());
		uploadRequest.setS3UploadSource(uploadSource);

		// Upload the data object into the S3 archive we are migrating to, and update
		// the task w/ new archive location.
		HpcDataMigrationProgressListener progressListener = new HpcDataMigrationProgressListener(
				dataObjectMigrationTask, this);
		dataObjectMigrationTask.setToS3ArchiveLocation(s3DataTransferProxy.uploadDataObject(toS3ArchiveAuthToken,
				uploadRequest, toS3ArchiveDataTransferConfiguration.getBaseArchiveDestination(),
				toS3ArchiveDataTransferConfiguration.getUploadRequestURLExpiration(), progressListener, null,
				toS3ArchiveDataTransferConfiguration.getEncryptedTransfer(),
				toS3ArchiveDataTransferConfiguration.getStorageClass()).getArchiveLocation());

		// Update the task and persist.
		dataObjectMigrationTask.setStatus(HpcDataMigrationStatus.IN_PROGRESS);
		dataMigrationDAO.upsertDataMigrationTask(dataObjectMigrationTask);
	}

	@Override
	public void completeDataObjectMigrationTask(HpcDataMigrationTask dataObjectMigrationTask,
			HpcDataMigrationResult result, String message) throws HpcException {
		if (!dataObjectMigrationTask.getType().equals(HpcDataMigrationType.DATA_OBJECT)) {
			throw new HpcException("Migration type mismatch", HpcErrorType.UNEXPECTED_ERROR);
		}

		if (result.equals(HpcDataMigrationResult.COMPLETED)) {
			try {
				// Add metadata to the object in the target S3 archive.
				HpcArchiveObjectMetadata objectMetadata = dataTransferService.addSystemGeneratedMetadataToDataObject(
						dataObjectMigrationTask.getToS3ArchiveLocation(), HpcDataTransferType.S_3,
						dataObjectMigrationTask.getConfigurationId(),
						dataObjectMigrationTask.getToS3ArchiveConfigurationId(),
						dataObjectMigrationTask.getDataObjectId(), dataObjectMigrationTask.getRegistrarId());

				// Update the system metadata w/ the new S3 archive id, location and
				// deep-archive status/date after migration.
				String checksum = objectMetadata.getChecksum();
				HpcDeepArchiveStatus deepArchiveStatus = objectMetadata.getDeepArchiveStatus();
				Calendar deepArchiveDate = deepArchiveStatus != null
						&& deepArchiveStatus.equals(HpcDeepArchiveStatus.IN_PROGRESS) ? Calendar.getInstance() : null;
				securityService.executeAsSystemAccount(Optional.empty(),
						() -> metadataService.updateDataObjectSystemGeneratedMetadata(dataObjectMigrationTask.getPath(),
								dataObjectMigrationTask.getToS3ArchiveLocation(), null, checksum, null, null, null,
								null, null, null, dataObjectMigrationTask.getToS3ArchiveConfigurationId(),
								deepArchiveStatus, deepArchiveDate));

				// Delete the data object from the source S3 archive.
				dataTransferService.deleteDataObject(dataObjectMigrationTask.getFromS3ArchiveLocation(),
						HpcDataTransferType.S_3, dataObjectMigrationTask.getConfigurationId(),
						dataObjectMigrationTask.getFromS3ArchiveConfigurationId());

			} catch (HpcException e) {
				message = "Failed to complete data migration for task: " + dataObjectMigrationTask.getId() + " ["
						+ e.getMessage() + " ] - " + dataObjectMigrationTask.getPath();
				logger.error(message, e);
				result = HpcDataMigrationResult.FAILED;
			}
		}

		// Delete the task and insert a result record.
		dataMigrationDAO.deleteDataMigrationTask(dataObjectMigrationTask.getId());
		dataMigrationDAO.upsertDataMigrationTaskResult(dataObjectMigrationTask, Calendar.getInstance(), result,
				message);
	}

	@Override
	public void completeCollectionMigrationTask(HpcDataMigrationTask collectionMigrationTask, String message)
			throws HpcException {
		if (!collectionMigrationTask.getType().equals(HpcDataMigrationType.COLLECTION)) {
			throw new HpcException("Migration type mismatch", HpcErrorType.UNEXPECTED_ERROR);
		}

		// Determine the collection migration result.
		HpcDataMigrationResult result = HpcDataMigrationResult.COMPLETED;
		if (!StringUtils.isEmpty(message)) {
			result = HpcDataMigrationResult.FAILED;
		} else if (!dataMigrationDAO.getDataObjectMigrationTasks(collectionMigrationTask.getId()).isEmpty()) {
			// Collection migration task still in progress. At least one data object
			// migration task is still active.
			logger.info("Collection migration task still in progress: {}", collectionMigrationTask.getId());
			return;
		} else {
			// Get a map of result counts for data object migrations that are associated w/
			// this collection migration.
			Map<HpcDataMigrationResult, Integer> resultsCount = dataMigrationDAO
					.getCollectionMigrationResultCount(collectionMigrationTask.getId());

			if (Optional.ofNullable(resultsCount.get(HpcDataMigrationResult.FAILED)).orElse(0) > 0) {
				// All data object migration tasks under this collection migration task
				// completed, but at least one failed.
				result = HpcDataMigrationResult.FAILED;
			} else if (Optional.ofNullable(resultsCount.get(HpcDataMigrationResult.IGNORED)).orElse(0) > 0) {
				// All data object migration tasks under this collection migration task
				// completed, but at least one failed.
				result = HpcDataMigrationResult.COMPLETED_IGNORED_ITEMS;
			}
		}

		// Delete the task and insert a result record.
		dataMigrationDAO.deleteDataMigrationTask(collectionMigrationTask.getId());
		dataMigrationDAO.upsertDataMigrationTaskResult(collectionMigrationTask, Calendar.getInstance(), result,
				message);
	}

	@Override
	public void resetMigrationTasksInProcess() throws HpcException {
		dataMigrationDAO.setDataMigrationTasksStatus(HpcDataMigrationStatus.IN_PROGRESS,
				HpcDataMigrationStatus.RECEIVED);
	}

	@Override
	public HpcDataMigrationTask createCollectionMigrationTask(String path, String userId, String configurationId,
			String toS3ArchiveConfigurationId) throws HpcException {
		// Create and persist a migration task.
		HpcDataMigrationTask migrationTask = new HpcDataMigrationTask();
		migrationTask.setPath(path);
		migrationTask.setUserId(userId);
		migrationTask.setConfigurationId(configurationId);
		migrationTask.setToS3ArchiveConfigurationId(toS3ArchiveConfigurationId);
		migrationTask.setCreated(Calendar.getInstance());
		migrationTask.setStatus(HpcDataMigrationStatus.RECEIVED);
		migrationTask.setType(HpcDataMigrationType.COLLECTION);

		// Persist the task.
		dataMigrationDAO.upsertDataMigrationTask(migrationTask);
		return migrationTask;
	}

	@Override
	public void updateDataMigrationTask(HpcDataMigrationTask dataMigrationTask) throws HpcException {
		dataMigrationDAO.upsertDataMigrationTask(dataMigrationTask);
	}
}
