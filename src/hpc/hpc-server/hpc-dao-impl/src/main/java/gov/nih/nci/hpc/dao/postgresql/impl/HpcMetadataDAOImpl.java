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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
	// Metadata Query Operators
	private static final String EQUAL_OPERATOR = "EQUAL"; 
	private static final String LIKE_OPERATOR = "LIKE"; 
			
    // SQL Queries.
	private static final String GET_COLLECTION_IDS_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main_ovrd\" collection " +
	        "where collection.meta_attr_name = ? and collection.meta_attr_value = ?";
	
	private static final String GET_COLLECTION_IDS_LIKE_SQL = 
			"select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main_ovrd\" collection " +
		    "where collection.meta_attr_name = ? and collection.meta_attr_value like ?";
	
	private static final String GET_DATA_OBJECT_IDS_EQUAL_SQL = 
			"select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main_ovrd\" dataObject " +
		    "where dataObject.meta_attr_name = ? and dataObject.meta_attr_value = ?";
	
	private static final String GET_DATA_OBJECT_IDS_LIKE_SQL = 
			"select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main_ovrd\" dataObject " +
		    "where dataObject.meta_attr_name = ? and dataObject.meta_attr_value like ?";
		   

    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Row mappers.
	private HpcObjectIdRowMapper objectIdRowMapper = new HpcObjectIdRowMapper();
	
	// Maps between metadata query operator to its SQL query
	private Map<String, String> dataObjectSQLQueries = new HashMap<>();
	private Map<String, String> collectionSQLQueries = new HashMap<>();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcMetadataDAOImpl()
    {
    	dataObjectSQLQueries.put(EQUAL_OPERATOR, GET_DATA_OBJECT_IDS_EQUAL_SQL);
    	dataObjectSQLQueries.put(LIKE_OPERATOR, GET_DATA_OBJECT_IDS_LIKE_SQL);
    	
    	collectionSQLQueries.put(EQUAL_OPERATOR, GET_COLLECTION_IDS_EQUAL_SQL);
    	collectionSQLQueries.put(LIKE_OPERATOR, GET_COLLECTION_IDS_LIKE_SQL);
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
			 HpcPreparedQuery prepareQuery = prepareQuery(collectionSQLQueries, metadataQueries);
		     return jdbcTemplate.query(prepareQuery.sql, objectIdRowMapper, prepareQuery.args);
		     
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
			 HpcPreparedQuery prepareQuery = prepareQuery(dataObjectSQLQueries, metadataQueries);
		     return jdbcTemplate.query(prepareQuery.sql, objectIdRowMapper, prepareQuery.args);
		     
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
	
	// Prepared query.
	private class HpcPreparedQuery
	{
		public String sql = null;
		public Object[] args = null;
	}
	
    /**
     * Prepare a SQL query. Map operators to SQL and concatenate them with 'intersect'.
     *
     * @param sqlQueries The map from metadata query operator to SQL queries.
     * @param metadataQueries The metadata queries
     * 
     * @throws HpcException
     */
    private HpcPreparedQuery prepareQuery(Map<String, String> sqlQueries, 
    		                              List<HpcMetadataQuery> metadataQueries) 
    		                             throws HpcException
    {
    	StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<String> args = new ArrayList<>();
    	
    	for(HpcMetadataQuery metadataQuery : metadataQueries) {
    		String sqlQuery = sqlQueries.get(metadataQuery.getOperator());
    		if(sqlQuery == null) {
    		   throw new HpcException("Invalid metadata query operator: " + metadataQuery.getOperator(),
    				                  HpcErrorType.INVALID_REQUEST_INPUT);
    		}
    		
    		if(!args.isEmpty()) {
    			sqlQueryBuilder.append(" INTERSECT ");
    		}
    		sqlQueryBuilder.append(sqlQuery);
    		
    		args.add(metadataQuery.getAttribute());
    		args.add(metadataQuery.getValue());
    	}
    	
    	HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
    	preparedQuery.sql = sqlQueryBuilder.toString();
    	preparedQuery.args = args.toArray();
    	return preparedQuery;
    }
}

 