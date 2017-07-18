/**
 * HpcDataDownloadDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import gov.nih.nci.hpc.dao.HpcDataDownloadDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTaskResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Object Download Cleanup DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcDataDownloadDAOImpl implements HpcDataDownloadDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	public static final String INSERT_DATA_OBJECT_DOWNLOAD_TASK_SQL = 
		   "insert into public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" (\"USER_ID\") values(NULL)";
	public static final String UPSERT_DATA_OBJECT_DOWNLOAD_TASK_SQL = 
		   "insert into public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" ( " +
                   "\"ID\", \"USER_ID\", \"PATH\", \"DOC\", \"DATA_TRANSFER_REQUEST_ID\", \"DATA_TRANSFER_TYPE\", \"DOWNLOAD_FILE_PATH\"," +
                   "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", \"DESTINATION_LOCATION_FILE_ID\", " + 
                   "\"COMPLETION_EVENT\", \"CREATED\") " + 
                   "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
           "on conflict(\"ID\") do update set \"USER_ID\"=excluded.\"USER_ID\", " + 
                        "\"PATH\"=excluded.\"PATH\", " + 
                        "\"DOC\"=excluded.\"DOC\", " + 
                        "\"DATA_TRANSFER_REQUEST_ID\"=excluded.\"DATA_TRANSFER_REQUEST_ID\", " + 
                        "\"DATA_TRANSFER_TYPE\"=excluded.\"DATA_TRANSFER_TYPE\", " +
                        "\"DOWNLOAD_FILE_PATH\"=excluded.\"DOWNLOAD_FILE_PATH\", " +
                        "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\"=excluded.\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", " +
                        "\"DESTINATION_LOCATION_FILE_ID\"=excluded.\"DESTINATION_LOCATION_FILE_ID\", " +
                        "\"COMPLETION_EVENT\"=excluded.\"COMPLETION_EVENT\", " +
                        "\"CREATED\"=excluded.\"CREATED\"";
	
	public static final String DELETE_DATA_OBJECT_DOWNLOAD_TASK_SQL = 
		   "delete from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" where " + "\"ID\" = ?";

	public static final String GET_DATA_OBJECT_DOWNLOAD_TASK_SQL = 
		   "select * from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" where " + "\"ID\" = ?";
	
	public static final String GET_DATA_OBJECT_DOWNLOAD_TASKS_SQL = 
		   "select * from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" where " + "\"DATA_TRANSFER_TYPE\" = ?";
	
	public static final String UPSERT_DATA_OBJECT_DOWNLOAD_TASK_RESULT_SQL = 
		   "insert into public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK_RESULT\" ( " +
                   "\"ID\", \"USER_ID\", \"PATH\", \"DOC\", \"DATA_TRANSFER_REQUEST_ID\", \"DATA_TRANSFER_TYPE\", " +
                   "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", \"DESTINATION_LOCATION_FILE_ID\", \"RESULT\", " +
                   "\"MESSAGE\", \"CREATED\", \"COMPLETED\") " + 
                   "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
           "on conflict(\"ID\") do update set \"USER_ID\"=excluded.\"USER_ID\", " + 
                        "\"PATH\"=excluded.\"PATH\", " + 
                        "\"DOC\"=excluded.\"DOC\", " + 
                        "\"DATA_TRANSFER_REQUEST_ID\"=excluded.\"DATA_TRANSFER_REQUEST_ID\", " + 
                        "\"DATA_TRANSFER_TYPE\"=excluded.\"DATA_TRANSFER_TYPE\", " +
                        "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\"=excluded.\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", " +
                        "\"DESTINATION_LOCATION_FILE_ID\"=excluded.\"DESTINATION_LOCATION_FILE_ID\", " +
                        "\"RESULT\"=excluded.\"RESULT\", " +
                        "\"MESSAGE\"=excluded.\"MESSAGE\", " +
                        "\"CREATED\"=excluded.\"CREATED\", " +
                        "\"COMPLETED\"=excluded.\"COMPLETED\"";
	
	public static final String GET_DATA_OBJECT_DOWNLOAD_TASK_RESULT_SQL = 
		   "select * from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK_RESULT\" where " + "\"ID\" = ?";
	
	public static final String INSERT_COLLECTION_DOWNLOAD_TASK_SQL = 
		   "insert into public.\"HPC_COLLECTION_DOWNLOAD_TASK\" (\"USER_ID\") values(NULL)";
	
	public static final String UPSERT_COLLECTION_DOWNLOAD_TASK_SQL = 
		   "insert into public.\"HPC_COLLECTION_DOWNLOAD_TASK\" ( " +
                   "\"ID\", \"USER_ID\", \"PATH\", \"DESTINATION_LOCATION_FILE_CONTAINER_ID\", " + 
				   "\"DESTINATION_LOCATION_FILE_ID\", \"STATUS\", \"CREATED\") " + 
                   "values (?, ?, ?, ?, ?, ?, ??) " +
           "on conflict(\"ID\") do update set \"USER_ID\"=excluded.\"USER_ID\", " + 
                        "\"PATH\"=excluded.\"PATH\", " + 
                        "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\"=excluded.\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", " +
                        "\"DESTINATION_LOCATION_FILE_ID\"=excluded.\"DESTINATION_LOCATION_FILE_ID\", " +
                        "\"STATUS\"=excluded.\"STATUS\", " +
                        "\"CREATED\"=excluded.\"CREATED\"";
	
	public static final String GET_COLLECTION_DOWNLOAD_TASKS_SQL = 
		   "select * from public.\"HPC_COLLECTION_DOWNLOAD_TASK\" where " + "\"STATUS\" = ?";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// HpcDataObjectDownloadCleanup table to object mapper.
	private RowMapper<HpcDataObjectDownloadTask> dataObjectDownloadTaskRowMapper = (rs, rowNum) -> 
	{
		HpcDataObjectDownloadTask dataObjectDownloadTask = new HpcDataObjectDownloadTask();
		dataObjectDownloadTask.setId(rs.getInt("ID"));
		dataObjectDownloadTask.setUserId(rs.getString("USER_ID"));
		dataObjectDownloadTask.setDoc(rs.getString("DOC"));
		dataObjectDownloadTask.setPath(rs.getString("PATH"));
		dataObjectDownloadTask.setDataTransferRequestId(rs.getString("DATA_TRANSFER_REQUEST_ID"));
		dataObjectDownloadTask.setDataTransferType(
				  HpcDataTransferType.fromValue(rs.getString(("DATA_TRANSFER_TYPE"))));
		dataObjectDownloadTask.setDownloadFilePath(rs.getString("DOWNLOAD_FILE_PATH"));
		dataObjectDownloadTask.setCompletionEvent(rs.getBoolean("COMPLETION_EVENT"));
		
		String destinationLocationFileContainerId = rs.getString("DESTINATION_LOCATION_FILE_CONTAINER_ID");
		String destinationLocationFileId = rs.getString("DESTINATION_LOCATION_FILE_ID");
		if(destinationLocationFileContainerId != null && 
		   destinationLocationFileId != null) {
		   HpcFileLocation destinationLocation = new HpcFileLocation();
		   destinationLocation.setFileContainerId(destinationLocationFileContainerId);
		   destinationLocation.setFileId(destinationLocationFileId);
		   dataObjectDownloadTask.setDestinationLocation(destinationLocation);
		}
    	Calendar created = Calendar.getInstance();
    	created.setTime(rs.getTimestamp("CREATED"));
    	dataObjectDownloadTask.setCreated(created);
		
        return dataObjectDownloadTask;
	};
	
	// HpcDataObjectDownloadCleanup table to object mapper.
	private RowMapper<HpcDataObjectDownloadTaskResult> dataObjectDownloadTaskResultRowMapper = (rs, rowNum) -> 
	{
		HpcDataObjectDownloadTaskResult dataObjectDownloadTaskResult = new HpcDataObjectDownloadTaskResult();
		dataObjectDownloadTaskResult.setId(rs.getInt("ID"));
		dataObjectDownloadTaskResult.setUserId(rs.getString("USER_ID"));
		dataObjectDownloadTaskResult.setDoc(rs.getString("DOC"));
		dataObjectDownloadTaskResult.setPath(rs.getString("PATH"));
		dataObjectDownloadTaskResult.setDataTransferRequestId(rs.getString("DATA_TRANSFER_REQUEST_ID"));
		dataObjectDownloadTaskResult.setDataTransferType(
				  HpcDataTransferType.fromValue(rs.getString(("DATA_TRANSFER_TYPE"))));
		String destinationLocationFileContainerId = rs.getString("DESTINATION_LOCATION_FILE_CONTAINER_ID");
		String destinationLocationFileId = rs.getString("DESTINATION_LOCATION_FILE_ID");
		if(destinationLocationFileContainerId != null && 
		   destinationLocationFileId != null) {
		   HpcFileLocation destinationLocation = new HpcFileLocation();
		   destinationLocation.setFileContainerId(destinationLocationFileContainerId);
		   destinationLocation.setFileId(destinationLocationFileId);
		   dataObjectDownloadTaskResult.setDestinationLocation(destinationLocation);
		}
		dataObjectDownloadTaskResult.setResult(rs.getBoolean("RESULT"));
		dataObjectDownloadTaskResult.setMessage(rs.getString("MESSAGE"));
    	Calendar created = Calendar.getInstance();
    	created.setTime(rs.getTimestamp("CREATED"));
    	dataObjectDownloadTaskResult.setCreated(created);
    	
    	Calendar completed = Calendar.getInstance();
    	created.setTime(rs.getTimestamp("COMPLETED"));
    	dataObjectDownloadTaskResult.setCompleted(completed);
		
        return dataObjectDownloadTaskResult;
	};
	
	// HpcDataObjectDownloadCleanup table to object mapper.
	private RowMapper<HpcCollectionDownloadTask> collectionDownloadTaskRowMapper = (rs, rowNum) -> 
	{
		HpcCollectionDownloadTask collectionDownloadTask = new HpcCollectionDownloadTask();
		collectionDownloadTask.setId(rs.getInt("ID"));
		collectionDownloadTask.setUserId(rs.getString("USER_ID"));
		collectionDownloadTask.setPath(rs.getString("PATH"));
		collectionDownloadTask.setStatus(
				  HpcCollectionDownloadTaskStatus.fromValue(rs.getString(("STATUS"))));
		collectionDownloadTask.setMessage(rs.getString("MESSAGE"));
		String destinationLocationFileContainerId = rs.getString("DESTINATION_LOCATION_FILE_CONTAINER_ID");
		String destinationLocationFileId = rs.getString("DESTINATION_LOCATION_FILE_ID");
		if(destinationLocationFileContainerId != null && 
		   destinationLocationFileId != null) {
		   HpcFileLocation destinationLocation = new HpcFileLocation();
		   destinationLocation.setFileContainerId(destinationLocationFileContainerId);
		   destinationLocation.setFileId(destinationLocationFileId);
		   collectionDownloadTask.setDestinationLocation(destinationLocation);
		}
		
    	Calendar created = Calendar.getInstance();
    	created.setTime(rs.getTimestamp("CREATED"));
    	collectionDownloadTask.setCreated(created);
    	
        return collectionDownloadTask;
	};
	
    // The logger instance.
	private static final Logger logger = LoggerFactory.getLogger(HpcDataDownloadDAOImpl.class.getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcDataDownloadDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataDownloadDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void upsertDataObjectDownloadTask(HpcDataObjectDownloadTask dataObjectDownloadTask) 
			                                throws HpcException
    {
		try {
			 if(dataObjectDownloadTask.getId() == null) {
			    dataObjectDownloadTask.setId(nextDataObjectDownloadTaskId());
			 }
			 
		     jdbcTemplate.update(UPSERT_DATA_OBJECT_DOWNLOAD_TASK_SQL,
		    		             dataObjectDownloadTask.getId(),
					    		 dataObjectDownloadTask.getUserId(),
					    		 dataObjectDownloadTask.getPath(),
					    		 dataObjectDownloadTask.getDoc(),
					    		 dataObjectDownloadTask.getDataTransferRequestId(),
					    		 dataObjectDownloadTask.getDataTransferType().value(),
					    		 dataObjectDownloadTask.getDownloadFilePath(),
					    		 dataObjectDownloadTask.getDestinationLocation().getFileContainerId(),
					    		 dataObjectDownloadTask.getDestinationLocation().getFileId(),
					    		 dataObjectDownloadTask.getCompletionEvent(),
					    		 dataObjectDownloadTask.getCreated());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a data object download task: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
    }
	
	@Override 
	public HpcDataObjectDownloadTask getDataObjectDownloadTask(int id) throws HpcException
	{
		try {
		     return jdbcTemplate.queryForObject(GET_DATA_OBJECT_DOWNLOAD_TASK_SQL, 
		    		                            dataObjectDownloadTaskRowMapper, id);
		     
		} catch(IncorrectResultSizeDataAccessException irse) {
			    logger.error("Multiple tasks with the same ID found", irse);
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get a data object download task: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}
	
	@Override
	public void deleteDataObjectDownloadTask(int id) throws HpcException
	{
		try {
		     jdbcTemplate.update(DELETE_DATA_OBJECT_DOWNLOAD_TASK_SQL, id);
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to delete a data object download task: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}
	
	@Override 
	public List<HpcDataObjectDownloadTask> getDataObjectDownloadTasks(HpcDataTransferType dataTransferType) 
			                                                         throws HpcException
	{
		try {
		     return jdbcTemplate.query(GET_DATA_OBJECT_DOWNLOAD_TASKS_SQL, 
		    		                   dataObjectDownloadTaskRowMapper, dataTransferType.value());
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get data object download tasks: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}
	
	@Override
    public void upsertDataObjectDownloadResult(HpcDataObjectDownloadTask dataObjectDownloadTask,
                                               boolean result, String message, Calendar completed) 
                                              throws HpcException
    {
		try {
		     jdbcTemplate.update(UPSERT_DATA_OBJECT_DOWNLOAD_TASK_RESULT_SQL,
		    		             dataObjectDownloadTask.getId(),
					    		 dataObjectDownloadTask.getUserId(),
					    		 dataObjectDownloadTask.getPath(),
					    		 dataObjectDownloadTask.getDoc(),
					    		 dataObjectDownloadTask.getDataTransferRequestId(),
					    		 dataObjectDownloadTask.getDataTransferType().value(),
					    		 dataObjectDownloadTask.getDestinationLocation().getFileContainerId(),
					    		 dataObjectDownloadTask.getDestinationLocation().getFileId(),
					    		 result, message,
					    		 dataObjectDownloadTask.getCreated(), completed);
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a data object download task: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}		
    }
	
	@Override 
	public HpcDataObjectDownloadTaskResult getDataObjectDownloadTaskResult(int id) throws HpcException
	{
		try {
		     return jdbcTemplate.queryForObject(GET_DATA_OBJECT_DOWNLOAD_TASK_RESULT_SQL, 
		    		                            dataObjectDownloadTaskResultRowMapper, id);
		     
		} catch(IncorrectResultSizeDataAccessException irse) {
			    logger.error("Multiple tasks results with the same ID found", irse);
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get a data object download task result: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}
	
	public void upsertCollectionDownloadTask(HpcCollectionDownloadTask collectionDownloadTask) 
                                               throws HpcException
    {
		try {
			 if(collectionDownloadTask.getId() == null) {
				 collectionDownloadTask.setId(nextCollectionDownloadTaskId());
			 }
			 
		     jdbcTemplate.update(UPSERT_COLLECTION_DOWNLOAD_TASK_SQL,
					    		 collectionDownloadTask.getId(),
					    		 collectionDownloadTask.getUserId(),
					    		 collectionDownloadTask.getPath(),
					    		 collectionDownloadTask.getDestinationLocation().getFileContainerId(),
					    		 collectionDownloadTask.getDestinationLocation().getFileId(),
					    		 collectionDownloadTask.getStatus().value(),
					    		 collectionDownloadTask.getCreated());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a collection download request: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
    }
	
	@Override
	public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(
                   HpcCollectionDownloadTaskStatus status) 
                   throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_COLLECTION_DOWNLOAD_TASKS_SQL, 
		    		                   collectionDownloadTaskRowMapper, status.value());
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get collection download requests: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
    }
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Insert a blank data object download task and return its id.
     *
     * @return A newly created data object download task id.
     */
	private int nextDataObjectDownloadTaskId() 
	{
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update((connection) ->
		                    {
								PreparedStatement ps =
								        connection.prepareStatement(INSERT_DATA_OBJECT_DOWNLOAD_TASK_SQL, 
								 		                            new String[] {"ID"});
								return ps;
							}, keyHolder);
		
		return keyHolder.getKey().intValue();
	}
	
    /**
     * Insert a blank collection download request and return its id.
     *
     * @return A newly created collection download request id.
     */
	private int nextCollectionDownloadTaskId() 
	{
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update((connection) ->
		                    {
								PreparedStatement ps =
								        connection.prepareStatement(INSERT_COLLECTION_DOWNLOAD_TASK_SQL, 
								 		                            new String[] {"ID"});
								return ps;
							}, keyHolder);
		
		return keyHolder.getKey().intValue();
	}
}

 