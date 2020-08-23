/**
 * HpcDataManagementAuditDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import java.util.Calendar;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import gov.nih.nci.hpc.dao.HpcDataManagementAuditDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcAuditRequestType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Object Deletion DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcDataManagementAuditDAOImpl implements HpcDataManagementAuditDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	public static final String INSERT_SQL = 
		   "insert into public.\"HPC_DATA_MANAGEMENT_AUDIT\" ( " +
                    "\"USER_ID\", \"PATH\", \"REQUEST_TYPE\", \"METADATA_BEFORE\", " +
				    "\"METADATA_AFTER\", \"ARCHIVE_FILE_CONTAINER_ID\"," +
                    "\"ARCHIVE_FILE_ID\", \"DATA_MANAGEMENT_STATUS\", \"DATA_TRANSFER_STATUS\"," +
                    "\"MESSAGE\", \"COMPLETED\") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; 
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	@Qualifier("hpcPostgreSQLJdbcTemplate")
	private JdbcTemplate jdbcTemplate = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcDataManagementAuditDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataObjectDeletionDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void insert(String userId, String path, HpcAuditRequestType requestType,
	                   HpcMetadataEntries metadataBefore, HpcMetadataEntries metadataAfter,
	                   HpcFileLocation archiveLocation, boolean dataManagementStatus,
	                   Boolean dataTransferStatus, String message, Calendar completed) 
			          throws HpcException
    {
		String fileContainerId = null;
		String fileId = null;
		if(archiveLocation != null) {
		   fileContainerId = archiveLocation.getFileContainerId();
		   fileId = archiveLocation.getFileId();
		}
		
		try {
		     jdbcTemplate.update(INSERT_SQL, userId, path, requestType.value(),
		    		             toJSONString(metadataBefore), toJSONString(metadataAfter), 
		    		             fileContainerId, fileId, dataManagementStatus,
		    		             dataTransferStatus, message, completed);
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to insert a data management audit record: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
    }
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
	/** 
     * Convert metadata list into a JSON string.
     * 
     * @param metadataEntries A list of metadata entries.
     * @return A JSON representation of the metadata.
     */
	@SuppressWarnings("unchecked")
	private String toJSONString(HpcMetadataEntries metadataEntries)
	{
		if(metadataEntries == null) {
		   return null;
		}
		
		JSONObject jsonMetadata = new JSONObject();
		
		// Map the self metadata entries.
		JSONArray jsonSelfMetadataEntries = new JSONArray();
		for(HpcMetadataEntry metadataEntry : metadataEntries.getSelfMetadataEntries()) {
			jsonSelfMetadataEntries.add(toJSON(metadataEntry));
		}
		jsonMetadata.put("selfMetadataEntries", jsonSelfMetadataEntries);
		
		// Map the parent metadata entries.
		JSONArray jsonParentMetadataEntries = new JSONArray();
		for(HpcMetadataEntry metadataEntry : metadataEntries.getParentMetadataEntries()) {
			jsonParentMetadataEntries.add(toJSON(metadataEntry));
		}
		jsonMetadata.put("parentMetadataEntries", jsonParentMetadataEntries);
		
		return jsonMetadata.toJSONString();
	}
	
	/** 
     * Convert metadata entry into a JSON object.
     * 
     * @param metadataEntry A metadata entry domain object.
     * @return A JSON object represenation of the domain object.
     */
	@SuppressWarnings("unchecked")
	private JSONObject toJSON(HpcMetadataEntry metadataEntry)
	{
		JSONObject jsonMetadataEntry = new JSONObject();
		jsonMetadataEntry.put("attribute", metadataEntry.getAttribute());
		jsonMetadataEntry.put("value", metadataEntry.getValue());
		if(metadataEntry.getUnit() != null) {
		   jsonMetadataEntry.put("unit", metadataEntry.getUnit());
		}
		if(metadataEntry.getLevel() != null) {
		   jsonMetadataEntry.put("level", metadataEntry.getLevel());
		}
		if(metadataEntry.getLevelLabel() != null) {
		   jsonMetadataEntry.put("levelLabel", metadataEntry.getLevelLabel());
		}
		
		return jsonMetadataEntry;
	}
}

 