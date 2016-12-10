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
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataLevelAttributes;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryLevelFilter;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
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
    
    // SQL Queries.
	private static final String GET_COLLECTION_IDS_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where collection.meta_attr_name = ? and collection.meta_attr_value = ?";
	
	private static final String GET_COLLECTION_IDS_NOT_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where collection.meta_attr_name = ? and collection.meta_attr_value <> ?";
	
	private static final String GET_COLLECTION_IDS_LIKE_SQL = 
			"select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
		    "where collection.meta_attr_name = ? and collection.meta_attr_value like ?";
	
	private static final String GET_COLLECTION_IDS_NUM_LESS_THAN_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where collection.meta_attr_name = ? and num_less_than(collection.meta_attr_value, ?) = true";
	
	private static final String GET_COLLECTION_IDS_NUM_LESS_OR_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where collection.meta_attr_name = ? and num_less_or_equal(collection.meta_attr_value, ?) = true";
	
	private static final String GET_COLLECTION_IDS_NUM_GREATER_THAN_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where collection.meta_attr_name = ? and num_greater_than(collection.meta_attr_value, ?) = true";
	
	private static final String GET_COLLECTION_IDS_NUM_GREATER_OR_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where collection.meta_attr_name = ? and num_greater_or_equal(collection.meta_attr_value, ?) = true";
	
	private static final String GET_DATA_OBJECT_IDS_EQUAL_SQL = 
			"select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
		    "where dataObject.meta_attr_name = ? and dataObject.meta_attr_value = ?";
	
	private static final String GET_DATA_OBJECT_IDS_NOT_EQUAL_SQL = 
			"select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
		    "where dataObject.meta_attr_name = ? and dataObject.meta_attr_value <> ?";
	
	private static final String GET_DATA_OBJECT_IDS_LIKE_SQL = 
			"select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
		    "where dataObject.meta_attr_name = ? and dataObject.meta_attr_value like ?";
	
	private static final String GET_DATA_OBJECT_IDS_NUM_LESS_THAN_SQL = 
		    "select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
	        "where dataObject.meta_attr_name = ? and num_less_than(dataObject.meta_attr_value, ?) = true";
	
	private static final String GET_DATA_OBJECT_IDS_NUM_LESS_OR_EQUAL_SQL = 
		    "select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
	        "where dataObject.meta_attr_name = ? and num_less_or_equal(dataObject.meta_attr_value, ?) = true";
	
	private static final String GET_DATA_OBJECT_IDS_NUM_GREATER_THAN_SQL = 
		    "select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
	        "where dataObject.meta_attr_name = ? and num_greater_than(dataObject.meta_attr_value, ?) = true";
	
	private static final String GET_DATA_OBJECT_IDS_NUM_GREATER_OR_EQUAL_SQL = 
		    "select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
	        "where dataObject.meta_attr_name = ? and num_greater_or_equal(dataObject.meta_attr_value, ?) = true";
	
	private static final String DATA_OBJECT_LEVEL_EQUAL_FILTER = " and dataObject.level = ?";
	private static final String DATA_OBJECT_LEVEL_NOT_EQUAL_FILTER = " and dataObject.level <> ?";
	private static final String DATA_OBJECT_LEVEL_NUM_LESS_THAN_FILTER = " and dataObject.level < ?";
	private static final String DATA_OBJECT_LEVEL_NUM_LESS_OR_EQUAL_FILTER = " and dataObject.level <= ?";
	private static final String DATA_OBJECT_LEVEL_NUM_GREATER_THAN_FILTER = " and dataObject.level > ?";
	private static final String DATA_OBJECT_LEVEL_NUM_GREATER_OR_EQUAL_FILTER = " and dataObject.level >= ?";
	
	private static final String COLLECTION_LEVEL_EQUAL_FILTER = " and collection.level = ?";
	private static final String COLLECTION_LEVEL_NOT_EQUAL_FILTER = " and collection.level <> ?";
	private static final String COLLECTION_LEVEL_NUM_LESS_THAN_FILTER = " and collection.level < ?";
	private static final String COLLECTION_LEVEL_NUM_LESS_OR_EQUAL_FILTER = " and collection.level <= ?";
	private static final String COLLECTION_LEVEL_NUM_GREATER_THAN_FILTER = " and collection.level > ?";
	private static final String COLLECTION_LEVEL_NUM_GREATER_OR_EQUAL_FILTER = " and collection.level >= ?";
		   
	private static final String COLLECTION_USER_ACCESS_SQL = 
			"select access.object_id from public.\"r_objt_access\" access, " +
			"public.\"r_user_main\" account where account.user_name = ? and access.user_id = account.user_id";
	
	private static final String DATA_OBJECT_USER_ACCESS_SQL = 
			"select access.object_id from public.\"r_objt_access\" access, " +
			"public.\"r_user_main\" account where account.user_name = ? and access.user_id = account.user_id";
	
	private static final String LIMIT_OFFSET_SQL = " order by object_path limit ? offset ?";
	
	private static final String GET_COLLECTION_PATHS_SQL = 
			"select distinct object_path from public.\"r_coll_hierarchy_meta_main\" where object_id in ";
	
	private static final String GET_DATA_OBJECT_PATHS_SQL = 
			"select distinct object_path from public.\"r_data_hierarchy_meta_main\" where object_id in ";
	
	private static final String GET_COLLECTION_METADATA_SQL = 
			"select meta_attr_name,  meta_attr_value, level " + 
	        "from public.\"r_coll_hierarchy_meta_main\" where object_path = ? and level >= ? ";
	
	private static final String GET_DATA_OBJECT_METADATA_SQL = 
			"select meta_attr_name, meta_attr_value, level " + 
	        "from public.\"r_data_hierarchy_meta_main\" where object_path = ? and level >= ? ";
	
	private static final String REFRESH_VIEW_SQL = "refresh materialized view concurrently";
	
	private static final String GET_COLLECTION_METADATA_ATTRIBUTES_SQL = 
			"select level, array_agg(distinct meta_attr_name) as attributes from public.\"r_coll_hierarchy_meta_main\" " +
	        "where object_id in (" + COLLECTION_USER_ACCESS_SQL +") ";
	
	private static final String GET_DATA_OBJECT_METADATA_ATTRIBUTES_SQL = 
			"select level, array_agg(distinct meta_attr_name) as attributes from public.\"r_data_hierarchy_meta_main\" " +
			"where object_id in (" +  DATA_OBJECT_USER_ACCESS_SQL +") ";
	
	private static final String GET_METADATA_ATTRIBUTES_GROUP_ORDER_BY_SQL = 
			" group by level order by level";
	
    // The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Row mappers.
	private SingleColumnRowMapper<String> objectPathRowMapper = new SingleColumnRowMapper<>();
	private HpcMetadataLevelAttributesRowMapper metadataLevelAttributeRowMapper = new HpcMetadataLevelAttributesRowMapper();
	HpcMetadataEntryRowMapper metadataEntryRowMapper = new HpcMetadataEntryRowMapper();
	
	// Maps between metadata query operator to its SQL query.
	private Map<HpcMetadataQueryOperator, String> dataObjectSQLQueries = new HashMap<>();
	private Map<HpcMetadataQueryOperator, String> collectionSQLQueries = new HashMap<>();
	
	// Maps between metadata query operator to a level SQL filter ('where' condition)
	private Map<HpcMetadataQueryOperator, String> dataObjectSQLLevelFilters = new HashMap<>();
	private Map<HpcMetadataQueryOperator, String> collectionSQLLevelFilters = new HashMap<>();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcMetadataDAOImpl()
    {
    	dataObjectSQLQueries.put(HpcMetadataQueryOperator.EQUAL, 
    			                 GET_DATA_OBJECT_IDS_EQUAL_SQL);
    	dataObjectSQLQueries.put(HpcMetadataQueryOperator.NOT_EQUAL, 
    			                 GET_DATA_OBJECT_IDS_NOT_EQUAL_SQL);
    	dataObjectSQLQueries.put(HpcMetadataQueryOperator.LIKE, 
    			                 GET_DATA_OBJECT_IDS_LIKE_SQL);
    	dataObjectSQLQueries.put(HpcMetadataQueryOperator.NUM_LESS_THAN, 
    			                 GET_DATA_OBJECT_IDS_NUM_LESS_THAN_SQL);
    	dataObjectSQLQueries.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL, 
    			                 GET_DATA_OBJECT_IDS_NUM_LESS_OR_EQUAL_SQL);
    	dataObjectSQLQueries.put(HpcMetadataQueryOperator.NUM_GREATER_THAN, 
                                 GET_DATA_OBJECT_IDS_NUM_GREATER_THAN_SQL);
    	dataObjectSQLQueries.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL, 
    			                 GET_DATA_OBJECT_IDS_NUM_GREATER_OR_EQUAL_SQL);
    	
    	collectionSQLQueries.put(HpcMetadataQueryOperator.EQUAL, 
    			                 GET_COLLECTION_IDS_EQUAL_SQL);
    	collectionSQLQueries.put(HpcMetadataQueryOperator.NOT_EQUAL, 
    			                 GET_COLLECTION_IDS_NOT_EQUAL_SQL);
    	collectionSQLQueries.put(HpcMetadataQueryOperator.LIKE, 
    			                 GET_COLLECTION_IDS_LIKE_SQL);
    	collectionSQLQueries.put(HpcMetadataQueryOperator.NUM_LESS_THAN, 
    			                 GET_COLLECTION_IDS_NUM_LESS_THAN_SQL);
    	collectionSQLQueries.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL, 
    			                 GET_COLLECTION_IDS_NUM_LESS_OR_EQUAL_SQL);
    	collectionSQLQueries.put(HpcMetadataQueryOperator.NUM_GREATER_THAN, 
                                 GET_COLLECTION_IDS_NUM_GREATER_THAN_SQL);
    	collectionSQLQueries.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL, 
    			                 GET_COLLECTION_IDS_NUM_GREATER_OR_EQUAL_SQL);
    	
    	dataObjectSQLLevelFilters.put(HpcMetadataQueryOperator.EQUAL, 
    			                      DATA_OBJECT_LEVEL_EQUAL_FILTER);
    	dataObjectSQLLevelFilters.put(HpcMetadataQueryOperator.NOT_EQUAL, 
                                      DATA_OBJECT_LEVEL_NOT_EQUAL_FILTER);
    	dataObjectSQLLevelFilters.put(HpcMetadataQueryOperator.NUM_LESS_THAN, 
                                      DATA_OBJECT_LEVEL_NUM_LESS_THAN_FILTER);
    	dataObjectSQLLevelFilters.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL, 
                                      DATA_OBJECT_LEVEL_NUM_LESS_OR_EQUAL_FILTER);
    	dataObjectSQLLevelFilters.put(HpcMetadataQueryOperator.NUM_GREATER_THAN, 
                                      DATA_OBJECT_LEVEL_NUM_GREATER_THAN_FILTER);
    	dataObjectSQLLevelFilters.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL, 
                                      DATA_OBJECT_LEVEL_NUM_GREATER_OR_EQUAL_FILTER);
    	
    	collectionSQLLevelFilters.put(HpcMetadataQueryOperator.EQUAL, 
    			                      COLLECTION_LEVEL_EQUAL_FILTER);
    	collectionSQLLevelFilters.put(HpcMetadataQueryOperator.NOT_EQUAL, 
                                      COLLECTION_LEVEL_NOT_EQUAL_FILTER);
    	collectionSQLLevelFilters.put(HpcMetadataQueryOperator.NUM_LESS_THAN, 
                                      COLLECTION_LEVEL_NUM_LESS_THAN_FILTER);
    	collectionSQLLevelFilters.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL, 
                                      COLLECTION_LEVEL_NUM_LESS_OR_EQUAL_FILTER);
    	collectionSQLLevelFilters.put(HpcMetadataQueryOperator.NUM_GREATER_THAN, 
                                      COLLECTION_LEVEL_NUM_GREATER_THAN_FILTER);
    	collectionSQLLevelFilters.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL, 
                                      COLLECTION_LEVEL_NUM_GREATER_OR_EQUAL_FILTER);
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcMetadataDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
    public List<String> getCollectionPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                               String dataManagementUsername,
    		                               int offset, int limit,
    		                               HpcMetadataQueryLevelFilter defaultLevelFilter) 
                                          throws HpcException
    {
		return getPaths(prepareQuery(GET_COLLECTION_PATHS_SQL, 
                                     toQuery(collectionSQLQueries, compoundMetadataQuery, 
                                    		 collectionSQLLevelFilters, defaultLevelFilter),
                                     COLLECTION_USER_ACCESS_SQL, 
                                     dataManagementUsername, offset, limit));
    }

	@Override 
	public List<String> getDataObjectPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
			                               String dataManagementUsername,
			                               int offset, int limit,
			                               HpcMetadataQueryLevelFilter defaultLevelFilter) 
                                          throws HpcException
    {
		return getPaths(prepareQuery(GET_DATA_OBJECT_PATHS_SQL, 
                                     toQuery(dataObjectSQLQueries, compoundMetadataQuery,
                                    		 dataObjectSQLLevelFilters, defaultLevelFilter),
                                     DATA_OBJECT_USER_ACCESS_SQL, 
                                     dataManagementUsername, offset, limit));
    }
	
    @Override
    public List<HpcMetadataEntry> getCollectionMetadata(String path, int minLevel) throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_COLLECTION_METADATA_SQL, metadataEntryRowMapper, 
		    		                   path, minLevel);
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get collection hierarchical metadata: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}	
    }
    
    @Override
    public List<HpcMetadataEntry> getDataObjectMetadata(String path, int minLevel) throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_DATA_OBJECT_METADATA_SQL, metadataEntryRowMapper, 
		    		                   path, minLevel);
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get data object hierarchical metadata: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}	
    }
    
    @Override
    public List<HpcMetadataLevelAttributes> getCollectionMetadataAttributes(
    		                                   Integer level, HpcMetadataQueryOperator levelOperator,
    		                                   String dataManagementUsername) 
    		                                   throws HpcException
    {
    	return getMetadataAttributes(GET_COLLECTION_METADATA_ATTRIBUTES_SQL, 
    			                     level, levelOperator, dataManagementUsername,
    			                     collectionSQLLevelFilters);
    }
    
    @Override
    public List<HpcMetadataLevelAttributes> getDataObjectMetadataAttributes(
    		                                   Integer level, HpcMetadataQueryOperator levelOperator,
    		                                   String dataManagementUsername) 
    		                                   throws HpcException
    {
    	return getMetadataAttributes(GET_DATA_OBJECT_METADATA_ATTRIBUTES_SQL, 
    			                     level, levelOperator, dataManagementUsername, 
    			                     dataObjectSQLLevelFilters);
    }
    
	@Override
	public void refreshViews() throws HpcException
    {
		try {
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_coll_hierarchy_metamap");
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_coll_hierarchy_meta_main");
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_data_hierarchy_metamap");
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
	private class HpcMetadataEntryRowMapper implements RowMapper<HpcMetadataEntry>
	{
		@Override
		public HpcMetadataEntry mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			HpcMetadataEntry metadataEntry = new HpcMetadataEntry();
			Long level = rs.getLong("LEVEL");
			metadataEntry.setLevel(level != null ? level.intValue() : null);
			metadataEntry.setAttribute(rs.getString("META_ATTR_NAME"));
			metadataEntry.setValue(rs.getString("META_ATTR_VALUE"));
			
			return metadataEntry;
		}
	}
	
	private class HpcMetadataLevelAttributesRowMapper implements RowMapper<HpcMetadataLevelAttributes>
	{
		@Override
		public HpcMetadataLevelAttributes mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			logger.error("ERAN: Mapper");
			HpcMetadataLevelAttributes metadataLevelAttributes = new HpcMetadataLevelAttributes();
			Long level = rs.getLong("LEVEL");
			metadataLevelAttributes.setLevel(level != null ? level.intValue() : null);
			metadataLevelAttributes.getMetadataAttributes().addAll(
				    Arrays.asList((String[]) rs.getArray("ATTRIBUTES").getArray()));
			return metadataLevelAttributes;
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
     * @param getObjectPathsQuery The query to get object paths based on object IDs.
     * @param userQuery The calculated SQL query based on user input (represented by query domain objects).
     * @param userAccessQuery The user access query to append.
     * @param dataManagementUsername The data management user name.
     * @param offset Skip that many path in the returned results.
     * @param limit No more than 'limit' paths will be returned.
     * @return A prepared query.
     */
    private HpcPreparedQuery prepareQuery(String getObjectPathsQuery,
    		                              HpcPreparedQuery userQuery,
    		                              String userAccessQuery,
    		                              String dataManagementUsername,
    		                              int offset, int limit) 
    {
    	StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<Object> args = new ArrayList<>();
    	
    	// Combine the metadata queries into a single SQL statement.
    	sqlQueryBuilder.append(getObjectPathsQuery + "(");
    	sqlQueryBuilder.append(userQuery.sql);
    	args.addAll(Arrays.asList(userQuery.args));
    	
    	// Add a query to only include entities the user can access.
    	sqlQueryBuilder.append(" intersect ");
    	sqlQueryBuilder.append(userAccessQuery + ")");
    	args.add(dataManagementUsername);
    	
    	sqlQueryBuilder.append(LIMIT_OFFSET_SQL);
    	args.add(limit);
    	args.add(offset);
    	
    	HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
    	preparedQuery.sql = sqlQueryBuilder.toString();
    	preparedQuery.args = args.toArray();
    	return preparedQuery;
    }
    
    /**
     * Create a SQL statement from List&lt;HpcMetadataQuery&gt;. 
     *
     * @param sqlQueries The map from metadata query operator to SQL queries.
     * @param metadataQueries The metadata queries.
     * @param operator The compound metadata query operator to use.
     * @param sqlLevelFilters The map from query operator to SQL level filter ('where' condition).
     * @param defaultLevelFilter A default level filter to use if not provided in the query.
     * @return A prepared query.
     * @throws HpcException If invalid metadata query operator provided.
     */
    private HpcPreparedQuery toQuery(Map<HpcMetadataQueryOperator, String> sqlQueries, 
    		                         List<HpcMetadataQuery> metadataQueries,
    		                         HpcCompoundMetadataQueryOperator operator,
    		                         Map<HpcMetadataQueryOperator, String> sqlLevelFilters,
    		                         HpcMetadataQueryLevelFilter defaultLevelFilter) 
    		                        throws HpcException
    {
    	StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<Object> args = new ArrayList<>();
    
		sqlQueryBuilder.append("(");
		for(HpcMetadataQuery metadataQuery : metadataQueries) {
			String sqlQuery = sqlQueries.get(metadataQuery.getOperator());
			if(sqlQuery == null) {
			   throw new HpcException("Invalid metadata query operator: " + metadataQuery.getOperator(),
					                  HpcErrorType.INVALID_REQUEST_INPUT);
			}
			
			// Append the compound metadata query operator if not the first query in the list.
			if(!args.isEmpty()) {
			   sqlQueryBuilder.append(" " + toSQLOperator(operator) + " ");
			}
			
			// Append the SQL query representing the requested metadata query operator and its arguments.
			sqlQueryBuilder.append(sqlQuery);
			args.add(metadataQuery.getAttribute());
			args.add(metadataQuery.getValue());
			
			// Add a filter for level. 
			HpcMetadataQueryLevelFilter levelFilter = metadataQuery.getLevelFilter() != null ?
					                                  metadataQuery.getLevelFilter() : defaultLevelFilter;
			if(levelFilter != null) {
			   String sqlLevelFilter = sqlLevelFilters.get(levelFilter.getOperator());
			   if(sqlLevelFilter == null) {
			      throw new HpcException("Invalid level operator: " + levelFilter.getOperator(),
			                             HpcErrorType.INVALID_REQUEST_INPUT); 
			   }
			   sqlQueryBuilder.append(sqlLevelFilter);
			   args.add(levelFilter.getLevel());
			}
		}
		
		sqlQueryBuilder.append(")");
		
    	HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
    	preparedQuery.sql = sqlQueryBuilder.toString();
    	preparedQuery.args = args.toArray();
    	return preparedQuery;
    }
    
    /**
     * Create a SQL statement from HpcCompoundMetadataQuery.
     *
     * @param sqlQueries The map from metadata query operator to SQL queries.
     * @param compoundMetadataQuery The compound query to create SQL from.
     * @param sqlLevelFilters The map from query operator to level filter ('where' condition).
     * @param defaultLevelFilter A default level filter to use if not provided in the query.
     * @return A prepared query.
     * @throws HpcException on service failure.
     */
    private HpcPreparedQuery toQuery(Map<HpcMetadataQueryOperator, String> sqlQueries, 
    		                         HpcCompoundMetadataQuery compoundMetadataQuery,
    		                         Map<HpcMetadataQueryOperator, String> sqlLevelFilters,
    		                         HpcMetadataQueryLevelFilter defaultLevelFilter) 
    		                        throws HpcException
    {
    	StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<Object> args = new ArrayList<>();
    
		sqlQueryBuilder.append("(");
		// Append the simple queries.
		if(compoundMetadataQuery.getQueries() != null && !compoundMetadataQuery.getQueries().isEmpty()) {
			HpcPreparedQuery query = toQuery(sqlQueries, compoundMetadataQuery.getQueries(), 
					                         compoundMetadataQuery.getOperator(), sqlLevelFilters, 
					                         defaultLevelFilter);	
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
			   HpcPreparedQuery query = toQuery(sqlQueries, nestedCompoundQuery, sqlLevelFilters,
					                            defaultLevelFilter);	
               sqlQueryBuilder.append(query.sql);
               args.addAll(Arrays.asList(query.args));			   
		   }
		}
		sqlQueryBuilder.append(")");
		
    	HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
    	preparedQuery.sql = sqlQueryBuilder.toString();
    	preparedQuery.args = args.toArray();
    	return preparedQuery;
    }
    
    /**
     * Execute a SQL query to get collection or data object paths
     *
     * @param prepareQuery The prepared query to execute.
     * @return A list of paths.
     * @throws HpcException on database error.
     */
    private List<String> getPaths(HpcPreparedQuery prepareQuery) throws HpcException
    {
		try {
		     return jdbcTemplate.query(prepareQuery.sql, objectPathRowMapper, prepareQuery.args);
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get collection/data-object Paths: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}		
    }
    
    /**
     * Converts a query operator enum to SQL
     *
     * @param operator The operator to convert.
     * @return A SQL operator string.
     */
    private String toSQLOperator(HpcCompoundMetadataQueryOperator operator)
    {
    	return operator.equals(HpcCompoundMetadataQueryOperator.AND) ? "intersect" : "union";  
    }
    
    /**
     * Get a list of metadata attributes currently registered.
     *
     * @param query The query to invoke (for collection or data object metadata attributes).
     * @param level Filter the results by level. (Optional).
     * @param levelOperator The operator to use in the level filter. (Optional).
     * @param dataManagementUsername The Data Management user name. 
     * @param sqlLevelFilters The map from query operator to level filter ('where' condition).
     * @return A list of metadata attributes for each level.
     * @throws HpcException on database error or invalid level operator.
     */
    private List<HpcMetadataLevelAttributes> getMetadataAttributes(String query, Integer level, 
    		                                    HpcMetadataQueryOperator levelOperator,
    		                                    String dataManagementUsername,
    		                                    Map<HpcMetadataQueryOperator, String> sqlLevelFilters) 
                                                throws HpcException
	{
    	StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<Object> args = new ArrayList<>();
    	args.add(dataManagementUsername);
    	
    	sqlQueryBuilder.append(query);
    	
    	// Add level filter if provided.
    	if(level != null && levelOperator != null) {
		   String sqlLevelFilter = sqlLevelFilters.get(levelOperator);
		   if(sqlLevelFilter == null) {
			  throw new HpcException("Invalid level operator: " + levelOperator,
			                         HpcErrorType.INVALID_REQUEST_INPUT); 
		   }
		   sqlQueryBuilder.append(sqlLevelFilter);
		   args.add(level);
    	}
    	
    	// Add the grouping and order SQL.
    	sqlQueryBuilder.append(GET_METADATA_ATTRIBUTES_GROUP_ORDER_BY_SQL);
    	
		try {
			 return jdbcTemplate.query(sqlQueryBuilder.toString(), 
					                   metadataLevelAttributeRowMapper, 
					                   args.toArray());
		
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get metadata attributes: " + 
		                               e.getMessage(),
			    	                   HpcErrorType.DATABASE_ERROR, e);
		}
	}
}

 