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

import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
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
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
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
	public static final String UPSERT_DATA_OBJECT_DOWNLOAD_TASK_SQL = 
		   "insert into public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" ( " +
                   "\"ID\", \"USER_ID\", \"PATH\", \"DOC\", \"DATA_TRANSFER_REQUEST_ID\", \"DATA_TRANSFER_TYPE\", " + 
				   "\"DATA_TRANSFER_STATUS\", \"DOWNLOAD_FILE_PATH\"," +
				   "\"ARCHIVE_LOCATION_FILE_CONTAINER_ID\", \"ARCHIVE_LOCATION_FILE_ID\", " + 
                   "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", \"DESTINATION_LOCATION_FILE_ID\", " + 
                   "\"COMPLETION_EVENT\", \"CREATED\") " + 
                   "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
           "on conflict(\"ID\") do update set \"USER_ID\"=excluded.\"USER_ID\", " + 
                        "\"PATH\"=excluded.\"PATH\", " + 
                        "\"DOC\"=excluded.\"DOC\", " + 
                        "\"DATA_TRANSFER_REQUEST_ID\"=excluded.\"DATA_TRANSFER_REQUEST_ID\", " + 
                        "\"DATA_TRANSFER_TYPE\"=excluded.\"DATA_TRANSFER_TYPE\", " +
                        "\"DATA_TRANSFER_STATUS\"=excluded.\"DATA_TRANSFER_STATUS\", " +
                        "\"DOWNLOAD_FILE_PATH\"=excluded.\"DOWNLOAD_FILE_PATH\", " +
                        "\"ARCHIVE_LOCATION_FILE_CONTAINER_ID\"=excluded.\"ARCHIVE_LOCATION_FILE_CONTAINER_ID\", " +
                        "\"ARCHIVE_LOCATION_FILE_ID\"=excluded.\"ARCHIVE_LOCATION_FILE_ID\", " +
                        "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\"=excluded.\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", " +
                        "\"DESTINATION_LOCATION_FILE_ID\"=excluded.\"DESTINATION_LOCATION_FILE_ID\", " +
                        "\"COMPLETION_EVENT\"=excluded.\"COMPLETION_EVENT\", " +
                        "\"CREATED\"=excluded.\"CREATED\"";
	
	public static final String DELETE_DATA_OBJECT_DOWNLOAD_TASK_SQL = 
		   "delete from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" where \"ID\" = ?";

	public static final String GET_DATA_OBJECT_DOWNLOAD_TASK_SQL = 
		   "select * from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" where \"ID\" = ?";
	
	public static final String GET_DATA_OBJECT_DOWNLOAD_TASKS_SQL = 
		   "select * from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" where \"DATA_TRANSFER_TYPE\" = ? " +
	                 " order by \"CREATED\"";
	
	public static final String UPSERT_DOWNLOAD_TASK_RESULT_SQL = 
		   "insert into public.\"HPC_DOWNLOAD_TASK_RESULT\" ( " +
                   "\"ID\", \"USER_ID\", \"PATH\", \"DOC\", \"DATA_TRANSFER_REQUEST_ID\", \"DATA_TRANSFER_TYPE\", " +
                   "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", \"DESTINATION_LOCATION_FILE_ID\", \"RESULT\", " +
                   "\"TYPE\", \"MESSAGE\", \"ITEMS\", \"CREATED\", \"COMPLETED\") " + 
                   "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
           "on conflict on constraint \"HPC_DOWNLOAD_TASK_RESULT_pkey\" do update set \"USER_ID\"=excluded.\"USER_ID\", " + 
                        "\"PATH\"=excluded.\"PATH\", " + 
                        "\"DOC\"=excluded.\"DOC\", " + 
                        "\"DATA_TRANSFER_REQUEST_ID\"=excluded.\"DATA_TRANSFER_REQUEST_ID\", " + 
                        "\"DATA_TRANSFER_TYPE\"=excluded.\"DATA_TRANSFER_TYPE\", " +
                        "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\"=excluded.\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", " +
                        "\"DESTINATION_LOCATION_FILE_ID\"=excluded.\"DESTINATION_LOCATION_FILE_ID\", " +
                        "\"RESULT\"=excluded.\"RESULT\", " +
                        "\"TYPE\"=excluded.\"TYPE\", " +
                        "\"MESSAGE\"=excluded.\"MESSAGE\", " +
                        "\"ITEMS\"=excluded.\"ITEMS\", " +
                        "\"CREATED\"=excluded.\"CREATED\", " +
                        "\"COMPLETED\"=excluded.\"COMPLETED\"";
	
	public static final String GET_DOWNLOAD_TASK_RESULT_SQL = 
		   "select * from public.\"HPC_DOWNLOAD_TASK_RESULT\" where \"ID\" = ? and \"TYPE\" = ?";
	
	public static final String UPSERT_COLLECTION_DOWNLOAD_TASK_SQL = 
		   "insert into public.\"HPC_COLLECTION_DOWNLOAD_TASK\" ( " +
                   "\"ID\", \"USER_ID\", \"PATH\", \"DESTINATION_LOCATION_FILE_CONTAINER_ID\", " + 
				   "\"DESTINATION_LOCATION_FILE_ID\", \"ITEMS\", \"STATUS\", \"TYPE\", " + 
                   "\"DATA_OBJECT_PATHS\", \"CREATED\") " + 
                   "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
           "on conflict(\"ID\") do update set \"USER_ID\"=excluded.\"USER_ID\", " + 
                        "\"PATH\"=excluded.\"PATH\", " + 
                        "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\"=excluded.\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", " +
                        "\"DESTINATION_LOCATION_FILE_ID\"=excluded.\"DESTINATION_LOCATION_FILE_ID\", " +
                        "\"ITEMS\"=excluded.\"ITEMS\", " +
                        "\"STATUS\"=excluded.\"STATUS\", " +
                        "\"TYPE\"=excluded.\"TYPE\", " +
                        "\"DATA_OBJECT_PATHS\"=excluded.\"DATA_OBJECT_PATHS\", " +
                        "\"CREATED\"=excluded.\"CREATED\"";
	
	public static final String GET_COLLECTION_DOWNLOAD_TASK_SQL = 
		   "select * from public.\"HPC_COLLECTION_DOWNLOAD_TASK\" where \"ID\" = ?";
	
	public static final String DELETE_COLLECTION_DOWNLOAD_TASK_SQL = 
		   "delete from public.\"HPC_COLLECTION_DOWNLOAD_TASK\" where \"ID\" = ?";
	
	public static final String GET_COLLECTION_DOWNLOAD_TASKS_SQL = 
		   "select * from public.\"HPC_COLLECTION_DOWNLOAD_TASK\" where \"STATUS\" = ? " +
	                 "order by \"CREATED\"";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// HpcDataObjectDownloadTask table to object mapper.
	private RowMapper<HpcDataObjectDownloadTask> dataObjectDownloadTaskRowMapper = (rs, rowNum) -> 
	{
		HpcDataObjectDownloadTask dataObjectDownloadTask = new HpcDataObjectDownloadTask();
		dataObjectDownloadTask.setId(rs.getString("ID"));
		dataObjectDownloadTask.setUserId(rs.getString("USER_ID"));
		dataObjectDownloadTask.setDoc(rs.getString("DOC"));
		dataObjectDownloadTask.setPath(rs.getString("PATH"));
		dataObjectDownloadTask.setDataTransferRequestId(rs.getString("DATA_TRANSFER_REQUEST_ID"));
		dataObjectDownloadTask.setDataTransferType(
				  HpcDataTransferType.fromValue(rs.getString(("DATA_TRANSFER_TYPE"))));
		dataObjectDownloadTask.setDataTransferStatus(
				  HpcDataTransferDownloadStatus.fromValue(rs.getString(("DATA_TRANSFER_STATUS"))));
		dataObjectDownloadTask.setDownloadFilePath(rs.getString("DOWNLOAD_FILE_PATH"));
		dataObjectDownloadTask.setCompletionEvent(rs.getBoolean("COMPLETION_EVENT"));
		
		String archiveLocationFileContainerId = rs.getString("ARCHIVE_LOCATION_FILE_CONTAINER_ID");
		String archiveLocationFileId = rs.getString("ARCHIVE_LOCATION_FILE_ID");
		if(archiveLocationFileContainerId != null && 
		   archiveLocationFileId != null) {
		   HpcFileLocation archiveLocation = new HpcFileLocation();
		   archiveLocation.setFileContainerId(archiveLocationFileContainerId);
		   archiveLocation.setFileId(archiveLocationFileId);
		   dataObjectDownloadTask.setArchiveLocation(archiveLocation);
		}
		
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
	
	// HpcDownloadTaskResult table to object mapper.
	private RowMapper<HpcDownloadTaskResult> downloadTaskResultRowMapper = (rs, rowNum) -> 
	{
		HpcDownloadTaskResult downloadTaskResult = new HpcDownloadTaskResult();
		downloadTaskResult.setId(rs.getString("ID"));
		downloadTaskResult.setUserId(rs.getString("USER_ID"));
		downloadTaskResult.setDoc(rs.getString("DOC"));
		downloadTaskResult.setType(HpcDownloadTaskType.fromValue(rs.getString(("TYPE"))));
		downloadTaskResult.setPath(rs.getString("PATH"));
		downloadTaskResult.setDataTransferRequestId(rs.getString("DATA_TRANSFER_REQUEST_ID"));
		String dataTransferType = rs.getString("DATA_TRANSFER_TYPE");
		downloadTaskResult.setDataTransferType(dataTransferType != null ?
				                               HpcDataTransferType.fromValue(dataTransferType) : null);
		String destinationLocationFileContainerId = rs.getString("DESTINATION_LOCATION_FILE_CONTAINER_ID");
		String destinationLocationFileId = rs.getString("DESTINATION_LOCATION_FILE_ID");
		if(destinationLocationFileContainerId != null && 
		   destinationLocationFileId != null) {
		   HpcFileLocation destinationLocation = new HpcFileLocation();
		   destinationLocation.setFileContainerId(destinationLocationFileContainerId);
		   destinationLocation.setFileId(destinationLocationFileId);
		   downloadTaskResult.setDestinationLocation(destinationLocation);
		}
		downloadTaskResult.setResult(rs.getBoolean("RESULT"));
		downloadTaskResult.setMessage(rs.getString("MESSAGE"));
		downloadTaskResult.getItems().addAll(fromJSON(rs.getString("ITEMS")));
		
    	Calendar created = Calendar.getInstance();
    	created.setTime(rs.getTimestamp("CREATED"));
    	downloadTaskResult.setCreated(created);
    	
    	Calendar completed = Calendar.getInstance();
    	completed.setTime(rs.getTimestamp("COMPLETED"));
    	downloadTaskResult.setCompleted(completed);
		
        return downloadTaskResult;
	};
	
	// HpcCollectionDownloadTask table to object mapper.
	private RowMapper<HpcCollectionDownloadTask> collectionDownloadTaskRowMapper = (rs, rowNum) -> 
	{
		HpcCollectionDownloadTask collectionDownloadTask = new HpcCollectionDownloadTask();
		collectionDownloadTask.setId(rs.getString("ID"));
		collectionDownloadTask.setUserId(rs.getString("USER_ID"));
		collectionDownloadTask.setPath(rs.getString("PATH"));
		collectionDownloadTask.setType(HpcDownloadTaskType.fromValue(rs.getString(("TYPE"))));
		collectionDownloadTask.setStatus(
				  HpcCollectionDownloadTaskStatus.fromValue(rs.getString(("STATUS"))));
		String destinationLocationFileContainerId = rs.getString("DESTINATION_LOCATION_FILE_CONTAINER_ID");
		String destinationLocationFileId = rs.getString("DESTINATION_LOCATION_FILE_ID");
		if(destinationLocationFileContainerId != null && 
		   destinationLocationFileId != null) {
		   HpcFileLocation destinationLocation = new HpcFileLocation();
		   destinationLocation.setFileContainerId(destinationLocationFileContainerId);
		   destinationLocation.setFileId(destinationLocationFileId);
		   collectionDownloadTask.setDestinationLocation(destinationLocation);
		}
		collectionDownloadTask.getItems().addAll(fromJSON(rs.getString("ITEMS")));
		
    	Calendar created = Calendar.getInstance();
    	created.setTime(rs.getTimestamp("CREATED"));
    	collectionDownloadTask.setCreated(created);
    	
		// Extract the data objects paths.
    	Array sqlArray = rs.getArray("DATA_OBJECT_PATHS");
    	if(sqlArray != null) {
		   String[] dataObjectPaths = (String[]) sqlArray.getArray();
		   int dataObjectPathsSize = dataObjectPaths.length;
		   for(int i = 0; i < dataObjectPathsSize; i++) {
			   collectionDownloadTask.getDataObjectPaths().add(dataObjectPaths[i]);
		   }
		}
    	
        return collectionDownloadTask;
	};
	
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
			    dataObjectDownloadTask.setId(UUID.randomUUID().toString());
			 }
			 
		     jdbcTemplate.update(UPSERT_DATA_OBJECT_DOWNLOAD_TASK_SQL,
		    		             dataObjectDownloadTask.getId(),
					    		 dataObjectDownloadTask.getUserId(),
					    		 dataObjectDownloadTask.getPath(),
					    		 dataObjectDownloadTask.getDoc(),
					    		 dataObjectDownloadTask.getDataTransferRequestId(),
					    		 dataObjectDownloadTask.getDataTransferType().value(),
					    		 dataObjectDownloadTask.getDataTransferStatus().value(),
					    		 dataObjectDownloadTask.getDownloadFilePath(),
					    		 dataObjectDownloadTask.getArchiveLocation().getFileContainerId(),
					    		 dataObjectDownloadTask.getArchiveLocation().getFileId(),
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
	public HpcDataObjectDownloadTask getDataObjectDownloadTask(String id) throws HpcException
	{
		try {
		     return jdbcTemplate.queryForObject(GET_DATA_OBJECT_DOWNLOAD_TASK_SQL, 
		    		                            dataObjectDownloadTaskRowMapper, id);
		     
		} catch(IncorrectResultSizeDataAccessException irse) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get a data object download task: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}
	
	@Override
	public void deleteDataObjectDownloadTask(String id) throws HpcException
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
    public void upsertDownloadTaskResult(HpcDownloadTaskResult taskResult)
                                        throws HpcException
    {
		try {
		     jdbcTemplate.update(UPSERT_DOWNLOAD_TASK_RESULT_SQL,
					    		 taskResult.getId(),
					    		 taskResult.getUserId(),
					    		 taskResult.getPath(),
					    		 taskResult.getDoc(),
					    		 taskResult.getDataTransferRequestId(),
					    		 taskResult.getDataTransferType() != null ? 
					    		     taskResult.getDataTransferType().value() : null,
					    		 taskResult.getDestinationLocation().getFileContainerId(),
					    		 taskResult.getDestinationLocation().getFileId(),
					    		 taskResult.getResult(),
					    		 taskResult.getType().value(),
					    		 taskResult.getMessage(),
					    		 toJSON(taskResult.getItems()),
					    		 taskResult.getCreated(),
					    		 taskResult.getCompleted());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a download task result: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}	
    }
	
	@Override 
	public HpcDownloadTaskResult getDownloadTaskResult(String id, HpcDownloadTaskType taskType) 
			                                          throws HpcException
	{
		try {
		     return jdbcTemplate.queryForObject(GET_DOWNLOAD_TASK_RESULT_SQL, 
		    		                            downloadTaskResultRowMapper, id, taskType.value());
		     
		} catch(IncorrectResultSizeDataAccessException irse) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get a download task result: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}
	
	public void upsertCollectionDownloadTask(HpcCollectionDownloadTask collectionDownloadTask) 
                                               throws HpcException
    {
		try {
			 if(collectionDownloadTask.getId() == null) {
				collectionDownloadTask.setId(UUID.randomUUID().toString());
			 }
			 
			 Array sqlArray = null;
			 if(!collectionDownloadTask.getDataObjectPaths().isEmpty()) {
				Connection conn = jdbcTemplate.getDataSource().getConnection();
				try {
					 sqlArray = conn.createArrayOf("text", collectionDownloadTask.getDataObjectPaths().toArray());
					 
				} finally {
					       conn.close();
				}
			 }
			 
		     jdbcTemplate.update(UPSERT_COLLECTION_DOWNLOAD_TASK_SQL,
					    		 collectionDownloadTask.getId(),
					    		 collectionDownloadTask.getUserId(),
					    		 collectionDownloadTask.getPath(),
					    		 collectionDownloadTask.getDestinationLocation().getFileContainerId(),
					    		 collectionDownloadTask.getDestinationLocation().getFileId(),
					    		 toJSON(collectionDownloadTask.getItems()),
					    		 collectionDownloadTask.getStatus().value(),
					    		 collectionDownloadTask.getType().value(),
					    		 sqlArray,
					    		 collectionDownloadTask.getCreated());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a collection download request: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		} catch(SQLException se) {
		        throw new HpcException("Failed to upsert a collection download request: " + se.getMessage(),
		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, se);
}
    }
	
	@Override 
	public HpcCollectionDownloadTask getCollectionDownloadTask(String id) throws HpcException
	{
		try {
		     return jdbcTemplate.queryForObject(GET_COLLECTION_DOWNLOAD_TASK_SQL, 
		    		                            collectionDownloadTaskRowMapper, id);
		     
		} catch(IncorrectResultSizeDataAccessException irse) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get a collection download task: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}
	
	@Override
	public void deleteCollectionDownloadTask(String id) throws HpcException
	{
		try {
		     jdbcTemplate.update(DELETE_COLLECTION_DOWNLOAD_TASK_SQL, id);
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to delete a collection download task: " + e.getMessage(),
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
		        throw new HpcException("Failed to get collection download tasks: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
    }
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /** 
     * Convert a list of collection download items into a JSON string.
     * 
     * @param downloadItems The list of collection download items.
     * @return A JSON representation of download items.
     */
	@SuppressWarnings("unchecked")
	private String toJSON(List<HpcCollectionDownloadTaskItem> downloadItems)
	{
		JSONArray jsonDownloadItems = new JSONArray();
		for(HpcCollectionDownloadTaskItem downloadItem : downloadItems) {
			JSONObject jsonDownloadItem = new JSONObject();
			jsonDownloadItem.put("path", downloadItem.getPath());
			if(downloadItem.getDataObjectDownloadTaskId() != null) {
			   jsonDownloadItem.put("dataObjectDownloadTaskId", downloadItem.getDataObjectDownloadTaskId());
			}
			if(downloadItem.getMessage() != null) {
			   jsonDownloadItem.put("message", downloadItem.getMessage());
			}
			if(downloadItem.getResult() != null) {
			   jsonDownloadItem.put("result", downloadItem.getResult());
			}
			jsonDownloadItem.put("destinationLocationFileContainerId", downloadItem.getDestinationLocation().getFileContainerId());
			jsonDownloadItem.put("destinationLocationFileId", downloadItem.getDestinationLocation().getFileId());
			
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
	private List<HpcCollectionDownloadTaskItem> fromJSON(String jsonDownloadItemsStr)
	{
		List<HpcCollectionDownloadTaskItem> downloadItems = new ArrayList<>();
		if(StringUtils.isEmpty(jsonDownloadItemsStr)) {
		   return downloadItems;
		}

		// Parse the JSON string.
		JSONObject jsonObj = null;
		try {
			 jsonObj = (JSONObject) (new JSONParser().parse(jsonDownloadItemsStr));
			 
		} catch(ParseException e) {
			    return downloadItems;
		}
		
		// Map the download items.
	    JSONArray jsonDownloadItems = (JSONArray) jsonObj.get("items");
  	    if(jsonDownloadItems != null) {
		   Iterator<JSONObject> downloadItemIterator = jsonDownloadItems.iterator();
	       while(downloadItemIterator.hasNext()) {
	    	     JSONObject jsonDownloadItem = downloadItemIterator.next();
	    	     HpcCollectionDownloadTaskItem downloadItem = new HpcCollectionDownloadTaskItem();
	    	     downloadItem.setPath(jsonDownloadItem.get("path").toString());
	    	     
	    	     Object dataObjectDownloadTaskId = jsonDownloadItem.get("dataObjectDownloadTaskId");
	    	     if(dataObjectDownloadTaskId != null) {
	    	    	downloadItem.setDataObjectDownloadTaskId(dataObjectDownloadTaskId.toString());
	    	     }
	    	     
	    	     Object message = jsonDownloadItem.get("message");
	    	     if(message != null) {
	    	    	downloadItem.setMessage(message.toString());
	    	     }
	    	     
	    	     Object result = jsonDownloadItem.get("result");
	    	     if(result != null) {
	    	    	downloadItem.setResult(Boolean.valueOf(result.toString()));
	    	     }
	    	     
	    	    HpcFileLocation destinationLocation = new HpcFileLocation();
	    	    destinationLocation.setFileContainerId(
	    	    		   jsonDownloadItem.get("destinationLocationFileContainerId").toString());
	    	    destinationLocation.setFileId(
	    	    		   jsonDownloadItem.get("destinationLocationFileId").toString());
	    	    downloadItem.setDestinationLocation(destinationLocation); 

	    	    downloadItems.add(downloadItem);
	 		}
  	    }
	    	     
	    return downloadItems;     
	}	
}

 