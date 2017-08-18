/**
 * HpcDataTransferQueueDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import gov.nih.nci.hpc.dao.HpcDataTransferQueueDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Globus transfer (upload/download) queues DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcDataTransferQueueDAOImpl implements HpcDataTransferQueueDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	public static final String UPSERT_UPLOAD_QUEUE_SQL = 
		   "insert into public.\"HPC_DATA_TRANSFER_UPLOAD_QUEUE\" ( " +
                   "\"PATH\", \"CALLER_OBJECT_ID\", \"USER_ID\", \"SOURCE_LOCATION_FILE_CONTAINER_ID\", " +
				   "\"SOURCE_LOCATION_FILE_ID\", \"DOC\") " + 
                   "values (?, ?, ?, ?, ?, ?) " +
           "on conflict(\"PATH\") do update set \"CALLER_OBJECT_ID\"=excluded.\"CALLER_OBJECT_ID\", " + 
                        "\"USER_ID\"=excluded.\"USER_ID\", " + 
                        "\"SOURCE_LOCATION_FILE_CONTAINER_ID\"=excluded.\"SOURCE_LOCATION_FILE_CONTAINER_ID\", " + 
                        "\"SOURCE_LOCATION_FILE_ID\"=excluded.\"SOURCE_LOCATION_FILE_ID\", " + 
                        "\"DOC\"=excluded.\"DOC\"";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcDataTransferQueueDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataDownloadDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void upsertUploadQueue(HpcDataObjectUploadRequest uploadRequest) throws HpcException
    {
		try {
		     jdbcTemplate.update(UPSERT_UPLOAD_QUEUE_SQL,
		    		             uploadRequest.getPath(),
		    		             uploadRequest.getCallerObjectId(),
		    		             uploadRequest.getUserId(),
		    		             uploadRequest.getSourceLocation().getFileContainerId(),
		    		             uploadRequest.getSourceLocation().getFileId(),
		    		             uploadRequest.getDoc());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert into data transfer upload queue: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
    }
}

 