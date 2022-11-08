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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.collect.Iterables;

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

	// A configured ID representing the server performing a migration task.
	@Value("${hpc.service.serverId}")
	private String serverId = null;

	// A list of servers running the data migration scheduled tasks
	@Value("${hpc.service.dataMigration.serverIds}")
	private String dataMigrationServerIds = null;

	// A cycle iterator (round robin) of data migration server IDs to assign
	// migration tasks to.
	private Iterator<String> dataMigrationServerIdCycleIter = null;

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
			String fromS3ArchiveConfigurationId, String toS3ArchiveConfigurationId, String collectionMigrationTaskId,
			boolean alignArchivePath) throws HpcException {
		// Check if a task already exist.
		HpcDataMigrationTask migrationTask = dataMigrationDAO.getDataObjectMigrationTask(collectionMigrationTaskId,
				path);
		if (migrationTask != null) {
			return migrationTask;
		}

		// Check if the task already completed.
		migrationTask = new HpcDataMigrationTask();
		String taskId = dataMigrationDAO.getDataObjectMigrationTaskResultId(collectionMigrationTaskId, path);
		if (!StringUtils.isEmpty(taskId)) {
			migrationTask.setId(taskId);
			return migrationTask;
		}

		// Create and persist a migration task.
		migrationTask.setPath(path);
		migrationTask.setUserId(userId);
		migrationTask.setConfigurationId(configurationId);
		migrationTask.setFromS3ArchiveConfigurationId(fromS3ArchiveConfigurationId);
		migrationTask.setToS3ArchiveConfigurationId(toS3ArchiveConfigurationId);
		migrationTask.setCreated(Calendar.getInstance());
		migrationTask.setStatus(HpcDataMigrationStatus.RECEIVED);
		migrationTask.setType(HpcDataMigrationType.DATA_OBJECT);
		migrationTask.setParentId(collectionMigrationTaskId);
		migrationTask.setAlignArchivePath(alignArchivePath);

		// Persist the task.
		dataMigrationDAO.upsertDataMigrationTask(migrationTask);
		return migrationTask;
	}

	@Override
	public List<HpcDataMigrationTask> getDataMigrationTasks(HpcDataMigrationStatus status, HpcDataMigrationType type)
			throws HpcException {

		List<HpcDataMigrationTask> dataMigrationTasks = dataMigrationDAO.getDataMigrationTasks(status, type, serverId);
		logger.info("{} Data Migration Tasks retrieved for [{}, {}, {}]", dataMigrationTasks.size(), status, type,
				serverId);

		return dataMigrationTasks;
	}

	@Override
	public void assignDataMigrationTasks() throws HpcException {
		for (HpcDataMigrationTask dataMigrationTask : dataMigrationDAO.getUnassignedDataMigrationTasks()) {
			String assignedServerId = dataMigrationServerIdCycleIter.next();
			dataMigrationDAO.setDataMigrationTaskServerId(dataMigrationTask.getId(), assignedServerId);

			logger.info("Data migration: task - {} assigned to {}", dataMigrationTask.getId(), assignedServerId);
		}
	}

	@Override
	public void migrateDataObject(HpcDataMigrationTask dataObjectMigrationTask) throws HpcException {
		if (!dataObjectMigrationTask.getAlignArchivePath() && dataObjectMigrationTask.getToS3ArchiveConfigurationId()
				.equals(dataObjectMigrationTask.getFromS3ArchiveConfigurationId())) {
			// Migration not needed.
			completeDataObjectMigrationTask(dataObjectMigrationTask, HpcDataMigrationResult.IGNORED, null, null, null);
			return;
		}

		// Get the data transfer configuration for both source and target S3 archives in
		// this migration task. Note that it's the same in the case of archive path
		// alignment
		HpcDataTransferConfiguration fromS3ArchiveDataTransferConfiguration = dataManagementConfigurationLocator
				.getDataTransferConfiguration(dataObjectMigrationTask.getConfigurationId(),
						dataObjectMigrationTask.getFromS3ArchiveConfigurationId(), HpcDataTransferType.S_3);
		if (StringUtils.isEmpty(dataObjectMigrationTask.getFromS3ArchiveConfigurationId())) {
			dataObjectMigrationTask.setFromS3ArchiveConfigurationId(fromS3ArchiveDataTransferConfiguration.getId());
		}

		HpcDataTransferConfiguration toS3ArchiveDataTransferConfiguration = !dataObjectMigrationTask
				.getAlignArchivePath()
						? dataManagementConfigurationLocator.getDataTransferConfiguration(
								dataObjectMigrationTask.getConfigurationId(),
								dataObjectMigrationTask.getToS3ArchiveConfigurationId(), HpcDataTransferType.S_3)
						: fromS3ArchiveDataTransferConfiguration;

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
		Object toS3ArchiveAuthToken = !dataObjectMigrationTask.getAlignArchivePath() ? s3DataTransferProxy.authenticate(
				toS3ArchiveDataTransferSystemAccount, toS3ArchiveDataTransferConfiguration.getUrlOrRegion(),
				toS3ArchiveDataTransferConfiguration.getEncryptionAlgorithm(),
				toS3ArchiveDataTransferConfiguration.getEncryptionKey()) : fromS3ArchiveAuthToken;

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
				dataObjectMigrationTask, this, fromS3ArchiveAuthToken, toS3ArchiveAuthToken);
		dataObjectMigrationTask.setToS3ArchiveLocation(s3DataTransferProxy.uploadDataObject(toS3ArchiveAuthToken,
				uploadRequest, toS3ArchiveDataTransferConfiguration.getBaseArchiveDestination(),
				toS3ArchiveDataTransferConfiguration.getUploadRequestURLExpiration(), progressListener,
				dataTransferService.generateArchiveMetadata(dataObjectMigrationTask.getConfigurationId(),
						dataObjectMigrationTask.getDataObjectId(), dataObjectMigrationTask.getRegistrarId()),
				toS3ArchiveDataTransferConfiguration.getEncryptedTransfer(),
				toS3ArchiveDataTransferConfiguration.getStorageClass()).getArchiveLocation());

		logger.info("Archive migration started: {} - {}:{} -> {}:{}", dataObjectMigrationTask.getId(),
				dataObjectMigrationTask.getFromS3ArchiveLocation().getFileContainerId(),
				dataObjectMigrationTask.getFromS3ArchiveLocation().getFileId(),
				dataObjectMigrationTask.getToS3ArchiveLocation().getFileContainerId(),
				dataObjectMigrationTask.getToS3ArchiveLocation().getFileId());

		// Update the task and persist.
		dataObjectMigrationTask.setStatus(HpcDataMigrationStatus.IN_PROGRESS);
		dataMigrationDAO.upsertDataMigrationTask(dataObjectMigrationTask);
	}

	@Override
	public void completeDataObjectMigrationTask(HpcDataMigrationTask dataObjectMigrationTask,
			HpcDataMigrationResult result, String message, Object fromS3ArchiveAuthToken, Object toS3ArchiveAuthToken)
			throws HpcException {
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
				Calendar deepArchiveDate = deepArchiveStatus != null ? Calendar.getInstance() : null;
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

		// Shutdown the transfer managers.
		try {
			if (fromS3ArchiveAuthToken != null) {
				s3DataTransferProxy.shutdown(fromS3ArchiveAuthToken);
			}
			if (toS3ArchiveAuthToken != null) {
				s3DataTransferProxy.shutdown(toS3ArchiveAuthToken);
			}
		} catch (HpcException e) {
			logger.error("Failed to shutdown TransferManager", e);
		}
	}

	@Override
	public void completeBulkMigrationTask(HpcDataMigrationTask bulkMigrationTask, String message) throws HpcException {
		// Determine the bulk migration result.
		HpcDataMigrationResult result = HpcDataMigrationResult.COMPLETED;
		if (!StringUtils.isEmpty(message)) {
			result = HpcDataMigrationResult.FAILED;
		} else if (!dataMigrationDAO.getDataObjectMigrationTasks(bulkMigrationTask.getId()).isEmpty()) {
			// bulk migration task still in progress. At least one data object
			// migration task is still active.
			logger.info("Bulk migration task still in progress: {} - {}", bulkMigrationTask.getId(),
					bulkMigrationTask.getType());
			return;
		} else {
			// Get a map of result counts for data object migrations that are associated w/
			// this bulk migration.
			Map<HpcDataMigrationResult, Integer> resultsCount = dataMigrationDAO
					.getCollectionMigrationResultCount(bulkMigrationTask.getId());

			if (Optional.ofNullable(resultsCount.get(HpcDataMigrationResult.FAILED)).orElse(0) > 0) {
				// All data object migration tasks under this bulk migration task
				// completed, but at least one failed.
				result = HpcDataMigrationResult.FAILED;
			} else if (Optional.ofNullable(resultsCount.get(HpcDataMigrationResult.IGNORED)).orElse(0) > 0) {
				// All data object migration tasks under this bulk migration task
				// completed, but at least one ignored.
				result = HpcDataMigrationResult.COMPLETED_IGNORED_ITEMS;
			}
		}

		// Delete the task and insert a result record.
		dataMigrationDAO.deleteDataMigrationTask(bulkMigrationTask.getId());
		dataMigrationDAO.upsertDataMigrationTaskResult(bulkMigrationTask, Calendar.getInstance(), result, message);
	}

	@Override
	public void resetMigrationTasksInProcess() throws HpcException {
		// If needed, re-setting the cycle iterator of migration server IDs.
		if (dataMigrationServerIdCycleIter == null) {
			if (StringUtils.isEmpty(dataMigrationServerIds)) {
				throw new HpcException(
						"No migration servers are configured. Check hpc.service.dataMigration.serverIds property",
						HpcErrorType.SPRING_CONFIGURATION_ERROR);
			}
			String[] serverIds = dataMigrationServerIds.split(",");
			logger.info("{} Data migration servers configured: {}", serverIds.length, dataMigrationServerIds);
			dataMigrationServerIdCycleIter = Iterables.cycle(serverIds).iterator();
		}

		dataMigrationDAO.setDataMigrationTasksStatus(HpcDataMigrationStatus.IN_PROGRESS, false,
				HpcDataMigrationStatus.RECEIVED);
		dataMigrationDAO.setDataMigrationTasksStatus(HpcDataMigrationStatus.RECEIVED, false,
				HpcDataMigrationStatus.RECEIVED);
	}

	@Override
	public HpcDataMigrationTask createCollectionMigrationTask(String path, String userId, String configurationId,
			String toS3ArchiveConfigurationId, boolean alignArchivePath) throws HpcException {
		// Create and persist a migration task.
		HpcDataMigrationTask migrationTask = new HpcDataMigrationTask();
		migrationTask.setPath(path);
		migrationTask.setUserId(userId);
		migrationTask.setConfigurationId(configurationId);
		migrationTask.setToS3ArchiveConfigurationId(toS3ArchiveConfigurationId);
		migrationTask.setCreated(Calendar.getInstance());
		migrationTask.setStatus(HpcDataMigrationStatus.RECEIVED);
		migrationTask.setType(HpcDataMigrationType.COLLECTION);
		migrationTask.setAlignArchivePath(alignArchivePath);

		// Persist the task.
		dataMigrationDAO.upsertDataMigrationTask(migrationTask);
		return migrationTask;
	}

	@Override
	public HpcDataMigrationTask createDataObjectsMigrationTask(List<String> dataObjectPaths, String userId,
			String configurationId, String toS3ArchiveConfigurationId) throws HpcException {
		// Create and persist a migration task.
		HpcDataMigrationTask migrationTask = new HpcDataMigrationTask();
		migrationTask.getDataObjectPaths().addAll(dataObjectPaths);
		migrationTask.setUserId(userId);
		migrationTask.setConfigurationId(configurationId);
		migrationTask.setToS3ArchiveConfigurationId(toS3ArchiveConfigurationId);
		migrationTask.setCreated(Calendar.getInstance());
		migrationTask.setStatus(HpcDataMigrationStatus.RECEIVED);
		migrationTask.setType(HpcDataMigrationType.DATA_OBJECT_LIST);
		migrationTask.setAlignArchivePath(false);

		// Persist the task.
		dataMigrationDAO.upsertDataMigrationTask(migrationTask);
		return migrationTask;
	}

	@Override
	public HpcDataMigrationTask createCollectionsMigrationTask(List<String> collectionPaths, String userId,
			String configurationId, String toS3ArchiveConfigurationId) throws HpcException {
		// Create and persist a migration task.
		HpcDataMigrationTask migrationTask = new HpcDataMigrationTask();
		migrationTask.getCollectionPaths().addAll(collectionPaths);
		migrationTask.setUserId(userId);
		migrationTask.setConfigurationId(configurationId);
		migrationTask.setToS3ArchiveConfigurationId(toS3ArchiveConfigurationId);
		migrationTask.setCreated(Calendar.getInstance());
		migrationTask.setStatus(HpcDataMigrationStatus.RECEIVED);
		migrationTask.setType(HpcDataMigrationType.COLLECTION_LIST);
		migrationTask.setAlignArchivePath(false);

		// Persist the task.
		dataMigrationDAO.upsertDataMigrationTask(migrationTask);
		return migrationTask;
	}

	@Override
	public void updateDataMigrationTask(HpcDataMigrationTask dataMigrationTask) throws HpcException {
		dataMigrationDAO.upsertDataMigrationTask(dataMigrationTask);
	}

	@Override
	public boolean markInProcess(HpcDataMigrationTask dataObjectMigrationTask, boolean inProcess) throws HpcException {
		// Only set in-process to true if this task in a RECEIVED status, and the
		// in-process not already true.
		boolean updated = true;

		if (!inProcess || (!dataObjectMigrationTask.getInProcess()
				&& dataObjectMigrationTask.getStatus().equals(HpcDataMigrationStatus.RECEIVED))) {
			updated = dataMigrationDAO.setDataMigrationTaskInProcess(dataObjectMigrationTask.getId(), inProcess);
		}

		if (updated) {
			dataObjectMigrationTask.setInProcess(inProcess);
		}

		return updated;
	}
}
