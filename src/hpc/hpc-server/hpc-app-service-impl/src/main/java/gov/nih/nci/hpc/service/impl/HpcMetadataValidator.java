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
import gov.nih.nci.hpc.exception.HpcException;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private static final String COLLECTION_TYPE_ATTRIBUTE = "collection_type"; 
	
    // System generated metadata attributes.
	public static final String ID_ATTRIBUTE = "uuid";
	public static final String REGISTRAR_ID_ATTRIBUTE = "registered_by";
	public static final String REGISTRAR_NAME_ATTRIBUTE = "registered_by_name";
	public static final String REGISTRAR_DOC_ATTRIBUTE = "registered_by_doc";
	public static final String SOURCE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE = 
                               "source_file_container_id"; 
	public static final String SOURCE_LOCATION_FILE_ID_ATTRIBUTE = 
                               "source_file_id"; 
	public static final String ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE = 
			                   "archive_file_container_id"; 
	public static final String ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE = 
			                   "archive_file_id"; 
	public static final String DATA_TRANSFER_REQUEST_ID_ATTRIBUTE = 
                               "data_transfer_request_id";
	public static final String DATA_TRANSFER_STATUS_ATTRIBUTE = 
                               "data_transfer_status";
	public static final String DATA_TRANSFER_TYPE_ATTRIBUTE = 
                               "data_transfer_type";
	public static final String SOURCE_FILE_SIZE_ATTRIBUTE = 
                               "source_file_size";
	public static final String CALLER_OBJECT_ID_ATTRIBUTE = 
                               "archive_caller_object_id";
	public static final String METADATA_ORIGIN_ATTRIBUTE = 
                               "metadata_origin";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// Metadata validation rules.
	List<HpcMetadataValidationRule> collectionMetadataValidationRules = new ArrayList<>();
	List<HpcMetadataValidationRule> dataObjectMetadataValidationRules = new ArrayList<>();
	
	// Set of system generated metadata attributes.
	Set<String> systemGeneratedMetadataAttributes = new HashSet<>();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    @SuppressWarnings("unused")
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
    public HpcMetadataValidator(String metadataValidationRulesPath) throws HpcException
    {
		try {
	         JSONObject jsonMetadataValidationRules = getMetadataValidationRulesJSON(metadataValidationRulesPath);
	         
	         collectionMetadataValidationRules.addAll(
	        		   rulesFromJSON((JSONArray) jsonMetadataValidationRules.get("collectionMetadataValidationRules")));
	         dataObjectMetadataValidationRules.addAll(
	        		   rulesFromJSON((JSONArray) jsonMetadataValidationRules.get("dataObjectMetadataValidationRules")));
	         
	         List<String> attributes = 
	              Arrays.asList(ID_ATTRIBUTE, REGISTRAR_ID_ATTRIBUTE, REGISTRAR_NAME_ATTRIBUTE,
	            		        REGISTRAR_DOC_ATTRIBUTE, SOURCE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE,
	            		        SOURCE_LOCATION_FILE_ID_ATTRIBUTE, ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE,
	            		        ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE, DATA_TRANSFER_REQUEST_ID_ATTRIBUTE,
	            		        DATA_TRANSFER_STATUS_ATTRIBUTE, DATA_TRANSFER_TYPE_ATTRIBUTE,
	            		        SOURCE_FILE_SIZE_ATTRIBUTE, CALLER_OBJECT_ID_ATTRIBUTE, 
	            		        METADATA_ORIGIN_ATTRIBUTE);
	         
	         systemGeneratedMetadataAttributes.addAll(attributes);
	         
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
     * @param existingMetadataEntries Optional (can be null). The metadata entries currently associated 
     *                                with the collection or data object.
     * @param addUpdateMetadataEntries Optional (can be null) A list of metadata entries
     *                                 that are being added or updated to 'metadataEntries'.
     * 
     * @throws HpcException If the metadata is invalid.
     */
    public void validateCollectionMetadata(List<HpcMetadataEntry> existingMetadataEntries,
    		                               List<HpcMetadataEntry> addUpdateMetadataEntries) 
    		                              throws HpcException
    {
    	validateMetadata(existingMetadataEntries, 
    			         addUpdateMetadataEntries,
    			         collectionMetadataValidationRules);
    }
    
    /**
     * Validate data object metadata. Null unit values are converted to empty strings.
     *
     * @param existingMetadataEntries Optional (can be null). The metadata entries currently associated 
     *                                with the collection or data object.
     * @param addUpdateMetadataEntries Optional (can be null) A list of metadata entries
     *                                 that are being added or updated to 'metadataEntries'. 
     * 
     * @throws HpcException If the metadata is invalid.
     */
    public void validateDataObjectMetadata(List<HpcMetadataEntry> existingMetadataEntries,
    		                               List<HpcMetadataEntry> addUpdateMetadataEntries) 
    		                              throws HpcException
    {
    	validateMetadata(existingMetadataEntries, 
    			         addUpdateMetadataEntries,
    			         dataObjectMetadataValidationRules);
    }
    
    /**
     * Return the set of system generated metadata attributes
     *
     * @return The set of system generated metadata attributes.
     */
    public Set<String> getSystemGeneratedMetadataAttributes()
    {
    	return systemGeneratedMetadataAttributes;
    }
    		                               
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
    
    /**
     * Validate metadata. Null unit values are converted to empty strings.
     *
     * @param existingMetadataEntries Optional (can be null). The metadata entries currently associated 
     *                                with the collection or data object.
     * @param addUpdateMetadataEntries Optional (can be null) A list of metadata entries
     *                                 that are being added or updated to 'metadataEntries'. 
     * @param metadataValidationRules Validation rules to apply.
     * 
     * @throws HpcException If the metadata is invalid.
     */
    private void validateMetadata(List<HpcMetadataEntry> existingMetadataEntries,
    		                      List<HpcMetadataEntry> addUpdateMetadataEntries,
    		                      List<HpcMetadataValidationRule> metadataValidationRules) 
    		                     throws HpcException
    {
    	// Crate a metadata <attribute, value> map. Put existing entries first.
    	Map<String, String> metadataEntriesMap = new HashMap<>();
    	if(existingMetadataEntries != null) {
    	   for(HpcMetadataEntry metadataEntry : existingMetadataEntries) {
    		   metadataEntriesMap.put(metadataEntry.getAttribute(), metadataEntry.getValue());
    		   // Default null unit values to empty string (This is an iRODS expectation).
    		   if(metadataEntry.getUnit() == null) {
    		      metadataEntry.setUnit("");	
    		   }
    	   }
    	}
    	
    	// Add Add/Update metadata entries to the map.
    	Map<String, String> addUpdateMetadataEntriesMap = new HashMap<>();
    	for(HpcMetadataEntry metadataEntry : addUpdateMetadataEntries) {
    		metadataEntriesMap.put(metadataEntry.getAttribute(), metadataEntry.getValue());
    		addUpdateMetadataEntriesMap.put(metadataEntry.getAttribute(), metadataEntry.getValue());
    		// Default null unit values to empty string (This is an iRODS expectation).
    		if(metadataEntry.getUnit() == null) {
    		   metadataEntry.setUnit("");	
    		}
    	}
    	
    	// Validate the add/update metadata entries don't include reserved system generated metadata.
    	for(String metadataAttribue : systemGeneratedMetadataAttributes) {
    		if(addUpdateMetadataEntriesMap.containsKey(metadataAttribue)) {
    		   throw new HpcException("System generated metadata can't be set/changed: " + 
    				                  metadataAttribue, 
                                      HpcErrorType.INVALID_REQUEST_INPUT);
    		}
    	}
    	
    	// Execute the validation rules.
	    for(HpcMetadataValidationRule metadataValidationRule: metadataValidationRules) {
	    	// Check if rules needs to be skipped.
	    	if(skipRule(metadataValidationRule, metadataEntriesMap)) {
	    	   continue;
	    	}
	    
	    	// Apply default value/unit if default is defined and metadata was not provided.
	    	HpcMetadataEntry defaultMetadataEntry = 
	    	    generateDefaultMetadataEntry(metadataValidationRule, metadataEntriesMap);
	    	if(defaultMetadataEntry != null) {
			   addUpdateMetadataEntries.add(defaultMetadataEntry);
			   metadataEntriesMap.put(defaultMetadataEntry.getAttribute(), defaultMetadataEntry.getValue());
	    	}
	    	
	    	// Validate a mandatory metadata is provided.
	    	if(metadataValidationRule.getMandatory() &&
	    	   !metadataEntriesMap.containsKey(metadataValidationRule.getAttribute())) {	
			   // Metadata entry is missing, but no default is defined.
			   throw new HpcException("Missing mandataory metadata: " + 
			    		              metadataValidationRule.getAttribute(), 
			                          HpcErrorType.INVALID_REQUEST_INPUT);
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
     * Check if a metadata validation rules needs to be skipped.
     *
     * @param metadataValidationRule The validation rule.
     * @param metadataEntriesMap The metadata entries.
     * @return true if the rule needs to be skipped.
     */
    private boolean skipRule(HpcMetadataValidationRule metadataValidationRule,
    		                 Map<String, String> metadataEntriesMap)
    {
		// Skip disabled rules.
		if(!metadataValidationRule.getRuleEnabled()) {
		   return true;
		}
	
	    // Skip rules for other collection types.
		String collectionType = metadataEntriesMap.get(COLLECTION_TYPE_ATTRIBUTE);
		if(collectionType != null &&
		   metadataValidationRule.getCollectionTypes() != null &&
		   !metadataValidationRule.getCollectionTypes().isEmpty() &&
		   !metadataValidationRule.getCollectionTypes().contains(collectionType)) {
		   return true;
		}
		
		return false;
	}
    
    /**
     * Generate a default metadata entry if needed by the validation rule.
     *
     * @param metadataValidationRule The validation rule.
     * @param metadataEntriesMap The metadata entries.
     * @return A default metadata entry if needed.
     */
    private HpcMetadataEntry generateDefaultMetadataEntry(
    		   HpcMetadataValidationRule metadataValidationRule,
               Map<String, String> metadataEntriesMap)
    {
		// Apply default value/unit if default is defined and metadata was not provided.
		if(metadataValidationRule.getDefaultValue() != null &&
		   !metadataValidationRule.getDefaultValue().isEmpty() &&
		   !metadataEntriesMap.containsKey(metadataValidationRule.getAttribute())) {
		   HpcMetadataEntry defaultMetadataEntry = new HpcMetadataEntry();
		   defaultMetadataEntry.setAttribute(metadataValidationRule.getAttribute());
		   defaultMetadataEntry.setValue(metadataValidationRule.getDefaultValue());
		   defaultMetadataEntry.setUnit(metadataValidationRule.getDefaultUnit() != null ?
			 	                        metadataValidationRule.getDefaultUnit() : "");
		   return defaultMetadataEntry;
		}
		
		return null;
    }
    
    /**
     * Instantiate list metadata validation rules from JSON.
     *
     * @param jsonMetadataValidationRules The validation rules JSON array. 
     * @return List<HpcMetadataValidationRule> A collection of metadata validation rules.
     * 
     * @throws HpcException If failed to parse the JSON
     */
    @SuppressWarnings("unchecked")
	private List<HpcMetadataValidationRule> rulesFromJSON(JSONArray jsonMetadataValidationRules) 
    		                                             throws HpcException
    {
    	List<HpcMetadataValidationRule> validationRules = new ArrayList<>();
    	
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
    
    /**
     * Load the metadata validation rules from file.
     * 
     * @param metadataValidationRulesPath The path to the validation rules JSON.
     * @return a JSON object with the validation rules
     * @throws HpcException
     */
	private JSONObject getMetadataValidationRulesJSON(String metadataValidationRulesPath) 
			                                         throws HpcException
    {
		try {
	         FileReader reader = new FileReader(metadataValidationRulesPath);
	         JSONObject jsonMetadataValidationRules = (JSONObject) ((JSONObject) new JSONParser().parse(reader)).get("HpcMetadataValidationRules");
	         if(jsonMetadataValidationRules == null) {
	       	    throw new HpcException("No validation rules",
	                                   HpcErrorType.SPRING_CONFIGURATION_ERROR); 
	         }
	         
             if(!jsonMetadataValidationRules.containsKey("collectionMetadataValidationRules") ||
                !jsonMetadataValidationRules.containsKey("dataObjectMetadataValidationRules")) {
	       	    throw new HpcException("Invalid JSON rules: " + jsonMetadataValidationRules,
	                                   HpcErrorType.SPRING_CONFIGURATION_ERROR);
             }
	         
	         return jsonMetadataValidationRules;
	         
		} catch(Exception e) {
		    throw new HpcException("Could not open or parse: " + metadataValidationRulesPath,
                                   HpcErrorType.SPRING_CONFIGURATION_ERROR, e);
		}
    }
}

 