/**
 * HpcEventDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import gov.nih.nci.hpc.dao.HpcUserQueryDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryOperator;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQueryOperator;
import gov.nih.nci.hpc.exception.HpcException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * <p>
 * HPC User Query DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcUserQueryDAOImpl implements HpcUserQueryDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	private static final String UPSERT_USER_QUERY_SQL = 
			"insert into public.\"HPC_USER_QUERY\" ( " +
	                "\"USER_ID\", \"QUERY_NAME\", \"QUERY\") " +
	                "values (?, ?, ?) " + 
	        "on conflict(\"USER_ID\", \"QUERY_NAME\") do update " +
            "set \"QUERY\"=excluded.\"QUERY\"";
	
	private static final String DELETE_USER_QUERY_SQL = 
		    "delete from public.\"HPC_USER_QUERY\" where \"USER_ID\" = ? and \"QUERY_NAME\" = ?";
	
	private static final String GET_USER_QUERIES_SQL = 
		    "select * from public.\"HPC_USER_QUERY\" where \"USER_ID\" = ?";
	
	private static final String GET_USER_QUERY_SQL = 
		    "select * from public.\"HPC_USER_QUERY\" where \"USER_ID\" = ? and \"QUERY_NAME\" = ?";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Encryptor.
	@Autowired
	HpcEncryptor encryptor = null;
	
	// Row mappers.
	private HpcUserQueryRowMapper userQueryRowMapper = new HpcUserQueryRowMapper();
	
    // The logger instance.
	private static final Logger logger = 
			LoggerFactory.getLogger(HpcUserDAOImpl.class.getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcUserQueryDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcEventDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void upsertQuery(String nciUserId,
                            HpcCompoundMetadataQuery compoundMetadataQuery) 
                           throws HpcException
	{
		try {
		     jdbcTemplate.update(UPSERT_USER_QUERY_SQL,
		    		             nciUserId, compoundMetadataQuery.getName(),
		    		             encryptor.encrypt(toJSONString(compoundMetadataQuery)));
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a user query " + 
		                               e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, e);
		}
	}
	
	@Override
	public void deleteQuery(String nciUserId, String queryName) throws HpcException
	{
		try {
		     jdbcTemplate.update(DELETE_USER_QUERY_SQL, nciUserId, queryName);
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to delete a user query" + 
		                               e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, e);
		}   
	}
	
	@Override
	public List<HpcCompoundMetadataQuery> getQueries(String nciUserId) throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_USER_QUERIES_SQL, userQueryRowMapper, nciUserId);
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get user queries: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}	    	
    }
    
    @Override
    public HpcCompoundMetadataQuery getQuery(String nciUserId, String queryName) 
    		                                throws HpcException
    {
    	{
    		try {
    		     return jdbcTemplate.queryForObject(GET_USER_QUERY_SQL, userQueryRowMapper, 
    		    		                            nciUserId, queryName);
    		     
    		} catch(IncorrectResultSizeDataAccessException irse) {
    			    logger.error("Multiple queries with the same name found", irse);
    			    return null;
    			    
    		} catch(DataAccessException e) {
    		        throw new HpcException("Failed to get a user query: " + e.getMessage(),
    		    	    	               HpcErrorType.DATABASE_ERROR, e);
    		}
    	}
    }
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  

	// HpcEvent Row to Object mapper.
	private class HpcUserQueryRowMapper implements RowMapper<HpcCompoundMetadataQuery>
	{
		@Override
		public HpcCompoundMetadataQuery mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			return fromJSON(encryptor.decrypt(rs.getBytes("QUERY")));
		}
	}
	
    /** 
     * Convert compound query into a JSON string.
     * 
     * @param payloadEntries List of payload entries.
     * @return A JSON representation of the payload entries.
     */
	private String toJSONString(HpcCompoundMetadataQuery compoundMetadataQuery)
	{
		return toJSON(compoundMetadataQuery).toJSONString();
	}
	
    /** 
     * Convert compound query into a JSON object.
     * 
     * @param payloadEntries List of payload entries.
     * @return A JSON representation of the payload entries.
     */
	@SuppressWarnings("unchecked")
	private JSONObject toJSON(HpcCompoundMetadataQuery compoundMetadataQuery)
	{
		JSONObject jsonCompoundMetadataQuery = new JSONObject();
		
		if(compoundMetadataQuery == null) {
		   return jsonCompoundMetadataQuery;
		}
		
		// Map the compound operator
		jsonCompoundMetadataQuery.put("operator", compoundMetadataQuery.getOperator().value());
		jsonCompoundMetadataQuery.put("name", compoundMetadataQuery.getName());
		
		// Map the nested metadata queries.
		JSONArray jsonQueries = new JSONArray();
		for(HpcMetadataQuery nestedQquery : compoundMetadataQuery.getQueries()) {
			JSONObject jsonQuery = new JSONObject();
			jsonQuery.put("attribute", nestedQquery.getAttribute());
			jsonQuery.put("operator", nestedQquery.getOperator().value());
			jsonQuery.put("value", nestedQquery.getValue());
			if(nestedQquery.getLevel() != null && nestedQquery.getLevelOperator() != null) {
			   jsonQuery.put("level", nestedQquery.getLevel().toString());
			   jsonQuery.put("levelOperator", nestedQquery.getLevelOperator().value());
			   
			}
			
			jsonQueries.add(jsonQuery);
		}
		jsonCompoundMetadataQuery.put("queries", jsonQueries);
		
		// Map the nested compound queries.
		JSONArray jsonCompoundQueries = new JSONArray();
		for(HpcCompoundMetadataQuery nestedCompoundQuery : compoundMetadataQuery.getCompoundQueries()) {
		    jsonCompoundQueries.add(toJSON(nestedCompoundQuery));
		}
		jsonCompoundMetadataQuery.put("compoundQueries", jsonCompoundQueries);
		
		return jsonCompoundMetadataQuery;
	}
	  
    /** 
     * Convert JSON string to HpcCompoundMetadataQuery domain object.
     * 
     * @param jsonCompoundMetadataQueryStr The compound query JSON string.
     * @return HpcCompoundMetadataQuery
     */
	private HpcCompoundMetadataQuery fromJSON(String jsonCompoundMetadataQueryStr)
	{
		HpcCompoundMetadataQuery compoundMetadataQuery = new HpcCompoundMetadataQuery();
		if(jsonCompoundMetadataQueryStr == null || jsonCompoundMetadataQueryStr.isEmpty()) {
		   return compoundMetadataQuery;
		}

		// Parse the JSON string.
		JSONObject jsonCompoundMetadataQuery = null;
		try {
			 jsonCompoundMetadataQuery = (JSONObject) (new JSONParser().parse(jsonCompoundMetadataQueryStr));
			 
		} catch(ParseException e) {
			    return compoundMetadataQuery;
		}
		
		return fromJSON(jsonCompoundMetadataQuery);
	}	
	
    /** 
     * Convert JSON string to HpcCompoundMetadataQuery domain object.
     * 
     * @param jsonCompoundMetadataQueryStr The compound query JSON.
     * @return HpcCompoundMetadataQuery
     */
	@SuppressWarnings("unchecked")
	private HpcCompoundMetadataQuery fromJSON(JSONObject jsonCompoundMetadataQuery)
	{
		HpcCompoundMetadataQuery compoundMetadataQuery = new HpcCompoundMetadataQuery();
		compoundMetadataQuery.setOperator(HpcCompoundMetadataQueryOperator.fromValue(
				                          jsonCompoundMetadataQuery.get("operator").toString()));
		compoundMetadataQuery.setName(jsonCompoundMetadataQuery.get("name").toString());
		
		// Map the nested metadata queries.
	    JSONArray jsonQueries = (JSONArray) jsonCompoundMetadataQuery.get("queries");
  	    if(jsonQueries != null) {
		   Iterator<JSONObject> queriesIterator = jsonQueries.iterator();
	       while(queriesIterator.hasNext()) {
	    	     compoundMetadataQuery.getQueries().add(metadataQueryFromJSON(queriesIterator.next()));
	       }
  	    }
  	    
		// Map the nested compound metadata queries.
	    JSONArray jsonCompoundQueries = (JSONArray) jsonCompoundMetadataQuery.get("compoundQueries");
  	    if(jsonCompoundQueries != null) {
		   Iterator<JSONObject> compoundQueriesIterator = jsonCompoundQueries.iterator();
	       while(compoundQueriesIterator.hasNext()) {
	    	     compoundMetadataQuery.getCompoundQueries().add(fromJSON(compoundQueriesIterator.next()));
	       }
  	    }
  	    
  	    return compoundMetadataQuery;
	}	
	
    /**
     * Instantiate a HpcMetadataQuery from JSON.
     *
     * @param jsonMetadataQuery The metadata query JSON object. 
     * @return HpcMetadataQuery
     * 
     * @throws HpcException If failed to parse the JSON.
     */
	private HpcMetadataQuery metadataQueryFromJSON(JSONObject jsonMetadataQuery) 
    {
    	HpcMetadataQuery metadataQuery = new HpcMetadataQuery();
		
    	metadataQuery.setAttribute(jsonMetadataQuery.get("attribute").toString());
    	metadataQuery.setOperator(HpcMetadataQueryOperator.fromValue(
    			                  jsonMetadataQuery.get("operator").toString()));
    	metadataQuery.setValue(jsonMetadataQuery.get("value").toString());
    	Object level = jsonMetadataQuery.get("level");
    	Object levelOperator = jsonMetadataQuery.get("levelOperator");
    	if(level != null && levelOperator != null) {
    	   metadataQuery.setLevel(Integer.valueOf(level.toString()));
    	   metadataQuery.setLevelOperator(HpcMetadataQueryOperator.fromValue(
    			                          jsonMetadataQuery.get("levelOperator").toString()));
    	}
    	
    	return metadataQuery;
    }
	
	
}

 