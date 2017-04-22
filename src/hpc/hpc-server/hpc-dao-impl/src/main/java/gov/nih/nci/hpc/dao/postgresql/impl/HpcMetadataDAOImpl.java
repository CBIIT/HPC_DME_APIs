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
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryAttributeMatch;
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
	        "where collection.meta_attr_value = ?";
	
	private static final String GET_COLLECTION_IDS_NOT_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where collection.meta_attr_value <> ?";
	
	private static final String GET_COLLECTION_IDS_LIKE_SQL = 
			"select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
		    "where lower(collection.meta_attr_value) like lower(?)";
	
	private static final String GET_COLLECTION_IDS_NUM_LESS_THAN_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where num_less_than(collection.meta_attr_value, ?) = true";
	
	private static final String GET_COLLECTION_IDS_NUM_LESS_OR_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where num_less_or_equal(collection.meta_attr_value, ?) = true";
	
	private static final String GET_COLLECTION_IDS_NUM_GREATER_THAN_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where num_greater_than(collection.meta_attr_value, ?) = true";
	
	private static final String GET_COLLECTION_IDS_NUM_GREATER_OR_EQUAL_SQL = 
		    "select distinct collection.object_id from public.\"r_coll_hierarchy_meta_main\" collection " +
	        "where num_greater_or_equal(collection.meta_attr_value, ?) = true";
	
	private static final String GET_COLLECTION_EXACT_ATTRIBUTE_MATCH_FILTER = " and collection.meta_attr_name = ?";
	
	private static final String GET_DATA_OBJECT_IDS_EQUAL_SQL = 
			"select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
		    "where dataObject.meta_attr_value = ?";
	
	private static final String GET_DATA_OBJECT_IDS_NOT_EQUAL_SQL = 
			"select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
		    "where dataObject.meta_attr_value <> ?";
	
	private static final String GET_DATA_OBJECT_IDS_LIKE_SQL = 
			"select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
		    "where lower(dataObject.meta_attr_value) like lower(?)";
	
	private static final String GET_DATA_OBJECT_IDS_NUM_LESS_THAN_SQL = 
		    "select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
	        "where num_less_than(dataObject.meta_attr_value, ?) = true";
	
	private static final String GET_DATA_OBJECT_IDS_NUM_LESS_OR_EQUAL_SQL = 
		    "select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
	        "where num_less_or_equal(dataObject.meta_attr_value, ?) = true";
	
	private static final String GET_DATA_OBJECT_IDS_NUM_GREATER_THAN_SQL = 
		    "select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
	        "where num_greater_than(dataObject.meta_attr_value, ?) = true";
	
	private static final String GET_DATA_OBJECT_IDS_NUM_GREATER_OR_EQUAL_SQL = 
		    "select distinct dataObject.object_id from public.\"r_data_hierarchy_meta_main\" dataObject " +
	        "where num_greater_or_equal(dataObject.meta_attr_value, ?) = true";
	
	private static final String GET_DATA_OBJECT_EXACT_ATTRIBUTE_MATCH_FILTER = " and dataObject.meta_attr_name = ?";
	
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
	
	private static final String DATA_OBJECT_LEVEL_LABEL_EQUAL_FILTER = " and dataObject.level_label = ?";
	private static final String DATA_OBJECT_LEVEL_LABEL_NOT_EQUAL_FILTER = " and dataObject.level_label <> ?";
	private static final String DATA_OBJECT_LEVEL_LABEL_LIKE_FILTER = " and dataObject.level_label like ?";
	
	private static final String COLLECTION_LEVEL_LABEL_EQUAL_FILTER = " and collection.level_label = ?";
	private static final String COLLECTION_LEVEL_LABEL_NOT_EQUAL_FILTER = " and collection.level_label <> ?";
	private static final String COLLECTION_LEVEL_LABEL_LIKE_FILTER = " and collection.level_label like ?";
		   
	private static final String USER_ACCESS_SQL = 
			"select distinct access.object_id from public.\"r_objt_access\" access, public.\"r_user_group\" user_group, " +
			"public.\"r_user_main\" account where (account.user_name = ? and access.user_id = account.user_id) or " +
			"(access.user_id = user_group.group_user_id and user_group.group_user_id in " + 
			"(select user_group.group_user_id from public.\"r_user_group\" user_group, " +
		    "public.\"r_user_main\" account where account.user_name = ? and account.user_id = user_group.user_id))";
	
	private static final String USER_ACCESS_ARRAY_SQL = 
			"select array_agg(distinct access.object_id) from public.\"r_objt_access\" access, public.\"r_user_group\" user_group, " +
			"public.\"r_user_main\" account where (account.user_name = ? and access.user_id = account.user_id) or " +
			"(access.user_id = user_group.group_user_id and user_group.group_user_id in " + 
			"(select user_group.group_user_id from public.\"r_user_group\" user_group, " +
		    "public.\"r_user_main\" account where account.user_name = ? and account.user_id = user_group.user_id))";
	
	private static final String LIMIT_OFFSET_SQL = " order by object_path limit ? offset ?";
	
	private static final String GET_COLLECTION_PATHS_SQL = 
			"select distinct object_path from public.\"r_coll_hierarchy_meta_main\" where object_id in ";
	
	private static final String GET_COLLECTION_COUNT_SQL = 
			"select count(distinct object_id) from public.\"r_coll_hierarchy_meta_main\" where object_id in ";
	
	private static final String GET_DATA_OBJECT_PATHS_SQL = 
			"select distinct object_path from public.\"r_data_hierarchy_meta_main\" where object_id in ";
	
	private static final String GET_DATA_OBJECT_COUNT_SQL = 
			"select count(distinct object_id) from public.\"r_data_hierarchy_meta_main\" where object_id in ";
	
	private static final String GET_COLLECTION_METADATA_SQL = 
			"select meta_attr_name,  meta_attr_value, meta_attr_unit, level, level_label " + 
	        "from public.\"r_coll_hierarchy_meta_main\" where object_path = ? and level >= ? order by level";
	
	private static final String GET_DATA_OBJECT_METADATA_SQL = 
			"select meta_attr_name, meta_attr_value, meta_attr_unit, level, level_label " + 
	        "from public.\"r_data_hierarchy_meta_main\" where object_path = ? and level >= ? order by level";
	
	private static final String REFRESH_VIEW_SQL = "refresh materialized view concurrently";
	
	private static final String GET_COLLECTION_METADATA_ATTRIBUTES_SQL = 
			"select collection.level_label, array_agg(collection.meta_attr_name) as attributes " +
	        "from public.\"r_coll_hierarchy_meta_attr_name\" collection " +
	        "where collection.object_ids && (" + USER_ACCESS_ARRAY_SQL +") ";
	
	private static final String GET_DATA_OBJECT_METADATA_ATTRIBUTES_SQL = 
			"select dataObject.level_label, array_agg(dataObject.meta_attr_name) as attributes " +
			"from public.\"r_data_hierarchy_meta_attr_name\" dataObject " +
			"where dataObject.object_ids && (" +  USER_ACCESS_ARRAY_SQL +") ";
	
	private static final String GET_METADATA_ATTRIBUTES_GROUP_ORDER_BY_SQL = 
			" group by level_label order by level_label";
	
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
	
	// SQL Maps from operators to queries and filters.
	HpcSQLMaps dataObjectSQL = new HpcSQLMaps();
	HpcSQLMaps collectionSQL = new HpcSQLMaps();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcMetadataDAOImpl()
    {
    	dataObjectSQL.queries.put(HpcMetadataQueryOperator.EQUAL, 
    			                  GET_DATA_OBJECT_IDS_EQUAL_SQL);
    	dataObjectSQL.queries.put(HpcMetadataQueryOperator.NOT_EQUAL, 
    			                  GET_DATA_OBJECT_IDS_NOT_EQUAL_SQL);
    	dataObjectSQL.queries.put(HpcMetadataQueryOperator.LIKE, 
    			                  GET_DATA_OBJECT_IDS_LIKE_SQL);
    	dataObjectSQL.queries.put(HpcMetadataQueryOperator.NUM_LESS_THAN, 
    			                  GET_DATA_OBJECT_IDS_NUM_LESS_THAN_SQL);
    	dataObjectSQL.queries.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL, 
    			                  GET_DATA_OBJECT_IDS_NUM_LESS_OR_EQUAL_SQL);
    	dataObjectSQL.queries.put(HpcMetadataQueryOperator.NUM_GREATER_THAN, 
                                  GET_DATA_OBJECT_IDS_NUM_GREATER_THAN_SQL);
    	dataObjectSQL.queries.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL, 
    			                  GET_DATA_OBJECT_IDS_NUM_GREATER_OR_EQUAL_SQL);
    	
    	collectionSQL.queries.put(HpcMetadataQueryOperator.EQUAL, 
    			                  GET_COLLECTION_IDS_EQUAL_SQL);
    	collectionSQL.queries.put(HpcMetadataQueryOperator.NOT_EQUAL, 
    			                  GET_COLLECTION_IDS_NOT_EQUAL_SQL);
    	collectionSQL.queries.put(HpcMetadataQueryOperator.LIKE, 
    			                  GET_COLLECTION_IDS_LIKE_SQL);
    	collectionSQL.queries.put(HpcMetadataQueryOperator.NUM_LESS_THAN, 
    			                  GET_COLLECTION_IDS_NUM_LESS_THAN_SQL);
    	collectionSQL.queries.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL, 
    			                  GET_COLLECTION_IDS_NUM_LESS_OR_EQUAL_SQL);
    	collectionSQL.queries.put(HpcMetadataQueryOperator.NUM_GREATER_THAN, 
                                  GET_COLLECTION_IDS_NUM_GREATER_THAN_SQL);
    	collectionSQL.queries.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL, 
    			                  GET_COLLECTION_IDS_NUM_GREATER_OR_EQUAL_SQL);
    	
    	dataObjectSQL.levelFilters.put(HpcMetadataQueryOperator.EQUAL, 
    			                       DATA_OBJECT_LEVEL_EQUAL_FILTER);
    	dataObjectSQL.levelFilters.put(HpcMetadataQueryOperator.NOT_EQUAL, 
                                      DATA_OBJECT_LEVEL_NOT_EQUAL_FILTER);
    	dataObjectSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_LESS_THAN, 
                                       DATA_OBJECT_LEVEL_NUM_LESS_THAN_FILTER);
    	dataObjectSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL, 
                                       DATA_OBJECT_LEVEL_NUM_LESS_OR_EQUAL_FILTER);
    	dataObjectSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_GREATER_THAN, 
                                       DATA_OBJECT_LEVEL_NUM_GREATER_THAN_FILTER);
    	dataObjectSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL, 
                                       DATA_OBJECT_LEVEL_NUM_GREATER_OR_EQUAL_FILTER);
    	
    	collectionSQL.levelFilters.put(HpcMetadataQueryOperator.EQUAL, 
    			                       COLLECTION_LEVEL_EQUAL_FILTER);
    	collectionSQL.levelFilters.put(HpcMetadataQueryOperator.NOT_EQUAL, 
                                       COLLECTION_LEVEL_NOT_EQUAL_FILTER);
    	collectionSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_LESS_THAN, 
                                       COLLECTION_LEVEL_NUM_LESS_THAN_FILTER);
    	collectionSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_LESS_OR_EQUAL, 
                                       COLLECTION_LEVEL_NUM_LESS_OR_EQUAL_FILTER);
    	collectionSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_GREATER_THAN, 
                                       COLLECTION_LEVEL_NUM_GREATER_THAN_FILTER);
    	collectionSQL.levelFilters.put(HpcMetadataQueryOperator.NUM_GREATER_OR_EQUAL, 
                                       COLLECTION_LEVEL_NUM_GREATER_OR_EQUAL_FILTER);
    	
    	dataObjectSQL.levelLabelFilters.put(HpcMetadataQueryOperator.EQUAL, 
                                            DATA_OBJECT_LEVEL_LABEL_EQUAL_FILTER);
    	dataObjectSQL.levelLabelFilters.put(HpcMetadataQueryOperator.NOT_EQUAL, 
                                            DATA_OBJECT_LEVEL_LABEL_NOT_EQUAL_FILTER);
    	dataObjectSQL.levelLabelFilters.put(HpcMetadataQueryOperator.LIKE, 
                                            DATA_OBJECT_LEVEL_LABEL_LIKE_FILTER);
    	
    	collectionSQL.levelLabelFilters.put(HpcMetadataQueryOperator.EQUAL, 
                                            COLLECTION_LEVEL_LABEL_EQUAL_FILTER);
    	collectionSQL.levelLabelFilters.put(HpcMetadataQueryOperator.NOT_EQUAL, 
                                            COLLECTION_LEVEL_LABEL_NOT_EQUAL_FILTER);
    	collectionSQL.levelLabelFilters.put(HpcMetadataQueryOperator.LIKE, 
                                            COLLECTION_LEVEL_LABEL_LIKE_FILTER);
    	
    	dataObjectSQL.exactAttributeMatchFilter = GET_DATA_OBJECT_EXACT_ATTRIBUTE_MATCH_FILTER;
    	collectionSQL.exactAttributeMatchFilter = GET_COLLECTION_EXACT_ATTRIBUTE_MATCH_FILTER;
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
                                     toQuery(collectionSQL, compoundMetadataQuery, defaultLevelFilter),
                                     dataManagementUsername, offset, limit));
    }
	
	@Override
    public int getCollectionCount(HpcCompoundMetadataQuery compoundMetadataQuery,
    		                      String dataManagementUsername,
                                  HpcMetadataQueryLevelFilter defaultLevelFilter) 
                                 throws HpcException
    {
		return getCount(prepareQuery(GET_COLLECTION_COUNT_SQL, 
                                     toQuery(collectionSQL, compoundMetadataQuery, defaultLevelFilter),
                                     dataManagementUsername, null, null));
    }

	@Override 
	public List<String> getDataObjectPaths(HpcCompoundMetadataQuery compoundMetadataQuery,
			                               String dataManagementUsername,
			                               int offset, int limit,
			                               HpcMetadataQueryLevelFilter defaultLevelFilter) 
                                          throws HpcException
    {
		return getPaths(prepareQuery(GET_DATA_OBJECT_PATHS_SQL, 
                                     toQuery(dataObjectSQL, compoundMetadataQuery, defaultLevelFilter),
                                     dataManagementUsername, offset, limit));
    }
	
	@Override 
	public int getDataObjectCount(HpcCompoundMetadataQuery compoundMetadataQuery,
			                      String dataManagementUsername,
			                      HpcMetadataQueryLevelFilter defaultLevelFilter) 
                                 throws HpcException
    {
		return getCount(prepareQuery(GET_DATA_OBJECT_COUNT_SQL, 
                                     toQuery(dataObjectSQL, compoundMetadataQuery, defaultLevelFilter),
                                     dataManagementUsername, null, null));
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
    public List<HpcMetadataLevelAttributes> 
    getCollectionMetadataAttributes(String levelLabel, String dataManagementUsername) 
    		                       throws HpcException
    {
    	return getMetadataAttributes(GET_COLLECTION_METADATA_ATTRIBUTES_SQL, 
    			                     levelLabel, dataManagementUsername,
    			                     COLLECTION_LEVEL_LABEL_EQUAL_FILTER);
    }
    
    @Override
    public List<HpcMetadataLevelAttributes> 
    getDataObjectMetadataAttributes(String levelLabel, String dataManagementUsername) 
    		                       throws HpcException
    {
    	return getMetadataAttributes(GET_DATA_OBJECT_METADATA_ATTRIBUTES_SQL, 
    			                     levelLabel, dataManagementUsername, 
    			                     DATA_OBJECT_LEVEL_LABEL_EQUAL_FILTER);
    }
    
	@Override
	public void refreshViews() throws HpcException
    {
		try {
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_coll_hierarchy_metamap");
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_coll_hierarchy_meta_main");
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_coll_hierarchy_meta_attr_name");
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_data_hierarchy_metamap");
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_data_hierarchy_meta_main");
		     jdbcTemplate.execute(REFRESH_VIEW_SQL + " r_data_hierarchy_meta_attr_name");
		     
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
			metadataEntry.setAttribute(rs.getString("META_ATTR_NAME"));
			metadataEntry.setValue(rs.getString("META_ATTR_VALUE"));
			String unit = rs.getString("META_ATTR_UNIT");
			metadataEntry.setUnit(unit != null && !unit.isEmpty() ? unit : null);
			
			Long level = rs.getLong("LEVEL");
			metadataEntry.setLevel(level != null ? level.intValue() : null);
			metadataEntry.setLevelLabel(rs.getString("LEVEL_LABEL"));
			
			return metadataEntry;
		}
	}
	
	private class HpcMetadataLevelAttributesRowMapper implements RowMapper<HpcMetadataLevelAttributes>
	{
		@Override
		public HpcMetadataLevelAttributes mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			HpcMetadataLevelAttributes metadataLevelAttributes = new HpcMetadataLevelAttributes();
			metadataLevelAttributes.setLevelLabel(rs.getString("LEVEL_LABEL"));
			
			// Extract the metadata attributes for this level. Defensive coding to exclude any null values.
			String[] metadataAttributes = (String[]) rs.getArray("ATTRIBUTES").getArray();
			int metadataAttributesSize = metadataAttributes.length;
			for(int i = 0; i < metadataAttributesSize; i++) {
				if(metadataAttributes[i] != null) {
				   metadataLevelAttributes.getMetadataAttributes().add(metadataAttributes[i]);
				}
			}
			
			return metadataLevelAttributes;
		}
	}
	
	// Prepared query.
	private class HpcPreparedQuery
	{
		public String sql = null;
		public Object[] args = null;
	}
	
	// SQL Maps from operators to queries and filters.
	private class HpcSQLMaps
	{
		private Map<HpcMetadataQueryOperator, String> queries = new HashMap<>();
		private Map<HpcMetadataQueryOperator, String> levelFilters = new HashMap<>();
		private Map<HpcMetadataQueryOperator, String> levelLabelFilters = new HashMap<>();	
		private String exactAttributeMatchFilter = null;
	}
	
    /**
     * Prepare a SQL query. Map operators to SQL and concatenate them with 'intersect'.
     *
     * @param getObjectPathsQuery The query to get object paths based on object IDs.
     * @param userQuery The calculated SQL query based on user input (represented by query domain objects).
     * @param dataManagementUsername The data management user name.
     * @param offset Skip that many path in the returned results.
     * @param limit No more than 'limit' paths will be returned.
     * @return A prepared query.
     */
    private HpcPreparedQuery prepareQuery(String getObjectPathsQuery,
    		                              HpcPreparedQuery userQuery,
    		                              String dataManagementUsername,
    		                              Integer offset, Integer limit) 
    {
    	StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<Object> args = new ArrayList<>();
    	
    	// Combine the metadata queries into a single SQL statement.
    	sqlQueryBuilder.append(getObjectPathsQuery + "(");
    	sqlQueryBuilder.append(userQuery.sql);
    	args.addAll(Arrays.asList(userQuery.args));
    	
    	// Add a query to only include entities the user can access.
    	sqlQueryBuilder.append(" intersect ");
    	sqlQueryBuilder.append(USER_ACCESS_SQL + ")");
    	args.add(dataManagementUsername);
    	args.add(dataManagementUsername);
    	
    	if(offset != null && limit != null) {
    	   sqlQueryBuilder.append(LIMIT_OFFSET_SQL);
    	   args.add(limit);
    	   args.add(offset);
    	}
    	
    	HpcPreparedQuery preparedQuery = new HpcPreparedQuery();
    	preparedQuery.sql = sqlQueryBuilder.toString();
    	preparedQuery.args = args.toArray();
    	return preparedQuery;
    }
    
    /**
     * Create a SQL statement from List&lt;HpcMetadataQuery&gt;. 
     *
     * @param sql The map from query operator to SQL queries and filters.
     * @param metadataQueries The metadata queries.
     * @param operator The compound metadata query operator to use.
     * @param defaultLevelFilter A default level filter to use if not provided in the query.
     * @return A prepared query.
     * @throws HpcException If invalid metadata query operator provided.
     */
    private HpcPreparedQuery toQuery(HpcSQLMaps sql, 
    		                         List<HpcMetadataQuery> metadataQueries,
    		                         HpcCompoundMetadataQueryOperator operator,
    		                         HpcMetadataQueryLevelFilter defaultLevelFilter) 
    		                        throws HpcException
    {
    	StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<Object> args = new ArrayList<>();
    
		sqlQueryBuilder.append("(");
		for(HpcMetadataQuery metadataQuery : metadataQueries) {
			String sqlQuery = sql.queries.get(metadataQuery.getOperator());
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
			args.add(metadataQuery.getValue());

			// Optionally append a filter to have exact attribute match. 
			if(metadataQuery.getAttributeMatch() == null || 
			   metadataQuery.getAttributeMatch().equals(HpcMetadataQueryAttributeMatch.EXACT)) {
			   sqlQueryBuilder.append(sql.exactAttributeMatchFilter);
			   args.add(metadataQuery.getAttribute());
			}
			
			// Add a filter for level. 
			HpcMetadataQueryLevelFilter levelFilter = metadataQuery.getLevelFilter() != null ?
					                                  metadataQuery.getLevelFilter() : defaultLevelFilter;
			if(levelFilter != null) {
			   boolean labelFilter = levelFilter.getLabel() != null;
		       String sqlLevelFilter = labelFilter ? sql.levelLabelFilters.get(levelFilter.getOperator()) :  
		    	                                     sql.levelFilters.get(levelFilter.getOperator());
		       if(sqlLevelFilter == null) {
		          throw new HpcException("Invalid level operator: " + levelFilter.getOperator(),
		                                 HpcErrorType.INVALID_REQUEST_INPUT); 
		       }
		       sqlQueryBuilder.append(sqlLevelFilter);
		       args.add(labelFilter ? levelFilter.getLabel() : levelFilter.getLevel());
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
     * @param sql The map from query operator to SQL queries and filters.
     * @param compoundMetadataQuery The compound query to create SQL from.
     * @param defaultLevelFilter A default level filter to use if not provided in the query.
     * @return A prepared query.
     * @throws HpcException on service failure.
     */
    private HpcPreparedQuery toQuery(HpcSQLMaps sql,  
    		                         HpcCompoundMetadataQuery compoundMetadataQuery,
    		                         HpcMetadataQueryLevelFilter defaultLevelFilter) 
    		                        throws HpcException
    {
    	StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<Object> args = new ArrayList<>();
    
		sqlQueryBuilder.append("(");
		// Append the simple queries.
		if(compoundMetadataQuery.getQueries() != null && !compoundMetadataQuery.getQueries().isEmpty()) {
			HpcPreparedQuery query = toQuery(sql, compoundMetadataQuery.getQueries(), 
					                         compoundMetadataQuery.getOperator(), defaultLevelFilter);	
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
			   HpcPreparedQuery query = toQuery(sql, nestedCompoundQuery, defaultLevelFilter);	
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
     * Execute a SQL query to get collection or data object paths.
     *
     * @param preparedQuery The prepared query to execute.
     * @return A list of paths.
     * @throws HpcException on database error.
     */
    private List<String> getPaths(HpcPreparedQuery preparedQuery) throws HpcException
    {
		try {
		     return jdbcTemplate.query(preparedQuery.sql, objectPathRowMapper, preparedQuery.args);
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get collection/data-object Paths: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}		
    }
    
    /**
     * Execute a SQL query to get collection or data object count.
     *
     * @param preparedQuery The prepared query to execute.
     * @return The count
     * @throws HpcException on database error.
     */
    private int getCount(HpcPreparedQuery preparedQuery) throws HpcException
    {
		try {
		     return jdbcTemplate.queryForObject(preparedQuery.sql, Integer.class, preparedQuery.args);
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to count collection/data-object: " + 
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
     * @param levelLabel Filter the results by level label. (Optional).
     * @param dataManagementUsername The Data Management user name. 
     * @param sqlLevelLabelFilter The SQL filter to apply for level label ('where' condition).
     * @return A list of metadata attributes for each level.
     * @throws HpcException on database.
     */
    private List<HpcMetadataLevelAttributes> getMetadataAttributes(String query, String levelLabel, 
    		                                                       String dataManagementUsername,
    		                                                       String sqlLevelLabelFilter) 
                                                                  throws HpcException
	{
    	StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<Object> args = new ArrayList<>();
    	args.add(dataManagementUsername);
    	args.add(dataManagementUsername);
    	
    	sqlQueryBuilder.append(query);
    	
    	// Add level label filter if provided.
    	if(levelLabel != null && !levelLabel.isEmpty()) {
		   sqlQueryBuilder.append(sqlLevelLabelFilter);
		   args.add(levelLabel);
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

 