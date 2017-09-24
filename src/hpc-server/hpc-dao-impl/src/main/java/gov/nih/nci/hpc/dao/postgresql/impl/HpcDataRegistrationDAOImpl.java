/**
 * HpcDataRegistrationDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

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

import gov.nih.nci.hpc.dao.HpcDataRegistrationDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObjectListRegistrationTaskStatus;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObjectRegistrationTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcDataObjectListRegistrationItem;
import gov.nih.nci.hpc.domain.model.HpcDataObjectListRegistrationResult;
import gov.nih.nci.hpc.domain.model.HpcDataObjectListRegistrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationRequest;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Registration DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcDataRegistrationDAOImpl implements HpcDataRegistrationDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	public static final String UPSERT_DATA_OBJECT_LIST_REGISTRATION_TASK_SQL = 
		   "insert into public.\"HPC_DATA_OBJECT_LIST_REGISTRATION_TASK\" ( " +
                   "\"ID\", \"USER_ID\", \"DOC\", \"STATUS\", \"ITEMS\", \"CREATED\") " + 
                   "values (?, ?, ?, ?, ?, ?) " +
           "on conflict(\"ID\") do update set \"USER_ID\"=excluded.\"USER_ID\", " + 
                        "\"DOC\"=excluded.\"DOC\", " + 
                        "\"STATUS\"=excluded.\"STATUS\", " +
                        "\"ITEMS\"=excluded.\"ITEMS\", " +
                        "\"CREATED\"=excluded.\"CREATED\"";
	
	public static final String GET_DATA_OBJECT_LIST_REGISTRATION_TASK_SQL = 
		   "select * from public.\"HPC_DATA_OBJECT_LIST_REGISTRATION_TASK\" where \"ID\" = ?";
	
	public static final String DELETE_DATA_OBJECT_LIST_REGISTRATION_TASK_SQL = 
		   "delete from public.\"HPC_DATA_OBJECT_LIST_REGISTRATION_TASK\" where \"ID\" = ?";
	
	public static final String GET_DATA_OBJECT_LIST_REGISTRATION_TASKS_SQL = 
		   "select * from public.\"HPC_DATA_OBJECT_LIST_REGISTRATION_TASK\" where \"STATUS\" = ? " +
		   "order by \"CREATED\"";
	
	public static final String UPSERT_DATA_OBJECT_LIST_REGISTRATION_RESULT_SQL = 
		   "insert into public.\"HPC_DATA_OBJECT_LIST_REGISTRATION_RESULT\" ( " +
	                    "\"ID\", \"USER_ID\", \"RESULT\", \"MESSAGE\", \"ITEMS\", \"CREATED\", \"COMPLETED\") " + 
	                    "values (?, ?, ?, ?, ?, ?, ?) " +
	       "on conflict(\"ID\") do update set \"USER_ID\"=excluded.\"USER_ID\", " + 
	                    "\"RESULT\"=excluded.\"RESULT\", " + 
	                    "\"MESSAGE\"=excluded.\"MESSAGE\", " +
	                    "\"ITEMS\"=excluded.\"ITEMS\", " +
	                    "\"CREATED\"=excluded.\"CREATED\", " +
	                    "\"COMPLETED\"=excluded.\"COMPLETED\"";
	
	public static final String GET_DATA_OBJECT_LIST_REGISTRATION_RESULT_SQL = 
		   "select * from public.\"HPC_DATA_OBJECT_LIST_REGISTRATION_RESULT\" where \"ID\" = ?";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// HpcDataObjectListRegistrationTask table to object mapper.
	private RowMapper<HpcDataObjectListRegistrationTask> dataObjectListRegistrationTaskRowMapper = (rs, rowNum) -> 
	{
		HpcDataObjectListRegistrationTask dataObjectListRegistrationTask = new HpcDataObjectListRegistrationTask();
		dataObjectListRegistrationTask.setId(rs.getString("ID"));
		dataObjectListRegistrationTask.setUserId(rs.getString("USER_ID"));
		dataObjectListRegistrationTask.setDoc(rs.getString("DOC"));
		dataObjectListRegistrationTask.setStatus(
				  HpcDataObjectListRegistrationTaskStatus.fromValue(rs.getString(("STATUS"))));
		dataObjectListRegistrationTask.getItems().addAll(fromJSON(rs.getString("ITEMS")));
		
    	Calendar created = Calendar.getInstance();
    	created.setTime(rs.getTimestamp("CREATED"));
    	dataObjectListRegistrationTask.setCreated(created);
    	
        return dataObjectListRegistrationTask;
	};
	
	// HpcDataObjectListRegistrationResulr table to object mapper.
	private RowMapper<HpcDataObjectListRegistrationResult> dataObjectListRegistrationResultRowMapper = (rs, rowNum) -> 
	{
		HpcDataObjectListRegistrationResult dataObjectListRegistrationResult = new HpcDataObjectListRegistrationResult();
		dataObjectListRegistrationResult.setId(rs.getString("ID"));
		dataObjectListRegistrationResult.setUserId(rs.getString("USER_ID"));
		dataObjectListRegistrationResult.setResult(rs.getBoolean("RESULT"));
		dataObjectListRegistrationResult.setMessage(rs.getString("MESSAGE"));
		dataObjectListRegistrationResult.getItems().addAll(fromJSON(rs.getString("ITEMS")));
		
    	Calendar created = Calendar.getInstance();
    	created.setTime(rs.getTimestamp("CREATED"));
    	dataObjectListRegistrationResult.setCreated(created);
    	
    	Calendar completed = Calendar.getInstance();
    	completed.setTime(rs.getTimestamp("COMPLETED"));
    	dataObjectListRegistrationResult.setCompleted(completed);
    	
        return dataObjectListRegistrationResult;
	};
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcDataRegistrationDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataRegistrationDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
	public void upsertDataObjectListRegistrationTask(
			          HpcDataObjectListRegistrationTask dataObjectListRegistrationTask) 
                      throws HpcException
    {
		try {
			 if(dataObjectListRegistrationTask.getId() == null) {
				dataObjectListRegistrationTask.setId(UUID.randomUUID().toString());
			 }
			 
		     jdbcTemplate.update(UPSERT_DATA_OBJECT_LIST_REGISTRATION_TASK_SQL,
		    		             dataObjectListRegistrationTask.getId(),
		    		             dataObjectListRegistrationTask.getUserId(),
		    		             dataObjectListRegistrationTask.getDoc(),
		    		             dataObjectListRegistrationTask.getStatus().value(),
		    		             toJSON(dataObjectListRegistrationTask.getItems()),
		    		             dataObjectListRegistrationTask.getCreated());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a data object list registration request: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		} 
    }
    
    @Override
    public HpcDataObjectListRegistrationTask getDataObjectListRegistrationTask(String id) throws HpcException
    {
		try {
		     return jdbcTemplate.queryForObject(GET_DATA_OBJECT_LIST_REGISTRATION_TASK_SQL, 
		    		                            dataObjectListRegistrationTaskRowMapper, id);
		     
		} catch(IncorrectResultSizeDataAccessException irse) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get a data object list registration task: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}    
    }
    
    @Override
    public void deleteDataObjectListRegistrationTask(String id) throws HpcException
	{
		try {
		     jdbcTemplate.update(DELETE_DATA_OBJECT_LIST_REGISTRATION_TASK_SQL, id);
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to delete a data object list registration task: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}
    
    @Override
    public List<HpcDataObjectListRegistrationTask> 
           getDataObjectListRegistrationTasks(HpcDataObjectListRegistrationTaskStatus status) 
                                             throws HpcException
	{
    	try {
		     return jdbcTemplate.query(GET_DATA_OBJECT_LIST_REGISTRATION_TASKS_SQL, 
		    		                   dataObjectListRegistrationTaskRowMapper, status.value());
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get data object list registration tasks: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}
    
    @Override
    public void upsertDataObjectListRegistrationResult(HpcDataObjectListRegistrationResult registrationResult) 
                                                      throws HpcException
    {
    	try {
   		     jdbcTemplate.update(UPSERT_DATA_OBJECT_LIST_REGISTRATION_RESULT_SQL,
   		    		             registrationResult.getId(),
   		    		             registrationResult.getUserId(),
   		    		             registrationResult.getResult(),
   		    		             registrationResult.getMessage(),
   		    		             toJSON(registrationResult.getItems()),
   		    		             registrationResult.getCreated(),
   		    		             registrationResult.getCompleted());
   		     
   		} catch(DataAccessException e) {
   			    throw new HpcException("Failed to upsert a data object list registration result: " + e.getMessage(),
   			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
   		} 
    }
    
    @Override
    public HpcDataObjectListRegistrationResult getDataObjectListRegistrationResult(String id) throws HpcException
    {
		try {
		     return jdbcTemplate.queryForObject(GET_DATA_OBJECT_LIST_REGISTRATION_RESULT_SQL, 
		    		                            dataObjectListRegistrationResultRowMapper, id);
		     
		} catch(IncorrectResultSizeDataAccessException irse) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get a data object list registration result: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}        	
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /** 
     * Convert a list of data object registration items into a JSON string.
     * 
     * @param downloadItems The list of collection download items.
     * @return A JSON representation of download items.
     */
	@SuppressWarnings("unchecked")
	private String toJSON(List<HpcDataObjectListRegistrationItem> registrationItems)
	{
		JSONArray jsonRegistrationItems = new JSONArray();
		for(HpcDataObjectListRegistrationItem registrationItem : registrationItems) {
			JSONObject jsonTask = new JSONObject();
			HpcDataObjectRegistrationTaskItem taskItem = registrationItem.getTask();
			jsonTask.put("path", taskItem.getPath());
			if(taskItem.getResult() != null) {
			   jsonTask.put("result", taskItem.getResult().toString());
			}
			if(taskItem.getMessage() != null) {
			   jsonTask.put("message", taskItem.getMessage());
			}
			if(taskItem.getCompleted() != null) {
			   jsonTask.put("completed", taskItem.getCompleted().getTime().getTime());
			}
			
			JSONObject jsonRequest = new JSONObject();
			HpcDataObjectRegistrationRequest request = registrationItem.getRequest();
			if(request.getCreateParentCollections() != null) {
			   jsonRequest.put("createParentCollection", request.getCreateParentCollections());
			}
			if(request.getCallerObjectId() != null) {
			   jsonRequest.put("callerObjectId", request.getCallerObjectId());
			}
			
			jsonRequest.put("sourceFileContainerId", request.getSource().getFileContainerId());
			jsonRequest.put("sourceFileId", request.getSource().getFileId());
			jsonRequest.put("metadataEntries", toJSONArray(request.getMetadataEntries()));
			jsonRequest.put("parentCollectionMetadataEntries", 
					        toJSONArray(request.getParentCollectionMetadataEntries()));
			
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
	private JSONArray toJSONArray(List<HpcMetadataEntry> metadataEntries)
	{
		JSONArray jsonMetadataEntries = new JSONArray();
		for(HpcMetadataEntry metadataEntry : metadataEntries) {
			JSONObject jsonMetadataEntry = new JSONObject();
			jsonMetadataEntry.put("attribute", metadataEntry.getAttribute());
			jsonMetadataEntry.put("value", metadataEntry.getValue());
			if(metadataEntry.getUnit() != null) {
			   jsonMetadataEntry.put("unit", metadataEntry.getUnit());
			}
			
			jsonMetadataEntries.add(jsonMetadataEntry);
		}
		
		return jsonMetadataEntries;
	}
	
    /** 
     * Convert a JSON array to a list of metadata entries.
     * 
     * @param jsonMetadataEntries The list of collection download items.
     * @return A JSON representation of download items.
     */
	@SuppressWarnings("unchecked")
	private List<HpcMetadataEntry> fromJSONArray(JSONArray jsonMetadataEntries)
	{
		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
		jsonMetadataEntries.forEach((entry -> {
			JSONObject jsonMetadataEntry = (JSONObject) entry;
			HpcMetadataEntry metadataEntry = new HpcMetadataEntry();
			metadataEntry.setAttribute(jsonMetadataEntry.get("attribute").toString());
			metadataEntry.setValue(jsonMetadataEntry.get("value").toString());
			Object unit = jsonMetadataEntry.get("unit");
			if(unit != null) {
			   metadataEntry.setUnit(unit.toString());
			}
			metadataEntries.add(metadataEntry);
		}));
		
		return metadataEntries;
	}
	
    /** 
     * Convert JSON string to a list of data object list registration items.
     * 
     * @param jsonRegistrationItemsStr The registration items JSON string.
     * @return A list of data object registration download items.
     */
	@SuppressWarnings("unchecked")
	private List<HpcDataObjectListRegistrationItem> fromJSON(String jsonRegistrationItemsStr)
	{
		List<HpcDataObjectListRegistrationItem> registrationItems = new ArrayList<>();
		if(StringUtils.isEmpty(jsonRegistrationItemsStr)) {
		   return registrationItems;
		}

		// Parse the JSON string.
		JSONObject jsonObj = null;
		try {
			 jsonObj = (JSONObject) (new JSONParser().parse(jsonRegistrationItemsStr));
			 
		} catch(ParseException e) {
			    return registrationItems;
		}
		
		// Map the download items.
	    JSONArray jsonRegistrationItems = (JSONArray) jsonObj.get("items");
  	    if(jsonRegistrationItems != null) {
		   Iterator<JSONObject> registrationItemIterator = jsonRegistrationItems.iterator();
	       while(registrationItemIterator.hasNext()) {
	    	     HpcDataObjectListRegistrationItem registrationItem = new HpcDataObjectListRegistrationItem();
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
	private HpcDataObjectRegistrationTaskItem toRegistrationTask(JSONObject jsonTask)
	{
		if(jsonTask == null) {
		   return null;
		}
		
	   	HpcDataObjectRegistrationTaskItem task = new HpcDataObjectRegistrationTaskItem();
	   	task.setPath(jsonTask.get("path").toString());
	   	
	   	Object result = jsonTask.get("result");
	    if(result != null) {
	       task.setResult(Boolean.valueOf(result.toString()));
	    }
	    
	    Object message = jsonTask.get("message");
	    if(message != null) {
	       task.setMessage(message.toString());
	    }
   	
	    Object completed = jsonTask.get("completed");
	    if(completed != null) {
	       Calendar cal = Calendar.getInstance();
	       cal.setTimeInMillis((Long) completed); 
	       task.setCompleted(cal);
	    }
	
		return task;
	}
	
    /** 
     * Convert JSON registration request to the domain object.
     * 
     * @param jsonRequest The registration request JSON string.
     * @return A registration request.
     */
	private HpcDataObjectRegistrationRequest toRegistrationRequest(JSONObject jsonRequest)
	{
		if(jsonRequest == null) {
		   return null;
		}
		
		HpcDataObjectRegistrationRequest request = new HpcDataObjectRegistrationRequest();
		Object callerObjectId = jsonRequest.get("callerObjectId");
	    if(callerObjectId != null) {
	       request.setCallerObjectId(callerObjectId.toString());
	    }
	    
	    Object createParentCollection = jsonRequest.get("createParentCollection");
	    if(createParentCollection != null) {
	       request.setCreateParentCollections(Boolean.valueOf(createParentCollection.toString()));
	    }
	    
	    HpcFileLocation source = new HpcFileLocation();
	    source.setFileContainerId(jsonRequest.get("sourceFileContainerId").toString());
	    source.setFileId(jsonRequest.get("sourceFileId").toString());
	    request.setSource(source);
	    
	    Object metadataEntries = jsonRequest.get("metadataEntries");
	    if(metadataEntries != null) {
	       request.getMetadataEntries().addAll(fromJSONArray((JSONArray) metadataEntries));
	    }
	    
	    Object parentCollectionMetadataEntries = jsonRequest.get("parentCollectionMetadataEntries");
	    if(parentCollectionMetadataEntries != null) {
	       request.getParentCollectionMetadataEntries().addAll(
	    		      fromJSONArray((JSONArray) parentCollectionMetadataEntries));
	    }
	
		return request;
	}
}

 