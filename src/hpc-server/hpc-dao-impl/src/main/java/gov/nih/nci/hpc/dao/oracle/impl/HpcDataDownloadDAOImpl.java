/**
 * HpcDataDownloadDAOImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao.oracle.impl;

import static gov.nih.nci.hpc.util.HpcUtil.fromPathsString;
import static gov.nih.nci.hpc.util.HpcUtil.toPathsString;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.annotation.Transactional;

import gov.nih.nci.hpc.dao.HpcDataDownloadDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaAccount;
import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcBoxDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTaskStatusFilter;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcGoogleDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcUserDownloadRequest;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Download DAO Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataDownloadDAOImpl implements HpcDataDownloadDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String CREATE_DATA_OBJECT_DOWNLOAD_TASK_SQL = "insert into HPC_DATA_OBJECT_DOWNLOAD_TASK (ID, USER_ID, PATH, CONFIGURATION_ID, S3_ARCHIVE_CONFIGURATION_ID, DATA_TRANSFER_REQUEST_ID, DATA_TRANSFER_TYPE, "
			+ "DATA_TRANSFER_STATUS, DOWNLOAD_FILE_PATH, ARCHIVE_LOCATION_FILE_CONTAINER_ID, ARCHIVE_LOCATION_FILE_ID, DESTINATION_LOCATION_FILE_CONTAINER_ID, "
			+ "DESTINATION_LOCATION_FILE_ID, DESTINATION_TYPE, S3_ACCOUNT_ACCESS_KEY, S3_ACCOUNT_SECRET_KEY, S3_ACCOUNT_REGION, S3_ACCOUNT_URL, "
			+ "S3_ACCOUNT_PATH_STYLE_ACCESS_ENABLED, GOOGLE_ACCESS_TOKEN, ASPERA_ACCOUNT_USER, ASPERA_ACCOUNT_PASSWORD, ASPERA_ACCOUNT_HOST, BOX_ACCESS_TOKEN, BOX_REFRESH_TOKEN, "
			+ "COMPLETION_EVENT, COLLECTION_DOWNLOAD_TASK_ID, PERCENT_COMPLETE, STAGING_PERCENT_COMPLETE, DATA_SIZE, CREATED, "
			+ "PROCESSED, IN_PROCESS, RESTORE_REQUESTED, S3_DOWNLOAD_TASK_SERVER_ID, FIRST_HOP_RETRIED, RETRY_TASK_ID, RETRY_USER_ID, DOC, PRIORITY) "
			+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

	private static final String UPDATE_DATA_OBJECT_DOWNLOAD_TASK_SQL = "update HPC_DATA_OBJECT_DOWNLOAD_TASK "
			+ "set USER_ID = ?, PATH = ?, CONFIGURATION_ID = ?, S3_ARCHIVE_CONFIGURATION_ID = ?, DATA_TRANSFER_REQUEST_ID = ?, "
			+ "DATA_TRANSFER_TYPE = ?, DATA_TRANSFER_STATUS = ?, DOWNLOAD_FILE_PATH = ?, ARCHIVE_LOCATION_FILE_CONTAINER_ID = ?, ARCHIVE_LOCATION_FILE_ID = ?, "
			+ "DESTINATION_LOCATION_FILE_CONTAINER_ID = ?, DESTINATION_LOCATION_FILE_ID = ?, DESTINATION_TYPE = ?, S3_ACCOUNT_ACCESS_KEY = ?, "
			+ "S3_ACCOUNT_SECRET_KEY = ?, S3_ACCOUNT_REGION = ?, S3_ACCOUNT_URL = ?, S3_ACCOUNT_PATH_STYLE_ACCESS_ENABLED = ?, GOOGLE_ACCESS_TOKEN = ?, "
			+ "ASPERA_ACCOUNT_USER = ?, ASPERA_ACCOUNT_PASSWORD = ?, ASPERA_ACCOUNT_HOST = ?, BOX_ACCESS_TOKEN = ?, BOX_REFRESH_TOKEN = ?, "
			+ "COMPLETION_EVENT = ?, COLLECTION_DOWNLOAD_TASK_ID = ?, PERCENT_COMPLETE = ?, STAGING_PERCENT_COMPLETE = ?, DATA_SIZE = ?, CREATED = ?, PROCESSED = ?, IN_PROCESS = ?, "
			+ "RESTORE_REQUESTED = ?, S3_DOWNLOAD_TASK_SERVER_ID = ?, FIRST_HOP_RETRIED = ?, RETRY_TASK_ID = ?, RETRY_USER_ID = ? where ID = ?";

	private static final String DELETE_DATA_OBJECT_DOWNLOAD_TASK_SQL = "delete from HPC_DATA_OBJECT_DOWNLOAD_TASK where ID = ?";

	private static final String UPDATE_DATA_OBJECTS_DOWNLOAD_TASK_STATUS_SQL = "update HPC_DATA_OBJECT_DOWNLOAD_TASK set DATA_TRANSFER_STATUS = ? where COLLECTION_DOWNLOAD_TASK_ID = ?";

	private static final String UPDATE_DATA_OBJECT_DOWNLOAD_TASK_PRIORITY_SQL = "update HPC_DATA_OBJECT_DOWNLOAD_TASK set PRIORITY = ? where ID = ?";

	private static final String UPDATE_DATA_OBJECTS_DOWNLOAD_TASK_PRIORITY_SQL = "update HPC_DATA_OBJECT_DOWNLOAD_TASK set PRIORITY = ? where COLLECTION_DOWNLOAD_TASK_ID = ?";

	private static final String UPDATE_DATA_OBJECT_DOWNLOAD_TASK_STATUS_FILTER = " or (DATA_TRANSFER_STATUS = ? and DESTINATION_TYPE = ?)";

	private static final String SELECT_FOR_UPDATE_DATA_OBJECT_DOWNLOAD_TASK_IN_PROCESS_SQL = "select * from HPC_DATA_OBJECT_DOWNLOAD_TASK where ID = ? and IN_PROCESS != ? for update skip locked";

	private static final String SET_DATA_OBJECT_DOWNLOAD_TASK_IN_PROCESS_SQL = "update HPC_DATA_OBJECT_DOWNLOAD_TASK set IN_PROCESS = ?, S3_DOWNLOAD_TASK_SERVER_ID = ? where ID = ? and IN_PROCESS != ?";

	private static final String RESET_DATA_OBJECT_DOWNLOAD_TASK_IN_PROCESS_SQL = "update HPC_DATA_OBJECT_DOWNLOAD_TASK set IN_PROCESS = '0', S3_DOWNLOAD_TASK_SERVER_ID = null where IN_PROCESS = '1' "
			+ "and S3_DOWNLOAD_TASK_SERVER_ID is null or S3_DOWNLOAD_TASK_SERVER_ID = ?";

	private static final String SET_DATA_OBJECT_DOWNLOAD_TASK_PROCESSED_SQL = "update HPC_DATA_OBJECT_DOWNLOAD_TASK set PROCESSED = ? where ID = ?";

	private static final String GET_DATA_OBJECT_DOWNLOAD_TASK_SQL = "select * from HPC_DATA_OBJECT_DOWNLOAD_TASK where ID = ?";

	private static final String GET_DATA_OBJECT_DOWNLOAD_TASK_STATUS_SQL = "select DATA_TRANSFER_STATUS from HPC_DATA_OBJECT_DOWNLOAD_TASK where ID = ?";

	private static final String GET_DATA_OBJECT_DOWNLOAD_TASKS_SQL = "select * from HPC_DATA_OBJECT_DOWNLOAD_TASK order by PRIORITY, CREATED";

	private static final String GET_ALL_DATA_OBJECT_DOWNLOAD_TASK_BY_STATUS_SQL = "select * from HPC_DATA_OBJECT_DOWNLOAD_TASK where DATA_TRANSFER_STATUS = ? ";

	private static final String GET_ALL_DATA_OBJECT_DOWNLOAD_TASK_BY_COLLECTION_DOWNLOAD_TASK_ID_SQL = "select * from HPC_DATA_OBJECT_DOWNLOAD_TASK where COLLECTION_DOWNLOAD_TASK_ID = ? ";

	private static final String GET_ALL_DATA_OBJECT_DOWNLOAD_RESULTS_BY_COLLECTION_DOWNLOAD_TASK_ID_SQL = "select * from HPC_DOWNLOAD_TASK_RESULT where COLLECTION_DOWNLOAD_TASK_ID = ? and TYPE = 'DATA_OBJECT'";

	private static final String GET_DATA_OBJECT_DOWNLOAD_TASK_BY_STATUS_SQL = "select * from HPC_DATA_OBJECT_DOWNLOAD_TASK where DATA_TRANSFER_STATUS = ? "
			+ "and (PROCESSED < ? or PROCESSED is null) order by PRIORITY, CREATED fetch next 1 rows only";

	private static final String GET_DATA_OBJECT_DOWNLOAD_TASK_BY_STATUS_AND_TYPE_SQL = "select * from HPC_DATA_OBJECT_DOWNLOAD_TASK where "
			+ "DATA_TRANSFER_STATUS = ? and DATA_TRANSFER_TYPE = ? and (PROCESSED < ? or PROCESSED is null) order by PRIORITY, "
			+ "CREATED fetch next 1 rows only";

	private static final String UPSERT_DOWNLOAD_TASK_RESULT_SQL = "merge into HPC_DOWNLOAD_TASK_RESULT using dual on (ID = ?) "
			+ "when matched then update set USER_ID = ?, PATH = ?, DATA_TRANSFER_REQUEST_ID = ?, DATA_TRANSFER_TYPE = ?, "
			+ "ARCHIVE_LOCATION_FILE_CONTAINER_ID = ?, ARCHIVE_LOCATION_FILE_ID = ?, "
			+ "DESTINATION_LOCATION_FILE_CONTAINER_ID = ?, DESTINATION_LOCATION_FILE_CONTAINER_NAME = ?, DESTINATION_LOCATION_FILE_ID = ?, "
			+ "DESTINATION_TYPE = ?, RESULT = ?, TYPE = ?, MESSAGE = ?, COMPLETION_EVENT = ?, COLLECTION_DOWNLOAD_TASK_ID = ?, EFFECTIVE_TRANSFER_SPEED = ?, "
			+ "DATA_SIZE = ?, CREATED = ?, COMPLETED = ?, RESTORE_REQUESTED = ?, RETRY_TASK_ID = ?, RETRY_USER_ID = ?, FIRST_HOP_RETRIED = ?, DOC = ?, GOOGLE_DRIVE_ACCESS_TOKEN = ? "
			+ "when not matched then insert (ID, USER_ID, PATH, DATA_TRANSFER_REQUEST_ID, DATA_TRANSFER_TYPE, ARCHIVE_LOCATION_FILE_CONTAINER_ID, ARCHIVE_LOCATION_FILE_ID, "
			+ "DESTINATION_LOCATION_FILE_CONTAINER_ID, DESTINATION_LOCATION_FILE_CONTAINER_NAME, DESTINATION_LOCATION_FILE_ID, DESTINATION_TYPE, RESULT, TYPE, MESSAGE, COMPLETION_EVENT, "
			+ "COLLECTION_DOWNLOAD_TASK_ID, EFFECTIVE_TRANSFER_SPEED, DATA_SIZE, CREATED, COMPLETED, RESTORE_REQUESTED, RETRY_TASK_ID, RETRY_USER_ID, FIRST_HOP_RETRIED, DOC, GOOGLE_DRIVE_ACCESS_TOKEN) "
			+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

	private static final String UPDATE_DOWNLOAD_TASK_RESULT_CLOBS_SQL = "update HPC_DOWNLOAD_TASK_RESULT set ITEMS = ?, COLLECTION_PATHS = ? where ID = ?";

	private static final String GET_DOWNLOAD_TASK_RESULT_SQL = "select * from HPC_DOWNLOAD_TASK_RESULT where ID = ? and TYPE = ?";

	private static final String UPSERT_COLLECTION_DOWNLOAD_TASK_SQL = "merge into HPC_COLLECTION_DOWNLOAD_TASK  using dual on (ID = ?) "
			+ "when matched then update set USER_ID = ?, PATH = ?, CONFIGURATION_ID = ?, DESTINATION_LOCATION_FILE_CONTAINER_ID = ?, "
			+ "DESTINATION_LOCATION_FILE_ID = ?, DESTINATION_OVERWRITE = ?, S3_ACCOUNT_ACCESS_KEY = ?, S3_ACCOUNT_SECRET_KEY = ?, S3_ACCOUNT_REGION = ?, "
			+ "S3_ACCOUNT_URL = ?, S3_ACCOUNT_PATH_STYLE_ACCESS_ENABLED = ?, GOOGLE_DRIVE_ACCESS_TOKEN = ?, GOOGLE_CLOUD_ACCESS_TOKEN = ?,"
			+ "ASPERA_ACCOUNT_USER = ?, ASPERA_ACCOUNT_PASSWORD = ?, ASPERA_ACCOUNT_HOST = ?, BOX_ACCESS_TOKEN = ?, BOX_REFRESH_TOKEN = ?, "
			+ "APPEND_PATH_TO_DOWNLOAD_DESTINATION = ?, APPEND_COLLECTION_NAME_TO_DOWNLOAD_DESTINATION = ?, STATUS = ?, TYPE = ?, CREATED = ?, RETRY_TASK_ID = ?, RETRY_USER_ID = ?, "
			+ "DATA_TRANSFER_REQUEST_ID = ?, DESTINATION_TYPE = ?, DOC = ? "
			+ "when not matched then insert (ID, USER_ID, PATH, CONFIGURATION_ID, DESTINATION_LOCATION_FILE_CONTAINER_ID, DESTINATION_LOCATION_FILE_ID, "
			+ "DESTINATION_OVERWRITE, S3_ACCOUNT_ACCESS_KEY, S3_ACCOUNT_SECRET_KEY, S3_ACCOUNT_REGION, S3_ACCOUNT_URL, S3_ACCOUNT_PATH_STYLE_ACCESS_ENABLED, "
			+ "GOOGLE_DRIVE_ACCESS_TOKEN, GOOGLE_CLOUD_ACCESS_TOKEN, ASPERA_ACCOUNT_USER, ASPERA_ACCOUNT_PASSWORD, ASPERA_ACCOUNT_HOST, BOX_ACCESS_TOKEN, BOX_REFRESH_TOKEN, "
			+ "APPEND_PATH_TO_DOWNLOAD_DESTINATION, APPEND_COLLECTION_NAME_TO_DOWNLOAD_DESTINATION, STATUS, TYPE, CREATED, RETRY_TASK_ID, RETRY_USER_ID, DATA_TRANSFER_REQUEST_ID, DESTINATION_TYPE, DOC) "
			+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

	private static final String UPDATE_COLLECTION_DOWNLOAD_TASK_CLOBS_SQL = "update HPC_COLLECTION_DOWNLOAD_TASK set ITEMS = ?, DATA_OBJECT_PATHS = ?, COLLECTION_PATHS = ? where ID = ?";

	private static final String UPDATE_COLLECTION_DOWNLOAD_TASK_PRIORITY_SQL = "update HPC_COLLECTION_DOWNLOAD_TASK set PRIORITY = ? where ID = ?";

	private static final String GET_COLLECTION_DOWNLOAD_TASK_SQL = "select * from HPC_COLLECTION_DOWNLOAD_TASK where ID = ?";

	private static final String DELETE_COLLECTION_DOWNLOAD_TASK_SQL = "delete from HPC_COLLECTION_DOWNLOAD_TASK where ID = ?";
	
	private static final String GET_COLLECTION_DOWNLOAD_TASK_PRIORITY_SQL = "select PRIORITY from HPC_COLLECTION_DOWNLOAD_TASK where ID = ?";

	private static final String GET_COLLECTION_DOWNLOAD_TASKS_BY_STATUS_SQL = "select * from HPC_COLLECTION_DOWNLOAD_TASK where STATUS = ? "
			+ "order by \"PRIORITY\", \"CREATED\"";

	private static final String GET_COLLECTION_DOWNLOAD_TASKS_IN_PROCESS_SQL = "select * from HPC_COLLECTION_DOWNLOAD_TASK where IN_PROCESS = '1' "
			+ "order by \"PRIORITY\", \"CREATED\"";

	private static final String GET_COLLECTION_DOWNLOAD_TASKS_SQL = "select * from HPC_COLLECTION_DOWNLOAD_TASK where STATUS = ? "
			+ "and IN_PROCESS = ? order by PRIORITY, CREATED";

	private static final String GET_DATA_OBJECT_DOWNLOAD_REQUESTS_SQL = "select null as USER_ID, ID, PATH, CREATED, 'DATA_OBJECT' as TYPE, "
			+ "null as COMPLETED, null as RESULT, null as MESSAGE, null as ITEMS, DESTINATION_TYPE, RETRY_USER_ID, DATA_TRANSFER_STATUS as STATUS, STAGING_PERCENT_COMPLETE from HPC_DATA_OBJECT_DOWNLOAD_TASK where USER_ID = ? and COMPLETION_EVENT = '1' "
			+ "order by CREATED";

	private static final String GET_COLLECTION_DOWNLOAD_REQUESTS_SQL = "select null as USER_ID, ID, PATH, CREATED, TYPE, null as COMPLETED, "
			+ "null as RESULT, null as MESSAGE, ITEMS, DESTINATION_TYPE, RETRY_USER_ID, STATUS, TOTAL_BYTES_TRANSFERRED from HPC_COLLECTION_DOWNLOAD_TASK where USER_ID = ? order by CREATED";

	private static final String GET_DOWNLOAD_RESULTS_SQL = "select null as USER_ID, ID, PATH, CREATED, TYPE, COMPLETED, RESULT, RETRY_USER_ID, ITEMS, DESTINATION_TYPE, MESSAGE "
			+ "from HPC_DOWNLOAD_TASK_RESULT where USER_ID = ? and COMPLETION_EVENT = '1' order by CREATED desc offset ? rows fetch next ? rows only";

	private static final String GET_DOWNLOAD_RESULTS_COUNT_SQL = "select count(*) from HPC_DOWNLOAD_TASK_RESULT where USER_ID = ? and COMPLETION_EVENT = '1'";

	private static final String GET_DATA_OBJECT_DOWNLOAD_REQUESTS_FOR_DOC_SQL = "select TASK.USER_ID, ID, PATH, TASK.CREATED, 'DATA_OBJECT' as TYPE, "
			+ "null as COMPLETED, null as RESULT, null as MESSAGE, null as MESSAGE, null as ITEMS, DESTINATION_TYPE, RETRY_USER_ID, DATA_TRANSFER_STATUS as STATUS, STAGING_PERCENT_COMPLETE from HPC_DATA_OBJECT_DOWNLOAD_TASK TASK, HPC_USER USER1 where USER1.USER_ID=TASK.USER_ID "
			+ "and (TASK.DOC= ? or TASK.USER_ID = ?) and COMPLETION_EVENT = '1' order by CREATED";

	private static final String GET_ALL_DATA_OBJECT_DOWNLOAD_REQUESTS_SQL = "select USER_ID, ID, PATH, CREATED, 'DATA_OBJECT' as TYPE, null as COMPLETED, "
			+ "null as RESULT, null as MESSAGE, null as ITEMS, DESTINATION_TYPE, RETRY_USER_ID, DATA_TRANSFER_STATUS as STATUS, STAGING_PERCENT_COMPLETE from HPC_DATA_OBJECT_DOWNLOAD_TASK where COMPLETION_EVENT = '1' order by CREATED";

	private static final String GET_COLLECTION_DOWNLOAD_REQUESTS_FOR_DOC_SQL = "select TASK.USER_ID, ID, PATH, TASK.CREATED, TYPE, null as COMPLETED, "
			+ "null as RESULT, null as MESSAGE, ITEMS, DESTINATION_TYPE, RETRY_USER_ID, STATUS, TOTAL_BYTES_TRANSFERRED from HPC_COLLECTION_DOWNLOAD_TASK TASK, HPC_USER USER1 where USER1.USER_ID = TASK.USER_ID and (TASK.DOC = ? or TASK.USER_ID = ?) order by CREATED";

	private static final String GET_ALL_COLLECTION_DOWNLOAD_REQUESTS_SQL = "select USER_ID, ID, PATH, CREATED, TYPE, null as COMPLETED, "
			+ "null as RESULT, null as MESSAGE, ITEMS, DESTINATION_TYPE, RETRY_USER_ID, STATUS, TOTAL_BYTES_TRANSFERRED from HPC_COLLECTION_DOWNLOAD_TASK order by CREATED";

	private static final String GET_DOWNLOAD_RESULTS_FOR_DOC_SQL = "select TASK.USER_ID, ID, PATH, TASK.CREATED, TYPE, COMPLETED, RESULT, ITEMS, DESTINATION_TYPE, RETRY_USER_ID, MESSAGE "
			+ "from HPC_DOWNLOAD_TASK_RESULT TASK, HPC_USER USER1 where USER1.USER_ID = TASK.USER_ID and (TASK.DOC = ? or TASK.USER_ID = ?) and "
			+ "COMPLETION_EVENT = '1' order by CREATED desc offset ? rows fetch next ? rows only";

	private static final String GET_ALL_DOWNLOAD_RESULTS_SQL = "select USER_ID, ID, PATH, CREATED, TYPE, COMPLETED, RESULT, RETRY_USER_ID, ITEMS, DESTINATION_TYPE, MESSAGE "
			+ "from HPC_DOWNLOAD_TASK_RESULT where COMPLETION_EVENT = '1' order by CREATED desc offset ? rows fetch next ? rows only";

	private static final String GET_DOWNLOAD_RESULTS_COUNT_FOR_DOC_SQL = "select count(*) from HPC_DOWNLOAD_TASK_RESULT TASK, HPC_USER USER1 where "
			+ "USER1.USER_ID = TASK.USER_ID and (TASK.DOC = ? or TASK.USER_ID = ?) and COMPLETION_EVENT = '1'";

	private static final String GET_ALL_DOWNLOAD_RESULTS_COUNT_SQL = "select count(*) from HPC_DOWNLOAD_TASK_RESULT where COMPLETION_EVENT = '1'";

	private static final String GET_DATA_OBJECT_DOWNLOAD_TASKS_COUNT_BY_STATUS_AND_TYPE_SQL = "select count(*) from HPC_DATA_OBJECT_DOWNLOAD_TASK where "
			+ "DATA_TRANSFER_TYPE = ? and DESTINATION_TYPE = ? and DATA_TRANSFER_STATUS = ? and S3_DOWNLOAD_TASK_SERVER_ID = ?";

	private static final String GET_GLOBUS_DATA_OBJECT_DOWNLOAD_TASKS_COUNT_IN_PROGRESS_FOR_USER_BY_PATH_SQL = "select count(*) from HPC_DATA_OBJECT_DOWNLOAD_TASK where "
			+ "(DATA_TRANSFER_STATUS = 'IN_PROGRESS' OR DATA_TRANSFER_TYPE = 'GLOBUS') and DESTINATION_TYPE = 'GLOBUS' and USER_ID = ? and PATH = ? ";

	private static final String GET_COLLECTION_DOWNLOAD_REQUESTS_COUNT_SQL = "select count(*) from HPC_COLLECTION_DOWNLOAD_TASK where USER_ID = ? and "
			+ "STATUS = ? and IN_PROCESS = ?";

	private static final String GET_COLLECTION_DOWNLOAD_REQUESTS_COUNT_BY_PATH_AND_ENDPOINT_SQL = "select count(*) from HPC_COLLECTION_DOWNLOAD_TASK where "
			+ "PATH = ? AND DESTINATION_LOCATION_FILE_CONTAINER_ID = ?";

	private static final String GET_COLLECTION_DOWNLOAD_TASKS_COUNT_BY_USER_AND_PATH_SQL = "select count(*) from HPC_COLLECTION_DOWNLOAD_TASK where "
			+ "USER_ID = ? AND PATH = ? and IN_PROCESS = ?";

	private static final String GET_COLLECTION_DOWNLOAD_TASKS_COUNT_BY_USER_SQL = "select count(*) from HPC_COLLECTION_DOWNLOAD_TASK where "
			+ "USER_ID = ? and IN_PROCESS = ?";

	private static final String SET_COLLECTION_DOWNLOAD_TASK_IN_PROCESS_SQL = "update HPC_COLLECTION_DOWNLOAD_TASK set IN_PROCESS = ? where ID = ?";

	private static final String RESET_COLLECTION_DOWNLOAD_TASK_IN_PROCESS_SQL = "update HPC_COLLECTION_DOWNLOAD_TASK set IN_PROCESS = '0', DESTINATION_OVERWRITE = '1' where ID = ?";

	private static final String SET_COLLECTION_DOWNLOAD_TASK_CANCELLATION_REQUEST_SQL = "update HPC_COLLECTION_DOWNLOAD_TASK set CANCELLATION_REQUESTED = ? where ID = ?";

	private static final String GET_COLLECTION_DOWNLOAD_TASK_CANCELLATION_REQUEST_SQL = "select CANCELLATION_REQUESTED from HPC_COLLECTION_DOWNLOAD_TASK where ID = ?";

	private static final String GET_TOTAL_DOWNLOADS_SIZE_SQL = "select sum(DATA_SIZE) from HPC_DATA_OBJECT_DOWNLOAD_TASK where USER_ID = ? and DATA_TRANSFER_STATUS = ?";

	private static final String SELECT_FOR_UPDATE_TOTAL_BYTES_TRANSFERRED_SQL = "select * from HPC_COLLECTION_DOWNLOAD_TASK where ID = ? and STATUS = 'RECEIVED' for update nowait";
	private static final String UPDATE_TOTAL_BYTES_TRANSFERRED_SQL = "update HPC_COLLECTION_DOWNLOAD_TASK set TOTAL_BYTES_TRANSFERRED = nvl(TOTAL_BYTES_TRANSFERRED, 0) + ? where ID = ? and STATUS = 'RECEIVED'";

	private static final String REMOVE_GOOGLE_ACCESS_TOKEN_FROM_DOWNLOAD_TASK_RESULT_SQL = "update HPC_DOWNLOAD_TASK_RESULT set GOOGLE_DRIVE_ACCESS_TOKEN='' where" 
			+ " GOOGLE_DRIVE_ACCESS_TOKEN is not null and COMPLETED < sysdate  - (?/24)";
	
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// Lob handler
	private LobHandler lobHandler = new DefaultLobHandler();

	// Encryptor.
	@Autowired
	private HpcEncryptor encryptor = null;

	// HpcDataObjectDownloadTask table to object mapper.
	private RowMapper<HpcDataObjectDownloadTask> dataObjectDownloadTaskRowMapper = (rs, rowNum) -> {
		HpcDataObjectDownloadTask dataObjectDownloadTask = new HpcDataObjectDownloadTask();
		dataObjectDownloadTask.setId(rs.getString("ID"));
		dataObjectDownloadTask.setUserId(rs.getString("USER_ID"));
		dataObjectDownloadTask.setConfigurationId(rs.getString("CONFIGURATION_ID"));
		dataObjectDownloadTask.setS3ArchiveConfigurationId(rs.getString("S3_ARCHIVE_CONFIGURATION_ID"));
		dataObjectDownloadTask.setPath(rs.getString("PATH"));
		dataObjectDownloadTask.setDataTransferRequestId(rs.getString("DATA_TRANSFER_REQUEST_ID"));
		dataObjectDownloadTask.setDataTransferType(HpcDataTransferType.fromValue(rs.getString(("DATA_TRANSFER_TYPE"))));
		dataObjectDownloadTask
				.setDataTransferStatus(HpcDataTransferDownloadStatus.fromValue(rs.getString(("DATA_TRANSFER_STATUS"))));
		dataObjectDownloadTask.setDownloadFilePath(rs.getString("DOWNLOAD_FILE_PATH"));
		dataObjectDownloadTask.setCompletionEvent(rs.getBoolean("COMPLETION_EVENT"));
		dataObjectDownloadTask.setCollectionDownloadTaskId(rs.getString("COLLECTION_DOWNLOAD_TASK_ID"));
		dataObjectDownloadTask.setPercentComplete(rs.getInt("PERCENT_COMPLETE"));
		dataObjectDownloadTask.setSize(rs.getLong("DATA_SIZE"));
		dataObjectDownloadTask.setInProcess(rs.getBoolean("IN_PROCESS"));
		dataObjectDownloadTask.setRestoreRequested(rs.getBoolean("RESTORE_REQUESTED"));
		dataObjectDownloadTask.setS3DownloadTaskServerId(rs.getString("S3_DOWNLOAD_TASK_SERVER_ID"));
		dataObjectDownloadTask.setFirstHopRetried(rs.getBoolean("FIRST_HOP_RETRIED"));
		dataObjectDownloadTask.setRetryUserId(rs.getString("RETRY_USER_ID"));
		dataObjectDownloadTask.setRetryTaskId(rs.getString("RETRY_TASK_ID"));
		dataObjectDownloadTask.setDoc(rs.getString("DOC"));
		dataObjectDownloadTask.setPriority(rs.getInt("PRIORITY"));

		int stagingPercentComplete = rs.getInt("STAGING_PERCENT_COMPLETE");
		dataObjectDownloadTask.setStagingPercentComplete(stagingPercentComplete > 0 ? stagingPercentComplete : null);

		String archiveLocationFileContainerId = rs.getString("ARCHIVE_LOCATION_FILE_CONTAINER_ID");
		String archiveLocationFileId = rs.getString("ARCHIVE_LOCATION_FILE_ID");
		if (archiveLocationFileContainerId != null && archiveLocationFileId != null) {
			HpcFileLocation archiveLocation = new HpcFileLocation();
			archiveLocation.setFileContainerId(archiveLocationFileContainerId);
			archiveLocation.setFileId(archiveLocationFileId);
			dataObjectDownloadTask.setArchiveLocation(archiveLocation);
		}

		String destinationType = rs.getString("DESTINATION_TYPE");
		dataObjectDownloadTask
				.setDestinationType(destinationType != null ? HpcDataTransferType.fromValue(destinationType) : null);

		Calendar created = Calendar.getInstance();
		created.setTime(rs.getTimestamp("CREATED"));
		dataObjectDownloadTask.setCreated(created);

		String destinationLocationFileContainerId = rs.getString("DESTINATION_LOCATION_FILE_CONTAINER_ID");
		String destinationLocationFileId = rs.getString("DESTINATION_LOCATION_FILE_ID");
		HpcFileLocation destinationLocation = null;
		if (destinationLocationFileContainerId != null && destinationLocationFileId != null) {
			destinationLocation = new HpcFileLocation();
			destinationLocation.setFileContainerId(destinationLocationFileContainerId);
			destinationLocation.setFileId(destinationLocationFileId);
		}

		HpcS3Account s3Account = null;
		byte[] s3AccountAccessKey = rs.getBytes("S3_ACCOUNT_ACCESS_KEY");
		byte[] s3AccountSecretKey = rs.getBytes("S3_ACCOUNT_SECRET_KEY");
		if (s3AccountAccessKey != null && s3AccountSecretKey != null) {
			s3Account = new HpcS3Account();
			s3Account.setAccessKey(encryptor.decrypt(s3AccountAccessKey));
			s3Account.setSecretKey(encryptor.decrypt(s3AccountSecretKey));
			s3Account.setRegion(rs.getString("S3_ACCOUNT_REGION"));
			s3Account.setUrl(rs.getString("S3_ACCOUNT_URL"));
			s3Account.setPathStyleAccessEnabled(rs.getBoolean("S3_ACCOUNT_PATH_STYLE_ACCESS_ENABLED"));
		}

		String googleAccessToken = null;
		byte[] token = rs.getBytes("GOOGLE_ACCESS_TOKEN");
		if (token != null) {
			googleAccessToken = encryptor.decrypt(token);
		}

		HpcAsperaAccount asperaAccount = null;
		byte[] asperaAccountUser = rs.getBytes("ASPERA_ACCOUNT_USER");
		byte[] asperaAccountPassword = rs.getBytes("ASPERA_ACCOUNT_PASSWORD");
		String asperaAccountHost = rs.getString("ASPERA_ACCOUNT_HOST");
		if (asperaAccountUser != null && asperaAccountPassword != null && asperaAccountHost != null) {
			asperaAccount = new HpcAsperaAccount();
			asperaAccount.setUser(encryptor.decrypt(asperaAccountUser));
			asperaAccount.setPassword(encryptor.decrypt(asperaAccountPassword));
			asperaAccount.setHost(asperaAccountHost);
		}

		String boxAccessToken = null;
		byte[] accessToken = rs.getBytes("BOX_ACCESS_TOKEN");
		if (accessToken != null) {
			boxAccessToken = encryptor.decrypt(accessToken);
		}
		String boxRefreshToken = null;
		byte[] refreshToken = rs.getBytes("BOX_REFRESH_TOKEN");
		if (refreshToken != null) {
			boxRefreshToken = encryptor.decrypt(refreshToken);
		}

		if (dataObjectDownloadTask.getDestinationType().equals(HpcDataTransferType.S_3)) {
			HpcS3DownloadDestination s3DownloadDestination = new HpcS3DownloadDestination();
			s3DownloadDestination.setDestinationLocation(destinationLocation);
			s3DownloadDestination.setAccount(s3Account);
			dataObjectDownloadTask.setS3DownloadDestination(s3DownloadDestination);

		} else if (dataObjectDownloadTask.getDestinationType().equals(HpcDataTransferType.GLOBUS)) {
			HpcGlobusDownloadDestination globusDownloadDestination = new HpcGlobusDownloadDestination();
			globusDownloadDestination.setDestinationLocation(destinationLocation);
			dataObjectDownloadTask.setGlobusDownloadDestination(globusDownloadDestination);

		} else if (dataObjectDownloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_DRIVE)) {
			HpcGoogleDownloadDestination googleDriveDownloadDestination = new HpcGoogleDownloadDestination();
			googleDriveDownloadDestination.setDestinationLocation(destinationLocation);
			googleDriveDownloadDestination.setAccessToken(googleAccessToken);
			dataObjectDownloadTask.setGoogleDriveDownloadDestination(googleDriveDownloadDestination);

		} else if (dataObjectDownloadTask.getDestinationType().equals(HpcDataTransferType.GOOGLE_CLOUD_STORAGE)) {
			HpcGoogleDownloadDestination googleCloudStorageDownloadDestination = new HpcGoogleDownloadDestination();
			googleCloudStorageDownloadDestination.setDestinationLocation(destinationLocation);
			googleCloudStorageDownloadDestination.setAccessToken(googleAccessToken);
			dataObjectDownloadTask.setGoogleCloudStorageDownloadDestination(googleCloudStorageDownloadDestination);

		} else if (dataObjectDownloadTask.getDestinationType().equals(HpcDataTransferType.ASPERA)) {
			HpcAsperaDownloadDestination asperaDownloadDestination = new HpcAsperaDownloadDestination();
			asperaDownloadDestination.setDestinationLocation(destinationLocation);
			asperaDownloadDestination.setAccount(asperaAccount);
			dataObjectDownloadTask.setAsperaDownloadDestination(asperaDownloadDestination);

		} else if (dataObjectDownloadTask.getDestinationType().equals(HpcDataTransferType.BOX)) {
			HpcBoxDownloadDestination boxDownloadDestination = new HpcBoxDownloadDestination();
			boxDownloadDestination.setDestinationLocation(destinationLocation);
			boxDownloadDestination.setAccessToken(boxAccessToken);
			boxDownloadDestination.setRefreshToken(boxRefreshToken);
			dataObjectDownloadTask.setBoxDownloadDestination(boxDownloadDestination);
		}

		return dataObjectDownloadTask;
	};

	// HpcDownloadTaskResult table to object mapper.
	private RowMapper<HpcDownloadTaskResult> downloadTaskResultRowMapper = (rs, rowNum) -> {
		HpcDownloadTaskResult downloadTaskResult = new HpcDownloadTaskResult();
		downloadTaskResult.setId(rs.getString("ID"));
		downloadTaskResult.setUserId(rs.getString("USER_ID"));
		downloadTaskResult.setType(HpcDownloadTaskType.fromValue(rs.getString(("TYPE"))));
		downloadTaskResult.setPath(rs.getString("PATH"));
		downloadTaskResult.getCollectionPaths().addAll(fromPathsString(rs.getString("COLLECTION_PATHS")));
		downloadTaskResult.setDataTransferRequestId(rs.getString("DATA_TRANSFER_REQUEST_ID"));
		downloadTaskResult.setRetryTaskId(rs.getString("RETRY_TASK_ID"));
		downloadTaskResult.setRetryUserId(rs.getString("RETRY_USER_ID"));
		String dataTransferType = rs.getString("DATA_TRANSFER_TYPE");
		downloadTaskResult
				.setDataTransferType(dataTransferType != null ? HpcDataTransferType.fromValue(dataTransferType) : null);
		String archiveLocationFileContainerId = rs.getString("ARCHIVE_LOCATION_FILE_CONTAINER_ID");
		String archiveLocationFileId = rs.getString("ARCHIVE_LOCATION_FILE_ID");
		if (archiveLocationFileContainerId != null && archiveLocationFileId != null) {
			HpcFileLocation archiveLocation = new HpcFileLocation();
			archiveLocation.setFileContainerId(archiveLocationFileContainerId);
			archiveLocation.setFileId(archiveLocationFileId);
			downloadTaskResult.setArchiveLocation(archiveLocation);
		}
		String destinationLocationFileContainerId = rs.getString("DESTINATION_LOCATION_FILE_CONTAINER_ID");
		String destinationLocationFileContainerName = rs.getString("DESTINATION_LOCATION_FILE_CONTAINER_NAME");
		String destinationLocationFileId = rs.getString("DESTINATION_LOCATION_FILE_ID");
		if (destinationLocationFileContainerId != null && destinationLocationFileId != null) {
			HpcFileLocation destinationLocation = new HpcFileLocation();
			destinationLocation.setFileContainerId(destinationLocationFileContainerId);
			destinationLocation.setFileContainerName(destinationLocationFileContainerName);
			destinationLocation.setFileId(destinationLocationFileId);
			downloadTaskResult.setDestinationLocation(destinationLocation);
		}
		String destinationType = rs.getString("DESTINATION_TYPE");
		downloadTaskResult
				.setDestinationType(destinationType != null ? HpcDataTransferType.fromValue(destinationType) : null);
		downloadTaskResult.setDoc(rs.getString("DOC"));
		downloadTaskResult.setResult(HpcDownloadResult.fromValue(rs.getString("RESULT")));
		downloadTaskResult.setMessage(rs.getString("MESSAGE"));
		downloadTaskResult.getItems().addAll(fromJSON(rs.getString("ITEMS")));
		downloadTaskResult.setCompletionEvent(rs.getBoolean("COMPLETION_EVENT"));
		downloadTaskResult.setCollectionDownloadTaskId(rs.getString("COLLECTION_DOWNLOAD_TASK_ID"));
		downloadTaskResult.setEffectiveTransferSpeed(rs.getInt("EFFECTIVE_TRANSFER_SPEED"));
		long size = rs.getLong("DATA_SIZE");
		downloadTaskResult.setSize(size > 0 ? size : null);

		Calendar created = Calendar.getInstance();
		created.setTime(rs.getTimestamp("CREATED"));
		downloadTaskResult.setCreated(created);

		Calendar completed = Calendar.getInstance();
		completed.setTime(rs.getTimestamp("COMPLETED"));
		downloadTaskResult.setCompleted(completed);
		downloadTaskResult.setRestoreRequested(rs.getBoolean("RESTORE_REQUESTED"));
		downloadTaskResult.setFirstHopRetried(rs.getBoolean("FIRST_HOP_RETRIED"));

		String googleDriveAccessToken = null;
		byte[] driveToken = rs.getBytes("GOOGLE_DRIVE_ACCESS_TOKEN");
		if (driveToken != null && driveToken.length > 0) {
			googleDriveAccessToken = encryptor.decrypt(driveToken);
		}
		if (googleDriveAccessToken != null) {
			HpcGoogleDownloadDestination googleDriveDownloadDestination = new HpcGoogleDownloadDestination();
			googleDriveDownloadDestination.setDestinationLocation(downloadTaskResult.getDestinationLocation());
			googleDriveDownloadDestination.setAccessToken(googleDriveAccessToken);
			downloadTaskResult.setGoogleDriveDownloadDestination(googleDriveDownloadDestination);
		}
		
		return downloadTaskResult;
	};

	// HpcCollectionDownloadTask table to object mapper.
	private RowMapper<HpcCollectionDownloadTask> collectionDownloadTaskRowMapper = (rs, rowNum) -> {
		HpcCollectionDownloadTask collectionDownloadTask = new HpcCollectionDownloadTask();
		collectionDownloadTask.setId(rs.getString("ID"));
		collectionDownloadTask.setUserId(rs.getString("USER_ID"));
		collectionDownloadTask.setPath(rs.getString("PATH"));
		collectionDownloadTask.setConfigurationId(rs.getString("CONFIGURATION_ID"));
		collectionDownloadTask.setType(HpcDownloadTaskType.fromValue(rs.getString(("TYPE"))));
		collectionDownloadTask.setStatus(HpcCollectionDownloadTaskStatus.fromValue(rs.getString(("STATUS"))));
		collectionDownloadTask.setRetryTaskId(rs.getString("RETRY_TASK_ID"));
		collectionDownloadTask.setRetryUserId(rs.getString("RETRY_USER_ID"));
		collectionDownloadTask.setDataTransferRequestId(rs.getString("DATA_TRANSFER_REQUEST_ID"));
		collectionDownloadTask.setDoc(rs.getString("DOC"));
		collectionDownloadTask.setPriority(rs.getInt("PRIORITY"));

		long totalBytesTransferred = rs.getLong("TOTAL_BYTES_TRANSFERRED");
		collectionDownloadTask.setTotalBytesTransferred(totalBytesTransferred > 0 ? totalBytesTransferred : null);

		String destinationType = rs.getString("DESTINATION_TYPE");
		if (destinationType != null)
			collectionDownloadTask.setDestinationType(HpcDataTransferType.fromValue(destinationType));
		String destinationLocationFileContainerId = rs.getString("DESTINATION_LOCATION_FILE_CONTAINER_ID");
		String destinationLocationFileId = rs.getString("DESTINATION_LOCATION_FILE_ID");

		HpcFileLocation destinationLocation = null;
		if (destinationLocationFileContainerId != null && destinationLocationFileId != null) {
			destinationLocation = new HpcFileLocation();
			destinationLocation.setFileContainerId(destinationLocationFileContainerId);
			destinationLocation.setFileId(destinationLocationFileId);
		}

		HpcS3Account s3Account = null;
		byte[] s3AccountAccessKey = rs.getBytes("S3_ACCOUNT_ACCESS_KEY");
		byte[] s3AccountSecretKey = rs.getBytes("S3_ACCOUNT_SECRET_KEY");
		if (s3AccountAccessKey != null && s3AccountSecretKey != null) {
			s3Account = new HpcS3Account();
			s3Account.setAccessKey(this.encryptor.decrypt(s3AccountAccessKey));
			s3Account.setSecretKey(this.encryptor.decrypt(s3AccountSecretKey));
			s3Account.setRegion(rs.getString("S3_ACCOUNT_REGION"));
			s3Account.setUrl(rs.getString("S3_ACCOUNT_URL"));
			s3Account.setPathStyleAccessEnabled(rs.getBoolean("S3_ACCOUNT_PATH_STYLE_ACCESS_ENABLED"));
		}

		String googleDriveAccessToken = null;
		byte[] driveToken = rs.getBytes("GOOGLE_DRIVE_ACCESS_TOKEN");
		if (driveToken != null && driveToken.length > 0) {
			googleDriveAccessToken = encryptor.decrypt(driveToken);
		}

		String googleCloudAccessToken = null;
		byte[] cloudToken = rs.getBytes("GOOGLE_CLOUD_ACCESS_TOKEN");
		if (cloudToken != null && cloudToken.length > 0) {
			googleCloudAccessToken = encryptor.decrypt(cloudToken);
		}

		HpcAsperaAccount asperaAccount = null;
		byte[] asperaAccountUser = rs.getBytes("ASPERA_ACCOUNT_USER");
		byte[] asperaAccountPassword = rs.getBytes("ASPERA_ACCOUNT_PASSWORD");
		String asperaAccountHost = rs.getString("ASPERA_ACCOUNT_HOST");
		if (asperaAccountUser != null && asperaAccountPassword != null && asperaAccountHost != null) {
			asperaAccount = new HpcAsperaAccount();
			asperaAccount.setUser(encryptor.decrypt(asperaAccountUser));
			asperaAccount.setPassword(encryptor.decrypt(asperaAccountPassword));
			asperaAccount.setHost(asperaAccountHost);
		}

		String boxAccessToken = null;
		byte[] accessToken = rs.getBytes("BOX_ACCESS_TOKEN");
		if (accessToken != null) {
			boxAccessToken = encryptor.decrypt(accessToken);
		}
		String boxRefreshToken = null;
		byte[] refreshToken = rs.getBytes("BOX_REFRESH_TOKEN");
		if (refreshToken != null) {
			boxRefreshToken = encryptor.decrypt(refreshToken);
		}

		if (s3Account != null) {
			HpcS3DownloadDestination s3DownloadDestination = new HpcS3DownloadDestination();
			s3DownloadDestination.setDestinationLocation(destinationLocation);
			s3DownloadDestination.setAccount(s3Account);
			collectionDownloadTask.setS3DownloadDestination(s3DownloadDestination);
			if (destinationType == null) {
				collectionDownloadTask.setDestinationType(HpcDataTransferType.S_3);
			}

		} else if (googleDriveAccessToken != null) {
			HpcGoogleDownloadDestination googleDriveDownloadDestination = new HpcGoogleDownloadDestination();
			googleDriveDownloadDestination.setDestinationLocation(destinationLocation);
			googleDriveDownloadDestination.setAccessToken(googleDriveAccessToken);
			collectionDownloadTask.setGoogleDriveDownloadDestination(googleDriveDownloadDestination);
			if (destinationType == null) {
				collectionDownloadTask.setDestinationType(HpcDataTransferType.GOOGLE_DRIVE);
			}

		} else if (googleCloudAccessToken != null) {
			HpcGoogleDownloadDestination googleCloudStorageDownloadDestination = new HpcGoogleDownloadDestination();
			googleCloudStorageDownloadDestination.setDestinationLocation(destinationLocation);
			googleCloudStorageDownloadDestination.setAccessToken(googleCloudAccessToken);
			collectionDownloadTask.setGoogleCloudStorageDownloadDestination(googleCloudStorageDownloadDestination);
			if (destinationType == null) {
				collectionDownloadTask.setDestinationType(HpcDataTransferType.GOOGLE_CLOUD_STORAGE);
			}

		} else if (asperaAccount != null) {
			HpcAsperaDownloadDestination asperaDownloadDestination = new HpcAsperaDownloadDestination();
			asperaDownloadDestination.setDestinationLocation(destinationLocation);
			asperaDownloadDestination.setAccount(asperaAccount);
			collectionDownloadTask.setAsperaDownloadDestination(asperaDownloadDestination);
			if (destinationType == null) {
				collectionDownloadTask.setDestinationType(HpcDataTransferType.ASPERA);
			}

		} else if (boxAccessToken != null && boxRefreshToken != null) {
			HpcBoxDownloadDestination boxDownloadDestination = new HpcBoxDownloadDestination();
			boxDownloadDestination.setDestinationLocation(destinationLocation);
			boxDownloadDestination.setAccessToken(boxAccessToken);
			boxDownloadDestination.setRefreshToken(boxRefreshToken);
			collectionDownloadTask.setBoxDownloadDestination(boxDownloadDestination);
			if (destinationType == null) {
				collectionDownloadTask.setDestinationType(HpcDataTransferType.BOX);
			}

		} else {
			HpcGlobusDownloadDestination globusDownloadDestination = new HpcGlobusDownloadDestination();
			globusDownloadDestination.setDestinationLocation(destinationLocation);
			globusDownloadDestination.setDestinationOverwrite(rs.getBoolean("DESTINATION_OVERWRITE"));
			collectionDownloadTask.setGlobusDownloadDestination(globusDownloadDestination);
			if (destinationType == null) {
				collectionDownloadTask.setDestinationType(HpcDataTransferType.GLOBUS);
			}
		}

		collectionDownloadTask.setAppendPathToDownloadDestination(rs.getBoolean("APPEND_PATH_TO_DOWNLOAD_DESTINATION"));
		collectionDownloadTask.setAppendCollectionNameToDownloadDestination(
				rs.getBoolean("APPEND_COLLECTION_NAME_TO_DOWNLOAD_DESTINATION"));
		collectionDownloadTask.getItems().addAll(fromJSON(rs.getString("ITEMS")));

		Calendar created = Calendar.getInstance();
		created.setTime(rs.getTimestamp("CREATED"));
		collectionDownloadTask.setCreated(created);

		collectionDownloadTask.getDataObjectPaths().addAll(fromPathsString(rs.getString("DATA_OBJECT_PATHS")));
		collectionDownloadTask.getCollectionPaths().addAll(fromPathsString(rs.getString("COLLECTION_PATHS")));

		return collectionDownloadTask;
	};

	// HpcUserDownloadRequest table to object mapper.
	private RowMapper<HpcUserDownloadRequest> userDownloadRequestRowMapper = (rs, rowNum) -> {
		HpcUserDownloadRequest userDownloadRequest = new HpcUserDownloadRequest();
		userDownloadRequest.setTaskId(rs.getString("ID"));
		userDownloadRequest.setPath(rs.getString("PATH"));
		userDownloadRequest.setType(HpcDownloadTaskType.fromValue(rs.getString(("TYPE"))));

		if (hasColumnWithValue(rs, "STATUS")) {
			userDownloadRequest.setStatus(rs.getString(("STATUS")));

			if (userDownloadRequest.getStatus().equals("RECEIVED")) {
				// If this is a two hop data object download where the 1st hop (staging)
				// is done and the 2nd hop has not started, OR
				// this is a two hop collection download where the 1st hop is not done
				// but the 2nd hop has already started then show the status as IN_PROGRESS
				if ((hasColumnWithValue(rs, "STAGING_PERCENT_COMPLETE") && rs.getInt("STAGING_PERCENT_COMPLETE") == 100)
						|| (hasColumnWithValue(rs, "TOTAL_BYTES_TRANSFERRED")
								&& rs.getLong("TOTAL_BYTES_TRANSFERRED") > 0)) {
					userDownloadRequest.setStatus("IN_PROGRESS");
				}
			}
		}

		if (rs.getObject("RESULT") != null) {
			userDownloadRequest.setResult(HpcDownloadResult.fromValue(rs.getString("RESULT")));
		}
		if (rs.getObject("MESSAGE") != null) {
			userDownloadRequest.setMessage(rs.getString("MESSAGE"));
		}
		if (rs.getObject("USER_ID") != null) {
			userDownloadRequest.setUserId(rs.getString("USER_ID"));
		}
		Calendar created = Calendar.getInstance();
		created.setTime(rs.getTimestamp("CREATED"));
		userDownloadRequest.setCreated(created);

		if (rs.getTimestamp("COMPLETED") != null) {
			Calendar completed = Calendar.getInstance();
			completed.setTime(rs.getTimestamp("COMPLETED"));
			userDownloadRequest.setCompleted(completed);
		}
		if (rs.getObject("DESTINATION_TYPE") != null) {
			userDownloadRequest.setDestinationType(HpcDataTransferType.fromValue(rs.getString("DESTINATION_TYPE")));
		}
		if (rs.getObject("RETRY_USER_ID") != null) {
			userDownloadRequest.setRetryUserId(rs.getString("RETRY_USER_ID"));
		}
		userDownloadRequest.getItems().addAll(fromJSON(rs.getString("ITEMS")));

		return userDownloadRequest;
	};

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcDataDownloadDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataDownloadDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void createDataObjectDownloadTask(HpcDataObjectDownloadTask dataObjectDownloadTask) throws HpcException {
		try {
			if (dataObjectDownloadTask.getId() != null) {
				throw new HpcException("Task ID provided in download task creation: " + dataObjectDownloadTask.getId(),
						HpcErrorType.UNEXPECTED_ERROR);
			}
			dataObjectDownloadTask.setId(UUID.randomUUID().toString());

			HpcFileLocation destinationLocation = null;
			byte[] s3AccountAccessKey = null;
			byte[] s3AccountSecretKey = null;
			String s3AccountRegion = null;
			String s3AccountUrl = null;
			Boolean s3AccountPathStyleAccessEnabled = null;
			byte[] googleAccessToken = null;
			byte[] asperaAccountUser = null;
			byte[] asperaAccountPassword = null;
			String asperaAccountHost = null;
			byte[] boxAccessToken = null;
			byte[] boxRefreshToken = null;
			if (dataObjectDownloadTask.getGlobusDownloadDestination() != null) {
				destinationLocation = dataObjectDownloadTask.getGlobusDownloadDestination().getDestinationLocation();
			} else if (dataObjectDownloadTask.getS3DownloadDestination() != null) {
				destinationLocation = dataObjectDownloadTask.getS3DownloadDestination().getDestinationLocation();
				HpcS3Account s3Account = dataObjectDownloadTask.getS3DownloadDestination().getAccount();
				s3AccountAccessKey = encryptor.encrypt(s3Account.getAccessKey());
				s3AccountSecretKey = encryptor.encrypt(s3Account.getSecretKey());
				s3AccountRegion = s3Account.getRegion();
				s3AccountUrl = s3Account.getUrl();
				s3AccountPathStyleAccessEnabled = s3Account.getPathStyleAccessEnabled();
			} else if (dataObjectDownloadTask.getGoogleDriveDownloadDestination() != null) {
				destinationLocation = dataObjectDownloadTask.getGoogleDriveDownloadDestination()
						.getDestinationLocation();
				googleAccessToken = encryptor
						.encrypt(dataObjectDownloadTask.getGoogleDriveDownloadDestination().getAccessToken());
			} else if (dataObjectDownloadTask.getGoogleCloudStorageDownloadDestination() != null) {
				destinationLocation = dataObjectDownloadTask.getGoogleCloudStorageDownloadDestination()
						.getDestinationLocation();
				googleAccessToken = encryptor
						.encrypt(dataObjectDownloadTask.getGoogleCloudStorageDownloadDestination().getAccessToken());

			} else if (dataObjectDownloadTask.getAsperaDownloadDestination() != null) {
				destinationLocation = dataObjectDownloadTask.getAsperaDownloadDestination().getDestinationLocation();
				HpcAsperaAccount asperaAccount = dataObjectDownloadTask.getAsperaDownloadDestination().getAccount();
				asperaAccountUser = encryptor.encrypt(asperaAccount.getUser());
				asperaAccountPassword = encryptor.encrypt(asperaAccount.getPassword());
				asperaAccountHost = asperaAccount.getHost();

			} else if (dataObjectDownloadTask.getBoxDownloadDestination() != null) {
				destinationLocation = dataObjectDownloadTask.getBoxDownloadDestination().getDestinationLocation();
				boxAccessToken = encryptor.encrypt(dataObjectDownloadTask.getBoxDownloadDestination().getAccessToken());
				boxRefreshToken = encryptor
						.encrypt(dataObjectDownloadTask.getBoxDownloadDestination().getRefreshToken());

			} else {
				throw new HpcException("No download destination in a download task", HpcErrorType.UNEXPECTED_ERROR);
			}
			
			if(StringUtils.isNotEmpty(dataObjectDownloadTask.getCollectionDownloadTaskId())) {
			    // Get the collection download task priority
			    dataObjectDownloadTask.setPriority(getCollectionDownloadTaskPriority(dataObjectDownloadTask.getCollectionDownloadTaskId()));
			}

			jdbcTemplate.update(CREATE_DATA_OBJECT_DOWNLOAD_TASK_SQL, dataObjectDownloadTask.getId(),
					dataObjectDownloadTask.getUserId(), dataObjectDownloadTask.getPath(),
					dataObjectDownloadTask.getConfigurationId(), dataObjectDownloadTask.getS3ArchiveConfigurationId(),
					dataObjectDownloadTask.getDataTransferRequestId(),
					dataObjectDownloadTask.getDataTransferType().value(),
					dataObjectDownloadTask.getDataTransferStatus().value(),
					dataObjectDownloadTask.getDownloadFilePath(),
					dataObjectDownloadTask.getArchiveLocation().getFileContainerId(),
					dataObjectDownloadTask.getArchiveLocation().getFileId(), destinationLocation.getFileContainerId(),
					destinationLocation.getFileId(), dataObjectDownloadTask.getDestinationType().value(),
					s3AccountAccessKey, s3AccountSecretKey, s3AccountRegion, s3AccountUrl,
					s3AccountPathStyleAccessEnabled, googleAccessToken, asperaAccountUser, asperaAccountPassword,
					asperaAccountHost, boxAccessToken, boxRefreshToken, dataObjectDownloadTask.getCompletionEvent(),
					dataObjectDownloadTask.getCollectionDownloadTaskId(), dataObjectDownloadTask.getPercentComplete(),
					dataObjectDownloadTask.getStagingPercentComplete(), dataObjectDownloadTask.getSize(),
					dataObjectDownloadTask.getCreated(), dataObjectDownloadTask.getProcessed(),
					Optional.ofNullable(dataObjectDownloadTask.getInProcess()).orElse(false),
					Optional.ofNullable(dataObjectDownloadTask.getRestoreRequested()).orElse(false),
					dataObjectDownloadTask.getS3DownloadTaskServerId(), dataObjectDownloadTask.getFirstHopRetried(),
					dataObjectDownloadTask.getRetryTaskId(), dataObjectDownloadTask.getRetryUserId(),
					dataObjectDownloadTask.getDoc(),
					dataObjectDownloadTask.getPriority());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to create a data object download task: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public boolean updateDataObjectDownloadTask(HpcDataObjectDownloadTask dataObjectDownloadTask) throws HpcException {
		try {
			if (dataObjectDownloadTask.getId() == null) {
				throw new HpcException("Task ID not provided in download task update", HpcErrorType.UNEXPECTED_ERROR);
			}

			HpcFileLocation destinationLocation = null;
			byte[] s3AccountAccessKey = null;
			byte[] s3AccountSecretKey = null;
			String s3AccountRegion = null;
			String s3AccountUrl = null;
			Boolean s3AccountPathStyleAccessEnabled = null;
			byte[] googleAccessToken = null;
			byte[] asperaAccountUser = null;
			byte[] asperaAccountPassword = null;
			String asperaAccountHost = null;
			byte[] boxAccessToken = null;
			byte[] boxRefreshToken = null;

			if (dataObjectDownloadTask.getGlobusDownloadDestination() != null) {
				destinationLocation = dataObjectDownloadTask.getGlobusDownloadDestination().getDestinationLocation();
			} else if (dataObjectDownloadTask.getS3DownloadDestination() != null) {
				destinationLocation = dataObjectDownloadTask.getS3DownloadDestination().getDestinationLocation();
				HpcS3Account s3Account = dataObjectDownloadTask.getS3DownloadDestination().getAccount();
				s3AccountAccessKey = encryptor.encrypt(s3Account.getAccessKey());
				s3AccountSecretKey = encryptor.encrypt(s3Account.getSecretKey());
				s3AccountRegion = s3Account.getRegion();
				s3AccountUrl = s3Account.getUrl();
				s3AccountPathStyleAccessEnabled = s3Account.getPathStyleAccessEnabled();
			} else if (dataObjectDownloadTask.getGoogleDriveDownloadDestination() != null) {
				destinationLocation = dataObjectDownloadTask.getGoogleDriveDownloadDestination()
						.getDestinationLocation();
				googleAccessToken = encryptor
						.encrypt(dataObjectDownloadTask.getGoogleDriveDownloadDestination().getAccessToken());
			} else if (dataObjectDownloadTask.getGoogleCloudStorageDownloadDestination() != null) {
				destinationLocation = dataObjectDownloadTask.getGoogleCloudStorageDownloadDestination()
						.getDestinationLocation();
				googleAccessToken = encryptor
						.encrypt(dataObjectDownloadTask.getGoogleCloudStorageDownloadDestination().getAccessToken());
			} else if (dataObjectDownloadTask.getAsperaDownloadDestination() != null) {
				destinationLocation = dataObjectDownloadTask.getAsperaDownloadDestination().getDestinationLocation();
				HpcAsperaAccount asperaAccount = dataObjectDownloadTask.getAsperaDownloadDestination().getAccount();
				asperaAccountUser = encryptor.encrypt(asperaAccount.getUser());
				asperaAccountPassword = encryptor.encrypt(asperaAccount.getPassword());
				asperaAccountHost = asperaAccount.getHost();

			} else if (dataObjectDownloadTask.getBoxDownloadDestination() != null) {
				destinationLocation = dataObjectDownloadTask.getBoxDownloadDestination().getDestinationLocation();
				boxAccessToken = encryptor.encrypt(dataObjectDownloadTask.getBoxDownloadDestination().getAccessToken());
				boxRefreshToken = encryptor
						.encrypt(dataObjectDownloadTask.getBoxDownloadDestination().getRefreshToken());

			} else {
				throw new HpcException("No download destination in a download task", HpcErrorType.UNEXPECTED_ERROR);
			}

			return jdbcTemplate.update(UPDATE_DATA_OBJECT_DOWNLOAD_TASK_SQL, dataObjectDownloadTask.getUserId(),
					dataObjectDownloadTask.getPath(), dataObjectDownloadTask.getConfigurationId(),
					dataObjectDownloadTask.getS3ArchiveConfigurationId(),
					dataObjectDownloadTask.getDataTransferRequestId(),
					dataObjectDownloadTask.getDataTransferType().value(),
					dataObjectDownloadTask.getDataTransferStatus().value(),
					dataObjectDownloadTask.getDownloadFilePath(),
					dataObjectDownloadTask.getArchiveLocation().getFileContainerId(),
					dataObjectDownloadTask.getArchiveLocation().getFileId(), destinationLocation.getFileContainerId(),
					destinationLocation.getFileId(), dataObjectDownloadTask.getDestinationType().value(),
					s3AccountAccessKey, s3AccountSecretKey, s3AccountRegion, s3AccountUrl,
					s3AccountPathStyleAccessEnabled, googleAccessToken, asperaAccountUser, asperaAccountPassword,
					asperaAccountHost, boxAccessToken, boxRefreshToken, dataObjectDownloadTask.getCompletionEvent(),
					dataObjectDownloadTask.getCollectionDownloadTaskId(), dataObjectDownloadTask.getPercentComplete(),
					dataObjectDownloadTask.getStagingPercentComplete(), dataObjectDownloadTask.getSize(),
					dataObjectDownloadTask.getCreated(), dataObjectDownloadTask.getProcessed(),
					Optional.ofNullable(dataObjectDownloadTask.getInProcess()).orElse(false),
					Optional.ofNullable(dataObjectDownloadTask.getRestoreRequested()).orElse(false),
					dataObjectDownloadTask.getS3DownloadTaskServerId(), dataObjectDownloadTask.getFirstHopRetried(),
					dataObjectDownloadTask.getRetryTaskId(), dataObjectDownloadTask.getRetryUserId(),
					dataObjectDownloadTask.getId()) > 0;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to create a data object download task: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public HpcDataObjectDownloadTask getDataObjectDownloadTask(String id) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_DATA_OBJECT_DOWNLOAD_TASK_SQL, dataObjectDownloadTaskRowMapper, id);

		} catch (IncorrectResultSizeDataAccessException irse) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get a data object download task: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void deleteDataObjectDownloadTask(String id) throws HpcException {
		try {
			jdbcTemplate.update(DELETE_DATA_OBJECT_DOWNLOAD_TASK_SQL, id);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to delete a data object download task: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void updateDataObjectDownloadTasksStatus(String collectionDownloadTaskId,
			List<HpcDataObjectDownloadTaskStatusFilter> filters, HpcDataTransferDownloadStatus toStatus)
			throws HpcException {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		// Add the task-id and to-status values to the query.
		sqlQueryBuilder.append(UPDATE_DATA_OBJECTS_DOWNLOAD_TASK_STATUS_SQL);
		args.add(toStatus.value());
		args.add(collectionDownloadTaskId);

		// Add each pair of the from-status and destination-type as a filter to the
		// query.
		if (filters != null && !filters.isEmpty()) {
			sqlQueryBuilder.append(" and (1 = 0");
			filters.forEach(filter -> {
				sqlQueryBuilder.append(UPDATE_DATA_OBJECT_DOWNLOAD_TASK_STATUS_FILTER);
				args.add(filter.getStatus().value());
				args.add(filter.getDestination().value());
			});
			sqlQueryBuilder.append(")");
		}

		// Execute the query.
		try {
			jdbcTemplate.update(sqlQueryBuilder.toString(), args.toArray());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to update a data object download task status: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void updateDataObjectDownloadTaskPriority(String id, int priority) throws HpcException {
		try {
			jdbcTemplate.update(UPDATE_DATA_OBJECT_DOWNLOAD_TASK_PRIORITY_SQL, priority, id);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to update a data object download task priority with task id: " + id + " " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}
	
	@Override
	public void updateDataObjectsDownloadTaskPriority(String collectionDownloadTaskId, int priority) throws HpcException {
		try {
			jdbcTemplate.update(UPDATE_DATA_OBJECTS_DOWNLOAD_TASK_PRIORITY_SQL, priority, collectionDownloadTaskId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to update a data objects download task priority with collection task id: " + collectionDownloadTaskId + " " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}
	
	@Override
	public HpcDataTransferDownloadStatus getDataObjectDownloadTaskStatus(String id) throws HpcException {
		try {
			String taskStatusStr = jdbcTemplate.queryForObject(GET_DATA_OBJECT_DOWNLOAD_TASK_STATUS_SQL, String.class,
					id);
			return taskStatusStr != null ? HpcDataTransferDownloadStatus.fromValue(taskStatusStr) : null;

		} catch (IncorrectResultSizeDataAccessException irse) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get a data object download task status : " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcDataObjectDownloadTask> getDataObjectDownloadTasks() throws HpcException {
		try {
			return jdbcTemplate.query(GET_DATA_OBJECT_DOWNLOAD_TASKS_SQL, dataObjectDownloadTaskRowMapper);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data object download tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcDataObjectDownloadTask> getDataObjectDownloadTaskByStatus(
			HpcDataTransferDownloadStatus dataTransferStatus) throws HpcException {
		try {
			return jdbcTemplate.query(GET_ALL_DATA_OBJECT_DOWNLOAD_TASK_BY_STATUS_SQL, dataObjectDownloadTaskRowMapper,
					dataTransferStatus.value());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data object download tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcDataObjectDownloadTask> getDataObjectDownloadTaskByCollectionDownloadTaskId(
			String collectionDownloadTaskId) throws HpcException {
		try {
			return jdbcTemplate.query(GET_ALL_DATA_OBJECT_DOWNLOAD_TASK_BY_COLLECTION_DOWNLOAD_TASK_ID_SQL,
					dataObjectDownloadTaskRowMapper, collectionDownloadTaskId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data object download tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcDownloadTaskResult> getDataObjectDownloadTaskResultByCollectionDownloadTaskId(
			String collectionDownloadTaskId) throws HpcException {
		try {
			return jdbcTemplate.query(GET_ALL_DATA_OBJECT_DOWNLOAD_RESULTS_BY_COLLECTION_DOWNLOAD_TASK_ID_SQL,
					downloadTaskResultRowMapper, collectionDownloadTaskId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data object download task results: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcDataObjectDownloadTask> getNextDataObjectDownloadTask(
			HpcDataTransferDownloadStatus dataTransferStatus, HpcDataTransferType dataTransferType, Date processed)
			throws HpcException {
		try {
			Timestamp timestamp = new Timestamp(processed.getTime());
			if (dataTransferType != null) {
				return jdbcTemplate.query(GET_DATA_OBJECT_DOWNLOAD_TASK_BY_STATUS_AND_TYPE_SQL,
						dataObjectDownloadTaskRowMapper, dataTransferStatus.value(), dataTransferType.value(),
						timestamp);
			}
			return jdbcTemplate.query(GET_DATA_OBJECT_DOWNLOAD_TASK_BY_STATUS_SQL, dataObjectDownloadTaskRowMapper,
					dataTransferStatus.value(), timestamp);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data object download tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	@Transactional
	public boolean setDataObjectDownloadTaskInProcess(String id, boolean inProcess, String s3DownloadTaskServerId)
			throws HpcException {
		try {
			jdbcTemplate.queryForObject(SELECT_FOR_UPDATE_DATA_OBJECT_DOWNLOAD_TASK_IN_PROCESS_SQL,
					dataObjectDownloadTaskRowMapper, id, inProcess);

			boolean updated = jdbcTemplate.update(SET_DATA_OBJECT_DOWNLOAD_TASK_IN_PROCESS_SQL, inProcess,
					s3DownloadTaskServerId, id, inProcess) > 0;
			if (!updated) {
				logger.info("download task: [taskId={}] - Row not updated after locking. inProcess={}", id, inProcess);
			}
			return updated;

		} catch (IncorrectResultSizeDataAccessException irse) {
			logger.info("download task: [taskId={}] - Row not updated (locked or not found): {}", id,
					irse.getMessage());
			return false;

		} catch (DataAccessException e) {
			logger.error("download task: [taskId={}] - Failed to update row: {}", id, e);
			return false;
		}
	}

	@Override
	public void resetDataObjectDownloadTaskInProcess(String s3DownloadTaskServerId) throws HpcException {
		try {
			jdbcTemplate.update(RESET_DATA_OBJECT_DOWNLOAD_TASK_IN_PROCESS_SQL, s3DownloadTaskServerId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to reset data object download tasks in-process value: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void setDataObjectDownloadTaskProcessed(String id, Calendar processed) throws HpcException {
		try {
			jdbcTemplate.update(SET_DATA_OBJECT_DOWNLOAD_TASK_PROCESSED_SQL, processed, id);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to set a data object download task w/ processed value: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void upsertDownloadTaskResult(HpcDownloadTaskResult taskResult) throws HpcException {
		try {
			String dataTransferType = taskResult.getDataTransferType() != null
					? taskResult.getDataTransferType().value()
					: null;
			String destinationType = taskResult.getDestinationType() != null ? taskResult.getDestinationType().value()
					: null;
			String collectionPaths = !taskResult.getCollectionPaths().isEmpty()
					? toPathsString(taskResult.getCollectionPaths())
					: null;
			String archiveLocationFileContainerId = taskResult.getArchiveLocation() != null
					? taskResult.getArchiveLocation().getFileContainerId()
					: null;
			String archiveLocationFileId = taskResult.getArchiveLocation() != null
					? taskResult.getArchiveLocation().getFileId()
					: null;
			byte[] googleDriveAccessToken = taskResult.getGoogleDriveDownloadDestination() != null
					? encryptor.encrypt(taskResult.getGoogleDriveDownloadDestination().getAccessToken())
					: null;

			jdbcTemplate.update(UPSERT_DOWNLOAD_TASK_RESULT_SQL, taskResult.getId(), taskResult.getUserId(),
					taskResult.getPath(), taskResult.getDataTransferRequestId(), dataTransferType,
					archiveLocationFileContainerId, archiveLocationFileId,
					taskResult.getDestinationLocation().getFileContainerId(),
					taskResult.getDestinationLocation().getFileContainerName(),
					taskResult.getDestinationLocation().getFileId(), destinationType, taskResult.getResult().value(),
					taskResult.getType().value(), taskResult.getMessage(), taskResult.getCompletionEvent(),
					taskResult.getCollectionDownloadTaskId(), taskResult.getEffectiveTransferSpeed(),
					taskResult.getSize(), taskResult.getCreated(), taskResult.getCompleted(),
					Optional.ofNullable(taskResult.getRestoreRequested()).orElse(false), taskResult.getRetryTaskId(),
					taskResult.getRetryUserId(), taskResult.getFirstHopRetried(), taskResult.getDoc(), googleDriveAccessToken,
					taskResult.getId(), taskResult.getUserId(), taskResult.getPath(),
					taskResult.getDataTransferRequestId(), dataTransferType, archiveLocationFileContainerId,
					archiveLocationFileId, taskResult.getDestinationLocation().getFileContainerId(),
					taskResult.getDestinationLocation().getFileContainerName(),
					taskResult.getDestinationLocation().getFileId(), destinationType, taskResult.getResult().value(),
					taskResult.getType().value(), taskResult.getMessage(), taskResult.getCompletionEvent(),
					taskResult.getCollectionDownloadTaskId(), taskResult.getEffectiveTransferSpeed(),
					taskResult.getSize(), taskResult.getCreated(), taskResult.getCompleted(),
					Optional.ofNullable(taskResult.getRestoreRequested()).orElse(false), taskResult.getRetryTaskId(),
					taskResult.getRetryUserId(), taskResult.getFirstHopRetried(), taskResult.getDoc(), googleDriveAccessToken);

			jdbcTemplate.update(UPDATE_DOWNLOAD_TASK_RESULT_CLOBS_SQL,
					new Object[] { new SqlLobValue(toJSON(taskResult.getItems()), lobHandler),
							new SqlLobValue(collectionPaths, lobHandler), taskResult.getId() },
					new int[] { Types.CLOB, Types.CLOB, Types.VARCHAR });

		} catch (DataAccessException e) {
			throw new HpcException("Failed to upsert a download task result: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public HpcDownloadTaskResult getDownloadTaskResult(String id, HpcDownloadTaskType taskType) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_DOWNLOAD_TASK_RESULT_SQL, downloadTaskResultRowMapper, id,
					taskType.value());

		} catch (IncorrectResultSizeDataAccessException irse) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get a download task result: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void upsertCollectionDownloadTask(HpcCollectionDownloadTask collectionDownloadTask) throws HpcException {
		try {
			if (collectionDownloadTask.getId() == null) {
				collectionDownloadTask.setId(UUID.randomUUID().toString());
			}

			String dataObjectPaths = !collectionDownloadTask.getDataObjectPaths().isEmpty()
					? toPathsString(collectionDownloadTask.getDataObjectPaths())
					: null;
			String collectionPaths = !collectionDownloadTask.getCollectionPaths().isEmpty()
					? toPathsString(collectionDownloadTask.getCollectionPaths())
					: null;

			HpcFileLocation destinationLocation = null;
			Boolean destinationOverwrite = null;
			byte[] s3AccountAccessKey = null;
			byte[] s3AccountSecretKey = null;
			String s3AccountRegion = null;
			String s3AccountUrl = null;
			Boolean s3AccountPathStyleAccessEnabled = null;
			String destinationType = null;
			byte[] asperaAccountUser = null;
			byte[] asperaAccountPassword = null;
			String asperaAccountHost = null;
			byte[] googleDriveAccessToken = null;
			byte[] googleCloudAccessToken = null;
			byte[] boxAccessToken = null;
			byte[] boxRefreshToken = null;

			if (collectionDownloadTask.getGlobusDownloadDestination() != null) {
				destinationLocation = collectionDownloadTask.getGlobusDownloadDestination().getDestinationLocation();
				destinationOverwrite = collectionDownloadTask.getGlobusDownloadDestination().getDestinationOverwrite();
				destinationType = HpcDataTransferType.GLOBUS.name();
			} else if (collectionDownloadTask.getS3DownloadDestination() != null) {
				destinationLocation = collectionDownloadTask.getS3DownloadDestination().getDestinationLocation();
				HpcS3Account s3Account = collectionDownloadTask.getS3DownloadDestination().getAccount();
				s3AccountAccessKey = encryptor.encrypt(s3Account.getAccessKey());
				s3AccountSecretKey = encryptor.encrypt(s3Account.getSecretKey());
				s3AccountRegion = s3Account.getRegion();
				s3AccountUrl = s3Account.getUrl();
				s3AccountPathStyleAccessEnabled = s3Account.getPathStyleAccessEnabled();
				destinationType = HpcDataTransferType.S_3.name();
			} else if (collectionDownloadTask.getGoogleDriveDownloadDestination() != null) {
				destinationLocation = collectionDownloadTask.getGoogleDriveDownloadDestination()
						.getDestinationLocation();
				googleDriveAccessToken = encryptor
						.encrypt(collectionDownloadTask.getGoogleDriveDownloadDestination().getAccessToken());
				destinationType = HpcDataTransferType.GOOGLE_DRIVE.name();
			} else if (collectionDownloadTask.getGoogleCloudStorageDownloadDestination() != null) {
				destinationLocation = collectionDownloadTask.getGoogleCloudStorageDownloadDestination()
						.getDestinationLocation();
				googleCloudAccessToken = encryptor
						.encrypt(collectionDownloadTask.getGoogleCloudStorageDownloadDestination().getAccessToken());
				destinationType = HpcDataTransferType.GOOGLE_CLOUD_STORAGE.name();
			} else if (collectionDownloadTask.getAsperaDownloadDestination() != null) {
				destinationLocation = collectionDownloadTask.getAsperaDownloadDestination().getDestinationLocation();
				HpcAsperaAccount asperaAccount = collectionDownloadTask.getAsperaDownloadDestination().getAccount();
				asperaAccountUser = encryptor.encrypt(asperaAccount.getUser());
				asperaAccountPassword = encryptor.encrypt(asperaAccount.getPassword());
				asperaAccountHost = asperaAccount.getHost();
				destinationType = HpcDataTransferType.ASPERA.name();
			} else if (collectionDownloadTask.getBoxDownloadDestination() != null) {
				destinationLocation = collectionDownloadTask.getBoxDownloadDestination().getDestinationLocation();
				boxAccessToken = encryptor.encrypt(collectionDownloadTask.getBoxDownloadDestination().getAccessToken());
				boxRefreshToken = encryptor
						.encrypt(collectionDownloadTask.getBoxDownloadDestination().getRefreshToken());
				destinationType = HpcDataTransferType.BOX.name();

			} else {
				throw new HpcException("No download destination in a collection download task",
						HpcErrorType.UNEXPECTED_ERROR);
			}

			jdbcTemplate.update(UPSERT_COLLECTION_DOWNLOAD_TASK_SQL, collectionDownloadTask.getId(),
					collectionDownloadTask.getUserId(), collectionDownloadTask.getPath(),
					collectionDownloadTask.getConfigurationId(), destinationLocation.getFileContainerId(),
					destinationLocation.getFileId(), destinationOverwrite, s3AccountAccessKey, s3AccountSecretKey,
					s3AccountRegion, s3AccountUrl, s3AccountPathStyleAccessEnabled, googleDriveAccessToken,
					googleCloudAccessToken, asperaAccountUser, asperaAccountPassword, asperaAccountHost, boxAccessToken,
					boxRefreshToken, collectionDownloadTask.getAppendPathToDownloadDestination(),
					collectionDownloadTask.getAppendCollectionNameToDownloadDestination(),
					collectionDownloadTask.getStatus().value(), collectionDownloadTask.getType().value(),
					collectionDownloadTask.getCreated(), collectionDownloadTask.getRetryTaskId(),
					collectionDownloadTask.getRetryUserId(), collectionDownloadTask.getDataTransferRequestId(),
					destinationType, collectionDownloadTask.getDoc(), collectionDownloadTask.getId(),
					collectionDownloadTask.getUserId(), collectionDownloadTask.getPath(),
					collectionDownloadTask.getConfigurationId(), destinationLocation.getFileContainerId(),
					destinationLocation.getFileId(), destinationOverwrite, s3AccountAccessKey, s3AccountSecretKey,
					s3AccountRegion, s3AccountUrl, s3AccountPathStyleAccessEnabled, googleDriveAccessToken,
					googleCloudAccessToken, asperaAccountUser, asperaAccountPassword, asperaAccountHost, boxAccessToken,
					boxRefreshToken, collectionDownloadTask.getAppendPathToDownloadDestination(),
					collectionDownloadTask.getAppendCollectionNameToDownloadDestination(),
					collectionDownloadTask.getStatus().value(), collectionDownloadTask.getType().value(),
					collectionDownloadTask.getCreated(), collectionDownloadTask.getRetryTaskId(),
					collectionDownloadTask.getRetryUserId(), collectionDownloadTask.getDataTransferRequestId(),
					destinationType, collectionDownloadTask.getDoc());

			jdbcTemplate.update(UPDATE_COLLECTION_DOWNLOAD_TASK_CLOBS_SQL,
					new Object[] { new SqlLobValue(toJSON(collectionDownloadTask.getItems()), lobHandler),
							new SqlLobValue(dataObjectPaths, lobHandler), new SqlLobValue(collectionPaths, lobHandler),
							collectionDownloadTask.getId() },
					new int[] { Types.CLOB, Types.CLOB, Types.CLOB, Types.VARCHAR });

		} catch (DataAccessException e) {
			throw new HpcException("Failed to upsert a collection download request: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public HpcCollectionDownloadTask getCollectionDownloadTask(String id) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_COLLECTION_DOWNLOAD_TASK_SQL, collectionDownloadTaskRowMapper, id);

		} catch (IncorrectResultSizeDataAccessException irse) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get a collection download task: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void deleteCollectionDownloadTask(String id) throws HpcException {
		try {
			jdbcTemplate.update(DELETE_COLLECTION_DOWNLOAD_TASK_SQL, id);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to delete a collection download task: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}
	
	@Override
	public void updateCollectionDownloadTaskPriority(String id, int priority) throws HpcException {
		try {
			jdbcTemplate.update(UPDATE_COLLECTION_DOWNLOAD_TASK_PRIORITY_SQL, priority, id);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to update a collection download task priority with ID: " + id + " " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcCollectionDownloadTask> getCollectionDownloadTasksInProcess() throws HpcException {
		try {
			return jdbcTemplate.query(GET_COLLECTION_DOWNLOAD_TASKS_IN_PROCESS_SQL, collectionDownloadTaskRowMapper);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get collection download tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus status)
			throws HpcException {
		try {
			return jdbcTemplate.query(GET_COLLECTION_DOWNLOAD_TASKS_BY_STATUS_SQL, collectionDownloadTaskRowMapper,
					status.value());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get collection download tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus status,
			boolean inProcess) throws HpcException {
		try {
			return jdbcTemplate.query(GET_COLLECTION_DOWNLOAD_TASKS_SQL, collectionDownloadTaskRowMapper,
					status.value(), inProcess);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get collection download tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public int getCollectionDownloadTasksCount(String userId, HpcCollectionDownloadTaskStatus status, boolean inProcess)
			throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_COLLECTION_DOWNLOAD_REQUESTS_COUNT_SQL, Integer.class, userId,
					status.value(), inProcess);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to count collection download requests: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public int getCollectionDownloadRequestsCountByPathAndEndpoint(String path, String endpoint) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_COLLECTION_DOWNLOAD_REQUESTS_COUNT_BY_PATH_AND_ENDPOINT_SQL,
					Integer.class, path, endpoint);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to count collection download requests: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public int getCollectionDownloadTasksCountByUserAndPath(String userId, String path, boolean inProcess)
			throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_COLLECTION_DOWNLOAD_TASKS_COUNT_BY_USER_AND_PATH_SQL, Integer.class,
					userId, path, inProcess);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to count collection download tasks for user " + userId + " and path " + path
					+ ": " + e.getMessage(), HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public int getCollectionDownloadTasksCountByUser(String userId, boolean inProcess) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_COLLECTION_DOWNLOAD_TASKS_COUNT_BY_USER_SQL, Integer.class, userId,
					inProcess);

		} catch (DataAccessException e) {
			throw new HpcException(
					"Failed to count collection download tasks for user " + userId + ": " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public int getDataObjectDownloadTasksCountByStatusAndType(HpcDataTransferType dataTransferType,
			HpcDataTransferType destinationType, HpcDataTransferDownloadStatus status, String s3DownloadTaskServerId)
			throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_DATA_OBJECT_DOWNLOAD_TASKS_COUNT_BY_STATUS_AND_TYPE_SQL,
					Integer.class, dataTransferType.value(), destinationType.value(), status.value(),
					s3DownloadTaskServerId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get inprocess data object download tasks count: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public int getGlobusDataObjectDownloadTasksCountInProgressForUserByPath(String userId, String path)
			throws HpcException {
		try {
			return jdbcTemplate.queryForObject(
					GET_GLOBUS_DATA_OBJECT_DOWNLOAD_TASKS_COUNT_IN_PROGRESS_FOR_USER_BY_PATH_SQL, Integer.class, userId,
					path);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get inprocess data object download tasks count: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void setCollectionDownloadTaskInProcess(String id, boolean inProcess) throws HpcException {
		try {
			jdbcTemplate.update(SET_COLLECTION_DOWNLOAD_TASK_IN_PROCESS_SQL, inProcess, id);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to set a collection download task w/ in-process value: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void resetCollectionDownloadTaskInProcess(String id) throws HpcException {
		try {
			jdbcTemplate.update(RESET_COLLECTION_DOWNLOAD_TASK_IN_PROCESS_SQL, id);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to reset collection download tasks in-process value: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void setCollectionDownloadTaskCancellationRequested(String id, boolean cancellationRequest)
			throws HpcException {
		try {
			jdbcTemplate.update(SET_COLLECTION_DOWNLOAD_TASK_CANCELLATION_REQUEST_SQL, cancellationRequest, id);

		} catch (DataAccessException e) {
			throw new HpcException(
					"Failed to set a collection download task w/ cancellation request: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public boolean getCollectionDownloadTaskCancellationRequested(String id) throws HpcException {
		try {
			Boolean cancellationRequested = jdbcTemplate
					.queryForObject(GET_COLLECTION_DOWNLOAD_TASK_CANCELLATION_REQUEST_SQL, Boolean.class, id);
			return cancellationRequested != null ? cancellationRequested : false;

		} catch (EmptyResultDataAccessException e) {
			// If it can not find the collection download task, it is cancelled and removed
			// from the table.
			return true;
		} catch (DataAccessException e) {
			throw new HpcException("Failed to get cancellation request of: " + id + " " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcUserDownloadRequest> getDataObjectDownloadRequests(String userId) throws HpcException {
		try {
			return jdbcTemplate.query(GET_DATA_OBJECT_DOWNLOAD_REQUESTS_SQL, userDownloadRequestRowMapper, userId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data object download requests: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcUserDownloadRequest> getCollectionDownloadRequests(String userId) throws HpcException {
		try {
			return jdbcTemplate.query(GET_COLLECTION_DOWNLOAD_REQUESTS_SQL, userDownloadRequestRowMapper, userId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get collection download requests: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcUserDownloadRequest> getDownloadResults(String userId, int offset, int limit) throws HpcException {
		try {
			return jdbcTemplate.query(GET_DOWNLOAD_RESULTS_SQL, userDownloadRequestRowMapper, userId, offset, limit);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get download results: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public int getDownloadResultsCount(String userId) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_DOWNLOAD_RESULTS_COUNT_SQL, Integer.class, userId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to count download results: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcUserDownloadRequest> getDataObjectDownloadRequestsForDoc(String doc, String userId)
			throws HpcException {
		try {
			return jdbcTemplate.query(GET_DATA_OBJECT_DOWNLOAD_REQUESTS_FOR_DOC_SQL, userDownloadRequestRowMapper, doc,
					userId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data object download requests: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcUserDownloadRequest> getAllDataObjectDownloadRequests() throws HpcException {
		try {
			return jdbcTemplate.query(GET_ALL_DATA_OBJECT_DOWNLOAD_REQUESTS_SQL, userDownloadRequestRowMapper);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data object download requests: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcUserDownloadRequest> getCollectionDownloadRequestsForDoc(String doc, String userId)
			throws HpcException {
		try {
			return jdbcTemplate.query(GET_COLLECTION_DOWNLOAD_REQUESTS_FOR_DOC_SQL, userDownloadRequestRowMapper, doc,
					userId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get collection download requests: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcUserDownloadRequest> getAllCollectionDownloadRequests() throws HpcException {
		try {
			return jdbcTemplate.query(GET_ALL_COLLECTION_DOWNLOAD_REQUESTS_SQL, userDownloadRequestRowMapper);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get collection download requests: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcUserDownloadRequest> getDownloadResultsForDoc(String doc, String userId, int offset, int limit)
			throws HpcException {
		try {
			return jdbcTemplate.query(GET_DOWNLOAD_RESULTS_FOR_DOC_SQL, userDownloadRequestRowMapper, doc, userId,
					offset, limit);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get download results: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcUserDownloadRequest> getAllDownloadResults(int offset, int limit) throws HpcException {
		try {
			return jdbcTemplate.query(GET_ALL_DOWNLOAD_RESULTS_SQL, userDownloadRequestRowMapper, offset, limit);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get download results: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public int getDownloadResultsCountForDoc(String doc, String userId) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_DOWNLOAD_RESULTS_COUNT_FOR_DOC_SQL, Integer.class, doc, userId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to count download results: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public int getAllDownloadResultsCount() throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_ALL_DOWNLOAD_RESULTS_COUNT_SQL, Integer.class);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to count download results: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public double getTotalDownloadsSize(String userId, HpcDataTransferDownloadStatus dataTransferDownloadStatus)
			throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_TOTAL_DOWNLOADS_SIZE_SQL, Double.class, userId,
					dataTransferDownloadStatus.value()) / Double.valueOf(FileUtils.ONE_GB);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to sum total downloads per user: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void updateTotalBytesTransferred(String collectionDownloadTaskId, long bytesTransferred)
			throws HpcException {
		try {
			jdbcTemplate.update(SELECT_FOR_UPDATE_TOTAL_BYTES_TRANSFERRED_SQL, collectionDownloadTaskId);
			int rowsUpdated = jdbcTemplate.update(UPDATE_TOTAL_BYTES_TRANSFERRED_SQL, bytesTransferred,
					collectionDownloadTaskId);
			if (rowsUpdated == 1) {
				logger.info(
						"download task: [taskId={}] - total bytes transferred incremented by {} while in RECEIVED state",
						collectionDownloadTaskId, bytesTransferred);
			}

		} catch (DataAccessException e) {
			logger.error("download task: [taskId={}] - failed to increment total bytes transferred",
					collectionDownloadTaskId, e);
		}
	}
	
	@Override
    public Integer getCollectionDownloadTaskPriority(String id) throws HpcException {
        try {
            return jdbcTemplate.queryForObject(GET_COLLECTION_DOWNLOAD_TASK_PRIORITY_SQL, Integer.class,
                    id);

        } catch (IncorrectResultSizeDataAccessException irse) {
            return null;

        } catch (DataAccessException e) {
            throw new HpcException("Failed to get a collection download task priority : " + e.getMessage(),
                    HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
        }
    }


	@Override
	public void removeGoogleAccessTokens(Integer googleAccessTokenRetentionPeriod) throws HpcException {
		try {
			jdbcTemplate.update(REMOVE_GOOGLE_ACCESS_TOKEN_FROM_DOWNLOAD_TASK_RESULT_SQL, googleAccessTokenRetentionPeriod);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to remove google access tokens retained for " + googleAccessTokenRetentionPeriod + " hours",
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}
	
	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Convert a list of collection download items into a JSON string.
	 *
	 * @param downloadItems The list of collection download items.
	 * @return A JSON representation of download items.
	 */
	@SuppressWarnings("unchecked")
	private String toJSON(List<HpcCollectionDownloadTaskItem> downloadItems) {
		JSONArray jsonDownloadItems = new JSONArray();
		for (HpcCollectionDownloadTaskItem downloadItem : downloadItems) {
			JSONObject jsonDownloadItem = new JSONObject();
			jsonDownloadItem.put("path", downloadItem.getPath());
			if (downloadItem.getCollectionPath() != null) {
				jsonDownloadItem.put("collectionPath", downloadItem.getCollectionPath());
			}
			if (downloadItem.getDataObjectDownloadTaskId() != null) {
				jsonDownloadItem.put("dataObjectDownloadTaskId", downloadItem.getDataObjectDownloadTaskId());
			}
			if (downloadItem.getMessage() != null) {
				jsonDownloadItem.put("message", downloadItem.getMessage());
			}
			if (downloadItem.getResult() != null) {
				jsonDownloadItem.put("result", downloadItem.getResult().value());
			}
			if (downloadItem.getArchiveLocation() != null) {
				jsonDownloadItem.put("archiveLocationFileContainerId",
						downloadItem.getArchiveLocation().getFileContainerId());
				jsonDownloadItem.put("archiveLocationFileId", downloadItem.getArchiveLocation().getFileId());
			}
			jsonDownloadItem.put("destinationLocationFileContainerId",
					downloadItem.getDestinationLocation().getFileContainerId());
			jsonDownloadItem.put("destinationLocationFileId", downloadItem.getDestinationLocation().getFileId());
			if (downloadItem.getEffectiveTransferSpeed() != null) {
				jsonDownloadItem.put("effectiveTransferSpeed", downloadItem.getEffectiveTransferSpeed());
			}
			if (downloadItem.getSize() != null) {
				jsonDownloadItem.put("size", downloadItem.getSize());
			}
			if (downloadItem.getPercentComplete() != null) {
				jsonDownloadItem.put("percentComplete", downloadItem.getPercentComplete());
			}
			if (downloadItem.getRestoreInProgress() != null) {
				jsonDownloadItem.put("restoreInProgress", downloadItem.getRestoreInProgress());
			}
			if (downloadItem.getStagingInProgress() != null) {
				jsonDownloadItem.put("stagingInProgress", downloadItem.getStagingInProgress());
			}
			if (downloadItem.getStagingPercentComplete() != null) {
				jsonDownloadItem.put("stagingPercentComplete", downloadItem.getStagingPercentComplete());
			}

			jsonDownloadItems.add(jsonDownloadItem);
		}

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("items", jsonDownloadItems);

		return jsonObj.toJSONString();
	}

	/**
	 * Convert JSON string to a list of collection download items
	 *
	 * @param jsonDownloadItemsStr The download items JSON string.
	 * @return A list of collection download items.
	 */
	@SuppressWarnings("unchecked")
	private List<HpcCollectionDownloadTaskItem> fromJSON(String jsonDownloadItemsStr) {
		List<HpcCollectionDownloadTaskItem> downloadItems = new ArrayList<>();
		if (StringUtils.isEmpty(jsonDownloadItemsStr)) {
			return downloadItems;
		}

		// Parse the JSON string.
		JSONObject jsonObj = null;
		try {
			jsonObj = (JSONObject) (new JSONParser().parse(jsonDownloadItemsStr));

		} catch (ParseException e) {
			return downloadItems;
		}

		// Map the download items.
		JSONArray jsonDownloadItems = (JSONArray) jsonObj.get("items");
		if (jsonDownloadItems != null) {
			Iterator<JSONObject> downloadItemIterator = jsonDownloadItems.iterator();
			while (downloadItemIterator.hasNext()) {
				JSONObject jsonDownloadItem = downloadItemIterator.next();
				HpcCollectionDownloadTaskItem downloadItem = new HpcCollectionDownloadTaskItem();
				downloadItem.setPath(jsonDownloadItem.get("path").toString());

				Object collectionPath = jsonDownloadItem.get("collectionPath");
				if (collectionPath != null) {
					downloadItem.setCollectionPath(collectionPath.toString());
				}

				Object dataObjectDownloadTaskId = jsonDownloadItem.get("dataObjectDownloadTaskId");
				if (dataObjectDownloadTaskId != null) {
					downloadItem.setDataObjectDownloadTaskId(dataObjectDownloadTaskId.toString());
				}

				Object message = jsonDownloadItem.get("message");
				if (message != null) {
					downloadItem.setMessage(message.toString());
				}

				// Result was captured as boolean before changed to HpcDownloadResult enum.
				// Need to account for both options.
				Object result = jsonDownloadItem.get("result");
				if (result != null) {
					if (result.toString().equals("true")) {
						downloadItem.setResult(HpcDownloadResult.COMPLETED);
					} else if (result.toString().equals("false")) {
						downloadItem.setResult(HpcDownloadResult.FAILED);
					} else {
						downloadItem.setResult(HpcDownloadResult.fromValue(result.toString()));
					}
				}

				HpcFileLocation destinationLocation = new HpcFileLocation();
				destinationLocation
						.setFileContainerId(jsonDownloadItem.get("destinationLocationFileContainerId").toString());
				destinationLocation.setFileId(jsonDownloadItem.get("destinationLocationFileId").toString());
				downloadItem.setDestinationLocation(destinationLocation);

				Object effectiveTransferSpeed = jsonDownloadItem.get("effectiveTransferSpeed");
				if (effectiveTransferSpeed != null) {
					downloadItem.setEffectiveTransferSpeed(Integer.valueOf(effectiveTransferSpeed.toString()));
				}

				Object size = jsonDownloadItem.get("size");
				if (size != null) {
					downloadItem.setSize(Long.valueOf(size.toString()));
				}

				Object percentComplete = jsonDownloadItem.get("percentComplete");
				if (percentComplete != null) {
					downloadItem.setPercentComplete(Integer.valueOf(percentComplete.toString()));
				}

				Object restoreInProgress = jsonDownloadItem.get("restoreInProgress");
				if (restoreInProgress != null) {
					downloadItem.setRestoreInProgress(restoreInProgress.toString().equals("true"));
				}

				Object stagingInProgress = jsonDownloadItem.get("stagingInProgress");
				if (stagingInProgress != null) {
					downloadItem.setStagingInProgress(stagingInProgress.toString().equals("true"));
				}

				Object stagingPercentComplete = jsonDownloadItem.get("stagingPercentComplete");
				if (stagingPercentComplete != null) {
					downloadItem.setStagingPercentComplete(Integer.valueOf(stagingPercentComplete.toString()));
				}

				downloadItems.add(downloadItem);
			}
		}

		return downloadItems;
	}

	/**
	 * Check if the given resultset has the column with the specified table
	 *
	 * @param rs         The resultset to check
	 * @param columnName The label of the specified column
	 * @return True if the column exists
	 * @throws SQLException
	 */
	public boolean hasColumnWithValue(ResultSet rs, String columnName) throws SQLException {
		ResultSetMetaData metaData = rs.getMetaData();
		int columns = metaData.getColumnCount();
		for (int i = 1; i <= columns; i++) {
			if (columnName.equalsIgnoreCase(metaData.getColumnLabel(i))) {
				if (rs.getObject(columnName) != null) {
					return true;
				}
			}
		}
		return false;
	}
}
