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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import gov.nih.nci.hpc.dao.HpcDataMigrationDAO;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationStatus;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationTask;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.service.HpcDataMigrationService;

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
			String fromS3ArchiveConfigurationId, String toS3ArchiveConfigurationId) throws HpcException {
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
		if (s3DataTransferProxy != null) {
			logger.error("ERAN: yes");
		} else {
			logger.error("ERAN: no");
		}
	}
}
