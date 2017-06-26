/**
 * HpcDataObjectDeletionDAOImpl.java
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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import gov.nih.nci.hpc.dao.HpcDataObjectDeletionDAO;
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

public class HpcDataObjectDeletionDAOImpl implements HpcDataObjectDeletionDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	public static final String INSERT_SQL = 
		   "insert into public.\"HPC_DATA_OBJECT_DELETION_HISTORY\" ( " +
                    "\"USER_ID\", \"PATH\", \"METADATA\", \"ARCHIVE_FILE_CONTAINER_ID\"," +
                    "\"ARCHIVE_FILE_ID\", \"ARCHIVE_DELETE_STATUS\", \"DATA_MANAGEMENT_DELETE_STATUS\"," +
                    "\"DELETED\", \"MESSAGE\") values (?, ?, ?, ?, ?, ?, ?, ?, ?)"; 
	
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
    private HpcDataObjectDeletionDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataObjectDeletionDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void insert(String userId, String path, HpcMetadataEntries metadataEntries,
	                   HpcFileLocation archiveLocation, boolean archiveDeleteStatus,
    		           boolean dataManagementDeleteStatus, Calendar deleted, String message) 
			          throws HpcException
    {
		try {
		     jdbcTemplate.update(INSERT_SQL, userId, path, 
		    		             toJSONString(metadataEntries), 
		    		             archiveLocation.getFileContainerId(), 
		    		             archiveLocation.getFileId(), archiveDeleteStatus,
		    		             dataManagementDeleteStatus, deleted, message);
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to insert a data object deletion: " + e.getMessage(),
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

 