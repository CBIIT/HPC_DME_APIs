/**
 * HpcDataRegistrationDAOImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao.oracle.impl;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.StringUtils;

import gov.nih.nci.hpc.dao.HpcDataRegistrationDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcBulkDataObjectRegistrationTaskStatus;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObjectRegistrationTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcStreamingUploadSource;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcBulkMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcBulkMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationItem;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationResult;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationRequest;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationResult;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Registration DAO Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataRegistrationDAOImpl implements HpcDataRegistrationDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String UPSERT_BULK_DATA_OBJECT_REGISTRATION_TASK_SQL = "merge into HPC_BULK_DATA_OBJECT_REGISTRATION_TASK using dual on (ID = ?) "
			+ "when matched then update set USER_ID = ?, UI_URL = ?, STATUS = ?, ITEMS = ?, CREATED = ? "
			+ "when not matched then insert (ID, USER_ID, UI_URL, STATUS, ITEMS, CREATED) "
			+ "values (?, ?, ?, ?, ?, ?)";

	private static final String GET_BULK_DATA_OBJECT_REGISTRATION_TASK_SQL = "select * from HPC_BULK_DATA_OBJECT_REGISTRATION_TASK where ID = ?";

	private static final String DELETE_BULK_DATA_OBJECT_REGISTRATION_TASK_SQL = "delete from HPC_BULK_DATA_OBJECT_REGISTRATION_TASK where ID = ?";

	private static final String GET_BULK_DATA_OBJECT_REGISTRATION_TASKS_SQL = "select * from HPC_BULK_DATA_OBJECT_REGISTRATION_TASK where STATUS = ? "
			+ "order by CREATED";

	private static final String GET_BULK_DATA_OBJECT_REGISTRATION_TASKS_FOR_USER_SQL = "select * from HPC_BULK_DATA_OBJECT_REGISTRATION_TASK where USER_ID = ? "
			+ "order by CREATED";

	private static final String UPSERT_BULK_DATA_OBJECT_REGISTRATION_RESULT_SQL = "merge into HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT using dual on (ID = ?) "
			+ "when matched then update set USER_ID = ?, RESULT = ?, MESSAGE = ?, EFFECTIVE_TRANSFER_SPEED = ?, "
			+ "ITEMS = ?, CREATED = ?, COMPLETED = ? "
			+ "when not matched then insert (ID, USER_ID, RESULT, MESSAGE, EFFECTIVE_TRANSFER_SPEED, ITEMS, "
			+ "CREATED, COMPLETED) values (?, ?, ?, ?, ?, ?, ?, ?) ";

	private static final String GET_BULK_DATA_OBJECT_REGISTRATION_RESULT_SQL = "select * from HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT where ID = ?";

	private static final String GET_BULK_DATA_OBJECT_REGISTRATION_RESULTS_SQL = "select * from HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT where USER_ID = ? "
			+ "order by CREATED desc offset ? rows fetch next ? rows only";

	private static final String GET_BULK_DATA_OBJECT_REGISTRATION_RESULTS_COUNT_SQL = "select count(*) from HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT where USER_ID = ?";

	private static final String INSERT_DATA_OBJECT_REGISTRATION_RESULT_SQL = "insert into HPC_DATA_OBJECT_REGISTRATION_RESULT ("
			+ "ID, PATH, USER_ID, UPLOAD_METHOD, RESULT, MESSAGE, EFFECTIVE_TRANSFER_SPEED, "
			+ "DATA_TRANSFER_REQUEST_ID, SOURCE_LOCATION_FILE_ID, SOURCE_LOCATION_FILE_CONTAINER_ID, "
			+ "SOURCE_LOCATION_FILE_CONTAINER_NAME, CREATED, COMPLETED) "
			+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String GET_BULK_DATA_OBJECT_REGISTRATION_TASKS_FOR_DOC_SQL = "select TASK.* from HPC_BULK_DATA_OBJECT_REGISTRATION_TASK TASK, public.HPC_USER USER1 where USER1.USER_ID=TASK.USER_ID and USER1.DOC= ?  "
			+ "order by CREATED";

	private static final String GET_ALL_BULK_DATA_OBJECT_REGISTRATION_TASKS_SQL = "select * from public.HPC_BULK_DATA_OBJECT_REGISTRATION_TASK order by CREATED";

	private static final String GET_BULK_DATA_OBJECT_REGISTRATION_RESULTS_FOR_DOC_SQL = "select TASK.* from HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT TASK, HPC_USER USER1 where USER1.USER_ID=TASK.USER_ID and USER1.DOC = ? "
			+ "order by CREATED desc offset ? rows fetch next ? rows only";

	private static final String GET_ALL_BULK_DATA_OBJECT_REGISTRATION_RESULTS_SQL = "select * from HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT order by CREATED desc offset ? rows fetch next ? rows only";

	private static final String GET_BULK_DATA_OBJECT_REGISTRATION_RESULTS_COUNT_FOR_DOC_SQL = "select count(*) from HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT TASK, HPC_USER USER1 where USER1.USER_ID=TASK.USER_ID and USER1.DOC = ?";

	private static final String GET_ALL_BULK_DATA_OBJECT_REGISTRATION_RESULTS_COUNT_SQL = "select count(*) from HPC_BULK_DATA_OBJECT_REGISTRATION_RESULT ";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// Encryptor.
	@Autowired
	HpcEncryptor encryptor = null;

	// The Spring JDBC Template instance.
	@Autowired
	// TODO: Remove after Oracle migration
	@Qualifier("hpcOracleJdbcTemplate")
	// TODO: END
	private JdbcTemplate jdbcTemplate = null;

	// HpcBulkDataObjectRegistrationTask table to object mapper.
	private RowMapper<HpcBulkDataObjectRegistrationTask> bulkDataObjectRegistrationTaskRowMapper = (rs, rowNum) -> {
		HpcBulkDataObjectRegistrationTask bulkDataObjectRegistrationTask = new HpcBulkDataObjectRegistrationTask();
		bulkDataObjectRegistrationTask.setId(rs.getString("ID"));
		bulkDataObjectRegistrationTask.setUserId(rs.getString("USER_ID"));
		bulkDataObjectRegistrationTask.setUiURL(rs.getString("UI_URL"));
		bulkDataObjectRegistrationTask
				.setStatus(HpcBulkDataObjectRegistrationTaskStatus.fromValue(rs.getString(("STATUS"))));
		bulkDataObjectRegistrationTask.getItems().addAll(fromJSON(rs.getString("ITEMS")));

		Calendar created = Calendar.getInstance();
		created.setTime(rs.getTimestamp("CREATED"));
		bulkDataObjectRegistrationTask.setCreated(created);

		return bulkDataObjectRegistrationTask;
	};

	// HpcBulkDataObjectRegistrationResulr table to object mapper.
	private RowMapper<HpcBulkDataObjectRegistrationResult> bulkDataObjectRegistrationResultRowMapper = (rs, rowNum) -> {
		HpcBulkDataObjectRegistrationResult bulkDdataObjectRegistrationResult = new HpcBulkDataObjectRegistrationResult();
		bulkDdataObjectRegistrationResult.setId(rs.getString("ID"));
		bulkDdataObjectRegistrationResult.setUserId(rs.getString("USER_ID"));
		bulkDdataObjectRegistrationResult.setResult(rs.getBoolean("RESULT"));
		bulkDdataObjectRegistrationResult.setMessage(rs.getString("MESSAGE"));
		bulkDdataObjectRegistrationResult.getItems().addAll(fromJSON(rs.getString("ITEMS")));
		bulkDdataObjectRegistrationResult.setEffectiveTransferSpeed(rs.getInt("EFFECTIVE_TRANSFER_SPEED"));

		Calendar created = Calendar.getInstance();
		created.setTime(rs.getTimestamp("CREATED"));
		bulkDdataObjectRegistrationResult.setCreated(created);

		Calendar completed = Calendar.getInstance();
		completed.setTime(rs.getTimestamp("COMPLETED"));
		bulkDdataObjectRegistrationResult.setCompleted(completed);

		return bulkDdataObjectRegistrationResult;
	};

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcDataRegistrationDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataRegistrationDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void upsertBulkDataObjectRegistrationTask(HpcBulkDataObjectRegistrationTask dataObjectListRegistrationTask)
			throws HpcException {
		try {
			if (dataObjectListRegistrationTask.getId() == null) {
				dataObjectListRegistrationTask.setId(UUID.randomUUID().toString());
			}

			String items = toJSON(dataObjectListRegistrationTask.getItems());
			jdbcTemplate.update(UPSERT_BULK_DATA_OBJECT_REGISTRATION_TASK_SQL, dataObjectListRegistrationTask.getId(),
					dataObjectListRegistrationTask.getUserId(), dataObjectListRegistrationTask.getUiURL(),
					dataObjectListRegistrationTask.getStatus().value(), items,
					dataObjectListRegistrationTask.getCreated(), dataObjectListRegistrationTask.getId(),
					dataObjectListRegistrationTask.getUserId(), dataObjectListRegistrationTask.getUiURL(),
					dataObjectListRegistrationTask.getStatus().value(), items,
					dataObjectListRegistrationTask.getCreated());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to upsert a bulk data object registration request: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public HpcBulkDataObjectRegistrationTask getBulkDataObjectRegistrationTask(String id) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_BULK_DATA_OBJECT_REGISTRATION_TASK_SQL,
					bulkDataObjectRegistrationTaskRowMapper, id);

		} catch (IncorrectResultSizeDataAccessException irse) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get a bulk data object registration task: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void deleteBulkDataObjectRegistrationTask(String id) throws HpcException {
		try {
			jdbcTemplate.update(DELETE_BULK_DATA_OBJECT_REGISTRATION_TASK_SQL, id);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to delete a bulk data object registration task: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcBulkDataObjectRegistrationTask> getBulkDataObjectRegistrationTasks(
			HpcBulkDataObjectRegistrationTaskStatus status) throws HpcException {
		try {
			return jdbcTemplate.query(GET_BULK_DATA_OBJECT_REGISTRATION_TASKS_SQL,
					bulkDataObjectRegistrationTaskRowMapper, status.value());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get bulk data object registration tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void upsertBulkDataObjectRegistrationResult(HpcBulkDataObjectRegistrationResult registrationResult)
			throws HpcException {
		try {
			String items = toJSON(registrationResult.getItems());
			jdbcTemplate.update(UPSERT_BULK_DATA_OBJECT_REGISTRATION_RESULT_SQL, registrationResult.getId(),
					registrationResult.getUserId(), registrationResult.getResult(), registrationResult.getMessage(),
					registrationResult.getEffectiveTransferSpeed(), items, registrationResult.getCreated(),
					registrationResult.getCompleted(), registrationResult.getId(), registrationResult.getUserId(),
					registrationResult.getResult(), registrationResult.getMessage(),
					registrationResult.getEffectiveTransferSpeed(), items, registrationResult.getCreated(),
					registrationResult.getCompleted());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to upsert a bulk data object registration result: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public HpcBulkDataObjectRegistrationResult getBulkDataObjectRegistrationResult(String id) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_BULK_DATA_OBJECT_REGISTRATION_RESULT_SQL,
					bulkDataObjectRegistrationResultRowMapper, id);

		} catch (IncorrectResultSizeDataAccessException irse) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get a bulk data object registration result: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcBulkDataObjectRegistrationTask> getBulkDataObjectRegistrationTasks(String userId)
			throws HpcException {
		try {
			return jdbcTemplate.query(GET_BULK_DATA_OBJECT_REGISTRATION_TASKS_FOR_USER_SQL,
					bulkDataObjectRegistrationTaskRowMapper, userId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get bulk data object registration tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcBulkDataObjectRegistrationResult> getBulkDataObjectRegistrationResults(String userId, int offset,
			int limit) throws HpcException {
		try {
			return jdbcTemplate.query(GET_BULK_DATA_OBJECT_REGISTRATION_RESULTS_SQL,
					bulkDataObjectRegistrationResultRowMapper, userId, offset, limit);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get bulk data object rwegistration results: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public int getBulkDataObjectRegistrationResultsCount(String userId) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_BULK_DATA_OBJECT_REGISTRATION_RESULTS_COUNT_SQL, Integer.class,
					userId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to count download results: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void insertDataObjectRegistrationResult(HpcDataObjectRegistrationResult registrationResult)
			throws HpcException {
		try {
			String fileId = null, containerId = null, containerName = null;
			HpcFileLocation sourceLocation = registrationResult.getSourceLocation();
			if (sourceLocation != null) {
				fileId = sourceLocation.getFileId();
				containerId = sourceLocation.getFileContainerId();
				containerName = sourceLocation.getFileContainerName();
			}

			jdbcTemplate.update(INSERT_DATA_OBJECT_REGISTRATION_RESULT_SQL, registrationResult.getId(),
					registrationResult.getPath(), registrationResult.getUserId(),
					registrationResult.getUploadMethod() != null ? registrationResult.getUploadMethod().value() : null,
					registrationResult.getResult(), registrationResult.getMessage(),
					registrationResult.getEffectiveTransferSpeed(), registrationResult.getDataTransferRequestId(),
					fileId, containerId, containerName, registrationResult.getCreated(),
					registrationResult.getCompleted());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to insert a data object registration result: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcBulkDataObjectRegistrationTask> getBulkDataObjectRegistrationTasksForDoc(String doc)
			throws HpcException {
		try {
			return jdbcTemplate.query(GET_BULK_DATA_OBJECT_REGISTRATION_TASKS_FOR_DOC_SQL,
					bulkDataObjectRegistrationTaskRowMapper, doc);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get bulk data object registration tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcBulkDataObjectRegistrationTask> getAllBulkDataObjectRegistrationTasks() throws HpcException {
		try {
			return jdbcTemplate.query(GET_ALL_BULK_DATA_OBJECT_REGISTRATION_TASKS_SQL,
					bulkDataObjectRegistrationTaskRowMapper);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get bulk data object registration tasks: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcBulkDataObjectRegistrationResult> getBulkDataObjectRegistrationResultsForDoc(String doc, int offset,
			int limit) throws HpcException {
		try {
			return jdbcTemplate.query(GET_BULK_DATA_OBJECT_REGISTRATION_RESULTS_FOR_DOC_SQL,
					bulkDataObjectRegistrationResultRowMapper, doc, offset, limit);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get bulk data object rwegistration results: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcBulkDataObjectRegistrationResult> getAllBulkDataObjectRegistrationResults(int offset, int limit)
			throws HpcException {
		try {
			return jdbcTemplate.query(GET_ALL_BULK_DATA_OBJECT_REGISTRATION_RESULTS_SQL,
					bulkDataObjectRegistrationResultRowMapper, offset, limit);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get bulk data object rwegistration results: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public int getBulkDataObjectRegistrationResultsCountForDoc(String doc) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_BULK_DATA_OBJECT_REGISTRATION_RESULTS_COUNT_FOR_DOC_SQL,
					Integer.class, doc);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to count download results: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public int getAllBulkDataObjectRegistrationResultsCount() throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_ALL_BULK_DATA_OBJECT_REGISTRATION_RESULTS_COUNT_SQL, Integer.class);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to count download results: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Convert a list of data object registration items into a JSON string.
	 *
	 * @param registrationItems The list of data object registration items.
	 * @return A JSON representation of the registration items.
	 */
	@SuppressWarnings("unchecked")
	private String toJSON(List<HpcBulkDataObjectRegistrationItem> registrationItems) {
		JSONArray jsonRegistrationItems = new JSONArray();
		for (HpcBulkDataObjectRegistrationItem registrationItem : registrationItems) {
			JSONObject jsonTask = new JSONObject();
			HpcDataObjectRegistrationTaskItem taskItem = registrationItem.getTask();
			jsonTask.put("path", taskItem.getPath());
			if (taskItem.getResult() != null) {
				jsonTask.put("result", taskItem.getResult().toString());
			}
			if (taskItem.getMessage() != null) {
				jsonTask.put("message", taskItem.getMessage());
			}
			if (taskItem.getCompleted() != null) {
				jsonTask.put("completed", taskItem.getCompleted().getTime().getTime());
			}
			if (taskItem.getEffectiveTransferSpeed() != null) {
				jsonTask.put("effectiveTransferSpeed", taskItem.getEffectiveTransferSpeed().toString());
			}
			if (taskItem.getPercentComplete() != null) {
				jsonTask.put("percentComplete", taskItem.getPercentComplete().toString());
			}
			if (taskItem.getSize() != null) {
				jsonTask.put("size", taskItem.getSize().toString());
			}

			JSONObject jsonRequest = new JSONObject();
			HpcDataObjectRegistrationRequest request = registrationItem.getRequest();
			if (request.getCreateParentCollections() != null) {
				jsonRequest.put("createParentCollection", request.getCreateParentCollections());
			}
			if (request.getCallerObjectId() != null) {
				jsonRequest.put("callerObjectId", request.getCallerObjectId());
			}
			if (request.getGlobusUploadSource() != null) {
				JSONObject jsonGlobusUploadSource = new JSONObject();
				HpcGlobusUploadSource globusUploadSource = request.getGlobusUploadSource();
				jsonGlobusUploadSource.put("sourceFileContainerId",
						globusUploadSource.getSourceLocation().getFileContainerId());
				jsonGlobusUploadSource.put("sourceFileId", globusUploadSource.getSourceLocation().getFileId());
				jsonRequest.put("globusUploadSource", jsonGlobusUploadSource);
			}
			if (request.getS3UploadSource() != null) {
				JSONObject jsonS3UploadSource = new JSONObject();
				HpcStreamingUploadSource s3UploadSource = request.getS3UploadSource();
				jsonS3UploadSource.put("sourceFileContainerId",
						s3UploadSource.getSourceLocation().getFileContainerId());
				jsonS3UploadSource.put("sourceFileId", s3UploadSource.getSourceLocation().getFileId());
				if (s3UploadSource.getAccount() != null) {
					HpcS3Account s3Account = s3UploadSource.getAccount();
					jsonS3UploadSource.put("accountAccessKey",
							Base64.getEncoder().encodeToString(encryptor.encrypt(s3Account.getAccessKey())));
					jsonS3UploadSource.put("accountSecretKey",
							Base64.getEncoder().encodeToString(encryptor.encrypt(s3Account.getSecretKey())));
					jsonS3UploadSource.put("region", s3Account.getRegion());
				}
				jsonRequest.put("s3UploadSource", jsonS3UploadSource);
			}
			if (request.getGoogleDriveUploadSource() != null) {
				JSONObject jsonGoogleDriveUploadSource = new JSONObject();
				HpcStreamingUploadSource googleUploadSource = request.getGoogleDriveUploadSource();
				jsonGoogleDriveUploadSource.put("sourceFileContainerId",
						googleUploadSource.getSourceLocation().getFileContainerId());
				jsonGoogleDriveUploadSource.put("sourceFileId", googleUploadSource.getSourceLocation().getFileId());
				if (googleUploadSource.getAccessToken() != null) {
					jsonGoogleDriveUploadSource.put("accessToken",
							Base64.getEncoder().encodeToString(encryptor.encrypt(googleUploadSource.getAccessToken())));
				}
				jsonRequest.put("googleDriveUploadSource", jsonGoogleDriveUploadSource);
			}
			if (request.getLinkSourcePath() != null) {
				jsonRequest.put("linkSourcePath", request.getLinkSourcePath());
			}

			jsonRequest.put("metadataEntries", toJSONArray(request.getMetadataEntries()));
			if (request.getParentCollectionsBulkMetadataEntries() != null) {
				jsonRequest.put("parentCollectionsBulkMetadataEntries",
						toJSON(request.getParentCollectionsBulkMetadataEntries()));
			}

			JSONObject jsonRegistrationItem = new JSONObject();
			jsonRegistrationItem.put("task", jsonTask);
			jsonRegistrationItem.put("request", jsonRequest);

			jsonRegistrationItems.add(jsonRegistrationItem);
		}

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("items", jsonRegistrationItems);

		return jsonObj.toJSONString();
	}

	/**
	 * Convert a list of metadata entries into a JSON string.
	 *
	 * @param metadataEntries The list of metadata entries
	 * @return A JSON representation of the metadata entries.
	 */
	@SuppressWarnings("unchecked")
	private JSONArray toJSONArray(List<HpcMetadataEntry> metadataEntries) {
		JSONArray jsonMetadataEntries = new JSONArray();
		for (HpcMetadataEntry metadataEntry : metadataEntries) {
			JSONObject jsonMetadataEntry = new JSONObject();
			jsonMetadataEntry.put("attribute", metadataEntry.getAttribute());
			jsonMetadataEntry.put("value", metadataEntry.getValue());
			if (metadataEntry.getUnit() != null) {
				jsonMetadataEntry.put("unit", metadataEntry.getUnit());
			}

			jsonMetadataEntries.add(jsonMetadataEntry);
		}

		return jsonMetadataEntries;
	}

	/**
	 * Convert a HpcBulkMetadataEntries object into a JSON string.
	 *
	 * @param bulkMetadataEntries The bulk metadata entries object to convert.
	 * @return A JSON representation of the bulk metadata entries.
	 */
	@SuppressWarnings("unchecked")
	private JSONObject toJSON(HpcBulkMetadataEntries bulkMetadataEntries) {
		JSONArray jsonPathsMetadataEntries = new JSONArray();
		bulkMetadataEntries.getPathsMetadataEntries()
				.forEach(bulkMetadataEntry -> jsonPathsMetadataEntries.add(toJSON(bulkMetadataEntry)));

		JSONObject jsonBulkMetadataEntries = new JSONObject();
		jsonBulkMetadataEntries.put("pathsMetadataEntries", jsonPathsMetadataEntries);
		jsonBulkMetadataEntries.put("defaultCollectionMetadataEntries",
				toJSONArray(bulkMetadataEntries.getDefaultCollectionMetadataEntries()));

		return jsonBulkMetadataEntries;
	}

	/**
	 * Convert a HpcBulkMetadataEntry object into a JSON string.
	 *
	 * @param bulkMetadataEntry The bulk metadata entry object.
	 * @return A JSON representation of the metadata entry object.
	 */
	@SuppressWarnings("unchecked")
	private JSONObject toJSON(HpcBulkMetadataEntry bulkMetadataEntry) {
		JSONObject jsonBulkMetadataEntry = new JSONObject();
		jsonBulkMetadataEntry.put("path", bulkMetadataEntry.getPath());
		jsonBulkMetadataEntry.put("pathMetadataEntries", toJSONArray(bulkMetadataEntry.getPathMetadataEntries()));

		return jsonBulkMetadataEntry;
	}

	/**
	 * Convert a JSON array to a list of metadata entries.
	 *
	 * @param jsonMetadataEntries The list of collection download items.
	 * @return A JSON representation of download items.
	 */
	@SuppressWarnings("unchecked")
	private List<HpcMetadataEntry> fromJSONArray(JSONArray jsonMetadataEntries) {
		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
		if (jsonMetadataEntries == null)
			return metadataEntries;

		jsonMetadataEntries.forEach((entry -> {
			JSONObject jsonMetadataEntry = (JSONObject) entry;
			HpcMetadataEntry metadataEntry = new HpcMetadataEntry();
			metadataEntry.setAttribute(jsonMetadataEntry.get("attribute").toString());
			metadataEntry.setValue(jsonMetadataEntry.get("value").toString());
			Object unit = jsonMetadataEntry.get("unit");
			if (unit != null) {
				metadataEntry.setUnit(unit.toString());
			}
			metadataEntries.add(metadataEntry);
		}));

		return metadataEntries;
	}

	/**
	 * Convert a JSON bulk metadata entries to a domain object
	 *
	 * @param jsonBulkMetadataEntries The bulk metadata entries JSON.
	 * @return The domain object
	 */
	@SuppressWarnings("unchecked")
	private HpcBulkMetadataEntries fromJSON(JSONObject jsonBulkMetadataEntries) {
		HpcBulkMetadataEntries bulkMetadataEntries = new HpcBulkMetadataEntries();

		// In case there are no path metadata entries, create an empty array.
		// This is needed to work around issue with UI not able to de-serialize null
		// array.
		bulkMetadataEntries.getPathsMetadataEntries();

		if (jsonBulkMetadataEntries.get("pathsMetadataEntries") != null) {
			JSONArray jsonPathsMetadataEntries = (JSONArray) jsonBulkMetadataEntries.get("pathsMetadataEntries");
			jsonPathsMetadataEntries.forEach(entry -> {
				JSONObject jsonBulkMetadataEntry = (JSONObject) entry;
				HpcBulkMetadataEntry bulkMetadataEntry = new HpcBulkMetadataEntry();
				bulkMetadataEntry.setPath(jsonBulkMetadataEntry.get("path").toString());
				bulkMetadataEntry.getPathMetadataEntries()
						.addAll(fromJSONArray((JSONArray) jsonBulkMetadataEntry.get("pathMetadataEntries")));

				bulkMetadataEntries.getPathsMetadataEntries().add(bulkMetadataEntry);
			});
		}

		bulkMetadataEntries.getDefaultCollectionMetadataEntries()
				.addAll(fromJSONArray((JSONArray) jsonBulkMetadataEntries.get("defaultCollectionMetadataEntries")));
		return bulkMetadataEntries;
	}

	/**
	 * Convert JSON string to a list of bulk data object registration items.
	 *
	 * @param jsonRegistrationItemsStr The registration items JSON string.
	 * @return A list of data object registration download items.
	 */
	@SuppressWarnings("unchecked")
	private List<HpcBulkDataObjectRegistrationItem> fromJSON(String jsonRegistrationItemsStr) {
		List<HpcBulkDataObjectRegistrationItem> registrationItems = new ArrayList<>();
		if (StringUtils.isEmpty(jsonRegistrationItemsStr)) {
			return registrationItems;
		}

		// Parse the JSON string.
		JSONObject jsonObj = null;
		try {
			jsonObj = (JSONObject) (new JSONParser().parse(jsonRegistrationItemsStr));

		} catch (ParseException e) {
			return registrationItems;
		}

		// Map the download items.
		JSONArray jsonRegistrationItems = (JSONArray) jsonObj.get("items");
		if (jsonRegistrationItems != null) {
			Iterator<JSONObject> registrationItemIterator = jsonRegistrationItems.iterator();
			while (registrationItemIterator.hasNext()) {
				HpcBulkDataObjectRegistrationItem registrationItem = new HpcBulkDataObjectRegistrationItem();
				JSONObject jsonRegistrationItem = registrationItemIterator.next();

				registrationItem.setTask(toRegistrationTask((JSONObject) jsonRegistrationItem.get("task")));
				registrationItem.setRequest(toRegistrationRequest((JSONObject) jsonRegistrationItem.get("request")));

				registrationItems.add(registrationItem);
			}
		}

		return registrationItems;
	}

	/**
	 * Convert JSON registration task to the domain object.
	 *
	 * @param jsonTask The registration task JSON string.
	 * @return A registration task item.
	 */
	private HpcDataObjectRegistrationTaskItem toRegistrationTask(JSONObject jsonTask) {
		if (jsonTask == null) {
			return null;
		}

		HpcDataObjectRegistrationTaskItem task = new HpcDataObjectRegistrationTaskItem();
		task.setPath(jsonTask.get("path").toString());

		Object result = jsonTask.get("result");
		if (result != null) {
			task.setResult(Boolean.valueOf(result.toString()));
		}

		Object message = jsonTask.get("message");
		if (message != null) {
			task.setMessage(message.toString());
		}

		Object completed = jsonTask.get("completed");
		if (completed != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis((Long) completed);
			task.setCompleted(cal);
		}

		Object effectiveTransferSpeed = jsonTask.get("effectiveTransferSpeed");
		if (effectiveTransferSpeed != null) {
			task.setEffectiveTransferSpeed(Integer.valueOf(effectiveTransferSpeed.toString()));
		}

		Object percentComplete = jsonTask.get("percentComplete");
		if (percentComplete != null) {
			task.setPercentComplete(Integer.valueOf(percentComplete.toString()));
		}

		Object size = jsonTask.get("size");
		if (size != null) {
			task.setSize(Long.valueOf(size.toString()));
		}

		return task;
	}

	/**
	 * Convert JSON registration request to the domain object.
	 *
	 * @param jsonRequest The registration request JSON string.
	 * @return A registration request.
	 */
	private HpcDataObjectRegistrationRequest toRegistrationRequest(JSONObject jsonRequest) {
		if (jsonRequest == null) {
			return null;
		}

		HpcDataObjectRegistrationRequest request = new HpcDataObjectRegistrationRequest();
		Object callerObjectId = jsonRequest.get("callerObjectId");
		if (callerObjectId != null) {
			request.setCallerObjectId(callerObjectId.toString());
		}

		Object createParentCollection = jsonRequest.get("createParentCollection");
		if (createParentCollection != null) {
			request.setCreateParentCollections(Boolean.valueOf(createParentCollection.toString()));
		}

		// HpcDataObjectRegistrationRequest data structure has changed to support upload
		// from AWS S3
		// This code ensures we are backwards compatible with the old structure, as we
		// have data in the
		// DB
		// with the old structure.
		if (jsonRequest.get("sourceFileContainerId") != null && jsonRequest.get("sourceFileId") != null) {
			HpcGlobusUploadSource globusUploadSource = new HpcGlobusUploadSource();
			HpcFileLocation source = new HpcFileLocation();
			source.setFileContainerId(jsonRequest.get("sourceFileContainerId").toString());
			source.setFileId(jsonRequest.get("sourceFileId").toString());
			globusUploadSource.setSourceLocation(source);
			request.setGlobusUploadSource(globusUploadSource);
		}

		if (jsonRequest.get("globusUploadSource") != null) {
			JSONObject jsonGlobusUploadSource = (JSONObject) jsonRequest.get("globusUploadSource");
			HpcGlobusUploadSource globusUploadSource = new HpcGlobusUploadSource();
			HpcFileLocation source = new HpcFileLocation();
			source.setFileContainerId(jsonGlobusUploadSource.get("sourceFileContainerId").toString());
			source.setFileId(jsonGlobusUploadSource.get("sourceFileId").toString());
			globusUploadSource.setSourceLocation(source);
			request.setGlobusUploadSource(globusUploadSource);
		}

		if (jsonRequest.get("s3UploadSource") != null) {
			JSONObject jsonS3UploadSource = (JSONObject) jsonRequest.get("s3UploadSource");
			HpcStreamingUploadSource s3UploadSource = new HpcStreamingUploadSource();
			HpcFileLocation source = new HpcFileLocation();
			source.setFileContainerId(jsonS3UploadSource.get("sourceFileContainerId").toString());
			source.setFileId(jsonS3UploadSource.get("sourceFileId").toString());
			s3UploadSource.setSourceLocation(source);
			if (jsonS3UploadSource.get("accountAccessKey") != null
					&& jsonS3UploadSource.get("accountSecretKey") != null) {
				HpcS3Account s3Account = new HpcS3Account();
				s3Account.setAccessKey(encryptor
						.decrypt(Base64.getDecoder().decode(jsonS3UploadSource.get("accountAccessKey").toString())));
				s3Account.setSecretKey(encryptor
						.decrypt(Base64.getDecoder().decode(jsonS3UploadSource.get("accountSecretKey").toString())));
				s3Account.setRegion(jsonS3UploadSource.get("region").toString());
				s3UploadSource.setAccount(s3Account);
			}
			request.setS3UploadSource(s3UploadSource);
		}

		if (jsonRequest.get("googleDriveUploadSource") != null) {
			JSONObject jsonGoogleDriveUploadSource = (JSONObject) jsonRequest.get("googleDriveUploadSource");
			HpcStreamingUploadSource googleDriveUploadSource = new HpcStreamingUploadSource();
			HpcFileLocation source = new HpcFileLocation();
			source.setFileContainerId(jsonGoogleDriveUploadSource.get("sourceFileContainerId").toString());
			source.setFileId(jsonGoogleDriveUploadSource.get("sourceFileId").toString());
			googleDriveUploadSource.setSourceLocation(source);
			if (jsonGoogleDriveUploadSource.get("accessToken") != null) {
				googleDriveUploadSource.setAccessToken(encryptor.decrypt(
						Base64.getDecoder().decode(jsonGoogleDriveUploadSource.get("accessToken").toString())));
			}
			request.setGoogleDriveUploadSource(googleDriveUploadSource);
		}

		Object linkSourcePath = jsonRequest.get("linkSourcePath");
		if (linkSourcePath != null) {
			request.setLinkSourcePath(linkSourcePath.toString());
		}

		Object metadataEntries = jsonRequest.get("metadataEntries");
		if (metadataEntries != null) {
			request.getMetadataEntries().addAll(fromJSONArray((JSONArray) metadataEntries));
		}

		Object parentCollectionsBulkMetadataEntries = jsonRequest.get("parentCollectionsBulkMetadataEntries");
		if (parentCollectionsBulkMetadataEntries != null) {
			request.setParentCollectionsBulkMetadataEntries(
					fromJSON((JSONObject) parentCollectionsBulkMetadataEntries));
		}

		return request;
	}
}
