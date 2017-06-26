/**
 * HpcDataObjectDownloadCleanupDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import gov.nih.nci.hpc.dao.HpcDataObjectDownloadCleanupDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadCleanup;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * <p>
 * HPC Data Object Download Cleanup DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcDataObjectDownloadCleanupDAOImpl implements HpcDataObjectDownloadCleanupDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	public static final String UPSERT_SQL = 
		   "insert into public.\"HPC_DATA_OBJECT_DOWNLOAD_CLEANUP\" ( " +
                    "\"USER_ID\", \"PATH\", \"DOC\", \"DATA_TRANSFER_REQUEST_ID\", \"DATA_TRANSFER_TYPE\", \"DOWNLOAD_FILE_PATH\"," +
                    "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", \"DESTINATION_LOCATION_FILE_ID\") " +
                    "values (?, ?, ?, ?, ?, ?, ?, ?) " +
           "on conflict(\"DATA_TRANSFER_REQUEST_ID\") do update set \"USER_ID\"=excluded.\"USER_ID\", " + 
                        "\"PATH\"=excluded.\"PATH\", " + 
                        "\"DOC\"=excluded.\"DOC\", " + 
                        "\"DATA_TRANSFER_REQUEST_ID\"=excluded.\"DATA_TRANSFER_REQUEST_ID\", " + 
                        "\"DATA_TRANSFER_TYPE\"=excluded.\"DATA_TRANSFER_TYPE\", " +
                        "\"DOWNLOAD_FILE_PATH\"=excluded.\"DOWNLOAD_FILE_PATH\", " +
                        "\"DESTINATION_LOCATION_FILE_CONTAINER_ID\"=excluded.\"DESTINATION_LOCATION_FILE_CONTAINER_ID\", " +
                        "\"DESTINATION_LOCATION_FILE_ID\"=excluded.\"DESTINATION_LOCATION_FILE_ID\"";
	
	public static final String DELETE_SQL = 
		   "delete from public.\"HPC_DATA_OBJECT_DOWNLOAD_CLEANUP\" where " +
	                    "\"DATA_TRANSFER_REQUEST_ID\" = ?";

	public static final String GET_ALL_SQL = 
		   "select * from public.\"HPC_DATA_OBJECT_DOWNLOAD_CLEANUP\"";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Row mapper.
	private HpcDataObjectDownloadCleanupRowMapper rowMapper = new HpcDataObjectDownloadCleanupRowMapper();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcDataObjectDownloadCleanupDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataObjectDownloadCleanupDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void upsert(HpcDataObjectDownloadCleanup dataObjectDownloadCleanup) 
			          throws HpcException
    {
		
		try {
		     jdbcTemplate.update(UPSERT_SQL,
		    		             dataObjectDownloadCleanup.getUserId(),
		    		             dataObjectDownloadCleanup.getPath(),
		    		             dataObjectDownloadCleanup.getDoc(),
		    		             dataObjectDownloadCleanup.getDataTransferRequestId(),
		    		             dataObjectDownloadCleanup.getDataTransferType().value(),
		    		             dataObjectDownloadCleanup.getDownloadFilePath(),
		    		             dataObjectDownloadCleanup.getDestinationLocation().getFileContainerId(),
		    		             dataObjectDownloadCleanup.getDestinationLocation().getFileId());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a data object download cleanup: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
    }
	
	@Override
	public void delete(String dataTransferRequestId) throws HpcException
	{
		try {
		     jdbcTemplate.update(DELETE_SQL, dataTransferRequestId);
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to delete a data object download cleanup: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}
	
	@Override 
	public List<HpcDataObjectDownloadCleanup> getAll() throws HpcException
	{
		try {
		     return jdbcTemplate.query(GET_ALL_SQL, rowMapper);
		     
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
	
	// HpcUser Table to Object mapper.
	private class HpcDataObjectDownloadCleanupRowMapper 
	              implements RowMapper<HpcDataObjectDownloadCleanup>
	{
		@Override
		public HpcDataObjectDownloadCleanup mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			HpcDataObjectDownloadCleanup dataObjectDownloadCleanup = new HpcDataObjectDownloadCleanup();
			dataObjectDownloadCleanup.setUserId(rs.getString("USER_ID"));
			dataObjectDownloadCleanup.setDoc(rs.getString("DOC"));
			dataObjectDownloadCleanup.setPath(rs.getString("PATH"));
			dataObjectDownloadCleanup.setDataTransferRequestId(rs.getString("DATA_TRANSFER_REQUEST_ID"));
			dataObjectDownloadCleanup.setDataTransferType(
					  HpcDataTransferType.fromValue(rs.getString(("DATA_TRANSFER_TYPE"))));
			dataObjectDownloadCleanup.setDownloadFilePath(rs.getString("DOWNLOAD_FILE_PATH"));
			HpcFileLocation destinationLocation = new HpcFileLocation();
			destinationLocation.setFileContainerId(rs.getString("DESTINATION_LOCATION_FILE_CONTAINER_ID"));
			destinationLocation.setFileId(rs.getString("DESTINATION_LOCATION_FILE_ID"));
			dataObjectDownloadCleanup.setDestinationLocation(destinationLocation);
            
            return dataObjectDownloadCleanup;
		}
	}
}

 