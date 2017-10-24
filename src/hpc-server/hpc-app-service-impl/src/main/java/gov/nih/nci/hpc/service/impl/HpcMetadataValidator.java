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
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.domain.model.HpcDocConfiguration;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * <p>
 * Validates various metadata provided by the user. Validation rules are DOC specific.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcMetadataValidator
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Collection type attribute name.
	public static final String COLLECTION_TYPE_ATTRIBUTE = "collection_type"; 
	
    // System generated metadata attributes.
	public static final String ID_ATTRIBUTE = "uuid";
	public static final String REGISTRAR_ID_ATTRIBUTE = "registered_by";
	public static final String REGISTRAR_NAME_ATTRIBUTE = "registered_by_name";
	public static final String REGISTRAR_DOC_ATTRIBUTE = "registered_by_doc";
	public static final String SOURCE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE = "source_file_container_id"; 
	public static final String SOURCE_LOCATION_FILE_ID_ATTRIBUTE = "source_file_id"; 
	public static final String ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE = "archive_file_container_id"; 
	public static final String ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE = "archive_file_id"; 
	public static final String DATA_TRANSFER_REQUEST_ID_ATTRIBUTE = "data_transfer_request_id";
	public static final String DATA_TRANSFER_STATUS_ATTRIBUTE = "data_transfer_status";
	public static final String DATA_TRANSFER_TYPE_ATTRIBUTE = "data_transfer_type";
	public static final String DATA_TRANSFER_STARTED_ATTRIBUTE = "data_transfer_started";
	public static final String DATA_TRANSFER_COMPLETED_ATTRIBUTE = "data_transfer_completed";
	public static final String SOURCE_FILE_SIZE_ATTRIBUTE = "source_file_size";
	public static final String CALLER_OBJECT_ID_ATTRIBUTE = "archive_caller_object_id";
	public static final String CHECKSUM_ATTRIBUTE = "checksum";
	public static final String METADATA_UPDATED_ATTRIBUTE = "metadata_updated";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// DOC configuration locator.
	@Autowired
	private HpcDocConfigurationLocator docConfigurationLocator = null;
	
	// Set of system generated metadata attributes.
	Set<String> systemGeneratedMetadataAttributes = new HashSet<>();
	List<String> collectionSystemGeneratedMetadataAttributeNames = new ArrayList<>();
	List<String> dataObjectSystemGeneratedMetadataAttributeNames = new ArrayList<>();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
    private HpcMetadataValidator() 
    {
    	List<String> attributes = 
	                 Arrays.asList(ID_ATTRIBUTE, REGISTRAR_ID_ATTRIBUTE, REGISTRAR_NAME_ATTRIBUTE,
	              		           REGISTRAR_DOC_ATTRIBUTE, SOURCE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE,
	            		           SOURCE_LOCATION_FILE_ID_ATTRIBUTE, ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE,
	            		           ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE, DATA_TRANSFER_REQUEST_ID_ATTRIBUTE,
	            		           DATA_TRANSFER_STATUS_ATTRIBUTE, DATA_TRANSFER_TYPE_ATTRIBUTE,
	            		           DATA_TRANSFER_STARTED_ATTRIBUTE, DATA_TRANSFER_COMPLETED_ATTRIBUTE, 
	            		           SOURCE_FILE_SIZE_ATTRIBUTE, CALLER_OBJECT_ID_ATTRIBUTE, 
	            		           CHECKSUM_ATTRIBUTE, METADATA_UPDATED_ATTRIBUTE);
    	List<String> collectionAttributes = 
    			     Arrays.asList(ID_ATTRIBUTE, REGISTRAR_ID_ATTRIBUTE, REGISTRAR_NAME_ATTRIBUTE,
		                           REGISTRAR_DOC_ATTRIBUTE, METADATA_UPDATED_ATTRIBUTE);
	         
        systemGeneratedMetadataAttributes.addAll(attributes);
	    collectionSystemGeneratedMetadataAttributeNames.addAll(collectionAttributes);
	    dataObjectSystemGeneratedMetadataAttributeNames.addAll(attributes);
    }	
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Validate collection metadata. Null unit values are converted to empty strings.
     *
     * @param doc The DOC to determine the validation rules (which are DOC specific). 
     * @param existingMetadataEntries Optional (can be null). The metadata entries currently associated 
     *                                with the collection or data object.
     * @param addUpdateMetadataEntries Optional (can be null) A list of metadata entries
     *                                 that are being added or updated to 'metadataEntries'.
     * 
     * @throws HpcException If the metadata is invalid.
     */
    public void validateCollectionMetadata(String doc,
    		                               List<HpcMetadataEntry> existingMetadataEntries,
    		                               List<HpcMetadataEntry> addUpdateMetadataEntries) 
    		                              throws HpcException
    {
    	HpcDocConfiguration docConfiguration = docConfigurationLocator.get(doc);
    	if(docConfiguration == null) {
    	   throw new HpcException("Invalid DOC: " + doc, HpcRequestRejectReason.INVALID_DOC);
    	}
    	
    	validateMetadata(existingMetadataEntries, 
    			         addUpdateMetadataEntries,
    			         docConfiguration.getCollectionMetadataValidationRules(),
    			         null);
    }
    
    /**
     * Validate data object metadata. Null unit values are converted to empty strings.
     *
     * @param doc The DOC to determine the validation rules (which are DOC specific). 
     * @param existingMetadataEntries Optional (can be null). The metadata entries currently associated 
     *                                with the collection or data object.
     * @param addUpdateMetadataEntries Optional (can be null) A list of metadata entries
     *                                 that are being added or updated to 'metadataEntries'. 
     * @param collectionType The type of collection containing the data object.
     * 
     * @throws HpcException If the metadata is invalid.
     */
    public void validateDataObjectMetadata(String doc,
    		                               List<HpcMetadataEntry> existingMetadataEntries,
    		                               List<HpcMetadataEntry> addUpdateMetadataEntries,
    		                               String collectionType) 
    		                              throws HpcException
    {
    	HpcDocConfiguration docConfiguration = docConfigurationLocator.get(doc);
    	if(docConfiguration == null) {
    	   throw new HpcException("Invalid DOC: " + doc, HpcRequestRejectReason.INVALID_DOC);
    	}
    	
    	validateMetadata(existingMetadataEntries, 
    			         addUpdateMetadataEntries,
    			         docConfiguration.getDataObjectMetadataValidationRules(),
    			         collectionType);
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

    /**
     * Return the set of system generated metadata attributes
     *
     * @return The List of system generated metadata attributes for a collection.
     */
    public List<String> getCollectionSystemGeneratedMetadataAttributeNames()
    {
    	return collectionSystemGeneratedMetadataAttributeNames;
    }
    
    /**
     * Return the set of system generated metadata attributes
     *
     * @return The List of system generated metadata attributes for an object.
     */
    public List<String> getDataObjectSystemGeneratedMetadataAttributeNames()
    {
    	return dataObjectSystemGeneratedMetadataAttributeNames;
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
     * @param collectionType (Optional) The collection type. In case of data object, the type of collection
     *                       containing the data object.
     * 
     * @throws HpcException If the metadata is invalid.
     */
    private void validateMetadata(List<HpcMetadataEntry> existingMetadataEntries,
    		                      List<HpcMetadataEntry> addUpdateMetadataEntries,
    		                      List<HpcMetadataValidationRule> metadataValidationRules,
    		                      String collectionType) 
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
    		if(addUpdateMetadataEntriesMap.put(metadataEntry.getAttribute(), metadataEntry.getValue()) != null) {
    		   // Metadata attributes are expected to be unique.
    		   throw new HpcException("Metadata attribute is not unique: " +  metadataEntry.getAttribute(),
                                      HpcErrorType.INVALID_REQUEST_INPUT);
    		}
    		
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
    	
    	// Determining the collection type of this collection or collection containing this data object.
    	if(StringUtils.isEmpty(collectionType)) {
     	   collectionType = metadataEntriesMap.get(COLLECTION_TYPE_ATTRIBUTE);
     	}
    	
    	// Execute the validation rules.
	    for(HpcMetadataValidationRule metadataValidationRule: metadataValidationRules) {
	    	// Check if rules needs to be skipped.
	    	if(skipRule(metadataValidationRule, metadataEntriesMap, collectionType)) {
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
     * @param collectionType the collection type for collection metadata, 
     *                       or the collection type hosting the data object.
     * @return true if the rule needs to be skipped.
     */
    private boolean skipRule(HpcMetadataValidationRule metadataValidationRule,
    		                 Map<String, String> metadataEntriesMap,
    		                 String collectionType)
    {
		// Skip disabled rules.
		if(!metadataValidationRule.getRuleEnabled()) {
		   return true;
		}
	
	    // Skip rules for other collection types.
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
}

 