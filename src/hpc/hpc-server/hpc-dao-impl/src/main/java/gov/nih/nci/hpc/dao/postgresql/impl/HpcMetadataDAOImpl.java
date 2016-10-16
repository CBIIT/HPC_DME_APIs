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
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcHierarchicalMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.exception.HpcException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;

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
	private static final String NOT_EQUAL_OPERATOR = "NOT_EQUAL"; 
	private static final String LIKE_OPERATOR = "LIKE"; 
	private static final String NUM_LESS_THAN_OPERATOR = "NUM_LESS_THAN"; 
	private static final String NUM_LESS_OR_EQUAL_OPERATOR = "NUM_LESS_OR_EQUAL"; 
	private static final String NUM_GREATER_OR_EQUAL_OPERATOR = "NUM_GREATER_OR_EQUAL"; 
			
    // SQL Queries.
	private static final String GET_COLLECTION_IDS_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main_ovrd\" collection " +
	        "where collection.meta_attr_name = ? and collection.meta_attr_value = ?";
	
	private static final String GET_COLLECTION_IDS_NOT_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main_ovrd\" collection " +
	        "where collection.meta_attr_name = ? and collection.meta_attr_value <> ?";
	
	private static final String GET_COLLECTION_IDS_LIKE_SQL = 
			"select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main_ovrd\" collection " +
		    "where collection.meta_attr_name = ? and collection.meta_attr_value like ?";
	
	private static final String GET_COLLECTION_IDS_NUM_LESS_THAN_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main_ovrd\" collection " +
	        "where collection.meta_attr_name = ? and num_less_than(collection.meta_attr_value, ?) = true";
	
	private static final String GET_COLLECTION_IDS_NUM_LESS_OR_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main_ovrd\" collection " +
	        "where collection.meta_attr_name = ? and num_less_or_equal(collection.meta_attr_value, ?) = true";
	
	private static final String GET_COLLECTION_IDS_NUM_GREATER_OR_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main_ovrd\" collection " +
	        "where collection.meta_attr_name = ? and num_greater_or_equal(collection.meta_attr_value, ?) = true";
	
	private static final String GET_DATA_OBJECT_IDS_EQUAL_SQL = 
			"select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main_ovrd\" dataObject " +
		    "where dataObject.meta_attr_name = ? and dataObject.meta_attr_value = ?";
	
	private static final String GET_DATA_OBJECT_IDS_NOT_EQUAL_SQL = 
			"select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main_ovrd\" dataObject " +
		    "where dataObject.meta_attr_name = ? and dataObject.meta_attr_value <> ?";
	
	private static final String GET_DATA_OBJECT_IDS_LIKE_SQL = 
			"select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main_ovrd\" dataObject " +
		    "where dataObject.meta_attr_name = ? and dataObject.meta_attr_value like ?";
	
	private static final String GET_DATA_OBJECT_IDS_NUM_LESS_THAN_SQL = 
		    "select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main_ovrd\" dataObject " +
	        "where dataObject.meta_attr_name = ? and num_less_than(dataObject.meta_attr_value, ?) = true";
	
	private static final String GET_DATA_OBJECT_IDS_NUM_LESS_OR_EQUAL_SQL = 
		    "select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main_ovrd\" dataObject " +
	        "where dataObject.meta_attr_name = ? and num_less_or_equal(dataObject.meta_attr_value, ?) = true";
	
	private static final String GET_DATA_OBJECT_IDS_NUM_GREATER_OR_EQUAL_SQL = 
		    "select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main_ovrd\" dataObject " +
	        "where dataObject.meta_attr_name = ? and num_greater_or_equal(dataObject.meta_attr_value, ?) = true";
		   
	private static final String COLLECTION_USER_ACCESS_SQL = 
			"select access.object_id from public.\"r_objt_access\" access, " +
			"public.\"r_user_main\" account where account.user_name = ? and access.user_id = account.user_id";
	
	private static final String DATA_OBJECT_USER_ACCESS_SQL = 
			"select access.object_id from public.\"r_objt_access\" access, " +
			"public.\"r_user_main\" account where account.user_name = ? and access.user_id = account.user_id";
	
	private static final String GET_COLLECTION_PATHS_SQL = 
			"select distinct object_path from public.\"r_coll_hierarchy_meta_main\" where object_id in ";
	
	private static final String GET_DATA_OBJECT_PATHS_SQL = 
			"select distinct object_path from public.\"r_data_hierarchy_meta_main\" where object_id in ";
	
	private static final String GET_COLLECTION_METADATA_SQL = 
			"select meta_attr_name,  meta_attr_value, level " + 
	        "from public.\"r_coll_hierarchy_meta_main\" where object_path = ? ";
	
	private static final String GET_DATA_OBJECT_METADATA_SQL = 
			"select meta_attr_name, meta_attr_value, level " + 
	        "from public.\"r_data_hierarchy_meta_main\" where object_path = ? ";
	
	private static final String REFRESH_VIEW_SQL = "refresh materialized view concurrently";
			 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Row mappers.
	private SingleColumnRowMapper<String> objectPathRowMapper = new SingleColumnRowMapper<>();
	HpcHierarchicalMetadataEntryRowMapper hierarchicalMetadataEntryRowMapper = 
			                              new HpcHierarchicalMetadataEntryRowMapper();
	
	// Maps between metadata query operator to its SQL query
	private Map<String, String> dataObjectSQLQueries = new HashMap<>();
	private Map<String, String> collectionSQLQueries = new HashMap<>();
	
    // The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
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
    	dataObjectSQLQueries.put(NOT_EQUAL_OPERATOR, GET_DATA_OBJECT_IDS_NOT_EQUAL_SQL);
    	dataObjectSQLQueries.put(LIKE_OPERATOR, GET_DATA_OBJECT_IDS_LIKE_SQL);
    	dataObjectSQLQueries.put(NUM_LESS_THAN_OPERATOR, GET_DATA_OBJECT_IDS_NUM_LESS_THAN_SQL);
    	dataObjectSQLQueries.put(NUM_LESS_OR_EQUAL_OPERATOR, GET_DATA_OBJECT_IDS_NUM_LESS_OR_EQUAL_SQL);
    	dataObjectSQLQueries.put(NUM_GREATER_OR_EQUAL_OPERATOR, GET_DATA_OBJECT_IDS_NUM_GREATER_OR_EQUAL_SQL);
    	
    	collectionSQLQueries.put(EQUAL_OPERATOR, GET_COLLECTION_IDS_EQUAL_SQL);
    	collectionSQLQueries.put(NOT_EQUAL_OPERATOR, GET_COLLECTION_IDS_NOT_EQUAL_SQL);
    	collectionSQLQueries.put(LIKE_OPERATOR, GET_COLLECTION_IDS_LIKE_SQL);
    	collectionSQLQueries.put(NUM_LESS_THAN_OPERATOR, GET_COLLECTION_IDS_NUM_LESS_THAN_SQL);
    	collectionSQLQueries.put(NUM_LESS_OR_EQUAL_OPERATOR, GET_COLLECTION_IDS_NUM_LESS_OR_EQUAL_SQL);
    	collectionSQLQueries.put(NUM_GREATER_OR_EQUAL_OPERATOR, GET_COLLECTION_IDS_NUM_GREATER_OR_EQUAL_SQL);
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcMetadataDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
    public List<String> getCollectionPaths(List<HpcMetadataQuery> metadataQueries,
    		                               String dataManagementUsername) 
                                          throws HpcException
    {
		HpcPreparedQuery q = toQuery(collectionSQLQueries, metadataQueries, 
       		                         HpcCompoundMetadataQueryOperator.ALL);
		logger.error("ERAN SQL:" + q.sql);
		logger.error("ERAN ARGS: " + Arrays.asList(q.args));
		
		return getPaths(prepareQuery(GET_COLLECTION_PATHS_SQL, 
                                     toQuery(collectionSQLQueries, metadataQueries, 
                                    		 HpcCompoundMetadataQueryOperator.ALL),
                                     COLLECTION_USER_ACCESS_SQL, 
                                     dataManagementUsername));
    }
	
	@Override
    public List<String> getCollectionPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                               String dataManagementUsername) 
                                          throws HpcException
    {
		HpcPreparedQuery q = toQuery(collectionSQLQueries, compoundMetadataQuery);
		logger.error("ERAN SQL:" + q.sql);
		logger.error("ERAN ARGS: " + Arrays.asList(q.args));

		return getPaths(prepareQuery(GET_COLLECTION_PATHS_SQL, 
                                     toQuery(collectionSQLQueries, compoundMetadataQuery),
                                     COLLECTION_USER_ACCESS_SQL, 
                                     dataManagementUsername));
    }

	@Override 
	public List<String> getDataObjectPaths(List<HpcMetadataQuery> metadataQueries,
			                               String dataManagementUsername) 
                                          throws HpcException
    {
		return getPaths(prepareQuery(GET_DATA_OBJECT_PATHS_SQL, 
                                     toQuery(dataObjectSQLQueries, metadataQueries,
                                    		 HpcCompoundMetadataQueryOperator.ALL),
                                     DATA_OBJECT_USER_ACCESS_SQL, 
                                     dataManagementUsername));
    }
	
	@Override 
	public List<String> getDataObjectPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
			                               String dataManagementUsername) 
                                          throws HpcException
    {
		return getPaths(prepareQuery(GET_DATA_OBJECT_PATHS_SQL, 
                                     toQuery(dataObjectSQLQueries, compoundMetadataQuery),
                                     DATA_OBJECT_USER_ACCESS_SQL, 
                                     dataManagementUsername));
    }
	
    @Override
    public List<HpcHierarchicalMetadataEntry> getCollectionMetadata(String path) throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_COLLECTION_METADATA_SQL, hierarchicalMetadataEntryRowMapper, path);
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get collection hierarchical metadata: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}	
    }
    
    @Override
    public List<HpcHierarchicalMetadataEntry> getDataObjectMetadata(String path) throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_DATA_OBJECT_METADATA_SQL, hierarchicalMetadataEntryRowMapper, path);
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get data object hierarchical metadata: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}	
    }
    
	@Override
	public void refreshViews() throws HpcException
    {
		try {
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_coll_hierarchy_metamap");
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_coll_hierarchy_meta_main_ovrd");
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_coll_hierarchy_meta_main");
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_data_hierarchy_metamap");
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_data_hierarchy_meta_main_ovrd");
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_data_hierarchy_meta_main");
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to refresh materialized views: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, e);
		}
    }
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
	// Row Mapper
	private class HpcHierarchicalMetadataEntryRowMapper implements RowMapper<HpcHierarchicalMetadataEntry>
	{
		@Override
		public HpcHierarchicalMetadataEntry mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			HpcHierarchicalMetadataEntry hierarchicalMetadataEntry = new HpcHierarchicalMetadataEntry();
			Long level = rs.getLong("LEVEL");
			hierarchicalMetadataEntry.setLevel(level != null ? level.intValue() : null);
			HpcMetadataEntry metadataEntry = new HpcMetadataEntry();
			metadataEntry.setAttribute(rs.getString("META_ATTR_NAME"));
			metadataEntry.setValue(rs.getString("META_ATTR_VALUE"));
			hierarchicalMetadataEntry.setMetadataEntry(metadataEntry);
			
			return hierarchicalMetadataEntry;
		}
	}
	
	// Prepared query.
	private class HpcPreparedQuery
	{
		public String sql = null;
		public String[] args = null;
	}
	
    /**
     * Prepare a SQL query. Map operators to SQL and concatenate them with 'intersect'.
     *
     * @param getObjectPathsQuery The query to get object paths based on object IDs.
     * @param userQuery The calculated SQL query based on user input (represented by query domain objects).
     * @param userAccessQuery The user access query to append.
     * @param dataManagementUsername The data management user name.
     * 
     * @throws HpcException
     */
    private HpcPreparedQuery prepareQuery(String getObjectPathsQuery,
    		                              HpcPreparedQuery userQuery,
    		                              String userAccessQuery,
    		                              String dataManagementUsername) 
    		                             throws HpcException
    {
    	StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<String> args = new ArrayList<>();
    	
    	// Combine the metadata queries into a single SQL statement.
    	sqlQueryBuilder.append(getObjectPathsQuery + "(");
    	sqlQueryBuilder.append(userQuery.sql);
    	args.addAll(Arrays.asList(userQuery.args));
    	
    	// Add a query to only include entities the user can access.
    	sqlQueryBuilder.append(" intersect ");
    	sqlQueryBuilder.append(userAccessQuery + ")");
    	args.add(dataManagementUsername);
    	
    	HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
    	preparedQuery.sql = sqlQueryBuilder.toString();
    	preparedQuery.args = args.toArray(new String[0]);
    	return preparedQuery;
    }
    
    /**
     * Create a SQL statement from List<HpcMetadataQuery>
     *
     * @param sqlQueries The map from metadata query operator to SQL queries.
     * @param metadataQueries The metadata queries.
     * @param operator The operator to use.
     * 
     * @throws HpcException
     */
    private HpcPreparedQuery toQuery(Map<String, String> sqlQueries, 
    		                         List<HpcMetadataQuery> metadataQueries,
    		                         HpcCompoundMetadataQueryOperator operator) 
    		                        throws HpcException
    {
    	StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<String> args = new ArrayList<>();
    
		sqlQueryBuilder.append("(");
		for(HpcMetadataQuery metadataQuery : metadataQueries) {
			String sqlQuery = sqlQueries.get(metadataQuery.getOperator());
			if(sqlQuery == null) {
			   throw new HpcException("Invalid metadata query operator: " + metadataQuery.getOperator(),
					                  HpcErrorType.INVALID_REQUEST_INPUT);
			}
			
			if(!args.isEmpty()) {
			   sqlQueryBuilder.append(" " + toSQLOperator(operator) + " ");
			}
			sqlQueryBuilder.append(sqlQuery);
			
			args.add(metadataQuery.getAttribute());
			args.add(metadataQuery.getValue());
		}
		sqlQueryBuilder.append(")");
		
    	HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
    	preparedQuery.sql = sqlQueryBuilder.toString();
    	preparedQuery.args = args.toArray(new String[0]);
    	return preparedQuery;
    }
    
    /**
     * Create a SQL statement from HpcCompoundMetadataQuery
     *
     * @param sqlQueries The map from metadata query operator to SQL queries.
     * @param metadataQueries The metadata queries.
     * 
     * @throws HpcException
     */
    private HpcPreparedQuery toQuery(Map<String, String> sqlQueries, 
    		                         HpcCompoundMetadataQuery compoundMetadataQuery) 
    		                        throws HpcException
    {
    	StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<String> args = new ArrayList<>();
    
		sqlQueryBuilder.append("(");
		// Append the simple queries.
		if(compoundMetadataQuery.getQueries() != null && !compoundMetadataQuery.getQueries().isEmpty()) {
			HpcPreparedQuery query = toQuery(sqlQueries, compoundMetadataQuery.getQueries(), 
					                         compoundMetadataQuery.getOperator());	
			sqlQueryBuilder.append(query.sql);
			args.addAll(Arrays.asList(query.args));
		}
		
		// Append the nested compound queries.
		if(compoundMetadataQuery.getCompoundQueries() != null && 
		   !compoundMetadataQuery.getCompoundQueries().isEmpty()){
		   if(!args.isEmpty()) {
			  sqlQueryBuilder.append(" " + toSQLOperator(compoundMetadataQuery.getOperator()) + " ");
		   }
		   boolean firstNestedQuery = true;
		   for(HpcCompoundMetadataQuery nestedCompoundQuery : compoundMetadataQuery.getCompoundQueries()) {
			   if(!firstNestedQuery) {
				  sqlQueryBuilder.append(" " + toSQLOperator(compoundMetadataQuery.getOperator()) + " ");
			   } else {
				       firstNestedQuery = false;
			   }
			   HpcPreparedQuery query = toQuery(sqlQueries,  nestedCompoundQuery);	
               sqlQueryBuilder.append(query.sql);
               args.addAll(Arrays.asList(query.args));			   
		   }
		}
		sqlQueryBuilder.append(")");
		
    	HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
    	preparedQuery.sql = sqlQueryBuilder.toString();
    	preparedQuery.args = args.toArray(new String[0]);
    	return preparedQuery;
    }
    
    /**
     * Execute a SQL query to get collection or data object paths
     *
     * @param prepareQuery The prepared query to execute.
     * 
     * @throws HpcException
     */
    private List<String> getPaths(HpcPreparedQuery prepareQuery) throws HpcException
    {
		try {
		     return jdbcTemplate.query(prepareQuery.sql, objectPathRowMapper, 
		    		                   (Object[]) prepareQuery.args);
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get collection/data-object Paths: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}		
    }
    
    /**
     * Coverts a query operator enum to SQL
     *
     * @param operator The operator to convert.
     * 
     * @throws HpcException
     */
    private String toSQLOperator(HpcCompoundMetadataQueryOperator operator)
    {
    	return operator.equals(HpcCompoundMetadataQueryOperator.ALL) ? "intersect" : "union";  
    }
}

 