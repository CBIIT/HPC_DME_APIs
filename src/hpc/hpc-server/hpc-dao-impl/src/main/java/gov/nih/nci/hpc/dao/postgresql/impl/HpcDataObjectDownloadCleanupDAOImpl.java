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
import gov.nih.nci.hpc.domain.error.HpcErrorType;
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
 * @version $Id $
 */

public class HpcDataObjectDownloadCleanupDAOImpl implements HpcDataObjectDownloadCleanupDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	public static final String UPSERT_SQL = 
		   "insert into public.\"HPC_DATA_OBJECT_DOWNLOAD_CLEANUP\" ( " +
                    "\"DATA_TRANSFER_REQUEST_ID\", \"DATA_TRANSFER_TYPE\", \"FILE_PATH\") " +
                    "values (?, ?, ?) " +
           "on conflict(\"REQUEST_ID\") do update set \"TRANSFER_TYPE\"=excluded.\"DATA_TRANSFER_TYPE\", " +
                                                     "\"FILE_PATH\"=excluded.\"FILE_PATH\"";
	
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
    // HpcManagedUserDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void upsert(HpcDataObjectDownloadCleanup dataObjectDownloadCleanup) 
			          throws HpcException
    {
		try {
		     jdbcTemplate.update(UPSERT_SQL,
		    		             dataObjectDownloadCleanup.getDataTransferRequestId(),
		    		             dataObjectDownloadCleanup.getDataTransferType().value(),
		    		             dataObjectDownloadCleanup.getFilePath());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a data object download cleanup: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, e);
		}
    }
	
	@Override
	public void delete(String dataTransferRequestId) throws HpcException
	{
		try {
		     jdbcTemplate.update(DELETE_SQL, dataTransferRequestId);
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to delete a data object download cleanup: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, e);
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
		        throw new HpcException("Failed to get a system account: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
	// HpcUser Table to Object mapper.
	private class HpcDataObjectDownloadCleanupRowMapper 
	              implements RowMapper<HpcDataObjectDownloadCleanup>
	{
		public HpcDataObjectDownloadCleanup mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			HpcDataObjectDownloadCleanup dataObjectDownloadCleanup = new HpcDataObjectDownloadCleanup();
			dataObjectDownloadCleanup.setDataTransferRequestId(rs.getString("DATA_TRANSFER_REQUEST_ID"));
			dataObjectDownloadCleanup.setDataTransferType(
					  HpcDataTransferType.fromValue(rs.getString(("DATA_TRANSFER_TYPE"))));
            
            return dataObjectDownloadCleanup;
		}
	}
}

 