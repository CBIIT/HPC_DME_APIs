/**
 * HpcMetadataValidator.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.domain.model.HpcMetadataValidationRules;
import gov.nih.nci.hpc.exception.HpcException;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * <p>
 * Validates various metadata provided by the user.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcMetadataValidator
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Collection type attribute name.
	private final static String COLLECTION_TYPE_ATTRIBUTE = "collection_type"; 
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// Metadata validation rules collection.
	private HpcMetadataValidationRules metadataValidationRules = new HpcMetadataValidationRules();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcMetadataValidator() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }  
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param metadataValidationRulesPath The path to the validation rules JSON.
     * 
     * @throws HpcException
     */
    private HpcMetadataValidator(String metadataValidationRulesPath) throws HpcException
    {
		try {
	         FileReader reader = new FileReader(metadataValidationRulesPath);
	         JSONObject jsonMetadataValidationRules = (JSONObject) ((JSONObject) new JSONParser().parse(reader)).get("HpcMetadataValidationRules");
	         
	         if(jsonMetadataValidationRules == null ||
	        	!jsonMetadataValidationRules.containsKey("collectionMetadataValidationRules") ||
	            !jsonMetadataValidationRules.containsKey("dataObjectMetadataValidationRules")) {
	        	 throw new HpcException("Invalid JSON rules: " + jsonMetadataValidationRules,
		                                HpcErrorType.SPRING_CONFIGURATION_ERROR);	
	         }
	         
	         metadataValidationRules.getCollectionMetadataValidationRules().addAll(
	        		 fromJSON((JSONArray) jsonMetadataValidationRules.get("collectionMetadataValidationRules")));
	         metadataValidationRules.getDataObjectMetadataValidationRules().addAll(
	        		 fromJSON((JSONArray) jsonMetadataValidationRules.get("dataObjectMetadataValidationRules")));
	         
		} catch(Exception e) {
			    throw new HpcException("Could not open or parse: " + metadataValidationRulesPath,
                                       HpcErrorType.SPRING_CONFIGURATION_ERROR, e);
		}
    }		

    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Validate collection metadata. Null unit values are converted to empty strings.
     *
     * @param metadataEntries The metadata entries collection to validate.
     * 
     * @throws HpcException If the metadata is invalid.
     */
    public void validateCollectionMetadata(List<HpcMetadataEntry> metadataEntries) 
    		                              throws HpcException
    {
    	validateMetadata(metadataEntries, 
    			         metadataValidationRules.getCollectionMetadataValidationRules());
    }
    
    /**
     * Validate data object metadata. Null unit values are converted to empty strings.
     *
     * @param metadataEntries The metadata entries collection to validate.
     * 
     * @throws HpcException If the metadata is invalid.
     */
    public void validateDataObjectMetadata(List<HpcMetadataEntry> metadataEntries) 
    		                              throws HpcException
    {
    	validateMetadata(metadataEntries, 
    			         metadataValidationRules.getDataObjectMetadataValidationRules());
    }
    		                               
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
    
    /**
     * Validate metadata. Null unit values are converted to empty strings.
     *
     * @param metadataEntries The metadata entries collection to validate.
     * @param metadataValidationRules Validation rules to apply.
     * 
     * @throws HpcException If the metadata is invalid.
     */
    private void validateMetadata(List<HpcMetadataEntry> metadataEntries,
    		                      List<HpcMetadataValidationRule> metadataValidationRules) 
    		                     throws HpcException
    {
    	// Crate a metadata <attribute, value> map.
    	Map<String, String> metadataEntriesMap = new HashMap<String, String>();
    	for(HpcMetadataEntry metadataEntry : metadataEntries) {
    		metadataEntriesMap.put(metadataEntry.getAttribute(), metadataEntry.getValue());
    		// Default null unit values to empty string (This is an iRODS expectation).
    		if(metadataEntry.getUnit() == null) {
    		   metadataEntry.setUnit("");	
    		}
    	}
    	
	    for(HpcMetadataValidationRule metadataValidationRule: metadataValidationRules) {
	    	// Skip disabled rules.
	    	if(!metadataValidationRule.getRuleEnabled()) {
	    	   continue;
	    	}
	    
	        // Skip rules for other collection types.
	    	String collectionType = metadataEntriesMap.get(COLLECTION_TYPE_ATTRIBUTE);
	    	if(collectionType != null &&
	    	   metadataValidationRule.getCollectionType() != null &&
	    	   !metadataValidationRule.getCollectionType().isEmpty() &&
	    	   !metadataValidationRule.getCollectionType().equals(collectionType)) {
	    	   continue;
	    	}
	    	
	    	// Apply default value/unit if default is defined and metadata was not provided.
	    	if(metadataValidationRule.getDefaultValue() != null &&
	    	   !metadataValidationRule.getDefaultValue().isEmpty()) {
	    	   if(!metadataEntriesMap.containsKey(metadataValidationRule.getAttribute())) {
				  HpcMetadataEntry defaultMetadataEntry = new HpcMetadataEntry();
				  defaultMetadataEntry.setAttribute(metadataValidationRule.getAttribute());
				  defaultMetadataEntry.setValue(metadataValidationRule.getDefaultValue());
				  defaultMetadataEntry.setUnit(metadataValidationRule.getDefaultUnit() != null ?
						                       metadataValidationRule.getDefaultUnit() : "");
				  metadataEntries.add(defaultMetadataEntry);
				  metadataEntriesMap.put(defaultMetadataEntry.getAttribute(), defaultMetadataEntry.getValue());
			   }
	    	}
	    	
	    	// Validate a mandatory metadata is provided.
	    	if(metadataValidationRule.getMandatory()) {
	    	   if(!metadataEntriesMap.containsKey(metadataValidationRule.getAttribute())) {	
				  // Metadata entry is missing, but no default is defined.
			      throw new HpcException("Missing mandataory metadata: " + 
			    		                 metadataValidationRule.getAttribute(), 
			                             HpcErrorType.INVALID_REQUEST_INPUT);
			   }
			}
	    	
	    	// Validate the metadata value is valid.
	    	if(metadataValidationRule.getValidValues() != null &&
	    	   !metadataValidationRule.getValidValues().isEmpty()) {
	    	   String value = metadataEntriesMap.get(metadataValidationRule.getAttribute());
	    	   if(metadataEntriesMap.containsKey(metadataValidationRule.getAttribute()) &&
	    		  !metadataValidationRule.getValidValues().contains(value)) {
	    		  throw new HpcException("Invalid Metadata Value: " + 
	    		                         metadataValidationRule.getAttribute() + " = " + 
	    				                 value + ". Valid values: " +
  		                                 metadataValidationRule.getValidValues(), 
                                         HpcErrorType.INVALID_REQUEST_INPUT);
	    	   }
	    	}
		}
    }  
    
    /**
     * Instantiate list metadata validation rules from JSON.
     *
     * @param jsonMetadataValidationRules The validation rules JSON array. 
     * 
     * @throws HpcException If failed to parse the JSON
     */
    @SuppressWarnings("unchecked")
	private List<HpcMetadataValidationRule> fromJSON(JSONArray jsonMetadataValidationRules) 
    		                                        throws HpcException
    {
    	List<HpcMetadataValidationRule> metadataValidationRules = 
    			                        new ArrayList<HpcMetadataValidationRule>();
    	
    	// Iterate through the rules and map to POJO.
    	Iterator<JSONObject> rulesIterator = jsonMetadataValidationRules.iterator();
    	while(rulesIterator.hasNext()) {
    		  JSONObject jsonMetadataValidationRule = rulesIterator.next();
    		
	    	  if(!jsonMetadataValidationRule.containsKey("attribute") ||
	    		 !jsonMetadataValidationRule.containsKey("mandatory") ||
	    		 !jsonMetadataValidationRule.containsKey("ruleEnabled") ||
	    		 !jsonMetadataValidationRule.containsKey("DOC")) {
	    		 throw new HpcException("Invalid rule JSON object: " + jsonMetadataValidationRule,
	    		                        HpcErrorType.SPRING_CONFIGURATION_ERROR);	
	    	  }
	    			
	    	  // JSON -> POJO.
	    	  HpcMetadataValidationRule metadataValidationRule = new HpcMetadataValidationRule();
	    	  metadataValidationRule.setAttribute((String) jsonMetadataValidationRule.get("attribute"));
	    	  metadataValidationRule.setMandatory((Boolean) jsonMetadataValidationRule.get("mandatory"));
	    	  metadataValidationRule.setRuleEnabled((Boolean) jsonMetadataValidationRule.get("ruleEnabled"));
	    	  metadataValidationRule.setDOC((String) jsonMetadataValidationRule.get("DOC"));
	    	  metadataValidationRule.setDefaultValue((String) jsonMetadataValidationRule.get("defaultValue"));
	    	  metadataValidationRule.setDefaultUnit((String) jsonMetadataValidationRule.get("defaultUnit"));
	    	  metadataValidationRule.setCollectionType((String) jsonMetadataValidationRule.get("collectionType"));
	    	  
	    	  // Extract the valid values.
	    	  JSONArray jsonValidValues = (JSONArray) jsonMetadataValidationRule.get("validValues");
	    	  if(jsonValidValues != null) {
	    	     Iterator<String> validValuesIterator = jsonValidValues.iterator();
	    	     while(validValuesIterator.hasNext()) {
	    	    	   metadataValidationRule.getValidValues().add(validValuesIterator.next());
	    	     }
	    	  }
	    	  
	    	  metadataValidationRules.add(metadataValidationRule);
    	}
    	
    	return metadataValidationRules;
    }
}

 