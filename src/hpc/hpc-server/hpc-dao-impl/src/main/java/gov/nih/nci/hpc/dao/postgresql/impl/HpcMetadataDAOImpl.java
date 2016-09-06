/**
 * HpcMetadataDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.exception.HpcException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * <p>
 * HPC Metadata DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcMetadataDAOImpl implements HpcMetadataDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	public static final String GET_COLLECTION_IDS_SQL = 
		   "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main_ovrd\" collection " +
	       "where collection.meta_attr_name = ? and collection.meta_attr_value ? ?";
	
	public static final String GET_DATA_OBJECT_IDS_SQL = 
			   "select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main_ovrd\" dataObject " +
		       "where dataObject.meta_attr_name = ? and dataObject.meta_attr_value ? ?";
		   

    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Row mappers.
	private HpcObjectIdRowMapper objectIdRowMapper = new HpcObjectIdRowMapper();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcMetadataDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcMetadataDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
    public List<Integer> getCollectionIds(List<HpcMetadataQuery> metadataQueries) 
                                         throws HpcException
    {
		try {
			 Iterator<HpcMetadataQuery> metadataQueriesIter = metadataQueries.iterator();
			 HpcMetadataQuery query = metadataQueriesIter.next();
		     return jdbcTemplate.query(GET_COLLECTION_IDS_SQL, objectIdRowMapper,
		    		                   query.getAttribute(), query.getOperator(), query.getValue());
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get collection IDs: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}		
    }

	@Override 
	public List<Integer> getDataObjectIds(List<HpcMetadataQuery> metadataQueries) 
                                         throws HpcException
    {
		try {
			 Iterator<HpcMetadataQuery> metadataQueriesIter = metadataQueries.iterator();
			 HpcMetadataQuery query = metadataQueriesIter.next();
		     return jdbcTemplate.query(GET_DATA_OBJECT_IDS_SQL, objectIdRowMapper,
		    		                   query.getAttribute(), query.getOperator(), query.getValue());
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get data object IDs: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}		
    }
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
	// Map Object ID (from Long to Integer)
	private class HpcObjectIdRowMapper implements RowMapper<Integer>
	{
		@Override
		public Integer mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			Long objectId = rs.getLong("OBJECT_ID");
			return objectId != null ? objectId.intValue() : null;
		}
	}
}

 