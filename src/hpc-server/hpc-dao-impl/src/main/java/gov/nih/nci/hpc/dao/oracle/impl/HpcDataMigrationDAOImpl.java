/**
 * HpcDataMigrationDAOImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao.oracle.impl;

import java.util.AbstractMap;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import gov.nih.nci.hpc.dao.HpcDataMigrationDAO;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationResult;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationStatus;
import gov.nih.nci.hpc.domain.datamigration.HpcDataMigrationType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcDataMigrationTask;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Migration DAO Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataMigrationDAOImpl implements HpcDataMigrationDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String UPSERT_DATA_MIGRATION_TASK_SQL = "merge into HPC_DATA_MIGRATION_TASK using dual on (ID = ?) "
			+ "when matched then update set PARENT_ID = ?, USER_ID = ?, PATH = ?, CONFIGURATION_ID = ?, FROM_S3_ARCHIVE_CONFIGURATION_ID = ?, "
			+ "TO_S3_ARCHIVE_CONFIGURATION_ID = ?, TYPE = ?, STATUS = ?, CREATED = ? "
			+ "when not matched then insert (ID, PARENT_ID, USER_ID, PATH, CONFIGURATION_ID, FROM_S3_ARCHIVE_CONFIGURATION_ID, "
			+ "TO_S3_ARCHIVE_CONFIGURATION_ID, TYPE, STATUS, CREATED) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

	private static final String UPSERT_DATA_MIGRATION_TASK_RESULT_SQL = "merge into HPC_DATA_MIGRATION_TASK_RESULT using dual on (ID = ?) "
			+ "when matched then update set PARENT_ID = ?, USER_ID = ?, PATH = ?, CONFIGURATION_ID = ?, FROM_S3_ARCHIVE_CONFIGURATION_ID = ?, "
			+ "TO_S3_ARCHIVE_CONFIGURATION_ID = ?, TYPE = ?, RESULT = ?, CREATED = ?, COMPLETED = ?, MESSAGE = ?, "
			+ "FROM_S3_ARCHIVE_LOCATION_FILE_CONTAINER_ID = ?, FROM_S3_ARCHIVE_LOCATION_FILE_ID = ?, "
			+ "TO_S3_ARCHIVE_LOCATION_FILE_CONTAINER_ID = ?, TO_S3_ARCHIVE_LOCATION_FILE_ID = ? "
			+ "when not matched then insert (ID, PARENT_ID, USER_ID, PATH, CONFIGURATION_ID, FROM_S3_ARCHIVE_CONFIGURATION_ID, "
			+ "TO_S3_ARCHIVE_CONFIGURATION_ID, TYPE, RESULT, CREATED, COMPLETED, MESSAGE, FROM_S3_ARCHIVE_LOCATION_FILE_CONTAINER_ID, "
			+ "FROM_S3_ARCHIVE_LOCATION_FILE_ID, TO_S3_ARCHIVE_LOCATION_FILE_CONTAINER_ID, TO_S3_ARCHIVE_LOCATION_FILE_ID) "
			+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

	private static final String SET_DATA_MIGRATION_TASKS_STATUS_SQL = "update HPC_DATA_MIGRATION_TASK set STATUS = ? where STATUS = ?";

	private static final String DELETE_DATA_MIGRATION_TASK_SQL = "delete from HPC_DATA_MIGRATION_TASK where ID = ?";

	private static final String GET_DATA_MIGRATION_TASKS_SQL = "select * from HPC_DATA_MIGRATION_TASK where STATUS = ? and TYPE = ?";

	private static final String GET_DATA_OBJECT_MIGRATION_TASKS_SQL = "select * from HPC_DATA_MIGRATION_TASK  where PARENT_ID = ?";

	private static final String GET_DATA_OBJECT_MIGRATION_TASK_SQL = "select * from HPC_DATA_MIGRATION_TASK  where PARENT_ID = ? and PATH = ?";

	private static final String GET_COLLECTION_MIGRATION_RESULT_COUNT_SQL = "select RESULT, count(RESULT) as COUNT from HPC_DATA_MIGRATION_TASK_RESULT where PARENT_ID = ? group by RESULT";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;

	// HpcDataMigrationTask table to object mapper.
	private RowMapper<HpcDataMigrationTask> dataMigrationTaskRowMapper = (rs, rowNum) -> {
		HpcDataMigrationTask dataMigrationTask = new HpcDataMigrationTask();
		dataMigrationTask.setId(rs.getString("ID"));
		dataMigrationTask.setParentId(rs.getString("PARENT_ID"));
		dataMigrationTask.setUserId(rs.getString("USER_ID"));
		dataMigrationTask.setPath(rs.getString("PATH"));
		dataMigrationTask.setConfigurationId(rs.getString("CONFIGURATION_ID"));
		dataMigrationTask.setFromS3ArchiveConfigurationId(rs.getString("FROM_S3_ARCHIVE_CONFIGURATION_ID"));
		dataMigrationTask.setToS3ArchiveConfigurationId(rs.getString("TO_S3_ARCHIVE_CONFIGURATION_ID"));
		dataMigrationTask.setType(HpcDataMigrationType.fromValue(rs.getString(("TYPE"))));
		dataMigrationTask.setStatus(HpcDataMigrationStatus.fromValue(rs.getString(("STATUS"))));

		Calendar created = Calendar.getInstance();
		created.setTime(rs.getTimestamp("CREATED"));
		dataMigrationTask.setCreated(created);

		return dataMigrationTask;
	};

	// Mapper to get a specific result count of items in a collection migration
	// task.
	private RowMapper<Map.Entry<HpcDataMigrationResult, Integer>> collectionMigrationResultCountRowMapper = (rs,
			rowNum) -> {
		return new AbstractMap.SimpleEntry<HpcDataMigrationResult, Integer>(
				HpcDataMigrationResult.fromValue(rs.getString("RESULT")), rs.getInt("COUNT"));
	};

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcDataMigrationDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataMigrationDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void upsertDataMigrationTask(HpcDataMigrationTask dataMigrationTask) throws HpcException {
		try {
			if (StringUtils.isEmpty(dataMigrationTask.getId())) {
				dataMigrationTask.setId(UUID.randomUUID().toString());
			}

			jdbcTemplate.update(UPSERT_DATA_MIGRATION_TASK_SQL, dataMigrationTask.getId(),
					dataMigrationTask.getParentId(), dataMigrationTask.getUserId(), dataMigrationTask.getPath(),
					dataMigrationTask.getConfigurationId(), dataMigrationTask.getFromS3ArchiveConfigurationId(),
					dataMigrationTask.getToS3ArchiveConfigurationId(), dataMigrationTask.getType().value(),
					dataMigrationTask.getStatus().value(), dataMigrationTask.getCreated(), dataMigrationTask.getId(),
					dataMigrationTask.getParentId(), dataMigrationTask.getUserId(), dataMigrationTask.getPath(),
					dataMigrationTask.getConfigurationId(), dataMigrationTask.getFromS3ArchiveConfigurationId(),
					dataMigrationTask.getToS3ArchiveConfigurationId(), dataMigrationTask.getType().value(),
					dataMigrationTask.getStatus().value(), dataMigrationTask.getCreated());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to upsert a data migration task: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcDataMigrationTask> getDataMigrationTasks(HpcDataMigrationStatus status, HpcDataMigrationType type)
			throws HpcException {
		try {
			return jdbcTemplate.query(GET_DATA_MIGRATION_TASKS_SQL, dataMigrationTaskRowMapper, status.value(),
					type.value());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get a data migration tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcDataMigrationTask> getDataObjectMigrationTasks(String collectionMigrationTaskId)
			throws HpcException {
		try {
			return jdbcTemplate.query(GET_DATA_OBJECT_MIGRATION_TASKS_SQL, dataMigrationTaskRowMapper,
					collectionMigrationTaskId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get a data object migration tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	public HpcDataMigrationTask getDataObjectMigrationTask(String collectionMigrationTaskId, String path)
			throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_DATA_OBJECT_MIGRATION_TASK_SQL, HpcDataMigrationTask.class,
					collectionMigrationTaskId, path);

		} catch (IncorrectResultSizeDataAccessException irse) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get a data object migration tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void deleteDataMigrationTask(String id) throws HpcException {
		try {
			jdbcTemplate.update(DELETE_DATA_MIGRATION_TASK_SQL, id);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to delete a data migration task: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void upsertDataMigrationTaskResult(HpcDataMigrationTask dataMigrationTask, Calendar completed,
			HpcDataMigrationResult result, String message) throws HpcException {
		HpcFileLocation fromS3ArchiveLocation = Optional.ofNullable(dataMigrationTask.getFromS3ArchiveLocation())
				.orElse(new HpcFileLocation());
		HpcFileLocation toS3ArchiveLocation = Optional.ofNullable(dataMigrationTask.getToS3ArchiveLocation())
				.orElse(new HpcFileLocation());
		try {
			jdbcTemplate.update(UPSERT_DATA_MIGRATION_TASK_RESULT_SQL, dataMigrationTask.getId(),
					dataMigrationTask.getParentId(), dataMigrationTask.getUserId(), dataMigrationTask.getPath(),
					dataMigrationTask.getConfigurationId(), dataMigrationTask.getFromS3ArchiveConfigurationId(),
					dataMigrationTask.getToS3ArchiveConfigurationId(), dataMigrationTask.getType().value(),
					result.value(), dataMigrationTask.getCreated(), completed, message,
					fromS3ArchiveLocation.getFileContainerId(), fromS3ArchiveLocation.getFileId(),
					toS3ArchiveLocation.getFileContainerId(), toS3ArchiveLocation.getFileId(),
					dataMigrationTask.getId(), dataMigrationTask.getParentId(), dataMigrationTask.getUserId(),
					dataMigrationTask.getPath(), dataMigrationTask.getConfigurationId(),
					dataMigrationTask.getFromS3ArchiveConfigurationId(),
					dataMigrationTask.getToS3ArchiveConfigurationId(), dataMigrationTask.getType().value(),
					result.value(), dataMigrationTask.getCreated(), completed, message,
					fromS3ArchiveLocation.getFileContainerId(), fromS3ArchiveLocation.getFileId(),
					toS3ArchiveLocation.getFileContainerId(), toS3ArchiveLocation.getFileId());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to upsert a data migration task result: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void setDataMigrationTasksStatus(HpcDataMigrationStatus fromStatus, HpcDataMigrationStatus toStatus)
			throws HpcException {
		try {
			jdbcTemplate.update(SET_DATA_MIGRATION_TASKS_STATUS_SQL, toStatus.value(), fromStatus.value());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to update data migration tasks status: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public Map<HpcDataMigrationResult, Integer> getCollectionMigrationResultCount(String collectionMigrationTaskId)
			throws HpcException {
		try {
			Map<HpcDataMigrationResult, Integer> resultsCountMap = new EnumMap<>(HpcDataMigrationResult.class);
			jdbcTemplate
					.query(GET_COLLECTION_MIGRATION_RESULT_COUNT_SQL, collectionMigrationResultCountRowMapper,
							collectionMigrationTaskId)
					.forEach(resultCount -> resultsCountMap.put(resultCount.getKey(), resultCount.getValue()));

			return resultsCountMap;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get a collection migration result count: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

}
