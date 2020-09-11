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
package gov.nih.nci.hpc.dao.postgresql.impl;

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.StringUtils;

import gov.nih.nci.hpc.dao.HpcDataDownloadDAO;
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
import gov.nih.nci.hpc.domain.datatransfer.HpcGoogleDriveDownloadDestination;
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
	private static final String UPSERT_DATA_OBJECT_DOWNLOAD_TASK_SQL = "insert into public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" ( "
			+ "\"ID\", \"USER_ID\", \"PATH\", \"CONFIGURATION_ID\", \"S3_ARCHIVE_CONFIGURATION_ID\", \"DATA_TRANSFER_REQUEST_ID\", "
			+ "\"DATA_TRANSFER_TYPE\", \"DATA_TRANSFER_STATUS\", \"DOWNLOAD_FILE_PATH\","
			+ "\"ARCHIVE_LOCATION_FILE_CONTAINER_ID\", \"ARCHIVE_LOCATION_FILE_ID\", "
			+ "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", \"DESTINATION_LOCATION_FILE_ID\", \"DESTINATION_TYPE\", "
			+ "\"S3_ACCOUNT_ACCESS_KEY\", \"S3_ACCOUNT_SECRET_KEY\", \"S3_ACCOUNT_REGION\", \"GOOGLE_DRIVE_ACCESS_TOKEN\", "
			+ "\"COMPLETION_EVENT\", \"PERCENT_COMPLETE\", \"SIZE\", \"CREATED\", \"PROCESSED\", \"IN_PROCESS\") "
			+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
			+ "on conflict(\"ID\") do update set \"USER_ID\"=excluded.\"USER_ID\", " + "\"PATH\"=excluded.\"PATH\", "
			+ "\"CONFIGURATION_ID\"=excluded.\"CONFIGURATION_ID\", "
			+ "\"S3_ARCHIVE_CONFIGURATION_ID\"=excluded.\"S3_ARCHIVE_CONFIGURATION_ID\", "
			+ "\"DATA_TRANSFER_REQUEST_ID\"=excluded.\"DATA_TRANSFER_REQUEST_ID\", "
			+ "\"DATA_TRANSFER_TYPE\"=excluded.\"DATA_TRANSFER_TYPE\", "
			+ "\"DATA_TRANSFER_STATUS\"=excluded.\"DATA_TRANSFER_STATUS\", "
			+ "\"DOWNLOAD_FILE_PATH\"=excluded.\"DOWNLOAD_FILE_PATH\", "
			+ "\"ARCHIVE_LOCATION_FILE_CONTAINER_ID\"=excluded.\"ARCHIVE_LOCATION_FILE_CONTAINER_ID\", "
			+ "\"ARCHIVE_LOCATION_FILE_ID\"=excluded.\"ARCHIVE_LOCATION_FILE_ID\", "
			+ "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\"=excluded.\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", "
			+ "\"DESTINATION_LOCATION_FILE_ID\"=excluded.\"DESTINATION_LOCATION_FILE_ID\", "
			+ "\"DESTINATION_TYPE\"=excluded.\"DESTINATION_TYPE\", "
			+ "\"S3_ACCOUNT_ACCESS_KEY\"=excluded.\"S3_ACCOUNT_ACCESS_KEY\", "
			+ "\"S3_ACCOUNT_SECRET_KEY\"=excluded.\"S3_ACCOUNT_SECRET_KEY\", "
			+ "\"S3_ACCOUNT_REGION\"=excluded.\"S3_ACCOUNT_REGION\", "
			+ "\"GOOGLE_DRIVE_ACCESS_TOKEN\"=excluded.\"GOOGLE_DRIVE_ACCESS_TOKEN\", "
			+ "\"COMPLETION_EVENT\"=excluded.\"COMPLETION_EVENT\", "
			+ "\"PERCENT_COMPLETE\"=excluded.\"PERCENT_COMPLETE\", " + "\"SIZE\"=excluded.\"SIZE\", "
			+ "\"CREATED\"=excluded.\"CREATED\", " + "\"PROCESSED\"=excluded.\"PROCESSED\", "
			+ "\"IN_PROCESS\"=excluded.\"IN_PROCESS\"";

	private static final String DELETE_DATA_OBJECT_DOWNLOAD_TASK_SQL = "delete from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" where \"ID\" = ?";

	private static final String UPDATE_DATA_OBJECT_DOWNLOAD_TASK_STATUS_SQL = "update public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" set \"DATA_TRANSFER_STATUS\" = ? where \"ID\" = ?";

	private static final String UPDATE_DATA_OBJECT_DOWNLOAD_TASK_STATUS_FILTER = " or (\"DATA_TRANSFER_STATUS\" = ? and \"DESTINATION_TYPE\" = ?)";

	private static final String GET_DATA_OBJECT_DOWNLOAD_TASK_SQL = "select * from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" where \"ID\" = ?";

	private static final String GET_DATA_OBJECT_DOWNLOAD_TASK_STATUS_SQL = "select \"DATA_TRANSFER_STATUS\" from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" where \"ID\" = ?";

	private static final String GET_DATA_OBJECT_DOWNLOAD_TASKS_SQL = "select * from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" order by \"PRIORITY\", \"CREATED\"";

	private static final String GET_DATA_OBJECT_DOWNLOAD_TASK_BY_STATUS_SQL = "select * from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" where \"DATA_TRANSFER_STATUS\" = ? "
			+ "and (\"PROCESSED\" < ? or \"PROCESSED\" is null) and \"CREATED\" < ? order by \"PRIORITY\", \"CREATED\" limit 1";

	private static final String GET_DATA_OBJECT_DOWNLOAD_TASK_BY_STATUS_AND_TYPE_SQL = "select * from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" where \"DATA_TRANSFER_STATUS\" = ? and \"DATA_TRANSFER_TYPE\" = ? "
			+ "and (\"PROCESSED\" < ? or \"PROCESSED\" is null) and \"CREATED\" < ? order by \"PRIORITY\", \"CREATED\" limit 1";

	private static final String UPSERT_DOWNLOAD_TASK_RESULT_SQL = "insert into public.\"HPC_DOWNLOAD_TASK_RESULT\" ( "
			+ "\"ID\", \"USER_ID\", \"PATH\", \"DATA_TRANSFER_REQUEST_ID\", \"DATA_TRANSFER_TYPE\", "
			+ "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", \"DESTINATION_LOCATION_FILE_CONTAINER_NAME\", \"DESTINATION_LOCATION_FILE_ID\", "
			+ "\"DESTINATION_TYPE\", \"RESULT\", \"TYPE\", \"MESSAGE\", \"ITEMS\", \"COMPLETION_EVENT\", \"EFFECTIVE_TRANSFER_SPEED\", "
			+ "\"SIZE\", \"CREATED\", \"COMPLETED\") "
			+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
			+ "on conflict on constraint \"HPC_DOWNLOAD_TASK_RESULT_pkey\" do update set \"USER_ID\"=excluded.\"USER_ID\", "
			+ "\"PATH\"=excluded.\"PATH\", " + "\"DATA_TRANSFER_REQUEST_ID\"=excluded.\"DATA_TRANSFER_REQUEST_ID\", "
			+ "\"DATA_TRANSFER_TYPE\"=excluded.\"DATA_TRANSFER_TYPE\", "
			+ "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\"=excluded.\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", "
			+ "\"DESTINATION_LOCATION_FILE_CONTAINER_NAME\"=excluded.\"DESTINATION_LOCATION_FILE_CONTAINER_NAME\", "
			+ "\"DESTINATION_LOCATION_FILE_ID\"=excluded.\"DESTINATION_LOCATION_FILE_ID\", "
			+ "\"DESTINATION_TYPE\"=excluded.\"DESTINATION_TYPE\", " + "\"RESULT\"=excluded.\"RESULT\", "
			+ "\"TYPE\"=excluded.\"TYPE\", " + "\"MESSAGE\"=excluded.\"MESSAGE\", " + "\"ITEMS\"=excluded.\"ITEMS\", "
			+ "\"COMPLETION_EVENT\"=excluded.\"COMPLETION_EVENT\", "
			+ "\"EFFECTIVE_TRANSFER_SPEED\"=excluded.\"EFFECTIVE_TRANSFER_SPEED\", " + "\"SIZE\"=excluded.\"SIZE\", "
			+ "\"CREATED\"=excluded.\"CREATED\", " + "\"COMPLETED\"=excluded.\"COMPLETED\"";

	private static final String GET_DOWNLOAD_TASK_RESULT_SQL = "select * from public.\"HPC_DOWNLOAD_TASK_RESULT\" where \"ID\" = ? and \"TYPE\" = ?";

	private static final String UPSERT_COLLECTION_DOWNLOAD_TASK_SQL = "insert into public.\"HPC_COLLECTION_DOWNLOAD_TASK\" ( "
			+ "\"ID\", \"USER_ID\", \"PATH\", \"CONFIGURATION_ID\", \"DESTINATION_LOCATION_FILE_CONTAINER_ID\", "
			+ "\"DESTINATION_LOCATION_FILE_ID\", \"DESTINATION_OVERWRITE\", \"S3_ACCOUNT_ACCESS_KEY\", "
			+ "\"S3_ACCOUNT_SECRET_KEY\", \"S3_ACCOUNT_REGION\", \"GOOGLE_DRIVE_ACCESS_TOKEN\", \"APPEND_PATH_TO_DOWNLOAD_DESTINATION\", \"ITEMS\", "
			+ "\"STATUS\", \"TYPE\", \"DATA_OBJECT_PATHS\", \"COLLECTION_PATHS\", \"CREATED\") "
			+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
			+ "on conflict(\"ID\") do update set \"USER_ID\"=excluded.\"USER_ID\", " + "\"PATH\"=excluded.\"PATH\", "
			+ "\"CONFIGURATION_ID\"=excluded.\"CONFIGURATION_ID\", "
			+ "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\"=excluded.\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", "
			+ "\"DESTINATION_LOCATION_FILE_ID\"=excluded.\"DESTINATION_LOCATION_FILE_ID\", "
			+ "\"DESTINATION_OVERWRITE\"=excluded.\"DESTINATION_OVERWRITE\", "
			+ "\"S3_ACCOUNT_ACCESS_KEY\"=excluded.\"S3_ACCOUNT_ACCESS_KEY\", "
			+ "\"S3_ACCOUNT_SECRET_KEY\"=excluded.\"S3_ACCOUNT_SECRET_KEY\", "
			+ "\"S3_ACCOUNT_REGION\"=excluded.\"S3_ACCOUNT_REGION\", "
			+ "\"GOOGLE_DRIVE_ACCESS_TOKEN\"=excluded.\"GOOGLE_DRIVE_ACCESS_TOKEN\", "
			+ "\"APPEND_PATH_TO_DOWNLOAD_DESTINATION\"=excluded.\"APPEND_PATH_TO_DOWNLOAD_DESTINATION\", "
			+ "\"ITEMS\"=excluded.\"ITEMS\", " + "\"STATUS\"=excluded.\"STATUS\", " + "\"TYPE\"=excluded.\"TYPE\", "
			+ "\"DATA_OBJECT_PATHS\"=excluded.\"DATA_OBJECT_PATHS\", "
			+ "\"COLLECTION_PATHS\"=excluded.\"COLLECTION_PATHS\", " + "\"CREATED\"=excluded.\"CREATED\"";

	private static final String GET_COLLECTION_DOWNLOAD_TASK_SQL = "select * from public.\"HPC_COLLECTION_DOWNLOAD_TASK\" where \"ID\" = ?";

	private static final String DELETE_COLLECTION_DOWNLOAD_TASK_SQL = "delete from public.\"HPC_COLLECTION_DOWNLOAD_TASK\" where \"ID\" = ?";

	private static final String GET_COLLECTION_DOWNLOAD_TASKS_SQL = "select * from public.\"HPC_COLLECTION_DOWNLOAD_TASK\" where \"STATUS\" = ? "
			+ "order by \"PRIORITY\", \"CREATED\"";

	private static final String GET_COLLECTION_DOWNLOAD_TASKS_IN_PROCESS_SQL = "select * from public.\"HPC_COLLECTION_DOWNLOAD_TASK\" where \"STATUS\" = ? and \"IN_PROCESS\" = ? "
			+ "order by \"PRIORITY\", \"CREATED\"";

	private static final String GET_DATA_OBJECT_DOWNLOAD_REQUESTS_SQL = "select null as \"USER_ID\", \"ID\", \"PATH\", \"CREATED\", 'DATA_OBJECT' as \"TYPE\", null as \"COMPLETED\", "
			+ "null as \"RESULT\", null as \"ITEMS\" from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" where "
			+ "\"USER_ID\" = ? and \"COMPLETION_EVENT\" = true order by \"CREATED\"";

	private static final String GET_COLLECTION_DOWNLOAD_REQUESTS_SQL = "select null as \"USER_ID\", \"ID\", \"PATH\", \"CREATED\", \"TYPE\", null as \"COMPLETED\", "
			+ "null as \"RESULT\", \"ITEMS\" from public.\"HPC_COLLECTION_DOWNLOAD_TASK\" where \"USER_ID\" = ? order by \"CREATED\"";

	private static final String GET_DOWNLOAD_RESULTS_SQL = "select null as \"USER_ID\", \"ID\", \"PATH\", \"CREATED\", \"TYPE\", \"COMPLETED\", \"RESULT\", \"ITEMS\" "
			+ "from public.\"HPC_DOWNLOAD_TASK_RESULT\" where \"USER_ID\" = ? and "
			+ "\"COMPLETION_EVENT\" = true order by \"CREATED\" desc limit ? offset ?";

	private static final String GET_DOWNLOAD_RESULTS_COUNT_SQL = "select count(*) from public.\"HPC_DOWNLOAD_TASK_RESULT\" where \"USER_ID\" = ? and "
			+ "\"COMPLETION_EVENT\" = true";

    private static final String GET_DATA_OBJECT_DOWNLOAD_REQUESTS_FOR_DOC_SQL = "select TASK.\"USER_ID\",\"ID\", \"PATH\", TASK.\"CREATED\", 'DATA_OBJECT' as \"TYPE\", null as \"COMPLETED\", "
        + "null as \"RESULT\", null as \"ITEMS\" from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" TASK, public.\"HPC_USER\" USER1 where USER1.\"USER_ID\"=TASK.\"USER_ID\" and "
        + "USER1.\"DOC\"= ? and \"COMPLETION_EVENT\" = true order by \"CREATED\"";

    private static final String GET_ALL_DATA_OBJECT_DOWNLOAD_REQUESTS_SQL = "select \"ID\", \"PATH\", \"CREATED\", 'DATA_OBJECT' as \"TYPE\", null as \"COMPLETED\", "
        + "null as \"RESULT\", null as \"ITEMS\" from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" where "
        + "\"COMPLETION_EVENT\" = true order by \"CREATED\"";

    private static final String GET_COLLECTION_DOWNLOAD_REQUESTS_FOR_DOC_SQL = "select TASK.\"USER_ID\", \"ID\", \"PATH\", TASK.\"CREATED\", \"TYPE\", null as \"COMPLETED\", "
        + "null as \"RESULT\", \"ITEMS\" from public.\"HPC_COLLECTION_DOWNLOAD_TASK\" TASK, public.\"HPC_USER\" USER1 where USER1.\"USER_ID\"=TASK.\"USER_ID\" and USER1.\"DOC\"= ? order by \"CREATED\"";

    private static final String GET_ALL_COLLECTION_DOWNLOAD_REQUESTS_SQL = "select \"USER_ID\", \"ID\", \"PATH\", \"CREATED\", \"TYPE\", null as \"COMPLETED\", "
        + "null as \"RESULT\", \"ITEMS\" from public.\"HPC_COLLECTION_DOWNLOAD_TASK\" order by \"CREATED\"";

    private static final String GET_DOWNLOAD_RESULTS_FOR_DOC_SQL = "select TASK.\"USER_ID\", \"ID\", \"PATH\", TASK.\"CREATED\", \"TYPE\", \"COMPLETED\", \"RESULT\", \"ITEMS\" "
            + "from public.\"HPC_DOWNLOAD_TASK_RESULT\" TASK, public.\"HPC_USER\" USER1  where USER1.\"USER_ID\" = TASK.\"USER_ID\" and USER1.\"DOC\"=? and "
            + "\"COMPLETION_EVENT\" = true order by \"CREATED\" desc limit ? offset ?";
        
    private static final String GET_ALL_DOWNLOAD_RESULTS_SQL = "select \"USER_ID\", \"ID\", \"PATH\", \"CREATED\", \"TYPE\", \"COMPLETED\", \"RESULT\", \"ITEMS\" "
            + "from public.\"HPC_DOWNLOAD_TASK_RESULT\" where  "
            + "\"COMPLETION_EVENT\" = true order by \"CREATED\" desc limit ? offset ?";

	private static final String GET_DOWNLOAD_RESULTS_COUNT_FOR_DOC_SQL = "select count(*) from public.\"HPC_DOWNLOAD_TASK_RESULT\" TASK, public.\"HPC_USER\" USER1  where USER1.\"USER_ID\" = TASK.\"USER_ID\" and USER1.\"DOC\"=? and "
            + "\"COMPLETION_EVENT\" = true";

	private static final String GET_ALL_DOWNLOAD_RESULTS_COUNT_SQL = "select count(*) from public.\"HPC_DOWNLOAD_TASK_RESULT\" where "
        + "\"COMPLETION_EVENT\" = true";

	private static final String GET_COLLECTION_DOWNLOAD_REQUESTS_COUNT_SQL = "select count(*) from public.\"HPC_COLLECTION_DOWNLOAD_TASK\" where \"USER_ID\" = ? and "
			+ "\"STATUS\" = ? and \"IN_PROCESS\" = ?";

	private static final String SET_COLLECTION_DOWNLOAD_TASK_IN_PROCESS_SQL = "update public.\"HPC_COLLECTION_DOWNLOAD_TASK\" set \"IN_PROCESS\" = ? where \"ID\" = ?";

	private static final String SET_COLLECTION_DOWNLOAD_TASK_CANCELLATION_REQUEST_SQL = "update public.\"HPC_COLLECTION_DOWNLOAD_TASK\" set \"CANCELLATION_REQUESTED\" = ? where \"ID\" = ?";

	private static final String GET_COLLECTION_DOWNLOAD_TASK_CANCELLATION_REQUEST_SQL = "select \"CANCELLATION_REQUESTED\" from public.\"HPC_COLLECTION_DOWNLOAD_TASK\" where \"ID\" = ?";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;

	// Encryptor.
	@Autowired
	HpcEncryptor encryptor = null;

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
		dataObjectDownloadTask.setPercentComplete(rs.getInt("PERCENT_COMPLETE"));
		dataObjectDownloadTask.setSize(rs.getLong("SIZE"));
		dataObjectDownloadTask.setInProcess(rs.getBoolean("IN_PROCESS"));

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
		}

		String googleDriveAccessToken = null;
		byte[] token = rs.getBytes("GOOGLE_DRIVE_ACCESS_TOKEN");
		if (token != null) {
			googleDriveAccessToken = encryptor.decrypt(token);
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
			HpcGoogleDriveDownloadDestination googleDriveDownloadDestination = new HpcGoogleDriveDownloadDestination();
			googleDriveDownloadDestination.setDestinationLocation(destinationLocation);
			googleDriveDownloadDestination.setAccessToken(googleDriveAccessToken);
			dataObjectDownloadTask.setGoogleDriveDownloadDestination(googleDriveDownloadDestination);
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
		downloadTaskResult.setDataTransferRequestId(rs.getString("DATA_TRANSFER_REQUEST_ID"));
		String dataTransferType = rs.getString("DATA_TRANSFER_TYPE");
		downloadTaskResult
				.setDataTransferType(dataTransferType != null ? HpcDataTransferType.fromValue(dataTransferType) : null);
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
		downloadTaskResult.setResult(HpcDownloadResult.fromValue(rs.getString("RESULT")));
		downloadTaskResult.setMessage(rs.getString("MESSAGE"));
		downloadTaskResult.getItems().addAll(fromJSON(rs.getString("ITEMS")));
		downloadTaskResult.setCompletionEvent(rs.getBoolean("COMPLETION_EVENT"));
		downloadTaskResult.setEffectiveTransferSpeed(rs.getInt("EFFECTIVE_TRANSFER_SPEED"));
		long size = rs.getLong("SIZE");
		downloadTaskResult.setSize(size > 0 ? size : null);

		Calendar created = Calendar.getInstance();
		created.setTime(rs.getTimestamp("CREATED"));
		downloadTaskResult.setCreated(created);

		Calendar completed = Calendar.getInstance();
		completed.setTime(rs.getTimestamp("COMPLETED"));
		downloadTaskResult.setCompleted(completed);

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
		}

		String googleDriveAccessToken = null;
		byte[] token = rs.getBytes("GOOGLE_DRIVE_ACCESS_TOKEN");
		if (token != null) {
			googleDriveAccessToken = encryptor.decrypt(token);
		}

		if (s3Account != null) {
			HpcS3DownloadDestination s3DownloadDestination = new HpcS3DownloadDestination();
			s3DownloadDestination.setDestinationLocation(destinationLocation);
			s3DownloadDestination.setAccount(s3Account);
			collectionDownloadTask.setS3DownloadDestination(s3DownloadDestination);
		} else if (googleDriveAccessToken != null) {
			HpcGoogleDriveDownloadDestination googleDriveDownloadDestination = new HpcGoogleDriveDownloadDestination();
			googleDriveDownloadDestination.setDestinationLocation(destinationLocation);
			googleDriveDownloadDestination.setAccessToken(googleDriveAccessToken);
			collectionDownloadTask.setGoogleDriveDownloadDestination(googleDriveDownloadDestination);
		} else {
			HpcGlobusDownloadDestination globusDownloadDestination = new HpcGlobusDownloadDestination();
			globusDownloadDestination.setDestinationLocation(destinationLocation);
			globusDownloadDestination.setDestinationOverwrite(rs.getBoolean("DESTINATION_OVERWRITE"));
			collectionDownloadTask.setGlobusDownloadDestination(globusDownloadDestination);
		}

		collectionDownloadTask.setAppendPathToDownloadDestination(rs.getBoolean("APPEND_PATH_TO_DOWNLOAD_DESTINATION"));
		collectionDownloadTask.getItems().addAll(fromJSON(rs.getString("ITEMS")));

		Calendar created = Calendar.getInstance();
		created.setTime(rs.getTimestamp("CREATED"));
		collectionDownloadTask.setCreated(created);

		// Extract the data objects paths.
		Array dataObjectsSQLArray = rs.getArray("DATA_OBJECT_PATHS");
		if (dataObjectsSQLArray != null) {
			String[] dataObjectPaths = (String[]) dataObjectsSQLArray.getArray();
			int dataObjectPathsSize = dataObjectPaths.length;
			for (int i = 0; i < dataObjectPathsSize; i++) {
				collectionDownloadTask.getDataObjectPaths().add(dataObjectPaths[i]);
			}
		}

		// Extract the collection paths.
		Array collectionSQLArray = rs.getArray("COLLECTION_PATHS");
		if (collectionSQLArray != null) {
			String[] collectionPaths = (String[]) collectionSQLArray.getArray();
			int collectionPathsSize = collectionPaths.length;
			for (int i = 0; i < collectionPathsSize; i++) {
				collectionDownloadTask.getCollectionPaths().add(collectionPaths[i]);
			}
		}

		return collectionDownloadTask;
	};

	// HpcUserDownloadRequest table to object mapper.
	private RowMapper<HpcUserDownloadRequest> userDownloadRequestRowMapper = (rs, rowNum) -> {
		HpcUserDownloadRequest userDownloadRequest = new HpcUserDownloadRequest();
		userDownloadRequest.setTaskId(rs.getString("ID"));
		userDownloadRequest.setPath(rs.getString("PATH"));
		userDownloadRequest.setType(HpcDownloadTaskType.fromValue(rs.getString(("TYPE"))));

		if (rs.getObject("RESULT") != null) {
			userDownloadRequest.setResult(HpcDownloadResult.fromValue(rs.getString("RESULT")));
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
	public void upsertDataObjectDownloadTask(HpcDataObjectDownloadTask dataObjectDownloadTask) throws HpcException {
		try {
			if (dataObjectDownloadTask.getId() == null) {
				dataObjectDownloadTask.setId(UUID.randomUUID().toString());
			}

			HpcFileLocation destinationLocation = null;
			byte[] s3AccountAccessKey = null;
			byte[] s3AccountSecretKey = null;
			String s3AccountRegion = null;
			byte[] googleDriveAccessToken = null;
			if (dataObjectDownloadTask.getGlobusDownloadDestination() != null) {
				destinationLocation = dataObjectDownloadTask.getGlobusDownloadDestination().getDestinationLocation();
			} else if (dataObjectDownloadTask.getS3DownloadDestination() != null) {
				destinationLocation = dataObjectDownloadTask.getS3DownloadDestination().getDestinationLocation();
				HpcS3Account s3Account = dataObjectDownloadTask.getS3DownloadDestination().getAccount();
				s3AccountAccessKey = encryptor.encrypt(s3Account.getAccessKey());
				s3AccountSecretKey = encryptor.encrypt(s3Account.getSecretKey());
				s3AccountRegion = s3Account.getRegion();
			} else if (dataObjectDownloadTask.getGoogleDriveDownloadDestination() != null) {
				destinationLocation = dataObjectDownloadTask.getGoogleDriveDownloadDestination()
						.getDestinationLocation();
				googleDriveAccessToken = encryptor
						.encrypt(dataObjectDownloadTask.getGoogleDriveDownloadDestination().getAccessToken());
			}

			jdbcTemplate.update(UPSERT_DATA_OBJECT_DOWNLOAD_TASK_SQL, dataObjectDownloadTask.getId(),
					dataObjectDownloadTask.getUserId(), dataObjectDownloadTask.getPath(),
					dataObjectDownloadTask.getConfigurationId(), dataObjectDownloadTask.getS3ArchiveConfigurationId(),
					dataObjectDownloadTask.getDataTransferRequestId(),
					dataObjectDownloadTask.getDataTransferType().value(),
					dataObjectDownloadTask.getDataTransferStatus().value(),
					dataObjectDownloadTask.getDownloadFilePath(),
					dataObjectDownloadTask.getArchiveLocation().getFileContainerId(),
					dataObjectDownloadTask.getArchiveLocation().getFileId(), destinationLocation.getFileContainerId(),
					destinationLocation.getFileId(), dataObjectDownloadTask.getDestinationType().value(),
					s3AccountAccessKey, s3AccountSecretKey, s3AccountRegion, googleDriveAccessToken,
					dataObjectDownloadTask.getCompletionEvent(), dataObjectDownloadTask.getPercentComplete(),
					dataObjectDownloadTask.getSize(), dataObjectDownloadTask.getCreated(),
					dataObjectDownloadTask.getProcessed(),
					Optional.ofNullable(dataObjectDownloadTask.getInProcess()).orElse(false));

		} catch (DataAccessException e) {
			throw new HpcException("Failed to upsert a data object download task: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
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
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public void deleteDataObjectDownloadTask(String id) throws HpcException {
		try {
			jdbcTemplate.update(DELETE_DATA_OBJECT_DOWNLOAD_TASK_SQL, id);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to delete a data object download task: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public boolean updateDataObjectDownloadTaskStatus(String id, List<HpcDataObjectDownloadTaskStatusFilter> filters,
			HpcDataTransferDownloadStatus toStatus) throws HpcException {
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		// Add the task-id and to-status values to the query.
		sqlQueryBuilder.append(UPDATE_DATA_OBJECT_DOWNLOAD_TASK_STATUS_SQL);
		args.add(toStatus.value());
		args.add(id);

		// Add each pair of the from-status and destination-type as a filter to the
		// query.
		if (filters != null && !filters.isEmpty()) {
			sqlQueryBuilder.append(" and (false");
			filters.forEach(filter -> {
				sqlQueryBuilder.append(UPDATE_DATA_OBJECT_DOWNLOAD_TASK_STATUS_FILTER);
				args.add(filter.getStatus().value());
				args.add(filter.getDestination().value());
			});
			sqlQueryBuilder.append(")");
		}

		// Execute the query.
		try {
			return (jdbcTemplate.update(sqlQueryBuilder.toString(), args.toArray()) > 0);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to update a data object download task status: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
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
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public List<HpcDataObjectDownloadTask> getDataObjectDownloadTasks() throws HpcException {
		try {
			return jdbcTemplate.query(GET_DATA_OBJECT_DOWNLOAD_TASKS_SQL, dataObjectDownloadTaskRowMapper);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data object download tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
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
						timestamp, timestamp);
			}
			return jdbcTemplate.query(GET_DATA_OBJECT_DOWNLOAD_TASK_BY_STATUS_SQL, dataObjectDownloadTaskRowMapper,
					dataTransferStatus.value(), timestamp, timestamp);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data object download tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public void upsertDownloadTaskResult(HpcDownloadTaskResult taskResult) throws HpcException {
		try {
			jdbcTemplate.update(UPSERT_DOWNLOAD_TASK_RESULT_SQL, taskResult.getId(), taskResult.getUserId(),
					taskResult.getPath(), taskResult.getDataTransferRequestId(),
					taskResult.getDataTransferType() != null ? taskResult.getDataTransferType().value() : null,
					taskResult.getDestinationLocation().getFileContainerId(),
					taskResult.getDestinationLocation().getFileContainerName(),
					taskResult.getDestinationLocation().getFileId(),
					taskResult.getDestinationType() != null ? taskResult.getDestinationType().value() : null,
					taskResult.getResult().value(), taskResult.getType().value(), taskResult.getMessage(),
					toJSON(taskResult.getItems()), taskResult.getCompletionEvent(),
					taskResult.getEffectiveTransferSpeed(), taskResult.getSize(), taskResult.getCreated(),
					taskResult.getCompleted());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to upsert a download task result: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
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
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public void upsertCollectionDownloadTask(HpcCollectionDownloadTask collectionDownloadTask) throws HpcException {
		try {
			if (collectionDownloadTask.getId() == null) {
				collectionDownloadTask.setId(UUID.randomUUID().toString());
			}

			Array dataObjectPathsSQLArray = null;
			if (!collectionDownloadTask.getDataObjectPaths().isEmpty()) {
				Connection conn = jdbcTemplate.getDataSource().getConnection();
				try {
					dataObjectPathsSQLArray = conn.createArrayOf("text",
							collectionDownloadTask.getDataObjectPaths().toArray());

				} finally {
					conn.close();
				}
			}

			Array collectionPathsSQLArray = null;
			if (!collectionDownloadTask.getCollectionPaths().isEmpty()) {
				Connection conn = jdbcTemplate.getDataSource().getConnection();
				try {
					collectionPathsSQLArray = conn.createArrayOf("text",
							collectionDownloadTask.getCollectionPaths().toArray());

				} finally {
					conn.close();
				}
			}

			HpcFileLocation destinationLocation = null;
			Boolean destinationOverwrite = null;
			byte[] s3AccountAccessKey = null;
			byte[] s3AccountSecretKey = null;
			String s3AccountRegion = null;
			byte[] googleDriveAccessToken = null;
			if (collectionDownloadTask.getGlobusDownloadDestination() != null) {
				destinationLocation = collectionDownloadTask.getGlobusDownloadDestination().getDestinationLocation();
				destinationOverwrite = collectionDownloadTask.getGlobusDownloadDestination().getDestinationOverwrite();
			} else if (collectionDownloadTask.getS3DownloadDestination() != null) {
				destinationLocation = collectionDownloadTask.getS3DownloadDestination().getDestinationLocation();
				HpcS3Account s3Account = collectionDownloadTask.getS3DownloadDestination().getAccount();
				s3AccountAccessKey = encryptor.encrypt(s3Account.getAccessKey());
				s3AccountSecretKey = encryptor.encrypt(s3Account.getSecretKey());
				s3AccountRegion = s3Account.getRegion();
			} else if (collectionDownloadTask.getGoogleDriveDownloadDestination() != null) {
				destinationLocation = collectionDownloadTask.getGoogleDriveDownloadDestination()
						.getDestinationLocation();
				googleDriveAccessToken = encryptor
						.encrypt(collectionDownloadTask.getGoogleDriveDownloadDestination().getAccessToken());
			}

			jdbcTemplate.update(UPSERT_COLLECTION_DOWNLOAD_TASK_SQL, collectionDownloadTask.getId(),
					collectionDownloadTask.getUserId(), collectionDownloadTask.getPath(),
					collectionDownloadTask.getConfigurationId(), destinationLocation.getFileContainerId(),
					destinationLocation.getFileId(), destinationOverwrite, s3AccountAccessKey, s3AccountSecretKey,
					s3AccountRegion, googleDriveAccessToken,
					collectionDownloadTask.getAppendPathToDownloadDestination(),
					toJSON(collectionDownloadTask.getItems()), collectionDownloadTask.getStatus().value(),
					collectionDownloadTask.getType().value(), dataObjectPathsSQLArray, collectionPathsSQLArray,
					collectionDownloadTask.getCreated());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to upsert a collection download request: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		} catch (SQLException se) {
			throw new HpcException("Failed to upsert a collection download request: " + se.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, se);
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
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public void deleteCollectionDownloadTask(String id) throws HpcException {
		try {
			jdbcTemplate.update(DELETE_COLLECTION_DOWNLOAD_TASK_SQL, id);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to delete a collection download task: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus status)
			throws HpcException {
		try {
			return jdbcTemplate.query(GET_COLLECTION_DOWNLOAD_TASKS_SQL, collectionDownloadTaskRowMapper,
					status.value());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get collection download tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus status,
			boolean inProcess) throws HpcException {
		try {
			return jdbcTemplate.query(GET_COLLECTION_DOWNLOAD_TASKS_IN_PROCESS_SQL, collectionDownloadTaskRowMapper,
					status.value(), inProcess);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get collection download tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
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
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public void setCollectionDownloadTaskInProcess(String id, boolean inProcess) throws HpcException {
		try {
			jdbcTemplate.update(SET_COLLECTION_DOWNLOAD_TASK_IN_PROCESS_SQL, inProcess, id);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to set a collection download task w/ in-process value: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
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
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public boolean getCollectionDownloadTaskCancellationRequested(String id) throws HpcException {
		try {
			Boolean cancellationRequested = jdbcTemplate
					.queryForObject(GET_COLLECTION_DOWNLOAD_TASK_CANCELLATION_REQUEST_SQL, Boolean.class, id);
			return cancellationRequested != null ? cancellationRequested : false;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get cancellation request of: " + id + " " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public List<HpcUserDownloadRequest> getDataObjectDownloadRequests(String userId) throws HpcException {
		try {
			return jdbcTemplate.query(GET_DATA_OBJECT_DOWNLOAD_REQUESTS_SQL, userDownloadRequestRowMapper, userId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get data object download requests: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public List<HpcUserDownloadRequest> getCollectionDownloadRequests(String userId) throws HpcException {
		try {
			return jdbcTemplate.query(GET_COLLECTION_DOWNLOAD_REQUESTS_SQL, userDownloadRequestRowMapper, userId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get collection download requests: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public List<HpcUserDownloadRequest> getDownloadResults(String userId, int offset, int limit) throws HpcException {
		try {
			return jdbcTemplate.query(GET_DOWNLOAD_RESULTS_SQL, userDownloadRequestRowMapper, userId, limit, offset);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get download results: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

	@Override
	public int getDownloadResultsCount(String userId) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_DOWNLOAD_RESULTS_COUNT_SQL, Integer.class, userId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to count download results: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.POSTGRESQL, e);
		}
	}
	
    @Override
    public List<HpcUserDownloadRequest> getDataObjectDownloadRequestsForDoc(String doc) throws HpcException {
        try {
            return jdbcTemplate.query(GET_DATA_OBJECT_DOWNLOAD_REQUESTS_FOR_DOC_SQL, userDownloadRequestRowMapper, doc);

        } catch (DataAccessException e) {
            throw new HpcException("Failed to get data object download requests: " + e.getMessage(),
                    HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
        }
    }
    
    @Override
    public List<HpcUserDownloadRequest> getAllDataObjectDownloadRequests() throws HpcException {
        try {
            return jdbcTemplate.query(GET_ALL_DATA_OBJECT_DOWNLOAD_REQUESTS_SQL, userDownloadRequestRowMapper);

        } catch (DataAccessException e) {
            throw new HpcException("Failed to get data object download requests: " + e.getMessage(),
                    HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
        }
    }
    
    @Override
    public List<HpcUserDownloadRequest> getCollectionDownloadRequestsForDoc(String doc) throws HpcException {
        try {
            return jdbcTemplate.query(GET_COLLECTION_DOWNLOAD_REQUESTS_FOR_DOC_SQL, userDownloadRequestRowMapper, doc);

        } catch (DataAccessException e) {
            throw new HpcException("Failed to get collection download requests: " + e.getMessage(),
                    HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
        }
    }
    
    @Override
    public List<HpcUserDownloadRequest> getAllCollectionDownloadRequests() throws HpcException {
        try {
            return jdbcTemplate.query(GET_ALL_COLLECTION_DOWNLOAD_REQUESTS_SQL, userDownloadRequestRowMapper);

        } catch (DataAccessException e) {
            throw new HpcException("Failed to get collection download requests: " + e.getMessage(),
                    HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
        }
    }

    
    @Override
    public List<HpcUserDownloadRequest> getDownloadResultsForDoc(String doc, int offset, int limit) throws HpcException {
        try {
            return jdbcTemplate.query(GET_DOWNLOAD_RESULTS_FOR_DOC_SQL, userDownloadRequestRowMapper, doc, limit, offset);

        } catch (DataAccessException e) {
            throw new HpcException("Failed to get download results: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
                    HpcIntegratedSystem.POSTGRESQL, e);
        }
    }
    
    @Override
    public List<HpcUserDownloadRequest> getAllDownloadResults(int offset, int limit) throws HpcException {
        try {
            return jdbcTemplate.query(GET_ALL_DOWNLOAD_RESULTS_SQL, userDownloadRequestRowMapper, limit, offset);

        } catch (DataAccessException e) {
            throw new HpcException("Failed to get download results: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
                    HpcIntegratedSystem.POSTGRESQL, e);
        }
    }

	@Override
    public int getDownloadResultsCountForDoc(String doc) throws HpcException {
        try {
            return jdbcTemplate.queryForObject(GET_DOWNLOAD_RESULTS_COUNT_FOR_DOC_SQL, Integer.class, doc);

        } catch (DataAccessException e) {
            throw new HpcException("Failed to count download results: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
                    HpcIntegratedSystem.POSTGRESQL, e);
        }
    }
	
	@Override
    public int getAllDownloadResultsCount() throws HpcException {
        try {
            return jdbcTemplate.queryForObject(GET_ALL_DOWNLOAD_RESULTS_COUNT_SQL, Integer.class);

        } catch (DataAccessException e) {
            throw new HpcException("Failed to count download results: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
                    HpcIntegratedSystem.POSTGRESQL, e);
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
			if (downloadItem.getDataObjectDownloadTaskId() != null) {
				jsonDownloadItem.put("dataObjectDownloadTaskId", downloadItem.getDataObjectDownloadTaskId());
			}
			if (downloadItem.getMessage() != null) {
				jsonDownloadItem.put("message", downloadItem.getMessage());
			}
			if (downloadItem.getResult() != null) {
				jsonDownloadItem.put("result", downloadItem.getResult().value());
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

				downloadItems.add(downloadItem);
			}
		}

		return downloadItems;
	}
}
