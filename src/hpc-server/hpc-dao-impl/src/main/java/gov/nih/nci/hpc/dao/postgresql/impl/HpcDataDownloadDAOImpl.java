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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import gov.nih.nci.hpc.dao.HpcDataDownloadDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
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
                   "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", \"DESTINATION_LOCATION_FILE_ID\", \"CREATED\") " + 
                   "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
           "on conflict(\"ID\") do update set \"USER_ID\"=excluded.\"USER_ID\", " + 
                        "\"PATH\"=excluded.\"PATH\", " + 
                        "\"DOC\"=excluded.\"DOC\", " + 
                        "\"DATA_TRANSFER_REQUEST_ID\"=excluded.\"DATA_TRANSFER_REQUEST_ID\", " + 
                        "\"DATA_TRANSFER_TYPE\"=excluded.\"DATA_TRANSFER_TYPE\", " +
                        "\"DOWNLOAD_FILE_PATH\"=excluded.\"DOWNLOAD_FILE_PATH\", " +
                        "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\"=excluded.\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", " +
                        "\"DESTINATION_LOCATION_FILE_ID\"=excluded.\"DESTINATION_LOCATION_FILE_ID\", " +
                        "\"CREATED\"=excluded.\"CREATED\"";
	
	
	public static final String DELETE_DATA_OBJECT_DOWNLOAD_TASK_SQL = 
		   "delete from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" where " + "\"ID\" = ?";

	public static final String GET_DATA_OBJECT_DOWNLOAD_TASKS_SQL = 
		   "select * from public.\"HPC_DATA_OBJECT_DOWNLOAD_TASK\" where " + "\"DATA_TRANSFER_TYPE\" = ?";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// HpcDataObjectDownloadCleanup table to object mapper.
	private RowMapper<HpcDataObjectDownloadTask> rowMapper = (rs, rowNum) -> 
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
    	created.setTime(rs.getDate("CREATED"));
    	dataObjectDownloadTask.setCreated(created);
		
        return dataObjectDownloadTask;
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
					    		 dataObjectDownloadTask.getCreated());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a data object download task: " + e.getMessage(),
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
	public List<HpcDataObjectDownloadTask> getDataObjectDownloadTasks(HpcDataTransferType dataTransferType) throws HpcException
	{
		try {
		     return jdbcTemplate.query(GET_DATA_OBJECT_DOWNLOAD_TASKS_SQL, rowMapper, dataTransferType.value());
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get data object download cleanup: " + 
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
								 		                            new String[] {"\"ID\""});
								return ps;
							}, keyHolder);
		
		return keyHolder.getKey().intValue();
	}
}

 