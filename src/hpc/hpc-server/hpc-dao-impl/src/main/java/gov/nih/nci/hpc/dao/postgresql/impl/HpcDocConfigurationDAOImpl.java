/**
 * HpcDocConfigurationDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import gov.nih.nci.hpc.dao.HpcDocConfigurationDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataHierarchy;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.domain.model.HpcDocConfiguration;
import gov.nih.nci.hpc.exception.HpcException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * <p>
 * HPC DOC Configuration DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDocConfigurationDAOImpl implements HpcDocConfigurationDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	private static final String GET_DOC_CONFIGURATIONS_SQL = 
			                    "select * from public.\"HPC_DOC_CONFIGURATION\"";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Row mapper.
	private HpcDocConfigRowMapper rowMapper = new HpcDocConfigRowMapper();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcDocConfigurationDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDocConfigDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public List<HpcDocConfiguration> getDocConfigurations() throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_DOC_CONFIGURATIONS_SQL, rowMapper);
		     
		} catch(IncorrectResultSizeDataAccessException irse) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get DOC configurations: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}		
    }
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
	// HpcUser Table to Object mapper.
	private class HpcDocConfigRowMapper implements RowMapper<HpcDocConfiguration>
	{
		@Override
		public HpcDocConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			HpcDocConfiguration docConfiguration = new HpcDocConfiguration();
			docConfiguration.setDoc(rs.getString("DOC"));
			docConfiguration.setBasePath(rs.getString("BASE_PATH"));
			try {
			     docConfiguration.setDataHierarchy(getDataHierarchyFromJSONStr(rs.getString("DATA_HIERARCHY")));
			     docConfiguration.getCollectionMetadataValidationRules().addAll(
			        getMetadataValidationRulesFromJSONStr(rs.getString("COLLECTION_METADATA_VALIDATION_RULES")));
			     docConfiguration.getDataObjectMetadataValidationRules().addAll(
					        getMetadataValidationRulesFromJSONStr(rs.getString("DATA_OBJECT_METADATA_VALIDATION_RULES")));
			     
			} catch(HpcException e) {
				    throw new SQLException(e);
			}

			return docConfiguration;
		}
	}
	
    /**
     * Verify connection to DB. Called by Spring as init-method.
     * 
     * @throws HpcException If it failed to connect to the database.
     */
	@SuppressWarnings("unused")
	private void dbConnect() throws HpcException
    {
    	try {
    	     jdbcTemplate.getDataSource().getConnection();
    	     
    	} catch(Exception e) {
    		    throw new HpcException(
    		    		     "Failed to connect to PostgreSQL DB. Check credentials config", 
    		    		     HpcErrorType.DATABASE_ERROR, e);
    	}
    } 
	
    /**
     * Instantiate a data hierarchy from a string.
     * 
     * @param dataHierarchyJSONStr The data hierarchy JSON string.
     * @return HpcDataHierarchy
     * @throws HpcException If failed to parse JSON.
     */
	private HpcDataHierarchy getDataHierarchyFromJSONStr(String dataHierarchyJSONStr) 
			                                            throws HpcException
    {
		if(dataHierarchyJSONStr == null) {
		   return null;
		}
		
		try {
	         return dataHierarchyFromJSON((JSONObject) new JSONParser().parse(dataHierarchyJSONStr));
	         
		} catch(Exception e) {
		        throw new HpcException("Failed to parse data hierarchy JSON: " + dataHierarchyJSONStr,
                                       HpcErrorType.DATABASE_ERROR, e);
		}
    }
	
    /**
     * Instantiate a HpcDataHierarchy from JSON object.
     *
     * @param jsonDataHierarchy The data hierarchy JSON object. 
     * @return HpcDataHierarchy
     * 
     * @throws HpcException If failed to parse the JSON.
     */
    @SuppressWarnings("unchecked")
	private HpcDataHierarchy dataHierarchyFromJSON(JSONObject jsonDataHierarchy) 
    		                                      throws HpcException
    {
		HpcDataHierarchy dataHierarchy = new HpcDataHierarchy();
		
		if(!jsonDataHierarchy.containsKey("collectionType") ||
		   !jsonDataHierarchy.containsKey("isDataObjectContainer")) {
 	       throw new HpcException("Invalid Data Hierarchy Definition: " + jsonDataHierarchy,
 	                               HpcErrorType.DATABASE_ERROR);
		}
		
		dataHierarchy.setCollectionType((String)jsonDataHierarchy.get("collectionType"));
		dataHierarchy.setIsDataObjectContainer((Boolean)jsonDataHierarchy.get("isDataObjectContainer"));
    	
    	// Iterate through the sub collections.
	    JSONArray jsonSubCollections = (JSONArray) jsonDataHierarchy.get("subCollections");
  	    if(jsonSubCollections != null) {
		   Iterator<JSONObject> subCollectionsIterator = jsonSubCollections.iterator();
	       while(subCollectionsIterator.hasNext()) {
	    	     dataHierarchy.getSubCollectionsHierarchies().add(dataHierarchyFromJSON(subCollectionsIterator.next()));
	       }
  	    }
  	    
  	    return dataHierarchy;
    }
    
    /**
     * Instantiate metadata validation rules from string.
     * 
     * @param metadataValidationRulesJSONStr The metadata validation rules JSON String.
     * @return a list of metadata validation rules.
     * @throws HpcException on service failure.
     */
	private List<HpcMetadataValidationRule> 
	        getMetadataValidationRulesFromJSONStr(String metadataValidationRulesJSONStr) 
			                                     throws HpcException
    {
		if(metadataValidationRulesJSONStr == null) {
		   return new ArrayList<HpcMetadataValidationRule>();
		}
		
		try {
	         JSONArray jsonMetadataValidationRules = 
	        		   (JSONArray) ((JSONObject) new JSONParser().parse(metadataValidationRulesJSONStr)).
	        		                                              get("metadataValidationRules");
	         if(jsonMetadataValidationRules == null) {
	       	    throw new HpcException("Empty validation rules", HpcErrorType.DATABASE_ERROR); 
	         }
	         
	         return metadataValidationRulesFromJSON(jsonMetadataValidationRules);
	         
		} catch(Exception e) {
		    throw new HpcException("Failed to parse metadata validation rules JSON: " + metadataValidationRulesJSONStr,
                                   HpcErrorType.DATABASE_ERROR, e);
		}
    }   
	
    /**
     * Instantiate list metadata validation rules from JSON.
     *
     * @param jsonMetadataValidationRules The validation rules JSON array. 
     * @return A collection of metadata validation rules.
     * @throws HpcException If failed to parse the JSON.
     */
    @SuppressWarnings("unchecked")
	private List<HpcMetadataValidationRule> metadataValidationRulesFromJSON(JSONArray jsonMetadataValidationRules) 
    		                                                               throws HpcException
    {
    	List<HpcMetadataValidationRule> validationRules = new ArrayList<>();
    	
    	// Iterate through the rules and map to POJO.
    	Iterator<JSONObject> rulesIterator = jsonMetadataValidationRules.iterator();
    	while(rulesIterator.hasNext()) {
    		  JSONObject jsonMetadataValidationRule = rulesIterator.next();
    		
	    	  if(!jsonMetadataValidationRule.containsKey("attribute") ||
	    		 !jsonMetadataValidationRule.containsKey("mandatory") ||
	    		 !jsonMetadataValidationRule.containsKey("ruleEnabled")) {
	    		 throw new HpcException("Invalid rule JSON object: " + jsonMetadataValidationRule,
	    		                        HpcErrorType.DATABASE_ERROR);	
	    	  }
	    			
	    	  // JSON -> POJO.
	    	  HpcMetadataValidationRule metadataValidationRule = new HpcMetadataValidationRule();
	    	  metadataValidationRule.setAttribute((String) jsonMetadataValidationRule.get("attribute"));
	    	  metadataValidationRule.setMandatory((Boolean) jsonMetadataValidationRule.get("mandatory"));
	    	  metadataValidationRule.setRuleEnabled((Boolean) jsonMetadataValidationRule.get("ruleEnabled"));
	    	  metadataValidationRule.setDefaultValue((String) jsonMetadataValidationRule.get("defaultValue"));
	    	  metadataValidationRule.setDefaultUnit((String) jsonMetadataValidationRule.get("defaultUnit"));
	    	  
	    	  JSONArray jsonCollectionTypes = (JSONArray) jsonMetadataValidationRule.get("collectionTypes");
	    	  if(jsonCollectionTypes != null) {
		    	 Iterator<String> collectionTypeIterator = jsonCollectionTypes.iterator();
		    	 while(collectionTypeIterator.hasNext()) {
		    	   	   metadataValidationRule.getCollectionTypes().add(collectionTypeIterator.next());
		    	 }
	    	  }
	    	  
	    	  // Extract the valid values.
	    	  JSONArray jsonValidValues = (JSONArray) jsonMetadataValidationRule.get("validValues");
	    	  if(jsonValidValues != null) {
	    	     Iterator<String> validValuesIterator = jsonValidValues.iterator();
	    	     while(validValuesIterator.hasNext()) {
	    	    	   metadataValidationRule.getValidValues().add(validValuesIterator.next());
	    	     }
	    	  }
	    	  
	    	  validationRules.add(metadataValidationRule);
    	}
    	
    	return validationRules;
    }
}

 